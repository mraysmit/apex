package dev.mars.apex.core.service.enrichment;

import dev.mars.apex.core.config.yaml.YamlEnrichment;
import dev.mars.apex.core.config.yaml.YamlRuleConfiguration;
import dev.mars.apex.core.service.ExpressionEvaluatorService;
import dev.mars.apex.core.service.lookup.LookupServiceRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive unit tests for compound lookup functionality.
 * Tests all compound lookup patterns documented in lookups.md.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Compound Lookup Tests")
class CompoundLookupTest {

    @Mock
    private LookupServiceRegistry lookupServiceRegistry;

    @Mock
    private ExpressionEvaluatorService expressionEvaluatorService;

    private YamlEnrichmentProcessor enrichmentProcessor;

    @BeforeEach
    void setUp() {
        enrichmentProcessor = new YamlEnrichmentProcessor(lookupServiceRegistry, expressionEvaluatorService);
    }

    @Test
    @DisplayName("Should perform compound customer lookup with region")
    void shouldPerformCompoundCustomerLookupWithRegion() {
        // Given
        Map<String, Object> inputData = Map.of(
            "customerId", "CUST123",
            "region", "US"
        );

        YamlEnrichment enrichment = createCompoundCustomerEnrichment();
        
        // Mock expression evaluation for lookup key
        when(expressionEvaluatorService.evaluate(eq("#customerId + '-' + #region"), any()))
            .thenReturn("CUST123-US");

        // Mock lookup service response
        Map<String, Object> lookupResult = Map.of(
            "compound_key", "CUST123-US",
            "name", "John Doe",
            "tier", "GOLD",
            "credit_limit", 50000
        );
        when(lookupServiceRegistry.lookup(eq("inlineDatasetService"), eq("CUST123-US")))
            .thenReturn(lookupResult);

        // When
        Object result = enrichmentProcessor.enrichObject(enrichment, inputData);

        // Then
        assertThat(result).isInstanceOf(Map.class);
        @SuppressWarnings("unchecked")
        Map<String, Object> enrichedData = (Map<String, Object>) result;
        
        assertThat(enrichedData).containsEntry("customerId", "CUST123");
        assertThat(enrichedData).containsEntry("region", "US");
        assertThat(enrichedData).containsEntry("customerName", "John Doe");
        assertThat(enrichedData).containsEntry("customerTier", "GOLD");
        assertThat(enrichedData).containsEntry("creditLimit", 50000);
    }

    @Test
    @DisplayName("Should perform trading pair lookup")
    void shouldPerformTradingPairLookup() {
        // Given
        Map<String, Object> inputData = Map.of(
            "baseCurrency", "eur",
            "quoteCurrency", "usd"
        );

        YamlEnrichment enrichment = createTradingPairEnrichment();
        
        // Mock expression evaluation for lookup key
        when(expressionEvaluatorService.evaluate(eq("#baseCurrency.toUpperCase() + '/' + #quoteCurrency.toUpperCase()"), any()))
            .thenReturn("EUR/USD");

        // Mock lookup service response
        Map<String, Object> lookupResult = Map.of(
            "pair", "EUR/USD",
            "spread", 0.0002,
            "min_size", 1000,
            "trading_hours", "24/5"
        );
        when(lookupServiceRegistry.lookup(eq("inlineDatasetService"), eq("EUR/USD")))
            .thenReturn(lookupResult);

        // When
        Object result = enrichmentProcessor.enrichObject(enrichment, inputData);

        // Then
        assertThat(result).isInstanceOf(Map.class);
        @SuppressWarnings("unchecked")
        Map<String, Object> enrichedData = (Map<String, Object>) result;
        
        assertThat(enrichedData).containsEntry("baseCurrency", "eur");
        assertThat(enrichedData).containsEntry("quoteCurrency", "usd");
        assertThat(enrichedData).containsEntry("spread", 0.0002);
        assertThat(enrichedData).containsEntry("minimumSize", 1000);
        assertThat(enrichedData).containsEntry("tradingHours", "24/5");
    }

