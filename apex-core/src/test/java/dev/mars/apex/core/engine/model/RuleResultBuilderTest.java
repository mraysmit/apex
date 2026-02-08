package dev.mars.apex.core.engine.model;

import dev.mars.apex.core.constants.SeverityConstants;
import dev.mars.apex.core.service.monitoring.RulePerformanceMetrics;
import dev.mars.apex.core.test.extension.ColoredTestOutputExtension;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/*
 * Copyright 2025 Mark Andrew Ray-Smith Cityline Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * Tests for RuleResult.Builder (Phase 1 refactoring).
 * Validates:
 *  1. Fluent builder API constructs valid RuleResult instances
 *  2. Default values work correctly
 *  3. toBuilder() creates a proper copy that can be modified
 *  4. Builder produces results equivalent to legacy constructors
 *  5. All fields are correctly propagated
 *  6. Null-safety for collection fields
 *  7. Build validation (resultType required)
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2026-02-08
 */
@DisplayName("RuleResult Builder Tests (Phase 1)")
@ExtendWith(ColoredTestOutputExtension.class)
class RuleResultBuilderTest {

    // =========================================================================
    // 1. Basic Builder Construction
    // =========================================================================

    @Test
    @DisplayName("Builder creates a valid MATCH result with all fields")
    void testBuilderCreatesMatchResult() {
        Map<String, Object> enrichedData = Map.of("key", "value");
        List<String> failures = List.of("msg1");

        RuleResult result = RuleResult.builder()
                .ruleId("rule-001")
                .ruleName("trade-validation")
                .message("Trade is valid")
                .severity(SeverityConstants.WARNING)
                .triggered(true)
                .resultType(RuleResult.ResultType.MATCH)
                .enrichedData(enrichedData)
                .failureMessages(failures)
                .success(true)
                .successCode("SC-001")
                .errorCode("EC-001")
                .mapToField(List.of("result.status"))
                .build();

        assertNotNull(result.getId(), "Should have a generated UUID");
        assertEquals("rule-001", result.getRuleId());
        assertEquals("trade-validation", result.getRuleName());
        assertEquals("Trade is valid", result.getMessage());
        assertEquals(SeverityConstants.WARNING, result.getSeverity());
        assertTrue(result.isTriggered());
        assertEquals(RuleResult.ResultType.MATCH, result.getResultType());
        assertNotNull(result.getTimestamp());
        assertEquals("value", result.getEnrichedData().get("key"));
        assertEquals(List.of("msg1"), result.getFailureMessages());
        assertTrue(result.isSuccess());
        assertEquals("SC-001", result.getSuccessCode());
        assertEquals("EC-001", result.getErrorCode());
        assertEquals(List.of("result.status"), result.getMapToField());
    }

    @Test
    @DisplayName("Builder creates a minimal NO_MATCH result with defaults")
    void testBuilderMinimalNoMatchResult() {
        RuleResult result = RuleResult.builder()
                .ruleName("check-1")
                .message("Not matched")
                .triggered(false)
                .resultType(RuleResult.ResultType.NO_MATCH)
                .build();

        assertEquals("check-1", result.getRuleName());
        assertFalse(result.isTriggered());
        assertEquals(RuleResult.ResultType.NO_MATCH, result.getResultType());
        // Defaults
        assertEquals(SeverityConstants.INFO, result.getSeverity());
        assertNull(result.getRuleId());
        assertNull(result.getPerformanceMetrics());
        assertTrue(result.getEnrichedData().isEmpty());
        assertTrue(result.getFailureMessages().isEmpty());
        assertTrue(result.isSuccess(), "Default success should be true");
        assertNull(result.getSuccessCode());
        assertNull(result.getErrorCode());
        assertNull(result.getMapToField());
        assertTrue(result.getChildResults().isEmpty());
        assertTrue(result.getExecutionPath().isEmpty());
    }

    @Test
    @DisplayName("Builder creates ERROR result")
    void testBuilderErrorResult() {
        RuleResult result = RuleResult.builder()
                .ruleName("broken-rule")
                .message("SpEL parse error")
                .severity(SeverityConstants.ERROR)
                .triggered(false)
                .resultType(RuleResult.ResultType.ERROR)
                .success(false)
                .failureMessages(List.of("parse error at position 5"))
                .build();

        assertEquals(RuleResult.ResultType.ERROR, result.getResultType());
        assertFalse(result.isSuccess());
        assertTrue(result.hasFailures());
        assertEquals(1, result.getFailureMessages().size());
    }

    // =========================================================================
    // 2. Builder Validation
    // =========================================================================

    @Test
    @DisplayName("Builder throws IllegalStateException when resultType is null")
    void testBuilderRequiresResultType() {
        RuleResult.Builder builder = RuleResult.builder()
                .ruleName("test")
                .message("msg");

        IllegalStateException ex = assertThrows(IllegalStateException.class, builder::build);
        assertEquals("resultType must be set", ex.getMessage());
    }

