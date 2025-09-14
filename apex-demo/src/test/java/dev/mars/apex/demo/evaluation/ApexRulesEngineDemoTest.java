package dev.mars.apex.demo.evaluation;

import dev.mars.apex.demo.DemoTestBase;
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
 * ApexRulesEngineDemoTest - JUnit 5 Test for APEX Rules Engine Demo
 *
 * This test validates authentic APEX rules engine functionality using real APEX services:
 * - Investment validation scenarios with amount-based processing
 * - Risk assessment processing with age and experience calculations
 * - Compliance checking workflows with regulatory requirements
 * - Sequential dependency processing with conditional logic
 * - Result-based routing patterns with business rules
 *
 * REAL APEX SERVICES TESTED:
 * - EnrichmentService: Real APEX enrichment processor for rules engine operations
 * - YamlConfigurationLoader: Real YAML configuration loading and validation
 * - ExpressionEvaluatorService: Real SpEL expression evaluation for business rules
 * - LookupServiceRegistry: Real lookup service management for rule processing
 *
 * NO HARDCODED SIMULATION: All processing uses authentic APEX core services with YAML-driven configuration.
 * NO HARDCODED OBJECTS: No manual investment, risk, or compliance object creation.
 *
 * BUSINESS LOGIC VALIDATION:
 * - Validates 2 enrichments execute (investment-validation + risk-assessment)
 * - Tests investment validation logic (>100k = HIGH_VALUE, <=100k = STANDARD)
 * - Tests risk assessment calculations (age-based risk scores, experience multipliers)
 * - Tests compliance processing paths (CONSERVATIVE/MODERATE/AGGRESSIVE routing)
 * - Verifies portfolio recommendations (>1M = PREMIUM, <=1M = STANDARD)
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-09-13
 * @version 1.0 - JUnit 5 conversion from ApexRulesEngineDemo.java
 */
class ApexRulesEngineDemoTest extends DemoTestBase {

    private static final Logger logger = LoggerFactory.getLogger(ApexRulesEngineDemoTest.class);

    /**
     * Test investment validation functionality using real APEX services
     * YAML defines 2 enrichments: both execute for investment scenarios
     */
    @Test
    void testInvestmentValidationFunctionality() {
        logger.info("=== Testing Investment Validation Functionality ===");
        
        // Load YAML configuration
        var config = loadAndValidateYaml("test-configs/apexrulesenginedemo-test.yaml");
        
        // Create test data for HIGH_VALUE investment scenario
        Map<String, Object> testData = new HashMap<>();
        testData.put("investmentAmount", 150000);  // > 100k = HIGH_VALUE
        testData.put("customerRiskProfile", "MODERATE");
        testData.put("customerAge", 35);  // 30-50 = risk score 70
        testData.put("investmentExperience", "EXPERIENCED");  // multiplier 1.2
        testData.put("riskTolerance", "MODERATE");  // MEDIUM rating
        testData.put("portfolioValue", 1500000);  // > 1M = PREMIUM
        
        // Process using real APEX services
        var result = enrichmentService.enrichObject(config, testData);

        // Verify processing completed successfully
        assertNotNull(result, "Enrichment result should not be null");

        // Cast result to Map for field access
        @SuppressWarnings("unchecked")
        Map<String, Object> enrichedData = (Map<String, Object>) result;

        // BUSINESS LOGIC VALIDATION - Investment Validation Enrichment
        assertEquals("HIGH_VALUE", enrichedData.get("validationStatus"),
            "Investment amount > 100k should result in HIGH_VALUE status");
        assertEquals("true", enrichedData.get("requiresApproval"),
            "Investment amount > 100k should require approval");
        assertEquals("MEDIUM_RISK", enrichedData.get("processingPath"),
            "MODERATE risk profile should result in MEDIUM_RISK processing path");

        // BUSINESS LOGIC VALIDATION - Risk Assessment Enrichment
        assertEquals(70, enrichedData.get("riskScore"),
            "Customer age 35 (30-50 range) should result in risk score 70");
        assertEquals(1.2, enrichedData.get("experienceMultiplier"),
            "EXPERIENCED investment experience should result in multiplier 1.2");
        assertEquals("MEDIUM", enrichedData.get("finalRiskRating"),
            "MODERATE risk tolerance should result in MEDIUM final risk rating");
        assertEquals("PREMIUM_PORTFOLIO", enrichedData.get("portfolioRecommendation"),
            "Portfolio value > 1M should result in PREMIUM_PORTFOLIO recommendation");
        
        logger.info("✅ Investment validation functionality test completed successfully");
    }

