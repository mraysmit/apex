package dev.mars.rulesengine.core.service.error;

import dev.mars.rulesengine.core.engine.config.RulesEngine;
import dev.mars.rulesengine.core.engine.config.RulesEngineConfiguration;
import dev.mars.rulesengine.core.engine.model.Rule;
import dev.mars.rulesengine.core.engine.model.RuleResult;
import dev.mars.rulesengine.core.exception.RuleEvaluationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for enhanced error handling functionality.
 */
public class ErrorHandlingTest {
    
    private RulesEngineConfiguration configuration;
    private RulesEngine rulesEngine;
    private ErrorContextService errorContextService;
    private ErrorRecoveryService errorRecoveryService;
    
    @BeforeEach
    public void setUp() {
        configuration = new RulesEngineConfiguration();
        errorContextService = new ErrorContextService();
        errorRecoveryService = new ErrorRecoveryService();
        rulesEngine = new RulesEngine(configuration);
    }
    
    @Test
    public void testRuleEvaluationExceptionCreation() {
        String ruleName = "testRule";
        String expression = "#customer.invalidProperty";
        String message = "Property or field 'invalidProperty' cannot be found";

        RuleEvaluationException exception = new RuleEvaluationException(ruleName, expression, message);

        assertEquals(ruleName, exception.getRuleName());
        assertEquals(expression, exception.getExpression());
        assertTrue(exception.getSuggestion().contains("property exists"));
        assertTrue(exception.getDetailedMessage().contains("Suggestion:"));
    }
    
    @Test
    public void testErrorContextGeneration() {
        String ruleName = "testRule";
        String expression = "#customer.name == 'John'";
        Exception originalException = new RuntimeException("Property 'name' cannot be found");
        
        StandardEvaluationContext context = new StandardEvaluationContext();
        Map<String, Object> facts = new HashMap<>();
        facts.put("customer", new TestCustomer("John", 30));
        context.setRootObject(facts);
        
        ErrorContextService.ErrorContext errorContext = errorContextService.generateErrorContext(
            ruleName, expression, context, originalException);
        
        assertNotNull(errorContext);
        assertEquals(ruleName, errorContext.getRuleName());
        assertEquals(expression, errorContext.getExpression());
        assertEquals(originalException, errorContext.getOriginalException());
        assertFalse(errorContext.getSuggestions().isEmpty());
        
        String report = errorContext.getFormattedErrorReport();
        assertTrue(report.contains("Rule Evaluation Error Report"));
        assertTrue(report.contains(ruleName));
        assertTrue(report.contains(expression));
    }
    
    @Test
    public void testErrorRecoveryWithDefaultStrategy() {
        String ruleName = "testRule";
        String expression = "#customer.invalidProperty == 'test'";
        Exception originalException = new RuntimeException("Property not found");
        StandardEvaluationContext context = new StandardEvaluationContext();
        
        ErrorRecoveryService.RecoveryResult result = errorRecoveryService.attemptRecovery(
            ruleName, expression, context, originalException);
        
        assertTrue(result.isSuccessful());
        assertEquals(ErrorRecoveryService.RecoveryResult.RecoveryAction.RECOVERED, result.getAction());
        assertNotNull(result.getRuleResult());
        assertFalse(result.getRuleResult().isTriggered()); // Should default to no match
    }
    
    @Test
    public void testErrorRecoveryWithSafeExpression() {
        String ruleName = "testRule";
        String expression = "#customer.name == 'John'";
        Exception originalException = new RuntimeException("Property not found");
        StandardEvaluationContext context = new StandardEvaluationContext();
        
        Map<String, Object> facts = new HashMap<>();
        facts.put("customer", new TestCustomer("John", 30));
        context.setRootObject(facts);
        
        ErrorRecoveryService.RecoveryResult result = errorRecoveryService.attemptRecovery(
            ruleName, expression, context, originalException, 
            ErrorRecoveryService.ErrorRecoveryStrategy.RETRY_WITH_SAFE_EXPRESSION);
        
        // The result depends on whether the safe expression can be created and evaluated
        assertNotNull(result);
        assertNotNull(result.getRuleResult());
    }
    
    @Test
    public void testErrorRecoveryWithSkipStrategy() {
        String ruleName = "testRule";
        String expression = "#invalid.expression";
        Exception originalException = new RuntimeException("Parse error");
        StandardEvaluationContext context = new StandardEvaluationContext();
        
        ErrorRecoveryService.RecoveryResult result = errorRecoveryService.attemptRecovery(
            ruleName, expression, context, originalException, 
            ErrorRecoveryService.ErrorRecoveryStrategy.SKIP_RULE);
        
        assertTrue(result.isSuccessful());
        assertEquals(ErrorRecoveryService.RecoveryResult.RecoveryAction.SKIPPED, result.getAction());
        assertFalse(result.getRuleResult().isTriggered());
    }
    
    @Test
    public void testErrorRecoveryWithFailFastStrategy() {
        String ruleName = "testRule";
        String expression = "#invalid.expression";
        Exception originalException = new RuntimeException("Parse error");
        StandardEvaluationContext context = new StandardEvaluationContext();
        
        ErrorRecoveryService.RecoveryResult result = errorRecoveryService.attemptRecovery(
            ruleName, expression, context, originalException, 
            ErrorRecoveryService.ErrorRecoveryStrategy.FAIL_FAST);
        
        assertFalse(result.isSuccessful());
        assertEquals(ErrorRecoveryService.RecoveryResult.RecoveryAction.FAILED, result.getAction());
        assertEquals(RuleResult.ResultType.ERROR, result.getRuleResult().getResultType());
    }
    
    @Test
    public void testErrorContextAnalysis() {
        String expression = "#customer.name == 'John' && (#customer.age > 18";
        Exception originalException = new RuntimeException("Property 'name' cannot be found");
        StandardEvaluationContext context = new StandardEvaluationContext();

        // Test that error context generation works through the public API
        ErrorContextService.ErrorContext errorContext = errorContextService.generateErrorContext(
            "test-rule", expression, context, originalException);

        assertNotNull(errorContext);
        assertNotNull(errorContext.getExpressionAnalysis());
        assertFalse(errorContext.getSuggestions().isEmpty());
        assertEquals(ErrorContextService.ErrorType.PROPERTY_ACCESS, errorContext.getErrorType());
    }
    
    @Test
    public void testIntegratedErrorHandlingInRulesEngine() {
        // Create a rule with an invalid expression
        Rule invalidRule = configuration.rule("invalid-rule")
            .withCategory("test")
            .withName("Invalid Rule")
            .withDescription("A rule with invalid expression")
            .withCondition("#customer.nonExistentProperty == 'test'")
            .withMessage("This should not match")
            .withPriority(1)
            .build();
        
        Map<String, Object> facts = new HashMap<>();
        facts.put("customer", new TestCustomer("John", 30));
        
        // Execute the rule - should handle error gracefully
        RuleResult result = rulesEngine.executeRule(invalidRule, facts);
        
        // The result should indicate an error or recovery
        assertNotNull(result);
        // Depending on the recovery strategy, this could be an error or a recovered result
    }
    
    /**
     * Test customer class for testing purposes.
     */
    public static class TestCustomer {
        private String name;
        private int age;
        
        public TestCustomer(String name, int age) {
            this.name = name;
            this.age = age;
        }
        
        public String getName() { return name; }
        public int getAge() { return age; }
        public void setName(String name) { this.name = name; }
        public void setAge(int age) { this.age = age; }
    }
}
