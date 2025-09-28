package dev.mars.apex.core.util;

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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for YAML stage validation functionality.
 * 
 * Tests the new processing-stages validation features including:
 * - Stage configuration validation
 * - Circular dependency detection
 * - Failure policy validation
 * - Stage uniqueness validation
 * - Backward compatibility with rule-configurations
 * 
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 1.0.0
 */
class YamlMetadataValidatorStageTest {

    @TempDir
    Path tempDir;
    
    private YamlMetadataValidator validator;
    
    @BeforeEach
    void setUp() {
        validator = new YamlMetadataValidator(tempDir.toString());
    }
    
    private void writeFile(String relativePath, String content) throws IOException {
        Path filePath = tempDir.resolve(relativePath);
        Files.createDirectories(filePath.getParent());
        Files.write(filePath, content.getBytes());
    }
    
    @Test
    void testValidStageBasedScenario() throws IOException {
        String validStageScenario = """
            metadata:
              id: "stage-scenario-001"
              name: "Valid Stage Scenario"
              version: "1.0.0"
              description: "A valid stage-based scenario"
              type: "scenario"
              business-domain: "Testing"
              owner: "test.team@company.com"
            
            scenario:
              scenario-id: "stage-test-scenario"
              data-types:
                - "TestDataType"
              processing-stages:
                - stage-name: "validation"
                  config-file: "config/validation-rules.yaml"
                  execution-order: 1
                  failure-policy: "terminate"
                  required: true
                  stage-metadata:
                    description: "Validation stage"
                    sla-ms: 500
                    
                - stage-name: "enrichment"
                  config-file: "config/enrichment-rules.yaml"
                  execution-order: 2
                  failure-policy: "continue-with-warnings"
                  depends-on: ["validation"]
                  stage-metadata:
                    description: "Enrichment stage"
                    sla-ms: 1000
            """;
        
        writeFile("scenarios/valid-stage-scenario.yaml", validStageScenario);
        
        YamlValidationResult result = validator.validateFile("scenarios/valid-stage-scenario.yaml");
        
        assertTrue(result.isValid(), "Valid stage scenario should pass validation. Errors: " + result.getErrors());
        assertEquals(0, result.getErrorCount());
        assertEquals("VALID", result.getStatus());
    }
    
    @Test
    void testLegacyRuleConfigurationsStillValid() throws IOException {
        String legacyScenario = """
            metadata:
              id: "legacy-scenario-001"
              name: "Legacy Rule Configuration Scenario"
              version: "1.0.0"
              description: "A legacy rule-configurations scenario"
              type: "scenario"
              business-domain: "Testing"
              owner: "test.team@company.com"
            
            scenario:
              scenario-id: "legacy-test-scenario"
              data-types:
                - "TestDataType"
              rule-configurations:
                - "config/validation-rules.yaml"
                - "config/enrichment-rules.yaml"
            """;
        
        writeFile("scenarios/legacy-scenario.yaml", legacyScenario);
        
        YamlValidationResult result = validator.validateFile("scenarios/legacy-scenario.yaml");
        
        assertTrue(result.isValid(), "Legacy scenario should still be valid. Errors: " + result.getErrors());
        assertEquals(0, result.getErrorCount());
        assertEquals("VALID", result.getStatus());
    }
    
    @Test
    void testMissingBothConfigurationsError() throws IOException {
        String invalidScenario = """
            metadata:
              id: "invalid-scenario-001"
              name: "Invalid Scenario"
              version: "1.0.0"
              description: "Scenario missing both configurations"
              type: "scenario"
              business-domain: "Testing"
              owner: "test.team@company.com"
            
            scenario:
              scenario-id: "invalid-test-scenario"
              data-types:
                - "TestDataType"
              # Missing both processing-stages and rule-configurations
            """;
        
        writeFile("scenarios/invalid-scenario.yaml", invalidScenario);
        
        YamlValidationResult result = validator.validateFile("scenarios/invalid-scenario.yaml");
        
        assertFalse(result.isValid(), "Scenario missing both configurations should fail validation");
        assertTrue(result.getErrors().stream().anyMatch(error -> 
            error.contains("Scenario must have either 'processing-stages' or 'rule-configurations'")));
    }
    
