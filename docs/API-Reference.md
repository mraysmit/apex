# SpEL Rules Engine - API Reference

## Overview

This document provides a comprehensive reference for all APIs available in the SpEL Rules Engine, organized by complexity level and use case.

## API Layers

### Layer 1: Ultra-Simple API (`Rules` class)

The `Rules` class provides static methods for immediate rule evaluation without configuration.

#### One-Liner Evaluation

```java
public static boolean check(String condition, Map<String, Object> facts)
public static boolean check(String condition, Object data)
```

**Purpose**: Evaluate a rule condition in a single line of code.

**Examples**:
```java
boolean result = Rules.check("#age >= 18", Map.of("age", 25));
boolean valid = Rules.check("#data.balance > 1000", customer);
```

#### Named Rules

```java
public static void define(String name, String condition)
public static void define(String name, String condition, String message)
public static boolean test(String ruleName, Map<String, Object> facts)
public static boolean test(String ruleName, Object data)
```

**Purpose**: Define reusable rules with names for better organization.

**Examples**:
```java
Rules.define("adult", "#age >= 18", "Customer is an adult");
boolean isAdult = Rules.test("adult", customer);
```

#### Fluent Validation

```java
public static ValidationBuilder validate(Object data)
public static ValidationBuilder validate(Map<String, Object> facts)
```

**Purpose**: Start a fluent validation chain for readable multi-condition validation.

**Examples**:
```java
boolean valid = Rules.validate(customer)
    .minimumAge(18)
    .emailRequired()
    .passes();
```

#### Utility Methods

```java
public static RulesEngine getEngine()
public static void clearNamedRules()
public static String[] getDefinedRules()
public static boolean isDefined(String ruleName)
```

---

### ValidationBuilder Class

Provides fluent validation with built-in helpers and detailed error reporting.

#### Core Validation Methods

```java
public ValidationBuilder that(String condition, String errorMessage)
public ValidationBuilder that(String condition)
```

**Purpose**: Add custom validation conditions.

#### Built-in Validation Helpers

```java
public ValidationBuilder minimumAge(int minimumAge)
public ValidationBuilder emailRequired()
public ValidationBuilder phoneRequired()
public ValidationBuilder minimumBalance(double minimumBalance)
public ValidationBuilder notNull(String fieldName)
public ValidationBuilder notEmpty(String fieldName)
```

**Purpose**: Common validation patterns with intelligent error messages.

#### Result Methods

```java
public boolean passes()
public boolean fails()
public ValidationResult validate()
public String getFirstError()
public List<String> getErrors()
public int getRuleCount()
```

**Examples**:
```java
ValidationResult result = Rules.validate(customer)
    .minimumAge(18)
    .emailRequired()
    .that("#data.balance >= 0", "Balance cannot be negative")
    .validate();

if (!result.isValid()) {
    result.getErrors().forEach(System.out::println);
}
```

---

### ValidationResult Class

Comprehensive validation results with detailed error information.

#### Status Methods

```java
public boolean isValid()
public boolean isInvalid()
public boolean hasErrors()
public int getErrorCount()
```

#### Error Access Methods

```java
public List<String> getErrors()
public String getFirstError()
public String getErrorsAsString()
public String getErrorsAsString(String separator)
```

#### Utility Methods

```java
public ValidationResult combine(ValidationResult other)
public ValidationResult addError(String error)
public String getSummary()
public void throwIfInvalid() throws ValidationException
public void throwIfInvalid(String message) throws ValidationException
```

#### Static Factory Methods

```java
public static ValidationResult success()
public static ValidationResult failure(String error)
public static ValidationResult failure(List<String> errors)
```

---

### Layer 2: Template-Based API (`RuleSet` class)

The `RuleSet` class provides template-based rule creation for structured scenarios.

#### Rule Set Types

```java
public static ValidationRuleSet validation()
public static BusinessRuleSet business()
public static EligibilityRuleSet eligibility()
public static FinancialRuleSet financial()
```

#### ValidationRuleSet

