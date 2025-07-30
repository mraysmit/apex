package dev.mars.apex.core.api;

import dev.mars.apex.core.engine.config.RulesEngine;
import dev.mars.apex.core.engine.config.RulesEngineConfiguration;
import dev.mars.apex.core.engine.model.Rule;
import dev.mars.apex.core.engine.model.RuleResult;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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
 * Instance-based service for rule evaluation.
 *
 * This class is part of the PeeGeeQ message queue system, providing
 * production-ready PostgreSQL-based message queuing capabilities.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-07-27
 * @version 1.0
 */
/**
 * Instance-based service for rule evaluation.
 * This class provides the same functionality as the static Rules class,
 * but follows the principle of dependency injection and avoids static methods.
 * 
 * <p>Examples:</p>
 * <pre>
 * // Create an instance
 * RulesService rulesService = new RulesService();
 * 
 * // Rule evaluation
 * boolean result = rulesService.check("#age >= 18", Map.of("age", 25));
 * 
 * // Named rules for reuse
 * rulesService.define("adult", "#age >= 18");
 * boolean isAdult = rulesService.test("adult", customer);
 * 
 * // Fluent validation
 * boolean valid = rulesService.validate(customer)
 *     .that("#age >= 18", "Must be adult")
 *     .that("#email != null", "Email required")
 *     .passes();
 * </pre>
 */
public class RulesService {
    
    private final RulesEngine engine;
    private final Map<String, Rule> namedRules = new ConcurrentHashMap<>();
    
    /**
     * Create a new RulesService with a default RulesEngine.
     */
    public RulesService() {
        RulesEngineConfiguration config = new RulesEngineConfiguration();
        this.engine = new RulesEngine(config);
    }
    
    /**
     * Create a new RulesService with the specified RulesEngine.
     * 
     * @param engine The RulesEngine to use
     */
    public RulesService(RulesEngine engine) {
        this.engine = engine;
    }
    
    /**
     * Evaluate a condition against provided facts.
     * 
     * @param condition The SpEL condition to evaluate
     * @param facts The facts to evaluate against
     * @return true if the condition evaluates to true
     */
    public boolean check(String condition, Map<String, Object> facts) {
        try {
            String ruleId = "check-" + Math.abs(condition.hashCode());
            Rule rule = createSimpleRule(ruleId, "Check", condition, "Condition met");
            RuleResult result = engine.executeRule(rule, facts);
            return result.isTriggered();
        } catch (Exception e) {
            return false; // Fail safely for simple API
        }
    }
    
    /**
     * Evaluate a condition against a single object.
     * The object will be available as 'data' in the condition.
     * 
     * @param condition The SpEL condition to evaluate (use 'data' to reference the object)
     * @param data The object to evaluate against
     * @return true if the condition evaluates to true
     */
    public boolean check(String condition, Object data) {
        Map<String, Object> facts = new HashMap<>();
        facts.put("data", data);
        return check(condition, facts);
    }
    
    /**
     * Define a named rule for reuse.
     * Named rules can be tested multiple times without recompilation.
     * 
     * @param name The name of the rule
     * @param condition The SpEL condition
     */
    public void define(String name, String condition) {
        define(name, condition, "Rule '" + name + "' matched");
    }
    
    /**
     * Define a named rule with a custom message.
     * 
     * @param name The name of the rule
     * @param condition The SpEL condition
     * @param message The message when the rule matches
     */
    public void define(String name, String condition, String message) {
        String ruleId = "named-" + name;
        Rule rule = createSimpleRule(ruleId, name, condition, message);
        namedRules.put(name, rule);
    }
    
    /**
     * Test a previously defined named rule against facts.
     * 
     * @param ruleName The name of the rule to test
     * @param facts The facts to evaluate against
     * @return true if the rule matches
     */
    public boolean test(String ruleName, Map<String, Object> facts) {
        Rule rule = namedRules.get(ruleName);
        if (rule == null) {
            throw new IllegalArgumentException("Rule '" + ruleName + "' not found. Use define() first.");
        }
        
        try {
            RuleResult result = engine.executeRule(rule, facts);
            return result.isTriggered();
        } catch (Exception e) {
            return false; // Fail safely for simple API
        }
    }
    
    /**
     * Test a previously defined named rule against a single object.
     * 
     * @param ruleName The name of the rule to test
     * @param data The object to evaluate against
     * @return true if the rule matches
     */
    public boolean test(String ruleName, Object data) {
        Map<String, Object> facts = new HashMap<>();
        facts.put("data", data);
        return test(ruleName, facts);
    }
    
    /**
     * Start a fluent validation chain for an object.
     * This provides a readable way to validate multiple conditions.
     * 
     * @param data The object to validate
     * @return A validation builder for chaining conditions
     */
    public ValidationBuilder validate(Object data) {
        return new ValidationBuilder(data);
    }
    
    /**
     * Start a fluent validation chain for a map of facts.
     * 
     * @param facts The facts to validate
     * @return A validation builder for chaining conditions
     */
    public ValidationBuilder validate(Map<String, Object> facts) {
        return new ValidationBuilder(facts);
    }
    
    /**
     * Get the underlying rules engine for advanced operations.
     * Use this when you need access to the full API.
     * 
     * @return The underlying RulesEngine instance
     */
    public RulesEngine getEngine() {
        return engine;
    }
    
    /**
     * Clear all named rules.
     * Useful for testing or when you want to start fresh.
     */
    public void clearNamedRules() {
        namedRules.clear();
    }
    
    /**
     * Get all defined rule names.
     * 
     * @return Array of rule names
     */
    public String[] getDefinedRules() {
        return namedRules.keySet().toArray(new String[0]);
    }
    
    /**
     * Check if a named rule exists.
     * 
     * @param ruleName The name of the rule to check
     * @return true if the rule is defined
     */
    public boolean isDefined(String ruleName) {
        return namedRules.containsKey(ruleName);
    }
    
    // Helper method to create simple rules
    private Rule createSimpleRule(String id, String name, String condition, String message) {
        RulesEngineConfiguration config = engine.getConfiguration();
        return config.rule(id)
                .withCategory("simple")
                .withName(name)
                .withCondition(condition)
                .withMessage(message)
                .withPriority(100)
                .build();
    }
}
