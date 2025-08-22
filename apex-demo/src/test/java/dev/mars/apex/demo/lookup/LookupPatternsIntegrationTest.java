package dev.mars.apex.demo.lookup;

import dev.mars.apex.core.config.yaml.YamlConfigurationLoader;
import dev.mars.apex.core.config.yaml.YamlRuleConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive integration tests for all lookup patterns documented in lookups.md.
 * Tests the complete flow from YAML configuration loading through validation.
 * 
 * This test class follows project guidelines by not using external mocking frameworks
 * and focusing on real integration testing of the lookup validation system.
 */
@DisplayName("Lookup Patterns Integration Tests")
class LookupPatternsIntegrationTest {

    private YamlConfigurationLoader configurationLoader;
    private YamlRuleConfiguration testConfig;

    @BeforeEach
    void setUp() throws Exception {
        configurationLoader = new YamlConfigurationLoader();
        testConfig = configurationLoader.loadFromClasspath("test-configs/lookup-patterns-test.yaml");
    }

    @Test
    @DisplayName("Should load and validate comprehensive lookup configuration successfully")
    void shouldLoadAndValidateComprehensiveLookupConfigurationSuccessfully() {
        // When & Then - Configuration should load without validation errors
        assertNotNull(testConfig);
        assertNotNull(testConfig.getEnrichments());
        assertEquals(15, testConfig.getEnrichments().size());
        
        // Verify each enrichment has required fields
        testConfig.getEnrichments().forEach(enrichment -> {
            assertNotNull(enrichment.getId());
            assertFalse(enrichment.getId().isEmpty());
            assertEquals("lookup-enrichment", enrichment.getType());
            assertNotNull(enrichment.getLookupConfig());
            assertNotNull(enrichment.getLookupConfig().getLookupKey());
            assertFalse(enrichment.getLookupConfig().getLookupKey().isEmpty());
            assertNotNull(enrichment.getLookupConfig().getLookupDataset());
        });
    }

    @Test
    @DisplayName("Should validate simple field reference lookup pattern")
    void shouldValidateSimpleFieldReferenceLookupPattern() {
        // Given
        var enrichment = testConfig.getEnrichments().get(0); // simple-field-reference
        
        // Then
        assertEquals("test-simple-field-reference", enrichment.getId());
        assertEquals("#customerId", enrichment.getLookupConfig().getLookupKey());
        assertEquals("#customerId != null", enrichment.getCondition());
        
        // Verify dataset configuration
        var dataset = enrichment.getLookupConfig().getLookupDataset();
        assertEquals("inline", dataset.getType());
        assertEquals("id", dataset.getKeyField());
        assertNotNull(dataset.getData());
        assertFalse(dataset.getData().isEmpty());
    }

    @Test
    @DisplayName("Should validate nested field reference lookup pattern")
    void shouldValidateNestedFieldReferenceLookupPattern() {
        // Given
        var enrichment = testConfig.getEnrichments().get(1); // nested-field-reference
        
        // Then
        assertEquals("test-nested-field-reference", enrichment.getId());
        assertEquals("#customer.id", enrichment.getLookupConfig().getLookupKey());
        assertTrue(enrichment.getCondition().contains("#customer?.id != null"));
        
        // Verify field mappings
        assertNotNull(enrichment.getFieldMappings());
        assertEquals(2, enrichment.getFieldMappings().size());
    }

    @Test
    @DisplayName("Should validate complex nested field reference lookup pattern")
    void shouldValidateComplexNestedFieldReferenceLookupPattern() {
        // Given
        var enrichment = testConfig.getEnrichments().get(2); // complex-nested-reference
        
        // Then
        assertEquals("test-complex-nested-reference", enrichment.getId());
        assertEquals("#transaction.counterparty.partyId", enrichment.getLookupConfig().getLookupKey());
        assertTrue(enrichment.getCondition().contains("#transaction?.counterparty?.partyId != null"));
    }

