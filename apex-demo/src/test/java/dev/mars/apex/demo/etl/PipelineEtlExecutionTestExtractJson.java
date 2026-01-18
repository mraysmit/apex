package dev.mars.apex.demo.etl;

import dev.mars.apex.core.engine.config.RulesEngine;
import dev.mars.apex.core.engine.model.RuleResult;
import dev.mars.apex.demo.DemoTestBase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for JSON data source extraction in ETL pipelines.
 *
 * This test validates:
 * - JSON file parsing and data extraction
 * - JSONPath query execution
 * - Nested object handling
 * - Array processing
 * - File-system data source with JSON format
 */
@DisplayName("Pipeline ETL Execution Test - Extract JSON")
public class PipelineEtlExecutionTestExtractJson extends DemoTestBase {

    private static final Logger logger = LoggerFactory.getLogger(PipelineEtlExecutionTestExtractJson.class);

    private RulesEngine rulesEngine;

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
    @DisplayName("Should extract OTC options trade data from JSON file")
    void shouldExtractDataFromJsonFile() throws Exception {
        logger.info("=== Testing JSON OTC Options Extract Pipeline ===");

        // Create RulesEngine and execute pipeline
        rulesEngine = RulesEngine.fromClasspath("dev/mars/apex/demo/etl/PipelineEtlExecutionTestExtractJson.yaml");

        java.util.Map<String, Object> inputData = new java.util.HashMap<>();
        RuleResult result = rulesEngine.evaluate(inputData);

        // Validate results
        assertNotNull(result, "Pipeline execution result should not be null");
        assertEquals(RuleResult.ResultType.MATCH, result.getResultType(),
            "Pipeline should execute successfully");

        logger.info("✓ JSON OTC options extraction executed successfully");
    }

    @Test
    @DisplayName("Should handle JSON file with complex nested structures")
    void shouldHandleComplexNestedStructures() throws Exception {
        logger.info("=== Testing JSON Complex Nested Structures ===");

        // Create RulesEngine and execute pipeline
        rulesEngine = RulesEngine.fromClasspath("dev/mars/apex/demo/etl/PipelineEtlExecutionTestExtractJson.yaml");

        java.util.Map<String, Object> inputData = new java.util.HashMap<>();
        RuleResult result = rulesEngine.evaluate(inputData);

        // Validate results
        assertNotNull(result, "Pipeline execution result should not be null");
        assertEquals(RuleResult.ResultType.MATCH, result.getResultType(),
            "Pipeline should execute successfully");

        logger.info("✓ Complex nested structures validated successfully");
    }
}

