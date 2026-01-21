# APEX Execution Trace API Design

## Overview

This document defines the API design for APEX execution tracing that aligns with the existing YAML dependency tree API format. The execution trace provides runtime visibility into scenario processing, rule evaluation, enrichment execution, and data transformations - structured to enable overlay visualization with the static dependency tree.

### Key Innovation: Dataset-Specific Rule Evaluation

The execution trace captures how validation rules evaluate against different datasets with actual runtime values. This provides complete traceability from static YAML rule definitions to dynamic runtime evaluations, enabling precise debugging and audit trails.

### Core Features

- **Hierarchical Execution Tree**: TreeNode structure compatible with D3.js visualization
- **Decision Data Capture**: Optional metadata explaining why specific execution paths were taken
- **Dataset-Specific Rule Evaluation**: Captures how each validation rule evaluates against different datasets with actual runtime values
- **Rule Definition vs Runtime Separation**: Distinguishes between static YAML rule definitions and dynamic runtime evaluations
- **Sub-Condition Breakdown**: Complex validation conditions broken down into individual expressions with actual value substitution
- **Complete Validation Traceability**: Full audit trail from input values to validation results for every dataset
- **Dataset Tracking**: Complete traceability of data transformations through `datasetId` and `datasetIdName`
- **Performance Monitoring**: Execution timing and resource utilization metrics
- **Real-time and Historical Access**: Query current executions or retrieve historical traces

## Design Principles

1. **Format Alignment**: Use the same hierarchical TreeNode structure as YAML dependency tree API
2. **D3.js Compatibility**: Nested `children` array format for direct visualization
3. **Overlay Capability**: Execution trace can be overlaid on dependency tree for side-by-side comparison
4. **Dataset-Specific Evaluation**: Capture actual runtime values and evaluation results for each dataset that flows through validation rules
5. **Rule Definition Separation**: Distinguish between static YAML rule definitions and dynamic runtime evaluations with actual data
6. **Sub-Condition Granularity**: Break down complex conditions into individual expressions showing actual value substitution
7. **Complete Audit Trail**: Provide full traceability from input values to validation results for compliance and debugging
8. **Queryable**: Support filtering by scenario, time range, execution status, dataset ID, and validation results
9. **Performance**: Capture detailed evaluation metrics without impacting processing performance

---

## API Response Format

### Execution Trace Tree Structure

The execution trace uses the **same TreeNode format** as the dependency tree, with execution-specific metadata and **decision data**:

```json
{
  "name": "scenario-otc-options-validation",
  "id": "exec-uuid-12345",
  "path": "scenarios/otc-options-validation.yaml",
  "type": "scenario-execution",
  "depth": 0,
  "height": 5,
  "childCount": 3,
  "executionOrder": 1,
  "status": "SUCCESS",
  "startTime": "2025-10-27T10:15:30.123Z",
  "endTime": "2025-10-27T10:15:30.456Z",
  "durationMs": 333,
  "datasetId": "trade-OTC-20251027-12345",
  "datasetIdName": "trading-system-xx-trade-id",
  "decisionData": {
    "scenarioRegistryRouting": {
      "routingStrategy": "classification-based",
      "dataType": "HashMap",
      "routingDecisions": [
        {
          "routingMethod": "direct-data-type-mapping",
          "dataTypeMatched": false,
          "availableScenarios": []
        },
        {
          "routingMethod": "classification-rule-evaluation",
          "scenariosEvaluated": [
            {
              "scenarioId": "otc-option-us",
              "classificationRule": "#'tradeType'] == 'OTCOption' && #'region'] == 'US'",
              "classificationResult": true,
              "evaluationOrder": 1
            }
          ],
          "selectedScenario": "otc-option-us",
          "selectionReason": "First matching classification rule"
        }
      ],
      "fallbackToDefault": false,
      "defaultScenario": "generic-trade-processing"
    },
    "scenarioSelection": {
      "classificationRule": "#'tradeType'] == 'OTCOption' && #'region'] == 'US'",
      "classificationResult": true,
      "alternativesConsidered": ["scenario-fx-spot", "scenario-equity-option"],
      "selectionReason": "Classification rule matched: US OTC Option trade",
      "confidence": 0.95
    },
    "executionStrategy": {
      "stageExecutionMode": "sequential",
      "failurePolicyApplied": "none",
      "dependencyResolution": "all-dependencies-met"
    }
  },
  "children": [
    {
      "name": "discovery-stage",
      "id": "exec-stage-001",
      "path": "scenarios/otc-options-validation.yaml#discovery",
      "type": "stage-execution",
      "depth": 1,
      "height": 4,
      "childCount": 2,
      "executionOrder": 1,
      "status": "SUCCESS",
      "startTime": "2025-10-27T10:15:30.125Z",
      "endTime": "2025-10-27T10:15:30.200Z",
      "durationMs": 75,
      "decisionData": {
        "stageExecution": {
          "dependencyCheck": {
            "dependsOn": [],
            "allDependenciesMet": true,
            "skippedDependencies": []
          },
          "failurePolicy": "continue-with-warnings",
          "executionDecision": "execute",
          "executionReason": "No dependencies, stage is required"
        }
      },
      "children": [
        {
          "name": "enrichment-group-market-data",
          "id": "exec-enrich-001",
          "path": "enrichments/market-data.yaml",
          "type": "enrichment-execution",
          "depth": 2,
          "height": 3,
          "childCount": 3,
          "executionOrder": 1,
          "status": "SUCCESS",
          "startTime": "2025-10-27T10:15:30.130Z",
          "endTime": "2025-10-27T10:15:30.160Z",
          "durationMs": 30,
          "decisionData": {
            "enrichmentGroupExecution": {
              "executionMode": "sequential",
              "parallelExecution": false,
              "stopOnFirstFailure": false,
              "groupCondition": "#underlying != null",
              "groupConditionResult": true,
              "executionDecision": "execute-group",
              "enrichmentsInGroup": [
                {
                  "enrichmentId": "enrich-spot-price",
                  "executionOrder": 1,
                  "skipped": false,
                  "skipReason": null
                },
                {
                  "enrichmentId": "enrich-volatility",
                  "executionOrder": 2,
                  "skipped": false,
                  "skipReason": null
                },
                {
                  "enrichmentId": "enrich-risk-free-rate",
                  "executionOrder": 3,
                  "skipped": false,
                  "skipReason": null
                }
              ],
              "groupResult": "SUCCESS",
              "terminationReason": "all-enrichments-completed"
            }
          },
          "executionSummary": {
            "enrichmentsProcessed": 3,
            "enrichmentsSucceeded": 3,
            "enrichmentsFailed": 0,
            "dataFieldsEnriched": ["spotPrice", "volatility", "riskFreeRate"]
          },
          "children": [
            {
              "name": "enrich-spot-price",
              "id": "exec-enrich-001-1",
              "type": "enrichment-step",
              "depth": 3,
              "executionOrder": 1,
              "status": "SUCCESS",
              "durationMs": 10,
              "inputData": {"underlying": "AAPL"},
              "outputData": {"spotPrice": 175.50},
              "dataSource": "market-data-service",
              "decisionData": {
                "enrichmentStep": {
                  "conditionEvaluation": {
                    "condition": "#underlying != null",
                    "conditionResult": true,
                    "inputValues": {"underlying": "AAPL"}
                  },
                  "lookupDecision": {
                    "dataSourceSelected": "market-data-service",
                    "lookupKey": "AAPL",
                    "cacheHit": false,
                    "fallbackUsed": false
                  }
                }
              }
            }
          ]
        },
        {
          "name": "rule-group-instrument-validation",
          "id": "exec-rule-001",
          "path": "rules/instrument-validation.yaml",
          "type": "rule-group-execution",
          "depth": 2,
          "height": 2,
          "childCount": 4,
          "executionOrder": 2,
          "status": "SUCCESS",
          "startTime": "2025-10-27T10:15:30.165Z",
          "endTime": "2025-10-27T10:15:30.195Z",
          "durationMs": 30,
          "decisionData": {
            "ruleGroupExecution": {
              "executionMode": "sequential",
              "operator": "AND",
              "stopOnFirstFailure": false,
              "shortCircuitEnabled": true,
              "rulesEvaluated": [
                {
                  "ruleName": "validate-underlying-exists",
                  "condition": "underlying != null && underlying.length() > 0",
                  "conditionResult": true,
                  "executionDecision": "continue",
                  "shortCircuitTriggered": false
                }
              ]
            }
          },
          "executionSummary": {
            "rulesEvaluated": 4,
            "rulesPassed": 4,
            "rulesFailed": 0,
            "triggered": true,
            "severity": "INFO"
          },
          "children": [
            {
              "name": "validate-underlying-exists",
              "id": "exec-rule-001-1",
              "type": "rule-execution",
              "depth": 3,
              "executionOrder": 1,
              "status": "PASSED",
              "durationMs": 5,
              "condition": "underlying != null && underlying.length() > 0",
              "result": true,
              "message": "Underlying instrument is valid",
              "severity": "INFO",
              "decisionData": {
                "ruleExecution": {
                  "conditionEvaluation": {
                    "originalCondition": "underlying != null && underlying.length() > 0",
                    "evaluatedCondition": "'AAPL' != null && 'AAPL'.length() > 0",
                    "conditionResult": true,
                    "inputValues": {"underlying": "AAPL"}
                  },
                  "executionPath": "condition-passed",
                  "severityAssignment": {
                    "configuredSeverity": "INFO",
                    "finalSeverity": "INFO",
                    "severityReason": "Rule passed with configured severity"
                  }
                }
              }
            }
          ]
        }
      ]
    }
  ],
  "executionSummary": {
    "scenarioId": "scenario-otc-options-validation",
    "executionStatus": "SUCCESSFUL",
    "totalDurationMs": 333
  },
  "healthScore": 100,
  "circular": false,
  "warnings": [],
  "reviewFlags": []
}
```

---

## Key Properties Mapping

### Common Properties (Shared with Dependency Tree)

| Property | Type | Description | Example |
|----------|------|-------------|---------|
| `name` | String | Human-readable name | `"discovery-stage"` |
| `id` | String | Unique execution identifier | `"exec-uuid-12345"` |
| `path` | String | YAML file path (links to config) | `"scenarios/otc-options.yaml"` |
| `type` | String | Node type (execution-specific) | `"scenario-execution"` |
| `depth` | Integer | Tree depth (0=root) | `0` |
| `height` | Integer | Distance to deepest leaf | `5` |
| `childCount` | Integer | Number of direct children | `3` |
| `children` | Array | Nested child nodes | `[...]` |
| `healthScore` | Integer | Execution health (0-100) | `100` |
| `circular` | Boolean | Circular execution detected | `false` |

### Execution-Specific Properties

| Property | Type | Description | Example |
|----------|------|-------------|---------|
| `executionOrder` | Integer | Sequence in parent context | `1` |
| `status` | String | Execution status | `"SUCCESS"`, `"FAILURE"`, `"SKIPPED"` |
| `startTime` | ISO8601 | Execution start timestamp | `"2025-10-27T10:15:30.123Z"` |
| `endTime` | ISO8601 | Execution end timestamp | `"2025-10-27T10:15:30.456Z"` |
| `durationMs` | Long | Execution duration in milliseconds | `333` |
| `datasetId` | String | Unique identifier for the dataset being processed (optional) | `"trade-12345"`, `"batch-2025-10-27-001"` |
| `datasetIdName` | String | External system name/label for the dataset ID (optional) | `"trading-system-xx-trade-id"`, `"murex-trade-ref"` |
| `executionSummary` | Object | Type-specific execution summary | See below |
| `inputData` | Object | Input data snapshot (optional) | `{"underlying": "AAPL"}` |
| `outputData` | Object | Output data snapshot (optional) | `{"spotPrice": 175.50}` |
| `warnings` | Array | Warning messages | `["SLA exceeded"]` |
| `reviewFlags` | Array | Manual review flags | `[]` |
| `decisionData` | Object | Decision-specific data (optional) | See Decision Data Types below |

### Node Types

| Type | Description | Parent Types | Children Types |
|------|-------------|--------------|----------------|
| `scenario-execution` | Root scenario execution | None | `stage-execution` |
| `stage-execution` | Scenario stage execution | `scenario-execution` | `rule-group-execution`, `enrichment-execution` |
| `rule-group-execution` | Rule group evaluation | `stage-execution` | `rule-execution` |
| `rule-execution` | Individual rule evaluation | `rule-group-execution` | None (leaf) |
| `enrichment-execution` | Enrichment group processing | `stage-execution` | `enrichment-step` |
| `enrichment-step` | Individual enrichment | `enrichment-execution` | None (leaf) |

---

## Decision Data Types

The execution trace API captures decision data at multiple levels of the APEX execution hierarchy. Each level represents different types of decisions made during processing, from high-level scenario routing down to individual field transformations.

### Decision Data Hierarchy

The complete decision hierarchy in APEX includes:

1. **Scenario Registry Level**: How scenarios are selected from the registry
2. **Scenario Level**: Individual scenario classification and execution strategy
3. **Stage Level**: Stage dependency evaluation and failure policy application
4. **Rule Group Level**: Group execution modes, operators, and short-circuit logic
5. **Rule Level**: Individual rule condition evaluation and execution paths
6. **Enrichment Group Level**: Group condition evaluation and enrichment orchestration
7. **Enrichment Level**: Individual enrichment conditions and conditional mappings
8. **Enrichment Step Level**: Lookup decisions, transformations, and data source selection

### Scenario Registry Decision Data

**Purpose**: Captures how APEX selects a scenario from the registry based on incoming data.

**Key Decision Points**:
- Data type determination and direct mapping attempts
- Classification rule evaluation across all registered scenarios
- Routing rule evaluation (if configured)
- Default scenario fallback logic

**Implementation Location**: `DataTypeScenarioService.getScenarioForData()`

