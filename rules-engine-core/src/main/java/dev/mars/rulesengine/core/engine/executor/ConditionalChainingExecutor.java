package dev.mars.rulesengine.core.engine.executor;

import dev.mars.rulesengine.core.config.yaml.YamlRuleChain;
import dev.mars.rulesengine.core.engine.context.ChainedEvaluationContext;
import dev.mars.rulesengine.core.engine.model.Rule;
import dev.mars.rulesengine.core.engine.model.RuleChainResult;
import dev.mars.rulesengine.core.engine.model.RuleResult;
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
 * Executor for Pattern 1: Conditional Chaining.
 * Execute Rule B only if Rule A triggers.
 * 
 * YAML Configuration Example:
 * ```yaml
 * rule-chains:
 *   - id: "high-value-processing"
 *     pattern: "conditional-chaining"
 *     configuration:
 *       trigger-rule:
 *         id: "high-value-check"
 *         condition: "#customerType == 'PREMIUM' && #transactionAmount > 100000"
 *         message: "High-value customer transaction detected"
 *       conditional-rules:
 *         on-trigger:
 *           - id: "enhanced-due-diligence"
 *             condition: "#accountAge >= 3"
 *             message: "Enhanced due diligence check"
 *         on-no-trigger:
 *           - id: "standard-processing"
 *             condition: "true"
 *             message: "Standard processing applied"
 * ```
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-07-28
 * @version 1.0
 */
public class ConditionalChainingExecutor extends PatternExecutor {
    
    public ConditionalChainingExecutor(RuleEngineService ruleEngineService, ExpressionEvaluatorService evaluatorService) {
        super(ruleEngineService, evaluatorService);
    }
    
    @Override
    public RuleChainResult execute(YamlRuleChain ruleChain, Map<String, Object> configuration, ChainedEvaluationContext context) {
        logger.info("Executing conditional chaining pattern for rule chain: " + ruleChain.getId());
        
        if (!validateConfiguration(configuration)) {
            return createValidationFailure(ruleChain, "Invalid conditional chaining configuration");
        }
        
        RuleChainResult.Builder resultBuilder = RuleChainResult.builder(ruleChain.getId(), getPatternName())
                .ruleChainName(ruleChain.getName());
        
        try {
            // Get trigger rule configuration
            Map<String, Object> triggerRuleConfig = getMapValue(configuration, "trigger-rule");
            if (triggerRuleConfig == null) {
                return createValidationFailure(ruleChain, "trigger-rule configuration is missing");
            }
            
            // Create and execute trigger rule
            Rule triggerRule = createRuleFromConfig(triggerRuleConfig);
            context.setCurrentStage("trigger-evaluation");
            
            RuleResult triggerResult = executeRule(triggerRule, context, resultBuilder);
            
            // Store trigger result for conditional execution
            context.addStageResult("triggerResult", triggerResult.isTriggered());
            resultBuilder.addStageResult("triggerResult", triggerResult.isTriggered());
            
            // Get conditional rules configuration
            Map<String, Object> conditionalRulesConfig = getMapValue(configuration, "conditional-rules");
            if (conditionalRulesConfig == null) {
                return createValidationFailure(ruleChain, "conditional-rules configuration is missing");
            }
            
            // Execute appropriate conditional rules based on trigger result
            if (triggerResult.isTriggered()) {
                logger.info("Trigger rule fired, executing on-trigger rules");
                context.setCurrentStage("on-trigger-execution");
                executeConditionalRules(conditionalRulesConfig, "on-trigger", context, resultBuilder);
                resultBuilder.finalOutcome("TRIGGERED_PATH_COMPLETED");
            } else {
                logger.info("Trigger rule did not fire, executing on-no-trigger rules");
                context.setCurrentStage("on-no-trigger-execution");
                executeConditionalRules(conditionalRulesConfig, "on-no-trigger", context, resultBuilder);
                resultBuilder.finalOutcome("NON_TRIGGERED_PATH_COMPLETED");
            }
            
            return resultBuilder.successful(true).build();
            
        } catch (Exception e) {
            logger.severe("Error executing conditional chaining: " + e.getMessage());
            return resultBuilder.errorMessage("Execution error: " + e.getMessage()).build();
        }
    }
    
