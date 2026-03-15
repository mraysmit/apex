# Step 3: Section Registry Pattern - Implementation Plan

## 🎯 Objective
Replace string allocation and splitting logic in `mergeNumberedSections()` with O(1) map lookups to reduce GC pressure at high frequency (1000s req/sec).

---

## Current Problem

### Location
`OrderedYamlParser.mergeNumberedSections()`

### Current Implementation (Inefficient)
```java
// For EVERY numbered section found:
for (String sectionName : orderedMap.keySet()) {
    String normalizedName = normalizeSectionName(sectionName);  // String regex + replaceAll
    
    if (sectionName.matches(".*-\\d+$")) {  // Regex evaluation
        String baseName = sectionName.replaceAll("-\\d+$", "");  // String allocation
        
        if (NUMBERED_SUFFIX_SECTIONS.contains(baseName)) {
            // More string operations...
        }
    }
}
```

### Performance Issues
At 1000 requests/sec with multiple numbered sections per request:
- **String allocations**: `replaceAll()` creates new strings on every call
- **Regex evaluations**: Pattern matching on every section name
- **GC pressure**: "Death by 1000 cuts" - temporary objects constantly created/destroyed

---

## Solution: Explicit Section Registry

### Concept
Replace dynamic string parsing with pre-computed lookup table:

**Old Way**: `if (key.endsWith("-2")) ...` (String operations)  
**New Way**: `Strategy strategy = registry.get(key);` (O(1) reference lookup)

---

## 🔧 Implementation Steps

### Step 3.1: Create SectionRegistry Class

**New File**: `apex-core/src/main/java/dev/mars/apex/core/config/yaml/SectionRegistry.java`

```java
package dev.mars.apex.core.config.yaml;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * PERFORMANCE OPTIMIZATION: Pre-computed registry for section name lookups.
 * 
 * Replaces string allocation/regex evaluation with O(1) map lookups.
 * 
 * Previously: For each section, perform regex match + string replace
 * Now: Single map lookup to get normalized name + merge strategy
 * 
 * Thread-safe singleton pattern for concurrent high-frequency access.
 * 
 * @see apex_architecture_and_code_review.md - Section 3
 */
public class SectionRegistry {
    
    private static final SectionRegistry INSTANCE = new SectionRegistry();
    
    // Known base section names
    private static final Set<String> BASE_SECTIONS = Set.of(
        "enrichments", "rules", "enrichment-groups", "rule-groups",
        "transformations", "rule-chains", "enrichment-refs", "rule-refs"
    );
    
    // Cache of section name → normalized name
    private final Map<String, String> normalizedCache = new ConcurrentHashMap<>(64);
    
    // Cache of section name → merge strategy
    private final Map<String, MergeStrategy> strategyCache = new ConcurrentHashMap<>(64);
    
    // Compiled pattern for numbered suffixes (compiled once, reused)
    private static final Pattern NUMBERED_SUFFIX_PATTERN = Pattern.compile("^(.+)-(\\d+)$");
    
    private SectionRegistry() {
        // Singleton - private constructor
        precomputeCommonSections();
    }
    
    public static SectionRegistry getInstance() {
        return INSTANCE;
    }
    
    /**
     * Pre-compute lookups for common section patterns.
     * Reduces first-request overhead.
     */
    private void precomputeCommonSections() {
        // Pre-cache base sections
        for (String base : BASE_SECTIONS) {
            normalizedCache.put(base, base);
            strategyCache.put(base, MergeStrategy.BASE_SECTION);
            
            // Pre-cache common numbered variations (1-10)
            for (int i = 1; i <= 10; i++) {
                String numbered = base + "-" + i;
                normalizedCache.put(numbered, base);
                strategyCache.put(numbered, MergeStrategy.NUMBERED_SECTION);
            }
        }
    }
    
    /**
     * Get normalized section name (O(1) after first lookup).
     * 
     * @param sectionName Original section name (e.g., "enrichments-2")
     * @return Normalized name (e.g., "enrichments")
     */
    public String getNormalizedName(String sectionName) {
        return normalizedCache.computeIfAbsent(sectionName, this::computeNormalizedName);
    }
    
    /**
     * Get merge strategy for section (O(1) after first lookup).
     */
    public MergeStrategy getMergeStrategy(String sectionName) {
        return strategyCache.computeIfAbsent(sectionName, this::computeMergeStrategy);
    }
    
    /**
     * Compute normalized name (called only once per unique section name).
     */
    private String computeNormalizedName(String sectionName) {
        java.util.regex.Matcher matcher = NUMBERED_SUFFIX_PATTERN.matcher(sectionName);
        if (matcher.matches()) {
            String baseName = matcher.group(1);
            if (BASE_SECTIONS.contains(baseName)) {
                return baseName;
            }
        }
        return sectionName;
    }
    
    /**
     * Compute merge strategy (called only once per unique section name).
     */
    private MergeStrategy computeMergeStrategy(String sectionName) {
        java.util.regex.Matcher matcher = NUMBERED_SUFFIX_PATTERN.matcher(sectionName);
        if (matcher.matches() && BASE_SECTIONS.contains(matcher.group(1))) {
            return MergeStrategy.NUMBERED_SECTION;
        }
        return MergeStrategy.BASE_SECTION;
    }
    
    /**
     * Clear caches (for testing).
     */
    void clearCaches() {
        normalizedCache.clear();
        strategyCache.clear();
        precomputeCommonSections();
    }
    
    /**
     * Strategy for handling section merging.
     */
    public enum MergeStrategy {
        BASE_SECTION,      // Standard section, no merging needed
        NUMBERED_SECTION   // Numbered suffix, needs merging into base
    }
}
```

