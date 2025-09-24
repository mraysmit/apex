package dev.mars.apex.rest.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Bean;

/**
 * Minimal Spring Boot test configuration for integration tests.
 * Provides required beans to bootstrap the test context.
 */
@TestConfiguration
public class IntegrationTestConfiguration {

    @Bean
    public TestRestTemplate testRestTemplate() {
        return new TestRestTemplate();
    }
}
