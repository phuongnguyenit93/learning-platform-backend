package com.learningplatform.backend.shared.auth.model;

import java.io.Serializable;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class AppUserRoleId implements Serializable {

    @Column(name = "USER_ID", nullable = false)
    private Long userId;

    @Column(name = "ROLE_ID", nullable = false)
    private Long roleId;

    protected AppUserRoleId() {
    }

    public AppUserRoleId(Long userId, Long roleId) {
        this.userId = userId;
        this.roleId = roleId;
    }

    public Long getUserId() { return userId; }
    public Long getRoleId() { return roleId; }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof AppUserRoleId that)) return false;
        return Objects.equals(userId, that.userId) && Objects.equals(roleId, that.roleId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, roleId);
    }
}
