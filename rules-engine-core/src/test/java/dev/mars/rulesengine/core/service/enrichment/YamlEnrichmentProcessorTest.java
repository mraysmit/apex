package dev.mars.rulesengine.core.service.enrichment;

import dev.mars.rulesengine.core.config.yaml.YamlEnrichment;
import dev.mars.rulesengine.core.service.engine.ExpressionEvaluatorService;
import dev.mars.rulesengine.core.service.lookup.LookupService;
import dev.mars.rulesengine.core.service.lookup.LookupServiceRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for YamlEnrichmentProcessor.
 * This test validates that YAML enrichment configurations are properly executed at runtime.
 */
public class YamlEnrichmentProcessorTest {

    private YamlEnrichmentProcessor processor;
    private LookupServiceRegistry serviceRegistry;
    private ExpressionEvaluatorService evaluatorService;

    @BeforeEach
    void setUp() {
        serviceRegistry = new LookupServiceRegistry();
        evaluatorService = new ExpressionEvaluatorService();
        processor = new YamlEnrichmentProcessor(serviceRegistry, evaluatorService);
        
        // Set up test lookup services
        setupTestLookupServices();
    }

    private void setupTestLookupServices() {
        // Create counterparty lookup service
        LookupService counterpartyService = new LookupService(
            "counterpartyLookupService",
            Arrays.asList("CPTY001", "CPTY002", "CPTY003")
        );
        
        // Set up enrichment data for counterparty service
        Map<String, Object> counterpartyData = new HashMap<>();
        counterpartyData.put("CPTY001", createCounterpartyData("Goldman Sachs", "A+", "W22LROWP2IHZNBB6K528", "US"));
        counterpartyData.put("CPTY002", createCounterpartyData("JP Morgan", "AA-", "7H6GLXDRUGQFU57RNE97", "US"));
        counterpartyData.put("CPTY003", createCounterpartyData("Deutsche Bank", "A-", "7LTWFZYICNSX8D621K86", "DE"));
        counterpartyService.setEnrichmentData(counterpartyData);
        
        serviceRegistry.registerService(counterpartyService);
        
        // Create currency lookup service
        LookupService currencyService = new LookupService(
            "currencyLookupService",
            Arrays.asList("USD", "EUR", "GBP", "JPY")
        );
        
        Map<String, Object> currencyData = new HashMap<>();
        currencyData.put("USD", createCurrencyData("US Dollar", 2, true));
        currencyData.put("EUR", createCurrencyData("Euro", 2, true));
        currencyData.put("GBP", createCurrencyData("British Pound", 2, true));
        currencyData.put("JPY", createCurrencyData("Japanese Yen", 0, true));
        currencyService.setEnrichmentData(currencyData);
        
        serviceRegistry.registerService(currencyService);
    }

    private Map<String, Object> createCounterpartyData(String name, String rating, String lei, String jurisdiction) {
        Map<String, Object> data = new HashMap<>();
        data.put("name", name);
        data.put("rating", rating);
        data.put("lei", lei);
        data.put("jurisdiction", jurisdiction);
        return data;
    }

    private Map<String, Object> createCurrencyData(String name, int decimalPlaces, boolean isActive) {
        Map<String, Object> data = new HashMap<>();
        data.put("name", name);
        data.put("decimalPlaces", decimalPlaces);
        data.put("isActive", isActive);
        return data;
    }

