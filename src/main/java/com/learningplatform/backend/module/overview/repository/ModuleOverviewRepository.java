package com.learningplatform.backend.module.overview.repository;

import java.util.Optional;

import com.learningplatform.backend.module.overview.model.ModuleOverview;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

/** Read-only repository for one localized Markdown overview per Module. */
public interface ModuleOverviewRepository extends Repository<ModuleOverview, Long> {

    @Query("""
            select overview from ModuleOverview overview
            join fetch overview.module module
            join fetch overview.language language
            where module.slug = :moduleSlug
              and module.active = true
              and language.code = :languageCode
              and language.active = true
            """)
    Optional<ModuleOverview> findVisibleByModuleSlugAndLanguage(
            @Param("moduleSlug") String moduleSlug,
            @Param("languageCode") String languageCode);

    Optional<ModuleOverview> findByModule_IdAndLanguage_Code(Long moduleId, String languageCode);
}