```json
{
  "scenarioRegistryRouting": {
    "routingStrategy": "classification-based",
    "dataType": "HashMap",
    "dataTypeSource": "data.getClass().getSimpleName()",
    "registryConfiguration": {
      "totalScenariosRegistered": 5,
      "scenariosWithClassificationRules": 3,
      "scenariosWithDataTypes": 2,
      "defaultScenarioConfigured": true
    },
    "routingDecisions": [
      {
        "routingMethod": "direct-data-type-mapping",
        "dataTypeMatched": false,
        "availableScenarios": [],
        "reason": "No scenarios registered for data type 'HashMap'"
      },
      {
        "routingMethod": "routing-rules-evaluation",
        "routingRulesConfigured": false,
        "skipped": true,
        "reason": "No routing rules configured in registry"
      },
      {
        "routingMethod": "classification-rule-evaluation",
        "scenariosEvaluated": [
          {
            "scenarioId": "otc-option-us",
            "classificationRule": "#'tradeType'] == 'OTCOption' && #'region'] == 'US'",
            "classificationResult": true,
            "evaluationOrder": 1,
            "evaluationTimeMs": 2,
            "inputValues": {
              "tradeType": "OTCOption",
              "region": "US"
            }
          },
          {
            "scenarioId": "fx-spot-processing",
            "classificationRule": "#'tradeType'] == 'SPOT'",
            "classificationResult": false,
            "evaluationOrder": 2,
            "evaluationTimeMs": 1,
            "inputValues": {
              "tradeType": "OTCOption"
            }
          },
          {
            "scenarioId": "bond-processing",
            "classificationRule": "#'instrumentType'] == 'BOND'",
            "classificationResult": false,
            "evaluationOrder": 3,
            "evaluationTimeMs": 1,
            "inputValues": {
              "instrumentType": null
            }
          }
        ],
        "selectedScenario": "otc-option-us",
        "selectionReason": "First matching classification rule",
        "shortCircuitEnabled": true,
        "shortCircuitTriggered": true
      }
    ],
    "fallbackToDefault": false,
    "defaultScenario": "generic-trade-processing",
    "finalDecision": {
      "selectedScenario": "otc-option-us",
      "selectionMethod": "classification-rule-evaluation",
      "confidence": 1.0,
      "totalEvaluationTimeMs": 4
    }
  }
}
```

### Scenario-Level Decision Data

**Purpose**: Captures decisions made within a specific scenario after it has been selected.

**Key Decision Points**:
- Individual scenario classification rule validation
- Execution strategy determination (sequential vs parallel stages)
- Stage dependency resolution
- Failure policy application

**Implementation Location**: `ScenarioConfiguration.matchesClassificationRule()`, `ScenarioStageExecutor.executeStages()`

```json
{
  "scenarioSelection": {
    "scenarioId": "otc-option-us",
    "classificationRule": "#'tradeType'] == 'OTCOption' && #'region'] == 'US'",
    "classificationResult": true,
    "classificationConfidence": 0.95,
    "alternativesConsidered": ["scenario-fx-spot", "scenario-equity-option"],
    "selectionReason": "Classification rule matched: US OTC Option trade",
    "fallbackUsed": false,
    "validationPerformed": true,
    "validationTimeMs": 1
  },
  "executionStrategy": {
    "stageExecutionMode": "sequential",
    "parallelExecutionEnabled": false,
    "failurePolicyApplied": "none",
    "dependencyResolution": "all-dependencies-met",
    "totalStagesConfigured": 3,
    "requiredStages": 2,
    "optionalStages": 1
  },
  "scenarioMetadata": {
    "businessDomain": "Derivatives Trading",
    "owner": "derivatives.team@company.com",
    "version": "1.2.0",
    "lastModified": "2025-10-15T14:30:00Z"
  }
}
```

### Stage-Level Decision Data

**Purpose**: Captures decisions made for individual stage execution within a scenario.

**Key Decision Points**:
- Stage dependency evaluation and resolution
- Failure policy application and routing decisions
- Conditional execution path selection
- Stage skipping logic

**Implementation Location**: `ScenarioStageExecutor.shouldExecuteStage()`, `ScenarioStageExecutor.executeStage()`

```json
{
  "stageExecution": {
    "stageName": "enrichment-stage",
    "executionOrder": 2,
    "required": false,
    "dependencyCheck": {
      "dependsOn": ["validation-stage"],
      "allDependenciesMet": true,
      "skippedDependencies": [],
      "dependencyResults": {
        "validation-stage": "SUCCESS"
      },
      "dependencyEvaluationTimeMs": 1
    },
    "failurePolicy": "continue-with-warnings",
    "failurePolicySource": "stage-configuration",
    "executionDecision": "execute",
    "executionReason": "All dependencies met, stage is required",
    "conditionalExecution": {
      "hasCondition": true,
      "condition": "#riskLevel == 'HIGH'",
      "conditionResult": true,
      "conditionEvaluationTimeMs": 2,
      "pathTaken": "on-true",
      "alternativePath": "on-false",
      "inputValues": {
        "riskLevel": "HIGH"
      }
    },
    "stageMetadata": {
      "configFile": "config/enrichment-rules.yaml",
      "description": "Enrich trade with reference data",
      "slaMs": 5000
    }
  }
}
```

### Rule Group Decision Data
```json
{
  "ruleGroupExecution": {
    "executionMode": "sequential",
    "operator": "AND",
    "stopOnFirstFailure": true,
    "shortCircuitEnabled": true,
    "parallelExecution": false,
    "rulesEvaluated": [
      {
        "ruleName": "validate-amount",
        "condition": "#amount > 0",
        "conditionResult": true,
        "executionDecision": "continue",
        "shortCircuitTriggered": false
      }
    ],
    "groupResult": true,
    "terminationReason": "all-rules-passed"
  }
}
```

### Rule-Level Decision Data

**Purpose**: Captures decisions made during individual rule evaluation with actual dataset values.

**Key Decision Points**:
- Rule condition evaluation with runtime data values
- Execution path selection (condition passed/failed)
- Severity assignment and escalation decisions
- Validation results and error messages
- Short-circuit logic and rule chaining

**Implementation Location**: `RuleEngineService.evaluateRule()`, `RuleProcessor.processRule()`

#### Validation Rule Evaluation Example

**YAML Rule Definition:**
```yaml
rules:
  - name: "validate-trade-amount"
    condition: "#amount > 0 && #amount <= 10000000"
    severity: "ERROR"
    message: "Trade amount must be between 0 and 10M"

  - name: "validate-counterparty-rating"
    condition: "#counterpartyRating != null && #counterpartyRating >= 'BBB'"
    severity: "WARNING"
    message: "Counterparty rating below investment grade"
```

**Runtime Decision Data for Dataset #1 (Pass Case):**
```json
{
  "ruleExecution": {
    "ruleName": "validate-trade-amount",
    "ruleDefinition": {
      "originalCondition": "#amount > 0 && #amount <= 10000000",
      "configuredSeverity": "ERROR",
      "configuredMessage": "Trade amount must be between 0 and 10M"
    },
    "datasetEvaluation": {
      "datasetId": "trade-12345",
      "inputValues": {
        "amount": 1500000
      },
      "conditionEvaluation": {
        "evaluatedCondition": "1500000 > 0 && 1500000 <= 10000000",
        "subConditionResults": [
          {
            "expression": "#amount > 0",
            "evaluatedAs": "1500000 > 0",
            "result": true
          },
          {
            "expression": "#amount <= 10000000",
            "evaluatedAs": "1500000 <= 10000000",
            "result": true
          }
        ],
        "finalConditionResult": true,
        "evaluationTimeMs": 1
      },
      "executionPath": "condition-passed",
      "validationResult": {
        "status": "PASS",
        "severity": null,
        "message": null,
        "errorCode": null
      }
    }
  }
}
```

**Runtime Decision Data for Dataset #2 (Failure Case):**
```json
{
  "ruleExecution": {
    "ruleName": "validate-trade-amount",
    "ruleDefinition": {
      "originalCondition": "#amount > 0 && #amount <= 10000000",
      "configuredSeverity": "ERROR",
      "configuredMessage": "Trade amount must be between 0 and 10M"
    },
    "datasetEvaluation": {
      "datasetId": "trade-67890",
      "inputValues": {
        "amount": 15000000
      },
      "conditionEvaluation": {
        "evaluatedCondition": "15000000 > 0 && 15000000 <= 10000000",
        "subConditionResults": [
          {
            "expression": "#amount > 0",
            "evaluatedAs": "15000000 > 0",
            "result": true
          },
          {
            "expression": "#amount <= 10000000",
            "evaluatedAs": "15000000 <= 10000000",
            "result": false
          }
        ],
        "finalConditionResult": false,
        "evaluationTimeMs": 1
      },
      "executionPath": "condition-failed",
      "validationResult": {
        "status": "FAIL",
        "severity": "ERROR",
        "message": "Trade amount must be between 0 and 10M",
        "errorCode": "AMOUNT_EXCEEDS_LIMIT",
        "failedValue": 15000000,
        "expectedRange": "0 to 10000000"
      }
    }
  }
}
```

#### Key Aspects of Validation Rule Capture

**1. Rule Definition vs Runtime Evaluation**
- **Rule Definition**: Static YAML configuration that defines the validation logic
- **Runtime Evaluation**: Actual execution with specific dataset values showing what happened

**2. Dataset-Specific Evaluation**
- Each dataset that passes through the rule gets its own `datasetEvaluation` section
- Same rule definition, different input values and results for each dataset
- Complete traceability from input values to final validation result

**3. Sub-Condition Breakdown**
- Complex conditions (with && or ||) are broken down into individual expressions
- Each sub-condition shows its evaluation with actual values
- Enables precise debugging of which part of a complex condition failed

**4. Validation Result Details**
- **Pass Cases**: Show that validation succeeded with null severity/message
- **Fail Cases**: Show configured severity, error message, error codes, and failed values
- **Expected vs Actual**: Clear indication of what was expected vs what was received

**5. Multiple Datasets Through Same Rule**
```json
// Same rule "validate-trade-amount" evaluated for different datasets:

// Dataset 1: $1.5M trade (PASS)
"inputValues": {"amount": 1500000}
"finalConditionResult": true
"validationResult": {"status": "PASS"}

// Dataset 2: $15M trade (FAIL)
"inputValues": {"amount": 15000000}
"finalConditionResult": false
"validationResult": {"status": "FAIL", "severity": "ERROR"}

// Dataset 3: -$100K trade (FAIL)
"inputValues": {"amount": -100000}
"finalConditionResult": false
"validationResult": {"status": "FAIL", "severity": "ERROR"}
```

This approach captures the complete evaluation path for every dataset that flows through each validation rule, providing full traceability and debugging capability.

#### Complete Multi-Dataset Example

**Scenario**: Three different trades processed through the same validation rule

**YAML Rule Definition:**
```yaml
rules:
  - name: "validate-trade-amount"
    condition: "#amount > 0 && #amount <= 10000000"
    severity: "ERROR"
    message: "Trade amount must be between 0 and 10M"
```

**Execution Trace Node for Rule:**
```json
{
  "name": "validate-trade-amount",
  "id": "rule-exec-uuid-789",
  "type": "rule-execution",
  "executionStatus": "COMPLETED",
  "startTime": "2025-10-27T10:15:30.123Z",
  "endTime": "2025-10-27T10:15:30.127Z",
  "decisionData": {
    "ruleExecution": {
      "ruleName": "validate-trade-amount",
      "ruleDefinition": {
        "originalCondition": "#amount > 0 && #amount <= 10000000",
        "configuredSeverity": "ERROR",
        "configuredMessage": "Trade amount must be between 0 and 10M"
      },
      "datasetEvaluations": [
        {
          "datasetId": "trade-12345",
          "datasetIdName": "US Equity Option Trade",
          "inputValues": {
            "amount": 1500000,
            "currency": "USD",
            "tradeType": "OPTION"
          },
          "conditionEvaluation": {
            "evaluatedCondition": "1500000 > 0 && 1500000 <= 10000000",
            "subConditionResults": [
              {
                "expression": "#amount > 0",
                "evaluatedAs": "1500000 > 0",
                "result": true
              },
              {
                "expression": "#amount <= 10000000",
                "evaluatedAs": "1500000 <= 10000000",
                "result": true
              }
            ],
            "finalConditionResult": true,
            "evaluationTimeMs": 1
          },
          "executionPath": "condition-passed",
          "validationResult": {
            "status": "PASS",
            "severity": null,
            "message": null,
            "errorCode": null
          }
        },
        {
          "datasetId": "trade-67890",
          "datasetIdName": "Large Corporate Bond Trade",
          "inputValues": {
            "amount": 15000000,
            "currency": "USD",
            "tradeType": "BOND"
          },
          "conditionEvaluation": {
            "evaluatedCondition": "15000000 > 0 && 15000000 <= 10000000",
            "subConditionResults": [
              {
                "expression": "#amount > 0",
                "evaluatedAs": "15000000 > 0",
                "result": true
              },
              {
                "expression": "#amount <= 10000000",
                "evaluatedAs": "15000000 <= 10000000",
                "result": false
              }
            ],
            "finalConditionResult": false,
            "evaluationTimeMs": 1
          },
          "executionPath": "condition-failed",
          "validationResult": {
            "status": "FAIL",
            "severity": "ERROR",
            "message": "Trade amount must be between 0 and 10M",
            "errorCode": "AMOUNT_EXCEEDS_LIMIT",
            "failedValue": 15000000,
            "expectedRange": "0 to 10000000"
          }
        },
        {
          "datasetId": "trade-11111",
          "datasetIdName": "Invalid Negative Trade",
          "inputValues": {
            "amount": -100000,
            "currency": "USD",
            "tradeType": "EQUITY"
          },
          "conditionEvaluation": {
            "evaluatedCondition": "-100000 > 0 && -100000 <= 10000000",
            "subConditionResults": [
              {
                "expression": "#amount > 0",
                "evaluatedAs": "-100000 > 0",
                "result": false
              },
              {
                "expression": "#amount <= 10000000",
                "evaluatedAs": "-100000 <= 10000000",
                "result": true
              }
            ],
            "finalConditionResult": false,
            "evaluationTimeMs": 1
          },
          "executionPath": "condition-failed",
          "validationResult": {
            "status": "FAIL",
            "severity": "ERROR",
            "message": "Trade amount must be between 0 and 10M",
            "errorCode": "AMOUNT_NEGATIVE",
            "failedValue": -100000,
            "expectedRange": "0 to 10000000"
          }
        }
      ],
      "ruleExecutionSummary": {
        "totalDatasetsEvaluated": 3,
        "datasetsPassedValidation": 1,
        "datasetsFailedValidation": 2,
        "totalEvaluationTimeMs": 3,
        "mostCommonFailureReason": "AMOUNT_EXCEEDS_LIMIT"
      }
    }
  },
  "children": []
}
```

This comprehensive example shows how the same validation rule processes multiple datasets, capturing the complete evaluation path for each one with actual runtime values and results.

## Implementation Strategy for Dataset-Specific Rule Evaluation

### Core Implementation Approach

#### 1. Rule Execution Context Enhancement

