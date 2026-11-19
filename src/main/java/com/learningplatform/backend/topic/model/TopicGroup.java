package com.learningplatform.backend.topic.model;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import com.learningplatform.backend.shared.persistence.NumericBooleanConverter;
import org.hibernate.annotations.Check;

/**
 * A hierarchical group within a Topic.
 * The FK only guarantees that the parent exists; same-Topic and cycle
 * validation are enforced in the admin structure service before writes.
 */
@Entity
@Check(constraints = "IS_ACTIVE IN (0, 1) AND (PARENT_GROUP_ID IS NULL OR PARENT_GROUP_ID <> ID)")
@Table(name = "TOPIC_GROUP",
        uniqueConstraints = @UniqueConstraint(
                name = "UQ_TG_TOPIC_SLUG", columnNames = {"TOPIC_ID", "SLUG"}),
        indexes = @Index(name = "IX_TG_TOPIC_PARENT_SORT",
                columnList = "TOPIC_ID, PARENT_GROUP_ID, SORT_ORDER"))
public class TopicGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "TOPIC_ID", nullable = false,
            foreignKey = @ForeignKey(name = "FK_TG_TOPIC"))
    private Topic topic;

    @Column(name = "GROUP_NAME", nullable = false, length = 200)
    private String groupName;

    @Column(name = "SLUG", nullable = false, length = 150)
    private String slug;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "PARENT_GROUP_ID",
            foreignKey = @ForeignKey(name = "FK_TG_PARENT"))
    private TopicGroup parentGroup;

    @Column(name = "SORT_ORDER", nullable = false)
    private Integer sortOrder;

    @Convert(converter = NumericBooleanConverter.class)
    @Column(name = "IS_ACTIVE", nullable = false)
    private Boolean active;

    protected TopicGroup() {
    }

    public TopicGroup(Topic topic, String name, String slug, int order, boolean active) {
        this.topic = topic;
        this.groupName = name;
        this.slug = slug;
        this.sortOrder = order;
        this.active = active;
    }

    public void edit(String name, String slug, boolean active) {
        this.groupName = name;
        this.slug = slug;
        this.active = active;
    }

    public void relocate(TopicGroup parentGroup, int order) {
        this.parentGroup = parentGroup;
        this.sortOrder = order;
    }

    public Long getId() { return id; }
    public Topic getTopic() { return topic; }
    public String getGroupName() { return groupName; }
    public String getSlug() { return slug; }
    public TopicGroup getParentGroup() { return parentGroup; }
    public Integer getSortOrder() { return sortOrder; }
    public Boolean getActive() { return active; }
}
