package dev.mars.apex.core.service.lookup;

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

import dev.mars.apex.core.config.yaml.YamlEnrichment;
import dev.mars.apex.core.test.extension.ColoredTestOutputExtension;
import dev.mars.apex.core.test.extension.TestClassLoggingExtension;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test to verify that empty dataset errors are correctly suppressed for database and rest-api types
 * (which use wrapper pattern with intentionally empty datasets) but logged as ERROR for inline types.
 * 
 * This test proves the fix for the architectural pattern where DatabaseDatasetLookupService and 
 * RestApiDatasetLookupService extend DatasetLookupService with intentionally empty datasets as wrappers.
 * 
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2.1
 */
@ExtendWith({ColoredTestOutputExtension.class, TestClassLoggingExtension.class})
class EmptyDatasetWarningSuppressionTest {

    private ByteArrayOutputStream logCapture;
    private PrintStream originalErr;

    @BeforeEach
    void setUp() {
        // Capture System.err to catch log output
        logCapture = new ByteArrayOutputStream();
        originalErr = System.err;
        System.setErr(new PrintStream(logCapture));
    }

    @AfterEach
    void tearDown() {
        // Restore original System.err
        System.setErr(originalErr);
    }

    @Test
    @DisplayName("Should NOT error for empty database-type dataset (wrapper pattern)")
    void testDatabaseTypeEmptyDatasetNoWarning() {
        // Create database-type dataset with intentionally empty data (wrapper pattern)
        YamlEnrichment.LookupDataset dataset = new YamlEnrichment.LookupDataset();
        dataset.setType("database");
        dataset.setKeyField("key");
        dataset.setData(Collections.emptyList()); // Intentionally empty for wrapper
        
        // Create service - this should NOT log an error
        DatasetLookupService service = new DatasetLookupService("test-db-service", dataset);
        
        // Verify no error was logged
        String logOutput = logCapture.toString();
        assertFalse(logOutput.contains("Dataset has no data records"), 
            "Database-type dataset should NOT log error for empty data (wrapper pattern). Log: " + logOutput);
        
        // Verify service was created successfully
        assertNotNull(service);
        assertEquals("test-db-service", service.getName());
    }

    @Test
    @DisplayName("Should NOT error for empty rest-api-type dataset (wrapper pattern)")
    void testRestApiTypeEmptyDatasetNoWarning() {
        // Create rest-api-type dataset with intentionally empty data (wrapper pattern)
        YamlEnrichment.LookupDataset dataset = new YamlEnrichment.LookupDataset();
        dataset.setType("rest-api");
        dataset.setKeyField("key");
        dataset.setData(Collections.emptyList()); // Intentionally empty for wrapper
        
        // Create service - this should NOT log an error
        DatasetLookupService service = new DatasetLookupService("test-rest-service", dataset);
        
        // Verify no error was logged
        String logOutput = logCapture.toString();
        assertFalse(logOutput.contains("Dataset has no data records"), 
            "REST API-type dataset should NOT log error for empty data (wrapper pattern). Log: " + logOutput);
        
        // Verify service was created successfully
        assertNotNull(service);
        assertEquals("test-rest-service", service.getName());
    }

    @Test
    @DisplayName("Should ERROR for empty inline-type dataset (broken configuration)")
    void testInlineTypeEmptyDatasetWarns() {
        // Clear previous log capture
        logCapture.reset();
        
        // Create inline-type dataset with empty data (this is a configuration error)
        YamlEnrichment.LookupDataset dataset = new YamlEnrichment.LookupDataset();
        dataset.setType("inline");
        dataset.setKeyField("key");
        dataset.setData(Collections.emptyList()); // Empty inline dataset is an error
        
        // Create service - this SHOULD log an ERROR
        DatasetLookupService service = new DatasetLookupService("test-inline-service", dataset);
        
        // Verify ERROR was logged
        String logOutput = logCapture.toString();
        assertTrue(logOutput.contains("Dataset has no data records"), 
            "Inline-type dataset SHOULD log ERROR for empty data (configuration error). Log: " + logOutput);
        
        // Verify service was created successfully (but with empty dataset)
        assertNotNull(service);
        assertEquals("test-inline-service", service.getName());
    }

    @Test
    @DisplayName("Should ERROR for empty yaml-file-type dataset (broken configuration)")
    void testYamlFileTypeEmptyDatasetWarns() {
        // Clear previous log capture
        logCapture.reset();
        
        // Create yaml-file-type dataset with empty data (this is a configuration error)
        YamlEnrichment.LookupDataset dataset = new YamlEnrichment.LookupDataset();
        dataset.setType("yaml-file");
        dataset.setKeyField("key");
        dataset.setData(Collections.emptyList()); // Empty yaml-file dataset is an error
        
        // Create service - this SHOULD log an ERROR
        DatasetLookupService service = new DatasetLookupService("test-yaml-file-service", dataset);
        
        // Verify ERROR was logged
        String logOutput = logCapture.toString();
        assertTrue(logOutput.contains("Dataset has no data records"), 
            "YAML file-type dataset SHOULD log ERROR for empty data (configuration error). Log: " + logOutput);
        
        // Verify service was created successfully (but with empty dataset)
        assertNotNull(service);
        assertEquals("test-yaml-file-service", service.getName());
    }

    @Test
    @DisplayName("Should handle null dataset type gracefully (default to error)")
    void testNullTypeEmptyDatasetWarns() {
        // Clear previous log capture
        logCapture.reset();
        
        // Create dataset with null type and empty data
        YamlEnrichment.LookupDataset dataset = new YamlEnrichment.LookupDataset();
        dataset.setType(null); // Null type
        dataset.setKeyField("key");
        dataset.setData(Collections.emptyList());
        
        // Create service - this SHOULD log an ERROR (safe default)
        DatasetLookupService service = new DatasetLookupService("test-null-type-service", dataset);
        
        // Verify ERROR was logged (null type defaults to error behavior)
        String logOutput = logCapture.toString();
        assertTrue(logOutput.contains("Dataset has no data records"), 
            "Null-type dataset SHOULD log ERROR for empty data (safe default behavior). Log: " + logOutput);
        
        // Verify service was created successfully
        assertNotNull(service);
        assertEquals("test-null-type-service", service.getName());
    }
}
