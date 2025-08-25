package dev.mars.apex.core.util;

import java.util.*;

/**
 * Generic mock data provider for testing and demonstration purposes.
 * Provides reusable mock data structures that can be used across different APEX components.
 * 
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-08-25
 * @version 1.0
 */
public class MockDataProvider {
    
    /**
     * Create a mock lookup dataset for testing enrichment functionality.
     * 
     * @param keyField The field name to use as the lookup key
     * @param entries The number of entries to generate
     * @return A list of maps representing the lookup dataset
     */
    public static List<Map<String, Object>> createMockLookupDataset(String keyField, int entries) {
        List<Map<String, Object>> dataset = new ArrayList<>();
        
        for (int i = 0; i < entries; i++) {
            Map<String, Object> entry = new HashMap<>();
            entry.put(keyField, "KEY_" + String.format("%03d", i));
            entry.put("name", "Entry " + i);
            entry.put("description", "Mock entry number " + i);
            entry.put("active", TestDataGenerator.randomBoolean());
            entry.put("value", TestDataGenerator.randomAmount(10.0, 1000.0));
            entry.put("category", "Category " + (i % 5));
            entry.put("priority", TestDataGenerator.randomInt(1, 10));
            dataset.add(entry);
        }
        
        return dataset;
    }
    
    /**
     * Create a mock currency lookup dataset.
     * 
     * @return A list of currency data maps
     */
    public static List<Map<String, Object>> createMockCurrencyDataset() {
        List<Map<String, Object>> currencies = new ArrayList<>();
        
        currencies.add(createCurrencyEntry("USD", "US Dollar", "United States", true, "$"));
        currencies.add(createCurrencyEntry("EUR", "Euro", "European Union", true, "€"));
        currencies.add(createCurrencyEntry("GBP", "British Pound", "United Kingdom", true, "£"));
        currencies.add(createCurrencyEntry("JPY", "Japanese Yen", "Japan", true, "¥"));
        currencies.add(createCurrencyEntry("CHF", "Swiss Franc", "Switzerland", true, "CHF"));
        currencies.add(createCurrencyEntry("CAD", "Canadian Dollar", "Canada", true, "C$"));
        currencies.add(createCurrencyEntry("AUD", "Australian Dollar", "Australia", true, "A$"));
        currencies.add(createCurrencyEntry("SEK", "Swedish Krona", "Sweden", true, "kr"));
        currencies.add(createCurrencyEntry("NOK", "Norwegian Krone", "Norway", true, "kr"));
        currencies.add(createCurrencyEntry("DKK", "Danish Krone", "Denmark", true, "kr"));
        
        return currencies;
    }
    
    /**
     * Create a mock country lookup dataset.
     * 
     * @return A list of country data maps
     */
    public static List<Map<String, Object>> createMockCountryDataset() {
        List<Map<String, Object>> countries = new ArrayList<>();
        
        countries.add(createCountryEntry("US", "United States", "USD", "America", "GMT-5"));
        countries.add(createCountryEntry("UK", "United Kingdom", "GBP", "Europe", "GMT"));
        countries.add(createCountryEntry("DE", "Germany", "EUR", "Europe", "GMT+1"));
        countries.add(createCountryEntry("FR", "France", "EUR", "Europe", "GMT+1"));
        countries.add(createCountryEntry("JP", "Japan", "JPY", "Asia", "GMT+9"));
        countries.add(createCountryEntry("CA", "Canada", "CAD", "America", "GMT-5"));
        countries.add(createCountryEntry("AU", "Australia", "AUD", "Oceania", "GMT+10"));
        countries.add(createCountryEntry("CH", "Switzerland", "CHF", "Europe", "GMT+1"));
        countries.add(createCountryEntry("SE", "Sweden", "SEK", "Europe", "GMT+1"));
        countries.add(createCountryEntry("NO", "Norway", "NOK", "Europe", "GMT+1"));
        
        return countries;
    }
    
    /**
     * Create a mock validation rules dataset.
     * 
     * @return A list of validation rule maps
     */
    public static List<Map<String, Object>> createMockValidationRules() {
        List<Map<String, Object>> rules = new ArrayList<>();
        
        rules.add(createValidationRule("AMOUNT_POSITIVE", "#amount > 0", "Amount must be positive", "ERROR"));
        rules.add(createValidationRule("CURRENCY_VALID", "#currency != null && #currency.length() == 3", "Currency must be 3 characters", "ERROR"));
        rules.add(createValidationRule("EMAIL_FORMAT", "#email != null && #email.contains('@')", "Email must contain @", "WARNING"));
        rules.add(createValidationRule("NAME_LENGTH", "#name != null && #name.length() > 2", "Name must be longer than 2 characters", "ERROR"));
        rules.add(createValidationRule("DATE_FUTURE", "#date != null && #date.isAfter(T(java.time.LocalDate).now())", "Date must be in future", "WARNING"));
        
        return rules;
    }
    
