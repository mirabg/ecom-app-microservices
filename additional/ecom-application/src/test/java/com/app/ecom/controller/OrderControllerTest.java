package com.app.ecom.controller;

import com.app.ecom.model.Order;
import com.app.ecom.model.Product;
import com.app.ecom.model.User;
import com.app.ecom.repository.CartItemRepository;
import com.app.ecom.repository.OrderRepository;
import com.app.ecom.repository.ProductRepository;
import com.app.ecom.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CartItemRepository cartItemRepository;

    @Autowired
    private OrderRepository orderRepository;

    // -----------------------------------------------------------------------
    // createOrder
    // -----------------------------------------------------------------------

    @Test
    void createOrderReturnsOkAndPersistsOrder() throws Exception {
        User user = createUser("order-success@example.com");
        Product p1 = createProduct("Laptop", "999.99", 5);
        Product p2 = createProduct("Bag",    "49.99",  10);

        addToCart(user, p1, 1);
        addToCart(user, p2, 2);

        mockMvc.perform(post("/api/orders")
                        .header("X-User-ID", user.getId().toString()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.totalAmount").value(1099.97))
                .andExpect(jsonPath("$.orderItems.length()").value(2))
                .andExpect(jsonPath("$.createdAt").isNotEmpty());

        List<Order> orders = orderRepository.findAll();
        assertEquals(1, orders.size());
        assertEquals(new BigDecimal("1099.97"), orders.get(0).getTotalAmount());
        assertEquals(2, orders.get(0).getItems().size());
    }

    @Test
    void createOrderClearsCartAfterCreation() throws Exception {
        User user = createUser("order-clear-cart@example.com");
        Product product = createProduct("Monitor", "299.99", 3);

        addToCart(user, product, 1);
        assertEquals(1, cartItemRepository.count());

        mockMvc.perform(post("/api/orders")
                        .header("X-User-ID", user.getId().toString()))
                .andExpect(status().isCreated());

        assertEquals(0, cartItemRepository.count());
    }

    @Test
    void createOrderReturnsOrderItemDetailsMatchingCartContents() throws Exception {
        User user = createUser("order-items@example.com");
        Product product = createProduct("Keyboard", "79.99", 8);

        addToCart(user, product, 3);

        mockMvc.perform(post("/api/orders")
                        .header("X-User-ID", user.getId().toString()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderItems[0].productId").value(product.getId().intValue()))
                .andExpect(jsonPath("$.orderItems[0].quantity").value(3))
                .andExpect(jsonPath("$.orderItems[0].price").value(239.97));
    }

    @Test
    void createOrderReturnsBadRequestWhenCartIsEmpty() throws Exception {
        User user = createUser("empty-cart-order@example.com");

        mockMvc.perform(post("/api/orders")
                        .header("X-User-ID", user.getId().toString()))
                .andExpect(status().isBadRequest());

        assertEquals(0, orderRepository.count());
    }

    @Test
    void createOrderReturnsBadRequestWhenUserDoesNotExist() throws Exception {
        mockMvc.perform(post("/api/orders")
                        .header("X-User-ID", "999999"))
                .andExpect(status().isBadRequest());

        assertEquals(0, orderRepository.count());
    }

    @Test
    void createOrderReturnsBadRequestWhenUserHeaderIsMalformed() throws Exception {
        mockMvc.perform(post("/api/orders")
                        .header("X-User-ID", "not-a-number"))
                .andExpect(status().isBadRequest());

        assertEquals(0, orderRepository.count());
    }

    @Test
    void createOrderDoesNotPersistWhenUserMissing() throws Exception {
        // Confirm no side-effects when the request fails
        mockMvc.perform(post("/api/orders")
                        .header("X-User-ID", "888888"))
                .andExpect(status().isBadRequest());

        assertTrue(orderRepository.findAll().isEmpty());
    }

    // -----------------------------------------------------------------------
    // helpers
    // -----------------------------------------------------------------------

    private User createUser(String email) {
        User user = new User();
        user.setFirstName("Test");
        user.setLastName("User");
        user.setEmail(email);
        user.setPhone("5551234567");
        return userRepository.save(user);
    }

    private Product createProduct(String name, String price, int stock) {
        Product product = new Product();
        product.setName(name);
        product.setDescription(name + " description");
        product.setPrice(new BigDecimal(price));
        product.setStockQuantity(stock);
        product.setCategory("Electronics");
        product.setImageUrl("https://cdn.example.com/" + name.toLowerCase().replace(" ", "-") + ".png");
        return productRepository.save(product);
    }

    private void addToCart(User user, Product product, int quantity) throws Exception {
        mockMvc.perform(post("/api/cart")
                        .header("X-User-ID", user.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":" + product.getId() + ",\"quantity\":" + quantity + "}"))
                .andExpect(status().isCreated());
    }
}

