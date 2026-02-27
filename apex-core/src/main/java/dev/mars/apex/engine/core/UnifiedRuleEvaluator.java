package dev.mars.apex.engine.core;

import dev.mars.apex.core.config.error.ErrorRecoveryConfig;
import dev.mars.apex.core.config.error.SeverityRecoveryPolicy;
import dev.mars.apex.core.constants.SeverityConstants;
import dev.mars.apex.engine.model.Rule;
import dev.mars.apex.engine.model.RuleResult;
import dev.mars.apex.core.service.monitoring.RulePerformanceMonitor;
import dev.mars.apex.core.service.monitoring.RulePerformanceMetrics;
import dev.mars.apex.core.service.error.ErrorRecoveryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.SpelEvaluationException;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.util.List;
import java.util.Map;

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
 * @author Mark A Ray-Smith
 * @since 2025-09-27
 * @version 1.0
 */
public class UnifiedRuleEvaluator {

    private static final Logger logger = LoggerFactory.getLogger(UnifiedRuleEvaluator.class);

    private final ExpressionParser parser;
    private final ErrorRecoveryService errorRecoveryService;
    private final RulePerformanceMonitor performanceMonitor;
    private final ErrorRecoveryConfig errorRecoveryConfig;
    private final ExpressionEvaluatorService evaluatorService;

    // Collaborators extracted from this class for single-responsibility
    private final MessageTemplateResolver messageTemplateResolver;
    private final FieldMappingProcessor fieldMappingProcessor;
    private final ErrorRecoveryHandler errorRecoveryHandler;
    
    /**
     * Create a new UnifiedRuleEvaluator with default components.
     */
    public UnifiedRuleEvaluator() {
        this.parser = new SpelExpressionParser();
        this.errorRecoveryService = new ErrorRecoveryService();
        this.performanceMonitor = new RulePerformanceMonitor();
        this.errorRecoveryConfig = new ErrorRecoveryConfig();
        this.evaluatorService = new ExpressionEvaluatorService(this.parser);
        this.messageTemplateResolver = new MessageTemplateResolver(this.parser);
        this.fieldMappingProcessor = new FieldMappingProcessor(this.parser);
        this.errorRecoveryHandler = new ErrorRecoveryHandler(this.errorRecoveryConfig, this.errorRecoveryService, this.performanceMonitor);
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
        this(parser, errorRecoveryService, performanceMonitor, new ErrorRecoveryConfig());
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
        this.evaluatorService = new ExpressionEvaluatorService(this.parser);
        this.messageTemplateResolver = new MessageTemplateResolver(this.parser);
        this.fieldMappingProcessor = new FieldMappingProcessor(this.parser);
        this.errorRecoveryHandler = new ErrorRecoveryHandler(this.errorRecoveryConfig, this.errorRecoveryService, this.performanceMonitor);
    }

    /**
     * Create a new UnifiedRuleEvaluator with custom components including evaluator service.
     *
     * @param evaluatorService The expression evaluator service
     * @param errorRecoveryService The error recovery service
     * @param performanceMonitor The performance monitor
     * @param errorRecoveryConfig The error recovery configuration
     */
    public UnifiedRuleEvaluator(ExpressionEvaluatorService evaluatorService,
                               ErrorRecoveryService errorRecoveryService,
                               RulePerformanceMonitor performanceMonitor,
                               ErrorRecoveryConfig errorRecoveryConfig) {
        this.evaluatorService = evaluatorService != null ? evaluatorService : new ExpressionEvaluatorService();
        this.parser = this.evaluatorService.getParser();
        this.errorRecoveryService = errorRecoveryService != null ? errorRecoveryService : new ErrorRecoveryService();
        this.performanceMonitor = performanceMonitor != null ? performanceMonitor : new RulePerformanceMonitor();
        this.errorRecoveryConfig = errorRecoveryConfig != null ? errorRecoveryConfig : new ErrorRecoveryConfig();
        this.messageTemplateResolver = new MessageTemplateResolver(this.parser);
        this.fieldMappingProcessor = new FieldMappingProcessor(this.parser);
        this.errorRecoveryHandler = new ErrorRecoveryHandler(this.errorRecoveryConfig, this.errorRecoveryService, this.performanceMonitor);
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
            logger.debug("No rule provided for evaluation");
            return RuleResult.noRules();
        }
        
