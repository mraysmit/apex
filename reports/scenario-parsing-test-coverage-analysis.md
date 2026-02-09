# Scenario Parsing Test Coverage Analysis
## For Consolidation of Triplicated Parsing Logic

**Date**: 2025-01-22  
**Purpose**: Prove zero regressions when consolidating the three copies of scenario parsing logic into a single canonical implementation.

---

## 1. The Three Parsing Implementations (The Triplication)

### Location 1: `ScenarioRegistryLoader` (config/loader/)
- **File**: `apex-core/src/main/java/dev/mars/apex/core/config/loader/ScenarioRegistryLoader.java` (~1312 lines)
- **Parsing methods**: `parseScenarioConfiguration()` (lines ~670–810), `parseScenarioStage()` (lines ~810–860)
- **Called by**: `RulesEngine.fromScenarioRegistry()` → `ScenarioRegistryLoader.loadRegistry()`
- **This is the most feature-rich copy** — supports everything the other two do plus:
  - String-form `classification-rule` (e.g., `classification-rule: "#tradeType == 'OTCOption'"`)  
  - `enabled` flag parsing on scenarios
  - `metadata` map parsing on scenarios
  - Legacy aliases: `stage-id` → `stage-name`, `rule-configuration` → `config-file`
  - Component file detection via `validateAndLogComponentFile()`
  - `description` extraction from nested `stage-metadata`
  - Uses `stage.setProcessingStages(stages)` directly

### Location 2: `ScenarioParser` (engine/scenario/)
- **File**: `apex-core/src/main/java/dev/mars/apex/core/engine/scenario/ScenarioParser.java` (~219 lines)
- **Parsing methods**: `parseFromYaml(YamlRuleConfiguration)`, `parseScenarioConfiguration(Map)`, `parseScenarioStage(Map)`
- **Called by**: `ScenarioEvaluationManager.evaluateScenario(Map)` → `scenarioParser.parseFromYaml(this.yamlConfig)`
- **This is the "single YAML file" path** — used when `RulesEngine.fromFile("scenario.yaml").evaluateScenario(data)`
- Only supports Map-form `classification-rule`, no `enabled`/`metadata`/legacy-aliases
- Uses `ScenarioConfiguration.withStages()` factory (creates new object, overwrites)

### Location 3: `ScenarioRegistryManager` (engine/scenario/)
- **File**: `apex-core/src/main/java/dev/mars/apex/core/engine/scenario/ScenarioRegistryManager.java` (~304 lines)
- **Parsing methods**: `parseScenarioConfiguration(Map)`, `parseScenarioStage(Map)` — **near-identical to ScenarioParser**
- **Called by**: Not directly for parsing in the main flow. Primarily used for `ScenarioLookupStrategy` implementation (classification matching, scenario lookup)
- Contains `getScenario()`, `findMatchingScenario()`, `evaluateClassificationRule()`

---

## 2. Execution Flow Map

Understanding which parser each API path exercises:

```
Path A: RulesEngine.fromFile("scenario.yaml").evaluateScenario(data)
  └─ ScenarioEvaluationManager.evaluateScenario(Map)
       └─ ScenarioParser.parseFromYaml(yamlConfig)         ← LOCATION 2
            └─ ScenarioParser.parseScenarioConfiguration()
            └─ ScenarioParser.parseScenarioStage()

Path B: RulesEngine.fromScenarioRegistry("registry.yaml").evaluateScenario("id", data)
  └─ RulesEngine.fromScenarioRegistry()
       └─ ScenarioRegistryLoader.loadRegistry()             ← LOCATION 1
            └─ ScenarioRegistryLoader.parseScenarioConfiguration()
            └─ ScenarioRegistryLoader.parseScenarioStage()
  └─ ScenarioEvaluationManager.evaluateScenario(String, Map)
       └─ scenarioLookup.getScenario(id)                   ← LOCATION 3 (lookup only)
       └─ ScenarioStageExecutor.executeStages()

Path C: RulesEngine.fromScenarioRegistry("registry.yaml").evaluateWithClassification(data)
  └─ RulesEngine.fromScenarioRegistry()
       └─ ScenarioRegistryLoader.loadRegistry()             ← LOCATION 1
  └─ ScenarioEvaluationManager.evaluateWithClassification(Map)
       └─ scenarioLookup.findMatchingScenario(data)         ← LOCATION 3 (classification only)
       └─ ScenarioStageExecutor.executeStages()
```

