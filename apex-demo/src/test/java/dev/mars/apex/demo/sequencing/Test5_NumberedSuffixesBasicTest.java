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
 * Test 5: Numbered Suffixes Basic
 * 
 * Tests that numbered suffixes (enrichments-1, enrichments-2, enrichments-3) are:
 * 1. Recognized by the parser
 * 2. Merged into a single enrichments section
 * 3. Executed in document order
 * 
 * Expected execution order:
 * 1. batch1-item1 (from enrichments-1)
 * 2. batch1-item2 (from enrichments-1)
 * 3. batch2-item1 (from enrichments-2)
 * 4. batch2-item2 (from enrichments-2)
 * 5. batch3-item1 (from enrichments-3)
 * 6. batch3-item2 (from enrichments-3)
 */
public class Test5_NumberedSuffixesBasicTest extends DemoTestBase {

    private static final Logger LOGGER = LoggerFactory.getLogger(Test5_NumberedSuffixesBasicTest.class);

    @BeforeEach
    public void clearExecutionTracker() {
        ExecutionTracker.clear();
    }

    @Test
    public void testNumberedSuffixes() throws Exception {
        LOGGER.info("=== TEST 5: Numbered Suffixes Basic ===");

        // Create RulesEngine from YAML
        RulesEngine engine = RulesEngine.fromClasspath("dev/mars/apex/demo/sequencing/Test5_NumberedSuffixesBasicTest.yaml");

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
                "Should execute EXACTLY 6 items: 2 from each numbered section");

        // 2. EXACT execution order
        List<String> expected = List.of(
            "batch1-item1", "batch1-item2",  // from enrichments-1
            "batch2-item1", "batch2-item2",  // from enrichments-2
            "batch3-item1", "batch3-item2"   // from enrichments-3
        );
        assertEquals(expected, executionLog,
                "Execution order MUST preserve document order across numbered sections");

        // 3. Verify what executed
        assertTrue(executionLog.contains("batch1-item1"), "batch1-item1 MUST execute");
        assertTrue(executionLog.contains("batch1-item2"), "batch1-item2 MUST execute");
        assertTrue(executionLog.contains("batch2-item1"), "batch2-item1 MUST execute");
        assertTrue(executionLog.contains("batch2-item2"), "batch2-item2 MUST execute");
        assertTrue(executionLog.contains("batch3-item1"), "batch3-item1 MUST execute");
        assertTrue(executionLog.contains("batch3-item2"), "batch3-item2 MUST execute");

        // 4. Verify NO double execution
        assertEquals(1, Collections.frequency(executionLog, "batch1-item1"),
                "batch1-item1 MUST execute EXACTLY ONCE");
        assertEquals(1, Collections.frequency(executionLog, "batch1-item2"),
                "batch1-item2 MUST execute EXACTLY ONCE");
        assertEquals(1, Collections.frequency(executionLog, "batch2-item1"),
                "batch2-item1 MUST execute EXACTLY ONCE");
        assertEquals(1, Collections.frequency(executionLog, "batch2-item2"),
                "batch2-item2 MUST execute EXACTLY ONCE");
        assertEquals(1, Collections.frequency(executionLog, "batch3-item1"),
                "batch3-item1 MUST execute EXACTLY ONCE");
        assertEquals(1, Collections.frequency(executionLog, "batch3-item2"),
                "batch3-item2 MUST execute EXACTLY ONCE");

        // 5. Verify execution positions
        assertEquals("batch1-item1", executionLog.get(0), "Position 0 MUST be batch1-item1");
        assertEquals("batch1-item2", executionLog.get(1), "Position 1 MUST be batch1-item2");
        assertEquals("batch2-item1", executionLog.get(2), "Position 2 MUST be batch2-item1");
        assertEquals("batch2-item2", executionLog.get(3), "Position 3 MUST be batch2-item2");
        assertEquals("batch3-item1", executionLog.get(4), "Position 4 MUST be batch3-item1");
        assertEquals("batch3-item2", executionLog.get(5), "Position 5 MUST be batch3-item2");

        LOGGER.info("[OK] TEST 5 PASSED: All 5 definitive assertion types verified");
        LOGGER.info("  [OK] Exact count: 6 items");
        LOGGER.info("  [OK] Exact order: batch1-item1, batch1-item2, batch2-item1, batch2-item2, batch3-item1, batch3-item2");
        LOGGER.info("  [OK] What executed: All 6 items from 3 numbered sections");
        LOGGER.info("  [OK] No double execution: Each item executes exactly once");
        LOGGER.info("  [OK] Position verification: All items at correct positions");
        LOGGER.info("  [OK] Numbered suffixes: Merged correctly and preserved document order");
    }
}