        // Skip disabled rules - they should not be evaluated
        if (!dev.mars.apex.core.util.EnabledFilter.isEnabled(rule)) {
            logger.debug("Rule '{}' is disabled, skipping evaluation", rule.getName());
            return RuleResult.noMatch(rule.getName(), "Rule is disabled", SeverityConstants.INFO);
        }
        
        logger.debug("Starting rule evaluation: {}", rule.getName());
        logger.debug("Rule details - id: '{}', severity: '{}', condition: '{}'", 
                        rule.getId(), rule.getSeverity(), rule.getCondition());

        // Start performance monitoring
        RulePerformanceMetrics.Builder metricsBuilder = performanceMonitor.startEvaluation(rule.getName(), "evaluation");
        
        try {
            // Validate rule has required condition
            if (rule.getCondition() == null || rule.getCondition().trim().isEmpty()) {
                logger.warn("Rule '{}' has no condition to evaluate", rule.getName());
                RulePerformanceMetrics metrics = performanceMonitor.completeEvaluation(metricsBuilder, rule.getCondition());
                
                return RuleResult.errorWithCode(rule.getName(), "Rule has no condition to evaluate", rule.getSeverity(), "APEX-RULE-001", metrics);
            }
            
            // Parse and evaluate the SpEL expression
            logger.debug("Parsing SpEL expression for rule '{}': {}", rule.getName(), rule.getCondition());
            Expression exp = parser.parseExpression(rule.getCondition());
            Boolean result;
            
            try {
                result = exp.getValue(context, Boolean.class);
                logger.debug("SpEL evaluation for rule '{}' returned: {}", rule.getName(), result);
            } catch (SpelEvaluationException e) {
                logger.debug("SpEL evaluation exception for rule '{}': {}", rule.getName(), e.getMessage());
                // Delegate to error recovery handler for consistent error handling
                // This ensures SpEL evaluation errors go through the same recovery logic
                // as other exceptions, respecting severity-based recovery policies
                return errorRecoveryHandler.handleEvaluationError(rule, e, metricsBuilder);
            }

            // Store result in context if result-field is configured
            if (rule.getResultField() != null && !rule.getResultField().trim().isEmpty()) {
                boolean booleanResult = (result != null && result);
                context.setVariable(rule.getResultField(), booleanResult);
                logger.debug("Stored rule result in context: {} = {}", rule.getResultField(), booleanResult);
            }

            // Complete performance monitoring for successful evaluation
            RulePerformanceMetrics metrics = performanceMonitor.completeEvaluation(metricsBuilder, rule.getCondition());

            // Log the completion with performance metrics
            logger.debug("Rule evaluation completed: {} -> {}", rule.getName(), result != null && result);
            

            // Evaluate codes and apply field mappings
            Map<String, Object> enrichedData = new java.util.HashMap<>();
            String evaluatedSuccessCode = null;
            String evaluatedErrorCode = null;

            if (result != null && result) {
                // Rule matched - evaluate success code
                evaluatedSuccessCode = fieldMappingProcessor.evaluateCode(rule.getSuccessCode(), context);
                logger.debug("Rule matched: {}", rule.getName());

                // Apply field mappings if configured
                if (rule.getMapToField() != null) {
                    fieldMappingProcessor.applyFieldMappings(rule.getMapToField(), (StandardEvaluationContext) context, enrichedData, evaluatedSuccessCode, null);
                }

                // When condition is TRUE, the rule matched successfully - severity is irrelevant
                // Always return MATCH with success=true
                String resolvedMessage = messageTemplateResolver.resolve(rule.getMessage(), context);
                RuleResult matchResult = RuleResult.builder()
                        .ruleId(rule.getId())
                        .ruleName(rule.getName())
                        .message(resolvedMessage)
                        .severity(rule.getSeverity())
                        .triggered(true)
                        .resultType(RuleResult.ResultType.MATCH)
                        .performanceMetrics(metrics)
                        .enrichedData(enrichedData)
                        .success(true)
                        .successCode(evaluatedSuccessCode)
                        .mapToField(rule.getMapToField())
                        .build();
                return matchResult;
            } else {
                // Rule did not match - evaluate error code
                evaluatedErrorCode = fieldMappingProcessor.evaluateCode(rule.getErrorCode(), context);
                logger.debug("Rule did not match: {}", rule.getName());

                // Apply field mappings if configured
                if (rule.getMapToField() != null) {
                    fieldMappingProcessor.applyFieldMappings(rule.getMapToField(), (StandardEvaluationContext) context, enrichedData, null, evaluatedErrorCode);
                }

                // Determine ResultType based on severity and error recovery configuration
                // When condition is FALSE, severity determines if processing should fail-fast
                RuleResult.ResultType resultType = RuleResult.ResultType.NO_MATCH;
                boolean shouldFail = false;
                
                String severity = rule.getSeverity() != null ? rule.getSeverity() : SeverityConstants.INFO;
                if (SeverityConstants.ERROR.equalsIgnoreCase(severity) || 
                    SeverityConstants.CRITICAL.equalsIgnoreCase(severity)) {
                    // Check if error recovery is disabled for this severity (default for ERROR/CRITICAL)
                    if (!errorRecoveryConfig.isRecoveryEnabledForSeverity(severity)) {
                        // Per design: ERROR/CRITICAL with recovery disabled should use FAIL_FAST
                        resultType = RuleResult.ResultType.ERROR;
                        shouldFail = true;
                        logger.warn("Rule '{}' did not match with {} severity - evaluation will fail (recovery disabled)",
                                       rule.getName(), severity);
                    }
                }

                // Return result with codes and mappings
                String noMatchMessageTemplate = rule.getNoMatchMessage() != null ? rule.getNoMatchMessage() : rule.getMessage();
                String resolvedNoMatchMessage = messageTemplateResolver.resolve(noMatchMessageTemplate, context);
                RuleResult noMatchResult = RuleResult.builder()
                        .ruleId(rule.getId())
                        .ruleName(rule.getName())
                        .message(resolvedNoMatchMessage)
                        .severity(rule.getSeverity())
                        .triggered(false)
                        .resultType(resultType)
                        .performanceMetrics(metrics)
                        .enrichedData(enrichedData)
                        .success(!shouldFail)
                        .errorCode(evaluatedErrorCode)
                        .mapToField(rule.getMapToField())
                        .build();
                return noMatchResult;
            }
            
        } catch (Exception e) {
            return errorRecoveryHandler.handleEvaluationError(rule, e, metricsBuilder);
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
        logger.debug("evaluateRule(Rule, Map) called for rule: {}", rule != null ? rule.getName() : "null");
        logger.debug("evaluateRule(Rule, Map) - facts keys: {}, facts size: {}", 
                        facts != null ? facts.keySet() : "null", facts != null ? facts.size() : 0);

        if (rule == null) {
            return RuleResult.noRules();
        }

        // NOTE: Parameter validation removed after #data refactoring.
        // With direct variable access (#variable instead of #data['variable']), 
        // SpEL naturally handles missing variables by throwing PropertyNotFoundException,
        // which is properly caught and handled by the error recovery system.
        // This allows the severity-based error recovery to work correctly.
        
        // Create evaluation context
        logger.debug("Creating evaluation context for rule '{}' with {} fact entries", rule.getName(), facts.size());
        StandardEvaluationContext context = createEvaluationContext(facts);

        RuleResult result = evaluateRule(rule, context);
        logger.debug("Rule '{}' evaluation result - triggered: {}, resultType: {}", 
                        rule.getName(), result.isTriggered(), result.getResultType());

        // Store result in facts and enrichedData if result-field is configured
        if (rule.getResultField() != null && !rule.getResultField().trim().isEmpty()) {
            // Store in facts map for subsequent rules to access (flat key)
            facts.put(rule.getResultField(), result.isTriggered());
            logger.debug("Stored rule result in facts: {} = {}", rule.getResultField(), result.isTriggered());

            // Also add to enrichedData so it's returned to the caller
            Map<String, Object> enrichedData = new java.util.HashMap<>(result.getEnrichedData());

            // Support nested field notation (e.g., "validation.isHighValue" creates nested structure)
            setNestedValue(enrichedData, rule.getResultField(), result.isTriggered());

            // Create new RuleResult with updated enrichedData using toBuilder()
            result = result.toBuilder()
                    .enrichedData(enrichedData)
                    .build();
            logger.debug("Added result-field to enrichedData: {} = {}", rule.getResultField(), result.isTriggered());
        }

        return result;
    }

