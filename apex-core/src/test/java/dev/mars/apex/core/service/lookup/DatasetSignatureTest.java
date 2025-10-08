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

import dev.mars.apex.core.config.yaml.YamlEnrichment.LookupDataset;
import dev.mars.apex.core.config.yaml.YamlEnrichment.LookupDataset.ParameterMapping;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for DatasetSignature class.
 * 
 * Tests cover:
 * - Signature generation for all dataset types (inline, file, database, REST API)
 * - Hash stability (same data = same signature)
 * - Equality and hashCode contract
 * - Edge cases (null, empty data)
 * - Error handling
 * 
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 1.0.0
 */
class DatasetSignatureTest {

    @Test
    @DisplayName("Should create signature for inline dataset")
    void testInlineDatasetSignature() {
        // Create inline dataset
        LookupDataset dataset = new LookupDataset();
        dataset.setType("inline");
        dataset.setKeyField("code");
        
        List<Map<String, Object>> data = new ArrayList<>();
        Map<String, Object> record1 = new HashMap<>();
        record1.put("code", "USD");
        record1.put("name", "US Dollar");
        data.add(record1);
        
        Map<String, Object> record2 = new HashMap<>();
        record2.put("code", "EUR");
        record2.put("name", "Euro");
        data.add(record2);
        
        dataset.setData(data);
        
        // Generate signature
        DatasetSignature signature = DatasetSignature.from(dataset);
        
        // Verify
        assertNotNull(signature);
        assertEquals("inline", signature.getType());
        assertEquals("code", signature.getKeyField());
        assertNotNull(signature.getContentHash());
        assertFalse(signature.getContentHash().isEmpty());
        
        // Verify string representations
        assertTrue(signature.toShortString().startsWith("inline-"));
        assertTrue(signature.toString().contains("inline"));
        assertTrue(signature.toString().contains("code"));
    }

    @Test
    @DisplayName("Should generate same signature for identical inline data")
    void testInlineDatasetHashStability() {
        // Create first dataset
        LookupDataset dataset1 = createInlineDataset();
        
        // Create second identical dataset
        LookupDataset dataset2 = createInlineDataset();
        
        // Generate signatures
        DatasetSignature sig1 = DatasetSignature.from(dataset1);
        DatasetSignature sig2 = DatasetSignature.from(dataset2);
        
        // Verify they are equal
        assertEquals(sig1, sig2);
        assertEquals(sig1.hashCode(), sig2.hashCode());
        assertEquals(sig1.toString(), sig2.toString());
    }

    @Test
    @DisplayName("Should generate different signatures for different inline data")
    void testInlineDatasetDifferentData() {
        // Create first dataset
        LookupDataset dataset1 = new LookupDataset();
        dataset1.setType("inline");
        dataset1.setKeyField("code");
        dataset1.setData(Arrays.asList(createRecord("USD", "US Dollar")));
        
        // Create second dataset with different data
        LookupDataset dataset2 = new LookupDataset();
        dataset2.setType("inline");
        dataset2.setKeyField("code");
        dataset2.setData(Arrays.asList(createRecord("EUR", "Euro")));
        
        // Generate signatures
        DatasetSignature sig1 = DatasetSignature.from(dataset1);
        DatasetSignature sig2 = DatasetSignature.from(dataset2);
        
        // Verify they are different
        assertNotEquals(sig1, sig2);
        assertNotEquals(sig1.getContentHash(), sig2.getContentHash());
    }

    @Test
    @DisplayName("Should create signature for file-based dataset")
    void testFileBasedDatasetSignature() {
        LookupDataset dataset = new LookupDataset();
        dataset.setType("yaml-file");
        dataset.setKeyField("id");
        dataset.setFilePath("data/currencies.yaml");
        
        DatasetSignature signature = DatasetSignature.from(dataset);
        
        assertNotNull(signature);
        assertEquals("yaml-file", signature.getType());
        assertEquals("id", signature.getKeyField());
        assertTrue(signature.getContentHash().contains("currencies.yaml"));
    }

    @Test
    @DisplayName("Should generate same signature for same file path")
    void testFileBasedHashStability() {
        LookupDataset dataset1 = new LookupDataset();
        dataset1.setType("csv-file");
        dataset1.setKeyField("code");
        dataset1.setFilePath("data/test.csv");
        
        LookupDataset dataset2 = new LookupDataset();
        dataset2.setType("csv-file");
        dataset2.setKeyField("code");
        dataset2.setFilePath("data/test.csv");
        
        DatasetSignature sig1 = DatasetSignature.from(dataset1);
        DatasetSignature sig2 = DatasetSignature.from(dataset2);
        
        assertEquals(sig1, sig2);
    }

    @Test
    @DisplayName("Should create signature for database dataset")
    void testDatabaseDatasetSignature() {
        LookupDataset dataset = new LookupDataset();
        dataset.setType("database");
        dataset.setKeyField("id");
        dataset.setConnectionName("main-db");
        dataset.setQuery("SELECT * FROM currencies WHERE code = :code");
        
        ParameterMapping param = new ParameterMapping();
        param.setField("currencyCode");
        param.setType("string");
        dataset.setParameters(Arrays.asList(param));
        
        DatasetSignature signature = DatasetSignature.from(dataset);
        
        assertNotNull(signature);
        assertEquals("database", signature.getType());
        assertEquals("id", signature.getKeyField());
        assertNotNull(signature.getContentHash());
    }

