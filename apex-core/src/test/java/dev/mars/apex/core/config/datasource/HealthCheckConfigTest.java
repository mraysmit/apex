package dev.mars.apex.core.config.datasource;

import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive unit tests for HealthCheckConfig.
 * 
 * Tests cover:
 * - Constructor behavior and initialization
 * - Basic health check properties (enabled, interval, timeout, retry settings)
 * - Health check specifics (query, endpoint, expected response)
 * - Failure handling configuration (thresholds, logging, alerting)
 * - Circuit breaker integration settings
 * - Boolean convenience methods (isEnabled, shouldLogFailures, etc.)
 * - Utility methods (millisecond conversions)
 * - Validation logic for all property types
 * - Copy method deep cloning behavior
 * - Equals and hashCode contracts
 * - ToString representation
 * - Edge cases and error handling
 * 
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 1.0.0
 */
class HealthCheckConfigTest {

    private HealthCheckConfig config;

    @BeforeEach
    void setUp() {
        config = new HealthCheckConfig();
    }

    // ========================================
    // Constructor Tests
    // ========================================

    @Test
    @DisplayName("Should initialize with default constructor")
    void testDefaultConstructor() {
        HealthCheckConfig config = new HealthCheckConfig();
        
        // Basic properties should have default values
        assertTrue(config.getEnabled()); // Default is true
        assertEquals(60L, config.getIntervalSeconds()); // Default is 1 minute
        assertEquals(10L, config.getTimeoutSeconds()); // Default is 10 seconds
        assertEquals(3, config.getRetryAttempts()); // Default is 3
        assertEquals(1000L, config.getRetryDelay()); // Default is 1 second
        
        // Health check specifics should be null
        assertNull(config.getQuery());
        assertNull(config.getEndpoint());
        assertNull(config.getExpectedResponse());
        
        // Failure handling should have default values
        assertEquals(3, config.getFailureThreshold()); // Default is 3
        assertEquals(1, config.getSuccessThreshold()); // Default is 1
        assertTrue(config.getLogFailures()); // Default is true
        assertFalse(config.getAlertOnFailure()); // Default is false
        assertNull(config.getAlertEndpoint());
        
        // Circuit breaker integration should have default values
        assertFalse(config.getCircuitBreakerIntegration()); // Default is false
        assertEquals(5, config.getCircuitBreakerFailureThreshold()); // Default is 5
        assertEquals(60L, config.getCircuitBreakerTimeoutSeconds()); // Default is 1 minute
    }

    @Test
    @DisplayName("Should initialize with parameterized constructor")
    void testParameterizedConstructor() {
        HealthCheckConfig config = new HealthCheckConfig(false, 30L, 5L);
        
        assertFalse(config.getEnabled());
        assertEquals(30L, config.getIntervalSeconds());
        assertEquals(5L, config.getTimeoutSeconds());
        
        // Other properties should still have defaults
        assertEquals(3, config.getRetryAttempts());
        assertEquals(1000L, config.getRetryDelay());
        assertEquals(3, config.getFailureThreshold());
        assertEquals(1, config.getSuccessThreshold());
        assertTrue(config.getLogFailures());
        assertFalse(config.getAlertOnFailure());
        assertFalse(config.getCircuitBreakerIntegration());
    }

    @Test
    @DisplayName("Should handle null values in parameterized constructor")
    void testParameterizedConstructorWithNulls() {
        HealthCheckConfig config = new HealthCheckConfig(null, null, null);
        
        assertNull(config.getEnabled());
        assertNull(config.getIntervalSeconds());
        assertNull(config.getTimeoutSeconds());
        
        // Other properties should still have defaults
        assertEquals(3, config.getRetryAttempts());
        assertEquals(1000L, config.getRetryDelay());
    }

    // ========================================
    // Basic Health Check Property Tests
    // ========================================

    @Test
    @DisplayName("Should set and get basic health check properties")
    void testBasicHealthCheckProperties() {
        config.setEnabled(false);
        config.setIntervalSeconds(120L);
        config.setTimeoutSeconds(15L);
        config.setRetryAttempts(5);
        config.setRetryDelay(2000L);
        
        assertFalse(config.getEnabled());
        assertEquals(120L, config.getIntervalSeconds());
        assertEquals(15L, config.getTimeoutSeconds());
        assertEquals(5, config.getRetryAttempts());
        assertEquals(2000L, config.getRetryDelay());
    }

