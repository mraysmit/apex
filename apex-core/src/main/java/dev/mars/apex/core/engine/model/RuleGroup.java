package dev.mars.apex.core.engine.model;

import dev.mars.apex.core.constants.ErrorHandlingConstants;
import dev.mars.apex.core.constants.SeverityConstants;
import dev.mars.apex.core.util.EnabledFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
* This class is part of the APEX A powerful expression processor for Java applications.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-07-27
 * @version 1.0
 */
public class RuleGroup implements RuleBase {
    private static final Logger logger = LoggerFactory.getLogger(RuleGroup.class);

    private final UUID uuid;
    private final String id;
    private final Set<Category> categories;
    private final String name;
    private final String description;
    private final int priority;
    private final Map<Integer, Rule> rulesBySequence;
    private final boolean isAndOperator;
    private final boolean stopOnFirstFailure;
    private final boolean parallelExecution;
    private final boolean debugMode;
    private String message;

    /**
     * Error handling strategy for exceptions during rule group evaluation.
     * Valid values: {@link ErrorHandlingConstants#FAIL_FAST}, {@link ErrorHandlingConstants#CONTINUE_ON_ERROR}, {@link ErrorHandlingConstants#SKIP_ON_ERROR}
     * Default: {@link ErrorHandlingConstants#DEFAULT_STRATEGY}
     *
     * @since 2025-07-30
     */
    private final String errorHandling;

    // Enterprise metadata fields
    private String createdBy;
    private String businessDomain;
    private String businessOwner;
    private String sourceSystem;
    private String effectiveDate;
    private String expirationDate;

    // Rule result tracking for conditional mapping support
    private final Map<String, Boolean> ruleResults = new HashMap<>();
    private boolean groupResult = false;

    // Individual rule results for severity aggregation
    private final List<RuleResult> individualRuleResults = new ArrayList<>();
    private final RuleGroupSeverityAggregator severityAggregator = new RuleGroupSeverityAggregator();

    /**
     * Create a new rule group with default execution settings.
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
        this(id, category, name, description, priority, isAndOperator, true, false, false);
    }

    /**
     * Create a new rule group with full configuration options.
     *
     * @param id The unique identifier of the rule group
     * @param category The initial category of the rule group
     * @param name The name of the rule group
     * @param description The description of what the rule group does
     * @param priority The priority of the rule group (lower numbers = higher priority)
     * @param isAndOperator Whether to use AND (true) or OR (false) to combine rules
     * @param stopOnFirstFailure Whether to stop evaluation on first failure (AND) or success (OR)
     * @param parallelExecution Whether to execute rules in parallel when possible
     * @param debugMode Whether to enable debug mode (disables short-circuiting for complete evaluation)
     */
    public RuleGroup(String id, String category, String name, String description,
                     int priority, boolean isAndOperator, boolean stopOnFirstFailure,
                     boolean parallelExecution, boolean debugMode) {
        this(id, category, name, description, priority, isAndOperator, stopOnFirstFailure,
             parallelExecution, debugMode, ErrorHandlingConstants.DEFAULT_STRATEGY);
    }

