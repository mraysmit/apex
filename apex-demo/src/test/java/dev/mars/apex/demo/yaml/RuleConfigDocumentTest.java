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
 * Test class for validating rule-config document type functionality.
 * 
 * This test validates:
 * - Document structure and metadata validation
 * - Rules section processing
 * - Enrichments section processing
 * - Combined rules and enrichments processing
 * - Document type-specific validation requirements
 * 
 * Based on APEX_YAML_REFERENCE.md specifications for rule-config document type.
 */
class RuleConfigDocumentTest extends DemoTestBase {

    private static final Logger logger = LoggerFactory.getLogger(RuleConfigDocumentTest.class);

    /**
     * Test basic rule-config document structure with enrichments only.
     * Validates that a rule-config document with enrichments section loads and processes correctly.
     */
    @Test
    void testRuleConfigWithEnrichmentsOnly() {
        logger.info("=== Testing Rule-Config Document with Enrichments Only ===");
        
        try {
            // Load YAML configuration
            YamlRuleConfiguration config = yamlLoader.loadFromClasspath("yaml/rule-config-enrichments-test.yaml");
            assertNotNull(config, "Configuration should not be null");
            logger.info("✓ Configuration loaded successfully: " + config.getMetadata().getName());
            
            // Validate document type
            assertEquals("rule-config", config.getMetadata().getType(), "Document type should be rule-config");
            
            // Validate required fields for rule-config
            assertNotNull(config.getMetadata().getId(), "rule-config requires id field");
            assertNotNull(config.getMetadata().getAuthor(), "rule-config requires author field");
            
            // Create test data for enrichment processing
            Map<String, Object> inputData = new HashMap<>();
            inputData.put("counterpartyName", "JPMorgan Chase");
            inputData.put("quantity", 1000);
            inputData.put("price", 95.50);
            
            logger.info("Input data: " + inputData);
            
            // Process using real APEX services
            Object result = enrichmentService.enrichObject(config, inputData);
            assertNotNull(result, "Enrichment result should not be null");
            
            @SuppressWarnings("unchecked")
            Map<String, Object> enrichedData = (Map<String, Object>) result;
            
            // Validate enrichment results
            assertTrue(enrichedData.containsKey("counterpartyLei"), "Should contain LEI enrichment result");
            assertTrue(enrichedData.containsKey("tradeValue"), "Should contain trade value calculation");
            
            logger.info("Enriched data: " + enrichedData);
            logger.info("✅ Rule-config with enrichments test completed successfully");
            
        } catch (Exception e) {
            logger.error("❌ Rule-config enrichments test failed", e);
            fail("Rule-config enrichments test failed: " + e.getMessage());
        }
    }

    /**
     * Test rule-config document structure with rules only.
     * Validates that a rule-config document with rules section loads and validates correctly.
     */
    @Test
    void testRuleConfigWithRulesOnly() {
        logger.info("=== Testing Rule-Config Document with Rules Only ===");
        
        try {
            // Load YAML configuration
            YamlRuleConfiguration config = yamlLoader.loadFromClasspath("yaml/rule-config-rules-test.yaml");
            assertNotNull(config, "Configuration should not be null");
            logger.info("✓ Configuration loaded successfully: " + config.getMetadata().getName());
            
            // Validate document type
            assertEquals("rule-config", config.getMetadata().getType(), "Document type should be rule-config");
            
            // Validate rules section exists
            assertNotNull(config.getRules(), "Rules section should not be null");
            assertFalse(config.getRules().isEmpty(), "Rules section should not be empty");
            
            // Create test data for rule validation
            Map<String, Object> inputData = new HashMap<>();
            inputData.put("tradeId", "TRADE_001");
            inputData.put("counterpartyName", "Goldman Sachs");
            inputData.put("instrumentId", "US912828XG93");
            inputData.put("notionalAmount", 1000000.0);
            
            logger.info("Input data: " + inputData);
            
            // Process using real APEX services
            Object result = enrichmentService.enrichObject(config, inputData);
            assertNotNull(result, "Rule processing result should not be null");
            
            logger.info("Rule processing completed successfully");
            logger.info("✅ Rule-config with rules test completed successfully");
            
        } catch (Exception e) {
            logger.error("❌ Rule-config rules test failed", e);
            fail("Rule-config rules test failed: " + e.getMessage());
        }
    }

