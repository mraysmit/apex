package dev.mars.rulesengine.demo.logging;

import dev.mars.rulesengine.core.engine.config.RulesEngine;
import dev.mars.rulesengine.core.engine.config.RulesEngineConfiguration;
import dev.mars.rulesengine.core.engine.model.Rule;
import dev.mars.rulesengine.core.engine.model.RuleResult;
import dev.mars.rulesengine.core.util.LoggingContext;
import dev.mars.rulesengine.core.util.RulesEngineLogger;
import org.slf4j.bridge.SLF4JBridgeHandler;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.LogManager;

/**
 * Demonstration of the enhanced logging capabilities in the SpEL Rules Engine.
 * This demo shows:
 * - Structured logging with correlation IDs
 * - Performance monitoring
 * - Context management
 * - Audit logging
 * - Error handling with recovery
 */
public class LoggingImprovementsDemo {
    
    private static final RulesEngineLogger logger = new RulesEngineLogger(LoggingImprovementsDemo.class);
    
    public static void main(String[] args) {
        // Set up JUL-to-SLF4J bridge for backward compatibility
        setupJulToSlf4jBridge();

        logger.info("Starting Logging Improvements Demo");

        // Initialize correlation context for the entire demo
        String correlationId = LoggingContext.initializeContext();
        logger.info("Demo session started with correlation ID: {}", correlationId);
        
        try {
            // Demo 1: Basic rule evaluation with enhanced logging
            demonstrateBasicRuleEvaluation();
            
            // Demo 2: Performance monitoring
            demonstratePerformanceMonitoring();
            
            // Demo 3: Error handling and recovery
            demonstrateErrorHandling();
            
            // Demo 4: Audit logging
            demonstrateAuditLogging();
            
            // Demo 5: Context management
            demonstrateContextManagement();
            
        } catch (Exception e) {
            logger.error("Demo execution failed", e);
        } finally {
            LoggingContext.clearContext();
            logger.info("Demo completed");
        }
    }
    
    private static void demonstrateBasicRuleEvaluation() {
        logger.info("=== Demo 1: Basic Rule Evaluation with Enhanced Logging ===");
        
        // Create rules engine with enhanced logging
        RulesEngineConfiguration config = new RulesEngineConfiguration();
        RulesEngine engine = new RulesEngine(config);
        
        // Create a simple rule
        Rule customerRule = new Rule("customer-validation", 
                                   "#age >= 18 && #income > 30000", 
                                   "Customer is eligible for premium service");
        
        // Prepare facts
        Map<String, Object> facts = new HashMap<>();
        facts.put("age", 25);
        facts.put("income", 45000);
        
        // Execute rule - this will demonstrate enhanced logging
        RuleResult result = engine.executeRule(customerRule, facts);
        
        logger.info("Rule evaluation result: {}", result.isTriggered() ? "MATCHED" : "NO_MATCH");
        
        if (result.hasPerformanceMetrics()) {
            logger.performance(result.getRuleName(), 
                             result.getPerformanceMetrics().getEvaluationTimeMillis(),
                             null);
        }
    }
    
    private static void demonstratePerformanceMonitoring() {
        logger.info("=== Demo 2: Performance Monitoring ===");
        
        RulesEngineConfiguration config = new RulesEngineConfiguration();
        RulesEngine engine = new RulesEngine(config);
        
        // Create a more complex rule that might be slower
        Rule complexRule = new Rule("complex-validation", 
                                  "#customer.address.country == 'US' && " +
                                  "#customer.creditScore > 700 && " +
                                  "#customer.accountHistory.size() > 5", 
                                  "Customer qualifies for premium rates");
        
        // Create nested object structure
        Map<String, Object> customer = new HashMap<>();
        Map<String, Object> address = new HashMap<>();
        address.put("country", "US");
        customer.put("address", address);
        customer.put("creditScore", 750);
        customer.put("accountHistory", java.util.Arrays.asList("acc1", "acc2", "acc3", "acc4", "acc5", "acc6"));
        
        Map<String, Object> facts = new HashMap<>();
        facts.put("customer", customer);
        
        // Execute rule multiple times to show performance patterns
        for (int i = 0; i < 3; i++) {
            LoggingContext.setRulePhase("iteration-" + (i + 1));
            RuleResult result = engine.executeRule(complexRule, facts);
            
            if (result.hasPerformanceMetrics()) {
                double evaluationTime = result.getPerformanceMetrics().getEvaluationTimeMillis();
                
                // Simulate slow rule warning
                if (evaluationTime > 5.0) { // Threshold of 5ms
                    logger.slowRule(complexRule.getName(), evaluationTime, 5.0);
                } else {
                    logger.performance(complexRule.getName(), evaluationTime, null);
                }
            }
        }
        
        LoggingContext.clearRuleContext();
    }
    
