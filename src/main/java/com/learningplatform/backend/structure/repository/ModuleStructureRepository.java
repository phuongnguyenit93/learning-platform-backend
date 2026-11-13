package com.learningplatform.backend.structure.repository;

import java.util.List;
import java.util.Optional;

import com.learningplatform.backend.structure.model.ModuleStructure;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

// Read-only for now; content and structure changes are managed in Oracle.
public interface ModuleStructureRepository extends Repository<ModuleStructure, Long> {

    Optional<ModuleStructure> findByNodeKey(String nodeKey);

    Optional<ModuleStructure> findBySlug(String slug);

    @Query("""
            select n from ModuleStructure n
            where n.topic.id = :topicId
            order by n.sortOrder, n.idStructure
            """)
    List<ModuleStructure> findByTopicId(@Param("topicId") Long topicId);
}
