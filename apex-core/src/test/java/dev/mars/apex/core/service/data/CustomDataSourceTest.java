package dev.mars.apex.core.service.data;

import org.junit.jupiter.api.*;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive unit tests for CustomDataSource.
 * 
 * Tests cover:
 * - Constructor validation
 * - Basic data source functionality
 * - Data storage and retrieval
 * - Type checking and validation
 * - Null handling and edge cases
 * - Data modification operations
 * - Thread safety considerations
 * 
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 1.0.0
 */
class CustomDataSourceTest {

    private CustomDataSource dataSource;
    private static final String TEST_NAME = "test-datasource";
    private static final String TEST_DATA_TYPE = "test-data";

    @BeforeEach
    void setUp() {
        dataSource = new CustomDataSource(TEST_NAME, TEST_DATA_TYPE);
    }

    // ========================================
    // Constructor Tests
    // ========================================

    @Test
    @DisplayName("Should create CustomDataSource with valid parameters")
    void testConstructorWithValidParameters() {
        CustomDataSource ds = new CustomDataSource("my-source", "my-type");
        
        assertEquals("my-source", ds.getName());
        assertEquals("my-type", ds.getDataType());
        assertTrue(ds.supportsDataType("my-type"));
    }

    @Test
    @DisplayName("Should handle null parameters in constructor")
    void testConstructorWithNullParameters() {
        // Constructor should accept null values (though not recommended)
        CustomDataSource dsNullName = new CustomDataSource(null, "type");
        CustomDataSource dsNullType = new CustomDataSource("name", null);
        CustomDataSource dsBothNull = new CustomDataSource(null, null);
        
        assertNull(dsNullName.getName());
        assertEquals("type", dsNullName.getDataType());
        
        assertEquals("name", dsNullType.getName());
        assertNull(dsNullType.getDataType());
        
        assertNull(dsBothNull.getName());
        assertNull(dsBothNull.getDataType());
    }

    // ========================================
    // Basic Interface Implementation Tests
    // ========================================

    @Test
    @DisplayName("Should return correct name")
    void testGetName() {
        assertEquals(TEST_NAME, dataSource.getName());
    }

    @Test
    @DisplayName("Should return correct data type")
    void testGetDataType() {
        assertEquals(TEST_DATA_TYPE, dataSource.getDataType());
    }

    @Test
    @DisplayName("Should support configured data type")
    void testSupportsDataType() {
        assertTrue(dataSource.supportsDataType(TEST_DATA_TYPE));
        assertFalse(dataSource.supportsDataType("other-type"));
        assertFalse(dataSource.supportsDataType(""));
        assertFalse(dataSource.supportsDataType(null));
    }

    @Test
    @DisplayName("Should handle case-sensitive data type checking")
    void testSupportsDataTypeCaseSensitive() {
        assertFalse(dataSource.supportsDataType(TEST_DATA_TYPE.toUpperCase()));
        assertFalse(dataSource.supportsDataType("Test-Data"));
        assertFalse(dataSource.supportsDataType("TEST-DATA"));
    }

    // ========================================
    // Data Retrieval Tests
    // ========================================

    @Test
    @DisplayName("Should return null for unsupported data type")
    void testGetDataUnsupportedType() {
        Object result = dataSource.getData("unsupported-type");
        assertNull(result);
    }

    @Test
    @DisplayName("Should return null for null data type")
    void testGetDataNullType() {
        Object result = dataSource.getData(null);
        assertNull(result);
    }

    @Test
    @DisplayName("Should return null when no data is stored")
    void testGetDataNoDataStored() {
        Object result = dataSource.getData(TEST_DATA_TYPE);
        assertNull(result);
    }

    @Test
    @DisplayName("Should ignore parameters in getData")
    void testGetDataIgnoresParameters() {
        // Add some test data
        String testData = "test-value";
        dataSource.addData(TEST_DATA_TYPE, testData);
        
        // Parameters should be ignored
        Object result1 = dataSource.getData(TEST_DATA_TYPE);
        Object result2 = dataSource.getData(TEST_DATA_TYPE, "param1", "param2");
        Object result3 = dataSource.getData(TEST_DATA_TYPE, 123, true, null);
        
        assertEquals(testData, result1);
        assertEquals(testData, result2);
        assertEquals(testData, result3);
    }

