package com.learningplatform.backend.module.knowledge.model;

import com.learningplatform.backend.shared.language.model.Language;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Index;
import jakarta.persistence.Lob;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;

/** Localized Knowledge content and lesson metadata. */
@Entity
@Immutable
@Table(name = "MODULE_KNOWLEDGE_LANGUAGE",
        indexes = @Index(name = "IX_MKL_LANGUAGE", columnList = "LANGUAGE_CODE"))
public class ModuleKnowledgeLanguage {

    @EmbeddedId
    private ModuleKnowledgeLanguageId id;

    @MapsId("knowledgeId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "KNOWLEDGE_ID", nullable = false,
            foreignKey = @ForeignKey(name = "FK_MKL_KNOWLEDGE"))
    private ModuleKnowledge knowledge;

    @MapsId("languageCode")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "LANGUAGE_CODE", nullable = false,
            foreignKey = @ForeignKey(name = "FK_MKL_LANGUAGE"))
    private Language language;

    @Lob
    @Column(name = "CONTENT", columnDefinition = "CLOB")
    private String content;

    @Column(name = "STATUS", length = 30)
    private String status;

    @Column(name = "FOCUS", length = 1000)
    private String focus;

    @Column(name = "PROBLEM_SOLVED", length = 1000)
    private String problemSolved;

    @Column(name = "MECHANISM", length = 1000)
    private String mechanism;

    @Column(name = "APPLICATION_VALUE", length = 1000)
    private String applicationValue;

    @Lob
    @Column(name = "SUMMARY", columnDefinition = "CLOB")
    private String summary;

    protected ModuleKnowledgeLanguage() {
    }

    public ModuleKnowledgeLanguageId getId() { return id; }
    public ModuleKnowledge getKnowledge() { return knowledge; }
    public Language getLanguage() { return language; }
    public String getContent() { return content; }
    public String getStatus() { return status; }
    public String getFocus() { return focus; }
    public String getProblemSolved() { return problemSolved; }
    public String getMechanism() { return mechanism; }
    public String getApplicationValue() { return applicationValue; }
    public String getSummary() { return summary; }
}
