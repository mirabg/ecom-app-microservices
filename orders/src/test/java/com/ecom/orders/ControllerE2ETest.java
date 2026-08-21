package com.ecom.orders;

import com.ecom.orders.client.ProductServiceClient;
import com.ecom.orders.client.UserServiceClient;
import com.ecom.orders.dto.ProductResponse;
import com.ecom.orders.dto.UserResponse;
import com.ecom.orders.repository.CartItemRepository;
import com.ecom.orders.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;


import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ControllerE2ETest {
    private static final String CART_USER_ID = "507f1f77bcf86cd799439011";
    private static final String ORDER_USER_ID = "507f1f77bcf86cd799439017";
    private static final String UNKNOWN_USER_ID = "missing-user";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CartItemRepository cartItemRepository;

    @Autowired
    private OrderRepository orderRepository;

    @MockitoBean
    private ProductServiceClient productServiceClient;

    @MockitoBean
    private UserServiceClient userServiceClient;

    @BeforeEach
    void setUp() {
        orderRepository.deleteAll();
        cartItemRepository.deleteAll();

        when(productServiceClient.getProductById(anyLong())).thenAnswer(invocation -> {
            Long id = invocation.getArgument(0);
            ProductResponse response = new ProductResponse();
            response.setId(id);
            response.setName("Test Product " + id);
            response.setPrice(BigDecimal.TEN);
            response.setStockQuantity(100);
            response.setActive(true);
            return response;
        });

        when(userServiceClient.getUserById(anyString())).thenAnswer(invocation -> {
            String id = invocation.getArgument(0);
            if (!CART_USER_ID.equals(id) && !ORDER_USER_ID.equals(id)) {
                return null;
            }

            UserResponse response = new UserResponse();
            response.setId(id);
            response.setFirstName("Test");
            response.setLastName("User");
            return response;
        });
    }

    @Test
    void addToCartAndGetCartItems() throws Exception {
        mockMvc.perform(post("/api/cart")
                        .header("X-User-ID", CART_USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "productId": 101,
                                  "quantity": 2
                                }
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/cart")
                        .header("X-User-ID", CART_USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].productId").value(101))
                .andExpect(jsonPath("$[0].quantity").value(2))
                .andExpect(jsonPath("$[0].price").value(20.00));
    }

    @Test
    void addSameProductTwiceAccumulatesQuantityAndPrice() throws Exception {
        mockMvc.perform(post("/api/cart")
                        .header("X-User-ID", CART_USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "productId": 101,
                                  "quantity": 2
                                }
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/cart")
                        .header("X-User-ID", CART_USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "productId": 101,
                                  "quantity": 3
                                }
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/cart")
                        .header("X-User-ID", CART_USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].productId").value(101))
                .andExpect(jsonPath("$[0].quantity").value(5))
                .andExpect(jsonPath("$[0].price").value(50.00));
    }

    @Test
    void removeFromCartDeletesSpecificItem() throws Exception {
        mockMvc.perform(post("/api/cart")
                        .header("X-User-ID", CART_USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "productId": 101,
                                  "quantity": 1
                                }
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(delete("/api/cart/items/{productId}", 101)
                        .header("X-User-ID", CART_USER_ID))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/cart")
                        .header("X-User-ID", CART_USER_ID))
                .andExpect(status().isNotFound());
    }

    @Test
    void createOrderFromCartReturnsCreatedOrderAndClearsCart() throws Exception {
        mockMvc.perform(post("/api/cart")
                        .header("X-User-ID", ORDER_USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "productId": 201,
                                  "quantity": 2
                                }
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/cart")
                        .header("X-User-ID", ORDER_USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "productId": 202,
                                  "quantity": 1
                                }
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/orders")
                        .header("X-User-ID", ORDER_USER_ID))
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
                        .header("X-User-ID", ORDER_USER_ID))
                .andExpect(status().isNotFound());
    }

    @Test
    void unknownUserIdReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/cart")
                        .header("X-User-ID", UNKNOWN_USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "productId": 101,
                                  "quantity": 1
                                }
                                """))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/orders")
                        .header("X-User-ID", UNKNOWN_USER_ID))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createOrderWithEmptyCartReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/orders")
                        .header("X-User-ID", CART_USER_ID))
                .andExpect(status().isBadRequest());
    }
}

