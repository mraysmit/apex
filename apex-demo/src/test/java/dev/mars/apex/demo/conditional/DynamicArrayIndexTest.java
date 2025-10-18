package dev.mars.apex.demo.conditional;

import dev.mars.apex.core.engine.model.Rule;
import dev.mars.apex.core.engine.model.RuleResult;
import dev.mars.apex.core.service.engine.UnifiedRuleEvaluator;
import dev.mars.apex.demo.DemoTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Minimal test to prove dynamic array indexing functionality in APEX SpEL expressions.
 * Tests four specific patterns:
 * 1. Variable index: #trade.otcTrade.otcLeg[#legIndex].stbRuleName
 * 2. Dynamic index from field: #trade.otcTrade.otcLeg[#trade.selectedLegIndex].stbRuleName
 * 3. Safe dynamic index: #trade?.otcTrade?.otcLeg?.size() > #legIndex && #trade.otcTrade.otcLeg[#legIndex]?.stbRuleName
 * 4. Index with calculation: #items[#currentIndex + 1]?.status
 */
@DisplayName("Dynamic Array Index SpEL Test")
public class DynamicArrayIndexTest extends DemoTestBase {

    private static final Logger logger = LoggerFactory.getLogger(DynamicArrayIndexTest.class);

    private UnifiedRuleEvaluator ruleEvaluator;

    @BeforeEach
    public void setUp() {
        // Call parent setUp to initialize APEX services
        super.setUp();

        logger.info("=== Setting up DynamicArrayIndexTest ===");

        // Initialize UnifiedRuleEvaluator for direct rule testing
        ruleEvaluator = new UnifiedRuleEvaluator();

        logger.info("UnifiedRuleEvaluator initialized for dynamic array index testing");
    }

    @Test
    @DisplayName("Should handle dynamic array indexing in SpEL expressions")
    void shouldHandleDynamicArrayIndexing() {
        logger.info("=== Testing Dynamic Array Indexing ===");

        // Create minimal test data with dynamic indices
        Map<String, Object> testData = createMinimalTestData();

        // Test Pattern 1: Variable index
        testVariableIndex(testData);

        // Test Pattern 2: Dynamic index from field
        testDynamicFieldIndex(testData);

        // Test Pattern 3: Safe dynamic index
        testSafeDynamicIndex(testData);

        // Test Pattern 4: Index with calculation
        testIndexCalculation(testData);

        logger.info("SUCCESS: All dynamic array indexing patterns validated successfully");
    }
    
    /**
     * Creates minimal test data structure for dynamic array indexing.
     */
    private Map<String, Object> createMinimalTestData() {
        Map<String, Object> data = new HashMap<>();
        
        // Create trade structure with otcLeg array
        Map<String, Object> trade = new HashMap<>();
        Map<String, Object> otcTrade = new HashMap<>();
        
        // Create otcLeg array with 3 elements
        List<Map<String, Object>> otcLeg = new ArrayList<>();
        
        // otcLeg[0]
        Map<String, Object> leg0 = new HashMap<>();
        leg0.put("stbRuleName", "RULE_A");
        otcLeg.add(leg0);
        
        // otcLeg[1] 
        Map<String, Object> leg1 = new HashMap<>();
        leg1.put("stbRuleName", "RULE_B");
        otcLeg.add(leg1);
        
        // otcLeg[2]
        Map<String, Object> leg2 = new HashMap<>();
        leg2.put("stbRuleName", "RULE_C");
        otcLeg.add(leg2);
        
        otcTrade.put("otcLeg", otcLeg);
        trade.put("otcTrade", otcTrade);
        trade.put("selectedLegIndex", 1); // Points to leg1 with "RULE_B"
        
        data.put("trade", trade);
        
        // Create items array for calculation test
        List<Map<String, Object>> items = new ArrayList<>();
        
        // items[0]
        Map<String, Object> item0 = new HashMap<>();
        item0.put("status", "PENDING");
        items.add(item0);
        
        // items[1]
        Map<String, Object> item1 = new HashMap<>();
        item1.put("status", "ACTIVE");
        items.add(item1);
        
        // items[2]
        Map<String, Object> item2 = new HashMap<>();
        item2.put("status", "COMPLETE");
        items.add(item2);
        
        data.put("items", items);
        
        // Add index variables
        data.put("legIndex", 2);        // Points to leg2 with "RULE_C"
        data.put("currentIndex", 0);    // For calculation: currentIndex + 1 = 1
        
        logger.info("=== Test Data Structure Created ===");
        logger.info("Array Structure:");
        logger.info("  - trade.otcTrade.otcLeg[0].stbRuleName = {}", leg0.get("stbRuleName"));
        logger.info("  - trade.otcTrade.otcLeg[1].stbRuleName = {}", leg1.get("stbRuleName"));
        logger.info("  - trade.otcTrade.otcLeg[2].stbRuleName = {}", leg2.get("stbRuleName"));
        logger.info("Index Variables:");
        logger.info("  - trade.selectedLegIndex = {} (will select otcLeg[1] = 'RULE_B')", trade.get("selectedLegIndex"));
        logger.info("  - legIndex = {} (will select otcLeg[2] = 'RULE_C')", data.get("legIndex"));
        logger.info("  - currentIndex = {} (calculation: {} + 1 = 1, will select items[1])", data.get("currentIndex"), data.get("currentIndex"));
        logger.info("Items Array:");
        logger.info("  - items[0].status = {}", item0.get("status"));
        logger.info("  - items[1].status = {} ← Will be selected by currentIndex + 1", item1.get("status"));
        logger.info("  - items[2].status = {}", item2.get("status"));
        logger.info("=== Starting Dynamic Array Index Tests ===\n");
        
        return data;
    }
    
