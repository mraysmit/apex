# SpEL Rules Engine - Complete User Guide

## Overview

The SpEL Rules Engine is a powerful, flexible, and user-friendly rule evaluation system built on Spring Expression Language. It provides enterprise-grade capabilities with a progressive API design that scales from simple one-liners to complex rule management systems.

### Key Features

- **Three-Layer API Design**: Simple → Structured → Advanced
- **YAML Configuration**: External rule and dataset management
- **YAML Dataset Enrichment**: Inline datasets in configuration files
- **Performance Monitoring**: Enterprise-grade observability
- **Enhanced Error Handling**: Production-ready reliability
- **Financial Services Ready**: OTC derivatives validation
- **100% Backward Compatible**: Zero breaking changes

### What's New: YAML Dataset Enrichment

The Rules Engine now includes **revolutionary YAML Dataset Enrichment** functionality! You can now embed small lookup datasets directly in YAML configuration files, eliminating the need for external services for static reference data.

#### Key Benefits
- **Inline Datasets**: Embed lookup data directly in YAML files
- **No External Services**: Eliminate dependencies on external lookup services
- **High Performance**: Sub-millisecond in-memory lookups with caching
- **Business Editable**: Non-technical users can modify reference data
- **Version Controlled**: Datasets stored with configuration in Git

## Quick Start (5 Minutes)

### 1. One-Liner Rule Evaluation

```java
import dev.mars.rulesengine.core.api.Rules;

// Evaluate a rule in one line
boolean isAdult = Rules.check("#age >= 18", Map.of("age", 25)); // true
boolean hasBalance = Rules.check("#balance > 1000", Map.of("balance", 500)); // false

// With objects
Customer customer = new Customer("John", 25, "john@example.com");
boolean valid = Rules.check("#data.age >= 18 && #data.email != null", customer); // true
```

### 2. Template-Based Rules

```java
import dev.mars.rulesengine.core.api.RuleSet;

// Create validation rules using templates
RulesEngine validation = RuleSet.validation()
    .ageCheck(18)
    .emailRequired()
    .balanceMinimum(1000)
    .build();

ValidationResult result = validation.validate(customer);
```

### 3. YAML Configuration

Create a `rules.yaml` file:

```yaml
metadata:
  name: "Customer Validation Rules"
  version: "1.0.0"

rules:
  - id: "age-check"
    name: "Age Validation"
    condition: "#data.age >= 18"
    message: "Customer must be at least 18 years old"
    
  - id: "email-check"
    name: "Email Validation"
    condition: "#data.email != null && #data.email.contains('@')"
    message: "Valid email address is required"

enrichments:
  - id: "status-enrichment"
    type: "lookup-enrichment"
    condition: "['statusCode'] != null"
    lookup-config:
      lookup-dataset:
        type: "inline"
        key-field: "code"
        data:
          - code: "A"
            name: "Active"
            description: "Active customer"
          - code: "I"
            name: "Inactive"
            description: "Inactive customer"
    field-mappings:
      - source-field: "name"
        target-field: "statusName"
      - source-field: "description"
        target-field: "statusDescription"
```

Load and use the configuration:

```java
// Load YAML configuration
RulesEngineConfiguration config = YamlConfigurationLoader.load("rules.yaml");
RulesEngine engine = new RulesEngine(config);

// Evaluate rules with enrichment
Map<String, Object> data = Map.of(
    "age", 25,
    "email", "john@example.com",
    "statusCode", "A"
);

RuleResult result = engine.evaluate(data);
```

## Core Concepts

### Rules
Rules are the fundamental building blocks that define business logic:

```yaml
rules:
  - id: "trade-amount-validation"
    name: "Trade Amount Validation"
    condition: "#amount > 0 && #amount <= 1000000"
    message: "Trade amount must be between 0 and 1,000,000"
    severity: "ERROR"
    tags: ["financial", "validation"]
```

### Enrichments
Enrichments add data to your objects during rule evaluation:

```yaml
enrichments:
  - id: "currency-enrichment"
    type: "lookup-enrichment"
    condition: "['currency'] != null"
    lookup-config:
      lookup-dataset:
        type: "inline"
        key-field: "code"
        cache-enabled: true
        data:
          - code: "USD"
            name: "US Dollar"
            region: "North America"
          - code: "EUR"
            name: "Euro"
            region: "Europe"
    field-mappings:
      - source-field: "name"
        target-field: "currencyName"
      - source-field: "region"
        target-field: "currencyRegion"
```

### Datasets
Datasets provide lookup data for enrichments:

#### Inline Datasets (Recommended for small, static data)
```yaml
lookup-dataset:
  type: "inline"
  key-field: "code"
  cache-enabled: true
  cache-ttl-seconds: 3600
  data:
    - code: "USD"
      name: "US Dollar"
    - code: "EUR"
      name: "Euro"
```

#### External Dataset Files (Recommended for larger, reusable data)
```yaml
lookup-dataset:
  type: "yaml-file"
  file-path: "datasets/currencies.yaml"
  key-field: "code"
  cache-enabled: true
```

## YAML Configuration Guide

### Configuration Structure

