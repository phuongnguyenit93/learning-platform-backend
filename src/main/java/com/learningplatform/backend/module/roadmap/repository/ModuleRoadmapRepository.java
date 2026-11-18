package com.learningplatform.backend.module.roadmap.repository;

import java.util.List;
import java.util.Optional;

import com.learningplatform.backend.module.roadmap.model.ModuleRoadmap;
import org.springframework.data.repository.Repository;

/** Read-only repository for language-independent roadmap milestones. */
public interface ModuleRoadmapRepository extends Repository<ModuleRoadmap, Long> {

    List<ModuleRoadmap> findByModule_IdAndActiveTrueOrderBySortOrderAscIdAsc(Long moduleId);

    Optional<ModuleRoadmap> findByModule_IdAndRoadmapKey(Long moduleId, String roadmapKey);
}
