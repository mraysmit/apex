package dev.mars.apex.core.service.engine;

import dev.mars.apex.core.engine.model.Rule;
import dev.mars.apex.core.engine.model.RuleResult;
import dev.mars.apex.core.engine.model.RuleGroup;
import dev.mars.apex.core.engine.model.RuleBase;
import dev.mars.apex.core.service.monitoring.RulePerformanceMonitor;
import dev.mars.apex.core.service.monitoring.RulePerformanceMetrics;
import dev.mars.apex.core.service.error.ErrorRecoveryService;
import dev.mars.apex.core.util.RulesEngineLogger;
import dev.mars.apex.core.util.TestAwareLogger;
import dev.mars.apex.core.util.RuleParameterExtractor;
import dev.mars.apex.core.util.LoggingContext;
import dev.mars.apex.core.engine.config.MapPropertyAccessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Unified Rule Evaluator - Single evaluation engine for all APEX rule evaluation paths.
 * 
 * This class consolidates all rule evaluation logic into a single, consistent implementation
 * that provides standardized error handling, performance monitoring, and result formatting.
 * 
 * Key Features:
 * - Single SpEL evaluation path for consistency
 * - Standardized error message format: "Rule evaluation failed: {ruleName} - {exception}"
 * - Centralized error recovery logic
 * - Comprehensive logging and metrics
 * - Graceful error handling following APEX principles
 * 
 * @author APEX Rules Engine
 * @since Phase 1 - Unified Evaluation Engine
 * @version 1.0
 */
public class UnifiedRuleEvaluator {

    private static final Logger logger = LoggerFactory.getLogger(UnifiedRuleEvaluator.class);
    private static final RulesEngineLogger rulesLogger = new RulesEngineLogger(UnifiedRuleEvaluator.class);

    private final ExpressionParser parser;
    private final ErrorRecoveryService errorRecoveryService;
    private final RulePerformanceMonitor performanceMonitor;
    
    /**
     * Standard error message format for consistency across all evaluation paths.
     */
    private static final String ERROR_MESSAGE_FORMAT = "Rule evaluation failed: %s - %s";
    
    /**
     * Create a new UnifiedRuleEvaluator with default components.
     */
    public UnifiedRuleEvaluator() {
        this.parser = new SpelExpressionParser();
        this.errorRecoveryService = new ErrorRecoveryService();
        this.performanceMonitor = new RulePerformanceMonitor();
    }
    
    /**
     * Create a new UnifiedRuleEvaluator with custom components.
     * 
     * @param parser The SpEL expression parser
     * @param errorRecoveryService The error recovery service
     * @param performanceMonitor The performance monitor
     */
    public UnifiedRuleEvaluator(ExpressionParser parser, 
                               ErrorRecoveryService errorRecoveryService,
                               RulePerformanceMonitor performanceMonitor) {
        this.parser = parser != null ? parser : new SpelExpressionParser();
        this.errorRecoveryService = errorRecoveryService != null ? errorRecoveryService : new ErrorRecoveryService();
        this.performanceMonitor = performanceMonitor != null ? performanceMonitor : new RulePerformanceMonitor();
    }
    
    /**
     * Evaluate a single rule against the provided context.
     * This is the core evaluation method that all other methods delegate to.
     * 
     * @param rule The rule to evaluate
     * @param context The evaluation context
     * @return The rule evaluation result
     */
    public RuleResult evaluateRule(Rule rule, EvaluationContext context) {
        if (rule == null) {
            rulesLogger.info("No rule provided for evaluation");
            return RuleResult.noRules();
        }
        
        // Set up logging context
        LoggingContext.setRuleName(rule.getName());
        LoggingContext.setRulePhase("evaluation");
        rulesLogger.info("Starting rule evaluation: {}", rule.getName());

        // Start performance monitoring
        RulePerformanceMetrics.Builder metricsBuilder = performanceMonitor.startEvaluation(rule.getName(), "evaluation");
        
        try {
            // Validate rule has required condition
            if (rule.getCondition() == null || rule.getCondition().trim().isEmpty()) {
                TestAwareLogger.warn(rulesLogger, "Rule '{}' has no condition to evaluate", rule.getName());
                RulePerformanceMetrics metrics = performanceMonitor.completeEvaluation(metricsBuilder, rule.getCondition());
                LoggingContext.clearContext();
                return RuleResult.error(rule.getName(), "Rule has no condition to evaluate", rule.getSeverity(), metrics);
            }
            
            // Parse and evaluate the SpEL expression
            Expression exp = parser.parseExpression(rule.getCondition());
            Boolean result = exp.getValue(context, Boolean.class);
            
            // Complete performance monitoring for successful evaluation
            RulePerformanceMetrics metrics = performanceMonitor.completeEvaluation(metricsBuilder, rule.getCondition());

            // Log the completion with performance metrics
            rulesLogger.info("Rule evaluation completed: {} -> {}", rule.getName(), result != null && result);
            LoggingContext.clearContext();
            
            // Return appropriate result
            if (result != null && result) {
                rulesLogger.info("Rule matched: {}", rule.getName());
                return RuleResult.match(rule.getName(), rule.getMessage(), rule.getSeverity(), metrics);
            } else {
                rulesLogger.debug("Rule did not match: {}", rule.getName());
                return RuleResult.noMatch();
            }
            
        } catch (Exception e) {
            return handleEvaluationError(rule, e, metricsBuilder);
        }
    }
    
