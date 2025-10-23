# APEX Rule Categories - Enterprise Business Rules Management Guide

## Table of Contents
1. [Business Overview](#business-overview)
2. [Design Philosophy](#design-philosophy)
3. [Core Concepts](#core-concepts)
4. [Implementation Guide](#implementation-guide)
5. [Enterprise Patterns](#enterprise-patterns)
6. [Best Practices](#best-practices)
7. [Technical Reference](#technical-reference)
8. [Implementation Status](#implementation-status)

---

## Business Overview

### The Enterprise Challenge

In large organisations, business rules proliferate rapidly across systems, teams, and domains. Without proper organisation and governance, this leads to:

- **Rule Sprawl**: Thousands of rules scattered across systems with no clear ownership
- **Governance Gaps**: No visibility into who owns what rules or when they were created
- **Maintenance Nightmares**: Rules become orphaned when teams change or projects end
- **Compliance Risks**: Inability to track rule lifecycle for audit and regulatory requirements
- **Operational Inefficiency**: Duplicate rules, conflicting logic, and inconsistent execution patterns

### The Solution: Rule Categories

APEX Rule Categories provide an **organisational framework** that transforms chaotic rule collections into well-governed, maintainable business logic assets.

### Business Value Proposition

| Business Challenge | Category Solution | Value Delivered |
|-------------------|-------------------|-----------------|
| **Governance** | Mandatory business ownership and domain tracking | Clear accountability and audit trails |
| **Lifecycle Management** | Effective/expiration date controls | Automated rule retirement and compliance |
| **Operational Excellence** | Execution control and priority management | Optimised performance and predictable behavior |
| **Team Collaboration** | Domain-based organisation | Clear boundaries and reduced conflicts |
| **Risk Management** | Centralised metadata and change tracking | Enhanced visibility and control |

---

## Design Philosophy

### 1. **Metadata Inheritance by Design**

**Design Principle**: Rules should inherit enterprise context from their organisational container.

**Business Rationale**: In enterprise environments, most business rules share common metadata within their functional domain. Rather than requiring every rule to specify business owner, domain, and lifecycle information, categories provide this context once and rules inherit it automatically.

**Implementation Logic**:
```
Rule Metadata = Rule-Specific Overrides + Category Defaults + System Defaults
```

This creates a **hierarchy of specificity** where:
- Rule-level metadata takes highest precedence (specific overrides)
- Category-level metadata provides domain defaults
- System-level metadata ensures no field is ever null

### 2. **Governance Through Structure**

**Design Principle**: Organisational structure should enforce governance requirements.

**Business Rationale**: Compliance and audit requirements demand clear ownership, lifecycle tracking, and change management. By making governance metadata mandatory at the category level, we ensure every rule has proper business context without burdening rule authors.

### 3. **Execution Control at Scale**

**Design Principle**: Performance and behavior should be configurable at the organisational level.

**Business Rationale**: Different business domains have different performance requirements and failure tolerance. Trading systems need fail-fast behavior, while reporting systems can tolerate individual rule failures. Categories allow domain-specific execution strategies.

### 4. **Lifecycle Management as First-Class Concept**

**Design Principle**: Rules have business lifecycles that must be managed systematically.

**Business Rationale**: Business rules are not permanent. Regulations change, business processes evolve, and temporary rules expire. Categories provide systematic lifecycle management with effective and expiration dates, enabling automated rule retirement and compliance reporting.

---

## Core Concepts

### 1. Rule Grouping - Organise by Business Function

**Concept**: Categories group related rules by their business purpose, not technical implementation.

**Business Logic**: Rules that serve the same business function should be managed together, regardless of their technical complexity. This enables:
- **Domain Expertise**: Subject matter experts can focus on their area
- **Change Management**: Related rules can be updated together
- **Impact Analysis**: Understanding downstream effects of changes
- **Testing Strategy**: Functional test suites aligned with business domains

**Example Organisation**:
```
├── customer-onboarding/          # New customer validation and setup
├── transaction-processing/       # Payment and transfer rules
├── risk-management/             # Credit, fraud, and compliance rules
├── regulatory-reporting/        # Audit and regulatory compliance
└── data-quality/               # Validation and enrichment rules
```

### 2. Metadata Inheritance - Enterprise Context Propagation

**Concept**: Rules automatically inherit business context from their category, reducing duplication and ensuring consistency.

**Inheritance Hierarchy**:
```
┌─────────────────┐    ┌──────────────────┐    ┌─────────────────┐
│   Rule Level    │ -> │  Category Level  │ -> │  System Level   │
│   (Specific)    │    │   (Domain)       │    │   (Default)     │
└─────────────────┘    └──────────────────┘    └─────────────────┘
```

**Inherited Properties**:
- **business-domain**: Functional area (e.g., "Trade Processing", "Risk Management")
- **business-owner**: Team or individual responsible for the rules
- **created-by**: Original author for audit trails
- **effective-date**: When rules become active
- **expiration-date**: When rules should be retired
- **execution-behavior**: Performance and failure handling preferences

**Business Benefits**:
- **Consistency**: All rules in a domain share common metadata
- **Efficiency**: Authors focus on business logic, not administrative details
- **Governance**: Automatic compliance with enterprise metadata requirements
- **Auditability**: Clear ownership and lifecycle tracking

### 3. Governance - Systematic Business Ownership

**Concept**: Every rule must have clear business ownership and domain classification for enterprise governance.

**Governance Framework**:
```yaml
categories:
  - name: "trade-validation"
    business-domain: "Trade Processing"           # What business area
    business-owner: "trading-ops@bank.com"       # Who is responsible
    created-by: "john.smith@bank.com"            # Who created it
    effective-date: "2025-01-01T00:00:00Z"       # When it starts
    expiration-date: "2025-12-31T23:59:59Z"      # When it ends
```

**Governance Benefits**:
- **Accountability**: Clear ownership for every rule
- **Lifecycle Management**: Systematic rule retirement
- **Audit Compliance**: Complete change history and ownership
- **Risk Management**: Understanding business impact of rule changes

### 4. Lifecycle Management - Temporal Rule Control

**Concept**: Business rules have natural lifecycles that must be managed systematically.

**Lifecycle States**:
- **Development**: Rules being created and tested
- **Active**: Rules in production use
- **Deprecated**: Rules marked for retirement
- **Expired**: Rules automatically disabled

**Temporal Control**:
```yaml
categories:
  - name: "covid-emergency-rules"
    effective-date: "2020-03-15T00:00:00Z"    # Emergency start
    expiration-date: "2023-12-31T23:59:59Z"   # Planned sunset

  - name: "basel-iii-compliance"
    effective-date: "2025-01-01T00:00:00Z"    # Regulatory deadline
    # No expiration - ongoing compliance requirement
```

**Business Value**:
- **Regulatory Compliance**: Automatic rule activation for regulatory deadlines
- **Emergency Response**: Rapid deployment and retirement of crisis rules
- **Maintenance Reduction**: Automatic cleanup of expired rules
- **Change Management**: Planned transitions between rule versions

### 5. Execution Control - Performance and Behavior Management

**Concept**: Different business domains require different execution strategies and performance characteristics.

**Execution Strategies**:

**Fail-Fast (Trading Systems)**:
```yaml
categories:
  - name: "trade-execution"
    stop-on-first-failure: true     # Stop immediately on any failure
    parallel-execution: false       # Sequential for deterministic order
    priority: 1                     # Highest priority
```

**Fault-Tolerant (Reporting Systems)**:
```yaml
categories:
  - name: "daily-reports"
    stop-on-first-failure: false    # Continue despite individual failures
    parallel-execution: true        # Parallel for performance
    priority: 50                    # Lower priority
```

**Performance Optimisation**:
- **Priority-Based Execution**: Critical rules execute first
- **Parallel Processing**: Independent rules run concurrently
- **Failure Handling**: Domain-appropriate error strategies
- **Resource Management**: Category-level resource allocation

---

## Implementation Guide

### Step 1: Design Your Category Structure

**Business-First Approach**: Start with business domains, not technical concerns.

**Example for Financial Services**:
```yaml
categories:
  # Customer Lifecycle Management
  - name: "customer-onboarding"
    description: "New customer validation and KYC rules"
    business-domain: "Customer Management"
    business-owner: "customer-ops@bank.com"
    priority: 10
    stop-on-first-failure: true    # Critical for compliance

  - name: "customer-maintenance"
    description: "Ongoing customer data validation"
    business-domain: "Customer Management"
    business-owner: "customer-ops@bank.com"
    priority: 20
    stop-on-first-failure: false   # Allow partial updates

  # Transaction Processing
  - name: "payment-validation"
    description: "Payment transaction validation rules"
    business-domain: "Payment Processing"
    business-owner: "payments-team@bank.com"
    priority: 5                    # Higher priority than customer rules
    parallel-execution: false      # Sequential for consistency

  # Risk and Compliance
  - name: "aml-screening"
    description: "Anti-money laundering screening rules"
    business-domain: "Compliance"
    business-owner: "compliance@bank.com"
    created-by: "regulatory-team@bank.com"
    effective-date: "2025-01-01T00:00:00Z"
    priority: 1                    # Highest priority
    stop-on-first-failure: true    # Zero tolerance for compliance
```

### Step 2: Implement Metadata Inheritance Strategy

**Inheritance Patterns**:

**Pattern 1: Domain-Specific Defaults**
```yaml
categories:
  - name: "trading-rules"
    business-domain: "Trading"
    business-owner: "trading-desk@bank.com"
    created-by: "trading-systems@bank.com"
    stop-on-first-failure: true
    parallel-execution: false

rules:
  - id: "position-limit-check"
    category: "trading-rules"
    # Inherits: business-domain="Trading", business-owner="trading-desk@bank.com"
    # Inherits: stop-on-first-failure=true, parallel-execution=false
    condition: "#position <= #limit"
    message: "Position exceeds limit"

  - id: "margin-requirement"
    category: "trading-rules"
    business-owner: "risk-management@bank.com"  # Override category default
    # Inherits: business-domain="Trading", created-by="trading-systems@bank.com"
    condition: "#margin >= #requirement"
    message: "Insufficient margin"
```

**Pattern 2: Lifecycle-Managed Categories**
```yaml
categories:
  - name: "regulatory-2025"
    description: "New regulatory requirements effective 2025"
    business-domain: "Compliance"
    business-owner: "regulatory@bank.com"
    effective-date: "2025-01-01T00:00:00Z"
    expiration-date: "2025-12-31T23:59:59Z"

  - name: "regulatory-2026"
    description: "Updated regulatory requirements for 2026"
    business-domain: "Compliance"
    business-owner: "regulatory@bank.com"
    effective-date: "2026-01-01T00:00:00Z"
    # No expiration - ongoing requirement
```

### Step 3: Configure Execution Behavior

**Performance-Critical Domains**:
```yaml
categories:
  - name: "real-time-trading"
    priority: 1
    parallel-execution: false       # Deterministic order
    stop-on-first-failure: true    # Fail fast

  - name: "market-data-validation"
    priority: 2
    parallel-execution: true        # High throughput
    stop-on-first-failure: false   # Continue processing
```

**Batch Processing Domains**:
```yaml
categories:
  - name: "end-of-day-reports"
    priority: 50
    parallel-execution: true        # Maximise throughput
    stop-on-first-failure: false   # Complete all processing

  - name: "data-quality-checks"
    priority: 60
    parallel-execution: true
    stop-on-first-failure: false
```

---

## Enterprise Patterns

### Pattern 1: Domain-Driven Category Design

**Principle**: Align categories with business domains, not technical systems.

**Anti-Pattern** (Technical Organisation):
```yaml
categories:
  - name: "database-rules"      # ❌ Technical focus
  - name: "api-validation"      # ❌ Technical focus
  - name: "batch-processing"    # ❌ Technical focus
```

**Best Practice** (Business Organisation):
```yaml
categories:
  - name: "customer-onboarding"    # ✅ Business focus
  - name: "trade-settlement"       # ✅ Business focus
  - name: "regulatory-reporting"   # ✅ Business focus
```

### Pattern 2: Hierarchical Governance

**Principle**: Use category hierarchy to reflect organisational structure.

```yaml
categories:
  # Executive Level - Strategic Rules
  - name: "enterprise-policies"
    business-domain: "Enterprise Governance"
    business-owner: "ceo@bank.com"
    priority: 1

  # Division Level - Operational Rules
  - name: "retail-banking"
    business-domain: "Retail Banking"
    business-owner: "retail-head@bank.com"
    priority: 10

  - name: "investment-banking"
    business-domain: "Investment Banking"
    business-owner: "ib-head@bank.com"
    priority: 10

  # Department Level - Functional Rules
  - name: "retail-lending"
    business-domain: "Retail Banking"
    business-owner: "lending-manager@bank.com"
    priority: 20
```

### Pattern 3: Temporal Rule Management

**Principle**: Use categories to manage rule lifecycles systematically.

```yaml
categories:
  # Emergency Response
  - name: "covid-emergency"
    effective-date: "2020-03-15T00:00:00Z"
    expiration-date: "2023-12-31T23:59:59Z"

  # Regulatory Compliance
  - name: "basel-iii-2025"
    effective-date: "2025-01-01T00:00:00Z"
    # No expiration - permanent requirement

  # Seasonal Rules
  - name: "year-end-processing"
    effective-date: "2024-12-01T00:00:00Z"
    expiration-date: "2025-01-31T23:59:59Z"
```

### Pattern 4: Performance Optimisation

**Principle**: Configure execution behavior based on business requirements.

```yaml
categories:
  # Mission-Critical (Trading)
  - name: "trade-execution"
    priority: 1
    stop-on-first-failure: true
    parallel-execution: false

  # High-Volume (Payments)
  - name: "payment-processing"
    priority: 5
    stop-on-first-failure: false
    parallel-execution: true

  # Batch Processing (Reports)
  - name: "reporting"
    priority: 50
    stop-on-first-failure: false
    parallel-execution: true
```

---

## Best Practices

### 1. Category Naming Conventions

**Use Business Language, Not Technical Terms**:
```yaml
# ✅ Good - Business-focused names
categories:
  - name: "customer-onboarding"
  - name: "trade-settlement"
  - name: "regulatory-reporting"
  - name: "risk-assessment"

# ❌ Avoid - Technical implementation details
categories:
  - name: "database-validation"
  - name: "api-rules"
  - name: "batch-job-rules"
```

**Follow Consistent Naming Patterns**:
- Use kebab-case for category names
- Include business domain context
- Avoid abbreviations and acronyms
- Use descriptive, self-documenting names

### 2. Metadata Management

**Always Specify Core Governance Fields**:
```yaml
categories:
  - name: "customer-validation"
    description: "Customer data validation and verification rules"
    business-domain: "Customer Management"        # Required
    business-owner: "customer-ops@bank.com"       # Required
    created-by: "john.smith@bank.com"             # Required
    effective-date: "2025-01-01T00:00:00Z"        # Recommended
    priority: 10                                  # Required
```

**Use Consistent Date Formats**:
- Always use ISO 8601 format: `YYYY-MM-DDTHH:MM:SSZ`
- Include timezone information
- Use UTC for consistency across environments

### 3. Priority Assignment Strategy

**Priority Ranges by Business Criticality**:
```yaml
# Priority 1-10: Mission Critical (Trading, Payments)
- name: "trade-execution"
  priority: 1

- name: "payment-processing"
  priority: 5

# Priority 11-30: Business Critical (Customer, Risk)
- name: "customer-onboarding"
  priority: 15

- name: "risk-assessment"
  priority: 20

# Priority 31-50: Operational (Reporting, Analytics)
- name: "daily-reports"
  priority: 35

- name: "data-analytics"
  priority: 40

# Priority 51+: Administrative (Maintenance, Cleanup)
- name: "data-cleanup"
  priority: 60
```

### 4. Execution Configuration Guidelines

**Match Execution Strategy to Business Requirements**:

**Real-Time Systems** (Trading, Payments):
```yaml
categories:
  - name: "real-time-trading"
    stop-on-first-failure: true    # Zero tolerance for errors
    parallel-execution: false      # Deterministic execution order
    priority: 1                    # Highest priority
```

**Batch Systems** (Reporting, Analytics):
```yaml
categories:
  - name: "batch-reporting"
    stop-on-first-failure: false   # Continue despite individual failures
    parallel-execution: true       # Maximise throughput
    priority: 50                   # Lower priority
```

### 5. Lifecycle Management Best Practices

**Plan for Rule Retirement**:
```yaml
categories:
  # Temporary rules should always have expiration dates
  - name: "covid-emergency-rules"
    effective-date: "2020-03-15T00:00:00Z"
    expiration-date: "2023-12-31T23:59:59Z"

  # Regulatory rules should have clear effective dates
  - name: "basel-iii-2025"
    effective-date: "2025-01-01T00:00:00Z"
    # No expiration for ongoing compliance
```

**Version Management Strategy**:
```yaml
# Use year-based versioning for regulatory rules
categories:
  - name: "mifid-ii-2024"
    expiration-date: "2024-12-31T23:59:59Z"

  - name: "mifid-ii-2025"
    effective-date: "2025-01-01T00:00:00Z"
```

---

## Technical Reference

### Category Properties Reference

| Property | Type | Required | Purpose | Example |
|----------|------|----------|---------|---------|
| `name` | String | Yes | Unique category identifier | "customer-validation" |
| `description` | String | Yes | Human-readable description | "Customer data validation rules" |
| `business-domain` | String | Yes | Business domain classification | "Customer Management" |
| `business-owner` | String | Yes | Team/person responsible | "customer-ops@bank.com" |
| `created-by` | String | Yes | Original creator | "john.smith@bank.com" |
| `priority` | Integer | Yes | Execution priority (1=highest) | 10 |
| `enabled` | Boolean | No | Whether category is active | true (default) |
| `effective-date` | String | No | When category becomes active | "2025-01-01T00:00:00Z" |
| `expiration-date` | String | No | When category expires | "2025-12-31T23:59:59Z" |
| `stop-on-first-failure` | Boolean | No | Stop execution on first failure | false (default) |
| `parallel-execution` | Boolean | No | Execute rules in parallel | true (default) |

### Metadata Inheritance Rules

**Inheritance Priority** (highest to lowest):
1. **Rule-level metadata** - Explicit values on individual rules (highest precedence)
2. **Category-level metadata** - Default values from assigned category (fallback)
3. **System-level defaults** - APEX framework defaults (last resort)

---

## Complete Inheritance Reference

### ✅ **Fully Inheritable Properties**

These properties can be defined at the category level and will be inherited by all rules and rule groups in that category. Both rules and rule groups can override any of these by specifying their own values.

| Property | Type | Purpose | Category Example | Rule/Rule Group Override Example |
|----------|------|---------|------------------|----------------------------------|
| `business-domain` | String | Business functional area | "Trade Processing" | "Risk Management" |
| `business-owner` | String | Team/person responsible | "trading-ops@bank.com" | "risk-team@bank.com" |
| `created-by` | String | Original creator | "john.smith@bank.com" | "jane.doe@bank.com" |
| `effective-date` | String (ISO 8601) | When rule/group becomes active | "2025-01-01T00:00:00Z" | "2025-02-01T00:00:00Z" |
| `expiration-date` | String (ISO 8601) | When rule/group expires | "2025-12-31T23:59:59Z" | "2025-06-30T23:59:59Z" |

**Inheritance Behavior**:
- **Rules**: If a rule doesn't specify these properties, it automatically inherits them from its assigned category. If the rule does specify them, the rule's values take precedence.
- **Rule Groups**: If a rule group doesn't specify these properties, it automatically inherits them from its assigned category. If the rule group does specify them, the rule group's values take precedence.

### 🔄 **Execution Control Properties** (Category-Level Only)

These properties control how rules within a category are executed. They are defined at the category level and affect all rules in the category, but individual rules cannot override them.

| Property | Type | Purpose | Default | Notes |
|----------|------|---------|---------|-------|
| `stop-on-first-failure` | Boolean | Stop category execution on first rule failure | false | Category-wide behavior |
| `parallel-execution` | Boolean | Execute rules in category in parallel | true | Category-wide behavior |
| `priority` | Integer | Category execution priority (1=highest) | 100 | Affects category ordering |

**Behavior**: These properties define the execution strategy for the entire category and cannot be overridden by individual rules.

### ❌ **Non-Inheritable Properties** (Rule-Specific Only)

These properties are always rule-specific and cannot be inherited from categories. Each rule must define its own values.

| Property | Type | Purpose | Why Not Inheritable |
|----------|------|---------|---------------------|
| `id` | String | Unique rule identifier | Must be unique per rule |
| `name` | String | Human-readable rule name | Specific to each rule's purpose |
| `description` | String | Detailed rule description | Specific to each rule's logic |
| `condition` | String | SpEL expression for rule logic | Core business logic, always unique |
| `message` | String | Message when rule triggers | Specific to rule's validation |
| `severity` | String | Rule severity (ERROR, WARNING, INFO) | Specific to rule's importance |
| `enabled` | Boolean | Whether rule is active | Individual rule control |
| `tags` | List<String> | Rule classification tags | Rule-specific metadata |
| `validation` | Object | Validation configuration | Rule-specific validation logic |
| `default-value` | Object | Default value for error recovery | Rule-specific fallback |
| `success-code` | String | Success response code | Rule-specific response |
| `error-code` | String | Error response code | Rule-specific response |
| `map-to-field` | Object | Field mapping configuration | Rule-specific mapping |
| `custom-properties` | Map | Custom metadata | Rule-specific extensions |

### 🔧 **Special Cases and Advanced Inheritance**

#### 1. **Priority Inheritance**
```yaml
categories:
  - name: "high-priority-rules"
    priority: 5                    # Category priority

rules:
  - id: "rule-1"
    category: "high-priority-rules"
    priority: 10                   # Rule priority (overrides category)
    # Rule executes with priority 10, not 5
```

#### 2. **Source System (Rule-Only)**
```yaml
rules:
  - id: "external-rule"
    category: "validation"
    source-system: "external-api"  # Only available at rule level
    # Categories cannot define source-system
```

#### 3. **Multiple Categories (Future Enhancement)**
```yaml
rules:
  - id: "multi-category-rule"
    categories: ["validation", "compliance"]  # Multiple category assignment
    # Inheritance priority: validation > compliance > system defaults
```

---

## Inheritance Examples

### Example 1: Complete Inheritance
```yaml
categories:
  - name: "compliance-rules"
    business-domain: "Regulatory Compliance"
    business-owner: "compliance@bank.com"
    created-by: "regulatory-team@bank.com"
    effective-date: "2025-01-01T00:00:00Z"
    expiration-date: "2025-12-31T23:59:59Z"

rules:
  - id: "kyc-check"
    name: "KYC Validation"
    category: "compliance-rules"
    condition: "#customer.kycStatus == 'VERIFIED'"
    message: "Customer KYC verification required"
    # Inherits ALL metadata from compliance-rules category
```

**Result**: Rule inherits business-domain="Regulatory Compliance", business-owner="compliance@bank.com", etc.

### Example 2: Partial Override
```yaml
rules:
  - id: "special-kyc-check"
    name: "Special KYC Validation"
    category: "compliance-rules"
    business-owner: "special-compliance@bank.com"  # Override category owner
    effective-date: "2025-02-01T00:00:00Z"         # Override effective date
    condition: "#customer.specialKycStatus == 'VERIFIED'"
    message: "Special customer KYC verification required"
    # Inherits: business-domain, created-by, expiration-date from category
    # Overrides: business-owner, effective-date with rule-specific values
```

**Result**: Rule uses its own business-owner and effective-date, but inherits business-domain, created-by, and expiration-date from category.

### Example 3: Complete Override
```yaml
rules:
  - id: "custom-rule"
    name: "Custom Business Rule"
    category: "compliance-rules"
    business-domain: "Custom Domain"               # Override
    business-owner: "custom-team@bank.com"         # Override
    created-by: "custom-developer@bank.com"        # Override
    effective-date: "2025-03-01T00:00:00Z"         # Override
    expiration-date: "2025-09-30T23:59:59Z"        # Override
    condition: "#customLogic == true"
    message: "Custom validation rule"
    # Overrides ALL inheritable metadata
```

**Result**: Rule uses all its own metadata values, ignoring category defaults entirely.

### Example 4: Rule Group Complete Inheritance
```yaml
categories:
  - name: "trading-rules"
    business-domain: "Trading Operations"
    business-owner: "trading-desk@bank.com"
    created-by: "trading-systems@bank.com"
    effective-date: "2025-01-01T00:00:00Z"
    expiration-date: "2025-12-31T23:59:59Z"

rule-groups:
  - id: "position-validation-group"
    name: "Position Validation Rules"
    category: "trading-rules"
    operator: "AND"
    rule-ids: ["position-limit", "exposure-check", "margin-requirement"]
    # Inherits ALL metadata from trading-rules category
```

**Result**: Rule group inherits business-domain="Trading Operations", business-owner="trading-desk@bank.com", created-by="trading-systems@bank.com", effective-date, and expiration-date from category.

### Example 5: Rule Group Partial Override
```yaml
rule-groups:
  - id: "special-trading-group"
    name: "Special Trading Rules"
    category: "trading-rules"
    operator: "OR"
    business-owner: "special-trading@bank.com"  # Override category owner
    created-by: "special-systems@bank.com"      # Override creator
    rule-ids: ["high-value-trade", "vip-client-trade"]
    # Inherits business-domain, effective-date, expiration-date from category
```

**Result**: Rule group overrides business-owner and created-by, but inherits business-domain, effective-date, and expiration-date from category.

### Example 6: No Category Assignment
```yaml
rules:
  - id: "standalone-rule"
    name: "Standalone Validation"
    business-owner: "direct-team@bank.com"
    condition: "#data.value > 1000"
    message: "Value exceeds threshold"
    # No category assignment - no inheritance

rule-groups:
  - id: "standalone-group"
    name: "Standalone Rule Group"
    operator: "AND"
    business-owner: "direct-team@bank.com"
    rule-ids: ["standalone-rule"]
    # No category assignment - no inheritance
```

**Result**: Both rule and rule group use only explicitly specified metadata, no inheritance occurs.

### YAML Schema Example

```yaml
# Complete Enterprise Example
metadata:
  id: "enterprise-trading-rules"
  type: "rule-config"
  version: "2025.1"

categories:
  # Mission-Critical Trading Rules
  - name: "trade-execution"
    description: "Real-time trade execution validation and processing rules"
    business-domain: "Trading Operations"
    business-owner: "trading-desk@bank.com"
    created-by: "trading-systems@bank.com"
    effective-date: "2025-01-01T00:00:00Z"
    priority: 1
    stop-on-first-failure: true
    parallel-execution: false

  # Risk Management Rules
  - name: "risk-controls"
    description: "Position limits, margin requirements, and risk assessment rules"
    business-domain: "Risk Management"
    business-owner: "risk-management@bank.com"
    created-by: "risk-systems@bank.com"
    effective-date: "2025-01-01T00:00:00Z"
    priority: 2
    stop-on-first-failure: true
    parallel-execution: false

  # Regulatory Compliance
  - name: "mifid-ii-2025"
    description: "MiFID II compliance rules effective 2025"
    business-domain: "Regulatory Compliance"
    business-owner: "compliance@bank.com"
    created-by: "regulatory-team@bank.com"
    effective-date: "2025-01-01T00:00:00Z"
    priority: 3
    stop-on-first-failure: true
    parallel-execution: false

  # Data Quality and Enrichment
  - name: "data-enrichment"
    description: "Market data enrichment and reference data validation"
    business-domain: "Data Management"
    business-owner: "data-team@bank.com"
    created-by: "data-engineering@bank.com"
    effective-date: "2025-01-01T00:00:00Z"
    priority: 10
    stop-on-first-failure: false
    parallel-execution: true

rules:
  # Trading Rules - Inherit from trade-execution category
  - id: "position-limit-check"
    name: "Position Limit Validation"
    category: "trade-execution"
    condition: "#position.quantity <= #limits.maxPosition"
    message: "Position exceeds maximum limit"
    severity: "ERROR"

  - id: "settlement-date-validation"
    name: "Settlement Date Validation"
    category: "trade-execution"
    condition: "#trade.settlementDate >= #trade.tradeDate"
    message: "Settlement date must be on or after trade date"
    severity: "ERROR"

  # Risk Rules - Inherit from risk-controls category
  - id: "margin-requirement"
    name: "Margin Requirement Check"
    category: "risk-controls"
    condition: "#account.availableMargin >= #trade.requiredMargin"
    message: "Insufficient margin for trade"
    severity: "ERROR"

  # Compliance Rules - Inherit from mifid-ii-2025 category
  - id: "best-execution-check"
    name: "MiFID II Best Execution"
    category: "mifid-ii-2025"
    condition: "#venue.bestExecution == true"
    message: "Trade executed on best execution venue"
    severity: "INFO"

  # Data Rules - Inherit from data-enrichment category
  - id: "instrument-classification"
    name: "Instrument Classification Enrichment"
    category: "data-enrichment"
    business-owner: "reference-data@bank.com"  # Override category default
    condition: "#instrument.type != null"
    message: "Instrument classified successfully"
    severity: "INFO"
```

---

## Implementation Status

### ✅ **Fully Documented** - Business and Technical Guide Complete

This comprehensive guide provides the business rationale, design philosophy, and implementation patterns for APEX Rule Categories.

### 🔄 **Partially Implemented** - Core Infrastructure Complete, Examples and Testing In Progress

**What's Working**:
- ✅ **YamlCategory Model** - Complete with all documented properties
- ✅ **Category Parsing** - YAML loader supports categories section
- ✅ **Metadata Inheritance Logic** - Implemented in YamlRuleFactory.createRuleWithMetadata()
- ✅ **Category Assignment** - Rules can reference categories by name
- ✅ **Priority Ordering** - Categories support priority-based execution

**What's In Progress**:
- 🔄 **Working YAML Examples** - Creating comprehensive demo configurations
- 🔄 **Integration Testing** - Verifying metadata inheritance end-to-end
- 🔄 **Demo Test Suite** - Practical examples in apex-demo module

**What's Planned**:
- ⏳ **Lifecycle Management** - Effective/expiration date enforcement
- ⏳ **Execution Control** - Category-level stop-on-failure and parallel execution
- ⏳ **Performance Optimisation** - Category-based rule filtering and caching
- ⏳ **Validation Framework** - Category name uniqueness and reference validation

### 🎯 **Current Focus: High Priority Implementation Tasks**

1. **Create Working YAML Examples** - Comprehensive category configurations in apex-demo
2. **Fix Metadata Inheritance** - Ensure YamlRuleFactory properly inherits category metadata
3. **Add Integration Tests** - End-to-end testing of category functionality

### 📋 **Implementation Roadmap**

**Phase 1: Foundation** (Current)
- ✅ Core data models and parsing
- 🔄 Basic metadata inheritance
- 🔄 Working examples and tests

**Phase 2: Enterprise Features** (Next)
- ⏳ Lifecycle management enforcement
- ⏳ Execution control implementation
- ⏳ Performance optimisation

**Phase 3: Advanced Governance** (Future)
- ⏳ Category validation and constraints
- ⏳ Audit and change tracking
- ⏳ Enterprise reporting and analytics

---

## Summary

APEX Rule Categories represent a **paradigm shift** from ad-hoc rule management to **enterprise-grade business rules governance**. This guide provides the business context, design rationale, and implementation patterns needed to successfully deploy categories in production environments.

### Key Takeaways

1. **Business-First Design** - Categories organise rules by business function, not technical implementation
2. **Governance Through Structure** - Mandatory metadata ensures accountability and auditability
3. **Inheritance for Efficiency** - Rules inherit enterprise context, reducing duplication and ensuring consistency
4. **Lifecycle Management** - Systematic rule retirement and compliance tracking
5. **Performance Optimisation** - Domain-specific execution strategies and priority management

### Next Steps

1. **Review Implementation Status** - Check current progress on high-priority tasks
2. **Plan Category Structure** - Design categories aligned with your business domains
3. **Implement Gradually** - Start with core categories and expand systematically
4. **Monitor and Optimise** - Use category metadata for governance reporting and performance tuning

**For Technical Implementation**: See the working examples in `apex-demo/src/test/java/dev/mars/apex/demo/categories/`

**For YAML Reference**: See Section 4.3 in `docs/APEX_YAML_REFERENCE.md`

**For Current Status**: Check the integration tests in `apex-core/src/test/java/dev/mars/apex/core/config/yaml/CategoryMetadataInheritanceIntegrationTest.java`

---

## Categories Across APEX Document Types

### Document Type Support Matrix

APEX supports multiple document types, but categories have different levels of support across them:

| Document Type | Categories Support | Category Usage | Inheritance Support |
|---------------|-------------------|----------------|-------------------|
| `rule-config` | ✅ **Full Support** | Rules and rule groups can reference categories | ✅ Full metadata inheritance |
| `enrichment` | ❌ **Not Supported** | Enrichments cannot reference categories | ❌ No inheritance |
| `rule-groups` | ✅ **Full Support** | Rule groups can reference categories (within rule-config) | ✅ Full metadata inheritance |
| `enrichment-groups` | ❌ **Not Supported** | Enrichment groups cannot reference categories | ❌ No inheritance |
| `dataset` | ❌ **Not Supported** | Datasets cannot reference categories | ❌ No inheritance |
| `scenario` | ❌ **Not Supported** | Scenarios cannot reference categories | ❌ No inheritance |
| `external-data-config` | ❌ **Not Supported** | External configs cannot reference categories | ❌ No inheritance |
| `pipeline-config` | ❌ **Not Supported** | Pipelines cannot reference categories | ❌ No inheritance |

### Why Categories Are Rule-Config Specific

**Design Rationale**: Categories were designed specifically for **business rules governance** within `rule-config` documents because:

1. **Business Logic Focus**: Rules represent business logic that requires governance, ownership, and lifecycle management
2. **Enterprise Metadata**: Rules need business domain, ownership, and audit trail information
3. **Execution Control**: Rules benefit from category-level execution strategies and priority management
4. **Compliance Requirements**: Business rules are subject to regulatory oversight and change management

**Other Document Types Have Different Purposes**:
- **Enrichments**: Technical data transformation, not business logic
- **Rule Groups**: Execution containers, not business logic themselves
- **Datasets**: Reference data, managed separately from business rules
- **Scenarios**: Workflow orchestration, not individual business logic

### Working with Categories in Mixed Configurations

#### Pattern 1: Rule-Config with Categories + Enrichments

```yaml
# File: business-rules.yaml
metadata:
  id: "business-validation-rules"
  type: "rule-config"

categories:
  - name: "customer-validation"
    business-domain: "Customer Management"
    business-owner: "customer-ops@bank.com"
    priority: 10

rules:
  - id: "customer-age-check"
    category: "customer-validation"  # ✅ Supported
    condition: "#age >= 18"
    message: "Customer must be 18 or older"

enrichments:
  - id: "customer-enrichment"
    type: "lookup-enrichment"
    # ❌ Cannot use: category: "customer-validation"
    # ❌ Cannot inherit metadata from categories
    condition: "#customerId != null"
    lookup-config:
      lookup-key: "#customerId"
      lookup-dataset:
        type: "inline"
        data:
          "12345": { "status": "ACTIVE", "tier": "PREMIUM" }
```

#### Pattern 2: Rule-Config with Categories + Rule Groups

```yaml
# File: grouped-business-rules.yaml
metadata:
  id: "grouped-validation-rules"
  type: "rule-config"

categories:
  - name: "validation-rules"
    business-domain: "Data Quality"
    business-owner: "data-team@bank.com"
    priority: 10

rules:
  - id: "required-field-check"
    category: "validation-rules"  # ✅ Rules can reference categories
    condition: "#name != null"
    message: "Name is required"

rule-groups:
  - id: "validation-group"
    name: "Data Validation Group"
    # ❌ Cannot use: category: "validation-rules"
    # ❌ Cannot inherit metadata from categories
    operator: "AND"
    rule-ids:
      - "required-field-check"  # References rule that has category
```

#### Pattern 3: Multi-File Configuration with Categories

```yaml
# File: categories-and-rules.yaml
metadata:
  id: "main-business-rules"
  type: "rule-config"

categories:
  - name: "trade-validation"
    business-domain: "Trading"
    business-owner: "trading-ops@bank.com"
    priority: 5

rules:
  - id: "trade-amount-check"
    category: "trade-validation"
    condition: "#amount > 0"
    message: "Trade amount must be positive"

# Reference external enrichment file
enrichment-refs:
  - "trade-enrichments.yaml"  # Separate enrichment document

---
# File: trade-enrichments.yaml
metadata:
  id: "trade-enrichments"
  type: "enrichment"  # Different document type

# ❌ Cannot define categories in enrichment documents
# categories:  # This would be invalid

enrichments:
  - id: "trade-value-calculation"
    type: "calculation-enrichment"
    # ❌ Cannot reference categories from other files
    condition: "#quantity != null && #price != null"
    calculation-config:
      expression: "#quantity * #price"
      result-field: "tradeValue"
```

### Alternative Organization Strategies

Since categories only work in `rule-config` documents, use these patterns for organizing other document types:

#### 1. **Naming Conventions**

```yaml
# Use consistent naming patterns across document types
# File: customer-validation-rules.yaml (rule-config with categories)
# File: customer-enrichment-rules.yaml (enrichment document)
# File: customer-reference-data.yaml (dataset document)
```

#### 2. **Metadata Tags**

```yaml
# Use tags for cross-document organization
metadata:
  id: "customer-enrichments"
  type: "enrichment"
  tags: ["customer", "validation", "data-quality"]  # Organizational tags

enrichments:
  - id: "customer-profile-lookup"
    type: "lookup-enrichment"
    # Use consistent naming and documentation
```

#### 3. **Directory Structure**

```
apex-configurations/
├── customer-management/
│   ├── rules/
│   │   └── customer-validation-rules.yaml    # rule-config with categories
│   ├── enrichments/
│   │   └── customer-enrichments.yaml         # enrichment documents
│   └── datasets/
│       └── customer-reference-data.yaml      # dataset documents
└── trading/
    ├── rules/
    │   └── trade-validation-rules.yaml       # rule-config with categories
    └── enrichments/
        └── trade-enrichments.yaml            # enrichment documents
```

### Future Enhancements

**Potential Future Support** (not currently implemented):

```yaml
# Hypothetical future enhancement - NOT currently supported
metadata:
  id: "future-enrichments"
  type: "enrichment"

# Future: Categories in enrichment documents
categories:
  - name: "data-enrichment"
    business-domain: "Data Management"
    business-owner: "data-team@bank.com"

enrichments:
  - id: "customer-lookup"
    category: "data-enrichment"  # Future: Category reference
    type: "lookup-enrichment"
    # Future: Inherit business-domain, business-owner from category
```

**Current Recommendation**: Use categories exclusively in `rule-config` documents and rely on naming conventions, tags, and directory structure for organizing other document types.
