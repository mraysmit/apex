package dev.mars.apex.demo.rulegroups;

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

import dev.mars.apex.core.config.yaml.YamlConfigurationLoader;
import dev.mars.apex.core.config.yaml.YamlRulesEngineService;
import dev.mars.apex.core.engine.config.RulesEngine;
import dev.mars.apex.core.engine.model.RuleGroup;
import dev.mars.apex.core.engine.model.RuleResult;
import dev.mars.apex.demo.ColoredTestOutputExtension;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.*;
import static dev.mars.apex.demo.ColoredTestOutputExtension.*;

/**
 * Hierarchical Rule Group References Tests.
 *
 * Demonstrates APEX's hierarchical rule group composition capability where rule groups
 * can reference other rule groups, creating a 3-level hierarchy:
 * 1. Individual Rules (base-rules.yaml)
 * 2. Base Rule Groups (base-groups.yaml) - reference individual rules
 * 3. Composite Rule Groups (composite-groups.yaml) - reference base rule groups
 */
@ExtendWith(ColoredTestOutputExtension.class)
@DisplayName("Hierarchical Rule Group References Tests")
public class HierarchicalRuleGroupTest {

    private static final Logger logger = LoggerFactory.getLogger(HierarchicalRuleGroupTest.class);

    private YamlConfigurationLoader yamlLoader;
    private YamlRulesEngineService rulesEngineService;

    @BeforeEach
    void setUp() {
        this.yamlLoader = new YamlConfigurationLoader();
        this.rulesEngineService = new YamlRulesEngineService();
    }

    @Test
    @DisplayName("Complete Customer Onboarding - 3-Level Hierarchy")
    void testCompleteCustomerOnboarding() throws Exception {
        logInfo("Testing complete customer onboarding with 3-level rule group hierarchy");
        
        // Load the hierarchical configuration files
        RulesEngine engine = rulesEngineService.createRulesEngineFromMultipleFiles(
            "src/test/resources/rulegroups/hierarchical/base-rules.yaml",
            "src/test/resources/rulegroups/hierarchical/base-groups.yaml",
            "src/test/resources/rulegroups/hierarchical/composite-groups.yaml"
        );
        
        // Verify the composite rule group exists
        RuleGroup completeOnboarding = engine.getConfiguration().getRuleGroupById("complete-onboarding");
        assertNotNull(completeOnboarding, "Complete onboarding rule group should be found");
        
        // Test with valid customer data
        Map<String, Object> validCustomer = createValidCustomerData();
        RuleResult result = engine.executeRuleGroupsList(List.of(completeOnboarding), validCustomer);
        
        assertTrue(result.isTriggered(), "Valid customer should pass complete onboarding");
        logSuccess("Valid customer passed complete onboarding validation");
        
        // Test with invalid customer data (age too young)
        Map<String, Object> invalidCustomer = createInvalidCustomerData();
        RuleResult invalidResult = engine.executeRuleGroupsList(List.of(completeOnboarding), invalidCustomer);
        
        assertFalse(invalidResult.isTriggered(), "Invalid customer should fail complete onboarding");
        logInfo("Invalid customer correctly failed complete onboarding validation");
    }

    @Test
    @DisplayName("Premium Customer Onboarding - Hierarchical with OR Logic")
    void testPremiumCustomerOnboarding() throws Exception {
        logInfo("Testing premium customer onboarding with hierarchical rule groups");
        
        RulesEngine engine = rulesEngineService.createRulesEngineFromMultipleFiles(
            "src/test/resources/rulegroups/hierarchical/base-rules.yaml",
            "src/test/resources/rulegroups/hierarchical/base-groups.yaml",
            "src/test/resources/rulegroups/hierarchical/composite-groups.yaml"
        );
        
        RuleGroup premiumOnboarding = engine.getConfiguration().getRuleGroupById("premium-onboarding");
        assertNotNull(premiumOnboarding, "Premium onboarding rule group should be found");
        
        // Test with customer who has good credit but lower income
        Map<String, Object> premiumCustomer = createPremiumCustomerData();
        RuleResult result = engine.executeRuleGroupsList(List.of(premiumOnboarding), premiumCustomer);
        
        assertTrue(result.isTriggered(), "Premium customer should pass with relaxed financial validation");
        logSuccess("Premium customer passed with relaxed financial validation");
    }

    @Test
    @DisplayName("Mixed Validation - Rule Groups + Individual Rules")
    void testMixedValidation() throws Exception {
        logInfo("Testing mixed validation combining rule group references with individual rules");
        
        RulesEngine engine = rulesEngineService.createRulesEngineFromMultipleFiles(
            "src/test/resources/rulegroups/hierarchical/base-rules.yaml",
            "src/test/resources/rulegroups/hierarchical/base-groups.yaml",
            "src/test/resources/rulegroups/hierarchical/composite-groups.yaml"
        );
        
        RuleGroup mixedValidation = engine.getConfiguration().getRuleGroupById("mixed-validation");
        assertNotNull(mixedValidation, "Mixed validation rule group should be found");
        
        // Test with customer who passes basic and compliance but has high debt ratio
        Map<String, Object> customerData = createHighDebtCustomerData();
        RuleResult result = engine.executeRuleGroupsList(List.of(mixedValidation), customerData);



        // Should fail due to debt-to-income ratio rule
        assertFalse(result.isTriggered(), "Customer with high debt ratio should fail mixed validation");
        logInfo("Customer with high debt ratio correctly failed mixed validation");
    }

