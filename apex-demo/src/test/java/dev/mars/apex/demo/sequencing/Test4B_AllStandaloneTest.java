package dev.mars.apex.demo.sequencing.order_guarantee;

import dev.mars.apex.core.engine.config.RulesEngine;
import dev.mars.apex.demo.DemoTestBase;
import dev.mars.apex.demo.sequencing.ExecutionTracker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test 4B: All Standalone Enrichments
 * 
 * Proves: When NO groups exist, ALL enrichments execute at their definition positions
 * 
 * YAML Structure:
 * - 4 standalone enrichments (NOT in any group)
 * - NO enrichment-groups section
 * 
 * Expected Execution Order:
 * 1. standalone-1 (position 1)
 * 2. standalone-2 (position 2)
 * 3. standalone-3 (position 3)
 * 4. standalone-4 (position 4)
 * 
 * Definitive Assertions (5 types):
 * 1. EXACT execution count: 4 items
 * 2. EXACT execution order: standalone-1, standalone-2, standalone-3, standalone-4
 * 3. Verify what executed: All 4 standalone enrichments
 * 4. Verify NO double execution: Each item executes exactly once
 * 5. Verify execution positions: Each item at correct position
 */
public class Test4B_AllStandaloneTest extends DemoTestBase {
    private static final Logger LOGGER = LoggerFactory.getLogger(Test4B_AllStandaloneTest.class);

    @BeforeEach
    public void clearTracker() {
        ExecutionTracker.clear();
    }

    @Test
    public void testAllStandaloneEnrichments() throws Exception {
        LOGGER.info("=== TEST 4B: All Standalone Enrichments ===");

        // Create RulesEngine from YAML
        RulesEngine engine = RulesEngine.fromFile("src/test/java/dev/mars/apex/demo/sequencing/order_guarantee/Test4B_AllStandaloneTest.yaml");

        // Create test data
        Map<String, Object> data = new HashMap<>();

        // Execute
        engine.evaluate(data);

        // Get execution log
        List<String> executionLog = ExecutionTracker.getExecutionLog();
        LOGGER.info("Execution Log ({} items): {}", executionLog.size(), executionLog);

        // ===== DEFINITIVE ASSERTIONS (5 types) =====

        // 1. EXACT execution count
        assertEquals(4, executionLog.size(),
                "Should execute EXACTLY 4 items: all standalone enrichments");

        // 2. EXACT execution order
        List<String> expected = List.of("standalone-1", "standalone-2", "standalone-3", "standalone-4");
        assertEquals(expected, executionLog,
                "Execution order MUST be: standalone-1, standalone-2, standalone-3, standalone-4");

        // 3. Verify what executed
        assertTrue(executionLog.contains("standalone-1"), "standalone-1 MUST execute at position 1");
        assertTrue(executionLog.contains("standalone-2"), "standalone-2 MUST execute at position 2");
        assertTrue(executionLog.contains("standalone-3"), "standalone-3 MUST execute at position 3");
        assertTrue(executionLog.contains("standalone-4"), "standalone-4 MUST execute at position 4");

        // 4. Verify NO double execution
        assertEquals(1, Collections.frequency(executionLog, "standalone-1"),
                "standalone-1 MUST execute EXACTLY ONCE");
        assertEquals(1, Collections.frequency(executionLog, "standalone-2"),
                "standalone-2 MUST execute EXACTLY ONCE");
        assertEquals(1, Collections.frequency(executionLog, "standalone-3"),
                "standalone-3 MUST execute EXACTLY ONCE");
        assertEquals(1, Collections.frequency(executionLog, "standalone-4"),
                "standalone-4 MUST execute EXACTLY ONCE");

        // 5. Verify execution positions
        assertEquals("standalone-1", executionLog.get(0), "Position 0 MUST be standalone-1");
        assertEquals("standalone-2", executionLog.get(1), "Position 1 MUST be standalone-2");
        assertEquals("standalone-3", executionLog.get(2), "Position 2 MUST be standalone-3");
        assertEquals("standalone-4", executionLog.get(3), "Position 3 MUST be standalone-4");

        LOGGER.info("✓ TEST 4B PASSED: All 5 definitive assertion types verified");
        LOGGER.info("  ✓ Exact count: 4 items");
        LOGGER.info("  ✓ Exact order: standalone-1, standalone-2, standalone-3, standalone-4");
        LOGGER.info("  ✓ What executed: All 4 standalone enrichments");
        LOGGER.info("  ✓ No double execution: Each item executes exactly once");
        LOGGER.info("  ✓ Position verification: All items at correct positions");
    }
}

