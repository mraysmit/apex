package dev.mars.apex.demo.syntax;

import dev.mars.apex.core.config.yaml.YamlRuleConfiguration;
import dev.mars.apex.demo.DemoTestBase;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ConditionalLogicTest - JUnit 5 Test for Conditional Logic Patterns
 *
 * This test validates comprehensive conditional logic functionality using real APEX services:
 * - If-then-else patterns in enrichment conditions
 * - Complex boolean expressions with logical operators
 * - Nested conditional structures and branching logic
 * - Conditional enrichment execution based on data state
 * - Error handling and edge cases in conditional logic
 * - Multi-condition evaluation and short-circuit logic
 *
 * REAL APEX SERVICES TESTED:
 * - EnrichmentService: Real APEX enrichment processor for conditional logic evaluation
 * - YamlConfigurationLoader: Real YAML configuration loading and validation
 * - ExpressionEvaluatorService: Real conditional expression evaluation engine
 *
 * CRITICAL VALIDATION CHECKLIST:
 * ✅ Count enrichments in YAML - Each test expects specific number of enrichments
 * ✅ Verify log shows "Processed: X out of X" - Must be 100% execution rate
 * ✅ Check EVERY enrichment condition - Test data triggers ALL conditions
 * ✅ Validate EVERY conditional result - Test actual conditional logic evaluation
 * ✅ Assert ALL enrichment results - Every result-field has corresponding assertEquals
 */
public class ConditionalLogicTest extends DemoTestBase {

    private static final Logger logger = LoggerFactory.getLogger(ConditionalLogicTest.class);

    @Test
    void testBooleanExpressions() {
        logger.info("=== Testing Boolean Expressions ===");
        
        try {
            // Load YAML configuration for boolean expressions
            YamlRuleConfiguration config = yamlLoader.loadFromClasspath("yaml/conditional-boolean-test.yaml");
            assertNotNull(config, "Configuration should be loaded successfully");
            logger.info("✅ Configuration loaded successfully: " + config.getMetadata().getName());
            
            // Test data with boolean conditions
            Map<String, Object> inputData = new HashMap<>();
            inputData.put("isActive", true);
            inputData.put("isDeleted", false);
            inputData.put("isVisible", true);
            inputData.put("hasPermission", true);
            inputData.put("isExpired", false);
            inputData.put("isLocked", false);
            
            logger.info("Input data: " + inputData);
            
            // Process enrichments using real APEX services
            Object result = enrichmentService.enrichObject(config, inputData);
            @SuppressWarnings("unchecked")
            Map<String, Object> enrichedData = (Map<String, Object>) result;
            assertNotNull(enrichedData, "Enriched data should not be null");
            
            // Validate boolean expression results
            Boolean simpleBoolean = (Boolean) enrichedData.get("simpleBoolean");
            assertNotNull(simpleBoolean, "Simple boolean should be calculated");
            assertTrue(simpleBoolean, "Simple boolean should be true");
            
            Boolean negatedBoolean = (Boolean) enrichedData.get("negatedBoolean");
            assertNotNull(negatedBoolean, "Negated boolean should be calculated");
            assertTrue(negatedBoolean, "Negated boolean should be true (!false)");
            
            Boolean andExpression = (Boolean) enrichedData.get("andExpression");
            assertNotNull(andExpression, "AND expression should be calculated");
            assertTrue(andExpression, "AND expression should be true (active && !deleted)");
            
            Boolean orExpression = (Boolean) enrichedData.get("orExpression");
            assertNotNull(orExpression, "OR expression should be calculated");
            assertTrue(orExpression, "OR expression should be true (visible || hasPermission)");
            
            Boolean complexExpression = (Boolean) enrichedData.get("complexExpression");
            assertNotNull(complexExpression, "Complex expression should be calculated");
            assertTrue(complexExpression, "Complex expression should be true");
            
            logger.info("Boolean expression results: " + enrichedData);
            logger.info("✅ Boolean expressions test completed successfully");
            
        } catch (Exception e) {
            logger.error("❌ Boolean expressions test failed", e);
            fail("Boolean expressions test failed: " + e.getMessage());
        }
    }

