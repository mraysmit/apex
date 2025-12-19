# Parallel Execution Implementation Guide

## Current Status

**Location:** `apex-core/src/main/java/dev/mars/apex/core/engine/pipeline/PipelineExecutor.java:231-236`

**Current Implementation:**
```java
private void executeStepsInParallel(List<PipelineStep> steps, YamlPipelineExecutionResult result)
        throws DataPipelineException {
    // For now, implement as sequential - parallel execution would require more complex dependency management
    executeStepsSequentially(steps, result);
}
```

**Status:** Parallel mode currently falls back to sequential execution.

---

## What's Required to Implement True Parallel Execution

### 1. **Dependency Graph Construction**

The `PipelineStep` class already has the `depends-on` field (line 34-35):
```java
@JsonProperty("depends-on")
private List<String> dependsOn; // step dependencies
```

**Required Implementation:**
- Build a directed acyclic graph (DAG) from the pipeline steps
- Identify steps with no dependencies (can run immediately)
- Identify steps that can run in parallel (no shared dependencies)
- Detect circular dependencies and fail fast

**Algorithm:**
```java
private Map<String, Set<String>> buildDependencyGraph(List<PipelineStep> steps) {
    Map<String, Set<String>> graph = new HashMap<>();
    
    for (PipelineStep step : steps) {
        graph.put(step.getName(), new HashSet<>());
        if (step.getDependsOn() != null) {
            graph.get(step.getName()).addAll(step.getDependsOn());
        }
    }
    
    // Validate: check for circular dependencies
    detectCircularDependencies(graph);
    
    return graph;
}
```

---

### 2. **Topological Sorting**

Sort steps into execution levels where:
- **Level 0:** Steps with no dependencies
- **Level 1:** Steps that only depend on Level 0 steps
- **Level N:** Steps that depend on steps from levels 0 to N-1

**Algorithm:**
```java
private List<List<PipelineStep>> computeExecutionLevels(
        List<PipelineStep> steps, 
        Map<String, Set<String>> dependencyGraph) {
    
    List<List<PipelineStep>> levels = new ArrayList<>();
    Set<String> completed = new HashSet<>();
    Map<String, PipelineStep> stepMap = steps.stream()
        .collect(Collectors.toMap(PipelineStep::getName, s -> s));
    
    while (completed.size() < steps.size()) {
        List<PipelineStep> currentLevel = new ArrayList<>();
        
        for (PipelineStep step : steps) {
            if (completed.contains(step.getName())) continue;
            
            Set<String> deps = dependencyGraph.get(step.getName());
            if (deps.isEmpty() || completed.containsAll(deps)) {
                currentLevel.add(step);
            }
        }
        
        if (currentLevel.isEmpty()) {
            throw new DataPipelineException("Circular dependency detected or unresolvable dependencies");
        }
        
        levels.add(currentLevel);
        currentLevel.forEach(s -> completed.add(s.getName()));
    }
    
    return levels;
}
```

---

### 3. **Parallel Execution with ExecutorService**

Execute each level in parallel using Java's `ExecutorService`:

**Implementation:**
```java
private void executeStepsInParallel(List<PipelineStep> steps, YamlPipelineExecutionResult result)
        throws DataPipelineException {
    
    LOGGER.info("Executing pipeline in parallel mode with {} steps", steps.size());
    
    // Build dependency graph
    Map<String, Set<String>> dependencyGraph = buildDependencyGraph(steps);
    
    // Compute execution levels
    List<List<PipelineStep>> executionLevels = computeExecutionLevels(steps, dependencyGraph);
    
    LOGGER.info("Computed {} execution levels for parallel execution", executionLevels.size());
    
    // Execute each level in parallel
    ExecutorService executor = Executors.newFixedThreadPool(
        Math.min(steps.size(), Runtime.getRuntime().availableProcessors())
    );
    
    try {
        for (int level = 0; level < executionLevels.size(); level++) {
            List<PipelineStep> levelSteps = executionLevels.get(level);
            LOGGER.info("Executing level {} with {} parallel steps: {}", 
                level, levelSteps.size(), 
                levelSteps.stream().map(PipelineStep::getName).collect(Collectors.toList()));
            
            // Submit all steps in this level for parallel execution
            List<Future<Void>> futures = new ArrayList<>();
            
            for (PipelineStep step : levelSteps) {
                Future<Void> future = executor.submit(() -> {
                    executeStep(step, result);
                    return null;
                });
                futures.add(future);
            }
            
            // Wait for all steps in this level to complete
            for (Future<Void> future : futures) {
                try {
                    future.get(); // This will throw if the step failed
                } catch (ExecutionException e) {
                    Throwable cause = e.getCause();
                    if (cause instanceof DataPipelineException) {
                        throw (DataPipelineException) cause;
                    }
                    throw new DataPipelineException("Step execution failed", cause);
                }
            }
            
            LOGGER.info("Level {} completed successfully", level);
        }
    } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new DataPipelineException("Pipeline execution interrupted", e);
    } finally {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
```

---

### 4. **Thread-Safe Pipeline Context**

The current `pipelineContext` (Map) is not thread-safe. Need to use `ConcurrentHashMap`:

**Current Code (line ~60):**
```java
private final Map<String, Object> pipelineContext = new HashMap<>();
```

**Required Change:**
```java
private final Map<String, Object> pipelineContext = new ConcurrentHashMap<>();
```

---

### 5. **Circular Dependency Detection**

Implement cycle detection using DFS:

