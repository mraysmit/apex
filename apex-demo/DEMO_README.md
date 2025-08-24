# APEX - Comprehensive Demo Suite

**Complete demonstrations of APEX capabilities with automatic discovery and execution**

This comprehensive demo suite showcases the full power of APEX (Advanced Processing Engine for eXpressions) through organized demonstrations spanning from simple validations to enterprise-grade solutions. The suite includes both a curated interactive experience and automatic discovery of all available demos.

## Quick Start

### Interactive Playground (Recommended for First-Time Users)
```bash
# Start the APEX Playground web interface
cd ../apex-playground
mvn spring-boot:run

# Open in browser
http://localhost:8081/playground
```

**Features:**
- **Interactive Example Browser:** Browse 11+ categorized YAML examples
- **One-Click Loading:** Load any configuration with sample data instantly
- **Real-Time Testing:** Test configurations immediately in the browser
- **Professional UI:** Modal dialog with organized categories
- **Complete Integration:** Access to all 113 YAML resources from this demo suite

### Interactive Demo Runner (Command Line)
```bash
# Interactive mode with menu - curated 5-demo experience
java -cp target/classes dev.mars.apex.demo.DemoRunner

# Run all curated demos sequentially
java -cp target/classes dev.mars.apex.demo.DemoRunner all
```

### Automatic Demo Discovery (Complete Suite)
```bash
# Run ALL available demos automatically (25+ demos)
java -cp target/classes dev.mars.apex.demo.AllDemosRunner

# List all discovered demos without running
java -cp target/classes dev.mars.apex.demo.AllDemosRunner --list

# Run demos from specific package
java -cp target/classes dev.mars.apex.demo.AllDemosRunner --package examples
java -cp target/classes dev.mars.apex.demo.AllDemosRunner --package advanced
java -cp target/classes dev.mars.apex.demo.AllDemosRunner --package bootstrap
```

### Run Specific Demo (Interactive Runner)
```bash
# Quick 5-minute introduction
java -cp target/classes dev.mars.apex.demo.DemoRunner quickstart

# Three-layer API design
java -cp target/classes dev.mars.apex.demo.DemoRunner layered

# Dataset enrichment
java -cp target/classes dev.mars.apex.demo.DemoRunner dataset

# Financial services examples
java -cp target/classes dev.mars.apex.demo.DemoRunner financial

# Performance monitoring
java -cp target/classes dev.mars.apex.demo.DemoRunner performance
```

### Alternative Demo Running Options

#### Option 1: Complete Learning Path
Run all demos in sequence for a comprehensive learning experience:

```bash
java -cp target/classes dev.mars.apex.demo.AllDemosRunner
```

#### Option 2: Individual Categories
Run specific demo categories based on your learning needs:

```bash
# Quick introduction (5-10 minutes)
java -cp target/classes dev.mars.apex.demo.runners.quickstart.QuickStartRunner

# Core concepts deep dive (15-20 minutes)
java -cp target/classes dev.mars.apex.demo.runners.fundamentals.FundamentalsRunner

# Implementation patterns (20-30 minutes)
java -cp target/classes dev.mars.apex.demo.runners.patterns.PatternsRunner

# Real-world applications (30-45 minutes)
java -cp target/classes dev.mars.apex.demo.runners.industry.IndustryRunner

# Advanced techniques (45+ minutes)
java -cp target/classes dev.mars.apex.demo.runners.advanced.AdvancedRunner
```

#### Option 3: Individual Demos
Run specific demos for focused learning:

```bash
# Lookup pattern examples
java -cp target/classes dev.mars.apex.demo.examples.lookups.SimpleFieldLookupDemo
java -cp target/classes dev.mars.apex.demo.examples.lookups.ConditionalExpressionLookupDemo
java -cp target/classes dev.mars.apex.demo.examples.lookups.NestedFieldLookupDemo
java -cp target/classes dev.mars.apex.demo.examples.lookups.CompoundKeyLookupDemo

# Financial services examples
java -cp target/classes dev.mars.apex.demo.financial.ComprehensiveFinancialSettlementDemo
java -cp target/classes dev.mars.apex.demo.bootstrap.CustodyAutoRepairBootstrap
```

