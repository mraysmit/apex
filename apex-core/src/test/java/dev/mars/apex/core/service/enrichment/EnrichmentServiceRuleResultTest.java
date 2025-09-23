package dev.mars.apex.core.service.enrichment;

import dev.mars.apex.core.config.yaml.YamlEnrichment;
import dev.mars.apex.core.config.yaml.YamlRuleConfiguration;
import dev.mars.apex.core.engine.model.RuleResult;
import dev.mars.apex.core.service.engine.ExpressionEvaluatorService;
import dev.mars.apex.core.service.lookup.LookupServiceRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
 * Comprehensive test suite for EnrichmentService RuleResult integration (Phase 4).
 * Tests the new enrichObjectWithResult() methods that provide programmatic access
 * to enrichment success/failure status and detailed error information.
 *
 * This class is part of the APEX A powerful expression processor for Java applications.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-09-22
 * @version 1.0
 */
class EnrichmentServiceRuleResultTest {

    private static final Logger logger = LoggerFactory.getLogger(EnrichmentServiceRuleResultTest.class);

    private EnrichmentService enrichmentService;
    private LookupServiceRegistry serviceRegistry;
    private ExpressionEvaluatorService evaluatorService;

    @BeforeEach
    void setUp() {
        logger.info("Setting up EnrichmentService for RuleResult testing...");
        
        // Initialize services
        serviceRegistry = new LookupServiceRegistry();
        evaluatorService = new ExpressionEvaluatorService();
        enrichmentService = new EnrichmentService(serviceRegistry, evaluatorService);
        
        logger.info("EnrichmentService initialized successfully");
    }

    @Test
    @DisplayName("Should return success RuleResult when no enrichments provided")
    void testEnrichObjectWithResult_NoEnrichments() {
        logger.info("=== Testing enrichObjectWithResult with no enrichments ===");
        
        Map<String, Object> inputData = new HashMap<>();
        inputData.put("id", 1);
        inputData.put("name", "Test");
        
        // Test with null YAML config
        RuleResult result1 = enrichmentService.enrichObjectWithResult((YamlRuleConfiguration) null, inputData);
        
        assertNotNull(result1);
        assertTrue(result1.isSuccess(), "Should succeed when no enrichments to process");
        assertFalse(result1.hasFailures(), "Should have no failures");
        assertTrue(result1.getFailureMessages().isEmpty(), "Should have no failure messages");
        assertNotNull(result1.getEnrichedData(), "Should have enriched data");
        
        // Test with empty enrichments list
        List<YamlEnrichment> emptyList = new ArrayList<>();
        RuleResult result2 = enrichmentService.enrichObjectWithResult(emptyList, inputData);
        
        assertNotNull(result2);
        assertTrue(result2.isSuccess(), "Should succeed when empty enrichments list");
        assertFalse(result2.hasFailures(), "Should have no failures");
        
        // Test with null enrichment
        RuleResult result3 = enrichmentService.enrichObjectWithResult((YamlEnrichment) null, inputData);
        
        assertNotNull(result3);
        assertTrue(result3.isSuccess(), "Should succeed when null enrichment");
        assertFalse(result3.hasFailures(), "Should have no failures");
        
        logger.info("✅ No enrichments test passed");
    }

    @Test
    @DisplayName("Should return success RuleResult for successful enrichment")
    void testEnrichObjectWithResult_Success() {
        logger.info("=== Testing enrichObjectWithResult with successful enrichment ===");
        
        // Create test data
        Map<String, Object> inputData = new HashMap<>();
        inputData.put("id", 1);
        
        // Create inline dataset enrichment (no required fields)
        YamlEnrichment enrichment = createInlineDatasetEnrichment("test-enrichment", false);
        
        RuleResult result = enrichmentService.enrichObjectWithResult(enrichment, inputData);
        
        assertNotNull(result);
        assertTrue(result.isSuccess(), "Should succeed for successful enrichment");
        assertFalse(result.hasFailures(), "Should have no failures");
        assertTrue(result.getFailureMessages().isEmpty(), "Should have no failure messages");
        assertNotNull(result.getEnrichedData(), "Should have enriched data");
        
        logger.info("✅ Successful enrichment test passed");
    }

