# RulesEngine Refactoring Plan

**Date:** January 22, 2026  
**Status:** PLANNING  
**Target Version:** APEX 2.2  
**Priority:** HIGH - Maintainability & Technical Debt

## Executive Summary

The `RulesEngine` class has grown to **3,307 lines** and **157 KB**, violating the Single Responsibility Principle. This document outlines a comprehensive refactoring strategy to decompose it into focused, maintainable components while preserving backward compatibility and the APEX 2.1 external data-source reference architecture.

## Current State Analysis

### File Metrics
- **Lines of Code:** 3,307
- **File Size:** 157 KB
- **Methods:** 75+
- **Responsibilities:** 12+ distinct areas

### Identified Responsibilities

| Responsibility | Line Range (Approx) | Methods | Complexity |
|---------------|---------------------|---------|------------|
| Factory Methods | 380-656 | 5 | Medium |
| Builder Pattern | 2929-3307 | 15+ | High |
| Scenario Parsing | 2297-2531 | 6 | High |
| Scenario Execution | 1950-2140 | 4 | Medium |
| Rule Execution | 713-1195 | 8 | Medium |
| Enrichment Processing | 838-1024 | 3 | Medium |
| Rule Chain Execution | 2606-2929 | 4 | Very High |
| Pipeline Execution | 271-342 | 2 | Medium |
| Sequential Processing | 1256-1880 | 7 | Very High |
| Deep Copy Utilities | 2159-2253 | 2 | Low |
| Data Source Registry | 330-375 | 4 | Low |
| SpEL Context Management | 701-712 | 1 | Low |

### Dependencies Analysis

**External Dependencies:**
- `YamlConfigurationLoader` - YAML parsing
- `ScenarioStageExecutor` - Stage execution
- `EnrichmentService` - Enrichment processing
- `RulePerformanceMonitor` - Performance tracking
- Spring Expression Language (SpEL) - Expression evaluation

**Internal Dependencies:**
- 50+ classes used across methods
- Tight coupling to YAML configuration model
- Heavy use of `YamlRuleConfiguration` throughout

## Refactoring Goals

### Primary Objectives
1. **Reduce Complexity** - Break down into cohesive, single-responsibility classes
2. **Improve Testability** - Enable isolated unit testing of components
3. **Maintain Compatibility** - 100% backward compatibility for existing API
4. **Preserve Architecture** - Keep APEX 2.1 external data-source reference system
5. **Enable Extension** - Make it easier to add new processing patterns

### Non-Goals
- ❌ Change public API signatures
- ❌ Modify YAML configuration format
- ❌ Alter execution behavior or results
- ❌ Break existing demos or tests

## Proposed Architecture

### New Class Structure

```
dev.mars.apex.core.engine.config/
├── RulesEngine.java (FACADE - 300-500 lines)
│   ├── Public API methods (delegate to components)
│   └── Backward compatibility layer
│
├── factory/
│   ├── RulesEngineFactory.java (FACTORY)
│   │   ├── fromFile()
│   │   ├── fromClasspath()
│   │   ├── fromYamlConfig()
│   │   ├── fromScenarioRegistry()
│   │   └── evaluateYaml(), evaluateYamlFile()
│   │
│   └── RulesEngineBuilder.java (BUILDER)
│       ├── Extracted from inner Builder class
│       ├── Path resolution logic
│       └── Configuration assembly
│
├── execution/
│   ├── RuleExecutor.java (RULES)
│   │   ├── executeRule()
│   │   ├── executeRulesList()
│   │   ├── executeRuleGroupsList()
│   │   ├── executeRules()
│   │   └── evaluateRule()
│   │
│   ├── EnrichmentExecutor.java (ENRICHMENTS)
│   │   ├── executeEnrichmentGroupsList()
│   │   ├── processEnrichmentGroup()
│   │   └── aggregateEnrichmentResults()
│   │
│   ├── RuleChainExecutor.java (CHAINS)
│   │   ├── executeRuleChain()
│   │   ├── executeResultBasedRoutingPattern()
│   │   ├── executeConditionalChainingPattern()
│   │   └── executeSequentialDependencyPattern()
│   │
│   ├── PipelineExecutor.java (PIPELINES)
│   │   ├── executePipeline()
│   │   └── initializePipelineComponents()
│   │
│   └── SequentialProcessor.java (SEQUENTIAL)
│       ├── evaluateSequential()
│       ├── processItem()
│       ├── processEnrichmentItem()
│       ├── processRuleItem()
│       ├── processEnrichmentGroupItem()
│       ├── processRuleGroupItem()
│       ├── processTransformationItem()
│       └── processRuleChainItem()
│
├── scenario/
│   ├── ScenarioExecutionCoordinator.java (COORDINATOR)
│   │   ├── evaluateScenario(Map)
│   │   ├── evaluateScenario(String, Map)
│   │   ├── evaluateWithClassification(Map)
│   │   └── getScenario()
│   │
│   ├── ScenarioParser.java (PARSER)
│   │   ├── parseScenarioFromYaml()
│   │   ├── parseScenarioConfiguration()
│   │   ├── parseScenarioStage()
│   │   ├── findMatchingScenario()
│   │   └── loadErrorRecoveryConfig()
│   │
│   └── ScenarioEvaluatorAdapter.java (ADAPTER)
│       └── Implements ScenarioEvaluator interface
│
└── util/
    ├── DataCopyUtility.java (UTILITY)
    │   ├── deepCopyMap()
    │   └── deepCopyValue()
    │
    ├── SpELContextFactory.java (UTILITY)
    │   └── createContext()
    │
    └── DataSourceRegistry.java (REGISTRY)
        └── Inner DataSourceRegistry class extraction
```