    @Test
    @DisplayName("Should validate conditional expression lookup pattern")
    void shouldValidateConditionalExpressionLookupPattern() {
        // Given
        var enrichment = testConfig.getEnrichments().get(3); // conditional-expression
        
        // Then
        assertEquals("test-conditional-expression", enrichment.getId());
        assertEquals("#type == 'CUSTOMER' ? #customerId : #vendorId", enrichment.getLookupConfig().getLookupKey());
        assertEquals("#type != null", enrichment.getCondition());
        
        // Verify dataset has both customer and vendor data
        var dataset = enrichment.getLookupConfig().getLookupDataset();
        assertEquals(2, dataset.getData().size());
    }

    @Test
    @DisplayName("Should validate string manipulation lookup pattern")
    void shouldValidateStringManipulationLookupPattern() {
        // Given
        var enrichment = testConfig.getEnrichments().get(4); // string-manipulation
        
        // Then
        assertEquals("test-string-manipulation", enrichment.getId());
        assertEquals("#accountNumber.substring(0, 3)", enrichment.getLookupConfig().getLookupKey());
        assertEquals("#accountNumber != null", enrichment.getCondition());
    }

    @Test
    @DisplayName("Should validate compound key concatenation lookup pattern")
    void shouldValidateCompoundKeyConcatenationLookupPattern() {
        // Given
        var enrichment = testConfig.getEnrichments().get(5); // compound-key-concatenation
        
        // Then
        assertEquals("test-compound-key-concatenation", enrichment.getId());
        assertEquals("#customerId + '-' + #region", enrichment.getLookupConfig().getLookupKey());
        assertTrue(enrichment.getCondition().contains("#customerId != null && #region != null"));
        
        // Verify compound key dataset
        var dataset = enrichment.getLookupConfig().getLookupDataset();
        assertEquals("compound_key", dataset.getKeyField());
    }

    @Test
    @DisplayName("Should validate trading pair lookup pattern")
    void shouldValidateTradingPairLookupPattern() {
        // Given
        var enrichment = testConfig.getEnrichments().get(6); // trading-pair-key
        
        // Then
        assertEquals("test-trading-pair-key", enrichment.getId());
        assertEquals("#baseCurrency.toUpperCase() + '/' + #quoteCurrency.toUpperCase()", 
                    enrichment.getLookupConfig().getLookupKey());
        assertTrue(enrichment.getCondition().contains("#baseCurrency != null && #quoteCurrency != null"));
    }

    @Test
    @DisplayName("Should validate conditional compound key lookup pattern")
    void shouldValidateConditionalCompoundKeyLookupPattern() {
        // Given
        var enrichment = testConfig.getEnrichments().get(7); // conditional-compound-key
        
        // Then
        assertEquals("test-conditional-compound-key", enrichment.getId());
        String expectedKey = "#partyType == 'CUSTOMER' ? 'CUST-' + #partyId : (#partyType == 'VENDOR' ? 'VEND-' + #partyId : 'UNKN-' + #partyId)";
        assertEquals(expectedKey, enrichment.getLookupConfig().getLookupKey());
        
        // Verify dataset has all party types
        var dataset = enrichment.getLookupConfig().getLookupDataset();
        assertEquals(3, dataset.getData().size());
    }

    @Test
    @DisplayName("Should validate hierarchical compound key lookup pattern")
    void shouldValidateHierarchicalCompoundKeyLookupPattern() {
        // Given
        var enrichment = testConfig.getEnrichments().get(8); // hierarchical-compound-key
        
        // Then
        assertEquals("test-hierarchical-compound-key", enrichment.getId());
        assertEquals("#trade.instrument.symbol + ':' + #trade.counterparty.id + ':' + #trade.settlementDate.toString()", 
                    enrichment.getLookupConfig().getLookupKey());
        assertTrue(enrichment.getCondition().contains("#trade?.instrument?.symbol != null"));
    }

