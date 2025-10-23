# APEX Rule Categories Guide

**Date:** 2025-10-23
**Version:** 1.1
**Author:** APEX Development Team

## Overview

This guide provides comprehensive advice on rule category organization, metadata inheritance timing, and best practices for large collections of tightly related rules and rule groups in APEX.

## 🎯 Category Design Scope

**✅ Categories are designed for the IMPLEMENTATION LAYER:**
- **Rules** - Individual business logic units
- **Rule Groups** - Collections of related rules
- **Enrichments** - Data enhancement operations
- **Enrichment Groups** - Collections of related enrichments

**❌ Categories are NOT designed for the ORCHESTRATION LAYER:**
- **Scenarios** - Business process workflows (use business-domain metadata instead)
- **Scenario Registry** - Enterprise routing catalog (use business-domain metadata instead)
- **Processing Stages** - Workflow execution phases (inherit from underlying rule configs)

**Key Principle**: Categories provide **technical governance and metadata inheritance** for rule-level components, while scenarios provide **business process orchestration** with their own metadata structure.

## 📅 Metadata Inheritance Timing

### When Does Metadata Inheritance Happen?

**✅ Metadata inheritance happens at YAML loading/object creation time, NOT at runtime.**

```java
// Inheritance happens during createRuleWithMetadata() - at object creation time
String createdBy = yamlRule.getCreatedBy();
if (createdBy == null && yamlCategory != null) {
    createdBy = yamlCategory.getCreatedBy();  // INHERITANCE HAPPENS HERE
}
```

### Detailed Flow:

1. **YAML Parsing** → `YamlConfigurationLoader.fromYamlString()` parses YAML into `YamlRuleConfiguration`
2. **Category Cache Population** → `YamlRuleFactory.createRules()` populates category cache first
3. **Rule Creation** → `createRuleWithMetadata()` performs inheritance during object construction
4. **Metadata Resolution** → Rule-level metadata takes precedence, category metadata fills gaps
5. **Object Creation** → Final `Rule` object created with resolved metadata

**Key Point**: Once a `Rule` or `RuleGroup` object is created, the metadata is **immutable and baked-in**. There's no runtime inheritance lookup.

## 🔗 Relationship to Scenarios and Scenario Registry

### Architectural Separation

**Scenarios (Orchestration Layer)**:
- Define business process workflows and routing logic
- Use `business-domain` metadata for enterprise organization
- Reference rule configuration files through processing stages
- Handle classification rules and execution order

**Categories (Implementation Layer)**:
- Provide governance within rule configuration files
- Enable metadata inheritance for rules, rule groups, enrichments, and enrichment groups
- Support technical ownership and audit requirements
- Organize implementation components by business function

### Example Architecture

```yaml
# SCENARIO REGISTRY (Orchestration Layer)
scenarios:
  - id: "fx-trade-processing"
    business-domain: "Trading"           # Business organization
    owner: "fx.trading@bank.com"
    config-file: "scenarios/fx-scenario.yaml"

# INDIVIDUAL SCENARIO (Orchestration Layer)
scenario:
  scenario-id: "fx-trade-processing"
  processing-stages:
    - stage-name: "validation"
      config-file: "rules/fx-validation.yaml"    # References implementation
      execution-order: 1

# RULE CONFIGURATION (Implementation Layer)
categories:                              # Categories live here
  - name: "pre-trade-validation"
    business-domain: "Trading"           # Aligns with scenario business-domain
    business-owner: "Pre-Trade Team"
    priority: 5

rules:
  - id: "counterparty-validation"
    category: "pre-trade-validation"     # Category inheritance
    condition: "#counterpartyId != null"
```

**Key Point**: Categories operate **within** rule configuration files that scenarios reference, providing **fine-grained governance** for the **implementation components** that scenarios orchestrate.

## 📁 Category Organization Patterns

### Pattern 1: Single-File Organization (Small to Medium)

