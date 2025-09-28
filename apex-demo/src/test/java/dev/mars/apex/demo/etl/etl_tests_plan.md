# **Detailed Implementation Plan for APEX ETL Processing Tests**

## **Test Structure Overview**

```
PipelineEtlExecutionTest.java
├── @Nested PipelineStepExecutionTests
├── @Nested ExecutionModeTests  
├── @Nested ErrorHandlingTests
├── @Nested RetryMechanismTests
└── @Nested DataFlowTests
```

## **1. Pipeline Step Execution Tests**

### **1.1 Extract Step Testing**
**Objective**: Verify `PipelineExecutor.executeExtractStep()` correctly pulls data from DataSource

**Test Cases:**
- `shouldExecuteExtractStepFromCsvSource()`
- `shouldExecuteExtractStepFromDatabaseSource()`
- `shouldHandleEmptyDataSourceGracefully()`
- `shouldFailExtractStepWithInvalidSource()`

**Implementation Details:**
```java
// Setup: Create CSV file with test data
// Execute: Call pipelineEngine.executePipeline() with extract-only pipeline
// Verify: Check pipelineContext contains extracted data
// Verify: Validate PipelineStepResult shows success and record count
```

### **1.2 Load Step Testing**
**Objective**: Verify `PipelineExecutor.executeLoadStep()` writes data to DataSink

**Test Cases:**
- `shouldExecuteLoadStepToFileSystemSink()`
- `shouldExecuteLoadStepToDatabaseSink()`
- `shouldHandleBatchLoadOperations()`
- `shouldSkipInvalidRecordsGracefully()`

**Implementation Details:**
```java
// Setup: Pre-populate pipelineContext with test data
// Execute: Run pipeline with load-only step
// Verify: Check target sink contains expected data
// Verify: Validate load statistics (processed/failed counts)
```

### **1.3 Transform Step Testing**
**Objective**: Test data transformation between extract and load

**Test Cases:**
- `shouldApplyTransformationRules()`
- `shouldFilterRecordsBasedOnConditions()`
- `shouldAggregateDataDuringTransform()`
- `shouldHandleTransformationErrors()`

**Implementation Details:**
```java
// Setup: Pipeline with extract → transform → load steps
// Execute: Run full pipeline with transformation logic
// Verify: Compare input vs output data for correct transformations
```

### **1.4 Step Dependency Testing**
**Objective**: Verify `depends-on` relationships work correctly

**Test Cases:**
- `shouldExecuteStepsInDependencyOrder()`
- `shouldFailWhenDependencyStepFails()`
- `shouldSkipOptionalDependentSteps()`
- `shouldDetectCircularDependencies()`

**Implementation Details:**
```java
// Setup: Pipeline with complex dependency chain (A → B → C, A → D)
// Execute: Run pipeline and track step execution order
// Verify: Steps execute in correct topological order
// Verify: Dependency failures propagate correctly
```

## **2. Execution Mode Tests**

### **2.1 Sequential Mode Testing**
**Objective**: Test `PipelineExecutor.executeStepsSequentially()`

**Test Cases:**
- `shouldExecuteStepsInSequentialOrder()`
- `shouldRespectStepDependenciesInSequentialMode()`
- `shouldMeasureSequentialExecutionTiming()`

**Implementation Details:**
```java
// Setup: Pipeline with 4+ steps, some with dependencies
// Execute: Run with mode: "sequential"
// Verify: Steps execute one after another (timing validation)
// Verify: No parallel execution occurs
```

### **2.2 Parallel Mode Testing**
**Objective**: Test `PipelineExecutor.executeStepsInParallel()`

**Test Cases:**
- `shouldExecuteIndependentStepsInParallel()`
- `shouldRespectDependenciesInParallelMode()`
- `shouldImprovePerformanceWithParallelExecution()`

**Implementation Details:**
```java
// Setup: Pipeline with independent steps + dependent steps
// Execute: Run with mode: "parallel"
// Verify: Independent steps run concurrently
// Verify: Dependent steps wait for prerequisites
// Verify: Overall execution time < sequential time
```

### **2.3 Dependency Resolution Testing**
**Objective**: Test `PipelineExecutor.topologicalSort()`

**Test Cases:**
- `shouldSortStepsByDependencies()`
- `shouldHandleComplexDependencyGraphs()`
- `shouldDetectAndRejectCircularDependencies()`

**Implementation Details:**
```java
// Setup: Create complex dependency graph
// Execute: Call topologicalSort() directly
// Verify: Returned order satisfies all dependencies
// Verify: Circular dependencies throw exception
```

## **3. Error Handling Tests**

### **3.1 Stop-on-Error Testing**
**Objective**: Verify pipeline stops when error-handling: "stop-on-error"

**Test Cases:**
- `shouldStopPipelineOnFirstStepFailure()`
- `shouldNotExecuteSubsequentStepsAfterFailure()`
- `shouldPropagateErrorDetailsCorrectly()`

**Implementation Details:**
```java
// Setup: Pipeline with failing step in middle
// Execute: Run with error-handling: "stop-on-error"
// Verify: Pipeline execution stops at failing step
// Verify: Subsequent steps never execute
// Verify: YamlPipelineExecutionResult shows failure details
```

### **3.2 Continue-on-Error Testing**
**Objective**: Verify pipeline continues when error-handling: "continue-on-error"

