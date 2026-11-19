package com.learningplatform.backend.topic.dto;

public record AdminGroupDto(Long id, Long topicId, String name, String slug,
        boolean active, Long parentGroupId, int sortOrder) {
}
