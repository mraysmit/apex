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

package dev.mars.apex.demo.etl;

import dev.mars.apex.core.engine.config.RulesEngine;
import dev.mars.apex.core.engine.model.RuleResult;
import dev.mars.apex.demo.DemoTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Focused, simplified test suite for APEX ETL Pipeline functionality.
 * 
 * This test class provides clear, easy-to-follow tests that validate:
 * - File system load operations (CSV to JSON)
 * - Database load operations (CSV to H2)
 * - Error handling and validation
 * 
 * Each test is self-contained and uses a single YAML configuration file
 * for maximum clarity and debuggability.
 */
@DisplayName("Focused ETL Pipeline Tests")
public class FocusedEtlTest extends DemoTestBase {

    private static final Logger logger = LoggerFactory.getLogger(FocusedEtlTest.class);
    private RulesEngine rulesEngine;

    @BeforeEach
    public void setUp() {
        super.setUp();
        logger.info("Setting up Focused ETL Test...");

        // Create test data directories and files
        createTestDirectories();
        createTestDataFiles();

        logger.info("✓ Focused ETL Test setup completed");
    }

    @org.junit.jupiter.api.AfterEach
    public void tearDown() {
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
    @DisplayName("Should load data from CSV to JSON file")
    void shouldLoadDataFromCsvToJsonFile() throws Exception {
        logger.info("=== Testing CSV to JSON File Load ===");

        // Create RulesEngine and execute pipeline
        rulesEngine = RulesEngine.fromClasspath("dev/mars/apex/demo/etl/PipelineEtlExecutionTestLoadFilesystem.yaml");

        java.util.Map<String, Object> inputData = new java.util.HashMap<>();
        RuleResult result = rulesEngine.evaluate(inputData);

        // Validate results
        assertNotNull(result, "Pipeline execution result should not be null");
        assertEquals(RuleResult.ResultType.MATCH, result.getResultType(),
            "Pipeline should execute successfully");

        // Verify output file was created
        Path outputFile = Paths.get("./demo-data/json/customers.json");
        assertTrue(Files.exists(outputFile), "Output JSON file should be created");
        assertTrue(Files.size(outputFile) > 0, "Output file should not be empty");

        logger.info("✓ CSV to JSON file load test completed successfully");
        logger.info("  - Result type: {}", result.getResultType());
        logger.info("  - Output file: {}", outputFile.toAbsolutePath());
    }

    @Test
    @DisplayName("Should load data from CSV to H2 database")
    void shouldLoadDataFromCsvToH2Database() throws Exception {
        logger.info("=== Testing CSV to H2 Database Load ===");

        // Create RulesEngine and execute pipeline
        rulesEngine = RulesEngine.fromClasspath("dev/mars/apex/demo/etl/PipelineEtlExecutionTestLoadDatabase.yaml");

        java.util.Map<String, Object> inputData = new java.util.HashMap<>();
        RuleResult result = rulesEngine.evaluate(inputData);

        // Validate pipeline execution
        assertNotNull(result, "Pipeline execution result should not be null");
        assertEquals(RuleResult.ResultType.MATCH, result.getResultType(),
            "Pipeline should execute successfully");
        logger.info("✓ Pipeline executed successfully");

        // Validate data was actually loaded into database
        validateDatabaseContents();

        logger.info("✓ CSV to H2 database load test completed successfully");
    }

    @Test
    @DisplayName("Should extract data from CSV file only")
    void shouldExtractDataFromCsvFileOnly() throws Exception {
        logger.info("=== Testing CSV Extract Only ===");

        // Create RulesEngine and execute pipeline
        rulesEngine = RulesEngine.fromClasspath("dev/mars/apex/demo/etl/PipelineEtlExecutionTestExtractCsv.yaml");

        java.util.Map<String, Object> inputData = new java.util.HashMap<>();
        RuleResult result = rulesEngine.evaluate(inputData);

        // Validate results
        assertNotNull(result, "Pipeline execution result should not be null");
        assertEquals(RuleResult.ResultType.MATCH, result.getResultType(),
            "Pipeline should execute successfully");

        logger.info("✓ CSV extract only test completed successfully");
        logger.info("  - Result type: {}", result.getResultType());
    }

    @Test
    @DisplayName("Should handle empty CSV file gracefully")
    void shouldHandleEmptyCsvFileGracefully() throws Exception {
        logger.info("=== Testing Empty CSV File Handling ===");

        // Create empty CSV file for testing
        createEmptyTestFile();

        // Create RulesEngine and execute pipeline
        rulesEngine = RulesEngine.fromClasspath("dev/mars/apex/demo/etl/PipelineEtlExecutionTestExtractEmptyCsv.yaml");

        java.util.Map<String, Object> inputData = new java.util.HashMap<>();
        RuleResult result = rulesEngine.evaluate(inputData);

        // Validate graceful handling of empty data
        assertNotNull(result, "Pipeline execution result should not be null");
        assertEquals(RuleResult.ResultType.MATCH, result.getResultType(),
            "Pipeline should handle empty data gracefully");

        logger.info("✓ Empty CSV file handling test completed successfully");
    }

    // Helper methods
    private void createTestDirectories() {
        try {
            Files.createDirectories(Paths.get("./demo-data/csv"));
            Files.createDirectories(Paths.get("./demo-data/json"));
            Files.createDirectories(Paths.get("./demo-data/database"));
        } catch (IOException e) {
            throw new RuntimeException("Failed to create test directories", e);
        }
    }

    private void createTestDataFiles() {
        // Create customers.csv for extract tests
        createCsvFile("customers.csv", new String[][]{
            {"id", "name", "email", "status"},
            {"1", "John Doe", "john@example.com", "ACTIVE"},
            {"2", "Jane Smith", "jane@example.com", "ACTIVE"},
            {"3", "Bob Johnson", "bob@example.com", "INACTIVE"}
        });

        // Create load-test-customers.csv for load tests
        createCsvFile("load-test-customers.csv", new String[][]{
            {"id", "name", "email", "status"},
            {"1", "John Doe", "john@example.com", "ACTIVE"},
            {"2", "Jane Smith", "jane@example.com", "ACTIVE"},
            {"3", "Bob Johnson", "bob@example.com", "INACTIVE"}
        });

        // Create load-db-test-customers.csv for database load tests
        createCsvFile("load-db-test-customers.csv", new String[][]{
            {"id", "name", "email", "status"},
            {"1", "John Doe", "john@example.com", "ACTIVE"},
            {"2", "Jane Smith", "jane@example.com", "ACTIVE"},
            {"3", "Bob Johnson", "bob@example.com", "INACTIVE"}
        });
    }

    private void createCsvFile(String filename, String[][] data) {
        Path csvFile = Paths.get("./demo-data/csv/" + filename);
        try (PrintWriter writer = new PrintWriter(new FileWriter(csvFile.toFile()))) {
            for (String[] row : data) {
                writer.println(String.join(",", row));
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to create CSV file: " + filename, e);
        }
    }

    private void createEmptyTestFile() {
        Path emptyFile = Paths.get("./demo-data/csv/empty-customers.csv");
        try (PrintWriter writer = new PrintWriter(new FileWriter(emptyFile.toFile()))) {
            writer.println("id,name,email,status"); // Header only
        } catch (IOException e) {
            throw new RuntimeException("Failed to create empty test CSV file", e);
        }
    }

    /**
     * Validate that data was correctly loaded into the H2 database.
     * Follows coding principles: validate actual business results, not YAML syntax.
     */
    private void validateDatabaseContents() throws Exception {
        logger.info("Validating database contents");

        // Connect to H2 database
        String jdbcUrl = "jdbc:h2:./target/test/etl/output/database/output_db";
        try (Connection conn = DriverManager.getConnection(jdbcUrl, "sa", "")) {
            Statement stmt = conn.createStatement();

            // Verify table exists
            ResultSet tables = conn.getMetaData().getTables(null, null, "CUSTOMER_OUTPUT", null);
            assertTrue(tables.next(), "Table 'customer_output' should exist in database");
            logger.info("✓ Table 'customer_output' exists");

            // Verify record count
            ResultSet countRs = stmt.executeQuery("SELECT COUNT(*) as cnt FROM customer_output");
            assertTrue(countRs.next(), "Should have count result");
            int recordCount = countRs.getInt("cnt");
            assertEquals(3, recordCount, "Should have loaded 3 customer records");
            logger.info("✓ Database contains {} records", recordCount);

            // Verify specific data values
            ResultSet dataRs = stmt.executeQuery(
                "SELECT id, name, email, status FROM customer_output ORDER BY id");

            // Verify record 1
            assertTrue(dataRs.next(), "Should have first record");
            assertEquals(1, dataRs.getInt("id"), "First record ID should be 1");
            assertEquals("John Doe", dataRs.getString("name"), "First record name should match");
            assertEquals("john@example.com", dataRs.getString("email"), "First record email should match");
            assertEquals("ACTIVE", dataRs.getString("status"), "First record status should match");

            // Verify record 2
            assertTrue(dataRs.next(), "Should have second record");
            assertEquals(2, dataRs.getInt("id"), "Second record ID should be 2");
            assertEquals("Jane Smith", dataRs.getString("name"), "Second record name should match");

            // Verify record 3
            assertTrue(dataRs.next(), "Should have third record");
            assertEquals(3, dataRs.getInt("id"), "Third record ID should be 3");
            assertEquals("Bob Johnson", dataRs.getString("name"), "Third record name should match");

            logger.info("✓ All {} records verified successfully", recordCount);
        }
    }
}
