package dev.mars.apex.core.service.data.external;

import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive unit tests for DataSourceType.
 * 
 * Tests cover:
 * - Enum values and properties
 * - Configuration value mapping
 * - Capability checks (real-time, batch, transactions, caching)
 * - Utility methods and string representation
 * - Edge cases and null handling
 * - Case sensitivity handling
 * 
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 1.0.0
 */
class DataSourceTypeTest {

    // ========================================
    // Enum Values and Properties Tests
    // ========================================

    @Test
    @DisplayName("Should have correct number of enum values")
    void testEnumValues() {
        DataSourceType[] types = DataSourceType.values();
        assertEquals(6, types.length);
        
        assertEquals(DataSourceType.DATABASE, types[0]);
        assertEquals(DataSourceType.REST_API, types[1]);
        assertEquals(DataSourceType.MESSAGE_QUEUE, types[2]);
        assertEquals(DataSourceType.FILE_SYSTEM, types[3]);
        assertEquals(DataSourceType.CACHE, types[4]);
        assertEquals(DataSourceType.CUSTOM, types[5]);
    }

    @Test
    @DisplayName("Should have correct config values")
    void testConfigValues() {
        assertEquals("database", DataSourceType.DATABASE.getConfigValue());
        assertEquals("rest-api", DataSourceType.REST_API.getConfigValue());
        assertEquals("message-queue", DataSourceType.MESSAGE_QUEUE.getConfigValue());
        assertEquals("file-system", DataSourceType.FILE_SYSTEM.getConfigValue());
        assertEquals("cache", DataSourceType.CACHE.getConfigValue());
        assertEquals("custom", DataSourceType.CUSTOM.getConfigValue());
    }

    @Test
    @DisplayName("Should have correct display names")
    void testDisplayNames() {
        assertEquals("Database", DataSourceType.DATABASE.getDisplayName());
        assertEquals("REST API", DataSourceType.REST_API.getDisplayName());
        assertEquals("Message Queue", DataSourceType.MESSAGE_QUEUE.getDisplayName());
        assertEquals("File System", DataSourceType.FILE_SYSTEM.getDisplayName());
        assertEquals("Cache", DataSourceType.CACHE.getDisplayName());
        assertEquals("Custom", DataSourceType.CUSTOM.getDisplayName());
    }

    @Test
    @DisplayName("Should have correct descriptions")
    void testDescriptions() {
        assertEquals("JDBC-based database connections", DataSourceType.DATABASE.getDescription());
        assertEquals("HTTP REST API endpoints", DataSourceType.REST_API.getDescription());
        assertEquals("Message queue systems", DataSourceType.MESSAGE_QUEUE.getDescription());
        assertEquals("File-based data sources", DataSourceType.FILE_SYSTEM.getDescription());
        assertEquals("In-memory cache systems", DataSourceType.CACHE.getDescription());
        assertEquals("Custom data source implementations", DataSourceType.CUSTOM.getDescription());
    }

    // ========================================
    // Configuration Value Mapping Tests
    // ========================================

    @Test
    @DisplayName("Should find data source type by config value")
    void testFromConfigValue() {
        assertEquals(DataSourceType.DATABASE, DataSourceType.fromConfigValue("database"));
        assertEquals(DataSourceType.REST_API, DataSourceType.fromConfigValue("rest-api"));
        assertEquals(DataSourceType.MESSAGE_QUEUE, DataSourceType.fromConfigValue("message-queue"));
        assertEquals(DataSourceType.FILE_SYSTEM, DataSourceType.fromConfigValue("file-system"));
        assertEquals(DataSourceType.CACHE, DataSourceType.fromConfigValue("cache"));
        assertEquals(DataSourceType.CUSTOM, DataSourceType.fromConfigValue("custom"));
    }

    @Test
    @DisplayName("Should handle case insensitive config value lookup")
    void testFromConfigValueCaseInsensitive() {
        assertEquals(DataSourceType.DATABASE, DataSourceType.fromConfigValue("DATABASE"));
        assertEquals(DataSourceType.DATABASE, DataSourceType.fromConfigValue("Database"));
        assertEquals(DataSourceType.DATABASE, DataSourceType.fromConfigValue("DaTaBaSe"));
        
        assertEquals(DataSourceType.REST_API, DataSourceType.fromConfigValue("REST-API"));
        assertEquals(DataSourceType.REST_API, DataSourceType.fromConfigValue("Rest-Api"));
        
        assertEquals(DataSourceType.MESSAGE_QUEUE, DataSourceType.fromConfigValue("MESSAGE-QUEUE"));
        assertEquals(DataSourceType.MESSAGE_QUEUE, DataSourceType.fromConfigValue("Message-Queue"));
        
        assertEquals(DataSourceType.FILE_SYSTEM, DataSourceType.fromConfigValue("FILE-SYSTEM"));
        assertEquals(DataSourceType.FILE_SYSTEM, DataSourceType.fromConfigValue("File-System"));
        
        assertEquals(DataSourceType.CACHE, DataSourceType.fromConfigValue("CACHE"));
        assertEquals(DataSourceType.CACHE, DataSourceType.fromConfigValue("Cache"));
        
        assertEquals(DataSourceType.CUSTOM, DataSourceType.fromConfigValue("CUSTOM"));
        assertEquals(DataSourceType.CUSTOM, DataSourceType.fromConfigValue("Custom"));
    }