    @Test
    @DisplayName("Should return failure RuleResult for required field mapping failure")
    void testEnrichObjectWithResult_RequiredFieldFailure() {
        logger.info("=== Testing enrichObjectWithResult with required field failure ===");
        
        // Create test data that will cause required field failure
        Map<String, Object> inputData = new HashMap<>();
        inputData.put("id", 999); // ID that won't be found in dataset
        
        // Create enrichment with required field mapping
        YamlEnrichment enrichment = createInlineDatasetEnrichment("test-enrichment", true);
        
        RuleResult result = enrichmentService.enrichObjectWithResult(enrichment, inputData);
        
        assertNotNull(result);
        assertFalse(result.isSuccess(), "Should fail when required field mapping fails");
        assertTrue(result.hasFailures(), "Should have failures");
        assertFalse(result.getFailureMessages().isEmpty(), "Should have failure messages");
        
        // Check failure message content
        List<String> failureMessages = result.getFailureMessages();
        assertTrue(failureMessages.stream().anyMatch(msg -> 
            msg.contains("Required field enrichment failed")), 
            "Should contain required field failure message");
        
        assertNotNull(result.getEnrichedData(), "Should still have enriched data (partial)");
        
        logger.info("✅ Required field failure test passed");
    }

    @Test
    @DisplayName("Should handle mixed required field mappings correctly")
    void testEnrichObjectWithResult_MixedRequiredFields() {
        logger.info("=== Testing enrichObjectWithResult with mixed required field mappings ===");
        
        // Create enrichment with mixed required/optional field mappings
        YamlEnrichment enrichment = createMixedRequiredFieldEnrichment();
        
        // Test case 1: Missing optional field only (should succeed)
        Map<String, Object> inputData1 = new HashMap<>();
        inputData1.put("id", 1); // Has name and category, missing description
        
        RuleResult result1 = enrichmentService.enrichObjectWithResult(enrichment, inputData1);
        
        assertTrue(result1.isSuccess(), "Should succeed when only optional field is missing");
        assertFalse(result1.hasFailures(), "Should have no failures when only optional field is missing");
        
        // Test case 2: Missing required field (should fail)
        Map<String, Object> inputData2 = new HashMap<>();
        inputData2.put("id", 2); // Has description and category, missing name (required)
        
        RuleResult result2 = enrichmentService.enrichObjectWithResult(enrichment, inputData2);
        
        assertFalse(result2.isSuccess(), "Should fail when required field is missing");
        assertTrue(result2.hasFailures(), "Should have failures when required field is missing");
        assertFalse(result2.getFailureMessages().isEmpty(), "Should have failure messages");
        
        logger.info("✅ Mixed required field mappings test passed");
    }

    @Test
    @DisplayName("Should handle YAML configuration with multiple enrichments")
    void testEnrichObjectWithResult_YamlConfiguration() {
        logger.info("=== Testing enrichObjectWithResult with YAML configuration ===");
        
        // Create YAML configuration with multiple enrichments
        YamlRuleConfiguration yamlConfig = createYamlConfigurationWithEnrichments();
        
        Map<String, Object> inputData = new HashMap<>();
        inputData.put("id", 1);
        
        RuleResult result = enrichmentService.enrichObjectWithResult(yamlConfig, inputData);
        
        assertNotNull(result);
        assertTrue(result.isSuccess(), "Should succeed for valid YAML configuration");
        assertFalse(result.hasFailures(), "Should have no failures");
        assertNotNull(result.getEnrichedData(), "Should have enriched data");
        
        logger.info("✅ YAML configuration test passed");
    }

