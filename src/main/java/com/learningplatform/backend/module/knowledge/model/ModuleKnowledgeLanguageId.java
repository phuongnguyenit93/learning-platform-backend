package com.learningplatform.backend.module.knowledge.model;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class ModuleKnowledgeLanguageId implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Column(name = "KNOWLEDGE_ID", nullable = false)
    private Long knowledgeId;

    @Column(name = "LANGUAGE_CODE", nullable = false, length = 10)
    private String languageCode;

    protected ModuleKnowledgeLanguageId() {
    }

    public ModuleKnowledgeLanguageId(Long knowledgeId, String languageCode) {
        this.knowledgeId = knowledgeId;
        this.languageCode = languageCode;
    }

    public Long getKnowledgeId() { return knowledgeId; }
    public String getLanguageCode() { return languageCode; }

    @Override
    public boolean equals(Object value) {
        if (this == value) return true;
        if (!(value instanceof ModuleKnowledgeLanguageId other)) return false;
        return Objects.equals(knowledgeId, other.knowledgeId)
                && Objects.equals(languageCode, other.languageCode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(knowledgeId, languageCode);
    }
}
