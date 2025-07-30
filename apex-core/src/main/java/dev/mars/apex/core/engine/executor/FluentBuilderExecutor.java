package dev.mars.apex.core.engine.executor;

import dev.mars.apex.core.config.yaml.YamlRuleChain;
import dev.mars.apex.core.engine.context.ChainedEvaluationContext;
import dev.mars.apex.core.engine.model.Rule;
import dev.mars.apex.core.engine.model.RuleChainResult;
import dev.mars.apex.core.engine.model.RuleResult;
import dev.mars.apex.core.service.engine.ExpressionEvaluatorService;
import dev.mars.apex.core.service.engine.RuleEngineService;

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
 * Executor for Pattern 6: Fluent Rule Builder.
 * Compose rules with conditional execution paths using a fluent API.
 *
 * YAML Configuration Example:
 * ```yaml
 * rule-chains:
 *   - id: "customer-processing-tree"
 *     pattern: "fluent-builder"
 *     configuration:
 *       root-rule:
 *         id: "customer-type-check"
 *         condition: "#customerType == 'VIP' || #customerType == 'PREMIUM'"
 *         message: "High-tier customer detected"
 *         on-success:
 *           rule:
 *             id: "high-value-check"
 *             condition: "#transactionAmount > 100000"
 *             message: "High-value transaction detected"
 *             on-success:
 *               rule:
 *                 id: "final-approval"
 *                 condition: "true"
 *                 message: "Final approval granted"
 *             on-failure:
 *               rule:
 *                 id: "standard-processing"
 *                 condition: "true"
 *                 message: "Standard processing applied"
 *         on-failure:
 *           rule:
 *             id: "basic-validation"
 *             condition: "#transactionAmount > 0"
 *             message: "Basic validation check"
 * ```
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-07-28
 * @version 1.0
 */
public class FluentBuilderExecutor extends PatternExecutor {

    public FluentBuilderExecutor(RuleEngineService ruleEngineService, ExpressionEvaluatorService evaluatorService) {
        super(ruleEngineService, evaluatorService);
    }

    @Override
    public RuleChainResult execute(YamlRuleChain ruleChain, Map<String, Object> configuration, ChainedEvaluationContext context) {
        logger.info("Executing fluent builder pattern for rule chain: " + ruleChain.getId());

        if (!validateConfiguration(configuration)) {
            return createValidationFailure(ruleChain, "Invalid fluent builder configuration");
        }

        RuleChainResult.Builder resultBuilder = RuleChainResult.builder(ruleChain.getId(), getPatternName())
                .ruleChainName(ruleChain.getName());

        try {
            // Get root rule configuration
            Map<String, Object> rootRuleConfig = getMapValue(configuration, "root-rule");
            if (rootRuleConfig == null) {
                return createValidationFailure(ruleChain, "root-rule configuration is missing");
            }

            context.setCurrentStage("fluent-tree-execution");

            // Execute the fluent rule tree starting from root
            String finalOutcome = executeFluentRuleNode(rootRuleConfig, context, resultBuilder, 0);

            resultBuilder.finalOutcome(finalOutcome != null ? finalOutcome : "FLUENT_TREE_COMPLETED");
            return resultBuilder.successful(true).build();

        } catch (Exception e) {
            logger.severe("Error executing fluent builder: " + e.getMessage());
            return resultBuilder.errorMessage("Execution error: " + e.getMessage()).build();
        }
    }

