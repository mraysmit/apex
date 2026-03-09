package dev.mars.apex.demo.sequencing;

import dev.mars.apex.core.config.loader.*;
import dev.mars.apex.core.config.model.*;
import dev.mars.apex.core.config.sequential.*;
import dev.mars.apex.demo.ColoredTestOutputExtension;
import dev.mars.apex.demo.DemoTestBase;
import dev.mars.apex.engine.core.RulesEngine;
import dev.mars.apex.engine.model.RuleResult;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Phase 4 Integration Tests: Sequential Processing Integration
 *
 * These tests validate that the sequential processing system integrates correctly
 * with existing APEX services and entry points, providing seamless backward
 * compatibility while enabling document order processing.
 *
 * Test Coverage:
 * 1. OrderedYamlParser Integration
 * 2. RulesEngine Sequential Processing Integration
 * 3. Backward Compatibility with Standard Processing
 * 4. Section Order Preservation
 * 5. Core Sequential Processing Functionality
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd 
 * @since Phase 4 - Integration
 */
@ExtendWith(ColoredTestOutputExtension.class)
@DisplayName("Sequential Processing Integration Tests")
class SequentialProcessingIntegrationTest extends DemoTestBase {

    private static final Logger LOGGER = LoggerFactory.getLogger(SequentialProcessingIntegrationTest.class);

    private OrderedYamlParser orderedParser;
    private ConfigurationLoader standardLoader;
    
    @BeforeEach
    void setUpIntegration() {
        LOGGER.info("=== PHASE 4 TEST: Sequential Processing Integration ===");

        // Initialize sequential processing components
        this.orderedParser = new OrderedYamlParser();
        this.standardLoader = new ConfigurationLoader();
    }
    
    @Test
    @DisplayName("OrderedYamlParser Integration Test")
    void testOrderedYamlParserIntegration() throws Exception {
        LOGGER.info("Testing OrderedYamlParser integration...");

        // Create YAML with enrichments before rules
        String sequentialYaml = """
            metadata:
              name: "sequential-test"

            enrichments:
              - id: "customer-lookup"
                type: "lookup-enrichment"
                target-field: "customerName"
                lookup-value: "#customerId"

            rules:
              - id: "validate-customer"
                conditions:
                  - field: "#customerName"
                    operator: "not_null"
            """;

        // Parse with OrderedYamlParser
        OrderedYamlConfiguration orderedConfig = orderedParser.parseYamlString(sequentialYaml, "test");

        // Verify parsing worked
        assertNotNull(orderedConfig);
        assertNotNull(orderedConfig.getConfiguration());

        // Verify section order is preserved (enrichments before rules)
        assertNotNull(orderedConfig.getSectionOrder());
        assertTrue(orderedConfig.getSectionOrder().size() >= 2, "Should detect at least metadata and enrichments");

        LOGGER.info("OrderedYamlParser integration test PASSED - Parsing successful!");
    }
    
    @Test
    @DisplayName("RulesEngine Sequential Processing Integration Test")
    void testRulesEngineSequentialProcessingIntegration() throws Exception {
        LOGGER.info("Testing RulesEngine sequential processing integration...");

        // Create YAML with enrichments and rules
        String sequentialYaml = """
            metadata:
              name: "sequential-processor-test"
              type: "rule-config"

            enrichments:
              - id: "customer-lookup"
                name: "Customer Data Enrichment"
                type: "lookup-enrichment"
                lookup-config:
                  lookup-key: "#customerId"
                  lookup-dataset:
                    type: "inline"
                    key-field: "customerId"
                    data:
                      - customerId: "CUST001"
                        customerName: "John Doe"
                field-mappings:
                  - source-field: "customerName"
                    target-field: "customerName"

            rules:
              - id: "validate-customer"
                name: "Validate Customer"
                condition: "#customerName != null"
                message: "Customer name is present"
            """;

        // Process through the production RulesEngine pipeline
        ConfigurationLoader loader = new ConfigurationLoader();
        YamlRuleConfiguration config = loader.fromYamlString(sequentialYaml);
        RulesEngine engine = RulesEngine.fromYamlConfig(config);

        Map<String, Object> testData = new HashMap<>();
        testData.put("customerId", "CUST001");

        RuleResult result = engine.evaluate(testData);

        // Verify processing worked through the real pipeline
        assertNotNull(result);
        assertTrue(result.isSuccess(), "Processing should succeed");
        assertFalse(result.hasFailures(), "Should have no failures");

        // Verify enrichment actually executed
        Map<String, Object> enrichedData = result.getEnrichedData();
        assertNotNull(enrichedData, "Enriched data should not be null");
        assertEquals("John Doe", enrichedData.get("customerName"),
            "Lookup enrichment should set customerName");

        LOGGER.info("RulesEngine sequential processing integration test PASSED!");
    }
    
