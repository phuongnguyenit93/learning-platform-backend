package com.learningplatform.backend.shared.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import com.learningplatform.backend.shared.auth.model.AppRole;
import com.learningplatform.backend.shared.auth.model.AppUserRole;
import com.learningplatform.backend.shared.auth.repository.AppRoleRepository;
import com.learningplatform.backend.shared.auth.repository.AppUserRepository;
import com.learningplatform.backend.shared.auth.repository.AppUserRoleRepository;
import com.learningplatform.backend.shared.auth.service.AccountService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
@ActiveProfiles("oracle")
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:accountJpa;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class AccountServiceTest {

    @Autowired private AccountService accounts;
    @Autowired private AppUserRepository users;
    @Autowired private AppRoleRepository roles;
    @Autowired private AppUserRoleRepository userRoles;

    @BeforeEach
    void seedRoles() {
        roles.save(new AppRole("USER"));
        roles.save(new AppRole("ADMIN"));
    }

    @Test
    void createsUserAndPreservesRolesAcrossRepeatedGoogleLogins() {
        accounts.recordGoogleLogin(googleUser("google-identity-1", "person@gmail.com", "Learner"));
        var profile = accounts.findByGoogleSub("google-identity-1").orElseThrow();
        assertEquals("person@gmail.com", profile.email());
        assertEquals("Learner", profile.displayName());
        assertEquals(List.of("USER"), profile.roles());
        assertFalse(accounts.hasRole("google-identity-1", "ADMIN"));

        var account = users.findByGoogleSub("google-identity-1").orElseThrow();
        var adminRole = roles.findByCode("ADMIN").orElseThrow();
        userRoles.saveAndFlush(new AppUserRole(account, adminRole));

        accounts.recordGoogleLogin(googleUser("google-identity-1", "updated@gmail.com", "Updated"));
        var after = accounts.findByGoogleSub("google-identity-1").orElseThrow();
        assertEquals(profile.id(), after.id());
        assertEquals("updated@gmail.com", after.email());
        assertEquals(List.of("ADMIN", "USER"), after.roles());
        assertTrue(accounts.hasRole("google-identity-1", "ADMIN"));
        assertEquals(1L, accounts.listUsers(0, 20).total());
        assertEquals(List.of("ADMIN", "USER"), accounts.listUsers(0, 20).items().getFirst().roles());
        assertEquals(1L, users.count());
        assertEquals(2L, userRoles.count());
    }

    @Test
    void disabledUserCannotLoginOrUseAdminPermissions() {
        accounts.recordGoogleLogin(googleUser("google-identity-1", "person@gmail.com", "Learner"));
        var account = users.findByGoogleSub("google-identity-1").orElseThrow();
        account.disable();
        users.saveAndFlush(account);
        assertThrows(IllegalStateException.class,
                () -> accounts.recordGoogleLogin(googleUser("google-identity-1", "person@gmail.com", "Learner")));
        assertTrue(accounts.findByGoogleSub("google-identity-1").isEmpty());
        assertFalse(accounts.hasRole("google-identity-1", "USER"));
    }

    @Test
    void paginatesUsersAndKeepsEachUsersRolesSeparate() {
        accounts.recordGoogleLogin(googleUser("google-identity-1", "first@gmail.com", "First"));
        accounts.recordGoogleLogin(googleUser("google-identity-2", "second@gmail.com", "Second"));
        var page = accounts.listUsers(0, 1);
        assertEquals(2L, page.total());
        assertEquals(1, page.items().size());
        assertEquals(List.of("USER"), page.items().getFirst().roles());
        assertEquals(1, accounts.listUsers(1, 1).items().size());
    }

    private DefaultOidcUser googleUser(String subject, String email, String displayName) {
        var claims = Map.<String, Object>of(
                "sub", subject,
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
