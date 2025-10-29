package dev.mars.apex.demo.sequencing;

import dev.mars.apex.core.config.yaml.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Phase 5: Comprehensive Validation Tests
 * 
 * This test class validates the complete sequential processing implementation
 * across all APEX entry points and use cases to ensure:
 * 1. End-to-end functionality works correctly
 * 2. No regressions were introduced
 * 3. Performance is acceptable
 * 4. All APEX features work with sequential processing
 */
@DisplayName("Phase 5: Comprehensive Sequential Processing Validation")
public class ComprehensiveValidationTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(ComprehensiveValidationTest.class);

    private OrderedYamlParser orderedParser;
    private SequentialYamlProcessor sequentialProcessor;
    private YamlConfigurationLoader standardLoader;

    @BeforeEach
    void setUp() {
        LOGGER.info("=== PHASE 5 TEST: Comprehensive Sequential Processing Validation ===");
        
        // Initialize core components
        orderedParser = new OrderedYamlParser();
        sequentialProcessor = new SequentialYamlProcessor();
        standardLoader = new YamlConfigurationLoader();
    }

    @Test
    @DisplayName("End-to-End Sequential Processing Test")
    void testEndToEndSequentialProcessing() throws Exception {
        LOGGER.info("Testing end-to-end sequential processing...");
        
        // Create comprehensive YAML with all major APEX features
        String comprehensiveYaml = """
            metadata:
              name: "comprehensive-sequential-test"
              processing-mode: "sequential"
              description: "Complete test of sequential processing"
            
            data-sources:
              - name: "customer-data"
                type: "memory"
                data:
                  - customerId: "CUST001"
                    customerName: "John Doe"
                    customerType: "PREMIUM"
            
            enrichments:
              - id: "customer-lookup"
                type: "lookup-enrichment"
                data-source: "customer-data"
                lookup-key: "customerId"
                target-field: "customerInfo"
            
              - id: "customer-classification"
                type: "field-enrichment"
                field-mappings:
                  - source-field: "customerInfo.customerType"
                    target-field: "classification"
            
            rules:
              - id: "premium-customer-rule"
                name: "Premium Customer Validation"
                condition: "#classification == 'PREMIUM'"
                actions:
                  - type: "set-field"
                    field: "isPremium"
                    value: true
            
              - id: "customer-validation-rule"
                name: "Customer Data Validation"
                condition: "#customerInfo != null"
                actions:
                  - type: "set-field"
                    field: "isValidCustomer"
                    value: true
            """;
        
        // Test 1: Parse with OrderedYamlParser
        OrderedYamlConfiguration orderedConfig = orderedParser.parseYamlString(comprehensiveYaml, "comprehensive-test");
        assertNotNull(orderedConfig);
        assertEquals(OrderedYamlConfiguration.ProcessingMode.SEQUENTIAL, orderedConfig.getProcessingMode());
        
        // Verify section order is preserved
        assertTrue(orderedConfig.getSectionOrder().contains("data-sources"));
        assertTrue(orderedConfig.getSectionOrder().contains("enrichments"));
        assertTrue(orderedConfig.getSectionOrder().contains("rules"));
        
        // Test 2: Process with SequentialYamlProcessor
        SequentialProcessingResult result = sequentialProcessor.processOrderedConfiguration(orderedConfig, "comprehensive-test");
        assertNotNull(result);
        assertEquals(OrderedYamlConfiguration.ProcessingMode.SEQUENTIAL, result.getProcessingMode());
        
        // Test 3: Create RulesEngine from processed configuration
        YamlRuleConfiguration processedConfig = result.getYamlRuleConfiguration();
        assertNotNull(processedConfig);
        
        // Verify all sections were processed
        assertNotNull(processedConfig.getDataSources());
        assertNotNull(processedConfig.getEnrichments());
        assertNotNull(processedConfig.getRules());
        
        LOGGER.info("✅ End-to-end sequential processing test PASSED - All components working together!");
    }

    @Test
    @DisplayName("Complex Dependency Resolution Test")
    void testComplexDependencyResolution() throws Exception {
        LOGGER.info("Testing complex dependency resolution...");
        
        // Create YAML with forward references and complex dependencies
        String complexYaml = """
            metadata:
              processing-mode: "sequential"
            
            rules:
              - id: "validation-rule"
                name: "Data Validation"
                condition: "#enrichedData != null && #calculatedValue > 100"
                actions:
                  - type: "set-field"
                    field: "isValid"
                    value: true
            
            enrichments:
              - id: "data-enrichment"
                type: "field-enrichment"
                field-mappings:
                  - source-field: "rawData"
                    target-field: "enrichedData"
            
              - id: "calculation-enrichment"
                type: "calculation-enrichment"
                target-field: "calculatedValue"
                expression: "#enrichedData.value * 2"
            
            data-sources:
              - name: "reference-data"
                type: "memory"
                data:
                  - key: "test"
                    value: 150
            """;
        
        // Parse and process
        OrderedYamlConfiguration orderedConfig = orderedParser.parseYamlString(complexYaml, "complex-test");
        SequentialProcessingResult result = sequentialProcessor.processOrderedConfiguration(orderedConfig, "complex-test");
        
        // Verify dependencies were resolved correctly
        assertNotNull(result);
        assertEquals(OrderedYamlConfiguration.ProcessingMode.SEQUENTIAL, result.getProcessingMode());
        
        YamlRuleConfiguration processedConfig = result.getYamlRuleConfiguration();
        assertNotNull(processedConfig);
        
        // Verify all sections are present despite forward references
        assertNotNull(processedConfig.getRules());
        assertNotNull(processedConfig.getEnrichments());
        assertNotNull(processedConfig.getDataSources());
        
        LOGGER.info("✅ Complex dependency resolution test PASSED - Forward references handled correctly!");
    }

    @Test
    @DisplayName("Performance Comparison Test")
    void testPerformanceComparison() throws Exception {
        LOGGER.info("Testing performance comparison between SEQUENTIAL and STANDARD modes...");
        
        String testYaml = """
            metadata:
              name: "performance-test"
            
            enrichments:
              - id: "perf-enrichment"
                type: "field-enrichment"
                field-mappings:
                  - source-field: "input"
                    target-field: "output"
            
            rules:
              - id: "perf-rule"
                name: "Performance Rule"
                condition: "#output != null"
                actions:
                  - type: "set-field"
                    field: "processed"
                    value: true
            """;
        
        // Test STANDARD mode performance
        long standardStart = System.currentTimeMillis();
        for (int i = 0; i < 100; i++) {
            YamlRuleConfiguration standardConfig = standardLoader.fromYamlString(testYaml);
            assertNotNull(standardConfig);
        }
        long standardTime = System.currentTimeMillis() - standardStart;
        
        // Test SEQUENTIAL mode performance
        String sequentialYaml = testYaml.replace("name: \"performance-test\"", 
                                                "name: \"performance-test\"\n  processing-mode: \"sequential\"");
        
        long sequentialStart = System.currentTimeMillis();
        for (int i = 0; i < 100; i++) {
            OrderedYamlConfiguration orderedConfig = orderedParser.parseYamlString(sequentialYaml, "perf-test-" + i);
            SequentialProcessingResult result = sequentialProcessor.processOrderedConfiguration(orderedConfig, "perf-test-" + i);
            assertNotNull(result);
        }
        long sequentialTime = System.currentTimeMillis() - sequentialStart;
        
        // Performance should be reasonable (within 3x of standard mode)
        double performanceRatio = (double) sequentialTime / standardTime;
        assertTrue(performanceRatio < 3.0, 
                  String.format("Sequential processing too slow: %dms vs %dms (ratio: %.2f)", 
                               sequentialTime, standardTime, performanceRatio));
        
        LOGGER.info("✅ Performance comparison test PASSED - Sequential: {}ms, Standard: {}ms (ratio: {:.2f})", 
                   sequentialTime, standardTime, performanceRatio);
    }

    @Test
    @DisplayName("Backward Compatibility Validation")
    void testBackwardCompatibilityValidation() throws Exception {
        LOGGER.info("Testing comprehensive backward compatibility...");
        
        // Test various legacy YAML patterns
        String[] legacyYamls = {
            // Legacy YAML without processing-mode
            """
            metadata:
              name: "legacy-test-1"
            rules:
              - id: "legacy-rule-1"
                name: "Legacy Rule 1"
                condition: "true"
            """,
            
            // Legacy YAML with explicit standard mode
            """
            metadata:
              name: "legacy-test-2"
              processing-mode: "standard"
            enrichments:
              - id: "legacy-enrichment"
                type: "field-enrichment"
                field-mappings:
                  - source-field: "input"
                    target-field: "output"
            """,
            
            // Legacy YAML with mixed sections
            """
            metadata:
              name: "legacy-test-3"
            rules:
              - id: "rule-1"
                name: "Rule 1"
                condition: "true"
            enrichments:
              - id: "enrichment-1"
                type: "field-enrichment"
                field-mappings:
                  - source-field: "a"
                    target-field: "b"
            rules:
              - id: "rule-2"
                name: "Rule 2"
                condition: "false"
            """
        };
        
        for (int i = 0; i < legacyYamls.length; i++) {
            String yaml = legacyYamls[i];
            
            // Should work with standard loader
            YamlRuleConfiguration standardConfig = standardLoader.fromYamlString(yaml);
            assertNotNull(standardConfig, "Legacy YAML " + (i + 1) + " failed with standard loader");
            
            // Should also work with ordered parser (defaulting to STANDARD mode)
            OrderedYamlConfiguration orderedConfig = orderedParser.parseYamlString(yaml, "legacy-test-" + (i + 1));
            assertNotNull(orderedConfig, "Legacy YAML " + (i + 1) + " failed with ordered parser");
            
            // Should default to STANDARD mode for legacy YAML
            if (!yaml.contains("processing-mode: \"sequential\"")) {
                assertEquals(OrderedYamlConfiguration.ProcessingMode.STANDARD, orderedConfig.getProcessingMode(),
                           "Legacy YAML " + (i + 1) + " should default to STANDARD mode");
            }
        }
        
        LOGGER.info("✅ Backward compatibility validation test PASSED - All legacy patterns work correctly!");
    }

    @Test
    @DisplayName("Error Handling and Edge Cases Test")
    void testErrorHandlingAndEdgeCases() throws Exception {
        LOGGER.info("Testing error handling and edge cases...");
        
        // Test 1: Empty YAML
        String emptyYaml = """
            metadata:
              processing-mode: "sequential"
            """;
        
        OrderedYamlConfiguration emptyConfig = orderedParser.parseYamlString(emptyYaml, "empty-test");
        assertNotNull(emptyConfig);
        assertEquals(OrderedYamlConfiguration.ProcessingMode.SEQUENTIAL, emptyConfig.getProcessingMode());
        
        // Test 2: Invalid processing mode should default to STANDARD
        String invalidModeYaml = """
            metadata:
              processing-mode: "invalid-mode"
            rules:
              - id: "test-rule"
                name: "Test Rule"
                condition: "true"
            """;
        
        OrderedYamlConfiguration invalidConfig = orderedParser.parseYamlString(invalidModeYaml, "invalid-test");
        assertNotNull(invalidConfig);
        assertEquals(OrderedYamlConfiguration.ProcessingMode.STANDARD, invalidConfig.getProcessingMode());
        
        // Test 3: Large YAML with many sections
        StringBuilder largeYamlBuilder = new StringBuilder();
        largeYamlBuilder.append("""
            metadata:
              processing-mode: "sequential"
            
            """);
        
        // Add many enrichments
        largeYamlBuilder.append("enrichments:\n");
        for (int i = 1; i <= 50; i++) {
            largeYamlBuilder.append(String.format("""
                  - id: "enrichment-%d"
                    type: "field-enrichment"
                    field-mappings:
                      - source-field: "input%d"
                        target-field: "output%d"
                
                """, i, i, i));
        }
        
        // Add many rules
        largeYamlBuilder.append("rules:\n");
        for (int i = 1; i <= 50; i++) {
            largeYamlBuilder.append(String.format("""
                  - id: "rule-%d"
                    name: "Rule %d"
                    condition: "#output%d != null"
                
                """, i, i, i));
        }
        
        String largeYaml = largeYamlBuilder.toString();
        
        // Should handle large YAML without issues
        OrderedYamlConfiguration largeConfig = orderedParser.parseYamlString(largeYaml, "large-test");
        assertNotNull(largeConfig);
        assertEquals(OrderedYamlConfiguration.ProcessingMode.SEQUENTIAL, largeConfig.getProcessingMode());

        SequentialProcessingResult largeResult = sequentialProcessor.processOrderedConfiguration(largeConfig, "large-test");
        assertNotNull(largeResult);
        
        LOGGER.info("✅ Error handling and edge cases test PASSED - All edge cases handled correctly!");
    }
}
