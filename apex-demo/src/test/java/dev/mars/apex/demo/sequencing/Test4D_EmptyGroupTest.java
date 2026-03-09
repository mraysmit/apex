package dev.mars.apex.demo.sequencing;

import dev.mars.apex.engine.core.RulesEngine;
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
 * Test 4D: Empty Group Edge Case
 * 
 * Proves: Empty groups don't break the system
 * 
 * YAML Structure:
 * - 1 standalone enrichment
 * - 1 grouped enrichment
 * - 1 enrichment-group with 1 enrichment
 * - 1 empty enrichment-group
 * 
 * Expected Execution Order:
 * 1. standalone-1 (position 1)
 * 2. grouped-1 (via group-A at position 3)
 * 
 * Definitive Assertions (5 types):
 * 1. EXACT execution count: 2 items
 * 2. EXACT execution order: standalone-1, grouped-1
 * 3. Verify what executed: standalone-1 and grouped-1
 * 4. Verify NO double execution: Each item executes exactly once
 * 5. Verify execution positions: Each item at correct position
 */
public class Test4D_EmptyGroupTest extends DemoTestBase {
    private static final Logger LOGGER = LoggerFactory.getLogger(Test4D_EmptyGroupTest.class);

    @BeforeEach
    public void clearTracker() {
        ExecutionTracker.clear();
    }

    @Test
    public void testEmptyGroup() throws Exception {
        LOGGER.info("=== TEST 4D: Empty Group Edge Case ===");

        // Create RulesEngine from YAML
        RulesEngine engine = RulesEngine.fromClasspath("dev/mars/apex/demo/sequencing/Test4D_EmptyGroupTest.yaml");

        // Create test data
        Map<String, Object> data = new HashMap<>();

        // Execute
        engine.evaluate(data);

        // Get execution log
        List<String> executionLog = ExecutionTracker.getExecutionLog();
        LOGGER.info("Execution Log ({} items): {}", executionLog.size(), executionLog);

        // ===== DEFINITIVE ASSERTIONS (5 types) =====

        // 1. EXACT execution count
        assertEquals(2, executionLog.size(),
                "Should execute EXACTLY 2 items: 1 standalone + 1 via group");

        // 2. EXACT execution order
        List<String> expected = List.of("standalone-1", "grouped-1");
        assertEquals(expected, executionLog,
                "Execution order MUST be: standalone-1, grouped-1 (via group-A)");

        // 3. Verify what executed
        assertTrue(executionLog.contains("standalone-1"), "standalone-1 MUST execute at position 1");
        assertTrue(executionLog.contains("grouped-1"), "grouped-1 MUST execute via group-A");

        // 4. Verify NO double execution
        assertEquals(1, Collections.frequency(executionLog, "standalone-1"),
                "standalone-1 MUST execute EXACTLY ONCE");
        assertEquals(1, Collections.frequency(executionLog, "grouped-1"),
                "grouped-1 MUST execute EXACTLY ONCE (via group only, NOT at definition position)");

        // 5. Verify execution positions
        assertEquals("standalone-1", executionLog.get(0), "Position 0 MUST be standalone-1");
        assertEquals("grouped-1", executionLog.get(1), "Position 1 MUST be grouped-1 (from group-A)");

        LOGGER.info("[OK] TEST 4D PASSED: All 5 definitive assertion types verified");
        LOGGER.info("  [OK] Exact count: 2 items");
        LOGGER.info("  [OK] Exact order: standalone-1, grouped-1");
        LOGGER.info("  [OK] What executed: standalone-1 and grouped-1");
        LOGGER.info("  [OK] No double execution: Each item executes exactly once");
        LOGGER.info("  [OK] Position verification: All items at correct positions");
        LOGGER.info("  [OK] Edge case: Empty group handled gracefully");
    }
}

