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
package dev.mars.apex.core.config.loader;

import dev.mars.apex.core.config.exception.ConfigurationException;
import dev.mars.apex.core.config.model.YamlCategory;
import dev.mars.apex.core.config.model.YamlRule;
import dev.mars.apex.core.config.model.YamlRuleConfiguration;
import dev.mars.apex.core.config.model.YamlRuleGroup;
import dev.mars.apex.core.config.model.condition.SharedConditionGroup;
import dev.mars.apex.core.config.model.condition.SharedConditionRule;
import dev.mars.apex.core.constants.SeverityConstants;

import java.util.Set;

/**
 * Validates individual rules, rule groups, and categories within a loaded configuration.
 *
 * <p>Extracted from {@link ConfigurationLoader} (Phase 13e decomposition) to isolate
 * inline validation logic that was not yet delegated to dedicated validators
 * ({@code DuplicateValidator}, {@code RuleChainValidator}, etc.).</p>
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2.4
 */
class InlineConfigurationValidator {

    /**
     * Validate all rules in the configuration.
     */
    void validateRules(YamlRuleConfiguration config) throws ConfigurationException {
        if (config.getRules() != null) {
            for (YamlRule rule : config.getRules()) {
                validateRule(rule);
            }
        }
    }

    /**
     * Validate a rule configuration.
     * Enforces mutual exclusivity: exactly one of {@code condition} or {@code conditions} must be present.
     */
    void validateRule(YamlRule rule) throws ConfigurationException {
        if (rule.getId() == null || rule.getId().trim().isEmpty()) {
            throw new ConfigurationException("Rule ID is required");
        }
        if (rule.getName() == null || rule.getName().trim().isEmpty()) {
            throw new ConfigurationException("Rule name is required for rule: " + rule.getId());
        }

        boolean hasCondition = rule.getCondition() != null && !rule.getCondition().trim().isEmpty();
        boolean hasConditions = rule.getConditions() != null;

        if (hasCondition && hasConditions) {
            throw new ConfigurationException(
                "Rule '" + rule.getId() + "' defines both 'condition' and 'conditions'. " +
                "Exactly one must be provided.");
        }
        if (!hasCondition && !hasConditions) {
            throw new ConfigurationException(
                "Rule '" + rule.getId() + "' must define either 'condition' or 'conditions'.");
        }

        if (hasConditions) {
            validateConditionGroup(rule.getId(), rule.getConditions());
        }

        // Validate severity if present
        if (rule.getSeverity() != null) {
            String severity = rule.getSeverity().trim().toUpperCase();
            if (!SeverityConstants.VALID_SEVERITIES.contains(severity)) {
                throw new ConfigurationException("Rule '" + rule.getId() + "' has invalid severity '" +
                    rule.getSeverity() + "'. Must be one of: " + String.join(", ", SeverityConstants.VALID_SEVERITIES));
            }
        }
    }

    private static final Set<String> VALID_OPERATORS = Set.of("AND", "OR");
    private static final Set<String> VALID_CONDITION_TYPES = Set.of("expression", "lookup", "function");

    /**
     * Validate a structured condition group.
     */
    void validateConditionGroup(String ruleId, SharedConditionGroup group) throws ConfigurationException {
        if (group.getOperator() == null || group.getOperator().trim().isEmpty()) {
            throw new ConfigurationException(
                "Rule '" + ruleId + "' conditions must specify an 'operator' (AND or OR).");
        }
        String op = group.getOperator().trim().toUpperCase();
        if (!VALID_OPERATORS.contains(op)) {
            throw new ConfigurationException(
                "Rule '" + ruleId + "' conditions has invalid operator '" + group.getOperator() +
                "'. Must be AND or OR.");
        }
        if (group.getRules() == null || group.getRules().isEmpty()) {
            throw new ConfigurationException(
                "Rule '" + ruleId + "' conditions must contain at least one rule predicate.");
        }
        for (int i = 0; i < group.getRules().size(); i++) {
            validateConditionRule(ruleId, group.getRules().get(i), i);
        }
    }

    /**
     * Validate an individual condition predicate within a structured condition group.
     */
    void validateConditionRule(String ruleId, SharedConditionRule rule, int index) throws ConfigurationException {
        String type = rule.getType() != null ? rule.getType().trim().toLowerCase() : "expression";

        if (!VALID_CONDITION_TYPES.contains(type)) {
            throw new ConfigurationException(
                "Rule '" + ruleId + "' conditions[" + index + "] has invalid type '" +
                rule.getType() + "'. Must be one of: expression, lookup, function.");
        }

        // Expression type requires an explicit condition; lookup/function default to "true"
        boolean hasCondition = rule.getCondition() != null && !rule.getCondition().trim().isEmpty();
        if ("expression".equals(type) && !hasCondition) {
            throw new ConfigurationException(
                "Rule '" + ruleId + "' conditions[" + index + "] type 'expression' must have a 'condition' SpEL expression.");
        }

        if ("lookup".equals(type)) {
            if (rule.getLookupConfig() == null) {
                throw new ConfigurationException(
                    "Rule '" + ruleId + "' conditions[" + index + "] type 'lookup' requires 'lookup-config'.");
            }
        }

        if ("function".equals(type)) {
            if (rule.getEnrichmentGroupRef() == null || rule.getEnrichmentGroupRef().trim().isEmpty()) {
                throw new ConfigurationException(
                    "Rule '" + ruleId + "' conditions[" + index +
                    "] type 'function' requires 'enrichment-group-ref'.");
            }
        }
    }

    /**
     * Validate all rule groups in the configuration.
     */
    void validateRuleGroups(YamlRuleConfiguration config) throws ConfigurationException {
        if (config.getRuleGroups() != null) {
            for (YamlRuleGroup group : config.getRuleGroups()) {
                validateRuleGroup(group);
            }
        }
    }

    /**
     * Validate a rule group configuration.
     */
    void validateRuleGroup(YamlRuleGroup group) throws ConfigurationException {
        if (group.getId() == null || group.getId().trim().isEmpty()) {
            throw new ConfigurationException("Rule group ID is required");
        }
        if (group.getName() == null || group.getName().trim().isEmpty()) {
            throw new ConfigurationException("Rule group name is required for group: " + group.getId());
        }
    }

    /**
     * Validate all categories in the configuration.
     */
    void validateCategories(YamlRuleConfiguration config) throws ConfigurationException {
        if (config.getCategories() != null) {
            for (YamlCategory category : config.getCategories()) {
                validateCategory(category);
            }
        }
    }

    /**
     * Validate a category configuration.
     */
    void validateCategory(YamlCategory category) throws ConfigurationException {
        if (category.getName() == null || category.getName().trim().isEmpty()) {
            throw new ConfigurationException("Category name is required");
        }
    }
}
