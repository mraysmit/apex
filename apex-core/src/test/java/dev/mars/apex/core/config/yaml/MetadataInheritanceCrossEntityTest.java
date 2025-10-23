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
import dev.mars.apex.core.engine.model.metadata.RuleMetadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test suite for cross-entity metadata inheritance scenarios,
 * including rules and rule groups in the same configuration.
 */
@DisplayName("Cross-Entity Metadata Inheritance Tests")
class MetadataInheritanceCrossEntityTest {

    private static final Logger logger = LoggerFactory.getLogger(MetadataInheritanceCrossEntityTest.class);

    private YamlConfigurationLoader loader;
    private YamlRuleFactory factory;

    @BeforeEach
    void setUp() {
        loader = new YamlConfigurationLoader();
        factory = new YamlRuleFactory();
    }

    @Test
    @DisplayName("Rules and rule groups in same category inherit consistently")
    void testRulesAndGroupsInSameCategoryInheritConsistently() throws Exception {
        String yaml = """
            metadata:
              name: "Cross-Entity Consistency Test"
              type: "rule-config"
              version: "1.0.0"

            categories:
              - name: "shared-category"
                description: "Category shared by rules and groups"
                priority: 10
                enabled: true
                business-domain: "SharedDomain"
                business-owner: "SharedOwner"
                created-by: "SharedCreator"
                effective-date: "2024-01-01"
                expiration-date: "2024-12-31"

            rules:
              - id: "rule1"
                name: "Rule in shared category"
                category: "shared-category"
                condition: "true"
                message: "Test rule"
                
              - id: "rule2"
                name: "Another rule in shared category"
                category: "shared-category"
                condition: "true"
                message: "Test rule 2"
                
            rule-groups:
              - id: "group1"
                name: "Group in shared category"
                category: "shared-category"
                rules: ["rule1", "rule2"]
                
              - id: "group2"
                name: "Another group in shared category"
                category: "shared-category"
                rules: ["rule1"]
            """;

        YamlRuleConfiguration config = loader.fromYamlString(yaml);
        List<Rule> rules = factory.createRules(config);
        List<RuleGroup> groups = factory.createRuleGroups(config, factory.createRulesEngineConfiguration(config));

        // Verify all entities inherit the same metadata from shared category
        Rule rule1 = findRuleById(rules, "rule1");
        Rule rule2 = findRuleById(rules, "rule2");
        RuleGroup group1 = findRuleGroupById(groups, "group1");
        RuleGroup group2 = findRuleGroupById(groups, "group2");

        // All should have identical inherited metadata
        String expectedDomain = "SharedDomain";
        String expectedOwner = "SharedOwner";
        String expectedCreator = "SharedCreator";
        String expectedEffective = "2024-01-01";
        String expectedExpiration = "2024-12-31";

        assertRuleMetadata(rule1, expectedDomain, expectedOwner, expectedCreator, expectedEffective, expectedExpiration);
        assertRuleMetadata(rule2, expectedDomain, expectedOwner, expectedCreator, expectedEffective, expectedExpiration);
        assertRuleGroupMetadata(group1, expectedDomain, expectedOwner, expectedCreator, expectedEffective, expectedExpiration);
        assertRuleGroupMetadata(group2, expectedDomain, expectedOwner, expectedCreator, expectedEffective, expectedExpiration);
    }

    @Test
    @DisplayName("Rules and rule groups with different override patterns")
    void testRulesAndGroupsWithDifferentOverridePatterns() throws Exception {
        String yaml = """
            metadata:
              name: "Different Override Patterns Test"
              type: "rule-config"
              version: "1.0.0"

            categories:
              - name: "base-category"
                description: "Base category for override testing"
                priority: 10
                enabled: true
                business-domain: "BaseDomain"
                business-owner: "BaseOwner"
                created-by: "BaseCreator"
                effective-date: "2024-01-01"
                expiration-date: "2024-12-31"

            rules:
              - id: "rule-no-override"
                name: "Rule with no overrides"
                category: "base-category"
                condition: "true"
                message: "Test"
                
              - id: "rule-partial-override"
                name: "Rule with partial override"
                category: "base-category"
                condition: "true"
                message: "Test"
                business-owner: "RuleOwner"
                
              - id: "rule-full-override"
                name: "Rule with full override"
                category: "base-category"
                condition: "true"
                message: "Test"
                business-domain: "RuleDomain"
                business-owner: "RuleOwner"
                created-by: "RuleCreator"
                effective-date: "2025-01-01"
                expiration-date: "2025-12-31"
                
            rule-groups:
              - id: "group-no-override"
                name: "Group with no overrides"
                category: "base-category"
                rules: ["rule-no-override"]
                
              - id: "group-partial-override"
                name: "Group with partial override"
                category: "base-category"
                business-domain: "GroupDomain"
                created-by: "GroupCreator"
                rules: ["rule-partial-override"]
                
              - id: "group-full-override"
                name: "Group with full override"
                category: "base-category"
                business-domain: "GroupDomain"
                business-owner: "GroupOwner"
                created-by: "GroupCreator"
                effective-date: "2025-01-01"
                expiration-date: "2025-12-31"
                rules: ["rule-full-override"]
            """;

        YamlRuleConfiguration config = loader.fromYamlString(yaml);
        List<Rule> rules = factory.createRules(config);
        List<RuleGroup> groups = factory.createRuleGroups(config, factory.createRulesEngineConfiguration(config));

        // Verify no override entities
        Rule ruleNoOverride = findRuleById(rules, "rule-no-override");
        RuleGroup groupNoOverride = findRuleGroupById(groups, "group-no-override");
        assertRuleMetadata(ruleNoOverride, "BaseDomain", "BaseOwner", "BaseCreator", "2024-01-01", "2024-12-31");
        assertRuleGroupMetadata(groupNoOverride, "BaseDomain", "BaseOwner", "BaseCreator", "2024-01-01", "2024-12-31");

        // Verify partial override entities
        Rule rulePartialOverride = findRuleById(rules, "rule-partial-override");
        RuleGroup groupPartialOverride = findRuleGroupById(groups, "group-partial-override");
        assertRuleMetadata(rulePartialOverride, "BaseDomain", "RuleOwner", "BaseCreator", "2024-01-01", "2024-12-31");
        assertRuleGroupMetadata(groupPartialOverride, "GroupDomain", "BaseOwner", "GroupCreator", "2024-01-01", "2024-12-31");

        // Verify full override entities
        Rule ruleFullOverride = findRuleById(rules, "rule-full-override");
        RuleGroup groupFullOverride = findRuleGroupById(groups, "group-full-override");
        assertRuleMetadata(ruleFullOverride, "RuleDomain", "RuleOwner", "RuleCreator", "2025-01-01", "2025-12-31");
        assertRuleGroupMetadata(groupFullOverride, "GroupDomain", "GroupOwner", "GroupCreator", "2025-01-01", "2025-12-31");
    }

