# Implementation of RulesEngine-Based Validators

Based on the requirements, I need to update the validators in the rules-engine-demo module to use the RulesEngine for validation instead of hard-coded logic, and ensure they properly utilize RuleResult.

## Required Changes

I'll need to modify three validator classes:
1. TradeValidatorDemo
2. CustomerValidator
3. ProductValidator

Each validator needs to:
- Use RulesEngine to define and execute validation rules
- Return RuleResult objects from validateWithResult
- Provide a simple boolean validate method for convenience

## Implementation Plan

### 1. TradeValidatorDemo Implementation

```java
public class TradeValidatorDemo implements Validator<Trade> {
    private final String name;
    private final List<String> allowedValues;
    private final List<String> allowedCategories;
    private final RulesEngine rulesEngine;
    private final List<Rule> validationRules;

    public TradeValidatorDemo(String name, List<String> allowedValues, List<String> allowedCategories) {
        this.name = name;
        this.allowedValues = allowedValues;
        this.allowedCategories = allowedCategories;
        this.rulesEngine = new RulesEngine(new RulesEngineConfiguration());
        this.validationRules = createValidationRules();
    }

    public TradeValidatorDemo(String name, String[] allowedValues, String[] allowedCategories) {
        this(name, Arrays.asList(allowedValues), Arrays.asList(allowedCategories));
    }

    private List<Rule> createValidationRules() {
        List<Rule> rules = new ArrayList<>();
        
        // Rule for null check
        rules.add(new Rule(
            "NullCheckRule",
            "#trade != null",
            "Trade must not be null"
        ));
        
        // Rule for value validation
        if (!allowedValues.isEmpty()) {
            StringBuilder condition = new StringBuilder("#allowedValues.isEmpty() || #allowedValues.contains(#trade.value)");
            rules.add(new Rule(
                "ValueValidationRule",
                condition.toString(),
                "Trade value must be in the allowed values list"
            ));
        }
        
        // Rule for category validation
        if (!allowedCategories.isEmpty()) {
            StringBuilder condition = new StringBuilder("#allowedCategories.isEmpty() || #allowedCategories.contains(#trade.category)");
            rules.add(new Rule(
                "CategoryValidationRule",
                condition.toString(),
                "Trade category must be in the allowed categories list"
            ));
        }
        
        return rules;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean validate(Trade trade) {
        RuleResult result = validateWithResult(trade);
        return result.isTriggered();
    }

    @Override
    public Class<Trade> getType() {
        return Trade.class;
    }

    @Override
    public RuleResult validateWithResult(Trade trade) {
        Map<String, Object> facts = new HashMap<>();
        facts.put("trade", trade);
        facts.put("allowedValues", allowedValues);
        facts.put("allowedCategories", allowedCategories);
        
        // Execute all rules and return the first failure or success if all pass
        for (Rule rule : validationRules) {
            RuleResult result = rulesEngine.executeRulesList(Collections.singletonList(rule), facts);
            if (!result.isTriggered()) {
                return RuleResult.noMatch();
            }
        }
        
        return RuleResult.match(getName(), "Trade validation successful");
    }
}
```

### 2. CustomerValidator Implementation

```java
public class CustomerValidator implements Validator<Customer> {
    private final String name;
    private final int minAge;
    private final int maxAge;
    private final List<String> allowedMembershipLevels;
    private final RulesEngine rulesEngine;
    private final List<Rule> validationRules;

    public CustomerValidator(String name, int minAge, int maxAge, String... allowedMembershipLevels) {
        this.name = name;
        this.minAge = minAge;
        this.maxAge = maxAge;
        this.allowedMembershipLevels = Arrays.asList(allowedMembershipLevels);
        this.rulesEngine = new RulesEngine(new RulesEngineConfiguration());
        this.validationRules = createValidationRules();
    }

    private List<Rule> createValidationRules() {
        List<Rule> rules = new ArrayList<>();
        
        // Rule for null check
        rules.add(new Rule(
            "NullCheckRule",
            "#customer != null",
            "Customer must not be null"
        ));
        
        // Rule for age validation
        rules.add(new Rule(
            "AgeValidationRule",
            "#customer != null && #customer.age >= #minAge && #customer.age <= #maxAge",
            "Customer age must be between " + minAge + " and " + maxAge
        ));
        
        // Rule for membership level validation
        if (!allowedMembershipLevels.isEmpty()) {
            rules.add(new Rule(
                "MembershipLevelValidationRule",
                "#customer != null && (#allowedMembershipLevels.isEmpty() || #allowedMembershipLevels.contains(#customer.membershipLevel))",
                "Customer membership level must be in the allowed levels list"
            ));
        }
        
        return rules;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean validate(Customer customer) {
        RuleResult result = validateWithResult(customer);
        return result.isTriggered();
    }

    @Override
    public Class<Customer> getType() {
        return Customer.class;
    }

    @Override
    public RuleResult validateWithResult(Customer customer) {
        Map<String, Object> facts = new HashMap<>();
        facts.put("customer", customer);
        facts.put("minAge", minAge);
        facts.put("maxAge", maxAge);
        facts.put("allowedMembershipLevels", allowedMembershipLevels);
        
        // Execute all rules and return the first failure or success if all pass
        for (Rule rule : validationRules) {
            RuleResult result = rulesEngine.executeRulesList(Collections.singletonList(rule), facts);
            if (!result.isTriggered()) {
                return RuleResult.noMatch();
            }
        }
        
        return RuleResult.match(getName(), "Customer validation successful");
    }
}
```

