package com.learningplatform.backend.shared.auth.model;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import com.learningplatform.backend.shared.persistence.NumericBooleanConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "APP_USER", uniqueConstraints = @UniqueConstraint(
        name = "APP_USER_GOOGLE_SUB_UK", columnNames = "GOOGLE_SUB"))
public class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID", nullable = false)
    private Long id;

    @Column(name = "GOOGLE_SUB", nullable = false, length = 255)
    private String googleSub;

    @Column(name = "EMAIL", length = 320)
    private String email;

    @Convert(converter = NumericBooleanConverter.class)
    @Column(name = "EMAIL_VERIFIED", nullable = false)
    private Boolean emailVerified = false;

    @Column(name = "DISPLAY_NAME", length = 255)
    private String displayName;

    @Column(name = "AVATAR_URL", length = 1024)
    private String avatarUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "STATUS", nullable = false, length = 16)
    private AppUserStatus status = AppUserStatus.ACTIVE;

    @Column(name = "CREATED_AT", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "UPDATED_AT", nullable = false)
    private OffsetDateTime updatedAt;

    @Column(name = "LAST_LOGIN_AT")
    private OffsetDateTime lastLoginAt;

    protected AppUser() {
    }

    public AppUser(String googleSub) {
        this.googleSub = googleSub;
    }

    public void recordLogin(String email, boolean emailVerified, String displayName, String avatarUrl) {
        this.email = email;
        this.emailVerified = emailVerified;
        this.displayName = displayName;
        this.avatarUrl = avatarUrl;
        this.lastLoginAt = OffsetDateTime.now(ZoneOffset.UTC);
    }

    public void disable() {
        this.status = AppUserStatus.DISABLED;
    }

    @PrePersist
    void onCreate() {
        var now = OffsetDateTime.now(ZoneOffset.UTC);
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = OffsetDateTime.now(ZoneOffset.UTC);
    }

    public Long getId() { return id; }
    public String getGoogleSub() { return googleSub; }
    public String getEmail() { return email; }
    public Boolean getEmailVerified() { return emailVerified; }
    public String getDisplayName() { return displayName; }
    public String getAvatarUrl() { return avatarUrl; }
    public AppUserStatus getStatus() { return status; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public OffsetDateTime getLastLoginAt() { return lastLoginAt; }
}
