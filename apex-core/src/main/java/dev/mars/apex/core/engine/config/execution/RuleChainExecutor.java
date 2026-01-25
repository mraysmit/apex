/*
 * Copyright 2024 Mars Raysmit
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
package dev.mars.apex.core.engine.config.execution;

import dev.mars.apex.core.config.yaml.YamlEnrichmentGroup;
import dev.mars.apex.core.config.yaml.YamlRuleChain;
import dev.mars.apex.core.config.yaml.YamlRuleConfiguration;
import dev.mars.apex.core.constants.SeverityConstants;
import dev.mars.apex.core.engine.model.Category;
import dev.mars.apex.core.engine.model.EnrichmentGroup;
import dev.mars.apex.core.engine.model.Rule;
import dev.mars.apex.core.engine.model.RuleResult;
import dev.mars.apex.core.service.enrichment.EnrichmentGroupFactory;
import dev.mars.apex.core.service.engine.UnifiedRuleEvaluator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Executor for rule chain processing logic.
 * Handles different rule chain patterns including result-based routing and conditional chaining.
 * 
 * <p>This class extracts rule chain execution logic from RulesEngine to maintain
 * focused responsibilities. It supports multiple chain patterns for complex routing logic.</p>
 * 
 * @since 2.1 (Phase 6 refactoring)
 */
public class RuleChainExecutor {
    private static final Logger logger = LoggerFactory.getLogger(RuleChainExecutor.class);
    
    private final ExpressionParser parser;
    private final UnifiedRuleEvaluator unifiedEvaluator;
    private final EnrichmentGroupExecutor enrichmentGroupExecutor;
    
    public RuleChainExecutor(ExpressionParser parser, 
                            UnifiedRuleEvaluator unifiedEvaluator,
                            EnrichmentGroupExecutor enrichmentGroupExecutor) {
        this.parser = parser;
        this.unifiedEvaluator = unifiedEvaluator;
        this.enrichmentGroupExecutor = enrichmentGroupExecutor;
    }
    
    /**
     * Process a single rule chain by ID.
     *
     * @param chainId The rule chain ID to process
     * @param yamlConfig The YAML configuration
     * @param data The data to evaluate
     * @param contextFactory Function to create evaluation context
     * @return RuleResult from processing the rule chain
     */
    public RuleResult processRuleChain(String chainId, 
                                      YamlRuleConfiguration yamlConfig, 
                                      Map<String, Object> data,
                                      java.util.function.Function<Map<String, Object>, StandardEvaluationContext> contextFactory) {
        logger.debug("processRuleChain() entry - chainId: '{}', data keys: {}", chainId, data.keySet());
        
        // Find rule chain in yamlConfig
        YamlRuleChain chain = findRuleChainById(yamlConfig, chainId);
        if (chain == null) {
            logger.warn("Rule chain not found: {}", chainId);
            return RuleResult.error("rule-chain:" + chainId, "Rule chain not found");
        }

        if (!chain.isEnabled()) {
            logger.info("Rule chain '{}' is disabled, skipping", chainId);
            logger.debug("processRuleChain() - chain disabled, returning no-match");
            return RuleResult.noMatch(chainId, "Rule chain disabled", SeverityConstants.INFO);
        }

        logger.info("Processing rule chain: {} (Pattern: {})", chain.getName(), chain.getPattern());
        logger.debug("processRuleChain() - chain '{}' has configuration: {}", chainId, 
                    chain.getConfiguration() != null ? "yes" : "no");

        // Handle different patterns
        if ("conditional-chaining".equals(chain.getPattern())) {
            logger.debug("processRuleChain() - executing conditional-chaining pattern");
            return executeConditionalChainingPattern(chain, data, contextFactory);
        } else if ("result-based-routing".equals(chain.getPattern())) {
            logger.debug("processRuleChain() - executing result-based-routing pattern");
            return executeResultBasedRoutingPattern(chain, yamlConfig, data, contextFactory);
        } else {
            logger.warn("Rule chain pattern '{}' not yet supported", chain.getPattern());
            return RuleResult.noMatch(chainId, "Pattern not supported: " + chain.getPattern(), SeverityConstants.INFO);
        }
    }
    
    /**
     * Find a rule chain by ID in the configuration.
     *
     * @param config The YAML configuration
     * @param chainId The rule chain ID to find
     * @return The YamlRuleChain if found, null otherwise
     */
    private YamlRuleChain findRuleChainById(YamlRuleConfiguration config, String chainId) {
        if (config.getRuleChains() != null) {
            for (YamlRuleChain chain : config.getRuleChains()) {
                if (chainId.equals(chain.getId())) {
                    return chain;
                }
            }
        }
        return null;
    }
    
