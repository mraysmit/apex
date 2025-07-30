package dev.mars.apex.core.engine.executor;

import dev.mars.apex.core.config.yaml.YamlRuleChain;
import dev.mars.apex.core.engine.context.ChainedEvaluationContext;
import dev.mars.apex.core.engine.model.Rule;
import dev.mars.apex.core.engine.model.RuleChainResult;
import dev.mars.apex.core.service.engine.ExpressionEvaluatorService;
import dev.mars.apex.core.service.engine.RuleEngineService;

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
 * Executor for Pattern 3: Result-Based Routing.
 * Route to different rule sets based on previous results.
 * 
 * YAML Configuration Example:
 * ```yaml
 * rule-chains:
 *   - id: "risk-based-routing"
 *     pattern: "result-based-routing"
 *     configuration:
 *       router-rule:
 *         id: "risk-assessment"
 *         condition: "#riskScore > 70 ? 'HIGH_RISK' : (#riskScore > 30 ? 'MEDIUM_RISK' : 'LOW_RISK')"
 *         message: "Risk level determined"
 *         output-variable: "riskLevel"
 *       routes:
 *         HIGH_RISK:
 *           rules:
 *             - id: "manager-approval"
 *               condition: "#transactionAmount > 100000"
 *               message: "Manager approval required"
 *         MEDIUM_RISK:
 *           rules:
 *             - id: "auto-approval-limit"
 *               condition: "#transactionAmount <= 50000"
 *               message: "Within auto-approval limit"
 * ```
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-07-28
 * @version 1.0
 */
public class ResultBasedRoutingExecutor extends PatternExecutor {
    
    public ResultBasedRoutingExecutor(RuleEngineService ruleEngineService, ExpressionEvaluatorService evaluatorService) {
        super(ruleEngineService, evaluatorService);
    }
    
    @Override
    public RuleChainResult execute(YamlRuleChain ruleChain, Map<String, Object> configuration, ChainedEvaluationContext context) {
        logger.info("Executing result-based routing pattern for rule chain: " + ruleChain.getId());
        
        if (!validateConfiguration(configuration)) {
            return createValidationFailure(ruleChain, "Invalid result-based routing configuration");
        }
        
        RuleChainResult.Builder resultBuilder = RuleChainResult.builder(ruleChain.getId(), getPatternName())
                .ruleChainName(ruleChain.getName());
        
        try {
            // Execute router rule to determine the route
            String routeKey = executeRouterRule(configuration, context, resultBuilder);
            if (routeKey == null) {
                return resultBuilder.errorMessage("Router rule execution failed").build();
            }
            
            logger.info("Router determined route: " + routeKey);
            context.addStageResult("routeKey", routeKey);
            resultBuilder.addStageResult("routeKey", routeKey);
            
            // Execute the appropriate route
            boolean routeExecuted = executeRoute(configuration, routeKey, context, resultBuilder);
            if (!routeExecuted) {
                return resultBuilder.errorMessage("Route execution failed for: " + routeKey).build();
            }
            
            resultBuilder.finalOutcome("ROUTE_" + routeKey + "_COMPLETED");
            return resultBuilder.successful(true).build();
            
        } catch (Exception e) {
            logger.severe("Error executing result-based routing: " + e.getMessage());
            return resultBuilder.errorMessage("Execution error: " + e.getMessage()).build();
        }
    }
    
