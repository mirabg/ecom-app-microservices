package com.app.ecom.service;

import com.app.ecom.dto.OrderItemDTO;
import com.app.ecom.dto.OrderResponse;
import com.app.ecom.model.*;
import com.app.ecom.repository.OrderRepository;
import com.app.ecom.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class OrderService {
    private final CartService cartService;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;

    public OrderService(CartService cartService, UserRepository userRepository, OrderRepository orderRepository) {
        this.cartService = cartService;
        this.userRepository = userRepository;
        this.orderRepository = orderRepository;
    }

    @Transactional
    public Optional<OrderResponse> createOrder(Long userId) {
        // Validate user
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            return Optional.empty();
        }
        User user = userOpt.get();

        // Validate cart items
        Optional<List<CartItem>> cartItemsOpt = cartService.getCartItemEntitiesForUser(userId);
        if (cartItemsOpt.isEmpty() || cartItemsOpt.get().isEmpty()) {
            return Optional.empty();
        }
        List<CartItem> cartItems = cartItemsOpt.get();

        // Calc total
        BigDecimal totalPrice = cartItems.stream()
                .map(CartItem::getPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Create order with items
        Order order = new Order();
        order.setUser(user);
        order.setTotalAmount(totalPrice);

        List<OrderItem> orderItems = cartItems.stream()
                .map(item -> {
                    OrderItem oi = new OrderItem();
                    oi.setProduct(item.getProduct());
                    oi.setQuantity(item.getQuantity());
                    oi.setPrice(item.getPrice());
                    oi.setOrder(order);
                    return oi;
                })
                .toList();

        order.setItems(orderItems);
        Order savedOrder = orderRepository.save(order);

        // Clear cart
        cartService.clearCartForUser(user);

        return Optional.of(toResponse(savedOrder));
    }

    private OrderResponse toResponse(Order order) {
        OrderResponse response = new OrderResponse();
        response.setId(order.getId());
        response.setTotalAmount(order.getTotalAmount());
        response.setStatus(order.getOrderStatus());
        response.setCreatedAt(order.getCreatedAt());

        List<OrderItemDTO> itemDTOs = order.getItems().stream()
                .map(item -> {
                    OrderItemDTO dto = new OrderItemDTO();
                    dto.setId(item.getId());
                    dto.setProductId(item.getProduct().getId());
                    dto.setQuantity(item.getQuantity());
                    dto.setPrice(item.getPrice());
                    return dto;
                })
                .toList();
        response.setOrderItems(itemDTOs);
        return response;
    }
}