    /**
     * Execute conditional rules based on the trigger result.
     * 
     * @param conditionalRulesConfig The conditional rules configuration
     * @param ruleSetKey The key for the rule set to execute ("on-trigger" or "on-no-trigger")
     * @param context The chained evaluation context
     * @param resultBuilder The result builder
     */
    private void executeConditionalRules(Map<String, Object> conditionalRulesConfig, String ruleSetKey, 
                                       ChainedEvaluationContext context, RuleChainResult.Builder resultBuilder) {
        List<Object> ruleConfigs = getListValue(conditionalRulesConfig, ruleSetKey);
        if (ruleConfigs == null || ruleConfigs.isEmpty()) {
            logger.info("No " + ruleSetKey + " rules configured, skipping");
            return;
        }
        
        logger.info("Executing " + ruleConfigs.size() + " " + ruleSetKey + " rules");
        
        for (Object ruleConfigObj : ruleConfigs) {
            if (ruleConfigObj instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> ruleConfig = (Map<String, Object>) ruleConfigObj;
                
                Rule conditionalRule = createRuleFromConfig(ruleConfig);
                RuleResult result = executeRule(conditionalRule, context, resultBuilder);
                
                // Store individual rule results
                String resultKey = ruleSetKey + "_" + conditionalRule.getName() + "_result";
                context.addStageResult(resultKey, result.isTriggered());
                resultBuilder.addStageResult(resultKey, result.isTriggered());
            }
        }
    }
    
    @Override
    public boolean validateConfiguration(Map<String, Object> configuration) {
        if (configuration == null) {
            logger.warning("Configuration is null");
            return false;
        }
        
        // Validate trigger-rule configuration
        if (!hasRequiredKey(configuration, "trigger-rule")) {
            logger.warning("Missing required key: trigger-rule");
            return false;
        }
        
        Map<String, Object> triggerRuleConfig = getMapValue(configuration, "trigger-rule");
        if (triggerRuleConfig == null) {
            logger.warning("trigger-rule must be a map");
            return false;
        }
        
        if (!hasRequiredKey(triggerRuleConfig, "condition")) {
            logger.warning("trigger-rule missing required condition");
            return false;
        }
        
        // Validate conditional-rules configuration
        if (!hasRequiredKey(configuration, "conditional-rules")) {
            logger.warning("Missing required key: conditional-rules");
            return false;
        }
        
        Map<String, Object> conditionalRulesConfig = getMapValue(configuration, "conditional-rules");
        if (conditionalRulesConfig == null) {
            logger.warning("conditional-rules must be a map");
            return false;
        }
        
        // At least one of on-trigger or on-no-trigger should be present
        if (!conditionalRulesConfig.containsKey("on-trigger") && !conditionalRulesConfig.containsKey("on-no-trigger")) {
            logger.warning("conditional-rules must contain at least one of 'on-trigger' or 'on-no-trigger'");
            return false;
        }
        
        // Validate rule lists if present
        if (conditionalRulesConfig.containsKey("on-trigger")) {
            List<Object> onTriggerRules = getListValue(conditionalRulesConfig, "on-trigger");
            if (onTriggerRules == null) {
                logger.warning("on-trigger must be a list");
                return false;
            }
            
            if (!validateRuleList(onTriggerRules, "on-trigger")) {
                return false;
            }
        }
        
        if (conditionalRulesConfig.containsKey("on-no-trigger")) {
            List<Object> onNoTriggerRules = getListValue(conditionalRulesConfig, "on-no-trigger");
            if (onNoTriggerRules == null) {
                logger.warning("on-no-trigger must be a list");
                return false;
            }
            
            if (!validateRuleList(onNoTriggerRules, "on-no-trigger")) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Validate a list of rule configurations.
     * 
     * @param ruleList The list of rule configurations
     * @param listName The name of the list for error reporting
     * @return true if valid, false otherwise
     */
    private boolean validateRuleList(List<Object> ruleList, String listName) {
        for (int i = 0; i < ruleList.size(); i++) {
            Object ruleObj = ruleList.get(i);
            if (!(ruleObj instanceof Map)) {
                logger.warning(listName + " rule at index " + i + " must be a map");
                return false;
            }
            
            @SuppressWarnings("unchecked")
            Map<String, Object> ruleConfig = (Map<String, Object>) ruleObj;
            
            if (!hasRequiredKey(ruleConfig, "condition")) {
                logger.warning(listName + " rule at index " + i + " missing required condition");
                return false;
            }
        }
        return true;
    }
    
    @Override
    public String getPatternName() {
        return "conditional-chaining";
    }
}