    /**
     * Execute a fluent rule node and its children based on the result.
     *
     * @param ruleConfig The rule configuration
     * @param context The chained evaluation context
     * @param resultBuilder The result builder
     * @param depth The current depth in the tree (for logging)
     * @return The final outcome from this branch
     */
    private String executeFluentRuleNode(Map<String, Object> ruleConfig, ChainedEvaluationContext context,
                                       RuleChainResult.Builder resultBuilder, int depth) {
        try {
            // Create and execute the rule
            Rule rule = createRuleFromConfig(ruleConfig);

            String indent = "  ".repeat(depth);
            logger.info(indent + "Executing fluent rule: " + rule.getName());

            RuleResult ruleResult = executeRule(rule, context, resultBuilder);

            // Store rule result in context
            String resultKey = "fluent_rule_" + rule.getName() + "_result";
            context.addStageResult(resultKey, ruleResult.isTriggered());
            resultBuilder.addStageResult(resultKey, ruleResult.isTriggered());

            logger.info(indent + "Rule '" + rule.getName() + "' result: " +
                       (ruleResult.isTriggered() ? "SUCCESS" : "FAILURE"));

            // Determine next rule based on result
            Map<String, Object> nextRuleConfig = null;
            String branchType = null;

            if (ruleResult.isTriggered()) {
                // Rule succeeded, check for on-success branch
                Map<String, Object> onSuccess = getMapValue(ruleConfig, "on-success");
                if (onSuccess != null) {
                    nextRuleConfig = getMapValue(onSuccess, "rule");
                    branchType = "on-success";
                }
            } else {
                // Rule failed, check for on-failure branch
                Map<String, Object> onFailure = getMapValue(ruleConfig, "on-failure");
                if (onFailure != null) {
                    nextRuleConfig = getMapValue(onFailure, "rule");
                    branchType = "on-failure";
                }
            }

            // Execute next rule if present
            if (nextRuleConfig != null) {
                logger.info(indent + "Following " + branchType + " branch");
                return executeFluentRuleNode(nextRuleConfig, context, resultBuilder, depth + 1);
            } else {
                // Leaf node reached
                String leafOutcome = ruleResult.isTriggered() ? "SUCCESS" : "FAILURE";
                logger.info(indent + "Leaf node reached with outcome: " + leafOutcome);
                return leafOutcome;
            }

        } catch (Exception e) {
            logger.severe("Error executing fluent rule node at depth " + depth + ": " + e.getMessage());
            return "ERROR";
        }
    }

    @Override
    public boolean validateConfiguration(Map<String, Object> configuration) {
        if (configuration == null) {
            logger.warning("Configuration is null");
            return false;
        }

        // Validate root-rule configuration
        if (!hasRequiredKey(configuration, "root-rule")) {
            logger.warning("Missing required key: root-rule");
            return false;
        }

        Map<String, Object> rootRuleConfig = getMapValue(configuration, "root-rule");
        if (rootRuleConfig == null) {
            logger.warning("root-rule must be a map");
            return false;
        }

        // Validate the rule tree starting from root
        return validateFluentRuleNode(rootRuleConfig, "root-rule", 0);
    }

    /**
     * Validate a fluent rule node and its children recursively.
     *
     * @param ruleConfig The rule configuration to validate
     * @param nodeName The name of this node for error reporting
     * @param depth The current depth (to prevent infinite recursion)
     * @return true if valid, false otherwise
     */
    private boolean validateFluentRuleNode(Map<String, Object> ruleConfig, String nodeName, int depth) {
        // Prevent infinite recursion
        if (depth > 20) {
            logger.warning("Rule tree too deep (max depth 20) at node: " + nodeName);
            return false;
        }

        // Validate required condition
        if (!hasRequiredKey(ruleConfig, "condition")) {
            logger.warning("Rule node '" + nodeName + "' missing required condition");
            return false;
        }

        // Validate on-success branch if present
        if (ruleConfig.containsKey("on-success")) {
            Map<String, Object> onSuccess = getMapValue(ruleConfig, "on-success");
            if (onSuccess == null) {
                logger.warning("Rule node '" + nodeName + "' on-success must be a map");
                return false;
            }

            if (onSuccess.containsKey("rule")) {
                Map<String, Object> successRule = getMapValue(onSuccess, "rule");
                if (successRule == null) {
                    logger.warning("Rule node '" + nodeName + "' on-success rule must be a map");
                    return false;
                }

                if (!validateFluentRuleNode(successRule, nodeName + ".on-success", depth + 1)) {
                    return false;
                }
            }
        }

        // Validate on-failure branch if present
        if (ruleConfig.containsKey("on-failure")) {
            Map<String, Object> onFailure = getMapValue(ruleConfig, "on-failure");
            if (onFailure == null) {
                logger.warning("Rule node '" + nodeName + "' on-failure must be a map");
                return false;
            }

            if (onFailure.containsKey("rule")) {
                Map<String, Object> failureRule = getMapValue(onFailure, "rule");
                if (failureRule == null) {
                    logger.warning("Rule node '" + nodeName + "' on-failure rule must be a map");
                    return false;
                }

                if (!validateFluentRuleNode(failureRule, nodeName + ".on-failure", depth + 1)) {
                    return false;
                }
            }
        }

        return true;
    }

    @Override
    public String getPatternName() {
        return "fluent-builder";
    }
}
