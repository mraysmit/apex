/*
 * * Copyright 2025 Mark Andrew Ray-Smith Cityline Ltd 
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
package dev.mars.apex.engine.execution;

import dev.mars.apex.core.config.model.YamlRuleChain;
import dev.mars.apex.core.config.model.YamlRuleConfiguration;
import dev.mars.apex.core.constants.SeverityConstants;
import dev.mars.apex.engine.model.Category;
import dev.mars.apex.engine.model.EnrichmentGroup;
import dev.mars.apex.engine.model.Rule;
import dev.mars.apex.engine.model.RuleResult;
import dev.mars.apex.engine.core.UnifiedRuleEvaluator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
 * @since 2026-01-22
 */
public class RuleChainExecutor {
    private static final Logger logger = LoggerFactory.getLogger(RuleChainExecutor.class);
    
    private final UnifiedRuleEvaluator unifiedEvaluator;
    private final EnrichmentGroupExecutor enrichmentGroupExecutor;
    
    public RuleChainExecutor(UnifiedRuleEvaluator unifiedEvaluator,
                            EnrichmentGroupExecutor enrichmentGroupExecutor) {
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
     * @param enrichmentGroupIndex Pre-built index of enrichment groups for O(1) lookup (may be null or empty)
     * @return RuleResult from processing the rule chain
     */
    public RuleResult processRuleChain(String chainId, 
                                      YamlRuleConfiguration yamlConfig, 
                                      Map<String, Object> data,
                                      java.util.function.Function<Map<String, Object>, StandardEvaluationContext> contextFactory,
                                      Map<String, EnrichmentGroup> enrichmentGroupIndex) {
        logger.debug("processRuleChain() entry - chainId: '{}', data keys: {}", chainId, data.keySet());
        
        // Find rule chain in yamlConfig
        YamlRuleChain chain = findRuleChainById(yamlConfig, chainId);
        if (chain == null) {
            logger.warn("Rule chain not found: {}", chainId);
            return RuleResult.error("rule-chain:" + chainId, "Rule chain not found");
        }

        if (!dev.mars.apex.core.util.EnabledFilter.isEnabled(chain)) {
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
            return executeResultBasedRoutingPattern(chain, yamlConfig, data, contextFactory, enrichmentGroupIndex);
        } else if ("sequential-dependency".equals(chain.getPattern())) {
            logger.debug("processRuleChain() - executing sequential-dependency pattern");
            return executeSequentialDependencyPattern(chain, yamlConfig, data, contextFactory);
        } else if ("accumulative-chaining".equals(chain.getPattern())) {
            logger.debug("processRuleChain() - executing accumulative-chaining pattern");
            return executeAccumulativeChainingPattern(chain, yamlConfig, data, contextFactory);
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
     * @param enrichmentGroupIndex Pre-built enrichment group index for O(1) lookup
     * @return RuleResult from execution
     */
    @SuppressWarnings("unchecked")
    private RuleResult executeResultBasedRoutingPattern(YamlRuleChain chain, 
                                                       YamlRuleConfiguration yamlConfig, 
                                                       Map<String, Object> data,
                                                       java.util.function.Function<Map<String, Object>, StandardEvaluationContext> contextFactory,
                                                       Map<String, EnrichmentGroup> enrichmentGroupIndex) {
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

        String routeKey = null;
        try {
            Object result = unifiedEvaluator.evaluateRouterExpression(chain.getId() + "-router", condition, data);
            routeKey = result != null ? result.toString() : "null";
        } catch (Exception e) {
            logger.error("Error evaluating router rule for chain '{}': {}", chain.getId(), e.getMessage());
            logger.debug("Full exception details:", e);
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
                        EnrichmentGroup group = findEnrichmentGroup(groupId, enrichmentGroupIndex);
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
        boolean triggered = false;
        try {
            // Route trigger-rule through UnifiedRuleEvaluator for error recovery and monitoring
            Rule triggerRule = new Rule(
                chain.getId() + "-trigger",
                Collections.singleton(new Category("chain-trigger", 100)),
                "Trigger-" + chain.getId(),
                condition,
                message != null ? message : "Rule chain trigger",
                message,
                100,
                SeverityConstants.INFO,
                null, null, null, null, null,
                resultField,
                null, true
            );
            RuleResult triggerResult = unifiedEvaluator.evaluateRule(triggerRule, data);
            triggered = triggerResult.isTriggered();
            logger.debug("executeConditionalChainingPattern() - trigger-rule evaluated to: {}", triggered);
        } catch (Exception e) {
            logger.error("Error evaluating trigger rule for chain '{}': {}", chain.getId(), e.getMessage());
            logger.debug("Full stack trace for trigger rule evaluation error:", e);
            return RuleResult.error(chain.getId(), "Trigger evaluation failed: " + e.getMessage());
        }

        // Result-field storage handled by UnifiedRuleEvaluator.evaluateRule() above

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
     * Execute a rule chain with the 'sequential-dependency' pattern.
     *
     * <p>Processes stages in order, where each stage evaluates a rule and stores
     * the result in an output-variable. Subsequent stages can reference output
     * variables from earlier stages in their conditions.</p>
     *
     * @param chain The rule chain to execute
     * @param yamlConfig The YAML configuration containing rule definitions
     * @param data The data to evaluate
     * @param contextFactory Function to create evaluation context
     * @return RuleResult from execution
     */
    @SuppressWarnings("unchecked")
    private RuleResult executeSequentialDependencyPattern(YamlRuleChain chain,
                                                         YamlRuleConfiguration yamlConfig,
                                                         Map<String, Object> data,
                                                         java.util.function.Function<Map<String, Object>, StandardEvaluationContext> contextFactory) {
        Map<String, Object> config = chain.getConfiguration();
        if (config == null) {
            return RuleResult.error(chain.getId(), "Missing configuration for rule chain");
        }

        List<Map<String, Object>> stages = (List<Map<String, Object>>) config.get("stages");
        if (stages == null || stages.isEmpty()) {
            return RuleResult.error(chain.getId(), "Missing or empty 'stages' for sequential-dependency pattern");
        }

        logger.info("Executing sequential-dependency chain '{}' with {} stages", chain.getName(), stages.size());

        for (int i = 0; i < stages.size(); i++) {
            Map<String, Object> stage = stages.get(i);
            String ruleRef = (String) stage.get("rule");
            String outputVariable = (String) stage.get("output-variable");

            if (ruleRef == null || ruleRef.isEmpty()) {
                return RuleResult.error(chain.getId(), "Stage " + (i + 1) + " missing 'rule' reference");
            }

            // Find the referenced rule in the YAML config rules section
            Rule rule = findRuleInConfig(yamlConfig, ruleRef);
            if (rule == null) {
                return RuleResult.error(chain.getId(), "Rule not found for stage " + (i + 1) + ": " + ruleRef);
            }

            logger.info("Executing stage {} of {}: rule '{}'{}", 
                        i + 1, stages.size(), ruleRef,
                        outputVariable != null ? " → " + outputVariable : "");

            // Evaluate the rule against current data (which includes output-variables from prior stages)
            RuleResult stageResult = unifiedEvaluator.evaluateRule(rule, data);

            // Store result in output-variable if configured
            if (outputVariable != null && !outputVariable.isEmpty()) {
                data.put(outputVariable, stageResult.isTriggered());
                logger.debug("Stored output-variable '{}' = {}", outputVariable, stageResult.isTriggered());
            }

            // If a stage fails, stop the chain (sequential dependency semantics)
            if (!stageResult.isTriggered()) {
                logger.info("Sequential-dependency chain '{}' stopped at stage {} (rule '{}')", 
                           chain.getName(), i + 1, ruleRef);
                return RuleResult.noMatch(chain.getId(), 
                    "Sequential dependency stopped at stage " + (i + 1) + ": " + ruleRef, 
                    SeverityConstants.INFO);
            }
        }

        logger.info("Sequential-dependency chain '{}' completed all {} stages successfully", 
                    chain.getName(), stages.size());
        return RuleResult.match(chain.getId(), "All " + stages.size() + " stages completed successfully");
    }

    /**
     * Execute a rule chain with the 'accumulative-chaining' pattern.
     *
     * <p>Evaluates each accumulation rule and accumulates a score based on configured
     * weights. If a rule matches, its weight is added to the accumulator. The final
     * accumulated score is stored in the accumulator-variable. An optional decision-rule
     * is evaluated against the final score to produce the chain outcome.</p>
     *
     * @param chain The rule chain to execute
     * @param yamlConfig The YAML configuration containing rule definitions
     * @param data The data to evaluate
     * @param contextFactory Function to create evaluation context
     * @return RuleResult from execution
     */
    @SuppressWarnings("unchecked")
    private RuleResult executeAccumulativeChainingPattern(YamlRuleChain chain,
                                                         YamlRuleConfiguration yamlConfig,
                                                         Map<String, Object> data,
                                                         java.util.function.Function<Map<String, Object>, StandardEvaluationContext> contextFactory) {
        Map<String, Object> config = chain.getConfiguration();
        if (config == null) {
            return RuleResult.error(chain.getId(), "Missing configuration for rule chain");
        }

        // Support both "accumulator-variable" (validator) and "accumulator" (YAML Reference) keys
        String accumulatorVariable = (String) config.get("accumulator-variable");
        if (accumulatorVariable == null) {
            accumulatorVariable = "accumulatedScore";
        }

        // Parse initial accumulator value (defaults to 0)
        double accumulator = 0.0;
        Object initialValue = config.get("accumulator");
        if (initialValue != null) {
            try {
                accumulator = Double.parseDouble(initialValue.toString());
            } catch (NumberFormatException e) {
                logger.warn("Invalid accumulator initial value '{}', defaulting to 0", initialValue);
            }
        }

        List<Map<String, Object>> accumulationRules = (List<Map<String, Object>>) config.get("accumulation-rules");
        if (accumulationRules == null || accumulationRules.isEmpty()) {
            return RuleResult.error(chain.getId(), "Missing or empty 'accumulation-rules' for accumulative-chaining pattern");
        }

        logger.info("Executing accumulative-chaining chain '{}' with {} rules, initial score: {}",
                    chain.getName(), accumulationRules.size(), accumulator);

        for (int i = 0; i < accumulationRules.size(); i++) {
            Map<String, Object> accRule = accumulationRules.get(i);
            String ruleRef = (String) accRule.get("rule");
            Number weightNum = (Number) accRule.get("weight");
            double weight = weightNum != null ? weightNum.doubleValue() : 0.0;

            if (ruleRef == null || ruleRef.isEmpty()) {
                return RuleResult.error(chain.getId(), "Accumulation rule " + (i + 1) + " missing 'rule' reference");
            }

            Rule rule = findRuleInConfig(yamlConfig, ruleRef);
            if (rule == null) {
                return RuleResult.error(chain.getId(), "Rule not found for accumulation rule " + (i + 1) + ": " + ruleRef);
            }

            RuleResult ruleResult = unifiedEvaluator.evaluateRule(rule, data);

            if (ruleResult.isTriggered()) {
                accumulator += weight;
                logger.info("Rule '{}' matched: weight {} applied, accumulated score: {}", ruleRef, weight, accumulator);
            } else {
                logger.info("Rule '{}' did not match: weight {} not applied, score remains: {}", ruleRef, weight, accumulator);
            }
        }

        // Store accumulated score in data for decision-rule and enrichments
        data.put(accumulatorVariable, accumulator);
        logger.info("Final accumulated score stored in '{}': {}", accumulatorVariable, accumulator);

        // Evaluate optional decision-rule
        String decisionRuleRef = (String) config.get("decision-rule");
        if (decisionRuleRef != null && !decisionRuleRef.isEmpty()) {
            Rule decisionRule = findRuleInConfig(yamlConfig, decisionRuleRef);
            if (decisionRule == null) {
                return RuleResult.error(chain.getId(), "Decision rule not found: " + decisionRuleRef);
            }

            RuleResult decisionResult = unifiedEvaluator.evaluateRule(decisionRule, data);
            if (decisionResult.isTriggered()) {
                logger.info("Decision rule '{}' passed with accumulated score {}", decisionRuleRef, accumulator);
                return RuleResult.match(chain.getId(), "Accumulative score " + accumulator + " — decision rule passed");
            } else {
                logger.info("Decision rule '{}' failed with accumulated score {}", decisionRuleRef, accumulator);
                return RuleResult.noMatch(chain.getId(),
                    "Accumulative score " + accumulator + " — decision rule failed", SeverityConstants.INFO);
            }
        }

        // No decision-rule: return match with the accumulated score as the result
        return RuleResult.match(chain.getId(), "Accumulated score: " + accumulator);
    }

    /**
     * Find a Rule object by ID from the YAML configuration's rules section.
     *
     * @param yamlConfig The YAML configuration
     * @param ruleId The rule ID to find
     * @return The Rule object if found, null otherwise
     */
    private Rule findRuleInConfig(YamlRuleConfiguration yamlConfig, String ruleId) {
        if (yamlConfig.getRules() != null) {
            for (var yamlRule : yamlConfig.getRules()) {
                if (ruleId.equals(yamlRule.getId())) {
                    return new Rule(
                        yamlRule.getId(),
                        Collections.singleton(new Category("default", 100)),
                        yamlRule.getName() != null ? yamlRule.getName() : "Rule-" + yamlRule.getId(),
                        yamlRule.getCondition(),
                        yamlRule.getMessage(),
                        yamlRule.getMessage(),
                        100,
                        yamlRule.getSeverity() != null ? yamlRule.getSeverity() : SeverityConstants.INFO,
                        null, null, null, null, null,
                        yamlRule.getResultField(),
                        null, true
                    );
                }
            }
        }
        return null;
    }

    /**
     * Find an enrichment group by ID using the pre-built index.
     *
     * <p>Phase 7 optimisation: uses the cached enrichment group index instead of calling
     * {@code EnrichmentGroupFactory.buildEnrichmentGroups()} per lookup — eliminates
     * O(n×m) factory rebuilds.</p>
     *
     * @param groupId The enrichment group ID
     * @param enrichmentGroupIndex Pre-built enrichment group index
     * @return The enrichment group if found, null otherwise
     */
    private EnrichmentGroup findEnrichmentGroup(String groupId, Map<String, EnrichmentGroup> enrichmentGroupIndex) {
        if (enrichmentGroupIndex != null) {
            return enrichmentGroupIndex.get(groupId);
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
                ruleResultField, // resultField
                null, // noMatchMessage
                true  // enabled
            );
            rules.add(r);
        }
        return rules;
    }
}
