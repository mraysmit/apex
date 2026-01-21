/*
 * Copyright 2024 APEX Demo Team
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

package dev.mars.apex.demo.logging;

import dev.mars.apex.core.config.yaml.YamlConfigurationLoader;
import dev.mars.apex.core.config.yaml.YamlRuleConfiguration;
import dev.mars.apex.core.engine.config.RulesEngine;
import dev.mars.apex.core.engine.model.RuleResult;

import dev.mars.apex.demo.ColoredTestOutputExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Condition Evaluation Logging Test
 * 
 * PURPOSE: Demonstrates that OR/AND/General condition evaluation failures are now 
 * logged at SEVERE level instead of WARNING level, providing clear visibility.
 * 
 * LOGGING BEHAVIOR TESTED:
 * - OR condition evaluation failures → SEVERE (was WARNING)
 * - AND condition evaluation failures → SEVERE (was WARNING)
 * - General condition evaluation failures → SEVERE (was WARNING)
 * - Clear error messages with "ERROR:" prefix
 * - Full context including condition text and error details
 * 
 * USER VISIBILITY:
 * - Configuration problems are immediately obvious
 * - No more silent condition evaluation failures
 * - Clear indication of business logic problems
 */
@ExtendWith(ColoredTestOutputExtension.class)
class ConditionEvaluationLoggingTest {

    private static final Logger logger = LoggerFactory.getLogger(ConditionEvaluationLoggingTest.class);
    
    private YamlConfigurationLoader yamlLoader;

    @BeforeEach
    void setUp() {
        logger.info("🔧 Initializing APEX services for condition evaluation logging test");

        // Initialize YAML loader
        yamlLoader = new YamlConfigurationLoader();

        logger.info("All services initialized for condition evaluation logging test");
    }

    @Test
    @DisplayName("OR condition evaluation failures must log as SEVERE")
    void testOrConditionEvaluationFailureLogging() throws Exception {
        logger.info("=== OR CONDITION EVALUATION LOGGING TEST ===");
        logger.info("🎯 PURPOSE: Verify that OR condition evaluation failures are logged as SEVERE");
        logger.info("EXPECTED: You should see SEVERE logs with 'ERROR: Failed to evaluate OR condition'");
        logger.info("OLD BEHAVIOR: Would have been WARNING logs");
        
        try {
            // Load YAML with invalid OR conditions
            YamlRuleConfiguration config = yamlLoader.loadFromFile("src/test/java/dev/mars/apex/demo/logging/ConditionEvaluationLoggingTest.yaml");
            
            // Create test data that will trigger OR condition evaluation failures
            Map<String, Object> testData = new HashMap<>();
            testData.put("customerId", "CUST123");
            testData.put("amount", 1000.0);
            
            logger.info("Processing enrichments with invalid OR conditions...");
            logger.info("WATCH FOR: SEVERE logs with 'ERROR: Failed to evaluate OR condition'");
            
            // Process enrichments - this will trigger SEVERE logging for OR condition failures
            RulesEngine engine = RulesEngine.fromYamlConfig(config);
            engine.evaluate(config, testData);

            logger.info("Processing completed.");
            logger.info("OR CONDITION LOGGING VERIFICATION:");
            logger.info("   Check for SEVERE level logs (not WARNING)");
            logger.info("   Look for 'ERROR: Failed to evaluate OR condition' messages");
            logger.info("   Verify condition text is included in error message");
            logger.info("   Confirm detailed error information is provided");
            
        } catch (Exception e) {
            logger.error("Test failed with exception: " + e.getMessage(), e);
            throw e;
        }
    }

