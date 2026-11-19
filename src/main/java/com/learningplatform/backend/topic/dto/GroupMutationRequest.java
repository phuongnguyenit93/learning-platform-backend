package com.learningplatform.backend.topic.dto;

public record GroupMutationRequest(String name, String slug, Boolean active, String revision) {
}
