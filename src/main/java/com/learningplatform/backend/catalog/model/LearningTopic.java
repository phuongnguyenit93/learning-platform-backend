package com.learningplatform.backend.catalog.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;

@Entity
@Immutable
@Table(name = "TOPIC")
public class LearningTopic {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @Column(name = "CODE", nullable = false, length = 50, unique = true)
    private String code;

    @Column(name = "SLUG", nullable = false, length = 100, unique = true)
    private String slug;

    @Column(name = "NAME", nullable = false, length = 150)
    private String name;

    @Column(name = "DESCRIPTION", length = 500)
    private String description;

    // Store a complete <svg>...</svg> document (including viewBox) in Oracle CLOB.
    @Lob
    @Column(name = "ICON_SVG", nullable = false, columnDefinition = "CLOB")
    private String iconSvg;

    @Enumerated(EnumType.STRING)
    @Column(name = "STATUS", nullable = false, length = 30)
    private TopicStatus status;

    @Column(name = "SORT_ORDER", nullable = false)
    private Integer sortOrder;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "CATEGORY_ID",
            nullable = false,
            foreignKey = @ForeignKey(name = "FK_TOPIC_CATEGORY"))
    private LearningCategory category;

    protected LearningTopic() {
    }

    public Long getId() { return id; }
    public String getCode() { return code; }
    public String getSlug() { return slug; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getIconSvg() { return iconSvg; }
    public TopicStatus getStatus() { return status; }
    public Integer getSortOrder() { return sortOrder; }
    public LearningCategory getCategory() { return category; }
}