**Current APEX Rule Processing:**
```java
// Existing approach - single evaluation per rule
RuleResult evaluateRule(Rule rule, Object data) {
    // Evaluate condition against data
    // Return single result
}
```

**Enhanced for Execution Trace:**
```java
// Enhanced approach - capture detailed evaluation data
RuleExecutionTrace evaluateRuleWithTrace(Rule rule, Object data, String datasetId) {
    RuleExecutionTrace trace = new RuleExecutionTrace();

    // Capture rule definition
    trace.setRuleDefinition(rule.getCondition(), rule.getSeverity(), rule.getMessage());

    // Capture dataset context
    trace.setDatasetContext(datasetId, extractDatasetName(data), extractInputValues(data));

    // Evaluate with sub-condition breakdown
    ConditionEvaluationResult evaluation = evaluateConditionWithBreakdown(rule.getCondition(), data);
    trace.setConditionEvaluation(evaluation);

    // Capture validation result
    ValidationResult result = createValidationResult(evaluation, rule);
    trace.setValidationResult(result);

    return trace;
}
```

#### 2. Sub-Condition Evaluation Engine

**SpEL Expression Breakdown:**
```java
public class ConditionEvaluationEngine {

    public ConditionEvaluationResult evaluateWithBreakdown(String condition, Object data) {
        // Parse complex condition into sub-expressions
        List<SubCondition> subConditions = parseCondition(condition);

        List<SubConditionResult> subResults = new ArrayList<>();
        for (SubCondition subCondition : subConditions) {
            SubConditionResult subResult = new SubConditionResult();
            subResult.setExpression(subCondition.getExpression());
            subResult.setEvaluatedAs(substituteValues(subCondition.getExpression(), data));
            subResult.setResult(evaluateExpression(subCondition.getExpression(), data));
            subResults.add(subResult);
        }

        // Evaluate final condition
        boolean finalResult = evaluateFinalCondition(condition, data);

        return new ConditionEvaluationResult(
            substituteValues(condition, data),
            subResults,
            finalResult
        );
    }

    private String substituteValues(String expression, Object data) {
        // Replace #field references with actual values
        // "#amount > 0" becomes "1500000 > 0"
        return expressionSubstituter.substitute(expression, data);
    }
}
```

#### 3. Multi-Dataset Aggregation

**Rule-Level Aggregation:**
```java
public class RuleExecutionAggregator {

    private List<RuleExecutionTrace> datasetEvaluations = new ArrayList<>();

    public void addDatasetEvaluation(RuleExecutionTrace trace) {
        datasetEvaluations.add(trace);
    }

    public RuleExecutionSummary createSummary() {
        return RuleExecutionSummary.builder()
            .totalDatasetsEvaluated(datasetEvaluations.size())
            .datasetsPassedValidation(countPassed())
            .datasetsFailedValidation(countFailed())
            .totalEvaluationTimeMs(sumEvaluationTime())
            .mostCommonFailureReason(findMostCommonFailure())
            .build();
    }
}
```

#### 4. Integration Points in APEX Core

**Key Integration Locations:**

1. **`RuleEngineService.evaluateRule()`**:
   - Wrap existing rule evaluation with trace capture
   - Collect dataset-specific evaluation data
   - Aggregate multiple dataset evaluations per rule

2. **`ScenarioStageExecutor.executeStage()`**:
   - Coordinate rule execution tracing across all rules in a stage
   - Maintain dataset context throughout stage execution

3. **`DataTypeScenarioService.processData()`**:
   - Assign unique dataset IDs for traceability
   - Pass dataset context through entire execution pipeline

#### 5. Performance Considerations

**Conditional Trace Capture:**
```java
@Service
public class ExecutionTraceService {

    @Value("${apex.trace.enabled:false}")
    private boolean traceEnabled;

    @Value("${apex.trace.detailed-conditions:false}")
    private boolean detailedConditionTrace;

    public RuleExecutionTrace captureRuleExecution(Rule rule, Object data, String datasetId) {
        if (!traceEnabled) {
            return null; // No performance impact when disabled
        }

        RuleExecutionTrace trace = new RuleExecutionTrace();

        if (detailedConditionTrace) {
            // Full sub-condition breakdown (more expensive)
            trace.setConditionEvaluation(evaluateWithFullBreakdown(rule, data));
        } else {
            // Basic evaluation only (faster)
            trace.setConditionEvaluation(evaluateBasic(rule, data));
        }

        return trace;
    }
}
```

This implementation strategy enables comprehensive dataset-specific rule evaluation capture while maintaining performance and providing configurable levels of detail.

## YAML Configuration Alignment Requirements

### Overview

To have the execution trace precisely aligned to the YAML configuration, the trace must capture **every YAML configuration element** that influences execution decisions. This ensures complete traceability from static configuration to runtime behavior.

### Required YAML Configuration Elements in Execution Trace

#### 1. **Metadata Alignment**

**YAML Configuration:**
```yaml
metadata:
  id: trade-processing-scenario
  name: Trade Processing Scenario
  version: 1.0.0
  type: scenario
  author: apex.demo@company.com
  business-domain: Trading
  owner: trading.team@company.com
  tags: [scenario, trade-processing, end-to-end]
```

**Execution Trace Requirement:**
```json
{
  "name": "trade-processing-scenario",
  "id": "exec-uuid-12345",
  "yamlConfiguration": {
    "configId": "trade-processing-scenario",
    "configName": "Trade Processing Scenario",
    "configVersion": "1.0.0",
    "configType": "scenario",
    "configAuthor": "apex.demo@company.com",
    "businessDomain": "Trading",
    "owner": "trading.team@company.com",
    "tags": ["scenario", "trade-processing", "end-to-end"],
    "configFilePath": "scenarios/06-trade-processing-scenario.yaml"
  }
}
```

#### 2. **File Dependency Alignment**

**YAML Configuration:**
```yaml
rule-configurations:
  - 02-validation-groups.yaml
enrichment-refs:
  - 03-enrichment-rules.yaml
  - 05-database-lookups.yaml
config-files:
  - 70-config-db.yaml
```

**Execution Trace Requirement:**
```json
{
  "yamlConfiguration": {
    "fileDependencies": {
      "ruleConfigurations": ["02-validation-groups.yaml"],
      "enrichmentRefs": ["03-enrichment-rules.yaml", "05-database-lookups.yaml"],
      "configFiles": ["70-config-db.yaml"]
    },
    "resolvedDependencies": {
      "02-validation-groups.yaml": {
        "loaded": true,
        "loadTime": "2025-10-27T10:15:29.123Z",
        "ruleGroupsLoaded": ["mandatory-trade-validation", "optional-checks"],
        "rulesLoaded": ["trade-amount-validation", "counterparty-validation"]
      }
    }
  }
}
```

#### 3. **Scenario Configuration Alignment**

**YAML Configuration:**
```yaml
scenario:
  scenario-id: trade-processing
  description: Complete trade processing workflow
  classification-rule:
    condition: "#tradeType != null && (#tradeType == 'SPOT' || #tradeType == 'FORWARD')"
    description: Route FX trades to processing
  processing-stages:
    - stage-name: validation
      description: Validate incoming trade data
      config-file: 02-validation-groups.yaml
      execution-order: 1
      failure-policy: terminate
  data-types:
    - HashMap
    - Map
```

**Execution Trace Requirement:**
```json
{
  "yamlConfiguration": {
    "scenarioConfig": {
      "scenarioId": "trade-processing",
      "description": "Complete trade processing workflow",
      "classificationRule": {
        "condition": "#tradeType != null && (#tradeType == 'SPOT' || #tradeType == 'FORWARD')",
        "description": "Route FX trades to processing"
      },
      "processingStages": [
        {
          "stageName": "validation",
          "description": "Validate incoming trade data",
          "configFile": "02-validation-groups.yaml",
          "executionOrder": 1,
          "failurePolicy": "terminate"
        }
      ],
      "dataTypes": ["HashMap", "Map"]
    }
  }
}
```

#### 4. **Rule Group Configuration Alignment**

**YAML Configuration:**
```yaml
rule-groups:
  - id: mandatory-trade-validation
    name: Mandatory Trade Validation
    description: Essential validation rules for all trades
    operator: AND
    stop-on-first-failure: true
    parallel-execution: false
    priority: 10
    rule-ids:
      - trade-amount-validation
      - counterparty-validation
    rule-references:
      - rule-id: trade-amount-validation
        sequence: 1
        enabled: true
        override-priority: 5
```

**Execution Trace Requirement:**
```json
{
  "yamlConfiguration": {
    "ruleGroupConfig": {
      "id": "mandatory-trade-validation",
      "name": "Mandatory Trade Validation",
      "description": "Essential validation rules for all trades",
      "operator": "AND",
      "stopOnFirstFailure": true,
      "parallelExecution": false,
      "priority": 10,
      "ruleIds": ["trade-amount-validation", "counterparty-validation"],
      "ruleReferences": [
        {
          "ruleId": "trade-amount-validation",
          "sequence": 1,
          "enabled": true,
          "overridePriority": 5
        }
      ]
    }
  }
}
```

#### 5. **Rule Configuration Alignment**

**YAML Configuration:**
```yaml
rules:
  - id: trade-amount-validation
    name: Trade Amount Validation
    description: Validates trade amount is within acceptable limits
    condition: "#amount > 0 && #amount <= 10000000"
    severity: ERROR
    message: "Trade amount must be between 0 and 10M"
    priority: 5
    enabled: true
    category: validation
    tags: [amount, validation, critical]
    business-domain: Trading
    business-owner: trading.team@company.com
```

**Execution Trace Requirement:**
```json
{
  "yamlConfiguration": {
    "ruleConfig": {
      "id": "trade-amount-validation",
      "name": "Trade Amount Validation",
      "description": "Validates trade amount is within acceptable limits",
      "condition": "#amount > 0 && #amount <= 10000000",
      "severity": "ERROR",
      "message": "Trade amount must be between 0 and 10M",
      "priority": 5,
      "enabled": true,
      "category": "validation",
      "tags": ["amount", "validation", "critical"],
      "businessDomain": "Trading",
      "businessOwner": "trading.team@company.com"
    }
  }
}
```

#### 6. **Enrichment Configuration Alignment**

**YAML Configuration:**
```yaml
enrichments:
  - id: trade-reference-lookup
    name: Trade Reference Lookup
    type: lookup-enrichment
    description: Lookup trade reference data from PostgreSQL
    condition: "#tradeId != null"
    severity: WARNING
    lookup-config:
      lookup-key: "#tradeId"
      lookup-dataset:
        type: database
        data-source-ref: trade-database
        query: "SELECT trade_status, booking_date FROM trades WHERE trade_id = :tradeId"
        parameters:
          - field: tradeId
            type: string
    field-mappings:
      - source-field: TRADE_STATUS
        target-field: tradeStatus
      - source-field: BOOKING_DATE
        target-field: bookingDate
```

**Execution Trace Requirement:**
```json
{
  "yamlConfiguration": {
    "enrichmentConfig": {
      "id": "trade-reference-lookup",
      "name": "Trade Reference Lookup",
      "type": "lookup-enrichment",
      "description": "Lookup trade reference data from PostgreSQL",
      "condition": "#tradeId != null",
      "severity": "WARNING",
      "lookupConfig": {
        "lookupKey": "#tradeId",
        "lookupDataset": {
          "type": "database",
          "dataSourceRef": "trade-database",
          "query": "SELECT trade_status, booking_date FROM trades WHERE trade_id = :tradeId",
          "parameters": [
            {"field": "tradeId", "type": "string"}
          ]
        }
      },
      "fieldMappings": [
        {"sourceField": "TRADE_STATUS", "targetField": "tradeStatus"},
        {"sourceField": "BOOKING_DATE", "targetField": "bookingDate"}
      ]
    }
  }
}
```

#### 7. **Data Source Configuration Alignment**

**YAML Configuration:**
```yaml
data-sources:
  - name: trade-database
    type: database
    source-type: postgresql
    description: PostgreSQL database for trade reference data
    connection:
      host: localhost
      port: 5432
      database: apex_trades
      username: apex_user
      password: ${DB_PASSWORD:password}
      pool-size: 10
      timeout: 30000
    queries:
      getTradeById: "SELECT * FROM trades WHERE trade_id = :tradeId"
      getActiveTrades: "SELECT * FROM trades WHERE status = 'ACTIVE'"
    cache:
      enabled: true
      ttlSeconds: 300
      maxSize: 1000
```

**Execution Trace Requirement:**
```json
{
  "yamlConfiguration": {
    "dataSourceConfig": {
      "name": "trade-database",
      "type": "database",
      "sourceType": "postgresql",
      "description": "PostgreSQL database for trade reference data",
      "connection": {
        "host": "localhost",
        "port": 5432,
        "database": "apex_trades",
        "username": "apex_user",
        "poolSize": 10,
        "timeout": 30000
      },
      "queries": {
        "getTradeById": "SELECT * FROM trades WHERE trade_id = :tradeId",
        "getActiveTrades": "SELECT * FROM trades WHERE status = 'ACTIVE'"
      },
      "cache": {
        "enabled": true,
        "ttlSeconds": 300,
        "maxSize": 1000
      }
    }
  }
}
```

#### 8. **Enrichment Group Configuration Alignment**

**YAML Configuration:**
```yaml
enrichment-groups:
  - id: basic-trade-enrichment
    name: Basic Trade Enrichment
    description: Essential enrichment for all trades
    execution-mode: sequential
    stop-on-first-failure: false
    parallel-execution: false
    priority: 10
    enrichment-ids:
      - currency-enrichment
      - trade-classification
    enrichment-references:
      - enrichment-id: currency-enrichment
        sequence: 1
        enabled: true
        override-priority: 5
```

**Execution Trace Requirement:**
```json
{
  "yamlConfiguration": {
    "enrichmentGroupConfig": {
      "id": "basic-trade-enrichment",
      "name": "Basic Trade Enrichment",
      "description": "Essential enrichment for all trades",
      "executionMode": "sequential",
      "stopOnFirstFailure": false,
      "parallelExecution": false,
      "priority": 10,
      "enrichmentIds": ["currency-enrichment", "trade-classification"],
      "enrichmentReferences": [
        {
          "enrichmentId": "currency-enrichment",
          "sequence": 1,
          "enabled": true,
          "overridePriority": 5
        }
      ]
    }
  }
}
```

### Configuration Override and Runtime Resolution

