package com.learningplatform.backend.catalog.service;

import java.util.List;

import com.learningplatform.backend.catalog.dto.LearningCategoryDto;
import com.learningplatform.backend.catalog.dto.LearningTopicDto;
import com.learningplatform.backend.catalog.model.LearningCategory;
import com.learningplatform.backend.catalog.model.TopicStatus;
import com.learningplatform.backend.catalog.repository.LearningCategoryRepository;
import com.learningplatform.backend.catalog.repository.LearningTopicRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Profile("oracle")
@Transactional(readOnly = true)
public class LearningCatalogService {

    private final LearningCategoryRepository categories;
    private final LearningTopicRepository topics;

    public LearningCatalogService(
            LearningCategoryRepository categories,
            LearningTopicRepository topics) {
        this.categories = categories;
        this.topics = topics;
    }

    public List<LearningCategoryDto> getCategories() {
        return categories.findActiveCategories().stream()
                .map(LearningCatalogService::toDto)
                .toList();
    }

    public List<LearningTopicDto> getTopics() {
        return topics.findVisibleTopics(TopicStatus.HIDDEN).stream()
                .map(topic -> new LearningTopicDto(
                        topic.getId(),
                        topic.getCode(),
                        topic.getSlug(),
                        topic.getName(),
                        topic.getDescription(),
                        topic.getIconSvg(),
                        topic.getStatus(),
                        topic.getSortOrder(),
                        toDto(topic.getCategory())))
                .toList();
    }

    private static LearningCategoryDto toDto(LearningCategory category) {
        return new LearningCategoryDto(
                category.getId(),
                category.getCode(),
                category.getName(),
                category.getSortOrder());
    }
}
