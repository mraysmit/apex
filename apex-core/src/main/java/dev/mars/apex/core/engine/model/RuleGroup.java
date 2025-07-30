package dev.mars.apex.core.engine.model;

import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.util.*;
import java.util.stream.Collectors;

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
 * A group of rules that can be combined with AND or OR operators.
 *
 * This class is part of the PeeGeeQ message queue system, providing
 * production-ready PostgreSQL-based message queuing capabilities.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-07-27
 * @version 1.0
 */
public class RuleGroup implements RuleBase {
    private static final ExpressionParser parser = new SpelExpressionParser();

    private final UUID uuid;
    private final String id;
    private final Set<Category> categories;
    private final String name;
    private final String description;
    private final int priority;
    private final Map<Integer, Rule> rulesBySequence;
    private final boolean isAndOperator;
    private String message;

    /**
     * Create a new rule group.
     *
     * @param id The unique identifier of the rule group
     * @param category The initial category of the rule group
     * @param name The name of the rule group
     * @param description The description of what the rule group does
     * @param priority The priority of the rule group (lower numbers = higher priority)
     * @param isAndOperator Whether to use AND (true) or OR (false) to combine rules
     */
    public RuleGroup(String id, String category, String name, String description,
                     int priority, boolean isAndOperator) {
        this.uuid = UUID.randomUUID();
        this.id = id;
        this.categories = new HashSet<>();
        this.categories.add(new Category(category, priority));
        this.name = name;
        this.description = description;
        this.priority = priority;
        this.rulesBySequence = new HashMap<>();
        this.isAndOperator = isAndOperator;
        this.message = description; // Default message is the description
    }

    /**
     * Create a new rule group with multiple category names.
     *
     * @param id The unique identifier of the rule group
     * @param categoryNames The set of category names this rule group belongs to
     * @param name The name of the rule group
     * @param description The description of what the rule group does
     * @param priority The priority of the rule group (lower numbers = higher priority)
     * @param isAndOperator Whether to use AND (true) or OR (false) to combine rules
     * @return A new rule group
     */
    public static RuleGroup fromCategoryNames(String id, Set<String> categoryNames, String name, String description,
                                             int priority, boolean isAndOperator) {
        Set<Category> categoryObjects = new HashSet<>();
        for (String categoryName : categoryNames) {
            categoryObjects.add(new Category(categoryName, priority));
        }
        return new RuleGroup(id, categoryObjects, name, description, priority, isAndOperator);
    }

    /**
     * Create a new rule group with multiple category objects.
     *
     * @param id The unique identifier of the rule group
     * @param categories The set of category objects this rule group belongs to
     * @param name The name of the rule group
     * @param description The description of what the rule group does
     * @param priority The priority of the rule group (lower numbers = higher priority)
     * @param isAndOperator Whether to use AND (true) or OR (false) to combine rules
     */
    public RuleGroup(String id, Set<Category> categories, String name, String description,
                     int priority, boolean isAndOperator) {
        this.uuid = UUID.randomUUID();
        this.id = id;
        this.categories = new HashSet<>(categories);
        this.name = name;
        this.description = description;
        this.priority = priority;
        this.rulesBySequence = new HashMap<>();
        this.isAndOperator = isAndOperator;
        this.message = description; // Default message is the description
    }

    /**
     * Add a rule to this group with a specific sequence number.
     *
     * @param rule The rule to add
     * @param sequenceNumber The sequence number for this rule within the group
     */
    public void addRule(Rule rule, int sequenceNumber) {
        if (rule == null) {
            System.err.println("Cannot add null rule to group '" + name + "'");
            return;
        }
        rulesBySequence.put(sequenceNumber, rule);
    }

    /**
     * Get the rules in this group, sorted by sequence number.
     *
     * @return A list of rules sorted by sequence number
     */
    public List<Rule> getRules() {
        return rulesBySequence.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(Map.Entry::getValue)
                .collect(Collectors.toList());
    }

