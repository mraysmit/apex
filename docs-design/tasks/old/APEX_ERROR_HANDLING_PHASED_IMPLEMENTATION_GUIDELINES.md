# Phase Implementation Guidelines
**Complete Guide for Implementing Error Handling Phases**

**Date:** 2025-11-15
**Source:** Day 1-3 Implementation Experience + Compliance Analysis

---

## Day 1 Compliance Summary

### What Was Implemented
- `processTransformationsWithResult(List<YamlTransformation>, Object)` → RuleResult
- `processTransformationWithResult(YamlTransformation, Object)` → RuleResult
- Legacy `processTransformations()` deprecated with migration guidance
- All methods track errors in `RuleResult.failureMessages`
- All methods set `RuleResult.resultType = ERROR` on failures
- All methods return `RuleResult.match()` on success
- 4/4 unit tests passing, BUILD SUCCESS

### Error Handling Compliance
- Transformation errors return `RuleResult.error()` (not thrown exceptions)
- Errors logged as `logger.error("CRITICAL: ...")` (not warnings)
- Severity set to `SeverityConstants.ERROR`
- Fail-fast behavior: returns error immediately on first failure
- Lower-level methods throw exceptions, outer methods catch and convert
- No exception swallowing: all errors propagated to caller
- Condition evaluation failures log warnings and skip transformation (graceful degradation)

### 🔴 Critical Bugs Fixed
**Bug 1: Exception Swallowing**
- Problem: Lower-level methods caught exceptions and returned original object
- Impact: Errors were lost, callers couldn't detect failures
- Fix: Changed catch blocks to re-throw as `RuntimeException`

**Bug 2: Test Design Issue**
- Problem: Tests used enrichment SpEL syntax (`#amount`) instead of transformation syntax
- Fix: Updated to use `sourceField` + `#value` pattern

### 📚 Root Cause: Why Tests Were Not Created First
- Day 1 code implemented WITHOUT tests (VIOLATION of prompts.txt line 451)
- User challenged: "are you following the principles in prompts.txt?"
- Tests created and revealed exception swallowing bug
- Writing tests FIRST would have caught the bug immediately
- **Commitment:** ALL future implementation will follow "Write Tests First"

---

## Pre-Implementation Checklist

### Before Writing Any Code
- [ ] Read the implementation plan requirements for the day
- [ ] Read error handling principles from `prompts.txt` (lines 455-614)
- [ ] Identify which error type: Configuration Error vs Business Logic Failure
- [ ] Review decision matrix (prompts.txt lines 602-614)
- [ ] Check for existing patterns in the codebase
- [ ] **WRITE TESTS FIRST** (prompts.txt line 451) - this is MANDATORY

### Test-First Development (MANDATORY)
- [ ] Create test file BEFORE implementation file
- [ ] Write all required unit tests (from implementation plan)
- [ ] Tests should FAIL initially (proving they test the right thing)
- [ ] Implement code to make tests pass
- [ ] Verify all tests pass before marking task complete
- [ ] **Lesson from Day 1:** Tests reveal bugs that code review misses

---

## Implementation Requirements

### Method Signatures
- [ ] Add `*WithResult()` methods that return `RuleResult`
- [ ] Keep legacy methods for backward compatibility
- [ ] Mark legacy methods as `@Deprecated(since = "X.X", forRemoval = true)`
- [ ] Add comprehensive JavaDoc with migration guidance
- [ ] Example: `processTransformationsWithResult()` (Day 1 Lines 110-163)

### Error Tracking
- [ ] Track errors in `RuleResult.failureMessages`
- [ ] Set `RuleResult.resultType = ERROR` on failures
- [ ] Set `RuleResult.resultType = MATCH` on success
- [ ] Use `SeverityConstants.ERROR` for business logic failures
- [ ] Use `SeverityConstants.WARNING` for configuration errors
- [ ] Use `SeverityConstants.INFO` for informational messages
- [ ] Example: Day 1 Lines 141-145, 215-219