#### Configuration Resolution Chain
```json
{
  "yamlConfiguration": {
    "configurationResolution": {
      "baseConfiguration": {
        "file": "02-validation-groups.yaml",
        "ruleGroupId": "mandatory-trade-validation",
        "originalPriority": 10
      },
      "overrides": [
        {
          "source": "rule-reference",
          "field": "priority",
          "originalValue": 10,
          "overrideValue": 5,
          "reason": "override-priority in rule-reference"
        }
      ],
      "finalConfiguration": {
        "priority": 5,
        "enabled": true,
        "sequence": 1
      }
    }
  }
}
```

### Complete YAML-to-Execution Alignment

Every execution trace node must include:

1. **Original YAML Configuration**: Exact configuration as defined in YAML files
2. **Configuration Resolution**: How overrides and references were resolved
3. **File Dependencies**: Which YAML files were loaded and their dependency chain
4. **Runtime Application**: How the configuration was applied to actual data
5. **Configuration Metadata**: Author, version, business domain, tags, etc.

This ensures **complete traceability** from YAML configuration to runtime execution behavior.

## Complete YAML-Aligned Execution Trace Example

### Scenario: Trade Processing with Complete Configuration Alignment

**YAML Files Structure:**
```
scenarios/
├── 06-trade-processing-scenario.yaml    # Main scenario
├── 02-validation-groups.yaml            # Rule groups
├── 01-base-validation-rules.yaml        # Individual rules
├── 03-enrichment-rules.yaml             # Enrichments
└── 05-database-lookups.yaml             # Data sources
```

**Complete Execution Trace Node with YAML Alignment:**
```json
{
  "name": "trade-processing-scenario",
  "id": "exec-uuid-12345",
  "path": "scenarios/06-trade-processing-scenario.yaml",
  "type": "scenario-execution",
  "executionStatus": "COMPLETED",
  "startTime": "2025-10-27T10:15:30.123Z",
  "endTime": "2025-10-27T10:15:30.456Z",

  "yamlConfiguration": {
    "configId": "trade-processing-scenario",
    "configName": "Trade Processing Scenario",
    "configVersion": "1.0.0",
    "configType": "scenario",
    "configAuthor": "apex.demo@company.com",
    "businessDomain": "Trading",
    "owner": "trading.team@company.com",
    "tags": ["scenario", "trade-processing", "end-to-end"],
    "configFilePath": "scenarios/06-trade-processing-scenario.yaml",

    "fileDependencies": {
      "ruleConfigurations": ["02-validation-groups.yaml"],
      "enrichmentRefs": ["03-enrichment-rules.yaml", "05-database-lookups.yaml"]
    },

    "scenarioConfig": {
      "scenarioId": "trade-processing",
      "description": "Complete trade processing workflow",
      "classificationRule": {
        "condition": "#tradeType != null && (#tradeType == 'SPOT' || #tradeType == 'FORWARD')",
        "description": "Route FX trades to processing"
      },
      "processingStages": [
        {
          "stageName": "validation",
          "description": "Validate incoming trade data",
          "configFile": "02-validation-groups.yaml",
          "executionOrder": 1,
          "failurePolicy": "terminate"
        }
      ],
      "dataTypes": ["HashMap", "Map"]
    }
  },

  "decisionData": {
    "scenarioRegistryRouting": {
      "routingStrategy": "classification-based",
      "dataTypeEvaluated": "HashMap",
      "classificationRuleEvaluation": {
        "condition": "#tradeType != null && (#tradeType == 'SPOT' || #tradeType == 'FORWARD')",
        "evaluatedCondition": "'SPOT' != null && ('SPOT' == 'SPOT' || 'SPOT' == 'FORWARD')",
        "result": true,
        "inputValues": {"tradeType": "SPOT"}
      },
      "scenarioSelected": "trade-processing",
      "selectionReason": "classification-rule-matched"
    }
  },

  "children": [
    {
      "name": "validation",
      "id": "stage-exec-uuid-456",
      "type": "stage-execution",
      "executionStatus": "COMPLETED",

      "yamlConfiguration": {
        "stageConfig": {
          "stageName": "validation",
          "description": "Validate incoming trade data",
          "configFile": "02-validation-groups.yaml",
          "executionOrder": 1,
          "failurePolicy": "terminate"
        },
        "resolvedConfigFile": {
          "filePath": "02-validation-groups.yaml",
          "loaded": true,
          "loadTime": "2025-10-27T10:15:29.123Z",
          "configId": "validation-groups",
          "configType": "rule-config"
        }
      },

      "children": [
        {
          "name": "mandatory-trade-validation",
          "id": "rulegroup-exec-uuid-789",
          "type": "rule-group-execution",
          "executionStatus": "COMPLETED",

          "yamlConfiguration": {
            "ruleGroupConfig": {
              "id": "mandatory-trade-validation",
              "name": "Mandatory Trade Validation",
              "description": "Essential validation rules for all trades",
              "operator": "AND",
              "stopOnFirstFailure": true,
              "parallelExecution": false,
              "priority": 10,
              "ruleIds": ["trade-amount-validation", "counterparty-validation"],
              "ruleReferences": [
                {
                  "ruleId": "trade-amount-validation",
                  "sequence": 1,
                  "enabled": true,
                  "overridePriority": 5
                }
              ]
            },
            "configurationResolution": {
              "baseConfiguration": {
                "file": "02-validation-groups.yaml",
                "originalPriority": 10
              },
              "overrides": [],
              "finalConfiguration": {
                "priority": 10,
                "enabled": true,
                "operator": "AND"
              }
            }
          },

          "decisionData": {
            "ruleGroupExecution": {
              "executionMode": "sequential",
              "operator": "AND",
              "stopOnFirstFailure": true,
              "rulesInGroup": [
                {
                  "ruleId": "trade-amount-validation",
                  "sequence": 1,
                  "enabled": true,
                  "priority": 5
                }
              ],
              "groupResult": "SUCCESS",
              "terminationReason": "all-rules-passed"
            }
          },

          "children": [
            {
              "name": "trade-amount-validation",
              "id": "rule-exec-uuid-101",
              "type": "rule-execution",
              "executionStatus": "COMPLETED",

              "yamlConfiguration": {
                "ruleConfig": {
                  "id": "trade-amount-validation",
                  "name": "Trade Amount Validation",
                  "description": "Validates trade amount is within acceptable limits",
                  "condition": "#amount > 0 && #amount <= 10000000",
                  "severity": "ERROR",
                  "message": "Trade amount must be between 0 and 10M",
                  "priority": 5,
                  "enabled": true,
                  "category": "validation",
                  "tags": ["amount", "validation", "critical"],
                  "businessDomain": "Trading",
                  "businessOwner": "trading.team@company.com"
                },
                "configurationResolution": {
                  "baseConfiguration": {
                    "file": "01-base-validation-rules.yaml",
                    "originalPriority": 10
                  },
                  "overrides": [
                    {
                      "source": "rule-reference",
                      "field": "priority",
                      "originalValue": 10,
                      "overrideValue": 5,
                      "reason": "override-priority in rule-reference"
                    }
                  ],
                  "finalConfiguration": {
                    "priority": 5,
                    "enabled": true,
                    "sequence": 1
                  }
                }
              },

              "decisionData": {
                "ruleExecution": {
                  "ruleName": "trade-amount-validation",
                  "ruleDefinition": {
                    "originalCondition": "#amount > 0 && #amount <= 10000000",
                    "configuredSeverity": "ERROR",
                    "configuredMessage": "Trade amount must be between 0 and 10M"
                  },
                  "datasetEvaluations": [
                    {
                      "datasetId": "trade-12345",
                      "datasetIdName": "SPOT FX Trade",
                      "inputValues": {"amount": 1500000, "tradeType": "SPOT"},
                      "conditionEvaluation": {
                        "evaluatedCondition": "1500000 > 0 && 1500000 <= 10000000",
                        "subConditionResults": [
                          {
                            "expression": "#amount > 0",
                            "evaluatedAs": "1500000 > 0",
                            "result": true
                          },
                          {
                            "expression": "#amount <= 10000000",
                            "evaluatedAs": "1500000 <= 10000000",
                            "result": true
                          }
                        ],
                        "finalConditionResult": true,
                        "evaluationTimeMs": 1
                      },
                      "executionPath": "condition-passed",
                      "validationResult": {
                        "status": "PASS",
                        "severity": null,
                        "message": null,
                        "errorCode": null
                      }
                    }
                  ]
                }
              },

              "children": []
            }
          ]
        }
      ]
    }
  ]
}
```

This complete example demonstrates **perfect alignment** between YAML configuration and execution trace, capturing every configuration element that influenced the runtime behavior.

## Implementation Requirements for YAML Alignment

### Core Implementation Components

#### 1. **Configuration Capture Service**

```java
@Service
public class YamlConfigurationCaptureService {

    public YamlConfigurationData captureScenarioConfig(YamlScenario scenario, String filePath) {
        return YamlConfigurationData.builder()
            .configId(scenario.getMetadata().getId())
            .configName(scenario.getMetadata().getName())
            .configVersion(scenario.getMetadata().getVersion())
            .configType(scenario.getMetadata().getType())
            .configAuthor(scenario.getMetadata().getAuthor())
            .businessDomain(scenario.getMetadata().getBusinessDomain())
            .owner(scenario.getMetadata().getOwner())
            .tags(scenario.getMetadata().getTags())
            .configFilePath(filePath)
            .fileDependencies(captureFileDependencies(scenario))
            .scenarioConfig(captureScenarioDetails(scenario))
            .build();
    }

    public YamlConfigurationData captureRuleGroupConfig(YamlRuleGroup ruleGroup, String filePath) {
        return YamlConfigurationData.builder()
            .ruleGroupConfig(RuleGroupConfig.builder()
                .id(ruleGroup.getId())
                .name(ruleGroup.getName())
                .description(ruleGroup.getDescription())
                .operator(ruleGroup.getOperator())
                .stopOnFirstFailure(ruleGroup.getStopOnFirstFailure())
                .parallelExecution(ruleGroup.getParallelExecution())
                .priority(ruleGroup.getPriority())
                .ruleIds(ruleGroup.getRuleIds())
                .ruleReferences(captureRuleReferences(ruleGroup.getRuleReferences()))
                .build())
            .build();
    }

    public ConfigurationResolution resolveConfigurationOverrides(
            YamlConfigurationData baseConfig,
            List<Override> overrides) {
        return ConfigurationResolution.builder()
            .baseConfiguration(baseConfig)
            .overrides(overrides)
            .finalConfiguration(applyOverrides(baseConfig, overrides))
            .build();
    }
}
```

#### 2. **Execution Trace Builder with YAML Integration**

```java
@Service
public class ExecutionTraceBuilder {

    @Autowired
    private YamlConfigurationCaptureService configCaptureService;

    public ExecutionTraceNode buildScenarioExecutionNode(
            YamlScenario scenario,
            String filePath,
            ScenarioExecutionResult executionResult) {

        YamlConfigurationData yamlConfig = configCaptureService.captureScenarioConfig(scenario, filePath);

        return ExecutionTraceNode.builder()
            .name(scenario.getMetadata().getId())
            .id(generateExecutionId())
            .path(filePath)
            .type("scenario-execution")
            .executionStatus(executionResult.getStatus())
            .startTime(executionResult.getStartTime())
            .endTime(executionResult.getEndTime())
            .yamlConfiguration(yamlConfig)
            .decisionData(buildScenarioDecisionData(scenario, executionResult))
            .children(buildStageExecutionNodes(scenario, executionResult))
            .build();
    }

    public ExecutionTraceNode buildRuleExecutionNode(
            YamlRule rule,
            String sourceFilePath,
            List<RuleExecutionTrace> datasetEvaluations,
            ConfigurationResolution configResolution) {

        YamlConfigurationData yamlConfig = configCaptureService.captureRuleConfig(rule, sourceFilePath);
        yamlConfig.setConfigurationResolution(configResolution);

        return ExecutionTraceNode.builder()
            .name(rule.getId())
            .id(generateExecutionId())
            .type("rule-execution")
            .yamlConfiguration(yamlConfig)
            .decisionData(buildRuleDecisionData(rule, datasetEvaluations))
            .children(Collections.emptyList())
            .build();
    }
}
```

#### 3. **Configuration Resolution Engine**

```java
@Service
public class ConfigurationResolutionEngine {

    public ConfigurationResolution resolveRuleConfiguration(
            YamlRule baseRule,
            YamlRuleGroup.RuleReference ruleReference,
            String sourceFile) {

        List<ConfigurationOverride> overrides = new ArrayList<>();

        // Check for priority override
        if (ruleReference.getOverridePriority() != null) {
            overrides.add(ConfigurationOverride.builder()
                .source("rule-reference")
                .field("priority")
                .originalValue(baseRule.getPriority())
                .overrideValue(ruleReference.getOverridePriority())
                .reason("override-priority in rule-reference")
                .build());
        }

        // Check for enabled override
        if (ruleReference.getEnabled() != null) {
            overrides.add(ConfigurationOverride.builder()
                .source("rule-reference")
                .field("enabled")
                .originalValue(baseRule.getEnabled())
                .overrideValue(ruleReference.getEnabled())
                .reason("enabled override in rule-reference")
                .build());
        }

        return ConfigurationResolution.builder()
            .baseConfiguration(BaseConfiguration.builder()
                .file(sourceFile)
                .originalPriority(baseRule.getPriority())
                .originalEnabled(baseRule.getEnabled())
                .build())
            .overrides(overrides)
            .finalConfiguration(applyOverrides(baseRule, overrides))
            .build();
    }

    public ConfigurationResolution resolveEnrichmentConfiguration(
            YamlEnrichment baseEnrichment,
            YamlEnrichmentGroup.EnrichmentReference enrichmentReference,
            String sourceFile) {

        // Similar logic for enrichment configuration resolution
        return resolveEnrichmentOverrides(baseEnrichment, enrichmentReference, sourceFile);
    }
}
```

#### 4. **File Dependency Tracker**

