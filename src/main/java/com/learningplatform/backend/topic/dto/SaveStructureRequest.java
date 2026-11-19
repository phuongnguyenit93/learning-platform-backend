package com.learningplatform.backend.topic.dto;

import java.util.List;

public record SaveStructureRequest(String revision, List<StructureMoveNode> nodes) {
}
