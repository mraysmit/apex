package dev.mars.apex.demo.etl;

import dev.mars.apex.core.config.yaml.YamlConfigurationLoader;
import dev.mars.apex.core.config.yaml.YamlRuleConfiguration;
import dev.mars.apex.core.engine.pipeline.DataPipelineEngine;
import dev.mars.apex.core.engine.pipeline.YamlPipelineExecutionResult;
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
    
    private DataPipelineEngine pipelineEngine;
    private YamlConfigurationLoader yamlLoader;

    @BeforeEach
    public void setUp() {
        super.setUp();
        logger.info("Setting up Simple CSV to JSON Test...");
        pipelineEngine = new DataPipelineEngine();
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
        // Close pipeline engine if it was created
        if (pipelineEngine != null) {
            try {
                pipelineEngine.shutdown();
                logger.info("Pipeline engine shut down successfully");
            } catch (Exception e) {
                logger.warn("Error shutting down pipeline engine", e);
            }
        }
        super.tearDown();
    }

    @Test
    @DisplayName("Should process 1000 CSV records to JSON")
    void shouldProcessCsvToJson() throws Exception {
        logger.info("=== Testing Simple CSV to JSON Pipeline (1000 records) ===");

        // Load and execute pipeline
        YamlRuleConfiguration config = yamlLoader.loadFromFile(
            "src/test/java/dev/mars/apex/demo/etl/SimpleCsvToJsonTest.yaml");
        
        pipelineEngine.initialize(config);
        long startTime = System.currentTimeMillis();
        YamlPipelineExecutionResult result = pipelineEngine.executePipeline("simple-csv-to-json");
        long executionTime = System.currentTimeMillis() - startTime;

        // Validate pipeline execution
        assertNotNull(result, "Pipeline result should not be null");
        assertTrue(result.isSuccess(), "Pipeline should execute successfully");
        assertEquals(2, result.getStepResults().size(), "Should have 2 steps");

        // Validate steps
        var extractResult = result.getStepResults().get(0);
        assertEquals("extract-csv", extractResult.getStepName());
        assertTrue(extractResult.isSuccess(), "Extract step should succeed");

        var loadResult = result.getStepResults().get(1);
        assertEquals("load-json", loadResult.getStepName());
        assertTrue(loadResult.isSuccess(), "Load step should succeed");

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
        logger.info("  - Extract step: {} (success: {})", extractResult.getStepName(), extractResult.isSuccess());
        logger.info("  - Load step: {} (success: {})", loadResult.getStepName(), loadResult.isSuccess());
        logger.info("  - Output file: {}", outputFile.toAbsolutePath());
        logger.info("  - Execution time: {}ms", executionTime);
        logger.info("  - File size: {} bytes", Files.size(outputFile));
    }


}