```yaml
# Rule Configuration File (Implementation Layer)
metadata:
  type: "rule-config"

# Categories defined at the top of the file
categories:
  - name: "compliance-rules"
    business-domain: "Compliance"
    business-owner: "Compliance Officer"
    created-by: "John Compliance"
    effective-date: "2025-01-01"
    expiration-date: "2025-12-31"
    priority: 5

  - name: "operations-enrichments"
    business-domain: "Operations"
    business-owner: "Operations Manager"
    created-by: "Sarah Operations"
    effective-date: "2025-01-01"
    expiration-date: "2025-06-30"
    priority: 20

# Rules reference categories
rules:
  - id: "inherited-metadata-rule"
    category: "compliance-rules"  # References category above
    condition: "#amount <= 10000"
    message: "Transaction amount within compliance limits"
    # Inherits all metadata from compliance-rules category

# Rule groups reference categories
rule-groups:
  - id: "compliance-validation-group"
    category: "compliance-rules"  # Same category inheritance
    rule-ids: ["inherited-metadata-rule"]

# Enrichments reference categories
enrichments:
  - id: "operations-data-enrichment"
    category: "operations-enrichments"  # Category inheritance for enrichments
    type: "field-enrichment"
    condition: "#data != null"

# Enrichment groups reference categories
enrichment-groups:
  - id: "operations-enrichment-group"
    category: "operations-enrichments"  # Category inheritance for enrichment groups
    enrichment-ids: ["operations-data-enrichment"]
```

**✅ Best for**: Up to 50-100 implementation components (rules + enrichments), single business domain

### Pattern 2: Multi-File with Shared Categories (Large Collections)

**Main Rule Configuration File:**
```yaml
# main-financial-rules.yaml
metadata:
  name: "Financial Trading Rules"
  type: "rule-config"

# Define all categories in main file for governance
categories:
  - name: "pre-trade-validation"
    business-domain: "Trading"
    business-owner: "Trading Desk"
    created-by: "Trading System"
    priority: 5
    effective-date: "2024-01-01"

  - name: "post-trade-settlement"
    business-domain: "Operations"
    business-owner: "Settlement Team"
    created-by: "Settlement System"
    priority: 10
    effective-date: "2024-01-01"

  - name: "risk-enrichment"
    business-domain: "Risk"
    business-owner: "Risk Manager"
    created-by: "Risk System"
    priority: 1
    effective-date: "2024-01-01"

# Cross-file references for implementation components
rule-refs:
  - source: "pre-trade-rules.yaml"
  - source: "settlement-rules.yaml"

enrichment-refs:
  - source: "risk-enrichments.yaml"
```

**Domain-Specific Implementation Files:**
```yaml
# pre-trade-rules.yaml (Rules and Rule Groups)
rules:
  - id: "counterparty-validation"
    category: "pre-trade-validation"  # References main file category
    condition: "#counterpartyId != null"
    message: "Valid counterparty required"
    # Inherits metadata from pre-trade-validation category

rule-groups:
  - id: "pre-trade-validation-group"
    category: "pre-trade-validation"  # Rule groups also inherit
    rule-ids: ["counterparty-validation"]

# risk-enrichments.yaml (Enrichments and Enrichment Groups)
enrichments:
  - id: "risk-score-calculation"
    category: "risk-enrichment"  # References main file category
    type: "field-enrichment"
    condition: "#data != null"
    # Inherits metadata from risk-enrichment category

enrichment-groups:
  - id: "risk-enrichment-group"
    category: "risk-enrichment"  # Enrichment groups also inherit
    enrichment-ids: ["risk-score-calculation"]
```

**✅ Best for**: 100+ implementation components (rules + enrichments + groups), multiple business domains, enterprise governance

### Pattern 3: Hierarchical Category Organization

```yaml
categories:
  # Top-level business domains
  - name: "trading"
    business-domain: "Trading"
    business-owner: "Head of Trading"
    priority: 10
    
  # Sub-domain categories
  - name: "trading-pre-trade"
    parent-category: "trading"
    business-domain: "Trading"
    business-owner: "Pre-Trade Team"
    priority: 5
    
  - name: "trading-execution"
    parent-category: "trading"
    business-domain: "Trading"
    business-owner: "Execution Team"
    priority: 7
```

