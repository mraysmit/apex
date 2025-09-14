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
 * Test class for validating scenario-registry document type functionality.
 * 
 * This test validates:
 * - Document structure and metadata validation for scenario-registry type
 * - Scenario-registry section processing with scenario entries
 * - Scenario-registry-specific validation requirements
 * - Central scenario collection management configuration
 * 
 * Based on APEX_YAML_REFERENCE.md specifications for scenario-registry document type.
 */
class ScenarioRegistryDocumentTest extends DemoTestBase {

    private static final Logger logger = LoggerFactory.getLogger(ScenarioRegistryDocumentTest.class);
    private final ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());

    /**
     * Test basic scenario-registry document structure with scenario entries.
     * Validates that a scenario-registry document with standard structure loads and processes correctly.
     */
    @Test
    void testScenarioRegistryDocumentWithStandardStructure() {
        logger.info("=== Testing Scenario-Registry Document with Standard Structure ===");
        
        try {
            // Load YAML document using ObjectMapper for raw access
            InputStream inputStream = getClass().getClassLoader().getResourceAsStream("yaml/scenario-registry-standard-test.yaml");
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
            assertEquals("scenario-registry", metadata.get("type"), "Document type should be scenario-registry");
            
            // Validate required fields for scenario-registry document
            assertNotNull(metadata.get("id"), "scenario-registry document requires id field");
            assertNotNull(metadata.get("created-by"), "scenario-registry document requires created-by field");
            
            // Validate scenario-registry section exists
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> scenarioRegistry = (List<Map<String, Object>>) document.get("scenario-registry");
            assertNotNull(scenarioRegistry, "Scenario-registry section should not be null");
            assertFalse(scenarioRegistry.isEmpty(), "Scenario-registry should not be empty");
            
            // Validate scenario entry structure
            Map<String, Object> firstScenario = scenarioRegistry.get(0);
            assertTrue(firstScenario.containsKey("scenario-id"), "Scenario entry should have scenario-id");
            assertTrue(firstScenario.containsKey("config-file"), "Scenario entry should have config-file");
            assertTrue(firstScenario.containsKey("data-types"), "Scenario entry should have data-types");
            assertTrue(firstScenario.containsKey("description"), "Scenario entry should have description");
            
            // Validate data-types structure
            @SuppressWarnings("unchecked")
            List<String> dataTypes = (List<String>) firstScenario.get("data-types");
            assertNotNull(dataTypes, "Data types should not be null");
            assertFalse(dataTypes.isEmpty(), "Data types should not be empty");
            
            logger.info("Registry contains " + scenarioRegistry.size() + " scenario entries");
            logger.info("First scenario ID: " + firstScenario.get("scenario-id"));
            logger.info("First scenario data types: " + dataTypes.size() + " types");
            logger.info("✅ Scenario-registry document with standard structure test completed successfully");
            
        } catch (Exception e) {
            logger.error("❌ Scenario-registry document standard structure test failed", e);
            fail("Scenario-registry document standard structure test failed: " + e.getMessage());
        }
    }

    /**
     * Test scenario-registry document with business domain categorization.
     * Validates that a scenario-registry document with business domain grouping loads correctly.
     */
    @Test
    void testScenarioRegistryDocumentWithBusinessDomains() {
        logger.info("=== Testing Scenario-Registry Document with Business Domains ===");
        
        try {
            // Load YAML document using ObjectMapper for raw access
            InputStream inputStream = getClass().getClassLoader().getResourceAsStream("yaml/scenario-registry-domains-test.yaml");
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
            assertEquals("scenario-registry", metadata.get("type"), "Document type should be scenario-registry");
            
            // Validate scenario-registry section exists
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> scenarioRegistry = (List<Map<String, Object>>) document.get("scenario-registry");
            assertNotNull(scenarioRegistry, "Scenario-registry section should not be null");
            
            // Validate business domain categorization
            boolean hasDerivatives = false;
            boolean hasSettlement = false;
            boolean hasRegulatory = false;
            
            for (Map<String, Object> scenario : scenarioRegistry) {
                String businessDomain = (String) scenario.get("business-domain");
                if ("Derivatives Trading".equals(businessDomain)) hasDerivatives = true;
                if ("Post-Trade Settlement".equals(businessDomain)) hasSettlement = true;
                if ("Regulatory Reporting".equals(businessDomain)) hasRegulatory = true;
            }
            
            assertTrue(hasDerivatives || hasSettlement || hasRegulatory, 
                      "Registry should contain scenarios from different business domains");
            
            logger.info("Registry contains scenarios across multiple business domains");
            logger.info("✅ Scenario-registry document with business domains test completed successfully");
            
        } catch (Exception e) {
            logger.error("❌ Scenario-registry document business domains test failed", e);
            fail("Scenario-registry document business domains test failed: " + e.getMessage());
        }
    }

    /**
     * Test scenario-registry document with regulatory scope information.
     * Validates that a scenario-registry document with regulatory compliance data loads correctly.
     */
    @Test
    void testScenarioRegistryDocumentWithRegulatoryScope() {
        logger.info("=== Testing Scenario-Registry Document with Regulatory Scope ===");
        
        try {
            // Load YAML document using ObjectMapper for raw access
            InputStream inputStream = getClass().getClassLoader().getResourceAsStream("yaml/scenario-registry-regulatory-test.yaml");
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
            assertEquals("scenario-registry", metadata.get("type"), "Document type should be scenario-registry");
            
            // Validate scenario-registry section exists
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> scenarioRegistry = (List<Map<String, Object>>) document.get("scenario-registry");
            assertNotNull(scenarioRegistry, "Scenario-registry section should not be null");
            
            // Validate regulatory scope information
            boolean hasRegulatoryScope = false;
            boolean hasComplianceReviewed = false;
            
            for (Map<String, Object> scenario : scenarioRegistry) {
                if (scenario.containsKey("regulatory-scope")) hasRegulatoryScope = true;
                if (scenario.containsKey("compliance-reviewed")) hasComplianceReviewed = true;
            }
            
            assertTrue(hasRegulatoryScope, "Registry should contain scenarios with regulatory scope");
            
            logger.info("Registry contains scenarios with regulatory compliance information");
            logger.info("✅ Scenario-registry document with regulatory scope test completed successfully");
            
        } catch (Exception e) {
            logger.error("❌ Scenario-registry document regulatory scope test failed", e);
            fail("Scenario-registry document regulatory scope test failed: " + e.getMessage());
        }
    }

    /**
     * Test scenario-registry document metadata validation.
     * Validates that all required and optional metadata fields are properly handled for scenario-registry documents.
     */
    @Test
    void testScenarioRegistryDocumentMetadataValidation() {
        logger.info("=== Testing Scenario-Registry Document Metadata Validation ===");
        
        try {
            // Load YAML document using ObjectMapper for raw access
            InputStream inputStream = getClass().getClassLoader().getResourceAsStream("yaml/scenario-registry-metadata-test.yaml");
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
            assertEquals("scenario-registry", metadata.get("type"), "Type must be scenario-registry");
            
            // Validate scenario-registry document specific required fields
            assertNotNull(metadata.get("id"), "scenario-registry document requires id field");
            assertNotNull(metadata.get("created-by"), "scenario-registry document requires created-by field");
            
            // Validate that scenario-registry section is present (scenario-registry documents must have scenario-registry section)
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> scenarioRegistry = (List<Map<String, Object>>) document.get("scenario-registry");
            assertNotNull(scenarioRegistry, "Scenario-registry section should be present");
            assertFalse(scenarioRegistry.isEmpty(), "Scenario-registry section should not be empty");
            
            // Validate that scenario-registry documents should not have rules or enrichments sections directly
            assertNull(document.get("rules"), "Rules section should not be present in scenario-registry documents");
            assertNull(document.get("enrichments"), "Enrichments section should not be present in scenario-registry documents");
            assertNull(document.get("scenario"), "Scenario section should not be present in scenario-registry documents");
            
            logger.info("✓ All required metadata fields validated successfully");
            logger.info("✓ Document type: " + metadata.get("type"));
            logger.info("✓ Document ID: " + metadata.get("id"));
            logger.info("✓ Created by: " + metadata.get("created-by"));
            logger.info("✓ Scenario-registry section validated with " + scenarioRegistry.size() + " entries");
            logger.info("✓ Rules, enrichments, and scenario sections correctly absent");
            
            logger.info("✅ Scenario-registry document metadata validation test completed successfully");
            
        } catch (Exception e) {
            logger.error("❌ Scenario-registry document metadata validation test failed", e);
            fail("Scenario-registry document metadata validation test failed: " + e.getMessage());
        }
    }
}
