package com.learningplatform.backend.auth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class PublicSecurityIntegrationTest {

    @Autowired MockMvc mockMvc;

    @Test
    void backendWithoutOracleOrGoogleCredentialsStartsAndReportsDisabledLogin() throws Exception {
        mockMvc.perform(get("/api/auth/config"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.googleLoginEnabled").value(false));
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/topics"))
                .andExpect(status().isNotFound());
    }
}