    /**
     * Sets a value in a map using dot notation to create nested structures.
     * For example, "validation.isHighValue" will create a nested map structure:
     * { "validation": { "isHighValue": value } }
     *
     * @param map The map to update
     * @param path The dot-separated path (e.g., "validation.isHighValue")
     * @param value The value to set
     */
    @SuppressWarnings("unchecked")
    private void setNestedValue(Map<String, Object> map, String path, Object value) {
        if (path == null || path.trim().isEmpty()) {
            return;
        }

        String[] parts = path.split("\\.");

        // If no dots, just set the value directly
        if (parts.length == 1) {
            map.put(path, value);
            return;
        }

        // Navigate/create nested structure
        Map<String, Object> current = map;
        for (int i = 0; i < parts.length - 1; i++) {
            String part = parts[i];
            Object existing = current.get(part);

            if (existing instanceof Map) {
                current = (Map<String, Object>) existing;
            } else {
                // Create new nested map
                Map<String, Object> nested = new java.util.HashMap<>();
                current.put(part, nested);
                current = nested;
            }
        }

        // Set the final value
        current.put(parts[parts.length - 1], value);
    }

    /**
     * Evaluate a router expression that returns a non-boolean value (e.g., a route key).
     *
     * <p>This method is the canonical path for evaluating router-rule expressions in
     * result-based-routing chains. Unlike {@link #evaluateRule(Rule, Map)}, which evaluates
     * conditions as {@code Boolean.class}, this method evaluates as {@code Object.class}
     * to support expressions that return strings, numbers, or other route key values.</p>
     *
     * <p>Provides the same cross-cutting concerns as standard rule evaluation:
     * <ul>
     *   <li>Performance monitoring (timing, metrics)</li>
     *   <li>Error recovery (severity-based policies)</li>
     *   <li>Consistent logging</li>
     * </ul>
     *
     * @param ruleId The identifier for the router rule (used in logging and metrics)
     * @param expression The SpEL expression to evaluate
     * @param data The facts/data map for evaluation context
     * @return The expression result as an Object, or null if evaluation fails
     * @throws dev.mars.apex.core.exception.ApexEvaluationException if a CRITICAL error occurs
     *         and recovery is not enabled
     * @since 2026-02-26
     */
    public Object evaluateRouterExpression(String ruleId, String expression, Map<String, Object> data) {
        logger.debug("Evaluating router expression for '{}': {}", ruleId, expression);

        // Start performance monitoring
        RulePerformanceMetrics.Builder metricsBuilder = performanceMonitor.startEvaluation(ruleId, "router-evaluation");

        try {
            StandardEvaluationContext context = createEvaluationContext(data);
            Expression exp = parser.parseExpression(expression);
            Object result = exp.getValue(context, Object.class);

            // Complete performance monitoring
            performanceMonitor.completeEvaluation(metricsBuilder, expression);
            logger.debug("Router expression for '{}' evaluated to: {}", ruleId, result);
            return result;
        } catch (Exception e) {
            logger.error("Error evaluating router expression for '{}': {}", ruleId, e.getMessage());
            logger.debug("Full exception details for router evaluation:", e);

            // Apply error recovery using a transient Rule for consistent severity handling
            String severity = SeverityConstants.ERROR;
            if (errorRecoveryConfig.isRecoveryEnabledForSeverity(severity)) {
                SeverityRecoveryPolicy policy = errorRecoveryConfig.getSeverityPolicy(severity);
                String strategyName = policy != null ? policy.getStrategy() : "CONTINUE_WITH_DEFAULT";

                if (errorRecoveryConfig.isLogRecoveryAttempts()) {
                    logger.info("Attempting error recovery for router '{}' with strategy '{}'", ruleId, strategyName);
                }

                ErrorRecoveryService.ErrorRecoveryStrategy strategy = "FAIL_FAST".equals(strategyName) ?
                    ErrorRecoveryService.ErrorRecoveryStrategy.FAIL_FAST :
                    ErrorRecoveryService.ErrorRecoveryStrategy.CONTINUE_WITH_DEFAULT;
                ErrorRecoveryService.RecoveryResult recoveryResult =
                    errorRecoveryService.attemptRecovery(ruleId, expression, null, e, strategy);

                if (recoveryResult != null && recoveryResult.isSuccessful()) {
                    logger.info("Recovery successful for router '{}', returning null route key", ruleId);
                    performanceMonitor.completeEvaluation(metricsBuilder, expression, e);
                    return null;
                }
            }

            performanceMonitor.completeEvaluation(metricsBuilder, expression, e);
            return null;
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
     * This method delegates to ExpressionEvaluatorService to ensure consistent SpEL evaluation behavior.
     *
     * @param facts The facts to include in the context
     * @return The evaluation context
     */
    private StandardEvaluationContext createEvaluationContext(Map<String, Object> facts) {
        return evaluatorService.createEvaluationContext(facts);
    }

    /**
     * Evaluate a list of rules against the provided facts map.
     * Evaluates ALL rules to ensure result-field values are stored for all rules,
     * then returns the first significant result (error or match, whichever came first).
     *
     * <p><strong>Termination semantics:</strong> All rules are always evaluated (no short-circuit).
     * This ensures that result-field values from every rule are available in enrichedData,
     * even if an earlier rule matched or errored.</p>
     *
     * @param rules The rules to evaluate
     * @param facts The facts to evaluate against
     * @return The first significant result with accumulated enrichedData from all rules,
     *         or NO_MATCH if no rules matched
     */
    public RuleResult evaluateRules(List<Rule> rules, Map<String, Object> facts) {
        logger.debug("evaluateRules(List<Rule>, Map) called with {} rules", rules != null ? rules.size() : 0);

        if (rules == null || rules.isEmpty()) {
            return RuleResult.noRules();
        }

        logger.debug("Evaluating {} rules", rules.size());

        // Accumulate enrichedData from all rules
        Map<String, Object> accumulatedEnrichedData = new java.util.HashMap<>();
        RuleResult firstSignificantResult = null; // First error OR first match, whichever comes first

        // Evaluate ALL rules to ensure result-field values are stored
        for (Rule rule : rules) {
            // Evaluate each rule individually to ensure result-field storage
            RuleResult result = evaluateRule(rule, facts);

            // Accumulate enrichedData from this rule (includes result-field values)
            if (result.getEnrichedData() != null) {
                accumulatedEnrichedData.putAll(result.getEnrichedData());
            }

            // Track first significant result (error or match), preserving order
            if (firstSignificantResult == null) {
                if (result.isError() || result.isTriggered()) {
                    firstSignificantResult = result;
                }
            }
        }

        // Return first significant result if any (error or match, whichever came first)
        if (firstSignificantResult != null) {
            return firstSignificantResult.toBuilder()
                    .enrichedData(accumulatedEnrichedData)
                    .build();
        }

        logger.debug("No rules matched");
        // Return noMatch with accumulated enrichedData from all evaluated rules
        return RuleResult.builder()
                .ruleName("no-match")
                .message("No matching rules found")
                .severity(SeverityConstants.INFO)
                .triggered(false)
                .resultType(RuleResult.ResultType.NO_MATCH)
                .enrichedData(accumulatedEnrichedData)
                .success(true)
                .build();
    }

    /**

     * @param message The message template to resolve
     * @param context The SpEL evaluation context containing variable bindings
     * @return The message with all resolvable placeholders replaced by their values
     */
    String resolveMessageTemplate(String message, EvaluationContext context) {
        return messageTemplateResolver.resolve(message, context);
    }
}