```java
@Service
public class FileDependencyTracker {

    private final Map<String, FileDependencyInfo> loadedFiles = new ConcurrentHashMap<>();

    public FileDependencyInfo trackFileLoad(String filePath, String configType) {
        FileDependencyInfo info = FileDependencyInfo.builder()
            .filePath(filePath)
            .configType(configType)
            .loaded(true)
            .loadTime(Instant.now())
            .build();

        loadedFiles.put(filePath, info);
        return info;
    }

    public FileDependencies captureFileDependencies(YamlScenario scenario) {
        return FileDependencies.builder()
            .ruleConfigurations(scenario.getRuleConfigurations())
            .enrichmentRefs(scenario.getEnrichmentRefs())
            .configFiles(scenario.getConfigFiles())
            .resolvedDependencies(resolveAllDependencies(scenario))
            .build();
    }

    private Map<String, FileDependencyInfo> resolveAllDependencies(YamlScenario scenario) {
        Map<String, FileDependencyInfo> resolved = new HashMap<>();

        // Track rule configuration files
        if (scenario.getRuleConfigurations() != null) {
            for (String configFile : scenario.getRuleConfigurations()) {
                FileDependencyInfo info = loadedFiles.get(configFile);
                if (info != null) {
                    resolved.put(configFile, info);
                }
            }
        }

        // Track enrichment reference files
        if (scenario.getEnrichmentRefs() != null) {
            for (String enrichmentFile : scenario.getEnrichmentRefs()) {
                FileDependencyInfo info = loadedFiles.get(enrichmentFile);
                if (info != null) {
                    resolved.put(enrichmentFile, info);
                }
            }
        }

        return resolved;
    }
}
```

### Data Model Classes

#### 5. **YAML Configuration Data Models**

```java
@Data
@Builder
public class YamlConfigurationData {
    private String configId;
    private String configName;
    private String configVersion;
    private String configType;
    private String configAuthor;
    private String businessDomain;
    private String owner;
    private List<String> tags;
    private String configFilePath;

    private FileDependencies fileDependencies;
    private ScenarioConfig scenarioConfig;
    private RuleGroupConfig ruleGroupConfig;
    private RuleConfig ruleConfig;
    private EnrichmentConfig enrichmentConfig;
    private EnrichmentGroupConfig enrichmentGroupConfig;
    private DataSourceConfig dataSourceConfig;

    private ConfigurationResolution configurationResolution;
}

@Data
@Builder
public class FileDependencies {
    private List<String> ruleConfigurations;
    private List<String> enrichmentRefs;
    private List<String> configFiles;
    private Map<String, FileDependencyInfo> resolvedDependencies;
}

@Data
@Builder
public class FileDependencyInfo {
    private String filePath;
    private String configType;
    private boolean loaded;
    private Instant loadTime;
    private List<String> ruleGroupsLoaded;
    private List<String> rulesLoaded;
    private List<String> enrichmentsLoaded;
    private List<String> dataSourcesLoaded;
}

@Data
@Builder
public class ConfigurationResolution {
    private BaseConfiguration baseConfiguration;
    private List<ConfigurationOverride> overrides;
    private FinalConfiguration finalConfiguration;
}

@Data
@Builder
public class ConfigurationOverride {
    private String source;
    private String field;
    private Object originalValue;
    private Object overrideValue;
    private String reason;
}

@Data
@Builder
public class ScenarioConfig {
    private String scenarioId;
    private String description;
    private ClassificationRule classificationRule;
    private List<ProcessingStage> processingStages;
    private List<String> dataTypes;
}

@Data
@Builder
public class RuleGroupConfig {
    private String id;
    private String name;
    private String description;
    private String operator;
    private Boolean stopOnFirstFailure;
    private Boolean parallelExecution;
    private Integer priority;
    private List<String> ruleIds;
    private List<RuleReferenceConfig> ruleReferences;
}

@Data
@Builder
public class RuleConfig {
    private String id;
    private String name;
    private String description;
    private String condition;
    private String severity;
    private String message;
    private Integer priority;
    private Boolean enabled;
    private String category;
    private List<String> tags;
    private String businessDomain;
    private String businessOwner;
}

@Data
@Builder
public class EnrichmentConfig {
    private String id;
    private String name;
    private String type;
    private String description;
    private String condition;
    private String severity;
    private LookupConfig lookupConfig;
    private List<FieldMapping> fieldMappings;
    private List<ConditionalMapping> conditionalMappings;
}

@Data
@Builder
public class DataSourceConfig {
    private String name;
    private String type;
    private String sourceType;
    private String description;
    private ConnectionConfig connection;
    private Map<String, String> queries;
    private CacheConfig cache;
}
```

### Integration Points in APEX Core

#### 6. **ScenarioStageExecutor Integration**

```java
@Service
public class ScenarioStageExecutor {

    @Autowired
    private ExecutionTraceBuilder traceBuilder;

    @Autowired
    private YamlConfigurationCaptureService configCaptureService;

    public ScenarioExecutionResult executeScenario(YamlScenario scenario, Object data, String filePath) {
        // Capture YAML configuration
        YamlConfigurationData yamlConfig = configCaptureService.captureScenarioConfig(scenario, filePath);

        // Execute scenario
        ScenarioExecutionResult result = performScenarioExecution(scenario, data);

        // Build execution trace with YAML alignment
        ExecutionTraceNode traceNode = traceBuilder.buildScenarioExecutionNode(scenario, filePath, result);

        // Store execution trace
        executionTraceService.storeExecutionTrace(traceNode);

        return result;
    }

    public StageExecutionResult executeStage(ProcessingStage stage, YamlScenario scenario, Object data) {
        // Load stage configuration file
        String configFilePath = stage.getConfigFile();
        YamlRuleConfig stageConfig = yamlLoader.loadRuleConfig(configFilePath);

        // Track file dependency
        FileDependencyInfo dependencyInfo = fileDependencyTracker.trackFileLoad(configFilePath, "rule-config");

        // Execute stage with configuration tracking
        StageExecutionResult result = performStageExecution(stage, stageConfig, data);

        // Build stage execution trace with YAML alignment
        ExecutionTraceNode stageTraceNode = traceBuilder.buildStageExecutionNode(stage, stageConfig, result, dependencyInfo);

        return result;
    }
}
```

#### 7. **RuleEngineService Integration**

```java
@Service
public class RuleEngineService {

    @Autowired
    private ConfigurationResolutionEngine configResolutionEngine;

    @Autowired
    private ExecutionTraceBuilder traceBuilder;

    public RuleGroupExecutionResult executeRuleGroup(YamlRuleGroup ruleGroup, Object data, String sourceFile) {
        List<ExecutionTraceNode> ruleExecutionNodes = new ArrayList<>();

        for (YamlRuleGroup.RuleReference ruleRef : ruleGroup.getRuleReferences()) {
            // Load base rule configuration
            YamlRule baseRule = ruleRepository.findById(ruleRef.getRuleId());

            // Resolve configuration overrides
            ConfigurationResolution configResolution = configResolutionEngine.resolveRuleConfiguration(
                baseRule, ruleRef, sourceFile);

            // Execute rule with dataset-specific evaluation
            List<RuleExecutionTrace> datasetEvaluations = executeRuleWithDatasetTracking(baseRule, data);

            // Build rule execution trace with YAML alignment
            ExecutionTraceNode ruleTraceNode = traceBuilder.buildRuleExecutionNode(
                baseRule, sourceFile, datasetEvaluations, configResolution);

            ruleExecutionNodes.add(ruleTraceNode);
        }

        return RuleGroupExecutionResult.builder()
            .executionTraceNodes(ruleExecutionNodes)
            .build();
    }

    private List<RuleExecutionTrace> executeRuleWithDatasetTracking(YamlRule rule, Object data) {
        List<RuleExecutionTrace> evaluations = new ArrayList<>();

        // Generate unique dataset ID
        String datasetId = generateDatasetId(data);
        String datasetIdName = extractDatasetName(data);

        // Capture input values
        Map<String, Object> inputValues = extractInputValues(rule.getCondition(), data);

        // Evaluate condition with sub-condition breakdown
        ConditionEvaluationResult conditionResult = conditionEvaluationEngine.evaluateWithBreakdown(
            rule.getCondition(), data);

        // Create validation result
        ValidationResult validationResult = createValidationResult(conditionResult, rule);

        // Build dataset evaluation trace
        RuleExecutionTrace evaluation = RuleExecutionTrace.builder()
            .datasetId(datasetId)
            .datasetIdName(datasetIdName)
            .inputValues(inputValues)
            .conditionEvaluation(conditionResult)
            .executionPath(conditionResult.getFinalConditionResult() ? "condition-passed" : "condition-failed")
            .validationResult(validationResult)
            .build();

        evaluations.add(evaluation);
        return evaluations;
    }
}
```

#### 8. **EnrichmentService Integration**

```java
@Service
public class EnrichmentService {

    @Autowired
    private ConfigurationResolutionEngine configResolutionEngine;

    @Autowired
    private ExecutionTraceBuilder traceBuilder;

    public EnrichmentGroupExecutionResult executeEnrichmentGroup(
            YamlEnrichmentGroup enrichmentGroup, Object data, String sourceFile) {

        List<ExecutionTraceNode> enrichmentExecutionNodes = new ArrayList<>();

        for (YamlEnrichmentGroup.EnrichmentReference enrichmentRef : enrichmentGroup.getEnrichmentReferences()) {
            // Load base enrichment configuration
            YamlEnrichment baseEnrichment = enrichmentRepository.findById(enrichmentRef.getEnrichmentId());

            // Resolve configuration overrides
            ConfigurationResolution configResolution = configResolutionEngine.resolveEnrichmentConfiguration(
                baseEnrichment, enrichmentRef, sourceFile);

            // Execute enrichment with dataset-specific evaluation
            List<EnrichmentExecutionTrace> datasetEvaluations = executeEnrichmentWithDatasetTracking(
                baseEnrichment, data);

            // Build enrichment execution trace with YAML alignment
            ExecutionTraceNode enrichmentTraceNode = traceBuilder.buildEnrichmentExecutionNode(
                baseEnrichment, sourceFile, datasetEvaluations, configResolution);

            enrichmentExecutionNodes.add(enrichmentTraceNode);
        }

        return EnrichmentGroupExecutionResult.builder()
            .executionTraceNodes(enrichmentExecutionNodes)
            .build();
    }

    private List<EnrichmentExecutionTrace> executeEnrichmentWithDatasetTracking(
            YamlEnrichment enrichment, Object data) {

        List<EnrichmentExecutionTrace> evaluations = new ArrayList<>();

        // Generate unique dataset ID
        String datasetId = generateDatasetId(data);
        String datasetIdName = extractDatasetName(data);

        // Capture input values for condition evaluation
        Map<String, Object> inputValues = extractInputValues(enrichment.getCondition(), data);

        // Evaluate enrichment condition
        ConditionEvaluationResult conditionResult = conditionEvaluationEngine.evaluateWithBreakdown(
            enrichment.getCondition(), data);

        // Execute enrichment logic if condition passes
        EnrichmentResult enrichmentResult = null;
        if (conditionResult.getFinalConditionResult()) {
            enrichmentResult = performEnrichment(enrichment, data);
        }

        // Build dataset evaluation trace
        EnrichmentExecutionTrace evaluation = EnrichmentExecutionTrace.builder()
            .datasetId(datasetId)
            .datasetIdName(datasetIdName)
            .inputValues(inputValues)
            .conditionEvaluation(conditionResult)
            .executionPath(conditionResult.getFinalConditionResult() ? "condition-passed" : "condition-failed")
            .enrichmentResult(enrichmentResult)
            .build();

        evaluations.add(evaluation);
        return evaluations;
    }
}
```

### Performance and Storage Considerations

#### 9. **Execution Trace Storage Strategy**

```java
@Service
public class ExecutionTraceStorageService {

    @Value("${apex.trace.storage.strategy:database}")
    private String storageStrategy;

    @Value("${apex.trace.retention.days:30}")
    private int retentionDays;

    @Value("${apex.trace.compression.enabled:true}")
    private boolean compressionEnabled;

    public void storeExecutionTrace(ExecutionTraceNode traceNode) {
        switch (storageStrategy) {
            case "database":
                storeInDatabase(traceNode);
                break;
            case "file":
                storeInFile(traceNode);
                break;
            case "elasticsearch":
                storeInElasticsearch(traceNode);
                break;
            default:
                throw new IllegalArgumentException("Unknown storage strategy: " + storageStrategy);
        }
    }

    private void storeInDatabase(ExecutionTraceNode traceNode) {
        // Separate storage for execution trace and YAML configuration
        ExecutionTraceEntity traceEntity = convertToEntity(traceNode);
        YamlConfigurationEntity configEntity = convertYamlConfigToEntity(traceNode.getYamlConfiguration());

        // Store with compression if enabled
        if (compressionEnabled) {
            traceEntity.setDecisionDataCompressed(compress(traceEntity.getDecisionData()));
            configEntity.setConfigDataCompressed(compress(configEntity.getConfigData()));
        }

        executionTraceRepository.save(traceEntity);
        yamlConfigurationRepository.save(configEntity);
    }

    @Scheduled(cron = "0 0 2 * * ?") // Daily at 2 AM
    public void cleanupOldTraces() {
        Instant cutoffDate = Instant.now().minus(retentionDays, ChronoUnit.DAYS);
        executionTraceRepository.deleteByCreatedTimeBefore(cutoffDate);
        yamlConfigurationRepository.deleteByCreatedTimeBefore(cutoffDate);
    }
}
```

#### 10. **Query and Retrieval API**

```java
@RestController
@RequestMapping("/api/execution-traces")
public class ExecutionTraceController {

    @Autowired
    private ExecutionTraceQueryService queryService;

    @GetMapping("/{executionId}")
    public ExecutionTraceNode getExecutionTrace(@PathVariable String executionId) {
        return queryService.getExecutionTrace(executionId);
    }

    @GetMapping("/scenario/{scenarioId}")
    public List<ExecutionTraceNode> getExecutionTracesByScenario(
            @PathVariable String scenarioId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endTime,
            @RequestParam(required = false) String executionStatus) {

        return queryService.getExecutionTracesByScenario(scenarioId, startTime, endTime, executionStatus);
    }

    @GetMapping("/dataset/{datasetId}")
    public List<ExecutionTraceNode> getExecutionTracesByDataset(@PathVariable String datasetId) {
        return queryService.getExecutionTracesByDataset(datasetId);
    }

    @GetMapping("/rule/{ruleId}/evaluations")
    public List<RuleExecutionTrace> getRuleEvaluations(
            @PathVariable String ruleId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endTime) {

        return queryService.getRuleEvaluations(ruleId, startTime, endTime);
    }

    @GetMapping("/yaml-config/{configId}")
    public YamlConfigurationData getYamlConfiguration(@PathVariable String configId) {
        return queryService.getYamlConfiguration(configId);
    }

    @GetMapping("/validation-failures")
    public List<ValidationFailureTrace> getValidationFailures(
            @RequestParam(required = false) String severity,
            @RequestParam(required = false) String errorCode,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endTime) {

        return queryService.getValidationFailures(severity, errorCode, startTime, endTime);
    }
}
```

