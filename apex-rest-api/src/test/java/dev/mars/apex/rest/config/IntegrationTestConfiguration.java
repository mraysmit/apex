package dev.mars.apex.rest.config;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.TestConfiguration;

import jakarta.annotation.PostConstruct;

/**
 * Test configuration to resolve Spring Boot 3.4.8 compatibility issues.
 * Excludes problematic auto-configurations for testing and sets up test-aware logging.
 */
@TestConfiguration
@EnableAutoConfiguration(exclude = {
    org.springframework.boot.actuate.autoconfigure.metrics.task.TaskExecutorMetricsAutoConfiguration.class
})
public class IntegrationTestConfiguration {

    /**
     * Set system property to enable test-aware logging in core module.
     */
    @PostConstruct
    public void setupTestEnvironment() {
        System.setProperty("test.environment", "true");
    }
}
