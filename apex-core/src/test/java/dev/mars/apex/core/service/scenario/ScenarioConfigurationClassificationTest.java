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
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ScenarioConfiguration classification rule functionality.
 * 
 * Tests cover:
 * - Classification rule evaluation with SpEL expressions
 * - Simple single-field classification
 * - Multiple field AND conditions
 * - Numeric comparisons
 * - Validation requirements
 * 
 * FOLLOWS CODING PRINCIPLES FROM prompts.txt:
 * - Test actual functionality, not just configuration parsing
 * - Use real SpEL evaluation against Map data
 * - Progressive complexity (simple to complex)
 * - Validate business logic outcomes
 * 
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 1.0.0
 */
@DisplayName("ScenarioConfiguration Classification Rule Tests")
class ScenarioConfigurationClassificationTest {

    private ScenarioConfiguration scenario;

    @BeforeEach
    void setUp() {
        scenario = new ScenarioConfiguration();
        scenario.setScenarioId("test-scenario");
        scenario.setName("Test Scenario");
    }

    // ========================================
    // Level 1: Simple Single-Field Classification
    // ========================================

    @Test
    @DisplayName("Should match simple single-field classification rule")
    void testSimpleSingleFieldClassification() {
        // Given: Scenario with simple classification rule
        scenario.setClassificationRuleCondition("#data['tradeType'] == 'OTCOption'");
        scenario.setClassificationRuleDescription("Matches OTC option trades");

        // When: Evaluate against matching data
        Map<String, Object> matchingData = new HashMap<>();
        matchingData.put("tradeType", "OTCOption");

        // Then: Should match
        assertTrue(scenario.matchesClassificationRule(matchingData),
            "Should match when tradeType is OTCOption");
    }

    @Test
    @DisplayName("Should not match when field value differs")
    void testSimpleSingleFieldNoMatch() {
        // Given: Scenario with simple classification rule
        scenario.setClassificationRuleCondition("#data['tradeType'] == 'OTCOption'");

        // When: Evaluate against non-matching data
        Map<String, Object> nonMatchingData = new HashMap<>();
        nonMatchingData.put("tradeType", "Swap");

        // Then: Should not match
        assertFalse(scenario.matchesClassificationRule(nonMatchingData),
            "Should not match when tradeType is Swap");
    }

    @Test
    @DisplayName("Should not match when field is missing")
    void testSimpleSingleFieldMissingField() {
        // Given: Scenario with simple classification rule
        scenario.setClassificationRuleCondition("#data['tradeType'] == 'OTCOption'");

        // When: Evaluate against data without the field
        Map<String, Object> dataWithoutField = new HashMap<>();
        dataWithoutField.put("otherField", "value");

        // Then: Should not match (gracefully handle missing field)
        assertFalse(scenario.matchesClassificationRule(dataWithoutField),
            "Should not match when required field is missing");
    }

    // ========================================
    // Level 2: Multiple Field AND Conditions
    // ========================================

    @Test
    @DisplayName("Should match multiple field AND conditions")
    void testMultipleFieldAndConditions() {
        // Given: Scenario with AND condition
        scenario.setClassificationRuleCondition(
            "#data['tradeType'] == 'OTCOption' && #data['region'] == 'US'");

        // When: Evaluate against matching data
        Map<String, Object> matchingData = new HashMap<>();
        matchingData.put("tradeType", "OTCOption");
        matchingData.put("region", "US");

        // Then: Should match
        assertTrue(scenario.matchesClassificationRule(matchingData),
            "Should match when both tradeType is OTCOption AND region is US");
    }

    @Test
    @DisplayName("Should not match when one AND condition fails")
    void testMultipleFieldAndConditionsPartialMatch() {
        // Given: Scenario with AND condition
        scenario.setClassificationRuleCondition(
            "#data['tradeType'] == 'OTCOption' && #data['region'] == 'US'");

        // When: Evaluate against partially matching data
        Map<String, Object> partialData = new HashMap<>();
        partialData.put("tradeType", "OTCOption");
        partialData.put("region", "EU");  // Wrong region

        // Then: Should not match
        assertFalse(scenario.matchesClassificationRule(partialData),
            "Should not match when region is EU instead of US");
    }

    // ========================================
    // Level 3: Numeric Comparisons
    // ========================================

