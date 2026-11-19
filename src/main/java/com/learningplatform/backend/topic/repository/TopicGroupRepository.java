package com.learningplatform.backend.topic.repository;

import java.util.List;
import java.util.Optional;

import com.learningplatform.backend.topic.model.TopicGroup;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;

public interface TopicGroupRepository extends JpaRepository<TopicGroup, Long> {

    @Query("""
            select g from TopicGroup g
            join fetch g.topic t
            left join fetch g.parentGroup p
            where t.id = :topicId
            order by g.sortOrder, g.id
            """)
    List<TopicGroup> findByTopicId(@Param("topicId") Long topicId);

    Optional<TopicGroup> findById(Long id);

    @Query("select g.topic.id from TopicGroup g where g.id = :groupId")
    Optional<Long> findOwningTopicId(@Param("groupId") Long groupId);

    boolean existsByTopic_IdAndSlug(Long topicId, String slug);

    boolean existsByTopic_IdAndSlugAndIdNot(Long topicId, String slug, Long id);
}