    /**
     * Evaluate this rule group against the provided context.
     *
     * @param context The evaluation context
     * @return True if the rule group condition is satisfied, false otherwise
     */
    public boolean evaluate(StandardEvaluationContext context) {
        if (rulesBySequence.isEmpty()) {
            return false;
        }

        // Sort rules by sequence number
        List<Integer> sequenceNumbers = new ArrayList<>(rulesBySequence.keySet());
        sequenceNumbers.sort(Integer::compareTo);

        // Evaluate rules in sequence order
        boolean result = isAndOperator; // Start with true for AND, false for OR
        for (Integer seq : sequenceNumbers) {
            Rule rule = rulesBySequence.get(seq);
            if (rule == null) {
                System.err.println("Null rule found at sequence " + seq + " in group '" + name + "', skipping");
                continue;
            }
            try {
                Expression exp = parser.parseExpression(rule.getCondition());
                Boolean ruleResult = exp.getValue(context, Boolean.class);

                if (ruleResult == null) {
                    ruleResult = false;
                }

                if (isAndOperator) {
                    // AND logic: if any rule is false, the result is false
                    result = result && ruleResult;
                    if (!result) {
                        break; // Short-circuit for AND
                    }
                } else {
                    // OR logic: if any rule is true, the result is true
                    result = result || ruleResult;
                    if (result) {
                        break; // Short-circuit for OR
                    }
                }
            } catch (Exception e) {
                System.err.println("Error evaluating rule '" + rule.getName() + "' in group '" + name + "': " + e.getMessage());
                if (isAndOperator) {
                    // For AND groups, any error means the group fails
                    return false;
                }
                // For OR groups, continue evaluating other rules
            }
        }

        // If the group evaluated to true, update the message
        if (result) {
            updateMessage();
        }

        return result;
    }

    /**
     * Update the message based on the evaluation result
     */
    private void updateMessage() {
        List<Rule> rules = getRules();

        if (rules.isEmpty()) {
            this.message = "No rules in group";
            return;
        }

        if (rules.size() == 1) {
            this.message = rules.get(0).getMessage();
            return;
        }

        StringBuilder messageBuilder = new StringBuilder();
        messageBuilder.append(name).append(": ");

        if (isAndOperator) {
            for (int i = 0; i < rules.size(); i++) {
                if (i > 0) {
                    messageBuilder.append(" AND ");
                }
                messageBuilder.append(rules.get(i).getMessage());
            }
        } else {
            for (int i = 0; i < rules.size(); i++) {
                if (i > 0) {
                    messageBuilder.append(" OR ");
                }
                messageBuilder.append(rules.get(i).getMessage());
            }
        }

        this.message = messageBuilder.toString();
    }

    public String getId() {
        return id;
    }

    public Set<Category> getCategories() {
        return categories;
    }

    /**
     * Add a category to this rule group.
     *
     * @param category The category to add
     */
    public void addCategory(Category category) {
        this.categories.add(category);
    }

    /**
     * Add a category to this rule group by name.
     *
     * @param categoryName The name of the category to add
     * @param sequenceNumber The sequence number of the category
     */
    public void addCategory(String categoryName, int sequenceNumber) {
        this.categories.add(new Category(categoryName, sequenceNumber));
    }

    /**
     * Check if this rule group has a specific category.
     *
     * @param category The category to check
     * @return True if the rule group has the category, false otherwise
     */
    public boolean hasCategory(Category category) {
        return this.categories.contains(category);
    }

    /**
     * Check if this rule group has a category with the specified name.
     *
     * @param categoryName The name of the category to check
     * @return True if the rule group has a category with the specified name, false otherwise
     */
    public boolean hasCategory(String categoryName) {
        return this.categories.stream().anyMatch(c -> c.getName().equals(categoryName));
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public int getPriority() {
        return priority;
    }

    public UUID getUuid() {
        return uuid;
    }

    public boolean isAndOperator() {
        return isAndOperator;
    }

    /**
     * Get the message for this rule group.
     *
     * @return The message
     */
    public String getMessage() {
        return message;
    }
}
