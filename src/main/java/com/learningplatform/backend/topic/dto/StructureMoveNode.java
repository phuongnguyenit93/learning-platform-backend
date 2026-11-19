package com.learningplatform.backend.topic.dto;

import java.util.List;

public record StructureMoveNode(String kind, Long id, List<StructureMoveNode> children) {
}