    // ========================================
    // Data Storage Tests
    // ========================================

    @Test
    @DisplayName("Should store and retrieve string data")
    void testAddAndGetStringData() {
        String testData = "Hello, World!";
        dataSource.addData(TEST_DATA_TYPE, testData);
        
        String result = dataSource.getData(TEST_DATA_TYPE);
        assertEquals(testData, result);
    }

    @Test
    @DisplayName("Should store and retrieve complex objects")
    void testAddAndGetComplexData() {
        List<String> testData = Arrays.asList("item1", "item2", "item3");
        dataSource.addData(TEST_DATA_TYPE, testData);
        
        @SuppressWarnings("unchecked")
        List<String> result = dataSource.getData(TEST_DATA_TYPE);
        assertEquals(testData, result);
        assertEquals(3, result.size());
        assertTrue(result.contains("item2"));
    }

    @Test
    @DisplayName("Should store and retrieve null data")
    void testAddAndGetNullData() {
        dataSource.addData(TEST_DATA_TYPE, null);
        
        Object result = dataSource.getData(TEST_DATA_TYPE);
        assertNull(result);
    }

    @Test
    @DisplayName("Should overwrite existing data")
    void testOverwriteData() {
        // Add initial data
        String initialData = "initial";
        dataSource.addData(TEST_DATA_TYPE, initialData);
        assertEquals(initialData, dataSource.getData(TEST_DATA_TYPE));
        
        // Overwrite with new data
        String newData = "updated";
        dataSource.addData(TEST_DATA_TYPE, newData);
        assertEquals(newData, dataSource.getData(TEST_DATA_TYPE));
    }

    @Test
    @DisplayName("Should not store data for unsupported type")
    void testAddDataUnsupportedType() {
        String testData = "test-value";
        dataSource.addData("unsupported-type", testData);
        
        // Data should not be stored
        Object result = dataSource.getData("unsupported-type");
        assertNull(result);
        
        // Original type should still work
        dataSource.addData(TEST_DATA_TYPE, testData);
        assertEquals(testData, dataSource.getData(TEST_DATA_TYPE));
    }

    @Test
    @DisplayName("Should handle null data type in addData")
    void testAddDataNullType() {
        String testData = "test-value";
        
        // Should not throw exception, but should not store data
        assertDoesNotThrow(() -> dataSource.addData(null, testData));
        
        // Verify no data was stored
        assertNull(dataSource.getData(TEST_DATA_TYPE));
    }

    // ========================================
    // Data Removal Tests
    // ========================================

    @Test
    @DisplayName("Should remove stored data")
    void testRemoveData() {
        // Add data first
        String testData = "test-value";
        dataSource.addData(TEST_DATA_TYPE, testData);
        assertEquals(testData, dataSource.getData(TEST_DATA_TYPE));
        
        // Remove data
        dataSource.removeData(TEST_DATA_TYPE);
        assertNull(dataSource.getData(TEST_DATA_TYPE));
    }

    @Test
    @DisplayName("Should handle remove for unsupported type")
    void testRemoveDataUnsupportedType() {
        // Add some data first
        String testData = "test-value";
        dataSource.addData(TEST_DATA_TYPE, testData);
        
        // Try to remove unsupported type (should not affect existing data)
        assertDoesNotThrow(() -> dataSource.removeData("unsupported-type"));
        
        // Original data should still be there
        assertEquals(testData, dataSource.getData(TEST_DATA_TYPE));
    }

    @Test
    @DisplayName("Should handle remove for null type")
    void testRemoveDataNullType() {
        // Add some data first
        String testData = "test-value";
        dataSource.addData(TEST_DATA_TYPE, testData);
        
        // Try to remove null type (should not affect existing data)
        assertDoesNotThrow(() -> dataSource.removeData(null));
        
        // Original data should still be there
        assertEquals(testData, dataSource.getData(TEST_DATA_TYPE));
    }

    @Test
    @DisplayName("Should handle remove when no data exists")
    void testRemoveDataWhenEmpty() {
        // Should not throw exception when removing from empty data source
        assertDoesNotThrow(() -> dataSource.removeData(TEST_DATA_TYPE));
        
        // Should still return null
        assertNull(dataSource.getData(TEST_DATA_TYPE));
    }

