package com.learningplatform.backend.auth.dto;

import java.util.List;

public record AdminUsersPageDto(
        List<AdminUserDto> items,
        int page,
        int size,
        long total) {
}