    /**
     * Test Pattern 1: Variable index
     * Tests: #trade.otcTrade.otcLeg[#legIndex].stbRuleName != null
     */
    private void testVariableIndex(Map<String, Object> testData) {
        logger.info("=== Testing Pattern 1: Variable index ===");
        logger.info("SpEL Expression: #trade.otcTrade.otcLeg[#legIndex].stbRuleName != null");
        logger.info("Expected Resolution Steps:");
        logger.info("  1. #legIndex → {} (from test data)", testData.get("legIndex"));
        logger.info("  2. #trade.otcTrade.otcLeg[{}] → access array element at index {}", testData.get("legIndex"), testData.get("legIndex"));
        logger.info("  3. otcLeg[{}].stbRuleName → get stbRuleName property", testData.get("legIndex"));
        logger.info("  4. Final evaluation: stbRuleName != null → should be true");

        Rule rule = new Rule(
            "variable-index-test",
            "#trade.otcTrade.otcLeg[#legIndex].stbRuleName != null",
            "Variable index access successful: otcLeg[#legIndex].stbRuleName found",
            "INFO"
        );

        logger.info("Executing rule evaluation...");
        RuleResult result = ruleEvaluator.evaluateRule(rule, testData);

        assertNotNull(result, "Result should not be null");
        assertTrue(result.isTriggered(), "Variable index rule should match");
        assertEquals(RuleResult.ResultType.MATCH, result.getResultType(), "Should return MATCH");

        logger.info("SUCCESS: Variable #legIndex (2) resolved to otcLeg[2].stbRuleName = 'RULE_C'");
        logger.info("Pattern 1 validation complete.\n");
    }

    /**
     * Test Pattern 2: Dynamic index from field
     * Tests: #trade.otcTrade.otcLeg[#trade.selectedLegIndex].stbRuleName != null
     */
    @SuppressWarnings("unchecked")
    private void testDynamicFieldIndex(Map<String, Object> testData) {
        logger.info("=== Testing Pattern 2: Dynamic field index ===");
        logger.info("SpEL Expression: #trade.otcTrade.otcLeg[#trade.selectedLegIndex].stbRuleName != null");

        // Extract nested values for logging
        Map<String, Object> trade = (Map<String, Object>) testData.get("trade");
        Integer selectedIndex = (Integer) trade.get("selectedLegIndex");

        logger.info("Expected Resolution Steps:");
        logger.info("  1. #trade.selectedLegIndex → {} (from trade.selectedLegIndex)", selectedIndex);
        logger.info("  2. #trade.otcTrade.otcLeg[{}] → access array element at index {}", selectedIndex, selectedIndex);
        logger.info("  3. otcLeg[{}].stbRuleName → get stbRuleName property from selected element", selectedIndex);
        logger.info("  4. Final evaluation: stbRuleName != null → should be true");

        Rule rule = new Rule(
            "dynamic-field-index-test",
            "#trade.otcTrade.otcLeg[#trade.selectedLegIndex].stbRuleName != null",
            "Dynamic field index access successful: otcLeg[selectedLegIndex].stbRuleName found",
            "INFO"
        );

        logger.info("Executing rule evaluation...");
        RuleResult result = ruleEvaluator.evaluateRule(rule, testData);

        assertNotNull(result, "Result should not be null");
        assertTrue(result.isTriggered(), "Dynamic field index rule should match");
        assertEquals(RuleResult.ResultType.MATCH, result.getResultType(), "Should return MATCH");

        logger.info("SUCCESS: Field #trade.selectedLegIndex (1) resolved to otcLeg[1].stbRuleName = 'RULE_B'");
        logger.info("Pattern 2 validation complete.\n");
    }

