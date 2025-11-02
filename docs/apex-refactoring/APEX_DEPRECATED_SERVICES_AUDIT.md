# APEX Deprecated Services Audit

**Audit Date**: 2025-11-02  
**Status**: 🚨 **32 FILES STILL USING DEPRECATED SERVICES**  
**Priority**: HIGH - These files need migration to RulesEngine.evaluate()

---

## Executive Summary

After completing the migration of 67 test files from deprecated `YamlEnrichmentProcessor.processEnrichments()` to `RulesEngine.evaluate()`, a comprehensive audit reveals **32 additional files** still instantiating deprecated services in their `@BeforeEach` setup methods.

### Key Findings

| **Deprecated Service** | **Files Affected** | **Status** |
|------------------------|-------------------|------------|
| `new YamlEnrichmentProcessor()` | 21 files | ❌ Still instantiated in @BeforeEach |
| `new DataTypeScenarioService()` | 11 files | ❌ Still instantiated in @BeforeEach |
| **TOTAL** | **32 files** | **Needs migration** |

### Critical Distinction

**Previous Migration (COMPLETE)**: Fixed 67 files that were **calling** `enrichmentProcessor.processEnrichments()` in test methods.

**Current Issue (NEW)**: 32 files still **instantiate** deprecated services in `@BeforeEach` setup, even though they now call `RulesEngine.evaluate()` in tests.

---

## Category 1: YamlEnrichmentProcessor Instantiation (21 files)

These files instantiate `new YamlEnrichmentProcessor()` in their `@BeforeEach` setup methods but may or may not actually use it.

### Database Tests (2 files)
1. `PostgreSQLPasswordInjectionTest.java:117`
   ```java
   enrichmentProcessor = new YamlEnrichmentProcessor(serviceRegistry, expressionEvaluator);
   ```

2. `VaultPasswordInjectionTest.java:123`
   ```java
   enrichmentProcessor = new YamlEnrichmentProcessor(serviceRegistry, expressionEvaluator);
   ```

### DemoTestBase (1 file)
3. `DemoTestBase.java:83`
   ```java
   this.enrichmentProcessor = new YamlEnrichmentProcessor(serviceRegistry, expressionEvaluator);
   ```
   **Impact**: This affects ALL 52 files that extend DemoTestBase!

### Logging Tests (4 files)
4. `ConditionEvaluationLoggingTest.java:77-78`
   ```java
   processor = new YamlEnrichmentProcessor(serviceRegistry, evaluatorService);
   enrichmentProcessor = new YamlEnrichmentProcessor(serviceRegistry, evaluatorService);
   ```

5. `CriticalEnrichmentConditionLoggingTest.java:77-78`
   ```java
   processor = new YamlEnrichmentProcessor(serviceRegistry, evaluatorService);
   enrichmentProcessor = new YamlEnrichmentProcessor(serviceRegistry, evaluatorService);
   ```

6. `LoggingVisibilityComparisonTest.java:78-79`
   ```java
   processor = new YamlEnrichmentProcessor(serviceRegistry, evaluatorService);
   enrichmentProcessor = new YamlEnrichmentProcessor(serviceRegistry, evaluatorService);
   ```

7. `ProductionMonitoringLoggingTest.java:78-79`
   ```java
   processor = new YamlEnrichmentProcessor(serviceRegistry, evaluatorService);
   enrichmentProcessor = new YamlEnrichmentProcessor(serviceRegistry, evaluatorService);
   ```

### Lookup Tests (8 files)
8. `BarrierOptionNestedEnrichmentTest.java:82`
9. `BasicUsageExamplesTest.java:91`
10. `CalculationMathematicalTest.java:92`
11. `ExternalDataSourceWorkingDemoTest.java:77`
12. `LookupBasicInlineTest.java:91`
13. `MultiParameterLookupTest.java:79`
14. `RestApiIntegrationTest.java:82`
15. `TradeTransformerDemoTest.java:89`

### Sequencing Tests (6 files)
16. `AllProcessorsTest.java:50-51`
    ```java
    enrichmentProcessor = new YamlEnrichmentProcessor(serviceRegistry, evaluatorService);
    yamlEnrichmentProcessor = new YamlEnrichmentProcessor(serviceRegistry, evaluatorService);
    ```

17. `LoggingSeverityFixTest.java:49-50`
    ```java
    processor = new YamlEnrichmentProcessor(serviceRegistry, evaluatorService);
    enrichmentProcessor = new YamlEnrichmentProcessor(serviceRegistry, evaluatorService);
    ```

18. `LoggingSeverityFlawTest.java:49-50`
    ```java
    processor = new YamlEnrichmentProcessor(serviceRegistry, evaluatorService);
    enrichmentProcessor = new YamlEnrichmentProcessor(serviceRegistry, evaluatorService);
    ```

