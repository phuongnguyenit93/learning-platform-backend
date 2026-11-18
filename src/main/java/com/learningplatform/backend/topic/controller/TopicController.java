package com.learningplatform.backend.topic.controller;

import java.util.List;

import com.learningplatform.backend.topic.dto.TopicDto;
import com.learningplatform.backend.topic.service.TopicService;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Profile("oracle")
@RequestMapping("/api/v1/topics")
@CrossOrigin(origins = {
        "https://nguyendodinhphuong.name.vn",
        "https://learning-platform.phuongnguyenit93.workers.dev",
        "http://localhost:5173"
})
public class TopicController {

    private final TopicService topics;

    public TopicController(TopicService topics) {
        this.topics = topics;
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public List<TopicDto> getTopics() {
        return topics.getVisibleTopics();
    }
}
