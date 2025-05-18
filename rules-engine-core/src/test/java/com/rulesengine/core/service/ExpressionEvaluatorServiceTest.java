package com.rulesengine.core.service;

import com.rulesengine.core.engine.model.RuleResult;
import com.rulesengine.core.engine.model.RuleResult.ResultType;
import com.rulesengine.core.service.engine.ExpressionEvaluatorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for ExpressionEvaluatorService.
 */
public class ExpressionEvaluatorServiceTest {

    private ExpressionEvaluatorService evaluatorService;
    private StandardEvaluationContext context;

    @BeforeEach
    public void setUp() {
        evaluatorService = new ExpressionEvaluatorService();
        context = new StandardEvaluationContext();

        // Set up some variables in the context
        context.setVariable("intValue", 10);
        context.setVariable("stringValue", "test");
        context.setVariable("booleanValue", true);
    }

    @Test
    public void testEvaluateWithIntegerExpression() {
        // Test a simple arithmetic expression
        Integer result = evaluatorService.evaluate("2 + 2", context, Integer.class);
        assertEquals(4, result);
    }

    @Test
    public void testEvaluateWithStringExpression() {
        // Test string concatenation
        String result = evaluatorService.evaluate("'Hello, ' + 'World!'", context, String.class);
        assertEquals("Hello, World!", result);
    }

    @Test
    public void testEvaluateWithVariables() {
        // Test using variables from the context
        Integer result = evaluatorService.evaluate("#intValue * 2", context, Integer.class);
        assertEquals(20, result);

        String stringResult = evaluatorService.evaluate("#stringValue + ' value'", context, String.class);
        assertEquals("test value", stringResult);

        Boolean booleanResult = evaluatorService.evaluate("!#booleanValue", context, Boolean.class);
        assertFalse(booleanResult);
    }

    @Test
    public void testEvaluateWithInvalidExpression() {
        // Test with an invalid expression
        Object result = evaluatorService.evaluate("invalid expression", context, Object.class);
        assertNull(result);
    }

    @Test
    public void testEvaluateWithResultForBooleanTrue() {
        // Test evaluateWithResult with a boolean expression that evaluates to true
        RuleResult result = evaluatorService.evaluateWithResult("2 > 1", context, Boolean.class);
        assertTrue(result.isTriggered());
        assertNotEquals(ResultType.ERROR, result.getResultType());
        assertEquals("Expression", result.getRuleName());
        assertTrue(result.getMessage().contains("Expression evaluated to true"));
    }

    @Test
    public void testEvaluateWithResultForBooleanFalse() {
        // Test evaluateWithResult with a boolean expression that evaluates to false
        RuleResult result = evaluatorService.evaluateWithResult("2 < 1", context, Boolean.class);
        assertFalse(result.isTriggered());
        assertNotEquals(ResultType.ERROR, result.getResultType());
    }

    @Test
    public void testEvaluateWithResultForNonBoolean() {
        // Test evaluateWithResult with a non-boolean expression
        RuleResult result = evaluatorService.evaluateWithResult("2 + 2", context, Integer.class);
        assertTrue(result.isTriggered());
        assertNotEquals(ResultType.ERROR, result.getResultType());
        assertEquals("Expression", result.getRuleName());
        assertTrue(result.getMessage().contains("Expression evaluated successfully"));
    }

    @Test
    public void testEvaluateWithResultForNull() {
        // Test evaluateWithResult with an expression that evaluates to null
        RuleResult result = evaluatorService.evaluateWithResult("null", context, Object.class);
        assertFalse(result.isTriggered());
        assertNotEquals(ResultType.ERROR, result.getResultType());
    }

    @Test
    public void testEvaluateWithResultForError() {
        // Test evaluateWithResult with an invalid expression
        RuleResult result = evaluatorService.evaluateWithResult("invalid expression", context, Object.class);
        assertFalse(result.isTriggered());
        assertEquals(ResultType.ERROR, result.getResultType());
        assertEquals("Expression", result.getRuleName());
        assertTrue(result.getMessage().contains("Error evaluating expression"));
    }

    @Test
    public void testEvaluateQuietly() {
        // Test evaluateQuietly with a valid expression
        Integer result = evaluatorService.evaluateQuietly("2 + 2", context, Integer.class);
        assertEquals(4, result);
    }

    @Test
    public void testEvaluateQuietlyWithInvalidExpression() {
        // Test evaluateQuietly with an invalid expression
        Object result = evaluatorService.evaluateQuietly("invalid expression", context, Object.class);
        assertNull(result);
    }

    @Test
    public void testGetParser() {
        // Test getParser
        assertNotNull(evaluatorService.getParser());
    }
}
