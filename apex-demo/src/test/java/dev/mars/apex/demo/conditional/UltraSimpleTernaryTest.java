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
 * Ultra Simple Ternary Test - EVEN SIMPLER than rule groups!
 * 
 * Achieves the same result with just 1 enrichment using ternary operators:
 * - No rules needed
 * - No rule groups needed  
 * - Just pure SpEL conditional logic
 * - Same sequential evaluation: A->FIRST, B->SECOND, C->THIRD
 */
@DisplayName("Ultra Simple Ternary Test")
public class UltraSimpleTernaryTest extends DemoTestBase {

    @Test
    @DisplayName("Should apply FIRST when input=A")
    void testInputA() {
        logger.info("=== Testing Ternary: input='A' -> output='FIRST' ===");
        
        try {
            Map<String, Object> testData = new HashMap<>();
            testData.put("input", "A");

            YamlRuleConfiguration config = yamlLoader.loadFromFile("src/test/java/dev/mars/apex/demo/conditional/ultra-simple-ternary-test.yaml");
            Object enrichmentResult = enrichmentService.enrichObject(config, testData);
            @SuppressWarnings("unchecked")
            Map<String, Object> result = (Map<String, Object>) enrichmentResult;

            assertEquals("FIRST", result.get("output"), "Should map A to FIRST");
            logger.info("✅ Ternary A test passed: " + result);
        } catch (Exception e) {
            logger.error("Test failed: " + e.getMessage(), e);
            fail("Test failed: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("Should apply SECOND when input=B")
    void testInputB() {
        logger.info("=== Testing Ternary: input='B' -> output='SECOND' ===");
        
        try {
            Map<String, Object> testData = new HashMap<>();
            testData.put("input", "B");

            YamlRuleConfiguration config = yamlLoader.loadFromFile("src/test/java/dev/mars/apex/demo/conditional/ultra-simple-ternary-test.yaml");
            Object enrichmentResult = enrichmentService.enrichObject(config, testData);
            @SuppressWarnings("unchecked")
            Map<String, Object> result = (Map<String, Object>) enrichmentResult;

            assertEquals("SECOND", result.get("output"), "Should map B to SECOND");
            logger.info("✅ Ternary B test passed: " + result);
        } catch (Exception e) {
            logger.error("Test failed: " + e.getMessage(), e);
            fail("Test failed: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("Should apply THIRD when input=C")
    void testInputC() {
        logger.info("=== Testing Ternary: input='C' -> output='THIRD' ===");
        
        try {
            Map<String, Object> testData = new HashMap<>();
            testData.put("input", "C");

            YamlRuleConfiguration config = yamlLoader.loadFromFile("src/test/java/dev/mars/apex/demo/conditional/ultra-simple-ternary-test.yaml");
            Object enrichmentResult = enrichmentService.enrichObject(config, testData);
            @SuppressWarnings("unchecked")
            Map<String, Object> result = (Map<String, Object>) enrichmentService.enrichObject(config, testData);

            assertEquals("THIRD", result.get("output"), "Should map C to THIRD");
            logger.info("✅ Ternary C test passed: " + result);
        } catch (Exception e) {
            logger.error("Test failed: " + e.getMessage(), e);
            fail("Test failed: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("Should apply null when input=X (no match)")
    void testInputX() {
        logger.info("=== Testing Ternary: input='X' -> output=null ===");
        
        try {
            Map<String, Object> testData = new HashMap<>();
            testData.put("input", "X");

            YamlRuleConfiguration config = yamlLoader.loadFromFile("src/test/java/dev/mars/apex/demo/conditional/ultra-simple-ternary-test.yaml");
            Object enrichmentResult = enrichmentService.enrichObject(config, testData);
            @SuppressWarnings("unchecked")
            Map<String, Object> result = (Map<String, Object>) enrichmentResult;

            assertNull(result.get("output"), "Should be null for no match");
            logger.info("✅ Ternary X test passed: " + result);
        } catch (Exception e) {
            logger.error("Test failed: " + e.getMessage(), e);
            fail("Test failed: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("Should validate configuration simplicity")
    void testConfigurationSimplicity() {
        logger.info("=== Testing Configuration Simplicity ===");
        
        try {
            YamlRuleConfiguration config = yamlLoader.loadFromFile("src/test/java/dev/mars/apex/demo/conditional/ultra-simple-ternary-test.yaml");
            
            assertNotNull(config, "Configuration should load");
            assertEquals("Ultra Simple Ternary Test", config.getMetadata().getName());
            
            // Prove simplicity
            assertTrue(config.getRules() == null || config.getRules().isEmpty(), "Should have NO rules");
            assertTrue(config.getRuleGroups() == null || config.getRuleGroups().isEmpty(), "Should have NO rule groups");
            assertEquals(1, config.getEnrichments().size(), "Should have only 1 enrichment");
            
            logger.info("✅ Configuration simplicity validated - NO rules, NO rule groups, just 1 enrichment!");
        } catch (Exception e) {
            logger.error("Test failed: " + e.getMessage(), e);
            fail("Test failed: " + e.getMessage());
        }
    }
}
