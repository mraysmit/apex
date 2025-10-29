package dev.mars.apex.demo.sequencing;

import dev.mars.apex.core.config.yaml.OrderedYamlConfiguration;
import dev.mars.apex.core.config.yaml.OrderedYamlParser;
import dev.mars.apex.core.config.yaml.YamlConfigurationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Phase 1 Complex Tests: OrderedYamlParser Advanced Scenarios
 * 
 * These tests validate OrderedYamlParser with complex YAML configurations
 * that demonstrate the fundamental design flaw and its fix.
 * 
 * Test Coverage:
 * - Complex multi-section YAML files
 * - Real-world business logic patterns
 * - All supported YAML section types
 * - Section position and ordering logic
 * 
 * @author APEX Sequential Processing Implementation - Phase 1
 */
class OrderedYamlParserComplexTest {
    
    private static final Logger LOGGER = Logger.getLogger(OrderedYamlParserComplexTest.class.getName());
    
    private OrderedYamlParser parser;
    
    @BeforeEach
    void setUp() {
        parser = new OrderedYamlParser();
        LOGGER.info("=== PHASE 1 COMPLEX TEST: OrderedYamlParser Advanced Scenarios ===");
    }
    
    @Test
    @DisplayName("🎯 COMPLEX TEST: Should handle all YAML section types in order")
    void testAllSectionTypesInOrder() throws YamlConfigurationException {
        LOGGER.info("Testing all YAML section types in order...");
        
        String complexYaml = """
            metadata:
              name: "Complete APEX Configuration"
              type: "comprehensive-config"
              version: "2.0"
              processing-mode: "sequential"
            
            data-sources:
              - id: "customer-db"
                type: "database"
                connection-string: "jdbc:h2:mem:customers"
            
            categories:
              - id: "validation"
                name: "Data Validation"
                description: "Customer data validation rules"
            
            enrichments:
              - id: "enrich-customer"
                name: "Enrich Customer Data"
                type: "lookup-enrichment"
                data-source: "customer-db"
                lookup-key: "#customerId"
                target-field: "customerData"
            
            rules:
              - id: "validate-customer"
                name: "Validate Customer"
                category: "validation"
                condition: "#customerData != null && #customerData.status == 'ACTIVE'"
                message: "Customer is valid and active"
            
            rule-groups:
              - id: "customer-processing"
                name: "Customer Processing Group"
                rule-references:
                  - rule-id: "validate-customer"
            
            enrichment-groups:
              - id: "customer-enrichment-group"
                name: "Customer Enrichment Group"
                enrichment-references:
                  - enrichment-id: "enrich-customer"
            
            transformations:
              - id: "normalize-customer"
                name: "Normalize Customer Data"
                type: "field-transformation"
                source-field: "customerData.name"
                target-field: "normalizedName"
                transformation: "toUpperCase()"
            
            rule-chains:
              - id: "customer-chain"
                name: "Customer Processing Chain"
                pattern: "sequential"
                configuration:
                  stop-on-failure: true
            """;
        
        OrderedYamlConfiguration orderedConfig = parser.parseYamlString(complexYaml);
        List<String> sectionOrder = orderedConfig.getSectionOrder();
        
        LOGGER.info("Complex YAML section order: " + sectionOrder);
        
        // Verify all sections are detected in correct order
        String[] expectedOrder = {
            "metadata", "data-sources", "categories", "enrichments", 
            "rules", "rule-groups", "enrichment-groups", "transformations", "rule-chains"
        };
        
        assertEquals(expectedOrder.length, sectionOrder.size(), "Should detect all sections");
        
        for (int i = 0; i < expectedOrder.length; i++) {
            assertEquals(expectedOrder[i], sectionOrder.get(i), 
                        "Section " + i + " should be " + expectedOrder[i]);
        }
        
        // Verify specific ordering relationships
        assertTrue(orderedConfig.isSectionBefore("data-sources", "enrichments"),
                  "Data sources should come before enrichments");
        assertTrue(orderedConfig.isSectionBefore("enrichments", "rules"),
                  "Enrichments should come before rules");
        assertTrue(orderedConfig.isSectionBefore("rules", "rule-groups"),
                  "Rules should come before rule groups");
        
        // Verify populated sections
        List<String> populatedSections = orderedConfig.getPopulatedSections();
        assertEquals(expectedOrder.length, populatedSections.size(), 
                    "All sections should be populated");
        
        LOGGER.info("✅ All section types in order test PASSED");
    }
    