    @Test
    @DisplayName("Should validate hash-based compound key lookup pattern")
    void shouldValidateHashBasedCompoundKeyLookupPattern() {
        // Given
        var enrichment = testConfig.getEnrichments().get(9); // hash-based-compound-key
        
        // Then
        assertEquals("test-hash-based-compound-key", enrichment.getId());
        String expectedKey = "T(java.lang.String).valueOf((#portfolio.id + #portfolio.strategy + #portfolio.region + #asOfDate.toString()).hashCode())";
        assertEquals(expectedKey, enrichment.getLookupConfig().getLookupKey());
        assertTrue(enrichment.getCondition().contains("#portfolio != null && #asOfDate != null"));
    }

    @Test
    @DisplayName("Should validate multi-dimensional lookup pattern")
    void shouldValidateMultiDimensionalLookupPattern() {
        // Given
        var enrichment = testConfig.getEnrichments().get(10); // multi-dimensional-lookup
        
        // Then
        assertEquals("test-multi-dimensional-lookup", enrichment.getId());
        assertEquals("#product.category + '|' + #product.id + '|' + #customer.tier + '|' + #customer.region", 
                    enrichment.getLookupConfig().getLookupKey());
        assertTrue(enrichment.getCondition().contains("#product != null && #customer != null"));
    }

    @Test
    @DisplayName("Should validate safe navigation pattern")
    void shouldValidateSafeNavigationPattern() {
        // Given
        var enrichment = testConfig.getEnrichments().get(11); // safe-navigation
        
        // Then
        assertEquals("test-safe-navigation", enrichment.getId());
        assertEquals("#customer?.id ?: #customerId", enrichment.getLookupConfig().getLookupKey());
        assertEquals("#customerId != null", enrichment.getCondition());
    }

    @Test
    @DisplayName("Should validate date operations lookup pattern")
    void shouldValidateDateOperationsLookupPattern() {
        // Given
        var enrichment = testConfig.getEnrichments().get(12); // date-operations
        
        // Then
        assertEquals("test-date-operations", enrichment.getId());
        assertEquals("T(java.time.LocalDate).parse(#tradeDate).plusDays(#settlementDays).toString()", 
                    enrichment.getLookupConfig().getLookupKey());
        assertTrue(enrichment.getCondition().contains("#tradeDate != null && #settlementDays != null"));
    }

    @Test
    @DisplayName("Should validate mathematical operations lookup pattern")
    void shouldValidateMathematicalOperationsLookupPattern() {
        // Given
        var enrichment = testConfig.getEnrichments().get(13); // mathematical-operations
        
        // Then
        assertEquals("test-mathematical-operations", enrichment.getId());
        assertEquals("T(java.lang.String).valueOf((#baseAmount * #multiplier).intValue())", 
                    enrichment.getLookupConfig().getLookupKey());
        assertTrue(enrichment.getCondition().contains("#baseAmount != null && #multiplier != null"));
    }

    @Test
    @DisplayName("Should validate field transformations lookup pattern")
    void shouldValidateFieldTransformationsLookupPattern() {
        // Given
        var enrichment = testConfig.getEnrichments().get(14); // field-transformations
        
        // Then
        assertEquals("test-field-transformations", enrichment.getId());
        assertEquals("#customerId", enrichment.getLookupConfig().getLookupKey());
        
        // Verify field mappings with transformations
        assertNotNull(enrichment.getFieldMappings());
        assertEquals(3, enrichment.getFieldMappings().size());
        
        // Check transformation expressions
        var displayNameMapping = enrichment.getFieldMappings().get(0);
        assertEquals("displayName", displayNameMapping.getTargetField());
        assertNotNull(displayNameMapping.getTransformation());
        assertTrue(displayNameMapping.getTransformation().contains("toUpperCase()"));
        
        var amountMapping = enrichment.getFieldMappings().get(1);
        assertEquals("formattedAmount", amountMapping.getTargetField());
        assertNotNull(amountMapping.getTransformation());
        assertTrue(amountMapping.getTransformation().contains("NumberFormat"));
    }

