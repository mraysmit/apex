package dev.mars.rulesengine.core.engine.config;

import dev.mars.rulesengine.core.engine.model.Rule;
import dev.mars.rulesengine.core.engine.model.RuleBase;
import dev.mars.rulesengine.core.engine.model.RuleGroup;
import dev.mars.rulesengine.core.engine.model.RuleResult;
import dev.mars.rulesengine.core.exception.RuleEvaluationException;
import dev.mars.rulesengine.core.service.error.ErrorRecoveryService;
import dev.mars.rulesengine.core.service.monitoring.RulePerformanceMetrics;
import dev.mars.rulesengine.core.service.monitoring.RulePerformanceMonitor;
import dev.mars.rulesengine.core.util.LoggingContext;
import dev.mars.rulesengine.core.util.RuleParameterExtractor;
import dev.mars.rulesengine.core.util.RulesEngineLogger;
import dev.mars.rulesengine.core.util.TestAwareLogger;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.util.*;

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
 * This class implements a business rules engine using SpEL.
 *
 * This class is part of the PeeGeeQ message queue system, providing
 * production-ready PostgreSQL-based message queuing capabilities.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-07-27
 * @version 1.0
 */
/**
 * This class implements a business rules engine using SpEL.
 * It provides a flexible, configurable rules system that can be easily extended
 * and modified without changing the core code.
 * 
 * This class is responsible only for rule evaluation, not configuration.
 * Configuration is handled by the RulesEngineConfiguration class.
 */
public class RulesEngine {
    private static final RulesEngineLogger logger = new RulesEngineLogger(RulesEngine.class);
    private final ExpressionParser parser;
    private final RulesEngineConfiguration configuration;
    private final ErrorRecoveryService errorRecoveryService;
    private final RulePerformanceMonitor performanceMonitor;


    /**
     * Create a new RulesEngine with the given configuration.
     *
     * @param configuration The configuration for this rules engine
     */
    public RulesEngine(RulesEngineConfiguration configuration) {
        this(configuration, new SpelExpressionParser(), new ErrorRecoveryService(), new RulePerformanceMonitor());
    }

    /**
     * Create a new RulesEngine with the given configuration and expression parser.
     *
     * @param configuration The configuration for this rules engine
     * @param parser The expression parser to use
     */
    public RulesEngine(RulesEngineConfiguration configuration, ExpressionParser parser) {
        this(configuration, parser, new ErrorRecoveryService(), new RulePerformanceMonitor());
    }

    /**
     * Create a new RulesEngine with the given configuration, expression parser, and error recovery service.
     *
     * @param configuration The configuration for this rules engine
     * @param parser The expression parser to use
     * @param errorRecoveryService The error recovery service to use for handling evaluation errors
     */
    public RulesEngine(RulesEngineConfiguration configuration, ExpressionParser parser, ErrorRecoveryService errorRecoveryService) {
        this(configuration, parser, errorRecoveryService, new RulePerformanceMonitor());
    }

    /**
     * Create a new RulesEngine with the given configuration, expression parser, error recovery service, and performance monitor.
     *
     * @param configuration The configuration for this rules engine
     * @param parser The expression parser to use
     * @param errorRecoveryService The error recovery service to use for handling evaluation errors
     * @param performanceMonitor The performance monitor to use for tracking rule evaluation metrics
     */
    public RulesEngine(RulesEngineConfiguration configuration, ExpressionParser parser,
                      ErrorRecoveryService errorRecoveryService, RulePerformanceMonitor performanceMonitor) {
        this.configuration = configuration;
        this.parser = parser;
        this.errorRecoveryService = errorRecoveryService;
        this.performanceMonitor = performanceMonitor;

        // Initialize logging context
        LoggingContext.initializeContext();

        logger.configuration("RulesEngine", "Initialized with configuration: " + configuration.getClass().getSimpleName());
        logger.debug("Using parser: {}", parser.getClass().getSimpleName());
        logger.debug("Using error recovery service: {}", errorRecoveryService.getClass().getSimpleName());
        logger.debug("Using performance monitor: {}", performanceMonitor.getClass().getSimpleName());
    }

    /**
     * Get the configuration for this rules engine.
     *
     * @return The configuration for this rules engine
     */
    public RulesEngineConfiguration getConfiguration() {
        return configuration;
    }

    /**
     * Get the performance monitor for this rules engine.
     *
     * @return The performance monitor
     */
    public RulePerformanceMonitor getPerformanceMonitor() {
        return performanceMonitor;
    }

