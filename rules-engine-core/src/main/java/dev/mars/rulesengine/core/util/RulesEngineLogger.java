package dev.mars.rulesengine.core.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

import java.util.function.Supplier;

/**
 * Enhanced logger wrapper for the Rules Engine that provides:
 * - Performance-optimized logging with lazy evaluation
 * - Structured logging support
 * - Rule-specific context management
 * - Consistent log formatting
 */
public class RulesEngineLogger {
    private final Logger logger;
    
    // Predefined markers for different types of logging
    public static final Marker RULE_EVALUATION = MarkerFactory.getMarker("RULE_EVALUATION");
    public static final Marker PERFORMANCE = MarkerFactory.getMarker("PERFORMANCE");
    public static final Marker ERROR_RECOVERY = MarkerFactory.getMarker("ERROR_RECOVERY");
    public static final Marker AUDIT = MarkerFactory.getMarker("AUDIT");
    public static final Marker CONFIGURATION = MarkerFactory.getMarker("CONFIGURATION");

    /**
     * Create a new RulesEngineLogger for the specified class.
     * 
     * @param clazz The class to create the logger for
     */
    public RulesEngineLogger(Class<?> clazz) {
        this.logger = LoggerFactory.getLogger(clazz);
    }

    /**
     * Create a new RulesEngineLogger with the specified name.
     * 
     * @param name The logger name
     */
    public RulesEngineLogger(String name) {
        this.logger = LoggerFactory.getLogger(name);
    }

    /**
     * Get the underlying SLF4J logger.
     * 
     * @return The SLF4J logger
     */
    public Logger getLogger() {
        return logger;
    }

    // Rule Evaluation Logging

    /**
     * Log rule evaluation start.
     * 
     * @param ruleName The name of the rule
     */
    public void ruleEvaluationStart(String ruleName) {
        if (logger.isInfoEnabled()) {
            LoggingContext.setRuleName(ruleName);
            LoggingContext.setRulePhase("evaluation");
            logger.info(RULE_EVALUATION, "Starting evaluation of rule: {}", ruleName);
        }
    }

    /**
     * Log rule evaluation completion.
     * 
     * @param ruleName The name of the rule
     * @param result The evaluation result
     * @param evaluationTimeMs The evaluation time in milliseconds
     */
    public void ruleEvaluationComplete(String ruleName, boolean result, double evaluationTimeMs) {
        if (logger.isInfoEnabled()) {
            LoggingContext.setEvaluationTime(evaluationTimeMs);
            logger.info(RULE_EVALUATION, "Rule '{}' evaluated to {} in {}ms",
                       ruleName, result, String.format("%.2f", evaluationTimeMs));
        }
    }

    /**
     * Log rule evaluation error.
     * 
     * @param ruleName The name of the rule
     * @param error The error that occurred
     */
    public void ruleEvaluationError(String ruleName, Throwable error) {
        if (logger.isWarnEnabled()) {
            LoggingContext.setRuleName(ruleName);
            LoggingContext.setRulePhase("error");
            logger.warn(RULE_EVALUATION, "Error evaluating rule '{}': {}", 
                       ruleName, error.getMessage(), error);
        }
    }

    // Performance Logging

    /**
     * Log performance metrics.
     * 
     * @param ruleName The rule name
     * @param evaluationTimeMs The evaluation time
     * @param memoryUsed The memory used (optional)
     */
    public void performance(String ruleName, double evaluationTimeMs, Long memoryUsed) {
        if (logger.isDebugEnabled()) {
            LoggingContext.setRuleName(ruleName);
            LoggingContext.setEvaluationTime(evaluationTimeMs);

            if (memoryUsed != null) {
                logger.debug(PERFORMANCE, "Rule '{}' performance: {}ms, {}KB memory",
                           ruleName, String.format("%.2f", evaluationTimeMs), memoryUsed / 1024);
            } else {
                logger.debug(PERFORMANCE, "Rule '{}' performance: {}ms",
                           ruleName, String.format("%.2f", evaluationTimeMs));
            }
        }
    }

    /**
     * Log slow rule warning.
     * 
     * @param ruleName The rule name
     * @param evaluationTimeMs The evaluation time
     * @param threshold The threshold that was exceeded
     */
    public void slowRule(String ruleName, double evaluationTimeMs, double threshold) {
        if (logger.isWarnEnabled()) {
            LoggingContext.setRuleName(ruleName);
            LoggingContext.setEvaluationTime(evaluationTimeMs);
            logger.warn(PERFORMANCE, "Slow rule detected: '{}' took {}ms (threshold: {}ms)",
                       ruleName, String.format("%.2f", evaluationTimeMs), String.format("%.2f", threshold));
        }
    }

