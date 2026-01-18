package dev.mars.apex.demo.sequencing;

import dev.mars.apex.core.config.yaml.YamlConfigurationException;
import dev.mars.apex.core.engine.config.RulesEngine;
import dev.mars.apex.demo.DemoTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.*;

/**
 * EDGE CASE TEST 3: Duplicate IDs Across Numbered Sections
 * 
 * Tests that duplicate enrichment IDs across numbered sections (enrichments-1, enrichments-2)
 * are properly detected and rejected.
 * 
 * YAML Structure:
 * - enrichments-1: contains "duplicate-id"
 * - enrichments-2: contains "duplicate-id" (SAME ID!)
 * 
 * Expected Behavior:
 * - Should throw YamlConfigurationException with message about duplicate ID
 * - Should NOT allow configuration to load
 * 
 * This test verifies that the duplicate ID validation runs AFTER numbered sections are merged.
 */
@DisplayName("EDGE CASE 3: Duplicate IDs Across Numbered Sections")
public class TestEdge3_DuplicateIDsAcrossNumberedSectionsTest extends DemoTestBase {

    private static final Logger LOGGER = LoggerFactory.getLogger(TestEdge3_DuplicateIDsAcrossNumberedSectionsTest.class);

    @Test
    @DisplayName("Should reject duplicate enrichment IDs across numbered sections")
    public void testDuplicateIDsAcrossNumberedSections() {
        LOGGER.info("=== EDGE CASE TEST 3: Duplicate IDs Across Numbered Sections ===");

        // Attempt to load YAML with duplicate IDs
        YamlConfigurationException exception = assertThrows(
            YamlConfigurationException.class,
            () -> RulesEngine.fromClasspath("dev/mars/apex/demo/sequencing/TestEdge3_DuplicateIDsAcrossNumberedSectionsTest.yaml"),
            "Should throw YamlConfigurationException for duplicate IDs"
        );

        // Verify error message
        String errorMessage = exception.getMessage().toLowerCase();
        assertTrue(errorMessage.contains("duplicate"), 
            "Error message should mention 'duplicate': " + exception.getMessage());
        assertTrue(errorMessage.contains("enrichment") || errorMessage.contains("id"), 
            "Error message should mention 'enrichment' or 'id': " + exception.getMessage());

        LOGGER.info("✅ EDGE CASE TEST 3 PASSED: Duplicate IDs correctly rejected");
        LOGGER.info("   Error message: {}", exception.getMessage());
    }
}

