package dev.mars.apex.core.service.scenario;

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

import dev.mars.apex.core.config.component.ComponentConfiguration;
import dev.mars.apex.core.config.component.ComponentLoader;
import dev.mars.apex.core.config.yaml.YamlConfigurationException;
import dev.mars.apex.core.config.yaml.YamlConfigurationLoader;
import dev.mars.apex.core.config.yaml.YamlRuleConfiguration;
import dev.mars.apex.core.engine.config.RulesEngine;
import dev.mars.apex.core.engine.model.ExecutionStep;
import dev.mars.apex.core.engine.model.RuleResult;
import dev.mars.apex.core.config.yaml.YamlRuleFactory;
import dev.mars.apex.core.service.engine.ExpressionEvaluatorService;
import dev.mars.apex.core.util.TestAwareLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;

/**
 * Executor for processing scenario stages with dependency management, conditional execution, and failure policies.
 *
 * Follows the existing pattern from ComplexWorkflowExecutor and SequentialDependencyExecutor
 * but specialized for financial trade processing workflows. Handles stage dependencies,
 * conditional execution, failure policies, and provides comprehensive result tracking.
 *
 * EXECUTION FEATURES:
 * - Conditional stage execution via SpEL expressions
 * - Dependency-aware stage execution
 * - Configurable failure policies per stage
 * - Performance monitoring and SLA tracking
 * - Comprehensive error handling and recovery
 * - Context sharing between stages
 *
 * CONDITIONAL EXECUTION:
 * - Stages can have optional SpEL conditions that control execution
 * - Conditions are evaluated before dependency checks
 * - If condition evaluates to false, stage is skipped
 * - Condition evaluation errors result in stage being skipped (safe default)
 * - Conditions have access to data context (#data, #scenarioContext, etc.)
 *
 * FAILURE POLICIES:
 * - terminate: Stop processing immediately if stage fails
 * - continue-with-warnings: Log warnings but continue to next stage
 * - flag-for-review: Mark for manual review but continue processing
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 1.0.0
 */
public class ScenarioStageExecutor {

    private static final Logger logger = LoggerFactory.getLogger(ScenarioStageExecutor.class);

    private final YamlConfigurationLoader configLoader;
    private final YamlRuleFactory ruleFactory;
    private final ExpressionEvaluatorService expressionEvaluator;

    public ScenarioStageExecutor() {
        this.configLoader = new YamlConfigurationLoader();
        this.ruleFactory = new YamlRuleFactory();
        this.expressionEvaluator = new ExpressionEvaluatorService();
    }

    public ScenarioStageExecutor(YamlConfigurationLoader configLoader, YamlRuleFactory ruleFactory) {
        this.configLoader = configLoader != null ? configLoader : new YamlConfigurationLoader();
        this.ruleFactory = ruleFactory != null ? ruleFactory : new YamlRuleFactory();
        this.expressionEvaluator = new ExpressionEvaluatorService();
    }
    
