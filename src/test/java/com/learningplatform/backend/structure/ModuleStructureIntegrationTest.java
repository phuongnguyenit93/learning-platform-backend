package com.learningplatform.backend.structure;

import com.learningplatform.backend.structure.model.ModuleStructure;
import com.learningplatform.backend.structure.model.StructureType;
import com.learningplatform.backend.structure.repository.ModuleStructureRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.util.List;

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
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ActiveProfiles("oracle")
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:structuretest;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class ModuleStructureIntegrationTest {

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private ModuleStructureRepository structures;

    @Autowired
    private MockMvc mvc;

    @BeforeAll
    void addSameTopicParentConstraintToTestSchema() {
        // Hibernate maps PARENT_NODE_KEY as a String. The production Oracle DDL
        // enforces this composite self-FK; reproduce it in the H2 integration test.
        jdbc.execute("""
                alter table MODULE_STRUCTURE add constraint FK_MS_PARENT
                    foreign key (TOPIC_ID, PARENT_NODE_KEY)
                    references MODULE_STRUCTURE (TOPIC_ID, NODE_KEY)
                """);
    }

    @BeforeEach
    void prepareTopics() {
        jdbc.update("delete from MODULE_STRUCTURE");
        jdbc.update("delete from TOPIC");
        jdbc.update("delete from CATEGORY");
        jdbc.update("insert into CATEGORY (ID, CODE, NAME, SORT_ORDER, IS_ACTIVE) values (1, 'BACKEND', 'Backend', 0, 1)");
        jdbc.update("""
                insert into TOPIC (ID, CATEGORY_ID, CODE, SLUG, NAME, ICON_SVG, STATUS, SORT_ORDER)
                values (10, 1, 'JAVA', 'java', 'Java', '<svg/>', 'AVAILABLE', 0)
                """);
        jdbc.update("""
                insert into TOPIC (ID, CATEGORY_ID, CODE, SLUG, NAME, ICON_SVG, STATUS, SORT_ORDER)
                values (20, 1, 'SPRING', 'spring', 'Spring', '<svg/>', 'AVAILABLE', 1)
                """);
    }

    private void insertNode(int id, int topicId, String nodeKey, String parentNodeKey,
                            String slug, String type, int sortOrder) {
        jdbc.update("""
                insert into MODULE_STRUCTURE
                    (ID_STRUCTURE, TOPIC_ID, NODE_KEY, PARENT_NODE_KEY,
                     NODE_TYPE, SLUG, NAME, SORT_ORDER, IS_ACTIVE)
                values (?, ?, ?, ?, ?, ?, ?, ?, 1)
                """, id, topicId, nodeKey, parentNodeKey, type, slug, slug, sortOrder);
    }

    @Test
    void readsHierarchicalNodesByTopicUsingNaturalKeys() {
        insertNode(1, 10, "JAVA_ROOT", null, "java-root", "GROUP", 0);
        insertNode(2, 10, "JAVA_CORE", "JAVA_ROOT", "java-core", "GROUP", 0);
        insertNode(3, 10, "JAVA_STREAM_API", "JAVA_CORE", "java-stream-api", "MODULE", 2);
        insertNode(4, 10, "JAVA_CONCURRENCY", "JAVA_CORE", "java-concurrency", "MODULE", 1);
        insertNode(5, 20, "SPRING_ROOT", null, "spring-root", "GROUP", 0);

        List<ModuleStructure> javaNodes = structures.findByTopicId(10L);
        assertEquals(4, javaNodes.size());
        assertEquals(List.of("JAVA_ROOT", "JAVA_CORE", "JAVA_CONCURRENCY", "JAVA_STREAM_API"),
                javaNodes.stream().map(ModuleStructure::getNodeKey).toList());

        ModuleStructure stream = structures.findByNodeKey("JAVA_STREAM_API").orElseThrow();
        assertEquals(10L, stream.getTopic().getId());
        assertEquals("JAVA_CORE", stream.getParentNodeKey());
        assertEquals(StructureType.MODULE, stream.getType());
        assertEquals("java-stream-api", stream.getSlug());
        assertTrue(stream.getActive());

        assertEquals(1, structures.findByTopicId(20L).size());
        assertFalse(structures.findByNodeKey("DOES_NOT_EXIST").isPresent());
    }

    @Test
    void nodeKeyIsGloballyUniqueEvenAcrossTopics() {
        insertNode(1, 10, "FUNDAMENTALS", null, "java-fundamentals", "MODULE", 0);
        assertThrows(DataIntegrityViolationException.class,
                () -> insertNode(2, 20, "FUNDAMENTALS", null, "spring-fundamentals", "MODULE", 0));
    }

    @Test
    void slugIsGloballyUniqueEvenAcrossTopics() {
        insertNode(1, 10, "JAVA_FUNDAMENTALS", null, "fundamentals", "MODULE", 0);
        assertThrows(DataIntegrityViolationException.class,
                () -> insertNode(2, 20, "SPRING_FUNDAMENTALS", null, "fundamentals", "MODULE", 0));
    }

    @Test
    void parentMustExistInTheSameTopic() {
        insertNode(1, 10, "JAVA_CORE", null, "java-core", "GROUP", 0);
        assertThrows(DataIntegrityViolationException.class,
                () -> insertNode(2, 20, "SPRING_CHILD", "JAVA_CORE", "spring-child", "MODULE", 0));
        assertThrows(DataIntegrityViolationException.class,
                () -> insertNode(3, 10, "JAVA_ORPHAN", "MISSING_PARENT", "java-orphan", "MODULE", 0));
    }

    @Test
    void moduleCanContainChildrenWithoutChangingItsType() {
        insertNode(1, 10, "MICROSERVICE", null, "microservice", "MODULE", 0);
        insertNode(2, 10, "MICROSERVICE_ORDERS", "MICROSERVICE", "order-service", "MODULE", 0);

        assertEquals("MICROSERVICE", structures.findByNodeKey("MICROSERVICE_ORDERS")
                .orElseThrow().getParentNodeKey());
    }

    @Test
    void returnsNestedStructureSortedWithinEachParent() throws Exception {
        insertNode(1, 10, "JAVA_ROOT", null, "java-root", "GROUP", 0);
        insertNode(2, 10, "JAVA_CORE", "JAVA_ROOT", "java-core", "GROUP", 10);
        insertNode(3, 10, "JAVA_STREAM_API", "JAVA_CORE", "java-stream-api", "MODULE", 20);
        insertNode(4, 10, "JAVA_CONCURRENCY", "JAVA_CORE", "java-concurrency", "MODULE", 5);
        insertNode(5, 10, "JAVA_ADVANCE", "JAVA_ROOT", "java-advance", "GROUP", 1);
        insertNode(6, 20, "SPRING_ROOT", null, "spring-root", "GROUP", 0);
        insertNode(7, 10, "JAVA_VERSION", null, "java-version", "GROUP", 2);

        mvc.perform(get("/api/v1/topics/10/structure"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value(1))
                .andExpect(jsonPath("$.root.id").value("topic:10"))
                .andExpect(jsonPath("$.root.name").value("Java"))
                .andExpect(jsonPath("$.root.kind").value("GROUP"))
                .andExpect(jsonPath("$.root.routeId").doesNotExist())
                .andExpect(jsonPath("$.root.children.length()").value(2))
                .andExpect(jsonPath("$.root.children[0].id").value("JAVA_ROOT"))
                .andExpect(jsonPath("$.root.children[0].nodeKey").value("JAVA_ROOT"))
                .andExpect(jsonPath("$.root.children[0].parentNodeKey").doesNotExist())
                .andExpect(jsonPath("$.root.children[0].routeId").doesNotExist())
                .andExpect(jsonPath("$.root.children[0].kind").value("GROUP"))
                .andExpect(jsonPath("$.root.children[0].children[0].nodeKey").value("JAVA_ADVANCE"))
                .andExpect(jsonPath("$.root.children[0].children[1].nodeKey").value("JAVA_CORE"))
                .andExpect(jsonPath("$.root.children[0].children[1].children[0].nodeKey").value("JAVA_CONCURRENCY"))
                .andExpect(jsonPath("$.root.children[0].children[1].children[0].routeId").value("JAVA_CONCURRENCY"))
                .andExpect(jsonPath("$.root.children[0].children[1].children[0].kind").value("MODULE"))
                .andExpect(jsonPath("$.root.children[0].children[1].children[0].path").value("/module/java/java-root/java-core/java-concurrency"))
                .andExpect(jsonPath("$.root.children[0].children[1].children[1].nodeKey").value("JAVA_STREAM_API"))
                .andExpect(jsonPath("$.root.children[0].children[1].children[1].slug").value("java-stream-api"))
                .andExpect(jsonPath("$.root.children[0].children[1].children[1].children.length()").value(0))
                .andExpect(jsonPath("$.root.children[1].nodeKey").value("JAVA_VERSION"));

        mvc.perform(get("/api/v1/topics/20/structure"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.root.name").value("Spring"))
                .andExpect(jsonPath("$.root.children[0].nodeKey").value("SPRING_ROOT"))
                .andExpect(jsonPath("$.root.children.length()").value(1));
    }

    @Test
    void hidesInactiveNodesWithTheirDescendants() throws Exception {
        insertNode(1, 10, "JAVA_ROOT", null, "java-root", "GROUP", 0);
        insertNode(2, 10, "JAVA_HIDDEN", "JAVA_ROOT", "java-hidden", "GROUP", 0);
        insertNode(3, 10, "JAVA_CHILD", "JAVA_HIDDEN", "java-child", "MODULE", 0);
        jdbc.update("update MODULE_STRUCTURE set IS_ACTIVE = 0 where NODE_KEY = 'JAVA_HIDDEN'");

        mvc.perform(get("/api/v1/topics/10/structure"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.root.children[0].nodeKey").value("JAVA_ROOT"))
                .andExpect(jsonPath("$.root.children[0].children.length()").value(0));
    }

    @Test
    void distinguishesEmptyVisibleTopicFromMissingOrHiddenTopic() throws Exception {
        mvc.perform(get("/api/v1/topics/10/structure"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value(1))
                .andExpect(jsonPath("$.root.children.length()").value(0));
        mvc.perform(get("/api/v1/topics/999/structure"))
                .andExpect(status().isNotFound());

        jdbc.update("update TOPIC set STATUS = 'HIDDEN' where ID = 10");
        mvc.perform(get("/api/v1/topics/10/structure"))
                .andExpect(status().isNotFound());

        jdbc.update("update TOPIC set STATUS = 'AVAILABLE' where ID = 10");
        jdbc.update("update CATEGORY set IS_ACTIVE = 0 where ID = 1");
        mvc.perform(get("/api/v1/topics/10/structure"))
                .andExpect(status().isNotFound());
    }

    @Test
    void doesNotExposeStructureMutationRoute() throws Exception {
        mvc.perform(post("/api/v1/topics/10/structure"))
                .andExpect(status().isMethodNotAllowed());
    }
}
