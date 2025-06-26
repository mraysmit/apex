# SpEL Rules Engine - Best Practices and Patterns

## Overview

This guide provides best practices, common patterns, and anti-patterns for using the SpEL Rules Engine effectively in production environments.

## 🎯 **Choosing the Right API Layer**

### When to Use Ultra-Simple API (Layer 1)

**Use Cases**:
- Quick validation checks
- Prototype development
- Simple business logic
- One-off rule evaluations
- Learning and experimentation

**Examples**:
```java
// Quick validation
if (Rules.check("#age >= 18", customer)) {
    // Process adult customer
}

// Simple business logic
boolean eligible = Rules.check("#balance > 1000 && #creditScore > 650", application);

// Prototype validation
boolean valid = Rules.validate(data)
    .minimumAge(18)
    .emailRequired()
    .passes();
```

**Benefits**:
- Immediate productivity
- No configuration required
- Perfect for learning
- Minimal code overhead

### When to Use Template-Based API (Layer 2)

**Use Cases**:
- Structured validation scenarios
- Domain-specific rule sets
- Reusable rule patterns
- Team standardization
- Medium complexity applications

**Examples**:
```java
// Customer validation service
public class CustomerValidationService {
    private final RulesEngine validationEngine = RuleSet.validation()
        .ageCheck(18)
        .emailRequired()
        .phoneRequired()
        .fieldRequired("firstName")
        .stringLength("lastName", 2, 50)
        .build();
    
    public ValidationResult validateCustomer(Customer customer) {
        // Use structured validation
    }
}

// Financial compliance service
public class ComplianceService {
    private final RulesEngine complianceEngine = RuleSet.financial()
        .minimumBalance(1000)
        .transactionLimit(10000)
        .kycRequired()
        .build();
}
```

**Benefits**:
- Organized rule management
- Domain-specific abstractions
- Team consistency
- Reusable patterns

### When to Use Advanced API (Layer 3)

**Use Cases**:
- Complex rule hierarchies
- Custom rule categories
- Advanced configuration needs
- Integration with existing systems
- Enterprise-grade applications

**Examples**:
```java
// Complex enterprise rules
RulesEngineConfiguration config = new RulesEngineConfiguration();

// Custom categories and priorities
Rule criticalRule = config.rule("critical-001")
    .withCategory("security")
    .withPriority(1)
    .withCondition("complex security expression")
    .build();

Rule businessRule = config.rule("business-001")
    .withCategory("business-logic")
    .withPriority(10)
    .withCondition("complex business expression")
    .build();

RulesEngine engine = new RulesEngine(config);
```

**Benefits**:
- Full control and flexibility
- Custom categorization
- Advanced error handling
- Enterprise integration

---

## 🏗️ **Architecture Patterns**

### 1. Service Layer Pattern

**Structure**: Encapsulate rule logic in dedicated service classes.

```java
@Service
public class CustomerEligibilityService {
    
    private final RulesEngine eligibilityEngine;
    
    public CustomerEligibilityService() {
        this.eligibilityEngine = RuleSet.eligibility()
            .minimumAge(18)
            .minimumIncome(50000)
            .creditScoreCheck(650)
            .build();
    }
    
    public EligibilityResult checkEligibility(Customer customer) {
        Map<String, Object> facts = customerToFacts(customer);
        
        List<Rule> rules = eligibilityEngine.getConfiguration().getAllRules();
        List<RuleResult> results = eligibilityEngine.executeRules(rules, facts);
        
        return new EligibilityResult(results);
    }
    
    private Map<String, Object> customerToFacts(Customer customer) {
        // Convert customer to facts map
        return Map.of(
            "age", customer.getAge(),
            "income", customer.getIncome(),
            "creditScore", customer.getCreditScore()
        );
    }
}
```

### 2. Factory Pattern for Rule Sets

**Structure**: Use factories to create different rule configurations.

```java
public class RuleSetFactory {
    
    public static RulesEngine createValidationEngine(ValidationLevel level) {
        RuleSet.ValidationRuleSet builder = RuleSet.validation();
        
        switch (level) {
            case BASIC:
                return builder.ageCheck(18).emailRequired().build();
            case STANDARD:
                return builder.ageCheck(18).emailRequired().phoneRequired().build();
            case COMPREHENSIVE:
                return builder.ageCheck(18).emailRequired().phoneRequired()
                    .fieldRequired("firstName").fieldRequired("lastName")
                    .stringLength("address", 10, 200).build();
            default:
                throw new IllegalArgumentException("Unknown validation level: " + level);
        }
    }
    
    public static RulesEngine createBusinessEngine(BusinessDomain domain) {
        switch (domain) {
            case FINANCIAL:
                return RuleSet.financial()
                    .minimumBalance(1000)
                    .transactionLimit(10000)
                    .kycRequired()
                    .build();
            case ECOMMERCE:
                return RuleSet.business()
                    .premiumEligibility("#totalPurchases > 5000")
                    .discountEligibility("#membershipLevel == 'Gold'")
                    .build();
            default:
                throw new IllegalArgumentException("Unknown business domain: " + domain);
        }
    }
}
```

