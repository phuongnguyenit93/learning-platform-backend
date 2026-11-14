package com.learningplatform.backend.roadmap.dto;

import java.util.List;

/** Roadmap is fetched separately from the lightweight module structure tree. */
public record ModuleRoadmapDto(
        String slug,
        String languageCode,
        List<RoadmapMilestoneDto> roadmap) {
}
