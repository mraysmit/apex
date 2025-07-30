package dev.mars.apex.core.engine.executor;

import dev.mars.apex.core.config.yaml.YamlRuleChain;
import dev.mars.apex.core.engine.context.ChainedEvaluationContext;
import dev.mars.apex.core.engine.model.Rule;
import dev.mars.apex.core.engine.model.RuleChainResult;
import dev.mars.apex.core.engine.model.RuleResult;
import dev.mars.apex.core.service.engine.ExpressionEvaluatorService;
import dev.mars.apex.core.service.engine.RuleEngineService;

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
 * Executor for Pattern 5: Complex Financial Workflow.
 * Real-world nested rule scenario with multi-stage processing and dependencies.
 *
 * YAML Configuration Example:
 * ```yaml
 * rule-chains:
 *   - id: "trade-processing-workflow"
 *     pattern: "complex-workflow"
 *     configuration:
 *       stages:
 *         - stage: "pre-validation"
 *           name: "Pre-Validation Stage"
 *           rules:
 *             - condition: "#tradeType != null && #notionalAmount != null"
 *               message: "Basic trade data validation"
 *           failure-action: "terminate"
 *         - stage: "risk-assessment"
 *           name: "Risk Assessment Stage"
 *           depends-on: ["pre-validation"]
 *           rules:
 *             - condition: "#notionalAmount > 1000000 ? 'HIGH' : 'MEDIUM'"
 *               message: "Risk level assessment"
 *           output-variable: "riskLevel"
 *         - stage: "approval"
 *           name: "Approval Stage"
 *           depends-on: ["risk-assessment"]
 *           conditional-execution:
 *             condition: "#riskLevel == 'HIGH'"
 *             on-true:
 *               rules:
 *                 - condition: "#seniorApprovalRequired = true"
 *                   message: "Senior approval required"
 *             on-false:
 *               rules:
 *                 - condition: "#standardApproval = true"
 *                   message: "Standard approval applied"
 * ```
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-07-28
 * @version 1.0
 */
public class ComplexWorkflowExecutor extends PatternExecutor {

    public ComplexWorkflowExecutor(RuleEngineService ruleEngineService, ExpressionEvaluatorService evaluatorService) {
        super(ruleEngineService, evaluatorService);
    }

    @Override
    public RuleChainResult execute(YamlRuleChain ruleChain, Map<String, Object> configuration, ChainedEvaluationContext context) {
        logger.info("Executing complex workflow pattern for rule chain: " + ruleChain.getId());

        if (!validateConfiguration(configuration)) {
            return createValidationFailure(ruleChain, "Invalid complex workflow configuration");
        }

        RuleChainResult.Builder resultBuilder = RuleChainResult.builder(ruleChain.getId(), getPatternName())
                .ruleChainName(ruleChain.getName());

        try {
            // Get stages configuration
            List<Object> stagesConfig = getListValue(configuration, "stages");
            if (stagesConfig == null || stagesConfig.isEmpty()) {
                return createValidationFailure(ruleChain, "stages configuration is missing or empty");
            }

            logger.info("Executing " + stagesConfig.size() + " workflow stages");

            // Build dependency graph and execution order
            Map<String, WorkflowStage> stageMap = buildStageMap(stagesConfig);
            List<String> executionOrder = calculateExecutionOrder(stageMap);

            // Execute stages in dependency order
            for (String stageName : executionOrder) {
                WorkflowStage stage = stageMap.get(stageName);
                if (!executeWorkflowStage(stage, context, resultBuilder)) {
                    // Check failure action
                    if ("terminate".equals(stage.getFailureAction())) {
                        return resultBuilder.errorMessage("Stage '" + stageName + "' failed with terminate action").build();
                    }
                    // Continue with other stages if failure action is not terminate
                    logger.warning("Stage '" + stageName + "' failed but continuing execution");
                }
            }

            resultBuilder.finalOutcome("COMPLEX_WORKFLOW_COMPLETED");
            return resultBuilder.successful(true).build();

        } catch (Exception e) {
            logger.severe("Error executing complex workflow: " + e.getMessage());
            return resultBuilder.errorMessage("Execution error: " + e.getMessage()).build();
        }
    }

