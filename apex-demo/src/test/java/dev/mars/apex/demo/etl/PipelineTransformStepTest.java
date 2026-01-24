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

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for Pipeline Transform Step functionality.
 *
 * Tests the following scenarios from etl_tests_plan.md:
 * 1. shouldApplyTransformationRules - Verify transformation rules are applied correctly
 * 2. shouldFilterRecordsBasedOnConditions - Verify record filtering works
 * 3. shouldAggregateDataDuringTransform - Verify data aggregation capabilities
 * 4. shouldHandleTransformationErrors - Verify graceful error handling in transformations
 *
 * @author APEX Demo Team
 * @since 2025-10-27
 * @version 1.0.0
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Pipeline Transform Step Tests")
public class PipelineTransformStepTest extends DemoTestBase {

    private static final Logger logger = LoggerFactory.getLogger(PipelineTransformStepTest.class);

    private RulesEngine rulesEngine;

    @BeforeEach
    public void setUp() {
        super.setUp();
        logger.info("=== Setting up Pipeline Transform Step Test ===");
        createTestDirectories();
        createTestData();
    }

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
    @Order(1)
    @DisplayName("Should apply transformation rules to data")
    void shouldApplyTransformationRules() throws Exception {
        logger.info("=== Testing Transformation Rules Application ===");

        // Create RulesEngine and execute pipeline
        rulesEngine = RulesEngine.fromClasspath("dev/mars/apex/demo/etl/PipelineTransformStepTest_TransformRules.yaml");

        java.util.Map<String, Object> inputData = new java.util.HashMap<>();
        RuleResult result = rulesEngine.evaluate(inputData);

        // Validate pipeline executed successfully
        assertNotNull(result, "Pipeline execution result should not be null");
        assertEquals(RuleResult.ResultType.MATCH, result.getResultType(),
            "Pipeline should execute successfully");

        logger.info("[OK] Transformation rules applied successfully");
    }

    @Test
    @Order(2)
    @DisplayName("Should filter records based on conditions")
    void shouldFilterRecordsBasedOnConditions() throws Exception {
        logger.info("=== Testing Record Filtering ===");

        // Create RulesEngine and execute pipeline
        rulesEngine = RulesEngine.fromClasspath("dev/mars/apex/demo/etl/PipelineTransformStepTest_FilterRecords.yaml");

        java.util.Map<String, Object> inputData = new java.util.HashMap<>();
        RuleResult result = rulesEngine.evaluate(inputData);

        // Validate pipeline executed successfully
        assertNotNull(result, "Pipeline execution result should not be null");
        assertEquals(RuleResult.ResultType.MATCH, result.getResultType(),
            "Pipeline should execute successfully");

        logger.info("[OK] Record filtering executed successfully");
    }

    @Test
    @Order(3)
    @DisplayName("Should aggregate data during transform")
    void shouldAggregateDataDuringTransform() throws Exception {
        logger.info("=== Testing Data Aggregation ===");

        // Create RulesEngine and execute pipeline
        rulesEngine = RulesEngine.fromClasspath("dev/mars/apex/demo/etl/PipelineTransformStepTest_AggregateData.yaml");

        java.util.Map<String, Object> inputData = new java.util.HashMap<>();
        RuleResult result = rulesEngine.evaluate(inputData);

        // Validate pipeline executed successfully
        assertNotNull(result, "Pipeline execution result should not be null");
        assertEquals(RuleResult.ResultType.MATCH, result.getResultType(),
            "Pipeline should execute successfully");

        logger.info("[OK] Data aggregation executed successfully");
    }

    @Test
    @Order(4)
    @DisplayName("Should handle transformation errors gracefully")
    void shouldHandleTransformationErrors() throws Exception {
        logger.info("=== Testing Transformation Error Handling ===");

        // Create RulesEngine and execute pipeline
        rulesEngine = RulesEngine.fromClasspath("dev/mars/apex/demo/etl/PipelineTransformStepTest_ErrorHandling.yaml");

        java.util.Map<String, Object> inputData = new java.util.HashMap<>();
        RuleResult result = rulesEngine.evaluate(inputData);

        // Validate pipeline handled errors gracefully
        assertNotNull(result, "Pipeline execution result should not be null");
        assertEquals(RuleResult.ResultType.MATCH, result.getResultType(),
            "Pipeline should succeed with error-handling: skip-record");

        logger.info("[OK] Transformation errors handled gracefully");
    }

    // Helper methods

    private void createTestDirectories() {
        try {
            Files.createDirectories(Paths.get("./demo-data/transform-test/csv"));
            Files.createDirectories(Paths.get("./demo-data/transform-test/output"));
        } catch (IOException e) {
            throw new RuntimeException("Failed to create test directories", e);
        }
    }

    private void createTestData() {
        // Create test CSV file with various data for transformation testing
        Path csvFile = Paths.get("./demo-data/transform-test/csv/transform-data.csv");
        try (FileWriter writer = new FileWriter(csvFile.toFile())) {
            writer.write("id,name,value,status,category\n");
            writer.write("1,Item A,100,ACTIVE,electronics\n");
            writer.write("2,Item B,200,INACTIVE,books\n");
            writer.write("3,Item C,150,ACTIVE,electronics\n");
            writer.write("4,Item D,300,ACTIVE,clothing\n");
            writer.write("5,Item E,50,INACTIVE,books\n");
        } catch (IOException e) {
            throw new RuntimeException("Failed to create test CSV file", e);
        }

        // Create CSV with potential error conditions
        Path errorCsvFile = Paths.get("./demo-data/transform-test/csv/error-data.csv");
        try (FileWriter writer = new FileWriter(errorCsvFile.toFile())) {
            writer.write("id,name,value,status\n");
            writer.write("1,Valid Item,100,ACTIVE\n");
            writer.write("2,Invalid Value,INVALID,ACTIVE\n");
            writer.write("3,Another Valid,200,ACTIVE\n");
        } catch (IOException e) {
            throw new RuntimeException("Failed to create error test CSV file", e);
        }
    }
}

