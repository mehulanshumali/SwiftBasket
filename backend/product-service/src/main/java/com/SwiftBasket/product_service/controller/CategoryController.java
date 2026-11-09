package com.SwiftBasket.product_service.controller;

import java.util.List;

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
import org.springframework.web.bind.annotation.RestController;

import com.SwiftBasket.product_service.dto.request.CategoryRequest;
import com.SwiftBasket.product_service.dto.response.CategoryResponse;
import com.SwiftBasket.product_service.entity.Category;
import com.SwiftBasket.product_service.exception.AdminAccessException;
import com.SwiftBasket.product_service.mapper.CategoryMapper;
import com.SwiftBasket.product_service.service.CategoryService;

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
@RequestMapping("/api/v1/categories")
@Slf4j
@Tag(name = "Categories", description = "Product category management endpoints")
public class CategoryController {

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private CategoryMapper categoryMapper;

    @GetMapping
    @Operation(summary = "List all categories", description = "Returns all product categories for storefront navigation.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Category list retrieved successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = CategoryResponse.class)))
    })
    public ResponseEntity<List<CategoryResponse>> getAllCategories() {
        log.info("Fetching all categories");
        List<CategoryResponse> response = categoryService.getAllCategories().stream()
                .map(categoryMapper::toResponse)
                .toList();
        return ResponseEntity.ok(response);
    }

    @PostMapping
    @Operation(summary = "Create category", description = "Creates a new product category. Admin only.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Category created successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = CategoryResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid input"),
        @ApiResponse(responseCode = "403", description = "Admin role required"),
        @ApiResponse(responseCode = "409", description = "Category name already exists")
    })
    public ResponseEntity<CategoryResponse> createCategory(
            @Parameter(description = "User role injected by Kong", required = true)
            @RequestHeader(value = "X-User-Role", required = false) String userRole,
            @Valid @RequestBody CategoryRequest request) {

        log.info("Create category request for name: {}", request.getName());
        requireAdmin(userRole, "create categories");

        Category category = categoryService.createCategory(request);
        CategoryResponse response = categoryMapper.toResponse(category);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update category", description = "Updates a category's name and description. Admin only.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Category updated successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = CategoryResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid input"),
        @ApiResponse(responseCode = "403", description = "Admin role required"),
        @ApiResponse(responseCode = "404", description = "Category not found"),
        @ApiResponse(responseCode = "409", description = "Category name already in use")
    })
    public ResponseEntity<CategoryResponse> updateCategory(
            @Parameter(description = "User role injected by Kong", required = true)
            @RequestHeader(value = "X-User-Role", required = false) String userRole,
            @Parameter(description = "Category ID", required = true) @PathVariable Long id,
            @Valid @RequestBody CategoryRequest request) {

        log.info("Update category request for ID: {}", id);
        requireAdmin(userRole, "update categories");

        Category category = categoryService.updateCategory(id, request);
        CategoryResponse response = categoryMapper.toResponse(category);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete category", description = "Deletes a category. Fails with 409 if products still reference it. Admin only.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Category deleted successfully"),
        @ApiResponse(responseCode = "403", description = "Admin role required"),
        @ApiResponse(responseCode = "404", description = "Category not found"),
        @ApiResponse(responseCode = "409", description = "Category is referenced by products")
    })
    public ResponseEntity<Void> deleteCategory(
            @Parameter(description = "User role injected by Kong", required = true)
            @RequestHeader(value = "X-User-Role", required = false) String userRole,
            @Parameter(description = "Category ID", required = true) @PathVariable Long id) {

        log.info("Delete category request for ID: {}", id);
        requireAdmin(userRole, "delete categories");

        categoryService.deleteCategory(id);
        return ResponseEntity.noContent().build();
    }

    private void requireAdmin(String role, String action) {
        if (role == null || !role.equals("ADMIN")) {
            log.warn("Non-admin attempt to {} ( role: {})", action, role);
            throw new AdminAccessException("Admin role required to " + action);
        }
    }
}