    // ========================================
    // Clear Data Tests
    // ========================================

    @Test
    @DisplayName("Should clear all data")
    void testClearData() {
        // Add some data
        dataSource.addData(TEST_DATA_TYPE, "test-value");
        assertEquals("test-value", dataSource.getData(TEST_DATA_TYPE));
        
        // Clear all data
        dataSource.clearData();
        assertNull(dataSource.getData(TEST_DATA_TYPE));
    }

    @Test
    @DisplayName("Should handle clear when no data exists")
    void testClearDataWhenEmpty() {
        // Should not throw exception when clearing empty data source
        assertDoesNotThrow(() -> dataSource.clearData());
        
        // Should still return null
        assertNull(dataSource.getData(TEST_DATA_TYPE));
    }

    // ========================================
    // Edge Cases and Error Handling Tests
    // ========================================

    @Test
    @DisplayName("Should handle empty string data type")
    void testEmptyStringDataType() {
        CustomDataSource ds = new CustomDataSource("test", "");
        
        assertEquals("", ds.getDataType());
        assertTrue(ds.supportsDataType(""));
        assertFalse(ds.supportsDataType("non-empty"));
        
        ds.addData("", "test-data");
        assertEquals("test-data", ds.getData(""));
    }

    @Test
    @DisplayName("Should handle whitespace-only data type")
    void testWhitespaceDataType() {
        String whitespaceType = "   ";
        CustomDataSource ds = new CustomDataSource("test", whitespaceType);

        assertEquals(whitespaceType, ds.getDataType());
        assertTrue(ds.supportsDataType(whitespaceType));
        assertFalse(ds.supportsDataType(""));
        assertTrue(ds.supportsDataType("   ")); // Same whitespace should match
        assertFalse(ds.supportsDataType("  ")); // Different number of spaces

        ds.addData(whitespaceType, "test-data");
        assertEquals("test-data", ds.getData(whitespaceType));
    }

    @Test
    @DisplayName("Should handle special characters in data type")
    void testSpecialCharactersInDataType() {
        String specialType = "test-data_type.with@special#chars!";
        CustomDataSource ds = new CustomDataSource("test", specialType);

        assertEquals(specialType, ds.getDataType());
        assertTrue(ds.supportsDataType(specialType));

        ds.addData(specialType, "test-data");
        assertEquals("test-data", ds.getData(specialType));
    }

    // ========================================
    // Thread Safety Tests
    // ========================================

    @Test
    @DisplayName("Should handle concurrent read operations")
    void testConcurrentReads() throws InterruptedException {
        // Add test data
        String testData = "concurrent-test-data";
        dataSource.addData(TEST_DATA_TYPE, testData);

        final int threadCount = 10;
        final int operationsPerThread = 100;
        final List<String> results = Collections.synchronizedList(new ArrayList<>());
        final List<Thread> threads = new ArrayList<>();

        // Create multiple threads that read data concurrently
        for (int i = 0; i < threadCount; i++) {
            Thread thread = new Thread(() -> {
                for (int j = 0; j < operationsPerThread; j++) {
                    String result = dataSource.getData(TEST_DATA_TYPE);
                    results.add(result);
                }
            });
            threads.add(thread);
            thread.start();
        }

        // Wait for all threads to complete
        for (Thread thread : threads) {
            thread.join();
        }

        // Verify all reads returned the correct data
        assertEquals(threadCount * operationsPerThread, results.size());
        for (String result : results) {
            assertEquals(testData, result);
        }
    }

    @Test
    @DisplayName("Should handle concurrent write operations")
    void testConcurrentWrites() throws InterruptedException {
        final int threadCount = 10;
        final List<Thread> threads = new ArrayList<>();
        final Set<String> writtenValues = Collections.synchronizedSet(new HashSet<>());

        // Create multiple threads that write data concurrently
        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            Thread thread = new Thread(() -> {
                String value = "thread-" + threadId + "-data";
                writtenValues.add(value);
                dataSource.addData(TEST_DATA_TYPE, value);
            });
            threads.add(thread);
            thread.start();
        }

        // Wait for all threads to complete
        for (Thread thread : threads) {
            thread.join();
        }