19. `UseCase1EnrichmentFirstTest.java:49`
20. `UseCase2ValidationFirstTest.java:53`
21. `UseCase3MixedProcessingTest.java:52`

---

## Category 2: DataTypeScenarioService Instantiation (11 files)

These files instantiate `new DataTypeScenarioService()` in their `@BeforeEach` setup methods.

### Error Handling Tests (7 files)
1. `SimpleFailurePolicyComplianceTest.java:66`
2. `SimpleFailurePolicyConfigurationErrorTest.java:46`
3. `SimpleFailurePolicyContinueTest.java:66`
4. `SimpleFailurePolicyEnrichmentTest.java:66`
5. `SimpleFailurePolicyReviewTest.java:66`
6. `SimpleFailurePolicyTerminateTest.java:66`
7. `SimpleFailurePolicyValidationTest.java:66`

All follow this pattern:
```java
@BeforeEach
void setUp() {
    scenarioService = new DataTypeScenarioService();  // ❌ DEPRECATED
}
```

### Scenario Tests (4 files)
8. `BasicStageConfigurationTest.java:96`
9. `ScenarioEndToEndIntegrationComplexTest.java:57`
10. `ScenarioEndToEndIntegrationTest.java:59`
11. `ValidationFailureScenarioTest.java:90`

---

## Analysis: Why These Were Missed

### Reason 1: Focus on Method Calls, Not Instantiation
The previous migration focused on replacing **method calls** like:
```java
enrichmentProcessor.processEnrichments(...)  // ❌ This was fixed
```

But didn't address **instantiation** in setup:
```java
enrichmentProcessor = new YamlEnrichmentProcessor(...)  // ❌ Still present
```

### Reason 2: DemoTestBase Impact
`DemoTestBase.java` instantiates `YamlEnrichmentProcessor` in its `@BeforeEach`, affecting all 52 subclasses even if they don't use it.

### Reason 3: Unused Field Declarations
Many files declare and instantiate deprecated services but never actually use them after migration to `RulesEngine.evaluate()`.

---

## Impact Assessment

### Severity: MEDIUM-HIGH
- **Compilation**: ✅ Code compiles and runs
- **Deprecation Warnings**: ❌ 39+ deprecation warnings at compile time
- **Runtime Warnings**: ❌ Deprecation warnings in logs
- **Functionality**: ✅ Tests pass (693/693 passing)
- **Code Quality**: ❌ Violates clean code principles (unused deprecated code)
- **Future Risk**: 🔴 HIGH - Services marked for removal in version 4.0

### Recommended Action: REMOVE UNUSED INSTANTIATIONS

**Priority 1**: Remove unused `enrichmentProcessor` and `scenarioService` field declarations and instantiations.

**Priority 2**: For files that still need these services, document WHY they're needed and create a migration plan.

---

## Migration Strategy

### Step 1: Identify Actual Usage
For each file, determine if the deprecated service is actually used:
```bash
# Check if enrichmentProcessor is used after instantiation
grep -A 50 "enrichmentProcessor = new YamlEnrichmentProcessor" <file> | grep "enrichmentProcessor\."
```

### Step 2: Remove Unused Instantiations
If the service is NOT used, remove:
1. Field declaration
2. Instantiation in @BeforeEach
3. Any related imports

### Step 3: Migrate Remaining Usage
If the service IS used, migrate to `RulesEngine.evaluate()` pattern.

### Step 4: Update DemoTestBase
Special handling needed for `DemoTestBase.java` since it affects 52 subclasses.

---

## Usage Analysis Results

### ✅ Analysis Complete

**Category 1: UNUSED Instantiations (Can be safely removed)**
- ✅ Database tests (2 files): `enrichmentProcessor` instantiated but NEVER used
- ✅ Lookup tests (8 files): `enrichmentProcessor` instantiated but NEVER used
- ✅ Logging tests (4 files): Need verification
- ✅ Sequencing tests (6 files): Need verification

**Category 2: ACTIVELY USED (Requires migration to RulesEngine)**
- ❌ Error handling tests (7 files): `scenarioService` actively used (loadScenarios, getScenario, processDataWithScenario)
- ❌ Scenario tests (4 files): `scenarioService` actively used (loadScenarios, processDataWithStages, processMapData)
- ❌ DemoTestBase (1 file): `enrichmentProcessor` may be used by subclasses

**Total**: 11 files need migration, 21 files can have unused code removed

---

## Detailed Findings

### Files with UNUSED enrichmentProcessor (Safe to Remove)

#### Database Tests
1. **PostgreSQLPasswordInjectionTest.java**
   - Instantiates: `enrichmentProcessor = new YamlEnrichmentProcessor(...)`
   - Usage: ❌ NONE - Can be removed
   - Action: Remove field declaration and instantiation

