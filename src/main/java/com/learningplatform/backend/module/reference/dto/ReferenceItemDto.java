package com.learningplatform.backend.module.reference.dto;

public record ReferenceItemDto(
        Long id,
        String title,
        String url,
        String description,
        Integer sortOrder) {
}
