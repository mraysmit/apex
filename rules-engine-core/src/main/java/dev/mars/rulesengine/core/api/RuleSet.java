package dev.mars.rulesengine.core.api;

import dev.mars.rulesengine.core.engine.config.RulesEngine;
import dev.mars.rulesengine.core.engine.config.RulesEngineConfiguration;
import dev.mars.rulesengine.core.engine.model.Rule;
import dev.mars.rulesengine.core.engine.model.RuleResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Template-based rule set creation for structured rule management.
 * This class provides pre-built rule templates for common scenarios.
 * 
 * <p>Examples:</p>
 * <pre>
 * // Validation rules
 * RuleEngine validation = RuleSet.validation()
 *     .ageCheck(18)
 *     .emailRequired()
 *     .phoneRequired()
 *     .build();
 * 
 * // Business rules
 * RuleEngine business = RuleSet.business()
 *     .premiumEligibility("#balance > 5000 && #membershipYears > 2")
 *     .discountEligibility("#age > 65 || #membershipLevel == 'Gold'")
 *     .build();
 * 
 * // Eligibility rules
 * RuleEngine eligibility = RuleSet.eligibility()
 *     .minimumAge(18)
 *     .minimumIncome(50000)
 *     .creditScoreCheck(650)
 *     .build();
 * </pre>
 */
public class RuleSet {
    
    /**
     * Create a validation rule set builder.
     * Validation rules are typically used for data validation and input checking.
     * 
     * @return A new validation rule set builder
     */
    public static ValidationRuleSet validation() {
        return new ValidationRuleSet();
    }
    
    /**
     * Create a business rule set builder.
     * Business rules encode business logic and decision-making criteria.
     * 
     * @return A new business rule set builder
     */
    public static BusinessRuleSet business() {
        return new BusinessRuleSet();
    }
    
    /**
     * Create an eligibility rule set builder.
     * Eligibility rules determine qualification for services, products, or benefits.
     * 
     * @return A new eligibility rule set builder
     */
    public static EligibilityRuleSet eligibility() {
        return new EligibilityRuleSet();
    }
    
    /**
     * Create a financial rule set builder.
     * Financial rules handle money-related validations and business logic.
     * 
     * @return A new financial rule set builder
     */
    public static FinancialRuleSet financial() {
        return new FinancialRuleSet();
    }
    
    /**
     * Base class for rule set builders.
     */
    public abstract static class BaseRuleSet {
        protected final List<Rule> rules = new ArrayList<>();
        protected final RulesEngineConfiguration config = new RulesEngineConfiguration();
        protected int nextPriority = 1;
        
        /**
         * Add a custom rule to this rule set.
         * 
         * @param name The name of the rule
         * @param condition The SpEL condition
         * @param message The message when the rule matches
         * @return This builder for method chaining
         */
        public BaseRuleSet customRule(String name, String condition, String message) {
            Rule rule = createRule(name, condition, message, getCategory());
            rules.add(rule);
            return this;
        }
        
        /**
         * Build a rules engine with all the configured rules.
         * 
         * @return A new RulesEngine instance with the configured rules
         */
        public RulesEngine build() {
            // Register all rules with the configuration
            for (Rule rule : rules) {
                config.registerRule(rule);
            }
            return new RulesEngine(config);
        }
        
        /**
         * Get all the rules in this rule set.
         * 
         * @return List of configured rules
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
        
        protected abstract String getCategory();
        
        protected Rule createRule(String name, String condition, String message, String category) {
            String id = category + "-" + name.toLowerCase().replaceAll("\\s+", "-") + "-" + nextPriority;
            Rule rule = config.rule(id)
                    .withCategory(category)
                    .withName(name)
                    .withCondition(condition)
                    .withMessage(message)
                    .withPriority(nextPriority++)
                    .build();
            return rule;
        }
    }
    
    /**
     * Builder for validation rule sets.
     */
    public static class ValidationRuleSet extends BaseRuleSet {
        
        @Override
        protected String getCategory() {
            return "validation";
        }
        
        /**
         * Add an age validation rule.
         * 
         * @param minimumAge The minimum required age
         * @return This builder for method chaining
         */
        public ValidationRuleSet ageCheck(int minimumAge) {
            Rule rule = createRule(
                "Age Check", 
                "#age >= " + minimumAge, 
                "Must be at least " + minimumAge + " years old",
                getCategory()
            );
            rules.add(rule);
            return this;
        }
        
        /**
         * Add an email required validation rule.
         * 
         * @return This builder for method chaining
         */
        public ValidationRuleSet emailRequired() {
            Rule rule = createRule(
                "Email Required", 
                "#email != null && #email.length() > 0", 
                "Email address is required",
                getCategory()
            );
            rules.add(rule);
            return this;
        }
        
        /**
         * Add a phone required validation rule.
         * 
         * @return This builder for method chaining
         */
        public ValidationRuleSet phoneRequired() {
            Rule rule = createRule(
                "Phone Required", 
                "#phone != null && #phone.length() > 0", 
                "Phone number is required",
                getCategory()
            );
            rules.add(rule);
            return this;
        }
        