    /**
     * Test risk assessment processing functionality using real APEX services
     * YAML defines 2 enrichments: both execute for risk assessment scenarios
     */
    @Test
    void testRiskAssessmentProcessingFunctionality() {
        logger.info("=== Testing Risk Assessment Processing Functionality ===");
        
        // Load YAML configuration
        var config = loadAndValidateYaml("test-configs/apexrulesenginedemo-test.yaml");
        
        // Create test data for STANDARD investment with young customer
        Map<String, Object> testData = new HashMap<>();
        testData.put("investmentAmount", 75000);  // <= 100k = STANDARD
        testData.put("customerRiskProfile", "CONSERVATIVE");
        testData.put("customerAge", 25);  // < 30 = risk score 85
        testData.put("investmentExperience", "BEGINNER");  // multiplier 0.8
        testData.put("riskTolerance", "CONSERVATIVE");  // LOW rating
        testData.put("portfolioValue", 500000);  // <= 1M = STANDARD
        
        // Process using real APEX services
        var result = enrichmentService.enrichObject(config, testData);

        // Verify processing completed successfully
        assertNotNull(result, "Enrichment result should not be null");

        // Cast result to Map for field access
        @SuppressWarnings("unchecked")
        Map<String, Object> enrichedData = (Map<String, Object>) result;

        // BUSINESS LOGIC VALIDATION - Investment Validation Enrichment
        assertEquals("STANDARD", enrichedData.get("validationStatus"),
            "Investment amount <= 100k should result in STANDARD status");
        assertEquals("false", enrichedData.get("requiresApproval"),
            "Investment amount <= 100k should not require approval");
        assertEquals("LOW_RISK", enrichedData.get("processingPath"),
            "CONSERVATIVE risk profile should result in LOW_RISK processing path");

        // BUSINESS LOGIC VALIDATION - Risk Assessment Enrichment
        assertEquals(85, enrichedData.get("riskScore"),
            "Customer age 25 (< 30) should result in risk score 85");
        assertEquals(0.8, enrichedData.get("experienceMultiplier"),
            "BEGINNER investment experience should result in multiplier 0.8");
        assertEquals("LOW", enrichedData.get("finalRiskRating"),
            "CONSERVATIVE risk tolerance should result in LOW final risk rating");
        assertEquals("STANDARD_PORTFOLIO", enrichedData.get("portfolioRecommendation"),
            "Portfolio value <= 1M should result in STANDARD_PORTFOLIO recommendation");
        
        logger.info("✅ Risk assessment processing functionality test completed successfully");
    }

