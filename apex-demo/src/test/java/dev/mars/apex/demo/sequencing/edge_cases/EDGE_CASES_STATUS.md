# APEX Sequential Processing - Edge Cases Status

## Purpose
Track the status of edge case testing for APEX sequential processing and document order guarantee.

## Edge Cases Summary

### ✅ **ALREADY PROTECTED** (Existing Validation)

#### 1. ✅ Circular Group References
**Status**: PROTECTED by apex-core validation  
**Location**: `YamlConfigurationLoader.validateEnrichmentGroups()` (lines 1185-1206)  
**Algorithm**: Kahn's algorithm (topological sort) for cycle detection  
**Test**: `YamlEnrichmentValidationTest.shouldFailOnCyclicEnrichmentGroupReferences()`  
**Error Message**: `"Cyclic enrichment-group-references detected"`

**Example**:
```yaml
enrichment-groups:
  - id: g1
    enrichment-group-references: [ g2 ]
  - id: g2
    enrichment-group-references: [ g1 ]  # ❌ Circular!
```

---

#### 2. ✅ Self-Referencing Groups
**Status**: PROTECTED by apex-core validation  
**Location**: `YamlConfigurationLoader.validateEnrichmentGroups()` (line 1172-1174)  
**Test**: `YamlEnrichmentValidationTest.shouldFailOnSelfReferenceInEnrichmentGroup()`  
**Error Message**: `"Enrichment group 'X' cannot reference itself"`

**Example**:
```yaml
enrichment-groups:
  - id: self
    enrichment-group-references: [ self ]  # ❌ Self-reference!
```

---

#### 3. ✅ Duplicate IDs Across Numbered Sections
**Status**: PROTECTED by apex-core validation  
**Location**: `YamlConfigurationLoader.validateDuplicateEnrichmentIds()` (lines 2617-2631)  
**Test**: `TestEdge3_DuplicateIDsAcrossNumberedSectionsTest` (NEW - created today)  
**Error Message**: `"Duplicate enrichment ID found: duplicate-id"`  
**Note**: Validation runs AFTER numbered sections are merged, so duplicates are caught

**Example**:
```yaml
enrichments-1:
  - id: "duplicate-id"
enrichments-2:
  - id: "duplicate-id"  # ❌ Duplicate!
```

---

### 🔴 **NOT YET TESTED** (Need Investigation)

#### 4. ❓ ID Collision Between Inline and External Enrichments
**Status**: NEEDS TESTING  
**Risk**: Main.yaml and external.yaml both define enrichment with same ID, group references that ID  
**Question**: Which one wins? Does it break? Is there validation?

**Example**:
```yaml
# main.yaml
enrichments:
  - id: "enrich-1"
enrichment-refs:
  - "external.yaml"  # Also has "enrich-1"
enrichment-groups:
  - id: "group-A"
    enrichment-ids:
      - "enrich-1"  # ❓ Which one?
```

---

#### 5. ❓ Forward Reference to External Enrichments
**Status**: NEEDS TESTING  
**Risk**: enrichment-refs comes AFTER enrichment-groups that reference external items  
**Question**: Does reference resolution happen before group validation?

**Example**:
```yaml
enrichment-groups:
  - id: "group-A"
    enrichment-ids:
      - "external-1"  # ❓ Not loaded yet?
enrichment-refs:
  - "external.yaml"  # Contains "external-1"
```

---

#### 6. ❓ Multiple Reference Sections with Numbered Suffixes
**Status**: NEEDS TESTING  
**Risk**: Multiple `enrichment-refs` sections with numbered suffixes  
**Question**: Does placeholder expansion handle multiple refs sections?

**Example**:
```yaml
enrichment-refs:
  - "external-file-1.yaml"
enrichment-refs-1:  # ❓ Does this work?
  - "external-file-2.yaml"
```

---

#### 7. ❓ Empty Numbered Sections
**Status**: NEEDS TESTING  
**Risk**: Empty numbered sections mixed with populated ones  
**Question**: Does merge logic handle empty sections correctly?

**Example**:
```yaml
enrichments-1:
  # Empty!
enrichments-2:
  - id: "item-1"
enrichments-3:
  # Empty!
```

---

