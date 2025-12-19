# YAML Import Test Coverage Analysis

**Date:** 2025-12-19
**Version:** 1.0
**Purpose:** Calculate comprehensive test coverage for APEX Visual Editor YAML import validation

## Executive Summary

Based on analysis of **APEX_BLOCKS_PROTOTYPE_GUIDE.md** (50+ blocks in 12 categories) and **APEX_YAML_REFERENCE.md** (~155 keywords, ~140 functionally implemented), we need a systematic approach to test all possible YAML configuration combinations.

**Current Coverage:** 6 basic test samples (12 total tests with round-trip)  
**Required Coverage:** **87 comprehensive test samples** covering all block types and combinations  
**Gap:** 81 additional test samples needed for complete coverage

---

## Block Inventory

### Total Blocks: 52 Unique Block Types

| Category | Block Types | Count |
|----------|-------------|-------|
| **1. Configuration** | Configuration, Data Source Config, Scenario Config, Scenario Registry, Component Config | 5 |
| **2. Scenarios** | Scenario, Classification Rule, Processing Stage, Scenario Reference | 4 |
| **3. Components** | File Reference, Data Source Reference | 2 |
| **4. Sections** | Data Source Refs, Enrichment Groups, Enrichments, Rule Groups, Rules, Transformations, Error Recovery | 7 |
| **5. Error Recovery** | Error Recovery, Severity Policy | 2 |
| **6. Data Sources** | Database Source, REST API Source, File Source, Query, Endpoint, Data Source Reference | 6 |
| **7. Rules** | Rule, Rule Group, Rule Reference | 3 |
| **8. Transformations** | Conditional Transformation, Set Field | 2 |
| **9. Enrichments** | Enrichment Group, Enrichment Reference, Calculation Enrichment, Field Enrichment, Field Mapping | 5 |
| **10. Lookups** | Lookup Enrichment, Reference Dataset, Database Dataset, Lookup Parameter | 4 |
| **11. Logic** | Comparison, Logic, Field Reference, Text, Number, Boolean | 6 |
| **12. Templates** | (Dynamic, loaded at runtime) | Variable |

---

## YAML Structure Combinations

### 1. Configuration Root Types (5 variants)

Each configuration type supports different child structures:

1. **Rule Configuration** (`apex_rule_config`)
   - Can contain: Rules, Rule Groups, Enrichments, Enrichment Groups, Transformations, Error Recovery, Data Source Refs
   
2. **Data Source Configuration** (`apex_data_source_config`)
   - Can contain: Database Sources, REST API Sources, File Sources
   
3. **Scenario Configuration** (`apex_scenario_config`)
   - Can contain: Scenario with Classification/Processing Stages
   
4. **Scenario Registry** (`apex_scenario_registry`)
   - Can contain: Scenario References
   
5. **Component Configuration** (`apex_component_config`)
   - Can contain: File References, Component Refs

### 2. Section Combinations (7 section types)

Each section can contain different child blocks:

- **Data Source Refs Section**: Data Source Reference blocks
- **Enrichment Groups Section**: Enrichment Group blocks
- **Enrichments Section**: Enrichment blocks (Calculation, Field, Lookup)
- **Rule Groups Section**: Rule Group blocks
- **Rules Section**: Rule blocks
- **Transformations Section**: Transformation blocks
- **Error Recovery Section**: Severity Policy blocks

### 3. Enrichment Type Variations (5 types)

1. **Lookup Enrichment**
   - With Reference Dataset
   - With Database Dataset (requires parameters)
   - With YAML data source
   - With multiple result mappings (2-10 mappings)
   
