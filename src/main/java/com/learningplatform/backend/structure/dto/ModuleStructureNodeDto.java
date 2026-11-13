package com.learningplatform.backend.structure.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.learningplatform.backend.structure.model.StructureType;

/**
 * A tree node compatible with learning-platform's ModuleCatalogNode.
 * Database identity and natural keys are also exposed for future API consumers.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ModuleStructureNodeDto(
        Long idStructure,
        String id,
        String name,
        StructureType kind,
        String routeId,
        String path,
        String nodeKey,
        String parentNodeKey,
        String slug,
        String description,
        Integer sortOrder,
        List<ModuleStructureNodeDto> children) {
}
