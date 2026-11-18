package com.learningplatform.backend.category.repository;

import java.util.List;

import com.learningplatform.backend.category.model.Category;
import org.springframework.data.repository.Repository;

/** Read-only repository for the category catalog. */
public interface CategoryRepository extends Repository<Category, Long> {

    List<Category> findByActiveTrueOrderBySortOrderAscIdAsc();
}
