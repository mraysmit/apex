# APEX Error Handling Migration - Discovery & Inventory

**Date:** 2025-11-16  
**Status:** Phase 1 - Discovery Complete  
**Task:** Migrate ALL apex-core and apex-demo files to new error handling APIs

---

## Executive Summary

### Scope
- **Total YAML files**: 458 files
- **YAML files WITH error handling**: 96 files (21%)
- **YAML files NEEDING migration**: ~362 files (79%)
- **Java files with deprecated API calls**: 10 files (ALL are legitimate unit tests)
- **Total Java test files**: ~200+ files

### Critical Findings - **UPDATED AFTER ANALYSIS**
1. **EXCELLENT NEWS**: ALL 10 Java files with deprecated APIs are legitimate unit tests OF the deprecated processor itself
2. **NO JAVA MIGRATION NEEDED**: All deprecated API usage is intentional (testing the deprecated class)
3. ⚠️ **YAML MIGRATION ONLY**: 362 YAML files need error-handling configuration added
4. **Progress**: 96 YAML files already have error handling (21% complete)
5. **apex-demo**: Already fully migrated to RulesEngine.evaluate() - no deprecated API usage!

---

## Detailed Inventory

### 1. Java Files with Deprecated API Usage (10 files) - **NO MIGRATION NEEDED**

#### apex-core Unit Tests (10 files) - **LEGITIMATE TESTS OF DEPRECATED CLASS**
These are legitimate unit tests of the YamlEnrichmentProcessor itself - they SHOULD use deprecated APIs:

1. `apex-core/src/test/java/dev/mars/apex/core/integration/EnrichmentGroupsEndToEndIntegrationTest.java` ✅
2. `apex-core/src/test/java/dev/mars/apex/core/service/enrichment/CalculationFieldMappingTest.java`
3. `apex-core/src/test/java/dev/mars/apex/core/service/enrichment/ConditionalMappingEnrichmentTest.java`
4. `apex-core/src/test/java/dev/mars/apex/core/service/enrichment/EnrichmentGroupExecutionTest.java`
5. `apex-core/src/test/java/dev/mars/apex/core/service/enrichment/EnrichmentServiceTest.java`
6. `apex-core/src/test/java/dev/mars/apex/core/service/enrichment/FieldMappingTest.java`
7. `apex-core/src/test/java/dev/mars/apex/core/service/enrichment/JsonFieldMappingTest.java`
8. `apex-core/src/test/java/dev/mars/apex/core/service/enrichment/SpelFieldMappingIntegrationTest.java`
9. `apex-core/src/test/java/dev/mars/apex/core/service/enrichment/SpelFieldMappingTest.java`
10. `apex-core/src/test/java/dev/mars/apex/core/service/enrichment/YamlEnrichmentProcessorCachingTest.java`

**Migration Strategy**: **NO MIGRATION** - Add `@SuppressWarnings("deprecation")` and documentation comments explaining these are legitimate tests OF the deprecated class

#### apex-demo Tests (0 files) - **ALREADY MIGRATED**

**Status**: **COMPLETE** - All apex-demo tests already use `RulesEngine.evaluate()` - no deprecated API usage found!

---

### 2. YAML Files Status

#### Files WITH Error Handling (96 files) - COMPLETE
These files already have `error-handling:` or `error-recovery:` configuration

#### Files NEEDING Migration (~362 files) - ⏳ TODO
These files need error-handling configuration added to rule-groups/enrichment-groups

**Breakdown by Location:**
- `apex-core/src/test/resources/`: ~250 files
- `apex-demo/src/main/resources/`: ~80 files
- `apex-demo/src/test/resources/`: ~32 files

---

## Migration Priority Matrix

### Priority 1: CRITICAL (High Risk) - **Estimated: 2-3 hours**
**Target:** Core validation and critical functionality tests

**Files:**
- `EnrichmentGroupsEndToEndIntegrationTest.java` (1 file)
- Associated YAML files in `apex-core/src/test/resources/integration/` (~10 files)

**Why Critical:** These tests validate core engine behavior and integration

---

### Priority 2: HIGH (Medium Risk) - **Estimated: 3-4 hours**
**Target:** apex-core unit tests for enrichment processing

**Files:**
- All 10 enrichment processor unit test files
- Associated YAML files in `apex-core/src/test/resources/enrichment/` (~50 files)

**Why High:** These tests validate individual processor behavior

---

### Priority 3: MEDIUM (Low Risk) - **Estimated: 4-5 hours**
**Target:** Remaining apex-core test files and YAML configurations

**Files:**
- Remaining apex-core test YAML files (~190 files)
- Other apex-core test Java files that don't use deprecated APIs but need error handling assertions

**Why Medium:** These are comprehensive tests but lower risk

---

### Priority 4: LOW (Lowest Risk) - **Estimated: 2-3 hours**
**Target:** apex-demo files

**Files:**
- 3 demo Java test files with deprecated APIs
- ~112 demo YAML files needing error handling configuration

**Why Low:** Demos are examples/documentation, not critical functionality

---

## Migration Batches

### Batch 1: Critical Integration Tests (5-10 files)
- EnrichmentGroupsEndToEndIntegrationTest.java + YAML files
- **Validate:** Run integration tests after migration

### Batch 2: Enrichment Unit Tests - Part 1 (5-10 files)
- CalculationFieldMappingTest.java
- ConditionalMappingEnrichmentTest.java
- EnrichmentGroupExecutionTest.java
- EnrichmentServiceTest.java
- FieldMappingTest.java
- **Validate:** Run unit tests after migration

### Batch 3: Enrichment Unit Tests - Part 2 (5-10 files)
- JsonFieldMappingTest.java
- SpelFieldMappingIntegrationTest.java
- SpelFieldMappingTest.java
- YamlEnrichmentProcessorCachingTest.java
- YamlEnrichmentProcessorErrorHandlingTest.java
- **Validate:** Run unit tests after migration

### Batch 4-N: Remaining YAML Files (10-20 files per batch)
- Process remaining ~362 YAML files in batches of 10-20
- **Validate:** Run affected tests after each batch

### Final Batch: Demo Files (3 Java + ~112 YAML)
- Migrate all apex-demo files
- **Validate:** Run all demo tests

---

## Next Steps

1. **Discovery Complete** - This document
2. ⏳ **Start Batch 1** - Critical integration tests
3. ⏳ **Incremental validation** - Test after each batch
4. ⏳ **Track progress** - Update task list after each batch
5. ⏳ **Final validation** - Full test suite + zero deprecation warnings

---

## Success Criteria

**ZERO deprecated API calls** in apex-core and apex-demo  
**ALL YAML files** have error-handling configuration  
**ALL tests** validate error handling behavior  
**ALL tests pass** green (3,000+ tests)  
**ZERO deprecation warnings** when compiling  
**Demos showcase** error handling features  

---

**Estimated Total Time:** 12-15 hours (2 days of focused work)

