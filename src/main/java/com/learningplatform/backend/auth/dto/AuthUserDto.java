package com.learningplatform.backend.auth.dto;

import java.util.List;

public record AuthUserDto(
        Long id,
        String email,
        String displayName,
        String avatarUrl,
        List<String> roles) {
}