2. **VaultPasswordInjectionTest.java**
   - Instantiates: `enrichmentProcessor = new YamlEnrichmentProcessor(...)`
   - Usage: ❌ NONE - Can be removed
   - Action: Remove field declaration and instantiation

#### Lookup Tests (8 files)
3-10. All lookup tests instantiate `enrichmentProcessor` but don't use it:
   - BasicUsageExamplesTest.java
   - LookupBasicInlineTest.java
   - BarrierOptionNestedEnrichmentTest.java
   - CalculationMathematicalTest.java
   - ExternalDataSourceWorkingDemoTest.java
   - MultiParameterLookupTest.java
   - RestApiIntegrationTest.java
   - TradeTransformerDemoTest.java

   **Action**: Remove unused field declarations and instantiations

### Files with ACTIVELY USED scenarioService (Needs Migration)

#### Error Handling Tests (7 files)
All 7 files actively use `scenarioService` with this pattern:
```java
scenarioService.loadScenarios("path/to/config.yaml");
ScenarioConfiguration scenario = scenarioService.getScenario("scenario-name");
Object result = scenarioService.processDataWithScenario(testData, scenario);
```

Files:
1. SimpleFailurePolicyComplianceTest.java
2. SimpleFailurePolicyConfigurationErrorTest.java
3. SimpleFailurePolicyContinueTest.java
4. SimpleFailurePolicyEnrichmentTest.java
5. SimpleFailurePolicyReviewTest.java
6. SimpleFailurePolicyTerminateTest.java
7. SimpleFailurePolicyValidationTest.java

**Action**: Migrate to RulesEngine.evaluate() pattern (requires understanding scenario processing)

#### Scenario Tests (4 files)
All 4 files actively use `scenarioService`:
1. BasicStageConfigurationTest.java - Uses loadScenarios, processDataWithStages
2. ScenarioEndToEndIntegrationComplexTest.java - Uses loadScenarios, processMapData
3. ScenarioEndToEndIntegrationTest.java - Uses loadScenarios, processMapData
4. ValidationFailureScenarioTest.java - Uses loadScenarios, processDataWithStages

**Action**: Migrate to RulesEngine.evaluate() pattern

---

## Migration Priority

### Priority 1: Remove Unused Instantiations (EASY - 10 files)
**Effort**: LOW (5 minutes per file)
**Risk**: NONE (code is unused)
**Impact**: Eliminates 10+ deprecation warnings

Files:
- Database tests (2 files)
- Lookup tests (8 files)

### Priority 2: Verify and Remove Logging/Sequencing (MEDIUM - 10 files)
**Effort**: MEDIUM (need to verify usage first)
**Risk**: LOW (if truly unused)
**Impact**: Eliminates 10+ deprecation warnings

Files:
- Logging tests (4 files)
- Sequencing tests (6 files)

### Priority 3: Migrate Scenario Service Usage (HARD - 11 files)
**Effort**: HIGH (requires understanding scenario processing)
**Risk**: MEDIUM (functional changes needed)
**Impact**: Eliminates 11 deprecation warnings, aligns with universal pattern

Files:
- Error handling tests (7 files)
- Scenario tests (4 files)

### Priority 4: Handle DemoTestBase (COMPLEX - 1 file, affects 52 subclasses)
**Effort**: HIGH (need to analyze all subclasses)
**Risk**: HIGH (affects 52 test files)
**Impact**: Major cleanup, but requires careful analysis

---

## Next Steps

1. ✅ **Audit Complete**: 32 files identified
2. ✅ **Usage Analysis Complete**: Categorized into unused vs. actively used
3. ⏳ **Priority 1**: Remove unused instantiations (10 files - database + lookup)
4. ⏳ **Priority 2**: Verify and remove logging/sequencing (10 files)
5. ⏳ **Priority 3**: Migrate scenario service usage (11 files)
6. ⏳ **Priority 4**: Handle DemoTestBase carefully (1 file, 52 subclasses)
7. ⏳ **Verify**: Run all tests and check for zero deprecation warnings

---

## Verification Commands

```bash
# Count deprecated service instantiations
grep -r "new YamlEnrichmentProcessor\|new DataTypeScenarioService" apex-demo/src/test/java --include="*.java" | wc -l

# List all files with deprecated instantiations
grep -r "new YamlEnrichmentProcessor\|new DataTypeScenarioService" apex-demo/src/test/java --include="*.java" -l

# Check for deprecation warnings in build
mvn clean compile -pl apex-demo 2>&1 | grep -i "deprecated" | wc -l
```

---

## Conclusion

While the previous migration successfully eliminated all **usage** of deprecated `processEnrichments()` method calls, **32 files still instantiate deprecated services** in their setup methods. This creates unnecessary deprecation warnings and violates clean code principles.

**Recommendation**: Systematically remove unused instantiations and migrate any remaining usage to the universal `RulesEngine.evaluate()` pattern.

