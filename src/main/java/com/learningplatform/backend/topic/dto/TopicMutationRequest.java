package com.learningplatform.backend.topic.dto;

public record TopicMutationRequest(String code, String slug, String name, String description) {
}