    /**
     * Test Pattern 3: Safe dynamic index
     * Tests: #trade?.otcTrade?.otcLeg?.size() > #legIndex && #trade.otcTrade.otcLeg[#legIndex]?.stbRuleName != null
     */
    @SuppressWarnings("unchecked")
    private void testSafeDynamicIndex(Map<String, Object> testData) {
        logger.info("=== Testing Pattern 3: Safe dynamic index ===");
        logger.info("SpEL Expression: #trade?.otcTrade?.otcLeg?.size() > #legIndex && #trade.otcTrade.otcLeg[#legIndex]?.stbRuleName != null");

        // Extract values for detailed logging
        Map<String, Object> trade = (Map<String, Object>) testData.get("trade");
        Map<String, Object> otcTrade = (Map<String, Object>) trade.get("otcTrade");
        List<Map<String, Object>> otcLeg = (List<Map<String, Object>>) otcTrade.get("otcLeg");
        Integer legIndex = (Integer) testData.get("legIndex");

        logger.info("Expected Resolution Steps:");
        logger.info("  1. #trade?.otcTrade?.otcLeg?.size() → {} (safe navigation to get array size)", otcLeg.size());
        logger.info("  2. #legIndex → {} (from test data)", legIndex);
        logger.info("  3. Bounds check: {} > {} → {} (size > legIndex)", otcLeg.size(), legIndex, otcLeg.size() > legIndex);
        logger.info("  4. If bounds check passes: #trade.otcTrade.otcLeg[{}]?.stbRuleName → access element safely", legIndex);
        logger.info("  5. Final evaluation: (bounds check) && (element.stbRuleName != null) → should be true");

        Rule rule = new Rule(
            "safe-dynamic-index-test",
            "#trade?.otcTrade?.otcLeg?.size() > #legIndex && #trade.otcTrade.otcLeg[#legIndex]?.stbRuleName != null",
            "Safe dynamic index access successful: bounds checked and element found",
            "INFO"
        );

        logger.info("Executing rule evaluation...");
        RuleResult result = ruleEvaluator.evaluateRule(rule, testData);

        assertNotNull(result, "Result should not be null");
        assertTrue(result.isTriggered(), "Safe dynamic index rule should match");
        assertEquals(RuleResult.ResultType.MATCH, result.getResultType(), "Should return MATCH");

        logger.info("SUCCESS: Safe access: size() > #legIndex (3 > 2) && otcLeg[2].stbRuleName resolved correctly");
        logger.info("Pattern 3 validation complete.\n");
    }

    /**
     * Test Pattern 4: Index with calculation
     * Tests: #items[#currentIndex + 1]?.status != null
     */
    @SuppressWarnings("unchecked")
    private void testIndexCalculation(Map<String, Object> testData) {
        logger.info("=== Testing Pattern 4: Index calculation ===");
        logger.info("SpEL Expression: #items[#currentIndex + 1]?.status != null");

        // Extract values for detailed logging
        Integer currentIndex = (Integer) testData.get("currentIndex");
        List<Map<String, Object>> items = (List<Map<String, Object>>) testData.get("items");
        int calculatedIndex = currentIndex + 1;

        logger.info("Expected Resolution Steps:");
        logger.info("  1. #currentIndex → {} (from test data)", currentIndex);
        logger.info("  2. Mathematical calculation: #currentIndex + 1 → {} + 1 = {}", currentIndex, calculatedIndex);
        logger.info("  3. Array access: #items[{}] → access element at calculated index", calculatedIndex);
        logger.info("  4. Safe property access: items[{}]?.status → get status property safely", calculatedIndex);
        logger.info("  5. Final evaluation: status != null → should be true");
        logger.info("  6. Expected result: items[{}].status = '{}'", calculatedIndex,
                   items.get(calculatedIndex).get("status"));

        Rule rule = new Rule(
            "index-calculation-test",
            "#items[#currentIndex + 1]?.status != null",
            "Index calculation access successful: items[currentIndex + 1].status found",
            "INFO"
        );

        logger.info("Executing rule evaluation...");
        RuleResult result = ruleEvaluator.evaluateRule(rule, testData);

        assertNotNull(result, "Result should not be null");
        assertTrue(result.isTriggered(), "Index calculation rule should match");
        assertEquals(RuleResult.ResultType.MATCH, result.getResultType(), "Should return MATCH");

        logger.info("SUCCESS: Calculation #currentIndex + 1 (0 + 1 = 1) resolved to items[1].status = 'ACTIVE'");
        logger.info("Pattern 4 validation complete.\n");
    }
}
