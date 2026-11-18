package com.learningplatform.backend.shared.language.model;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import com.learningplatform.backend.shared.persistence.NumericBooleanConverter;
import org.hibernate.annotations.Check;
import org.hibernate.annotations.Immutable;

/** Shared read-only language catalog used by every localized feature. */
@Entity
@Immutable
@Check(constraints = "IS_ACTIVE IN (0, 1)")
@Table(name = "LANGUAGE")
public class Language {

    @Id
    @Column(name = "CODE", nullable = false, length = 10)
    private String code;

    @Column(name = "NAME", nullable = false, length = 100)
    private String name;

    @Convert(converter = NumericBooleanConverter.class)
    @Column(name = "IS_ACTIVE", nullable = false)
    private Boolean active;

    protected Language() {
    }

    public String getCode() { return code; }
    public String getName() { return name; }
    public Boolean getActive() { return active; }
}
