package dev.mars.rulesengine.core.service.error;

import dev.mars.rulesengine.core.engine.model.RuleResult;
import dev.mars.rulesengine.core.exception.RuleEvaluationException;
import dev.mars.rulesengine.core.exception.RuleEngineException;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;

import java.util.Map;
import java.util.logging.Logger;
import java.util.logging.Level;

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
 * Service for handling error recovery in rule evaluation.
 *
 * This enum is part of the PeeGeeQ message queue system, providing
 * production-ready PostgreSQL-based message queuing capabilities.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-07-27
 * @version 1.0
 */
public class ErrorRecoveryService {
    private static final Logger LOGGER = Logger.getLogger(ErrorRecoveryService.class.getName());
    private static final ExpressionParser parser = new SpelExpressionParser();
    
    private final ErrorRecoveryStrategy defaultStrategy;
    
    public ErrorRecoveryService() {
        this.defaultStrategy = ErrorRecoveryStrategy.CONTINUE_WITH_DEFAULT;
    }
    
    public ErrorRecoveryService(ErrorRecoveryStrategy defaultStrategy) {
        this.defaultStrategy = defaultStrategy;
    }
    
    /**
     * Attempt to recover from a rule evaluation error.
     * 
     * @param ruleName The name of the rule that failed
     * @param expression The expression that caused the error
     * @param context The evaluation context
     * @param originalException The original exception
     * @param strategy The recovery strategy to use
     * @return A recovery result indicating success or failure
     */
    public RecoveryResult attemptRecovery(String ruleName, String expression, 
                                        EvaluationContext context, Exception originalException,
                                        ErrorRecoveryStrategy strategy) {
        LOGGER.info("Attempting error recovery for rule: " + ruleName);
        
        try {
            switch (strategy) {
                case CONTINUE_WITH_DEFAULT:
                    return handleContinueWithDefault(ruleName, originalException);
                    
                case RETRY_WITH_SAFE_EXPRESSION:
                    return handleRetryWithSafeExpression(ruleName, expression, context, originalException);
                    
                case SKIP_RULE:
                    return handleSkipRule(ruleName, originalException);
                    
                case FAIL_FAST:
                    return handleFailFast(ruleName, originalException);
                    
                default:
                    return handleContinueWithDefault(ruleName, originalException);
            }
        } catch (Exception recoveryException) {
            LOGGER.log(Level.WARNING, "Error recovery failed for rule: " + ruleName, recoveryException);
            return RecoveryResult.failed(ruleName, "Recovery attempt failed: " + recoveryException.getMessage());
        }
    }
    
    /**
     * Attempt recovery using the default strategy.
     */
    public RecoveryResult attemptRecovery(String ruleName, String expression, 
                                        EvaluationContext context, Exception originalException) {
        return attemptRecovery(ruleName, expression, context, originalException, defaultStrategy);
    }
    
    private RecoveryResult handleContinueWithDefault(String ruleName, Exception originalException) {
        LOGGER.info("Using default recovery for rule: " + ruleName);
        RuleResult defaultResult = RuleResult.noMatch();
        return RecoveryResult.recovered(ruleName, defaultResult, "Continued with default result");
    }
    
    private RecoveryResult handleRetryWithSafeExpression(String ruleName, String expression, 
                                                       EvaluationContext context, Exception originalException) {
        LOGGER.info("Attempting safe expression retry for rule: " + ruleName);
        
        // Try to create a safer version of the expression
        String safeExpression = createSafeExpression(expression);
        if (safeExpression.equals(expression)) {
            // No safe alternative found
            return RecoveryResult.failed(ruleName, "No safe expression alternative available");
        }
        
        try {
            Expression exp = parser.parseExpression(safeExpression);
            Boolean result = exp.getValue(context, Boolean.class);
            
            if (result != null && result) {
                RuleResult ruleResult = RuleResult.match(ruleName, "Rule matched with safe expression");
                return RecoveryResult.recovered(ruleName, ruleResult, "Recovered using safe expression: " + safeExpression);
            } else {
                RuleResult ruleResult = RuleResult.noMatch();
                return RecoveryResult.recovered(ruleName, ruleResult, "Safe expression evaluated to false");
            }
        } catch (Exception e) {
            return RecoveryResult.failed(ruleName, "Safe expression also failed: " + e.getMessage());
        }
    }
    
    private RecoveryResult handleSkipRule(String ruleName, Exception originalException) {
        LOGGER.info("Skipping rule due to error: " + ruleName);
        return RecoveryResult.skipped(ruleName, "Rule skipped due to evaluation error");
    }
    
    private RecoveryResult handleFailFast(String ruleName, Exception originalException) {
        LOGGER.severe("Failing fast for rule: " + ruleName);
        return RecoveryResult.failed(ruleName, "Fail-fast strategy: " + originalException.getMessage());
    }
    
    /**
     * Create a safer version of an expression by adding null checks and safe navigation.
     */
    private String createSafeExpression(String expression) {
        if (expression == null || expression.trim().isEmpty()) {
            return "false";
        }

        // Simple safety transformations
        String safeExpression = expression;

        // Replace direct property access with safe navigation (fix regex)
        safeExpression = safeExpression.replaceAll("\\.(\\w+)", "?.$1");

        // Wrap in null check if it doesn't already have one
        if (!safeExpression.contains("!=") && !safeExpression.contains("==") && !safeExpression.contains("?")) {
            safeExpression = "(" + safeExpression + ") != null && (" + safeExpression + ")";
        }

        return safeExpression;
    }
    
    /**
     * Error recovery strategies.
     */
    public enum ErrorRecoveryStrategy {
        CONTINUE_WITH_DEFAULT,  // Continue execution with a default result
        RETRY_WITH_SAFE_EXPRESSION,  // Try to create a safer version of the expression
        SKIP_RULE,  // Skip the problematic rule
        FAIL_FAST   // Stop execution immediately
    }
    
    /**
     * Result of an error recovery attempt.
     */
    public static class RecoveryResult {
        private final String ruleName;
        private final boolean successful;
        private final RuleResult ruleResult;
        private final String recoveryMessage;
        private final RecoveryAction action;
        
        private RecoveryResult(String ruleName, boolean successful, RuleResult ruleResult, 
                             String recoveryMessage, RecoveryAction action) {
            this.ruleName = ruleName;
            this.successful = successful;
            this.ruleResult = ruleResult;
            this.recoveryMessage = recoveryMessage;
            this.action = action;
        }
        
        public static RecoveryResult recovered(String ruleName, RuleResult result, String message) {
            return new RecoveryResult(ruleName, true, result, message, RecoveryAction.RECOVERED);
        }
        
        public static RecoveryResult skipped(String ruleName, String message) {
            return new RecoveryResult(ruleName, true, RuleResult.noMatch(), message, RecoveryAction.SKIPPED);
        }
        
        public static RecoveryResult failed(String ruleName, String message) {
            return new RecoveryResult(ruleName, false, RuleResult.error(ruleName, message), message, RecoveryAction.FAILED);
        }
        
        public String getRuleName() { return ruleName; }
        public boolean isSuccessful() { return successful; }
        public RuleResult getRuleResult() { return ruleResult; }
        public String getRecoveryMessage() { return recoveryMessage; }
        public RecoveryAction getAction() { return action; }
        
        public enum RecoveryAction {
            RECOVERED, SKIPPED, FAILED
        }
    }
}
