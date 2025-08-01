package dev.mars.apex.core.service.engine;

import dev.mars.apex.core.engine.model.RuleResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.spel.support.StandardEvaluationContext;



import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ExpressionEvaluatorService.
 * Tests SpEL expression evaluation, variable binding, and error handling.
 */
class ExpressionEvaluatorServiceTest {

    private ExpressionEvaluatorService expressionEvaluator;
    private EvaluationContext context;

    @BeforeEach
    void setUp() {
        expressionEvaluator = new ExpressionEvaluatorService();
        context = new StandardEvaluationContext();
    }

    @Test
    @DisplayName("Should create expression evaluator successfully")
    void testExpressionEvaluatorCreation() {
        assertNotNull(expressionEvaluator);
    }

    @Test
    @DisplayName("Should evaluate simple boolean expressions")
    void testSimpleBooleanExpressions() {
        // Set up context variables using # prefix for variable access
        context.setVariable("age", 25);
        context.setVariable("active", true);

        // Test simple comparisons using the actual API with # prefix for variables
        Boolean result1 = expressionEvaluator.evaluate("#age > 18", context, Boolean.class);
        assertNotNull(result1);
        assertTrue(result1);

        Boolean result2 = expressionEvaluator.evaluate("#age < 18", context, Boolean.class);
        assertNotNull(result2);
        assertFalse(result2);

        Boolean result3 = expressionEvaluator.evaluate("#active == true", context, Boolean.class);
        assertNotNull(result3);
        assertTrue(result3);

        Boolean result4 = expressionEvaluator.evaluate("#active == false", context, Boolean.class);
        assertNotNull(result4);
        assertFalse(result4);
    }

    @Test
    @DisplayName("Should evaluate complex boolean expressions")
    void testComplexBooleanExpressions() {
        // Set up context variables
        context.setVariable("age", 25);
        context.setVariable("income", 50000);
        context.setVariable("hasJob", true);
        context.setVariable("creditScore", 750);

        // Test logical operators using the actual API with # prefix for variables
        Boolean result1 = expressionEvaluator.evaluate("#age >= 18 && #income > 30000", context, Boolean.class);
        assertNotNull(result1);
        assertTrue(result1);

        Boolean result2 = expressionEvaluator.evaluate("#hasJob == true || #creditScore > 700", context, Boolean.class);
        assertNotNull(result2);
        assertTrue(result2);

        Boolean result3 = expressionEvaluator.evaluate("(#age > 21 && #income > 40000) || #creditScore > 800", context, Boolean.class);
        assertNotNull(result3);
        assertTrue(result3);

        Boolean result4 = expressionEvaluator.evaluate("#age < 18 && #income > 100000", context, Boolean.class);
        assertNotNull(result4);
        assertFalse(result4);
    }

    @Test
    @DisplayName("Should evaluate mathematical expressions")
    void testMathematicalExpressions() {
        // Set up context variables
        context.setVariable("price", 100.0);
        context.setVariable("quantity", 5);
        context.setVariable("discount", 0.1);

        // Test arithmetic operations using the actual API with # prefix for variables
        Double result1 = expressionEvaluator.evaluate("#price * #quantity", context, Double.class);
        assertNotNull(result1);
        assertEquals(500.0, result1);

        Double result2 = expressionEvaluator.evaluate("#price * #discount", context, Double.class);
        assertNotNull(result2);
        assertEquals(10.0, result2);

        Double result3 = expressionEvaluator.evaluate("(#price * #quantity) * (1 - #discount)", context, Double.class);
        assertNotNull(result3);
        assertEquals(450.0, result3);
    }

    @Test
    @DisplayName("Should evaluate with result tracking")
    void testEvaluateWithResult() {
        // Set up context variables
        context.setVariable("amount", 150.0);

        // Test evaluation with result tracking using # prefix for variables
        RuleResult result = expressionEvaluator.evaluateWithResult("#amount > 100", context, Boolean.class);

        assertNotNull(result);
        assertTrue(result.isTriggered());
    }

    @Test
    @DisplayName("Should evaluate quietly without logging")
    void testEvaluateQuietly() {
        // Set up context variables
        context.setVariable("value", 42);

        // Test quiet evaluation using # prefix for variables
        Integer result = expressionEvaluator.evaluateQuietly("#value * 2", context, Integer.class);

        assertNotNull(result);
        assertEquals(84, result);
    }

    @Test
    @DisplayName("Should handle null values gracefully")
    void testNullValueHandling() {
        // Set up context with null value
        context.setVariable("nullValue", null);
        context.setVariable("name", "John");

        // Test null checks using # prefix for variables
        Boolean result1 = expressionEvaluator.evaluate("#nullValue == null", context, Boolean.class);
        assertNotNull(result1);
        assertTrue(result1);

        Boolean result2 = expressionEvaluator.evaluate("#name != null", context, Boolean.class);
        assertNotNull(result2);
        assertTrue(result2);
    }

    @Test
    @DisplayName("Should handle string operations")
    void testStringOperations() {
        // Set up context variables
        context.setVariable("text", "Hello World");

        // Test string methods using # prefix for variables
        String result1 = expressionEvaluator.evaluate("#text.toUpperCase()", context, String.class);
        assertNotNull(result1);
        assertEquals("HELLO WORLD", result1);

        String result2 = expressionEvaluator.evaluate("#text.toLowerCase()", context, String.class);
        assertNotNull(result2);
        assertEquals("hello world", result2);

        Integer result3 = expressionEvaluator.evaluate("#text.length()", context, Integer.class);
        assertNotNull(result3);
        assertEquals(11, result3);

        Boolean result4 = expressionEvaluator.evaluate("#text.startsWith('Hello')", context, Boolean.class);
        assertNotNull(result4);
        assertTrue(result4);
    }
}
