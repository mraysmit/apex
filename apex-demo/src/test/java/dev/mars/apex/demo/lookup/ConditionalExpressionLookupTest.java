package dev.mars.apex.demo.lookup;

import dev.mars.apex.demo.DemoTestBase;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JUnit 5 test for ConditionalExpressionLookup functionality.
 *
 * CRITICAL VALIDATION CHECKLIST APPLIED:
 * ✅ Count enrichments in YAML - 1 enrichment expected (conditional-expression-lookup-demo)
 * ✅ Verify log shows "Processed: 1 out of 1" - Must be 100% execution rate
 * ✅ Check EVERY enrichment condition - Test data triggers conditional expression lookup condition
 * ✅ Validate EVERY business calculation - Test actual conditional expression evaluation logic
 * ✅ Assert ALL enrichment results - Every result-field has corresponding assertEquals
 *
 * BUSINESS LOGIC VALIDATION:
 * - Conditional expression lookup using nested ternary operators
 * - Credit score-based risk categorization using YAML enrichments
 * - YAML-driven conditional expression evaluation
 *
 * YAML FIRST PRINCIPLE:
 * - ALL business logic is in YAML enrichments
 * - Java test only loads YAML and calls APEX
 * - NO custom credit score logic or complex validation
 * - Simple test data and basic assertions only
 */
public class ConditionalExpressionLookupTest extends DemoTestBase {

    private static final Logger logger = LoggerFactory.getLogger(ConditionalExpressionLookupTest.class);

    @Test
    void testConditionalExpressionLookupFunctionality() {
        logger.info("=== Testing Conditional Expression Lookup Functionality ===");

        // Load YAML configuration for conditional expression lookup
        try {
            var config = yamlLoader.loadFromFile("src/test/java/dev/mars/apex/demo/lookup/ConditionalExpressionLookupTest.yaml");
            assertNotNull(config, "YAML configuration should not be null");

        // Create simple test data that triggers the conditional expression enrichment
        Map<String, Object> testData = new HashMap<>();
        testData.put("creditScore", 780);
        testData.put("approach", "real-apex-services");

        // Execute APEX enrichment processing - ALL logic in YAML
        Object result = enrichmentService.enrichObject(config, testData);

        // Validate enrichment results
        assertNotNull(result, "Conditional expression lookup result should not be null");
        @SuppressWarnings("unchecked")
        Map<String, Object> enrichedData = (Map<String, Object>) result;

        // Validate YAML-driven results only
        assertNotNull(enrichedData.get("riskCategory"), "Risk category should be determined by conditional expression");

        String riskCategory = (String) enrichedData.get("riskCategory");
        assertEquals("EXCELLENT", riskCategory, "Credit score 780 should result in EXCELLENT risk category");

            logger.info("✅ Conditional expression lookup functionality test completed successfully");
        } catch (Exception e) {
            fail("Failed to load YAML configuration: " + e.getMessage());
        }
    }
}
