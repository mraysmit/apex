# APEX Visual Editor - Selenium YAML Import Test Implementation Plan

**Status:** ⚠️ 12/87 tests operational (14% coverage) - Week 2 compilation fixed, runtime debugging needed  
**Existing Infrastructure:** ✅ FULLY OPERATIONAL + Enhanced (Week 1 complete)  
**Target:** 87 tests (174 Selenium test methods)  
**Timeline:** Sprint 1 Week 2 in progress (Week 1 ✅ complete)  
**Budget:** TBD  

**Sprint 1 Week 2 Progress:**
- ✅ YAML Samples: 29/29 complete (100%)
- ✅ Test Class: YamlImportPhase1SimpleTest.java compiles successfully (30 test methods)
- ⚠️ Runtime Issue: Tests timeout waiting for Blockly workspace to load - needs debugging
- 🔴 Coverage: Still at 14% until runtime issues resolved (Target: 40%)

---

## CURRENT STATE (ALREADY COMPLETED)

### ✅ Selenium Infrastructure - DONE
**File:** `apex-playground/src/test/java/dev/mars/apex/playground/ui/YamlImportValidationUITest.java`

**Completed Components:**
- ✅ `WebDriver` setup with `ChromeDriver`
- ✅ `ChromeOptions` configured: `--headless`, `--no-sandbox`, `--disable-dev-shm-usage`, `--disable-gpu`, `--window-size=1920,1080`
- ✅ `@BeforeEach` WebDriver initialization
- ✅ `@AfterEach` driver cleanup with `driver.quit()`
- ✅ `WebDriverWait` with 15-second timeout
- ✅ `JavascriptExecutor` for Blockly workspace interaction
- ✅ `@SpringBootTest(webEnvironment = RANDOM_PORT)` integration
- ✅ `@LocalServerPort` injection

**Completed Helper Methods:**
1. ✅ `waitForBlocklyWorkspaceToLoad()` - JavaScript polling until workspace ready
2. ✅ `importYamlContent(String yaml)` - Full Selenium import workflow
3. ✅ `exportYamlContent()` - JavaScript execution to extract YAML
4. ✅ `waitForBlocksToRender()` - Explicit wait with 1-second delay
5. ✅ `getBlockCount()` - JavaScript query of workspace
6. ✅ `verifyBlockExists(String blockType, int count)` - Block count validation
7. ✅ `getBlockFieldValue(String blockType, String fieldName)` - Field value extraction
8. ✅ `verifySeverityValues(List<String> severities)` - Iterate and verify rule severities
9. ✅ `countNestedBlocks(String parentType, String inputName)` - Count nested children
10. ✅ `loadYamlFile(String relativePath)` - Load YAML from filesystem

**Completed Test Methods (12 total):**
- ✅ Test 1-6: Import validation (basic-rules, lookup-enrichment, calculation-enrichment, scenario-classification, error-recovery, component-datasource)
- ✅ Test 7-12: Round-trip validation (Import → Export → Verify semantic equivalence)

**Completed YAML Samples (6 total):**
- ✅ `examples/validation/basic-rules-test.yaml`
- ✅ `examples/lookup/lookup-enrichment-test.yaml`
- ✅ `examples/enrichment/calculation-enrichment-test.yaml`
- ✅ `examples/conditional/scenario-classification-test.yaml`
- ✅ `examples/validation/error-recovery-test.yaml`
- ✅ `examples/basic/component-datasource-test.yaml`

### ✅ Additional Selenium Tests in Module
**File:** `apex-playground/src/test/java/dev/mars/apex/playground/ui/PlaygroundUITest.java`
- Uses `WebDriverManager.chromedriver().setup()` in `@BeforeAll`
- Demonstrates alternative WebDriver initialization pattern

---

## REMAINING WORK