### Class Responsibility Matrix

| Class | Primary Responsibility | LOC Target | Depends On |
|-------|----------------------|-----------|------------|
| `RulesEngine` | Public API facade, delegation | 300-500 | All executors |
| `RulesEngineFactory` | Instance creation | 150-200 | Builder, Parser |
| `RulesEngineBuilder` | Fluent configuration | 200-300 | Factory |
| `RuleExecutor` | Rule execution logic | 300-400 | SpELContextFactory |
| `EnrichmentExecutor` | Enrichment processing | 200-300 | EnrichmentService |
| `RuleChainExecutor` | Chain pattern execution | 400-600 | RuleExecutor |
| `PipelineExecutor` | Pipeline orchestration | 150-200 | Data sources |
| `SequentialProcessor` | Sequential processing | 500-700 | All executors |
| `ScenarioExecutionCoordinator` | Scenario orchestration | 200-300 | ScenarioParser, ScenarioStageExecutor |
| `ScenarioParser` | YAML to scenario objects | 250-350 | YamlConfigurationLoader |
| `ScenarioEvaluatorAdapter` | Interface implementation | 50-100 | ScenarioExecutionCoordinator |
| `DataCopyUtility` | Deep copy operations | 100-150 | None |
| `SpELContextFactory` | SpEL context creation | 50-100 | Spring SpEL |
| `DataSourceRegistry` | Data source management | 100-150 | ExternalDataSource |

## Implementation Strategy

### Phase 1: Preparation (Week 1)
**Goal:** Set up infrastructure without breaking changes

#### Tasks
1. **Create Package Structure**
   ```
   mkdir -p apex-core/src/main/java/dev/mars/apex/core/engine/config/factory
   mkdir -p apex-core/src/main/java/dev/mars/apex/core/engine/config/execution
   mkdir -p apex-core/src/main/java/dev/mars/apex/core/engine/config/scenario
   mkdir -p apex-core/src/main/java/dev/mars/apex/core/engine/config/util
   ```

2. **Create Baseline Tests**
   - Copy all existing `RulesEngine` tests
   - Mark as "LEGACY_COMPATIBILITY_TESTS"
   - These must ALL pass after refactoring

3. **Document Current API Surface**
   - Extract all public methods
   - Document return types and parameters
   - Create API compatibility checklist

4. **Set Up Feature Flags** (if needed)
   - Environment variable: `APEX_USE_LEGACY_ENGINE=true/false`
   - Allows gradual migration

### Phase 2: Utility Extraction (Week 1-2)
**Goal:** Extract zero-dependency utilities first

#### Tasks
1. **Create `DataCopyUtility`**
   - Extract `deepCopyMap()` and `deepCopyValue()`
   - Add comprehensive tests for edge cases
   - Update `RulesEngine` to use utility

2. **Create `SpELContextFactory`**
   - Extract `createContext()` method
   - Add context caching if beneficial
   - Update all SpEL usage points

