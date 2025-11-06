# YAML Ordering Test Coverage Plan

## Executive Summary

This document tracks comprehensive test coverage for YAML sequential processing in APEX.
The goal is to "test it until it breaks" by creating every possible YAML ordering variation.

**Current Status**: 21/21 tests passing (Priority 1 complete)
**Next Steps**: Implement Priority 2 and Priority 3 tests

---

## ✅ Priority 1: Reference Expansion Ordering Tests (6/6 COMPLETE)

### Test 1.1: Enrichment-Refs Placeholder Expansion ✅
- **File**: `EnrichmentRefsSequentialOrderTest.java` / `EnrichmentRefsSequentialOrderTest.yaml`
- **YAML Order**: `enrichments → enrichment-refs → rules`
- **Tests**: 3/3 passing
- **Proves**: 
  - Enrichment-refs loads external enrichments correctly
  - Placeholder expansion works for enrichment-refs
  - External enrichments execute in correct position

### Test 1.2: Rule-Refs Placeholder Expansion ✅
- **File**: `RuleRefsSequentialOrderTest.java` / `RuleRefsSequentialOrderTest.yaml`
- **YAML Order**: `enrichments → rule-refs → rules`
- **Tests**: 3/3 passing
- **Proves**: 
  - Rule-refs loads external rules correctly
  - Placeholder expansion works for rule-refs
  - External rules execute in correct position

### Test 1.3a: Enrichment-Refs BEFORE Inline Enrichments ✅
- **File**: `EnrichmentRefsBeforeInlineTest.java` / `EnrichmentRefsBeforeInlineTest.yaml`
- **YAML Order**: `enrichment-refs → enrichments → rules`
- **Tests**: 4/4 passing
- **Proves**: 
  - Enrichment-refs expands BEFORE inline enrichments
  - External enrichments execute before inline enrichments
  - Dependency chain works: E1-ref → E2-ref → E3-inline

### Test 1.3b: Rule-Refs BEFORE Inline Rules ✅
- **File**: `RuleRefsBeforeInlineTest.java` / `RuleRefsBeforeInlineTest.yaml`
- **YAML Order**: `enrichments → rule-refs → rules`
- **Tests**: 4/4 passing
- **Proves**: 
  - Rule-refs expands BEFORE inline rules
  - External rules execute before inline rules
  - Dependency chain works: E1 → R1-ref → R2-ref → R3-inline

### Test 1.3c: Both Refs - Enrichment-Refs First ✅
- **File**: `BothRefsEnrichmentFirstTest.java` / `BothRefsEnrichmentFirstTest.yaml`
- **YAML Order**: `enrichment-refs → enrichments → rule-refs → rules`
- **Tests**: 3/3 passing
- **Proves**: 
  - Both enrichment-refs AND rule-refs work in same file
  - Enrichment-refs expands BEFORE rule-refs
  - All enrichments execute BEFORE all rules
  - Processing: E1-ref → E2-ref → E3-inline → R1-ref → R2-ref → R3-inline

### Test 1.3d: Both Refs - Rule-Refs First ✅
- **File**: `BothRefsRuleFirstTest.java` / `BothRefsRuleFirstTest.yaml`
- **YAML Order**: `rule-refs → enrichment-refs` (REFS-ONLY!)
- **Tests**: 4/4 passing
- **Proves**: 
  - Both rule-refs AND enrichment-refs work in same file
  - Rule-refs expands BEFORE enrichment-refs
  - Rules execute BEFORE enrichments (unusual but valid)
  - **CRITICAL**: REFS-ONLY configurations work correctly
  - Processing: R1-ref → R2-ref → E1-ref → E2-ref

---

## 🔄 Priority 2: Group Ordering Tests (0/4 PENDING)

### Test 2.1: Enrichment-Group-Refs Placeholder Expansion
- **File**: `EnrichmentGroupRefsSequentialOrderTest.java` / `.yaml` (TO CREATE)
- **YAML Order**: `enrichments → enrichment-group-refs → rules`
- **Expected Processing**: E1-inline → EG1-ref → EG2-ref → R1-inline
- **Proves**: 
  - Enrichment-group-refs loads external enrichment groups correctly
  - Placeholder expansion works for enrichment-group-refs
  - External enrichment groups execute in correct position

