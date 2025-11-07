package dev.mars.apex.demo.sequencing.order_guarantee;

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
 * Test 4: Standalone Enrichments with Groups
 * 
 * CRITICAL TEST: Proves that enrichments NOT referenced by enrichment-groups execute directly.
 * 
 * This test definitively proves the groups-only logic:
 * - Enrichments referenced by groups → definitions only (executed by group)
 * - Enrichments NOT referenced by groups → execute directly (standalone)
 * 
 * YAML Structure:
 * - standalone-1 (NOT referenced by any group)
 * - grouped-1 (referenced by group-A)
 * - standalone-2 (NOT referenced by any group)
 * - grouped-2 (referenced by group-A)
 * - group-A (references grouped-1, grouped-2)
 * 
 * Expected itemOrder: [standalone-1, standalone-2, group-A]
 * Expected ExecutionTracker: [standalone-1, standalone-2, grouped-1, grouped-2]
 */
public class Test4_StandaloneEnrichmentsTest extends DemoTestBase {

    private static final Logger LOGGER = LoggerFactory.getLogger(Test4_StandaloneEnrichmentsTest.class);

    @BeforeEach
    public void clearTracker() {
        ExecutionTracker.clear();
    }

    @Test
    @DisplayName("TEST 4: Standalone enrichments execute directly, grouped enrichments execute via group")
    public void testStandaloneEnrichmentsWithGroups() throws Exception {
        LOGGER.info("=== TEST 4: Standalone Enrichments with Groups ===");

        // Arrange
        String yamlPath = "src/test/java/dev/mars/apex/demo/sequencing/order_guarantee/test4-main.yaml";
        RulesEngine engine = RulesEngine.fromFile(yamlPath);

        Map<String, Object> testData = new HashMap<>();
        testData.put("value", "test");

        // Act
        RuleResult result = engine.evaluate(testData);

        // Assert
        assertTrue(result.isSuccess(), "Should succeed");
        
        List<String> executionLog = ExecutionTracker.getExecutionLog();
        LOGGER.info("Execution Log ({} items): {}", executionLog.size(), executionLog);

        // Expected execution order:
        // 1. standalone-1 (executes directly - not in any group)
        // 2. standalone-2 (executes directly - not in any group)
        // 3. grouped-1 (executed by group-A)
        // 4. grouped-2 (executed by group-A)
        
        assertEquals(4, executionLog.size(), "Should execute 4 enrichments");
        assertEquals("standalone-1", executionLog.get(0), "Position 1: standalone-1 executes directly");
        assertEquals("standalone-2", executionLog.get(1), "Position 2: standalone-2 executes directly");
        assertEquals("grouped-1", executionLog.get(2), "Position 3: grouped-1 executed by group-A");
        assertEquals("grouped-2", executionLog.get(3), "Position 4: grouped-2 executed by group-A");
        
        LOGGER.info("✅ TEST 4 PASSED: Standalone enrichments execute directly, grouped enrichments execute via group");
    }
}