    // =========================================================================
    // 3. toBuilder() Copies and Modifications
    // =========================================================================

    @Test
    @DisplayName("toBuilder() creates a copy with same field values")
    void testToBuilderCopiesFields() {
        Map<String, Object> enrichedData = new HashMap<>(Map.of("a", 1, "b", 2));
        List<String> failures = new ArrayList<>(List.of("fail-1"));

        RuleResult original = RuleResult.builder()
                .ruleId("r-id")
                .ruleName("original-rule")
                .message("original message")
                .severity(SeverityConstants.ERROR)
                .triggered(true)
                .resultType(RuleResult.ResultType.MATCH)
                .enrichedData(enrichedData)
                .failureMessages(failures)
                .success(true)
                .successCode("S1")
                .errorCode("E1")
                .mapToField(List.of("field.x"))
                .build();

        RuleResult copy = original.toBuilder().build();

        // Same field values
        assertEquals(original.getRuleId(), copy.getRuleId());
        assertEquals(original.getRuleName(), copy.getRuleName());
        assertEquals(original.getMessage(), copy.getMessage());
        assertEquals(original.getSeverity(), copy.getSeverity());
        assertEquals(original.isTriggered(), copy.isTriggered());
        assertEquals(original.getResultType(), copy.getResultType());
        assertEquals(original.getEnrichedData(), copy.getEnrichedData());
        assertEquals(original.getFailureMessages(), copy.getFailureMessages());
        assertEquals(original.isSuccess(), copy.isSuccess());
        assertEquals(original.getSuccessCode(), copy.getSuccessCode());
        assertEquals(original.getErrorCode(), copy.getErrorCode());
        assertEquals(original.getMapToField(), copy.getMapToField());

        // Different UUID (freshly generated)
        assertNotEquals(original.getId(), copy.getId(), "Copy should have a new UUID");
    }

    @Test
    @DisplayName("toBuilder() allows modifying a single field without losing others")
    void testToBuilderModifySingleField() {
        RuleResult original = RuleResult.builder()
                .ruleId("r-001")
                .ruleName("validate-trade")
                .message("Trade valid")
                .severity(SeverityConstants.INFO)
                .triggered(true)
                .resultType(RuleResult.ResultType.MATCH)
                .enrichedData(Map.of("price", 100.0))
                .success(true)
                .successCode("S-200")
                .build();

        // Modify only message and enrichedData
        Map<String, Object> mergedData = new HashMap<>(original.getEnrichedData());
        mergedData.put("volume", 5000);

        RuleResult updated = original.toBuilder()
                .message("Trade valid (enriched)")
                .enrichedData(mergedData)
                .build();

        // Changed fields
        assertEquals("Trade valid (enriched)", updated.getMessage());
        assertEquals(5000, updated.getEnrichedData().get("volume"));
        assertEquals(100.0, updated.getEnrichedData().get("price"));

        // Unchanged fields
        assertEquals("r-001", updated.getRuleId());
        assertEquals("validate-trade", updated.getRuleName());
        assertEquals(SeverityConstants.INFO, updated.getSeverity());
        assertTrue(updated.isTriggered());
        assertEquals(RuleResult.ResultType.MATCH, updated.getResultType());
        assertTrue(updated.isSuccess());
        assertEquals("S-200", updated.getSuccessCode());
    }

    // =========================================================================
    // 4. Equivalence with Legacy Constructors
    // =========================================================================

    @Test
    @DisplayName("Builder produces equivalent result to 4-param constructor")
    void testBuilderEquivalentToFourParamConstructor() {
        RuleResult legacy = new RuleResult("rule-x", "msg-x", true, RuleResult.ResultType.MATCH);

        RuleResult built = RuleResult.builder()
                .ruleName("rule-x")
                .message("msg-x")
                .triggered(true)
                .resultType(RuleResult.ResultType.MATCH)
                .build();

        assertEquals(legacy.getRuleName(), built.getRuleName());
        assertEquals(legacy.getMessage(), built.getMessage());
        assertEquals(legacy.isTriggered(), built.isTriggered());
        assertEquals(legacy.getResultType(), built.getResultType());
        assertEquals(legacy.getSeverity(), built.getSeverity());
        assertEquals(legacy.isSuccess(), built.isSuccess());
    }

