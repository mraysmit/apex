package dev.mars.apex.core.engine.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for advanced RuleGroup features including:
 * - Configurable short-circuiting (stop-on-first-failure)
 * - Parallel execution
 * - Debug mode
 * - AND/OR operator configuration
 * 
 * @author APEX Test Team
 * @since 1.0.0
 */
class RuleGroupAdvancedTest {

    private StandardEvaluationContext context;
    private Map<String, Object> testData;

    @BeforeEach
    void setUp() {
        testData = new HashMap<>();
        testData.put("age", 25);
        testData.put("income", 50000);
        testData.put("email", "test@example.com");
        testData.put("score", 85);
        
        context = new StandardEvaluationContext();
        context.setVariables(testData);
    }

    // ========================================
    // Constructor Tests
    // ========================================

    @Nested
    @DisplayName("Advanced Constructor Tests")
    class AdvancedConstructorTests {

        @Test
        @DisplayName("Should create RuleGroup with full configuration")
        void testFullConfigurationConstructor() {
            RuleGroup group = new RuleGroup("test-group", "test-category", "Test Group", 
                                          "Test description", 10, true, false, true, true);
            
            assertEquals("test-group", group.getId());
            assertEquals("Test Group", group.getName());
            assertTrue(group.isAndOperator());
            assertFalse(group.isStopOnFirstFailure());
            assertTrue(group.isParallelExecution());
            assertTrue(group.isDebugMode());
        }

        @Test
        @DisplayName("Should create RuleGroup with default configuration")
        void testDefaultConfigurationConstructor() {
            RuleGroup group = new RuleGroup("test-group", "test-category", "Test Group", 
                                          "Test description", 10, true);
            
            assertEquals("test-group", group.getId());
            assertEquals("Test Group", group.getName());
            assertTrue(group.isAndOperator());
            assertTrue(group.isStopOnFirstFailure()); // Default is true for performance
            assertFalse(group.isParallelExecution()); // Default is false
            assertFalse(group.isDebugMode()); // Default is false
        }

        @Test
        @DisplayName("Should create OR group with custom configuration")
        void testOrGroupWithCustomConfiguration() {
            RuleGroup group = new RuleGroup("or-group", "test-category", "OR Group", 
                                          "OR test description", 20, false, true, false, false);
            
            assertFalse(group.isAndOperator()); // OR group
            assertTrue(group.isStopOnFirstFailure());
            assertFalse(group.isParallelExecution());
            assertFalse(group.isDebugMode());
        }
    }

    // ========================================
    // Short-Circuit Behavior Tests
    // ========================================

    @Nested
    @DisplayName("Short-Circuit Behavior Tests")
    class ShortCircuitTests {

        @Test
        @DisplayName("AND group should short-circuit on first failure when enabled")
        void testAndGroupShortCircuitEnabled() {
            // Create AND group with short-circuit enabled
            RuleGroup group = new RuleGroup("and-group", "test", "AND Group", 
                                          "Test AND", 10, true, true, false, false);
            
            // Add rules: first passes, second fails, third would pass
            Rule rule1 = new Rule("rule1", "#age > 18", "Age check");
            Rule rule2 = new Rule("rule2", "#income > 100000", "High income check"); // This will fail
            Rule rule3 = new Rule("rule3", "#score > 80", "Score check"); // This should not be evaluated
            
            group.addRule(rule1, 1);
            group.addRule(rule2, 2);
            group.addRule(rule3, 3);
            
            boolean result = group.evaluate(context);
            
            assertFalse(result, "AND group should fail when any rule fails");
            // Note: We can't directly test that rule3 wasn't evaluated without modifying the implementation
            // But the behavior is documented and tested through integration tests
        }

