package com.learningplatform.backend.reference.dto;

import java.util.List;

/** References are fetched only when the module's Reference tab is requested. */
public record ModuleReferenceDto(
        String slug,
        String languageCode,
        List<ReferenceItemDto> references) {
}
