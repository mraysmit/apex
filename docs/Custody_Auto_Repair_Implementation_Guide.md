# Custody Auto-Repair Implementation Guide

## SpEL Rules Engine Demonstration for Asian Markets Settlement

**Version:** 1.0  
**Date:** 2025-07-29  
**Author:** Mark Andrew Ray-Smith Cityline Ltd

---

## Table of Contents

1. [Executive Summary](#executive-summary)
2. [SpEL Rules Engine Features Utilized](#spel-rules-engine-features-utilized)
3. [System Architecture](#system-architecture)
4. [Data Model Overview](#data-model-overview)
5. [Dataset Requirements](#dataset-requirements)
6. [Rule Configuration](#rule-configuration)
7. [Implementation Details](#implementation-details)
8. [Business Value](#business-value)

---

## Executive Summary

The Custody Auto-Repair example demonstrates a production-ready implementation of Standing Instruction (SI) auto-repair for custody settlement operations in Asian markets. This system automatically repairs incomplete or ambiguous settlement instructions using weighted rule-based decision making, external YAML configuration, and comprehensive enrichment datasets.

### Key Capabilities Demonstrated

- **Weighted Rule Evaluation**: Hierarchical decision making with Client (0.6) > Market (0.3) > Instrument (0.1) priority weighting
- **External Configuration**: Business-user maintainable YAML configuration without code deployment
- **Asian Market Focus**: Complete support for Japan, Hong Kong, Singapore, and Korea settlement conventions
- **Enrichment-Based Architecture**: Automatic field population using lookup datasets and field mappings
- **Exception Handling**: Sophisticated logic for high-value transactions and client opt-outs
- **Comprehensive Audit Trail**: Full decision tracking for regulatory compliance

---

## SpEL Rules Engine Features Utilized

### 1. Accumulative Chaining Pattern

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

**Features Used:**
- Weighted score accumulation across multiple rule evaluations
- Conditional scoring based on data availability
- Final decision thresholds with multiple outcome paths
- Context variable management (`#repairScore`)

### 2. Conditional Chaining Pattern

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

**Features Used:**
- Boolean trigger evaluation
- Branching logic based on trigger results
- Exception handling for ineligible instructions

### 3. Lookup Enrichments

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

**Features Used:**
- Inline YAML datasets for business-user maintenance
- Key-based lookup with configurable key fields
- Automatic field mapping and population
- Conditional enrichment application
- Multiple enrichment types (client, market, instrument, counterparty, custodial)

### 4. SpEL Expression Evaluation

Advanced **Spring Expression Language** usage throughout:

```yaml
# Complex conditional logic
condition: "#instruction.clientId != null && #availableClientSIs.containsKey(#instruction.clientId) ? 60 : 0"

# Nested conditional expressions
condition: "#repairScore >= 50 ? 'REPAIR_APPROVED' : (#repairScore >= 20 ? 'PARTIAL_REPAIR' : 'MANUAL_REVIEW_REQUIRED')"

# Object property access and method calls
condition: "#instruction.requiresRepair && !#instruction.highValueTransaction"
```

**Features Used:**
- Ternary conditional operators
- Object property navigation
- Method invocation on context objects
- Boolean logic combinations
- Numeric comparisons and calculations

### 5. YAML Configuration Management

**External configuration** capabilities:

```yaml
metadata:
  name: "Custody Auto-Repair Rules"
  version: "1.0"
  description: "Standing Instruction auto-repair rules for custody settlement in Asian markets"
  author: "Mark Andrew Ray-Smith Cityline Ltd"
  tags: ["custody", "settlement", "auto-repair", "asian-markets"]
```

**Features Used:**
- Metadata management for configuration tracking
- Version control and change management
- Descriptive documentation within configuration
- Tag-based categorization
- Business-user friendly structure

---

## System Architecture

### Component Overview

```
┌─────────────────────────────────────────────────────────────────┐
│                    Custody Auto-Repair System                   │
├─────────────────────────────────────────────────────────────────┤
│  Input: SettlementInstruction (incomplete/ambiguous)           │
│  Output: SIRepairResult (repaired instruction + audit trail)   │
└─────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────┐
│                    SpEL Rules Engine                            │
├─────────────────────────────────────────────────────────────────┤
│  • YAML Configuration Loader                                   │
│  • Rule Chain Executor (Accumulative + Conditional)            │
│  • Enrichment Service (Lookup-based)                          │
│  • Expression Evaluator (SpEL)                                │
└─────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────┐
│                    Configuration Layer                          │
├─────────────────────────────────────────────────────────────────┤
│  • custody-auto-repair-rules.yaml                             │
│  • Inline datasets (Client/Market/Instrument SIs)             │
│  • Field mappings and enrichment rules                        │
│  • Weighted decision thresholds                               │
└─────────────────────────────────────────────────────────────────┘
```

### Processing Flow

1. **Input Validation**: Settlement instruction eligibility check
2. **Enrichment Phase**: Lookup and populate missing fields using inline datasets
3. **Rule Evaluation**: Weighted scoring across client/market/instrument rules
4. **Decision Making**: Apply thresholds and determine repair action
5. **Result Generation**: Create comprehensive audit trail and repair result

---

## Data Model Overview

### Core Domain Objects

#### SettlementInstruction
Primary input object representing a custody settlement instruction:

```java
public class SettlementInstruction {
    // Instruction Identification
    private String instructionId;
    private LocalDate instructionDate;
    private LocalDate settlementDate;
    
    // Client Information
    private String clientId;
    private String clientName;
    private String clientAccountId;
    
    // Market Information
    private String market; // "JAPAN", "HONG_KONG", "SINGAPORE", "KOREA"
    private String marketMic; // Market Identifier Code
    
    // Instrument Information
    private String instrumentType; // "EQUITY", "FIXED_INCOME", "FX", "DERIVATIVES"
    private String instrumentId;
    private String isin;
    private String currency;
    
    // Settlement Details (may be missing - triggers auto-repair)
    private BigDecimal settlementAmount;
    private String settlementCurrency;
    private String settlementMethod; // "DVP", "FOP", "CASH"
    private String deliveryInstruction;
    
    // Counterparty Information (auto-repair target)
    private String counterpartyId;
    private String counterpartyName;
    private String counterpartyBic;
    private String counterpartyAccount;
    
    // Custodial Information (auto-repair target)
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
    
    // Rule Matching
    private String applicabilityCondition; // SpEL expression
    private int priority;
    private double weight; // 0.6 (client), 0.3 (market), 0.1 (instrument)
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
