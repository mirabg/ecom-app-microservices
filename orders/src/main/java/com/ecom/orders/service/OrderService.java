package com.ecom.orders.service;


import com.ecom.orders.dto.CartItemResponse;
import com.ecom.orders.dto.OrderItemDTO;
import com.ecom.orders.dto.OrderResponse;
import com.ecom.orders.model.Order;
import com.ecom.orders.model.OrderItem;
import com.ecom.orders.repository.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class OrderService {
    private final CartService cartService;
    private final OrderRepository orderRepository;

    public OrderService(CartService cartService, OrderRepository orderRepository) {
        this.cartService = cartService;
        this.orderRepository = orderRepository;
    }

    @Transactional
    public Optional<OrderResponse> createOrder(Long userId) {
        Optional<List<CartItemResponse>> cartItemsOpt = cartService.getCartItems(userId);
        if (cartItemsOpt.isEmpty()) {
            return Optional.empty();
        }

        List<CartItemResponse> cartItems = cartItemsOpt.get();

        // Calc total
        BigDecimal totalPrice = cartItems.stream()
                .map(CartItemResponse::getPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Create order with items
        Order order = new Order();

        order.setUserId(userId);
        order.setTotalAmount(totalPrice);

        List<OrderItem> orderItems = cartItems.stream()
                .map(item -> {
                    OrderItem oi = new OrderItem();
                    oi.setQuantity(item.getQuantity());
                    oi.setPrice(item.getPrice());
                    oi.setProductId(item.getProductId());
                    oi.setOrder(order);
                    return oi;
                })
                .toList();

        order.setItems(orderItems);
        Order savedOrder = orderRepository.save(order);

        // Clear cart
        cartService.clearCartItems(userId);

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
                    dto.setProductId(item.getProductId());
                    dto.setQuantity(item.getQuantity());
                    dto.setPrice(item.getPrice());
                    return dto;
                })
                .toList();
        response.setOrderItems(itemDTOs);
        return response;
    }
}
