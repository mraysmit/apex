package dev.mars.apex.core.service.data;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

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
 * Test class for CustomDataSource.
 *
 * This class is part of the PeeGeeQ message queue system, providing
 * production-ready PostgreSQL-based message queuing capabilities.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-07-27
 * @version 1.0
 */
/**
 * Test class for CustomDataSource.
 */
public class CustomDataSourceTest {
    private CustomDataSource customDataSource;
    private CustomDataSource anotherCustomDataSource;

    @BeforeEach
    public void setUp() {
        customDataSource = new CustomDataSource("TestCustomDataSource", "testCustomData");
        anotherCustomDataSource = new CustomDataSource("AnotherCustomDataSource", "anotherCustomData");
    }

    @Test
    public void testConstructor() {
        assertEquals("TestCustomDataSource", customDataSource.getName());
        assertEquals("testCustomData", customDataSource.getDataType());
    }

    @Test
    public void testGetName() {
        assertEquals("TestCustomDataSource", customDataSource.getName());
        assertEquals("AnotherCustomDataSource", anotherCustomDataSource.getName());
    }

    @Test
    public void testGetDataType() {
        assertEquals("testCustomData", customDataSource.getDataType());
        assertEquals("anotherCustomData", anotherCustomDataSource.getDataType());
    }

    @Test
    public void testSupportsDataType() {
        assertTrue(customDataSource.supportsDataType("testCustomData"));
        assertFalse(customDataSource.supportsDataType("unsupportedDataType"));

        // Test with null data type - this should return false
        // because null is not a supported data type
        assertFalse(customDataSource.supportsDataType(null));
    }

    @Test
    public void testGetDataWithNoData() {
        // Test getting data when no data has been added
        Object data = customDataSource.getData("testCustomData");
        assertNull(data);
    }

    @Test
    public void testAddAndGetData() {
        // Create test data
        List<String> testData = new ArrayList<>();
        testData.add("Test Data 1");
        testData.add("Test Data 2");

        // Add data
        customDataSource.addData("testCustomData", testData);

        // Get data
        List<String> retrievedData = customDataSource.getData("testCustomData");
        assertNotNull(retrievedData);
        assertEquals(2, retrievedData.size());
        assertEquals("Test Data 1", retrievedData.get(0));
        assertEquals("Test Data 2", retrievedData.get(1));
    }

    @Test
    public void testAddDataWithUnsupportedDataType() {
        // Create test data
        List<String> testData = new ArrayList<>();
        testData.add("Test Data 1");

        // Add data with unsupported data type
        customDataSource.addData("unsupportedDataType", testData);

        // Verify data was not added
        Object retrievedData = customDataSource.getData("unsupportedDataType");
        assertNull(retrievedData);
    }

    @Test
    public void testRemoveData() {
        // Create and add test data
        List<String> testData = new ArrayList<>();
        testData.add("Test Data 1");
        customDataSource.addData("testCustomData", testData);

        // Verify data was added
        List<String> retrievedData = customDataSource.getData("testCustomData");
        assertNotNull(retrievedData);

        // Remove data
        customDataSource.removeData("testCustomData");

        // Verify data was removed
        Object removedData = customDataSource.getData("testCustomData");
        assertNull(removedData);
    }

    @Test
    public void testRemoveDataWithUnsupportedDataType() {
        // Create and add test data
        List<String> testData = new ArrayList<>();
        testData.add("Test Data 1");
        customDataSource.addData("testCustomData", testData);

        // Remove data with unsupported data type
        customDataSource.removeData("unsupportedDataType");

        // Verify original data is still there
        List<String> retrievedData = customDataSource.getData("testCustomData");
        assertNotNull(retrievedData);
        assertEquals(1, retrievedData.size());
        assertEquals("Test Data 1", retrievedData.get(0));
    }

    @Test
    public void testClearData() {
        // Create and add test data
        List<String> testData1 = new ArrayList<>();
        testData1.add("Test Data 1");
        customDataSource.addData("testCustomData", testData1);

        // Create and add another test data to another data source
        List<String> testData2 = new ArrayList<>();
        testData2.add("Test Data 2");
        anotherCustomDataSource.addData("anotherCustomData", testData2);

        // Clear data from first data source
        customDataSource.clearData();

        // Verify data was cleared from first data source
        Object clearedData = customDataSource.getData("testCustomData");
        assertNull(clearedData);

        // Verify data in second data source is still there
        List<String> retrievedData = anotherCustomDataSource.getData("anotherCustomData");
        assertNotNull(retrievedData);
        assertEquals(1, retrievedData.size());
        assertEquals("Test Data 2", retrievedData.get(0));
    }

    @Test
    public void testGetDataWithUnsupportedDataType() {
        // Create and add test data
        List<String> testData = new ArrayList<>();
        testData.add("Test Data 1");
        customDataSource.addData("testCustomData", testData);

        // Get data with unsupported data type
        Object unsupportedData = customDataSource.getData("unsupportedDataType");
        assertNull(unsupportedData);
    }

    @Test
    public void testAddAndUpdateData() {
        // Create and add initial test data
        List<String> initialData = new ArrayList<>();
        initialData.add("Initial Data");
        customDataSource.addData("testCustomData", initialData);

        // Verify initial data
        List<String> retrievedInitialData = customDataSource.getData("testCustomData");
        assertNotNull(retrievedInitialData);
        assertEquals(1, retrievedInitialData.size());
        assertEquals("Initial Data", retrievedInitialData.get(0));

        // Create and add updated test data
        List<String> updatedData = new ArrayList<>();
        updatedData.add("Updated Data");
        customDataSource.addData("testCustomData", updatedData);

        // Verify updated data
        List<String> retrievedUpdatedData = customDataSource.getData("testCustomData");
        assertNotNull(retrievedUpdatedData);
        assertEquals(1, retrievedUpdatedData.size());
        assertEquals("Updated Data", retrievedUpdatedData.get(0));
    }
}
