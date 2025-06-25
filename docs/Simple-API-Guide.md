# Simple Rules Engine API Guide

## Overview

The Simple Rules Engine API provides an easy-to-use interface for common rule evaluation scenarios. It abstracts away the complexity of the full configuration system while still providing access to the underlying power when needed.

## Quick Start

### Basic Setup

```java
import dev.mars.rulesengine.core.api.SimpleRulesEngine;

// Create a simple rules engine instance
SimpleRulesEngine engine = new SimpleRulesEngine();
```

### Common Use Cases

#### 1. Age Eligibility Check

```java
// Check if customer meets minimum age requirement
boolean isEligible = engine.isAgeEligible(25, 18); // true
boolean notEligible = engine.isAgeEligible(16, 21); // false
```

#### 2. Amount Range Validation

```java
// Check if amount is within acceptable range
boolean validAmount = engine.isAmountInRange(100.0, 50.0, 200.0); // true
boolean invalidAmount = engine.isAmountInRange(300.0, 50.0, 200.0); // false
```

#### 3. Required Fields Validation

```java
public class Customer {
    private String name;
    private String email;
    // getters and setters...
}

Customer customer = new Customer("John Doe", "john@example.com");

// Validate required fields are present
boolean valid = engine.validateRequiredFields(customer, "name", "email"); // true

Customer incomplete = new Customer("Jane", null);
boolean invalid = engine.validateRequiredFields(incomplete, "name", "email"); // false
```

#### 4. Simple Condition Evaluation

```java
// Evaluate conditions against data map
Map<String, Object> data = new HashMap<>();
data.put("age", 25);
data.put("status", "active");
data.put("balance", 1000.0);

boolean result = engine.evaluate("#age > 18 && #status == 'active'", data); // true

// Evaluate against single object
Customer customer = new Customer("John", "john@example.com");
boolean result2 = engine.evaluate("#data.name == 'John'", customer); // true
```

## Rule Builders

### Validation Rules

```java
// Create validation rules with fluent API
boolean result = engine.validationRule("Age Check", "#age >= 18", "Customer is adult")
    .priority(1)
    .description("Validates customer age for service eligibility")
    .test(customerData);
```

### Business Rules

```java
// Create business logic rules
boolean eligible = engine.businessRule("Premium Eligibility", 
        "#data.balance > 5000 && #data.membershipLevel == 'Gold'", 
        "Customer eligible for premium services")
    .description("Determines premium service eligibility")
    .test(customer);
```

### Eligibility Rules

```java
// Create eligibility determination rules
Map<String, Object> loanData = new HashMap<>();
loanData.put("creditScore", 750);
loanData.put("income", 75000);
loanData.put("employmentStatus", "employed");

boolean loanEligible = engine.eligibilityRule("Loan Eligibility", 
        "#creditScore >= 700 && #income >= 50000 && #employmentStatus == 'employed'", 
        "Eligible for loan")
    .priority(2)
    .test(loanData);
```

## Advanced Usage

### Access to Underlying Engine

```java
// Get access to full rules engine for advanced operations
RulesEngine fullEngine = engine.getEngine();
RulesEngineConfiguration config = engine.getConfiguration();

// Use advanced features
List<Rule> allRules = config.getAllRules();
RuleResult result = fullEngine.executeRulesForCategory("validation", facts);
```

### Building and Reusing Rules

```java
// Build rules for reuse
Rule ageRule = engine.validationRule("Age Validation", "#age >= 18", "Valid age")
    .description("Standard age validation")
    .build();

// The rule is automatically registered and can be reused
// Subsequent calls with the same parameters will reuse the existing rule
```

## Examples by Domain

### Financial Services

```java
SimpleRulesEngine engine = new SimpleRulesEngine();

// Investment eligibility
Map<String, Object> investor = new HashMap<>();
investor.put("netWorth", 100000);
investor.put("investmentExperience", "intermediate");
investor.put("riskTolerance", "moderate");

boolean canInvest = engine.eligibilityRule("Investment Eligibility",
    "#netWorth >= 50000 && #investmentExperience != 'none'",
    "Eligible for investment products")
    .test(investor);

// Transaction validation
Map<String, Object> transaction = new HashMap<>();
transaction.put("amount", 5000);
transaction.put("accountBalance", 10000);
transaction.put("dailyLimit", 7500);

boolean validTransaction = engine.validationRule("Transaction Validation",
    "#amount <= #accountBalance && #amount <= #dailyLimit",
    "Transaction is valid")
    .test(transaction);
```

