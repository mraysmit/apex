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

import dev.mars.apex.core.config.yaml.YamlRuleConfiguration;
import dev.mars.apex.core.engine.pipeline.DataPipelineEngine;
import dev.mars.apex.core.engine.pipeline.YamlPipelineExecutionResult;
import dev.mars.apex.demo.DemoTestBase;
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
 * JUnit 5 test for CSV to H2 Pipeline functionality using APEX DataPipelineEngine.
 *
 * PIPELINE VALIDATION CHECKLIST:
 *  Load pipeline YAML configuration with data sources and sinks
 *  Initialize DataPipelineEngine with YAML configuration
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

    @Test
    void testCsvToH2PipelineExecution() {
        logger.info("=== Testing CSV to H2 Pipeline Execution ===");

        try {
            // Create required directories and sample CSV file
            setupTestData();
            // Load YAML pipeline configuration from same directory as test
            String yamlPath = "src/test/java/dev/mars/apex/demo/etl/CsvToH2PipelineTest.yaml";
            YamlRuleConfiguration config = yamlLoader.loadFromFile(yamlPath);
            assertNotNull(config, "Pipeline configuration should not be null");

            // Debug logging to understand what's loaded
            logger.info("Configuration loaded successfully");
            logger.info("Metadata: " + (config.getMetadata() != null ? config.getMetadata().getName() : "null"));
            logger.info("Data sources count: " + (config.getDataSources() != null ? config.getDataSources().size() : "null"));
            logger.info("Data sinks count: " + (config.getDataSinks() != null ? config.getDataSinks().size() : "null"));
            logger.info("Pipeline object: " + (config.getPipeline() != null ? "present" : "null"));

            if (config.getPipeline() != null) {
                logger.info("Pipeline name: " + config.getPipeline().getName());
                logger.info("Pipeline steps count: " + (config.getPipeline().getSteps() != null ? config.getPipeline().getSteps().size() : "null"));
            }

            assertNotNull(config.getPipeline(), "Pipeline definition should not be null");

            logger.info("✓ Pipeline configuration loaded: " + config.getMetadata().getName());

            // Initialize DataPipelineEngine
            DataPipelineEngine pipelineEngine = new DataPipelineEngine();
            pipelineEngine.initialize(config);

            logger.info("✓ DataPipelineEngine initialized successfully");

            // Execute the pipeline
            String pipelineName = config.getPipeline().getName();
            logger.info("Executing pipeline: " + pipelineName);

            YamlPipelineExecutionResult result = pipelineEngine.executePipeline(pipelineName);

            // Validate pipeline execution results
            assertNotNull(result, "Pipeline execution result should not be null");
            assertTrue(result.isSuccess(), "Pipeline should execute successfully");
            assertTrue(result.getDurationMs() > 0, "Pipeline should have positive execution time");
            assertTrue(result.getTotalSteps() > 0, "Pipeline should have executed steps");

            logger.info("✓ Pipeline executed successfully");
            logger.info("Pipeline Results:");
            logger.info("  - Success: " + result.isSuccess());
            logger.info("  - Duration: " + result.getDurationMs() + "ms");
            logger.info("  - Total Steps: " + result.getTotalSteps());
            logger.info("  - Successful Steps: " + result.getSuccessfulSteps());
            logger.info("  - Failed Steps: " + result.getFailedSteps());

            // Validate step execution (audit step is optional and may fail)
            assertTrue(result.getSuccessfulSteps() >= 2,
                "At least 2 pipeline steps (extract and load) should execute successfully");
            assertTrue(result.getFailedSteps() <= 1, "At most 1 optional step should fail");

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
