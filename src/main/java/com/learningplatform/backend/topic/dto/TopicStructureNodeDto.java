package com.learningplatform.backend.topic.dto;

import java.util.List;

/** One node in the database-backed Topic navigation tree. */
public record TopicStructureNodeDto(
        String id,
        String nodeKey,
        String name,
        TopicStructureNodeKind kind,
        String path,
        String slug,
        String description,
        Integer sortOrder,
        List<TopicStructureNodeDto> children) {
}
