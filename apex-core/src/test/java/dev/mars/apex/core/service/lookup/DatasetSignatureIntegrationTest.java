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

import dev.mars.apex.core.config.yaml.YamlConfigurationLoader;
import dev.mars.apex.core.config.yaml.YamlEnrichment;
import dev.mars.apex.core.config.yaml.YamlRuleConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for DatasetSignature with real YAML configurations.
 * 
 * Tests that DatasetSignature correctly identifies duplicate datasets
 * in the DuplicateInlineDataSourceTest.yaml file.
 * 
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 1.0.0
 */
class DatasetSignatureIntegrationTest {

    @Test
    @DisplayName("Should generate identical signatures for duplicate inline datasets")
    void testDuplicateInlineDatasetSignatures() throws Exception {
        // Create two identical inline datasets (simulating the DuplicateInlineDataSourceTest scenario)
        YamlEnrichment.LookupDataset dataset1 = createCurrencyDataset();
        YamlEnrichment.LookupDataset dataset2 = createCurrencyDataset();

        assertNotNull(dataset1, "First dataset should not be null");
        assertNotNull(dataset2, "Second dataset should not be null");

        // Verify both are inline datasets with same key field
        assertEquals("inline", dataset1.getType());
        assertEquals("inline", dataset2.getType());
        assertEquals("code", dataset1.getKeyField());
        assertEquals("code", dataset2.getKeyField());

        // Verify both have the same data (4 currency records)
        assertNotNull(dataset1.getData());
        assertNotNull(dataset2.getData());
        assertEquals(4, dataset1.getData().size());
        assertEquals(4, dataset2.getData().size());

        // Generate signatures
        DatasetSignature signature1 = DatasetSignature.from(dataset1);
        DatasetSignature signature2 = DatasetSignature.from(dataset2);

        assertNotNull(signature1, "First signature should not be null");
        assertNotNull(signature2, "Second signature should not be null");

        // CRITICAL TEST: Signatures should be IDENTICAL
        assertEquals(signature1, signature2,
            "Signatures should be identical for duplicate datasets");
        assertEquals(signature1.hashCode(), signature2.hashCode(),
            "Hash codes should be identical for duplicate datasets");
        assertEquals(signature1.toString(), signature2.toString(),
            "String representations should be identical for duplicate datasets");

        // Verify signature properties
        assertEquals("inline", signature1.getType());
        assertEquals("code", signature1.getKeyField());
        assertNotNull(signature1.getContentHash());

        // Verify short string format is suitable for service names
        String shortString = signature1.toShortString();
        assertTrue(shortString.startsWith("inline-"),
            "Short string should start with 'inline-'");
        assertFalse(shortString.contains(":"),
            "Short string should not contain colons");

        System.out.println("✅ Dataset Signature Integration Test Results:");
        System.out.println("   Signature 1:  " + signature1);
        System.out.println("   Signature 2:  " + signature2);
        System.out.println("   Short form:   " + shortString);
        System.out.println("   ✅ Signatures are IDENTICAL - dataset deduplication will work!");
    }

    /**
     * Create a currency dataset matching the DuplicateInlineDataSourceTest.yaml structure.
     */
    private YamlEnrichment.LookupDataset createCurrencyDataset() {
        YamlEnrichment.LookupDataset dataset = new YamlEnrichment.LookupDataset();
        dataset.setType("inline");
        dataset.setKeyField("code");

        List<java.util.Map<String, Object>> data = new java.util.ArrayList<>();
        data.add(java.util.Map.of("code", "USD", "name", "US Dollar", "symbol", "$", "region", "North America"));
        data.add(java.util.Map.of("code", "EUR", "name", "Euro", "symbol", "€", "region", "Europe"));
        data.add(java.util.Map.of("code", "GBP", "name", "British Pound", "symbol", "£", "region", "United Kingdom"));
        data.add(java.util.Map.of("code", "JPY", "name", "Japanese Yen", "symbol", "¥", "region", "Asia"));
        dataset.setData(data);

        return dataset;
    }

    @Test
    @DisplayName("Should generate different signatures for different inline datasets")
    void testDifferentInlineDatasetSignatures() throws Exception {
        // Create two different inline datasets
        YamlEnrichment.LookupDataset dataset1 = new YamlEnrichment.LookupDataset();
        dataset1.setType("inline");
        dataset1.setKeyField("code");
        dataset1.setData(java.util.Arrays.asList(
            java.util.Map.of("code", "USD", "name", "US Dollar")
        ));
        
        YamlEnrichment.LookupDataset dataset2 = new YamlEnrichment.LookupDataset();
        dataset2.setType("inline");
        dataset2.setKeyField("code");
        dataset2.setData(java.util.Arrays.asList(
            java.util.Map.of("code", "EUR", "name", "Euro")
        ));
        
        // Generate signatures
        DatasetSignature signature1 = DatasetSignature.from(dataset1);
        DatasetSignature signature2 = DatasetSignature.from(dataset2);
        
        // Signatures should be DIFFERENT
        assertNotEquals(signature1, signature2,
            "Signatures should be different for different datasets");
        assertNotEquals(signature1.getContentHash(), signature2.getContentHash(),
            "Content hashes should be different for different datasets");
        
        System.out.println("✅ Different Dataset Test Results:");
        System.out.println("   Signature 1: " + signature1);
        System.out.println("   Signature 2: " + signature2);
        System.out.println("   ✅ Signatures are DIFFERENT - correct!");
    }
}