## Interactive Playground Integration

All YAML configurations in this demo suite are now accessible through the **APEX Playground** web interface, providing an intuitive way to explore and test configurations.

### Available Example Categories in Playground

#### Quickstart (2 examples)
- **Quick Start Demo** - 5-minute introduction with validation rules
- **File Processing Configuration** - YAML configuration for file processing workflows

#### Financial (3 examples)
- **Financial Validation Rules** - Trading system validation and compliance
- **Financial Enrichment Rules** - Data enrichment for financial instruments
- **Settlement Validation Rules** - Post-trade settlement processing

#### Validation (2 examples)
- **Custody Auto Repair Rules** - Automated custody processing and repair
- **Derivatives Validation Rules** - OTC derivatives validation patterns

#### Lookup (2 examples)
- **Comprehensive Lookup Demo** - Advanced lookup patterns and enrichment
- **Compound Key Lookup** - Multi-field lookup configurations

#### Advanced (3 examples)
- **Batch Processing Demo** - Large-scale batch data processing
- **Dataset Enrichment Rules** - Advanced data enrichment patterns
- **Rule Chains Patterns** - Complex rule chaining and orchestration

### Playground Usage
1. **Start playground:** `cd ../apex-playground && mvn spring-boot:run`
2. **Open browser:** http://localhost:8081/playground
3. **Click "Load Example"** to browse all available configurations
4. **Select any example** to load YAML rules and sample data
5. **Test immediately** with the integrated rules engine

## Demo Organization

### Three Demo Access Methods Available

#### 1. Interactive Playground (`apex-playground`)
**Perfect for:** First-time users, visual exploration, immediate testing
- **Web Interface:** Professional browser-based experience
- **Visual Example Browser:** Categorized YAML configurations
- **Instant Testing:** Load and test configurations immediately
- **Complete Integration:** Access to all 113 YAML resources
- **No Setup Required:** Just start and browse

#### 2. Interactive Demo Runner (`DemoRunner`)
**Perfect for:** Command-line users, guided exploration, presentations
- **Curated Experience:** 5 focused demonstrations
- **Interactive Menu:** User-friendly navigation
- **Progressive Learning:** From basic concepts to advanced features
- **Documentation Aligned:** Matches user guide examples exactly

#### 3. Automatic Demo Discovery (`AllDemosRunner`)
**Perfect for:** Comprehensive testing, development, CI/CD integration
- **Complete Coverage:** 25+ demos across all packages
- **Auto-Discovery:** No manual registration required
- **Package Filtering:** Run specific demo categories
- **Robust Execution:** Isolated demo runs with error handling

### Demo Package Structure

#### Core Demos (2 demos)
- `QuickStartDemo` - 5-minute introduction to APEX
- `LayeredAPIDemo` - Three-layer API design philosophy

#### Examples Demos (15+ demos)
- `BasicUsageExamples` - Fundamental concepts and operations
- `AdvancedFeaturesDemo` - Advanced APEX features
- `YamlDatasetDemo` - Dataset enrichment capabilities
- `FinancialServicesDemo` - Financial domain demonstrations
- `PerformanceDemo` - Performance monitoring features
- `BatchProcessingDemo` - Batch processing capabilities
- `CommoditySwapValidationDemo` - Financial instrument validation
- `CustodyAutoRepairDemo` - Custody auto-repair functionality
- `FinancialTradingDemo` - Trading system examples
- `FluentRuleBuilderExample` - Fluent API examples
- `PerformanceMonitoringDemo` - Performance monitoring
- `SimplifiedAPIDemo` - Simplified API usage
- `YamlConfigurationDemo` - YAML configuration examples
- And more...

#### Advanced Demos (4+ demos)
- `ApexAdvancedFeaturesDemo` - Advanced APEX capabilities
- `DataServiceManagerDemo` - Data service integration
- `DynamicMethodExecutionDemo` - Dynamic method execution
- `PerformanceAndExceptionDemo` - Error handling and monitoring

