# APEX - Unified Demo Suite

**Comprehensive demonstrations of APEX capabilities**

This unified demo suite showcases the full power of APEX (Advanced Processing Engine for eXpressions) through five focused demonstrations, from simple validations to enterprise-grade solutions.

## Quick Start

### Run All Demos
```bash
# Interactive mode with menu
java -cp target/classes dev.mars.rulesengine.demo.DemoRunner

# Run all demos sequentially
java -cp target/classes dev.mars.rulesengine.demo.DemoRunner all
```

### Run Specific Demo
```bash
# Quick 5-minute introduction
java -cp target/classes dev.mars.rulesengine.demo.DemoRunner quickstart

# Three-layer API design
java -cp target/classes dev.mars.rulesengine.demo.DemoRunner layered

# Dataset enrichment
java -cp target/classes dev.mars.rulesengine.demo.DemoRunner dataset

# Financial services examples
java -cp target/classes dev.mars.rulesengine.demo.DemoRunner financial

# Performance monitoring
java -cp target/classes dev.mars.rulesengine.demo.DemoRunner performance
```

## Demo Suite Overview

### 1. QuickStart Demo (5 Minutes)
**Perfect for:** First-time users, quick evaluation

**Demonstrates:**
- One-liner rule evaluation with `Rules.check()`
- Template-based validation with `ValidationBuilder`
- YAML configuration loading and usage
- Core concepts and basic patterns

**Key Examples:**
```java
// One-liner evaluation
boolean isAdult = Rules.check("#age >= 18", Map.of("age", 25));

// Template-based validation
ValidationResult result = rulesService.validate(customer)
    .minimumAge(18)
    .emailRequired()
    .validate();

// YAML configuration
RulesEngine engine = YamlRulesEngineService.loadFromFile("rules.yaml");
```

### 2. Layered API Demo
**Perfect for:** Understanding API design philosophy

**Demonstrates:**
- **Layer 1:** Ultra-Simple API (90% of use cases)
- **Layer 2:** Template-Based Rules (8% of use cases)
- **Layer 3:** Advanced Configuration (2% of use cases)
- Progressive complexity and when to use each layer
- Same business rule implemented at all three layers

**Key Insight:** Start simple, add complexity only when needed.

### 3. YAML Dataset Demo
**Perfect for:** Understanding dataset enrichment

**Demonstrates:**
- Inline datasets embedded in YAML files
- Field mappings for data transformation
- Conditional processing based on data content
- Performance benefits of in-memory lookups
- Business value and ROI analysis

**Key Feature:**
```yaml
enrichments:
  - id: "currency-enrichment"
    lookup-config:
      lookup-dataset:
        type: "inline"
        data:
          - code: "USD"
            name: "US Dollar"
            region: "North America"
```

### 4. Financial Services Demo
**Perfect for:** Financial domain applications

**Demonstrates:**
- OTC derivatives validation (Commodity Total Return Swaps)
- Currency reference data integration
- Counterparty enrichment and validation
- Regulatory compliance checks
- Risk management rules
- Complete trade lifecycle validation

**Production-Ready:** Real-world financial trading patterns.

### 5. Performance Demo
**Perfect for:** Enterprise deployment planning

**Demonstrates:**
- Performance monitoring setup and configuration
- Real-time metrics collection and analysis
- Concurrent execution monitoring
- Performance optimization techniques
- Exception handling and recovery
- Performance insights and recommendations

**Enterprise-Grade:** < 1% monitoring overhead.

## Architecture Highlights

### Three-Layer API Design
```
Layer 1: Ultra-Simple (90%)    → Rules.check("#condition", data)
Layer 2: Template-Based (8%)   → ValidationBuilder with helpers
Layer 3: Advanced Config (2%)  → Full YAML configuration
```

### YAML Datasets
- **Traditional:** External lookup services, network latency, complex dependencies
- **Our Approach:** Inline datasets, sub-millisecond lookups, zero dependencies

### Enterprise Performance
- **Monitoring Overhead:** < 1%
- **Lookup Performance:** Sub-millisecond
- **Concurrent Safety:** Full thread safety
- **Error Recovery:** Graceful degradation

## Project Structure

