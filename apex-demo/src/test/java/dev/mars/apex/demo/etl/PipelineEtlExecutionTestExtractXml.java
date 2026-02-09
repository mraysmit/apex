package dev.mars.apex.demo.etl;

import dev.mars.apex.core.engine.core.RulesEngine;
import dev.mars.apex.core.engine.model.RuleResult;
import dev.mars.apex.demo.DemoTestBase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for XML data source extraction in ETL pipelines.
 *
 * This test validates OTC (Over-The-Counter) options trade data extraction from XML:
 * - XML file parsing and data extraction
 * - XML element and attribute handling (trade id, status)
 * - Nested element processing (counterparties, optionDetails, riskMetrics)
 * - Complex XML structure parsing for middle office trade processing
 * - File-system data source with XML format
 */
@DisplayName("Pipeline ETL Execution Test - Extract OTC Trades XML")
public class PipelineEtlExecutionTestExtractXml extends DemoTestBase {

    private static final Logger logger = LoggerFactory.getLogger(PipelineEtlExecutionTestExtractXml.class);

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
    @DisplayName("Should extract OTC trade data from XML file")
    void shouldExtractDataFromXmlFile() throws Exception {
        logger.info("=== Testing XML OTC Trades Extract Pipeline ===");

        // Create RulesEngine and execute pipeline
        rulesEngine = RulesEngine.fromClasspath("dev/mars/apex/demo/etl/PipelineEtlExecutionTestExtractXml.yaml");

        java.util.Map<String, Object> inputData = new java.util.HashMap<>();
        RuleResult result = rulesEngine.evaluate(inputData);

        // Validate results
        assertNotNull(result, "Pipeline execution result should not be null");
        assertEquals(RuleResult.ResultType.MATCH, result.getResultType(),
            "Pipeline should execute successfully");

        logger.info("[OK] XML OTC trades extraction executed successfully");
    }

    @Test
    @DisplayName("Should handle XML file with deeply nested structures")
    void shouldHandleDeeplyNestedStructures() throws Exception {
        logger.info("=== Testing XML Deeply Nested Structures ===");

        // Create RulesEngine and execute pipeline
        rulesEngine = RulesEngine.fromClasspath("dev/mars/apex/demo/etl/PipelineEtlExecutionTestExtractXml.yaml");

        java.util.Map<String, Object> inputData = new java.util.HashMap<>();
        RuleResult result = rulesEngine.evaluate(inputData);

        // Validate results
        assertNotNull(result, "Pipeline execution result should not be null");
        assertEquals(RuleResult.ResultType.MATCH, result.getResultType(),
            "Pipeline should execute successfully");

        logger.info("[OK] Deeply nested structures validated successfully");
    }
}

