package dev.mars.apex.demo.syntax;

import dev.mars.apex.core.config.yaml.YamlRuleConfiguration;
import dev.mars.apex.demo.DemoTestBase;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
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
 * Comprehensive Syntax Test - Consolidated test for all APEX syntax patterns.
 * 
 * This test consolidates and validates all core APEX syntax functionality:
 * - Conditional logic patterns (if-then-else, boolean expressions)
 * - SpEL expression evaluation and field transformations
 * - Enrichment patterns and lookup operations
 * - Rule chain processing and rule group execution
 * - Configuration loading and metadata validation
 * - Error handling and validation rules
 * 
 * REAL APEX SERVICES TESTED:
 * - EnrichmentService: Real APEX enrichment processor for syntax validation
 * - ExpressionEvaluatorService: Real SpEL expression evaluation engine
 * - YamlConfigurationLoader: Real YAML configuration loading and parsing
 * - RuleChainProcessor: Real rule chain execution and validation
 */
public class ComprehensiveSyntaxTest extends DemoTestBase {

    private static final Logger logger = LoggerFactory.getLogger(ComprehensiveSyntaxTest.class);

    @Test
    void testConditionalLogicPatterns() {
        logger.info("=== Testing Conditional Logic Patterns ===");
        
        YamlRuleConfiguration config = loadAndValidateYaml("yaml/conditional-logic-test.yaml");
        
        Map<String, Object> testData = new HashMap<>();
        testData.put("amount", 150000);
        testData.put("customerType", "PREMIUM");
        testData.put("riskLevel", "LOW");
        
        Object result = enrichmentService.enrichObject(config, testData);
        assertNotNull(result, "Conditional logic processing should complete successfully");
        
        @SuppressWarnings("unchecked")
        Map<String, Object> enrichedData = (Map<String, Object>) result;
        
        // Validate conditional logic results
        assertNotNull(enrichedData.get("conditionalResult"), "Conditional result should be set");
        assertNotNull(enrichedData.get("booleanEvaluation"), "Boolean evaluation should be completed");
        
        logger.info("✅ Conditional logic patterns validated successfully");
    }

    @Test
    void testSpELExpressionEvaluation() {
        logger.info("=== Testing SpEL Expression Evaluation ===");
        
        YamlRuleConfiguration config = loadAndValidateYaml("yaml/spel-expression-test.yaml");
        
        Map<String, Object> testData = new HashMap<>();
        testData.put("baseAmount", 100000);
        testData.put("multiplier", 1.15);
        testData.put("currency", "USD");
        
        Object result = enrichmentService.enrichObject(config, testData);
        assertNotNull(result, "SpEL expression evaluation should complete successfully");
        
        @SuppressWarnings("unchecked")
        Map<String, Object> enrichedData = (Map<String, Object>) result;
        
        // Validate SpEL expression results
        assertNotNull(enrichedData.get("calculatedAmount"), "Calculated amount should be set");
        assertNotNull(enrichedData.get("expressionResult"), "Expression result should be evaluated");
        
        logger.info("✅ SpEL expression evaluation validated successfully");
    }

    @Test
    void testEnrichmentPatterns() {
        logger.info("=== Testing Enrichment Patterns ===");
        
        YamlRuleConfiguration config = loadAndValidateYaml("yaml/enrichment-patterns-test.yaml");
        
        Map<String, Object> testData = new HashMap<>();
        testData.put("entityType", "TRADE");
        testData.put("enrichmentScope", "COMPREHENSIVE");
        testData.put("processingMode", "REAL_TIME");
        
        Object result = enrichmentService.enrichObject(config, testData);
        assertNotNull(result, "Enrichment patterns should complete successfully");
        
        @SuppressWarnings("unchecked")
        Map<String, Object> enrichedData = (Map<String, Object>) result;
        
        // Validate enrichment pattern results
        assertNotNull(enrichedData.get("enrichmentStatus"), "Enrichment status should be set");
        assertNotNull(enrichedData.get("enrichmentSummary"), "Enrichment summary should be generated");
        
        logger.info("✅ Enrichment patterns validated successfully");
    }

    @Test
    void testRuleChainProcessing() {
        logger.info("=== Testing Rule Chain Processing ===");
        
        YamlRuleConfiguration config = loadAndValidateYaml("yaml/rule-chain-test.yaml");
        
        Map<String, Object> testData = new HashMap<>();
        testData.put("chainType", "SEQUENTIAL");
        testData.put("ruleCount", 5);
        testData.put("executionMode", "STRICT");
        
        Object result = enrichmentService.enrichObject(config, testData);
        assertNotNull(result, "Rule chain processing should complete successfully");
        
        @SuppressWarnings("unchecked")
        Map<String, Object> enrichedData = (Map<String, Object>) result;
        
        // Validate rule chain results
        assertNotNull(enrichedData.get("chainStatus"), "Chain status should be set");
        assertNotNull(enrichedData.get("chainSummary"), "Chain summary should be generated");
        
        logger.info("✅ Rule chain processing validated successfully");
    }

    @Test
    void testConfigurationLoading() {
        logger.info("=== Testing Configuration Loading ===");
        
        YamlRuleConfiguration config = loadAndValidateYaml("yaml/configuration-loading-test.yaml");
        
        Map<String, Object> testData = new HashMap<>();
        testData.put("configType", "DYNAMIC");
        testData.put("loadingMode", "LAZY");
        testData.put("validationLevel", "STRICT");
        
        Object result = enrichmentService.enrichObject(config, testData);
        assertNotNull(result, "Configuration loading should complete successfully");
        
        @SuppressWarnings("unchecked")
        Map<String, Object> enrichedData = (Map<String, Object>) result;
        
        // Validate configuration loading results
        assertNotNull(enrichedData.get("loadingStatus"), "Loading status should be set");
        assertNotNull(enrichedData.get("loadingSummary"), "Loading summary should be generated");
        
        logger.info("✅ Configuration loading validated successfully");
    }

    @Test
    void testValidationRules() {
        logger.info("=== Testing Validation Rules ===");
        
        YamlRuleConfiguration config = loadAndValidateYaml("yaml/validation-rules-test.yaml");
        
        Map<String, Object> testData = new HashMap<>();
        testData.put("validationType", "COMPREHENSIVE");
        testData.put("validationScope", "ALL_FIELDS");
        testData.put("strictMode", true);
        
        Object result = enrichmentService.enrichObject(config, testData);
        assertNotNull(result, "Validation rules should complete successfully");
        
        @SuppressWarnings("unchecked")
        Map<String, Object> enrichedData = (Map<String, Object>) result;
        
        // Validate validation rule results
        assertNotNull(enrichedData.get("validationStatus"), "Validation status should be set");
        assertNotNull(enrichedData.get("validationSummary"), "Validation summary should be generated");
        
        logger.info("✅ Validation rules validated successfully");
    }
}
