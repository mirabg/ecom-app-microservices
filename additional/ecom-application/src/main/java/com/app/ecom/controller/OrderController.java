package com.app.ecom.controller;

import com.app.ecom.dto.OrderResponse;
import com.app.ecom.service.OrderService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders")
public class OrderController {
    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(@RequestHeader("X-User-ID") String userId) {
        long id;
        try {
            id = Long.parseLong(userId);
        } catch (NumberFormatException e) {
            return ResponseEntity.badRequest().build();
        }
        return orderService.createOrder(id)
                .map(orderResponse -> new ResponseEntity<>(orderResponse, HttpStatus.CREATED))
                .orElseGet(() -> ResponseEntity.badRequest().build());
    }
}
