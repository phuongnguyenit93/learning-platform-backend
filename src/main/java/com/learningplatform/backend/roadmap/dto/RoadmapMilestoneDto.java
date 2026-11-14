package com.learningplatform.backend.roadmap.dto;

import java.util.List;

public record RoadmapMilestoneDto(
        Long id,
        String title,
        String purpose,
        List<String> objectives,
        Integer sortOrder) {
}
