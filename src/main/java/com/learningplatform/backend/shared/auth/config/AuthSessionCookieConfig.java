package com.learningplatform.backend.shared.auth.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.session.web.http.CookieSerializer;
import org.springframework.session.web.http.DefaultCookieSerializer;

@Configuration
@Profile("oracle")
@ConditionalOnProperty(name = "app.auth.google.enabled", havingValue = "true")
public class AuthSessionCookieConfig {

    @Bean
    CookieSerializer sessionCookieSerializer() {
        var cookies = new DefaultCookieSerializer();
        cookies.setCookieName("SESSION");
        cookies.setCookiePath("/");
        cookies.setUseHttpOnlyCookie(true);
        cookies.setUseSecureCookie(true);
        cookies.setSameSite("Lax");
        // Persistent login across browser restarts; Oracle enforces idle timeout.
        cookies.setCookieMaxAge(90 * 24 * 60 * 60);
        return cookies;
    }
}
