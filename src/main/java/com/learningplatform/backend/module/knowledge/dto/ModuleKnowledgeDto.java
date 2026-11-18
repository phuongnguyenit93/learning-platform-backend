package com.learningplatform.backend.module.knowledge.dto;

import com.learningplatform.backend.module.knowledge.model.Difficulty;

/** Public, fully written Markdown and QA metadata for one localized Section. */
public record ModuleKnowledgeDto(
        String moduleSlug,
        String languageCode,
        KnowledgeMenuNodeDto chapter,
        KnowledgeMenuNodeDto section,
        Long knowledgeId,
        Difficulty difficulty,
        String markdownContent,
        String focus,
        String problemSolved,
        String mechanism,
        String applicationValue,
        String summary) {
}
