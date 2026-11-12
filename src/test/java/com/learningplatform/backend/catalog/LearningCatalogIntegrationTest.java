package com.learningplatform.backend.catalog;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("oracle")
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:catalogtest;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class LearningCatalogIntegrationTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JdbcTemplate jdbc;

    @BeforeEach
    void prepareData() {
        jdbc.update("delete from TOPIC");
        jdbc.update("delete from CATEGORY");
        jdbc.update("""
                insert into CATEGORY
                    (ID, CODE, NAME, SORT_ORDER, IS_ACTIVE)
                values (1, 'JVM', 'Java Virtual Machine', 1, 1)
                """);
        jdbc.update("""
                insert into CATEGORY
                    (ID, CODE, NAME, SORT_ORDER, IS_ACTIVE)
                values (2, 'ARCHIVED', 'Hidden', 2, 0)
                """);
        jdbc.update("""
                insert into TOPIC
                    (ID, CATEGORY_ID, CODE, SLUG, NAME, DESCRIPTION,
                     ICON_SVG, STATUS, SORT_ORDER)
                values (1, 1, 'JAVA', 'java', 'Java', 'Java fundamentals',
                        ?, 'AVAILABLE', 1)
                """, "<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 10 10\"><path d=\"M1 1\"/></svg>");
        jdbc.update("""
                insert into TOPIC
                    (ID, CATEGORY_ID, CODE, SLUG, NAME,
                     ICON_SVG, STATUS, SORT_ORDER)
                values (2, 1, 'HIDDEN', 'hidden', 'Hidden',
                        ?, 'HIDDEN', 2)
                """, "<svg xmlns=\"http://www.w3.org/2000/svg\"/>");
        jdbc.update("""
                insert into TOPIC
                    (ID, CATEGORY_ID, CODE, SLUG, NAME,
                     ICON_SVG, STATUS, SORT_ORDER)
                values (3, 2, 'INACTIVE', 'inactive', 'Hidden',
                        ?, 'AVAILABLE', 3)
                """, "<svg xmlns=\"http://www.w3.org/2000/svg\"/>");
    }

    @Test
    void readsVisibleTopicsWithCategoryAndRawSvg() throws Exception {
        mvc.perform(get("/api/v1/topics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].slug").value("java"))
                .andExpect(jsonPath("$[0].description").value("Java fundamentals"))
                .andExpect(jsonPath("$[0].iconSvg").value("<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 10 10\"><path d=\"M1 1\"/></svg>"))
                .andExpect(jsonPath("$[0].category.code").value("JVM"))
                .andExpect(jsonPath("$[0].category.name").value("Java Virtual Machine"));
    }

    @Test
    void readsSvgLargerThanVarchar2LimitFromClob() throws Exception {
        String longSvg = "<svg xmlns=\"http://www.w3.org/2000/svg\"><desc>"
                + "a".repeat(5000) + "</desc></svg>";
        jdbc.update("update TOPIC set ICON_SVG = ? where ID = 1", longSvg);

        mvc.perform(get("/api/v1/topics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].iconSvg").value(longSvg));
    }

    @Test
    void readsActiveCategories() throws Exception {
        mvc.perform(get("/api/v1/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Java Virtual Machine"));
    }

    @Test
    void exposesNoMutationRoutes() throws Exception {
        mvc.perform(post("/api/v1/topics")).andExpect(status().isMethodNotAllowed());
        mvc.perform(put("/api/v1/topics")).andExpect(status().isMethodNotAllowed());
        mvc.perform(patch("/api/v1/topics")).andExpect(status().isMethodNotAllowed());
        mvc.perform(delete("/api/v1/topics")).andExpect(status().isMethodNotAllowed());
        mvc.perform(post("/api/v1/categories")).andExpect(status().isMethodNotAllowed());
    }
}
