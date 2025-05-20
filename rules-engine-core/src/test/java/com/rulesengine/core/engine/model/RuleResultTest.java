package com.rulesengine.core.engine.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for RuleResult.
 * This class tests the functionality of the RuleResult class.
 */
public class RuleResultTest {

    /**
     * Test creating a match result.
     */
    @Test
    public void testCreateMatchResult() {
        // Create a match result
        RuleResult result = RuleResult.match("TestRule", "Test message");

        // Verify result properties
        assertEquals("TestRule", result.getRuleName(), "Rule name should match");
        assertEquals("Test message", result.getMessage(), "Message should match");
        assertTrue(result.isTriggered(), "Result should be triggered");
        assertEquals(RuleResult.ResultType.MATCH, result.getResultType(), "Result type should be MATCH");
        assertNotNull(result.getId(), "Result ID should not be null");
        assertNotNull(result.getTimestamp(), "Result timestamp should not be null");
    }

    /**
     * Test creating a no-match result.
     */
    @Test
    public void testCreateNoMatchResult() {
        // Create a no-match result
        RuleResult result = RuleResult.noMatch();

        // Verify result properties
        assertEquals("no-match", result.getRuleName(), "Rule name should be no-match");
        assertEquals("No matching rules found", result.getMessage(), "Message should be 'No matching rules found'");
        assertFalse(result.isTriggered(), "Result should not be triggered");
        assertEquals(RuleResult.ResultType.NO_MATCH, result.getResultType(), "Result type should be NO_MATCH");
        assertNotNull(result.getId(), "Result ID should not be null");
        assertNotNull(result.getTimestamp(), "Result timestamp should not be null");
    }

    /**
     * Test creating a no-rules result.
     */
    @Test
    public void testCreateNoRulesResult() {
        // Create a no-rules result
        RuleResult result = RuleResult.noRules();

        // Verify result properties
        assertEquals("no-rule", result.getRuleName(), "Rule name should be no-rule");
        assertEquals("No rules provided", result.getMessage(), "Message should be 'No rules provided'");
        assertFalse(result.isTriggered(), "Result should not be triggered");
        assertEquals(RuleResult.ResultType.NO_RULES, result.getResultType(), "Result type should be NO_RULES");
        assertNotNull(result.getId(), "Result ID should not be null");
        assertNotNull(result.getTimestamp(), "Result timestamp should not be null");
    }

    /**
     * Test creating an error result.
     */
    @Test
    public void testCreateErrorResult() {
        // Create an error result
        RuleResult result = RuleResult.error("TestRule", "An error occurred");

        // Verify result properties
        assertEquals("TestRule", result.getRuleName(), "Rule name should match");
        assertEquals("An error occurred", result.getMessage(), "Message should match");
        assertFalse(result.isTriggered(), "Result should not be triggered");
        assertEquals(RuleResult.ResultType.ERROR, result.getResultType(), "Result type should be ERROR");
        assertNotNull(result.getId(), "Result ID should not be null");
        assertNotNull(result.getTimestamp(), "Result timestamp should not be null");
    }

    /**
     * Test creating a result with the constructor.
     */
    @Test
    public void testCreateResultWithConstructor() {
        // Create a result with the constructor
        RuleResult result = new RuleResult("TestRule", "Test message", true, RuleResult.ResultType.MATCH);

        // Verify result properties
        assertEquals("TestRule", result.getRuleName(), "Rule name should match");
        assertEquals("Test message", result.getMessage(), "Message should match");
        assertTrue(result.isTriggered(), "Result should be triggered");
        assertEquals(RuleResult.ResultType.MATCH, result.getResultType(), "Result type should be MATCH");
        assertNotNull(result.getId(), "Result ID should not be null");
        assertNotNull(result.getTimestamp(), "Result timestamp should not be null");
    }

    /**
     * Test result equality.
     */
    @Test
    public void testResultEquality() {
        // Create two results with the same ID
        RuleResult result1 = new RuleResult("TestRule", "Test message");
        RuleResult result2 = new RuleResult("TestRule", "Test message");

        // Create a result with a different ID
        RuleResult result3 = new RuleResult("AnotherRule", "Another message");

        // Verify equality
        assertNotEquals(result1, result2, "Results with different IDs should not be equal");
        assertNotEquals(result1, result3, "Results with different IDs should not be equal");

        // Verify hashCode
        assertNotEquals(result1.hashCode(), result2.hashCode(), "Results with different IDs should have different hash codes");
        assertNotEquals(result1.hashCode(), result3.hashCode(), "Results with different IDs should have different hash codes");
    }

    /**
     * Test result toString.
     */
    @Test
    public void testResultToString() {
        // Create a result
        RuleResult result = RuleResult.match("TestRule", "Test message");

        // Verify toString
        String toString = result.toString();
        assertTrue(toString.contains("TestRule"), "toString should contain the rule name");
        assertTrue(toString.contains("Test message"), "toString should contain the message");
        assertTrue(toString.contains("MATCH"), "toString should contain the result type");
    }
}