    @Test
    void testBothConfigurationsWarning() throws IOException {
        String warningScenario = """
            metadata:
              id: "warning-scenario-001"
              name: "Warning Scenario"
              version: "1.0.0"
              description: "Scenario with both configurations"
              type: "scenario"
              business-domain: "Testing"
              owner: "test.team@company.com"
            
            scenario:
              scenario-id: "warning-test-scenario"
              data-types:
                - "TestDataType"
              rule-configurations:
                - "config/legacy-rules.yaml"
              processing-stages:
                - stage-name: "validation"
                  config-file: "config/validation-rules.yaml"
                  execution-order: 1
            """;
        
        writeFile("scenarios/warning-scenario.yaml", warningScenario);
        
        YamlValidationResult result = validator.validateFile("scenarios/warning-scenario.yaml");
        
        assertTrue(result.isValid(), "Scenario with both configurations should be valid");
        assertTrue(result.hasWarnings(), "Should have warnings about both configurations");
        assertTrue(result.getWarnings().stream().anyMatch(warning -> 
            warning.contains("processing-stages' will take precedence")));
        assertEquals("VALID_WITH_WARNINGS", result.getStatus());
    }
    
    @Test
    void testEmptyProcessingStagesError() throws IOException {
        String emptyStagesScenario = """
            metadata:
              id: "empty-stages-001"
              name: "Empty Stages Scenario"
              version: "1.0.0"
              description: "Scenario with empty processing-stages"
              type: "scenario"
              business-domain: "Testing"
              owner: "test.team@company.com"
            
            scenario:
              scenario-id: "empty-stages-scenario"
              data-types:
                - "TestDataType"
              processing-stages: []
            """;
        
        writeFile("scenarios/empty-stages-scenario.yaml", emptyStagesScenario);
        
        YamlValidationResult result = validator.validateFile("scenarios/empty-stages-scenario.yaml");
        
        assertFalse(result.isValid(), "Empty processing-stages should fail validation");
        assertTrue(result.getErrors().stream().anyMatch(error -> 
            error.contains("processing-stages cannot be empty")));
    }
    
    @Test
    void testInvalidStageStructure() throws IOException {
        String invalidStageStructure = """
            metadata:
              id: "invalid-structure-001"
              name: "Invalid Stage Structure"
              version: "1.0.0"
              description: "Scenario with invalid stage structure"
              type: "scenario"
              business-domain: "Testing"
              owner: "test.team@company.com"
            
            scenario:
              scenario-id: "invalid-structure-scenario"
              data-types:
                - "TestDataType"
              processing-stages:
                - "not-a-map"
                - stage-name: "valid-stage"
                  config-file: "config/rules.yaml"
                  execution-order: 1
            """;
        
        writeFile("scenarios/invalid-structure-scenario.yaml", invalidStageStructure);
        
        YamlValidationResult result = validator.validateFile("scenarios/invalid-structure-scenario.yaml");
        
        assertFalse(result.isValid(), "Invalid stage structure should fail validation");
        assertTrue(result.getErrors().stream().anyMatch(error -> 
            error.contains("Processing stage 0 must be a map/object")));
    }
    
    @Test
    void testMissingRequiredStageFields() throws IOException {
        String missingFieldsScenario = """
            metadata:
              id: "missing-fields-001"
              name: "Missing Fields Scenario"
              version: "1.0.0"
              description: "Scenario with missing required stage fields"
              type: "scenario"
              business-domain: "Testing"
              owner: "test.team@company.com"
            
            scenario:
              scenario-id: "missing-fields-scenario"
              data-types:
                - "TestDataType"
              processing-stages:
                - stage-name: "incomplete-stage"
                  # Missing config-file and execution-order
                  failure-policy: "terminate"
            """;
        
        writeFile("scenarios/missing-fields-scenario.yaml", missingFieldsScenario);
        
        YamlValidationResult result = validator.validateFile("scenarios/missing-fields-scenario.yaml");
        
        assertFalse(result.isValid(), "Missing required stage fields should fail validation");
        assertTrue(result.getErrors().stream().anyMatch(error -> 
            error.contains("missing required field: config-file")));
        assertTrue(result.getErrors().stream().anyMatch(error -> 
            error.contains("missing required field: execution-order")));
    }
    
