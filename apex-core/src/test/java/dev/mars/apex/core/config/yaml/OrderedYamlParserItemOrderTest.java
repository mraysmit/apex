package dev.mars.apex.core.config.yaml;
import dev.mars.apex.core.config.model.*;
import dev.mars.apex.core.config.loader.*;
import dev.mars.apex.core.config.exception.*;
import dev.mars.apex.core.config.service.*;

import dev.mars.apex.core.config.sequential.OrderedYamlConfiguration;
import dev.mars.apex.core.config.sequential.OrderedYamlParser;
import dev.mars.apex.core.config.sequential.ProcessingItem;
import org.junit.jupiter.api.BeforeEach;

import dev.mars.apex.core.test.extension.ColoredTestOutputExtension;
import dev.mars.apex.core.test.extension.TestClassLoggingExtension;
import org.junit.jupiter.api.DisplayName;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;


import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for OrderedYamlParser item-level order extraction.
 * 
 * These tests validate that OrderedYamlParser correctly extracts the order of
 * individual items (enrichments, rules, groups, etc.) as they appear in the YAML document.
 * 
 * This is Step 2 of the Sequential Processing Fix implementation.
 * 
 * Test Coverage:
 * - Item order extraction from YAML documents
 * - Interleaved items from different sections
 * - Multiple items within same section
 * - Edge cases (empty sections, missing IDs, etc.)
 * 
 * @author Mark Andrew Ray-Smith Cityline Ltd  - Step 2
 */
@ExtendWith({ColoredTestOutputExtension.class, TestClassLoggingExtension.class})
class OrderedYamlParserItemOrderTest {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(OrderedYamlParserItemOrderTest.class);
    
    private OrderedYamlParser parser;
    
    @BeforeEach
    void setUp() {
        parser = new OrderedYamlParser();
        LOGGER.info("=== STEP 2 TEST: Item Order Extraction ===");
    }
    
    @Test
    @DisplayName("Should extract item order from simple YAML with enrichments and rules")
    void testSimpleItemOrderExtraction() throws ConfigurationException {
        LOGGER.info("Testing simple item order extraction...");
        
        String yaml = """
            metadata:
              name: "Simple Item Order Test"
              type: "test-config"
            
            enrichments:
              - id: "enrich-1"
                name: "First Enrichment"
                type: "lookup-enrichment"
              - id: "enrich-2"
                name: "Second Enrichment"
                type: "lookup-enrichment"
            
            rules:
              - id: "rule-1"
                name: "First Rule"
                condition: "true"
              - id: "rule-2"
                name: "Second Rule"
                condition: "true"
            """;
        
        OrderedYamlConfiguration orderedConfig = parser.parseYamlString(yaml);
        List<ProcessingItem> itemOrder = orderedConfig.getItemOrder();
        
        LOGGER.info("Extracted item order: " + itemOrder);
        
        // Verify item order
        assertEquals(4, itemOrder.size(), "Should extract 4 items");
        
        // Verify enrichments come first
        assertEquals("enrichments", itemOrder.get(0).getSectionType());
        assertEquals("enrich-1", itemOrder.get(0).getItemId());
        
        assertEquals("enrichments", itemOrder.get(1).getSectionType());
        assertEquals("enrich-2", itemOrder.get(1).getItemId());
        
        // Verify rules come second
        assertEquals("rules", itemOrder.get(2).getSectionType());
        assertEquals("rule-1", itemOrder.get(2).getItemId());
        
        assertEquals("rules", itemOrder.get(3).getSectionType());
        assertEquals("rule-2", itemOrder.get(3).getItemId());
        
        LOGGER.info("Simple item order extraction test PASSED");
    }
    
    @Test
    @DisplayName("Should extract item order with interleaved sections (E1, R1, E2, R2)")
    void testInterleavedItemOrder() throws ConfigurationException {
        LOGGER.info("Testing interleaved item order extraction...");
        
        // Note: YAML doesn't allow duplicate keys, so we can't have multiple enrichments/rules sections
        // This test verifies that items within sections are extracted in order
        String yaml = """
            metadata:
              name: "Interleaved Test"
              type: "test-config"
            
            enrichments:
              - id: "enrich-customer"
                name: "Enrich Customer"
                type: "lookup-enrichment"
              - id: "enrich-account"
                name: "Enrich Account"
                type: "lookup-enrichment"
            
            rules:
              - id: "validate-customer"
                name: "Validate Customer"
                condition: "#customerData != null"
              - id: "validate-account"
                name: "Validate Account"
                condition: "#accountData != null"
            
            enrichment-groups:
              - id: "group-1"
                name: "Enrichment Group 1"
                enrichment-ids:
                  - "enrich-customer"
            
            rule-groups:
              - id: "rule-group-1"
                name: "Rule Group 1"
                rule-ids:
                  - "validate-customer"
            """;
        
        OrderedYamlConfiguration orderedConfig = parser.parseYamlString(yaml);
        List<ProcessingItem> itemOrder = orderedConfig.getItemOrder();
        
        LOGGER.info("Extracted item order: " + itemOrder);
        
        // Verify all items are extracted in document order
        assertEquals(6, itemOrder.size(), "Should extract 6 items");
        
        // Verify order: enrichments → rules → enrichment-groups → rule-groups
        assertEquals("enrichments:enrich-customer", itemOrder.get(0).toString());
        assertEquals("enrichments:enrich-account", itemOrder.get(1).toString());
        assertEquals("rules:validate-customer", itemOrder.get(2).toString());
        assertEquals("rules:validate-account", itemOrder.get(3).toString());
        assertEquals("enrichment-groups:group-1", itemOrder.get(4).toString());
        assertEquals("rule-groups:rule-group-1", itemOrder.get(5).toString());
        
        LOGGER.info("Interleaved item order extraction test PASSED");
    }
    
