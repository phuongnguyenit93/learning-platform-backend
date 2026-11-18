package com.learningplatform.backend.module.menu.model;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class ModuleMenuLanguageId implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Column(name = "MENU_ID", nullable = false)
    private Long menuId;

    @Column(name = "LANGUAGE_CODE", nullable = false, length = 10)
    private String languageCode;

    protected ModuleMenuLanguageId() {
    }

    public ModuleMenuLanguageId(Long menuId, String languageCode) {
        this.menuId = menuId;
        this.languageCode = languageCode;
    }

    public Long getMenuId() { return menuId; }
    public String getLanguageCode() { return languageCode; }

    @Override
    public boolean equals(Object value) {
        if (this == value) return true;
        if (!(value instanceof ModuleMenuLanguageId other)) return false;
        return Objects.equals(menuId, other.menuId)
                && Objects.equals(languageCode, other.languageCode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(menuId, languageCode);
    }
}
