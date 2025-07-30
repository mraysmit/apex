package dev.mars.apex.core.engine.executor;

import dev.mars.apex.core.config.yaml.YamlRuleChain;
import dev.mars.apex.core.engine.context.ChainedEvaluationContext;
import dev.mars.apex.core.engine.model.Rule;
import dev.mars.apex.core.engine.model.RuleChainResult;
import dev.mars.apex.core.engine.model.RuleResult;
import dev.mars.apex.core.service.engine.ExpressionEvaluatorService;
import dev.mars.apex.core.service.engine.RuleEngineService;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

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
 * Base class for pattern-specific rule chain executors.
 * Provides common functionality and utilities for all pattern executors.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-07-28
 * @version 1.0
 */
public abstract class PatternExecutor {
    
    protected final Logger logger;
    protected final RuleEngineService ruleEngineService;
    protected final ExpressionEvaluatorService evaluatorService;
    
    /**
     * Create a new pattern executor.
     * 
     * @param ruleEngineService The rule engine service for executing individual rules
     * @param evaluatorService The expression evaluator service for evaluating SpEL expressions
     */
    protected PatternExecutor(RuleEngineService ruleEngineService, ExpressionEvaluatorService evaluatorService) {
        this.logger = Logger.getLogger(this.getClass().getName());
        this.ruleEngineService = ruleEngineService;
        this.evaluatorService = evaluatorService;
    }
    
    /**
     * Execute the pattern-specific rule chain logic.
     * 
     * @param ruleChain The rule chain configuration
     * @param configuration The pattern-specific configuration
     * @param context The chained evaluation context
     * @return The result of the pattern execution
     */
    public abstract RuleChainResult execute(YamlRuleChain ruleChain, Map<String, Object> configuration, ChainedEvaluationContext context);
    
    /**
     * Validate the pattern-specific configuration.
     * 
     * @param configuration The configuration to validate
     * @return true if valid, false otherwise
     */
    public abstract boolean validateConfiguration(Map<String, Object> configuration);
    
    /**
     * Get the pattern name handled by this executor.
     * 
     * @return The pattern name
     */
    public abstract String getPatternName();
    
    /**
     * Execute a single rule and add the result to the chain context.
     * 
     * @param rule The rule to execute
     * @param context The chained evaluation context
     * @param resultBuilder The result builder to add the rule result to
     * @return The rule result
     */
    protected RuleResult executeRule(Rule rule, ChainedEvaluationContext context, RuleChainResult.Builder resultBuilder) {
        try {
            context.setCurrentRule(rule.getName());
            context.addToExecutionPath(rule.getName());
            
            logger.fine("Executing rule: " + rule.getName());
            
            List<RuleResult> results = ruleEngineService.evaluateRules(Arrays.asList(rule), context);
            RuleResult result = results.isEmpty() ? RuleResult.noMatch() : results.get(0);
            
            resultBuilder.addRuleResult(result);
            resultBuilder.addToExecutionPath(rule.getName());
            
            logger.fine("Rule '" + rule.getName() + "' result: " + (result.isTriggered() ? "TRIGGERED" : "NOT TRIGGERED"));
            
            return result;
            
        } catch (Exception e) {
            logger.warning("Error executing rule '" + rule.getName() + "': " + e.getMessage());
            RuleResult errorResult = RuleResult.error(rule.getName(), "Execution error: " + e.getMessage());
            resultBuilder.addRuleResult(errorResult);
            return errorResult;
        }
    }
    
    /**
     * Evaluate a SpEL expression and return the result.
     * 
     * @param expression The SpEL expression to evaluate
     * @param context The evaluation context
     * @param resultType The expected result type
     * @param <T> The result type
     * @return The evaluation result
     */
    protected <T> T evaluateExpression(String expression, ChainedEvaluationContext context, Class<T> resultType) {
        try {
            return evaluatorService.evaluate(expression, context, resultType);
        } catch (Exception e) {
            logger.warning("Error evaluating expression '" + expression + "': " + e.getMessage());
            throw new RuntimeException("Expression evaluation failed: " + expression, e);
        }
    }
    
