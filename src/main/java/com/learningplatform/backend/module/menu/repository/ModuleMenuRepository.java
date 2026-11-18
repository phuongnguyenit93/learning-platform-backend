package com.learningplatform.backend.module.menu.repository;

import java.util.List;
import java.util.Optional;

import com.learningplatform.backend.module.menu.model.MenuNodeType;
import com.learningplatform.backend.module.menu.model.ModuleMenu;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

/** Read-only repository for the two-level Chapter/Section menu tree. */
public interface ModuleMenuRepository extends Repository<ModuleMenu, Long> {

    @Query("""
            select menu from ModuleMenu menu
            join fetch menu.module module
            where module.id = :moduleId
              and menu.nodeType = :nodeType
              and menu.active = true
            order by menu.sortOrder, menu.id
            """)
    List<ModuleMenu> findActiveByModuleAndNodeType(
            @Param("moduleId") Long moduleId,
            @Param("nodeType") MenuNodeType nodeType);

    @Query("""
            select menu from ModuleMenu menu
            where menu.menuParentId = :chapterId
              and menu.menuParentType = :parentType
              and menu.nodeType = :nodeType
              and menu.active = true
            order by menu.sortOrder, menu.id
            """)
    List<ModuleMenu> findActiveChildren(
            @Param("chapterId") Long chapterId,
            @Param("parentType") MenuNodeType parentType,
            @Param("nodeType") MenuNodeType nodeType);

    @Query("""
            select menu from ModuleMenu menu
            where menu.menuParentId in :chapterIds
              and menu.menuParentType = :parentType
              and menu.nodeType = :nodeType
              and menu.active = true
            order by menu.menuParentId, menu.sortOrder, menu.id
            """)
    List<ModuleMenu> findActiveChildrenForChapters(
            @Param("chapterIds") List<Long> chapterIds,
            @Param("parentType") MenuNodeType parentType,
            @Param("nodeType") MenuNodeType nodeType);

    @Query("""
            select menu from ModuleMenu menu
            where menu.module.id = :moduleId
              and menu.nodeType = :nodeType
              and menu.slug = :slug
              and menu.active = true
            """)
    Optional<ModuleMenu> findActiveChapterByModuleAndSlug(
            @Param("moduleId") Long moduleId,
            @Param("nodeType") MenuNodeType nodeType,
            @Param("slug") String slug);

    @Query("""
            select menu from ModuleMenu menu
            where menu.menuParentId = :chapterId
              and menu.menuParentType = :parentType
              and menu.nodeType = :nodeType
              and menu.slug = :slug
              and menu.active = true
            """)
    Optional<ModuleMenu> findActiveSectionByChapterAndSlug(
            @Param("chapterId") Long chapterId,
            @Param("parentType") MenuNodeType parentType,
            @Param("nodeType") MenuNodeType nodeType,
            @Param("slug") String slug);

    Optional<ModuleMenu> findByIdAndActiveTrue(Long id);
}