    /**
     * Build a map of workflow stages from configuration.
     */
    private Map<String, WorkflowStage> buildStageMap(List<Object> stagesConfig) {
        Map<String, WorkflowStage> stageMap = new HashMap<>();

        for (Object stageObj : stagesConfig) {
            if (stageObj instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> stageConfig = (Map<String, Object>) stageObj;

                WorkflowStage stage = new WorkflowStage();
                stage.setStageId(getStringValue(stageConfig, "stage", "stage-" + System.currentTimeMillis()));
                stage.setName(getStringValue(stageConfig, "name", stage.getStageId()));
                stage.setFailureAction(getStringValue(stageConfig, "failure-action", "continue"));
                stage.setOutputVariable(getStringValue(stageConfig, "output-variable", null));

                // Parse dependencies
                List<Object> dependsOn = getListValue(stageConfig, "depends-on");
                if (dependsOn != null) {
                    List<String> dependencies = new ArrayList<>();
                    for (Object dep : dependsOn) {
                        dependencies.add(dep.toString());
                    }
                    stage.setDependencies(dependencies);
                }

                // Parse rules
                List<Object> rulesConfig = getListValue(stageConfig, "rules");
                if (rulesConfig != null) {
                    List<Rule> rules = new ArrayList<>();
                    for (Object ruleObj : rulesConfig) {
                        if (ruleObj instanceof Map) {
                            @SuppressWarnings("unchecked")
                            Map<String, Object> ruleConfig = (Map<String, Object>) ruleObj;
                            rules.add(createRuleFromConfig(ruleConfig));
                        }
                    }
                    stage.setRules(rules);
                }

                // Parse conditional execution
                Map<String, Object> conditionalExecution = getMapValue(stageConfig, "conditional-execution");
                if (conditionalExecution != null) {
                    stage.setConditionalExecution(conditionalExecution);
                }

                stageMap.put(stage.getStageId(), stage);
            }
        }

        return stageMap;
    }

    /**
     * Calculate execution order based on dependencies using topological sort.
     */
    private List<String> calculateExecutionOrder(Map<String, WorkflowStage> stageMap) {
        List<String> executionOrder = new ArrayList<>();
        Set<String> visited = new HashSet<>();
        Set<String> visiting = new HashSet<>();

        for (String stageId : stageMap.keySet()) {
            if (!visited.contains(stageId)) {
                topologicalSort(stageId, stageMap, visited, visiting, executionOrder);
            }
        }

        Collections.reverse(executionOrder); // Reverse to get correct order
        return executionOrder;
    }

    /**
     * Topological sort helper method.
     */
    private void topologicalSort(String stageId, Map<String, WorkflowStage> stageMap,
                                Set<String> visited, Set<String> visiting, List<String> executionOrder) {
        if (visiting.contains(stageId)) {
            throw new RuntimeException("Circular dependency detected involving stage: " + stageId);
        }

        if (visited.contains(stageId)) {
            return;
        }

        visiting.add(stageId);

        WorkflowStage stage = stageMap.get(stageId);
        if (stage.getDependencies() != null) {
            for (String dependency : stage.getDependencies()) {
                if (stageMap.containsKey(dependency)) {
                    topologicalSort(dependency, stageMap, visited, visiting, executionOrder);
                }
            }
        }

        visiting.remove(stageId);
        visited.add(stageId);
        executionOrder.add(stageId);
    }

    /**
     * Execute a single workflow stage.
     */
    private boolean executeWorkflowStage(WorkflowStage stage, ChainedEvaluationContext context,
                                       RuleChainResult.Builder resultBuilder) {
        try {
            logger.info("Executing workflow stage: " + stage.getName());
            context.setCurrentStage(stage.getName());

            // Check if stage has conditional execution
            if (stage.getConditionalExecution() != null) {
                return executeConditionalStage(stage, context, resultBuilder);
            } else {
                return executeStandardStage(stage, context, resultBuilder);
            }

        } catch (Exception e) {
            logger.severe("Error executing workflow stage '" + stage.getName() + "': " + e.getMessage());
            return false;
        }
    }

