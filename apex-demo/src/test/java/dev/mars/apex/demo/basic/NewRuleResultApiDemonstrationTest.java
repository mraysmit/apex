package dev.mars.apex.demo.basic;

import dev.mars.apex.core.engine.config.RulesEngine;
import dev.mars.apex.core.engine.config.RulesEngineConfiguration;
import dev.mars.apex.core.engine.model.Rule;
import dev.mars.apex.core.engine.model.RuleGroup;
import dev.mars.apex.core.engine.model.RuleResult;
import dev.mars.apex.core.service.error.ErrorRecoveryService;
import dev.mars.apex.core.service.monitoring.RulePerformanceMonitor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.expression.spel.standard.SpelExpressionParser;

import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Demonstration of the new clean RuleResult API design.
 * 
 * This test shows the difference between the old confusing API and the new clean API
 * that provides semantic clarity and proper failure diagnostics.
 * 
 * @author APEX Enhancement Team
 * @since 2025-09-24
 * @version 1.0
 */
@DisplayName("New RuleResult API Demonstration")
public class NewRuleResultApiDemonstrationTest {

    private RulesEngine rulesEngine;

    @BeforeEach
    void setUp() {
        RulesEngineConfiguration config = new RulesEngineConfiguration();
        rulesEngine = new RulesEngine(config, new SpelExpressionParser(),
                                    new ErrorRecoveryService(), new RulePerformanceMonitor(), null);
    }

    @Test
    @DisplayName("Demonstrate new clean API for success cases")
    void testNewApiSuccessCase() {
        System.out.println("=== NEW API DEMONSTRATION: SUCCESS CASE ===");
        
        // Create a rule group that will match
        RuleGroup successGroup = new RuleGroup("success-group", "test", "Success Group", 
                                              "All rules pass", 10, true, true, false, false);
        successGroup.addRule(new Rule("rule1", "true", "Rule 1 passed", "INFO"), 1);
        successGroup.addRule(new Rule("rule2", "true", "Rule 2 passed", "WARNING"), 2);
        
        // Execute the rule group
        RuleResult result = rulesEngine.executeRuleGroupsList(java.util.List.of(successGroup), new HashMap<>());
        
        // Demonstrate NEW CLEAN API
        System.out.println("\n--- NEW CLEAN API (Success Case) ---");
        System.out.println("result.isTriggered(): " + result.isTriggered());
        System.out.println("result.getRuleMatchedName(): " + result.getRuleMatchedName());
        System.out.println("result.getLastFailedGroupName(): " + result.getLastFailedGroupName());
        System.out.println("result.getLastFailedGroupMessage(): " + result.getLastFailedGroupMessage());
        System.out.println("result.getHighestFailedSeverity(): " + result.getHighestFailedSeverity());
        
        // Verify new API behavior
        assertTrue(result.isTriggered(), "Rule group should match");
        assertEquals("Success Group", result.getRuleMatchedName(), "Should return matched group name");
        assertNull(result.getLastFailedGroupName(), "No failure info when rule matched");
        assertNull(result.getLastFailedGroupMessage(), "No failure info when rule matched");
        assertNull(result.getHighestFailedSeverity(), "No failure info when rule matched");
        
        // Show backward compatibility
        System.out.println("\n--- BACKWARD COMPATIBILITY ---");
        @SuppressWarnings("deprecation")
        String deprecatedRuleName = result.getRuleName();
        System.out.println("result.getRuleName() [DEPRECATED]: " + deprecatedRuleName);
        assertEquals("Success Group", deprecatedRuleName, "Backward compatibility preserved");
        
        System.out.println("\n✅ SUCCESS CASE: New API provides clear semantics!");
    }

    @Test
    @DisplayName("Demonstrate new clean API for failure cases")
    void testNewApiFailureCase() {
        System.out.println("\n=== NEW API DEMONSTRATION: FAILURE CASE ===");
        
        // Create a rule group that will fail
        RuleGroup failureGroup = new RuleGroup("failure-group", "test", "Failure Group", 
                                              "Some rules fail", 10, true, true, false, false);
        failureGroup.addRule(new Rule("rule1", "true", "Rule 1 passed", "INFO"), 1);
        failureGroup.addRule(new Rule("rule2", "false", "Rule 2 failed", "ERROR"), 2);  // This will fail
        failureGroup.addRule(new Rule("rule3", "true", "Rule 3 passed", "WARNING"), 3);
        
        // Execute the rule group
        RuleResult result = rulesEngine.executeRuleGroupsList(java.util.List.of(failureGroup), new HashMap<>());
        
        // Demonstrate NEW CLEAN API
        System.out.println("\n--- NEW CLEAN API (Failure Case) ---");
        System.out.println("result.isTriggered(): " + result.isTriggered());
        System.out.println("result.getRuleMatchedName(): " + result.getRuleMatchedName());
        System.out.println("result.getLastFailedGroupName(): " + result.getLastFailedGroupName());
        System.out.println("result.getLastFailedGroupMessage(): " + result.getLastFailedGroupMessage());
        System.out.println("result.getHighestFailedSeverity(): " + result.getHighestFailedSeverity());
        
        // Verify new API behavior
        assertFalse(result.isTriggered(), "Rule group should fail");
        assertNull(result.getRuleMatchedName(), "No matched rule when group fails");
        assertEquals("Failure Group", result.getLastFailedGroupName(), "Should return failed group name");
        assertNotNull(result.getLastFailedGroupMessage(), "Should have failure message");
        assertEquals("ERROR", result.getHighestFailedSeverity(), "Should return highest failed severity");
        
        // Show backward compatibility
        System.out.println("\n--- BACKWARD COMPATIBILITY ---");
        @SuppressWarnings("deprecation")
        String deprecatedRuleName = result.getRuleName();
        System.out.println("result.getRuleName() [DEPRECATED]: " + deprecatedRuleName);
        assertEquals("Failure Group", deprecatedRuleName, "Backward compatibility preserved");
        
        System.out.println("\n✅ FAILURE CASE: New API provides rich failure diagnostics!");
    }

    @Test
    @DisplayName("Compare old confusing behavior vs new clear behavior")
    void testApiComparison() {
        System.out.println("\n=== API COMPARISON: OLD vs NEW ===");
        
        // Create a failing rule group
        RuleGroup group = new RuleGroup("test-group", "test", "Test Group", 
                                       "Test message", 10, true, true, false, false);
        group.addRule(new Rule("rule1", "false", "Failed rule", "ERROR"), 1);
        
        RuleResult result = rulesEngine.executeRuleGroupsList(java.util.List.of(group), new HashMap<>());
        
        System.out.println("\n--- OLD CONFUSING API ---");
        System.out.println("❌ PROBLEM: getRuleName() returns failed group name, not matched rule name");
        @SuppressWarnings("deprecation")
        String oldApi = result.getRuleName();
        System.out.println("result.getRuleName(): " + oldApi + " (confusing - is this a matched rule or failed group?)");
        
        System.out.println("\n--- NEW CLEAR API ---");
        System.out.println("✅ SOLUTION: Separate methods with clear semantics");
        System.out.println("result.getRuleMatchedName(): " + result.getRuleMatchedName() + " (null = no rule matched)");
        System.out.println("result.getLastFailedGroupName(): " + result.getLastFailedGroupName() + " (diagnostic info)");
        System.out.println("result.getHighestFailedSeverity(): " + result.getHighestFailedSeverity() + " (failure severity)");
        
        System.out.println("\n🎯 RESULT: API now has clear semantics and rich failure diagnostics!");
    }
}
