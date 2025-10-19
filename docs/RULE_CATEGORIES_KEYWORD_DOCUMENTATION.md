# Category Keyword Documentation - Complete Reference

## Executive Summary

The `category` keyword in APEX YAML is now fully documented in section **4.3 Rule Categories** of `docs/APEX_YAML_REFERENCE.md`.

## Purpose of the `category` Keyword

The `category` keyword serves as a **rule classification and organizational mechanism** that enables:

1. **Rule Grouping** - Organize related rules by business function
2. **Metadata Inheritance** - Rules inherit enterprise metadata from their category
3. **Governance** - Track business ownership, domain, and creation information
4. **Lifecycle Management** - Control rule effectiveness with dates
5. **Execution Control** - Configure category-level execution behavior

## How It Works

### Document-Level Definition
Categories are defined once at the document level:

```yaml
categories:
  - name: "validation"
    business-domain: "Trade Processing"
    business-owner: "validation-team@bank.com"
    priority: 10
```

### Rule-Level Reference
Rules reference categories by name:

```yaml
rules:
  - id: "trade-id-required"
    category: "validation"  # Reference the category
    condition: "#tradeId != null"
    message: "Trade ID is required"
    severity: "ERROR"
```

### Metadata Inheritance
Rules automatically inherit metadata from their category:
- Rule-level metadata takes precedence
- Category-level metadata is used as fallback
- System defaults apply if neither specifies a value

## Category Properties

| Property | Type | Purpose |
|----------|------|---------|
| `name` | String | Unique category identifier |
| `description` | String | Human-readable description |
| `priority` | Integer | Execution priority (lower = higher) |
| `enabled` | Boolean | Whether category is active |
| `business-domain` | String | Business domain classification |
| `business-owner` | String | Owner responsible for category |
| `created-by` | String | Creator identifier |
| `effective-date` | String | When category becomes effective |
| `expiration-date` | String | When category expires |
| `stop-on-first-failure` | Boolean | Stop on first failure |
| `parallel-execution` | Boolean | Execute rules in parallel |

## Real-World Example

```yaml
metadata:
  id: "trade-processing-rules"
  type: "rule-config"

categories:
  - name: "validation"
    description: "Data validation checks"
    business-domain: "Trade Processing"
    business-owner: "validation-team@bank.com"
    priority: 10

  - name: "enrichment"
    description: "Data enrichment"
    business-domain: "Trade Processing"
    business-owner: "enrichment-team@bank.com"
    priority: 20

  - name: "compliance"
    description: "Regulatory compliance"
    business-domain: "Compliance"
    business-owner: "compliance-team@bank.com"
    priority: 30

rules:
  - id: "trade-id-required"
    name: "Trade ID Required"
    category: "validation"
    condition: "#tradeId != null"
    message: "Trade ID is required"
    severity: "ERROR"

  - id: "market-classification"
    name: "Market Classification"
    category: "enrichment"
    condition: "#instrumentType != null"
    message: "Market classification determined"
    severity: "INFO"

  - id: "emir-reporting-check"
    name: "EMIR Reporting Required"
    category: "compliance"
    condition: "#counterparty.jurisdiction == 'EU'"
    message: "EMIR reporting required"
    severity: "WARNING"
```

## Best Practices

1. **Organize by Business Function** - Use categories like "validation", "enrichment", "compliance"
2. **Use Consistent Naming** - Use kebab-case: "trade-validation" not "TradeValidation"
3. **Assign Clear Ownership** - Always specify `business-owner` for accountability
4. **Use Lifecycle Dates** - Track when rules are active with `effective-date` and `expiration-date`
5. **Document Purpose** - Always include a clear `description`

## Documentation Location

**File**: `docs/APEX_YAML_REFERENCE.md`
**Section**: 4.3 Rule Categories
**Lines**: 1005-1342
**Subsections**: 7 detailed subsections

## What's Documented

✅ Category Overview - 5 key purposes
✅ Defining Categories - Complete YAML structure
✅ Category Properties - 11 properties with descriptions
✅ Metadata Inheritance - How inheritance works
✅ Practical Examples - 3 real-world scenarios
✅ Best Practices - 5 actionable guidelines
✅ Property Precedence - Override rules and examples

## Key Insights

- Categories are **optional** but **recommended** for enterprise deployments
- Categories enable **metadata inheritance** reducing configuration duplication
- Categories support **lifecycle management** with effective/expiration dates
- Categories can control **execution behavior** at the category level
- Rule-level properties **always override** category-level properties

