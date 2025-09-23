package dev.mars.apex.core.engine.executor;

import dev.mars.apex.core.config.yaml.YamlRuleChain;
import dev.mars.apex.core.constants.SeverityConstants;
import dev.mars.apex.core.engine.context.ChainedEvaluationContext;
import dev.mars.apex.core.engine.model.Rule;
import dev.mars.apex.core.engine.model.RuleChainResult;
import dev.mars.apex.core.service.engine.ExpressionEvaluatorService;
import dev.mars.apex.core.service.engine.RuleEngineService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
 * Executor for Pattern 4: Accumulative Chaining.
 * Build up a score/result across multiple rules.
 *
 * YAML Configuration Example:
 * ```yaml
 * rule-chains:
 *   - id: "credit-scoring"
 *     pattern: "accumulative-chaining"
 *     configuration:
 *       accumulator-variable: "totalScore"
 *       initial-value: 0
 *       accumulation-rules:
 *         - id: "credit-score-component"
 *           condition: "#creditScore >= 700 ? 25 : (#creditScore >= 650 ? 15 : 10)"
 *           message: "Credit score component calculated"
 *           weight: 1.0
 *         - id: "income-component"
 *           condition: "#annualIncome >= 80000 ? 20 : (#annualIncome >= 60000 ? 15 : 10)"
 *           message: "Income component calculated"
 *           weight: 1.0
 *       final-decision-rule:
 *         id: "loan-decision"
 *         condition: "#totalScore >= 60 ? 'APPROVED' : (#totalScore >= 40 ? 'CONDITIONAL' : 'DENIED')"
 *         message: "Final loan decision"
 * ```
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-07-28
 * @version 1.0
 */
public class AccumulativeChainingExecutor extends PatternExecutor {

    public AccumulativeChainingExecutor(RuleEngineService ruleEngineService, ExpressionEvaluatorService evaluatorService) {
        super(ruleEngineService, evaluatorService);
    }

    @Override
    public RuleChainResult execute(YamlRuleChain ruleChain, Map<String, Object> configuration, ChainedEvaluationContext context) {
        logger.info("Executing accumulative chaining pattern for rule chain: " + ruleChain.getId());

        if (!validateConfiguration(configuration)) {
            return createValidationFailure(ruleChain, "Invalid accumulative chaining configuration");
        }

        RuleChainResult.Builder resultBuilder = RuleChainResult.builder(ruleChain.getId(), getPatternName())
                .ruleChainName(ruleChain.getName());

        try {
            // Initialize accumulator
            String accumulatorVariable = getStringValue(configuration, "accumulator-variable", "totalScore");
            Object initialValueObj = configuration.get("initial-value");
            Number initialValue = initialValueObj instanceof Number ? (Number) initialValueObj : 0;

            logger.info("Initializing accumulator '" + accumulatorVariable + "' with value: " + initialValue);

            // Set initial accumulator value in context
            context.addStageResult(accumulatorVariable, initialValue);
            context.setVariable(accumulatorVariable, initialValue);
            resultBuilder.addStageResult(accumulatorVariable + "_initial", initialValue);

            // Execute accumulation rules
            double currentScore = initialValue.doubleValue();
            currentScore = executeAccumulationRules(configuration, context, resultBuilder, accumulatorVariable, currentScore);

            // Update final accumulator value
            context.addStageResult(accumulatorVariable, currentScore);
            context.setVariable(accumulatorVariable, currentScore);
            resultBuilder.addStageResult(accumulatorVariable + "_final", currentScore);

            // Execute final decision rule if present
            String finalDecision = executeFinalDecisionRule(configuration, context, resultBuilder);

            resultBuilder.finalOutcome(finalDecision != null ? finalDecision : "ACCUMULATION_COMPLETED");
            resultBuilder.addStageResult("finalDecision", finalDecision);

            logger.info("Accumulative chaining completed with final score: " + currentScore + ", decision: " + finalDecision);
            return resultBuilder.successful(true).build();

        } catch (Exception e) {
            logger.severe("Error executing accumulative chaining: " + e.getMessage());
            return resultBuilder.errorMessage("Execution error: " + e.getMessage()).build();
        }
    }

