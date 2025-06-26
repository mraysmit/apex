# SpEL Rules Engine - Migration Guide

## Overview

This guide helps you migrate to the latest version of the SpEL Rules Engine and take advantage of the new simplified APIs, performance monitoring, and enhanced error handling features.

## 🔄 **Migration Strategy**

### Zero-Risk Migration Approach

The SpEL Rules Engine follows a **zero-breaking-changes** policy. All existing code will continue to work exactly as before, while new features are available as optional enhancements.

**Migration Philosophy**:
1. **Existing code works unchanged** - No immediate action required
2. **Optional enhancements** - Adopt new features incrementally
3. **Progressive improvement** - Upgrade at your own pace
4. **Backward compatibility** - Always maintained

---

## 📋 **Version Compatibility Matrix**

| Feature | v1.0.x | v1.1.x | v1.2.x | v1.3.x |
|---------|--------|--------|--------|--------|
| **Core Rules Engine** | ✅ | ✅ | ✅ | ✅ |
| **Enhanced Error Handling** | ❌ | ✅ | ✅ | ✅ |
| **Performance Monitoring** | ❌ | ❌ | ✅ | ✅ |
| **Simplified API** | ❌ | ❌ | ❌ | ✅ |
| **Template-Based Rules** | ❌ | ❌ | ❌ | ✅ |

---

## 🚀 **Migration Paths**

### From v1.0.x to Latest

#### Step 1: Update Dependencies
```xml
<!-- Update your pom.xml -->
<dependency>
    <groupId>dev.mars</groupId>
    <artifactId>rules-engine-core</artifactId>
    <version>1.3.0</version>
</dependency>
```

#### Step 2: Verify Existing Code (No Changes Required)
```java
// Your existing code continues to work unchanged
RulesEngineConfiguration config = new RulesEngineConfiguration();
Rule rule = config.rule("existing-rule")
    .withCategory("validation")
    .withName("Existing Rule")
    .withCondition("#age >= 18")
    .withMessage("Age requirement met")
    .build();

config.registerRule(rule);
RulesEngine engine = new RulesEngine(config);
RuleResult result = engine.executeRule(rule, facts);
```

#### Step 3: Optional Enhancements

**Add Performance Monitoring** (Optional):
```java
// Access new performance metrics
if (result.hasPerformanceMetrics()) {
    RulePerformanceMetrics metrics = result.getPerformanceMetrics();
    System.out.println("Evaluation time: " + metrics.getEvaluationTimeMillis() + "ms");
}

// Monitor engine performance
RulePerformanceMonitor monitor = engine.getPerformanceMonitor();
PerformanceSnapshot snapshot = monitor.getRuleSnapshot("existing-rule");
```

**Use Enhanced Error Handling** (Optional):
```java
// Enhanced error handling with detailed context
try {
    RuleResult result = engine.executeRule(rule, facts);
} catch (RuleEvaluationException e) {
    System.out.println("Error code: " + e.getErrorCode());
    System.out.println("Suggestions: " + e.getSuggestions());
    System.out.println("Detailed message: " + e.getDetailedMessage());
}
```

**Adopt Simplified API for New Rules** (Optional):
```java
// For new simple rules, use the simplified API
boolean isAdult = Rules.check("#age >= 18", customer);

// For new validation scenarios
ValidationResult validation = Rules.validate(customer)
    .minimumAge(18)
    .emailRequired()
    .validate();
```

### From v1.1.x to Latest

#### Existing Enhanced Error Handling
Your enhanced error handling code continues to work unchanged:

```java
// Existing error handling code works as-is
try {
    RuleResult result = engine.executeRule(rule, facts);
} catch (RuleEvaluationException e) {
    // All existing error handling continues to work
    handleError(e);
}
```

#### New Optional Features
- Performance monitoring (automatic)
- Simplified API (for new rules)
- Template-based rules (for new scenarios)

### From v1.2.x to Latest

#### Existing Performance Monitoring
Your performance monitoring code continues to work unchanged:

```java
// Existing performance monitoring code works as-is
RulePerformanceMonitor monitor = engine.getPerformanceMonitor();
List<RulePerformanceMetrics> history = monitor.getRuleHistory("rule-name");
```

#### New Optional Features
- Simplified API (for new rules)
- Template-based rules (for new scenarios)

