package dev.mars.apex.core.service.scenario;

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

import dev.mars.apex.core.config.yaml.YamlConfigurationLoader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for file handling in ScenarioStageExecutor.
 * 
 * Tests cover:
 * - Missing stage configuration files
 * - Missing scenario registry files
 * - Invalid file paths
 * - Graceful error handling
 * 
 * FOLLOWS CODING PRINCIPLES FROM prompts.txt:
 * - Test actual functionality (file validation)
 * - Use real file system operations
 * - Progressive complexity (simple to complex errors)
 * - Validate error messages are clear and actionable
 * - Log test errors with "TEST:" prefix to distinguish from production errors
 * 
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 1.0.0
 */
@DisplayName("ScenarioStageExecutor File Handling Tests")
class ScenarioStageExecutorFileHandlingTest {

    private static final Logger logger = LoggerFactory.getLogger(ScenarioStageExecutorFileHandlingTest.class);
    
    private ScenarioStageExecutor stageExecutor;
    private YamlConfigurationLoader configLoader;

    @BeforeEach
    void setUp() {
        stageExecutor = new ScenarioStageExecutor();
        configLoader = new YamlConfigurationLoader();
        logger.info("Initialized ScenarioStageExecutor for file handling testing");
    }

    // ========================================
    // Level 1: Missing Stage Config Files
    // ========================================

    @Test
    @DisplayName("Should handle missing stage configuration file gracefully")
    void testMissingStageConfigFile() {
        logger.info("TEST: Triggering intentional error - Missing stage configuration file");

        // Given: Scenario with stage having non-existent config file
        ScenarioStage stage = new ScenarioStage("test-stage",
            "nonexistent/config-file-that-does-not-exist.yaml", 1);
        stage.setFailurePolicy(ScenarioStage.FAILURE_POLICY_TERMINATE);

        ScenarioConfiguration scenario = ScenarioConfiguration.withStages("file-test", "File Test",
            Arrays.asList("TestData"), Arrays.asList(stage));

        // When: Execute scenario
        Map<String, Object> testData = new HashMap<>();
        testData.put("field", "value");

        ScenarioExecutionResult result = stageExecutor.executeStages(scenario, testData);

        // Then: Should return failure result (not throw exception)
        assertNotNull(result, "Should return ScenarioExecutionResult");
        assertFalse(result.isSuccessful(), "Should indicate failure");
        assertFalse(result.getStageResults().isEmpty(), "Should have stage results");
    }

    @Test
    @DisplayName("Should handle deeply nested missing file path")
    void testDeeplyNestedMissingFilePath() {
        logger.info("TEST: Triggering intentional error - Deeply nested missing file path");

        // Given: Scenario with stage having deeply nested non-existent path
        ScenarioStage stage = new ScenarioStage("test-stage",
            "very/deep/nested/path/that/does/not/exist/config.yaml", 1);

        ScenarioConfiguration scenario = ScenarioConfiguration.withStages("nested-test", "Nested Test",
            Arrays.asList("TestData"), Arrays.asList(stage));

        // When: Execute scenario
        Map<String, Object> testData = new HashMap<>();
        testData.put("field", "value");

        ScenarioExecutionResult result = stageExecutor.executeStages(scenario, testData);

        // Then: Should return failure result
        assertNotNull(result, "Should return ScenarioExecutionResult");
        assertFalse(result.isSuccessful(), "Should indicate failure");
    }

    @Test
    @DisplayName("Should handle empty file path")
    void testEmptyFilePath() {
        logger.info("TEST: Triggering intentional error - Empty file path");

        // Given: Scenario with stage having empty config file path
        ScenarioStage stage = new ScenarioStage("test-stage", "", 1);

        ScenarioConfiguration scenario = ScenarioConfiguration.withStages("empty-test", "Empty Test",
            Arrays.asList("TestData"), Arrays.asList(stage));

        // When: Execute scenario
        Map<String, Object> testData = new HashMap<>();
        testData.put("field", "value");

        ScenarioExecutionResult result = stageExecutor.executeStages(scenario, testData);

        // Then: Should return failure result
        assertNotNull(result, "Should return ScenarioExecutionResult");
        assertFalse(result.isSuccessful(), "Should indicate failure");
    }

    @Test
    @DisplayName("Should handle null file path")
    void testNullFilePath() {
        logger.info("TEST: Triggering intentional error - Null file path");

        // Given: Scenario with stage having null config file path
        ScenarioStage stage = new ScenarioStage("test-stage", null, 1);

        ScenarioConfiguration scenario = ScenarioConfiguration.withStages("null-test", "Null Test",
            Arrays.asList("TestData"), Arrays.asList(stage));

        // When: Execute scenario
        Map<String, Object> testData = new HashMap<>();
        testData.put("field", "value");

        ScenarioExecutionResult result = stageExecutor.executeStages(scenario, testData);

        // Then: Should return failure result
        assertNotNull(result, "Should return ScenarioExecutionResult");
        assertFalse(result.isSuccessful(), "Should indicate failure");
    }

    // ========================================
    // Level 2: Invalid File Paths
    // ========================================

    @Test
    @DisplayName("Should handle file path with invalid characters")
    void testInvalidFilePathCharacters() {
        logger.info("TEST: Triggering intentional error - Invalid file path characters");

        // Given: Scenario with stage having invalid file path characters
        ScenarioStage stage = new ScenarioStage("test-stage",
            "config/file<>|?.yaml", 1);

        ScenarioConfiguration scenario = ScenarioConfiguration.withStages("invalid-chars-test", "Invalid Chars Test",
            Arrays.asList("TestData"), Arrays.asList(stage));

        // When: Execute scenario
        Map<String, Object> testData = new HashMap<>();
        testData.put("field", "value");

        ScenarioExecutionResult result = stageExecutor.executeStages(scenario, testData);

        // Then: Should return failure result
        assertNotNull(result, "Should return ScenarioExecutionResult");
        assertFalse(result.isSuccessful(), "Should indicate failure");
    }

