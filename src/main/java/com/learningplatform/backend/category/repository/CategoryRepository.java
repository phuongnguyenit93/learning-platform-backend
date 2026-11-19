package com.learningplatform.backend.category.repository;

import java.util.List;
import java.util.Optional;

import com.learningplatform.backend.category.model.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    List<Category> findByActiveTrueOrderBySortOrderAscIdAsc();

    List<Category> findAllByOrderBySortOrderAscIdAsc();

    Optional<Category> findByCode(String code);

    boolean existsByCodeAndIdNot(String code, Long id);

    @Query("select coalesce(max(c.sortOrder), 0) from Category c")
    Integer maxSortOrder();
}
