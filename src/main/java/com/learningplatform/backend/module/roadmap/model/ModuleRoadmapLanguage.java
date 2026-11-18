package com.learningplatform.backend.module.roadmap.model;

import com.learningplatform.backend.shared.language.model.Language;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;

/** Translated title, purpose and objective for a roadmap milestone. */
@Entity
@Immutable
@Table(name = "MODULE_ROADMAP_LANGUAGE",
        indexes = @Index(name = "IX_MRL_LANGUAGE", columnList = "LANGUAGE_CODE"))
public class ModuleRoadmapLanguage {

    @EmbeddedId
    private ModuleRoadmapLanguageId id;

    @MapsId("roadmapId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ROADMAP_ID", nullable = false,
            foreignKey = @ForeignKey(name = "FK_MRL_ROADMAP"))
    private ModuleRoadmap roadmap;

    @MapsId("languageCode")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "LANGUAGE_CODE", nullable = false,
            foreignKey = @ForeignKey(name = "FK_MRL_LANGUAGE"))
    private Language language;

    @Column(name = "TITLE", nullable = false, length = 300)
    private String title;

    @Column(name = "PURPOSE", nullable = false, length = 1000)
    private String purpose;

    @Column(name = "OBJECTIVE", length = 2000)
    private String objective;

    protected ModuleRoadmapLanguage() {
    }

    public ModuleRoadmapLanguageId getId() { return id; }
    public ModuleRoadmap getRoadmap() { return roadmap; }
    public Language getLanguage() { return language; }
    public String getTitle() { return title; }
    public String getPurpose() { return purpose; }
    public String getObjective() { return objective; }
}
