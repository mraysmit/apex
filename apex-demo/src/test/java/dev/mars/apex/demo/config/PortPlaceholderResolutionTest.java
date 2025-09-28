/*
 * Copyright 2024 APEX Process Consultants. This code is confidential and proprietary.
 * Unauthorized copying, distribution, or use is strictly prohibited.
 */
package dev.mars.apex.demo.config;

import dev.mars.apex.core.config.yaml.YamlConfigurationLoader;
import dev.mars.apex.core.config.yaml.YamlRuleConfiguration;
import dev.mars.apex.core.config.yaml.YamlDataSource;
import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.*;

/**
 * PORT Placeholder Resolution Test
 * 
 * This test specifically verifies that the YAML property resolution system
 * correctly resolves ${PORT} placeholders using system properties.
 * 
 * This demonstrates the proper way to handle dynamic configuration values
 * in APEX YAML files without creating temporary files.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("PORT Placeholder Resolution Test")
class PortPlaceholderResolutionTest {

    private static final Logger logger = LoggerFactory.getLogger(PortPlaceholderResolutionTest.class);
    
    private YamlConfigurationLoader yamlLoader;
    private static final String TEST_PORT = "12345";
    private static final String YAML_FILE_PATH = "src/test/java/dev/mars/apex/demo/config/PortPlaceholderResolutionTest.yaml";

    @BeforeEach
    void setUp() {
        logger.info("=== Setting up PORT Placeholder Resolution Test ===");
        yamlLoader = new YamlConfigurationLoader();
    }

    @AfterEach
    void tearDown() {
        // Clean up system property after each test
        System.clearProperty("PORT");
        logger.info("=== Cleaned up PORT system property ===");
    }

    @Test
    @Order(1)
    @DisplayName("Should resolve PORT placeholder when system property is set")
    void testPortPlaceholderResolution() {
        logger.info("=== Testing PORT Placeholder Resolution ===");

        try {
            // Set PORT system property BEFORE loading YAML
            System.setProperty("PORT", TEST_PORT);
            logger.info("Set PORT system property to: {}", TEST_PORT);

            // Load YAML configuration - should resolve ${PORT} placeholder
            YamlRuleConfiguration config = yamlLoader.loadFromFile(YAML_FILE_PATH);
            assertNotNull(config, "YAML configuration should not be null");
            
            // Verify configuration loaded successfully
            assertEquals("PORT Placeholder Resolution Test", config.getMetadata().getName());
            logger.info("✓ YAML configuration loaded successfully");

            // Verify data source was loaded
            assertNotNull(config.getDataSources(), "Data sources should not be null");
            assertEquals(1, config.getDataSources().size(), "Should have exactly 1 data source");
            
            YamlDataSource dataSource = config.getDataSources().get(0);
            assertEquals("test-api", dataSource.getName());
            logger.info("✓ Data source loaded: {}", dataSource.getName());

            // Verify PORT placeholder was resolved in base-url
            assertNotNull(dataSource.getConnection(), "Connection configuration should not be null");
            String baseUrl = (String) dataSource.getConnection().get("base-url");
            assertNotNull(baseUrl, "Base URL should not be null");
            
            String expectedUrl = "http://localhost:" + TEST_PORT;
            assertEquals(expectedUrl, baseUrl, "PORT placeholder should be resolved to actual port");
            logger.info("✓ PORT placeholder resolved correctly: {} -> {}", "${PORT}", TEST_PORT);
            logger.info("✓ Final base URL: {}", baseUrl);

            // Verify enrichment was loaded
            assertNotNull(config.getEnrichments(), "Enrichments should not be null");
            assertEquals(1, config.getEnrichments().size(), "Should have exactly 1 enrichment");
            logger.info("✓ Enrichment loaded successfully");

            logger.info("🎉 PORT placeholder resolution test PASSED");

        } catch (Exception e) {
            logger.error("❌ PORT placeholder resolution test FAILED: {}", e.getMessage(), e);
            fail("PORT placeholder resolution failed: " + e.getMessage());
        }
    }

    @Test
    @Order(2)
    @DisplayName("Should fail when PORT system property is not set")
    void testPortPlaceholderResolutionFailure() {
        logger.info("=== Testing PORT Placeholder Resolution Failure ===");

        try {
            // Do NOT set PORT system property - should cause failure
            logger.info("PORT system property not set - expecting failure");

            // Attempt to load YAML configuration - should fail with property not found
            Exception exception = assertThrows(Exception.class, () -> {
                yamlLoader.loadFromFile(YAML_FILE_PATH);
            });

            // Verify the exception message indicates PORT property not found
            String errorMessage = exception.getMessage();
            assertTrue(errorMessage.contains("Property not found: PORT"), 
                      "Exception should indicate PORT property not found, but was: " + errorMessage);
            
            logger.info("✓ Expected failure occurred: {}", errorMessage);
            logger.info("🎉 PORT placeholder resolution failure test PASSED");

        } catch (AssertionError e) {
            logger.error("❌ PORT placeholder resolution failure test FAILED: {}", e.getMessage());
            throw e;
        }
    }

    @Test
    @Order(3)
    @DisplayName("Should resolve PORT placeholder with different port values")
    void testPortPlaceholderResolutionWithDifferentPorts() {
        logger.info("=== Testing PORT Placeholder Resolution with Different Ports ===");

        String[] testPorts = {"8080", "9090", "51234", "12345"};

        for (String port : testPorts) {
            try {
                logger.info("Testing with PORT = {}", port);
                
                // Set PORT system property
                System.setProperty("PORT", port);

                // Load YAML configuration
                YamlRuleConfiguration config = yamlLoader.loadFromFile(YAML_FILE_PATH);
                assertNotNull(config, "YAML configuration should not be null");

                // Verify PORT placeholder was resolved correctly
                YamlDataSource dataSource = config.getDataSources().get(0);
                String baseUrl = (String) dataSource.getConnection().get("base-url");
                String expectedUrl = "http://localhost:" + port;
                
                assertEquals(expectedUrl, baseUrl, "PORT should resolve to " + port);
                logger.info("✓ PORT {} resolved correctly to: {}", port, baseUrl);

            } catch (Exception e) {
                logger.error("❌ Failed to resolve PORT {}: {}", port, e.getMessage());
                fail("Failed to resolve PORT " + port + ": " + e.getMessage());
            }
        }

        logger.info("🎉 PORT placeholder resolution with different ports test PASSED");
    }
}
