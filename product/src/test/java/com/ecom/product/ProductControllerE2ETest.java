package com.ecom.product;

import com.ecom.product.respository.ProductRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class ProductControllerE2ETest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProductRepository productRepository;


    @BeforeEach
    void setUp() {
        productRepository.deleteAll();
    }

    private static final String LAPTOP_JSON = """
            {
              "name": "Laptop",
              "description": "A powerful laptop",
              "price": 999.99,
              "stockQuantity": 10,
              "category": "Electronics",
              "imageUrl": "http://example.com/laptop.jpg"
            }
            """;

    private static final String PHONE_JSON = """
            {
              "name": "Smartphone",
              "description": "Latest smartphone",
              "price": 699.99,
              "stockQuantity": 25,
              "category": "Electronics",
              "imageUrl": "http://example.com/phone.jpg"
            }
            """;

    @Test
    void createProductReturnsCreatedWithCorrectFields() throws Exception {
        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(LAPTOP_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.name").value("Laptop"))
                .andExpect(jsonPath("$.description").value("A powerful laptop"))
                .andExpect(jsonPath("$.price").value(999.99))
                .andExpect(jsonPath("$.stockQuantity").value(10))
                .andExpect(jsonPath("$.category").value("Electronics"))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void getAllProductsReturnsOnlyActiveProducts() throws Exception {
        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(LAPTOP_JSON))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PHONE_JSON))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].name", containsInAnyOrder("Laptop", "Smartphone")));
    }

    @Test
    void getProductByIdReturnsActiveProduct() throws Exception {
        String createJson = mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(LAPTOP_JSON))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        long id = ((Number) JsonPath.read(createJson, "$.id")).longValue();

        mockMvc.perform(get("/api/products/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.name").value("Laptop"))
                .andExpect(jsonPath("$.price").value(999.99))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void getProductByIdReturnsNotFoundForNonExistentProduct() throws Exception {
        mockMvc.perform(get("/api/products/{id}", 99999L))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateProductUpdatesFieldsCorrectly() throws Exception {
        String createJson = mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(LAPTOP_JSON))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        long id = ((Number) JsonPath.read(createJson, "$.id")).longValue();

        String updateJson = """
                {
                  "name": "Gaming Laptop",
                  "description": "High-end gaming laptop",
                  "price": 1499.99,
                  "stockQuantity": 5,
                  "category": "Gaming",
                  "imageUrl": "http://example.com/gaming-laptop.jpg"
                }
                """;

        mockMvc.perform(put("/api/products/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.name").value("Gaming Laptop"))
                .andExpect(jsonPath("$.price").value(1499.99))
                .andExpect(jsonPath("$.category").value("Gaming"));
    }

    @Test
    void deleteProductSoftDeletesAndHidesFromList() throws Exception {
        String createJson = mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(LAPTOP_JSON))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        long id = ((Number) JsonPath.read(createJson, "$.id")).longValue();

        mockMvc.perform(delete("/api/products/{id}", id))
                .andExpect(status().isNoContent());

        // Soft-deleted product should not appear in the active list
        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        // Soft-deleted product should return 404 for fetch-by-id endpoint
        mockMvc.perform(get("/api/products/{id}", id))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteNonExistentProductReturnsNotFound() throws Exception {
        mockMvc.perform(delete("/api/products/99999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateNonExistentProductReturnsNotFound() throws Exception {
        mockMvc.perform(put("/api/products/99999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(LAPTOP_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    void searchProductsByKeywordReturnsMatchingProducts() throws Exception {
        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(LAPTOP_JSON))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PHONE_JSON))
                .andExpect(status().isCreated());

        // Search for "laptop" — should only match the laptop
        mockMvc.perform(get("/api/products/search").param("keyword", "laptop"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name").value("Laptop"));
    }

    @Test
    void searchProductsExcludesOutOfStockItems() throws Exception {
        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(LAPTOP_JSON))
                .andExpect(status().isCreated());

        // Create an out-of-stock laptop variant
        String outOfStockJson = """
                {
                  "name": "Laptop Pro",
                  "description": "Out of stock laptop",
                  "price": 1299.99,
                  "stockQuantity": 0,
                  "category": "Electronics",
                  "imageUrl": "http://example.com/laptop-pro.jpg"
                }
                """;

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(outOfStockJson))
                .andExpect(status().isCreated());

        // Search should only return the in-stock laptop
        mockMvc.perform(get("/api/products/search").param("keyword", "laptop"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name").value("Laptop"));
    }
}
