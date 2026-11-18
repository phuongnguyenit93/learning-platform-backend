package com.learningplatform.backend.topic.dto;

import com.learningplatform.backend.category.dto.CategoryDto;
import com.learningplatform.backend.topic.model.TopicStatus;

/** Public Topic catalog representation consumed by learning-platform. */
public record TopicDto(
        Long id,
        String code,
        String slug,
        String name,
        String description,
        String iconSvg,
        TopicStatus status,
        Integer sortOrder,
        CategoryDto category) {
}
