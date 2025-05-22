package dev.mars.rulesengine.core.service.data;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for DataSource interface.
 * Tests a simple implementation of the DataSource interface.
 */
public class DataSourceTest {
    private TestDataSource dataSource;
    private TestDataSource emptyDataSource;

    @BeforeEach
    public void setUp() {
        dataSource = new TestDataSource("TestDataSource", "testData");
        emptyDataSource = new TestDataSource("EmptyDataSource", "emptyData");
        
        // Initialize test data
        List<String> testData = new ArrayList<>();
        testData.add("Test Data 1");
        testData.add("Test Data 2");
        dataSource.setData(testData);
        
        // Initialize parameterized data
        Map<String, Object> parameterizedData = new HashMap<>();
        List<String> filteredData = new ArrayList<>();
        filteredData.add("Filtered Data 1");
        filteredData.add("Filtered Data 2");
        parameterizedData.put("filter1", filteredData);
        dataSource.setParameterizedData(parameterizedData);
    }

    @Test
    public void testGetName() {
        assertEquals("TestDataSource", dataSource.getName());
        assertEquals("EmptyDataSource", emptyDataSource.getName());
    }

    @Test
    public void testGetDataType() {
        assertEquals("testData", dataSource.getDataType());
        assertEquals("emptyData", emptyDataSource.getDataType());
    }

    @Test
    public void testSupportsDataType() {
        assertTrue(dataSource.supportsDataType("testData"));
        assertFalse(dataSource.supportsDataType("unsupportedDataType"));
        assertFalse(dataSource.supportsDataType(null));
        assertFalse(dataSource.supportsDataType(""));
    }

    @Test
    public void testGetData() {
        // Test getting data without parameters
        List<String> retrievedData = dataSource.getData("testData");
        assertNotNull(retrievedData);
        assertEquals(2, retrievedData.size());
        assertEquals("Test Data 1", retrievedData.get(0));
        assertEquals("Test Data 2", retrievedData.get(1));
        
        // Test getting data with parameters
        List<String> filteredData = dataSource.getData("testData", "filter1");
        assertNotNull(filteredData);
        assertEquals(2, filteredData.size());
        assertEquals("Filtered Data 1", filteredData.get(0));
        assertEquals("Filtered Data 2", filteredData.get(1));
        
        // Test getting data with unsupported data type
        Object unsupportedData = dataSource.getData("unsupportedDataType");
        assertNull(unsupportedData);
        
        // Test getting data from empty data source
        Object emptyData = emptyDataSource.getData("emptyData");
        assertNull(emptyData);
    }

    @Test
    public void testGetDataWithNonExistentParameter() {
        // Test getting data with a parameter that doesn't exist
        Object nonExistentData = dataSource.getData("testData", "nonExistentFilter");
        assertNull(nonExistentData);
    }

    /**
     * Test implementation of DataSource for testing.
     */
    private static class TestDataSource implements DataSource {
        private final String name;
        private final String dataType;
        private Object data;
        private Map<String, Object> parameterizedData;

        public TestDataSource(String name, String dataType) {
            this.name = name;
            this.dataType = dataType;
            this.parameterizedData = new HashMap<>();
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String getDataType() {
            return dataType;
        }

        @Override
        public boolean supportsDataType(String dataType) {
            return this.dataType != null && !this.dataType.isEmpty() && this.dataType.equals(dataType);
        }

        @SuppressWarnings("unchecked")
        @Override
        public <T> T getData(String dataType, Object... parameters) {
            if (!supportsDataType(dataType)) {
                return null;
            }

            if (parameters.length > 0) {
                return (T) parameterizedData.get(parameters[0]);
            }

            return (T) data;
        }

        public void setData(Object data) {
            this.data = data;
        }

        public void setParameterizedData(Map<String, Object> parameterizedData) {
            this.parameterizedData = parameterizedData;
        }
    }
}