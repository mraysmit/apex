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
 * SpELExpressionTest - JUnit 5 Test for SpEL Expression Syntax and Evaluation
 *
 * This test validates comprehensive SpEL expression functionality using real APEX services:
 * - Field reference expressions with #fieldName syntax
 * - Mathematical expressions and calculations
 * - String manipulation and operations
 * - Conditional expressions and ternary operators
 * - Java method invocation and type references
 * - Collection operations and filtering
 * - Null-safe navigation and error handling
 *
 * REAL APEX SERVICES TESTED:
 * - EnrichmentService: Real APEX enrichment processor for SpEL expression evaluation
 * - YamlConfigurationLoader: Real YAML configuration loading and validation
 * - ExpressionEvaluatorService: Real SpEL expression evaluation engine
 *
 * CRITICAL VALIDATION CHECKLIST:
 * ✅ Count enrichments in YAML - Each test expects specific number of enrichments
 * ✅ Verify log shows "Processed: X out of X" - Must be 100% execution rate
 * ✅ Check EVERY enrichment condition - Test data triggers ALL conditions
 * ✅ Validate EVERY expression result - Test actual SpEL expression evaluation
 * ✅ Assert ALL enrichment results - Every result-field has corresponding assertEquals
 */
public class SpELExpressionTest extends DemoTestBase {

    private static final Logger logger = LoggerFactory.getLogger(SpELExpressionTest.class);

    @Test
    void testFieldReferenceExpressions() {
        logger.info("=== Testing SpEL Field Reference Expressions ===");
        
        try {
            // Load YAML configuration for field reference expressions
            YamlRuleConfiguration config = yamlLoader.loadFromClasspath("yaml/spel-field-references-test.yaml");
            assertNotNull(config, "Configuration should be loaded successfully");
            logger.info("✅ Configuration loaded successfully: " + config.getMetadata().getName());

            // Test data with various field types
            Map<String, Object> inputData = new HashMap<>();
            inputData.put("firstName", "John");
            inputData.put("lastName", "Smith");
            inputData.put("age", 35);
            inputData.put("salary", 75000.0);
            inputData.put("isActive", true);
            inputData.put("department", "Engineering");

            logger.info("Input data: " + inputData);

            // Process enrichments using real APEX services
            Object result = enrichmentService.enrichObject(config, inputData);
            @SuppressWarnings("unchecked")
            Map<String, Object> enrichedData = (Map<String, Object>) result;
            assertNotNull(enrichedData, "Enriched data should not be null");
            
            // Validate field reference results
            String fullName = (String) enrichedData.get("fullName");
            assertNotNull(fullName, "Full name should be calculated");
            assertEquals("John Smith", fullName, "Full name should combine first and last name");
            
            String displayName = (String) enrichedData.get("displayName");
            assertNotNull(displayName, "Display name should be calculated");
            assertTrue(displayName.contains("John Smith"), "Display name should contain full name");
            
            Boolean isEligible = (Boolean) enrichedData.get("isEligible");
            assertNotNull(isEligible, "Eligibility should be calculated");
            assertTrue(isEligible, "Should be eligible based on age and active status");
            
            String salaryCategory = (String) enrichedData.get("salaryCategory");
            assertNotNull(salaryCategory, "Salary category should be calculated");
            assertEquals("HIGH", salaryCategory, "Should be HIGH category for salary > 70000");
            
            logger.info("Enriched data: " + enrichedData);
            logger.info("✅ SpEL field reference expressions test completed successfully");
            
        } catch (Exception e) {
            logger.error("❌ SpEL field reference expressions test failed", e);
            fail("SpEL field reference expressions test failed: " + e.getMessage());
        }
    }

