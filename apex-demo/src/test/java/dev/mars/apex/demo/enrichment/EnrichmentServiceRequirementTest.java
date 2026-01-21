package dev.mars.apex.demo.enrichment;

import dev.mars.apex.core.config.yaml.YamlRuleConfiguration;
import dev.mars.apex.core.engine.config.RulesEngine;
import dev.mars.apex.core.engine.model.RuleResult;
import dev.mars.apex.demo.DemoTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

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

/**
 * EnrichmentServiceRequirementTest - Tests for enrichment functionality
 *
 * This test suite validates enrichment processing with inline datasets.
 * Modern APEX automatically handles enrichments via YamlEnrichmentProcessor,
 * no external service registration required for inline datasets.
 *
 * TESTS:
 * - Configuration with enrichments processes successfully
 * - Configuration without enrichments executes successfully
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-10-17
 * @version 2.0 - Updated for modern APEX architecture with automatic enrichment processing
 */
class EnrichmentServiceRequirementTest extends DemoTestBase {

    private static final Logger logger = LoggerFactory.getLogger(EnrichmentServiceRequirementTest.class);

    @Test
    @DisplayName("Configuration with enrichments processes successfully")
    void testConfigurationWithEnrichmentsAndServiceProvided() {
        logger.info("=== TEST: Enrichments with inline datasets ===");

        try {
            // Load configuration with enrichments
            YamlRuleConfiguration config = yamlLoader.loadFromFile(
                "src/test/java/dev/mars/apex/demo/enrichment/EnrichmentServiceRequirementTest.yaml");
            assertNotNull(config, "Configuration should load");
            logger.info("Configuration loaded with enrichments");

            // Create RulesEngine - enrichments handled automatically
            RulesEngine engine = new RulesEngine(rulesEngineConfiguration);
            logger.info("RulesEngine created - enrichments processed automatically");

            // Execute configuration
            Map<String, Object> testData = new HashMap<>();
            testData.put("transactionId", "TXN001");
            testData.put("customerId", "CUST001");
            testData.put("amount", 1000.0);

            RuleResult result = engine.evaluate(config, testData);
            assertNotNull(result, "Result should not be null");
            logger.info("Configuration executed successfully");
            logger.info("   Result: success={}, message={}", result.isSuccess(), result.getMessage());

        } catch (Exception e) {
            logger.error("Test failed: {}", e.getMessage(), e);
            fail("Test should not throw exception: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("Configuration without enrichments executes successfully")
    void testConfigurationWithoutEnrichmentsAndNoService() {
        logger.info("=== TEST: No enrichments defined ===");

        try {
            // Create empty configuration without enrichments
            YamlRuleConfiguration emptyConfig = new YamlRuleConfiguration();

            // Create RulesEngine - no enrichments to process
            RulesEngine engine = new RulesEngine(rulesEngineConfiguration);
            logger.info("RulesEngine created - no enrichments defined");

            // Execute configuration
            Map<String, Object> testData = new HashMap<>();
            testData.put("amount", 1000.0);

            RuleResult result = engine.evaluate(emptyConfig, testData);
            assertNotNull(result, "Result should not be null");
            assertTrue(result.isSuccess(), "Should succeed when no enrichments defined");
            logger.info("Configuration executed successfully without enrichments");
            logger.info("   Result: success={}", result.isSuccess());

        } catch (Exception e) {
            logger.error("Test failed: {}", e.getMessage(), e);
            fail("Test should not throw exception: " + e.getMessage());
        }
    }
}