---

## 🎯 **Incremental Adoption Strategies**

### Strategy 1: Start with New Rules

**Approach**: Use simplified API for all new rules while keeping existing rules unchanged.

```java
// Existing rules (keep unchanged)
RulesEngineConfiguration config = new RulesEngineConfiguration();
Rule existingRule = config.rule("existing-rule")
    .withCondition("#existingCondition")
    .build();

// New rules (use simplified API)
Rules.define("new-adult-check", "#age >= 18");
Rules.define("new-premium-check", "#balance > 5000");

// Use both approaches
RuleResult existingResult = engine.executeRule(existingRule, facts);
boolean newResult = Rules.test("new-adult-check", customer);
```

### Strategy 2: Gradual Service Migration

**Approach**: Migrate one service at a time to new APIs.

```java
// Phase 1: Keep existing validation service
@Service
public class LegacyValidationService {
    // Existing implementation using traditional API
}

// Phase 2: Create new validation service with simplified API
@Service
public class ModernValidationService {
    
    public ValidationResult validateCustomer(Customer customer) {
        return Rules.validate(customer)
            .minimumAge(18)
            .emailRequired()
            .phoneRequired()
            .validate();
    }
}

// Phase 3: Gradually switch consumers
@Controller
public class CustomerController {
    
    @Autowired
    private ModernValidationService validationService; // Switch when ready
    
    // Use new service
}
```

### Strategy 3: Template Migration

**Approach**: Replace complex rule configurations with templates.

```java
// Before: Complex configuration
RulesEngineConfiguration config = new RulesEngineConfiguration();
config.registerRule(config.rule("age-check")
    .withCategory("validation")
    .withCondition("#age >= 18")
    .withMessage("Must be adult")
    .build());
config.registerRule(config.rule("email-check")
    .withCategory("validation")
    .withCondition("#email != null")
    .withMessage("Email required")
    .build());
// ... more rules

// After: Template-based (when ready to migrate)
RulesEngine validationEngine = RuleSet.validation()
    .ageCheck(18)
    .emailRequired()
    .phoneRequired()
    .build();
```

---

## 🔧 **Configuration Migration**

### Performance Monitoring Configuration

**Automatic Enablement**:
Performance monitoring is enabled by default with sensible defaults. No configuration required.

**Optional Customization**:
```java
// Customize performance monitoring (optional)
RulePerformanceMonitor monitor = engine.getPerformanceMonitor();
monitor.setMaxHistorySize(1000);
monitor.setTrackMemory(true);
monitor.setTrackComplexity(true);

// Disable if needed (not recommended)
monitor.setEnabled(false);
```

### Error Handling Configuration

**Automatic Enhancement**:
Enhanced error handling is automatically available. No configuration required.

**Optional Customization**:
```java
// Customize error recovery (optional)
ErrorRecoveryService errorService = new ErrorRecoveryService();
RulesEngine engine = new RulesEngine(config, parser, errorService);
```

---

## 📊 **Testing Migration**

### Verify Existing Tests

**All existing tests should pass unchanged**:
```java
// Existing tests continue to work
@Test
public void existingTestShouldStillPass() {
    RulesEngineConfiguration config = new RulesEngineConfiguration();
    Rule rule = config.rule("test-rule")
        .withCondition("#value > 10")
        .build();
    
    RulesEngine engine = new RulesEngine(config);
    RuleResult result = engine.executeRule(rule, Map.of("value", 15));
    
    assertTrue(result.isTriggered()); // Still works
}
```

### Add New Feature Tests

**Test new features incrementally**:
```java
// Test simplified API
@Test
public void testSimplifiedAPI() {
    boolean result = Rules.check("#age >= 18", Map.of("age", 25));
    assertTrue(result);
}

// Test performance monitoring
@Test
public void testPerformanceMonitoring() {
    RulesEngine engine = new RulesEngine(new RulesEngineConfiguration());
    Rule rule = engine.getConfiguration().rule("perf-test")
        .withCondition("#value > 0")
        .build();
    
    RuleResult result = engine.executeRule(rule, Map.of("value", 1));
    
    assertTrue(result.hasPerformanceMetrics());
    assertNotNull(result.getPerformanceMetrics());
}

// Test template-based rules
@Test
public void testTemplateRules() {
    RulesEngine engine = RuleSet.validation()
        .ageCheck(18)
        .emailRequired()
        .build();
    
    assertNotNull(engine);
    assertEquals(2, engine.getConfiguration().getAllRules().size());
}
```