3. **Create `DataSourceRegistry`**
   - Extract inner `DataSourceRegistry` class
   - Make it a standalone component
   - Add thread-safety tests

**Verification:** Run full test suite - 0 failures

### Phase 3: Execution Components (Week 2-3)
**Goal:** Extract execution logic into focused classes

#### Tasks
1. **Create `RuleExecutor`**
   - Extract methods:
     - `executeRule()`
     - `executeRulesList()`
     - `executeRuleGroupsList()`
     - `executeRules()`
     - `executeRulesForCategory()`
     - `evaluateRule()`
     - `evaluateRules()`
     - `evaluateRulesForCategory()`
   - Inject dependencies: `SpELContextFactory`, `RulePerformanceMonitor`
   - Create comprehensive unit tests

2. **Create `EnrichmentExecutor`**
   - Extract methods:
     - `executeEnrichmentGroupsList()`
     - `processEnrichmentGroup()`
     - `aggregateEnrichmentResults()`
   - Inject dependencies: `EnrichmentService`, `YamlConfigurationLoader`
   - Test with actual enrichment demos

3. **Create `PipelineExecutor`**
   - Extract methods:
     - `executePipeline()`
     - `initializePipelineComponents()`
   - Test with data-sync scenarios

**Verification:** Run demo tests - all scenarios pass

### Phase 4: Chain and Sequential Processing (Week 3-4)
**Goal:** Extract complex processing logic

#### Tasks
1. **Create `RuleChainExecutor`**
   - Extract all rule chain pattern methods
   - Support 6 chain patterns:
     - Sequential Dependency
     - Conditional Chaining
     - Result-Based Routing
     - Priority-Based Execution
     - Parallel Execution
     - Dynamic Chain Assembly
   - Test each pattern independently

2. **Create `SequentialProcessor`**
   - Extract `evaluateSequential()` and all `processXxxItem()` methods
   - This is the most complex component
   - Coordinate with other executors
   - Extensive integration tests required

**Verification:** Run all chain demos - verify execution order

### Phase 5: Scenario Components (Week 4-5)
**Goal:** Extract scenario-specific logic

#### Tasks
1. **Create `ScenarioParser`**
   - Extract all scenario parsing methods
   - Independent of execution logic
   - Test with all demo scenario YAML files

2. **Create `ScenarioExecutionCoordinator`**
   - Extract scenario execution methods
   - Delegates to `ScenarioStageExecutor` (existing)
   - Coordinates with `ScenarioParser`
   - Test with OTC options bootstrap demo

3. **Create `ScenarioEvaluatorAdapter`**
   - Simple adapter for `ScenarioEvaluator` interface
   - Delegates to coordinator

**Verification:** Run all scenario demos - bootstrap, classification, etc.

### Phase 6: Factory and Builder (Week 5-6)
**Goal:** Extract creation logic

#### Tasks
1. **Create `RulesEngineBuilder`**
   - Extract inner `Builder` class
   - Move to top-level class
   - Keep all path resolution logic
   - Test builder patterns from demos

2. **Create `RulesEngineFactory`**
   - Extract all static factory methods:
     - `fromFile()`
     - `fromClasspath()`
     - `fromYamlConfig()`
     - `fromScenarioRegistry()`
     - `evaluateYaml()`
     - `evaluateYamlFile()`
   - Delegate to `RulesEngineBuilder` where appropriate

**Verification:** Test all factory methods with existing demos

### Phase 7: Facade Integration (Week 6)
**Goal:** Create lightweight RulesEngine facade

#### Tasks
1. **Refactor `RulesEngine` to Facade**
   ```java
   public class RulesEngine {
       // Component dependencies
       private final RuleExecutor ruleExecutor;
       private final EnrichmentExecutor enrichmentExecutor;
       private final RuleChainExecutor chainExecutor;
       private final PipelineExecutor pipelineExecutor;
       private final SequentialProcessor sequentialProcessor;
       private final ScenarioExecutionCoordinator scenarioCoordinator;
       
       // Delegate all public methods
       public RuleResult executeRule(Rule rule, Map<String, Object> facts) {
           return ruleExecutor.executeRule(rule, facts);
       }
       
       // ... (delegate remaining public methods)
   }
   ```

2. **Update Factory Methods**
   - Static factory methods delegate to `RulesEngineFactory`
   - Maintain exact same signatures