### 3. Chain of Responsibility Pattern

**Structure**: Chain multiple rule sets for complex validation flows.

```java
public class ValidationChain {
    
    private final List<ValidationStep> steps;
    
    public ValidationChain() {
        this.steps = Arrays.asList(
            new BasicValidationStep(),
            new BusinessValidationStep(),
            new ComplianceValidationStep()
        );
    }
    
    public ValidationResult validate(Object data) {
        for (ValidationStep step : steps) {
            ValidationResult result = step.validate(data);
            if (!result.isValid()) {
                return result; // Fail fast
            }
        }
        return ValidationResult.success();
    }
    
    private interface ValidationStep {
        ValidationResult validate(Object data);
    }
    
    private static class BasicValidationStep implements ValidationStep {
        private final RulesEngine engine = RuleSet.validation()
            .ageCheck(18).emailRequired().build();
            
        @Override
        public ValidationResult validate(Object data) {
            // Implement validation logic
            return ValidationResult.success();
        }
    }
}
```

### 4. Strategy Pattern for Rule Selection

**Structure**: Use strategies to select appropriate rule sets dynamically.

```java
public class RuleSelectionStrategy {
    
    public RulesEngine selectRulesEngine(Context context) {
        if (context.isHighRisk()) {
            return createHighRiskRules();
        } else if (context.isPremiumCustomer()) {
            return createPremiumRules();
        } else {
            return createStandardRules();
        }
    }
    
    private RulesEngine createHighRiskRules() {
        return RuleSet.financial()
            .minimumBalance(10000)
            .transactionLimit(1000)
            .kycRequired()
            .customRule("Enhanced Due Diligence", 
                       "#riskScore > 8", 
                       "Enhanced due diligence required")
            .build();
    }
}
```

---

## 🎯 **Performance Best Practices**

### 1. Rule Optimization

**Optimize Rule Conditions**:
```java
// ❌ Inefficient - complex nested conditions
"#customer.address != null && #customer.address.country != null && #customer.address.country.code == 'US' && #customer.age >= 18 && #customer.income > 50000"

// ✅ Efficient - simplified conditions
"#country == 'US' && #age >= 18 && #income > 50000"
```

**Use Performance Monitoring**:
```java
// Monitor rule performance
RulePerformanceMonitor monitor = engine.getPerformanceMonitor();

// Regular performance analysis
Map<String, PerformanceSnapshot> snapshots = monitor.getAllSnapshots();
List<PerformanceAnalyzer.PerformanceInsight> insights = 
    PerformanceAnalyzer.analyzePerformance(snapshots);

// Act on insights
insights.stream()
    .filter(insight -> "SLOW_RULE".equals(insight.getType()))
    .forEach(insight -> {
        logger.warn("Slow rule detected: " + insight.getRuleName());
        // Optimize the rule
    });
```

### 2. Memory Management

**Efficient Fact Preparation**:
```java
// ❌ Inefficient - large object graphs
Map<String, Object> facts = new HashMap<>();
facts.put("customer", largeCustomerObject); // Contains unnecessary data

// ✅ Efficient - minimal fact extraction
Map<String, Object> facts = Map.of(
    "age", customer.getAge(),
    "income", customer.getIncome(),
    "country", customer.getAddress().getCountry().getCode()
);
```

**Monitor Memory Usage**:
```java
// Enable memory tracking
monitor.setTrackMemory(true);

// Analyze memory patterns
snapshots.values().stream()
    .filter(s -> s.getAverageMemoryUsed() > MEMORY_THRESHOLD)
    .forEach(s -> logger.warn("High memory usage: " + s.getRuleName()));
```

### 3. Caching Strategies

**Named Rule Caching**:
```java
// Define frequently used rules once
static {
    Rules.define("adult", "#age >= 18");
    Rules.define("premium", "#balance > 5000");
    Rules.define("us-resident", "#country == 'US'");
}

// Reuse cached rules
boolean isAdult = Rules.test("adult", customer);
boolean isPremium = Rules.test("premium", customer);
```

