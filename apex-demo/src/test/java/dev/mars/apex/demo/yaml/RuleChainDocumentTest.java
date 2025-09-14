package dev.mars.apex.demo.yaml;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import dev.mars.apex.demo.DemoTestBase;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for validating rule-chain document type functionality.
 * 
 * This test validates:
 * - Document structure and metadata validation for rule-chain type
 * - Rule-chains section processing with sequential execution definitions
 * - Rule-chain-specific validation requirements
 * - Different rule chain patterns (sequential, conditional, workflow)
 * 
 * Based on APEX_YAML_REFERENCE.md specifications for rule-chain document type.
 */
class RuleChainDocumentTest extends DemoTestBase {

    private static final Logger logger = LoggerFactory.getLogger(RuleChainDocumentTest.class);
    private final ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());

    /**
     * Test basic rule-chain document structure with sequential execution.
     * Validates that a rule-chain document with sequential pattern loads and processes correctly.
     */
    @Test
    void testRuleChainDocumentWithSequentialExecution() {
        logger.info("=== Testing Rule-Chain Document with Sequential Execution ===");
        
        try {
            // Load YAML document using ObjectMapper for raw access
            InputStream inputStream = getClass().getClassLoader().getResourceAsStream("yaml/rule-chain-sequential-test.yaml");
            assertNotNull(inputStream, "YAML file should be found");
            
            @SuppressWarnings("unchecked")
            Map<String, Object> document = yamlMapper.readValue(inputStream, Map.class);
            assertNotNull(document, "Document should not be null");
            
            // Validate metadata section
            @SuppressWarnings("unchecked")
            Map<String, Object> metadata = (Map<String, Object>) document.get("metadata");
            assertNotNull(metadata, "Metadata section should not be null");
            logger.info("✓ Document loaded successfully: " + metadata.get("name"));
            
            // Validate document type
            assertEquals("rule-chain", metadata.get("type"), "Document type should be rule-chain");
            
            // Validate required fields for rule-chain document
            assertNotNull(metadata.get("id"), "rule-chain document requires id field");
            assertNotNull(metadata.get("author"), "rule-chain document requires author field");
            
            // Validate rule-chains section exists
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> ruleChains = (List<Map<String, Object>>) document.get("rule-chains");
            assertNotNull(ruleChains, "Rule-chains section should not be null");
            assertFalse(ruleChains.isEmpty(), "Rule-chains should not be empty");
            
            // Validate rule chain structure
            Map<String, Object> firstChain = ruleChains.get(0);
            assertTrue(firstChain.containsKey("id"), "Rule chain should have id");
            assertTrue(firstChain.containsKey("pattern"), "Rule chain should have pattern");
            assertTrue(firstChain.containsKey("configuration"), "Rule chain should have configuration");
            
            // Validate sequential pattern
            assertEquals("sequential-dependency", firstChain.get("pattern"), "Pattern should be sequential-dependency");
            
            // Validate configuration structure
            @SuppressWarnings("unchecked")
            Map<String, Object> configuration = (Map<String, Object>) firstChain.get("configuration");
            assertTrue(configuration.containsKey("stages"), "Configuration should have stages");
            
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> stages = (List<Map<String, Object>>) configuration.get("stages");
            assertNotNull(stages, "Stages should not be null");
            assertFalse(stages.isEmpty(), "Stages should not be empty");
            
            logger.info("Rule chain contains " + stages.size() + " sequential stages");
            logger.info("First chain ID: " + firstChain.get("id"));
            logger.info("✅ Rule-chain document with sequential execution test completed successfully");
            
        } catch (Exception e) {
            logger.error("❌ Rule-chain document sequential execution test failed", e);
            fail("Rule-chain document sequential execution test failed: " + e.getMessage());
        }
    }

    /**
     * Test rule-chain document with conditional chaining pattern.
     * Validates that a rule-chain document with conditional logic loads correctly.
     */
    @Test
    void testRuleChainDocumentWithConditionalChaining() {
        logger.info("=== Testing Rule-Chain Document with Conditional Chaining ===");
        
        try {
            // Load YAML document using ObjectMapper for raw access
            InputStream inputStream = getClass().getClassLoader().getResourceAsStream("yaml/rule-chain-conditional-test.yaml");
            assertNotNull(inputStream, "YAML file should be found");
            
            @SuppressWarnings("unchecked")
            Map<String, Object> document = yamlMapper.readValue(inputStream, Map.class);
            assertNotNull(document, "Document should not be null");
            
            // Validate metadata section
            @SuppressWarnings("unchecked")
            Map<String, Object> metadata = (Map<String, Object>) document.get("metadata");
            assertNotNull(metadata, "Metadata section should not be null");
            logger.info("✓ Document loaded successfully: " + metadata.get("name"));
            
            // Validate document type
            assertEquals("rule-chain", metadata.get("type"), "Document type should be rule-chain");
            
            // Validate rule-chains section exists
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> ruleChains = (List<Map<String, Object>>) document.get("rule-chains");
            assertNotNull(ruleChains, "Rule-chains section should not be null");
            
            // Validate conditional pattern
            Map<String, Object> firstChain = ruleChains.get(0);
            assertEquals("conditional-chaining", firstChain.get("pattern"), "Pattern should be conditional-chaining");
            
            // Validate configuration structure for conditional chaining
            @SuppressWarnings("unchecked")
            Map<String, Object> configuration = (Map<String, Object>) firstChain.get("configuration");
            assertTrue(configuration.containsKey("rules"), "Configuration should have rules");
            
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> rules = (List<Map<String, Object>>) configuration.get("rules");
            assertNotNull(rules, "Rules should not be null");
            assertFalse(rules.isEmpty(), "Rules should not be empty");
            
            // Validate rule structure
            Map<String, Object> firstRule = rules.get(0);
            assertTrue(firstRule.containsKey("id"), "Rule should have id");
            assertTrue(firstRule.containsKey("condition"), "Rule should have condition");
            assertTrue(firstRule.containsKey("next-rule"), "Rule should have next-rule");
            
            logger.info("Conditional chain contains " + rules.size() + " conditional rules");
            logger.info("✅ Rule-chain document with conditional chaining test completed successfully");
            
        } catch (Exception e) {
            logger.error("❌ Rule-chain document conditional chaining test failed", e);
            fail("Rule-chain document conditional chaining test failed: " + e.getMessage());
        }
    }

    /**
     * Test rule-chain document with complex workflow pattern.
     * Validates that a rule-chain document with complex workflow configuration loads correctly.
     */
    @Test
    void testRuleChainDocumentWithComplexWorkflow() {
        logger.info("=== Testing Rule-Chain Document with Complex Workflow ===");
        
        try {
            // Load YAML document using ObjectMapper for raw access
            InputStream inputStream = getClass().getClassLoader().getResourceAsStream("yaml/rule-chain-workflow-test.yaml");
            assertNotNull(inputStream, "YAML file should be found");
            
            @SuppressWarnings("unchecked")
            Map<String, Object> document = yamlMapper.readValue(inputStream, Map.class);
            assertNotNull(document, "Document should not be null");
            
            // Validate metadata section
            @SuppressWarnings("unchecked")
            Map<String, Object> metadata = (Map<String, Object>) document.get("metadata");
            assertNotNull(metadata, "Metadata section should not be null");
            logger.info("✓ Document loaded successfully: " + metadata.get("name"));
            
            // Validate document type
            assertEquals("rule-chain", metadata.get("type"), "Document type should be rule-chain");
            
            // Validate rule-chains section exists
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> ruleChains = (List<Map<String, Object>>) document.get("rule-chains");
            assertNotNull(ruleChains, "Rule-chains section should not be null");
            
            // Validate complex workflow pattern
            Map<String, Object> firstChain = ruleChains.get(0);
            assertEquals("complex-workflow", firstChain.get("pattern"), "Pattern should be complex-workflow");
            
            // Validate configuration structure for complex workflow
            @SuppressWarnings("unchecked")
            Map<String, Object> configuration = (Map<String, Object>) firstChain.get("configuration");
            assertTrue(configuration.containsKey("stages"), "Configuration should have stages");
            
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> stages = (List<Map<String, Object>>) configuration.get("stages");
            assertNotNull(stages, "Stages should not be null");
            
            // Validate stage dependencies
            boolean hasDependencies = false;
            for (Map<String, Object> stage : stages) {
                if (stage.containsKey("depends-on")) {
                    hasDependencies = true;
                    break;
                }
            }
            assertTrue(hasDependencies, "Complex workflow should have stage dependencies");
            
            logger.info("Complex workflow contains " + stages.size() + " workflow stages");
            logger.info("✅ Rule-chain document with complex workflow test completed successfully");
            
        } catch (Exception e) {
            logger.error("❌ Rule-chain document complex workflow test failed", e);
            fail("Rule-chain document complex workflow test failed: " + e.getMessage());
        }
    }

    /**
     * Test rule-chain document metadata validation.
     * Validates that all required and optional metadata fields are properly handled for rule-chain documents.
     */
    @Test
    void testRuleChainDocumentMetadataValidation() {
        logger.info("=== Testing Rule-Chain Document Metadata Validation ===");
        
        try {
            // Load YAML document using ObjectMapper for raw access
            InputStream inputStream = getClass().getClassLoader().getResourceAsStream("yaml/rule-chain-metadata-test.yaml");
            assertNotNull(inputStream, "YAML file should be found");
            
            @SuppressWarnings("unchecked")
            Map<String, Object> document = yamlMapper.readValue(inputStream, Map.class);
            assertNotNull(document, "Document should not be null");
            
            // Validate metadata section
            @SuppressWarnings("unchecked")
            Map<String, Object> metadata = (Map<String, Object>) document.get("metadata");
            assertNotNull(metadata, "Metadata should not be null");
            
            // Validate required metadata fields
            assertNotNull(metadata.get("name"), "Name is required");
            assertNotNull(metadata.get("version"), "Version is required");
            assertNotNull(metadata.get("description"), "Description is required");
            assertEquals("rule-chain", metadata.get("type"), "Type must be rule-chain");
            
            // Validate rule-chain document specific required fields
            assertNotNull(metadata.get("id"), "rule-chain document requires id field");
            assertNotNull(metadata.get("author"), "rule-chain document requires author field");
            
            // Validate that rule-chains section is present (rule-chain documents must have rule-chains section)
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> ruleChains = (List<Map<String, Object>>) document.get("rule-chains");
            assertNotNull(ruleChains, "Rule-chains section should be present");
            assertFalse(ruleChains.isEmpty(), "Rule-chains section should not be empty");
            
            // Validate that rule-chain documents should not have rules or enrichments sections directly
            assertNull(document.get("rules"), "Rules section should not be present in rule-chain documents");
            assertNull(document.get("enrichments"), "Enrichments section should not be present in rule-chain documents");
            assertNull(document.get("scenario"), "Scenario section should not be present in rule-chain documents");
            
            logger.info("✓ All required metadata fields validated successfully");
            logger.info("✓ Document type: " + metadata.get("type"));
            logger.info("✓ Document ID: " + metadata.get("id"));
            logger.info("✓ Document author: " + metadata.get("author"));
            logger.info("✓ Rule-chains section validated with " + ruleChains.size() + " chains");
            logger.info("✓ Rules, enrichments, and scenario sections correctly absent");
            
            logger.info("✅ Rule-chain document metadata validation test completed successfully");
            
        } catch (Exception e) {
            logger.error("❌ Rule-chain document metadata validation test failed", e);
            fail("Rule-chain document metadata validation test failed: " + e.getMessage());
        }
    }
}
