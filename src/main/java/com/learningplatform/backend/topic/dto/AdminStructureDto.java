package com.learningplatform.backend.topic.dto;

import java.util.List;

public record AdminStructureDto(Long topicId, String revision, List<AdminStructureNodeDto> nodes) {
}