    @Test
    void testInvalidExecutionOrder() throws IOException {
        String invalidExecutionOrder = """
            metadata:
              id: "invalid-execution-order-001"
              name: "Invalid Execution Order"
              version: "1.0.0"
              description: "Scenario with invalid execution order"
              type: "scenario"
              business-domain: "Testing"
              owner: "test.team@company.com"
            
            scenario:
              scenario-id: "invalid-execution-order-scenario"
              data-types:
                - "TestDataType"
              processing-stages:
                - stage-name: "invalid-order-stage"
                  config-file: "config/rules.yaml"
                  execution-order: 0  # Invalid: must be positive
            """;
        
        writeFile("scenarios/invalid-execution-order-scenario.yaml", invalidExecutionOrder);
        
        YamlValidationResult result = validator.validateFile("scenarios/invalid-execution-order-scenario.yaml");
        
        assertFalse(result.isValid(), "Invalid execution order should fail validation");
        assertTrue(result.getErrors().stream().anyMatch(error -> 
            error.contains("execution-order must be a positive integer")));
    }
    
    @Test
    void testInvalidFailurePolicy() throws IOException {
        String invalidFailurePolicy = """
            metadata:
              id: "invalid-failure-policy-001"
              name: "Invalid Failure Policy"
              version: "1.0.0"
              description: "Scenario with invalid failure policy"
              type: "scenario"
              business-domain: "Testing"
              owner: "test.team@company.com"
            
            scenario:
              scenario-id: "invalid-failure-policy-scenario"
              data-types:
                - "TestDataType"
              processing-stages:
                - stage-name: "invalid-policy-stage"
                  config-file: "config/rules.yaml"
                  execution-order: 1
                  failure-policy: "invalid-policy"  # Invalid policy
            """;
        
        writeFile("scenarios/invalid-failure-policy-scenario.yaml", invalidFailurePolicy);
        
        YamlValidationResult result = validator.validateFile("scenarios/invalid-failure-policy-scenario.yaml");
        
        assertFalse(result.isValid(), "Invalid failure policy should fail validation");
        assertTrue(result.getErrors().stream().anyMatch(error ->
            error.contains("invalid failure-policy: invalid-policy")));
        assertTrue(result.getErrors().stream().anyMatch(error ->
            error.contains("Valid policies:")));
    }

    @Test
    void testDuplicateStageNames() throws IOException {
        String duplicateNamesScenario = """
            metadata:
              id: "duplicate-names-001"
              name: "Duplicate Stage Names"
              version: "1.0.0"
              description: "Scenario with duplicate stage names"
              type: "scenario"
              business-domain: "Testing"
              owner: "test.team@company.com"

            scenario:
              scenario-id: "duplicate-names-scenario"
              data-types:
                - "TestDataType"
              processing-stages:
                - stage-name: "validation"
                  config-file: "config/validation1.yaml"
                  execution-order: 1
                - stage-name: "validation"  # Duplicate name
                  config-file: "config/validation2.yaml"
                  execution-order: 2
            """;

        writeFile("scenarios/duplicate-names-scenario.yaml", duplicateNamesScenario);

        YamlValidationResult result = validator.validateFile("scenarios/duplicate-names-scenario.yaml");

        assertFalse(result.isValid(), "Duplicate stage names should fail validation");
        assertTrue(result.getErrors().stream().anyMatch(error ->
            error.contains("Duplicate stage name: validation")));
    }

    @Test
    void testDuplicateExecutionOrders() throws IOException {
        String duplicateOrdersScenario = """
            metadata:
              id: "duplicate-orders-001"
              name: "Duplicate Execution Orders"
              version: "1.0.0"
              description: "Scenario with duplicate execution orders"
              type: "scenario"
              business-domain: "Testing"
              owner: "test.team@company.com"

            scenario:
              scenario-id: "duplicate-orders-scenario"
              data-types:
                - "TestDataType"
              processing-stages:
                - stage-name: "validation"
                  config-file: "config/validation.yaml"
                  execution-order: 1
                - stage-name: "enrichment"
                  config-file: "config/enrichment.yaml"
                  execution-order: 1  # Duplicate order
            """;

        writeFile("scenarios/duplicate-orders-scenario.yaml", duplicateOrdersScenario);

        YamlValidationResult result = validator.validateFile("scenarios/duplicate-orders-scenario.yaml");

        assertFalse(result.isValid(), "Duplicate execution orders should fail validation");
        assertTrue(result.getErrors().stream().anyMatch(error ->
            error.contains("Duplicate execution order: 1")));
    }

