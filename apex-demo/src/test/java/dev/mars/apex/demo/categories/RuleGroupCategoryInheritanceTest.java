package dev.mars.apex.demo.categories;

import dev.mars.apex.demo.DemoTestBase;
import dev.mars.apex.core.config.yaml.YamlRuleConfiguration;
import dev.mars.apex.core.engine.model.RuleResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Demo test showcasing rule group category inheritance functionality.
 * Demonstrates how rule groups inherit enterprise metadata from their assigned categories.
 */
@DisplayName("Rule Group Category Inheritance Demo")
class RuleGroupCategoryInheritanceTest extends DemoTestBase {

    @Test
    @DisplayName("Demo: Trading rule group inherits from trading category")
    void testTradingRuleGroupInheritance() throws Exception {
        String yaml = """
            metadata:
              name: "Trading Rule Group Inheritance Demo"
              type: "rule-config"
              version: "1.0.0"

            categories:
              - name: "trading-operations"
                description: "Trading operations and validation rules"
                priority: 10
                enabled: true
                business-domain: "Trading Operations"
                business-owner: "trading-desk@bank.com"
                created-by: "trading-systems@bank.com"
                effective-date: "2025-01-01"
                expiration-date: "2025-12-31"
                stop-on-first-failure: true
                parallel-execution: false

            rules:
              - id: "position-limit-check"
                name: "Position Limit Validation"
                condition: "amount <= 1000000"
                message: "Position exceeds limit"
                category: "trading-operations"

              - id: "margin-requirement-check"
                name: "Margin Requirement Validation"
                condition: "margin >= (amount * 0.1)"
                message: "Insufficient margin"
                category: "trading-operations"

              - id: "risk-exposure-check"
                name: "Risk Exposure Validation"
                condition: "exposure <= 5000000"
                message: "Risk exposure too high"
                category: "trading-operations"

            rule-groups:
              - id: "pre-trade-validation"
                name: "Pre-Trade Validation Group"
                description: "Validates trades before execution"
                category: "trading-operations"
                operator: "AND"
                rule-ids:
                  - "position-limit-check"
                  - "margin-requirement-check"
                  - "risk-exposure-check"
            """;

        YamlRuleConfiguration config = yamlLoader.fromYamlString(yaml);
        assertNotNull(config, "Configuration should be loaded");

        // Test with valid trading data
        Map<String, Object> validTrade = new HashMap<>();
        validTrade.put("amount", 500000);
        validTrade.put("margin", 75000);
        validTrade.put("exposure", 2000000);

        RuleResult result = testEvaluation(config, validTrade);

        logger.info("=== Trading Rule Group Inheritance Demo ===");
        logger.info("Category: trading-operations");
        logger.info("Business Domain: Trading Operations (inherited)");
        logger.info("Business Owner: trading-desk@bank.com (inherited)");
        logger.info("Created By: trading-systems@bank.com (inherited)");
        logger.info("Effective Date: 2025-01-01 (inherited)");
        logger.info("Expiration Date: 2025-12-31 (inherited)");
        logger.info("Rule Group: pre-trade-validation");
        logger.info("Validation Result: " + (result.isSuccess() ? "PASSED" : "FAILED"));
        logger.info("Message: " + result.getMessage());
        
        assertTrue(result.isSuccess(), "Valid trade should pass all validations");
        assertEquals("SUCCESS", result.getSeverity(), "Should have SUCCESS severity");
    }

