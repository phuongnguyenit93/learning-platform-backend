package com.learningplatform.backend.shared.auth.model;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;

@Entity
@Table(name = "APP_USER_ROLE")
public class AppUserRole {

    @EmbeddedId
    private AppUserRoleId id;

    @MapsId("userId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "USER_ID", nullable = false)
    private AppUser user;

    @MapsId("roleId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ROLE_ID", nullable = false)
    private AppRole role;

    protected AppUserRole() {
    }

    public AppUserRole(AppUser user, AppRole role) {
        this.id = new AppUserRoleId(user.getId(), role.getId());
        this.user = user;
        this.role = role;
    }

    public AppUserRoleId getId() { return id; }
    public AppUser getUser() { return user; }
    public AppRole getRole() { return role; }
}
