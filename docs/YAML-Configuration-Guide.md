# YAML Configuration Guide for Rules Engine

## Overview

This guide explains how to externalize rules and enrichments configuration using YAML files in the Rules Engine project. This approach allows business users to modify rules without code changes or redeployment.

## Benefits of YAML Configuration

### For Business Users
- **No Code Changes Required**: Modify rules by editing YAML files
- **Runtime Configuration**: Load new rules without recompiling
- **Version Control**: Track rule changes using standard version control
- **Environment-Specific Rules**: Different configurations for dev/test/prod
- **Business-Friendly Syntax**: YAML is readable and editable by non-developers

### For Developers
- **Separation of Concerns**: Business logic separated from application code
- **Maintainability**: Easier to maintain and update rules
- **Testing**: Easy to create test configurations
- **Modularity**: Split rules across multiple files by domain or function

## YAML Configuration Structure

### Root Configuration File Structure

```yaml
metadata:
  name: "Configuration Name"
  version: "1.0.0"
  description: "Configuration description"
  author: "Author Name"
  created: "2024-01-15"
  last-modified: "2024-01-15"
  tags: ["tag1", "tag2"]

categories:
  - name: "category-name"
    display-name: "Display Name"
    description: "Category description"
    priority: 10
    enabled: true

rules:
  - id: "rule-id"
    name: "Rule Name"
    description: "Rule description"
    category: "category-name"
    condition: "#field != null"
    message: "Validation message"
    priority: 10
    enabled: true

rule-groups:
  - id: "group-id"
    name: "Group Name"
    description: "Group description"
    category: "category-name"
    rule-ids: ["rule-id-1", "rule-id-2"]

enrichments:
  - id: "enrichment-id"
    name: "Enrichment Name"
    type: "lookup-enrichment"
    target-type: "Trade"
    enabled: true

transformations:
  - id: "transformation-id"
    name: "Transformation Name"
    type: "field-transformation"
    target-type: "Trade"
    enabled: true
```

## Configuration Sections

### 1. Metadata Section

Provides information about the configuration file:

```yaml
metadata:
  name: "Financial Validation Rules"
  version: "1.0.0"
  description: "Validation rules for financial instruments"
  author: "Business Rules Team"
  created: "2024-01-15"
  last-modified: "2024-01-15"
  tags: ["financial", "validation", "commodity-swaps"]
```

### 2. Categories Section

Defines rule categories for organization:

```yaml
categories:
  - name: "validation"
    display-name: "Validation Rules"
    description: "Basic field and data validation rules"
    priority: 10
    enabled: true
    execution-order: 1
    stop-on-first-failure: false
    parallel-execution: false
```

### 3. Rules Section

Defines individual business rules:

```yaml
rules:
  - id: "trade-id-required"
    name: "Trade ID Required"
    description: "Validates that trade ID is present and not empty"
    category: "validation"
    condition: "#tradeId != null && #tradeId.trim().length() > 0"
    message: "Trade ID is required"
    priority: 10
    enabled: true
    tags: ["required-field", "trade-identification"]
```

### 4. Rule Groups Section

Organizes rules into logical groups:

```yaml
rule-groups:
  - id: "basic-validation"
    name: "Basic Field Validation"
    description: "Essential field validation rules"
    category: "validation"
    priority: 10
    enabled: true
    stop-on-first-failure: true
    parallel-execution: false
    rule-ids:
      - "trade-id-required"
      - "trade-date-required"
      - "amount-positive"
```

### 5. Enrichments Section

Defines data enrichment configurations:

```yaml
enrichments:
  - id: "counterparty-enrichment"
    name: "Counterparty Data Enrichment"
    description: "Enriches trade with counterparty static data"
    type: "lookup-enrichment"
    target-type: "Trade"
    enabled: true
    priority: 10
    condition: "#counterpartyId != null"
    lookup-config:
      lookup-service: "counterpartyLookupService"
      lookup-key: "#counterpartyId"
      cache-enabled: true
      cache-ttl-seconds: 3600
    field-mappings:
      - source-field: "name"
        target-field: "counterpartyName"
        required: true
      - source-field: "rating"
        target-field: "counterpartyRating"
        default-value: "NR"
```

## Usage Examples

### Loading Configuration in Java