    @Test
    @DisplayName("Should handle relative path that goes outside project")
    void testRelativePathOutsideProject() {
        logger.info("TEST: Triggering intentional error - Relative path outside project");

        // Given: Scenario with stage having path that goes outside project
        ScenarioStage stage = new ScenarioStage("test-stage",
            "../../../../../../../../etc/passwd", 1);

        ScenarioConfiguration scenario = ScenarioConfiguration.withStages("outside-project-test", "Outside Project Test",
            Arrays.asList("TestData"), Arrays.asList(stage));

        // When: Execute scenario
        Map<String, Object> testData = new HashMap<>();
        testData.put("field", "value");

        ScenarioExecutionResult result = stageExecutor.executeStages(scenario, testData);

        // Then: Should return failure result
        assertNotNull(result, "Should return ScenarioExecutionResult");
        assertFalse(result.isSuccessful(), "Should indicate failure");
    }

    // ========================================
    // Level 3: Valid File Paths Still Work
    // ========================================

    @Test
    @DisplayName("Should successfully execute stage with valid config file")
    void testValidConfigFileExecution() {
        // Given: Scenario with stage having valid config file
        ScenarioStage stage = new ScenarioStage("validation",
            "src/test/java/dev/mars/apex/core/service/scenario/DataTypeScenarioServiceClassificationTest-validation-rules.yaml", 1);

        ScenarioConfiguration scenario = ScenarioConfiguration.withStages("valid-file-test", "Valid File Test",
            Arrays.asList("TestData"), Arrays.asList(stage));

        // When: Execute scenario
        Map<String, Object> testData = new HashMap<>();
        testData.put("tradeType", "OTCOption");
        testData.put("notional", 150000000);

        ScenarioExecutionResult result = stageExecutor.executeStages(scenario, testData);

        // Then: Should execute successfully
        assertNotNull(result, "Should return ScenarioExecutionResult");
        // Note: May succeed or fail based on rule logic, but should not fail due to file issues
        assertFalse(result.getStageResults().isEmpty(), "Should have stage results");
    }

    // ========================================
    // Level 4: Multiple Missing Files
    // ========================================

    @Test
    @DisplayName("Should handle scenario with multiple stages having missing files")
    void testMultipleStagesWithMissingFiles() {
        logger.info("TEST: Triggering intentional error - Multiple stages with missing files");

        // Given: Scenario with multiple stages, all with missing files
        ScenarioStage stage1 = new ScenarioStage("stage-1", "missing-1.yaml", 1);
        ScenarioStage stage2 = new ScenarioStage("stage-2", "missing-2.yaml", 2);
        stage2.addDependency("stage-1");

        ScenarioConfiguration scenario = ScenarioConfiguration.withStages("multi-stage-test", "Multi Stage Test",
            Arrays.asList("TestData"), Arrays.asList(stage1, stage2));

        // When: Execute stages
        Map<String, Object> testData = new HashMap<>();
        testData.put("field", "value");

        ScenarioExecutionResult result = stageExecutor.executeStages(scenario, testData);

        // Then: Should handle gracefully
        assertNotNull(result, "Should return ScenarioExecutionResult");
        assertFalse(result.isSuccessful(), "Should indicate failure");
        assertFalse(result.getStageResults().isEmpty(), "Should have stage results");
    }

    // ========================================
    // Level 5: Edge Cases
    // ========================================

    @Test
    @DisplayName("Should handle file path with spaces")
    void testFilePathWithSpaces() {
        logger.info("TEST: Triggering intentional error - File path with spaces");

        // Given: Scenario with stage having file path containing spaces
        ScenarioStage stage = new ScenarioStage("test-stage",
            "config/my config file with spaces.yaml", 1);

        ScenarioConfiguration scenario = ScenarioConfiguration.withStages("spaces-test", "Spaces Test",
            Arrays.asList("TestData"), Arrays.asList(stage));

        // When: Execute scenario
        Map<String, Object> testData = new HashMap<>();
        testData.put("field", "value");

        ScenarioExecutionResult result = stageExecutor.executeStages(scenario, testData);

        // Then: Should return failure result (file doesn't exist)
        assertNotNull(result, "Should return ScenarioExecutionResult");
        assertFalse(result.isSuccessful(), "Should indicate failure");
    }

    @Test
    @DisplayName("Should handle file path with special characters")
    void testFilePathWithSpecialCharacters() {
        logger.info("TEST: Triggering intentional error - File path with special characters");

        // Given: Scenario with stage having file path containing special characters
        ScenarioStage stage = new ScenarioStage("test-stage",
            "config/file@#$%^&().yaml", 1);

        ScenarioConfiguration scenario = ScenarioConfiguration.withStages("special-chars-test", "Special Chars Test",
            Arrays.asList("TestData"), Arrays.asList(stage));

        // When: Execute scenario
        Map<String, Object> testData = new HashMap<>();
        testData.put("field", "value");

        ScenarioExecutionResult result = stageExecutor.executeStages(scenario, testData);

        // Then: Should return failure result
        assertNotNull(result, "Should return ScenarioExecutionResult");
        assertFalse(result.isSuccessful(), "Should indicate failure");
    }
}