    @Test
    @DisplayName("Should handle null basic health check properties")
    void testNullBasicHealthCheckProperties() {
        config.setEnabled(null);
        config.setIntervalSeconds(null);
        config.setTimeoutSeconds(null);
        config.setRetryAttempts(null);
        config.setRetryDelay(null);
        
        assertNull(config.getEnabled());
        assertNull(config.getIntervalSeconds());
        assertNull(config.getTimeoutSeconds());
        assertNull(config.getRetryAttempts());
        assertNull(config.getRetryDelay());
    }

    @Test
    @DisplayName("Should provide boolean convenience method for enabled")
    void testEnabledConvenienceMethod() {
        config.setEnabled(true);
        assertTrue(config.isEnabled());
        
        config.setEnabled(false);
        assertFalse(config.isEnabled());
        
        config.setEnabled(null);
        assertFalse(config.isEnabled()); // null should be false
    }

    // ========================================
    // Health Check Specifics Property Tests
    // ========================================

    @Test
    @DisplayName("Should set and get health check specific properties")
    void testHealthCheckSpecificProperties() {
        config.setQuery("SELECT 1 FROM dual");
        config.setEndpoint("/health");
        config.setExpectedResponse("OK");
        
        assertEquals("SELECT 1 FROM dual", config.getQuery());
        assertEquals("/health", config.getEndpoint());
        assertEquals("OK", config.getExpectedResponse());
    }

    @Test
    @DisplayName("Should handle null health check specific properties")
    void testNullHealthCheckSpecificProperties() {
        config.setQuery(null);
        config.setEndpoint(null);
        config.setExpectedResponse(null);
        
        assertNull(config.getQuery());
        assertNull(config.getEndpoint());
        assertNull(config.getExpectedResponse());
    }

    // ========================================
    // Failure Handling Property Tests
    // ========================================

    @Test
    @DisplayName("Should set and get failure handling properties")
    void testFailureHandlingProperties() {
        config.setFailureThreshold(5);
        config.setSuccessThreshold(2);
        config.setLogFailures(false);
        config.setAlertOnFailure(true);
        config.setAlertEndpoint("https://alerts.example.com/webhook");
        
        assertEquals(5, config.getFailureThreshold());
        assertEquals(2, config.getSuccessThreshold());
        assertFalse(config.getLogFailures());
        assertTrue(config.getAlertOnFailure());
        assertEquals("https://alerts.example.com/webhook", config.getAlertEndpoint());
    }

    @Test
    @DisplayName("Should handle null failure handling properties")
    void testNullFailureHandlingProperties() {
        config.setFailureThreshold(null);
        config.setSuccessThreshold(null);
        config.setLogFailures(null);
        config.setAlertOnFailure(null);
        config.setAlertEndpoint(null);
        
        assertNull(config.getFailureThreshold());
        assertNull(config.getSuccessThreshold());
        assertNull(config.getLogFailures());
        assertNull(config.getAlertOnFailure());
        assertNull(config.getAlertEndpoint());
    }

    @Test
    @DisplayName("Should provide boolean convenience methods for failure handling")
    void testFailureHandlingConvenienceMethods() {
        // Test shouldLogFailures
        config.setLogFailures(true);
        assertTrue(config.shouldLogFailures());
        
        config.setLogFailures(false);
        assertFalse(config.shouldLogFailures());
        
        config.setLogFailures(null);
        assertFalse(config.shouldLogFailures()); // null should be false
        
        // Test shouldAlertOnFailure
        config.setAlertOnFailure(true);
        assertTrue(config.shouldAlertOnFailure());
        
        config.setAlertOnFailure(false);
        assertFalse(config.shouldAlertOnFailure());
        
        config.setAlertOnFailure(null);
        assertFalse(config.shouldAlertOnFailure()); // null should be false
    }

    // ========================================
    // Circuit Breaker Integration Property Tests
    // ========================================

    @Test
    @DisplayName("Should set and get circuit breaker integration properties")
    void testCircuitBreakerIntegrationProperties() {
        config.setCircuitBreakerIntegration(true);
        config.setCircuitBreakerFailureThreshold(10);
        config.setCircuitBreakerTimeoutSeconds(120L);
        
        assertTrue(config.getCircuitBreakerIntegration());
        assertEquals(10, config.getCircuitBreakerFailureThreshold());
        assertEquals(120L, config.getCircuitBreakerTimeoutSeconds());
    }

