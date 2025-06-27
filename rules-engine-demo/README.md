# SpEL Rules Engine Demo Suite

A comprehensive demonstration of the SpEL Rules Engine capabilities, featuring real-world financial use cases, new layered APIs, and advanced monitoring features.

## 🎯 Overview

This demo module has been **rationalized and reorganized** to provide:

- **Unified Framework**: Consistent demo structure with standardized interfaces
- **Consolidated Examples**: Organized by category and complexity level
- **Layered API Design**: Ultra-simple, template-based, and advanced configuration
- **Performance Monitoring**: Real-time metrics and optimization techniques
- **Interactive Experience**: Menu-driven interface with non-interactive options
- **Reduced Complexity**: Streamlined from 50+ files to focused, maintainable structure

## 🏗️ Rationalized Module Structure

```
rules-engine-demo/
├── src/main/java/dev/mars/rulesengine/demo/
│   ├── DemoLauncher.java                          # 🆕 Unified entry point
│   ├── framework/                                 # 🆕 Demo framework
│   │   ├── Demo.java                             # Standard demo interface
│   │   ├── DemoCategory.java                     # Demo categorization
│   │   └── DemoFramework.java                    # Unified demo management
│   ├── examples/                                  # 🔄 Consolidated examples
│   │   ├── BasicUsageExamples.java               # 🆕 Fundamental concepts
│   │   ├── LayeredAPIDemo.java                   # 🆕 Improved API demonstration
│   │   ├── PerformanceMonitoringDemo.java        # 🆕 Consolidated performance demos
│   │   └── financial/                            # Financial instrument examples
│   │       ├── model/                            # Financial data models
│   │       └── CommoditySwapValidationDemo.java  # Main financial demo
│   ├── datasets/                                  # Test data and static data
│   ├── rulesets/                                  # Pre-built rule collections
│   ├── showcase/                                  # 📦 Legacy feature demonstrations
│   ├── simplified/                                # 📦 Legacy simplified API examples
│   ├── api/                                       # 📦 Legacy static utility API
│   └── [legacy packages]/                        # 📦 Preserved for compatibility
└── README.md                                      # Updated documentation
```

**Legend**: 🆕 New | 🔄 Improved | 📦 Legacy (preserved for compatibility)

## 🚀 Quick Start

### Running the Rationalized Demo Suite

```bash
# Interactive mode with unified menu
java -cp target/classes dev.mars.rulesengine.demo.DemoLauncher

# Or using Maven
mvn exec:java

# Non-interactive mode for specific demos
java -cp target/classes dev.mars.rulesengine.demo.DemoLauncher "Basic Usage Examples"
java -cp target/classes dev.mars.rulesengine.demo.DemoLauncher "Layered API Demonstration"
java -cp target/classes dev.mars.rulesengine.demo.DemoLauncher "Performance Monitoring"

# Run all demos
java -cp target/classes dev.mars.rulesengine.demo.DemoLauncher all

# List available demos
java -cp target/classes dev.mars.rulesengine.demo.DemoLauncher list

# Generate comprehensive report
java -cp target/classes dev.mars.rulesengine.demo.DemoLauncher report
```

### Available Demo Categories

#### 🎯 **Basic Usage** (Perfect for newcomers)
- **Basic Usage Examples** - Fundamental concepts and simple validation operations

#### 🚀 **API Demonstrations** (Understanding the layered design)
- **Layered API Demonstration** - Comprehensive three-layer API showcase
- **Simplified API Demo (Legacy)** - Original simplified API examples

#### 🏦 **Financial Examples** (Real-world use cases)
- **Financial Instrument Validation** - OTC Commodity Total Return Swap processing

#### ⚡ **Performance Monitoring** (Optimization and monitoring)
- **Performance Monitoring** - Consolidated performance and optimization demos
- **Performance & Exception Showcase** - Advanced monitoring features

#### 🔧 **Advanced Integration** (Complex scenarios)
- Legacy integration examples (preserved for compatibility)

## 💼 Financial Use Case: OTC Commodity Total Return Swaps

### What is a Commodity Total Return Swap?

A Total Return Swap (TRS) is a derivative contract where:
- One party pays the total return (price appreciation + income) of a commodity reference
- The other party typically pays a floating rate (e.g., LIBOR + spread)
- No physical delivery of the underlying commodity
- Used for gaining exposure to commodity prices without direct ownership

### Validation and Enrichment Process

The demo showcases a complete trade capture and validation workflow:

1. **Trade Capture**: Receive OTC commodity swap details
2. **Basic Validation**: Field presence, format, and data type checks
3. **Business Logic**: Notional limits, maturity constraints, currency consistency
4. **Static Data Validation**: Client, counterparty, currency, commodity reference data
5. **Regulatory Compliance**: Jurisdiction-specific rules (Dodd-Frank, EMIR, MiFID II)
6. **Risk Management**: Concentration limits, exposure calculations
7. **Enrichment**: Populate missing fields from static data
8. **Post-Trade Processing**: Settlement, booking, and lifecycle management

### Sample Commodity Swap

```java
CommodityTotalReturnSwap swap = new CommodityTotalReturnSwap(
    "TRS001",                    // Trade ID
    "CP001",                     // Counterparty ID (Global Investment Bank)
    "CLI001",                    // Client ID (Pension Fund Alpha)
    "ENERGY",                    // Commodity Type
    "WTI",                       // Reference Index (West Texas Intermediate)
    new BigDecimal("10000000"),  // Notional Amount ($10M)
    "USD",                       // Notional Currency
    LocalDate.now(),             // Trade Date
    LocalDate.now().plusYears(1) // Maturity Date
);
```

## 🔧 New Layered APIs Design

