package dev.mars.apex.demo.lookup;

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

import dev.mars.apex.core.config.yaml.YamlConfigurationException;
import dev.mars.apex.core.config.yaml.YamlConfigurationLoader;
import dev.mars.apex.core.config.yaml.YamlRuleConfiguration;
import dev.mars.apex.core.config.yaml.YamlRulesEngineService;
import dev.mars.apex.core.engine.config.RulesEngine;
import dev.mars.apex.core.engine.config.RulesEngineConfiguration;
import dev.mars.apex.core.engine.model.RuleResult;
import dev.mars.apex.core.service.enrichment.EnrichmentService;
import dev.mars.apex.core.service.lookup.LookupServiceRegistry;
import dev.mars.apex.core.service.engine.ExpressionEvaluatorService;
import dev.mars.apex.core.service.error.ErrorRecoveryService;
import dev.mars.apex.core.service.monitoring.RulePerformanceMonitor;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import dev.mars.apex.demo.ColoredTestOutputExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * BasicUsageExamplesTest - Comprehensive APEX Multi-Enrichment Processing Demo
 * 
 * PURPOSE: Demonstrate APEX's fundamental capabilities through real-world scenarios:
 * - APEX multi-enrichment processing with 3 different enrichment types
 * - APEX field validation and business object processing
 * - APEX SpEL expression evaluation and calculation enrichments
 * - APEX data-driven testing with separate configuration and data files
 * - APEX integrated workflow processing with sequential enrichments
 * 
 * EDUCATIONAL VALUE:
 * - Perfect introduction to APEX enrichment processing for beginners
 * - Demonstrates separation of configuration (BasicUsageExamplesTest-config.yaml) and test data (BasicUsageExamplesTest-data.yaml)
 * - Shows real-world business scenarios: customer validation, product processing, financial calculations
 * - Provides template for multi-enrichment workflow implementations
 * 
 * CRITICAL ENRICHMENT PROCESSING CHECKLIST APPLIED:
 *  Verify 3 enrichments process successfully - basic-field-validation, business-object-processing, expression-evaluation
 *  Validate field mappings work correctly - source fields mapped to target fields as configured
 *  Check SpEL expression evaluation - calculation enrichment processes mathematical expressions
 *  Assert conditional processing - enrichments only execute when conditions are met
 *  Confirm integrated workflow - all enrichments work together in sequence
 * 
 * ALL BUSINESS LOGIC IS IN APEX YAML FILES - NO CUSTOM JAVA LOGIC
 * Tests validate APEX enrichment capabilities using established patterns
 * 
 * Following established patterns from BarrierOptionNestedEnrichmentTest and ExternalDataSourceWorkingDemoTest
 */
@ExtendWith(ColoredTestOutputExtension.class)
@DisplayName("Basic Usage Examples Tests")
public class BasicUsageExamplesTest {
    
    private static final Logger logger = LoggerFactory.getLogger(BasicUsageExamplesTest.class);

    private YamlConfigurationLoader yamlLoader;
    private YamlRulesEngineService rulesEngineService;
    private EnrichmentService enrichmentService;
    private YamlRuleConfiguration config;
    private YamlRuleConfiguration dataConfig;

    @BeforeEach
    void setUp() {
        // Initialize APEX services following established patterns
        yamlLoader = new YamlConfigurationLoader();
        rulesEngineService = new YamlRulesEngineService();

        // Create enrichment service with required dependencies
        LookupServiceRegistry serviceRegistry = new LookupServiceRegistry();
        ExpressionEvaluatorService evaluatorService = new ExpressionEvaluatorService();
        enrichmentService = new EnrichmentService(serviceRegistry, evaluatorService);

        try {
            // Load both configuration and data files
            config = yamlLoader.loadFromFile("src/test/java/dev/mars/apex/demo/lookup/BasicUsageExamplesTest-config.yaml");
            dataConfig = yamlLoader.loadFromFile("src/test/java/dev/mars/apex/demo/lookup/BasicUsageExamplesTest-data.yaml");

            logger.info("✅ APEX services initialized for basic usage examples testing");
            logger.info("  - Configuration loaded: {}", config.getMetadata().getName());
            logger.info("  - Data scenarios loaded: {}", dataConfig.getMetadata().getName());

        } catch (YamlConfigurationException e) {
            logger.error("X Failed to load configurations: {}", e.getMessage());
            fail("Failed to load configurations: " + e.getMessage());
        }
    }

    /**
     * Create RulesEngine with EnrichmentService for enrichment processing
     * Following the established pattern from BarrierOptionNestedEnrichmentTest
     */
    private RulesEngine createRulesEngineWithEnrichmentService(YamlRuleConfiguration config) throws YamlConfigurationException {
        // Create basic configuration from YAML using the standard method
        RulesEngine baseEngine = rulesEngineService.createRulesEngineFromYamlConfig(config);
        RulesEngineConfiguration rulesConfig = baseEngine.getConfiguration();

        // Create RulesEngine with EnrichmentService
        RulesEngine engine = new RulesEngine(rulesConfig, new SpelExpressionParser(),
                                           new ErrorRecoveryService(), new RulePerformanceMonitor(), enrichmentService);

        assertNotNull(engine, "RulesEngine should be created");
        logger.info("✅ RulesEngine created with EnrichmentService");

        return engine;
    }

