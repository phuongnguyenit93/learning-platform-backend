package com.learningplatform.backend.roadmap.service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.learningplatform.backend.catalog.model.TopicStatus;
import com.learningplatform.backend.catalog.repository.LearningTopicRepository;
import com.learningplatform.backend.roadmap.dto.ModuleRoadmapDto;
import com.learningplatform.backend.roadmap.dto.RoadmapMilestoneDto;
import com.learningplatform.backend.roadmap.model.ModuleRoadmap;
import com.learningplatform.backend.roadmap.repository.ModuleRoadmapRepository;
import com.learningplatform.backend.structure.model.ModuleStructure;
import com.learningplatform.backend.structure.model.StructureType;
import com.learningplatform.backend.structure.repository.ModuleStructureRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Profile("oracle")
@Transactional(readOnly = true)
public class ModuleRoadmapService {

    private final ModuleRoadmapRepository roadmaps;
    private final ModuleStructureRepository structures;
    private final LearningTopicRepository topics;

    public ModuleRoadmapService(ModuleRoadmapRepository roadmaps,
                                ModuleStructureRepository structures,
                                LearningTopicRepository topics) {
        this.roadmaps = roadmaps;
        this.structures = structures;
        this.topics = topics;
    }

    public ModuleRoadmapDto getRoadmap(String moduleSlug, String languageCode) {
        if (languageCode == null || languageCode.isBlank() || languageCode.length() > 10) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid language code");
        }

        ModuleStructure module = structures.findBySlug(moduleSlug)
                .orElseThrow(ModuleRoadmapService::notFound);

        // Mirror Overview visibility: no GROUP, inactive node, hidden topic/category
        // or a module beneath an inactive or inconsistent ancestor.
        if (module.getType() != StructureType.MODULE || !Boolean.TRUE.equals(module.getActive())) {
            throw notFound();
        }
        Long topicId = module.getTopic().getId();
        if (topics.findVisibleById(topicId, TopicStatus.HIDDEN).isEmpty()) {
            throw notFound();
        }
        assertVisibleParents(module, topicId);

        List<ModuleRoadmap> milestones = roadmaps.findVisibleLanguageMilestones(
                module.getIdStructure(), languageCode);
        // Missing/disabled language or no authored Roadmap: 404, no fallback.
        if (milestones.isEmpty()) {
            throw notFound();
        }

        return new ModuleRoadmapDto(module.getSlug(), languageCode,
                milestones.stream().map(this::toDto).toList());
    }

    private RoadmapMilestoneDto toDto(ModuleRoadmap milestone) {
        return new RoadmapMilestoneDto(
                milestone.getId(),
                milestone.getTitle(),
                milestone.getPurpose(),
                parseObjectives(milestone.getObjective()),
                milestone.getSortOrder());
    }

    /** Each non-blank line of OBJECTIVE becomes one objective in the API. */
    private static List<String> parseObjectives(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        return value.lines()
                .map(String::trim)
                .filter(line -> !line.isEmpty())
                .toList();
    }

    private void assertVisibleParents(ModuleStructure module, Long topicId) {
        Set<String> visited = new HashSet<>();
        visited.add(module.getNodeKey());
        String parentKey = module.getParentNodeKey();
        while (parentKey != null) {
            if (!visited.add(parentKey)) {
                throw notFound();
            }
            ModuleStructure parent = structures.findByNodeKey(parentKey)
                    .orElseThrow(ModuleRoadmapService::notFound);
            if (!topicId.equals(parent.getTopic().getId()) || !Boolean.TRUE.equals(parent.getActive())) {
                throw notFound();
            }
            parentKey = parent.getParentNodeKey();
        }
    }

    private static ResponseStatusException notFound() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Module roadmap not found");
    }
}
