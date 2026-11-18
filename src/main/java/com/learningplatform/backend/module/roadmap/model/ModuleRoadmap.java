package com.learningplatform.backend.module.roadmap.model;

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

/** Language-independent identity and ordering of one roadmap milestone. */
@Entity
@Immutable
@Check(constraints = "IS_ACTIVE IN (0, 1)")
@Table(name = "MODULE_ROADMAP",
        uniqueConstraints = @UniqueConstraint(
                name = "UQ_MR_MODULE_KEY", columnNames = {"MODULE_ID", "ROADMAP_KEY"}),
        indexes = @Index(name = "IX_MR_MODULE_SORT", columnList = "MODULE_ID, SORT_ORDER, ID"))
public class ModuleRoadmap {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "MODULE_ID", nullable = false,
            foreignKey = @ForeignKey(name = "FK_MR_MODULE"))
    private LearningModule module;

    @Column(name = "ROADMAP_KEY", nullable = false, length = 150)
    private String roadmapKey;

    @Column(name = "SORT_ORDER", nullable = false)
    private Integer sortOrder;

    @Convert(converter = NumericBooleanConverter.class)
    @Column(name = "IS_ACTIVE", nullable = false)
    private Boolean active;

    protected ModuleRoadmap() {
    }

    public Long getId() { return id; }
    public LearningModule getModule() { return module; }
    public String getRoadmapKey() { return roadmapKey; }
    public Integer getSortOrder() { return sortOrder; }
    public Boolean getActive() { return active; }
}