    @Test
    @DisplayName("Builder produces equivalent result to 13-param constructor")
    void testBuilderEquivalentToThirteenParamConstructor() {
        Map<String, Object> data = Map.of("k", "v");
        List<String> fails = List.of("f1");

        RuleResult legacy = new RuleResult("rid", "rname", "msg",
                SeverityConstants.WARNING, true, RuleResult.ResultType.MATCH,
                null, data, fails, true, "SC", "EC", List.of("field"));

        RuleResult built = RuleResult.builder()
                .ruleId("rid")
                .ruleName("rname")
                .message("msg")
                .severity(SeverityConstants.WARNING)
                .triggered(true)
                .resultType(RuleResult.ResultType.MATCH)
                .enrichedData(data)
                .failureMessages(fails)
                .success(true)
                .successCode("SC")
                .errorCode("EC")
                .mapToField(List.of("field"))
                .build();

        assertEquals(legacy.getRuleId(), built.getRuleId());
        assertEquals(legacy.getRuleName(), built.getRuleName());
        assertEquals(legacy.getMessage(), built.getMessage());
        assertEquals(legacy.getSeverity(), built.getSeverity());
        assertEquals(legacy.isTriggered(), built.isTriggered());
        assertEquals(legacy.getResultType(), built.getResultType());
        assertEquals(legacy.getEnrichedData(), built.getEnrichedData());
        assertEquals(legacy.getFailureMessages(), built.getFailureMessages());
        assertEquals(legacy.isSuccess(), built.isSuccess());
        assertEquals(legacy.getSuccessCode(), built.getSuccessCode());
        assertEquals(legacy.getErrorCode(), built.getErrorCode());
        assertEquals(legacy.getMapToField(), built.getMapToField());
    }

    // =========================================================================
    // 5. Null Safety and Collection Isolation
    // =========================================================================

    @Test
    @DisplayName("Builder handles null collections gracefully")
    void testBuilderNullCollections() {
        RuleResult result = RuleResult.builder()
                .ruleName("null-test")
                .message("test")
                .resultType(RuleResult.ResultType.NO_MATCH)
                .enrichedData(null)
                .failureMessages(null)
                .childResults(null)
                .executionPath(null)
                .build();

        assertNotNull(result.getEnrichedData());
        assertTrue(result.getEnrichedData().isEmpty());
        assertNotNull(result.getFailureMessages());
        assertTrue(result.getFailureMessages().isEmpty());
        assertNotNull(result.getChildResults());
        assertTrue(result.getChildResults().isEmpty());
        assertNotNull(result.getExecutionPath());
        assertTrue(result.getExecutionPath().isEmpty());
    }

    @Test
    @DisplayName("Builder performs defensive copy of collections")
    void testBuilderDefensiveCopy() {
        Map<String, Object> mutableMap = new HashMap<>();
        mutableMap.put("initial", "value");
        List<String> mutableList = new ArrayList<>();
        mutableList.add("msg-1");

        RuleResult result = RuleResult.builder()
                .ruleName("isolation-test")
                .message("test")
                .resultType(RuleResult.ResultType.MATCH)
                .triggered(true)
                .enrichedData(mutableMap)
                .failureMessages(mutableList)
                .build();

        // Mutate originals
        mutableMap.put("added", "later");
        mutableList.add("msg-2");

        // Result should be unaffected
        assertFalse(result.getEnrichedData().containsKey("added"),
                "Mutation of original map should not affect result");
        assertEquals(1, result.getFailureMessages().size(),
                "Mutation of original list should not affect result");
    }

    // =========================================================================
    // 6. Child Results and Execution Path
    // =========================================================================

    @Test
    @DisplayName("Builder supports childResults for composite evaluations")
    void testBuilderWithChildResults() {
        RuleResult child1 = RuleResult.builder()
                .ruleName("child-1")
                .message("passed")
                .triggered(true)
                .resultType(RuleResult.ResultType.MATCH)
                .build();

        RuleResult child2 = RuleResult.builder()
                .ruleName("child-2")
                .message("failed")
                .triggered(false)
                .resultType(RuleResult.ResultType.NO_MATCH)
                .build();

        RuleResult parent = RuleResult.builder()
                .ruleName("composite")
                .message("1 of 2 matched")
                .triggered(true)
                .resultType(RuleResult.ResultType.MATCH)
                .childResults(List.of(child1, child2))
                .build();

        assertEquals(2, parent.getChildResults().size());
        assertEquals("child-1", parent.getChildResults().get(0).getRuleName());
        assertEquals("child-2", parent.getChildResults().get(1).getRuleName());
    }

    @Test
    @DisplayName("Builder supports performanceMetrics")
    void testBuilderWithPerformanceMetrics() {
        RulePerformanceMetrics metrics = new RulePerformanceMetrics.Builder("perf-rule")
                .build();

        RuleResult result = RuleResult.builder()
                .ruleName("perf-rule")
                .message("measured")
                .triggered(true)
                .resultType(RuleResult.ResultType.MATCH)
                .performanceMetrics(metrics)
                .build();

        assertTrue(result.hasPerformanceMetrics());
        assertEquals("perf-rule", result.getPerformanceMetrics().getRuleName());
    }

    // =========================================================================
    // 7. Severity Default
    // =========================================================================

    @Test
    @DisplayName("Builder defaults severity to INFO when null is explicitly set")
    void testBuilderSeverityNullDefault() {
        RuleResult result = RuleResult.builder()
                .ruleName("sev-test")
                .message("test")
                .severity(null)
                .resultType(RuleResult.ResultType.MATCH)
                .triggered(true)
                .build();

        assertEquals(SeverityConstants.INFO, result.getSeverity(),
                "Null severity should default to INFO");
    }
}
