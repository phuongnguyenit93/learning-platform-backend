package com.learningplatform.backend.module.knowledge.repository;

import java.util.List;
import java.util.Optional;

import com.learningplatform.backend.module.knowledge.dto.KnowledgeInsightMetadataDto;
import com.learningplatform.backend.module.knowledge.model.ModuleKnowledgeLanguage;
import com.learningplatform.backend.module.knowledge.model.ModuleKnowledgeLanguageId;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

/** Read-only localized lesson content and QA metadata. */
public interface ModuleKnowledgeLanguageRepository
        extends Repository<ModuleKnowledgeLanguage, ModuleKnowledgeLanguageId> {

    @Query("""
            select translation from ModuleKnowledgeLanguage translation
            join fetch translation.language language
            where translation.knowledge.id = :knowledgeId
              and language.code = :languageCode
              and language.active = true
            """)
    Optional<ModuleKnowledgeLanguage> findVisibleByKnowledgeAndLanguage(
            @Param("knowledgeId") Long knowledgeId,
            @Param("languageCode") String languageCode);

    /**
     * Selects only scalar metadata; Hibernate must not materialize either
     * CONTENT or SUMMARY CLOB when rendering chapter comparisons.
     */
    @Query("""
            select new com.learningplatform.backend.module.knowledge.dto.KnowledgeInsightMetadataDto(
                translation.knowledge.id, translation.focus, translation.problemSolved,
                translation.mechanism, translation.applicationValue)
            from ModuleKnowledgeLanguage translation
            join translation.language language
            where translation.knowledge.id in :knowledgeIds
              and language.code = :languageCode
              and language.active = true
            """)
    List<KnowledgeInsightMetadataDto> findInsightMetadataByKnowledgeIdsAndLanguage(
            @Param("knowledgeIds") List<Long> knowledgeIds,
            @Param("languageCode") String languageCode);
}
