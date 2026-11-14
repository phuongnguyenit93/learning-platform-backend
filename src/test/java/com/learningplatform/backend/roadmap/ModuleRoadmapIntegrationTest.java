package com.learningplatform.backend.roadmap;

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
        "spring.datasource.url=jdbc:h2:mem:moduleroadmaptest;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class ModuleRoadmapIntegrationTest {

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private MockMvc mvc;

    @BeforeEach
    void seed() {
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

    private void insertMilestone(long id, long moduleId, String lang,
                                 String title, String purpose, String objective, int sortOrder) {
        jdbc.update("insert into MODULE_ROADMAP "
                        + "(ID_ROADMAP, ID_STRUCTURE, LANGUAGE_CODE, TITLE, PURPOSE, OBJECTIVE, SORT_ORDER) "
                        + "values (?, ?, ?, ?, ?, ?, ?)",
                id, moduleId, lang, title, purpose, objective, sortOrder);
    }

    @Test
    void returnsOrderedMilestonesWithArrayObjectivesForRequestedModuleAndLanguage() throws Exception {
        insertMilestone(30, 101, "vi", "Đóng gói", "Bảo vệ trạng thái",
                "Ẩn chi tiết bên trong.\r\nKiểm soát thay đổi.", 20);
        insertMilestone(10, 101, "vi", "Tư duy OOP", "Mô hình hóa bài toán",
                "Nhận diện đối tượng.\n\nMô tả tương tác.", 10);
        insertMilestone(20, 101, "vi", "Lớp và đối tượng", "Hiểu class", null, 10);
        insertMilestone(40, 101, "en", "OOP mindset", "Model problems", "Identify objects.", 1);
        insertMilestone(50, 102, "vi", "Stream", "Other module", "Learn Stream.", 1);

        mvc.perform(get("/api/v1/modules/java-oop/roadmap").param("lang", "vi"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slug").value("java-oop"))
                .andExpect(jsonPath("$.languageCode").value("vi"))
                .andExpect(jsonPath("$.roadmap.length()").value(3))
                .andExpect(jsonPath("$.roadmap[0].id").value(10))
                .andExpect(jsonPath("$.roadmap[0].title").value("Tư duy OOP"))
                .andExpect(jsonPath("$.roadmap[0].purpose").value("Mô hình hóa bài toán"))
                .andExpect(jsonPath("$.roadmap[0].objectives[0]").value("Nhận diện đối tượng."))
                .andExpect(jsonPath("$.roadmap[0].objectives[1]").value("Mô tả tương tác."))
                .andExpect(jsonPath("$.roadmap[0].sortOrder").value(10))
                .andExpect(jsonPath("$.roadmap[1].id").value(20))
                .andExpect(jsonPath("$.roadmap[1].objectives.length()").value(0))
                .andExpect(jsonPath("$.roadmap[2].id").value(30))
                .andExpect(jsonPath("$.roadmap[2].objectives[1]").value("Kiểm soát thay đổi."))
                .andExpect(jsonPath("$.roadmap[0].milestoneKey").doesNotExist());

        mvc.perform(get("/api/v1/modules/java-oop/roadmap").param("lang", "en"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roadmap.length()").value(1))
                .andExpect(jsonPath("$.roadmap[0].title").value("OOP mindset"));
    }

    @Test
    void missingModuleLanguageOrTranslationDoesNotFallBack() throws Exception {
        insertMilestone(1, 101, "vi", "Tư duy OOP", "Mục đích", null, 1);

        mvc.perform(get("/api/v1/modules/java-oop/roadmap").param("lang", "en"))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/modules/java-oop/roadmap").param("lang", "ja"))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/modules/java-stream/roadmap").param("lang", "vi"))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/modules/missing/roadmap").param("lang", "vi"))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/modules/java-oop/roadmap"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/modules/java-oop/roadmap").param("lang", " "))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/modules/java-oop/roadmap").param("lang", "x".repeat(11)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void disabledLanguageDoesNotExposeRoadmap() throws Exception {
        insertMilestone(1, 101, "vi", "Tư duy OOP", "Mục đích", null, 1);
        jdbc.update("update LANGUAGE set IS_ACTIVE = 0 where CODE = 'vi'");

        mvc.perform(get("/api/v1/modules/java-oop/roadmap").param("lang", "vi"))
                .andExpect(status().isNotFound());
    }

    @Test
    void hiddenModuleGroupOrInactiveAncestorDoesNotExposeRoadmap() throws Exception {
        insertMilestone(1, 101, "vi", "Tư duy OOP", "Mục đích", null, 1);

        jdbc.update("update MODULE_STRUCTURE set IS_ACTIVE = 0 where ID_STRUCTURE = 101");
        mvc.perform(get("/api/v1/modules/java-oop/roadmap").param("lang", "vi"))
                .andExpect(status().isNotFound());

        jdbc.update("update MODULE_STRUCTURE set IS_ACTIVE = 1, NODE_TYPE = 'GROUP' where ID_STRUCTURE = 101");
        mvc.perform(get("/api/v1/modules/java-oop/roadmap").param("lang", "vi"))
                .andExpect(status().isNotFound());

        jdbc.update("update MODULE_STRUCTURE set NODE_TYPE = 'MODULE' where ID_STRUCTURE = 101");
        jdbc.update("insert into MODULE_STRUCTURE "
                + "(ID_STRUCTURE, TOPIC_ID, NODE_KEY, NODE_TYPE, SLUG, NAME, SORT_ORDER, IS_ACTIVE) "
                + "values (100, 10, 'JAVA_PARENT', 'GROUP', 'java-parent', 'Parent', 0, 0)");
        jdbc.update("update MODULE_STRUCTURE set PARENT_NODE_KEY = 'JAVA_PARENT' where ID_STRUCTURE = 101");
        mvc.perform(get("/api/v1/modules/java-oop/roadmap").param("lang", "vi"))
                .andExpect(status().isNotFound());

        jdbc.update("update MODULE_STRUCTURE set IS_ACTIVE = 1 where ID_STRUCTURE = 100");
        mvc.perform(get("/api/v1/modules/java-oop/roadmap").param("lang", "vi"))
                .andExpect(status().isOk());
    }

    @Test
    void hiddenTopicOrInactiveCategoryDoesNotExposeRoadmap() throws Exception {
        insertMilestone(1, 101, "vi", "Tư duy OOP", "Mục đích", null, 1);

        jdbc.update("update TOPIC set STATUS = 'HIDDEN' where ID = 10");
        mvc.perform(get("/api/v1/modules/java-oop/roadmap").param("lang", "vi"))
                .andExpect(status().isNotFound());

        jdbc.update("update TOPIC set STATUS = 'AVAILABLE' where ID = 10");
        jdbc.update("update CATEGORY set IS_ACTIVE = 0 where ID = 1");
        mvc.perform(get("/api/v1/modules/java-oop/roadmap").param("lang", "vi"))
                .andExpect(status().isNotFound());
    }

    @Test
    void databaseEnforcesForeignKeysAndRequiredFields() {
        assertThrows(DataIntegrityViolationException.class,
                () -> insertMilestone(1, 999, "vi", "Unknown module", "Purpose", null, 1));
        assertThrows(DataIntegrityViolationException.class,
                () -> insertMilestone(1, 101, "ja", "Unknown language", "Purpose", null, 1));
        assertThrows(DataIntegrityViolationException.class,
                () -> insertMilestone(1, 101, "vi", null, "Purpose", null, 1));
    }

    @Test
    void doesNotIncludeRoadmapInStructureTreeAndDoesNotExposeWrites() throws Exception {
        insertMilestone(1, 101, "vi", "Tư duy OOP", "Mục đích", null, 1);

        mvc.perform(get("/api/v1/topics/10/structure"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.root.children[0].roadmap").doesNotExist())
                .andExpect(jsonPath("$.root.children[0].objectives").doesNotExist());

        mvc.perform(post("/api/v1/modules/java-oop/roadmap").param("lang", "vi"))
                .andExpect(status().isMethodNotAllowed());
    }
}
