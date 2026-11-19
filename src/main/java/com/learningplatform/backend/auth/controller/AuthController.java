package com.learningplatform.backend.auth.controller;

import java.util.Map;

import com.learningplatform.backend.auth.dto.AuthUserDto;
import com.learningplatform.backend.auth.service.AccountService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final ObjectProvider<AccountService> accounts;
    private final boolean googleEnabled;

    public AuthController(ObjectProvider<AccountService> accounts,
            @Value("${app.auth.google.enabled:false}") boolean googleEnabled) {
        this.accounts = accounts;
        this.googleEnabled = googleEnabled;
    }

    @GetMapping("/config")
    public Map<String, Boolean> config() {
        return Map.of("googleLoginEnabled", googleEnabled);
    }

    @GetMapping("/me")
    public ResponseEntity<AuthUserDto> me(@AuthenticationPrincipal OidcUser user,
            HttpServletRequest request) {
        if (user == null || accounts.getIfAvailable() == null) {
            return ResponseEntity.status(401).cacheControl(CacheControl.noStore()).build();
        }
        return accounts.getObject().findByGoogleSub(user.getSubject())
                .map(account -> ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(account))
                .orElseGet(() -> {
                    HttpSession session = request.getSession(false);
                    if (session != null) {
                        session.invalidate();
                    }
                    return ResponseEntity.status(401).cacheControl(CacheControl.noStore()).build();
                });
    }

    @GetMapping("/csrf")
    public ResponseEntity<Map<String, String>> csrf(HttpServletRequest request) {
        var token = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
        if (token == null) {
            return ResponseEntity.status(503).build();
        }
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(Map.of("token", token.getToken(), "headerName", token.getHeaderName()));
    }
}
