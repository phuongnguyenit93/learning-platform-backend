package com.learningplatform.backend.category.service;

import java.util.List;

import com.learningplatform.backend.category.dto.CategoryDto;
import com.learningplatform.backend.category.model.Category;
import com.learningplatform.backend.category.repository.CategoryRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    private static CategoryDto toDto(Category category) {
        return new CategoryDto(
                category.getId(),
                category.getCode(),
                category.getName(),
                category.getSortOrder());
    }
}
