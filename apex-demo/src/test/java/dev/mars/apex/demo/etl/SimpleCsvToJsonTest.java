package dev.mars.apex.demo.etl;

import dev.mars.apex.core.config.yaml.YamlConfigurationLoader;
import dev.mars.apex.core.config.yaml.YamlRuleConfiguration;
import dev.mars.apex.core.engine.config.RulesEngine;
import dev.mars.apex.core.engine.model.RuleResult;
import dev.mars.apex.demo.DemoTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Simple CSV to JSON ETL Pipeline Test
 * Tests processing 1000 customer records from CSV to JSON
 */
@DisplayName("Simple CSV to JSON Pipeline Test")
class SimpleCsvToJsonTest extends DemoTestBase {

    private static final Logger logger = LoggerFactory.getLogger(SimpleCsvToJsonTest.class);

    private RulesEngine rulesEngine;
    private YamlConfigurationLoader yamlLoader;

    @BeforeEach
    public void setUp() {
        super.setUp();
        logger.info("Setting up Simple CSV to JSON Test...");
        yamlLoader = new YamlConfigurationLoader();

        try {
            // Create output directory for JSON files
            Path outputDir = Paths.get("./demo-data/json");
            Files.createDirectories(outputDir);

        } catch (IOException e) {
            throw new RuntimeException("Failed to setup test data", e);
        }

        logger.info("✓ Simple CSV to JSON Test setup completed");
    }

    @AfterEach
    public void tearDown() {
        // Close rules engine if it was created
        if (rulesEngine != null) {
            try {
                rulesEngine.shutdown();
                logger.info("Rules engine shut down successfully");
            } catch (Exception e) {
                logger.warn("Error shutting down rules engine", e);
            }
        }
        super.tearDown();
    }

    @Test
    @DisplayName("Should process 1000 CSV records to JSON")
    void shouldProcessCsvToJson() throws Exception {
        logger.info("=== Testing Simple CSV to JSON Pipeline (1000 records) ===");

        // Create RulesEngine and execute pipeline
        rulesEngine = RulesEngine.fromFile(
            "src/test/java/dev/mars/apex/demo/etl/SimpleCsvToJsonTest.yaml");

        long startTime = System.currentTimeMillis();
        java.util.Map<String, Object> inputData = new java.util.HashMap<>();
        RuleResult result = rulesEngine.evaluate(inputData);
        long executionTime = System.currentTimeMillis() - startTime;

        // Validate pipeline execution
        assertNotNull(result, "Pipeline result should not be null");
        assertEquals(RuleResult.ResultType.MATCH, result.getResultType(),
            "Pipeline should execute successfully");
        assertTrue(result.getMessage().contains("Sequential evaluation completed successfully"),
            "Result message should indicate successful evaluation");

        // Validate output file
        Path outputFile = Paths.get("./demo-data/json/customers-1000.json");
        assertTrue(Files.exists(outputFile), "JSON output file should exist");

        // Validate file content
        String jsonContent = Files.readString(outputFile);
        assertTrue(jsonContent.startsWith("["), "JSON should start with array");
        assertTrue(jsonContent.endsWith("]"), "JSON should end with array");
        assertTrue(jsonContent.contains("Customer-1"), "Should contain first customer");
        assertTrue(jsonContent.contains("Customer-1000"), "Should contain last customer");

        logger.info("✓ Pipeline completed successfully");
        logger.info("  - Result type: {}", result.getResultType());
        logger.info("  - Output file: {}", outputFile.toAbsolutePath());
        logger.info("  - Execution time: {}ms", executionTime);
        logger.info("  - File size: {} bytes", Files.size(outputFile));
    }


}
