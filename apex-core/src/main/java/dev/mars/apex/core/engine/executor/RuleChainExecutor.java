package dev.mars.apex.core.engine.executor;

import dev.mars.apex.core.config.yaml.YamlRuleChain;
import dev.mars.apex.core.engine.context.ChainedEvaluationContext;
import dev.mars.apex.core.engine.model.RuleChainResult;
import dev.mars.apex.core.service.engine.ExpressionEvaluatorService;
import dev.mars.apex.core.service.engine.RuleEngineService;

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
 * Core executor for rule chains that supports all 6 nested rules and rule chaining patterns.
 * 
 * Supported patterns:
 * 1. conditional-chaining - Execute Rule B only if Rule A triggers
 * 2. sequential-dependency - Each rule builds upon results from the previous rule
 * 3. result-based-routing - Route to different rule sets based on previous results
 * 4. accumulative-chaining - Build up a score/result across multiple rules
 * 5. complex-workflow - Real-world nested rule scenario with multi-stage processing
 * 6. fluent-builder - Compose rules with conditional execution paths using fluent API
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-07-28
 * @version 1.0
 */
public class RuleChainExecutor {
    
    private static final Logger LOGGER = Logger.getLogger(RuleChainExecutor.class.getName());
    
    // Pattern-specific executors
    private final ConditionalChainingExecutor conditionalChainingExecutor;
    private final SequentialDependencyExecutor sequentialDependencyExecutor;
    private final ResultBasedRoutingExecutor resultBasedRoutingExecutor;
    private final AccumulativeChainingExecutor accumulativeChainingExecutor;
    private final ComplexWorkflowExecutor complexWorkflowExecutor;
    private final FluentBuilderExecutor fluentBuilderExecutor;
    
    /**
     * Create a new rule chain executor.
     *
     * @param ruleEngineService The rule engine service for executing individual rules
     * @param evaluatorService The expression evaluator service for evaluating SpEL expressions
     */
    public RuleChainExecutor(RuleEngineService ruleEngineService, ExpressionEvaluatorService evaluatorService) {
        
        // Initialize pattern-specific executors
        this.conditionalChainingExecutor = new ConditionalChainingExecutor(ruleEngineService, evaluatorService);
        this.sequentialDependencyExecutor = new SequentialDependencyExecutor(ruleEngineService, evaluatorService);
        this.resultBasedRoutingExecutor = new ResultBasedRoutingExecutor(ruleEngineService, evaluatorService);
        this.accumulativeChainingExecutor = new AccumulativeChainingExecutor(ruleEngineService, evaluatorService);
        this.complexWorkflowExecutor = new ComplexWorkflowExecutor(ruleEngineService, evaluatorService);
        this.fluentBuilderExecutor = new FluentBuilderExecutor(ruleEngineService, evaluatorService);
    }
    
    /**
     * Execute a rule chain with the given context.
     * 
     * @param ruleChain The rule chain configuration to execute
     * @param context The chained evaluation context
     * @return The result of the rule chain execution
     */
    public RuleChainResult executeRuleChain(YamlRuleChain ruleChain, ChainedEvaluationContext context) {
        if (ruleChain == null) {
            return RuleChainResult.failure("unknown", "unknown", "Rule chain is null");
        }
        
        if (!ruleChain.isEnabled()) {
            LOGGER.info("Rule chain '" + ruleChain.getId() + "' is disabled, skipping execution");
            return RuleChainResult.builder(ruleChain.getId(), ruleChain.getPattern())
                    .ruleChainName(ruleChain.getName())
                    .finalOutcome("SKIPPED")
                    .successful(true)
                    .addMetadata("reason", "Rule chain is disabled")
                    .build();
        }
        
        String pattern = ruleChain.getPattern();
        if (pattern == null) {
            return RuleChainResult.failure(ruleChain.getId(), "unknown", "Rule chain pattern is not specified");
        }
        
        LOGGER.info("Executing rule chain '" + ruleChain.getId() + "' with pattern '" + pattern + "'");
        
        try {
            // Set chain metadata in context
            context.addChainMetadata("ruleChainId", ruleChain.getId());
            context.addChainMetadata("ruleChainName", ruleChain.getName());
            context.addChainMetadata("pattern", pattern);
            
            // Route to appropriate pattern executor
            RuleChainResult result = executeByPattern(ruleChain, context);
            
            LOGGER.info("Rule chain '" + ruleChain.getId() + "' completed with outcome: " + result.getFinalOutcome());
            return result;
            
        } catch (Exception e) {
            LOGGER.severe("Error executing rule chain '" + ruleChain.getId() + "': " + e.getMessage());
            return RuleChainResult.failure(ruleChain.getId(), pattern, "Execution error: " + e.getMessage());
        }
    }
    
