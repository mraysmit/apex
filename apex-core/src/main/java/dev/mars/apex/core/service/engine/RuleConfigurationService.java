package dev.mars.apex.core.service.engine;

import dev.mars.apex.core.engine.model.Rule;
import dev.mars.apex.core.engine.model.RuleGroup;
import dev.mars.apex.core.engine.config.RulesEngineConfiguration;

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
 * Service for managing rule configurations.
 *
* This class is part of the APEX A powerful expression processor for Java applications.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-07-27
 * @version 1.0
 */
/**
 * Service for managing rule configurations.
 * This class hosts the main repository of Rules for the project and provides methods
 * for registering and retrieving rules.
 */
public class RuleConfigurationService {
    private final RulesEngineConfiguration configuration;

    /**
     * Create a new RuleConfigurationService with the given configuration.
     *
     * @param configuration The configuration to use
     */
    public RuleConfigurationService(RulesEngineConfiguration configuration) {
        this.configuration = configuration;
    }

    /**
     * Get the configuration used by this service.
     *
     * @return The configuration
     */
    public RulesEngineConfiguration getConfiguration() {
        return configuration;
    }

    /**
     * Register a rule with the given parameters.
     *
     * @param id The ID of the rule
     * @param category The category of the rule
     * @param name The name of the rule
     * @param condition The condition of the rule
     * @param message The message of the rule
     * @param description The description of the rule
     * @param priority The priority of the rule
     * @return The created rule
     */
    public Rule registerRule(String id, String category, String name, String condition, 
                          String message, String description, int priority) {
        Rule rule = createRule(id, category, name, condition, message, description, priority);
        return configuration.registerRule(rule);
    }

    /**
     * Register a rule group with AND operator.
     *
     * @param id The ID of the rule group
     * @param category The category of the rule group
     * @param name The name of the rule group
     * @param description The description of the rule group
     * @param priority The priority of the rule group
     * @return The created rule group
     */
    public RuleGroup registerRuleGroupWithAnd(String id, String category, String name,
                                           String description, int priority) {
        return configuration.createRuleGroupWithAnd(id, category, name, description, priority);
    }

    /**
     * Register a rule group with OR operator.
     *
     * @param id The ID of the rule group
     * @param category The category of the rule group
     * @param name The name of the rule group
     * @param description The description of the rule group
     * @param priority The priority of the rule group
     * @return The created rule group
     */
    public RuleGroup registerRuleGroupWithOr(String id, String category, String name,
                                          String description, int priority) {
        return configuration.createRuleGroupWithOr(id, category, name, description, priority);
    }

    /**
     * Add a rule to a rule group.
     *
     * @param group The rule group
     * @param rule The rule to add
     * @param sequenceNumber The sequence number for the rule within the group
     */
    public void addRuleToGroup(RuleGroup group, Rule rule, int sequenceNumber) {
        if (group != null && rule != null) {
            group.addRule(rule, sequenceNumber);
        }
    }

    /**
     * Get a rule by its ID.
     *
     * @param id The ID of the rule
     * @return The rule with the specified ID, or null if not found
     */
    public Rule getRuleById(String id) {
        return configuration.getRuleById(id);
    }

    /**
     * Get a rule group by its ID.
     *
     * @param id The ID of the rule group
     * @return The rule group with the specified ID, or null if not found
     */
    public RuleGroup getRuleGroupById(String id) {
        return configuration.getRuleGroupById(id);
    }

    /**
     * Create a new rule with the given parameters.
     *
     * @param id The ID of the rule
     * @param category The category of the rule
     * @param name The name of the rule
     * @param condition The condition of the rule
     * @param message The message of the rule
     * @param description The description of the rule
     * @param priority The priority of the rule
     * @return The created rule
     */
    public Rule createRule(String id, String category, String name, String condition, 
                          String message, String description, int priority) {
        return configuration.rule(id)
            .withCategory(category)
            .withName(name)
            .withCondition(condition)
            .withMessage(message)
            .withDescription(description)
            .withPriority(priority)
            .build();
    }
}
