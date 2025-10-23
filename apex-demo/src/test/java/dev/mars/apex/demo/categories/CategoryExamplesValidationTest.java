/*
 * Copyright (c) 2025 APEX Development Team
 * Licensed under the Apache License, Version 2.0
 */

package dev.mars.apex.demo.categories;

import dev.mars.apex.core.config.yaml.YamlRuleConfiguration;
import dev.mars.apex.core.config.yaml.YamlCategory;
import dev.mars.apex.demo.DemoTestBase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Validation test for all category examples in apex-demo.
 * 
 * This test demonstrates:
 * 1. Category definitions in separate YAML files
 * 2. Rules and enrichments referencing external categories
 * 3. Metadata inheritance patterns
 * 4. Enterprise governance scenarios
 */
@DisplayName("Category Examples Validation")
public class CategoryExamplesValidationTest extends DemoTestBase {

    private static final Logger logger = LoggerFactory.getLogger(CategoryExamplesValidationTest.class);

    @Test
    @DisplayName("Should validate basic category examples")
    public void testBasicCategoryExamples() {
        logger.info("=== Validating Basic Category Examples ===");

        // Test metadata inheritance example
        YamlRuleConfiguration inheritanceConfig = loadAndValidateYaml(
            "dev/mars/apex/demo/categories/MetadataInheritanceTest.yaml");

        assertNotNull(inheritanceConfig.getCategories(), "Inheritance categories should be defined");
        assertEquals(3, inheritanceConfig.getCategories().size(), "Should have 3 inheritance categories");

        assertNotNull(inheritanceConfig.getRules(), "Inheritance rules should be defined");
        assertEquals(5, inheritanceConfig.getRules().size(), "Should have 5 inheritance rules");

        logger.info("** Basic category examples validation passed");
    }

    @Test
    @DisplayName("Should validate separate file category definitions")
    public void testSeparateFileCategoryDefinitions() {
        logger.info("=== Validating Separate File Category Definitions ===");

        // Load enterprise categories defined in separate file
        YamlRuleConfiguration categoryConfig = loadAndValidateYaml(
            "dev/mars/apex/demo/categories/separate-files/enterprise-categories.yaml");
        
        assertNotNull(categoryConfig.getCategories(), "Enterprise categories should be defined");
        assertEquals(11, categoryConfig.getCategories().size(), "Should have 11 enterprise categories");

        // Validate category metadata
        YamlCategory riskCategory = categoryConfig.getCategories().stream()
            .filter(cat -> "credit-risk".equals(cat.getName()))
            .findFirst()
            .orElse(null);

        assertNotNull(riskCategory, "Credit risk category should exist");
        assertEquals("Risk Management", riskCategory.getBusinessDomain());
        assertEquals("Chief Risk Officer", riskCategory.getBusinessOwner());

        // Load rules that reference external categories
        YamlRuleConfiguration rulesConfig = loadAndValidateYaml(
            "dev/mars/apex/demo/categories/separate-files/rules-with-external-categories.yaml");
        
        assertNotNull(rulesConfig.getRules(), "Rules should be defined");
        assertEquals(15, rulesConfig.getRules().size(), "Should have 15 rules");

        // Load enrichments that reference external categories
        YamlRuleConfiguration enrichmentsConfig = loadAndValidateYaml(
            "dev/mars/apex/demo/categories/separate-files/enrichments-with-external-categories.yaml");
        
        assertNotNull(enrichmentsConfig.getEnrichments(), "Enrichments should be defined");
        assertEquals(16, enrichmentsConfig.getEnrichments().size(), "Should have 16 enrichments");

        logger.info("** Separate file category definitions validation passed");
    }

    // @Test
    // @DisplayName("Should validate inheritance pattern examples")
    // public void testInheritancePatternExamples() {
    //     logger.info("=== Validating Inheritance Pattern Examples ===");

    //     // Load inheritance patterns configuration
    //     YamlRuleConfiguration config = loadAndValidateYaml(
    //         "dev/mars/apex/demo/categories/inheritance-patterns/metadata-inheritance-examples.yaml");

    //     assertNotNull(config.getCategories(), "Categories should be defined");
    //     assertEquals(3, config.getCategories().size(), "Should have 3 categories");

    //     assertNotNull(config.getRules(), "Rules should be defined");
    //     assertEquals(6, config.getRules().size(), "Should have 6 rules");

    //     assertNotNull(config.getEnrichments(), "Enrichments should be defined");
    //     assertEquals(4, config.getEnrichments().size(), "Should have 4 enrichments");

    //     // Validate category metadata completeness
    //     YamlCategory complianceCategory = config.getCategories().stream()
    //         .filter(cat -> "compliance-category".equals(cat.getName()))
    //         .findFirst()
    //         .orElse(null);

