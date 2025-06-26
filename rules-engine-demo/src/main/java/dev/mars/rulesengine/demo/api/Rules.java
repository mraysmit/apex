package dev.mars.rulesengine.demo.api;

import dev.mars.rulesengine.core.api.RulesService;
import dev.mars.rulesengine.core.api.ValidationBuilder;
import dev.mars.rulesengine.core.engine.config.RulesEngine;
import dev.mars.rulesengine.core.engine.model.Rule;

import java.util.Map;

/**
 * Ultra-simple static API for rule evaluation.
 * This class provides the simplest possible interface for common rule scenarios,
 * requiring minimal configuration and setup.
 * 
 * <p>Examples:</p>
 * <pre>
 * // One-liner rule evaluation
 * boolean result = Rules.check("#age >= 18", Map.of("age", 25));
 * 
 * // Named rules for reuse
 * Rules.define("adult", "#age >= 18");
 * boolean isAdult = Rules.test("adult", customer);
 * 
 * // Fluent validation
 * boolean valid = Rules.validate(customer)
 *     .that("#age >= 18", "Must be adult")
 *     .that("#email != null", "Email required")
 *     .passes();
 * </pre>
 * 
 * This is a static utility wrapper around the RulesService class from the core module.
 * It maintains backward compatibility for demo code while allowing the core module
 * to follow proper dependency injection principles.
 */
public class Rules {
    
    // Shared service instance for static methods
    private static final RulesService SERVICE = new RulesService();
    
    /**
     * Evaluate a condition against provided facts in a single line.
     * This is the simplest way to use the rules engine.
     * 
     * @param condition The SpEL condition to evaluate
     * @param facts The facts to evaluate against
     * @return true if the condition evaluates to true
     * 
     * @example
     * <pre>
     * boolean result = Rules.check("#age >= 18", Map.of("age", 25)); // true
     * boolean result = Rules.check("#balance > 1000", Map.of("balance", 500)); // false
     * </pre>
     */
    public static boolean check(String condition, Map<String, Object> facts) {
        return SERVICE.check(condition, facts);
    }
    
    /**
     * Evaluate a condition against a single object.
     * The object will be available as 'data' in the condition.
     * 
     * @param condition The SpEL condition to evaluate (use 'data' to reference the object)
     * @param data The object to evaluate against
     * @return true if the condition evaluates to true
     * 
     * @example
     * <pre>
     * Customer customer = new Customer("John", 25);
     * boolean result = Rules.check("#data.age >= 18", customer); // true
     * </pre>
     */
    public static boolean check(String condition, Object data) {
        return SERVICE.check(condition, data);
    }
    
    /**
     * Define a named rule for reuse.
     * Named rules can be tested multiple times without recompilation.
     * 
     * @param name The name of the rule
     * @param condition The SpEL condition
     * 
     * @example
     * <pre>
     * Rules.define("adult", "#age >= 18");
     * Rules.define("premium", "#balance > 5000 && #membershipLevel == 'Gold'");
     * </pre>
     */
    public static void define(String name, String condition) {
        SERVICE.define(name, condition);
    }
    
    /**
     * Define a named rule with a custom message.
     * 
     * @param name The name of the rule
     * @param condition The SpEL condition
     * @param message The message when the rule matches
     * 
     * @example
     * <pre>
     * Rules.define("adult", "#age >= 18", "Customer is an adult");
     * Rules.define("premium", "#balance > 5000", "Eligible for premium services");
     * </pre>
     */
    public static void define(String name, String condition, String message) {
        SERVICE.define(name, condition, message);
    }
    
    /**
     * Test a previously defined named rule against facts.
     * 
     * @param ruleName The name of the rule to test
     * @param facts The facts to evaluate against
     * @return true if the rule matches
     * 
     * @example
     * <pre>
     * Rules.define("adult", "#age >= 18");
     * boolean result = Rules.test("adult", Map.of("age", 25)); // true
     * </pre>
     */
    public static boolean test(String ruleName, Map<String, Object> facts) {
        return SERVICE.test(ruleName, facts);
    }
    
    /**
     * Test a previously defined named rule against a single object.
     * 
     * @param ruleName The name of the rule to test
     * @param data The object to evaluate against
     * @return true if the rule matches
     * 
     * @example
     * <pre>
     * Rules.define("adult", "#data.age >= 18");
     * Customer customer = new Customer("John", 25);
     * boolean result = Rules.test("adult", customer); // true
     * </pre>
     */
    public static boolean test(String ruleName, Object data) {
        return SERVICE.test(ruleName, data);
    }
    
    /**
     * Start a fluent validation chain for an object.
     * This provides a readable way to validate multiple conditions.
     * 
     * @param data The object to validate
     * @return A validation builder for chaining conditions
     * 
     * @example
     * <pre>
     * boolean valid = Rules.validate(customer)
     *     .that("#data.age >= 18", "Must be adult")
     *     .that("#data.email != null", "Email required")
     *     .passes();
     * </pre>
     */
    public static ValidationBuilder validate(Object data) {
        return SERVICE.validate(data);
    }
    
    /**
     * Start a fluent validation chain for a map of facts.
     * 
     * @param facts The facts to validate
     * @return A validation builder for chaining conditions
     * 
     * @example
     * <pre>
     * boolean valid = Rules.validate(Map.of("age", 25, "email", "john@example.com"))
     *     .that("#age >= 18", "Must be adult")
     *     .that("#email != null", "Email required")
     *     .passes();
     * </pre>
     */
    public static ValidationBuilder validate(Map<String, Object> facts) {
        return SERVICE.validate(facts);
    }
    
    /**
     * Get the underlying rules engine for advanced operations.
     * Use this when you need access to the full API.
     * 
     * @return The underlying RulesEngine instance
     */
    public static RulesEngine getEngine() {
        return SERVICE.getEngine();
    }
    
    /**
     * Clear all named rules.
     * Useful for testing or when you want to start fresh.
     */
    public static void clearNamedRules() {
        SERVICE.clearNamedRules();
    }
    
    /**
     * Get all defined rule names.
     * 
     * @return Array of rule names
     */
    public static String[] getDefinedRules() {
        return SERVICE.getDefinedRules();
    }
    
    /**
     * Check if a named rule exists.
     * 
     * @param ruleName The name of the rule to check
     * @return true if the rule is defined
     */
    public static boolean isDefined(String ruleName) {
        return SERVICE.isDefined(ruleName);
    }
}