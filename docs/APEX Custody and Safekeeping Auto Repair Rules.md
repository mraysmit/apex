
Custody Settlement and Safekeeping Auto-Repair Rules
===================================================

**Version:** 1.0  
**Date:** 2025-07-29  
**Author:** Mark Andrew Ray-Smith Cityline Ltd

### Overview of Standing Instruction (SI) Transaction Auto-Repair Rules:

In custody settlement and safekeeping, particularly within varied Asian markets, transaction instructions occasionally fail initial validation or matching due to missing, ambiguous, or incorrect instruction fields. To mitigate settlement delays, automated standing instruction (SI) repair rules can be implemented to identify and automatically correct or enrich these instructions based on predefined rules, logic, or client-specific agreements.

**Core Concept:**

* **Standing Instructions (SI)** represent pre-defined default or fallback instructions stored for clients, markets, or instruments, used to auto-repair or enrich failing transaction instructions.
* **Auto-Repair Logic** is activated when incoming instructions are incomplete, ambiguous, or fail validation.
* **Weighted Rule-Based Decisioning:** Instead of hard-coded deterministic logic, decisions are made based on fuzzy, weighted scoring. This enables a more nuanced, adaptive decision-making approach, suitable for complex or ambiguous scenarios.

---

## Key Business Requirements:

### 1. Identification and Classification of SI Repair Triggers:

* Clearly define conditions under which a settlement instruction becomes a candidate for auto-repair. Examples:

    * Missing or ambiguous delivery instructions (e.g., custodial accounts, counterparty details, settlement methods).
    * Non-conformance with market-specific rules or instrument-specific settlement requirements.

### 2. Rules Repository and Granularity:

* Create rules at varying levels of granularity:

    * **Client-level rules:** SI unique to particular client preferences or service-level agreements.
    * **Market-level rules:** Rules conforming to specific market practices (e.g., Japan, Singapore, Hong Kong, Korea, etc.).
    * **Instrument-level rules:** Instrument-specific settlement conventions (e.g., equities, fixed income, FX, derivatives).

* Example rule granularity:

  ```
  If [Client X] AND [Market = Japan] AND [Instrument Type = Equity] AND [Counterparty Missing]
  THEN apply [SI = “Default Japanese Equity Custodian”]
  ```

### 3. Weighted Decision Logic (Fuzzy Rule Engine):

* Establish weightings/priorities for each rule based on reliability, confidence, specificity, or hierarchy.
* Use aggregate scoring to identify best-fit SI:

    * Each matched rule contributes a weighted score.
    * The SI with the highest accumulated score across matched rules is selected.
    * Thresholds can be configured to determine confidence levels required for activation.
* Example weighting scenario:

  ```
  Rule 1: Client-specific rule match (weight: 0.6)
  Rule 2: Market-specific rule match (weight: 0.3)
  Rule 3: Instrument-specific rule match (weight: 0.1)

  Aggregate Weighted Score = 0.6(Client) + 0.3(Market) + 0.1(Instrument)
  ```

### 4. Business Logic for Rule Activation and Prioritization:

* Prioritize rules in descending order of specificity:

    1. **Explicit Client Instructions:** Highest priority
    2. **Market Conventions:** Medium priority
    3. **Instrument defaults:** Lower priority

* Logic for tie-break scenarios must be explicitly defined (e.g., default to client-level rules or alert for manual validation).

### 5. Exception Management:

* Clearly defined exception conditions to prevent unintended SI application:

    * Sensitive or high-value instructions always requiring manual intervention.
    * Client-specific opt-outs from automated repair.

### 6. Audit and Traceability:

* Each auto-repair action must have clear audit trails, including:

    * Original instruction details.
    * SI applied (and why).
    * Rule weightings that contributed to the decision.

---

## Java Rules Engine Integration Requirements:

Given an existing Java-based rules engine, requirements for implementation include:

### 1. Configuration & Maintainability:

* Rules must be externally configurable (e.g., via database, XML, JSON, YAML) to allow non-developers (business analysts or operations team) to maintain and adjust weights, rules, and conditions without code deployment.

### 2. Rule Definition and Storage:

* Standardized rule definition language (e.g., Drools, Easy Rules, or proprietary schema) to manage rule lifecycle:

    * Conditions
    * Actions (assignments)
    * Weighting/scoring attributes

### 3. Real-time Rule Evaluation:

* Rules engine must efficiently evaluate and select SI with minimal latency, suitable for real-time trade settlement scenarios.

### 4. Reporting and Monitoring:

* Provide real-time monitoring dashboard or reporting capability, showing:

    * SI application statistics per market/client/instrument.
    * Rule usage frequency and effectiveness.
    * Exceptions or overrides requiring human intervention.

### 5. Extensibility:

* Rules architecture should support easy addition or modification of future market, client, or instrument-specific rules without extensive coding or redevelopment.

---

## Example Use Case (Scenario):

| Attribute                    | Input Instruction  | Matched Rule                                            | Weight                |
| ---------------------------- | ------------------ | ------------------------------------------------------- | --------------------- |
| Client                       | Client A           | Client-specific SI exists                               | 0.5                   |
| Market                       | Hong Kong          | Market default SI                                       | 0.3                   |
| Instrument                   | HK Listed Equities | Instrument-specific SI                                  | 0.2                   |
| Settlement Counterparty      | Missing            | Matches auto-repair condition                           | Trigger               |
| **Aggregate Weighted Score** |                    | Client SI (0.5) + Market SI (0.3) + Instrument SI (0.2) | 1.0 (Full confidence) |

**Decision:** Apply Client-specific SI due to highest weighted score.

---

## TODO:

* Finalize conditions, attributes, and weights with stakeholders.
* Define rule format/schema for Java rule engine.
* Develop and deploy rules into the Java-based rule engine.
* Test rule accuracy and edge cases in various Asian market scenarios.
* Monitor and adjust rule weights based on operational data and performance feedback.

This structured, weighted-rule approach provides robust flexibility, enabling rapid, accurate, automated decision-making tailored precisely to the nuanced demands of custody settlement and safekeeping across diverse Asian markets.

**Core Problem**: Settlement instructions sometimes fail validation due to missing, ambiguous, or incorrect fields, causing delays. The solution is automated repair using predefined standing instructions.

**Weighted Rule-Based Approach**: Instead of hard-coded logic, the system uses fuzzy, weighted scoring where:
* Multiple rules can match a scenario
* Each rule has a weight/priority
* The system aggregates scores to make decisions
* Higher aggregate scores indicate better matches

**Rule Granularity Hierarchy**:
* Client-level (highest priority, weight ~0.5-0.6)
* Market-level (medium priority, weight ~0.3)
* Instrument-level (lowest priority, weight ~0.1-0.2)

**Technical Requirements:**
* External configuration (YAML/JSON) for business users
* Real-time evaluation with low latency
* Comprehensive audit trails
* Monitoring and reporting capabilities
* Extensible architecture
How This Relates to Your Rules Engine:
This document describes a perfect use case for your existing rules engine framework! It demonstrates:
Weighted rule evaluation (similar to your accumulative chaining pattern)
Conditional rule execution based on missing/invalid data
Hierarchical rule prioritization
External configuration requirements (which you already support via YAML)
Complex business logic with multiple evaluation criteria