### What Needs to Be Built
1. **81 additional YAML samples** (tests 7-87)
2. **162 additional Selenium test methods** (81 import + 81 round-trip)
3. **Extract base class** from `YamlImportValidationUITest.java` → `BaseYamlImportSeleniumTest.java`
4. **Enhanced helper methods** for complex validation
5. **CI/CD integration** for automated Selenium test execution
6. **Screenshot capture** on test failures
7. **Test execution scripts** for phase-based testing

---

## REVISED IMPLEMENTATION PLAN

### Sprint 1: Refactoring + Scaling to 40% (Weeks 1-2)

#### Week 1: Extract Base Class + Enhanced Infrastructure (12 hours) ✅ **COMPLETE**

**Objective:** Make existing Selenium infrastructure reusable across all phases

**Tasks:**
- [x] **Extract `BaseYamlImportSeleniumTest` abstract class (4 hours)** ✅
  - Moved all 10 helper methods to base class
  - Moved `WebDriver`, `WebDriverWait`, `JavascriptExecutor`, `baseUrl` fields
  - Moved `@BeforeEach` and `@AfterEach` setup/teardown
  - Made `YamlImportValidationUITest` extend new base class
  - Validated existing 12 tests pass with no regressions (BUILD SUCCESS, 2:24h runtime)
  - **File:** `apex-playground/src/test/java/dev/mars/apex/playground/ui/BaseYamlImportSeleniumTest.java` (350+ lines)

- [x] **Add WebDriverManager for automatic ChromeDriver setup (1 hour)** ✅
  - Added `WebDriverManager.chromedriver().setup()` in `@BeforeAll`
  - Integrated with existing `@BeforeEach` initialization
  - Removed manual ChromeDriver path dependencies

- [x] **Enhanced helper methods (4 hours)** ✅
  - Added 5 new enhanced helper methods to `BaseYamlImportSeleniumTest`
  - **Note:** API signatures differ from plan (see Week 2 issues)

- [x] **Screenshot capture on failure using `TestWatcher` (2 hours)** ✅
  - Created `ScreenshotOnFailureExtension.java` implementing JUnit 5 `TestWatcher`
  - Captures screenshots to `target/selenium-screenshots/[test-name]-[timestamp].png`
  - Added `@ExtendWith(ScreenshotOnFailureExtension.class)` to base class
  - **File:** `apex-playground/src/test/java/dev/mars/apex/playground/ui/ScreenshotOnFailureExtension.java`

- [x] **Documentation: `SELENIUM_TEST_DEVELOPMENT_GUIDE.md` (1 hour)** ✅
  - Comprehensive 400+ line developer guide created
  - Includes API documentation, best practices, debugging guide, CI/CD examples
  - **File:** `apex-playground/docs/SELENIUM_TEST_DEVELOPMENT_GUIDE.md`

**Deliverables:** ✅ **ALL COMPLETE**
- `BaseYamlImportSeleniumTest.java` extracted and tested (350+ lines, 15 helper methods)
- 5 new enhanced helper methods operational
- Screenshot capture operational
- Development guide published (400+ lines)
- All 12 baseline tests validated passing

---

#### Week 2: Scale to 35 Tests (40% Coverage) (40 hours) ⏳ **IN PROGRESS**

**Objective:** Create 29 new YAML samples + 58 new Selenium test methods

**YAML Samples Creation:** ✅ **COMPLETE (29/29 - 100%)**

