package com.learningplatform.backend.category.controller;

import java.net.URI;
import java.util.List;

import com.learningplatform.backend.category.dto.AdminCategoryDto;
import com.learningplatform.backend.category.dto.CategoryMutationRequest;
import com.learningplatform.backend.category.service.CategoryService;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Profile("oracle")
@RequestMapping("/api/admin/categories")
public class AdminCategoryController {
    private final CategoryService categories;

    public AdminCategoryController(CategoryService categories) {
        this.categories = categories;
    }

    @GetMapping
    public List<AdminCategoryDto> list() {
        return categories.getAllForAdmin();
    }

    @PostMapping
    public ResponseEntity<AdminCategoryDto> create(@RequestBody CategoryMutationRequest request) {
        var created = categories.create(request);
        return ResponseEntity.created(URI.create("/api/admin/categories/" + created.id())).body(created);
    }

    @PutMapping("/{id}")
    public AdminCategoryDto update(@PathVariable Long id, @RequestBody CategoryMutationRequest request) {
        return categories.update(id, request);
    }
}
