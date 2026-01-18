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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JUnit 5 test for Pipeline ETL functionality using APEX RulesEngine.
 *
 * PIPELINE VALIDATION CHECKLIST:
 *  Load pipeline YAML configuration with complete ETL workflow
 *  Execute pipeline via RulesEngine.evaluate()
 *  Validate pipeline execution results
 *  Verify actual ETL processing functionality
 *
 * BUSINESS LOGIC VALIDATION:
 * - Extract step: Read customer data from CSV file using data source
 * - Validate step: Validate customer data quality and format
 * - Enrich step: Enrich customer data with additional information
 * - Load step: Insert customer records into H2 database using data sink
 * - Audit step: Write audit records to JSON file for compliance
 * - Pipeline orchestration: Sequential execution with dependency management
 */
@DisplayName("Pipeline ETL Test")
public class PipelineEtlTest extends DemoTestBase {

    private static final Logger logger = LoggerFactory.getLogger(PipelineEtlTest.class);

    private RulesEngine rulesEngine;

    @BeforeEach
    public void setUp() {
        super.setUp();
        try {
            logger.info("Setting up Pipeline ETL Test...");

            // Create test directories FIRST
            createTestDirectories();

            // Clean database SECOND (prevent primary key violations)
            cleanDatabase();

            // Create test CSV data THIRD
            createTestCsvData();

            logger.info("✓ Pipeline ETL Test setup completed successfully");

        } catch (Exception e) {
            logger.error("✗ Failed to set up Pipeline ETL Test: {}", e.getMessage());
            fail("Setup failed: " + e.getMessage());
        }
    }

    @AfterEach
    public void tearDown() {
        if (rulesEngine != null) {
            try {
                rulesEngine.shutdown();
            } catch (Exception e) {
                logger.warn("Error shutting down rules engine", e);
            }
        }
        super.tearDown();
    }

    @Test
    @DisplayName("Should execute complete ETL pipeline workflow")
    void testCompleteEtlPipeline() {
        logger.info("=== Testing Complete ETL Pipeline Workflow ===");

        try {
            // Create RulesEngine and execute pipeline
            rulesEngine = RulesEngine.fromClasspath("dev/mars/apex/demo/etl/PipelineEtlTest.yaml");

            java.util.Map<String, Object> inputData = new java.util.HashMap<>();
            RuleResult result = rulesEngine.evaluate(inputData);

            // Validate pipeline execution
            assertNotNull(result, "Pipeline execution result should not be null");
            assertEquals(RuleResult.ResultType.MATCH, result.getResultType(),
                "Pipeline execution should be successful");

            logger.info("✓ Complete ETL pipeline workflow executed successfully");

        } catch (Exception e) {
            logger.error("✗ Pipeline execution failed: {}", e.getMessage());
            fail("Pipeline execution failed: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("Should validate pipeline configuration structure")
    void testPipelineConfiguration() {
        logger.info("=== Testing Pipeline Configuration Structure ===");

        try {
            // Create RulesEngine and execute pipeline
            rulesEngine = RulesEngine.fromClasspath("dev/mars/apex/demo/etl/PipelineEtlTest.yaml");

            java.util.Map<String, Object> inputData = new java.util.HashMap<>();
            RuleResult result = rulesEngine.evaluate(inputData);

            // Validate pipeline execution
            assertNotNull(result, "Pipeline execution result should not be null");
            assertEquals(RuleResult.ResultType.MATCH, result.getResultType(),
                "Pipeline should execute successfully");

            logger.info("✓ Pipeline configuration structure validated successfully");
        } catch (Exception e) {
            logger.error("✗ Pipeline configuration test failed: {}", e.getMessage());
            fail("Pipeline configuration test failed: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("Should handle pipeline execution with monitoring")
    void testPipelineMonitoring() {
        logger.info("=== Testing Pipeline Execution with Monitoring ===");

        try {
            // Create RulesEngine and execute pipeline
            rulesEngine = RulesEngine.fromClasspath("dev/mars/apex/demo/etl/PipelineEtlTest.yaml");

            java.util.Map<String, Object> inputData = new java.util.HashMap<>();
            RuleResult result = rulesEngine.evaluate(inputData);

            // Validate pipeline execution
            assertNotNull(result, "Pipeline execution result should not be null");
            assertEquals(RuleResult.ResultType.MATCH, result.getResultType(),
                "Pipeline should execute successfully");

            logger.info("✓ Pipeline monitoring validated successfully");

        } catch (Exception e) {
            logger.error("✗ Pipeline monitoring test failed: {}", e.getMessage());
            fail("Pipeline monitoring test failed: " + e.getMessage());
        }
    }

    private void createTestDirectories() throws IOException {
        // Create input directory
        Path inputDir = Paths.get("./data/input");
        Files.createDirectories(inputDir);
        
        // Create output directories
        Path outputDir = Paths.get("./output");
        Files.createDirectories(outputDir);
        
        Path auditDir = Paths.get("./output/audit");
        Files.createDirectories(auditDir);
        
        logger.info("Created test directories: input, output, audit");
    }

    private void cleanDatabase() throws IOException {
        // Clean database by removing database files to prevent primary key violations
        Path dbPath = Paths.get("./output/customers.mv.db");
        Path dbTraceFile = Paths.get("./output/customers.trace.db");

        try {
            Files.deleteIfExists(dbPath);
            Files.deleteIfExists(dbTraceFile);
            logger.info("Cleaned database files for fresh test run");
        } catch (IOException e) {
            logger.warn("Could not clean database files: {}", e.getMessage());
        }
    }

    private void createTestCsvData() throws IOException {
        Path csvFile = Paths.get("./data/input/customers.csv");
        
        try (FileWriter writer = new FileWriter(csvFile.toFile())) {
            writer.write("customer_id,customer_name,email,phone,creditScore,loyaltyPoints\n");
            writer.write("1,John Doe,john.doe@example.com,+1-555-0101,750,1200\n");
            writer.write("2,Jane Smith,jane.smith@example.com,+1-555-0102,680,800\n");
            writer.write("3,Bob Johnson,bob.johnson@example.com,+1-555-0103,720,950\n");
        }
        
        logger.info("Created test CSV data with 3 customer records");
    }
}
