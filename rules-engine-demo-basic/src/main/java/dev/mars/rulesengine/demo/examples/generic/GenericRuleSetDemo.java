package dev.mars.rulesengine.demo.examples.generic;

import dev.mars.rulesengine.core.api.RuleSet;
import dev.mars.rulesengine.core.engine.config.RulesEngine;
import dev.mars.rulesengine.core.engine.model.Rule;
import dev.mars.rulesengine.core.engine.model.RuleResult;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Comprehensive demonstration of the new generic, extensible RuleSet architecture.
 * 
 * This demo showcases how the core engine is now completely domain-agnostic
 * and supports unlimited user-defined categories without any hardcoded
 * business domain knowledge.
 * 
 * Key improvements demonstrated:
 * - Generic category system (no hardcoded domains)
 * - User-extensible categories
 * - Enterprise metadata support
 * - Robust validation and error handling
 * - Unique ID generation
 * - Comprehensive audit trails
 */
public class GenericRuleSetDemo {
    
    public static void main(String[] args) {
        System.out.println("=== GENERIC RULE SET ARCHITECTURE DEMO ===");
        System.out.println("Showcasing domain-agnostic, user-extensible rule categories\n");
        
        GenericRuleSetDemo demo = new GenericRuleSetDemo();
        
        // Step 1: Demonstrate generic category creation
        demo.demonstrateGenericCategories();
        
        // Step 2: Show enterprise metadata support
        demo.demonstrateEnterpriseMetadata();
        
        // Step 3: Demonstrate validation and error handling
        demo.demonstrateValidationFeatures();
        
        // Step 4: Show bulk rule operations
        demo.demonstrateBulkOperations();
        
        // Step 5: Execute rules and show results
        demo.demonstrateRuleExecution();
        
        System.out.println("\n=== ARCHITECTURAL BENEFITS ACHIEVED ===");
        System.out.println("✓ Domain Agnostic: Core engine has zero business domain knowledge");
        System.out.println("✓ User Extensible: Categories defined by users, not framework");
        System.out.println("✓ Enterprise Ready: Full metadata and audit trail support");
        System.out.println("✓ Robust Validation: Comprehensive parameter and rule validation");
        System.out.println("✓ Unique IDs: Collision-resistant ID generation");
        System.out.println("✓ Unlimited Flexibility: Any domain can be supported");
        
        System.out.println("\n=== DEMO COMPLETED ===");
    }
    
    /**
     * Demonstrate how users can create any category they need.
     */
    private void demonstrateGenericCategories() {
        System.out.println("=== STEP 1: GENERIC CATEGORY CREATION ===");
        System.out.println("Users can create ANY category - no hardcoded business domains!\n");
        
        // Healthcare domain
        RuleSet.GenericRuleSet patientEligibility = RuleSet.category("patient-eligibility")
            .withCreatedBy("healthcare.admin@hospital.com")
            .withBusinessDomain("Healthcare")
            .customRule("Age Check", "#age >= 18", "Patient must be adult")
            .customRule("Insurance Check", "#hasInsurance == true", "Insurance required");
        
        System.out.println("✓ Healthcare: " + patientEligibility.getCategoryName() + 
                          " (" + patientEligibility.getRuleCount() + " rules)");
        
        // Manufacturing domain
        RuleSet.GenericRuleSet qualityControl = RuleSet.category("quality-control")
            .withCreatedBy("qc.manager@manufacturing.com")
            .withBusinessDomain("Manufacturing")
            .customRule("Temperature Check", "#temperature >= 20 && #temperature <= 25", "Temperature in range")
            .customRule("Pressure Check", "#pressure <= 100", "Pressure within limits");
        
        System.out.println("✓ Manufacturing: " + qualityControl.getCategoryName() + 
                          " (" + qualityControl.getRuleCount() + " rules)");
        
        // Data Governance domain
        RuleSet.GenericRuleSet dataGovernance = RuleSet.category("data-governance")
            .withCreatedBy("governance.officer@company.com")
            .withBusinessDomain("Data Management")
            .customRule("PII Protection", "#containsPII == false || #encrypted == true", "PII must be encrypted")
            .customRule("Retention Policy", "#dataAge <= 2555", "Data within retention period");
        
        System.out.println("✓ Data Governance: " + dataGovernance.getCategoryName() + 
                          " (" + dataGovernance.getRuleCount() + " rules)");
        
        // Logistics domain
        RuleSet.GenericRuleSet shippingValidation = RuleSet.category("shipping-validation")
            .withCreatedBy("logistics.coordinator@shipping.com")
            .withBusinessDomain("Logistics")
            .customRule("Weight Limit", "#packageWeight <= 50", "Package within weight limit")
            .customRule("Destination Check", "#destination != null && #destination.length() > 0", "Valid destination");
        
        System.out.println("✓ Logistics: " + shippingValidation.getCategoryName() + 
                          " (" + shippingValidation.getRuleCount() + " rules)");
        
        System.out.println("\nKey Insight: Core engine supports ANY domain without modification!\n");
    }
    