    @Test
    @DisplayName("Should match numeric comparison")
    void testNumericComparison() {
        // Given: Scenario with numeric condition
        scenario.setClassificationRuleCondition(
            "#data['tradeType'] == 'OTCOption' && #data['notional'] > 100000000");

        // When: Evaluate against high-value trade
        Map<String, Object> highValueTrade = new HashMap<>();
        highValueTrade.put("tradeType", "OTCOption");
        highValueTrade.put("notional", 150000000);

        // Then: Should match
        assertTrue(scenario.matchesClassificationRule(highValueTrade),
            "Should match when notional is greater than 100 million");
    }

    @Test
    @DisplayName("Should not match when numeric threshold not met")
    void testNumericComparisonBelowThreshold() {
        // Given: Scenario with numeric condition
        scenario.setClassificationRuleCondition(
            "#data['tradeType'] == 'OTCOption' && #data['notional'] > 100000000");

        // When: Evaluate against low-value trade
        Map<String, Object> lowValueTrade = new HashMap<>();
        lowValueTrade.put("tradeType", "OTCOption");
        lowValueTrade.put("notional", 50000000);

        // Then: Should not match
        assertFalse(scenario.matchesClassificationRule(lowValueTrade),
            "Should not match when notional is below 100 million");
    }

    // ========================================
    // Level 4: Validation and Edge Cases
    // ========================================

    @Test
    @DisplayName("Should return false when no classification rule defined")
    void testNoClassificationRule() {
        // Given: Scenario without classification rule
        // (no setClassificationRuleCondition called)

        // When: Evaluate against any data
        Map<String, Object> data = new HashMap<>();
        data.put("tradeType", "OTCOption");

        // Then: Should return false
        assertFalse(scenario.matchesClassificationRule(data),
            "Should return false when no classification rule is defined");
    }

    @Test
    @DisplayName("Should detect when classification rule exists")
    void testHasClassificationRule() {
        // Given: Scenario without classification rule
        assertFalse(scenario.hasClassificationRule(),
            "Should return false when no classification rule");

        // When: Set classification rule
        scenario.setClassificationRuleCondition("#data['tradeType'] == 'OTCOption'");

        // Then: Should detect it exists
        assertTrue(scenario.hasClassificationRule(),
            "Should return true when classification rule is set");
    }

    @Test
    @DisplayName("Should validate scenario with classification rule")
    void testValidateWithClassificationRule() {
        // Given: Scenario with classification rule (no data-types)
        scenario.setClassificationRuleCondition("#data['tradeType'] == 'OTCOption'");

        // When/Then: Should pass validation
        assertDoesNotThrow(() -> scenario.validate(),
            "Should pass validation when classification rule is present");
    }

    @Test
    @DisplayName("Should validate scenario with data-types (backward compatibility)")
    void testValidateWithDataTypes() {
        // Given: Scenario with data-types (no classification rule)
        scenario.setDataTypes(Arrays.asList("OTCOption", "java.util.Map"));

        // When/Then: Should pass validation
        assertDoesNotThrow(() -> scenario.validate(),
            "Should pass validation when data-types are present (backward compatibility)");
    }

    @Test
    @DisplayName("Should fail validation when neither classification rule nor data-types")
    void testValidateWithoutClassificationOrDataTypes() {
        // Given: Scenario without classification rule AND without data-types
        // (no setClassificationRuleCondition and no setDataTypes called)

        // When/Then: Should fail validation
        IllegalStateException exception = assertThrows(IllegalStateException.class,
            () -> scenario.validate(),
            "Should throw exception when neither classification rule nor data-types are present");

        assertTrue(exception.getMessage().contains("must have either 'classification-rule' or 'data-types'"),
            "Exception message should explain the requirement");
    }

    @Test
    @DisplayName("Should handle empty classification rule condition")
    void testEmptyClassificationRuleCondition() {
        // Given: Scenario with empty classification rule
        scenario.setClassificationRuleCondition("");

        // When: Check if has classification rule
        // Then: Should return false for empty string
        assertFalse(scenario.hasClassificationRule(),
            "Should return false for empty classification rule condition");
    }

    @Test
    @DisplayName("Should handle null data gracefully")
    void testNullDataHandling() {
        // Given: Scenario with classification rule
        scenario.setClassificationRuleCondition("#data['tradeType'] == 'OTCOption'");

        // When: Evaluate against null data
        // Then: Should not throw exception, should return false
        assertFalse(scenario.matchesClassificationRule(null),
            "Should handle null data gracefully and return false");
    }
}

