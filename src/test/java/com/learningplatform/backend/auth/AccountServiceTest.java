package com.learningplatform.backend.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import com.learningplatform.backend.auth.service.AccountService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;

class AccountServiceTest {

    private JdbcTemplate jdbc;
    private AccountService accounts;

    @BeforeEach
    void createSchema() {
        var datasource = new DriverManagerDataSource(
                "jdbc:h2:mem:accounts" + System.nanoTime() + ";MODE=Oracle;DB_CLOSE_DELAY=-1",
                "sa", "");
        jdbc = new JdbcTemplate(datasource);
        jdbc.execute("""
                CREATE TABLE APP_USER (
                  ID BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                  GOOGLE_SUB VARCHAR(255) UNIQUE NOT NULL,
                  EMAIL VARCHAR(320), EMAIL_VERIFIED INTEGER,
                  DISPLAY_NAME VARCHAR(255), AVATAR_URL VARCHAR(1024),
                  STATUS VARCHAR(16) DEFAULT 'ACTIVE' NOT NULL,
                  CREATED_AT TIMESTAMP WITH TIME ZONE,
                  UPDATED_AT TIMESTAMP WITH TIME ZONE,
                  LAST_LOGIN_AT TIMESTAMP WITH TIME ZONE
                )
                """);
        jdbc.execute("""
                CREATE TABLE APP_ROLE (
                  ID BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                  CODE VARCHAR(32) UNIQUE NOT NULL
                )
                """);
        jdbc.execute("""
                CREATE TABLE APP_USER_ROLE (
                  USER_ID BIGINT NOT NULL, ROLE_ID BIGINT NOT NULL,
                  PRIMARY KEY (USER_ID, ROLE_ID)
                )
                """);
        jdbc.update("INSERT INTO APP_ROLE (CODE) VALUES ('USER')");
        jdbc.update("INSERT INTO APP_ROLE (CODE) VALUES ('ADMIN')");
        accounts = new AccountService(jdbc);
    }

    @Test
    void createsUserAndPreservesRolesAcrossRepeatedGoogleLogins() {
        accounts.recordGoogleLogin(googleUser("person@gmail.com", "Learner"));
        var profile = accounts.findByGoogleSub("google-identity-1").orElseThrow();
        assertEquals("person@gmail.com", profile.email());
        assertEquals("Learner", profile.displayName());
        assertEquals(List.of("USER"), profile.roles());
        assertFalse(accounts.hasRole("google-identity-1", "ADMIN"));

        jdbc.update("""
                INSERT INTO APP_USER_ROLE (USER_ID, ROLE_ID)
                SELECT ?, ID FROM APP_ROLE WHERE CODE = 'ADMIN'
                """, profile.id());
        accounts.recordGoogleLogin(googleUser("updated@gmail.com", "Updated"));
        var after = accounts.findByGoogleSub("google-identity-1").orElseThrow();
        assertEquals(profile.id(), after.id());
        assertEquals("updated@gmail.com", after.email());
        assertEquals(List.of("ADMIN", "USER"), after.roles());
        assertTrue(accounts.hasRole("google-identity-1", "ADMIN"));
        assertEquals(1L, accounts.listUsers(0, 20).total());
        assertEquals(List.of("ADMIN", "USER"), accounts.listUsers(0, 20).items().getFirst().roles());
        assertEquals(1, jdbc.queryForObject(
                "SELECT COUNT(*) FROM APP_USER WHERE GOOGLE_SUB = 'google-identity-1'",
                Integer.class));
    }

    @Test
    void disabledUserCannotLoginOrUseAdminPermissions() {
        accounts.recordGoogleLogin(googleUser("person@gmail.com", "Learner"));
        jdbc.update("UPDATE APP_USER SET STATUS = 'DISABLED' WHERE GOOGLE_SUB = 'google-identity-1'");
        assertThrows(IllegalStateException.class,
                () -> accounts.recordGoogleLogin(googleUser("person@gmail.com", "Learner")));
        assertTrue(accounts.findByGoogleSub("google-identity-1").isEmpty());
        assertFalse(accounts.hasRole("google-identity-1", "USER"));
    }

    private DefaultOidcUser googleUser(String email, String displayName) {
        var claims = Map.<String, Object>of(
                "sub", "google-identity-1",
                "email", email,
                "email_verified", true,
                "name", displayName,
                "picture", "https://example.com/avatar.png");
        var token = new OidcIdToken("id-token", Instant.now(),
                Instant.now().plusSeconds(3600), claims);
        return new DefaultOidcUser(
                List.of(new SimpleGrantedAuthority("OIDC_USER")), token);
    }
}
