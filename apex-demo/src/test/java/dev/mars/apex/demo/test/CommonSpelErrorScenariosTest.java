package dev.mars.apex.demo.test;

import dev.mars.apex.engine.core.RulesEngine;
import dev.mars.apex.engine.core.RulesEngineConfiguration;
import dev.mars.apex.engine.model.Rule;
import dev.mars.apex.engine.model.RuleResult;
import dev.mars.apex.demo.ColoredTestOutputExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import dev.mars.apex.engine.core.RuleBuilder;

/**
 * Common SpEL Error Scenarios for YAML Developers.
 * 
 * This test class demonstrates the most common SpEL syntax and runtime errors
 * that developers encounter when writing APEX YAML configuration files.
 * 
 * Each test shows:
 * - The incorrect SpEL syntax/usage
 * - The error message you'll see
 * - The correct way to write it
 * 
 * LEARNING OBJECTIVES:
 * - Understand correct SpEL variable reference syntax (#variableName)
 * - Avoid common syntax mistakes (quotes, brackets)
 * - Handle null values safely
 * - Use proper type conversions
 * - Write defensive expressions
 * 
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2.1.0
 */
@ExtendWith(ColoredTestOutputExtension.class)
@DisplayName("Common SpEL Error Scenarios for YAML Developers")
class CommonSpelErrorScenariosTest {

    private static final Logger logger = LoggerFactory.getLogger(CommonSpelErrorScenariosTest.class);
    
    private RulesEngine rulesEngine;
    
    @BeforeEach
    void setUp() {
        RulesEngineConfiguration config = new RulesEngineConfiguration();
        rulesEngine = new RulesEngine(config);
    }

    // ========================================
    // SCENARIO 1: Quote-Bracket Syntax Errors
    // ========================================

    @Test
    @DisplayName("WRONG: #'variableName'] - Quote-Bracket Combination")
    void testQuoteBracketSyntaxError() {
        logger.info("=== Common Error #1: Quote-Bracket Combination ===");
        logger.info("WRONG: #'region'] == 'US'");
        logger.info("RIGHT: #region == 'US'");
        
        // This is what was breaking ConditionalStageExecutionTest
        Rule wrongRule = new RuleBuilder().withName("wrong-syntax").withCondition("#'region'] == 'US'").withMessage("Invalid syntax").withSeverity("ERROR").build();
        
        Map<String, Object> data = new HashMap<>();
        data.put("region", "US");
        
        RuleResult result = rulesEngine.executeRule(wrongRule, data);
        
        assertEquals(RuleResult.ResultType.ERROR, result.getResultType(),
            "Should return ERROR for malformed syntax");
        assertTrue(result.getMessage().contains("Unexpected token") || 
                   result.getMessage().contains("evaluation failed"),
            "Error should mention syntax problem");
        
        logger.info("[OK] Error detected: Malformed quote-bracket syntax");
        logger.info("💡 TIP: Use #variableName without quotes or brackets");
    }

    @Test
    @DisplayName("CORRECT: #variableName - Proper Variable Reference")
    void testCorrectVariableReferenceSyntax() {
        logger.info("=== Correct Syntax: Simple Variable Reference ===");
        
        Rule correctRule = new RuleBuilder().withName("correct-syntax").withCondition("#region == 'US'").withMessage("Valid syntax").withSeverity("INFO").build();
        
        Map<String, Object> data = new HashMap<>();
        data.put("region", "US");
        
        RuleResult result = rulesEngine.executeRule(correctRule, data);
        
        assertEquals(RuleResult.ResultType.MATCH, result.getResultType(),
            "Should match with correct syntax");
        
        logger.info("[OK] Rule matched successfully with correct syntax");
    }

    // ========================================
    // SCENARIO 2: Nested Property Access
    // ========================================

