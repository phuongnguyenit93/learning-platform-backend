package com.learningplatform.backend.catalog.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;

@Entity
@Immutable
@Table(name = "CATEGORY")
public class LearningCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @Column(name = "CODE", nullable = false, length = 50, unique = true)
    private String code;

    @Column(name = "NAME", nullable = false, length = 150)
    private String name;

    @Column(name = "SORT_ORDER", nullable = false)
    private Integer sortOrder;

    @Column(name = "IS_ACTIVE", nullable = false)
    private Boolean active;

    protected LearningCategory() {
    }

    public Long getId() { return id; }
    public String getCode() { return code; }
    public String getName() { return name; }
    public Integer getSortOrder() { return sortOrder; }
    public Boolean getActive() { return active; }
}
