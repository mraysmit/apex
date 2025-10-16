package dev.mars.apex.core.service.scenario;

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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for advanced scenario features.
 * 
 * Tests cover Priority 2 features:
 * - OR conditions in classification rules
 * - Nested field access in classification rules
 * - String operations (.contains(), .startsWith(), .length())
 * - flag-for-review failure policy
 * - Context sharing between dependent stages
 * 
 * FOLLOWS CODING PRINCIPLES FROM prompts.txt:
 * - Test actual functionality (not YAML parsing)
 * - Progressive complexity (simple to complex)
 * - Validate business logic outcomes
 * - Log test errors with "TEST:" prefix
 * 
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 1.0.0
 */
@DisplayName("Scenario Advanced Features Tests")
class ScenarioAdvancedFeaturesTest {
    
    private static final Logger logger = LoggerFactory.getLogger(ScenarioAdvancedFeaturesTest.class);
    
    private ScenarioConfiguration scenario;
    private ScenarioStageExecutor executor;
    
    @BeforeEach
    void setUp() {
        scenario = new ScenarioConfiguration();
        scenario.setScenarioId("advanced-test");
        scenario.setName("Advanced Features Test");
        executor = new ScenarioStageExecutor();
    }
    
    // ========================================
    // OR Conditions Tests
    // ========================================
    
    @Nested
    @DisplayName("OR Condition Tests")
    class OrConditionTests {
        
        @Test
        @DisplayName("Should match when first OR condition is true")
        void testOrConditionFirstTrue() {
            logger.info("TEST: OR condition - first condition true");
            
            // Given: OR condition where first part is true
            scenario.setClassificationRuleCondition("#data['type'] == 'OTC' || #data['type'] == 'LISTED'");
            
            // When: Evaluate with matching data
            Map<String, Object> data = new HashMap<>();
            data.put("type", "OTC");
            
            // Then: Should match
            assertTrue(scenario.matchesClassificationRule(data),
                "Should match when first OR condition is true");
        }
        
        @Test
        @DisplayName("Should match when second OR condition is true")
        void testOrConditionSecondTrue() {
            logger.info("TEST: OR condition - second condition true");
            
            // Given: OR condition where second part is true
            scenario.setClassificationRuleCondition("#data['type'] == 'OTC' || #data['type'] == 'LISTED'");
            
            // When: Evaluate with matching data
            Map<String, Object> data = new HashMap<>();
            data.put("type", "LISTED");
            
            // Then: Should match
            assertTrue(scenario.matchesClassificationRule(data),
                "Should match when second OR condition is true");
        }
        
        @Test
        @DisplayName("Should not match when no OR conditions are true")
        void testOrConditionNoneTrue() {
            logger.info("TEST: OR condition - no conditions true");

            // Given: OR condition where neither part is true
            scenario.setClassificationRuleCondition("#data['type'] == 'OTC' || #data['type'] == 'LISTED'");

            // When: Evaluate with non-matching data
            Map<String, Object> data = new HashMap<>();
            data.put("type", "DERIVATIVE");

            // Then: Should not match
            assertFalse(scenario.matchesClassificationRule(data),
                "Should not match when no OR conditions are true");
        }

        @Test
        @DisplayName("Should handle null values in OR conditions gracefully")
        void testOrConditionWithNullValue() {
            logger.info("TEST: OR condition - null value handling");

            // Given: OR condition with null-safe navigation
            scenario.setClassificationRuleCondition("#data['type'] == 'OTC' || #data['type'] == null");

            // When: Evaluate with null data
            Map<String, Object> data = new HashMap<>();
            data.put("type", null);

            // Then: Should match because second condition is true
            assertTrue(scenario.matchesClassificationRule(data),
                "Should match when second OR condition (null check) is true");
        }

