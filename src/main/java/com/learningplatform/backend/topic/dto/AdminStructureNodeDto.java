package com.learningplatform.backend.topic.dto;

import java.util.List;

/** id = TOPIC_GROUP.ID for GROUP, TOPIC_GROUP_MODULE.ID for MODULE. */
public record AdminStructureNodeDto(String kind, Long id, String name, String slug,
        boolean active, List<AdminStructureNodeDto> children) {
}
