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
 * Test class for validating bootstrap document type functionality.
 * 
 * This test validates:
 * - Document structure and metadata validation for bootstrap type
 * - Bootstrap section processing with demo configurations
 * - Data-sources section processing with infrastructure setup
 * - Bootstrap-specific validation requirements
 * 
 * Based on APEX_YAML_REFERENCE.md specifications for bootstrap document type.
 */
class BootstrapDocumentTest extends DemoTestBase {

    private static final Logger logger = LoggerFactory.getLogger(BootstrapDocumentTest.class);
    private final ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());

    /**
     * Test basic bootstrap document structure with demo configuration.
     * Validates that a bootstrap document with standard structure loads and processes correctly.
     */
    @Test
    void testBootstrapDocumentWithDemoConfiguration() {
        logger.info("=== Testing Bootstrap Document with Demo Configuration ===");
        
        try {
            // Load YAML document using ObjectMapper for raw access
            InputStream inputStream = getClass().getClassLoader().getResourceAsStream("yaml/bootstrap-demo-test.yaml");
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
            assertEquals("bootstrap", metadata.get("type"), "Document type should be bootstrap");
            
            // Validate required fields for bootstrap document
            assertNotNull(metadata.get("id"), "bootstrap document requires id field");
            assertNotNull(metadata.get("business-domain"), "bootstrap document requires business-domain field");
            assertNotNull(metadata.get("created-by"), "bootstrap document requires created-by field");
            
            // Validate bootstrap section exists
            @SuppressWarnings("unchecked")
            Map<String, Object> bootstrap = (Map<String, Object>) document.get("bootstrap");
            assertNotNull(bootstrap, "Bootstrap section should not be null");
            
            // Validate bootstrap configuration structure
            assertTrue(bootstrap.containsKey("demo-scenarios"), "Bootstrap should have demo-scenarios");
            assertTrue(bootstrap.containsKey("infrastructure"), "Bootstrap should have infrastructure");
            
            // Validate demo scenarios
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> demoScenarios = (List<Map<String, Object>>) bootstrap.get("demo-scenarios");
            assertNotNull(demoScenarios, "Demo scenarios should not be null");
            assertFalse(demoScenarios.isEmpty(), "Demo scenarios should not be empty");
            
            Map<String, Object> firstScenario = demoScenarios.get(0);
            assertTrue(firstScenario.containsKey("scenario-id"), "Demo scenario should have scenario-id");
            assertTrue(firstScenario.containsKey("description"), "Demo scenario should have description");
            assertTrue(firstScenario.containsKey("data-types"), "Demo scenario should have data-types");
            
            logger.info("Bootstrap contains " + demoScenarios.size() + " demo scenarios");
            logger.info("First scenario ID: " + firstScenario.get("scenario-id"));
            logger.info("✅ Bootstrap document with demo configuration test completed successfully");
            
        } catch (Exception e) {
            logger.error("❌ Bootstrap document demo configuration test failed", e);
            fail("Bootstrap document demo configuration test failed: " + e.getMessage());
        }
    }

    /**
     * Test bootstrap document with data sources configuration.
     * Validates that a bootstrap document with data sources infrastructure loads correctly.
     */
    @Test
    void testBootstrapDocumentWithDataSources() {
        logger.info("=== Testing Bootstrap Document with Data Sources ===");
        
        try {
            // Load YAML document using ObjectMapper for raw access
            InputStream inputStream = getClass().getClassLoader().getResourceAsStream("yaml/bootstrap-datasources-test.yaml");
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
            assertEquals("bootstrap", metadata.get("type"), "Document type should be bootstrap");
            
            // Validate data-sources section exists
            @SuppressWarnings("unchecked")
            Map<String, Object> dataSources = (Map<String, Object>) document.get("data-sources");
            assertNotNull(dataSources, "Data-sources section should not be null");
            
            // Validate database configuration
            assertTrue(dataSources.containsKey("database"), "Data sources should have database configuration");
            @SuppressWarnings("unchecked")
            Map<String, Object> database = (Map<String, Object>) dataSources.get("database");
            assertTrue(database.containsKey("type"), "Database should have type");
            assertTrue(database.containsKey("url"), "Database should have url");
            assertTrue(database.containsKey("username"), "Database should have username");
            
            // Validate external APIs configuration
            assertTrue(dataSources.containsKey("external-apis"), "Data sources should have external-apis configuration");
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> externalApis = (List<Map<String, Object>>) dataSources.get("external-apis");
            assertNotNull(externalApis, "External APIs should not be null");
            assertFalse(externalApis.isEmpty(), "External APIs should not be empty");
            
            Map<String, Object> firstApi = externalApis.get(0);
            assertTrue(firstApi.containsKey("name"), "External API should have name");
            assertTrue(firstApi.containsKey("base-url"), "External API should have base-url");
            assertTrue(firstApi.containsKey("authentication"), "External API should have authentication");
            
            logger.info("Data sources configured with database and " + externalApis.size() + " external APIs");
            logger.info("✅ Bootstrap document with data sources test completed successfully");
            
        } catch (Exception e) {
            logger.error("❌ Bootstrap document data sources test failed", e);
            fail("Bootstrap document data sources test failed: " + e.getMessage());
        }
    }

    /**
     * Test bootstrap document with infrastructure setup.
     * Validates that a bootstrap document with complete infrastructure configuration loads correctly.
     */
    @Test
    void testBootstrapDocumentWithInfrastructure() {
        logger.info("=== Testing Bootstrap Document with Infrastructure Setup ===");
        
        try {
            // Load YAML document using ObjectMapper for raw access
            InputStream inputStream = getClass().getClassLoader().getResourceAsStream("yaml/bootstrap-infrastructure-test.yaml");
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
            assertEquals("bootstrap", metadata.get("type"), "Document type should be bootstrap");
            
            // Validate bootstrap section with infrastructure
            @SuppressWarnings("unchecked")
            Map<String, Object> bootstrap = (Map<String, Object>) document.get("bootstrap");
            assertNotNull(bootstrap, "Bootstrap section should not be null");
            
            // Validate infrastructure configuration
            assertTrue(bootstrap.containsKey("infrastructure"), "Bootstrap should have infrastructure");
            @SuppressWarnings("unchecked")
            Map<String, Object> infrastructure = (Map<String, Object>) bootstrap.get("infrastructure");
            
            // Validate database setup
            assertTrue(infrastructure.containsKey("database-setup"), "Infrastructure should have database-setup");
            @SuppressWarnings("unchecked")
            Map<String, Object> databaseSetup = (Map<String, Object>) infrastructure.get("database-setup");
            assertTrue(databaseSetup.containsKey("create-tables"), "Database setup should have create-tables");
            assertTrue(databaseSetup.containsKey("seed-data"), "Database setup should have seed-data");
            
            // Validate performance monitoring
            assertTrue(infrastructure.containsKey("performance-monitoring"), "Infrastructure should have performance-monitoring");
            @SuppressWarnings("unchecked")
            Map<String, Object> performanceMonitoring = (Map<String, Object>) infrastructure.get("performance-monitoring");
            assertTrue(performanceMonitoring.containsKey("enabled"), "Performance monitoring should have enabled flag");
            assertTrue(performanceMonitoring.containsKey("metrics"), "Performance monitoring should have metrics");
            
            logger.info("Infrastructure configured with database setup and performance monitoring");
            logger.info("✅ Bootstrap document with infrastructure test completed successfully");
            
        } catch (Exception e) {
            logger.error("❌ Bootstrap document infrastructure test failed", e);
            fail("Bootstrap document infrastructure test failed: " + e.getMessage());
        }
    }

    /**
     * Test bootstrap document metadata validation.
     * Validates that all required and optional metadata fields are properly handled for bootstrap documents.
     */
    @Test
    void testBootstrapDocumentMetadataValidation() {
        logger.info("=== Testing Bootstrap Document Metadata Validation ===");
        
        try {
            // Load YAML document using ObjectMapper for raw access
            InputStream inputStream = getClass().getClassLoader().getResourceAsStream("yaml/bootstrap-metadata-test.yaml");
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
            assertEquals("bootstrap", metadata.get("type"), "Type must be bootstrap");
            
            // Validate bootstrap document specific required fields
            assertNotNull(metadata.get("id"), "bootstrap document requires id field");
            assertNotNull(metadata.get("business-domain"), "bootstrap document requires business-domain field");
            assertNotNull(metadata.get("created-by"), "bootstrap document requires created-by field");
            
            // Validate that bootstrap section is present (bootstrap documents must have bootstrap section)
            @SuppressWarnings("unchecked")
            Map<String, Object> bootstrap = (Map<String, Object>) document.get("bootstrap");
            assertNotNull(bootstrap, "Bootstrap section should be present");
            
            // Validate that data-sources section is present (bootstrap documents must have data-sources section)
            @SuppressWarnings("unchecked")
            Map<String, Object> dataSources = (Map<String, Object>) document.get("data-sources");
            assertNotNull(dataSources, "Data-sources section should be present");
            
            // Validate that bootstrap documents should not have rules or enrichments sections directly
            assertNull(document.get("rules"), "Rules section should not be present in bootstrap documents");
            assertNull(document.get("enrichments"), "Enrichments section should not be present in bootstrap documents");
            assertNull(document.get("scenario"), "Scenario section should not be present in bootstrap documents");
            
            logger.info("✓ All required metadata fields validated successfully");
            logger.info("✓ Document type: " + metadata.get("type"));
            logger.info("✓ Document ID: " + metadata.get("id"));
            logger.info("✓ Business domain: " + metadata.get("business-domain"));
            logger.info("✓ Created by: " + metadata.get("created-by"));
            logger.info("✓ Bootstrap and data-sources sections validated");
            logger.info("✓ Rules, enrichments, and scenario sections correctly absent");
            
            logger.info("✅ Bootstrap document metadata validation test completed successfully");
            
        } catch (Exception e) {
            logger.error("❌ Bootstrap document metadata validation test failed", e);
            fail("Bootstrap document metadata validation test failed: " + e.getMessage());
        }
    }
}