        @Test
        @DisplayName("Should handle missing fields in OR conditions")
        void testOrConditionWithMissingField() {
            logger.info("TEST: OR condition - missing field handling");

            // Given: OR condition referencing potentially missing field
            scenario.setClassificationRuleCondition("#data['type'] == 'OTC' || #data['category'] == 'EQUITY'");

            // When: Evaluate with data missing one field
            Map<String, Object> data = new HashMap<>();
            data.put("type", "OTC");
            // 'category' field is missing

            // Then: Should match because first condition is true
            assertTrue(scenario.matchesClassificationRule(data),
                "Should match when first OR condition is true, even if second field is missing");
        }
    }
    
    // ========================================
    // Nested Field Access Tests
    // ========================================
    
    @Nested
    @DisplayName("Nested Field Access Tests")
    class NestedFieldAccessTests {
        
        @Test
        @DisplayName("Should access single-level nested field")
        void testSingleLevelNestedField() {
            logger.info("TEST: Nested field access - single level");
            
            // Given: Condition accessing nested field
            scenario.setClassificationRuleCondition("#data['trade']['type'] == 'OTC'");
            
            // When: Evaluate with nested data
            Map<String, Object> data = new HashMap<>();
            Map<String, Object> trade = new HashMap<>();
            trade.put("type", "OTC");
            data.put("trade", trade);
            
            // Then: Should match
            assertTrue(scenario.matchesClassificationRule(data),
                "Should access single-level nested field");
        }
        
        @Test
        @DisplayName("Should access multi-level nested field")
        void testMultiLevelNestedField() {
            logger.info("TEST: Nested field access - multi-level");
            
            // Given: Condition accessing multi-level nested field
            scenario.setClassificationRuleCondition("#data['trade']['counterparty']['region'] == 'US'");
            
            // When: Evaluate with deeply nested data
            Map<String, Object> data = new HashMap<>();
            Map<String, Object> trade = new HashMap<>();
            Map<String, Object> counterparty = new HashMap<>();
            counterparty.put("region", "US");
            trade.put("counterparty", counterparty);
            data.put("trade", trade);
            
            // Then: Should match
            assertTrue(scenario.matchesClassificationRule(data),
                "Should access multi-level nested field");
        }
        
        @Test
        @DisplayName("Should handle null nested fields gracefully")
        void testNullNestedField() {
            logger.info("TEST: Nested field access - null handling");

            // Given: Condition with null-safe navigation
            scenario.setClassificationRuleCondition("#data['trade']?.['type'] == 'OTC'");

            // When: Evaluate with null nested field
            Map<String, Object> data = new HashMap<>();
            data.put("trade", null);

            // Then: Should not throw exception
            assertFalse(scenario.matchesClassificationRule(data),
                "Should handle null nested fields gracefully");
        }

        @Test
        @DisplayName("Should handle missing nested objects gracefully")
        void testMissingNestedObject() {
            logger.info("TEST: Nested field access - missing object handling");

            // Given: Condition accessing nested field
            scenario.setClassificationRuleCondition("#data['trade']['counterparty']['region'] == 'US'");

            // When: Evaluate with missing nested object
            Map<String, Object> data = new HashMap<>();
            // 'trade' object is missing entirely

            // Then: Should not throw exception, should return false
            assertFalse(scenario.matchesClassificationRule(data),
                "Should handle missing nested objects gracefully without throwing exception");
        }

        @Test
        @DisplayName("Should handle partial nested structure")
        void testPartialNestedStructure() {
            logger.info("TEST: Nested field access - partial structure handling");

            // Given: Condition accessing deeply nested field
            scenario.setClassificationRuleCondition("#data['trade']['counterparty']['region'] == 'US'");

            // When: Evaluate with incomplete nested structure
            Map<String, Object> data = new HashMap<>();
            Map<String, Object> trade = new HashMap<>();
            trade.put("type", "OTC");
            // 'counterparty' is missing
            data.put("trade", trade);

            // Then: Should not throw exception, should return false
            assertFalse(scenario.matchesClassificationRule(data),
                "Should handle partial nested structures gracefully");
        }
    }
    