    @Test
    @DisplayName("Should return null for unknown config values")
    void testFromConfigValueUnknown() {
        assertNull(DataSourceType.fromConfigValue("unknown"));
        assertNull(DataSourceType.fromConfigValue("invalid"));
        assertNull(DataSourceType.fromConfigValue("nosql"));
        assertNull(DataSourceType.fromConfigValue(""));
        assertNull(DataSourceType.fromConfigValue("   "));
    }

    @Test
    @DisplayName("Should return null for null config value")
    void testFromConfigValueNull() {
        assertNull(DataSourceType.fromConfigValue(null));
    }

    // ========================================
    // Real-time Support Tests
    // ========================================

    @Test
    @DisplayName("Should correctly identify real-time support")
    void testSupportsRealTime() {
        assertTrue(DataSourceType.DATABASE.supportsRealTime());
        assertTrue(DataSourceType.REST_API.supportsRealTime());
        assertTrue(DataSourceType.MESSAGE_QUEUE.supportsRealTime());
        assertFalse(DataSourceType.FILE_SYSTEM.supportsRealTime()); // Typically requires polling
        assertTrue(DataSourceType.CACHE.supportsRealTime());
        assertTrue(DataSourceType.CUSTOM.supportsRealTime());
    }

    @Test
    @DisplayName("Should support real-time for most types")
    void testRealTimeSupportedTypes() {
        assertTrue(DataSourceType.DATABASE.supportsRealTime());
        assertTrue(DataSourceType.REST_API.supportsRealTime());
        assertTrue(DataSourceType.MESSAGE_QUEUE.supportsRealTime());
        assertTrue(DataSourceType.CACHE.supportsRealTime());
        assertTrue(DataSourceType.CUSTOM.supportsRealTime());
    }

    @Test
    @DisplayName("Should not support real-time for file system")
    void testFileSystemNoRealTime() {
        assertFalse(DataSourceType.FILE_SYSTEM.supportsRealTime());
    }

    // ========================================
    // Batch Operations Support Tests
    // ========================================

    @Test
    @DisplayName("Should correctly identify batch operations support")
    void testSupportsBatchOperations() {
        assertTrue(DataSourceType.DATABASE.supportsBatchOperations());
        assertFalse(DataSourceType.REST_API.supportsBatchOperations());
        assertTrue(DataSourceType.MESSAGE_QUEUE.supportsBatchOperations());
        assertTrue(DataSourceType.FILE_SYSTEM.supportsBatchOperations());
        assertFalse(DataSourceType.CACHE.supportsBatchOperations());
        assertTrue(DataSourceType.CUSTOM.supportsBatchOperations());
    }

    @Test
    @DisplayName("Should support batch operations for specific types")
    void testBatchOperationsSupportedTypes() {
        assertTrue(DataSourceType.DATABASE.supportsBatchOperations());
        assertTrue(DataSourceType.MESSAGE_QUEUE.supportsBatchOperations());
        assertTrue(DataSourceType.FILE_SYSTEM.supportsBatchOperations());
        assertTrue(DataSourceType.CUSTOM.supportsBatchOperations());
    }

    @Test
    @DisplayName("Should not support batch operations for some types")
    void testBatchOperationsNotSupportedTypes() {
        assertFalse(DataSourceType.REST_API.supportsBatchOperations());
        assertFalse(DataSourceType.CACHE.supportsBatchOperations());
    }

    // ========================================
    // Transaction Support Tests
    // ========================================

    @Test
    @DisplayName("Should correctly identify transaction support")
    void testSupportsTransactions() {
        assertTrue(DataSourceType.DATABASE.supportsTransactions());
        assertFalse(DataSourceType.REST_API.supportsTransactions());
        assertTrue(DataSourceType.MESSAGE_QUEUE.supportsTransactions());
        assertFalse(DataSourceType.FILE_SYSTEM.supportsTransactions());
        assertFalse(DataSourceType.CACHE.supportsTransactions());
        assertFalse(DataSourceType.CUSTOM.supportsTransactions());
    }

