package com.ecom.orders;

import com.ecom.orders.repository.CartItemRepository;
import com.ecom.orders.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;


import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ControllerE2ETest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CartItemRepository cartItemRepository;

    @Autowired
    private OrderRepository orderRepository;

    @BeforeEach
    void setUp() {
        orderRepository.deleteAll();
        cartItemRepository.deleteAll();
    }

    @Test
    void addToCartAndGetCartItems() throws Exception {
        mockMvc.perform(post("/api/cart")
                        .header("X-User-ID", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "productId": 101,
                                  "quantity": 2
                                }
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/cart")
                        .header("X-User-ID", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].productId").value(101))
                .andExpect(jsonPath("$[0].quantity").value(2))
                .andExpect(jsonPath("$[0].price").value(20.00));
    }

    @Test
    void addSameProductTwiceAccumulatesQuantityAndPrice() throws Exception {
        mockMvc.perform(post("/api/cart")
                        .header("X-User-ID", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "productId": 101,
                                  "quantity": 2
                                }
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/cart")
                        .header("X-User-ID", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "productId": 101,
                                  "quantity": 3
                                }
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/cart")
                        .header("X-User-ID", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].productId").value(101))
                .andExpect(jsonPath("$[0].quantity").value(5))
                .andExpect(jsonPath("$[0].price").value(50.00));
    }

    @Test
    void removeFromCartDeletesSpecificItem() throws Exception {
        mockMvc.perform(post("/api/cart")
                        .header("X-User-ID", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "productId": 101,
                                  "quantity": 1
                                }
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(delete("/api/cart/items/{productId}", 101)
                        .header("X-User-ID", "1"))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/cart")
                        .header("X-User-ID", "1"))
                .andExpect(status().isNotFound());
    }

    @Test
    void createOrderFromCartReturnsCreatedOrderAndClearsCart() throws Exception {
        mockMvc.perform(post("/api/cart")
                        .header("X-User-ID", "7")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "productId": 201,
                                  "quantity": 2
                                }
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/cart")
                        .header("X-User-ID", "7")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "productId": 202,
                                  "quantity": 1
                                }
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/orders")
                        .header("X-User-ID", "7"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.totalAmount").value(30.00))
                .andExpect(jsonPath("$.orderItems", hasSize(2)))
                .andExpect(jsonPath("$.orderItems[0].productId").value(201))
                .andExpect(jsonPath("$.orderItems[0].quantity").value(2))
                .andExpect(jsonPath("$.orderItems[0].price").value(20.00))
                .andExpect(jsonPath("$.orderItems[1].productId").value(202))
                .andExpect(jsonPath("$.orderItems[1].quantity").value(1))
                .andExpect(jsonPath("$.orderItems[1].price").value(10.00));

        mockMvc.perform(get("/api/cart")
                        .header("X-User-ID", "7"))
                .andExpect(status().isNotFound());
    }

    @Test
    void invalidUserHeaderReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/cart")
                        .header("X-User-ID", "abc")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "productId": 101,
                                  "quantity": 1
                                }
                                """))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/orders")
                        .header("X-User-ID", "abc"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createOrderWithEmptyCartReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/orders")
                        .header("X-User-ID", "99"))
                .andExpect(status().isBadRequest());
    }
}