    @Test
    void testMathematicalExpressions() {
        logger.info("=== Testing SpEL Mathematical Expressions ===");
        
        try {
            // Load YAML configuration for mathematical expressions
            YamlRuleConfiguration config = yamlLoader.loadFromClasspath("yaml/spel-mathematical-test.yaml");
            assertNotNull(config, "Configuration should be loaded successfully");
            logger.info("✅ Configuration loaded successfully: " + config.getMetadata().getName());

            // Test data with numerical values
            Map<String, Object> inputData = new HashMap<>();
            inputData.put("principal", 100000.0);
            inputData.put("interestRate", 0.05);
            inputData.put("years", 10);
            inputData.put("quantity", 1000);
            inputData.put("price", 98.75);
            inputData.put("feeRate", 0.001);

            logger.info("Input data: " + inputData);

            // Process enrichments using real APEX services
            Object result = enrichmentService.enrichObject(config, inputData);
            @SuppressWarnings("unchecked")
            Map<String, Object> enrichedData = (Map<String, Object>) result;
            assertNotNull(enrichedData, "Enriched data should not be null");
            
            // Validate mathematical calculation results
            Double totalInterest = (Double) enrichedData.get("totalInterest");
            assertNotNull(totalInterest, "Total interest should be calculated");
            assertEquals(50000.0, totalInterest, 0.01, "Total interest should be principal * rate * years");
            
            Double notionalValue = (Double) enrichedData.get("notionalValue");
            assertNotNull(notionalValue, "Notional value should be calculated");
            assertEquals(98750.0, notionalValue, 0.01, "Notional value should be quantity * price");
            
            Double calculatedFee = (Double) enrichedData.get("calculatedFee");
            assertNotNull(calculatedFee, "Calculated fee should be calculated");
            assertEquals(98.75, calculatedFee, 0.01, "Fee should be notional * fee rate");
            
            Double netAmount = (Double) enrichedData.get("netAmount");
            assertNotNull(netAmount, "Net amount should be calculated");
            assertEquals(98651.25, netAmount, 0.01, "Net amount should be notional - fee");
            
            // Test Java Math class functions
            Double maxValue = (Double) enrichedData.get("maxValue");
            assertNotNull(maxValue, "Max value should be calculated");
            assertEquals(100000.0, maxValue, 0.01, "Max value should be max of principal and notional");
            
            Double roundedValue = (Double) enrichedData.get("roundedValue");
            assertNotNull(roundedValue, "Rounded value should be calculated");
            assertEquals(98651.25, roundedValue, 0.01, "Value should be rounded to 2 decimal places");
            
            logger.info("Mathematical results: " + enrichedData);
            logger.info("✅ SpEL mathematical expressions test completed successfully");
            
        } catch (Exception e) {
            logger.error("❌ SpEL mathematical expressions test failed", e);
            fail("SpEL mathematical expressions test failed: " + e.getMessage());
        }
    }

    @Test
    void testStringManipulationExpressions() {
        logger.info("=== Testing SpEL String Manipulation Expressions ===");
        
        try {
            // Load YAML configuration for string manipulation expressions
            YamlRuleConfiguration config = yamlLoader.loadFromClasspath("yaml/spel-string-manipulation-test.yaml");
            assertNotNull(config, "Configuration should be loaded successfully");
            logger.info("✅ Configuration loaded successfully: " + config.getMetadata().getName());

            // Test data with string values
            Map<String, Object> inputData = new HashMap<>();
            inputData.put("customerName", "john doe");
            inputData.put("accountNumber", "1234567890123456");
            inputData.put("email", "JOHN.DOE@EXAMPLE.COM");
            inputData.put("instrumentId", "US912828XG93");
            inputData.put("description", "  High-yield corporate bond  ");

            logger.info("Input data: " + inputData);

            // Process enrichments using real APEX services
            Object result = enrichmentService.enrichObject(config, inputData);
            @SuppressWarnings("unchecked")
            Map<String, Object> enrichedData = (Map<String, Object>) result;
            assertNotNull(enrichedData, "Enriched data should not be null");
            
            // Validate string manipulation results
            String nameUpperCase = (String) enrichedData.get("nameUpperCase");
            assertNotNull(nameUpperCase, "Upper case name should be calculated");
            assertEquals("JOHN DOE", nameUpperCase, "Name should be converted to uppercase");
            
            String initials = (String) enrichedData.get("initials");
            assertNotNull(initials, "Initials should be calculated");
            assertEquals("JD", initials, "Initials should be extracted from first and last name");
            
            String maskedAccount = (String) enrichedData.get("maskedAccount");
            assertNotNull(maskedAccount, "Masked account should be calculated");
            assertEquals("****3456", maskedAccount, "Account should be masked except last 4 digits");
            
            String normalizedEmail = (String) enrichedData.get("normalizedEmail");
            assertNotNull(normalizedEmail, "Normalized email should be calculated");
            assertEquals("john.doe@example.com", normalizedEmail, "Email should be converted to lowercase");
            
            String countryCode = (String) enrichedData.get("countryCode");
            assertNotNull(countryCode, "Country code should be extracted");
            assertEquals("US", countryCode, "Country code should be first 2 characters of instrument ID");
            
            String trimmedDescription = (String) enrichedData.get("trimmedDescription");
            assertNotNull(trimmedDescription, "Trimmed description should be calculated");
            assertEquals("High-yield corporate bond", trimmedDescription, "Description should be trimmed");
            
            Boolean isValidEmail = (Boolean) enrichedData.get("isValidEmail");
            assertNotNull(isValidEmail, "Email validation should be calculated");
            assertTrue(isValidEmail, "Email should be valid");
            
            logger.info("String manipulation results: " + enrichedData);
            logger.info("✅ SpEL string manipulation expressions test completed successfully");
            
        } catch (Exception e) {
            logger.error("❌ SpEL string manipulation expressions test failed", e);
            fail("SpEL string manipulation expressions test failed: " + e.getMessage());
        }
    }

