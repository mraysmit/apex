package dev.mars.apex.core.engine.config;

import dev.mars.apex.core.engine.model.Category;
import dev.mars.apex.core.engine.model.Rule;
import dev.mars.apex.core.engine.model.RuleBase;
import dev.mars.apex.core.engine.model.RuleGroup;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;

import java.util.*;
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
 * This class handles the configuration and setup of rules for the Rules Engine.
 *
 * This class is part of the PeeGeeQ message queue system, providing
 * production-ready PostgreSQL-based message queuing capabilities.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-07-27
 * @version 1.0
 */
/**
 * This class handles the configuration and setup of rules for the Rules Engine.
 * It provides methods for registering rules, creating rule groups, and managing rule relationships.
 * 
 * The configuration maintains three main collections:
 * 1. Rules by category - for quick lookup of rules by their category
 * 2. Rules by ID - for quick lookup of individual rules
 * 3. Rule groups by ID - for quick lookup of rule groups
 */
public class RulesEngineConfiguration {
    private static final Logger LOGGER = Logger.getLogger(RulesEngineConfiguration.class.getName());
    static final ExpressionParser parser = new SpelExpressionParser();

    private final Map<Category, List<RuleBase>> rulesByCategory = new HashMap<>();
    private final Map<String, Rule> rulesById = new HashMap<>();
    private final Map<String, RuleGroup> ruleGroupsById = new HashMap<>();

    // Map to store categories by name for quick lookup
    private final Map<String, Category> categoriesByName = new HashMap<>();

    /**
     * Create a new rule builder with a generated ID.
     * This is the recommended way to create and register rules.
     * 
     * @return A new rule builder
     */
    public RuleBuilder rule() {
        return new RuleBuilder();
    }

    /**
     * Create a new rule builder with the specified ID.
     * This is the recommended way to create and register rules with a specific ID.
     * 
     * @param id The unique identifier for the rule
     * @return A new rule builder
     */
    public RuleBuilder rule(String id) {
        return new RuleBuilder(id);
    }

    /**
     * Register a rule that has already been created.
     * 
     * @param rule The rule to register
     * @return The registered rule for method chaining
     */
    public Rule registerRule(Rule rule) {
        rulesById.put(rule.getId(), rule);
        for (Category category : rule.getCategories()) {
            addRuleToCategory(rule, category);
        }
        return rule;
    }


    /**
     * Add a rule to a category.
     * 
     * @param rule The rule to add
     * @param category The category to add the rule to
     */
    private void addRuleToCategory(RuleBase rule, Category category) {
        rulesByCategory.computeIfAbsent(category, k -> new ArrayList<>()).add(rule);
        // Sort rules by priority
        sortRulesByPriority(category);
    }



    /**
     * Get a category by name, creating it if it doesn't exist.
     * 
     * @param categoryName The name of the category
     * @return The category with the specified name
     */
    private Category getCategoryByName(String categoryName) {
        return categoriesByName.computeIfAbsent(categoryName, 
            name -> new Category(name, 100));
    }

    /**
     * Add a rule to a rule group with a specific sequence number.
     *
     * @param groupId The ID of the rule group
     * @param ruleId The ID of the rule to add
     * @param sequenceNumber The sequence number for this rule within the group
     * @return True if the rule was added successfully, false otherwise
     */
    public boolean addRuleToGroup(String groupId, String ruleId, int sequenceNumber) {
        RuleGroup group = ruleGroupsById.get(groupId);
        if (group == null) {
            LOGGER.warning("Rule group with ID '" + groupId + "' not found");
            return false;
        }

        Rule rule = rulesById.get(ruleId);
        if (rule == null) {
            LOGGER.warning("Rule with ID '" + ruleId + "' not found");
            return false;
        }

        group.addRule(rule, sequenceNumber);
        return true;
    }

    /**
     * Sort rules in a category by priority.
     * 
     * @param category The category to sort
     */
    private void sortRulesByPriority(Category category) {
        List<RuleBase> rules = rulesByCategory.get(category);
        if (rules != null) {
            rules.sort(Comparator.comparingInt(RuleBase::getPriority));
        }
    }



