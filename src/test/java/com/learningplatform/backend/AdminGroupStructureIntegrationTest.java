package com.learningplatform.backend;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.learningplatform.backend.shared.auth.model.AppRole;
import com.learningplatform.backend.shared.auth.model.AppUserRole;
import com.learningplatform.backend.shared.auth.repository.AppRoleRepository;
import com.learningplatform.backend.shared.auth.repository.AppUserRepository;
import com.learningplatform.backend.shared.auth.repository.AppUserRoleRepository;
import com.learningplatform.backend.shared.auth.service.AccountService;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("oracle")
@Transactional
@TestPropertySource(properties = {
        "app.auth.google.enabled=true",
        "spring.security.oauth2.client.registration.google.client-id=test-client",
        "spring.security.oauth2.client.registration.google.client-secret=test-secret",
        "spring.datasource.url=jdbc:h2:mem:adminGroups;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.session.jdbc.initialize-schema=always"
})
class AdminGroupStructureIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired ObjectMapper json;
    @Autowired EntityManager entityManager;
    @Autowired AccountService accounts;
    @Autowired AppRoleRepository roles;
    @Autowired AppUserRepository users;
    @Autowired AppUserRoleRepository userRoles;

    @BeforeEach
    void seed() {
        var adminRole = roles.save(new AppRole("ADMIN"));
        roles.save(new AppRole("USER"));
        accounts.recordGoogleLogin(google("structure-admin"));
        accounts.recordGoogleLogin(google("structure-user"));
        userRoles.save(new AppUserRole(users.findByGoogleSub("structure-admin").orElseThrow(), adminRole));
        jdbc.update("insert into CATEGORY (ID, CODE, NAME, SORT_ORDER, IS_ACTIVE) values (50, 'BACKEND', 'Backend', 1, 1)");
        jdbc.update("insert into TOPIC (ID, CATEGORY_ID, CODE, SLUG, NAME, STATUS, SORT_ORDER) values (60, 50, 'JAVA', 'java', 'Java', 'AVAILABLE', 1)");
        jdbc.update("insert into TOPIC (ID, CATEGORY_ID, CODE, SLUG, NAME, STATUS, SORT_ORDER) values (61, 50, 'SPRING', 'spring', 'Spring', 'AVAILABLE', 2)");
        jdbc.update("insert into MODULE (ID, SERVICE_NAME, SLUG, NAME, IS_ACTIVE) values (100, 'MODULE_A', 'module-a', 'Module A', 1)");
        jdbc.update("insert into MODULE (ID, SERVICE_NAME, SLUG, NAME, IS_ACTIVE) values (101, 'MODULE_B', 'module-b', 'Module B', 0)");
        jdbc.update("insert into TOPIC_GROUP (ID, TOPIC_ID, GROUP_NAME, SLUG, SORT_ORDER, IS_ACTIVE) values (200, 60, 'Group A', 'group-a', 1, 1)");
        jdbc.update("insert into TOPIC_GROUP (ID, TOPIC_ID, GROUP_NAME, SLUG, PARENT_GROUP_ID, SORT_ORDER, IS_ACTIVE) values (201, 60, 'Group B', 'group-b', 200, 1, 0)");
        jdbc.update("insert into TOPIC_GROUP (ID, TOPIC_ID, GROUP_NAME, SLUG, SORT_ORDER, IS_ACTIVE) values (202, 61, 'Spring Group', 'spring-group', 1, 1)");
        jdbc.update("insert into TOPIC_GROUP_MODULE (ID, GROUP_ID, MODULE_ID, SORT_ORDER, IS_ACTIVE) values (300, 200, 100, 2, 1)");
        jdbc.update("insert into TOPIC_GROUP_MODULE (ID, TOPIC_ID, MODULE_ID, SORT_ORDER, IS_ACTIVE) values (301, 60, 101, 3, 0)");
        entityManager.flush();
        entityManager.clear();
    }

    @Test
    void adminSeesInactiveNodesAndCanCreateRootThenEditWithoutChangingTopic() throws Exception {
        mvc.perform(get("/api/admin/topics/60/structure").with(asAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nodes[0].id").value(200))
                .andExpect(jsonPath("$.nodes[0].children[0].active").value(false))
                .andExpect(jsonPath("$.nodes[1].kind").value("MODULE"))
                .andExpect(jsonPath("$.nodes[1].active").value(false));
        mvc.perform(get("/api/admin/topics/60/structure"))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/admin/topics/60/structure").with(asUser()))
                .andExpect(status().isForbidden());
        String revision = snapshot().get("revision").asText();
        String add = json.writeValueAsString(Map.of("name", "New root", "slug", "new-root",
                "active", true, "revision", revision));
        mvc.perform(post("/api/admin/topics/60/groups").with(asUser()).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(add))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/admin/topics/60/groups").with(asAdmin())
                .contentType(MediaType.APPLICATION_JSON).content(add))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/admin/topics/60/groups").with(asAdmin()).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(add))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.topicId").value(60))
                .andExpect(jsonPath("$.parentGroupId").isEmpty())
                .andExpect(jsonPath("$.sortOrder").value(4));
        mvc.perform(post("/api/admin/topics/60/groups").with(asAdmin()).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(add))
                .andExpect(status().isConflict());
        String latestRevision = snapshot().get("revision").asText();
        mvc.perform(post("/api/admin/topics/60/groups").with(asAdmin()).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("name", "Collision", "slug", "module-a",
                        "active", true, "revision", latestRevision))))
                .andExpect(status().isConflict());
        mvc.perform(put("/api/admin/groups/200").with(asAdmin()).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("name", "Renamed", "slug", "renamed-group",
                        "active", false, "revision", latestRevision))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.topicId").value(60))
                .andExpect(jsonPath("$.active").value(false));
        mvc.perform(get("/api/v1/topics/60/structure"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.root.children[0].name").value("New root"));
    }

    @Test
    void savesMixedNestedTreeAndRejectsStaleRevisionWithoutPartialWrites() throws Exception {
        String revision = snapshot().get("revision").asText();
        String changed = json.writeValueAsString(Map.of("revision", revision, "nodes", List.of(
                Map.of("kind", "MODULE", "id", 301),
                Map.of("kind", "GROUP", "id", 201, "children", List.of(
                        Map.of("kind", "GROUP", "id", 200, "children", List.of(
                                Map.of("kind", "MODULE", "id", 300))))))));
        String response = mvc.perform(put("/api/admin/topics/60/structure").with(asAdmin()).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(changed))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertNotEquals(revision, json.readTree(response).get("revision").asText());
        assertEquals(201L, jdbc.queryForObject("select PARENT_GROUP_ID from TOPIC_GROUP where ID=200", Long.class));
        assertEquals(null, jdbc.queryForObject("select PARENT_GROUP_ID from TOPIC_GROUP where ID=201", Long.class));
        assertEquals(1, jdbc.queryForObject("select SORT_ORDER from TOPIC_GROUP where ID=200", Integer.class));
        assertEquals(2, jdbc.queryForObject("select SORT_ORDER from TOPIC_GROUP where ID=201", Integer.class));
        assertEquals(1, jdbc.queryForObject("select SORT_ORDER from TOPIC_GROUP_MODULE where ID=301", Integer.class));
        assertEquals(200L, jdbc.queryForObject("select GROUP_ID from TOPIC_GROUP_MODULE where ID=300", Long.class));

        mvc.perform(put("/api/admin/topics/60/structure").with(asAdmin()).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(changed))
                .andExpect(status().isConflict());
        assertEquals(201L, jdbc.queryForObject("select PARENT_GROUP_ID from TOPIC_GROUP where ID=200", Long.class));
    }

    @Test
    void invalidTopologyCannotWriteAnythingAndCrossTopicNodeIsRejected() throws Exception {
        String revision = snapshot().get("revision").asText();
        var repeated = Map.of("revision", revision, "nodes", List.of(
                Map.of("kind", "GROUP", "id", 200, "children", List.of(
                        Map.of("kind", "GROUP", "id", 201, "children", List.of()),
                        Map.of("kind", "MODULE", "id", 300),
                        Map.of("kind", "MODULE", "id", 300))),
                Map.of("kind", "MODULE", "id", 301)));
        mvc.perform(put("/api/admin/topics/60/structure").with(asAdmin()).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(repeated)))
                .andExpect(status().isBadRequest());
        assertEquals(200L, jdbc.queryForObject("select PARENT_GROUP_ID from TOPIC_GROUP where ID=201", Long.class));
        assertEquals(2, jdbc.queryForObject("select SORT_ORDER from TOPIC_GROUP_MODULE where ID=300", Integer.class));

        var foreign = Map.of("revision", revision, "nodes", List.of(
                Map.of("kind", "GROUP", "id", 202, "children", List.of()),
                Map.of("kind", "GROUP", "id", 200, "children", List.of(
                        Map.of("kind", "GROUP", "id", 201, "children", List.of()),
                        Map.of("kind", "MODULE", "id", 300))),
                Map.of("kind", "MODULE", "id", 301)));
        mvc.perform(put("/api/admin/topics/60/structure").with(asAdmin()).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(foreign)))
                .andExpect(status().isBadRequest());
    }

    private JsonNode snapshot() throws Exception {
        String data = mvc.perform(get("/api/admin/topics/60/structure").with(asAdmin()))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return json.readTree(data);
    }

    private org.springframework.test.web.servlet.request.RequestPostProcessor asAdmin() {
        return oidcLogin().idToken(token -> token.subject("structure-admin"));
    }

    private org.springframework.test.web.servlet.request.RequestPostProcessor asUser() {
        return oidcLogin().idToken(token -> token.subject("structure-user"));
    }

    private DefaultOidcUser google(String subject) {
        var claims = Map.<String, Object>of("sub", subject, "email", subject + "@gmail.com",
                "email_verified", true, "name", subject);
        return new DefaultOidcUser(List.of(new SimpleGrantedAuthority("OIDC_USER")),
                new OidcIdToken("token", Instant.now(), Instant.now().plusSeconds(3600), claims));
    }
}
