package com.learningplatform.backend.topic.controller;

import java.net.URI;

import com.learningplatform.backend.topic.dto.AdminGroupDto;
import com.learningplatform.backend.topic.dto.AdminStructureDto;
import com.learningplatform.backend.topic.dto.GroupMutationRequest;
import com.learningplatform.backend.topic.dto.SaveStructureRequest;
import com.learningplatform.backend.topic.service.TopicStructureAdminService;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Profile("oracle")
@RequestMapping("/api/admin")
public class AdminGroupController {

    private final TopicStructureAdminService structures;

    public AdminGroupController(TopicStructureAdminService structures) {
        this.structures = structures;
    }

    @GetMapping("/topics/{topicId}/structure")
    public AdminStructureDto getStructure(@PathVariable Long topicId) {
        return structures.getStructure(topicId);
    }

    @PostMapping("/topics/{topicId}/groups")
    public ResponseEntity<AdminGroupDto> create(@PathVariable Long topicId,
            @RequestBody GroupMutationRequest request) {
        var group = structures.createGroup(topicId, request);
        return ResponseEntity.created(URI.create("/api/admin/groups/" + group.id())).body(group);
    }

    @PutMapping("/groups/{groupId}")
    public AdminGroupDto edit(@PathVariable Long groupId, @RequestBody GroupMutationRequest request) {
        return structures.editGroup(groupId, request);
    }

    @PutMapping("/topics/{topicId}/structure")
    public AdminStructureDto saveStructure(@PathVariable Long topicId,
            @RequestBody SaveStructureRequest request) {
        return structures.saveStructure(topicId, request);
    }
}
