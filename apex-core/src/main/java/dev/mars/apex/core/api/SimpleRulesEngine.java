package dev.mars.apex.core.api;

import dev.mars.apex.core.engine.config.RulesEngine;
import dev.mars.apex.core.engine.config.RulesEngineConfiguration;
import dev.mars.apex.core.engine.model.Rule;
import dev.mars.apex.core.engine.model.RuleResult;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

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
 * Simplified API for common rule engine use cases.
 *
 * This class is part of the PeeGeeQ message queue system, providing
 * production-ready PostgreSQL-based message queuing capabilities.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-07-27
 * @version 1.0
 */
public class SimpleRulesEngine {
    private final RulesEngineConfiguration configuration;
    private final RulesEngine engine;
    
    public SimpleRulesEngine() {
        this.configuration = new RulesEngineConfiguration();
        this.engine = new RulesEngine(configuration);
    }
    
    /**
     * Create a simple validation rule.
     * 
     * @param name The name of the rule
     * @param condition The SpEL condition to evaluate
     * @param message The message to return when the rule matches
     * @return A fluent builder for further configuration
     */
    public SimpleRuleBuilder validationRule(String name, String condition, String message) {
        return new SimpleRuleBuilder(this, name, condition, message, "validation");
    }
    
    /**
     * Create a simple business rule.
     * 
     * @param name The name of the rule
     * @param condition The SpEL condition to evaluate
     * @param message The message to return when the rule matches
     * @return A fluent builder for further configuration
     */
    public SimpleRuleBuilder businessRule(String name, String condition, String message) {
        return new SimpleRuleBuilder(this, name, condition, message, "business");
    }
    
    /**
     * Create a simple eligibility rule.
     * 
     * @param name The name of the rule
     * @param condition The SpEL condition to evaluate
     * @param message The message to return when the rule matches
     * @return A fluent builder for further configuration
     */
    public SimpleRuleBuilder eligibilityRule(String name, String condition, String message) {
        return new SimpleRuleBuilder(this, name, condition, message, "eligibility");
    }
    
    /**
     * Check if a customer is eligible for something based on age.
     * 
     * @param customerAge The customer's age
     * @param minimumAge The minimum required age
     * @return true if eligible, false otherwise
     */
    public boolean isAgeEligible(int customerAge, int minimumAge) {
        Map<String, Object> facts = new HashMap<>();
        facts.put("customerAge", customerAge);
        facts.put("minimumAge", minimumAge);
        
        String ruleId = "age-eligibility-" + minimumAge;
        Rule rule = getOrCreateAgeEligibilityRule(ruleId, minimumAge);
        
        RuleResult result = engine.executeRule(rule, facts);
        return result.isTriggered();
    }
    
    /**
     * Check if an amount is within a specified range.
     * 
     * @param amount The amount to check
     * @param minAmount The minimum allowed amount
     * @param maxAmount The maximum allowed amount
     * @return true if within range, false otherwise
     */
    public boolean isAmountInRange(double amount, double minAmount, double maxAmount) {
        Map<String, Object> facts = new HashMap<>();
        facts.put("amount", amount);
        facts.put("minAmount", minAmount);
        facts.put("maxAmount", maxAmount);
        
        String ruleId = "amount-range-" + minAmount + "-" + maxAmount;
        Rule rule = getOrCreateAmountRangeRule(ruleId, minAmount, maxAmount);
        
        RuleResult result = engine.executeRule(rule, facts);
        return result.isTriggered();
    }
    
    /**
     * Validate that required fields are present and not empty.
     * 
     * @param data The data object to validate
     * @param requiredFields The names of required fields
     * @return true if all required fields are present and not empty
     */
    public boolean validateRequiredFields(Object data, String... requiredFields) {
        Map<String, Object> facts = new HashMap<>();
        facts.put("data", data);
        
        for (String field : requiredFields) {
            String ruleId = "required-field-" + field;
            Rule rule = getOrCreateRequiredFieldRule(ruleId, field);
            
            RuleResult result = engine.executeRule(rule, facts);
            if (!result.isTriggered()) {
                return false; // Field is missing or empty
            }
        }
        
        return true; // All fields are present
    }
    
    /**
     * Execute a simple condition against provided data.
     * 
     * @param condition The SpEL condition to evaluate
     * @param data The data to evaluate against
     * @return true if the condition evaluates to true
     */
    public boolean evaluate(String condition, Map<String, Object> data) {
        String ruleId = "simple-condition-" + condition.hashCode();
        Rule rule = getOrCreateSimpleRule(ruleId, condition);
        
        RuleResult result = engine.executeRule(rule, data);
        return result.isTriggered();
    }
    
