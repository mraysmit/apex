package dev.mars.apex.core.config.yaml;

import dev.mars.apex.core.engine.model.Enrichment;
import dev.mars.apex.core.engine.model.EnrichmentGroup;
import dev.mars.apex.core.engine.model.Rule;
import dev.mars.apex.core.engine.model.RuleGroup;
import dev.mars.apex.core.service.enrichment.EnrichmentGroupFactory;
import org.junit.jupiter.api.*;

import java.util.List;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test suite to verify consistency between rule and enrichment category inheritance.
 * Ensures that both rules and enrichments follow the same inheritance patterns.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-10-23
 * @version 1.0
 */
@DisplayName("Enrichment-Rule Category Consistency Tests")
class EnrichmentRuleCategoryConsistencyTest {

    private static final Logger LOGGER = Logger.getLogger(EnrichmentRuleCategoryConsistencyTest.class.getName());

    private YamlConfigurationLoader loader;
    private YamlRuleFactory factory;

    @BeforeEach
    void setUp() {
        loader = new YamlConfigurationLoader();
        factory = new YamlRuleFactory();
    }

    @AfterEach
    void tearDown() {
        factory.clearCache();
    }

    @Test
    @DisplayName("Rules and enrichments inherit consistently from same category")
    void testConsistentInheritanceFromSameCategory() throws Exception {
        String yaml = """
            metadata:
              name: "Consistency Test Configuration"
              type: "rule-config"
              version: "1.0.0"

            categories:
              - name: "shared-category"
                description: "Category shared by rules and enrichments"
                priority: 10
                enabled: true
                business-domain: "Shared Domain"
                business-owner: "shared-owner@company.com"
                created-by: "shared-creator@company.com"
                effective-date: "2024-01-01"
                expiration-date: "2024-12-31"

            rules:
              - id: "test-rule"
                name: "Test Rule"
                category: "shared-category"
                condition: "true"
                message: "Test message"
                enabled: true

            enrichments:
              - id: "test-enrichment"
                name: "Test Enrichment"
                category: "shared-category"
                type: "field-enrichment"
                enabled: true
                field-mappings:
                  - source-field: "testField"
                    target-field: "enrichedTestField"

            rule-groups:
              - id: "test-rule-group"
                name: "Test Rule Group"
                category: "shared-category"
                rule-ids:
                  - "test-rule"

            enrichment-groups:
              - id: "test-enrichment-group"
                name: "Test Enrichment Group"
                category: "shared-category"
                enrichment-ids:
                  - "test-enrichment"
            """;

        YamlRuleConfiguration config = loader.fromYamlString(yaml);
        
        // Create all entities
        List<Rule> rules = factory.createRules(config);
        List<Enrichment> enrichments = factory.createEnrichments(config);
        List<RuleGroup> ruleGroups = factory.createRuleGroups(config, factory.createRulesEngineConfiguration(config));
        List<EnrichmentGroup> enrichmentGroups = EnrichmentGroupFactory.buildEnrichmentGroups(config);

        // Find entities
        Rule rule = findRuleById(rules, "test-rule");
        Enrichment enrichment = findEnrichmentById(enrichments, "test-enrichment");
        RuleGroup ruleGroup = findRuleGroupById(ruleGroups, "test-rule-group");
        EnrichmentGroup enrichmentGroup = findEnrichmentGroupById(enrichmentGroups, "test-enrichment-group");

        // Verify all entities exist
        assertNotNull(rule, "Rule should be created");
        assertNotNull(enrichment, "Enrichment should be created");
        assertNotNull(ruleGroup, "Rule group should be created");
        assertNotNull(enrichmentGroup, "Enrichment group should be created");

        // Verify consistent metadata inheritance
        String expectedDomain = "Shared Domain";
        String expectedOwner = "shared-owner@company.com";
        String expectedCreator = "shared-creator@company.com";
        String expectedEffective = "2024-01-01";
        String expectedExpiration = "2024-12-31";

        // Check rule metadata
        assertEquals(expectedDomain, rule.getMetadata().getBusinessDomain().orElse(null), "Rule business domain should match");
        assertEquals(expectedOwner, rule.getMetadata().getBusinessOwner().orElse(null), "Rule business owner should match");
        assertEquals(expectedCreator, rule.getMetadata().getCreatedByUser(), "Rule created by should match");

        // Check enrichment metadata
        assertEquals(expectedDomain, enrichment.getBusinessDomain(), "Enrichment business domain should match");
        assertEquals(expectedOwner, enrichment.getBusinessOwner(), "Enrichment business owner should match");
        assertEquals(expectedCreator, enrichment.getCreatedBy(), "Enrichment created by should match");
        assertEquals(expectedEffective, enrichment.getEffectiveDate(), "Enrichment effective date should match");
        assertEquals(expectedExpiration, enrichment.getExpirationDate(), "Enrichment expiration date should match");

        // Check rule group metadata
        assertEquals(expectedDomain, ruleGroup.getBusinessDomain(), "Rule group business domain should match");
        assertEquals(expectedOwner, ruleGroup.getBusinessOwner(), "Rule group business owner should match");
        assertEquals(expectedCreator, ruleGroup.getCreatedBy(), "Rule group created by should match");
        assertEquals(expectedEffective, ruleGroup.getEffectiveDate(), "Rule group effective date should match");
        assertEquals(expectedExpiration, ruleGroup.getExpirationDate(), "Rule group expiration date should match");

        // Check enrichment group metadata
        assertEquals(expectedDomain, enrichmentGroup.getBusinessDomain(), "Enrichment group business domain should match");
        assertEquals(expectedOwner, enrichmentGroup.getBusinessOwner(), "Enrichment group business owner should match");
        assertEquals(expectedCreator, enrichmentGroup.getCreatedBy(), "Enrichment group created by should match");
        assertEquals(expectedEffective, enrichmentGroup.getEffectiveDate(), "Enrichment group effective date should match");
        assertEquals(expectedExpiration, enrichmentGroup.getExpirationDate(), "Enrichment group expiration date should match");

        LOGGER.info("Consistent inheritance verified for all entity types");
    }