```
rules-engine-demo/
├── src/main/java/dev/mars/rulesengine/demo/
│   ├── DemoRunner.java              # Unified entry point
│   ├── QuickStartDemo.java          # 5-minute introduction
│   ├── LayeredAPIDemo.java          # Three-layer API design
│   ├── YamlDatasetDemo.java         # Dataset enrichment
│   ├── FinancialServicesDemo.java   # Financial domain
│   ├── PerformanceDemo.java         # Performance monitoring
│   └── model/
│       └── Customer.java            # Demo model classes
├── src/main/resources/demo-rules/
│   ├── quick-start.yaml             # Basic configuration
│   ├── financial-validation.yaml   # Financial rules
│   └── dataset-enrichment.yaml     # Dataset examples
└── src/test/java/                   # Comprehensive tests
    ├── QuickStartDemoTest.java
    ├── LayeredAPIDemoTest.java
    └── DemoRunnerTest.java
```

## Interactive Features

### Menu-Driven Interface
```
Available Demonstrations:

  1. QuickStart Demo        - 5-minute introduction to core concepts
  2. Layered API Demo       - Three-layer API design philosophy
  3. YAML Dataset Demo      - Dataset enrichment
  4. Financial Services     - OTC derivatives and trading rules
  5. Performance Demo       - Monitoring and optimization
  6. Run All Demos          - Complete demonstration suite
  7. About                   - Project information and features

Enter your choice (1-7, or 'q' to quit):
```

### Command Line Interface
```bash
# All available commands
java DemoRunner help

# Command aliases supported
java DemoRunner quick     # Same as quickstart
java DemoRunner api       # Same as layered
java DemoRunner yaml      # Same as dataset
java DemoRunner finance   # Same as financial
java DemoRunner perf      # Same as performance
```

## Testing

### Run All Tests
```bash
mvn test
```

### Test Coverage
- **QuickStartDemoTest:** Verifies all user guide examples work exactly as documented
- **LayeredAPIDemoTest:** Validates three-layer API demonstration
- **DemoRunnerTest:** Tests entry point functionality and command handling
- **Integration Tests:** End-to-end demo execution verification

### Test Philosophy
Tests verify that documented functionality works **exactly as specified** in the user guide, ensuring consistency between documentation and implementation.

## Getting Started

### Prerequisites
- Java 17 or higher
- Maven 3.6 or higher

### Build and Run
```bash
# Build the project
mvn clean compile

# Run interactive demo suite
java -cp target/classes dev.mars.rulesengine.demo.DemoRunner

# Run specific demo
java -cp target/classes dev.mars.rulesengine.demo.DemoRunner quickstart

# Run all demos
java -cp target/classes dev.mars.rulesengine.demo.DemoRunner all
```

## Documentation References

- **Complete User Guide:** `docs/COMPLETE_USER_GUIDE.md`
- **Technical Reference:** `docs/TECHNICAL_IMPLEMENTATION_GUIDE.md`
- **YAML Dataset Guide:** `docs/YAML-Dataset-Enrichment-Guide.md`
- **Financial Services:** `docs/FINANCIAL_SERVICES_GUIDE.md`

## Key Innovations

### 1. Three-Layer API Design
Progressive complexity that serves 100% of use cases while keeping 90% simple.

### 2. YAML Dataset Enrichment
Eliminates external lookup services for small static reference data.

### 3. Enterprise Performance Monitoring
Production-ready monitoring with < 1% overhead.

### 4. Financial Services Ready
Real-world OTC derivatives validation patterns.

### 5. 100% Backward Compatible
Seamless integration with existing systems.

## Next Steps

1. **Start with QuickStart:** Get familiar with core concepts (5 minutes)
2. **Explore Layered API:** Understand the design philosophy
3. **Try YAML Datasets:** See the enrichment approach
4. **Financial Examples:** Real-world domain patterns
5. **Performance Analysis:** Enterprise deployment insights

## Pro Tips

- **Development:** Start with Layer 1 (Ultra-Simple API)
- **Testing:** Use Layer 2 (Template-Based) for detailed validation
- **Production:** Consider Layer 3 (Advanced Config) for enterprise rules
- **Performance:** Monitor with built-in performance tracking
- **Datasets:** Use inline YAML datasets for small static reference data

---

**Ready to improve your business rules management?**

Start with: `java -cp target/classes dev.mars.rulesengine.demo.DemoRunner quickstart`

