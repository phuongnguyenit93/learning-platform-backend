package com.learningplatform.backend.module.knowledge.controller;

import com.learningplatform.backend.module.knowledge.dto.ModuleKnowledgeDto;
import com.learningplatform.backend.module.knowledge.dto.ModuleChapterInsightsDto;
import com.learningplatform.backend.module.knowledge.service.ModuleChapterInsightsService;
import com.learningplatform.backend.module.knowledge.service.ModuleKnowledgeService;
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
public class ModuleKnowledgeController {

    private final ModuleKnowledgeService knowledge;
    private final ModuleChapterInsightsService insights;

    public ModuleKnowledgeController(ModuleKnowledgeService knowledge, ModuleChapterInsightsService insights) {
        this.knowledge = knowledge;
        this.insights = insights;
    }

    @GetMapping(
            value = "/{moduleSlug}/knowledge/{chapterSlug}/{sectionSlug}",
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ModuleKnowledgeDto getKnowledge(
            @PathVariable String moduleSlug,
            @PathVariable String chapterSlug,
            @PathVariable String sectionSlug,
            @RequestParam("lang") String languageCode) {
        return knowledge.getKnowledge(moduleSlug, chapterSlug, sectionSlug, languageCode);
    }

    @GetMapping(
            value = "/{moduleSlug}/chapters/{chapterSlug}/insights",
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ModuleChapterInsightsDto getChapterInsights(
            @PathVariable String moduleSlug,
            @PathVariable String chapterSlug,
            @RequestParam("lang") String languageCode) {
        return insights.getInsights(moduleSlug, chapterSlug, languageCode);
    }
}
