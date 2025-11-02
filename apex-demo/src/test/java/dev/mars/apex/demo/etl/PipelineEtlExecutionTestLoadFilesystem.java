package dev.mars.apex.demo.etl;

import dev.mars.apex.core.engine.config.RulesEngine;
import dev.mars.apex.core.engine.model.RuleResult;
import dev.mars.apex.demo.DemoTestBase;
import org.junit.jupiter.api.AfterEach;
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

    private RulesEngine rulesEngine;

    @BeforeEach
    public void setUp() {
        super.setUp();
        logger.info("Setting up Filesystem Load Pipeline Test...");

        try {
            // Ensure output directory exists
            Path outputDir = Paths.get("./demo-data/json");
            Files.createDirectories(outputDir);
        } catch (IOException e) {
            throw new RuntimeException("Failed to create output directory", e);
        }

        logger.info("✓ Filesystem Load Pipeline Test setup completed");
    }

    @AfterEach
    public void tearDown() {
        if (rulesEngine != null) {
            try {
                rulesEngine.shutdown();
            } catch (Exception e) {
                logger.warn("Error shutting down rules engine", e);
            }
        }
        super.tearDown();
    }

    @Test
    @DisplayName("Should load data from CSV to JSON file")
    void shouldLoadDataFromCsvToJsonFile() throws Exception {
        logger.info("=== Testing Filesystem Load Pipeline ===");

        // Create RulesEngine and execute pipeline
        rulesEngine = RulesEngine.fromFile(
            "src/test/java/dev/mars/apex/demo/etl/PipelineEtlExecutionTestLoadFilesystem.yaml");

        java.util.Map<String, Object> inputData = new java.util.HashMap<>();
        RuleResult result = rulesEngine.evaluate(inputData);

        // Validate results
        assertNotNull(result, "Pipeline execution result should not be null");
        assertEquals(RuleResult.ResultType.MATCH, result.getResultType(),
            "Pipeline should execute successfully");

        // Verify output file was created
        Path outputFile = Paths.get("./demo-data/json/customers.json");
        assertTrue(Files.exists(outputFile), "Output JSON file should be created");
        assertTrue(Files.size(outputFile) > 0, "Output file should not be empty");

        logger.info("✓ Filesystem load pipeline test completed successfully");
        logger.info("  - Output file: {}", outputFile.toAbsolutePath());
    }
}
