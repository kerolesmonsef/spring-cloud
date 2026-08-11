package com.keroles.wso2server.auth.controller;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthApiTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void loginReturnsJwtForSeedUser() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"mbank\",\"password\":\"123456\"}"))
                .andExpect(status().isOk())
                .andExpect(result -> {
                    JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
                    if (!body.hasNonNull("token") || body.get("token").asText().isBlank()) {
                        throw new AssertionError("login response must contain a token");
                    }
                });
    }

    @Test
    void invalidLoginReturnsUnauthorized() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"mbank\",\"password\":\"wrong\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void dataRequiresJwt() throws Exception {
        mockMvc.perform(get("/data"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void dataReturnsStaticMessageForValidJwt() throws Exception {
        String response = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"mbank\",\"password\":\"123456\"}"))
                .andReturn().getResponse().getContentAsString();
        String token = objectMapper.readTree(response).get("token").asText();

        mockMvc.perform(get("/data").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"message\":\"hello you are logged in and authorized\"}"));
    }
}