    @Test
    @DisplayName("Should support transactions for database and message queue")
    void testTransactionsSupportedTypes() {
        assertTrue(DataSourceType.DATABASE.supportsTransactions());
        assertTrue(DataSourceType.MESSAGE_QUEUE.supportsTransactions());
    }

    @Test
    @DisplayName("Should not support transactions for most types")
    void testTransactionsNotSupportedTypes() {
        assertFalse(DataSourceType.REST_API.supportsTransactions());
        assertFalse(DataSourceType.FILE_SYSTEM.supportsTransactions());
        assertFalse(DataSourceType.CACHE.supportsTransactions());
        assertFalse(DataSourceType.CUSTOM.supportsTransactions());
    }

    // ========================================
    // Caching Recommendation Tests
    // ========================================

    @Test
    @DisplayName("Should correctly identify caching recommendations")
    void testRecommendsCaching() {
        assertTrue(DataSourceType.DATABASE.recommendsCaching());
        assertTrue(DataSourceType.REST_API.recommendsCaching());
        assertFalse(DataSourceType.MESSAGE_QUEUE.recommendsCaching());
        assertTrue(DataSourceType.FILE_SYSTEM.recommendsCaching());
        assertFalse(DataSourceType.CACHE.recommendsCaching()); // Already a cache
        assertFalse(DataSourceType.CUSTOM.recommendsCaching());
    }

    @Test
    @DisplayName("Should recommend caching for slow data sources")
    void testCachingRecommendedTypes() {
        assertTrue(DataSourceType.DATABASE.recommendsCaching());
        assertTrue(DataSourceType.REST_API.recommendsCaching());
        assertTrue(DataSourceType.FILE_SYSTEM.recommendsCaching());
    }

    @Test
    @DisplayName("Should not recommend caching for fast or special types")
    void testCachingNotRecommendedTypes() {
        assertFalse(DataSourceType.MESSAGE_QUEUE.recommendsCaching());
        assertFalse(DataSourceType.CACHE.recommendsCaching());
        assertFalse(DataSourceType.CUSTOM.recommendsCaching());
    }

    // ========================================
    // String Representation Tests
    // ========================================

    @Test
    @DisplayName("Should have meaningful toString representation")
    void testToString() {
        assertEquals("Database (database)", DataSourceType.DATABASE.toString());
        assertEquals("REST API (rest-api)", DataSourceType.REST_API.toString());
        assertEquals("Message Queue (message-queue)", DataSourceType.MESSAGE_QUEUE.toString());
        assertEquals("File System (file-system)", DataSourceType.FILE_SYSTEM.toString());
        assertEquals("Cache (cache)", DataSourceType.CACHE.toString());
        assertEquals("Custom (custom)", DataSourceType.CUSTOM.toString());
    }

    @Test
    @DisplayName("Should have consistent toString format for all types")
    void testToStringFormat() {
        for (DataSourceType type : DataSourceType.values()) {
            String toString = type.toString();

            assertTrue(toString.contains(type.getDisplayName()));
            assertTrue(toString.contains(type.getConfigValue()));
            assertTrue(toString.contains("("));
            assertTrue(toString.contains(")"));
            assertEquals(type.getDisplayName() + " (" + type.getConfigValue() + ")", toString);
        }
    }

    // ========================================
    // Comprehensive Capability Matrix Tests
    // ========================================

    @Test
    @DisplayName("Should have correct capability matrix for DATABASE")
    void testDatabaseCapabilities() {
        DataSourceType type = DataSourceType.DATABASE;
        
        assertTrue(type.supportsRealTime());
        assertTrue(type.supportsBatchOperations());
        assertTrue(type.supportsTransactions());
        assertTrue(type.recommendsCaching());
        
        assertEquals("database", type.getConfigValue());
        assertEquals("Database", type.getDisplayName());
        assertEquals("JDBC-based database connections", type.getDescription());
    }

    @Test
    @DisplayName("Should have correct capability matrix for REST_API")
    void testRestApiCapabilities() {
        DataSourceType type = DataSourceType.REST_API;
        
        assertTrue(type.supportsRealTime());
        assertFalse(type.supportsBatchOperations());
        assertFalse(type.supportsTransactions());
        assertTrue(type.recommendsCaching());
        
        assertEquals("rest-api", type.getConfigValue());
        assertEquals("REST API", type.getDisplayName());
        assertEquals("HTTP REST API endpoints", type.getDescription());
    }

