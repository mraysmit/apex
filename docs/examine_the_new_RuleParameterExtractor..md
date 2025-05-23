```markdown
# Implementing Key-Value Pair Parameters in DynamicCustomerValidatorDemo

## Current Implementation Analysis

The current `DynamicCustomerValidatorDemo` class uses explicit constructor parameters:
```java
public DynamicCustomerValidatorDemo(String name, int minAge, int maxAge, String... allowedMembershipLevels) {
    this.name = name;
    this.minAge = minAge;
    this.maxAge = maxAge;
    this.allowedMembershipLevels = Arrays.asList(allowedMembershipLevels);
    // ...
}
```

This approach has several limitations:
1. It's not flexible - adding new parameters requires changing the constructor signature
2. It doesn't leverage the `RuleParameterExtractor` class effectively
3. It doesn't follow the pattern used in other demo classes like `ProductTransformerDemo` and `CustomerTransformerDemo`

## Proposed Solution

I'll redesign the `DynamicCustomerValidatorDemo` class to use a key-value pair approach for parameters:

1. Replace explicit constructor parameters with a `Map<String, Object>` parameter
2. Use `RuleParameterExtractor` to extract and validate parameters from rules
3. Store parameters in a map rather than as individual fields
4. Update validation methods to use parameters from the map

## Implementation Details

### 1. New Class Structure

```java
public class DynamicCustomerValidatorDemo {
    private final String name;
    private final Map<String, Object> parameters;
    private final RulesEngine rulesEngine;
    private final RuleGroup validationRuleGroup;
    private final StandardEvaluationContext context;

    /**
     * Create a new DynamicCustomerValidatorDemo with the specified parameters.
     *
     * @param name The name of the validator
     * @param parameters Map of validation parameters (minAge, maxAge, allowedMembershipLevels, etc.)
     */
    public DynamicCustomerValidatorDemo(String name, Map<String, Object> parameters) {
        this.name = name;
        this.parameters = new HashMap<>(parameters);
        this.rulesEngine = new RulesEngine(new RulesEngineConfiguration());
        this.context = new StandardEvaluationContext();
        
        // Initialize context with validation parameters
        for (Map.Entry<String, Object> entry : parameters.entrySet()) {
            context.setVariable(entry.getKey(), entry.getValue());
        }
        
        this.validationRuleGroup = createValidationRuleGroup();
        
        // Use RuleParameterExtractor to validate that all required parameters exist
        Set<String> missingParams = RuleParameterExtractor.validateParameters(validationRuleGroup, parameters);
        if (!missingParams.isEmpty()) {
            throw new IllegalArgumentException("Missing required parameters: " + missingParams);
        }
    }
    
    // Rest of the class implementation...
}
```

### 2. Updated Validation Methods

```java
public RuleResult validateWithResult(Customer customer) {
    // Set the customer in the context
    context.setVariable("customer", customer);

    // Create initial facts map with customer data and parameters
    Map<String, Object> initialFacts = new HashMap<>(parameters);
    initialFacts.put("customer", customer);

    // Use RuleParameterExtractor to ensure all required parameters exist in the facts map
    Map<String, Object> facts = RuleParameterExtractor.ensureParameters(validationRuleGroup, initialFacts);

    // Execute the rule group using the rules engine
    return rulesEngine.executeRuleGroupsList(Collections.singletonList(validationRuleGroup), facts);
}
```

### 3. Updated Rule Creation

```java
private RuleGroup createValidationRuleGroup() {
    // Create a rule group with AND operator (all rules must pass)
    RuleGroup ruleGroup = new RuleGroup(
        "CustomerValidationRuleGroup",
        "CustomerValidation",
        name,
        "Validates customer against defined criteria",
        1,
        true // AND operator
    );

    // Rule for null check
    Rule nullCheckRule = new Rule(
        "NullCheckRule",
        "#customer != null",
        "Customer must not be null"
    );
    ruleGroup.addRule(nullCheckRule, 1);

    // Rule for age validation
    int minAge = parameters.containsKey("minAge") ? (int)parameters.get("minAge") : 0;
    int maxAge = parameters.containsKey("maxAge") ? (int)parameters.get("maxAge") : Integer.MAX_VALUE;
    
    Rule ageValidationRule = new Rule(
        "AgeValidationRule",
        "#customer != null && #customer.age >= #minAge && #customer.age <= #maxAge",
        "Customer age must be between " + minAge + " and " + maxAge
    );
    ruleGroup.addRule(ageValidationRule, 2);

    // Rule for membership level validation
    @SuppressWarnings("unchecked")
    List<String> allowedMembershipLevels = parameters.containsKey("allowedMembershipLevels") ? 
        (List<String>)parameters.get("allowedMembershipLevels") : Collections.emptyList();
    
    if (!allowedMembershipLevels.isEmpty()) {
        Rule membershipLevelValidationRule = new Rule(
            "MembershipLevelValidationRule",
            "#customer != null && (#allowedMembershipLevels.isEmpty() || #allowedMembershipLevels.contains(#customer.membershipLevel))",
            "Customer membership level must be in the allowed levels list"
        );
        ruleGroup.addRule(membershipLevelValidationRule, 3);
    }

    return ruleGroup;
}
```

### 4. Updated Main Method Example

```java
public static void main(String[] args) {
    // Create parameters map for senior gold members
    Map<String, Object> seniorGoldParams = new HashMap<>();
    seniorGoldParams.put("minAge", 60);
    seniorGoldParams.put("maxAge", 100);
    seniorGoldParams.put("allowedMembershipLevels", Collections.singletonList("Gold"));
    
    // Create a validator for senior gold members
    DynamicCustomerValidatorDemo seniorGoldValidator = new DynamicCustomerValidatorDemo(
            "seniorGoldValidator", seniorGoldParams);
    
    // Rest of the main method...
}
```

## Benefits of This Approach

1. **Flexibility**: New parameters can be added without changing the constructor signature
2. **Consistency**: Follows the pattern used in other demo classes
3. **Validation**: Uses `RuleParameterExtractor` to validate parameters against rule requirements
4. **Extensibility**: Makes it easier to add new validation rules that use different parameters
5. **Maintainability**: Centralizes parameter handling in one place

## Implementation Notes

1. The `RuleParameterExtractor` is now more explicitly part of the validation process
2. Parameters are checked against rule requirements at construction time
3. The class is more flexible and can handle additional parameters without code changes
4. The implementation follows SOLID principles with clear separation of concerns
```