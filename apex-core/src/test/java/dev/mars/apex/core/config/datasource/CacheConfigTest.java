package dev.mars.apex.core.config.datasource;

import dev.mars.apex.core.config.datasource.CacheConfig.EvictionPolicy;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive unit tests for CacheConfig.
 * 
 * Tests cover:
 * - Constructor behavior and initialization
 * - Basic cache properties (enabled, TTL, maxSize, eviction policy)
 * - Advanced cache features (preload, refresh ahead, statistics, compression)
 * - Cache warming configuration
 * - Distributed cache configuration
 * - Boolean convenience methods (isEnabled, isPreloadEnabled, etc.)
 * - Utility methods (TTL/idle time conversions, refresh ahead calculations)
 * - Validation logic for all property types
 * - Copy method deep cloning behavior
 * - Equals and hashCode contracts
 * - ToString representation
 * - EvictionPolicy enum functionality
 * - Edge cases and error handling
 * 
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 1.0.0
 */
class CacheConfigTest {

    private CacheConfig config;

    @BeforeEach
    void setUp() {
        config = new CacheConfig();
    }

    // ========================================
    // Constructor Tests
    // ========================================

    @Test
    @DisplayName("Should initialize with default constructor")
    void testDefaultConstructor() {
        CacheConfig config = new CacheConfig();
        
        // Basic properties should have default values
        assertTrue(config.getEnabled()); // Default is true
        assertEquals(3600L, config.getTtlSeconds()); // Default is 1 hour
        assertEquals(1800L, config.getMaxIdleSeconds()); // Default is 30 minutes
        assertEquals(10000, config.getMaxSize()); // Default is 10000
        assertEquals(EvictionPolicy.LRU, config.getEvictionPolicy()); // Default is LRU
        
        // Advanced properties should have default values
        assertFalse(config.getPreloadEnabled()); // Default is false
        assertFalse(config.getRefreshAhead()); // Default is false
        assertEquals(75L, config.getRefreshAheadFactor()); // Default is 75%
        assertTrue(config.getStatisticsEnabled()); // Default is true
        assertNull(config.getKeyPrefix()); // Default is null
        assertFalse(config.getCompressionEnabled()); // Default is false
        assertEquals("json", config.getSerializationFormat()); // Default is json
        
        // Cache warming properties should have default values
        assertFalse(config.getWarmupEnabled()); // Default is false
        assertEquals(100, config.getWarmupBatchSize()); // Default is 100
        assertEquals(0L, config.getWarmupDelay()); // Default is 0
        
        // Distributed cache properties should have default values
        assertFalse(config.getDistributedCache()); // Default is false
        assertNull(config.getCacheCluster()); // Default is null
        assertEquals(1, config.getReplicationFactor()); // Default is 1
    }

    @Test
    @DisplayName("Should initialize with parameterized constructor")
    void testParameterizedConstructor() {
        CacheConfig config = new CacheConfig(false, 7200L, 5000);
        
        assertFalse(config.getEnabled());
        assertEquals(7200L, config.getTtlSeconds());
        assertEquals(5000, config.getMaxSize());
        
        // Other properties should still have defaults
        assertEquals(1800L, config.getMaxIdleSeconds());
        assertEquals(EvictionPolicy.LRU, config.getEvictionPolicy());
        assertFalse(config.getPreloadEnabled());
    }

    @Test
    @DisplayName("Should handle null values in parameterized constructor")
    void testParameterizedConstructorWithNulls() {
        CacheConfig config = new CacheConfig(null, null, null);
        
        assertNull(config.getEnabled());
        assertNull(config.getTtlSeconds());
        assertNull(config.getMaxSize());
        
        // Other properties should still have defaults
        assertEquals(1800L, config.getMaxIdleSeconds());
        assertEquals(EvictionPolicy.LRU, config.getEvictionPolicy());
    }

    // ========================================
    // Basic Cache Property Tests
    // ========================================

    @Test
    @DisplayName("Should set and get basic cache properties")
    void testBasicCacheProperties() {
        config.setEnabled(false);
        config.setTtlSeconds(7200L);
        config.setMaxIdleSeconds(3600L);
        config.setMaxSize(5000);
        config.setEvictionPolicy(EvictionPolicy.LFU);
        
        assertFalse(config.getEnabled());
        assertEquals(7200L, config.getTtlSeconds());
        assertEquals(3600L, config.getMaxIdleSeconds());
        assertEquals(5000, config.getMaxSize());
        assertEquals(EvictionPolicy.LFU, config.getEvictionPolicy());
    }

