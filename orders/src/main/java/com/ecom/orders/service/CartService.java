package com.ecom.orders.service;


import com.ecom.orders.client.ProductServiceClient;
import com.ecom.orders.client.UserServiceClient;
import com.ecom.orders.dto.CartItemRequest;
import com.ecom.orders.dto.CartItemResponse;
import com.ecom.orders.dto.ProductResponse;
import com.ecom.orders.dto.UserResponse;
import com.ecom.orders.model.CartItem;
import com.ecom.orders.repository.CartItemRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@Transactional(readOnly = true)
public class CartService {
    private final ProductServiceClient productServiceClient;
    private final UserServiceClient userServiceClient;

    private final CartItemRepository cartItemRepository;

    public CartService(ProductServiceClient productServiceClient, UserServiceClient userServiceClient, CartItemRepository cartItemRepository) {
        this.productServiceClient = productServiceClient;
        this.userServiceClient = userServiceClient;
        this.cartItemRepository = cartItemRepository;
    }

    public boolean addToCartFallback(String userId, CartItemRequest request, Throwable throwable) {
        log.warn("Fallback method called for addToCart due to: {}", throwable.getMessage());
        return false;
    }

    @Transactional
    @CircuitBreaker(name ="orderServiceCircuitBreaker", fallbackMethod = "addToCartFallback")
    public boolean addToCart(String userId, CartItemRequest request) {
        if (userId == null || userId.isBlank()) {
            return false;
        }

        ProductResponse productResponse = productServiceClient.getProductById(request.getProductId());
        if(productResponse == null || productResponse.getStockQuantity() == null || productResponse.getPrice() == null) {
            return false;
        }

        if(productResponse.getStockQuantity() < request.getQuantity()) {
            return false;
        }

        UserResponse userResponse = userServiceClient.getUserById(userId);
        if (userResponse == null){
            return false;
        }

        CartItem existingCartItem = cartItemRepository.findByUserIdAndProductId(userId, request.getProductId());

        if (existingCartItem != null) {
            existingCartItem.setQuantity(existingCartItem.getQuantity() + request.getQuantity());
            existingCartItem.setPrice(existingCartItem.getPrice().add(productResponse.getPrice().multiply(BigDecimal.valueOf(request.getQuantity()))));
            cartItemRepository.save(existingCartItem);
        } else {
            CartItem cartItem = new CartItem();
            cartItem.setUserId(userId);
            cartItem.setProductId(request.getProductId());
            cartItem.setQuantity(request.getQuantity());
            cartItem.setPrice(productResponse.getPrice().multiply(BigDecimal.valueOf(request.getQuantity())));
            cartItemRepository.save(cartItem);
        }

        return true;
    }

    @Transactional
    public boolean removeFromCart(String userId, Long productId) {
        if (userId == null || userId.isBlank() || productId == null) {
            return false;
        }

        CartItem existingCartItem = cartItemRepository.findByUserIdAndProductId(userId, productId);
        if (existingCartItem == null) {
            return false;
        }

        cartItemRepository.deleteByUserIdAndProductId(userId, productId);

        return true;
    }

    
    @Transactional
    public void clearCartItems(String userId) {
        cartItemRepository.deleteByUserId(userId);
    }

    public Optional<List<CartItemResponse>> getCartItems(String userId) {
        if (userId == null || userId.isBlank()) {
            return Optional.empty();
        }

        List<CartItem> items = cartItemRepository.findByUserId(userId);
        if (items.isEmpty()) {
            return Optional.empty();
        }

        List<CartItemResponse> responses = items.stream().map(this::toResponse).toList();
        return Optional.of(responses);
    }

    private CartItemResponse toResponse(CartItem item) {
        CartItemResponse response = new CartItemResponse();
        response.setId(item.getId());
        response.setProductId(item.getProductId());
        response.setQuantity(item.getQuantity());
        response.setPrice(item.getPrice());
        response.setCreatedAt(item.getCreatedAt());
        return response;
    }
}
