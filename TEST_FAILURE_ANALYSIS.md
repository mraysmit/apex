# Scenario Test Failures - Root Cause Analysis

## Summary
7 test failures in apex-core Scenario tests, all related to concurrent execution and load testing. Tests are getting 0 successful executions when expecting 50-100.

## Failing Tests

### 1. ScenarioConcurrentAccessTest (3 failures)
- **testConcurrentCacheAccess**: Expected 50 successful accesses, got 0
- **testMultiThreadedScenarioExecution**: Expected 100 successful executions, got 0  
- **testResultIsolationBetweenThreads**: Thread 0 execution failed

### 2. ScenarioLoadTest (3 failures)
- **testConcurrentLoadProcessing**: Expected 50 successful scenarios, got 0
- **testSequentialLoadProcessing**: Most scenarios should succeed but failed
- **testPerformanceConsistency**: Performance variance too high (100%)

### 3. ScenarioMemoryProfilingTest (1 failure)
- **testMemoryLeakDetection**: Memory growth should stabilize but didn't

## Root Cause

All tests are calling `ScenarioStageExecutor.executeStages()` with stages that reference non-existent YAML config files:

```java
ScenarioStage stage = new ScenarioStage("test-stage", "config/test.yaml", 1);
```

The `executeStages()` method tries to load these files:
```java
YamlRuleConfiguration stageConfig = configLoader.loadFromFile(stage.getConfigFile());
```

When the file doesn't exist, the loader throws an exception or returns null, causing all executions to fail silently.

## Test Pattern Issues

The tests use a simplified pattern that doesn't match the actual APEX architecture:

1. **Missing YAML Configuration Files**: Tests reference `config/test.yaml`, `config/stage1.yaml`, etc. that don't exist
2. **Incomplete Setup**: Tests don't properly initialize the scenario with valid configurations
3. **No Error Handling**: Tests don't catch or log the actual exceptions being thrown
4. **Unrealistic Expectations**: Tests expect 100% success rate without proper configuration

## Solution Approach

### Option 1: Fix Tests to Use Real Configuration
- Create minimal YAML configuration files for tests
- Use existing test YAML files from other scenario tests
- Properly initialize scenarios with valid configurations

### Option 2: Mock the Configuration Loading
- Mock `YamlConfigurationLoader` to return valid configurations
- Mock `RulesEngine` to return successful results
- Focus tests on concurrent execution logic, not YAML loading

### Option 3: Simplify Tests
- Remove these complex concurrent/load tests
- Focus on unit tests for individual components
- Add integration tests with real YAML files

## Recommended Fix

**Use Option 1 + better error logging**:

1. Create test YAML configuration files in `apex-core/src/test/resources/config/`
2. Update tests to use `src/test/resources` path
3. Add try-catch blocks to log actual exceptions
4. Verify stage execution results contain error details

## Files to Modify

1. `apex-core/src/test/java/dev/mars/apex/core/service/scenario/ScenarioConcurrentAccessTest.java`
2. `apex-core/src/test/java/dev/mars/apex/core/service/scenario/ScenarioLoadTest.java`
3. `apex-core/src/test/java/dev/mars/apex/core/service/scenario/ScenarioMemoryProfilingTest.java`

## Next Steps

1. Examine existing test YAML files to understand format
2. Create minimal test YAML configurations
3. Update test code to use correct paths and add error logging
4. Re-run tests to verify fixes

