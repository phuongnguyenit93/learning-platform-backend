package com.learningplatform.backend.language.model;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;
import org.hibernate.type.NumericBooleanConverter;

/** Shared read-only catalog of languages for all localized content tables. */
@Entity
@Immutable
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