    private static void demonstrateErrorHandling() {
        logger.info("=== Demo 3: Error Handling and Recovery ===");
        
        RulesEngineConfiguration config = new RulesEngineConfiguration();
        RulesEngine engine = new RulesEngine(config);
        
        // Create a rule with an intentional error
        Rule errorRule = new Rule("error-prone-rule", 
                                "#nonExistentProperty.someMethod()", 
                                "This rule will fail");
        
        Map<String, Object> facts = new HashMap<>();
        facts.put("validProperty", "value");
        
        // Execute rule - this will trigger error handling and recovery
        RuleResult result = engine.executeRule(errorRule, facts);
        
        logger.info("Error rule result: {}", result.getResultType());
        
        if (result.getResultType() == RuleResult.ResultType.ERROR) {
            logger.warn("Rule execution failed as expected: {}", result.getMessage());
        }
    }
    
    private static void demonstrateAuditLogging() {
        logger.info("=== Demo 4: Audit Logging ===");
        
        // Simulate user context
        LoggingContext.setUserId("demo-user-123");
        
        // Log various audit events
        logger.audit("RULE_CREATION", "new-customer-rule", "New rule created for customer validation");
        logger.audit("RULE_EXECUTION", "customer-validation", "Rule executed for customer ID: CUST-456");
        logger.audit("CONFIGURATION_CHANGE", null, "Rules engine configuration updated");
        
        // Use the utility method for audit logging
        LoggingContext.auditLog("SYSTEM_EVENT", null, "Demo audit logging completed");
        
        LoggingContext.clearKey(LoggingContext.USER_ID);
    }
    
    private static void demonstrateContextManagement() {
        logger.info("=== Demo 5: Context Management ===");
        
        // Demonstrate context scoping
        LoggingContext.withRuleContext("scoped-rule", "validation", () -> {
            logger.info("Inside scoped context - rule name and phase are automatically set");
            
            // Nested context
            LoggingContext.withCorrelationId("nested-correlation-456", () -> {
                logger.info("Inside nested correlation context");
                logger.debug("Current correlation ID: {}", LoggingContext.getCorrelationId());
            });
            
            logger.info("Back to original correlation context");
        });
        
        logger.info("Context cleared - back to original state");
        
        // Demonstrate manual context management
        LoggingContext.setRuleName("manual-rule");
        LoggingContext.setRulePhase("processing");
        LoggingContext.setEvaluationTime(42.5);
        
        logger.info("Manual context set");
        logger.debug("Context data: {}", LoggingContext.getContextData());
        
        LoggingContext.clearRuleContext();
        logger.info("Rule context cleared, correlation ID preserved");
    }
    
    /**
     * Utility method to demonstrate lazy logging for expensive operations.
     */
    private static void demonstrateLazyLogging() {
        logger.info("=== Demo 6: Lazy Logging ===");
        
        // This expensive operation will only be called if DEBUG logging is enabled
        logger.debug(() -> {
            // Simulate expensive operation
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 1000; i++) {
                sb.append("expensive-").append(i).append(" ");
            }
            return "Expensive debug message: " + sb.toString();
        });
        
        // Conditional logging for performance
        if (logger.isDebugEnabled()) {
            String expensiveResult = performExpensiveOperation();
            logger.debug("Expensive operation result: {}", expensiveResult);
        }
    }
    
    private static String performExpensiveOperation() {
        // Simulate expensive operation
        try {
            Thread.sleep(10); // Simulate work
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return "expensive-result";
    }

    /**
     * Set up JUL-to-SLF4J bridge to route old Java Util Logging through SLF4J.
     * This allows existing demo files using JUL to work with the new logging system.
     */
    private static void setupJulToSlf4jBridge() {
        // Remove existing handlers attached to j.u.l root logger
        LogManager.getLogManager().reset();

        // Add SLF4JBridgeHandler to j.u.l's root logger
        SLF4JBridgeHandler.install();

        logger.info("JUL-to-SLF4J bridge installed - old logging will now route through SLF4J");
    }
}
