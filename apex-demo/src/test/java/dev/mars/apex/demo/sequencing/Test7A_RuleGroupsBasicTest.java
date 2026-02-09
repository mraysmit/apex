package dev.mars.apex.demo.sequencing;

import dev.mars.apex.core.engine.core.RulesEngine;
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
 * Test 7A: Rule Groups with Groups-Only Logic
 * 
 * Tests that rule-groups work correctly with groups-only logic.
 * 
 * YAML Structure:
 * - rules: standalone-rule-1, grouped-rule-1, standalone-rule-2, grouped-rule-2
 * - rule-groups: rule-group-A (references grouped-rule-1, grouped-rule-2)
 * 
 * Expected execution order:
 * 1. standalone-rule-1 (NOT in group)
 * 2. standalone-rule-2 (NOT in group)
 * 3. grouped-rule-1 (via rule-group-A)
 * 4. grouped-rule-2 (via rule-group-A)
 * 
 * Key verification:
 * - Rules NOT referenced by groups execute at their definition position
 * - Rules referenced by groups execute via the group, not at definition position
 * - No double execution
 */
public class Test7A_RuleGroupsBasicTest extends DemoTestBase {

    private static final Logger LOGGER = LoggerFactory.getLogger(Test7A_RuleGroupsBasicTest.class);

    @BeforeEach
    public void clearExecutionTracker() {
        ExecutionTracker.clear();
    }

    @Test
    public void testRuleGroupsBasic() throws Exception {
        LOGGER.info("=== TEST 7A: Rule Groups with Groups-Only Logic ===");

        // Create RulesEngine from YAML
        RulesEngine engine = RulesEngine.fromClasspath("dev/mars/apex/demo/sequencing/Test7A_RuleGroupsBasicTest.yaml");

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
            "standalone-rule-1",  // NOT in group
            "standalone-rule-2",  // NOT in group
            "grouped-rule-1",     // via rule-group-A
            "grouped-rule-2"      // via rule-group-A
        );
        assertEquals(expected, executionLog,
                "Execution order MUST be: standalone-rule-1, standalone-rule-2, grouped-rule-1, grouped-rule-2");

        // 3. Verify what executed
        assertTrue(executionLog.contains("standalone-rule-1"), "standalone-rule-1 MUST execute");
        assertTrue(executionLog.contains("standalone-rule-2"), "standalone-rule-2 MUST execute");
        assertTrue(executionLog.contains("grouped-rule-1"), "grouped-rule-1 MUST execute");
        assertTrue(executionLog.contains("grouped-rule-2"), "grouped-rule-2 MUST execute");

        // 4. Verify NO double execution
        assertEquals(1, Collections.frequency(executionLog, "standalone-rule-1"),
                "standalone-rule-1 MUST execute EXACTLY ONCE");
        assertEquals(1, Collections.frequency(executionLog, "standalone-rule-2"),
                "standalone-rule-2 MUST execute EXACTLY ONCE");
        assertEquals(1, Collections.frequency(executionLog, "grouped-rule-1"),
                "grouped-rule-1 MUST execute EXACTLY ONCE (via group, not at definition)");
        assertEquals(1, Collections.frequency(executionLog, "grouped-rule-2"),
                "grouped-rule-2 MUST execute EXACTLY ONCE (via group, not at definition)");

        // 5. Verify execution positions
        assertEquals("standalone-rule-1", executionLog.get(0), "Position 0 MUST be standalone-rule-1");
        assertEquals("standalone-rule-2", executionLog.get(1), "Position 1 MUST be standalone-rule-2");
        assertEquals("grouped-rule-1", executionLog.get(2), "Position 2 MUST be grouped-rule-1");
        assertEquals("grouped-rule-2", executionLog.get(3), "Position 3 MUST be grouped-rule-2");

        LOGGER.info("[OK] TEST 7A PASSED: All 5 definitive assertion types verified");
        LOGGER.info("  [OK] Exact count: 4 items");
        LOGGER.info("  [OK] Exact order: standalone-rule-1, standalone-rule-2, grouped-rule-1, grouped-rule-2");
        LOGGER.info("  [OK] What executed: 2 standalone + 2 grouped");
        LOGGER.info("  [OK] No double execution: Each rule executes exactly once");
        LOGGER.info("  [OK] Position verification: All rules at correct positions");
        LOGGER.info("  [OK] Rule groups: Groups-only logic works correctly for rules");
    }
}

