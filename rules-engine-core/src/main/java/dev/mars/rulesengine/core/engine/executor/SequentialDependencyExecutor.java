package dev.mars.rulesengine.core.engine.executor;

import dev.mars.rulesengine.core.config.yaml.YamlRuleChain;
import dev.mars.rulesengine.core.engine.context.ChainedEvaluationContext;
import dev.mars.rulesengine.core.engine.model.Rule;
import dev.mars.rulesengine.core.engine.model.RuleChainResult;
import dev.mars.rulesengine.core.service.engine.ExpressionEvaluatorService;
import dev.mars.rulesengine.core.service.engine.RuleEngineService;

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
 * Executor for Pattern 2: Sequential Dependency.
 * Each rule builds upon results from the previous rule.
 * 
 * YAML Configuration Example:
 * ```yaml
 * rule-chains:
 *   - id: "discount-calculation-pipeline"
 *     pattern: "sequential-dependency"
 *     configuration:
 *       stages:
 *         - stage: 1
 *           name: "Base Discount Calculation"
 *           rule:
 *             id: "base-discount"
 *             condition: "#customerTier == 'GOLD' ? 0.15 : (#customerTier == 'SILVER' ? 0.10 : 0.05)"
 *             message: "Base discount calculated"
 *           output-variable: "baseDiscount"
 *         - stage: 2
 *           name: "Regional Multiplier"
 *           rule:
 *             id: "regional-multiplier"
 *             condition: "#region == 'US' ? #baseDiscount * 1.2 : #baseDiscount"
 *             message: "Regional multiplier applied"
 *           output-variable: "finalDiscount"
 * ```
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-07-28
 * @version 1.0
 */
public class SequentialDependencyExecutor extends PatternExecutor {
    
    public SequentialDependencyExecutor(RuleEngineService ruleEngineService, ExpressionEvaluatorService evaluatorService) {
        super(ruleEngineService, evaluatorService);
    }
    
    @Override
    public RuleChainResult execute(YamlRuleChain ruleChain, Map<String, Object> configuration, ChainedEvaluationContext context) {
        logger.info("Executing sequential dependency pattern for rule chain: " + ruleChain.getId());
        
        if (!validateConfiguration(configuration)) {
            return createValidationFailure(ruleChain, "Invalid sequential dependency configuration");
        }
        
        RuleChainResult.Builder resultBuilder = RuleChainResult.builder(ruleChain.getId(), getPatternName())
                .ruleChainName(ruleChain.getName());
        
        try {
            // Get stages configuration
            List<Object> stagesConfig = getListValue(configuration, "stages");
            if (stagesConfig == null || stagesConfig.isEmpty()) {
                return createValidationFailure(ruleChain, "stages configuration is missing or empty");
            }
            
            logger.info("Executing " + stagesConfig.size() + " sequential stages");
            
            // Execute stages in order
            for (int i = 0; i < stagesConfig.size(); i++) {
                Object stageObj = stagesConfig.get(i);
                if (!(stageObj instanceof Map)) {
                    return createValidationFailure(ruleChain, "Stage at index " + i + " must be a map");
                }
                
                @SuppressWarnings("unchecked")
                Map<String, Object> stageConfig = (Map<String, Object>) stageObj;
                
                if (!executeStage(stageConfig, i + 1, context, resultBuilder)) {
                    // Stage execution failed
                    return resultBuilder.errorMessage("Stage " + (i + 1) + " execution failed").build();
                }
            }
            
            resultBuilder.finalOutcome("SEQUENTIAL_PIPELINE_COMPLETED");
            return resultBuilder.successful(true).build();
            
        } catch (Exception e) {
            logger.severe("Error executing sequential dependency: " + e.getMessage());
            return resultBuilder.errorMessage("Execution error: " + e.getMessage()).build();
        }
    }
    