**Key insight**: `ScenarioRegistryManager` parsing methods (`parseScenarioConfiguration`, `parseScenarioStage`) are **dead code** in the current flow. The registry path uses `ScenarioRegistryLoader` for parsing and `ScenarioRegistryManager` only for lookup/classification. The non-registry path uses `ScenarioParser`.

---

## 3. Complete Test Inventory

### 3.1 Tests Exercising Path A (ScenarioParser via fromFile/evaluateScenario)

| # | Test File | Module | What It Tests | YAML Files | Parsing Features Exercised |
|---|-----------|--------|---------------|------------|---------------------------|
| 1 | `ScenarioBasedRulesEngineTest` | apex-core | OTC option validation, multi-stage | Inline YAML via `YamlConfigurationLoader` | Modern format, processing-stages, stage-name |
| 2 | `CleanEnrichmentDemoTest` | apex-core | Enrichment returns clean data | `scenario/clean-enrichment-demo.yaml` | Modern format, processing-stages |
| 3 | `ScenarioConcurrentAccessTest` | apex-core | Thread safety of scenario execution | `ScenarioConcurrentAccessTest-scenario.yaml` | Modern format, classification-rule (Map) |
| 4 | `StageDataPersistenceTest` | apex-core | Output persistence between stages | TestConfigLoader (in-memory) | N/A (bypasses YAML parsing) |
| 5 | `ScenarioStageExecutorTest` | apex-core | Stage execution, dependencies | TestConfigLoader | N/A (bypasses YAML parsing) |
| 6 | `ScenarioTracingTest` | apex-core | Execution path capture | TestConfigLoader | N/A (bypasses YAML parsing) |
| 7 | `ConditionalStageExecutionTest` (core) | apex-core | SpEL condition evaluation | TestConfigLoader | N/A (bypasses YAML parsing) |

### 3.2 Tests Exercising Path B (ScenarioRegistryLoader via fromScenarioRegistry + evaluateScenario)

| # | Test File | Module | What It Tests | YAML Files | Parsing Features Exercised |
|---|-----------|--------|---------------|------------|---------------------------|
| 1 | `RulesEngineScenarioRegistryTest` | apex-core | Registry loading, scenario retrieval, errors | Temp-dir YAML | Modern format, registry structure |
| 2 | `RulesEngineMalformedRegistryTest` | apex-core | Malformed registry error handling | Temp-dir YAML (intentionally broken) | Error paths, missing sections |
| 3 | `RulesEngineFromScenarioRegistryTest` | apex-core | `fromScenarioRegistry()` classpath loading | `scenario/test-registry.yaml` | Modern format, classpath resolution |
| 4 | `ScenarioEnabledFieldTest` | apex-core | Enabled flag parsing | Temp-dir YAML with `enabled:` | **enabled flag**, ScenarioRegistryLoader direct |
| 5 | `ScenarioClasspathIntegrationTest` | apex-demo | Classpath-based loading | `classpath-integration/scenario-registry.yaml` | **Legacy aliases** (stage-id), classpath resolution |
| 6 | `SearchPathResolutionOrderTest` | apex-core | Filesystem vs classpath precedence | ScenarioRegistryLoader direct | File resolution |
| 7 | `EnvironmentConfigurationTest` | apex-core | System property/env var search paths | ScenarioRegistryLoader direct | Config resolution |
| 8 | `ComponentScenarioTest` | apex-demo | Component expansion, nested components | `ComponentScenarioTest-registry.yaml` | Components, modern format |
| 9 | `BasicStageConfigurationTest` | apex-demo | Stage-based processing | `BasicStageConfigurationTest-scenario.yaml` | Modern format: stage-metadata, depends-on, failure-policy, execution-order |
| 10 | `ComponentFailurePolicyTest` | apex-core | Component failure policies | `scenario/component-failure-policy-test-registry.yaml` | Components, failure-policy |
| 11 | `SimpleFailurePolicyTerminateTest` | apex-demo | Terminate failure policy | Co-located YAML | Failure-policy: terminate |
| 12 | `SimpleFailurePolicyContinueTest` | apex-demo | Continue failure policy | Co-located YAML | Failure-policy: continue-with-warnings |
| 13 | `SimpleFailurePolicyReviewTest` | apex-demo | Review failure policy | Co-located YAML | Failure-policy: flag-for-review |
| 14 | `SimpleFailurePolicyEnrichmentTest` | apex-demo | Enrichment failure handling | Co-located YAML | Enrichment stages |
| 15 | `SimpleFailurePolicyValidationTest` | apex-demo | Validation failure handling | Co-located YAML | Validation stages |
| 16 | `SimpleFailurePolicyComplianceTest` | apex-demo | Compliance failures | Co-located YAML | Compliance rules |
| 17 | `SimpleFailurePolicyConfigurationErrorTest` | apex-demo | Config error handling | Co-located YAML (3 variants) | Error paths |
| 18 | `ValidationFailureScenarioTest` | apex-demo | Validation rule failures | `ValidationFailureScenarioTest.yaml` | Negative testing |
| 19 | `ScenarioEndToEndIntegrationTest` | apex-demo | Complete E2E flow | Inline YAML in temp dir | Modern format, classification routing |
| 20 | `ScenarioEndToEndIntegrationComplexTest` | apex-demo | Multi-scenario complex E2E | Inline YAML | Classification routing between scenarios |

