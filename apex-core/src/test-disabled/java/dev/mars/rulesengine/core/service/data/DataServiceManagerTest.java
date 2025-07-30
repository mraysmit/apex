package dev.mars.apex.core.service.data;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
 * Test class for DataServiceManager.
 *
 * This class is part of the PeeGeeQ message queue system, providing
 * production-ready PostgreSQL-based message queuing capabilities.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-07-27
 * @version 1.0
 */
/**
 * Test class for DataServiceManager.
 */
public class DataServiceManagerTest {
    private DataServiceManager dataServiceManager;
    private MockDataSource mockDataSource;
    private MockDataSource anotherMockDataSource;

    @BeforeEach
    public void setUp() {
        dataServiceManager = new DataServiceManager();
        mockDataSource = new MockDataSource("TestDataSource", "testData");
        anotherMockDataSource = new MockDataSource("AnotherDataSource", "anotherData");
    }

    @Test
    public void testConstructor() {
        // Verify that a new DataServiceManager can be created
        assertNotNull(dataServiceManager);
    }

    @Test
    public void testLoadDataSource() {
        // Load a data source
        dataServiceManager.loadDataSource(mockDataSource);

        // Verify the data source was loaded
        DataSource loadedDataSource = dataServiceManager.getDataSourceByName("TestDataSource");
        assertNotNull(loadedDataSource);
        assertEquals("TestDataSource", loadedDataSource.getName());
        assertEquals("testData", loadedDataSource.getDataType());
    }

    @Test
    public void testLoadDataSourceWithNullDataSource() {
        // Load a null data source
        dataServiceManager.loadDataSource(null);

        // Verify no data source was loaded
        DataSource loadedDataSource = dataServiceManager.getDataSourceByName("TestDataSource");
        assertNull(loadedDataSource);
    }

    @Test
    public void testLoadDataSourceWithNullName() {
        // Create a mock data source with null name
        MockDataSource nullNameDataSource = new MockDataSource(null, "testData");

        // Load the data source
        dataServiceManager.loadDataSource(nullNameDataSource);

        // Verify no data source was loaded
        DataSource loadedDataSource = dataServiceManager.getDataSourceByType("testData");
        assertNull(loadedDataSource);
    }

    @Test
    public void testLoadDataSourceWithEmptyName() {
        // Create a mock data source with empty name
        MockDataSource emptyNameDataSource = new MockDataSource("", "testData");

        // Load the data source
        dataServiceManager.loadDataSource(emptyNameDataSource);

        // Verify no data source was loaded
        DataSource loadedDataSource = dataServiceManager.getDataSourceByType("testData");
        assertNull(loadedDataSource);
    }

    @Test
    public void testLoadDataSourceWithNullDataType() {
        // Create a mock data source with null data type
        MockDataSource nullDataTypeDataSource = new MockDataSource("TestDataSource", null);

        // Load the data source
        dataServiceManager.loadDataSource(nullDataTypeDataSource);

        // Verify no data source was loaded
        DataSource loadedDataSource = dataServiceManager.getDataSourceByName("TestDataSource");
        assertNull(loadedDataSource);
    }

    @Test
    public void testLoadDataSourceWithEmptyDataType() {
        // Create a mock data source with empty data type
        MockDataSource emptyDataTypeDataSource = new MockDataSource("TestDataSource", "");

        // Load the data source
        dataServiceManager.loadDataSource(emptyDataTypeDataSource);

        // Verify no data source was loaded
        DataSource loadedDataSource = dataServiceManager.getDataSourceByName("TestDataSource");
        assertNull(loadedDataSource);
    }

    @Test
    public void testLoadDataSources() {
        // Load multiple data sources
        dataServiceManager.loadDataSources(mockDataSource, anotherMockDataSource);

        // Verify the data sources were loaded
        DataSource loadedDataSource1 = dataServiceManager.getDataSourceByName("TestDataSource");
        assertNotNull(loadedDataSource1);
        assertEquals("TestDataSource", loadedDataSource1.getName());
        assertEquals("testData", loadedDataSource1.getDataType());

        DataSource loadedDataSource2 = dataServiceManager.getDataSourceByName("AnotherDataSource");
        assertNotNull(loadedDataSource2);
        assertEquals("AnotherDataSource", loadedDataSource2.getName());
        assertEquals("anotherData", loadedDataSource2.getDataType());
    }

    @Test
    public void testLoadDataSourcesWithNullDataSources() {
        // Load null data sources
        dataServiceManager.loadDataSources(null);

        // Verify no data sources were loaded
        DataSource loadedDataSource = dataServiceManager.getDataSourceByName("TestDataSource");
        assertNull(loadedDataSource);
    }

    @Test
    public void testLoadDataSourcesWithNullDataSource() {
        // Load data sources with a null data source
        dataServiceManager.loadDataSources(mockDataSource, null);

        // Verify only the non-null data source was loaded
        DataSource loadedDataSource1 = dataServiceManager.getDataSourceByName("TestDataSource");
        assertNotNull(loadedDataSource1);
        assertEquals("TestDataSource", loadedDataSource1.getName());
        assertEquals("testData", loadedDataSource1.getDataType());

        DataSource loadedDataSource2 = dataServiceManager.getDataSourceByName("AnotherDataSource");
        assertNull(loadedDataSource2);
    }