    @Test
    @DisplayName("Tiered Validation - Multiple Hierarchical Paths")
    void testTieredValidation() throws Exception {
        logInfo("Testing tiered validation with multiple hierarchical validation paths");
        
        RulesEngine engine = rulesEngineService.createRulesEngineFromMultipleFiles(
            "src/test/resources/rulegroups/hierarchical/base-rules.yaml",
            "src/test/resources/rulegroups/hierarchical/base-groups.yaml",
            "src/test/resources/rulegroups/hierarchical/composite-groups.yaml"
        );
        
        RuleGroup tieredValidation = engine.getConfiguration().getRuleGroupById("tiered-validation");
        assertNotNull(tieredValidation, "Tiered validation rule group should be found");
        
        // Test with existing customer data (should pass via customer-update-validation path)
        Map<String, Object> existingCustomer = createExistingCustomerData();
        RuleResult result = engine.executeRuleGroupsList(List.of(tieredValidation), existingCustomer);
        
        assertTrue(result.isTriggered(), "Existing customer should pass tiered validation");
        logSuccess("Existing customer passed tiered validation via appropriate path");
    }

    // Helper methods to create test data
    private Map<String, Object> createValidCustomerData() {
        Map<String, Object> customer = new HashMap<>();
        customer.put("name", "John Doe");
        customer.put("age", 30);
        customer.put("email", "john.doe@example.com");
        customer.put("creditScore", 750);
        customer.put("annualIncome", 75000);
        customer.put("monthlyDebt", 1500);
        customer.put("kycDocuments", List.of("passport", "utility_bill"));
        customer.put("sanctionsStatus", "CLEAR");
        customer.put("pepStatus", "NOT_PEP");
        
        Map<String, Object> data = new HashMap<>();
        data.put("customer", customer);
        return data;
    }

    private Map<String, Object> createInvalidCustomerData() {
        Map<String, Object> customer = new HashMap<>();
        customer.put("name", "Jane Smith");
        customer.put("age", 16);  // Too young
        customer.put("email", "jane.smith@example.com");
        customer.put("creditScore", 650);
        customer.put("annualIncome", 50000);
        customer.put("monthlyDebt", 1000);
        customer.put("kycDocuments", List.of("passport", "utility_bill"));
        customer.put("sanctionsStatus", "CLEAR");
        customer.put("pepStatus", "NOT_PEP");
        
        Map<String, Object> data = new HashMap<>();
        data.put("customer", customer);
        return data;
    }

    private Map<String, Object> createPremiumCustomerData() {
        Map<String, Object> customer = new HashMap<>();
        customer.put("name", "Premium Customer");
        customer.put("age", 35);
        customer.put("email", "premium@example.com");
        customer.put("creditScore", 800);  // Excellent credit
        customer.put("annualIncome", 40000);  // Lower income but good credit
        customer.put("monthlyDebt", 800);
        customer.put("kycDocuments", List.of("passport", "utility_bill", "bank_statement"));
        customer.put("sanctionsStatus", "CLEAR");
        customer.put("pepStatus", "NOT_PEP");
        
        Map<String, Object> data = new HashMap<>();
        data.put("customer", customer);
        return data;
    }

    private Map<String, Object> createHighDebtCustomerData() {
        Map<String, Object> customer = new HashMap<>();
        customer.put("name", "High Debt Customer");
        customer.put("age", 28);
        customer.put("email", "highdebt@example.com");
        customer.put("creditScore", 700);
        customer.put("annualIncome", 60000);
        customer.put("monthlyDebt", 2500);  // High debt ratio (50%)
        customer.put("kycDocuments", List.of("passport", "utility_bill"));
        customer.put("sanctionsStatus", "CLEAR");
        customer.put("pepStatus", "NOT_PEP");
        
        Map<String, Object> data = new HashMap<>();
        data.put("customer", customer);
        return data;
    }

    private Map<String, Object> createExistingCustomerData() {
        Map<String, Object> customer = new HashMap<>();
        customer.put("name", "Existing Customer");
        customer.put("age", 45);
        customer.put("email", "existing@example.com");
        customer.put("sanctionsStatus", "CLEAR");
        customer.put("pepStatus", "NOT_PEP");
        // Minimal data for existing customer validation
        
        Map<String, Object> data = new HashMap<>();
        data.put("customer", customer);
        return data;
    }
}