### Step 3.2: Update OrderedYamlParser to Use Registry

**File**: `apex-core/src/main/java/dev/mars/apex/core/config/yaml/OrderedYamlParser.java`

**Changes**:

1. **Add registry reference**:
```java
public class OrderedYamlParser {
    
    private static final Logger logger = LoggerFactory.getLogger(OrderedYamlParser.class);
    private static final ObjectMapper YAML_MAPPER = createYamlMapper();
    
    // STEP 3: Section Registry for O(1) lookups
    private static final SectionRegistry SECTION_REGISTRY = SectionRegistry.getInstance();
    
    // ...existing code...
}
```

2. **Replace normalizeSectionName() method**:
```java
// OLD METHOD (Delete):
private String normalizeSectionName(String sectionName) {
    if (sectionName == null) return null;
    
    if (sectionName.matches(".*-\\d+$")) {  // REGEX + STRING ALLOCATION
        String baseName = sectionName.replaceAll("-\\d+$", "");  // MORE ALLOCATION
        if (NUMBERED_SUFFIX_SECTIONS.contains(baseName)) {
            return baseName;
        }
    }
    return sectionName;
}

// NEW METHOD (Replace with):
private String normalizeSectionName(String sectionName) {
    if (sectionName == null) return null;
    return SECTION_REGISTRY.getNormalizedName(sectionName);  // O(1) LOOKUP
}
```

3. **Optimize mergeNumberedSections()**:
```java
private void mergeNumberedSections(Map<String, Object> orderedMap, YamlRuleConfiguration config) {
    Map<String, List<Object>> sectionsToMerge = new LinkedHashMap<>();
    
    // OPTIMIZED: Use registry instead of string operations
    for (Map.Entry<String, Object> entry : orderedMap.entrySet()) {
        String sectionName = entry.getKey();
        
        // O(1) lookup instead of regex + string operations
        MergeStrategy strategy = SECTION_REGISTRY.getMergeStrategy(sectionName);
        
        if (strategy == MergeStrategy.NUMBERED_SECTION) {
            String normalizedName = SECTION_REGISTRY.getNormalizedName(sectionName);
            Object sectionValue = entry.getValue();
            
            if (sectionValue instanceof List) {
                sectionsToMerge.computeIfAbsent(normalizedName, k -> new ArrayList<>())
                              .addAll((List<?>) sectionValue);
                              
                logger.info("Found numbered section '{}' with {} items to merge into '{}'",
                           sectionName, ((List<?>) sectionValue).size(), normalizedName);
            }
        }
    }
    
    // Merge collected items into base sections
    for (Map.Entry<String, List<Object>> entry : sectionsToMerge.entrySet()) {
        mergeIntoBaseSection(entry.getKey(), entry.getValue(), config);
    }
}
```

### Step 3.3: Create Unit Tests

**New File**: `apex-core/src/test/java/dev/mars/apex/core/config/yaml/SectionRegistryTest.java`

