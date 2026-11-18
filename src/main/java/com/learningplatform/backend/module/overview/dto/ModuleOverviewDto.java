package com.learningplatform.backend.module.overview.dto;

/** Overview Markdown is fetched separately from the navigation tree. */
public record ModuleOverviewDto(
        String slug,
        String languageCode,
        String markdownContent) {
}
