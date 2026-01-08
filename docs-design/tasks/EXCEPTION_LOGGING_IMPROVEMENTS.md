# Exception Logging Improvements

## Problem Statement

Developers have reported that exception details are being swallowed and not available in debug logging. When exceptions are caught and logged with only `e.getMessage()`, the full stack trace is lost, making debugging extremely difficult.

## Current Anti-Pattern

```java
} catch (Exception e) {
    logger.warn("Failed to process: " + e.getMessage());
    // Stack trace is LOST!
}
```

## Correct Pattern

```java
} catch (Exception e) {
    logger.warn("Failed to process: {}", e.getMessage(), e);
    // Stack trace is preserved as the third parameter
}
```

Or for debug-level logging:

```java
} catch (Exception e) {
    logger.debug("Failed to process: {}", e.getMessage());
    logger.debug("Full exception details:", e);
    // Stack trace available when debug logging is enabled
}
```

## Files Requiring Fixes

### High Priority (Exception details completely lost)

1. **DatasetSignature.java** - 3 occurrences
   - Line 149: `logger.warn("Failed to hash inline data: " + e.getMessage());`
   - Line 208: `logger.warn("Failed to hash database config: " + e.getMessage());`
   - Line 246: `logger.warn("Failed to hash REST API config: " + e.getMessage());`

2. **DatasetLookupServiceFactory.java** - 7 occurrences
   - Line 333: YAML file reading failure
   - Line 340: YAML dataset loading failure
   - Line 404: CSV dataset loading failure
   - Line 490: File-system dataset loading failure
   - Line 531: JsonDataLoader fallback
   - Line 581: XmlDataLoader fallback
   - Line 634: CsvDataLoader failure

3. **ScenarioConfiguration.java** - 2 occurrences
   - Line 410: Classification rule evaluation failure
   - Line 450: Classification rule evaluation failure

4. **YamlEnrichmentProcessor.java** - 1 occurrence
   - Line 1920: Variable copying failure

5. **YamlTransformationProcessor.java** - 2 occurrences
   - Line 509: Get field value failure
   - Line 553: Set field value failure

6. **ContentClassifier.java** - 1 occurrence
   - Line 102: Content classification failure

7. **GenericTransformerService.java** - 2 occurrences (already has exception, but uses string concatenation)
   - Line 238: `logger.warn("Error transforming value: " + e.getMessage(), e);`
   - Should use: `logger.warn("Error transforming value: {}", e.getMessage(), e);`

8. **GenericTransformer.java** - 3 occurrences (same pattern)
   - Line 128, 156, 213: Mix of string concatenation with exception parameter

### Medium Priority (Exception details in some cases, but inconsistent)

9. **YamlDependencyService.java** - Multiple occurrences
   - Some use `(e)` parameter, some don't - need consistency

10. **YamlValidationService.java** - 1 occurrence
    - Line 198: Already includes exception parameter, but good to verify

11. **RestApiLookupService.java** - 3 occurrences (System.out.println - separate issue)
    - Lines with System.out.println need to be converted to proper logging

## Implementation Plan

### Phase 1: Add Exception Parameters (Immediate Fix)
All `logger.warn()` and `logger.error()` calls that catch exceptions MUST include the exception as the last parameter:

```java
logger.warn("Description: {}", e.getMessage(), e);
logger.error("Description: {}", e.getMessage(), e);
```

### Phase 2: Replace String Concatenation
Replace all `"text" + e.getMessage()` with parameterized logging:

```java
// Before
logger.warn("Failed: " + e.getMessage(), e);

// After
logger.warn("Failed: {}", e.getMessage(), e);
```

### Phase 3: Add Debug-Level Full Stack Traces
For expected exceptions or validation failures, add debug-level logging:

```java
} catch (ValidationException e) {
    logger.warn("Validation failed: {}", e.getMessage());
    logger.debug("Full validation exception details:", e);
}
```

## Benefits

1. **Full debugging context**: Stack traces available in logs
2. **Production flexibility**: Can enable debug logging when needed
3. **Performance**: No performance impact (exception only serialized if logging enabled)
4. **Developer productivity**: Faster problem diagnosis

## Testing

After changes:
1. Run all tests with debug logging enabled
2. Trigger known error scenarios
3. Verify stack traces appear in logs
4. Verify no performance degradation

## Related Issues

- Debug Logging Standardization (completed)
- RulesEngineLogger adoption (in progress)
- System.out/System.err elimination (in progress)

