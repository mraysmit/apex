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

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
    
    /**
     * Standard error message format for consistency across all evaluation paths.
     */
    private static final String ERROR_MESSAGE_FORMAT = "Rule evaluation failed: %s - %s";
    
    /**
     * Pattern to extract variable names from SpEL expressions.
     * Matches #variableName patterns.
     */
    private static final Pattern SPEL_VARIABLE_PATTERN = Pattern.compile("#(\\w+)");
    
    /**
     * Pattern to match Handlebars-style placeholders in rule messages.
     * Matches {{#expression}} format used in YAML rule message templates.
     * Also supports #{expression} format used by TemplateProcessorService.
     */
    private static final Pattern HANDLEBARS_PLACEHOLDER_PATTERN = Pattern.compile("\\{\\{(#[^}]+)\\}\\}");
    private static final Pattern HASH_PLACEHOLDER_PATTERN = Pattern.compile("#\\{([^}]+)\\}");
    
    /**
     * Create a new UnifiedRuleEvaluator with default components.
     */
    public UnifiedRuleEvaluator() {
        this.parser = new SpelExpressionParser();
        this.errorRecoveryService = new ErrorRecoveryService();
        this.performanceMonitor = new RulePerformanceMonitor();
        this.errorRecoveryConfig = new ErrorRecoveryConfig();
        this.evaluatorService = new ExpressionEvaluatorService(this.parser);
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
            logger.info("No rule provided for evaluation");
            return RuleResult.noRules();
        }
        
        // Skip disabled rules - they should not be evaluated
        if (!dev.mars.apex.core.util.EnabledFilter.isEnabled(rule)) {
            logger.info("Rule '{}' is disabled, skipping evaluation", rule.getName());
            return RuleResult.noMatch(rule.getName(), "Rule is disabled", SeverityConstants.INFO);
        }
        
        logger.info("Starting rule evaluation: {}", rule.getName());
        logger.debug("Rule details - id: '{}', severity: '{}', condition: '{}'", 
                        rule.getId(), rule.getSeverity(), rule.getCondition());

        // Start performance monitoring
        RulePerformanceMetrics.Builder metricsBuilder = performanceMonitor.startEvaluation(rule.getName(), "evaluation");
        
        try {
            // Validate rule has required condition
            if (rule.getCondition() == null || rule.getCondition().trim().isEmpty()) {
                logger.warn("Rule '{}' has no condition to evaluate", rule.getName());
                RulePerformanceMetrics metrics = performanceMonitor.completeEvaluation(metricsBuilder, rule.getCondition());
                
                return RuleResult.error(rule.getName(), "Rule has no condition to evaluate", rule.getSeverity(), metrics);
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
                return handleEvaluationError(rule, e, metricsBuilder);
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
            logger.info("Rule evaluation completed: {} -> {}", rule.getName(), result != null && result);
            

            // Evaluate codes and apply field mappings
            Map<String, Object> enrichedData = new java.util.HashMap<>();
            String evaluatedSuccessCode = null;
            String evaluatedErrorCode = null;

            if (result != null && result) {
                // Rule matched - evaluate success code
                evaluatedSuccessCode = evaluateCode(rule.getSuccessCode(), context);
                logger.info("Rule matched: {}", rule.getName());

                // Apply field mappings if configured
                if (rule.getMapToField() != null) {
                    applyFieldMappings(rule.getMapToField(), context, enrichedData, evaluatedSuccessCode, null);
                }

                // When condition is TRUE, the rule matched successfully - severity is irrelevant
                // Always return MATCH with success=true
                String resolvedMessage = resolveMessageTemplate(rule.getMessage(), context);
                RuleResult matchResult = new RuleResult(rule.getId(), rule.getName(), resolvedMessage, rule.getSeverity(),
                                                       true, RuleResult.ResultType.MATCH, metrics, enrichedData,
                                                       new java.util.ArrayList<>(), true, evaluatedSuccessCode, null, rule.getMapToField());
                return matchResult;
            } else {
                // Rule did not match - evaluate error code
                evaluatedErrorCode = evaluateCode(rule.getErrorCode(), context);
                logger.debug("Rule did not match: {}", rule.getName());

                // Apply field mappings if configured
                if (rule.getMapToField() != null) {
                    applyFieldMappings(rule.getMapToField(), context, enrichedData, null, evaluatedErrorCode);
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
                String resolvedNoMatchMessage = resolveMessageTemplate(noMatchMessageTemplate, context);
                RuleResult noMatchResult = new RuleResult(rule.getId(), rule.getName(), resolvedNoMatchMessage, rule.getSeverity(),
                                                         false, resultType, metrics, enrichedData,
                                                         new java.util.ArrayList<>(), !shouldFail, null, evaluatedErrorCode, rule.getMapToField());
                return noMatchResult;
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
        logger.info("Phase 5: evaluateRule(Rule, Map) called for rule: {}", rule != null ? rule.getName() : "null");
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
            logger.info("Phase 5: Stored rule result in facts: {} = {}", rule.getResultField(), result.isTriggered());

            // Also add to enrichedData so it's returned to the caller
            Map<String, Object> enrichedData = new java.util.HashMap<>(result.getEnrichedData());

            // Support nested field notation (e.g., "validation.isHighValue" creates nested structure)
            setNestedValue(enrichedData, rule.getResultField(), result.isTriggered());

            // Create new RuleResult with updated enrichedData
            result = new RuleResult(result.getRuleName(), result.getMessage(), result.getSeverity(),
                                   result.isTriggered(), result.getResultType(), result.getPerformanceMetrics(),
                                   enrichedData, result.getFailureMessages(), result.isSuccess(),
                                   result.getSuccessCode(), result.getErrorCode(), result.getMapToField());
            logger.info("Phase 5: Added result-field to enrichedData: {} = {}", rule.getResultField(), result.isTriggered());
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
     * Handle evaluation errors with consistent error recovery logic.
     *
     * @param rule The rule that failed evaluation
     * @param exception The exception that occurred
     * @param metricsBuilder The performance metrics builder
     * @return The error result or recovered result
     */
    private RuleResult handleEvaluationError(Rule rule, Exception exception, RulePerformanceMetrics.Builder metricsBuilder) {
        // Initialize recovery tracking variables
        boolean recoveryAttempted = false;
        boolean recoverySuccessful = false;
        String recoveryStrategy = null;
        String recoveryReason = exception.getClass().getSimpleName();
        Instant recoveryStartTime = null;
        Duration recoveryTime = null;

        // Create enhanced error message with undefined variable detection
        String errorMessage = createEnhancedErrorMessage(rule, exception);
        String severity = rule.getSeverity() != null ? rule.getSeverity() : SeverityConstants.ERROR;
        
        // Log the enhanced error message
        if (logger.isInfoEnabled()) {
            logger.info("Rule evaluation issue for '{}': {}", 
                rule.getName(), errorMessage);
        }

        // Attempt error recovery based on configurable severity policies
        if (errorRecoveryConfig.isRecoveryEnabledForSeverity(severity)) {
            // Start recovery timing
            recoveryAttempted = true;
            recoveryStartTime = Instant.now();

            SeverityRecoveryPolicy policy = errorRecoveryConfig.getSeverityPolicy(severity);

            String actualStrategy = policy != null ? policy.getStrategy() : "default";

            if (errorRecoveryConfig.isLogRecoveryAttempts()) {
                logger.info("Attempting error recovery for rule '{}' with severity '{}' using strategy '{}'",
                    rule.getName(), severity, actualStrategy);
            }

            // Phase 3A Enhancement: Check if rule has a specific default-value
            if (rule.getDefaultValue() != null) {
                recoveryStrategy = "RULE_DEFAULT_VALUE";
                if (errorRecoveryConfig.isLogRecoveryAttempts()) {
                    logger.info("Using rule-specific default value for recovery: rule='{}', defaultValue='{}'",
                        rule.getName(), rule.getDefaultValue());
                }
                recoverySuccessful = true;
                // Calculate recovery time
                if (recoveryStartTime != null) {
                    recoveryTime = Duration.between(recoveryStartTime, Instant.now());
                }

                // Complete performance monitoring with recovery metrics
                RulePerformanceMetrics metrics = buildMetricsWithRecovery(metricsBuilder, rule, exception,
                    recoveryAttempted, recoverySuccessful, recoveryStrategy, recoveryReason, recoveryTime);

                
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
                // Calculate recovery time
                if (recoveryStartTime != null) {
                    recoveryTime = Duration.between(recoveryStartTime, Instant.now());
                }

                // Complete performance monitoring with recovery metrics
                RulePerformanceMetrics metrics = buildMetricsWithRecovery(metricsBuilder, rule, exception,
                    recoveryAttempted, recoverySuccessful, recoveryStrategy, recoveryReason, recoveryTime);

                // Preserve original rule severity in recovery result
                RuleResult originalResult = recoveryResult.getRuleResult();
                RuleResult resultWithCorrectSeverity = new RuleResult(
                    originalResult.getRuleName(),
                    originalResult.getMessage(),
                    severity,  // Preserve original rule severity, not recovery default
                    false,     // Not triggered
                    originalResult.getResultType(),
                    metrics
                );
                
                
                return resultWithCorrectSeverity;
            } else {
                // Recovery failed
                recoverySuccessful = false;
                recoveryStrategy = actualStrategy;
                // Calculate recovery time even for failed recovery
                if (recoveryStartTime != null) {
                    recoveryTime = Duration.between(recoveryStartTime, Instant.now());
                }
            }
        }
        
        // Log error details at appropriate level based on severity, using enhanced message
        if (SeverityConstants.CRITICAL.equalsIgnoreCase(severity)) {
            logger.error("CRITICAL rule evaluation error for '{}': {}", rule.getName(), errorMessage);
        } else if (SeverityConstants.WARNING.equalsIgnoreCase(severity)) {
            logger.info("Rule evaluation warning for '{}': {}", rule.getName(), errorMessage);
        } else {
            logger.info("Rule evaluation error for '{}': {}", rule.getName(), errorMessage);
        }

        // Always log full exception details at DEBUG level for troubleshooting
        logger.debug("Full exception details for rule '{}':", rule.getName(), exception);

        // Complete performance monitoring with recovery metrics (even for failed recovery)
        RulePerformanceMetrics finalMetrics = buildMetricsWithRecovery(metricsBuilder, rule, exception,
            recoveryAttempted, recoverySuccessful, recoveryStrategy, recoveryReason, recoveryTime);

        
        return RuleResult.error(rule.getName(), errorMessage, severity, finalMetrics);
    }

    /**
     * Build performance metrics with recovery information.
     * Only includes recovery metrics if metrics are enabled in configuration.
     */
    private RulePerformanceMetrics buildMetricsWithRecovery(RulePerformanceMetrics.Builder metricsBuilder,
                                                           Rule rule, Exception exception,
                                                           boolean recoveryAttempted, boolean recoverySuccessful,
                                                           String recoveryStrategy, String recoveryReason,
                                                           Duration recoveryTime) {
        // Complete the basic evaluation metrics first
        RulePerformanceMetrics baseMetrics = performanceMonitor.completeEvaluation(metricsBuilder, rule.getCondition(), exception);

        // Only add recovery metrics if metrics are enabled
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
     * This method delegates to ExpressionEvaluatorService to ensure consistent SpEL evaluation behavior.
     *
     * @param facts The facts to include in the context
     * @return The evaluation context
     */
    private StandardEvaluationContext createEvaluationContext(Map<String, Object> facts) {
        return evaluatorService.createEvaluationContext(facts);
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
            logger.info("No rules provided for evaluation");
            return RuleResult.noRules();
        }

        logger.info("Evaluating {} rules", rules.size());

        // Accumulate enrichedData from all rules (even non-matching ones)
        Map<String, Object> accumulatedEnrichedData = new java.util.HashMap<>();

        for (Rule rule : rules) {
            RuleResult result = evaluateRule(rule, context);

            // Accumulate enrichedData from this rule (field mappings)
            if (result.getEnrichedData() != null) {
                accumulatedEnrichedData.putAll(result.getEnrichedData());
            }

            // Return first match or error (but with accumulated enrichedData)
            if (result.isTriggered() || result.getResultType() == RuleResult.ResultType.ERROR) {
                // If this result doesn't have all accumulated data, merge it
                if (accumulatedEnrichedData.size() > (result.getEnrichedData() != null ? result.getEnrichedData().size() : 0)) {
                    Map<String, Object> mergedData = new java.util.HashMap<>(accumulatedEnrichedData);
                    if (result.getEnrichedData() != null) {
                        mergedData.putAll(result.getEnrichedData());
                    }
                    // Create new result with merged enrichedData
                    return new RuleResult(result.getRuleName(), result.getMessage(), result.getSeverity(),
                                         result.isTriggered(), result.getResultType(), result.getPerformanceMetrics(),
                                         mergedData, result.getFailureMessages(), result.isSuccess(),
                                         result.getSuccessCode(), result.getErrorCode(), result.getMapToField());
                }
                return result;
            }
        }

        logger.info("No rules matched");
        // Return noMatch with accumulated enrichedData from all evaluated rules
        return new RuleResult("no-match", "No matching rules found", SeverityConstants.INFO, false, RuleResult.ResultType.NO_MATCH,
                             null, accumulatedEnrichedData, new java.util.ArrayList<>(), true, null, null, null);
    }

    /**
     * Evaluate a list of rules against the provided facts map.
     * Convenience method that creates the evaluation context.
     * Evaluates ALL rules to ensure result-field values are stored for all rules.
     *
     * @param rules The rules to evaluate
     * @param facts The facts to evaluate against
     * @return The result with accumulated enrichedData from all rules
     */
    public RuleResult evaluateRules(List<Rule> rules, Map<String, Object> facts) {
        logger.info("Phase 5: evaluateRules(List<Rule>, Map) called with {} rules", rules != null ? rules.size() : 0);

        if (rules == null || rules.isEmpty()) {
            return RuleResult.noRules();
        }

        logger.info("Evaluating {} rules", rules.size());

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
                if (result.getResultType() == RuleResult.ResultType.ERROR || result.isTriggered()) {
                    firstSignificantResult = result;
                }
            }
        }

        // Return first significant result if any (error or match, whichever came first)
        if (firstSignificantResult != null) {
            return new RuleResult(firstSignificantResult.getRuleName(), firstSignificantResult.getMessage(), firstSignificantResult.getSeverity(),
                                 firstSignificantResult.isTriggered(), firstSignificantResult.getResultType(), firstSignificantResult.getPerformanceMetrics(),
                                 accumulatedEnrichedData, firstSignificantResult.getFailureMessages(), firstSignificantResult.isSuccess(),
                                 firstSignificantResult.getSuccessCode(), firstSignificantResult.getErrorCode(), firstSignificantResult.getMapToField());
        }

        logger.info("No rules matched");
        // Return noMatch with accumulated enrichedData from all evaluated rules
        return new RuleResult("no-match", "No matching rules found", SeverityConstants.INFO, false, RuleResult.ResultType.NO_MATCH,
                             null, accumulatedEnrichedData, new java.util.ArrayList<>(), true, null, null, null);
    }

    /**
     * Evaluate a code expression (either constant or SpEL).
     * Phase 4 Enhancement: Supports both constant strings and SpEL expressions for codes.
     *
     * @param codeExpression The code expression to evaluate (e.g., "SUCCESS_CODE" or "#amount > 100 ? 'HIGH' : 'LOW'")
     * @param context The evaluation context for SpEL expressions
     * @return The evaluated code string, or null if evaluation fails
     */
    private String evaluateCode(String codeExpression, EvaluationContext context) {
        if (codeExpression == null || codeExpression.trim().isEmpty()) {
            return null;
        }

        try {
            // Check if it's a SpEL expression (starts with #)
            if (codeExpression.trim().startsWith("#")) {
                Expression exp = parser.parseExpression(codeExpression);
                Object result = exp.getValue(context);
                return result != null ? result.toString() : null;
            } else {
                // It's a constant string
                return codeExpression;
            }
        } catch (Exception e) {
            logger.warn("Error evaluating code expression '{}': {}", codeExpression, e.getMessage());
            return null;
        }
    }

    /**
     * Apply field mappings to the enriched data.
     * Phase 4 Enhancement: Supports generic field mapping using SpEL expressions.
     *
     * @param mapToField The field mapping configuration
     * @param context The evaluation context for SpEL expressions
     * @param enrichedData The enriched data map to update with mapped values
     * @param successCode The evaluated success code (available as #success-code in expressions)
     * @param errorCode The evaluated error code (available as #error-code in expressions)
     */
    private void applyFieldMappings(List<String> mapToField, EvaluationContext context, Map<String, Object> enrichedData,
                                   String successCode, String errorCode) {
        if (mapToField == null) {
            return;
        }

        try {
            // Create a context that includes the codes
            StandardEvaluationContext mappingContext = (StandardEvaluationContext) context;
            if (successCode != null) {
                // Use underscore instead of hyphen for SpEL compatibility
                mappingContext.setVariable("success_code", successCode);
            }
            if (errorCode != null) {
                // Use underscore instead of hyphen for SpEL compatibility
                mappingContext.setVariable("error_code", errorCode);
            }

            // Handle both single mapping (String) and multiple mappings (List<String>)
            // Apply each mapping
            for (String mapping : mapToField) {
                applyFieldMapping(mapping, mappingContext, enrichedData);
            }
        } catch (Exception e) {
            logger.warn("Error applying field mappings: {}", e.getMessage());
        }
    }

    /**
     * Apply a single field mapping expression.
     * Parses expressions like "fieldName = #success-code" or "status = #amount > 100 ? 'HIGH' : 'LOW'"
     *
     * @param mapping The mapping expression
     * @param context The evaluation context
     * @param enrichedData The enriched data map to update
     */
    private void applyFieldMapping(String mapping, StandardEvaluationContext context, Map<String, Object> enrichedData) {
        try {
            // Parse the mapping: "fieldName = expression"
            String[] parts = mapping.split("=", 2);
            if (parts.length != 2) {
                logger.warn("Invalid field mapping format: {}. Expected 'fieldName = expression'", mapping);
                return;
            }

            String fieldName = parts[0].trim();
            String expression = parts[1].trim();

            // Evaluate the expression
            Expression exp = parser.parseExpression(expression);
            Object value = exp.getValue(context);

            // Store the mapped value in enriched data
            enrichedData.put(fieldName, value);
            logger.info("Applied field mapping: {} = {}", fieldName, value);
        } catch (Exception e) {
            logger.warn("Error applying field mapping '{}': {}", mapping, e.getMessage());
        }
    }
    
    /**
     * Create enhanced error message that provides helpful context about undefined variables.
     * Detects SpEL errors related to undefined/null variables and enhances the message.
     *
     * @param rule The rule that failed
     * @param exception The exception that occurred
     * @return Enhanced error message with undefined variable details
     */
    private String createEnhancedErrorMessage(Rule rule, Exception exception) {
        String baseMessage = exception.getMessage();
        
        // Check if this is a "null context object" error (EL1011E) which typically means undefined variable
        if (baseMessage != null && (baseMessage.contains("EL1011E") || 
                                   baseMessage.contains("null context object") ||
                                   baseMessage.contains("Attempted to call method"))) {
            
            // Try to extract the variable name from the rule condition
            String condition = rule.getCondition();
            if (condition != null && condition.contains("#")) {
                // Find all #variable references in the condition
                Matcher matcher = SPEL_VARIABLE_PATTERN.matcher(condition);
                if (matcher.find()) {
                    String varName = matcher.group(1);
                    return String.format("Rule evaluation failed: %s - Rule references undefined or inaccessible variable '%s' in condition: %s",
                        rule.getName(), varName, baseMessage);
                }
            }
        }
        
        // For other exceptions, use standard format
        return String.format(ERROR_MESSAGE_FORMAT, rule.getName(), baseMessage);
    }
    
    /**
     * Extract variable name from SpEL exception message or condition.
     * Attempts to parse messages like "Property or field 'age' cannot be found"
     * or extract from condition like "#undefinedVariable.length() > 0"
     *
     * @param exceptionMessage The exception message
     * @param condition The rule condition that failed
     * @return The variable name, or "unknown" if not parseable
     */
    private String extractVariableName(String exceptionMessage, String condition) {
        if (exceptionMessage == null && condition == null) {
            return "unknown";
        }
        
        // First try to extract variable name from exception message
        // Patterns like: "Property or field 'varName'"
        if (exceptionMessage != null) {
            int startQuote = exceptionMessage.indexOf('\'');
            if (startQuote >= 0) {
                int endQuote = exceptionMessage.indexOf('\'', startQuote + 1);
                if (endQuote > startQuote) {
                    return exceptionMessage.substring(startQuote + 1, endQuote);
                }
            }
        }
        
        // If that didn't work, try to extract from condition
        // Pattern: #variableName or #variableName.something
        if (condition != null && condition.contains("#")) {
            int hashIndex = condition.indexOf('#');
            int endIndex = hashIndex + 1;
            
            // Find the end of the variable name (stops at space, dot, bracket, or operator)
            while (endIndex < condition.length()) {
                char c = condition.charAt(endIndex);
                if (!Character.isLetterOrDigit(c) && c != '_') {
                    break;
                }
                endIndex++;
            }
            
            if (endIndex > hashIndex + 1) {
                return condition.substring(hashIndex + 1, endIndex);
            }
        }
        
        return "unknown";
    }
    
    /**
     * Resolve message template placeholders against the SpEL evaluation context.
     * Supports two placeholder formats:
     * <ul>
     *   <li>{@code {{#expression}}} - Handlebars-style (used in most YAML configs)</li>
     *   <li>{@code #{expression}} - SpEL template style (used by TemplateProcessorService)</li>
     * </ul>
     * 
     * The expression inside the placeholder is evaluated as a SpEL expression against
     * the provided context. If evaluation fails, the original placeholder is preserved.
     *
     * @param message The message template to resolve
     * @param context The SpEL evaluation context containing variable bindings
     * @return The message with all resolvable placeholders replaced by their values
     */
    String resolveMessageTemplate(String message, EvaluationContext context) {
        if (message == null || context == null) {
            return message;
        }
        
        // Quick check: if no placeholders, return as-is
        if (!message.contains("{{#") && !message.contains("#{")) {
            return message;
        }
        
        String resolved = message;
        
        // Resolve {{#expression}} (Handlebars-style) placeholders
        if (resolved.contains("{{#")) {
            Matcher hbMatcher = HANDLEBARS_PLACEHOLDER_PATTERN.matcher(resolved);
            StringBuilder sb = new StringBuilder();
            while (hbMatcher.find()) {
                String spelExpr = hbMatcher.group(1); // e.g., "#age" or "#amount"
                try {
                    Expression expression = parser.parseExpression(spelExpr);
                    Object value = expression.getValue(context);
                    String replacement = value != null ? Matcher.quoteReplacement(value.toString()) : "";
                    hbMatcher.appendReplacement(sb, replacement);
                    logger.trace("Resolved message placeholder '{{{{{}}}}}' to '{}'", spelExpr, value);
                } catch (Exception e) {
                    // Preserve original placeholder on error
                    hbMatcher.appendReplacement(sb, Matcher.quoteReplacement(hbMatcher.group(0)));
                    logger.debug("Could not resolve message placeholder '{}': {}", spelExpr, e.getMessage());
                }
            }
            hbMatcher.appendTail(sb);
            resolved = sb.toString();
        }
        
        //Resolve #{expression} (SpEL template) placeholders
        if (resolved.contains("#{")) {
            Matcher spelMatcher = HASH_PLACEHOLDER_PATTERN.matcher(resolved);
            StringBuilder sb = new StringBuilder();
            while (spelMatcher.find()) {
                String spelExpr = spelMatcher.group(1); // e.g., "age" or "amount"
                try {
                    Expression expression = parser.parseExpression(spelExpr);
                    Object value = expression.getValue(context);
                    String replacement = value != null ? Matcher.quoteReplacement(value.toString()) : "";
                    spelMatcher.appendReplacement(sb, replacement);
                    logger.trace("Resolved message placeholder '#{{{}}}' to '{}'", spelExpr, value);
                } catch (Exception e) {
                    spelMatcher.appendReplacement(sb, Matcher.quoteReplacement(spelMatcher.group(0)));
                    logger.debug("Could not resolve message placeholder '{}': {}", spelExpr, e.getMessage());
                }
            }
            spelMatcher.appendTail(sb);
            resolved = sb.toString();
        }
        
        if (!resolved.equals(message)) {
            logger.debug("Resolved message template: '{}' -> '{}'", message, resolved);
        }
        
        return resolved;
    }
}


