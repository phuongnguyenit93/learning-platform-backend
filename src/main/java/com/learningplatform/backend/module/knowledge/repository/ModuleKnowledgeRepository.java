package com.learningplatform.backend.module.knowledge.repository;

import java.util.List;
import java.util.Optional;

import com.learningplatform.backend.module.knowledge.model.ModuleKnowledge;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

/** Read-only language-independent Knowledge metadata attached to a Section. */
public interface ModuleKnowledgeRepository extends Repository<ModuleKnowledge, Long> {

    Optional<ModuleKnowledge> findByMenuIdAndActiveTrue(Long menuId);

    Optional<ModuleKnowledge> findByIdAndActiveTrue(Long id);

    /**
     * Read-only Oracle/H2 scalar projection for menu availability. REGEXP_LIKE
     * works on CLOBs without returning their content to Hibernate/Jackson.
     * Oracle and H2 both support the Perl \S non-whitespace character class.
     */
    @Query(value = """
            select menu.ID as menuId, knowledge.ID as knowledgeId,
                   knowledge.DIFFICULT as difficulty,
                   case when knowledge.DIFFICULT is not null
                         and translation.STATUS = 'WRITTEN'
                         and REGEXP_LIKE(translation.CONTENT, '\\S')
                         and REGEXP_LIKE(translation.SUMMARY, '\\S')
                         and REGEXP_LIKE(translation.FOCUS, '\\S')
                         and REGEXP_LIKE(translation.PROBLEM_SOLVED, '\\S')
                         and REGEXP_LIKE(translation.MECHANISM, '\\S')
                         and REGEXP_LIKE(translation.APPLICATION_VALUE, '\\S')
                        then 1 else 0 end as available
            from MODULE_MENU menu
            left join MODULE_KNOWLEDGE knowledge
                   on knowledge.MENU_ID = menu.ID
                  and knowledge.IS_ACTIVE = 1
            left join MODULE_KNOWLEDGE_LANGUAGE translation
                   on translation.KNOWLEDGE_ID = knowledge.ID
                  and translation.LANGUAGE_CODE = :languageCode
            where menu.ID in (:sectionIds)
              and menu.NODE_TYPE = 'SECTION'
              and menu.IS_ACTIVE = 1
            """, nativeQuery = true)
    List<KnowledgeAvailability> findAvailabilityBySectionIdsAndLanguage(
            @Param("sectionIds") List<Long> sectionIds,
            @Param("languageCode") String languageCode);

    interface KnowledgeAvailability {
        Long getMenuId();
        Long getKnowledgeId();
        String getDifficulty();
        Integer getAvailable();
    }
}
