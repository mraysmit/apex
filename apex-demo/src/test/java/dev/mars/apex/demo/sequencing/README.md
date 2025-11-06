# APEX Sequential Processing Test Suite

## Overview

This directory contains comprehensive tests for YAML sequential processing in APEX.
The tests verify that YAML sections and items are processed in **document order** (the order they appear in the YAML file).

## Quick Status

**Current Status**: ✅ 21/21 tests passing (Priority 1 complete)

```
Priority 1: Reference Expansion Ordering    ✅ 6/6 tests (21 test methods)
Priority 2: Group Ordering Tests            ⏳ 0/4 tests (pending)
Priority 3: Complex Multi-Section Tests     ⏳ 0/2 tests (pending)
```

## Test Files

### ✅ Completed Tests (Priority 1)

| Test File | YAML Order | Purpose |
|-----------|------------|---------|
| `EnrichmentRefsSequentialOrderTest` | `enrichments → enrichment-refs → rules` | Enrichment-refs expansion |
| `RuleRefsSequentialOrderTest` | `enrichments → rule-refs → rules` | Rule-refs expansion |
| `EnrichmentRefsBeforeInlineTest` | `enrichment-refs → enrichments → rules` | Refs before inline (enrichments) |
| `RuleRefsBeforeInlineTest` | `enrichments → rule-refs → rules` | Refs before inline (rules) |
| `BothRefsEnrichmentFirstTest` | `enrichment-refs → enrichments → rule-refs → rules` | Both refs (enrichment first) |
| `BothRefsRuleFirstTest` | `rule-refs → enrichment-refs` | Both refs (rule first, refs-only) |

### 🔄 Pending Tests (Priority 2)

| Test File | YAML Order | Purpose |
|-----------|------------|---------|
| `EnrichmentGroupRefsSequentialOrderTest` | `enrichments → enrichment-group-refs → rules` | Enrichment-group-refs expansion |
| `RuleGroupRefsSequentialOrderTest` | `enrichments → rule-group-refs → rules` | Rule-group-refs expansion |
| `MixedEnrichmentGroupsAndItemsTest` | `enrichment-refs → enrichments → enrichment-group-refs → enrichment-groups` | Mixed groups and items |
| `MixedRuleGroupsAndItemsTest` | `rule-refs → rules → rule-group-refs → rule-groups` | Mixed groups and items |

### 🎯 Pending Tests (Priority 3)

| Test File | YAML Order | Purpose |
|-----------|------------|---------|
| `AllSectionTypesSequentialTest` | All 8 section types in order | Ultimate complexity test |
| `RulesBeforeEnrichmentsTest` | Rules before enrichments | Reverse order test |

## External Reference Files

### Current Files
- `external-enrichments-otc.yaml` - 2 enrichments for OTC options
- `external-rules-otc.yaml` - 2 rules for OTC options

### Needed for Priority 2
- `external-enrichment-groups-otc.yaml` - 2 enrichment groups (TO CREATE)
- `external-rule-groups-otc.yaml` - 2 rule groups (TO CREATE)

## Running Tests

### Run all sequencing tests
```bash
mvn test -Dtest="EnrichmentRefsSequentialOrderTest,RuleRefsSequentialOrderTest,EnrichmentRefsBeforeInlineTest,RuleRefsBeforeInlineTest,BothRefsEnrichmentFirstTest,BothRefsRuleFirstTest" -pl apex-demo
```

### Run individual test
```bash
mvn test -Dtest=BothRefsEnrichmentFirstTest -pl apex-demo
```

### Run full test suite
```bash
mvn test -pl apex-demo
```

## Key Concepts

### Sequential Processing
- **Mode**: Set `processing-mode: "sequential"` in YAML metadata
- **Behavior**: Process sections/items in the order they appear in YAML
- **Default**: Standard mode (hardcoded order: enrichments → rules)

### Item-Level vs Section-Level Processing
- **Item-Level**: Process individual items (E1 → E2 → R1 → R2) - **NEW**
- **Section-Level**: Process entire sections (all enrichments → all rules) - **OLD**
- **Verification**: Check logs for "Processing X items in document order"

### Placeholder Expansion
- **Concept**: Insert placeholders during parsing, expand after reference loading
- **Example**: `ProcessingItem("enrichment-refs", "*")` → `ProcessingItem("enrichments", "E1")`, `ProcessingItem("enrichments", "E2")`
- **Location**: `YamlConfigurationLoader.expandReferencePlaceholders()`

## Test Pattern

All tests follow this pattern:

```java
@Test
@DisplayName("Test 1: Verify items loaded")
void testItemsLoaded() {
    // Load configuration
    // Verify correct number of items loaded
    // Verify items have correct IDs
}

@Test
@DisplayName("Test 2: CRITICAL - Verify execution order")
void testExecutionOrder() {
    // Load configuration
    // Execute with test data
    // Verify enriched data shows correct execution order
    // Log detailed verification
}

@Test
@DisplayName("Test 3: Verify edge cases")
void testEdgeCases() {
    // Test failure scenarios
    // Test unusual data
    // Verify error handling
}
```

## Critical Findings

### 1. apex-core Rebuild Required
After making changes to apex-core, always rebuild:
```bash
mvn clean install -pl apex-core -DskipTests
```

### 2. REFS-ONLY Configurations Work
YAML files with ONLY refs (no inline items) work correctly.
See `BothRefsRuleFirstTest.yaml` for example.

### 3. Item-Level Processing Confirmed
All tests now show item-level processing in logs:
```
Processing 4 items in document order
```

## Debugging Tips

### Check if item-level processing is active
Look for this log line:
```
Processing X items in document order
```

If you see this instead, item-level processing is NOT active:
```
Processing X sections in document order
```

### Verify placeholder expansion
Look for these log lines:
```
Extracted 2 items in document order
Expanded item order from 2 to 4 items
```

### Check itemOrder contents
Use the debug pattern from `DebugItemOrderTest` (deleted after use):
```java
List<ProcessingItem> itemOrder = config.getItemOrder();
for (ProcessingItem item : itemOrder) {
    logger.info("[{}] {} : {}", i, item.getSectionType(), item.getItemId());
}
```

## Common Issues

### Issue: Methods not found (getItemOrder, etc.)
**Solution**: Rebuild apex-core
```bash
mvn clean install -pl apex-core -DskipTests
```

### Issue: Tests show section-level processing
**Solution**: Check if apex-core was rebuilt after sequential processing implementation

### Issue: Placeholder not expanding
**Solution**: Check logs for "Expanded item order from X to Y items"

## Documentation

- **Test Plan**: `YAML_ORDERING_TEST_PLAN.md` - Comprehensive test coverage plan
- **Implementation**: See apex-core source files:
  - `OrderedYamlParser.java`
  - `YamlConfigurationLoader.java`
  - `RulesEngine.java`
  - `ProcessingItem.java`

## Contributing

When adding new tests:
1. Follow the established test pattern
2. Use OTC options domain for test data
3. Include detailed logging with section markers
4. Verify both success and failure scenarios
5. Update this README and the test plan

## Contact

For questions about sequential processing tests, refer to:
- `YAML_ORDERING_TEST_PLAN.md` - Detailed test plan
- `prompts.txt` - Implementation principles
- Existing test files - Working examples

