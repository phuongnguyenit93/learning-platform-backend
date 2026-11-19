package com.learningplatform.backend.auth.service;

import java.util.List;
import java.util.Optional;

import com.learningplatform.backend.auth.dto.AdminUserDto;
import com.learningplatform.backend.auth.dto.AdminUsersPageDto;
import com.learningplatform.backend.auth.dto.AuthUserDto;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Profile("oracle")
public class AccountService {

    private final JdbcTemplate jdbc;

    public AccountService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Transactional
    public void recordGoogleLogin(OidcUser user) {
        // Google 'sub' is the identity key; email can change and must not be used as a login key.
        if (user.getSubject() == null || user.getSubject().isBlank()) {
            throw new IllegalArgumentException("Missing Google subject");
        }
        jdbc.update("""
                MERGE INTO APP_USER u
                USING (SELECT CAST(? AS VARCHAR2(255)) GOOGLE_SUB,
                              CAST(? AS VARCHAR2(320)) EMAIL,
                              CAST(? AS NUMBER(1)) EMAIL_VERIFIED,
                              CAST(? AS VARCHAR2(255)) DISPLAY_NAME,
                              CAST(? AS VARCHAR2(1024)) AVATAR_URL FROM DUAL) v
                ON (u.GOOGLE_SUB = v.GOOGLE_SUB)
                WHEN MATCHED THEN UPDATE SET
                    u.EMAIL = v.EMAIL, u.EMAIL_VERIFIED = v.EMAIL_VERIFIED,
                    u.DISPLAY_NAME = v.DISPLAY_NAME, u.AVATAR_URL = v.AVATAR_URL,
                    u.UPDATED_AT = SYSTIMESTAMP, u.LAST_LOGIN_AT = SYSTIMESTAMP
                WHEN NOT MATCHED THEN INSERT
                    (GOOGLE_SUB, EMAIL, EMAIL_VERIFIED, DISPLAY_NAME, AVATAR_URL,
                     STATUS, CREATED_AT, UPDATED_AT, LAST_LOGIN_AT)
                VALUES
                    (v.GOOGLE_SUB, v.EMAIL, v.EMAIL_VERIFIED, v.DISPLAY_NAME,
                     v.AVATAR_URL, 'ACTIVE', SYSTIMESTAMP, SYSTIMESTAMP, SYSTIMESTAMP)
                """, user.getSubject(), user.getEmail(),
                Boolean.TRUE.equals(user.getEmailVerified()) ? 1 : 0,
                user.getFullName(), user.getPicture());

        // Default role is created only once; Google never supplies application roles.
        jdbc.update("""
                MERGE INTO APP_USER_ROLE ur
                USING (SELECT u.ID USER_ID, r.ID ROLE_ID
                       FROM APP_USER u JOIN APP_ROLE r ON r.CODE = 'USER'
                       WHERE u.GOOGLE_SUB = ?) v
                ON (ur.USER_ID = v.USER_ID AND ur.ROLE_ID = v.ROLE_ID)
                WHEN NOT MATCHED THEN INSERT (USER_ID, ROLE_ID)
                VALUES (v.USER_ID, v.ROLE_ID)
                """, user.getSubject());

        if (findByGoogleSub(user.getSubject()).isEmpty()) {
            throw new IllegalStateException("The user account is disabled");
        }
    }

    @Transactional(readOnly = true)
    public Optional<AuthUserDto> findByGoogleSub(String googleSub) {
        var accounts = jdbc.query("""
                SELECT ID, EMAIL, DISPLAY_NAME, AVATAR_URL
                FROM APP_USER
                WHERE GOOGLE_SUB = ? AND STATUS = 'ACTIVE'
                """, (rs, rowNum) -> new AuthUserDto(
                rs.getLong("ID"), rs.getString("EMAIL"),
                rs.getString("DISPLAY_NAME"), rs.getString("AVATAR_URL"),
                List.of()), googleSub);
        if (accounts.isEmpty()) {
            return Optional.empty();
        }
        AuthUserDto account = accounts.getFirst();
        List<String> roles = jdbc.queryForList("""
                SELECT r.CODE FROM APP_ROLE r
                JOIN APP_USER_ROLE ur ON ur.ROLE_ID = r.ID
                WHERE ur.USER_ID = ?
                ORDER BY r.CODE
                """, String.class, account.id());
        return Optional.of(new AuthUserDto(account.id(), account.email(),
                account.displayName(), account.avatarUrl(), roles));
    }

    @Transactional(readOnly = true)
    public boolean hasRole(String googleSub, String role) {
        Integer found = jdbc.queryForObject("""
                SELECT COUNT(*) FROM APP_USER u
                JOIN APP_USER_ROLE ur ON ur.USER_ID = u.ID
                JOIN APP_ROLE r ON r.ID = ur.ROLE_ID
                WHERE u.GOOGLE_SUB = ? AND u.STATUS = 'ACTIVE' AND r.CODE = ?
                """, Integer.class, googleSub, role);
        return found != null && found > 0;
    }

    @Transactional(readOnly = true)
    public AdminUsersPageDto listUsers(int page, int size) {
        int offset = Math.multiplyExact(page, size);
        var users = jdbc.query("""
                SELECT ID, EMAIL, DISPLAY_NAME, STATUS
                FROM APP_USER ORDER BY ID
                OFFSET ? ROWS FETCH NEXT ? ROWS ONLY
                """, (rs, rowNum) -> {
            long id = rs.getLong("ID");
            return new AdminUserDto(id, rs.getString("EMAIL"),
                    rs.getString("DISPLAY_NAME"), rs.getString("STATUS"),
                    rolesForUser(id));
        }, offset, size);
        Long total = jdbc.queryForObject("SELECT COUNT(*) FROM APP_USER", Long.class);
        return new AdminUsersPageDto(users, page, size, total == null ? 0 : total);
    }

    private List<String> rolesForUser(long userId) {
        return jdbc.queryForList("""
                SELECT r.CODE FROM APP_ROLE r
                JOIN APP_USER_ROLE ur ON ur.ROLE_ID = r.ID
                WHERE ur.USER_ID = ? ORDER BY r.CODE
                """, String.class, userId);
    }
}
