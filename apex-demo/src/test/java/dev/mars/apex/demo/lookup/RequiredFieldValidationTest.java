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

package dev.mars.apex.demo.lookup;


import dev.mars.apex.demo.infrastructure.DemoTestBase;
import dev.mars.apex.core.engine.model.RuleResult;
import dev.mars.apex.core.config.yaml.YamlRuleConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Required Field Validation Test")
class RequiredFieldValidationTest extends DemoTestBase {

    private static final Logger logger = LoggerFactory.getLogger(RequiredFieldValidationTest.class);

    @Test
    @DisplayName("Test required field works when field exists")
    void testRequiredFieldExists() {
        try {
            var config = yamlLoader.loadFromFile("src/test/java/dev/mars/apex/demo/lookup/RequiredFieldValidationTest.yaml");
            assertNotNull(config, "YAML configuration should not be null");

            Map<String, Object> inputData = new HashMap<>();
            inputData.put("id", "1");

            // Use complete APEX evaluation workflow
            RuleResult result = testEvaluation(config, inputData);
            assertNotNull(result, "RuleResult should not be null when required field exists");

            // Check APEX rule result status
            assertTrue(result.isSuccess(), "APEX evaluation should succeed when required field exists");
            assertFalse(result.hasFailures(), "Should have no failures when required field exists");
            assertTrue(result.getFailureMessages().isEmpty(), "Should have no failure messages when successful");

            // Verify enriched data contains the mapped field
            Map<String, Object> enrichedData = result.getEnrichedData();
            assertNotNull(enrichedData, "Enriched data should not be null");
            assertEquals("Test1", enrichedData.get("resultName"), "Required field should be mapped successfully");

            logger.info(" Required field test passed: {}", enrichedData.get("resultName"));
            logger.info(" APEX evaluation successful: isSuccess={}, hasFailures={}",
                       result.isSuccess(), result.hasFailures());

        } catch (Exception e) {
            logger.error(" Test failed", e);
            fail("Test failed: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("Test required field fails when field missing")
    void testRequiredFieldMissing() {
        try {
            var config = yamlLoader.loadFromFile("src/test/java/dev/mars/apex/demo/lookup/RequiredFieldValidationTest.yaml");
            assertNotNull(config, "YAML configuration should not be null");

            Map<String, Object> inputData = new HashMap<>();
            inputData.put("id", "999"); // Non-existent ID that will cause lookup failure

            // Use complete APEX evaluation workflow
            RuleResult result = testEvaluation(config, inputData);
            assertNotNull(result, "RuleResult should not be null even when required field fails");

            // Check APEX rule result status - should fail due to required field missing
            assertFalse(result.isSuccess(), "APEX evaluation should fail when required field is missing");
            assertTrue(result.hasFailures(), "Should have failures when required field is missing");
            assertFalse(result.getFailureMessages().isEmpty(), "Should have failure messages when required field fails");

            // Verify failure message contains information about the missing required field
            String failureMessage = result.getFailureMessages().get(0);
            assertTrue(failureMessage.contains("Required field"), "Failure message should mention required field");

            // Verify enriched data preserves original input but doesn't have the required field
            Map<String, Object> enrichedData = result.getEnrichedData();
            assertNotNull(enrichedData, "Enriched data should not be null");
            assertEquals("999", enrichedData.get("id"), "Original input data should be preserved");
            assertNull(enrichedData.get("resultName"), "Required field should be null when lookup fails");

            logger.info(" Required field validation test passed - field correctly null: {}", enrichedData.get("resultName"));
            logger.info(" APEX system correctly reported failure: isSuccess={}, hasFailures={}",
                       result.isSuccess(), result.hasFailures());
            logger.info(" Failure message: {}", failureMessage);

        } catch (Exception e) {
            logger.error(" Test failed", e);
            fail("Test failed: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("Test required field validation behavior")
    void testRequiredFieldValidationBehavior() {
        try {
            var config = yamlLoader.loadFromFile("src/test/java/dev/mars/apex/demo/lookup/RequiredFieldValidationTest.yaml");
            assertNotNull(config, "YAML configuration should not be null");

            // Test 1: Successful lookup
            Map<String, Object> successData = new HashMap<>();
            successData.put("id", "2");

            RuleResult successResult = testEvaluation(config, successData);
            assertTrue(successResult.isSuccess(), "Should succeed when required field exists");
            assertFalse(successResult.hasFailures(), "Should have no failures for successful lookup");
            assertEquals("Test2", successResult.getEnrichedData().get("resultName"), "Should successfully map existing field");

            // Test 2: Failed lookup with required field
            Map<String, Object> failData = new HashMap<>();
            failData.put("id", "nonexistent");

            RuleResult failResult = testEvaluation(config, failData);
            assertFalse(failResult.isSuccess(), "Should fail when required field is missing");
            assertTrue(failResult.hasFailures(), "Should have failures for missing required field");
            assertFalse(failResult.getFailureMessages().isEmpty(), "Should have failure messages");
            assertNull(failResult.getEnrichedData().get("resultName"), "Should return null for missing required field");

            logger.info(" Required field validation behavior test completed successfully");
            logger.info(" APEX system correctly handles both success and failure cases");
            logger.info(" Success case: isSuccess={}, hasFailures={}",
                       successResult.isSuccess(), successResult.hasFailures());
            logger.info(" Failure case: isSuccess={}, hasFailures={}, failureCount={}",
                       failResult.isSuccess(), failResult.hasFailures(), failResult.getFailureMessages().size());

        } catch (Exception e) {
            logger.error(" Test failed", e);
            fail("Test failed: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("Test comprehensive RuleResult API methods")
    void testRuleResultApiMethods() {
        try {
            var config = yamlLoader.loadFromFile("src/test/java/dev/mars/apex/demo/lookup/RequiredFieldValidationTest.yaml");
            assertNotNull(config, "YAML configuration should not be null");

            // Test successful case
            Map<String, Object> successData = new HashMap<>();
            successData.put("id", "1");

            RuleResult successResult = testEvaluation(config, successData);

            // Demonstrate all new RuleResult API methods
            logger.info("=== Demonstrating RuleResult API Methods ===");
            logger.info("result.isSuccess(): {}", successResult.isSuccess());
            logger.info("result.hasFailures(): {}", successResult.hasFailures());
            logger.info("result.getFailureMessages(): {}", successResult.getFailureMessages());
            logger.info("result.getEnrichedData(): {}", successResult.getEnrichedData());

            // Verify API methods work correctly for success case
            assertTrue(successResult.isSuccess(), "isSuccess() should return true for successful enrichment");
            assertFalse(successResult.hasFailures(), "hasFailures() should return false for successful enrichment");
            assertTrue(successResult.getFailureMessages().isEmpty(), "getFailureMessages() should return empty list for success");
            assertNotNull(successResult.getEnrichedData(), "getEnrichedData() should return enriched data map");
            assertEquals("Test1", successResult.getEnrichedData().get("resultName"), "Enriched data should contain mapped field");

            // Test failure case
            Map<String, Object> failData = new HashMap<>();
            failData.put("id", "999");

            RuleResult failResult = testEvaluation(config, failData);

            logger.info("=== Failure Case API Methods ===");
            logger.info("result.isSuccess(): {}", failResult.isSuccess());
            logger.info("result.hasFailures(): {}", failResult.hasFailures());
            logger.info("result.getFailureMessages(): {}", failResult.getFailureMessages());
            logger.info("result.getEnrichedData(): {}", failResult.getEnrichedData());

            // Verify API methods work correctly for failure case
            assertFalse(failResult.isSuccess(), "isSuccess() should return false for failed enrichment");
            assertTrue(failResult.hasFailures(), "hasFailures() should return true for failed enrichment");
            assertFalse(failResult.getFailureMessages().isEmpty(), "getFailureMessages() should contain failure details");
            assertNotNull(failResult.getEnrichedData(), "getEnrichedData() should still return data map even on failure");
            assertNull(failResult.getEnrichedData().get("resultName"), "Failed required field should be null in enriched data");

            logger.info(" All RuleResult API methods working correctly");
            logger.info(" APEX system provides complete programmatic access to rule results");

        } catch (Exception e) {
            logger.error(" Test failed", e);
            fail("Test failed: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("Should handle mixed required field mappings correctly")
    void shouldHandleMixedRequiredFieldMappings() {
        logger.info("=== Testing Mixed Required Field Mappings ===");

        // Create a YAML configuration with multiple field mappings:
        // - One required field that will be missing
        // - One optional field that will be missing
        // - One required field that will be present
        String mixedRequiredYaml = """
            metadata:
              id: "mixed-required-field-test"
              name: "Mixed Required Field Test"

            enrichments:
              - id: "mixed-required-test"
                type: "lookup-enrichment"
                enabled: true

                lookup-config:
                  lookup-key: "#id"
                  lookup-dataset:
                    type: "inline"
                    key-field: "id"
                    data:
                      - id: 1
                        name: "Test1"
                        category: "CategoryA"
                        # Note: description is missing
                      - id: 2
                        description: "Test2 Description"
                        category: "CategoryB"
                        # Note: name is missing (required field!)
                      - id: 3
                        name: "Test3"
                        description: "Test3 Description"
                        category: "CategoryC"
                        # All fields present

                field-mappings:
                  - source-field: "name"
                    target-field: "resultName"
                    required: true
                  - source-field: "description"
                    target-field: "resultDescription"
                    required: false
                  - source-field: "category"
                    target-field: "resultCategory"
                    required: true
            """;

        try {
            // Test case 1: Missing optional field only (should succeed)
            logger.info("--- Test Case 1: Missing optional field only ---");
            YamlRuleConfiguration config1 = yamlLoader.fromYamlString(mixedRequiredYaml);
            Map<String, Object> data1 = Map.of("id", 1);
            RuleResult result1 = testEvaluation(config1, data1);

            assertTrue(result1.isSuccess(), "Should succeed when only optional field is missing");
            assertFalse(result1.hasFailures(), "Should have no failures when only optional field is missing");
            assertEquals("Test1", result1.getEnrichedData().get("resultName"), "Required name field should be present");
            assertEquals("CategoryA", result1.getEnrichedData().get("resultCategory"), "Required category field should be present");
            assertNull(result1.getEnrichedData().get("resultDescription"), "Optional description field should be null");

            // Test case 2: Missing required field (should fail)
            logger.info("--- Test Case 2: Missing required field ---");
            YamlRuleConfiguration config2 = yamlLoader.fromYamlString(mixedRequiredYaml);
            Map<String, Object> data2 = Map.of("id", 2);
            RuleResult result2 = testEvaluation(config2, data2);

            assertFalse(result2.isSuccess(), "Should fail when required field is missing");
            assertTrue(result2.hasFailures(), "Should have failures when required field is missing");
            assertFalse(result2.getFailureMessages().isEmpty(), "Should have failure messages");

            // Test case 3: All fields present (should succeed)
            logger.info("--- Test Case 3: All fields present ---");
            YamlRuleConfiguration config3 = yamlLoader.fromYamlString(mixedRequiredYaml);
            Map<String, Object> data3 = Map.of("id", 3);
            RuleResult result3 = testEvaluation(config3, data3);

            assertTrue(result3.isSuccess(), "Should succeed when all fields are present");
            assertFalse(result3.hasFailures(), "Should have no failures when all fields are present");
            assertEquals("Test3", result3.getEnrichedData().get("resultName"), "Required name field should be present");
            assertEquals("Test3 Description", result3.getEnrichedData().get("resultDescription"), "Optional description field should be present");
            assertEquals("CategoryC", result3.getEnrichedData().get("resultCategory"), "Required category field should be present");

            logger.info(" Mixed required field mappings test completed successfully");
            logger.info(" APEX system correctly handles required vs optional field distinctions");

        } catch (Exception e) {
            logger.error(" Mixed required field test failed", e);
            fail("Mixed required field test failed: " + e.getMessage());
        }
    }
}
