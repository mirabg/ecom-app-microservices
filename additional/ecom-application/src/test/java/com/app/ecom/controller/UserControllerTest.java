package com.app.ecom.controller;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class UserControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void getAllUsersReturnsOk() throws Exception {
		mockMvc.perform(get("/api/users"))
				.andExpect(status().isOk());
	}

	@Test
	void createThenGetUserByIdWorks() throws Exception {
		mockMvc.perform(post("/api/users")
						.contentType(MediaType.APPLICATION_JSON)
						.content(validUserJson("Jane", "Doe", "jane@example.com", "5551234567", "123 Main St", "Austin")))
				.andExpect(status().isOk())
				.andExpect(content().string("User created successfully"));

		MvcResult listResult = mockMvc.perform(get("/api/users"))
				.andExpect(status().isOk())
				.andReturn();

		List<String> createdIds = JsonPath.read(
				listResult.getResponse().getContentAsString(),
				"$[?(@.email == 'jane@example.com')].id"
		);
		assertFalse(createdIds.isEmpty());

		mockMvc.perform(get("/api/users/" + createdIds.getFirst()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.firstName").value("Jane"));
	}

	@Test
	void getUserByIdReturnsNotFoundWhenMissing() throws Exception {
		mockMvc.perform(get("/api/users/999999"))
				.andExpect(status().isNotFound());
	}

	@Test
	void updateUserReturnsOkWhenExisting() throws Exception {
		mockMvc.perform(post("/api/users")
						.contentType(MediaType.APPLICATION_JSON)
						.content(validUserJson("Alex", "Parker", "alex@example.com", "5551112222", "123 Main St", "Austin")))
				.andExpect(status().isOk());

		MvcResult listResult = mockMvc.perform(get("/api/users"))
				.andExpect(status().isOk())
				.andReturn();

		List<String> createdIds = JsonPath.read(
				listResult.getResponse().getContentAsString(),
				"$[?(@.email == 'alex@example.com')].id"
		);
		assertFalse(createdIds.isEmpty());

		mockMvc.perform(put("/api/users/" + createdIds.getFirst())
						.contentType(MediaType.APPLICATION_JSON)
						.content(validUserJson("Alicia", "Parker", "alicia@example.com", "9990001111", "500 New Ave", "Dallas")))
				.andExpect(status().isOk())
				.andExpect(content().string("User updated successfully"));

		mockMvc.perform(get("/api/users/" + createdIds.getFirst()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.firstName").value("Alicia"))
				.andExpect(jsonPath("$.lastName").value("Parker"))
				.andExpect(jsonPath("$.email").value("alicia@example.com"))
				.andExpect(jsonPath("$.phone").value("9990001111"))
				.andExpect(jsonPath("$.address.street").value("500 New Ave"))
				.andExpect(jsonPath("$.address.city").value("Dallas"));
	}

	@Test
	void updateUserReturnsNotFoundWhenMissing() throws Exception {
		mockMvc.perform(put("/api/users/999999")
						.contentType(MediaType.APPLICATION_JSON)
						.content(validUserJson("Ghost", "User", "ghost@example.com", "5558889999", "Nowhere St", "Nowhere")))
				.andExpect(status().isNotFound());
	}

	@Test
	void createWithEmptyPayloadStillReturnsSuccessCurrentBehavior() throws Exception {
		mockMvc.perform(post("/api/users")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{}"))
				.andExpect(status().isOk())
				.andExpect(content().string("User created successfully"));
	}

	@Test
	void updateWithNullAddressClearsPersistedAddress() throws Exception {
		mockMvc.perform(post("/api/users")
						.contentType(MediaType.APPLICATION_JSON)
						.content(validUserJson("Taylor", "Reed", "taylor@example.com", "7771112222", "40 Lake Rd", "Miami")))
				.andExpect(status().isOk());

		MvcResult listResult = mockMvc.perform(get("/api/users"))
				.andExpect(status().isOk())
				.andReturn();

		List<String> createdIds = JsonPath.read(
				listResult.getResponse().getContentAsString(),
				"$[?(@.email == 'taylor@example.com')].id"
		);
		assertFalse(createdIds.isEmpty());

		String updateWithoutAddress = "{" +
				"\"firstName\":\"Taylor\"," +
				"\"lastName\":\"Reed\"," +
				"\"email\":\"taylor.updated@example.com\"," +
				"\"phone\":\"7773334444\"," +
				"\"address\":null" +
				"}";

		mockMvc.perform(put("/api/users/" + createdIds.getFirst())
						.contentType(MediaType.APPLICATION_JSON)
						.content(updateWithoutAddress))
				.andExpect(status().isOk())
				.andExpect(content().string("User updated successfully"));

		mockMvc.perform(get("/api/users/" + createdIds.getFirst()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.email").value("taylor.updated@example.com"))
				.andExpect(jsonPath("$.phone").value("7773334444"))
				.andExpect(jsonPath("$.address").value(nullValue()));
	}

	private String validUserJson(String firstName, String lastName, String email, String phone, String street, String city) {
		return "{" +
				"\"firstName\":\"" + firstName + "\"," +
				"\"lastName\":\"" + lastName + "\"," +
				"\"email\":\"" + email + "\"," +
				"\"phone\":\"" + phone + "\"," +
				"\"address\":{" +
				"\"street\":\"" + street + "\"," +
				"\"city\":\"" + city + "\"," +
				"\"state\":\"TX\"," +
				"\"zip\":\"78701\"," +
				"\"country\":\"USA\"}" +
				"}";
	}
}

