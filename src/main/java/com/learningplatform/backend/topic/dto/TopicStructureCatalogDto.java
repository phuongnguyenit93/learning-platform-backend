package com.learningplatform.backend.topic.dto;

/** Versioned navigation tree for one Topic. */
public record TopicStructureCatalogDto(
        int version,
        TopicStructureNodeDto root) {
}
