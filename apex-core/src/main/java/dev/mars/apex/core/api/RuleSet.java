package dev.mars.apex.core.api;

import dev.mars.apex.core.engine.config.RulesEngine;
import dev.mars.apex.core.engine.config.RulesEngineConfiguration;
import dev.mars.apex.core.engine.model.Rule;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

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
 * Generic, extensible rule set creation framework.
 *
* This class is part of the APEX A powerful expression processor for Java applications.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-07-27
 * @version 1.0
 */
/**
 * Generic, extensible rule set creation framework.
 *
 * This class provides a completely domain-agnostic approach to rule set creation.
 * Users define their own categories and rule templates without any hardcoded
 * business domain knowledge in the core engine.
 *
 * <p>Key Principles:</p>
 * <ul>
 *   <li><strong>Domain Agnostic</strong>: No hardcoded business categories</li>
 *   <li><strong>User Extensible</strong>: Categories defined by users</li>
 *   <li><strong>Generic Core</strong>: Framework provides structure, not content</li>
 *   <li><strong>Enterprise Ready</strong>: Full metadata and audit support</li>
 * </ul>
 *
 * <p>Examples:</p>
 * <pre>
 * // Users define their own categories - no hardcoded domains
 * GenericRuleSet patientEligibility = RuleSet.category("patient-eligibility")
 *     .withCreatedBy("healthcare.admin@hospital.com")
 *     .withBusinessDomain("Healthcare")
 *     .customRule("Age Check", "#age >= 18", "Patient must be adult")
 *     .customRule("Insurance Check", "#hasInsurance == true", "Insurance required");
 *
 * // Any domain can be supported
 * GenericRuleSet qualityControl = RuleSet.category("quality-control")
 *     .withCreatedBy("qc.manager@manufacturing.com")
 *     .withBusinessDomain("Manufacturing")
 *     .customRule("Temperature Check", "#temperature >= 20 && #temperature <= 25", "Temperature in range")
 *     .customRule("Pressure Check", "#pressure <= 100", "Pressure within limits");
 *
 * // Enterprise governance categories
 * GenericRuleSet dataGovernance = RuleSet.category("data-governance")
 *     .withCreatedBy("governance.officer@company.com")
 *     .withBusinessDomain("Data Management")
 *     .customRule("PII Protection", "#containsPII == false || #encrypted == true", "PII must be encrypted");
 * </pre>
 */
public class RuleSet {

    /**
     * Create a generic rule set for any user-defined category.
     * This is the primary factory method for creating domain-agnostic rule sets.
     *
     * @param categoryName The user-defined category name (e.g., "patient-eligibility",
     *                     "quality-control", "data-governance", "risk-management")
     * @return A new generic rule set builder for the specified category
     * @throws IllegalArgumentException if categoryName is null or empty
     */
    public static GenericRuleSet category(String categoryName) {
        validateCategoryName(categoryName);
        return new GenericRuleSet(categoryName);
    }

    /**
     * Create a typed rule set for advanced scenarios where users want to extend
     * the generic rule set with domain-specific methods.
     *
     * @param <T> The type of rule set to create
     * @param categoryName The user-defined category name
     * @param ruleSetClass The class of the rule set to instantiate
     * @return A new instance of the specified rule set type
     * @throws IllegalArgumentException if parameters are invalid
     * @throws RuntimeException if the rule set cannot be instantiated
     */
    public static <T extends GenericRuleSet> T category(String categoryName, Class<T> ruleSetClass) {
        validateCategoryName(categoryName);
        try {
            return ruleSetClass.getDeclaredConstructor(String.class).newInstance(categoryName);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create rule set of type " + ruleSetClass.getSimpleName(), e);
        }
    }

    /**
     * Validate that a category name is acceptable.
     *
     * @param categoryName The category name to validate
     * @throws IllegalArgumentException if the category name is invalid
     */
    private static void validateCategoryName(String categoryName) {
        if (categoryName == null || categoryName.trim().isEmpty()) {
            throw new IllegalArgumentException("Category name cannot be null or empty");
        }
        if (categoryName.length() > 100) {
            throw new IllegalArgumentException("Category name cannot exceed 100 characters");
        }
        if (!categoryName.matches("^[a-zA-Z0-9][a-zA-Z0-9\\-_]*$")) {
            throw new IllegalArgumentException("Category name must start with alphanumeric character and contain only letters, numbers, hyphens, and underscores");
        }
    }

    /**
     * Generic rule set builder that supports any user-defined category.
     * This class is completely domain-agnostic and provides enterprise-grade
     * features including comprehensive metadata support, validation, and audit trails.
     */
    public static class GenericRuleSet {
        protected final List<Rule> rules = new ArrayList<>();
        protected final RulesEngineConfiguration config = new RulesEngineConfiguration();
        protected final String categoryName;
        protected final AtomicInteger priorityCounter = new AtomicInteger(1);

        // Metadata for all rules in this set
        protected String createdByUser = "system";
        protected String businessDomain;
        protected String businessOwner;
        protected String sourceSystem = "RULE_SET_API";
        protected Instant effectiveDate;
        protected Instant expirationDate;

