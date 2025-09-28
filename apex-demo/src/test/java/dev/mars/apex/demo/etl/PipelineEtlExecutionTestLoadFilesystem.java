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
 * Test class for PipelineEtlExecutionTestLoadFilesystem.yaml
 * Tests loading data to filesystem (JSON file)
 */
@DisplayName("Filesystem Load Pipeline Test")
class PipelineEtlExecutionTestLoadFilesystem extends DemoTestBase {

    private static final Logger logger = LoggerFactory.getLogger(PipelineEtlExecutionTestLoadFilesystem.class);
    
    private DataPipelineEngine pipelineEngine;
    private YamlConfigurationLoader yamlLoader;

    @BeforeEach
    public void setUp() {
        super.setUp();
        logger.info("Setting up Filesystem Load Pipeline Test...");
        pipelineEngine = new DataPipelineEngine();
        yamlLoader = new YamlConfigurationLoader();

        try {
            // Ensure output directory exists
            Path outputDir = Paths.get("./target/test/etl/output");
            Files.createDirectories(outputDir);
        } catch (IOException e) {
            throw new RuntimeException("Failed to create output directory", e);
        }

        logger.info("✓ Filesystem Load Pipeline Test setup completed");
    }

    @Test
    @DisplayName("Should load data from CSV to JSON file")
    void shouldLoadDataFromCsvToJsonFile() throws Exception {
        logger.info("=== Testing Filesystem Load Pipeline ===");

        // Load the YAML configuration
        YamlRuleConfiguration config = yamlLoader.loadFromFile(
            "src/test/java/dev/mars/apex/demo/etl/PipelineEtlExecutionTestLoadFilesystem.yaml");
        
        // Initialize and execute pipeline
        pipelineEngine.initialize(config);
        YamlPipelineExecutionResult result = pipelineEngine.executePipeline("load-filesystem-pipeline");

        // Validate results
        assertNotNull(result, "Pipeline execution result should not be null");
        assertTrue(result.isSuccess(), "Pipeline should execute successfully");
        assertEquals(2, result.getStepResults().size(), "Should have 2 step results (extract + load)");

        // Validate extract step
        var extractResult = result.getStepResults().get(0);
        assertEquals("extract-customers", extractResult.getStepName());
        assertTrue(extractResult.isSuccess(), "Extract step should succeed");
        
        // Validate load step
        var loadResult = result.getStepResults().get(1);
        assertEquals("load-to-file", loadResult.getStepName());
        assertTrue(loadResult.isSuccess(), "Load step should succeed");

        // Verify output file was created
        Path outputFile = Paths.get("./target/test/etl/output/customers.json");
        assertTrue(Files.exists(outputFile), "Output JSON file should be created");
        assertTrue(Files.size(outputFile) > 0, "Output file should not be empty");

        logger.info("✓ Filesystem load pipeline test completed successfully");
        logger.info("  - Extract step: {} (success: {})", extractResult.getStepName(), extractResult.isSuccess());
        logger.info("  - Load step: {} (success: {})", loadResult.getStepName(), loadResult.isSuccess());
        logger.info("  - Output file: {}", outputFile.toAbsolutePath());
        logger.info("  - Total execution time: {}ms", result.getDurationMs());
    }
}