    /**
     * Execute a stage with conditional execution logic.
     */
    private boolean executeConditionalStage(WorkflowStage stage, ChainedEvaluationContext context,
                                          RuleChainResult.Builder resultBuilder) {
        Map<String, Object> conditionalExecution = stage.getConditionalExecution();
        String condition = getStringValue(conditionalExecution, "condition", "true");

        try {
            // Evaluate the conditional expression
            Boolean conditionResult = evaluateExpression(condition, context, Boolean.class);

            logger.info("Stage '" + stage.getName() + "' condition evaluated to: " + conditionResult);

            // Execute appropriate rules based on condition result
            Map<String, Object> executionBranch;
            if (conditionResult != null && conditionResult) {
                executionBranch = getMapValue(conditionalExecution, "on-true");
            } else {
                executionBranch = getMapValue(conditionalExecution, "on-false");
            }

            if (executionBranch != null) {
                List<Object> branchRules = getListValue(executionBranch, "rules");
                if (branchRules != null) {
                    return executeStageRules(branchRules, stage, context, resultBuilder);
                }
            }

            // No rules to execute in this branch
            return true;

        } catch (Exception e) {
            logger.severe("Error evaluating conditional stage '" + stage.getName() + "': " + e.getMessage());
            return false;
        }
    }

    /**
     * Execute a standard stage without conditional logic.
     */
    private boolean executeStandardStage(WorkflowStage stage, ChainedEvaluationContext context,
                                       RuleChainResult.Builder resultBuilder) {
        if (stage.getRules() == null || stage.getRules().isEmpty()) {
            logger.info("Stage '" + stage.getName() + "' has no rules to execute");
            return true;
        }

        boolean allPassed = true;
        Object stageResult = null;

        for (Rule rule : stage.getRules()) {
            RuleResult ruleResult = executeRule(rule, context, resultBuilder);

            if (!ruleResult.isTriggered()) {
                allPassed = false;
                if ("terminate".equals(stage.getFailureAction())) {
                    logger.warning("Rule '" + rule.getName() + "' failed in stage '" + stage.getName() + "' with terminate action");
                    return false;
                }
            } else {
                // For stages with output variables, use the last successful rule result
                if (stage.getOutputVariable() != null) {
                    try {
                        stageResult = evaluateExpression(rule.getCondition(), context, Object.class);
                    } catch (Exception e) {
                        logger.warning("Could not evaluate rule condition as output for stage '" + stage.getName() + "'");
                    }
                }
            }
        }

        // Store stage result if output variable is specified
        if (stage.getOutputVariable() != null && stageResult != null) {
            context.addStageResult(stage.getOutputVariable(), stageResult);
            context.setVariable(stage.getOutputVariable(), stageResult);
            resultBuilder.addStageResult(stage.getOutputVariable(), stageResult);
        }

        // Store stage execution summary
        String stageResultKey = "stage_" + stage.getStageId() + "_result";
        context.addStageResult(stageResultKey, allPassed ? "SUCCESS" : "PARTIAL_SUCCESS");
        resultBuilder.addStageResult(stageResultKey, allPassed ? "SUCCESS" : "PARTIAL_SUCCESS");

        return allPassed;
    }

    /**
     * Execute rules from a conditional branch.
     */
    private boolean executeStageRules(List<Object> rulesConfig, WorkflowStage stage,
                                    ChainedEvaluationContext context, RuleChainResult.Builder resultBuilder) {
        boolean allPassed = true;

        for (Object ruleObj : rulesConfig) {
            if (ruleObj instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> ruleConfig = (Map<String, Object>) ruleObj;

                Rule rule = createRuleFromConfig(ruleConfig);
                RuleResult ruleResult = executeRule(rule, context, resultBuilder);

                if (!ruleResult.isTriggered()) {
                    allPassed = false;
                    if ("terminate".equals(stage.getFailureAction())) {
                        return false;
                    }
                }
            }
        }

        return allPassed;
    }

