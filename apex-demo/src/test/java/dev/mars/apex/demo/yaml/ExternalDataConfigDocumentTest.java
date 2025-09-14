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
 * Test class for validating external-data-config document type functionality.
 * 
 * This test validates:
 * - Document structure and metadata validation for external-data-config type
 * - DataSources section processing with external data source configurations
 * - Configuration section processing with connection and authentication settings
 * - External-data-config-specific validation requirements
 * 
 * Based on APEX_YAML_REFERENCE.md specifications for external-data-config document type.
 */
class ExternalDataConfigDocumentTest extends DemoTestBase {

    private static final Logger logger = LoggerFactory.getLogger(ExternalDataConfigDocumentTest.class);
    private final ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());

    /**
     * Test basic external-data-config document structure with database configuration.
     * Validates that an external-data-config document with database sources loads and processes correctly.
     */
    @Test
    void testExternalDataConfigDocumentWithDatabaseSources() {
        logger.info("=== Testing External-Data-Config Document with Database Sources ===");
        
        try {
            // Load YAML document using ObjectMapper for raw access
            InputStream inputStream = getClass().getClassLoader().getResourceAsStream("yaml/external-data-config-database-test.yaml");
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
            assertEquals("external-data-config", metadata.get("type"), "Document type should be external-data-config");
            
            // Validate required fields for external-data-config document
            assertNotNull(metadata.get("id"), "external-data-config document requires id field");
            assertNotNull(metadata.get("author"), "external-data-config document requires author field");
            
            // Validate dataSources section exists
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> dataSources = (List<Map<String, Object>>) document.get("dataSources");
            assertNotNull(dataSources, "DataSources section should not be null");
            assertFalse(dataSources.isEmpty(), "DataSources should not be empty");
            
            // Validate database data source structure
            Map<String, Object> firstDataSource = dataSources.get(0);
            assertTrue(firstDataSource.containsKey("name"), "Data source should have name");
            assertTrue(firstDataSource.containsKey("type"), "Data source should have type");
            assertTrue(firstDataSource.containsKey("connection"), "Data source should have connection");
            
            // Validate database type
            assertEquals("database", firstDataSource.get("type"), "First data source should be database type");
            
            // Validate connection structure
            @SuppressWarnings("unchecked")
            Map<String, Object> connection = (Map<String, Object>) firstDataSource.get("connection");
            assertTrue(connection.containsKey("host"), "Connection should have host");
            assertTrue(connection.containsKey("port"), "Connection should have port");
            assertTrue(connection.containsKey("database"), "Connection should have database");
            
            logger.info("Data sources contains " + dataSources.size() + " database configurations");
            logger.info("First data source name: " + firstDataSource.get("name"));
            logger.info("✅ External-data-config document with database sources test completed successfully");
            
        } catch (Exception e) {
            logger.error("❌ External-data-config document database sources test failed", e);
            fail("External-data-config document database sources test failed: " + e.getMessage());
        }
    }

    /**
     * Test external-data-config document with REST API configuration.
     * Validates that an external-data-config document with REST API sources loads correctly.
     */
    @Test
    void testExternalDataConfigDocumentWithRestApiSources() {
        logger.info("=== Testing External-Data-Config Document with REST API Sources ===");
        
        try {
            // Load YAML document using ObjectMapper for raw access
            InputStream inputStream = getClass().getClassLoader().getResourceAsStream("yaml/external-data-config-restapi-test.yaml");
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
            assertEquals("external-data-config", metadata.get("type"), "Document type should be external-data-config");
            
            // Validate dataSources section exists
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> dataSources = (List<Map<String, Object>>) document.get("dataSources");
            assertNotNull(dataSources, "DataSources section should not be null");
            
            // Validate REST API data source structure
            Map<String, Object> firstDataSource = dataSources.get(0);
            assertEquals("rest-api", firstDataSource.get("type"), "First data source should be rest-api type");
            
            // Validate connection structure for REST API
            @SuppressWarnings("unchecked")
            Map<String, Object> connection = (Map<String, Object>) firstDataSource.get("connection");
            assertTrue(connection.containsKey("baseUrl"), "REST API connection should have baseUrl");
            assertTrue(connection.containsKey("timeout"), "REST API connection should have timeout");
            
            // Validate authentication structure
            assertTrue(firstDataSource.containsKey("authentication"), "REST API should have authentication");
            @SuppressWarnings("unchecked")
            Map<String, Object> authentication = (Map<String, Object>) firstDataSource.get("authentication");
            assertTrue(authentication.containsKey("type"), "Authentication should have type");
            
            // Validate endpoints structure
            assertTrue(firstDataSource.containsKey("endpoints"), "REST API should have endpoints");
            @SuppressWarnings("unchecked")
            Map<String, Object> endpoints = (Map<String, Object>) firstDataSource.get("endpoints");
            assertFalse(endpoints.isEmpty(), "Endpoints should not be empty");
            
            logger.info("REST API data source configured with " + endpoints.size() + " endpoints");
            logger.info("✅ External-data-config document with REST API sources test completed successfully");
            
        } catch (Exception e) {
            logger.error("❌ External-data-config document REST API sources test failed", e);
            fail("External-data-config document REST API sources test failed: " + e.getMessage());
        }
    }

    /**
     * Test external-data-config document with configuration section.
     * Validates that an external-data-config document with global configuration loads correctly.
     */
    @Test
    void testExternalDataConfigDocumentWithConfiguration() {
        logger.info("=== Testing External-Data-Config Document with Configuration ===");
        
        try {
            // Load YAML document using ObjectMapper for raw access
            InputStream inputStream = getClass().getClassLoader().getResourceAsStream("yaml/external-data-config-configuration-test.yaml");
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
            assertEquals("external-data-config", metadata.get("type"), "Document type should be external-data-config");
            
            // Validate configuration section exists
            @SuppressWarnings("unchecked")
            Map<String, Object> configuration = (Map<String, Object>) document.get("configuration");
            assertNotNull(configuration, "Configuration section should not be null");
            
            // Validate global configuration settings
            assertTrue(configuration.containsKey("defaultConnectionTimeout"), "Configuration should have defaultConnectionTimeout");
            assertTrue(configuration.containsKey("retryPolicy"), "Configuration should have retryPolicy");
            assertTrue(configuration.containsKey("monitoring"), "Configuration should have monitoring");
            
            // Validate retry policy structure
            @SuppressWarnings("unchecked")
            Map<String, Object> retryPolicy = (Map<String, Object>) configuration.get("retryPolicy");
            assertTrue(retryPolicy.containsKey("maxRetries"), "Retry policy should have maxRetries");
            assertTrue(retryPolicy.containsKey("backoffStrategy"), "Retry policy should have backoffStrategy");
            
            // Validate monitoring structure
            @SuppressWarnings("unchecked")
            Map<String, Object> monitoring = (Map<String, Object>) configuration.get("monitoring");
            assertTrue(monitoring.containsKey("enabled"), "Monitoring should have enabled flag");
            assertTrue(monitoring.containsKey("healthCheckInterval"), "Monitoring should have healthCheckInterval");
            
            logger.info("Configuration includes global settings for timeout, retry, and monitoring");
            logger.info("✅ External-data-config document with configuration test completed successfully");
            
        } catch (Exception e) {
            logger.error("❌ External-data-config document configuration test failed", e);
            fail("External-data-config document configuration test failed: " + e.getMessage());
        }
    }

    /**
     * Test external-data-config document metadata validation.
     * Validates that all required and optional metadata fields are properly handled for external-data-config documents.
     */
    @Test
    void testExternalDataConfigDocumentMetadataValidation() {
        logger.info("=== Testing External-Data-Config Document Metadata Validation ===");
        
        try {
            // Load YAML document using ObjectMapper for raw access
            InputStream inputStream = getClass().getClassLoader().getResourceAsStream("yaml/external-data-config-metadata-test.yaml");
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
            assertEquals("external-data-config", metadata.get("type"), "Type must be external-data-config");
            
            // Validate external-data-config document specific required fields
            assertNotNull(metadata.get("id"), "external-data-config document requires id field");
            assertNotNull(metadata.get("author"), "external-data-config document requires author field");
            
            // Validate that dataSources section is present (external-data-config documents must have dataSources section)
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> dataSources = (List<Map<String, Object>>) document.get("dataSources");
            assertNotNull(dataSources, "DataSources section should be present");
            assertFalse(dataSources.isEmpty(), "DataSources section should not be empty");
            
            // Validate that configuration section is present (external-data-config documents must have configuration section)
            @SuppressWarnings("unchecked")
            Map<String, Object> configuration = (Map<String, Object>) document.get("configuration");
            assertNotNull(configuration, "Configuration section should be present");
            
            // Validate that external-data-config documents should not have rules or enrichments sections directly
            assertNull(document.get("rules"), "Rules section should not be present in external-data-config documents");
            assertNull(document.get("enrichments"), "Enrichments section should not be present in external-data-config documents");
            assertNull(document.get("scenario"), "Scenario section should not be present in external-data-config documents");
            
            logger.info("✓ All required metadata fields validated successfully");
            logger.info("✓ Document type: " + metadata.get("type"));
            logger.info("✓ Document ID: " + metadata.get("id"));
            logger.info("✓ Document author: " + metadata.get("author"));
            logger.info("✓ DataSources section validated with " + dataSources.size() + " sources");
            logger.info("✓ Configuration section validated");
            logger.info("✓ Rules, enrichments, and scenario sections correctly absent");
            
            logger.info("✅ External-data-config document metadata validation test completed successfully");
            
        } catch (Exception e) {
            logger.error("❌ External-data-config document metadata validation test failed", e);
            fail("External-data-config document metadata validation test failed: " + e.getMessage());
        }
    }
}
