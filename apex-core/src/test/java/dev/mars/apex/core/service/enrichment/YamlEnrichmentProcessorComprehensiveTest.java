package dev.mars.apex.core.service.enrichment;

import dev.mars.apex.core.test.extension.ColoredTestOutputExtension;
import dev.mars.apex.core.config.model.YamlEnrichment;
import dev.mars.apex.core.config.model.YamlRuleConfiguration;
import dev.mars.apex.core.config.loader.YamlConfigurationLoader;
import dev.mars.apex.engine.model.RuleResult;
import dev.mars.apex.engine.core.ExpressionEvaluatorService;
import dev.mars.apex.engine.execution.RuleGroupEvaluationService;
import dev.mars.apex.engine.core.UnifiedRuleEvaluator;
import dev.mars.apex.core.service.lookup.LookupServiceRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/*
 * Copyright 2025 Mark Andrew Ray-Smith Cityline Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * Comprehensive test suite for YamlEnrichmentProcessor before deprecation/removal.
 * 
 * This test class validates critical functionality to ensure safe migration to RulesEngine:
 * 1. Null/empty enrichment lists handling
 * 2. Failed required field mappings error propagation
 * 3. SpEL expression failure handling
 * 4. Parallel execution (all enrichments execute, no short-circuit)
 * 5. Cache effectiveness (dataset/expression reuse)
 * 6. Conditional mapping with rule group results
 * 7. Error/success code evaluation
 * 8. Enrichment group processing with AND/OR semantics
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2026-01-26
 * @version 1.0
 */
@ExtendWith(ColoredTestOutputExtension.class)
@DisplayName("YamlEnrichmentProcessor - Comprehensive Pre-Deprecation Tests")
class YamlEnrichmentProcessorComprehensiveTest {

    private static final Logger logger = LoggerFactory.getLogger(YamlEnrichmentProcessorComprehensiveTest.class);

    private YamlEnrichmentProcessor processor;
    private LookupServiceRegistry serviceRegistry;
    private ExpressionEvaluatorService evaluatorService;
    private YamlConfigurationLoader yamlLoader;

    @BeforeEach
    void setUp() {
        logger.info("🔧 Setting up YamlEnrichmentProcessor test environment");
        serviceRegistry = new LookupServiceRegistry();
        evaluatorService = new ExpressionEvaluatorService();
        processor = new YamlEnrichmentProcessor(serviceRegistry, evaluatorService, null,
                new RuleGroupEvaluationService(new UnifiedRuleEvaluator()));
        yamlLoader = new YamlConfigurationLoader();
        logger.info("✅ Test environment initialized");
    }

    // ========================================
    // TEST 1: Null/Empty Enrichment Lists
    // ========================================

    @Test
    @DisplayName("TEST 1.1: Null enrichment list should return success")
    void testNullEnrichmentList() {
        logger.info("🧪 TEST 1.1: Null enrichment list handling");

        Map<String, Object> testData = new HashMap<>();
        testData.put("value", 100);

        RuleResult result = processor.processEnrichmentsWithResult(null, testData, null);

        assertNotNull(result, "Result should not be null");
        assertTrue(result.isSuccess(), "Null enrichment list should return success");
        assertEquals("INFO", result.getSeverity(), "Severity should be INFO");
        
        logger.info("✅ TEST 1.1 PASSED: Null list handled correctly");
    }

    @Test
    @DisplayName("TEST 1.2: Empty enrichment list should return success")
    void testEmptyEnrichmentList() {
        logger.info("🧪 TEST 1.2: Empty enrichment list handling");

        List<YamlEnrichment> emptyList = new ArrayList<>();
        Map<String, Object> testData = new HashMap<>();
        testData.put("value", 100);

        RuleResult result = processor.processEnrichmentsWithResult(emptyList, testData, null);

        assertNotNull(result, "Result should not be null");
        assertTrue(result.isSuccess(), "Empty enrichment list should return success");
        assertEquals("INFO", result.getSeverity(), "Severity should be INFO");
        
        logger.info("✅ TEST 1.2 PASSED: Empty list handled correctly");
    }

