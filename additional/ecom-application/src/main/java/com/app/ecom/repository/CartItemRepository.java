package com.app.ecom.repository;

import com.app.ecom.model.CartItem;
import com.app.ecom.model.Product;
import com.app.ecom.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {
    CartItem findByUserAndProduct(User user, Product product);

    List<CartItem> findByUserOrderByCreatedAtAscIdAsc(User user);

    void deleteByUserAndProduct(User user, Product product);

    void deleteByUser(User user);
}
