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
 * FieldTransformationTest - JUnit 5 Test for Field Transformation Operations
 *
 * This test validates comprehensive field transformation functionality using real APEX services:
 * - Data type conversions and transformations
 * - Field mapping and renaming operations
 * - Value transformations and formatting
 * - Multi-field transformation patterns
 * - Expression-based field transformations
 * - Complex field mapping scenarios
 *
 * REAL APEX SERVICES TESTED:
 * - EnrichmentService: Real APEX enrichment processor for field transformations
 * - YamlConfigurationLoader: Real YAML configuration loading and validation
 * - ExpressionEvaluatorService: Real field transformation expression evaluation
 *
 * CRITICAL VALIDATION CHECKLIST:
 * ✅ Count enrichments in YAML - Each test expects specific number of enrichments
 * ✅ Verify log shows "Processed: X out of X" - Must be 100% execution rate
 * ✅ Check EVERY field mapping - Test data triggers ALL transformations
 * ✅ Validate EVERY transformation result - Test actual field transformation logic
 * ✅ Assert ALL transformed fields - Every target-field has corresponding assertEquals
 */
public class FieldTransformationTest extends DemoTestBase {

    private static final Logger logger = LoggerFactory.getLogger(FieldTransformationTest.class);

    @Test
    void testBasicFieldMappings() {
        logger.info("=== Testing Basic Field Mappings ===");
        
        try {
            // Load YAML configuration for basic field mappings
            YamlRuleConfiguration config = yamlLoader.loadFromClasspath("yaml/field-basic-mappings-test.yaml");
            assertNotNull(config, "Configuration should be loaded successfully");
            logger.info("✅ Configuration loaded successfully: " + config.getMetadata().getName());
            
            // Test data with various field types
            Map<String, Object> inputData = new HashMap<>();
            inputData.put("firstName", "john");
            inputData.put("lastName", "smith");
            inputData.put("email", "JOHN.SMITH@EXAMPLE.COM");
            inputData.put("phoneNumber", "1234567890");
            inputData.put("accountBalance", 25000.50);
            inputData.put("isActive", true);
            
            logger.info("Input data: " + inputData);
            
            // Process enrichments using real APEX services
            Object result = enrichmentService.enrichObject(config, inputData);
            @SuppressWarnings("unchecked")
            Map<String, Object> enrichedData = (Map<String, Object>) result;
            assertNotNull(enrichedData, "Enriched data should not be null");
            
            // Validate basic field mapping results
            String fullName = (String) enrichedData.get("fullName");
            assertNotNull(fullName, "Full name should be mapped");
            assertEquals("JOHN SMITH", fullName, "Full name should be uppercase concatenation");
            
            String normalizedEmail = (String) enrichedData.get("normalizedEmail");
            assertNotNull(normalizedEmail, "Normalized email should be mapped");
            assertEquals("john.smith@example.com", normalizedEmail, "Email should be lowercase");
            
            String formattedPhone = (String) enrichedData.get("formattedPhone");
            assertNotNull(formattedPhone, "Formatted phone should be mapped");
            assertEquals("(123) 456-7890", formattedPhone, "Phone should be formatted");
            
            String balanceCategory = (String) enrichedData.get("balanceCategory");
            assertNotNull(balanceCategory, "Balance category should be mapped");
            assertEquals("MEDIUM", balanceCategory, "Should be MEDIUM for 25k balance");
            
            String statusText = (String) enrichedData.get("statusText");
            assertNotNull(statusText, "Status text should be mapped");
            assertEquals("ACTIVE", statusText, "Should be ACTIVE for true boolean");
            
            logger.info("Basic field mapping results: " + enrichedData);
            logger.info("✅ Basic field mappings test completed successfully");
            
        } catch (Exception e) {
            logger.error("❌ Basic field mappings test failed", e);
            fail("Basic field mappings test failed: " + e.getMessage());
        }
    }

