package com.learningplatform.backend.topic.repository;

import java.util.List;
import java.util.Optional;

import com.learningplatform.backend.topic.model.TopicGroupModule;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

/** Read-only repository for the unique placement of each Module in the Topic tree. */
public interface TopicGroupModuleRepository extends Repository<TopicGroupModule, Long> {

    @Query("""
            select placement from TopicGroupModule placement
            join fetch placement.module m
            where placement.topic.id = :topicId
              and placement.topicGroup is null
              and placement.active = true
              and m.active = true
            order by placement.sortOrder, m.id
            """)
    List<TopicGroupModule> findActiveDirectModules(@Param("topicId") Long topicId);

    @Query("""
            select placement from TopicGroupModule placement
            join fetch placement.module m
            where placement.topicGroup.id = :groupId
              and placement.active = true
              and m.active = true
            order by placement.sortOrder, m.id
            """)
    List<TopicGroupModule> findActiveModulesByGroup(@Param("groupId") Long groupId);

    @Query("""
            select placement from TopicGroupModule placement
            join fetch placement.module m
            join fetch placement.topicGroup g
            where g.topic.id = :topicId
              and placement.active = true
              and m.active = true
            order by placement.sortOrder, m.id
            """)
    List<TopicGroupModule> findActiveGroupedModulesByTopic(@Param("topicId") Long topicId);

    Optional<TopicGroupModule> findByModule_Id(Long moduleId);

    Optional<TopicGroupModule> findByModule_IdAndActiveTrue(Long moduleId);
}
