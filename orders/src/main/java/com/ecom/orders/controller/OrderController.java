package com.ecom.orders.controller;

import com.ecom.orders.dto.OrderResponse;
import com.ecom.orders.service.OrderService;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
@RequestMapping("/api/orders")
public class OrderController {
    private final OrderService orderService;
    private final RabbitTemplate rabbitTemplate;

    public OrderController(OrderService orderService, RabbitTemplate rabbitTemplate) {
        this.orderService = orderService;
        this.rabbitTemplate = rabbitTemplate;
    }

    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(@RequestHeader("X-User-ID") String userId) {
        Optional<String> parsedUserId = parseUserId(userId);
        if (parsedUserId.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        return orderService.createOrder(parsedUserId.get())
                .map(orderResponse -> new ResponseEntity<>(orderResponse, HttpStatus.CREATED))
                .orElseGet(() -> ResponseEntity.badRequest().build());
    }

    @GetMapping("/test-order-notification")
    public String testOrderNotification() {
        // Uses the exchange/routing key already configured on rabbitTemplate in RabbitMqConfig
        rabbitTemplate.convertAndSend("Test order notification message");
        return "testOrderNotification";
    }

    private Optional<String> parseUserId(String userId) {
        if (userId == null) {
            return Optional.empty();
        }
        String normalized = userId.trim();
        return normalized.isEmpty() ? Optional.empty() : Optional.of(normalized);
    }
}