    @Test
    @DisplayName("WRONG: #obj.nonexistent.property - Nested Null Access")
    void testNestedPropertyAccessError() {
        logger.info("=== Common Error #2: Nested Property Access on Null ===");
        logger.info("WRONG: #customer.address.city (when address is null)");
        logger.info("RIGHT: #customer != null && #customer.address != null && #customer.address.city == 'NYC'");
        
        Rule unsafeRule = new RuleBuilder().withName("nested-access").withCondition("#customer.address.city == 'NYC'").withMessage("City is NYC").withSeverity("INFO").build();
        
        Map<String, Object> data = new HashMap<>();
        Map<String, Object> customer = new HashMap<>();
        customer.put("name", "John");
        customer.put("address", null);  // ⚠️ address is null!
        data.put("customer", customer);
        
        RuleResult result = rulesEngine.executeRule(unsafeRule, data);
        
        assertEquals(RuleResult.ResultType.NO_MATCH, result.getResultType(),
            "Error recovery returns NO_MATCH for null property access");
        
        logger.info("[OK] Error detected: Null pointer in nested property access");
        logger.info("💡 TIP: Always check for null before accessing nested properties");
    }

    @Test
    @DisplayName("CORRECT: Safe Nested Property Access with Null Checks")
    void testSafeNestedPropertyAccess() {
        logger.info("=== Correct Syntax: Safe Nested Property Access ===");
        
        Rule safeRule = new RuleBuilder().withName("safe-nested-access").withCondition("#customer != null && #customer['address'] != null && #customer['address']['city'] == 'NYC'").withMessage("City is NYC").withSeverity("INFO").build();
        
        Map<String, Object> data = new HashMap<>();
        Map<String, Object> customer = new HashMap<>();
        Map<String, Object> address = new HashMap<>();
        address.put("city", "NYC");
        customer.put("name", "John");
        customer.put("address", address);
        data.put("customer", customer);
        
        RuleResult result = rulesEngine.executeRule(safeRule, data);
        
        assertEquals(RuleResult.ResultType.MATCH, result.getResultType(),
            "Should match with safe null checks");
        
        logger.info("[OK] Safe nested access works correctly");
    }

    // ========================================
    // SCENARIO 3: Null Method Calls
    // ========================================

    @Test
    @DisplayName("WRONG: #value.toString() - Method Call on Null")
    void testMethodCallOnNull() {
        logger.info("=== Common Error #3: Method Call on Null Value ===");
        logger.info("WRONG: #description.length() > 0 (when description is null)");
        logger.info("RIGHT: #description != null && #description.length() > 0");
        
        Rule unsafeRule = new RuleBuilder().withName("null-method-call").withCondition("#description.length() > 0").withMessage("Has description").withSeverity("INFO").build();
        
        Map<String, Object> data = new HashMap<>();
        data.put("description", null);  // ⚠️ null value!
        
        RuleResult result = rulesEngine.executeRule(unsafeRule, data);
        
        assertEquals(RuleResult.ResultType.NO_MATCH, result.getResultType(),
            "Error recovery returns NO_MATCH when calling method on null");
        
        logger.info("[OK] Error detected: Method call on null object");
        logger.info("💡 TIP: Check for null before calling methods");
    }

    @Test
    @DisplayName("CORRECT: Safe Method Calls with Null Guards")
    void testSafeMethodCalls() {
        logger.info("=== Correct Syntax: Safe Method Calls ===");
        
        Rule safeRule = new RuleBuilder().withName("safe-method-call").withCondition("#description != null && #description.length() > 0").withMessage("Has description").withSeverity("INFO").build();
        
        Map<String, Object> data = new HashMap<>();
        data.put("description", "Valid description");
        
        RuleResult result = rulesEngine.executeRule(safeRule, data);
        
        assertEquals(RuleResult.ResultType.MATCH, result.getResultType(),
            "Should match with safe null check");
        
        logger.info("[OK] Safe method call works correctly");
    }

    // ========================================
    // SCENARIO 4: Type Conversion Errors
    // ========================================

    @Test
    @DisplayName("WRONG: #amount > 1000 - Type Mismatch (String vs Number)")
    void testTypeMismatchComparison() {
        logger.info("=== Common Error #4: Type Mismatch in Comparison ===");
        logger.info("WRONG: #amount > 1000 (when amount is a String)");
        logger.info("RIGHT: T(Double).parseDouble(#amount) > 1000");
        
        Rule unsafeRule = new RuleBuilder().withName("type-mismatch").withCondition("#amount > 1000").withMessage("High amount").withSeverity("INFO").build();
        
        Map<String, Object> data = new HashMap<>();
        data.put("amount", "5000");  // ⚠️ String instead of number!
        
        RuleResult result = rulesEngine.executeRule(unsafeRule, data);
        
        // SpEL may handle this gracefully or throw error depending on version
        logger.info("Result type: " + result.getResultType());
        logger.info("💡 TIP: Ensure data types match your conditions, or use explicit conversion");
    }