**Test Cases:**
- `shouldContinuePipelineAfterStepFailure()`
- `shouldExecuteAllStepsDespiteFailures()`
- `shouldCollectAllErrorsInResult()`

**Implementation Details:**
```java
// Setup: Pipeline with multiple failing steps
// Execute: Run with error-handling: "continue-on-error"
// Verify: All steps attempt execution
// Verify: Pipeline completes with partial success
// Verify: All errors captured in result
```

### **3.3 Optional Steps Testing**
**Objective**: Test optional: true step behavior

**Test Cases:**
- `shouldContinueWhenOptionalStepFails()`
- `shouldNotAffectPipelineSuccessStatus()`
- `shouldStillExecuteDependentSteps()`

**Implementation Details:**
```java
// Setup: Pipeline with optional failing step
// Execute: Run pipeline
// Verify: Optional step failure doesn't stop pipeline
// Verify: Steps depending on optional step still execute
```

## **4. Retry Mechanism Tests**

### **4.1 Max-Retries Testing**
**Objective**: Test max-retries configuration behavior

**Test Cases:**
- `shouldRetryFailedStepsUpToMaxRetries()`
- `shouldStopRetryingAfterMaxAttemptsReached()`
- `shouldSucceedOnRetryAttempt()`
- `shouldRespectZeroMaxRetries()`

**Implementation Details:**
```java
// Setup: Pipeline with intermittently failing step
// Execute: Run with max-retries: 3
// Verify: Step retries exactly 3 times before final failure
// Verify: Successful retry stops further attempts
// Measure: Total execution time includes retry delays
```

### **4.2 Retry-Delay Testing**
**Objective**: Test retry-delay-ms timing behavior

**Test Cases:**
- `shouldWaitSpecifiedDelayBetweenRetries()`
- `shouldRespectDifferentDelayConfigurations()`
- `shouldHandleZeroRetryDelay()`

**Implementation Details:**
```java
// Setup: Failing step with retry-delay-ms: 1000
// Execute: Run pipeline and measure timing
// Verify: Delays of ~1000ms between retry attempts
// Verify: Total execution time = base time + (retries × delay)
```

### **4.3 Backoff Strategy Testing**
**Objective**: Test exponential backoff if implemented

**Test Cases:**
- `shouldApplyExponentialBackoffIfConfigured()`
- `shouldRespectMaxDelayLimits()`

**Implementation Details:**
```java
// Setup: Step with backoff-multiplier configuration
// Execute: Run with multiple retries
// Verify: Delays increase exponentially (1s, 2s, 4s, 8s...)
// Verify: Max delay cap is respected
```

## **5. Data Flow Tests**

### **5.1 Pipeline Context Testing**
**Objective**: Test data flow between steps via pipelineContext

**Test Cases:**
- `shouldPassDataBetweenStepsViaPipelineContext()`
- `shouldIsolateDataBetweenPipelineRuns()`
- `shouldHandleLargeDataSetsInContext()`

**Implementation Details:**
```java
// Setup: Extract step → Transform step → Load step pipeline
// Execute: Run pipeline with test data
// Verify: Data extracted in step 1 available in step 2
// Verify: Transformed data in step 2 available in step 3
// Verify: Context cleared between pipeline runs
```

### **5.2 Step Results Testing**
**Objective**: Verify PipelineStepResult captures execution outcomes

**Test Cases:**
- `shouldCaptureStepExecutionMetrics()`
- `shouldRecordStepSuccessAndFailureStatus()`
- `shouldTrackDataProcessingStatistics()`

**Implementation Details:**
```java
// Setup: Multi-step pipeline with various outcomes
// Execute: Run pipeline
// Verify: Each step has PipelineStepResult with:
//   - Execution time
//   - Success/failure status  
//   - Records processed count
//   - Error details if failed
```

### **5.3 Data Integrity Testing**
**Objective**: Test data consistency through extract → load pipeline

**Test Cases:**
- `shouldMaintainDataIntegrityThroughPipeline()`
- `shouldHandleDataTypeConversions()`
- `shouldValidateDataQualityRules()`
- `shouldDetectDataCorruption()`

**Implementation Details:**
```java
// Setup: CSV source → Database sink pipeline
// Execute: Run pipeline with known test dataset
// Verify: All source records appear in sink
// Verify: Data types converted correctly
// Verify: No data loss or corruption
// Verify: Referential integrity maintained
```

## **Test Infrastructure Requirements**

### **Test Data Setup**
- **CSV Files**: Customer data, transaction data, reference data
- **H2 Database**: Source and sink tables with realistic schemas
- **JSON Files**: Configuration data, lookup tables
- **File System**: Input/output directories for file-based processing

### **Test Utilities**
- **Pipeline Configuration Builder**: Programmatically create pipeline YAML
- **Data Verification Helpers**: Compare source vs sink data
- **Timing Utilities**: Measure execution times accurately
- **Error Injection**: Simulate failures at specific steps

### **Test Execution Strategy**
- **Isolated Tests**: Each test uses separate data and directories
- **Cleanup**: Automatic cleanup of test data after each test
- **Parallel Safe**: Tests can run concurrently without interference
- **Performance Baseline**: Establish timing baselines for performance tests

## **Key Focus**

This plan focuses exclusively on **ETL pipeline processing workflows** and avoids any enrichment, validation, or YAML parsing tests. All tests will validate actual data movement, step orchestration, and pipeline execution behavior using real DataSource and DataSink implementations.