    @Test
    @DisplayName("Should perform conditional compound key lookup")
    void shouldPerformConditionalCompoundKeyLookup() {
        // Given - Customer type
        Map<String, Object> inputData = Map.of(
            "partyType", "CUSTOMER",
            "partyId", "12345"
        );

        YamlEnrichment enrichment = createConditionalCounterpartyEnrichment();
        
        // Mock expression evaluation for lookup key
        when(expressionEvaluatorService.evaluate(
            eq("#partyType == 'CUSTOMER' ? 'CUST-' + #partyId : (#partyType == 'VENDOR' ? 'VEND-' + #partyId : 'UNKN-' + #partyId)"), 
            any()))
            .thenReturn("CUST-12345");

        // Mock lookup service response
        Map<String, Object> lookupResult = Map.of(
            "party_key", "CUST-12345",
            "legal_name", "ABC Corporation",
            "party_type", "CUSTOMER",
            "credit_rating", "AAA"
        );
        when(lookupServiceRegistry.lookup(eq("inlineDatasetService"), eq("CUST-12345")))
            .thenReturn(lookupResult);

        // When
        Object result = enrichmentProcessor.enrichObject(enrichment, inputData);

        // Then
        assertThat(result).isInstanceOf(Map.class);
        @SuppressWarnings("unchecked")
        Map<String, Object> enrichedData = (Map<String, Object>) result;
        
        assertThat(enrichedData).containsEntry("partyType", "CUSTOMER");
        assertThat(enrichedData).containsEntry("partyId", "12345");
        assertThat(enrichedData).containsEntry("legalName", "ABC Corporation");
        assertThat(enrichedData).containsEntry("creditRating", "AAA");
    }

    @Test
    @DisplayName("Should perform hierarchical compound key lookup")
    void shouldPerformHierarchicalCompoundKeyLookup() {
        // Given
        Map<String, Object> instrument = Map.of("symbol", "AAPL");
        Map<String, Object> counterparty = Map.of("id", "GOLDMAN");
        Map<String, Object> trade = Map.of(
            "instrument", instrument,
            "counterparty", counterparty,
            "settlementDate", LocalDate.of(2025, 8, 25)
        );
        Map<String, Object> inputData = Map.of("trade", trade);

        YamlEnrichment enrichment = createHierarchicalTradeEnrichment();
        
        // Mock expression evaluation for lookup key
        when(expressionEvaluatorService.evaluate(
            eq("#trade.instrument.symbol + ':' + #trade.counterparty.id + ':' + #trade.settlementDate.toString()"), 
            any()))
            .thenReturn("AAPL:GOLDMAN:2025-08-25");

        // Mock lookup service response
        Map<String, Object> lookupResult = Map.of(
            "settlement_key", "AAPL:GOLDMAN:2025-08-25",
            "settlement_instructions", "DVP",
            "custodian", "BNY Mellon",
            "account_number", "ACC123456"
        );
        when(lookupServiceRegistry.lookup(eq("databaseService"), eq("AAPL:GOLDMAN:2025-08-25")))
            .thenReturn(lookupResult);

        // When
        Object result = enrichmentProcessor.enrichObject(enrichment, inputData);

        // Then
        assertThat(result).isInstanceOf(Map.class);
        @SuppressWarnings("unchecked")
        Map<String, Object> enrichedData = (Map<String, Object>) result;
        
        assertThat(enrichedData).containsKey("trade");
        assertThat(enrichedData).containsEntry("settlementInstructions", "DVP");
        assertThat(enrichedData).containsEntry("custodian", "BNY Mellon");
        assertThat(enrichedData).containsEntry("accountNumber", "ACC123456");
    }

    @Test
    @DisplayName("Should perform multi-dimensional product lookup")
    void shouldPerformMultiDimensionalProductLookup() {
        // Given
        Map<String, Object> product = Map.of(
            "category", "ELECTRONICS",
            "id", "PROD456"
        );
        Map<String, Object> customer = Map.of(
            "tier", "GOLD",
            "region", "US"
        );
        Map<String, Object> inputData = Map.of(
            "product", product,
            "customer", customer,
            "effectiveDate", LocalDate.of(2025, 8, 22)
        );

        YamlEnrichment enrichment = createMultiDimensionalProductEnrichment();
        
        // Mock expression evaluation for lookup key
        when(expressionEvaluatorService.evaluate(
            eq("#product.category + '|' + #product.id + '|' + #customer.tier + '|' + #customer.region"), 
            any()))
            .thenReturn("ELECTRONICS|PROD456|GOLD|US");

        // Mock lookup service response
        Map<String, Object> lookupResult = Map.of(
            "base_price", 999.99,
            "discount_rate", 0.15,
            "minimum_quantity", 1,
            "currency", "USD"
        );
        when(lookupServiceRegistry.lookup(eq("databaseService"), eq("ELECTRONICS|PROD456|GOLD|US")))
            .thenReturn(lookupResult);

        // When
        Object result = enrichmentProcessor.enrichObject(enrichment, inputData);

        // Then
        assertThat(result).isInstanceOf(Map.class);
        @SuppressWarnings("unchecked")
        Map<String, Object> enrichedData = (Map<String, Object>) result;
        
        assertThat(enrichedData).containsKey("product");
        assertThat(enrichedData).containsKey("customer");
        assertThat(enrichedData).containsEntry("productBasePrice", 999.99);
        assertThat(enrichedData).containsEntry("customerDiscountRate", 0.15);
        assertThat(enrichedData).containsEntry("minimumOrderQuantity", 1);
        assertThat(enrichedData).containsEntry("pricingCurrency", "USD");
    }

