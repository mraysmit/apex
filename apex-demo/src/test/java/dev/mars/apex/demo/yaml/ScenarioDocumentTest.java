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
 * Test class for validating scenario document type functionality.
 * 
 * This test validates:
 * - Document structure and metadata validation for scenario type
 * - Scenario section processing with data-types and rule-configurations
 * - Scenario-specific validation requirements
 * - End-to-end processing scenario configuration
 * 
 * Based on APEX_YAML_REFERENCE.md specifications for scenario document type.
 */
class ScenarioDocumentTest extends DemoTestBase {

    private static final Logger logger = LoggerFactory.getLogger(ScenarioDocumentTest.class);
    private final ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());

    /**
     * Test basic scenario document structure with data types and rule configurations.
     * Validates that a scenario document with standard structure loads and processes correctly.
     */
    @Test
    void testScenarioDocumentWithStandardStructure() {
        logger.info("=== Testing Scenario Document with Standard Structure ===");

        try {
            // Load YAML document using ObjectMapper for raw access
            InputStream inputStream = getClass().getClassLoader().getResourceAsStream("yaml/scenario-standard-test.yaml");
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
            assertEquals("scenario", metadata.get("type"), "Document type should be scenario");

            // Validate required fields for scenario document
            assertNotNull(metadata.get("id"), "scenario document requires id field");
            assertNotNull(metadata.get("business-domain"), "scenario document requires business-domain field");
            assertNotNull(metadata.get("owner"), "scenario document requires owner field");

            // Validate scenario section exists
            @SuppressWarnings("unchecked")
            Map<String, Object> scenario = (Map<String, Object>) document.get("scenario");
            assertNotNull(scenario, "Scenario section should not be null");

            // Validate scenario structure
            assertTrue(scenario.containsKey("scenario-id"), "Scenario should have scenario-id");
            assertTrue(scenario.containsKey("data-types"), "Scenario should have data-types");
            assertTrue(scenario.containsKey("rule-configurations"), "Scenario should have rule-configurations");

            // Validate data-types section
            @SuppressWarnings("unchecked")
            List<String> dataTypes = (List<String>) scenario.get("data-types");
            assertNotNull(dataTypes, "Data types should not be null");
            assertFalse(dataTypes.isEmpty(), "Data types should not be empty");

            // Validate rule-configurations section
            @SuppressWarnings("unchecked")
            List<String> ruleConfigurations = (List<String>) scenario.get("rule-configurations");
            assertNotNull(ruleConfigurations, "Rule configurations should not be null");
            assertFalse(ruleConfigurations.isEmpty(), "Rule configurations should not be empty");

            logger.info("Scenario ID: " + scenario.get("scenario-id"));
            logger.info("Data types: " + dataTypes.size() + " types configured");
            logger.info("Rule configurations: " + ruleConfigurations.size() + " configurations referenced");
            logger.info("✅ Scenario document with standard structure test completed successfully");

        } catch (Exception e) {
            logger.error("❌ Scenario document standard structure test failed", e);
            fail("Scenario document standard structure test failed: " + e.getMessage());
        }
    }

    /**
     * Test scenario document with processing pipeline configuration.
     * Validates that a scenario document with processing pipeline loads correctly.
     */
    @Test
    void testScenarioDocumentWithProcessingPipeline() {
        logger.info("=== Testing Scenario Document with Processing Pipeline ===");

        try {
            // Load YAML document using ObjectMapper for raw access
            InputStream inputStream = getClass().getClassLoader().getResourceAsStream("yaml/scenario-pipeline-test.yaml");
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
            assertEquals("scenario", metadata.get("type"), "Document type should be scenario");

            // Validate scenario section exists
            @SuppressWarnings("unchecked")
            Map<String, Object> scenario = (Map<String, Object>) document.get("scenario");
            assertNotNull(scenario, "Scenario section should not be null");

            // Validate processing pipeline structure
            assertTrue(scenario.containsKey("processing-pipeline"), "Scenario should have processing-pipeline");

            @SuppressWarnings("unchecked")
            Map<String, Object> pipeline = (Map<String, Object>) scenario.get("processing-pipeline");
            assertNotNull(pipeline, "Processing pipeline should not be null");

            // Validate pipeline components
            assertTrue(pipeline.containsKey("validation-config"), "Pipeline should have validation-config");
            assertTrue(pipeline.containsKey("enrichment-config"), "Pipeline should have enrichment-config");

            logger.info("Processing pipeline configured with " + pipeline.size() + " components");
            logger.info("✅ Scenario document with processing pipeline test completed successfully");

        } catch (Exception e) {
            logger.error("❌ Scenario document processing pipeline test failed", e);
            fail("Scenario document processing pipeline test failed: " + e.getMessage());
        }
    }

    /**
     * Test scenario document with routing rules configuration.
     * Validates that a scenario document with conditional routing rules loads correctly.
     */
    @Test
    void testScenarioDocumentWithRoutingRules() {
        logger.info("=== Testing Scenario Document with Routing Rules ===");

        try {
            // Load YAML document using ObjectMapper for raw access
            InputStream inputStream = getClass().getClassLoader().getResourceAsStream("yaml/scenario-routing-test.yaml");
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
            assertEquals("scenario", metadata.get("type"), "Document type should be scenario");

            // Validate scenario section exists
            @SuppressWarnings("unchecked")
            Map<String, Object> scenario = (Map<String, Object>) document.get("scenario");
            assertNotNull(scenario, "Scenario section should not be null");

            // Validate routing rules structure
            assertTrue(scenario.containsKey("routing-rules"), "Scenario should have routing-rules");

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> routingRules = (List<Map<String, Object>>) scenario.get("routing-rules");
            assertNotNull(routingRules, "Routing rules should not be null");
            assertFalse(routingRules.isEmpty(), "Routing rules should not be empty");

            // Validate routing rule structure
            Map<String, Object> firstRule = routingRules.get(0);
            assertTrue(firstRule.containsKey("condition"), "Routing rule should have condition");
            assertTrue(firstRule.containsKey("config-override") || firstRule.containsKey("enrichment-override"),
                      "Routing rule should have override configuration");

            logger.info("Routing rules: " + routingRules.size() + " rules configured");
            logger.info("✅ Scenario document with routing rules test completed successfully");

        } catch (Exception e) {
            logger.error("❌ Scenario document routing rules test failed", e);
            fail("Scenario document routing rules test failed: " + e.getMessage());
        }
    }

    /**
     * Test scenario document metadata validation.
     * Validates that all required and optional metadata fields are properly handled for scenario documents.
     */
    @Test
    void testScenarioDocumentMetadataValidation() {
        logger.info("=== Testing Scenario Document Metadata Validation ===");

        try {
            // Load YAML document using ObjectMapper for raw access
            InputStream inputStream = getClass().getClassLoader().getResourceAsStream("yaml/scenario-metadata-test.yaml");
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
            assertEquals("scenario", metadata.get("type"), "Type must be scenario");

            // Validate scenario document specific required fields
            assertNotNull(metadata.get("id"), "scenario document requires id field");
            assertNotNull(metadata.get("business-domain"), "scenario document requires business-domain field");
            assertNotNull(metadata.get("owner"), "scenario document requires owner field");

            // Validate that scenario section is present (scenario documents must have scenario section)
            @SuppressWarnings("unchecked")
            Map<String, Object> scenario = (Map<String, Object>) document.get("scenario");
            assertNotNull(scenario, "Scenario section should be present");

            // Validate that scenario documents should not have rules or enrichments sections directly
            assertNull(document.get("rules"), "Rules section should not be present in scenario documents");
            assertNull(document.get("enrichments"), "Enrichments section should not be present in scenario documents");

            logger.info("✓ All required metadata fields validated successfully");
            logger.info("✓ Document type: " + metadata.get("type"));
            logger.info("✓ Document ID: " + metadata.get("id"));
            logger.info("✓ Business domain: " + metadata.get("business-domain"));
            logger.info("✓ Business owner: " + metadata.get("owner"));
            logger.info("✓ Scenario section validated, rules and enrichments sections correctly absent");

            logger.info("✅ Scenario document metadata validation test completed successfully");

        } catch (Exception e) {
            logger.error("❌ Scenario document metadata validation test failed", e);
            fail("Scenario document metadata validation test failed: " + e.getMessage());
        }
    }
}