    /**
     * Create a new rule by combining multiple rules with an "AND" operator.
     * 
     * @param id The unique identifier of the new rule
     * @param category The category of the new rule
     * @param name The name of the new rule
     * @param rules The rules to combine
     * @param message The message to return if the combined rule is triggered
     * @param description The description of what the combined rule does
     * @param priority The priority of the combined rule
     * @return The newly created rule, or null if no rules to combine
     */
    public Rule combineWithAnd(String id, Category category, String name, List<Rule> rules,
                               String message, String description, int priority) {
        if (rules == null || rules.isEmpty()) {
            LOGGER.warning("No rules to combine");
            return null;
        }

        StringBuilder combinedCondition = new StringBuilder();
        for (int i = 0; i < rules.size(); i++) {
            if (i > 0) {
                combinedCondition.append(" && ");
            }
            combinedCondition.append("(").append(rules.get(i).getCondition()).append(")");
        }

        return rule(id)
            .withCategory(category)
            .withName(name)
            .withCondition(combinedCondition.toString())
            .withMessage(message)
            .withDescription(description)
            .withPriority(priority)
            .build();
    }

    /**
     * Create a new rule by combining multiple rules with an "AND" operator.
     * 
     * @param id The unique identifier of the new rule
     * @param categoryName The name of the category of the new rule
     * @param name The name of the new rule
     * @param rules The rules to combine
     * @param message The message to return if the combined rule is triggered
     * @param description The description of what the combined rule does
     * @param priority The priority of the combined rule
     * @return The newly created rule, or null if no rules to combine
     */
    public Rule combineWithAnd(String id, String categoryName, String name, List<Rule> rules,
                               String message, String description, int priority) {
        Category category = getCategoryByName(categoryName);
        return combineWithAnd(id, category, name, rules, message, description, priority);
    }

    /**
     * Create a new rule by combining multiple rules with an "OR" operator.
     * 
     * @param id The unique identifier of the new rule
     * @param category The category of the new rule
     * @param name The name of the new rule
     * @param rules The rules to combine
     * @param message The message to return if the combined rule is triggered
     * @param description The description of what the combined rule does
     * @param priority The priority of the combined rule
     * @return The newly created rule, or null if no rules to combine
     */
    public Rule combineWithOr(String id, Category category, String name, List<Rule> rules,
                              String message, String description, int priority) {
        if (rules == null || rules.isEmpty()) {
            LOGGER.warning("No rules to combine");
            return null;
        }

        StringBuilder combinedCondition = new StringBuilder();
        for (int i = 0; i < rules.size(); i++) {
            if (i > 0) {
                combinedCondition.append(" || ");
            }
            combinedCondition.append("(").append(rules.get(i).getCondition()).append(")");
        }

        return rule(id)
            .withCategory(category)
            .withName(name)
            .withCondition(combinedCondition.toString())
            .withMessage(message)
            .withDescription(description)
            .withPriority(priority)
            .build();
    }

    /**
     * Create a new rule by combining multiple rules with an "OR" operator.
     * 
     * @param id The unique identifier of the new rule
     * @param categoryName The name of the category of the new rule
     * @param name The name of the new rule
     * @param rules The rules to combine
     * @param message The message to return if the combined rule is triggered
     * @param description The description of what the combined rule does
     * @param priority The priority of the combined rule
     * @return The newly created rule, or null if no rules to combine
     */
    public Rule combineWithOr(String id, String categoryName, String name, List<Rule> rules,
                              String message, String description, int priority) {
        Category category = getCategoryByName(categoryName);
        return combineWithOr(id, category, name, rules, message, description, priority);
    }


    /**
     * Create a new rule group builder with a generated ID.
     * This is the recommended way to create and register rule groups.
     * 
     * @return A new rule group builder
     */
    public RuleGroupBuilder group() {
        return new RuleGroupBuilder();
    }

    /**
     * Create a new rule group builder with the specified ID.
     * This is the recommended way to create and register rule groups with a specific ID.
     * 
     * @param id The unique identifier for the rule group
     * @return A new rule group builder
     */
    public RuleGroupBuilder group(String id) {
        return new RuleGroupBuilder(id);
    }

    /**
     * Register a rule group that has already been created.
     * 
     * @param group The rule group to register
     * @return The registered rule group for method chaining
     */
    public RuleGroup registerRuleGroup(RuleGroup group) {
        ruleGroupsById.put(group.getId(), group);
        for (Category category : group.getCategories()) {
            rulesByCategory.computeIfAbsent(category, k -> new ArrayList<>()).add(group);
            sortRulesByPriority(category);
        }
        return group;
    }



    /**
     * Get the rules for a specific category.
     * 
     * @param category The category to get rules for
     * @return The list of rules for the category
     */
    public List<RuleBase> getRulesForCategory(Category category) {
        return rulesByCategory.getOrDefault(category, new ArrayList<>());
    }

    /**
     * Get the rules for a specific category by name.
     * 
     * @param categoryName The name of the category to get rules for
     * @return The list of rules for the category
     */
    public List<RuleBase> getRulesForCategory(String categoryName) {
        Category category = getCategoryByName(categoryName);
        return getRulesForCategory(category);
    }