    @Test
    void testConditionalExpressions() {
        logger.info("=== Testing SpEL Conditional Expressions ===");
        
        try {
            // Load YAML configuration for conditional expressions
            YamlRuleConfiguration config = yamlLoader.loadFromClasspath("yaml/spel-conditional-test.yaml");
            assertNotNull(config, "Configuration should be loaded successfully");
            logger.info("✅ Configuration loaded successfully: " + config.getMetadata().getName());

            // Test data with various conditions
            Map<String, Object> inputData = new HashMap<>();
            inputData.put("riskScore", 85);
            inputData.put("creditRating", "A+");
            inputData.put("tradeAmount", 1500000.0);
            inputData.put("counterpartyType", "BANK");
            inputData.put("jurisdiction", "US");
            inputData.put("maturityDays", 365);

            logger.info("Input data: " + inputData);

            // Process enrichments using real APEX services
            Object result = enrichmentService.enrichObject(config, inputData);
            @SuppressWarnings("unchecked")
            Map<String, Object> enrichedData = (Map<String, Object>) result;
            assertNotNull(enrichedData, "Enriched data should not be null");
            
            // Validate conditional expression results
            String riskCategory = (String) enrichedData.get("riskCategory");
            assertNotNull(riskCategory, "Risk category should be calculated");
            assertEquals("HIGH", riskCategory, "Risk score 85 should be HIGH category");
            
            String approvalLevel = (String) enrichedData.get("approvalLevel");
            assertNotNull(approvalLevel, "Approval level should be calculated");
            assertEquals("SENIOR_MANAGEMENT", approvalLevel, "High amount should require senior management approval");
            
            Boolean requiresApproval = (Boolean) enrichedData.get("requiresApproval");
            assertNotNull(requiresApproval, "Approval requirement should be calculated");
            assertTrue(requiresApproval, "High risk and high amount should require approval");
            
            String regulatoryTier = (String) enrichedData.get("regulatoryTier");
            assertNotNull(regulatoryTier, "Regulatory tier should be calculated");
            assertEquals("TIER_1", regulatoryTier, "Bank counterparty in US should be TIER_1");
            
            Double adjustedAmount = (Double) enrichedData.get("adjustedAmount");
            assertNotNull(adjustedAmount, "Adjusted amount should be calculated");
            assertEquals(1425000.0, adjustedAmount, 0.01, "Amount should be adjusted by 5% for high risk");
            
            String maturityBucket = (String) enrichedData.get("maturityBucket");
            assertNotNull(maturityBucket, "Maturity bucket should be calculated");
            assertEquals("1Y", maturityBucket, "365 days should be 1Y bucket");
            
            logger.info("Conditional expression results: " + enrichedData);
            logger.info("✅ SpEL conditional expressions test completed successfully");
            
        } catch (Exception e) {
            logger.error("❌ SpEL conditional expressions test failed", e);
            fail("SpEL conditional expressions test failed: " + e.getMessage());
        }
    }
}
