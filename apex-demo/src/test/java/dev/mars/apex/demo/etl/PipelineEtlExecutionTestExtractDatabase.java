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
 * Test class for PipelineEtlExecutionTestExtractDatabase.yaml
 * Tests database extraction functionality
 */
@DisplayName("Database Extract Pipeline Test")
class PipelineEtlExecutionTestExtractDatabase extends DemoTestBase {

    private static final Logger logger = LoggerFactory.getLogger(PipelineEtlExecutionTestExtractDatabase.class);
    
    private DataPipelineEngine pipelineEngine;
    private YamlConfigurationLoader yamlLoader;

    @BeforeEach
    public void setUp() {
        super.setUp();
        logger.info("Setting up Database Extract Pipeline Test...");
        pipelineEngine = new DataPipelineEngine();
        yamlLoader = new YamlConfigurationLoader();

        try {
            // Ensure database directory exists
            Path dbDir = Paths.get("./target/test/etl/data/database");
            Files.createDirectories(dbDir);
        } catch (IOException e) {
            throw new RuntimeException("Failed to create database directory", e);
        }

        logger.info("✓ Database Extract Pipeline Test setup completed");
    }

    @Test
    @DisplayName("Should extract data from H2 database")
    void shouldExtractDataFromH2Database() throws Exception {
        logger.info("=== Testing Database Extract Pipeline ===");

        // Load the YAML configuration
        YamlRuleConfiguration config = yamlLoader.loadFromFile(
            "src/test/java/dev/mars/apex/demo/etl/PipelineEtlExecutionTestExtractDatabase.yaml");
        
        // Initialize and execute pipeline
        pipelineEngine.initialize(config);
        YamlPipelineExecutionResult result = pipelineEngine.executePipeline("database-extract-pipeline");

        // Validate results
        assertNotNull(result, "Pipeline execution result should not be null");
        assertTrue(result.isSuccess(), "Pipeline should execute successfully");
        assertEquals(1, result.getStepResults().size(), "Should have 1 step result (extract only)");

        // Validate extract step
        var extractResult = result.getStepResults().get(0);
        assertEquals("extract-customers", extractResult.getStepName());
        assertTrue(extractResult.isSuccess(), "Extract step should succeed");

        logger.info("✓ Database extract pipeline test completed successfully");
        logger.info("  - Extract step: {} (success: {})", extractResult.getStepName(), extractResult.isSuccess());
        logger.info("  - Total execution time: {}ms", result.getDurationMs());
    }
}
