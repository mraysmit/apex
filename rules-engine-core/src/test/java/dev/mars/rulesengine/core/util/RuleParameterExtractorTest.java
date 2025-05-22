package dev.mars.rulesengine.core.util;

import dev.mars.rulesengine.core.engine.config.RuleBuilder;
import dev.mars.rulesengine.core.engine.config.RuleGroupBuilder;
import dev.mars.rulesengine.core.engine.model.Rule;
import dev.mars.rulesengine.core.engine.model.RuleGroup;
import dev.mars.rulesengine.core.engine.model.RuleResult;
import dev.mars.rulesengine.core.service.engine.ExpressionEvaluatorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * Test class for RuleParameterExtractor.
 */
public class RuleParameterExtractorTest {

    @Test
    public void testExtractParametersFromCondition() {
        // Test with a simple condition
        String condition = "#age > 18";
        Set<String> parameters = RuleParameterExtractor.extractParameters(condition);
        assertEquals(1, parameters.size());
        assertTrue(parameters.contains("age"));

        // Test with multiple parameters
        condition = "#age > 18 && #income > 50000 && #creditScore > 700";
        parameters = RuleParameterExtractor.extractParameters(condition);
        assertEquals(3, parameters.size());
        assertTrue(parameters.contains("age"));
        assertTrue(parameters.contains("income"));
        assertTrue(parameters.contains("creditScore"));

        // Test with repeated parameters
        condition = "#age > 18 && #age < 65";
        parameters = RuleParameterExtractor.extractParameters(condition);
        assertEquals(1, parameters.size());
        assertTrue(parameters.contains("age"));

        // Test with no parameters
        condition = "true";
        parameters = RuleParameterExtractor.extractParameters(condition);
        assertEquals(0, parameters.size());

        // Test with null condition
        parameters = RuleParameterExtractor.extractParameters((String) null);
        assertEquals(0, parameters.size());

        // Test with empty condition
        parameters = RuleParameterExtractor.extractParameters("");
        assertEquals(0, parameters.size());
    }

    @Test
    public void testExtractParametersFromRule() {
        // Test with a simple rule
        Rule rule = new RuleBuilder()
                .withName("Age Rule")
                .withCondition("#age > 18")
                .withMessage("Person is an adult")
                .build();

        Set<String> parameters = RuleParameterExtractor.extractParameters(rule);
        assertEquals(1, parameters.size());
        assertTrue(parameters.contains("age"));

        // Test with null rule
        parameters = RuleParameterExtractor.extractParameters((Rule) null);
        assertEquals(0, parameters.size());
    }

    @Test
    public void testExtractParametersFromRuleGroup() {
        // Create a rule group with multiple rules
        RuleGroup ruleGroup = new RuleGroupBuilder()
                .withName("Test Rule Group")
                .withDescription("Test rule group")
                .withAndOperator()
                .build();

        Rule rule1 = new RuleBuilder()
                .withName("Age Rule")
                .withCondition("#age > 18")
                .withMessage("Person is an adult")
                .build();

        Rule rule2 = new RuleBuilder()
                .withName("Income Rule")
                .withCondition("#income > 50000")
                .withMessage("Person has good income")
                .build();

        ruleGroup.addRule(rule1, 1);
        ruleGroup.addRule(rule2, 2);

        Set<String> parameters = RuleParameterExtractor.extractParameters(ruleGroup);
        assertEquals(2, parameters.size());
        assertTrue(parameters.contains("age"));
        assertTrue(parameters.contains("income"));

        // Test with null rule group
        parameters = RuleParameterExtractor.extractParameters((RuleGroup) null);
        assertEquals(0, parameters.size());
    }