### 3.3 Tests Exercising Path C (ScenarioRegistryLoader + evaluateWithClassification)

| # | Test File | Module | What It Tests | YAML Files | Parsing Features Exercised |
|---|-----------|--------|---------------|------------|---------------------------|
| 1 | `RulesEngineClassificationRoutingTest` | apex-core | Classification routing: OTC, swaps, US, EMEA, high-notional | `RulesEngineClassificationRoutingTest-registry.yaml` + 5 scenario YAMLs | Map-form classification-rule, SpEL routing |
| 2 | `ScenarioEndToEndIntegrationTest` | apex-demo | E2E with classification | Inline YAML | Classification matching |
| 3 | `ScenarioEndToEndIntegrationComplexTest` | apex-demo | Multi-scenario classification | Inline YAML | Complex classification |
| 4 | `StageEnabledFlagTest` | apex-demo | Feature toggles via disabled stages | Inline YAML in temp dir | enabled/disabled stages |
| 5 | `ConditionalStageExecutionTest` (demo) | apex-demo | Region/value-based conditional staging | `ConditionalStageExecutionTest.yaml` | Conditional execution |

### 3.4 Unit Tests (No YAML Parsing — Test Domain Objects Directly)

| # | Test File | Module | What It Tests |
|---|-----------|--------|---------------|
| 1 | `ScenarioConfigurationStageTest` | apex-core | ScenarioConfiguration stage management, withStages(), backward compatibility |
| 2 | `ScenarioConfigurationClassificationTest` | apex-core | SpEL classification rule evaluation (single field, AND, numeric, null, missing) |
| 3 | `ScenarioConfigurationSpelValidationTest` | apex-core | Invalid SpEL syntax, undefined variables, type mismatches |
| 4 | `ScenarioAdvancedFeaturesTest` | apex-core | OR conditions, nested fields, string ops, flag-for-review |
| 5 | `ScenarioStageTest` | apex-core | ScenarioStage constructors, dependency, failure policy, metadata, utilities |
| 6 | `ScenarioStageCircularDependencyTest` | apex-core | Circular dependency detection (A→A, A→B→A, A→B→C→A) |
| 7 | `ScenarioStageEnabledFieldTest` | apex-core | Stage enabled flag defaults, setEnabled, equals/hashCode |
| 8 | `ScenarioStageMissingDependencyTest` | apex-core | Missing dependency handling in executor |
| 9 | `ScenarioStageExecutorFileHandlingTest` | apex-core | Missing config files, invalid paths |
| 10 | `ScenarioPerformanceMonitoringTest` | apex-core | Execution time tracking |
| 11 | `ScenarioSlaTimeoutEnforcementTest` | apex-core | Timeout enforcement |
| 12 | `ResultThreadSafetyTest` | apex-core | Concurrent addStageResult |
| 13 | `StageExecutionConcurrencyTest` | apex-core | setStageOutputs non-atomicity, shallow copy risks |

---

## 4. Coverage Assessment by Category

### (a) `ScenarioParser.parseFromYaml()` / `ScenarioParser.parseScenarioConfiguration()`
**Coverage: INDIRECT ONLY — No direct unit tests**

- `ScenarioParser` is exercised **only through** `ScenarioEvaluationManager.evaluateScenario(Map)`, which is itself called via `RulesEngine.fromFile("scenario.yaml").evaluateScenario(data)`.
- Tests that exercise this path: `ScenarioBasedRulesEngineTest`, `CleanEnrichmentDemoTest`, `ScenarioConcurrentAccessTest` (3 tests)
- **No test directly instantiates `ScenarioParser` or calls `parseFromYaml()`/`parseScenarioConfiguration()`.**
- **GAP**: If consolidation changes `ScenarioParser` behavior, failures would only surface as E2E scenario execution failures, not pinpointed parsing errors.

