package com.learningplatform.backend.structure.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.learningplatform.backend.catalog.model.LearningTopic;
import com.learningplatform.backend.catalog.model.TopicStatus;
import com.learningplatform.backend.catalog.repository.LearningTopicRepository;
import com.learningplatform.backend.structure.dto.ModuleStructureCatalogDto;
import com.learningplatform.backend.structure.dto.ModuleStructureNodeDto;
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
public class ModuleStructureService {

    private static final Comparator<ModuleStructure> SIBLING_ORDER =
            Comparator.comparing(ModuleStructure::getSortOrder)
                    .thenComparing(ModuleStructure::getIdStructure);

    private final LearningTopicRepository topics;
    private final ModuleStructureRepository structures;

    public ModuleStructureService(LearningTopicRepository topics, ModuleStructureRepository structures) {
        this.topics = topics;
        this.structures = structures;
    }

    public ModuleStructureCatalogDto getStructure(Long topicId) {
        LearningTopic topic = topics.findVisibleById(topicId, TopicStatus.HIDDEN)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Topic not found"));

        // Load once; assemble by natural keys without extra DB queries per level.
        Map<String, List<ModuleStructure>> byParent = new HashMap<>();
        for (ModuleStructure node : structures.findByTopicId(topicId)) {
            byParent.computeIfAbsent(node.getParentNodeKey(), ignored -> new ArrayList<>()).add(node);
        }
        byParent.values().forEach(nodes -> nodes.sort(SIBLING_ORDER));

        String rootPath = "/module/" + topic.getSlug();
        ModuleStructureNodeDto root = new ModuleStructureNodeDto(
                null, "topic:" + topic.getId(), topic.getName(), StructureType.GROUP,
                null, rootPath, null, null, topic.getSlug(), topic.getDescription(),
                null, childrenOf(null, rootPath, byParent, new HashSet<>()));
        return new ModuleStructureCatalogDto(1, root);
    }

    private static List<ModuleStructureNodeDto> childrenOf(
            String parentKey,
            String parentPath,
            Map<String, List<ModuleStructure>> byParent,
            Set<String> ancestors) {
        List<ModuleStructureNodeDto> result = new ArrayList<>();
        for (ModuleStructure node : byParent.getOrDefault(parentKey, List.of())) {
            // An inactive node hides its entire subtree, not just its own record.
            if (!Boolean.TRUE.equals(node.getActive())) {
                continue;
            }
            if (!ancestors.add(node.getNodeKey())) {
                throw new IllegalStateException("Cycle in module structure: " + node.getNodeKey());
            }
            try {
                String path = parentPath + "/" + node.getSlug();
                result.add(new ModuleStructureNodeDto(
                        node.getIdStructure(), node.getNodeKey(), node.getName(), node.getType(),
                        node.getType() == StructureType.MODULE ? node.getNodeKey() : null,
                        path, node.getNodeKey(), node.getParentNodeKey(), node.getSlug(),
                        node.getDescription(), node.getSortOrder(),
                        childrenOf(node.getNodeKey(), path, byParent, ancestors)));
            } finally {
                ancestors.remove(node.getNodeKey());
            }
        }
        return List.copyOf(result);
    }
}
