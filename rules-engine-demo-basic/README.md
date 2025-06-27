# SpEL Rules Engine Demo Suite - Basic

A comprehensive demonstration of the SpEL Rules Engine capabilities, featuring real-world financial use cases, new layered APIs, and advanced monitoring features.

## 🎯 Overview

This demo module has been completely reorganized to provide:

- **Structured Examples**: Organized by domain and complexity level
- **Financial Focus**: Real-world OTC derivative validation and enrichment
- **New Layered APIs**: Ultra-simple, template-based, and advanced configuration
- **Performance Monitoring**: Real-time metrics and optimization techniques
- **Exception Handling**: Robust error recovery and graceful degradation
- **Static Data Integration**: Comprehensive reference data validation

## 🏗️ New Module Structure

```
rules-engine-demo-basic/
├── src/main/java/dev/mars/rulesengine/demo/
│   ├── ComprehensiveRulesEngineDemo.java          # Main entry point
│   ├── examples/                                   # Domain-specific examples
│   │   └── financial/                             # Financial instrument examples
│   │       ├── model/                             # Financial data models
│   │       │   ├── CommodityTotalReturnSwap.java  # OTC commodity swap model
│   │       │   └── StaticDataEntities.java        # Reference data models
│   │       └── CommoditySwapValidationDemo.java   # Main financial demo
│   ├── datasets/                                   # Test data and static data
│   │   └── FinancialStaticDataProvider.java       # Static data repository
│   ├── rulesets/                                   # Pre-built rule collections
│   │   └── FinancialValidationRuleSet.java        # Financial validation rules
│   ├── showcase/                                   # Feature demonstrations
│   │   └── PerformanceAndExceptionShowcase.java   # Performance & error handling
│   └── simplified/                                 # Simplified API examples
│       └── SimplifiedAPIDemo.java                 # Layered APIs demo
└── README.md                                       # This file
```

## 🚀 Quick Start

### Running the Demo

```bash
# Interactive mode with menu
java -cp target/classes dev.mars.rulesengine.demo.ComprehensiveRulesEngineDemo

# Non-interactive mode for specific demos
java -cp target/classes dev.mars.rulesengine.demo.ComprehensiveRulesEngineDemo financial
java -cp target/classes dev.mars.rulesengine.demo.ComprehensiveRulesEngineDemo performance
```

### Available Demo Options

1. **🏦 Financial Instrument Validation** - OTC Commodity Total Return Swap processing
2. **🚀 Simplified APIs Demonstration** - New layered API design
3. **⚡ Performance & Exception Handling** - Monitoring and error recovery
4. **🔄 Complete End-to-End Demo** - Full feature showcase
5. **📊 Static Data Validation** - Reference data integration
6. **🎯 Quick Start Guide** - 5-minute introduction

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

## 📄 License

This demo module is part of the SpEL Rules Engine project and follows the same licensing terms.
