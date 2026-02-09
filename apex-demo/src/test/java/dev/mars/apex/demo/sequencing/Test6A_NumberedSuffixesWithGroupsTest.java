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
 * Test 6A: Numbered Suffixes with Groups-Only Logic
 * 
 * Tests that groups-only logic works correctly when enrichments are defined in numbered sections.
 * 
 * YAML Structure:
 * - enrichments-1: standalone-1, grouped-1
 * - enrichments-2: standalone-2, grouped-2
 * - enrichment-groups: group-A (references grouped-1, grouped-2)
 * 
 * Expected execution order:
 * 1. standalone-1 (from enrichments-1, NOT in group)
 * 2. standalone-2 (from enrichments-2, NOT in group)
 * 3. grouped-1 (from enrichments-1, via group-A)
 * 4. grouped-2 (from enrichments-2, via group-A)
 * 
 * Key verification:
 * - Enrichments in numbered sections are merged correctly
 * - Groups-only logic filters grouped enrichments from their definition positions
 * - Grouped enrichments execute via the group, not at definition position
 * - Standalone enrichments execute at their definition position
 */
public class Test6A_NumberedSuffixesWithGroupsTest extends DemoTestBase {

    private static final Logger LOGGER = LoggerFactory.getLogger(Test6A_NumberedSuffixesWithGroupsTest.class);

    @BeforeEach
    public void clearExecutionTracker() {
        ExecutionTracker.clear();
    }

    @Test
    public void testNumberedSuffixesWithGroups() throws Exception {
        LOGGER.info("=== TEST 6A: Numbered Suffixes with Groups-Only Logic ===");

        // Create RulesEngine from YAML
        RulesEngine engine = RulesEngine.fromClasspath("dev/mars/apex/demo/sequencing/Test6A_NumberedSuffixesWithGroupsTest.yaml");

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
                "Should execute EXACTLY 4 items: 2 standalone + 2 grouped");

        // 2. EXACT execution order
        List<String> expected = List.of(
            "standalone-1",  // from enrichments-1 (NOT in group)
            "standalone-2",  // from enrichments-2 (NOT in group)
            "grouped-1",     // from enrichments-1 (via group-A)
            "grouped-2"      // from enrichments-2 (via group-A)
        );
        assertEquals(expected, executionLog,
                "Execution order MUST be: standalone-1, standalone-2, grouped-1, grouped-2");

        // 3. Verify what executed
        assertTrue(executionLog.contains("standalone-1"), "standalone-1 MUST execute");
        assertTrue(executionLog.contains("standalone-2"), "standalone-2 MUST execute");
        assertTrue(executionLog.contains("grouped-1"), "grouped-1 MUST execute");
        assertTrue(executionLog.contains("grouped-2"), "grouped-2 MUST execute");

        // 4. Verify NO double execution
        assertEquals(1, Collections.frequency(executionLog, "standalone-1"),
                "standalone-1 MUST execute EXACTLY ONCE");
        assertEquals(1, Collections.frequency(executionLog, "standalone-2"),
                "standalone-2 MUST execute EXACTLY ONCE");
        assertEquals(1, Collections.frequency(executionLog, "grouped-1"),
                "grouped-1 MUST execute EXACTLY ONCE (via group, not at definition)");
        assertEquals(1, Collections.frequency(executionLog, "grouped-2"),
                "grouped-2 MUST execute EXACTLY ONCE (via group, not at definition)");

        // 5. Verify execution positions
        assertEquals("standalone-1", executionLog.get(0), "Position 0 MUST be standalone-1");
        assertEquals("standalone-2", executionLog.get(1), "Position 1 MUST be standalone-2");
        assertEquals("grouped-1", executionLog.get(2), "Position 2 MUST be grouped-1");
        assertEquals("grouped-2", executionLog.get(3), "Position 3 MUST be grouped-2");

        LOGGER.info("[OK] TEST 6A PASSED: All 5 definitive assertion types verified");
        LOGGER.info("  [OK] Exact count: 4 items");
        LOGGER.info("  [OK] Exact order: standalone-1, standalone-2, grouped-1, grouped-2");
        LOGGER.info("  [OK] What executed: 2 standalone + 2 grouped");
        LOGGER.info("  [OK] No double execution: Each item executes exactly once");
        LOGGER.info("  [OK] Position verification: All items at correct positions");
        LOGGER.info("  [OK] Groups-only logic: Works correctly with numbered suffixes");
        LOGGER.info("  [OK] Numbered suffixes: Merged correctly before groups-only logic applied");
    }
}

