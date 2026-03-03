package dev.mars.apex.core.config.validation;

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

import dev.mars.apex.core.config.exception.ConfigurationException;
import dev.mars.apex.core.config.model.YamlRuleChain;
import dev.mars.apex.core.config.model.YamlRuleConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

/**
 * Validates rule chain configurations including pattern-specific validation
 * for all six chain patterns: conditional-chaining, sequential-dependency,
 * result-based-routing, accumulative-chaining, complex-workflow, and fluent-builder.
 *
 * <p>Extracted from {@code ConfigurationLoader} as part of the validation layer refactoring.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-07-27
 */
public class RuleChainValidator {

    private static final Logger logger = LoggerFactory.getLogger(RuleChainValidator.class);

    /**
     * Validate all rule chains in the configuration.
     *
     * @param config the YAML rule configuration to validate
     * @throws ConfigurationException if any rule chain is invalid
     */
    public void validate(YamlRuleConfiguration config) throws ConfigurationException {
        if (config.getRuleChains() != null) {
            for (YamlRuleChain ruleChain : config.getRuleChains()) {
                validateRuleChain(ruleChain);
            }
        }
    }

    /**
     * Validate a rule chain configuration.
     */
    private void validateRuleChain(YamlRuleChain ruleChain) throws ConfigurationException {
        if (ruleChain.getId() == null || ruleChain.getId().trim().isEmpty()) {
            throw new ConfigurationException("Rule chain ID is required");
        }
        if (ruleChain.getName() == null || ruleChain.getName().trim().isEmpty()) {
            throw new ConfigurationException("Rule chain name is required for chain: " + ruleChain.getId());
        }
        if (ruleChain.getPattern() == null || ruleChain.getPattern().trim().isEmpty()) {
            throw new ConfigurationException("Rule chain pattern is required for chain: " + ruleChain.getId());
        }

        // Validate pattern-specific configuration
        validateRuleChainPattern(ruleChain);
    }

    /**
     * Validate pattern-specific rule chain configuration.
     */
    private void validateRuleChainPattern(YamlRuleChain ruleChain) throws ConfigurationException {
        String pattern = ruleChain.getPattern();
        String chainId = ruleChain.getId();
        Map<String, Object> config = ruleChain.getConfiguration();

        if (config == null || config.isEmpty()) {
            throw new ConfigurationException("Rule chain configuration is required for pattern '" + pattern + "' in chain: " + chainId);
        }

        switch (pattern) {
            case "conditional-chaining":
                validateConditionalChainingPattern(config, chainId);
                break;
            case "sequential-dependency":
                validateSequentialDependencyPattern(config, chainId);
                break;
            case "result-based-routing":
                validateResultBasedRoutingPattern(config, chainId);
                break;
            case "accumulative-chaining":
                validateAccumulativeChainingPattern(config, chainId);
                break;
            case "complex-workflow":
                validateComplexWorkflowPattern(config, chainId);
                break;
            case "fluent-builder":
                validateFluentBuilderPattern(config, chainId);
                break;
            default:
                logger.warn("Unknown rule chain pattern '" + pattern + "' for chain: " + chainId);
        }
    }

    /**
     * Validate conditional-chaining pattern configuration.
     */
    private void validateConditionalChainingPattern(Map<String, Object> config, String chainId) throws ConfigurationException {
        if (!config.containsKey("trigger-rule") || config.get("trigger-rule") == null) {
            throw new ConfigurationException("Missing required 'trigger-rule' for conditional-chaining pattern in chain: " + chainId);
        }
        if (!config.containsKey("conditional-rules") || config.get("conditional-rules") == null) {
            throw new ConfigurationException("Missing required 'conditional-rules' for conditional-chaining pattern in chain: " + chainId);
        }
    }

    /**
     * Validate sequential-dependency pattern configuration.
     */
    @SuppressWarnings("unchecked")
    private void validateSequentialDependencyPattern(Map<String, Object> config, String chainId) throws ConfigurationException {
        if (!config.containsKey("stages") || config.get("stages") == null) {
            throw new ConfigurationException("Missing required 'stages' for sequential-dependency pattern in chain: " + chainId);
        }

        Object stagesObj = config.get("stages");
        if (!(stagesObj instanceof List)) {
            throw new ConfigurationException("'stages' must be a list for sequential-dependency pattern in chain: " + chainId);
        }

        List<Object> stages = (List<Object>) stagesObj;
        if (stages.isEmpty()) {
            throw new ConfigurationException("'stages' cannot be empty for sequential-dependency pattern in chain: " + chainId);
        }
    }

    /**
     * Validate result-based-routing pattern configuration.
     */
    private void validateResultBasedRoutingPattern(Map<String, Object> config, String chainId) throws ConfigurationException {
        if (!config.containsKey("router-rule") || config.get("router-rule") == null) {
            throw new ConfigurationException("Missing required 'router-rule' for result-based-routing pattern in chain: " + chainId);
        }
        if (!config.containsKey("routes") || config.get("routes") == null) {
            throw new ConfigurationException("Missing required 'routes' for result-based-routing pattern in chain: " + chainId);
        }
    }

    /**
     * Validate accumulative-chaining pattern configuration.
     */
    private void validateAccumulativeChainingPattern(Map<String, Object> config, String chainId) throws ConfigurationException {
        if (!config.containsKey("accumulator-variable") || config.get("accumulator-variable") == null) {
            throw new ConfigurationException("Missing required 'accumulator-variable' for accumulative-chaining pattern in chain: " + chainId);
        }
        if (!config.containsKey("accumulation-rules") || config.get("accumulation-rules") == null) {
            throw new ConfigurationException("Missing required 'accumulation-rules' for accumulative-chaining pattern in chain: " + chainId);
        }
    }

    /**
     * Validate complex-workflow pattern configuration.
     */
    @SuppressWarnings("unchecked")
    private void validateComplexWorkflowPattern(Map<String, Object> config, String chainId) throws ConfigurationException {
        if (!config.containsKey("stages") || config.get("stages") == null) {
            throw new ConfigurationException("Missing required 'stages' for complex-workflow pattern in chain: " + chainId);
        }

        Object stagesObj = config.get("stages");
        if (!(stagesObj instanceof List)) {
            throw new ConfigurationException("'stages' must be a list for complex-workflow pattern in chain: " + chainId);
        }

        List<Object> stages = (List<Object>) stagesObj;
        if (stages.isEmpty()) {
            throw new ConfigurationException("'stages' cannot be empty for complex-workflow pattern in chain: " + chainId);
        }
    }

    /**
     * Validate fluent-builder pattern configuration.
     */
    private void validateFluentBuilderPattern(Map<String, Object> config, String chainId) throws ConfigurationException {
        if (!config.containsKey("root-rule") || config.get("root-rule") == null) {
            throw new ConfigurationException("Missing required 'root-rule' for fluent-builder pattern in chain: " + chainId);
        }
    }
}