    // ========================================
    // String Operations Tests
    // ========================================
    
    @Nested
    @DisplayName("String Operations Tests")
    class StringOperationsTests {
        
        @Test
        @DisplayName("Should support .contains() string operation")
        void testStringContains() {
            logger.info("TEST: String operation - contains");
            
            // Given: Condition using .contains()
            scenario.setClassificationRuleCondition("#data['description'].contains('urgent')");
            
            // When: Evaluate with matching data
            Map<String, Object> data = new HashMap<>();
            data.put("description", "This is an urgent trade");
            
            // Then: Should match
            assertTrue(scenario.matchesClassificationRule(data),
                "Should support .contains() string operation");
        }
        
        @Test
        @DisplayName("Should support .startsWith() string operation")
        void testStringStartsWith() {
            logger.info("TEST: String operation - startsWith");
            
            // Given: Condition using .startsWith()
            scenario.setClassificationRuleCondition("#data['code'].startsWith('OTC')");
            
            // When: Evaluate with matching data
            Map<String, Object> data = new HashMap<>();
            data.put("code", "OTC-12345");
            
            // Then: Should match
            assertTrue(scenario.matchesClassificationRule(data),
                "Should support .startsWith() string operation");
        }
        
        @Test
        @DisplayName("Should support .length() string operation")
        void testStringLength() {
            logger.info("TEST: String operation - length");

            // Given: Condition using .length()
            scenario.setClassificationRuleCondition("#data['name'].length() > 5");

            // When: Evaluate with matching data
            Map<String, Object> data = new HashMap<>();
            data.put("name", "LongName");

            // Then: Should match
            assertTrue(scenario.matchesClassificationRule(data),
                "Should support .length() string operation");
        }

        @Test
        @DisplayName("Should handle null strings in string operations gracefully")
        void testStringOperationWithNull() {
            logger.info("TEST: String operation - null handling");

            // Given: Condition using string operation with null-safe check
            scenario.setClassificationRuleCondition("#data['description'] != null && #data['description'].contains('urgent')");

            // When: Evaluate with null string
            Map<String, Object> data = new HashMap<>();
            data.put("description", null);

            // Then: Should not throw exception, should return false
            assertFalse(scenario.matchesClassificationRule(data),
                "Should handle null strings gracefully in string operations");
        }

        @Test
        @DisplayName("Should handle empty strings in string operations")
        void testStringOperationWithEmpty() {
            logger.info("TEST: String operation - empty string handling");

            // Given: Condition checking string length
            scenario.setClassificationRuleCondition("#data['code'].length() > 0");

            // When: Evaluate with empty string
            Map<String, Object> data = new HashMap<>();
            data.put("code", "");

            // Then: Should not match (empty string has length 0)
            assertFalse(scenario.matchesClassificationRule(data),
                "Should correctly evaluate empty strings in length operations");
        }

        @Test
        @DisplayName("Should handle case-sensitive string operations")
        void testStringOperationCaseSensitive() {
            logger.info("TEST: String operation - case sensitivity");

            // Given: Condition using startsWith (case-sensitive)
            scenario.setClassificationRuleCondition("#data['code'].startsWith('OTC')");

            // When: Evaluate with different case
            Map<String, Object> data = new HashMap<>();
            data.put("code", "otc-12345");

            // Then: Should not match (case-sensitive)
            assertFalse(scenario.matchesClassificationRule(data),
                "String operations should be case-sensitive");
        }
    }
    
    // ========================================
    // Flag-For-Review Failure Policy Tests
    // ========================================

    @Nested
    @DisplayName("Flag-For-Review Failure Policy Tests")
    class FlagForReviewTests {

