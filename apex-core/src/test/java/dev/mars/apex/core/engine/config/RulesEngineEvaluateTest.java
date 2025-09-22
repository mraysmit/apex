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

package dev.mars.apex.core.engine.config;

import dev.mars.apex.core.config.yaml.YamlRuleConfiguration;
import dev.mars.apex.core.engine.model.RuleResult;
import dev.mars.apex.core.service.enrichment.EnrichmentService;
import dev.mars.apex.core.service.engine.ExpressionEvaluatorService;
import dev.mars.apex.core.service.error.ErrorRecoveryService;
import dev.mars.apex.core.service.lookup.LookupServiceRegistry;
import dev.mars.apex.core.service.monitoring.RulePerformanceMonitor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.expression.spel.standard.SpelExpressionParser;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for the new unified evaluate() method in RulesEngine.
 * This tests the Phase 2 implementation of the RuleResult API.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-09-22
 * @version 1.0
 */
class RulesEngineEvaluateTest {

    private RulesEngine rulesEngine;
    private RulesEngineConfiguration configuration;
    private EnrichmentService enrichmentService;

    @BeforeEach
    void setUp() {
        // Create basic configuration
        configuration = new RulesEngineConfiguration();
        
        // Create enrichment service
        LookupServiceRegistry serviceRegistry = new LookupServiceRegistry();
        ExpressionEvaluatorService evaluatorService = new ExpressionEvaluatorService();
        enrichmentService = new EnrichmentService(serviceRegistry, evaluatorService);
        
        // Create RulesEngine with EnrichmentService
        rulesEngine = new RulesEngine(configuration, new SpelExpressionParser(),
                                     new ErrorRecoveryService(), new RulePerformanceMonitor(), enrichmentService);
    }

    @Test
    @DisplayName("Test evaluate() method with null YAML configuration")
    void testEvaluateWithNullYamlConfig() {
        Map<String, Object> inputData = new HashMap<>();
        inputData.put("test", "value");

        // This should handle null gracefully and return a failure result
        RuleResult result = rulesEngine.evaluate(null, inputData);

        assertNotNull(result, "Result should not be null");
        assertFalse(result.isSuccess(), "Should fail with null configuration");
        assertTrue(result.hasFailures(), "Should have failures");
        assertFalse(result.getFailureMessages().isEmpty(), "Should have failure messages");
    }

    @Test
    @DisplayName("Test evaluate() method with empty YAML configuration")
    void testEvaluateWithEmptyYamlConfig() {
        YamlRuleConfiguration yamlConfig = new YamlRuleConfiguration();
        Map<String, Object> inputData = new HashMap<>();
        inputData.put("test", "value");
        
        RuleResult result = rulesEngine.evaluate(yamlConfig, inputData);
        
        assertNotNull(result, "Result should not be null");
        assertTrue(result.isSuccess(), "Empty configuration should be successful");
        assertFalse(result.hasFailures(), "Empty configuration should have no failures");
        assertTrue(result.getFailureMessages().isEmpty(), "Should have no failure messages");
        
        Map<String, Object> enrichedData = result.getEnrichedData();
        assertNotNull(enrichedData, "Enriched data should not be null");
        assertEquals("value", enrichedData.get("test"), "Original data should be preserved");
    }

    @Test
    @DisplayName("Test evaluate() method without EnrichmentService")
    void testEvaluateWithoutEnrichmentService() {
        // Create RulesEngine without EnrichmentService
        RulesEngine engineWithoutEnrichment = new RulesEngine(configuration);
        
        YamlRuleConfiguration yamlConfig = new YamlRuleConfiguration();
        Map<String, Object> inputData = new HashMap<>();
        inputData.put("test", "value");
        
        RuleResult result = engineWithoutEnrichment.evaluate(yamlConfig, inputData);
        
        assertNotNull(result, "Result should not be null");
        assertTrue(result.isSuccess(), "Should be successful when no enrichments defined");
        assertFalse(result.hasFailures(), "Should have no failures");
        
        Map<String, Object> enrichedData = result.getEnrichedData();
        assertNotNull(enrichedData, "Enriched data should not be null");
        assertEquals("value", enrichedData.get("test"), "Original data should be preserved");
    }

