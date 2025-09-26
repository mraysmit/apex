package dev.mars.apex.core.engine.config;

import dev.mars.apex.core.engine.model.Rule;
import dev.mars.apex.core.engine.model.RuleResult;
import dev.mars.apex.core.service.enrichment.EnrichmentService;
import dev.mars.apex.core.service.engine.ExpressionEvaluatorService;
import dev.mars.apex.core.service.error.ErrorRecoveryService;
import dev.mars.apex.core.service.lookup.LookupServiceRegistry;
import dev.mars.apex.core.service.monitoring.RulePerformanceMonitor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.expression.spel.standard.SpelExpressionParser;

import java.util.HashMap;
import java.util.Map;

/**
 * Simple test to understand the actual behavior of rule evaluation errors.
 */
class SimpleErrorHandlingTest {
    
    private static final Logger logger = LoggerFactory.getLogger(SimpleErrorHandlingTest.class);
    
    private RulesEngine rulesEngine;
    
    @BeforeEach
    void setUp() {
        RulesEngineConfiguration configuration = new RulesEngineConfiguration();
        LookupServiceRegistry serviceRegistry = new LookupServiceRegistry();
        ExpressionEvaluatorService expressionEvaluator = new ExpressionEvaluatorService();
        EnrichmentService enrichmentService = new EnrichmentService(serviceRegistry, expressionEvaluator);
        
        rulesEngine = new RulesEngine(
            configuration,
            new SpelExpressionParser(),
            new ErrorRecoveryService(),
            new RulePerformanceMonitor(),
            enrichmentService
        );
    }
    
    @Test
    void testActualBehaviorOfMissingProperty() {
        logger.info("Testing actual behavior of missing property access");
        
        // Create rule that accesses missing property
        Rule rule = new Rule(
            "missing-property-test",
            "#data.nonExistentField != null",
            "Property should exist",
            "ERROR"
        );
        
        // Create facts without the property
        Map<String, Object> data = new HashMap<>();
        data.put("quantity", 100);
        // Missing "nonExistentField"
        
        Map<String, Object> facts = new HashMap<>();
        facts.put("data", data);
        
        // Execute rule
        RuleResult result = rulesEngine.executeRule(rule, facts);
        
        // Log the actual result
        logger.info("Rule result type: {}", result.getResultType());
        logger.info("Rule result message: {}", result.getMessage());
        logger.info("Rule result severity: {}", result.getSeverity());
        logger.info("Rule result triggered: {}", result.isTriggered());
        logger.info("Rule result success: {}", result.isSuccess());
        
        // This test is just for observation, no assertions
    }
    
    @Test
    void testActualBehaviorOfNullAccess() {
        logger.info("Testing actual behavior of null access");
        
        // Create rule that accesses null field
        Rule rule = new Rule(
            "null-access-test",
            "#data.nullField.toString() != null",
            "Null field access",
            "ERROR"
        );
        
        // Create facts with null field
        Map<String, Object> data = new HashMap<>();
        data.put("nullField", null);
        
        Map<String, Object> facts = new HashMap<>();
        facts.put("data", data);
        
        // Execute rule
        RuleResult result = rulesEngine.executeRule(rule, facts);
        
        // Log the actual result
        logger.info("Null access rule result type: {}", result.getResultType());
        logger.info("Null access rule result message: {}", result.getMessage());
        logger.info("Null access rule result severity: {}", result.getSeverity());
        logger.info("Null access rule result triggered: {}", result.isTriggered());
        
        // This test is just for observation, no assertions
    }
    
    @Test
    void testActualBehaviorOfTypeError() {
        logger.info("Testing actual behavior of type error");
        
        // Create rule that causes type error
        Rule rule = new Rule(
            "type-error-test",
            "#data.stringField > 100",
            "Type error test",
            "WARNING"
        );
        
        // Create facts with string field
        Map<String, Object> data = new HashMap<>();
        data.put("stringField", "not-a-number");
        
        Map<String, Object> facts = new HashMap<>();
        facts.put("data", data);
        
        // Execute rule
        RuleResult result = rulesEngine.executeRule(rule, facts);
        
        // Log the actual result
        logger.info("Type error rule result type: {}", result.getResultType());
        logger.info("Type error rule result message: {}", result.getMessage());
        logger.info("Type error rule result severity: {}", result.getSeverity());
        logger.info("Type error rule result triggered: {}", result.isTriggered());
        
        // This test is just for observation, no assertions
    }
    
    @Test
    void testActualBehaviorOfMethodNotFound() {
        logger.info("Testing actual behavior of method not found");
        
        // Create rule that calls non-existent method
        Rule rule = new Rule(
            "method-not-found-test",
            "#data.value.nonExistentMethod() > 0",
            "Method not found test",
            "CRITICAL"
        );
        
        // Create facts with valid data
        Map<String, Object> data = new HashMap<>();
        data.put("value", 100);
        
        Map<String, Object> facts = new HashMap<>();
        facts.put("data", data);
        
        // Execute rule
        RuleResult result = rulesEngine.executeRule(rule, facts);
        
        // Log the actual result
        logger.info("Method not found rule result type: {}", result.getResultType());
        logger.info("Method not found rule result message: {}", result.getMessage());
        logger.info("Method not found rule result severity: {}", result.getSeverity());
        logger.info("Method not found rule result triggered: {}", result.isTriggered());
        
        // This test is just for observation, no assertions
    }
}
