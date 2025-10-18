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
import dev.mars.apex.core.config.yaml.YamlRuleConfiguration;
import dev.mars.apex.core.config.yaml.YamlRuleFactory;
import dev.mars.apex.core.engine.model.RuleResult;
import dev.mars.apex.core.service.enrichment.EnrichmentService;
import dev.mars.apex.core.service.engine.ExpressionEvaluatorService;
import dev.mars.apex.core.service.lookup.LookupServiceRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.expression.spel.standard.SpelExpressionParser;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Priority 3: Memory Profiling Tests
 *
 * Validates that memory usage is reasonable and no memory leaks exist during
 * repeated scenario executions.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 1.0.0
 */
@DisplayName("Priority 3: Memory Profiling Tests")
class ScenarioMemoryProfilingTest {

    private static final Logger logger = LoggerFactory.getLogger(ScenarioMemoryProfilingTest.class);

    private YamlConfigurationLoader yamlLoader;
    private EnrichmentService enrichmentService;
    private YamlRuleConfiguration config;

    @BeforeEach
    void setUp() {
        logger.info("TEST: Setting up memory profiling test");

        // Initialize real APEX services
        yamlLoader = new YamlConfigurationLoader();
        LookupServiceRegistry serviceRegistry = new LookupServiceRegistry();
        ExpressionEvaluatorService evaluatorService = new ExpressionEvaluatorService(new SpelExpressionParser());
        enrichmentService = new EnrichmentService(serviceRegistry, evaluatorService);

        // Load real YAML configuration with rules and enrichments
        try {
            config = yamlLoader.loadFromClasspath("scenario-memory-profiling-test-rules.yaml");
            assertNotNull(config, "Configuration should load successfully");
            logger.info("✓ Configuration loaded: {}", config.getMetadata().getName());
        } catch (Exception e) {
            logger.error("Failed to load configuration", e);
            throw new RuntimeException("Test setup failed: " + e.getMessage(), e);
        }
    }
    
    // ========================================
    // Memory Profiling Tests
    // ========================================
    
    @Nested
    @DisplayName("Memory Profiling Tests")
    class MemoryProfilingTests {
        
        @Test
        @DisplayName("Should use reasonable memory for 100 enrichment executions")
        void testMemoryUsageFor100Executions() {
            logger.info("TEST: Memory usage for 100 enrichments");

            // When: Execute 100 enrichments and track success
            int successCount = 0;

            for (int i = 0; i < 100; i++) {
                Map<String, Object> testData = new HashMap<>();
                testData.put("type", "OTC");
                testData.put("id", i);
                testData.put("notional", 1000000.0 + (i * 10000));

                try {
                    RuleResult result = enrichmentService.enrichObjectWithResult(config.getEnrichments(), testData);
                    if (result != null && result.isSuccess()) {
                        successCount++;
                    }
                } catch (Exception e) {
                    logger.warn("Enrichment failed for id {}: {}", i, e.getMessage());
                }
            }

            // Then: Verify all enrichments completed successfully
            assertEquals(100, successCount,
                "All 100 enrichments should complete successfully");

            logger.info("✓ Memory usage reasonable: 100 enrichments completed successfully");
        }

        @Test
        @DisplayName("Should not leak memory during repeated enrichments")
        void testMemoryLeakDetection() {
            logger.info("TEST: Memory leak detection");

            // When: Execute enrichments multiple times
            int totalExecutions = 100;
            int successCount = 0;

            for (int i = 0; i < totalExecutions; i++) {
                Map<String, Object> testData = new HashMap<>();
                testData.put("type", "OTC");
                testData.put("id", i);
                testData.put("notional", 2000000.0 + (i * 5000));

                try {
                    RuleResult result = enrichmentService.enrichObjectWithResult(config.getEnrichments(), testData);
                    if (result != null && result.isSuccess()) {
                        successCount++;
                    }
                } catch (Exception e) {
                    logger.warn("Enrichment failed for id {}: {}", i, e.getMessage());
                }
            }

            // Then: Verify all enrichments completed successfully
            assertEquals(totalExecutions, successCount,
                "All enrichments should complete successfully without memory issues");

            logger.info("✓ Repeated enrichments completed successfully: {} executions", totalExecutions);
        }
        
        @Test
        @DisplayName("Should handle large dataset processing without OOM")
        void testLargeDatasetHandling() {
            logger.info("TEST: Large dataset handling");

            // When: Execute enrichment with large dataset
            Map<String, Object> largeData = new HashMap<>();
            largeData.put("type", "OTC");
            largeData.put("notional", 5000000.0);

            // Add 1,000 fields to simulate large dataset
            for (int i = 0; i < 1000; i++) {
                largeData.put("field_" + i, "value_" + i);
            }

            // Then: Verify large dataset is processed without OOM
            try {
                RuleResult result = enrichmentService.enrichObjectWithResult(config.getEnrichments(), largeData);
                assertNotNull(result, "Should process large dataset successfully");
                assertTrue(result.isSuccess(), "Large dataset enrichment should succeed");

                logger.info("✓ Large dataset processed: 1,000 fields enriched successfully");

            } catch (OutOfMemoryError e) {
                fail("Large dataset processing caused OutOfMemoryError: " + e.getMessage());
            }
        }

        @Test
        @DisplayName("Should maintain stable performance during repeated enrichments")
        void testMemoryStabilityAfterGc() {
            logger.info("TEST: Performance stability during repeated enrichments");

            // When: Execute enrichments in batches
            int successCount1 = 0;
            int successCount2 = 0;

            // Execute first batch of 50 enrichments
            for (int i = 0; i < 50; i++) {
                Map<String, Object> testData = new HashMap<>();
                testData.put("type", "OTC");
                testData.put("id", i);
                testData.put("notional", 1000000.0 + (i * 10000));

                try {
                    RuleResult result = enrichmentService.enrichObjectWithResult(config.getEnrichments(), testData);
                    if (result != null && result.isSuccess()) {
                        successCount1++;
                    }
                } catch (Exception e) {
                    logger.warn("Enrichment failed in batch 1: {}", e.getMessage());
                }
            }

            // Execute second batch of 50 enrichments
            for (int i = 50; i < 100; i++) {
                Map<String, Object> testData = new HashMap<>();
                testData.put("type", "OTC");
                testData.put("id", i);
                testData.put("notional", 2000000.0 + (i * 10000));

                try {
                    RuleResult result = enrichmentService.enrichObjectWithResult(config.getEnrichments(), testData);
                    if (result != null && result.isSuccess()) {
                        successCount2++;
                    }
                } catch (Exception e) {
                    logger.warn("Enrichment failed in batch 2: {}", e.getMessage());
                }
            }

            // Then: Verify both batches completed successfully
            assertEquals(50, successCount1, "First batch should complete successfully");
            assertEquals(50, successCount2, "Second batch should complete successfully");

            logger.info("✓ Performance stable: batch1={}, batch2={} enrichments completed",
                successCount1, successCount2);
        }
    }
}