        /**
         * Create a new generic rule set for the specified category.
         *
         * @param categoryName The user-defined category name
         */
        public GenericRuleSet(String categoryName) {
            this.categoryName = categoryName;
        }

        // === METADATA CONFIGURATION METHODS ===

        /**
         * Set the user who is creating these rules.
         * This is critical for audit trails and governance.
         *
         * @param createdByUser The user creating the rules
         * @return This builder for method chaining
         */
        public GenericRuleSet withCreatedBy(String createdByUser) {
            validateParameter(createdByUser, "createdByUser cannot be null or empty");
            this.createdByUser = createdByUser;
            return this;
        }

        /**
         * Set the business domain for these rules.
         *
         * @param businessDomain The business domain (e.g., "Healthcare", "Manufacturing")
         * @return This builder for method chaining
         */
        public GenericRuleSet withBusinessDomain(String businessDomain) {
            this.businessDomain = businessDomain;
            return this;
        }

        /**
         * Set the business owner for these rules.
         *
         * @param businessOwner The business owner responsible for these rules
         * @return This builder for method chaining
         */
        public GenericRuleSet withBusinessOwner(String businessOwner) {
            this.businessOwner = businessOwner;
            return this;
        }

        /**
         * Set the source system for these rules.
         *
         * @param sourceSystem The system creating these rules
         * @return This builder for method chaining
         */
        public GenericRuleSet withSourceSystem(String sourceSystem) {
            this.sourceSystem = sourceSystem;
            return this;
        }

        /**
         * Set the effective date for these rules.
         *
         * @param effectiveDate When these rules become effective
         * @return This builder for method chaining
         */
        public GenericRuleSet withEffectiveDate(Instant effectiveDate) {
            this.effectiveDate = effectiveDate;
            return this;
        }

        /**
         * Set the expiration date for these rules.
         *
         * @param expirationDate When these rules expire
         * @return This builder for method chaining
         */
        public GenericRuleSet withExpirationDate(Instant expirationDate) {
            this.expirationDate = expirationDate;
            return this;
        }

        // === RULE CREATION METHODS ===

        /**
         * Add a custom rule to this rule set with comprehensive validation and metadata.
         *
         * @param name The name of the rule (must be unique within the set)
         * @param condition The SpEL condition (will be validated)
         * @param message The message when the rule matches
         * @return This builder for method chaining
         * @throws IllegalArgumentException if parameters are invalid
         * @throws IllegalStateException if rule name already exists
         */
        public GenericRuleSet customRule(String name, String condition, String message) {
            validateRuleParameters(name, condition, message);
            checkForDuplicateRuleName(name);

            Rule rule = createRuleWithMetadata(name, condition, message);
            rules.add(rule);
            return this;
        }

        /**
         * Add a custom rule with additional description.
         *
         * @param name The name of the rule
         * @param condition The SpEL condition
         * @param message The message when the rule matches
         * @param description Detailed description of what the rule does
         * @return This builder for method chaining
         */
        public GenericRuleSet customRule(String name, String condition, String message, String description) {
            validateRuleParameters(name, condition, message);
            validateParameter(description, "description cannot be null or empty");
            checkForDuplicateRuleName(name);

            Rule rule = createRuleWithMetadata(name, condition, message, description);
            rules.add(rule);
            return this;
        }

        /**
         * Add multiple rules at once for bulk operations.
         *
         * @param ruleDefinitions List of rule definitions
         * @return This builder for method chaining
         */
        public GenericRuleSet customRules(List<RuleDefinition> ruleDefinitions) {
            validateParameter(ruleDefinitions, "ruleDefinitions cannot be null");

            for (RuleDefinition def : ruleDefinitions) {
                customRule(def.getName(), def.getCondition(), def.getMessage(), def.getDescription());
            }
            return this;
        }

        // === BUILD METHODS ===

        /**
         * Build a rules engine with all the configured rules.
         * Performs final validation and optimization before creating the engine.
         *
         * @return A new RulesEngine instance with the configured rules
         * @throws IllegalStateException if no rules have been added or validation fails
         */
        public RulesEngine build() {
            if (rules.isEmpty()) {
                throw new IllegalStateException("Cannot build rule set with no rules. Add at least one rule.");
            }

            // Perform final validation
            validateRuleSet();

            // Register all rules with the configuration
            for (Rule rule : rules) {
                config.registerRule(rule);
            }

            return new RulesEngine(config);
        }

        /**
         * Get all the rules in this rule set without building an engine.
         * Useful for inspection, testing, or manual rule management.
         *
         * @return Immutable list of configured rules
         */
        public List<Rule> getRules() {
            return new ArrayList<>(rules);
        }

        /**
         * Get the number of rules in this rule set.
         *
         * @return The number of rules
         */
        public int getRuleCount() {
            return rules.size();
        }

        /**
         * Get the category name for this rule set.
         *
         * @return The category name
         */
        public String getCategoryName() {
            return categoryName;
        }

