package dev.mars.apex.core.engine.config;

import dev.mars.apex.core.config.error.ErrorRecoveryConfig;
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

import static org.junit.jupiter.api.Assertions.*;

/**
 * Debug test to understand why ErrorHandlingProofTestRunner is failing.
 */
class ErrorRecoveryDebugTest {
    
    private static final Logger logger = LoggerFactory.getLogger(ErrorRecoveryDebugTest.class);
    
    private RulesEngine rulesEngine;
    private ErrorRecoveryConfig errorRecoveryConfig;
    
    @BeforeEach
    void setUp() {
        logger.info("Setting up debug test");
        
        // Create configuration and rules engine
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
        
        // Create error recovery config to check defaults
        errorRecoveryConfig = new ErrorRecoveryConfig();
    }
    
    @Test
    void testErrorRecoveryConfigDefaults() {
        logger.info("Testing ErrorRecoveryConfig defaults");

        // Check that CRITICAL severity is configured to not recover
        boolean criticalRecoveryEnabled = errorRecoveryConfig.isRecoveryEnabledForSeverity("CRITICAL");
        logger.info("CRITICAL severity recovery enabled: {}", criticalRecoveryEnabled);
        assertFalse(criticalRecoveryEnabled, "CRITICAL severity should not have recovery enabled by default");

        // Check that ERROR severity is configured to not recover
        boolean errorRecoveryEnabled = errorRecoveryConfig.isRecoveryEnabledForSeverity("ERROR");
        logger.info("ERROR severity recovery enabled: {}", errorRecoveryEnabled);
        assertFalse(errorRecoveryEnabled, "ERROR severity should not have recovery enabled by default");

        // Check WARNING severity
        boolean warningRecoveryEnabled = errorRecoveryConfig.isRecoveryEnabledForSeverity("WARNING");
        logger.info("WARNING severity recovery enabled: {}", warningRecoveryEnabled);
        assertTrue(warningRecoveryEnabled, "WARNING severity should have recovery enabled by default");
    }
    
    @Test
    void testErrorSeverityRuleEvaluation() {
        logger.info("Testing ERROR severity rule evaluation");

        // Create a rule that will fail with ERROR severity
        Rule rule = new Rule("test-rule", "#data.missing != null", "Test", "ERROR");
        Map<String, Object> facts = createEmptyFacts();

        logger.info("Executing rule: {}", rule.getName());
        logger.info("Rule severity: {}", rule.getSeverity());
        logger.info("ErrorRecoveryConfig recovery enabled for ERROR: {}", errorRecoveryConfig.isRecoveryEnabledForSeverity("ERROR"));

        RuleResult result = rulesEngine.executeRule(rule, facts);

        logger.info("Result type: {}", result.getResultType());
        logger.info("Result message: {}", result.getMessage());
        logger.info("Result severity: {}", result.getSeverity());

        // This should return ERROR result type, not NO_MATCH
        assertEquals(RuleResult.ResultType.ERROR, result.getResultType(),
                    "ERROR severity rule should return ERROR result type");
    }

    @Test
    void testCriticalSeverityRuleEvaluation() {
        logger.info("Testing CRITICAL severity rule evaluation");

        // Create a rule that will fail with CRITICAL severity
        Rule rule = new Rule("test-rule", "#data.missing != null", "Test", "CRITICAL");
        Map<String, Object> facts = createEmptyFacts();

        logger.info("Executing rule: {}", rule.getName());
        logger.info("Rule severity: {}", rule.getSeverity());
        logger.info("ErrorRecoveryConfig recovery enabled for CRITICAL: {}", errorRecoveryConfig.isRecoveryEnabledForSeverity("CRITICAL"));

        RuleResult result = rulesEngine.executeRule(rule, facts);

        logger.info("Result type: {}", result.getResultType());
        logger.info("Result message: {}", result.getMessage());
        logger.info("Result severity: {}", result.getSeverity());

        // This should return ERROR result type, not NO_MATCH
        assertEquals(RuleResult.ResultType.ERROR, result.getResultType(),
                    "CRITICAL severity rule should return ERROR result type");
    }
    
    @Test
    void testWarningSeverityRuleEvaluation() {
        logger.info("Testing WARNING severity rule evaluation");
        
        // Create a rule that will fail with WARNING severity
        Rule rule = new Rule("warning-rule", "#data.missing != null", "Test", "WARNING");
        Map<String, Object> facts = createEmptyFacts();
        
        logger.info("Executing rule: {}", rule.getName());
        RuleResult result = rulesEngine.executeRule(rule, facts);
        
        logger.info("Result type: {}", result.getResultType());
        logger.info("Result message: {}", result.getMessage());
        logger.info("Result severity: {}", result.getSeverity());
        
        // This should return NO_MATCH (recovered) for WARNING severity
        assertEquals(RuleResult.ResultType.NO_MATCH, result.getResultType(),
                    "WARNING severity rule should be recovered and return NO_MATCH");
    }

    @Test
    void testExactReplicationOfErrorHandlingProofTestRunner() {
        logger.info("Testing exact replication of ErrorHandlingProofTestRunner logic");

        // This is the exact same test that's failing in ErrorHandlingProofTestRunner
        Rule rule = new Rule("test-rule", "#data.missing != null", "Test", "CRITICAL");
        Map<String, Object> facts = createEmptyFacts();

        logger.info("Executing rule: {}", rule.getName());
        logger.info("Rule condition: {}", rule.getCondition());
        logger.info("Rule severity: {}", rule.getSeverity());
        logger.info("Facts: {}", facts);

        RuleResult result = rulesEngine.executeRule(rule, facts);

        logger.info("Result type: {}", result.getResultType());
        logger.info("Result message: {}", result.getMessage());
        logger.info("Result severity: {}", result.getSeverity());

        // This is the exact assertion that's failing
        assertEquals(RuleResult.ResultType.ERROR, result.getResultType(),
                    "Should return ERROR result type");
    }

    private Map<String, Object> createEmptyFacts() {
        Map<String, Object> facts = new HashMap<>();
        facts.put("data", new HashMap<String, Object>());
        return facts;
    }
}