### Test 2.2: Rule-Group-Refs Placeholder Expansion
- **File**: `RuleGroupRefsSequentialOrderTest.java` / `.yaml` (TO CREATE)
- **YAML Order**: `enrichments → rule-group-refs → rules`
- **Expected Processing**: E1-inline → RG1-ref → RG2-ref → R1-inline
- **Proves**: 
  - Rule-group-refs loads external rule groups correctly
  - Placeholder expansion works for rule-group-refs
  - External rule groups execute in correct position

### Test 2.3: Mixed Groups and Items - Enrichments
- **File**: `MixedEnrichmentGroupsAndItemsTest.java` / `.yaml` (TO CREATE)
- **YAML Order**: `enrichment-refs → enrichments → enrichment-group-refs → enrichment-groups`
- **Expected Processing**: E1-ref → E2-ref → E3-inline → EG1-ref → EG2-ref → EG3-inline
- **Proves**: 
  - Individual enrichments and enrichment groups can coexist
  - Refs expand before inline items for both enrichments and groups
  - Complex interleaving works correctly

### Test 2.4: Mixed Groups and Items - Rules
- **File**: `MixedRuleGroupsAndItemsTest.java` / `.yaml` (TO CREATE)
- **YAML Order**: `rule-refs → rules → rule-group-refs → rule-groups`
- **Expected Processing**: R1-ref → R2-ref → R3-inline → RG1-ref → RG2-ref → RG3-inline
- **Proves**: 
  - Individual rules and rule groups can coexist
  - Refs expand before inline items for both rules and groups
  - Complex interleaving works correctly

---

## 🎯 Priority 3: Complex Multi-Section Tests (0/2 PENDING)

### Test 3.1: All Section Types in Document Order
- **File**: `AllSectionTypesSequentialTest.java` / `.yaml` (TO CREATE)
- **YAML Order**: `enrichment-refs → enrichments → enrichment-group-refs → enrichment-groups → rule-refs → rules → rule-group-refs → rule-groups`
- **Expected Processing**: 
  - E1-ref → E2-ref → E3-inline → EG1-ref → EG2-ref → EG3-inline → 
  - R1-ref → R2-ref → R3-inline → RG1-ref → RG2-ref → RG3-inline
- **Proves**: 
  - ALL section types work together in one file
  - All refs expand in correct positions
  - Complex multi-section processing works correctly
  - **ULTIMATE TEST**: Maximum complexity scenario

### Test 3.2: Reverse Order - Rules Before Enrichments
- **File**: `RulesBeforeEnrichmentsTest.java` / `.yaml` (TO CREATE)
- **YAML Order**: `rule-refs → rules → rule-group-refs → rule-groups → enrichment-refs → enrichments → enrichment-group-refs → enrichment-groups`
- **Expected Processing**: 
  - R1-ref → R2-ref → R3-inline → RG1-ref → RG2-ref → RG3-inline → 
  - E1-ref → E2-ref → E3-inline → EG1-ref → EG2-ref → EG3-inline
- **Proves**: 
  - Rules can execute BEFORE enrichments (unusual but valid)
  - Sequential processing respects document order even when unusual
  - Backward compatibility with unusual configurations

---

## 📊 Test Coverage Summary

### Completed Tests
| Priority | Category | Tests | Status |
|----------|----------|-------|--------|
| 1 | Reference Expansion | 6 | ✅ 21/21 passing |
| 2 | Group Ordering | 4 | ⏳ Pending |
| 3 | Complex Multi-Section | 2 | ⏳ Pending |
| **TOTAL** | | **12** | **21/21 passing** |

### Coverage Analysis

**What IS Covered (Priority 1):**
- ✅ Enrichment-refs placeholder expansion
- ✅ Rule-refs placeholder expansion
- ✅ Enrichment-refs BEFORE inline enrichments
- ✅ Rule-refs BEFORE inline rules
- ✅ Both refs together (enrichment-refs first)
- ✅ Both refs together (rule-refs first)
- ✅ REFS-ONLY configurations (no inline items)