    @Test
    @DisplayName("Should handle null circuit breaker integration properties")
    void testNullCircuitBreakerIntegrationProperties() {
        config.setCircuitBreakerIntegration(null);
        config.setCircuitBreakerFailureThreshold(null);
        config.setCircuitBreakerTimeoutSeconds(null);
        
        assertNull(config.getCircuitBreakerIntegration());
        assertNull(config.getCircuitBreakerFailureThreshold());
        assertNull(config.getCircuitBreakerTimeoutSeconds());
    }

    @Test
    @DisplayName("Should provide boolean convenience method for circuit breaker integration")
    void testCircuitBreakerIntegrationConvenienceMethod() {
        config.setCircuitBreakerIntegration(true);
        assertTrue(config.isCircuitBreakerIntegrationEnabled());

        config.setCircuitBreakerIntegration(false);
        assertFalse(config.isCircuitBreakerIntegrationEnabled());

        config.setCircuitBreakerIntegration(null);
        assertFalse(config.isCircuitBreakerIntegrationEnabled()); // null should be false
    }

    // ========================================
    // Utility Method Tests
    // ========================================

    @Test
    @DisplayName("Should convert interval seconds to milliseconds")
    void testIntervalMillisecondsConversion() {
        config.setIntervalSeconds(60L);
        assertEquals(60000L, config.getIntervalMilliseconds());

        config.setIntervalSeconds(30L);
        assertEquals(30000L, config.getIntervalMilliseconds());

        config.setIntervalSeconds(0L);
        assertEquals(0L, config.getIntervalMilliseconds());

        config.setIntervalSeconds(null);
        assertEquals(0L, config.getIntervalMilliseconds());
    }

    @Test
    @DisplayName("Should convert timeout seconds to milliseconds")
    void testTimeoutMillisecondsConversion() {
        config.setTimeoutSeconds(10L);
        assertEquals(10000L, config.getTimeoutMilliseconds());

        config.setTimeoutSeconds(5L);
        assertEquals(5000L, config.getTimeoutMilliseconds());

        config.setTimeoutSeconds(0L);
        assertEquals(0L, config.getTimeoutMilliseconds());

        config.setTimeoutSeconds(null);
        assertEquals(0L, config.getTimeoutMilliseconds());
    }

    @Test
    @DisplayName("Should convert circuit breaker timeout seconds to milliseconds")
    void testCircuitBreakerTimeoutMillisecondsConversion() {
        config.setCircuitBreakerTimeoutSeconds(60L);
        assertEquals(60000L, config.getCircuitBreakerTimeoutMilliseconds());

        config.setCircuitBreakerTimeoutSeconds(120L);
        assertEquals(120000L, config.getCircuitBreakerTimeoutMilliseconds());

        config.setCircuitBreakerTimeoutSeconds(0L);
        assertEquals(0L, config.getCircuitBreakerTimeoutMilliseconds());

        config.setCircuitBreakerTimeoutSeconds(null);
        assertEquals(0L, config.getCircuitBreakerTimeoutMilliseconds());
    }

    // ========================================
    // Validation Tests
    // ========================================

    @Test
    @DisplayName("Should validate successfully with valid configuration")
    void testValidConfiguration() {
        config.setIntervalSeconds(60L);
        config.setTimeoutSeconds(10L);
        config.setRetryAttempts(3);
        config.setRetryDelay(1000L);
        config.setFailureThreshold(3);
        config.setSuccessThreshold(1);
        config.setCircuitBreakerFailureThreshold(5);
        config.setCircuitBreakerTimeoutSeconds(60L);

        assertDoesNotThrow(() -> config.validate());
    }

