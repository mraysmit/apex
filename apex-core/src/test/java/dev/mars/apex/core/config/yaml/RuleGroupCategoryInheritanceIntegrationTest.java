package dev.mars.apex.core.config.yaml;

import dev.mars.apex.core.engine.model.RuleGroup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for rule group category inheritance functionality.
 * Tests that rule groups properly inherit enterprise metadata from their assigned categories.
 */
@DisplayName("Rule Group Category Inheritance Integration Tests")
class RuleGroupCategoryInheritanceIntegrationTest {

    private YamlConfigurationLoader loader;
    private YamlRuleFactory factory;

    @BeforeEach
    void setUp() {
        loader = new YamlConfigurationLoader();
        factory = new YamlRuleFactory();
    }

    @Test
    @DisplayName("Rule group should inherit all metadata from category when none specified")
    void testCompleteMetadataInheritanceForRuleGroup() throws Exception {
        String yaml = """
            metadata:
              name: "Rule Group Category Inheritance Test"
              type: "rule-config"
              version: "1.0.0"

            categories:
              - name: "Compliance"
                description: "Compliance and regulatory rules"
                priority: 10
                enabled: true
                business-domain: "Financial Services"
                business-owner: "Compliance Team"
                created-by: "John Smith"
                effective-date: "2024-01-01"
                expiration-date: "2024-12-31"

            rules:
              - id: "rule1"
                name: "Basic validation rule"
                condition: "amount > 0"
                message: "Amount must be positive"
                category: "Compliance"

              - id: "rule2"
                name: "Secondary validation rule"
                condition: "currency != null"
                message: "Currency must be specified"
                category: "Compliance"

            rule-groups:
              - id: "compliance-group"
                name: "Compliance Rule Group"
                description: "Group of compliance rules"
                category: "Compliance"
                operator: "AND"
                rule-ids:
                  - "rule1"
                  - "rule2"
            """;

        YamlRuleConfiguration config = loader.fromYamlString(yaml);
        assertNotNull(config, "Configuration should be loaded");

        // Create rule groups with the factory
        List<RuleGroup> ruleGroups = factory.createRuleGroups(config, factory.createRulesEngineConfiguration(config));
        assertNotNull(ruleGroups, "Rule groups should be created");
        assertEquals(1, ruleGroups.size(), "Should have one rule group");

        RuleGroup ruleGroup = ruleGroups.get(0);
        assertEquals("compliance-group", ruleGroup.getId(), "Rule group ID should match");

        // Verify metadata inheritance from category
        assertEquals("Financial Services", ruleGroup.getBusinessDomain(), "Should inherit business domain from category");
        assertEquals("Compliance Team", ruleGroup.getBusinessOwner(), "Should inherit business owner from category");
        assertEquals("John Smith", ruleGroup.getCreatedBy(), "Should inherit created-by from category");
        assertEquals("2024-01-01", ruleGroup.getEffectiveDate(), "Should inherit effective date from category");
        assertEquals("2024-12-31", ruleGroup.getExpirationDate(), "Should inherit expiration date from category");
    }

    @Test
    @DisplayName("Rule group should override category metadata when specified")
    void testPartialMetadataOverrideForRuleGroup() throws Exception {
        String yaml = """
            metadata:
              name: "Rule Group Metadata Override Test"
              type: "rule-config"
              version: "1.0.0"

            categories:
              - name: "Operations"
                description: "Operational rules"
                priority: 20
                enabled: true
                business-domain: "Operations"
                business-owner: "Operations Team"
                created-by: "Jane Doe"
                effective-date: "2024-01-01"
                expiration-date: "2024-12-31"

            rules:
              - id: "op-rule1"
                name: "Operational rule"
                condition: "status == 'active'"
                message: "Status must be active"
                category: "Operations"

            rule-groups:
              - id: "ops-group"
                name: "Operations Rule Group"
                description: "Group of operational rules"
                category: "Operations"
                operator: "OR"
                business-owner: "Special Ops Team"
                created-by: "Bob Wilson"
                rule-ids:
                  - "op-rule1"
            """;

        YamlRuleConfiguration config = loader.fromYamlString(yaml);
        assertNotNull(config, "Configuration should be loaded");

        // Create rule groups with the factory
        List<RuleGroup> ruleGroups = factory.createRuleGroups(config, factory.createRulesEngineConfiguration(config));
        assertNotNull(ruleGroups, "Rule groups should be created");
        assertEquals(1, ruleGroups.size(), "Should have one rule group");

        RuleGroup ruleGroup = ruleGroups.get(0);
        assertEquals("ops-group", ruleGroup.getId(), "Rule group ID should match");

        // Verify metadata inheritance and override
        assertEquals("Operations", ruleGroup.getBusinessDomain(), "Should inherit business domain from category");
        assertEquals("Special Ops Team", ruleGroup.getBusinessOwner(), "Should override business owner from rule group");
        assertEquals("Bob Wilson", ruleGroup.getCreatedBy(), "Should override created-by from rule group");
        assertEquals("2024-01-01", ruleGroup.getEffectiveDate(), "Should inherit effective date from category");
        assertEquals("2024-12-31", ruleGroup.getExpirationDate(), "Should inherit expiration date from category");
    }

    @Test
    @DisplayName("Rule group should use default category when none specified")
    void testDefaultCategoryForRuleGroup() throws Exception {
        String yaml = """
            metadata:
              name: "Rule Group Default Category Test"
              type: "rule-config"
              version: "1.0.0"

            rules:
              - id: "default-rule1"
                name: "Default rule"
                condition: "value > 100"
                message: "Value must be greater than 100"

            rule-groups:
              - id: "default-group"
                name: "Default Rule Group"
                description: "Group with default category"
                operator: "AND"
                business-owner: "Default Team"
                rule-ids:
                  - "default-rule1"
            """;

        YamlRuleConfiguration config = loader.fromYamlString(yaml);
        assertNotNull(config, "Configuration should be loaded");

        // Create rule groups with the factory
        List<RuleGroup> ruleGroups = factory.createRuleGroups(config, factory.createRulesEngineConfiguration(config));
        assertNotNull(ruleGroups, "Rule groups should be created");
        assertEquals(1, ruleGroups.size(), "Should have one rule group");

        RuleGroup ruleGroup = ruleGroups.get(0);
        assertEquals("default-group", ruleGroup.getId(), "Rule group ID should match");

        // Verify metadata - should only have what's explicitly set
        assertNull(ruleGroup.getBusinessDomain(), "Should not inherit business domain (no category)");
        assertEquals("Default Team", ruleGroup.getBusinessOwner(), "Should have explicitly set business owner");
        assertNull(ruleGroup.getCreatedBy(), "Should not inherit created-by (no category)");
        assertNull(ruleGroup.getEffectiveDate(), "Should not inherit effective date (no category)");
        assertNull(ruleGroup.getExpirationDate(), "Should not inherit expiration date (no category)");
    }
}