### E-commerce

```java
// Product eligibility
Map<String, Object> customer = new HashMap<>();
customer.put("age", 25);
customer.put("membershipLevel", "premium");
customer.put("country", "US");

boolean canPurchase = engine.eligibilityRule("Product Purchase",
    "#age >= 18 && (#membershipLevel == 'premium' || #country == 'US')",
    "Can purchase restricted product")
    .test(customer);

// Discount eligibility
Map<String, Object> order = new HashMap<>();
order.put("totalAmount", 150);
order.put("customerType", "returning");
order.put("itemCount", 5);

boolean discountEligible = engine.businessRule("Discount Eligibility",
    "#totalAmount > 100 && #customerType == 'returning'",
    "Eligible for loyalty discount")
    .test(order);
```

### Healthcare

```java
// Treatment eligibility
Map<String, Object> patient = new HashMap<>();
patient.put("age", 45);
patient.put("condition", "diabetes");
patient.put("insuranceCoverage", true);
patient.put("allergies", Arrays.asList("penicillin"));

boolean treatmentEligible = engine.eligibilityRule("Treatment Eligibility",
    "#age >= 18 && #insuranceCoverage && !#allergies.contains('medication_component')",
    "Eligible for treatment")
    .test(patient);
```

## Best Practices

### 1. Use Descriptive Names

```java
// Good
engine.validationRule("Customer Age Verification", "#age >= 18", "Customer meets age requirement");

// Avoid
engine.validationRule("Rule1", "#age >= 18", "OK");
```

### 2. Provide Clear Messages

```java
// Good
engine.businessRule("Premium Service Eligibility", 
    "#balance > 5000 && #membershipYears >= 2", 
    "Customer qualifies for premium services based on balance and membership duration");

// Avoid
engine.businessRule("Check", "#balance > 5000 && #membershipYears >= 2", "Yes");
```

### 3. Use Safe Navigation for Optional Properties

```java
// Good - handles null values gracefully
engine.evaluate("#customer?.address?.zipCode == '12345'", data);

// Risky - may throw null pointer exception
engine.evaluate("#customer.address.zipCode == '12345'", data);
```

### 4. Validate Input Data

```java
// Validate data before rule evaluation
if (customer != null && customer.getName() != null) {
    boolean result = engine.evaluate("#data.name == 'John'", customer);
}
```

### 5. Use Appropriate Rule Types

- **Validation Rules**: For data validation and format checking
- **Business Rules**: For business logic and process decisions
- **Eligibility Rules**: For determining qualification or access rights

## Error Handling

The Simple API automatically benefits from the enhanced error handling:

```java
try {
    boolean result = engine.evaluate("#customer.invalidProperty", data);
} catch (Exception e) {
    // Errors are automatically handled with recovery strategies
    // Check logs for detailed error context and suggestions
}
```

## Performance Considerations

### Rule Reuse

The Simple API automatically reuses rules with the same parameters:

```java
// First call creates the rule
boolean result1 = engine.isAgeEligible(25, 18);

// Second call reuses the existing rule (better performance)
boolean result2 = engine.isAgeEligible(30, 18);
```

### Batch Operations

For multiple evaluations, consider using the underlying engine:

```java
RulesEngine fullEngine = engine.getEngine();
List<RuleResult> results = fullEngine.executeAllRules(facts);
```

## Migration from Full API

If you're currently using the full configuration API, you can gradually migrate:

```java
// Old way
RulesEngineConfiguration config = new RulesEngineConfiguration();
Rule rule = config.rule("age-check")
    .category("validation")
    .name("Age Check")
    .condition("#age >= 18")
    .message("Valid age")
    .build();

// New simple way
SimpleRulesEngine simple = new SimpleRulesEngine();
boolean result = simple.validationRule("Age Check", "#age >= 18", "Valid age")
    .test(data);

// Or access the underlying configuration for complex scenarios
RulesEngineConfiguration config = simple.getConfiguration();
```
