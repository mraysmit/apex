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

package dev.mars.apex.core.severity;

import dev.mars.apex.core.config.yaml.YamlConfigurationException;
import dev.mars.apex.core.config.yaml.YamlConfigurationLoader;
import dev.mars.apex.core.config.yaml.YamlRuleConfiguration;
import dev.mars.apex.core.constants.SeverityConstants;
import dev.mars.apex.core.engine.model.RuleResult;
import dev.mars.apex.core.service.enrichment.EnrichmentService;
import dev.mars.apex.core.service.engine.ExpressionEvaluatorService;
import dev.mars.apex.core.service.lookup.LookupServiceRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive integration test for APEX Severity Validation System.
 * 
 * This test validates the complete end-to-end severity processing pipeline:
 * - YAML configuration loading with severity validation
 * - Rule processing with severity information
 * - Enrichment processing with severity aggregation
 * - RuleResult creation with proper severity handling
 * - Complete workflow integration
 * 
 * Tests all phases of the APEX Severity Validation Implementation Plan:
 * - Phase 1: SeverityConstants creation
 * - Phase 2: Existing severity code refactoring
 * - Phase 3: Enrichment severity validation
 * - Phase 4: Enrichment processing with severity
 * - Phase 5: Integration testing (this test)
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-09-24
 * @version 1.0
 */
@DisplayName("APEX Severity Integration Test")
public class SeverityIntegrationTest {

    private static final Logger logger = LoggerFactory.getLogger(SeverityIntegrationTest.class);

    // Real APEX services for integration testing
    private YamlConfigurationLoader yamlLoader;
    private EnrichmentService enrichmentService;
    private LookupServiceRegistry serviceRegistry;
    private ExpressionEvaluatorService expressionEvaluator;

    @BeforeEach
    void setUp() {
        logger.info("Setting up APEX services for severity integration testing...");
        
        // Initialize real APEX services
        this.yamlLoader = new YamlConfigurationLoader();
        this.serviceRegistry = new LookupServiceRegistry();
        this.expressionEvaluator = new ExpressionEvaluatorService();
        this.enrichmentService = new EnrichmentService(serviceRegistry, expressionEvaluator);
        
        logger.info("APEX services initialized successfully");
    }

    @Test
    @DisplayName("Should validate complete severity workflow with INFO enrichments")
    void testCompleteWorkflowWithInfoSeverity() throws YamlConfigurationException {
        logger.info("=== Testing Complete Severity Workflow with INFO Enrichments ===");
        
        // Create YAML configuration with INFO severity enrichments
        String yamlConfig = """
            metadata:
              name: "Severity Integration Test - INFO"
              version: "1.0.0"
              description: "Integration test for INFO severity enrichments"
              type: "rule-configuration"
            
            enrichments:
              - id: "info-enrichment-1"
                type: "lookup-enrichment"
                condition: "#testField != null"
                severity: "INFO"
                enabled: true
                lookup-config:
                  lookup-key: "#key"
                  lookup-dataset:
                    type: "inline"
                    key-field: "key"
                    data:
                      - key: "test"
                        value: "Test Value 1"
                field-mappings:
                  - source-field: "value"
                    target-field: "enrichedValue1"
              
              - id: "info-enrichment-2"
                type: "lookup-enrichment"
                condition: "#testField != null"
                severity: "INFO"
                enabled: true
                lookup-config:
                  lookup-key: "#key"
                  lookup-dataset:
                    type: "inline"
                    key-field: "key"
                    data:
                      - key: "test"
                        value: "Test Value 2"
                field-mappings:
                  - source-field: "value"
                    target-field: "enrichedValue2"
            """;
        
        // Load configuration and verify it loads successfully
        YamlRuleConfiguration config = yamlLoader.fromYamlString(yamlConfig);
        assertNotNull(config, "Configuration should load successfully");
        assertNotNull(config.getEnrichments(), "Enrichments should be present");
        assertEquals(2, config.getEnrichments().size(), "Should have 2 enrichments");
        
        // Verify enrichment severities are set correctly
        assertEquals("INFO", config.getEnrichments().get(0).getSeverity(), "First enrichment should have INFO severity");
        assertEquals("INFO", config.getEnrichments().get(1).getSeverity(), "Second enrichment should have INFO severity");
        
        // Create test data
        Map<String, Object> testData = new HashMap<>();
        testData.put("testField", "test");
        testData.put("key", "test");
        
        // Process enrichments and verify result
        RuleResult result = enrichmentService.enrichObjectWithResult(config.getEnrichments(), testData);
        
        assertNotNull(result, "RuleResult should not be null");
        assertTrue(result.isSuccess(), "Processing should succeed");
        assertEquals(SeverityConstants.INFO, result.getSeverity(), "Result should have INFO severity");
        assertFalse(result.hasFailures(), "Should have no failures");
        
        logger.info("✅ Complete workflow with INFO severity completed successfully");
    }

