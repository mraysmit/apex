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
 * Test 6B: Complex Numbered Suffixes with Multiple Groups
 * 
 * Tests complex interleaving of numbered sections and multiple groups.
 * 
 * YAML Structure (document order):
 * 1. enrichments-1: standalone-1, grouped-A1
 * 2. enrichment-groups-1: group-A (references grouped-A1, grouped-A2)
 * 3. enrichments-2: standalone-2, grouped-A2, grouped-B1
 * 4. enrichment-groups-2: group-B (references grouped-B1)
 * 5. enrichments-3: standalone-3
 * 
 * Expected execution order:
 * 1. standalone-1 (from enrichments-1, NOT in group)
 * 2. grouped-A1 (from enrichments-1, via group-A at position 2)
 * 3. grouped-A2 (from enrichments-2, via group-A at position 2)
 * 4. standalone-2 (from enrichments-2, NOT in group)
 * 5. grouped-B1 (from enrichments-2, via group-B at position 4)
 * 6. standalone-3 (from enrichments-3, NOT in group)
 */
public class Test6B_ComplexNumberedWithGroupsTest extends DemoTestBase {

    private static final Logger LOGGER = LoggerFactory.getLogger(Test6B_ComplexNumberedWithGroupsTest.class);

    @BeforeEach
    public void clearExecutionTracker() {
        ExecutionTracker.clear();
    }

    @Test
    public void testComplexNumberedWithGroups() throws Exception {
        LOGGER.info("=== TEST 6B: Complex Numbered Suffixes with Multiple Groups ===");

        // Create RulesEngine from YAML
        RulesEngine engine = RulesEngine.fromFile("src/test/java/dev/mars/apex/demo/sequencing/Test6B_ComplexNumberedWithGroupsTest.yaml");

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
                "Should execute EXACTLY 6 items: 3 standalone + 3 grouped");

        // 2. EXACT execution order
        List<String> expected = List.of(
            "standalone-1",  // Position 1: enrichments-1 (NOT in group)
            "grouped-A1",    // Position 2: via group-A
            "grouped-A2",    // Position 2: via group-A
            "standalone-2",  // Position 3: enrichments-2 (NOT in group)
            "grouped-B1",    // Position 4: via group-B
            "standalone-3"   // Position 5: enrichments-3 (NOT in group)
        );
        assertEquals(expected, executionLog,
                "Execution order MUST preserve document order with groups executing at their position");

        // 3. Verify what executed
        assertTrue(executionLog.contains("standalone-1"), "standalone-1 MUST execute");
        assertTrue(executionLog.contains("standalone-2"), "standalone-2 MUST execute");
        assertTrue(executionLog.contains("standalone-3"), "standalone-3 MUST execute");
        assertTrue(executionLog.contains("grouped-A1"), "grouped-A1 MUST execute");
        assertTrue(executionLog.contains("grouped-A2"), "grouped-A2 MUST execute");
        assertTrue(executionLog.contains("grouped-B1"), "grouped-B1 MUST execute");

        // 4. Verify NO double execution
        assertEquals(1, Collections.frequency(executionLog, "standalone-1"),
                "standalone-1 MUST execute EXACTLY ONCE");
        assertEquals(1, Collections.frequency(executionLog, "standalone-2"),
                "standalone-2 MUST execute EXACTLY ONCE");
        assertEquals(1, Collections.frequency(executionLog, "standalone-3"),
                "standalone-3 MUST execute EXACTLY ONCE");
        assertEquals(1, Collections.frequency(executionLog, "grouped-A1"),
                "grouped-A1 MUST execute EXACTLY ONCE (via group-A)");
        assertEquals(1, Collections.frequency(executionLog, "grouped-A2"),
                "grouped-A2 MUST execute EXACTLY ONCE (via group-A)");
        assertEquals(1, Collections.frequency(executionLog, "grouped-B1"),
                "grouped-B1 MUST execute EXACTLY ONCE (via group-B)");

        // 5. Verify execution positions
        assertEquals("standalone-1", executionLog.get(0), "Position 0 MUST be standalone-1");
        assertEquals("grouped-A1", executionLog.get(1), "Position 1 MUST be grouped-A1");
        assertEquals("grouped-A2", executionLog.get(2), "Position 2 MUST be grouped-A2");
        assertEquals("standalone-2", executionLog.get(3), "Position 3 MUST be standalone-2");
        assertEquals("grouped-B1", executionLog.get(4), "Position 4 MUST be grouped-B1");
        assertEquals("standalone-3", executionLog.get(5), "Position 5 MUST be standalone-3");

        LOGGER.info("✓ TEST 6B PASSED: All 5 definitive assertion types verified");
        LOGGER.info("  ✓ Exact count: 6 items");
        LOGGER.info("  ✓ Exact order: Complex interleaving preserved");
        LOGGER.info("  ✓ What executed: 3 standalone + 3 grouped");
        LOGGER.info("  ✓ No double execution: Each item executes exactly once");
        LOGGER.info("  ✓ Position verification: All items at correct positions");
        LOGGER.info("  ✓ Complex scenario: Multiple numbered sections + multiple groups work correctly");
    }
}

