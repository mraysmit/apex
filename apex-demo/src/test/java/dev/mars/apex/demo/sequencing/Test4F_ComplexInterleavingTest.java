package dev.mars.apex.demo.sequencing;

import dev.mars.apex.core.engine.config.RulesEngine;
import dev.mars.apex.demo.DemoTestBase;
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
 * Test 4F: Complex Interleaving
 * 
 * Proves: Complex patterns of standalone and grouped items work correctly
 * 
 * YAML Structure:
 * - 3 standalone enrichments (positions 1, 3, 5)
 * - 3 grouped enrichments (positions 2, 4, 6)
 * - 2 enrichment-groups (group-A with 2 enrichments, group-B with 1 enrichment)
 * 
 * Expected Execution Order:
 * 1. standalone-1 (position 1)
 * 2. standalone-2 (position 3)
 * 3. standalone-3 (position 5)
 * 4. grouped-1 (via group-A at position 7)
 * 5. grouped-2 (via group-A at position 7)
 * 6. grouped-3 (via group-B at position 8)
 * 
 * Definitive Assertions (5 types):
 * 1. EXACT execution count: 6 items
 * 2. EXACT execution order: standalone-1, standalone-2, standalone-3, grouped-1, grouped-2, grouped-3
 * 3. Verify what executed: All 6 enrichments
 * 4. Verify NO double execution: Each item executes exactly once
 * 5. Verify execution positions: Each item at correct position
 */
public class Test4F_ComplexInterleavingTest extends DemoTestBase {
    private static final Logger LOGGER = LoggerFactory.getLogger(Test4F_ComplexInterleavingTest.class);

    @BeforeEach
    public void clearTracker() {
        ExecutionTracker.clear();
    }

    @Test
    public void testComplexInterleaving() throws Exception {
        LOGGER.info("=== TEST 4F: Complex Interleaving ===");

        // Create RulesEngine from YAML
        RulesEngine engine = RulesEngine.fromClasspath("dev/mars/apex/demo/sequencing/Test4F_ComplexInterleavingTest.yaml");

        // Create test data
        Map<String, Object> data = new HashMap<>();

        // Execute
        engine.evaluate(data);

        // Get execution log
        List<String> executionLog = ExecutionTracker.getExecutionLog();
        LOGGER.info("Execution Log ({} items): {}", executionLog.size(), executionLog);

        // ===== DEFINITIVE ASSERTIONS (5 types) =====

        // 1. EXACT execution count
        assertEquals(6, executionLog.size(),
                "Should execute EXACTLY 6 items: 3 standalone + 3 via groups");

        // 2. EXACT execution order
        List<String> expected = List.of("standalone-1", "standalone-2", "standalone-3", "grouped-1", "grouped-2", "grouped-3");
        assertEquals(expected, executionLog,
                "Execution order MUST be: standalone-1, standalone-2, standalone-3, grouped-1, grouped-2 (via group-A), grouped-3 (via group-B)");

        // 3. Verify what executed
        assertTrue(executionLog.contains("standalone-1"), "standalone-1 MUST execute at position 1");
        assertTrue(executionLog.contains("standalone-2"), "standalone-2 MUST execute at position 3");
        assertTrue(executionLog.contains("standalone-3"), "standalone-3 MUST execute at position 5");
        assertTrue(executionLog.contains("grouped-1"), "grouped-1 MUST execute via group-A");
        assertTrue(executionLog.contains("grouped-2"), "grouped-2 MUST execute via group-A");
        assertTrue(executionLog.contains("grouped-3"), "grouped-3 MUST execute via group-B");

        // 4. Verify NO double execution
        assertEquals(1, Collections.frequency(executionLog, "standalone-1"),
                "standalone-1 MUST execute EXACTLY ONCE");
        assertEquals(1, Collections.frequency(executionLog, "standalone-2"),
                "standalone-2 MUST execute EXACTLY ONCE");
        assertEquals(1, Collections.frequency(executionLog, "standalone-3"),
                "standalone-3 MUST execute EXACTLY ONCE");
        assertEquals(1, Collections.frequency(executionLog, "grouped-1"),
                "grouped-1 MUST execute EXACTLY ONCE (via group only, NOT at position 2)");
        assertEquals(1, Collections.frequency(executionLog, "grouped-2"),
                "grouped-2 MUST execute EXACTLY ONCE (via group only, NOT at position 4)");
        assertEquals(1, Collections.frequency(executionLog, "grouped-3"),
                "grouped-3 MUST execute EXACTLY ONCE (via group only, NOT at position 6)");

        // 5. Verify execution positions
        assertEquals("standalone-1", executionLog.get(0), "Position 0 MUST be standalone-1");
        assertEquals("standalone-2", executionLog.get(1), "Position 1 MUST be standalone-2");
        assertEquals("standalone-3", executionLog.get(2), "Position 2 MUST be standalone-3");
        assertEquals("grouped-1", executionLog.get(3), "Position 3 MUST be grouped-1 (from group-A)");
        assertEquals("grouped-2", executionLog.get(4), "Position 4 MUST be grouped-2 (from group-A)");
        assertEquals("grouped-3", executionLog.get(5), "Position 5 MUST be grouped-3 (from group-B)");

        LOGGER.info("✓ TEST 4F PASSED: All 5 definitive assertion types verified");
        LOGGER.info("  ✓ Exact count: 6 items");
        LOGGER.info("  ✓ Exact order: standalone-1, standalone-2, standalone-3, grouped-1, grouped-2, grouped-3");
        LOGGER.info("  ✓ What executed: All 6 enrichments");
        LOGGER.info("  ✓ No double execution: Each item executes exactly once");
        LOGGER.info("  ✓ Position verification: All items at correct positions");
        LOGGER.info("  ✓ Complex interleaving: Handled correctly");
    }
}