    @Test
    void testLookupEnrichmentExecution() {
        // Create a test trade object
        TestTrade trade = new TestTrade();
        trade.counterpartyId = "CPTY001";
        trade.notionalCurrency = "USD";

        // Create YAML enrichment configuration for counterparty
        YamlEnrichment enrichment = new YamlEnrichment();
        enrichment.setId("counterparty-enrichment");
        enrichment.setName("Counterparty Data Enrichment");
        enrichment.setType("lookup-enrichment");
        enrichment.setTargetType("TestTrade");
        enrichment.setEnabled(true);
        enrichment.setPriority(10);
        enrichment.setCondition("#counterpartyId != null");

        // Set up lookup configuration
        YamlEnrichment.LookupConfig lookupConfig = new YamlEnrichment.LookupConfig();
        lookupConfig.setLookupService("counterpartyLookupService");
        lookupConfig.setLookupKey("#counterpartyId");
        lookupConfig.setCacheEnabled(true);
        lookupConfig.setCacheTtlSeconds(3600);
        enrichment.setLookupConfig(lookupConfig);

        // Set up field mappings
        List<YamlEnrichment.FieldMapping> fieldMappings = Arrays.asList(
            createFieldMapping("name", "counterpartyName", true, null),
            createFieldMapping("rating", "counterpartyRating", false, "NR"),
            createFieldMapping("lei", "counterpartyLei", false, null),
            createFieldMapping("jurisdiction", "counterpartyJurisdiction", false, "UNKNOWN")
        );
        enrichment.setFieldMappings(fieldMappings);

        // Process the enrichment
        Object result = processor.processEnrichment(enrichment, trade);

        // Verify the enrichment was applied
        assertSame(trade, result);
        assertEquals("Goldman Sachs", trade.counterpartyName);
        assertEquals("A+", trade.counterpartyRating);
        assertEquals("W22LROWP2IHZNBB6K528", trade.counterpartyLei);
        assertEquals("US", trade.counterpartyJurisdiction);
    }

    @Test
    void testMultipleEnrichmentsExecution() {
        // Create a test trade object
        TestTrade trade = new TestTrade();
        trade.counterpartyId = "CPTY002";
        trade.notionalCurrency = "EUR";

        // Create multiple enrichments
        List<YamlEnrichment> enrichments = Arrays.asList(
            createCounterpartyEnrichment(),
            createCurrencyEnrichment()
        );

        // Process all enrichments
        Object result = processor.processEnrichments(enrichments, trade);

        // Verify both enrichments were applied
        assertSame(trade, result);
        
        // Counterparty enrichment
        assertEquals("JP Morgan", trade.counterpartyName);
        assertEquals("AA-", trade.counterpartyRating);
        assertEquals("7H6GLXDRUGQFU57RNE97", trade.counterpartyLei);
        assertEquals("US", trade.counterpartyJurisdiction);
        
        // Currency enrichment
        assertEquals("Euro", trade.currencyName);
        assertEquals(2, trade.currencyDecimalPlaces);
        assertEquals(true, trade.currencyActive);
    }

    @Test
    void testEnrichmentConditionEvaluation() {
        // Create a test trade object without counterpartyId
        TestTrade trade = new TestTrade();
        trade.counterpartyId = null; // This should prevent enrichment
        trade.notionalCurrency = "USD";

        YamlEnrichment enrichment = createCounterpartyEnrichment();

        // Process the enrichment
        Object result = processor.processEnrichment(enrichment, trade);

        // Verify the enrichment was NOT applied due to condition
        assertSame(trade, result);
        assertNull(trade.counterpartyName);
        assertNull(trade.counterpartyRating);
    }

    @Test
    void testEnrichmentWithDefaultValues() {
        // Create a test trade object
        TestTrade trade = new TestTrade();
        trade.counterpartyId = "UNKNOWN_COUNTERPARTY"; // This won't be found in lookup

        YamlEnrichment enrichment = createCounterpartyEnrichment();

        // Process the enrichment
        Object result = processor.processEnrichment(enrichment, trade);

        // Verify default values were applied
        assertSame(trade, result);
        assertEquals("NR", trade.counterpartyRating); // Default value
        assertEquals("UNKNOWN", trade.counterpartyJurisdiction); // Default value
    }