    @Test
    public void testValidateParameters() {
        // Create a rule with parameters
        Rule rule = new RuleBuilder()
                .withName("Test Rule")
                .withCondition("#age > 18 && #income > 50000")
                .withMessage("Test message")
                .build();

        // Create a facts map with all parameters
        Map<String, Object> facts = new HashMap<>();
        facts.put("age", 30);
        facts.put("income", 60000);

        // Validate parameters - should be empty (all parameters are present)
        Set<String> missingParameters = RuleParameterExtractor.validateParameters(rule, facts);
        assertTrue(missingParameters.isEmpty());

        // Create a facts map with missing parameters
        Map<String, Object> incompleteFacts = new HashMap<>();
        incompleteFacts.put("age", 30);

        // Validate parameters - should contain "income"
        missingParameters = RuleParameterExtractor.validateParameters(rule, incompleteFacts);
        assertEquals(1, missingParameters.size());
        assertTrue(missingParameters.contains("income"));

        // Validate parameters with null facts
        missingParameters = RuleParameterExtractor.validateParameters(rule, null);
        assertEquals(2, missingParameters.size());
        assertTrue(missingParameters.contains("age"));
        assertTrue(missingParameters.contains("income"));
    }

    @Test
    public void testValidateParametersForRuleGroup() {
        // Create a rule group with multiple rules
        RuleGroup ruleGroup = new RuleGroupBuilder()
                .withName("Test Rule Group")
                .withDescription("Test rule group")
                .withAndOperator()
                .build();

        Rule rule1 = new RuleBuilder()
                .withName("Age Rule")
                .withCondition("#age > 18")
                .withMessage("Person is an adult")
                .build();

        Rule rule2 = new RuleBuilder()
                .withName("Income Rule")
                .withCondition("#income > 50000")
                .withMessage("Person has good income")
                .build();

        ruleGroup.addRule(rule1, 1);
        ruleGroup.addRule(rule2, 2);

        // Create a facts map with all parameters
        Map<String, Object> facts = new HashMap<>();
        facts.put("age", 30);
        facts.put("income", 60000);

        // Validate parameters - should be empty (all parameters are present)
        Set<String> missingParameters = RuleParameterExtractor.validateParameters(ruleGroup, facts);
        assertTrue(missingParameters.isEmpty());

        // Create a facts map with missing parameters
        Map<String, Object> incompleteFacts = new HashMap<>();
        incompleteFacts.put("age", 30);

        // Validate parameters - should contain "income"
        missingParameters = RuleParameterExtractor.validateParameters(ruleGroup, incompleteFacts);
        assertEquals(1, missingParameters.size());
        assertTrue(missingParameters.contains("income"));
    }

    @Test
    public void testEnsureParameters() {
        // Create a rule with parameters
        Rule rule = new RuleBuilder()
                .withName("Test Rule")
                .withCondition("#age > 18 && #income > 50000")
                .withMessage("Test message")
                .build();

        // Create a facts map with missing parameters
        Map<String, Object> incompleteFacts = new HashMap<>();
        incompleteFacts.put("age", 30);

        // Ensure parameters - should add "income" with null value
        Map<String, Object> updatedFacts = RuleParameterExtractor.ensureParameters(rule, incompleteFacts);
        assertEquals(2, updatedFacts.size());
        assertTrue(updatedFacts.containsKey("age"));
        assertTrue(updatedFacts.containsKey("income"));
        assertEquals(30, updatedFacts.get("age"));
        assertNull(updatedFacts.get("income"));

        // Ensure parameters with null facts
        updatedFacts = RuleParameterExtractor.ensureParameters(rule, null);
        assertEquals(2, updatedFacts.size());
        assertTrue(updatedFacts.containsKey("age"));
        assertTrue(updatedFacts.containsKey("income"));
        assertNull(updatedFacts.get("age"));
        assertNull(updatedFacts.get("income"));
    }

