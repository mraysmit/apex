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
import dev.mars.apex.core.engine.model.RuleGroup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Performance and stress tests for metadata inheritance to ensure
 * the inheritance logic scales well with large configurations.
 */
@DisplayName("Metadata Inheritance Performance Tests")
class MetadataInheritancePerformanceTest {

    private static final Logger logger = LoggerFactory.getLogger(MetadataInheritancePerformanceTest.class);

    private YamlConfigurationLoader loader;
    private YamlRuleFactory factory;

    @BeforeEach
    void setUp() {
        loader = new YamlConfigurationLoader();
        factory = new YamlRuleFactory();
    }

    @Test
    @DisplayName("Large number of categories with inheritance")
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void testLargeNumberOfCategoriesWithInheritance() throws Exception {
        logger.info("Testing inheritance performance with large number of categories...");
        
        StringBuilder yaml = new StringBuilder();
        yaml.append("""
            metadata:
              name: "Large Categories Performance Test"
              type: "rule-config"
              version: "1.0.0"

            categories:
            """);

        // Create 100 categories
        int numCategories = 100;
        for (int i = 1; i <= numCategories; i++) {
            yaml.append(String.format("""
              - name: "category%d"
                description: "Category %d"
                priority: %d
                enabled: true
                business-domain: "Domain%d"
                business-owner: "Owner%d"
                created-by: "Creator%d"
                effective-date: "2024-01-01"
                expiration-date: "2024-12-31"
                
            """, i, i, i, i, i, i));
        }

        yaml.append("rules:\n");
        // Create 500 rules, each referencing a category
        int numRules = 500;
        for (int i = 1; i <= numRules; i++) {
            int categoryIndex = (i % numCategories) + 1;
            yaml.append(String.format("""
              - id: "rule%d"
                name: "Rule %d"
                category: "category%d"
                condition: "true"
                message: "Test rule %d"
                
            """, i, i, categoryIndex, i));
        }

        yaml.append("rule-groups:\n");
        // Create 100 rule groups
        int numGroups = 100;
        for (int i = 1; i <= numGroups; i++) {
            int categoryIndex = (i % numCategories) + 1;
            int ruleIndex = (i * 5) % numRules + 1; // Reference different rules
            yaml.append(String.format("""
              - id: "group%d"
                name: "Group %d"
                category: "category%d"
                rules: ["rule%d"]
                
            """, i, i, categoryIndex, ruleIndex));
        }

        long startTime = System.currentTimeMillis();
        
        YamlRuleConfiguration config = loader.fromYamlString(yaml.toString());
        List<Rule> rules = factory.createRules(config);
        List<RuleGroup> groups = factory.createRuleGroups(config, factory.createRulesEngineConfiguration(config));
        
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        
        logger.info("Created {} rules and {} groups with {} categories in {} ms", 
                   rules.size(), groups.size(), numCategories, duration);

        // Verify correct counts
        assertEquals(numRules, rules.size(), "Should create all rules");
        assertEquals(numGroups, groups.size(), "Should create all groups");

        // Verify inheritance works for a sample
        Rule sampleRule = rules.get(0);
        RuleGroup sampleGroup = groups.get(0);
        
        assertNotNull(sampleRule.getMetadata(), "Sample rule should have metadata");
        assertNotNull(sampleGroup.getBusinessDomain(), "Sample group should have inherited domain");
        
        // Performance assertion - should complete within reasonable time
        assertTrue(duration < 10000, "Large configuration should process within 10 seconds");
    }

    @Test
    @DisplayName("Deep inheritance hierarchy stress test")
    @Timeout(value = 15, unit = TimeUnit.SECONDS)
    void testDeepInheritanceHierarchyStressTest() throws Exception {
        logger.info("Testing inheritance with complex override patterns...");
        
        StringBuilder yaml = new StringBuilder();
        yaml.append("""
            metadata:
              name: "Deep Inheritance Stress Test"
              type: "rule-config"
              version: "1.0.0"

            categories:
            """);

        // Create 50 categories with full metadata
        int numCategories = 50;
        for (int i = 1; i <= numCategories; i++) {
            yaml.append(String.format("""
              - name: "category%d"
                description: "Category %d"
                priority: %d
                enabled: true
                business-domain: "Domain%d"
                business-owner: "Owner%d"
                created-by: "Creator%d"
                source-system: "System%d"
                effective-date: "2024-%02d-01"
                expiration-date: "2024-%02d-28"
                
            """, i, i, i, i, i, i, i, (i % 12) + 1, (i % 12) + 1));
        }

        yaml.append("rules:\n");
        // Create 1000 rules with various override patterns
        int numRules = 1000;
        for (int i = 1; i <= numRules; i++) {
            int categoryIndex = (i % numCategories) + 1;
            yaml.append(String.format("""
              - id: "rule%d"
                name: "Rule %d"
                category: "category%d"
                condition: "true"
                message: "Test rule %d"
            """, i, i, categoryIndex, i));
            
            // Add overrides for some rules
            if (i % 3 == 0) {
                yaml.append(String.format("    business-owner: \"RuleOwner%d\"\n", i));
            }
            if (i % 5 == 0) {
                yaml.append(String.format("    created-by: \"RuleCreator%d\"\n", i));
            }
            if (i % 7 == 0) {
                yaml.append(String.format("    business-domain: \"RuleDomain%d\"\n", i));
            }
            yaml.append("\n");
        }

        yaml.append("rule-groups:\n");
        // Create 200 rule groups with various override patterns
        int numGroups = 200;
        for (int i = 1; i <= numGroups; i++) {
            int categoryIndex = (i % numCategories) + 1;
            int ruleIndex1 = (i * 3) % numRules + 1;
            int ruleIndex2 = (i * 3 + 1) % numRules + 1;
            yaml.append(String.format("""
              - id: "group%d"
                name: "Group %d"
                category: "category%d"
                rules: ["rule%d", "rule%d"]
            """, i, i, categoryIndex, ruleIndex1, ruleIndex2));
            
            // Add overrides for some groups
            if (i % 4 == 0) {
                yaml.append(String.format("    business-domain: \"GroupDomain%d\"\n", i));
            }
            if (i % 6 == 0) {
                yaml.append(String.format("    business-owner: \"GroupOwner%d\"\n", i));
            }
            if (i % 8 == 0) {
                yaml.append(String.format("    created-by: \"GroupCreator%d\"\n", i));
            }
            yaml.append("\n");
        }

        long startTime = System.currentTimeMillis();
        
        YamlRuleConfiguration config = loader.fromYamlString(yaml.toString());
        List<Rule> rules = factory.createRules(config);
        List<RuleGroup> groups = factory.createRuleGroups(config, factory.createRulesEngineConfiguration(config));
        
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        
        logger.info("Processed {} rules and {} groups with complex inheritance in {} ms", 
                   rules.size(), groups.size(), duration);

        // Verify correct counts
        assertEquals(numRules, rules.size(), "Should create all rules");
        assertEquals(numGroups, groups.size(), "Should create all groups");

        // Verify inheritance and override logic works correctly
        verifyInheritanceLogic(rules, groups);
        
        // Performance assertion
        assertTrue(duration < 8000, "Complex inheritance should process within 8 seconds");
    }

