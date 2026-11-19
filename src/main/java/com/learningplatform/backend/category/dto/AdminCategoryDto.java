package com.learningplatform.backend.category.dto;

public record AdminCategoryDto(Long id, String code, String name, Integer sortOrder, Boolean active) {
}
