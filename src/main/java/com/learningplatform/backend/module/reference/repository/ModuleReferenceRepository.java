package com.learningplatform.backend.module.reference.repository;

import java.util.List;

import com.learningplatform.backend.module.reference.model.ModuleReference;
import org.springframework.data.repository.Repository;

/** Read-only repository for language-independent reference URLs. */
public interface ModuleReferenceRepository extends Repository<ModuleReference, Long> {

    List<ModuleReference> findByModule_IdAndActiveTrueOrderBySortOrderAscIdAsc(Long moduleId);
}