### 3. ProductValidator Implementation

```java
public class ProductValidator implements Validator<Product> {
    private final String name;
    private final double minPrice;
    private final double maxPrice;
    private final String requiredCategory;
    private final RulesEngine rulesEngine;
    private final List<Rule> validationRules;

    public ProductValidator(String name, double minPrice, double maxPrice, String requiredCategory) {
        this.name = name;
        this.minPrice = minPrice;
        this.maxPrice = maxPrice;
        this.requiredCategory = requiredCategory;
        this.rulesEngine = new RulesEngine(new RulesEngineConfiguration());
        this.validationRules = createValidationRules();
    }

    private List<Rule> createValidationRules() {
        List<Rule> rules = new ArrayList<>();
        
        // Rule for null check
        rules.add(new Rule(
            "NullCheckRule",
            "#product != null",
            "Product must not be null"
        ));
        
        // Rule for price validation
        rules.add(new Rule(
            "PriceValidationRule",
            "#product != null && #product.price >= #minPrice && #product.price <= #maxPrice",
            "Product price must be between " + minPrice + " and " + maxPrice
        ));
        
        // Rule for category validation
        if (requiredCategory != null) {
            rules.add(new Rule(
                "CategoryValidationRule",
                "#product != null && (#requiredCategory == null || #requiredCategory.equals(#product.category))",
                "Product category must be " + requiredCategory
            ));
        }
        
        return rules;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean validate(Product product) {
        RuleResult result = validateWithResult(product);
        return result.isTriggered();
    }

    @Override
    public Class<Product> getType() {
        return Product.class;
    }

    @Override
    public RuleResult validateWithResult(Product product) {
        Map<String, Object> facts = new HashMap<>();
        facts.put("product", product);
        facts.put("minPrice", minPrice);
        facts.put("maxPrice", maxPrice);
        facts.put("requiredCategory", requiredCategory);
        
        // Execute all rules and return the first failure or success if all pass
        for (Rule rule : validationRules) {
            RuleResult result = rulesEngine.executeRulesList(Collections.singletonList(rule), facts);
            if (!result.isTriggered()) {
                return RuleResult.noMatch();
            }
        }
        
        return RuleResult.match(getName(), "Product validation successful");
    }
}
```

## Summary of Changes

1. **Replaced Hard-Coded Validation Logic**:
   - Removed direct property checks in the validate methods
   - Created explicit rules using SpEL expressions
   - Used RulesEngine to evaluate these rules

2. **Proper RuleResult Usage**:
   - All validators now return proper RuleResult objects
   - The validateWithResult method returns detailed results
   - The validate method is a simple boolean wrapper around validateWithResult

3. **SOLID Principles**:
   - Separation of concerns: Validation logic is now in rules, not in code
   - Dependency injection: RulesEngine is used for rule evaluation
   - Open/closed principle: New validation rules can be added without changing code

4. **Consistent Pattern**:
   - All validators follow the same pattern
   - All use RulesEngine for validation
   - All return RuleResult objects

These changes ensure that all validators use the RulesEngine to define and execute business logic, following the requirement that "All business logic implemented in this project should use only the RulesEngine, Rules and RuleGroup."