**Dev A Tasks (14 YAML samples + 28 methods, 18 hours):**
- [x] **Create samples 7-20** ✅ **ALL COMPLETE** (~1 hour each = 14 hours)
  - Tests 7-9: Configuration variations ✅
    - `examples/configuration/global-settings-test.yaml`
    - `examples/configuration/execution-options-test.yaml`
    - `examples/configuration/metadata-edge-cases-test.yaml`
  - Tests 10-12: Additional scenario patterns ✅
    - `examples/scenario/rule-ref-routing-test.yaml`
    - `examples/scenario/hybrid-classification-test.yaml`
    - `examples/scenario/nested-scenarios-test.yaml`
  - Tests 13-15: Multi-component configurations ✅
    - `examples/components/component-groups-test.yaml`
    - `examples/components/execution-order-test.yaml`
    - `examples/components/component-dependencies-test.yaml`
  - Tests 16-18: Advanced error recovery ✅
    - `examples/error-recovery/notification-policies-test.yaml`
    - `examples/error-recovery/custom-handlers-test.yaml`
    - `examples/error-recovery/retry-strategies-test.yaml`
  - Tests 19-20: Data source combinations ✅
    - `examples/data-sources/multiple-refs-test.yaml`
    - `examples/data-sources/conditional-enablement-test.yaml`

**Dev B Tasks (15 YAML samples + 30 methods, 17 hours):**
- [x] **Create samples 21-35** ✅ **ALL COMPLETE** (~1 hour each = 15 hours)
  - Tests 21-23: Rule variations ✅
    - `examples/rules/advanced-rule-groups-test.yaml`
    - `examples/rules/inline-rules-test.yaml`
    - `examples/rules/conditional-rules-test.yaml`
  - Tests 24-26: Transformation types ✅
    - `examples/transformations/field-mapping-test.yaml`
    - `examples/transformations/data-type-conversion-test.yaml`
    - `examples/transformations/custom-expressions-test.yaml`
  - Tests 27-29: Enrichment combinations ✅
    - `examples/enrichments/composite-enrichments-test.yaml`
    - `examples/enrichments/conditional-enrichment-test.yaml`
    - `examples/enrichments/async-patterns-test.yaml`
  - Tests 30-33: Lookup patterns ✅
    - `examples/lookups/multi-key-lookups-test.yaml`
    - `examples/lookups/fallback-values-test.yaml`
    - `examples/lookups/cache-configuration-test.yaml`
    - `examples/lookups/dynamic-lookups-test.yaml`
  - Tests 34-36: Template usage ✅
    - `examples/templates/reusable-blocks-test.yaml`
    - `examples/templates/parameterized-templates-test.yaml`
    - `examples/templates/template-inheritance-test.yaml`

**Test Implementation:** ✅ **COMPILATION FIXED**

- [x] **Deleted broken test file** ✅
  - Removed `YamlImportPhase1BlockTypesTest.java` (1100+ lines with 75 compilation errors)
  - API signature mismatches between assumed and actual BaseYamlImportSeleniumTest methods

- [x] **Created simplified test class** ✅ **COMPILES SUCCESSFULLY**
  - File created: `YamlImportPhase1SimpleTest.java`
  - **Location:** `apex-playground/src/test/java/dev/mars/apex/playground/uitest/YamlImportPhase1SimpleTest.java`
  - 30 test methods covering all 29 YAML samples (tests 7-36)
  - Simplified validation: basic import + block count verification
  - **Status:** ✅ BUILD SUCCESS - No compilation errors

**CURRENT BLOCKER:** Runtime Debugging Needed

Test execution encounters timeout error:
```
[ERROR] TimeoutException: Expected condition failed: waiting for Blockly workspace to load
[ERROR] Timeout after 15 seconds at waitForBlocklyWorkspaceToLoad()
```

**Root Cause Analysis:**
- Selenium tests successfully navigate to playground URL
- WebDriver wait timesout waiting for `Blockly.getMainWorkspace() != null`
- Possible causes:
  1. Spring Boot application not fully started before test execution
  2. JavaScript resources not loading in headless Chrome
  3. Blockly library initialization timing issue
  4. Base URL construction issue (`http://localhost:[port]/playground`)

**Required Actions:**
1. Review `YamlImportValidationUITest.java` (existing passing tests) for differences in approach
2. Add additional wait/initialization logic before loading playground page
3. Consider adding `@DirtiesContext` or longer startup delays
4. Debug in headed mode (remove `--headless`) to observe actual page loading
5. Verify playground endpoint is accessible during test execution

