package com.learningplatform.backend.category.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import com.learningplatform.backend.shared.persistence.NumericBooleanConverter;
import org.hibernate.annotations.Check;
import org.hibernate.annotations.Immutable;
import jakarta.persistence.Convert;

/** Read-only category catalog. Category writes are managed outside this API. */
@Entity
@Immutable
@Check(constraints = "IS_ACTIVE IN (0, 1)")
@Table(name = "CATEGORY",
        uniqueConstraints = @UniqueConstraint(name = "UQ_CATEGORY_CODE", columnNames = "CODE"))
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID", nullable = false)
    private Long id;

    @Column(name = "CODE", nullable = false, length = 50)
    private String code;

    @Column(name = "NAME", nullable = false, length = 150)
    private String name;

    @Column(name = "SORT_ORDER", nullable = false)
    private Integer sortOrder;

    @Convert(converter = NumericBooleanConverter.class)
    @Column(name = "IS_ACTIVE", nullable = false)
    private Boolean active;

    protected Category() {
    }

    public Long getId() { return id; }
    public String getCode() { return code; }
    public String getName() { return name; }
    public Integer getSortOrder() { return sortOrder; }
    public Boolean getActive() { return active; }
}