### Layer 1: Ultra-Simple API (90% of use cases)

```java
// One-liner validations
boolean isValid = Rules.check("#notionalAmount > 1000000", swap);

// Named rules for reuse
Rules.define("minimum-notional", "#notionalAmount >= 1000000");
boolean meetsMinimum = Rules.test("minimum-notional", swap);
```

### Layer 2: Template-Based Rules (8% of use cases)

```java
// Structured validation rule sets
RulesEngine validation = RuleSet.validation()
    .requiredField("tradeId", "Trade ID is required")
    .minimumValue("notionalAmount", new BigDecimal("1000000"), "Minimum $1M")
    .currencyCheck("notionalCurrency", "Valid currency required")
    .build();
```

### Layer 3: Advanced Configuration (2% of use cases)

```java
// Full control with performance monitoring
RulesEngineConfiguration config = new RulesEngineConfiguration();
Rule complexRule = config.rule("complex-validation")
    .withCategory("business-logic")
    .withCondition("#notionalAmount > 1000000 && #maturityDate.isBefore(#tradeDate.plusYears(5))")
    .withMessage("Complex validation passed")
    .withPriority(10)
    .build();
```

## 📊 Performance Monitoring Features

### Real-Time Metrics

- **Rule-level timing**: Individual rule execution times
- **Success/failure tracking**: Rule evaluation outcomes
- **Concurrent execution**: Multi-threaded performance analysis
- **Throughput measurement**: Rules per second
- **Error capture**: Exception details and recovery

### Performance Dashboard

```
┌─────────────────────────────────────────────────────────┐
│                 RULES ENGINE METRICS                    │
├─────────────────────────────────────────────────────────┤
│ Total Evaluations:     1,250                           │
│ Total Execution Time:  125.50 ms                       │
│ Average Time/Rule:     0.10 ms                         │
│ Peak Throughput:       9,960 eval/sec                  │
└─────────────────────────────────────────────────────────┘
```

## 🛡️ Exception Handling & Recovery

### Error Scenarios Handled

- **Invalid Expressions**: Syntax errors, null references
- **Missing Data**: Graceful handling of incomplete data
- **Static Data Failures**: Fallback mechanisms for reference data
- **Performance Degradation**: Automatic optimization suggestions
- **Concurrent Access**: Thread-safe error handling

### Recovery Mechanisms

- **Graceful Degradation**: Continue processing valid rules when some fail
- **Error Context**: Detailed error information for debugging
- **Retry Logic**: Automatic retry for transient failures
- **Circuit Breaker**: Prevent cascading failures

## 📚 Static Data Integration

### Reference Data Types

1. **Client Data**: Identity, regulatory classification, credit limits
2. **Account Data**: Account types, authorization, limits
3. **Counterparty Data**: Credit ratings, regulatory status, LEI
4. **Currency Data**: ISO codes, trading status, decimal places
5. **Commodity Data**: Reference indices, providers, quote currencies

### Validation Rules

```java
// Client validation
Rules.check("#staticDataProvider.isValidClient(#clientId)", context);

// Account ownership verification
Rules.check("#staticDataProvider.getClientAccount(#accountId).clientId == #clientId", context);

// Currency trading status
Rules.check("#staticDataProvider.isValidCurrency(#currency)", context);
```

## 🎓 Learning Path

### Beginner (5 minutes)
1. Run the Quick Start Guide (Option 6)
2. Try simple one-liner validations
3. Explore basic financial examples

### Intermediate (15 minutes)
1. Run the Financial Instrument Demo (Option 1)
2. Understand static data validation
3. Explore template-based rules

### Advanced (30 minutes)
1. Run the Performance Showcase (Option 3)
2. Study concurrent execution patterns
3. Implement custom rule sets

### Expert (60 minutes)
1. Run the Complete Demo (Option 4)
2. Analyze performance metrics
3. Design domain-specific rule frameworks

## 🔗 Integration Examples

### Trade Capture System Integration

```java
// Validate incoming trade
CommodityTotalReturnSwap trade = parseIncomingTrade(tradeData);
ValidationResult result = validateTrade(trade);

if (result.isValid()) {
    enrichTradeWithStaticData(trade);
    bookTrade(trade);
} else {
    rejectTrade(trade, result.getErrors());
}
```

### Post-Trade Processing

```java
// Settlement validation
boolean canSettle = Rules.check(
    "#tradeStatus == 'CONFIRMED' && #settlementDate <= #today.plusDays(#settlementDays)",
    trade
);

// Regulatory reporting
boolean requiresReporting = Rules.check(
    "#regulatoryRegime == 'DODD_FRANK' && #notionalAmount > 1000000",
    trade
);
```

## 🧪 Testing

### Unit Tests
- Individual rule validation
- Static data provider tests
- Performance benchmark tests

### Integration Tests
- End-to-end trade processing
- Concurrent execution tests
- Error recovery scenarios

### Performance Tests
- Load testing with high rule volumes
- Memory usage optimization
- Throughput measurement

## 📈 Performance Optimization Tips

1. **Use Simple Expressions**: Prefer `#amount > 1000000` over complex BigDecimal comparisons
2. **Cache Static Data**: Load reference data once and reuse
3. **Enable Rule Caching**: Reuse compiled expressions
4. **Monitor Performance**: Use built-in monitoring for optimization
5. **Batch Processing**: Process multiple trades together for efficiency

## 🤝 Contributing

To add new examples or improve existing ones:

1. Follow the established package structure
2. Include comprehensive JavaDoc documentation
3. Add performance monitoring to new rules
4. Include both positive and negative test cases
5. Update this README with new features

## 📄 License

This demo module is part of the SpEL Rules Engine project and follows the same licensing terms.
