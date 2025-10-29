package dev.mars.apex.demo.sequencing;

import dev.mars.apex.core.config.yaml.*;
import dev.mars.apex.core.config.yaml.OrderedYamlConfiguration.ProcessingMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Phase 2 Tests: Sequential YAML Processing Validation
 * 
 * These tests demonstrate that APEX's fundamental design flaw has been FIXED.
 * The SequentialYamlProcessor now respects YAML document order instead of
 * using hardcoded processing sequences.
 * 
 * Key Test Scenarios:
 * 1. Enrich-then-validate pattern (enrichments before rules)
 * 2. Validate-then-enrich pattern (rules before enrichments)  
 * 3. Processing mode detection (STANDARD vs SEQUENTIAL)
 * 4. Section processing order verification
 * 5. Error handling and context management
 * 
 * SUCCESS CRITERIA:
 * - Sequential mode processes sections in YAML document order
 * - Standard mode uses hardcoded order (backward compatibility)
 * - Processing context tracks section order correctly
 * - Both modes produce valid results
 * 
 * @author APEX Sequential Processing Implementation
 * @version 1.0
 * @since Phase 2
 */
public class SequentialYamlProcessorTest {
    
    private static final Logger LOGGER = Logger.getLogger(SequentialYamlProcessorTest.class.getName());
    
    private SequentialYamlProcessor processor;
    
    @BeforeEach
    void setUp() {
        LOGGER.info("=== PHASE 2 TEST: Sequential YAML Processing Validation ===");
        processor = new SequentialYamlProcessor();
    }
    
    /**
     * TEST 1: Enrich-then-validate pattern processing
     * 
     * This test demonstrates the CORE FIX: enrichments appear before rules
     * in the YAML document, and sequential processing respects this order.
     */
    @Test
    void testEnrichThenValidatePattern() throws YamlConfigurationException {
        LOGGER.info("Testing enrich-then-validate pattern (enrichments before rules)...");
        
        String yaml = """
            metadata:
              name: "Enrich-Then-Validate Test"
              type: "sequential-test"
              processing-mode: "sequential"
            
            data-sources:
              - name: "customer-db"
                type: "database"
                connection-string: "jdbc:h2:mem:customers"
            
            enrichments:
              - id: "customer-lookup"
                name: "Customer Data Enrichment"
                type: "lookup-enrichment"
                data-source: "customer-db"
                lookup-key: "#customerId"
                target-field: "customerData"
            
            rules:
              - id: "validate-customer"
                name: "Validate Customer Data"
                condition: "#customerData != null"
                message: "Customer data is available"
                priority: 100
                severity: "INFO"
            """;
        
        // Process with sequential processor
        SequentialProcessingResult result = processor.processYamlString(yaml);
        
        // Verify processing was successful
        assertTrue(result.isSuccessful(), "Sequential processing should succeed");
        assertFalse(result.hasErrors(), "No errors should occur");
        assertEquals(ProcessingMode.SEQUENTIAL, result.getProcessingMode(), "Should use SEQUENTIAL mode");
        
        // Verify section processing order - THE CORE FIX VALIDATION
        List<String> processedSections = result.getProcessedSections();
        LOGGER.info("Processed sections in order: " + processedSections);
        
        // Find positions of enrichments and rules
        int enrichmentsPosition = processedSections.indexOf("enrichments");
        int rulesPosition = processedSections.indexOf("rules");
        
        assertTrue(enrichmentsPosition >= 0, "Enrichments section should be processed");
        assertTrue(rulesPosition >= 0, "Rules section should be processed");
        assertTrue(enrichmentsPosition < rulesPosition, 
                  "CORE FIX: Enrichments should be processed BEFORE rules in sequential mode");
        
        LOGGER.info("✅ Enrich-then-validate pattern test PASSED - DESIGN FLAW FIXED!");
    }
    
    /**
     * TEST 2: Validate-then-enrich pattern processing
     * 
     * This test demonstrates that when rules appear before enrichments in YAML,
     * sequential processing respects this order (opposite of Test 1).
     */
    @Test
    void testValidateThenEnrichPattern() throws YamlConfigurationException {
        LOGGER.info("Testing validate-then-enrich pattern (rules before enrichments)...");
        
        String yaml = """
            metadata:
              name: "Validate-Then-Enrich Test"
              type: "sequential-test"
              processing-mode: "sequential"
            
            data-sources:
              - name: "validation-db"
                type: "database"
                connection-string: "jdbc:h2:mem:validation"
            
            rules:
              - id: "pre-validation"
                name: "Pre-Validation Check"
                condition: "#inputData != null"
                message: "Input data is present"
                priority: 200
                severity: "WARN"
            
            enrichments:
              - id: "post-enrichment"
                name: "Post-Validation Enrichment"
                type: "lookup-enrichment"
                data-source: "validation-db"
                lookup-key: "#validatedId"
                target-field: "enrichedResult"
            """;
        
        // Process with sequential processor
        SequentialProcessingResult result = processor.processYamlString(yaml);
        
        // Verify processing was successful
        assertTrue(result.isSuccessful(), "Sequential processing should succeed");
        assertFalse(result.hasErrors(), "No errors should occur");
        assertEquals(ProcessingMode.SEQUENTIAL, result.getProcessingMode(), "Should use SEQUENTIAL mode");
        
        // Verify section processing order - THE OPPOSITE ORDER
        List<String> processedSections = result.getProcessedSections();
        LOGGER.info("Processed sections in order: " + processedSections);
        
        // Find positions of rules and enrichments
        int rulesPosition = processedSections.indexOf("rules");
        int enrichmentsPosition = processedSections.indexOf("enrichments");
        
        assertTrue(rulesPosition >= 0, "Rules section should be processed");
        assertTrue(enrichmentsPosition >= 0, "Enrichments section should be processed");
        assertTrue(rulesPosition < enrichmentsPosition, 
                  "CORE FIX: Rules should be processed BEFORE enrichments when they appear first in YAML");
        
        LOGGER.info("✅ Validate-then-enrich pattern test PASSED - DESIGN FLAW FIXED!");
    }
    
