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
import dev.mars.apex.core.constants.SeverityConstants;

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
     */
    void validateRule(YamlRule rule) throws ConfigurationException {
        if (rule.getId() == null || rule.getId().trim().isEmpty()) {
            throw new ConfigurationException("Rule ID is required");
        }
        if (rule.getName() == null || rule.getName().trim().isEmpty()) {
            throw new ConfigurationException("Rule name is required for rule: " + rule.getId());
        }
        if (rule.getCondition() == null || rule.getCondition().trim().isEmpty()) {
            throw new ConfigurationException("Rule condition is required for rule: " + rule.getId());
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