    /**
     * Execute the router rule to determine which route to take.
     * 
     * @param configuration The rule chain configuration
     * @param context The chained evaluation context
     * @param resultBuilder The result builder
     * @return The route key, or null if execution failed
     */
    private String executeRouterRule(Map<String, Object> configuration, ChainedEvaluationContext context, 
                                   RuleChainResult.Builder resultBuilder) {
        try {
            Map<String, Object> routerRuleConfig = getMapValue(configuration, "router-rule");
            if (routerRuleConfig == null) {
                logger.warning("router-rule configuration is missing");
                return null;
            }
            
            context.setCurrentStage("router-evaluation");
            
            // Create router rule
            Rule routerRule = createRuleFromConfig(routerRuleConfig);
            
            // Evaluate the router rule condition to get the route key
            String condition = routerRule.getCondition();
            Object routeResult = evaluatorService.evaluate(condition, context, Object.class);
            
            if (routeResult == null) {
                logger.warning("Router rule returned null result");
                return null;
            }
            
            String routeKey = routeResult.toString();
            
            // Also execute as a rule for tracking purposes
            executeRule(routerRule, context, resultBuilder);
            
            // Store router result if output variable is specified
            String outputVariable = getStringValue(routerRuleConfig, "output-variable", null);
            if (outputVariable != null) {
                context.addStageResult(outputVariable, routeKey);
                context.setVariable(outputVariable, routeKey);
                resultBuilder.addStageResult(outputVariable, routeKey);
            }
            
            return routeKey;
            
        } catch (Exception e) {
            logger.severe("Error executing router rule: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Execute the rules for the determined route.
     * 
     * @param configuration The rule chain configuration
     * @param routeKey The route key determined by the router rule
     * @param context The chained evaluation context
     * @param resultBuilder The result builder
     * @return true if route executed successfully, false otherwise
     */
    private boolean executeRoute(Map<String, Object> configuration, String routeKey, 
                               ChainedEvaluationContext context, RuleChainResult.Builder resultBuilder) {
        try {
            Map<String, Object> routesConfig = getMapValue(configuration, "routes");
            if (routesConfig == null) {
                logger.warning("routes configuration is missing");
                return false;
            }
            
            Map<String, Object> routeConfig = getMapValue(routesConfig, routeKey);
            if (routeConfig == null) {
                logger.warning("No route configuration found for key: " + routeKey);
                // This might be acceptable - some routes might not have specific rules
                context.addStageResult("routeExecutionResult", "NO_RULES_FOR_ROUTE");
                resultBuilder.addStageResult("routeExecutionResult", "NO_RULES_FOR_ROUTE");
                return true;
            }
            
            context.setCurrentStage("route-" + routeKey + "-execution");
            
            // Execute rules for this route
            List<Object> routeRules = getListValue(routeConfig, "rules");
            if (routeRules == null || routeRules.isEmpty()) {
                logger.info("No rules configured for route: " + routeKey);
                context.addStageResult("routeExecutionResult", "NO_RULES_CONFIGURED");
                resultBuilder.addStageResult("routeExecutionResult", "NO_RULES_CONFIGURED");
                return true;
            }
            
            logger.info("Executing " + routeRules.size() + " rules for route: " + routeKey);
            
            int executedRules = 0;
            int triggeredRules = 0;
            
            for (Object ruleObj : routeRules) {
                if (ruleObj instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> ruleConfig = (Map<String, Object>) ruleObj;
                    
                    Rule routeRule = createRuleFromConfig(ruleConfig);
                    var result = executeRule(routeRule, context, resultBuilder);
                    
                    executedRules++;
                    if (result.isTriggered()) {
                        triggeredRules++;
                    }
                    
                    // Store individual rule result
                    String resultKey = "route_" + routeKey + "_" + routeRule.getName() + "_result";
                    context.addStageResult(resultKey, result.isTriggered());
                    resultBuilder.addStageResult(resultKey, result.isTriggered());
                }
            }
            
            // Store route execution summary
            context.addStageResult("routeExecutionResult", "COMPLETED");
            context.addStageResult("routeExecutedRules", executedRules);
            context.addStageResult("routeTriggeredRules", triggeredRules);
            
            resultBuilder.addStageResult("routeExecutionResult", "COMPLETED");
            resultBuilder.addStageResult("routeExecutedRules", executedRules);
            resultBuilder.addStageResult("routeTriggeredRules", triggeredRules);
            
            logger.info("Route " + routeKey + " completed: " + executedRules + " rules executed, " + triggeredRules + " triggered");
            return true;
            
        } catch (Exception e) {
            logger.severe("Error executing route " + routeKey + ": " + e.getMessage());
            return false;
        }
    }
    
    @Override
    public boolean validateConfiguration(Map<String, Object> configuration) {
        if (configuration == null) {
            logger.warning("Configuration is null");
            return false;
        }
        
        // Validate router-rule configuration
        if (!hasRequiredKey(configuration, "router-rule")) {
            logger.warning("Missing required key: router-rule");
            return false;
        }
        
        Map<String, Object> routerRuleConfig = getMapValue(configuration, "router-rule");
        if (routerRuleConfig == null) {
            logger.warning("router-rule must be a map");
            return false;
        }
        
        if (!hasRequiredKey(routerRuleConfig, "condition")) {
            logger.warning("router-rule missing required condition");
            return false;
        }
        
        // Validate routes configuration
        if (!hasRequiredKey(configuration, "routes")) {
            logger.warning("Missing required key: routes");
            return false;
        }
        
        Map<String, Object> routesConfig = getMapValue(configuration, "routes");
        if (routesConfig == null) {
            logger.warning("routes must be a map");
            return false;
        }
        
        if (routesConfig.isEmpty()) {
            logger.warning("routes map cannot be empty");
            return false;
        }
        
        // Validate each route configuration
        for (Map.Entry<String, Object> routeEntry : routesConfig.entrySet()) {
            String routeKey = routeEntry.getKey();
            Object routeValue = routeEntry.getValue();
            
            if (!(routeValue instanceof Map)) {
                logger.warning("Route '" + routeKey + "' configuration must be a map");
                return false;
            }
            
            @SuppressWarnings("unchecked")
            Map<String, Object> routeConfig = (Map<String, Object>) routeValue;
            
            // Routes can have rules, but it's optional
            if (routeConfig.containsKey("rules")) {
                List<Object> routeRules = getListValue(routeConfig, "rules");
                if (routeRules == null) {
                    logger.warning("Route '" + routeKey + "' rules must be a list");
                    return false;
                }
                
                // Validate each rule in the route
                for (int i = 0; i < routeRules.size(); i++) {
                    Object ruleObj = routeRules.get(i);
                    if (!(ruleObj instanceof Map)) {
                        logger.warning("Route '" + routeKey + "' rule at index " + i + " must be a map");
                        return false;
                    }
                    
                    @SuppressWarnings("unchecked")
                    Map<String, Object> ruleConfig = (Map<String, Object>) ruleObj;
                    
                    if (!hasRequiredKey(ruleConfig, "condition")) {
                        logger.warning("Route '" + routeKey + "' rule at index " + i + " missing required condition");
                        return false;
                    }
                }
            }
        }
        
        return true;
    }
    
    @Override
    public String getPatternName() {
        return "result-based-routing";
    }
}