    @Test
    @DisplayName("Should have correct capability matrix for MESSAGE_QUEUE")
    void testMessageQueueCapabilities() {
        DataSourceType type = DataSourceType.MESSAGE_QUEUE;
        
        assertTrue(type.supportsRealTime());
        assertTrue(type.supportsBatchOperations());
        assertTrue(type.supportsTransactions());
        assertFalse(type.recommendsCaching());
        
        assertEquals("message-queue", type.getConfigValue());
        assertEquals("Message Queue", type.getDisplayName());
        assertEquals("Message queue systems", type.getDescription());
    }

    @Test
    @DisplayName("Should have correct capability matrix for FILE_SYSTEM")
    void testFileSystemCapabilities() {
        DataSourceType type = DataSourceType.FILE_SYSTEM;
        
        assertFalse(type.supportsRealTime());
        assertTrue(type.supportsBatchOperations());
        assertFalse(type.supportsTransactions());
        assertTrue(type.recommendsCaching());
        
        assertEquals("file-system", type.getConfigValue());
        assertEquals("File System", type.getDisplayName());
        assertEquals("File-based data sources", type.getDescription());
    }

    @Test
    @DisplayName("Should have correct capability matrix for CACHE")
    void testCacheCapabilities() {
        DataSourceType type = DataSourceType.CACHE;
        
        assertTrue(type.supportsRealTime());
        assertFalse(type.supportsBatchOperations());
        assertFalse(type.supportsTransactions());
        assertFalse(type.recommendsCaching());
        
        assertEquals("cache", type.getConfigValue());
        assertEquals("Cache", type.getDisplayName());
        assertEquals("In-memory cache systems", type.getDescription());
    }

    @Test
    @DisplayName("Should have correct capability matrix for CUSTOM")
    void testCustomCapabilities() {
        DataSourceType type = DataSourceType.CUSTOM;
        
        assertTrue(type.supportsRealTime());
        assertTrue(type.supportsBatchOperations());
        assertFalse(type.supportsTransactions());
        assertFalse(type.recommendsCaching());
        
        assertEquals("custom", type.getConfigValue());
        assertEquals("Custom", type.getDisplayName());
        assertEquals("Custom data source implementations", type.getDescription());
    }

    // ========================================
    // Edge Cases and Validation Tests
    // ========================================

    @Test
    @DisplayName("Should handle invalid config values gracefully")
    void testInvalidConfigValues() {
        String[] invalidValues = {"", "   ", "\t", "\n", "null", "undefined", "none"};
        for (String invalidValue : invalidValues) {
            assertNull(DataSourceType.fromConfigValue(invalidValue));
        }
    }

    @Test
    @DisplayName("Should handle config values with extra whitespace")
    void testConfigValuesWithWhitespace() {
        // The current implementation doesn't trim whitespace, so these should return null
        assertNull(DataSourceType.fromConfigValue(" database "));
        assertNull(DataSourceType.fromConfigValue("database "));
        assertNull(DataSourceType.fromConfigValue(" database"));
        assertNull(DataSourceType.fromConfigValue("\tdatabase\t"));
    }

    @Test
    @DisplayName("Should have non-null properties for all enum values")
    void testNonNullProperties() {
        for (DataSourceType type : DataSourceType.values()) {
            assertNotNull(type.getConfigValue());
            assertNotNull(type.getDisplayName());
            assertNotNull(type.getDescription());
            assertNotNull(type.toString());

            assertFalse(type.getConfigValue().isEmpty());
            assertFalse(type.getDisplayName().isEmpty());
            assertFalse(type.getDescription().isEmpty());
            assertFalse(type.toString().isEmpty());
        }
    }

    @Test
    @DisplayName("Should have unique config values")
    void testUniqueConfigValues() {
        DataSourceType[] types = DataSourceType.values();
        
        for (int i = 0; i < types.length; i++) {
            for (int j = i + 1; j < types.length; j++) {
                assertNotEquals(types[i].getConfigValue(), types[j].getConfigValue(),
                    "Config values should be unique: " + types[i] + " vs " + types[j]);
            }
        }
    }

    @Test
    @DisplayName("Should have unique display names")
    void testUniqueDisplayNames() {
        DataSourceType[] types = DataSourceType.values();
        
        for (int i = 0; i < types.length; i++) {
            for (int j = i + 1; j < types.length; j++) {
                assertNotEquals(types[i].getDisplayName(), types[j].getDisplayName(),
                    "Display names should be unique: " + types[i] + " vs " + types[j]);
            }
        }
    }
}