**Engine Instance Reuse**:
```java
// ❌ Inefficient - creating engines repeatedly
public boolean validate(Customer customer) {
    RulesEngine engine = RuleSet.validation().ageCheck(18).build(); // Expensive
    // ... validation logic
}

// ✅ Efficient - reuse engine instances
@Component
public class ValidationService {
    private final RulesEngine validationEngine = RuleSet.validation()
        .ageCheck(18).emailRequired().build(); // Created once
    
    public boolean validate(Customer customer) {
        // Reuse engine
    }
}
```

---

## 🛡️ **Error Handling Best Practices**

### 1. Graceful Degradation

```java
public class RobustValidationService {
    
    public ValidationResult validateCustomer(Customer customer) {
        try {
            return Rules.validate(customer)
                .minimumAge(18)
                .emailRequired()
                .validate();
        } catch (Exception e) {
            logger.error("Validation failed for customer: " + customer.getId(), e);
            
            // Graceful degradation - basic validation
            return performBasicValidation(customer);
        }
    }
    
    private ValidationResult performBasicValidation(Customer customer) {
        List<String> errors = new ArrayList<>();
        
        if (customer.getAge() < 18) {
            errors.add("Customer must be at least 18 years old");
        }
        
        if (customer.getEmail() == null || customer.getEmail().isEmpty()) {
            errors.add("Email is required");
        }
        
        return new ValidationResult(errors.isEmpty(), errors);
    }
}
```

### 2. Comprehensive Error Context

```java
public class EnhancedValidationService {
    
    public ValidationResult validateWithContext(Customer customer) {
        try {
            return Rules.validate(customer)
                .minimumAge(18)
                .emailRequired()
                .validate();
        } catch (RuleEvaluationException e) {
            logger.error("Rule evaluation failed", e);
            
            // Provide detailed error context
            return ValidationResult.failure(
                "Validation failed: " + e.getDetailedMessage() + 
                ". Suggestions: " + String.join(", ", e.getSuggestions())
            );
        }
    }
}
```

### 3. Monitoring and Alerting

```java
@Component
public class RuleMonitoringService {
    
    private final MeterRegistry meterRegistry;
    
    @EventListener
    public void handleRuleEvaluation(RuleEvaluationEvent event) {
        RuleResult result = event.getResult();
        
        // Track success/failure rates
        meterRegistry.counter("rule.evaluations", 
            "rule", result.getRuleName(),
            "status", result.isTriggered() ? "success" : "failure")
            .increment();
        
        // Track performance metrics
        if (result.hasPerformanceMetrics()) {
            RulePerformanceMetrics metrics = result.getPerformanceMetrics();
            
            meterRegistry.timer("rule.evaluation.time", "rule", metrics.getRuleName())
                .record(metrics.getEvaluationTime());
            
            // Alert on slow rules
            if (metrics.getEvaluationTimeMillis() > 100) {
                alertingService.sendAlert("Slow rule detected: " + metrics.getRuleName());
            }
        }
    }
}
```

---

## 🚫 **Anti-Patterns to Avoid**

### 1. Over-Complex Conditions

**❌ Anti-Pattern**:
```java
// Overly complex single rule
Rules.check("#customer.profile.preferences.notifications.email.enabled == true && " +
           "#customer.profile.preferences.notifications.email.frequency == 'daily' && " +
           "#customer.subscription.plan.features.contains('premium_notifications') && " +
           "#customer.subscription.status == 'active' && " +
           "#customer.subscription.expiryDate.isAfter(T(java.time.LocalDate).now())", 
           customer);
```

**✅ Better Approach**:
```java
// Break into multiple focused rules
boolean emailEnabled = Rules.check("#emailNotificationsEnabled", customer);
boolean dailyFrequency = Rules.check("#notificationFrequency == 'daily'", customer);
boolean premiumFeatures = Rules.check("#hasPremiumFeatures", customer);
boolean activeSubscription = Rules.check("#subscriptionActive", customer);

boolean eligible = emailEnabled && dailyFrequency && premiumFeatures && activeSubscription;
```

### 2. Ignoring Performance Metrics

**❌ Anti-Pattern**:
```java
// Ignoring performance data
RuleResult result = engine.executeRule(rule, facts);
// No performance analysis
```