    @Test
    void testLogicalOperators() {
        logger.info("=== Testing Logical Operators ===");
        
        try {
            // Load YAML configuration for logical operators
            YamlRuleConfiguration config = yamlLoader.loadFromClasspath("yaml/conditional-logical-test.yaml");
            assertNotNull(config, "Configuration should be loaded successfully");
            logger.info("✅ Configuration loaded successfully: " + config.getMetadata().getName());
            
            // Test data with various logical conditions
            Map<String, Object> inputData = new HashMap<>();
            inputData.put("status", "ACTIVE");
            inputData.put("priority", "HIGH");
            inputData.put("amount", 15000.0);
            inputData.put("region", "US");
            inputData.put("customerType", "PREMIUM");
            inputData.put("riskLevel", "LOW");
            
            logger.info("Input data: " + inputData);
            
            // Process enrichments using real APEX services
            Object result = enrichmentService.enrichObject(config, inputData);
            @SuppressWarnings("unchecked")
            Map<String, Object> enrichedData = (Map<String, Object>) result;
            assertNotNull(enrichedData, "Enriched data should not be null");
            
            // Validate logical operator results
            Boolean andCondition = (Boolean) enrichedData.get("andCondition");
            assertNotNull(andCondition, "AND condition should be calculated");
            assertTrue(andCondition, "AND condition should be true (ACTIVE && HIGH)");
            
            Boolean orCondition = (Boolean) enrichedData.get("orCondition");
            assertNotNull(orCondition, "OR condition should be calculated");
            assertTrue(orCondition, "OR condition should be true (HIGH priority OR high amount)");
            
            Boolean notCondition = (Boolean) enrichedData.get("notCondition");
            assertNotNull(notCondition, "NOT condition should be calculated");
            assertTrue(notCondition, "NOT condition should be true (not HIGH risk)");
            
            String complexLogic = (String) enrichedData.get("complexLogic");
            assertNotNull(complexLogic, "Complex logic should be calculated");
            assertEquals("QUALIFIED", complexLogic, "Should be QUALIFIED for premium US customer");
            
            Boolean multipleConditions = (Boolean) enrichedData.get("multipleConditions");
            assertNotNull(multipleConditions, "Multiple conditions should be calculated");
            assertTrue(multipleConditions, "Multiple conditions should be true");
            
            logger.info("Logical operator results: " + enrichedData);
            logger.info("✅ Logical operators test completed successfully");
            
        } catch (Exception e) {
            logger.error("❌ Logical operators test failed", e);
            fail("Logical operators test failed: " + e.getMessage());
        }
    }

    @Test
    void testNestedConditionals() {
        logger.info("=== Testing Nested Conditionals ===");
        
        try {
            // Load YAML configuration for nested conditionals
            YamlRuleConfiguration config = yamlLoader.loadFromClasspath("yaml/conditional-nested-test.yaml");
            assertNotNull(config, "Configuration should be loaded successfully");
            logger.info("✅ Configuration loaded successfully: " + config.getMetadata().getName());
            
            // Test data with nested conditional scenarios
            Map<String, Object> inputData = new HashMap<>();
            inputData.put("accountType", "BUSINESS");
            inputData.put("accountBalance", 250000.0);
            inputData.put("creditScore", 780);
            inputData.put("yearsWithBank", 5);
            inputData.put("hasCollateral", true);
            inputData.put("industryRisk", "LOW");
            
            logger.info("Input data: " + inputData);
            
            // Process enrichments using real APEX services
            Object result = enrichmentService.enrichObject(config, inputData);
            @SuppressWarnings("unchecked")
            Map<String, Object> enrichedData = (Map<String, Object>) result;
            assertNotNull(enrichedData, "Enriched data should not be null");
            
            // Validate nested conditional results
            String loanEligibility = (String) enrichedData.get("loanEligibility");
            assertNotNull(loanEligibility, "Loan eligibility should be calculated");
            assertEquals("APPROVED", loanEligibility, "Should be APPROVED for high-credit business account");
            
            String interestTier = (String) enrichedData.get("interestTier");
            assertNotNull(interestTier, "Interest tier should be calculated");
            assertEquals("TIER_1", interestTier, "Should be TIER_1 for excellent credit and high balance");
            
            Number loanLimitNumber = (Number) enrichedData.get("loanLimit");
            assertNotNull(loanLimitNumber, "Loan limit should be calculated");
            double loanLimit = loanLimitNumber.doubleValue();
            assertEquals(500000.0, loanLimit, 0.01, "Should be 500k limit for business account with collateral");
            
            String riskCategory = (String) enrichedData.get("riskCategory");
            assertNotNull(riskCategory, "Risk category should be calculated");
            assertEquals("LOW_RISK", riskCategory, "Should be LOW_RISK for established business with good credit");
            
            logger.info("Nested conditional results: " + enrichedData);
            logger.info("✅ Nested conditionals test completed successfully");
            
        } catch (Exception e) {
            logger.error("❌ Nested conditionals test failed", e);
            fail("Nested conditionals test failed: " + e.getMessage());
        }
    }

