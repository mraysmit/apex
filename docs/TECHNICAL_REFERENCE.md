# SpEL Rules Engine - Technical Reference Guide

## Architecture Overview

### Core Components

The SpEL Rules Engine is built with a layered architecture that provides flexibility while maintaining performance:

```
┌─────────────────────────────────────────────────────────────┐
│                    API Layer (3 Levels)                    │
├─────────────────────────────────────────────────────────────┤
│  Ultra-Simple API  │  Template-Based API  │  Advanced API  │
├─────────────────────────────────────────────────────────────┤
│                   Core Rules Engine                        │
├─────────────────────────────────────────────────────────────┤
│  Performance Monitor │  Error Recovery  │  Configuration   │
├─────────────────────────────────────────────────────────────┤
│              Spring Expression Language (SpEL)             │
└─────────────────────────────────────────────────────────────┘
```

### Key Classes and Interfaces

#### Core Engine Classes
- `RulesEngine` - Main rule execution engine
- `RulesEngineConfiguration` - Configuration management
- `Rule` - Individual rule representation
- `RuleGroup` - Collection of related rules
- `RuleResult` - Execution result with metadata

#### API Layer Classes
- `Rules` - Static utility class for ultra-simple API
- `RuleSet` - Template-based rule creation
- `ValidationBuilder` - Fluent validation interface
- `RulesService` - Instance-based service for dependency injection

#### Performance Monitoring
- `RulePerformanceMonitor` - Central monitoring service
- `RulePerformanceMetrics` - Individual rule metrics
- `PerformanceSnapshot` - Aggregated performance data
- `PerformanceAnalyzer` - Analysis and insights generation

#### Error Handling
- `ErrorRecoveryService` - Error recovery management
- `RuleEngineException` - Base exception hierarchy
- `ErrorRecoveryStrategy` - Recovery strategy enumeration

## Implementation Patterns

### 1. Ultra-Simple API Pattern

```java
// Static utility methods for quick evaluations
public class Rules {
    public static boolean check(String expression, Object data) {
        return RulesEngine.getDefault().evaluate(expression, data).isSuccess();
    }
    
    public static <T> T calculate(String expression, Object data, Class<T> returnType) {
        return RulesEngine.getDefault().calculate(expression, data, returnType);
    }
}
```

### 2. Template-Based API Pattern

```java
// Fluent builder for common validation scenarios
public class RuleSet {
    public static ValidationBuilder validation() {
        return new ValidationBuilder();
    }
    
    public static class ValidationBuilder {
        public ValidationBuilder ageCheck(int minimumAge) {
            return addRule("#data.age >= " + minimumAge);
        }
        
        public ValidationBuilder emailRequired() {
            return addRule("#data.email != null && #data.email.contains('@')");
        }
        
        public RulesEngine build() {
            return new RulesEngine(configuration);
        }
    }
}
```

### 3. Advanced Configuration Pattern

```java
// Full configuration-based approach
RulesEngineConfiguration config = new RulesEngineConfiguration.Builder()
    .withPerformanceMonitoring(true)
    .withErrorRecovery(ErrorRecoveryStrategy.CONTINUE_ON_ERROR)
    .withCaching(true)
    .addRule(rule1)
    .addRule(rule2)
    .addEnrichment(enrichment1)
    .build();

RulesEngine engine = new RulesEngine(config);
```

## Performance Monitoring

### Automatic Metrics Collection

The engine automatically collects comprehensive performance metrics with < 1% overhead:

```java
// Enable performance monitoring
RulesEngineConfiguration config = new RulesEngineConfiguration.Builder()
    .withPerformanceMonitoring(true)
    .withMetricsCollectionInterval(Duration.ofSeconds(30))
    .build();

// Access performance data
PerformanceSnapshot snapshot = engine.getPerformanceMonitor().getSnapshot();
System.out.println("Average execution time: " + snapshot.getAverageExecutionTime());
System.out.println("Memory usage: " + snapshot.getMemoryUsage());
```

### Performance Metrics

#### Rule-Level Metrics
- Execution time (min, max, average, percentiles)
- Success/failure rates
- Memory usage per rule
- Cache hit/miss ratios

#### Engine-Level Metrics
- Total throughput (rules/second)
- Concurrent execution statistics
- Resource utilization
- Error rates and patterns

### Performance Analysis

