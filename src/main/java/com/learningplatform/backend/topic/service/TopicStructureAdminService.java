package com.learningplatform.backend.topic.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.learningplatform.backend.topic.dto.AdminGroupDto;
import com.learningplatform.backend.topic.dto.AdminStructureDto;
import com.learningplatform.backend.topic.dto.AdminStructureNodeDto;
import com.learningplatform.backend.topic.dto.GroupMutationRequest;
import com.learningplatform.backend.topic.dto.SaveStructureRequest;
import com.learningplatform.backend.topic.dto.StructureMoveNode;
import com.learningplatform.backend.topic.model.Topic;
import com.learningplatform.backend.topic.model.TopicGroup;
import com.learningplatform.backend.topic.model.TopicGroupModule;
import com.learningplatform.backend.topic.repository.TopicGroupModuleRepository;
import com.learningplatform.backend.topic.repository.TopicGroupRepository;
import com.learningplatform.backend.topic.repository.TopicRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** Only /api/admin routes call this service. Mutations serialize on the Topic row. */
@Service
@Profile("oracle")
public class TopicStructureAdminService {

    private static final int MAX_DEPTH = 64;

    private final TopicRepository topics;
    private final TopicGroupRepository groups;
    private final TopicGroupModuleRepository placements;

    public TopicStructureAdminService(TopicRepository topics, TopicGroupRepository groups,
            TopicGroupModuleRepository placements) {
        this.topics = topics;
        this.groups = groups;
        this.placements = placements;
    }

    @Transactional(readOnly = true)
    public AdminStructureDto getStructure(Long topicId) {
        Topic topic = requireTopic(topicId);
        return buildSnapshot(topic, loadGroups(topicId), placements.findAllByOwningTopic(topicId));
    }

    @Transactional
    public AdminGroupDto createGroup(Long topicId, GroupMutationRequest request) {
        Topic topic = lockTopic(topicId);
        if (request == null) throw invalid("Group details and revision are required");
        String name = requiredName(request.name());
        String slug = requiredSlug(request.slug());
        boolean active = requiredActive(request.active());
        List<TopicGroup> allGroups = loadGroups(topicId);
        List<TopicGroupModule> allPlacements = placements.findAllByOwningTopic(topicId);
        requireRevision(request.revision(), buildSnapshot(topic, allGroups, allPlacements).revision());
        validateSlug(slug, null, allGroups, allPlacements);

        int last = 0;
        for (TopicGroup group : allGroups) {
            if (group.getParentGroup() == null) last = Math.max(last, group.getSortOrder());
        }
        for (TopicGroupModule placement : allPlacements) {
            if (placement.getTopicGroup() == null) last = Math.max(last, placement.getSortOrder());
        }
        var created = groups.saveAndFlush(new TopicGroup(topic, name, slug, Math.addExact(last, 1), active));
        return toDto(created);
    }

    @Transactional
    public AdminGroupDto editGroup(Long groupId, GroupMutationRequest request) {
        Long topicId = groups.findOwningTopicId(groupId)
                .orElseThrow(() -> notFound("Group not found"));
        Topic topic = lockTopic(topicId);
        if (request == null) throw invalid("Group details and revision are required");
        List<TopicGroup> allGroups = loadGroups(topicId);
        List<TopicGroupModule> allPlacements = placements.findAllByOwningTopic(topicId);
        requireRevision(request.revision(), buildSnapshot(topic, allGroups, allPlacements).revision());
        var existing = groups.findById(groupId)
                .orElseThrow(() -> notFound("Group not found"));
        String name = requiredName(request.name());
        String slug = requiredSlug(request.slug());
        boolean active = requiredActive(request.active());
        validateSlug(slug, groupId, allGroups, allPlacements);
        existing.edit(name, slug, active);
        groups.flush();
        return toDto(existing);
    }

    /** All node updates run inside exactly one transaction: any failure rolls all of them back. */
    @Transactional
    public AdminStructureDto saveStructure(Long topicId, SaveStructureRequest request) {
        Topic topic = lockTopic(topicId);
        if (request == null || request.revision() == null || request.revision().isBlank()
                || request.nodes() == null) throw invalid("Revision and complete nodes are required");

        List<TopicGroup> allGroups = loadGroups(topicId);
        List<TopicGroupModule> allPlacements = placements.findAllByOwningTopic(topicId);
        var before = buildSnapshot(topic, allGroups, allPlacements);
        requireRevision(request.revision(), before.revision());

        Map<Long, TopicGroup> byGroup = new HashMap<>();
        for (TopicGroup group : allGroups) byGroup.put(group.getId(), group);
        Map<Long, TopicGroupModule> byPlacement = new HashMap<>();
        for (TopicGroupModule placement : allPlacements) byPlacement.put(placement.getId(), placement);

        var groupMoves = new ArrayList<GroupMove>();
        var moduleMoves = new ArrayList<ModuleMove>();
        var visitedGroups = new HashSet<Long>();
        var visitedPlacements = new HashSet<Long>();
        collectMoves(request.nodes(), null, 0, byGroup, byPlacement,
                visitedGroups, visitedPlacements, groupMoves, moduleMoves);
        if (visitedGroups.size() != byGroup.size() || visitedPlacements.size() != byPlacement.size()) {
            throw invalid("The entire topic structure must be submitted without missing nodes");
        }

        for (GroupMove move : groupMoves) move.group().relocate(move.parent(), move.order());
        for (ModuleMove move : moduleMoves) move.placement().relocate(topic, move.parent(), move.order());
        groups.flush();
        placements.flush();
        return buildSnapshot(topic, allGroups, allPlacements);
    }

