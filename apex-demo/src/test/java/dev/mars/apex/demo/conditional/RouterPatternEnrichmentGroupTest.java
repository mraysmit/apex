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

import dev.mars.apex.core.config.yaml.ProcessingItem;
import dev.mars.apex.core.config.yaml.YamlRuleConfiguration;
import dev.mars.apex.core.engine.config.RulesEngine;
import dev.mars.apex.core.engine.model.RuleResult;
import dev.mars.apex.demo.DemoTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for the "Router" pattern with enrichment-group-references.
 */
@DisplayName("Router Pattern Enrichment Group Test")
public class RouterPatternEnrichmentGroupTest extends DemoTestBase {

    private static final Logger logger = LoggerFactory.getLogger(RouterPatternEnrichmentGroupTest.class.getName());

    @Test
    @DisplayName("Should route correctly and execute enrichment groups")
    void testRouterPatternWithEnrichmentGroups() {
        logger.info("=== Testing Router Pattern (Enrichment Groups) ===");

        try {
            // Load YAML configuration
            YamlRuleConfiguration config = yamlLoader.loadFromFile("src/test/java/dev/mars/apex/demo/conditional/RouterPatternEnrichmentGroupTest.yaml");
            assertNotNull(config, "Configuration should not be null");
            
            // CRITICAL: Filter itemOrder to ONLY include rule-chains items.
            // This prevents enrichments and enrichment-groups from being executed globally
            // by the engine, ensuring they are only executed if the Router triggers them.
            List<ProcessingItem> ruleChainsOnly = config.getItemOrder().stream()
                    .filter(item -> "rule-chains".equals(item.getSectionType()))
                    .toList();
            config.setItemOrder(ruleChainsOnly);
            
            logger.info("[OK] Configuration loaded successfully");

            RulesEngine engine = RulesEngine.fromYamlConfig(config);

            // Scenario 1: MANUAL_REVIEW
            logger.info("--- Scenario 1: MANUAL_REVIEW ---");
            Map<String, Object> manualInput = new HashMap<>();
            manualInput.put("processingPath", "MANUAL_REVIEW");
            logger.info("Input Data: {}", manualInput);

            RuleResult manualResult = engine.evaluate(config, manualInput);
            Map<String, Object> manualEnriched = manualResult.getEnrichedData();
            logger.info("Result Data: {}", manualEnriched);

            assertTrue(manualEnriched.containsKey("reviewStatus"), "Should have reviewStatus field");
            assertEquals(true, manualEnriched.get("reviewStatus"), "Review status flag should be true");
            assertTrue(manualEnriched.containsKey("supervisorNotified"), "Should have supervisorNotified field");
            
            // Verify that AUTO_APPROVE enrichments did NOT run
            assertFalse(manualEnriched.containsKey("approvalStatus"), "Should NOT have approvalStatus field");
            
            logger.info("[OK] Scenario 1 passed: Correctly routed to MANUAL_REVIEW");


            // Scenario 2: AUTO_APPROVE
            logger.info("--- Scenario 2: AUTO_APPROVE ---");
            Map<String, Object> approveInput = new HashMap<>();
            approveInput.put("processingPath", "AUTO_APPROVE");
            logger.info("Input Data: {}", approveInput);

            RuleResult approveResult = engine.evaluate(config, approveInput);
            Map<String, Object> approveEnriched = approveResult.getEnrichedData();
            logger.info("Result Data: {}", approveEnriched);

            assertTrue(approveEnriched.containsKey("approvalStatus"), "Should have approvalStatus field");
            assertEquals(true, approveEnriched.get("approvalStatus"), "Approval status flag should be true");
            assertTrue(approveEnriched.containsKey("confirmationSent"), "Should have confirmationSent field");
            
            // Verify that MANUAL_REVIEW enrichments did NOT run
            assertFalse(approveEnriched.containsKey("reviewStatus"), "Should NOT have reviewStatus field");
            
            logger.info("[OK] Scenario 2 passed: Correctly routed to AUTO_APPROVE");

        } catch (Exception e) {
            logger.error("Test failed: " + e.getMessage(), e);
            fail("Test failed: " + e.getMessage());
        }
    }
}
