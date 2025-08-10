package dev.mars.apex.core.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/*
 * Copyright 2025 Mark Andrew Ray-Smith Cityline Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * Utility class for managing logging context and structured logging.
 *
 * This class is part of the PeeGeeQ message queue system, providing
 * production-ready PostgreSQL-based message queuing capabilities.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-07-27
 * @version 1.0
 */
/**
 * Utility class for managing logging context and structured logging.
 * This class provides methods for setting up MDC (Mapped Diagnostic Context)
 * and managing correlation IDs for better log traceability.
 */
public class LoggingContext {

    // MDC Keys
    public static final String CORRELATION_ID = "correlationId";
    public static final String RULE_NAME = "ruleName";
    public static final String RULE_PHASE = "rulePhase";
    public static final String EVALUATION_TIME = "evaluationTime";
    public static final String USER_ID = "userId";
    public static final String OPERATION = "operation";
    
    // Thread-local storage for context data
    private static final ThreadLocal<Map<String, String>> contextData = 
        ThreadLocal.withInitial(ConcurrentHashMap::new);

    /**
     * Initialize a new logging context with a correlation ID.
     * 
     * @return The generated correlation ID
     */
    public static String initializeContext() {
        String correlationId = UUID.randomUUID().toString();
        setCorrelationId(correlationId);
        return correlationId;
    }

    /**
     * Initialize a logging context with a specific correlation ID.
     * 
     * @param correlationId The correlation ID to use
     */
    public static void initializeContext(String correlationId) {
        setCorrelationId(correlationId);
    }

    /**
     * Set the correlation ID for the current thread.
     * 
     * @param correlationId The correlation ID
     */
    public static void setCorrelationId(String correlationId) {
        MDC.put(CORRELATION_ID, correlationId);
        contextData.get().put(CORRELATION_ID, correlationId);
    }

    /**
     * Set the rule name for the current thread.
     * 
     * @param ruleName The name of the rule being processed
     */
    public static void setRuleName(String ruleName) {
        MDC.put(RULE_NAME, ruleName);
        contextData.get().put(RULE_NAME, ruleName);
    }

    /**
     * Set the rule phase for the current thread.
     * 
     * @param phase The current phase of rule processing (e.g., "parsing", "evaluation", "recovery")
     */
    public static void setRulePhase(String phase) {
        MDC.put(RULE_PHASE, phase);
        contextData.get().put(RULE_PHASE, phase);
    }

    /**
     * Set the evaluation time for the current thread.
     * 
     * @param evaluationTimeMs The evaluation time in milliseconds
     */
    public static void setEvaluationTime(double evaluationTimeMs) {
        String timeStr = String.format("%.2f", evaluationTimeMs);
        MDC.put(EVALUATION_TIME, timeStr);
        contextData.get().put(EVALUATION_TIME, timeStr);
    }

    /**
     * Set the user ID for audit logging.
     * 
     * @param userId The user ID
     */
    public static void setUserId(String userId) {
        MDC.put(USER_ID, userId);
        contextData.get().put(USER_ID, userId);
    }

    /**
     * Set the operation for audit logging.
     * 
     * @param operation The operation being performed
     */
    public static void setOperation(String operation) {
        MDC.put(OPERATION, operation);
        contextData.get().put(OPERATION, operation);
    }

    /**
     * Get the current correlation ID.
     * 
     * @return The correlation ID or null if not set
     */
    public static String getCorrelationId() {
        return MDC.get(CORRELATION_ID);
    }

    /**
     * Get the current rule name.
     * 
     * @return The rule name or null if not set
     */
    public static String getRuleName() {
        return MDC.get(RULE_NAME);
    }

    /**
     * Get all context data for the current thread.
     * 
     * @return A copy of the context data
     */
    public static Map<String, String> getContextData() {
        return Map.copyOf(contextData.get());
    }

    /**
     * Clear a specific MDC key.
     * 
     * @param key The key to clear
     */
    public static void clearKey(String key) {
        MDC.remove(key);
        contextData.get().remove(key);
    }

    /**
     * Clear the rule-specific context (rule name, phase, evaluation time).
     * Keeps correlation ID and user context.
     */
    public static void clearRuleContext() {
        clearKey(RULE_NAME);
        clearKey(RULE_PHASE);
        clearKey(EVALUATION_TIME);
    }

    /**
     * Clear all logging context for the current thread.
     */
    public static void clearContext() {
        MDC.clear();
        contextData.get().clear();
    }

    /**
     * Execute a runnable with a specific rule context.
     * 
     * @param ruleName The rule name
     * @param phase The rule phase
     * @param runnable The code to execute
     */
    public static void withRuleContext(String ruleName, String phase, Runnable runnable) {
        String previousRuleName = getRuleName();
        String previousPhase = MDC.get(RULE_PHASE);
        
        try {
            setRuleName(ruleName);
            setRulePhase(phase);
            runnable.run();
        } finally {
            if (previousRuleName != null) {
                setRuleName(previousRuleName);
            } else {
                clearKey(RULE_NAME);
            }
            
            if (previousPhase != null) {
                setRulePhase(previousPhase);
            } else {
                clearKey(RULE_PHASE);
            }
        }
    }

    /**
     * Execute a runnable with a specific correlation ID.
     * 
     * @param correlationId The correlation ID
     * @param runnable The code to execute
     */
    public static void withCorrelationId(String correlationId, Runnable runnable) {
        String previousCorrelationId = getCorrelationId();
        
        try {
            setCorrelationId(correlationId);
            runnable.run();
        } finally {
            if (previousCorrelationId != null) {
                setCorrelationId(previousCorrelationId);
            } else {
                clearKey(CORRELATION_ID);
            }
        }
    }

    /**
     * Create an audit logger for the rules engine.
     * 
     * @return An audit logger
     */
    public static Logger getAuditLogger() {
        return LoggerFactory.getLogger("dev.mars.apex.audit");
    }

    /**
     * Log an audit event.
     * 
     * @param operation The operation being performed
     * @param ruleName The rule name (optional)
     * @param message The audit message
     */
    public static void auditLog(String operation, String ruleName, String message) {
        Logger auditLogger = getAuditLogger();
        String previousOperation = MDC.get(OPERATION);
        String previousRuleName = getRuleName();
        
        try {
            setOperation(operation);
            if (ruleName != null) {
                setRuleName(ruleName);
            }
            auditLogger.info(message);
        } finally {
            if (previousOperation != null) {
                setOperation(previousOperation);
            } else {
                clearKey(OPERATION);
            }
            
            if (previousRuleName != null) {
                setRuleName(previousRuleName);
            } else if (ruleName != null) {
                clearKey(RULE_NAME);
            }
        }
    }
}