    /**
     * Execute a rule chain with the 'result-based-routing' pattern.
     *
     * @param chain The rule chain to execute
     * @param yamlConfig The YAML configuration
     * @param data The data to evaluate
     * @param contextFactory Function to create evaluation context
     * @return RuleResult from execution
     */
    @SuppressWarnings("unchecked")
    private RuleResult executeResultBasedRoutingPattern(YamlRuleChain chain, 
                                                       YamlRuleConfiguration yamlConfig, 
                                                       Map<String, Object> data,
                                                       java.util.function.Function<Map<String, Object>, StandardEvaluationContext> contextFactory) {
        Map<String, Object> config = chain.getConfiguration();
        if (config == null) {
            return RuleResult.error(chain.getId(), "Missing configuration for rule chain");
        }

        // 1. Evaluate Router Rule
        Map<String, Object> routerRuleConfig = (Map<String, Object>) config.get("router-rule");
        if (routerRuleConfig == null) {
            return RuleResult.error(chain.getId(), "Missing router-rule configuration");
        }

        String condition = (String) routerRuleConfig.get("condition");
        String resultField = (String) routerRuleConfig.get("result-field");

        StandardEvaluationContext context = contextFactory.apply(data);
        String routeKey = null;
        try {
            Expression exp = parser.parseExpression(condition);
            Object result = exp.getValue(context);
            routeKey = result != null ? result.toString() : "null";
        } catch (Exception e) {
            logger.error("Error evaluating router rule for chain '{}': {}", chain.getId(), e.getMessage());
            return RuleResult.error(chain.getId(), "Router evaluation failed: " + e.getMessage());
        }

        // Set result field if specified
        if (resultField != null && !resultField.isEmpty()) {
            data.put(resultField, routeKey);
            logger.debug("Set result field '{}' to {}", resultField, routeKey);
        }

        logger.info("Router evaluated to route: '{}'", routeKey);

        // 2. Execute Route Rules
        Map<String, Object> routes = (Map<String, Object>) config.get("routes");
        if (routes != null) {
            Object routeObj = routes.get(routeKey);
            List<Map<String, Object>> rulesConfig = null;
            
            if (routeObj instanceof Map) {
                Map<String, Object> routeConfig = (Map<String, Object>) routeObj;
                rulesConfig = (List<Map<String, Object>>) routeConfig.get("rules");

                // Handle enrichment groups
                List<String> enrichmentGroupRefs = null;
                if (routeConfig.containsKey("enrichment-group-references")) {
                    enrichmentGroupRefs = (List<String>) routeConfig.get("enrichment-group-references");
                } else if (routeConfig.containsKey("enrichment-groups")) {
                    enrichmentGroupRefs = (List<String>) routeConfig.get("enrichment-groups");
                }

                if (enrichmentGroupRefs != null && !enrichmentGroupRefs.isEmpty()) {
                    logger.info("Executing {} enrichment groups for route '{}'", enrichmentGroupRefs.size(), routeKey);
                    List<EnrichmentGroup> groupsToExecute = new ArrayList<>();
                    
                    for (String groupId : enrichmentGroupRefs) {
                        EnrichmentGroup group = findEnrichmentGroup(groupId, yamlConfig);
                        if (group != null) {
                            groupsToExecute.add(group);
                        } else {
                            logger.warn("Enrichment group '{}' not found for route '{}'", groupId, routeKey);
                        }
                    }
                    
                    if (!groupsToExecute.isEmpty()) {
                        RuleResult enrichmentResult = enrichmentGroupExecutor.executeEnrichmentGroupsList(groupsToExecute, data, yamlConfig);
                        if (enrichmentResult.getEnrichedData() != null) {
                            data.putAll(enrichmentResult.getEnrichedData());
                            logger.debug("Merged enriched data from route '{}' into context", routeKey);
                        }
                    }
                }
            } else if (routeObj instanceof List) {
                // Support direct list for backward compatibility or simpler syntax
                rulesConfig = (List<Map<String, Object>>) routeObj;
            }
            
            if (rulesConfig != null) {
                logger.info("Executing route '{}' for chain '{}'", routeKey, chain.getName());
                // Convert simple map configs to Rule objects and execute
                List<Rule> rules = createRulesFromConfig(rulesConfig);
                unifiedEvaluator.evaluateRules(rules, data);
                return RuleResult.match(chain.getId(), "Executed route: " + routeKey);
            } else {
                // If we executed enrichment groups but no rules, consider it a match
                if (routeObj instanceof Map) {
                    Map<String, Object> routeConfig = (Map<String, Object>) routeObj;
                    if (routeConfig.containsKey("enrichment-group-references") || routeConfig.containsKey("enrichment-groups")) {
                        return RuleResult.match(chain.getId(), "Executed route (enrichment only): " + routeKey);
                    }
                }
                
                logger.info("No rules defined for route '{}' in chain '{}'", routeKey, chain.getName());
                return RuleResult.noMatch(chain.getId(), "No rules for route: " + routeKey, SeverityConstants.INFO);
            }
        }

        return RuleResult.noMatch(chain.getId(), "No routes configuration found", SeverityConstants.WARNING);
    }
    