```java
public ValidationRuleSet ageCheck(int minimumAge)
public ValidationRuleSet emailRequired()
public ValidationRuleSet phoneRequired()
public ValidationRuleSet fieldRequired(String fieldName)
public ValidationRuleSet stringLength(String fieldName, int minLength, int maxLength)
public ValidationRuleSet customRule(String name, String condition, String message)
public RulesEngine build()
```

**Example**:
```java
RulesEngine validation = RuleSet.validation()
    .ageCheck(18)
    .emailRequired()
    .phoneRequired()
    .fieldRequired("firstName")
    .stringLength("lastName", 2, 50)
    .build();
```

#### BusinessRuleSet

```java
public BusinessRuleSet premiumEligibility(String condition)
public BusinessRuleSet discountEligibility(String condition)
public BusinessRuleSet vipStatus(String condition)
public BusinessRuleSet customRule(String name, String condition, String message)
public RulesEngine build()
```

#### EligibilityRuleSet

```java
public EligibilityRuleSet minimumAge(int minimumAge)
public EligibilityRuleSet minimumIncome(double minimumIncome)
public EligibilityRuleSet creditScoreCheck(int minimumScore)
public EligibilityRuleSet customRule(String name, String condition, String message)
public RulesEngine build()
```

#### FinancialRuleSet

```java
public FinancialRuleSet minimumBalance(double minimumBalance)
public FinancialRuleSet transactionLimit(double maxAmount)
public FinancialRuleSet kycRequired()
public FinancialRuleSet customRule(String name, String condition, String message)
public RulesEngine build()
```

#### Base Methods (Available on All Rule Sets)

```java
public List<Rule> getRules()
public int getRuleCount()
public BaseRuleSet customRule(String name, String condition, String message)
public RulesEngine build()
```

---

### Layer 3: Advanced Configuration API

The traditional API for complex scenarios requiring full control.

#### RulesEngineConfiguration

```java
public RuleBuilder rule(String id)
public RulesEngineConfiguration registerRule(Rule rule)
public RulesEngineConfiguration registerRuleGroup(RuleGroup ruleGroup)
public Rule getRuleById(String id)
public List<Rule> getRulesByCategory(String category)
public List<Rule> getAllRules()
public RuleGroup getRuleGroupById(String id)
public List<RuleGroup> getAllRuleGroups()
```

#### RuleBuilder

```java
public RuleBuilder withCategory(String category)
public RuleBuilder withName(String name)
public RuleBuilder withDescription(String description)
public RuleBuilder withCondition(String condition)
public RuleBuilder withMessage(String message)
public RuleBuilder withPriority(int priority)
public Rule build()
```

#### RulesEngine

```java
public RulesEngine(RulesEngineConfiguration configuration)
public RulesEngine(RulesEngineConfiguration configuration, ExpressionParser parser)
public RulesEngine(RulesEngineConfiguration configuration, ExpressionParser parser, ErrorRecoveryService errorRecoveryService)
public RulesEngine(RulesEngineConfiguration configuration, ExpressionParser parser, ErrorRecoveryService errorRecoveryService, RulePerformanceMonitor performanceMonitor)

public RuleResult executeRule(Rule rule, Map<String, Object> facts)
public List<RuleResult> executeRules(List<Rule> rules, Map<String, Object> facts)
public List<RuleResult> executeRulesByCategory(String category, Map<String, Object> facts)
public RuleResult executeRuleGroup(RuleGroup ruleGroup, Map<String, Object> facts)

public RulesEngineConfiguration getConfiguration()
public RulePerformanceMonitor getPerformanceMonitor()
```

---

## Performance Monitoring API

### RulePerformanceMetrics

```java
public String getRuleName()
public Instant getStartTime()
public Instant getEndTime()
public Duration getEvaluationTime()
public long getEvaluationTimeMillis()
public long getEvaluationTimeNanos()
public long getMemoryUsedBytes()
public long getMemoryBeforeBytes()
public long getMemoryAfterBytes()
public int getExpressionComplexity()
public boolean isCacheHit()
public String getEvaluationPhase()
public Exception getEvaluationException()
public boolean hasException()
public String getPerformanceSummary()
```

### RulePerformanceMonitor

