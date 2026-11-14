package com.learningplatform.backend.reference.controller;

import com.learningplatform.backend.reference.dto.ModuleReferenceDto;
import com.learningplatform.backend.reference.service.ModuleReferenceService;
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
public class ModuleReferenceController {

    private final ModuleReferenceService service;

    public ModuleReferenceController(ModuleReferenceService service) {
        this.service = service;
    }

    @GetMapping(value = "/{moduleSlug}/reference", produces = MediaType.APPLICATION_JSON_VALUE)
    public ModuleReferenceDto getReferences(@PathVariable String moduleSlug,
                                            @RequestParam("lang") String languageCode) {
        return service.getReferences(moduleSlug, languageCode);
    }
}