    @Test
    @DisplayName("Should provide defensive copies of enriched data and failure messages")
    void testEnrichObjectWithResult_DataImmutability() {
        logger.info("=== Testing data immutability in RuleResult ===");
        
        Map<String, Object> inputData = new HashMap<>();
        inputData.put("id", 999); // Will cause failure
        
        YamlEnrichment enrichment = createInlineDatasetEnrichment("test-enrichment", true);
        RuleResult result = enrichmentService.enrichObjectWithResult(enrichment, inputData);
        
        // Get the returned collections
        Map<String, Object> enrichedData = result.getEnrichedData();
        List<String> failureMessages = result.getFailureMessages();
        
        // Modify the returned collections
        enrichedData.put("newKey", "newValue");
        failureMessages.add("new message");
        
        // Get fresh copies and verify they weren't affected
        Map<String, Object> freshEnrichedData = result.getEnrichedData();
        List<String> freshFailureMessages = result.getFailureMessages();
        
        assertFalse(freshEnrichedData.containsKey("newKey"), 
            "Enriched data should be immutable (defensive copy)");
        assertFalse(freshFailureMessages.contains("new message"), 
            "Failure messages should be immutable (defensive copy)");
        
        logger.info("✅ Data immutability test passed");
    }

    // ========================================
    // Helper Methods
    // ========================================

    /**
     * Create an inline dataset enrichment for testing.
     */
    private YamlEnrichment createInlineDatasetEnrichment(String id, boolean requiredField) {
        YamlEnrichment enrichment = new YamlEnrichment();
        enrichment.setId(id);
        enrichment.setType("lookup-enrichment");
        enrichment.setEnabled(true);
        
        // Create lookup config with inline dataset
        YamlEnrichment.LookupConfig lookupConfig = new YamlEnrichment.LookupConfig();
        lookupConfig.setLookupKey("#id");
        
        YamlEnrichment.LookupDataset dataset = new YamlEnrichment.LookupDataset();
        dataset.setType("inline");
        dataset.setKeyField("id");
        
        // Create test data
        List<Map<String, Object>> data = new ArrayList<>();
        Map<String, Object> record1 = new HashMap<>();
        record1.put("id", 1);
        record1.put("name", "Test1");
        record1.put("category", "CategoryA");
        data.add(record1);
        
        Map<String, Object> record2 = new HashMap<>();
        record2.put("id", 2);
        record2.put("description", "Test2 Description");
        record2.put("category", "CategoryB");
        data.add(record2);
        
        dataset.setData(data);
        lookupConfig.setLookupDataset(dataset);
        enrichment.setLookupConfig(lookupConfig);
        
        // Create field mappings
        List<YamlEnrichment.FieldMapping> fieldMappings = new ArrayList<>();
        
        YamlEnrichment.FieldMapping nameMapping = new YamlEnrichment.FieldMapping();
        nameMapping.setSourceField("name");
        nameMapping.setTargetField("resultName");
        nameMapping.setRequired(requiredField);
        fieldMappings.add(nameMapping);
        
        enrichment.setFieldMappings(fieldMappings);
        
        return enrichment;
    }

    /**
     * Create an enrichment with mixed required/optional field mappings.
     */
    private YamlEnrichment createMixedRequiredFieldEnrichment() {
        YamlEnrichment enrichment = createInlineDatasetEnrichment("mixed-enrichment", false);
        
        // Override field mappings with mixed required settings
        List<YamlEnrichment.FieldMapping> fieldMappings = new ArrayList<>();
        
        // Required field
        YamlEnrichment.FieldMapping nameMapping = new YamlEnrichment.FieldMapping();
        nameMapping.setSourceField("name");
        nameMapping.setTargetField("resultName");
        nameMapping.setRequired(true);
        fieldMappings.add(nameMapping);
        
        // Optional field
        YamlEnrichment.FieldMapping descMapping = new YamlEnrichment.FieldMapping();
        descMapping.setSourceField("description");
        descMapping.setTargetField("resultDescription");
        descMapping.setRequired(false);
        fieldMappings.add(descMapping);
        
        // Required field
        YamlEnrichment.FieldMapping categoryMapping = new YamlEnrichment.FieldMapping();
        categoryMapping.setSourceField("category");
        categoryMapping.setTargetField("resultCategory");
        categoryMapping.setRequired(true);
        fieldMappings.add(categoryMapping);
        
        enrichment.setFieldMappings(fieldMappings);
        
        return enrichment;
    }