2. **Calculation Enrichment**
   - Simple arithmetic (#quantity * #price)
   - Complex expressions (conditional, date math, string manipulation)
   - Multiple source fields (2-5 fields)
   
3. **Field Enrichment**
   - With conditional transformations
   - With multiple field mappings
   
4. **Conditional Mapping**
   - If-then-else mappings
   - Multiple conditions
   
5. **Enrichment Groups**
   - AND operator
   - OR operator
   - Nested groups

### 4. Rule Type Variations (3 structures + variations)

1. **Standalone Rules**
   - ERROR severity
   - WARNING severity
   - INFO severity
   - With/without conditions
   - With/without priorities
   - With/without categories
   
2. **Rule Groups**
   - AND operator
   - OR operator
   - With rule-ids list
   - With rule-references (detailed)
   - Nested rule groups
   - With stop-on-first-failure
   - With parallel-execution
   - With error-handling (fail-fast, continue-on-error, skip-on-error)
   
3. **Rule Chains** (6 patterns)
   - Conditional chaining
   - Sequential dependency
   - Result-based routing
   - Accumulative chaining
   - Complex workflow
   - Fluent builder

### 5. Scenario Variations (3 approaches)

1. **Classification-Based Routing**
   - Single field classification
   - Multiple classifications (3-10 entries)
   - With default scenario
   
2. **Stage-Based Processing**
   - Sequential stages (2-5 stages)
   - With dependencies
   - With conditional execution
   - Different failure policies (terminate, continue-with-warnings, flag-for-review)
   
3. **Scenario Registry**
   - Type-based routing
   - Classification-based routing
   - Multiple scenario references

### 6. Data Source Variations (3 types × variations)

1. **Database Sources**
   - PostgreSQL
   - H2
   - With named queries
   - With parameterized queries
   - With connection pooling
   
2. **REST API Sources**
   - With authentication
   - Multiple endpoints
   - With response mapping
   - With circuit breaker
   
3. **File Sources**
   - CSV format
   - JSON format
   - XML format
   - With polling interval

### 7. Error Recovery Variations

1. **Global Error Recovery**
   - Different default strategies (4 options)
   - Multiple severity policies (2-4 policies)
   
2. **Rule Group Error Handling**
   - fail-fast
   - continue-on-error
   - skip-on-error
   
3. **Enrichment Group Error Handling**
   - fail-fast
   - continue-on-error
   - skip-on-error

### 8. Component Variations

1. **File References**
   - rule-configurations
   - enrichment-refs
   - component-refs
   - config-files
   - With execution order
   - With different failure policies
   
2. **Nested Components**
   - 2-level nesting
   - 3-level nesting
   - Multiple file references per component

### 9. Complex Expression Variations

1. **Conditions**
   - Simple comparisons (==, !=, <, >, <=, >=)
   - Logical operators (AND, OR, NOT)
   - Null checks (#field != null)
   - Collection operations (#list.contains())
   - Date comparisons
   - String operations (matches, contains, startsWith)
   
2. **Calculations**
   - Arithmetic (+, -, *, /, %)
   - Date math (plusDays, minusMonths)
   - String concatenation
   - Ternary operators
   - Function calls (Math.round, String.format)

---

## Test Coverage Matrix

### Phase 1: Individual Block Types (35 tests)

Test each block type in isolation:

| Test # | Block Type | Description | File Path |
|--------|------------|-------------|-----------|
| 1 | Rule (ERROR) | Single ERROR rule | examples/validation/rule-error-test.yaml |
| 2 | Rule (WARNING) | Single WARNING rule | examples/validation/rule-warning-test.yaml |
| 3 | Rule (INFO) | Single INFO rule | examples/validation/rule-info-test.yaml |
| 4 | Rule Group (AND) | AND rule group | examples/rulegroups/rulegroup-and-test.yaml |
| 5 | Rule Group (OR) | OR rule group | examples/rulegroups/rulegroup-or-test.yaml |
| 6 | Rule Group (error-handling) | With error-handling | examples/rulegroups/rulegroup-errorhandling-test.yaml |
| 7 | Enrichment Group (AND) | AND enrichment group | examples/enrichment/enrichmentgroup-and-test.yaml |
| 8 | Enrichment Group (OR) | OR enrichment group | examples/enrichment/enrichmentgroup-or-test.yaml |
| 9 | Lookup (Reference Dataset) | Reference dataset lookup | examples/lookup/lookup-reference-test.yaml |
| 10 | Lookup (Database Dataset) | Database dataset lookup | examples/lookup/lookup-database-test.yaml |
| 11 | Lookup (Multiple Mappings) | 5+ result mappings | examples/lookup/lookup-multimapping-test.yaml |
| 12 | Calculation (Simple) | Basic arithmetic | examples/enrichment/calculation-simple-test.yaml |
| 13 | Calculation (Complex) | Complex SpEL expression | examples/enrichment/calculation-complex-test.yaml |
| 14 | Field Enrichment | Field transformations | examples/enrichment/field-enrichment-test.yaml |
| 15 | Conditional Mapping | If-then-else mapping | examples/enrichment/conditional-mapping-test.yaml |
| 16 | Conditional Transformation | True/false actions | examples/transformation/conditional-transform-test.yaml |
| 17 | Set Field | Simple field setter | examples/transformation/set-field-test.yaml |
| 18 | Scenario (Classification) | Classification routing | examples/conditional/scenario-classification-test.yaml (exists) |
| 19 | Scenario (Stages) | Stage-based processing | examples/conditional/scenario-stages-test.yaml |
| 20 | Scenario (Dependencies) | Stage dependencies | examples/conditional/scenario-dependencies-test.yaml |
| 21 | Scenario Registry (Type) | Type-based routing | examples/conditional/registry-type-test.yaml |
| 22 | Scenario Registry (Class) | Classification routing | examples/conditional/registry-classification-test.yaml |
| 23 | Component (Simple) | Single file reference | examples/basic/component-simple-test.yaml |
| 24 | Component (Multi-file) | Multiple file references | examples/basic/component-multifile-test.yaml |
| 25 | Component (Nested) | Nested components | examples/basic/component-nested-test.yaml |
| 26 | Data Source (Database) | PostgreSQL source | examples/lookup/datasource-database-test.yaml |
| 27 | Data Source (REST API) | REST API source | examples/lookup/datasource-rest-test.yaml |
| 28 | Data Source (File CSV) | CSV file source | examples/lookup/datasource-csv-test.yaml |
| 29 | Data Source (File JSON) | JSON file source | examples/lookup/datasource-json-test.yaml |
| 30 | Error Recovery (Global) | Global error recovery | examples/validation/error-recovery-test.yaml (exists) |
| 31 | Severity Policy (ERROR) | ERROR policy | examples/validation/severity-error-test.yaml |
| 32 | Severity Policy (WARNING) | WARNING policy | examples/validation/severity-warning-test.yaml |
| 33 | Severity Policy (Multiple) | Multiple policies | examples/validation/severity-multiple-test.yaml |
| 34 | Data Source Reference | External data source ref | examples/basic/component-datasource-test.yaml (exists) |
| 35 | File Reference | External file ref | examples/basic/file-reference-test.yaml |

### Phase 2: Section Combinations (14 tests)

Test configuration with different section combinations:

| Test # | Sections Combined | Description | File Path |
|--------|-------------------|-------------|-----------|
| 36 | Rules + Enrichments | Both rules and enrichments | examples/validation/rules-enrichments-test.yaml |
| 37 | Rules + Rule Groups | Mix of rules and groups | examples/rulegroups/rules-groups-test.yaml |
| 38 | Enrichments + Lookups | Enrichments with lookups | examples/enrichment/enrichments-lookups-test.yaml |
| 39 | Rules + Transformations | Rules with transformations | examples/transformation/rules-transforms-test.yaml |
| 40 | Rules + Error Recovery | Rules with error recovery | examples/validation/rules-errorrecovery-test.yaml |
| 41 | Rules + Data Source Refs | Rules with external data | examples/validation/rules-datasourcerefs-test.yaml |
| 42 | Enrichments + Error Recovery | Enrichments with error recovery | examples/enrichment/enrichments-errorrecovery-test.yaml |
| 43 | All Sections (Full Config) | Complete configuration | examples/validation/full-configuration-test.yaml |
| 44 | Enrichment Groups + Enrichments | Mixed groups and standalone | examples/enrichment/groups-standalone-test.yaml |
| 45 | Rule Groups + Rules | Mixed groups and standalone | examples/rulegroups/groups-standalone-test.yaml |
| 46 | Nested Rule Groups | Rule groups within groups | examples/rulegroups/nested-groups-test.yaml |
| 47 | Nested Enrichment Groups | Enrichment groups within groups | examples/enrichment/nested-groups-test.yaml |
| 48 | Multiple Data Source Refs | 3+ data source references | examples/lookup/multiple-datasourcerefs-test.yaml |
| 49 | Transformations + Enrichments | Transforms with enrichments | examples/transformation/transforms-enrichments-test.yaml |

### Phase 3: Complex Scenarios (18 tests)

Test advanced patterns and edge cases:

| Test # | Pattern | Description | File Path |
|--------|---------|-------------|-----------|
| 50 | Scenario (Multi-stage) | 5+ processing stages | examples/conditional/scenario-multistage-test.yaml |
| 51 | Scenario (Conditional Stages) | Stages with conditions | examples/conditional/scenario-conditional-test.yaml |
| 52 | Scenario (Failure Policies) | Different failure policies | examples/conditional/scenario-failurepolicies-test.yaml |
| 53 | Lookup (Chained) | Multiple sequential lookups | examples/lookup/lookup-chained-test.yaml |
| 54 | Lookup (Parallel) | Multiple parallel lookups | examples/lookup/lookup-parallel-test.yaml |
| 55 | Calculation (Date Math) | Date calculations | examples/enrichment/calculation-datemath-test.yaml |
| 56 | Calculation (String Ops) | String operations | examples/enrichment/calculation-stringops-test.yaml |
| 57 | Conditional (Nested) | Nested conditionals | examples/transformation/conditional-nested-test.yaml |
| 58 | Conditional (Ternary) | Ternary operators | examples/transformation/conditional-ternary-test.yaml |
| 59 | Rule (Priority Ordering) | Rules with priorities | examples/validation/rules-priorities-test.yaml |
| 60 | Rule (Categories) | Rules with categories | examples/validation/rules-categories-test.yaml |
| 61 | Rule (Custom Properties) | Rules with custom props | examples/validation/rules-customprops-test.yaml |
| 62 | Enrichment (Parallel Execution) | Parallel enrichments | examples/enrichment/enrichment-parallel-test.yaml |
| 63 | Component (Execution Order) | Ordered file execution | examples/basic/component-executionorder-test.yaml |
| 64 | Component (Failure Policies) | Different failure policies | examples/basic/component-failurepolicies-test.yaml |
| 65 | Error Recovery (Retry Logic) | Retry with delays | examples/validation/errorrecovery-retry-test.yaml |
| 66 | Error Recovery (Safe Expression) | Safe expression fallback | examples/validation/errorrecovery-safeexpr-test.yaml |
| 67 | Data Source (Connection Pool) | With connection pooling | examples/lookup/datasource-connpool-test.yaml |

### Phase 4: Rule Chains (6 tests)

Test all 6 rule chain patterns (NOT currently supported in visual editor):

| Test # | Chain Pattern | Description | File Path |
|--------|---------------|-------------|-----------|
| 68 | Conditional Chaining | If-then chain | examples/rulegroups/chain-conditional-test.yaml |
| 69 | Sequential Dependency | Ordered execution | examples/rulegroups/chain-sequential-test.yaml |
| 70 | Result-Based Routing | Route by result | examples/rulegroups/chain-routing-test.yaml |
| 71 | Accumulative Chaining | Accumulate results | examples/rulegroups/chain-accumulative-test.yaml |
| 72 | Complex Workflow | Multi-path workflow | examples/rulegroups/chain-workflow-test.yaml |
| 73 | Fluent Builder | Builder pattern | examples/rulegroups/chain-fluent-test.yaml |

### Phase 5: Edge Cases & Stress Tests (14 tests)

Test boundary conditions and large configurations:

| Test # | Edge Case | Description | File Path |
|--------|-----------|-------------|-----------|
| 74 | Empty Metadata | Minimal metadata | examples/validation/edge-emptymeta-test.yaml |
| 75 | Maximum Metadata | All metadata fields | examples/validation/edge-maxmeta-test.yaml |
| 76 | Large Rule Count | 50+ rules | examples/validation/edge-manyrules-test.yaml |
| 77 | Large Enrichment Count | 50+ enrichments | examples/enrichment/edge-manyenrichments-test.yaml |
| 78 | Deep Nesting (3 levels) | Deeply nested structures | examples/basic/edge-deepnesting-test.yaml |
| 79 | Long SpEL Expressions | Complex 200+ char expressions | examples/enrichment/edge-longspel-test.yaml |
| 80 | Special Characters | Quotes, colons, escapes | examples/validation/edge-specialchars-test.yaml |
| 81 | Unicode Characters | UTF-8 content | examples/validation/edge-unicode-test.yaml |
| 82 | Multiple Classifications | 10+ classification entries | examples/conditional/edge-manyclasses-test.yaml |
| 83 | Multiple Result Mappings | 20+ mappings | examples/lookup/edge-manymappings-test.yaml |
| 84 | Multiple Severity Policies | All 4 severity levels | examples/validation/edge-allseverities-test.yaml |
| 85 | Null Field Handling | Null checks and safety | examples/validation/edge-nullhandling-test.yaml |
| 86 | Collection Operations | List/Map operations | examples/enrichment/edge-collections-test.yaml |
| 87 | Bootstrap Pattern | Complete OTC workflow | examples/etl/bootstrap-otc-test.yaml |

---

## Implementation Priority

### High Priority (Must Have - 20 tests)

Complete coverage of core functionality:

- Tests 1-6: All rule severities and group operators
- Tests 9-13: All lookup and calculation types
- Tests 18-20: Scenario variations
- Tests 26-29: All data source types
- Tests 36-43: Section combinations
- Tests 50-52: Complex scenarios

### Medium Priority (Should Have - 35 tests)

Extended functionality and patterns:

- Tests 7-8, 14-17: Remaining enrichment/transformation types
- Tests 21-25: Component variations
- Tests 30-35: Error recovery configurations
- Tests 44-49: Mixed configurations
- Tests 53-67: Advanced patterns

### Low Priority (Nice to Have - 32 tests)

Edge cases and stress tests:

- Tests 68-73: Rule chains (not supported in visual editor yet)
- Tests 74-87: Edge cases and boundary conditions

---

## Round-Trip Test Multiplication

**Each test requires 2 test methods:**
1. Import test (verify blocks created correctly)
2. Round-trip test (import → export → verify semantic equivalence)

**Total Test Methods Required:** 87 × 2 = **174 test methods**

---

## Recommended Test Organization

### File Structure

```
apex-playground/
  src/test/java/dev/mars/apex/playground/ui/
    YamlImportValidationUITest.java (current - 12 tests)
    YamlImportPhase1BlockTypesTest.java (tests 1-35, 70 methods)
    YamlImportPhase2CombinationsTest.java (tests 36-49, 28 methods)
    YamlImportPhase3ComplexTest.java (tests 50-67, 36 methods)
    YamlImportPhase4ChainsTest.java (tests 68-73, 12 methods)
    YamlImportPhase5EdgeCasesTest.java (tests 74-87, 28 methods)
```

### Test Class Distribution

- **YamlImportValidationUITest**: Current 6 samples (12 methods) - COMPLETE
- **YamlImportPhase1BlockTypesTest**: 35 samples × 2 = 70 methods
- **YamlImportPhase2CombinationsTest**: 14 samples × 2 = 28 methods
- **YamlImportPhase3ComplexTest**: 18 samples × 2 = 36 methods
- **YamlImportPhase4ChainsTest**: 6 samples × 2 = 12 methods
- **YamlImportPhase5EdgeCasesTest**: 14 samples × 2 = 28 methods

**Total:** 87 samples, 174 test methods across 6 test classes

---

## Estimated Effort

### Test YAML File Creation
- Simple tests (1-20): ~15 min each = 5 hours
- Medium tests (21-60): ~30 min each = 20 hours
- Complex tests (61-87): ~45 min each = 20 hours
- **Total:** ~45 hours

### Test Method Implementation
- Setup infrastructure (base class, helpers): 4 hours
- Import tests (~5 min each × 87): 7 hours
- Round-trip tests (~5 min each × 87): 7 hours
- **Total:** ~18 hours

### Documentation & Validation
- Update APEX_BLOCKS_PROTOTYPE_GUIDE.md: 2 hours
- Create test execution scripts: 2 hours
- Initial validation runs: 4 hours
- **Total:** ~8 hours

**Grand Total:** ~71 hours of development effort

---

## Success Criteria

1. ✅ **Block Coverage**: All 52 block types tested
2. ✅ **Combination Coverage**: All valid section combinations tested
3. ✅ **Pattern Coverage**: All APEX patterns (chains, scenarios, components) tested
4. ✅ **Edge Case Coverage**: Boundary conditions and stress tests included
5. ✅ **Round-Trip Validation**: 100% semantic preservation verified
6. ✅ **Documentation**: All test samples documented in guide

---

## Current Status

**Completed:** 6/87 samples (7% complete)  
**Remaining:** 81 samples (93% remaining)

### Immediate Next Steps

1. Create Phase 1 test samples (tests 1-35) - focuses on individual block types
2. Implement YamlImportPhase1BlockTypesTest.java (70 test methods)
3. Validate Phase 1 execution
4. Proceed to Phase 2 (combination testing)

---

## Implementation Plan

### Overview

This plan outlines a phased approach to achieving 100% YAML import test coverage over **4 sprints (8 weeks)** with **2 developers** working in parallel on test creation and validation.

### Team Structure

**Primary Developer (Dev A):**
- YAML sample creation
- Test method implementation
- Documentation updates

**Secondary Developer (Dev B):**
- Test execution and validation
- Infrastructure setup
- CI/CD integration

**Shared Responsibilities:**
- Code reviews
- Bug fixes
- Performance optimization

---

### Sprint 1: Foundation & Phase 1 (Weeks 1-2)

**Objectives:**
- Setup test infrastructure
- Complete Phase 1 block type tests
- Establish baseline coverage

#### Week 1: Selenium Test Infrastructure Setup (16 hours)

**Dev A Tasks: Selenium Base Test Framework (12 hours)**

- [ ] **Selenium WebDriver Configuration (3 hours)**
  - Install and configure ChromeDriver (compatible with Chrome version)
  - Setup `WebDriver` initialization in `@BeforeEach`
  - Configure `ChromeOptions` for headless mode (`--headless`, `--no-sandbox`, `--disable-dev-shm-usage`)
  - Set browser window size (`--window-size=1920,1080`)
  - Implement `WebDriver` cleanup in `@AfterEach`
  - Add WebDriverManager for automatic driver version management

- [ ] **Create BaseYamlImportSeleniumTest Abstract Class (4 hours)**
  ```java
  abstract class BaseYamlImportSeleniumTest {
      protected WebDriver driver;
      protected WebDriverWait wait;
      protected JavascriptExecutor js;
      protected String baseUrl;
  }
  ```
  - Initialize `WebDriverWait` with configurable timeout (default 15 seconds)
  - Initialize `JavascriptExecutor` for Blockly workspace interaction
  - Setup `@LocalServerPort` injection for Spring Boot test
  - Implement `waitForBlocklyWorkspaceToLoad()` using JavaScript polling

- [ ] **Selenium Helper Methods (3 hours)**
  - `importYamlContent(String yaml)` - Selenium implementation:
    - Click "Import YAML" button using `ExpectedConditions.elementToBeClickable()`
    - Wait for modal visibility using `ExpectedConditions.visibilityOfElementLocated()`
    - Enter YAML into textarea with `sendKeys()`
    - Click import and wait for modal invisibility
  - `exportYamlContent()` - JavaScript execution to get generated YAML
  - `waitForBlocksToRender()` - Explicit wait with configurable delay
  - `verifyBlockExists(String blockType, int count)` - JavaScript execution
  - `getBlockFieldValue(String blockType, String fieldName)` - JavaScript execution
  - `getBlockCount()` - JavaScript query of Blockly workspace
  - `verifySeverityValues(List<String>)` - Iterate blocks via JavaScript
  - `countNestedBlocks(String parentType, String inputName)` - JavaScript traversal

- [ ] **YAML Test Data Framework (2 hours)**
  - `loadYamlFile(String relativePath)` using `Files.readString()`
  - Path resolution for `examples/` directory
  - Fallback to project root if not found
  - YAML validation before loading
  - Caching mechanism for repeated loads

**Dev B Tasks: Selenium CI/CD & Monitoring (12 hours)**

- [ ] **Selenium CI/CD Pipeline Configuration (4 hours)**
  - Maven Surefire plugin configuration for Selenium tests
  - Chrome/ChromeDriver installation in CI environment (GitHub Actions/Jenkins)
  - Headless Chrome configuration for CI (`Xvfb` if needed on Linux)
  - Environment variable configuration (`SELENIUM_HEADLESS=true`)
  - Test execution profiles (`mvn test -P selenium-tests`)
  - Parallel test execution configuration (careful with browser instances)

- [ ] **Selenium Test Execution Scripts (3 hours)**
  - `run-selenium-import-tests.bat`:
    ```batch
    @echo off
    cd apex-playground
    mvn test -Dtest=YamlImport*Test -Dselenium.headless=true
    ```
  - `run-selenium-import-tests.sh`:
    ```bash
    #!/bin/bash
    cd apex-playground
    mvn test -Dtest=YamlImport*Test -Dselenium.headless=true
    ```
  - `run-phase-tests.bat [phase-number]`:
    ```batch
    mvn test -Dtest=YamlImportPhase%1*Test
    ```
  - Test result aggregation and HTML report generation

- [ ] **Selenium Test Monitoring & Debugging (3 hours)**
  - Screenshot capture on test failure using `TakesScreenshot`
  - Save screenshots to `target/selenium-screenshots/[test-name]-[timestamp].png`
  - Selenium execution time tracking per test
  - Browser console log capture on failure
  - Network request logging (if applicable)
  - Selenium Grid readiness (for future distributed testing)

- [ ] **Selenium Stability Enhancements (2 hours)**
  - Implement retry logic for flaky Selenium tests using `@RepeatedTest`
  - Configure implicit wait fallback (discouraged, prefer explicit waits)
  - Add StaleElementReferenceException handling
  - Add ElementNotInteractableException handling
  - Implement custom `ExpectedCondition` for Blockly block count

**Shared Tasks (4 hours):**
- [ ] Selenium environment validation (1 hour)
  - Verify ChromeDriver compatibility
  - Test headless vs. headed mode
  - Validate JavaScript execution in Blockly context
- [ ] Create Selenium test documentation (2 hours)
  - Selenium setup guide
  - Troubleshooting common Selenium issues
  - Chrome version compatibility matrix
- [ ] Code review of Selenium infrastructure (1 hour)

**Deliverables:**
- Selenium WebDriver infrastructure fully configured
- BaseYamlImportSeleniumTest with all helper methods
- Selenium CI/CD pipeline operational
- Screenshot capture and logging enabled
- Test execution scripts for Windows/Linux

#### Week 2: Phase 1 Implementation (40 hours)

**Dev A Tasks (Tests 1-18, 36 hours):**
- [ ] Create YAML samples 1-18 (18 hours, ~1 hour each)
  - Tests 1-3: Rule severities
  - Tests 4-6: Rule groups
  - Tests 7-8: Enrichment groups
  - Tests 9-11: Lookup variations
  - Tests 12-15: Calculation and field enrichments
  - Tests 16-17: Transformations
  - Test 18: Scenario classification
- [ ] Implement `YamlImportPhase1BlockTypesTest.java` (Part 1) (18 hours)
  - 18 import test methods
  - 18 round-trip test methods
  - Total: 36 methods

**Dev B Tasks (Tests 19-35, 34 hours):**
- [ ] Create YAML samples 19-35 (17 hours, ~1 hour each)
  - Tests 19-22: Scenario/registry variations
  - Tests 23-25: Component variations
  - Tests 26-29: Data source types
  - Tests 30-35: Error recovery configurations
- [ ] Implement `YamlImportPhase1BlockTypesTest.java` (Part 2) (17 hours)
  - 17 import test methods
  - 17 round-trip test methods
  - Total: 34 methods

**Shared Tasks (4 hours):**
- [ ] Code review and integration (2 hours)
- [ ] Execute full Phase 1 suite (1 hour)
- [ ] Fix failures and document issues (1 hour)

**Deliverables:**
- 35 YAML test samples (Tests 1-35)
- 70 test methods implemented
- Phase 1 test suite passing

**Sprint 1 Milestones:**
- ✅ Test infrastructure operational
- ✅ 40% of samples completed (35/87)
- ✅ 40% of test methods implemented (70/174)

---

### Sprint 2: Phase 2 & Phase 3 (Weeks 3-4)

**Objectives:**
- Complete section combination testing
- Implement complex scenario tests
- Reach 75% coverage

#### Week 3: Phase 2 Implementation (28 hours)

**Dev A Tasks (Tests 36-42, 14 hours):**
- [ ] Create YAML samples 36-42 (7 hours, ~1 hour each)
  - Test 36: Rules + Enrichments
  - Test 37: Rules + Rule Groups
  - Test 38: Enrichments + Lookups
  - Test 39: Rules + Transformations
  - Test 40: Rules + Error Recovery
  - Test 41: Rules + Data Source Refs
  - Test 42: Enrichments + Error Recovery
- [ ] Implement `YamlImportPhase2CombinationsTest.java` (Part 1) (7 hours)
  - 7 import test methods
  - 7 round-trip test methods
  - Total: 14 methods

**Dev B Tasks (Tests 43-49, 14 hours):**
- [ ] Create YAML samples 43-49 (7 hours, ~1 hour each)
  - Test 43: Full configuration (all sections)
  - Test 44: Enrichment groups + standalone
  - Test 45: Rule groups + standalone
  - Test 46: Nested rule groups
  - Test 47: Nested enrichment groups
  - Test 48: Multiple data source refs
  - Test 49: Transforms + enrichments
- [ ] Implement `YamlImportPhase2CombinationsTest.java` (Part 2) (7 hours)
  - 7 import test methods
  - 7 round-trip test methods
  - Total: 14 methods

**Shared Tasks (2 hours):**
- [ ] Code review and integration (1 hour)
- [ ] Execute Phase 2 suite (30 min)
- [ ] Fix failures (30 min)

**Deliverables:**
- 14 YAML test samples (Tests 36-49)
- 28 test methods implemented
- Phase 2 test suite passing

#### Week 4: Phase 3 Implementation (36 hours)

**Dev A Tasks (Tests 50-58, 18 hours):**
- [ ] Create YAML samples 50-58 (9 hours, ~1 hour each)
  - Tests 50-52: Multi-stage scenarios
  - Tests 53-54: Chained/parallel lookups
  - Tests 55-56: Date math and string operations
  - Tests 57-58: Nested conditionals
- [ ] Implement `YamlImportPhase3ComplexTest.java` (Part 1) (9 hours)
  - 9 import test methods
  - 9 round-trip test methods
  - Total: 18 methods

**Dev B Tasks (Tests 59-67, 18 hours):**
- [ ] Create YAML samples 59-67 (9 hours, ~1 hour each)
  - Tests 59-62: Rule priorities, categories, custom props, parallel
  - Tests 63-64: Component execution order and policies
  - Tests 65-67: Error recovery advanced patterns
- [ ] Implement `YamlImportPhase3ComplexTest.java` (Part 2) (9 hours)
  - 9 import test methods
  - 9 round-trip test methods
  - Total: 18 methods

**Shared Tasks (2 hours):**
- [ ] Code review and integration (1 hour)
- [ ] Execute Phase 3 suite (30 min)
- [ ] Performance testing on complex samples (30 min)

**Deliverables:**
- 18 YAML test samples (Tests 50-67)
- 36 test methods implemented
- Phase 3 test suite passing

**Sprint 2 Milestones:**
- ✅ 77% of samples completed (67/87)
- ✅ 77% of test methods implemented (134/174)
- ✅ Complex scenario testing validated

---

### Sprint 3: Phase 4 & Phase 5 Start (Weeks 5-6)

**Objectives:**
- Complete rule chain tests (known limitations)
- Begin edge case testing
- Reach 90% coverage

#### Week 5: Phase 4 Implementation (12 hours) + Edge Cases Start (16 hours)

**Dev A Tasks:**
- [ ] Create YAML samples 68-73 (6 hours, ~1 hour each)
  - All 6 rule chain patterns
  - Document expected limitations
- [ ] Implement `YamlImportPhase4ChainsTest.java` (6 hours)
  - 6 import test methods
  - 6 round-trip test methods
  - Total: 12 methods
  - Add @Disabled annotations with notes about visual editor limitations
- [ ] Create YAML samples 74-80 (7 hours, ~1 hour each)
  - Tests 74-75: Metadata edge cases
  - Tests 76-77: Large counts
  - Tests 78-80: Nesting and expression edge cases

**Dev B Tasks:**
- [ ] Execute and document Phase 4 limitations (3 hours)
  - Test what imports vs. what should import
  - Document gap analysis for future features
- [ ] Create YAML samples 81-87 (7 hours, ~1 hour each)
  - Tests 81-84: Special chars, unicode, many items
  - Tests 85-87: Null handling, collections, bootstrap
- [ ] Begin Phase 5 infrastructure (6 hours)
  - Setup performance monitoring for large tests
  - Create screenshot automation for edge cases
  - Prepare stress test execution framework

**Shared Tasks (2 hours):**
- [ ] Review Phase 4 limitations documentation (1 hour)
- [ ] Plan visual editor enhancements based on gaps (1 hour)

**Deliverables:**
- 14 YAML test samples (Tests 68-81)
- 12 test methods implemented
- Phase 4 complete with documented limitations
- Phase 5 setup in progress

#### Week 6: Phase 5 Completion (28 hours)

**Dev A Tasks (14 hours):**
- [ ] Implement `YamlImportPhase5EdgeCasesTest.java` (Part 1) (7 hours)
  - Tests 74-80 (7 import + 7 round-trip = 14 methods)

**Dev B Tasks (14 hours):**
- [ ] Implement `YamlImportPhase5EdgeCasesTest.java` (Part 2) (7 hours)
  - Tests 81-87 (7 import + 7 round-trip = 14 methods)
- [ ] Execute full Phase 5 suite (2 hours)
- [ ] Performance analysis and optimization (3 hours)
  - Identify slow tests
  - Optimize wait times
  - Add parallel test execution where safe

**Shared Tasks (2 hours):**
- [ ] Code review all Phase 5 tests (1 hour)
- [ ] Execute complete test suite (all 174 methods) (1 hour)

**Deliverables:**
- 6 YAML test samples completed (Tests 82-87)
- 28 test methods implemented
- Phase 5 complete
- Full suite execution validated

**Sprint 3 Milestones:**
- ✅ 100% of samples completed (87/87)
- ✅ 100% of test methods implemented (174/174)
- ✅ All phases validated

---

### Sprint 4: Documentation & Optimization (Weeks 7-8)

**Objectives:**
- Complete documentation
- Optimize test execution
- Integrate into CI/CD
- Knowledge transfer

#### Week 7: Documentation & Polish (16 hours)

**Dev A Tasks:**
- [ ] Update APEX_BLOCKS_PROTOTYPE_GUIDE.md (4 hours)
  - Add references to all 87 test samples
  - Update validation checklist
  - Add troubleshooting guidance from test learnings
- [ ] Create test execution guide (3 hours)
  - How to run individual tests
  - How to run phase tests
  - How to interpret failures
  - How to add new tests
- [ ] Create known limitations document (2 hours)
  - Document Phase 4 gaps (rule chains)
  - Document visual editor vs. YAML spec differences
  - Prioritize future enhancements

**Dev B Tasks:**
- [ ] Optimize test execution performance (6 hours)
  - Reduce wait times where safe
  - Parallelize independent tests
  - Cache expensive operations
  - Target: Complete suite under 30 minutes
- [ ] Create test data generation utilities (3 hours)
  - YAML template generator
  - Random data generator for stress tests
  - Bulk test creation scripts

**Shared Tasks (2 hours):**
- [ ] Review all documentation (1 hour)
- [ ] Execute full suite with optimizations (1 hour)

**Deliverables:**
- Complete test documentation
- Optimized test suite (30 min target)
- Test utilities for future expansion

#### Week 8: CI/CD Integration & Knowledge Transfer (16 hours)

**Dev A Tasks:**
- [ ] CI/CD pipeline integration (4 hours)
  - Add to Jenkins/GitHub Actions
  - Configure scheduled runs
  - Setup failure notifications
  - Create test result dashboards
- [ ] Create knowledge transfer materials (4 hours)
  - Architecture overview presentation
  - Video walkthrough of test suite
  - Maintenance guide
  - FAQ document

**Dev B Tasks:**
- [ ] Test stability improvements (4 hours)
  - Fix flaky tests
  - Add retry logic for transient failures
  - Improve error messages
  - Add test tagging (smoke, regression, stress)
- [ ] Performance benchmarking (4 hours)
  - Baseline performance metrics
  - Track test execution trends
  - Identify regression candidates
  - Create performance reports

**Shared Tasks (2 hours):**
- [ ] Final validation and sign-off (1 hour)
- [ ] Knowledge transfer session with team (1 hour)

**Deliverables:**
- CI/CD fully integrated
- Knowledge transfer complete
- Test suite production-ready

**Sprint 4 Milestones:**
- ✅ Complete documentation
- ✅ CI/CD integration operational
- ✅ Team trained on test suite
- ✅ Production deployment ready
(Selenium Setup) 
---

### Risk Management

#### Identified Risks & Mitigation

| Risk | Probability | Impact | Mitigation Strategy |
|------|-------------|--------|---------------------|
| **Visual editor import bugs** | High | High | Test incrementally; document bugs for dev team to fix in parallel; prioritize critical paths |
| **Selenium infrastructure delays** | Medium | High | **Sprint 1 Week 1 entirely dedicated to Selenium setup**; parallel ChromeDriver and WebDriverManager testing; fallback to manual driver management if auto-download fails |
| **Selenium test flakiness** | High | Medium | **Explicit waits only** (no implicit waits); retry logic with `@RepeatedTest`; StaleElementReferenceException handling; JavaScript polling for Blockly state; screenshot capture on all failures |
| **ChromeDriver version mismatch** | Medium | High | Use **WebDriverManager** for automatic version matching; lock Chrome version in CI; test on multiple Chrome versions locally |
| **Headless mode rendering issues** | Medium | Medium | Test both headless and headed modes; increase viewport size (`--window-size=1920,1080`); add `--disable-gpu` flag; screenshot comparison between modes |
| **JavaScript execution failures** | Medium | High | Wrap all `JavascriptExecutor` calls in try-catch; validate Blockly workspace loaded before executing; add timeout handling; log JavaScript errors |
| **YAML spec ambiguity** | Medium | Medium | Reference APEX_YAML_REFERENCE.md; consult with APEX core team; create test cases for ambiguous scenarios |
| **Performance issues (large tests)** | Medium | Low | Optimize in Sprint 4; use headless mode exclusively in CI; parallelize test execution carefully (browser instances); implement test tagging for smoke vs. full regression |
| **CI/CD Chrome installation** | Low | High | Use official Chrome Docker images; document manual installation steps; test CI pipeline early in Week 1 |
| **Browser memory leaks** | Low | Medium | Ensure `driver.quit()` in `@AfterEach`; monitor memory usage; restart browser every N tests if needed |
| **Resource availability** | Low | High | Cross-train developers on Selenium; document all WebDriver patterns; enable async work on independent test phases |
| **Scope creep** | Medium | Medium | Lock scope at 87 tests + Selenium infrastructure; track new requests for v2.0; maintain backlog for future enhancements |

#### Q**Selenium infrastructure fully operational** (ChromeDriver, WebDriverWait, JavascriptExecutor)
- [ ] All Phase 1 Selenium tests pass on first execution (both headless and headed modes)
- [ ] No Selenium test takes longer than 30 seconds (including browser startup)
- [ ] Code coverage of Selenium test infrastructure > 80%
- [ ] Screenshot capture working on test failures
- [ ] CI/CD pipeline successfully runs headless Selenium tests

**Sprint 1:**
- [ ] All Phase 1 tests pass on first execution
- [ ] No test takes longer than 30 seconds
- [ ] Code coverage of test infrastructure > 80%

**Sprint 2:**
- [ ] All Phase 2 & 3 tests pass
- [ ] Round-trip semantic validation 100% accurate
- [ ] No regression in Phase 1 tests

**Sprint 3:**
- [ ] All 174 tests implemented
- [ ] Full suite executes in < 45 minutes
- [ ] All known limitations documented

**Sprint 4:**
- [ ] Documentation complete and reviewed
- [ ] CI/CD pipeline executes successfully
- [ ] Team sign-off on knowledge transfer

---

### Success Metrics

#### Quantitative Metrics

- **Coverage**: 100% of 52 block types tested
- **Test Count**: 174 test methods implemented
- **Execution Time**: Full suite < 30 minutes
- **Pass Rate**: 100% on clean environment
- **Stability**: < 2% flaky test rate
- **CI/CD Integration**: Automated runs on every PR

#### Qualitative Metrics

- **Documentation Quality**: Team can add new tests without assistance
- **Maintainability**: Clear test organization and naming
- **Debuggability**: Failures provide actionable error messages
- **Knowledge Transfer**: 2+ team members fully trained

---

### Resource Allocation

#### Developer Time Breakdown

| Phase | Dev A Hours | Dev B Hours | Shared Hours | Total Hours |
|-------|-------------|-------------|--------------|-------------|
| Sprint 1 Week 1 | 12 | 12 | 4 | 28 |
| Sprint 1 Week 2 | 36 | 34 | 4 | 74 |
| Sprint 2 Week 3 | 14 | 14 | 2 | 30 |
| Sprint 2 Week 4 | 18 | 18 | 2 | 38 |
| Sprint 3 Week 5 | 13 | 13 | 2 | 28 |
| Sprint 3 Week 6 | 14 | 14 | 2 | 30 |
| Sprint 4 Week 7 | 9 | 9 | 2 | 20 |
| Sprint 4 Week 8 | 8 | 8 | 2 | 18 |
| **Total** | **124** | **122** | **20** | **266** |

**Total Effort:** 266 hours (~33 person-days across 8 weeks)

#### Budget Breakdown

Assuming 2 developers @ $100/hour:
- Developer time: 266 hours × $100 = **$26,600**
- Infrastructure costs (CI/CD, tools): **$1,000**
- Contingency (15%): **$4,140**

**Total Budget:** ~**$31,740**

---

### Timeline Summary

```
Week 1: Infrastructure Setup
Week 2: Phase 1 (Tests 1-35)
Week 3: Phase 2 (Tests 36-49)
Week 4: Phase 3 (Tests 50-67)
Week 5: Phase 4 (Tests 68-73) + Phase 5 Start
Week 6: Phase 5 Completion (Tests 74-87)
Week 7: Documentation & Optimization
Week 8: CI/CD Integration & Knowledge Transfer
```

**Project Duration:** 8 weeks (2 months)  
**Completion Date:** ~February 13, 2026

---

### Dependencies

#### Internal Dependencies

1. **APEX Playground Spring Boot Application**
   - Must be operational on configurable port (default: 8081)
   - `@SpringBootTest(webEnvironment = RANDOM_PORT)` support
   - `/playground/apex_editor_main.html` endpoint accessible

2. **Selenium WebDriver Infrastructure**
   - Selenium Java bindings (version 4.x+)
   - ChromeDriver executable (compatible with Chrome version)
   - WebDriverManager for automatic driver management
   - Headless Chrome support

3. **Maven Test Framework**
   - JUnit 5 (Jupiter) test framework
   - Maven Surefire plugin (version 3.x+)
   - Spring Boot Test dependencies
   - TestNG (optional, if preferred over JUnit)

4. **Blockly JavaScript API**
   - Blockly workspace accessible via `Blockly.getMainWorkspace()`
   - YAML import/export functionality implemented in `apex_editor_main.html`
   - `js-yaml` library for YAML parsing
   - Import modal (`#yamlImportModal`) functional
   - Export/download YAML functionality operational

5. **Development Environment**
   - Java 17+ (for Selenium 4 compatibility)
   - Chrome browser installed (version 120+)
   - Access to APEX source code for reference
   - File system access to `examples/` directory

#### External Dependencies

1. **Selenium WebDriver Components**
   - **ChromeDriver**: Version compatible with installed Chrome (auto-managed by WebDriverManager)
   - **Chrome Browser**: Version 120+ recommended
   - **Selenium Java**: 4.15.0+ (latest stable)
   - **WebDriverManager**: 5.6.0+ for automatic driver setup

2. **CI/CD Environment**
   - **Platform**: Jenkins, GitHub Actions, or GitLab CI
   - **Linux CI Requirements**: Xvfb for headless X server (if not using native headless)
   - **Chrome Installation**: Chrome/Chromium in CI container
   - **ChromeDriver**: Installed or auto-downloaded via WebDriverManager
   - **Network Access**: Localhost port binding for Spring Boot app

3. **Human Resources**
   - Code review availability from senior developers
   - APEX core team for YAML specification clarifications
   - DevOps support for CI/CD configuration
   - QA team for test validation (optional)

4. **Infrastructure**
   - Build server with sufficient resources (2+ CPU cores, 4GB+ RAM for Selenium)
   - Artifact repository for Selenium screenshots
   - Test report hosting (Jenkins HTML reports, Allure, etc.)

---

### Post-Implementation Maintenance

#### Ongoing Activities

- **Weekly test execution** as part of CI/CD
- **Monthly test review** for flakiness and performance
- **Quarterly test expansion** as new APEX features are added
- **Annual test refactoring** for maintainability

#### Test Suite Evolution (v2.0)

Future enhancements to consider:

1. **Visual Regression Testing** - Screenshot comparison
2. **Accessibility Testing** - WCAG compliance
3. **Browser Compatibility** - Firefox, Safari, Edge
4. **Mobile Responsive Testing** - Tablet and mobile layouts
5. **Performance Benchmarking** - Load time tracking
6. **API Integration Tests** - REST API validation
7. **Rule Chain Support** - When visual editor adds support
8. **Pipeline Support** - When visual editor adds support

---

## Conclusion

To achieve comprehensive YAML import validation coverage for the APEX Visual Editor:

- **87 test YAML samples** are required (vs. current 6)
- **174 total test methods** needed (import + round-trip for each)
- **6 test classes** recommended for organization
- **8 weeks / 266 hours** implementation timeline
- **~$31,740** total budget (2 developers)

This systematic, phased approach ensures complete coverage of APEX's 50+ block types, all valid YAML combinations, advanced patterns, and edge cases, providing confidence that the visual editor accurately represents the full APEX YAML specification.

### Implementation Timeline at a Glance

| Sprint | Focus | Tests | Methods | Completion |
|--------|-------|-------|---------|------------|
| Sprint 1 (Weeks 1-2) | Infrastructure + Phase 1 | 35 | 70 | 40% |
| Sprint 2 (Weeks 3-4) | Phase 2 + Phase 3 | 32 | 64 | 77% |
| Sprint 3 (Weeks 5-6) | Phase 4 + Phase 5 | 20 | 40 | 100% |
| Sprint 4 (Weeks 7-8) | Documentation + CI/CD | 0 | 0 | Production |

**Start Date:** December 19, 2025  
**Target Completion:** February 13, 2026  
**Status:** Ready to begin Sprint 1
