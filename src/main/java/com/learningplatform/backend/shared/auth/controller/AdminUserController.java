package com.learningplatform.backend.shared.auth.controller;

import com.learningplatform.backend.shared.auth.dto.AdminUsersPageDto;
import com.learningplatform.backend.shared.auth.service.AccountService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

@RestController
@Profile("oracle")
@ConditionalOnProperty(name = "app.auth.google.enabled", havingValue = "true")
@RequestMapping("/api/admin/users")
public class AdminUserController {

    private final AccountService accounts;

    public AdminUserController(AccountService accounts) {
        this.accounts = accounts;
    }

    @GetMapping
    public AdminUsersPageDto listUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        if (page < 0 || page > 100000 || size < 1 || size > 50) {
            throw new ResponseStatusException(BAD_REQUEST, "Invalid pagination");
        }
        return accounts.listUsers(page, size);
    }
}
