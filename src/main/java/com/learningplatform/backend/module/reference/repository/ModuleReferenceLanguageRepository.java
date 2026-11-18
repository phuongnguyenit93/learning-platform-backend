package com.learningplatform.backend.module.reference.repository;

import java.util.List;
import java.util.Optional;

import com.learningplatform.backend.module.reference.model.ModuleReferenceLanguage;
import com.learningplatform.backend.module.reference.model.ModuleReferenceLanguageId;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

/** Read-only localized title and description for reference URLs. */
public interface ModuleReferenceLanguageRepository
        extends Repository<ModuleReferenceLanguage, ModuleReferenceLanguageId> {

    @Query("""
            select translation from ModuleReferenceLanguage translation
            join fetch translation.reference reference
            join fetch translation.language language
            where reference.module.id = :moduleId
              and reference.active = true
              and language.code = :languageCode
              and language.active = true
            order by reference.sortOrder, reference.id
            """)
    List<ModuleReferenceLanguage> findVisibleByModuleAndLanguage(
            @Param("moduleId") Long moduleId,
            @Param("languageCode") String languageCode);

    Optional<ModuleReferenceLanguage> findById(ModuleReferenceLanguageId id);
}
