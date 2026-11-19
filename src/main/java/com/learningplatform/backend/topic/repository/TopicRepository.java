package com.learningplatform.backend.topic.repository;

import java.util.List;
import java.util.Optional;

import com.learningplatform.backend.topic.model.Topic;
import com.learningplatform.backend.topic.model.TopicStatus;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;

public interface TopicRepository extends JpaRepository<Topic, Long> {

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

    boolean existsByCodeAndIdNot(String code, Long id);

    boolean existsBySlugAndIdNot(String slug, Long id);

    @Query("""
            select t from Topic t join fetch t.category c
            order by c.sortOrder, c.id, t.sortOrder, t.id
            """)
    List<Topic> findAllForAdmin();

    @Query("select coalesce(max(t.sortOrder), 0) from Topic t where t.category.id = :categoryId")
    Integer maxSortOrderInCategory(@Param("categoryId") Long categoryId);
}
