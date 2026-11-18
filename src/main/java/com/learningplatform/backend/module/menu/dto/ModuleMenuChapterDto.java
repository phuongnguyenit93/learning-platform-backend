package com.learningplatform.backend.module.menu.dto;

import java.util.List;

/** A localized Chapter with its ordered Sections. */
public record ModuleMenuChapterDto(
        Long id,
        String slug,
        String title,
        String description,
        Integer sortOrder,
        int lessonCount,
        List<ModuleMenuSectionDto> sections) {
}
