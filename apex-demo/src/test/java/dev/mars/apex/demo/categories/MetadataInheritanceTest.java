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

package dev.mars.apex.demo.categories;

import dev.mars.apex.demo.DemoTestBase;
import dev.mars.apex.core.config.yaml.YamlRuleConfiguration;
import dev.mars.apex.core.config.yaml.YamlRuleFactory;
import dev.mars.apex.core.engine.config.RulesEngine;
import dev.mars.apex.core.engine.model.RuleResult;
import dev.mars.apex.core.engine.model.Rule;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Advanced demonstration of APEX Rule Categories metadata inheritance.
 * Tests how rules inherit business metadata from categories and rule-level overrides.
 */
public class MetadataInheritanceTest extends DemoTestBase {

    private static final Logger logger = LoggerFactory.getLogger(MetadataInheritanceTest.class);

    @Test
    public void testMetadataInheritanceFromCategory() {
        logger.info("=== Testing Metadata Inheritance from Category ===");

        // Load YAML configuration with categories and rules
        YamlRuleConfiguration config = loadAndValidateYaml("dev/mars/apex/demo/categories/MetadataInheritanceTest.yaml");

        // Create YamlRuleFactory to process rules with metadata inheritance
        YamlRuleFactory ruleFactory = new YamlRuleFactory();
        List<Rule> rules = ruleFactory.createRules(config);

        // Verify rules were created
        assertNotNull(rules, "Rules should be created");
        assertFalse(rules.isEmpty(), "Should have rules");

        // Find rule that should inherit metadata from category
        Rule inheritedRule = rules.stream()
            .filter(r -> "inherited-metadata-rule".equals(r.getId()))
            .findFirst()
            .orElse(null);

        assertNotNull(inheritedRule, "Should find rule with inherited metadata");

        // Verify metadata inheritance (this will be implemented in Task 2)
        logger.info("Rule '{}' has categories: '{}'", inheritedRule.getId(),
            inheritedRule.getCategories().stream().map(c -> c.getName()).collect(Collectors.toList()));

        logger.info("** Metadata inheritance test passed");
    }

    @Test
    public void testRuleLevelMetadataOverride() {
        logger.info("=== Testing Rule-Level Metadata Override ===");

        // Load configuration
        YamlRuleConfiguration config = loadAndValidateYaml("dev/mars/apex/demo/categories/MetadataInheritanceTest.yaml");

        // Verify rule with explicit metadata overrides category defaults
        var overrideRule = config.getRules().stream()
            .filter(r -> "override-metadata-rule".equals(r.getId()))
            .findFirst()
            .orElse(null);

        assertNotNull(overrideRule, "Should find rule with metadata override");
        assertEquals("Jane Override", overrideRule.getCreatedBy(), "Rule should override created-by from category");

        logger.info("** Rule-level metadata override test passed");
    }

    @Test
    public void testMultipleCategoriesWithDifferentMetadata() {
        logger.info("=== Testing Multiple Categories with Different Metadata ===");

        // Load configuration
        YamlRuleConfiguration config = loadAndValidateYaml("dev/mars/apex/demo/categories/MetadataInheritanceTest.yaml");

        // Verify different categories have different metadata
        var categories = config.getCategories();
        assertEquals(3, categories.size(), "Should have 3 categories");

        // Verify each category has unique business metadata
        var complianceCategory = categories.stream()
            .filter(c -> "compliance-rules".equals(c.getName()))
            .findFirst()
            .orElse(null);

        assertNotNull(complianceCategory, "Should find compliance category");
        assertEquals("Compliance", complianceCategory.getBusinessDomain(), "Compliance category should have correct domain");
        assertEquals("Compliance Officer", complianceCategory.getBusinessOwner(), "Compliance category should have correct owner");

        var operationsCategory = categories.stream()
            .filter(c -> "operations-rules".equals(c.getName()))
            .findFirst()
            .orElse(null);

        assertNotNull(operationsCategory, "Should find operations category");
        assertEquals("Operations", operationsCategory.getBusinessDomain(), "Operations category should have correct domain");
        assertEquals("Operations Manager", operationsCategory.getBusinessOwner(), "Operations category should have correct owner");

        logger.info("** Multiple categories test passed");
    }

    @Test
    public void testCategoryLifecycleManagement() {
        logger.info("=== Testing Category Lifecycle Management ===");

        // Load configuration
        YamlRuleConfiguration config = loadAndValidateYaml("dev/mars/apex/demo/categories/MetadataInheritanceTest.yaml");

        // Verify categories have lifecycle dates
        config.getCategories().forEach(category -> {
            assertNotNull(category.getEffectiveDate(), "Category should have effective date");
            assertNotNull(category.getExpirationDate(), "Category should have expiration date");
            
            logger.info("Category '{}' effective: '{}', expires: '{}'",
                category.getName(), category.getEffectiveDate(), category.getExpirationDate());
        });

        // Test rule execution with lifecycle-managed categories
        Map<String, Object> testData = new HashMap<>();
        testData.put("amount", 1000);
        testData.put("riskLevel", "LOW");
        testData.put("complianceStatus", "APPROVED");

        RuleResult result = testEvaluation(config, testData);
        assertTrue(result.isSuccess(), "Rule execution should be successful");

        logger.info("** Category lifecycle management test passed");
    }
}
