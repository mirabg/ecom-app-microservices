package com.ecom.orders.dto;


import lombok.Data;

@Data
public class CartItemRequest {
    private Long productId;
    private Integer quantity;
}
