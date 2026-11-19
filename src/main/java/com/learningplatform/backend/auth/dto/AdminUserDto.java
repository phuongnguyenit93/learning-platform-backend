package com.learningplatform.backend.auth.dto;

import java.util.List;

public record AdminUserDto(
        Long id,
        String email,
        String displayName,
        String status,
        List<String> roles) {
}
