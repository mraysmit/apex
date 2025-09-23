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
package dev.mars.apex.demo.conditional;

import dev.mars.apex.core.config.yaml.YamlRuleConfiguration;
import dev.mars.apex.demo.DemoTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Ultra Simple Rule OR Test - Demonstrates rule groups with OR logic
 * 
 * Tests 3 rules in sequence with OR logic where each rule maps a different 
 * constant to the same target field. First matching rule wins.
 * 
 * Key Features:
 * - Rule group with OR operator
 * - Sequential rule evaluation (A->FIRST, B->SECOND, C->THIRD)
 * - Rule result references in enrichments
 * - First-match-wins processing
 */
@DisplayName("Ultra Simple Rule OR Test")
public class UltraSimpleRuleOrTest extends DemoTestBase {

    @Test
    @DisplayName("Should apply FIRST when input=A (first rule match)")
    void testInputA() {
        logger.info("=== Testing Rule OR: input='A' -> output='FIRST' ===");
        
        try {
            Map<String, Object> testData = new HashMap<>();
            testData.put("input", "A");

            YamlRuleConfiguration config = yamlLoader.loadFromFile("src/test/java/dev/mars/apex/demo/conditional/ultra-simple-rule-or-test.yaml");
            Object enrichmentResult = enrichmentService.enrichObject(config, testData);
            @SuppressWarnings("unchecked")
            Map<String, Object> result = (Map<String, Object>) enrichmentResult;

            assertEquals("FIRST", result.get("output"), "Should map A to FIRST");
            logger.info("✅ Rule OR A test passed: " + result);
        } catch (Exception e) {
            logger.error("Test failed: " + e.getMessage(), e);
            fail("Test failed: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("Should apply SECOND when input=B (second rule match)")
    void testInputB() {
        logger.info("=== Testing Rule OR: input='B' -> output='SECOND' ===");
        
        try {
            Map<String, Object> testData = new HashMap<>();
            testData.put("input", "B");

            YamlRuleConfiguration config = yamlLoader.loadFromFile("src/test/java/dev/mars/apex/demo/conditional/ultra-simple-rule-or-test.yaml");
            Object enrichmentResult = enrichmentService.enrichObject(config, testData);
            @SuppressWarnings("unchecked")
            Map<String, Object> result = (Map<String, Object>) enrichmentResult;

            assertEquals("SECOND", result.get("output"), "Should map B to SECOND");
            logger.info("✅ Rule OR B test passed: " + result);
        } catch (Exception e) {
            logger.error("Test failed: " + e.getMessage(), e);
            fail("Test failed: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("Should apply THIRD when input=C (third rule match)")
    void testInputC() {
        logger.info("=== Testing Rule OR: input='C' -> output='THIRD' ===");
        
        try {
            Map<String, Object> testData = new HashMap<>();
            testData.put("input", "C");

            YamlRuleConfiguration config = yamlLoader.loadFromFile("src/test/java/dev/mars/apex/demo/conditional/ultra-simple-rule-or-test.yaml");
            Object enrichmentResult = enrichmentService.enrichObject(config, testData);
            @SuppressWarnings("unchecked")
            Map<String, Object> result = (Map<String, Object>) enrichmentResult;

            assertEquals("THIRD", result.get("output"), "Should map C to THIRD");
            logger.info("✅ Rule OR C test passed: " + result);
        } catch (Exception e) {
            logger.error("Test failed: " + e.getMessage(), e);
            fail("Test failed: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("Should have no output when input=X (no rule match)")
    void testInputX() {
        logger.info("=== Testing Rule OR: input='X' -> no output (no match) ===");
        
        try {
            Map<String, Object> testData = new HashMap<>();
            testData.put("input", "X");

            YamlRuleConfiguration config = yamlLoader.loadFromFile("src/test/java/dev/mars/apex/demo/conditional/ultra-simple-rule-or-test.yaml");
            Object enrichmentResult = enrichmentService.enrichObject(config, testData);
            @SuppressWarnings("unchecked")
            Map<String, Object> result = (Map<String, Object>) enrichmentResult;

            assertNull(result.get("output"), "Should have no output when no rules match");
            logger.info("✅ Rule OR X test passed: " + result);
        } catch (Exception e) {
            logger.error("Test failed: " + e.getMessage(), e);
            fail("Test failed: " + e.getMessage());
        }
    }
}
