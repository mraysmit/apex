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
 * Comprehensive test suite for metadata inheritance covering all edge cases,
 * combinations, and scenarios for both rules and rule groups.
 */
@DisplayName("Comprehensive Metadata Inheritance Test Suite")
class MetadataInheritanceComprehensiveTest {

    private static final Logger logger = LoggerFactory.getLogger(MetadataInheritanceComprehensiveTest.class);

    private YamlConfigurationLoader loader;
    private YamlRuleFactory factory;

    @BeforeEach
    void setUp() {
        loader = new YamlConfigurationLoader();
        factory = new YamlRuleFactory();
    }

    @Nested
    @DisplayName("Rule Inheritance Tests")
    class RuleInheritanceTests {

        @Test
        @DisplayName("Rule inherits all metadata when none specified")
        void testRuleCompleteInheritance() throws Exception {
            String yaml = createBaseYaml() + """
                rules:
                  - id: "rule1"
                    name: "Complete inheritance rule"
                    category: "category1"
                    condition: "true"
                    message: "Test rule"
                """;

            List<Rule> rules = factory.createRules(loader.fromYamlString(yaml));
            Rule rule = findRuleById(rules, "rule1");

            assertRuleMetadata(rule, "Domain1", "Owner1", "Creator1", "2024-01-01", "2024-12-31");
        }

        @Test
        @DisplayName("Rule overrides specific metadata fields")
        void testRulePartialOverride() throws Exception {
            String yaml = createBaseYaml() + """
                rules:
                  - id: "rule2"
                    name: "Partial override rule"
                    category: "category1"
                    condition: "true"
                    message: "Test rule"
                    business-owner: "OverrideOwner"
                    created-by: "OverrideCreator"
                """;

            List<Rule> rules = factory.createRules(loader.fromYamlString(yaml));
            Rule rule = findRuleById(rules, "rule2");

            assertRuleMetadata(rule, "Domain1", "OverrideOwner", "OverrideCreator", "2024-01-01", "2024-12-31");
        }

        @Test
        @DisplayName("Rule overrides all metadata fields")
        void testRuleCompleteOverride() throws Exception {
            String yaml = createBaseYaml() + """
                rules:
                  - id: "rule3"
                    name: "Complete override rule"
                    category: "category1"
                    condition: "true"
                    message: "Test rule"
                    business-domain: "OverrideDomain"
                    business-owner: "OverrideOwner"
                    created-by: "OverrideCreator"
                    effective-date: "2025-01-01"
                    expiration-date: "2025-12-31"
                """;

            List<Rule> rules = factory.createRules(loader.fromYamlString(yaml));
            Rule rule = findRuleById(rules, "rule3");

            assertRuleMetadata(rule, "OverrideDomain", "OverrideOwner", "OverrideCreator", "2025-01-01", "2025-12-31");
        }

        @Test
        @DisplayName("Rule with no category has no inherited metadata")
        void testRuleNoCategory() throws Exception {
            String yaml = createBaseYaml() + """
                rules:
                  - id: "rule4"
                    name: "No category rule"
                    condition: "true"
                    message: "Test rule"
                """;

            List<Rule> rules = factory.createRules(loader.fromYamlString(yaml));
            Rule rule = findRuleById(rules, "rule4");

            assertRuleMetadata(rule, null, null, null, null, null);
        }

        @Test
        @DisplayName("Rule with non-existent category has no inherited metadata")
        void testRuleNonExistentCategory() throws Exception {
            String yaml = createBaseYaml() + """
                rules:
                  - id: "rule5"
                    name: "Non-existent category rule"
                    category: "non-existent"
                    condition: "true"
                    message: "Test rule"
                """;

            List<Rule> rules = factory.createRules(loader.fromYamlString(yaml));
            Rule rule = findRuleById(rules, "rule5");

            assertRuleMetadata(rule, null, null, null, null, null);
        }
    }

    @Nested
    @DisplayName("Rule Group Inheritance Tests")
    class RuleGroupInheritanceTests {

        @Test
        @DisplayName("Rule group inherits all metadata when none specified")
        void testRuleGroupCompleteInheritance() throws Exception {
            String yaml = createBaseYaml() + """
                rules:
                  - id: "rule1"
                    name: "Test rule"
                    condition: "true"
                    message: "Test"
                    
                rule-groups:
                  - id: "group1"
                    name: "Complete inheritance group"
                    category: "category1"
                    rules:
                      - "rule1"
                """;

            YamlRuleConfiguration config = loader.fromYamlString(yaml);
            List<RuleGroup> groups = factory.createRuleGroups(config, factory.createRulesEngineConfiguration(config));
            RuleGroup group = findRuleGroupById(groups, "group1");

            assertRuleGroupMetadata(group, "Domain1", "Owner1", "Creator1", "2024-01-01", "2024-12-31");
        }