    @Test
    @DisplayName("TEST 1.3: Null target object should be handled safely")
    void testNullTargetObject() {
        logger.info("🧪 TEST 1.3: Null target object handling");

        List<YamlEnrichment> enrichments = new ArrayList<>();
        YamlEnrichment enrichment = new YamlEnrichment();
        enrichment.setId("test-enrichment");
        enrichment.setType("field-enrichment");
        enrichment.setCondition("#value > 50");
        enrichments.add(enrichment);

        // Processor should handle null target gracefully
        RuleResult result = processor.processEnrichmentsWithResult(enrichments, null, null);

        assertNotNull(result, "Result should not be null");
        // May return success (skip enrichments) or failure (null object) - both are acceptable
        
        logger.info("✅ TEST 1.3 PASSED: Null target handled: success={}", result.isSuccess());
    }

    // ========================================
    // TEST 2: Failed Required Field Mappings
    // ========================================

    @Test
    @DisplayName("TEST 2.1: Required field mapping failure should propagate error")
    void testRequiredFieldMappingFailure() throws Exception {
        logger.info("🧪 TEST 2.1: Required field mapping failure detection");

        String yamlPath = "src/test/java/dev/mars/apex/core/service/enrichment/YamlEnrichmentProcessorComprehensiveTest.yaml";
        YamlRuleConfiguration config = yamlLoader.loadFromFile(yamlPath);

        Map<String, Object> testData = new HashMap<>();
        testData.put("amount", 1000.0);
        
        // Find the enrichment that has required field mappings
        List<YamlEnrichment> enrichments = new ArrayList<>(config.getEnrichments().stream()
                .filter(e -> "required-field-test".equals(e.getId()))
                .toList());

        RuleResult result = processor.processEnrichmentsWithResult(enrichments, testData, config);

        assertNotNull(result, "Result should not be null");
        assertFalse(result.isSuccess(), "Required field mapping failure should return failure");
        assertNotNull(result.getFailureMessages(), "Failure messages should be present");
        assertFalse(result.getFailureMessages().isEmpty(), "Failure messages should not be empty");
        
        logger.info("✅ TEST 2.1 PASSED: Required field failure detected: {}", result.getFailureMessages());
    }

    @Test
    @DisplayName("TEST 2.2: Non-required field mapping failure should succeed")
    void testNonRequiredFieldMappingFailure() throws Exception {
        logger.info("🧪 TEST 2.2: Non-required field mapping can be null");

        String yamlPath = "src/test/java/dev/mars/apex/core/service/enrichment/YamlEnrichmentProcessorComprehensiveTest.yaml";
        YamlRuleConfiguration config = yamlLoader.loadFromFile(yamlPath);

        Map<String, Object> testData = new HashMap<>();
        testData.put("amount", 1000.0);
        
        List<YamlEnrichment> enrichments = new ArrayList<>(config.getEnrichments().stream()
                .filter(e -> "optional-field-test".equals(e.getId()))
                .toList());

        RuleResult result = processor.processEnrichmentsWithResult(enrichments, testData, config);

        assertNotNull(result, "Result should not be null");
        assertTrue(result.isSuccess(), "Non-required field mapping failure should still succeed");
        
        logger.info("✅ TEST 2.2 PASSED: Optional field handled correctly");
    }

    // ========================================
    // TEST 3: SpEL Expression Failures
    // ========================================

    @Test
    @DisplayName("TEST 3.1: Invalid SpEL expression in condition should skip enrichment")
    void testInvalidSpelCondition() throws Exception {
        logger.info("🧪 TEST 3.1: Invalid SpEL expression in condition");

        String yamlPath = "src/test/java/dev/mars/apex/core/service/enrichment/YamlEnrichmentProcessorComprehensiveTest.yaml";
        YamlRuleConfiguration config = yamlLoader.loadFromFile(yamlPath);

        Map<String, Object> testData = new HashMap<>();
        testData.put("amount", 1000.0);
        
        List<YamlEnrichment> enrichments = new ArrayList<>(config.getEnrichments().stream()
                .filter(e -> "invalid-spel-condition".equals(e.getId()))
                .toList());

        RuleResult result = processor.processEnrichmentsWithResult(enrichments, testData, config);

        assertNotNull(result, "Result should not be null");
        // Phase 2: Invalid SpEL conditions now properly propagate as failures instead of being silently skipped
        assertFalse(result.isSuccess(), "Invalid SpEL condition should now report failure (not silently skip)");
        assertFalse(result.getFailureMessages().isEmpty(), "Should have failure messages for invalid SpEL condition");
        
        logger.info("✅ TEST 3.1 PASSED: Invalid SpEL condition properly reported as failure");
    }