    /**
     * Create a rule from a map configuration.
     * 
     * @param ruleConfig The rule configuration map
     * @return The created rule
     */
    protected Rule createRuleFromConfig(Map<String, Object> ruleConfig) {
        String id = getStringValue(ruleConfig, "id", "rule-" + System.currentTimeMillis());
        String name = getStringValue(ruleConfig, "name", id);
        String condition = getStringValue(ruleConfig, "condition", "true");
        String message = getStringValue(ruleConfig, "message", "Rule " + name + " executed");
        
        return new Rule(name, condition, message);
    }
    
    /**
     * Get a string value from a configuration map with a default value.
     * 
     * @param config The configuration map
     * @param key The key to look up
     * @param defaultValue The default value if key is not found
     * @return The string value
     */
    protected String getStringValue(Map<String, Object> config, String key, String defaultValue) {
        Object value = config.get(key);
        return value != null ? value.toString() : defaultValue;
    }
    
    /**
     * Get an integer value from a configuration map with a default value.
     * 
     * @param config The configuration map
     * @param key The key to look up
     * @param defaultValue The default value if key is not found
     * @return The integer value
     */
    protected Integer getIntegerValue(Map<String, Object> config, String key, Integer defaultValue) {
        Object value = config.get(key);
        if (value instanceof Integer) {
            return (Integer) value;
        } else if (value instanceof Number) {
            return ((Number) value).intValue();
        } else if (value instanceof String) {
            try {
                return Integer.parseInt((String) value);
            } catch (NumberFormatException e) {
                logger.warning("Invalid integer value for key '" + key + "': " + value);
                return defaultValue;
            }
        }
        return defaultValue;
    }
    
    /**
     * Get a boolean value from a configuration map with a default value.
     * 
     * @param config The configuration map
     * @param key The key to look up
     * @param defaultValue The default value if key is not found
     * @return The boolean value
     */
    protected Boolean getBooleanValue(Map<String, Object> config, String key, Boolean defaultValue) {
        Object value = config.get(key);
        if (value instanceof Boolean) {
            return (Boolean) value;
        } else if (value instanceof String) {
            return Boolean.parseBoolean((String) value);
        }
        return defaultValue;
    }
    
    /**
     * Get a map value from a configuration map.
     * 
     * @param config The configuration map
     * @param key The key to look up
     * @return The map value, or null if not found or not a map
     */
    @SuppressWarnings("unchecked")
    protected Map<String, Object> getMapValue(Map<String, Object> config, String key) {
        Object value = config.get(key);
        if (value instanceof Map) {
            return (Map<String, Object>) value;
        }
        return null;
    }
    
    /**
     * Get a list value from a configuration map.
     * 
     * @param config The configuration map
     * @param key The key to look up
     * @return The list value, or null if not found or not a list
     */
    @SuppressWarnings("unchecked")
    protected List<Object> getListValue(Map<String, Object> config, String key) {
        Object value = config.get(key);
        if (value instanceof List) {
            return (List<Object>) value;
        }
        return null;
    }
    
    /**
     * Check if a required configuration key exists.
     * 
     * @param config The configuration map
     * @param key The required key
     * @return true if the key exists, false otherwise
     */
    protected boolean hasRequiredKey(Map<String, Object> config, String key) {
        return config.containsKey(key) && config.get(key) != null;
    }
    
    /**
     * Create a failure result for configuration validation errors.
     * 
     * @param ruleChain The rule chain
     * @param errorMessage The error message
     * @return A failure result
     */
    protected RuleChainResult createValidationFailure(YamlRuleChain ruleChain, String errorMessage) {
        return RuleChainResult.failure(ruleChain.getId(), ruleChain.getPattern(), 
                "Configuration validation failed: " + errorMessage);
    }
}
