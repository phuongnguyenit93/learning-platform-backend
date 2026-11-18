package com.learningplatform.backend.module.core.controller;

import com.learningplatform.backend.module.core.dto.ModuleCoreDto;
import com.learningplatform.backend.module.core.service.ModuleCoreService;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Profile("oracle")
@RequestMapping("/api/v1/modules")
@CrossOrigin(origins = {
        "https://nguyendodinhphuong.name.vn",
        "https://learning-platform.phuongnguyenit93.workers.dev",
        "http://localhost:5173"
})
public class ModuleCoreController {

    private final ModuleCoreService modules;

    public ModuleCoreController(ModuleCoreService modules) {
        this.modules = modules;
    }

    @GetMapping(value = "/{moduleSlug}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ModuleCoreDto getModule(@PathVariable String moduleSlug) {
        return modules.getModuleCore(moduleSlug);
    }
}