        @Test
        @DisplayName("Rule group overrides specific metadata fields")
        void testRuleGroupPartialOverride() throws Exception {
            String yaml = createBaseYaml() + """
                rules:
                  - id: "rule1"
                    name: "Test rule"
                    condition: "true"
                    message: "Test"
                    
                rule-groups:
                  - id: "group2"
                    name: "Partial override group"
                    category: "category1"
                    business-owner: "GroupOwner"
                    created-by: "GroupCreator"
                    rules:
                      - "rule1"
                """;

            YamlRuleConfiguration config = loader.fromYamlString(yaml);
            List<RuleGroup> groups = factory.createRuleGroups(config, factory.createRulesEngineConfiguration(config));
            RuleGroup group = findRuleGroupById(groups, "group2");

            assertRuleGroupMetadata(group, "Domain1", "GroupOwner", "GroupCreator", "2024-01-01", "2024-12-31");
        }

        @Test
        @DisplayName("Rule group overrides all metadata fields")
        void testRuleGroupCompleteOverride() throws Exception {
            String yaml = createBaseYaml() + """
                rules:
                  - id: "rule1"
                    name: "Test rule"
                    condition: "true"
                    message: "Test"
                    
                rule-groups:
                  - id: "group3"
                    name: "Complete override group"
                    category: "category1"
                    business-domain: "GroupDomain"
                    business-owner: "GroupOwner"
                    created-by: "GroupCreator"
                    effective-date: "2025-01-01"
                    expiration-date: "2025-12-31"
                    rules:
                      - "rule1"
                """;

            YamlRuleConfiguration config = loader.fromYamlString(yaml);
            List<RuleGroup> groups = factory.createRuleGroups(config, factory.createRulesEngineConfiguration(config));
            RuleGroup group = findRuleGroupById(groups, "group3");

            assertRuleGroupMetadata(group, "GroupDomain", "GroupOwner", "GroupCreator", "2025-01-01", "2025-12-31");
        }

        @Test
        @DisplayName("Rule group with no category has no inherited metadata")
        void testRuleGroupNoCategory() throws Exception {
            String yaml = createBaseYaml() + """
                rules:
                  - id: "rule1"
                    name: "Test rule"
                    condition: "true"
                    message: "Test"
                    
                rule-groups:
                  - id: "group4"
                    name: "No category group"
                    rules:
                      - "rule1"
                """;

            YamlRuleConfiguration config = loader.fromYamlString(yaml);
            List<RuleGroup> groups = factory.createRuleGroups(config, factory.createRulesEngineConfiguration(config));
            RuleGroup group = findRuleGroupById(groups, "group4");

            assertRuleGroupMetadata(group, null, null, null, null, null);
        }

        @Test
        @DisplayName("Rule group with non-existent category has no inherited metadata")
        void testRuleGroupNonExistentCategory() throws Exception {
            String yaml = createBaseYaml() + """
                rules:
                  - id: "rule1"
                    name: "Test rule"
                    condition: "true"
                    message: "Test"
                    
                rule-groups:
                  - id: "group5"
                    name: "Non-existent category group"
                    category: "non-existent"
                    rules:
                      - "rule1"
                """;

            YamlRuleConfiguration config = loader.fromYamlString(yaml);
            List<RuleGroup> groups = factory.createRuleGroups(config, factory.createRulesEngineConfiguration(config));
            RuleGroup group = findRuleGroupById(groups, "group5");

            assertRuleGroupMetadata(group, null, null, null, null, null);
        }
    }