    // Rule Execution Methods

    /**
     * Create an evaluation context with the provided facts.
     *
     * @param facts The facts to add to the context
     * @return A new StandardEvaluationContext with the facts added as variables
     */
    private StandardEvaluationContext createContext(Map<String, Object> facts) {
        logger.debug("Creating evaluation context");
        StandardEvaluationContext context = new StandardEvaluationContext();

        // Add all facts to the evaluation context
        if (facts != null) {
            logger.debug("Adding {} facts to context", facts.size());
            for (Map.Entry<String, Object> fact : facts.entrySet()) {
                context.setVariable(fact.getKey(), fact.getValue());
                if (logger.isDebugEnabled()) {
                    logger.debug("Added fact: {} = {}", fact.getKey(),
                        fact.getValue() != null ? fact.getValue().getClass().getSimpleName() : "null");
                }
            }
        } else {
            logger.debug("No facts provided to context");
        }

        return context;
    }

    /**
     * Execute a single Rule object against the provided facts.
     *
     * @param rule The Rule object to execute
     * @param facts The facts to evaluate the rule against
     * @return The result of the rule evaluation, indicating whether it matched or not
     */
    public RuleResult executeRule(Rule rule, Map<String, Object> facts) {
        if (rule == null) {
            logger.info("No rule provided for execution");
            return RuleResult.noRules();
        }

        // Set up logging context for this rule evaluation
        LoggingContext.setRuleName(rule.getName());
        LoggingContext.setRulePhase("evaluation");

        logger.ruleEvaluationStart(rule.getName());
        logger.debug("Facts provided: {}", facts != null ? facts.keySet() : "none");

        // Start performance monitoring early to capture all scenarios
        RulePerformanceMetrics.Builder metricsBuilder = performanceMonitor.startEvaluation(rule.getName());

        // Check for missing parameters
        Set<String> missingParameters = RuleParameterExtractor.validateParameters(rule, facts);
        if (!missingParameters.isEmpty()) {
            TestAwareLogger.warn(logger, "Missing parameters for rule '{}': {}", rule.getName(), missingParameters);
            RulePerformanceMetrics metrics = performanceMonitor.completeEvaluation(metricsBuilder, rule.getCondition());
            LoggingContext.clearRuleContext();
            return RuleResult.error(rule.getName(), "Missing parameters: " + missingParameters, metrics);
        }

        StandardEvaluationContext context = createContext(facts);

        // Evaluate the rule
        try {
            Expression exp = parser.parseExpression(rule.getCondition());
            Boolean result = exp.getValue(context, Boolean.class);

            // Complete performance monitoring for successful evaluation
            RulePerformanceMetrics metrics = performanceMonitor.completeEvaluation(metricsBuilder, rule.getCondition());

            // Log the completion with performance metrics
            logger.ruleEvaluationComplete(rule.getName(), result != null && result, metrics.getEvaluationTimeMillis());

            if (result != null && result) {
                logger.audit("RULE_MATCH", rule.getName(), "Rule matched successfully");
                LoggingContext.clearRuleContext();
                return RuleResult.match(rule.getName(), rule.getMessage(), metrics);
            } else {
                LoggingContext.clearRuleContext();
                return RuleResult.noMatch(metrics);
            }
        } catch (Exception e) {
            logger.ruleEvaluationError(rule.getName(), e);

            // Create detailed exception with context and suggestions
            RuleEvaluationException ruleException = new RuleEvaluationException(
                rule.getName(),
                rule.getCondition(),
                e.getMessage(),
                e
            );

            // Complete performance monitoring for failed evaluation
            RulePerformanceMetrics metrics = performanceMonitor.completeEvaluation(metricsBuilder, rule.getCondition(), e);

            // Attempt error recovery
            logger.errorRecoveryAttempt(rule.getName(), "default");
            ErrorRecoveryService.RecoveryResult recoveryResult = errorRecoveryService.attemptRecovery(
                rule.getName(),
                rule.getCondition(),
                context,
                e
            );

            if (recoveryResult.isSuccessful()) {
                logger.errorRecoverySuccess(rule.getName(), "default");
                logger.audit("ERROR_RECOVERY", rule.getName(), "Error recovery successful: " + recoveryResult.getRecoveryMessage());

                // Preserve performance metrics in the recovered result
                RuleResult originalResult = recoveryResult.getRuleResult();
                LoggingContext.clearRuleContext();
                if (originalResult.hasPerformanceMetrics()) {
                    return originalResult;
                } else {
                    // Create a new result with the same properties but include performance metrics
                    return new RuleResult(originalResult.getRuleName(), originalResult.getMessage(),
                                        originalResult.isTriggered(), originalResult.getResultType(), metrics);
                }
            } else {
                logger.errorRecoveryFailed(rule.getName(), "default", new RuntimeException(recoveryResult.getRecoveryMessage()));
                logger.audit("ERROR_RECOVERY_FAILED", rule.getName(), "Error recovery failed: " + recoveryResult.getRecoveryMessage());
                LoggingContext.clearRuleContext();
                return RuleResult.error(rule.getName(), ruleException.getDetailedMessage(), metrics);
            }
        }
    }