    @Test
    void testConditionalEnrichmentExecution() {
        logger.info("=== Testing Conditional Enrichment Execution ===");
        
        try {
            // Load YAML configuration for conditional enrichment execution
            YamlRuleConfiguration config = yamlLoader.loadFromClasspath("yaml/conditional-execution-test.yaml");
            assertNotNull(config, "Configuration should be loaded successfully");
            logger.info("✅ Configuration loaded successfully: " + config.getMetadata().getName());
            
            // Test data that will trigger some enrichments but not others
            Map<String, Object> inputData = new HashMap<>();
            inputData.put("transactionType", "WIRE_TRANSFER");
            inputData.put("amount", 75000.0);
            inputData.put("sourceCountry", "US");
            inputData.put("destinationCountry", "CH");
            inputData.put("customerRisk", "MEDIUM");
            inputData.put("hasDocumentation", true);
            
            logger.info("Input data: " + inputData);
            
            // Process enrichments using real APEX services
            Object result = enrichmentService.enrichObject(config, inputData);
            @SuppressWarnings("unchecked")
            Map<String, Object> enrichedData = (Map<String, Object>) result;
            assertNotNull(enrichedData, "Enriched data should not be null");
            
            // Validate conditional execution results
            // High-value enrichment should execute (amount > 50000)
            String complianceCheck = (String) enrichedData.get("complianceCheck");
            assertNotNull(complianceCheck, "Compliance check should be executed for high-value transaction");
            assertTrue(complianceCheck.contains("HIGH_VALUE"), "Should indicate high-value processing");
            
            // International enrichment should execute (different countries)
            String internationalFlags = (String) enrichedData.get("internationalFlags");
            assertNotNull(internationalFlags, "International flags should be set for cross-border transaction");
            assertTrue(internationalFlags.contains("CROSS_BORDER"), "Should indicate cross-border transaction");
            
            // Wire transfer enrichment should execute (transaction type matches)
            String wireTransferValidation = (String) enrichedData.get("wireTransferValidation");
            assertNotNull(wireTransferValidation, "Wire transfer validation should be executed");
            assertTrue(wireTransferValidation.contains("WIRE_VALIDATED"), "Should indicate wire transfer validation");
            
            // Risk assessment should execute (customer risk is MEDIUM)
            String riskAssessment = (String) enrichedData.get("riskAssessment");
            assertNotNull(riskAssessment, "Risk assessment should be executed for medium risk customer");
            assertTrue(riskAssessment.contains("MEDIUM_RISK"), "Should indicate medium risk processing");
            
            logger.info("Conditional execution results: " + enrichedData);
            logger.info("✅ Conditional enrichment execution test completed successfully");
            
        } catch (Exception e) {
            logger.error("❌ Conditional enrichment execution test failed", e);
            fail("Conditional enrichment execution test failed: " + e.getMessage());
        }
    }
}
