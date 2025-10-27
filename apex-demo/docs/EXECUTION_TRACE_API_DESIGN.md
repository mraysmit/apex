# APEX Execution Trace API Design

## Overview

This document defines the API design for APEX execution tracing that aligns with the existing YAML dependency tree API format. The execution trace provides runtime visibility into scenario processing, rule evaluation, enrichment execution, and data transformations - structured to enable overlay visualization with the static dependency tree.

## Design Principles

1. **Format Alignment**: Use the same hierarchical TreeNode structure as YAML dependency tree API
2. **D3.js Compatibility**: Nested `children` array format for direct visualization
3. **Overlay Capability**: Execution trace can be overlaid on dependency tree for side-by-side comparison
4. **Queryable**: Support filtering by scenario, time range, execution status
5. **Performance**: Capture detailed metrics without impacting processing performance

---

## API Response Format

### Execution Trace Tree Structure

The execution trace uses the **same TreeNode format** as the dependency tree, with execution-specific metadata:

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
              "dataSource": "market-data-service"
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
              "severity": "INFO"
            }
          ]
        }
      ]
    }
  ],
  "executionSummary": {
    "scenarioId": "scenario-otc-options-validation",
    "executionStatus": "SUCCESSFUL",
    "totalStages": 3,
    "stagesSucceeded": 3,
    "stagesFailed": 0,
    "stagesSkipped": 0,
    "totalRulesEvaluated": 12,
    "totalEnrichmentsProcessed": 5,
    "totalDurationMs": 333,
    "dataSnapshot": {
      "inputRecordCount": 1,
      "outputRecordCount": 1,
      "fieldsEnriched": 8,
      "validationsPassed": 12
    }
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

**Endpoint:** `GET /api/execution/history?scenarioId={id}&datasetId={id}&datasetIdName={name}&startTime={iso8601}&endTime={iso8601}&status={status}`

**Description:** Query execution history with filtering by scenario, dataset, dataset ID name, time range, and status

**Query Parameters:**
- `scenarioId` (optional): Filter by scenario ID
- `datasetId` (optional): Filter by dataset ID
- `datasetIdName` (optional): Filter by dataset ID name (external system identifier)
- `startTime` (optional): Filter executions after this timestamp
- `endTime` (optional): Filter executions before this timestamp
- `status` (optional): Filter by execution status (SUCCESS, FAILURE, etc.)

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
      "summary": "3 stages, 12 rules, 5 enrichments"
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

**Endpoint:** `GET /api/execution/{executionId}/overlay?rootFile={path}`

**Description:** Generate overlay data mapping execution trace to dependency tree

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
      "durationMs": 333
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
  "totalStages": 3,
  "stagesSucceeded": 3,
  "stagesFailed": 0,
  "stagesSkipped": 0,
  "totalRulesEvaluated": 12,
  "totalEnrichmentsProcessed": 5,
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

1. **Reuse TreeNode Class**: Extend existing `TreeNode` with execution-specific fields
2. **Leverage Existing Infrastructure**: Use `ScenarioExecutionResult`, `StageExecutionResult`, `RuleResult`
3. **Storage**: Store execution traces in time-series database or event store
4. **Performance**: Capture traces asynchronously to avoid impacting processing
5. **Retention**: Configurable retention policy (e.g., 30 days)
6. **Privacy**: Support data masking for sensitive fields in snapshots

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
    "totalStages": 3,
    "totalRulesEvaluated": 4,
    "totalEnrichmentsProcessed": 3
  },
  "healthScore": 100
}
```

### Key Alignment Points

| Property | Dependency Tree | Execution Trace | Alignment |
|----------|----------------|-----------------|-----------|
| `name` | File name | Same file name | ✅ Identical |
| `path` | File path | Same file path | ✅ Identical - enables mapping |
| `type` | `scenario`, `rule-config` | `scenario-execution`, `rule-group-execution` | ✅ Suffix pattern |
| `depth` | Tree depth | Same depth | ✅ Identical structure |
| `height` | Distance to leaf | Same calculation | ✅ Identical structure |
| `children` | Nested dependencies | Nested execution steps | ✅ Same hierarchy |
| `healthScore` | Static analysis | Runtime health | ✅ Same scale (0-100) |
| `contentSummary` | Static counts | `executionSummary` with runtime data | ✅ Parallel structure |

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

## Benefits of Format Alignment

1. **Reuse Visualization Code**: Same D3.js tree viewer works for both
2. **Easy Overlay**: Match nodes by `path` property for side-by-side comparison
3. **Consistent UX**: Users familiar with dependency tree can navigate execution trace
4. **Simplified Development**: Extend existing `TreeNode` class rather than create new structure
5. **Performance Analysis**: Overlay execution metrics on static structure to identify bottlenecks
6. **Debugging**: Trace which YAML files were executed and in what order
7. **Audit Trail**: Link runtime execution back to configuration files
8. **Dataset Traceability**: Query execution history by dataset ID for end-to-end tracking

---

## Next Steps

1. **Create ExecutionTreeNode Model**: Extend `TreeNode` with execution fields including `datasetId` and `datasetIdName`
2. **Implement ExecutionTraceService**: Capture execution steps during scenario processing
   - Extract dataset ID and ID name from input data or accept as parameters
   - Propagate both fields to all child execution nodes
   - Support configurable ID name extraction strategies (e.g., from metadata, headers, config)
3. **Add REST API Endpoints**: Expose execution trace via REST API
   - Support filtering by `datasetId` and `datasetIdName` in history queries
   - Include both fields in all response objects
4. **Extend D3 Viewer**: Add execution trace visualization mode
   - Display dataset ID and ID name in node tooltips (e.g., "Murex Trade: trade-12345")
   - Support filtering/highlighting by dataset ID or ID name
   - Color-code nodes by external system (e.g., Murex=blue, Calypso=green)
5. **Implement Overlay View**: Side-by-side dependency tree + execution trace
6. **Add Query Capabilities**: Filter executions by time, status, scenario, **dataset ID, and dataset ID name**
7. **Performance Testing**: Ensure trace capture doesn't impact processing performance
8. **Dataset ID Indexing**: Create database indexes on both `datasetId` and `datasetIdName` for efficient querying
9. **Multi-System Reporting**: Build dashboards showing execution metrics grouped by `datasetIdName` (per external system)

