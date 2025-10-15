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
 * Recovery Strategy Comparison Demo
 *
 * This demo compares different error recovery strategies available in APEX.
 * It demonstrates how different strategies handle rule failures and their
 * impact on overall processing behavior.
 *
 * ============================================================================
 * LEARNING OBJECTIVES:
 * - Understand different recovery strategies: CONTINUE_WITH_DEFAULT, SKIP_RULE, FAIL_FAST
 * - See how each strategy affects processing when rules fail
 * - Learn when to use each strategy based on business requirements
 * - Compare performance and behavior characteristics
 * ============================================================================
 *
 * CONCEPTS INTRODUCED:
 * - CONTINUE_WITH_DEFAULT: Use fallback values and continue processing
 * - SKIP_RULE: Skip failed rules but continue with others
 * - FAIL_FAST: Stop processing immediately on first failure
 * - Strategy selection based on business criticality
 * - Performance implications of different strategies
 */
@ExtendWith(ColoredTestOutputExtension.class)
public class RecoveryStrategyComparisonDemo extends DemoTestBase {

    private static final Logger logger = LoggerFactory.getLogger(RecoveryStrategyComparisonDemo.class);

    @Test
    @DisplayName("Demo 3: Recovery Strategy Comparison")
    void demonstrateRecoveryStrategies() throws Exception {
        logger.info("================================================================================");
        logger.info("STRATEGY DEMO: Comparing APEX Error Recovery Strategies");
        logger.info("================================================================================");

        // Test data that will cause some rules to fail
        Map<String, Object> problematicData = createProblematicTestData();
        
        logger.info("\n" + "=".repeat(70));
        logger.info("RECOVERY STRATEGY COMPARISON");
        logger.info("=".repeat(70));
        logger.info("Test Data: {}", problematicData);

        // Strategy 1: CONTINUE_WITH_DEFAULT
        logger.info("\n--- Strategy 1: CONTINUE_WITH_DEFAULT ---");
        testRecoveryStrategy("ContinueWithDefault", problematicData);

        // Strategy 2: SKIP_RULE  
        logger.info("\n--- Strategy 2: SKIP_RULE ---");
        testRecoveryStrategy("SkipRule", problematicData);

        // Strategy 3: FAIL_FAST
        logger.info("\n--- Strategy 3: FAIL_FAST ---");
        testRecoveryStrategy("FailFast", problematicData);

        // Compare strategies
        compareStrategies();

        logger.info("\n================================================================================");
        logger.info("DEMO COMPLETED: Recovery Strategy Comparison");
        logger.info("Key Takeaways:");
        logger.info("- CONTINUE_WITH_DEFAULT: Best for non-critical rules, provides fallback values");
        logger.info("- SKIP_RULE: Good for optional validations, continues processing without defaults");
        logger.info("- FAIL_FAST: Essential for critical validations, stops processing immediately");
        logger.info("- Choose strategy based on business impact of rule failures");
        logger.info("================================================================================");
    }

    /**
     * Test a specific recovery strategy configuration.
     */
    private void testRecoveryStrategy(String strategyName, Map<String, Object> testData) throws Exception {
        String configFile = String.format("src/test/java/dev/mars/apex/demo/metrics/RecoveryStrategy%sDemo.yaml", strategyName);
        
        try {
            // Load strategy-specific configuration
            YamlRuleConfiguration config = yamlLoader.loadFromFile(configFile);
            assertNotNull(config, "YAML configuration should load successfully for " + strategyName);

            // Create rules engine
            YamlRulesEngineService rulesEngineService = new YamlRulesEngineService();
            RulesEngine rulesEngine = rulesEngineService.createRulesEngineFromYamlConfig(config);

            // Measure execution time
            long startTime = System.nanoTime();
            RuleResult result = rulesEngine.evaluate(config, testData);
            long endTime = System.nanoTime();
            
            double executionMs = (endTime - startTime) / 1_000_000.0;

            // Analyze results
            analyzeStrategyResult(strategyName, result, executionMs);
            
        } catch (Exception e) {
            logger.info("Strategy {} encountered exception: {}", strategyName, e.getMessage());
            logger.info("This demonstrates the strategy's failure handling behavior");
        }
    }