    /**
     * Create a YAML configuration with multiple enrichments.
     */
    private YamlRuleConfiguration createYamlConfigurationWithEnrichments() {
        YamlRuleConfiguration config = new YamlRuleConfiguration();

        // Set metadata properly
        YamlRuleConfiguration.ConfigurationMetadata metadata = new YamlRuleConfiguration.ConfigurationMetadata();
        metadata.setId("test-config");
        metadata.setName("Test Configuration");
        metadata.setVersion("1.0.0");
        config.setMetadata(metadata);

        List<YamlEnrichment> enrichments = new ArrayList<>();
        enrichments.add(createInlineDatasetEnrichment("enrichment-1", false));
        enrichments.add(createInlineDatasetEnrichment("enrichment-2", false));

        config.setEnrichments(enrichments);

        return config;
    }

    @Test
    @DisplayName("Should aggregate severity from enrichments with INFO severity")
    void testEnrichObjectWithResult_InfoSeverity() {
        logger.info("=== Testing enrichObjectWithResult with INFO severity ===");

        // Create enrichment with INFO severity
        YamlEnrichment enrichment = createInlineDatasetEnrichment("test-enrichment", false);
        enrichment.setSeverity("INFO");

        List<YamlEnrichment> enrichments = new ArrayList<>();
        enrichments.add(enrichment);

        Map<String, Object> inputData = new HashMap<>();
        inputData.put("id", 1);

        RuleResult result = enrichmentService.enrichObjectWithResult(enrichments, inputData);

        assertNotNull(result);
        assertTrue(result.isSuccess(), "Should succeed for valid enrichment");
        assertEquals("INFO", result.getSeverity(), "Should have INFO severity");
        assertFalse(result.hasFailures(), "Should have no failures");
    }

    @Test
    @DisplayName("Should aggregate severity from enrichments with WARNING severity")
    void testEnrichObjectWithResult_WarningSeverity() {
        logger.info("=== Testing enrichObjectWithResult with WARNING severity ===");

        // Create enrichment with WARNING severity
        YamlEnrichment enrichment = createInlineDatasetEnrichment("test-enrichment", false);
        enrichment.setSeverity("WARNING");

        List<YamlEnrichment> enrichments = new ArrayList<>();
        enrichments.add(enrichment);

        Map<String, Object> inputData = new HashMap<>();
        inputData.put("id", 1);

        RuleResult result = enrichmentService.enrichObjectWithResult(enrichments, inputData);

        assertNotNull(result);
        assertTrue(result.isSuccess(), "Should succeed for valid enrichment");
        assertEquals("WARNING", result.getSeverity(), "Should have WARNING severity");
        assertFalse(result.hasFailures(), "Should have no failures");
    }

    @Test
    @DisplayName("Should aggregate highest severity from multiple enrichments")
    void testEnrichObjectWithResult_MultipleSeverities() {
        logger.info("=== Testing enrichObjectWithResult with multiple severities ===");

        // Create enrichments with different severities
        YamlEnrichment infoEnrichment = createInlineDatasetEnrichment("info-enrichment", false);
        infoEnrichment.setSeverity("INFO");

        YamlEnrichment warningEnrichment = createInlineDatasetEnrichment("warning-enrichment", false);
        warningEnrichment.setSeverity("WARNING");

        YamlEnrichment errorEnrichment = createInlineDatasetEnrichment("error-enrichment", false);
        errorEnrichment.setSeverity("ERROR");

        List<YamlEnrichment> enrichments = new ArrayList<>();
        enrichments.add(infoEnrichment);
        enrichments.add(warningEnrichment);
        enrichments.add(errorEnrichment);

        Map<String, Object> inputData = new HashMap<>();
        inputData.put("id", 1);

        RuleResult result = enrichmentService.enrichObjectWithResult(enrichments, inputData);

        assertNotNull(result);
        assertTrue(result.isSuccess(), "Should succeed for valid enrichments");
        assertEquals("ERROR", result.getSeverity(), "Should have highest severity (ERROR)");
        assertFalse(result.hasFailures(), "Should have no failures");
    }
}
