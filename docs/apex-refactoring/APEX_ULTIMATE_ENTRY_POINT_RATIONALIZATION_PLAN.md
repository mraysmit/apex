# APEX Ultimate Entry Point Rationalization Plan

**Date:** 2025-11-01  
**Status:** 🎯 **PLANNING PHASE**  
**Priority:** 🚨 **CRITICAL** - Architectural Simplification  
**Goal:** **ONE UNIVERSAL ENTRY POINT** - `RulesEngine.evaluate()`

---

## 🎯 Executive Summary

### The Vision

**APEX should have ONE universal entry point that processes ANY YAML configuration without requiring developers to know the content structure in advance.**

### Current State (11 Entry Points)

Based on `APEX_TEST_ENTRY_POINTS_ANALYSIS.md`, we have:

**Category 1: RulesEngine Entry Points (5 patterns)**
1. `YamlRulesEngineService.createRulesEngineFromFile()` - 18 files
2. `YamlRulesEngineService.createRulesEngineFromYamlConfig()` - 47 files
3. `YamlRulesEngineService.createRulesEngineFromMultipleFiles()` - 3 files
4. `new RulesEngine(RulesEngineConfiguration)` - 15 files
5. `RulesEngineService.createRulesEngineFromFile()` - 2 files (DEPRECATED)

**Category 2: EnrichmentProcessor Entry Points (1 pattern)**
6. `new YamlEnrichmentProcessor(registry, evaluator)` - 25 files

**Category 3: Specialized Engines (3 patterns)**
7. `new DataPipelineEngine()` - 17 files
8. `new DataTypeScenarioService()` - 11 files
9. `new SimpleRulesEngine()` - 1 file

**Total**: 99 test files using 11 different entry points

### Target State (1 Entry Point)

```java
// UNIVERSAL PATTERN - works for ANY YAML content
YamlConfigurationLoader loader = new YamlConfigurationLoader();
YamlRuleConfiguration config = loader.loadFromFile("any-business-logic.yaml");

RulesEngine engine = new RulesEngine(config);  // Auto-detects processing mode
RuleResult result = engine.evaluate(config, inputData);
```

**Key Principle**: Developer should NOT need to know whether YAML contains:
- Enrichments only
- Rules only
- Rule groups
- Pipelines
- Scenarios
- Mixed content

### Implementation Strategy

