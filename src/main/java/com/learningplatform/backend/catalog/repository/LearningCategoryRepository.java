package com.learningplatform.backend.catalog.repository;

import java.util.List;

import com.learningplatform.backend.catalog.model.LearningCategory;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

// Deliberately extends Repository, not JpaRepository: no save/delete methods.
public interface LearningCategoryRepository extends Repository<LearningCategory, Long> {

    @Query("select c from LearningCategory c where c.active = true order by c.sortOrder, c.id")
    List<LearningCategory> findActiveCategories();
}
