package com.app.ecom.controller;

import com.app.ecom.model.CartItem;
import com.app.ecom.model.Product;
import com.app.ecom.model.User;
import com.app.ecom.repository.CartItemRepository;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CartControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CartItemRepository cartItemRepository;

    @Test
    void addToCartReturnsCreatedAndPersistsCartItem() throws Exception {
        User user = createUser("jane@example.com");
        Product product = createProduct("Wireless Mouse", "49.99", 10);

        mockMvc.perform(post("/api/cart")
                        .header("X-User-ID", user.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cartItemJson(product.getId(), 2)))
                .andExpect(status().isCreated());

        CartItem cartItem = cartItemRepository.findByUserAndProduct(user, product);
        assertNotNull(cartItem);
        assertEquals(2, cartItem.getQuantity());
        assertEquals(new BigDecimal("99.98"), cartItem.getPrice());
    }

    @Test
    void addToCartMergesWithExistingCartItem() throws Exception {
        User user = createUser("alex@example.com");
        Product product = createProduct("Keyboard", "25.00", 10);

        mockMvc.perform(post("/api/cart")
                        .header("X-User-ID", user.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cartItemJson(product.getId(), 2)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/cart")
                        .header("X-User-ID", user.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cartItemJson(product.getId(), 3)))
                .andExpect(status().isCreated());

        CartItem cartItem = cartItemRepository.findByUserAndProduct(user, product);
        assertNotNull(cartItem);
        assertEquals(5, cartItem.getQuantity());
        assertEquals(new BigDecimal("125.00"), cartItem.getPrice());
        assertEquals(1, cartItemRepository.findAll().size());
    }

    @Test
    void addToCartReturnsBadRequestWhenProductMissing() throws Exception {
        User user = createUser("missing-product@example.com");

        mockMvc.perform(post("/api/cart")
                        .header("X-User-ID", user.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cartItemJson(999999L, 1)))
                .andExpect(status().isBadRequest());

        assertEquals(0, cartItemRepository.count());
    }

    @Test
    void addToCartReturnsBadRequestWhenUserMissing() throws Exception {
        Product product = createProduct("Desk Lamp", "19.99", 8);

        mockMvc.perform(post("/api/cart")
                        .header("X-User-ID", "999999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cartItemJson(product.getId(), 1)))
                .andExpect(status().isBadRequest());

        assertEquals(0, cartItemRepository.count());
    }

    @Test
    void addToCartReturnsBadRequestWhenUserHeaderMalformed() throws Exception {
        Product product = createProduct("USB Hub", "39.99", 8);

        mockMvc.perform(post("/api/cart")
                        .header("X-User-ID", "not-a-number")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cartItemJson(product.getId(), 1)))
                .andExpect(status().isBadRequest());

        assertEquals(0, cartItemRepository.count());
    }

    @Test
    void addToCartReturnsBadRequestWhenRequestedQuantityExceedsStock() throws Exception {
        User user = createUser("stock@example.com");
        Product product = createProduct("Webcam", "89.99", 1);

        mockMvc.perform(post("/api/cart")
                        .header("X-User-ID", user.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cartItemJson(product.getId(), 2)))
                .andExpect(status().isBadRequest());

        CartItem cartItem = cartItemRepository.findByUserAndProduct(user, product);
        assertNull(cartItem);
    }

    @Test
    void removeFromCartReturnsNoContentAndDeletesCartItem() throws Exception {
        User user = createUser("delete-success@example.com");
        Product product = createProduct("Speaker", "59.99", 5);

        mockMvc.perform(post("/api/cart")
                        .header("X-User-ID", user.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cartItemJson(product.getId(), 2)))
                .andExpect(status().isCreated());

        CartItem beforeDelete = cartItemRepository.findByUserAndProduct(user, product);
        assertNotNull(beforeDelete);

        mockMvc.perform(delete("/api/cart/items/{productId}", product.getId())
                        .header("X-User-ID", user.getId().toString()))
                .andExpect(status().isNoContent());

        CartItem afterDelete = cartItemRepository.findByUserAndProduct(user, product);
        assertNull(afterDelete);
    }

    @Test
    void removeFromCartReturnsBadRequestWhenProductMissing() throws Exception {
        User user = createUser("delete-missing-product@example.com");

        mockMvc.perform(delete("/api/cart/items/{productId}", 999999L)
                        .header("X-User-ID", user.getId().toString()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void removeFromCartReturnsBadRequestWhenUserMissing() throws Exception {
        Product product = createProduct("Mouse", "29.99", 6);

        mockMvc.perform(delete("/api/cart/items/{productId}", product.getId())
                        .header("X-User-ID", "999999"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void removeFromCartReturnsBadRequestWhenUserHeaderMalformed() throws Exception {
        Product product = createProduct("Dock", "129.99", 2);

        mockMvc.perform(delete("/api/cart/items/{productId}", product.getId())
                        .header("X-User-ID", "invalid-user-id"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getCartItemsReturnsItemsForUser() throws Exception {
        User user = createUser("get-items@example.com");
        Product product1 = createProduct("Monitor", "199.99", 3);
        Product product2 = createProduct("Keyboard", "49.99", 5);

        mockMvc.perform(post("/api/cart")
                        .header("X-User-ID", user.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cartItemJson(product1.getId(), 1)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/cart")
                        .header("X-User-ID", user.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cartItemJson(product2.getId(), 2)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/cart")
                        .header("X-User-ID", user.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[*].productId", contains(product1.getId().intValue(), product2.getId().intValue())))
                .andExpect(jsonPath("$[*].productName", contains("Monitor", "Keyboard")))
                .andExpect(jsonPath("$[*].quantity", contains(1, 2)));
    }

    @Test
    void getCartItemsReturnsEmptyForUserWithNoItems() throws Exception {
        User user = createUser("empty-cart@example.com");

        mockMvc.perform(get("/api/cart")
                        .header("X-User-ID", user.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void getCartItemsReturnsEmptyForUnknownUser() throws Exception {
        mockMvc.perform(get("/api/cart")
                        .header("X-User-ID", "999999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getCartItemsReturnsBadRequestForMalformedUserHeader() throws Exception {
        mockMvc.perform(get("/api/cart")
                        .header("X-User-ID", "abc"))
                .andExpect(status().isBadRequest());
    }

    private User createUser(String email) {
        User user = new User();
        user.setFirstName("Test");
        user.setLastName("User");
        user.setEmail(email);
        user.setPhone("5551234567");
        return userRepository.save(user);
    }

    private Product createProduct(String name, String price, int stockQuantity) {
        Product product = new Product();
        product.setName(name);
        product.setDescription(name + " description");
        product.setPrice(new BigDecimal(price));
        product.setStockQuantity(stockQuantity);
        product.setCategory("Accessories");
        product.setImageUrl("https://cdn.example.com/" + name.toLowerCase().replace(" ", "-") + ".png");
        return productRepository.save(product);
    }

    private String cartItemJson(Long productId, int quantity) {
        return "{" +
                "\"productId\":" + productId + "," +
                "\"quantity\":" + quantity +
                "}";
    }
}

