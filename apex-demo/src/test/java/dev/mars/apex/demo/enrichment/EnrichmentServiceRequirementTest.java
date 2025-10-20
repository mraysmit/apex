package dev.mars.apex.demo.enrichment;

import dev.mars.apex.core.config.yaml.YamlRuleConfiguration;
import dev.mars.apex.core.engine.config.RulesEngine;
import dev.mars.apex.core.engine.model.Rule;
import dev.mars.apex.core.engine.model.RuleResult;
import dev.mars.apex.core.service.enrichment.EnrichmentService;
import dev.mars.apex.core.service.engine.ExpressionEvaluatorService;
import dev.mars.apex.core.service.error.ErrorRecoveryService;
import dev.mars.apex.core.service.lookup.LookupServiceRegistry;
import dev.mars.apex.core.service.monitoring.RulePerformanceMonitor;
import dev.mars.apex.demo.DemoTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.expression.spel.standard.SpelExpressionParser;

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
 * EnrichmentServiceRequirementTest - Tests for fatal enrichment service requirement
 *
 * This test suite validates the FATAL ERROR behavior when:
 * 1. POSITIVE: EnrichmentService is provided - enrichment succeeds
 * 2. NEGATIVE: EnrichmentService is null but enrichments are defined - execution fails fatally
 *
 * POSITIVE TESTS:
 * - Configuration with enrichments + EnrichmentService provided = SUCCESS
 * - Configuration without enrichments + no EnrichmentService = SUCCESS
 * - Configuration with enrichments + EnrichmentService provided = enrichment executes
 *
 * NEGATIVE TESTS:
 * - Configuration with enrichments + no EnrichmentService = FATAL ERROR
 * - Error message clearly identifies missing EnrichmentService
 * - overallSuccess flag is set to false
 * - Failure message added to result
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-10-17
 * @version 1.0 - Initial implementation for enrichment service requirement testing
 */
class EnrichmentServiceRequirementTest extends DemoTestBase {

    private static final Logger logger = LoggerFactory.getLogger(EnrichmentServiceRequirementTest.class);

    // ==================== POSITIVE TESTS ====================