```yaml
metadata:
  name: "Configuration Name"
  version: "1.0.0"
  description: "Configuration description"
  author: "Team Name"
  created: "2024-01-15"
  last-modified: "2024-07-26"
  tags: ["tag1", "tag2"]

rules:
  # Rule definitions

enrichments:
  # Enrichment definitions

rule-groups:
  # Rule group definitions
```

### Rule Configuration

```yaml
rules:
  - id: "unique-rule-id"
    name: "Human Readable Name"
    condition: "#data.field > 100"
    message: "Validation message"
    severity: "ERROR"  # ERROR, WARNING, INFO
    enabled: true
    tags: ["validation", "business"]
    metadata:
      owner: "Business Team"
      domain: "Finance"
      purpose: "Regulatory compliance"
```

### Enrichment Configuration

```yaml
enrichments:
  - id: "enrichment-id"
    type: "lookup-enrichment"
    condition: "['field'] != null"
    enabled: true
    lookup-config:
      lookup-dataset:
        type: "inline"  # or "yaml-file"
        key-field: "lookupKey"
        cache-enabled: true
        cache-ttl-seconds: 3600
        default-values:
          defaultField: "defaultValue"
        data:
          - lookupKey: "key1"
            field1: "value1"
            field2: "value2"
    field-mappings:
      - source-field: "field1"
        target-field: "enrichedField1"
      - source-field: "field2"
        target-field: "enrichedField2"
```

## Dataset Enrichment

### When to Use Dataset Enrichment

**Good Candidates:**
- Currency codes and names
- Country codes and regions
- Status codes and descriptions
- Product categories
- Reference data that changes infrequently
- Small to medium datasets (< 1000 records)

**Not Suitable For:**
- Large datasets (> 1000 records)
- Frequently changing data
- Data requiring complex business logic
- Real-time data from external systems

### Dataset Types

#### 1. Inline Datasets
Best for small, unique datasets:

```yaml
lookup-dataset:
  type: "inline"
  key-field: "code"
  data:
    - code: "A"
      name: "Active"
    - code: "I"
      name: "Inactive"
```

#### 2. External YAML Files
Best for reusable datasets:

Create `datasets/statuses.yaml`:
```yaml
data:
  - code: "A"
    name: "Active"
    description: "Active status"
  - code: "I"
    name: "Inactive"
    description: "Inactive status"
```

Reference in configuration:
```yaml
lookup-dataset:
  type: "yaml-file"
  file-path: "datasets/statuses.yaml"
  key-field: "code"
```

### Performance Optimization

```yaml
lookup-dataset:
  type: "inline"
  key-field: "code"
  cache-enabled: true
  cache-ttl-seconds: 3600
  preload-enabled: true
  data:
    # Dataset entries
```

## Migration from External Services

### Step-by-Step Migration Process

#### Step 1: Identify Migration Candidates
Analyze your existing lookup services for:
- Small, static datasets (< 100 records)
- Infrequently changing reference data
- Simple key-value lookups

#### Step 2: Extract Data
Export data from your existing service:

```java
// Before: External service
@Service
public class CurrencyLookupService implements LookupService {
    public Currency lookup(String code) {
        // Database or API call
    }
}
```

#### Step 3: Create YAML Dataset
Convert to YAML format:

```yaml
enrichments:
  - id: "currency-enrichment"
    type: "lookup-enrichment"
    lookup-config:
      lookup-dataset:
        type: "inline"
        key-field: "code"
        data:
          - code: "USD"
            name: "US Dollar"
            region: "North America"
          - code: "EUR"
            name: "Euro"
            region: "Europe"
```

#### Step 4: Update Configuration
Replace service calls with enrichment:

```java
// After: YAML dataset enrichment
RulesEngineConfiguration config = YamlConfigurationLoader.load("config.yaml");
RulesEngine engine = new RulesEngine(config);
RuleResult result = engine.evaluate(data); // Enrichment happens automatically
```

#### Step 5: Test and Validate
Ensure the migration works correctly:

```java
@Test
public void testCurrencyEnrichment() {
    Map<String, Object> data = Map.of("currency", "USD");
    RuleResult result = engine.evaluate(data);
    
    assertEquals("US Dollar", result.getEnrichedData().get("currencyName"));
    assertEquals("North America", result.getEnrichedData().get("currencyRegion"));
}
```

## Best Practices

### Configuration Organization
- Use external dataset files for reusable data
- Keep inline datasets small (< 50 records)
- Use meaningful IDs and names
- Include metadata for documentation

### Performance
- Enable caching for frequently accessed datasets
- Use appropriate cache TTL values
- Monitor performance metrics
- Preload datasets when possible

### Maintenance
- Version control all configuration files
- Use environment-specific configurations
- Document dataset sources and update procedures
- Regular review and cleanup of unused datasets

## Getting Help

### Common Issues
- **Configuration not loading**: Check YAML syntax and file paths
- **Enrichment not working**: Verify condition expressions and field mappings
- **Performance issues**: Enable caching and monitor metrics
- **Data not found**: Check key field matching and default values

### Documentation Resources
- Technical Implementation Guide for architecture details
- Financial Services Guide for domain-specific examples
- Configuration examples and templates

### Support
- Create GitHub issues for bugs or feature requests
- Check existing documentation for common solutions
- Review configuration examples for similar use cases
