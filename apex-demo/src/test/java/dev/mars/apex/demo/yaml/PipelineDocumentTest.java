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
 * Test class for validating pipeline document type functionality.
 * 
 * This test validates:
 * - Document structure and metadata validation for pipeline type
 * - Pipeline section processing with ETL workflow orchestration
 * - Data-sources section processing with input data source configurations
 * - Data-sinks section processing with output data sink configurations
 * - Pipeline-specific validation requirements
 * 
 * Based on APEX_YAML_REFERENCE.md specifications for pipeline document type.
 */
class PipelineDocumentTest extends DemoTestBase {

    private static final Logger logger = LoggerFactory.getLogger(PipelineDocumentTest.class);
    private final ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());

    /**
     * Test basic pipeline document structure with ETL workflow.
     * Validates that a pipeline document with extract-transform-load steps loads and processes correctly.
     */
    @Test
    void testPipelineDocumentWithEtlWorkflow() {
        logger.info("=== Testing Pipeline Document with ETL Workflow ===");
        
        try {
            // Load YAML document using ObjectMapper for raw access
            InputStream inputStream = getClass().getClassLoader().getResourceAsStream("yaml/pipeline-etl-test.yaml");
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
            assertEquals("pipeline", metadata.get("type"), "Document type should be pipeline");
            
            // Validate required fields for pipeline document
            assertNotNull(metadata.get("id"), "pipeline document requires id field");
            assertNotNull(metadata.get("author"), "pipeline document requires author field");
            
            // Validate pipeline section exists
            @SuppressWarnings("unchecked")
            Map<String, Object> pipeline = (Map<String, Object>) document.get("pipeline");
            assertNotNull(pipeline, "Pipeline section should not be null");
            
            // Validate pipeline steps
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> steps = (List<Map<String, Object>>) pipeline.get("steps");
            assertNotNull(steps, "Pipeline steps should not be null");
            assertFalse(steps.isEmpty(), "Pipeline steps should not be empty");
            
            // Validate ETL step types - find extract and load steps
            Map<String, Object> extractStep = null;
            Map<String, Object> loadStep = null;

            for (Map<String, Object> step : steps) {
                String stepType = (String) step.get("type");
                if ("extract".equals(stepType) && extractStep == null) {
                    extractStep = step;
                } else if ("load".equals(stepType) && loadStep == null) {
                    loadStep = step;
                }
            }

            assertNotNull(extractStep, "Pipeline should have at least one extract step");
            assertNotNull(loadStep, "Pipeline should have at least one load step");

            assertEquals("extract", extractStep.get("type"), "Extract step should be extract type");
            assertTrue(extractStep.containsKey("source"), "Extract step should have source");

            assertEquals("load", loadStep.get("type"), "Load step should be load type");
            assertTrue(loadStep.containsKey("sink"), "Load step should have sink");
            assertTrue(loadStep.containsKey("depends-on"), "Load step should have dependencies");

            logger.info("Pipeline contains " + steps.size() + " ETL steps");
            logger.info("Extract step source: " + extractStep.get("source"));
            logger.info("Load step sink: " + loadStep.get("sink"));
            logger.info("✅ Pipeline document with ETL workflow test completed successfully");
            
        } catch (Exception e) {
            logger.error("❌ Pipeline document ETL workflow test failed", e);
            fail("Pipeline document ETL workflow test failed: " + e.getMessage());
        }
    }

    /**
     * Test pipeline document with data sources configuration.
     * Validates that a pipeline document with input data sources loads correctly.
     */
    @Test
    void testPipelineDocumentWithDataSources() {
        logger.info("=== Testing Pipeline Document with Data Sources ===");
        
        try {
            // Load YAML document using ObjectMapper for raw access
            InputStream inputStream = getClass().getClassLoader().getResourceAsStream("yaml/pipeline-datasources-test.yaml");
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
            assertEquals("pipeline", metadata.get("type"), "Document type should be pipeline");
            
            // Validate data-sources section exists
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> dataSources = (List<Map<String, Object>>) document.get("data-sources");
            assertNotNull(dataSources, "Data-sources section should not be null");
            assertFalse(dataSources.isEmpty(), "Data-sources should not be empty");
            
            // Validate data source structure
            Map<String, Object> firstDataSource = dataSources.get(0);
            assertTrue(firstDataSource.containsKey("name"), "Data source should have name");
            assertTrue(firstDataSource.containsKey("type"), "Data source should have type");
            assertTrue(firstDataSource.containsKey("connection"), "Data source should have connection");
            
            // Validate file system data source
            assertEquals("file-system", firstDataSource.get("type"), "First data source should be file-system type");
            
            // Validate connection structure
            @SuppressWarnings("unchecked")
            Map<String, Object> connection = (Map<String, Object>) firstDataSource.get("connection");
            assertTrue(connection.containsKey("basePath"), "File system connection should have basePath");
            assertTrue(connection.containsKey("filePattern"), "File system connection should have filePattern");
            
            // Validate file format structure
            assertTrue(firstDataSource.containsKey("fileFormat"), "File system source should have fileFormat");
            @SuppressWarnings("unchecked")
            Map<String, Object> fileFormat = (Map<String, Object>) firstDataSource.get("fileFormat");
            assertTrue(fileFormat.containsKey("type"), "File format should have type");
            
            logger.info("Data sources contains " + dataSources.size() + " input configurations");
            logger.info("First data source type: " + firstDataSource.get("type"));
            logger.info("✅ Pipeline document with data sources test completed successfully");
            
        } catch (Exception e) {
            logger.error("❌ Pipeline document data sources test failed", e);
            fail("Pipeline document data sources test failed: " + e.getMessage());
        }
    }

    /**
     * Test pipeline document with data sinks configuration.
     * Validates that a pipeline document with output data sinks loads correctly.
     */
    @Test
    void testPipelineDocumentWithDataSinks() {
        logger.info("=== Testing Pipeline Document with Data Sinks ===");
        
        try {
            // Load YAML document using ObjectMapper for raw access
            InputStream inputStream = getClass().getClassLoader().getResourceAsStream("yaml/pipeline-datasinks-test.yaml");
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
            assertEquals("pipeline", metadata.get("type"), "Document type should be pipeline");
            
            // Validate data-sinks section exists
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> dataSinks = (List<Map<String, Object>>) document.get("data-sinks");
            assertNotNull(dataSinks, "Data-sinks section should not be null");
            assertFalse(dataSinks.isEmpty(), "Data-sinks should not be empty");
            
            // Validate data sink structure
            Map<String, Object> firstDataSink = dataSinks.get(0);
            assertTrue(firstDataSink.containsKey("name"), "Data sink should have name");
            assertTrue(firstDataSink.containsKey("type"), "Data sink should have type");
            assertTrue(firstDataSink.containsKey("connection"), "Data sink should have connection");
            
            // Validate database data sink
            assertEquals("database", firstDataSink.get("type"), "First data sink should be database type");
            
            // Validate connection structure for database
            @SuppressWarnings("unchecked")
            Map<String, Object> connection = (Map<String, Object>) firstDataSink.get("connection");
            assertTrue(connection.containsKey("database"), "Database connection should have database");
            assertTrue(connection.containsKey("username"), "Database connection should have username");
            
            // Validate operations structure
            assertTrue(firstDataSink.containsKey("operations"), "Database sink should have operations");
            @SuppressWarnings("unchecked")
            Map<String, Object> operations = (Map<String, Object>) firstDataSink.get("operations");
            assertFalse(operations.isEmpty(), "Operations should not be empty");
            
            logger.info("Data sinks contains " + dataSinks.size() + " output configurations");
            logger.info("First data sink type: " + firstDataSink.get("type"));
            logger.info("✅ Pipeline document with data sinks test completed successfully");
            
        } catch (Exception e) {
            logger.error("❌ Pipeline document data sinks test failed", e);
            fail("Pipeline document data sinks test failed: " + e.getMessage());
        }
    }

    /**
     * Test pipeline document metadata validation.
     * Validates that all required and optional metadata fields are properly handled for pipeline documents.
     */
    @Test
    void testPipelineDocumentMetadataValidation() {
        logger.info("=== Testing Pipeline Document Metadata Validation ===");
        
        try {
            // Load YAML document using ObjectMapper for raw access
            InputStream inputStream = getClass().getClassLoader().getResourceAsStream("yaml/pipeline-metadata-test.yaml");
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
            assertEquals("pipeline", metadata.get("type"), "Type must be pipeline");
            
            // Validate pipeline document specific required fields
            assertNotNull(metadata.get("id"), "pipeline document requires id field");
            assertNotNull(metadata.get("author"), "pipeline document requires author field");
            
            // Validate that pipeline section is present (pipeline documents must have pipeline section)
            @SuppressWarnings("unchecked")
            Map<String, Object> pipeline = (Map<String, Object>) document.get("pipeline");
            assertNotNull(pipeline, "Pipeline section should be present");
            
            // Validate that data-sources section is present (pipeline documents must have data-sources section)
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> dataSources = (List<Map<String, Object>>) document.get("data-sources");
            assertNotNull(dataSources, "Data-sources section should be present");
            
            // Validate that data-sinks section is present (pipeline documents must have data-sinks section)
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> dataSinks = (List<Map<String, Object>>) document.get("data-sinks");
            assertNotNull(dataSinks, "Data-sinks section should be present");
            
            // Validate that pipeline documents should not have rules or enrichments sections directly
            assertNull(document.get("rules"), "Rules section should not be present in pipeline documents");
            assertNull(document.get("enrichments"), "Enrichments section should not be present in pipeline documents");
            assertNull(document.get("scenario"), "Scenario section should not be present in pipeline documents");
            
            logger.info("✓ All required metadata fields validated successfully");
            logger.info("✓ Document type: " + metadata.get("type"));
            logger.info("✓ Document ID: " + metadata.get("id"));
            logger.info("✓ Document author: " + metadata.get("author"));
            logger.info("✓ Pipeline section validated");
            logger.info("✓ Data-sources section validated with " + dataSources.size() + " sources");
            logger.info("✓ Data-sinks section validated with " + dataSinks.size() + " sinks");
            logger.info("✓ Rules, enrichments, and scenario sections correctly absent");
            
            logger.info("✅ Pipeline document metadata validation test completed successfully");
            
        } catch (Exception e) {
            logger.error("❌ Pipeline document metadata validation test failed", e);
            fail("Pipeline document metadata validation test failed: " + e.getMessage());
        }
    }
}
