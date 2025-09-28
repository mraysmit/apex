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
import java.nio.file.StandardOpenOption;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for PipelineEtlExecutionTestLoadBatch.yaml
 * Tests batch processing functionality
 */
@DisplayName("Batch Load Pipeline Test")
class PipelineEtlExecutionTestLoadBatch extends DemoTestBase {

    private static final Logger logger = LoggerFactory.getLogger(PipelineEtlExecutionTestLoadBatch.class);
    
    private DataPipelineEngine pipelineEngine;
    private YamlConfigurationLoader yamlLoader;

    @BeforeEach
    public void setUp() {
        super.setUp();
        logger.info("Setting up Batch Load Pipeline Test...");
        pipelineEngine = new DataPipelineEngine();
        yamlLoader = new YamlConfigurationLoader();

        try {
            // Ensure database directory exists
            Path dbDir = Paths.get("./demo-data/database");
            Files.createDirectories(dbDir);

        } catch (IOException e) {
            throw new RuntimeException("Failed to create database directory", e);
        }

        logger.info("✓ Batch Load Pipeline Test setup completed");
    }



    @Test
    @DisplayName("Should process data in batches")
    void shouldProcessDataInBatches() throws Exception {
        logger.info("=== Testing Batch Load Pipeline ===");

        // Load the YAML configuration
        YamlRuleConfiguration config = yamlLoader.loadFromFile(
            "src/test/java/dev/mars/apex/demo/etl/PipelineEtlExecutionTestLoadBatch.yaml");
        
        // Initialize and execute pipeline
        pipelineEngine.initialize(config);
        YamlPipelineExecutionResult result = pipelineEngine.executePipeline("batch-load-pipeline");

        // Validate results
        assertNotNull(result, "Pipeline execution result should not be null");
        assertTrue(result.isSuccess(), "Pipeline should execute successfully");
        assertEquals(2, result.getStepResults().size(), "Should have 2 step results (extract + load)");

        // Validate extract step
        var extractResult = result.getStepResults().get(0);
        assertEquals("extract-large-dataset", extractResult.getStepName());
        assertTrue(extractResult.isSuccess(), "Extract step should succeed");

        // Validate load step
        var loadResult = result.getStepResults().get(1);
        assertEquals("batch-load-customers", loadResult.getStepName());
        assertTrue(loadResult.isSuccess(), "Load step should succeed");

        // Verify database was created (this pipeline loads to database, not file)
        Path dbFile = Paths.get("./demo-data/database/batch_db.mv.db");
        assertTrue(Files.exists(dbFile), "Batch database file should be created");

        logger.info("✓ Batch load pipeline test completed successfully");
        logger.info("  - Extract step: {} (success: {})", extractResult.getStepName(), extractResult.isSuccess());
        logger.info("  - Load step: {} (success: {})", loadResult.getStepName(), loadResult.isSuccess());
        logger.info("  - Database file: {}", dbFile.toAbsolutePath());
        logger.info("  - Total execution time: {}ms", result.getDurationMs());
    }
}
