package dev.mars.apex.core.service.engine;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for ExpressionEvaluatorService enhanced context creation functionality.
 * 
 * This test class verifies that the Phase 3 enhancements to ExpressionEvaluatorService
 * provide the same HashMap property access capabilities as UnifiedRuleEvaluator.
 */
class ExpressionEvaluatorServiceEnhancedTest {

    private ExpressionEvaluatorService expressionEvaluator;

    @BeforeEach
    void setUp() {
        expressionEvaluator = new ExpressionEvaluatorService();
    }

    @Test
    @DisplayName("Should evaluate HashMap properties using enhanced context")
    void testEnhancedContextWithHashMapProperties() {
        // Given
        Map<String, Object> testData = new HashMap<>();
        testData.put("quantity", 1000);
        testData.put("instrumentType", "EQUITY");
        
        Map<String, Object> facts = new HashMap<>();
        facts.put("data", testData);

        // When & Then - Test simple property access
        Boolean result1 = expressionEvaluator.evaluateWithEnhancedContext("#data.quantity > 500", facts, Boolean.class);
        assertNotNull(result1, "Result should not be null");
        assertTrue(result1, "Quantity should be greater than 500");

        // Test string property access
        Boolean result2 = expressionEvaluator.evaluateWithEnhancedContext("#data.instrumentType == 'EQUITY'", facts, Boolean.class);
        assertNotNull(result2, "Result should not be null");
        assertTrue(result2, "Instrument type should be EQUITY");

        // Test combined expression
        Boolean result3 = expressionEvaluator.evaluateWithEnhancedContext(
            "#data.quantity > 500 && #data.instrumentType == 'EQUITY'", facts, Boolean.class);
        assertNotNull(result3, "Result should not be null");
        assertTrue(result3, "Combined expression should match");
    }

    @Test
    @DisplayName("Should create enhanced context with MapPropertyAccessor")
    void testEnhancedContextCreation() {
        // Given
        Map<String, Object> facts = new HashMap<>();
        facts.put("testValue", "hello");
        facts.put("testNumber", 42);

        // When
        var context = expressionEvaluator.createEnhancedContext(facts);

        // Then
        assertNotNull(context, "Context should not be null");
        
        // Test that variables are accessible
        assertEquals("hello", context.lookupVariable("testValue"));
        assertEquals(42, context.lookupVariable("testNumber"));
        
        // Test that root object is set (it's wrapped in a TypedValue)
        assertNotNull(context.getRootObject());
        assertTrue(context.getRootObject().toString().contains("testValue=hello"));
        assertTrue(context.getRootObject().toString().contains("testNumber=42"));
    }

    @Test
    @DisplayName("Should handle nested HashMap access")
    void testNestedHashMapAccess() {
        // Given
        Map<String, Object> nestedData = new HashMap<>();
        nestedData.put("price", 100.50);
        nestedData.put("currency", "USD");
        
        Map<String, Object> tradeData = new HashMap<>();
        tradeData.put("details", nestedData);
        tradeData.put("quantity", 1000);
        
        Map<String, Object> facts = new HashMap<>();
        facts.put("trade", tradeData);

        // When & Then
        Boolean result1 = expressionEvaluator.evaluateWithEnhancedContext("#trade.quantity > 500", facts, Boolean.class);
        assertTrue(result1, "Trade quantity should be accessible");

        Double result2 = expressionEvaluator.evaluateWithEnhancedContext("#trade.details.price", facts, Double.class);
        assertEquals(100.50, result2, "Nested price should be accessible");

        String result3 = expressionEvaluator.evaluateWithEnhancedContext("#trade.details.currency", facts, String.class);
        assertEquals("USD", result3, "Nested currency should be accessible");
    }

    @Test
    @DisplayName("Should handle error cases gracefully with enhanced context")
    void testEnhancedContextErrorHandling() {
        // Given
        Map<String, Object> facts = new HashMap<>();
        facts.put("validField", "test");

        // When & Then - Test non-existent property
        Boolean result1 = expressionEvaluator.evaluateWithEnhancedContext("#nonExistentField == 'test'", facts, Boolean.class);
        assertFalse(result1, "Non-existent field should return false for boolean expressions");

        // Test invalid expression
        String result2 = expressionEvaluator.evaluateWithEnhancedContext("#validField.invalidMethod()", facts, String.class);
        assertNull(result2, "Invalid method call should return null");
    }

    @Test
    @DisplayName("Should maintain backward compatibility with regular context")
    void testBackwardCompatibility() {
        // Given
        Map<String, Object> facts = new HashMap<>();
        facts.put("amount", 1000);
        facts.put("currency", "USD");

        // When & Then - Test both enhanced and regular context methods
        Boolean enhancedResult = expressionEvaluator.evaluateWithEnhancedContext("#amount > 500", facts, Boolean.class);
        assertTrue(enhancedResult, "Enhanced context should work");

        // Create regular context and test
        var regularContext = expressionEvaluator.createEnhancedContext(facts);
        Boolean regularResult = expressionEvaluator.evaluate("#amount > 500", regularContext, Boolean.class);
        assertTrue(regularResult, "Regular context should also work");

        // Results should be the same
        assertEquals(enhancedResult, regularResult, "Both methods should produce same result");
    }
}