    @Test
    @DisplayName("🎯 BUSINESS LOGIC: Should preserve enrich-then-validate pattern order")
    void testEnrichThenValidatePattern() throws YamlConfigurationException {
        LOGGER.info("Testing enrich-then-validate pattern preservation...");
        
        String enrichThenValidateYaml = """
            metadata:
              name: "Enrich Then Validate Pattern"
              type: "business-logic-config"
              description: "Demonstrates enrich-then-validate business pattern"
            
            data-sources:
              - id: "customer-lookup"
                type: "database"
                connection-string: "jdbc:h2:mem:customers"
            
            # STEP 1: Enrich customer data first
            enrichments:
              - id: "enrich-customer-profile"
                name: "Enrich Customer Profile"
                type: "lookup-enrichment"
                condition: "#customerId != null"
                data-source: "customer-lookup"
                lookup-key: "#customerId"
                target-field: "customerProfile"
              
              - id: "enrich-customer-preferences"
                name: "Enrich Customer Preferences"
                type: "lookup-enrichment"
                condition: "#customerProfile != null"
                data-source: "customer-lookup"
                lookup-key: "#customerProfile.id"
                target-field: "preferences"
            
            # STEP 2: Validate enriched data second
            rules:
              - id: "validate-customer-profile"
                name: "Validate Customer Profile"
                condition: "#customerProfile != null && #customerProfile.status == 'ACTIVE'"
                message: "Customer profile is valid and active"
                priority: 100
              
              - id: "validate-customer-preferences"
                name: "Validate Customer Preferences"
                condition: "#preferences != null && #preferences.marketingOptIn == true"
                message: "Customer has valid marketing preferences"
                priority: 200
            """;
        
        OrderedYamlConfiguration orderedConfig = parser.parseYamlString(enrichThenValidateYaml);
        
        // Verify the business logic pattern is preserved
        assertTrue(orderedConfig.isSectionBefore("enrichments", "rules"),
                  "Enrichments must come before rules for enrich-then-validate pattern");
        
        // Verify section positions
        assertEquals(1, orderedConfig.getSectionPosition("data-sources"));
        assertEquals(2, orderedConfig.getSectionPosition("enrichments"));
        assertEquals(3, orderedConfig.getSectionPosition("rules"));
        
        // Verify content is parsed correctly
        assertEquals(2, orderedConfig.getConfiguration().getEnrichments().size());
        assertEquals(2, orderedConfig.getConfiguration().getRules().size());
        
        LOGGER.info("✅ Enrich-then-validate pattern test PASSED");
    }
    
    @Test
    @DisplayName("🎯 BUSINESS LOGIC: Should preserve validate-then-enrich pattern order")
    void testValidateThenEnrichPattern() throws YamlConfigurationException {
        LOGGER.info("Testing validate-then-enrich pattern preservation...");
        
        String validateThenEnrichYaml = """
            metadata:
              name: "Validate Then Enrich Pattern"
              type: "business-logic-config"
              description: "Demonstrates validate-then-enrich business pattern"
            
            # STEP 1: Validate input data first
            rules:
              - id: "validate-customer-id"
                name: "Validate Customer ID"
                condition: "#customerId != null && #customerId.length() >= 5"
                message: "Customer ID is valid format"
                priority: 10
              
              - id: "validate-request-type"
                name: "Validate Request Type"
                condition: "#requestType != null && #requestType in ['INQUIRY', 'UPDATE', 'DELETE']"
                message: "Request type is valid"
                priority: 20
            
            # STEP 2: Enrich only after validation passes
            enrichments:
              - id: "enrich-validated-customer"
                name: "Enrich Validated Customer"
                type: "lookup-enrichment"
                condition: "#customerId != null"
                data-source: "customer-db"
                lookup-key: "#customerId"
                target-field: "customerData"
            
            data-sources:
              - id: "customer-db"
                type: "database"
                connection-string: "jdbc:h2:mem:customers"
            """;
        
        OrderedYamlConfiguration orderedConfig = parser.parseYamlString(validateThenEnrichYaml);
        
        // Verify the business logic pattern is preserved
        assertTrue(orderedConfig.isSectionBefore("rules", "enrichments"),
                  "Rules must come before enrichments for validate-then-enrich pattern");
        
        // Verify section order
        List<String> sectionOrder = orderedConfig.getSectionOrder();
        assertEquals("metadata", sectionOrder.get(0));
        assertEquals("rules", sectionOrder.get(1));
        assertEquals("enrichments", sectionOrder.get(2));
        assertEquals("data-sources", sectionOrder.get(3));
        
        LOGGER.info("✅ Validate-then-enrich pattern test PASSED");
    }
    
    @Test
    @DisplayName("🎯 SECTION POSITIONS: Should provide accurate position information")
    void testSectionPositionAccuracy() throws YamlConfigurationException {
        LOGGER.info("Testing section position accuracy...");
        
        String positionTestYaml = """
            metadata:
              name: "Position Test"
            
            categories:
              - id: "test-category"
            
            enrichments:
              - id: "test-enrichment"
            
            transformations:
              - id: "test-transformation"
            
            rules:
              - id: "test-rule"
            
            rule-groups:
              - id: "test-group"
            """;
        
        OrderedYamlConfiguration orderedConfig = parser.parseYamlString(positionTestYaml);
        
        // Test position accuracy
        assertEquals(0, orderedConfig.getSectionPosition("metadata"));
        assertEquals(1, orderedConfig.getSectionPosition("categories"));
        assertEquals(2, orderedConfig.getSectionPosition("enrichments"));
        assertEquals(3, orderedConfig.getSectionPosition("transformations"));
        assertEquals(4, orderedConfig.getSectionPosition("rules"));
        assertEquals(5, orderedConfig.getSectionPosition("rule-groups"));
        
        // Test non-existent section
        assertEquals(-1, orderedConfig.getSectionPosition("non-existent"));
        
        // Test section comparison
        assertTrue(orderedConfig.isSectionBefore("categories", "enrichments"));
        assertTrue(orderedConfig.isSectionBefore("enrichments", "transformations"));
        assertTrue(orderedConfig.isSectionBefore("transformations", "rules"));
        assertFalse(orderedConfig.isSectionBefore("rules", "enrichments"));
        
        LOGGER.info("✅ Section position accuracy test PASSED");
    }
}
