package com.learningplatform.backend.category.service;

import java.util.List;

import com.learningplatform.backend.category.dto.CategoryDto;
import com.learningplatform.backend.category.dto.AdminCategoryDto;
import com.learningplatform.backend.category.dto.CategoryMutationRequest;
import com.learningplatform.backend.category.model.Category;
import com.learningplatform.backend.category.repository.CategoryRepository;
import com.learningplatform.backend.shared.persistence.CatalogInput;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Profile("oracle")
@Transactional(readOnly = true)
public class CategoryService {

    private final CategoryRepository categories;

    public CategoryService(CategoryRepository categories) {
        this.categories = categories;
    }

    public List<CategoryDto> getActiveCategories() {
        return categories.findByActiveTrueOrderBySortOrderAscIdAsc().stream()
                .map(CategoryService::toDto)
                .toList();
    }

    public List<AdminCategoryDto> getAllForAdmin() {
        return categories.findAllByOrderBySortOrderAscIdAsc().stream()
                .map(CategoryService::toAdminDto).toList();
    }

    @Transactional
    public AdminCategoryDto create(CategoryMutationRequest request) {
        String code = CatalogInput.code(request.code());
        String name = CatalogInput.name(request.name());
        if (categories.findByCode(code).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Category CODE already exists");
        }
        var created = new Category(code, name, Math.addExact(categories.maxSortOrder(), 1));
        return toAdminDto(categories.saveAndFlush(created));
    }

    @Transactional
    public AdminCategoryDto update(Long id, CategoryMutationRequest request) {
        String code = CatalogInput.code(request.code());
        String name = CatalogInput.name(request.name());
        var category = categories.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found"));
        if (categories.existsByCodeAndIdNot(code, id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Category CODE already exists");
        }
        category.updateDetails(code, name);
        return toAdminDto(categories.saveAndFlush(category));
    }

    private static AdminCategoryDto toAdminDto(Category category) {
        return new AdminCategoryDto(category.getId(), category.getCode(), category.getName(),
                category.getSortOrder(), category.getActive());
    }

    private static CategoryDto toDto(Category category) {
        return new CategoryDto(
                category.getId(),
                category.getCode(),
                category.getName(),
                category.getSortOrder());
    }
}