    @Test
    @DisplayName("Rules and enrichments handle override patterns consistently")
    void testConsistentOverridePatterns() throws Exception {
        String yaml = """
            metadata:
              name: "Override Consistency Test"
              type: "rule-config"
              version: "1.0.0"

            categories:
              - name: "override-category"
                description: "Category for testing overrides"
                priority: 10
                enabled: true
                business-domain: "Original Domain"
                business-owner: "original-owner@company.com"
                created-by: "original-creator@company.com"
                effective-date: "2024-01-01"
                expiration-date: "2024-12-31"

            rules:
              - id: "override-rule"
                name: "Override Rule"
                category: "override-category"
                condition: "true"
                message: "Test message"
                enabled: true
                business-owner: "rule-override-owner@company.com"
                created-by: "rule-override-creator@company.com"

            enrichments:
              - id: "override-enrichment"
                name: "Override Enrichment"
                category: "override-category"
                type: "field-enrichment"
                enabled: true
                business-owner: "enrichment-override-owner@company.com"
                created-by: "enrichment-override-creator@company.com"
                field-mappings:
                  - source-field: "overrideField"
                    target-field: "enrichedOverrideField"

            rule-groups:
              - id: "override-rule-group"
                name: "Override Rule Group"
                category: "override-category"
                business-owner: "rule-group-override-owner@company.com"
                created-by: "rule-group-override-creator@company.com"
                rule-ids:
                  - "override-rule"

            enrichment-groups:
              - id: "override-enrichment-group"
                name: "Override Enrichment Group"
                category: "override-category"
                business-owner: "enrichment-group-override-owner@company.com"
                created-by: "enrichment-group-override-creator@company.com"
                enrichment-ids:
                  - "override-enrichment"
            """;

        YamlRuleConfiguration config = loader.fromYamlString(yaml);
        
        // Create all entities
        List<Rule> rules = factory.createRules(config);
        List<Enrichment> enrichments = factory.createEnrichments(config);
        List<RuleGroup> ruleGroups = factory.createRuleGroups(config, factory.createRulesEngineConfiguration(config));
        List<EnrichmentGroup> enrichmentGroups = EnrichmentGroupFactory.buildEnrichmentGroups(config);

        // Find entities
        Rule rule = findRuleById(rules, "override-rule");
        Enrichment enrichment = findEnrichmentById(enrichments, "override-enrichment");
        RuleGroup ruleGroup = findRuleGroupById(ruleGroups, "override-rule-group");
        EnrichmentGroup enrichmentGroup = findEnrichmentGroupById(enrichmentGroups, "override-enrichment-group");

        // Verify all entities exist
        assertNotNull(rule, "Rule should be created");
        assertNotNull(enrichment, "Enrichment should be created");
        assertNotNull(ruleGroup, "Rule group should be created");
        assertNotNull(enrichmentGroup, "Enrichment group should be created");

        // Verify override behavior is consistent
        // All should inherit business-domain but override business-owner and created-by
        String expectedDomain = "Original Domain";  // Inherited
        String expectedEffective = "2024-01-01";    // Inherited
        String expectedExpiration = "2024-12-31";   // Inherited

        // Check rule overrides
        assertEquals(expectedDomain, rule.getMetadata().getBusinessDomain().orElse(null), "Rule should inherit domain");
        assertEquals("rule-override-owner@company.com", rule.getMetadata().getBusinessOwner().orElse(null), "Rule should override owner");
        assertEquals("rule-override-creator@company.com", rule.getMetadata().getCreatedByUser(), "Rule should override creator");

        // Check enrichment overrides
        assertEquals(expectedDomain, enrichment.getBusinessDomain(), "Enrichment should inherit domain");
        assertEquals("enrichment-override-owner@company.com", enrichment.getBusinessOwner(), "Enrichment should override owner");
        assertEquals("enrichment-override-creator@company.com", enrichment.getCreatedBy(), "Enrichment should override creator");
        assertEquals(expectedEffective, enrichment.getEffectiveDate(), "Enrichment should inherit effective date");
        assertEquals(expectedExpiration, enrichment.getExpirationDate(), "Enrichment should inherit expiration date");

        // Check rule group overrides
        assertEquals(expectedDomain, ruleGroup.getBusinessDomain(), "Rule group should inherit domain");
        assertEquals("rule-group-override-owner@company.com", ruleGroup.getBusinessOwner(), "Rule group should override owner");
        assertEquals("rule-group-override-creator@company.com", ruleGroup.getCreatedBy(), "Rule group should override creator");
        assertEquals(expectedEffective, ruleGroup.getEffectiveDate(), "Rule group should inherit effective date");
        assertEquals(expectedExpiration, ruleGroup.getExpirationDate(), "Rule group should inherit expiration date");

        // Check enrichment group overrides
        assertEquals(expectedDomain, enrichmentGroup.getBusinessDomain(), "Enrichment group should inherit domain");
        assertEquals("enrichment-group-override-owner@company.com", enrichmentGroup.getBusinessOwner(), "Enrichment group should override owner");
        assertEquals("enrichment-group-override-creator@company.com", enrichmentGroup.getCreatedBy(), "Enrichment group should override creator");
        assertEquals(expectedEffective, enrichmentGroup.getEffectiveDate(), "Enrichment group should inherit effective date");
        assertEquals(expectedExpiration, enrichmentGroup.getExpirationDate(), "Enrichment group should inherit expiration date");

        LOGGER.info("Consistent override patterns verified for all entity types");
    }

