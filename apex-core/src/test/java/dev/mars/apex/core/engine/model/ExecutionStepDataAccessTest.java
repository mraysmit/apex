package dev.mars.apex.core.engine.model;

import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ExecutionStep data access methods.
 * 
 * This test suite validates the ExecutionStep class methods for accessing
 * step data, metrics, and metadata.
 * 
 * TEST COVERAGE:
 * - hasStepData() method (4 tests)
 * - getStepData() method
 * - getSuccessRate() calculation
 * - Null safety
 * - Edge cases
 * 
 * @author APEX Core Team
 * @since 2026-01-11
 * @version 1.0.0
 */
@DisplayName("ExecutionStep Data Access Tests")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ExecutionStepDataAccessTest {

    private static final Logger logger = LoggerFactory.getLogger(ExecutionStepDataAccessTest.class);

    // ========================================================================
    // TEST 15: hasStepData() with Data Present
    // ========================================================================
    
    @Test
    @Order(15)
    @DisplayName("hasStepData() should return true when data is present")
    public void hasStepDataShouldReturnTrueWhenDataPresent() {
        logger.info("=== Test 15: hasStepData() with Data Present ===");

        // Given: An ExecutionStep with data
        List<Map<String, Object>> testData = new ArrayList<>();
        testData.add(Map.of("id", 1, "name", "Test"));

        ExecutionStep step = new ExecutionStep("test-step", "PIPELINE_STEP", "SUCCESS",
            "Test step", 100L, testData, 1, 0);

        // When: Check if step has data
        boolean hasData = step.hasStepData();

        // Then: Should return true
        assertTrue(hasData, "hasStepData() should return true when data is present");
        assertNotNull(step.getStepData(), "getStepData() should return the data");
        assertEquals(testData, step.getStepData(), "Data should match what was set");

        logger.info("✓ hasStepData() correctly returns true for step with data");
    }

    // ========================================================================
    // TEST 16: hasStepData() with Null Data
    // ========================================================================
    
    @Test
    @Order(16)
    @DisplayName("hasStepData() should return false when data is null")
    public void hasStepDataShouldReturnFalseWhenDataNull() {
        logger.info("=== Test 16: hasStepData() with Null Data ===");

        // Given: An ExecutionStep with null data
        ExecutionStep step = new ExecutionStep("test-step", "PIPELINE_STEP", "SUCCESS",
            "Test step", 100L, null, 0, 0);

        // When: Check if step has data
        boolean hasData = step.hasStepData();

        // Then: Should return false
        assertFalse(hasData, "hasStepData() should return false when data is null");
        assertNull(step.getStepData(), "getStepData() should return null");

        logger.info("✓ hasStepData() correctly returns false for step with null data");
    }

    // ========================================================================
    // TEST 17: hasStepData() with Empty Collection
    // ========================================================================

    @Test
    @Order(17)
    @DisplayName("hasStepData() should return true for empty collection")
    public void hasStepDataShouldReturnTrueForEmptyCollection() {
        logger.info("=== Test 17: hasStepData() with Empty Collection ===");

        // Given: An ExecutionStep with empty list (not null)
        List<Map<String, Object>> emptyData = new ArrayList<>();

        ExecutionStep step = new ExecutionStep("test-step", "PIPELINE_STEP", "SUCCESS",
            "Test step", 100L, emptyData, 0, 0);

        // When: Check if step has data
        boolean hasData = step.hasStepData();

        // Then: Should return true (data object exists, even if empty)
        assertTrue(hasData, "hasStepData() should return true for empty collection");
        assertNotNull(step.getStepData(), "getStepData() should return the empty collection");
        assertTrue(step.getStepData() instanceof List, "Data should be a List");
        assertTrue(((List<?>) step.getStepData()).isEmpty(), "List should be empty");

        logger.info("✓ hasStepData() correctly returns true for empty collection");
    }

    // ========================================================================
    // TEST 18: hasStepData() without stepData Field Set
    // ========================================================================

    @Test
    @Order(18)
    @DisplayName("hasStepData() should return false when stepData field not set")
    public void hasStepDataShouldReturnFalseWhenFieldNotSet() {
        logger.info("=== Test 18: hasStepData() without stepData Field Set ===");

        // Given: An ExecutionStep without stepData field set (using basic constructor)
        ExecutionStep step = new ExecutionStep("test-step", "PIPELINE_STEP", "SUCCESS",
            "Test step", 100L);

        // When: Check if step has data
        boolean hasData = step.hasStepData();

        // Then: Should return false
        assertFalse(hasData, "hasStepData() should return false when field not set");
        assertNull(step.getStepData(), "getStepData() should return null");

        logger.info("✓ hasStepData() correctly returns false when field not set");
    }

    // ========================================================================
    // TEST 19: getSuccessRate() Calculation
    // ========================================================================

    @Test
    @Order(19)
    @DisplayName("getSuccessRate() should calculate correctly")
    public void getSuccessRateShouldCalculateCorrectly() {
        logger.info("=== Test 19: getSuccessRate() Calculation ===");

        // Test Case 1: All successful
        ExecutionStep allSuccess = new ExecutionStep("all-success", "PIPELINE_STEP", "SUCCESS",
            "All successful", 100L, null, 10, 0);

        assertEquals(100.0, allSuccess.getSuccessRate(), 0.01,
            "Success rate should be 100% when all records succeed");
        logger.info("  All success: {}%", allSuccess.getSuccessRate());

        // Test Case 2: All failed
        ExecutionStep allFailed = new ExecutionStep("all-failed", "PIPELINE_STEP", "FAILURE",
            "All failed", 100L, null, 0, 10);

        assertEquals(0.0, allFailed.getSuccessRate(), 0.01,
            "Success rate should be 0% when all records fail");
        logger.info("  All failed: {}%", allFailed.getSuccessRate());

        // Test Case 3: Partial success
        ExecutionStep partialSuccess = new ExecutionStep("partial-success", "PIPELINE_STEP", "SUCCESS",
            "Partial success", 100L, null, 7, 3);

        assertEquals(70.0, partialSuccess.getSuccessRate(), 0.01,
            "Success rate should be 70% when 7/10 records succeed");
        logger.info("  Partial success: {}%", partialSuccess.getSuccessRate());

        // Test Case 4: No records
        ExecutionStep noRecords = new ExecutionStep("no-records", "PIPELINE_STEP", "SUCCESS",
            "No records", 100L, null, 0, 0);

        assertEquals(100.0, noRecords.getSuccessRate(), 0.01,
            "Success rate should be 100% when no records processed");
        logger.info("  No records: {}%", noRecords.getSuccessRate());

        logger.info("✓ getSuccessRate() calculates correctly for all scenarios");
    }

    // ========================================================================
    // TEST 20: getSuccessRate() with Null Metrics
    // ========================================================================

    @Test
    @Order(20)
    @DisplayName("getSuccessRate() should handle null metrics")
    public void getSuccessRateShouldHandleNullMetrics() {
        logger.info("=== Test 20: getSuccessRate() with Null Metrics ===");

        // Test Case 1: Both null (using basic constructor)
        ExecutionStep bothNull = new ExecutionStep("both-null", "PIPELINE_STEP", "SUCCESS",
            "Both null", 100L);

        double rate1 = bothNull.getSuccessRate();
        assertEquals(100.0, rate1, 0.01,
            "Success rate should be 100% when both metrics are null and status is SUCCESS");
        logger.info("  Both null: {}%", rate1);

        // Test Case 2: Processed null, failed set
        ExecutionStep processedNull = new ExecutionStep("processed-null", "PIPELINE_STEP", "SUCCESS",
            "Processed null", 100L);
        processedNull.setRecordsFailed(5);

        double rate2 = processedNull.getSuccessRate();
        assertTrue(rate2 >= 0.0 && rate2 <= 100.0,
            "Success rate should be between 0 and 100");
        logger.info("  Processed null: {}%", rate2);

        // Test Case 3: Failed null, processed set
        ExecutionStep failedNull = new ExecutionStep("failed-null", "PIPELINE_STEP", "SUCCESS",
            "Failed null", 100L);
        failedNull.setRecordsProcessed(10);

        double rate3 = failedNull.getSuccessRate();
        assertTrue(rate3 >= 0.0 && rate3 <= 100.0,
            "Success rate should be between 0 and 100");
        logger.info("  Failed null: {}%", rate3);

        logger.info("✓ getSuccessRate() handles null metrics gracefully");
    }

    // ========================================================================
    // TEST 21: getStepData() Type Safety
    // ========================================================================

    @Test
    @Order(21)
    @DisplayName("getStepData() should preserve data types")
    public void getStepDataShouldPreserveDataTypes() {
        logger.info("=== Test 21: getStepData() Type Safety ===");

        // Test Case 1: List<Map<String, Object>>
        List<Map<String, Object>> listData = new ArrayList<>();
        listData.add(Map.of("id", 1, "name", "Test 1"));
        listData.add(Map.of("id", 2, "name", "Test 2"));

        ExecutionStep listStep = new ExecutionStep("list-step", "PIPELINE_STEP", "SUCCESS",
            "List data", 100L, listData, 2, 0);

        Object retrievedData1 = listStep.getStepData();
        assertTrue(retrievedData1 instanceof List, "Data should be a List");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> typedList = (List<Map<String, Object>>) retrievedData1;
        assertEquals(2, typedList.size(), "List should have 2 elements");
        logger.info("  List data preserved: {} records", typedList.size());

        // Test Case 2: Single Map
        Map<String, Object> mapData = new HashMap<>();
        mapData.put("total", 100);
        mapData.put("status", "complete");

        ExecutionStep mapStep = new ExecutionStep("map-step", "PIPELINE_STEP", "SUCCESS",
            "Map data", 100L, mapData, 1, 0);

        Object retrievedData2 = mapStep.getStepData();
        assertTrue(retrievedData2 instanceof Map, "Data should be a Map");
        @SuppressWarnings("unchecked")
        Map<String, Object> typedMap = (Map<String, Object>) retrievedData2;
        assertEquals(100, typedMap.get("total"), "Map data should be preserved");
        logger.info("  Map data preserved: {} keys", typedMap.size());

        // Test Case 3: Custom object
        TestDataObject customData = new TestDataObject("test", 42);

        ExecutionStep customStep = new ExecutionStep("custom-step", "PIPELINE_STEP", "SUCCESS",
            "Custom data", 100L, customData, 1, 0);

        Object retrievedData3 = customStep.getStepData();
        assertTrue(retrievedData3 instanceof TestDataObject, "Data should be TestDataObject");
        TestDataObject typedCustom = (TestDataObject) retrievedData3;
        assertEquals("test", typedCustom.getName(), "Custom object data should be preserved");
        assertEquals(42, typedCustom.getValue(), "Custom object data should be preserved");
        logger.info("  Custom object preserved: {}", typedCustom);

        logger.info("✓ getStepData() preserves all data types correctly");
    }

    // ========================================================================
    // TEST 22: Null Safety
    // ========================================================================

    @Test
    @Order(22)
    @DisplayName("ExecutionStep should handle null values safely")
    public void executionStepShouldHandleNullValuesSafely() {
        logger.info("=== Test 22: Null Safety ===");

        // Given: An ExecutionStep with minimal required fields
        ExecutionStep step = new ExecutionStep("minimal-step", "PIPELINE_STEP", "SUCCESS",
            "Minimal step", 100L);

        // When/Then: Access all methods without NPE
        assertDoesNotThrow(() -> {
            step.getName();
            step.getType();
            step.getStatus();
            step.getMessage();
            step.getTimestamp();
            step.getDurationMs();
            step.getStepData();
            step.getRecordsProcessed();
            step.getRecordsFailed();
            step.hasStepData();
            step.getSuccessRate();
        }, "No method should throw NPE");

        // Verify null-safe behavior
        assertFalse(step.hasStepData(), "hasStepData() should return false");
        assertNull(step.getStepData(), "getStepData() should return null");
        assertNull(step.getRecordsProcessed(), "getRecordsProcessed() can be null");
        assertNull(step.getRecordsFailed(), "getRecordsFailed() can be null");

        double successRate = step.getSuccessRate();
        assertTrue(successRate >= 0.0 && successRate <= 100.0,
            "getSuccessRate() should return valid percentage even with null metrics");

        logger.info("  All methods are null-safe");
        logger.info("  Success rate with null metrics: {}%", successRate);
        logger.info("✓ ExecutionStep handles null values safely");
    }

    // ========================================================================
    // Helper Classes
    // ========================================================================

    /**
     * Test data object for type preservation testing.
     */
    private static class TestDataObject implements java.io.Serializable {
        private final String name;
        private final int value;

        public TestDataObject(String name, int value) {
            this.name = name;
            this.value = value;
        }

        public String getName() {
            return name;
        }

        public int getValue() {
            return value;
        }

        @Override
        public String toString() {
            return "TestDataObject{name='" + name + "', value=" + value + "}";
        }
    }
}

