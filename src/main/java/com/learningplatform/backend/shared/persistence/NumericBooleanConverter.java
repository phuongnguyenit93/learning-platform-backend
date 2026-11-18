package com.learningplatform.backend.shared.persistence;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/** Maps Oracle NUMBER(1) values constrained to 0/1 to Java Boolean. */
@Converter
public class NumericBooleanConverter implements AttributeConverter<Boolean, Integer> {

    @Override
    public Integer convertToDatabaseColumn(Boolean value) {
        if (value == null) {
            return null;
        }
        return value ? 1 : 0;
    }

    @Override
    public Boolean convertToEntityAttribute(Integer value) {
        if (value == null) {
            return null;
        }
        return switch (value) {
            case 0 -> false;
            case 1 -> true;
            default -> throw new IllegalArgumentException(
                    "Expected NUMBER(1) Boolean value 0 or 1, got: " + value);
        };
    }
}
