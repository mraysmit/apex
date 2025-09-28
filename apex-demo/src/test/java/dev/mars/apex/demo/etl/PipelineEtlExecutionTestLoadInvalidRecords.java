package dev.mars.apex.demo.etl;

import dev.mars.apex.core.config.yaml.YamlConfigurationLoader;
import dev.mars.apex.core.config.yaml.YamlRuleConfiguration;
import dev.mars.apex.core.engine.pipeline.DataPipelineEngine;
import dev.mars.apex.core.engine.pipeline.YamlPipelineExecutionResult;
import dev.mars.apex.demo.DemoTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for PipelineEtlExecutionTestLoadInvalidRecords.yaml
 * Tests handling of invalid records during load operations
 */
@DisplayName("Invalid Records Load Pipeline Test")
class PipelineEtlExecutionTestLoadInvalidRecords extends DemoTestBase {

    private static final Logger logger = LoggerFactory.getLogger(PipelineEtlExecutionTestLoadInvalidRecords.class);
    
    private DataPipelineEngine pipelineEngine;
    private YamlConfigurationLoader yamlLoader;

    @BeforeEach
    public void setUp() {
        super.setUp();
        logger.info("Setting up Invalid Records Load Pipeline Test...");
        pipelineEngine = new DataPipelineEngine();
        yamlLoader = new YamlConfigurationLoader();

        try {
            // Ensure database output directory exists
            Path dbDir = Paths.get("./target/test/etl/output/database");
            Files.createDirectories(dbDir);

            // Create test data with invalid records
            createInvalidRecordsTestData();
        } catch (IOException e) {
            throw new RuntimeException("Failed to setup test data", e);
        }

        logger.info("✓ Invalid Records Load Pipeline Test setup completed");
    }

    @Test
    @DisplayName("Should handle invalid records gracefully")
    void shouldHandleInvalidRecordsGracefully() throws Exception {
        logger.info("=== Testing Invalid Records Load Pipeline ===");

        // Load the YAML configuration
        YamlRuleConfiguration config = yamlLoader.loadFromFile(
            "src/test/java/dev/mars/apex/demo/etl/PipelineEtlExecutionTestLoadInvalidRecords.yaml");
        
        // Initialize and execute pipeline
        pipelineEngine.initialize(config);
        YamlPipelineExecutionResult result = pipelineEngine.executePipeline("load-invalid-records-pipeline");

        // Validate results
        assertNotNull(result, "Pipeline execution result should not be null");
        assertTrue(result.isSuccess(), "Pipeline should execute successfully even with invalid records");
        assertEquals(2, result.getStepResults().size(), "Should have 2 step results (extract + load)");

        // Validate extract step
        var extractResult = result.getStepResults().get(0);
        assertEquals("extract-mixed-data", extractResult.getStepName());
        assertTrue(extractResult.isSuccess(), "Extract step should succeed");
        
        // Validate load step
        var loadResult = result.getStepResults().get(1);
        assertEquals("load-with-validation", loadResult.getStepName());
        assertTrue(loadResult.isSuccess(), "Load step should succeed despite invalid records");

        logger.info("✓ Invalid records load pipeline test completed successfully");
        logger.info("  - Extract step: {} (success: {})", extractResult.getStepName(), extractResult.isSuccess());
        logger.info("  - Load step: {} (success: {})", loadResult.getStepName(), loadResult.isSuccess());
        logger.info("  - Total execution time: {}ms", result.getDurationMs());
    }

    private void createInvalidRecordsTestData() throws IOException {
        Path inputDir = Paths.get("./target/test/etl/data/input");
        Files.createDirectories(inputDir);
        
        Path invalidCsvFile = inputDir.resolve("invalid-customers.csv");
        String csvContent = """
            id,name,email,status
            1,John Doe,john@example.com,ACTIVE
            2,,jane@example.com,ACTIVE
            3,Bob Johnson,,ACTIVE
            4,Alice Smith,alice@example.com,ACTIVE
            5,Invalid User,not-an-email,INVALID
            """;
        Files.writeString(invalidCsvFile, csvContent);
        
        logger.info("✓ Invalid records dataset created with mixed valid/invalid records");
    }
}