    /**
     * Execute a simple condition against a single object.
     * 
     * @param condition The SpEL condition to evaluate (use 'data' to reference the object)
     * @param data The object to evaluate against
     * @return true if the condition evaluates to true
     */
    public boolean evaluate(String condition, Object data) {
        Map<String, Object> facts = new HashMap<>();
        facts.put("data", data);
        return evaluate(condition, facts);
    }
    
    /**
     * Get access to the underlying rules engine for advanced operations.
     * 
     * @return The underlying RulesEngine instance
     */
    public RulesEngine getEngine() {
        return engine;
    }
    
    /**
     * Get access to the configuration for advanced setup.
     * 
     * @return The RulesEngineConfiguration instance
     */
    public RulesEngineConfiguration getConfiguration() {
        return configuration;
    }
    
    // Helper methods to create common rules
    
    private Rule getOrCreateAgeEligibilityRule(String ruleId, int minimumAge) {
        Rule existingRule = configuration.getRuleById(ruleId);
        if (existingRule != null) {
            return existingRule;
        }
        
        return configuration.rule(ruleId)
            .withCategory("eligibility")
            .withName("Age Eligibility Check")
            .withDescription("Check if customer meets minimum age requirement")
            .withCondition("#customerAge >= #minimumAge")
            .withMessage("Customer meets age requirement")
            .withPriority(1)
            .build();
    }
    
    private Rule getOrCreateAmountRangeRule(String ruleId, double minAmount, double maxAmount) {
        Rule existingRule = configuration.getRuleById(ruleId);
        if (existingRule != null) {
            return existingRule;
        }
        
        return configuration.rule(ruleId)
            .withCategory("validation")
            .withName("Amount Range Check")
            .withDescription("Check if amount is within specified range")
            .withCondition("#amount >= #minAmount && #amount <= #maxAmount")
            .withMessage("Amount is within valid range")
            .withPriority(1)
            .build();
    }
    
    private Rule getOrCreateRequiredFieldRule(String ruleId, String fieldName) {
        Rule existingRule = configuration.getRuleById(ruleId);
        if (existingRule != null) {
            return existingRule;
        }
        
        return configuration.rule(ruleId)
            .withCategory("validation")
            .withName("Required Field Check: " + fieldName)
            .withDescription("Check if required field is present and not empty")
            .withCondition("#data." + fieldName + " != null && #data." + fieldName + " != ''")
            .withMessage("Required field '" + fieldName + "' is present")
            .withPriority(1)
            .build();
    }
    
    private Rule getOrCreateSimpleRule(String ruleId, String condition) {
        Rule existingRule = configuration.getRuleById(ruleId);
        if (existingRule != null) {
            return existingRule;
        }
        
        return configuration.rule(ruleId)
            .withCategory("simple")
            .withName("Simple Condition")
            .withDescription("Simple condition evaluation")
            .withCondition(condition)
            .withMessage("Condition evaluated to true")
            .withPriority(1)
            .build();
    }
    
    /**
     * Fluent builder for simple rules.
     */
    public static class SimpleRuleBuilder {
        private final SimpleRulesEngine engine;
        private final String name;
        private final String condition;
        private final String message;
        private final String category;
        private int priority = 1;
        private String description;
        
        public SimpleRuleBuilder(SimpleRulesEngine engine, String name, String condition, String message, String category) {
            this.engine = engine;
            this.name = name;
            this.condition = condition;
            this.message = message;
            this.category = category;
            this.description = "Simple rule: " + name;
        }
        
        public SimpleRuleBuilder priority(int priority) {
            this.priority = priority;
            return this;
        }
        
        public SimpleRuleBuilder description(String description) {
            this.description = description;
            return this;
        }
        
        public Rule build() {
            String ruleId = category + "-" + name.toLowerCase().replaceAll("\\s+", "-");
            return engine.configuration.rule(ruleId)
                .withCategory(category)
                .withName(name)
                .withDescription(description)
                .withCondition(condition)
                .withMessage(message)
                .withPriority(priority)
                .build();
        }
        
        /**
         * Build and immediately test the rule with provided data.
         * 
         * @param data The data to test against
         * @return true if the rule matches
         */
        public boolean test(Map<String, Object> data) {
            Rule rule = build();
            RuleResult result = engine.engine.executeRule(rule, data);
            return result.isTriggered();
        }
        
        /**
         * Build and immediately test the rule with a single object.
         * 
         * @param data The object to test against
         * @return true if the rule matches
         */
        public boolean test(Object data) {
            Map<String, Object> facts = new HashMap<>();
            facts.put("data", data);
            return test(facts);
        }
    }
}
