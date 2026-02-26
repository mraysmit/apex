package dev.mars.apex.demo.sequencing;

import dev.mars.apex.core.config.sequential.OrderedYamlConfiguration;
import dev.mars.apex.core.config.sequential.OrderedYamlParser;
import dev.mars.apex.core.config.exception.YamlConfigurationException;
import dev.mars.apex.core.config.model.YamlRuleConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Phase 1 Tests: OrderedYamlParser Validation
 * 
 * These tests validate that OrderedYamlParser correctly preserves YAML section order
 * and parses all content accurately, enabling document order preservation.
 * 
 * Test Coverage:
 * - Section order preservation from YAML documents
 * - Accurate parsing of all YAML content
 * - Compatibility with existing YamlRuleConfiguration structure
 * - Edge cases and error handling
 * 
 * @author Mark Andrew Ray-Smith Cityline Ltd  - Phase 1
 */
class OrderedYamlParserTest {
    
    private static final Logger LOGGER = Logger.getLogger(OrderedYamlParserTest.class.getName());
    
    private OrderedYamlParser parser;
    
    @BeforeEach
    void setUp() {
        parser = new OrderedYamlParser();
        LOGGER.info("=== PHASE 1 TEST: OrderedYamlParser Validation ===");
    }
    
    @Test
    @DisplayName("🎯 CORE TEST: Should preserve section order from YAML document")
    void testSectionOrderPreservation() throws YamlConfigurationException {
        LOGGER.info("Testing section order preservation...");
        
        // YAML with intentional section order: enrichments BEFORE rules
        String yaml = """
            metadata:
              name: "Order Preservation Test"
              type: "test-config"
            
            enrichments:
              - id: "enrich-customer-data"
                name: "Enrich Customer Data"
                type: "lookup-enrichment"
                condition: "#customerId != null"
                lookup-key: "#customerId"
                data-source: "customer-db"
                target-field: "customerInfo"
            
            rules:
              - id: "validate-enriched-data"
                name: "Validate Enriched Data"
                condition: "#customerInfo != null && #customerInfo.status == 'ACTIVE'"
                message: "Customer is active and data is enriched"
            """;
        
        // Parse with order preservation
        OrderedYamlConfiguration orderedConfig = parser.parseYamlString(yaml);
        
        // Verify section order is preserved
        List<String> sectionOrder = orderedConfig.getSectionOrder();
        LOGGER.info("Detected section order: " + sectionOrder);
        
        // Assert correct order: metadata, enrichments, rules
        assertEquals(3, sectionOrder.size(), "Should detect 3 sections");
        assertEquals("metadata", sectionOrder.get(0), "First section should be metadata");
        assertEquals("enrichments", sectionOrder.get(1), "Second section should be enrichments");
        assertEquals("rules", sectionOrder.get(2), "Third section should be rules");
        
        // Verify enrichments come before rules (developer intent)
        assertTrue(orderedConfig.isSectionBefore("enrichments", "rules"), 
                  "Enrichments should come before rules in document order");
        
        // Verify configuration content is parsed correctly
        YamlRuleConfiguration config = orderedConfig.getConfiguration();
        assertNotNull(config.getMetadata(), "Metadata should be parsed");
        assertEquals("Order Preservation Test", config.getMetadata().getName());
        
        assertNotNull(config.getEnrichments(), "Enrichments should be parsed");
        assertEquals(1, config.getEnrichments().size());
        assertEquals("enrich-customer-data", config.getEnrichments().get(0).getId());
        
        assertNotNull(config.getRules(), "Rules should be parsed");
        assertEquals(1, config.getRules().size());
        assertEquals("validate-enriched-data", config.getRules().get(0).getId());
        
        LOGGER.info("Section order preservation test PASSED");
    }
    
    @Test
    @DisplayName("🎯 CORE TEST: Should handle rules BEFORE enrichments order")
    void testRulesBeforeEnrichmentsOrder() throws YamlConfigurationException {
        LOGGER.info("Testing rules before enrichments order...");
        
        // YAML with rules BEFORE enrichments (validate-then-enrich pattern)
        String yaml = """
            metadata:
              name: "Rules First Test"
              type: "test-config"
            
            rules:
              - id: "validate-input"
                name: "Validate Input Data"
                condition: "#customerId != null && #customerId.length() > 0"
                message: "Customer ID is valid"
            
            enrichments:
              - id: "enrich-after-validation"
                name: "Enrich After Validation"
                type: "lookup-enrichment"
                condition: "#customerId != null"
                lookup-key: "#customerId"
                data-source: "customer-db"
                target-field: "customerInfo"
            """;
        
        OrderedYamlConfiguration orderedConfig = parser.parseYamlString(yaml);
        List<String> sectionOrder = orderedConfig.getSectionOrder();
        
        // Verify rules come before enrichments
        assertEquals("metadata", sectionOrder.get(0));
        assertEquals("rules", sectionOrder.get(1));
        assertEquals("enrichments", sectionOrder.get(2));
        
        assertTrue(orderedConfig.isSectionBefore("rules", "enrichments"),
                  "Rules should come before enrichments in document order");
        
        LOGGER.info("Rules before enrichments order test PASSED");
    }
    