```java
private void detectCircularDependencies(Map<String, Set<String>> graph) 
        throws DataPipelineException {
    
    Set<String> visited = new HashSet<>();
    Set<String> recursionStack = new HashSet<>();
    
    for (String node : graph.keySet()) {
        if (hasCycle(node, graph, visited, recursionStack)) {
            throw new DataPipelineException(
                "Circular dependency detected in pipeline steps involving: " + node);
        }
    }
}

private boolean hasCycle(String node, Map<String, Set<String>> graph, 
                        Set<String> visited, Set<String> recursionStack) {
    
    if (recursionStack.contains(node)) {
        return true; // Cycle detected
    }
    
    if (visited.contains(node)) {
        return false; // Already processed
    }
    
    visited.add(node);
    recursionStack.add(node);
    
    Set<String> dependencies = graph.get(node);
    if (dependencies != null) {
        for (String dep : dependencies) {
            if (hasCycle(dep, graph, visited, recursionStack)) {
                return true;
            }
        }
    }
    
    recursionStack.remove(node);
    return false;
}
```

---

### 6. **Error Handling in Parallel Mode**

Handle failures according to the pipeline's error-handling configuration:

```java
// In executeStepsInParallel, modify the future.get() handling:

for (Future<Void> future : futures) {
    try {
        future.get();
    } catch (ExecutionException e) {
        Throwable cause = e.getCause();
        
        // Check error-handling configuration
        String errorHandling = pipeline.getExecution().getErrorHandling();
        
        if ("stop-on-error".equalsIgnoreCase(errorHandling)) {
            // Cancel all remaining futures
            futures.forEach(f -> f.cancel(true));
            throw new DataPipelineException("Pipeline failed in parallel execution", cause);
        } else if ("continue-on-error".equalsIgnoreCase(errorHandling)) {
            // Log error but continue
            LOGGER.error("Step failed but continuing due to error-handling policy", cause);
        }
    }
}
```

---

## Summary of Required Changes

### Files to Modify:
1. **`PipelineExecutor.java`** - Main implementation file

### New Methods to Add:
1. `buildDependencyGraph(List<PipelineStep>)` - Build DAG from steps
2. `computeExecutionLevels(List<PipelineStep>, Map<String, Set<String>>)` - Topological sort
3. `detectCircularDependencies(Map<String, Set<String>>)` - Cycle detection
4. `hasCycle(String, Map, Set, Set)` - DFS cycle detection helper
5. Rewrite `executeStepsInParallel(List<PipelineStep>, YamlPipelineExecutionResult)` - Actual parallel execution

### Fields to Change:
1. `pipelineContext` - Change from `HashMap` to `ConcurrentHashMap`

### Dependencies:
- `java.util.concurrent.ExecutorService`
- `java.util.concurrent.Executors`
- `java.util.concurrent.Future`
- `java.util.concurrent.ExecutionException`
- `java.util.concurrent.TimeUnit`

---

## Testing Strategy

### Existing Tests:
- `PipelineStepDependencyTest.java` - Already has tests for dependency handling
- Tests include: dependency order, failed dependencies, optional steps, circular dependencies

### New Tests Needed:
1. **Parallel execution timing test** - Verify steps actually run in parallel (measure timing)
2. **Parallel execution with multiple levels** - Verify correct level-by-level execution
3. **Parallel execution with error handling** - Verify stop-on-error and continue-on-error work correctly

---

## Estimated Effort

- **Implementation:** 4-6 hours
- **Testing:** 2-3 hours
- **Documentation:** 1 hour
- **Total:** ~8-10 hours

---

## Benefits of Implementation

1. **Performance:** Steps without dependencies can run concurrently
2. **Scalability:** Better utilization of multi-core systems
3. **Flexibility:** Users can choose between sequential and parallel modes
4. **Correctness:** Dependency management ensures correct execution order

---

## Example YAML Configuration

```yaml
pipeline:
  name: "parallel-etl-pipeline"
  description: "ETL pipeline with parallel execution"
  
  steps:
    # Level 0: No dependencies - run in parallel
    - name: "extract-customers"
      type: "extract"
      source: "customer-db"
      operation: "getAllCustomers"
    
    - name: "extract-orders"
      type: "extract"
      source: "order-db"
      operation: "getAllOrders"
    
    # Level 1: Depend on Level 0 - run in parallel after Level 0 completes
    - name: "transform-customers"
      type: "transform"
      depends-on: ["extract-customers"]
    
    - name: "transform-orders"
      type: "transform"
      depends-on: ["extract-orders"]
    
    # Level 2: Depend on Level 1 - run after Level 1 completes
    - name: "load-data-warehouse"
      type: "load"
      sink: "data-warehouse"
      depends-on: ["transform-customers", "transform-orders"]
  
  execution:
    mode: "parallel"
    error-handling: "stop-on-error"
```

**Execution Flow:**
- **Level 0:** `extract-customers` and `extract-orders` run in parallel
- **Level 1:** After Level 0 completes, `transform-customers` and `transform-orders` run in parallel
- **Level 2:** After Level 1 completes, `load-data-warehouse` runs

---

## References

- **PipelineStep.java:** Lines 34-35 (depends-on field), Lines 190-192 (hasDependencies method)
- **PipelineExecutor.java:** Lines 231-236 (current stub implementation)
- **PipelineStepDependencyTest.java:** Existing dependency tests
- **APEX_DATA_PIPELINE_ORCHESTRATION_GUIDE.md:** Pipeline orchestration documentation