    /**
     * Execute scenario stages in dependency order with failure policy enforcement.
     * 
     * @param scenario the scenario configuration with stages
     * @param data the data to process through the stages
     * @return comprehensive execution result
     */
    public ScenarioExecutionResult executeStages(ScenarioConfiguration scenario, Object data) {
        if (scenario == null) {
            throw new IllegalArgumentException("Scenario configuration cannot be null");
        }
        
        if (!scenario.hasStageConfiguration()) {
            throw new IllegalArgumentException("Scenario does not have stage configuration");
        }
        
        List<ScenarioStage> stages = scenario.getStagesByExecutionOrder();
        ScenarioExecutionResult result = new ScenarioExecutionResult(scenario.getScenarioId());

        logger.info("Executing {} stages for scenario '{}'", stages.size(), scenario.getScenarioId());

        int currentStageIndex = 0;
        for (ScenarioStage stage : stages) {
            if (!shouldExecuteStage(stage, data, result)) {
                String reason = getSkipReason(stage, data, result);
                logger.info("Skipping stage '{}': {}", stage.getStageName(), reason);
                result.addSkippedStage(stage.getStageName(), reason);
                
                // Add trace step for skipped stage
                result.addExecutionStep(new ExecutionStep(
                    stage.getStageName(), 
                    "SCENARIO_STAGE", 
                    "SKIPPED", 
                    reason, 
                    0
                ));
                
                currentStageIndex++;
                continue;
            }

            long stageStartTime = System.currentTimeMillis();
            StageExecutionResult stageResult = executeStage(stage, data, result);
            long duration = System.currentTimeMillis() - stageStartTime;
            stageResult.setExecutionTimeMs(duration);

            result.addStageResult(stageResult);
            
            // Add trace step for executed stage
            result.addExecutionStep(new ExecutionStep(
                stage.getStageName(), 
                "SCENARIO_STAGE", 
                stageResult.isSuccessful() ? "SUCCESS" : "FAILURE", 
                stageResult.getErrorMessage() != null ? stageResult.getErrorMessage() : "Stage completed", 
                duration
            ));
            
            // Add inner execution steps from the stage's rule result if available
            if (stageResult.getRuleResult() != null && stageResult.getRuleResult().getExecutionPath() != null) {
                result.addExecutionSteps(stageResult.getRuleResult().getExecutionPath());
            }

            logger.info("Stage '{}' completed: {}", stage.getStageName(), stageResult.getExecutionSummary());

            if (!handleStageResult(stage, stageResult, result)) {
                logger.warn("Terminating scenario execution due to stage '{}' failure policy", stage.getStageName());

                // Mark remaining stages as skipped due to termination
                for (int i = currentStageIndex + 1; i < stages.size(); i++) {
                    ScenarioStage remainingStage = stages.get(i);
                    result.addSkippedStage(remainingStage.getStageName(), "Scenario terminated due to previous stage failure");
                    
                    // Add trace step for skipped stage
                    result.addExecutionStep(new ExecutionStep(
                        remainingStage.getStageName(), 
                        "SCENARIO_STAGE", 
                        "SKIPPED", 
                        "Scenario terminated due to previous stage failure", 
                        0
                    ));
                }

                break; // Terminate processing based on failure policy
            }

            currentStageIndex++;
        }
        
        result.finalizeExecution();
        logger.info("Scenario execution completed: {}", result.getExecutionSummary());
        
        return result;
    }
    
    /**
     * Checks if a stage should be executed based on its condition and dependencies.
     *
     * @param stage the stage to check
     * @param data the input data for condition evaluation
     * @param result the current execution result
     * @return true if the stage should be executed
     */
    private boolean shouldExecuteStage(ScenarioStage stage, Object data, ScenarioExecutionResult result) {
        // First check condition (if specified)
        if (stage.hasCondition()) {
            try {
                Map<String, Object> facts = createFactsMap(data, result);
                Boolean conditionMet = expressionEvaluator.evaluateWithEnhancedContext(
                    stage.getCondition(), facts, Boolean.class);
                if (conditionMet == null || !conditionMet) {
                    logger.info("Stage '{}' condition not met - skipping: {}",
                        stage.getStageName(), stage.getCondition());
                    return false;
                }
                logger.debug("Stage '{}' condition met: {}",
                    stage.getStageName(), stage.getCondition());
            } catch (Exception e) {
                logger.warn("Stage '{}' condition evaluation failed - skipping: {}",
                    stage.getStageName(), e.getMessage());
                return false;
            }
        }

        // Then check dependencies
        if (!stage.hasDependencies()) {
            return true; // No dependencies, can execute
        }

        // Check if all dependencies are satisfied
        for (String dependency : stage.getDependsOn()) {
            if (!result.isStageSuccessful(dependency)) {
                logger.debug("Stage '{}' dependency '{}' not satisfied",
                    stage.getStageName(), dependency);
                return false;
            }
        }

        return true;
    }