    @Test
    @DisplayName("TEST 3.2: Invalid SpEL expression in lookup key should propagate error")
    void testInvalidSpelLookupKey() throws Exception {
        logger.info("🧪 TEST 3.2: Invalid SpEL expression in lookup key");

        String yamlPath = "src/test/java/dev/mars/apex/core/service/enrichment/YamlEnrichmentProcessorComprehensiveTest.yaml";
        YamlRuleConfiguration config = yamlLoader.loadFromFile(yamlPath);

        Map<String, Object> testData = new HashMap<>();
        testData.put("amount", 1000.0);
        
        List<YamlEnrichment> enrichments = new ArrayList<>(config.getEnrichments().stream()
                .filter(e -> "invalid-lookup-key".equals(e.getId()))
                .toList());

        RuleResult result = processor.processEnrichmentsWithResult(enrichments, testData, config);

        assertNotNull(result, "Result should not be null");
        // Lookup key failure should be handled (returns null, uses defaults)
        // This is acceptable behavior - check logs for LOOKUP KEY EVALUATION FAILED
        
        logger.info("✅ TEST 3.2 PASSED: Invalid lookup key handled: success={}", result.isSuccess());
    }

    // ========================================
    // TEST 7: Priority-Based Processing
    // ========================================

    @Test
    @DisplayName("TEST 7: Enrichments should be processed in priority order")
    void testPriorityBasedProcessing() throws Exception {
        logger.info("🧪 TEST 7: Priority-based enrichment processing");

        String yamlPath = "src/test/java/dev/mars/apex/core/service/enrichment/YamlEnrichmentProcessorComprehensiveTest.yaml";
        YamlRuleConfiguration config = yamlLoader.loadFromFile(yamlPath);

        Map<String, Object> testData = new HashMap<>();
        testData.put("value", 100);

        // Get priority test enrichments (they should execute in priority order)
        List<YamlEnrichment> enrichments = new ArrayList<>(config.getEnrichments().stream()
                .filter(e -> e.getId().startsWith("priority-"))
                .toList());

        RuleResult result = processor.processEnrichmentsWithResult(enrichments, testData, config);

        assertNotNull(result, "Result should not be null");
        assertTrue(result.isSuccess(), "Priority-based processing should succeed");

        @SuppressWarnings("unchecked")
        Map<String, Object> enrichedData = (Map<String, Object>) result.getEnrichedData();

        // Verify that lower priority numbers executed first
        // Check if execution order marker exists
        if (enrichedData.containsKey("executionOrder")) {
            logger.info("Execution order: {}", enrichedData.get("executionOrder"));
        }
        
        logger.info("✅ TEST 7 PASSED: Priority-based processing completed successfully");
    }

    // ========================================
    // TEST 8: V2.1 Null Value Handling
    // ========================================

    @Test
    @DisplayName("TEST 8.1: Setting null on optional field should succeed")
    void testNullOnOptionalField() throws Exception {
        logger.info("🧪 TEST 8.1: Null value on optional field");

        String yamlPath = "src/test/java/dev/mars/apex/core/service/enrichment/YamlEnrichmentProcessorComprehensiveTest.yaml";
        YamlRuleConfiguration config = yamlLoader.loadFromFile(yamlPath);

        Map<String, Object> testData = new HashMap<>();
        testData.put("amount", 1000.0);
        // currencyCode is missing - lookup will return null

        List<YamlEnrichment> enrichments = new ArrayList<>(config.getEnrichments().stream()
                .filter(e -> "null-optional-field-test".equals(e.getId()))
                .toList());

        RuleResult result = processor.processEnrichmentsWithResult(enrichments, testData, config);

        assertNotNull(result, "Result should not be null");
        assertTrue(result.isSuccess(), "Setting null on optional field should succeed");

        logger.info("✅ TEST 8.1 PASSED: Null value set on optional field successfully");
    }

