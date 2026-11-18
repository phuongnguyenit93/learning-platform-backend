package com.learningplatform.backend.module.roadmap.model;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class ModuleRoadmapLanguageId implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Column(name = "ROADMAP_ID", nullable = false)
    private Long roadmapId;

    @Column(name = "LANGUAGE_CODE", nullable = false, length = 10)
    private String languageCode;

    protected ModuleRoadmapLanguageId() {
    }

    public ModuleRoadmapLanguageId(Long roadmapId, String languageCode) {
        this.roadmapId = roadmapId;
        this.languageCode = languageCode;
    }

    public Long getRoadmapId() { return roadmapId; }
    public String getLanguageCode() { return languageCode; }

    @Override
    public boolean equals(Object value) {
        if (this == value) return true;
        if (!(value instanceof ModuleRoadmapLanguageId other)) return false;
        return Objects.equals(roadmapId, other.roadmapId)
                && Objects.equals(languageCode, other.languageCode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(roadmapId, languageCode);
    }
}
