package com.learningplatform.backend.module.knowledge.dto;

import java.util.List;

/** Ordered, lightweight comparison of published lessons within a Chapter. */
public record ModuleChapterInsightsDto(
        String moduleSlug,
        String languageCode,
        KnowledgeMenuNodeDto chapter,
        List<KnowledgeInsightLessonDto> lessons) {
}
