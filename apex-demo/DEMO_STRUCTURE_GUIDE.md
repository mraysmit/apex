# APEX Demo Structure Guide

## Overview

The APEX Rules Engine demo module has been reorganized into a clear, logical learning structure that takes users from basic concepts to advanced real-world applications. This guide explains the new organization and how to navigate the demos effectively.

## 🎯 Learning Path

The demos are organized in a progressive learning path:

```
QUICKSTART (5-10 min) → FUNDAMENTALS (15-20 min) → PATTERNS (20-30 min) → INDUSTRY (30-45 min) → ADVANCED (45+ min)
```

## 📁 Directory Structure

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

## 🚀 Getting Started

### Option 1: Complete Learning Path
Run all demos in sequence for a comprehensive learning experience:

```bash
java -cp ... dev.mars.apex.demo.runners.AllDemosRunner
```

### Option 2: Individual Categories
Run specific demo categories based on your learning needs:

```bash
# Quick introduction (5-10 minutes)
java -cp ... dev.mars.apex.demo.runners.quickstart.QuickStartRunner

# Core concepts deep dive (15-20 minutes)
java -cp ... dev.mars.apex.demo.runners.fundamentals.FundamentalsRunner

# Implementation patterns (20-30 minutes)
java -cp ... dev.mars.apex.demo.runners.patterns.PatternsRunner

# Real-world applications (30-45 minutes)
java -cp ... dev.mars.apex.demo.runners.industry.IndustryRunner

# Advanced techniques (45+ minutes)
java -cp ... dev.mars.apex.demo.runners.advanced.AdvancedRunner
```

### Option 3: Individual Demos
Run specific demos for focused learning:

```bash
# Lookup pattern examples
java -cp ... dev.mars.apex.demo.examples.lookups.SimpleFieldLookupDemo
java -cp ... dev.mars.apex.demo.examples.lookups.ConditionalExpressionLookupDemo
java -cp ... dev.mars.apex.demo.examples.lookups.NestedFieldLookupDemo
java -cp ... dev.mars.apex.demo.examples.lookups.CompoundKeyLookupDemo

# Financial services examples
java -cp ... dev.mars.apex.demo.financial.ComprehensiveFinancialSettlementDemo
java -cp ... dev.mars.apex.demo.bootstrap.CustodyAutoRepairBootstrap
```

## 📚 Demo Categories Explained

### 🏃‍♂️ QuickStart (5-10 minutes)
**Purpose**: Get up and running quickly with basic concepts
**Content**: 
- Basic validation rules using SpEL expressions
- Simple enrichment patterns
- YAML configuration fundamentals
- Core API usage

**Files**:
- `demos/quickstart/quick-start.yaml`

### 🎓 Fundamentals (15-20 minutes)
**Purpose**: Deep dive into core rules engine concepts
**Content**:
- Rules: Validation, business logic, compliance
- Enrichments: Lookup, calculation, transformation
- Datasets: Inline, external, compound keys
- Rule chains and orchestration

**Files**:
- `demos/fundamentals/rules/financial-validation-rules.yaml`
- Additional fundamental examples

### 🔧 Patterns (20-30 minutes)
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

### 🏢 Industry (30-45 minutes)
**Purpose**: Real-world industry applications
**Content**:
- **Financial Services**: Settlement, trading, custody operations
- Regulatory compliance scenarios
- Production-ready configurations
- Industry-specific patterns

**Files**:
- `demos/industry/financial-services/settlement/comprehensive-settlement-enrichment.yaml`
- `demos/industry/financial-services/custody/custody-auto-repair-rules.yaml`

### 🚀 Advanced (45+ minutes)
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

## 🔄 Backward Compatibility

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

## 🛠 Development Guidelines

### Adding New Demos
1. Choose the appropriate category based on complexity and learning objectives
2. Follow the naming convention: `descriptive-name.yaml`
3. Include comprehensive metadata in YAML files
4. Add corresponding Java demo classes
5. Update the appropriate runner class
6. Add path mapping to `ResourcePathResolver` if needed

### YAML Configuration Best Practices
1. **Metadata**: Always include name, version, description, and author
2. **Documentation**: Use comments to explain complex logic
3. **Organization**: Group related rules in rule chains
4. **Naming**: Use descriptive IDs and meaningful names
5. **Validation**: Test configurations thoroughly

### Testing New Demos
1. Run the validation script: `python validate_migration.py`
2. Test individual demo classes
3. Verify runner integration
4. Check backward compatibility

## 📖 Additional Resources

- **YAML Reference**: `reference/syntax-examples/`
- **API Documentation**: See JavaDoc in demo classes
- **Best Practices**: Embedded in runner classes and demo code
- **Troubleshooting**: Check logs and error messages for guidance

## 🤝 Contributing

When contributing new demos:
1. Follow the established directory structure
2. Maintain the learning progression philosophy
3. Include comprehensive documentation
4. Test thoroughly across different scenarios
5. Update this guide if adding new categories

## 📞 Support

For questions about the demo structure or specific examples:
1. Check the inline documentation in YAML files
2. Review the runner class explanations
3. Examine similar examples in the same category
4. Refer to the comprehensive lookup demo for advanced patterns

---

*This structure was designed to provide a clear learning path from basic concepts to advanced real-world applications, making the APEX Rules Engine accessible to developers at all skill levels.*