    /**
     * Execute a list of Rule objects against the provided facts.
     *
     * @param rules The list of Rule objects to execute
     * @param facts The facts to evaluate the rules against
     * @return The result of the first rule that matches, or a default result if no rules match
     */
    public RuleResult executeRulesList(List<Rule> rules, Map<String, Object> facts) {
        if (rules == null || rules.isEmpty()) {
            logger.info("No rules provided for execution");
            return RuleResult.noRules();
        }

        logger.info("Executing {} rules", rules.size());
        logger.debug("Facts provided: {}", facts != null ? facts.keySet() : "none");

        StandardEvaluationContext context = createContext(facts);

        // Evaluate rules in priority order
        for (Rule rule : rules) {
            logger.debug("Evaluating rule: {}", rule.getName());
            try {
                Expression exp = parser.parseExpression(rule.getCondition());
                Boolean result = exp.getValue(context, Boolean.class);
                logger.debug("Rule '{}' evaluated to: {}", rule.getName(), result);

                if (result != null && result) {
                    logger.info("Rule matched: {}", rule.getName());
                    return RuleResult.match(rule.getName(), rule.getMessage());
                }
            } catch (Exception e) {
                logger.warn("Error evaluating rule '{}': {}", rule.getName(), e.getMessage(), e);
            }
        }

        logger.info("No rules matched");
        return RuleResult.noMatch();
    }

    /**
     * Execute a list of RuleGroup objects against the provided facts.
     *
     * @param ruleGroups The list of RuleGroup objects to execute
     * @param facts The facts to evaluate the rule groups against
     * @return The result of the first rule group that matches, or a default result if no rule groups match
     */
    public RuleResult executeRuleGroupsList(List<RuleGroup> ruleGroups, Map<String, Object> facts) {
        if (ruleGroups == null || ruleGroups.isEmpty()) {
            logger.info("No rule groups provided for execution");
            return RuleResult.noRules();
        }

        logger.info("Executing {} rule groups", ruleGroups.size());
        logger.debug("Facts provided: {}", facts != null ? facts.keySet() : "none");

        StandardEvaluationContext context = createContext(facts);

        // Evaluate rule groups in priority order
        for (RuleGroup group : ruleGroups) {
            logger.debug("Evaluating rule group: {}", group.getName());
            try {
                boolean result = group.evaluate(context);
                logger.debug("Rule group '{}' evaluated to: {}", group.getName(), result);

                if (result) {
                    logger.info("Rule group matched: {}", group.getName());
                    return RuleResult.match(group.getName(), group.getMessage());
                }
            } catch (Exception e) {
                logger.warn("Error evaluating rule group '{}': {}", group.getName(), e.getMessage(), e);
            }
        }

        logger.info("No rule groups matched");
        return RuleResult.noMatch();
    }