#### Bootstrap Demos (3+ demos)
- `OtcOptionsBootstrapDemo` - OTC options processing
- `CommoditySwapValidationBootstrap` - Commodity swap validation
- `CustodyAutoRepairBootstrap` - Custody repair bootstrap

#### Rulesets Demos (10+ demos)
- `ComplianceServiceDemo` - Compliance rule processing
- `PricingServiceDemo` - Pricing rule services
- `RuleDefinitionServiceDemo` - Rule definition management
- `CustomerTransformerDemo` - Customer data transformation
- `PostTradeProcessingServiceDemo` - Post-trade processing
- And more service-oriented demonstrations...

## Interactive Demo Suite Overview (DemoRunner)

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

### Automatic Demo Discovery
- **No Ceremony:** Demos don't need to implement interfaces
- **Self-Contained:** Each demo runs independently with proper error handling
- **Flexible Execution:** Works with demos that have `main()` methods, `run()` methods, or both
- **Package Filtering:** Can run demos from specific packages
- **Clear Reporting:** Provides execution summary with success/failure counts

## YAML Configuration Resources (113 files)

### Core Rule Configurations
```
demo-rules/                          # Primary rule demonstrations (5 files)
├── quick-start.yaml                 # 5-minute introduction example (6,036 chars)
├── financial-validation.yaml       # Financial trading rules (13,019 chars)
├── custody-auto-repair-rules.yaml  # Custody processing (20,019 chars)
├── dataset-enrichment.yaml         # Data enrichment patterns (18,509 chars)
└── rule-chains-patterns.yaml       # Complex rule chaining (14,147 chars)
```

### Production-Ready Configurations
```
config/                              # Enterprise-grade configurations (9 files)
├── financial-enrichment-rules.yaml # Financial data enrichment (14,990 chars)
├── settlement-validation-rules.yaml # Settlement processing (11,082 chars)
├── derivatives-validation-rules.yaml # Derivatives validation (8,213 chars)
├── data-type-scenarios.yaml        # Data type handling scenarios
├── demo-data-sources.yaml          # Data source configurations
├── enhanced-enterprise-rules.yaml  # Enterprise rule patterns
├── financial-dataset-enrichment-rules.yaml # Dataset enrichment
├── financial-validation-rules.yaml # Financial validation patterns
└── production-demo-config.yaml     # Production configuration template
```

### Specialized Examples
```
demo-configs/                       # Advanced demonstration configs
├── comprehensive-lookup-demo.yaml  # Advanced lookup patterns (9,598 chars)

examples/lookups/                   # Lookup enrichment examples
├── compound-key-lookup.yaml       # Multi-field lookup (6,750 chars)

scenarios/                          # Business scenario configurations
├── commodity-swaps-scenario.yaml  # Commodity swap processing
├── otc-options-scenario.yaml      # OTC options validation
└── settlement-auto-repair-scenario.yaml # Settlement repair scenarios

yaml-examples/                      # Template and processing examples
├── file-processing-config.yaml    # File processing workflows (10,801 chars)
└── datasets/                      # Dataset configuration examples

bootstrap/                          # Infrastructure and setup configurations
├── otc-options-bootstrap.yaml     # OTC options bootstrap
├── commodity-swap-validation-bootstrap.yaml # Commodity validation
├── custody-auto-repair-bootstrap.yaml # Custody repair setup
└── [XML files and SQL scripts]
```

### Resource Access Methods
- **Playground UI:** Browse and test all configurations at http://localhost:8081/playground
- **Direct File Access:** All files available in `src/main/resources/`
- **Java Integration:** Load via `YamlRulesEngineService.loadFromResource()`

## Demo Structure Guide

### Learning Path

The demos are organized in a progressive learning path:

```
QUICKSTART (5-10 min) → FUNDAMENTALS (15-20 min) → PATTERNS (20-30 min) → INDUSTRY (30-45 min) → ADVANCED (45+ min)
```

### Organized Directory Structure

