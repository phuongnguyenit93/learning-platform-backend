package com.learningplatform.backend.category.controller;

import java.util.List;

import com.learningplatform.backend.category.dto.CategoryDto;
import com.learningplatform.backend.category.service.CategoryService;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Profile("oracle")
@RequestMapping("/api/v1/categories")
@CrossOrigin(origins = {
        "https://nguyendodinhphuong.name.vn",
        "https://learning-platform.phuongnguyenit93.workers.dev",
        "http://localhost:5173"
})
public class CategoryController {

    private final CategoryService categories;

    public CategoryController(CategoryService categories) {
        this.categories = categories;
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public List<CategoryDto> getCategories() {
        return categories.getActiveCategories();
    }
}
