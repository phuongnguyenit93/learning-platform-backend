package com.learningplatform.backend.topic.service;

import java.util.List;

import com.learningplatform.backend.category.dto.CategoryDto;
import com.learningplatform.backend.category.repository.CategoryRepository;
import com.learningplatform.backend.shared.persistence.CatalogInput;
import com.learningplatform.backend.topic.dto.TopicDto;
import com.learningplatform.backend.topic.dto.TopicMutationRequest;
import com.learningplatform.backend.topic.model.Topic;
import com.learningplatform.backend.topic.model.TopicStatus;
import com.learningplatform.backend.topic.repository.TopicRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Profile("oracle")
@Transactional(readOnly = true)
public class TopicService {

    private final TopicRepository topics;
    private final CategoryRepository categories;

    public TopicService(TopicRepository topics, CategoryRepository categories) {
        this.topics = topics;
        this.categories = categories;
    }

    public List<TopicDto> getVisibleTopics() {
        return topics.findVisibleTopics(TopicStatus.HIDDEN).stream()
                .map(TopicService::toDto)
                .toList();
    }

    public List<TopicDto> getAllForAdmin() {
        return topics.findAllForAdmin().stream().map(TopicService::toDto).toList();
    }

    @Transactional
    public TopicDto create(Long categoryId, TopicMutationRequest request) {
        String code = CatalogInput.code(request.code());
        String slug = CatalogInput.slug(request.slug());
        String name = CatalogInput.name(request.name());
        String description = CatalogInput.description(request.description());
        var category = categories.findById(categoryId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found"));
        validateUnique(code, slug, -1L);
        var created = new Topic(category, code, slug, name, description,
                Math.addExact(topics.maxSortOrderInCategory(categoryId), 1));
        return toDto(topics.saveAndFlush(created));
    }

    @Transactional
    public TopicDto update(Long id, TopicMutationRequest request) {
        String code = CatalogInput.code(request.code());
        String slug = CatalogInput.slug(request.slug());
        String name = CatalogInput.name(request.name());
        String description = CatalogInput.description(request.description());
        var topic = topics.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Topic not found"));
        validateUnique(code, slug, id);
        topic.updateDetails(code, slug, name, description);
        return toDto(topics.saveAndFlush(topic));
    }

    private void validateUnique(String code, String slug, Long excludedId) {
        if (topics.existsByCodeAndIdNot(code, excludedId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Topic CODE already exists");
        }
        if (topics.existsBySlugAndIdNot(slug, excludedId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Topic SLUG already exists");
        }
    }

    private static TopicDto toDto(Topic topic) {
        var category = topic.getCategory();
        return new TopicDto(
                topic.getId(),
                topic.getCode(),
                topic.getSlug(),
                topic.getName(),
                topic.getDescription(),
                topic.getIconSvg(),
                topic.getStatus(),
                topic.getSortOrder(),
                new CategoryDto(
                        category.getId(),
                        category.getCode(),
                        category.getName(),
                        category.getSortOrder()));
    }
}
