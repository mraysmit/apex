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

import dev.mars.apex.core.config.yaml.YamlConfigurationLoader;
import dev.mars.apex.core.config.yaml.YamlRuleConfiguration;
import dev.mars.apex.core.engine.config.RulesEngine;
import dev.mars.apex.core.engine.model.RuleResult;
import dev.mars.apex.core.config.yaml.YamlRuleFactory;
import dev.mars.apex.core.service.enrichment.EnrichmentService;
import dev.mars.apex.core.service.engine.ExpressionEvaluatorService;
import dev.mars.apex.core.service.error.ErrorRecoveryService;
import dev.mars.apex.core.service.lookup.LookupServiceRegistry;
import dev.mars.apex.core.service.monitoring.RulePerformanceMonitor;
import dev.mars.apex.core.util.TestAwareLogger;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Executor for processing scenario stages with dependency management and failure policies.
 * 
 * Follows the existing pattern from ComplexWorkflowExecutor and SequentialDependencyExecutor
 * but specialized for financial trade processing workflows. Handles stage dependencies,
 * failure policies, and provides comprehensive result tracking.
 * 
 * EXECUTION FEATURES:
 * - Dependency-aware stage execution
 * - Configurable failure policies per stage
 * - Performance monitoring and SLA tracking
 * - Comprehensive error handling and recovery
 * - Context sharing between stages
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
    private final EnrichmentService enrichmentService;

    public ScenarioStageExecutor() {
        this.configLoader = new YamlConfigurationLoader();
        this.ruleFactory = new YamlRuleFactory();
        // Create enrichment service with default dependencies
        LookupServiceRegistry serviceRegistry = new LookupServiceRegistry();
        ExpressionEvaluatorService expressionEvaluator = new ExpressionEvaluatorService();
        this.enrichmentService = new EnrichmentService(serviceRegistry, expressionEvaluator);
    }

    public ScenarioStageExecutor(YamlConfigurationLoader configLoader, YamlRuleFactory ruleFactory) {
        this.configLoader = configLoader != null ? configLoader : new YamlConfigurationLoader();
        this.ruleFactory = ruleFactory != null ? ruleFactory : new YamlRuleFactory();
        // Create enrichment service with default dependencies
        LookupServiceRegistry serviceRegistry = new LookupServiceRegistry();
        ExpressionEvaluatorService expressionEvaluator = new ExpressionEvaluatorService();
        this.enrichmentService = new EnrichmentService(serviceRegistry, expressionEvaluator);
    }

    public ScenarioStageExecutor(YamlConfigurationLoader configLoader, YamlRuleFactory ruleFactory, EnrichmentService enrichmentService) {
        this.configLoader = configLoader != null ? configLoader : new YamlConfigurationLoader();
        this.ruleFactory = ruleFactory != null ? ruleFactory : new YamlRuleFactory();
        this.enrichmentService = enrichmentService != null ? enrichmentService : createDefaultEnrichmentService();
    }

    private EnrichmentService createDefaultEnrichmentService() {
        LookupServiceRegistry serviceRegistry = new LookupServiceRegistry();
        ExpressionEvaluatorService expressionEvaluator = new ExpressionEvaluatorService();
        return new EnrichmentService(serviceRegistry, expressionEvaluator);
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
            if (!shouldExecuteStage(stage, result)) {
                String reason = getDependencyFailureReason(stage, result);
                logger.info("Skipping stage '{}' due to dependencies: {}", stage.getStageName(), reason);
                result.addSkippedStage(stage.getStageName(), reason);
                currentStageIndex++;
                continue;
            }

            long stageStartTime = System.currentTimeMillis();
            StageExecutionResult stageResult = executeStage(stage, data, result);
            stageResult.setExecutionTimeMs(System.currentTimeMillis() - stageStartTime);

            result.addStageResult(stageResult);

            logger.info("Stage '{}' completed: {}", stage.getStageName(), stageResult.getExecutionSummary());

            if (!handleStageResult(stage, stageResult, result)) {
                logger.warn("Terminating scenario execution due to stage '{}' failure policy", stage.getStageName());

                // Mark remaining stages as skipped due to termination
                for (int i = currentStageIndex + 1; i < stages.size(); i++) {
                    ScenarioStage remainingStage = stages.get(i);
                    result.addSkippedStage(remainingStage.getStageName(), "Scenario terminated due to previous stage failure");
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
     * Checks if a stage should be executed based on its dependencies.
     * 
     * @param stage the stage to check
     * @param result the current execution result
     * @return true if the stage should be executed
     */
    private boolean shouldExecuteStage(ScenarioStage stage, ScenarioExecutionResult result) {
        if (!stage.hasDependencies()) {
            return true; // No dependencies, can execute
        }
        
        // Check if all dependencies are satisfied
        for (String dependency : stage.getDependsOn()) {
            if (!result.isStageSuccessful(dependency)) {
                logger.debug("Stage '{}' dependency '{}' not satisfied", stage.getStageName(), dependency);
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Gets the reason why a stage cannot be executed due to dependency failures.
     * 
     * @param stage the stage
     * @param result the current execution result
     * @return reason for dependency failure
     */
    private String getDependencyFailureReason(ScenarioStage stage, ScenarioExecutionResult result) {
        if (!stage.hasDependencies()) {
            return "No dependencies";
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
            
            // Load stage configuration
            YamlRuleConfiguration stageConfig = configLoader.loadFromFile(stage.getConfigFile());
            
            // Create rules engine for this stage with enrichment service support
            RulesEngine stageEngine = new RulesEngine(
                ruleFactory.createRulesEngineConfiguration(stageConfig),
                new SpelExpressionParser(),
                new ErrorRecoveryService(),
                new RulePerformanceMonitor(),
                enrichmentService
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
            
        } catch (Exception e) {
            TestAwareLogger.error(logger, "Error executing stage '{}': {}", stage.getStageName(), e.getMessage(), e);
            
            String errorMessage = "Stage execution exception: " + e.getMessage();
            return stage.isRequired() ? 
                StageExecutionResult.criticalFailure(stage.getStageName(), errorMessage) :
                StageExecutionResult.failure(stage.getStageName(), errorMessage);
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
