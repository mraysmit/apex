package dev.mars.rulesengine.rest.config;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.TestConfiguration;

/**
 * Test configuration to resolve Spring Boot 3.4.8 compatibility issues.
 * Excludes problematic auto-configurations for testing.
 */
@TestConfiguration
@EnableAutoConfiguration(exclude = {
    org.springframework.boot.actuate.autoconfigure.metrics.task.TaskExecutorMetricsAutoConfiguration.class
})
public class IntegrationTestConfiguration {
    // Empty configuration class - just excludes problematic auto-configurations
}