    /**
     * Get a rule by its ID.
     * 
     * @param id The ID of the rule
     * @return The rule with the specified ID, or null if not found
     */
    public Rule getRuleById(String id) {
        Rule rule = rulesById.get(id);
        if (rule == null) {
            LOGGER.warning("Rule with ID '" + id + "' not found");
        }
        return rule;
    }

    /**
     * Get a rule group by its ID.
     *
     * @param id The ID of the rule group
     * @return The rule group with the specified ID, or null if not found
     */
    public RuleGroup getRuleGroupById(String id) {
        RuleGroup group = ruleGroupsById.get(id);
        if (group == null) {
            LOGGER.warning("Rule group with ID '" + id + "' not found");
        }
        return group;
    }

    /**
     * Get all registered rules.
     *
     * @return A list of all registered rules
     */
    public List<Rule> getAllRules() {
        return new ArrayList<>(rulesById.values());
    }

    /**
     * Get all registered rule groups.
     *
     * @return A list of all registered rule groups
     */
    public List<RuleGroup> getAllRuleGroups() {
        return new ArrayList<>(ruleGroupsById.values());
    }

    /**
     * Get all rule bases (rules and rule groups) across all categories.
     *
     * @return A list of all rule bases
     */
    public List<RuleBase> getAllRuleBases() {
        List<RuleBase> allRuleBases = new ArrayList<>();
        for (List<RuleBase> categoryRules : rulesByCategory.values()) {
            allRuleBases.addAll(categoryRules);
        }
        return allRuleBases;
    }

    /**
     * Create a new rule group with AND operator.
     *
     * @param id The unique identifier of the rule group
     * @param category The category of the rule group
     * @param name The name of the rule group
     * @param description The description of what the rule group does
     * @param priority The priority of the rule group (lower numbers = higher priority)
     * @return The newly created rule group
     */
    public RuleGroup createRuleGroupWithAnd(String id, Category category, String name,
                                            String description, int priority) {
        RuleGroup group = group(id)
            .withCategory(category)
            .withName(name)
            .withDescription(description)
            .withPriority(priority)
            .withAndOperator()
            .build();

        return registerRuleGroup(group);
    }

    /**
     * Create a new rule group with AND operator.
     *
     * @param id The unique identifier of the rule group
     * @param categoryName The name of the category of the rule group
     * @param name The name of the rule group
     * @param description The description of what the rule group does
     * @param priority The priority of the rule group (lower numbers = higher priority)
     * @return The newly created rule group
     */
    public RuleGroup createRuleGroupWithAnd(String id, String categoryName, String name,
                                            String description, int priority) {
        Category category = getCategoryByName(categoryName);
        return createRuleGroupWithAnd(id, category, name, description, priority);
    }

    /**
     * Create a new rule group with AND operator and multiple categories.
     *
     * @param id The unique identifier of the rule group
     * @param categoryNames The set of category names this rule group belongs to
     * @param name The name of the rule group
     * @param description The description of what the rule group does
     * @param priority The priority of the rule group (lower numbers = higher priority)
     * @return The newly created rule group
     */
    public RuleGroup createRuleGroupWithAnd(String id, Set<String> categoryNames, String name,
                                            String description, int priority) {
        RuleGroup group = group(id)
            .withCategoryNames(categoryNames)
            .withName(name)
            .withDescription(description)
            .withPriority(priority)
            .withAndOperator()
            .build();

        return registerRuleGroup(group);
    }

    /**
     * Create a new rule group with OR operator.
     *
     * @param id The unique identifier of the rule group
     * @param category The category of the rule group
     * @param name The name of the rule group
     * @param description The description of what the rule group does
     * @param priority The priority of the rule group (lower numbers = higher priority)
     * @return The newly created rule group
     */
    public RuleGroup createRuleGroupWithOr(String id, Category category, String name,
                                           String description, int priority) {
        RuleGroup group = group(id)
            .withCategory(category)
            .withName(name)
            .withDescription(description)
            .withPriority(priority)
            .withOrOperator()
            .build();

        return registerRuleGroup(group);
    }

    /**
     * Create a new rule group with OR operator.
     *
     * @param id The unique identifier of the rule group
     * @param categoryName The name of the category of the rule group
     * @param name The name of the rule group
     * @param description The description of what the rule group does
     * @param priority The priority of the rule group (lower numbers = higher priority)
     * @return The newly created rule group
     */
    public RuleGroup createRuleGroupWithOr(String id, String categoryName, String name,
                                           String description, int priority) {
        Category category = getCategoryByName(categoryName);
        return createRuleGroupWithOr(id, category, name, description, priority);
    }