```java
// Get detailed performance analysis
PerformanceAnalyzer analyzer = engine.getPerformanceAnalyzer();
List<PerformanceInsight> insights = analyzer.analyzePerformance();

for (PerformanceInsight insight : insights) {
    System.out.println("Issue: " + insight.getIssue());
    System.out.println("Recommendation: " + insight.getRecommendation());
    System.out.println("Impact: " + insight.getImpact());
}
```

## Rule Metadata System

### Core Audit Dates

The **two most critical attributes** for any enterprise rule are:
1. **`createdDate`** - When the rule was first created (NEVER null)
2. **`modifiedDate`** - When the rule was last modified (NEVER null)

```java
// These methods NEVER return null
Instant created = rule.getCreatedDate();     // ALWAYS available
Instant modified = rule.getModifiedDate();   // ALWAYS available

// Direct access from metadata
Instant created2 = rule.getMetadata().getCreatedDate();   // ALWAYS available
Instant modified2 = rule.getMetadata().getModifiedDate(); // ALWAYS available
```

### Comprehensive Metadata

```java
// Creating rules with full metadata
Rule rule = configuration.rule("TRADE-VAL-001")
    .withName("Trade Amount Validation")
    .withCondition("#amount > 0 && #amount <= 1000000")
    .withMessage("Trade amount is valid")
    .withMetadata(metadata -> metadata
        .withOwner("Trading Team")
        .withDomain("Finance")
        .withPurpose("Regulatory compliance")
        .withVersion("1.2.0")
        .withTags("trading", "validation", "regulatory")
        .withEffectiveDate(LocalDate.of(2024, 1, 1))
        .withExpirationDate(LocalDate.of(2024, 12, 31))
        .withCustomProperty("regulatoryReference", "MiFID-II-2024")
        .withCustomProperty("businessOwner", "john.doe@company.com")
    )
    .build();
```

### Metadata Queries

```java
// Query rules by metadata
List<Rule> tradingRules = engine.getRules().stream()
    .filter(rule -> rule.getMetadata().getDomain().equals("Finance"))
    .filter(rule -> rule.getMetadata().getTags().contains("trading"))
    .collect(Collectors.toList());

// Find rules modified in the last 30 days
Instant thirtyDaysAgo = Instant.now().minus(30, ChronoUnit.DAYS);
List<Rule> recentlyModified = engine.getRules().stream()
    .filter(rule -> rule.getModifiedDate().isAfter(thirtyDaysAgo))
    .collect(Collectors.toList());
```

## Configuration Examples

### Basic Rule Configuration

```yaml
metadata:
  name: "Customer Validation Rules"
  version: "1.0.0"
  description: "Basic customer validation"

rules:
  - id: "age-validation"
    name: "Age Check"
    condition: "#data.age >= 18"
    message: "Customer must be at least 18 years old"
    severity: "ERROR"
    metadata:
      owner: "Customer Team"
      domain: "Customer Management"
      tags: ["validation", "age"]
```

### Advanced Enrichment Configuration

```yaml
enrichments:
  - id: "comprehensive-currency-enrichment"
    type: "lookup-enrichment"
    condition: "['currency'] != null"
    enabled: true
    lookup-config:
      lookup-dataset:
        type: "yaml-file"
        file-path: "datasets/currencies.yaml"
        key-field: "code"
        cache-enabled: true
        cache-ttl-seconds: 3600
        preload-enabled: true
        default-values:
          region: "Unknown"
          isActive: false
          decimalPlaces: 2
    field-mappings:
      - source-field: "name"
        target-field: "currencyName"
      - source-field: "region"
        target-field: "currencyRegion"
      - source-field: "isActive"
        target-field: "currencyActive"
      - source-field: "decimalPlaces"
        target-field: "currencyDecimals"
    metadata:
      owner: "Finance Team"
      purpose: "Currency standardization"
      lastUpdated: "2024-07-26"
```

### Multi-Environment Configuration

```yaml
# config-dev.yaml
metadata:
  name: "Development Configuration"
  environment: "development"

enrichments:
  - id: "test-data-enrichment"
    lookup-config:
      lookup-dataset:
        type: "inline"
        data:
          - code: "TEST"
            name: "Test Data"

# config-prod.yaml  
metadata:
  name: "Production Configuration"
  environment: "production"

enrichments:
  - id: "production-data-enrichment"
    lookup-config:
      lookup-dataset:
        type: "yaml-file"
        file-path: "datasets/production-data.yaml"
```

### Performance-Optimized Configuration