    private void collectMoves(List<StructureMoveNode> children, TopicGroup parent, int depth,
            Map<Long, TopicGroup> byGroup, Map<Long, TopicGroupModule> byPlacement,
            Set<Long> visitedGroups, Set<Long> visitedPlacements,
            List<GroupMove> groupMoves, List<ModuleMove> moduleMoves) {
        if (depth > MAX_DEPTH) throw invalid("Topic structure nesting is too deep");
        int order = 0;
        for (StructureMoveNode child : children) {
            if (child == null || child.id() == null) throw invalid("Missing node ID");
            order = Math.addExact(order, 1);
            if ("GROUP".equals(child.kind())) {
                TopicGroup group = byGroup.get(child.id());
                if (group == null || !visitedGroups.add(child.id())) {
                    throw invalid("Unknown or repeated Group ID: " + child.id());
                }
                if (child.children() == null) throw invalid("Group children are required");
                groupMoves.add(new GroupMove(group, parent, order));
                collectMoves(child.children(), group, depth + 1, byGroup, byPlacement,
                        visitedGroups, visitedPlacements, groupMoves, moduleMoves);
            } else if ("MODULE".equals(child.kind())) {
                TopicGroupModule placement = byPlacement.get(child.id());
                if (placement == null || !visitedPlacements.add(child.id())) {
                    throw invalid("Unknown or repeated Module placement ID: " + child.id());
                }
                if (child.children() != null && !child.children().isEmpty()) {
                    throw invalid("Modules cannot contain child nodes");
                }
                moduleMoves.add(new ModuleMove(placement, parent, order));
            } else {
                throw invalid("Node kind must be GROUP or MODULE");
            }
        }
    }

    private AdminStructureDto buildSnapshot(Topic topic, List<TopicGroup> allGroups,
            List<TopicGroupModule> allPlacements) {
        Map<Long, List<SortableNode>> children = new HashMap<>();
        Map<Long, TopicGroup> byId = new HashMap<>();
        for (TopicGroup group : allGroups) byId.put(group.getId(), group);

        for (TopicGroup group : allGroups) {
            TopicGroup parent = group.getParentGroup();
            if (parent != null && !byId.containsKey(parent.getId())) {
                throw new IllegalStateException("Group parent belongs to another Topic: " + group.getId());
            }
            Long parentId = parent == null ? null : parent.getId();
            children.computeIfAbsent(parentId, ignored -> new ArrayList<>())
                    .add(new SortableNode(group.getSortOrder(), 0, group.getId(), group, null));
        }
        for (TopicGroupModule placement : allPlacements) {
            TopicGroup group = placement.getTopicGroup();
            if (group != null && !byId.containsKey(group.getId())) {
                throw new IllegalStateException("Module placement belongs to another Topic");
            }
            if (group == null && (placement.getTopic() == null
                    || !topic.getId().equals(placement.getTopic().getId()))) {
                throw new IllegalStateException("Module root placement belongs to another Topic");
            }
            Long parentId = group == null ? null : group.getId();
            children.computeIfAbsent(parentId, ignored -> new ArrayList<>())
                    .add(new SortableNode(placement.getSortOrder(), 1, placement.getId(), null, placement));
        }
        children.values().forEach(items -> items.sort(Comparator
                .comparingInt(SortableNode::order)
                .thenComparingInt(SortableNode::kind)
                .thenComparingLong(SortableNode::id)));

        var visited = new HashSet<Long>();
        var rootNodes = toNodes(null, 0, children, visited);
        if (visited.size() != allGroups.size()) {
            throw new IllegalStateException("Cycle or unreachable Group detected");
        }
        return new AdminStructureDto(topic.getId(), revision(topic, allGroups, allPlacements), rootNodes);
    }

