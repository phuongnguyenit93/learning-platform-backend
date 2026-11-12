package com.learningplatform.backend.health;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(HealthController.class)
class HealthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void respondsWithServiceStatus() throws Exception {
        mockMvc.perform(get("/api/v1/health"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.service").value("learning-platform-backend"));
    }

    @Test
    void allowsProductionFrontendOrigin() throws Exception {
        mockMvc.perform(get("/api/v1/health")
                        .header("Origin", "https://nguyendodinhphuong.name.vn"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "https://nguyendodinhphuong.name.vn"));
    }

    @Test
    void rejectsOtherOrigins() throws Exception {
        mockMvc.perform(get("/api/v1/health")
                        .header("Origin", "https://untrusted.example"))
                .andExpect(status().isForbidden());
    }
}
