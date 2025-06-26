package dev.mars.rulesengine.core.api;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Fluent builder for validation chains.
 * Provides a readable way to validate multiple conditions against data.
 * 
 * <p>Examples:</p>
 * <pre>
 * // Simple pass/fail validation
 * boolean valid = Rules.validate(customer)
 *     .that("#data.age >= 18", "Must be adult")
 *     .that("#data.email != null", "Email required")
 *     .passes();
 * 
 * // Detailed validation with error messages
 * ValidationResult result = Rules.validate(customer)
 *     .that("#data.age >= 18", "Must be at least 18 years old")
 *     .that("#data.email != null", "Email address is required")
 *     .that("#data.phone != null", "Phone number is required")
 *     .validate();
 * 
 * if (!result.isValid()) {
 *     System.out.println("Validation errors: " + result.getErrors());
 * }
 * </pre>
 */
public class ValidationBuilder {
    
    private final Object data;
    private final Map<String, Object> facts;
    private final List<ValidationRule> rules = new ArrayList<>();
    
    /**
     * Create a validation builder for an object.
     * The object will be available as 'data' in conditions.
     * 
     * @param data The object to validate
     */
    public ValidationBuilder(Object data) {
        this.data = data;
        this.facts = null;
    }
    
    /**
     * Create a validation builder for a map of facts.
     * 
     * @param facts The facts to validate
     */
    public ValidationBuilder(Map<String, Object> facts) {
        this.data = null;
        this.facts = facts;
    }
    
    /**
     * Add a validation condition with an error message.
     * 
     * @param condition The SpEL condition to check
     * @param errorMessage The message to show if validation fails
     * @return This builder for method chaining
     * 
     * @example
     * <pre>
     * builder.that("#data.age >= 18", "Must be at least 18 years old")
     *        .that("#data.email != null", "Email is required");
     * </pre>
     */
    public ValidationBuilder that(String condition, String errorMessage) {
        rules.add(new ValidationRule(condition, errorMessage));
        return this;
    }
    
    /**
     * Add a validation condition with a default error message.
     * 
     * @param condition The SpEL condition to check
     * @return This builder for method chaining
     * 
     * @example
     * <pre>
     * builder.that("#data.age >= 18")
     *        .that("#data.email != null");
     * </pre>
     */
    public ValidationBuilder that(String condition) {
        return that(condition, "Condition failed: " + condition);
    }
    
    /**
     * Add an age validation rule.
     * 
     * @param minimumAge The minimum required age
     * @return This builder for method chaining
     * 
     * @example
     * <pre>
     * builder.minimumAge(18); // Checks #data.age >= 18
     * </pre>
     */
    public ValidationBuilder minimumAge(int minimumAge) {
        return that("#data.age >= " + minimumAge, "Must be at least " + minimumAge + " years old");
    }
    
    /**
     * Add an email required validation rule.
     * 
     * @return This builder for method chaining
     * 
     * @example
     * <pre>
     * builder.emailRequired(); // Checks #data.email != null && #data.email.length() > 0
     * </pre>
     */
    public ValidationBuilder emailRequired() {
        return that("#data.email != null && #data.email.length() > 0", "Email address is required");
    }
    
    /**
     * Add a phone required validation rule.
     * 
     * @return This builder for method chaining
     * 
     * @example
     * <pre>
     * builder.phoneRequired(); // Checks #data.phone != null && #data.phone.length() > 0
     * </pre>
     */
    public ValidationBuilder phoneRequired() {
        return that("#data.phone != null && #data.phone.length() > 0", "Phone number is required");
    }
    
    /**
     * Add a minimum balance validation rule.
     * 
     * @param minimumBalance The minimum required balance
     * @return This builder for method chaining
     * 
     * @example
     * <pre>
     * builder.minimumBalance(1000.0); // Checks #data.balance >= 1000.0
     * </pre>
     */
    public ValidationBuilder minimumBalance(double minimumBalance) {
        return that("#data.balance >= " + minimumBalance, "Minimum balance of $" + minimumBalance + " required");
    }
    
