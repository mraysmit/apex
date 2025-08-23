package dev.mars.apex.playground;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

/**
 * Basic integration test for APEX Playground application.
 * 
 * Verifies that the Spring Boot application context loads successfully
 * and all components are properly configured.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-08-23
 * @version 1.0
 */
@SpringBootTest
@TestPropertySource(properties = {
    "spring.main.web-application-type=servlet",
    "server.port=0"  // Use random port for testing
})
class PlaygroundApplicationTest {

    /**
     * Test that the Spring Boot application context loads successfully.
     */
    @Test
    void contextLoads() {
        // This test will pass if the application context loads without errors
        // It verifies that all beans are properly configured and dependencies are resolved
    }
}
