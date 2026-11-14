package com.learningplatform.backend.reference.repository;

import java.util.List;

import com.learningplatform.backend.reference.model.ModuleReference;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

/** Read-only: the YAML importer manages all content writes outside this API. */
public interface ModuleReferenceRepository extends Repository<ModuleReference, Long> {

    @Query("""
            select r from ModuleReference r
            join r.language l
            where r.moduleStructure.idStructure = :idStructure
              and l.code = :languageCode
              and l.active = true
            order by r.sortOrder asc, r.id asc
            """)
    List<ModuleReference> findVisibleLanguageReferences(
            @Param("idStructure") Long idStructure,
            @Param("languageCode") String languageCode);
}