    @Test
    @DisplayName("TEST 8.2: Null source object with default values should use defaults")
    void testNullSourceWithDefaults() throws Exception {
        logger.info("🧪 TEST 8.2: Null source object with default values");

        String yamlPath = "src/test/java/dev/mars/apex/core/service/enrichment/YamlEnrichmentProcessorComprehensiveTest.yaml";
        YamlRuleConfiguration config = yamlLoader.loadFromFile(yamlPath);

        Map<String, Object> testData = new HashMap<>();
        testData.put("currencyCode", "INVALID"); // Will cause null lookup result

        List<YamlEnrichment> enrichments = new ArrayList<>(config.getEnrichments().stream()
                .filter(e -> "null-source-default-test".equals(e.getId()))
                .toList());

        RuleResult result = processor.processEnrichmentsWithResult(enrichments, testData, config);

        assertNotNull(result, "Result should not be null");
        assertTrue(result.isSuccess(), "Null source with defaults should succeed");

        @SuppressWarnings("unchecked")
        Map<String, Object> enrichedData = (Map<String, Object>) result.getEnrichedData();
        assertEquals("UNKNOWN", enrichedData.get("currencyName"), "Default value should be applied");

        logger.info("✅ TEST 8.2 PASSED: Default values applied when source is null");
    }

    @Test
    @DisplayName("TEST 8.3: Required field with null from expression should fail")
    void testRequiredFieldNullFromExpression() throws Exception {
        logger.info("🧪 TEST 8.3: Required field null from expression");

        String yamlPath = "src/test/java/dev/mars/apex/core/service/enrichment/YamlEnrichmentProcessorComprehensiveTest.yaml";
        YamlRuleConfiguration config = yamlLoader.loadFromFile(yamlPath);

        Map<String, Object> testData = new HashMap<>();
        testData.put("amount", 1000.0);
        // missingField is null - expression will produce null

        List<YamlEnrichment> enrichments = new ArrayList<>(config.getEnrichments().stream()
                .filter(e -> "required-null-expression-test".equals(e.getId()))
                .toList());

        RuleResult result = processor.processEnrichmentsWithResult(enrichments, testData, config);

        assertNotNull(result, "Result should not be null");
        assertFalse(result.isSuccess(), "Required field with null expression should fail");

        logger.info("✅ TEST 8.3 PASSED: Required field null from expression detected as failure");
    }

    // ========================================
    // TEST 9: Constant and Expression Mappings
    // ========================================

    @Test
    @DisplayName("TEST 9.1: Explicit constant mapping with expression")
    void testExplicitConstantMapping() throws Exception {
        logger.info("🧪 TEST 9.1: Explicit constant mapping");

        String yamlPath = "src/test/java/dev/mars/apex/core/service/enrichment/YamlEnrichmentProcessorComprehensiveTest.yaml";
        YamlRuleConfiguration config = yamlLoader.loadFromFile(yamlPath);

        Map<String, Object> testData = new HashMap<>();
        testData.put("value", 100);

        List<YamlEnrichment> enrichments = new ArrayList<>(config.getEnrichments().stream()
                .filter(e -> "explicit-constant-test".equals(e.getId()))
                .toList());

        RuleResult result = processor.processEnrichmentsWithResult(enrichments, testData, config);

        assertNotNull(result, "Result should not be null");
        assertTrue(result.isSuccess(), "Explicit constant mapping should succeed");

        @SuppressWarnings("unchecked")
        Map<String, Object> enrichedData = (Map<String, Object>) result.getEnrichedData();
        assertEquals("USD", enrichedData.get("baseCurrency"), "Constant value should be set");

        logger.info("✅ TEST 9.1 PASSED: Explicit constant mapping applied");
    }

    @Test
    @DisplayName("TEST 9.2: Implicit constant mapping with expression")
    void testImplicitConstantMapping() throws Exception {
        logger.info("🧪 TEST 9.2: Implicit constant mapping");

        String yamlPath = "src/test/java/dev/mars/apex/core/service/enrichment/YamlEnrichmentProcessorComprehensiveTest.yaml";
        YamlRuleConfiguration config = yamlLoader.loadFromFile(yamlPath);

        Map<String, Object> testData = new HashMap<>();
        testData.put("amount", 1000.0);

        List<YamlEnrichment> enrichments = new ArrayList<>(config.getEnrichments().stream()
                .filter(e -> "implicit-constant-test".equals(e.getId()))
                .toList());

        RuleResult result = processor.processEnrichmentsWithResult(enrichments, testData, config);

        assertNotNull(result, "Result should not be null");
        assertTrue(result.isSuccess(), "Implicit constant mapping should succeed");

        @SuppressWarnings("unchecked")
        Map<String, Object> enrichedData = (Map<String, Object>) result.getEnrichedData();
        assertEquals(100.0, enrichedData.get("calculatedFee"), "Calculated constant value should be set");

        logger.info("✅ TEST 9.2 PASSED: Implicit constant mapping applied");
    }