---

## 🚨 **Common Migration Issues and Solutions**

### Issue 1: Performance Overhead Concerns

**Concern**: "Will performance monitoring slow down my application?"

**Solution**: Performance monitoring has < 1% overhead and can be disabled if needed:
```java
// Disable performance monitoring if absolutely necessary
engine.getPerformanceMonitor().setEnabled(false);

// Or customize tracking
monitor.setTrackMemory(false); // Disable memory tracking only
monitor.setTrackComplexity(false); // Disable complexity tracking only
```

### Issue 2: Memory Usage Concerns

**Concern**: "Will performance history consume too much memory?"

**Solution**: Configure history retention:
```java
// Limit history size
monitor.setMaxHistorySize(100); // Keep only last 100 evaluations per rule

// Clear history periodically
monitor.clearMetrics(); // Clear all accumulated metrics
```

### Issue 3: API Confusion

**Concern**: "Which API should I use for different scenarios?"

**Solution**: Follow the layered approach:
```java
// Layer 1: Simple one-off evaluations
boolean simple = Rules.check("#age >= 18", data);

// Layer 2: Structured rule sets
RulesEngine structured = RuleSet.validation().ageCheck(18).build();

// Layer 3: Complex enterprise scenarios
RulesEngineConfiguration complex = new RulesEngineConfiguration();
// ... complex configuration
```

### Issue 4: Integration with Existing Monitoring

**Concern**: "How do I integrate with my existing monitoring system?"

**Solution**: Export metrics to your monitoring system:
```java
// Export to existing monitoring
@Component
public class MetricsExporter {
    
    @Autowired
    private MeterRegistry meterRegistry;
    
    @EventListener
    public void exportRuleMetrics(RuleEvaluationEvent event) {
        if (event.getResult().hasPerformanceMetrics()) {
            RulePerformanceMetrics metrics = event.getResult().getPerformanceMetrics();
            
            meterRegistry.timer("rule.evaluation.time", 
                "rule", metrics.getRuleName())
                .record(metrics.getEvaluationTime());
        }
    }
}
```

---

## 📈 **Migration Success Metrics**

### Track Migration Progress

**Code Simplification**:
- Measure lines of code reduction in new rules
- Track adoption of simplified APIs
- Monitor developer productivity improvements

**Performance Improvements**:
- Baseline current performance
- Monitor performance trends with new monitoring
- Identify and optimize slow rules

**Error Reduction**:
- Track error rates before and after migration
- Monitor error recovery success rates
- Measure debugging time improvements

### Example Metrics Dashboard

```java
// Migration metrics tracking
public class MigrationMetrics {
    
    public void trackAPIUsage() {
        // Track which APIs are being used
        meterRegistry.counter("api.usage", "layer", "simple").increment();
        meterRegistry.counter("api.usage", "layer", "template").increment();
        meterRegistry.counter("api.usage", "layer", "advanced").increment();
    }
    
    public void trackPerformanceImprovements() {
        // Track performance improvements
        double avgTime = performanceMonitor.getAverageEvaluationTimeMillis();
        meterRegistry.gauge("rules.performance.average_time", avgTime);
    }
    
    public void trackErrorReduction() {
        // Track error rates
        long totalEvaluations = performanceMonitor.getTotalEvaluations();
        long errorCount = getErrorCount();
        double errorRate = (double) errorCount / totalEvaluations;
        meterRegistry.gauge("rules.error.rate", errorRate);
    }
}
```

---

## 🎯 **Next Steps After Migration**

1. **Monitor Performance**: Use the new performance monitoring to identify optimization opportunities
2. **Simplify New Rules**: Use simplified APIs for all new rule development
3. **Gradual Refactoring**: Consider migrating complex existing rules to templates when convenient
4. **Team Training**: Train team members on new APIs and best practices
5. **Documentation Updates**: Update internal documentation to reflect new capabilities

The migration to the latest SpEL Rules Engine version is designed to be seamless and risk-free, allowing you to take advantage of powerful new features while maintaining full compatibility with existing code.
