package com.learningplatform.backend.module.knowledge.dto;

/** Internal scalar projection without Markdown and Summary CLOBs. */
public record KnowledgeInsightMetadataDto(
        Long knowledgeId,
        String focus,
        String problemSolved,
        String mechanism,
        String applicationValue) {
}
