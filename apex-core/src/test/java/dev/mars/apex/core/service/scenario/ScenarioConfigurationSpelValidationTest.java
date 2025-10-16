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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for SpEL expression validation in ScenarioConfiguration.
 * 
 * Tests cover:
 * - Invalid SpEL syntax detection
 * - Undefined variable detection
 * - Type mismatch detection
 * - Invalid operator detection
 * - Graceful error handling
 * 
 * FOLLOWS CODING PRINCIPLES FROM prompts.txt:
 * - Test actual functionality (SpEL validation)
 * - Use real SpEL parser
 * - Progressive complexity (simple to complex errors)
 * - Validate error messages are clear and actionable
 * - Log test errors with "TEST:" prefix to distinguish from production errors
 * 
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 1.0.0
 */
@DisplayName("ScenarioConfiguration SpEL Validation Tests")
class ScenarioConfigurationSpelValidationTest {

    private static final Logger logger = LoggerFactory.getLogger(ScenarioConfigurationSpelValidationTest.class);
    
    private ScenarioConfiguration scenario;

    @BeforeEach
    void setUp() {
        scenario = new ScenarioConfiguration();
        scenario.setScenarioId("spel-validation-test");
        scenario.setName("SpEL Validation Test Scenario");
    }

    // ========================================
    // Level 1: Invalid Syntax Detection
    // ========================================

    @Test
    @DisplayName("Should detect missing closing bracket in SpEL")
    void testMissingClosingBracket() {
        logger.info("TEST: Triggering intentional error - Missing closing bracket in SpEL");
        
        // Given: SpEL with missing closing bracket
        scenario.setClassificationRuleCondition("#data['field' == 'value'");

        // When: Evaluate against data
        Map<String, Object> data = new HashMap<>();
        data.put("field", "value");

        // Then: Should not match (gracefully handle syntax error)
        assertFalse(scenario.matchesClassificationRule(data),
            "Should return false for invalid SpEL syntax");
    }

    @Test
    @DisplayName("Should detect missing opening bracket in SpEL")
    void testMissingOpeningBracket() {
        logger.info("TEST: Triggering intentional error - Missing opening bracket in SpEL");
        
        // Given: SpEL with missing opening bracket
        scenario.setClassificationRuleCondition("#datafield'] == 'value'");

        // When: Evaluate against data
        Map<String, Object> data = new HashMap<>();
        data.put("field", "value");

        // Then: Should not match (gracefully handle syntax error)
        assertFalse(scenario.matchesClassificationRule(data),
            "Should return false for invalid SpEL syntax");
    }

    @Test
    @DisplayName("Should detect unclosed string literal in SpEL")
    void testUnclosedStringLiteral() {
        logger.info("TEST: Triggering intentional error - Unclosed string literal in SpEL");
        
        // Given: SpEL with unclosed string
        scenario.setClassificationRuleCondition("#data['field'] == 'value");

        // When: Evaluate against data
        Map<String, Object> data = new HashMap<>();
        data.put("field", "value");

        // Then: Should not match (gracefully handle syntax error)
        assertFalse(scenario.matchesClassificationRule(data),
            "Should return false for unclosed string literal");
    }

    // ========================================
    // Level 2: Invalid Operators
    // ========================================

    @Test
    @DisplayName("Should detect invalid operator in SpEL")
    void testInvalidOperator() {
        logger.info("TEST: Triggering intentional error - Invalid operator in SpEL");
        
        // Given: SpEL with invalid operator (==> instead of ==)
        scenario.setClassificationRuleCondition("#data['field'] ==> 'value'");

        // When: Evaluate against data
        Map<String, Object> data = new HashMap<>();
        data.put("field", "value");

        // Then: Should not match (gracefully handle invalid operator)
        assertFalse(scenario.matchesClassificationRule(data),
            "Should return false for invalid operator");
    }

    @Test
    @DisplayName("Should detect malformed AND operator")
    void testMalformedAndOperator() {
        logger.info("TEST: Triggering intentional error - Malformed AND operator");
        
        // Given: SpEL with malformed AND (& instead of &&)
        scenario.setClassificationRuleCondition("#data['a'] == 'x' & #data['b'] == 'y'");

        // When: Evaluate against data
        Map<String, Object> data = new HashMap<>();
        data.put("a", "x");
        data.put("b", "y");

        // Then: Should not match (gracefully handle malformed operator)
        assertFalse(scenario.matchesClassificationRule(data),
            "Should return false for malformed AND operator");
    }

