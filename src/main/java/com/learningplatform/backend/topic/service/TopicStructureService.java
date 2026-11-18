package com.learningplatform.backend.topic.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.learningplatform.backend.module.core.model.LearningModule;
import com.learningplatform.backend.topic.dto.TopicStructureCatalogDto;
import com.learningplatform.backend.topic.dto.TopicStructureNodeDto;
import com.learningplatform.backend.topic.dto.TopicStructureNodeKind;
import com.learningplatform.backend.topic.model.Topic;
import com.learningplatform.backend.topic.model.TopicGroup;
import com.learningplatform.backend.topic.model.TopicGroupModule;
import com.learningplatform.backend.topic.model.TopicStatus;
import com.learningplatform.backend.topic.repository.TopicGroupModuleRepository;
import com.learningplatform.backend.topic.repository.TopicGroupRepository;
import com.learningplatform.backend.topic.repository.TopicRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Profile("oracle")
@Transactional(readOnly = true)
public class TopicStructureService {

    private static final Comparator<TreeEntry> SIBLING_ORDER =
            Comparator.comparingInt(TreeEntry::sortOrder)
                    .thenComparingInt(TreeEntry::kindOrder)
                    .thenComparingLong(TreeEntry::databaseId);

    private final TopicRepository topics;
    private final TopicGroupRepository groups;
    private final TopicGroupModuleRepository placements;

    public TopicStructureService(
            TopicRepository topics,
            TopicGroupRepository groups,
            TopicGroupModuleRepository placements) {
        this.topics = topics;
        this.groups = groups;
        this.placements = placements;
    }

    public TopicStructureCatalogDto getStructure(Long topicId) {
        Topic topic = topics.findVisibleById(topicId, TopicStatus.HIDDEN)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Topic not found"));

        List<TopicGroup> allGroups = groups.findByTopicId(topicId);
        Map<Long, TopicGroup> groupsById = new HashMap<>();
        for (TopicGroup group : allGroups) {
            groupsById.put(group.getId(), group);
        }
        validateGroupHierarchy(topicId, allGroups, groupsById);

        Map<Long, List<TopicGroup>> activeChildrenByParent = new HashMap<>();
        List<TopicGroup> activeRoots = new ArrayList<>();
        for (TopicGroup group : allGroups) {
            if (!Boolean.TRUE.equals(group.getActive())) {
                continue;
            }
            TopicGroup parent = group.getParentGroup();
            if (parent == null) {
                activeRoots.add(group);
            } else if (Boolean.TRUE.equals(parent.getActive())) {
                activeChildrenByParent.computeIfAbsent(parent.getId(), ignored -> new ArrayList<>()).add(group);
            }
            // An active child of an inactive parent is intentionally hidden with that subtree.
        }

        Map<Long, List<TopicGroupModule>> modulesByGroup = new HashMap<>();
        for (TopicGroupModule placement : placements.findActiveGroupedModulesByTopic(topicId)) {
            Long groupId = placement.getTopicGroup().getId();
            modulesByGroup.computeIfAbsent(groupId, ignored -> new ArrayList<>()).add(placement);
        }

        List<TreeEntry> rootEntries = new ArrayList<>();
        Set<String> seenSlugs = new HashSet<>();
        Set<String> seenNodeKeys = new HashSet<>();
        for (TopicGroup group : activeRoots) {
            TopicStructureNodeDto node = buildGroupNode(
                    group, "/module/" + topic.getSlug(), activeChildrenByParent,
                    modulesByGroup, seenSlugs, seenNodeKeys, new HashSet<>());
            rootEntries.add(new TreeEntry(group.getSortOrder(), 0, group.getId(), node));
        }

        for (TopicGroupModule placement : placements.findActiveDirectModules(topicId)) {
            LearningModule module = placement.getModule();
            TopicStructureNodeDto node = buildModuleNode(
                    module, "/module/" + topic.getSlug(), placement.getSortOrder(),
                    seenSlugs, seenNodeKeys);
            rootEntries.add(new TreeEntry(placement.getSortOrder(), 1, module.getId(), node));
        }

        rootEntries.sort(SIBLING_ORDER);
        List<TopicStructureNodeDto> rootChildren = rootEntries.stream()
                .map(TreeEntry::node)
                .toList();

        String rootPath = "/module/" + topic.getSlug();
        TopicStructureNodeDto root = new TopicStructureNodeDto(
                "topic:" + topic.getId(),
                "TOPIC_" + topic.getId(),
                topic.getName(),
                TopicStructureNodeKind.GROUP,
                rootPath,
                topic.getSlug(),
                topic.getDescription(),
                null,
                rootChildren);

        return new TopicStructureCatalogDto(1, root);
    }

