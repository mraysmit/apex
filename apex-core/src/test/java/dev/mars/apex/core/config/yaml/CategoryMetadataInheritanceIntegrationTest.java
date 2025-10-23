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

package dev.mars.apex.core.config.yaml;

import dev.mars.apex.core.engine.model.Rule;
import dev.mars.apex.core.engine.model.Category;
import dev.mars.apex.core.engine.model.metadata.RuleMetadata;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive integration test for Rule Categories metadata inheritance.
 * Tests the complete workflow from YAML parsing to rule creation with inherited metadata.
 */
public class CategoryMetadataInheritanceIntegrationTest {

    private static final Logger logger = LoggerFactory.getLogger(CategoryMetadataInheritanceIntegrationTest.class);

    private YamlConfigurationLoader yamlLoader;
    private YamlRuleFactory ruleFactory;

    @BeforeEach
    public void setUp() {
        logger.info("Setting up Category Metadata Inheritance Integration Test...");
        yamlLoader = new YamlConfigurationLoader();
        ruleFactory = new YamlRuleFactory();
    }

    @Test
    public void testCompleteMetadataInheritanceWorkflow() throws Exception {
        logger.info("=== Testing Complete Metadata Inheritance Workflow ===");

        // Load YAML configuration with categories and rules
        String yamlPath = "dev/mars/apex/core/config/yaml/CategoryMetadataInheritanceIntegrationTest.yaml";
        YamlRuleConfiguration config = yamlLoader.loadFromClasspath(yamlPath);

        // Verify categories are loaded
        assertNotNull(config.getCategories(), "Categories should be loaded");
        assertEquals(3, config.getCategories().size(), "Should have 3 categories");

        // Debug: Print category information
        logger.info("Loaded categories:");
        config.getCategories().forEach(cat -> {
            logger.info("  Category: name='{}', domain='{}', owner='{}', creator='{}'",
                cat.getName(), cat.getBusinessDomain(), cat.getBusinessOwner(), cat.getCreatedBy());
        });

        // Verify rules are loaded
        assertNotNull(config.getRules(), "Rules should be loaded");
        assertEquals(4, config.getRules().size(), "Should have 4 rules");

        // Debug: Print rule information
        logger.info("Loaded rules:");
        config.getRules().forEach(rule -> {
            logger.info("  Rule: id='{}', category='{}', domain='{}', owner='{}', creator='{}'",
                rule.getId(), rule.getCategory(), rule.getBusinessDomain(), rule.getBusinessOwner(), rule.getCreatedBy());
        });

        // Create rules using YamlRuleFactory
        List<Rule> rules = ruleFactory.createRules(config);

        // Verify rules were created
        assertNotNull(rules, "Rules should be created");
        assertEquals(4, rules.size(), "Should have 4 rules created");

        // Debug: Print created rule metadata
        logger.info("Created rules with metadata:");
        rules.forEach(rule -> {
            logger.info("  Rule: id='{}', categories='{}', domain='{}', owner='{}', creator='{}'",
                rule.getId(),
                rule.getCategories().stream().map(c -> c.getName()).collect(java.util.stream.Collectors.toList()),
                rule.getMetadata().getBusinessDomain().orElse("null"),
                rule.getMetadata().getBusinessOwner().orElse("null"),
                rule.getMetadata().getCreatedByUser());
        });

        // Test metadata inheritance for each rule
        testInheritedMetadataRule(rules);
        testOverrideMetadataRule(rules);
        testCompleteOverrideRule(rules);
        testMultipleCategoriesRule(rules);

        logger.info("** Complete metadata inheritance workflow test passed");
    }

    private void testInheritedMetadataRule(List<Rule> rules) {
        logger.info("--- Testing Rule with Inherited Metadata ---");

        Rule inheritedRule = findRuleById(rules, "inherited-metadata-rule");
        assertNotNull(inheritedRule, "Should find inherited metadata rule");

        // Verify category assignment
        assertTrue(inheritedRule.hasCategory("compliance-rules"), "Rule should be in compliance-rules category");

        // Verify metadata inheritance
        RuleMetadata metadata = inheritedRule.getMetadata();
        assertNotNull(metadata, "Rule should have metadata");

        assertEquals("Compliance", metadata.getBusinessDomain().orElse(null), "Should inherit business domain from category");
        assertEquals("Compliance Officer", metadata.getBusinessOwner().orElse(null), "Should inherit business owner from category");
        assertEquals("John Compliance", metadata.getCreatedByUser(), "Should inherit created-by from category");

        // Verify lifecycle dates inheritance
        assertNotNull(metadata.getEffectiveDate(), "Should inherit effective date from category");
        assertNotNull(metadata.getExpirationDate(), "Should inherit expiration date from category");

        logger.info("Inherited metadata rule verified: domain='{}', owner='{}', creator='{}'",
            metadata.getBusinessDomain().orElse(null), metadata.getBusinessOwner().orElse(null), metadata.getCreatedByUser());
    }

