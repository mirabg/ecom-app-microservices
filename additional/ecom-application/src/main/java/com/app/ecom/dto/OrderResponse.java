package com.app.ecom.dto;

import com.app.ecom.model.OrderItem;
import com.app.ecom.model.OrderStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class OrderResponse {
    private Long id;
    private BigDecimal totalAmount;
    private OrderStatus status;
    private List<OrderItemDTO> orderItems;
    private LocalDateTime createdAt;
}