    //     assertNotNull(complianceCategory, "Compliance category should exist");
    //     assertEquals("Regulatory", complianceCategory.getBusinessDomain());
    //     assertEquals("Compliance Team", complianceCategory.getBusinessOwner());
    //     assertEquals("compliance-admin", complianceCategory.getCreatedBy());

    //     logger.info("** Inheritance pattern examples validation passed");
    // }

    @Test
    @DisplayName("Should validate enterprise scenario examples")
    public void testEnterpriseScenarioExamples() {
        logger.info("=== Validating Enterprise Scenario Examples ===");

        // Load financial services categories
        YamlRuleConfiguration config = loadAndValidateYaml(
            "dev/mars/apex/demo/categories/enterprise-scenarios/financial-services-categories.yaml");
        
        assertNotNull(config.getCategories(), "Categories should be defined");
        assertEquals(16, config.getCategories().size(), "Should have 16 financial services categories");

        // Validate comprehensive category coverage
        String[] expectedCategories = {
            "basel-iii-capital", "dodd-frank-compliance", "mifid-ii-compliance",
            "var-calculations", "stress-testing", "credit-risk-assessment",
            "pre-trade-controls", "post-trade-processing", "market-data-validation",
            "client-onboarding", "suitability-assessment", "regulatory-reporting",
            "management-reporting", "data-quality-management", "cybersecurity-controls", "business-continuity"
        };

        for (String expectedCategory : expectedCategories) {
            boolean found = config.getCategories().stream()
                .anyMatch(cat -> expectedCategory.equals(cat.getName()));
            assertTrue(found, "Should have category: " + expectedCategory);
        }

        // Validate business domain coverage
        long riskDomainCount = config.getCategories().stream()
            .filter(cat -> cat.getBusinessDomain() != null && cat.getBusinessDomain().contains("Risk"))
            .count();
        assertTrue(riskDomainCount >= 3, "Should have multiple risk management categories");

        long complianceDomainCount = config.getCategories().stream()
            .filter(cat -> cat.getBusinessDomain() != null && cat.getBusinessDomain().contains("Compliance"))
            .count();
        assertTrue(complianceDomainCount >= 1, "Should have multiple compliance categories");

        logger.info("** Enterprise scenario examples validation passed");
    }

    @Test
    @DisplayName("Should demonstrate comprehensive category ecosystem")
    public void testComprehensiveCategoryEcosystem() {
        logger.info("=== Demonstrating Comprehensive Category Ecosystem ===");

        // Count all components across all examples
        String[] configFiles = {
            "dev/mars/apex/demo/categories/MetadataInheritanceTest.yaml",
            "dev/mars/apex/demo/categories/separate-files/enterprise-categories.yaml",
            "dev/mars/apex/demo/categories/separate-files/rules-with-external-categories.yaml",
            "dev/mars/apex/demo/categories/separate-files/enrichments-with-external-categories.yaml",
            "dev/mars/apex/demo/categories/enterprise-scenarios/financial-services-categories.yaml"
        };

        int totalCategories = 0;
        int totalRules = 0;
        int totalEnrichments = 0;
        int totalRuleGroups = 0;
        int totalEnrichmentGroups = 0;

        for (String configFile : configFiles) {
            YamlRuleConfiguration config = loadAndValidateYaml(configFile);
            
            if (config.getCategories() != null) {
                totalCategories += config.getCategories().size();
            }
            if (config.getRules() != null) {
                totalRules += config.getRules().size();
            }
            if (config.getEnrichments() != null) {
                totalEnrichments += config.getEnrichments().size();
            }
            if (config.getRuleGroups() != null) {
                totalRuleGroups += config.getRuleGroups().size();
            }
            if (config.getEnrichmentGroups() != null) {
                totalEnrichmentGroups += config.getEnrichmentGroups().size();
            }
        }

        // Verify comprehensive coverage
        assertTrue(totalCategories >= 30, "Should have comprehensive category coverage");
        assertTrue(totalRules >= 15, "Should have substantial rule examples");
        assertTrue(totalEnrichments >= 8, "Should have substantial enrichment examples");

        logger.info("Category Ecosystem Summary:");
        logger.info("  Total Categories: {}", totalCategories);
        logger.info("  Total Rules: {}", totalRules);
        logger.info("  Total Enrichments: {}", totalEnrichments);
        logger.info("  Total Rule Groups: {}", totalRuleGroups);
        logger.info("  Total Enrichment Groups: {}", totalEnrichmentGroups);

        logger.info("** Comprehensive category ecosystem demonstration completed");
    }
}
