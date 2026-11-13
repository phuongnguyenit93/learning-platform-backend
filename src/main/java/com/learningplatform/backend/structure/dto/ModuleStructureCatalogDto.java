package com.learningplatform.backend.structure.dto;

/** Same navigation envelope as learning-platform's ModuleCatalog (version + root). */
public record ModuleStructureCatalogDto(int version, ModuleStructureNodeDto root) {
}
