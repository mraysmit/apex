package dev.mars.apex.demo.sequencing.order_guarantee;

import dev.mars.apex.core.config.yaml.YamlRuleConfiguration;
import dev.mars.apex.core.engine.config.RulesEngine;
import dev.mars.apex.core.engine.model.RuleResult;
import dev.mars.apex.demo.DemoTestBase;
import dev.mars.apex.demo.sequencing.ExecutionTracker;
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
 * Test 1: Prove that enrichment-refs executes at EXACT reference position.
 * 
 * YAML Structure:
 * 1. inline-before (inline enrichment)
 * 2. enrichment-refs (reference to external file)
 * 3. inline-after (inline rule)
 *
 * Expected Execution Order:
 * 1. inline-before
 * 2. external-1 (from external file)
 * 3. external-2 (from external file)
 * 4. inline-after (rule)
 * 
 * This test DEFINITIVELY PROVES that:
 * - enrichment-refs executes at EXACT position in YAML
 * - External enrichments execute in THEIR document order
 * - Inline enrichments before/after are not affected
 */
public class Test1_EnrichmentRefsPositionTest extends DemoTestBase {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(Test1_EnrichmentRefsPositionTest.class);
    
    @BeforeEach
    public void clearExecutionTracker() {
        ExecutionTracker.clear();
    }
    
    @Test
    @DisplayName("Test 1: enrichment-refs executes at EXACT reference position")
    public void testEnrichmentRefsExecutesAtReferencePosition() throws Exception {
        LOGGER.info("=== TEST 1: enrichment-refs Position Guarantee ===");
        
        // Load YAML configuration
        String yamlPath = "src/test/java/dev/mars/apex/demo/sequencing/order_guarantee/test1-main.yaml";
        RulesEngine engine = RulesEngine.fromFile(yamlPath);
        YamlRuleConfiguration config = yamlLoader.loadFromFile(yamlPath);

        // Create test data
        Map<String, Object> data = new HashMap<>();
        data.put("testId", "test1");

        // Execute rules engine
        RuleResult result = engine.evaluate(config, data);
        
        // Print execution log for debugging
        LOGGER.info("Execution Log:");
        ExecutionTracker.printLog();
        
        // Get actual execution order
        List<String> actualOrder = ExecutionTracker.getExecutionLog();
        
        // Define expected execution order (YAML document order)
        List<String> expectedOrder = List.of(
            "inline-before",
            "external-1",
            "external-2",
            "inline-after"
        );
        
        // CRITICAL ASSERTION: Execution order MUST match YAML document order EXACTLY
        assertEquals(expectedOrder, actualOrder, 
            "YAML DOCUMENT ORDER VIOLATED!\n" +
            "Expected: " + expectedOrder + "\n" +
            "Actual:   " + actualOrder + "\n" +
            "enrichment-refs MUST execute at EXACT position in YAML document");
        
        // Verify each item executed exactly once
        assertEquals(1, ExecutionTracker.getExecutionCount("inline-before"), 
            "inline-before must execute exactly once");
        assertEquals(1, ExecutionTracker.getExecutionCount("external-1"), 
            "external-1 must execute exactly once");
        assertEquals(1, ExecutionTracker.getExecutionCount("external-2"), 
            "external-2 must execute exactly once");
        assertEquals(1, ExecutionTracker.getExecutionCount("inline-after"), 
            "inline-after must execute exactly once");
        
        // Verify execution positions
        assertEquals(1, ExecutionTracker.getExecutionPosition("inline-before"), 
            "inline-before must execute at position 1");
        assertEquals(2, ExecutionTracker.getExecutionPosition("external-1"), 
            "external-1 must execute at position 2");
        assertEquals(3, ExecutionTracker.getExecutionPosition("external-2"), 
            "external-2 must execute at position 3");
        assertEquals(4, ExecutionTracker.getExecutionPosition("inline-after"), 
            "inline-after must execute at position 4");
        
        LOGGER.info("✅ TEST 1 PASSED: enrichment-refs executes at EXACT reference position");
    }
}

