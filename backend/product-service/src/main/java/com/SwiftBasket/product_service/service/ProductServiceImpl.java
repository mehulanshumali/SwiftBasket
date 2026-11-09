package com.SwiftBasket.product_service.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.SwiftBasket.product_service.document.ProductDocument;
import com.SwiftBasket.product_service.dto.request.ProductRequest;
import com.SwiftBasket.product_service.dto.response.PagedResponse;
import com.SwiftBasket.product_service.dto.response.ProductResponse;
import com.SwiftBasket.product_service.dto.response.ProductSearchResponse;
import com.SwiftBasket.product_service.entity.Category;
import com.SwiftBasket.product_service.entity.Product;
import com.SwiftBasket.product_service.exception.CategoryNotFoundException;
import com.SwiftBasket.product_service.exception.InsufficientStockException;
import com.SwiftBasket.product_service.exception.ProductAlreadyExistsException;
import com.SwiftBasket.product_service.exception.ProductNotFoundException;
import com.SwiftBasket.product_service.mapper.ProductMapper;
import com.SwiftBasket.product_service.repository.CategoryRepository;
import com.SwiftBasket.product_service.repository.ProductRepository;
import com.SwiftBasket.product_service.repository.ProductSearchRepository;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class ProductServiceImpl implements ProductService {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductSearchRepository productSearchRepository;

    @Autowired
    private ProductMapper productMapper;

    @Override
    public PagedResponse<ProductResponse> getAllProducts(int page, int size) {
        log.info("Fetching all products, page: {}, size: {}", page, size);

        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
        Page<Product> productPage = productRepository.findByActiveTrue(pageable);

        List<ProductResponse> content = productPage.getContent().stream()
                .map(productMapper::toResponse)
                .toList();

        log.info("Fetched {} products of {} total", content.size(), productPage.getTotalElements());
        return buildPagedResponse(content, productPage);
    }

    @Override
    public PagedResponse<ProductResponse> getProductsByCategory(Long categoryId, int page, int size) {
        log.info("Fetching products for category ID: {}, page: {}, size: {}", categoryId, page, size);

        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
        Page<Product> productPage = productRepository.findByCategoryIdAndActiveTrue(categoryId, pageable);

        List<ProductResponse> content = productPage.getContent().stream()
                .map(productMapper::toResponse)
                .toList();

        log.info("Fetched {} products for category ID: {}", content.size(), categoryId);
        return buildPagedResponse(content, productPage);
    }

    @Override
    public ProductResponse getProductById(Long productId) {
        log.info("Fetching product with ID: {}", productId);

        Product product = productRepository.findByIdAndActiveTrue(productId).orElseThrow(() -> {
            log.warn("Product not found with ID: {}", productId);
            return new ProductNotFoundException("Product not found");
        });

        return productMapper.toResponse(product);
    }

    @Override
    public PagedResponse<ProductSearchResponse> searchProducts(String query, int page, int size) {
        log.info("Searching products with query: '{}', page: {}, size: {}", query, page, size);

        Pageable pageable = PageRequest.of(page, size);
        Page<ProductDocument> documentPage = productSearchRepository.searchByKeyword(query, pageable);

        List<ProductSearchResponse> content = documentPage.getContent().stream()
                .map(productMapper::toSearchResponse)
                .toList();

        log.info("Search returned {} results of {} total for query: '{}'",
                content.size(), documentPage.getTotalElements(), query);
        return buildPagedResponse(content, documentPage);
    }

    @Override
    @Transactional
    public Product createProduct(ProductRequest request) {
        log.info("Creating new product with SKU: {}", request.getSku());

        Category category = categoryRepository.findById(request.getCategoryId()).orElseThrow(() -> {
            log.warn("Category not found with ID: {}", request.getCategoryId());
            return new CategoryNotFoundException("Category not found");
        });

        if (productRepository.findBySku(request.getSku()).isPresent()) {
            log.warn("Product already exists with SKU: {}", request.getSku());
            throw new ProductAlreadyExistsException("Product with this SKU already exists");
        }

        Product product = productMapper.toEntity(request, category);

        Product savedProduct = productRepository.save(product);
        log.info("Product created successfully with ID: {}", savedProduct.getId());

        indexProduct(savedProduct);
        return savedProduct;
    }

    @Override
    @Transactional
    public Product updateProduct(Long productId, ProductRequest request) {
        log.info("Updating product with ID: {}", productId);

        Product product = productRepository.findById(productId).orElseThrow(() -> {
            log.warn("Product not found with ID: {}", productId);
            return new ProductNotFoundException("Product not found");
        });

        Category category = categoryRepository.findById(request.getCategoryId()).orElseThrow(() -> {
            log.warn("Category not found with ID: {}", request.getCategoryId());
            return new CategoryNotFoundException("Category not found");
        });

        productRepository.findBySku(request.getSku()).ifPresent(existing -> {
            if (!existing.getId().equals(productId)) {
                log.warn("SKU already in use by product ID: {}", existing.getId());
                throw new ProductAlreadyExistsException("Product with this SKU already exists");
            }
        });

        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setImageUrl(request.getImageUrl());
        product.setStockQuantity(request.getStockQuantity() == null ? 0 : request.getStockQuantity());
        product.setCategory(category);

        Product updatedProduct = productRepository.save(product);
        log.info("Product updated successfully with ID: {} (version: {})",
                updatedProduct.getId(), updatedProduct.getVersion());

        indexProduct(updatedProduct);
        return updatedProduct;
    }

    @Override
    @Transactional
    public void deleteProduct(Long productId) {
        log.info("Soft-deleting product with ID: {}", productId);

        Product product = productRepository.findById(productId).orElseThrow(() -> {
            log.warn("Product not found with ID: {}", productId);
            return new ProductNotFoundException("Product not found");
        });

        product.setActive(false);
        productRepository.save(product);

        try {
            productSearchRepository.deleteById(String.valueOf(product.getId()));
        } catch (Exception e) {
            log.error("Failed to remove product {} from search index: {}", product.getId(), e.getMessage());
        }

        log.info("Product soft-deleted successfully with ID: {} SKU: {}", product.getId(), product.getSku());
    }

    @Override
    @Transactional
    public Product updateStock(Long productId, int delta) {
        log.info("Adjusting stock for product ID: {} by delta: {}", productId, delta);

        Product product = productRepository.findById(productId).orElseThrow(() -> {
            log.warn("Product not found with ID: {}", productId);
            return new ProductNotFoundException("Product not found");
        });

        int newStockQuantity = product.getStockQuantity() + delta;
        if (newStockQuantity < 0) {
            log.warn("Insufficient stock for product ID: {} - have: {}, requested: {}",
                    productId, product.getStockQuantity(), Math.abs(delta));
            throw new InsufficientStockException("Insufficient stock");
        }

        product.setStockQuantity(newStockQuantity);

        // @Version optimistic locking: concurrent updates throw OptimisticLockingFailureException
        Product updatedProduct = productRepository.save(product);
        log.info("Stock updated successfully for product ID: {} - new quantity: {} (version: {})",
                productId, newStockQuantity, updatedProduct.getVersion());
        return updatedProduct;
    }

    @Override
    @Transactional
    public int reindexAll() {
        log.info("Rebuilding search index from product database");

        productSearchRepository.deleteAll();

        int count = 0;
        List<Product> activeProducts = productRepository.findByActiveTrue(Pageable.unpaged()).getContent();
        for (Product product : activeProducts) {
            productSearchRepository.save(productMapper.toDocument(product));
            count++;
        }

        log.info("Search index rebuilt successfully with {} products", count);
        return count;
    }

    private void indexProduct(Product product) {
        try {
            productSearchRepository.save(productMapper.toDocument(product));
            log.info("Product ID: {} indexed in Elasticsearch", product.getId());
        } catch (Exception e) {
            // MySQL is the source of truth; the index can be rebuilt via /reindex
            log.error("Failed to index product ID: {} in Elasticsearch: {}", product.getId(), e.getMessage());
        }
    }

    private <T, P extends Page<?>> PagedResponse<T> buildPagedResponse(List<T> content, P page) {
        return PagedResponse.<T>builder()
                .content(content)
                .pageNumber(page.getNumber())
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .build();
    }
}