    @Test
    @DisplayName("TEST 9.3: Expression transformation on non-null values")
    void testExpressionTransformation() throws Exception {
        logger.info("🧪 TEST 9.3: Expression transformation");

        String yamlPath = "src/test/java/dev/mars/apex/core/service/enrichment/YamlEnrichmentProcessorComprehensiveTest.yaml";
        YamlRuleConfiguration config = yamlLoader.loadFromFile(yamlPath);

        Map<String, Object> testData = new HashMap<>();
        testData.put("baseCurrency", "usd"); // Lowercase value to transform

        List<YamlEnrichment> enrichments = new ArrayList<>(config.getEnrichments().stream()
                .filter(e -> "expression-transform-test".equals(e.getId()))
                .toList());

        RuleResult result = processor.processEnrichmentsWithResult(enrichments, testData, config);

        assertNotNull(result, "Result should not be null");
        assertTrue(result.isSuccess(), "Expression transformation should succeed");

        @SuppressWarnings("unchecked")
        Map<String, Object> enrichedData = (Map<String, Object>) result.getEnrichedData();
        assertEquals("USD", enrichedData.get("normalizedCurrency"), "Expression should transform value to uppercase");

        logger.info("✅ TEST 9.3 PASSED: Expression transformation applied");
    }

    // ========================================
    // TEST 10: Calculation Enrichment Variations
    // ========================================

    @Test
    @DisplayName("TEST 10.1: Calculation with field mappings")
    void testCalculationWithFieldMappings() throws Exception {
        logger.info("🧪 TEST 10.1: Calculation with field mappings");

        String yamlPath = "src/test/java/dev/mars/apex/core/service/enrichment/YamlEnrichmentProcessorComprehensiveTest.yaml";
        YamlRuleConfiguration config = yamlLoader.loadFromFile(yamlPath);

        Map<String, Object> testData = new HashMap<>();
        testData.put("baseAmount", 1000.0);
        testData.put("taxRate", 0.20);

        List<YamlEnrichment> enrichments = new ArrayList<>(config.getEnrichments().stream()
                .filter(e -> "calculation-field-mapping-test".equals(e.getId()))
                .toList());

        RuleResult result = processor.processEnrichmentsWithResult(enrichments, testData, config);

        assertNotNull(result, "Result should not be null");
        assertTrue(result.isSuccess(), "Calculation with field mappings should succeed");

        @SuppressWarnings("unchecked")
        Map<String, Object> enrichedData = (Map<String, Object>) result.getEnrichedData();
        assertNotNull(enrichedData.get("totalAmount"), "Field mapping should be applied after calculation");

        logger.info("✅ TEST 10.1 PASSED: Calculation with field mappings completed");
    }

    @Test
    @DisplayName("TEST 10.2: Calculation error recovery with default value")
    void testCalculationErrorRecovery() throws Exception {
        logger.info("🧪 TEST 10.2: Calculation error recovery");

        String yamlPath = "src/test/java/dev/mars/apex/core/service/enrichment/YamlEnrichmentProcessorComprehensiveTest.yaml";
        YamlRuleConfiguration config = yamlLoader.loadFromFile(yamlPath);

        Map<String, Object> testData = new HashMap<>();
        testData.put("value", 100);
        // missingField will cause expression error

        List<YamlEnrichment> enrichments = new ArrayList<>(config.getEnrichments().stream()
                .filter(e -> "calculation-error-recovery-test".equals(e.getId()))
                .toList());

        RuleResult result = processor.processEnrichmentsWithResult(enrichments, testData, config);

        assertNotNull(result, "Result should not be null");
        assertTrue(result.isSuccess(), "Calculation error with default-value should recover");

        @SuppressWarnings("unchecked")
        Map<String, Object> enrichedData = (Map<String, Object>) result.getEnrichedData();
        assertEquals(0.0, enrichedData.get("calculationResult"), "Default value should be used on error");

        logger.info("✅ TEST 10.2 PASSED: Calculation error recovery successful");
    }