    /**
     * Demonstrate comprehensive enterprise metadata support.
     */
    private void demonstrateEnterpriseMetadata() {
        System.out.println("=== STEP 2: ENTERPRISE METADATA SUPPORT ===");
        System.out.println("Full audit trail and governance metadata for enterprise use\n");
        
        Instant effectiveDate = Instant.now();
        Instant expirationDate = effectiveDate.plusSeconds(365 * 24 * 60 * 60); // 1 year
        
        RuleSet.GenericRuleSet enterpriseRules = RuleSet.category("risk-management")
            .withCreatedBy("risk.officer@enterprise.com")
            .withBusinessDomain("Risk Management")
            .withBusinessOwner("Chief Risk Officer")
            .withSourceSystem("ENTERPRISE_RISK_PLATFORM")
            .withEffectiveDate(effectiveDate)
            .withExpirationDate(expirationDate)
            .customRule("Credit Risk Assessment", 
                       "#creditScore >= 650 && #debtToIncome <= 0.43", 
                       "Credit risk within acceptable limits",
                       "Comprehensive credit risk assessment based on score and debt-to-income ratio")
            .customRule("Operational Risk Check", 
                       "#operationalRiskScore <= 75", 
                       "Operational risk acceptable",
                       "Validates that operational risk metrics are within enterprise thresholds");
        
        System.out.println("✓ Enterprise rule set created with comprehensive metadata:");
        System.out.println("  Category: " + enterpriseRules.getCategoryName());
        System.out.println("  Rules: " + enterpriseRules.getRuleCount());
        System.out.println("  Created by: risk.officer@enterprise.com");
        System.out.println("  Business Domain: Risk Management");
        System.out.println("  Business Owner: Chief Risk Officer");
        System.out.println("  Source System: ENTERPRISE_RISK_PLATFORM");
        System.out.println("  Effective Date: " + effectiveDate);
        System.out.println("  Expiration Date: " + expirationDate);
        
        // Show rule metadata
        List<Rule> rules = enterpriseRules.getRules();
        for (Rule rule : rules) {
            System.out.println("\n  Rule: " + rule.getName());
            System.out.println("    ID: " + rule.getId());
            System.out.println("    Created: " + rule.getCreatedDate());
            System.out.println("    Modified: " + rule.getModifiedDate());
            System.out.println("    Created By: " + rule.getMetadata().getCreatedByUser());
            System.out.println("    Business Owner: " + rule.getMetadata().getBusinessOwner().orElse("Not specified"));
        }
        
        System.out.println("\nKey Insight: Every rule has complete audit trail and governance metadata!\n");
    }
    
    /**
     * Demonstrate robust validation and error handling.
     */
    private void demonstrateValidationFeatures() {
        System.out.println("=== STEP 3: VALIDATION AND ERROR HANDLING ===");
        System.out.println("Comprehensive validation prevents common errors\n");
        
        // Valid category creation
        try {
            RuleSet.GenericRuleSet validRules = RuleSet.category("valid-category")
                .withCreatedBy("test.user@company.com")
                .customRule("Valid Rule", "#value > 0", "Value is positive");
            System.out.println("✓ Valid rule set created successfully");
        } catch (Exception e) {
            System.out.println("✗ Unexpected error: " + e.getMessage());
        }
        
        // Test category name validation
        try {
            RuleSet.category(""); // Empty category name
            System.out.println("✗ Should have failed for empty category name");
        } catch (IllegalArgumentException e) {
            System.out.println("✓ Correctly rejected empty category name: " + e.getMessage());
        }
        
        // Test invalid category name characters
        try {
            RuleSet.category("invalid category!"); // Invalid characters
            System.out.println("✗ Should have failed for invalid characters");
        } catch (IllegalArgumentException e) {
            System.out.println("✓ Correctly rejected invalid characters: " + e.getMessage());
        }
        
        // Test duplicate rule names
        try {
            RuleSet.category("test-duplicates")
                .customRule("Duplicate Rule", "#value > 0", "First rule")
                .customRule("Duplicate Rule", "#value < 100", "Second rule"); // Same name
            System.out.println("✗ Should have failed for duplicate rule names");
        } catch (IllegalStateException e) {
            System.out.println("✓ Correctly rejected duplicate rule names: " + e.getMessage());
        }
        
        // Test parameter validation
        try {
            RuleSet.category("test-params")
                .customRule(null, "#value > 0", "Message"); // Null name
            System.out.println("✗ Should have failed for null rule name");
        } catch (IllegalArgumentException e) {
            System.out.println("✓ Correctly rejected null rule name: " + e.getMessage());
        }
        
        System.out.println("\nKey Insight: Robust validation prevents common configuration errors!\n");
    }
