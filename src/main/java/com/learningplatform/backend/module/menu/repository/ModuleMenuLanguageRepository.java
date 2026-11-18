package com.learningplatform.backend.module.menu.repository;

import java.util.List;
import java.util.Optional;

import com.learningplatform.backend.module.menu.model.ModuleMenuLanguage;
import com.learningplatform.backend.module.menu.model.ModuleMenuLanguageId;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

/** Read-only translations for Chapter and Section menu nodes. */
public interface ModuleMenuLanguageRepository
        extends Repository<ModuleMenuLanguage, ModuleMenuLanguageId> {

    @Query("""
            select translation from ModuleMenuLanguage translation
            join fetch translation.language language
            where translation.menu.id = :menuId
              and language.code = :languageCode
              and language.active = true
            """)
    Optional<ModuleMenuLanguage> findVisibleByMenuAndLanguage(
            @Param("menuId") Long menuId,
            @Param("languageCode") String languageCode);

    @Query("""
            select translation from ModuleMenuLanguage translation
            join fetch translation.language language
            where translation.menu.id in :menuIds
              and language.code = :languageCode
              and language.active = true
            """)
    List<ModuleMenuLanguage> findVisibleByMenuIdsAndLanguage(
            @Param("menuIds") List<Long> menuIds,
            @Param("languageCode") String languageCode);

    List<ModuleMenuLanguage> findByLanguage_CodeAndMenu_IdIn(String languageCode, List<Long> menuIds);
}
