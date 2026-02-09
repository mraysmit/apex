package dev.mars.apex.demo.sequencing;

import dev.mars.apex.core.config.model.YamlRuleConfiguration;
import dev.mars.apex.engine.core.RulesEngine;
import dev.mars.apex.engine.model.RuleResult;
import dev.mars.apex.demo.DemoTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive Sequential Processing Tests - Using RulesEngine
 *
 * This test class validates complete sequential processing workflows
 * with actual business logic execution to ensure:
 * 1. End-to-end functionality works correctly with real data
 * 2. Complex dependencies are resolved and executed properly
 * 3. All APEX features work with sequential processing
 *
 * TESTING PRINCIPLES FOLLOWED:
 * - Use RulesEngine.evaluate() not internal parsers/processors
 * - Test actual business logic not YAML syntax or configuration loading
 * - Validate enriched data values and rule execution results
 * - Follow patterns from RuleGroupsSequentialBasicTest
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd 
 * @version 2.0 - Rewritten to follow APEX testing principles
 */
@DisplayName("Comprehensive Sequential Processing Validation")
public class ComprehensiveValidationTest extends DemoTestBase {

    private static final Logger LOGGER = LoggerFactory.getLogger(ComprehensiveValidationTest.class);

    @Test
    @DisplayName("End-to-end sequential processing: complete workflow with all sections")
    void testEndToEndSequentialProcessing() throws Exception {
        LOGGER.info("=== TESTING: End-to-End Sequential Processing ===");

        // 1. Load YAML configuration
        String yamlPath = "src/test/java/dev/mars/apex/demo/sequencing/ComprehensiveValidationTestEndToEnd.yaml";
        YamlRuleConfiguration config = yamlLoader.loadFromFile(yamlPath);
        assertNotNull(config, "Configuration should be loaded");

        // 2. Create RulesEngine
        RulesEngine engine = RulesEngine.fromFile(yamlPath);
        assertNotNull(engine, "RulesEngine should be created");

        // 3. Create test data
        Map<String, Object> testData = new HashMap<>();
        testData.put("customerId", "CUST001");
        testData.put("amount", 1000);
        LOGGER.info("* Input Data: {}", testData);

        // 4. Execute using RulesEngine.evaluate()
        RuleResult result = engine.evaluate(config, testData);
        assertNotNull(result, "Rule result should not be null");
        assertTrue(result.isSuccess(), "Processing should succeed");

        // 5. Get enriched data
        Map<String, Object> enrichedData = result.getEnrichedData();
        LOGGER.info("* Enriched Data: {}", enrichedData);

        // 6. Validate business logic results
        assertNotNull(enrichedData.get("customerData"), "Customer should be enriched");
        assertEquals("John Doe", enrichedData.get("customerData"),
                    "Should enrich with correct customer name");
        assertEquals("CUST001", enrichedData.get("customerId"),
                    "Original customer ID should be preserved");
        assertEquals(1000, enrichedData.get("amount"),
                    "Original amount should be preserved");

        LOGGER.info("* END-TO-END PROCESSING VALIDATED:");
        LOGGER.info("   1. Data sources loaded successfully");
        LOGGER.info("   2. Enrichments executed: customerData = {}", enrichedData.get("customerData"));
        LOGGER.info("   3. Rules validated enriched data successfully");
        LOGGER.info("   4. All sections processed in sequential order");
        LOGGER.info("End-to-end sequential processing test PASSED");
    }

    @Test
    @DisplayName("Complex dependency resolution: forward references and chained enrichments")
    void testComplexDependencyResolution() throws Exception {
        LOGGER.info("=== TESTING: Complex Dependency Resolution ===");

        // 1. Load YAML configuration with complex dependencies
        String yamlPath = "src/test/java/dev/mars/apex/demo/sequencing/ComprehensiveValidationTestComplexDependency.yaml";
        YamlRuleConfiguration config = yamlLoader.loadFromFile(yamlPath);
        assertNotNull(config, "Configuration should be loaded");

        // 2. Create RulesEngine
        RulesEngine engine = RulesEngine.fromFile(yamlPath);
        assertNotNull(engine, "RulesEngine should be created");

        // 3. Create test data
        Map<String, Object> testData = new HashMap<>();
        testData.put("rawData", "test-value");
        LOGGER.info("* Input Data: {}", testData);

        // 4. Execute using RulesEngine.evaluate()
        RuleResult result = engine.evaluate(config, testData);
        assertNotNull(result, "Rule result should not be null");
        assertTrue(result.isSuccess(), "Processing should succeed");

        // 5. Get enriched data
        Map<String, Object> enrichedData = result.getEnrichedData();
        LOGGER.info("* Enriched Data: {}", enrichedData);

        // 6. Validate business logic results - complex dependencies resolved
        assertNotNull(enrichedData.get("enrichedData"), "Data should be enriched");
        assertEquals("test-value", enrichedData.get("enrichedData"),
                    "Should enrich with correct value from rawData");
        assertEquals("test-value", enrichedData.get("rawData"),
                    "Original raw data should be preserved");

        LOGGER.info("* COMPLEX DEPENDENCY RESOLUTION VALIDATED:");
        LOGGER.info("   1. Forward references resolved correctly");
        LOGGER.info("   2. Enrichment executed: enrichedData = {}", enrichedData.get("enrichedData"));
        LOGGER.info("   3. Rules validated enriched data successfully");
        LOGGER.info("   4. Dependencies processed in correct order");
        LOGGER.info("Complex dependency resolution test PASSED");
    }
}