    /**
     * Execute a list of rules against the provided facts.
     * This method determines the type of objects in the list and delegates to the appropriate method.
     *
     * @param rules The list of rules to execute (can be a mix of Rule and RuleGroup objects)
     * @param facts The facts to evaluate the rules against
     * @return The result of the first rule that matches, or a default result if no rules match
     */
    public RuleResult executeRules(List<RuleBase> rules, Map<String, Object> facts) {
        if (rules == null || rules.isEmpty()) {
            logger.info("No rules provided for execution");
            return RuleResult.noRules();
        }

        logger.info("Executing {} rules/rule groups", rules.size());
        logger.debug("Facts provided: {}", facts != null ? facts.keySet() : "none");

        // Check if all rules are of the same type and delegate to the appropriate method
        boolean allRules = true;
        boolean allRuleGroups = true;

        for (RuleBase ruleObj : rules) {
            if (!(ruleObj instanceof Rule)) {
                allRules = false;
            }
            if (!(ruleObj instanceof RuleGroup)) {
                allRuleGroups = false;
            }
        }

        if (allRules) {
            // All objects are Rule instances, so we can safely cast and delegate
            logger.debug("All objects are Rule instances, delegating to executeRulesList");
            @SuppressWarnings("unchecked")
            List<Rule> rulesList = (List<Rule>) (List<?>) rules;
            return executeRulesList(rulesList, facts);
        } else if (allRuleGroups) {
            // All objects are RuleGroup instances, so we can safely cast and delegate
            logger.debug("All objects are RuleGroup instances, delegating to executeRuleGroupsList");
            @SuppressWarnings("unchecked")
            List<RuleGroup> ruleGroupsList = (List<RuleGroup>) (List<?>) rules;
            return executeRuleGroupsList(ruleGroupsList, facts);
        }

        logger.debug("Mixed list of rules and rule groups, processing manually");
        // Mixed list or unknown types, process manually
        StandardEvaluationContext context = createContext(facts);

        // Evaluate rules in priority order
        for (RuleBase ruleObj : rules) {
            logger.debug("Evaluating rule/rule group: {}", ruleObj.getName());
            try {
                if (ruleObj instanceof Rule) {
                    Rule rule = (Rule) ruleObj;
                    Expression exp = parser.parseExpression(rule.getCondition());
                    Boolean result = exp.getValue(context, Boolean.class);
                    logger.debug("Rule '{}' evaluated to: {}", rule.getName(), result);

                    if (result != null && result) {
                        logger.info("Rule matched: {}", rule.getName());
                        return RuleResult.match(rule.getName(), rule.getMessage());
                    }
                } else if (ruleObj instanceof RuleGroup) {
                    RuleGroup group = (RuleGroup) ruleObj;
                    boolean result = group.evaluate(context);
                    logger.debug("Rule group '{}' evaluated to: {}", group.getName(), result);

                    if (result) {
                        logger.info("Rule group matched: {}", group.getName());
                        return RuleResult.match(group.getName(), group.getMessage());
                    }
                }
            } catch (Exception e) {
                String ruleName = ruleObj.getName();
                logger.warn("Error evaluating rule/rule group '{}': {}", ruleName, e.getMessage(), e);
            }
        }

        logger.info("No rules or rule groups matched");
        return RuleResult.noMatch();
    }

    /**
     * Execute rules for a specific category against the provided facts.
     *
     * @param category The category of rules to execute
     * @param facts The facts to evaluate the rules against
     * @return The result of the first rule that matches, or a default result if no rules match
     */
    public RuleResult executeRulesForCategory(String category, Map<String, Object> facts) {
        logger.info("Executing rules for category: {}", category);
        List<RuleBase> rules = configuration.getRulesForCategory(category);
        logger.debug("Found {} rules/rule groups in category: {}", rules.size(), category);
        return executeRules(rules, facts);
    }

    /**
     * Simple evaluation method that returns only a boolean indicating whether a rule was triggered.
     * This method is provided for simplicity when only the boolean result is needed.
     *
     * @param rule The Rule object to evaluate
     * @param facts The facts to evaluate the rule against
     * @return true if the rule was triggered, false otherwise
     */
    public boolean evaluateRule(Rule rule, Map<String, Object> facts) {
        RuleResult result = executeRule(rule, facts);
        return result.isTriggered();
    }

    /**
     * Simple evaluation method that returns only a boolean indicating whether any rule in the list was triggered.
     * This method is provided for simplicity when only the boolean result is needed.
     *
     * @param rules The list of Rule objects to evaluate
     * @param facts The facts to evaluate the rules against
     * @return true if any rule was triggered, false otherwise
     */
    public boolean evaluateRules(List<RuleBase> rules, Map<String, Object> facts) {
        RuleResult result = executeRules(rules, facts);
        return result.isTriggered();
    }

    /**
     * Simple evaluation method that returns only a boolean indicating whether any rule in the specified category was triggered.
     * This method is provided for simplicity when only the boolean result is needed.
     *
     * @param category The category of rules to evaluate
     * @param facts The facts to evaluate the rules against
     * @return true if any rule was triggered, false otherwise
     */
    public boolean evaluateRulesForCategory(String category, Map<String, Object> facts) {
        RuleResult result = executeRulesForCategory(category, facts);
        return result.isTriggered();
    }
}
