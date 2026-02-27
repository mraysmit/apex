package dev.mars.apex.engine.core;

import dev.mars.apex.core.config.error.ErrorRecoveryConfig;
import dev.mars.apex.core.config.error.SeverityRecoveryPolicy;
import dev.mars.apex.core.constants.SeverityConstants;
import dev.mars.apex.core.service.error.ErrorRecoveryService;
import dev.mars.apex.core.service.monitoring.RulePerformanceMetrics;
import dev.mars.apex.core.service.monitoring.RulePerformanceMonitor;
import dev.mars.apex.engine.model.Rule;
import dev.mars.apex.engine.model.RuleResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Handles evaluation errors with severity-based recovery strategies.
 *
 * <p>Encapsulates the full error-recovery lifecycle:</p>
 * <ol>
 *   <li>Enhanced error message creation (including undefined-variable detection)</li>
 *   <li>Severity-based recovery policy lookup and execution</li>
 *   <li>Performance metrics construction with recovery information</li>
 *   <li>Error code classification (SpEL exception → APEX error code)</li>
 * </ol>
 *
 * <p>Extracted from {@link UnifiedRuleEvaluator} to isolate error-handling complexity
 * from the core evaluation path.</p>
 *
 * @author Mark A Ray-Smith
 * @since 2026-02-28
 * @version 1.0
 */
public class ErrorRecoveryHandler {

    private static final Logger logger = LoggerFactory.getLogger(ErrorRecoveryHandler.class);

    /**
     * Standard error message format for consistency across all evaluation paths.
     */
    private static final String ERROR_MESSAGE_FORMAT = "Rule evaluation failed: %s - %s";

    /**
     * Pattern to extract variable names from SpEL expressions.
     * Matches #variableName patterns.
     */
    private static final Pattern SPEL_VARIABLE_PATTERN = Pattern.compile("#(\\w+)");

    private final ErrorRecoveryConfig errorRecoveryConfig;
    private final ErrorRecoveryService errorRecoveryService;
    private final RulePerformanceMonitor performanceMonitor;

    /**
     * Create a new ErrorRecoveryHandler.
     *
     * @param errorRecoveryConfig The error recovery configuration (severity policies, toggles)
     * @param errorRecoveryService The error recovery service (executes recovery strategies)
     * @param performanceMonitor The performance monitor (completes metrics on error paths)
     */
    public ErrorRecoveryHandler(ErrorRecoveryConfig errorRecoveryConfig,
                                ErrorRecoveryService errorRecoveryService,
                                RulePerformanceMonitor performanceMonitor) {
        this.errorRecoveryConfig = errorRecoveryConfig;
        this.errorRecoveryService = errorRecoveryService;
        this.performanceMonitor = performanceMonitor;
    }

    /**
     * Handle an evaluation error with consistent error recovery logic.
     *
     * @param rule The rule that failed evaluation
     * @param exception The exception that occurred
     * @param metricsBuilder The performance metrics builder (already started)
     * @return The error result or recovered result
     */
    public RuleResult handleEvaluationError(Rule rule, Exception exception, RulePerformanceMetrics.Builder metricsBuilder) {
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

            // Check if rule has a specific default-value
            if (rule.getDefaultValue() != null) {
                recoveryStrategy = "RULE_DEFAULT_VALUE";
                if (errorRecoveryConfig.isLogRecoveryAttempts()) {
                    logger.info("Using rule-specific default value for recovery: rule='{}', defaultValue='{}'",
                        rule.getName(), rule.getDefaultValue());
                }
                recoverySuccessful = true;
                if (recoveryStartTime != null) {
                    recoveryTime = Duration.between(recoveryStartTime, Instant.now());
                }

                RulePerformanceMetrics metrics = buildMetricsWithRecovery(metricsBuilder, rule, exception,
                    recoveryAttempted, recoverySuccessful, recoveryStrategy, recoveryReason, recoveryTime);

                return RuleResult.match(rule.getName(), String.valueOf(rule.getDefaultValue()), severity, metrics);
            }

            // Use the error recovery service with the determined strategy
            ErrorRecoveryService.ErrorRecoveryStrategy strategy = "FAIL_FAST".equals(actualStrategy) ?
                ErrorRecoveryService.ErrorRecoveryStrategy.FAIL_FAST :
                ErrorRecoveryService.ErrorRecoveryStrategy.CONTINUE_WITH_DEFAULT;
            ErrorRecoveryService.RecoveryResult recoveryResult =
                errorRecoveryService.attemptRecovery(rule.getName(), rule.getCondition(), null, exception, strategy);

            if (recoveryResult != null && recoveryResult.isSuccessful()) {
                recoverySuccessful = true;
                recoveryStrategy = actualStrategy;
                if (recoveryStartTime != null) {
                    recoveryTime = Duration.between(recoveryStartTime, Instant.now());
                }

                RulePerformanceMetrics metrics = buildMetricsWithRecovery(metricsBuilder, rule, exception,
                    recoveryAttempted, recoverySuccessful, recoveryStrategy, recoveryReason, recoveryTime);

                // Preserve original rule severity in recovery result
                RuleResult originalResult = recoveryResult.getRuleResult();
                return originalResult.toBuilder()
                        .severity(severity)
                        .triggered(false)
                        .performanceMetrics(metrics)
                        .build();
            } else {
                // Recovery failed
                recoverySuccessful = false;
                recoveryStrategy = actualStrategy;
                if (recoveryStartTime != null) {
                    recoveryTime = Duration.between(recoveryStartTime, Instant.now());
                }
            }
        }