    /**
     * Add a not null validation rule for a field.
     * 
     * @param fieldName The name of the field to check
     * @return This builder for method chaining
     * 
     * @example
     * <pre>
     * builder.notNull("name"); // Checks #data.name != null
     * </pre>
     */
    public ValidationBuilder notNull(String fieldName) {
        return that("#data." + fieldName + " != null", fieldName + " is required");
    }
    
    /**
     * Add a not empty validation rule for a string field.
     * 
     * @param fieldName The name of the field to check
     * @return This builder for method chaining
     * 
     * @example
     * <pre>
     * builder.notEmpty("name"); // Checks #data.name != null && #data.name.length() > 0
     * </pre>
     */
    public ValidationBuilder notEmpty(String fieldName) {
        return that("#data." + fieldName + " != null && #data." + fieldName + ".length() > 0", 
                   fieldName + " cannot be empty");
    }
    
    /**
     * Check if all validation rules pass.
     * This is the simplest way to get a boolean result.
     * 
     * @return true if all validations pass, false otherwise
     * 
     * @example
     * <pre>
     * boolean valid = Rules.validate(customer)
     *     .minimumAge(18)
     *     .emailRequired()
     *     .passes();
     * </pre>
     */
    public boolean passes() {
        return rules.stream().allMatch(rule -> rule.test(getTestData()));
    }
    
    /**
     * Check if any validation rule fails.
     * 
     * @return true if any validation fails, false if all pass
     */
    public boolean fails() {
        return !passes();
    }
    
    /**
     * Perform validation and return detailed results.
     * This provides access to specific error messages.
     * 
     * @return A ValidationResult with success status and error messages
     * 
     * @example
     * <pre>
     * ValidationResult result = Rules.validate(customer)
     *     .minimumAge(18)
     *     .emailRequired()
     *     .validate();
     * 
     * if (!result.isValid()) {
     *     result.getErrors().forEach(System.out::println);
     * }
     * </pre>
     */
    public ValidationResult validate() {
        List<String> errors = new ArrayList<>();
        
        for (ValidationRule rule : rules) {
            if (!rule.test(getTestData())) {
                errors.add(rule.getErrorMessage());
            }
        }
        
        return new ValidationResult(errors.isEmpty(), errors);
    }
    
    /**
     * Get the first validation error, if any.
     * 
     * @return The first error message, or null if all validations pass
     */
    public String getFirstError() {
        for (ValidationRule rule : rules) {
            if (!rule.test(getTestData())) {
                return rule.getErrorMessage();
            }
        }
        return null;
    }
    
    /**
     * Get all validation errors.
     * 
     * @return List of error messages for failed validations
     */
    public List<String> getErrors() {
        return rules.stream()
                .filter(rule -> !rule.test(getTestData()))
                .map(ValidationRule::getErrorMessage)
                .collect(Collectors.toList());
    }
    
    /**
     * Get the number of validation rules.
     * 
     * @return The number of rules in this validation chain
     */
    public int getRuleCount() {
        return rules.size();
    }
    
    // Helper method to get the appropriate test data
    private Object getTestData() {
        return facts != null ? facts : data;
    }
    
    /**
     * Internal class representing a single validation rule.
     */
    private static class ValidationRule {
        private final String condition;
        private final String errorMessage;
        
        public ValidationRule(String condition, String errorMessage) {
            this.condition = condition;
            this.errorMessage = errorMessage;
        }
        
        public boolean test(Object data) {
            try {
                if (data instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> facts = (Map<String, Object>) data;
                    return Rules.check(condition, facts);
                } else {
                    return Rules.check(condition, data);
                }
            } catch (Exception e) {
                return false; // Fail safely
            }
        }
        
        public String getErrorMessage() {
            return errorMessage;
        }
        
        public String getCondition() {
            return condition;
        }
    }
}
