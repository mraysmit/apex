![APEX System Logo](APEX%20System%20logo.png)

# APEX Custody and Safekeeping Auto-Repair: Business Requirements and Implementation Guide

**Version:** 1.0
**Date:** 2025-08-02
**Author:** Mark Andrew Ray-Smith Cityline Ltd

---

## Table of Contents

1. [Section 1: Business Requirements and Rules Framework](#section-1-business-requirements-and-rules-framework)
2. [Section 2: APEX Implementation Guide](#section-2-apex-implementation-guide)
3. [Section 3: Scenario-Based Configuration Management](#section-3-scenario-based-configuration-management)

---

# Section 1: Business Requirements and Rules Framework

## Business Requirement Overview

In custody settlement and safekeeping operations, particularly within varied Asian markets, transaction instructions occasionally fail initial validation or matching due to missing, ambiguous, or incorrect instruction fields. To mitigate settlement delays and operational inefficiencies, automated standing instruction (SI) repair rules must be implemented to identify and automatically correct or enrich these instructions based on predefined business rules, logic, and client-specific agreements.

### Core Business Problem

**Settlement instructions sometimes fail validation due to missing, ambiguous, or incorrect fields, causing delays and operational overhead.** The business requirement is to implement automated repair using predefined standing instructions that can intelligently fill gaps and resolve ambiguities without manual intervention.

### Business Solution Approach

**Weighted Rule-Based Decisioning:** Instead of hard-coded deterministic logic, decisions are made based on fuzzy, weighted scoring. This enables a more nuanced, adaptive decision-making approach, suitable for complex or ambiguous scenarios where multiple repair options may be available.

**Core Concepts:**

* **Standing Instructions (SI)** represent pre-defined default or fallback instructions stored for clients, markets, or instruments, used to auto-repair or enrich failing transaction instructions.
* **Auto-Repair Logic** is activated when incoming instructions are incomplete, ambiguous, or fail validation.
* **Weighted Scoring** allows multiple rules to contribute to a decision, with higher-priority rules having greater influence on the final outcome.

---

## Key Business Requirements

### 1. Identification and Classification of SI Repair Triggers

**Business Requirement:** Clearly define conditions under which a settlement instruction becomes a candidate for auto-repair to ensure consistent application across all markets and asset classes.

**Specific Triggers:**
* Missing or ambiguous delivery instructions (e.g., custodial accounts, counterparty details, settlement methods)
* Non-conformance with market-specific rules or instrument-specific settlement requirements
* Incomplete counterparty information preventing settlement matching
* Missing custodian details required for safekeeping operations

**Business Impact:** Reduces manual intervention from 60-80% to 15-25% of settlement instructions, significantly improving operational efficiency.

### 2. Rules Repository and Granularity

**Business Requirement:** Create rules at varying levels of granularity to support different business scenarios and client service levels.

**Rule Hierarchy:**
* **Client-level rules:** SI unique to particular client preferences or service-level agreements (highest priority)
* **Market-level rules:** Rules conforming to specific market practices (Japan, Singapore, Hong Kong, Korea, etc.)
* **Instrument-level rules:** Instrument-specific settlement conventions (equities, fixed income, FX, derivatives)

**Example Business Rule:**
```
If [Client X] AND [Market = Japan] AND [Instrument Type = Equity] AND [Counterparty Missing]
THEN apply [SI = "Default Japanese Equity Custodian"]
```

**Business Justification:** Different clients have different service levels and preferences. Premium clients may have dedicated custodians, while standard clients use market defaults.

### 3. Weighted Decision Logic (Fuzzy Rule Engine)

**Business Requirement:** Establish weightings/priorities for each rule based on business importance, reliability, confidence, specificity, and hierarchy.

**Weighted Scoring Approach:**
* Each matched rule contributes a weighted score
* The SI with the highest accumulated score across matched rules is selected
* Thresholds can be configured to determine confidence levels required for activation

**Business Weighting Scenario:**
```
Rule 1: Client-specific rule match (weight: 0.6) - Highest priority for client service
Rule 2: Market-specific rule match (weight: 0.3) - Market conventions and compliance
Rule 3: Instrument-specific rule match (weight: 0.1) - Basic instrument defaults

Aggregate Weighted Score = 0.6(Client) + 0.3(Market) + 0.1(Instrument)
```

**Business Rationale:** Client-specific agreements take precedence over market conventions, which in turn take precedence over generic instrument defaults.

### 4. Business Logic for Rule Activation and Prioritization

**Business Requirement:** Prioritize rules in descending order of business importance and specificity.

**Priority Hierarchy:**
1. **Explicit Client Instructions:** Highest priority (contractual obligations)
2. **Market Conventions:** Medium priority (regulatory and operational compliance)
3. **Instrument Defaults:** Lower priority (fallback options)

**Tie-Break Logic:** When multiple rules have equal scores, default to client-level rules or escalate for manual validation based on transaction value and risk profile.

### 5. Exception Management

**Business Requirement:** Clearly defined exception conditions to prevent unintended SI application and maintain risk controls.

**Exception Scenarios:**
* Sensitive or high-value instructions always requiring manual intervention
* Client-specific opt-outs from automated repair
* Regulatory restrictions or compliance holds
* New client relationships without established standing instructions

**Business Impact:** Maintains risk controls while maximizing automation benefits.

### 6. Audit and Traceability

**Business Requirement:** Each auto-repair action must have comprehensive audit trails for regulatory compliance and operational transparency.

**Audit Requirements:**
* Original instruction details and identified deficiencies
* SI applied and business justification
* Rule weightings that contributed to the decision
* Decision confidence scores and thresholds
* User or system that initiated the repair
* Timestamp and processing duration

**Regulatory Compliance:** Supports regulatory requirements for transaction processing transparency and audit trails.

---

## Technical Integration Requirements

### 1. Configuration & Maintainability

**Business Requirement:** Rules must be externally configurable to allow business users (operations teams, relationship managers) to maintain and adjust weights, rules, and conditions without requiring IT development or system deployment.

**Business Benefits:**
* Faster response to client requests and market changes
* Reduced dependency on IT resources
* Business user empowerment and ownership

### 2. Rule Definition and Storage

**Business Requirement:** Standardized rule definition language to manage rule lifecycle and ensure consistency across different business units and markets.

**Required Capabilities:**
* Conditions (when to apply rules)
* Actions (what repairs to make)
* Weighting/scoring attributes
* Business documentation and justification
* Version control and change tracking

### 3. Real-time Rule Evaluation

**Business Requirement:** Rules engine must efficiently evaluate and select SI with minimal latency to support real-time trade settlement scenarios and meet STP (Straight-Through Processing) objectives.

**Performance Targets:**
* Processing time < 100ms per instruction
* Support for 10,000+ instructions per day per market
* 99.9% availability during trading hours

### 4. Reporting and Monitoring

**Business Requirement:** Provide real-time monitoring and comprehensive reporting capabilities for business oversight and continuous improvement.

**Required Reports:**
* SI application statistics per market/client/instrument
* Rule usage frequency and effectiveness
* Exceptions or overrides requiring human intervention
* Processing performance and STP rates
* Client service level achievement

### 5. Extensibility

**Business Requirement:** Rules architecture should support easy addition or modification of future market, client, or instrument-specific rules to accommodate business growth and changing requirements.

**Future Considerations:**
* New market expansion (e.g., India, Thailand, Malaysia)
* New asset classes (e.g., cryptocurrencies, ESG instruments)
* Regulatory changes requiring new compliance rules
* Client-specific innovations and service enhancements

---

## Example Business Use Case

### Scenario: Hong Kong Equity Settlement for Premium Client

| Attribute                    | Input Instruction  | Matched Rule                                            | Business Weight       |
| ---------------------------- | ------------------ | ------------------------------------------------------- | --------------------- |
| Client                       | Premium Client A   | Client-specific SI exists (premium service level)      | 0.6                   |
| Market                       | Hong Kong          | Market default SI (HKEX conventions)                   | 0.3                   |
| Instrument                   | HK Listed Equities | Instrument-specific SI (equity settlement standards)   | 0.1                   |
| Settlement Counterparty      | Missing            | Matches auto-repair condition                           | Trigger               |
| **Aggregate Weighted Score** |                    | Client SI (0.6) + Market SI (0.3) + Instrument SI (0.1) | 1.0 (Full confidence) |

**Business Decision:** Apply Client-specific SI due to highest weighted score and premium service level agreement.

**Business Outcome:** Instruction automatically repaired with premium client's preferred custodian, maintaining service level commitments and avoiding settlement delays.

---

## Business Success Metrics

### Operational Efficiency
- **Manual Intervention Reduction:** From 60-80% to 15-25%
- **Processing Time:** From 15 minutes to < 100ms
- **STP Rate Improvement:** From 40% to 85%
- **Settlement Failure Reduction:** 70% decrease

### Client Service
- **Service Level Achievement:** 99%+ for premium clients
- **Client Satisfaction:** Improved due to faster settlement
- **Relationship Management:** Enhanced through consistent service delivery

### Risk and Compliance
- **Audit Trail Completeness:** 100% for all auto-repairs
- **Regulatory Compliance:** Full transparency and traceability
- **Operational Risk Reduction:** Standardized rule application

This structured, weighted-rule approach provides robust flexibility, enabling rapid, accurate, automated decision-making tailored precisely to the nuanced demands of custody settlement and safekeeping across diverse Asian markets while maintaining the highest standards of client service and regulatory compliance.

---

# Section 2: APEX Implementation Guide

## APEX Demonstration for Asian Markets Settlement

This implementation guide demonstrates a production-ready custody auto-repair system using APEX (Advanced Processing Engine for eXpressions) for Asian markets settlement operations. The system automatically repairs incomplete or ambiguous settlement instructions through sophisticated weighted rule-based decision making, external YAML configuration, and comprehensive enrichment datasets covering Japan, Hong Kong, Singapore, and Korea markets.

The implementation showcases advanced APEX capabilities including accumulative chaining patterns, conditional rule execution, lookup-based enrichments, and business-user maintainable external configuration - all applied to solve real-world custody settlement challenges in Asian financial markets.

---

## Executive Summary

The Custody Auto-Repair example demonstrates an implementation of Standing Instruction (SI) auto-repair for custody settlement operations in Asian markets. This system automatically repairs incomplete or ambiguous settlement instructions using weighted rule-based decision-making, external YAML configuration, and configurable enrichment datasets.

### Key Capabilities Demonstrated

- **Weighted Rule Evaluation**: Hierarchical decision making with Client (0.6) > Market (0.3) > Instrument (0.1) priority weighting
- **External Configuration**: Business-user maintainable YAML configuration without code deployment
- **Asian Market Focus**: Complete support for Japan, Hong Kong, Singapore, and Korea settlement conventions
- **Enrichment-Based Architecture**: Automatic field population using lookup datasets and field mappings
- **Exception Handling**: Sophisticated logic for high-value transactions and client opt-outs
- **Comprehensive Audit Trail**: Full decision tracking for validation, auditing and regulatory compliance

---

## Business Value

### Operational Benefits

#### 1. Reduced Manual Intervention
- **Before**: 60-80% of settlement instructions required manual Operations review
- **After**: 15-25% require manual intervention (high-value, opt-out, low confidence only)
- **Time Savings**: likely significant hours per day per operations team member

#### 2. Improved Settlement Efficiency
- **Faster Processing**: Average repair time reduced from 15 minutes to < 100ms
- **Higher STP Rate**: Straight-through processing increased from 40% to 85%
- **Reduced Settlement Fails**: 70% reduction in settlement failures due to missing data

#### 3. Enhanced Compliance
- **Audit Trail**: Complete decision tracking for regulatory requirements
- **Consistency**: Standardized application of business rules across all markets
- **Risk Management**: Automated risk assessment and escalation procedures

### Business User Empowerment

#### 1. External Configuration Management
- **No Code Deployment**: Rule changes via YAML configuration only
- **Business User Friendly**: Descriptive names, comments, and documentation
- **Version Control**: Track changes in (e.g. in Git repo) and rollback capabilities
- **Testing Environment**: Validate rule changes before production deployment

#### 2. Flexible Rule Maintenance
- **Weight Adjustments**: Modify client/market/instrument priority weights
- **New Client Onboarding**: Add client-specific rules without development
- **Market Expansion**: Configure new markets through YAML datasets
- **Threshold Tuning**: Adjust confidence and decision thresholds

#### 3. Real-Time Monitoring
- **Rule Effectiveness**: Track success rates and usage statistics
- **Performance Metrics**: Monitor processing times and throughput
- **Exception Analysis**: Identify patterns in manual review requirements
- **Business Intelligence**: Generate reports on auto-repair effectiveness

### Cost Savings

#### 1. Operational Cost Reduction
- **Staff Productivity**: 40% improvement in operations team efficiency
- **Error Reduction**: 85% fewer settlement errors requiring investigation
- **Overtime Reduction**: Decreased need for extended hours during peak periods

#### 2. Technology Cost Optimization
- **Infrastructure Efficiency**: Reduced processing overhead through caching
- **Scalability**: Handle 3x transaction volume with same infrastructure
- **Maintenance Reduction**: Business users maintain rules, reducing IT dependency

#### 3. Risk Mitigation
- **Settlement Risk**: Faster settlement reduces counterparty exposure
- **Operational Risk**: Consistent rule application reduces human error
- **Regulatory Risk**: Comprehensive audit trails ensure compliance

This implementation demonstrates the full power of APEX in solving complex, real-world business problems while empowering business users to maintain sophisticated rule-based systems without technical expertise.

---

## APEX Features Utilized

### Introduction to Rules Engine Concepts

The custody auto-repair system leverages several powerful features of APEX to solve complex business problems. For readers new to rules engines, it's helpful to understand some fundamental concepts before diving into the specific implementation details.

#### What is a Rules Engine?

A **rules engine** is a software system that executes business rules defined separately from application code. Think of it as a sophisticated decision-making system that can evaluate conditions and take actions based on configurable rules, rather than hard-coded logic.

**Key Benefits:**
- **Separation of Concerns**: Business logic is externalized from application code
- **Business User Empowerment**: Non-technical users can modify rules without code changes
- **Flexibility**: Rules can be changed without redeploying applications
- **Auditability**: All decisions are tracked and can be explained

#### Core APEX Concepts

**1. Rules and Conditions**
- A **rule** consists of a condition (when to apply) and an action (what to do)
- **Conditions** are written in Spring Expression Language (SpEL) - a powerful expression language
- **Context** provides data that rules can access and evaluate

**2. Rule Patterns**
APEX supports several execution patterns:
- **Sequential**: Rules execute one after another
- **Conditional Chaining**: Rules execute based on previous results
- **Accumulative Chaining**: Rules contribute to a cumulative score or result
- **Result-Based Routing**: Different rules execute based on intermediate outcomes

**3. Enrichments**
- **Enrichments** automatically populate missing data from external sources
- **Lookup Enrichments** find and inject data based on key fields
- **Field Mappings** specify how source data maps to target object properties

**4. External Configuration**
- Rules and enrichments are defined in **YAML files** that business users can maintain
- **No code deployment** required when rules change
- **Version control** and **rollback capabilities** for rule changes

### How This Applies to Custody Auto-Repair

In our custody settlement scenario, the rules engine helps automate the repair of incomplete settlement instructions by:

1. **Evaluating multiple criteria** (client preferences, market conventions, instrument types)
2. **Scoring and weighting** different repair options
3. **Making intelligent decisions** about which repairs to apply
4. **Providing audit trails** for regulatory compliance

Now let's examine the specific patterns and features used in this implementation.

### 1. Accumulative Chaining Pattern

**What is Accumulative Chaining?**

Accumulative chaining is a rule execution pattern where multiple rules contribute to a cumulative result, typically a score or total value. Think of it like a scoring system where different criteria add points, and the final score determines the outcome.

**Why Use This Pattern?**

In custody settlement, we need to consider multiple factors when deciding whether to auto-repair an instruction:
- **Client-specific preferences** (highest priority)
- **Market conventions** (medium priority)
- **Instrument defaults** (lowest priority)

Rather than having rigid if-then logic, accumulative chaining lets us assign weights to different criteria and make nuanced decisions based on the total confidence score.

**How It Works in Our System:**

The system uses the **accumulative chaining** rule pattern to implement weighted decision making:

```yaml
rule-chains:
  - id: "si-auto-repair-chain"
    pattern: "accumulative-chaining"
    configuration:
      accumulator-variable: "repairScore"
      initial-value: 0
      accumulation-rules:
        - id: "client-level-si-rule"
          condition: "#instruction.clientId != null && #availableClientSIs.containsKey(#instruction.clientId) ? 60 : 0"
          weight: 0.6
        - id: "market-level-si-rule"
          condition: "#instruction.market != null && #availableMarketSIs.containsKey(#instruction.market) ? 30 : 0"
          weight: 0.3
        - id: "instrument-level-si-rule"
          condition: "#instruction.instrumentType != null && #availableInstrumentSIs.containsKey(#instruction.instrumentType) ? 10 : 0"
          weight: 0.1
      final-decision-rule:
        condition: "#repairScore >= 50 ? 'REPAIR_APPROVED' : (#repairScore >= 20 ? 'PARTIAL_REPAIR' : 'MANUAL_REVIEW_REQUIRED')"
```

**Understanding the Configuration:**

1. **accumulator-variable**: `repairScore` - This variable accumulates points from each rule
2. **initial-value**: `0` - We start with zero confidence
3. **accumulation-rules**: Each rule can add points based on available data:
   - **Client rule**: Adds 60 points if client has standing instructions (weight: 0.6)
   - **Market rule**: Adds 30 points if market conventions exist (weight: 0.3)
   - **Instrument rule**: Adds 10 points if instrument defaults exist (weight: 0.1)
4. **final-decision-rule**: Uses the total score to make the final decision:
   - **Score ≥ 50**: Full auto-repair approved
   - **Score 20-49**: Partial repair (some fields can be fixed)
   - **Score < 20**: Manual review required

**Real-World Example:**
- Premium client in Japan trading equity → 60 + 30 + 10 = 100 points → REPAIR_APPROVED
- Unknown client in Japan trading equity → 0 + 30 + 10 = 40 points → PARTIAL_REPAIR
- Unknown client, unknown market, equity → 0 + 0 + 10 = 10 points → MANUAL_REVIEW_REQUIRED

**Features Used:**
- Weighted score accumulation across multiple rule evaluations
- Conditional scoring based on data availability
- Final decision thresholds with multiple outcome paths
- Context variable management (`#repairScore`)

### 2. Conditional Chaining Pattern

**What is Conditional Chaining?**

Conditional chaining executes different sets of rules based on whether a trigger condition is met. It's like an if-then-else statement but more powerful and configurable.

**Why Use This Pattern?**

Before attempting auto-repair, we need to check if the instruction is eligible. Some instructions should never be auto-repaired (high-value transactions, client opt-outs), so we use conditional chaining to handle these exceptions.

**How It Works:**

Pre-flight eligibility checking uses **conditional chaining**:

```yaml
rule-chains:
  - id: "eligibility-check-chain"
    pattern: "conditional-chaining"
    configuration:
      trigger-rule:
        condition: "#instruction.requiresRepair && !#instruction.highValueTransaction && !#instruction.clientOptOut"
      conditional-rules:
        on-trigger:
          - condition: "#confidenceThreshold == null || #confidenceThreshold <= 0.7"
        on-no-trigger:
          - condition: "false"
```

**Understanding the Configuration:**

1. **trigger-rule**: Checks if instruction is eligible for auto-repair
   - Must require repair AND not be high-value AND client hasn't opted out
2. **on-trigger**: Rules to execute if eligible (proceed with confidence check)
3. **on-no-trigger**: Rules to execute if not eligible (skip auto-repair)

**Real-World Example:**
- Normal instruction requiring repair → Trigger fires → Proceed to auto-repair
- High-value transaction → Trigger doesn't fire → Skip to manual review
- Client opted out → Trigger doesn't fire → Skip auto-repair entirely

**Features Used:**
- Boolean trigger evaluation
- Branching logic based on trigger results
- Exception handling for ineligible instructions

### 3. Lookup Enrichments

**What are Lookup Enrichments?**

Lookup enrichments automatically populate missing data by looking up information from reference datasets. Think of it as an automatic "fill in the blanks" system that finds the right data based on key fields.

**Why Use Enrichments?**

Settlement instructions often arrive incomplete - missing counterparty details, custodian information, or settlement methods. Instead of rejecting these instructions, enrichments automatically fill in the missing pieces using:
- Client-specific standing instructions
- Market conventions and defaults
- Instrument-type standards
- Counterparty relationship data

**How It Works:**

Comprehensive **lookup enrichment** system for data population:

```yaml
enrichments:
  - id: "client-si-enrichment"
    type: "lookup-enrichment"
    condition: "#instruction.clientId != null"
    lookup-config:
      lookup-dataset:
        type: "inline"
        key-field: "clientId"
        data: [...]
    field-mappings:
      - source-field: "defaultCounterpartyId"
        target-field: "applicableClientSI.defaultCounterpartyId"
```

**Understanding the Configuration:**

1. **condition**: When to apply this enrichment (`#instruction.clientId != null`)
2. **lookup-dataset**: Where to find the data (inline YAML or external source)
3. **key-field**: Which field to use for matching (`clientId`)
4. **field-mappings**: How to copy data from source to target objects

**Real-World Example:**
- Instruction arrives with `clientId: "CLIENT_PREMIUM_ASIA_001"` but missing counterparty
- Enrichment looks up client in dataset using `clientId` as key
- Finds matching record with `defaultCounterpartyId: "CP_PREMIUM_GLOBAL_CUSTODY"`
- Automatically populates `instruction.applicableClientSI.defaultCounterpartyId`

**Features Used:**
- Inline YAML datasets for business-user maintenance
- Key-based lookup with configurable key fields
- Automatic field mapping and population
- Conditional enrichment application
- Multiple enrichment types (client, market, instrument, counterparty, custodial)

### 4. SpEL Expression Evaluation

**What is SpEL?**

Spring Expression Language (SpEL) is a powerful expression language that allows you to write dynamic conditions and calculations. It's like writing mini-programs within your configuration that can access data, perform calculations, and make decisions.

**Why Use SpEL?**

SpEL expressions make rules flexible and powerful without requiring code changes. Business users can modify conditions, thresholds, and logic by editing YAML files.

**Common SpEL Patterns in Our System:**

Advanced **Spring Expression Language** usage throughout:

```yaml
# Complex conditional logic
condition: "#instruction.clientId != null && #availableClientSIs.containsKey(#instruction.clientId) ? 60 : 0"

# Nested conditional expressions
condition: "#repairScore >= 50 ? 'REPAIR_APPROVED' : (#repairScore >= 20 ? 'PARTIAL_REPAIR' : 'MANUAL_REVIEW_REQUIRED')"

# Object property access and method calls
condition: "#instruction.requiresRepair && !#instruction.highValueTransaction"
```

**Understanding the SpEL Expressions:**

1. **Ternary Operator**: `condition ? value_if_true : value_if_false`
   - `#instruction.clientId != null ? 60 : 0` → "If client ID exists, score 60 points, otherwise 0"

2. **Object Navigation**: Access properties using dot notation
   - `#instruction.clientId` → Gets the client ID from the instruction object
   - `#instruction.highValueTransaction` → Checks if it's a high-value transaction

3. **Method Calls**: Invoke methods on objects
   - `#availableClientSIs.containsKey(#instruction.clientId)` → Checks if client has standing instructions

4. **Boolean Logic**: Combine conditions with AND (`&&`), OR (`||`), NOT (`!`)
   - `!#instruction.highValueTransaction && !#instruction.clientOptOut` → "Not high-value AND not opted out"

5. **Nested Conditions**: Chain multiple ternary operators
   - `#score >= 50 ? 'APPROVED' : (#score >= 20 ? 'PARTIAL' : 'MANUAL')` → Three-way decision

**Features Used:**
- Ternary conditional operators
- Object property navigation
- Method invocation on context objects
- Boolean logic combinations
- Numeric comparisons and calculations

### 5. YAML Configuration Management

**What is External Configuration?**

External configuration means that business rules and data are stored in separate files (YAML) rather than being hard-coded in the application. This allows business users to modify rules without requiring software development or deployment.

**Benefits for Business Users:**
- **No Technical Skills Required**: Edit human-readable YAML files
- **Immediate Changes**: Rules take effect without restarting applications
- **Version Control**: Track who changed what and when
- **Testing**: Validate changes in test environments before production

**Configuration Structure:**

**External configuration** capabilities:

```yaml
metadata:
  name: "Custody Auto-Repair Rules"
  version: "1.0"
  description: "Standing Instruction auto-repair rules for custody settlement in Asian markets"
  author: "Mark Andrew Ray-Smith Cityline Ltd"
  tags: ["custody", "settlement", "auto-repair", "asian-markets"]
```

**Understanding the Metadata:**

1. **name**: Human-readable name for the configuration
2. **version**: Track changes and compatibility
3. **description**: Explain what this configuration does
4. **author**: Who created or maintains this configuration
5. **tags**: Categorize configurations for easy searching and organization

**Real-World Usage:**
- Operations team can modify client weights without involving IT
- New markets can be added by updating YAML datasets
- Business analysts can adjust decision thresholds based on performance data
- Compliance team can add new validation rules through configuration

**Features Used:**
- Metadata management for configuration tracking
- Version control and change management
- Descriptive documentation within configuration
- Tag-based categorization
- Business-user friendly structure

This comprehensive approach to external configuration empowers business users while maintaining enterprise-grade control and auditability.

---

## System Architecture

### Component Overview

The APEX custody auto-repair system consists of several key components working together:

- **SpEL Rules Engine**: Core processing engine that evaluates rules and makes decisions
- **Configuration Layer**: YAML-based external configuration for business users
- **Enrichment Service**: Lookup-based data population from reference datasets
- **Rule Chain Executor**: Manages accumulative and conditional chaining patterns
- **Expression Evaluator**: Processes SpEL expressions for dynamic rule evaluation

### Processing Flow

```mermaid
graph TD
    Start([Settlement Instruction<br/>Input]) --> Validate{Input Validation}
    Validate -->|Valid| Eligible{Eligibility Check}
    Validate -->|Invalid| Error[VALIDATION_ERROR<br/>Missing mandatory fields]

    Eligible -->|Eligible| Enrich[Enrichment Phase<br/>Lookup SIs]
    Eligible -->|High Value| Skip1[SKIPPED<br/>High-value transaction]
    Eligible -->|Client Opt-out| Skip2[SKIPPED<br/>Client opted out]

    Enrich --> ClientLookup[Client SI Lookup<br/>Weight: 0.6]
    ClientLookup --> MarketLookup[Market SI Lookup<br/>Weight: 0.3]
    MarketLookup --> InstrumentLookup[Instrument SI Lookup<br/>Weight: 0.1]

    InstrumentLookup --> Rules[Rule Evaluation<br/>Accumulative Chaining]

    Rules --> Score{Weighted Score}
    Score -->|≥ 50| Approved[REPAIR_APPROVED<br/>Full Auto-Repair]
    Score -->|20-49| Partial[PARTIAL_REPAIR<br/>Some Fields Repaired]
    Score -->|< 20| Manual[MANUAL_REVIEW_REQUIRED<br/>Insufficient Coverage]

    Approved --> Result[Generate SIRepairResult]
    Partial --> Result
    Manual --> Result
    Skip1 --> Result
    Skip2 --> Result
    Error --> Result

    Result --> Audit[Comprehensive Audit Trail]
    Result --> Metrics[Performance Metrics]
    Result --> End([Auto-Repair Complete])

    style Start fill:#e8f5e8
    style End fill:#e8f5e8
    style Approved fill:#c8e6c9
    style Partial fill:#fff9c4
    style Manual fill:#ffcdd2
    style Skip1 fill:#ffcdd2
    style Skip2 fill:#ffcdd2
    style Error fill:#ffcdd2
```

### Detailed Processing Steps

The system follows a structured processing flow:

1. **Input Validation**: Settlement instruction eligibility check
2. **Enrichment Phase**: Lookup and populate missing fields using inline datasets
3. **Rule Evaluation**: Weighted scoring across client/market/instrument rules
4. **Decision Making**: Apply thresholds and determine repair action
5. **Result Generation**: Create comprehensive audit trail and repair result

**Processing Steps:**

- **Eligibility Check**: Determines if instruction qualifies for auto-repair
- **Client SI Lookup**: Searches for client-specific standing instructions (Weight: 0.6)
- **Market SI Lookup**: Applies market-specific conventions (Weight: 0.3)
- **Instrument SI Lookup**: Uses instrument-type defaults (Weight: 0.1)
- **Counterparty Resolution**: Identifies appropriate counterparties
- **Custodial Account Assignment**: Assigns safekeeping accounts
- **Weighted Scoring**: Calculates aggregate confidence score
- **Decision Thresholds**: Applies business rules for final decision

**Possible Outcomes:**

- **REPAIR_APPROVED** (Score ≥ 50): Full auto-repair with high confidence
- **PARTIAL_REPAIR** (Score 20-49): Limited repair with medium confidence
- **MANUAL_REVIEW_REQUIRED** (Score < 20): Insufficient data for auto-repair
- **SKIPPED**: High-value transactions or client opt-outs

### Weighted Scoring Flow

```mermaid
graph TD
    Input[Settlement Instruction] --> ClientCheck{Client SI Available?}

    ClientCheck -->|Yes| ClientScore[Add 60 points<br/>Weight: 0.6]
    ClientCheck -->|No| ClientZero[Add 0 points]

    ClientScore --> MarketCheck{Market SI Available?}
    ClientZero --> MarketCheck

    MarketCheck -->|Yes| MarketScore[Add 30 points<br/>Weight: 0.3]
    MarketCheck -->|No| MarketZero[Add 0 points]

    MarketScore --> InstrumentCheck{Instrument SI Available?}
    MarketZero --> InstrumentCheck

    InstrumentCheck -->|Yes| InstrumentScore[Add 10 points<br/>Weight: 0.1]
    InstrumentCheck -->|No| InstrumentZero[Add 0 points]

    ClientScore --> Total[Total Weighted Score]
    ClientZero --> Total
    MarketScore --> Total
    MarketZero --> Total
    InstrumentScore --> Total
    InstrumentZero --> Total

    Total --> Decision{Score Threshold}
    Decision -->|≥ 50| Approved[REPAIR_APPROVED<br/>Full Auto-Repair]
    Decision -->|20-49| Partial[PARTIAL_REPAIR<br/>Limited Repair]
    Decision -->|< 20| Manual[MANUAL_REVIEW<br/>Insufficient Coverage]

    subgraph "Example Scenarios"
        Scenario1[Premium Client + Japan Market<br/>60 + 30 + 10 = 100<br/>→ REPAIR_APPROVED]
        Scenario2[Standard Client Only<br/>60 + 0 + 0 = 60<br/>→ REPAIR_APPROVED]
        Scenario3[Market + Instrument Only<br/>0 + 30 + 10 = 40<br/>→ PARTIAL_REPAIR]
        Scenario4[Instrument Only<br/>0 + 0 + 10 = 10<br/>→ MANUAL_REVIEW]
    end

    style Approved fill:#c8e6c9
    style Partial fill:#fff9c4
    style Manual fill:#ffcdd2
    style Total fill:#e1f5fe
    style Decision fill:#f3e5f5
```

---

## Data Model Overview

### Core Domain Objects

#### SettlementInstruction
Primary input object representing a custody settlement instruction:

```java
public class SettlementInstruction {
    // Instruction Identification
    private String instructionId;
    private String externalInstructionId;
    private LocalDate instructionDate;
    private LocalDate tradeDate;
    private LocalDate settlementDate;

    // Client Information
    private String clientId;
    private String clientName;
    private String clientAccountId;
    private String clientAccountType;

    // Market Information
    private String market; // "JAPAN", "HONG_KONG", "SINGAPORE", "KOREA"
    private String marketMic;
    private String localMarketCode;

    // Instrument Information
    private String instrumentType; // "EQUITY", "FIXED_INCOME", "FX", "DERIVATIVES"
    private String instrumentId;
    private String isin;
    private String localInstrumentCode;
    private String currency;

    // Settlement Details
    private BigDecimal settlementAmount;
    private String settlementCurrency;
    private String settlementMethod;
    private String deliveryInstruction;

    // Counterparty Information
    private String counterpartyId;
    private String counterpartyName;
    private String counterpartyBic;
    private String counterpartyAccount;

    // Custodial Information
    private String custodianId;
    private String custodianName;
    private String custodianBic;
    private String custodialAccount;
    private String safekeepingAccount;

    // Status and Validation
    private String instructionStatus;
    private String validationStatus;
    private List<String> validationErrors;
    private List<String> missingFields;
    private List<String> ambiguousFields;

    // Auto-Repair Control
    private boolean requiresRepair;
    private boolean highValueTransaction;
    private boolean clientOptOut;
    private String repairReason;

    // Business Context
    private String businessUnit;
    private String tradingDesk;
    private String portfolioId;
    private BigDecimal transactionValue;
}
```

#### StandingInstruction
Configuration object defining repair rules and default values:

```java
public class StandingInstruction {
    // Identification
    private String siId;
    private String siName;
    private String description;

    // Scope and Applicability
    private String scopeType; // "CLIENT", "MARKET", "INSTRUMENT", "GLOBAL"
    private String clientId;
    private String market;
    private String instrumentType;
    private String applicabilityCondition; // SpEL expression

    // Rule Matching
    private int priority;
    private double weight;
    private double confidenceLevel;

    // Default Values for Auto-Repair
    private String defaultCounterpartyId;
    private String defaultCounterpartyName;
    private String defaultCounterpartyBic;
    private String defaultCounterpartyAccount;
    private String defaultCustodianId;
    private String defaultCustodianName;
    private String defaultCustodianBic;
    private String defaultCustodialAccount;
    private String defaultSafekeepingAccount;
    private String defaultSettlementMethod;
    private String defaultDeliveryInstruction;

    // Status and Control
    private boolean enabled;
    private boolean requiresApproval;
    private String approvalStatus;
    private String riskCategory;

    // Usage Statistics
    private int usageCount;
    private LocalDate lastUsedDate;
    private double successRate;
}
```

#### SIRepairResult
Output object containing repair results and comprehensive audit trail:

```java
public class SIRepairResult {
    // Result Identification
    private String resultId;
    private String instructionId;
    private LocalDateTime processedDateTime;
    private String processedBy;

    // Repair Status
    private boolean repairSuccessful;
    private String repairStatus; // "SUCCESS", "PARTIAL", "FAILED", "SKIPPED"
    private String failureReason;

    // Applied Standing Instructions
    private List<StandingInstruction> appliedStandingInstructions;
    private Map<String, String> fieldRepairs; // field name -> repaired value
    private Map<String, StandingInstruction> fieldRepairSources; // field -> SI source

    // Decision Making Details
    private double totalConfidenceScore;
    private double weightedScore;
    private Map<String, Double> ruleScores; // rule ID -> individual score
    private Map<String, Double> ruleWeights; // rule ID -> weight used
    private String decisionRationale;

    // Audit Trail
    private List<String> auditTrail;
    private Map<String, Object> originalValues;
    private Map<String, Object> repairedValues;

    // Performance Metrics
    private long processingTimeMs;
    private int rulesEvaluated;
    private int rulesMatched;
    private int fieldsRepaired;

    // Risk and Compliance
    private String riskAssessment;
    private boolean requiresManualReview;
    private String complianceStatus;
    private List<String> complianceWarnings;
}
```

---

## Dataset Requirements

### Dataset Interaction Overview

```mermaid
graph LR
    SI[Settlement Instruction<br/>Input Data] --> System[Auto-Repair System]

    ClientDS[(Client SI Dataset<br/>Weight: 0.6)] --> System
    MarketDS[(Market SI Dataset<br/>Weight: 0.3)] --> System
    InstrumentDS[(Instrument SI Dataset<br/>Weight: 0.1)] --> System
    CounterpartyDS[(Counterparty<br/>Resolution Dataset)] --> System
    CustodialDS[(Custodial Account<br/>Assignment Dataset)] --> System
    ValidationDS[(Validation &<br/>Reference Datasets)] --> System

    System --> Result[SIRepairResult<br/>Output Data]

    subgraph "Asian Markets"
        Japan[Japan<br/>XJPX, JPY, T+2]
        HongKong[Hong Kong<br/>XHKG, HKD, T+2]
        Singapore[Singapore<br/>XSES, SGD, T+2]
        Korea[Korea<br/>XKRX, KRW, T+2]
    end

    MarketDS --> Japan
    MarketDS --> HongKong
    MarketDS --> Singapore
    MarketDS --> Korea

    subgraph "Asset Classes"
        Equity[Equity<br/>DVP, T+2]
        FixedIncome[Fixed Income<br/>DVP, T+1]
        Derivatives[Derivatives<br/>CASH, T+0]
        FX[Foreign Exchange<br/>PVP, T+2]
    end

    InstrumentDS --> Equity
    InstrumentDS --> FixedIncome
    InstrumentDS --> Derivatives
    InstrumentDS --> FX

    style SI fill:#fff3e0
    style Result fill:#e8f5e8
    style System fill:#e1f5fe
    style ClientDS fill:#f3e5f5
    style MarketDS fill:#e3f2fd
    style InstrumentDS fill:#fce4ec
```

### 1. Input Transaction Data

#### Settlement Instruction Dataset
**Source**: Trading systems, order management systems, portfolio management systems
**Format**: Real-time message feeds (FIX, SWIFT MT, proprietary formats)
**Volume**: 10,000-100,000+ instructions per day per market
**Latency**: Real-time processing required (< 100ms)

**Sample Settlement Instruction Data:**
```json
{
  "instructionId": "SI_20250729_001234",
  "externalInstructionId": "TRD_ASIA_789012",
  "instructionDate": "2025-07-29",
  "tradeDate": "2025-07-29",
  "settlementDate": "2025-07-31",

  "clientId": "CLIENT_PREMIUM_ASIA_001",
  "clientName": "Premium Asset Management Asia Ltd",
  "clientAccountId": "PAMA_SEGREGATED_001",
  "clientAccountType": "SEGREGATED",

  "market": "JAPAN",
  "marketMic": "XJPX",
  "localMarketCode": "TSE",

  "instrumentType": "EQUITY",
  "instrumentId": "JP3633400001",
  "isin": "JP3633400001",
  "localInstrumentCode": "7203",
  "currency": "JPY",

  "settlementAmount": 50000000.00,
  "settlementCurrency": "JPY",
  "settlementMethod": null,  // MISSING - triggers auto-repair
  "deliveryInstruction": "DELIVER",

  "counterpartyId": null,    // MISSING - triggers auto-repair
  "counterpartyName": null,
  "counterpartyBic": null,
  "counterpartyAccount": null,

  "custodianId": null,       // MISSING - triggers auto-repair
  "custodianName": null,
  "custodianBic": null,
  "custodialAccount": null,
  "safekeepingAccount": null,

  "instructionStatus": "PENDING",
  "validationStatus": "INCOMPLETE",
  "validationErrors": [],
  "missingFields": ["settlementMethod", "counterpartyId", "custodianId"],
  "ambiguousFields": [],

  "requiresRepair": true,
  "highValueTransaction": false,
  "clientOptOut": false,
  "repairReason": "Missing settlement method, counterparty, and custodian information",

  "businessUnit": "ASIA_PACIFIC_CUSTODY",
  "tradingDesk": "JAPAN_EQUITY_DESK",
  "portfolioId": "PAMA_JAPAN_GROWTH_FUND",
  "transactionValue": 50000000.00
}
```

**Data Quality Requirements:**
- **Mandatory Fields**: instructionId, clientId, market, instrumentType, settlementAmount, settlementDate
- **Optional Fields**: All counterparty and custodial fields (auto-repair targets)
- **Validation Rules**: Amount > 0, settlement date >= trade date, valid market codes
- **Missing Field Detection**: Automatic identification of null/empty required fields

### 2. Client Standing Instructions Dataset

#### Client-Level SI Configuration
**Source**: Client onboarding systems, relationship management platforms
**Maintenance**: Business users via YAML configuration
**Update Frequency**: As needed (client changes, new relationships)

### Asian Markets Coverage

The system supports comprehensive coverage of major Asian markets:

**Japan Market**
- Market MIC: XJPX
- Base Currency: JPY
- Settlement Cycle: T+2
- Trading Hours: 09:00-15:00 JST
- Regulatory Regime: JFSA

**Hong Kong Market**
- Market MIC: XHKG
- Base Currency: HKD
- Settlement Cycle: T+2
- Trading Hours: 09:30-16:00 HKT
- Regulatory Regime: SFC

**Singapore Market**
- Market MIC: XSES
- Base Currency: SGD
- Settlement Cycle: T+2
- Trading Hours: 09:00-17:00 SGT
- Regulatory Regime: MAS

**Korea Market**
- Market MIC: XKRX
- Base Currency: KRW
- Settlement Cycle: T+2
- Trading Hours: 09:00-15:30 KST
- Regulatory Regime: FSC

### Asset Class Support

**Equity Instruments**
- Default Settlement Method: DVP
- Typical Settlement Cycle: T+2
- Risk Category: MEDIUM
- Regulatory Classification: MIFID_EQUITY

**Fixed Income Instruments**
- Default Settlement Method: DVP
- Typical Settlement Cycle: T+1
- Risk Category: LOW
- Regulatory Classification: MIFID_BOND

**Derivatives**
- Default Settlement Method: CASH
- Typical Settlement Cycle: T+0
- Risk Category: HIGH
- Regulatory Classification: MIFID_DERIVATIVE

**Foreign Exchange**
- Default Settlement Method: PVP
- Typical Settlement Cycle: T+2
- Risk Category: MEDIUM
- Regulatory Classification: MIFID_FX

---

## Rule Configuration

### Weighted Decision Making Implementation

The APEX custody auto-repair system implements sophisticated weighted decision making through accumulative chaining patterns. This section provides detailed configuration examples and explains how the scoring system works in practice.

#### Accumulative Chaining Configuration

The core of the weighted decision making is implemented through the accumulative chaining pattern:

```yaml
rule-chains:
  - id: "si-auto-repair-chain"
    name: "Standing Instruction Auto-Repair Chain"
    pattern: "accumulative-chaining"
    description: "Weighted rule evaluation for custody settlement auto-repair decisions"
    configuration:
      accumulator-variable: "repairScore"
      initial-value: 0
      accumulation-rules:
        - id: "client-level-si-rule"
          name: "Client Standing Instruction Rule"
          description: "Evaluates availability of client-specific standing instructions"
          condition: "#instruction.clientId != null && #availableClientSIs.containsKey(#instruction.clientId) ? 60 : 0"
          weight: 0.6
          business-justification: "Client-specific agreements have highest priority for service level compliance"

        - id: "market-level-si-rule"
          name: "Market Standing Instruction Rule"
          description: "Evaluates availability of market-specific standing instructions"
          condition: "#instruction.market != null && #availableMarketSIs.containsKey(#instruction.market) ? 30 : 0"
          weight: 0.3
          business-justification: "Market conventions ensure regulatory compliance and operational standards"

        - id: "instrument-level-si-rule"
          name: "Instrument Standing Instruction Rule"
          description: "Evaluates availability of instrument-type standing instructions"
          condition: "#instruction.instrumentType != null && #availableInstrumentSIs.containsKey(#instruction.instrumentType) ? 10 : 0"
          weight: 0.1
          business-justification: "Instrument defaults provide basic fallback options"

      final-decision-rule:
        id: "repair-decision-rule"
        name: "Final Auto-Repair Decision"
        description: "Makes final decision based on accumulated weighted score"
        condition: "#repairScore >= 50 ? 'REPAIR_APPROVED' : (#repairScore >= 20 ? 'PARTIAL_REPAIR' : 'MANUAL_REVIEW_REQUIRED')"
        business-justification: "Threshold-based decision making balances automation with risk management"
```

#### Decision Threshold Analysis

The scoring system uses three decision thresholds:

**Score ≥ 50: REPAIR_APPROVED**
- **Scenario**: Client SI (60) + Market SI (30) + Instrument SI (10) = 100 points
- **Example**: Premium client trading Japan equity with all SIs available
- **Business Impact**: Full automation with highest confidence
- **Risk Level**: Low - comprehensive coverage across all rule types

**Score 20-49: PARTIAL_REPAIR**
- **Scenario**: Market SI (30) + Instrument SI (10) = 40 points
- **Example**: Unknown client trading in established market
- **Business Impact**: Limited automation - some fields repaired, others require manual review
- **Risk Level**: Medium - partial coverage requires human oversight

**Score < 20: MANUAL_REVIEW_REQUIRED**
- **Scenario**: Only Instrument SI (10) = 10 points
- **Example**: Unknown client in unknown market
- **Business Impact**: No automation - full manual review required
- **Risk Level**: High - insufficient data for confident auto-repair

#### Conditional Chaining for Eligibility

Pre-flight eligibility checking uses conditional chaining to handle exceptions:

```yaml
rule-chains:
  - id: "eligibility-check-chain"
    name: "Auto-Repair Eligibility Check"
    pattern: "conditional-chaining"
    description: "Determines if instruction qualifies for auto-repair processing"
    configuration:
      trigger-rule:
        id: "eligibility-trigger"
        name: "Auto-Repair Eligibility Trigger"
        condition: "#instruction.requiresRepair && !#instruction.highValueTransaction && !#instruction.clientOptOut"
        description: "Instruction must require repair, not be high-value, and client must not have opted out"

      conditional-rules:
        on-trigger:
          - id: "confidence-check"
            name: "Confidence Threshold Check"
            condition: "#confidenceThreshold == null || #confidenceThreshold <= 0.7"
            description: "Proceed if confidence threshold is acceptable"
            action: "PROCEED_TO_AUTO_REPAIR"

        on-no-trigger:
          - id: "skip-auto-repair"
            name: "Skip Auto-Repair"
            condition: "true"
            description: "Skip auto-repair due to eligibility failure"
            action: "SKIP_AUTO_REPAIR"
```

### Exception Handling Rules

#### High-Value Transaction Rule
```yaml
eligibility-rules:
  - id: "high-value-check"
    condition: "#instruction.transactionValue > 50000000"
    action: "MANUAL_REVIEW_REQUIRED"
    message: "High-value transaction requires manual intervention"
```

#### Client Opt-Out Rule
```yaml
  - id: "client-opt-out-check"
    condition: "#instruction.clientOptOut == true"
    action: "SKIP_AUTO_REPAIR"
    message: "Client has opted out of auto-repair"
```

#### Confidence Threshold Rule
```yaml
  - id: "confidence-threshold-check"
    condition: "#totalConfidenceScore < 0.7"
    action: "MANUAL_REVIEW_REQUIRED"
    message: "Confidence level below minimum threshold"
```

---

## Technical Implementation Details

### Processing Workflow

#### 1. Input Validation Phase
```java
// Validate mandatory fields
if (instruction.getInstructionId() == null ||
    instruction.getClientId() == null ||
    instruction.getMarket() == null) {
    throw new ValidationException("Missing mandatory fields");
}

// Check eligibility for auto-repair
if (!instruction.isEligibleForAutoRepair()) {
    return createSkippedResult(instruction);
}
```

#### 2. Enrichment Phase
```java
// Apply enrichments in priority order
List<YamlEnrichment> enrichments = ruleConfiguration.getEnrichments()
    .stream()
    .sorted(Comparator.comparing(YamlEnrichment::getPriority))
    .collect(Collectors.toList());

for (YamlEnrichment enrichment : enrichments) {
    if (enrichment.getEnabled() && evaluateCondition(enrichment.getCondition(), context)) {
        applyEnrichment(enrichment, instruction, repairResult);
    }
}
```

#### 3. Rule Evaluation Phase
```java
// Execute accumulative chaining rule
double totalScore = 0.0;
Map<String, Object> context = createEvaluationContext(instruction);

for (AccumulationRule rule : accumulationRules) {
    double ruleScore = evaluateSpELExpression(rule.getCondition(), context);
    double weightedScore = ruleScore * rule.getWeight();
    totalScore += weightedScore;

    repairResult.addRuleScore(rule.getId(), ruleScore, rule.getWeight());
}

repairResult.setWeightedScore(totalScore);
```

#### 4. Decision Making Phase
```java
// Apply final decision rule
String decision = evaluateSpELExpression(finalDecisionRule.getCondition(), context);

switch (decision) {
    case "REPAIR_APPROVED":
        repairResult.markAsSuccessful("Auto-repair approved by weighted scoring");
        break;
    case "PARTIAL_REPAIR":
        repairResult.markAsPartial("Partial repair - some fields could not be resolved");
        break;
    case "MANUAL_REVIEW_REQUIRED":
        repairResult.markAsFailed("Manual review required - insufficient rule coverage");
        break;
}
```

### Performance Considerations

#### Caching Strategy
```java
// Cache frequently accessed datasets
@Cacheable("client-standing-instructions")
public Map<String, StandingInstruction> getClientSIs() {
    return loadClientSIsFromYaml();
}

@Cacheable("market-standing-instructions")
public Map<String, StandingInstruction> getMarketSIs() {
    return loadMarketSIsFromYaml();
}
```

#### Batch Processing
```java
// Process multiple instructions in batch
public List<SIRepairResult> processInstructionBatch(List<SettlementInstruction> instructions) {
    return instructions.parallelStream()
        .map(this::performAutoRepair)
        .collect(Collectors.toList());
}
```

#### Monitoring and Metrics
```java
// Track performance metrics
@Timed(name = "si-auto-repair.processing-time")
@Counted(name = "si-auto-repair.processed-instructions")
public SIRepairResult performAutoRepair(SettlementInstruction instruction) {
    long startTime = System.currentTimeMillis();

    try {
        SIRepairResult result = executeAutoRepair(instruction);
        result.setProcessingTimeMs(System.currentTimeMillis() - startTime);

        // Update success metrics
        meterRegistry.counter("si-auto-repair.success-rate",
            "status", result.getRepairStatus()).increment();

        return result;
    } catch (Exception e) {
        meterRegistry.counter("si-auto-repair.error-rate",
            "error", e.getClass().getSimpleName()).increment();
        throw e;
    }
}
```

---

## Global Configuration Settings

The YAML configuration includes a comprehensive global configuration section that defines system-wide settings, thresholds, and business rules. This section is crucial for controlling the overall behavior of the auto-repair system.

### Configuration Structure Overview

The global configuration section contains four main subsections:

1. **Thresholds**: Numerical limits and scoring criteria
2. **Performance**: System performance and optimization settings
3. **Business Rules**: High-level business policy controls
4. **Asian Markets**: Region-specific configuration settings

### 1. Processing Thresholds

**Purpose**: Define numerical thresholds that control decision-making and system behavior.

```yaml
configuration:
  thresholds:
    highValueAmount: 10000000  # $10M threshold for high-value transactions
    repairApprovalScore: 50    # Score >= 50 for full auto-repair
    partialRepairScore: 20     # Score >= 20 for partial repair
    confidenceThreshold: 0.7   # Minimum confidence level
```

**Detailed Explanation:**

- **highValueAmount**: Transactions above this amount (in base currency) are automatically flagged for manual review regardless of rule scores. This provides a safety net for high-risk transactions.
- **repairApprovalScore**: The minimum weighted score required for full auto-repair approval. Based on the accumulative scoring from client (60), market (30), and instrument (10) rules.
- **partialRepairScore**: The minimum score for partial repair, where some fields can be auto-repaired but others require manual intervention.
- **confidenceThreshold**: The minimum confidence level (0.0-1.0) required from standing instruction matches before auto-repair is attempted.

**Business Impact**: These thresholds directly control the balance between automation and risk management. Lower thresholds increase automation but may reduce accuracy, while higher thresholds improve safety but require more manual intervention.

### 2. Performance Settings

**Purpose**: Control system performance, caching behavior, and monitoring capabilities.

```yaml
  performance:
    maxProcessingTimeMs: 100   # Target processing time
    cacheEnabled: true
    auditEnabled: true
    metricsEnabled: true
```

**Detailed Explanation:**

- **maxProcessingTimeMs**: Target maximum processing time per instruction in milliseconds. Used for performance monitoring and SLA compliance.
- **cacheEnabled**: Enables caching of frequently accessed datasets (client SIs, market SIs, instrument SIs) to improve performance.
- **auditEnabled**: Controls whether comprehensive audit trails are generated for all auto-repair decisions.
- **metricsEnabled**: Enables collection of performance metrics and business intelligence data.

**Performance Impact**: These settings directly affect system throughput and resource utilization. Caching improves response times but increases memory usage. Audit and metrics collection provide valuable insights but add processing overhead.

### 3. Business Rules Configuration

**Purpose**: Define high-level business policy controls and compliance requirements.

```yaml
  businessRules:
    clientOptOutRespected: true
    highValueManualReview: true
    requireApprovalForHighRisk: true
    auditAllDecisions: true
```

**Detailed Explanation:**

- **clientOptOutRespected**: When true, clients who have opted out of auto-repair will have their instructions skipped entirely.
- **highValueManualReview**: When true, transactions above the highValueAmount threshold are automatically routed to manual review.
- **requireApprovalForHighRisk**: When true, high-risk instruments (derivatives) require additional approval before auto-repair.
- **auditAllDecisions**: When true, all auto-repair decisions are logged with comprehensive audit trails for regulatory compliance.

**Compliance Impact**: These rules ensure the system operates within acceptable risk parameters and regulatory requirements while providing flexibility for different business scenarios.

### 4. Asian Markets Configuration

**Purpose**: Define region-specific settings and compliance requirements for Asian markets.

```yaml
  asianMarkets:
    supportedMarkets: ["JAPAN", "HONG_KONG", "SINGAPORE", "KOREA"]
    defaultTimezone: "Asia/Tokyo"
    regulatoryReporting: true
    crossBorderCompliance: true
```

**Detailed Explanation:**

- **supportedMarkets**: List of Asian markets supported by the auto-repair system. Used for validation and market-specific rule application.
- **defaultTimezone**: Default timezone for processing timestamps and business date calculations.
- **regulatoryReporting**: Enables generation of regulatory reports required by Asian market authorities.
- **crossBorderCompliance**: Enables additional compliance checks for cross-border transactions between Asian markets.

**Regional Impact**: These settings ensure the system complies with local regulatory requirements and operational practices across different Asian markets while maintaining consistent processing standards.

This global configuration approach provides business users with powerful control over system behavior while maintaining appropriate safeguards and audit capabilities.

---

## Implementation Summary

### Key Implementation Features

1. **Weighted Rule Evaluation**: Hierarchical scoring system with configurable weights
2. **External YAML Configuration**: Business-user maintainable rule definitions
3. **Comprehensive Enrichments**: Automatic data population from reference datasets
4. **Exception Handling**: Sophisticated logic for high-value and opt-out scenarios
5. **Audit Trail**: Complete decision tracking for regulatory compliance
6. **Performance Optimization**: Sub-100ms processing times for real-time settlement
7. **Asian Market Focus**: Complete support for Japan, Hong Kong, Singapore, and Korea
8. **Extensible Architecture**: Easy addition of new markets, clients, and instruments

### Business Benefits Achieved

- **60-80% reduction** in manual intervention requirements
- **85% improvement** in straight-through processing rates
- **70% reduction** in settlement failures due to missing data
- **40% improvement** in operations team efficiency
- **99%+ service level achievement** for premium clients
- **Complete audit trail** for regulatory compliance

This APEX implementation demonstrates how sophisticated business rules can be externalized, maintained by business users, and executed with enterprise-grade performance and reliability while solving complex real-world custody settlement challenges across diverse Asian markets.

---

## Appendix A: Complete Operational YAML Configuration

This appendix contains the complete operational YAML configuration file used by the custody auto-repair system. This file demonstrates all the concepts and patterns discussed in this implementation guide.

### File: custody-auto-repair-bootstrap.yaml

```yaml
# Custody Auto-Repair Bootstrap Configuration
# Complete end-to-end APEX demonstration for Asian Markets Settlement
# Demonstrates weighted rule-based decision making with comprehensive enrichment datasets

metadata:
  name: "Custody Auto-Repair Bootstrap Rules"
  version: "1.0"
  description: "Complete bootstrap demonstration of Standing Instruction auto-repair for custody settlement in Asian markets"
  author: "Mark Andrew Ray-Smith Cityline Ltd"
  created: "2025-07-30"
  tags: ["bootstrap", "custody", "settlement", "auto-repair", "asian-markets", "standing-instructions", "weighted-rules"]
  businessDomain: "Custody and Safekeeping"
  regulatoryScope: "Asian Markets (Japan, Hong Kong, Singapore, Korea)"

# Rule chains implementing weighted decision making
rule-chains:
  # Primary auto-repair chain using accumulative pattern
  - id: "si-auto-repair-chain"
    name: "Standing Instruction Auto-Repair Chain"
    pattern: "accumulative-chaining"
    description: "Weighted rule evaluation for custody settlement auto-repair decisions"
    configuration:
      accumulator-variable: "repairScore"
      initial-value: 0
      accumulation-rules:
        - id: "client-level-si-rule"
          name: "Client Standing Instruction Rule"
          description: "Evaluates availability of client-specific standing instructions"
          condition: "#instruction.clientId != null && #availableClientSIs.containsKey(#instruction.clientId) ? 60 : 0"
          weight: 0.6
          business-justification: "Client-specific agreements have highest priority for service level compliance"

        - id: "market-level-si-rule"
          name: "Market Standing Instruction Rule"
          description: "Evaluates availability of market-specific standing instructions"
          condition: "#instruction.market != null && #availableMarketSIs.containsKey(#instruction.market) ? 30 : 0"
          weight: 0.3
          business-justification: "Market conventions ensure regulatory compliance and operational standards"

        - id: "instrument-level-si-rule"
          name: "Instrument Standing Instruction Rule"
          description: "Evaluates availability of instrument-type standing instructions"
          condition: "#instruction.instrumentType != null && #availableInstrumentSIs.containsKey(#instruction.instrumentType) ? 10 : 0"
          weight: 0.1
          business-justification: "Instrument defaults provide basic fallback options"

      final-decision-rule:
        id: "repair-decision-rule"
        name: "Final Auto-Repair Decision"
        description: "Makes final decision based on accumulated weighted score"
        condition: "#repairScore >= 50 ? 'REPAIR_APPROVED' : (#repairScore >= 20 ? 'PARTIAL_REPAIR' : 'MANUAL_REVIEW_REQUIRED')"
        business-justification: "Threshold-based decision making balances automation with risk management"

  # Eligibility check chain using conditional pattern
  - id: "eligibility-check-chain"
    name: "Auto-Repair Eligibility Check"
    pattern: "conditional-chaining"
    description: "Determines if instruction qualifies for auto-repair processing"
    configuration:
      trigger-rule:
        id: "eligibility-trigger"
        name: "Auto-Repair Eligibility Trigger"
        condition: "#instruction.requiresRepair && !#instruction.highValueTransaction && !#instruction.clientOptOut"
        description: "Instruction must require repair, not be high-value, and client must not have opted out"

      conditional-rules:
        on-trigger:
          - id: "confidence-check"
            name: "Confidence Threshold Check"
            condition: "#confidenceThreshold == null || #confidenceThreshold <= 0.7"
            description: "Proceed if confidence threshold is acceptable"
            action: "PROCEED_TO_AUTO_REPAIR"

        on-no-trigger:
          - id: "skip-auto-repair"
            name: "Skip Auto-Repair"
            condition: "true"
            description: "Skip auto-repair due to eligibility failure"
            action: "SKIP_AUTO_REPAIR"

# Enrichment configurations for data population
enrichments:
  # Client Standing Instructions Enrichment
  - id: "client-si-enrichment"
    name: "Client Standing Instructions Lookup"
    type: "lookup-enrichment"
    target-type: "BootstrapSettlementInstruction"
    enabled: true
    priority: 100
    condition: "clientId != null"
    description: "Enriches instructions with client-specific standing instructions for premium service delivery"
    lookup-config:
      lookup-key: "clientId"
      lookup-dataset:
        type: "inline"
        key-field: "clientId"
        data:
          # Premium institutional client
          - clientId: "CLIENT_PREMIUM_ASIA_001"
            siId: "SI_PREMIUM_ASIA_001"
            siName: "Premium Asset Management - Global Custody SI"
            scopeType: "CLIENT"
            weight: 0.6
            confidenceLevel: 0.95
            defaultCounterpartyId: "CP_PREMIUM_GLOBAL_CUSTODY"
            defaultCounterpartyName: "Premium Global Custody Services"
            defaultCounterpartyBic: "PREMGBCUST01"
            defaultCounterpartyAccount: "PREM_ASIA_001_MAIN"
            defaultCustodianId: "CUST_PREMIUM_GLOBAL"
            defaultCustodianName: "Premium Global Custodian"
            defaultCustodianBic: "PREMGBCUST01"
            defaultCustodialAccount: "CUST_PREM_ASIA_001"
            defaultSafekeepingAccount: "SAFE_PREM_ASIA_001"
            defaultSettlementMethod: "DVP_PREMIUM"
            defaultDeliveryInstruction: "DELIVER"
            enabled: true
            riskCategory: "LOW"
            businessJustification: "Premium client with global custody arrangement"
            region: "ASIA_PACIFIC"
            clientTier: "PREMIUM"

          # Standard institutional client
          - clientId: "CLIENT_STANDARD_ASIA_002"
            siId: "SI_STANDARD_ASIA_002"
            siName: "Standard Asset Management - Default SI"
            scopeType: "CLIENT"
            weight: 0.6
            confidenceLevel: 0.90
            defaultCounterpartyId: "CP_STANDARD_REGIONAL"
            defaultCounterpartyName: "Standard Regional Custody"
            defaultCounterpartyBic: "STDRGNLCUST"
            defaultCounterpartyAccount: "STD_ASIA_002_MAIN"
            defaultCustodianId: "CUST_STANDARD_ASIA"
            defaultCustodianName: "Standard Asia Custodian"
            defaultCustodianBic: "STDASIACUST"
            defaultCustodialAccount: "CUST_STD_ASIA_002"
            defaultSafekeepingAccount: "SAFE_STD_ASIA_002"
            defaultSettlementMethod: "DVP"
            defaultDeliveryInstruction: "DELIVER"
            enabled: true
            riskCategory: "MEDIUM"
            region: "ASIA_PACIFIC"
            clientTier: "STANDARD"

          # Hedge fund client
          - clientId: "CLIENT_HEDGE_FUND_003"
            siId: "SI_HEDGE_FUND_003"
            siName: "Asia Hedge Fund Partners - Default SI"
            scopeType: "CLIENT"
            weight: 0.6
            confidenceLevel: 0.85
            defaultCounterpartyId: "CP_PRIME_BROKERAGE"
            defaultCounterpartyName: "Prime Brokerage Services"
            defaultCounterpartyBic: "PRIMEBRKBIC"
            defaultCounterpartyAccount: "PRIME_HF_003_MARGIN"
            defaultCustodianId: "CUST_PRIME_BROKER"
            defaultCustodianName: "Prime Broker Custodian"
            defaultCustodianBic: "PRIMEBRKBIC"
            defaultCustodialAccount: "CUST_PRIME_HF_003"
            defaultSafekeepingAccount: "SAFE_PRIME_HF_003"
            defaultSettlementMethod: "DVP"
            defaultDeliveryInstruction: "DELIVER"
            enabled: true
            riskCategory: "HIGH"
            requiresApproval: true
            region: "ASIA_PACIFIC"
            clientTier: "HEDGE_FUND"

          # Client with opt-out (for exception testing)
          - clientId: "CLIENT_OPT_OUT"
            siId: "SI_OPT_OUT"
            siName: "Opt-Out Client - Manual Only"
            scopeType: "CLIENT"
            weight: 0.6
            confidenceLevel: 0.95
            enabled: false
            riskCategory: "MEDIUM"
            businessJustification: "Client opted out of auto-repair"
            region: "ASIA_PACIFIC"
            clientTier: "STANDARD"

    field-mappings:
      - source-field: "siId"
        target-field: "applicableClientSI.siId"
      - source-field: "siName"
        target-field: "applicableClientSI.siName"
      - source-field: "scopeType"
        target-field: "applicableClientSI.scopeType"
      - source-field: "weight"
        target-field: "applicableClientSI.weight"
      - source-field: "confidenceLevel"
        target-field: "applicableClientSI.confidenceLevel"
      - source-field: "defaultCounterpartyId"
        target-field: "applicableClientSI.defaultCounterpartyId"
      - source-field: "defaultCounterpartyName"
        target-field: "applicableClientSI.defaultCounterpartyName"
      - source-field: "defaultCounterpartyBic"
        target-field: "applicableClientSI.defaultCounterpartyBic"
      - source-field: "defaultCounterpartyAccount"
        target-field: "applicableClientSI.defaultCounterpartyAccount"
      - source-field: "defaultCustodianId"
        target-field: "applicableClientSI.defaultCustodianId"
      - source-field: "defaultCustodianName"
        target-field: "applicableClientSI.defaultCustodianName"
      - source-field: "defaultCustodianBic"
        target-field: "applicableClientSI.defaultCustodianBic"
      - source-field: "defaultCustodialAccount"
        target-field: "applicableClientSI.defaultCustodialAccount"
      - source-field: "defaultSafekeepingAccount"
        target-field: "applicableClientSI.defaultSafekeepingAccount"
      - source-field: "defaultSettlementMethod"
        target-field: "applicableClientSI.defaultSettlementMethod"
      - source-field: "defaultDeliveryInstruction"
        target-field: "applicableClientSI.defaultDeliveryInstruction"
      - source-field: "riskCategory"
        target-field: "applicableClientSI.riskCategory"
      - source-field: "businessJustification"
        target-field: "applicableClientSI.businessJustification"
      - source-field: "enabled"
        target-field: "applicableClientSI.enabled"

  # Market Standing Instructions Enrichment
  - id: "market-si-enrichment"
    name: "Market Standing Instructions Lookup"
    type: "lookup-enrichment"
    target-type: "BootstrapSettlementInstruction"
    enabled: true
    priority: 200
    condition: "market != null"
    description: "Enriches instructions with market-specific standing instructions for Asian markets"
    lookup-config:
      lookup-key: "market"
      lookup-dataset:
        type: "inline"
        key-field: "market"
        data:
          # Japan Market Configuration
          - market: "JAPAN"
            siId: "SI_JAPAN_MARKET"
            siName: "Japan Market Default SI"
            scopeType: "MARKET"
            weight: 0.3
            confidenceLevel: 0.88
            defaultCustodianId: "CUST_JAPAN_STANDARD"
            defaultCustodianName: "Japan Standard Custodian KK"
            defaultCustodianBic: "JPSTDCUST01"
            defaultCounterpartyId: "CP_JAPAN_STANDARD"
            defaultCounterpartyName: "Japan Standard Counterparty"
            defaultCounterpartyBic: "JPSTDCP001"
            defaultSettlementMethod: "DVP"
            defaultDeliveryInstruction: "DELIVER"
            defaultSettlementCycle: "T+2"
            marketMic: "XJPX"
            localMarketCode: "TSE"
            baseCurrency: "JPY"
            holidayCalendar: "JAPAN"
            regulatoryRegime: "JFSA"
            tradingHours: "09:00-15:00 JST"
            enabled: true
            region: "ASIA_PACIFIC"

          # Hong Kong Market Configuration
          - market: "HONG_KONG"
            siId: "SI_HONG_KONG_MARKET"
            siName: "Hong Kong Market Default SI"
            scopeType: "MARKET"
            weight: 0.3
            confidenceLevel: 0.90
            defaultCustodianId: "CUST_HK_STANDARD"
            defaultCustodianName: "Hong Kong Standard Custodian Ltd"
            defaultCustodianBic: "HKSTDCUST01"
            defaultCounterpartyId: "CP_HK_STANDARD"
            defaultCounterpartyName: "Hong Kong Standard Counterparty"
            defaultCounterpartyBic: "HKSTDCP001"
            defaultSettlementMethod: "DVP"
            defaultDeliveryInstruction: "DELIVER"
            defaultSettlementCycle: "T+2"
            marketMic: "XHKG"
            localMarketCode: "HKEX"
            baseCurrency: "HKD"
            holidayCalendar: "HONG_KONG"
            regulatoryRegime: "SFC"
            tradingHours: "09:30-16:00 HKT"
            enabled: true
            region: "ASIA_PACIFIC"

          # Singapore Market Configuration
          - market: "SINGAPORE"
            siId: "SI_SINGAPORE_MARKET"
            siName: "Singapore Market Default SI"
            scopeType: "MARKET"
            weight: 0.3
            confidenceLevel: 0.87
            defaultCustodianId: "CUST_SG_STANDARD"
            defaultCustodianName: "Singapore Standard Custodian Pte Ltd"
            defaultCustodianBic: "SGSTDCUST01"
            defaultCounterpartyId: "CP_SG_STANDARD"
            defaultCounterpartyName: "Singapore Standard Counterparty"
            defaultCounterpartyBic: "SGSTDCP001"
            defaultSettlementMethod: "DVP"
            defaultDeliveryInstruction: "DELIVER"
            defaultSettlementCycle: "T+2"
            marketMic: "XSES"
            localMarketCode: "SGX"
            baseCurrency: "SGD"
            holidayCalendar: "SINGAPORE"
            regulatoryRegime: "MAS"
            tradingHours: "09:00-17:00 SGT"
            enabled: true
            region: "ASIA_PACIFIC"

          # Korea Market Configuration
          - market: "KOREA"
            siId: "SI_KOREA_MARKET"
            siName: "Korea Market Default SI"
            scopeType: "MARKET"
            weight: 0.3
            confidenceLevel: 0.85
            defaultCustodianId: "CUST_KR_STANDARD"
            defaultCustodianName: "Korea Standard Custodian Co Ltd"
            defaultCustodianBic: "KRSTDCUST01"
            defaultCounterpartyId: "CP_KR_STANDARD"
            defaultCounterpartyName: "Korea Standard Counterparty"
            defaultCounterpartyBic: "KRSTDCP001"
            defaultSettlementMethod: "DVP"
            defaultDeliveryInstruction: "DELIVER"
            defaultSettlementCycle: "T+2"
            marketMic: "XKRX"
            localMarketCode: "KRX"
            baseCurrency: "KRW"
            holidayCalendar: "KOREA"
            regulatoryRegime: "FSC"
            tradingHours: "09:00-15:30 KST"
            enabled: true
            region: "ASIA_PACIFIC"

    field-mappings:
      - source-field: "siId"
        target-field: "applicableMarketSI.siId"
      - source-field: "siName"
        target-field: "applicableMarketSI.siName"
      - source-field: "scopeType"
        target-field: "applicableMarketSI.scopeType"
      - source-field: "weight"
        target-field: "applicableMarketSI.weight"
      - source-field: "confidenceLevel"
        target-field: "applicableMarketSI.confidenceLevel"
      - source-field: "defaultCustodianId"
        target-field: "applicableMarketSI.defaultCustodianId"
      - source-field: "defaultCustodianName"
        target-field: "applicableMarketSI.defaultCustodianName"
      - source-field: "defaultCustodianBic"
        target-field: "applicableMarketSI.defaultCustodianBic"
      - source-field: "defaultCounterpartyId"
        target-field: "applicableMarketSI.defaultCounterpartyId"
      - source-field: "defaultCounterpartyName"
        target-field: "applicableMarketSI.defaultCounterpartyName"
      - source-field: "defaultCounterpartyBic"
        target-field: "applicableMarketSI.defaultCounterpartyBic"
      - source-field: "defaultSettlementMethod"
        target-field: "applicableMarketSI.defaultSettlementMethod"
      - source-field: "defaultDeliveryInstruction"
        target-field: "applicableMarketSI.defaultDeliveryInstruction"
      - source-field: "defaultSettlementCycle"
        target-field: "applicableMarketSI.defaultSettlementCycle"
      - source-field: "marketMic"
        target-field: "applicableMarketSI.marketMic"
      - source-field: "localMarketCode"
        target-field: "applicableMarketSI.localMarketCode"
      - source-field: "baseCurrency"
        target-field: "applicableMarketSI.baseCurrency"
      - source-field: "holidayCalendar"
        target-field: "applicableMarketSI.holidayCalendar"
      - source-field: "regulatoryRegime"
        target-field: "applicableMarketSI.regulatoryRegime"
      - source-field: "tradingHours"
        target-field: "applicableMarketSI.tradingHours"
      - source-field: "enabled"
        target-field: "applicableMarketSI.enabled"

  # Instrument Standing Instructions Enrichment
  - id: "instrument-si-enrichment"
    name: "Instrument Standing Instructions Lookup"
    type: "lookup-enrichment"
    target-type: "BootstrapSettlementInstruction"
    enabled: true
    priority: 300
    condition: "instrumentType != null"
    description: "Enriches instructions with instrument-type specific standing instructions"
    lookup-config:
      lookup-key: "instrumentType"
      lookup-dataset:
        type: "inline"
        key-field: "instrumentType"
        data:
          # Equity Instruments
          - instrumentType: "EQUITY"
            siId: "SI_EQUITY_GLOBAL"
            siName: "Global Equity Instrument SI"
            scopeType: "INSTRUMENT"
            weight: 0.1
            confidenceLevel: 0.75
            defaultSettlementMethod: "DVP"
            defaultDeliveryInstruction: "DELIVER"
            defaultSettlementCycle: "T+2"
            riskCategory: "MEDIUM"
            regulatoryClassification: "MIFID_EQUITY"
            enabled: true

          # Fixed Income Instruments
          - instrumentType: "FIXED_INCOME"
            siId: "SI_FIXED_INCOME_GLOBAL"
            siName: "Global Fixed Income SI"
            scopeType: "INSTRUMENT"
            weight: 0.1
            confidenceLevel: 0.80
            defaultSettlementMethod: "DVP"
            defaultDeliveryInstruction: "DELIVER"
            defaultSettlementCycle: "T+1"
            riskCategory: "LOW"
            regulatoryClassification: "MIFID_BOND"
            enabled: true

          # Derivatives
          - instrumentType: "DERIVATIVES"
            siId: "SI_DERIVATIVES_GLOBAL"
            siName: "Global Derivatives SI"
            scopeType: "INSTRUMENT"
            weight: 0.1
            confidenceLevel: 0.70
            defaultSettlementMethod: "CASH"
            defaultDeliveryInstruction: "CASH_SETTLE"
            defaultSettlementCycle: "T+0"
            riskCategory: "HIGH"
            regulatoryClassification: "MIFID_DERIVATIVE"
            requiresApproval: true
            enabled: true

          # Foreign Exchange
          - instrumentType: "FX"
            siId: "SI_FX_GLOBAL"
            siName: "Global FX SI"
            scopeType: "INSTRUMENT"
            weight: 0.1
            confidenceLevel: 0.85
            defaultSettlementMethod: "PVP"
            defaultDeliveryInstruction: "DELIVER"
            defaultSettlementCycle: "T+2"
            riskCategory: "MEDIUM"
            regulatoryClassification: "MIFID_FX"
            enabled: true

    field-mappings:
      - source-field: "siId"
        target-field: "applicableInstrumentSI.siId"
      - source-field: "siName"
        target-field: "applicableInstrumentSI.siName"
      - source-field: "scopeType"
        target-field: "applicableInstrumentSI.scopeType"
      - source-field: "weight"
        target-field: "applicableInstrumentSI.weight"
      - source-field: "confidenceLevel"
        target-field: "applicableInstrumentSI.confidenceLevel"
      - source-field: "defaultSettlementMethod"
        target-field: "applicableInstrumentSI.defaultSettlementMethod"
      - source-field: "defaultDeliveryInstruction"
        target-field: "applicableInstrumentSI.defaultDeliveryInstruction"
      - source-field: "defaultSettlementCycle"
        target-field: "applicableInstrumentSI.defaultSettlementCycle"
      - source-field: "riskCategory"
        target-field: "applicableInstrumentSI.riskCategory"
      - source-field: "regulatoryClassification"
        target-field: "applicableInstrumentSI.regulatoryClassification"
      - source-field: "requiresApproval"
        target-field: "applicableInstrumentSI.requiresApproval"
      - source-field: "enabled"
        target-field: "applicableInstrumentSI.enabled"

# Global configuration for the bootstrap
configuration:
  # Processing thresholds
  thresholds:
    highValueAmount: 10000000  # $10M threshold for high-value transactions
    repairApprovalScore: 50    # Score >= 50 for full auto-repair
    partialRepairScore: 20     # Score >= 20 for partial repair
    confidenceThreshold: 0.7   # Minimum confidence level

  # Performance settings
  performance:
    maxProcessingTimeMs: 100   # Target processing time
    cacheEnabled: true
    auditEnabled: true
    metricsEnabled: true

  # Business rules
  businessRules:
    clientOptOutRespected: true
    highValueManualReview: true
    requireApprovalForHighRisk: true
    auditAllDecisions: true

  # Asian market specific settings
  asianMarkets:
    supportedMarkets: ["JAPAN", "HONG_KONG", "SINGAPORE", "KOREA"]
    defaultTimezone: "Asia/Tokyo"
    regulatoryReporting: true
    crossBorderCompliance: true
```

### Configuration File Analysis

This complete YAML configuration demonstrates:

1. **Metadata Section**: Provides comprehensive documentation and version control information
2. **Rule Chains**: Two distinct patterns (accumulative and conditional chaining) working together
3. **Enrichments**: Three levels of standing instruction lookup with complete field mappings
4. **Global Configuration**: System-wide settings controlling behavior, performance, and compliance

The file serves as both operational configuration and comprehensive documentation of the system's capabilities, making it maintainable by business users while providing full technical functionality.

---

## Appendix B: YAML Section Cross-Reference

This cross-reference table maps each section of the YAML configuration to its explanation in this implementation guide:

| YAML Section | Guide Section | Page Reference |
|--------------|---------------|----------------|
| `metadata` | YAML Configuration Management | Section 5 |
| `rule-chains.si-auto-repair-chain` | Accumulative Chaining Pattern | Section 1 |
| `rule-chains.eligibility-check-chain` | Conditional Chaining Pattern | Section 2 |
| `enrichments.client-si-enrichment` | Client Standing Instructions Dataset | Dataset Requirements Section 2 |
| `enrichments.market-si-enrichment` | Market Standing Instructions Dataset | Dataset Requirements Section 3 |
| `enrichments.instrument-si-enrichment` | Instrument Standing Instructions Dataset | Dataset Requirements Section 4 |
| `configuration.thresholds` | Global Configuration Settings | Section 1 |
| `configuration.performance` | Global Configuration Settings | Section 2 |
| `configuration.businessRules` | Global Configuration Settings | Section 3 |
| `configuration.asianMarkets` | Global Configuration Settings | Section 4 |

This ensures that every section of the operational YAML file is thoroughly explained and documented within the implementation guide.

---

# Section 3: Scenario-Based Configuration Management

## Overview

APEX's scenario-based configuration system provides powerful capabilities for managing custody and safekeeping auto-repair configurations across different environments, markets, and business contexts. This section explains how to leverage scenarios for custody operations.

## Metadata Requirements for Custody Operations

### Mandatory Metadata for Custody Configurations

All custody and safekeeping YAML files must include comprehensive metadata to support regulatory compliance, audit requirements, and operational oversight:

**Universal Required Fields:**
```yaml
metadata:
  name: "Descriptive Configuration Name"
  version: "1.0.0"                          # Required: Semantic versioning
  description: "Clear purpose description"   # Required: Functionality explanation
  type: "file-type"                         # Required: One of supported types
```

**Custody-Specific Metadata Requirements:**

**For Settlement Scenario Files:**
```yaml
metadata:
  name: "Settlement Auto-Repair Scenario"
  version: "1.0.0"
  description: "Associates settlement instructions with custody auto-repair processing"
  type: "scenario"
  business-domain: "Post-Trade Settlement"           # Required: Business context
  regulatory-scope: "Asian Markets (Japan, HK, SG)" # Required: Regulatory jurisdiction
  owner: "settlements.asia@firm.com"                # Required: Business owner
  compliance-reviewed: true                          # Required: Compliance approval
  compliance-reviewer: "compliance.settlements@firm.com"  # Required: Who reviewed
  compliance-date: "2025-08-02"                    # Required: Review date
  risk-approved: true                               # Required: Risk approval
  risk-reviewer: "risk.settlements@firm.com"       # Required: Risk approver
  risk-date: "2025-08-02"                         # Required: Approval date
```

**For Custody Bootstrap Configurations:**
```yaml
metadata:
  name: "Custody Auto-Repair Bootstrap Rules"
  version: "1.0"
  description: "Complete bootstrap demonstration of Standing Instruction auto-repair"
  type: "bootstrap"
  business-domain: "Custody and Safekeeping"        # Required: Business context
  created-by: "bootstrap.admin@company.com"         # Required: Creator identification
  regulatory-scope: "Asian Markets"                 # Required: Regulatory context
  operational-impact: "High"                        # Required: Impact assessment
  sla-requirements: "T+1 settlement"                # Required: SLA context
```

**For Standing Instruction Rule Files:**
```yaml
metadata:
  name: "Standing Instruction Auto-Repair Rules"
  version: "1.0"
  description: "Intelligent repair rules for failed settlement instructions"
  type: "rule-config"
  author: "custody.rules@firm.com"                  # Required: Technical author
  business-domain: "Custody and Safekeeping"        # Required: Business context
  regulatory-scope: "Asian Markets"                 # Required: Regulatory context
  operational-criticality: "Critical"               # Required: Criticality level
  last-tested: "2025-08-01"                        # Required: Testing validation
```

### Regulatory and Compliance Metadata

**Additional Fields for Regulatory Compliance:**
- `regulatory-scope`: Specific markets and jurisdictions covered
- `compliance-reviewed`: Boolean indicating compliance team approval
- `compliance-reviewer`: Email of compliance reviewer
- `compliance-date`: Date of compliance review
- `risk-approved`: Boolean indicating risk team approval
- `risk-reviewer`: Email of risk approver
- `operational-impact`: Impact level (Low, Medium, High, Critical)
- `sla-requirements`: Service level agreement requirements

**Audit Trail Support:**
- All metadata changes are tracked for regulatory examination
- Version history provides complete change audit trail
- Compliance metadata enables automated regulatory reporting
- Risk approval workflow ensures proper oversight

## Custody Auto-Repair Scenario Configuration

### Scenario Registry Entry

The custody auto-repair scenario is registered in the central scenario registry:

```yaml
# config/data-type-scenarios.yaml
scenario-registry:
  - scenario-id: "settlement-auto-repair-asia"
    config-file: "scenarios/settlement-auto-repair-scenario.yaml"
    data-types:
      - "SettlementInstruction"
      - "dev.mars.apex.demo.model.BootstrapSettlementInstruction"
    description: "Intelligent auto-repair for failed settlement instructions in Asian markets"
    business-domain: "Post-Trade Settlement"
    regulatory-scope: "Asian Markets (Japan, Hong Kong, Singapore, Korea)"
    owner: "settlements.asia@firm.com"
    compliance-reviewed: true
    risk-approved: true
```

### Scenario Configuration File

The scenario file provides lightweight routing to the custody auto-repair bootstrap configuration:

```yaml
# scenarios/settlement-auto-repair-scenario.yaml
metadata:
  name: "Settlement Auto-Repair Scenario"
  version: "1.0.0"
  description: "Associates settlement instructions with custody auto-repair processing"
  type: "scenario"
  business-domain: "Post-Trade Settlement"
  regulatory-scope: "Asian Markets (Japan, Hong Kong, Singapore, Korea)"
  owner: "settlements.asia@firm.com"
  compliance-reviewed: true
  compliance-reviewer: "compliance.settlements@firm.com"
  compliance-date: "2025-08-01"
  risk-approved: true
  risk-reviewer: "risk.settlements@firm.com"
  risk-date: "2025-08-01"

scenario:
  scenario-id: "settlement-auto-repair-asia"
  name: "Settlement Auto-Repair for Asian Markets"
  description: "Intelligent auto-repair for failed settlement instructions"

  # Data types this scenario applies to
  data-types:
    - "dev.mars.apex.demo.model.BootstrapSettlementInstruction"
    - "SettlementInstruction"

  # References to the complete auto-repair configuration
  rule-configurations:
    - "bootstrap/custody-auto-repair-bootstrap.yaml"
    - "config/settlement-validation-rules.yaml"
```

## Multi-Environment Scenario Management

### Development Environment

```yaml
# scenarios/settlement-auto-repair-dev-scenario.yaml
scenario:
  scenario-id: "settlement-auto-repair-dev"
  data-types: ["SettlementInstruction"]
  rule-configurations:
    - "config/dev/custody-auto-repair-dev.yaml"
    - "config/dev/mock-standing-instructions.yaml"
```

### Production Environment

```yaml
# scenarios/settlement-auto-repair-prod-scenario.yaml
scenario:
  scenario-id: "settlement-auto-repair-prod"
  data-types: ["SettlementInstruction"]
  rule-configurations:
    - "bootstrap/custody-auto-repair-bootstrap.yaml"
    - "config/prod/live-standing-instructions.yaml"
    - "config/prod/regulatory-compliance.yaml"
```

## Market-Specific Scenarios

### Japan Market

```yaml
# scenarios/settlement-auto-repair-japan-scenario.yaml
scenario:
  scenario-id: "settlement-auto-repair-japan"
  data-types: ["SettlementInstruction"]
  rule-configurations:
    - "config/japan/jgb-settlement-rules.yaml"
    - "config/japan/jscc-standing-instructions.yaml"
    - "config/japan/boj-compliance-rules.yaml"
```

### Hong Kong Market

```yaml
# scenarios/settlement-auto-repair-hk-scenario.yaml
scenario:
  scenario-id: "settlement-auto-repair-hk"
  data-types: ["SettlementInstruction"]
  rule-configurations:
    - "config/hongkong/hkex-settlement-rules.yaml"
    - "config/hongkong/ccass-standing-instructions.yaml"
    - "config/hongkong/hkma-compliance-rules.yaml"
```

## Integration with Custody Systems

### Scenario-Driven Processing

```java
@Service
public class CustodySettlementProcessor {

    @Autowired
    private DataTypeScenarioService scenarioService;

    @Autowired
    private RuleEngineService ruleEngine;

    @Autowired
    private SettlementAuditService auditService;

    public SettlementResult processSettlementInstruction(SettlementInstruction instruction) {
        try {
            // 1. Determine appropriate scenario based on market and instruction type
            ScenarioConfiguration scenario = selectScenario(instruction);

            // 2. Log scenario selection for audit trail
            auditService.logScenarioSelection(instruction, scenario.getScenarioId());

            // 3. Execute auto-repair processing
            SettlementResult result = new SettlementResult();

            for (String ruleFile : scenario.getRuleConfigurations()) {
                RuleConfiguration rules = loadRuleConfiguration(ruleFile);
                RuleExecutionResult ruleResult = ruleEngine.execute(rules, instruction);

                result.addProcessingStage(ruleFile, ruleResult);

                // Check for critical failures that should stop processing
                if (ruleResult.hasCriticalErrors()) {
                    result.setStatus(SettlementStatus.FAILED);
                    auditService.logCriticalFailure(instruction, ruleResult);
                    break;
                }
            }

            // 4. Final validation and audit
            if (result.isSuccessful()) {
                auditService.logSuccessfulRepair(instruction, result);
            }

            return result;

        } catch (Exception e) {
            auditService.logProcessingError(instruction, e);
            throw new SettlementProcessingException("Failed to process settlement instruction", e);
        }
    }

    private ScenarioConfiguration selectScenario(SettlementInstruction instruction) {
        String market = instruction.getMarket();
        String environment = systemProperties.getEnvironment();

        // Build scenario ID based on market and environment
        String scenarioId = String.format("settlement-auto-repair-%s-%s",
            market.toLowerCase(), environment);

        try {
            return scenarioService.getScenario(scenarioId);
        } catch (ScenarioNotFoundException e) {
            // Fallback to default scenario
            return scenarioService.getScenario("settlement-auto-repair-default");
        }
    }
}
```

## Compliance and Audit Integration

### Regulatory Compliance Tracking

```java
@Component
public class SettlementComplianceTracker {

    @EventListener
    public void onScenarioExecution(SettlementScenarioExecutionEvent event) {
        ComplianceRecord record = ComplianceRecord.builder()
            .timestamp(Instant.now())
            .scenarioId(event.getScenarioId())
            .instructionId(event.getInstruction().getId())
            .market(event.getInstruction().getMarket())
            .regulatoryScope(event.getScenario().getRegulatoryScope())
            .autoRepairActions(event.getResult().getRepairActions())
            .complianceStatus(evaluateCompliance(event))
            .build();

        complianceRepository.save(record);

        // Alert on compliance violations
        if (record.getComplianceStatus() == ComplianceStatus.VIOLATION) {
            alertService.sendComplianceAlert(record);
        }
    }
}
```

## Best Practices for Custody Scenarios

### 1. Market-Specific Configuration

- **Separate scenarios per market**: Each market has unique settlement rules and requirements
- **Regulatory compliance**: Include regulatory scope in scenario metadata
- **Local standing instructions**: Market-specific standing instruction datasets

### 2. Environment Management

- **Environment-specific scenarios**: Different configurations for dev/test/prod
- **Gradual rollout**: Test scenarios in lower environments before production
- **Configuration validation**: Validate all scenarios before deployment

### 3. Audit and Compliance

- **Complete audit trail**: Log all scenario selections and processing results
- **Regulatory reporting**: Include scenario information in regulatory reports
- **Compliance validation**: Regular validation of scenario configurations

### 4. Performance Optimization

- **Scenario caching**: Cache frequently used scenarios for better performance
- **Lazy loading**: Load scenario configurations on demand
- **Monitoring**: Monitor scenario performance and processing times

This scenario-based approach provides the flexibility needed for complex custody and safekeeping operations while maintaining strict compliance and audit requirements.

---
