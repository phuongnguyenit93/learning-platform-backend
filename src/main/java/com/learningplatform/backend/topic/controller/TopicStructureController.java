package com.learningplatform.backend.topic.controller;

import com.learningplatform.backend.topic.dto.TopicStructureCatalogDto;
import com.learningplatform.backend.topic.service.TopicStructureService;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Profile("oracle")
@RequestMapping("/api/v1/topics/{topicId}/structure")
@CrossOrigin(origins = {
        "https://nguyendodinhphuong.name.vn",
        "https://learning-platform.phuongnguyenit93.workers.dev",
        "http://localhost:5173"
})
public class TopicStructureController {

    private final TopicStructureService structures;

    public TopicStructureController(TopicStructureService structures) {
        this.structures = structures;
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public TopicStructureCatalogDto getStructure(@PathVariable Long topicId) {
        return structures.getStructure(topicId);
    }
}