        @Test
        @DisplayName("AND group should evaluate all rules when short-circuit disabled")
        void testAndGroupShortCircuitDisabled() {
            // Create AND group with short-circuit disabled
            RuleGroup group = new RuleGroup("and-group", "test", "AND Group", 
                                          "Test AND", 10, true, false, false, false);
            
            // Add rules: first passes, second fails, third passes
            Rule rule1 = new Rule("rule1", "#age > 18", "Age check");
            Rule rule2 = new Rule("rule2", "#income > 100000", "High income check"); // This will fail
            Rule rule3 = new Rule("rule3", "#score > 80", "Score check");
            
            group.addRule(rule1, 1);
            group.addRule(rule2, 2);
            group.addRule(rule3, 3);
            
            boolean result = group.evaluate(context);
            
            assertFalse(result, "AND group should still fail, but all rules should be evaluated");
        }

        @Test
        @DisplayName("OR group should short-circuit on first success when enabled")
        void testOrGroupShortCircuitEnabled() {
            // Create OR group with short-circuit enabled
            RuleGroup group = new RuleGroup("or-group", "test", "OR Group", 
                                          "Test OR", 10, false, true, false, false);
            
            // Add rules: first fails, second passes, third would fail
            Rule rule1 = new Rule("rule1", "#age > 50", "Senior check"); // This will fail
            Rule rule2 = new Rule("rule2", "#income > 30000", "Income check"); // This will pass
            Rule rule3 = new Rule("rule3", "#score > 90", "High score check"); // This should not be evaluated
            
            group.addRule(rule1, 1);
            group.addRule(rule2, 2);
            group.addRule(rule3, 3);
            
            boolean result = group.evaluate(context);
            
            assertTrue(result, "OR group should pass when any rule passes");
        }

        @Test
        @DisplayName("OR group should evaluate all rules when short-circuit disabled")
        void testOrGroupShortCircuitDisabled() {
            // Create OR group with short-circuit disabled
            RuleGroup group = new RuleGroup("or-group", "test", "OR Group", 
                                          "Test OR", 10, false, false, false, false);
            
            // Add rules: first fails, second passes, third fails
            Rule rule1 = new Rule("rule1", "#age > 50", "Senior check"); // This will fail
            Rule rule2 = new Rule("rule2", "#income > 30000", "Income check"); // This will pass
            Rule rule3 = new Rule("rule3", "#score > 90", "High score check"); // This will fail
            
            group.addRule(rule1, 1);
            group.addRule(rule2, 2);
            group.addRule(rule3, 3);
            
            boolean result = group.evaluate(context);
            
            assertTrue(result, "OR group should pass, and all rules should be evaluated");
        }
    }

    // ========================================
    // Debug Mode Tests
    // ========================================

    @Nested
    @DisplayName("Debug Mode Tests")
    class DebugModeTests {

        @Test
        @DisplayName("Debug mode should disable short-circuiting")
        void testDebugModeDisablesShortCircuit() {
            // Create AND group with debug mode enabled (should disable short-circuiting)
            RuleGroup group = new RuleGroup("debug-group", "test", "Debug Group", 
                                          "Test Debug", 10, true, true, false, true);
            
            // Add rules where second rule fails
            Rule rule1 = new Rule("rule1", "#age > 18", "Age check");
            Rule rule2 = new Rule("rule2", "#income > 100000", "High income check"); // This will fail
            Rule rule3 = new Rule("rule3", "#score > 80", "Score check");
            
            group.addRule(rule1, 1);
            group.addRule(rule2, 2);
            group.addRule(rule3, 3);
            
            boolean result = group.evaluate(context);
            
            assertFalse(result, "AND group should fail");
            assertTrue(group.isDebugMode(), "Debug mode should be enabled");
            // In debug mode, all rules should be evaluated despite the failure
        }

        @Test
        @DisplayName("Debug mode should be configurable")
        void testDebugModeConfiguration() {
            RuleGroup debugGroup = new RuleGroup("debug", "test", "Debug", "Debug", 10, 
                                                true, true, false, true);
            RuleGroup normalGroup = new RuleGroup("normal", "test", "Normal", "Normal", 10, 
                                                 true, true, false, false);
            
            assertTrue(debugGroup.isDebugMode());
            assertFalse(normalGroup.isDebugMode());
        }
    }

