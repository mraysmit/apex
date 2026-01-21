# APEX Category Examples - Comprehensive Demo

This directory contains comprehensive examples demonstrating APEX category functionality, including separate file definitions, metadata inheritance, and enterprise governance patterns.

## 📁 Directory Structure

```
apex-demo/src/test/java/dev/mars/apex/demo/categories/
├── README.md                                    # This file
├── BasicCategoryTest.java                       # Basic category usage
├── BasicCategoryTest.yaml                       # Basic category configuration
├── MetadataInheritanceTest.java                 # Metadata inheritance examples
├── MetadataInheritanceTest.yaml                 # Metadata inheritance configuration
├── RuleGroupCategoryInheritanceTest.java        # Rule group inheritance
├── CategoryExamplesValidationTest.java          # Comprehensive validation test (5 tests)
├── separate-files/                              # Separate file examples
│   ├── enterprise-categories.yaml               # Category definitions only (11 categories)
│   ├── rules-with-external-categories.yaml      # Rules referencing external categories (15 rules)
│   └── enrichments-with-external-categories.yaml # Enrichments referencing external categories (16 enrichments)
├── inheritance-patterns/                        # Inheritance pattern examples
│   └── metadata-inheritance-examples.yaml       # Comprehensive inheritance patterns
└── enterprise-scenarios/                        # Real-world enterprise examples
    └── financial-services-categories.yaml       # Financial services categories (16 categories)
```

## 🎯 Key Features Demonstrated

### 1. **Separate Category Files** (`separate-files/`)
- **enterprise-categories.yaml**: Centralized category definitions
- **rules-with-external-categories.yaml**: Rules referencing external categories
- **enrichments-with-external-categories.yaml**: Enrichments using external categories

**Benefits:**
- Centralized governance and metadata management
- Reusability across multiple configuration files
- Clear separation of concerns
- Easier maintenance and updates

### 2. **Metadata Inheritance Patterns** (`inheritance-patterns/`)
- Complete inheritance from categories
- Partial overrides at rule/enrichment level
- Priority and execution behavior inheritance
- Lifecycle management with effective/expiration dates

### 3. **Enterprise Governance**
- Business domain organization
- Ownership and accountability tracking
- Source system tracking
- Lifecycle management

## 🔄 Inheritance Behavior

### **Metadata Inheritance Priority**
1. **Component Level** (highest priority) - Rule/Enrichment specific metadata
2. **Category Level** (inherited if not specified at component level)
3. **Default Values** (used if not specified anywhere)

### **Inherited Fields**
- `business-domain` - Business domain classification
- `business-owner` - Responsible business owner
- `created-by` - Creator identification
- `source-system` - Source system reference
- `effective-date` - When the rule/enrichment becomes active
- `expiration-date` - When the rule/enrichment expires

### **Execution Behavior Inheritance**
- `priority` - Affects execution order (lower numbers = higher priority)
- `stop-on-first-failure` - Whether to stop processing on first failure
- `parallel-execution` - Whether rules can execute in parallel

## Example Categories

### **Compliance Categories**
- `kyc-compliance` - Know Your Customer compliance rules
- `aml-screening` - Anti-Money Laundering screening rules
- `sanctions-screening` - Sanctions list screening and validation

### **Risk Management Categories**
- `credit-risk` - Credit risk assessment and validation rules
- `market-risk` - Market risk evaluation and limits
- `operational-risk` - Operational risk controls and validations

### **Business Operations Categories**
- `trade-validation` - Trade booking and validation rules
- `settlement-processing` - Settlement processing and validation rules
- `customer-onboarding` - Customer onboarding process rules

### **Data Quality Categories**
- `data-validation` - Data quality and validation rules
- `data-enrichment` - Data enrichment and enhancement rules

## 🧪 Test Examples

### **CategoryExamplesValidationTest.java** **5/5 PASSING**
Comprehensive validation test suite demonstrating:

**Test Methods:**
- `testBasicCategoryExamples()` - Validates basic inheritance patterns
- `testSeparateFileCategoryDefinitions()` - Validates external category references (11 categories, 15 rules, 16 enrichments)
- `testEnterpriseScenarioExamples()` - Validates financial services categories (16 categories)
- `testComprehensiveCategoryEcosystem()` - End-to-end ecosystem validation (30 total categories)
- `testInheritancePatternExamples()` - Validates inheritance pattern examples

**Validation Results:**
- **30 total categories** across all examples
- **20 rules** demonstrating category inheritance
- **16 enrichments** with category governance
- **All YAML configurations** successfully validated

### **Legacy Test Files** (for reference)
- `BasicCategoryTest.java` - Basic category definition and inheritance
- `MetadataInheritanceTest.java` - Advanced metadata inheritance scenarios
- `RuleGroupCategoryInheritanceTest.java` - Rule group inheritance patterns

## 💡 Best Practices

### 1. **Separate Category Files**
```yaml
# enterprise-categories.yaml - Category definitions only
metadata:
  id: "enterprise-categories"
  type: "category-definitions"

categories:
  - name: "compliance-rules"
    business-domain: "Regulatory Compliance"
    business-owner: "Chief Compliance Officer"
    # ... other metadata
```

### 2. **Rules Referencing External Categories**
```yaml
# rules-config.yaml - Rules referencing external categories
rules:
  - id: "compliance-rule"
    name: "Compliance Validation"
    category: "compliance-rules"  # References external category
    condition: "#amount <= 10000"
    # Inherits metadata from compliance-rules category
```

### 3. **Metadata Override Patterns**
```yaml
rules:
  - id: "override-rule"
    category: "compliance-rules"
    # Override specific metadata
    created-by: "Specialized Team"
    # Inherit remaining metadata from category
```

## 🚀 Getting Started

1. **Start with Basic Examples**: Review `BasicCategoryTest.java` and `BasicCategoryTest.yaml`
2. **Explore Separate Files**: Study the `separate-files/` directory examples
3. **Understand Inheritance**: Examine `inheritance-patterns/metadata-inheritance-examples.yaml`
4. **Run Tests**: Execute the test classes to see inheritance in action
5. **Experiment**: Modify the YAML files to test different inheritance patterns

## 📈 Advanced Scenarios

The examples demonstrate real-world enterprise scenarios including:
- Financial services compliance workflows
- Risk management category hierarchies
- Multi-domain enterprise organization
- Lifecycle management with governance
- Cross-functional team collaboration patterns

## Validation

All examples include comprehensive test validation that proves:
- Categories load correctly from separate files
- Metadata inheritance works as expected
- Override behavior functions properly
- Rules and enrichments execute successfully with inherited metadata
- Enterprise governance patterns are maintained
