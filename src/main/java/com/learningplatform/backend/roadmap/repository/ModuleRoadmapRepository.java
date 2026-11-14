package com.learningplatform.backend.roadmap.repository;

import java.util.List;

import com.learningplatform.backend.roadmap.model.ModuleRoadmap;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

/** Read-only repository. Data is imported/maintained outside Spring Boot. */
public interface ModuleRoadmapRepository extends Repository<ModuleRoadmap, Long> {

    @Query("""
            select r from ModuleRoadmap r
            join r.language l
            where r.moduleStructure.idStructure = :idStructure
              and l.code = :languageCode
              and l.active = true
            order by r.sortOrder asc, r.id asc
            """)
    List<ModuleRoadmap> findVisibleLanguageMilestones(
            @Param("idStructure") Long idStructure,
            @Param("languageCode") String languageCode);
}
