package com.learningplatform.backend.shared.auth.dto;

import java.util.List;

public record AdminUsersPageDto(
        List<AdminUserDto> items,
        int page,
        int size,
        long total) {
}
