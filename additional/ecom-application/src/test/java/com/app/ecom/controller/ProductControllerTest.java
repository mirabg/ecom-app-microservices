package com.app.ecom.controller;

import com.app.ecom.model.Product;
import com.app.ecom.repository.ProductRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProductRepository productRepository;

    @Test
    void getAllProductsReturnsOkWhenEmpty() throws Exception {
        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void getAllProductsReturnsCreatedProducts() throws Exception {
        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validProductJson("Monitor", "4K monitor", "399.99", 15, "Displays", "https://cdn.example.com/monitor.png")))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validProductJson("Desk Lamp", "LED desk lamp", "29.99", 60, "Lighting", "https://cdn.example.com/lamp.png")))
                .andExpect(status().isCreated());

        MvcResult listResult = mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andReturn();

        List<String> productNames = JsonPath.read(listResult.getResponse().getContentAsString(), "$[*].name");
        List<String> productCategories = JsonPath.read(listResult.getResponse().getContentAsString(), "$[*].category");

        assertTrue(productNames.contains("Monitor"));
        assertTrue(productNames.contains("Desk Lamp"));
        assertTrue(productCategories.contains("Displays"));
        assertTrue(productCategories.contains("Lighting"));
    }

    @Test
    void createProductReturnsCreatedAndPayload() throws Exception {
        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validProductJson("Wireless Mouse", "Ergonomic bluetooth mouse", "49.99", 120, "Accessories", "https://cdn.example.com/mouse.png")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.name").value("Wireless Mouse"))
                .andExpect(jsonPath("$.description").value("Ergonomic bluetooth mouse"))
                .andExpect(jsonPath("$.price").value(49.99))
                .andExpect(jsonPath("$.stockQuantity").value(120))
                .andExpect(jsonPath("$.category").value("Accessories"))
                .andExpect(jsonPath("$.imageUrl").value("https://cdn.example.com/mouse.png"))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void createProductPersistsRecordInDatabase() throws Exception {
        MvcResult createResult = mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validProductJson("Laptop Stand", "Aluminum adjustable stand", "79.50", 45, "Office", "https://cdn.example.com/stand.png")))
                .andExpect(status().isCreated())
                .andReturn();

        Number createdIdNumber = JsonPath.read(createResult.getResponse().getContentAsString(), "$.id");
        assertNotNull(createdIdNumber);
        Long createdId = createdIdNumber.longValue();

        List<Product> products = productRepository.findAll();
        assertFalse(products.isEmpty());

        Product savedProduct = productRepository.findById(createdId)
                .orElseThrow(() -> new IllegalStateException("Expected saved product not found"));

        assertEquals("Laptop Stand", savedProduct.getName());
        assertEquals("Aluminum adjustable stand", savedProduct.getDescription());
        assertEquals(new BigDecimal("79.50"), savedProduct.getPrice());
        assertEquals(45, savedProduct.getStockQuantity());
        assertEquals("Office", savedProduct.getCategory());
        assertEquals("https://cdn.example.com/stand.png", savedProduct.getImageUrl());
        assertEquals(true, savedProduct.getActive());
    }

    @Test
    void updateProductReturnsOkAndPersistsChanges() throws Exception {
        MvcResult createResult = mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validProductJson("Keyboard", "Mechanical keyboard", "99.99", 30, "Accessories", "https://cdn.example.com/kb.png")))
                .andExpect(status().isCreated())
                .andReturn();

        Number createdIdNumber = JsonPath.read(createResult.getResponse().getContentAsString(), "$.id");
        Long createdId = createdIdNumber.longValue();

        mockMvc.perform(put("/api/products/{id}", createdId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validProductJson("Keyboard Pro", "Premium mechanical keyboard", "149.99", 20, "Gaming", "https://cdn.example.com/kb-pro.png")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(createdId))
                .andExpect(jsonPath("$.name").value("Keyboard Pro"))
                .andExpect(jsonPath("$.description").value("Premium mechanical keyboard"))
                .andExpect(jsonPath("$.price").value(149.99))
                .andExpect(jsonPath("$.stockQuantity").value(20))
                .andExpect(jsonPath("$.category").value("Gaming"))
                .andExpect(jsonPath("$.imageUrl").value("https://cdn.example.com/kb-pro.png"))
                .andExpect(jsonPath("$.active").value(true));

        Product updatedProduct = productRepository.findById(createdId)
                .orElseThrow(() -> new IllegalStateException("Expected updated product not found"));

        assertEquals("Keyboard Pro", updatedProduct.getName());
        assertEquals("Premium mechanical keyboard", updatedProduct.getDescription());
        assertEquals(new BigDecimal("149.99"), updatedProduct.getPrice());
        assertEquals(20, updatedProduct.getStockQuantity());
        assertEquals("Gaming", updatedProduct.getCategory());
        assertEquals("https://cdn.example.com/kb-pro.png", updatedProduct.getImageUrl());
        assertEquals(true, updatedProduct.getActive());
    }

    @Test
    void updateProductReturnsNotFoundWhenMissing() throws Exception {
        mockMvc.perform(put("/api/products/{id}", 999999L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validProductJson("Any", "Any", "1.00", 1, "Any", "https://cdn.example.com/any.png")))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteProductReturnsNoContentAndMarksInactive() throws Exception {
        MvcResult createResult = mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validProductJson("Webcam", "1080p webcam", "89.99", 22, "Accessories", "https://cdn.example.com/webcam.png")))
                .andExpect(status().isCreated())
                .andReturn();

        Number createdIdNumber = JsonPath.read(createResult.getResponse().getContentAsString(), "$.id");
        Long createdId = createdIdNumber.longValue();

        mockMvc.perform(delete("/api/products/{id}", createdId))
                .andExpect(status().isNoContent());

        Product deletedProduct = productRepository.findById(createdId)
                .orElseThrow(() -> new IllegalStateException("Expected deleted product not found"));
        assertEquals(false, deletedProduct.getActive());
    }

    @Test
    void deleteProductHidesProductFromGetAllResults() throws Exception {
        MvcResult createResult = mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validProductJson("Headset", "Wireless headset", "129.99", 10, "Audio", "https://cdn.example.com/headset.png")))
                .andExpect(status().isCreated())
                .andReturn();

        Number createdIdNumber = JsonPath.read(createResult.getResponse().getContentAsString(), "$.id");
        Long createdId = createdIdNumber.longValue();

        mockMvc.perform(delete("/api/products/{id}", createdId))
                .andExpect(status().isNoContent());

        MvcResult listResult = mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andReturn();

        List<Number> remainingIds = JsonPath.read(listResult.getResponse().getContentAsString(), "$[*].id");
        assertFalse(remainingIds.stream().anyMatch(id -> id.longValue() == createdId));
    }

    @Test
    void deleteProductReturnsNotFoundWhenMissing() throws Exception {
        mockMvc.perform(delete("/api/products/{id}", 999999L))
                .andExpect(status().isNotFound());
    }

    @Test
    void searchProductsReturnsCaseInsensitiveMatches() throws Exception {
        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validProductJson("Wireless Mouse", "Bluetooth mouse", "49.99", 25, "Accessories", "https://cdn.example.com/mouse.png")))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validProductJson("Gaming Mouse Pad", "Large mouse pad", "19.99", 40, "Accessories", "https://cdn.example.com/mouse-pad.png")))
                .andExpect(status().isCreated());

        MvcResult searchResult = mockMvc.perform(get("/api/products/search").param("keyword", "MOUSE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andReturn();

        List<String> names = JsonPath.read(searchResult.getResponse().getContentAsString(), "$[*].name");
        assertTrue(names.contains("Wireless Mouse"));
        assertTrue(names.contains("Gaming Mouse Pad"));
    }

    @Test
    void searchProductsReturnsEmptyWhenNoMatch() throws Exception {
        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validProductJson("Desk Lamp", "LED lamp", "29.99", 40, "Lighting", "https://cdn.example.com/lamp.png")))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/products/search").param("keyword", "nonexistent-keyword"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void searchProductsExcludesDeletedAndOutOfStock() throws Exception {
        MvcResult activeCreateResult = mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validProductJson("Monitor Pro", "4K monitor", "399.99", 8, "Displays", "https://cdn.example.com/monitor-pro.png")))
                .andExpect(status().isCreated())
                .andReturn();

        MvcResult deleteCreateResult = mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validProductJson("Monitor Legacy", "Old monitor", "99.99", 6, "Displays", "https://cdn.example.com/monitor-legacy.png")))
                .andExpect(status().isCreated())
                .andReturn();

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validProductJson("Monitor Out", "Out of stock monitor", "199.99", 0, "Displays", "https://cdn.example.com/monitor-out.png")))
                .andExpect(status().isCreated());

        Number deletedIdNumber = JsonPath.read(deleteCreateResult.getResponse().getContentAsString(), "$.id");
        mockMvc.perform(delete("/api/products/{id}", deletedIdNumber.longValue()))
                .andExpect(status().isNoContent());

        Number activeIdNumber = JsonPath.read(activeCreateResult.getResponse().getContentAsString(), "$.id");

        mockMvc.perform(get("/api/products/search").param("keyword", "monitor"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(activeIdNumber.longValue()))
                .andExpect(jsonPath("$[0].name").value("Monitor Pro"));
    }

    private String validProductJson(String name, String description, String price, int stockQuantity, String category, String imageUrl) {
        return "{" +
                "\"name\":\"" + name + "\"," +
                "\"description\":\"" + description + "\"," +
                "\"price\":" + price + "," +
                "\"stockQuantity\":" + stockQuantity + "," +
                "\"category\":\"" + category + "\"," +
                "\"imageUrl\":\"" + imageUrl + "\"" +
                "}";
    }
}