    @Test
    @DisplayName("Should generate same signature for identical database config")
    void testDatabaseHashStability() {
        LookupDataset dataset1 = createDatabaseDataset();
        LookupDataset dataset2 = createDatabaseDataset();
        
        DatasetSignature sig1 = DatasetSignature.from(dataset1);
        DatasetSignature sig2 = DatasetSignature.from(dataset2);
        
        assertEquals(sig1, sig2);
    }

    @Test
    @DisplayName("Should create signature for REST API dataset")
    void testRestApiDatasetSignature() {
        LookupDataset dataset = new LookupDataset();
        dataset.setType("rest-api");
        dataset.setKeyField("userId");
        dataset.setConnectionName("user-api");
        dataset.setEndpoint("/users/{id}");
        dataset.setOperationRef("getUserById");
        
        DatasetSignature signature = DatasetSignature.from(dataset);
        
        assertNotNull(signature);
        assertEquals("rest-api", signature.getType());
        assertEquals("userId", signature.getKeyField());
        assertNotNull(signature.getContentHash());
    }

    @Test
    @DisplayName("Should generate same signature for identical REST API config")
    void testRestApiHashStability() {
        LookupDataset dataset1 = createRestApiDataset();
        LookupDataset dataset2 = createRestApiDataset();
        
        DatasetSignature sig1 = DatasetSignature.from(dataset1);
        DatasetSignature sig2 = DatasetSignature.from(dataset2);
        
        assertEquals(sig1, sig2);
    }

    @Test
    @DisplayName("Should handle empty inline data")
    void testEmptyInlineData() {
        LookupDataset dataset = new LookupDataset();
        dataset.setType("inline");
        dataset.setKeyField("code");
        dataset.setData(new ArrayList<>());
        
        DatasetSignature signature = DatasetSignature.from(dataset);
        
        assertNotNull(signature);
        assertEquals("empty", signature.getContentHash());
    }

    @Test
    @DisplayName("Should handle null inline data")
    void testNullInlineData() {
        LookupDataset dataset = new LookupDataset();
        dataset.setType("inline");
        dataset.setKeyField("code");
        dataset.setData(null);
        
        DatasetSignature signature = DatasetSignature.from(dataset);
        
        assertNotNull(signature);
        assertEquals("empty", signature.getContentHash());
    }

    @Test
    @DisplayName("Should throw exception for null dataset")
    void testNullDataset() {
        assertThrows(IllegalArgumentException.class, () -> {
            DatasetSignature.from(null);
        });
    }

    @Test
    @DisplayName("Should throw exception for null dataset type")
    void testNullDatasetType() {
        LookupDataset dataset = new LookupDataset();
        dataset.setType(null);
        dataset.setKeyField("code");
        
        assertThrows(IllegalArgumentException.class, () -> {
            DatasetSignature.from(dataset);
        });
    }

    @Test
    @DisplayName("Should handle null key field gracefully")
    void testNullKeyField() {
        LookupDataset dataset = new LookupDataset();
        dataset.setType("inline");
        dataset.setKeyField(null);
        dataset.setData(Arrays.asList(createRecord("USD", "US Dollar")));
        
        // Should not throw, but log warning
        DatasetSignature signature = DatasetSignature.from(dataset);
        
        assertNotNull(signature);
        assertEquals("unknown", signature.getKeyField());
    }

    @Test
    @DisplayName("Should verify equals and hashCode contract")
    void testEqualsAndHashCode() {
        DatasetSignature sig1 = DatasetSignature.from(createInlineDataset());
        DatasetSignature sig2 = DatasetSignature.from(createInlineDataset());
        DatasetSignature sig3 = DatasetSignature.from(createDatabaseDataset());
        
        // Reflexive
        assertEquals(sig1, sig1);
        
        // Symmetric
        assertEquals(sig1, sig2);
        assertEquals(sig2, sig1);
        
        // Transitive (sig1 == sig2, sig2 == sig1, therefore sig1 == sig1)
        assertEquals(sig1, sig2);
        assertEquals(sig2, sig1);
        assertEquals(sig1, sig1);
        
        // Consistent hashCode
        assertEquals(sig1.hashCode(), sig2.hashCode());
        
        // Not equal to different signature
        assertNotEquals(sig1, sig3);
        
        // Not equal to null
        assertNotEquals(sig1, null);
        
        // Not equal to different type
        assertNotEquals(sig1, "string");
    }

    // Helper methods

    private LookupDataset createInlineDataset() {
        LookupDataset dataset = new LookupDataset();
        dataset.setType("inline");
        dataset.setKeyField("code");
        
        List<Map<String, Object>> data = new ArrayList<>();
        data.add(createRecord("USD", "US Dollar"));
        data.add(createRecord("EUR", "Euro"));
        dataset.setData(data);
        
        return dataset;
    }

    private LookupDataset createDatabaseDataset() {
        LookupDataset dataset = new LookupDataset();
        dataset.setType("database");
        dataset.setKeyField("id");
        dataset.setConnectionName("test-db");
        dataset.setQuery("SELECT * FROM test WHERE id = :id");
        
        ParameterMapping param = new ParameterMapping();
        param.setField("id");
        param.setType("string");
        dataset.setParameters(Arrays.asList(param));
        
        return dataset;
    }

    private LookupDataset createRestApiDataset() {
        LookupDataset dataset = new LookupDataset();
        dataset.setType("rest-api");
        dataset.setKeyField("userId");
        dataset.setConnectionName("api");
        dataset.setEndpoint("/users");
        dataset.setOperationRef("getUser");
        
        return dataset;
    }

    private Map<String, Object> createRecord(String code, String name) {
        Map<String, Object> record = new HashMap<>();
        record.put("code", code);
        record.put("name", name);
        return record;
    }
}