This comprehensive implementation provides **complete YAML-to-execution alignment** with performance optimization, storage strategies, and comprehensive query capabilities.

---

## Comprehensive Design Summary

### Complete APEX Execution Trace API Design

This document provides a **comprehensive and detailed design** for APEX execution tracing that achieves:

#### **1. Core Capabilities**

**Dataset-Specific Rule Evaluation**
- Captures how each validation rule evaluates against different datasets with actual runtime values
- Provides complete sub-condition breakdown showing which parts of complex conditions pass/fail
- Enables precise debugging by showing input values, evaluated expressions, and results
- Supports multiple datasets flowing through the same rule with individual evaluation traces

**Decision Data Capture at All Levels**
- **Scenario Registry Level**: Routing decisions, classification rule evaluations, scenario selection
- **Scenario Level**: Individual scenario validation and execution strategy
- **Stage Level**: Dependency checks, failure policy applications, conditional execution
- **Rule Group Level**: Group execution modes, operators (AND/OR), short-circuit logic
- **Rule Level**: Individual condition evaluations, execution paths, severity assignments
- **Enrichment Group Level**: Group conditions, execution orchestration, enrichment sequencing
- **Enrichment Level**: Individual enrichment conditions, conditional mappings, data transformations
- **Enrichment Step Level**: Lookup decisions, data source selection, field mappings

**Complete YAML Configuration Alignment**
- Every execution trace node includes complete YAML configuration metadata
- File dependency tracking with load timestamps and resolution status
- Configuration override resolution showing how rule-references and enrichment-references modify base configurations
- Perfect traceability from static YAML configuration to runtime execution behavior

#### **2. Technical Architecture**

**Hierarchical TreeNode Structure**
- Compatible with existing YAML dependency tree API format
- D3.js visualization ready with nested children arrays
- Overlay capability for side-by-side comparison with static dependency trees
- Consistent node structure across all execution levels

**Performance Optimized Implementation**
- Configurable trace capture levels (basic vs detailed)
- Conditional trace generation based on system settings
- Compressed storage for large decision data sets
- Efficient database schema with separate tables for execution traces and YAML configurations

**Comprehensive Data Models**
- Complete Java class structure for all configuration and execution data
- Builder patterns for easy object construction
- Proper separation of concerns between configuration capture and execution tracing
- Extensible design for future enhancements

#### **3. Implementation Strategy**

**Core Services**
- `YamlConfigurationCaptureService`: Captures complete YAML configuration data
- `ExecutionTraceBuilder`: Builds execution trace nodes with YAML alignment
- `ConfigurationResolutionEngine`: Resolves configuration overrides and references
- `FileDependencyTracker`: Tracks file dependencies and load status
- `ExecutionTraceStorageService`: Handles storage, compression, and retention

**Integration Points**
- `ScenarioStageExecutor`: Main orchestrator for scenario and stage execution tracing
- `RuleEngineService`: Rule evaluation with dataset-specific trace capture
- `EnrichmentService`: Enrichment execution with condition evaluation tracing
- `DataTypeScenarioService`: Scenario registry routing decision capture

**Storage and Retrieval**
- Multiple storage strategies (database, file, Elasticsearch)
- Comprehensive query API for execution traces, YAML configurations, and validation failures
- Automated cleanup and retention management
- Performance optimized indexing and compression

#### **4. Key Benefits**

**Complete Traceability**
- Every runtime decision can be traced back to specific YAML configuration
- Full audit trail from input values to validation results for compliance
- Perfect alignment between static configuration and dynamic execution

**Precise Debugging**
- Sub-condition breakdown shows exactly which part of complex conditions failed
- Dataset-specific evaluation shows how the same rule behaves with different data
- Configuration override visibility shows how rule-references modify base configurations

**Scientific Analysis**
- Detailed evaluation metrics enable quantitative analysis of business logic performance
- Complete decision data supports machine learning and optimization algorithms
- Historical trend analysis for performance monitoring and capacity planning

**Operational Excellence**
- Real-time execution monitoring with detailed decision visibility
- Comprehensive error analysis with root cause identification
- Configuration validation ensuring runtime behavior matches intended YAML configuration

#### **5. Use Cases Enabled**

**Development and Testing**
- Debug why specific rules fire or don't fire with actual data
- Validate that YAML configurations produce expected runtime behavior
- Test rule changes with historical data to predict impact

**Operations and Monitoring**
- Monitor business logic execution in real-time
- Identify performance bottlenecks in rule evaluation
- Track validation failure patterns and root causes

**Compliance and Auditing**
- Provide complete audit trails for regulatory compliance
- Demonstrate that business rules are applied consistently
- Track configuration changes and their impact on execution behavior

**Business Analysis**
- Analyze rule effectiveness and coverage
- Identify opportunities for rule optimization
- Understand data quality impact on business logic execution

### Conclusion

This comprehensive design provides **unprecedented visibility** into APEX business logic execution, enabling scientific-level analysis of rule evaluation, complete YAML configuration alignment, and dataset-specific tracing that supports debugging, compliance, optimization, and operational excellence.

The design is **production-ready** with performance optimization, scalable storage strategies, and comprehensive query capabilities that support both real-time monitoring and historical analysis of APEX business logic execution.

### Enrichment Group Decision Data
```json
{
  "enrichmentGroupExecution": {
    "executionMode": "sequential",
    "parallelExecution": false,
    "stopOnFirstFailure": false,
    "groupCondition": "#riskLevel == 'HIGH'",
    "groupConditionResult": true,
    "executionDecision": "execute-group",
    "enrichmentsInGroup": [
      {
        "enrichmentId": "market-data-lookup",
        "executionOrder": 1,
        "skipped": false,
        "skipReason": null
      },
      {
        "enrichmentId": "risk-calculation",
        "executionOrder": 2,
        "skipped": false,
        "skipReason": null
      }
    ],
    "groupResult": "SUCCESS",
    "terminationReason": "all-enrichments-completed"
  }
}
```

### Enrichment Decision Data
```json
{
  "enrichmentExecution": {
    "executionMode": "sequential",
    "stopOnFirstFailure": false,
    "stopOnFirstMatch": true,
    "conditionsEvaluated": [
      {
        "enrichmentId": "market-data-lookup",
        "condition": "#underlying != null",
        "conditionResult": true,
        "executionDecision": "execute",
        "priority": 1
      }
    ],
    "conditionalMappings": [
      {
        "mappingId": "high-value-mapping",
        "conditions": {
          "operator": "AND",
          "rules": [
            {"condition": "#amount > 1000000", "result": true}
          ]
        },
        "conditionResult": true,
        "mappingApplied": true,
        "stopOnFirstMatch": true
      }
    ]
  }
}
```

### Enrichment Step Decision Data
```json
{
  "enrichmentStep": {
    "conditionEvaluation": {
      "condition": "#underlying != null",
      "conditionResult": true,
      "inputValues": {"underlying": "AAPL"}
    },
    "lookupDecision": {
      "dataSourceSelected": "market-data-service",
      "lookupKey": "AAPL",
      "cacheHit": false,
      "fallbackUsed": false,
      "alternativeDataSources": ["backup-market-data"]
    },
    "transformationApplied": {
      "transformationType": "direct",
      "transformation": "#result.spotPrice",
      "transformationResult": 175.50
    }
  }
}
```

---

## Scenario Registry Routing Process

### Overview

The scenario registry routing process is the first and most critical decision point in APEX execution. It determines which scenario will process the incoming data, effectively setting the entire execution path. The decision data captures this multi-step routing process in detail.

### Routing Decision Flow

The `DataTypeScenarioService` follows this decision sequence:

1. **Data Type Determination**
   - Extract data type using `determineDataType(data)`
   - Typically returns class simple name (e.g., "HashMap", "TradeData")

2. **Direct Data Type Mapping** (`dataTypeToScenarios.get(dataType)`)
   - Check if any scenarios are registered for the specific data type
   - Uses scenario `data-types` configuration from YAML
   - Returns first matching scenario if found

3. **Routing Rules Evaluation** (`routingConfig.routeData(data, dataType)`)
   - Evaluate routing rules from registry configuration
   - Uses `routing.rules` section if configured
   - Supports complex routing logic beyond simple classification

4. **Classification Rule Evaluation** (`getScenarioForMapData(data)`)
   - Evaluate embedded classification rules in each scenario
   - Uses `scenario.classification-rule.condition` from scenario YAML
   - Evaluates scenarios in registration order
   - Short-circuits on first match

5. **Default Scenario Fallback** (`routingConfig.getDefaultScenario()`)
   - Use configured default scenario if no other routing succeeds
   - Uses `routing.default-scenario` from registry YAML

6. **No Scenario Found**
   - Return null if all routing methods fail
   - Results in processing failure with appropriate error

### Registry Configuration Types

#### Classification-Based Routing
```yaml
routing:
  strategy: "classification-based"
  default-scenario: "generic-trade-processing"
```

**Behavior**: Evaluates classification rules embedded in individual scenarios.

#### Type-Based Routing
```yaml
routing:
  strategy: "type-based"
  default-scenario: "generic-processing"
```

**Behavior**: Uses direct data type mapping from scenario `data-types` configuration.

#### Rule-Based Routing
```yaml
routing:
  strategy: "rule-based"
  rules:
    - condition: "#'source'] == 'MUREX'"
      target-scenario: "murex-processing"
    - condition: "#'source'] == 'CALYPSO'"
      target-scenario: "calypso-processing"
  default-scenario: "generic-processing"
```

**Behavior**: Evaluates routing rules before classification rules.

### Decision Data Capture Points

The scenario registry decision data captures:

- **Registry Configuration**: Total scenarios, routing strategy, default scenario
- **Data Type Analysis**: Determined data type and mapping attempts
- **Routing Method Attempts**: Each routing method tried and its result
- **Classification Rule Evaluations**: All scenarios evaluated with timing and results
- **Final Decision**: Selected scenario, method used, confidence, and total time

### Performance Considerations

- **Short-Circuit Evaluation**: Classification rule evaluation stops on first match
- **Caching**: Successful classifications can be cached based on data patterns
- **Timing**: Each evaluation step is timed for performance analysis
- **Confidence Scoring**: Decision confidence helps identify ambiguous routing cases

---

## REST API Endpoints

### 1. Generate Execution Trace Tree

**Endpoint:** `GET /api/execution/trace?executionId={id}`

**Description:** Retrieve hierarchical execution trace for a completed scenario execution

**Response:**
```json
{
  "status": "success",
  "executionId": "exec-uuid-12345",
  "scenarioId": "scenario-otc-options-validation",
  "datasetId": "trade-OTC-20251027-12345",
  "datasetIdName": "trading-system-xx-trade-id",
  "totalNodes": 42,
  "maxDepth": 5,
  "totalDurationMs": 333,
  "tree": { ExecutionTreeNode object },
  "timestamp": 1730025330123
}
```

### 2. Query Execution History

**Endpoint:** `GET /api/execution/history?scenarioId={id}&datasetId={id}&datasetIdName={name}&startTime={iso8601}&endTime={iso8601}&status={status}&includeDecisions={boolean}`

**Description:** Query execution history with filtering by scenario, dataset, dataset ID name, time range, status, and decision data

**Query Parameters:**
- `scenarioId` (optional): Filter by scenario ID
- `datasetId` (optional): Filter by dataset ID
- `datasetIdName` (optional): Filter by dataset ID name (external system identifier)
- `startTime` (optional): Filter executions after this timestamp
- `endTime` (optional): Filter executions before this timestamp
- `status` (optional): Filter by execution status (SUCCESS, FAILURE, etc.)
- `includeDecisions` (optional): Include decision data in response (default: false)

**Response:**
```json
{
  "status": "success",
  "totalExecutions": 150,
  "executions": [
    {
      "executionId": "exec-uuid-12345",
      "scenarioId": "scenario-otc-options-validation",
      "datasetId": "trade-OTC-20251027-12345",
      "datasetIdName": "trading-system-xx-trade-id",
      "status": "SUCCESS",
      "startTime": "2025-10-27T10:15:30.123Z",
      "durationMs": 333,
      "summary": "OTC Option trade processed through 3 stages with full enrichment"
    }
  ]
}
```

### 3. Get Execution Node Details

**Endpoint:** `GET /api/execution/{executionId}/node/{nodeId}`

**Description:** Retrieve detailed information for a specific execution node

**Response:**
```json
{
  "status": "success",
  "data": { ExecutionTreeNode with full metadata, input/output data }
}
```

### 4. Compare Execution with Dependency Tree

**Endpoint:** `GET /api/execution/{executionId}/overlay?rootFile={path}&includeDecisions={boolean}`

**Description:** Generate overlay data mapping execution trace to dependency tree with optional decision data

**Response:**
```json
{
  "status": "success",
  "executionTree": { ExecutionTreeNode },
  "dependencyTree": { TreeNode },
  "mapping": [
    {
      "dependencyPath": "scenarios/otc-options.yaml",
      "executionNodeId": "exec-uuid-12345",
      "status": "SUCCESS",
      "durationMs": 333,
      "decisionSummary": {
        "scenarioSelected": "otc-option-us",
        "stagesExecuted": 3,
        "rulesEvaluated": 12,
        "enrichmentsApplied": 5
      }
    }
  ]
}
```

### 5. Query Decision Data

**Endpoint:** `GET /api/execution/decisions?scenarioId={id}&decisionType={type}&conditionPattern={pattern}&startTime={iso8601}&endTime={iso8601}`

**Description:** Query specific decision data across executions for analysis and debugging

**Query Parameters:**
- `scenarioId` (optional): Filter by scenario ID
- `decisionType` (optional): Filter by decision type (scenarioRegistryRouting, scenarioSelection, stageExecution, ruleGroupExecution, ruleExecution, enrichmentGroupExecution, enrichmentExecution)
- `conditionPattern` (optional): Filter by condition pattern (e.g., "#amount > 1000000")
- `startTime` (optional): Filter decisions after this timestamp
- `endTime` (optional): Filter decisions before this timestamp

**Response:**
```json
{
  "status": "success",
  "totalDecisions": 150,
  "decisions": [
    {
      "executionId": "exec-uuid-12345",
      "nodeId": "exec-rule-001-1",
      "decisionType": "ruleExecution",
      "timestamp": "2025-10-27T10:15:30.145Z",
      "condition": "#amount > 1000000",
      "conditionResult": true,
      "inputValues": {"amount": 1500000},
      "executionPath": "condition-passed",
      "context": {
        "scenarioId": "trade-validation",
        "stageName": "amount-validation",
        "ruleName": "high-value-check"
      }
    }
  ]
}
```