    @Test
    @DisplayName("Memory efficiency with repeated category references")
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    void testMemoryEfficiencyWithRepeatedCategoryReferences() throws Exception {
        logger.info("Testing memory efficiency with repeated category references...");
        
        StringBuilder yaml = new StringBuilder();
        yaml.append("""
            metadata:
              name: "Memory Efficiency Test"
              type: "rule-config"
              version: "1.0.0"

            categories:
              - name: "shared-category"
                description: "Heavily referenced category"
                priority: 10
                enabled: true
                business-domain: "SharedDomain"
                business-owner: "SharedOwner"
                created-by: "SharedCreator"
                effective-date: "2024-01-01"
                expiration-date: "2024-12-31"

            rules:
            """);

        // Create 2000 rules all referencing the same category
        int numRules = 2000;
        for (int i = 1; i <= numRules; i++) {
            yaml.append(String.format("""
              - id: "rule%d"
                name: "Rule %d"
                category: "shared-category"
                condition: "true"
                message: "Test rule %d"
                
            """, i, i, i));
        }

        yaml.append("rule-groups:\n");
        // Create 500 rule groups all referencing the same category
        int numGroups = 500;
        for (int i = 1; i <= numGroups; i++) {
            int ruleIndex = (i * 4) % numRules + 1;
            yaml.append(String.format("""
              - id: "group%d"
                name: "Group %d"
                category: "shared-category"
                rules: ["rule%d"]
                
            """, i, i, ruleIndex));
        }

        long startTime = System.currentTimeMillis();
        Runtime runtime = Runtime.getRuntime();
        long memoryBefore = runtime.totalMemory() - runtime.freeMemory();
        
        YamlRuleConfiguration config = loader.fromYamlString(yaml.toString());
        List<Rule> rules = factory.createRules(config);
        List<RuleGroup> groups = factory.createRuleGroups(config, factory.createRulesEngineConfiguration(config));
        
        long memoryAfter = runtime.totalMemory() - runtime.freeMemory();
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        long memoryUsed = memoryAfter - memoryBefore;
        
        logger.info("Created {} rules and {} groups sharing one category in {} ms, using {} bytes", 
                   rules.size(), groups.size(), duration, memoryUsed);

        // Verify correct counts
        assertEquals(numRules, rules.size(), "Should create all rules");
        assertEquals(numGroups, groups.size(), "Should create all groups");

        // Verify all entities have the same inherited metadata
        for (Rule rule : rules) {
            assertNotNull(rule.getMetadata(), "All rules should have metadata");
            assertEquals("SharedDomain", rule.getMetadata().getBusinessDomain().orElse(null));
        }
        
        for (RuleGroup group : groups) {
            assertEquals("SharedDomain", group.getBusinessDomain(), "All groups should have shared domain");
        }
        
        // Performance and memory assertions
        assertTrue(duration < 5000, "Shared category processing should be efficient");
        logger.info("Actual memory usage: {} bytes ({} MB)", memoryUsed, memoryUsed / 1_000_000.0);
        assertTrue(memoryUsed < 150_000_000, "Memory usage should be reasonable (< 150MB)");
    }

    private void verifyInheritanceLogic(List<Rule> rules, List<RuleGroup> groups) {
        // Verify a sample of rules have correct inheritance/override behavior
        for (int i = 0; i < Math.min(10, rules.size()); i++) {
            Rule rule = rules.get(i);
            assertNotNull(rule.getMetadata(), "Rule should have metadata");
            
            // Rules with index divisible by 3 should have overridden business-owner
            if ((i + 1) % 3 == 0) {
                assertTrue(rule.getMetadata().getBusinessOwner().orElse("").startsWith("RuleOwner"),
                          "Rule should have overridden business owner");
            }
        }
        
        // Verify a sample of groups have correct inheritance/override behavior
        for (int i = 0; i < Math.min(10, groups.size()); i++) {
            RuleGroup group = groups.get(i);
            assertNotNull(group.getBusinessDomain(), "Group should have business domain");
            
            // Groups with index divisible by 4 should have overridden business-domain
            if ((i + 1) % 4 == 0) {
                assertTrue(group.getBusinessDomain().startsWith("GroupDomain"),
                          "Group should have overridden business domain");
            }
        }
    }
}
