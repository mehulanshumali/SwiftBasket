package com.SwiftBasket.cart_service.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.SwiftBasket.cart_service.document.Cart;
import com.SwiftBasket.cart_service.dto.AddItemRequest;
import com.SwiftBasket.cart_service.dto.CartResponse;
import com.SwiftBasket.cart_service.exception.UnauthorizedCartAccessException;
import com.SwiftBasket.cart_service.mapper.CartMapper;
import com.SwiftBasket.cart_service.service.CartService;

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
@RequestMapping("/api/v1/cart")
@Slf4j
@Tag(name = "Cart", description = "Shopping cart endpoints for registered users and guests")
public class CartController {

    @Autowired
    private CartService cartService;

    @Autowired
    private CartMapper cartMapper;

    @GetMapping
    @Operation(summary = "Get current cart", description = "Returns the authenticated user's or guest's cart with items, line totals, and total price.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Cart retrieved successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = CartResponse.class))),
        @ApiResponse(responseCode = "401", description = "No user or session identity provided")
    })
    public ResponseEntity<CartResponse> getCart(
            @Parameter(description = "User ID injected by Kong (logged-in users)")
            @RequestHeader(value = "X-User-ID", required = false) Long userId,
            @Parameter(description = "Guest session ID")
            @RequestHeader(value = "X-Session-ID", required = false) String sessionId) {

        requireIdentity(userId, sessionId);
        log.info("Get cart request - userId: {}, sessionId: {}", userId, sessionId);

        Cart cart = cartService.getCart(userId, sessionId);
        return ResponseEntity.ok(cartMapper.toResponse(cart));
    }

    @PostMapping("/items")
    @Operation(summary = "Add item to cart", description = "Adds a product with quantity; snapshots name/image/price from the product service; creates the cart if none exists.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Item added, full cart returned", content = @Content(mediaType = "application/json", schema = @Schema(implementation = CartResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid input"),
        @ApiResponse(responseCode = "401", description = "No user or session identity provided"),
        @ApiResponse(responseCode = "503", description = "Product service unavailable or product not found")
    })
    public ResponseEntity<CartResponse> addItem(
            @Parameter(description = "User ID injected by Kong (logged-in users)")
            @RequestHeader(value = "X-User-ID", required = false) Long userId,
            @Parameter(description = "Guest session ID")
            @RequestHeader(value = "X-Session-ID", required = false) String sessionId,
            @Valid @RequestBody AddItemRequest request) {

        requireIdentity(userId, sessionId);
        log.info("Add item request - productId: {}, quantity: {}, userId: {}, sessionId: {}",
                request.getProductId(), request.getQuantity(), userId, sessionId);

        Cart cart = cartService.addItem(userId, sessionId, request);
        return ResponseEntity.ok(cartMapper.toResponse(cart));
    }

    @PatchMapping("/items/{productId}")
    @Operation(summary = "Update item quantity", description = "Sets a new quantity for a cart item; quantity 0 removes the item.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Quantity updated, full cart returned", content = @Content(mediaType = "application/json", schema = @Schema(implementation = CartResponse.class))),
        @ApiResponse(responseCode = "401", description = "No user or session identity provided"),
        @ApiResponse(responseCode = "404", description = "Product not in cart")
    })
    public ResponseEntity<CartResponse> updateQuantity(
            @Parameter(description = "User ID injected by Kong (logged-in users)")
            @RequestHeader(value = "X-User-ID", required = false) Long userId,
            @Parameter(description = "Guest session ID")
            @RequestHeader(value = "X-Session-ID", required = false) String sessionId,
            @Parameter(description = "Product ID", required = true) @PathVariable Long productId,
            @Parameter(description = "New quantity (0 removes the item)", required = true)
            @RequestParam int quantity) {

        requireIdentity(userId, sessionId);
        log.info("Update quantity request - productId: {}, quantity: {}, userId: {}, sessionId: {}",
                productId, quantity, userId, sessionId);

        Cart cart = cartService.updateQuantity(userId, sessionId, productId, quantity);
        return ResponseEntity.ok(cartMapper.toResponse(cart));
    }

    @DeleteMapping("/items/{productId}")
    @Operation(summary = "Remove item from cart", description = "Removes a single product line from the cart.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Item removed, full cart returned", content = @Content(mediaType = "application/json", schema = @Schema(implementation = CartResponse.class))),
        @ApiResponse(responseCode = "401", description = "No user or session identity provided"),
        @ApiResponse(responseCode = "404", description = "Product not in cart")
    })
    public ResponseEntity<CartResponse> removeItem(
            @Parameter(description = "User ID injected by Kong (logged-in users)")
            @RequestHeader(value = "X-User-ID", required = false) Long userId,
            @Parameter(description = "Guest session ID")
            @RequestHeader(value = "X-Session-ID", required = false) String sessionId,
            @Parameter(description = "Product ID", required = true) @PathVariable Long productId) {

        requireIdentity(userId, sessionId);
        log.info("Remove item request - productId: {}, userId: {}, sessionId: {}",
                productId, userId, sessionId);

        Cart cart = cartService.removeItem(userId, sessionId, productId);
        return ResponseEntity.ok(cartMapper.toResponse(cart));
    }

    @DeleteMapping
    @Operation(summary = "Clear cart", description = "Removes all items but keeps the cart shell.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Cart cleared successfully"),
        @ApiResponse(responseCode = "401", description = "No user or session identity provided")
    })
    public ResponseEntity<Void> clearCart(
            @Parameter(description = "User ID injected by Kong (logged-in users)")
            @RequestHeader(value = "X-User-ID", required = false) Long userId,
            @Parameter(description = "Guest session ID")
            @RequestHeader(value = "X-Session-ID", required = false) String sessionId) {

        requireIdentity(userId, sessionId);
        log.info("Clear cart request - userId: {}, sessionId: {}", userId, sessionId);

        cartService.clearCart(userId, sessionId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/merge")
    @Operation(summary = "Merge guest cart into user cart", description = "Called after login; merges the guest session cart into the user's cart - duplicate products sum quantities, new products are appended; the guest cart is deleted; returns the merged cart.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Carts merged successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = CartResponse.class))),
        @ApiResponse(responseCode = "401", description = "No user or session identity provided")
    })
    public ResponseEntity<CartResponse> mergeGuestCart(
            @Parameter(description = "User ID injected by Kong (logged-in users)")
            @RequestHeader(value = "X-User-ID", required = false) Long userId,
            @Parameter(description = "Guest session ID")
            @RequestHeader(value = "X-Session-ID", required = false) String sessionId) {

        requireIdentity(userId, sessionId);
        log.info("Merge guest cart request - userId: {}, sessionId: {}", userId, sessionId);

        Cart cart = cartService.mergeGuestCart(userId, sessionId);
        return ResponseEntity.ok(cartMapper.toResponse(cart));
    }

    @GetMapping("/health")
    @Operation(summary = "Health check", description = "Simple health check endpoint to verify service is running.")
    @ApiResponse(responseCode = "200", description = "Service is healthy")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Cart Service is running");
    }

    // Identity resolution: Kong injects X-User-ID for logged-in users,
    // guests carry X-Session-ID. Exactly one must be present.
    private void requireIdentity(Long userId, String sessionId) {
        if (userId == null && sessionId == null) {
            log.warn("Cart request without user or session identity");
            throw new UnauthorizedCartAccessException("User or session identity required");
        }
    }
}
