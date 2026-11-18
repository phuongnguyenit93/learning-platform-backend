package com.learningplatform.backend.topic.repository;

import java.util.List;
import java.util.Optional;

import com.learningplatform.backend.topic.model.TopicGroup;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

/** Read-only access to the hierarchical groups owned by a Topic. */
public interface TopicGroupRepository extends Repository<TopicGroup, Long> {

    @Query("""
            select g from TopicGroup g
            join fetch g.topic t
            left join fetch g.parentGroup p
            where t.id = :topicId
            order by g.sortOrder, g.id
            """)
    List<TopicGroup> findByTopicId(@Param("topicId") Long topicId);

    Optional<TopicGroup> findById(Long id);
}