        // Log error details at appropriate level based on severity
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

        // Classify error code based on exception type and message
        String errorCode = classifyErrorCode(exception);

        return RuleResult.errorWithCode(rule.getName(), errorMessage, severity, errorCode, finalMetrics);
    }

    /**
     * Build performance metrics with recovery information.
     * Only includes recovery metrics if metrics are enabled in configuration.
     */
    RulePerformanceMetrics buildMetricsWithRecovery(RulePerformanceMetrics.Builder metricsBuilder,
                                                    Rule rule, Exception exception,
                                                    boolean recoveryAttempted, boolean recoverySuccessful,
                                                    String recoveryStrategy, String recoveryReason,
                                                    Duration recoveryTime) {
        // Complete the basic evaluation metrics first
        RulePerformanceMetrics baseMetrics = performanceMonitor.completeEvaluation(metricsBuilder, rule.getCondition(), exception);

        // Only add recovery metrics if metrics are enabled
        if (errorRecoveryConfig.isMetricsEnabled()) {
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
            return baseMetrics;
        }
    }

    /**
     * Classify the APEX error code based on the exception type and message.
     * Maps SpEL exceptions and other errors to standardized APEX error codes.
     *
     * @param exception The exception that occurred during rule evaluation
     * @return The APEX error code (e.g., APEX-RULE-001, APEX-RULE-002)
     */
    String classifyErrorCode(Exception exception) {
        String message = exception.getMessage();
        if (message == null) {
            return "APEX-RULE-999";
        }

        // EL1008E: Property or field not found
        if (message.contains("EL1008E") || message.contains("Property or field") || message.contains("cannot be found")) {
            return "APEX-RULE-002";
        }
        // EL1004E: Method not found
        if (message.contains("EL1004E") || message.contains("Method call:")) {
            return "APEX-RULE-005";
        }
        // EL1011E: Null context / undefined variable
        if (message.contains("EL1011E") || message.contains("null context object")) {
            return "APEX-RULE-004";
        }
        // EL1001E: Type conversion error
        if (message.contains("EL1001E") || message.contains("Type conversion")) {
            return "APEX-RULE-003";
        }
        // EL1030E: Operator overloading / arithmetic error
        if (message.contains("EL1030E") || message.contains("divide by zero") || message.contains("Division by zero")) {
            return "APEX-RULE-007";
        }
        // EL1041E: Access denied
        if (message.contains("EL1041E") || message.contains("not accessible")) {
            return "APEX-RULE-006";
        }
        // SpelParseException: expression syntax error
        if (exception instanceof org.springframework.expression.spel.SpelParseException) {
            return "APEX-RULE-001";
        }
        // SpelEvaluationException: generic evaluation error
        if (exception instanceof org.springframework.expression.spel.SpelEvaluationException) {
            return "APEX-RULE-001";
        }
        // Default: general rule error
        return "APEX-RULE-999";
    }

    /**
     * Create enhanced error message that provides helpful context about undefined variables.
     * Detects SpEL errors related to undefined/null variables and enhances the message.
     *
     * @param rule The rule that failed
     * @param exception The exception that occurred
     * @return Enhanced error message with undefined variable details
     */
    String createEnhancedErrorMessage(Rule rule, Exception exception) {
        String baseMessage = exception.getMessage();

        // Check if this is a "null context object" error (EL1011E) which typically means undefined variable
        if (baseMessage != null && (baseMessage.contains("EL1011E") ||
                                   baseMessage.contains("null context object") ||
                                   baseMessage.contains("Attempted to call method"))) {

            // Try to extract the variable name from the rule condition
            String condition = rule.getCondition();
            if (condition != null && condition.contains("#")) {
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
}
