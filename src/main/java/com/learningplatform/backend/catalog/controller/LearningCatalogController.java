package com.learningplatform.backend.catalog.controller;

import java.util.List;

import com.learningplatform.backend.catalog.dto.LearningCategoryDto;
import com.learningplatform.backend.catalog.dto.LearningTopicDto;
import com.learningplatform.backend.catalog.service.LearningCatalogService;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Profile("oracle")
@RequestMapping("/api/v1")
@CrossOrigin(origins = {
        "https://nguyendodinhphuong.name.vn",
        "https://learning-platform.phuongnguyenit93.workers.dev",
        "http://localhost:5173"
})
public class LearningCatalogController {

    private final LearningCatalogService service;

    public LearningCatalogController(LearningCatalogService service) {
        this.service = service;
    }

    @GetMapping(value = "/categories", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<LearningCategoryDto> categories() {
        return service.getCategories();
    }

    @GetMapping(value = "/topics", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<LearningTopicDto> topics() {
        return service.getTopics();
    }
}