    /**
     * Evaluate a single rule against the provided facts map.
     * Convenience method that creates the evaluation context.
     * 
     * @param rule The rule to evaluate
     * @param facts The facts to evaluate against
     * @return The rule evaluation result
     */
    public RuleResult evaluateRule(Rule rule, Map<String, Object> facts) {
        if (rule == null) {
            return RuleResult.noRules();
        }
        
        // Check for missing parameters
        Set<String> missingParameters = RuleParameterExtractor.validateParameters(rule, facts);
        if (!missingParameters.isEmpty()) {
            TestAwareLogger.warn(rulesLogger, "Missing parameters for rule '{}': {}", rule.getName(), missingParameters);
            return RuleResult.error(rule.getName(), "Missing parameters: " + missingParameters, rule.getSeverity());
        }
        
        // Create evaluation context
        StandardEvaluationContext context = createEvaluationContext(facts);
        
        return evaluateRule(rule, context);
    }
    
    /**
     * Handle evaluation errors with consistent error recovery logic.
     *
     * @param rule The rule that failed evaluation
     * @param exception The exception that occurred
     * @param metricsBuilder The performance metrics builder
     * @return The error result or recovered result
     */
    private RuleResult handleEvaluationError(Rule rule, Exception exception, RulePerformanceMetrics.Builder metricsBuilder) {
        // Log the error using the rules engine logger
        rulesLogger.ruleEvaluationError(rule.getName(), exception);
        
        // Complete performance monitoring for failed evaluation
        RulePerformanceMetrics metrics = performanceMonitor.completeEvaluation(metricsBuilder, rule.getCondition(), exception);
        
        // Create standardized error message
        String errorMessage = String.format(ERROR_MESSAGE_FORMAT, rule.getName(), exception.getMessage());
        String severity = rule.getSeverity() != null ? rule.getSeverity() : "ERROR";
        
        // Attempt error recovery based on severity (all non-CRITICAL errors)
        if (!"CRITICAL".equalsIgnoreCase(severity)) {
            rulesLogger.info("Attempting error recovery for rule '{}' with severity '{}'", rule.getName(), severity);
            // Use the existing error recovery service method signature
            ErrorRecoveryService.RecoveryResult recoveryResult = errorRecoveryService.attemptRecovery(rule.getName(), rule.getCondition(), null, exception);
            if (recoveryResult != null && recoveryResult.isSuccessful()) {
                LoggingContext.clearContext();
                return recoveryResult.getRuleResult();
            }
        }
        
        // Log error details at appropriate level based on severity
        if ("CRITICAL".equalsIgnoreCase(severity)) {
            rulesLogger.error("CRITICAL rule evaluation error for '{}': {}", rule.getName(), exception.getMessage());
        } else if ("WARNING".equalsIgnoreCase(severity)) {
            rulesLogger.info("Rule evaluation warning for '{}': {}", rule.getName(), exception.getMessage());
        } else {
            rulesLogger.info("Rule evaluation error for '{}': {}", rule.getName(), exception.getMessage());
        }

        // Always log full exception details at DEBUG level for troubleshooting
        rulesLogger.debug("Full exception details for rule '{}':", rule.getName(), exception);

        LoggingContext.clearContext();
        return RuleResult.error(rule.getName(), errorMessage, severity, metrics);
    }
    
    /**
     * Create a standard evaluation context from facts map.
     * This method replicates the context creation logic from RulesEngine
     * to ensure consistent SpEL evaluation behavior.
     *
     * @param facts The facts to include in the context
     * @return The evaluation context
     */
    private StandardEvaluationContext createEvaluationContext(Map<String, Object> facts) {
        StandardEvaluationContext context = new StandardEvaluationContext();

        // Add custom property accessor for Maps (enables #data.property syntax)
        context.addPropertyAccessor(new MapPropertyAccessor());

        if (facts != null) {
            // Set the facts map as the root object so properties can be accessed directly
            context.setRootObject(facts);

            // Also add facts as variables for backward compatibility (accessed with #variableName)
            for (Map.Entry<String, Object> entry : facts.entrySet()) {
                context.setVariable(entry.getKey(), entry.getValue());
            }
        }

        return context;
    }

    /**
     * Evaluate a list of rules against the provided context.
     * Returns the first rule that matches, or NO_MATCH if none match.
     *
     * @param rules The rules to evaluate
     * @param context The evaluation context
     * @return The result of the first matching rule, or NO_MATCH
     */
    public RuleResult evaluateRules(List<Rule> rules, EvaluationContext context) {
        if (rules == null || rules.isEmpty()) {
            rulesLogger.info("No rules provided for evaluation");
            return RuleResult.noRules();
        }

        rulesLogger.info("Evaluating {} rules", rules.size());

        for (Rule rule : rules) {
            RuleResult result = evaluateRule(rule, context);

            // Return first match or error
            if (result.isTriggered() || result.getResultType() == RuleResult.ResultType.ERROR) {
                return result;
            }
        }

        rulesLogger.info("No rules matched");
        return RuleResult.noMatch();
    }

    /**
     * Evaluate a list of rules against the provided facts map.
     * Convenience method that creates the evaluation context.
     *
     * @param rules The rules to evaluate
     * @param facts The facts to evaluate against
     * @return The result of the first matching rule, or NO_MATCH
     */
    public RuleResult evaluateRules(List<Rule> rules, Map<String, Object> facts) {
        if (rules == null || rules.isEmpty()) {
            return RuleResult.noRules();
        }

        // Create evaluation context
        StandardEvaluationContext context = createEvaluationContext(facts);

        return evaluateRules(rules, context);
    }
}
