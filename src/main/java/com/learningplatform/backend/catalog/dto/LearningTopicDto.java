package com.learningplatform.backend.catalog.dto;

import com.learningplatform.backend.catalog.model.TopicStatus;

public record LearningTopicDto(
        Long id,
        String code,
        String slug,
        String name,
        String description,
        String iconSvg,
        TopicStatus status,
        Integer sortOrder,
        LearningCategoryDto category) {
}
