# Rule Categories - Quick Start Guide

## What Are Rule Categories?

Rule categories are organizational containers that group related rules and provide metadata inheritance for enterprise governance.

## Why Use Categories?

1. **Organization** - Group rules by business function (validation, enrichment, compliance)
2. **Governance** - Track ownership, domain, and creation information
3. **Inheritance** - Rules inherit metadata from their category
4. **Lifecycle** - Control when rules are active with effective/expiration dates
5. **Execution** - Configure category-level execution behavior

## Basic Usage

### Step 1: Define Categories

```yaml
metadata:
  id: "my-rules"
  type: "rule-config"

categories:
  - name: "validation"
    description: "Data validation rules"
    business-domain: "Data Quality"
    business-owner: "validation-team@bank.com"
    priority: 10

  - name: "enrichment"
    description: "Data enrichment rules"
    business-domain: "Data Quality"
    business-owner: "enrichment-team@bank.com"
    priority: 20
```

### Step 2: Reference Categories in Rules

```yaml
rules:
  - id: "required-field-check"
    name: "Required Field Check"
    category: "validation"  # Reference the category
    condition: "#fieldName != null"
    message: "Field is required"
    severity: "ERROR"

  - id: "data-enrichment"
    name: "Data Enrichment"
    category: "enrichment"  # Different category
    condition: "true"
    message: "Data enriched"
    severity: "INFO"
```

## Key Concepts

### Metadata Inheritance

Rules automatically inherit metadata from their category:

```yaml
categories:
  - name: "validation"
    business-owner: "team@bank.com"
    created-by: "admin@bank.com"

rules:
  # Inherits business-owner and created-by from category
  - id: "rule-1"
    category: "validation"
    condition: "true"
    message: "Rule 1"
    severity: "ERROR"

  # Overrides business-owner, inherits created-by
  - id: "rule-2"
    category: "validation"
    business-owner: "other-team@bank.com"  # Override
    condition: "true"
    message: "Rule 2"
    severity: "ERROR"
```

### Property Precedence

When both category and rule specify a property:
1. **Rule-level value** takes precedence
2. **Category-level value** is used as fallback
3. **System default** applies if neither specifies

## Category Properties

| Property | Purpose | Example |
|----------|---------|---------|
| `name` | Unique identifier | "validation" |
| `description` | What the category does | "Data validation checks" |
| `priority` | Execution order | 10 (lower = higher) |
| `enabled` | Is category active? | true |
| `business-domain` | Business area | "Trade Processing" |
| `business-owner` | Responsible team | "team@bank.com" |
| `created-by` | Creator | "user@bank.com" |
| `effective-date` | When active | "2025-01-01" |
| `expiration-date` | When expires | "2025-12-31" |

## Best Practices

### 1. Organize by Business Function
```yaml
categories:
  - name: "input-validation"
  - name: "business-rules"
  - name: "compliance-checks"
```

### 2. Use Consistent Naming
```yaml
# ✅ Good
- name: "trade-validation"

# ❌ Avoid
- name: "TradeValidation"
- name: "trade_validation"
```

### 3. Assign Clear Ownership
```yaml
categories:
  - name: "validation"
    business-owner: "validation-team@bank.com"
    created-by: "john.doe@bank.com"
```

### 4. Use Lifecycle Dates
```yaml
categories:
  - name: "new-rules"
    effective-date: "2025-01-01"
    expiration-date: "2025-12-31"
```

### 5. Document Purpose
```yaml
categories:
  - name: "validation"
    description: "Validates trade data integrity and required fields"
```

## Real-World Example

```yaml
metadata:
  id: "trade-processing-rules"
  type: "rule-config"

categories:
  - name: "validation"
    description: "Trade data validation"
    business-domain: "Trade Processing"
    business-owner: "validation-team@bank.com"
    priority: 10

  - name: "compliance"
    description: "Regulatory compliance"
    business-domain: "Compliance"
    business-owner: "compliance-team@bank.com"
    priority: 20

rules:
  - id: "trade-id-required"
    name: "Trade ID Required"
    category: "validation"
    condition: "#tradeId != null"
    message: "Trade ID is required"
    severity: "ERROR"

  - id: "emir-reporting"
    name: "EMIR Reporting Required"
    category: "compliance"
    condition: "#counterparty.jurisdiction == 'EU'"
    message: "EMIR reporting required"
    severity: "WARNING"
```

## Where to Learn More

See section **4.3 Rule Categories** in `docs/APEX_YAML_REFERENCE.md` for:
- Complete property reference
- Metadata inheritance details
- Advanced examples
- Best practices
- Property precedence rules