**✅ Best for**: Complex organizations with clear hierarchies

### Pattern 4: Domain-Driven File Structure

```
financial-implementation/
├── main.yaml                           # Categories + metadata
├── compliance/
│   ├── kyc-rules.yaml                 # KYC rules (category: "compliance-kyc")
│   ├── aml-rules.yaml                 # AML rules (category: "compliance-aml")
│   ├── reporting-enrichments.yaml    # Reporting enrichments
│   └── compliance-groups.yaml        # Rule and enrichment groups
├── trading/
│   ├── pre-trade-rules.yaml          # Pre-trade validation rules
│   ├── execution-rules.yaml          # Trade execution rules
│   ├── post-trade-enrichments.yaml   # Settlement enrichments
│   └── trading-groups.yaml           # Rule and enrichment groups
└── risk/
    ├── market-risk-rules.yaml        # Market risk rules
    ├── credit-risk-enrichments.yaml  # Credit risk enrichments
    ├── operational-risk-rules.yaml   # Operational risk rules
    └── risk-groups.yaml              # Rule and enrichment groups
```

**✅ Best for**: Large enterprises with clear domain boundaries and mixed implementation components

## 📋 Best Practices

### 1. Category Governance

- **Define categories in main file** for centralized governance
- **Use consistent naming conventions** (kebab-case recommended)
- **Include comprehensive metadata** (business-owner, business-domain, etc.)
- **Document category purpose** and scope clearly

### 2. Logical Grouping Strategies

- **Group by business function** (compliance, trading, risk)
- **Group by lifecycle stage** (pre-trade, execution, post-trade)
- **Group by component type** (validation-rules, enrichment-calculations, audit-logging)
- **Group by priority/criticality** (critical, standard, informational)
- **Group by regulatory requirement** (MiFID, Basel, Dodd-Frank)

### 3. Metadata Strategy

- **business-domain**: Maps to organizational structure
- **business-owner**: Clear accountability and contact
- **created-by**: System or person responsible for creation
- **priority**: Execution order and business importance
- **effective-date/expiration-date**: Lifecycle management
- **source-system**: Origin system for audit trails

### 4. File Organization

- **Categories at top** of main rule configuration file
- **Implementation components reference categories** by name consistently (rules, rule groups, enrichments, enrichment groups)
- **Cross-file references** for modular architecture within rule configurations
- **Consistent directory structure** by business domain for rule configuration files
- **Clear naming conventions** for files and categories
- **Separate rule configurations from scenario definitions** (scenarios reference rule configs, not categories directly)

## 🎯 Enterprise Example: Large Financial Institution

### Scenario Layer (Orchestration)
```yaml
# scenarios/fx-trading-scenario.yaml
metadata:
  type: "scenario"
  business-domain: "Trading"

scenario:
  scenario-id: "fx-trading-processing"
  classification-rule:
    condition: "#data['tradeType'] == 'FX'"

  processing-stages:
    - stage-name: "validation"
      config-file: "rules/fx-validation-rules.yaml"    # References rule config
      execution-order: 1
    - stage-name: "enrichment"
      config-file: "rules/fx-enrichment-rules.yaml"   # References rule config
      execution-order: 2
```