3. **Update Builder**
   - Static `builder()` returns `RulesEngineBuilder` instance
   - Builder constructs facade with all components

**Verification:** 100% backward compatibility - all existing code works unchanged

### Phase 8: Testing & Validation (Week 7)
**Goal:** Comprehensive validation

#### Tasks
1. **Compatibility Testing**
   - Run ALL 16+ demo scenarios
   - Run fast-demo-tests.bat
   - Run full test suite
   - Zero failures allowed

2. **Performance Testing**
   - Compare execution times before/after
   - No more than 5% regression
   - Check memory usage (should improve)

3. **Integration Testing**
   - Test playground UI
   - Test REST API
   - Test all bootstrap demos
   - Test external data-source demos

4. **Documentation Updates**
   - Update architectural diagrams
   - Document new class structure
   - Update developer guide
   - Migration guide (for extensibility)

**Verification:** All tests green, performance acceptable

### Phase 9: Cleanup & Optimization (Week 8)
**Goal:** Polish and optimize

#### Tasks
1. **Code Review**
   - Review all new classes
   - Ensure consistent style
   - Add missing Javadoc

2. **Remove Dead Code**
   - Identify any unused methods
   - Clean up temporary compatibility code

3. **Optimization Opportunities**
   - Cache frequently used parsers
   - Optimize deep copy for common types
   - Consider lazy initialization where safe

4. **Final Documentation**
   - Architecture decision records (ADR)
   - Component interaction diagrams
   - Update APEX_TECHNICAL_REFERENCE.md

## Backward Compatibility Strategy

### Public API Preservation

**All existing public methods MUST remain:**
```java
// Factory methods - delegate to RulesEngineFactory
public static RulesEngine fromFile(String filePath)
public static RulesEngine fromClasspath(String resourcePath)
public static RulesEngine fromYamlConfig(YamlRuleConfiguration yamlConfig)
public static RulesEngine fromScenarioRegistry(String registryPath)
public static RuleResult evaluateYaml(String yamlString, Map<String, Object> inputData)
public static RuleResult evaluateYamlFile(String yamlFilePath, Map<String, Object> inputData)

// Instance methods - delegate to executors
public RuleResult executeRule(Rule rule, Map<String, Object> facts)
public RuleResult executeRulesList(List<Rule> rules, Map<String, Object> facts)
public RuleResult executeRuleGroupsList(List<RuleGroup> ruleGroups, Map<String, Object> facts)
public ScenarioExecutionResult evaluateScenario(Map<String, Object> inputData)
public ScenarioExecutionResult evaluateScenario(String scenarioId, Map<String, Object> inputData)
public ScenarioExecutionResult evaluateWithClassification(Map<String, Object> inputData)
public RuleResult evaluate(Map<String, Object> inputData)
public RuleResult evaluate(YamlRuleConfiguration yamlConfig, Map<String, Object> inputData)

// Builder - delegate to RulesEngineBuilder
public static Builder builder()
```

### Migration Path for Extensibility

**Old (Internal Extension - Not Supported):**
```java
// Users couldn't easily extend RulesEngine
```

**New (Clean Extension Points):**
```java
// Users can now:
1. Extend RuleExecutor for custom rule evaluation
2. Extend RuleChainExecutor for new chain patterns
3. Implement custom ScenarioParser for different formats
4. Plug in custom PipelineExecutor for specific domains
```

## Risk Assessment

### High Risks

| Risk | Probability | Impact | Mitigation |
|------|------------|--------|------------|
| Breaking existing demos | Medium | Critical | Comprehensive test suite, gradual rollout |
| Performance regression | Low | High | Benchmark tests, profile before/after |
| Unintended behavior change | Medium | High | Extensive integration tests, comparison testing |
| Incomplete extraction | Medium | Medium | Phased approach, checkpoint validation |

### Medium Risks

| Risk | Probability | Impact | Mitigation |
|------|------------|--------|------------|
| Thread-safety issues | Low | Medium | Review concurrent usage, stress tests |
| Memory leaks | Low | Medium | Profile memory, long-running tests |
| Configuration edge cases | Medium | Medium | Test all YAML examples |

### Low Risks

| Risk | Probability | Impact | Mitigation |
|------|------------|--------|------------|
| Documentation gaps | High | Low | Structured documentation plan |
| Build time increase | Low | Low | Acceptable tradeoff for maintainability |

