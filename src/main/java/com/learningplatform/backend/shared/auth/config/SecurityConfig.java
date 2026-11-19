package com.learningplatform.backend.shared.auth.config;

import java.util.List;

import com.learningplatform.backend.shared.auth.service.AccountService;
import com.learningplatform.backend.shared.auth.service.GoogleOidcUserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
public class SecurityConfig {

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        var cors = new CorsConfiguration();
        cors.setAllowedOrigins(List.of(
                "https://nguyendodinhphuong.name.vn",
                "https://learning-platform.phuongnguyenit93.workers.dev",
                "http://localhost:5173"));
        cors.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        cors.setAllowedHeaders(List.of("Content-Type", "Accept", "X-CSRF-TOKEN", "X-Requested-With"));
        cors.setAllowCredentials(true);
        var source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", cors);
        return source;
    }

    @Bean
    @ConditionalOnProperty(name = "app.auth.google.enabled", havingValue = "false", matchIfMissing = true)
    SecurityFilterChain publicSecurityFilterChain(HttpSecurity http) throws Exception {
        // Until OAuth credentials are provisioned, preserve existing public endpoints.
        return http
                .cors(Customizer.withDefaults())
                .csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                // Admin APIs must remain protected even when Google OAuth is disabled.
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/api/admin", "/api/admin/**").denyAll()
                        .anyRequest().permitAll())
                .build();
    }

    @Bean
    @Profile("oracle")
    @ConditionalOnProperty(name = "app.auth.google.enabled", havingValue = "true")
    SecurityFilterChain googleSecurityFilterChain(
            HttpSecurity http,
            AccountService accounts,
            GoogleOidcUserService oidcUsers,
            ClientRegistrationRepository clientRegistrations,
            @Value("${app.auth.frontend-url}") String frontendUrl) throws Exception {
        var authorizationRequestResolver = new DefaultOAuth2AuthorizationRequestResolver(
                clientRegistrations, "/oauth2/authorization");
        authorizationRequestResolver.setAuthorizationRequestCustomizer(
                builder -> builder.additionalParameters(params -> params.put("prompt", "select_account")));

        return http
                .cors(Customizer.withDefaults())
                // CSRF remains enabled for cookie-authenticated mutations (including logout).
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .exceptionHandling(errors -> errors.defaultAuthenticationEntryPointFor(
                        new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED),
                        request -> request.getRequestURI().equals("/api/admin")
                                || request.getRequestURI().startsWith("/api/admin/")))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/api/admin", "/api/admin/**").access((authentication, context) -> {
                            var current = authentication.get();
                            boolean allowed = current != null && current.isAuthenticated()
                                    && current.getPrincipal() instanceof OidcUser oidc
                                    && accounts.hasRole(oidc.getSubject(), "ADMIN");
                            return new AuthorizationDecision(allowed);
                        })
                        .anyRequest().permitAll())
                .oauth2Login(oauth -> oauth
                        .authorizationEndpoint(endpoint ->
                                endpoint.authorizationRequestResolver(authorizationRequestResolver))
                        .userInfoEndpoint(userInfo -> userInfo.oidcUserService(oidcUsers))
                        .successHandler((request, response, authentication) ->
                                response.sendRedirect(frontendUrl))
                        .failureHandler((request, response, exception) ->
                                response.sendRedirect(frontendUrl + "/?auth=error")))
                .logout(logout -> logout
                        .logoutUrl("/api/auth/logout")
                        .invalidateHttpSession(true)
                        .clearAuthentication(true)
                        .deleteCookies("SESSION")
                        .logoutSuccessHandler((request, response, authentication) ->
                                response.setStatus(204)))
                .build();
    }
}
