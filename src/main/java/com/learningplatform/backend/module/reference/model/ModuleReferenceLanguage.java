package com.learningplatform.backend.module.reference.model;

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

/** Localized title and description of a reference URL. */
@Entity
@Immutable
@Table(name = "MODULE_REFERENCE_LANGUAGE",
        indexes = @Index(name = "IX_MREFL_LANGUAGE", columnList = "LANGUAGE_CODE"))
public class ModuleReferenceLanguage {

    @EmbeddedId
    private ModuleReferenceLanguageId id;

    @MapsId("referenceId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "REFERENCE_ID", nullable = false,
            foreignKey = @ForeignKey(name = "FK_MREFL_REFERENCE"))
    private ModuleReference reference;

    @MapsId("languageCode")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "LANGUAGE_CODE", nullable = false,
            foreignKey = @ForeignKey(name = "FK_MREFL_LANGUAGE"))
    private Language language;

    @Column(name = "TITLE", nullable = false, length = 300)
    private String title;

    @Column(name = "DESCRIPTION", length = 1000)
    private String description;

    protected ModuleReferenceLanguage() {
    }

    public ModuleReferenceLanguageId getId() { return id; }
    public ModuleReference getReference() { return reference; }
    public Language getLanguage() { return language; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
}
