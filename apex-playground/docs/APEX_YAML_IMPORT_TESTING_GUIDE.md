# APEX Visual Editor - YAML Import Testing Guide

**Version:** 2.0  
**Last Updated:** December 19, 2025  
**Status:** ⚠️ 12/87 tests operational (14% coverage) - Week 2 compilation fixed, runtime debugging needed

> **📘 Purpose:** This comprehensive guide merges test coverage analysis, implementation planning, and developer documentation for YAML import testing in the APEX Visual Editor (Blockly-based).

---

## Table of Contents

1. [Executive Summary](#executive-summary)
2. [Current Status](#current-status)
3. [Quick Start Guide](#quick-start-guide)
4. [Test Coverage Analysis](#test-coverage-analysis)
5. [Implementation Plan](#implementation-plan)
6. [Developer Guide](#developer-guide)
7. [BaseYamlImportSeleniumTest API](#baseyamlimportseleniumtest-api)
8. [Creating New Tests](#creating-new-tests)
9. [Best Practices](#best-practices)
10. [Debugging & Troubleshooting](#debugging--troubleshooting)
11. [CI/CD Integration](#cicd-integration)
12. [Appendices](#appendices)

---

## Executive Summary

This guide covers comprehensive YAML import testing for the APEX Visual Editor. Based on analysis of **APEX_BLOCKS_PROTOTYPE_GUIDE.md** (52 unique block types in 12 categories) and **APEX_YAML_REFERENCE.md** (~155 keywords, ~140 functionally implemented), we need systematic testing of all possible YAML configuration combinations.

### Coverage Goals

- **Current Coverage:** 6 basic test samples (12 total tests with round-trip)  
- **Required Coverage:** 87 comprehensive test samples covering all block types and combinations  
- **Gap:** 81 additional test samples needed for complete coverage

### Project Metrics

- **Total Test Methods:** 174 (87 import + 87 round-trip)
- **Test Classes:** 6 organized by phase
- **Timeline:** 8 weeks (4 sprints)
- **Effort:** 266 hours (~33 person-days)
- **Budget:** ~$31,740 (2 developers @ $100/hour)

### Success Criteria

1. ✅ **Block Coverage**: All 52 block types tested
2. ✅ **Combination Coverage**: All valid section combinations tested
3. ✅ **Pattern Coverage**: All APEX patterns (chains, scenarios, components) tested
4. ✅ **Edge Case Coverage**: Boundary conditions and stress tests included
5. ✅ **Round-Trip Validation**: 100% semantic preservation verified
6. ✅ **Documentation**: All test samples documented in guide

---

## Current Status

### Sprint 1 Week 2 Progress

**✅ Completed:**
- Selenium infrastructure fully operational (Week 1 complete)
- BaseYamlImportSeleniumTest with 15 helper methods (350+ lines)
- Screenshot capture on failure (ScreenshotOnFailureExtension.java)
- Development guide documentation (merged into this document)
- 29/29 YAML samples created (100%)
- YamlImportPhase1SimpleTest.java compiles successfully (30 test methods)

**⚠️ Current Blocker:**
```
[ERROR] TimeoutException: Expected condition failed: waiting for Blockly workspace to load
[ERROR] Timeout after 15 seconds at waitForBlocklyWorkspaceToLoad()
```

**Root Cause Analysis:**
- Selenium tests successfully navigate to playground URL
- WebDriver timeout waiting for `Blockly.getMainWorkspace() != null`
- Possible causes:
  1. Spring Boot application not fully started before test execution
  2. JavaScript resources not loading in headless Chrome
  3. Blockly library initialization timing issue
  4. Base URL construction issue (`http://localhost:[port]/playground`)

**Required Actions:**
1. Review `YamlImportValidationUITest.java` (existing passing tests) for differences
2. Add additional wait/initialization logic before loading playground page
3. Consider adding `@DirtiesContext` or longer startup delays
4. Debug in headed mode (remove `--headless`) to observe actual page loading
5. Verify playground endpoint is accessible during test execution

### Coverage Status

| Metric | Current | Target | Progress |
|--------|---------|--------|----------|
| YAML Samples | 35 | 87 | 40% |
| Test Methods | 12 | 174 | 7% |
| Compilation | ✅ SUCCESS | ✅ SUCCESS | 100% |
| Execution | ⚠️ BLOCKED | ✅ PASSING | 0% |

---

## Quick Start Guide

### Prerequisites

- Java 17+
- Maven 3.8+
- Chrome browser installed (version 120+)
- APEX Playground running on localhost

### Running Existing Tests

```bash
# Run all YAML import tests
cd apex-playground
mvn test -Dtest=YamlImport*Test

# Run specific test class
mvn test -Dtest=YamlImportValidationUITest

# Run in headed mode (see browser)
mvn test -Dtest=YamlImportValidationUITest -Dselenium.headless=false

# Run specific test method
mvn test -Dtest=YamlImportValidationUITest#testImportBasicRulesConfiguration

# Run specific phase
mvn test -Dtest=YamlImportPhase1*Test

# With increased logging
mvn test -Dtest=YamlImportValidationUITest -Dlogging.level.dev.mars.apex=DEBUG
```

### Project Structure

```
apex-playground/
├── src/test/java/dev/mars/apex/playground/ui/
│   ├── BaseYamlImportSeleniumTest.java          # Abstract base class (350+ lines)
│   ├── ScreenshotOnFailureExtension.java        # Screenshot capture
│   ├── YamlImportValidationUITest.java          # Tests 1-6 (baseline) ✅
│   └── uitest/
│       └── YamlImportPhase1SimpleTest.java      # Tests 7-36 ⚠️ (runtime blocked)
├── examples/
│   ├── validation/                               # 6 YAML files
│   ├── lookup/                                   # 4 YAML files
│   ├── enrichment/                               # 12 YAML files
│   ├── conditional/                              # 8 YAML files
│   ├── configuration/                            # 3 YAML files
│   ├── scenario/                                 # 3 YAML files
│   ├── components/                               # 3 YAML files
│   ├── error-recovery/                           # 3 YAML files
│   ├── data-sources/                             # 2 YAML files
│   ├── rules/                                    # 3 YAML files
│   ├── transformations/                          # 3 YAML files
│   ├── enrichments/                              # 3 YAML files
│   ├── lookups/                                  # 4 YAML files
│   └── templates/                                # 3 YAML files
└── target/selenium-screenshots/                  # Auto-captured on failure
```

---

## Test Coverage Analysis

### Block Inventory

**Total Blocks: 52 Unique Block Types**

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

### Test Coverage Matrix

#### Phase 1: Individual Block Types (Tests 1-35)

Test each block type in isolation with import + round-trip validation.

| Test # | Block Type | Description | File Path | Status |
|--------|------------|-------------|-----------|--------|
| 1-6 | Basic configurations | Rules, lookups, calculations, scenarios, error recovery, components | examples/validation/, examples/lookup/, examples/enrichment/, examples/conditional/, examples/basic/ | ✅ COMPLETE |
| 7-9 | Configuration variations | Global settings, execution options, metadata edge cases | examples/configuration/ | ✅ YAML CREATED |
| 10-12 | Scenario patterns | Rule ref routing, hybrid classification, nested scenarios | examples/scenario/ | ✅ YAML CREATED |
| 13-15 | Multi-component configs | Component groups, execution order, dependencies | examples/components/ | ✅ YAML CREATED |
| 16-18 | Advanced error recovery | Notification policies, custom handlers, retry strategies | examples/error-recovery/ | ✅ YAML CREATED |
| 19-20 | Data source combinations | Multiple refs, conditional enablement | examples/data-sources/ | ✅ YAML CREATED |
| 21-23 | Rule variations | Advanced rule groups, inline rules, conditional rules | examples/rules/ | ✅ YAML CREATED |
| 24-26 | Transformation types | Field mapping, data type conversion, custom expressions | examples/transformations/ | ✅ YAML CREATED |
| 27-29 | Enrichment combinations | Composite enrichments, conditional enrichment, async patterns | examples/enrichments/ | ✅ YAML CREATED |
| 30-33 | Lookup patterns | Multi-key lookups, fallback values, cache config, dynamic lookups | examples/lookups/ | ✅ YAML CREATED |
| 34-36 | Template usage | Reusable blocks, parameterized templates, template inheritance | examples/templates/ | ✅ YAML CREATED |

**Phase 1 Deliverables:** 35 YAML samples, 70 test methods (35 import + 35 round-trip)

#### Phase 2: Section Combinations (Tests 36-49)

Test configurations with different section combinations.

| Test # | Sections Combined | Description | Status |
|--------|-------------------|-------------|--------|
| 36 | Rules + Enrichments | Both rules and enrichments | ⏳ PENDING |
| 37 | Rules + Rule Groups | Mix of rules and groups | ⏳ PENDING |
| 38 | Enrichments + Lookups | Enrichments with lookups | ⏳ PENDING |
| 39 | Rules + Transformations | Rules with transformations | ⏳ PENDING |
| 40 | Rules + Error Recovery | Rules with error recovery | ⏳ PENDING |
| 41 | Rules + Data Source Refs | Rules with external data | ⏳ PENDING |
| 42 | Enrichments + Error Recovery | Enrichments with error recovery | ⏳ PENDING |
| 43 | All Sections | Complete configuration | ⏳ PENDING |
| 44 | Enrichment Groups + Enrichments | Mixed groups and standalone | ⏳ PENDING |
| 45 | Rule Groups + Rules | Mixed groups and standalone | ⏳ PENDING |
| 46 | Nested Rule Groups | Rule groups within groups | ⏳ PENDING |
| 47 | Nested Enrichment Groups | Enrichment groups within groups | ⏳ PENDING |
| 48 | Multiple Data Source Refs | 3+ data source references | ⏳ PENDING |
| 49 | Transformations + Enrichments | Transforms with enrichments | ⏳ PENDING |

**Phase 2 Deliverables:** 14 YAML samples, 28 test methods

#### Phase 3: Complex Scenarios (Tests 50-67)

Test advanced patterns and edge cases.

| Test # | Pattern | Description | Status |
|--------|---------|-------------|--------|
| 50-52 | Multi-stage scenarios | 5+ processing stages, conditional stages, failure policies | ⏳ PENDING |
| 53-54 | Chained/parallel lookups | Multiple sequential/parallel lookups | ⏳ PENDING |
| 55-56 | Date math & string ops | Date calculations, string operations | ⏳ PENDING |
| 57-58 | Nested conditionals | Nested conditionals, ternary operators | ⏳ PENDING |
| 59-61 | Rule priorities & categories | Rules with priorities, categories, custom props | ⏳ PENDING |
| 62 | Parallel execution | Parallel enrichments | ⏳ PENDING |
| 63-64 | Component execution | Execution order, failure policies | ⏳ PENDING |
| 65-67 | Error recovery advanced | Retry logic, safe expression, connection pool | ⏳ PENDING |

**Phase 3 Deliverables:** 18 YAML samples, 36 test methods

#### Phase 4: Rule Chains (Tests 68-73)

Test all 6 rule chain patterns (NOT currently supported in visual editor).

| Test # | Chain Pattern | Description | Status |
|--------|---------------|-------------|--------|
| 68 | Conditional Chaining | If-then chain | ⏳ PENDING |
| 69 | Sequential Dependency | Ordered execution | ⏳ PENDING |
| 70 | Result-Based Routing | Route by result | ⏳ PENDING |
| 71 | Accumulative Chaining | Accumulate results | ⏳ PENDING |
| 72 | Complex Workflow | Multi-path workflow | ⏳ PENDING |
| 73 | Fluent Builder | Builder pattern | ⏳ PENDING |

**Phase 4 Deliverables:** 6 YAML samples, 12 test methods (with @Disabled annotations documenting limitations)

#### Phase 5: Edge Cases & Stress Tests (Tests 74-87)

Test boundary conditions and large configurations.

| Test # | Edge Case | Description | Status |
|--------|-----------|-------------|--------|
| 74-75 | Metadata edge cases | Empty metadata, maximum metadata | ⏳ PENDING |
| 76-77 | Large counts | 50+ rules, 50+ enrichments | ⏳ PENDING |
| 78-79 | Deep nesting & long expressions | 3-level nesting, 200+ char expressions | ⏳ PENDING |
| 80-81 | Special characters & Unicode | Quotes, colons, escapes, UTF-8 | ⏳ PENDING |
| 82-83 | Multiple items | 10+ classifications, 20+ mappings | ⏳ PENDING |
| 84-85 | All severities & null handling | All 4 severity levels, null checks | ⏳ PENDING |
| 86-87 | Collections & bootstrap | List/Map operations, complete OTC workflow | ⏳ PENDING |

**Phase 5 Deliverables:** 14 YAML samples, 28 test methods

### Round-Trip Test Multiplication

**Each test requires 2 test methods:**
1. **Import test** - Verify blocks created correctly from YAML
2. **Round-trip test** - Import → Export → Verify semantic equivalence

**Total Test Methods Required:** 87 × 2 = **174 test methods**

---

## Implementation Plan

### Timeline Overview

| Sprint | Weeks | Focus | Tests Added | Total Tests | Coverage | Status |
|--------|-------|-------|-------------|-------------|----------|---------|
| Baseline | - | Initial setup | 6 | 6 | 7% | ✅ COMPLETE |
| Sprint 1 | 1-2 | Infrastructure + Phase 1 | 29 | 35 | 40% | 🔄 IN PROGRESS |
| Sprint 2 | 3-4 | Phase 2 + Phase 3 | 32 | 67 | 77% | ⏳ PENDING |
| Sprint 3 | 5-6 | Phase 4 + Phase 5 | 20 | 87 | 100% | ⏳ PENDING |
| Sprint 4 | 7-8 | Documentation + CI/CD | 0 | 87 | 100% | ⏳ PENDING |

**Total Duration:** 8 weeks
**Target Completion:** February 13, 2026

### Sprint 1: Infrastructure + Phase 1 (Weeks 1-2)

#### Week 1: Selenium Infrastructure Setup ✅ COMPLETE

**Completed Deliverables:**
- ✅ BaseYamlImportSeleniumTest abstract class (350+ lines, 15 helper methods)
- ✅ WebDriverManager for automatic ChromeDriver setup
- ✅ Screenshot capture on failure (ScreenshotOnFailureExtension.java)
- ✅ Development guide (merged into this document)
- ✅ All 12 baseline tests validated passing

**Infrastructure Components:**
- WebDriver setup with ChromeOptions (headless mode)
- WebDriverWait with 15-second timeout
- JavascriptExecutor for Blockly workspace interaction
- Spring Boot integration (@SpringBootTest with RANDOM_PORT)
- Helper methods for YAML import/export and block validation

#### Week 2: Phase 1 Implementation ⏳ IN PROGRESS

**YAML Samples Creation:** ✅ COMPLETE (29/29 - 100%)

**Test Implementation:** ✅ COMPILATION FIXED, ⚠️ RUNTIME BLOCKED

- ✅ Created YamlImportPhase1SimpleTest.java (30 test methods)
- ✅ BUILD SUCCESS - No compilation errors
- ⚠️ Runtime timeout error - Blockly workspace not loading
- 🔴 Coverage still at 14% (12/87) - blocked until runtime issues resolved
- 🎯 Target: 40% (35/87) when tests pass

**Current Blocker Resolution Steps:**
1. Compare with YamlImportValidationUITest.java (working tests)
2. Add additional initialization waits
3. Debug in headed mode to observe page loading
4. Verify Spring Boot app fully started before navigation
5. Check base URL construction and endpoint accessibility

**Remaining Tasks:**
- [ ] Debug and fix Blockly workspace loading timeout
- [ ] Run full test suite (35 tests = 70 methods) in headless mode
- [ ] Run full test suite in headed mode to verify rendering
- [ ] Update coverage analysis with results

**Sprint 1 Target Milestones:**
- [ ] All 35 Phase 1 Selenium tests pass (headless + headed)
- [ ] No test exceeds 30 seconds
- [ ] Screenshot capture verified
- [ ] 40% coverage achieved

### Sprint 2: Phase 2 + Phase 3 (Weeks 3-4)

**Focus:** Section combinations and complex scenarios

**Week 3: Phase 2 Implementation**
- [ ] Create 14 YAML samples for section combinations
- [ ] Implement YamlImportPhase2CombinationsTest.java (28 test methods)
- [ ] Validate all tests pass
- [ ] Target: 56% coverage (49/87)

**Week 4: Phase 3 Implementation**
- [ ] Create 18 YAML samples for complex scenarios
- [ ] Implement YamlImportPhase3ComplexTest.java (36 test methods)
- [ ] Validate all tests pass
- [ ] Target: 77% coverage (67/87)

**Sprint 2 Deliverables:**
- 32 new YAML samples
- 64 new test methods
- 77% total coverage
- Updated documentation

### Sprint 3: Phase 4 + Phase 5 (Weeks 5-6)

**Focus:** Rule chains and edge cases

**Week 5: Phase 4 Implementation**
- [ ] Create 6 YAML samples for rule chains
- [ ] Implement YamlImportPhase4ChainsTest.java (12 test methods with @Disabled)
- [ ] Document visual editor limitations
- [ ] Target: 84% coverage (73/87)

**Week 6: Phase 5 Implementation**
- [ ] Create 14 YAML samples for edge cases
- [ ] Implement YamlImportPhase5EdgeCasesTest.java (28 test methods)
- [ ] Validate all tests pass
- [ ] Target: 100% coverage (87/87)

**Sprint 3 Deliverables:**
- 20 new YAML samples
- 40 new test methods
- 100% total coverage
- Comprehensive edge case documentation

### Sprint 4: Documentation + CI/CD (Weeks 7-8)

**Focus:** Finalization and automation

**Week 7: Documentation**
- [ ] Update this guide with all test results
- [ ] Create test execution reports
- [ ] Document known limitations
- [ ] Create troubleshooting guide updates

**Week 8: CI/CD Integration**
- [ ] Configure GitHub Actions workflow
- [ ] Setup headless Chrome in CI environment
- [ ] Configure screenshot artifact upload
- [ ] Setup test result reporting
- [ ] Performance optimization (parallel execution)

**Sprint 4 Deliverables:**
- Complete documentation
- Automated CI/CD pipeline
- Test execution reports
- Performance benchmarks

### Resource Allocation

**Team Structure:**
- **Developer 1 (Lead):** Infrastructure, complex tests, CI/CD
- **Developer 2:** YAML samples, test implementation, documentation

**Effort Breakdown:**
- Sprint 1: 66 hours (Infrastructure: 24h, Phase 1: 42h)
- Sprint 2: 80 hours (Phase 2: 40h, Phase 3: 40h)
- Sprint 3: 60 hours (Phase 4: 20h, Phase 5: 40h)
- Sprint 4: 60 hours (Documentation: 30h, CI/CD: 30h)
- **Total:** 266 hours (~33 person-days)

**Budget:** ~$31,740 (2 developers @ $100/hour)

---

## Developer Guide

### Test Class Structure

All YAML import tests should extend `BaseYamlImportSeleniumTest` and follow this structure:

```java
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class YamlImportPhase1SimpleTest extends BaseYamlImportSeleniumTest {

    @Test
    @Order(1)
    @DisplayName("Test 7: Import Configuration - Global Settings")
    void testImportConfigurationGlobalSettings() throws IOException {
        // 1. Load YAML file
        String yaml = loadYamlFile("examples/configuration/config-global-test.yaml");

        // 2. Navigate to playground
        driver.get(baseUrl + "/playground");
        waitForBlocklyWorkspaceToLoad();

        // 3. Import YAML
        importYamlContent(yaml);
        waitForBlocksToRender();

        // 4. Verify blocks created
        verifyBlockExists("apex_configuration", 1, "Should have 1 configuration block");
        verifyBlockExists("apex_metadata", 1, "Should have 1 metadata block");

        // 5. Verify specific field values
        String name = getBlockFieldValue("apex_configuration", "name");
        assertEquals("Global Configuration", name, "Configuration name should match");
    }

    @Test
    @Order(2)
    @DisplayName("Test 7 Round-Trip: Configuration - Global Settings")
    void testRoundTripConfigurationGlobalSettings() throws IOException {
        // 1. Load original YAML
        String originalYaml = loadYamlFile("examples/configuration/config-global-test.yaml");

        // 2. Import
        driver.get(baseUrl + "/playground");
        waitForBlocklyWorkspaceToLoad();
        importYamlContent(originalYaml);
        waitForBlocksToRender();

        // 3. Export
        String exportedYaml = exportYamlContent();

        // 4. Verify semantic equivalence
        verifyYamlStructure(exportedYaml, List.of("type", "name", "metadata"));
        assertTrue(exportedYaml.contains("Global Configuration"),
            "Exported YAML should preserve configuration name");
    }
}
```

### Test Naming Conventions

- **Test Class:** `YamlImportPhase{N}{Description}Test.java`
- **Import Test Method:** `testImport{BlockType}{Variation}()`
- **Round-Trip Test Method:** `testRoundTrip{BlockType}{Variation}()`
- **Display Name:** `"Test {#}: {Description}"` and `"Test {#} Round-Trip: {Description}"`

### YAML File Organization

```
examples/
├── validation/          # Basic validation tests (Tests 1-6)
├── configuration/       # Configuration blocks (Tests 7-9)
├── scenario/            # Scenario patterns (Tests 10-12)
├── components/          # Component configs (Tests 13-15)
├── error-recovery/      # Error recovery (Tests 16-18)
├── data-sources/        # Data sources (Tests 19-20)
├── rules/               # Rule variations (Tests 21-23)
├── transformations/     # Transformations (Tests 24-26)
├── enrichments/         # Enrichments (Tests 27-29)
├── lookups/             # Lookup patterns (Tests 30-33)
└── templates/           # Templates (Tests 34-36)
```

---

## BaseYamlImportSeleniumTest API

### Core Infrastructure

#### Fields

```java
protected WebDriver driver;              // Selenium WebDriver instance
protected WebDriverWait wait;            // WebDriverWait with 15-second timeout
protected String baseUrl;                // http://localhost:{port}
protected JavascriptExecutor js;         // For Blockly workspace interaction
protected int port;                      // Spring Boot random port
```

#### Lifecycle Methods

```java
@BeforeAll static void setupClass()      // Initializes WebDriverManager
@BeforeEach void setUp()                 // Creates WebDriver, ChromeOptions
@AfterEach void tearDown()               // Cleans up WebDriver with quit()
```

### Helper Methods Reference

#### YAML Import/Export

```java
// Load YAML from file system
String yaml = loadYamlFile("examples/validation/basic-rules-test.yaml");

// Import YAML into visual editor
importYamlContent(yaml);

// Wait for blocks to render (1-second delay)
waitForBlocksToRender();

// Export YAML from workspace
String exported = exportYamlContent();
```

#### Block Validation

```java
// Get total block count
int count = getBlockCount();
assertEquals(5, count, "Should have 5 blocks");

// Verify specific block type exists with count
verifyBlockExists("apex_rule", 3, "Should have 3 rule blocks");

// Get field value from block
String severity = getBlockFieldValue("apex_rule", "severity");
assertEquals("ERROR", severity);

// Verify multiple severity values
verifySeverityValues(List.of("ERROR", "WARNING", "INFO"));

// Count nested blocks
int nestedCount = countNestedBlocks("apex_rule_group", "rules");
assertEquals(3, nestedCount, "Rule group should have 3 nested rules");
```

#### Advanced Validation

```java
// Verify nested block structure
Map<String, Integer> expectedCounts = Map.of(
    "rules", 3,
    "enrichments", 2
);
verifyNestedBlockStructure("apex_configuration", expectedCounts);

// Verify block execution order
List<String> expectedOrder = List.of(
    "apex_enrichment",
    "apex_rule",
    "apex_transformation"
);
verifyBlockOrder(expectedOrder);

// Verify YAML structure
verifyYamlStructure(exportedYaml, List.of("rules", "enrichments", "metadata"));
```

### Complete Helper Method List

| Method | Purpose | Parameters | Returns |
|--------|---------|------------|---------|
| `loadYamlFile()` | Load YAML from file | `String relativePath` | `String` |
| `importYamlContent()` | Import YAML to editor | `String yamlContent` | `void` |
| `exportYamlContent()` | Export YAML from editor | - | `String` |
| `waitForBlocklyWorkspaceToLoad()` | Wait for workspace ready | - | `void` |
| `waitForBlocksToRender()` | Wait after import (1s) | - | `void` |
| `getBlockCount()` | Count all blocks | - | `int` |
| `verifyBlockExists()` | Verify block type + count | `String type, int count, String message` | `void` |
| `getBlockFieldValue()` | Get field value | `String blockType, String fieldName` | `String` |
| `verifySeverityValues()` | Verify rule severities | `List<String> severities` | `void` |
| `countNestedBlocks()` | Count nested children | `String parentType, String inputName` | `int` |
| `verifyNestedBlockStructure()` | Verify nested counts | `String parentType, Map<String, Integer> expectedCounts` | `void` |
| `verifyBlockOrder()` | Verify execution order | `List<String> expectedBlockTypes` | `void` |
| `verifyYamlStructure()` | Verify YAML sections | `String yaml, List<String> requiredSections` | `void` |
| `getBlocksByType()` | Get all blocks of type | `String blockType` | `List<Object>` |
| `verifyBlockFieldValues()` | Verify multiple fields | `String blockType, Map<String, String> expectedValues` | `void` |

---

## Creating New Tests

### Step-by-Step Guide

#### 1. Create YAML Sample

Create a new YAML file in the appropriate `examples/` subdirectory:

```yaml
# examples/enrichments/enrichment-composite-test.yaml
type: "configuration"
name: "Composite Enrichment Test"
metadata:
  description: "Test composite enrichment with multiple operations"
  version: "1.0"

enrichments:
  - type: "calculation-enrichment"
    name: "Calculate Total"
    target-field: "totalAmount"
    expression: "#baseAmount + #taxAmount"
  - type: "field-enrichment"
    name: "Set Status"
    target-field: "status"
    source-field: "processedStatus"
```

#### 2. Create Test Methods

Add import and round-trip test methods to the appropriate test class:

```java
@Test
@Order(27)
@DisplayName("Test 27: Import Enrichment - Composite")
void testImportEnrichmentComposite() throws IOException {
    String yaml = loadYamlFile("examples/enrichments/enrichment-composite-test.yaml");

    driver.get(baseUrl + "/playground");
    waitForBlocklyWorkspaceToLoad();
    importYamlContent(yaml);
    waitForBlocksToRender();

    // Verify blocks
    verifyBlockExists("apex_configuration", 1, "Should have 1 configuration block");
    verifyBlockExists("apex_enrichments", 1, "Should have 1 enrichments section");
    verifyBlockExists("apex_calculation_enrichment", 1, "Should have 1 calculation enrichment");
    verifyBlockExists("apex_field_enrichment", 1, "Should have 1 field enrichment");

    // Verify field values
    String targetField = getBlockFieldValue("apex_calculation_enrichment", "target_field");
    assertEquals("totalAmount", targetField, "Target field should be totalAmount");
}

@Test
@Order(28)
@DisplayName("Test 27 Round-Trip: Enrichment - Composite")
void testRoundTripEnrichmentComposite() throws IOException {
    String originalYaml = loadYamlFile("examples/enrichments/enrichment-composite-test.yaml");

    driver.get(baseUrl + "/playground");
    waitForBlocklyWorkspaceToLoad();
    importYamlContent(originalYaml);
    waitForBlocksToRender();

    String exportedYaml = exportYamlContent();

    // Verify structure preserved
    verifyYamlStructure(exportedYaml, List.of("type", "name", "enrichments"));
    assertTrue(exportedYaml.contains("totalAmount"), "Should preserve target field");
    assertTrue(exportedYaml.contains("processedStatus"), "Should preserve source field");
}
```

#### 3. Run and Validate

```bash
# Run new test
mvn test -Dtest=YamlImportPhase1SimpleTest#testImportEnrichmentComposite

# Run in headed mode to debug
mvn test -Dtest=YamlImportPhase1SimpleTest#testImportEnrichmentComposite -Dselenium.headless=false

# Check screenshot if failed
ls -la target/selenium-screenshots/
```

---

## Best Practices

### Test Design

1. **One Concept Per Test**
   - Each test should focus on a single block type or pattern
   - Don't combine unrelated validations in one test

2. **Descriptive Names**
   - Use clear, descriptive test method names
   - Include block type and variation in name
   - Use `@DisplayName` for human-readable descriptions

3. **Comprehensive Validation**
   - Always verify block count
   - Verify specific field values
   - Check nested block structures
   - Validate round-trip preservation

4. **Proper Ordering**
   - Use `@Order` annotation for sequential execution
   - Group related tests together
   - Import test before round-trip test

### YAML Sample Design

1. **Minimal but Complete**
   - Include only necessary fields for the test
   - Avoid unnecessary complexity
   - Focus on the feature being tested

2. **Realistic Data**
   - Use realistic field names and values
   - Follow APEX naming conventions
   - Include proper metadata

3. **Edge Cases**
   - Test boundary conditions
   - Include special characters where appropriate
   - Test null/empty values

### Error Handling

1. **Meaningful Assertions**
   ```java
   // ❌ Bad
   assertEquals(3, count);

   // ✅ Good
   assertEquals(3, count, "Should have 3 rule blocks after import");
   ```

2. **Screenshot Capture**
   - Screenshots automatically captured on failure
   - Located in `target/selenium-screenshots/`
   - Named with test class and method name

3. **Debugging Output**
   ```java
   // Add debug output when needed
   System.out.println("Block count: " + getBlockCount());
   System.out.println("Exported YAML:\n" + exportedYaml);
   ```

### Performance

1. **Minimize Waits**
   - Use explicit waits instead of `Thread.sleep()` where possible
   - Only call `waitForBlocksToRender()` after import
   - Don't add unnecessary delays

2. **Reuse WebDriver**
   - WebDriver is created once per test method
   - Cleaned up automatically in `@AfterEach`
   - Don't create additional WebDriver instances

3. **Parallel Execution**
   - Tests can run in parallel (future enhancement)
   - Ensure tests are independent
   - Don't share state between tests

---

## Debugging & Troubleshooting

### Common Issues

#### 1. Blockly Workspace Not Loading

**Symptom:**
```
TimeoutException: Expected condition failed: waiting for Blockly workspace to load
```

**Solutions:**
- Increase timeout in `WebDriverWait` (default 15 seconds)
- Run in headed mode to observe page loading: `-Dselenium.headless=false`
- Check Spring Boot application started: verify logs show "Started ApexPlaygroundApplication"
- Verify playground endpoint accessible: `curl http://localhost:{port}/playground`
- Add additional wait before navigation:
  ```java
  Thread.sleep(2000); // Wait for Spring Boot to fully start
  driver.get(baseUrl + "/playground");
  ```

#### 2. Import Modal Not Found

**Symptom:**
```
NoSuchElementException: Unable to locate element: #importYamlBtn
```

**Solutions:**
- Verify button ID in HTML matches test code
- Check modal is not already open (auto-opened by page scripts)
- Add explicit wait for button:
  ```java
  wait.until(ExpectedConditions.elementToBeClickable(By.id("importYamlBtn")));
  ```

#### 3. Blocks Not Rendering

**Symptom:**
- Import succeeds but `getBlockCount()` returns 0
- Blocks visible in headed mode but not detected

**Solutions:**
- Increase `waitForBlocksToRender()` delay
- Verify JavaScript execution context:
  ```java
  Object workspace = js.executeScript("return Blockly.getMainWorkspace()");
  System.out.println("Workspace: " + workspace);
  ```
- Check browser console for JavaScript errors (headed mode)

#### 4. Round-Trip YAML Mismatch

**Symptom:**
- Exported YAML doesn't match original
- Semantic equivalence validation fails

**Solutions:**
- YAML formatting differences are acceptable (whitespace, order)
- Focus on semantic equivalence, not exact string match
- Use `verifyYamlStructure()` for section presence
- Use `contains()` for specific values
- Don't compare raw YAML strings directly

### Debugging Techniques

#### 1. Headed Mode Debugging

Run tests with visible browser to observe behavior:

```bash
mvn test -Dtest=YamlImportValidationUITest -Dselenium.headless=false
```

Add breakpoints and step through test execution to see browser interactions.

#### 2. JavaScript Console Inspection

In headed mode, open browser DevTools (F12) to:
- Check for JavaScript errors
- Inspect Blockly workspace state
- Verify block structure
- Debug YAML generation

#### 3. Screenshot Analysis

Check screenshots captured on failure:

```bash
ls -la target/selenium-screenshots/
# Example: YamlImportValidationUITest_testImportBasicRulesConfiguration_20251219_143022.png
```

Screenshots show exact browser state at failure point.

#### 4. Verbose Logging

Enable DEBUG logging for detailed output:

```bash
mvn test -Dtest=YamlImportValidationUITest -Dlogging.level.dev.mars.apex=DEBUG
```

Or add to test class:

```java
@TestPropertySource(properties = {
    "logging.level.dev.mars.apex=DEBUG",
    "logging.level.org.springframework=DEBUG"
})
```

#### 5. Manual YAML Verification

Load YAML manually in playground to verify it's valid:

1. Start playground: `mvn spring-boot:run`
2. Open browser: `http://localhost:8081/playground`
3. Click "Import YAML"
4. Paste YAML content
5. Verify blocks render correctly

---

## CI/CD Integration

### GitHub Actions Workflow

Create `.github/workflows/selenium-tests.yml`:

```yaml
name: Selenium YAML Import Tests

on:
  push:
    branches: [ main, develop ]
  pull_request:
    branches: [ main, develop ]

jobs:
  selenium-tests:
    runs-on: ubuntu-latest

    steps:
    - uses: actions/checkout@v3

    - name: Set up JDK 17
      uses: actions/setup-java@v3
      with:
        java-version: '17'
        distribution: 'temurin'
        cache: maven

    - name: Install Chrome
      run: |
        wget -q -O - https://dl-ssl.google.com/linux/linux_signing_key.pub | sudo apt-key add -
        sudo sh -c 'echo "deb [arch=amd64] http://dl.google.com/linux/chrome/deb/ stable main" >> /etc/apt/sources.list.d/google-chrome.list'
        sudo apt-get update
        sudo apt-get install -y google-chrome-stable

    - name: Run Selenium Tests
      run: |
        cd apex-playground
        mvn test -Dtest=YamlImport*Test

    - name: Upload Screenshots on Failure
      if: failure()
      uses: actions/upload-artifact@v3
      with:
        name: selenium-screenshots
        path: apex-playground/target/selenium-screenshots/
        retention-days: 7

    - name: Publish Test Report
      if: always()
      uses: dorny/test-reporter@v1
      with:
        name: Selenium Test Results
        path: apex-playground/target/surefire-reports/*.xml
        reporter: java-junit
```

### Local CI Simulation

Test CI configuration locally:

```bash
# Install Chrome (if not already installed)
# Ubuntu/Debian:
sudo apt-get install google-chrome-stable

# macOS:
brew install --cask google-chrome

# Run tests in headless mode (CI simulation)
cd apex-playground
mvn clean test -Dtest=YamlImport*Test

# Check results
ls -la target/surefire-reports/
ls -la target/selenium-screenshots/
```

### Performance Optimization

#### Parallel Test Execution

Configure Maven Surefire for parallel execution:

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-surefire-plugin</artifactId>
    <version>3.0.0-M9</version>
    <configuration>
        <parallel>methods</parallel>
        <threadCount>4</threadCount>
        <perCoreThreadCount>true</perCoreThreadCount>
    </configuration>
</plugin>
```

**Note:** Ensure tests are independent before enabling parallel execution.

#### Test Execution Time Targets

| Phase | Tests | Target Time | Max Time |
|-------|-------|-------------|----------|
| Phase 1 | 35 | 3-5 minutes | 10 minutes |
| Phase 2 | 14 | 2-3 minutes | 5 minutes |
| Phase 3 | 18 | 3-4 minutes | 7 minutes |
| Phase 4 | 6 | 1-2 minutes | 3 minutes |
| Phase 5 | 14 | 2-3 minutes | 5 minutes |
| **Total** | **87** | **11-17 min** | **30 min** |

---

## Appendices

### Appendix A: Test Execution Checklist

Before committing new tests:

- [ ] YAML file created in correct `examples/` subdirectory
- [ ] Import test method implemented with `@Test`, `@Order`, `@DisplayName`
- [ ] Round-trip test method implemented
- [ ] Both tests pass in headless mode
- [ ] Both tests pass in headed mode
- [ ] Block count verified
- [ ] Field values verified
- [ ] Round-trip semantic equivalence verified
- [ ] Test execution time < 30 seconds
- [ ] No compilation warnings
- [ ] Code follows naming conventions
- [ ] Meaningful assertion messages included

### Appendix B: YAML Sample Template

```yaml
# examples/{category}/{test-name}.yaml
type: "configuration"
name: "{Test Name}"
metadata:
  description: "{Test description}"
  version: "1.0"
  author: "APEX Team"

# Add relevant sections based on test focus
rules:
  - type: "rule"
    name: "Sample Rule"
    severity: "ERROR"
    condition: "#field != null"
    message: "Field is required"

enrichments:
  - type: "calculation-enrichment"
    name: "Sample Calculation"
    target-field: "result"
    expression: "#value * 2"

# ... other sections as needed
```

### Appendix C: Common Block Types Reference

| Block Type | YAML Keyword | Category | Example |
|------------|--------------|----------|---------|
| `apex_configuration` | `type: "configuration"` | Configuration | Root config block |
| `apex_rule` | `type: "rule"` | Rules | Validation rule |
| `apex_rule_group` | `type: "rule-group"` | Rules | Rule group (AND/OR) |
| `apex_enrichment` | `type: "calculation-enrichment"` | Enrichments | Calculation |
| `apex_enrichment_group` | `type: "enrichment-group"` | Enrichments | Enrichment group |
| `apex_lookup_enrichment` | `type: "lookup-enrichment"` | Lookups | Lookup operation |
| `apex_field_enrichment` | `type: "field-enrichment"` | Enrichments | Field mapping |
| `apex_conditional_transformation` | `type: "conditional-transformation"` | Transformations | If-then-else |
| `apex_scenario` | `type: "scenario"` | Scenarios | Scenario config |
| `apex_data_source` | `type: "database-source"` | Data Sources | Database connection |
| `apex_error_recovery` | `type: "error-recovery"` | Error Recovery | Error handling |

### Appendix D: Selenium WebDriver Configuration

**ChromeOptions Used:**

```java
ChromeOptions options = new ChromeOptions();
options.addArguments("--headless");           // Run without GUI
options.addArguments("--no-sandbox");         // Required for CI environments
options.addArguments("--disable-dev-shm-usage"); // Overcome limited resource problems
options.addArguments("--disable-gpu");        // Disable GPU acceleration
options.addArguments("--window-size=1920,1080"); // Set viewport size
```

**WebDriverWait Configuration:**

```java
wait = new WebDriverWait(driver, Duration.ofSeconds(15));
```

**WebDriverManager:**

```java
@BeforeAll
static void setupClass() {
    WebDriverManager.chromedriver().setup(); // Auto-download matching ChromeDriver
}
```

### Appendix E: Resources and References

**Documentation:**
- APEX_BLOCKS_PROTOTYPE_GUIDE.md - Complete block type reference (52 blocks)
- APEX_YAML_REFERENCE.md - YAML syntax reference (~155 keywords)
- BaseYamlImportSeleniumTest.java - Test infrastructure source code

**External Resources:**
- [Selenium WebDriver Documentation](https://www.selenium.dev/documentation/webdriver/)
- [WebDriverManager GitHub](https://github.com/bonigarcia/webdrivermanager)
- [JUnit 5 User Guide](https://junit.org/junit5/docs/current/user-guide/)
- [Google Blockly Documentation](https://developers.google.com/blockly)

**Tools:**
- Chrome DevTools - Browser debugging
- Maven Surefire - Test execution and reporting
- GitHub Actions - CI/CD automation

---

## Document History

| Version | Date | Changes | Author |
|---------|------|---------|--------|
| 1.0 | 2025-12-15 | Initial coverage analysis | APEX Team |
| 1.5 | 2025-12-17 | Added implementation plan | APEX Team |
| 1.8 | 2025-12-18 | Added development guide | APEX Team |
| 2.0 | 2025-12-19 | **Merged all three documents into comprehensive guide** | APEX Team |

**Merged Documents:**
- YAML_IMPORT_TEST_COVERAGE_ANALYSIS.md (1,121 lines)
- SELENIUM_IMPORT_TEST_IMPLEMENTATION_PLAN.md (392 lines)
- SELENIUM_TEST_DEVELOPMENT_GUIDE.md (570 lines)

**Total Content:** 2,083 lines consolidated into single comprehensive guide

---

**End of Document**