    /**
     * Test compliance checking workflow functionality using real APEX services
     * YAML defines 2 enrichments: both execute for compliance scenarios
     */
    @Test
    void testComplianceCheckingWorkflowFunctionality() {
        logger.info("=== Testing Compliance Checking Workflow Functionality ===");
        
        // Load YAML configuration
        var config = loadAndValidateYaml("test-configs/apexrulesenginedemo-test.yaml");
        
        // Create test data for AGGRESSIVE risk scenario
        Map<String, Object> testData = new HashMap<>();
        testData.put("investmentAmount", 250000);  // > 100k = HIGH_VALUE
        testData.put("customerRiskProfile", "AGGRESSIVE");
        testData.put("customerAge", 55);  // >= 50 = risk score 55
        testData.put("investmentExperience", "INTERMEDIATE");  // multiplier 1.0
        testData.put("riskTolerance", "AGGRESSIVE");  // HIGH rating
        testData.put("portfolioValue", 2000000);  // > 1M = PREMIUM
        
        // Process using real APEX services
        var result = enrichmentService.enrichObject(config, testData);

        // Verify processing completed successfully
        assertNotNull(result, "Enrichment result should not be null");

        // Cast result to Map for field access
        @SuppressWarnings("unchecked")
        Map<String, Object> enrichedData = (Map<String, Object>) result;

        // BUSINESS LOGIC VALIDATION - Investment Validation Enrichment
        assertEquals("HIGH_VALUE", enrichedData.get("validationStatus"),
            "Investment amount > 100k should result in HIGH_VALUE status");
        assertEquals("true", enrichedData.get("requiresApproval"),
            "Investment amount > 100k should require approval");
        assertEquals("HIGH_RISK", enrichedData.get("processingPath"),
            "AGGRESSIVE risk profile should result in HIGH_RISK processing path");

        // BUSINESS LOGIC VALIDATION - Risk Assessment Enrichment
        assertEquals(55, enrichedData.get("riskScore"),
            "Customer age 55 (>= 50) should result in risk score 55");
        assertEquals(1.0, enrichedData.get("experienceMultiplier"),
            "INTERMEDIATE investment experience should result in multiplier 1.0");
        assertEquals("HIGH", enrichedData.get("finalRiskRating"),
            "AGGRESSIVE risk tolerance should result in HIGH final risk rating");
        assertEquals("PREMIUM_PORTFOLIO", enrichedData.get("portfolioRecommendation"),
            "Portfolio value > 1M should result in PREMIUM_PORTFOLIO recommendation");
        
        logger.info("✅ Compliance checking workflow functionality test completed successfully");
    }

    /**
     * Test sequential dependency processing functionality using real APEX services
     * YAML defines 2 enrichments: both execute for sequential processing scenarios
     */
    @Test
    void testSequentialDependencyProcessingFunctionality() {
        logger.info("=== Testing Sequential Dependency Processing Functionality ===");
        
        // Load YAML configuration
        var config = loadAndValidateYaml("test-configs/apexrulesenginedemo-test.yaml");
        
        // Create test data for edge case scenario (exactly 100k)
        Map<String, Object> testData = new HashMap<>();
        testData.put("investmentAmount", 100000);  // exactly 100k = STANDARD (not > 100k)
        testData.put("customerRiskProfile", "MODERATE");
        testData.put("customerAge", 45);  // 30-50 = risk score 70
        testData.put("investmentExperience", "EXPERIENCED");  // multiplier 1.2
        testData.put("riskTolerance", "MODERATE");  // MEDIUM rating
        testData.put("portfolioValue", 1000000);  // exactly 1M = STANDARD (not > 1M)
        
        // Process using real APEX services
        var result = enrichmentService.enrichObject(config, testData);

        // Verify processing completed successfully
        assertNotNull(result, "Enrichment result should not be null");

        // Cast result to Map for field access
        @SuppressWarnings("unchecked")
        Map<String, Object> enrichedData = (Map<String, Object>) result;

        // BUSINESS LOGIC VALIDATION - Investment Validation Enrichment (Edge Case)
        assertEquals("STANDARD", enrichedData.get("validationStatus"),
            "Investment amount exactly 100k should result in STANDARD status (not > 100k)");
        assertEquals("false", enrichedData.get("requiresApproval"),
            "Investment amount exactly 100k should not require approval (not > 100k)");
        assertEquals("MEDIUM_RISK", enrichedData.get("processingPath"),
            "MODERATE risk profile should result in MEDIUM_RISK processing path");

        // BUSINESS LOGIC VALIDATION - Risk Assessment Enrichment (Edge Case)
        assertEquals(70, enrichedData.get("riskScore"),
            "Customer age 45 (30-50 range) should result in risk score 70");
        assertEquals(1.2, enrichedData.get("experienceMultiplier"),
            "EXPERIENCED investment experience should result in multiplier 1.2");
        assertEquals("MEDIUM", enrichedData.get("finalRiskRating"),
            "MODERATE risk tolerance should result in MEDIUM final risk rating");
        assertEquals("STANDARD_PORTFOLIO", enrichedData.get("portfolioRecommendation"),
            "Portfolio value exactly 1M should result in STANDARD_PORTFOLIO (not > 1M)");
        
        logger.info("✅ Sequential dependency processing functionality test completed successfully");
    }
}