    /**
     * Gets the reason why a stage is being skipped (condition or dependency failure).
     *
     * @param stage the stage
     * @param data the input data for condition evaluation
     * @param result the current execution result
     * @return reason for skipping the stage
     */
    private String getSkipReason(ScenarioStage stage, Object data, ScenarioExecutionResult result) {
        // Check condition first
        if (stage.hasCondition()) {
            try {
                Map<String, Object> facts = createFactsMap(data, result);
                Boolean conditionMet = expressionEvaluator.evaluateWithEnhancedContext(
                    stage.getCondition(), facts, Boolean.class);
                if (conditionMet == null || !conditionMet) {
                    return "Condition not met: " + stage.getCondition();
                }
            } catch (Exception e) {
                return "Condition evaluation failed: " + e.getMessage();
            }
        }

        // Check dependencies
        if (!stage.hasDependencies()) {
            return "Unknown reason";
        }

        List<String> failedDependencies = new ArrayList<>();
        for (String dependency : stage.getDependsOn()) {
            if (!result.isStageSuccessful(dependency)) {
                failedDependencies.add(dependency);
            }
        }

        if (failedDependencies.isEmpty()) {
            return "Dependencies satisfied";
        } else {
            return "Failed dependencies: " + String.join(", ", failedDependencies);
        }
    }
    
    /**
     * Executes a single stage.
     *
     * @param stage the stage to execute
     * @param data the input data
     * @param context the execution context
     * @return stage execution result
     */
    private StageExecutionResult executeStage(ScenarioStage stage, Object data, ScenarioExecutionResult context) {
        logger.info("Executing stage '{}' with config: {}", stage.getStageName(), stage.getConfigFile());

        try {
            // Validate stage configuration
            List<String> validationErrors = stage.validate();
            if (!validationErrors.isEmpty()) {
                String errorMessage = "Stage configuration errors: " + String.join(", ", validationErrors);
                TestAwareLogger.warn(logger, "Configuration error in stage '{}': {}", stage.getStageName(), errorMessage);
                return StageExecutionResult.configurationError(stage.getStageName(), errorMessage);
            }

            // Check if config file is a component
            boolean isComponent = configLoader.isComponentFile(stage.getConfigFile());

            if (isComponent) {
                // Handle component file - expand and execute all referenced files
                return executeComponentStage(stage, data, context);
            } else {
                // Handle regular config file
                return executeRegularStage(stage, data, context);
            }

        } catch (Exception e) {
            TestAwareLogger.error(logger, "Error executing stage '{}': {}", stage.getStageName(), e.getMessage(), e);

            String errorMessage = "Stage execution exception: " + e.getMessage();
            return stage.isRequired() ?
                StageExecutionResult.criticalFailure(stage.getStageName(), errorMessage) :
                StageExecutionResult.failure(stage.getStageName(), errorMessage);
        }
    }

    /**
     * Executes a stage with a regular configuration file.
     */
    private StageExecutionResult executeRegularStage(ScenarioStage stage, Object data, ScenarioExecutionResult context)
            throws YamlConfigurationException {

        // Load stage configuration with file-system first, then classpath fallback
        YamlRuleConfiguration stageConfig;
        try {
            stageConfig = configLoader.loadFromFile(stage.getConfigFile());
        } catch (dev.mars.apex.core.config.yaml.YamlConfigurationException e) {
            // Fallback: treat path as classpath resource (tests/resources)
            stageConfig = configLoader.loadFromClasspath(stage.getConfigFile());
        }

        // Create rules engine for this stage
        RulesEngine stageEngine = new RulesEngine(
            ruleFactory.createRulesEngineConfiguration(stageConfig)
        );

        // Create facts map with data and context
        Map<String, Object> facts = createFactsMap(data, context);

        // Execute stage rules using the unified evaluation method
        RuleResult ruleResult = stageEngine.evaluate(stageConfig, facts);

        // Create stage result based on rule execution
        if (ruleResult.isSuccess()) {
            StageExecutionResult stageResult = StageExecutionResult.success(stage.getStageName(), ruleResult);

            // Add enriched data as stage outputs if available
            if (ruleResult.getEnrichedData() != null && !ruleResult.getEnrichedData().isEmpty()) {
                stageResult.setStageOutputs(ruleResult.getEnrichedData());
            }

            return stageResult;
        } else {
            String errorMessage = "Stage execution failed: " + ruleResult.getMessage();
            if (!ruleResult.getFailureMessages().isEmpty()) {
                errorMessage += " - " + String.join(", ", ruleResult.getFailureMessages());
            }

            return stage.isRequired() ?
                StageExecutionResult.criticalFailure(stage.getStageName(), errorMessage) :
                StageExecutionResult.nonCriticalFailure(stage.getStageName(), errorMessage);
        }
    }

