package com.learningplatform.backend.module_overview.model;

import com.learningplatform.backend.language.model.Language;
import com.learningplatform.backend.structure.model.ModuleStructure;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;

/**
 * Read-only localized Overview. Content is raw Markdown from BASE.md, not YAML.
 * Structure and language use shared foreign keys; no duplicate module tree.
 */
@Entity
@Immutable
@Table(name = "MODULE_OVERVIEW")
public class ModuleOverview {

    @EmbeddedId
    private ModuleOverviewId id;

    @MapsId("idStructure")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ID_STRUCTURE", nullable = false,
            foreignKey = @ForeignKey(name = "FK_MO_STRUCTURE"))
    private ModuleStructure moduleStructure;

    @MapsId("languageCode")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "LANGUAGE_CODE", nullable = false,
            foreignKey = @ForeignKey(name = "FK_MO_LANGUAGE"))
    private Language language;

    @Lob
    @Column(name = "MARKDOWN_CONTENT", nullable = false, columnDefinition = "CLOB")
    private String markdownContent;

    protected ModuleOverview() {
    }

    public ModuleOverviewId getId() { return id; }
    public ModuleStructure getModuleStructure() { return moduleStructure; }
    public Language getLanguage() { return language; }
    public String getMarkdownContent() { return markdownContent; }
}