---

## Execution Summary Formats

### Scenario Execution Summary
```json
{
  "scenarioId": "scenario-otc-options-validation",
  "datasetId": "trade-OTC-20251027-12345",
  "datasetIdName": "trading-system-xx-trade-id",
  "executionStatus": "SUCCESSFUL",
  "totalDurationMs": 333
}
```

### Stage Execution Summary
```json
{
  "stageName": "discovery-stage",
  "stageType": "DISCOVERY",
  "ruleGroupsExecuted": 2,
  "enrichmentGroupsExecuted": 1,
  "successful": true
}
```

### Rule Group Execution Summary
```json
{
  "rulesEvaluated": 4,
  "rulesPassed": 4,
  "rulesFailed": 0,
  "triggered": true,
  "severity": "INFO",
  "operator": "AND"
}
```

### Enrichment Execution Summary
```json
{
  "enrichmentsProcessed": 3,
  "enrichmentsSucceeded": 3,
  "enrichmentsFailed": 0,
  "dataFieldsEnriched": ["spotPrice", "volatility", "riskFreeRate"]
}
```

---

## Visualization Overlay Strategy

### Side-by-Side Comparison
- **Left Panel**: Static dependency tree (YAML structure)
- **Right Panel**: Execution trace tree (runtime execution)
- **Synchronized Navigation**: Clicking a node highlights corresponding node in both trees
- **Color Coding**: 
  - Green: Successful execution
  - Red: Failed execution
  - Yellow: Warnings
  - Gray: Skipped/Not executed

### Performance Heatmap Overlay
- Overlay execution duration on dependency tree nodes
- Color intensity based on execution time
- Identify performance bottlenecks visually

### Data Flow Visualization
- Show data transformations at each node
- Input → Processing → Output flow
- Field-level enrichment tracking

---

## Implementation Notes

### Core Infrastructure
1. **Reuse TreeNode Class**: Extend existing `TreeNode` with execution-specific fields and decision data
2. **Leverage Existing Infrastructure**: Use `ScenarioExecutionResult`, `StageExecutionResult`, `RuleResult`
3. **Storage**: Store execution traces in time-series database or event store with decision data indexing
4. **Performance**: Capture traces asynchronously to avoid impacting processing
5. **Retention**: Configurable retention policy (e.g., 30 days for traces, 90 days for decision data)
6. **Privacy**: Support data masking for sensitive fields in snapshots and decision data

### Decision Data Capture Strategy
7. **Conditional Capture**: Decision data capture can be enabled/disabled per environment (dev/test/prod)
8. **Selective Capture**: Configure which decision types to capture (scenario-registry, scenario, stage, rule-group, rule, enrichment-group, enrichment)
9. **Performance Impact**: Decision data adds ~15-20% overhead - use sampling in high-volume environments
10. **Storage Optimization**: Store decision data separately from execution traces for efficient querying
11. **Indexing Strategy**: Index decision data by condition patterns, execution paths, and timestamps
12. **Data Compression**: Compress decision data for long-term storage while maintaining query performance

### Integration Points
13. **DataTypeScenarioService**: Capture scenario registry routing decisions and classification rule evaluations
14. **ScenarioStageExecutor**: Capture stage dependency decisions and failure policy applications
15. **RuleEngineService**: Capture rule condition evaluations and short-circuit decisions
16. **EnrichmentService**: Capture enrichment group execution decisions and individual enrichment condition evaluations
17. **ConditionalChainingExecutor**: Capture trigger rule decisions and path selections
18. **ComplexWorkflowExecutor**: Capture conditional execution branch decisions
19. **YamlEnrichmentProcessor**: Capture stop-on-first-match and priority-based decisions

### Decision Data Storage Strategy

#### Database Schema Design
```sql
-- Main execution trace table
CREATE TABLE execution_traces (
    execution_id VARCHAR(36) PRIMARY KEY,
    scenario_id VARCHAR(100),
    dataset_id VARCHAR(100),
    dataset_id_name VARCHAR(100),
    start_time TIMESTAMP,
    end_time TIMESTAMP,
    status VARCHAR(20),
    decision_data_enabled BOOLEAN DEFAULT false
);

-- Decision data table (separate for performance)
CREATE TABLE execution_decisions (
    decision_id VARCHAR(36) PRIMARY KEY,
    execution_id VARCHAR(36) REFERENCES execution_traces(execution_id),
    node_id VARCHAR(100),
    decision_type VARCHAR(50), -- scenarioRegistryRouting, scenarioSelection, etc.
    decision_level INTEGER, -- 0=registry, 1=scenario, 2=stage, etc.
    timestamp TIMESTAMP,
    condition_pattern VARCHAR(500),
    condition_result BOOLEAN,
    execution_path VARCHAR(100),
    decision_data JSONB, -- Full decision data object
    evaluation_time_ms INTEGER
);

-- Indexes for efficient querying
CREATE INDEX idx_decisions_execution_id ON execution_decisions(execution_id);
CREATE INDEX idx_decisions_type_timestamp ON execution_decisions(decision_type, timestamp);
CREATE INDEX idx_decisions_condition_pattern ON execution_decisions(condition_pattern);
CREATE INDEX idx_decisions_execution_path ON execution_decisions(execution_path);
CREATE INDEX idx_decisions_result_timestamp ON execution_decisions(condition_result, timestamp);
```

#### Storage Optimization
- **Separate Tables**: Keep decision data separate from execution traces for query performance
- **JSONB Storage**: Use JSONB for flexible decision data structure with indexing support
- **Partitioning**: Partition by timestamp for efficient time-range queries
- **Compression**: Compress older decision data while maintaining query performance
- **Retention Policies**: Different retention for traces (30 days) vs decisions (90 days)

#### Indexing Strategy
- **Primary Indexes**: execution_id, decision_type, timestamp
- **Composite Indexes**: (decision_type, timestamp), (condition_result, timestamp)
- **Pattern Indexes**: condition_pattern for finding similar conditions
- **JSONB Indexes**: GIN indexes on decision_data for complex queries

---

## Format Alignment Example

### Dependency Tree Node (Static YAML Structure)
```json
{
  "name": "otc-options-validation.yaml",
  "id": "scenarios/otc-options-validation.yaml",
  "path": "scenarios/otc-options-validation.yaml",
  "type": "scenario",
  "depth": 0,
  "height": 3,
  "childCount": 2,
  "children": [
    {
      "name": "market-data.yaml",
      "id": "enrichments/market-data.yaml",
      "path": "enrichments/market-data.yaml",
      "type": "enrichment-config",
      "depth": 1,
      "height": 2,
      "childCount": 0,
      "contentSummary": {
        "fileType": "enrichments",
        "enrichmentCount": 3
      }
    },
    {
      "name": "instrument-validation.yaml",
      "id": "rules/instrument-validation.yaml",
      "path": "rules/instrument-validation.yaml",
      "type": "rule-config",
      "depth": 1,
      "height": 2,
      "childCount": 0,
      "contentSummary": {
        "fileType": "rules",
        "ruleCount": 4,
        "ruleGroupCount": 1
      }
    }
  ],
  "contentSummary": {
    "fileType": "scenario",
    "stageCount": 3
  },
  "healthScore": 95
}
```

### Execution Trace Node (Runtime Execution)
```json
{
  "name": "otc-options-validation.yaml",
  "id": "exec-2025-10-27-12345",
  "path": "scenarios/otc-options-validation.yaml",
  "type": "scenario-execution",
  "depth": 0,
  "height": 3,
  "childCount": 2,
  "executionOrder": 1,
  "status": "SUCCESS",
  "startTime": "2025-10-27T10:15:30.123Z",
  "endTime": "2025-10-27T10:15:30.456Z",
  "durationMs": 333,
  "datasetId": "trade-OTC-20251027-12345",
  "datasetIdName": "trading-system-xx-trade-id",
  "children": [
    {
      "name": "market-data.yaml",
      "id": "exec-2025-10-27-12345-enrich-001",
      "path": "enrichments/market-data.yaml",
      "type": "enrichment-execution",
      "depth": 1,
      "height": 2,
      "childCount": 3,
      "executionOrder": 1,
      "status": "SUCCESS",
      "startTime": "2025-10-27T10:15:30.130Z",
      "endTime": "2025-10-27T10:15:30.160Z",
      "durationMs": 30,
      "executionSummary": {
        "enrichmentsProcessed": 3,
        "enrichmentsSucceeded": 3,
        "dataFieldsEnriched": ["spotPrice", "volatility", "riskFreeRate"]
      }
    },
    {
      "name": "instrument-validation.yaml",
      "id": "exec-2025-10-27-12345-rule-001",
      "path": "rules/instrument-validation.yaml",
      "type": "rule-group-execution",
      "depth": 1,
      "height": 2,
      "childCount": 4,
      "executionOrder": 2,
      "status": "SUCCESS",
      "startTime": "2025-10-27T10:15:30.165Z",
      "endTime": "2025-10-27T10:15:30.195Z",
      "durationMs": 30,
      "executionSummary": {
        "rulesEvaluated": 4,
        "rulesPassed": 4,
        "triggered": true
      }
    }
  ],
  "executionSummary": {
    "scenarioId": "scenario-otc-options-validation",
    "executionStatus": "SUCCESSFUL",
    "totalDurationMs": 333
  },
  "healthScore": 100
}
```

### Key Alignment Points

| Property | Dependency Tree | Execution Trace | Alignment |
|----------|----------------|-----------------|-----------|
| `name` | File name | Same file name | Identical |
| `path` | File path | Same file path | Identical - enables mapping |
| `type` | `scenario`, `rule-config` | `scenario-execution`, `rule-group-execution` | Suffix pattern |
| `depth` | Tree depth | Same depth | Identical structure |
| `height` | Distance to leaf | Same calculation | Identical structure |
| `children` | Nested dependencies | Nested execution steps | Same hierarchy |
| `healthScore` | Static analysis | Runtime health | Same scale (0-100) |
| `contentSummary` | Static counts | `executionSummary` with runtime data | Parallel structure |

**Overlay Mapping Strategy:**
- Match nodes by `path` property
- Compare `contentSummary.ruleCount` with `executionSummary.rulesEvaluated`
- Visualize execution `durationMs` as heatmap on dependency tree
- Show `status` as color overlay (green/red/yellow)

---

## Dataset ID Usage

### Purpose
The `datasetId` and `datasetIdName` properties provide unique identification and context for the dataset being processed during execution.

**`datasetId`**: The actual unique identifier value (e.g., `"trade-OTC-20251027-12345"`)

**`datasetIdName`**: The external system name or semantic label for the ID type (e.g., `"trading-system-xx-trade-id"`, `"murex-trade-ref"`, `"calypso-deal-id"`)

This dual-field approach enables:

1. **Traceability**: Link execution trace back to the specific trade, batch, or record being processed
2. **Debugging**: Query all executions for a specific dataset to diagnose issues
3. **Audit Trail**: Track processing history for regulatory compliance with clear source system identification
4. **Correlation**: Connect execution traces across multiple scenarios processing the same dataset
5. **Monitoring**: Track dataset processing through the entire pipeline
6. **Multi-System Support**: Distinguish between different external system ID types (e.g., Murex vs Calypso trade IDs)
7. **Human Readability**: Display meaningful labels in UIs instead of just raw IDs

### Dataset ID Examples

| Use Case | Dataset ID | Dataset ID Name | Description | Query Use Case |
|----------|-----------|----------------|-------------|----------------|
| Trade Processing (Murex) | `trade-OTC-20251027-12345` | `murex-trade-ref` | Trade from Murex system | Find all Murex trade executions |
| Trade Processing (Calypso) | `CAL-FX-789456` | `calypso-deal-id` | Deal from Calypso system | Find all Calypso deal executions |
| Trade Processing (Generic) | `trade-12345` | `trading-system-xx-trade-id` | Generic trading system | Find executions from specific trading system |
| Batch Processing | `batch-2025-10-27-001` | `batch-processing-id` | Daily batch identifier | Track batch processing progress |
| Message Processing | `msg-FIX-4.4-789456` | `fix-message-id` | FIX protocol message | Trace FIX message processing |
| File Processing | `file-trades-20251027-103045` | `file-upload-id` | Uploaded file identifier | Track file processing lifecycle |
| Record Processing | `record-12345678` | `database-record-id` | Database record key | Find executions for specific record |
| Bloomberg Trade | `BBG-TRADE-456789` | `bloomberg-trade-id` | Bloomberg trade reference | Find all Bloomberg trade executions |
| Summit Trade | `SUMMIT-12345` | `summit-trade-id` | Summit system trade | Find all Summit trade executions |
| Kondor+ Deal | `KP-DEAL-789` | `kondor-deal-id` | Kondor+ deal reference | Find all Kondor+ deal executions |

### Real-World Example

**Scenario**: A middle office processes trades from multiple trading systems

```json
{
  "executionId": "exec-001",
  "scenarioId": "trade-validation",
  "datasetId": "OTC-20251027-12345",
  "datasetIdName": "murex-trade-ref",
  "status": "SUCCESS"
}
```

```json
{
  "executionId": "exec-002",
  "scenarioId": "trade-validation",
  "datasetId": "CAL-FX-789456",
  "datasetIdName": "calypso-deal-id",
  "status": "FAILURE"
}
```

**Query Examples:**
- "Show me all failed trades" → Filter by `status=FAILURE`
- "Show me all Murex trades" → Filter by `datasetIdName=murex-trade-ref`
- "Show me failed Calypso trades" → Filter by `datasetIdName=calypso-deal-id&status=FAILURE`
- "Show me this specific trade" → Filter by `datasetId=OTC-20251027-12345`
- "Show me this specific Calypso deal" → Filter by `datasetId=CAL-FX-789456&datasetIdName=calypso-deal-id`

### Query Patterns

**Find all executions for a specific trade:**
```
GET /api/execution/history?datasetId=trade-OTC-20251027-12345
```

**Find all failed executions for a batch:**
```
GET /api/execution/history?datasetId=batch-2025-10-27-001&status=FAILURE
```

**Trace a dataset through multiple scenarios:**
```
GET /api/execution/history?datasetId=trade-OTC-20251027-12345
# Returns executions across validation, enrichment, routing scenarios
```

**Find all executions from Murex system:**
```
GET /api/execution/history?datasetIdName=murex-trade-ref
# Returns all executions processing Murex trades
```

**Find all failed Calypso trades:**
```
GET /api/execution/history?datasetIdName=calypso-deal-id&status=FAILURE
# Returns all failed executions for Calypso deals
```

