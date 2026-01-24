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

package dev.mars.apex.demo.errorhandling;

import dev.mars.apex.core.engine.config.RulesEngine;
import dev.mars.apex.core.engine.model.RuleResult;
import dev.mars.apex.demo.ColoredTestOutputExtension;
import dev.mars.apex.demo.DemoTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Reference Test Implementation for APEX_ERROR_HANDLING_GUIDE.md
 *
 * This test class demonstrates the complete testing pattern described in the guide:
 * 1. Happy Path Test (Condition = TRUE)
 * 2. Error Handling Test (Condition = FALSE with ERROR severity)
 * 3. Recovery Test (Condition = FALSE with recovery enabled)
 *
 * Each business rule should have all three test scenarios to ensure comprehensive coverage.
 *
 * This test uses YAML-based configuration to demonstrate the patterns in a way that
 * developers can easily copy and adapt for their own business rules.
 *
 * Follows prompts.txt principles:
 * - Simple and focused on the requirement
 * - Tests actual APEX functionality using RulesEngine
 * - Validates functional results with specific assertions
 * - Extends DemoTestBase for consistent test setup
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-01-17
 * @version 1.0
 */
@ExtendWith(ColoredTestOutputExtension.class)
@DisplayName("APEX Error Handling Guide - Reference Implementation")
class ErrorHandlingGuideReferenceTest extends DemoTestBase {

    private static final Logger logger = LoggerFactory.getLogger(ErrorHandlingGuideReferenceTest.class);

    // ========================================
    // EXAMPLE 1: Trade ID Validation
    // Business Rule: tradeId must not be null
    // Condition: #'tradeId'] != null
    // ========================================

    @Test
    @DisplayName("Example 1 - Scenario 1: Happy Path - tradeId exists (Condition = TRUE)")
    void testTradeIdValidation_HappyPath() throws Exception {
        logger.info("=== Example 1 - Scenario 1: Happy Path ===");
        logger.info("Testing: When tradeId EXISTS, condition evaluates to TRUE → rule MATCHES → SUCCESS");

        // Test data makes condition TRUE (tradeId exists)
        Map<String, Object> data = new HashMap<>();
        data.put("tradeId", "TRADE-001");
        data.put("amount", 1000000.0);

        // Load YAML configuration with ERROR severity rule
        var config = yamlLoader.loadFromFile(
            "src/test/java/dev/mars/apex/demo/errorhandling/ErrorHandlingGuideReferenceTest-happy-path.yaml"
        );

        RulesEngine engine = RulesEngine.fromYamlConfig(config);

        // Get the rule from engine configuration
        var rule = engine.getConfiguration().getRuleById("trade-id-required");
        assertNotNull(rule, "Rule should be found in configuration");

        // Execute rule evaluation
        RuleResult result = engine.executeRule(rule, data);

        // Expected: SUCCESS (rule matches - condition is TRUE)
        assertNotNull(result, "Result should not be null");
        assertTrue(result.isTriggered(), "Rule should be triggered when condition is TRUE");
        assertEquals(RuleResult.ResultType.MATCH, result.getResultType(), "Result type should be MATCH");
        assertTrue(result.isSuccess(), "Result should be successful");

        logger.info("[OK] Happy Path: Condition TRUE → SUCCESS (MATCH)");
        logger.info("This demonstrates that when business rule condition evaluates to TRUE, the rule matches successfully");
    }

    @Test
    @DisplayName("Example 1 - Scenario 2: Error Handling - tradeId missing (Condition = FALSE, ERROR severity, recovery disabled)")
    void testTradeIdValidation_ErrorHandling() throws Exception {
        logger.info("=== Example 1 - Scenario 2: Error Handling ===");
        logger.info("Testing: When tradeId MISSING, condition evaluates to FALSE + ERROR severity → FAIL_FAST");

        // Test data makes condition FALSE (tradeId is null)
        Map<String, Object> data = new HashMap<>();
        data.put("tradeId", null);  // tradeId is null (condition will evaluate to FALSE)
        data.put("amount", 1000000.0);

        // Load YAML configuration with ERROR severity and recovery disabled
        var config = yamlLoader.loadFromFile(
            "src/test/java/dev/mars/apex/demo/errorhandling/ErrorHandlingGuideReferenceTest-error-handling.yaml"
        );

        RulesEngine engine = RulesEngine.fromYamlConfig(config);

        // Get the rule from engine configuration
        var rule = engine.getConfiguration().getRuleById("trade-id-required");
        assertNotNull(rule, "Rule should be found in configuration");

        // Execute rule evaluation
        RuleResult result = engine.executeRule(rule, data);

        // Expected: FAIL_FAST (condition is FALSE, ERROR severity, no recovery)
        assertNotNull(result, "Result should not be null");
        assertFalse(result.isTriggered(), "Rule should not be triggered when condition is FALSE");
        assertEquals(RuleResult.ResultType.ERROR, result.getResultType(), "Result type should be ERROR");
        assertFalse(result.isSuccess(), "Result should not be successful");

        logger.info("[OK] Error Handling: Condition FALSE + ERROR severity + recovery disabled → FAIL_FAST (ERROR)");
        logger.info("This demonstrates that when business rule condition evaluates to FALSE with ERROR severity, the system fails fast");
    }

    @Test
    @DisplayName("Example 1 - Scenario 3: Recovery - tradeId missing (Condition = FALSE, WARNING severity, recovery enabled)")
    void testTradeIdValidation_Recovery() throws Exception {
        logger.info("=== Example 1 - Scenario 3: Recovery ===");
        logger.info("Testing: When tradeId MISSING, condition evaluates to FALSE + WARNING severity → CONTINUE (recovery)");

        // Test data makes condition FALSE (tradeId is null)
        Map<String, Object> data = new HashMap<>();
        data.put("tradeId", null);  // tradeId is null (condition will evaluate to FALSE)
        data.put("amount", 1000000.0);

        // Load YAML configuration with WARNING severity (recovery enabled by default)
        var config = yamlLoader.loadFromFile(
            "src/test/java/dev/mars/apex/demo/errorhandling/ErrorHandlingGuideReferenceTest-recovery.yaml"
        );

        RulesEngine engine = RulesEngine.fromYamlConfig(config);

        // Get the rule from engine configuration
        var rule = engine.getConfiguration().getRuleById("trade-id-recommended");
        assertNotNull(rule, "Rule should be found in configuration");

        // Execute rule evaluation
        RuleResult result = engine.executeRule(rule, data);

        // Expected: CONTINUE (condition is FALSE, WARNING severity, recovery enabled)
        assertNotNull(result, "Result should not be null");
        assertFalse(result.isTriggered(), "Rule should not be triggered when condition is FALSE");
        assertEquals(RuleResult.ResultType.NO_MATCH, result.getResultType(), "Result type should be NO_MATCH (recovered)");
        assertTrue(result.isSuccess(), "Result should be successful (recovered)");

        logger.info("[OK] Recovery: Condition FALSE + WARNING severity + recovery enabled → CONTINUE (NO_MATCH)");
        logger.info("This demonstrates that when business rule condition evaluates to FALSE with WARNING severity, recovery allows processing to continue");
    }

}