    @Override
    public boolean validateConfiguration(Map<String, Object> configuration) {
        if (configuration == null) {
            logger.warning("Configuration is null");
            return false;
        }

        // Validate stages configuration
        if (!hasRequiredKey(configuration, "stages")) {
            logger.warning("Missing required key: stages");
            return false;
        }

        List<Object> stagesConfig = getListValue(configuration, "stages");
        if (stagesConfig == null) {
            logger.warning("stages must be a list");
            return false;
        }

        if (stagesConfig.isEmpty()) {
            logger.warning("stages list cannot be empty");
            return false;
        }

        // Validate each stage
        Set<String> stageIds = new HashSet<>();
        for (int i = 0; i < stagesConfig.size(); i++) {
            Object stageObj = stagesConfig.get(i);
            if (!(stageObj instanceof Map)) {
                logger.warning("Stage at index " + i + " must be a map");
                return false;
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> stageConfig = (Map<String, Object>) stageObj;

            if (!validateStageConfiguration(stageConfig, i, stageIds)) {
                return false;
            }
        }

        return true;
    }

    /**
     * Validate a single stage configuration.
     */
    private boolean validateStageConfiguration(Map<String, Object> stageConfig, int stageIndex, Set<String> stageIds) {
        // Validate stage ID
        if (!hasRequiredKey(stageConfig, "stage")) {
            logger.warning("Stage at index " + stageIndex + " missing required key: stage");
            return false;
        }

        String stageId = getStringValue(stageConfig, "stage", null);
        if (stageId == null || stageId.trim().isEmpty()) {
            logger.warning("Stage at index " + stageIndex + " has empty stage ID");
            return false;
        }

        if (stageIds.contains(stageId)) {
            logger.warning("Duplicate stage ID: " + stageId);
            return false;
        }
        stageIds.add(stageId);

        // Validate failure action if present
        if (stageConfig.containsKey("failure-action")) {
            String failureAction = getStringValue(stageConfig, "failure-action", "continue");
            if (!"terminate".equals(failureAction) && !"continue".equals(failureAction)) {
                logger.warning("Stage '" + stageId + "' has invalid failure-action: " + failureAction +
                             " (must be 'terminate' or 'continue')");
                return false;
            }
        }

        // Validate dependencies if present
        if (stageConfig.containsKey("depends-on")) {
            List<Object> dependsOn = getListValue(stageConfig, "depends-on");
            if (dependsOn == null) {
                logger.warning("Stage '" + stageId + "' depends-on must be a list");
                return false;
            }
        }

        // Validate rules if present
        if (stageConfig.containsKey("rules")) {
            List<Object> rules = getListValue(stageConfig, "rules");
            if (rules == null) {
                logger.warning("Stage '" + stageId + "' rules must be a list");
                return false;
            }

            for (int i = 0; i < rules.size(); i++) {
                Object ruleObj = rules.get(i);
                if (!(ruleObj instanceof Map)) {
                    logger.warning("Stage '" + stageId + "' rule at index " + i + " must be a map");
                    return false;
                }

                @SuppressWarnings("unchecked")
                Map<String, Object> ruleConfig = (Map<String, Object>) ruleObj;

                if (!hasRequiredKey(ruleConfig, "condition")) {
                    logger.warning("Stage '" + stageId + "' rule at index " + i + " missing required condition");
                    return false;
                }
            }
        }

        // Validate conditional execution if present
        if (stageConfig.containsKey("conditional-execution")) {
            Map<String, Object> conditionalExecution = getMapValue(stageConfig, "conditional-execution");
            if (conditionalExecution == null) {
                logger.warning("Stage '" + stageId + "' conditional-execution must be a map");
                return false;
            }

            if (!hasRequiredKey(conditionalExecution, "condition")) {
                logger.warning("Stage '" + stageId + "' conditional-execution missing required condition");
                return false;
            }
        }

        return true;
    }

    @Override
    public String getPatternName() {
        return "complex-workflow";
    }

    /**
     * Internal class representing a workflow stage.
     */
    private static class WorkflowStage {
        private String stageId;
        private String name;
        private List<String> dependencies;
        private List<Rule> rules;
        private String failureAction = "continue";
        private String outputVariable;
        private Map<String, Object> conditionalExecution;

        // Getters and setters
        public String getStageId() { return stageId; }
        public void setStageId(String stageId) { this.stageId = stageId; }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public List<String> getDependencies() { return dependencies; }
        public void setDependencies(List<String> dependencies) { this.dependencies = dependencies; }

        public List<Rule> getRules() { return rules; }
        public void setRules(List<Rule> rules) { this.rules = rules; }

        public String getFailureAction() { return failureAction; }
        public void setFailureAction(String failureAction) { this.failureAction = failureAction; }

        public String getOutputVariable() { return outputVariable; }
        public void setOutputVariable(String outputVariable) { this.outputVariable = outputVariable; }

        public Map<String, Object> getConditionalExecution() { return conditionalExecution; }
        public void setConditionalExecution(Map<String, Object> conditionalExecution) { this.conditionalExecution = conditionalExecution; }
    }
}
