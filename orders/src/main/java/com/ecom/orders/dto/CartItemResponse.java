package com.ecom.orders.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class CartItemResponse {
    private Long id;
    private Long productId;
    private Integer quantity;
    private BigDecimal price;
    private LocalDateTime createdAt;
}
