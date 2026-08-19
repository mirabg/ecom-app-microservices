package com.ecom.user;

import com.ecom.user.repository.UserRepository;
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
class UserControllerE2ETest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    private static final String JOHN_JSON = """
            {
              "firstName": "John",
              "lastName": "Doe",
              "email": "john.doe@example.com",
              "phone": "555-1234"
            }
            """;

    private static final String JANE_JSON = """
            {
              "firstName": "Jane",
              "lastName": "Smith",
              "email": "jane.smith@example.com",
              "phone": "555-5678"
            }
            """;

    private static final String JOHN_WITH_ADDRESS_JSON = """
            {
              "firstName": "John",
              "lastName": "Doe",
              "email": "john.doe@example.com",
              "phone": "555-1234",
              "address": {
                "street": "123 Main St",
                "city": "Springfield",
                "state": "IL",
                "zip": "62701",
                "country": "US"
              }
            }
            """;

    /** Helper: create a user and return their string ID from the GET-all list. */
    private String createUserAndGetId(String userJson) throws Exception {
        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(userJson))
                .andExpect(status().isOk());

        String listJson = mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        return JsonPath.read(listJson, "$[0].id");
    }

    @Test
    void createUserReturnsSuccessMessage() throws Exception {
        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JOHN_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string("User created successfully"));
    }

    @Test
    void getAllUsersReturnsAllCreatedUsers() throws Exception {
        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JOHN_JSON))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JANE_JSON))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].email",
                        containsInAnyOrder("john.doe@example.com", "jane.smith@example.com")));
    }

    @Test
    void getUserByIdReturnsCorrectUser() throws Exception {
        String userId = createUserAndGetId(JOHN_JSON);

        mockMvc.perform(get("/api/users/{id}", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId))
                .andExpect(jsonPath("$.firstName").value("John"))
                .andExpect(jsonPath("$.lastName").value("Doe"))
                .andExpect(jsonPath("$.email").value("john.doe@example.com"))
                .andExpect(jsonPath("$.phone").value("555-1234"))
                .andExpect(jsonPath("$.role").value("CUSTOMER"));
    }

    @Test
    void createUserWithAddressAndVerify() throws Exception {
        String userId = createUserAndGetId(JOHN_WITH_ADDRESS_JSON);

        mockMvc.perform(get("/api/users/{id}", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.address.street").value("123 Main St"))
                .andExpect(jsonPath("$.address.city").value("Springfield"))
                .andExpect(jsonPath("$.address.state").value("IL"))
                .andExpect(jsonPath("$.address.zip").value("62701"))
                .andExpect(jsonPath("$.address.country").value("US"));
    }

    @Test
    void updateUserReturnsOkWithSuccessMessage() throws Exception {
        String userId = createUserAndGetId(JOHN_JSON);

        String updateJson = """
                {
                  "firstName": "Jonathan",
                  "lastName": "Doe",
                  "email": "jonathan.doe@example.com",
                  "phone": "555-9999"
                }
                """;

        mockMvc.perform(put("/api/users/{id}", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateJson))
                .andExpect(status().isOk())
                .andExpect(content().string("User updated successfully"));

        // Verify the fields were actually updated
        mockMvc.perform(get("/api/users/{id}", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Jonathan"))
                .andExpect(jsonPath("$.email").value("jonathan.doe@example.com"));
    }

    @Test
    void updateUserAddsAddressWhenNotPreviouslySet() throws Exception {
        String userId = createUserAndGetId(JOHN_JSON);

        // Verify no address initially
        mockMvc.perform(get("/api/users/{id}", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.address").doesNotExist());

        // Update with an address
        mockMvc.perform(put("/api/users/{id}", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JOHN_WITH_ADDRESS_JSON))
                .andExpect(status().isOk());

        // Verify address is now set
        mockMvc.perform(get("/api/users/{id}", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.address.street").value("123 Main St"))
                .andExpect(jsonPath("$.address.city").value("Springfield"));
    }

    @Test
    void getUserByNonExistentIdReturnsNotFound() throws Exception {
        mockMvc.perform(get("/api/users/99999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateNonExistentUserReturnsNotFound() throws Exception {
        mockMvc.perform(put("/api/users/99999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JOHN_JSON))
                .andExpect(status().isNotFound());
    }
}