    /**
     * Create a mock enrichment configuration.
     * 
     * @param enrichmentName The name of the enrichment
     * @param lookupKey The lookup key expression
     * @param dataset The dataset to use for lookup
     * @return A map representing the enrichment configuration
     */
    public static Map<String, Object> createMockEnrichmentConfig(String enrichmentName, String lookupKey, List<Map<String, Object>> dataset) {
        Map<String, Object> enrichment = new HashMap<>();
        enrichment.put("name", enrichmentName);
        enrichment.put("type", "lookup-enrichment");
        enrichment.put("enabled", true);
        
        Map<String, Object> lookupConfig = new HashMap<>();
        lookupConfig.put("lookupKey", lookupKey);
        
        Map<String, Object> lookupDataset = new HashMap<>();
        lookupDataset.put("keyField", "code");
        lookupDataset.put("data", dataset);
        
        lookupConfig.put("lookupDataset", lookupDataset);
        enrichment.put("lookupConfig", lookupConfig);
        
        return enrichment;
    }
    
    // Helper methods
    
    private static Map<String, Object> createCurrencyEntry(String code, String name, String country, boolean active, String symbol) {
        Map<String, Object> currency = new HashMap<>();
        currency.put("code", code);
        currency.put("name", name);
        currency.put("country", country);
        currency.put("active", active);
        currency.put("symbol", symbol);
        currency.put("decimalPlaces", code.equals("JPY") ? 0 : 2);
        return currency;
    }
    
    private static Map<String, Object> createCountryEntry(String code, String name, String currency, String region, String timezone) {
        Map<String, Object> country = new HashMap<>();
        country.put("code", code);
        country.put("name", name);
        country.put("currency", currency);
        country.put("region", region);
        country.put("timezone", timezone);
        country.put("active", true);
        return country;
    }
    
    private static Map<String, Object> createValidationRule(String id, String condition, String message, String severity) {
        Map<String, Object> rule = new HashMap<>();
        rule.put("id", id);
        rule.put("condition", condition);
        rule.put("message", message);
        rule.put("severity", severity);
        rule.put("enabled", true);
        return rule;
    }
    
    /**
     * Create a mock YAML configuration structure for testing.
     * 
     * @return A map representing a complete YAML configuration
     */
    public static Map<String, Object> createMockYamlConfiguration() {
        Map<String, Object> config = new HashMap<>();
        
        // Metadata
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("name", "Mock Configuration");
        metadata.put("version", "1.0");
        metadata.put("description", "Mock configuration for testing");
        metadata.put("type", "rule-config");
        config.put("metadata", metadata);
        
        // Enrichments
        List<Map<String, Object>> enrichments = new ArrayList<>();
        enrichments.add(createMockEnrichmentConfig("currency-enrichment", "#currency", createMockCurrencyDataset()));
        config.put("enrichments", enrichments);
        
        // Rules
        config.put("rules", createMockValidationRules());
        
        return config;
    }
    
    /**
     * Create test data that matches a specific schema.
     * 
     * @param schema Map defining the expected fields and their types
     * @return A map with test data matching the schema
     */
    public static Map<String, Object> createTestDataForSchema(Map<String, Class<?>> schema) {
        Map<String, Object> data = new HashMap<>();
        
        for (Map.Entry<String, Class<?>> entry : schema.entrySet()) {
            String fieldName = entry.getKey();
            Class<?> fieldType = entry.getValue();
            
            if (fieldType == String.class) {
                data.put(fieldName, TestDataGenerator.randomString(8));
            } else if (fieldType == Integer.class) {
                data.put(fieldName, TestDataGenerator.randomInt(1, 100));
            } else if (fieldType == Double.class) {
                data.put(fieldName, TestDataGenerator.randomDouble(1.0, 100.0));
            } else if (fieldType == Boolean.class) {
                data.put(fieldName, TestDataGenerator.randomBoolean());
            } else {
                data.put(fieldName, "MOCK_" + fieldName.toUpperCase());
            }
        }
        
        return data;
    }
}
