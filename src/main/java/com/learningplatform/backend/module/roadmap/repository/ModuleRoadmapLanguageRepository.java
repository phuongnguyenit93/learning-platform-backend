package com.learningplatform.backend.module.roadmap.repository;

import java.util.List;
import java.util.Optional;

import com.learningplatform.backend.module.roadmap.model.ModuleRoadmapLanguage;
import com.learningplatform.backend.module.roadmap.model.ModuleRoadmapLanguageId;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

/** Read-only localized roadmap content. */
public interface ModuleRoadmapLanguageRepository
        extends Repository<ModuleRoadmapLanguage, ModuleRoadmapLanguageId> {

    @Query("""
            select translation from ModuleRoadmapLanguage translation
            join fetch translation.roadmap roadmap
            join fetch translation.language language
            where roadmap.module.id = :moduleId
              and roadmap.active = true
              and language.code = :languageCode
              and language.active = true
            order by roadmap.sortOrder, roadmap.id
            """)
    List<ModuleRoadmapLanguage> findVisibleByModuleAndLanguage(
            @Param("moduleId") Long moduleId,
            @Param("languageCode") String languageCode);

    Optional<ModuleRoadmapLanguage> findById(ModuleRoadmapLanguageId id);
}