        // Verify that one of the written values is stored
        // (Due to race conditions, we can't predict which one)
        String finalValue = dataSource.getData(TEST_DATA_TYPE);
        assertNotNull(finalValue);
        assertTrue(writtenValues.contains(finalValue));
    }

    @Test
    @DisplayName("Should handle concurrent read/write operations")
    void testConcurrentReadWrite() throws InterruptedException {
        // Initialize with some data
        dataSource.addData(TEST_DATA_TYPE, "initial-data");

        final int readerThreads = 5;
        final int writerThreads = 3;
        final int operationsPerThread = 50;
        final List<String> readResults = Collections.synchronizedList(new ArrayList<>());
        final List<Thread> threads = new ArrayList<>();

        // Create reader threads
        for (int i = 0; i < readerThreads; i++) {
            Thread thread = new Thread(() -> {
                for (int j = 0; j < operationsPerThread; j++) {
                    String result = dataSource.getData(TEST_DATA_TYPE);
                    if (result != null) {
                        readResults.add(result);
                    }
                }
            });
            threads.add(thread);
            thread.start();
        }

        // Create writer threads
        for (int i = 0; i < writerThreads; i++) {
            final int threadId = i;
            Thread thread = new Thread(() -> {
                for (int j = 0; j < operationsPerThread; j++) {
                    String value = "writer-" + threadId + "-op-" + j;
                    dataSource.addData(TEST_DATA_TYPE, value);
                }
            });
            threads.add(thread);
            thread.start();
        }

        // Wait for all threads to complete
        for (Thread thread : threads) {
            thread.join();
        }

        // Verify that reads returned valid data (no null values due to race conditions)
        assertFalse(readResults.isEmpty());
        for (String result : readResults) {
            assertNotNull(result);
            assertTrue(result.startsWith("initial-data") || result.startsWith("writer-"));
        }
    }

    // ========================================
    // Performance and Stress Tests
    // ========================================

    @Test
    @DisplayName("Should handle large data objects")
    void testLargeDataObjects() {
        // Create a large list
        List<String> largeList = new ArrayList<>();
        for (int i = 0; i < 10000; i++) {
            largeList.add("item-" + i);
        }

        // Store and retrieve large data
        dataSource.addData(TEST_DATA_TYPE, largeList);

        @SuppressWarnings("unchecked")
        List<String> result = dataSource.getData(TEST_DATA_TYPE);

        assertNotNull(result);
        assertEquals(10000, result.size());
        assertEquals("item-0", result.get(0));
        assertEquals("item-9999", result.get(9999));
    }

    @Test
    @DisplayName("Should handle multiple data operations efficiently")
    void testMultipleOperationsPerformance() {
        final int operationCount = 1000;

        long startTime = System.currentTimeMillis();

        // Perform many add/get operations
        for (int i = 0; i < operationCount; i++) {
            String data = "data-" + i;
            dataSource.addData(TEST_DATA_TYPE, data);
            String result = dataSource.getData(TEST_DATA_TYPE);
            assertEquals(data, result);
        }

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        // Should complete within reasonable time (adjust threshold as needed)
        assertTrue(duration < 1000, "Operations should complete within 1 second, took: " + duration + "ms");
    }

    // ========================================
    // Integration with DataSource Interface Tests
    // ========================================

    @Test
    @DisplayName("Should work correctly as DataSource interface")
    void testAsDataSourceInterface() {
        // Test polymorphic usage
        dev.mars.apex.core.service.data.DataSource ds = dataSource;

        assertEquals(TEST_NAME, ds.getName());
        assertEquals(TEST_DATA_TYPE, ds.getDataType());
        assertTrue(ds.supportsDataType(TEST_DATA_TYPE));
        assertFalse(ds.supportsDataType("other-type"));

        // Test data operations through interface
        String testData = "interface-test-data";
        dataSource.addData(TEST_DATA_TYPE, testData); // Use concrete class for add

        String result = ds.getData(TEST_DATA_TYPE);
        assertEquals(testData, result);

        // Test lookup method (default implementation)
        Object lookupResult = ds.lookup(TEST_DATA_TYPE);
        assertEquals(testData, lookupResult);
    }

    @Test
    @DisplayName("Should handle toString representation")
    void testToStringRepresentation() {
        String toString = dataSource.toString();
        assertNotNull(toString);
        // Should contain class name
        assertTrue(toString.contains("CustomDataSource"));
    }
}