    /**
     * Execute accumulation rules and build up the score.
     *
     * @param configuration The rule chain configuration
     * @param context The chained evaluation context
     * @param resultBuilder The result builder
     * @param accumulatorVariable The name of the accumulator variable
     * @param currentScore The current accumulated score
     * @return The updated accumulated score
     */
    private double executeAccumulationRules(Map<String, Object> configuration, ChainedEvaluationContext context,
                                          RuleChainResult.Builder resultBuilder, String accumulatorVariable,
                                          double currentScore) {
        List<Object> accumulationRules = getListValue(configuration, "accumulation-rules");
        if (accumulationRules == null || accumulationRules.isEmpty()) {
            logger.warning("No accumulation rules configured");
            return currentScore;
        }

        // Apply rule selection logic if configured
        List<Object> selectedRules = selectRulesForExecution(configuration, accumulationRules, context);

        logger.info("Total rules available: " + accumulationRules.size() +
                   ", Selected for execution: " + selectedRules.size());
        context.setCurrentStage("accumulation-rules-execution");

        // Store selection results for audit trail
        resultBuilder.addStageResult("total_rules_available", accumulationRules.size());
        resultBuilder.addStageResult("rules_selected_for_execution", selectedRules.size());
        context.addStageResult("total_rules_available", accumulationRules.size());
        context.addStageResult("rules_selected_for_execution", selectedRules.size());

        for (int i = 0; i < selectedRules.size(); i++) {
            Object ruleObj = selectedRules.get(i);
            if (!(ruleObj instanceof Map)) {
                logger.warning("Selected rule at index " + i + " is not a map, skipping");
                continue;
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> ruleConfig = (Map<String, Object>) ruleObj;

            currentScore = executeAccumulationRule(ruleConfig, i + 1, context, resultBuilder,
                                                 accumulatorVariable, currentScore);
        }

        return currentScore;
    }

    /**
     * Select rules for execution based on configured selection strategy.
     *
     * @param configuration The rule chain configuration
     * @param allRules All available accumulation rules
     * @param context The evaluation context
     * @return List of selected rules to execute
     */
    private List<Object> selectRulesForExecution(Map<String, Object> configuration, List<Object> allRules,
                                               ChainedEvaluationContext context) {
        // Check if rule selection is configured
        Map<String, Object> ruleSelection = getMapValue(configuration, "rule-selection");
        if (ruleSelection == null) {
            // No selection configured, return all rules (backward compatibility)
            logger.info("No rule selection configured, executing all " + allRules.size() + " rules");
            return new ArrayList<>(allRules);
        }

        String strategy = getStringValue(ruleSelection, "strategy", "all");
        logger.info("Applying rule selection strategy: " + strategy);

        switch (strategy.toLowerCase()) {
            case "weight-threshold":
                return selectRulesByWeightThreshold(ruleSelection, allRules);
            case "top-weighted":
                return selectTopWeightedRules(ruleSelection, allRules);
            case "priority-based":
                return selectRulesByPriority(ruleSelection, allRules);
            case "dynamic-threshold":
                return selectRulesByDynamicThreshold(ruleSelection, allRules, context);
            case "all":
            default:
                logger.info("Using default strategy 'all', executing all rules");
                return new ArrayList<>(allRules);
        }
    }

    /**
     * Select rules based on weight threshold.
     */
    private List<Object> selectRulesByWeightThreshold(Map<String, Object> ruleSelection, List<Object> allRules) {
        double threshold = getDoubleValue(ruleSelection, "weight-threshold", 0.0);
        logger.info("Selecting rules with weight >= " + threshold);

        List<Object> selectedRules = new ArrayList<>();
        for (Object ruleObj : allRules) {
            if (ruleObj instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> ruleConfig = (Map<String, Object>) ruleObj;
                double weight = getDoubleValue(ruleConfig, "weight", 1.0);

                if (weight >= threshold) {
                    selectedRules.add(ruleObj);
                    logger.info("Selected rule '" + getStringValue(ruleConfig, "id", "unnamed") +
                               "' with weight " + weight);
                } else {
                    logger.info("Skipped rule '" + getStringValue(ruleConfig, "id", "unnamed") +
                               "' with weight " + weight + " (below threshold " + threshold + ")");
                }
            }
        }

        return selectedRules;
    }

    /**
     * Select top N rules by weight.
     */
    private List<Object> selectTopWeightedRules(Map<String, Object> ruleSelection, List<Object> allRules) {
        int maxRules = getIntValue(ruleSelection, "max-rules", allRules.size());
        logger.info("Selecting top " + maxRules + " rules by weight");

        // Create list of rules with their weights for sorting
        List<RuleWithWeight> rulesWithWeights = new ArrayList<>();
        for (Object ruleObj : allRules) {
            if (ruleObj instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> ruleConfig = (Map<String, Object>) ruleObj;
                double weight = getDoubleValue(ruleConfig, "weight", 1.0);
                rulesWithWeights.add(new RuleWithWeight(ruleObj, weight));
            }
        }

        // Sort by weight (descending) and take top N
        rulesWithWeights.sort((a, b) -> Double.compare(b.weight, a.weight));

        List<Object> selectedRules = new ArrayList<>();
        for (int i = 0; i < Math.min(maxRules, rulesWithWeights.size()); i++) {
            RuleWithWeight ruleWithWeight = rulesWithWeights.get(i);
            selectedRules.add(ruleWithWeight.rule);

            @SuppressWarnings("unchecked")
            Map<String, Object> ruleConfig = (Map<String, Object>) ruleWithWeight.rule;
            logger.info("Selected rule '" + getStringValue(ruleConfig, "id", "unnamed") +
                       "' with weight " + ruleWithWeight.weight + " (rank " + (i + 1) + ")");
        }

        return selectedRules;
    }

    /**
     * Select rules based on priority levels.
     */
    private List<Object> selectRulesByPriority(Map<String, Object> ruleSelection, List<Object> allRules) {
        String minPriority = getStringValue(ruleSelection, "min-priority", "LOW");
        logger.info("Selecting rules with priority >= " + minPriority);

        // Define priority order: HIGH > MEDIUM > LOW
        Map<String, Integer> priorityOrder = Map.of(
            "HIGH", 3,
            "MEDIUM", 2,
            "LOW", 1
        );

        int minPriorityValue = priorityOrder.getOrDefault(minPriority.toUpperCase(), 1);

        List<Object> selectedRules = new ArrayList<>();
        for (Object ruleObj : allRules) {
            if (ruleObj instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> ruleConfig = (Map<String, Object>) ruleObj;
                String priority = getStringValue(ruleConfig, "priority", "LOW").toUpperCase();
                int priorityValue = priorityOrder.getOrDefault(priority, 1);

                if (priorityValue >= minPriorityValue) {
                    selectedRules.add(ruleObj);
                    logger.info("Selected rule '" + getStringValue(ruleConfig, "id", "unnamed") +
                               "' with priority " + priority);
                } else {
                    logger.info("Skipped rule '" + getStringValue(ruleConfig, "id", "unnamed") +
                               "' with priority " + priority + " (below minimum " + minPriority + ")");
                }
            }
        }

        // Sort selected rules by priority (HIGH first) and then by weight
        selectedRules.sort((a, b) -> {
            @SuppressWarnings("unchecked")
            Map<String, Object> configA = (Map<String, Object>) a;
            @SuppressWarnings("unchecked")
            Map<String, Object> configB = (Map<String, Object>) b;

            String priorityA = getStringValue(configA, "priority", "LOW").toUpperCase();
            String priorityB = getStringValue(configB, "priority", "LOW").toUpperCase();

            int priorityValueA = priorityOrder.getOrDefault(priorityA, 1);
            int priorityValueB = priorityOrder.getOrDefault(priorityB, 1);

            // First sort by priority (descending)
            int priorityComparison = Integer.compare(priorityValueB, priorityValueA);
            if (priorityComparison != 0) {
                return priorityComparison;
            }

            // Then sort by weight (descending)
            double weightA = getDoubleValue(configA, "weight", 1.0);
            double weightB = getDoubleValue(configB, "weight", 1.0);
            return Double.compare(weightB, weightA);
        });

        return selectedRules;
    }

    /**
     * Select rules based on dynamic threshold calculated from context.
     */
    private List<Object> selectRulesByDynamicThreshold(Map<String, Object> ruleSelection, List<Object> allRules,
                                                     ChainedEvaluationContext context) {
        String thresholdExpression = getStringValue(ruleSelection, "threshold-expression", "0.5");
        logger.info("Calculating dynamic threshold using expression: " + thresholdExpression);

        try {
            // Evaluate the threshold expression using the current context
            Double dynamicThreshold = evaluateExpression(thresholdExpression, context, Double.class);
            if (dynamicThreshold == null) {
                logger.warning("Dynamic threshold expression returned null, using default 0.5");
                dynamicThreshold = 0.5;
            }

            logger.info("Dynamic threshold calculated as: " + dynamicThreshold);

            List<Object> selectedRules = new ArrayList<>();
            for (Object ruleObj : allRules) {
                if (ruleObj instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> ruleConfig = (Map<String, Object>) ruleObj;
                    double weight = getDoubleValue(ruleConfig, "weight", 1.0);

                    if (weight >= dynamicThreshold) {
                        selectedRules.add(ruleObj);
                        logger.info("Selected rule '" + getStringValue(ruleConfig, "id", "unnamed") +
                                   "' with weight " + weight + " (above dynamic threshold " + dynamicThreshold + ")");
                    } else {
                        logger.info("Skipped rule '" + getStringValue(ruleConfig, "id", "unnamed") +
                                   "' with weight " + weight + " (below dynamic threshold " + dynamicThreshold + ")");
                    }
                }
            }

            return selectedRules;

        } catch (Exception e) {
            logger.severe("Error evaluating dynamic threshold expression '" + thresholdExpression + "': " + e.getMessage());
            logger.info("Falling back to executing all rules");
            return new ArrayList<>(allRules);
        }
    }

    /**
     * Execute a single accumulation rule.
     *
     * @param ruleConfig The rule configuration
     * @param ruleIndex The rule index (1-based)
     * @param context The chained evaluation context
     * @param resultBuilder The result builder
     * @param accumulatorVariable The name of the accumulator variable
     * @param currentScore The current accumulated score
     * @return The updated accumulated score
     */
    private double executeAccumulationRule(Map<String, Object> ruleConfig, int ruleIndex,
                                         ChainedEvaluationContext context, RuleChainResult.Builder resultBuilder,
                                         String accumulatorVariable, double currentScore) {
        try {
            // Create rule from configuration
            Rule accumulationRule = createRuleFromConfig(ruleConfig);
            String ruleName = accumulationRule.getName();

            logger.info("Executing accumulation rule " + ruleIndex + ": " + ruleName);

            // Get weight (default 1.0)
            double weight = 1.0;
            Object weightObj = ruleConfig.get("weight");
            if (weightObj instanceof Number) {
                weight = ((Number) weightObj).doubleValue();
            }

            // Evaluate the rule condition to get the component score
            String condition = accumulationRule.getCondition();
            Object componentResult = evaluatorService.evaluate(condition, context, Object.class);

            double componentScore = 0.0;
            if (componentResult instanceof Number) {
                componentScore = ((Number) componentResult).doubleValue();
            } else if (componentResult instanceof Boolean) {
                // Boolean results: true = 1, false = 0
                componentScore = ((Boolean) componentResult) ? 1.0 : 0.0;
            } else if (componentResult != null) {
                // Try to parse as double
                try {
                    componentScore = Double.parseDouble(componentResult.toString());
                } catch (NumberFormatException e) {
                    logger.warning("Could not parse component result as number: " + componentResult + ", using 0");
                    componentScore = 0.0;
                }
            }

            // Apply weight
            double weightedScore = componentScore * weight;

            // Add to accumulator
            double newScore = currentScore + weightedScore;

            logger.info("Rule " + ruleIndex + " (" + ruleName + "): component=" + componentScore +
                       ", weight=" + weight + ", weighted=" + weightedScore + ", total=" + newScore);

            // Execute rule for tracking purposes
            executeRule(accumulationRule, context, resultBuilder);

            // Store component results
            String componentKey = "component_" + ruleIndex + "_" + ruleName;
            context.addStageResult(componentKey + "_score", componentScore);
            context.addStageResult(componentKey + "_weighted", weightedScore);
            resultBuilder.addStageResult(componentKey + "_score", componentScore);
            resultBuilder.addStageResult(componentKey + "_weighted", weightedScore);

            // Update accumulator in context for next rule
            context.addStageResult(accumulatorVariable, newScore);
            context.setVariable(accumulatorVariable, newScore);

            return newScore;

        } catch (Exception e) {
            logger.severe("Error executing accumulation rule " + ruleIndex + ": " + e.getMessage());
            return currentScore; // Return unchanged score on error
        }
    }

    /**
     * Execute the final decision rule based on the accumulated score.
     *
     * @param configuration The rule chain configuration
     * @param context The chained evaluation context
     * @param resultBuilder The result builder
     * @return The final decision result, or null if no final decision rule is configured
     */
    private String executeFinalDecisionRule(Map<String, Object> configuration, ChainedEvaluationContext context,
                                          RuleChainResult.Builder resultBuilder) {
        Map<String, Object> finalDecisionConfig = getMapValue(configuration, "final-decision-rule");
        if (finalDecisionConfig == null) {
            logger.info("No final decision rule configured");
            return null;
        }

        try {
            context.setCurrentStage("final-decision-execution");

            // Create final decision rule
            Rule finalDecisionRule = createRuleFromConfig(finalDecisionConfig);

            logger.info("Executing final decision rule: " + finalDecisionRule.getName());

            // Evaluate the final decision condition
            String condition = finalDecisionRule.getCondition();
            Object decisionResult = evaluatorService.evaluate(condition, context, Object.class);

            String finalDecision = decisionResult != null ? decisionResult.toString() : "UNKNOWN";

            logger.info("Final decision: " + finalDecision);

            // Execute rule for tracking purposes
            executeRule(finalDecisionRule, context, resultBuilder);

            return finalDecision;

        } catch (Exception e) {
            logger.severe("Error executing final decision rule: " + e.getMessage());
            return SeverityConstants.ERROR;
        }
    }

    @Override
    public boolean validateConfiguration(Map<String, Object> configuration) {
        if (configuration == null) {
            logger.warning("Configuration is null");
            return false;
        }

        // Validate accumulator-variable (optional, has default)
        String accumulatorVariable = getStringValue(configuration, "accumulator-variable", "totalScore");
        if (accumulatorVariable.trim().isEmpty()) {
            logger.warning("accumulator-variable cannot be empty");
            return false;
        }

        // Validate initial-value (optional, defaults to 0)
        if (configuration.containsKey("initial-value")) {
            Object initialValue = configuration.get("initial-value");
            if (!(initialValue instanceof Number)) {
                logger.warning("initial-value must be a number");
                return false;
            }
        }

        // Validate accumulation-rules (required)
        if (!hasRequiredKey(configuration, "accumulation-rules")) {
            logger.warning("Missing required key: accumulation-rules");
            return false;
        }

        List<Object> accumulationRules = getListValue(configuration, "accumulation-rules");
        if (accumulationRules == null) {
            logger.warning("accumulation-rules must be a list");
            return false;
        }

        if (accumulationRules.isEmpty()) {
            logger.warning("accumulation-rules list cannot be empty");
            return false;
        }

        // Validate each accumulation rule
        for (int i = 0; i < accumulationRules.size(); i++) {
            Object ruleObj = accumulationRules.get(i);
            if (!(ruleObj instanceof Map)) {
                logger.warning("Accumulation rule at index " + i + " must be a map");
                return false;
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> ruleConfig = (Map<String, Object>) ruleObj;

            if (!validateAccumulationRuleConfiguration(ruleConfig, i)) {
                return false;
            }
        }

        // Validate final-decision-rule (optional)
        if (configuration.containsKey("final-decision-rule")) {
            Map<String, Object> finalDecisionConfig = getMapValue(configuration, "final-decision-rule");
            if (finalDecisionConfig == null) {
                logger.warning("final-decision-rule must be a map");
                return false;
            }

            if (!hasRequiredKey(finalDecisionConfig, "condition")) {
                logger.warning("final-decision-rule missing required condition");
                return false;
            }
        }

        // Validate rule-selection if present
        if (configuration.containsKey("rule-selection")) {
            if (!validateRuleSelectionConfiguration(configuration)) {
                return false;
            }
        }

        return true;
    }

    /**
     * Validate a single accumulation rule configuration.
     *
     * @param ruleConfig The rule configuration to validate
     * @param ruleIndex The rule index for error reporting
     * @return true if valid, false otherwise
     */
    private boolean validateAccumulationRuleConfiguration(Map<String, Object> ruleConfig, int ruleIndex) {
        // Validate required condition
        if (!hasRequiredKey(ruleConfig, "condition")) {
            logger.warning("Accumulation rule at index " + ruleIndex + " missing required condition");
            return false;
        }

        // Validate weight if present
        if (ruleConfig.containsKey("weight")) {
            Object weight = ruleConfig.get("weight");
            if (!(weight instanceof Number)) {
                logger.warning("Accumulation rule at index " + ruleIndex + " weight must be a number");
                return false;
            }
        }

        // Validate priority if present
        if (ruleConfig.containsKey("priority")) {
            String priority = getStringValue(ruleConfig, "priority", "LOW");
            if (!priority.matches("(?i)(HIGH|MEDIUM|LOW)")) {
                logger.warning("Accumulation rule at index " + ruleIndex + " priority must be HIGH, MEDIUM, or LOW");
                return false;
            }
        }

        return true;
    }

    /**
     * Validate rule selection configuration.
     */
    private boolean validateRuleSelectionConfiguration(Map<String, Object> configuration) {
        Map<String, Object> ruleSelection = getMapValue(configuration, "rule-selection");
        if (ruleSelection == null) {
            logger.warning("rule-selection must be a map");
            return false;
        }

        String strategy = getStringValue(ruleSelection, "strategy", "all");
        switch (strategy.toLowerCase()) {
            case "weight-threshold":
                if (!ruleSelection.containsKey("weight-threshold")) {
                    logger.warning("weight-threshold strategy requires 'weight-threshold' parameter");
                    return false;
                }
                Object threshold = ruleSelection.get("weight-threshold");
                if (!(threshold instanceof Number)) {
                    logger.warning("weight-threshold must be a number");
                    return false;
                }
                break;

            case "top-weighted":
                if (ruleSelection.containsKey("max-rules")) {
                    Object maxRules = ruleSelection.get("max-rules");
                    if (!(maxRules instanceof Number) || ((Number) maxRules).intValue() <= 0) {
                        logger.warning("max-rules must be a positive integer");
                        return false;
                    }
                }
                break;

            case "priority-based":
                if (ruleSelection.containsKey("min-priority")) {
                    String minPriority = getStringValue(ruleSelection, "min-priority", "LOW");
                    if (!minPriority.matches("(?i)(HIGH|MEDIUM|LOW)")) {
                        logger.warning("min-priority must be HIGH, MEDIUM, or LOW");
                        return false;
                    }
                }
                break;

            case "dynamic-threshold":
                if (!hasRequiredKey(ruleSelection, "threshold-expression")) {
                    logger.warning("dynamic-threshold strategy requires 'threshold-expression' parameter");
                    return false;
                }
                break;

            case "all":
                // No additional validation needed
                break;

            default:
                logger.warning("Unknown rule selection strategy: " + strategy);
                return false;
        }

        return true;
    }

    @Override
    public String getPatternName() {
        return "accumulative-chaining";
    }

    /**
     * Helper method to get double value from configuration.
     */
    private double getDoubleValue(Map<String, Object> config, String key, double defaultValue) {
        Object value = config.get(key);
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        return defaultValue;
    }

    /**
     * Helper method to get integer value from configuration.
     */
    private int getIntValue(Map<String, Object> config, String key, int defaultValue) {
        Object value = config.get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return defaultValue;
    }

    /**
     * Helper class to hold a rule with its weight for sorting.
     */
    private static class RuleWithWeight {
        final Object rule;
        final double weight;

        RuleWithWeight(Object rule, double weight) {
            this.rule = rule;
            this.weight = weight;
        }
    }
}
