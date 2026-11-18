package com.learningplatform.backend.module.knowledge.dto;

/** Identifies the requested localized chapter or section. */
public record KnowledgeMenuNodeDto(
        Long id,
        String slug,
        String title,
        String description) {
}