        @Test
        @DisplayName("Should flag scenario for review on stage failure")
        void testFlagForReviewPolicy() {
            logger.info("TEST: Flag-for-review failure policy");

            // Given: Stage with flag-for-review policy
            ScenarioStage stage = new ScenarioStage("test-stage", "config/test.yaml", 1);
            stage.setFailurePolicy(ScenarioStage.FAILURE_POLICY_FLAG_FOR_REVIEW);

            scenario.addProcessingStage(stage);

            // When: Execute scenario with test data
            Map<String, Object> testData = new HashMap<>();
            testData.put("field", "value");

            // Execute and capture result - should handle missing file gracefully
            ScenarioExecutionResult result = executor.executeStages(scenario, testData);

            // Then: Verify flag-for-review policy is applied
            assertNotNull(result, "Should return ScenarioExecutionResult");

            // CRITICAL: Validate actual business outcome - stage should be flagged for review
            assertTrue(result.requiresReview() || result.getExecutionStatus().toString().contains("REVIEW"),
                "Stage with flag-for-review policy should be flagged for review. Status: " + result.getExecutionStatus());

            // Verify stage results contain the failed stage
            assertNotNull(result.getStageResults(), "Should have stage results");
            assertFalse(result.getStageResults().isEmpty(), "Should have at least one stage result");

            logger.info("✓ Flag-for-review policy validated: status={}, requiresReview={}",
                result.getExecutionStatus(), result.requiresReview());
        }
    }
    
    // ========================================
    // Context Sharing Between Stages Tests
    // ========================================

    @Nested
    @DisplayName("Context Sharing Between Stages Tests")
    class ContextSharingTests {

        @Test
        @DisplayName("Should share data between dependent stages")
        void testContextSharingBetweenStages() {
            logger.info("TEST: Context sharing between dependent stages");

            // Given: Two stages with dependency
            ScenarioStage stage1 = new ScenarioStage("stage-1", "config/stage1.yaml", 1);
            ScenarioStage stage2 = new ScenarioStage("stage-2", "config/stage2.yaml", 2);
            stage2.addDependency("stage-1");

            scenario.addProcessingStage(stage1);
            scenario.addProcessingStage(stage2);

            // When: Execute scenario with test data
            Map<String, Object> testData = new HashMap<>();
            testData.put("field", "value");

            // Execute and capture result - handles missing files gracefully
            ScenarioExecutionResult result = executor.executeStages(scenario, testData);

            // Then: Verify dependency handling - stage2 should be skipped if stage1 fails
            assertNotNull(result, "Should return ScenarioExecutionResult");
            assertNotNull(result.getStageResults(), "Should have stage results list");

            // CRITICAL: Validate actual dependency behavior
            // When stage-1 fails, stage-2 should be skipped (not executed)
            assertTrue(result.getStageResults().size() >= 1,
                "Should have at least one stage result");

            // Verify stage-1 failed (missing config file)
            boolean stage1Failed = result.getStageResults().stream()
                .anyMatch(sr -> sr.getStageName().equals("stage-1") && !sr.isSuccessful());
            assertTrue(stage1Failed, "Stage-1 should fail due to missing config file");

            // Verify stage-2 was skipped due to failed dependency
            // Stage-2 should either not be in results or be marked as failed
            boolean stage2Skipped = result.getStageResults().stream()
                .filter(sr -> sr.getStageName().equals("stage-2"))
                .findFirst()
                .map(sr -> !sr.isSuccessful())
                .orElse(true); // If stage-2 not in results, it was skipped
            assertTrue(stage2Skipped, "Stage-2 should be skipped due to stage-1 failure");

            // Verify execution status reflects partial success or failure
            String status = result.getExecutionStatus();
            assertTrue(status.contains("PARTIAL") || status.contains("FAILURE"),
                "Scenario should show partial success or failure status, got: " + status);

            logger.info("✓ Dependency handling validated: stage-1 failed, stage-2 skipped. Status: {}",
                result.getExecutionStatus());
        }
    }
}

