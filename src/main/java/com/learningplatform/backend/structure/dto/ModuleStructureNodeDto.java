package com.learningplatform.backend.structure.dto;

import java.util.List;

import com.learningplatform.backend.structure.model.StructureType;

/** A tree node returned by the read-only Topic structure API. */
public record ModuleStructureNodeDto(
        Long idStructure,
        String nodeKey,
        String parentNodeKey,
        StructureType type,
        String slug,
        String name,
        String description,
        Integer sortOrder,
        List<ModuleStructureNodeDto> children) {
}