    @Test
    @DisplayName("Should fail validation with negative interval")
    void testValidationNegativeInterval() {
        config.setIntervalSeconds(-1L);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> config.validate());
        assertEquals("Health check interval must be positive", exception.getMessage());
    }

    @Test
    @DisplayName("Should fail validation with zero interval")
    void testValidationZeroInterval() {
        config.setIntervalSeconds(0L);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> config.validate());
        assertEquals("Health check interval must be positive", exception.getMessage());
    }

    @Test
    @DisplayName("Should fail validation with negative timeout")
    void testValidationNegativeTimeout() {
        config.setTimeoutSeconds(-1L);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> config.validate());
        assertEquals("Health check timeout must be positive", exception.getMessage());
    }

    @Test
    @DisplayName("Should fail validation with zero timeout")
    void testValidationZeroTimeout() {
        config.setTimeoutSeconds(0L);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> config.validate());
        assertEquals("Health check timeout must be positive", exception.getMessage());
    }

    @Test
    @DisplayName("Should fail validation with negative retry attempts")
    void testValidationNegativeRetryAttempts() {
        config.setRetryAttempts(-1);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> config.validate());
        assertEquals("Retry attempts cannot be negative", exception.getMessage());
    }

    @Test
    @DisplayName("Should allow zero retry attempts")
    void testValidationZeroRetryAttempts() {
        config.setRetryAttempts(0);

        assertDoesNotThrow(() -> config.validate());
    }

    @Test
    @DisplayName("Should fail validation with negative retry delay")
    void testValidationNegativeRetryDelay() {
        config.setRetryDelay(-1L);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> config.validate());
        assertEquals("Retry delay cannot be negative", exception.getMessage());
    }

    @Test
    @DisplayName("Should allow zero retry delay")
    void testValidationZeroRetryDelay() {
        config.setRetryDelay(0L);

        assertDoesNotThrow(() -> config.validate());
    }

    @Test
    @DisplayName("Should fail validation with negative failure threshold")
    void testValidationNegativeFailureThreshold() {
        config.setFailureThreshold(-1);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> config.validate());
        assertEquals("Failure threshold must be positive", exception.getMessage());
    }

    @Test
    @DisplayName("Should fail validation with zero failure threshold")
    void testValidationZeroFailureThreshold() {
        config.setFailureThreshold(0);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> config.validate());
        assertEquals("Failure threshold must be positive", exception.getMessage());
    }

    @Test
    @DisplayName("Should fail validation with negative success threshold")
    void testValidationNegativeSuccessThreshold() {
        config.setSuccessThreshold(-1);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> config.validate());
        assertEquals("Success threshold must be positive", exception.getMessage());
    }

    @Test
    @DisplayName("Should fail validation with zero success threshold")
    void testValidationZeroSuccessThreshold() {
        config.setSuccessThreshold(0);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> config.validate());
        assertEquals("Success threshold must be positive", exception.getMessage());
    }

    @Test
    @DisplayName("Should fail validation with negative circuit breaker failure threshold")
    void testValidationNegativeCircuitBreakerFailureThreshold() {
        config.setCircuitBreakerFailureThreshold(-1);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> config.validate());
        assertEquals("Circuit breaker failure threshold must be positive", exception.getMessage());
    }

    @Test
    @DisplayName("Should fail validation with zero circuit breaker failure threshold")
    void testValidationZeroCircuitBreakerFailureThreshold() {
        config.setCircuitBreakerFailureThreshold(0);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> config.validate());
        assertEquals("Circuit breaker failure threshold must be positive", exception.getMessage());
    }

    @Test
    @DisplayName("Should fail validation with negative circuit breaker timeout")
    void testValidationNegativeCircuitBreakerTimeout() {
        config.setCircuitBreakerTimeoutSeconds(-1L);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> config.validate());
        assertEquals("Circuit breaker timeout must be positive", exception.getMessage());
    }

    @Test
    @DisplayName("Should fail validation with zero circuit breaker timeout")
    void testValidationZeroCircuitBreakerTimeout() {
        config.setCircuitBreakerTimeoutSeconds(0L);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> config.validate());
        assertEquals("Circuit breaker timeout must be positive", exception.getMessage());
    }

    @Test
    @DisplayName("Should fail validation when alert on failure is enabled but no alert endpoint")
    void testValidationAlertOnFailureWithoutEndpoint() {
        config.setAlertOnFailure(true);
        config.setAlertEndpoint(null);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> config.validate());
        assertEquals("Alert endpoint is required when alert on failure is enabled", exception.getMessage());
    }

    @Test
    @DisplayName("Should fail validation when alert on failure is enabled but empty alert endpoint")
    void testValidationAlertOnFailureWithEmptyEndpoint() {
        config.setAlertOnFailure(true);
        config.setAlertEndpoint("   ");

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> config.validate());
        assertEquals("Alert endpoint is required when alert on failure is enabled", exception.getMessage());
    }

    @Test
    @DisplayName("Should validate successfully when alert on failure is disabled")
    void testValidationAlertOnFailureDisabled() {
        config.setAlertOnFailure(false);
        config.setAlertEndpoint(null);

        assertDoesNotThrow(() -> config.validate());
    }

    @Test
    @DisplayName("Should validate successfully when alert on failure is enabled with valid endpoint")
    void testValidationAlertOnFailureWithValidEndpoint() {
        config.setAlertOnFailure(true);
        config.setAlertEndpoint("https://alerts.example.com/webhook");

        assertDoesNotThrow(() -> config.validate());
    }

    @Test
    @DisplayName("Should handle null values in validation")
    void testValidationWithNullValues() {
        config.setIntervalSeconds(null);
        config.setTimeoutSeconds(null);
        config.setRetryAttempts(null);
        config.setRetryDelay(null);
        config.setFailureThreshold(null);
        config.setSuccessThreshold(null);
        config.setCircuitBreakerFailureThreshold(null);
        config.setCircuitBreakerTimeoutSeconds(null);
        config.setAlertOnFailure(null);
        config.setAlertEndpoint(null);

        assertDoesNotThrow(() -> config.validate());
    }

    // ========================================
    // Copy Method Tests
    // ========================================

    @Test
    @DisplayName("Should create deep copy with all properties")
    void testCopyMethod() {
        // Set up original configuration with all properties
        config.setEnabled(false);
        config.setIntervalSeconds(120L);
        config.setTimeoutSeconds(15L);
        config.setRetryAttempts(5);
        config.setRetryDelay(2000L);
        config.setQuery("SELECT 1 FROM dual");
        config.setEndpoint("/health");
        config.setExpectedResponse("OK");
        config.setFailureThreshold(5);
        config.setSuccessThreshold(2);
        config.setLogFailures(false);
        config.setAlertOnFailure(true);
        config.setAlertEndpoint("https://alerts.example.com/webhook");
        config.setCircuitBreakerIntegration(true);
        config.setCircuitBreakerFailureThreshold(10);
        config.setCircuitBreakerTimeoutSeconds(120L);

        // Create copy
        HealthCheckConfig copy = config.copy();

        // Verify all properties are copied
        assertEquals(config.getEnabled(), copy.getEnabled());
        assertEquals(config.getIntervalSeconds(), copy.getIntervalSeconds());
        assertEquals(config.getTimeoutSeconds(), copy.getTimeoutSeconds());
        assertEquals(config.getRetryAttempts(), copy.getRetryAttempts());
        assertEquals(config.getRetryDelay(), copy.getRetryDelay());
        assertEquals(config.getQuery(), copy.getQuery());
        assertEquals(config.getEndpoint(), copy.getEndpoint());
        assertEquals(config.getExpectedResponse(), copy.getExpectedResponse());
        assertEquals(config.getFailureThreshold(), copy.getFailureThreshold());
        assertEquals(config.getSuccessThreshold(), copy.getSuccessThreshold());
        assertEquals(config.getLogFailures(), copy.getLogFailures());
        assertEquals(config.getAlertOnFailure(), copy.getAlertOnFailure());
        assertEquals(config.getAlertEndpoint(), copy.getAlertEndpoint());
        assertEquals(config.getCircuitBreakerIntegration(), copy.getCircuitBreakerIntegration());
        assertEquals(config.getCircuitBreakerFailureThreshold(), copy.getCircuitBreakerFailureThreshold());
        assertEquals(config.getCircuitBreakerTimeoutSeconds(), copy.getCircuitBreakerTimeoutSeconds());
    }

    @Test
    @DisplayName("Should handle null values in copy method")
    void testCopyWithNullValues() {
        // Create a config with explicit null values (overriding defaults)
        config.setEnabled(null);
        config.setIntervalSeconds(null);
        config.setTimeoutSeconds(null);
        config.setRetryAttempts(null);
        config.setRetryDelay(null);
        config.setQuery(null);
        config.setEndpoint(null);
        config.setExpectedResponse(null);
        config.setFailureThreshold(null);
        config.setSuccessThreshold(null);
        config.setLogFailures(null);
        config.setAlertOnFailure(null);
        config.setAlertEndpoint(null);
        config.setCircuitBreakerIntegration(null);
        config.setCircuitBreakerFailureThreshold(null);
        config.setCircuitBreakerTimeoutSeconds(null);

        HealthCheckConfig copy = config.copy();

        // All properties should be null in the copy
        assertNull(copy.getEnabled());
        assertNull(copy.getIntervalSeconds());
        assertNull(copy.getTimeoutSeconds());
        assertNull(copy.getRetryAttempts());
        assertNull(copy.getRetryDelay());
        assertNull(copy.getQuery());
        assertNull(copy.getEndpoint());
        assertNull(copy.getExpectedResponse());
        assertNull(copy.getFailureThreshold());
        assertNull(copy.getSuccessThreshold());
        assertNull(copy.getLogFailures());
        assertNull(copy.getAlertOnFailure());
        assertNull(copy.getAlertEndpoint());
        assertNull(copy.getCircuitBreakerIntegration());
        assertNull(copy.getCircuitBreakerFailureThreshold());
        assertNull(copy.getCircuitBreakerTimeoutSeconds());
    }

    @Test
    @DisplayName("Should create independent copy that can be modified")
    void testCopyIndependence() {
        config.setEnabled(true);
        config.setIntervalSeconds(60L);
        config.setQuery("SELECT 1");

        HealthCheckConfig copy = config.copy();

        // Modify original
        config.setEnabled(false);
        config.setIntervalSeconds(120L);
        config.setQuery("SELECT 2");

        // Copy should remain unchanged
        assertTrue(copy.getEnabled());
        assertEquals(60L, copy.getIntervalSeconds());
        assertEquals("SELECT 1", copy.getQuery());

        // Modify copy
        copy.setEnabled(null);
        copy.setIntervalSeconds(30L);
        copy.setQuery("SELECT 3");

        // Original should remain unchanged
        assertFalse(config.getEnabled());
        assertEquals(120L, config.getIntervalSeconds());
        assertEquals("SELECT 2", config.getQuery());
    }

    // ========================================
    // Equals and HashCode Tests
    // ========================================

    @Test
    @DisplayName("Should be equal to itself")
    void testEqualsReflexive() {
        config.setEnabled(true);
        config.setIntervalSeconds(60L);

        assertEquals(config, config);
        assertEquals(config.hashCode(), config.hashCode());
    }

    @Test
    @DisplayName("Should be equal to another instance with same properties")
    void testEqualsSymmetric() {
        config.setEnabled(true);
        config.setIntervalSeconds(60L);
        config.setTimeoutSeconds(10L);
        config.setQuery("SELECT 1");
        config.setEndpoint("/health");

        HealthCheckConfig other = new HealthCheckConfig();
        other.setEnabled(true);
        other.setIntervalSeconds(60L);
        other.setTimeoutSeconds(10L);
        other.setQuery("SELECT 1");
        other.setEndpoint("/health");

        assertEquals(config, other);
        assertEquals(other, config);
        assertEquals(config.hashCode(), other.hashCode());
    }

    @Test
    @DisplayName("Should not be equal to null")
    void testEqualsNull() {
        config.setEnabled(true);

        assertNotEquals(config, null);
    }

    @Test
    @DisplayName("Should not be equal to different class")
    void testEqualsDifferentClass() {
        config.setEnabled(true);

        assertNotEquals(config, "not a HealthCheckConfig");
        assertNotEquals(config, new Object());
    }

    @Test
    @DisplayName("Should not be equal when enabled flags differ")
    void testEqualsEnabledDifference() {
        config.setEnabled(true);
        config.setIntervalSeconds(60L);

        HealthCheckConfig other = new HealthCheckConfig();
        other.setEnabled(false);
        other.setIntervalSeconds(60L);

        assertNotEquals(config, other);
    }

    @Test
    @DisplayName("Should not be equal when intervals differ")
    void testEqualsIntervalDifference() {
        config.setEnabled(true);
        config.setIntervalSeconds(60L);

        HealthCheckConfig other = new HealthCheckConfig();
        other.setEnabled(true);
        other.setIntervalSeconds(120L);

        assertNotEquals(config, other);
    }

    @Test
    @DisplayName("Should not be equal when timeouts differ")
    void testEqualsTimeoutDifference() {
        config.setEnabled(true);
        config.setTimeoutSeconds(10L);

        HealthCheckConfig other = new HealthCheckConfig();
        other.setEnabled(true);
        other.setTimeoutSeconds(15L);

        assertNotEquals(config, other);
    }

    @Test
    @DisplayName("Should not be equal when queries differ")
    void testEqualsQueryDifference() {
        config.setEnabled(true);
        config.setQuery("SELECT 1");

        HealthCheckConfig other = new HealthCheckConfig();
        other.setEnabled(true);
        other.setQuery("SELECT 2");

        assertNotEquals(config, other);
    }

    @Test
    @DisplayName("Should not be equal when endpoints differ")
    void testEqualsEndpointDifference() {
        config.setEnabled(true);
        config.setEndpoint("/health");

        HealthCheckConfig other = new HealthCheckConfig();
        other.setEnabled(true);
        other.setEndpoint("/status");

        assertNotEquals(config, other);
    }

    @Test
    @DisplayName("Should handle null values in equals comparison")
    void testEqualsWithNullValues() {
        HealthCheckConfig config1 = new HealthCheckConfig();
        HealthCheckConfig config2 = new HealthCheckConfig();

        // Both have default values, should be equal
        assertEquals(config1, config2);
        assertEquals(config1.hashCode(), config2.hashCode());

        // Change one property to make them different
        config1.setEnabled(false);
        assertNotEquals(config1, config2);

        // Make them the same again
        config2.setEnabled(false);
        assertEquals(config1, config2);
        assertEquals(config1.hashCode(), config2.hashCode());
    }

    // ========================================
    // ToString Tests
    // ========================================

    @Test
    @DisplayName("Should generate meaningful toString representation")
    void testToString() {
        config.setEnabled(false);
        config.setIntervalSeconds(120L);
        config.setTimeoutSeconds(15L);
        config.setQuery("SELECT 1 FROM dual");
        config.setEndpoint("/health");
        config.setFailureThreshold(5);
        config.setSuccessThreshold(2);

        String result = config.toString();

        assertNotNull(result);
        assertTrue(result.contains("false")); // enabled
        assertTrue(result.contains("120")); // intervalSeconds
        assertTrue(result.contains("15")); // timeoutSeconds
        assertTrue(result.contains("SELECT 1 FROM dual")); // query
        assertTrue(result.contains("/health")); // endpoint
        assertTrue(result.contains("5")); // failureThreshold
        assertTrue(result.contains("2")); // successThreshold
        assertTrue(result.contains("HealthCheckConfig"));
    }

    @Test
    @DisplayName("Should handle null values in toString")
    void testToStringWithNulls() {
        // Set some properties to null explicitly
        config.setEnabled(null);
        config.setIntervalSeconds(null);
        config.setTimeoutSeconds(null);
        config.setQuery(null);
        config.setEndpoint(null);

        String result = config.toString();

        assertNotNull(result);
        assertTrue(result.contains("HealthCheckConfig"));
        assertTrue(result.contains("null"));
    }

    @Test
    @DisplayName("Should be consistent toString output")
    void testToStringConsistency() {
        config.setEnabled(true);
        config.setIntervalSeconds(60L);
        config.setTimeoutSeconds(10L);
        config.setQuery("SELECT 1");

        String result1 = config.toString();
        String result2 = config.toString();

        assertEquals(result1, result2);
    }

    @Test
    @DisplayName("Should include key health check properties in toString")
    void testToStringKeyProperties() {
        config.setEnabled(true);
        config.setIntervalSeconds(30L);
        config.setTimeoutSeconds(5L);
        config.setQuery("SELECT COUNT(*) FROM users");
        config.setEndpoint("/api/health");
        config.setFailureThreshold(3);
        config.setSuccessThreshold(1);

        String result = config.toString();

        // Verify all key properties are included
        assertTrue(result.contains("enabled=true"));
        assertTrue(result.contains("intervalSeconds=30"));
        assertTrue(result.contains("timeoutSeconds=5"));
        assertTrue(result.contains("query='SELECT COUNT(*) FROM users'"));
        assertTrue(result.contains("endpoint='/api/health'"));
        assertTrue(result.contains("failureThreshold=3"));
        assertTrue(result.contains("successThreshold=1"));
    }
}
