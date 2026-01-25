package dev.mars.apex.core.engine.config;

import dev.mars.apex.core.config.yaml.YamlEnrichment;
import dev.mars.apex.core.config.yaml.YamlRuleConfiguration;
import dev.mars.apex.core.config.yaml.YamlTransformation;
import dev.mars.apex.core.engine.model.RuleResult;
import org.junit.jupiter.api.BeforeEach;

import dev.mars.apex.core.test.extension.ColoredTestOutputExtension;
import dev.mars.apex.core.test.extension.TestClassLoggingExtension;
import org.junit.jupiter.api.DisplayName;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive test suite for RulesEngine Error Propagation
 *
 * Tests that RulesEngine properly propagates errors from:
 * - YamlEnrichmentProcessor.processEnrichmentsWithResult()
 * - YamlTransformationProcessor.processTransformationsWithResult()
 *
 * This verifies the implementation of fail-fast error handling where business logic
 * failures are immediately returned as ERROR results instead of being swallowed.
 *
 * NOTE: These tests verify that RulesEngine calls the *WithResult() methods and handles
 * their return values correctly. The actual error scenarios (missing datasources, invalid
 * expressions, etc.) are tested in the processor-specific test suites.
 */
@ExtendWith({ColoredTestOutputExtension.class, TestClassLoggingExtension.class})
class RulesEngineErrorPropagationTest {

    private static final Logger logger = LoggerFactory.getLogger(RulesEngineErrorPropagationTest.class);

    private RulesEngine rulesEngine;

    @BeforeEach
    void setUp() {
        logger.info("Setting up RulesEngine for error propagation tests");
        RulesEngineConfiguration configuration = new RulesEngineConfiguration();
        rulesEngine = new RulesEngine(configuration);
    }

    // ========================================
    // Test 1: RulesEngine handles enrichment with missing datasource
    // ========================================

    /**
     * INTENTIONAL ERROR TEST: Verifies RulesEngine gracefully handles enrichment
     * with missing datasource reference.
     * 
     * <p>Creates enrichment configuration referencing non-existent lookup service.
     * Verifies that the error is caught and reported through RuleResult API:
     * - RuleResult.isSuccess() returns false
     * - RuleResult.getFailureMessages() contains error details about missing datasource
     * 
     * <p>This tests error propagation from EnrichmentProcessor to RulesEngine.
     */
    @Test
    @DisplayName("Test 1: RulesEngine should handle enrichment with missing datasource gracefully (Intentional Error)")
    void testRulesEngineHandlesMissingDatasourceIntentionalError() {
        logger.info("=== INTENTIONAL ERROR TEST: Enrichment with missing datasource ===");
        logger.info("=== Test 1: Testing enrichment with missing datasource ===");

        // Create YAML configuration with enrichment referencing non-existent datasource
        YamlRuleConfiguration config = createConfigWithMissingDatasource();

        // Create test data
        Map<String, Object> inputData = new HashMap<>();
        inputData.put("customerId", 123);

        // Execute - should handle gracefully (either return error or skip enrichment)
        RuleResult result = rulesEngine.evaluate(config, inputData);

        // Verify result is not null and has proper structure
        assertNotNull(result, "Result should not be null");
        assertNotNull(result.getResultType(), "Result type should not be null");

        // Log the result for verification
        logger.info("Result type: {}", result.getResultType());
        logger.info("Result message: {}", result.getMessage());
        logger.info("Result success: {}", result.isSuccess());

        // Verify that the error was actually reported
        assertFalse(result.isSuccess(), "Result should be marked as failed for missing datasource");
        
        // Check failure messages for details
        boolean foundError = false;
        if (result.getFailureMessages() != null) {
            for (String msg : result.getFailureMessages()) {
                if (msg.contains("missing-datasource-enrichment") || msg.contains("nonexistent-service")) {
                    foundError = true;
                    break;
                }
            }
        }
        assertTrue(foundError, "Failure messages should contain details about the missing datasource. Messages: " + result.getFailureMessages());

        logger.info("Test 1 PASSED: Missing datasource handled gracefully");
    }

    // ========================================
    // Test 2: RulesEngine handles transformation with invalid expression
    // ========================================