    /**
     * Execute a rule chain by routing to the appropriate pattern executor.
     * 
     * @param ruleChain The rule chain configuration
     * @param context The chained evaluation context
     * @return The result of the pattern-specific execution
     */
    private RuleChainResult executeByPattern(YamlRuleChain ruleChain, ChainedEvaluationContext context) {
        String pattern = ruleChain.getPattern().toLowerCase();
        Map<String, Object> configuration = ruleChain.getConfiguration();
        
        if (configuration == null) {
            return RuleChainResult.failure(ruleChain.getId(), ruleChain.getPattern(), 
                    "Rule chain configuration is missing");
        }
        
        switch (pattern) {
            case "conditional-chaining":
                return conditionalChainingExecutor.execute(ruleChain, configuration, context);
                
            case "sequential-dependency":
                return sequentialDependencyExecutor.execute(ruleChain, configuration, context);
                
            case "result-based-routing":
                return resultBasedRoutingExecutor.execute(ruleChain, configuration, context);
                
            case "accumulative-chaining":
                return accumulativeChainingExecutor.execute(ruleChain, configuration, context);
                
            case "complex-workflow":
                return complexWorkflowExecutor.execute(ruleChain, configuration, context);
                
            case "fluent-builder":
                return fluentBuilderExecutor.execute(ruleChain, configuration, context);
                
            default:
                return RuleChainResult.failure(ruleChain.getId(), ruleChain.getPattern(), 
                        "Unsupported rule chain pattern: " + pattern);
        }
    }
    
    /**
     * Validate a rule chain configuration.
     * 
     * @param ruleChain The rule chain to validate
     * @return true if valid, false otherwise
     */
    public boolean validateRuleChain(YamlRuleChain ruleChain) {
        if (ruleChain == null) {
            LOGGER.warning("Rule chain is null");
            return false;
        }
        
        if (ruleChain.getId() == null || ruleChain.getId().trim().isEmpty()) {
            LOGGER.warning("Rule chain ID is missing or empty");
            return false;
        }
        
        if (ruleChain.getPattern() == null || ruleChain.getPattern().trim().isEmpty()) {
            LOGGER.warning("Rule chain pattern is missing or empty for chain: " + ruleChain.getId());
            return false;
        }
        
        String pattern = ruleChain.getPattern().toLowerCase();
        if (!isSupportedPattern(pattern)) {
            LOGGER.warning("Unsupported rule chain pattern '" + pattern + "' for chain: " + ruleChain.getId());
            return false;
        }
        
        if (ruleChain.getConfiguration() == null) {
            LOGGER.warning("Rule chain configuration is missing for chain: " + ruleChain.getId());
            return false;
        }
        
        // Pattern-specific validation would be performed by individual executors
        return true;
    }
    
    /**
     * Check if a pattern is supported.
     * 
     * @param pattern The pattern name (case-insensitive)
     * @return true if supported, false otherwise
     */
    private boolean isSupportedPattern(String pattern) {
        switch (pattern.toLowerCase()) {
            case "conditional-chaining":
            case "sequential-dependency":
            case "result-based-routing":
            case "accumulative-chaining":
            case "complex-workflow":
            case "fluent-builder":
                return true;
            default:
                return false;
        }
    }
    
    /**
     * Get the list of supported patterns.
     * 
     * @return Array of supported pattern names
     */
    public String[] getSupportedPatterns() {
        return new String[]{
            "conditional-chaining",
            "sequential-dependency", 
            "result-based-routing",
            "accumulative-chaining",
            "complex-workflow",
            "fluent-builder"
        };
    }
}
