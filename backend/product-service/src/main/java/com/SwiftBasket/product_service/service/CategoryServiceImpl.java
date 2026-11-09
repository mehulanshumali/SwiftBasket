package com.SwiftBasket.product_service.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.SwiftBasket.product_service.dto.request.CategoryRequest;
import com.SwiftBasket.product_service.entity.Category;
import com.SwiftBasket.product_service.exception.CategoryAlreadyExistsException;
import com.SwiftBasket.product_service.exception.CategoryInUseException;
import com.SwiftBasket.product_service.exception.CategoryNotFoundException;
import com.SwiftBasket.product_service.repository.CategoryRepository;
import com.SwiftBasket.product_service.repository.ProductRepository;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class CategoryServiceImpl implements CategoryService {

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductRepository productRepository;

    @Override
    public List<Category> getAllCategories() {
        log.info("Fetching all categories");
        return categoryRepository.findAll();
    }

    @Override
    @Transactional
    public Category createCategory(CategoryRequest request) {
        log.info("Creating new category with name: {}", request.getName());

        if (categoryRepository.findByName(request.getName()).isPresent()) {
            log.warn("Category already exists with name: {}", request.getName());
            throw new CategoryAlreadyExistsException("Category with this name already exists");
        }

        Category category = Category.builder()
                .name(request.getName())
                .description(request.getDescription())
                .build();

        Category savedCategory = categoryRepository.save(category);
        log.info("Category created successfully with ID: {}", savedCategory.getId());
        return savedCategory;
    }

    @Override
    @Transactional
    public Category updateCategory(Long categoryId, CategoryRequest request) {
        log.info("Updating category with ID: {}", categoryId);

        Category category = categoryRepository.findById(categoryId).orElseThrow(() -> {
            log.warn("Category not found with ID: {}", categoryId);
            return new CategoryNotFoundException("Category not found");
        });

        categoryRepository.findByName(request.getName()).ifPresent(existing -> {
            if (!existing.getId().equals(categoryId)) {
                log.warn("Category name already in use: {}", request.getName());
                throw new CategoryAlreadyExistsException("Category name is already in use");
            }
        });

        category.setName(request.getName());
        category.setDescription(request.getDescription());

        Category updatedCategory = categoryRepository.save(category);
        log.info("Category updated successfully with ID: {}", updatedCategory.getId());
        return updatedCategory;
    }

    @Override
    @Transactional
    public void deleteCategory(Long categoryId) {
        log.info("Deleting category with ID: {}", categoryId);

        Category category = categoryRepository.findById(categoryId).orElseThrow(() -> {
            log.warn("Category not found with ID: {}", categoryId);
            return new CategoryNotFoundException("Category not found");
        });

        if (productRepository.existsByCategoryId(categoryId)) {
            log.warn("Cannot delete category ID {} - products still reference it", categoryId);
            throw new CategoryInUseException("Cannot delete category: products still reference it");
        }

        categoryRepository.delete(category);
        log.info("Category deleted successfully with ID: {}", categoryId);
    }
}
