package com.SwiftBasket.cart_service.service;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import com.SwiftBasket.cart_service.client.ProductClient;
import com.SwiftBasket.cart_service.document.Cart;
import com.SwiftBasket.cart_service.document.CartItem;
import com.SwiftBasket.cart_service.dto.AddItemRequest;
import com.SwiftBasket.cart_service.dto.ProductResponse;
import com.SwiftBasket.cart_service.event.CartItemAddedEvent;
import com.SwiftBasket.cart_service.exception.CartItemNotFoundException;
import com.SwiftBasket.cart_service.repository.CartRepository;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class CartServiceImpl implements CartService {

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private ProductClient productClient;

    @Autowired
    private CartCacheService cacheService;

    @Autowired
    private KafkaTemplate<String, CartItemAddedEvent> kafkaTemplate;

    @Value("${kafka_topic.name}")
    private String cartItemAddedTopic;

    @Override
    public Cart getCart(Long userId, String sessionId) {
        log.info("Fetching cart - userId: {}, sessionId: {}", userId, sessionId);

        // cache-aside read: try Redis first, fall back to Mongo, then warm the cache
        Optional<Cart> cached = cacheService.get(userId, sessionId);
        if (cached.isPresent()) {
            return cached.get();
        }

        Optional<Cart> cart = findCart(userId, sessionId);
        Cart result = cart.orElseGet(() -> buildEmptyCart(userId, sessionId));

        // only cache carts that actually exist in Mongo - never the empty shell
        // (a cached shell would mask a real cart created moments later)
        if (result.getId() != null) {
            cacheService.put(result);
        }

        log.info("Cart fetched - cartId: {}, itemCount: {}",
                result.getId(), result.getItems().size());
        return result;
    }

    @Override
    public Cart addItem(Long userId, String sessionId, AddItemRequest request) {
        log.info("Adding product {} (quantity {}) to cart - userId: {}, sessionId: {}",
                request.getProductId(), request.getQuantity(), userId, sessionId);

        ProductResponse product = productClient.getProduct(request.getProductId());

        Cart cart = findCart(userId, sessionId)
                .orElseGet(() -> Cart.builder()
                        .userId(userId)
                        .sessionId(sessionId)
                        .build());
        boolean isNewCart = cart.getId() == null;

        Optional<CartItem> existing = cart.getItems().stream()
                .filter(item -> item.getProductId().equals(request.getProductId()))
                .findFirst();

        if (existing.isPresent()) {
            CartItem item = existing.get();
            item.setQuantity(item.getQuantity() + request.getQuantity());
            log.info("Increased quantity for product {} to {}", request.getProductId(), item.getQuantity());
        } else {
            cart.getItems().add(CartItem.builder()
                    .productId(product.getId())
                    .name(product.getName())
                    .imageUrl(product.getImageUrl())
                    .unitPrice(product.getPrice())
                    .quantity(request.getQuantity())
                    .build());
            log.info("Added new item: {} to cart", product.getName());
        }

        Cart savedCart = cartRepository.save(cart);
        log.info("Cart saved - cartId: {}, items: {}, total: {}{}",
                savedCart.getId(), savedCart.getItems().size(), savedCart.getTotalPrice(),
                isNewCart ? " (new cart created)" : "");

        cacheService.evict(userId, sessionId);
        publishCartItemAddedEvent(savedCart, request.getProductId());
        return savedCart;
    }

    @Override
    public Cart updateQuantity(Long userId, String sessionId, Long productId, int quantity) {
        log.info("Updating quantity for product {} to {} - userId: {}, sessionId: {}",
                productId, quantity, userId, sessionId);

        Optional<Cart> cartOptional = findCart(userId, sessionId);
        if (cartOptional.isEmpty()) {
            log.info("No cart exists - nothing to update, returning empty cart");
            return buildEmptyCart(userId, sessionId);
        }

        Cart cart = cartOptional.get();
        CartItem item = findItem(cart, productId);

        if (quantity == 0) {
            // quantity 0 means "remove this line" - same as removeItem
            cart.getItems().remove(item);
            log.info("Quantity 0 - removed product {} from cart", productId);
        } else {
            item.setQuantity(quantity);
            log.info("Quantity for product {} set to {}", productId, quantity);
        }

        Cart savedCart = cartRepository.save(cart);
        log.info("Cart saved - cartId: {}, items: {}", savedCart.getId(), savedCart.getItems().size());
        cacheService.evict(userId, sessionId);
        return savedCart;
    }

    @Override
    public Cart removeItem(Long userId, String sessionId, Long productId) {
        log.info("Removing product {} from cart - userId: {}, sessionId: {}",
                productId, userId, sessionId);

        Optional<Cart> cartOptional = findCart(userId, sessionId);
        if (cartOptional.isEmpty()) {
            // idempotent: cart without this item is already the desired end state
            log.info("No cart exists - nothing to remove, returning empty cart");
            return buildEmptyCart(userId, sessionId);
        }

        Cart cart = cartOptional.get();
        CartItem item = findItem(cart, productId);
        cart.getItems().remove(item);

        Cart savedCart = cartRepository.save(cart);
        log.info("Product {} removed - cartId: {}, items: {}",
                productId, savedCart.getId(), savedCart.getItems().size());
        cacheService.evict(userId, sessionId);
        return savedCart;
    }

    @Override
    public void clearCart(Long userId, String sessionId) {
        log.info("Clearing cart - userId: {}, sessionId: {}", userId, sessionId);

        Optional<Cart> cartOptional = findCart(userId, sessionId);
        if (cartOptional.isEmpty()) {
            // idempotent: no cart = already empty
            log.info("No cart exists - nothing to clear");
            return;
        }

        Cart cart = cartOptional.get();
        cart.getItems().clear();

        cartRepository.save(cart);
        log.info("Cart cleared - shell kept, cartId: {}", cart.getId());
        cacheService.evict(userId, sessionId);
    }

    @Override
    public Cart mergeGuestCart(Long userId, String sessionId) {
        log.info("Merging guest cart into user cart - userId: {}, sessionId: {}", userId, sessionId);

        // no guest cart: nothing to merge, return the user's cart as-is
        Optional<Cart> guestCartOptional = cartRepository.findBySessionId(sessionId);
        if (guestCartOptional.isEmpty()) {
            log.info("No guest cart for sessionId {} - nothing to merge", sessionId);
            return getCart(userId, null);
        }

        Cart guestCart = guestCartOptional.get();

        // no user cart yet: adopt the guest cart instead of copying it
        Optional<Cart> userCartOptional = cartRepository.findByUserId(userId);
        if (userCartOptional.isEmpty()) {
            guestCart.setUserId(userId);
            guestCart.setSessionId(null);
            Cart savedCart = cartRepository.save(guestCart);
            log.info("Guest cart adopted as user cart - cartId: {}, items: {}",
                    savedCart.getId(), savedCart.getItems().size());

            // guest key (cart re-homed) and user key (cart now exists) both stale
            cacheService.evict(null, sessionId);
            cacheService.evict(userId, null);
            return savedCart;
        }

        // both exist: merge item lines - duplicates sum quantities, new lines append
        Cart userCart = userCartOptional.get();
        for (CartItem guestItem : guestCart.getItems()) {
            userCart.getItems().stream()
                    .filter(item -> item.getProductId().equals(guestItem.getProductId()))
                    .findFirst()
                    .ifPresentOrElse(
                            item -> item.setQuantity(item.getQuantity() + guestItem.getQuantity()),
                            () -> userCart.getItems().add(guestItem));
        }

        Cart savedCart = cartRepository.save(userCart);
        cartRepository.delete(guestCart);
        log.info("Guest cart merged and deleted - cartId: {}, items: {}, total: {}",
                savedCart.getId(), savedCart.getItems().size(), savedCart.getTotalPrice());

        // user cart changed AND guest cart is gone - both keys must go
        cacheService.evict(userId, null);
        cacheService.evict(null, sessionId);
        return savedCart;
    }

    private void publishCartItemAddedEvent(Cart cart, Long productId) {
        try {
            cart.getItems().stream()
                    .filter(item -> item.getProductId().equals(productId))
                    .findFirst()
                    .ifPresent(item -> {
                        CartItemAddedEvent event = CartItemAddedEvent.builder()
                                .cartId(cart.getId())
                                .userId(cart.getUserId())
                                .productId(item.getProductId())
                                .productName(item.getName())
                                .quantity(item.getQuantity())
                                .unitPrice(item.getUnitPrice())
                                .addedAt(LocalDateTime.now())
                                .build();
                        kafkaTemplate.send(cartItemAddedTopic, event);
                        log.info("Published cart.item.added event - cartId: {}, productId: {}",
                                cart.getId(), item.getProductId());
                    });
        } catch (Exception e) {
            // publishing must never break the add-to-cart flow - analytics is best-effort
            log.error("Failed to publish cart.item.added event - cartId: {}, productId: {}",
                    cart.getId(), productId, e);
        }
    }

    private CartItem findItem(Cart cart, Long productId) {
        return cart.getItems().stream()
                .filter(item -> item.getProductId().equals(productId))
                .findFirst()
                .orElseThrow(() -> {
                    log.warn("Product {} not found in cart", productId);
                    return new CartItemNotFoundException(
                            "Product " + productId + " is not in the cart");
                });
    }

    private Optional<Cart> findCart(Long userId, String sessionId) {
        if (userId != null) {
            return cartRepository.findByUserId(userId);
        }
        if (sessionId != null) {
            return cartRepository.findBySessionId(sessionId);
        }
        log.warn("No identity provided - userId and sessionId both null");
        return Optional.empty();
    }

    private Cart buildEmptyCart(Long userId, String sessionId) {
        log.info("No cart found for userId: {}, sessionId: {} - returning empty cart (not persisted)",
                userId, sessionId);
        return Cart.builder()
                .userId(userId)
                .sessionId(sessionId)
                .build();
    }
}