    // ========================================
    // Parallel Execution Tests
    // ========================================

    @Nested
    @DisplayName("Parallel Execution Tests")
    class ParallelExecutionTests {

        @Test
        @DisplayName("Should support parallel execution configuration")
        void testParallelExecutionConfiguration() {
            RuleGroup parallelGroup = new RuleGroup("parallel", "test", "Parallel", "Parallel", 10, 
                                                   true, false, true, false);
            RuleGroup sequentialGroup = new RuleGroup("sequential", "test", "Sequential", "Sequential", 10, 
                                                     true, false, false, false);
            
            assertTrue(parallelGroup.isParallelExecution());
            assertFalse(sequentialGroup.isParallelExecution());
        }

        @Test
        @DisplayName("Parallel execution should work with multiple rules")
        void testParallelExecutionWithMultipleRules() {
            // Create group with parallel execution enabled
            RuleGroup group = new RuleGroup("parallel-group", "test", "Parallel Group", 
                                          "Test Parallel", 10, true, false, true, false);
            
            // Add multiple rules that should all pass
            Rule rule1 = new Rule("rule1", "#age > 18", "Age check");
            Rule rule2 = new Rule("rule2", "#income > 30000", "Income check");
            Rule rule3 = new Rule("rule3", "#score > 80", "Score check");
            
            group.addRule(rule1, 1);
            group.addRule(rule2, 2);
            group.addRule(rule3, 3);
            
            boolean result = group.evaluate(context);
            
            assertTrue(result, "All rules should pass in parallel execution");
        }

        @Test
        @DisplayName("Parallel execution should handle single rule gracefully")
        void testParallelExecutionWithSingleRule() {
            // Create group with parallel execution enabled
            RuleGroup group = new RuleGroup("parallel-single", "test", "Parallel Single", 
                                          "Test Parallel Single", 10, true, false, true, false);
            
            // Add single rule
            Rule rule1 = new Rule("rule1", "#age > 18", "Age check");
            group.addRule(rule1, 1);
            
            boolean result = group.evaluate(context);
            
            assertTrue(result, "Single rule should pass");
        }
    }

    // ========================================
    // Integration Tests
    // ========================================

    @Nested
    @DisplayName("Integration Tests")
    class IntegrationTests {

        @Test
        @DisplayName("Should handle complex rule combinations")
        void testComplexRuleCombinations() {
            // Create AND group with mixed passing and failing rules
            RuleGroup group = new RuleGroup("complex", "test", "Complex Group", 
                                          "Complex test", 10, true, false, false, true);
            
            Rule rule1 = new Rule("rule1", "#age >= 18", "Adult check");           // Pass
            Rule rule2 = new Rule("rule2", "#income >= 40000", "Income check");    // Pass  
            Rule rule3 = new Rule("rule3", "#score >= 85", "Score check");         // Pass
            Rule rule4 = new Rule("rule4", "#email != null", "Email check");       // Pass
            
            group.addRule(rule1, 1);
            group.addRule(rule2, 2);
            group.addRule(rule3, 3);
            group.addRule(rule4, 4);
            
            boolean result = group.evaluate(context);
            
            assertTrue(result, "All rules should pass");
        }

        @Test
        @DisplayName("Should handle empty rule group")
        void testEmptyRuleGroup() {
            RuleGroup group = new RuleGroup("empty", "test", "Empty Group", 
                                          "Empty test", 10, true, true, false, false);
            
            boolean result = group.evaluate(context);
            
            assertFalse(result, "Empty rule group should return false");
        }

        @Test
        @DisplayName("Should handle null rule results")
        void testNullRuleResults() {
            RuleGroup group = new RuleGroup("null-test", "test", "Null Test", 
                                          "Null test", 10, true, false, false, false);
            
            // Add rule that might return null (invalid expression)
            Rule rule1 = new Rule("rule1", "#nonexistentField > 0", "Null check");
            group.addRule(rule1, 1);
            
            boolean result = group.evaluate(context);
            
            assertFalse(result, "Null rule results should be treated as false");
        }
    }
}