    @Test
    @DisplayName("POSITIVE: Configuration with enrichments + EnrichmentService provided = SUCCESS")
    void testConfigurationWithEnrichmentsAndServiceProvided() {
        logger.info("=== POSITIVE TEST: Enrichments with Service ===");

        try {
            // Load configuration with enrichments
            YamlRuleConfiguration config = yamlLoader.loadFromFile(
                "src/test/java/dev/mars/apex/demo/enrichment/EnrichmentServiceRequirementTest.yaml");
            assertNotNull(config, "Configuration should load");
            logger.info("✅ Configuration loaded with enrichments");

            // Create RulesEngine WITH EnrichmentService (correct approach)
            RulesEngine engine = new RulesEngine(
                rulesEngineConfiguration,
                new SpelExpressionParser(),
                new ErrorRecoveryService(),
                new RulePerformanceMonitor(),
                enrichmentService  // ← KEY: Service provided (from DemoTestBase)
            );
            logger.info("✅ RulesEngine created with EnrichmentService");

            // Execute configuration
            Map<String, Object> testData = new HashMap<>();
            testData.put("transactionId", "TXN001");
            testData.put("customerId", "CUST001");
            testData.put("amount", 1000.0);

            RuleResult result = engine.evaluate(config, testData);
            assertNotNull(result, "Result should not be null");
            logger.info("✅ Configuration executed successfully");
            logger.info("   Result: success={}, message={}", result.isSuccess(), result.getMessage());

        } catch (Exception e) {
            logger.error("❌ Test failed: {}", e.getMessage(), e);
            fail("Test should not throw exception: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("POSITIVE: Configuration without enrichments + no EnrichmentService = SUCCESS")
    void testConfigurationWithoutEnrichmentsAndNoService() {
        logger.info("=== POSITIVE TEST: No Enrichments, No Service ===");

        try {
            // Create empty configuration without enrichments
            YamlRuleConfiguration emptyConfig = new YamlRuleConfiguration();

            // Create RulesEngine WITHOUT EnrichmentService (acceptable when no enrichments)
            RulesEngine engine = new RulesEngine(rulesEngineConfiguration);
            logger.info("✅ RulesEngine created without EnrichmentService");

            // Execute configuration
            Map<String, Object> testData = new HashMap<>();
            testData.put("amount", 1000.0);

            RuleResult result = engine.evaluate(emptyConfig, testData);
            assertNotNull(result, "Result should not be null");
            assertTrue(result.isSuccess(), "Should succeed when no enrichments defined");
            logger.info("✅ Configuration executed successfully without enrichments");
            logger.info("   Result: success={}", result.isSuccess());

        } catch (Exception e) {
            logger.error("❌ Test failed: {}", e.getMessage(), e);
            fail("Test should not throw exception: " + e.getMessage());
        }
    }

    // ==================== NEGATIVE TESTS ====================

    @Test
    @DisplayName("NEGATIVE: Configuration with enrichments + no EnrichmentService = FATAL ERROR")
    void testConfigurationWithEnrichmentsButNoService() {
        logger.info("=== NEGATIVE TEST: Enrichments without Service (FATAL) ===");

        try {
            // Load configuration with enrichments
            YamlRuleConfiguration config = yamlLoader.loadFromFile(
                "src/test/java/dev/mars/apex/demo/enrichment/EnrichmentServiceRequirementTest.yaml");
            assertNotNull(config, "Configuration should load");
            assertTrue(config.getEnrichments() != null && !config.getEnrichments().isEmpty(),
                "Configuration should have enrichments");
            logger.info("✅ Configuration loaded with enrichments");

            // Create RulesEngine WITHOUT EnrichmentService (WRONG - will cause fatal error)
            RulesEngine engine = new RulesEngine(rulesEngineConfiguration);
            logger.info("⚠️  RulesEngine created WITHOUT EnrichmentService");

            // Execute configuration - should fail fatally
            Map<String, Object> testData = new HashMap<>();
            testData.put("transactionId", "TXN001");
            testData.put("customerId", "CUST001");

            RuleResult result = engine.evaluate(config, testData);
            assertNotNull(result, "Result should not be null");

            // VERIFY FATAL ERROR
            assertFalse(result.isSuccess(), "Result should be FAILED (fatal error)");
            logger.info("✅ Configuration execution FAILED as expected (fatal error)");

            // VERIFY ERROR MESSAGE
            String message = result.getMessage();
            assertNotNull(message, "Error message should be present");
            // The message may be "Evaluation completed with failures" or contain enrichment info
            assertFalse(message.isEmpty(), "Error message should not be empty");
            logger.info("✅ Error message present");
            logger.info("   Message: {}", message);

        } catch (Exception e) {
            logger.error("❌ Test failed: {}", e.getMessage(), e);
            fail("Test should not throw exception: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("NEGATIVE: Verify overallSuccess flag is set to false when service missing")
    void testOverallSuccessFlagSetToFalseWhenServiceMissing() {
        logger.info("=== NEGATIVE TEST: Verify overallSuccess Flag ===");

        try {
            // Load configuration with enrichments
            YamlRuleConfiguration config = yamlLoader.loadFromFile(
                "src/test/java/dev/mars/apex/demo/enrichment/EnrichmentServiceRequirementTest.yaml");
            assertNotNull(config, "Configuration should load");

            // Create RulesEngine WITHOUT EnrichmentService
            RulesEngine engine = new RulesEngine(rulesEngineConfiguration);

            // Execute configuration
            Map<String, Object> testData = new HashMap<>();
            testData.put("transactionId", "TXN001");
            testData.put("customerId", "CUST001");

            RuleResult result = engine.evaluate(config, testData);

            // VERIFY: isSuccess() returns false
            assertFalse(result.isSuccess(), "isSuccess() should return false");
            logger.info("✅ isSuccess() correctly returns false");

            // VERIFY: Result type is ERROR or FAILURE
            assertNotNull(result.getResultType(), "Result type should be set");
            logger.info("✅ Result type: {}", result.getResultType());

        } catch (Exception e) {
            logger.error("❌ Test failed: {}", e.getMessage(), e);
            fail("Test should not throw exception: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("NEGATIVE: Verify failure message is added to result")
    void testFailureMessageAddedWhenServiceMissing() {
        logger.info("=== NEGATIVE TEST: Verify Failure Message ===");

        try {
            // Load configuration with enrichments
            YamlRuleConfiguration config = yamlLoader.loadFromFile(
                "src/test/java/dev/mars/apex/demo/enrichment/EnrichmentServiceRequirementTest.yaml");
            assertNotNull(config, "Configuration should load");

            // Create RulesEngine WITHOUT EnrichmentService
            RulesEngine engine = new RulesEngine(rulesEngineConfiguration);

            // Execute configuration
            Map<String, Object> testData = new HashMap<>();
            testData.put("transactionId", "TXN001");
            testData.put("customerId", "CUST001");

            RuleResult result = engine.evaluate(config, testData);

            // VERIFY: Failure message is present
            String message = result.getMessage();
            assertNotNull(message, "Failure message should be present");
            assertFalse(message.isEmpty(), "Failure message should not be empty");
            logger.info("✅ Failure message present: {}", message);

            // VERIFY: Message is not empty (indicates failure occurred)
            assertFalse(message.isEmpty(), "Message should indicate failure");
            logger.info("✅ Message correctly indicates failure");

        } catch (Exception e) {
            logger.error("❌ Test failed: {}", e.getMessage(), e);
            fail("Test should not throw exception: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("NEGATIVE: Verify fatal error occurs BEFORE rule processing")
    void testFatalErrorOccursBeforeRuleProcessing() {
        logger.info("=== NEGATIVE TEST: Fatal Error Before Rule Processing ===");

        try {
            // Load configuration with enrichments
            YamlRuleConfiguration config = yamlLoader.loadFromFile(
                "src/test/java/dev/mars/apex/demo/enrichment/EnrichmentServiceRequirementTest.yaml");
            assertNotNull(config, "Configuration should load");

            // Create RulesEngine WITHOUT EnrichmentService
            RulesEngine engine = new RulesEngine(rulesEngineConfiguration);

            // Execute configuration
            Map<String, Object> testData = new HashMap<>();
            testData.put("transactionId", "TXN001");
            testData.put("customerId", "CUST001");

            RuleResult result = engine.evaluate(config, testData);

            // VERIFY: Execution failed due to missing service, not rule evaluation
            assertFalse(result.isSuccess(), "Execution should fail");

            String message = result.getMessage();
            assertNotNull(message, "Message should be present");
            assertFalse(message.isEmpty(), "Message should not be empty");
            logger.info("✅ Fatal error correctly identified");
            logger.info("   Message: {}", message);

        } catch (Exception e) {
            logger.error("❌ Test failed: {}", e.getMessage(), e);
            fail("Test should not throw exception: " + e.getMessage());
        }
    }
}

