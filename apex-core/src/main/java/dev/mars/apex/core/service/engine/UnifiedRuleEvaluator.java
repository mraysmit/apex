package dev.mars.apex.core.service.engine;

import dev.mars.apex.core.config.error.ErrorRecoveryConfig;
import dev.mars.apex.core.config.error.SeverityRecoveryPolicy;
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

import java.time.Duration;
import java.time.Instant;
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
    private final ErrorRecoveryConfig errorRecoveryConfig;
    
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
        this.errorRecoveryConfig = new ErrorRecoveryConfig();
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
        this.errorRecoveryConfig = new ErrorRecoveryConfig();
    }

    /**
     * Create a new UnifiedRuleEvaluator with custom components including error recovery config.
     *
     * @param parser The SpEL expression parser
     * @param errorRecoveryService The error recovery service
     * @param performanceMonitor The performance monitor
     * @param errorRecoveryConfig The error recovery configuration
     */
    public UnifiedRuleEvaluator(ExpressionParser parser,
                               ErrorRecoveryService errorRecoveryService,
                               RulePerformanceMonitor performanceMonitor,
                               ErrorRecoveryConfig errorRecoveryConfig) {
        this.parser = parser != null ? parser : new SpelExpressionParser();
        this.errorRecoveryService = errorRecoveryService != null ? errorRecoveryService : new ErrorRecoveryService();
        this.performanceMonitor = performanceMonitor != null ? performanceMonitor : new RulePerformanceMonitor();
        this.errorRecoveryConfig = errorRecoveryConfig != null ? errorRecoveryConfig : new ErrorRecoveryConfig();


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

        // Phase 3B: Initialize recovery tracking variables
        boolean recoveryAttempted = false;
        boolean recoverySuccessful = false;
        String recoveryStrategy = null;
        String recoveryReason = exception.getClass().getSimpleName();
        Instant recoveryStartTime = null;
        Duration recoveryTime = null;

        // Create standardized error message
        String errorMessage = String.format(ERROR_MESSAGE_FORMAT, rule.getName(), exception.getMessage());
        String severity = rule.getSeverity() != null ? rule.getSeverity() : "ERROR";

        // Attempt error recovery based on configurable severity policies
        if (errorRecoveryConfig.isRecoveryEnabledForSeverity(severity)) {
            // Phase 3B: Start recovery timing
            recoveryAttempted = true;
            recoveryStartTime = Instant.now();

            SeverityRecoveryPolicy policy = errorRecoveryConfig.getSeverityPolicy(severity);

            String actualStrategy = policy != null ? policy.getStrategy() : "default";

            if (errorRecoveryConfig.isLogRecoveryAttempts()) {
                rulesLogger.info("Attempting error recovery for rule '{}' with severity '{}' using strategy '{}'",
                    rule.getName(), severity, actualStrategy);
            }

            // Phase 3A Enhancement: Check if rule has a specific default-value
            if (rule.getDefaultValue() != null) {
                recoveryStrategy = "RULE_DEFAULT_VALUE";
                if (errorRecoveryConfig.isLogRecoveryAttempts()) {
                    rulesLogger.info("Using rule-specific default value for recovery: rule='{}', defaultValue='{}'",
                        rule.getName(), rule.getDefaultValue());
                }
                recoverySuccessful = true;
                // Phase 3B: Calculate recovery time
                if (recoveryStartTime != null) {
                    recoveryTime = Duration.between(recoveryStartTime, Instant.now());
                }

                // Complete performance monitoring with recovery metrics
                RulePerformanceMetrics metrics = buildMetricsWithRecovery(metricsBuilder, rule, exception,
                    recoveryAttempted, recoverySuccessful, recoveryStrategy, recoveryReason, recoveryTime);

                LoggingContext.clearContext();
                return RuleResult.match(rule.getName(), String.valueOf(rule.getDefaultValue()), severity, metrics);
            }

            // Use the error recovery service with the determined strategy
            ErrorRecoveryService.ErrorRecoveryStrategy strategy = "FAIL_FAST".equals(actualStrategy) ?
                ErrorRecoveryService.ErrorRecoveryStrategy.FAIL_FAST :
                ErrorRecoveryService.ErrorRecoveryStrategy.CONTINUE_WITH_DEFAULT;
            ErrorRecoveryService.RecoveryResult recoveryResult = errorRecoveryService.attemptRecovery(rule.getName(), rule.getCondition(), null, exception, strategy);
            if (recoveryResult != null && recoveryResult.isSuccessful()) {
                recoverySuccessful = true;
                recoveryStrategy = actualStrategy;
                // Phase 3B: Calculate recovery time
                if (recoveryStartTime != null) {
                    recoveryTime = Duration.between(recoveryStartTime, Instant.now());
                }

                // Complete performance monitoring with recovery metrics
                RulePerformanceMetrics metrics = buildMetricsWithRecovery(metricsBuilder, rule, exception,
                    recoveryAttempted, recoverySuccessful, recoveryStrategy, recoveryReason, recoveryTime);

                LoggingContext.clearContext();
                return recoveryResult.getRuleResult();
            } else {
                // Recovery failed
                recoverySuccessful = false;
                recoveryStrategy = actualStrategy;
                // Phase 3B: Calculate recovery time even for failed recovery
                if (recoveryStartTime != null) {
                    recoveryTime = Duration.between(recoveryStartTime, Instant.now());
                }
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

        // Complete performance monitoring with recovery metrics (even for failed recovery)
        RulePerformanceMetrics finalMetrics = buildMetricsWithRecovery(metricsBuilder, rule, exception,
            recoveryAttempted, recoverySuccessful, recoveryStrategy, recoveryReason, recoveryTime);

        LoggingContext.clearContext();
        return RuleResult.error(rule.getName(), errorMessage, severity, finalMetrics);
    }

    /**
     * Phase 3B: Build performance metrics with recovery information.
     * Only includes recovery metrics if metrics are enabled in configuration.
     */
    private RulePerformanceMetrics buildMetricsWithRecovery(RulePerformanceMetrics.Builder metricsBuilder,
                                                           Rule rule, Exception exception,
                                                           boolean recoveryAttempted, boolean recoverySuccessful,
                                                           String recoveryStrategy, String recoveryReason,
                                                           Duration recoveryTime) {
        // Complete the basic evaluation metrics first
        RulePerformanceMetrics baseMetrics = performanceMonitor.completeEvaluation(metricsBuilder, rule.getCondition(), exception);

        // Phase 3B: Only add recovery metrics if metrics are enabled
        if (errorRecoveryConfig.isMetricsEnabled()) {
            // Create a new builder from the base metrics and add recovery information
            return new RulePerformanceMetrics.Builder(baseMetrics.getRuleName())
                .startTime(baseMetrics.getStartTime())
                .endTime(baseMetrics.getEndTime())
                .evaluationTime(baseMetrics.getEvaluationTime())
                .memoryUsed(baseMetrics.getMemoryUsedBytes())
                .memoryBefore(baseMetrics.getMemoryBeforeBytes())
                .memoryAfter(baseMetrics.getMemoryAfterBytes())
                .expressionComplexity(baseMetrics.getExpressionComplexity())
                .cacheHit(baseMetrics.isCacheHit())
                .evaluationPhase(baseMetrics.getEvaluationPhase())
                .evaluationException(baseMetrics.getEvaluationException())
                .recoveryAttempted(recoveryAttempted)
                .recoverySuccessful(recoverySuccessful)
                .recoveryStrategy(recoveryStrategy)
                .recoveryReason(recoveryReason)
                .recoveryTime(recoveryTime)
                .build();
        } else {
            // Return base metrics without recovery information
            return baseMetrics;
        }
    }

    /**
     * Get the current error recovery configuration.
     *
     * @return The error recovery configuration
     */
    public ErrorRecoveryConfig getErrorRecoveryConfig() {
        return errorRecoveryConfig;
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
