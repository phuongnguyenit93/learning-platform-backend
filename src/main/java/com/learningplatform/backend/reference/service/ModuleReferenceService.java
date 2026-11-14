package com.learningplatform.backend.reference.service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.learningplatform.backend.catalog.model.TopicStatus;
import com.learningplatform.backend.catalog.repository.LearningTopicRepository;
import com.learningplatform.backend.reference.dto.ModuleReferenceDto;
import com.learningplatform.backend.reference.dto.ReferenceItemDto;
import com.learningplatform.backend.reference.model.ModuleReference;
import com.learningplatform.backend.reference.repository.ModuleReferenceRepository;
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
public class ModuleReferenceService {

    private final ModuleReferenceRepository references;
    private final ModuleStructureRepository structures;
    private final LearningTopicRepository topics;

    public ModuleReferenceService(ModuleReferenceRepository references,
                                  ModuleStructureRepository structures,
                                  LearningTopicRepository topics) {
        this.references = references;
        this.structures = structures;
        this.topics = topics;
    }

    public ModuleReferenceDto getReferences(String moduleSlug, String languageCode) {
        if (languageCode == null || languageCode.isBlank() || languageCode.length() > 10) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid language code");
        }

        ModuleStructure module = structures.findBySlug(moduleSlug)
                .orElseThrow(ModuleReferenceService::notFound);

        // Match Overview/Roadmap visibility and avoid exposing hidden content.
        if (module.getType() != StructureType.MODULE || !Boolean.TRUE.equals(module.getActive())) {
            throw notFound();
        }
        Long topicId = module.getTopic().getId();
        if (topics.findVisibleById(topicId, TopicStatus.HIDDEN).isEmpty()) {
            throw notFound();
        }
        assertVisibleParents(module, topicId);

        List<ModuleReference> items = references.findVisibleLanguageReferences(
                module.getIdStructure(), languageCode);
        // No empty/missing translation fallback to a different language.
        if (items.isEmpty()) {
            throw notFound();
        }

        return new ModuleReferenceDto(module.getSlug(), languageCode,
                items.stream().map(this::toDto).toList());
    }

    private ReferenceItemDto toDto(ModuleReference item) {
        return new ReferenceItemDto(
                item.getId(), item.getTitle(), item.getUrl(),
                item.getDescription() == null ? "" : item.getDescription(),
                item.getSortOrder());
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
                    .orElseThrow(ModuleReferenceService::notFound);
            if (!topicId.equals(parent.getTopic().getId()) || !Boolean.TRUE.equals(parent.getActive())) {
                throw notFound();
            }
            parentKey = parent.getParentNodeKey();
        }
    }

    private static ResponseStatusException notFound() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Module references not found");
    }
}
