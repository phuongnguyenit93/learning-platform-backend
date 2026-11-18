package com.learningplatform.backend.module.core.service;

import java.util.HashSet;
import java.util.Set;

import com.learningplatform.backend.module.core.dto.ModuleCoreDto;
import com.learningplatform.backend.module.core.model.LearningModule;
import com.learningplatform.backend.module.core.repository.LearningModuleRepository;
import com.learningplatform.backend.topic.model.TopicGroup;
import com.learningplatform.backend.topic.model.TopicGroupModule;
import com.learningplatform.backend.topic.model.TopicStatus;
import com.learningplatform.backend.topic.repository.TopicGroupModuleRepository;
import com.learningplatform.backend.topic.repository.TopicRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Resolves the canonical Module independently from its current navigation
 * placement and enforces the same visibility rules for every Module API.
 */
@Service
@Profile("oracle")
@Transactional(readOnly = true)
public class ModuleCoreService {

    private final LearningModuleRepository modules;
    private final TopicGroupModuleRepository placements;
    private final TopicRepository topics;

    public ModuleCoreService(
            LearningModuleRepository modules,
            TopicGroupModuleRepository placements,
            TopicRepository topics) {
        this.modules = modules;
        this.placements = placements;
        this.topics = topics;
    }

    public ModuleCoreDto getModuleCore(String moduleSlug) {
        LearningModule module = getVisibleModule(moduleSlug);
        return new ModuleCoreDto(
                module.getId(),
                module.getServiceName(),
                module.getSlug(),
                module.getName(),
                module.getServiceNameDescription(),
                module.getSpecialNote());
    }

    /** Returns the canonical Module only when its current placement is visible. */
    public LearningModule getVisibleModule(String moduleSlug) {
        LearningModule module = modules.findBySlugAndActiveTrue(moduleSlug)
                .orElseThrow(ModuleCoreService::notFound);

        TopicGroupModule placement = placements.findByModule_IdAndActiveTrue(module.getId())
                .orElseThrow(ModuleCoreService::notFound);

        // The schema enforces exactly one placement owner (Topic XOR Group).
        // Recheck here so malformed legacy data cannot expose a Module.
        TopicGroup group = placement.getTopicGroup();
        Long topicId;
        if (group == null && placement.getTopic() != null) {
            topicId = placement.getTopic().getId();
        } else if (group != null && placement.getTopic() == null) {
            topicId = group.getTopic().getId();
            assertVisibleGroupAncestors(group, topicId);
        } else {
            throw notFound();
        }

        if (topics.findVisibleById(topicId, TopicStatus.HIDDEN).isEmpty()) {
            throw notFound();
        }
        return module;
    }

    private static void assertVisibleGroupAncestors(TopicGroup group, Long topicId) {
        Set<Long> visited = new HashSet<>();
        TopicGroup current = group;
        while (current != null) {
            if (current.getId() == null || !visited.add(current.getId())
                    || !Boolean.TRUE.equals(current.getActive())
                    || current.getTopic() == null
                    || !topicId.equals(current.getTopic().getId())) {
                throw notFound();
            }
            current = current.getParentGroup();
        }
    }

    private static ResponseStatusException notFound() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Module not found");
    }
}