    @Test
    void testCircularDependencies() throws IOException {
        String circularDependencyScenario = """
            metadata:
              id: "circular-dependency-001"
              name: "Circular Dependency Scenario"
              version: "1.0.0"
              description: "Scenario with circular dependencies"
              type: "scenario"
              business-domain: "Testing"
              owner: "test.team@company.com"

            scenario:
              scenario-id: "circular-dependency-scenario"
              data-types:
                - "TestDataType"
              processing-stages:
                - stage-name: "stage-a"
                  config-file: "config/stage-a.yaml"
                  execution-order: 1
                  depends-on: ["stage-b"]
                - stage-name: "stage-b"
                  config-file: "config/stage-b.yaml"
                  execution-order: 2
                  depends-on: ["stage-c"]
                - stage-name: "stage-c"
                  config-file: "config/stage-c.yaml"
                  execution-order: 3
                  depends-on: ["stage-a"]  # Creates circular dependency
            """;

        writeFile("scenarios/circular-dependency-scenario.yaml", circularDependencyScenario);

        YamlValidationResult result = validator.validateFile("scenarios/circular-dependency-scenario.yaml");

        assertFalse(result.isValid(), "Circular dependencies should fail validation");
        assertTrue(result.getErrors().stream().anyMatch(error ->
            error.contains("Circular dependency detected")));
    }

    @Test
    void testNonExistentDependency() throws IOException {
        String nonExistentDependencyScenario = """
            metadata:
              id: "non-existent-dependency-001"
              name: "Non-Existent Dependency Scenario"
              version: "1.0.0"
              description: "Scenario with non-existent dependency"
              type: "scenario"
              business-domain: "Testing"
              owner: "test.team@company.com"

            scenario:
              scenario-id: "non-existent-dependency-scenario"
              data-types:
                - "TestDataType"
              processing-stages:
                - stage-name: "validation"
                  config-file: "config/validation.yaml"
                  execution-order: 1
                - stage-name: "enrichment"
                  config-file: "config/enrichment.yaml"
                  execution-order: 2
                  depends-on: ["non-existent-stage"]  # References non-existent stage
            """;

        writeFile("scenarios/non-existent-dependency-scenario.yaml", nonExistentDependencyScenario);

        YamlValidationResult result = validator.validateFile("scenarios/non-existent-dependency-scenario.yaml");

        assertFalse(result.isValid(), "Non-existent dependency should fail validation");
        assertTrue(result.getErrors().stream().anyMatch(error ->
            error.contains("depends on non-existent stage: non-existent-stage")));
    }

    @Test
    void testValidComplexStageConfiguration() throws IOException {
        String complexValidScenario = """
            metadata:
              id: "complex-valid-001"
              name: "Complex Valid Stage Configuration"
              version: "1.0.0"
              description: "Complex but valid stage configuration"
              type: "scenario"
              business-domain: "Testing"
              owner: "test.team@company.com"

            scenario:
              scenario-id: "complex-valid-scenario"
              data-types:
                - "TestDataType"
              processing-stages:
                - stage-name: "validation"
                  config-file: "config/validation.yaml"
                  execution-order: 1
                  failure-policy: "terminate"
                  required: true
                  stage-metadata:
                    description: "Initial validation"
                    sla-ms: 500
                    critical: true

                - stage-name: "enrichment-basic"
                  config-file: "config/enrichment-basic.yaml"
                  execution-order: 2
                  failure-policy: "continue-with-warnings"
                  depends-on: ["validation"]
                  stage-metadata:
                    description: "Basic enrichment"
                    sla-ms: 1000

                - stage-name: "enrichment-advanced"
                  config-file: "config/enrichment-advanced.yaml"
                  execution-order: 3
                  failure-policy: "flag-for-review"
                  depends-on: ["enrichment-basic"]
                  stage-metadata:
                    description: "Advanced enrichment"
                    sla-ms: 2000

                - stage-name: "compliance"
                  config-file: "config/compliance.yaml"
                  execution-order: 4
                  failure-policy: "terminate"
                  depends-on: ["validation", "enrichment-basic"]
                  required: true
                  stage-metadata:
                    description: "Compliance checks"
                    sla-ms: 1500
                    critical: true
            """;

        writeFile("scenarios/complex-valid-scenario.yaml", complexValidScenario);

        YamlValidationResult result = validator.validateFile("scenarios/complex-valid-scenario.yaml");

        assertTrue(result.isValid(), "Complex valid scenario should pass validation. Errors: " + result.getErrors());
        assertEquals(0, result.getErrorCount());
        assertEquals("VALID", result.getStatus());
    }
}