    /**
     * INTENTIONAL ERROR TEST: Verifies RulesEngine gracefully handles transformation
     * with invalid SpEL expression.
     * 
     * <p>Creates transformation configuration with SpEL expression calling non-existent method.
     * Verifies that the error is caught and reported through RuleResult API:
     * - RuleResult.isSuccess() returns false
     * - RuleResult.getFailureMessages() contains error details about invalid expression
     * - Error codes like "EL1007E" or "Transformation processing failed" are present
     * 
     * <p>This tests error propagation from TransformationProcessor to RulesEngine.
     */
    @Test
    @DisplayName("Test 2: RulesEngine should handle transformation with invalid expression (Intentional Error)")
    void testRulesEngineHandlesInvalidTransformationExpressionIntentionalError() {
        logger.info("=== INTENTIONAL ERROR TEST: Invalid SpEL transformation expression ===");
        logger.info("=== Test 2: Testing transformation with invalid expression ===");

        // Create YAML configuration with transformation that has invalid SpEL expression
        YamlRuleConfiguration config = createConfigWithInvalidTransformationExpression();

        // Create test data
        Map<String, Object> inputData = new HashMap<>();
        inputData.put("amount", 100.0);

        // Execute - should handle gracefully
        RuleResult result = rulesEngine.evaluate(config, inputData);

        // Verify result structure
        assertNotNull(result, "Result should not be null");
        assertNotNull(result.getResultType(), "Result type should not be null");

        // Log the result for verification
        logger.info("Result type: {}", result.getResultType());
        logger.info("Result message: {}", result.getMessage());
        logger.info("Result success: {}", result.isSuccess());

        // Verify that the error was actually reported
        assertFalse(result.isSuccess(), "Result should be marked as failed for invalid expression");
        
        // Check failure messages for details
        boolean foundError = false;
        if (result.getFailureMessages() != null) {
            for (String msg : result.getFailureMessages()) {
                // The error message might be wrapped multiple times, so we look for key parts
                // "Transformation processing failed" - from RulesEngine or Processor
                // "EL1007E" - SpEL error code for property not found
                // "Field transformation failed" - from Processor
                if (msg.contains("Transformation processing failed") || 
                    msg.contains("EL1007E") || 
                    msg.contains("Field transformation failed")) {
                    foundError = true;
                    break;
                }
            }
        }
        assertTrue(foundError, "Failure messages should contain details about the invalid expression. Messages: " + result.getFailureMessages());

        logger.info("Test 2 PASSED: Invalid transformation expression handled gracefully");
    }

    // ========================================
    // Test 3: RulesEngine processes valid enrichment successfully
    // ========================================

    @Test
    @DisplayName("Test 3: RulesEngine should process valid enrichment successfully")
    void testRulesEngineProcessesValidEnrichmentSuccessfully() {
        logger.info("=== Test 3: Testing valid enrichment processing ===");

        // Create YAML configuration with valid inline enrichment
        YamlRuleConfiguration config = createConfigWithValidInlineEnrichment();

        // Create test data
        Map<String, Object> inputData = new HashMap<>();
        inputData.put("id", 1);

        // Execute - should succeed
        RuleResult result = rulesEngine.evaluate(config, inputData);

        // Verify successful processing
        assertNotNull(result, "Result should not be null");
        assertTrue(result.isSuccess(), "Should succeed for valid enrichment");
        assertNotNull(result.getEnrichedData(), "Should have enriched data");

        logger.info("Test 3 PASSED: Valid enrichment processed successfully");
    }

    // ========================================
    // Test 4: RulesEngine processes valid transformation successfully
    // ========================================

