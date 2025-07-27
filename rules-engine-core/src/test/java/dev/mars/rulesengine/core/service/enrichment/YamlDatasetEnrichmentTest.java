package dev.mars.rulesengine.core.service.enrichment;

import dev.mars.rulesengine.core.config.yaml.YamlEnrichment;
import dev.mars.rulesengine.core.service.engine.ExpressionEvaluatorService;
import dev.mars.rulesengine.core.service.lookup.LookupServiceRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import testmodels.TestTrade;

import java.util.*;

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
 * Integration tests for YAML dataset-based enrichment.
 *
 * This class is part of the PeeGeeQ message queue system, providing
 * production-ready PostgreSQL-based message queuing capabilities.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-07-27
 * @version 1.0
 */
public class YamlDatasetEnrichmentTest {

    private YamlEnrichmentProcessor processor;
    private LookupServiceRegistry serviceRegistry;

    @BeforeEach
    void setUp() {
        serviceRegistry = new LookupServiceRegistry();
        ExpressionEvaluatorService evaluatorService = new ExpressionEvaluatorService();
        processor = new YamlEnrichmentProcessor(serviceRegistry, evaluatorService);
    }

    @Test
    void testInlineDatasetEnrichment() {
        // Create enrichment with inline currency dataset
        YamlEnrichment enrichment = createCurrencyEnrichment();
        
        // Create test trade object using Map to avoid module system restrictions
        Map<String, Object> trade = new HashMap<>();
        trade.put("notionalCurrency", "USD");

        // Process enrichment
        Object result = processor.processEnrichment(enrichment, trade);

        // Verify enrichment was applied
        assertSame(trade, result);
        assertEquals("US Dollar", trade.get("currencyName"));
        assertEquals(2, trade.get("currencyDecimalPlaces"));
        assertEquals(true, trade.get("currencyActive"));
        assertEquals("North America", trade.get("currencyRegion"));
    }

    @Test
    void testMultipleDatasetEnrichments() {
        // Create multiple enrichments with different datasets
        List<YamlEnrichment> enrichments = Arrays.asList(
            createCurrencyEnrichment(),
            createCountryEnrichment()
        );
        
        // Create test trade object using Map
        Map<String, Object> trade = new HashMap<>();
        trade.put("notionalCurrency", "EUR");
        trade.put("counterpartyJurisdiction", "GB");

        // Process all enrichments
        Object result = processor.processEnrichments(enrichments, trade);

        // Verify both enrichments were applied
        assertSame(trade, result);

        // Currency enrichment
        assertEquals("Euro", trade.get("currencyName"));
        assertEquals(2, trade.get("currencyDecimalPlaces"));
        assertEquals("Europe", trade.get("currencyRegion"));

        // Country enrichment
        assertEquals("United Kingdom", trade.get("jurisdictionName"));
        assertEquals("Europe", trade.get("jurisdictionRegion"));
        assertEquals("UK_REGULATIONS", trade.get("regulatoryRegime"));
    }

    @Test
    void testEnrichmentWithDefaultValues() {
        YamlEnrichment enrichment = createCurrencyEnrichment();
        
        // Create test trade with unknown currency using Map
        Map<String, Object> trade = new HashMap<>();
        trade.put("notionalCurrency", "XYZ"); // Unknown currency

        // Process enrichment
        Object result = processor.processEnrichment(enrichment, trade);

        // Verify default values were applied
        assertSame(trade, result);
        assertEquals("Unknown", trade.get("currencyRegion")); // Default value
        assertEquals(false, trade.get("currencyActive")); // Default value
        assertNull(trade.get("currencyName")); // No default for this field
    }

    @Test
    void testConditionalDatasetEnrichment() {
        YamlEnrichment enrichment = createCurrencyEnrichment();
        
        // Create test trade without currency (condition should fail) using Map
        Map<String, Object> trade = new HashMap<>();
        trade.put("notionalCurrency", null);

        // Process enrichment
        Object result = processor.processEnrichment(enrichment, trade);

        // Verify enrichment was NOT applied due to condition
        assertSame(trade, result);
        assertNull(trade.get("currencyName"));
        assertNull(trade.get("currencyRegion"));
    }

    @Test
    void testMixedServiceAndDatasetEnrichments() {
        // This test would require both service-based and dataset-based enrichments
        // For now, we'll test that dataset enrichments work alongside empty service registry
        
        YamlEnrichment datasetEnrichment = createCurrencyEnrichment();
        
        Map<String, Object> trade = new HashMap<>();
        trade.put("notionalCurrency", "GBP");

        Object result = processor.processEnrichment(datasetEnrichment, trade);

        assertSame(trade, result);
        assertEquals("British Pound", trade.get("currencyName"));
        assertEquals("Europe", trade.get("currencyRegion"));
    }

    @Test
    void testInvalidDatasetConfiguration() {
        // Create enrichment with invalid dataset (no key field)
        YamlEnrichment invalidEnrichment = new YamlEnrichment();
        invalidEnrichment.setId("invalid-enrichment");
        invalidEnrichment.setType("lookup-enrichment");
        invalidEnrichment.setEnabled(true);
        
        YamlEnrichment.LookupConfig lookupConfig = new YamlEnrichment.LookupConfig();
        YamlEnrichment.LookupDataset invalidDataset = new YamlEnrichment.LookupDataset();
        invalidDataset.setType("inline");
        // Missing key field
        invalidDataset.setData(Arrays.asList(Map.of("code", "USD")));
        lookupConfig.setLookupDataset(invalidDataset);
        invalidEnrichment.setLookupConfig(lookupConfig);
        
        TestTrade trade = new TestTrade();
        
        // Should throw exception for invalid configuration
        assertThrows(EnrichmentException.class, () -> {
            processor.processEnrichment(invalidEnrichment, trade);
        });
    }

