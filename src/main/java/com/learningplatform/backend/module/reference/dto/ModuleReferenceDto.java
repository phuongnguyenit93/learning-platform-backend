package com.learningplatform.backend.module.reference.dto;

import java.util.List;

/** Localized curated references for one Module. */
public record ModuleReferenceDto(
        String slug,
        String languageCode,
        List<ReferenceItemDto> references) {
}