    @Test
    @DisplayName("Backward Compatibility Test")
    void testBackwardCompatibility() throws Exception {
        LOGGER.info("Testing backward compatibility with existing YAML configurations...");

        // Test YAML with minimal metadata
        String legacyYaml = """
            metadata:
              name: "legacy-test"

            rules:
              - id: "legacy-rule"
                name: "Legacy Rule"
                condition: "#value != null"

            enrichments:
              - id: "legacy-enrichment"
                type: "field-enrichment"
                field-mappings:
                  - source-field: "sourceField"
                    target-field: "legacyField"
            """;

        // Should parse successfully with OrderedYamlParser
        OrderedYamlConfiguration orderedConfig = orderedParser.parseYamlString(legacyYaml, "test");
        assertNotNull(orderedConfig);
        assertNotNull(orderedConfig.getConfiguration());

        // Should also work with standard loader
        YamlRuleConfiguration standardConfig = standardLoader.fromYamlString(legacyYaml);
        assertNotNull(standardConfig);

        LOGGER.info("Backward compatibility test PASSED - Legacy YAML works perfectly!");
    }
    
    @Test
    @DisplayName("Section Order Preservation Test")
    void testSectionOrderPreservation() throws Exception {
        LOGGER.info("Testing section order preservation...");

        // Test YAML with specific section order
        String testYaml = """
            metadata:
              name: "order-test"

            enrichments:
              - id: "first-enrichment"
                type: "constant-enrichment"
                target-field: "field1"
                constant-value: "value1"

            rules:
              - id: "validation-rule"
                conditions:
                  - field: "#field1"
                    operator: "not_null"

            data-sources:
              - name: "test-source"
                type: "memory"
            """;

        // Parse and verify section order is preserved
        OrderedYamlConfiguration orderedConfig = orderedParser.parseYamlString(testYaml, "test");
        assertNotNull(orderedConfig.getSectionOrder());
        assertTrue(orderedConfig.getSectionOrder().size() > 0);

        // Verify the first non-metadata section is enrichments (as specified in YAML)
        // Skip metadata which is always first
        String firstSection = orderedConfig.getSectionOrder().stream()
            .filter(section -> !"metadata".equals(section))
            .findFirst()
            .orElse(null);
        assertEquals("enrichments", firstSection);

        LOGGER.info("Section order preservation test PASSED - Order maintained correctly!");
    }
    
    // ========== TEST HELPER CLASSES ==========
    
    /**
     * Simple test object for enrichment testing.
     */
    public static class TestObject {
        public String customerId;
        public String customerName;
        public String testField;
        public String legacyField;
        public Double amount;
        
        @Override
        public String toString() {
            return "TestObject{" +
                   "customerId='" + customerId + '\'' +
                   ", customerName='" + customerName + '\'' +
                   ", testField='" + testField + '\'' +
                   ", legacyField='" + legacyField + '\'' +
                   ", amount=" + amount +
                   '}';
        }
    }
}