    // Error Recovery Logging

    /**
     * Log error recovery attempt.
     * 
     * @param ruleName The rule name
     * @param strategy The recovery strategy
     */
    public void errorRecoveryAttempt(String ruleName, String strategy) {
        if (logger.isInfoEnabled()) {
            LoggingContext.setRuleName(ruleName);
            LoggingContext.setRulePhase("recovery");
            logger.info(ERROR_RECOVERY, "Attempting error recovery for rule '{}' using strategy: {}", 
                       ruleName, strategy);
        }
    }

    /**
     * Log successful error recovery.
     * 
     * @param ruleName The rule name
     * @param strategy The recovery strategy used
     */
    public void errorRecoverySuccess(String ruleName, String strategy) {
        if (logger.isInfoEnabled()) {
            logger.info(ERROR_RECOVERY, "Error recovery successful for rule '{}' using strategy: {}", 
                       ruleName, strategy);
        }
    }

    /**
     * Log failed error recovery.
     * 
     * @param ruleName The rule name
     * @param strategy The recovery strategy attempted
     * @param error The error that occurred during recovery
     */
    public void errorRecoveryFailed(String ruleName, String strategy, Throwable error) {
        if (logger.isWarnEnabled()) {
            logger.warn(ERROR_RECOVERY, "Error recovery failed for rule '{}' using strategy '{}': {}", 
                       ruleName, strategy, error.getMessage(), error);
        }
    }

    // Configuration Logging

    /**
     * Log configuration changes.
     * 
     * @param component The component being configured
     * @param message The configuration message
     */
    public void configuration(String component, String message) {
        if (logger.isInfoEnabled()) {
            logger.info(CONFIGURATION, "Configuration [{}]: {}", component, message);
        }
    }

    // Audit Logging

    /**
     * Log audit events.
     * 
     * @param operation The operation being performed
     * @param ruleName The rule name (optional)
     * @param message The audit message
     */
    public void audit(String operation, String ruleName, String message) {
        LoggingContext.auditLog(operation, ruleName, message);
    }

    // Standard Logging Methods with Performance Optimization

    /**
     * Log debug message with lazy evaluation.
     * 
     * @param messageSupplier Supplier for the log message
     */
    public void debug(Supplier<String> messageSupplier) {
        if (logger.isDebugEnabled()) {
            logger.debug(messageSupplier.get());
        }
    }

    /**
     * Log debug message.
     * 
     * @param message The message
     * @param args The arguments
     */
    public void debug(String message, Object... args) {
        if (logger.isDebugEnabled()) {
            logger.debug(message, args);
        }
    }

    /**
     * Log info message with lazy evaluation.
     * 
     * @param messageSupplier Supplier for the log message
     */
    public void info(Supplier<String> messageSupplier) {
        if (logger.isInfoEnabled()) {
            logger.info(messageSupplier.get());
        }
    }

    /**
     * Log info message.
     * 
     * @param message The message
     * @param args The arguments
     */
    public void info(String message, Object... args) {
        if (logger.isInfoEnabled()) {
            logger.info(message, args);
        }
    }

    /**
     * Log warning message.
     * 
     * @param message The message
     * @param args The arguments
     */
    public void warn(String message, Object... args) {
        if (logger.isWarnEnabled()) {
            logger.warn(message, args);
        }
    }

    /**
     * Log warning message with exception.
     * 
     * @param message The message
     * @param throwable The exception
     * @param args The arguments
     */
    public void warn(String message, Throwable throwable, Object... args) {
        if (logger.isWarnEnabled()) {
            logger.warn(String.format(message, args), throwable);
        }
    }

    /**
     * Log error message.
     * 
     * @param message The message
     * @param args The arguments
     */
    public void error(String message, Object... args) {
        if (logger.isErrorEnabled()) {
            logger.error(message, args);
        }
    }

    /**
     * Log error message with exception.
     * 
     * @param message The message
     * @param throwable The exception
     * @param args The arguments
     */
    public void error(String message, Throwable throwable, Object... args) {
        if (logger.isErrorEnabled()) {
            logger.error(String.format(message, args), throwable);
        }
    }

    // Utility methods for checking log levels

    public boolean isDebugEnabled() {
        return logger.isDebugEnabled();
    }

    public boolean isInfoEnabled() {
        return logger.isInfoEnabled();
    }

    public boolean isWarnEnabled() {
        return logger.isWarnEnabled();
    }

    public boolean isErrorEnabled() {
        return logger.isErrorEnabled();
    }
}