**⚡ BREAKING CHANGES FIRST APPROACH** (User's Recommendation):
1. Make ALL breaking changes in apex-core first
2. Let ~99 tests break (EXPECTED)
3. Fix failures systematically by pattern group
4. Complete in **6 weeks** instead of 9+ weeks

**Why**: Not in production yet = perfect time for breaking changes. Test failures reveal exactly what needs migration.

---

## 🚨 The Fundamental Problem

### Content-Aware Service Selection Anti-Pattern

**Current broken pattern**:
```java
// ❌ WRONG: Developer must inspect YAML to choose correct service
if (yamlContainsOnlyEnrichments) {
    YamlEnrichmentProcessor processor = new YamlEnrichmentProcessor(...);
    result = processor.processEnrichments(...);
} else if (yamlContainsPipeline) {
    DataPipelineEngine engine = new DataPipelineEngine();
    result = engine.executePipeline(...);
} else if (yamlContainsScenario) {
    DataTypeScenarioService service = new DataTypeScenarioService();
    result = service.processScenario(...);
} else {
    RulesEngine engine = new RulesEngine(...);
    result = engine.evaluate(...);
}
```

**Correct universal pattern**:
```java
// ✅ CORRECT: One entry point handles everything
RulesEngine engine = new RulesEngine(config);
RuleResult result = engine.evaluate(config, inputData);
// Automatically processes: enrichments, rules, rule-groups, pipelines, scenarios, etc.
```

---

## 📊 Impact Analysis

### Files Affected by Entry Point

| **Entry Point** | **File Count** | **Migration Complexity** | **Risk Level** |
|-----------------|----------------|-------------------------|----------------|
| Pattern 1.2 (createRulesEngineFromYamlConfig) | 47 | LOW | 🟢 Low |
| Pattern 2.1 (new YamlEnrichmentProcessor) | 25 | MEDIUM | 🟡 Medium |
| Pattern 1.1 (createRulesEngineFromFile) | 18 | LOW | 🟢 Low |
| Pattern 3.1 (DataPipelineEngine) | 17 | HIGH | 🔴 High |
| Pattern 1.4 (new RulesEngine) | 15 | LOW | 🟢 Low |
| Pattern 3.2 (DataTypeScenarioService) | 11 | HIGH | 🔴 High |
| Pattern 1.3 (createRulesEngineFromMultipleFiles) | 3 | LOW | 🟢 Low |
| Pattern 1.5 (RulesEngineService) | 2 | LOW | 🟢 Low |
| Pattern 3.3 (SimpleRulesEngine) | 1 | MEDIUM | 🟡 Medium |
| **TOTAL** | **99** | | |

### Risk Assessment

**🟢 Low Risk (85 files)**: Patterns 1.1, 1.2, 1.3, 1.4, 1.5
- Already using RulesEngine or factory methods
- Simple migration to universal pattern

**🟡 Medium Risk (26 files)**: Patterns 2.1, 3.3
- Using specialized processors for specific content types
- Need to verify RulesEngine handles these cases

**🔴 High Risk (28 files)**: Patterns 3.1, 3.2
- Using completely different engines (Pipeline, Scenario)
- May require RulesEngine enhancement to support these use cases

---

## 🎯 Revised Implementation Strategy

### ⚡ BREAKING CHANGES FIRST APPROACH

**User's Recommendation**: Make all changes in apex-core first, then see which tests break.

**Rationale**:
- We're not in production yet - breaking changes are acceptable
- Test failures will reveal exactly what needs migration
- Faster than migrating 99 files one-by-one
- Forces comprehensive testing of new universal pattern

---

## 🎯 Phased Migration Plan

### Phase 1: Foundation - Enhance RulesEngine (Weeks 1-2)

**Goal**: Ensure RulesEngine can handle ALL YAML content types

**⚡ BREAKING CHANGES APPROACH**: Make all apex-core changes first, let tests break, then fix them

#### Task 1.1: Simplify RulesEngine Constructor (BREAKING CHANGE)

**Current Complex Constructor**:
```java
public RulesEngine(RulesEngineConfiguration config,
                   SpelExpressionParser parser,
                   ErrorRecoveryService errorService,
                   RulePerformanceMonitor monitor,
                   EnrichmentService enrichmentService)
```

**New Simple Constructor**:
```java
public RulesEngine(YamlRuleConfiguration config) {
    // Auto-create all required services
    this.config = config;
    this.parser = new SpelExpressionParser();
    this.errorService = new ErrorRecoveryService();
    this.monitor = new RulePerformanceMonitor();
    this.enrichmentService = new EnrichmentService(
        new LookupServiceRegistry(),
        new ExpressionEvaluatorService()
    );
    this.pipelineEngine = new DataPipelineEngine();
    this.scenarioService = new DataTypeScenarioService();
}
```

**Impact**: All tests using `new RulesEngine(...)` will break - EXPECTED

#### Task 1.2: Verify RulesEngine Capabilities
- ✅ Enrichments processing - ALREADY SUPPORTED
- ✅ Rules processing - ALREADY SUPPORTED
- ✅ Rule groups processing - ALREADY SUPPORTED
- ❓ Pipeline processing - NEEDS VERIFICATION
- ❓ Scenario processing - NEEDS VERIFICATION
- ❓ Transformation processing - NEEDS VERIFICATION

#### Task 1.3: Add Missing Capabilities to RulesEngine
```java
// RulesEngine should auto-detect and process:
public RuleResult evaluate(YamlRuleConfiguration config, Map<String, Object> inputData) {
    // Phase 1: Process enrichments (if present)
    if (config.hasEnrichments()) {
        processEnrichments(config, inputData);
    }
    
    // Phase 2: Process pipelines (if present)
    if (config.hasPipeline()) {
        processPipeline(config, inputData);
    }
    
    // Phase 3: Process scenarios (if present)
    if (config.hasScenarios()) {
        processScenarios(config, inputData);
    }
    
    // Phase 4: Process rules (if present)
    if (config.hasRules() || config.hasRuleGroups()) {
        processRules(config, inputData);
    }
    
    return buildResult(inputData);
}
```

#### Task 1.4: Deprecate Factory Methods (BREAKING CHANGE)

**Mark as @Deprecated with forRemoval=true**:
```java
@Deprecated(since = "3.0", forRemoval = true)
public RulesEngine createRulesEngineFromYamlConfig(YamlRuleConfiguration config) {
    LOGGER.warning("DEPRECATED: Use new RulesEngine(config) instead");
    return new RulesEngine(config);
}

@Deprecated(since = "3.0", forRemoval = true)
public RulesEngine createRulesEngineFromFile(String filePath) {
    LOGGER.warning("DEPRECATED: Use YamlConfigurationLoader + new RulesEngine(config)");
    // ...
}
```

**Impact**: 68 tests using factory methods will get deprecation warnings - EXPECTED

#### Task 1.5: Deprecate Specialized Engines (BREAKING CHANGE)

**Mark as @Deprecated**:
```java
@Deprecated(since = "3.0", forRemoval = true)
public class YamlEnrichmentProcessor {
    // 25 files will break
}

@Deprecated(since = "3.0", forRemoval = true)
public class DataPipelineEngine {
    // 17 files will break
}

@Deprecated(since = "3.0", forRemoval = true)
public class DataTypeScenarioService {
    // 11 files will break
}
```

**Impact**: 53 tests using specialized engines will break - EXPECTED

**Deliverables**:
- ✅ RulesEngine supports all YAML content types
- ✅ Simple constructor that auto-initializes dependencies
- ✅ All old entry points deprecated
- ⚠️ **EXPECTED: ~99 test failures in apex-demo**

---

### Phase 2: Fix Broken Tests (Weeks 2-4)

**Goal**: Fix all broken tests revealed by Phase 1 changes

**Strategy**: Let the test failures guide the migration

#### Task 2.1: Run Tests and Identify Failures

```bash
cd apex-demo
mvn clean test
```

**Expected Result**: ~99 test failures due to:
- Deprecated factory methods
- Deprecated specialized engines
- Changed RulesEngine constructor signature

#### Task 2.2: Categorize Failures by Pattern

**Group 1: Factory Method Failures (68 files)**
- Pattern 1.1 (18 files) - createRulesEngineFromFile
- Pattern 1.2 (47 files) - createRulesEngineFromYamlConfig
- Pattern 1.3 (3 files) - createRulesEngineFromMultipleFiles

**Group 2: Specialized Engine Failures (26 files)**
- Pattern 2.1 (25 files) - YamlEnrichmentProcessor
- Pattern 3.3 (1 file) - SimpleRulesEngine

**Group 3: High-Risk Failures (28 files)**
- Pattern 3.1 (17 files) - DataPipelineEngine
- Pattern 3.2 (11 files) - DataTypeScenarioService

**Group 4: Direct Constructor Failures (15 files)**
- Pattern 1.4 (15 files) - new RulesEngine(complex constructor)

#### Task 2.3: Fix Group 1 - Factory Method Failures (68 files)

**Migration Pattern**:
```java
// BEFORE:
YamlRulesEngineService service = new YamlRulesEngineService();
RulesEngine engine = service.createRulesEngineFromYamlConfig(config);

// AFTER:
RulesEngine engine = new RulesEngine(config);
```

**Approach**: Batch fix with find/replace where possible

#### Task 2.4: Fix Group 4 - Direct Constructor Failures (15 files)

**Migration Pattern**:
```java
// BEFORE:
RulesEngine engine = new RulesEngine(config, parser, errorService, monitor, enrichmentService);

// AFTER:
RulesEngine engine = new RulesEngine(config);
```

**Approach**: Simple constructor call replacement

**Deliverables**:
- ✅ 83 files migrated (Groups 1 + 4)
- ✅ Tests passing for migrated files
- ⚠️ 26 files still failing (Groups 2 + 3)

---

### Phase 3: Fix Specialized Engine Failures (Week 3)

**Goal**: Fix 26 medium-risk failures (Groups 2)

#### Task 3.1: Fix Pattern 2.1 - YamlEnrichmentProcessor (25 files)
```java
// BEFORE: Enrichment-only processing
YamlEnrichmentProcessor processor = new YamlEnrichmentProcessor(registry, evaluator);
Object result = processor.processEnrichments(config.getEnrichments(), testData);

// AFTER: Universal processing
RulesEngine engine = new RulesEngine(config);
RuleResult result = engine.evaluate(config, testData);
Object enrichedData = result.getEnrichedData();
```

**Challenge**: Tests may be specifically testing enrichment-only behavior
**Solution**: Verify RulesEngine handles enrichment-only YAML files correctly

#### Task 3.2: Fix Pattern 3.3 - SimpleRulesEngine (1 file)

```java
// BEFORE: Rules-only engine
SimpleRulesEngine engine = new SimpleRulesEngine();

// AFTER: Universal engine
RulesEngine engine = new RulesEngine(config);
```

**Deliverables**:
- ✅ 26 files migrated (Group 2)
- ✅ Enrichment-only tests passing
- ✅ Rules-only tests passing
- ⚠️ 28 files still failing (Group 3)

---

### Phase 4: Fix High-Risk Pipeline/Scenario Failures (Weeks 4-5)

**Goal**: Fix 28 high-risk failures (Group 3)

**Critical Decision Point**: Can RulesEngine handle pipeline/scenario processing?

#### Task 4.1: Investigate Pipeline Test Failures (17 files)

**Approach**: Run the tests and see what breaks

```bash
# Run ETL tests specifically
mvn test -Dtest="**/etl/**/*Test.java"
```

**Expected Failures**: Tests using `DataPipelineEngine` directly

**Analysis Questions**:
1. What methods are tests calling on DataPipelineEngine?
2. What YAML structure do pipeline tests use?
3. Can RulesEngine.evaluate() handle this, or does it need enhancement?

**Decision Tree**:
- **If pipeline is just orchestrated enrichments/rules**: Migrate to RulesEngine directly
- **If pipeline needs special handling**: Add pipeline support to RulesEngine.evaluate()
- **If pipeline is fundamentally different**: Keep DataPipelineEngine as internal delegate

#### Task 4.2: Investigate Scenario Test Failures (11 files)

**Approach**: Run the tests and see what breaks

```bash
# Run scenario tests specifically
mvn test -Dtest="**/scenario/**/*Test.java"
```

**Expected Failures**: Tests using `DataTypeScenarioService` directly

**Analysis Questions**:
1. What is a "scenario" in APEX terms?
2. What methods are tests calling on DataTypeScenarioService?
3. Can scenarios be handled by RulesEngine.evaluate()?

**Decision Tree**:
- **If scenario is just orchestrated rules**: Migrate to RulesEngine directly
- **If scenario needs special handling**: Add scenario support to RulesEngine.evaluate()
- **If scenario is fundamentally different**: Keep ScenarioService as internal delegate

#### Task 4.3: Implement Required RulesEngine Enhancements

**Based on Task 4.1 and 4.2 findings, enhance RulesEngine**:

```java
public RuleResult evaluate(YamlRuleConfiguration config, Map<String, Object> inputData) {
    // Auto-detect content type and delegate appropriately

    if (config.hasPipeline()) {
        return processPipelineContent(config, inputData);
    }

    if (config.hasScenario()) {
        return processScenarioContent(config, inputData);
    }

    // Standard enrichment + rules processing
    return processStandardContent(config, inputData);
}
```

#### Task 4.4: Fix All Pipeline/Scenario Tests

**Migration Pattern** (if RulesEngine can handle it):
```java
// BEFORE:
DataPipelineEngine pipelineEngine = new DataPipelineEngine();
PipelineExecutionResult result = pipelineEngine.executePipeline(...);

// AFTER:
RulesEngine engine = new RulesEngine(config);
RuleResult result = engine.evaluate(config, inputData);
```

**Deliverables**:
- ✅ RulesEngine enhanced for pipeline/scenario support (if needed)
- ✅ 28 high-risk files migrated
- ✅ All ETL tests passing
- ✅ All scenario tests passing
- ✅ **ALL 99 TESTS NOW PASSING**

---

### Phase 5: Clean Up and Documentation (Week 6)

**Goal**: Remove deprecated code and update documentation

#### Task 5.1: Remove Deprecated Entry Points

**Since all tests are now migrated, REMOVE (not just deprecate)**:
```java
// REMOVE ENTIRELY:
- YamlRulesEngineService.createRulesEngineFromYamlConfig()
- YamlRulesEngineService.createRulesEngineFromFile()
- YamlRulesEngineService.createRulesEngineFromMultipleFiles()
- YamlEnrichmentProcessor class (if not used internally)
- DataPipelineEngine class (if fully integrated into RulesEngine)
- DataTypeScenarioService class (if fully integrated into RulesEngine)
- SimpleRulesEngine class
```

**Note**: Keep specialized engines as internal delegates if RulesEngine uses them

#### Task 5.2: Update Documentation

- Update all guides to show universal pattern only
- Remove references to deprecated entry points
- Update APEX_YAML_REFERENCE.md with universal pattern examples
- Update README files in apex-demo

#### Task 5.3: Final Verification

```bash
# Run full test suite
mvn clean test

# Verify no deprecated method usage
grep -r "@Deprecated" apex-core/src/main/java

# Verify all tests use universal pattern
grep -r "createRulesEngineFrom" apex-demo/src/test/java  # Should return nothing
grep -r "new YamlEnrichmentProcessor" apex-demo/src/test/java  # Should return nothing
```

**Deliverables**:
- ✅ 11 entry points → 1 entry point
- ✅ Simplified architecture
- ✅ All deprecated code removed
- ✅ Documentation updated
- ✅ All 99 tests passing with universal pattern

---

## 🎯 Success Criteria

### Quantitative Metrics

| **Metric** | **Before** | **After** | **Improvement** |
|------------|-----------|---------|----------------|
| **Entry Points** | 11 | 1 | 91% reduction |
| **Factory Methods** | 8 | 0 | 100% reduction |
| **Specialized Engines** | 4 | 0 | 100% reduction |
| **Lines of Code** | ~5000 | ~2000 | 60% reduction |
| **Test Complexity** | High | Low | Significant |
| **Implementation Time** | 9+ weeks | **6 weeks** | **33% faster** |

### Qualitative Goals

✅ **Developer Experience**:
- No need to inspect YAML content before choosing entry point
- Single, obvious way to process any YAML configuration
- Clear, simple API

✅ **Maintainability**:
- One code path to maintain instead of 11
- Easier to add new YAML section types
- Reduced testing surface

✅ **Architecture**:
- Content-agnostic design
- Universal processing model
- Future-proof for new YAML features

---

## 📋 Implementation Checklist (REVISED - Breaking Changes First)

### Phase 1: Foundation - Make Breaking Changes in apex-core (Week 1)
- [ ] Simplify RulesEngine constructor to `new RulesEngine(YamlRuleConfiguration)`
- [ ] Verify RulesEngine handles enrichments (should already work)
- [ ] Verify RulesEngine handles rules (should already work)
- [ ] Verify RulesEngine handles rule-groups (should already work)
- [ ] Deprecate all factory methods in YamlRulesEngineService
- [ ] Deprecate YamlEnrichmentProcessor class
- [ ] Deprecate DataPipelineEngine class
- [ ] Deprecate DataTypeScenarioService class
- [ ] Deprecate SimpleRulesEngine class
- [ ] Commit changes to apex-core
- [ ] **EXPECTED: ~99 test failures in apex-demo**

### Phase 2: Fix Factory Method Failures (Week 2)
- [ ] Run `mvn test` in apex-demo and capture failures
- [ ] Fix 47 files using Pattern 1.2 (createRulesEngineFromYamlConfig)
- [ ] Fix 18 files using Pattern 1.1 (createRulesEngineFromFile)
- [ ] Fix 3 files using Pattern 1.3 (createRulesEngineFromMultipleFiles)
- [ ] Fix 15 files using Pattern 1.4 (new RulesEngine with old constructor)
- [ ] Fix 2 files using Pattern 1.5 (RulesEngineService)
- [ ] Verify 85 files now pass tests
- [ ] **REMAINING: ~26 failures**

### Phase 3: Fix Specialized Engine Failures (Week 3)
- [ ] Fix 25 files using Pattern 2.1 (YamlEnrichmentProcessor)
- [ ] Fix 1 file using Pattern 3.3 (SimpleRulesEngine)
- [ ] Verify enrichment-only tests pass
- [ ] Verify rules-only tests pass
- [ ] **REMAINING: ~28 failures (pipeline/scenario)**

### Phase 4: Fix Pipeline/Scenario Failures (Weeks 4-5)
- [ ] Run ETL tests and analyze failures (17 files)
- [ ] Run scenario tests and analyze failures (11 files)
- [ ] Determine if RulesEngine needs enhancement for pipelines
- [ ] Determine if RulesEngine needs enhancement for scenarios
- [ ] Implement required RulesEngine enhancements
- [ ] Fix all 17 pipeline test files
- [ ] Fix all 11 scenario test files
- [ ] Verify all ETL tests pass
- [ ] Verify all scenario tests pass
- [ ] **ALL 99 TESTS NOW PASSING**

### Phase 5: Clean Up and Documentation (Week 6)
- [ ] Remove all deprecated entry points from apex-core
- [ ] Update all documentation to show universal pattern
- [ ] Remove references to old entry points from guides
- [ ] Update APEX_YAML_REFERENCE.md
- [ ] Final verification: `mvn clean test` (all tests pass)
- [ ] Verify no deprecated code remains
- [ ] Verify no old entry point usage in tests
- [ ] Create release notes

---

## 🚨 Risk Mitigation

### Risk 1: Pipeline Processing Incompatibility
**Risk**: DataPipelineEngine may have fundamentally different processing model
**Mitigation**:
- Thorough analysis in Phase 4
- Keep DataPipelineEngine as internal delegate if needed
- RulesEngine can wrap pipeline results in RuleResult

### Risk 2: Scenario Processing Incompatibility
**Risk**: DataTypeScenarioService may not fit RulesEngine model
**Mitigation**:
- Analyze scenario requirements early
- Consider scenarios as orchestrated rule/enrichment sequences
- Keep ScenarioService as internal delegate if needed

### Risk 3: Breaking Changes for External Users
**Risk**: External code may depend on current entry points
**Mitigation**:
- Long deprecation period (2+ versions)
- Clear migration guides
- Backward compatibility during transition
- Comprehensive release notes

### Risk 4: Performance Regression
**Risk**: Universal entry point may be slower than specialized engines
**Mitigation**:
- Performance benchmarks before/after
- Optimize hot paths
- Keep specialized engines as internal optimizations if needed

---

## 📊 Expected Benefits

### For Developers
- ✅ **Simpler API**: One entry point instead of 11
- ✅ **No Content Inspection**: Don't need to know YAML structure
- ✅ **Faster Development**: Less time choosing the right entry point
- ✅ **Fewer Errors**: Can't choose wrong entry point

### For Maintainers
- ✅ **Less Code**: 60% reduction in entry point code
- ✅ **Easier Testing**: One code path instead of 11
- ✅ **Simpler Architecture**: Clear, unified design
- ✅ **Better Documentation**: One pattern to document

### For APEX Project
- ✅ **Professional API**: Industry-standard single entry point
- ✅ **Future-Proof**: Easy to add new YAML section types
- ✅ **Better Adoption**: Simpler API = more users
- ✅ **Reduced Bugs**: Fewer code paths = fewer bugs

---

## 🎯 Final Target Architecture

### The Universal Pattern

```java
// STEP 1: Load YAML (any content type)
YamlConfigurationLoader loader = new YamlConfigurationLoader();
YamlRuleConfiguration config = loader.loadFromFile("business-logic.yaml");

// STEP 2: Create RulesEngine (auto-initializes everything)
RulesEngine engine = new RulesEngine(config);

// STEP 3: Execute (processes whatever is in the YAML)
RuleResult result = engine.evaluate(config, inputData);

// STEP 4: Get results
Map<String, Object> enrichedData = result.getEnrichedData();
boolean success = result.isSuccess();
List<String> messages = result.getMessages();
```

### What RulesEngine.evaluate() Does Internally

```java
public RuleResult evaluate(YamlRuleConfiguration config, Map<String, Object> inputData) {
    // Auto-detect processing mode
    ProcessingMode mode = config.getMetadata().getProcessingMode();

    // Process in document order (if sequential) or standard order
    if (mode == ProcessingMode.SEQUENTIAL) {
        return processSequentially(config, inputData);
    } else {
        return processStandard(config, inputData);
    }
}

private RuleResult processSequentially(YamlRuleConfiguration config, Map<String, Object> inputData) {
    // Process sections in YAML document order
    for (YamlSection section : config.getSectionsInOrder()) {
        switch (section.getType()) {
            case ENRICHMENTS -> processEnrichments(section, inputData);
            case ENRICHMENT_GROUPS -> processEnrichmentGroups(section, inputData);
            case RULES -> processRules(section, inputData);
            case RULE_GROUPS -> processRuleGroups(section, inputData);
            case PIPELINE -> processPipeline(section, inputData);
            case SCENARIO -> processScenario(section, inputData);
            case TRANSFORMATIONS -> processTransformations(section, inputData);
        }
    }
    return buildResult(inputData);
}
```

---

## 🚀 Next Steps (REVISED APPROACH)

### Immediate Actions

1. **✅ APPROVED STRATEGY**: Make all breaking changes in apex-core first
2. **Start Phase 1**: Simplify RulesEngine constructor and deprecate all old entry points
3. **Expect ~99 test failures**: This is GOOD - it shows us exactly what needs migration
4. **Let tests guide migration**: Fix failures systematically by pattern group
5. **Complete in 6 weeks** instead of 9+ weeks

### Why This Approach is Better

✅ **Faster**: 6 weeks instead of 9+ weeks (33% faster)
✅ **More thorough**: Test failures reveal ALL migration points
✅ **Less error-prone**: No manual file-by-file migration planning
✅ **Cleaner**: Breaking changes force complete migration
✅ **Not in production**: Perfect time for breaking changes

### Implementation Order

```
Week 1: Make ALL breaking changes in apex-core
        ↓
        ~99 test failures (EXPECTED)
        ↓
Week 2: Fix factory method failures (85 files)
        ↓
        ~26 test failures remaining
        ↓
Week 3: Fix specialized engine failures (26 files)
        ↓
        ~28 test failures remaining
        ↓
Weeks 4-5: Fix pipeline/scenario failures (28 files)
        ↓
        ALL TESTS PASSING
        ↓
Week 6: Remove deprecated code, update docs
        ↓
        COMPLETE: 11 → 1 entry point
```

---

**Key Principle**: APEX should have **ONE** way to process YAML configurations, regardless of content type. Developers focus on business logic, not YAML structure inspection.

**Implementation Strategy**: Break everything first, then fix it systematically. Not in production = perfect time for breaking changes.