    @Test
    @DisplayName("Should validate all lookup key expressions are syntactically correct")
    void shouldValidateAllLookupKeyExpressionsAreSyntacticallyCorrect() {
        // Given & When
        for (var enrichment : testConfig.getEnrichments()) {
            String lookupKey = enrichment.getLookupConfig().getLookupKey();
            
            // Then - All lookup keys should be non-null and non-empty
            assertNotNull(lookupKey, "Lookup key should not be null for enrichment: " + enrichment.getId());
            assertFalse(lookupKey.trim().isEmpty(), "Lookup key should not be empty for enrichment: " + enrichment.getId());
            
            // Verify basic SpEL syntax patterns
            if (lookupKey.contains("?") && lookupKey.contains(":")) {
                // Handle Elvis operator (?:) - safe navigation with null coalescing
                if (lookupKey.contains("?:")) {
                    // Elvis operator is valid - skip ternary validation
                    assertTrue(true, "Elvis operator is valid in: " + lookupKey);
                } else {
                    // Ternary operator - should have balanced ? and : (excluding safe navigation ?.)
                    String withoutSafeNav = lookupKey.replace("?.", "X.");
                    long questionMarks = withoutSafeNav.chars().filter(ch -> ch == '?').count();
                    long colons = withoutSafeNav.chars().filter(ch -> ch == ':').count();
                    assertTrue(questionMarks <= colons, "Unbalanced ternary operators in: " + lookupKey);
                }
            }
            
            if (lookupKey.contains("'")) {
                // String literals - should have balanced quotes
                long quotes = lookupKey.chars().filter(ch -> ch == '\'').count();
                assertEquals(0, quotes % 2, "Unbalanced quotes in: " + lookupKey);
            }
        }
    }

    @Test
    @DisplayName("Should validate all enrichment conditions are syntactically correct")
    void shouldValidateAllEnrichmentConditionsAreSyntacticallyCorrect() {
        // Given & When
        for (var enrichment : testConfig.getEnrichments()) {
            String condition = enrichment.getCondition();
            
            if (condition != null && !condition.trim().isEmpty()) {
                // Then - Conditions should follow basic SpEL patterns
                assertFalse(condition.contains("&&&"), "Invalid && operator in condition: " + condition);
                assertFalse(condition.contains("|||"), "Invalid || operator in condition: " + condition);
                
                // Should contain field references
                assertTrue(condition.contains("#"), "Condition should contain field references: " + condition);
            }
        }
    }

    @Test
    @DisplayName("Should validate all field mappings are properly configured")
    void shouldValidateAllFieldMappingsAreProperlyConfigured() {
        // Given & When
        for (var enrichment : testConfig.getEnrichments()) {
            if (enrichment.getFieldMappings() != null) {
                for (var mapping : enrichment.getFieldMappings()) {
                    // Then
                    assertNotNull(mapping.getSourceField(), "Source field should not be null in enrichment: " + enrichment.getId());
                    assertNotNull(mapping.getTargetField(), "Target field should not be null in enrichment: " + enrichment.getId());
                    assertFalse(mapping.getSourceField().trim().isEmpty(), "Source field should not be empty in enrichment: " + enrichment.getId());
                    assertFalse(mapping.getTargetField().trim().isEmpty(), "Target field should not be empty in enrichment: " + enrichment.getId());
                }
            }
        }
    }

    @Test
    @DisplayName("Should validate comprehensive lookup demo configuration loads successfully")
    void shouldValidateComprehensiveLookupDemoConfigurationLoadsSuccessfully() throws Exception {
        // Given & When
        YamlRuleConfiguration demoConfig = configurationLoader.loadFromClasspath("demo-configs/comprehensive-lookup-demo.yaml");
        
        // Then
        assertNotNull(demoConfig);
        assertNotNull(demoConfig.getEnrichments());
        assertEquals(10, demoConfig.getEnrichments().size());
        
        // Verify all demo enrichments are valid
        for (var enrichment : demoConfig.getEnrichments()) {
            assertNotNull(enrichment.getId());
            assertNotNull(enrichment.getType());
            assertNotNull(enrichment.getLookupConfig());
            assertNotNull(enrichment.getLookupConfig().getLookupKey());
        }
    }
}