    /**
     * Execute a single stage in the sequential pipeline.
     * 
     * @param stageConfig The stage configuration
     * @param stageNumber The stage number (1-based)
     * @param context The chained evaluation context
     * @param resultBuilder The result builder
     * @return true if stage executed successfully, false otherwise
     */
    private boolean executeStage(Map<String, Object> stageConfig, int stageNumber, 
                               ChainedEvaluationContext context, RuleChainResult.Builder resultBuilder) {
        try {
            // Get stage metadata
            String stageName = getStringValue(stageConfig, "name", "Stage " + stageNumber);
            String outputVariable = getStringValue(stageConfig, "output-variable", null);
            
            logger.info("Executing stage " + stageNumber + ": " + stageName);
            context.setCurrentStage(stageName);
            
            // Get rule configuration
            Map<String, Object> ruleConfig = getMapValue(stageConfig, "rule");
            if (ruleConfig == null) {
                logger.warning("Stage " + stageNumber + " missing rule configuration");
                return false;
            }
            
            // Create and execute the rule
            Rule stageRule = createRuleFromConfig(ruleConfig);
            
            // For sequential dependency, we evaluate the rule condition as an expression
            // to get the computed result, not just true/false
            String condition = stageRule.getCondition();
            Object stageResult;
            
            try {
                // Try to evaluate as an expression to get the actual result
                stageResult = evaluatorService.evaluate(condition, context, Object.class);
                logger.info("Stage " + stageNumber + " result: " + stageResult);
                
                // Also execute as a rule for tracking purposes
                executeRule(stageRule, context, resultBuilder);
                
            } catch (Exception e) {
                logger.warning("Error evaluating stage " + stageNumber + " expression: " + e.getMessage());
                return false;
            }
            
            // Store the stage result
            String resultKey = outputVariable != null ? outputVariable : ("stage" + stageNumber + "Result");
            context.addStageResult(resultKey, stageResult);
            resultBuilder.addStageResult(resultKey, stageResult);
            
            // Make the result available as a variable for subsequent stages
            context.setVariable(resultKey, stageResult);
            
            logger.info("Stage " + stageNumber + " completed successfully, result stored as: " + resultKey);
            return true;
            
        } catch (Exception e) {
            logger.severe("Error executing stage " + stageNumber + ": " + e.getMessage());
            return false;
        }
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
        for (int i = 0; i < stagesConfig.size(); i++) {
            Object stageObj = stagesConfig.get(i);
            if (!(stageObj instanceof Map)) {
                logger.warning("Stage at index " + i + " must be a map");
                return false;
            }
            
            @SuppressWarnings("unchecked")
            Map<String, Object> stageConfig = (Map<String, Object>) stageObj;
            
            if (!validateStageConfiguration(stageConfig, i)) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Validate a single stage configuration.
     * 
     * @param stageConfig The stage configuration to validate
     * @param stageIndex The stage index for error reporting
     * @return true if valid, false otherwise
     */
    private boolean validateStageConfiguration(Map<String, Object> stageConfig, int stageIndex) {
        // Validate rule configuration
        if (!hasRequiredKey(stageConfig, "rule")) {
            logger.warning("Stage at index " + stageIndex + " missing required key: rule");
            return false;
        }
        
        Map<String, Object> ruleConfig = getMapValue(stageConfig, "rule");
        if (ruleConfig == null) {
            logger.warning("Stage at index " + stageIndex + " rule must be a map");
            return false;
        }
        
        if (!hasRequiredKey(ruleConfig, "condition")) {
            logger.warning("Stage at index " + stageIndex + " rule missing required condition");
            return false;
        }
        
        // Stage number is optional but if present should be valid
        if (stageConfig.containsKey("stage")) {
            Object stageValue = stageConfig.get("stage");
            if (!(stageValue instanceof Number)) {
                logger.warning("Stage at index " + stageIndex + " stage number must be a number");
                return false;
            }
        }
        
        return true;
    }
    
    @Override
    public String getPatternName() {
        return "sequential-dependency";
    }
}