```yaml
enrichments:
  - id: "high-performance-lookup"
    type: "lookup-enrichment"
    lookup-config:
      lookup-dataset:
        type: "yaml-file"
        file-path: "datasets/large-dataset.yaml"
        key-field: "id"
        # Performance optimizations
        cache-enabled: true
        cache-ttl-seconds: 7200
        preload-enabled: true
        cache-max-size: 10000
        # Monitoring
        performance-monitoring: true
        log-cache-stats: true
```

## Integration Patterns

### Spring Boot Integration

```java
@Configuration
@EnableRulesEngine
public class RulesEngineConfig {
    
    @Bean
    @Primary
    public RulesEngine primaryRulesEngine() {
        return YamlConfigurationLoader.load("classpath:rules/primary-rules.yaml")
            .createEngine();
    }
    
    @Bean("validationEngine")
    public RulesEngine validationRulesEngine() {
        return YamlConfigurationLoader.load("classpath:rules/validation-rules.yaml")
            .createEngine();
    }
}

@Service
public class BusinessService {
    
    @Autowired
    private RulesEngine rulesEngine;
    
    @Autowired
    @Qualifier("validationEngine")
    private RulesEngine validationEngine;
    
    public void processData(Object data) {
        RuleResult validationResult = validationEngine.evaluate(data);
        if (validationResult.isSuccess()) {
            RuleResult businessResult = rulesEngine.evaluate(data);
            // Process results
        }
    }
}
```

### Microservices Integration

```java
@RestController
@RequestMapping("/api/rules")
public class RulesController {
    
    @Autowired
    private RulesEngine rulesEngine;
    
    @PostMapping("/evaluate")
    public ResponseEntity<RuleResult> evaluateRules(@RequestBody Map<String, Object> data) {
        try {
            RuleResult result = rulesEngine.evaluate(data);
            return ResponseEntity.ok(result);
        } catch (RuleEngineException e) {
            return ResponseEntity.badRequest()
                .body(RuleResult.error("Rule evaluation failed: " + e.getMessage()));
        }
    }
    
    @GetMapping("/performance")
    public ResponseEntity<PerformanceSnapshot> getPerformanceMetrics() {
        PerformanceSnapshot snapshot = rulesEngine.getPerformanceMonitor().getSnapshot();
        return ResponseEntity.ok(snapshot);
    }
}
```

### Batch Processing Integration

```java
@Component
public class BatchRulesProcessor {
    
    @Autowired
    private RulesEngine rulesEngine;
    
    @EventListener
    public void processBatch(BatchProcessingEvent event) {
        List<Object> dataItems = event.getDataItems();
        
        // Parallel processing with rules engine
        List<RuleResult> results = dataItems.parallelStream()
            .map(rulesEngine::evaluate)
            .collect(Collectors.toList());
            
        // Process results
        results.forEach(this::handleResult);
    }
    
    private void handleResult(RuleResult result) {
        if (!result.isSuccess()) {
            // Handle validation failures
            log.warn("Rule validation failed: {}", result.getFailureReasons());
        }
        
        // Process enriched data
        Map<String, Object> enrichedData = result.getEnrichedData();
        // Continue processing...
    }
}
```

## Error Handling and Recovery

### Error Recovery Strategies

```java
// Configure error recovery behavior
RulesEngineConfiguration config = new RulesEngineConfiguration.Builder()
    .withErrorRecovery(ErrorRecoveryStrategy.CONTINUE_ON_ERROR)
    .withMaxRetries(3)
    .withRetryDelay(Duration.ofMillis(100))
    .build();
```

### Custom Error Handling

```java
public class CustomErrorHandler implements RuleErrorHandler {
    
    @Override
    public void handleRuleError(Rule rule, Exception error, Object data) {
        log.error("Rule {} failed for data {}: {}", 
            rule.getId(), data, error.getMessage());
            
        // Custom error handling logic
        if (error instanceof ValidationException) {
            // Handle validation errors
        } else if (error instanceof EnrichmentException) {
            // Handle enrichment errors
        }
    }
    
    @Override
    public boolean shouldContinueOnError(Rule rule, Exception error) {
        // Decide whether to continue processing other rules
        return !(error instanceof CriticalValidationException);
    }
}
```

## Testing Strategies

### Unit Testing Rules

