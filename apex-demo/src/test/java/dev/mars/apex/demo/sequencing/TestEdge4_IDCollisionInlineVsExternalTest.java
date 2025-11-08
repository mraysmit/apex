package dev.mars.apex.demo.sequencing.edge_cases;

import dev.mars.apex.core.config.yaml.YamlConfigurationException;
import dev.mars.apex.core.engine.config.RulesEngine;
import dev.mars.apex.demo.DemoTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.*;

/**
 * EDGE CASE TEST 4: ID Collision Between Inline and External Enrichments
 * 
 * Tests that duplicate enrichment IDs between inline enrichments (in main file) 
 * and external enrichments (in referenced file) are properly detected and rejected.
 * 
 * YAML Structure:
 * - main.yaml: contains inline enrichment "collision-id"
 * - external.yaml: contains enrichment "collision-id" (SAME ID!)
 * - main.yaml references external.yaml via enrichment-refs
 * 
 * Expected Behavior:
 * - Should throw YamlConfigurationException with message about duplicate ID
 * - Should NOT allow configuration to load
 * 
 * This test verifies that duplicate ID validation runs AFTER external files are merged.
 */
@DisplayName("EDGE CASE 4: ID Collision Between Inline and External Enrichments")
public class TestEdge4_IDCollisionInlineVsExternalTest extends DemoTestBase {

    private static final Logger LOGGER = LoggerFactory.getLogger(TestEdge4_IDCollisionInlineVsExternalTest.class);

    @Test
    @DisplayName("Should reject duplicate enrichment IDs between inline and external files")
    public void testIDCollisionInlineVsExternal() {
        LOGGER.info("=== EDGE CASE TEST 4: ID Collision Between Inline and External Enrichments ===");

        // Attempt to load YAML with ID collision
        YamlConfigurationException exception = assertThrows(
            YamlConfigurationException.class,
            () -> RulesEngine.fromFile("src/test/java/dev/mars/apex/demo/sequencing/edge_cases/TestEdge4_IDCollisionInlineVsExternalTest.yaml"),
            "Should throw YamlConfigurationException for duplicate IDs across inline and external files"
        );

        // Verify error message
        String errorMessage = exception.getMessage().toLowerCase();
        assertTrue(errorMessage.contains("duplicate"), 
            "Error message should mention 'duplicate': " + exception.getMessage());
        assertTrue(errorMessage.contains("enrichment") || errorMessage.contains("id"), 
            "Error message should mention 'enrichment' or 'id': " + exception.getMessage());
        assertTrue(errorMessage.contains("collision-id"), 
            "Error message should mention the duplicate ID 'collision-id': " + exception.getMessage());

        LOGGER.info("✅ EDGE CASE TEST 4 PASSED: ID collision correctly rejected");
        LOGGER.info("   Error message: {}", exception.getMessage());
    }
}