    /**
     * Execute a rule chain with the 'conditional-chaining' pattern.
     *
     * @param chain The rule chain to execute
     * @param data The data to evaluate
     * @param contextFactory Function to create evaluation context
     * @return RuleResult from execution
     */
    @SuppressWarnings("unchecked")
    private RuleResult executeConditionalChainingPattern(YamlRuleChain chain, 
                                                        Map<String, Object> data,
                                                        java.util.function.Function<Map<String, Object>, StandardEvaluationContext> contextFactory) {
        logger.debug("executeConditionalChainingPattern() - chain: '{}', data size: {}", 
                    chain.getId(), data.size());
        
        Map<String, Object> config = chain.getConfiguration();
        if (config == null) {
            return RuleResult.error(chain.getId(), "Missing configuration for rule chain");
        }

        // 1. Evaluate Trigger Rule
        Map<String, Object> triggerRuleConfig = (Map<String, Object>) config.get("trigger-rule");
        if (triggerRuleConfig == null) {
            return RuleResult.error(chain.getId(), "Missing trigger-rule configuration");
        }

        String condition = (String) triggerRuleConfig.get("condition");
        String message = (String) triggerRuleConfig.get("message");
        String resultField = (String) triggerRuleConfig.get("result-field");

        logger.debug("executeConditionalChainingPattern() - evaluating trigger-rule with condition: {}", condition);
        StandardEvaluationContext context = contextFactory.apply(data);
        boolean triggered = false;
        try {
            Expression exp = parser.parseExpression(condition);
            Boolean result = exp.getValue(context, Boolean.class);
            triggered = result != null && result;
            logger.debug("executeConditionalChainingPattern() - trigger-rule evaluated to: {}", triggered);
        } catch (Exception e) {
            logger.error("Error evaluating trigger rule for chain '{}': {}", chain.getId(), e.getMessage());
            logger.debug("Full stack trace for trigger rule evaluation error:", e);
            return RuleResult.error(chain.getId(), "Trigger evaluation failed: " + e.getMessage());
        }

        // Set result field if specified
        if (resultField != null && !resultField.trim().isEmpty()) {
            data.put(resultField, triggered);
            logger.debug("executeConditionalChainingPattern() - stored result-field '{}' = {}", resultField, triggered);
            logger.debug("Set result field '{}' to {}", resultField, triggered);
        }

        // 2. Execute Conditional Rules
        Map<String, Object> conditionalRules = (Map<String, Object>) config.get("conditional-rules");
        if (conditionalRules != null) {
            String sectionToExecute = triggered ? "on-trigger" : "on-no-trigger";
            List<Map<String, Object>> rulesConfig = (List<Map<String, Object>>) conditionalRules.get(sectionToExecute);
            
            if (rulesConfig != null) {
                logger.info("Executing '{}' path for chain '{}'", sectionToExecute, chain.getName());
                // Convert simple map configs to Rule objects and execute
                List<Rule> rules = createRulesFromConfig(rulesConfig);
                unifiedEvaluator.evaluateRules(rules, data);
            }
        }

        if (triggered) {
            return RuleResult.match(chain.getId(), message != null ? message : "Rule chain triggered");
        } else {
            return RuleResult.noMatch(chain.getId(), "Rule chain not triggered", SeverityConstants.INFO);
        }
    }
    
    /**
     * Find an enrichment group by ID.
     * 
     * @param groupId The enrichment group ID
     * @param yamlConfig The YAML configuration
     * @return The enrichment group if found, null otherwise
     */
    private EnrichmentGroup findEnrichmentGroup(String groupId, YamlRuleConfiguration yamlConfig) {
        // Try to find in YAML config first
        if (yamlConfig != null && yamlConfig.getEnrichmentGroups() != null) {
            for (YamlEnrichmentGroup yamlGroup : yamlConfig.getEnrichmentGroups()) {
                if (groupId.equals(yamlGroup.getId())) {
                    // Found in YAML, build it
                    List<EnrichmentGroup> groups = EnrichmentGroupFactory.buildEnrichmentGroups(yamlConfig);
                    for (EnrichmentGroup g : groups) {
                        if (groupId.equals(g.getId())) {
                            return g;
                        }
                    }
                }
            }
        }
        return null;
    }
    
    /**
     * Create Rule objects from configuration maps.
     * 
     * @param rulesConfig List of rule configuration maps
     * @return List of Rule objects
     */
    private List<Rule> createRulesFromConfig(List<Map<String, Object>> rulesConfig) {
        List<Rule> rules = new ArrayList<>();
        for (Map<String, Object> rc : rulesConfig) {
            String ruleId = (String) rc.get("id");
            String ruleCondition = (String) rc.get("condition");
            String ruleMessage = (String) rc.get("message");
            String ruleResultField = (String) rc.get("result-field");
            String ruleSeverity = (String) rc.get("severity");
            
            logger.debug("Creating rule '{}' with result-field: '{}'", ruleId, ruleResultField);
            
            // Use full constructor to include resultField and severity
            Rule r = new Rule(
                ruleId, 
                Collections.singleton(new Category("default", 100)), 
                "Rule-" + ruleId, 
                ruleCondition, 
                ruleMessage, 
                ruleMessage, 
                100, 
                ruleSeverity != null ? ruleSeverity : SeverityConstants.INFO, 
                null, // metadata
                null, // defaultValue
                null, // successCode
                null, // errorCode
                null, // mapToField
                ruleResultField // resultField
            );
            rules.add(r);
        }
        return rules;
    }
}