## Testing Strategy

### Test Categories

1. **Unit Tests** (New)
   - Each new class has focused unit tests
   - Mock dependencies
   - Target: 80%+ coverage per class

2. **Integration Tests** (Existing + New)
   - Test component interactions
   - Use real dependencies
   - All demo scenarios as integration tests

3. **Compatibility Tests** (New)
   - Copy of ALL existing tests
   - Must pass 100% after refactoring
   - Run in CI/CD

4. **Performance Tests** (New)
   - Benchmark before/after
   - OTC options bootstrap demo (complex scenario)
   - 1000 rule evaluation benchmark
   - Memory usage profiling

5. **Regression Tests** (Existing)
   - All existing demos
   - Fast demo tests
   - Bootstrap demos

### Success Criteria

✅ **All existing tests pass without modification**  
✅ **All 16+ demo scenarios execute successfully**  
✅ **Fast demo tests complete in under 15 seconds**  
✅ **No performance regression > 5%**  
✅ **Memory usage same or improved**  
✅ **Code coverage maintained or improved**  
✅ **Zero breaking changes to public API**  
✅ **All Javadoc complete**  
✅ **Architecture documentation updated**

## Rollout Strategy

### Development Branch Strategy
```
master (current)
  ↓
refactor/rules-engine-decomposition (main work)
  ↓
├── refactor/phase-1-utilities
├── refactor/phase-2-execution
├── refactor/phase-3-scenarios
└── refactor/phase-4-factory
```

### Deployment Phases

**Phase 1 - Internal Testing (Week 7)**
- Merge all refactoring to feature branch
- Run complete test suite
- Internal code review

**Phase 2 - Beta Testing (Week 8)**
- Deploy to test environment
- Run all demos manually
- Stress testing with large datasets

**Phase 3 - Production Release (Week 9)**
- Merge to master
- Tag as APEX 2.2
- Release notes highlighting maintainability improvements

## Metrics & Success Tracking

### Maintainability Metrics

| Metric | Current | Target | Post-Refactor |
|--------|---------|--------|---------------|
| Lines per class | 3,307 | < 800 | TBD |
| Methods per class | 75 | < 20 | TBD |
| Cyclomatic complexity | High | Medium | TBD |
| Class coupling | High | Low-Medium | TBD |
| Test coverage | ~70% | 80%+ | TBD |

### Performance Metrics

| Benchmark | Baseline | Target | Post-Refactor |
|-----------|----------|--------|---------------|
| OTC Bootstrap Demo | ~500ms | < 525ms | TBD |
| 1000 Rules Eval | ~200ms | < 210ms | TBD |
| Simple Scenario | ~50ms | < 55ms | TBD |
| Memory (bootstrap) | ~150MB | < 150MB | TBD |

## Future Enhancements (Post-Refactoring)

### Enabled by Clean Architecture

1. **Custom Execution Strategies**
   - User-defined rule execution order
   - Custom chain patterns
   - Domain-specific executors

2. **Alternative Parsers**
   - JSON scenario configuration
   - XML configuration support
   - Programmatic scenario building

3. **Performance Optimizations**
   - Parallel rule evaluation
   - Rule caching strategies
   - Lazy initialization patterns

4. **Enhanced Monitoring**
   - Per-component metrics
   - Detailed execution traces
   - Performance profiling hooks

5. **Pluggable Components**
   - Custom enrichment executors
   - Alternative pipeline engines
   - External scenario registries

## Decision Log

### Key Architectural Decisions

**ADR-001: Facade Pattern for RulesEngine**
- **Decision:** Keep RulesEngine as a facade delegating to components
- **Rationale:** Maintains 100% backward compatibility
- **Alternatives Considered:** Complete replacement (rejected - breaking change)

**ADR-002: Package Structure**
- **Decision:** Group by responsibility (factory, execution, scenario, util)
- **Rationale:** Clear separation of concerns, intuitive navigation
- **Alternatives Considered:** Flat structure (rejected - too many classes)

**ADR-003: Phased Implementation**
- **Decision:** 8-phase incremental approach
- **Rationale:** Reduces risk, allows checkpoint validation
- **Alternatives Considered:** Big-bang refactoring (rejected - too risky)

