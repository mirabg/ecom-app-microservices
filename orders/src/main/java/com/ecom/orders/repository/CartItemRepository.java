package com.ecom.orders.repository;

import com.ecom.orders.model.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {
    List<CartItem> findByUserId(String userId);

    CartItem findByUserIdAndProductId(String userId, Long productId);

    void deleteByUserId(String userId);

    void deleteByUserIdAndProductId(String userId, Long productId);
}
