package com.learningplatform.backend.health;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.Banner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.ConfigurableApplicationContext;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DockerSecretsConfigTest {

    @Test
    void bindsMountedSecretFilenamesToDatasourceProperties(@TempDir Path directory) throws Exception {
        Files.writeString(directory.resolve("spring.datasource.url"), "jdbc:oracle:thin:@example");
        Files.writeString(directory.resolve("spring.datasource.username"), "READER");
        Files.writeString(directory.resolve("spring.datasource.password"), "test-only-password");

        SpringApplication application = new SpringApplication(EmptyConfiguration.class);
        application.setWebApplicationType(WebApplicationType.NONE);
        application.setBannerMode(Banner.Mode.OFF);
        application.setLogStartupInfo(false);

        String location = directory.toAbsolutePath().toString().replace('\\', '/');
        try (ConfigurableApplicationContext context = application.run(
                "--spring.config.import=configtree:" + location + "/")) {
            assertEquals("jdbc:oracle:thin:@example",
                    context.getEnvironment().getProperty("spring.datasource.url"));
            assertEquals("READER",
                    context.getEnvironment().getProperty("spring.datasource.username"));
            assertEquals("test-only-password",
                    context.getEnvironment().getProperty("spring.datasource.password"));
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class EmptyConfiguration {
    }
}
