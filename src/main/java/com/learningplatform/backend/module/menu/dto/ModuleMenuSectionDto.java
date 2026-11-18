package com.learningplatform.backend.module.menu.dto;

import com.learningplatform.backend.module.knowledge.model.Difficulty;

/** A localized Section; never contains lesson Markdown or Summary CLOBs. */
public record ModuleMenuSectionDto(
        Long id,
        String slug,
        String title,
        String description,
        Integer sortOrder,
        Long knowledgeId,
        Difficulty difficulty,
        boolean available,
        boolean hasSummary) {
}
