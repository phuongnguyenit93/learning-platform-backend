package com.learningplatform.backend.module.core.model;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import com.learningplatform.backend.shared.persistence.NumericBooleanConverter;
import org.hibernate.annotations.Check;
import org.hibernate.annotations.Immutable;

/** Core identity and metadata of a learning module. Placement is owned by topic. */
@Entity
@Immutable
@Check(constraints = "IS_ACTIVE IN (0, 1)")
@Table(name = "MODULE", uniqueConstraints = {
        @UniqueConstraint(name = "UQ_MODULE_SERVICE_NAME", columnNames = "SERVICE_NAME"),
        @UniqueConstraint(name = "UQ_MODULE_SLUG", columnNames = "SLUG")
})
public class LearningModule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID", nullable = false)
    private Long id;

    @Column(name = "SERVICE_NAME", nullable = false, length = 150)
    private String serviceName;

    @Column(name = "SLUG", nullable = false, length = 200)
    private String slug;

    @Column(name = "NAME", nullable = false, length = 200)
    private String name;

    @Column(name = "SERVICE_NAME_DESCRIPTION", length = 1000)
    private String serviceNameDescription;

    @Column(name = "SPECIAL_NOTE", length = 2000)
    private String specialNote;

    @Convert(converter = NumericBooleanConverter.class)
    @Column(name = "IS_ACTIVE", nullable = false)
    private Boolean active;

    protected LearningModule() {
    }

    public Long getId() { return id; }
    public String getServiceName() { return serviceName; }
    public String getSlug() { return slug; }
    public String getName() { return name; }
    public String getServiceNameDescription() { return serviceNameDescription; }
    public String getSpecialNote() { return specialNote; }
    public Boolean getActive() { return active; }
}
