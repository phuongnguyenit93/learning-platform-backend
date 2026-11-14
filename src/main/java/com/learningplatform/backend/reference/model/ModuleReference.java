package com.learningplatform.backend.reference.model;

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

/** One localized, read-only reference link for a MODULE node. */
@Entity
@Immutable
@Table(name = "MODULE_REFERENCE")
public class ModuleReference {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID_REFERENCE")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ID_STRUCTURE", nullable = false,
            foreignKey = @ForeignKey(name = "FK_MREF_STRUCTURE"))
    private ModuleStructure moduleStructure;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "LANGUAGE_CODE", nullable = false,
            foreignKey = @ForeignKey(name = "FK_MREF_LANGUAGE"))
    private Language language;

    @Column(name = "TITLE", nullable = false, length = 300)
    private String title;

    @Column(name = "URL", nullable = false, length = 2000)
    private String url;

    @Column(name = "DESCRIPTION", length = 1000)
    private String description;

    @Column(name = "SORT_ORDER", nullable = false)
    private Integer sortOrder;

    protected ModuleReference() {
    }

    public Long getId() { return id; }
    public ModuleStructure getModuleStructure() { return moduleStructure; }
    public Language getLanguage() { return language; }
    public String getTitle() { return title; }
    public String getUrl() { return url; }
    public String getDescription() { return description; }
    public Integer getSortOrder() { return sortOrder; }
}
