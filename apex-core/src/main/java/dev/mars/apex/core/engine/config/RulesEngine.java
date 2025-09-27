package dev.mars.apex.core.engine.config;

import dev.mars.apex.core.config.error.ErrorRecoveryConfig;
import dev.mars.apex.core.config.yaml.YamlRuleConfiguration;
import dev.mars.apex.core.constants.SeverityConstants;
import dev.mars.apex.core.engine.model.Rule;
import dev.mars.apex.core.engine.model.RuleBase;
import dev.mars.apex.core.engine.model.RuleGroup;
import dev.mars.apex.core.engine.model.RuleGroupEvaluationResult;
import dev.mars.apex.core.engine.model.RuleResult;
import dev.mars.apex.core.exception.RuleEvaluationException;
import dev.mars.apex.core.service.enrichment.EnrichmentService;
import dev.mars.apex.core.service.error.ErrorRecoveryService;
import dev.mars.apex.core.service.monitoring.RulePerformanceMetrics;
import dev.mars.apex.core.service.monitoring.RulePerformanceMonitor;
import dev.mars.apex.core.service.engine.UnifiedRuleEvaluator;
import dev.mars.apex.core.util.LoggingContext;
import dev.mars.apex.core.util.RuleParameterExtractor;
import dev.mars.apex.core.util.RulesEngineLogger;
import dev.mars.apex.core.util.TestAwareLogger;
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
* This class is part of the APEX A powerful expression processor for Java applications.
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
    private final EnrichmentService enrichmentService;
    private final UnifiedRuleEvaluator unifiedEvaluator;


    /**
     * Create a new RulesEngine with the given configuration.
     *
     * @param configuration The configuration for this rules engine
     */
    public RulesEngine(RulesEngineConfiguration configuration) {
        this(configuration, new SpelExpressionParser(), new ErrorRecoveryService(), new RulePerformanceMonitor(), null);
    }

    /**
     * Create a new RulesEngine with the given configuration and expression parser.
     *
     * @param configuration The configuration for this rules engine
     * @param parser The expression parser to use
     */
    public RulesEngine(RulesEngineConfiguration configuration, ExpressionParser parser) {
        this(configuration, parser, new ErrorRecoveryService(), new RulePerformanceMonitor(), null);
    }

    /**
     * Create a new RulesEngine with the given configuration, expression parser, and error recovery service.
     *
     * @param configuration The configuration for this rules engine
     * @param parser The expression parser to use
     * @param errorRecoveryService The error recovery service to use for handling evaluation errors
     */
    public RulesEngine(RulesEngineConfiguration configuration, ExpressionParser parser, ErrorRecoveryService errorRecoveryService) {
        this(configuration, parser, errorRecoveryService, new RulePerformanceMonitor(), null);
    }

    /**
     * Create a new RulesEngine with the given configuration, expression parser, error recovery service, and performance monitor.
     *
     * @param configuration The configuration for this rules engine
     * @param parser The expression parser to use
     * @param errorRecoveryService The error recovery service to use for handling evaluation errors
     * @param performanceMonitor The performance monitor to use for tracking rule evaluation metrics
     * @param enrichmentService The enrichment service to use for processing enrichments (optional)
     */
    public RulesEngine(RulesEngineConfiguration configuration, ExpressionParser parser,
                      ErrorRecoveryService errorRecoveryService, RulePerformanceMonitor performanceMonitor,
                      EnrichmentService enrichmentService) {
        this(configuration, parser, errorRecoveryService, performanceMonitor, enrichmentService, new ErrorRecoveryConfig());
    }

    /**
     * Create a new RulesEngine with the given configuration, expression parser, error recovery service, performance monitor, and custom error recovery config.
     *
     * @param configuration The configuration for this rules engine
     * @param parser The expression parser to use
     * @param errorRecoveryService The error recovery service to use for handling evaluation errors
     * @param performanceMonitor The performance monitor to use for tracking rule evaluation metrics
     * @param enrichmentService The enrichment service to use for processing enrichments (optional)
     * @param errorRecoveryConfig The error recovery configuration to use
     */
    public RulesEngine(RulesEngineConfiguration configuration, ExpressionParser parser,
                      ErrorRecoveryService errorRecoveryService, RulePerformanceMonitor performanceMonitor,
                      EnrichmentService enrichmentService, ErrorRecoveryConfig errorRecoveryConfig) {
        this.configuration = configuration;
        this.parser = parser;
        this.errorRecoveryService = errorRecoveryService;
        this.performanceMonitor = performanceMonitor;
        this.enrichmentService = enrichmentService;

        // Initialize the unified evaluator with the provided error recovery configuration
        this.unifiedEvaluator = new UnifiedRuleEvaluator(parser, errorRecoveryService, performanceMonitor, errorRecoveryConfig);

        // Initialize logging context
        LoggingContext.initializeContext();

        logger.configuration("RulesEngine", "Initialized with configuration: " + configuration.getClass().getSimpleName());
        logger.debug("Using parser: {}", parser.getClass().getSimpleName());
        logger.debug("Using error recovery service: {}", errorRecoveryService.getClass().getSimpleName());
        logger.debug("Using performance monitor: {}", performanceMonitor.getClass().getSimpleName());
        logger.debug("Using enrichment service: {}", enrichmentService != null ? enrichmentService.getClass().getSimpleName() : "none");
        logger.debug("Using error recovery config: enabled={}", errorRecoveryConfig.isEnabled());
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

        // Add custom property accessor for Maps
        context.addPropertyAccessor(new MapPropertyAccessor());

        // Add all facts to the evaluation context
        if (facts != null) {
            logger.debug("Adding {} facts to context", facts.size());

            // Set the facts map as the root object so properties can be accessed directly
            context.setRootObject(facts);

            // Also add facts as variables for backward compatibility (accessed with #variableName)
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
        // Delegate to the unified evaluator for consistent behavior
        return unifiedEvaluator.evaluateRule(rule, facts);
    }

    /**
     * Execute a list of Rule objects against the provided facts.
     *
     * @param rules The list of Rule objects to execute
     * @param facts The facts to evaluate the rules against
     * @return The result of the first rule that matches, or a default result if no rules match
     */
    public RuleResult executeRulesList(List<Rule> rules, Map<String, Object> facts) {
        // Delegate to the unified evaluator for consistent behavior
        return unifiedEvaluator.evaluateRules(rules, facts);
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

        // Track the highest severity from failed rule groups
        String highestFailedSeverity = SeverityConstants.INFO;
        String lastFailedGroupName = null;
        String lastFailedGroupMessage = null;

        // Evaluate rule groups in priority order
        for (RuleGroup group : ruleGroups) {
            logger.debug("Evaluating rule group: {}", group.getName());
            try {
                // Use detailed evaluation to get severity aggregation
                RuleGroupEvaluationResult evaluationResult = group.evaluateWithDetails(context);
                boolean result = evaluationResult.isGroupResult();
                String aggregatedSeverity = evaluationResult.getAggregatedSeverity();

                logger.debug("Rule group '{}' evaluated to: {} with aggregated severity: {}",
                           group.getName(), result, aggregatedSeverity);

                if (result) {
                    logger.info("Rule group matched: {}", group.getName());
                    return RuleResult.match(group.getName(), group.getMessage(), aggregatedSeverity);
                } else {
                    // Track failed group with highest severity
                    if (getSeverityPriority(aggregatedSeverity) > getSeverityPriority(highestFailedSeverity)) {
                        highestFailedSeverity = aggregatedSeverity;
                        lastFailedGroupName = group.getName();
                        lastFailedGroupMessage = group.getMessage();
                    }
                }
            } catch (Exception e) {
                logger.info("Rule group evaluation issue for '{}': {}", group.getName(), e.getMessage());
                logger.debug("Full exception details for rule group '{}':", group.getName(), e);
            }
        }

        logger.info("No rule groups matched");

        // Return result with highest severity from failed groups
        if (lastFailedGroupName != null) {
            return RuleResult.noMatch(lastFailedGroupName, lastFailedGroupMessage, highestFailedSeverity);
        } else {
            return RuleResult.noMatch();
        }
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
                        return RuleResult.match(rule.getName(), rule.getMessage(), rule.getSeverity());
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
                String errorMessage = String.format("Rule evaluation failed: %s", e.getMessage());

                // Get severity from rule configuration
                String severity = "ERROR"; // Default severity for evaluation errors
                if (ruleObj instanceof Rule) {
                    Rule rule = (Rule) ruleObj;
                    severity = rule.getSeverity() != null ? rule.getSeverity() : "ERROR";
                }

                // Log error details at appropriate level based on severity
                if ("CRITICAL".equalsIgnoreCase(severity)) {
                    logger.error("CRITICAL rule evaluation error for '{}': {}", ruleName, e.getMessage());
                } else if ("WARNING".equalsIgnoreCase(severity)) {
                    logger.info("Rule evaluation warning for '{}': {}", ruleName, e.getMessage());
                } else {
                    logger.info("Rule evaluation error for '{}': {}", ruleName, e.getMessage());
                }

                // Always log full exception details at DEBUG level for troubleshooting
                logger.debug("Full exception details for rule/rule group '{}':", ruleName, e);

                return RuleResult.error(ruleName, errorMessage, severity);
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

    /**
     * Unified evaluation method that processes both enrichments and rules, returning comprehensive results.
     * This method provides the complete APEX evaluation workflow with enrichment processing followed by rule evaluation.
     *
     * @param yamlConfig The YAML configuration containing enrichments and rules
     * @param inputData The input data to process
     * @return A comprehensive RuleResult containing success status, enriched data, and failure messages
     */
    public RuleResult evaluate(YamlRuleConfiguration yamlConfig, Map<String, Object> inputData) {
        logger.info("Starting unified evaluation with enrichments and rules");

        // Handle null inputs gracefully
        if (yamlConfig == null) {
            logger.warn("YAML configuration is null");
            List<String> failureMessages = new ArrayList<>();
            failureMessages.add("YAML configuration is null");
            Map<String, Object> enrichedData = inputData != null ? new HashMap<>(inputData) : new HashMap<>();
            return RuleResult.evaluationFailure(failureMessages, enrichedData, "evaluation", "Null YAML configuration");
        }

        if (inputData == null) {
            logger.warn("Input data is null");
            List<String> failureMessages = new ArrayList<>();
            failureMessages.add("Input data is null");
            return RuleResult.evaluationFailure(failureMessages, new HashMap<>(), "evaluation", "Null input data");
        }

        List<String> failureMessages = new ArrayList<>();
        Map<String, Object> enrichedData = new HashMap<>(inputData);
        boolean overallSuccess = true;

        try {
            // Phase 1: Process enrichments if available and EnrichmentService is configured
            if (enrichmentService != null && yamlConfig.getEnrichments() != null && !yamlConfig.getEnrichments().isEmpty()) {
                logger.info("Processing {} enrichments", yamlConfig.getEnrichments().size());

                try {
                    // Store original data size to detect enrichment failures
                    int originalDataSize = enrichedData.size();

                    Object enrichmentResult = enrichmentService.enrichObject(yamlConfig, enrichedData);

                    if (enrichmentResult instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> enrichmentMap = (Map<String, Object>) enrichmentResult;

                        // Check for enrichment failures by detecting missing required fields
                        boolean enrichmentFailed = detectEnrichmentFailures(yamlConfig, enrichmentMap, originalDataSize);

                        if (enrichmentFailed) {
                            overallSuccess = false;
                            failureMessages.add("Required field enrichment failed - check logs for CRITICAL ERROR details");
                            logger.warn("Enrichment failed due to required field mapping failures");
                        }

                        enrichedData = enrichmentMap;
                        logger.debug("Enrichment completed successfully, enriched data size: {}", enrichedData.size());
                    } else {
                        logger.warn("Enrichment result is not a Map, using original data");
                        overallSuccess = false;
                        failureMessages.add("Enrichment result format is invalid");
                    }
                } catch (Exception e) {
                    logger.info("Enrichment processing issue: {}", e.getMessage());
                    logger.debug("Full enrichment exception details:", e);
                    overallSuccess = false;
                    failureMessages.add("Enrichment processing failed: " + e.getMessage());
                }
            } else if (yamlConfig.getEnrichments() != null && !yamlConfig.getEnrichments().isEmpty()) {
                logger.warn("Enrichments defined in configuration but no EnrichmentService available");
                overallSuccess = false;
                failureMessages.add("Enrichments defined but no EnrichmentService configured");
            }

            // Phase 2: Process individual rules if available
            List<Rule> allRules = configuration.getAllRules();
            if (allRules != null && !allRules.isEmpty()) {
                logger.info("Processing {} individual rules", allRules.size());
                RuleResult ruleResult = executeRulesList(allRules, enrichedData);

                if (ruleResult.getResultType() == RuleResult.ResultType.ERROR) {
                    overallSuccess = false;
                    failureMessages.add("Rule evaluation error: " + ruleResult.getMessage());
                }
            }

            // Phase 3: Process rule groups if available
            List<RuleGroup> allRuleGroups = configuration.getAllRuleGroups();
            if (allRuleGroups != null && !allRuleGroups.isEmpty()) {
                logger.info("Processing {} rule groups", allRuleGroups.size());
                RuleResult ruleGroupResult = executeRuleGroupsList(allRuleGroups, enrichedData);

                if (ruleGroupResult.getResultType() == RuleResult.ResultType.ERROR) {
                    overallSuccess = false;
                    failureMessages.add("Rule group evaluation error: " + ruleGroupResult.getMessage());
                }
            }

            // Return comprehensive result
            if (overallSuccess && failureMessages.isEmpty()) {
                logger.info("Unified evaluation completed successfully");
                return RuleResult.evaluationSuccess(enrichedData, "evaluation", "Evaluation completed successfully");
            } else {
                logger.info("Unified evaluation completed with {} failures", failureMessages.size());
                return RuleResult.evaluationFailure(failureMessages, enrichedData, "evaluation", "Evaluation completed with failures");
            }

        } catch (Exception e) {
            logger.error("Unified evaluation failed with exception: {}", e.getMessage());
            logger.debug("Full unified evaluation exception details:", e);
            failureMessages.add("Evaluation failed: " + e.getMessage());
            return RuleResult.evaluationFailure(failureMessages, enrichedData, "evaluation", "Evaluation failed");
        }
    }

    /**
     * Detect enrichment failures by checking if required fields were successfully enriched.
     * This method examines the enrichment configuration and checks if required target fields
     * are present in the enriched data.
     */
    private boolean detectEnrichmentFailures(YamlRuleConfiguration yamlConfig, Map<String, Object> enrichedData, int originalDataSize) {
        if (yamlConfig.getEnrichments() == null || yamlConfig.getEnrichments().isEmpty()) {
            return false;
        }

        boolean hasFailures = false;

        for (var enrichment : yamlConfig.getEnrichments()) {
            if (enrichment.getFieldMappings() != null) {
                for (var mapping : enrichment.getFieldMappings()) {
                    // Check if this is a required field mapping
                    if (mapping.getRequired() != null && mapping.getRequired()) {
                        String targetField = mapping.getTargetField();

                        // Check if the required target field is missing or null in enriched data
                        if (!enrichedData.containsKey(targetField) || enrichedData.get(targetField) == null) {
                            logger.debug("Required field '{}' is missing from enriched data", targetField);
                            hasFailures = true;
                        }
                    }
                }
            }
        }

        return hasFailures;
    }

    /**
     * Simplified unified evaluation method that processes both enrichments and rules.
     * This is a convenience method for when you only have input data and want complete processing.
     *
     * @param inputData The input data to process
     * @return A comprehensive RuleResult containing success status, enriched data, and failure messages
     */
    public RuleResult evaluate(Map<String, Object> inputData) {
        // This method requires that the RulesEngine was created with a YamlRuleConfiguration
        // We'll need to extract it from the configuration or require it to be passed
        logger.warn("evaluate(Map) method called but requires YamlRuleConfiguration - use evaluate(YamlRuleConfiguration, Map) instead");

        // For now, return a basic result indicating this method needs the YAML config
        List<String> failureMessages = new ArrayList<>();
        failureMessages.add("evaluate(Map) method requires YamlRuleConfiguration parameter");
        return RuleResult.evaluationFailure(failureMessages, inputData, "evaluation", "Missing YAML configuration");
    }

    /**
     * Get the priority value for a severity level.
     * Higher values indicate higher severity.
     *
     * @param severity The severity level (ERROR, WARNING, INFO)
     * @return The priority value (3 for ERROR, 2 for WARNING, 1 for INFO)
     */
    private int getSeverityPriority(String severity) {
        return SeverityConstants.getSeverityPriority(severity);
    }
}
