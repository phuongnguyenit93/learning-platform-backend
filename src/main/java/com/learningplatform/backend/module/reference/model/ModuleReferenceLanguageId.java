package com.learningplatform.backend.module.reference.model;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class ModuleReferenceLanguageId implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Column(name = "REFERENCE_ID", nullable = false)
    private Long referenceId;

    @Column(name = "LANGUAGE_CODE", nullable = false, length = 10)
    private String languageCode;

    protected ModuleReferenceLanguageId() {
    }

    public ModuleReferenceLanguageId(Long referenceId, String languageCode) {
        this.referenceId = referenceId;
        this.languageCode = languageCode;
    }

    public Long getReferenceId() { return referenceId; }
    public String getLanguageCode() { return languageCode; }

    @Override
    public boolean equals(Object value) {
        if (this == value) return true;
        if (!(value instanceof ModuleReferenceLanguageId other)) return false;
        return Objects.equals(referenceId, other.referenceId)
                && Objects.equals(languageCode, other.languageCode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(referenceId, languageCode);
    }
}
