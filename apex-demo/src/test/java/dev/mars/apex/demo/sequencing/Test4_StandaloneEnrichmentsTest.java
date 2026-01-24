package dev.mars.apex.demo.sequencing;

import dev.mars.apex.core.engine.config.RulesEngine;
import dev.mars.apex.core.engine.model.RuleResult;
import dev.mars.apex.demo.DemoTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
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
        String yamlPath = "src/test/java/dev/mars/apex/demo/sequencing/Test4_StandaloneEnrichmentsTest.yaml";
        RulesEngine engine = RulesEngine.fromFile(yamlPath);

        Map<String, Object> testData = new HashMap<>();
        testData.put("value", "test");

        // Act
        RuleResult result = engine.evaluate(testData);

        // Assert
        assertTrue(result.isSuccess(), "Should succeed");

        List<String> executionLog = ExecutionTracker.getExecutionLog();
        LOGGER.info("Execution Log ({} items): {}", executionLog.size(), executionLog);

        // ===== DEFINITIVE ASSERTIONS (5 types) =====

        // 1. EXACT execution count
        assertEquals(4, executionLog.size(),
            "Should execute EXACTLY 4 items: 2 standalone + 2 via group");

        // 2. EXACT execution order
        List<String> expected = List.of("standalone-1", "standalone-2", "grouped-1", "grouped-2");
        assertEquals(expected, executionLog,
            "Execution order MUST be: standalone-1, standalone-2, grouped-1 (via group), grouped-2 (via group)");

        // 3. Verify what executed
        assertTrue(executionLog.contains("standalone-1"), "standalone-1 MUST execute at position 1");
        assertTrue(executionLog.contains("standalone-2"), "standalone-2 MUST execute at position 3");
        assertTrue(executionLog.contains("grouped-1"), "grouped-1 MUST execute via group-A");
        assertTrue(executionLog.contains("grouped-2"), "grouped-2 MUST execute via group-A");

        // 4. Verify NO double execution
        assertEquals(1, Collections.frequency(executionLog, "grouped-1"),
            "grouped-1 MUST execute EXACTLY ONCE (via group only, NOT at position 2)");
        assertEquals(1, Collections.frequency(executionLog, "grouped-2"),
            "grouped-2 MUST execute EXACTLY ONCE (via group only, NOT at position 4)");

        // 5. Verify execution positions
        assertEquals("standalone-1", executionLog.get(0), "Position 0 MUST be standalone-1");
        assertEquals("standalone-2", executionLog.get(1), "Position 1 MUST be standalone-2");
        assertEquals("grouped-1", executionLog.get(2), "Position 2 MUST be grouped-1 (from group-A)");
        assertEquals("grouped-2", executionLog.get(3), "Position 3 MUST be grouped-2 (from group-A)");

        LOGGER.info("TEST 4A PASSED: All 5 definitive assertion types verified");
        LOGGER.info("  [OK] Exact count: 4 items");
        LOGGER.info("  [OK] Exact order: standalone-1, standalone-2, grouped-1, grouped-2");
        LOGGER.info("  [OK] What executed: All 4 items present");
        LOGGER.info("  [OK] No double execution: Each item executes exactly once");
        LOGGER.info("  [OK] Position verification: All items at correct positions");
    }
}

