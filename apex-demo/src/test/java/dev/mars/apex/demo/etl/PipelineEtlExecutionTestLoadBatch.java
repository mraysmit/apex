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
 * Test class for PipelineEtlExecutionTestLoadBatch.yaml
 * Tests batch processing functionality
 */
@DisplayName("Batch Load Pipeline Test")
class PipelineEtlExecutionTestLoadBatch extends DemoTestBase {

    private static final Logger logger = LoggerFactory.getLogger(PipelineEtlExecutionTestLoadBatch.class);

    private RulesEngine rulesEngine;

    @BeforeEach
    public void setUp() {
        super.setUp();
        logger.info("Setting up Batch Load Pipeline Test...");

        try {
            // Ensure database directory exists
            Path dbDir = Paths.get("./demo-data/database");
            Files.createDirectories(dbDir);

        } catch (IOException e) {
            throw new RuntimeException("Failed to create database directory", e);
        }

        logger.info("[OK] Batch Load Pipeline Test setup completed");
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
    @DisplayName("Should process data in batches")
    void shouldProcessDataInBatches() throws Exception {
        logger.info("=== Testing Batch Load Pipeline ===");

        // Create RulesEngine and execute pipeline
        rulesEngine = RulesEngine.fromClasspath("dev/mars/apex/demo/etl/PipelineEtlExecutionTestLoadBatch.yaml");

        java.util.Map<String, Object> inputData = new java.util.HashMap<>();
        RuleResult result = rulesEngine.evaluate(inputData);

        // Validate results
        assertNotNull(result, "Pipeline execution result should not be null");
        assertEquals(RuleResult.ResultType.MATCH, result.getResultType(),
            "Pipeline should execute successfully");

        // Verify database was created (this pipeline loads to database, not file)
        Path dbFile = Paths.get("./demo-data/database/batch_db.mv.db");
        assertTrue(Files.exists(dbFile), "Batch database file should be created");

        logger.info("[OK] Batch load pipeline test completed successfully");
        logger.info("  - Database file: {}", dbFile.toAbsolutePath());
    }
}