    @Test
    @DisplayName("Should handle null values in compound key gracefully")
    void shouldHandleNullValuesInCompoundKeyGracefully() {
        // Given
        Map<String, Object> inputData = Map.of(
            "customerId", "CUST123"
            // region is missing/null
        );

        YamlEnrichment enrichment = createSafeCompoundEnrichment();
        
        // Mock expression evaluation for lookup key with null handling
        when(expressionEvaluatorService.evaluate(eq("#customerId + '-' + (#region ?: 'DEFAULT')"), any()))
            .thenReturn("CUST123-DEFAULT");

        // Mock lookup service response
        Map<String, Object> lookupResult = Map.of(
            "compound_key", "CUST123-DEFAULT",
            "name", "Default Customer",
            "tier", "STANDARD"
        );
        when(lookupServiceRegistry.lookup(eq("inlineDatasetService"), eq("CUST123-DEFAULT")))
            .thenReturn(lookupResult);

        // When
        Object result = enrichmentProcessor.enrichObject(enrichment, inputData);

        // Then
        assertThat(result).isInstanceOf(Map.class);
        @SuppressWarnings("unchecked")
        Map<String, Object> enrichedData = (Map<String, Object>) result;
        
        assertThat(enrichedData).containsEntry("customerId", "CUST123");
        assertThat(enrichedData).containsEntry("customerName", "Default Customer");
        assertThat(enrichedData).containsEntry("customerTier", "STANDARD");
    }

    // Helper methods to create enrichment configurations

    private YamlEnrichment createCompoundCustomerEnrichment() {
        YamlEnrichment enrichment = new YamlEnrichment();
        enrichment.setId("compound-customer-lookup");
        enrichment.setType("lookup-enrichment");
        enrichment.setCondition("#customerId != null && #region != null");
        
        YamlEnrichment.LookupConfig lookupConfig = new YamlEnrichment.LookupConfig();
        lookupConfig.setLookupService("inlineDatasetService");
        lookupConfig.setLookupKey("#customerId + '-' + #region");
        enrichment.setLookupConfig(lookupConfig);
        
        // Field mappings
        List<YamlEnrichment.FieldMapping> fieldMappings = List.of(
            createFieldMapping("name", "customerName"),
            createFieldMapping("tier", "customerTier"),
            createFieldMapping("credit_limit", "creditLimit")
        );
        enrichment.setFieldMappings(fieldMappings);
        
        return enrichment;
    }

    private YamlEnrichment createTradingPairEnrichment() {
        YamlEnrichment enrichment = new YamlEnrichment();
        enrichment.setId("trading-pair-lookup");
        enrichment.setType("lookup-enrichment");
        enrichment.setCondition("#baseCurrency != null && #quoteCurrency != null");
        
        YamlEnrichment.LookupConfig lookupConfig = new YamlEnrichment.LookupConfig();
        lookupConfig.setLookupService("inlineDatasetService");
        lookupConfig.setLookupKey("#baseCurrency.toUpperCase() + '/' + #quoteCurrency.toUpperCase()");
        enrichment.setLookupConfig(lookupConfig);
        
        // Field mappings
        List<YamlEnrichment.FieldMapping> fieldMappings = List.of(
            createFieldMapping("spread", "spread"),
            createFieldMapping("min_size", "minimumSize"),
            createFieldMapping("trading_hours", "tradingHours")
        );
        enrichment.setFieldMappings(fieldMappings);
        
        return enrichment;
    }

