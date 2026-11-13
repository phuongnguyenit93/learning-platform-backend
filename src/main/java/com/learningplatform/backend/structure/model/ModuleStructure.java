package com.learningplatform.backend.structure.model;

import com.learningplatform.backend.catalog.model.LearningTopic;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.hibernate.annotations.Immutable;
import org.hibernate.type.NumericBooleanConverter;

/**
 * One GROUP or MODULE node in a topic's tree.
 *
 * Parent references are expressed by stable node keys, not generated database IDs.
 * Both nodeKey and slug are globally unique, including across different topics.
 */
@Entity
@Immutable
@Table(name = "MODULE_STRUCTURE", uniqueConstraints = {
        @UniqueConstraint(name = "UQ_MS_NODE_KEY", columnNames = "NODE_KEY"),
        @UniqueConstraint(name = "UQ_MS_SLUG", columnNames = "SLUG"),
        // Required as the target of the (TOPIC_ID, PARENT_NODE_KEY) database FK.
        @UniqueConstraint(name = "UQ_MS_TOPIC_NODE", columnNames = {"TOPIC_ID", "NODE_KEY"})
})
public class ModuleStructure {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID_STRUCTURE")
    private Long idStructure;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "TOPIC_ID", nullable = false,
            foreignKey = @ForeignKey(name = "FK_MS_TOPIC"))
    private LearningTopic topic;

    @Column(name = "NODE_KEY", nullable = false, length = 150)
    private String nodeKey;

    // Root nodes use NULL. Oracle enforces that a parent belongs to this same topic.
    @Column(name = "PARENT_NODE_KEY", length = 150)
    private String parentNodeKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "NODE_TYPE", nullable = false, length = 20)
    private StructureType type;

    @Column(name = "SLUG", nullable = false, length = 200)
    private String slug;

    @Column(name = "NAME", nullable = false, length = 200)
    private String name;

    @Column(name = "DESCRIPTION", length = 1000)
    private String description;

    @Column(name = "SORT_ORDER", nullable = false)
    private Integer sortOrder;

    @Convert(converter = NumericBooleanConverter.class)
    @Column(name = "IS_ACTIVE", nullable = false)
    private Boolean active;

    protected ModuleStructure() {
    }

    public Long getIdStructure() { return idStructure; }
    public LearningTopic getTopic() { return topic; }
    public String getNodeKey() { return nodeKey; }
    public String getParentNodeKey() { return parentNodeKey; }
    public StructureType getType() { return type; }
    public String getSlug() { return slug; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public Integer getSortOrder() { return sortOrder; }
    public Boolean getActive() { return active; }
}
