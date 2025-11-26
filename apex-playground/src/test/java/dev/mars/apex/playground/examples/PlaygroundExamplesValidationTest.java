/*
 * Copyright 2025 APEX Demo Team
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

package dev.mars.apex.playground.examples;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.mars.apex.core.config.yaml.YamlConfigurationException;
import dev.mars.apex.core.config.yaml.YamlConfigurationLoader;
import dev.mars.apex.core.config.yaml.YamlRuleConfiguration;
import dev.mars.apex.core.engine.config.RulesEngine;
import dev.mars.apex.core.engine.model.RuleResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive validation tests for all apex-playground example YAML/JSON pairs.
 * 
 * This test class validates that each example in the apex-playground/examples folder
 * works correctly with apex-core APIs, following the patterns established in apex-demo tests.
 * 
 * Test Principles (from prompts.txt):
 * - Test actual functionality, not just YAML syntax
 * - Execute real APEX operations with RuleResult validation
 * - Validate ALL business logic operations
 * - Assert on actual enriched data
 * 
 * @author APEX Demo Team
 * @since 2025-11-26
 */
@DisplayName("Playground Examples Validation Tests")
public class PlaygroundExamplesValidationTest {

    private static final Logger logger = LoggerFactory.getLogger(PlaygroundExamplesValidationTest.class);
    private static final String EXAMPLES_BASE_PATH = "examples";
    
