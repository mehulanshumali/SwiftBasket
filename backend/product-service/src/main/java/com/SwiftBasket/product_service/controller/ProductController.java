package com.SwiftBasket.product_service.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.SwiftBasket.product_service.dto.request.ProductRequest;
import com.SwiftBasket.product_service.dto.response.PagedResponse;
import com.SwiftBasket.product_service.dto.response.ProductResponse;
import com.SwiftBasket.product_service.dto.response.ProductSearchResponse;
import com.SwiftBasket.product_service.entity.Product;
import com.SwiftBasket.product_service.exception.AdminAccessException;
import com.SwiftBasket.product_service.mapper.ProductMapper;
import com.SwiftBasket.product_service.service.ProductService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/v1/products")
@Slf4j
@Tag(name = "Products", description = "Product browsing, search, and management endpoints")
public class ProductController {

    @Autowired
    private ProductService productService;

    @Autowired
    private ProductMapper productMapper;

    @GetMapping
    @Operation(summary = "Browse products", description = "Returns a paginated list of active products, optionally filtered by category.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Product list retrieved successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = PagedResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid pagination parameters")
    })
    public ResponseEntity<PagedResponse<ProductResponse>> getAllProducts(
            @Parameter(description = "Filter by category ID") @RequestParam(required = false) Long categoryId,
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size) {

        log.info("Browse products request - categoryId: {}, page: {}, size: {}", categoryId, page, size);

        PagedResponse<ProductResponse> response = (categoryId != null)
                ? productService.getProductsByCategory(categoryId, page, size)
                : productService.getAllProducts(page, size);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/search")
    @Operation(summary = "Search products", description = "Full-text search on product name and description via Elasticsearch with fuzzy matching.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Search results retrieved successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = PagedResponse.class))),
        @ApiResponse(responseCode = "400", description = "Missing search query")
    })
    public ResponseEntity<PagedResponse<ProductSearchResponse>> searchProducts(
            @Parameter(description = "Search query", required = true) @RequestParam String q,
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size) {

        log.info("Search products request - query: '{}', page: {}, size: {}", q, page, size);
        PagedResponse<ProductSearchResponse> response = productService.searchProducts(q, page, size);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/health")
    @Operation(summary = "Health check", description = "Simple health check endpoint to verify service is running.")
    @ApiResponse(responseCode = "200", description = "Service is healthy")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Product Service is running");
    }

    @PostMapping("/reindex")
    @Operation(summary = "Rebuild search index", description = "Rebuilds the full Elasticsearch index from the product database. Admin only.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Index rebuilt, returns number of indexed products"),
        @ApiResponse(responseCode = "403", description = "Admin role required")
    })
    public ResponseEntity<Integer> reindex(
            @Parameter(description = "User role injected by Kong", required = true)
            @RequestHeader(value = "X-User-Role", required = false) String userRole) {

        log.info("Search index rebuild requested");
        requireAdmin(userRole, "rebuild search index");
        int count = productService.reindexAll();
        return ResponseEntity.ok(count);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get product details", description = "Fetches full product details including SKU, price, stock, and category.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Product retrieved successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProductResponse.class))),
        @ApiResponse(responseCode = "404", description = "Product not found")
    })
    public ResponseEntity<ProductResponse> getProductById(
            @Parameter(description = "Product ID", required = true) @PathVariable Long id) {

        log.info("Fetching product details for ID: {}", id);
        ProductResponse response = productService.getProductById(id);
        return ResponseEntity.ok(response);
    }

    @PostMapping
    @Operation(summary = "Create product", description = "Creates a new product. Validates SKU uniqueness and category existence. Admin only.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Product created successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProductResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid input"),
        @ApiResponse(responseCode = "403", description = "Admin role required"),
        @ApiResponse(responseCode = "404", description = "Category not found"),
        @ApiResponse(responseCode = "409", description = "SKU already exists")
    })
    public ResponseEntity<ProductResponse> createProduct(
            @Parameter(description = "User role injected by Kong", required = true)
            @RequestHeader(value = "X-User-Role", required = false) String userRole,
            @Valid @RequestBody ProductRequest request) {

        log.info("Create product request for SKU: {}", request.getSku());
        requireAdmin(userRole, "create products");

        Product product = productService.createProduct(request);
        ProductResponse response = productMapper.toResponse(product);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update product", description = "Updates an existing product and re-indexes it in Elasticsearch. Admin only.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Product updated successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProductResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid input"),
        @ApiResponse(responseCode = "403", description = "Admin role required"),
        @ApiResponse(responseCode = "404", description = "Product or category not found"),
        @ApiResponse(responseCode = "409", description = "SKU already in use")
    })
    public ResponseEntity<ProductResponse> updateProduct(
            @Parameter(description = "User role injected by Kong", required = true)
            @RequestHeader(value = "X-User-Role", required = false) String userRole,
            @Parameter(description = "Product ID", required = true) @PathVariable Long id,
            @Valid @RequestBody ProductRequest request) {

        log.info("Update product request for ID: {}", id);
        requireAdmin(userRole, "update products");

        Product product = productService.updateProduct(id, request);
        ProductResponse response = productMapper.toResponse(product);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete product", description = "Soft-deletes a product (sets active=false) and removes it from the search index. Admin only.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Product deleted successfully"),
        @ApiResponse(responseCode = "403", description = "Admin role required"),
        @ApiResponse(responseCode = "404", description = "Product not found")
    })
    public ResponseEntity<Void> deleteProduct(
            @Parameter(description = "User role injected by Kong", required = true)
            @RequestHeader(value = "X-User-Role", required = false) String userRole,
            @Parameter(description = "Product ID", required = true) @PathVariable Long id) {

        log.info("Delete product request for ID: {}", id);
        requireAdmin(userRole, "delete products");

        productService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/stock")
    @Operation(summary = "Adjust product stock", description = "Atomically adjusts stock by a delta (negative = reserve, positive = release). Uses optimistic locking.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Stock adjusted successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProductResponse.class))),
        @ApiResponse(responseCode = "404", description = "Product not found"),
        @ApiResponse(responseCode = "409", description = "Insufficient stock or concurrent modification")
    })
    public ResponseEntity<ProductResponse> updateStock(
            @Parameter(description = "Product ID", required = true) @PathVariable Long id,
            @Parameter(description = "Stock adjustment (negative = reserve, positive = release)", required = true)
            @RequestParam int delta) {

        log.info("Stock adjustment request for product ID: {} delta: {}", id, delta);
        Product product = productService.updateStock(id, delta);
        ProductResponse response = productMapper.toResponse(product);
        return ResponseEntity.ok(response);
    }

    private void requireAdmin(String role, String action) {
        if (role == null || !role.equals("ADMIN")) {
            log.warn("Non-admin attempt to {} (role: {})", action, role);
            throw new AdminAccessException("Admin role required to " + action);
        }
    }
}
