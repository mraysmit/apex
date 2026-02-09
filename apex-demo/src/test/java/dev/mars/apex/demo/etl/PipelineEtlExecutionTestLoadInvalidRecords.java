package dev.mars.apex.demo.etl;

import dev.mars.apex.core.engine.core.RulesEngine;
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
 * Test class for PipelineEtlExecutionTestLoadInvalidRecords.yaml
 * Tests handling of invalid records during load operations
 */
@DisplayName("Invalid Records Load Pipeline Test")
class PipelineEtlExecutionTestLoadInvalidRecords extends DemoTestBase {

    private static final Logger logger = LoggerFactory.getLogger(PipelineEtlExecutionTestLoadInvalidRecords.class);

    private RulesEngine rulesEngine;

    @BeforeEach
    public void setUp() {
        super.setUp();
        logger.info("Setting up Invalid Records Load Pipeline Test...");

        try {
            // Ensure database output directory exists
            Path dbDir = Paths.get("./target/test/etl/output/database");
            Files.createDirectories(dbDir);

            // Create test data with invalid records
            createInvalidRecordsTestData();
        } catch (IOException e) {
            throw new RuntimeException("Failed to setup test data", e);
        }

        logger.info("[OK] Invalid Records Load Pipeline Test setup completed");
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
    @DisplayName("Should handle invalid records gracefully")
    void shouldHandleInvalidRecordsGracefully() throws Exception {
        logger.info("=== Testing Invalid Records Load Pipeline ===");

        // Create RulesEngine and execute pipeline
        rulesEngine = RulesEngine.fromClasspath("dev/mars/apex/demo/etl/PipelineEtlExecutionTestLoadInvalidRecords.yaml");

        java.util.Map<String, Object> inputData = new java.util.HashMap<>();
        RuleResult result = rulesEngine.evaluate(inputData);

        // Validate results
        assertNotNull(result, "Pipeline execution result should not be null");
        assertEquals(RuleResult.ResultType.MATCH, result.getResultType(),
            "Pipeline should execute successfully even with invalid records");

        logger.info("[OK] Invalid records load pipeline test completed successfully");
    }

    private void createInvalidRecordsTestData() throws IOException {
        // CSV file already exists in demo-data/csv/invalid-customers.csv
        Path invalidCsvFile = Paths.get("./demo-data/csv/invalid-customers.csv");
        if (Files.exists(invalidCsvFile)) {
            logger.info("[OK] Invalid records dataset already exists in demo-data");
        } else {
            logger.warn("Invalid records dataset not found at: {}", invalidCsvFile);
        }
    }
}
