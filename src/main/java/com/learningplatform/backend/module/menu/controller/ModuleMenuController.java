package com.learningplatform.backend.module.menu.controller;

import com.learningplatform.backend.module.menu.dto.ModuleMenuDto;
import com.learningplatform.backend.module.menu.service.ModuleMenuService;
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
public class ModuleMenuController {

    private final ModuleMenuService menus;

    public ModuleMenuController(ModuleMenuService menus) {
        this.menus = menus;
    }

    @GetMapping(value = "/{moduleSlug}/menu", produces = MediaType.APPLICATION_JSON_VALUE)
    public ModuleMenuDto getMenu(
            @PathVariable String moduleSlug,
            @RequestParam("lang") String languageCode) {
        return menus.getMenu(moduleSlug, languageCode);
    }
}