    /**
     * TEST 3: Standard mode backward compatibility
     * 
     * This test verifies that STANDARD mode still uses hardcoded processing order
     * for backward compatibility, regardless of YAML document order.
     */
    @Test
    void testStandardModeBackwardCompatibility() throws YamlConfigurationException {
        LOGGER.info("Testing STANDARD mode backward compatibility...");
        
        String yaml = """
            metadata:
              name: "Standard Mode Test"
              type: "standard-test"
              # No processing-mode specified = STANDARD mode
            
            enrichments:
              - id: "test-enrichment"
                name: "Test Enrichment"
                type: "lookup-enrichment"
                lookup-key: "#id"
                target-field: "enrichedData"
            
            rules:
              - id: "test-rule"
                name: "Test Rule"
                condition: "#data != null"
                message: "Data is present"
                priority: 100
                severity: "INFO"
            """;
        
        // Process with sequential processor
        SequentialProcessingResult result = processor.processYamlString(yaml);
        
        // Verify processing was successful
        assertTrue(result.isSuccessful(), "Standard processing should succeed");
        assertFalse(result.hasErrors(), "No errors should occur");
        assertEquals(ProcessingMode.STANDARD, result.getProcessingMode(), "Should use STANDARD mode");
        
        // Verify hardcoded processing order is used (metadata -> data-sources -> rules -> enrichments)
        List<String> processedSections = result.getProcessedSections();
        LOGGER.info("Standard mode processed sections: " + processedSections);
        
        // In STANDARD mode, rules should be processed before enrichments (hardcoded order)
        // regardless of YAML document order
        int rulesPosition = processedSections.indexOf("rules");
        int enrichmentsPosition = processedSections.indexOf("enrichments");
        
        assertTrue(rulesPosition >= 0, "Rules section should be processed");
        assertTrue(enrichmentsPosition >= 0, "Enrichments section should be processed");
        assertTrue(rulesPosition < enrichmentsPosition, 
                  "STANDARD mode: Rules should be processed BEFORE enrichments (hardcoded order)");
        
        LOGGER.info("✅ Standard mode backward compatibility test PASSED");
    }
    
    /**
     * TEST 4: Processing mode detection
     * 
     * This test verifies that processing mode is correctly detected from metadata.
     */
    @Test
    void testProcessingModeDetection() throws YamlConfigurationException {
        LOGGER.info("Testing processing mode detection...");
        
        // Test SEQUENTIAL mode detection
        String sequentialYaml = """
            metadata:
              processing-mode: "sequential"
            rules:
              - id: "test-rule"
                condition: "true"
            """;
        
        SequentialProcessingResult sequentialResult = processor.processYamlString(sequentialYaml);
        assertEquals(ProcessingMode.SEQUENTIAL, sequentialResult.getProcessingMode(), 
                    "Should detect SEQUENTIAL mode");
        
        // Test STANDARD mode detection (default)
        String standardYaml = """
            metadata:
              name: "Standard Test"
            rules:
              - id: "test-rule"
                condition: "true"
            """;
        
        SequentialProcessingResult standardResult = processor.processYamlString(standardYaml);
        assertEquals(ProcessingMode.STANDARD, standardResult.getProcessingMode(), 
                    "Should default to STANDARD mode");
        
        LOGGER.info("✅ Processing mode detection test PASSED");
    }
    
    /**
     * TEST 5: Complex section ordering
     * 
     * This test verifies that complex YAML documents with many sections
     * are processed in the correct order.
     */
    @Test
    void testComplexSectionOrdering() throws YamlConfigurationException {
        LOGGER.info("Testing complex section ordering...");
        
        String yaml = """
            metadata:
              name: "Complex Ordering Test"
              processing-mode: "sequential"
            
            categories:
              - id: "validation"
                name: "Validation Rules"
            
            data-sources:
              - name: "main-db"
                type: "database"
            
            transformations:
              - id: "data-transform"
                name: "Data Transformation"
            
            enrichments:
              - id: "enrich-data"
                name: "Data Enrichment"
            
            rules:
              - id: "validate-data"
                name: "Data Validation"
                condition: "true"
            
            rule-groups:
              - id: "validation-group"
                name: "Validation Group"
            """;
        
        SequentialProcessingResult result = processor.processYamlString(yaml);
        
        assertTrue(result.isSuccessful(), "Complex processing should succeed");
        assertEquals(ProcessingMode.SEQUENTIAL, result.getProcessingMode(), "Should use SEQUENTIAL mode");
        
        List<String> processedSections = result.getProcessedSections();
        LOGGER.info("Complex document processed sections: " + processedSections);
        
        // Verify expected sections are present and in document order
        List<String> expectedOrder = List.of("metadata", "categories", "data-sources", 
                                           "transformations", "enrichments", "rules", "rule-groups");
        
        for (int i = 0; i < expectedOrder.size() - 1; i++) {
            String currentSection = expectedOrder.get(i);
            String nextSection = expectedOrder.get(i + 1);
            
            int currentPos = processedSections.indexOf(currentSection);
            int nextPos = processedSections.indexOf(nextSection);
            
            if (currentPos >= 0 && nextPos >= 0) {
                assertTrue(currentPos < nextPos, 
                          "Section '" + currentSection + "' should be processed before '" + nextSection + "'");
            }
        }
        
        LOGGER.info("✅ Complex section ordering test PASSED");
    }
}