**✅ Better Approach**:
```java
// Monitor and act on performance data
RuleResult result = engine.executeRule(rule, facts);

if (result.hasPerformanceMetrics()) {
    RulePerformanceMetrics metrics = result.getPerformanceMetrics();
    
    if (metrics.getEvaluationTimeMillis() > PERFORMANCE_THRESHOLD) {
        logger.warn("Slow rule detected: {} took {}ms", 
                   metrics.getRuleName(), 
                   metrics.getEvaluationTimeMillis());
        
        // Consider rule optimization
        optimizationService.scheduleRuleOptimization(metrics.getRuleName());
    }
}
```

### 3. Inconsistent Error Handling

**❌ Anti-Pattern**:
```java
// Inconsistent error handling
try {
    Rules.check(condition1, data);
} catch (Exception e) {
    // Swallow exception
}

try {
    Rules.check(condition2, data);
} catch (Exception e) {
    throw new RuntimeException(e); // Different handling
}
```

**✅ Better Approach**:
```java
// Consistent error handling strategy
public class ConsistentValidationService {
    
    public ValidationResult validate(Object data) {
        try {
            return Rules.validate(data)
                .that(condition1, "Error message 1")
                .that(condition2, "Error message 2")
                .validate();
        } catch (Exception e) {
            logger.error("Validation failed", e);
            return ValidationResult.failure("Validation system error: " + e.getMessage());
        }
    }
}
```

### 4. Creating Engines Repeatedly

**❌ Anti-Pattern**:
```java
// Creating engines in loops or frequently called methods
public boolean validateCustomer(Customer customer) {
    RulesEngine engine = RuleSet.validation().ageCheck(18).build(); // Expensive!
    // ... validation
}
```

**✅ Better Approach**:
```java
// Reuse engine instances
@Component
public class ValidationService {
    private final RulesEngine validationEngine = RuleSet.validation()
        .ageCheck(18).emailRequired().build(); // Created once
    
    public boolean validateCustomer(Customer customer) {
        // Reuse engine
    }
}
```

---

## 📊 **Testing Best Practices**

### 1. Comprehensive Test Coverage

```java
@Test
public class CustomerValidationTest {
    
    @Test
    void shouldPassValidationForValidCustomer() {
        Customer validCustomer = new Customer("John", 25, "john@example.com");
        
        ValidationResult result = Rules.validate(validCustomer)
            .minimumAge(18)
            .emailRequired()
            .validate();
        
        assertTrue(result.isValid());
        assertEquals(0, result.getErrorCount());
    }
    
    @Test
    void shouldFailValidationForInvalidCustomer() {
        Customer invalidCustomer = new Customer("Jane", 16, null);
        
        ValidationResult result = Rules.validate(invalidCustomer)
            .minimumAge(18)
            .emailRequired()
            .validate();
        
        assertFalse(result.isValid());
        assertEquals(2, result.getErrorCount());
        assertTrue(result.getErrors().contains("Must be at least 18 years old"));
        assertTrue(result.getErrors().contains("Email address is required"));
    }
    
    @Test
    void shouldTrackPerformanceMetrics() {
        Customer customer = new Customer("John", 25, "john@example.com");
        
        // Use traditional API to access performance metrics
        RulesEngine engine = new RulesEngine(new RulesEngineConfiguration());
        Rule rule = engine.getConfiguration().rule("test-rule")
            .withCondition("#age >= 18")
            .build();
        
        RuleResult result = engine.executeRule(rule, Map.of("age", 25));
        
        assertTrue(result.hasPerformanceMetrics());
        assertNotNull(result.getPerformanceMetrics());
        assertTrue(result.getPerformanceMetrics().getEvaluationTimeNanos() > 0);
    }
}
```

### 2. Performance Testing

```java
@Test
public class PerformanceTest {
    
    @Test
    void shouldMeetPerformanceRequirements() {
        Customer customer = new Customer("John", 25, "john@example.com");
        
        // Warm up
        for (int i = 0; i < 100; i++) {
            Rules.check("#age >= 18", customer);
        }
        
        // Measure performance
        long startTime = System.nanoTime();
        for (int i = 0; i < 1000; i++) {
            Rules.check("#age >= 18", customer);
        }
        long endTime = System.nanoTime();
        
        double avgTimeMs = (endTime - startTime) / 1_000_000.0 / 1000.0;
        
        // Assert performance requirements
        assertTrue(avgTimeMs < 1.0, "Average evaluation time should be < 1ms, was: " + avgTimeMs);
    }
}
```

By following these best practices and patterns, you can build robust, performant, and maintainable rule-based applications using the SpEL Rules Engine.
