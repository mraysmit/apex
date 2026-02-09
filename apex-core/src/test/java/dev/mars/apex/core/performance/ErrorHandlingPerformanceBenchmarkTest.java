package dev.mars.apex.core.performance;

import dev.mars.apex.core.config.YamlConfigurationLoader;
import dev.mars.apex.core.config.YamlRuleConfiguration;
import dev.mars.apex.core.engine.config.RulesEngine;
import dev.mars.apex.core.engine.model.RuleResult;
import org.junit.jupiter.api.BeforeEach;

import dev.mars.apex.core.test.extension.ColoredTestOutputExtension;
import dev.mars.apex.core.test.extension.TestClassLoggingExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Performance benchmark tests for error handling overhead.
 * 
 * Tests verify that error handling adds < 5ms overhead per operation.
 * 
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 1.1.0
 */
@ExtendWith({ColoredTestOutputExtension.class, TestClassLoggingExtension.class})
class ErrorHandlingPerformanceBenchmarkTest {

    private static final Logger logger = LoggerFactory.getLogger(ErrorHandlingPerformanceBenchmarkTest.class);
    
    private static final int WARMUP_ITERATIONS = 100;
    private static final int MEASUREMENT_ITERATIONS = 1000;
    private static final double MAX_OVERHEAD_MS = 5.0;
    
    private YamlConfigurationLoader yamlLoader;

    @BeforeEach
    void setUp() {
        yamlLoader = new YamlConfigurationLoader();
    }

    @Test
    void testRuleEvaluationPerformanceOverhead() throws Exception {
        logger.info("\n" + "=".repeat(80));
        logger.info("PERFORMANCE BENCHMARK: Rule Evaluation with Error Handling");
        logger.info("=".repeat(80));

        // YAML configuration with error-recovery enabled
        String yamlWithErrorRecovery = """
            metadata:
              name: "Performance Test - With Error Recovery"
              version: "1.0"

            error-recovery:
              enabled: true
              default-strategy: "CONTINUE_WITH_DEFAULT"
              severity-policies:
                CRITICAL:
                  recovery-enabled: false
                  strategy: "FAIL_FAST"
                ERROR:
                  recovery-enabled: true
                  strategy: "CONTINUE_WITH_DEFAULT"
                  max-retries: 1
                WARNING:
                  recovery-enabled: true
                  strategy: "SKIP_RULE"
                INFO:
                  recovery-enabled: true
                  strategy: "SKIP_RULE"

            rules:
              - id: "test-rule-1"
                name: "test-rule"
                condition: "#amount > 1000"
            """;

        // YAML configuration without error-recovery
        String yamlWithoutErrorRecovery = """
            metadata:
              name: "Performance Test - Without Error Recovery"
              version: "1.0"

            rules:
              - id: "test-rule-1"
                name: "test-rule"
                condition: "#amount > 1000"
            """;

        // Load configurations
        YamlRuleConfiguration configWithRecovery = yamlLoader.fromYamlString(yamlWithErrorRecovery);
        YamlRuleConfiguration configWithoutRecovery = yamlLoader.fromYamlString(yamlWithoutErrorRecovery);

        // Create engines
        RulesEngine engineWithRecovery = RulesEngine.fromYamlConfig(configWithRecovery);
        RulesEngine engineWithoutRecovery = RulesEngine.fromYamlConfig(configWithoutRecovery);

        // Test data
        Map<String, Object> testData = new HashMap<>();
        testData.put("amount", 5000.0);

        // Warmup phase
        logger.info("\n--- Warmup Phase ({} iterations) ---", WARMUP_ITERATIONS);
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            engineWithRecovery.evaluate(testData);
            engineWithoutRecovery.evaluate(testData);
        }

        // Measurement phase - WITHOUT error recovery
        logger.info("\n--- Measurement Phase: WITHOUT Error Recovery ({} iterations) ---", MEASUREMENT_ITERATIONS);
        long startWithout = System.nanoTime();
        for (int i = 0; i < MEASUREMENT_ITERATIONS; i++) {
            RuleResult result = engineWithoutRecovery.evaluate(testData);
            assertNotNull(result);
        }
        long timeWithout = System.nanoTime() - startWithout;
        double avgWithoutMs = (timeWithout / 1_000_000.0) / MEASUREMENT_ITERATIONS;

        // Measurement phase - WITH error recovery
        logger.info("\n--- Measurement Phase: WITH Error Recovery ({} iterations) ---", MEASUREMENT_ITERATIONS);
        long startWith = System.nanoTime();
        for (int i = 0; i < MEASUREMENT_ITERATIONS; i++) {
            RuleResult result = engineWithRecovery.evaluate(testData);
            assertNotNull(result);
        }
        long timeWith = System.nanoTime() - startWith;
        double avgWithMs = (timeWith / 1_000_000.0) / MEASUREMENT_ITERATIONS;

        // Calculate overhead
        double overheadMs = avgWithMs - avgWithoutMs;
        double overheadPercent = (overheadMs / avgWithoutMs) * 100.0;

        // Log results
        logger.info("\n" + "=".repeat(80));
        logger.info("PERFORMANCE RESULTS");
        logger.info("=".repeat(80));
        logger.info("Average time WITHOUT error recovery: {} ms", String.format("%.4f", avgWithoutMs));
        logger.info("Average time WITH error recovery:    {} ms", String.format("%.4f", avgWithMs));
        logger.info("Overhead:                             {} ms ({}%)",
            String.format("%.4f", overheadMs), String.format("%.2f", overheadPercent));
        logger.info("Target overhead:                      < {} ms", String.format("%.1f", MAX_OVERHEAD_MS));
        logger.info("=".repeat(80));

        // Verify overhead is acceptable
        assertTrue(overheadMs < MAX_OVERHEAD_MS, 
            String.format("Error handling overhead (%.4f ms) exceeds target (%.1f ms)", overheadMs, MAX_OVERHEAD_MS));
        
        logger.info("\nPASS: Error handling overhead is within acceptable limits");
    }
}