```java
@Test
public void testAgeValidationRule() {
    // Arrange
    Map<String, Object> data = Map.of("age", 25);
    
    // Act
    RuleResult result = rulesEngine.evaluate(data);
    
    // Assert
    assertTrue(result.isSuccess());
    assertFalse(result.hasFailures());
}

@Test
public void testEnrichmentFunctionality() {
    // Arrange
    Map<String, Object> data = Map.of("currency", "USD");
    
    // Act
    RuleResult result = rulesEngine.evaluate(data);
    
    // Assert
    assertEquals("US Dollar", result.getEnrichedData().get("currencyName"));
    assertEquals("North America", result.getEnrichedData().get("currencyRegion"));
}
```

### Integration Testing

```java
@SpringBootTest
@TestPropertySource(properties = {
    "rules.config.path=classpath:test-rules.yaml"
})
public class RulesEngineIntegrationTest {
    
    @Autowired
    private RulesEngine rulesEngine;
    
    @Test
    public void testFullWorkflow() {
        // Test complete rule evaluation workflow
        Map<String, Object> testData = createTestData();
        RuleResult result = rulesEngine.evaluate(testData);
        
        // Verify results
        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertFalse(result.getEnrichedData().isEmpty());
    }
}
```

## Performance Optimization

### Caching Strategies

```yaml
# Optimal caching configuration
enrichments:
  - id: "cached-lookup"
    lookup-config:
      lookup-dataset:
        cache-enabled: true
        cache-ttl-seconds: 3600
        cache-max-size: 1000
        preload-enabled: true
        cache-refresh-ahead: true
```

### Memory Management

```java
// Configure memory-efficient processing
RulesEngineConfiguration config = new RulesEngineConfiguration.Builder()
    .withMemoryOptimization(true)
    .withMaxConcurrentEvaluations(100)
    .withResultCaching(false) // Disable for memory-constrained environments
    .build();
```

### Monitoring and Alerting

```java
// Set up performance monitoring
PerformanceMonitor monitor = rulesEngine.getPerformanceMonitor();
monitor.addThreshold("execution-time", Duration.ofMillis(100));
monitor.addThreshold("memory-usage", 50_000_000L); // 50MB

monitor.onThresholdExceeded((metric, value, threshold) -> {
    log.warn("Performance threshold exceeded: {} = {} (threshold: {})",
        metric, value, threshold);
    // Send alert
});
```

## Comprehensive Configuration Examples

### Financial Services Template

```yaml
metadata:
  name: "Financial Services Rules"
  version: "2.0.0"
  description: "Comprehensive financial services validation and enrichment"
  domain: "Financial Services"
  tags: ["finance", "trading", "compliance"]

# Currency enrichment with comprehensive data
enrichments:
  - id: "currency-enrichment"
    type: "lookup-enrichment"
    condition: "['currency'] != null"
    lookup-config:
      lookup-dataset:
        type: "yaml-file"
        file-path: "datasets/currencies.yaml"
        key-field: "code"
        cache-enabled: true
        cache-ttl-seconds: 7200
        default-values:
          region: "Unknown"
          isActive: false
          decimalPlaces: 2
    field-mappings:
      - source-field: "name"
        target-field: "currencyName"
      - source-field: "region"
        target-field: "currencyRegion"
      - source-field: "isActive"
        target-field: "currencyActive"

  # Country enrichment for regulatory compliance
  - id: "country-enrichment"
    type: "lookup-enrichment"
    condition: "['countryCode'] != null"
    lookup-config:
      lookup-dataset:
        type: "yaml-file"
        file-path: "datasets/countries.yaml"
        key-field: "code"
        cache-enabled: true
    field-mappings:
      - source-field: "name"
        target-field: "countryName"
      - source-field: "region"
        target-field: "region"
      - source-field: "regulatoryZone"
        target-field: "regulatoryZone"

rules:
  - id: "trade-amount-validation"
    name: "Trade Amount Validation"
    condition: "#amount != null && #amount > 0 && #amount <= 10000000"
    message: "Trade amount must be positive and not exceed 10M"
    severity: "ERROR"
    metadata:
      owner: "Trading Team"
      purpose: "Risk management"

  - id: "currency-validation"
    name: "Currency Code Validation"
    condition: "#currencyActive == true"
    message: "Currency must be active for trading"
    severity: "ERROR"
    depends-on: ["currency-enrichment"]
```

### Multi-Dataset Enrichment Example

