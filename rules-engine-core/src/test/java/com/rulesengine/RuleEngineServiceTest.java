package com.rulesengine;

import com.rulesengine.core.engine.model.Rule;
import com.rulesengine.core.service.engine.RuleEngineService;
import com.rulesengine.core.service.engine.ExpressionEvaluatorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for RuleEngineService.
 */
public class RuleEngineServiceTest {

    private RuleEngineService ruleEngineService;
    private ExpressionEvaluatorService evaluatorService;
    private ByteArrayOutputStream outContent;
    private PrintStream originalOut;

    @BeforeEach
    public void setUp() {
        evaluatorService = new ExpressionEvaluatorService();
        ruleEngineService = new RuleEngineService(evaluatorService);

        // Capture System.out to verify output
        outContent = new ByteArrayOutputStream();
        originalOut = System.out;
        System.setOut(new PrintStream(outContent));
    }

    @Test
    public void testEvaluateRules() {
        // Create a simple rule
        Rule rule = new Rule(
            "Test Rule",
            "2 + 2 == 4",
            "Simple arithmetic test"
        );

        List<Rule> rules = new ArrayList<>();
        rules.add(rule);

        // Create a context
        StandardEvaluationContext context = new StandardEvaluationContext();

        // Evaluate the rule
        ruleEngineService.evaluateRules(rules, context);

        // Verify output
        String output = outContent.toString();
        assertTrue(output.contains("Test Rule: Simple arithmetic test"));
        assertTrue(output.contains("Result: true"));
    }

    @Test
    public void testEvaluateRulesWithVariables() {
        // Create a rule that uses variables
        Rule rule = new Rule(
            "Variable Test",
            "#value > 10",
            "Value is greater than 10"
        );

        List<Rule> rules = new ArrayList<>();
        rules.add(rule);

        // Create a context with a variable
        StandardEvaluationContext context = new StandardEvaluationContext();
        context.setVariable("value", 15);

        // Evaluate the rule
        ruleEngineService.evaluateRules(rules, context);

        // Verify output
        String output = outContent.toString();
        assertTrue(output.contains("Variable Test: Value is greater than 10"));
        assertTrue(output.contains("Result: true"));
    }

    @Test
    public void testEvaluateRulesWithError() {
        // Create a rule with an invalid expression
        Rule rule = new Rule(
            "Error Test",
            "invalid expression",
            "This should cause an error"
        );

        List<Rule> rules = new ArrayList<>();
        rules.add(rule);

        // Redirect System.err to capture error messages
        ByteArrayOutputStream errContent = new ByteArrayOutputStream();
        PrintStream originalErr = System.err;
        System.setErr(new PrintStream(errContent));

        try {
            // Create a context
            StandardEvaluationContext context = new StandardEvaluationContext();

            // Evaluate the rule
            ruleEngineService.evaluateRules(rules, context);

            // Verify error output
            String errorOutput = errContent.toString();
            // Just check if there's any error output, as the exact format might vary
            assertFalse(errorOutput.isEmpty());
        } finally {
            // Restore System.err
            System.setErr(originalErr);
        }
    }
}