    @Test
    @DisplayName("🎯 EDGE CASE: Should handle empty sections gracefully")
    void testEmptySectionsHandling() throws YamlConfigurationException {
        LOGGER.info("Testing empty sections handling...");
        
        String yaml = """
            metadata:
              name: "Empty Sections Test"
              type: "test-config"
            
            rules: []
            enrichments: []
            rule-groups: []
            """;
        
        OrderedYamlConfiguration orderedConfig = parser.parseYamlString(yaml);
        
        // Verify sections are detected even if empty
        List<String> sectionOrder = orderedConfig.getSectionOrder();
        assertTrue(sectionOrder.contains("rules"), "Empty rules section should be detected");
        assertTrue(sectionOrder.contains("enrichments"), "Empty enrichments section should be detected");
        assertTrue(sectionOrder.contains("rule-groups"), "Empty rule-groups section should be detected");
        
        // Verify populated sections excludes empty ones
        List<String> populatedSections = orderedConfig.getPopulatedSections();
        assertTrue(populatedSections.contains("metadata"), "Metadata should be populated");
        assertFalse(populatedSections.contains("rules"), "Empty rules should not be populated");
        assertFalse(populatedSections.contains("enrichments"), "Empty enrichments should not be populated");
        
        LOGGER.info("Empty sections handling test PASSED");
    }
    
    @Test
    @DisplayName("🎯 ERROR HANDLING: Should handle invalid YAML gracefully")
    void testInvalidYamlHandling() {
        LOGGER.info("Testing invalid YAML handling...");
        
        String invalidYaml = """
            metadata:
              name: "Invalid YAML Test"
            rules:
              - id: "test-rule"
                condition: "#invalid syntax here [[[
            """;
        
        // Should throw YamlConfigurationException for invalid YAML
        assertThrows(YamlConfigurationException.class, () -> {
            parser.parseYamlString(invalidYaml);
        }, "Invalid YAML should throw YamlConfigurationException");
        
        LOGGER.info("Invalid YAML handling test PASSED");
    }
    
    @Test
    @DisplayName("🎯 COMPATIBILITY: Should maintain full compatibility with YamlRuleConfiguration")
    void testYamlRuleConfigurationCompatibility() throws YamlConfigurationException {
        LOGGER.info("Testing YamlRuleConfiguration compatibility...");
        
        String yaml = """
            metadata:
              name: "Compatibility Test"
              type: "test-config"
              version: "1.0"
              description: "Testing full compatibility"
            
            data-sources:
              - name: "test-db"
                type: "database"
                connection-string: "jdbc:h2:mem:test"
            
            enrichments:
              - id: "test-enrichment"
                name: "Test Enrichment"
                type: "lookup-enrichment"
                data-source: "test-db"
                lookup-key: "#id"
                target-field: "enrichedData"
            
            rules:
              - id: "test-rule"
                name: "Test Rule"
                condition: "#enrichedData != null"
                message: "Data is enriched"
                priority: 100
                severity: "INFO"
            
            rule-groups:
              - id: "test-group"
                name: "Test Group"
                rule-references:
                  - rule-id: "test-rule"
            """;
        
        OrderedYamlConfiguration orderedConfig = parser.parseYamlString(yaml);
        YamlRuleConfiguration config = orderedConfig.getConfiguration();
        
        // Verify all sections are parsed correctly
        assertNotNull(config.getMetadata());
        assertEquals("Compatibility Test", config.getMetadata().getName());
        assertEquals("1.0", config.getMetadata().getVersion());
        
        assertNotNull(config.getDataSources());
        assertEquals(1, config.getDataSources().size());
        assertEquals("test-db", config.getDataSources().get(0).getName());
        
        assertNotNull(config.getEnrichments());
        assertEquals(1, config.getEnrichments().size());
        assertEquals("test-enrichment", config.getEnrichments().get(0).getId());
        
        assertNotNull(config.getRules());
        assertEquals(1, config.getRules().size());
        assertEquals("test-rule", config.getRules().get(0).getId());
        assertEquals(Integer.valueOf(100), config.getRules().get(0).getPriority());
        
        assertNotNull(config.getRuleGroups());
        assertEquals(1, config.getRuleGroups().size());
        assertEquals("test-group", config.getRuleGroups().get(0).getId());
        
        LOGGER.info("YamlRuleConfiguration compatibility test PASSED");
    }
}