    private YamlEnrichment createCurrencyEnrichment() {
        YamlEnrichment enrichment = new YamlEnrichment();
        enrichment.setId("currency-dataset-enrichment");
        enrichment.setName("Currency Dataset Enrichment");
        enrichment.setType("lookup-enrichment");
        // No target type restriction for Map objects
        enrichment.setEnabled(true);
        enrichment.setPriority(10);
        enrichment.setCondition("['notionalCurrency'] != null");  // Map syntax for SpEL
        
        // Create lookup configuration with inline dataset
        YamlEnrichment.LookupConfig lookupConfig = new YamlEnrichment.LookupConfig();
        lookupConfig.setLookupKey("['notionalCurrency']");  // Map syntax for SpEL
        
        // Create inline currency dataset
        YamlEnrichment.LookupDataset dataset = new YamlEnrichment.LookupDataset();
        dataset.setType("inline");
        dataset.setKeyField("code");
        
        List<Map<String, Object>> data = Arrays.asList(
            createCurrencyRecord("USD", "US Dollar", 2, true, "North America"),
            createCurrencyRecord("EUR", "Euro", 2, true, "Europe"),
            createCurrencyRecord("GBP", "British Pound", 2, true, "Europe"),
            createCurrencyRecord("JPY", "Japanese Yen", 0, true, "Asia")
        );
        dataset.setData(data);
        
        // Set default values
        Map<String, Object> defaults = new HashMap<>();
        defaults.put("region", "Unknown");
        defaults.put("isActive", false);
        dataset.setDefaultValues(defaults);
        
        lookupConfig.setLookupDataset(dataset);
        enrichment.setLookupConfig(lookupConfig);
        
        // Set field mappings
        List<YamlEnrichment.FieldMapping> fieldMappings = Arrays.asList(
            createFieldMapping("name", "currencyName", false, null),
            createFieldMapping("decimalPlaces", "currencyDecimalPlaces", false, 2),
            createFieldMapping("isActive", "currencyActive", false, false),
            createFieldMapping("region", "currencyRegion", false, "Unknown")
        );
        enrichment.setFieldMappings(fieldMappings);
        
        return enrichment;
    }

    private YamlEnrichment createCountryEnrichment() {
        YamlEnrichment enrichment = new YamlEnrichment();
        enrichment.setId("country-dataset-enrichment");
        enrichment.setName("Country Dataset Enrichment");
        enrichment.setType("lookup-enrichment");
        // No target type restriction for Map objects
        enrichment.setEnabled(true);
        enrichment.setPriority(11);
        enrichment.setCondition("['counterpartyJurisdiction'] != null");  // Map syntax for SpEL
        
        // Create lookup configuration with inline dataset
        YamlEnrichment.LookupConfig lookupConfig = new YamlEnrichment.LookupConfig();
        lookupConfig.setLookupKey("['counterpartyJurisdiction']");  // Map syntax for SpEL
        
        // Create inline country dataset
        YamlEnrichment.LookupDataset dataset = new YamlEnrichment.LookupDataset();
        dataset.setType("inline");
        dataset.setKeyField("alpha2Code");
        
        List<Map<String, Object>> data = Arrays.asList(
            createCountryRecord("US", "United States", "North America", "US_REGULATIONS"),
            createCountryRecord("GB", "United Kingdom", "Europe", "UK_REGULATIONS"),
            createCountryRecord("DE", "Germany", "Europe", "EU_REGULATIONS"),
            createCountryRecord("JP", "Japan", "Asia", "JP_REGULATIONS")
        );
        dataset.setData(data);
        
        lookupConfig.setLookupDataset(dataset);
        enrichment.setLookupConfig(lookupConfig);
        
        // Set field mappings
        List<YamlEnrichment.FieldMapping> fieldMappings = Arrays.asList(
            createFieldMapping("name", "jurisdictionName", false, null),
            createFieldMapping("region", "jurisdictionRegion", false, "Unknown"),
            createFieldMapping("regulatoryRegime", "regulatoryRegime", false, "OTHER")
        );
        enrichment.setFieldMappings(fieldMappings);
        
        return enrichment;
    }

    private Map<String, Object> createCurrencyRecord(String code, String name, int decimals, 
                                                     boolean active, String region) {
        Map<String, Object> record = new HashMap<>();
        record.put("code", code);
        record.put("name", name);
        record.put("decimalPlaces", decimals);
        record.put("isActive", active);
        record.put("region", region);
        return record;
    }

    private Map<String, Object> createCountryRecord(String alpha2Code, String name, 
                                                    String region, String regulatoryRegime) {
        Map<String, Object> record = new HashMap<>();
        record.put("alpha2Code", alpha2Code);
        record.put("name", name);
        record.put("region", region);
        record.put("regulatoryRegime", regulatoryRegime);
        return record;
    }

    private YamlEnrichment.FieldMapping createFieldMapping(String sourceField, String targetField, 
                                                          boolean required, Object defaultValue) {
        YamlEnrichment.FieldMapping mapping = new YamlEnrichment.FieldMapping();
        mapping.setSourceField(sourceField);
        mapping.setTargetField(targetField);
        mapping.setRequired(required);
        mapping.setDefaultValue(defaultValue);
        return mapping;
    }


}