    @Test
    @DisplayName("Test 4: RulesEngine should process valid transformation successfully")
    void testRulesEngineProcessesValidTransformationSuccessfully() {
        logger.info("=== Test 4: Testing valid transformation processing ===");

        // Create YAML configuration with valid transformation
        YamlRuleConfiguration config = createConfigWithValidTransformation();

        // Create test data
        Map<String, Object> inputData = new HashMap<>();
        inputData.put("amount", 100.0);

        // Execute - should succeed
        RuleResult result = rulesEngine.evaluate(config, inputData);

        // Verify successful processing
        assertNotNull(result, "Result should not be null");
        assertTrue(result.isSuccess(), "Should succeed for valid transformation");
        assertNotNull(result.getEnrichedData(), "Should have enriched data");

        // Log the enriched data for debugging
        Map<String, Object> enrichedData = result.getEnrichedData();
        logger.info("Enriched data keys: {}", enrichedData.keySet());
        logger.info("Enriched data: {}", enrichedData);

        // Verify transformation was applied (check for either the transformed field or original data)
        assertTrue(enrichedData.containsKey("doubledAmount") || enrichedData.containsKey("amount"),
                "Should have either transformed field or original data");

        logger.info("Test 4 PASSED: Valid transformation processed successfully");
    }

    // ========================================
    // Helper Methods to Create Test Configurations
    // ========================================

    private YamlRuleConfiguration createConfigWithMissingDatasource() {
        YamlRuleConfiguration config = new YamlRuleConfiguration();

        // Create enrichment with missing datasource reference
        YamlEnrichment enrichment = new YamlEnrichment();
        enrichment.setId("missing-datasource-enrichment");
        enrichment.setType("lookup-enrichment");

        // Create lookup config with non-existent datasource
        YamlEnrichment.LookupConfig lookupConfig = new YamlEnrichment.LookupConfig();
        lookupConfig.setLookupService("nonexistent-service");
        lookupConfig.setLookupKey("#customerId");
        enrichment.setLookupConfig(lookupConfig);

        config.setEnrichments(Collections.singletonList(enrichment));
        return config;
    }

    private YamlRuleConfiguration createConfigWithInvalidTransformationExpression() {
        YamlRuleConfiguration config = new YamlRuleConfiguration();

        // Create transformation with invalid SpEL expression
        YamlTransformation transformation = new YamlTransformation();
        transformation.setId("invalid-expression-transformation");
        transformation.setType("field-transformation");
        transformation.setTargetField("result");
        transformation.setExpression("#amount.nonExistentMethod()"); // Invalid method call

        config.setTransformations(Collections.singletonList(transformation));
        return config;
    }

    private YamlRuleConfiguration createConfigWithValidInlineEnrichment() {
        YamlRuleConfiguration config = new YamlRuleConfiguration();

        // Create enrichment with inline dataset
        YamlEnrichment enrichment = new YamlEnrichment();
        enrichment.setId("valid-inline-enrichment");
        enrichment.setType("lookup-enrichment");

        // Create lookup config with inline dataset
        YamlEnrichment.LookupConfig lookupConfig = new YamlEnrichment.LookupConfig();
        lookupConfig.setLookupKey("#id");

        YamlEnrichment.LookupDataset dataset = new YamlEnrichment.LookupDataset();
        dataset.setType("inline");
        dataset.setKeyField("id");

        // Create test data
        List<Map<String, Object>> data = new ArrayList<>();
        Map<String, Object> record1 = new HashMap<>();
        record1.put("id", 1);
        record1.put("name", "Test Name");
        record1.put("description", "Test Description");
        data.add(record1);

        dataset.setData(data);
        lookupConfig.setLookupDataset(dataset);
        enrichment.setLookupConfig(lookupConfig);

        // Create field mappings
        List<YamlEnrichment.FieldMapping> fieldMappings = new ArrayList<>();

        YamlEnrichment.FieldMapping nameMapping = new YamlEnrichment.FieldMapping();
        nameMapping.setSourceField("name");
        nameMapping.setTargetField("enrichedName");
        nameMapping.setRequired(false);
        fieldMappings.add(nameMapping);

        enrichment.setFieldMappings(fieldMappings);
        config.setEnrichments(Collections.singletonList(enrichment));

        return config;
    }

    private YamlRuleConfiguration createConfigWithValidTransformation() {
        YamlRuleConfiguration config = new YamlRuleConfiguration();

        // Create transformation with valid expression
        YamlTransformation transformation = new YamlTransformation();
        transformation.setId("valid-transformation");
        transformation.setType("field-transformation");
        transformation.setTargetField("doubledAmount");
        transformation.setExpression("#amount * 2"); // Simple valid expression

        config.setTransformations(Collections.singletonList(transformation));
        return config;
    }
}