### Implementation Layer (Rule Configurations with Categories)
```yaml
# rules/fx-validation-rules.yaml
metadata:
  name: "FX Trading Validation Rules"
  type: "rule-config"
  version: "2.0.0"

categories:
  # Regulatory compliance (highest priority)
  - name: "regulatory-compliance"
    description: "Regulatory compliance validation rules"
    business-domain: "Compliance"
    business-owner: "Chief Compliance Officer"
    created-by: "Compliance System"
    source-system: "ComplianceEngine"
    priority: 1
    effective-date: "2024-01-01"
    expiration-date: "2024-12-31"

  # Trading operations (business priority)
  - name: "trading-validation"
    description: "Trading execution and validation rules"
    business-domain: "Trading"
    business-owner: "Head of Trading"
    created-by: "Trading System"
    source-system: "TradingEngine"
    priority: 10
    effective-date: "2024-01-01"

# Implementation components reference categories
rules:
  - id: "mifid-compliance-check"
    category: "regulatory-compliance"  # Category inheritance
    condition: "#clientType == 'PROFESSIONAL'"
    message: "MiFID compliance validated"

  - id: "counterparty-validation"
    category: "trading-validation"     # Category inheritance
    condition: "#counterpartyId != null"
    message: "Valid counterparty required"

rule-groups:
  - id: "compliance-validation-group"
    category: "regulatory-compliance"  # Rule groups inherit too
    rule-ids: ["mifid-compliance-check"]

  - id: "trading-validation-group"
    category: "trading-validation"     # Rule groups inherit too
    rule-ids: ["counterparty-validation"]
```

```yaml
# rules/fx-enrichment-rules.yaml
metadata:
  name: "FX Trading Enrichment Rules"
  type: "rule-config"

categories:
  - name: "risk-enrichment"
    description: "Risk calculation and enrichment"
    business-domain: "Risk"
    business-owner: "Chief Risk Officer"
    created-by: "Risk System"
    source-system: "RiskEngine"
    priority: 5
    effective-date: "2024-01-01"

# Implementation components reference categories
enrichments:
  - id: "risk-score-calculation"
    category: "risk-enrichment"       # Enrichments inherit from categories
    type: "field-enrichment"
    condition: "#data != null"

enrichment-groups:
  - id: "risk-enrichment-group"
    category: "risk-enrichment"       # Enrichment groups inherit too
    enrichment-ids: ["risk-score-calculation"]
```

## 🔧 Technical Considerations

### Memory Usage

- Categories are cached during rule/enrichment creation
- Each implementation component (rule, rule group, enrichment, enrichment group) creates its own Category object instances
- For large collections (1000+ implementation components), consider category reference sharing
- Metadata inheritance adds ~500 bytes per implementation component object

### Performance

- Inheritance happens once at load time, not at runtime
- Category cache lookup is O(1) during implementation component creation
- No runtime performance impact from inheritance
- Large rule configuration YAML files may have longer parse times

### Governance

- Centralized category definitions enable consistent metadata
- Business owners can be clearly identified and contacted
- Effective/expiration dates enable lifecycle management
- Source system tracking enables audit trails

## 🚀 Migration Strategy

### From Unorganized Implementation Components

1. **Analyze existing implementation components** (rules, enrichments, groups) by business function
2. **Identify common metadata patterns** across all component types
3. **Create category taxonomy** based on business domains and component types
4. **Define categories with comprehensive metadata**
5. **Migrate implementation components incrementally** by business area
6. **Validate inheritance** with comprehensive tests for all component types

### From Single-File to Multi-File

1. **Extract categories** to main rule configuration file
2. **Group implementation components** by business domain into separate rule configuration files
3. **Add cross-file references** in main rule configuration file
4. **Test inheritance** across file boundaries for all component types
5. **Update deployment processes** for multi-file rule configuration loading
6. **Ensure scenarios reference the correct rule configuration files** (not category files directly)

## 🎯 Summary

Categories provide **fine-grained governance and metadata inheritance** for implementation components (rules, rule groups, enrichments, enrichment groups) within rule configuration files. They operate at the **implementation layer** and are complementary to scenarios, which provide **business process orchestration** at the **orchestration layer**.

This separation of concerns enables:
- **Scenarios**: Business process definition and routing
- **Categories**: Technical governance and metadata inheritance
- **Clear boundaries**: Between business orchestration and technical implementation
- **Reusability**: Rule configurations can be shared across multiple scenarios
- **Scalability**: Both layers can scale independently

This approach provides **centralized governance** while enabling **modular development** and **clear business ownership** across both orchestration and implementation layers.
