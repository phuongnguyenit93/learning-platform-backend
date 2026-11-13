package com.learningplatform.backend.module_overview.model;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/** One module can have at most one Overview per language. */
@Embeddable
public class ModuleOverviewId implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Column(name = "ID_STRUCTURE", nullable = false)
    private Long idStructure;

    @Column(name = "LANGUAGE_CODE", nullable = false, length = 10)
    private String languageCode;

    protected ModuleOverviewId() {
    }

    public ModuleOverviewId(Long idStructure, String languageCode) {
        this.idStructure = idStructure;
        this.languageCode = languageCode;
    }

    public Long getIdStructure() { return idStructure; }
    public String getLanguageCode() { return languageCode; }

    @Override
    public boolean equals(Object value) {
        if (this == value) return true;
        if (!(value instanceof ModuleOverviewId other)) return false;
        return Objects.equals(idStructure, other.idStructure)
                && Objects.equals(languageCode, other.languageCode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(idStructure, languageCode);
    }
}
