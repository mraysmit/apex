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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for Phase 1: Rule Result References functionality.
 *
 * Tests the ability to reference individual rule results and rule group results
 * in field mappings for conditional processing.
 *
 * @author APEX Enhancement Team
 * @since 2025-09-17
 * @version 1.0
 */
@DisplayName("Rule Result References Test - Phase 1")
public class RuleResultReferencesTest extends DemoTestBase {

    private static final Logger logger = LoggerFactory.getLogger(RuleResultReferencesTest.class.getName());

    @Test
    @DisplayName("Should reference individual rule results in field mappings")
    void testBasicRuleResultReference() {
        logger.info("=== Testing Basic Rule Result Reference ===");

        try {
            // Load YAML configuration
            YamlRuleConfiguration config = yamlLoader.loadFromFile("src/test/java/dev/mars/apex/demo/conditional/rule-result-references-demo.yaml");
            assertNotNull(config, "Configuration should not be null");
            logger.info("✓ Configuration loaded successfully: " + config.getMetadata().getName());

            // Create test data that triggers high-value-rule
            Map<String, Object> inputData = new HashMap<>();
            inputData.put("amount", 15000.0);
            inputData.put("customerType", "STANDARD");
            inputData.put("priority", "NORMAL");

            logger.info("Input data: " + inputData);

            // Process using real APEX services
            Object result = enrichmentService.enrichObject(config, inputData);
            assertNotNull(result, "Enrichment result should not be null");
        
        @SuppressWarnings("unchecked")
        Map<String, Object> enrichedData = (Map<String, Object>) result;
        
            // Verify that high-value rule triggered and processing amount was calculated
            assertTrue(enrichedData.containsKey("processedAmount"), "Should contain processedAmount field");
            Double processedAmount = (Double) enrichedData.get("processedAmount");
            assertEquals(15750.0, processedAmount, 0.01, "Processed amount should be 15000 * 1.05 = 15750");

            logger.info("✓ Basic rule result reference working: processedAmount = " + processedAmount);

        } catch (Exception e) {
            logger.error("Test failed: " + e.getMessage(), e);
            fail("Test failed: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("Should process rule result references correctly")
    void testRuleResultTracking() {
        logger.info("=== Testing Rule Result References (Phase 1) ===");

        try {
            // Load configuration from classpath
            YamlRuleConfiguration config = yamlLoader.loadFromFile("src/test/java/dev/mars/apex/demo/conditional/rule-result-references-demo.yaml");
            assertNotNull(config, "Configuration should not be null");
            logger.info("✓ Configuration loaded successfully");

            // Create test data that should trigger the high-value-rule
            Map<String, Object> inputData = new HashMap<>();
            inputData.put("amount", 15000.0);  // Should trigger high-value-rule (condition: #amount > 10000)
            inputData.put("customerType", "STANDARD");
            inputData.put("priority", "NORMAL");

            logger.info("Input data: " + inputData);

            // Process using real APEX services
            Object result = enrichmentService.enrichObject(config, inputData);
            assertNotNull(result, "Enrichment result should not be null");

            @SuppressWarnings("unchecked")
            Map<String, Object> enrichedData = (Map<String, Object>) result;

            // Validate rule result references are working

            // 1. Basic rule result reference - should process because high-value-rule=true
            assertTrue(enrichedData.containsKey("processedAmount"), "Should contain processedAmount field");
            Double processedAmount = (Double) enrichedData.get("processedAmount");
            assertEquals(15750.0, processedAmount, 0.01, "Processed amount should be 15750.0 (15000 * 1.05)");

            // 2. Rule group result reference - should process because validation-group passed=true
            assertTrue(enrichedData.containsKey("validationStatus"), "Should contain validationStatus field");
            assertEquals("VALIDATED", enrichedData.get("validationStatus"), "Validation status should be VALIDATED");

            // 3. Complex conditional mapping - should process because rule results are not null
            assertTrue(enrichedData.containsKey("processingPriority"), "Should contain processingPriority field");
            assertEquals("HIGH", enrichedData.get("processingPriority"), "Processing priority should be HIGH");

            assertTrue(enrichedData.containsKey("serviceLevel"), "Should contain serviceLevel field");
            assertEquals("ENHANCED", enrichedData.get("serviceLevel"), "Service level should be ENHANCED");

            // 4. Conditional with fallback - should always process (condition: true)
            assertTrue(enrichedData.containsKey("finalStatus"), "Should contain finalStatus field");
            assertEquals("APPROVED", enrichedData.get("finalStatus"), "Final status should be APPROVED");

            assertTrue(enrichedData.containsKey("notificationLevel"), "Should contain notificationLevel field");
            assertEquals("LOW", enrichedData.get("notificationLevel"), "Notification level should be LOW");

            logger.info("✓ All rule result references working correctly!");
            logger.info("✓ Final enriched data: " + enrichedData);

        } catch (Exception e) {
            logger.error("Test failed: " + e.getMessage(), e);
            fail("Test failed: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("Should handle premium customer scenario correctly")
    void testPremiumCustomerScenario() {
        logger.info("=== Testing Premium Customer Scenario ===");

        try {
            YamlRuleConfiguration config = yamlLoader.loadFromFile("src/test/java/dev/mars/apex/demo/conditional/rule-result-references-demo.yaml");

            // Create test data for premium customer
            Map<String, Object> inputData = new HashMap<>();
            inputData.put("amount", 5000.0);  // Below high-value threshold
            inputData.put("customerType", "PREMIUM");  // Should trigger premium-customer-rule
            inputData.put("priority", "HIGH");

            Object result = enrichmentService.enrichObject(config, inputData);
            @SuppressWarnings("unchecked")
            Map<String, Object> enrichedData = (Map<String, Object>) result;

            // Premium customer should get MEDIUM notification level
            assertEquals("MEDIUM", enrichedData.get("notificationLevel"),
                        "Premium customer should get MEDIUM notification level");

            // Service level should be PREMIUM for premium customers
            assertEquals("PREMIUM", enrichedData.get("serviceLevel"),
                        "Premium customer should get PREMIUM service level");

            logger.info("✓ Premium customer scenario working correctly!");

        } catch (Exception e) {
            logger.error("Premium customer test failed: " + e.getMessage(), e);
            fail("Premium customer test failed: " + e.getMessage());
        }
    }
}
