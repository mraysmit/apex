# Enhanced Error Handling Guide

## Overview

The rules engine now includes comprehensive error handling capabilities that provide detailed error context, helpful suggestions, and automatic error recovery mechanisms. This guide explains how to use these features effectively.

## Key Features

### 1. Custom Exception Hierarchy

The rules engine now uses a structured exception hierarchy:

- `RuleEngineException` - Base exception for all rules engine errors
- `RuleEvaluationException` - Specific to rule evaluation failures
- `RuleConfigurationException` - For configuration-related issues
- `RuleValidationException` - For rule validation problems

### 2. Detailed Error Context

When a rule evaluation fails, the system provides:

- **Error Classification** - Categorizes the type of error (property access, method invocation, etc.)
- **Available Variables** - Shows what variables were available in the evaluation context
- **Expression Analysis** - Identifies potential syntax issues in the rule expression
- **Helpful Suggestions** - Provides specific recommendations for fixing the error

### 3. Error Recovery Strategies

The system supports multiple error recovery strategies:

- **CONTINUE_WITH_DEFAULT** - Continue execution with a default result
- **RETRY_WITH_SAFE_EXPRESSION** - Attempt to create a safer version of the expression
- **SKIP_RULE** - Skip the problematic rule and continue
- **FAIL_FAST** - Stop execution immediately

## Usage Examples

### Basic Error Handling

```java
RulesEngineConfiguration config = new RulesEngineConfiguration();
ErrorRecoveryService errorRecovery = new ErrorRecoveryService();
RulesEngine engine = new RulesEngine(config, new SpelExpressionParser(), errorRecovery);

// Create a rule with potential issues
Rule rule = config.rule("test-rule")
    .category("validation")
    .name("Customer Validation")
    .condition("#customer.invalidProperty == 'test'")  // This property doesn't exist
    .message("Customer is valid")
    .build();

Map<String, Object> facts = new HashMap<>();
facts.put("customer", new Customer("John", 30));

// Execute rule - error will be handled gracefully
RuleResult result = engine.executeRule(rule, facts);
```

### Custom Error Recovery Strategy

```java
// Create error recovery service with custom strategy
ErrorRecoveryService errorRecovery = new ErrorRecoveryService(
    ErrorRecoveryService.ErrorRecoveryStrategy.RETRY_WITH_SAFE_EXPRESSION
);

RulesEngine engine = new RulesEngine(config, new SpelExpressionParser(), errorRecovery);
```

### Detailed Error Analysis

```java
ErrorContextService errorContextService = new ErrorContextService();

try {
    // Rule evaluation that might fail
    RuleResult result = engine.executeRule(rule, facts);
} catch (Exception e) {
    // Generate detailed error context
    ErrorContextService.ErrorContext context = errorContextService.generateErrorContext(
        rule.getName(), 
        rule.getCondition(), 
        evaluationContext, 
        e
    );
    
    // Get formatted error report
    String report = context.getFormattedErrorReport();
    System.out.println(report);
    
    // Get specific suggestions
    List<String> suggestions = context.getSuggestions();
    suggestions.forEach(System.out::println);
}
```

## Error Types and Common Solutions

### Property Access Errors

**Error**: `Property 'name' cannot be found on object`

**Common Causes**:
- Typo in property name
- Property doesn't exist on the object
- Property is private without getter

**Solutions**:
- Verify property name spelling
- Check object structure
- Use safe navigation operator: `#customer?.name`
- Ensure getter methods are public

### Method Invocation Errors

**Error**: `Method 'getName()' cannot be found`

**Common Causes**:
- Method doesn't exist
- Wrong parameter types
- Method is private

**Solutions**:
- Check method name and parameters
- Ensure method is public
- Verify parameter types match

### Type Conversion Errors

**Error**: `Cannot cast String to Integer`

**Common Causes**:
- Unexpected data types
- Missing type conversion

**Solutions**:
- Use explicit type conversion: `T(Integer).valueOf(#value)`
- Validate input data types
- Add type checks in expressions

### Null Pointer Errors

**Error**: `null pointer exception`

**Common Causes**:
- Accessing properties on null objects
- Missing null checks

**Solutions**:
- Use safe navigation: `#customer?.name`
- Add null checks: `#customer != null && #customer.name == 'John'`
- Provide default values: `#customer?.name ?: 'Unknown'`

## Best Practices

### 1. Use Safe Navigation

```java
// Instead of: #customer.address.street
// Use: #customer?.address?.street
```

### 2. Add Null Checks

```java
// Instead of: #customer.name == 'John'
// Use: #customer != null && #customer.name == 'John'
```

### 3. Validate Input Data

```java
// Check data types before evaluation
if (facts.get("customer") instanceof Customer) {
    RuleResult result = engine.executeRule(rule, facts);
}
```

### 4. Choose Appropriate Recovery Strategy

- Use `CONTINUE_WITH_DEFAULT` for non-critical rules
- Use `RETRY_WITH_SAFE_EXPRESSION` for rules that can be made safer
- Use `SKIP_RULE` when rule failure shouldn't stop processing
- Use `FAIL_FAST` for critical validation rules

### 5. Monitor Error Patterns

```java
// Log error patterns for analysis
ErrorContextService.ErrorContext context = errorContextService.generateErrorContext(...);
logger.warn("Rule error pattern: {} - {}", 
    context.getErrorType(), 
    context.getExpression());
```

## Configuration

### Default Error Recovery

```java
// Use default recovery strategy (CONTINUE_WITH_DEFAULT)
RulesEngine engine = new RulesEngine(configuration);
```

### Custom Error Recovery

```java
// Configure specific recovery strategy
ErrorRecoveryService errorRecovery = new ErrorRecoveryService(
    ErrorRecoveryService.ErrorRecoveryStrategy.RETRY_WITH_SAFE_EXPRESSION
);
RulesEngine engine = new RulesEngine(configuration, parser, errorRecovery);
```

### Error Context Service

```java
// Create error context service for detailed analysis
ErrorContextService errorContextService = new ErrorContextService();

// Use in exception handling
ErrorContextService.ErrorContext context = errorContextService.generateErrorContext(
    ruleName, expression, evaluationContext, exception
);
```

## Testing Error Handling

```java
@Test
public void testErrorRecovery() {
    // Create rule with intentional error
    Rule rule = config.rule("error-test")
        .condition("#nonExistent.property")
        .build();
    
    // Execute and verify recovery
    RuleResult result = engine.executeRule(rule, facts);
    
    // Verify error was handled gracefully
    assertNotNull(result);
    // Result type depends on recovery strategy
}
```

## Troubleshooting

### Common Issues

1. **Recovery not working**: Check that ErrorRecoveryService is properly configured
2. **Missing error context**: Ensure ErrorContextService is being used
3. **Unclear error messages**: Use the detailed error report for more information

### Debug Mode

Enable detailed logging to see error recovery in action:

```java
Logger.getLogger("dev.mars.rulesengine.core.service.error").setLevel(Level.FINE);
```

This will show:
- Error recovery attempts
- Safe expression generation
- Recovery strategy decisions
- Detailed error analysis