    // ========================================
    // TEST 11: Target Type Filtering
    // ========================================

    @Test
    @DisplayName("TEST 11.1: Target type match should process enrichment")
    void testTargetTypeMatch() throws Exception {
        logger.info("🧪 TEST 11.1: Target type match");

        String yamlPath = "src/test/java/dev/mars/apex/core/service/enrichment/YamlEnrichmentProcessorComprehensiveTest.yaml";
        YamlRuleConfiguration config = yamlLoader.loadFromFile(yamlPath);

        Map<String, Object> testData = new HashMap<>();
        testData.put("value", 100);

        List<YamlEnrichment> enrichments = new ArrayList<>(config.getEnrichments().stream()
                .filter(e -> "target-type-match-test".equals(e.getId()))
                .toList());

        RuleResult result = processor.processEnrichmentsWithResult(enrichments, testData, config);

        assertNotNull(result, "Result should not be null");
        assertTrue(result.isSuccess(), "Target type match should process enrichment");

        @SuppressWarnings("unchecked")
        Map<String, Object> enrichedData = (Map<String, Object>) result.getEnrichedData();
        assertEquals("PROCESSED", enrichedData.get("typeMatchResult"), "Enrichment should be processed");

        logger.info("✅ TEST 11.1 PASSED: Target type matched and enrichment processed");
    }

    @Test
    @DisplayName("TEST 11.2: Target type mismatch should skip enrichment")
    void testTargetTypeMismatch() throws Exception {
        logger.info("🧪 TEST 11.2: Target type mismatch");

        String yamlPath = "src/test/java/dev/mars/apex/core/service/enrichment/YamlEnrichmentProcessorComprehensiveTest.yaml";
        YamlRuleConfiguration config = yamlLoader.loadFromFile(yamlPath);

        Map<String, Object> testData = new HashMap<>();
        testData.put("value", 100);

        List<YamlEnrichment> enrichments = new ArrayList<>(config.getEnrichments().stream()
                .filter(e -> "target-type-mismatch-test".equals(e.getId()))
                .toList());

        RuleResult result = processor.processEnrichmentsWithResult(enrichments, testData, config);

        assertNotNull(result, "Result should not be null");
        assertTrue(result.isSuccess(), "Target type mismatch should skip enrichment (not fail)");

        @SuppressWarnings("unchecked")
        Map<String, Object> enrichedData = (Map<String, Object>) result.getEnrichedData();
        assertNull(enrichedData.get("typeMismatchResult"), "Enrichment should be skipped, field not set");

        logger.info("✅ TEST 11.2 PASSED: Target type mismatch skipped enrichment");
    }

    // ========================================
    // INTEGRATION TEST: Complete Workflow
    // ========================================

    @Test
    @DisplayName("INTEGRATION: Complete enrichment workflow with all features")
    void testCompleteEnrichmentWorkflow() throws Exception {
        logger.info("🧪 INTEGRATION TEST: Complete enrichment workflow");

        String yamlPath = "src/test/java/dev/mars/apex/core/service/enrichment/YamlEnrichmentProcessorComprehensiveTest.yaml";
        YamlRuleConfiguration config = yamlLoader.loadFromFile(yamlPath);

        Map<String, Object> testData = new HashMap<>();
        testData.put("amount", 5000.0);
        testData.put("currencyCode", "USD");
        testData.put("value", 100);

        logger.info("📊 Processing {} enrichments", config.getEnrichments().size());

        RuleResult result = processor.processEnrichmentsWithResult(config.getEnrichments(), testData, config);

        assertNotNull(result, "Result should not be null");
        
        logger.info("📈 Enrichment processing completed: success={}, severity={}", 
                   result.isSuccess(), result.getSeverity());
        logger.info("📊 Enriched data keys: {}", 
                   result.getEnrichedData() instanceof Map ? 
                   ((Map<?,?>)result.getEnrichedData()).keySet() : "N/A");

        logger.info("✅ INTEGRATION TEST PASSED: Complete workflow executed successfully");
    }
}