    @Test
    public void testEnsureParametersForRuleGroup() {
        // Create a rule group with multiple rules
        RuleGroup ruleGroup = new RuleGroupBuilder()
                .withName("Test Rule Group")
                .withDescription("Test rule group")
                .withAndOperator()
                .build();

        Rule rule1 = new RuleBuilder()
                .withName("Age Rule")
                .withCondition("#age > 18")
                .withMessage("Person is an adult")
                .build();

        Rule rule2 = new RuleBuilder()
                .withName("Income Rule")
                .withCondition("#income > 50000 && #creditScore > 700")
                .withMessage("Person has good income")
                .build();

        ruleGroup.addRule(rule1, 1);
        ruleGroup.addRule(rule2, 2);

        // Create a facts map with missing parameters
        Map<String, Object> incompleteFacts = new HashMap<>();
        incompleteFacts.put("age", 30);
        incompleteFacts.put("income", 60000);

        // Ensure parameters - should add "creditScore" with null value
        Map<String, Object> updatedFacts = RuleParameterExtractor.ensureParameters(ruleGroup, incompleteFacts);
        assertEquals(3, updatedFacts.size());
        assertTrue(updatedFacts.containsKey("age"));
        assertTrue(updatedFacts.containsKey("income"));
        assertTrue(updatedFacts.containsKey("creditScore"));
        assertEquals(30, updatedFacts.get("age"));
        assertEquals(60000, updatedFacts.get("income"));
        assertNull(updatedFacts.get("creditScore"));
    }

    @Test
    public void testComplexParameterPatterns() {
        // Test with parameters in complex expressions
        String condition = "#customer.age > 18 && #customer.income > 50000";
        Set<String> parameters = RuleParameterExtractor.extractParameters(condition);
        assertEquals(1, parameters.size());
        assertTrue(parameters.contains("customer"));

        // Test with parameters in method calls
        condition = "#service.calculatePrice(#basePrice) > 100";
        parameters = RuleParameterExtractor.extractParameters(condition);
        assertEquals(2, parameters.size());
        assertTrue(parameters.contains("service"));
        assertTrue(parameters.contains("basePrice"));

        // Test with parameters in array/list access
        condition = "#items[0] == 'test' && #items.size() > 1";
        parameters = RuleParameterExtractor.extractParameters(condition);
        assertEquals(1, parameters.size());
        assertTrue(parameters.contains("items"));

        // Test with parameters in nested expressions
        condition = "(#a || #b) && (#c || #d)";
        parameters = RuleParameterExtractor.extractParameters(condition);
        assertEquals(4, parameters.size());
        assertTrue(parameters.contains("a"));
        assertTrue(parameters.contains("b"));
        assertTrue(parameters.contains("c"));
        assertTrue(parameters.contains("d"));
    }

    /**
     * Test class for ExpressionEvaluatorService.
     */
    public static class ExpressionEvaluatorServiceTest {

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
            assertNotEquals(RuleResult.ResultType.ERROR, result.getResultType());
            assertEquals("Expression", result.getRuleName());
            assertTrue(result.getMessage().contains("Expression evaluated to true"));
        }

        @Test
        public void testEvaluateWithResultForBooleanFalse() {
            // Test evaluateWithResult with a boolean expression that evaluates to false
            RuleResult result = evaluatorService.evaluateWithResult("2 < 1", context, Boolean.class);
            assertFalse(result.isTriggered());
            assertNotEquals(RuleResult.ResultType.ERROR, result.getResultType());
        }

        @Test
        public void testEvaluateWithResultForNonBoolean() {
            // Test evaluateWithResult with a non-boolean expression
            RuleResult result = evaluatorService.evaluateWithResult("2 + 2", context, Integer.class);
            assertTrue(result.isTriggered());
            assertNotEquals(RuleResult.ResultType.ERROR, result.getResultType());
            assertEquals("Expression", result.getRuleName());
            assertTrue(result.getMessage().contains("Expression evaluated successfully"));
        }

        @Test
        public void testEvaluateWithResultForNull() {
            // Test evaluateWithResult with an expression that evaluates to null
            RuleResult result = evaluatorService.evaluateWithResult("null", context, Object.class);
            assertFalse(result.isTriggered());
            assertNotEquals(RuleResult.ResultType.ERROR, result.getResultType());
        }

        @Test
        public void testEvaluateWithResultForError() {
            // Test evaluateWithResult with an invalid expression
            RuleResult result = evaluatorService.evaluateWithResult("invalid expression", context, Object.class);
            assertFalse(result.isTriggered());
            assertEquals(RuleResult.ResultType.ERROR, result.getResultType());
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
}