    @Test
    @DisplayName("Should handle null basic cache properties")
    void testNullBasicCacheProperties() {
        config.setEnabled(null);
        config.setTtlSeconds(null);
        config.setMaxIdleSeconds(null);
        config.setMaxSize(null);
        config.setEvictionPolicy(null);
        
        assertNull(config.getEnabled());
        assertNull(config.getTtlSeconds());
        assertNull(config.getMaxIdleSeconds());
        assertNull(config.getMaxSize());
        assertNull(config.getEvictionPolicy());
    }

    @Test
    @DisplayName("Should provide boolean convenience methods")
    void testBooleanConvenienceMethods() {
        // Test enabled
        config.setEnabled(true);
        assertTrue(config.isEnabled());
        
        config.setEnabled(false);
        assertFalse(config.isEnabled());
        
        config.setEnabled(null);
        assertFalse(config.isEnabled()); // null should be false
        
        // Test preload enabled
        config.setPreloadEnabled(true);
        assertTrue(config.isPreloadEnabled());
        
        config.setPreloadEnabled(false);
        assertFalse(config.isPreloadEnabled());
        
        config.setPreloadEnabled(null);
        assertFalse(config.isPreloadEnabled()); // null should be false
        
        // Test refresh ahead enabled
        config.setRefreshAhead(true);
        assertTrue(config.isRefreshAheadEnabled());
        
        config.setRefreshAhead(false);
        assertFalse(config.isRefreshAheadEnabled());
        
        config.setRefreshAhead(null);
        assertFalse(config.isRefreshAheadEnabled()); // null should be false
        
        // Test statistics enabled
        config.setStatisticsEnabled(true);
        assertTrue(config.isStatisticsEnabled());
        
        config.setStatisticsEnabled(false);
        assertFalse(config.isStatisticsEnabled());
        
        config.setStatisticsEnabled(null);
        assertFalse(config.isStatisticsEnabled()); // null should be false
        
        // Test compression enabled
        config.setCompressionEnabled(true);
        assertTrue(config.isCompressionEnabled());
        
        config.setCompressionEnabled(false);
        assertFalse(config.isCompressionEnabled());
        
        config.setCompressionEnabled(null);
        assertFalse(config.isCompressionEnabled()); // null should be false
        
        // Test warmup enabled
        config.setWarmupEnabled(true);
        assertTrue(config.isWarmupEnabled());
        
        config.setWarmupEnabled(false);
        assertFalse(config.isWarmupEnabled());
        
        config.setWarmupEnabled(null);
        assertFalse(config.isWarmupEnabled()); // null should be false
        
        // Test distributed cache
        config.setDistributedCache(true);
        assertTrue(config.isDistributedCache());
        
        config.setDistributedCache(false);
        assertFalse(config.isDistributedCache());
        
        config.setDistributedCache(null);
        assertFalse(config.isDistributedCache()); // null should be false
    }

    // ========================================
    // Advanced Cache Property Tests
    // ========================================

    @Test
    @DisplayName("Should set and get advanced cache properties")
    void testAdvancedCacheProperties() {
        config.setPreloadEnabled(true);
        config.setRefreshAhead(true);
        config.setRefreshAheadFactor(80L);
        config.setStatisticsEnabled(false);
        config.setKeyPrefix("test:");
        config.setCompressionEnabled(true);
        config.setSerializationFormat("protobuf");
        
        assertTrue(config.getPreloadEnabled());
        assertTrue(config.getRefreshAhead());
        assertEquals(80L, config.getRefreshAheadFactor());
        assertFalse(config.getStatisticsEnabled());
        assertEquals("test:", config.getKeyPrefix());
        assertTrue(config.getCompressionEnabled());
        assertEquals("protobuf", config.getSerializationFormat());
    }

    @Test
    @DisplayName("Should handle null advanced cache properties")
    void testNullAdvancedCacheProperties() {
        config.setPreloadEnabled(null);
        config.setRefreshAhead(null);
        config.setRefreshAheadFactor(null);
        config.setStatisticsEnabled(null);
        config.setKeyPrefix(null);
        config.setCompressionEnabled(null);
        config.setSerializationFormat(null);
        
        assertNull(config.getPreloadEnabled());
        assertNull(config.getRefreshAhead());
        assertNull(config.getRefreshAheadFactor());
        assertNull(config.getStatisticsEnabled());
        assertNull(config.getKeyPrefix());
        assertNull(config.getCompressionEnabled());
        assertNull(config.getSerializationFormat());
    }