### (b) `ScenarioRegistryManager.parseScenarioConfiguration()` / `parseScenarioStage()`
**Coverage: DEAD CODE — Never called in production flow**

- `ScenarioRegistryManager` contains full parsing code but **its parsing methods are never invoked** in any execution path. The registry loading uses `ScenarioRegistryLoader`, and `ScenarioRegistryManager` is only used for lookup (`getScenario()`) and classification (`findMatchingScenario()`).
- **0 tests exercise the parsing methods in ScenarioRegistryManager.**
- The lookup/classification methods ARE tested via `evaluateWithClassification()` and `evaluateScenario(String, Map)` paths.
- **IMPLICATION**: These parsing methods can be safely removed during consolidation with zero risk.

### (c) `ScenarioRegistryLoader` parsing path
**Coverage: WELL COVERED (20+ tests)**

- This is the primary parsing path for all registry-based scenarios.
- Exercised by every `RulesEngine.fromScenarioRegistry()` call across tests (see Section 3.2 and 3.3).
- Direct `ScenarioRegistryLoader` instantiation in: `ScenarioEnabledFieldTest`, `ScenarioClasspathIntegrationTest`, `SearchPathResolutionOrderTest`, `EnvironmentConfigurationTest`.
- **This should be the canonical parser** during consolidation.

### (d) `ScenarioRegistryManager` lookup / classification-rule evaluation
**Coverage: GOOD (8+ tests)**

- `findMatchingScenario()` tested by: `RulesEngineClassificationRoutingTest` (5 scenarios), `ScenarioEndToEndIntegrationTest`, `ScenarioEndToEndIntegrationComplexTest`, `StageEnabledFlagTest`
- `getScenario()` tested by: `RulesEngineScenarioRegistryTest`, all `SimpleFailurePolicy*Test` tests (7 tests), `ComponentScenarioTest`, `ComponentFailurePolicyTest`
- `evaluateClassificationRule()` indirectly tested via all classification routing tests.

### (e) Full E2E scenario execution
**Coverage: EXCELLENT (15+ tests)**

- `ScenarioEndToEndIntegrationTest`, `ScenarioEndToEndIntegrationComplexTest`, `RulesEngineClassificationRoutingTest`, `BasicStageConfigurationTest`, `ComponentScenarioTest`, `ValidationFailureScenarioTest`, all 7 `SimpleFailurePolicy*Test` tests.

### (f) Legacy YAML format (`data-types`, `rule-configurations`, `stage-id`)
**Coverage: PARTIAL**

- `stage-id` alias tested by: `ScenarioClasspathIntegrationTest` (classpath-integration scenarios use `stage-id`)
- `rule-configuration` alias: **NOT tested by any YAML file** (grep found 0 matches)
- `data-types` + `rule-configurations`: Legacy format exists in `basic-validation-scenario.yaml` but no test loads this file via registry
- `ScenarioConfigurationStageTest` tests backward compat programmatically (not via YAML parsing)
- **GAP**: The `rule-configuration` legacy alias in `ScenarioRegistryLoader` is untested.

### (g) Modern YAML format (`processing-stages`, `stage-name`, `config-file`)
**Coverage: EXCELLENT**

- Used by nearly all integration/E2E tests. `BasicStageConfigurationTest`, `RulesEngineClassificationRoutingTest`, all failure policy tests, both E2E integration tests, `ComponentScenarioTest`.

### (h) Stage parsing details
| Feature | Tested? | By Which Test(s) |
|---------|---------|-------------------|
| `stage-name` | ✅ | All modern-format tests |
| `config-file` | ✅ | All modern-format tests |
| `execution-order` | ✅ | `BasicStageConfigurationTest` |
| `failure-policy` | ✅ | `BasicStageConfigurationTest`, all `SimpleFailurePolicy*` |
| `depends-on` | ✅ | `BasicStageConfigurationTest`, `ScenarioStageCircularDependencyTest` |
| `required` | ✅ | `BasicStageConfigurationTest` |
| `condition` | ✅ | `ConditionalStageExecutionTest` (demo) |
| `stage-metadata` | ⚠️ PARTIAL | Present in YAML but metadata map assertion unclear |
| `description` (from stage-metadata) | ⚠️ PARTIAL | Used in `BasicStageConfigurationTest` YAML but no explicit assertion |
| `enabled` (stage-level) | ✅ | `ScenarioStageEnabledFieldTest`, `StageEnabledFlagTest` |
| `stage-id` (legacy alias) | ✅ | `ScenarioClasspathIntegrationTest` |
| `rule-configuration` (legacy alias) | ❌ NOT TESTED | No YAML file uses this alias |