```java
package dev.mars.apex.core.config.yaml;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SectionRegistryTest {
    
    private SectionRegistry registry;
    
    @BeforeEach
    void setUp() {
        registry = SectionRegistry.getInstance();
        registry.clearCaches(); // Start fresh for each test
    }
    
    @Test
    @DisplayName("Should normalize numbered sections to base name")
    void testNormalizeNumberedSections() {
        assertEquals("enrichments", registry.getNormalizedName("enrichments-1"));
        assertEquals("enrichments", registry.getNormalizedName("enrichments-2"));
        assertEquals("rules", registry.getNormalizedName("rules-10"));
        assertEquals("transformations", registry.getNormalizedName("transformations-99"));
    }
    
    @Test
    @DisplayName("Should return original name for base sections")
    void testBaseSections() {
        assertEquals("enrichments", registry.getNormalizedName("enrichments"));
        assertEquals("rules", registry.getNormalizedName("rules"));
        assertEquals("metadata", registry.getNormalizedName("metadata"));
    }
    
    @Test
    @DisplayName("Should identify numbered section strategy")
    void testMergeStrategy() {
        assertEquals(SectionRegistry.MergeStrategy.NUMBERED_SECTION, 
                    registry.getMergeStrategy("enrichments-1"));
        assertEquals(SectionRegistry.MergeStrategy.BASE_SECTION, 
                    registry.getMergeStrategy("enrichments"));
    }
    
    @Test
    @DisplayName("Should cache lookups for performance")
    void testCaching() {
        // First lookup - computes
        String result1 = registry.getNormalizedName("enrichments-100");
        
        // Second lookup - should return cached value
        String result2 = registry.getNormalizedName("enrichments-100");
        
        assertEquals(result1, result2);
        assertSame(result1, result2); // Same object reference = cached
    }
}
```

---

## 📈 Expected Performance Impact

### Before (Current)
```
Per numbered section:
- Regex pattern matching: ~500ns
- String replaceAll allocation: ~200ns  
- String comparison: ~100ns
Total per section: ~800ns

At 1000 req/sec with 5 numbered sections each:
- 5000 regex evaluations/sec
- 5000 string allocations/sec
- Significant GC pressure
```

### After (Registry)
```
Per numbered section:
- Map lookup (cached): ~10ns
- No allocations
Total per section: ~10ns

At 1000 req/sec with 5 numbered sections each:
- 5000 O(1) lookups/sec
- Zero allocations
- Minimal GC pressure
```

**Performance Gain**: ~98% reduction in section normalization overhead

---

## Testing Strategy

1. **Unit Tests**: `SectionRegistryTest` validates caching and lookups
2. **Integration Tests**: Run existing 2108 tests - should all pass
3. **Performance Tests**: Measure GC pause time before/after at high load

---

## 🎯 Success Criteria

- All 2108 tests pass
- No new string allocations in hot path
- Map lookup time < 20ns average
- GC pause time reduced at 1000 req/sec load
- Backward compatible (same API)

---

## 📝 Implementation Checklist

- [ ] Create `SectionRegistry.java` with caching logic
- [ ] Create `SectionRegistryTest.java` with unit tests
- [ ] Update `OrderedYamlParser.normalizeSectionName()` to use registry
- [ ] Update `OrderedYamlParser.mergeNumberedSections()` to use registry
- [ ] Run full test suite (verify 2108/2108 passing)
- [ ] Commit with performance benchmarks
- [ ] Update `CRITICAL_PERFORMANCE_REFACTORING_COMPLETE.md`

---

**Estimated Time**: 1-2 hours  
**Risk Level**: LOW (isolated change, easy to revert)  
**Priority**: MEDIUM (optimization, not critical like Steps 1-2)

**Current Status**: Steps 1-2 COMPLETE (all 2108 tests passing)

This can be done now or deferred after merging Steps 1-2 to master.

---

## Reference: Architectural Review Extract

From `docs/design/apex_architecture_and_code_review.md`:

> ### 3. Allocation Rate in Numbered Sections
> **Location**: `mergeNumberedSections`
> **Impact**: **GC Pressure**.
> The string splitting and list merging logic generates significant temporary object checking (Splitting keys, creating new ArrayLists) on every request. While functional, at 1000s req/sec, this creates "Death by 1000 cuts" for the Garbage Collector.
> **Fix**: The **Explicit Section Registry** (outlined below) replaces string allocs with O(1) map lookups.

> ### Strategic Optimizations
> **Explicit Section Registry**: Move from dynamic string parsing to a looked-up Registry.
> - **Old Way**: `if (key.endsWith("-2")) ...` (String allocs)
> - **New Way**: `Strategy s = registry.get(key);` (Reference lookup)
> This aligns perfectly with the dynamic high-frequency model by minimizing per-request CPU work.