```
apex-demo/src/main/resources/demos/
├── quickstart/                    # Getting started (5-10 minutes)
│   └── quick-start.yaml          # Basic validation and enrichment
├── fundamentals/                  # Core concepts (15-20 minutes)
│   ├── rules/                    # Validation and business logic
│   │   └── financial-validation-rules.yaml
│   ├── enrichments/              # Data transformation patterns
│   └── datasets/                 # Reference data management
├── patterns/                      # Implementation patterns (20-30 minutes)
│   ├── lookups/                  # Data lookup strategies
│   │   ├── simple-field-lookup.yaml
│   │   ├── conditional-expression-lookup.yaml
│   │   ├── nested-field-lookup.yaml
│   │   ├── compound-key-lookup.yaml
│   │   └── comprehensive-lookup-demo.yaml
│   ├── calculations/             # Mathematical operations
│   └── validations/              # Validation patterns
├── industry/                      # Real-world applications (30-45 minutes)
│   └── financial-services/       # Financial industry examples
│       ├── settlement/           # Trade settlement processing
│       │   └── comprehensive-settlement-enrichment.yaml
│       ├── trading/              # Trading operations
│       └── custody/              # Custody and safekeeping
│           └── custody-auto-repair-rules.yaml
├── bootstrap/                     # Bootstrap configurations
│   ├── custody-auto-repair/      # Custody auto-repair bootstrap
│   │   ├── bootstrap-config.yaml
│   │   ├── datasets/
│   │   └── sql/
│   └── commodity-swap/           # Commodity swap bootstrap
│       ├── datasets/
│       └── schemas/
└── advanced/                      # Advanced techniques (45+ minutes)
    ├── performance/              # Optimization strategies
    ├── integration/              # System integration
    └── complex-scenarios/        # Multi-step workflows

reference/                         # Reference materials
└── syntax-examples/              # YAML syntax examples
    └── file-processing-config.yaml
```

### Demo Categories Explained

#### QuickStart (5-10 minutes)
**Purpose**: Get up and running quickly with basic concepts
**Content**:
- Basic validation rules using SpEL expressions
- Simple enrichment patterns
- YAML configuration fundamentals
- Core API usage

**Files**:
- `demos/quickstart/quick-start.yaml`

#### Fundamentals (15-20 minutes)
**Purpose**: Deep dive into core rules engine concepts
**Content**:
- Rules: Validation, business logic, compliance
- Enrichments: Lookup, calculation, transformation
- Datasets: Inline, external, compound keys
- Rule chains and orchestration

**Files**:
- `demos/fundamentals/rules/financial-validation-rules.yaml`
- Additional fundamental examples

#### Patterns (20-30 minutes)
**Purpose**: Learn common implementation patterns
**Content**:
- **Lookups**: Simple, conditional, nested, compound key strategies
- **Calculations**: Mathematical, string, date operations
- **Validations**: Format, business rules, cross-field validation

**Files**:
- `demos/patterns/lookups/simple-field-lookup.yaml`
- `demos/patterns/lookups/conditional-expression-lookup.yaml`
- `demos/patterns/lookups/nested-field-lookup.yaml`
- `demos/patterns/lookups/compound-key-lookup.yaml`
- `demos/patterns/lookups/comprehensive-lookup-demo.yaml`

#### Industry (30-45 minutes)
**Purpose**: Real-world industry applications
**Content**:
- **Financial Services**: Settlement, trading, custody operations
- Regulatory compliance scenarios
- Production-ready configurations
- Industry-specific patterns

**Files**:
- `demos/industry/financial-services/settlement/comprehensive-settlement-enrichment.yaml`
- `demos/industry/financial-services/custody/custody-auto-repair-rules.yaml`

#### Advanced (45+ minutes)
**Purpose**: Advanced techniques and optimization
**Content**:
- Performance optimization strategies
- Complex integration patterns
- Multi-step workflow orchestration
- Advanced configuration techniques

**Files**:
- Advanced scenario configurations
- Performance optimization examples
- Integration pattern demonstrations

### Backward Compatibility