    // ========================================
    // Cache Warming Property Tests
    // ========================================

    @Test
    @DisplayName("Should set and get cache warming properties")
    void testCacheWarmingProperties() {
        config.setWarmupEnabled(true);
        config.setWarmupBatchSize(250);
        config.setWarmupDelay(5000L);
        
        assertTrue(config.getWarmupEnabled());
        assertEquals(250, config.getWarmupBatchSize());
        assertEquals(5000L, config.getWarmupDelay());
    }

    @Test
    @DisplayName("Should handle null cache warming properties")
    void testNullCacheWarmingProperties() {
        config.setWarmupEnabled(null);
        config.setWarmupBatchSize(null);
        config.setWarmupDelay(null);
        
        assertNull(config.getWarmupEnabled());
        assertNull(config.getWarmupBatchSize());
        assertNull(config.getWarmupDelay());
    }

    // ========================================
    // Distributed Cache Property Tests
    // ========================================

    @Test
    @DisplayName("Should set and get distributed cache properties")
    void testDistributedCacheProperties() {
        config.setDistributedCache(true);
        config.setCacheCluster("redis-cluster");
        config.setReplicationFactor(3);
        
        assertTrue(config.getDistributedCache());
        assertEquals("redis-cluster", config.getCacheCluster());
        assertEquals(3, config.getReplicationFactor());
    }

    @Test
    @DisplayName("Should handle null distributed cache properties")
    void testNullDistributedCacheProperties() {
        config.setDistributedCache(null);
        config.setCacheCluster(null);
        config.setReplicationFactor(null);

        assertNull(config.getDistributedCache());
        assertNull(config.getCacheCluster());
        assertNull(config.getReplicationFactor());
    }

    // ========================================
    // Utility Method Tests
    // ========================================

    @Test
    @DisplayName("Should convert TTL seconds to milliseconds")
    void testTtlMillisecondsConversion() {
        config.setTtlSeconds(60L);
        assertEquals(60000L, config.getTtlMilliseconds());

        config.setTtlSeconds(0L);
        assertEquals(0L, config.getTtlMilliseconds());

        config.setTtlSeconds(null);
        assertEquals(0L, config.getTtlMilliseconds());
    }

    @Test
    @DisplayName("Should convert max idle seconds to milliseconds")
    void testMaxIdleMillisecondsConversion() {
        config.setMaxIdleSeconds(30L);
        assertEquals(30000L, config.getMaxIdleMilliseconds());

        config.setMaxIdleSeconds(0L);
        assertEquals(0L, config.getMaxIdleMilliseconds());

        config.setMaxIdleSeconds(null);
        assertEquals(0L, config.getMaxIdleMilliseconds());
    }

    @Test
    @DisplayName("Should calculate refresh ahead threshold")
    void testRefreshAheadThresholdCalculation() {
        config.setTtlSeconds(100L);
        config.setRefreshAheadFactor(75L);
        assertEquals(75L, config.getRefreshAheadThresholdSeconds());

        config.setTtlSeconds(3600L);
        config.setRefreshAheadFactor(80L);
        assertEquals(2880L, config.getRefreshAheadThresholdSeconds()); // 3600 * 80 / 100

        config.setTtlSeconds(1000L);
        config.setRefreshAheadFactor(50L);
        assertEquals(500L, config.getRefreshAheadThresholdSeconds());
    }

    @Test
    @DisplayName("Should handle null values in refresh ahead threshold calculation")
    void testRefreshAheadThresholdWithNulls() {
        config.setTtlSeconds(null);
        config.setRefreshAheadFactor(75L);
        assertEquals(0L, config.getRefreshAheadThresholdSeconds());

        config.setTtlSeconds(100L);
        config.setRefreshAheadFactor(null);
        assertEquals(0L, config.getRefreshAheadThresholdSeconds());

        config.setTtlSeconds(null);
        config.setRefreshAheadFactor(null);
        assertEquals(0L, config.getRefreshAheadThresholdSeconds());
    }

    // ========================================
    // Validation Tests
    // ========================================

