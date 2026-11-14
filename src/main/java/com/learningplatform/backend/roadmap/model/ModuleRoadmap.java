package com.learningplatform.backend.roadmap.model;

import com.learningplatform.backend.language.model.Language;
import com.learningplatform.backend.structure.model.ModuleStructure;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;

/** One localized Roadmap milestone; content is managed outside the read-only API. */
@Entity
@Immutable
@Table(name = "MODULE_ROADMAP")
public class ModuleRoadmap {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID_ROADMAP")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ID_STRUCTURE", nullable = false,
            foreignKey = @ForeignKey(name = "FK_MR_STRUCTURE"))
    private ModuleStructure moduleStructure;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "LANGUAGE_CODE", nullable = false,
            foreignKey = @ForeignKey(name = "FK_MR_LANGUAGE"))
    private Language language;

    @Column(name = "TITLE", nullable = false, length = 300)
    private String title;

    @Column(name = "PURPOSE", nullable = false, length = 1000)
    private String purpose;

    /** One objective per line; no JSON or child table. */
    @Column(name = "OBJECTIVE", length = 2000)
    private String objective;

    @Column(name = "SORT_ORDER", nullable = false)
    private Integer sortOrder;

    protected ModuleRoadmap() {
    }

    public Long getId() { return id; }
    public ModuleStructure getModuleStructure() { return moduleStructure; }
    public Language getLanguage() { return language; }
    public String getTitle() { return title; }
    public String getPurpose() { return purpose; }
    public String getObjective() { return objective; }
    public Integer getSortOrder() { return sortOrder; }
}