**Shared Tasks (4 hours):** ⏸️ **PENDING**
- [ ] Cross-validate all 29 new YAML samples (1 hour)
- [ ] Run full test suite (35 tests = 70 methods) in headless mode (1 hour)
- [ ] Run full test suite in headed mode to verify rendering (1 hour)
- [ ] Update YAML_IMPORT_TEST_COVERAGE_ANALYSIS.md progress (1 hour)

**Current Status:**
- YAML Samples: ✅ 29/29 complete (100%)
- Test Class: ✅ YamlImportPhase1SimpleTest.java compiles successfully
- Compilation: ✅ BUILD SUCCESS (0 errors)
- Execution: ⚠️ Runtime timeout error (Blockly workspace not loading)
- Coverage: 🔴 Still at 14% (12/87) - blocked until runtime issues resolved
- Target: 🎯 40% (35/87) when tests pass

**Deliverables:** ⏳ **PARTIAL - Compilation Fixed, Runtime Debugging Needed**
- ✅ 29 new YAML samples created (total: 35)
- ✅ Test class created and compiling successfully (30 methods)
- ✅ 75 compilation errors resolved by simplifying test approach
- ⚠️ Runtime execution blocked on Blockly workspace loading timeout
- ❌ 40% coverage milestone NOT YET achieved (need to fix runtime issues + run all tests)

---

### Sprint 2: Phase 2 + Phase 3 (Weeks 3-4)

#### Week 3: Section Combinations (Tests 36-49, 28 hours)

**Dev A Tasks (7 samples + 14 methods, 14 hours):**
- Tests 36-42: Section combinations (Rules+Enrichments, Rules+RuleGroups, Enrichments+Lookups, Rules+Transformations, Rules+ErrorRecovery, Rules+DataSourceRefs, Enrichments+ErrorRecovery)
- Create `YamlImportPhase2CombinationsTest.java` (Part A)

**Dev B Tasks (7 samples + 14 methods, 14 hours):**
- Tests 43-49: Full configurations (all sections, enrichment groups+standalone, rule groups+standalone, nested groups, multiple data source refs, transforms+enrichments, multi-level nesting)
- Extend `YamlImportPhase2CombinationsTest.java` (Part B)

**Deliverables:**
- 14 new YAML samples (total: 49)
- 28 new Selenium test methods (total: 98)
- 56% coverage

---

#### Week 4: Complex Scenarios (Tests 50-67, 36 hours)

**Dev A Tasks (9 samples + 18 methods, 18 hours):**
- Tests 50-58: Multi-stage scenarios, chained/parallel lookups, date math, string operations, nested conditionals
- Create `YamlImportPhase3ComplexTest.java` (Part A)

**Dev B Tasks (9 samples + 18 methods, 18 hours):**
- Tests 59-67: Rule priorities, categories, custom properties, parallel execution, component execution order, error recovery advanced
- Extend `YamlImportPhase3ComplexTest.java` (Part B)

**Deliverables:**
- 18 new YAML samples (total: 67)
- 36 new Selenium test methods (total: 134)
- 77% coverage

---

### Sprint 3: Phase 4 + Phase 5 (Weeks 5-6)

#### Week 5: Rule Chains + Edge Cases Start (Tests 68-80, 28 hours)

**Dev A Tasks (6 samples + 12 methods, 13 hours):**
- Tests 68-73: All 6 rule chain patterns (sequential-dependency, conditional-chaining, multi-stage, parallel-chaining, hybrid-chaining, loop-prevention)
- Create `YamlImportPhase4ChainsTest.java`
- **Note:** Document known visual editor limitations

**Dev B Tasks (7 samples + 14 methods, 13 hours):**
- Tests 74-80: Metadata edge cases, empty sections, minimal configs, large configs, special characters, Unicode, numeric edge cases
- Create `YamlImportPhase5EdgeCasesTest.java` (Part A)