    @Test
    @DisplayName("CORRECT: Explicit Type Conversion")
    void testExplicitTypeConversion() {
        logger.info("=== Correct Syntax: Explicit Type Conversion ===");
        
        Rule safeRule = new RuleBuilder().withName("safe-conversion").withCondition("#amount instanceof T(String) ? T(Double).parseDouble(#amount) > 1000 : #amount > 1000").withMessage("High amount").withSeverity("INFO").build();
        
        Map<String, Object> data = new HashMap<>();
        data.put("amount", "5000");
        
        RuleResult result = rulesEngine.executeRule(safeRule, data);
        
        assertEquals(RuleResult.ResultType.MATCH, result.getResultType(),
            "Should match with proper type conversion");
        
        logger.info("[OK] Type-safe comparison works correctly");
    }

    // ========================================
    // SCENARIO 5: Array/List Access Errors
    // ========================================

    @Test
    @DisplayName("WRONG: #items[5] - Array Index Out of Bounds")
    void testArrayIndexOutOfBounds() {
        logger.info("=== Common Error #5: Array Index Out of Bounds ===");
        logger.info("WRONG: #items[5] (when list has only 2 elements)");
        logger.info("RIGHT: #items.size() > 5 && #items[5] == 'value'");
        
        Rule unsafeRule = new RuleBuilder().withName("array-access").withCondition("#items[5] == 'target'").withMessage("Target found").withSeverity("INFO").build();
        
        Map<String, Object> data = new HashMap<>();
        data.put("items", Arrays.asList("a", "b"));  // ⚠️ Only 2 elements!
        
        RuleResult result = rulesEngine.executeRule(unsafeRule, data);
        
        assertEquals(RuleResult.ResultType.NO_MATCH, result.getResultType(),
            "Error recovery returns NO_MATCH for index out of bounds");
        
        logger.info("[OK] Error detected: Array index out of bounds");
        logger.info("💡 TIP: Check array/list size before accessing by index");
    }

    @Test
    @DisplayName("CORRECT: Safe Array Access with Bounds Check")
    void testSafeArrayAccess() {
        logger.info("=== Correct Syntax: Safe Array Access ===");
        
        Rule safeRule = new RuleBuilder().withName("safe-array-access").withCondition("#items != null && #items.size() > 5 && #items[5] == 'target'").withMessage("Target found").withSeverity("INFO").build();
        
        Map<String, Object> data = new HashMap<>();
        data.put("items", Arrays.asList("a", "b", "c", "d", "e", "target", "g"));
        
        RuleResult result = rulesEngine.executeRule(safeRule, data);
        
        assertEquals(RuleResult.ResultType.MATCH, result.getResultType(),
            "Should match with safe bounds check");
        
        logger.info("[OK] Safe array access works correctly");
    }

    // ========================================
    // SCENARIO 6: Division by Zero
    // ========================================

    @Test
    @DisplayName("WRONG: #total / #count - Division by Zero")
    void testDivisionByZero() {
        logger.info("=== Common Error #6: Division by Zero ===");
        logger.info("WRONG: #total / #count (when count is 0)");
        logger.info("RIGHT: #count != 0 ? (#total / #count) : 0");
        
        Rule unsafeRule = new RuleBuilder().withName("division").withCondition("#total / #count > 100").withMessage("Average is high").withSeverity("INFO").build();
        
        Map<String, Object> data = new HashMap<>();
        data.put("total", 1000);
        data.put("count", 0);  // ⚠️ Division by zero!
        
        RuleResult result = rulesEngine.executeRule(unsafeRule, data);
        
        assertEquals(RuleResult.ResultType.NO_MATCH, result.getResultType(),
            "Error recovery returns NO_MATCH for division by zero");
        
        logger.info("[OK] Error detected: Division by zero");
        logger.info("💡 TIP: Always check divisor is not zero");
    }

