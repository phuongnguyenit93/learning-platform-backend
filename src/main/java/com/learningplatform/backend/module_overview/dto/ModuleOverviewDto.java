package com.learningplatform.backend.module_overview.dto;

/** Overview content is fetched separately from the lightweight module tree. */
public record ModuleOverviewDto(
        String slug,
        String languageCode,
        String markdownContent) {
}