    /**
     * Create test data that will cause rule failures.
     */
    private Map<String, Object> createProblematicTestData() {
        Map<String, Object> data = new HashMap<>();
        data.put("customerId", ""); // Empty customer ID - will fail validation
        data.put("amount", -1000.0); // Negative amount - will fail validation
        data.put("riskScore", 150); // Invalid risk score - will fail validation
        // Missing creditRating - will fail validation
        data.put("region", "INVALID_REGION"); // Invalid region - will fail validation
        return data;
    }

    /**
     * Analyze and display results for a specific recovery strategy.
     */
    private void analyzeStrategyResult(String strategyName, RuleResult result, double executionMs) {
        logger.info("Results for {} strategy:", strategyName);
        logger.info("  - Execution time: {}ms", String.format("%.2f%%", executionMs));
        logger.info("  - Success: {}", result.isSuccess());
        logger.info("  - Has failures: {}", result.hasFailures());
        logger.info("  - Enriched data fields: {}", result.getEnrichedData().size());
        
        if (result.hasFailures()) {
            logger.info("  - Failure count: {}", result.getFailureMessages().size());
            logger.info("  - Sample failures:");
            result.getFailureMessages().stream()
                .limit(3)
                .forEach(message -> logger.info("    * {}", message));
        }
        
        // Show strategy-specific behavior
        analyzeStrategyBehavior(strategyName, result);
    }

    /**
     * Analyze strategy-specific behavior patterns.
     */
    private void analyzeStrategyBehavior(String strategyName, RuleResult result) {
        switch (strategyName) {
            case "ContinueWithDefault":
                if (result.getEnrichedData().size() > 0) {
                    logger.info("  - Strategy provided default values for failed rules");
                    logger.info("  - Processing continued despite failures");
                } else {
                    logger.info("  - Strategy attempted to provide defaults but may have limitations");
                }
                break;
                
            case "SkipRule":
                if (result.isSuccess() || !result.hasFailures()) {
                    logger.info("  - Strategy successfully skipped failed rules");
                    logger.info("  - Processing continued with valid rules only");
                } else {
                    logger.info("  - Strategy encountered failures but continued processing");
                }
                break;
                
            case "FailFast":
                if (result.hasFailures()) {
                    logger.info("  - Strategy detected failures and stopped processing");
                    logger.info("  - Fast failure prevents further resource consumption");
                } else {
                    logger.info("  - Strategy would have failed fast if errors occurred");
                }
                break;
        }
    }

    /**
     * Compare the different recovery strategies.
     */
    private void compareStrategies() {
        logger.info("\n--- Recovery Strategy Comparison Summary ---");
        
        logger.info("\nCONTINUE_WITH_DEFAULT Strategy:");
        logger.info("  ✓ Provides fallback values for failed rules");
        logger.info("  ✓ Ensures processing always completes");
        logger.info("  ✓ Good for non-critical business rules");
        logger.info("  ⚠ May mask data quality issues");
        logger.info("  ⚠ Default values may not be business-appropriate");
        
        logger.info("\nSKIP_RULE Strategy:");
        logger.info("  ✓ Continues processing with valid rules only");
        logger.info("  ✓ Doesn't introduce potentially incorrect default values");
        logger.info("  ✓ Good for optional validations");
        logger.info("  ⚠ May result in incomplete processing");
        logger.info("  ⚠ Requires downstream systems to handle missing data");
        
        logger.info("\nFAIL_FAST Strategy:");
        logger.info("  ✓ Immediately identifies critical failures");
        logger.info("  ✓ Prevents processing of invalid data");
        logger.info("  ✓ Essential for critical business rules");
        logger.info("  ⚠ Stops all processing on first failure");
        logger.info("  ⚠ May be too strict for some use cases");
        
        logger.info("\nRecommendations:");
        logger.info("  • Use FAIL_FAST for critical validations (regulatory, safety)");
        logger.info("  • Use CONTINUE_WITH_DEFAULT for enrichment rules with sensible defaults");
        logger.info("  • Use SKIP_RULE for optional business rules");
        logger.info("  • Consider hybrid approaches with different strategies per rule severity");
    }
}
