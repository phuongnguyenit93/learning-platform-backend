package com.learningplatform.backend.module_overview.service;

import java.util.HashSet;
import java.util.Set;

import com.learningplatform.backend.catalog.model.TopicStatus;
import com.learningplatform.backend.catalog.repository.LearningTopicRepository;
import com.learningplatform.backend.module_overview.dto.ModuleOverviewDto;
import com.learningplatform.backend.module_overview.model.ModuleOverviewId;
import com.learningplatform.backend.module_overview.repository.ModuleOverviewRepository;
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
public class ModuleOverviewService {

    private final ModuleOverviewRepository overviews;
    private final ModuleStructureRepository structures;
    private final LearningTopicRepository topics;

    public ModuleOverviewService(ModuleOverviewRepository overviews,
                                 ModuleStructureRepository structures,
                                 LearningTopicRepository topics) {
        this.overviews = overviews;
        this.structures = structures;
        this.topics = topics;
    }

    public ModuleOverviewDto getOverview(String moduleSlug, String languageCode) {
        if (languageCode.isBlank() || languageCode.length() > 10) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid language code");
        }

        ModuleStructure module = structures.findBySlug(moduleSlug)
                .orElseThrow(ModuleOverviewService::notFound);

        // The Overview endpoint must not expose hidden topics, GROUPs, or
        // children of inactive ancestor nodes not visible in the navigation tree.
        if (module.getType() != StructureType.MODULE || !Boolean.TRUE.equals(module.getActive())) {
            throw notFound();
        }
        Long topicId = module.getTopic().getId();
        if (topics.findVisibleById(topicId, TopicStatus.HIDDEN).isEmpty()) {
            throw notFound();
        }
        assertVisibleParents(module, topicId);

        var overview = overviews.findById(new ModuleOverviewId(module.getIdStructure(), languageCode))
                .orElseThrow(ModuleOverviewService::notFound);
        if (!Boolean.TRUE.equals(overview.getLanguage().getActive())) {
            throw notFound();
        }

        return new ModuleOverviewDto(module.getSlug(), languageCode, overview.getMarkdownContent());
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
                    .orElseThrow(ModuleOverviewService::notFound);
            if (!topicId.equals(parent.getTopic().getId()) || !Boolean.TRUE.equals(parent.getActive())) {
                throw notFound();
            }
            parentKey = parent.getParentNodeKey();
        }
    }

    private static ResponseStatusException notFound() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Module overview not found");
    }
}