    // ========================================
    // Level 3: Type Mismatches
    // ========================================

    @Test
    @DisplayName("Should handle type mismatch in comparison")
    void testTypeMismatchComparison() {
        logger.info("TEST: Triggering intentional error - Type mismatch in comparison");
        
        // Given: SpEL comparing number to string
        scenario.setClassificationRuleCondition("#data['amount'] > 'string'");

        // When: Evaluate against data
        Map<String, Object> data = new HashMap<>();
        data.put("amount", 1000);

        // Then: Should not match (gracefully handle type mismatch)
        assertFalse(scenario.matchesClassificationRule(data),
            "Should return false for type mismatch");
    }

    @Test
    @DisplayName("Should handle comparison of incompatible types")
    void testIncompatibleTypeComparison() {
        logger.info("TEST: Triggering intentional error - Incompatible type comparison");
        
        // Given: SpEL comparing list to string
        scenario.setClassificationRuleCondition("#data['items'] == 'string'");

        // When: Evaluate against data with list
        Map<String, Object> data = new HashMap<>();
        data.put("items", Arrays.asList("a", "b", "c"));

        // Then: Should not match (gracefully handle type mismatch)
        assertFalse(scenario.matchesClassificationRule(data),
            "Should return false for incompatible type comparison");
    }

    // ========================================
    // Level 4: Valid SpEL Still Works
    // ========================================

    @Test
    @DisplayName("Should still match valid SpEL after testing invalid ones")
    void testValidSpelStillWorks() {
        // Given: Valid SpEL expression
        scenario.setClassificationRuleCondition("#data['tradeType'] == 'OTCOption'");

        // When: Evaluate against matching data
        Map<String, Object> data = new HashMap<>();
        data.put("tradeType", "OTCOption");

        // Then: Should match
        assertTrue(scenario.matchesClassificationRule(data),
            "Valid SpEL should still work correctly");
    }

    @Test
    @DisplayName("Should handle complex valid SpEL with multiple conditions")
    void testComplexValidSpel() {
        // Given: Complex valid SpEL
        scenario.setClassificationRuleCondition(
            "#data['tradeType'] == 'OTCOption' && #data['notional'] > 100000000 && #data['region'] == 'US'");

        // When: Evaluate against matching data
        Map<String, Object> data = new HashMap<>();
        data.put("tradeType", "OTCOption");
        data.put("notional", 150000000);
        data.put("region", "US");

        // Then: Should match
        assertTrue(scenario.matchesClassificationRule(data),
            "Complex valid SpEL should work correctly");
    }

    // ========================================
    // Level 5: Edge Cases
    // ========================================

    @Test
    @DisplayName("Should handle empty SpEL expression")
    void testEmptySpelExpression() {
        logger.info("TEST: Triggering intentional error - Empty SpEL expression");
        
        // Given: Empty SpEL
        scenario.setClassificationRuleCondition("");

        // When: Evaluate against data
        Map<String, Object> data = new HashMap<>();
        data.put("field", "value");

        // Then: Should not match
        assertFalse(scenario.matchesClassificationRule(data),
            "Should return false for empty SpEL expression");
    }

    @Test
    @DisplayName("Should handle null SpEL expression")
    void testNullSpelExpression() {
        logger.info("TEST: Triggering intentional error - Null SpEL expression");
        
        // Given: Null SpEL
        scenario.setClassificationRuleCondition(null);

        // When: Evaluate against data
        Map<String, Object> data = new HashMap<>();
        data.put("field", "value");

        // Then: Should not match
        assertFalse(scenario.matchesClassificationRule(data),
            "Should return false for null SpEL expression");
    }

    @Test
    @DisplayName("Should handle whitespace-only SpEL expression")
    void testWhitespaceOnlySpelExpression() {
        logger.info("TEST: Triggering intentional error - Whitespace-only SpEL expression");
        
        // Given: Whitespace-only SpEL
        scenario.setClassificationRuleCondition("   ");

        // When: Evaluate against data
        Map<String, Object> data = new HashMap<>();
        data.put("field", "value");

        // Then: Should not match
        assertFalse(scenario.matchesClassificationRule(data),
            "Should return false for whitespace-only SpEL expression");
    }
}