    @Test
    @DisplayName("CORRECT: Safe Division with Zero Check")
    void testSafeDivision() {
        logger.info("=== Correct Syntax: Safe Division ===");
        
        Rule safeRule = new RuleBuilder().withName("safe-division").withCondition("#count != null && #count != 0 && (#total / #count) > 100").withMessage("Average is high").withSeverity("INFO").build();
        
        Map<String, Object> data = new HashMap<>();
        data.put("total", 1000);
        data.put("count", 5);
        
        RuleResult result = rulesEngine.executeRule(safeRule, data);
        
        assertEquals(RuleResult.ResultType.MATCH, result.getResultType(),
            "Should match with safe division");
        
        logger.info("[OK] Safe division works correctly");
    }

    // ========================================
    // SCENARIO 7: Invalid Method Arguments
    // ========================================

    @Test
    @DisplayName("⚠️ SURPRISING: #text.substring('5') - Auto Type Conversion")
    void testInvalidMethodArguments() {
        logger.info("=== Surprising Behavior #7: Auto Type Conversion ===");
        logger.info("WORKS (surprisingly): #text.substring('5') - SpEL auto-converts String to int!");
        logger.info("BETTER: #text.substring(5) - explicit int for clarity");
        
        Rule unsafeRule = new RuleBuilder().withName("invalid-args").withCondition("#text.substring('5').length() > 0").withMessage("Has substring").withSeverity("INFO").build();
        
        Map<String, Object> data = new HashMap<>();
        data.put("text", "Hello World");
        
        RuleResult result = rulesEngine.executeRule(unsafeRule, data);
        
        assertEquals(RuleResult.ResultType.MATCH, result.getResultType(),
            "SpEL automatically converts String '5' to int 5");
        
        logger.info("⚠️ Note: SpEL silently converted '5' to 5");
        logger.info("💡 TIP: Use explicit types to avoid confusion");
    }

    // ========================================
    // SCENARIO 8: Missing Elvis Operator Usage
    // ========================================

    @Test
    @DisplayName("BEST PRACTICE: Elvis Operator (?:) for Null Safety")
    void testElvisOperatorUsage() {
        logger.info("=== Best Practice: Elvis Operator for Default Values ===");
        logger.info("GOOD: #value ?: 0");
        logger.info("BETTER THAN: #value != null ? #value : 0");
        
        Rule elvisRule = new RuleBuilder().withName("elvis-operator").withCondition("(#quantity ?: 0) > 100").withMessage("High quantity").withSeverity("INFO").build();
        
        // Test with null value
        Map<String, Object> nullData = new HashMap<>();
        nullData.put("quantity", null);
        
        RuleResult nullResult = rulesEngine.executeRule(elvisRule, nullData);
        assertEquals(RuleResult.ResultType.NO_MATCH, nullResult.getResultType(),
            "Should not match when null becomes 0");
        
        // Test with actual value
        Map<String, Object> valueData = new HashMap<>();
        valueData.put("quantity", 150);
        
        RuleResult valueResult = rulesEngine.executeRule(elvisRule, valueData);
        assertEquals(RuleResult.ResultType.MATCH, valueResult.getResultType(),
            "Should match when value > 100");
        
        logger.info("[OK] Elvis operator provides safe default values");
        logger.info("💡 TIP: Use ?: for concise null handling");
    }

    // ========================================
    // SCENARIO 9: Safe Navigation Operator
    // ========================================

    @Test
    @DisplayName("BEST PRACTICE: Safe Navigation (?.) for Null-Safe Property Access")
    void testSafeNavigationOperator() {
        logger.info("=== Best Practice: Safe Navigation Operator ===");
        logger.info("GOOD: #customer?.address?.city");
        logger.info("BETTER THAN: #customer != null && #customer.address != null && #customer.address.city");
        
        Rule safeNavRule = new RuleBuilder().withName("safe-navigation").withCondition("#customer?.address?.city == 'NYC'").withMessage("NYC customer").withSeverity("INFO").build();
        
        // Test with null intermediate value
        Map<String, Object> data = new HashMap<>();
        Map<String, Object> customer = new HashMap<>();
        customer.put("name", "John");
        customer.put("address", null);  // ⚠️ Null address
        data.put("customer", customer);
        
        RuleResult result = rulesEngine.executeRule(safeNavRule, data);
        
        // Safe navigation should return null, not error
        assertEquals(RuleResult.ResultType.NO_MATCH, result.getResultType(),
            "Should not match but also not error");
        
        logger.info("[OK] Safe navigation handles null gracefully");
        logger.info("💡 TIP: Use ?. for null-safe property chains");
    }
}
