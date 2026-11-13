package com.learningplatform.backend.structure.controller;

import com.learningplatform.backend.structure.dto.ModuleStructureCatalogDto;
import com.learningplatform.backend.structure.service.ModuleStructureService;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Profile("oracle")
@RequestMapping("/api/v1/topics")
@CrossOrigin(origins = {
        "https://nguyendodinhphuong.name.vn",
        "https://learning-platform.phuongnguyenit93.workers.dev",
        "http://localhost:5173"
})
public class ModuleStructureController {

    private final ModuleStructureService service;

    public ModuleStructureController(ModuleStructureService service) {
        this.service = service;
    }

    @GetMapping(value = "/{topicId}/structure", produces = MediaType.APPLICATION_JSON_VALUE)
    public ModuleStructureCatalogDto getStructure(@PathVariable Long topicId) {
        return service.getStructure(topicId);
    }
}