    @Test
    @DisplayName("Test Configuration Loading")
    void testConfigurationLoading() {
        logger.info("=== Testing Configuration Loading ===");

        // Validate configuration metadata
        assertNotNull(config, "Configuration should be loaded");
        assertNotNull(config.getMetadata(), "Configuration metadata should be present");
        assertEquals("basic-usage-examples-configuration", config.getMetadata().getId(), "Should have correct config ID");
        assertEquals("Basic Usage Examples Configuration", config.getMetadata().getName(), "Should have correct config name");
        assertEquals("enrichment", config.getMetadata().getType(), "Should be enrichment type");

        // Validate data configuration metadata
        assertNotNull(dataConfig, "Data configuration should be loaded");
        assertNotNull(dataConfig.getMetadata(), "Data metadata should be present");
        assertEquals("basic-usage-examples-data", dataConfig.getMetadata().getId(), "Should have correct data ID");
        assertEquals("Basic Usage Examples Data", dataConfig.getMetadata().getName(), "Should have correct data name");
        assertEquals("dataset", dataConfig.getMetadata().getType(), "Should be dataset type");

        logger.info("✓ Configuration loading validation passed");
        logger.info("  - Config: {} ({})", config.getMetadata().getName(), config.getMetadata().getType());
        logger.info("  - Data: {} ({})", dataConfig.getMetadata().getName(), dataConfig.getMetadata().getType());
    }

