package com.learningplatform.backend.shared.auth.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.learningplatform.backend.shared.auth.dto.AdminUserDto;
import com.learningplatform.backend.shared.auth.dto.AdminUsersPageDto;
import com.learningplatform.backend.shared.auth.dto.AuthUserDto;
import com.learningplatform.backend.shared.auth.model.AppUser;
import com.learningplatform.backend.shared.auth.model.AppUserRole;
import com.learningplatform.backend.shared.auth.model.AppUserRoleId;
import com.learningplatform.backend.shared.auth.model.AppUserStatus;
import com.learningplatform.backend.shared.auth.repository.AppRoleRepository;
import com.learningplatform.backend.shared.auth.repository.AppUserRepository;
import com.learningplatform.backend.shared.auth.repository.AppUserRoleRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Profile("oracle")
public class AccountService {

    private final AppUserRepository users;
    private final AppRoleRepository roles;
    private final AppUserRoleRepository userRoles;

    public AccountService(AppUserRepository users, AppRoleRepository roles, AppUserRoleRepository userRoles) {
        this.users = users;
        this.roles = roles;
        this.userRoles = userRoles;
    }

    @Transactional
    public void recordGoogleLogin(OidcUser googleUser) {
        // Google subject is stable. The email address is not an account identity.
        String subject = googleUser.getSubject();
        if (subject == null || subject.isBlank()) {
            throw new IllegalArgumentException("Missing Google subject");
        }

        AppUser account = users.findByGoogleSub(subject).orElseGet(() -> new AppUser(subject));
        if (account.getStatus() != AppUserStatus.ACTIVE) {
            throw new IllegalStateException("The user account is disabled");
        }
        account.recordLogin(googleUser.getEmail(), Boolean.TRUE.equals(googleUser.getEmailVerified()),
                googleUser.getFullName(), googleUser.getPicture());
        account = users.saveAndFlush(account);

        // Link USER only if missing. Existing ADMIN roles remain untouched.
        var defaultRole = roles.findByCode("USER")
                .orElseThrow(() -> new IllegalStateException("Missing default USER role"));
        var roleId = new AppUserRoleId(account.getId(), defaultRole.getId());
        if (!userRoles.existsById(roleId)) {
            userRoles.save(new AppUserRole(account, defaultRole));
        }
    }

    @Transactional(readOnly = true)
    public Optional<AuthUserDto> findByGoogleSub(String googleSub) {
        return users.findByGoogleSubAndStatus(googleSub, AppUserStatus.ACTIVE)
                .map(account -> new AuthUserDto(account.getId(), account.getEmail(),
                        account.getDisplayName(), account.getAvatarUrl(),
                        roleCodesByUser(List.of(account.getId())).getOrDefault(account.getId(), List.of())));
    }

    @Transactional(readOnly = true)
    public boolean hasRole(String googleSub, String role) {
        return userRoles.existsByUser_GoogleSubAndUser_StatusAndRole_Code(
                googleSub, AppUserStatus.ACTIVE, role);
    }

    @Transactional(readOnly = true)
    public AdminUsersPageDto listUsers(int page, int size) {
        var result = users.findAll(PageRequest.of(page, size, Sort.by("id").ascending()));
        var userIds = result.getContent().stream().map(AppUser::getId).toList();
        var rolesByUser = roleCodesByUser(userIds);
        var items = result.getContent().stream()
                .map(user -> new AdminUserDto(user.getId(), user.getEmail(),
                        user.getDisplayName(), user.getStatus().name(),
                        rolesByUser.getOrDefault(user.getId(), List.of())))
                .toList();
        return new AdminUsersPageDto(items, page, size, result.getTotalElements());
    }

    private Map<Long, List<String>> roleCodesByUser(List<Long> userIds) {
        if (userIds.isEmpty()) {
            return Map.of();
        }
        var codes = new HashMap<Long, List<String>>();
        for (AppUserRole link : userRoles.findByUser_IdIn(userIds)) {
            codes.computeIfAbsent(link.getUser().getId(), id -> new ArrayList<>())
                    .add(link.getRole().getCode());
        }
        codes.values().forEach(list -> list.sort(String::compareTo));
        return codes;
    }
}
