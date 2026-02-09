package dev.mars.apex.demo.sequencing;

import dev.mars.apex.core.config.yaml.*;
import dev.mars.apex.core.config.yaml.sequential.*;
import dev.mars.apex.core.engine.model.RuleResult;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.logging.Logger;

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
 * 2. SequentialYamlProcessor Integration
 * 3. Processing Mode Detection and Selection
 * 4. Backward Compatibility with Standard Processing
 * 5. Core Sequential Processing Functionality
 *
 * @author APEX Sequential Processing Implementation
 * @since Phase 4 - Integration
 */
@DisplayName("Phase 4: Sequential Processing Integration Tests")
class SequentialProcessingIntegrationTest {

    private static final Logger LOGGER = Logger.getLogger(SequentialProcessingIntegrationTest.class.getName());

    private OrderedYamlParser orderedParser;
    private SequentialYamlProcessor sequentialProcessor;
    private YamlConfigurationLoader standardLoader;
    
    @BeforeEach
    void setUp() {
        LOGGER.info("=== PHASE 4 TEST: Sequential Processing Integration ===");

        // Initialize sequential processing components
        this.orderedParser = new OrderedYamlParser();
        this.sequentialProcessor = new SequentialYamlProcessor();
        this.standardLoader = new YamlConfigurationLoader();
    }
    
    @Test
    @DisplayName("OrderedYamlParser Integration Test")
    void testOrderedYamlParserIntegration() throws Exception {
        LOGGER.info("Testing OrderedYamlParser integration...");

        // Create YAML with sequential processing mode
        String sequentialYaml = """
            metadata:
              name: "sequential-test"
              processing-mode: "sequential"

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
        assertEquals(OrderedYamlConfiguration.ProcessingMode.SEQUENTIAL, orderedConfig.getProcessingMode());

        LOGGER.info("OrderedYamlParser integration test PASSED - Parsing successful!");
    }
    
    @Test
    @DisplayName("SequentialYamlProcessor Integration Test")
    void testSequentialYamlProcessorIntegration() throws Exception {
        LOGGER.info("Testing SequentialYamlProcessor integration...");

        // Create YAML with sequential processing mode
        String sequentialYaml = """
            metadata:
              name: "sequential-processor-test"
              processing-mode: "sequential"

            enrichments:
              - id: "customer-lookup"
                type: "constant-enrichment"
                target-field: "customerName"
                constant-value: "John Doe"

            rules:
              - id: "validate-customer"
                conditions:
                  - field: "#customerName"
                    operator: "not_null"
            """;

        // Parse and process sequentially
        OrderedYamlConfiguration orderedConfig = orderedParser.parseYamlString(sequentialYaml, "test");
        RuleResult result = sequentialProcessor.processOrderedConfigurationWithResult(orderedConfig, "test");

        // Verify processing worked
        assertNotNull(result);
        assertTrue(result.isSuccess(), "Processing should succeed");
        assertEquals(RuleResult.ResultType.MATCH, result.getResultType(), "Result type should be MATCH");
        assertFalse(result.hasFailures(), "Should have no failures");

        LOGGER.info("SequentialYamlProcessor integration test PASSED - Processing successful!");
    }
    
    @Test
    @DisplayName("Processing Mode Detection Test")
    void testProcessingModeDetection() throws Exception {
        LOGGER.info("Testing processing mode detection...");

        // Test sequential mode detection
        String sequentialYaml = """
            metadata:
              processing-mode: "sequential"
            enrichments:
              - id: "test-enrichment"
            """;

        OrderedYamlConfiguration orderedConfig = orderedParser.parseYamlString(sequentialYaml, "test");
        assertEquals(OrderedYamlConfiguration.ProcessingMode.SEQUENTIAL, orderedConfig.getProcessingMode());

        // Test standard mode detection
        String standardYaml = """
            metadata:
              processing-mode: "standard"
            enrichments:
              - id: "test-enrichment"
            """;

        OrderedYamlConfiguration standardConfig = orderedParser.parseYamlString(standardYaml, "test");
        assertEquals(OrderedYamlConfiguration.ProcessingMode.STANDARD, standardConfig.getProcessingMode());

        LOGGER.info("Processing mode detection test PASSED - Modes detected correctly!");
    }
    
    @Test
    @DisplayName("Backward Compatibility Test")
    void testBackwardCompatibility() throws Exception {
        LOGGER.info("Testing backward compatibility with existing YAML configurations...");

        // Test YAML without processing-mode (should default to standard)
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

        // Should parse and default to STANDARD mode
        OrderedYamlConfiguration orderedConfig = orderedParser.parseYamlString(legacyYaml, "test");
        assertEquals(OrderedYamlConfiguration.ProcessingMode.STANDARD, orderedConfig.getProcessingMode());

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
              processing-mode: "sequential"

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