```java
// Create YAML rules engine service
YamlRulesEngineService yamlService = new YamlRulesEngineService();

// Load rules engine from YAML file
RulesEngine engine = yamlService.createRulesEngineFromClasspath("config/financial-rules.yaml");

// Load from multiple files
RulesEngine engine = yamlService.createRulesEngineFromMultipleFiles(
    "config/validation-rules.yaml",
    "config/enrichment-rules.yaml"
);

// Execute rules
Map<String, Object> context = createTradeContext();
List<RuleResult> results = engine.evaluateRules("validation", context);
```

### Rule Condition Expressions

Rules use Spring Expression Language (SpEL) for conditions:

```yaml
# Simple field validation
condition: "#tradeId != null"

# Complex validation with multiple conditions
condition: "#tradeDate != null && #effectiveDate != null && #effectiveDate.compareTo(#tradeDate) >= 0"

# Numeric comparisons
condition: "#notionalAmount != null && #notionalAmount.compareTo(new java.math.BigDecimal('1000000')) > 0"

# String pattern matching
condition: "#tradeId != null && #tradeId.matches('^[A-Z]{3}[0-9]{3,6}$')"

# List/Set operations
condition: "#currency != null && {'USD', 'EUR', 'GBP', 'JPY'}.contains(#currency)"
```

## Best Practices

### 1. File Organization

- **Separate by Domain**: Create separate files for different business domains
- **Environment-Specific**: Use different configurations for different environments
- **Modular Structure**: Split large configurations into smaller, focused files

```
config/
├── validation/
│   ├── basic-validation-rules.yaml
│   ├── business-validation-rules.yaml
│   └── regulatory-validation-rules.yaml
├── enrichment/
│   ├── static-data-enrichment.yaml
│   └── calculation-enrichment.yaml
└── environments/
    ├── dev-rules.yaml
    ├── test-rules.yaml
    └── prod-rules.yaml
```

### 2. Rule Naming Conventions

- Use descriptive, kebab-case IDs: `trade-id-required`
- Include business context in names: `counterparty-lei-format`
- Group related rules with common prefixes: `date-*`, `amount-*`

### 3. Priority Management

- Use consistent priority ranges:
  - 1-10: Critical validation rules
  - 11-20: Business logic rules
  - 21-30: Regulatory compliance rules
  - 31-40: Risk management rules

### 4. Documentation

- Always include meaningful descriptions
- Use tags to categorize rules
- Document complex SpEL expressions
- Maintain change logs in metadata

## Migration from Hard-Coded Rules

### Step 1: Identify Existing Rules

Review existing Java code to identify:
- Rule conditions and logic
- Rule categories and priorities
- Rule dependencies and relationships

### Step 2: Create YAML Configuration

Convert identified rules to YAML format:

```java
// Before: Hard-coded rule
new Rule("trade-id-required", 
         "#tradeId != null && #tradeId.trim().length() > 0", 
         "Trade ID is required")
```

```yaml
# After: YAML configuration
- id: "trade-id-required"
  name: "Trade ID Required"
  condition: "#tradeId != null && #tradeId.trim().length() > 0"
  message: "Trade ID is required"
  category: "validation"
  priority: 10
  enabled: true
```

### Step 3: Update Application Code

Replace hard-coded rule creation with YAML loading:

```java
// Before
RulesEngineConfiguration config = new RulesEngineConfiguration();
config.registerRule(new Rule("rule1", "condition1", "message1"));
config.registerRule(new Rule("rule2", "condition2", "message2"));

// After
YamlRulesEngineService yamlService = new YamlRulesEngineService();
RulesEngine engine = yamlService.createRulesEngineFromClasspath("config/rules.yaml");
```

## Troubleshooting

### Common Issues

1. **YAML Parsing Errors**
   - Check YAML syntax and indentation
   - Validate required fields are present
   - Ensure proper quoting of special characters

2. **Rule Condition Errors**
   - Verify SpEL expression syntax
   - Check field names match context data
   - Test expressions with sample data

3. **Configuration Loading Errors**
   - Verify file paths are correct
   - Check classpath resources are available
   - Ensure proper file permissions

### Validation

Use the built-in validation to check configurations:

```java
YamlConfigurationLoader loader = new YamlConfigurationLoader();
try {
    YamlRuleConfiguration config = loader.loadFromFile("config/rules.yaml");
    // Configuration is valid
} catch (YamlConfigurationException e) {
    // Handle validation errors
    System.err.println("Configuration error: " + e.getMessage());
}
```

## Conclusion

YAML configuration provides a powerful way to externalize business rules and enrichments, enabling business users to modify rules without code changes while maintaining the flexibility and power of the underlying rules engine.