        /**
         * Add a field required validation rule.
         * 
         * @param fieldName The name of the required field
         * @return This builder for method chaining
         */
        public ValidationRuleSet fieldRequired(String fieldName) {
            Rule rule = createRule(
                fieldName + " Required", 
                "#" + fieldName + " != null", 
                fieldName + " is required",
                getCategory()
            );
            rules.add(rule);
            return this;
        }
        
        /**
         * Add a string length validation rule.
         * 
         * @param fieldName The name of the field to check
         * @param minLength The minimum length
         * @param maxLength The maximum length
         * @return This builder for method chaining
         */
        public ValidationRuleSet stringLength(String fieldName, int minLength, int maxLength) {
            Rule rule = createRule(
                fieldName + " Length Check", 
                "#" + fieldName + " != null && #" + fieldName + ".length() >= " + minLength + 
                " && #" + fieldName + ".length() <= " + maxLength, 
                fieldName + " must be between " + minLength + " and " + maxLength + " characters",
                getCategory()
            );
            rules.add(rule);
            return this;
        }
    }
    
    /**
     * Builder for business rule sets.
     */
    public static class BusinessRuleSet extends BaseRuleSet {
        
        @Override
        protected String getCategory() {
            return "business";
        }
        
        /**
         * Add a premium eligibility rule.
         * 
         * @param condition The SpEL condition for premium eligibility
         * @return This builder for method chaining
         */
        public BusinessRuleSet premiumEligibility(String condition) {
            Rule rule = createRule(
                "Premium Eligibility", 
                condition, 
                "Customer is eligible for premium services",
                getCategory()
            );
            rules.add(rule);
            return this;
        }
        
        /**
         * Add a discount eligibility rule.
         * 
         * @param condition The SpEL condition for discount eligibility
         * @return This builder for method chaining
         */
        public BusinessRuleSet discountEligibility(String condition) {
            Rule rule = createRule(
                "Discount Eligibility", 
                condition, 
                "Customer is eligible for discount",
                getCategory()
            );
            rules.add(rule);
            return this;
        }
        
        /**
         * Add a VIP status rule.
         * 
         * @param condition The SpEL condition for VIP status
         * @return This builder for method chaining
         */
        public BusinessRuleSet vipStatus(String condition) {
            Rule rule = createRule(
                "VIP Status", 
                condition, 
                "Customer has VIP status",
                getCategory()
            );
            rules.add(rule);
            return this;
        }
    }
    
    /**
     * Builder for eligibility rule sets.
     */
    public static class EligibilityRuleSet extends BaseRuleSet {
        
        @Override
        protected String getCategory() {
            return "eligibility";
        }
        
        /**
         * Add a minimum age eligibility rule.
         * 
         * @param minimumAge The minimum required age
         * @return This builder for method chaining
         */
        public EligibilityRuleSet minimumAge(int minimumAge) {
            Rule rule = createRule(
                "Minimum Age", 
                "#age >= " + minimumAge, 
                "Must be at least " + minimumAge + " years old",
                getCategory()
            );
            rules.add(rule);
            return this;
        }
        
        /**
         * Add a minimum income eligibility rule.
         * 
         * @param minimumIncome The minimum required income
         * @return This builder for method chaining
         */
        public EligibilityRuleSet minimumIncome(double minimumIncome) {
            Rule rule = createRule(
                "Minimum Income", 
                "#income >= " + minimumIncome, 
                "Minimum income of $" + minimumIncome + " required",
                getCategory()
            );
            rules.add(rule);
            return this;
        }
        
        /**
         * Add a credit score eligibility rule.
         * 
         * @param minimumScore The minimum required credit score
         * @return This builder for method chaining
         */
        public EligibilityRuleSet creditScoreCheck(int minimumScore) {
            Rule rule = createRule(
                "Credit Score Check", 
                "#creditScore >= " + minimumScore, 
                "Minimum credit score of " + minimumScore + " required",
                getCategory()
            );
            rules.add(rule);
            return this;
        }
    }
    
    /**
     * Builder for financial rule sets.
     */
    public static class FinancialRuleSet extends BaseRuleSet {
        
        @Override
        protected String getCategory() {
            return "financial";
        }
        
        /**
         * Add a minimum balance rule.
         * 
         * @param minimumBalance The minimum required balance
         * @return This builder for method chaining
         */
        public FinancialRuleSet minimumBalance(double minimumBalance) {
            Rule rule = createRule(
                "Minimum Balance", 
                "#balance >= " + minimumBalance, 
                "Minimum balance of $" + minimumBalance + " required",
                getCategory()
            );
            rules.add(rule);
            return this;
        }
        
        /**
         * Add a transaction limit rule.
         * 
         * @param maxAmount The maximum transaction amount
         * @return This builder for method chaining
         */
        public FinancialRuleSet transactionLimit(double maxAmount) {
            Rule rule = createRule(
                "Transaction Limit", 
                "#amount <= " + maxAmount, 
                "Transaction amount cannot exceed $" + maxAmount,
                getCategory()
            );
            rules.add(rule);
            return this;
        }
        
        /**
         * Add a KYC verification rule.
         * 
         * @return This builder for method chaining
         */
        public FinancialRuleSet kycRequired() {
            Rule rule = createRule(
                "KYC Required", 
                "#kycVerified == true", 
                "KYC verification is required",
                getCategory()
            );
            rules.add(rule);
            return this;
        }
    }
}
