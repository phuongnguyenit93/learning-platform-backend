package com.learningplatform.backend.topic.model;

import com.learningplatform.backend.module.core.model.LearningModule;
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
import org.hibernate.annotations.Immutable;

/** Places a Module either directly in a Topic or inside one TopicGroup. */
@Entity
@Immutable
@Check(constraints = "IS_ACTIVE IN (0, 1) AND ((GROUP_ID IS NOT NULL AND TOPIC_ID IS NULL) OR (GROUP_ID IS NULL AND TOPIC_ID IS NOT NULL))")
@Table(name = "TOPIC_GROUP_MODULE",
        uniqueConstraints = @UniqueConstraint(name = "UQ_TGM_MODULE", columnNames = "MODULE_ID"),
        indexes = {
                @Index(name = "IX_TGM_GROUP_SORT", columnList = "GROUP_ID, SORT_ORDER"),
                @Index(name = "IX_TGM_TOPIC_SORT", columnList = "TOPIC_ID, SORT_ORDER")
        })
public class TopicGroupModule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "GROUP_ID", foreignKey = @ForeignKey(name = "FK_TGM_GROUP"))
    private TopicGroup topicGroup;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "TOPIC_ID", foreignKey = @ForeignKey(name = "FK_TGM_TOPIC"))
    private Topic topic;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "MODULE_ID", nullable = false,
            foreignKey = @ForeignKey(name = "FK_TGM_MODULE"))
    private LearningModule module;

    @Column(name = "SORT_ORDER", nullable = false)
    private Integer sortOrder;

    @Convert(converter = NumericBooleanConverter.class)
    @Column(name = "IS_ACTIVE", nullable = false)
    private Boolean active;

    protected TopicGroupModule() {
    }

    public Long getId() { return id; }
    public TopicGroup getTopicGroup() { return topicGroup; }
    public Topic getTopic() { return topic; }
    public LearningModule getModule() { return module; }
    public Integer getSortOrder() { return sortOrder; }
    public Boolean getActive() { return active; }
}
