package dev.mars.apex.demo.yaml;

import dev.mars.apex.core.config.yaml.YamlRuleConfiguration;
import dev.mars.apex.demo.DemoTestBase;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for validating enrichment document type functionality.
 * 
 * This test validates:
 * - Document structure and metadata validation for enrichment type
 * - Enrichments section processing (only valid section for enrichment documents)
 * - Different enrichment types (lookup, calculation, field)
 * - Document type-specific validation requirements
 * 
 * Based on APEX_YAML_REFERENCE.md specifications for enrichment document type.
 */
class EnrichmentDocumentTest extends DemoTestBase {

    private static final Logger logger = LoggerFactory.getLogger(EnrichmentDocumentTest.class);

    /**
     * Test basic enrichment document structure with lookup enrichments.
     * Validates that an enrichment document with lookup enrichments loads and processes correctly.
     */
    @Test
    void testEnrichmentDocumentWithLookupEnrichments() {
        logger.info("=== Testing Enrichment Document with Lookup Enrichments ===");
        
        try {
            // Load YAML configuration
            YamlRuleConfiguration config = yamlLoader.loadFromClasspath("yaml/enrichment-lookup-test.yaml");
            assertNotNull(config, "Configuration should not be null");
            logger.info("✓ Configuration loaded successfully: " + config.getMetadata().getName());
            
            // Validate document type
            assertEquals("enrichment", config.getMetadata().getType(), "Document type should be enrichment");
            
            // Validate required fields for enrichment document
            assertNotNull(config.getMetadata().getId(), "enrichment document requires id field");
            assertNotNull(config.getMetadata().getAuthor(), "enrichment document requires author field");
            
            // Validate enrichments section exists (only valid section for enrichment documents)
            assertNotNull(config.getEnrichments(), "Enrichments section should not be null");
            assertFalse(config.getEnrichments().isEmpty(), "Enrichments section should not be empty");
            
            // Create test data for lookup enrichment processing
            Map<String, Object> inputData = new HashMap<>();
            inputData.put("currencyCode", "USD");
            inputData.put("instrumentId", "US912828XG93");
            
            logger.info("Input data: " + inputData);
            
            // Process using real APEX services
            Object result = enrichmentService.enrichObject(config, inputData);
            assertNotNull(result, "Enrichment result should not be null");
            
            @SuppressWarnings("unchecked")
            Map<String, Object> enrichedData = (Map<String, Object>) result;
            
            // Validate enrichment results
            assertTrue(enrichedData.containsKey("currencyName"), "Should contain currency name enrichment");
            assertTrue(enrichedData.containsKey("instrumentType"), "Should contain instrument type enrichment");
            
            logger.info("Enriched data: " + enrichedData);
            logger.info("✅ Enrichment document with lookup enrichments test completed successfully");
            
        } catch (Exception e) {
            logger.error("❌ Enrichment document lookup test failed", e);
            fail("Enrichment document lookup test failed: " + e.getMessage());
        }
    }

    /**
     * Test enrichment document structure with calculation enrichments.
     * Validates that an enrichment document with calculation enrichments processes correctly.
     */
    @Test
    void testEnrichmentDocumentWithCalculationEnrichments() {
        logger.info("=== Testing Enrichment Document with Calculation Enrichments ===");
        
        try {
            // Load YAML configuration
            YamlRuleConfiguration config = yamlLoader.loadFromClasspath("yaml/enrichment-calculation-test.yaml");
            assertNotNull(config, "Configuration should not be null");
            logger.info("✓ Configuration loaded successfully: " + config.getMetadata().getName());
            
            // Validate document type
            assertEquals("enrichment", config.getMetadata().getType(), "Document type should be enrichment");
            
            // Create test data for calculation enrichment processing
            Map<String, Object> inputData = new HashMap<>();
            inputData.put("principal", 100000.0);
            inputData.put("interestRate", 0.05);
            inputData.put("years", 10);
            inputData.put("quantity", 1000);
            inputData.put("price", 98.75);
            
            logger.info("Input data: " + inputData);
            
            // Process using real APEX services
            Object result = enrichmentService.enrichObject(config, inputData);
            assertNotNull(result, "Calculation enrichment result should not be null");
            
            @SuppressWarnings("unchecked")
            Map<String, Object> enrichedData = (Map<String, Object>) result;
            
            // Validate calculation results
            assertTrue(enrichedData.containsKey("totalInterest"), "Should contain total interest calculation");
            assertTrue(enrichedData.containsKey("notionalValue"), "Should contain notional value calculation");
            
            // Validate calculation accuracy
            Double totalInterest = (Double) enrichedData.get("totalInterest");
            Double notionalValue = (Double) enrichedData.get("notionalValue");
            assertEquals(50000.0, totalInterest, 0.01, "Total interest should be calculated correctly");
            assertEquals(98750.0, notionalValue, 0.01, "Notional value should be calculated correctly");
            
            logger.info("Enriched data: " + enrichedData);
            logger.info("✅ Enrichment document with calculation enrichments test completed successfully");
            
        } catch (Exception e) {
            logger.error("❌ Enrichment document calculation test failed", e);
            fail("Enrichment document calculation test failed: " + e.getMessage());
        }
    }

