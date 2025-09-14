package dev.mars.apex.demo.yaml;

import dev.mars.apex.core.config.yaml.YamlDataset;
import dev.mars.apex.demo.DemoTestBase;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for validating dataset document type functionality.
 *
 * This test validates:
 * - Document structure and metadata validation for dataset type
 * - Data section processing (only valid section for dataset documents)
 * - Different data source types (inline, external, database, etc.)
 * - Dataset-specific validation requirements
 *
 * Based on APEX_YAML_REFERENCE.md specifications for dataset document type.
 */
class DatasetDocumentTest extends DemoTestBase {

    private static final Logger logger = LoggerFactory.getLogger(DatasetDocumentTest.class);
    private final ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());

    /**
     * Test basic dataset document structure with inline data.
     * Validates that a dataset document with inline data loads and processes correctly.
     */
    @Test
    void testDatasetDocumentWithInlineData() {
        logger.info("=== Testing Dataset Document with Inline Data ===");

        try {
            // Load YAML dataset using ObjectMapper
            InputStream inputStream = getClass().getClassLoader().getResourceAsStream("yaml/dataset-inline-test.yaml");
            assertNotNull(inputStream, "YAML file should be found");

            YamlDataset dataset = yamlMapper.readValue(inputStream, YamlDataset.class);
            assertNotNull(dataset, "Dataset should not be null");
            logger.info("✓ Dataset loaded successfully: " + dataset.getName());

            // Validate document type
            assertEquals("dataset", dataset.getType(), "Document type should be dataset");

            // Validate required fields for dataset document
            assertNotNull(dataset.getMetadata(), "Metadata should not be null");
            assertTrue(dataset.getMetadata().containsKey("id"), "dataset document requires id field");
            assertTrue(dataset.getMetadata().containsKey("source"), "dataset document requires source field");

            // Validate data section exists (only valid section for dataset documents)
            assertNotNull(dataset.getData(), "Data section should not be null");
            assertFalse(dataset.getData().isEmpty(), "Data section should not be empty");

            // The data section in YamlDataset is a List<Map<String, Object>>, not a Map
            // So we need to check the structure differently
            List<Map<String, Object>> dataRecords = dataset.getData();
            assertTrue(dataRecords.size() > 0, "Should have data records");

            // Check if we have counterparties data (first few records should be counterparties)
            Map<String, Object> firstRecord = dataRecords.get(0);
            assertTrue(firstRecord.containsKey("name"), "First record should have name field");
            assertTrue(firstRecord.containsKey("lei"), "First record should have LEI field");

            logger.info("Dataset contains " + dataRecords.size() + " total records");
            logger.info("✅ Dataset document with inline data test completed successfully");

        } catch (Exception e) {
            logger.error("❌ Dataset document inline data test failed", e);
            fail("Dataset document inline data test failed: " + e.getMessage());
        }
    }

    /**
     * Test dataset document structure with external data source configuration.
     * Validates that a dataset document with external data source references loads correctly.
     */
    @Test
    void testDatasetDocumentWithExternalData() {
        logger.info("=== Testing Dataset Document with External Data ===");

        try {
            // Load YAML dataset using ObjectMapper
            InputStream inputStream = getClass().getClassLoader().getResourceAsStream("yaml/dataset-external-test.yaml");
            assertNotNull(inputStream, "YAML file should be found");

            YamlDataset dataset = yamlMapper.readValue(inputStream, YamlDataset.class);
            assertNotNull(dataset, "Dataset should not be null");
            logger.info("✓ Dataset loaded successfully: " + dataset.getName());

            // Validate document type
            assertEquals("dataset", dataset.getType(), "Document type should be dataset");

            // Validate data section exists
            assertNotNull(dataset.getData(), "Data section should not be null");
            assertFalse(dataset.getData().isEmpty(), "Data section should not be empty");

            // Find external data source configuration record
            List<Map<String, Object>> dataRecords = dataset.getData();
            Map<String, Object> sourceConfigRecord = dataRecords.stream()
                .filter(record -> "source-config".equals(record.get("recordType")))
                .findFirst()
                .orElse(null);

            assertNotNull(sourceConfigRecord, "Should contain source-config record");
            assertTrue(sourceConfigRecord.containsKey("type"), "Source config should have type");
            assertEquals("external", sourceConfigRecord.get("type"), "Source type should be external");

            logger.info("External data source type: " + sourceConfigRecord.get("type"));
            logger.info("Dataset contains " + dataRecords.size() + " configuration records");
            logger.info("✅ Dataset document with external data test completed successfully");

        } catch (Exception e) {
            logger.error("❌ Dataset document external data test failed", e);
            fail("Dataset document external data test failed: " + e.getMessage());
        }
    }

    /**
     * Test dataset document structure with database data source.
     * Validates that a dataset document with database configuration loads correctly.
     */
    @Test
    void testDatasetDocumentWithDatabaseData() {
        logger.info("=== Testing Dataset Document with Database Data ===");

        try {
            // Load YAML dataset using ObjectMapper
            InputStream inputStream = getClass().getClassLoader().getResourceAsStream("yaml/dataset-database-test.yaml");
            assertNotNull(inputStream, "YAML file should be found");

            YamlDataset dataset = yamlMapper.readValue(inputStream, YamlDataset.class);
            assertNotNull(dataset, "Dataset should not be null");
            logger.info("✓ Dataset loaded successfully: " + dataset.getName());

            // Validate document type
            assertEquals("dataset", dataset.getType(), "Document type should be dataset");

            // Validate data section exists
            assertNotNull(dataset.getData(), "Data section should not be null");
            assertFalse(dataset.getData().isEmpty(), "Data section should not be empty");

            // Find database configuration record
            List<Map<String, Object>> dataRecords = dataset.getData();
            Map<String, Object> dbConfigRecord = dataRecords.stream()
                .filter(record -> "database-config".equals(record.get("recordType")))
                .findFirst()
                .orElse(null);

            assertNotNull(dbConfigRecord, "Should contain database-config record");
            assertTrue(dbConfigRecord.containsKey("type"), "Database config should have type");
            assertEquals("database", dbConfigRecord.get("type"), "Source type should be database");
            assertTrue(dbConfigRecord.containsKey("driver"), "Database config should have driver");

            logger.info("Database source type: " + dbConfigRecord.get("type"));
            logger.info("Dataset contains " + dataRecords.size() + " configuration records");
            logger.info("✅ Dataset document with database data test completed successfully");

        } catch (Exception e) {
            logger.error("❌ Dataset document database data test failed", e);
            fail("Dataset document database data test failed: " + e.getMessage());
        }
    }

    /**
     * Test dataset document metadata validation.
     * Validates that all required and optional metadata fields are properly handled for dataset documents.
     */
    @Test
    void testDatasetDocumentMetadataValidation() {
        logger.info("=== Testing Dataset Document Metadata Validation ===");

        try {
            // Load YAML dataset using ObjectMapper
            InputStream inputStream = getClass().getClassLoader().getResourceAsStream("yaml/dataset-metadata-test.yaml");
            assertNotNull(inputStream, "YAML file should be found");

            YamlDataset dataset = yamlMapper.readValue(inputStream, YamlDataset.class);
            assertNotNull(dataset, "Dataset should not be null");

            // Validate required metadata fields
            assertNotNull(dataset.getMetadata(), "Metadata should not be null");
            assertNotNull(dataset.getName(), "Name is required");
            assertNotNull(dataset.getVersion(), "Version is required");
            assertNotNull(dataset.getDescription(), "Description is required");
            assertEquals("dataset", dataset.getType(), "Type must be dataset");

            // Validate dataset document specific required fields
            assertTrue(dataset.getMetadata().containsKey("id"), "dataset document requires id field");
            assertTrue(dataset.getMetadata().containsKey("source"), "dataset document requires source field");

            // Validate that data section is present (dataset documents must have data section)
            assertNotNull(dataset.getData(), "Data section should be present");
            assertFalse(dataset.getData().isEmpty(), "Data section should not be empty");

            // Validate data content
            List<Map<String, Object>> dataRecords = dataset.getData();
            assertTrue(dataRecords.size() >= 2, "Should have at least 2 validation records");

            logger.info("✓ All required metadata fields validated successfully");
            logger.info("✓ Document type: " + dataset.getType());
            logger.info("✓ Document ID: " + dataset.getMetadata().get("id"));
            logger.info("✓ Document source: " + dataset.getMetadata().get("source"));
            logger.info("✓ Data section validated with " + dataRecords.size() + " records");

            logger.info("✅ Dataset document metadata validation test completed successfully");

        } catch (Exception e) {
            logger.error("❌ Dataset document metadata validation test failed", e);
            fail("Dataset document metadata validation test failed: " + e.getMessage());
        }
    }
}
