package com.learningplatform.backend.module.overview.model;

import com.learningplatform.backend.module.core.model.LearningModule;
import com.learningplatform.backend.shared.language.model.Language;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.hibernate.annotations.Immutable;

/** One Markdown overview per Module and language. */
@Entity
@Immutable
@Table(name = "MODULE_OVERVIEW",
        uniqueConstraints = @UniqueConstraint(
                name = "UQ_MO_MODULE_LANGUAGE", columnNames = {"MODULE_ID", "LANGUAGE_CODE"}))
public class ModuleOverview {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "MODULE_ID", nullable = false,
            foreignKey = @ForeignKey(name = "FK_MO_MODULE"))
    private LearningModule module;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "LANGUAGE_CODE", nullable = false,
            foreignKey = @ForeignKey(name = "FK_MO_LANGUAGE"))
    private Language language;

    @Lob
    @Column(name = "CONTENT", nullable = false, columnDefinition = "CLOB")
    private String content;

    protected ModuleOverview() {
    }

    public Long getId() { return id; }
    public LearningModule getModule() { return module; }
    public Language getLanguage() { return language; }
    public String getContent() { return content; }
}
