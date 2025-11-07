package dev.mars.apex.demo.sequencing.order_guarantee;

import dev.mars.apex.core.engine.config.RulesEngine;
import dev.mars.apex.core.engine.model.RuleResult;
import dev.mars.apex.demo.DemoTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test 3: Minimal Enrichment-Group Test
 * 
 * Purpose: Isolate and test if enrichment-groups can execute enrichments properly.
 * This is a minimal test to debug why enrichments in groups are being skipped.
 */
public class Test3_MinimalEnrichmentGroupTest extends DemoTestBase {

    private static final Logger LOGGER = LoggerFactory.getLogger(Test3_MinimalEnrichmentGroupTest.class);

    @Test
    @DisplayName("TEST 3: Minimal enrichment-group with lookup enrichment")
    public void testMinimalEnrichmentGroupWithLookup() throws Exception {
        LOGGER.info("=== TEST 3: Minimal Enrichment-Group Test ===");

        // Arrange
        String yamlPath = "src/test/java/dev/mars/apex/demo/sequencing/order_guarantee/test3-main.yaml";
        RulesEngine engine = RulesEngine.fromFile(yamlPath);

        // Test data with underlying field
        Map<String, Object> testData = new HashMap<>();
        testData.put("underlying", "NDX");

        // Act
        RuleResult result = engine.evaluate(testData);

        // Assert
        assertTrue(result.isSuccess(), "Should succeed");
        
        Map<String, Object> enrichedData = result.getEnrichedData();
        LOGGER.info("Enriched data: {}", enrichedData);
        
        // Check if lookup worked
        assertNotNull(enrichedData.get("currentSpotPrice"), "currentSpotPrice should be enriched");
        assertEquals(15000.0, enrichedData.get("currentSpotPrice"), "NDX spot should be 15000.0");
        
        LOGGER.info("✅ TEST 3 PASSED: Enrichment-group executed lookup enrichment successfully");
    }
}