**ADR-004: Maintain External Data-Source Architecture**
- **Decision:** Preserve APEX 2.1 data-source reference system
- **Rationale:** Core differentiator, works well, no issues
- **Alternatives Considered:** None - this is a strength

**ADR-005: No Feature Flags**
- **Decision:** Direct refactoring without runtime feature flags
- **Rationale:** Internal change only, tests ensure correctness
- **Alternatives Considered:** Feature flag toggle (rejected - unnecessary complexity)

## Open Questions

1. **Should we extract rule group execution to a separate executor?**
   - Currently in `RuleExecutor`
   - Could be `RuleGroupExecutor` for clarity
   - Decision: Defer until Phase 3 review

2. **Should ScenarioStageExecutor be refactored too?**
   - Already separate class
   - Could benefit from similar decomposition
   - Decision: Separate effort post-2.2

3. **How to handle private method visibility in tests?**
   - Package-private for testing?
   - Reflection-based testing?
   - Decision: TBD during Phase 2

4. **Should we version the internal architecture?**
   - Mark components with @since 2.2
   - Internal API stability guarantees?
   - Decision: Document but no formal guarantees (internal API)

## Resources Required

### Team
- **Lead Developer:** Full-time, 8 weeks
- **Code Reviewer:** Part-time, 2-3 hours/week
- **QA/Testing:** Part-time, weeks 7-8

### Tools
- IntelliJ IDEA refactoring tools
- Git for branch management
- JProfiler/VisualVM for performance analysis
- JUnit 5 for testing
- Mockito for mocking (if needed)

### Documentation
- Architecture diagrams (Mermaid/PlantUML)
- Updated Javadoc
- Migration guide (for future extensibility)
- ADR documents

## Appendix

### A. Current Class Dependencies Map

```
RulesEngine
├── Uses → YamlConfigurationLoader
├── Uses → YamlRuleConfiguration
├── Uses → ScenarioStageExecutor
├── Uses → EnrichmentService
├── Uses → RulePerformanceMonitor
├── Uses → ExpressionParser
├── Uses → ScenarioRegistry
└── Creates → RuleResult, ScenarioExecutionResult
```

### B. Method Categorization

**Factory Methods (5):**
- fromFile, fromClasspath, fromYamlConfig, fromScenarioRegistry
- evaluateYaml, evaluateYamlFile (static convenience)

**Rule Execution (8):**
- executeRule, executeRulesList, executeRuleGroupsList, executeRules
- evaluateRule, evaluateRules, executeRulesForCategory, evaluateRulesForCategory

**Enrichment Execution (3):**
- executeEnrichmentGroupsList, processEnrichmentGroup, aggregateEnrichmentResults

**Scenario Execution (4):**
- evaluateScenario (2 overloads), evaluateWithClassification, asScenario

**Scenario Parsing (6):**
- parseScenarioFromYaml, parseScenarioConfiguration, parseScenarioStage
- getScenario, findMatchingScenario, loadErrorRecoveryConfig

**Rule Chain Execution (4):**
- processRuleChainItem, findRuleChainById
- executeResultBasedRoutingPattern, executeConditionalChainingPattern

**Sequential Processing (7):**
- evaluateSequential, processItem, processEnrichmentItem, processRuleItem
- processEnrichmentGroupItem, processRuleGroupItem, processTransformationItem

**Pipeline Execution (2):**
- executePipeline, initializePipelineComponents

**Utilities (4):**
- deepCopyMap, deepCopyValue, createContext, getSeverityPriority

**Builder (15):**
- builder() static + 14 Builder methods

### C. References

- **APEX External Data-Source Guide:** `docs/APEX_LOOKUP_CONFIGURATION_GUIDE.md`
- **APEX Scenario Guide:** `docs/APEX_SCENARIO_GUIDE.md`
- **Copilot Instructions:** `.github/copilot-instructions.md`
- **Demo Patterns:** `apex-demo/src/test/java`
- **Bootstrap Examples:** `OtcOptionsBootstrapDemo`, `CommoditySwapBootstrapDemo`

---

## Change History

| Date | Version | Author | Changes |
|------|---------|--------|---------|
| 2026-01-22 | 1.0 | AI Agent | Initial planning document |

---

**Next Steps:**
1. Review and approve this plan
2. Create feature branch
3. Begin Phase 1 (Preparation)
4. Checkpoint review after each phase
