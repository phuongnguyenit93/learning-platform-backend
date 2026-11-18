package com.learningplatform.backend.module.roadmap.dto;

import java.util.List;

/** Localized ordered milestones for one Module. */
public record ModuleRoadmapDto(
        String slug,
        String languageCode,
        List<RoadmapMilestoneDto> roadmap) {
}
