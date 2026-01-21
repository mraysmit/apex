package dev.mars.apex.demo.sequencing;

import dev.mars.apex.core.config.yaml.YamlConfigurationLoader;
import dev.mars.apex.core.config.yaml.YamlRuleConfiguration;
import dev.mars.apex.core.engine.config.RulesEngine;
import dev.mars.apex.core.engine.model.RuleResult;
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
 * Test to verify when enrichment-group-references should execute.
 * 
 * This test demonstrates the difference between:
 * - FLATTENING: Referenced group's enrichments are copied into the target group
 * - NESTED EXECUTION: Referenced group executes when the target group references it
 * 
 * Current Implementation: FLATTENING (Phase 2 in EnrichmentGroupFactory)
 * User Expectation: NESTED EXECUTION (rbg1 should execute when e2_eg references it)
 */
@DisplayName("Enrichment Group References Execution Order Test")
public class TestEnrichmentGroupReferencesExecutionOrder {

    private static final Logger LOGGER = LoggerFactory.getLogger(TestEnrichmentGroupReferencesExecutionOrder.class);
    private final YamlConfigurationLoader yamlLoader = new YamlConfigurationLoader();

    @BeforeEach
    public void setUp() {
        ExecutionTracker.clear();
    }

    @Test
    @DisplayName("Verify enrichment-group-references execution order")
    public void testEnrichmentGroupReferencesExecutionOrder() throws Exception {
        LOGGER.info("=== TEST: Enrichment Group References Execution Order ===");

        // Load YAML configuration
        String yamlPath = "src/test/java/dev/mars/apex/demo/sequencing/TestEnrichmentGroupReferencesExecutionOrder.yaml";
        RulesEngine engine = RulesEngine.fromFile(yamlPath);
        YamlRuleConfiguration config = yamlLoader.loadFromFile(yamlPath);

        // Create test data
        Map<String, Object> data = new HashMap<>();
        data.put("testId", "enrichment-group-refs-test");

        // Execute rules engine
        RuleResult result = engine.evaluate(config, data);

        // Print execution log for debugging
        LOGGER.info("Execution Log:");
        ExecutionTracker.printLog();

        // Get actual execution order
        List<String> actualOrder = ExecutionTracker.getExecutionLog();

        LOGGER.info("=== ANALYSIS ===");
        LOGGER.info("Actual execution order: {}", actualOrder);
        
        // Current behavior (FLATTENING):
        // - rbg1's enrichments are copied into e2_eg during factory build
        // - Expected order: [e1, rbg1_enrichment, e2]
        // - rbg1 never executes as a separate group
        
        // Expected behavior (NESTED EXECUTION):
        // - rbg1 should execute when e2_eg references it
        // - Expected order: [e1, rbg1_enrichment, e2]
        // - But rbg1 should be tracked as executing within e2_eg context
        
        LOGGER.info("=== CURRENT BEHAVIOR (FLATTENING) ===");
        LOGGER.info("- rbg1's enrichments are COPIED into e2_eg during factory build");
        LOGGER.info("- rbg1 never executes as a separate group");
        LOGGER.info("- Execution order: e1_eg(e1) → e2_eg(rbg1_enrichment) → e3_eg(e2)");
        
        LOGGER.info("=== EXPECTED BEHAVIOR (NESTED EXECUTION) ===");
        LOGGER.info("- rbg1 should execute WHEN e2_eg references it");
        LOGGER.info("- rbg1 should execute as a nested call from e2_eg");
        LOGGER.info("- Execution order: e1_eg(e1) → e2_eg → rbg1(rbg1_enrichment) → e3_eg(e2)");
        
        // For now, verify the current flattening behavior
        List<String> expectedFlattenedOrder = List.of(
            "e1",              // From e1_eg
            "rbg1_enrichment", // From e2_eg (flattened from rbg1)
            "e2"               // From e3_eg
        );
        
        assertEquals(expectedFlattenedOrder, actualOrder,
            "Current behavior: rbg1's enrichments are flattened into e2_eg");
        
        LOGGER.info("Test PASSED - Current flattening behavior verified");
        LOGGER.info("⚠️  NOTE: This may not match user expectation of nested execution");
    }
    
    @Test
    @DisplayName("Verify rbg1 is not executed as a separate group")
    public void testReferencedGroupNotExecutedSeparately() throws Exception {
        LOGGER.info("=== TEST: Referenced Group Not Executed Separately ===");

        // Load YAML configuration
        String yamlPath = "src/test/java/dev/mars/apex/demo/sequencing/TestEnrichmentGroupReferencesExecutionOrder.yaml";
        RulesEngine engine = RulesEngine.fromFile(yamlPath);
        YamlRuleConfiguration config = yamlLoader.loadFromFile(yamlPath);

        // Create test data
        Map<String, Object> data = new HashMap<>();
        data.put("testId", "enrichment-group-refs-test");

        // Execute rules engine
        RuleResult result = engine.evaluate(config, data);

        // Get actual execution order
        List<String> actualOrder = ExecutionTracker.getExecutionLog();

        LOGGER.info("Execution order: {}", actualOrder);
        
        // Verify that rbg1_enrichment executes exactly once
        long rbg1Count = ExecutionTracker.getExecutionCount("rbg1_enrichment");
        assertEquals(1, rbg1Count, "rbg1_enrichment should execute exactly once");
        
        // Verify that rbg1 is not in the execution order as a separate group
        // (it's flattened into e2_eg)
        assertFalse(actualOrder.contains("rbg1"), 
            "rbg1 should not appear as a separate group in execution order (it's flattened)");
        
        LOGGER.info("Test PASSED - rbg1 is flattened, not executed separately");
    }
}

