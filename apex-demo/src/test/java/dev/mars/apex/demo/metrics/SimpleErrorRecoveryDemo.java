package dev.mars.apex.demo.metrics;

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

import dev.mars.apex.core.config.yaml.YamlRuleConfiguration;
import dev.mars.apex.core.config.yaml.YamlRulesEngineService;
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
 * Simple Error Recovery Demo
 *
 * This demo introduces the basic concepts of error recovery in APEX.
 * It shows how APEX can gracefully handle missing data and rule failures
 * using simple default value recovery strategies.
 *
 * ============================================================================
 * LEARNING OBJECTIVES:
 * - Understand what happens when rules fail due to missing data
 * - Learn how APEX can recover from errors using default values
 * - See the difference between error recovery enabled vs disabled
 * - Introduction to error recovery configuration
 * ============================================================================
 *
 * CONCEPTS INTRODUCED:
 * - Rule failures due to missing data
 * - Default value recovery strategy
 * - Error recovery configuration in YAML
 * - Graceful error handling vs exceptions
 */
@ExtendWith(ColoredTestOutputExtension.class)
public class SimpleErrorRecoveryDemo extends DemoTestBase {

    private static final Logger logger = LoggerFactory.getLogger(SimpleErrorRecoveryDemo.class);

    @Test
    @DisplayName("Demo 2: Simple Error Recovery Introduction")
    void demonstrateSimpleErrorRecovery() throws Exception {
        logger.info("================================================================================");
        logger.info("SIMPLE DEMO: Introduction to APEX Error Recovery");
        logger.info("================================================================================");

        // Step 1: Load YAML configuration with error recovery enabled
        YamlRuleConfiguration config = yamlLoader.loadFromFile(
            "src/test/java/dev/mars/apex/demo/metrics/SimpleErrorRecoveryDemo.yaml");
        assertNotNull(config, "YAML configuration should load successfully");

        // Step 2: Create rules engine
        YamlRulesEngineService rulesEngineService = new YamlRulesEngineService();
        RulesEngine rulesEngine = rulesEngineService.createRulesEngineFromYamlConfig(config);
        assertNotNull(rulesEngine, "Rules engine should be created successfully");

        logger.info("\n" + "=".repeat(60));
        logger.info("ERROR RECOVERY DEMONSTRATION");
        logger.info("=".repeat(60));

        // Step 3: Test with complete data (no errors expected)
        logger.info("\n--- Scenario 1: Complete Data (No Recovery Needed) ---");
        Map<String, Object> completeData = createCompleteTestData();
        RuleResult completeResult = rulesEngine.evaluate(config, completeData);
        analyzeResult("Complete Data", completeResult);

        // Step 4: Test with missing data (recovery should kick in)
        logger.info("\n--- Scenario 2: Missing Data (Recovery Required) ---");
        Map<String, Object> incompleteData = createIncompleteTestData();
        RuleResult recoveryResult = rulesEngine.evaluate(config, incompleteData);
        analyzeResult("Missing Data", recoveryResult);

        // Step 5: Test with completely empty data (maximum recovery)
        logger.info("\n--- Scenario 3: Empty Data (Maximum Recovery) ---");
        Map<String, Object> emptyData = new HashMap<>();
        RuleResult emptyResult = rulesEngine.evaluate(config, emptyData);
        analyzeResult("Empty Data", emptyResult);

        // Step 6: Demonstrate recovery benefits
        demonstrateRecoveryBenefits(completeResult, recoveryResult, emptyResult);

        logger.info("\n================================================================================");
        logger.info("DEMO COMPLETED: Simple Error Recovery");
        logger.info("Key Takeaways:");
        logger.info("- APEX can gracefully handle missing data using error recovery");
        logger.info("- Default value recovery provides fallback values when data is missing");
        logger.info("- Error recovery prevents application crashes from data quality issues");
        logger.info("- Recovery strategies can be configured per rule severity level");
        logger.info("================================================================================");
    }

    /**
     * Create complete test data with all required fields.
     */
    private Map<String, Object> createCompleteTestData() {
        Map<String, Object> data = new HashMap<>();
        data.put("customerId", "CUST001");
        data.put("amount", 5000.0);
        data.put("riskScore", 75);
        data.put("creditRating", "A");
        data.put("region", "AMERICAS");
        return data;
    }

    /**
     * Create incomplete test data with some missing fields.
     */
    private Map<String, Object> createIncompleteTestData() {
        Map<String, Object> data = new HashMap<>();
        data.put("customerId", "CUST002");
        data.put("amount", 3000.0);
        // Missing: riskScore, creditRating, region
        return data;
    }

    /**
     * Analyze and display rule evaluation results.
     */
    private void analyzeResult(String scenario, RuleResult result) {
        logger.info("Results for {}:", scenario);
        logger.info("  - Success: {}", result.isSuccess());
        logger.info("  - Has failures: {}", result.hasFailures());
        logger.info("  - Enriched data fields: {}", result.getEnrichedData().size());
        
        if (result.hasFailures()) {
            logger.info("  - Failure messages: {}", result.getFailureMessages().size());
            for (String message : result.getFailureMessages()) {
                logger.info("    * {}", message);
            }
        } else {
            logger.info("  - No failures detected");
        }
        
        // Show some enriched data if available
        if (!result.getEnrichedData().isEmpty()) {
            logger.info("  - Sample enriched data:");
            result.getEnrichedData().entrySet().stream()
                .limit(3)
                .forEach(entry -> logger.info("    * {}: {}", entry.getKey(), entry.getValue()));
        }
    }

    /**
     * Demonstrate the benefits of error recovery by comparing results.
     */
    private void demonstrateRecoveryBenefits(RuleResult complete, RuleResult recovery, RuleResult empty) {
        logger.info("\n--- Error Recovery Benefits Analysis ---");
        
        logger.info("Data Completeness Impact:");
        logger.info("  - Complete data success: {}", complete.isSuccess());
        logger.info("  - Incomplete data success: {}", recovery.isSuccess());
        logger.info("  - Empty data success: {}", empty.isSuccess());
        
        logger.info("\nError Recovery Effectiveness:");
        if (recovery.isSuccess() && empty.isSuccess()) {
            logger.info("  - Error recovery successfully handled missing data");
            logger.info("  - Application continued processing despite data quality issues");
            logger.info("  - Default values provided fallback behavior");
        } else {
            logger.info("  - Some scenarios still resulted in failures");
            logger.info("  - Additional recovery strategies may be needed");
        }
        
        logger.info("\nData Quality Insights:");
        logger.info("  - Complete data enriched fields: {}", complete.getEnrichedData().size());
        logger.info("  - Incomplete data enriched fields: {}", recovery.getEnrichedData().size());
        logger.info("  - Empty data enriched fields: {}", empty.getEnrichedData().size());
        
        if (complete.getEnrichedData().size() > recovery.getEnrichedData().size()) {
            logger.info("  - Missing data reduced enrichment effectiveness");
            logger.info("  - Consider improving data quality at source");
        }
    }
}