    @Test
    @DisplayName("Should validate complete severity workflow with mixed severities")
    void testCompleteWorkflowWithMixedSeverities() throws YamlConfigurationException {
        logger.info("=== Testing Complete Severity Workflow with Mixed Severities ===");
        
        // Create YAML configuration with mixed severity enrichments
        String yamlConfig = """
            metadata:
              name: "Severity Integration Test - Mixed"
              version: "1.0.0"
              description: "Integration test for mixed severity enrichments"
              type: "rule-configuration"
            
            enrichments:
              - id: "info-enrichment"
                type: "lookup-enrichment"
                condition: "#testField != null"
                severity: "INFO"
                enabled: true
                lookup-config:
                  lookup-key: "#key"
                  lookup-dataset:
                    type: "inline"
                    key-field: "key"
                    data:
                      - key: "test"
                        value: "Info Value"
                field-mappings:
                  - source-field: "value"
                    target-field: "infoValue"
              
              - id: "warning-enrichment"
                type: "lookup-enrichment"
                condition: "#testField != null"
                severity: "WARNING"
                enabled: true
                lookup-config:
                  lookup-key: "#key"
                  lookup-dataset:
                    type: "inline"
                    key-field: "key"
                    data:
                      - key: "test"
                        value: "Warning Value"
                field-mappings:
                  - source-field: "value"
                    target-field: "warningValue"
              
              - id: "error-enrichment"
                type: "lookup-enrichment"
                condition: "#testField != null"
                severity: "ERROR"
                enabled: true
                lookup-config:
                  lookup-key: "#key"
                  lookup-dataset:
                    type: "inline"
                    key-field: "key"
                    data:
                      - key: "test"
                        value: "Error Value"
                field-mappings:
                  - source-field: "value"
                    target-field: "errorValue"
            """;
        
        // Load configuration and verify it loads successfully
        YamlRuleConfiguration config = yamlLoader.fromYamlString(yamlConfig);
        assertNotNull(config, "Configuration should load successfully");
        assertNotNull(config.getEnrichments(), "Enrichments should be present");
        assertEquals(3, config.getEnrichments().size(), "Should have 3 enrichments");
        
        // Verify enrichment severities are set correctly
        assertEquals("INFO", config.getEnrichments().get(0).getSeverity(), "First enrichment should have INFO severity");
        assertEquals("WARNING", config.getEnrichments().get(1).getSeverity(), "Second enrichment should have WARNING severity");
        assertEquals("ERROR", config.getEnrichments().get(2).getSeverity(), "Third enrichment should have ERROR severity");
        
        // Create test data
        Map<String, Object> testData = new HashMap<>();
        testData.put("testField", "test");
        testData.put("key", "test");
        
        // Process enrichments and verify result
        RuleResult result = enrichmentService.enrichObjectWithResult(config.getEnrichments(), testData);
        
        assertNotNull(result, "RuleResult should not be null");
        assertTrue(result.isSuccess(), "Processing should succeed");
        assertEquals(SeverityConstants.ERROR, result.getSeverity(), "Result should have highest severity (ERROR)");
        assertFalse(result.hasFailures(), "Should have no failures");
        
        logger.info("✅ Complete workflow with mixed severities completed successfully");
    }

