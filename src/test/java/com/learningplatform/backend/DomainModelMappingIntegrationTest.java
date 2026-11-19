package com.learningplatform.backend;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import com.learningplatform.backend.category.repository.CategoryRepository;
import com.learningplatform.backend.module.core.repository.LearningModuleRepository;
import com.learningplatform.backend.module.knowledge.model.Difficulty;
import com.learningplatform.backend.module.knowledge.repository.ModuleKnowledgeLanguageRepository;
import com.learningplatform.backend.module.knowledge.repository.ModuleKnowledgeRepository;
import com.learningplatform.backend.module.menu.model.MenuNodeType;
import com.learningplatform.backend.module.menu.repository.ModuleMenuLanguageRepository;
import com.learningplatform.backend.module.menu.repository.ModuleMenuRepository;
import com.learningplatform.backend.module.overview.repository.ModuleOverviewRepository;
import com.learningplatform.backend.module.reference.repository.ModuleReferenceLanguageRepository;
import com.learningplatform.backend.module.roadmap.repository.ModuleRoadmapLanguageRepository;
import com.learningplatform.backend.shared.language.repository.LanguageRepository;
import com.learningplatform.backend.topic.model.TopicStatus;
import com.learningplatform.backend.topic.repository.TopicGroupModuleRepository;
import com.learningplatform.backend.topic.repository.TopicGroupRepository;
import com.learningplatform.backend.topic.repository.TopicRepository;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("oracle")
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:domainmodel;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class DomainModelMappingIntegrationTest {

    @Autowired private JdbcTemplate jdbc;
    @Autowired private MockMvc mockMvc;
    @Autowired private CategoryRepository categories;
    @Autowired private TopicRepository topics;
    @Autowired private TopicGroupRepository topicGroups;
    @Autowired private TopicGroupModuleRepository placements;
    @Autowired private LearningModuleRepository modules;
    @Autowired private LanguageRepository languages;
    @Autowired private ModuleRoadmapLanguageRepository roadmapLanguages;
    @Autowired private ModuleReferenceLanguageRepository referenceLanguages;
    @Autowired private ModuleOverviewRepository overviews;
    @Autowired private ModuleMenuRepository menus;
    @Autowired private ModuleMenuLanguageRepository menuLanguages;
    @Autowired private ModuleKnowledgeRepository knowledge;
    @Autowired private ModuleKnowledgeLanguageRepository knowledgeLanguages;

    @BeforeEach
    void seedCurrentSchema() {
        jdbc.update("delete from MODULE_KNOWLEDGE_LANGUAGE");
        jdbc.update("delete from MODULE_KNOWLEDGE");
        jdbc.update("delete from MODULE_MENU_LANGUAGE");
        jdbc.update("delete from MODULE_MENU");
        jdbc.update("delete from MODULE_OVERVIEW");
        jdbc.update("delete from MODULE_REFERENCE_LANGUAGE");
        jdbc.update("delete from MODULE_REFERENCE");
        jdbc.update("delete from MODULE_ROADMAP_LANGUAGE");
        jdbc.update("delete from MODULE_ROADMAP");
        jdbc.update("delete from TOPIC_GROUP_MODULE");
        jdbc.update("delete from TOPIC_GROUP");
        jdbc.update("delete from MODULE");
        jdbc.update("delete from LANGUAGE");
        jdbc.update("delete from TOPIC");
        jdbc.update("delete from CATEGORY");

        jdbc.update("""
                insert into CATEGORY (ID, CODE, NAME, SORT_ORDER, IS_ACTIVE)
                values (1, 'BACKEND', 'Backend', 1, 1)
                """);
        // ICON_SVG is nullable in the live Oracle schema.
        jdbc.update("""
                insert into TOPIC (ID, CATEGORY_ID, CODE, SLUG, NAME, ICON_SVG, STATUS, SORT_ORDER)
                values (10, 1, 'JAVA', 'java', 'Java', null, 'AVAILABLE', 1)
                """);
        jdbc.update("insert into LANGUAGE (CODE, NAME, IS_ACTIVE) values ('vi', 'Tiếng Việt', 1)");
        jdbc.update("insert into LANGUAGE (CODE, NAME, IS_ACTIVE) values ('en', 'English', 1)");
        jdbc.update("""
                insert into MODULE
                    (ID, SERVICE_NAME, SLUG, NAME, SERVICE_NAME_DESCRIPTION, SPECIAL_NOTE, IS_ACTIVE)
                values (100, 'JAVA_STREAM_API', 'java-stream-api', 'Java Stream API', null, null, 1)
                """);
        jdbc.update("""
                insert into MODULE
                    (ID, SERVICE_NAME, SLUG, NAME, SERVICE_NAME_DESCRIPTION, SPECIAL_NOTE, IS_ACTIVE)
                values (101, 'JAVA_COLLECTIONS', 'java-collections', 'Java Collections', null, null, 1)
                """);
        jdbc.update("""
                insert into TOPIC_GROUP
                    (ID, TOPIC_ID, GROUP_NAME, SLUG, PARENT_GROUP_ID, SORT_ORDER, IS_ACTIVE)
                values (200, 10, 'Java Core', 'java-core', null, 1, 1)
                """);
        jdbc.update("""
                insert into TOPIC_GROUP
                    (ID, TOPIC_ID, GROUP_NAME, SLUG, PARENT_GROUP_ID, SORT_ORDER, IS_ACTIVE)
                values (201, 10, 'Java Streams', 'java-streams', 200, 1, 1)
                """);
        jdbc.update("""
                insert into TOPIC_GROUP_MODULE (ID, GROUP_ID, TOPIC_ID, MODULE_ID, SORT_ORDER, IS_ACTIVE)
                values (300, 201, null, 100, 1, 1)
                """);
        jdbc.update("""
                insert into TOPIC_GROUP_MODULE (ID, GROUP_ID, TOPIC_ID, MODULE_ID, SORT_ORDER, IS_ACTIVE)
                values (301, null, 10, 101, 2, 1)
                """);
        jdbc.update("""
                insert into MODULE_ROADMAP (ID, MODULE_ID, ROADMAP_KEY, SORT_ORDER, IS_ACTIVE)
                values (400, 100, 'fundamentals', 1, 1)
                """);
        jdbc.update("""
                insert into MODULE_ROADMAP_LANGUAGE (ROADMAP_ID, LANGUAGE_CODE, TITLE, PURPOSE, OBJECTIVE)
                values (400, 'vi', 'Nền tảng', 'Nắm vững nền tảng', 'Hiểu Stream API')
                """);
        jdbc.update("""
                insert into MODULE_REFERENCE (ID, MODULE_ID, URL, SORT_ORDER, IS_ACTIVE)
                values (500, 100, 'https://docs.oracle.com/', 1, 1)
                """);
        jdbc.update("""
                insert into MODULE_REFERENCE_LANGUAGE (REFERENCE_ID, LANGUAGE_CODE, TITLE, DESCRIPTION)
                values (500, 'vi', 'Oracle Docs', 'Tài liệu chính thức')
                """);
        jdbc.update("""
                insert into MODULE_OVERVIEW (ID, MODULE_ID, LANGUAGE_CODE, CONTENT)
                values (600, 100, 'vi', '# Tổng quan')
                """);
        jdbc.update("""
                insert into MODULE_MENU
                    (ID, MODULE_ID, MENU_PARENT_ID, NODE_TYPE, MENU_PARENT_TYPE,
                     SLUG, SORT_ORDER, IS_ACTIVE)
                values (700, 100, null, 'CHAPTER', null, 'fundamentals', 1, 1)
                """);
        jdbc.update("""
                insert into MODULE_MENU
                    (ID, MODULE_ID, MENU_PARENT_ID, NODE_TYPE, MENU_PARENT_TYPE,
                     SLUG, SORT_ORDER, IS_ACTIVE)
                values (701, null, 700, 'SECTION', 'CHAPTER', 'stream-basics', 1, 1)
                """);
        jdbc.update("""
                insert into MODULE_MENU_LANGUAGE (MENU_ID, LANGUAGE_CODE, TITLE, DESCRIPTION)
                values (700, 'vi', 'Nền tảng', null)
                """);
        jdbc.update("""
                insert into MODULE_MENU_LANGUAGE (MENU_ID, LANGUAGE_CODE, TITLE, DESCRIPTION)
                values (701, 'vi', 'Stream cơ bản', 'Các khái niệm nền tảng')
                """);
        jdbc.update("""
                insert into MODULE_KNOWLEDGE (ID, MENU_ID, MENU_NODE_TYPE, DIFFICULT, IS_ACTIVE)
                values (800, 701, 'SECTION', 'INTERMEDIATE', 1)
                """);
        jdbc.update("""
                insert into MODULE_KNOWLEDGE_LANGUAGE
                    (KNOWLEDGE_ID, LANGUAGE_CODE, CONTENT, STATUS, FOCUS,
                     PROBLEM_SOLVED, MECHANISM, APPLICATION_VALUE, SUMMARY)
                values (800, 'vi', '# Stream', 'WRITTEN', 'Stream pipeline',
                        'Xử lý collection', 'Lazy evaluation', 'Viết pipeline', 'Tóm tắt')
                """);
    }

    @Test
    void mapsCatalogAndTopicPlacementAgainstTheCurrentSchema() {
        assertEquals(1, categories.findByActiveTrueOrderBySortOrderAscIdAsc().size());

        var topic = topics.findVisibleById(10L, TopicStatus.HIDDEN).orElseThrow();
        assertEquals("java", topic.getSlug());
        assertNull(topic.getIconSvg());

        var groups = topicGroups.findByTopicId(10L);
        assertEquals(2, groups.size());
        var childGroup = groups.stream().filter(group -> group.getId().equals(201L)).findFirst().orElseThrow();
        assertEquals("Java Streams", childGroup.getGroupName());
        assertEquals("java-streams", childGroup.getSlug());
        assertEquals("Java Core", childGroup.getParentGroup().getGroupName());

        var placedModule = placements.findActiveModulesByGroup(201L).getFirst().getModule();
        assertEquals("JAVA_STREAM_API", placedModule.getServiceName());
        assertEquals("JAVA_COLLECTIONS", placements.findActiveDirectModules(10L)
                .getFirst().getModule().getServiceName());
        assertEquals("java-stream-api", modules.findBySlugAndActiveTrue("java-stream-api")
                .orElseThrow().getSlug());
        assertEquals("vi", languages.findByCodeAndActiveTrue("vi").orElseThrow().getCode());
    }

    @Test
    void exposesCategoryTopicAndTopicStructureContractsForTheFrontend() throws Exception {
        mockMvc.perform(get("/api/v1/categories")
                        .header("Origin", "https://nguyendodinhphuong.name.vn"))
                .andExpect(status().isOk())
                .andExpect(header().string(
                        "Access-Control-Allow-Origin", "https://nguyendodinhphuong.name.vn"))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].code").value("BACKEND"))
                .andExpect(jsonPath("$[0].name").value("Backend"))
                .andExpect(jsonPath("$[0].sortOrder").value(1));

        mockMvc.perform(get("/api/v1/topics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(10))
                .andExpect(jsonPath("$[0].code").value("JAVA"))
                .andExpect(jsonPath("$[0].slug").value("java"))
                .andExpect(jsonPath("$[0].status").value("AVAILABLE"))
                .andExpect(jsonPath("$[0].iconSvg").value(nullValue()))
                .andExpect(jsonPath("$[0].category.id").value(1))
                .andExpect(jsonPath("$[0].category.code").value("BACKEND"));

        mockMvc.perform(get("/api/v1/topics/10/structure"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value(1))
                .andExpect(jsonPath("$.root.id").value("topic:10"))
                .andExpect(jsonPath("$.root.kind").value("GROUP"))
                .andExpect(jsonPath("$.root.slug").value("java"))
                .andExpect(jsonPath("$.root.children.length()").value(2))
                .andExpect(jsonPath("$.root.children[0].nodeKey").value("GROUP_200"))
                .andExpect(jsonPath("$.root.children[0].slug").value("java-core"))
                .andExpect(jsonPath("$.root.children[0].children[0].nodeKey").value("GROUP_201"))
                .andExpect(jsonPath("$.root.children[0].children[0].slug").value("java-streams"))
                .andExpect(jsonPath("$.root.children[0].children[0].children[0].nodeKey")
                        .value("JAVA_STREAM_API"))
                .andExpect(jsonPath("$.root.children[0].children[0].children[0].slug")
                        .value("java-stream-api"))
                .andExpect(jsonPath("$.root.children[1].nodeKey").value("JAVA_COLLECTIONS"))
                .andExpect(jsonPath("$.root.children[1].slug").value("java-collections"));
    }

    @Test
    void securityAllowsAnonymousRequestsAndCorsPreflight() throws Exception {
        mockMvc.perform(get("/api/v1/topics"))
                .andExpect(status().isOk());

        // Security must not return 401/403 for an unauthenticated request.
        // The controller has no POST mapping, so Spring MVC returns 405.
        mockMvc.perform(post("/api/v1/topics"))
                .andExpect(status().isMethodNotAllowed());

        mockMvc.perform(options("/api/v1/topics")
                        .header("Origin", "https://nguyendodinhphuong.name.vn")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string(
                        "Access-Control-Allow-Origin", "https://nguyendodinhphuong.name.vn"));
    }

    @Test
    void topicStructureReturnsNotFoundForHiddenTopics() throws Exception {
        jdbc.update("update TOPIC set STATUS = 'HIDDEN' where ID = 10");

        mockMvc.perform(get("/api/v1/topics/10/structure"))
                .andExpect(status().isNotFound());
    }

    @Test
    void exposesModuleCoreOverviewRoadmapAndReferenceContracts() throws Exception {
        mockMvc.perform(get("/api/v1/modules/java-stream-api")
                        .header("Origin", "https://nguyendodinhphuong.name.vn"))
                .andExpect(status().isOk())
                .andExpect(header().string(
                        "Access-Control-Allow-Origin", "https://nguyendodinhphuong.name.vn"))
                .andExpect(jsonPath("$.id").value(100))
                .andExpect(jsonPath("$.serviceName").value("JAVA_STREAM_API"))
                .andExpect(jsonPath("$.slug").value("java-stream-api"))
                .andExpect(jsonPath("$.name").value("Java Stream API"))
                .andExpect(jsonPath("$.description").value(nullValue()))
                .andExpect(jsonPath("$.specialNote").value(nullValue()));

        // A Module placed directly under a Topic (without a Group) is visible too.
        mockMvc.perform(get("/api/v1/modules/java-collections"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(101))
                .andExpect(jsonPath("$.slug").value("java-collections"));

        mockMvc.perform(get("/api/v1/modules/java-stream-api/overview?lang=vi"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slug").value("java-stream-api"))
                .andExpect(jsonPath("$.languageCode").value("vi"))
                .andExpect(jsonPath("$.markdownContent").value("# Tổng quan"));

        mockMvc.perform(get("/api/v1/modules/java-stream-api/roadmap?lang=vi"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slug").value("java-stream-api"))
                .andExpect(jsonPath("$.languageCode").value("vi"))
                .andExpect(jsonPath("$.roadmap.length()").value(1))
                .andExpect(jsonPath("$.roadmap[0].id").value(400))
                .andExpect(jsonPath("$.roadmap[0].title").value("Nền tảng"))
                .andExpect(jsonPath("$.roadmap[0].purpose").value("Nắm vững nền tảng"))
                .andExpect(jsonPath("$.roadmap[0].objectives[0]").value("Hiểu Stream API"))
                .andExpect(jsonPath("$.roadmap[0].sortOrder").value(1));

        mockMvc.perform(get("/api/v1/modules/java-stream-api/reference?lang=vi"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slug").value("java-stream-api"))
                .andExpect(jsonPath("$.languageCode").value("vi"))
                .andExpect(jsonPath("$.references.length()").value(1))
                .andExpect(jsonPath("$.references[0].id").value(500))
                .andExpect(jsonPath("$.references[0].title").value("Oracle Docs"))
                .andExpect(jsonPath("$.references[0].url").value("https://docs.oracle.com/"))
                .andExpect(jsonPath("$.references[0].description").value("Tài liệu chính thức"))
                .andExpect(jsonPath("$.references[0].sortOrder").value(1));
    }

    @Test
    void localizedModuleApisNeverFallbackToAnotherLanguageAndValidateLang() throws Exception {
        mockMvc.perform(get("/api/v1/modules/java-stream-api/overview?lang=en"))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/modules/java-stream-api/roadmap?lang=en"))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/modules/java-stream-api/reference?lang=en"))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/v1/modules/java-stream-api/overview"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/v1/modules/java-stream-api/roadmap?lang="))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/v1/modules/java-stream-api/reference?lang=01234567890"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void allModuleApisHideModulesUnderInactiveAncestorGroups() throws Exception {
        jdbc.update("update TOPIC_GROUP set IS_ACTIVE = 0 where ID = 200");

        mockMvc.perform(get("/api/v1/modules/java-stream-api"))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/modules/java-stream-api/overview?lang=vi"))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/modules/java-stream-api/roadmap?lang=vi"))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/modules/java-stream-api/reference?lang=vi"))
                .andExpect(status().isNotFound());
    }

    @Test
    void allModuleApisRequireAnActivePlacementAndVisibleTopic() throws Exception {
        jdbc.update("update TOPIC_GROUP_MODULE set IS_ACTIVE = 0 where ID = 300");
        mockMvc.perform(get("/api/v1/modules/java-stream-api"))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/modules/java-stream-api/overview?lang=vi"))
                .andExpect(status().isNotFound());

        jdbc.update("update TOPIC_GROUP_MODULE set IS_ACTIVE = 1 where ID = 300");
        jdbc.update("update TOPIC set STATUS = 'HIDDEN' where ID = 10");
        mockMvc.perform(get("/api/v1/modules/java-stream-api"))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/modules/java-stream-api/roadmap?lang=vi"))
                .andExpect(status().isNotFound());
    }

    @Test
    void allModuleApisRequireAnActiveModuleAndCategory() throws Exception {
        jdbc.update("update MODULE set IS_ACTIVE = 0 where ID = 100");
        mockMvc.perform(get("/api/v1/modules/java-stream-api"))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/modules/java-stream-api/overview?lang=vi"))
                .andExpect(status().isNotFound());

        jdbc.update("update MODULE set IS_ACTIVE = 1 where ID = 100");
        jdbc.update("update CATEGORY set IS_ACTIVE = 0 where ID = 1");
        mockMvc.perform(get("/api/v1/modules/java-stream-api"))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/modules/java-stream-api/roadmap?lang=vi"))
                .andExpect(status().isNotFound());
    }

    @Test
    void localizedModuleApisRequireAnActiveLanguage() throws Exception {
        jdbc.update("update LANGUAGE set IS_ACTIVE = 0 where CODE = 'vi'");

        mockMvc.perform(get("/api/v1/modules/java-stream-api/overview?lang=vi"))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/modules/java-stream-api/roadmap?lang=vi"))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/modules/java-stream-api/reference?lang=vi"))
                .andExpect(status().isNotFound());
    }

    @Test
    void mapsRoadmapReferenceAndOverviewWithTheirLanguageOwnership() {
        var roadmap = roadmapLanguages.findVisibleByModuleAndLanguage(100L, "vi").getFirst();
        assertEquals("fundamentals", roadmap.getRoadmap().getRoadmapKey());
        assertEquals("Nền tảng", roadmap.getTitle());

        var reference = referenceLanguages.findVisibleByModuleAndLanguage(100L, "vi").getFirst();
        assertEquals("https://docs.oracle.com/", reference.getReference().getUrl());
        assertEquals("Oracle Docs", reference.getTitle());

        var overview = overviews.findVisibleByModuleSlugAndLanguage("java-stream-api", "vi")
                .orElseThrow();
        assertEquals("# Tổng quan", overview.getContent());
        assertEquals(600L, overview.getId());
    }

    @Test
    void mapsTwoLevelMenuAndKnowledgeAttachedToSection() {
        var chapters = menus.findActiveByModuleAndNodeType(100L, MenuNodeType.CHAPTER);
        assertEquals(1, chapters.size());
        assertEquals("fundamentals", chapters.getFirst().getSlug());

        var sections = menus.findActiveChildren(700L, MenuNodeType.CHAPTER, MenuNodeType.SECTION);
        assertEquals(1, sections.size());
        assertEquals("stream-basics", sections.getFirst().getSlug());
        assertEquals("fundamentals", sections.getFirst().getParentMenu().getSlug());
        assertEquals("Stream cơ bản", menuLanguages
                .findVisibleByMenuAndLanguage(701L, "vi").orElseThrow().getTitle());

        var lesson = knowledge.findByMenuIdAndActiveTrue(701L).orElseThrow();
        assertEquals(Difficulty.INTERMEDIATE, lesson.getDifficult());
        assertEquals(MenuNodeType.SECTION, lesson.getMenu().getNodeType());
        assertEquals("# Stream", knowledgeLanguages
                .findVisibleByKnowledgeAndLanguage(lesson.getId(), "vi")
                .orElseThrow().getContent());
    }

    @Test
    void returnsLocalizedMenuWithoutLoadingMarkdownAndPublishedKnowledgeDetails() throws Exception {
        mockMvc.perform(get("/api/v1/modules/java-stream-api/menu?lang=vi")
                        .header("Origin", "https://nguyendodinhphuong.name.vn"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin",
                        "https://nguyendodinhphuong.name.vn"))
                .andExpect(jsonPath("$.slug").value("java-stream-api"))
                .andExpect(jsonPath("$.languageCode").value("vi"))
                .andExpect(jsonPath("$.chapters.length()").value(1))
                .andExpect(jsonPath("$.chapters[0].id").value(700))
                .andExpect(jsonPath("$.chapters[0].slug").value("fundamentals"))
                .andExpect(jsonPath("$.chapters[0].title").value("Nền tảng"))
                .andExpect(jsonPath("$.chapters[0].description").value(nullValue()))
                .andExpect(jsonPath("$.chapters[0].lessonCount").value(1))
                .andExpect(jsonPath("$.chapters[0].sections[0].id").value(701))
                .andExpect(jsonPath("$.chapters[0].sections[0].slug").value("stream-basics"))
                .andExpect(jsonPath("$.chapters[0].sections[0].title").value("Stream cơ bản"))
                .andExpect(jsonPath("$.chapters[0].sections[0].knowledgeId").value(800))
                .andExpect(jsonPath("$.chapters[0].sections[0].difficulty").value("INTERMEDIATE"))
                .andExpect(jsonPath("$.chapters[0].sections[0].available").value(true))
                .andExpect(jsonPath("$.chapters[0].sections[0].hasSummary").value(true))
                .andExpect(jsonPath("$.chapters[0].sections[0].markdownContent").doesNotExist())
                .andExpect(jsonPath("$.chapters[0].sections[0].summary").doesNotExist());

        mockMvc.perform(get("/api/v1/modules/java-stream-api/knowledge/fundamentals/stream-basics?lang=vi"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.moduleSlug").value("java-stream-api"))
                .andExpect(jsonPath("$.languageCode").value("vi"))
                .andExpect(jsonPath("$.chapter.id").value(700))
                .andExpect(jsonPath("$.chapter.slug").value("fundamentals"))
                .andExpect(jsonPath("$.chapter.title").value("Nền tảng"))
                .andExpect(jsonPath("$.section.id").value(701))
                .andExpect(jsonPath("$.section.slug").value("stream-basics"))
                .andExpect(jsonPath("$.section.description").value("Các khái niệm nền tảng"))
                .andExpect(jsonPath("$.knowledgeId").value(800))
                .andExpect(jsonPath("$.difficulty").value("INTERMEDIATE"))
                .andExpect(jsonPath("$.markdownContent").value("# Stream"))
                .andExpect(jsonPath("$.focus").value("Stream pipeline"))
                .andExpect(jsonPath("$.problemSolved").value("Xử lý collection"))
                .andExpect(jsonPath("$.mechanism").value("Lazy evaluation"))
                .andExpect(jsonPath("$.applicationValue").value("Viết pipeline"))
                .andExpect(jsonPath("$.summary").value("Tóm tắt"));
    }

    @Test
    void emptyVisibleModuleReturnsEmptyMenuInsteadOfNotFound() throws Exception {
        mockMvc.perform(get("/api/v1/modules/java-collections/menu?lang=vi"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slug").value("java-collections"))
                .andExpect(jsonPath("$.chapters.length()").value(0));
    }

    @Test
    void menuAndKnowledgeValidateLocaleWithoutFallback() throws Exception {
        mockMvc.perform(get("/api/v1/modules/java-stream-api/menu?lang=en"))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/modules/java-stream-api/knowledge/fundamentals/stream-basics?lang=en"))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/modules/java-stream-api/menu"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/v1/modules/java-stream-api/menu?lang="))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/v1/modules/java-stream-api/menu?lang=01234567890"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/v1/modules/java-stream-api/knowledge/fundamentals/stream-basics"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/v1/modules/java-stream-api/knowledge/fundamentals/stream-basics?lang=de"))
                .andExpect(status().isNotFound());

        // In a locale with localized menu titles but no Knowledge translation,
        // keep the Section as unavailable; do not fetch the vi lesson.
        jdbc.update("insert into MODULE_MENU_LANGUAGE (MENU_ID, LANGUAGE_CODE, TITLE) values (700, 'en', 'Basics')");
        jdbc.update("insert into MODULE_MENU_LANGUAGE (MENU_ID, LANGUAGE_CODE, TITLE) values (701, 'en', 'Stream basics')");
        mockMvc.perform(get("/api/v1/modules/java-stream-api/menu?lang=en"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.chapters[0].title").value("Basics"))
                .andExpect(jsonPath("$.chapters[0].sections[0].available").value(false));
        mockMvc.perform(get("/api/v1/modules/java-stream-api/knowledge/fundamentals/stream-basics?lang=en"))
                .andExpect(status().isNotFound());

        jdbc.update("update LANGUAGE set IS_ACTIVE = 0 where CODE = 'vi'");
        mockMvc.perform(get("/api/v1/modules/java-stream-api/menu?lang=vi"))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/modules/java-stream-api/knowledge/fundamentals/stream-basics?lang=vi"))
                .andExpect(status().isNotFound());
    }

    @Test
    void chapterAndSectionSlugResolutionIsOwnerScopedAndOrderedBySortThenId() throws Exception {
        jdbc.update("""
                insert into MODULE_MENU (ID, MODULE_ID, NODE_TYPE, SLUG, SORT_ORDER, IS_ACTIVE)
                values (702, 100, 'CHAPTER', 'advanced', 1, 1)
                """);
        jdbc.update("""
                insert into MODULE_MENU
                    (ID, MENU_PARENT_ID, NODE_TYPE, MENU_PARENT_TYPE, SLUG, SORT_ORDER, IS_ACTIVE)
                values (703, 702, 'SECTION', 'CHAPTER', 'stream-basics', 2, 1)
                """);
        jdbc.update("""
                insert into MODULE_MENU
                    (ID, MENU_PARENT_ID, NODE_TYPE, MENU_PARENT_TYPE, SLUG, SORT_ORDER, IS_ACTIVE)
                values (704, 700, 'SECTION', 'CHAPTER', 'second', 1, 1)
                """);
        jdbc.update("""
                insert into MODULE_MENU_LANGUAGE (MENU_ID, LANGUAGE_CODE, TITLE)
                values (702, 'vi', 'Nâng cao')
                """);
        jdbc.update("""
                insert into MODULE_MENU_LANGUAGE (MENU_ID, LANGUAGE_CODE, TITLE)
                values (703, 'vi', 'Stream nâng cao')
                """);
        jdbc.update("""
                insert into MODULE_MENU_LANGUAGE (MENU_ID, LANGUAGE_CODE, TITLE)
                values (704, 'vi', 'Thứ hai')
                """);
        jdbc.update("""
                insert into MODULE_KNOWLEDGE (ID, MENU_ID, MENU_NODE_TYPE, DIFFICULT, IS_ACTIVE)
                values (801, 703, 'SECTION', 'ADVANCED', 1)
                """);
        jdbc.update("""
                insert into MODULE_KNOWLEDGE_LANGUAGE
                    (KNOWLEDGE_ID, LANGUAGE_CODE, CONTENT, STATUS, FOCUS,
                     PROBLEM_SOLVED, MECHANISM, APPLICATION_VALUE, SUMMARY)
                values (801, 'vi', '# Advanced', 'WRITTEN', 'Focus', 'Problem', 'Mechanism', 'Value', 'Summary')
                """);

        mockMvc.perform(get("/api/v1/modules/java-stream-api/menu?lang=vi"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.chapters.length()").value(2))
                .andExpect(jsonPath("$.chapters[0].id").value(700))
                .andExpect(jsonPath("$.chapters[1].id").value(702))
                .andExpect(jsonPath("$.chapters[0].sections[0].id").value(701))
                .andExpect(jsonPath("$.chapters[0].sections[1].id").value(704))
                .andExpect(jsonPath("$.chapters[1].sections[0].id").value(703))
                .andExpect(jsonPath("$.chapters[0].lessonCount").value(1))
                .andExpect(jsonPath("$.chapters[1].lessonCount").value(1));

        mockMvc.perform(get("/api/v1/modules/java-stream-api/knowledge/fundamentals/stream-basics?lang=vi"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.knowledgeId").value(800));
        mockMvc.perform(get("/api/v1/modules/java-stream-api/knowledge/advanced/stream-basics?lang=vi"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.knowledgeId").value(801))
                .andExpect(jsonPath("$.markdownContent").value("# Advanced"));
        mockMvc.perform(get("/api/v1/modules/java-stream-api/knowledge/advanced/second?lang=vi"))
                .andExpect(status().isNotFound());
    }

    @Test
    void menuKeepsUnwrittenLessonsUnavailableAndDetailReturnsNotFound() throws Exception {
        jdbc.update("update MODULE_KNOWLEDGE_LANGUAGE set STATUS = 'DRAFT' where KNOWLEDGE_ID = 800");
        assertUnpublishedLesson(true);

        jdbc.update("update MODULE_KNOWLEDGE_LANGUAGE set STATUS = 'WRITTEN', CONTENT = '  ' where KNOWLEDGE_ID = 800");
        assertUnpublishedLesson(true);

        jdbc.update("update MODULE_KNOWLEDGE_LANGUAGE set CONTENT = null where KNOWLEDGE_ID = 800");
        assertUnpublishedLesson(true);

        jdbc.update("update MODULE_KNOWLEDGE_LANGUAGE set CONTENT = '# Stream' where KNOWLEDGE_ID = 800");
        jdbc.update("update MODULE_KNOWLEDGE set DIFFICULT = null where ID = 800");
        assertUnpublishedLesson(true);
    }

    private void assertUnpublishedLesson(boolean hasSummary) throws Exception {
        mockMvc.perform(get("/api/v1/modules/java-stream-api/menu?lang=vi"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.chapters[0].lessonCount").value(0))
                .andExpect(jsonPath("$.chapters[0].sections[0].available").value(false))
                .andExpect(jsonPath("$.chapters[0].sections[0].hasSummary").value(hasSummary));
        mockMvc.perform(get("/api/v1/modules/java-stream-api/knowledge/fundamentals/stream-basics?lang=vi"))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/modules/java-stream-api/chapters/fundamentals/insights?lang=vi"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lessons.length()").value(0));
    }

    @Test
    void writtenMarkdownWithNullOptionalMetadataIsPublicInAllThreeApis() throws Exception {
        jdbc.update("""
                update MODULE_KNOWLEDGE_LANGUAGE
                set FOCUS = null, PROBLEM_SOLVED = null, MECHANISM = null,
                    APPLICATION_VALUE = null, SUMMARY = null
                where KNOWLEDGE_ID = 800
                """);

        mockMvc.perform(get("/api/v1/modules/java-stream-api/menu?lang=vi"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.chapters[0].lessonCount").value(1))
                .andExpect(jsonPath("$.chapters[0].sections[0].available").value(true))
                .andExpect(jsonPath("$.chapters[0].sections[0].knowledgeId").value(800))
                .andExpect(jsonPath("$.chapters[0].sections[0].difficulty").value("INTERMEDIATE"))
                .andExpect(jsonPath("$.chapters[0].sections[0].hasSummary").value(false));

        mockMvc.perform(get("/api/v1/modules/java-stream-api/knowledge/fundamentals/stream-basics?lang=vi"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.knowledgeId").value(800))
                .andExpect(jsonPath("$.difficulty").value("INTERMEDIATE"))
                .andExpect(jsonPath("$.markdownContent").value("# Stream"))
                .andExpect(jsonPath("$.focus").value(nullValue()))
                .andExpect(jsonPath("$.problemSolved").value(nullValue()))
                .andExpect(jsonPath("$.mechanism").value(nullValue()))
                .andExpect(jsonPath("$.applicationValue").value(nullValue()))
                .andExpect(jsonPath("$.summary").value(nullValue()));

        mockMvc.perform(get("/api/v1/modules/java-stream-api/chapters/fundamentals/insights?lang=vi"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lessons.length()").value(1))
                .andExpect(jsonPath("$.lessons[0].knowledgeId").value(800))
                .andExpect(jsonPath("$.lessons[0].focus").value(nullValue()))
                .andExpect(jsonPath("$.lessons[0].problemSolved").value(nullValue()))
                .andExpect(jsonPath("$.lessons[0].mechanism").value(nullValue()))
                .andExpect(jsonPath("$.lessons[0].applicationValue").value(nullValue()));
    }

    @Test
    void whitespaceOnlyOptionalMetadataSerializesAsNullWithoutHidingLesson() throws Exception {
        jdbc.update("""
                update MODULE_KNOWLEDGE_LANGUAGE
                set FOCUS = '  ', PROBLEM_SOLVED = '   ', MECHANISM = '  ',
                    APPLICATION_VALUE = ' ', SUMMARY = '   '
                where KNOWLEDGE_ID = 800
                """);

        mockMvc.perform(get("/api/v1/modules/java-stream-api/menu?lang=vi"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.chapters[0].lessonCount").value(1))
                .andExpect(jsonPath("$.chapters[0].sections[0].available").value(true))
                .andExpect(jsonPath("$.chapters[0].sections[0].hasSummary").value(false));
        mockMvc.perform(get("/api/v1/modules/java-stream-api/knowledge/fundamentals/stream-basics?lang=vi"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.focus").value(nullValue()))
                .andExpect(jsonPath("$.problemSolved").value(nullValue()))
                .andExpect(jsonPath("$.mechanism").value(nullValue()))
                .andExpect(jsonPath("$.applicationValue").value(nullValue()))
                .andExpect(jsonPath("$.summary").value(nullValue()));
        mockMvc.perform(get("/api/v1/modules/java-stream-api/chapters/fundamentals/insights?lang=vi"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lessons.length()").value(1))
                .andExpect(jsonPath("$.lessons[0].focus").value(nullValue()))
                .andExpect(jsonPath("$.lessons[0].problemSolved").value(nullValue()))
                .andExpect(jsonPath("$.lessons[0].mechanism").value(nullValue()))
                .andExpect(jsonPath("$.lessons[0].applicationValue").value(nullValue()));
    }

    @Test
    void inactiveKnowledgeAndMenuAncestorsCannotBeRead() throws Exception {
        jdbc.update("update MODULE_KNOWLEDGE set IS_ACTIVE = 0 where ID = 800");
        mockMvc.perform(get("/api/v1/modules/java-stream-api/menu?lang=vi"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.chapters[0].sections[0].available").value(false));
        mockMvc.perform(get("/api/v1/modules/java-stream-api/knowledge/fundamentals/stream-basics?lang=vi"))
                .andExpect(status().isNotFound());

        jdbc.update("update MODULE_KNOWLEDGE set IS_ACTIVE = 1 where ID = 800");
        jdbc.update("update MODULE_MENU set IS_ACTIVE = 0 where ID = 701");
        mockMvc.perform(get("/api/v1/modules/java-stream-api/menu?lang=vi"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.chapters[0].sections.length()").value(0));
        mockMvc.perform(get("/api/v1/modules/java-stream-api/knowledge/fundamentals/stream-basics?lang=vi"))
                .andExpect(status().isNotFound());

        jdbc.update("update MODULE_MENU set IS_ACTIVE = 1 where ID = 701");
        jdbc.update("update MODULE_MENU set IS_ACTIVE = 0 where ID = 700");
        mockMvc.perform(get("/api/v1/modules/java-stream-api/menu?lang=vi"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.chapters.length()").value(0));
        mockMvc.perform(get("/api/v1/modules/java-stream-api/knowledge/fundamentals/stream-basics?lang=vi"))
                .andExpect(status().isNotFound());
    }

    @Test
    void menuAndKnowledgeRespectSharedModuleAndTopicVisibility() throws Exception {
        jdbc.update("update TOPIC_GROUP set IS_ACTIVE = 0 where ID = 200");
        mockMvc.perform(get("/api/v1/modules/java-stream-api/menu?lang=vi"))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/modules/java-stream-api/knowledge/fundamentals/stream-basics?lang=vi"))
                .andExpect(status().isNotFound());

        jdbc.update("update TOPIC_GROUP set IS_ACTIVE = 1 where ID = 200");
        jdbc.update("update TOPIC set STATUS = 'HIDDEN' where ID = 10");
        mockMvc.perform(get("/api/v1/modules/java-stream-api/menu?lang=vi"))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/modules/java-stream-api/knowledge/fundamentals/stream-basics?lang=vi"))
                .andExpect(status().isNotFound());

        jdbc.update("update TOPIC set STATUS = 'AVAILABLE' where ID = 10");
        jdbc.update("update TOPIC_GROUP_MODULE set IS_ACTIVE = 0 where ID = 300");
        mockMvc.perform(get("/api/v1/modules/java-stream-api/menu?lang=vi"))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/modules/java-stream-api/knowledge/fundamentals/stream-basics?lang=vi"))
                .andExpect(status().isNotFound());
    }

    @Test
    void menuAndKnowledgeHideInactiveModulesAndCategories() throws Exception {
        jdbc.update("update MODULE set IS_ACTIVE = 0 where ID = 100");
        mockMvc.perform(get("/api/v1/modules/java-stream-api/menu?lang=vi"))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/modules/java-stream-api/knowledge/fundamentals/stream-basics?lang=vi"))
                .andExpect(status().isNotFound());

        jdbc.update("update MODULE set IS_ACTIVE = 1 where ID = 100");
        jdbc.update("update CATEGORY set IS_ACTIVE = 0 where ID = 1");
        mockMvc.perform(get("/api/v1/modules/java-stream-api/menu?lang=vi"))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/modules/java-stream-api/knowledge/fundamentals/stream-basics?lang=vi"))
                .andExpect(status().isNotFound());
    }

    @Test
    void untranslatedSectionsNeverFallbackOrLeakDetails() throws Exception {
        jdbc.update("delete from MODULE_MENU_LANGUAGE where MENU_ID = 701 and LANGUAGE_CODE = 'vi'");
        mockMvc.perform(get("/api/v1/modules/java-stream-api/menu?lang=vi"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.chapters[0].title").value("Nền tảng"))
                .andExpect(jsonPath("$.chapters[0].sections.length()").value(0))
                .andExpect(jsonPath("$.chapters[0].lessonCount").value(0));
        mockMvc.perform(get("/api/v1/modules/java-stream-api/knowledge/fundamentals/stream-basics?lang=vi"))
                .andExpect(status().isNotFound());

        jdbc.update("delete from MODULE_MENU_LANGUAGE where MENU_ID = 700 and LANGUAGE_CODE = 'vi'");
        mockMvc.perform(get("/api/v1/modules/java-stream-api/menu?lang=vi"))
                .andExpect(status().isNotFound());
    }

    @Test
    void menuAndKnowledgeRejectMalformedOrUnknownSlugs() throws Exception {
        mockMvc.perform(get("/api/v1/modules/java-stream-api/knowledge/advanced/stream-basics?lang=vi"))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/modules/nonexistent/menu?lang=vi"))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/modules/java-stream-api/knowledge/fundamentals/stream%20basics?lang=vi"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void chapterInsightsContainOnlyPublishedMetadataWithoutAnyClobs() throws Exception {
        mockMvc.perform(get("/api/v1/modules/java-stream-api/chapters/fundamentals/insights?lang=vi"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.moduleSlug").value("java-stream-api"))
                .andExpect(jsonPath("$.languageCode").value("vi"))
                .andExpect(jsonPath("$.chapter.id").value(700))
                .andExpect(jsonPath("$.chapter.title").value("Nền tảng"))
                .andExpect(jsonPath("$.lessons.length()").value(1))
                .andExpect(jsonPath("$.lessons[0].id").value(701))
                .andExpect(jsonPath("$.lessons[0].slug").value("stream-basics"))
                .andExpect(jsonPath("$.lessons[0].title").value("Stream cơ bản"))
                .andExpect(jsonPath("$.lessons[0].sortOrder").value(1))
                .andExpect(jsonPath("$.lessons[0].knowledgeId").value(800))
                .andExpect(jsonPath("$.lessons[0].difficulty").value("INTERMEDIATE"))
                .andExpect(jsonPath("$.lessons[0].focus").value("Stream pipeline"))
                .andExpect(jsonPath("$.lessons[0].problemSolved").value("Xử lý collection"))
                .andExpect(jsonPath("$.lessons[0].mechanism").value("Lazy evaluation"))
                .andExpect(jsonPath("$.lessons[0].applicationValue").value("Viết pipeline"))
                .andExpect(jsonPath("$.lessons[0].summary").doesNotExist())
                .andExpect(jsonPath("$.lessons[0].markdownContent").doesNotExist());

        jdbc.update("update MODULE_KNOWLEDGE_LANGUAGE set SUMMARY = '   ' where KNOWLEDGE_ID = 800");
        mockMvc.perform(get("/api/v1/modules/java-stream-api/chapters/fundamentals/insights?lang=vi"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lessons.length()").value(1))
                .andExpect(jsonPath("$.lessons[0].knowledgeId").value(800));
        mockMvc.perform(get("/api/v1/modules/java-stream-api/menu?lang=vi"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.chapters[0].sections[0].available").value(true))
                .andExpect(jsonPath("$.chapters[0].sections[0].hasSummary").value(false));
    }

    @Test
    void chapterInsightsAreScopedToVisibleModuleChapterAndLocale() throws Exception {
        mockMvc.perform(get("/api/v1/modules/java-collections/chapters/fundamentals/insights?lang=vi"))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/modules/java-stream-api/chapters/fundamentals/insights?lang=en"))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/modules/java-stream-api/chapters/fundamentals/insights"))
                .andExpect(status().isBadRequest());

        jdbc.update("update TOPIC_GROUP set IS_ACTIVE = 0 where ID = 200");
        mockMvc.perform(get("/api/v1/modules/java-stream-api/chapters/fundamentals/insights?lang=vi"))
                .andExpect(status().isNotFound());
    }
}