    @Test
    @DisplayName("Should validate successfully with valid configuration")
    void testValidConfiguration() {
        config.setTtlSeconds(3600L);
        config.setMaxIdleSeconds(1800L);
        config.setMaxSize(10000);
        config.setRefreshAheadFactor(75L);
        config.setWarmupBatchSize(100);
        config.setWarmupDelay(0L);
        config.setReplicationFactor(1);

        assertDoesNotThrow(() -> config.validate());
    }

    @Test
    @DisplayName("Should fail validation with negative TTL")
    void testValidationNegativeTtl() {
        config.setTtlSeconds(-1L);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> config.validate());
        assertEquals("TTL seconds must be positive", exception.getMessage());
    }

    @Test
    @DisplayName("Should fail validation with zero TTL")
    void testValidationZeroTtl() {
        config.setTtlSeconds(0L);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> config.validate());
        assertEquals("TTL seconds must be positive", exception.getMessage());
    }

    @Test
    @DisplayName("Should fail validation with negative max idle seconds")
    void testValidationNegativeMaxIdle() {
        config.setMaxIdleSeconds(-1L);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> config.validate());
        assertEquals("Max idle seconds must be positive", exception.getMessage());
    }

    @Test
    @DisplayName("Should fail validation with zero max idle seconds")
    void testValidationZeroMaxIdle() {
        config.setMaxIdleSeconds(0L);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> config.validate());
        assertEquals("Max idle seconds must be positive", exception.getMessage());
    }

    @Test
    @DisplayName("Should fail validation with negative max size")
    void testValidationNegativeMaxSize() {
        config.setMaxSize(-1);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> config.validate());
        assertEquals("Max cache size must be positive", exception.getMessage());
    }

    @Test
    @DisplayName("Should fail validation with zero max size")
    void testValidationZeroMaxSize() {
        config.setMaxSize(0);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> config.validate());
        assertEquals("Max cache size must be positive", exception.getMessage());
    }

    @Test
    @DisplayName("Should fail validation with invalid refresh ahead factor")
    void testValidationInvalidRefreshAheadFactor() {
        config.setRefreshAheadFactor(0L);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> config.validate());
        assertEquals("Refresh ahead factor must be between 1 and 99", exception.getMessage());

        config.setRefreshAheadFactor(100L);

        exception = assertThrows(IllegalArgumentException.class,
            () -> config.validate());
        assertEquals("Refresh ahead factor must be between 1 and 99", exception.getMessage());

        config.setRefreshAheadFactor(-10L);

        exception = assertThrows(IllegalArgumentException.class,
            () -> config.validate());
        assertEquals("Refresh ahead factor must be between 1 and 99", exception.getMessage());
    }

    @Test
    @DisplayName("Should allow valid refresh ahead factor values")
    void testValidationValidRefreshAheadFactor() {
        config.setRefreshAheadFactor(1L);
        assertDoesNotThrow(() -> config.validate());

        config.setRefreshAheadFactor(50L);
        assertDoesNotThrow(() -> config.validate());

        config.setRefreshAheadFactor(99L);
        assertDoesNotThrow(() -> config.validate());
    }

    @Test
    @DisplayName("Should fail validation with negative warmup batch size")
    void testValidationNegativeWarmupBatchSize() {
        config.setWarmupBatchSize(-1);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> config.validate());
        assertEquals("Warmup batch size must be positive", exception.getMessage());
    }

    @Test
    @DisplayName("Should fail validation with zero warmup batch size")
    void testValidationZeroWarmupBatchSize() {
        config.setWarmupBatchSize(0);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> config.validate());
        assertEquals("Warmup batch size must be positive", exception.getMessage());
    }

    @Test
    @DisplayName("Should fail validation with negative warmup delay")
    void testValidationNegativeWarmupDelay() {
        config.setWarmupDelay(-1L);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> config.validate());
        assertEquals("Warmup delay cannot be negative", exception.getMessage());
    }

    @Test
    @DisplayName("Should allow zero warmup delay")
    void testValidationZeroWarmupDelay() {
        config.setWarmupDelay(0L);
        assertDoesNotThrow(() -> config.validate());
    }

    @Test
    @DisplayName("Should fail validation with negative replication factor")
    void testValidationNegativeReplicationFactor() {
        config.setReplicationFactor(-1);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> config.validate());
        assertEquals("Replication factor must be positive", exception.getMessage());
    }

    @Test
    @DisplayName("Should fail validation with zero replication factor")
    void testValidationZeroReplicationFactor() {
        config.setReplicationFactor(0);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> config.validate());
        assertEquals("Replication factor must be positive", exception.getMessage());
    }

    @Test
    @DisplayName("Should handle null values in validation")
    void testValidationWithNullValues() {
        config.setTtlSeconds(null);
        config.setMaxIdleSeconds(null);
        config.setMaxSize(null);
        config.setRefreshAheadFactor(null);
        config.setWarmupBatchSize(null);
        config.setWarmupDelay(null);
        config.setReplicationFactor(null);

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
        config.setTtlSeconds(7200L);
        config.setMaxIdleSeconds(3600L);
        config.setMaxSize(5000);
        config.setEvictionPolicy(EvictionPolicy.LFU);
        config.setPreloadEnabled(true);
        config.setRefreshAhead(true);
        config.setRefreshAheadFactor(80L);
        config.setStatisticsEnabled(false);
        config.setKeyPrefix("test:");
        config.setCompressionEnabled(true);
        config.setSerializationFormat("protobuf");
        config.setWarmupEnabled(true);
        config.setWarmupBatchSize(250);
        config.setWarmupDelay(5000L);
        config.setDistributedCache(true);
        config.setCacheCluster("redis-cluster");
        config.setReplicationFactor(3);

        // Create copy
        CacheConfig copy = config.copy();

        // Verify all properties are copied
        assertEquals(config.getEnabled(), copy.getEnabled());
        assertEquals(config.getTtlSeconds(), copy.getTtlSeconds());
        assertEquals(config.getMaxIdleSeconds(), copy.getMaxIdleSeconds());
        assertEquals(config.getMaxSize(), copy.getMaxSize());
        assertEquals(config.getEvictionPolicy(), copy.getEvictionPolicy());
        assertEquals(config.getPreloadEnabled(), copy.getPreloadEnabled());
        assertEquals(config.getRefreshAhead(), copy.getRefreshAhead());
        assertEquals(config.getRefreshAheadFactor(), copy.getRefreshAheadFactor());
        assertEquals(config.getStatisticsEnabled(), copy.getStatisticsEnabled());
        assertEquals(config.getKeyPrefix(), copy.getKeyPrefix());
        assertEquals(config.getCompressionEnabled(), copy.getCompressionEnabled());
        assertEquals(config.getSerializationFormat(), copy.getSerializationFormat());
        assertEquals(config.getWarmupEnabled(), copy.getWarmupEnabled());
        assertEquals(config.getWarmupBatchSize(), copy.getWarmupBatchSize());
        assertEquals(config.getWarmupDelay(), copy.getWarmupDelay());
        assertEquals(config.getDistributedCache(), copy.getDistributedCache());
        assertEquals(config.getCacheCluster(), copy.getCacheCluster());
        assertEquals(config.getReplicationFactor(), copy.getReplicationFactor());
    }

    @Test
    @DisplayName("Should handle null values in copy method")
    void testCopyWithNullValues() {
        // Create a config with explicit null values (overriding defaults)
        config.setEnabled(null);
        config.setTtlSeconds(null);
        config.setMaxIdleSeconds(null);
        config.setMaxSize(null);
        config.setEvictionPolicy(null);
        config.setPreloadEnabled(null);
        config.setRefreshAhead(null);
        config.setRefreshAheadFactor(null);
        config.setStatisticsEnabled(null);
        config.setKeyPrefix(null);
        config.setCompressionEnabled(null);
        config.setSerializationFormat(null);
        config.setWarmupEnabled(null);
        config.setWarmupBatchSize(null);
        config.setWarmupDelay(null);
        config.setDistributedCache(null);
        config.setCacheCluster(null);
        config.setReplicationFactor(null);

        CacheConfig copy = config.copy();

        // All properties should be null in the copy
        assertNull(copy.getEnabled());
        assertNull(copy.getTtlSeconds());
        assertNull(copy.getMaxIdleSeconds());
        assertNull(copy.getMaxSize());
        assertNull(copy.getEvictionPolicy());
        assertNull(copy.getPreloadEnabled());
        assertNull(copy.getRefreshAhead());
        assertNull(copy.getRefreshAheadFactor());
        assertNull(copy.getStatisticsEnabled());
        assertNull(copy.getKeyPrefix());
        assertNull(copy.getCompressionEnabled());
        assertNull(copy.getSerializationFormat());
        assertNull(copy.getWarmupEnabled());
        assertNull(copy.getWarmupBatchSize());
        assertNull(copy.getWarmupDelay());
        assertNull(copy.getDistributedCache());
        assertNull(copy.getCacheCluster());
        assertNull(copy.getReplicationFactor());
    }

    @Test
    @DisplayName("Should create independent copy that can be modified")
    void testCopyIndependence() {
        config.setEnabled(true);
        config.setTtlSeconds(3600L);
        config.setKeyPrefix("original:");

        CacheConfig copy = config.copy();

        // Modify original
        config.setEnabled(false);
        config.setTtlSeconds(7200L);
        config.setKeyPrefix("modified:");

        // Copy should remain unchanged
        assertTrue(copy.getEnabled());
        assertEquals(3600L, copy.getTtlSeconds());
        assertEquals("original:", copy.getKeyPrefix());

        // Modify copy
        copy.setEnabled(null);
        copy.setTtlSeconds(1800L);
        copy.setKeyPrefix("copy:");

        // Original should remain unchanged
        assertFalse(config.getEnabled());
        assertEquals(7200L, config.getTtlSeconds());
        assertEquals("modified:", config.getKeyPrefix());
    }

    // ========================================
    // Equals and HashCode Tests
    // ========================================

    @Test
    @DisplayName("Should be equal to itself")
    void testEqualsReflexive() {
        config.setEnabled(true);
        config.setTtlSeconds(3600L);

        assertEquals(config, config);
        assertEquals(config.hashCode(), config.hashCode());
    }

    @Test
    @DisplayName("Should be equal to another instance with same properties")
    void testEqualsSymmetric() {
        config.setEnabled(true);
        config.setTtlSeconds(3600L);
        config.setMaxSize(10000);
        config.setEvictionPolicy(EvictionPolicy.LRU);

        CacheConfig other = new CacheConfig();
        other.setEnabled(true);
        other.setTtlSeconds(3600L);
        other.setMaxSize(10000);
        other.setEvictionPolicy(EvictionPolicy.LRU);

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

        assertNotEquals(config, "not a CacheConfig");
        assertNotEquals(config, new Object());
    }

    @Test
    @DisplayName("Should not be equal when enabled flags differ")
    void testEqualsEnabledDifference() {
        config.setEnabled(true);
        config.setTtlSeconds(3600L);

        CacheConfig other = new CacheConfig();
        other.setEnabled(false);
        other.setTtlSeconds(3600L);

        assertNotEquals(config, other);
    }

    @Test
    @DisplayName("Should not be equal when TTL differs")
    void testEqualsTtlDifference() {
        config.setEnabled(true);
        config.setTtlSeconds(3600L);

        CacheConfig other = new CacheConfig();
        other.setEnabled(true);
        other.setTtlSeconds(7200L);

        assertNotEquals(config, other);
    }

    @Test
    @DisplayName("Should not be equal when max size differs")
    void testEqualsMaxSizeDifference() {
        config.setEnabled(true);
        config.setMaxSize(10000);

        CacheConfig other = new CacheConfig();
        other.setEnabled(true);
        other.setMaxSize(5000);

        assertNotEquals(config, other);
    }

    @Test
    @DisplayName("Should not be equal when eviction policy differs")
    void testEqualsEvictionPolicyDifference() {
        config.setEnabled(true);
        config.setEvictionPolicy(EvictionPolicy.LRU);

        CacheConfig other = new CacheConfig();
        other.setEnabled(true);
        other.setEvictionPolicy(EvictionPolicy.LFU);

        assertNotEquals(config, other);
    }

    @Test
    @DisplayName("Should handle null values in equals comparison")
    void testEqualsWithNullValues() {
        CacheConfig config1 = new CacheConfig();
        CacheConfig config2 = new CacheConfig();

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

        // Test with explicit null values
        config1.setEnabled(null);
        config1.setTtlSeconds(null);
        config1.setMaxSize(null);
        config1.setEvictionPolicy(null);

        config2.setEnabled(null);
        config2.setTtlSeconds(null);
        config2.setMaxSize(null);
        config2.setEvictionPolicy(null);

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
        config.setTtlSeconds(7200L);
        config.setMaxSize(5000);
        config.setEvictionPolicy(EvictionPolicy.LFU);
        config.setPreloadEnabled(true);
        config.setStatisticsEnabled(false);

        String result = config.toString();

        assertNotNull(result);
        assertTrue(result.contains("false")); // enabled
        assertTrue(result.contains("7200")); // ttlSeconds
        assertTrue(result.contains("5000")); // maxSize
        assertTrue(result.contains("LFU")); // evictionPolicy
        assertTrue(result.contains("true")); // preloadEnabled
        assertTrue(result.contains("CacheConfig"));
    }

    @Test
    @DisplayName("Should handle null values in toString")
    void testToStringWithNulls() {
        // Set properties to null explicitly
        config.setEnabled(null);
        config.setTtlSeconds(null);
        config.setMaxSize(null);
        config.setEvictionPolicy(null);
        config.setPreloadEnabled(null);
        config.setStatisticsEnabled(null);

        String result = config.toString();

        assertNotNull(result);
        assertTrue(result.contains("CacheConfig"));
        assertTrue(result.contains("null")); // null values should appear
    }

    @Test
    @DisplayName("Should be consistent toString output")
    void testToStringConsistency() {
        config.setEnabled(true);
        config.setTtlSeconds(3600L);
        config.setMaxSize(10000);

        String result1 = config.toString();
        String result2 = config.toString();

        assertEquals(result1, result2);
    }

    // ========================================
    // EvictionPolicy Enum Tests
    // ========================================

    @Test
    @DisplayName("Should have correct eviction policy enum values")
    void testEvictionPolicyEnumValues() {
        EvictionPolicy[] policies = EvictionPolicy.values();
        assertEquals(5, policies.length);

        assertEquals(EvictionPolicy.LRU, policies[0]);
        assertEquals(EvictionPolicy.LFU, policies[1]);
        assertEquals(EvictionPolicy.FIFO, policies[2]);
        assertEquals(EvictionPolicy.RANDOM, policies[3]);
        assertEquals(EvictionPolicy.TTL_BASED, policies[4]);
    }

    @Test
    @DisplayName("Should have correct eviction policy codes")
    void testEvictionPolicyCodes() {
        assertEquals("LRU", EvictionPolicy.LRU.getCode());
        assertEquals("LFU", EvictionPolicy.LFU.getCode());
        assertEquals("FIFO", EvictionPolicy.FIFO.getCode());
        assertEquals("RANDOM", EvictionPolicy.RANDOM.getCode());
        assertEquals("TTL_BASED", EvictionPolicy.TTL_BASED.getCode());
    }

    @Test
    @DisplayName("Should have correct eviction policy descriptions")
    void testEvictionPolicyDescriptions() {
        assertEquals("Least Recently Used", EvictionPolicy.LRU.getDescription());
        assertEquals("Least Frequently Used", EvictionPolicy.LFU.getDescription());
        assertEquals("First In, First Out", EvictionPolicy.FIFO.getDescription());
        assertEquals("Random eviction", EvictionPolicy.RANDOM.getDescription());
        assertEquals("Time-to-live based", EvictionPolicy.TTL_BASED.getDescription());
    }

    @Test
    @DisplayName("Should have non-null properties for all eviction policy enum values")
    void testEvictionPolicyNonNullProperties() {
        for (EvictionPolicy policy : EvictionPolicy.values()) {
            assertNotNull(policy.getCode());
            assertNotNull(policy.getDescription());

            assertFalse(policy.getCode().isEmpty());
            assertFalse(policy.getDescription().isEmpty());
        }
    }

    @Test
    @DisplayName("Should have unique eviction policy codes")
    void testEvictionPolicyUniqueCodes() {
        EvictionPolicy[] policies = EvictionPolicy.values();

        for (int i = 0; i < policies.length; i++) {
            for (int j = i + 1; j < policies.length; j++) {
                assertNotEquals(policies[i].getCode(), policies[j].getCode(),
                    "Eviction policy codes should be unique: " + policies[i] + " vs " + policies[j]);
            }
        }
    }

    @Test
    @DisplayName("Should set and get all eviction policy types")
    void testAllEvictionPolicyTypes() {
        for (EvictionPolicy policy : EvictionPolicy.values()) {
            config.setEvictionPolicy(policy);
            assertEquals(policy, config.getEvictionPolicy());
        }
    }
}
