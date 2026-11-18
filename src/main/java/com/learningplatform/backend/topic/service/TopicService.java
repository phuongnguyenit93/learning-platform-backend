package com.learningplatform.backend.topic.service;

import java.util.List;

import com.learningplatform.backend.category.dto.CategoryDto;
import com.learningplatform.backend.topic.dto.TopicDto;
import com.learningplatform.backend.topic.model.Topic;
import com.learningplatform.backend.topic.model.TopicStatus;
import com.learningplatform.backend.topic.repository.TopicRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Profile("oracle")
@Transactional(readOnly = true)
public class TopicService {

    private final TopicRepository topics;

    public TopicService(TopicRepository topics) {
        this.topics = topics;
    }

    public List<TopicDto> getVisibleTopics() {
        return topics.findVisibleTopics(TopicStatus.HIDDEN).stream()
                .map(TopicService::toDto)
                .toList();
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