### (i) Edge cases and error handling
| Edge Case | Tested? | By Which Test(s) |
|-----------|---------|-------------------|
| Null input data | ✅ | `RulesEngineClassificationRoutingTest`, `RulesEngineScenarioRegistryTest` |
| Missing/null scenario ID | ✅ | `RulesEngineScenarioRegistryTest` |
| Empty registry | ✅ | `RulesEngineMalformedRegistryTest` |
| Missing `scenarios` section | ✅ | `RulesEngineMalformedRegistryTest` |
| Invalid YAML syntax | ✅ | `RulesEngineMalformedRegistryTest` |
| Non-existent file path | ✅ | `RulesEngineMalformedRegistryTest`, `ScenarioStageExecutorFileHandlingTest` |
| Missing required fields | ✅ | `RulesEngineMalformedRegistryTest` |
| Classification no-match | ✅ | `RulesEngineClassificationRoutingTest` |
| String-form classification-rule | ❌ NOT TESTED | No YAML file uses string form (all use Map) |
| Disabled scenario skipped | ✅ | `ScenarioEnabledFieldTest` |
| Metadata map parsing | ⚠️ PARTIAL | `ScenarioEnabledFieldTest` writes temp YAML with metadata but unclear assertion |
| Circular stage dependencies | ✅ | `ScenarioStageCircularDependencyTest` |
| Missing stage dependencies | ✅ | `ScenarioStageMissingDependencyTest` |
| Concurrent scenario execution | ✅ | `ScenarioConcurrentAccessTest`, `StageExecutionConcurrencyTest`, `ResultThreadSafetyTest` |
| Component nested in scenario | ✅ | `ComponentScenarioTest` |
| Component failure policies | ✅ | `ComponentFailurePolicyTest` |

---

## 5. Critical Gaps for Consolidation

### GAP 1: No Direct Unit Tests for `ScenarioParser`
**Risk: MEDIUM**

`ScenarioParser.parseFromYaml()` and `parseScenarioConfiguration()` have no direct unit tests. They are only tested indirectly through 3 integration tests. During consolidation, if you change how YAML maps are parsed into `ScenarioConfiguration` objects, subtle bugs (e.g., field mapping changes, null handling) would only show up as cryptic E2E failures.

**Recommendation**: Before consolidating, add direct unit tests for `ScenarioParser.parseScenarioConfiguration()` that validate each field mapping:
- `scenario-id`, `name`, `description`
- `classification-rule` (map form)
- `processing-stages` list → `ScenarioStage` list
- Each stage field: `stage-name`, `config-file`, `execution-order`, `failure-policy`, `depends-on`, `required`, `condition`

### GAP 2: `ScenarioRegistryManager` Parsing Is Dead Code
**Risk: NONE for removal — but confirms triplication is real**

The parsing methods in `ScenarioRegistryManager` are never called. They duplicate `ScenarioParser` exactly. Safe to delete during consolidation.

### GAP 3: String-Form `classification-rule` Is Untested
**Risk: LOW (feature may be unused)**

`ScenarioRegistryLoader` supports `classification-rule: "#tradeType == 'OTCOption'"` (string form) in addition to the map form. No test YAML anywhere uses the string form. All tests use the map form:
```yaml
classification-rule:
  condition: "#tradeType == 'OTCOption'"
  description: "Matches OTC option trades"
```

**Recommendation**: Either (a) add a test for string-form, or (b) verify no production YAML uses it and remove the code path.

### GAP 4: `rule-configuration` Legacy Alias Is Untested
**Risk: LOW**

`ScenarioRegistryLoader` supports `rule-configuration` as an alias for `config-file`. No YAML file in the entire test suite uses this alias (confirmed via grep). The `stage-id` alias IS tested (via `ScenarioClasspathIntegrationTest`).

**Recommendation**: Add a test or deprecate/remove the alias.

### GAP 5: `stage-metadata.description` Extraction Is Weakly Tested  
**Risk: LOW**

