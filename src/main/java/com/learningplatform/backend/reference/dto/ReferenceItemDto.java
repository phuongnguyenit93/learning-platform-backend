package com.learningplatform.backend.reference.dto;

public record ReferenceItemDto(
        Long id,
        String title,
        String url,
        String description,
        Integer sortOrder) {
}