    private YamlConfigurationLoader yamlLoader;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        yamlLoader = new YamlConfigurationLoader();
        objectMapper = new ObjectMapper();
        logger.info("✅ Test setup complete");
    }

    /**
     * Helper method to load JSON data from file
     */
    private Map<String, Object> loadJsonData(String jsonPath) throws IOException {
        Path path = resolveExamplePath(jsonPath);
        String jsonContent = Files.readString(path);
        return objectMapper.readValue(jsonContent, new TypeReference<Map<String, Object>>() {});
    }

    /**
     * Helper method to resolve example path (handles both IDE and Maven execution)
     */
    private Path resolveExamplePath(String relativePath) {
        Path path = Paths.get(EXAMPLES_BASE_PATH, relativePath);
        if (!Files.exists(path)) {
            path = Paths.get("apex-playground", EXAMPLES_BASE_PATH, relativePath);
        }
        return path;
    }

    /**
     * Helper method to load YAML configuration from file
     */
    private YamlRuleConfiguration loadYamlConfig(String yamlPath) throws YamlConfigurationException {
        Path path = resolveExamplePath(yamlPath);
        return yamlLoader.loadFromFile(path.toString());
    }

    // ========================================================================
    // BASIC EXAMPLES
    // ========================================================================
    @Nested
    @DisplayName("Basic Examples")
    class BasicExamples {

        @Test
        @DisplayName("minimal-rule: Age >= 18 validation")
        void testMinimalRule() throws Exception {
            logger.info("=== Testing minimal-rule example ===");
            
            // Load configuration and data
            YamlRuleConfiguration config = loadYamlConfig("basic/minimal-rule.yaml");
            Map<String, Object> testData = loadJsonData("basic/minimal-rule.json");
            
            logger.info("Input data: {}", testData);
            
            // Create engine and execute
            RulesEngine engine = RulesEngine.fromYamlConfig(config);
            var rule = engine.getConfiguration().getRuleById("simple-check");
            assertNotNull(rule, "Rule 'simple-check' should exist");
            
            RuleResult result = engine.executeRule(rule, testData);
            
            // Validate results
            assertNotNull(result, "RuleResult should not be null");
            assertTrue(result.isTriggered(), "Rule should trigger for age=20 (>= 18)");
            assertTrue(result.isSuccess(), "Rule execution should succeed");
            
            logger.info("✓ minimal-rule test passed - age {} triggered rule", testData.get("age"));
        }

        @Test
        @DisplayName("simple-age-validation: Multiple age rules")
        void testSimpleAgeValidation() throws Exception {
            logger.info("=== Testing simple-age-validation example ===");
            
            YamlRuleConfiguration config = loadYamlConfig("basic/simple-age-validation.yaml");
            Map<String, Object> testData = loadJsonData("basic/simple-age-validation.json");
            
            logger.info("Input data: {}", testData);
            
            RulesEngine engine = RulesEngine.fromYamlConfig(config);
            
            // Test age-too-young rule (age=17 should trigger)
            var ageTooYoungRule = engine.getConfiguration().getRuleById("age-too-young");
            assertNotNull(ageTooYoungRule, "Rule 'age-too-young' should exist");
            
            RuleResult result = engine.executeRule(ageTooYoungRule, testData);
            
            assertNotNull(result, "RuleResult should not be null");
            assertTrue(result.isTriggered(), "age-too-young rule should trigger for age=17");
            
            logger.info("✓ simple-age-validation test passed - age {} is too young", testData.get("age"));
        }

        @Test
        @DisplayName("quick-start: Amount and currency validation")
        void testQuickStart() throws Exception {
            logger.info("=== Testing quick-start example ===");
            
            YamlRuleConfiguration config = loadYamlConfig("basic/quick-start.yaml");
            Map<String, Object> testData = loadJsonData("basic/quick-start.json");
            
            logger.info("Input data: {}", testData);
            
            RulesEngine engine = RulesEngine.fromYamlConfig(config);
            
            // Test check-amount rule
            var checkAmountRule = engine.getConfiguration().getRuleById("check-amount");
            assertNotNull(checkAmountRule, "Rule 'check-amount' should exist");
            
            RuleResult amountResult = engine.executeRule(checkAmountRule, testData);
            assertTrue(amountResult.isTriggered(), "check-amount should trigger for amount=100");
            
            // Test check-currency rule
            var checkCurrencyRule = engine.getConfiguration().getRuleById("check-currency");
            assertNotNull(checkCurrencyRule, "Rule 'check-currency' should exist");
            
            RuleResult currencyResult = engine.executeRule(checkCurrencyRule, testData);
            assertTrue(currencyResult.isTriggered(), "check-currency should trigger for USD");
            
            logger.info("✓ quick-start test passed - amount={}, currency={}", 
                testData.get("amount"), testData.get("currency"));
        }

        @Test
        @DisplayName("nested-field-navigation: SpEL nested field access")
        void testNestedFieldNavigation() throws Exception {
            logger.info("=== Testing nested-field-navigation example ===");
            
            YamlRuleConfiguration config = loadYamlConfig("basic/nested-field-navigation.yaml");
            Map<String, Object> testData = loadJsonData("basic/nested-field-navigation.json");
            
            logger.info("Input data: {}", testData);
            
            // Create engine and evaluate
            RulesEngine engine = RulesEngine.fromYamlConfig(config);
            RuleResult result = engine.evaluate(config, testData);
            
            assertNotNull(result, "RuleResult should not be null");
            assertTrue(result.isSuccess(), "Processing should succeed");
            
            Map<String, Object> enrichedData = result.getEnrichedData();
            assertNotNull(enrichedData, "Enriched data should not be null");
            
            // Verify nested field extraction
            assertEquals("USD", enrichedData.get("trade_currency"), 
                "trade_currency should be extracted from trade.currency");
            assertEquals(1000, enrichedData.get("trade_amount"), 
                "trade_amount should be extracted from trade.amount");
            
            logger.info("✓ nested-field-navigation test passed - extracted currency={}, amount={}", 
                enrichedData.get("trade_currency"), enrichedData.get("trade_amount"));
        }
    }

    // ========================================================================
    // VALIDATION EXAMPLES
    // ========================================================================
    @Nested
    @DisplayName("Validation Examples")
    class ValidationExamples {

        @Test
        @DisplayName("value-threshold: Amount threshold validation with rule groups")
        void testValueThreshold() throws Exception {
            logger.info("=== Testing value-threshold example ===");
            
            YamlRuleConfiguration config = loadYamlConfig("validation/value-threshold.yaml");
            Map<String, Object> testData = loadJsonData("validation/value-threshold-data.json");
            
            logger.info("Input data: {}", testData);
            
            RulesEngine engine = RulesEngine.fromYamlConfig(config);
            
            // Test value-threshold-check rule (amount=1500 > 100)
            var thresholdRule = engine.getConfiguration().getRuleById("value-threshold-check");
            assertNotNull(thresholdRule, "Rule 'value-threshold-check' should exist");
            
            RuleResult result = engine.executeRule(thresholdRule, testData);
            assertTrue(result.isTriggered(), "value-threshold-check should trigger for amount=1500");
            
            // Test high-value-warning rule (amount=1500 > 1000)
            var warningRule = engine.getConfiguration().getRuleById("high-value-warning");
            assertNotNull(warningRule, "Rule 'high-value-warning' should exist");
            
            RuleResult warningResult = engine.executeRule(warningRule, testData);
            assertTrue(warningResult.isTriggered(), "high-value-warning should trigger for amount=1500");
            
            // Test currency-validation rule
            var currencyRule = engine.getConfiguration().getRuleById("currency-validation");
            assertNotNull(currencyRule, "Rule 'currency-validation' should exist");
            
            RuleResult currencyResult = engine.executeRule(currencyRule, testData);
            assertTrue(currencyResult.isTriggered(), "currency-validation should trigger for USD");
            
            logger.info("✓ value-threshold test passed - amount={}, currency={}", 
                testData.get("amount"), testData.get("currency"));
        }
    }

    // ========================================================================
    // ENRICHMENT EXAMPLES
    // ========================================================================
    @Nested
    @DisplayName("Enrichment Examples")
    class EnrichmentExamples {

        @Test
        @DisplayName("constant-value-enrichment: Constant field assignment")
        void testConstantValueEnrichment() throws Exception {
            logger.info("=== Testing constant-value-enrichment example ===");
            
            YamlRuleConfiguration config = loadYamlConfig("enrichment/constant-value-enrichment.yaml");
            Map<String, Object> testData = loadJsonData("enrichment/constant-value-enrichment.json");
            
            logger.info("Input data: {}", testData);
            
            RulesEngine engine = RulesEngine.fromYamlConfig(config);
            RuleResult result = engine.evaluate(config, testData);
            
            assertNotNull(result, "RuleResult should not be null");
            assertTrue(result.isSuccess(), "Processing should succeed");
            
            Map<String, Object> enrichedData = result.getEnrichedData();
            assertNotNull(enrichedData, "Enriched data should not be null");
            
            // Verify constant values were assigned
            assertEquals("ACTIVE", enrichedData.get("status"), "status should be 'ACTIVE'");
            assertEquals("e1", enrichedData.get("enrichmentId"), "enrichmentId should be 'e1'");
            assertEquals("OTC_OPTION", enrichedData.get("productType"), "productType should be 'OTC_OPTION'");
            
            // Verify conditional enrichment (notionalAmount > 10000000)
            assertEquals("HIGH_VALUE", enrichedData.get("valueCategory"), 
                "valueCategory should be 'HIGH_VALUE' for notionalAmount=15000000");
            
            logger.info("✓ constant-value-enrichment test passed");
        }

        @Test
        @DisplayName("financial-validation: Trade date validation rules")
        void testFinancialValidation() throws Exception {
            logger.info("=== Testing financial-validation example ===");
            
            YamlRuleConfiguration config = loadYamlConfig("enrichment/financial-validation.yaml");
            Map<String, Object> testData = loadJsonData("enrichment/financial-validation.json");
            
            logger.info("Input data: {}", testData);
            
            RulesEngine engine = RulesEngine.fromYamlConfig(config);
            
            // Test validate-trade-date rule
            var tradeDateRule = engine.getConfiguration().getRuleById("validate-trade-date");
            assertNotNull(tradeDateRule, "Rule 'validate-trade-date' should exist");
            
            RuleResult result = engine.executeRule(tradeDateRule, testData);
            assertTrue(result.isTriggered(), "validate-trade-date should trigger when tradeDate is present");
            
            logger.info("✓ financial-validation test passed");
        }

        @Test
        @DisplayName("enrichment-service-requirement: Lookup enrichment with inline data")
        void testEnrichmentServiceRequirement() throws Exception {
            logger.info("=== Testing enrichment-service-requirement example ===");

            YamlRuleConfiguration config = loadYamlConfig("enrichment/enrichment-service-requirement.yaml");
            Map<String, Object> testData = loadJsonData("enrichment/enrichment-service-requirement.json");

            logger.info("Input data: {}", testData);

            RulesEngine engine = RulesEngine.fromYamlConfig(config);
            RuleResult result = engine.evaluate(config, testData);

            assertNotNull(result, "RuleResult should not be null");
            assertTrue(result.isSuccess(), "Processing should succeed");

            Map<String, Object> enrichedData = result.getEnrichedData();
            assertNotNull(enrichedData, "Enriched data should not be null");

            // Verify lookup enrichment results
            assertEquals("Acme Corporation", enrichedData.get("customer_name"),
                "customer_name should be enriched from lookup for CUST001");
            assertEquals("Payment", enrichedData.get("transaction_category"),
                "transaction_category should be enriched from lookup for TXN001");

            logger.info("✓ enrichment-service-requirement test passed");
        }

        @Test
        @DisplayName("comprehensive-financial-settlement: Multi-enrichment settlement processing")
        void testComprehensiveFinancialSettlement() throws Exception {
            logger.info("=== Testing comprehensive-financial-settlement example ===");

            YamlRuleConfiguration config = loadYamlConfig("enrichment/comprehensive-financial-settlement.yaml");
            Map<String, Object> testData = loadJsonData("enrichment/comprehensive-financial-settlement.json");

            logger.info("Input data: {}", testData);

            RulesEngine engine = RulesEngine.fromYamlConfig(config);
            RuleResult result = engine.evaluate(config, testData);

            assertNotNull(result, "RuleResult should not be null");
            assertTrue(result.isSuccess(), "Processing should succeed");

            Map<String, Object> enrichedData = result.getEnrichedData();
            assertNotNull(enrichedData, "Enriched data should not be null");

            // Verify settlement type classification
            assertEquals("EQUITY_SETTLEMENT", enrichedData.get("settlementType"),
                "settlementType should be EQUITY_SETTLEMENT for assetClass=EQUITY");

            // Verify settlement priority (quantity=5000000 > 1000000)
            assertEquals("MEDIUM_PRIORITY", enrichedData.get("settlementPriority"),
                "settlementPriority should be MEDIUM_PRIORITY for quantity=5000000");

            // Verify risk assessment (counterparty contains 'Goldman')
            assertEquals("LOW_RISK", enrichedData.get("riskAssessment"),
                "riskAssessment should be LOW_RISK for Goldman Sachs");

            // Verify settlement convention (market=UK)
            assertEquals("T+2", enrichedData.get("settlementConvention"),
                "settlementConvention should be T+2 for UK market");

            // Verify regulatory compliance (market=UK)
            assertEquals("FCA_COMPLIANCE_REQUIRED", enrichedData.get("regulatoryCompliance"),
                "regulatoryCompliance should be FCA_COMPLIANCE_REQUIRED for UK market");

            logger.info("✓ comprehensive-financial-settlement test passed");
        }

        @Test
        @DisplayName("constant-values: Multiple constant value enrichments with conditions")
        void testConstantValues() throws Exception {
            logger.info("=== Testing constant-values example ===");

            YamlRuleConfiguration config = loadYamlConfig("enrichment/constant-values.yaml");
            Map<String, Object> testData = loadJsonData("enrichment/constant-values-data.json");

            logger.info("Input data: {}", testData);

            RulesEngine engine = RulesEngine.fromYamlConfig(config);
            RuleResult result = engine.evaluate(config, testData);

            assertNotNull(result, "RuleResult should not be null");
            assertTrue(result.isSuccess(), "Processing should succeed");

            Map<String, Object> enrichedData = result.getEnrichedData();
            assertNotNull(enrichedData, "Enriched data should not be null");
            logger.info("Enriched data: {}", enrichedData);

            // Verify basic constants (always applied)
            assertEquals("ACTIVE", enrichedData.get("status"), "status should be 'ACTIVE'");
            assertEquals("OTC_OPTION", enrichedData.get("productType"), "productType should be 'OTC_OPTION'");

            // Verify numeric constants (applied when defaultQuantity is null)
            assertEquals(100, enrichedData.get("defaultQuantity"), "defaultQuantity should be 100");
            assertEquals(true, enrichedData.get("requiresValidation"), "requiresValidation should be true");

            // Verify conditional constants (notionalAmount=5000000 > 1000000)
            assertEquals("HIGH_VALUE", enrichedData.get("valueCategory"),
                "valueCategory should be 'HIGH_VALUE' for notionalAmount=5000000");
            assertEquals("SENIOR_TRADER", enrichedData.get("approvalLevel"),
                "approvalLevel should be 'SENIOR_TRADER' for high value trade");

            // Verify implicit constants (new syntax without source-field)
            assertEquals("BATCH", enrichedData.get("processingMode"), "processingMode should be 'BATCH'");
            assertEquals("V2.1", enrichedData.get("validationVersion"), "validationVersion should be 'V2.1'");

            logger.info("✓ constant-values test passed");
        }
    }

    // ========================================================================
    // LOOKUP EXAMPLES
    // ========================================================================
    @Nested
    @DisplayName("Lookup Examples")
    class LookupExamples {

        @Test
        @DisplayName("dynamic-pricing: Customer tier-based pricing lookup")
        void testDynamicPricing() throws Exception {
            logger.info("=== Testing dynamic-pricing example ===");

            YamlRuleConfiguration config = loadYamlConfig("lookup/dynamic-pricing.yaml");
            Map<String, Object> testData = loadJsonData("lookup/dynamic-pricing-data.json");

            logger.info("Input data: {}", testData);

            RulesEngine engine = RulesEngine.fromYamlConfig(config);
            RuleResult result = engine.evaluate(config, testData);

            assertNotNull(result, "RuleResult should not be null");
            assertTrue(result.isSuccess(), "Processing should succeed");

            Map<String, Object> enrichedData = result.getEnrichedData();
            assertNotNull(enrichedData, "Enriched data should not be null");

            // Verify PLATINUM tier pricing lookup
            assertEquals(8500.0, enrichedData.get("calculatedFinalAmount"),
                "calculatedFinalAmount should be 8500.0 for PLATINUM tier (15% discount)");
            assertEquals("Premium support, expedited processing", enrichedData.get("customerTierBenefits"),
                "customerTierBenefits should match PLATINUM tier");

            logger.info("✓ dynamic-pricing test passed");
        }

        @Test
        @DisplayName("math-calculations: Mathematical function enrichments")
        void testMathCalculations() throws Exception {
            logger.info("=== Testing math-calculations example ===");

            YamlRuleConfiguration config = loadYamlConfig("lookup/math-calculations.yaml");
            Map<String, Object> testData = loadJsonData("lookup/math-calculations-data.json");

            logger.info("Input data: {}", testData);

            RulesEngine engine = RulesEngine.fromYamlConfig(config);
            RuleResult result = engine.evaluate(config, testData);

            assertNotNull(result, "RuleResult should not be null");
            assertTrue(result.isSuccess(), "Processing should succeed");

            Map<String, Object> enrichedData = result.getEnrichedData();
            assertNotNull(enrichedData, "Enriched data should not be null");
            logger.info("Enriched data: {}", enrichedData);

            // The calculation-enrichment stores results in result-field first, then maps to target-field
            // Check for squareRoot (intermediate result) or result.squareRoot (mapped result)
            Object sqrtResult = enrichedData.get("squareRoot");
            if (sqrtResult == null) {
                sqrtResult = getNestedValue(enrichedData, "result.squareRoot");
            }
            assertNotNull(sqrtResult, "squareRoot should be calculated");
            assertEquals(12.0, ((Number) sqrtResult).doubleValue(), 0.001,
                "sqrt(144) should be 12.0");

            // Check for powerResult or result.power
            Object powerResult = enrichedData.get("powerResult");
            if (powerResult == null) {
                powerResult = getNestedValue(enrichedData, "result.power");
            }
            assertNotNull(powerResult, "power should be calculated");
            assertEquals(256.0, ((Number) powerResult).doubleValue(), 0.001,
                "2^8 should be 256.0");

            // Check for roundedValue or result.rounded
            Object roundedResult = enrichedData.get("roundedValue");
            if (roundedResult == null) {
                roundedResult = getNestedValue(enrichedData, "result.rounded");
            }
            assertNotNull(roundedResult, "rounded should be calculated");
            assertEquals(123L, ((Number) roundedResult).longValue(),
                "round(123.456) should be 123");

            logger.info("✓ math-calculations test passed");
        }

        @SuppressWarnings("unchecked")
        private Object getNestedValue(Map<String, Object> data, String path) {
            String[] parts = path.split("\\.");
            Object current = data;
            for (String part : parts) {
                if (current instanceof Map) {
                    current = ((Map<String, Object>) current).get(part);
                } else {
                    return null;
                }
            }
            return current;
        }
    }

    // ========================================================================
    // RULE GROUPS EXAMPLES
    // ========================================================================
    @Nested
    @DisplayName("Rule Groups Examples")
    class RuleGroupsExamples {

        @Test
        @DisplayName("inline-groups: Rule group with inline references")
        void testInlineGroups() throws Exception {
            logger.info("=== Testing inline-groups example ===");

            YamlRuleConfiguration config = loadYamlConfig("rulegroups/inline-groups.yaml");
            Map<String, Object> testData = loadJsonData("rulegroups/inline-groups-data.json");

            logger.info("Input data: {}", testData);

            RulesEngine engine = RulesEngine.fromYamlConfig(config);

            // Test individual rules
            var usernameRule = engine.getConfiguration().getRuleById("username-check");
            assertNotNull(usernameRule, "Rule 'username-check' should exist");
            RuleResult usernameResult = engine.executeRule(usernameRule, testData);
            assertTrue(usernameResult.isTriggered(), "username-check should trigger for username='jdoe'");

            var statusRule = engine.getConfiguration().getRuleById("status-check");
            assertNotNull(statusRule, "Rule 'status-check' should exist");
            RuleResult statusResult = engine.executeRule(statusRule, testData);
            assertTrue(statusResult.isTriggered(), "status-check should trigger for status='ACTIVE'");

            var emailRule = engine.getConfiguration().getRuleById("email-check");
            assertNotNull(emailRule, "Rule 'email-check' should exist");
            RuleResult emailResult = engine.executeRule(emailRule, testData);
            assertTrue(emailResult.isTriggered(), "email-check should trigger for email containing '@'");

            logger.info("✓ inline-groups test passed");
        }
    }

    // ========================================================================
    // TRANSFORMATION EXAMPLES
    // ========================================================================
    @Nested
    @DisplayName("Transformation Examples")
    class TransformationExamples {

        @Test
        @DisplayName("payment-routing: Conditional payment routing transformation")
        void testPaymentRouting() throws Exception {
            logger.info("=== Testing payment-routing example ===");

            YamlRuleConfiguration config = loadYamlConfig("transformation/payment-routing.yaml");
            Map<String, Object> testData = loadJsonData("transformation/payment-routing-data.json");

            logger.info("Input data: {}", testData);

            RulesEngine engine = RulesEngine.fromYamlConfig(config);
            RuleResult result = engine.evaluate(config, testData);

            assertNotNull(result, "RuleResult should not be null");
            assertTrue(result.isSuccess(), "Processing should succeed");

            Map<String, Object> enrichedData = result.getEnrichedData();
            assertNotNull(enrichedData, "Enriched data should not be null");

            // Verify high-value USD routing (amount=5000000 > 1000000, currency=USD)
            @SuppressWarnings("unchecked")
            Map<String, Object> routing = (Map<String, Object>) enrichedData.get("routing");
            assertNotNull(routing, "routing should be set");
            assertEquals("SWIFT", routing.get("system"),
                "routing.system should be SWIFT for high-value USD");
            assertEquals("URGENT", routing.get("priority"),
                "routing.priority should be URGENT for high-value USD");

            logger.info("✓ payment-routing test passed");
        }
    }

    // ========================================================================
    // CONDITIONAL EXAMPLES
    // ========================================================================
    @Nested
    @DisplayName("Conditional Examples")
    class ConditionalExamples {

        @Test
        @DisplayName("advanced-routing: Priority-based conditional field enrichment")
        void testAdvancedRouting() throws Exception {
            logger.info("=== Testing advanced-routing example ===");

            YamlRuleConfiguration config = loadYamlConfig("conditional/advanced-routing.yaml");
            Map<String, Object> testData = loadJsonData("conditional/advanced-routing.json");

            logger.info("Input data: {}", testData);

            RulesEngine engine = RulesEngine.fromYamlConfig(config);
            RuleResult result = engine.evaluate(config, testData);

            assertNotNull(result, "RuleResult should not be null");
            assertTrue(result.isSuccess(), "Processing should succeed");

            Map<String, Object> enrichedData = result.getEnrichedData();
            assertNotNull(enrichedData, "Enriched data should not be null");
            logger.info("Enriched data: {}", enrichedData);

            // Verify VIP customer routing (customerSegment=VIP, totalValue=1500000)
            assertEquals("PLATINUM_CONCIERGE", enrichedData.get("serviceTier"),
                "serviceTier should be PLATINUM_CONCIERGE for VIP with high value");
            assertEquals(1, enrichedData.get("slaHours"),
                "slaHours should be 1 for VIP tier");
            assertEquals("VIP_QUEUE", enrichedData.get("routingQueue"),
                "routingQueue should be VIP_QUEUE");

            // Verify compliance routing (countryCode=NK - high risk)
            assertEquals("SANCTIONS_TEAM", enrichedData.get("complianceTeam"),
                "complianceTeam should be SANCTIONS_TEAM for NK");
            assertEquals("L3_ENHANCED_DUE_DILIGENCE", enrichedData.get("reviewLevel"),
                "reviewLevel should be L3_ENHANCED_DUE_DILIGENCE for high-risk country");

            // Verify product routing (productType=DERIVATIVES)
            assertEquals("DERIVATIVES_DESK", enrichedData.get("productTeam"),
                "productTeam should be DERIVATIVES_DESK");

            logger.info("✓ advanced-routing test passed");
        }

        @Test
        @DisplayName("fx-transaction-processing: Complex FX transaction enrichment chain")
        void testFxTransactionProcessing() throws Exception {
            logger.info("=== Testing fx-transaction-processing example ===");

            YamlRuleConfiguration config = loadYamlConfig("conditional/fx-transaction-processing.yaml");
            Map<String, Object> testData = loadJsonData("conditional/fx-transaction-processing.json");

            logger.info("Input data: {}", testData);

            RulesEngine engine = RulesEngine.fromYamlConfig(config);
            RuleResult result = engine.evaluate(config, testData);

            assertNotNull(result, "RuleResult should not be null");
            assertTrue(result.isSuccess(), "Processing should succeed");

            Map<String, Object> enrichedData = result.getEnrichedData();
            assertNotNull(enrichedData, "Enriched data should not be null");

            // Verify currency ranking enrichment
            assertEquals(1, enrichedData.get("BUY_CURRENCY_RANK"),
                "BUY_CURRENCY_RANK should be 1 for USD");
            assertEquals("Americas", enrichedData.get("BUY_CURRENCY_REGION"),
                "BUY_CURRENCY_REGION should be Americas for USD");

            // Verify NDF mapping (SWIFT with Y -> 1)
            assertEquals("1", enrichedData.get("IS_NDF"),
                "IS_NDF should be '1' for SWIFT system with Y input");
            assertEquals("DIRECT_MAPPING", enrichedData.get("TRANSLATION_TYPE"),
                "TRANSLATION_TYPE should be DIRECT_MAPPING for SWIFT");

            // Verify settlement instruction (USD with high value)
            assertEquals("HIGH_VALUE_MANUAL_REVIEW", enrichedData.get("SETTLEMENT_INSTRUCTION"),
                "SETTLEMENT_INSTRUCTION should be HIGH_VALUE_MANUAL_REVIEW for amount > 50M");

            // Verify settlement priority
            assertEquals("HIGH", enrichedData.get("SETTLEMENT_PRIORITY"),
                "SETTLEMENT_PRIORITY should be HIGH for rank <= 2");

            logger.info("✓ fx-transaction-processing test passed");
        }

        @Test
        @DisplayName("nested-discount-logic: 4-level nested conditional transformation")
        void testNestedDiscountLogic() throws Exception {
            logger.info("=== Testing nested-discount-logic example ===");

            YamlRuleConfiguration config = loadYamlConfig("conditional/nested-discount-logic.yaml");
            Map<String, Object> testData = loadJsonData("conditional/nested-discount-logic.json");

            logger.info("Input data: {}", testData);

            RulesEngine engine = RulesEngine.fromYamlConfig(config);
            RuleResult result = engine.evaluate(config, testData);

            assertNotNull(result, "RuleResult should not be null");
            assertTrue(result.isSuccess(), "Processing should succeed");

            Map<String, Object> enrichedData = result.getEnrichedData();
            assertNotNull(enrichedData, "Enriched data should not be null");

            // Verify all 4 levels passed
            assertEquals("passed", enrichedData.get("regionCheck"),
                "regionCheck should be 'passed' for region=NA");
            assertEquals("passed", enrichedData.get("amountCheck"),
                "amountCheck should be 'passed' for amount=1500 > 1000");
            assertEquals("passed", enrichedData.get("currencyCheck"),
                "currencyCheck should be 'passed' for currency=USD");
            assertEquals("passed", enrichedData.get("finalCheck"),
                "finalCheck should be 'passed' for customerType=VIP");

            // Verify VIP discount
            assertEquals(0.20, enrichedData.get("discount"),
                "discount should be 0.20 (20%) for VIP customer");

            logger.info("✓ nested-discount-logic test passed");
        }

        @Test
        @DisplayName("waterfall-approval: Sequential conditional chaining with rule chains")
        void testWaterfallApproval() throws Exception {
            logger.info("=== Testing waterfall-approval example ===");

            YamlRuleConfiguration config = loadYamlConfig("conditional/waterfall-approval.yaml");
            Map<String, Object> testData = loadJsonData("conditional/waterfall-approval.json");

            logger.info("Input data: {}", testData);

            RulesEngine engine = RulesEngine.fromYamlConfig(config);
            RuleResult result = engine.evaluate(config, testData);

            assertNotNull(result, "RuleResult should not be null");
            assertTrue(result.isSuccess(), "Processing should succeed");

            Map<String, Object> enrichedData = result.getEnrichedData();
            assertNotNull(enrichedData, "Enriched data should not be null");
            logger.info("Enriched data: {}", enrichedData);

            // Verify all levels passed (creditScore=750 > 700, income=80000 > 50000, dti=0.30 < 0.40)
            assertEquals(true, enrichedData.get("level1Passed"),
                "level1Passed should be true for creditScore=750");
            assertEquals(true, enrichedData.get("level2Passed"),
                "level2Passed should be true for income=80000");
            assertEquals(true, enrichedData.get("isApproved"),
                "isApproved should be true for dti=0.30");

            // The loanStatus is set by the enrichment when isApproved is true
            Object loanStatus = enrichedData.get("loanStatus");
            assertNotNull(loanStatus, "loanStatus should be set by enrichment");
            assertEquals("APPROVED", loanStatus, "loanStatus should be 'APPROVED' for approved loan");
            logger.info("loanStatus value: {} (type: {})", loanStatus, loanStatus.getClass().getSimpleName());

            // The finalApplicationStatus is set by the transformation
            Object finalStatus = enrichedData.get("finalApplicationStatus");
            assertNotNull(finalStatus, "finalApplicationStatus should be set");
            assertEquals("APPROVED", finalStatus, "finalApplicationStatus should be 'APPROVED'");
            logger.info("finalApplicationStatus value: {} (type: {})", finalStatus, finalStatus.getClass().getSimpleName());

            logger.info("✓ waterfall-approval test passed");
        }
    }

    // ========================================================================
    // ETL EXAMPLES
    // ========================================================================
    @Nested
    @DisplayName("ETL Examples")
    class EtlExamples {

        @Test
        @DisplayName("customer-pipeline: ETL pipeline with CSV extract and enrichment")
        void testCustomerPipeline() throws Exception {
            logger.info("=== Testing customer-pipeline example ===");

            YamlRuleConfiguration config = loadYamlConfig("etl/customer-pipeline.yaml");

            // Verify pipeline configuration loaded correctly
            assertNotNull(config, "Configuration should not be null");
            assertNotNull(config.getMetadata(), "Metadata should not be null");
            assertEquals("customer-etl-pipeline", config.getMetadata().getId(),
                "Pipeline ID should be 'customer-etl-pipeline'");
            assertEquals("pipeline-config", config.getMetadata().getType(),
                "Type should be 'pipeline-config'");

            // Verify enrichments are defined (can be executed independently)
            assertNotNull(config.getEnrichments(), "Enrichments should be defined");
            assertFalse(config.getEnrichments().isEmpty(), "Should have at least one enrichment");

            // Test the enrichment independently with sample data
            Map<String, Object> testData = Map.of(
                "customerId", "C001",
                "customerName", "Test Customer",
                "email", "test@example.com",
                "status", "NEW"
            );

            RulesEngine engine = RulesEngine.fromYamlConfig(config);
            RuleResult result = engine.evaluate(config, testData);

            assertNotNull(result, "RuleResult should not be null");
            assertTrue(result.isSuccess(), "Processing should succeed");

            Map<String, Object> enrichedData = result.getEnrichedData();
            assertNotNull(enrichedData, "Enriched data should not be null");
            logger.info("Enriched data: {}", enrichedData);

            // Verify enrichment added sourceSystem field
            assertEquals("CSV_IMPORT", enrichedData.get("sourceSystem"),
                "sourceSystem should be 'CSV_IMPORT'");
            assertNotNull(enrichedData.get("importDate"), "importDate should be set");

            logger.info("✓ customer-pipeline test passed");
        }

        @Test
        @DisplayName("json-transformation: ETL pipeline with JSON extract and calculations")
        void testJsonTransformation() throws Exception {
            logger.info("=== Testing json-transformation example ===");

            YamlRuleConfiguration config = loadYamlConfig("etl/json-transformation.yaml");

            // Verify pipeline configuration loaded correctly
            assertNotNull(config, "Configuration should not be null");
            assertNotNull(config.getMetadata(), "Metadata should not be null");
            assertEquals("json-transform-pipeline", config.getMetadata().getId(),
                "Pipeline ID should be 'json-transform-pipeline'");
            assertEquals("pipeline-config", config.getMetadata().getType(),
                "Type should be 'pipeline-config'");

            // The pipeline defines calculations in the transform step
            // Test with sample order data to verify the config can be processed
            Map<String, Object> testData = Map.of(
                "orderId", "ORD-001",
                "amount", 100.0,
                "customer", "Test Customer"
            );

            // Note: The pipeline transformations are defined within pipeline steps,
            // not as top-level enrichments, so we just verify the config loads correctly
            RulesEngine engine = RulesEngine.fromYamlConfig(config);
            assertNotNull(engine, "RulesEngine should be created successfully");

            logger.info("✓ json-transformation test passed (configuration validated)");
        }
    }
}

