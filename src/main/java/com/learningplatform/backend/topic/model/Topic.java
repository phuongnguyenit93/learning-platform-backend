package com.learningplatform.backend.topic.model;

import com.learningplatform.backend.category.model.Category;
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
import jakarta.persistence.Index;
import jakarta.persistence.UniqueConstraint;
import org.hibernate.annotations.Check;

/** Topic catalog entry belonging to one Category; admin APIs own writes. */
@Entity
@Check(constraints = "STATUS IN ('AVAILABLE', 'COMING_SOON', 'HIDDEN')")
@Table(name = "TOPIC", uniqueConstraints = {
        @UniqueConstraint(name = "UQ_TOPIC_CODE", columnNames = "CODE"),
        @UniqueConstraint(name = "UQ_TOPIC_SLUG", columnNames = "SLUG")
}, indexes = @Index(name = "IX_TOPIC_CATEGORY", columnList = "CATEGORY_ID, SORT_ORDER"))
public class Topic {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "CATEGORY_ID", nullable = false,
            foreignKey = @ForeignKey(name = "FK_TOPIC_CATEGORY"))
    private Category category;

    @Column(name = "CODE", nullable = false, length = 50)
    private String code;

    @Column(name = "SLUG", nullable = false, length = 100)
    private String slug;

    @Column(name = "NAME", nullable = false, length = 150)
    private String name;

    @Column(name = "DESCRIPTION", length = 500)
    private String description;

    // Oracle currently allows ICON_SVG to be NULL; preserve that runtime schema.
    @Lob
    @Column(name = "ICON_SVG", columnDefinition = "CLOB")
    private String iconSvg;

    @Enumerated(EnumType.STRING)
    @Column(name = "STATUS", nullable = false, length = 30)
    private TopicStatus status;

    @Column(name = "SORT_ORDER", nullable = false)
    private Integer sortOrder;

    protected Topic() {
    }

    public Topic(Category category, String code, String slug, String name,
            String description, int sortOrder) {
        this.category = category;
        this.code = code;
        this.slug = slug;
        this.name = name;
        this.description = description;
        this.status = TopicStatus.COMING_SOON;
        this.sortOrder = sortOrder;
        this.iconSvg = null;
    }

    /** Deliberately does not modify ICON_SVG, STATUS, CATEGORY_ID or SORT_ORDER. */
    public void updateDetails(String code, String slug, String name, String description) {
        this.code = code;
        this.slug = slug;
        this.name = name;
        this.description = description;
    }

    public Long getId() { return id; }
    public Category getCategory() { return category; }
    public String getCode() { return code; }
    public String getSlug() { return slug; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getIconSvg() { return iconSvg; }
    public TopicStatus getStatus() { return status; }
    public Integer getSortOrder() { return sortOrder; }
}
