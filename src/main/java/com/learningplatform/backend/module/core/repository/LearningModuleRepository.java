package com.learningplatform.backend.module.core.repository;

import java.util.List;
import java.util.Optional;

import com.learningplatform.backend.module.core.model.LearningModule;
import org.springframework.data.repository.Repository;

/** Read-only repository for core Module identity and metadata. */
public interface LearningModuleRepository extends Repository<LearningModule, Long> {

    Optional<LearningModule> findById(Long id);

    Optional<LearningModule> findBySlugAndActiveTrue(String slug);

    Optional<LearningModule> findByServiceNameAndActiveTrue(String serviceName);

    List<LearningModule> findByActiveTrueOrderByNameAscIdAsc();
}