    /**
     * Create a new rule group with a single category and full configuration options including error handling.
     *
     * @param id The unique identifier of the rule group
     * @param category The initial category of the rule group
     * @param name The name of the rule group
     * @param description The description of what the rule group does
     * @param priority The priority of the rule group (lower numbers = higher priority)
     * @param isAndOperator Whether to use AND (true) or OR (false) to combine rules
     * @param stopOnFirstFailure Whether to stop evaluation on first failure (AND) or success (OR)
     * @param parallelExecution Whether to execute rules in parallel when possible
     * @param debugMode Whether to enable debug mode (disables short-circuiting for complete evaluation)
     * @param errorHandling Error handling strategy: {@link ErrorHandlingConstants#FAIL_FAST}, {@link ErrorHandlingConstants#CONTINUE_ON_ERROR}, or {@link ErrorHandlingConstants#SKIP_ON_ERROR}
     */
    public RuleGroup(String id, String category, String name, String description,
                     int priority, boolean isAndOperator, boolean stopOnFirstFailure,
                     boolean parallelExecution, boolean debugMode, String errorHandling) {
        this.uuid = UUID.randomUUID();
        this.id = id;
        this.categories = new HashSet<>();
        this.categories.add(new Category(category, priority));
        this.name = name;
        this.description = description;
        this.priority = priority;
        this.rulesBySequence = new HashMap<>();
        this.isAndOperator = isAndOperator;
        this.stopOnFirstFailure = stopOnFirstFailure;
        this.parallelExecution = parallelExecution;
        this.debugMode = debugMode;
        this.errorHandling = errorHandling != null ? errorHandling : ErrorHandlingConstants.DEFAULT_STRATEGY;
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
        return new RuleGroup(id, categoryObjects, name, description, priority, isAndOperator, true, false, false, ErrorHandlingConstants.DEFAULT_STRATEGY);
    }

    /**
     * Create a new rule group with multiple category objects and default execution settings.
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
        this(id, categories, name, description, priority, isAndOperator, true, false, false, ErrorHandlingConstants.DEFAULT_STRATEGY);
    }

    /**
     * Create a new rule group with multiple category objects and full configuration options.
     *
     * @param id The unique identifier of the rule group
     * @param categories The set of category objects this rule group belongs to
     * @param name The name of the rule group
     * @param description The description of what the rule group does
     * @param priority The priority of the rule group (lower numbers = higher priority)
     * @param isAndOperator Whether to use AND (true) or OR (false) to combine rules
     * @param stopOnFirstFailure Whether to stop evaluation on first failure (AND) or success (OR)
     * @param parallelExecution Whether to execute rules in parallel when possible
     * @param debugMode Whether to enable debug mode (disables short-circuiting for complete evaluation)
     */
    public RuleGroup(String id, Set<Category> categories, String name, String description,
                     int priority, boolean isAndOperator, boolean stopOnFirstFailure,
                     boolean parallelExecution, boolean debugMode) {
        this(id, categories, name, description, priority, isAndOperator, stopOnFirstFailure,
             parallelExecution, debugMode, ErrorHandlingConstants.DEFAULT_STRATEGY);
    }

