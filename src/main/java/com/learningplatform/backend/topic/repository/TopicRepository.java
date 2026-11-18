package com.learningplatform.backend.topic.repository;

import java.util.List;
import java.util.Optional;

import com.learningplatform.backend.topic.model.Topic;
import com.learningplatform.backend.topic.model.TopicStatus;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

/** Read-only repository for Topics and their Category visibility. */
public interface TopicRepository extends Repository<Topic, Long> {

    @Query("""
            select t from Topic t
            join fetch t.category c
            where c.active = true and t.status <> :hidden
            order by c.sortOrder, t.sortOrder, t.id
            """)
    List<Topic> findVisibleTopics(@Param("hidden") TopicStatus hidden);

    @Query("""
            select t from Topic t
            join fetch t.category c
            where t.id = :topicId and c.active = true and t.status <> :hidden
            """)
    Optional<Topic> findVisibleById(
            @Param("topicId") Long topicId,
            @Param("hidden") TopicStatus hidden);

    Optional<Topic> findBySlug(String slug);
}
