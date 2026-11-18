package com.learningplatform.backend.module.menu.model;

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

/** Localized title and description for one menu node. */
@Entity
@Immutable
@Table(name = "MODULE_MENU_LANGUAGE",
        indexes = @Index(name = "IX_MML_LANGUAGE", columnList = "LANGUAGE_CODE"))
public class ModuleMenuLanguage {

    @EmbeddedId
    private ModuleMenuLanguageId id;

    @MapsId("menuId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "MENU_ID", nullable = false,
            foreignKey = @ForeignKey(name = "FK_MML_MENU"))
    private ModuleMenu menu;

    @MapsId("languageCode")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "LANGUAGE_CODE", nullable = false,
            foreignKey = @ForeignKey(name = "FK_MML_LANGUAGE"))
    private Language language;

    @Column(name = "TITLE", nullable = false, length = 300)
    private String title;

    @Column(name = "DESCRIPTION", length = 1000)
    private String description;

    protected ModuleMenuLanguage() {
    }

    public ModuleMenuLanguageId getId() { return id; }
    public ModuleMenu getMenu() { return menu; }
    public Language getLanguage() { return language; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
}