The reorganization maintains **100% backward compatibility** through the `ResourcePathResolver` utility:

```java
// Old path automatically resolves to new location
String configPath = ResourcePathResolver.resolvePath("demo-rules/quick-start.yaml");
// Returns: "demos/quickstart/quick-start.yaml"

// Check if a path has been migrated
boolean hasMigration = ResourcePathResolver.hasMigration("config/financial-validation-rules.yaml");
// Returns: true

// Get all migrations
Map<String, String> allMigrations = ResourcePathResolver.getAllMigrations();
```

### Development Guidelines

#### Adding New Demos
1. Choose the appropriate category based on complexity and learning objectives
2. Follow the naming convention: `descriptive-name.yaml`
3. Include comprehensive metadata in YAML files
4. Add corresponding Java demo classes
5. Update the appropriate runner class
6. Add path mapping to `ResourcePathResolver` if needed

#### YAML Configuration Best Practices
1. **Metadata**: Always include name, version, description, and author
2. **Documentation**: Use comments to explain complex logic
3. **Organization**: Group related rules in rule chains
4. **Naming**: Use descriptive IDs and meaningful names
5. **Validation**: Test configurations thoroughly

#### Testing New Demos
1. Run the validation script: `python validate_migration.py`
2. Test individual demo classes
3. Verify runner integration
4. Check backward compatibility

## Project Structure

```
apex-demo/
├── src/main/java/dev/mars/apex/demo/
│   ├── DemoRunner.java              # Interactive entry point (5 curated demos)
│   ├── AllDemosRunner.java          # Automatic discovery runner (25+ demos)
│   ├── QuickStartDemo.java          # 5-minute introduction
│   ├── LayeredAPIDemo.java          # Three-layer API design
│   ├── advanced/                    # Advanced feature demonstrations
│   │   ├── ApexAdvancedFeaturesDemo.java
│   │   ├── DataServiceManagerDemo.java
│   │   ├── DynamicMethodExecutionDemo.java
│   │   └── PerformanceAndExceptionDemo.java
│   ├── bootstrap/                   # Bootstrap and infrastructure demos
│   │   ├── OtcOptionsBootstrapDemo.java
│   │   ├── CommoditySwapValidationBootstrap.java
│   │   └── CustodyAutoRepairBootstrap.java
│   ├── examples/                    # Core example demonstrations
│   │   ├── BasicUsageExamples.java
│   │   ├── YamlDatasetDemo.java
│   │   ├── FinancialServicesDemo.java
│   │   ├── PerformanceDemo.java
│   │   ├── BatchProcessingDemo.java
│   │   └── [15+ more examples]
│   ├── rulesets/                    # Service-oriented demonstrations
│   │   ├── ComplianceServiceDemo.java
│   │   ├── PricingServiceDemo.java
│   │   ├── RuleDefinitionServiceDemo.java
│   │   └── [10+ more service demos]
│   ├── data/                        # Data providers and mock services
│   ├── model/                       # Demo model classes
│   └── support/                     # Utility and support classes
├── src/main/resources/              # 113 YAML configuration files
│   ├── demo-rules/                  # Core rule configurations (5 files)
│   ├── config/                      # Production configurations (9 files)
│   ├── demo-configs/                # Advanced demo configs
│   ├── examples/lookups/            # Lookup examples
│   ├── scenarios/                   # Business scenarios
│   ├── yaml-examples/               # Template examples
│   ├── bootstrap/                   # Infrastructure configs
│   ├── batch-processing.yaml        # Batch processing demo
│   └── file-processing-rules.yaml  # File processing rules
└── src/test/java/                   # Comprehensive tests
    ├── AllDemosRunnerTest.java
    ├── AllDemosRunnerIntegrationTest.java
    ├── QuickStartDemoTest.java
    ├── LayeredAPIDemoTest.java
    └── [Package-specific test suites]
```

## Interactive Features

### Interactive Demo Runner Menu
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
# Interactive Demo Runner commands
java -cp target/classes dev.mars.apex.demo.DemoRunner help

