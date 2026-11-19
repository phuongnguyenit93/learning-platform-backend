package com.learningplatform.backend.auth.service;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;

@Service
@Profile("oracle")
@ConditionalOnProperty(name = "app.auth.google.enabled", havingValue = "true")
public class GoogleOidcUserService implements OAuth2UserService<OidcUserRequest, OidcUser> {

    private final OidcUserService google = new OidcUserService();
    private final AccountService accounts;

    public GoogleOidcUserService(AccountService accounts) {
        this.accounts = accounts;
    }

    @Override
    public OidcUser loadUser(OidcUserRequest request) throws OAuth2AuthenticationException {
        OidcUser user = google.loadUser(request);
        if (!"google".equals(request.getClientRegistration().getRegistrationId())) {
            throw new OAuth2AuthenticationException(new OAuth2Error("unsupported_provider"));
        }
        try {
            accounts.recordGoogleLogin(user);
        } catch (RuntimeException error) {
            // Never send database errors or account status to the OAuth redirect URL.
            throw new OAuth2AuthenticationException(new OAuth2Error("login_rejected"), error);
        }
        return user;
    }
}
