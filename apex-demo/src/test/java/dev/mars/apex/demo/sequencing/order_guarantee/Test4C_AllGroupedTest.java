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
 * Test 4C: All Grouped Enrichments
 * 
 * Proves: When ALL enrichments are in groups, NONE execute at definition position
 * 
 * YAML Structure:
 * - 4 grouped enrichments (ALL in groups)
 * - 2 enrichment-groups (group-A with 2 enrichments, group-B with 2 enrichments)
 * 
 * Expected Execution Order:
 * 1. grouped-1 (via group-A at position 5)
 * 2. grouped-2 (via group-A at position 5)
 * 3. grouped-3 (via group-B at position 6)
 * 4. grouped-4 (via group-B at position 6)
 * 
 * Definitive Assertions (5 types):
 * 1. EXACT execution count: 4 items
 * 2. EXACT execution order: grouped-1, grouped-2, grouped-3, grouped-4
 * 3. Verify what executed: All 4 grouped enrichments via groups
 * 4. Verify NO double execution: Each item executes exactly once
 * 5. Verify execution positions: Each item at correct position
 */
public class Test4C_AllGroupedTest extends DemoTestBase {
    private static final Logger LOGGER = LoggerFactory.getLogger(Test4C_AllGroupedTest.class);

    @BeforeEach
    public void clearTracker() {
        ExecutionTracker.clear();
    }

    @Test
    public void testAllGroupedEnrichments() throws Exception {
        LOGGER.info("=== TEST 4C: All Grouped Enrichments ===");

        // Create RulesEngine from YAML
        RulesEngine engine = RulesEngine.fromFile("src/test/java/dev/mars/apex/demo/sequencing/order_guarantee/Test4C_AllGroupedTest.yaml");

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
                "Should execute EXACTLY 4 items: all via groups");

        // 2. EXACT execution order
        List<String> expected = List.of("grouped-1", "grouped-2", "grouped-3", "grouped-4");
        assertEquals(expected, executionLog,
                "Execution order MUST be: grouped-1, grouped-2 (via group-A), grouped-3, grouped-4 (via group-B)");

        // 3. Verify what executed
        assertTrue(executionLog.contains("grouped-1"), "grouped-1 MUST execute via group-A");
        assertTrue(executionLog.contains("grouped-2"), "grouped-2 MUST execute via group-A");
        assertTrue(executionLog.contains("grouped-3"), "grouped-3 MUST execute via group-B");
        assertTrue(executionLog.contains("grouped-4"), "grouped-4 MUST execute via group-B");

        // 4. Verify NO double execution
        assertEquals(1, Collections.frequency(executionLog, "grouped-1"),
                "grouped-1 MUST execute EXACTLY ONCE (via group only, NOT at definition position)");
        assertEquals(1, Collections.frequency(executionLog, "grouped-2"),
                "grouped-2 MUST execute EXACTLY ONCE (via group only, NOT at definition position)");
        assertEquals(1, Collections.frequency(executionLog, "grouped-3"),
                "grouped-3 MUST execute EXACTLY ONCE (via group only, NOT at definition position)");
        assertEquals(1, Collections.frequency(executionLog, "grouped-4"),
                "grouped-4 MUST execute EXACTLY ONCE (via group only, NOT at definition position)");

        // 5. Verify execution positions
        assertEquals("grouped-1", executionLog.get(0), "Position 0 MUST be grouped-1 (from group-A)");
        assertEquals("grouped-2", executionLog.get(1), "Position 1 MUST be grouped-2 (from group-A)");
        assertEquals("grouped-3", executionLog.get(2), "Position 2 MUST be grouped-3 (from group-B)");
        assertEquals("grouped-4", executionLog.get(3), "Position 3 MUST be grouped-4 (from group-B)");

        LOGGER.info("✓ TEST 4C PASSED: All 5 definitive assertion types verified");
        LOGGER.info("  ✓ Exact count: 4 items");
        LOGGER.info("  ✓ Exact order: grouped-1, grouped-2, grouped-3, grouped-4");
        LOGGER.info("  ✓ What executed: All 4 grouped enrichments via groups");
        LOGGER.info("  ✓ No double execution: Each item executes exactly once");
        LOGGER.info("  ✓ Position verification: All items at correct positions");
    }
}

