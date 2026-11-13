package com.learningplatform.backend.module_overview.controller;

import com.learningplatform.backend.module_overview.dto.ModuleOverviewDto;
import com.learningplatform.backend.module_overview.service.ModuleOverviewService;
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
public class ModuleOverviewController {

    private final ModuleOverviewService service;

    public ModuleOverviewController(ModuleOverviewService service) {
        this.service = service;
    }

    @GetMapping(value = "/{moduleSlug}/overview", produces = MediaType.APPLICATION_JSON_VALUE)
    public ModuleOverviewDto getOverview(@PathVariable String moduleSlug,
                                         @RequestParam("lang") String languageCode) {
        return service.getOverview(moduleSlug, languageCode);
    }
}