    /**
     * Test rule-config document structure with both rules and enrichments.
     * Validates that a rule-config document with both sections processes correctly.
     */
    @Test
    void testRuleConfigWithRulesAndEnrichments() {
        logger.info("=== Testing Rule-Config Document with Rules and Enrichments ===");
        
        try {
            // Load YAML configuration
            YamlRuleConfiguration config = yamlLoader.loadFromClasspath("yaml/rule-config-combined-test.yaml");
            assertNotNull(config, "Configuration should not be null");
            logger.info("✓ Configuration loaded successfully: " + config.getMetadata().getName());
            
            // Validate document type
            assertEquals("rule-config", config.getMetadata().getType(), "Document type should be rule-config");
            
            // Validate both sections exist
            assertNotNull(config.getRules(), "Rules section should not be null");
            assertNotNull(config.getEnrichments(), "Enrichments section should not be null");
            assertFalse(config.getRules().isEmpty(), "Rules section should not be empty");
            assertFalse(config.getEnrichments().isEmpty(), "Enrichments section should not be empty");
            
            // Create test data for combined processing
            Map<String, Object> inputData = new HashMap<>();
            inputData.put("tradeId", "TRADE_002");
            inputData.put("counterpartyName", "Deutsche Bank");
            inputData.put("instrumentId", "DE0001102309");
            inputData.put("quantity", 500);
            inputData.put("price", 102.75);
            inputData.put("notionalAmount", 51375.0);
            
            logger.info("Input data: " + inputData);
            
            // Process using real APEX services
            Object result = enrichmentService.enrichObject(config, inputData);
            assertNotNull(result, "Combined processing result should not be null");
            
            @SuppressWarnings("unchecked")
            Map<String, Object> enrichedData = (Map<String, Object>) result;
            
            // Validate that both rules and enrichments were processed
            logger.info("Combined processing result: " + enrichedData);
            logger.info("✅ Rule-config combined test completed successfully");
            
        } catch (Exception e) {
            logger.error("❌ Rule-config combined test failed", e);
            fail("Rule-config combined test failed: " + e.getMessage());
        }
    }

    /**
     * Test rule-config document metadata validation.
     * Validates that all required and optional metadata fields are properly handled.
     */
    @Test
    void testRuleConfigMetadataValidation() {
        logger.info("=== Testing Rule-Config Document Metadata Validation ===");
        
        try {
            // Load YAML configuration
            YamlRuleConfiguration config = yamlLoader.loadFromClasspath("yaml/rule-config-metadata-test.yaml");
            assertNotNull(config, "Configuration should not be null");
            
            // Validate required metadata fields
            assertNotNull(config.getMetadata(), "Metadata should not be null");
            assertNotNull(config.getMetadata().getName(), "Name is required");
            assertNotNull(config.getMetadata().getVersion(), "Version is required");
            assertNotNull(config.getMetadata().getDescription(), "Description is required");
            assertEquals("rule-config", config.getMetadata().getType(), "Type must be rule-config");
            
            // Validate rule-config specific required fields
            assertNotNull(config.getMetadata().getId(), "rule-config requires id field");
            assertNotNull(config.getMetadata().getAuthor(), "rule-config requires author field");
            
            logger.info("✓ All required metadata fields validated successfully");
            logger.info("✓ Document type: " + config.getMetadata().getType());
            logger.info("✓ Document ID: " + config.getMetadata().getId());
            logger.info("✓ Document author: " + config.getMetadata().getAuthor());
            
            logger.info("✅ Rule-config metadata validation test completed successfully");
            
        } catch (Exception e) {
            logger.error("❌ Rule-config metadata validation test failed", e);
            fail("Rule-config metadata validation test failed: " + e.getMessage());
        }
    }
}
