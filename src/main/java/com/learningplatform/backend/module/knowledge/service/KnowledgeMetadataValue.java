package com.learningplatform.backend.module.knowledge.service;

/** Optional metadata is always JSON null when absent or whitespace-only. */
final class KnowledgeMetadataValue {

    private KnowledgeMetadataValue() {
    }

    static String optional(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
