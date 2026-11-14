package com.learningplatform.backend.reference;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("oracle")
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:modulereferencetest;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class ModuleReferenceIntegrationTest {

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private MockMvc mvc;

    @BeforeEach
    void seed() {
        jdbc.update("delete from MODULE_REFERENCE");
        jdbc.update("delete from MODULE_ROADMAP");
        jdbc.update("delete from MODULE_OVERVIEW");
        jdbc.update("delete from MODULE_STRUCTURE");
        jdbc.update("delete from LANGUAGE");
        jdbc.update("delete from TOPIC");
        jdbc.update("delete from CATEGORY");

        jdbc.update("insert into CATEGORY (ID, CODE, NAME, SORT_ORDER, IS_ACTIVE) "
                + "values (1, 'BACKEND', 'Backend', 0, 1)");
        jdbc.update("insert into TOPIC (ID, CATEGORY_ID, CODE, SLUG, NAME, ICON_SVG, STATUS, SORT_ORDER) "
                + "values (10, 1, 'JAVA', 'java', 'Java', '<svg/>', 'AVAILABLE', 0)");
        jdbc.update("insert into MODULE_STRUCTURE "
                + "(ID_STRUCTURE, TOPIC_ID, NODE_KEY, PARENT_NODE_KEY, NODE_TYPE, SLUG, NAME, SORT_ORDER, IS_ACTIVE) "
                + "values (101, 10, 'JAVA_OOP', null, 'MODULE', 'java-oop', 'OOP', 0, 1)");
        jdbc.update("insert into MODULE_STRUCTURE "
                + "(ID_STRUCTURE, TOPIC_ID, NODE_KEY, PARENT_NODE_KEY, NODE_TYPE, SLUG, NAME, SORT_ORDER, IS_ACTIVE) "
                + "values (102, 10, 'JAVA_STREAM', null, 'MODULE', 'java-stream', 'Stream', 1, 1)");
        jdbc.update("insert into LANGUAGE (CODE, NAME, IS_ACTIVE) values ('vi', 'Tiếng Việt', 1)");
        jdbc.update("insert into LANGUAGE (CODE, NAME, IS_ACTIVE) values ('en', 'English', 1)");
    }

    private void insertReference(long id, long moduleId, String lang, String title,
                                 String url, String description, int sortOrder) {
        jdbc.update("insert into MODULE_REFERENCE "
                        + "(ID_REFERENCE, ID_STRUCTURE, LANGUAGE_CODE, TITLE, URL, DESCRIPTION, SORT_ORDER) "
                        + "values (?, ?, ?, ?, ?, ?, ?)",
                id, moduleId, lang, title, url, description, sortOrder);
    }

    @Test
    void returnsOrderedLocalizedReferencesWithStableTieBreakerAndOptionalDescription() throws Exception {
        insertReference(30, 101, "vi", "Java Tutorials", "https://example.org/tutorials", "Đọc thêm", 20);
        insertReference(10, 101, "vi", "Java Language Specification",
                "https://docs.oracle.com/javase/specs/", null, 10);
        insertReference(20, 101, "vi", "Java API", "https://docs.oracle.com/javase/21/docs/api/",
                "Tài liệu API", 10);
        insertReference(40, 101, "en", "Java specification",
                "https://docs.oracle.com/javase/specs/", "Official docs", 1);
        insertReference(50, 102, "vi", "Stream API",
                "https://docs.oracle.com/javase/21/docs/api/java.base/java/util/stream/package-summary.html",
                "Another module", 1);

        mvc.perform(get("/api/v1/modules/java-oop/reference").param("lang", "vi"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slug").value("java-oop"))
                .andExpect(jsonPath("$.languageCode").value("vi"))
                .andExpect(jsonPath("$.references.length()").value(3))
                .andExpect(jsonPath("$.references[0].id").value(10))
                .andExpect(jsonPath("$.references[0].title").value("Java Language Specification"))
                .andExpect(jsonPath("$.references[0].description").value(""))
                .andExpect(jsonPath("$.references[0].sortOrder").value(10))
                .andExpect(jsonPath("$.references[1].id").value(20))
                .andExpect(jsonPath("$.references[1].url").value("https://docs.oracle.com/javase/21/docs/api/"))
                .andExpect(jsonPath("$.references[2].id").value(30));

        mvc.perform(get("/api/v1/modules/java-oop/reference").param("lang", "en"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.references.length()").value(1))
                .andExpect(jsonPath("$.references[0].title").value("Java specification"));
    }

    @Test
    void missingModuleTranslationOrInvalidLanguageIsRejected() throws Exception {
        insertReference(1, 101, "vi", "Java", "https://example.org/java", null, 1);

        mvc.perform(get("/api/v1/modules/java-oop/reference").param("lang", "en"))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/modules/java-oop/reference").param("lang", "ja"))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/modules/java-stream/reference").param("lang", "vi"))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/modules/missing/reference").param("lang", "vi"))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/modules/java-oop/reference"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/modules/java-oop/reference").param("lang", " "))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/modules/java-oop/reference").param("lang", "x".repeat(11)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void inactiveLanguageDoesNotExposeReferences() throws Exception {
        insertReference(1, 101, "vi", "Java", "https://example.org/java", null, 1);
        jdbc.update("update LANGUAGE set IS_ACTIVE = 0 where CODE = 'vi'");
        mvc.perform(get("/api/v1/modules/java-oop/reference").param("lang", "vi"))
                .andExpect(status().isNotFound());
    }

    @Test
    void inactiveModuleGroupOrHiddenAncestorDoesNotExposeReferences() throws Exception {
        insertReference(1, 101, "vi", "Java", "https://example.org/java", null, 1);

        jdbc.update("update MODULE_STRUCTURE set IS_ACTIVE = 0 where ID_STRUCTURE = 101");
        mvc.perform(get("/api/v1/modules/java-oop/reference").param("lang", "vi"))
                .andExpect(status().isNotFound());

        jdbc.update("update MODULE_STRUCTURE set IS_ACTIVE = 1, NODE_TYPE = 'GROUP' where ID_STRUCTURE = 101");
        mvc.perform(get("/api/v1/modules/java-oop/reference").param("lang", "vi"))
                .andExpect(status().isNotFound());

        jdbc.update("update MODULE_STRUCTURE set NODE_TYPE = 'MODULE' where ID_STRUCTURE = 101");
        jdbc.update("insert into MODULE_STRUCTURE "
                + "(ID_STRUCTURE, TOPIC_ID, NODE_KEY, NODE_TYPE, SLUG, NAME, SORT_ORDER, IS_ACTIVE) "
                + "values (100, 10, 'JAVA_PARENT', 'GROUP', 'java-parent', 'Parent', 0, 0)");
        jdbc.update("update MODULE_STRUCTURE set PARENT_NODE_KEY = 'JAVA_PARENT' where ID_STRUCTURE = 101");
        mvc.perform(get("/api/v1/modules/java-oop/reference").param("lang", "vi"))
                .andExpect(status().isNotFound());

        jdbc.update("update MODULE_STRUCTURE set IS_ACTIVE = 1 where ID_STRUCTURE = 100");
        mvc.perform(get("/api/v1/modules/java-oop/reference").param("lang", "vi"))
                .andExpect(status().isOk());
    }

    @Test
    void hiddenTopicOrInactiveCategoryDoesNotExposeReferences() throws Exception {
        insertReference(1, 101, "vi", "Java", "https://example.org/java", null, 1);
        jdbc.update("update TOPIC set STATUS = 'HIDDEN' where ID = 10");
        mvc.perform(get("/api/v1/modules/java-oop/reference").param("lang", "vi"))
                .andExpect(status().isNotFound());

        jdbc.update("update TOPIC set STATUS = 'AVAILABLE' where ID = 10");
        jdbc.update("update CATEGORY set IS_ACTIVE = 0 where ID = 1");
        mvc.perform(get("/api/v1/modules/java-oop/reference").param("lang", "vi"))
                .andExpect(status().isNotFound());
    }

    @Test
    void databaseRequiresExistingModuleLanguageAndTitleAndUrl() {
        assertThrows(DataIntegrityViolationException.class, () ->
                insertReference(1, 999, "vi", "Missing module", "https://example.org", null, 1));
        assertThrows(DataIntegrityViolationException.class, () ->
                insertReference(2, 101, "ja", "Unknown language", "https://example.org", null, 1));
        assertThrows(DataIntegrityViolationException.class, () ->
                insertReference(3, 101, "vi", null, "https://example.org", null, 1));
        assertThrows(DataIntegrityViolationException.class, () ->
                insertReference(4, 101, "vi", "Missing URL", null, null, 1));
    }

    @Test
    void referenceIsNotInNavigationAndNoWriteEndpointIsExposed() throws Exception {
        insertReference(1, 101, "vi", "Java", "https://example.org", null, 1);

        mvc.perform(get("/api/v1/topics/10/structure"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.root.children[0].references").doesNotExist());

        mvc.perform(post("/api/v1/modules/java-oop/reference").param("lang", "vi"))
                .andExpect(status().isMethodNotAllowed());
    }
}
