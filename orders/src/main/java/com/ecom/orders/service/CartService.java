package com.ecom.orders.service;


import com.ecom.orders.dto.CartItemRequest;
import com.ecom.orders.dto.CartItemResponse;
import com.ecom.orders.model.CartItem;
import com.ecom.orders.repository.CartItemRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class CartService {
    private static final BigDecimal HARDCODED_UNIT_PRICE = new BigDecimal("10.00");

    private final CartItemRepository cartItemRepository;

    public CartService(CartItemRepository cartItemRepository) {
        this.cartItemRepository = cartItemRepository;
    }

    @Transactional
    public boolean addToCart(Long userId, CartItemRequest request) {
        if (userId == null || request == null || request.getProductId() == null || request.getQuantity() == null || request.getQuantity() <= 0) {
            return false;
        }

        CartItem existingCartItem = cartItemRepository.findByUserIdAndProductId(userId, request.getProductId());

        if (existingCartItem != null) {
            existingCartItem.setQuantity(existingCartItem.getQuantity() + request.getQuantity());
            existingCartItem.setPrice(existingCartItem.getPrice().add(HARDCODED_UNIT_PRICE.multiply(BigDecimal.valueOf(request.getQuantity()))));
            cartItemRepository.save(existingCartItem);
        } else {
            CartItem cartItem = new CartItem();
            cartItem.setUserId(userId);
            cartItem.setProductId(request.getProductId());
            cartItem.setQuantity(request.getQuantity());
            cartItem.setPrice(HARDCODED_UNIT_PRICE.multiply(BigDecimal.valueOf(request.getQuantity())));
            cartItemRepository.save(cartItem);
        }

        return true;
    }

    @Transactional
    public boolean removeFromCart(Long userId, Long productId) {
        if (userId == null || productId == null) {
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
    public void clearCartItems(Long userId) {
        cartItemRepository.deleteByUserId(userId);
    }

    public Optional<List<CartItemResponse>> getCartItems(Long userId) {
        if (userId == null) {
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
