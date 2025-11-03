# APEX To Dos

**Date:** 2025-10-17  

- allow linking from standard error codes as per op model

- **[DESIGNED]** when an apex process is executed, i.e. a set of yaml files is proceed with a data set, a clear set of execution steps and actual validation, enrichments, etc should be precisely logged to a processing log or something similar that can be queried and monitored and ideally visualised against the actual data steps. We can already create a dependency tree for any yaml file and its child dependencies - we should be able to do the same for a processing execution and its steps.
  - **Design Document**: See `EXECUTION_TRACE_API_DESIGN.md` for complete API design
  - **Format Alignment**: Execution trace uses same TreeNode format as dependency tree API for easy overlay
  - **Key Features**:
    - Hierarchical execution tree with nested children (D3.js compatible)
    - Same structure as dependency tree (name, id, path, depth, height, children)
    - Execution-specific extensions (status, startTime, endTime, durationMs, executionSummary)
    - Dataset tracking: `datasetId` (unique identifier) and `datasetIdName` (external system label like "murex-trade-ref")
    - REST API endpoints: `/api/execution/trace`, `/api/execution/history`, `/api/execution/{id}/overlay`
    - Query by dataset ID, dataset ID name, scenario, time range, status
    - Overlay visualization: side-by-side dependency tree + execution trace with synchronized navigation
    - Performance heatmap: overlay execution duration on dependency tree nodes
    - Data flow tracking: input/output snapshots at each execution step
    - Multi-system support: distinguish and query executions by external system (Murex, Calypso, Bloomberg, etc.)

- **performance harness** 
- 


- apex config browser with  dependency tree with sub-directory