#### 8. ❓ Deeply Nested Group References (3+ levels)
**Status**: NEEDS TESTING  
**Risk**: Multi-level group references across multiple files  
**Question**: Does resolution work for 3+ levels?

**Example**:
```yaml
# file1.yaml: group-A -> group-B
# file2.yaml: group-B -> group-C
# file3.yaml: group-C -> actual-enrichment
```

---

#### 9. ❓ Disabled Groups with Enabled Items
**Status**: NEEDS TESTING  
**Risk**: Enrichment is enabled but its group is disabled  
**Question**: Does groups-only logic still apply?

**Example**:
```yaml
enrichments:
  - id: "grouped-1"
    enabled: true
enrichment-groups:
  - id: "group-A"
    enabled: false  # ❓ Does grouped-1 execute at definition position?
    enrichment-ids:
      - "grouped-1"
```

---

### 🔵 **FEATURE GAPS** (May Not Exist)

#### 10. ❓ Transformations with Groups-Only Logic
**Status**: NEEDS INVESTIGATION  
**Question**: Do transformation-groups exist? Does groups-only logic apply?

---

#### 11. ❓ Rule-Chains with Groups-Only Logic
**Status**: NEEDS INVESTIGATION  
**Question**: Do rule-chain-groups exist? Does groups-only logic apply?

---

#### 12. ❓ Pipeline with Numbered Suffixes
**Status**: NEEDS INVESTIGATION  
**Question**: Does pipeline support numbered suffixes (pipeline-1, pipeline-2)?

---

## Test Coverage Summary

| Priority | Edge Case | Status | Test Location |
|----------|-----------|--------|---------------|
| HIGH | Circular group references | ✅ PROTECTED | apex-core/YamlEnrichmentValidationTest |
| HIGH | Self-referencing groups | ✅ PROTECTED | apex-core/YamlEnrichmentValidationTest |
| HIGH | Duplicate IDs across numbered sections | ✅ PROTECTED | apex-demo/edge_cases/TestEdge3 |
| HIGH | ID collision (inline vs external) | ❓ NEEDS TEST | - |
| HIGH | Forward reference to external items | ❓ NEEDS TEST | - |
| MEDIUM | Multiple refs with numbered suffixes | ❓ NEEDS TEST | - |
| MEDIUM | Empty numbered sections | ❓ NEEDS TEST | - |
| MEDIUM | Deeply nested group references | ❓ NEEDS TEST | - |
| MEDIUM | Disabled groups with enabled items | ❓ NEEDS TEST | - |
| LOW | Transformations with groups | ❓ NEEDS INVESTIGATION | - |
| LOW | Rule-chains with groups | ❓ NEEDS INVESTIGATION | - |
| LOW | Pipeline with numbered suffixes | ❓ NEEDS INVESTIGATION | - |

---

## Next Steps

1. **HIGH PRIORITY**: Test ID collision between inline and external enrichments (#4)
2. **HIGH PRIORITY**: Test forward reference to external enrichments (#5)
3. **MEDIUM PRIORITY**: Test multiple reference sections with numbered suffixes (#6)
4. **MEDIUM PRIORITY**: Test empty numbered sections (#7)
5. **MEDIUM PRIORITY**: Test deeply nested group references (#8)
6. **MEDIUM PRIORITY**: Test disabled groups with enabled items (#9)
7. **LOW PRIORITY**: Investigate transformation-groups and rule-chain-groups features (#10, #11)
8. **LOW PRIORITY**: Test pipeline with numbered suffixes (#12)

---

## Key Findings

### ✅ **Excellent Protection Already in Place**
- **Circular dependencies**: Detected using Kahn's algorithm (topological sort)
- **Self-references**: Explicitly checked and rejected
- **Duplicate IDs**: Validated after numbered sections are merged
- **Missing references**: Validated before group creation

### 🎯 **Areas Needing More Testing**
- **Cross-file ID collisions**: Not yet tested
- **Reference ordering**: Not yet tested (refs before/after groups)
- **Complex numbered suffix scenarios**: Not yet tested
- **Disabled group behavior**: Not yet clarified

### 📚 **Documentation Needed**
- Clear rules for ID collision resolution (inline vs external)
- Reference resolution order guarantees
- Disabled group behavior specification