    @Test
    public void testGetDataSourceByName() {
        // Load a data source
        dataServiceManager.loadDataSource(mockDataSource);

        // Get the data source by name
        DataSource loadedDataSource = dataServiceManager.getDataSourceByName("TestDataSource");
        assertNotNull(loadedDataSource);
        assertEquals("TestDataSource", loadedDataSource.getName());
        assertEquals("testData", loadedDataSource.getDataType());
    }

    @Test
    public void testGetDataSourceByNameWithNonExistentName() {
        // Get a data source with a non-existent name
        DataSource loadedDataSource = dataServiceManager.getDataSourceByName("NonExistentDataSource");
        assertNull(loadedDataSource);
    }

    @Test
    public void testGetDataSourceByType() {
        // Load a data source
        dataServiceManager.loadDataSource(mockDataSource);

        // Get the data source by type
        DataSource loadedDataSource = dataServiceManager.getDataSourceByType("testData");
        assertNotNull(loadedDataSource);
        assertEquals("TestDataSource", loadedDataSource.getName());
        assertEquals("testData", loadedDataSource.getDataType());
    }

    @Test
    public void testGetDataSourceByTypeWithNonExistentType() {
        // Get a data source with a non-existent type
        DataSource loadedDataSource = dataServiceManager.getDataSourceByType("NonExistentDataType");
        assertNull(loadedDataSource);
    }

    @Test
    public void testRequestDataByName() {
        // Load a data source
        dataServiceManager.loadDataSource(mockDataSource);

        // Add some test data
        List<String> testData = new ArrayList<>();
        testData.add("Test Data 1");
        testData.add("Test Data 2");
        mockDataSource.setData(testData);

        // Request data by name
        List<String> requestedData = dataServiceManager.requestDataByName("TestDataSource", "testData");
        assertNotNull(requestedData);
        assertEquals(2, requestedData.size());
        assertEquals("Test Data 1", requestedData.get(0));
        assertEquals("Test Data 2", requestedData.get(1));
    }

    @Test
    public void testRequestDataByNameWithNonExistentName() {
        // Request data with a non-existent name
        List<String> requestedData = dataServiceManager.requestDataByName("NonExistentDataSource", "testData");
        assertNull(requestedData);
    }

    @Test
    public void testRequestDataByNameWithUnsupportedDataType() {
        // Load a data source
        dataServiceManager.loadDataSource(mockDataSource);

        // Request data with an unsupported data type
        List<String> requestedData = dataServiceManager.requestDataByName("TestDataSource", "unsupportedDataType");
        assertNull(requestedData);
    }

    @Test
    public void testRequestData() {
        // Load a data source
        dataServiceManager.loadDataSource(mockDataSource);

        // Add some test data
        List<String> testData = new ArrayList<>();
        testData.add("Test Data 1");
        testData.add("Test Data 2");
        mockDataSource.setData(testData);

        // Request data by type
        List<String> requestedData = dataServiceManager.requestData("testData");
        assertNotNull(requestedData);
        assertEquals(2, requestedData.size());
        assertEquals("Test Data 1", requestedData.get(0));
        assertEquals("Test Data 2", requestedData.get(1));
    }

    @Test
    public void testRequestDataWithNonExistentType() {
        // Request data with a non-existent type
        List<String> requestedData = dataServiceManager.requestData("NonExistentDataType");
        assertNull(requestedData);
    }

    @Test
    public void testRequestDataWithParameters() {
        // Load a data source
        dataServiceManager.loadDataSource(mockDataSource);

        // Add some test data
        Map<String, Object> parameterizedData = new HashMap<>();
        List<String> filteredData = new ArrayList<>();
        filteredData.add("Filtered Data 1");
        filteredData.add("Filtered Data 2");
        parameterizedData.put("filter1", filteredData);
        mockDataSource.setParameterizedData(parameterizedData);

        // Request data with parameters
        List<String> requestedData = dataServiceManager.requestData("testData", "filter1");
        assertNotNull(requestedData);
        assertEquals(2, requestedData.size());
        assertEquals("Filtered Data 1", requestedData.get(0));
        assertEquals("Filtered Data 2", requestedData.get(1));
    }

    @Test
    public void testInitializeWithMockData() {
        // Initialize with mock data
        dataServiceManager.initializeWithMockData();

        // This is a placeholder test since the method is currently a placeholder
        // In a real implementation, we would verify that mock data sources were loaded
        assertNotNull(dataServiceManager);
    }

    /**
     * Mock implementation of DataSource for testing.
     */
    private static class MockDataSource implements DataSource {
        private final String name;
        private final String dataType;
        private Object data;
        private Map<String, Object> parameterizedData;

        public MockDataSource(String name, String dataType) {
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
            return this.dataType != null && this.dataType.equals(dataType);
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
