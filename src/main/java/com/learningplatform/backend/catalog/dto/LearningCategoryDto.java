package com.learningplatform.backend.catalog.dto;

public record LearningCategoryDto(
        Long id,
        String code,
        String name,
        Integer sortOrder) {
}