    @Test
    void testCalculationEnrichment() {
        // Create a test trade object
        TestTrade trade = new TestTrade();
        trade.notionalAmount = 1000000.0;
        trade.rate = 0.05;

        // Create calculation enrichment
        YamlEnrichment enrichment = new YamlEnrichment();
        enrichment.setId("interest-calculation");
        enrichment.setType("calculation-enrichment");
        enrichment.setTargetType("TestTrade");
        enrichment.setEnabled(true);

        YamlEnrichment.CalculationConfig calcConfig = new YamlEnrichment.CalculationConfig();
        calcConfig.setExpression("#notionalAmount * #rate");
        calcConfig.setResultField("interestAmount");
        enrichment.setCalculationConfig(calcConfig);

        // Process the enrichment
        Object result = processor.processEnrichment(enrichment, trade);

        // Verify the calculation was applied
        assertSame(trade, result);
        assertEquals(50000.0, trade.interestAmount);
    }

    @Test
    void testMissingLookupService() {
        TestTrade trade = new TestTrade();
        trade.counterpartyId = "CPTY001";

        YamlEnrichment enrichment = new YamlEnrichment();
        enrichment.setType("lookup-enrichment");
        
        YamlEnrichment.LookupConfig lookupConfig = new YamlEnrichment.LookupConfig();
        lookupConfig.setLookupService("nonExistentService");
        lookupConfig.setLookupKey("#counterpartyId");
        enrichment.setLookupConfig(lookupConfig);

        // Should throw exception for missing service
        assertThrows(EnrichmentException.class, () -> {
            processor.processEnrichment(enrichment, trade);
        });
    }

    private YamlEnrichment createCounterpartyEnrichment() {
        YamlEnrichment enrichment = new YamlEnrichment();
        enrichment.setId("counterparty-enrichment");
        enrichment.setType("lookup-enrichment");
        enrichment.setTargetType("TestTrade");
        enrichment.setEnabled(true);
        enrichment.setPriority(10);
        enrichment.setCondition("#counterpartyId != null");

        YamlEnrichment.LookupConfig lookupConfig = new YamlEnrichment.LookupConfig();
        lookupConfig.setLookupService("counterpartyLookupService");
        lookupConfig.setLookupKey("#counterpartyId");
        enrichment.setLookupConfig(lookupConfig);

        enrichment.setFieldMappings(Arrays.asList(
            createFieldMapping("name", "counterpartyName", true, null),
            createFieldMapping("rating", "counterpartyRating", false, "NR"),
            createFieldMapping("lei", "counterpartyLei", false, null),
            createFieldMapping("jurisdiction", "counterpartyJurisdiction", false, "UNKNOWN")
        ));

        return enrichment;
    }

    private YamlEnrichment createCurrencyEnrichment() {
        YamlEnrichment enrichment = new YamlEnrichment();
        enrichment.setId("currency-enrichment");
        enrichment.setType("lookup-enrichment");
        enrichment.setTargetType("TestTrade");
        enrichment.setEnabled(true);
        enrichment.setPriority(11);
        enrichment.setCondition("#notionalCurrency != null");

        YamlEnrichment.LookupConfig lookupConfig = new YamlEnrichment.LookupConfig();
        lookupConfig.setLookupService("currencyLookupService");
        lookupConfig.setLookupKey("#notionalCurrency");
        enrichment.setLookupConfig(lookupConfig);

        enrichment.setFieldMappings(Arrays.asList(
            createFieldMapping("name", "currencyName", true, null),
            createFieldMapping("decimalPlaces", "currencyDecimalPlaces", false, 2),
            createFieldMapping("isActive", "currencyActive", false, true)
        ));

        return enrichment;
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

    /**
     * Test trade class for enrichment testing.
     */
    public static class TestTrade {
        public String counterpartyId;
        public String notionalCurrency;
        public Double notionalAmount;
        public Double rate;
        
        // Fields to be enriched
        public String counterpartyName;
        public String counterpartyRating;
        public String counterpartyLei;
        public String counterpartyJurisdiction;
        
        public String currencyName;
        public Integer currencyDecimalPlaces;
        public Boolean currencyActive;
        
        public Double interestAmount;
    }
}
