package com.learningplatform.backend.module_overview;

import com.learningplatform.backend.module_overview.model.ModuleOverview;
import com.learningplatform.backend.module_overview.model.ModuleOverviewId;
import com.learningplatform.backend.module_overview.repository.ModuleOverviewRepository;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("oracle")
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:moduleoverviewtest;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class ModuleOverviewIntegrationTest {

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private ModuleOverviewRepository overviews;

    @Autowired
    private MockMvc mvc;

    @BeforeEach
    void seed() {
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
                + "values (101, 10, 'JAVA_STREAM_API', null, 'MODULE', 'java-stream-api', 'Stream API', 0, 1)");
        jdbc.update("insert into LANGUAGE (CODE, NAME, IS_ACTIVE) values ('vi', 'Tiếng Việt', 1)");
        jdbc.update("insert into LANGUAGE (CODE, NAME, IS_ACTIVE) values ('en', 'English', 1)");
    }

    private void insertOverview(String languageCode, String markdown) {
        jdbc.update("insert into MODULE_OVERVIEW (ID_STRUCTURE, LANGUAGE_CODE, MARKDOWN_CONTENT) "
                + "values (101, ?, ?)", languageCode, markdown);
    }

    @Test
    void readsUnicodeMarkdownLongerThanVarcharLimitForEachLanguage() {
        String viMarkdown = "# Stream API\n\n**Giới thiệu**\n" + "Nội dung Tiếng Việt.\n".repeat(350);
        String enMarkdown = "# Stream API\n\nEnglish introduction with [docs](https://example.org).\n";
        assertTrue(viMarkdown.length() > 4_000);

        insertOverview("vi", viMarkdown);
        insertOverview("en", enMarkdown);

        ModuleOverview vi = overviews.findById(new ModuleOverviewId(101L, "vi")).orElseThrow();
        ModuleOverview en = overviews.findById(new ModuleOverviewId(101L, "en")).orElseThrow();

        assertEquals(viMarkdown, vi.getMarkdownContent());
        assertEquals(enMarkdown, en.getMarkdownContent());
        assertEquals(new ModuleOverviewId(101L, "vi"), vi.getId());
        assertEquals(new ModuleOverviewId(101L, "en"), en.getId());
        assertEquals(101L, vi.getModuleStructure().getIdStructure());
        assertEquals("vi", vi.getLanguage().getCode());
        assertEquals("en", en.getLanguage().getCode());
    }

    @Test
    void absentTranslationDoesNotFallBackToAnotherLanguage() {
        insertOverview("vi", "# Tiếng Việt");

        assertTrue(overviews.findById(new ModuleOverviewId(101L, "vi")).isPresent());
        assertFalse(overviews.findById(new ModuleOverviewId(101L, "en")).isPresent());
    }

    @Test
    void preventsDuplicateOverviewForSameModuleAndLanguage() {
        insertOverview("vi", "# First");

        assertThrows(DataIntegrityViolationException.class,
                () -> insertOverview("vi", "# Duplicate"));
    }

    @Test
    void requiresExistingLanguageAndModule() {
        assertThrows(DataIntegrityViolationException.class,
                () -> insertOverview("ja", "# No language row"));
        assertThrows(DataIntegrityViolationException.class, () -> jdbc.update(
                "insert into MODULE_OVERVIEW (ID_STRUCTURE, LANGUAGE_CODE, MARKDOWN_CONTENT) "
                        + "values (999, 'vi', '# No module row')"));
    }

    @Test
    void keepsMarkdownOutOfTreeNavigationPayload() throws Exception {
        insertOverview("vi", "# Should not be in navigation");

        mvc.perform(get("/api/v1/topics/10/structure"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.root.children[0].nodeKey").value("JAVA_STREAM_API"))
                .andExpect(jsonPath("$.root.children[0].overview").doesNotExist())
                .andExpect(jsonPath("$.root.children[0].markdownContent").doesNotExist());
    }

    @Test
    void getOverviewReturnsOnlyRequestedLanguageAndOriginalMarkdown() throws Exception {
        String viMarkdown = "# Stream API\n\n**Giới thiệu**\n" + "Ví dụ tiếng Việt.\n".repeat(350);
        String enMarkdown = "# Stream API\n\nEnglish **overview**.\n";
        insertOverview("vi", viMarkdown);
        insertOverview("en", enMarkdown);

        mvc.perform(get("/api/v1/modules/java-stream-api/overview").param("lang", "vi"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slug").value("java-stream-api"))
                .andExpect(jsonPath("$.languageCode").value("vi"))
                .andExpect(jsonPath("$.markdownContent").value(viMarkdown));
        mvc.perform(get("/api/v1/modules/java-stream-api/overview").param("lang", "en"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.languageCode").value("en"))
                .andExpect(jsonPath("$.markdownContent").value(enMarkdown));
    }

    @Test
    void missingLanguageOrTranslationDoesNotFallBack() throws Exception {
        insertOverview("vi", "# Chỉ có tiếng Việt");

        mvc.perform(get("/api/v1/modules/java-stream-api/overview").param("lang", "en"))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/modules/java-stream-api/overview").param("lang", "ja"))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/modules/java-stream-api/overview"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/modules/java-stream-api/overview").param("lang", " "))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/modules/java-stream-api/overview").param("lang", "a".repeat(11)))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/modules/does-not-exist/overview").param("lang", "vi"))
                .andExpect(status().isNotFound());
    }

    @Test
    void disabledLanguageIsNotAvailable() throws Exception {
        insertOverview("en", "# English");
        jdbc.update("update LANGUAGE set IS_ACTIVE = 0 where CODE = 'en'");

        mvc.perform(get("/api/v1/modules/java-stream-api/overview").param("lang", "en"))
                .andExpect(status().isNotFound());
    }

    @Test
    void cannotReadOverviewFromHiddenModuleGroupOrHiddenAncestor() throws Exception {
        insertOverview("vi", "# Private");
        jdbc.update("update MODULE_STRUCTURE set IS_ACTIVE = 0 where ID_STRUCTURE = 101");
        mvc.perform(get("/api/v1/modules/java-stream-api/overview").param("lang", "vi"))
                .andExpect(status().isNotFound());

        jdbc.update("update MODULE_STRUCTURE set IS_ACTIVE = 1, NODE_TYPE = 'GROUP' where ID_STRUCTURE = 101");
        mvc.perform(get("/api/v1/modules/java-stream-api/overview").param("lang", "vi"))
                .andExpect(status().isNotFound());

        jdbc.update("update MODULE_STRUCTURE set NODE_TYPE = 'MODULE' where ID_STRUCTURE = 101");
        jdbc.update("insert into MODULE_STRUCTURE "
                + "(ID_STRUCTURE, TOPIC_ID, NODE_KEY, NODE_TYPE, SLUG, NAME, SORT_ORDER, IS_ACTIVE) "
                + "values (100, 10, 'JAVA_PARENT', 'GROUP', 'java-parent', 'Parent', 0, 0)");
        jdbc.update("update MODULE_STRUCTURE set PARENT_NODE_KEY = 'JAVA_PARENT' where ID_STRUCTURE = 101");
        mvc.perform(get("/api/v1/modules/java-stream-api/overview").param("lang", "vi"))
                .andExpect(status().isNotFound());

        jdbc.update("update MODULE_STRUCTURE set IS_ACTIVE = 1 where ID_STRUCTURE = 100");
        mvc.perform(get("/api/v1/modules/java-stream-api/overview").param("lang", "vi"))
                .andExpect(status().isOk());
    }

    @Test
    void cannotReadOverviewFromHiddenTopicOrInactiveCategory() throws Exception {
        insertOverview("vi", "# Private");
        jdbc.update("update TOPIC set STATUS = 'HIDDEN' where ID = 10");
        mvc.perform(get("/api/v1/modules/java-stream-api/overview").param("lang", "vi"))
                .andExpect(status().isNotFound());

        jdbc.update("update TOPIC set STATUS = 'AVAILABLE' where ID = 10");
        jdbc.update("update CATEGORY set IS_ACTIVE = 0 where ID = 1");
        mvc.perform(get("/api/v1/modules/java-stream-api/overview").param("lang", "vi"))
                .andExpect(status().isNotFound());
    }

    @Test
    void controllerDoesNotExposeWriteMethod() throws Exception {
        mvc.perform(post("/api/v1/modules/java-stream-api/overview").param("lang", "vi"))
                .andExpect(status().isMethodNotAllowed());
    }
}