    @Test
    @DisplayName("Rules in groups with different categories")
    void testRulesInGroupsWithDifferentCategories() throws Exception {
        String yaml = """
            metadata:
              name: "Rules in Groups Different Categories Test"
              type: "rule-config"
              version: "1.0.0"

            categories:
              - name: "rule-category"
                description: "Category for rules"
                priority: 10
                enabled: true
                business-domain: "RuleDomain"
                business-owner: "RuleOwner"
                created-by: "RuleCreator"
                effective-date: "2024-01-01"
                expiration-date: "2024-12-31"
                
              - name: "group-category"
                description: "Category for groups"
                priority: 20
                enabled: true
                business-domain: "GroupDomain"
                business-owner: "GroupOwner"
                created-by: "GroupCreator"
                effective-date: "2025-01-01"
                expiration-date: "2025-12-31"

            rules:
              - id: "rule1"
                name: "Rule with rule category"
                category: "rule-category"
                condition: "true"
                message: "Test"
                
              - id: "rule2"
                name: "Rule with no category"
                condition: "true"
                message: "Test"
                
            rule-groups:
              - id: "group1"
                name: "Group with group category containing categorized rule"
                category: "group-category"
                rules: ["rule1"]
                
              - id: "group2"
                name: "Group with group category containing uncategorized rule"
                category: "group-category"
                rules: ["rule2"]
            """;

        YamlRuleConfiguration config = loader.fromYamlString(yaml);
        List<Rule> rules = factory.createRules(config);
        List<RuleGroup> groups = factory.createRuleGroups(config, factory.createRulesEngineConfiguration(config));

        // Verify rule inherits from its own category
        Rule rule1 = findRuleById(rules, "rule1");
        assertRuleMetadata(rule1, "RuleDomain", "RuleOwner", "RuleCreator", "2024-01-01", "2024-12-31");

        // Verify rule with no category has no metadata
        Rule rule2 = findRuleById(rules, "rule2");
        assertRuleMetadata(rule2, null, null, null, null, null);

        // Verify groups inherit from their own category regardless of contained rules
        RuleGroup group1 = findRuleGroupById(groups, "group1");
        RuleGroup group2 = findRuleGroupById(groups, "group2");
        assertRuleGroupMetadata(group1, "GroupDomain", "GroupOwner", "GroupCreator", "2025-01-01", "2025-12-31");
        assertRuleGroupMetadata(group2, "GroupDomain", "GroupOwner", "GroupCreator", "2025-01-01", "2025-12-31");
    }

    // Helper methods
    private Rule findRuleById(List<Rule> rules, String id) {
        return rules.stream()
                .filter(r -> id.equals(r.getId()))
                .findFirst()
                .orElse(null);
    }

    private RuleGroup findRuleGroupById(List<RuleGroup> groups, String id) {
        return groups.stream()
                .filter(g -> id.equals(g.getId()))
                .findFirst()
                .orElse(null);
    }

    private void assertRuleMetadata(Rule rule, String domain, String owner, String creator, String effectiveDate, String expirationDate) {
        assertNotNull(rule, "Rule should not be null");
        RuleMetadata metadata = rule.getMetadata();
        
        if (domain != null) {
            assertNotNull(metadata, "Rule should have metadata when domain is expected");
            assertEquals(domain, metadata.getBusinessDomain().orElse(null), "Business domain should match");
        }
        if (owner != null) {
            assertNotNull(metadata, "Rule should have metadata when owner is expected");
            assertEquals(owner, metadata.getBusinessOwner().orElse(null), "Business owner should match");
        }
        if (creator != null) {
            assertNotNull(metadata, "Rule should have metadata when creator is expected");
            assertEquals(creator, metadata.getCreatedByUser(), "Created by should match");
        }
    }

    private void assertRuleGroupMetadata(RuleGroup group, String domain, String owner, String creator, String effectiveDate, String expirationDate) {
        assertNotNull(group, "Rule group should not be null");
        
        assertEquals(domain, group.getBusinessDomain(), "Business domain should match");
        assertEquals(owner, group.getBusinessOwner(), "Business owner should match");
        assertEquals(creator, group.getCreatedBy(), "Created by should match");
        assertEquals(effectiveDate, group.getEffectiveDate(), "Effective date should match");
        assertEquals(expirationDate, group.getExpirationDate(), "Expiration date should match");
    }
}