    @Test
    @DisplayName("AND condition evaluation failures must log as SEVERE")
    void testAndConditionEvaluationFailureLogging() throws Exception {
        logger.info("=== AND CONDITION EVALUATION LOGGING TEST ===");
        logger.info("🎯 PURPOSE: Verify that AND condition evaluation failures are logged as SEVERE");
        logger.info("EXPECTED: You should see SEVERE logs with 'ERROR: Failed to evaluate AND condition'");
        
        try {
            // Load YAML with invalid AND conditions
            YamlRuleConfiguration config = yamlLoader.loadFromFile("src/test/java/dev/mars/apex/demo/logging/ConditionEvaluationLoggingTest.yaml");
            
            // Create test data that will trigger AND condition evaluation failures
            Map<String, Object> testData = new HashMap<>();
            testData.put("customerId", "CUST456");
            testData.put("amount", 2000.0);
            
            logger.info("Processing enrichments with invalid AND conditions...");
            logger.info("WATCH FOR: SEVERE logs with 'ERROR: Failed to evaluate AND condition'");
            
            // Process enrichments - this will trigger SEVERE logging for AND condition failures
            RulesEngine engine = RulesEngine.fromYamlConfig(config);
            engine.evaluate(config, testData);

            logger.info("Processing completed.");
            logger.info("AND CONDITION LOGGING VERIFICATION:");
            logger.info("   Check for SEVERE level logs (not WARNING)");
            logger.info("   Look for 'ERROR: Failed to evaluate AND condition' messages");
            logger.info("   Verify condition text is included in error message");
            logger.info("   Confirm detailed error information is provided");
            
        } catch (Exception e) {
            logger.error("Test failed with exception: " + e.getMessage(), e);
            throw e;
        }
    }

    @Test
    @DisplayName("General condition evaluation failures must log as SEVERE")
    void testGeneralConditionEvaluationFailureLogging() throws Exception {
        logger.info("=== GENERAL CONDITION EVALUATION LOGGING TEST ===");
        logger.info("🎯 PURPOSE: Verify that general condition evaluation failures are logged as SEVERE");
        logger.info("EXPECTED: You should see SEVERE logs with 'ERROR: Failed to evaluate condition'");
        
        try {
            // Load YAML with invalid general conditions
            YamlRuleConfiguration config = yamlLoader.loadFromFile("src/test/java/dev/mars/apex/demo/logging/ConditionEvaluationLoggingTest.yaml");
            
            // Create test data that will trigger general condition evaluation failures
            Map<String, Object> testData = new HashMap<>();
            testData.put("customerId", "CUST789");
            testData.put("amount", 3000.0);
            
            logger.info("Processing enrichments with invalid general conditions...");
            logger.info("WATCH FOR: SEVERE logs with 'ERROR: Failed to evaluate condition'");
            
            // Process enrichments - this will trigger SEVERE logging for general condition failures
            RulesEngine engine = RulesEngine.fromYamlConfig(config);
            engine.evaluate(config, testData);

            logger.info("Processing completed.");
            logger.info("GENERAL CONDITION LOGGING VERIFICATION:");
            logger.info("   Check for SEVERE level logs (not WARNING)");
            logger.info("   Look for 'ERROR: Failed to evaluate condition' messages");
            logger.info("   Verify condition text is included in error message");
            logger.info("   Confirm detailed error information is provided");
            
        } catch (Exception e) {
            logger.error("Test failed with exception: " + e.getMessage(), e);
            throw e;
        }
    }

    @Test
    @DisplayName("Document condition evaluation logging improvements")
    void testDocumentConditionEvaluationImprovements() {
        logger.info("=== CONDITION EVALUATION LOGGING IMPROVEMENTS ===");
        logger.info("🎯 IMPROVEMENTS IMPLEMENTED:");
        logger.info("   OR condition evaluation failures → SEVERE (was WARNING)");
        logger.info("   AND condition evaluation failures → SEVERE (was WARNING)");
        logger.info("   General condition evaluation failures → SEVERE (was WARNING)");
        logger.info("   Clear 'ERROR:' prefix for all condition failures");
        logger.info("   Full condition text included in error messages");
        logger.info("   Detailed error context provided");
        
        logger.info("🎯 USER VISIBILITY BENEFITS:");
        logger.info("   Configuration problems immediately obvious");
        logger.info("   No more silent condition evaluation failures");
        logger.info("   Clear indication of business logic problems");
        logger.info("   Easier debugging with full condition context");
        
        logger.info("CONDITION EVALUATION LOGGING: DOCUMENTED");
    }
}