        /**
         * Check if this rule set is empty.
         *
         * @return true if no rules have been added
         */
        public boolean isEmpty() {
            return rules.isEmpty();
        }

        // === PRIVATE HELPER METHODS ===

        /**
         * Create a rule with comprehensive metadata support.
         */
        private Rule createRuleWithMetadata(String name, String condition, String message) {
            return createRuleWithMetadata(name, condition, message, message);
        }

        /**
         * Create a rule with comprehensive metadata support and custom description.
         */
        private Rule createRuleWithMetadata(String name, String condition, String message, String description) {
            String uniqueId = generateUniqueRuleId(name);

            return config.rule(uniqueId)
                    .withCategory(categoryName)
                    .withName(name)
                    .withCondition(condition)
                    .withMessage(message)
                    .withDescription(description)
                    .withPriority(priorityCounter.getAndIncrement())
                    .withCreatedByUser(createdByUser)
                    .withBusinessDomain(businessDomain)
                    .withBusinessOwner(businessOwner)
                    .withSourceSystem(sourceSystem)
                    .withEffectiveDate(effectiveDate)
                    .withExpirationDate(expirationDate)
                    .withCustomProperty("ruleSetCategory", categoryName)
                    .withCustomProperty("creationMethod", "RuleSet.category(\"" + categoryName + "\")")
                    .build();
        }

        /**
         * Generate a unique rule ID that avoids collisions.
         */
        private String generateUniqueRuleId(String ruleName) {
            String sanitizedName = ruleName.toLowerCase()
                .replaceAll("[^a-z0-9\\-_]", "-")
                .replaceAll("-+", "-")
                .replaceAll("^-|-$", "");

            String timestamp = String.valueOf(System.currentTimeMillis() % 100000);
            String uuid = UUID.randomUUID().toString().substring(0, 8);

            return String.format("%s-%s-%s-%s",
                categoryName.toLowerCase(),
                sanitizedName,
                timestamp,
                uuid);
        }

        /**
         * Validate rule parameters before creating a rule.
         */
        private void validateRuleParameters(String name, String condition, String message) {
            validateParameter(name, "Rule name cannot be null or empty");
            validateParameter(condition, "Rule condition cannot be null or empty");
            validateParameter(message, "Rule message cannot be null or empty");

            if (name.length() > 200) {
                throw new IllegalArgumentException("Rule name cannot exceed 200 characters");
            }
            if (condition.length() > 2000) {
                throw new IllegalArgumentException("Rule condition cannot exceed 2000 characters");
            }
            if (message.length() > 500) {
                throw new IllegalArgumentException("Rule message cannot exceed 500 characters");
            }

            // Basic SpEL syntax validation
            if (!condition.trim().startsWith("#") && !condition.contains("true") && !condition.contains("false")) {
                // This is a simple check - more sophisticated validation could be added
                System.out.println("Warning: Rule condition '" + condition + "' may not be valid SpEL syntax");
            }
        }

        /**
         * Check for duplicate rule names within this rule set.
         */
        private void checkForDuplicateRuleName(String name) {
            boolean exists = rules.stream()
                .anyMatch(rule -> rule.getName().equals(name));

            if (exists) {
                throw new IllegalStateException("Rule with name '" + name + "' already exists in this rule set");
            }
        }

        /**
         * Validate the entire rule set before building.
         */
        private void validateRuleSet() {
            // Check for any validation issues across all rules
            Map<String, Long> nameCount = rules.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                    Rule::getName,
                    java.util.stream.Collectors.counting()
                ));

            nameCount.entrySet().stream()
                .filter(entry -> entry.getValue() > 1)
                .findFirst()
                .ifPresent(entry -> {
                    throw new IllegalStateException("Duplicate rule name detected: " + entry.getKey());
                });
        }

        /**
         * Validate a parameter is not null or empty.
         */
        private void validateParameter(Object parameter, String errorMessage) {
            if (parameter == null) {
                throw new IllegalArgumentException(errorMessage);
            }
            if (parameter instanceof String && ((String) parameter).trim().isEmpty()) {
                throw new IllegalArgumentException(errorMessage);
            }
        }
    }

    /**
     * Helper class for defining rules in bulk operations.
     * Provides a simple data structure for rule definitions.
     */
    public static class RuleDefinition {
        private final String name;
        private final String condition;
        private final String message;
        private final String description;

        /**
         * Create a rule definition with name, condition, and message.
         */
        public RuleDefinition(String name, String condition, String message) {
            this(name, condition, message, message);
        }

        /**
         * Create a rule definition with name, condition, message, and description.
         */
        public RuleDefinition(String name, String condition, String message, String description) {
            this.name = name;
            this.condition = condition;
            this.message = message;
            this.description = description;
        }

        public String getName() { return name; }
        public String getCondition() { return condition; }
        public String getMessage() { return message; }
        public String getDescription() { return description; }

        @Override
        public String toString() {
            return "RuleDefinition{name='" + name + "', condition='" + condition + "'}";
        }
    }
}