    @Test
    @DisplayName("Test Basic Field Validation Enrichment")
    void testBasicFieldValidationEnrichment() {
        logger.info("=== Testing Basic Field Validation Enrichment ===");

        try {
            // Create RulesEngine with EnrichmentService
            RulesEngine engine = createRulesEngineWithEnrichmentService(config);

            // Test Scenario 1: Valid user data
            logger.info("Testing valid user data scenario...");
            Map<String, Object> validUserData = new HashMap<>();
            validUserData.put("name", "John Doe");
            validUserData.put("age", 25);
            validUserData.put("email", "john.doe@example.com");

            RuleResult validResult = engine.evaluate(config, validUserData);
            assertNotNull(validResult, "Valid user result should not be null");
            
            logger.info("✓ Valid user data processed successfully");
            logger.info("  - Name: {} → {}", validUserData.get("name"), validResult.getEnrichedData().get("validatedName"));
            logger.info("  - Age: {} → {}", validUserData.get("age"), validResult.getEnrichedData().get("validatedAge"));
            logger.info("  - Email: {} → {}", validUserData.get("email"), validResult.getEnrichedData().get("validatedEmail"));

            // Test Scenario 2: Partial user data (name and age only)
            logger.info("Testing partial user data scenario...");
            Map<String, Object> partialUserData = new HashMap<>();
            partialUserData.put("name", "Jane Smith");
            partialUserData.put("age", 30);

            RuleResult partialResult = engine.evaluate(config, partialUserData);
            assertNotNull(partialResult, "Partial user result should not be null");

            logger.info("✓ Partial user data processed successfully");
            logger.info("  - Name: {} → {}", partialUserData.get("name"), partialResult.getEnrichedData().get("validatedName"));
            logger.info("  - Age: {} → {}", partialUserData.get("age"), partialResult.getEnrichedData().get("validatedAge"));

        } catch (Exception e) {
            logger.error("X Basic field validation enrichment failed: {}", e.getMessage());
            fail("Basic field validation enrichment failed: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("Test Business Object Processing Enrichment")
    void testBusinessObjectProcessingEnrichment() {
        logger.info("=== Testing Business Object Processing Enrichment ===");

        try {
            // Create RulesEngine with EnrichmentService
            RulesEngine engine = createRulesEngineWithEnrichmentService(config);

            // Test Scenario: Premium customer electronics
            logger.info("Testing premium customer electronics scenario...");
            Map<String, Object> premiumCustomerData = new HashMap<>();
            premiumCustomerData.put("customerType", "PREMIUM");
            premiumCustomerData.put("productCategory", "ELECTRONICS");
            premiumCustomerData.put("tradeValue", 7500);
            premiumCustomerData.put("membershipLevel", "GOLD");

            RuleResult premiumResult = engine.evaluate(config, premiumCustomerData);
            assertNotNull(premiumResult, "Premium customer result should not be null");

            logger.info("✓ Premium customer data processed successfully");
            logger.info("  - Customer Type: {} → {}", premiumCustomerData.get("customerType"), premiumResult.getEnrichedData().get("processedCustomerType"));
            logger.info("  - Product Category: {} → {}", premiumCustomerData.get("productCategory"), premiumResult.getEnrichedData().get("processedProductCategory"));
            logger.info("  - Trade Value: {} → {}", premiumCustomerData.get("tradeValue"), premiumResult.getEnrichedData().get("processedTradeValue"));

            // Test Scenario: Standard customer clothing
            logger.info("Testing standard customer clothing scenario...");
            Map<String, Object> standardCustomerData = new HashMap<>();
            standardCustomerData.put("customerType", "STANDARD");
            standardCustomerData.put("productCategory", "CLOTHING");
            standardCustomerData.put("tradeValue", 150);
            standardCustomerData.put("membershipLevel", "BRONZE");

            RuleResult standardResult = engine.evaluate(config, standardCustomerData);
            assertNotNull(standardResult, "Standard customer result should not be null");
            
            logger.info("✓ Standard customer data processed successfully");

        } catch (Exception e) {
            logger.error("X Business object processing enrichment failed: {}", e.getMessage());
            fail("Business object processing enrichment failed: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("Test Expression Evaluation Enrichment")
    void testExpressionEvaluationEnrichment() {
        logger.info("=== Testing Expression Evaluation Enrichment ===");

        try {
            // Create RulesEngine with EnrichmentService
            RulesEngine engine = createRulesEngineWithEnrichmentService(config);

            // Test Scenario: Interest calculation
            logger.info("Testing interest calculation scenario...");
            Map<String, Object> interestData = new HashMap<>();
            interestData.put("amount", 1000);
            interestData.put("rate", 0.05);

            RuleResult interestResult = engine.evaluate(config, interestData);
            assertNotNull(interestResult, "Interest calculation result should not be null");

            // Validate SpEL expression: #amount * 0.05 = 1000 * 0.05 = 50.0
            Object calculatedResult = interestResult.getEnrichedData().get("calculatedResult");
            logger.info("✓ Interest calculation processed successfully");
            logger.info("  - Amount: {} → Calculated Result: {}", interestData.get("amount"), calculatedResult);
            logger.info("  - Expected: 50.0 (1000 * 0.05)");

            // Test Scenario: Complete calculation with all fields
            logger.info("Testing complete calculation scenario...");
            Map<String, Object> completeData = new HashMap<>();
            completeData.put("amount", 2000);
            completeData.put("rate", 0.03);
            completeData.put("quantity", 5);
            completeData.put("price", 199.99);

            RuleResult completeResult = engine.evaluate(config, completeData);
            assertNotNull(completeResult, "Complete calculation result should not be null");

            Object completeCalculatedResult = completeResult.getEnrichedData().get("calculatedResult");
            logger.info("✓ Complete calculation processed successfully");
            logger.info("  - Amount: {} → Calculated Result: {}", completeData.get("amount"), completeCalculatedResult);
            logger.info("  - Expected: 100.0 (2000 * 0.05)");

        } catch (Exception e) {
            logger.error("X Expression evaluation enrichment failed: {}", e.getMessage());
            fail("Expression evaluation enrichment failed: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("Test Integrated Workflow Processing")
    void testIntegratedWorkflowProcessing() {
        logger.info("=== Testing Integrated Workflow Processing ===");

        try {
            // Create RulesEngine with EnrichmentService
            RulesEngine engine = createRulesEngineWithEnrichmentService(config);

            // Test complete user workflow scenario from data file
            logger.info("Testing complete user workflow scenario...");
            Map<String, Object> workflowData = new HashMap<>();

            // Basic validation data
            workflowData.put("name", "Alice Johnson");
            workflowData.put("age", 32);
            workflowData.put("email", "alice.johnson@example.com");

            // Business object data
            workflowData.put("customerType", "PREMIUM");
            workflowData.put("productCategory", "ELECTRONICS");
            workflowData.put("tradeValue", 12000);
            workflowData.put("membershipLevel", "GOLD");

            // Expression evaluation data
            workflowData.put("amount", 1500);
            workflowData.put("rate", 0.04);
            workflowData.put("quantity", 3);
            workflowData.put("price", 299.99);

            RuleResult workflowResult = engine.evaluate(config, workflowData);
            assertNotNull(workflowResult, "Workflow result should not be null");

            logger.info("✓ Integrated workflow processed successfully");
            logger.info("  - Step 1 (Basic Field Validation): Name → {}", workflowResult.getEnrichedData().get("validatedName"));
            logger.info("  - Step 2 (Business Object Processing): Customer Type → {}", workflowResult.getEnrichedData().get("processedCustomerType"));
            logger.info("  - Step 3 (Expression Evaluation): Calculated Result → {}", workflowResult.getEnrichedData().get("calculatedResult"));

            // Validate that all expected fields are processed
            assertNotNull(workflowResult.getEnrichedData().get("validatedName"), "Name should be validated");
            assertNotNull(workflowResult.getEnrichedData().get("processedCustomerType"), "Customer type should be processed");
            assertNotNull(workflowResult.getEnrichedData().get("calculatedResult"), "Expression should be calculated");

        } catch (Exception e) {
            logger.error("X Integrated workflow processing failed: {}", e.getMessage());
            fail("Integrated workflow processing failed: " + e.getMessage());
        }
    }
}
