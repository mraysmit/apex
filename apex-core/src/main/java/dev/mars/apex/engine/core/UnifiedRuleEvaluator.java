package dev.mars.apex.engine.core;

import dev.mars.apex.core.config.error.ErrorRecoveryConfig;
import dev.mars.apex.core.config.error.SeverityRecoveryPolicy;
import dev.mars.apex.core.config.model.condition.SharedConditionGroup;
import dev.mars.apex.core.config.model.condition.SharedConditionRule;
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
import org.springframework.expression.spel.support.StandardEvaluationContext;

import dev.mars.apex.core.config.EnrichmentGroupFactory;
import dev.mars.apex.core.config.model.YamlEnrichment;
import dev.mars.apex.core.config.model.YamlRuleConfiguration;
import dev.mars.apex.core.service.lookup.LookupService;
import dev.mars.apex.core.service.lookup.LookupServiceRegistry;
import dev.mars.apex.engine.execution.EnrichmentGroupExecutor;
import dev.mars.apex.engine.model.EnrichmentGroup;
import dev.mars.apex.engine.model.EnrichmentGroupResult;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

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

    // Optional: enables lookup predicate execution in structured condition rules (Phase 3)
    private LookupServiceRegistry serviceRegistry;

    // Optional: enables function predicate execution in structured condition rules (Phase 4)
    private YamlRuleConfiguration yamlRuleConfiguration;
    private Supplier<EnrichmentGroupExecutor> enrichmentGroupExecutorSupplier;
    
    /**
     * Create a new UnifiedRuleEvaluator with default components.
     */
    public UnifiedRuleEvaluator() {
        this.parser = SpelParserHolder.INSTANCE;
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
        this.parser = parser != null ? parser : SpelParserHolder.INSTANCE;
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
     * Create a new UnifiedRuleEvaluator with lookup execution support (Phase 3).
     *
     * <p>Lookup predicates in structured condition groups will be executed via the
     * supplied registry. Named lookup services registered under
     * {@link LookupServiceRegistry} are resolved at evaluation time using the
     * {@code lookup-service} name from the predicate's {@code lookup-config}.
     * The result is stashed as a context variable under {@code result-field} before
     * the SpEL gate condition is evaluated.</p>
     *
     * @param serviceRegistry Registry of named lookup services for condition predicate execution
     */
    public UnifiedRuleEvaluator(LookupServiceRegistry serviceRegistry) {
        this();
        this.serviceRegistry = serviceRegistry;
    }

    /**
     * Set the YAML rule configuration for function predicate execution.
     * Required for function-type structured condition predicates.
     * Called by {@link RulesEngine} after construction.
     */
    public void setYamlRuleConfiguration(YamlRuleConfiguration config) {
        this.yamlRuleConfiguration = config;
    }

    /**
     * Set the enrichment group executor supplier for function predicate execution.
     * Uses a supplier to break the circular construction dependency in
     * {@link RulesEngine} (same pattern as
     * {@code EnrichmentProcessor.setEnrichmentGroupExecutorSupplier}).
     */
    public void setEnrichmentGroupExecutorSupplier(Supplier<EnrichmentGroupExecutor> supplier) {
        this.enrichmentGroupExecutorSupplier = supplier;
    }

    /**
     * Evaluate a single rule against the provided context.
     * This is the public entry-point for callers that supply a pre-built context.
     * Delegates to {@link #evaluateRuleInternal} with a {@code null} facts Map.
     *
     * @param rule The rule to evaluate
     * @param context The evaluation context
     * @return The rule evaluation result
     */
    public RuleResult evaluateRule(Rule rule, EvaluationContext context) {
        return evaluateRuleInternal(rule, context, null);
    }

    /**
     * Core evaluation method. Accepts an optional facts Map so that function-type
     * structured-condition predicates can execute enrichment groups that mutate the
     * facts Map in place and then stash the output into the SpEL context.
     *
     * @param rule    The rule to evaluate
     * @param context The SpEL evaluation context
     * @param facts   The live facts Map, or {@code null} when called from the
     *                context-only path (function predicates fall back to gate-only evaluation)
     * @return The rule evaluation result
     */
    private RuleResult evaluateRuleInternal(Rule rule, EvaluationContext context, Map<String, Object> facts) {
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
        logger.debug("Rule details - id: '{}', severity: '{}', condition: '{}', hasConditions: {}", 
                        rule.getId(), rule.getSeverity(), rule.getCondition(), rule.getConditions() != null);

        // Start performance monitoring
        RulePerformanceMetrics.Builder metricsBuilder = performanceMonitor.startEvaluation(rule.getName(), "evaluation");
        
        try {
            Boolean result;

            if (rule.getConditions() != null) {
                // Structured condition group evaluation (AND/OR with typed predicates)
                logger.debug("Evaluating structured conditions for rule '{}': {} group with {} predicates",
                        rule.getName(), rule.getConditions().getOperator(),
                        rule.getConditions().getRules() != null ? rule.getConditions().getRules().size() : 0);
                try {
                    result = evaluateStructuredConditionGroup(rule.getConditions(), context, facts);
                    logger.debug("Structured condition evaluation for rule '{}' returned: {}", rule.getName(), result);
                } catch (Exception e) {
                    logger.debug("Structured condition evaluation exception for rule '{}': {}", rule.getName(), e.getMessage());
                    return errorRecoveryHandler.handleEvaluationError(rule, e, metricsBuilder);
                }
            } else if (rule.getCondition() != null && !rule.getCondition().trim().isEmpty()) {
                // Traditional string condition — parse and evaluate the SpEL expression
                logger.debug("Parsing SpEL expression for rule '{}': {}", rule.getName(), rule.getCondition());
                Expression exp = parser.parseExpression(rule.getCondition());
                try {
                    result = exp.getValue(context, Boolean.class);
                    logger.debug("SpEL evaluation for rule '{}' returned: {}", rule.getName(), result);
                } catch (SpelEvaluationException e) {
                    logger.debug("SpEL evaluation exception for rule '{}': {}", rule.getName(), e.getMessage());
                    return errorRecoveryHandler.handleEvaluationError(rule, e, metricsBuilder);
                }
            } else {
                // Neither condition nor conditions — invalid rule
                logger.warn("Rule '{}' has no condition to evaluate", rule.getName());
                RulePerformanceMetrics metrics = performanceMonitor.completeEvaluation(metricsBuilder, rule.getCondition());
                return RuleResult.errorWithCode(rule.getName(), "Rule has no condition to evaluate", rule.getSeverity(), "APEX-RULE-001", metrics);
            }

            // Store result in context if result-field is configured
            if (rule.getResultField() != null && !rule.getResultField().trim().isEmpty()) {
                boolean booleanResult = (result != null && result);
                context.setVariable(rule.getResultField(), booleanResult);
                logger.debug("Stored rule result in context: {} = {}", rule.getResultField(), booleanResult);
            }

            // Complete performance monitoring for successful evaluation
            String conditionForMetrics = rule.getCondition() != null ? rule.getCondition()
                    : ("structured:" + (rule.getConditions() != null ? rule.getConditions().getOperator() : "none"));
            RulePerformanceMetrics metrics = performanceMonitor.completeEvaluation(metricsBuilder, conditionForMetrics);

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

        RuleResult result = evaluateRuleInternal(rule, context, facts);
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

    // =========================================================================
    // Structured Condition Group Evaluation
    // =========================================================================

    /**
     * Evaluate a structured condition group (AND/OR) against the evaluation context.
     * No-facts delegation wrapper — calls the facts-aware overload with {@code null}.
     *
     * @param group   The structured condition group
     * @param context The SpEL evaluation context
     * @return true if the group evaluates to true, false otherwise
     */
    private boolean evaluateStructuredConditionGroup(SharedConditionGroup group, EvaluationContext context) {
        return evaluateStructuredConditionGroup(group, context, null);
    }

    /**
     * Evaluate a structured condition group (AND/OR) against the evaluation context.
     * Supports expression-type predicates with full SpEL evaluation.
     * Lookup predicates execute via the injected {@link LookupServiceRegistry} (if configured)
     * before evaluating their condition gate. Function predicates execute via the injected
     * {@link EnrichmentGroupExecutor} (if configured and {@code facts} is non-null).
     * The gate may be a flat SpEL {@code condition} string or a nested {@code conditions}
     * structured group; if both are set, {@code conditions} takes precedence.
     *
     * @param group   The structured condition group
     * @param context The SpEL evaluation context
     * @param facts   The live facts Map, or {@code null} when called from the context-only path
     * @return true if the group evaluates to true, false otherwise
     */
    private boolean evaluateStructuredConditionGroup(SharedConditionGroup group, EvaluationContext context,
                                                     Map<String, Object> facts) {
        if (group.getRules() == null || group.getRules().isEmpty()) {
            logger.debug("Structured condition group has no predicates, returning true");
            return true;
        }

        String operator = group.getOperator();
        if (operator == null) {
            operator = "AND";
        }

        if ("OR".equalsIgnoreCase(operator)) {
            for (SharedConditionRule rule : group.getRules()) {
                if (evaluateStructuredConditionRule(rule, context, facts)) {
                    logger.debug("OR predicate satisfied: {}", rule.getDescription());
                    return true;
                }
            }
            return false;
        } else {
            // AND (default)
            for (SharedConditionRule rule : group.getRules()) {
                if (!evaluateStructuredConditionRule(rule, context, facts)) {
                    logger.debug("AND predicate failed: {}", rule.getDescription());
                    return false;
                }
            }
            return true;
        }
    }

    /**
     * Evaluate a single structured condition predicate.
     * No-facts delegation wrapper — calls the facts-aware overload with {@code null}.
     *
     * @param rule    The condition predicate
     * @param context The SpEL evaluation context
     * @return true if the predicate is satisfied
     */
    private boolean evaluateStructuredConditionRule(SharedConditionRule rule, EvaluationContext context) {
        return evaluateStructuredConditionRule(rule, context, null);
    }

    /**
     * Evaluate a single structured condition predicate.
     * <ul>
     *   <li>{@code expression}: Evaluates the SpEL condition or nested conditions group directly.</li>
     *   <li>{@code lookup}: Executes the lookup via the injected {@link LookupServiceRegistry} (if present),
     *       stashes the result as a context variable under {@code result-field}, then evaluates the
     *       SpEL condition gate or nested conditions group. Falls back to gate-only evaluation when
     *       no registry is configured.</li>
     *   <li>{@code function}: Executes the referenced enrichment group via the injected
     *       {@link EnrichmentGroupExecutor} (if present), binds input-parameters into the facts Map,
     *       stashes the {@code output-field} value into the context, then evaluates the SpEL condition
     *       gate. Falls back to gate-only evaluation when executor/config/facts are unavailable.</li>
     * </ul>
     *
     * @param rule    The condition predicate
     * @param context The SpEL evaluation context
     * @param facts   The live facts Map, or {@code null} when called from the context-only path
     * @return true if the predicate is satisfied
     */
    private boolean evaluateStructuredConditionRule(SharedConditionRule rule, EvaluationContext context,
                                                    Map<String, Object> facts) {
        String type = rule.getType();

        if ("lookup".equalsIgnoreCase(type)) {
            if (serviceRegistry != null) {
                executeLookupPredicate(rule, context);
            } else {
                logger.debug("Lookup predicate '{}' — no LookupServiceRegistry configured; evaluating gate only",
                        rule.getDescription());
            }
        } else if ("function".equalsIgnoreCase(type)) {
            if (enrichmentGroupExecutorSupplier != null && yamlRuleConfiguration != null && facts != null) {
                executeFunctionPredicate(rule, context, facts);
            } else {
                logger.debug("Function predicate '{}' — EnrichmentGroupExecutor/YamlRuleConfiguration/facts not available; evaluating gate only",
                        rule.getDescription());
            }
        }

        // Evaluate the condition gate — supports both 'conditions' (structured group) and 'condition' (SpEL string)
        SharedConditionGroup nestedConditions = rule.getConditions();
        String condition = rule.getCondition();

        if (nestedConditions != null) {
            // Nested structured condition gate — delegate recursively
            logger.debug("Evaluating nested structured conditions gate for predicate '{}'", rule.getDescription());
            return evaluateStructuredConditionGroup(nestedConditions, context, facts);
        } else if (condition == null || condition.trim().isEmpty()) {
            // No condition gate — implicitly true (valid for function/lookup without a condition)
            return true;
        } else {
            // Simple SpEL condition gate
            Expression exp = parser.parseExpression(condition);
            Boolean result = exp.getValue(context, Boolean.class);
            return result != null && result;
        }
    }

    /**
     * Execute a function predicate: binds input-parameters, invokes the referenced enrichment group,
     * and stashes the {@code output-field} value from the facts Map into the evaluation context so
     * the gate condition can access it via {@code #outputField}.
     *
     * <p>Resolution order:
     * <ol>
     *   <li>Guard: {@code enrichment-group-ref} must be non-empty.</li>
     *   <li>Apply {@code input-parameters}: evaluate each {@code source-field} SpEL expression and
     *       put the result into {@code facts.put(targetField, value)}.</li>
     *   <li>Resolve enrichment group by {@code enrichment-group-ref} via
     *       {@link EnrichmentGroupFactory#buildEnrichmentGroups(YamlRuleConfiguration)}.</li>
     *   <li>Execute via {@link EnrichmentGroupExecutor#processEnrichmentGroup} passing the facts Map
     *       as {@code targetObject} (mutates Map in place).</li>
     *   <li>Read {@code output-field} from the facts Map and stash into the context.</li>
     * </ol>
     *
     * <p>All failures are logged as warnings and silently swallowed so the evaluator falls through
     * to gate evaluation with whatever value is currently in the context.</p>
     *
     * @param rule    The condition predicate
     * @param context The SpEL evaluation context to mutate with the stashed output
     * @param facts   The live facts Map — serves as {@code targetObject} for the enrichment group
     */
    private void executeFunctionPredicate(SharedConditionRule rule,
                                          EvaluationContext context,
                                          Map<String, Object> facts) {
        String groupRef = rule.getEnrichmentGroupRef();
        if (groupRef == null || groupRef.trim().isEmpty()) {
            logger.warn("Function predicate '{}' has no enrichment-group-ref; skipping execution",
                    rule.getDescription());
            return;
        }

        try {
            // Step 1: Apply input-parameters — bind values into the facts Map
            List<YamlEnrichment.FieldMapping> inputParams = rule.getInputParameters();
            if (inputParams != null) {
                for (YamlEnrichment.FieldMapping param : inputParams) {
                    try {
                        String src = param.getSourceField();
                        if (src == null || src.trim().isEmpty()) {
                            continue;
                        }
                        String spel = src.startsWith("#") ? src : "#" + src;
                        Object value = parser.parseExpression(spel).getValue(context);
                        if (param.getTargetField() != null) {
                            facts.put(param.getTargetField(), value);
                            logger.debug("Function predicate '{}' input: {} -> {} = {}",
                                    rule.getDescription(), src, param.getTargetField(), value);
                        }
                    } catch (Exception e) {
                        logger.warn("Function predicate '{}' failed to bind input-parameter '{}': {}",
                                rule.getDescription(), param.getSourceField(), e.getMessage());
                    }
                }
            }

            // Step 2: Resolve enrichment group by ref
            List<EnrichmentGroup> groups = EnrichmentGroupFactory.buildEnrichmentGroups(yamlRuleConfiguration);
            EnrichmentGroup targetGroup = groups.stream()
                    .filter(g -> groupRef.equals(g.getId()))
                    .findFirst()
                    .orElse(null);

            if (targetGroup == null) {
                logger.warn("Function predicate '{}': enrichment-group-ref '{}' not found in configuration",
                        rule.getDescription(), groupRef);
                return;
            }

            // Step 3: Execute the enrichment group — mutates the facts Map in place
            EnrichmentGroupExecutor executor = enrichmentGroupExecutorSupplier.get();
            EnrichmentGroupResult groupResult =
                    executor.processEnrichmentGroup(targetGroup, facts, yamlRuleConfiguration);

            if (!groupResult.isSuccess()) {
                logger.warn("Function predicate '{}': enrichment group '{}' execution failed: {}",
                        rule.getDescription(), groupRef, groupResult.getMessage());
                return;
            }

            // Step 4: Stash output-field from facts Map into context variable
            String outputField = rule.getOutputField();
            if (outputField != null && !outputField.trim().isEmpty()
                    && context instanceof StandardEvaluationContext) {
                Object outputValue = facts.get(outputField);
                ((StandardEvaluationContext) context).setVariable(outputField, outputValue);
                logger.debug("Function predicate '{}' stashed output into context variable '{}': {}",
                        rule.getDescription(), outputField, outputValue);
            }

        } catch (Exception e) {
            logger.warn("Function predicate '{}' execution failed: {}", rule.getDescription(), e.getMessage());
            logger.debug("Full stack trace for function predicate execution failure:", e);
        }
    }

    /**
     * Execute the lookup for a {@code lookup}-type condition predicate and stash the result
     * into the evaluation context so the gate condition can reference it via {@code #result-field}.
     *
     * <p>Resolution order:
     * <ol>
     *   <li>Resolve the named {@link LookupService} from {@link #serviceRegistry} using
     *       {@code lookup-config.lookup-service}.</li>
     *   <li>Evaluate the {@code lookup-key} SpEL expression against the current context.</li>
     *   <li>Call {@link LookupService#transform(Object)} with the resolved key.</li>
     *   <li>Stash the result into the context via
     *       {@link StandardEvaluationContext#setVariable(String, Object)} under {@code result-field},
     *       so the gate SpEL expression (e.g. {@code #customerTier == 'PREMIUM'}) can access it.</li>
     * </ol>
     * If the result is a {@code Map} that contains the {@code result-field} key, the scalar value
     * under that key is stashed (not the whole map) to avoid double-nesting.</p>
     *
     * <p>All failures (missing config, service not found, key evaluation failure, lookup exception)
     * are logged as warnings and silently swallowed so the evaluator can fall through to gate
     * evaluation with whatever value (or absence of value) is currently in the context.</p>
     *
     * @param rule    The condition predicate with {@code lookup-config} and {@code result-field}
     * @param context The SpEL evaluation context to mutate with the stashed result
     */
    private void executeLookupPredicate(SharedConditionRule rule, EvaluationContext context) {
        YamlEnrichment.LookupConfig lookupConfig = rule.getLookupConfig();
        if (lookupConfig == null) {
            logger.warn("Lookup predicate '{}' has no lookup-config; skipping execution", rule.getDescription());
            return;
        }

        String serviceName = lookupConfig.getLookupService();
        if (serviceName == null || serviceName.trim().isEmpty()) {
            logger.warn("Lookup predicate '{}' specifies no lookup-service name; skipping execution", rule.getDescription());
            return;
        }

        LookupService lookupService = serviceRegistry.getService(serviceName, LookupService.class);
        if (lookupService == null) {
            logger.warn("Lookup service '{}' not found in registry for predicate '{}'",
                    serviceName, rule.getDescription());
            return;
        }

        try {
            Expression keyExpr = parser.parseExpression(lookupConfig.getLookupKey());
            Object lookupKey = keyExpr.getValue(context);

            if (lookupKey == null) {
                logger.warn("Lookup key expression '{}' evaluated to null for predicate '{}'",
                        lookupConfig.getLookupKey(), rule.getDescription());
                return;
            }

            Object result = lookupService.transform(lookupKey);
            logger.debug("Lookup predicate '{}' executed with key '{}', result: {}",
                    rule.getDescription(), lookupKey, result);

            String resultField = rule.getResultField();
            if (resultField != null && !resultField.trim().isEmpty()
                    && context instanceof StandardEvaluationContext) {
                StandardEvaluationContext stdCtx = (StandardEvaluationContext) context;
                // If the result is a Map containing the resultField key, extract the scalar
                // to avoid stashing {resultField: value} as a whole-map context variable.
                if (result instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> resultMap = (Map<String, Object>) result;
                    stdCtx.setVariable(resultField,
                            resultMap.containsKey(resultField) ? resultMap.get(resultField) : result);
                } else {
                    stdCtx.setVariable(resultField, result);
                }
                logger.debug("Lookup predicate '{}' stashed result into context variable '{}': {}",
                        rule.getDescription(), resultField, result);
            }
        } catch (Exception e) {
            logger.warn("Lookup predicate '{}' execution failed: {}", rule.getDescription(), e.getMessage());
            logger.debug("Full stack trace for lookup predicate execution failure:", e);
        }
    }
}