    @Test
    void testDataTypeConversions() {
        logger.info("=== Testing Data Type Conversions ===");
        
        try {
            // Load YAML configuration for data type conversions
            YamlRuleConfiguration config = yamlLoader.loadFromClasspath("yaml/field-type-conversions-test.yaml");
            assertNotNull(config, "Configuration should be loaded successfully");
            logger.info("✅ Configuration loaded successfully: " + config.getMetadata().getName());
            
            // Test data with various data types
            Map<String, Object> inputData = new HashMap<>();
            inputData.put("stringNumber", "12345");
            inputData.put("stringDecimal", "123.45");
            inputData.put("stringBoolean", "true");
            inputData.put("numberString", 98765);
            inputData.put("decimalString", 456.78);
            inputData.put("booleanString", false);
            
            logger.info("Input data: " + inputData);
            
            // Process enrichments using real APEX services
            Object result = enrichmentService.enrichObject(config, inputData);
            @SuppressWarnings("unchecked")
            Map<String, Object> enrichedData = (Map<String, Object>) result;
            assertNotNull(enrichedData, "Enriched data should not be null");
            
            // Validate data type conversion results
            String convertedNumber = (String) enrichedData.get("convertedNumber");
            assertNotNull(convertedNumber, "Converted number should be calculated");
            assertEquals("12345", convertedNumber, "String number should be preserved as string");
            
            String convertedDecimal = (String) enrichedData.get("convertedDecimal");
            assertNotNull(convertedDecimal, "Converted decimal should be calculated");
            assertTrue(convertedDecimal.contains("123.45"), "String decimal should be formatted");
            
            String convertedBoolean = (String) enrichedData.get("convertedBoolean");
            assertNotNull(convertedBoolean, "Converted boolean should be calculated");
            assertEquals("TRUE", convertedBoolean, "String boolean should be uppercase");
            
            String numberAsString = (String) enrichedData.get("numberAsString");
            assertNotNull(numberAsString, "Number as string should be calculated");
            assertEquals("98765", numberAsString, "Number should be converted to string");
            
            String decimalFormatted = (String) enrichedData.get("decimalFormatted");
            assertNotNull(decimalFormatted, "Decimal formatted should be calculated");
            assertTrue(decimalFormatted.contains("456.78"), "Decimal should be formatted");
            
            String booleanText = (String) enrichedData.get("booleanText");
            assertNotNull(booleanText, "Boolean text should be calculated");
            assertEquals("FALSE", booleanText, "Boolean should be converted to uppercase text");
            
            logger.info("Data type conversion results: " + enrichedData);
            logger.info("✅ Data type conversions test completed successfully");
            
        } catch (Exception e) {
            logger.error("❌ Data type conversions test failed", e);
            fail("Data type conversions test failed: " + e.getMessage());
        }
    }

    @Test
    void testComplexTransformations() {
        logger.info("=== Testing Complex Transformations ===");
        
        try {
            // Load YAML configuration for complex transformations
            YamlRuleConfiguration config = yamlLoader.loadFromClasspath("yaml/field-complex-transformations-test.yaml");
            assertNotNull(config, "Configuration should be loaded successfully");
            logger.info("✅ Configuration loaded successfully: " + config.getMetadata().getName());
            
            // Test data with complex transformation scenarios
            Map<String, Object> inputData = new HashMap<>();
            inputData.put("customerName", "  john doe  ");
            inputData.put("accountNumber", "1234567890123456");
            inputData.put("transactionAmount", 15750.25);
            inputData.put("transactionDate", "2025-09-14");
            inputData.put("riskScore", 75);
            inputData.put("customerTier", "premium");
            
            logger.info("Input data: " + inputData);
            
            // Process enrichments using real APEX services
            Object result = enrichmentService.enrichObject(config, inputData);
            @SuppressWarnings("unchecked")
            Map<String, Object> enrichedData = (Map<String, Object>) result;
            assertNotNull(enrichedData, "Enriched data should not be null");
            
            // Validate complex transformation results
            String cleanedName = (String) enrichedData.get("cleanedName");
            assertNotNull(cleanedName, "Cleaned name should be transformed");
            assertEquals("JOHN DOE", cleanedName, "Name should be trimmed and uppercase");
            
            String maskedAccount = (String) enrichedData.get("maskedAccount");
            assertNotNull(maskedAccount, "Masked account should be transformed");
            assertTrue(maskedAccount.startsWith("****"), "Account should be masked");
            assertTrue(maskedAccount.endsWith("3456"), "Account should show last 4 digits");
            
            String formattedAmount = (String) enrichedData.get("formattedAmount");
            assertNotNull(formattedAmount, "Formatted amount should be transformed");
            assertTrue(formattedAmount.contains("15,750.25") || formattedAmount.contains("15750.25"), 
                      "Amount should be formatted with currency");
            
            String processedDate = (String) enrichedData.get("processedDate");
            assertNotNull(processedDate, "Processed date should be transformed");
            assertTrue(processedDate.contains("2025"), "Date should contain year");
            
            String riskCategory = (String) enrichedData.get("riskCategory");
            assertNotNull(riskCategory, "Risk category should be transformed");
            assertEquals("MEDIUM", riskCategory, "Risk score 75 should be MEDIUM");
            
            String tierStatus = (String) enrichedData.get("tierStatus");
            assertNotNull(tierStatus, "Tier status should be transformed");
            assertEquals("PREMIUM_CUSTOMER", tierStatus, "Premium tier should be transformed");
            
            logger.info("Complex transformation results: " + enrichedData);
            logger.info("✅ Complex transformations test completed successfully");
            
        } catch (Exception e) {
            logger.error("❌ Complex transformations test failed", e);
            fail("Complex transformations test failed: " + e.getMessage());
        }
    }

