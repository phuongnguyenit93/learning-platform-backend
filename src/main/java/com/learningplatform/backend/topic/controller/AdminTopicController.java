package com.learningplatform.backend.topic.controller;

import java.net.URI;
import java.util.List;

import com.learningplatform.backend.topic.dto.TopicDto;
import com.learningplatform.backend.topic.dto.TopicMutationRequest;
import com.learningplatform.backend.topic.service.TopicService;
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
public class AdminTopicController {
    private final TopicService topics;

    public AdminTopicController(TopicService topics) {
        this.topics = topics;
    }

    @GetMapping("/topics")
    public List<TopicDto> list() {
        return topics.getAllForAdmin();
    }

    @PostMapping("/categories/{categoryId}/topics")
    public ResponseEntity<TopicDto> create(@PathVariable Long categoryId, @RequestBody TopicMutationRequest request) {
        var created = topics.create(categoryId, request);
        return ResponseEntity.created(URI.create("/api/admin/topics/" + created.id())).body(created);
    }

    @PutMapping("/topics/{id}")
    public TopicDto update(@PathVariable Long id, @RequestBody TopicMutationRequest request) {
        return topics.update(id, request);
    }
}