    private YamlEnrichment createConditionalCounterpartyEnrichment() {
        YamlEnrichment enrichment = new YamlEnrichment();
        enrichment.setId("conditional-counterparty-lookup");
        enrichment.setType("lookup-enrichment");
        enrichment.setCondition("#partyId != null");
        
        YamlEnrichment.LookupConfig lookupConfig = new YamlEnrichment.LookupConfig();
        lookupConfig.setLookupService("inlineDatasetService");
        lookupConfig.setLookupKey("#partyType == 'CUSTOMER' ? 'CUST-' + #partyId : (#partyType == 'VENDOR' ? 'VEND-' + #partyId : 'UNKN-' + #partyId)");
        enrichment.setLookupConfig(lookupConfig);
        
        // Field mappings
        List<YamlEnrichment.FieldMapping> fieldMappings = List.of(
            createFieldMapping("legal_name", "legalName"),
            createFieldMapping("credit_rating", "creditRating")
        );
        enrichment.setFieldMappings(fieldMappings);
        
        return enrichment;
    }

    private YamlEnrichment createHierarchicalTradeEnrichment() {
        YamlEnrichment enrichment = new YamlEnrichment();
        enrichment.setId("hierarchical-trade-lookup");
        enrichment.setType("lookup-enrichment");
        enrichment.setCondition("#trade != null && #trade.instrument != null && #trade.counterparty != null");
        
        YamlEnrichment.LookupConfig lookupConfig = new YamlEnrichment.LookupConfig();
        lookupConfig.setLookupService("databaseService");
        lookupConfig.setLookupKey("#trade.instrument.symbol + ':' + #trade.counterparty.id + ':' + #trade.settlementDate.toString()");
        enrichment.setLookupConfig(lookupConfig);
        
        // Field mappings
        List<YamlEnrichment.FieldMapping> fieldMappings = List.of(
            createFieldMapping("settlement_instructions", "settlementInstructions"),
            createFieldMapping("custodian", "custodian"),
            createFieldMapping("account_number", "accountNumber")
        );
        enrichment.setFieldMappings(fieldMappings);
        
        return enrichment;
    }

    private YamlEnrichment createMultiDimensionalProductEnrichment() {
        YamlEnrichment enrichment = new YamlEnrichment();
        enrichment.setId("multi-dimensional-product-lookup");
        enrichment.setType("lookup-enrichment");
        enrichment.setCondition("#product != null && #customer != null && #effectiveDate != null");
        
        YamlEnrichment.LookupConfig lookupConfig = new YamlEnrichment.LookupConfig();
        lookupConfig.setLookupService("databaseService");
        lookupConfig.setLookupKey("#product.category + '|' + #product.id + '|' + #customer.tier + '|' + #customer.region");
        enrichment.setLookupConfig(lookupConfig);
        
        // Field mappings
        List<YamlEnrichment.FieldMapping> fieldMappings = List.of(
            createFieldMapping("base_price", "productBasePrice"),
            createFieldMapping("discount_rate", "customerDiscountRate"),
            createFieldMapping("minimum_quantity", "minimumOrderQuantity"),
            createFieldMapping("currency", "pricingCurrency")
        );
        enrichment.setFieldMappings(fieldMappings);
        
        return enrichment;
    }

    private YamlEnrichment createSafeCompoundEnrichment() {
        YamlEnrichment enrichment = new YamlEnrichment();
        enrichment.setId("safe-compound-lookup");
        enrichment.setType("lookup-enrichment");
        enrichment.setCondition("#customerId != null");
        
        YamlEnrichment.LookupConfig lookupConfig = new YamlEnrichment.LookupConfig();
        lookupConfig.setLookupService("inlineDatasetService");
        lookupConfig.setLookupKey("#customerId + '-' + (#region ?: 'DEFAULT')");
        enrichment.setLookupConfig(lookupConfig);
        
        // Field mappings
        List<YamlEnrichment.FieldMapping> fieldMappings = List.of(
            createFieldMapping("name", "customerName"),
            createFieldMapping("tier", "customerTier")
        );
        enrichment.setFieldMappings(fieldMappings);
        
        return enrichment;
    }

    private YamlEnrichment.FieldMapping createFieldMapping(String sourceField, String targetField) {
        YamlEnrichment.FieldMapping mapping = new YamlEnrichment.FieldMapping();
        mapping.setSourceField(sourceField);
        mapping.setTargetField(targetField);
        return mapping;
    }
}