    @Test
    void testMultiFieldTransformations() {
        logger.info("=== Testing Multi-Field Transformations ===");
        
        try {
            // Load YAML configuration for multi-field transformations
            YamlRuleConfiguration config = yamlLoader.loadFromClasspath("yaml/field-multi-transformations-test.yaml");
            assertNotNull(config, "Configuration should be loaded successfully");
            logger.info("✅ Configuration loaded successfully: " + config.getMetadata().getName());
            
            // Test data with multiple related fields
            Map<String, Object> inputData = new HashMap<>();
            inputData.put("street", "123 Main St");
            inputData.put("city", "New York");
            inputData.put("state", "NY");
            inputData.put("zipCode", "10001");
            inputData.put("country", "USA");
            inputData.put("latitude", 40.7128);
            inputData.put("longitude", -74.0060);
            
            logger.info("Input data: " + inputData);
            
            // Process enrichments using real APEX services
            Object result = enrichmentService.enrichObject(config, inputData);
            @SuppressWarnings("unchecked")
            Map<String, Object> enrichedData = (Map<String, Object>) result;
            assertNotNull(enrichedData, "Enriched data should not be null");
            
            // Validate multi-field transformation results
            String fullAddress = (String) enrichedData.get("fullAddress");
            assertNotNull(fullAddress, "Full address should be constructed");
            assertTrue(fullAddress.contains("123 Main St"), "Address should contain street");
            assertTrue(fullAddress.contains("New York"), "Address should contain city");
            assertTrue(fullAddress.contains("NY"), "Address should contain state");
            assertTrue(fullAddress.contains("10001"), "Address should contain zip");
            
            String cityState = (String) enrichedData.get("cityState");
            assertNotNull(cityState, "City state should be constructed");
            assertEquals("New York, NY", cityState, "City state should be formatted");
            
            String coordinates = (String) enrichedData.get("coordinates");
            assertNotNull(coordinates, "Coordinates should be constructed");
            assertTrue(coordinates.contains("40.7128"), "Coordinates should contain latitude");
            assertTrue(coordinates.contains("-74.006"), "Coordinates should contain longitude");
            
            String locationSummary = (String) enrichedData.get("locationSummary");
            assertNotNull(locationSummary, "Location summary should be constructed");
            assertTrue(locationSummary.contains("New York"), "Summary should contain city");
            assertTrue(locationSummary.contains("USA"), "Summary should contain country");
            
            logger.info("Multi-field transformation results: " + enrichedData);
            logger.info("✅ Multi-field transformations test completed successfully");
            
        } catch (Exception e) {
            logger.error("❌ Multi-field transformations test failed", e);
            fail("Multi-field transformations test failed: " + e.getMessage());
        }
    }
}