### Exception Propagation Pattern (CRITICAL)
- [ ] Lower-level methods THROW exceptions (don't swallow them)
- [ ] Outer methods CATCH exceptions and convert to `RuleResult.error()`
- [ ] Use `logger.error("CRITICAL: ...")` for business logic failures
- [ ] Use `logger.warn()` for configuration errors
- [ ] Never use "log and continue" pattern for business logic failures
- [ ] Example: Day 1 Lines 260-279 (throws), Lines 137-146 (catches)

---

## Error Handling Decision Matrix

### Business Logic Failures → Return Error Results
**Examples:**
- SpEL expression evaluation failed
- Transformation processing error
- Required enrichment failed
- Database connection failed
- Rule evaluation errors

**Implementation Pattern (from Day 1):**
```java
// Lower-level method: THROW exception (Lines 289-322)
private Object processFieldTransformation(...) {
    try {
        // ... transformation logic ...
    } catch (Exception e) {
        logger.error("CRITICAL: Field transformation failed: {}", id, e.getMessage(), e);
        throw new RuntimeException("Field transformation failed: " + e.getMessage(), e);  // THROW
    }
}

// Outer method: CATCH and convert to RuleResult.error() (Lines 137-146)
public RuleResult processTransformationsWithResult(...) {
    try {
        transformedObject = processTransformation(transformation, transformedObject);  // ← Can throw
    } catch (Exception e) {
        logger.error("CRITICAL: Transformation failed: {}", transformation.getId(), e.getMessage(), e);
        return RuleResult.error(
            "transformation:" + transformation.getId(),
            "Transformation processing failed: " + e.getMessage(),
            SeverityConstants.ERROR
        );
    }
}
```

### Configuration Errors → Graceful Degradation
**Examples:**
- Missing optional fields
- Invalid field mappings (user can fix)
- Condition evaluation failures
- Validation failures (user can correct)

**Implementation Pattern (from Day 1 Lines 237-247):**
```java
try {
    Boolean result = conditionExpr.getValue(context, Boolean.class);
    return result != null && result;
} catch (Exception e) {
    logger.warn("Failed to evaluate transformation condition for {}: {}",
        transformation.getId(), e.getMessage());
    return false;  // Continue with default (skip transformation)
}
```

---

## Testing Requirements

### Unit Tests (Per Day - 4 Required)
- [ ] Test 1: `*WithResult()` method returns `RuleResult`
- [ ] Test 2: Errors tracked in `RuleResult.failureMessages`
- [ ] Test 3: `RuleResult.resultType = ERROR` on errors
- [ ] Test 4: Successful operations return `RuleResult.match()`
- [ ] Test exception propagation works correctly
- [ ] Test all error scenarios (null values, invalid expressions, etc.)

### Test Verification (from Day 1)
- [ ] Run tests: `mvn test -Dtest=<TestClassName> -pl apex-core`
- [ ] Verify: `Tests run: 4, Failures: 0, Errors: 0, Skipped: 0`
- [ ] Verify: `BUILD SUCCESS`
- [ ] Check IDE for warnings/errors (use diagnostics tool)
- [ ] Review test output logs for unexpected errors

---

## Common Pitfalls to Avoid (from Day 1 Bugs)

### Exception Swallowing (Day 1 Bug #1)
```java
// WRONG: Swallows exception (original Day 1 bug)
} catch (Exception e) {
    logger.error("Failed", e);
    return targetObject;  // Appears successful, error lost
}
```

### Exception Re-Throwing (Day 1 Fix)
```java
// CORRECT: Re-throws exception
} catch (Exception e) {
    logger.error("CRITICAL: Failed", e);
    throw new RuntimeException("Failed: " + e.getMessage(), e);  // Propagates
}
```

### Wrong Error Type Classification
```java
// WRONG: Treating business logic failure as configuration error
} catch (SpelEvaluationException e) {
    logger.warn("Expression failed, continuing...");  // Should be ERROR
    return defaultValue;
}
```

### Correct Error Type Classification
```java
// CORRECT: Business logic failure throws exception
} catch (SpelEvaluationException e) {
    logger.error("CRITICAL: Expression failed", e);  // ERROR
    throw new RuntimeException("Expression failed", e);
}
```

---

## Post-Implementation Checklist

### Code Review
- [ ] All required methods implemented
- [ ] Exception propagation working correctly (no swallowing)
- [ ] Error handling principles followed (decision matrix)
- [ ] Legacy methods properly deprecated
- [ ] No IDE warnings or errors
- [ ] Code follows existing patterns

### Testing (MANDATORY)
- [ ] All unit tests passing (4/4, not 3/4)
- [ ] Tests definitively prove correct behavior
- [ ] No test failures, errors, or skipped tests
- [ ] Test coverage meets requirements (100%)
- [ ] Exception propagation verified by tests

### Documentation
- [ ] JavaDoc complete with migration guidance
- [ ] Deprecation warnings clear and actionable
- [ ] Code comments explain complex logic
- [ ] Update any affected documentation files

---

## Day Completion Criteria

**A day is NOT complete until:**
- All required methods implemented
- All required tests passing (4/4, not 3/4)
- No IDE warnings or errors
- Exception propagation verified
- Error handling principles followed
- Documentation complete
- Code reviewed against guidelines

**If any criterion is not met, the day is NOT complete.**

---

## Key Lessons from Day 1-3

1. **Write Tests First** - Tests reveal bugs that code review misses (Day 1 exception swallowing bug)
2. **Exception Propagation** - Lower methods throw, outer methods catch and convert (Day 1 Lines 260-279, 137-146)
3. **Error Type Classification** - Use decision matrix to determine handling strategy (prompts.txt lines 602-614)
4. **No Exception Swallowing** - Always re-throw or convert to RuleResult.error() (Day 1 Bug #1)
5. **Verify Everything** - Run tests, check logs, review IDE warnings (Day 1: 4/4 tests passing)
6. **Follow Patterns** - Look at existing code for established patterns (Day 1 implementation)
7. **Document Thoroughly** - Future developers need clear migration guidance (Day 1 JavaDoc)

---

## Requirements Traceability (Day 1 Example)

### Implementation Plan → Code → Tests
- Add `processTransformationsWithResult()` → Lines 110-163 → Test 1 (lines 45-66)
- Track errors in `RuleResult.failureMessages` → Lines 141-145 → Test 2 (lines 72-115)
- Set `RuleResult.resultType = ERROR` → Lines 141-145 → Test 3 (lines 121-148)
- Return `RuleResult.match()` on success → Lines 157-162 → Test 4 (lines 152-179)

### Error Handling Principles → Code
- Business Logic Failures → Return Error Results → Lines 137-146, 213-219
- Exception Propagation Pattern → Lines 260-279, 289-322
- Configuration Errors → Graceful Degradation → Lines 237-247

---

## Final Verification (Day 1 Results)

```
[INFO] Running dev.mars.apex.core.service.transformation.YamlTransformationProcessorRuleResultTest
[INFO] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

**Status:** COMPLETE AND FULLY COMPLIANT

