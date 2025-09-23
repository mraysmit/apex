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
* This class is part of the APEX A powerful expression processor for Java applications.
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
    private final boolean stopOnFirstFailure;
    private final boolean parallelExecution;
    private final boolean debugMode;
    private String message;

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
        return new RuleGroup(id, categoryObjects, name, description, priority, isAndOperator, true, false, false);
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
        this(id, categories, name, description, priority, isAndOperator, true, false, false);
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
     * Evaluate this rule group against the provided context.
     *
     * @param context The evaluation context
     * @return True if the rule group condition is satisfied, false otherwise
     */
    public boolean evaluate(StandardEvaluationContext context) {
        if (rulesBySequence.isEmpty()) {
            // Empty group behavior:
            // - AND groups: return true (all conditions vacuously satisfied)
            // - OR groups: return false (no conditions satisfied)
            return isAndOperator;
        }

        // Choose evaluation strategy based on configuration
        boolean result;
        if (parallelExecution && rulesBySequence.size() > 1) {
            result = evaluateParallel(context);
        } else {
            result = evaluateSequential(context);
        }

        // Store group result
        groupResult = result;

        // If the group evaluated to true, update the message
        if (result) {
            updateMessage();
        }

        return result;
    }

    /**
     * Evaluate this rule group against the provided context with detailed results.
     * This method provides comprehensive evaluation results including individual rule results
     * and aggregated severity information.
     *
     * @param context The evaluation context
     * @return Complete evaluation result with individual rule results and aggregated severity
     */
    public RuleGroupEvaluationResult evaluateWithDetails(StandardEvaluationContext context) {
        long startTime = System.currentTimeMillis();

        // Clear previous results
        individualRuleResults.clear();

        if (rulesBySequence.isEmpty()) {
            // Empty group behavior:
            // - AND groups: return true (all conditions vacuously satisfied)
            // - OR groups: return false (no conditions satisfied)
            boolean emptyGroupResult = isAndOperator;
            long duration = System.currentTimeMillis() - startTime;
            return new RuleGroupEvaluationResult(
                id, name, emptyGroupResult, new ArrayList<>(), "INFO", isAndOperator, duration
            );
        }

        // Choose evaluation strategy based on configuration
        boolean result;
        if (parallelExecution && rulesBySequence.size() > 1) {
            result = evaluateParallelWithDetails(context);
        } else {
            result = evaluateSequentialWithDetails(context);
        }

        // Store group result
        groupResult = result;

        // If the group evaluated to true, update the message
        if (result) {
            updateMessage();
        }

        // Aggregate severity from individual rule results
        String aggregatedSeverity = severityAggregator.aggregateSeverity(individualRuleResults, isAndOperator);

        long duration = System.currentTimeMillis() - startTime;
        return new RuleGroupEvaluationResult(
            id, name, result, new ArrayList<>(individualRuleResults),
            aggregatedSeverity, isAndOperator, duration
        );
    }

    /**
     * Evaluate rules sequentially with configurable short-circuiting.
     *
     * @param context The evaluation context
     * @return True if the rule group condition is satisfied, false otherwise
     */
    private boolean evaluateSequential(StandardEvaluationContext context) {
        // Clear previous results
        ruleResults.clear();

        // Sort rules by sequence number
        List<Integer> sequenceNumbers = new ArrayList<>(rulesBySequence.keySet());
        sequenceNumbers.sort(Integer::compareTo);

        // Determine if short-circuiting should be used
        boolean useShortCircuit = stopOnFirstFailure && !debugMode;

        // Evaluate rules in sequence order
        boolean result = isAndOperator; // Start with true for AND, false for OR
        int evaluatedCount = 0;
        int passedCount = 0;
        int failedCount = 0;

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

                // Store individual rule result
                ruleResults.put(rule.getId(), ruleResult);

                evaluatedCount++;
                if (ruleResult) {
                    passedCount++;
                } else {
                    failedCount++;
                }

                if (debugMode) {
                    System.out.println("DEBUG: Rule '" + rule.getName() + "' in group '" + name + "' evaluated to: " + ruleResult);
                }

                if (isAndOperator) {
                    // AND logic: if any rule is false, the result is false
                    result = result && ruleResult;
                    if (!result && useShortCircuit) {
                        if (debugMode) {
                            System.out.println("DEBUG: AND group '" + name + "' short-circuited after " + evaluatedCount + " rules");
                        }
                        break; // Short-circuit for AND
                    }
                } else {
                    // OR logic: if any rule is true, the result is true
                    result = result || ruleResult;
                    if (result && useShortCircuit) {
                        if (debugMode) {
                            System.out.println("DEBUG: OR group '" + name + "' short-circuited after " + evaluatedCount + " rules");
                        }
                        break; // Short-circuit for OR
                    }
                }
            } catch (Exception e) {
                evaluatedCount++;
                failedCount++;
                System.err.println("Error evaluating rule '" + rule.getName() + "' in group '" + name + "': " + e.getMessage());

                if (isAndOperator) {
                    // For AND groups, any error means the group fails
                    if (useShortCircuit) {
                        return false;
                    }
                    result = false;
                }
                // For OR groups, continue evaluating other rules
            }
        }

        if (debugMode) {
            System.out.println("DEBUG: Group '" + name + "' evaluation complete. " +
                             "Evaluated: " + evaluatedCount + ", Passed: " + passedCount +
                             ", Failed: " + failedCount + ", Final result: " + result);
        }

        // Store group result
        groupResult = result;

        return result;
    }

    /**
     * Evaluate rules sequentially with detailed result tracking.
     * This method creates RuleResult objects for each evaluated rule.
     *
     * @param context The evaluation context
     * @return True if the rule group condition is satisfied, false otherwise
     */
    private boolean evaluateSequentialWithDetails(StandardEvaluationContext context) {
        // Clear previous results
        ruleResults.clear();

        // Sort rules by sequence number
        List<Integer> sequenceNumbers = new ArrayList<>(rulesBySequence.keySet());
        sequenceNumbers.sort(Integer::compareTo);

        // Determine if short-circuiting should be used
        boolean useShortCircuit = stopOnFirstFailure && !debugMode;

        // Evaluate rules in sequence order
        boolean result = isAndOperator; // Start with true for AND, false for OR
        int evaluatedCount = 0;
        int passedCount = 0;
        int failedCount = 0;

        // Debug logging for empty groups
        if (sequenceNumbers.isEmpty()) {
            System.out.println("DEBUG: Empty group '" + name + "' with operator: " + (isAndOperator ? "AND" : "OR") + ", initial result: " + result);
        }

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

                // Store individual rule result
                ruleResults.put(rule.getId(), ruleResult);

                // Create RuleResult object for severity aggregation
                RuleResult ruleResultObj = ruleResult ?
                    RuleResult.match(rule.getName(), rule.getMessage(), rule.getSeverity()) :
                    RuleResult.noMatch(rule.getName(), rule.getMessage(), rule.getSeverity());
                individualRuleResults.add(ruleResultObj);

                evaluatedCount++;
                if (ruleResult) {
                    passedCount++;
                } else {
                    failedCount++;
                }

                if (debugMode) {
                    System.out.println("DEBUG: Rule '" + rule.getName() + "' in group '" + name + "' evaluated to: " + ruleResult + " (severity: " + rule.getSeverity() + ")");
                }

                if (isAndOperator) {
                    // AND logic: if any rule is false, the result is false
                    result = result && ruleResult;
                    if (!result && useShortCircuit) {
                        if (debugMode) {
                            System.out.println("DEBUG: AND group '" + name + "' short-circuited after " + evaluatedCount + " rules");
                        }
                        break; // Short-circuit for AND
                    }
                } else {
                    // OR logic: if any rule is true, the result is true
                    result = result || ruleResult;
                    if (result && useShortCircuit) {
                        if (debugMode) {
                            System.out.println("DEBUG: OR group '" + name + "' short-circuited after " + evaluatedCount + " rules");
                        }
                        break; // Short-circuit for OR
                    }
                }
            } catch (Exception e) {
                evaluatedCount++;
                failedCount++;
                System.err.println("Error evaluating rule '" + rule.getName() + "' in group '" + name + "': " + e.getMessage());

                // Create error RuleResult object
                RuleResult errorResult = RuleResult.error(rule.getName(),
                    "Error evaluating rule: " + e.getMessage(), rule.getSeverity());
                individualRuleResults.add(errorResult);

                if (isAndOperator) {
                    // For AND groups, any error means the group fails
                    if (useShortCircuit) {
                        return false;
                    }
                    result = false;
                }
                // For OR groups, continue evaluating other rules
            }
        }

        if (debugMode) {
            System.out.println("DEBUG: Group '" + name + "' evaluation complete. " +
                             "Evaluated: " + evaluatedCount + ", Passed: " + passedCount +
                             ", Failed: " + failedCount + ", Final result: " + result);
        }

        // Store group result
        groupResult = result;

        return result;
    }

    /**
     * Evaluate rules in parallel when possible.
     * Note: Parallel execution disables short-circuiting to ensure all rules are evaluated.
     *
     * @param context The evaluation context
     * @return True if the rule group condition is satisfied, false otherwise
     */
    private boolean evaluateParallel(StandardEvaluationContext context) {
        // Clear previous results
        ruleResults.clear();

        // Sort rules by sequence number
        List<Integer> sequenceNumbers = new ArrayList<>(rulesBySequence.keySet());
        sequenceNumbers.sort(Integer::compareTo);

        // Create a list of evaluation tasks
        List<java.util.concurrent.Callable<Boolean>> tasks = new ArrayList<>();
        List<String> ruleNames = new ArrayList<>();
        List<String> ruleIds = new ArrayList<>();

        for (Integer seq : sequenceNumbers) {
            Rule rule = rulesBySequence.get(seq);
            if (rule == null) {
                System.err.println("Null rule found at sequence " + seq + " in group '" + name + "', skipping");
                continue;
            }

            ruleNames.add(rule.getName());
            ruleIds.add(rule.getId());
            tasks.add(() -> {
                try {
                    Expression exp = parser.parseExpression(rule.getCondition());
                    Boolean ruleResult = exp.getValue(context, Boolean.class);

                    if (ruleResult == null) {
                        ruleResult = false;
                    }

                    if (debugMode) {
                        System.out.println("DEBUG: Rule '" + rule.getName() + "' in group '" + name + "' (parallel) evaluated to: " + ruleResult);
                    }

                    return ruleResult;
                } catch (Exception e) {
                    System.err.println("Error evaluating rule '" + rule.getName() + "' in group '" + name + "' (parallel): " + e.getMessage());
                    return false; // Treat exceptions as false
                }
            });
        }

        if (tasks.isEmpty()) {
            return false;
        }

        // Execute tasks in parallel
        java.util.concurrent.ExecutorService executor = java.util.concurrent.Executors.newFixedThreadPool(
            Math.min(tasks.size(), Runtime.getRuntime().availableProcessors())
        );

        try {
            List<java.util.concurrent.Future<Boolean>> futures = executor.invokeAll(tasks);

            // Collect results
            List<Boolean> results = new ArrayList<>();
            for (int i = 0; i < futures.size(); i++) {
                try {
                    Boolean result = futures.get(i).get();
                    results.add(result);
                    // Store individual rule result
                    ruleResults.put(ruleIds.get(i), result);
                } catch (Exception e) {
                    System.err.println("Error getting result for rule '" + ruleNames.get(i) + "' in group '" + name + "': " + e.getMessage());
                    results.add(false);
                    // Store failed result
                    ruleResults.put(ruleIds.get(i), false);
                }
            }

            // Apply AND/OR logic to results
            boolean finalResult = isAndOperator;
            int passedCount = 0;
            int failedCount = 0;

            for (Boolean result : results) {
                if (result) {
                    passedCount++;
                } else {
                    failedCount++;
                }

                if (isAndOperator) {
                    finalResult = finalResult && result;
                } else {
                    finalResult = finalResult || result;
                }
            }

            if (debugMode) {
                System.out.println("DEBUG: Group '" + name + "' parallel evaluation complete. " +
                                 "Total: " + results.size() + ", Passed: " + passedCount +
                                 ", Failed: " + failedCount + ", Final result: " + finalResult);
            }

            // Store group result
            groupResult = finalResult;

            return finalResult;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Parallel evaluation interrupted for group '" + name + "': " + e.getMessage());
            return false;
        } finally {
            executor.shutdown();
        }
    }

    /**
     * Evaluate rules in parallel with detailed result tracking.
     * Note: Parallel execution disables short-circuiting to ensure all rules are evaluated.
     *
     * @param context The evaluation context
     * @return True if the rule group condition is satisfied, false otherwise
     */
    private boolean evaluateParallelWithDetails(StandardEvaluationContext context) {
        // Clear previous results
        ruleResults.clear();

        // Sort rules by sequence number
        List<Integer> sequenceNumbers = new ArrayList<>(rulesBySequence.keySet());
        sequenceNumbers.sort(Integer::compareTo);

        // Create a list of evaluation tasks with rule references
        List<java.util.concurrent.Callable<RuleResult>> tasks = new ArrayList<>();
        List<String> ruleNames = new ArrayList<>();
        List<String> ruleIds = new ArrayList<>();

        for (Integer seq : sequenceNumbers) {
            Rule rule = rulesBySequence.get(seq);
            if (rule == null) {
                System.err.println("Null rule found at sequence " + seq + " in group '" + name + "', skipping");
                continue;
            }

            ruleNames.add(rule.getName());
            ruleIds.add(rule.getId());
            tasks.add(() -> {
                try {
                    Expression exp = parser.parseExpression(rule.getCondition());
                    Boolean ruleResult = exp.getValue(context, Boolean.class);

                    if (ruleResult == null) {
                        ruleResult = false;
                    }

                    if (debugMode) {
                        System.out.println("DEBUG: Rule '" + rule.getName() + "' in group '" + name + "' (parallel) evaluated to: " + ruleResult + " (severity: " + rule.getSeverity() + ")");
                    }

                    // Create RuleResult object
                    return ruleResult ?
                        RuleResult.match(rule.getName(), rule.getMessage(), rule.getSeverity()) :
                        RuleResult.noMatch(rule.getName(), rule.getMessage(), rule.getSeverity());

                } catch (Exception e) {
                    System.err.println("Error evaluating rule '" + rule.getName() + "' in group '" + name + "' (parallel): " + e.getMessage());
                    return RuleResult.error(rule.getName(), "Error evaluating rule: " + e.getMessage(), rule.getSeverity());
                }
            });
        }

        if (tasks.isEmpty()) {
            return false;
        }

        // Execute tasks in parallel
        java.util.concurrent.ExecutorService executor = java.util.concurrent.Executors.newFixedThreadPool(
            Math.min(tasks.size(), Runtime.getRuntime().availableProcessors())
        );

        try {
            List<java.util.concurrent.Future<RuleResult>> futures = executor.invokeAll(tasks);

            // Collect results
            List<Boolean> booleanResults = new ArrayList<>();
            for (int i = 0; i < futures.size(); i++) {
                try {
                    RuleResult ruleResult = futures.get(i).get();
                    individualRuleResults.add(ruleResult);

                    boolean boolResult = ruleResult.isTriggered();
                    booleanResults.add(boolResult);

                    // Store individual rule result
                    ruleResults.put(ruleIds.get(i), boolResult);
                } catch (Exception e) {
                    System.err.println("Error getting result for rule '" + ruleNames.get(i) + "' in group '" + name + "': " + e.getMessage());

                    // Create error result
                    RuleResult errorResult = RuleResult.error(ruleNames.get(i),
                        "Error getting result: " + e.getMessage(), "ERROR");
                    individualRuleResults.add(errorResult);

                    booleanResults.add(false);
                    ruleResults.put(ruleIds.get(i), false);
                }
            }

            // Apply AND/OR logic to results
            boolean finalResult = isAndOperator;
            int passedCount = 0;
            int failedCount = 0;

            for (Boolean result : booleanResults) {
                if (result) {
                    passedCount++;
                } else {
                    failedCount++;
                }

                if (isAndOperator) {
                    finalResult = finalResult && result;
                } else {
                    finalResult = finalResult || result;
                }
            }

            if (debugMode) {
                System.out.println("DEBUG: Group '" + name + "' parallel evaluation complete. " +
                                 "Total: " + booleanResults.size() + ", Passed: " + passedCount +
                                 ", Failed: " + failedCount + ", Final result: " + finalResult);
            }

            // Store group result
            groupResult = finalResult;

            return finalResult;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Parallel evaluation interrupted for group '" + name + "': " + e.getMessage());
            return false;
        } finally {
            executor.shutdown();
        }
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

    /**
     * Get the individual rule results from the last evaluation.
     *
     * @return Map of rule ID to boolean result
     */
    public Map<String, Boolean> getRuleResults() {
        return new HashMap<>(ruleResults);
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
}
