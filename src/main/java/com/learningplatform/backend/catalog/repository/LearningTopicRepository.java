package com.learningplatform.backend.catalog.repository;

import java.util.List;
import java.util.Optional;

import com.learningplatform.backend.catalog.model.LearningTopic;
import com.learningplatform.backend.catalog.model.TopicStatus;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

// Only read operations are exposed; all writes are managed directly in Oracle.
public interface LearningTopicRepository extends Repository<LearningTopic, Long> {

    @Query("""
            select t from LearningTopic t
            join fetch t.category c
            where c.active = true and t.status <> :hidden
            order by c.sortOrder, t.sortOrder, t.id
            """)
    List<LearningTopic> findVisibleTopics(@Param("hidden") TopicStatus hidden);

    @Query("""
            select t from LearningTopic t
            join fetch t.category c
            where t.id = :topicId and c.active = true and t.status <> :hidden
            """)
    Optional<LearningTopic> findVisibleById(
            @Param("topicId") Long topicId, @Param("hidden") TopicStatus hidden);
}