    /**
     * Test enrichment document structure with mixed enrichment types.
     * Validates that an enrichment document with multiple enrichment types processes correctly.
     */
    @Test
    void testEnrichmentDocumentWithMixedEnrichments() {
        logger.info("=== Testing Enrichment Document with Mixed Enrichment Types ===");
        
        try {
            // Load YAML configuration
            YamlRuleConfiguration config = yamlLoader.loadFromClasspath("yaml/enrichment-mixed-test.yaml");
            assertNotNull(config, "Configuration should not be null");
            logger.info("✓ Configuration loaded successfully: " + config.getMetadata().getName());
            
            // Validate document type
            assertEquals("enrichment", config.getMetadata().getType(), "Document type should be enrichment");
            
            // Validate enrichments section
            assertNotNull(config.getEnrichments(), "Enrichments section should not be null");
            assertTrue(config.getEnrichments().size() >= 2, "Should have multiple enrichments");
            
            // Create test data for mixed enrichment processing
            Map<String, Object> inputData = new HashMap<>();
            inputData.put("counterpartyName", "Goldman Sachs");
            inputData.put("tradeAmount", 1000000.0);
            inputData.put("feeRate", 0.001);
            
            logger.info("Input data: " + inputData);
            
            // Process using real APEX services
            Object result = enrichmentService.enrichObject(config, inputData);
            assertNotNull(result, "Mixed enrichment result should not be null");
            
            @SuppressWarnings("unchecked")
            Map<String, Object> enrichedData = (Map<String, Object>) result;
            
            // Validate mixed enrichment results
            assertTrue(enrichedData.containsKey("counterpartyLei"), "Should contain lookup enrichment result");
            assertTrue(enrichedData.containsKey("calculatedFee"), "Should contain calculation enrichment result");
            
            logger.info("Mixed enrichment result: " + enrichedData);
            logger.info("✅ Enrichment document with mixed enrichments test completed successfully");
            
        } catch (Exception e) {
            logger.error("❌ Enrichment document mixed test failed", e);
            fail("Enrichment document mixed test failed: " + e.getMessage());
        }
    }

    /**
     * Test enrichment document metadata validation.
     * Validates that all required and optional metadata fields are properly handled for enrichment documents.
     */
    @Test
    void testEnrichmentDocumentMetadataValidation() {
        logger.info("=== Testing Enrichment Document Metadata Validation ===");
        
        try {
            // Load YAML configuration
            YamlRuleConfiguration config = yamlLoader.loadFromClasspath("yaml/enrichment-metadata-test.yaml");
            assertNotNull(config, "Configuration should not be null");
            
            // Validate required metadata fields
            assertNotNull(config.getMetadata(), "Metadata should not be null");
            assertNotNull(config.getMetadata().getName(), "Name is required");
            assertNotNull(config.getMetadata().getVersion(), "Version is required");
            assertNotNull(config.getMetadata().getDescription(), "Description is required");
            assertEquals("enrichment", config.getMetadata().getType(), "Type must be enrichment");
            
            // Validate enrichment document specific required fields
            assertNotNull(config.getMetadata().getId(), "enrichment document requires id field");
            assertNotNull(config.getMetadata().getAuthor(), "enrichment document requires author field");
            
            // Validate that only enrichments section is present (enrichment documents should not have rules)
            assertNotNull(config.getEnrichments(), "Enrichments section should be present");
            assertNull(config.getRules(), "Rules section should not be present in enrichment documents");
            
            logger.info("✓ All required metadata fields validated successfully");
            logger.info("✓ Document type: " + config.getMetadata().getType());
            logger.info("✓ Document ID: " + config.getMetadata().getId());
            logger.info("✓ Document author: " + config.getMetadata().getAuthor());
            logger.info("✓ Enrichments section validated, rules section correctly absent");
            
            logger.info("✅ Enrichment document metadata validation test completed successfully");
            
        } catch (Exception e) {
            logger.error("❌ Enrichment document metadata validation test failed", e);
            fail("Enrichment document metadata validation test failed: " + e.getMessage());
        }
    }
}