# Command aliases supported
java -cp target/classes dev.mars.apex.demo.DemoRunner quick     # Same as quickstart
java -cp target/classes dev.mars.apex.demo.DemoRunner api       # Same as layered
java -cp target/classes dev.mars.apex.demo.DemoRunner yaml      # Same as dataset
java -cp target/classes dev.mars.apex.demo.DemoRunner finance   # Same as financial
java -cp target/classes dev.mars.apex.demo.DemoRunner perf      # Same as performance

# Automatic Demo Discovery commands
java -cp target/classes dev.mars.apex.demo.AllDemosRunner --list
java -cp target/classes dev.mars.apex.demo.AllDemosRunner --package core
java -cp target/classes dev.mars.apex.demo.AllDemosRunner --package examples
java -cp target/classes dev.mars.apex.demo.AllDemosRunner --package advanced
java -cp target/classes dev.mars.apex.demo.AllDemosRunner --package bootstrap
java -cp target/classes dev.mars.apex.demo.AllDemosRunner --package rulesets
```

### Automatic Demo Discovery Output
```
=== APEX Rules Engine - All Demos Runner ===
Automatically discovering and running all available demos...

Discovered 25 runnable demos:
  - dev.mars.apex.demo.QuickStartDemo (run())
  - dev.mars.apex.demo.examples.BasicUsageExamples (run())
  ...

════════════════════════════════════════════════════════════════════════════════
PACKAGE: CORE
════════════════════════════════════════════════════════════════════════════════

▶ Running: dev.mars.apex.demo.QuickStartDemo
  Method: run()
  ------------------------------------------------------------
  [Demo output...]
  ------------------------------------------------------------
✅ COMPLETED: QuickStartDemo (1250ms)

════════════════════════════════════════════════════════════════════════════════
EXECUTION SUMMARY
════════════════════════════════════════════════════════════════════════════════
Total Demos: 25
Successful: 25
Failed: 0
Skipped: 0

All demos completed successfully!
```

## Testing

### Run All Tests
```bash
mvn test
```

### Test Coverage
- **AllDemosRunnerTest:** Unit tests for demo discovery and basic functionality
- **AllDemosRunnerIntegrationTest:** Integration tests that actually run demos
- **QuickStartDemoTest:** Verifies all user guide examples work exactly as documented
- **LayeredAPIDemoTest:** Validates three-layer API demonstration
- **DemoRunnerTest:** Tests entry point functionality and command handling
- **Package-specific test suites:** Advanced, examples, rulesets, bootstrap tests

### Test Philosophy
Tests verify that documented functionality works **exactly as specified** in the user guide, ensuring consistency between documentation and implementation.

### Adding New Demos
To add a new demo to the automatic discovery:

1. Create your demo class with either:
   - A `public static void main(String[] args)` method
   - A `public void run()` method (no parameters)
   - Both methods (runner will prefer `run()`)

2. Add the class name to the `registerKnownDemoClasses()` method in `AllDemosRunner.java`

3. The demo will be automatically discovered and executed

## Getting Started

### Prerequisites
- Java 17 or higher
- Maven 3.6 or higher

### Build and Run

#### Interactive Playground (Recommended)
```bash
# Build both projects
mvn clean compile -f ../pom.xml

# Start the playground web interface
cd ../apex-playground
mvn spring-boot:run

# Open in browser
http://localhost:8081/playground

# Click "Load Example" to browse all 113 YAML configurations
```

#### Command Line Demos
```bash
# Build the project
mvn clean compile

# Run interactive demo suite (recommended for first-time users)
java -cp target/classes dev.mars.apex.demo.DemoRunner

# Run automatic demo discovery (all 25+ demos)
java -cp target/classes dev.mars.apex.demo.AllDemosRunner

# Run specific demo (interactive runner)
java -cp target/classes dev.mars.apex.demo.DemoRunner quickstart

# Run all curated demos (interactive runner)
java -cp target/classes dev.mars.apex.demo.DemoRunner all

