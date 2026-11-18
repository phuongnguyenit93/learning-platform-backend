package com.learningplatform.backend.module.core.dto;

/** Public core metadata for one visible learning Module. */
public record ModuleCoreDto(
        Long id,
        String serviceName,
        String slug,
        String name,
        String description,
        String specialNote) {
}