    @Test
    @DisplayName("Default category behavior is consistent across rules and enrichments")
    void testConsistentDefaultCategoryBehavior() throws Exception {
        String yaml = """
            metadata:
              name: "Default Category Test"
              type: "rule-config"
              version: "1.0.0"

            rules:
              - id: "default-rule"
                name: "Default Category Rule"
                condition: "true"
                message: "Test message"
                enabled: true

            enrichments:
              - id: "default-enrichment"
                name: "Default Category Enrichment"
                type: "field-enrichment"
                enabled: true
                field-mappings:
                  - source-field: "defaultField"
                    target-field: "enrichedDefaultField"
            """;

        YamlRuleConfiguration config = loader.fromYamlString(yaml);
        
        // Create entities
        List<Rule> rules = factory.createRules(config);
        List<Enrichment> enrichments = factory.createEnrichments(config);

        // Find entities
        Rule rule = findRuleById(rules, "default-rule");
        Enrichment enrichment = findEnrichmentById(enrichments, "default-enrichment");

        // Verify both use default category
        assertNotNull(rule, "Rule should be created");
        assertNotNull(enrichment, "Enrichment should be created");

        assertTrue(rule.getCategories().stream().anyMatch(c -> "default".equals(c.getName())), 
                  "Rule should have default category");
        assertTrue(enrichment.getCategories().stream().anyMatch(c -> "default".equals(c.getName())), 
                  "Enrichment should have default category");

        LOGGER.info("Consistent default category behavior verified");
    }

    // Helper methods
    private Rule findRuleById(List<Rule> rules, String id) {
        return rules.stream()
                .filter(r -> id.equals(r.getId()))
                .findFirst()
                .orElse(null);
    }

    private Enrichment findEnrichmentById(List<Enrichment> enrichments, String id) {
        return enrichments.stream()
                .filter(e -> id.equals(e.getId()))
                .findFirst()
                .orElse(null);
    }

    private RuleGroup findRuleGroupById(List<RuleGroup> groups, String id) {
        return groups.stream()
                .filter(g -> id.equals(g.getId()))
                .findFirst()
                .orElse(null);
    }

    private EnrichmentGroup findEnrichmentGroupById(List<EnrichmentGroup> groups, String id) {
        return groups.stream()
                .filter(g -> id.equals(g.getId()))
                .findFirst()
                .orElse(null);
    }
}
