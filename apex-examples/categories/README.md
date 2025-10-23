# APEX Category Examples

This directory demonstrates comprehensive category functionality in APEX, including separate file definitions, metadata inheritance, and enterprise governance patterns.

## 📁 Examples Overview

### 1. **Basic Examples** (`basic-examples/`)
Simple category usage patterns:
- Single-file category definitions
- Basic rule-to-category assignments
- Simple metadata inheritance

### 2. **Separate Files** (`separate-files/`)
Categories defined in dedicated YAML files:
- `categories-definitions.yaml` - Category definitions only
- `rules-with-categories.yaml` - Rules referencing external categories
- `enrichments-with-categories.yaml` - Enrichments using categories
- Demonstrates modular configuration management

### 3. **Inheritance Patterns** (`inheritance-patterns/`)
Advanced metadata inheritance scenarios:
- Complete inheritance from categories
- Partial overrides at rule/enrichment level
- Priority and execution behavior inheritance
- Lifecycle management patterns

### 4. **Enterprise Scenarios** (`enterprise-scenarios/`)
Real-world enterprise governance examples:
- Financial services compliance categories
- Trade processing workflow categories
- Risk management category hierarchies
- Multi-domain enterprise organization

## 🎯 Key Concepts Demonstrated

### **Category Definition Structure**
```yaml
categories:
  - name: "category-name"
    description: "Category description"
    priority: 10
    enabled: true
    # Enterprise metadata
    business-domain: "Domain Name"
    business-owner: "Owner Name"
    created-by: "Creator Name"
    effective-date: "2025-01-01"
    expiration-date: "2025-12-31"
    # Execution behavior
    stop-on-first-failure: false
    parallel-execution: true
```

### **Rule Category Assignment**
```yaml
rules:
  - id: "rule-id"
    name: "Rule Name"
    category: "category-name"  # References category
    condition: "#field > 100"
    message: "Validation message"
    # Inherits metadata from category
```

### **Enrichment Category Assignment**
```yaml
enrichments:
  - id: "enrichment-id"
    name: "Enrichment Name"
    category: "category-name"  # References category
    type: "field-enrichment"
    field-mappings:
      - source: "sourceField"
        target: "targetField"
    # Inherits metadata from category
```

## 🔄 Inheritance Behavior

### **Metadata Inheritance Priority**
1. **Component Level** (highest priority)
2. **Category Level** (inherited if not specified at component level)
3. **Default Values** (used if not specified anywhere)

### **Inherited Fields**
- `business-domain`
- `business-owner`
- `created-by`
- `effective-date`
- `expiration-date`

### **Execution Behavior Inheritance**
- `priority` (affects execution order)
- `stop-on-first-failure`
- `parallel-execution`

## 🧪 Testing Examples

Each example includes test files that demonstrate:
- Category loading from separate files
- Metadata inheritance verification
- Override behavior validation
- Integration scenario testing

## 💡 Best Practices

1. **Separate Category Files**: Define categories in dedicated files for reusability
2. **Meaningful Names**: Use descriptive category names that reflect business purpose
3. **Consistent Metadata**: Maintain consistent business metadata across related categories
4. **Lifecycle Management**: Use effective/expiration dates for governance
5. **Domain Organization**: Group categories by business domain for clarity
