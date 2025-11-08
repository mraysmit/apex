package dev.mars.apex.demo.etl;

/*
 * Copyright 2025 Mark Andrew Ray-Smith Cityline Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import dev.mars.apex.core.engine.config.RulesEngine;
import dev.mars.apex.core.engine.model.RuleResult;
import dev.mars.apex.demo.DemoTestBase;
import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test for Simple ETL Pipeline functionality using APEX RulesEngine.
 *
 * FOLLOWS CODING PRINCIPLES FROM prompts.txt:
 * ✅ Never validate YAML syntax - test actual pipeline execution
 * ✅ Execute real APEX pipeline operations using RulesEngine.evaluate()
 * ✅ Set up real data sources (CSV files) and sinks (H2 database)
 * ✅ Validate functional results with specific assertions on processed data
 * ✅ Test end-to-end workflows from data setup through pipeline execution to result validation
 *
 * BUSINESS LOGIC VALIDATION:
 * - Extract step: Read test data from CSV file using file-system data source
 * - Load step: Insert extracted data into H2 database
 * - Validate actual data was loaded into database with correct values
 * - Verify pipeline execution completes successfully
 *
 * @author APEX Demo Team
 * @since 2025-09-28
 * @version 1.0.0
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Simple Pipeline Test - ETL Functionality")
public class SimplePipelineTest extends DemoTestBase {

    private static final Logger logger = LoggerFactory.getLogger(SimplePipelineTest.class);

    private RulesEngine rulesEngine;

    @BeforeEach
    public void setUp() {
        super.setUp();
        logger.info("=== Setting up Simple Pipeline Test ===");
        setupTestData();
    }

    @AfterEach
    public void tearDown() {
        // Close rules engine if it was created
        if (rulesEngine != null) {
            try {
                rulesEngine.shutdown();
                logger.info("Rules engine shut down successfully");
            } catch (Exception e) {
                logger.warn("Error shutting down rules engine", e);
            }
        }
        super.tearDown();
    }

    @Test
    @Order(1)
    @DisplayName("Should extract data from CSV file and load into H2 database")
    void testCsvToH2PipelineExecution() throws Exception {
        logger.info("=== Testing CSV to H2 Pipeline Execution ===");

        // Initialize RulesEngine from YAML file
        rulesEngine = RulesEngine.fromFile("src/test/java/dev/mars/apex/demo/etl/SimplePipelineTest.yaml");
        assertNotNull(rulesEngine, "Rules engine should be created");
        logger.info("✓ RulesEngine initialized successfully");

        // Execute the pipeline
        java.util.Map<String, Object> inputData = new java.util.HashMap<>();
        RuleResult result = rulesEngine.evaluate(inputData);

        // Validate pipeline execution results
        assertNotNull(result, "Pipeline execution result should not be null");
        assertEquals(RuleResult.ResultType.MATCH, result.getResultType(),
            "Pipeline should execute successfully");
        logger.info("✓ Pipeline executed successfully");
        logger.info("  - Result type: {}", result.getResultType());
        logger.info("  - Message: {}", result.getMessage());

        // Validate actual data was loaded into H2 database
        validateDatabaseContents();

        logger.info("✓ CSV to H2 pipeline execution test completed successfully");
    }

    @Test
    @Order(2)
    @DisplayName("Should verify data transformation during pipeline execution")
    void testDataTransformationInPipeline() throws Exception {
        logger.info("=== Testing Data Transformation in Pipeline ===");

        // Initialize RulesEngine from YAML file
        rulesEngine = RulesEngine.fromFile("src/test/java/dev/mars/apex/demo/etl/SimplePipelineTest.yaml");

        // Execute the pipeline
        java.util.Map<String, Object> inputData = new java.util.HashMap<>();
        RuleResult result = rulesEngine.evaluate(inputData);

        // Validate pipeline execution
        assertNotNull(result, "Pipeline execution result should not be null");
        assertTrue(result.isSuccess(), "Pipeline should execute successfully");
        logger.info("✓ Pipeline executed successfully");

        // Validate specific data was transformed and loaded correctly
        try (Connection conn = getH2Connection()) {
            Statement stmt = conn.createStatement();

            // Verify record count
            ResultSet countRs = stmt.executeQuery("SELECT COUNT(*) as cnt FROM test");
            assertTrue(countRs.next(), "Should have count result");
            int recordCount = countRs.getInt("cnt");
            assertEquals(3, recordCount, "Should have loaded 3 records from CSV");
            logger.info("✓ Verified {} records loaded into database", recordCount);

            // Verify specific record values
            ResultSet dataRs = stmt.executeQuery("SELECT id, data FROM test ORDER BY id");

            // Record 1
            assertTrue(dataRs.next(), "Should have first record");
            assertEquals(1, dataRs.getInt("id"), "First record ID should be 1");
            assertEquals("test-data-1", dataRs.getString("data"), "First record data should match");

            // Record 2
            assertTrue(dataRs.next(), "Should have second record");
            assertEquals(2, dataRs.getInt("id"), "Second record ID should be 2");
            assertEquals("test-data-2", dataRs.getString("data"), "Second record data should match");

            // Record 3
            assertTrue(dataRs.next(), "Should have third record");
            assertEquals(3, dataRs.getInt("id"), "Third record ID should be 3");
            assertEquals("test-data-3", dataRs.getString("data"), "Third record data should match");

            logger.info("✓ All record values verified successfully");
        }

        logger.info("✓ Data transformation test completed successfully");
    }

    // ========================================
    // Helper Methods
    // ========================================

    /**
     * Set up test data for pipeline execution.
     * Creates CSV file with test records.
     */
    private void setupTestData() {
        try {
            logger.info("Setting up test data");

            // Create test directory
            Path testDir = Paths.get("./target/test");
            Files.createDirectories(testDir);

            // Create test CSV file with data
            Path csvFile = testDir.resolve("test.csv");
            try (FileWriter writer = new FileWriter(csvFile.toFile())) {
                writer.write("id,data\n");
                writer.write("1,test-data-1\n");
                writer.write("2,test-data-2\n");
                writer.write("3,test-data-3\n");
            }

            logger.info("✓ Test data setup completed");
            logger.info("  - Test directory: {}", testDir.toAbsolutePath());
            logger.info("  - CSV file: {}", csvFile.toAbsolutePath());
        } catch (IOException e) {
            logger.error("Failed to setup test data", e);
            throw new RuntimeException("Failed to setup test data", e);
        }
    }

    /**
     * Get H2 database connection for validation.
     */
    private Connection getH2Connection() throws Exception {
        String jdbcUrl = "jdbc:h2:./target/test/db";
        return DriverManager.getConnection(jdbcUrl, "sa", "");
    }

    /**
     * Validate that data was correctly loaded into the H2 database.
     */
    private void validateDatabaseContents() throws Exception {
        logger.info("Validating database contents");

        try (Connection conn = getH2Connection()) {
            Statement stmt = conn.createStatement();

            // Verify table exists
            ResultSet tables = conn.getMetaData().getTables(null, null, "TEST", null);
            assertTrue(tables.next(), "Table 'test' should exist in database");
            logger.info("✓ Table 'test' exists");

            // Verify record count
            ResultSet countRs = stmt.executeQuery("SELECT COUNT(*) as cnt FROM test");
            assertTrue(countRs.next(), "Should have count result");
            int recordCount = countRs.getInt("cnt");
            assertTrue(recordCount > 0, "Should have loaded at least one record");
            logger.info("✓ Database contains {} records", recordCount);

            // Verify data integrity
            ResultSet dataRs = stmt.executeQuery("SELECT id, data FROM test ORDER BY id");
            int verifiedRecords = 0;
            while (dataRs.next()) {
                int id = dataRs.getInt("id");
                String data = dataRs.getString("data");
                assertNotNull(data, "Data field should not be null for record " + id);
                verifiedRecords++;
                logger.info("  - Record {}: id={}, data={}", verifiedRecords, id, data);
            }

            assertEquals(recordCount, verifiedRecords, "Should verify all records");
            logger.info("✓ All {} records verified successfully", verifiedRecords);
        }
    }
}