**Find specific trade from specific system:**
```
GET /api/execution/history?datasetId=CAL-FX-789456&datasetIdName=calypso-deal-id
# Returns executions for Calypso deal CAL-FX-789456
```

### Benefits of Dataset ID Name

The `datasetIdName` field provides critical context that enables:

1. **Multi-System Environments**: Distinguish between trades from different systems (Murex, Calypso, Bloomberg, etc.)
2. **System-Level Reporting**: Generate reports showing execution metrics per external system
3. **Troubleshooting**: Quickly identify if issues are specific to one external system
4. **Audit Compliance**: Track which external system provided the data for regulatory reporting
5. **UI Display**: Show meaningful labels like "Murex Trade: OTC-12345" instead of just "OTC-12345"
6. **Data Lineage**: Trace data back to its source system for data governance
7. **System Migration**: Track executions during migration from one system to another
8. **Performance Analysis**: Compare processing performance across different external systems

**Example Scenario:**
- Same trade ID format (e.g., "12345") might exist in both Murex and Calypso
- Without `datasetIdName`, you can't distinguish which system the trade came from
- With `datasetIdName`, you can query: "Show me all failed Murex trades" vs "Show me all failed Calypso trades"

### Implementation Notes

- Both `datasetId` and `datasetIdName` are **optional** - not all executions process identifiable datasets
- `datasetIdName` provides semantic context for the ID type (e.g., which external system it came from)
- For batch processing, consider using batch ID rather than individual record IDs
- Dataset ID should be extracted from input data or provided by the caller
- Dataset ID name should be configured or derived from the data source (e.g., from message headers, metadata, or configuration)
- Store both fields at the root scenario execution level (propagates to all child nodes)
- Index both `datasetId` and `datasetIdName` in storage for efficient querying
- Support filtering by ID name to find all executions from a specific external system
- Consider using standardized naming conventions for `datasetIdName` (e.g., `{system}-{entity}-{type}` format)

---

## Benefits of Format Alignment with Decision Data

### Visualization and Analysis Benefits
1. **Reuse Visualization Code**: Same D3.js tree viewer works for both dependency and execution trees
2. **Easy Overlay**: Match nodes by `path` property for side-by-side comparison
3. **Consistent UX**: Users familiar with dependency tree can navigate execution trace
4. **Simplified Development**: Extend existing `TreeNode` class rather than create new structure
5. **Performance Analysis**: Overlay execution metrics on static structure to identify bottlenecks
6. **Debugging**: Trace which YAML files were executed and in what order
7. **Audit Trail**: Link runtime execution back to configuration files
8. **Dataset Traceability**: Query execution history by dataset ID for end-to-end tracking

### Decision Data Benefits
9. **Root Cause Analysis**: Understand why specific execution paths were taken
10. **Business Logic Validation**: Verify that conditions are evaluating as expected
11. **Configuration Debugging**: Identify misconfigured conditions or logic errors
12. **Compliance Auditing**: Track decision-making for regulatory compliance
13. **Performance Optimization**: Identify frequently evaluated conditions for caching
14. **A/B Testing**: Compare decision outcomes across different rule configurations
15. **Training and Documentation**: Show real examples of how business rules execute
16. **Predictive Analysis**: Analyze decision patterns to predict future execution paths

### Use Cases for Decision Data

#### Scenario Registry Debugging
- **"Why was this scenario selected?"** → Check scenario registry routing decisions and classification rule evaluations
- **"Why didn't my new scenario get selected?"** → Check classification rule evaluation order and results
- **"Which scenarios were considered for this data?"** → Review scenariosEvaluated array with evaluation results
- **"Why did routing fall back to the default scenario?"** → Check routing decision sequence and failure points
- **"How can I optimize scenario selection performance?"** → Analyze classification rule evaluation times and patterns

#### Scenario-Level Debugging
- **"Why was this specific scenario chosen over others?"** → Check individual scenario classification rule evaluation and alternatives considered
- **"Why did scenario validation fail?"** → Check scenario-level classification result and confidence scores

#### Stage-Level Debugging
- **"Why was this trade routed to manual review?"** → Check stage failure policy decisions
- **"Why was this stage skipped?"** → Check stage dependency evaluation and conditional execution results
- **"Why did this stage fail?"** → Check stage execution decision and failure policy application

#### Rule and Enrichment Debugging
- **"Why didn't this enrichment group execute?"** → Check enrichment group condition evaluations
- **"Why didn't this enrichment execute?"** → Check individual enrichment condition evaluations
- **"Why did this rule group short-circuit?"** → Check rule evaluation sequence and results
- **"Which conditional mapping was applied?"** → Check conditional mapping priority and condition results

#### Performance and Optimization
- **"What would happen if I change this condition?"** → Analyze historical decision patterns
- **"Which conditions are evaluated most frequently?"** → Query decision data by condition patterns
- **"Where are the performance bottlenecks in my rules?"** → Analyze decision evaluation times across all levels
- **"How can I reduce the number of scenario evaluations?"** → Review classification rule patterns and ordering

#### Compliance and Auditing
- **"Show me all high-value trades that were auto-approved"** → Query decision data for specific condition results
- **"Which scenarios processed trades from Murex vs Calypso?"** → Filter by scenario registry routing decisions
- **"What was the decision trail for this regulatory flagged trade?"** → Full decision data trace from registry to enrichment steps

---

## Next Steps

### Phase 1: Core Execution Trace Infrastructure
1. **Create ExecutionTreeNode Model**: Extend `TreeNode` with execution fields including `datasetId`, `datasetIdName`, and `decisionData`
2. **Implement ExecutionTraceService**: Capture execution steps during scenario processing
   - Extract dataset ID and ID name from input data or accept as parameters
   - Propagate both fields to all child execution nodes
   - Support configurable ID name extraction strategies (e.g., from metadata, headers, config)
3. **Add Basic REST API Endpoints**: Expose execution trace via REST API
   - Support filtering by `datasetId` and `datasetIdName` in history queries
   - Include both fields in all response objects

### Phase 2: Decision Data Capture
4. **Implement Decision Data Capture**: Add decision data collection to key execution points
   - **DataTypeScenarioService**: Capture scenario registry routing and classification rule evaluations
   - **ScenarioStageExecutor**: Capture dependency checks, failure policy decisions
   - **RuleEngineService**: Capture condition evaluations, short-circuit decisions
   - **EnrichmentService**: Capture enrichment group execution decisions and individual enrichment conditions
   - **ConditionalChainingExecutor**: Capture trigger decisions and path selections
5. **Decision Data Storage**: Design efficient storage and indexing for decision data
   - Separate decision data tables for query performance
   - Index by condition patterns, execution paths, timestamps
   - Support data compression and retention policies
6. **Enhanced REST API**: Add decision data endpoints
   - Decision-specific query endpoints
   - Decision data filtering and aggregation
   - Include decision data in execution trace responses

### Phase 3: Visualization and Analysis
7. **Extend D3 Viewer**: Add execution trace visualization mode with decision data
   - Display dataset ID and ID name in node tooltips (e.g., "Murex Trade: trade-12345")
   - Show decision data in expandable node details
   - Support filtering/highlighting by dataset ID, ID name, or decision criteria
   - Color-code nodes by external system and decision outcomes
8. **Implement Overlay View**: Side-by-side dependency tree + execution trace with decision overlay
9. **Decision Analysis Dashboard**: Build specialized views for decision analysis
   - Condition evaluation frequency and patterns
   - Decision path analysis and optimization suggestions
   - A/B testing support for rule configuration changes

### Phase 4: Advanced Features
10. **Add Query Capabilities**: Advanced filtering and search
    - Filter executions by time, status, scenario, dataset ID, dataset ID name, and decision criteria
    - Full-text search on decision data (conditions, execution paths)
    - Pattern matching for similar decision scenarios
11. **Performance Testing**: Ensure trace and decision capture doesn't impact processing performance
    - Benchmark with and without decision data capture
    - Implement sampling strategies for high-volume environments
    - Optimize storage and query performance
12. **Dataset ID Indexing**: Create database indexes on both `datasetId` and `datasetIdName` for efficient querying
13. **Multi-System Reporting**: Build dashboards showing execution metrics grouped by `datasetIdName` (per external system)
14. **Decision Data Analytics**: Advanced analytics and machine learning on decision patterns
    - Predict execution paths based on input data
    - Identify optimization opportunities in business logic
    - Anomaly detection in decision patterns

---

## Complete Decision Data Example

### Full Execution Trace with All Decision Levels

This example shows a complete execution trace with decision data captured at all levels of the APEX hierarchy:

```json
{
  "name": "scenario-otc-options-validation",
  "id": "exec-uuid-12345",
  "type": "scenario-execution",
  "status": "SUCCESS",
  "startTime": "2025-10-27T10:15:30.123Z",
  "endTime": "2025-10-27T10:15:30.456Z",
  "durationMs": 333,
  "datasetId": "trade-OTC-20251027-12345",
  "datasetIdName": "murex-trade-id",
  "decisionData": {
    "scenarioRegistryRouting": {
      "routingStrategy": "classification-based",
      "dataType": "HashMap",
      "routingDecisions": [
        {
          "routingMethod": "direct-data-type-mapping",
          "dataTypeMatched": false,
          "availableScenarios": []
        },
        {
          "routingMethod": "classification-rule-evaluation",
          "scenariosEvaluated": [
            {
              "scenarioId": "otc-option-us",
              "classificationRule": "#'tradeType'] == 'OTCOption' && #'region'] == 'US'",
              "classificationResult": true,
              "evaluationOrder": 1,
              "evaluationTimeMs": 2
            }
          ],
          "selectedScenario": "otc-option-us",
          "selectionReason": "First matching classification rule"
        }
      ],
      "finalDecision": {
        "selectedScenario": "otc-option-us",
        "selectionMethod": "classification-rule-evaluation",
        "totalEvaluationTimeMs": 2
      }
    },
    "scenarioSelection": {
      "scenarioId": "otc-option-us",
      "classificationResult": true,
      "executionStrategy": {
        "stageExecutionMode": "sequential",
        "totalStagesConfigured": 3
      }
    }
  },
  "children": [
    {
      "name": "validation-stage",
      "type": "stage-execution",
      "status": "SUCCESS",
      "durationMs": 45,
      "decisionData": {
        "stageExecution": {
          "stageName": "validation-stage",
          "executionDecision": "execute",
          "dependencyCheck": {
            "dependsOn": [],
            "allDependenciesMet": true
          },
          "failurePolicy": "terminate"
        }
      },
      "children": [
        {
          "name": "trade-validation-rules",
          "type": "rule-group-execution",
          "status": "SUCCESS",
          "durationMs": 30,
          "decisionData": {
            "ruleGroupExecution": {
              "executionMode": "sequential",
              "operator": "AND",
              "stopOnFirstFailure": false,
              "rulesEvaluated": [
                {
                  "ruleName": "validate-trade-amount",
                  "condition": "#amount > 0 && #amount <= 10000000",
                  "conditionResult": true,
                  "executionDecision": "continue"
                }
              ],
              "groupResult": true
            }
          },
          "children": [
            {
              "name": "validate-trade-amount",
              "type": "rule-execution",
              "status": "SUCCESS",
              "durationMs": 5,
              "decisionData": {
                "ruleExecution": {
                  "conditionEvaluation": {
                    "originalCondition": "#amount > 0 && #amount <= 10000000",
                    "evaluatedCondition": "1500000 > 0 && 1500000 <= 10000000",
                    "conditionResult": true,
                    "inputValues": {"amount": 1500000}
                  },
                  "executionPath": "condition-passed",
                  "severityAssignment": {
                    "configuredSeverity": "INFO",
                    "finalSeverity": "INFO"
                  }
                }
              }
            }
          ]
        }
      ]
    },
    {
      "name": "enrichment-stage",
      "type": "stage-execution",
      "status": "SUCCESS",
      "durationMs": 75,
      "decisionData": {
        "stageExecution": {
          "stageName": "enrichment-stage",
          "executionDecision": "execute",
          "dependencyCheck": {
            "dependsOn": ["validation-stage"],
            "allDependenciesMet": true,
            "dependencyResults": {"validation-stage": "SUCCESS"}
          }
        }
      },
      "children": [
        {
          "name": "market-data-enrichment-group",
          "type": "enrichment-group-execution",
          "status": "SUCCESS",
          "durationMs": 60,
          "decisionData": {
            "enrichmentGroupExecution": {
              "executionMode": "sequential",
              "groupCondition": "#underlying != null",
              "groupConditionResult": true,
              "executionDecision": "execute-group",
              "enrichmentsInGroup": [
                {
                  "enrichmentId": "spot-price-lookup",
                  "executionOrder": 1,
                  "skipped": false
                }
              ]
            }
          },
          "children": [
            {
              "name": "spot-price-lookup",
              "type": "enrichment-execution",
              "status": "SUCCESS",
              "durationMs": 25,
              "decisionData": {
                "enrichmentExecution": {
                  "conditionsEvaluated": [
                    {
                      "enrichmentId": "spot-price-lookup",
                      "condition": "#underlying != null",
                      "conditionResult": true,
                      "executionDecision": "execute"
                    }
                  ]
                }
              },
              "children": [
                {
                  "name": "market-data-lookup-step",
                  "type": "enrichment-step",
                  "status": "SUCCESS",
                  "durationMs": 15,
                  "decisionData": {
                    "enrichmentStep": {
                      "conditionEvaluation": {
                        "condition": "#underlying != null",
                        "conditionResult": true,
                        "inputValues": {"underlying": "AAPL"}
                      },
                      "lookupDecision": {
                        "dataSourceSelected": "market-data-service",
                        "lookupKey": "AAPL",
                        "cacheHit": false,
                        "fallbackUsed": false
                      }
                    }
                  }
                }
              ]
            }
          ]
        }
      ]
    }
  ]
}
```

### Decision Data Summary

This example demonstrates decision data capture at all 8 levels:

1. **Scenario Registry**: Classification rule evaluation across scenarios
2. **Scenario**: Individual scenario validation and execution strategy
3. **Stage**: Dependency checks and failure policy application
4. **Rule Group**: Group execution mode and rule orchestration
5. **Rule**: Individual condition evaluation and severity assignment
6. **Enrichment Group**: Group condition evaluation and enrichment orchestration
7. **Enrichment**: Individual enrichment condition evaluation
8. **Enrichment Step**: Lookup decisions and data source selection

Each level provides specific decision context that enables comprehensive debugging, auditing, and optimization of APEX business logic.