    @Test
    @DisplayName("Should reject invalid severity in YAML configuration")
    void testInvalidSeverityRejection() {
        logger.info("=== Testing Invalid Severity Rejection ===");
        
        // Create YAML configuration with invalid severity
        String invalidYamlConfig = """
            metadata:
              name: "Invalid Severity Test"
              version: "1.0.0"
              description: "Test for invalid severity handling"
              type: "rule-configuration"
            
            enrichments:
              - id: "invalid-enrichment"
                type: "lookup-enrichment"
                condition: "#testField != null"
                severity: "CRITICAL"
                enabled: true
                lookup-config:
                  lookup-key: "#key"
                  lookup-dataset:
                    type: "inline"
                    key-field: "key"
                    data:
                      - key: "test"
                        value: "Test Value"
                field-mappings:
                  - source-field: "value"
                    target-field: "enrichedValue"
            """;
        
        // Verify that loading configuration with invalid severity throws exception
        YamlConfigurationException exception = assertThrows(YamlConfigurationException.class, () -> {
            yamlLoader.fromYamlString(invalidYamlConfig);
        }, "Should throw YamlConfigurationException for invalid severity");
        
        assertTrue(exception.getMessage().contains("invalid severity"), 
                   "Exception message should mention invalid severity");
        assertTrue(exception.getMessage().contains("CRITICAL"), 
                   "Exception message should mention the invalid severity value");
        
        logger.info("✅ Invalid severity rejection test completed successfully");
    }

    @Test
    @DisplayName("Should handle default severity when not specified")
    void testDefaultSeverityHandling() throws YamlConfigurationException {
        logger.info("=== Testing Default Severity Handling ===");
        
        // Create YAML configuration without severity specified
        String yamlConfig = """
            metadata:
              name: "Default Severity Test"
              version: "1.0.0"
              description: "Test for default severity handling"
              type: "rule-configuration"
            
            enrichments:
              - id: "default-enrichment"
                type: "lookup-enrichment"
                condition: "#testField != null"
                enabled: true
                lookup-config:
                  lookup-key: "#key"
                  lookup-dataset:
                    type: "inline"
                    key-field: "key"
                    data:
                      - key: "test"
                        value: "Default Value"
                field-mappings:
                  - source-field: "value"
                    target-field: "enrichedValue"
            """;
        
        // Load configuration and verify it loads successfully
        YamlRuleConfiguration config = yamlLoader.fromYamlString(yamlConfig);
        assertNotNull(config, "Configuration should load successfully");
        assertNotNull(config.getEnrichments(), "Enrichments should be present");
        assertEquals(1, config.getEnrichments().size(), "Should have 1 enrichment");
        
        // Verify default severity is set
        assertEquals(SeverityConstants.INFO, config.getEnrichments().get(0).getSeverity(), 
                     "Enrichment should have default INFO severity");
        
        // Create test data
        Map<String, Object> testData = new HashMap<>();
        testData.put("testField", "test");
        testData.put("key", "test");
        
        // Process enrichments and verify result
        RuleResult result = enrichmentService.enrichObjectWithResult(config.getEnrichments(), testData);
        
        assertNotNull(result, "RuleResult should not be null");
        assertTrue(result.isSuccess(), "Processing should succeed");
        assertEquals(SeverityConstants.INFO, result.getSeverity(), "Result should have default INFO severity");
        assertFalse(result.hasFailures(), "Should have no failures");
        
        logger.info("✅ Default severity handling test completed successfully");
    }
}
