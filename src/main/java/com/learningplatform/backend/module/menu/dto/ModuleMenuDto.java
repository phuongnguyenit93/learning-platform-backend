package com.learningplatform.backend.module.menu.dto;

import java.util.List;

/** Lightweight two-level Knowledge navigation tree for one module/locale. */
public record ModuleMenuDto(
        String slug,
        String languageCode,
        List<ModuleMenuChapterDto> chapters) {
}
