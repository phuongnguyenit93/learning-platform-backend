package com.learningplatform.backend;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import com.learningplatform.backend.shared.auth.model.AppRole;
import com.learningplatform.backend.shared.auth.model.AppUserRole;
import com.learningplatform.backend.shared.auth.repository.AppRoleRepository;
import com.learningplatform.backend.shared.auth.repository.AppUserRepository;
import com.learningplatform.backend.shared.auth.repository.AppUserRoleRepository;
import com.learningplatform.backend.shared.auth.service.AccountService;
import jakarta.persistence.EntityManager;
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
        "spring.datasource.url=jdbc:h2:mem:adminCatalog;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.session.jdbc.initialize-schema=always"
})
class AdminCatalogIntegrationTest {

    @Autowired private MockMvc mvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private EntityManager entityManager;
    @Autowired private AccountService accounts;
    @Autowired private AppUserRepository users;
    @Autowired private AppRoleRepository roles;
    @Autowired private AppUserRoleRepository userRoles;

    @BeforeEach
    void setup() {
        var adminRole = roles.save(new AppRole("ADMIN"));
        roles.save(new AppRole("USER"));
        accounts.recordGoogleLogin(googleUser("catalog-admin"));
        accounts.recordGoogleLogin(googleUser("catalog-user"));
        var admin = users.findByGoogleSub("catalog-admin").orElseThrow();
        userRoles.save(new AppUserRole(admin, adminRole));
    }

    @Test
    void rejectsAnonymousAndOrdinaryUsersAndRequiresCsrf() throws Exception {
        mvc.perform(get("/api/admin/categories"))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/admin/categories")
                        .with(oidcLogin().idToken(token -> token.subject("catalog-user"))))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/admin/categories")
                        .with(oidcLogin().idToken(token -> token.subject("catalog-user")))
                        .with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"NO\",\"name\":\"Denied\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/admin/categories")
                        .with(oidcLogin().idToken(token -> token.subject("catalog-admin")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"NO\",\"name\":\"Denied\"}"))
                .andExpect(status().isForbidden());
        assertEquals(0, jdbc.queryForObject("select count(*) from CATEGORY", Integer.class));
    }

    @Test
    void allowsCredentialedAdminPreflightFromFrontend() throws Exception {
        mvc.perform(options("/api/admin/categories")
                        .header("Origin", "https://nguyendodinhphuong.name.vn")
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "content-type,x-csrf-token"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "https://nguyendodinhphuong.name.vn"))
                .andExpect(header().string("Access-Control-Allow-Credentials", "true"));
    }

    @Test
    void adminCanCreateAndEditCategoryAndTopicWithoutOverwritingIconOrStatus() throws Exception {
        mvc.perform(post("/api/admin/categories")
                        .with(oidcLogin().idToken(token -> token.subject("catalog-admin"))).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"backend\",\"name\":\"Backend\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("BACKEND"))
                .andExpect(jsonPath("$.sortOrder").value(1));

        Long categoryId = jdbc.queryForObject("select id from CATEGORY where CODE = 'BACKEND'", Long.class);
        mvc.perform(get("/api/admin/categories")
                        .with(oidcLogin().idToken(token -> token.subject("catalog-admin"))))
                .andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(1)));

        mvc.perform(post("/api/admin/categories/" + categoryId + "/topics")
                        .with(oidcLogin().idToken(token -> token.subject("catalog-admin"))).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"java\",\"slug\":\"java\",\"name\":\"Java\",\"description\":\"Java lessons\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sortOrder").value(1))
                .andExpect(jsonPath("$.status").value("COMING_SOON"))
                .andExpect(jsonPath("$.iconSvg").isEmpty());

        Long topicId = jdbc.queryForObject("select id from TOPIC where CODE = 'JAVA'", Long.class);
        jdbc.update("update TOPIC set ICON_SVG = '<svg></svg>', STATUS = 'HIDDEN' where ID = ?", topicId);
        // Simulate a DB-only icon/status edit outside Hibernate's first-level cache.
        entityManager.clear();
        mvc.perform(put("/api/admin/topics/" + topicId)
                        .with(oidcLogin().idToken(token -> token.subject("catalog-admin"))).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"JAVA_CORE\",\"slug\":\"java-core\",\"name\":\"Java Core\",\"description\":\"\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slug").value("java-core"))
                .andExpect(jsonPath("$.status").value("HIDDEN"))
                .andExpect(jsonPath("$.iconSvg").value("<svg></svg>"));

        mvc.perform(put("/api/admin/categories/" + categoryId)
                        .with(oidcLogin().idToken(token -> token.subject("catalog-admin"))).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"BACKEND_NEW\",\"name\":\"Backend new\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Backend new"));

        mvc.perform(get("/api/admin/topics")
                        .with(oidcLogin().idToken(token -> token.subject("catalog-admin"))))
                .andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(1)));
        mvc.perform(get("/api/v1/topics"))
                .andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(0)));
        assertEquals("HIDDEN", jdbc.queryForObject("select STATUS from TOPIC where ID = ?", String.class, topicId));
    }

    @Test
    void rejectsDuplicatesBadInputAndMissingRows() throws Exception {
        var admin = oidcLogin().idToken(token -> token.subject("catalog-admin"));
        mvc.perform(post("/api/admin/categories").with(admin).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"A\",\"name\":\"One\"}"))
                .andExpect(status().isCreated());
        mvc.perform(post("/api/admin/categories")
                        .with(oidcLogin().idToken(token -> token.subject("catalog-admin"))).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"A\",\"name\":\"Duplicate\"}"))
                .andExpect(status().isConflict());
        mvc.perform(post("/api/admin/categories")
                        .with(oidcLogin().idToken(token -> token.subject("catalog-admin"))).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"bad code\",\"name\":\"Invalid\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(put("/api/admin/topics/9999")
                        .with(oidcLogin().idToken(token -> token.subject("catalog-admin"))).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"JAVA\",\"slug\":\"java\",\"name\":\"Java\"}"))
                .andExpect(status().isNotFound());
    }

    private DefaultOidcUser googleUser(String subject) {
        var claims = Map.<String, Object>of("sub", subject, "email", subject + "@gmail.com",
                "email_verified", true, "name", subject);
        return new DefaultOidcUser(List.of(new SimpleGrantedAuthority("OIDC_USER")),
                new OidcIdToken("token", Instant.now(), Instant.now().plusSeconds(3600), claims));
    }
}