```java
public RulePerformanceMetrics.Builder startEvaluation(String ruleName)
public RulePerformanceMetrics.Builder startEvaluation(String ruleName, String phase)
public RulePerformanceMetrics completeEvaluation(RulePerformanceMetrics.Builder builder, String ruleCondition)
public RulePerformanceMetrics completeEvaluation(RulePerformanceMetrics.Builder builder, String ruleCondition, Exception exception)

public List<RulePerformanceMetrics> getRuleHistory(String ruleName)
public PerformanceSnapshot getRuleSnapshot(String ruleName)
public Map<String, PerformanceSnapshot> getAllSnapshots()

public long getTotalEvaluations()
public long getTotalEvaluationTimeNanos()
public double getAverageEvaluationTimeMillis()

public void clearMetrics()
public void setEnabled(boolean enabled)
public boolean isEnabled()
public void setMaxHistorySize(int maxHistorySize)
public void setTrackMemory(boolean trackMemory)
public void setTrackComplexity(boolean trackComplexity)
```

### PerformanceSnapshot

```java
public String getRuleName()
public long getEvaluationCount()
public Duration getTotalEvaluationTime()
public Duration getAverageEvaluationTime()
public Duration getMinEvaluationTime()
public Duration getMaxEvaluationTime()
public long getTotalMemoryUsed()
public long getAverageMemoryUsed()
public long getMinMemoryUsed()
public long getMaxMemoryUsed()
public double getAverageComplexity()
public int getMinComplexity()
public int getMaxComplexity()
public long getSuccessfulEvaluations()
public long getFailedEvaluations()
public double getSuccessRate()
public double getFailureRate()
public Instant getFirstEvaluation()
public Instant getLastEvaluation()
public Instant getLastUpdated()

public double getAverageEvaluationTimeMillis()
public double getMinEvaluationTimeMillis()
public double getMaxEvaluationTimeMillis()
public String getPerformanceSummary()
public String getDetailedReport()
```

### PerformanceAnalyzer

```java
public static List<PerformanceInsight> analyzePerformance(Map<String, PerformanceSnapshot> snapshots)
public static List<String> generateRecommendations(List<PerformanceInsight> insights)
public static String generatePerformanceReport(Map<String, PerformanceSnapshot> snapshots)
```

---

## Error Handling API

### Enhanced Exceptions

```java
// RuleEvaluationException
public String getErrorCode()
public ErrorContext getErrorContext()
public List<String> getSuggestions()
public String getDetailedMessage()

// RuleConfigurationException
public String getConfigurationIssue()
public List<String> getValidationErrors()

// RuleValidationException
public List<ValidationError> getValidationErrors()
public boolean hasErrors()
```

### ErrorRecoveryService

```java
public RecoveryResult attemptRecovery(String ruleName, String condition, StandardEvaluationContext context, Exception exception)

// RecoveryResult
public boolean isSuccessful()
public RuleResult getRuleResult()
public String getRecoveryMessage()
public RecoveryStrategy getStrategy()
```

---

## Usage Patterns

### Quick Start Pattern
```java
// Immediate evaluation
boolean result = Rules.check("#age >= 18", customer);
```

### Validation Pattern
```java
ValidationResult result = Rules.validate(customer)
    .minimumAge(18)
    .emailRequired()
    .validate();
```

### Template Pattern
```java
RulesEngine engine = RuleSet.validation()
    .ageCheck(18)
    .emailRequired()
    .build();
```

### Advanced Pattern
```java
RulesEngineConfiguration config = new RulesEngineConfiguration();
Rule rule = config.rule("complex-rule")
    .withCondition("complex expression")
    .build();
RulesEngine engine = new RulesEngine(config);
```

### Performance Monitoring Pattern
```java
RuleResult result = engine.executeRule(rule, facts);
if (result.hasPerformanceMetrics()) {
    RulePerformanceMetrics metrics = result.getPerformanceMetrics();
    // Analyze performance
}
```

For complete examples and detailed usage guides, see the [Simplified API Quick Start](Simplified-API-Quick-Start.md) and other documentation files.