    private void testOverrideMetadataRule(List<Rule> rules) {
        logger.info("--- Testing Rule with Metadata Override ---");

        Rule overrideRule = findRuleById(rules, "override-metadata-rule");
        assertNotNull(overrideRule, "Should find override metadata rule");

        // Verify category assignment
        assertTrue(overrideRule.hasCategory("operations-rules"), "Rule should be in operations-rules category");

        // Verify metadata inheritance and override
        RuleMetadata metadata = overrideRule.getMetadata();
        assertNotNull(metadata, "Rule should have metadata");

        assertEquals("Operations", metadata.getBusinessDomain().orElse(null), "Should inherit business domain from category");
        assertEquals("Operations Manager", metadata.getBusinessOwner().orElse(null), "Should inherit business owner from category");
        assertEquals("Jane Override", metadata.getCreatedByUser(), "Should override created-by from rule level");

        logger.info("Override metadata rule verified: domain='{}', owner='{}', creator='{}'",
            metadata.getBusinessDomain().orElse(null), metadata.getBusinessOwner().orElse(null), metadata.getCreatedByUser());
    }

    private void testCompleteOverrideRule(List<Rule> rules) {
        logger.info("--- Testing Rule with Complete Metadata Override ---");

        Rule completeOverrideRule = findRuleById(rules, "complete-override-rule");
        assertNotNull(completeOverrideRule, "Should find complete override rule");

        // Verify category assignment
        assertTrue(completeOverrideRule.hasCategory("risk-assessment"), "Rule should be in risk-assessment category");

        // Verify complete metadata override
        RuleMetadata metadata = completeOverrideRule.getMetadata();
        assertNotNull(metadata, "Rule should have metadata");

        assertEquals("Custom Domain", metadata.getBusinessDomain().orElse(null), "Should override business domain at rule level");
        assertEquals("Custom Owner", metadata.getBusinessOwner().orElse(null), "Should override business owner at rule level");
        assertEquals("Custom Creator", metadata.getCreatedByUser(), "Should override created-by at rule level");

        logger.info("Complete override rule verified: domain='{}', owner='{}', creator='{}'",
            metadata.getBusinessDomain().orElse(null), metadata.getBusinessOwner().orElse(null), metadata.getCreatedByUser());
    }

    private void testMultipleCategoriesRule(List<Rule> rules) {
        logger.info("--- Testing Rule with Multiple Categories ---");

        Rule multiCategoryRule = findRuleById(rules, "multi-category-rule");
        assertNotNull(multiCategoryRule, "Should find multi-category rule");

        // Verify category assignment
        assertTrue(multiCategoryRule.hasCategory("compliance-rules"), "Rule should be in compliance-rules category");

        // Verify metadata inheritance from primary category
        RuleMetadata metadata = multiCategoryRule.getMetadata();
        assertNotNull(metadata, "Rule should have metadata");

        assertEquals("Compliance", metadata.getBusinessDomain().orElse(null), "Should inherit from primary category");
        assertEquals("Compliance Officer", metadata.getBusinessOwner().orElse(null), "Should inherit from primary category");

        logger.info("Multi-category rule verified: domain='{}', owner='{}'",
            metadata.getBusinessDomain().orElse(null), metadata.getBusinessOwner().orElse(null));
    }

    private Rule findRuleById(List<Rule> rules, String ruleId) {
        return rules.stream()
            .filter(rule -> ruleId.equals(rule.getId()))
            .findFirst()
            .orElse(null);
    }

    @Test
    public void testCategoryPriorityOrdering() throws Exception {
        logger.info("=== Testing Category Priority Ordering ===");

        // Load configuration
        String yamlPath = "dev/mars/apex/core/config/yaml/CategoryMetadataInheritanceIntegrationTest.yaml";
        YamlRuleConfiguration config = yamlLoader.loadFromClasspath(yamlPath);

        // Create rules
        List<Rule> rules = ruleFactory.createRules(config);

        // Verify category priorities are reflected in rules
        Rule highPriorityRule = findRuleById(rules, "inherited-metadata-rule");
        Rule mediumPriorityRule = findRuleById(rules, "override-metadata-rule");

        assertNotNull(highPriorityRule, "Should find high priority rule");
        assertNotNull(mediumPriorityRule, "Should find medium priority rule");

        // Verify priority values
        assertEquals(5, highPriorityRule.getPriority(), "High priority rule should have priority 5");
        assertEquals(20, mediumPriorityRule.getPriority(), "Medium priority rule should have priority 20");

        logger.info("** Category priority ordering test passed");
    }

    @Test
    public void testCategoryLifecycleManagement() throws Exception {
        logger.info("=== Testing Category Lifecycle Management ===");

        // Load configuration
        String yamlPath = "dev/mars/apex/core/config/yaml/CategoryMetadataInheritanceIntegrationTest.yaml";
        YamlRuleConfiguration config = yamlLoader.loadFromClasspath(yamlPath);

        // Create rules
        List<Rule> rules = ruleFactory.createRules(config);

        // Verify lifecycle dates are inherited
        Rule ruleWithLifecycle = findRuleById(rules, "inherited-metadata-rule");
        assertNotNull(ruleWithLifecycle, "Should find rule with lifecycle");

        RuleMetadata metadata = ruleWithLifecycle.getMetadata();
        assertNotNull(metadata.getEffectiveDate(), "Rule should have effective date from category");
        assertNotNull(metadata.getExpirationDate(), "Rule should have expiration date from category");

        logger.info("Rule lifecycle dates: effective='{}', expiration='{}'",
            metadata.getEffectiveDate(), metadata.getExpirationDate());

        logger.info("** Category lifecycle management test passed");
    }
}
