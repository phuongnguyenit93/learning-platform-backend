package com.learningplatform.backend.category.dto;

/** Public, language-neutral Category catalog representation. */
public record CategoryDto(
        Long id,
        String code,
        String name,
        Integer sortOrder) {
}
