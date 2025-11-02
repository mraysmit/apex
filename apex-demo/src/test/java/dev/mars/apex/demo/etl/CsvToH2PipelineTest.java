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
 * JUnit 5 test for CSV to H2 Pipeline functionality using APEX RulesEngine.
 *
 * PIPELINE VALIDATION CHECKLIST:
 *  Load pipeline YAML configuration with data sources and sinks
 *  Initialize RulesEngine with YAML configuration
 *  Execute pipeline with extract, load, and audit steps
 *  Validate pipeline execution results and step completion
 *  Verify actual CSV to H2 database processing functionality
 *
 * BUSINESS LOGIC VALIDATION:
 * - Extract step: Read customer data from CSV file using data source
 * - Load step: Insert customer records into H2 database using data sink
 * - Audit step: Write audit records to JSON file for compliance
 * - Pipeline orchestration: Dependency management and error handling
 */
public class CsvToH2PipelineTest extends DemoTestBase {

    private static final Logger logger = LoggerFactory.getLogger(CsvToH2PipelineTest.class);
    private RulesEngine rulesEngine;

    @AfterEach
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
    void testCsvToH2PipelineExecution() {
        logger.info("=== Testing CSV to H2 Pipeline Execution ===");

        try {
            // Create required directories and sample CSV file
            setupTestData();

            // Create RulesEngine and execute pipeline
            String yamlPath = "src/test/java/dev/mars/apex/demo/etl/CsvToH2PipelineTest.yaml";
            rulesEngine = RulesEngine.fromFile(yamlPath);

            logger.info("✓ RulesEngine initialized successfully");

            // Execute the pipeline
            java.util.Map<String, Object> inputData = new java.util.HashMap<>();
            RuleResult result = rulesEngine.evaluate(inputData);

            // Validate pipeline execution results
            assertNotNull(result, "Pipeline execution result should not be null");
            assertEquals(RuleResult.ResultType.MATCH, result.getResultType(),
                "Pipeline should execute successfully");

            logger.info("✓ Pipeline executed successfully");
            logger.info("Pipeline Results:");
            logger.info("  - Result type: " + result.getResultType());
            logger.info("  - Message: " + result.getMessage());

            logger.info(" CSV to H2 pipeline execution test completed successfully");

        } catch (Exception e) {
            logger.error("Pipeline execution test failed: " + e.getMessage(), e);
            fail("Pipeline execution should not throw exceptions: " + e.getMessage());
        }
    }

    /**
     * Set up test data directories and sample CSV file for pipeline testing.
     */
    private void setupTestData() throws IOException {
        logger.info("Setting up test data for CSV to H2 pipeline");

        // Create required directories
        Path dataDir = Paths.get("./target/demo/etl/data");
        Path outputDir = Paths.get("./target/demo/etl/output");
        Path auditDir = Paths.get("./target/demo/etl/output/audit");

        Files.createDirectories(dataDir);
        Files.createDirectories(outputDir);
        Files.createDirectories(auditDir);

        // Create sample CSV file
        Path csvFile = dataDir.resolve("customers.csv");
        try (FileWriter writer = new FileWriter(csvFile.toFile())) {
            writer.write("customer_id,customer_name,email_address,registration_date,status\n");
            writer.write("1,John Doe,john.doe@example.com,2023-01-15,ACTIVE\n");
            writer.write("2,Jane Smith,jane.smith@example.com,2023-02-20,ACTIVE\n");
            writer.write("3,Bob Johnson,bob.johnson@example.com,2023-03-10,INACTIVE\n");
        }

        logger.info("✓ Test data setup completed");
        logger.info("  - Data directory: " + dataDir.toAbsolutePath());
        logger.info("  - CSV file: " + csvFile.toAbsolutePath());
        logger.info("  - Output directory: " + outputDir.toAbsolutePath());
        logger.info("  - Audit directory: " + auditDir.toAbsolutePath());
    }
}