`ScenarioRegistryLoader.parseScenarioStage()` extracts `description` from nested `stage-metadata`. While `BasicStageConfigurationTest` YAML files include `stage-metadata.description`, there's no explicit assertion that the parsed `ScenarioStage.getDescription()` matches.

**Recommendation**: Add an explicit assertion in `ScenarioClasspathIntegrationTest` or `BasicStageConfigurationTest`.

### GAP 6: `metadata` Map Parsing at Scenario Level  
**Risk: LOW**

`ScenarioRegistryLoader` parses a `metadata` map at the scenario level. `ScenarioEnabledFieldTest` writes temp YAML with metadata but the focus is on the `enabled` field. No test asserts the parsed metadata map's contents.

**Recommendation**: Add assertion for `scenario.getMetadata()` in existing tests.

---

## 6. Consolidation Strategy Recommendation

### Recommended Target: Promote `ScenarioRegistryLoader` parsing as canonical

1. **Keep**: `ScenarioRegistryLoader.parseScenarioConfiguration()` and `parseScenarioStage()` — they are the most complete
2. **Extract**: Move parsing logic to a shared utility class (e.g., `ScenarioConfigurationParser`)
3. **Replace**: `ScenarioParser.parseFromYaml()` should delegate to the shared parser
4. **Delete**: `ScenarioRegistryManager.parseScenarioConfiguration()` and `parseScenarioStage()` (dead code)

### Features Only in ScenarioRegistryLoader That Must Be Preserved

| Feature | In ScenarioRegistryLoader | In ScenarioParser | In ScenarioRegistryManager |
|---------|--------------------------|-------------------|---------------------------|
| String-form classification-rule | ✅ | ❌ | ❌ |
| Map-form classification-rule | ✅ | ✅ | ✅ |
| `enabled` flag | ✅ | ❌ | ❌ |
| `metadata` map | ✅ | ❌ | ❌ |
| `stage-id` alias | ✅ | ❌ | ❌ |
| `rule-configuration` alias | ✅ | ❌ | ❌ |
| Component file detection | ✅ | ❌ | ❌ |
| `description` from stage-metadata | ✅ | ❌ | ❌ |
| `withStages()` factory usage | ❌ (uses setProcessingStages) | ✅ | ✅ |

### Test Safety Net Before Consolidation

The following tests form the **minimum regression suite** that must pass before and after consolidation:

**Critical Path A tests** (exercises ScenarioParser):
1. `ScenarioBasedRulesEngineTest`
2. `CleanEnrichmentDemoTest`
3. `ScenarioConcurrentAccessTest`

**Critical Path B tests** (exercises ScenarioRegistryLoader):
4. `RulesEngineScenarioRegistryTest`
5. `RulesEngineMalformedRegistryTest`
6. `ScenarioEnabledFieldTest`
7. `ScenarioClasspathIntegrationTest`
8. `RulesEngineFromScenarioRegistryTest`
9. `BasicStageConfigurationTest`
10. `ComponentScenarioTest`

**Critical Path C tests** (exercises classification):
11. `RulesEngineClassificationRoutingTest`
12. `ScenarioEndToEndIntegrationTest`
13. `ScenarioEndToEndIntegrationComplexTest`

**Critical failure-policy tests**:
14-20. All 7 `SimpleFailurePolicy*Test` tests

**Critical infrastructure tests**:
21. `SearchPathResolutionOrderTest`
22. `EnvironmentConfigurationTest`

**Total minimum regression suite: 22 tests (42 test files if including all unit tests)**

---

## 7. Summary Statistics

| Metric | Count |
|--------|-------|
| Total scenario-related test files identified | 42 |
| Tests exercising ScenarioRegistryLoader (Path B/C) | 25+ |
| Tests exercising ScenarioParser (Path A) | 3 |
| Tests exercising ScenarioRegistryManager parsing | **0** (dead code) |
| Tests exercising ScenarioRegistryManager lookup | 15+ |
| Direct `ScenarioRegistryLoader` instantiation in tests | 4 |
| Direct `ScenarioParser` instantiation in tests | **0** |
| Direct `ScenarioRegistryManager` instantiation in tests | **0** |
| YAML files using modern format (processing-stages) | 80+ |
| YAML files using legacy format (stage-id alias) | 4 (classpath-integration only) |
| YAML files using string-form classification-rule | **0** |
| YAML files using `rule-configuration` alias | **0** |
| Critical coverage gaps | 6 (detailed in Section 5) |