    private TopicStructureNodeDto buildGroupNode(
            TopicGroup group,
            String parentPath,
            Map<Long, List<TopicGroup>> childrenByParent,
            Map<Long, List<TopicGroupModule>> modulesByGroup,
            Set<String> seenSlugs,
            Set<String> seenNodeKeys,
            Set<Long> ancestors) {
        if (!ancestors.add(group.getId())) {
            throw new IllegalStateException("Cycle in Topic Group hierarchy at ID " + group.getId());
        }
        try {
            String nodeKey = "GROUP_" + group.getId();
            String path = parentPath + "/" + group.getSlug();
            registerIdentity(nodeKey, group.getSlug(), seenNodeKeys, seenSlugs);

            List<TreeEntry> entries = new ArrayList<>();
            for (TopicGroup child : childrenByParent.getOrDefault(group.getId(), List.of())) {
                TopicStructureNodeDto node = buildGroupNode(
                        child, path, childrenByParent, modulesByGroup,
                        seenSlugs, seenNodeKeys, ancestors);
                entries.add(new TreeEntry(child.getSortOrder(), 0, child.getId(), node));
            }
            for (TopicGroupModule placement : modulesByGroup.getOrDefault(group.getId(), List.of())) {
                LearningModule module = placement.getModule();
                TopicStructureNodeDto node = buildModuleNode(
                        module, path, placement.getSortOrder(), seenSlugs, seenNodeKeys);
                entries.add(new TreeEntry(placement.getSortOrder(), 1, module.getId(), node));
            }
            entries.sort(SIBLING_ORDER);

            return new TopicStructureNodeDto(
                    nodeKey,
                    nodeKey,
                    group.getGroupName(),
                    TopicStructureNodeKind.GROUP,
                    path,
                    group.getSlug(),
                    null,
                    group.getSortOrder(),
                    entries.stream().map(TreeEntry::node).toList());
        } finally {
            ancestors.remove(group.getId());
        }
    }

    private TopicStructureNodeDto buildModuleNode(
            LearningModule module,
            String parentPath,
            Integer sortOrder,
            Set<String> seenSlugs,
            Set<String> seenNodeKeys) {
        String nodeKey = module.getServiceName();
        String path = parentPath + "/" + module.getSlug();
        registerIdentity(nodeKey, module.getSlug(), seenNodeKeys, seenSlugs);
        return new TopicStructureNodeDto(
                nodeKey,
                nodeKey,
                module.getName(),
                TopicStructureNodeKind.MODULE,
                path,
                module.getSlug(),
                module.getServiceNameDescription(),
                sortOrder,
                List.of());
    }

    private static void registerIdentity(
            String nodeKey,
            String slug,
            Set<String> seenNodeKeys,
            Set<String> seenSlugs) {
        if (nodeKey == null || nodeKey.isBlank()) {
            throw new IllegalStateException("Topic Structure node key must not be blank");
        }
        if (slug == null || slug.isBlank() || slug.contains("/")) {
            throw new IllegalStateException("Invalid Topic Structure slug: " + slug);
        }
        if (!seenNodeKeys.add(nodeKey)) {
            throw new IllegalStateException("Duplicate Topic Structure node key: " + nodeKey);
        }
        if (!seenSlugs.add(slug)) {
            throw new IllegalStateException("Duplicate Topic Structure slug: " + slug);
        }
    }

    private static void validateGroupHierarchy(
            Long topicId,
            List<TopicGroup> allGroups,
            Map<Long, TopicGroup> groupsById) {
        for (TopicGroup group : allGroups) {
            TopicGroup parent = group.getParentGroup();
            if (parent != null && !topicId.equals(parent.getTopic().getId())) {
                throw new IllegalStateException(
                        "Topic Group " + group.getId() + " has a parent in another Topic");
            }
        }

        Map<Long, Integer> visitState = new HashMap<>();
        for (TopicGroup group : allGroups) {
            validateNoCycle(group, groupsById, visitState);
        }
    }

    private static void validateNoCycle(
            TopicGroup group,
            Map<Long, TopicGroup> groupsById,
            Map<Long, Integer> visitState) {
        int state = visitState.getOrDefault(group.getId(), 0);
        if (state == 2) {
            return;
        }
        if (state == 1) {
            throw new IllegalStateException("Cycle in Topic Group hierarchy at ID " + group.getId());
        }

        visitState.put(group.getId(), 1);
        TopicGroup parent = group.getParentGroup();
        if (parent != null) {
            TopicGroup parentInTopic = groupsById.get(parent.getId());
            if (parentInTopic == null) {
                throw new IllegalStateException(
                        "Topic Group " + group.getId() + " references a parent outside its Topic");
            }
            validateNoCycle(parentInTopic, groupsById, visitState);
        }
        visitState.put(group.getId(), 2);
    }

    private record TreeEntry(
            Integer sortOrder,
            int kindOrder,
            Long databaseId,
            TopicStructureNodeDto node) {
        private TreeEntry {
            sortOrder = sortOrder == null ? Integer.MAX_VALUE : sortOrder;
        }
    }
}
