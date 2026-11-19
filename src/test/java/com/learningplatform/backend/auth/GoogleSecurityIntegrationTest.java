package com.learningplatform.backend.auth;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("oracle")
@TestPropertySource(properties = {
        "app.auth.google.enabled=true",
        "spring.security.oauth2.client.registration.google.client-id=test-client",
        "spring.security.oauth2.client.registration.google.client-secret=test-secret",
        "spring.datasource.url=jdbc:h2:mem:oauthSecurity;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.session.jdbc.initialize-schema=always"
})
class GoogleSecurityIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired JdbcTemplate jdbc;

    @Test
    void reportsLoginCapabilityAndRejectsAnonymousAdmin() throws Exception {
        mockMvc.perform(get("/api/auth/config"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.googleLoginEnabled").value(true));
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/oauth2/authorization/google"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", containsString("accounts.google.com")));
    }

    @Test
    void csrfProtectsLogoutAndPreflightKeepsCredentials() throws Exception {
        mockMvc.perform(post("/api/auth/logout"))
                .andExpect(status().isForbidden());
        var csrf = mockMvc.perform(get("/api/auth/csrf"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isString())
                .andExpect(jsonPath("$.headerName").value("X-CSRF-TOKEN"))
                .andReturn();
        String cookie = csrf.getResponse().getHeader("Set-Cookie");
        assertNotNull(cookie);
        assertTrue(cookie.contains("SESSION="));
        assertTrue(cookie.contains("Secure"));
        assertTrue(cookie.contains("HttpOnly"));
        assertTrue(cookie.contains("SameSite=Lax"));
        assertTrue(cookie.contains("Max-Age=7776000"));
        assertTrue(jdbc.queryForObject("SELECT COUNT(*) FROM SPRING_SESSION", Integer.class) > 0);
        mockMvc.perform(get("/api/auth/me")
                        .header("Origin", "https://nguyendodinhphuong.name.vn"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("Access-Control-Allow-Origin",
                        "https://nguyendodinhphuong.name.vn"))
                .andExpect(header().string("Access-Control-Allow-Credentials", "true"));
    }
}
