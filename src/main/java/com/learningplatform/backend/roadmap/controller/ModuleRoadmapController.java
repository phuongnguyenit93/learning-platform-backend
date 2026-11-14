package com.learningplatform.backend.roadmap.controller;

import com.learningplatform.backend.roadmap.dto.ModuleRoadmapDto;
import com.learningplatform.backend.roadmap.service.ModuleRoadmapService;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Profile("oracle")
@RequestMapping("/api/v1/modules")
@CrossOrigin(origins = {
        "https://nguyendodinhphuong.name.vn",
        "https://learning-platform.phuongnguyenit93.workers.dev",
        "http://localhost:5173"
})
public class ModuleRoadmapController {

    private final ModuleRoadmapService service;

    public ModuleRoadmapController(ModuleRoadmapService service) {
        this.service = service;
    }

    @GetMapping(value = "/{moduleSlug}/roadmap", produces = MediaType.APPLICATION_JSON_VALUE)
    public ModuleRoadmapDto getRoadmap(@PathVariable String moduleSlug,
                                       @RequestParam("lang") String languageCode) {
        return service.getRoadmap(moduleSlug, languageCode);
    }
}