    /**
     * Create a new rule group with OR operator and multiple categories.
     *
     * @param id The unique identifier of the rule group
     * @param categoryNames The set of category names this rule group belongs to
     * @param name The name of the rule group
     * @param description The description of what the rule group does
     * @param priority The priority of the rule group (lower numbers = higher priority)
     * @return The newly created rule group
     */
    public RuleGroup createRuleGroupWithOr(String id, Set<String> categoryNames, String name,
                                           String description, int priority) {
        RuleGroup group = group(id)
            .withCategoryNames(categoryNames)
            .withName(name)
            .withDescription(description)
            .withPriority(priority)
            .withOrOperator()
            .build();

        return registerRuleGroup(group);
    }

    /**
     * Combine two or more rules with an "AND" operator.
     *
     * @param id The unique identifier of the new combined rule
     * @param category The category of the new combined rule
     * @param name The name of the new combined rule
     * @param ruleIds The IDs of the rules to combine
     * @param message The message to return if the combined rule is triggered
     * @param description The description of what the combined rule does
     * @param priority The priority of the combined rule
     * @return The ID of the newly created rule, or null if any of the rules to combine don't exist
     */
    public String combineRulesWithAnd(String id, Category category, String name, List<String> ruleIds,
                                     String message, String description, int priority) {
        if (ruleIds == null || ruleIds.isEmpty()) {
            LOGGER.warning("No rule IDs to combine");
            return null;
        }

        List<Rule> rules = new ArrayList<>();
        for (String ruleId : ruleIds) {
            Rule rule = getRuleById(ruleId);
            if (rule == null) {
                LOGGER.warning("Rule with ID '" + ruleId + "' not found");
                return null;
            }
            rules.add(rule);
        }

        Rule combinedRule = combineWithAnd(id, category, name, rules, message, description, priority);
        if (combinedRule != null) {
            registerRule(combinedRule);
            return combinedRule.getId();
        }

        return null;
    }

    /**
     * Combine two or more rules with an "AND" operator.
     *
     * @param id The unique identifier of the new combined rule
     * @param categoryName The name of the category of the new combined rule
     * @param name The name of the new combined rule
     * @param ruleIds The IDs of the rules to combine
     * @param message The message to return if the combined rule is triggered
     * @param description The description of what the combined rule does
     * @param priority The priority of the combined rule
     * @return The ID of the newly created rule, or null if any of the rules to combine don't exist
     */
    public String combineRulesWithAnd(String id, String categoryName, String name, List<String> ruleIds,
                                     String message, String description, int priority) {
        Category category = getCategoryByName(categoryName);
        return combineRulesWithAnd(id, category, name, ruleIds, message, description, priority);
    }

    /**
     * Combine two or more rules with an "OR" operator.
     *
     * @param id The unique identifier of the new combined rule
     * @param category The category of the new combined rule
     * @param name The name of the new combined rule
     * @param ruleIds The IDs of the rules to combine
     * @param message The message to return if the combined rule is triggered
     * @param description The description of what the combined rule does
     * @param priority The priority of the combined rule
     * @return The ID of the newly created rule, or null if any of the rules to combine don't exist
     */
    public String combineRulesWithOr(String id, Category category, String name, List<String> ruleIds,
                                    String message, String description, int priority) {
        if (ruleIds == null || ruleIds.isEmpty()) {
            LOGGER.warning("No rule IDs to combine");
            return null;
        }

        List<Rule> rules = new ArrayList<>();
        for (String ruleId : ruleIds) {
            Rule rule = getRuleById(ruleId);
            if (rule == null) {
                LOGGER.warning("Rule with ID '" + ruleId + "' not found");
                return null;
            }
            rules.add(rule);
        }

        Rule combinedRule = combineWithOr(id, category, name, rules, message, description, priority);
        if (combinedRule != null) {
            registerRule(combinedRule);
            return combinedRule.getId();
        }

        return null;
    }

    /**
     * Combine two or more rules with an "OR" operator.
     *
     * @param id The unique identifier of the new combined rule
     * @param categoryName The name of the category of the new combined rule
     * @param name The name of the new combined rule
     * @param ruleIds The IDs of the rules to combine
     * @param message The message to return if the combined rule is triggered
     * @param description The description of what the combined rule does
     * @param priority The priority of the combined rule
     * @return The ID of the newly created rule, or null if any of the rules to combine don't exist
     */
    public String combineRulesWithOr(String id, String categoryName, String name, List<String> ruleIds,
                                    String message, String description, int priority) {
        Category category = getCategoryByName(categoryName);
        return combineRulesWithOr(id, category, name, ruleIds, message, description, priority);
    }
}
