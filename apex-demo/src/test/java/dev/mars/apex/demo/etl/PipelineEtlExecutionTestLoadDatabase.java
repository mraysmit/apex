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
 * Test class for PipelineEtlExecutionTestLoadDatabase.yaml
 * Tests loading data to H2 database
 */
@DisplayName("Database Load Pipeline Test")
class PipelineEtlExecutionTestLoadDatabase extends DemoTestBase {

    private static final Logger logger = LoggerFactory.getLogger(PipelineEtlExecutionTestLoadDatabase.class);

    private RulesEngine rulesEngine;

    @BeforeEach
    public void setUp() {
        super.setUp();
        logger.info("Setting up Database Load Pipeline Test...");

        try {
            // Ensure database output directory exists
            Path dbDir = Paths.get("./target/test/etl/output/database");
            Files.createDirectories(dbDir);
        } catch (IOException e) {
            throw new RuntimeException("Failed to create database directory", e);
        }

        logger.info("[OK] Database Load Pipeline Test setup completed");
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
    @DisplayName("Should load data from CSV to H2 database")
    void shouldLoadDataFromCsvToH2Database() throws Exception {
        logger.info("=== Testing Database Load Pipeline ===");

        // Create RulesEngine and execute pipeline
        rulesEngine = RulesEngine.fromClasspath("dev/mars/apex/demo/etl/PipelineEtlExecutionTestLoadDatabase.yaml");

        java.util.Map<String, Object> inputData = new java.util.HashMap<>();
        RuleResult result = rulesEngine.evaluate(inputData);

        // Validate results
        assertNotNull(result, "Pipeline execution result should not be null");
        assertEquals(RuleResult.ResultType.MATCH, result.getResultType(),
            "Pipeline should execute successfully");

        logger.info("[OK] Database load pipeline test completed successfully");
    }
}
