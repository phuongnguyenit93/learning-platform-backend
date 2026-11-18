package com.learningplatform.backend.module.roadmap.service;

import java.util.List;

import com.learningplatform.backend.module.core.model.LearningModule;
import com.learningplatform.backend.module.core.service.ModuleApiRequestValidator;
import com.learningplatform.backend.module.core.service.ModuleCoreService;
import com.learningplatform.backend.module.roadmap.dto.ModuleRoadmapDto;
import com.learningplatform.backend.module.roadmap.dto.RoadmapMilestoneDto;
import com.learningplatform.backend.module.roadmap.repository.ModuleRoadmapLanguageRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Profile("oracle")
@Transactional(readOnly = true)
public class ModuleRoadmapService {

    private final ModuleCoreService modules;
    private final ModuleRoadmapLanguageRepository translations;

    public ModuleRoadmapService(
            ModuleCoreService modules,
            ModuleRoadmapLanguageRepository translations) {
        this.modules = modules;
        this.translations = translations;
    }

    public ModuleRoadmapDto getRoadmap(String moduleSlug, String languageCode) {
        ModuleApiRequestValidator.validateLanguageCode(languageCode);
        LearningModule module = modules.getVisibleModule(moduleSlug);
        List<RoadmapMilestoneDto> milestones = translations
                .findVisibleByModuleAndLanguage(module.getId(), languageCode)
                .stream()
                .map(translation -> new RoadmapMilestoneDto(
                        translation.getRoadmap().getId(),
                        translation.getTitle(),
                        translation.getPurpose(),
                        parseObjectives(translation.getObjective()),
                        translation.getRoadmap().getSortOrder()))
                .toList();

        if (milestones.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Module roadmap not found");
        }
        return new ModuleRoadmapDto(module.getSlug(), languageCode, milestones);
    }

    /** Each non-blank line in OBJECTIVE is exposed as one objective. */
    private static List<String> parseObjectives(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        return value.lines().map(String::trim).filter(line -> !line.isEmpty()).toList();
    }
}
