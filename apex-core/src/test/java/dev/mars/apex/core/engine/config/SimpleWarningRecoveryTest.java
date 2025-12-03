package dev.mars.apex.core.engine.config;

import dev.mars.apex.core.config.error.ErrorRecoveryConfig;
import dev.mars.apex.core.engine.model.Rule;
import dev.mars.apex.core.engine.model.RuleResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Simple test to debug WARNING severity recovery issue.
 */
class SimpleWarningRecoveryTest {

    private RulesEngine rulesEngine;

    @BeforeEach
    void setUp() {
        System.out.println("=== Setting up SimpleWarningRecoveryTest ===");
        
        RulesEngineConfiguration configuration = new RulesEngineConfiguration();
        rulesEngine = new RulesEngine(configuration);
        
        // Check default error recovery config
        ErrorRecoveryConfig config = new ErrorRecoveryConfig();
        System.out.println("WARNING recovery enabled: " + config.isRecoveryEnabledForSeverity("WARNING"));
        System.out.println("WARNING recovery strategy: " + config.getRecoveryStrategy("WARNING"));
    }
    
    @Test
    void testWarningRecovery() {
        System.out.println("\n=== Testing WARNING Recovery ===");
        
        // Create a rule that WILL throw an exception - method call on missing variable
        Rule rule = new Rule("warning-rule", "#missingVariable.length() > 0", "Test", "WARNING");
        
        System.out.println("Rule: " + rule.getName());
        System.out.println("Condition: " + rule.getCondition());
        System.out.println("Severity: " + rule.getSeverity());
        
        // Empty facts map
        Map<String, Object> facts = new HashMap<>();
        System.out.println("Facts: " + facts);
        
        // Execute rule
        System.out.println("\nExecuting rule...");
        RuleResult result = rulesEngine.executeRule(rule, facts);
        
        // Print result
        System.out.println("\n=== Result ===");
        System.out.println("Result type: " + result.getResultType());
        System.out.println("Message: " + result.getMessage());
        System.out.println("Severity: " + result.getSeverity());
        System.out.println("Is triggered: " + result.isTriggered());
        System.out.println("Is success: " + result.isSuccess());
        
        // Check performance metrics for recovery info
        if (result.getPerformanceMetrics() != null) {
            System.out.println("\n=== Performance Metrics ===");
            System.out.println("Recovery attempted: " + result.getPerformanceMetrics().isRecoveryAttempted());
            System.out.println("Recovery successful: " + result.getPerformanceMetrics().isRecoverySuccessful());
            System.out.println("Recovery strategy: " + result.getPerformanceMetrics().getRecoveryStrategy());
            System.out.println("Recovery reason: " + result.getPerformanceMetrics().getRecoveryReason());
        }
        
        // Assertion
        assertEquals(RuleResult.ResultType.NO_MATCH, result.getResultType(),
                    "WARNING severity should be recovered to NO_MATCH");
    }
}