```yaml
# Complex scenario with multiple related datasets
enrichments:
  # Primary instrument enrichment
  - id: "instrument-enrichment"
    type: "lookup-enrichment"
    condition: "['instrumentId'] != null"
    lookup-config:
      lookup-dataset:
        type: "yaml-file"
        file-path: "datasets/instruments.yaml"
        key-field: "id"
        cache-enabled: true
    field-mappings:
      - source-field: "name"
        target-field: "instrumentName"
      - source-field: "type"
        target-field: "instrumentType"
      - source-field: "sector"
        target-field: "sector"

  # Sector-based risk enrichment (depends on instrument enrichment)
  - id: "sector-risk-enrichment"
    type: "lookup-enrichment"
    condition: "['sector'] != null"
    depends-on: ["instrument-enrichment"]
    lookup-config:
      lookup-dataset:
        type: "inline"
        key-field: "sector"
        data:
          - sector: "Technology"
            riskLevel: "HIGH"
            volatilityFactor: 1.5
          - sector: "Utilities"
            riskLevel: "LOW"
            volatilityFactor: 0.8
          - sector: "Healthcare"
            riskLevel: "MEDIUM"
            volatilityFactor: 1.2
    field-mappings:
      - source-field: "riskLevel"
        target-field: "sectorRisk"
      - source-field: "volatilityFactor"
        target-field: "volatilityFactor"

rules:
  - id: "high-risk-validation"
    name: "High Risk Instrument Check"
    condition: "#sectorRisk == 'HIGH' && #amount > 1000000"
    message: "High-risk instruments require additional approval for amounts > 1M"
    severity: "WARNING"
    depends-on: ["sector-risk-enrichment"]
```

### Environment-Specific Configuration

```yaml
# Development environment
metadata:
  name: "Development Rules"
  environment: "development"

enrichments:
  - id: "test-data-enrichment"
    type: "lookup-enrichment"
    lookup-config:
      lookup-dataset:
        type: "inline"
        key-field: "code"
        data:
          - code: "TEST001"
            name: "Test Instrument 1"
            type: "EQUITY"
          - code: "TEST002"
            name: "Test Instrument 2"
            type: "BOND"

---
# Production environment
metadata:
  name: "Production Rules"
  environment: "production"

enrichments:
  - id: "production-data-enrichment"
    type: "lookup-enrichment"
    lookup-config:
      lookup-dataset:
        type: "yaml-file"
        file-path: "datasets/production-instruments.yaml"
        key-field: "code"
        cache-enabled: true
        cache-ttl-seconds: 3600
        preload-enabled: true
```

## Advanced Dataset Patterns

### Hierarchical Dataset Structure

```yaml
# datasets/product-hierarchy.yaml
data:
  - code: "EQUITY"
    name: "Equity Securities"
    category: "SECURITIES"
    subcategories:
      - code: "COMMON"
        name: "Common Stock"
        riskWeight: 1.0
      - code: "PREFERRED"
        name: "Preferred Stock"
        riskWeight: 0.8

  - code: "FIXED_INCOME"
    name: "Fixed Income Securities"
    category: "SECURITIES"
    subcategories:
      - code: "GOVT_BOND"
        name: "Government Bond"
        riskWeight: 0.2
      - code: "CORP_BOND"
        name: "Corporate Bond"
        riskWeight: 0.5
```

### Time-Based Dataset Configuration

```yaml
enrichments:
  - id: "time-sensitive-enrichment"
    type: "lookup-enrichment"
    lookup-config:
      lookup-dataset:
        type: "inline"
        key-field: "code"
        time-based: true
        effective-date-field: "effectiveDate"
        data:
          - code: "RATE001"
            rate: 0.05
            effectiveDate: "2024-01-01"
          - code: "RATE001"
            rate: 0.055
            effectiveDate: "2024-07-01"
```

### Conditional Dataset Loading

```yaml
enrichments:
  - id: "conditional-enrichment"
    type: "lookup-enrichment"
    condition: "['region'] == 'US' && ['instrumentType'] == 'EQUITY'"
    lookup-config:
      lookup-dataset:
        type: "yaml-file"
        file-path: "datasets/us-equity-data.yaml"
        key-field: "symbol"

  - id: "alternative-enrichment"
    type: "lookup-enrichment"
    condition: "['region'] == 'EU' && ['instrumentType'] == 'EQUITY'"
    lookup-config:
      lookup-dataset:
        type: "yaml-file"
        file-path: "datasets/eu-equity-data.yaml"
        key-field: "isin"
```