**What is NOT Yet Covered (Priority 2 & 3):**
- ❌ Enrichment-group-refs placeholder expansion
- ❌ Rule-group-refs placeholder expansion
- ❌ Mixed groups and individual items (enrichments)
- ❌ Mixed groups and individual items (rules)
- ❌ All section types together (ultimate complexity test)
- ❌ Reverse order (rules before enrichments)

**Current Coverage**: ~50% of planned test scenarios
**Target Coverage**: 100% (12/12 tests)

---

## 🎯 Implementation Guidelines

### External Files Needed

For Priority 2 tests, create:
- `external-enrichment-groups-otc.yaml` - Contains 2 enrichment groups
- `external-rule-groups-otc.yaml` - Contains 2 rule groups

### Test Pattern to Follow

All tests should follow the established pattern:
1. **Test 1**: Verify items loaded from external files
2. **Test 2**: CRITICAL - Verify placeholder expansion and execution order
3. **Test 3**: Verify specific edge cases or failure scenarios
4. **Test 4** (optional): Additional verification tests

### Logging Requirements

All tests must include:
- Clear section markers (`═══════════════════════════════════════`)
- Input data logging
- Enriched data logging
- Execution order verification logging
- Success/failure status logging

### Assertion Requirements

All tests must verify:
- Correct number of items loaded
- Correct execution order (via enriched data state)
- Correct placeholder expansion (via logs)
- No regressions in existing functionality

---

## 🔍 Critical Findings from Priority 1

### Finding 1: apex-core Rebuild Required
- **Issue**: New methods not available in compiled classes
- **Root Cause**: apex-core not rebuilt after sequential processing implementation
- **Fix**: `mvn clean install -pl apex-core -DskipTests`
- **Impact**: All tests now use item-level processing correctly

### Finding 2: REFS-ONLY Configurations Work
- **Discovery**: YAML files with ONLY refs (no inline items) work correctly
- **Evidence**: BothRefsRuleFirstTest proves this
- **Implication**: No need to add inline items to trigger placeholder expansion

### Finding 3: Item-Level Processing Confirmed
- **Evidence**: Logs show "Processing X items in document order"
- **Previous**: Logs showed "Processing X sections in document order"
- **Verification**: All tests now show item-level processing

---

## 📝 Next Steps

1. **Immediate**: Create external group files
   - `external-enrichment-groups-otc.yaml`
   - `external-rule-groups-otc.yaml`

2. **Priority 2**: Implement group ordering tests (4 tests)
   - Test 2.1: Enrichment-Group-Refs Placeholder Expansion
   - Test 2.2: Rule-Group-Refs Placeholder Expansion
   - Test 2.3: Mixed Groups and Items - Enrichments
   - Test 2.4: Mixed Groups and Items - Rules

3. **Priority 3**: Implement complex multi-section tests (2 tests)
   - Test 3.1: All Section Types in Document Order
   - Test 3.2: Reverse Order - Rules Before Enrichments

4. **Final**: Run full test suite to ensure green baseline maintained

---

## 🎓 Lessons Learned

1. **Always rebuild after core changes**: apex-core must be rebuilt when adding new methods
2. **Test refs-only scenarios**: Don't assume inline items are required
3. **Read logs carefully**: Exit codes don't tell the full story
4. **Follow existing patterns**: Copy from working examples, don't reinvent
5. **Test incrementally**: Test after every change, don't batch changes
6. **Verify item-level processing**: Check logs for "Processing X items" not "Processing X sections"

---

## 📚 References

- **Implementation**: 8-phase sequential processing fix (completed)
- **Core Files**: 
  - `OrderedYamlParser.java` - Extracts item order with placeholders
  - `YamlConfigurationLoader.java` - Expands placeholders after reference processing
  - `RulesEngine.java` - Executes items in document order
  - `ProcessingItem.java` - Represents single processing item
- **Test Files**: `apex-demo/src/test/java/dev/mars/apex/demo/sequencing/`
- **External Files**: `external-enrichments-otc.yaml`, `external-rules-otc.yaml`

---

**Document Version**: 1.0  
**Last Updated**: 2025-11-06  
**Status**: Priority 1 Complete (21/21 tests passing)  
**Next Milestone**: Priority 2 Implementation (4 tests)

