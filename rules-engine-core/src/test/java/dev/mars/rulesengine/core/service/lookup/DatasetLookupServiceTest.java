package dev.mars.rulesengine.core.service.lookup;

import dev.mars.rulesengine.core.config.yaml.YamlEnrichment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for DatasetLookupService.
 * Tests inline dataset functionality with various scenarios.
 */
public class DatasetLookupServiceTest {

    private YamlEnrichment.LookupDataset currencyDataset;
    private DatasetLookupService currencyService;

    @BeforeEach
    void setUp() {
        // Create a currency dataset for testing
        currencyDataset = createCurrencyDataset();
        currencyService = new DatasetLookupService("currencyLookupService", currencyDataset);
    }

    private YamlEnrichment.LookupDataset createCurrencyDataset() {
        YamlEnrichment.LookupDataset dataset = new YamlEnrichment.LookupDataset();
        dataset.setType("inline");
        dataset.setKeyField("code");
        
        // Create currency data
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
        
        return dataset;
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

    @Test
    void testSuccessfulLookup() {
        // Test successful lookup
        Object result = currencyService.enrich("USD");
        
        assertNotNull(result);
        assertTrue(result instanceof Map);
        
        @SuppressWarnings("unchecked")
        Map<String, Object> currencyData = (Map<String, Object>) result;
        
        assertEquals("USD", currencyData.get("code"));
        assertEquals("US Dollar", currencyData.get("name"));
        assertEquals(2, currencyData.get("decimalPlaces"));
        assertEquals(true, currencyData.get("isActive"));
        assertEquals("North America", currencyData.get("region"));
    }

    @Test
    void testLookupWithDefaultValues() {
        // Test lookup for non-existent key should return default values
        Object result = currencyService.enrich("XYZ");
        
        assertNotNull(result);
        assertTrue(result instanceof Map);
        
        @SuppressWarnings("unchecked")
        Map<String, Object> defaultData = (Map<String, Object>) result;
        
        assertEquals("Unknown", defaultData.get("region"));
        assertEquals(false, defaultData.get("isActive"));
        assertEquals(2, defaultData.size()); // Only default values
    }

    @Test
    void testLookupWithNullKey() {
        // Test lookup with null key should return default values
        Object result = currencyService.enrich(null);
        
        assertNotNull(result);
        assertTrue(result instanceof Map);
        
        @SuppressWarnings("unchecked")
        Map<String, Object> defaultData = (Map<String, Object>) result;
        
        assertEquals("Unknown", defaultData.get("region"));
        assertEquals(false, defaultData.get("isActive"));
    }

    @Test
    void testValidation() {
        // Test validation for existing keys
        assertTrue(currencyService.validate("USD"));
        assertTrue(currencyService.validate("EUR"));
        assertTrue(currencyService.validate("GBP"));
        assertTrue(currencyService.validate("JPY"));
        
        // Test validation for non-existing keys
        assertFalse(currencyService.validate("XYZ"));
        assertFalse(currencyService.validate("ABC"));
        assertFalse(currencyService.validate(null));
    }

    @Test
    void testServiceProperties() {
        assertEquals("currencyLookupService", currencyService.getName());
        assertEquals("code", currencyService.getKeyField());
        
        Map<String, Object> defaults = currencyService.getDefaultValues();
        assertEquals("Unknown", defaults.get("region"));
        assertEquals(false, defaults.get("isActive"));
    }

    @Test
    void testGetAllRecords() {
        Map<String, Map<String, Object>> allRecords = currencyService.getAllRecords();
        
        assertEquals(4, allRecords.size());
        assertTrue(allRecords.containsKey("USD"));
        assertTrue(allRecords.containsKey("EUR"));
        assertTrue(allRecords.containsKey("GBP"));
        assertTrue(allRecords.containsKey("JPY"));
        
        Map<String, Object> usdRecord = allRecords.get("USD");
        assertEquals("US Dollar", usdRecord.get("name"));
        assertEquals(2, usdRecord.get("decimalPlaces"));
    }

    @Test
    void testDatasetStatistics() {
        Map<String, Object> stats = currencyService.getDatasetStatistics();
        
        assertEquals(4, stats.get("recordCount"));
        assertEquals("code", stats.get("keyField"));
        assertEquals(true, stats.get("hasDefaultValues"));
        assertEquals(2, stats.get("defaultValueCount"));
        assertEquals("inline", stats.get("datasetType"));
    }

    @Test
    void testEmptyDataset() {
        // Create dataset with no data
        YamlEnrichment.LookupDataset emptyDataset = new YamlEnrichment.LookupDataset();
        emptyDataset.setType("inline");
        emptyDataset.setKeyField("id");
        emptyDataset.setData(Collections.emptyList());
        
        DatasetLookupService emptyService = new DatasetLookupService("emptyService", emptyDataset);
        
        // Should return null for any lookup
        assertNull(emptyService.enrich("anything"));
        assertFalse(emptyService.validate("anything"));
        assertEquals(0, emptyService.getAllRecords().size());
    }

    @Test
    void testDatasetWithoutDefaults() {
        // Create dataset without default values
        YamlEnrichment.LookupDataset noDefaultsDataset = new YamlEnrichment.LookupDataset();
        noDefaultsDataset.setType("inline");
        noDefaultsDataset.setKeyField("code");
        noDefaultsDataset.setData(Arrays.asList(
            createCurrencyRecord("USD", "US Dollar", 2, true, "North America")
        ));
        // No default values set
        
        DatasetLookupService noDefaultsService = new DatasetLookupService("noDefaultsService", noDefaultsDataset);
        
        // Successful lookup should work
        Object result = noDefaultsService.enrich("USD");
        assertNotNull(result);
        
        // Failed lookup should return null (no defaults)
        Object failedResult = noDefaultsService.enrich("XYZ");
        assertNull(failedResult);
    }

    @Test
    void testInvalidDatasetConfiguration() {
        // Test dataset without key field
        YamlEnrichment.LookupDataset invalidDataset = new YamlEnrichment.LookupDataset();
        invalidDataset.setType("inline");
        // No key field set
        invalidDataset.setData(Arrays.asList(Map.of("code", "USD")));
        
        assertThrows(IllegalArgumentException.class, () -> {
            new DatasetLookupService("invalidService", invalidDataset);
        });
    }

    @Test
    void testRecordWithMissingKeyField() {
        // Create dataset with a record missing the key field
        YamlEnrichment.LookupDataset datasetWithMissingKey = new YamlEnrichment.LookupDataset();
        datasetWithMissingKey.setType("inline");
        datasetWithMissingKey.setKeyField("code");
        
        List<Map<String, Object>> data = Arrays.asList(
            createCurrencyRecord("USD", "US Dollar", 2, true, "North America"),
            Map.of("name", "Invalid Currency") // Missing "code" field
        );
        datasetWithMissingKey.setData(data);
        
        // Should create service but only include valid records
        DatasetLookupService serviceWithMissingKey = new DatasetLookupService("testService", datasetWithMissingKey);
        
        assertEquals(1, serviceWithMissingKey.getAllRecords().size());
        assertTrue(serviceWithMissingKey.validate("USD"));
        assertFalse(serviceWithMissingKey.validate("Invalid Currency"));
    }

    @Test
    void testToString() {
        String toString = currencyService.toString();
        
        assertTrue(toString.contains("DatasetLookupService"));
        assertTrue(toString.contains("currencyLookupService"));
        assertTrue(toString.contains("recordCount=4"));
        assertTrue(toString.contains("keyField='code'"));
        assertTrue(toString.contains("datasetType='inline'"));
    }
}