    @Test
    @DisplayName("Should handle empty sections gracefully")
    void testEmptySections() throws ConfigurationException {
        LOGGER.info("Testing empty sections handling...");
        
        String yaml = """
            metadata:
              name: "Empty Sections Test"
              type: "test-config"
            
            enrichments: []
            
            rules:
              - id: "rule-1"
                name: "Only Rule"
                condition: "true"
            
            rule-groups: []
            """;
        
        OrderedYamlConfiguration orderedConfig = parser.parseYamlString(yaml);
        List<ProcessingItem> itemOrder = orderedConfig.getItemOrder();
        
        LOGGER.info("Extracted item order: " + itemOrder);
        
        // Should only extract the one rule
        assertEquals(1, itemOrder.size(), "Should extract 1 item");
        assertEquals("rules:rule-1", itemOrder.get(0).toString());
        
        LOGGER.info("Empty sections handling test PASSED");
    }
    
    @Test
    @DisplayName("Should handle items without IDs gracefully")
    void testItemsWithoutIds() throws ConfigurationException {
        LOGGER.info("Testing items without IDs handling...");
        
        String yaml = """
            metadata:
              name: "Missing ID Test"
              type: "test-config"
            
            enrichments:
              - id: "enrich-1"
                name: "First Enrichment"
                type: "lookup-enrichment"
              - name: "No ID Enrichment"
                type: "lookup-enrichment"
              - id: "enrich-2"
                name: "Second Enrichment"
                type: "lookup-enrichment"
            
            rules:
              - id: "rule-1"
                name: "First Rule"
                condition: "true"
            """;
        
        OrderedYamlConfiguration orderedConfig = parser.parseYamlString(yaml);
        List<ProcessingItem> itemOrder = orderedConfig.getItemOrder();
        
        LOGGER.info("Extracted item order: " + itemOrder);
        
        // Should extract only items with IDs
        assertEquals(3, itemOrder.size(), "Should extract 3 items (skipping item without ID)");
        assertEquals("enrichments:enrich-1", itemOrder.get(0).toString());
        assertEquals("enrichments:enrich-2", itemOrder.get(1).toString());
        assertEquals("rules:rule-1", itemOrder.get(2).toString());
        
        LOGGER.info("Items without IDs handling test PASSED");
    }
    
    @Test
    @DisplayName("Should extract all section types correctly")
    void testAllSectionTypes() throws ConfigurationException {
        LOGGER.info("Testing all section types extraction...");
        
        String yaml = """
            metadata:
              name: "All Sections Test"
              type: "test-config"
            
            enrichments:
              - id: "enrich-1"
                name: "Enrichment"
                type: "lookup-enrichment"
            
            rules:
              - id: "rule-1"
                name: "Rule"
                condition: "true"
            
            enrichment-groups:
              - id: "eg-1"
                name: "Enrichment Group"
                enrichment-ids: ["enrich-1"]
            
            rule-groups:
              - id: "rg-1"
                name: "Rule Group"
                rule-ids: ["rule-1"]
            
            transformations:
              - id: "transform-1"
                name: "Transformation"
                type: "field-mapping"
            
            rule-chains:
              - id: "chain-1"
                name: "Rule Chain"
                rule-ids: ["rule-1"]
            """;
        
        OrderedYamlConfiguration orderedConfig = parser.parseYamlString(yaml);
        List<ProcessingItem> itemOrder = orderedConfig.getItemOrder();
        
        LOGGER.info("Extracted item order: " + itemOrder);
        
        // Verify all section types are extracted
        assertEquals(6, itemOrder.size(), "Should extract 6 items");
        
        assertTrue(itemOrder.stream().anyMatch(item -> item.isEnrichment()), "Should have enrichment");
        assertTrue(itemOrder.stream().anyMatch(item -> item.isRule()), "Should have rule");
        assertTrue(itemOrder.stream().anyMatch(item -> item.isEnrichmentGroup()), "Should have enrichment group");
        assertTrue(itemOrder.stream().anyMatch(item -> item.isRuleGroup()), "Should have rule group");
        assertTrue(itemOrder.stream().anyMatch(item -> item.isTransformation()), "Should have transformation");
        assertTrue(itemOrder.stream().anyMatch(item -> item.isRuleChain()), "Should have rule chain");
        
        LOGGER.info("All section types extraction test PASSED");
    }
}