    /**
     * Executes a stage with a component configuration file.
     * Expands the component and executes all referenced files in order.
     */
    private StageExecutionResult executeComponentStage(ScenarioStage stage, Object data, ScenarioExecutionResult context)
            throws YamlConfigurationException, IOException {

        logger.info("Stage '{}' references a component file - expanding component", stage.getStageName());

        // Load the component
        ComponentLoader componentLoader = new ComponentLoader();
        ComponentConfiguration component = componentLoader.loadComponent(stage.getConfigFile());

        logger.info("Component '{}' loaded with {} total file references",
                   component.getId(), component.getAllReferences().size());

        // Resolve all file references (handles nesting and execution order)
        List<ComponentLoader.ResolvedFileReference> resolvedFiles =
            componentLoader.resolveAllReferences(component, stage.getConfigFile());

        logger.info("Component '{}' resolved to {} configuration files",
                   component.getId(), resolvedFiles.size());

        // Execute each resolved file in order
        StageExecutionResult aggregatedResult = StageExecutionResult.success(stage.getStageName(), null);
        Map<String, Object> aggregatedOutputs = new HashMap<>();

        for (ComponentLoader.ResolvedFileReference fileRef : resolvedFiles) {
            logger.info("Executing component file: {} (depth: {})",
                       fileRef.getFilePath(), fileRef.getNestingDepth());

            // Determine effective failure policy (file-level overrides stage-level)
            String effectiveFailurePolicy = fileRef.getFailurePolicy() != null ?
                fileRef.getFailurePolicy() : stage.getFailurePolicy();

            // Execute the file
            StageExecutionResult fileResult = executeConfigFile(
                fileRef.getFilePath(),
                stage.getStageName(),
                data,
                context,
                effectiveFailurePolicy
            );

            // Aggregate outputs
            if (fileResult.getStageOutputs() != null) {
                aggregatedOutputs.putAll(fileResult.getStageOutputs());
            }

            // Check if we should terminate based on failure policy
            if (!fileResult.isSuccessful()) {
                if ("terminate".equals(effectiveFailurePolicy)) {
                    logger.warn("Component file '{}' failed with terminate policy - stopping component execution",
                               fileRef.getFilePath());
                    // Return a failure result
                    StageExecutionResult failureResult = StageExecutionResult.criticalFailure(
                        stage.getStageName(),
                        "Component execution terminated due to file failure: " + fileRef.getFilePath()
                    );
                    failureResult.setStageOutputs(aggregatedOutputs);
                    return failureResult;
                } else {
                    logger.warn("Component file '{}' failed but continuing with policy: {}",
                               fileRef.getFilePath(), effectiveFailurePolicy);
                }
            }
        }

        aggregatedResult.setStageOutputs(aggregatedOutputs);
        return aggregatedResult;
    }

