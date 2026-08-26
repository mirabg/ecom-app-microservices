package com.ecom.orders.controller;

import com.ecom.orders.dto.CartItemRequest;
import com.ecom.orders.dto.CartItemResponse;
import com.ecom.orders.service.CartService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/cart")
public class CartController {
    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @PostMapping
    public ResponseEntity<String> addToCart(@RequestHeader("X-User-ID") String userId, @RequestBody CartItemRequest request){
        Optional<String> parsedUserId = parseUserId(userId);
        if (parsedUserId.isEmpty()) {
            return ResponseEntity.badRequest().body("Invalid X-User-ID header");
        }

        boolean saved = cartService.addToCart(parsedUserId.get(), request);
        if(!saved) {
            return ResponseEntity.badRequest().body("Error adding item to cart.");
        }else {
            return ResponseEntity.status(HttpStatus.CREATED).build();
        }
    }

    @DeleteMapping("/items/{productId}")
    public ResponseEntity<Void> removeFromCart(@RequestHeader("X-User-ID") String userId, @PathVariable Long productId){
        Optional<String> parsedUserId = parseUserId(userId);
        if (parsedUserId.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        boolean removed = cartService.removeFromCart(parsedUserId.get(), productId);
        if(!removed) {
            return ResponseEntity.badRequest().build();
        }else {
            return ResponseEntity.noContent().build();
        }
    }

    @GetMapping
    public ResponseEntity<List<CartItemResponse>> getCartItems(@RequestHeader("X-User-ID") String userId) {
        Optional<String> parsedUserId = parseUserId(userId);
        if (parsedUserId.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        Optional<List<CartItemResponse>> cartItems = cartService.getCartItems(parsedUserId.get());
        return cartItems
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    private Optional<String> parseUserId(String userId) {
        if (userId == null) {
            return Optional.empty();
        }
        String normalized = userId.trim();
        return normalized.isEmpty() ? Optional.empty() : Optional.of(normalized);
    }


}