    /**
     * Create a new rule group with multiple category objects and full configuration options including error handling.
     *
     * @param id The unique identifier of the rule group
     * @param categories The set of category objects this rule group belongs to
     * @param name The name of the rule group
     * @param description The description of what the rule group does
     * @param priority The priority of the rule group (lower numbers = higher priority)
     * @param isAndOperator Whether to use AND (true) or OR (false) to combine rules
     * @param stopOnFirstFailure Whether to stop evaluation on first failure (AND) or success (OR)
     * @param parallelExecution Whether to execute rules in parallel when possible
     * @param debugMode Whether to enable debug mode (disables short-circuiting for complete evaluation)
     * @param errorHandling Error handling strategy: {@link ErrorHandlingConstants#FAIL_FAST}, {@link ErrorHandlingConstants#CONTINUE_ON_ERROR}, or {@link ErrorHandlingConstants#SKIP_ON_ERROR}
     */
    public RuleGroup(String id, Set<Category> categories, String name, String description,
                     int priority, boolean isAndOperator, boolean stopOnFirstFailure,
                     boolean parallelExecution, boolean debugMode, String errorHandling) {
        this.uuid = UUID.randomUUID();
        this.id = id;
        this.categories = new HashSet<>(categories);
        this.name = name;
        this.description = description;
        this.priority = priority;
        this.rulesBySequence = new HashMap<>();
        this.isAndOperator = isAndOperator;
        this.stopOnFirstFailure = stopOnFirstFailure;
        this.parallelExecution = parallelExecution;
        this.debugMode = debugMode;
        this.errorHandling = errorHandling != null ? errorHandling : ErrorHandlingConstants.DEFAULT_STRATEGY;
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
            logger.error("Cannot add null rule to group '{}'", name);
            return;
        }
        rulesBySequence.put(sequenceNumber, rule);
    }

    /**
     * Check if this rule group stops on first failure (AND) or success (OR).
     *
     * @return true if short-circuit evaluation is enabled
     */
    public boolean isStopOnFirstFailure() {
        return stopOnFirstFailure;
    }

    /**
     * Check if this rule group supports parallel execution.
     *
     * @return true if parallel execution is enabled
     */
    public boolean isParallelExecution() {
        return parallelExecution;
    }

    /**
     * Check if this rule group is in debug mode.
     *
     * @return true if debug mode is enabled (disables short-circuiting)
     */
    public boolean isDebugMode() {
        return debugMode;
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
     * Get the error handling strategy for this rule group.
     *
     * @return The error handling strategy: "fail-fast", "continue-on-error", or "skip-on-error"
     */
    public String getErrorHandling() {
        return errorHandling;
    }

    /**
     * Get the message for this rule group.
     *
     * @return The message
     */
    public String getMessage() {
        return message;
    }

    /**
     * Get the individual rule results from the last evaluation.
     *
     * @return Map of rule ID to boolean result
     */
    public Map<String, Boolean> getRuleResults() {
        return new HashMap<>(ruleResults);
    }

    /**
     * Update the internal rule results map and individual results from an external evaluation.
     * Used by {@code RuleGroupEvaluationService} (Phase 2) to propagate results back to the
     * group so that downstream code (e.g., enrichments referencing {@code #ruleResults['rule-id']})
     * can access individual outcomes.
     *
     * @param results     the individual rule results from the evaluation
     * @param groupResult the overall group boolean result
     */
    public void updateEvaluationResults(List<RuleResult> results, boolean groupResult) {
        this.ruleResults.clear();
        this.individualRuleResults.clear();
        for (RuleResult r : results) {
            if (r.getRuleId() != null) {
                this.ruleResults.put(r.getRuleId(), r.isTriggered());
            } else if (r.getRuleName() != null) {
                this.ruleResults.put(r.getRuleName(), r.isTriggered());
            }
            this.individualRuleResults.add(r);
        }
        this.groupResult = groupResult;
    }

    /**
     * Get the individual rule results with severity information from the last detailed evaluation.
     * This method returns results only if evaluateWithDetails() was called.
     *
     * @return List of individual rule results with severity information
     */
    public List<RuleResult> getIndividualRuleResults() {
        return new ArrayList<>(individualRuleResults);
    }

    /**
     * Get the overall group result from the last evaluation.
     *
     * @return True if the group passed, false otherwise
     */
    public boolean getGroupResult() {
        return groupResult;
    }

    // Enterprise metadata getters and setters
    public String getCreatedBy() {
        return createdBy;
    }

    public RuleGroup setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
        return this;
    }

    public String getBusinessDomain() {
        return businessDomain;
    }

    public RuleGroup setBusinessDomain(String businessDomain) {
        this.businessDomain = businessDomain;
        return this;
    }

    public String getBusinessOwner() {
        return businessOwner;
    }

    public RuleGroup setBusinessOwner(String businessOwner) {
        this.businessOwner = businessOwner;
        return this;
    }

    public String getSourceSystem() {
        return sourceSystem;
    }

    public RuleGroup setSourceSystem(String sourceSystem) {
        this.sourceSystem = sourceSystem;
        return this;
    }

    public String getEffectiveDate() {
        return effectiveDate;
    }

    public RuleGroup setEffectiveDate(String effectiveDate) {
        this.effectiveDate = effectiveDate;
        return this;
    }

    public String getExpirationDate() {
        return expirationDate;
    }

    public RuleGroup setExpirationDate(String expirationDate) {
        this.expirationDate = expirationDate;
        return this;
    }
}
