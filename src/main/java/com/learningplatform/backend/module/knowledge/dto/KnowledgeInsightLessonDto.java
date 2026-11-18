package com.learningplatform.backend.module.knowledge.dto;

import com.learningplatform.backend.module.knowledge.model.Difficulty;

/** The four comparison fields of one published Section, without its CLOBs. */
public record KnowledgeInsightLessonDto(
        Long id,
        String slug,
        String title,
        Integer sortOrder,
        Long knowledgeId,
        Difficulty difficulty,
        String focus,
        String problemSolved,
        String mechanism,
        String applicationValue) {
}