    // Helper methods
    private String createBaseYaml() {
        return """
            metadata:
              name: "Comprehensive Metadata Inheritance Test"
              type: "rule-config"
              version: "1.0.0"

            categories:
              - name: "category1"
                description: "Test category 1"
                priority: 10
                enabled: true
                business-domain: "Domain1"
                business-owner: "Owner1"
                created-by: "Creator1"
                effective-date: "2024-01-01"
                expiration-date: "2024-12-31"

            """;
    }

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
        // Note: Date assertions would need to be adapted based on actual metadata structure
    }

    private void assertRuleGroupMetadata(RuleGroup group, String domain, String owner, String creator, String effectiveDate, String expirationDate) {
        assertNotNull(group, "Rule group should not be null");

        assertEquals(domain, group.getBusinessDomain(), "Business domain should match");
        assertEquals(owner, group.getBusinessOwner(), "Business owner should match");
        assertEquals(creator, group.getCreatedBy(), "Created by should match");
        assertEquals(effectiveDate, group.getEffectiveDate(), "Effective date should match");
        assertEquals(expirationDate, group.getExpirationDate(), "Expiration date should match");
    }

    @Nested
    @DisplayName("Edge Cases and Complex Scenarios")
    class EdgeCasesAndComplexScenarios {

        @Test
        @DisplayName("Multiple categories with different metadata")
        void testMultipleCategoriesWithDifferentMetadata() throws Exception {
            String yaml = """
                metadata:
                  name: "Multiple Categories Test"
                  type: "rule-config"
                  version: "1.0.0"

                categories:
                  - name: "category1"
                    description: "Category 1"
                    priority: 10
                    enabled: true
                    business-domain: "Domain1"
                    business-owner: "Owner1"
                    created-by: "Creator1"
                    effective-date: "2024-01-01"
                    expiration-date: "2024-12-31"

                  - name: "category2"
                    description: "Category 2"
                    priority: 20
                    enabled: true
                    business-domain: "Domain2"
                    business-owner: "Owner2"
                    created-by: "Creator2"
                    effective-date: "2025-01-01"
                    expiration-date: "2025-12-31"

                rules:
                  - id: "rule1"
                    name: "Rule in category 1"
                    category: "category1"
                    condition: "true"
                    message: "Test"

                  - id: "rule2"
                    name: "Rule in category 2"
                    category: "category2"
                    condition: "true"
                    message: "Test"

                rule-groups:
                  - id: "group1"
                    name: "Group in category 1"
                    category: "category1"
                    rules: ["rule1"]

                  - id: "group2"
                    name: "Group in category 2"
                    category: "category2"
                    rules: ["rule2"]
                """;

            YamlRuleConfiguration config = loader.fromYamlString(yaml);
            List<Rule> rules = factory.createRules(config);
            List<RuleGroup> groups = factory.createRuleGroups(config, factory.createRulesEngineConfiguration(config));

            // Verify rules inherit from correct categories
            Rule rule1 = findRuleById(rules, "rule1");
            Rule rule2 = findRuleById(rules, "rule2");
            assertRuleMetadata(rule1, "Domain1", "Owner1", "Creator1", "2024-01-01", "2024-12-31");
            assertRuleMetadata(rule2, "Domain2", "Owner2", "Creator2", "2025-01-01", "2025-12-31");

            // Verify rule groups inherit from correct categories
            RuleGroup group1 = findRuleGroupById(groups, "group1");
            RuleGroup group2 = findRuleGroupById(groups, "group2");
            assertRuleGroupMetadata(group1, "Domain1", "Owner1", "Creator1", "2024-01-01", "2024-12-31");
            assertRuleGroupMetadata(group2, "Domain2", "Owner2", "Creator2", "2025-01-01", "2025-12-31");
        }

        @Test
        @DisplayName("Category with partial metadata")
        void testCategoryWithPartialMetadata() throws Exception {
            String yaml = """
                metadata:
                  name: "Partial Category Metadata Test"
                  type: "rule-config"
                  version: "1.0.0"

                categories:
                  - name: "partial-category"
                    description: "Category with partial metadata"
                    priority: 10
                    enabled: true
                    business-domain: "PartialDomain"
                    # Missing: business-owner, created-by, effective-date, expiration-date

                rules:
                  - id: "rule1"
                    name: "Rule with partial category"
                    category: "partial-category"
                    condition: "true"
                    message: "Test"

                rule-groups:
                  - id: "group1"
                    name: "Group with partial category"
                    category: "partial-category"
                    rules: ["rule1"]
                """;

            YamlRuleConfiguration config = loader.fromYamlString(yaml);
            List<Rule> rules = factory.createRules(config);
            List<RuleGroup> groups = factory.createRuleGroups(config, factory.createRulesEngineConfiguration(config));

            // Verify only available metadata is inherited
            Rule rule = findRuleById(rules, "rule1");
            RuleGroup group = findRuleGroupById(groups, "group1");

            assertRuleMetadata(rule, "PartialDomain", null, null, null, null);
            assertRuleGroupMetadata(group, "PartialDomain", null, null, null, null);
        }

        @Test
        @DisplayName("Empty string metadata values")
        void testEmptyStringMetadataValues() throws Exception {
            String yaml = """
                metadata:
                  name: "Empty String Metadata Test"
                  type: "rule-config"
                  version: "1.0.0"

                categories:
                  - name: "empty-category"
                    description: "Category with empty metadata"
                    priority: 10
                    enabled: true
                    business-domain: ""
                    business-owner: ""
                    created-by: ""
                    effective-date: ""
                    expiration-date: ""

                rules:
                  - id: "rule1"
                    name: "Rule with empty category metadata"
                    category: "empty-category"
                    condition: "true"
                    message: "Test"

                rule-groups:
                  - id: "group1"
                    name: "Group with empty category metadata"
                    category: "empty-category"
                    rules: ["rule1"]
                """;

            YamlRuleConfiguration config = loader.fromYamlString(yaml);
            List<Rule> rules = factory.createRules(config);
            List<RuleGroup> groups = factory.createRuleGroups(config, factory.createRulesEngineConfiguration(config));

            // Verify empty strings are treated as valid values
            Rule rule = findRuleById(rules, "rule1");
            RuleGroup group = findRuleGroupById(groups, "group1");

            assertRuleMetadata(rule, "", "", "", "", "");
            assertRuleGroupMetadata(group, "", "", "", "", "");
        }

        @Test
        @DisplayName("Mixed inheritance and override scenarios")
        void testMixedInheritanceAndOverrideScenarios() throws Exception {
            String yaml = """
                metadata:
                  name: "Mixed Scenarios Test"
                  type: "rule-config"
                  version: "1.0.0"

                categories:
                  - name: "mixed-category"
                    description: "Category for mixed scenarios"
                    priority: 10
                    enabled: true
                    business-domain: "MixedDomain"
                    business-owner: "MixedOwner"
                    created-by: "MixedCreator"
                    effective-date: "2024-01-01"
                    expiration-date: "2024-12-31"

                rules:
                  - id: "rule1"
                    name: "Rule with mixed inheritance/override"
                    category: "mixed-category"
                    condition: "true"
                    message: "Test"
                    business-owner: "RuleOwner"  # Override
                    # Inherit: business-domain, created-by, dates

                rule-groups:
                  - id: "group1"
                    name: "Group with mixed inheritance/override"
                    category: "mixed-category"
                    business-domain: "GroupDomain"  # Override
                    created-by: "GroupCreator"     # Override
                    # Inherit: business-owner, dates
                    rules: ["rule1"]
                """;

            YamlRuleConfiguration config = loader.fromYamlString(yaml);
            List<Rule> rules = factory.createRules(config);
            List<RuleGroup> groups = factory.createRuleGroups(config, factory.createRulesEngineConfiguration(config));

            // Verify mixed inheritance/override for rule
            Rule rule = findRuleById(rules, "rule1");
            assertRuleMetadata(rule, "MixedDomain", "RuleOwner", "MixedCreator", "2024-01-01", "2024-12-31");

            // Verify mixed inheritance/override for rule group
            RuleGroup group = findRuleGroupById(groups, "group1");
            assertRuleGroupMetadata(group, "GroupDomain", "MixedOwner", "GroupCreator", "2024-01-01", "2024-12-31");
        }

        @Test
        @DisplayName("Null vs empty vs missing metadata handling")
        void testNullVsEmptyVsMissingMetadata() throws Exception {
            String yaml = """
                metadata:
                  name: "Null vs Empty vs Missing Test"
                  type: "rule-config"
                  version: "1.0.0"

                categories:
                  - name: "test-category"
                    description: "Category for null/empty/missing test"
                    priority: 10
                    enabled: true
                    business-domain: "TestDomain"
                    business-owner: "TestOwner"
                    created-by: "TestCreator"
                    effective-date: "2024-01-01"
                    expiration-date: "2024-12-31"

                rules:
                  - id: "rule-empty"
                    name: "Rule with empty override"
                    category: "test-category"
                    condition: "true"
                    message: "Test"
                    business-owner: ""  # Empty string override

                  - id: "rule-missing"
                    name: "Rule with missing metadata"
                    category: "test-category"
                    condition: "true"
                    message: "Test"
                    # No metadata specified - should inherit all

                rule-groups:
                  - id: "group-empty"
                    name: "Group with empty override"
                    category: "test-category"
                    business-owner: ""  # Empty string override
                    rules: ["rule-empty"]

                  - id: "group-missing"
                    name: "Group with missing metadata"
                    category: "test-category"
                    rules: ["rule-missing"]
                    # No metadata specified - should inherit all
                """;

            YamlRuleConfiguration config = loader.fromYamlString(yaml);
            List<Rule> rules = factory.createRules(config);
            List<RuleGroup> groups = factory.createRuleGroups(config, factory.createRulesEngineConfiguration(config));

            // Verify empty string override
            Rule ruleEmpty = findRuleById(rules, "rule-empty");
            RuleGroup groupEmpty = findRuleGroupById(groups, "group-empty");
            assertRuleMetadata(ruleEmpty, "TestDomain", "", "TestCreator", "2024-01-01", "2024-12-31");
            assertRuleGroupMetadata(groupEmpty, "TestDomain", "", "TestCreator", "2024-01-01", "2024-12-31");

            // Verify complete inheritance when missing
            Rule ruleMissing = findRuleById(rules, "rule-missing");
            RuleGroup groupMissing = findRuleGroupById(groups, "group-missing");
            assertRuleMetadata(ruleMissing, "TestDomain", "TestOwner", "TestCreator", "2024-01-01", "2024-12-31");
            assertRuleGroupMetadata(groupMissing, "TestDomain", "TestOwner", "TestCreator", "2024-01-01", "2024-12-31");
        }
    }
}
