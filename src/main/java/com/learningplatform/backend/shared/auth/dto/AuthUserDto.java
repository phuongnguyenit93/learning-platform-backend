package com.learningplatform.backend.shared.auth.dto;

import java.util.List;

public record AuthUserDto(
        Long id,
        String email,
        String displayName,
        String avatarUrl,
        List<String> roles) {
}
