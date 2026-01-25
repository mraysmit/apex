package dev.mars.apex.core.engine.config;

import dev.mars.apex.core.engine.model.Rule;
import dev.mars.apex.core.engine.model.RuleResult;
import org.junit.jupiter.api.BeforeEach;

import dev.mars.apex.core.test.extension.ColoredTestOutputExtension;
import dev.mars.apex.core.test.extension.TestClassLoggingExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Simple test to understand the actual behavior of rule evaluation errors.
 * Updated to use standard RulesEngine entry point without deprecated EnrichmentService.
 */
@ExtendWith({ColoredTestOutputExtension.class, TestClassLoggingExtension.class})
class SimpleErrorHandlingTest {

    private static final Logger logger = LoggerFactory.getLogger(SimpleErrorHandlingTest.class);

    private RulesEngine rulesEngine;

    @BeforeEach
    void setUp() {
        RulesEngineConfiguration configuration = new RulesEngineConfiguration();
        rulesEngine = new RulesEngine(configuration);
    }
    
    @Test
    void testActualBehaviorOfMissingProperty() {
        logger.info("Testing actual behavior of missing property access");
        
        // Create rule that accesses missing property
        Rule rule = new Rule(
            "missing-property-test",
            "#nonExistentField != null",
            "Property should exist",
            "ERROR"
        );
        
        // Create facts without the property
        Map<String, Object> facts = new HashMap<>();
        facts.put("quantity", 100);
        // Missing "nonExistentField"
        
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
            "#nullField.toString() != null",
            "Null field access",
            "ERROR"
        );
        
        // Create facts with null field
        Map<String, Object> facts = new HashMap<>();
        facts.put("nullField", null);
        
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
            "#stringField > 100",
            "Type error test",
            "WARNING"
        );
        
        // Create facts with string field
        Map<String, Object> facts = new HashMap<>();
        facts.put("stringField", "not-a-number");
        
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
            "#value.nonExistentMethod() > 0",
            "Method not found test",
            "CRITICAL"
        );
        
        // Create facts with valid data
        Map<String, Object> facts = new HashMap<>();
        facts.put("value", 100);
        
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