    /**
     * Executes a single configuration file.
     */
    private StageExecutionResult executeConfigFile(
            String configFilePath,
            String stageName,
            Object data,
            ScenarioExecutionResult context,
            String failurePolicy) throws YamlConfigurationException {

        // Load configuration
        YamlRuleConfiguration config;
        try {
            config = configLoader.loadFromFile(configFilePath);
        } catch (dev.mars.apex.core.config.yaml.YamlConfigurationException e) {
            // Fallback: treat path as classpath resource
            config = configLoader.loadFromClasspath(configFilePath);
        }

        // Create rules engine
        RulesEngine engine = new RulesEngine(ruleFactory.createRulesEngineConfiguration(config));

        // Create facts map
        Map<String, Object> facts = createFactsMap(data, context);

        // Execute rules
        RuleResult ruleResult = engine.evaluate(config, facts);

        // Create result
        if (ruleResult.isSuccess()) {
            StageExecutionResult result = StageExecutionResult.success(stageName, ruleResult);
            if (ruleResult.getEnrichedData() != null && !ruleResult.getEnrichedData().isEmpty()) {
                result.setStageOutputs(ruleResult.getEnrichedData());
            }
            return result;
        } else {
            String errorMessage = "Config file execution failed: " + ruleResult.getMessage();
            if (!ruleResult.getFailureMessages().isEmpty()) {
                errorMessage += " - " + String.join(", ", ruleResult.getFailureMessages());
            }

            return "terminate".equals(failurePolicy) ?
                StageExecutionResult.criticalFailure(stageName, errorMessage) :
                StageExecutionResult.nonCriticalFailure(stageName, errorMessage);
        }
    }
    
    /**
     * Creates the facts map for rule execution.
     * 
     * @param data the input data
     * @param context the execution context
     * @return facts map for rule evaluation
     */
    private Map<String, Object> createFactsMap(Object data, ScenarioExecutionResult context) {
        Map<String, Object> facts = new HashMap<>();
        facts.put("data", data);
        facts.put("scenarioContext", context);
        facts.put("previousStageResults", context.getStageResults());
        facts.put("scenarioId", context.getScenarioId());
        facts.put("executionStartTime", context.getExecutionStartTime());
        
        // Add outputs from previous successful stages
        for (StageExecutionResult stageResult : context.getSuccessfulStages()) {
            if (!stageResult.getStageOutputs().isEmpty()) {
                String stagePrefix = stageResult.getStageName() + "_";
                stageResult.getStageOutputs().forEach((key, value) -> 
                    facts.put(stagePrefix + key, value));
            }
        }
        
        return facts;
    }
    
    /**
     * Handles the result of a stage execution and applies failure policy.
     * 
     * @param stage the executed stage
     * @param stageResult the stage execution result
     * @param scenarioResult the overall scenario result
     * @return true if processing should continue, false if it should terminate
     */
    private boolean handleStageResult(ScenarioStage stage, StageExecutionResult stageResult, ScenarioExecutionResult scenarioResult) {
        if (stageResult.isSuccessful()) {
            logger.info("Stage '{}' completed successfully", stage.getStageName());
            return true;
        }
        
        // Apply failure policy
        String failurePolicy = stage.getFailurePolicy();
        switch (failurePolicy) {
            case ScenarioStage.FAILURE_POLICY_TERMINATE:
                TestAwareLogger.error(logger, "Stage '{}' failed - terminating scenario execution", stage.getStageName());
                scenarioResult.setTerminated(true);
                return false;
                
            case ScenarioStage.FAILURE_POLICY_CONTINUE_WITH_WARNINGS:
                TestAwareLogger.warn(logger, "Stage '{}' failed - continuing with warnings", stage.getStageName());
                scenarioResult.addWarning("Stage '" + stage.getStageName() + "' failed but processing continued: " + stageResult.getErrorMessage());
                return true;
                
            case ScenarioStage.FAILURE_POLICY_FLAG_FOR_REVIEW:
                TestAwareLogger.warn(logger, "Stage '{}' failed - flagging for manual review", stage.getStageName());
                scenarioResult.setRequiresReview(true);
                scenarioResult.addReviewFlag("Stage '" + stage.getStageName() + "' requires manual review: " + stageResult.getErrorMessage());
                return true;
                
            default:
                TestAwareLogger.warn(logger, "Unknown failure policy '{}' for stage '{}' - treating as terminate", 
                                   failurePolicy, stage.getStageName());
                scenarioResult.setTerminated(true);
                return false;
        }
    }
}
