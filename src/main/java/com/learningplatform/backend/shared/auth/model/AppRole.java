package com.learningplatform.backend.shared.auth.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "APP_ROLE", uniqueConstraints = @UniqueConstraint(
        name = "APP_ROLE_CODE_UK", columnNames = "CODE"))
public class AppRole {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID", nullable = false)
    private Long id;

    @Column(name = "CODE", nullable = false, length = 32)
    private String code;

    protected AppRole() {
    }

    public AppRole(String code) {
        this.code = code;
    }

    public Long getId() { return id; }
    public String getCode() { return code; }
}
