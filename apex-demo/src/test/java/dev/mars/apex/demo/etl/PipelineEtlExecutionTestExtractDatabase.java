package dev.mars.apex.demo.etl;

import dev.mars.apex.core.config.yaml.YamlConfigurationLoader;
import dev.mars.apex.core.config.yaml.YamlRuleConfiguration;
import dev.mars.apex.core.engine.pipeline.DataPipelineEngine;
import dev.mars.apex.core.engine.pipeline.YamlPipelineExecutionResult;
import dev.mars.apex.demo.DemoTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for PipelineEtlExecutionTestExtractDatabase.yaml
 * Tests database extraction functionality
 */
@DisplayName("Database Extract Pipeline Test")
class PipelineEtlExecutionTestExtractDatabase extends DemoTestBase {

    private static final Logger logger = LoggerFactory.getLogger(PipelineEtlExecutionTestExtractDatabase.class);
    
    private DataPipelineEngine pipelineEngine;
    private YamlConfigurationLoader yamlLoader;

    @BeforeEach
    public void setUp() {
        super.setUp();
        logger.info("Setting up Database Extract Pipeline Test...");
        pipelineEngine = new DataPipelineEngine();
        yamlLoader = new YamlConfigurationLoader();

        try {
            // Ensure database directory exists
            Path dbDir = Paths.get("./target/test/etl/database");
            Files.createDirectories(dbDir);

            // Setup H2 database with customers table and test data
            setupCustomerDatabase();

        } catch (IOException e) {
            throw new RuntimeException("Failed to create database directory", e);
        }

        logger.info("✓ Database Extract Pipeline Test setup completed");
    }

    /**
     * Setup H2 database with customers table and test data.
     * Following the pattern from other ETL tests.
     */
    private void setupCustomerDatabase() {
        logger.info("Setting up H2 database with customer test data...");

        String jdbcUrl = "jdbc:h2:./target/test/etl/database/test_db;DB_CLOSE_DELAY=-1;MODE=PostgreSQL";

        try (Connection connection = DriverManager.getConnection(jdbcUrl, "sa", "")) {
            Statement statement = connection.createStatement();

            // Drop existing table
            statement.execute("DROP TABLE IF EXISTS customers");

            // Create customers table with columns expected by the query
            statement.execute("""
                CREATE TABLE customers (
                    id INTEGER PRIMARY KEY,
                    name VARCHAR(255) NOT NULL,
                    email VARCHAR(255),
                    status VARCHAR(50) DEFAULT 'ACTIVE',
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
                """);

            // Insert test data
            statement.execute("""
                INSERT INTO customers (id, name, email, status) VALUES
                (1, 'John Doe', 'john.doe@example.com', 'ACTIVE'),
                (2, 'Jane Smith', 'jane.smith@example.com', 'ACTIVE'),
                (3, 'Bob Johnson', 'bob.johnson@example.com', 'INACTIVE')
                """);

            logger.info("✓ H2 database setup completed successfully with 3 customer records");

        } catch (Exception e) {
            logger.error("Failed to setup H2 database: " + e.getMessage(), e);
            throw new RuntimeException("Database setup failed", e);
        }
    }

    @Test
    @DisplayName("Should extract data from H2 database")
    void shouldExtractDataFromH2Database() throws Exception {
        logger.info("=== Testing Database Extract Pipeline ===");

        // Load the YAML configuration
        YamlRuleConfiguration config = yamlLoader.loadFromFile(
            "src/test/java/dev/mars/apex/demo/etl/PipelineEtlExecutionTestExtractDatabase.yaml");
        
        // Initialize and execute pipeline
        pipelineEngine.initialize(config);
        YamlPipelineExecutionResult result = pipelineEngine.executePipeline("database-extract-pipeline");

        // Validate results
        assertNotNull(result, "Pipeline execution result should not be null");
        assertTrue(result.isSuccess(), "Pipeline should execute successfully");
        assertEquals(1, result.getStepResults().size(), "Should have 1 step result (extract only)");

        // Validate extract step
        var extractResult = result.getStepResults().get(0);
        assertEquals("extract-customers", extractResult.getStepName());
        assertTrue(extractResult.isSuccess(), "Extract step should succeed");

        // Validate that data was actually extracted
        assertNotNull(extractResult.getData(), "Extract step should return data");

        // The DatabaseDataSource.getData() method returns a single Map<String, Object> for the first row
        // This is different from the query() method which returns List<Map<String, Object>>
        Object extractedData = extractResult.getData();

        if (extractedData instanceof Map) {
            // Single record returned by getData()
            @SuppressWarnings("unchecked")
            Map<String, Object> singleRecord = (Map<String, Object>) extractedData;

            // Validate the structure of the record
            assertTrue(singleRecord.containsKey("ID"), "Record should contain ID field");
            assertTrue(singleRecord.containsKey("NAME"), "Record should contain NAME field");
            assertTrue(singleRecord.containsKey("EMAIL"), "Record should contain EMAIL field");
            assertTrue(singleRecord.containsKey("STATUS"), "Record should contain STATUS field");

            logger.info("✓ Database extract pipeline test completed successfully");
            logger.info("  - Extract step: {} (success: {})", extractResult.getStepName(), extractResult.isSuccess());
            logger.info("  - Single record extracted: {}", singleRecord);
            logger.info("  - Total execution time: {}ms", result.getDurationMs());

        } else if (extractedData instanceof List) {
            // Multiple records returned (if implementation changes)
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> recordList = (List<Map<String, Object>>) extractedData;
            assertTrue(recordList.size() > 0, "Should extract at least one customer record");

            // Validate the structure of the first record
            Map<String, Object> firstRecord = recordList.get(0);
            assertTrue(firstRecord.containsKey("ID"), "Record should contain ID field");
            assertTrue(firstRecord.containsKey("NAME"), "Record should contain NAME field");
            assertTrue(firstRecord.containsKey("EMAIL"), "Record should contain EMAIL field");
            assertTrue(firstRecord.containsKey("STATUS"), "Record should contain STATUS field");

            logger.info("✓ Database extract pipeline test completed successfully");
            logger.info("  - Extract step: {} (success: {})", extractResult.getStepName(), extractResult.isSuccess());
            logger.info("  - Records extracted: {}", recordList.size());
            logger.info("  - First record: {}", firstRecord);
            logger.info("  - Total execution time: {}ms", result.getDurationMs());

        } else {
            fail("Unexpected data type returned from extract step: " +
                 (extractedData != null ? extractedData.getClass().getName() : "null"));
        }
    }
}