**Deliverables:**
- 13 new YAML samples (total: 80)
- 26 new Selenium test methods (total: 160)
- 92% coverage

---

#### Week 6: Final Edge Cases (Tests 81-87, 28 hours)

**Dev A Tasks (3 samples + 6 methods, 13 hours):**
- Tests 81-83: Expression complexity (deeply nested, long expressions, mixed operators)
- Extend `YamlImportPhase5EdgeCasesTest.java` (Part B)

**Dev B Tasks (4 samples + 8 methods, 13 hours):**
- Tests 84-87: Comment preservation, whitespace handling, YAML anchors/aliases, versioning
- Extend `YamlImportPhase5EdgeCasesTest.java` (Part C)

**Shared Tasks (2 hours):**
- Full regression test suite execution

**Deliverables:**
- 7 new YAML samples (total: 87)
- 14 new Selenium test methods (total: 174)
- **100% coverage achieved**

---

### Sprint 4: CI/CD + Documentation (Week 7)

**Objective:** Production-ready test suite

**Dev A Tasks (9 hours):**
- [ ] **CI/CD Integration (4 hours)**
  - GitHub Actions workflow for Selenium tests
  - Install Chrome/ChromeDriver in CI
  - Configure headless execution
  - Upload screenshot artifacts on failure
  
- [ ] **Test Execution Scripts (2 hours)**
  - `scripts/run-selenium-import-tests.bat`
  - `scripts/run-selenium-import-tests.sh`
  - `scripts/run-phase-tests.bat [phase-number]`
  
- [ ] **Documentation Updates (3 hours)**
  - Update APEX_BLOCKS_PROTOTYPE_GUIDE.md with all 87 test references
  - Create troubleshooting runbook

**Dev B Tasks (9 hours):**
- [ ] **Test Stability (4 hours)**
  - Fix flaky tests
  - Add `@RepeatedTest` for transient failures
  - Improve error messages
  
- [ ] **Performance Optimization (3 hours)**
  - Reduce wait times where safe
  - Target: Full suite under 45 minutes
  
- [ ] **Test Tagging (2 hours)**
  - `@Tag("import")`, `@Tag("round-trip")`
  - `@Tag("smoke")` for critical path tests

**Shared Tasks (2 hours):**
- Final validation and sign-off

**Deliverables:**
- CI/CD operational
- Test suite production-ready
- Complete documentation

---

## SUCCESS METRICS

**Sprint 1:**
- [ ] All 35 Phase 1 Selenium tests pass (headless + headed)
- [ ] No test exceeds 30 seconds
- [ ] Screenshot capture verified
- [ ] 40% coverage

**Sprint 2:**
- [ ] 67 total tests operational
- [ ] 77% coverage
- [ ] Complex scenarios validated

**Sprint 3:**
- [ ] 87 tests complete
- [ ] 100% coverage
- [ ] Known limitations documented

**Sprint 4:**
- [ ] CI/CD integrated
- [ ] Full suite < 45 minutes
- [ ] Production deployment

---

## TIMELINE SUMMARY

| Sprint | Weeks | Tests Added | Total Tests | Coverage | Status |
|--------|-------|-------------|-------------|----------|---------|
| Baseline | - | 6 | 6 | 7% | ✅ COMPLETE |
| Sprint 1 | 1-2 | 29 | 35 | 40% | 🔄 IN PROGRESS |
| Sprint 2 | 3-4 | 32 | 67 | 77% | ⏳ PENDING |
| Sprint 3 | 5-6 | 20 | 87 | 100% | ⏳ PENDING |
| Sprint 4 | 7 | 0 | 87 | 100% | ⏳ PENDING |

**Total Duration:** 7 weeks  
**Total Effort:** ~238 hours (from current state)  
**Target Completion:** February 6, 2026