    @Test
    @DisplayName("Test evaluate() method with basic input data")
    void testEvaluateWithBasicInputData() {
        YamlRuleConfiguration yamlConfig = new YamlRuleConfiguration();
        Map<String, Object> inputData = new HashMap<>();
        inputData.put("amount", 100.0);
        inputData.put("currency", "USD");
        inputData.put("customerId", "CUST001");
        
        RuleResult result = rulesEngine.evaluate(yamlConfig, inputData);
        
        assertNotNull(result, "Result should not be null");
        assertTrue(result.isSuccess(), "Basic evaluation should be successful");
        assertFalse(result.hasFailures(), "Should have no failures");
        assertTrue(result.getFailureMessages().isEmpty(), "Should have no failure messages");
        
        // Test new API methods
        Map<String, Object> enrichedData = result.getEnrichedData();
        assertNotNull(enrichedData, "Enriched data should not be null");
        assertEquals(100.0, enrichedData.get("amount"), "Amount should be preserved");
        assertEquals("USD", enrichedData.get("currency"), "Currency should be preserved");
        assertEquals("CUST001", enrichedData.get("customerId"), "Customer ID should be preserved");
    }

    @Test
    @DisplayName("Test simplified evaluate() method with Map only")
    void testSimplifiedEvaluateMethod() {
        Map<String, Object> inputData = new HashMap<>();
        inputData.put("test", "value");
        
        RuleResult result = rulesEngine.evaluate(inputData);
        
        assertNotNull(result, "Result should not be null");
        assertFalse(result.isSuccess(), "Should fail without YAML configuration");
        assertTrue(result.hasFailures(), "Should have failures");
        assertFalse(result.getFailureMessages().isEmpty(), "Should have failure messages");
        
        assertTrue(result.getFailureMessages().get(0).contains("YamlRuleConfiguration"), 
                  "Should indicate missing YAML configuration");
    }

    @Test
    @DisplayName("Test evaluate() method preserves original data on errors")
    void testEvaluatePreservesDataOnErrors() {
        YamlRuleConfiguration yamlConfig = new YamlRuleConfiguration();
        Map<String, Object> inputData = new HashMap<>();
        inputData.put("originalField", "originalValue");
        inputData.put("numericField", 42);
        
        RuleResult result = rulesEngine.evaluate(yamlConfig, inputData);
        
        assertNotNull(result, "Result should not be null");
        
        Map<String, Object> enrichedData = result.getEnrichedData();
        assertNotNull(enrichedData, "Enriched data should not be null");
        assertEquals("originalValue", enrichedData.get("originalField"), "Original string field should be preserved");
        assertEquals(42, enrichedData.get("numericField"), "Original numeric field should be preserved");
    }

    @Test
    @DisplayName("Test evaluate() method with null input data")
    void testEvaluateWithNullInputData() {
        YamlRuleConfiguration yamlConfig = new YamlRuleConfiguration();

        // This should handle null input data gracefully
        RuleResult result = rulesEngine.evaluate(yamlConfig, null);

        assertNotNull(result, "Result should not be null");
        assertFalse(result.isSuccess(), "Should fail with null input data");
        assertTrue(result.hasFailures(), "Should have failures");
        assertFalse(result.getFailureMessages().isEmpty(), "Should have failure messages");
        assertTrue(result.getFailureMessages().get(0).contains("null"), "Should indicate null input data");
    }

    @Test
    @DisplayName("Test evaluate() method returns defensive copies")
    void testEvaluateReturnsDefensiveCopies() {
        YamlRuleConfiguration yamlConfig = new YamlRuleConfiguration();
        Map<String, Object> inputData = new HashMap<>();
        inputData.put("modifiable", "original");
        
        RuleResult result = rulesEngine.evaluate(yamlConfig, inputData);
        
        // Get the enriched data and try to modify it
        Map<String, Object> enrichedData = result.getEnrichedData();
        enrichedData.put("modifiable", "modified");
        enrichedData.put("newField", "newValue");
        
        // Get the data again and verify it wasn't modified
        Map<String, Object> enrichedDataAgain = result.getEnrichedData();
        assertEquals("original", enrichedDataAgain.get("modifiable"), "Original value should be preserved");
        assertNull(enrichedDataAgain.get("newField"), "New field should not exist");
    }
}