    @Test
    @DisplayName("Demo: Compliance rule group with partial override")
    void testComplianceRuleGroupPartialOverride() throws Exception {
        String yaml = """
            metadata:
              name: "Compliance Rule Group Override Demo"
              type: "rule-config"
              version: "1.0.0"

            categories:
              - name: "regulatory-compliance"
                description: "Regulatory compliance rules"
                priority: 5
                enabled: true
                business-domain: "Regulatory Compliance"
                business-owner: "compliance@bank.com"
                created-by: "regulatory-team@bank.com"
                effective-date: "2025-01-01"
                expiration-date: "2025-12-31"

            rules:
              - id: "kyc-verification"
                name: "KYC Verification"
                condition: "kycStatus == 'VERIFIED'"
                message: "KYC verification required"
                category: "regulatory-compliance"

              - id: "aml-screening"
                name: "AML Screening"
                condition: "amlStatus == 'CLEAR'"
                message: "AML screening failed"
                category: "regulatory-compliance"

            rule-groups:
              - id: "customer-onboarding"
                name: "Customer Onboarding Compliance"
                description: "Compliance checks for new customers"
                category: "regulatory-compliance"
                operator: "AND"
                business-owner: "onboarding-team@bank.com"  # Override category owner
                created-by: "onboarding-systems@bank.com"   # Override creator
                rule-ids:
                  - "kyc-verification"
                  - "aml-screening"
            """;

        YamlRuleConfiguration config = yamlLoader.fromYamlString(yaml);
        assertNotNull(config, "Configuration should be loaded");

        // Test with compliant customer data
        Map<String, Object> customerData = new HashMap<>();
        customerData.put("kycStatus", "VERIFIED");
        customerData.put("amlStatus", "CLEAR");

        RuleResult result = testEvaluation(config, customerData);

        logger.info("=== Compliance Rule Group Override Demo ===");
        logger.info("Category: regulatory-compliance");
        logger.info("Business Domain: Regulatory Compliance (inherited)");
        logger.info("Business Owner: onboarding-team@bank.com (OVERRIDDEN)");
        logger.info("Created By: onboarding-systems@bank.com (OVERRIDDEN)");
        logger.info("Effective Date: 2025-01-01 (inherited)");
        logger.info("Expiration Date: 2025-12-31 (inherited)");
        logger.info("Rule Group: customer-onboarding");
        logger.info("Validation Result: " + (result.isSuccess() ? "PASSED" : "FAILED"));
        logger.info("Message: " + result.getMessage());
        
        assertTrue(result.isSuccess(), "Compliant customer should pass all checks");
        assertEquals("SUCCESS", result.getSeverity(), "Should have SUCCESS severity");
    }

    @Test
    @DisplayName("Demo: Rule group without category (no inheritance)")
    void testRuleGroupWithoutCategory() throws Exception {
        String yaml = """
            metadata:
              name: "Standalone Rule Group Demo"
              type: "rule-config"
              version: "1.0.0"

            rules:
              - id: "basic-validation"
                name: "Basic Data Validation"
                condition: "value != null"
                message: "Value cannot be null"
                business-owner: "data-team@bank.com"

            rule-groups:
              - id: "standalone-validation"
                name: "Standalone Validation Group"
                description: "Basic validation without category"
                operator: "AND"
                business-owner: "validation-team@bank.com"
                created-by: "validation-systems@bank.com"
                rule-ids:
                  - "basic-validation"
            """;

        YamlRuleConfiguration config = yamlLoader.fromYamlString(yaml);
        assertNotNull(config, "Configuration should be loaded");

        // Test with valid data
        Map<String, Object> data = new HashMap<>();
        data.put("value", "test-value");

        RuleResult result = testEvaluation(config, data);

        logger.info("=== Standalone Rule Group Demo ===");
        logger.info("Category: default (no explicit category)");
        logger.info("Business Domain: null (no inheritance)");
        logger.info("Business Owner: validation-team@bank.com (explicit)");
        logger.info("Created By: validation-systems@bank.com (explicit)");
        logger.info("Effective Date: null (no inheritance)");
        logger.info("Expiration Date: null (no inheritance)");
        logger.info("Rule Group: standalone-validation");
        logger.info("Validation Result: " + (result.isSuccess() ? "PASSED" : "FAILED"));
        logger.info("Message: " + result.getMessage());
        
        assertTrue(result.isSuccess(), "Valid data should pass validation");
        assertEquals("SUCCESS", result.getSeverity(), "Should have SUCCESS severity");
    }
}
