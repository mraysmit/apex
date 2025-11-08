package dev.mars.apex.demo.sequencing;

import dev.mars.apex.core.config.yaml.YamlRuleConfiguration;
import dev.mars.apex.core.engine.config.RulesEngine;
import dev.mars.apex.core.engine.model.RuleResult;
import dev.mars.apex.demo.DemoTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test 2: Prove that when external file has BOTH enrichments and enrichment-groups,
 * ONLY enrichment-groups execute (not individual enrichments).
 * 
 * YAML Structure:
 * Main file:
 * 1. inline-before
 * 2. enrichment-refs -> external file
 * 3. inline-after
 * 
 * External file:
 * - enrichment-groups: [group1 with e1, e2]
 * - enrichments: [e1, e2]
 * 
 * Expected Execution Order:
 * 1. inline-before
 * 2. e1 (from group1, NOT from enrichments section)
 * 3. e2 (from group1, NOT from enrichments section)
 * 4. inline-after
 * 
 * Expected Execution Count:
 * - e1: 1 time ONLY (not twice)
 * - e2: 1 time ONLY (not twice)
 * 
 * This test DEFINITIVELY PROVES that:
 * - ONLY enrichment-groups execute (not individual enrichments)
 * - Enrichments section is for DEFINITION only
 * - Groups section is for EXECUTION control
 * - NO double execution
 * - Group-defined order is respected
 */
public class Test2_EnrichmentGroupsOnlyTest extends DemoTestBase {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(Test2_EnrichmentGroupsOnlyTest.class);
    
    @BeforeEach
    public void clearExecutionTracker() {
        ExecutionTracker.clear();
    }
    
    @Test
    @DisplayName("Test 2: ONLY enrichment-groups execute (not individual enrichments)")
    public void testOnlyEnrichmentGroupsExecute() throws Exception {
        LOGGER.info("=== TEST 2: Enrichment-Groups ONLY Guarantee ===");
        
        // Load YAML configuration
        String yamlPath = "src/test/java/dev/mars/apex/demo/sequencing/Test2_EnrichmentGroupsOnlyTest.yaml";
        RulesEngine engine = RulesEngine.fromFile(yamlPath);
        YamlRuleConfiguration config = yamlLoader.loadFromFile(yamlPath);

        // Create test data
        Map<String, Object> data = new HashMap<>();
        data.put("testId", "test2");

        // Execute rules engine
        RuleResult result = engine.evaluate(config, data);
        
        // Print execution log for debugging
        LOGGER.info("Execution Log:");
        ExecutionTracker.printLog();
        
        // Get actual execution order
        List<String> actualOrder = ExecutionTracker.getExecutionLog();
        
        // Define expected execution order
        List<String> expectedOrder = List.of(
            "inline-before",
            "e1",  // From group1, NOT from enrichments section
            "e2",  // From group1, NOT from enrichments section
            "inline-after"
        );
        
        // CRITICAL ASSERTION 1: Execution order MUST match expected
        assertEquals(expectedOrder, actualOrder, 
            "ENRICHMENT-GROUPS ONLY GUARANTEE VIOLATED!\n" +
            "Expected: " + expectedOrder + "\n" +
            "Actual:   " + actualOrder + "\n" +
            "When both enrichments and enrichment-groups exist, ONLY groups should execute");
        
        // CRITICAL ASSERTION 2: NO double execution
        long e1Count = ExecutionTracker.getExecutionCount("e1");
        long e2Count = ExecutionTracker.getExecutionCount("e2");
        
        assertEquals(1, e1Count, 
            "DOUBLE EXECUTION DETECTED!\n" +
            "e1 executed " + e1Count + " times, expected 1\n" +
            "Enrichments should execute ONLY as part of groups, not individually");
        
        assertEquals(1, e2Count, 
            "DOUBLE EXECUTION DETECTED!\n" +
            "e2 executed " + e2Count + " times, expected 1\n" +
            "Enrichments should execute ONLY as part of groups, not individually");
        
        // Verify execution positions
        assertEquals(2, ExecutionTracker.getExecutionPosition("e1"), 
            "e1 must execute at position 2 (from group, not from enrichments section)");
        assertEquals(3, ExecutionTracker.getExecutionPosition("e2"), 
            "e2 must execute at position 3 (from group, not from enrichments section)");
        
        LOGGER.info("✅ TEST 2 PASSED: ONLY enrichment-groups execute (no double execution)");
    }
}