# List all available demos
java -cp target/classes dev.mars.apex.demo.AllDemosRunner --list
```

## Documentation References

- **Complete User Guide:** `docs/APEX_RULES_ENGINE_USER_GUIDE.md`
- **Technical Reference:** `docs/APEX_TECHNICAL_REFERENCE.md`
- **Data Management Guide:** `docs/APEX_DATA_MANAGEMENT_GUIDE.md`
- **README First:** `docs/APEX_README_FIRST.md`

### Additional Resources

- **YAML Reference**: `reference/syntax-examples/`
- **API Documentation**: See JavaDoc in demo classes
- **Best Practices**: Embedded in runner classes and demo code
- **Troubleshooting**: Check logs and error messages for guidance

## Key Innovations

### 1. Three-Layer API Design
Progressive complexity that serves 100% of use cases while keeping 90% simple.

### 2. YAML Dataset Enrichment
Eliminates external lookup services for small static reference data.

### 3. Enterprise Performance Monitoring
Production-ready monitoring with < 1% overhead.

### 4. Financial Services Ready
Real-world OTC derivatives validation patterns.

### 5. Automatic Demo Discovery
No-ceremony demo execution with comprehensive coverage.

### 6. 100% Backward Compatible
Seamless integration with existing systems.

## Next Steps

### For First-Time Users (Recommended Path)
1. **Start with Playground:** http://localhost:8081/playground
2. **Browse Examples:** Click "Load Example" to see all 11 categories
3. **Try Quick Start:** Load the "Quick Start Demo" example
4. **Explore Financial:** Load financial validation and enrichment examples
5. **Test Advanced:** Try batch processing and rule chains

### For Command-Line Exploration
1. **Interactive Runner:** `java -cp target/classes dev.mars.apex.demo.DemoRunner`
2. **QuickStart Demo:** Get familiar with core concepts (5 minutes)
3. **Explore Layered API:** Understand the design philosophy
4. **Try YAML Datasets:** See the enrichment approach
5. **Financial Examples:** Real-world domain patterns

### For Comprehensive Exploration
1. **List All Demos:** `java -cp target/classes dev.mars.apex.demo.AllDemosRunner --list`
2. **Run Package-Specific Demos:** Focus on areas of interest
3. **Explore Bootstrap Demos:** Infrastructure and setup patterns
4. **Review Rulesets Demos:** Service-oriented architectures
5. **Performance Analysis:** Enterprise deployment insights

## Pro Tips

- **Development:** Start with Layer 1 (Ultra-Simple API)
- **Testing:** Use Layer 2 (Template-Based) for detailed validation
- **Production:** Consider Layer 3 (Advanced Config) for enterprise rules
- **Performance:** Monitor with built-in performance tracking
- **Datasets:** Use inline YAML datasets for small static reference data
- **Demo Discovery:** Use `AllDemosRunner` for comprehensive testing
- **Package Filtering:** Focus on specific demo categories for targeted learning

## Error Handling and Robustness

### AllDemosRunner Features
- **Isolated Execution:** Each demo runs independently - one failing demo doesn't stop the suite
- **Clear Error Reporting:** Exception details with execution timing
- **Graceful Handling:** Missing or broken demo classes handled gracefully
- **Performance Analysis:** Execution timing for performance insights

## Demo Structure Design Philosophy

*This structure was designed to provide a clear learning path from basic concepts to advanced real-world applications, making the APEX Rules Engine accessible to developers at all skill levels.*

The progressive learning approach ensures that:
- **Beginners** can start with simple concepts and build understanding gradually
- **Experienced developers** can jump to relevant patterns and industry examples
- **Enterprise teams** can focus on production-ready configurations and advanced techniques
- **All users** benefit from 100% backward compatibility during the transition

---

**Ready to improve your business rules management?**

**Interactive Playground (Recommended):** http://localhost:8081/playground

**First-time users:** `java -cp target/classes dev.mars.apex.demo.DemoRunner quickstart`

**Comprehensive exploration:** `java -cp target/classes dev.mars.apex.demo.AllDemosRunner --list`

**💡ro Tip:** The playground provides instant access to all 113 YAML configurations with professional UI and immediate testing capabilities!