    private List<AdminStructureNodeDto> toNodes(Long parentId, int depth,
            Map<Long, List<SortableNode>> children, Set<Long> visited) {
        if (depth > MAX_DEPTH) throw new IllegalStateException("Topic group depth exceeded");
        List<AdminStructureNodeDto> nodes = new ArrayList<>();
        for (SortableNode child : children.getOrDefault(parentId, List.of())) {
            if (child.group() != null) {
                TopicGroup group = child.group();
                if (!visited.add(group.getId())) throw new IllegalStateException("Cycle in Topic groups");
                nodes.add(new AdminStructureNodeDto("GROUP", group.getId(), group.getGroupName(),
                        group.getSlug(), Boolean.TRUE.equals(group.getActive()),
                        toNodes(group.getId(), depth + 1, children, visited)));
            } else {
                var placement = child.placement();
                var module = placement.getModule();
                nodes.add(new AdminStructureNodeDto("MODULE", placement.getId(), module.getName(),
                        module.getSlug(), Boolean.TRUE.equals(placement.getActive())
                                && Boolean.TRUE.equals(module.getActive()), List.of()));
            }
        }
        return nodes;
    }

    /** Hash the full physical snapshot so edits to name, active flag or position invalidate stale drafts. */
    private String revision(Topic topic, List<TopicGroup> allGroups, List<TopicGroupModule> allPlacements) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            append(digest, topic.getId(), topic.getSlug());
            allGroups.stream().sorted(Comparator.comparing(TopicGroup::getId)).forEach(g ->
                    append(digest, "G", g.getId(), g.getGroupName(), g.getSlug(), g.getActive(),
                            g.getParentGroup() == null ? null : g.getParentGroup().getId(), g.getSortOrder()));
            allPlacements.stream().sorted(Comparator.comparing(TopicGroupModule::getId)).forEach(p ->
                    append(digest, "M", p.getId(), p.getModule().getId(), p.getModule().getSlug(),
                            p.getModule().getName(), p.getModule().getActive(), p.getActive(),
                            p.getTopicGroup() == null ? null : p.getTopicGroup().getId(),
                            p.getTopic() == null ? null : p.getTopic().getId(), p.getSortOrder()));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private static void append(MessageDigest digest, Object... values) {
        for (Object value : values) {
            String content = Objects.toString(value, "<NULL>");
            byte[] data = content.getBytes(StandardCharsets.UTF_8);
            digest.update((byte) (data.length >>> 24));
            digest.update((byte) (data.length >>> 16));
            digest.update((byte) (data.length >>> 8));
            digest.update((byte) data.length);
            digest.update(data);
        }
    }

    private void validateSlug(String slug, Long excludedGroupId,
            List<TopicGroup> allGroups, List<TopicGroupModule> allPlacements) {
        for (TopicGroup group : allGroups) {
            if (!group.getId().equals(excludedGroupId) && slug.equals(group.getSlug())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Group slug already exists in Topic");
            }
        }
        for (TopicGroupModule placement : allPlacements) {
            if (slug.equals(placement.getModule().getSlug())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Group slug collides with a Module slug");
            }
        }
    }

    private static String requiredName(String value) {
        if (value == null || value.isBlank() || value.trim().length() > 200) {
            throw invalid("Group name must contain 1 to 200 characters");
        }
        return value.trim();
    }

    private static String requiredSlug(String value) {
        if (value == null || value.isBlank() || value.trim().length() > 150
                || !value.trim().matches("[a-z0-9]+(?:-[a-z0-9]+)*")) {
            throw invalid("Group slug must use lowercase letters, numbers and hyphens (1-150)");
        }
        return value.trim();
    }

    private static boolean requiredActive(Boolean active) {
        if (active == null) throw invalid("Active is required");
        return active;
    }

    private static void requireRevision(String supplied, String current) {
        if (supplied == null || supplied.isBlank()) throw invalid("Structure revision is required");
        if (!supplied.equals(current)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Topic structure changed since it was loaded; reload before saving");
        }
    }

    private Topic requireTopic(Long topicId) {
        return topics.findById(topicId).orElseThrow(() -> notFound("Topic not found"));
    }

    private Topic lockTopic(Long topicId) {
        return topics.lockForStructureChange(topicId).orElseThrow(() -> notFound("Topic not found"));
    }

    private List<TopicGroup> loadGroups(Long topicId) {
        return groups.findByTopicId(topicId);
    }

    private static AdminGroupDto toDto(TopicGroup group) {
        return new AdminGroupDto(group.getId(), group.getTopic().getId(),
                group.getGroupName(), group.getSlug(), Boolean.TRUE.equals(group.getActive()),
                group.getParentGroup() == null ? null : group.getParentGroup().getId(), group.getSortOrder());
    }

    private static ResponseStatusException invalid(String reason) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, reason);
    }

    private static ResponseStatusException notFound(String reason) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, reason);
    }

    private record SortableNode(int order, int kind, long id, TopicGroup group, TopicGroupModule placement) { }
    private record GroupMove(TopicGroup group, TopicGroup parent, int order) { }
    private record ModuleMove(TopicGroupModule placement, TopicGroup parent, int order) { }
}
