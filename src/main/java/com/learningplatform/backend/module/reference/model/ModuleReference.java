package com.learningplatform.backend.module.reference.model;

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
import com.learningplatform.backend.shared.persistence.NumericBooleanConverter;
import org.hibernate.annotations.Check;
import org.hibernate.annotations.Immutable;

/** Language-independent reference URL belonging to one Module. */
@Entity
@Immutable
@Check(constraints = "IS_ACTIVE IN (0, 1)")
@Table(name = "MODULE_REFERENCE",
        indexes = @Index(name = "IX_MREF_MODULE_SORT", columnList = "MODULE_ID, SORT_ORDER, ID"))
public class ModuleReference {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "MODULE_ID", nullable = false,
            foreignKey = @ForeignKey(name = "FK_MREF_MODULE"))
    private LearningModule module;

    @Column(name = "URL", nullable = false, length = 2000)
    private String url;

    @Column(name = "SORT_ORDER", nullable = false)
    private Integer sortOrder;

    @Convert(converter = NumericBooleanConverter.class)
    @Column(name = "IS_ACTIVE", nullable = false)
    private Boolean active;

    protected ModuleReference() {
    }

    public Long getId() { return id; }
    public LearningModule getModule() { return module; }
    public String getUrl() { return url; }
    public Integer getSortOrder() { return sortOrder; }
    public Boolean getActive() { return active; }
}
