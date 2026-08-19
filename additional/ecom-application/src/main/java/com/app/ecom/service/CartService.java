package com.app.ecom.service;

import com.app.ecom.dto.CartItemRequest;
import com.app.ecom.dto.CartItemResponse;
import com.app.ecom.model.CartItem;
import com.app.ecom.model.Product;
import com.app.ecom.model.User;
import com.app.ecom.repository.CartItemRepository;
import com.app.ecom.repository.ProductRepository;
import com.app.ecom.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class CartService {
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final CartItemRepository cartItemRepository;

    public CartService(ProductRepository productRepository, UserRepository userRepository, CartItemRepository cartItemRepository) {
        this.productRepository = productRepository;
        this.userRepository = userRepository;
        this.cartItemRepository = cartItemRepository;
    }

    @Transactional
    public boolean addToCart(Long userId, CartItemRequest request) {
        Optional<Product> productOpt = productRepository.findById(request.getProductId());
        if (productOpt.isEmpty()) {
            return false;
        }
        Product product = productOpt.get();
        if(product.getStockQuantity() < request.getQuantity()) {
            return false;
        }
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            return false;
        }

        User user = userOpt.get();
        CartItem existingCartItem = cartItemRepository.findByUserAndProduct(user, product);

        if(existingCartItem != null) {
            existingCartItem.setQuantity(existingCartItem.getQuantity() + request.getQuantity());
            existingCartItem.setPrice(existingCartItem.getPrice().add(product.getPrice().multiply(BigDecimal.valueOf(request.getQuantity()))));
            cartItemRepository.save(existingCartItem);
        }else{
            //create new cart item
            CartItem cartItem = new CartItem();
            cartItem.setUser(user);
            cartItem.setProduct(product);
            cartItem.setQuantity(request.getQuantity());
            cartItem.setPrice(product.getPrice().multiply(BigDecimal.valueOf(request.getQuantity())));
            cartItemRepository.save(cartItem);
        }

        return true;
    }

    @Transactional
    public boolean removeFromCart(Long userId, Long productId) {
        Optional<Product> productOpt = productRepository.findById(productId);
        if (productOpt.isEmpty()) {
            return false;
        }
        Product product = productOpt.get();
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            return false;
        }

        cartItemRepository.deleteByUserAndProduct(userOpt.get(), product);

        return true;
    }

    public Optional<List<CartItem>> getCartItemEntitiesForUser(Long userId) {
        return userRepository.findById(userId)
                .map(user -> cartItemRepository.findByUserOrderByCreatedAtAscIdAsc(user));
    }

    @Transactional
    public void clearCartForUser(User user) {
        cartItemRepository.deleteByUser(user);
    }

    public Optional<List<CartItemResponse>> getCartItems(Long userId) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            return Optional.empty();
        }

        List<CartItemResponse> items = cartItemRepository.findByUserOrderByCreatedAtAscIdAsc(userOpt.get())
                .stream()
                .map(this::toResponse)
                .toList();
        return Optional.of(items);
    }

    private CartItemResponse toResponse(CartItem item) {
        CartItemResponse response = new CartItemResponse();
        response.setId(item.getId());
        response.setProductId(item.getProduct().getId());
        response.setProductName(item.getProduct().getName());
        response.setQuantity(item.getQuantity());
        response.setPrice(item.getPrice());
        response.setCreatedAt(item.getCreatedAt());
        return response;
    }
}
