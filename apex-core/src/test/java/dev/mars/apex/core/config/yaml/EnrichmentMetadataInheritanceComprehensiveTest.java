package dev.mars.apex.core.config.yaml;

import dev.mars.apex.core.engine.model.Enrichment;
import dev.mars.apex.core.engine.model.EnrichmentGroup;
import dev.mars.apex.core.service.enrichment.EnrichmentGroupFactory;
import org.junit.jupiter.api.*;

import java.util.List;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive test suite for enrichment and enrichment group metadata inheritance.
 * Tests the same inheritance patterns as rules but for enrichments.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-10-23
 * @version 1.0
 */
@DisplayName("Enrichment Metadata Inheritance Comprehensive Tests")
class EnrichmentMetadataInheritanceComprehensiveTest {

    private static final Logger LOGGER = Logger.getLogger(EnrichmentMetadataInheritanceComprehensiveTest.class.getName());

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

    private String createBaseYaml() {
        return """
            metadata:
              name: "Enrichment Inheritance Test"
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
                
              - name: "category2"
                description: "Test category 2"
                priority: 20
                enabled: true
                business-domain: "Domain2"
                business-owner: "Owner2"
                created-by: "Creator2"
                effective-date: "2024-02-01"
                expiration-date: "2024-11-30"
            """;
    }

    @Nested
    @DisplayName("Enrichment Inheritance Tests")
    class EnrichmentInheritanceTests {

        @Test
        @DisplayName("Enrichment inherits all metadata when none specified")
        void testEnrichmentCompleteInheritance() throws Exception {
            String yaml = createBaseYaml() + """
                enrichments:
                  - id: "enrichment1"
                    name: "Complete inheritance enrichment"
                    category: "category1"
                    type: "field-enrichment"
                    enabled: true
                    field-mappings:
                      - source-field: "testField"
                        target-field: "enrichedField"
                """;

            List<Enrichment> enrichments = factory.createEnrichments(loader.fromYamlString(yaml));
            Enrichment enrichment = findEnrichmentById(enrichments, "enrichment1");

            assertEnrichmentMetadata(enrichment, "Domain1", "Owner1", "Creator1", "2024-01-01", "2024-12-31");
        }

        @Test
        @DisplayName("Enrichment overrides specific metadata fields")
        void testEnrichmentPartialOverride() throws Exception {
            String yaml = createBaseYaml() + """
                enrichments:
                  - id: "enrichment2"
                    name: "Partial override enrichment"
                    category: "category1"
                    type: "lookup-enrichment"
                    enabled: true
                    business-owner: "EnrichmentOwner"
                    created-by: "EnrichmentCreator"
                    lookup-config:
                      lookup-key: "#testKey"
                      lookup-dataset:
                        type: inline
                        key-field: "key"
                        data:
                          - key: "test"
                            value: "testValue"
                    field-mappings:
                      - source-field: "value"
                        target-field: "enrichedValue"
                """;

            List<Enrichment> enrichments = factory.createEnrichments(loader.fromYamlString(yaml));
            Enrichment enrichment = findEnrichmentById(enrichments, "enrichment2");

            assertEnrichmentMetadata(enrichment, "Domain1", "EnrichmentOwner", "EnrichmentCreator", "2024-01-01", "2024-12-31");
        }

        @Test
        @DisplayName("Enrichment overrides all metadata fields")
        void testEnrichmentCompleteOverride() throws Exception {
            String yaml = createBaseYaml() + """
                enrichments:
                  - id: "enrichment3"
                    name: "Complete override enrichment"
                    category: "category1"
                    type: "calculation-enrichment"
                    enabled: true
                    business-domain: "OverrideDomain"
                    business-owner: "OverrideOwner"
                    created-by: "OverrideCreator"
                    effective-date: "2025-01-01"
                    expiration-date: "2025-12-31"
                    calculation-config:
                      expression: "#testValue * 2"
                      result-field: "calculatedResult"
                    field-mappings:
                      - source-field: "calculatedResult"
                        target-field: "enrichedCalculation"
                """;

            List<Enrichment> enrichments = factory.createEnrichments(loader.fromYamlString(yaml));
            Enrichment enrichment = findEnrichmentById(enrichments, "enrichment3");

            assertEnrichmentMetadata(enrichment, "OverrideDomain", "OverrideOwner", "OverrideCreator", "2025-01-01", "2025-12-31");
        }

        @Test
        @DisplayName("Enrichment with default category when none specified")
        void testEnrichmentDefaultCategory() throws Exception {
            String yaml = createBaseYaml() + """
                enrichments:
                  - id: "enrichment4"
                    name: "Default category enrichment"
                    type: "field-enrichment"
                    enabled: true
                    field-mappings:
                      - source-field: "defaultField"
                        target-field: "enrichedDefault"
                """;

            List<Enrichment> enrichments = factory.createEnrichments(loader.fromYamlString(yaml));
            Enrichment enrichment = findEnrichmentById(enrichments, "enrichment4");

            assertNotNull(enrichment, "Enrichment should be created");
            assertTrue(enrichment.getCategories().stream().anyMatch(c -> "default".equals(c.getName())),
                      "Enrichment should have default category");
        }

        @Test
        @DisplayName("Enrichment inherits from different categories")
        void testEnrichmentDifferentCategories() throws Exception {
            String yaml = createBaseYaml() + """
                enrichments:
                  - id: "enrichment5"
                    name: "Category1 enrichment"
                    category: "category1"
                    type: "field-enrichment"
                    enabled: true
                    field-mappings:
                      - source-field: "field1"
                        target-field: "enriched1"

                  - id: "enrichment6"
                    name: "Category2 enrichment"
                    category: "category2"
                    type: "lookup-enrichment"
                    enabled: true
                    lookup-config:
                      lookup-key: "#key2"
                      lookup-dataset:
                        type: inline
                        key-field: "key"
                        data:
                          - key: "test2"
                            value: "testValue2"
                    field-mappings:
                      - source-field: "value"
                        target-field: "enriched2"
                """;

            List<Enrichment> enrichments = factory.createEnrichments(loader.fromYamlString(yaml));

            Enrichment enrichment1 = findEnrichmentById(enrichments, "enrichment5");
            Enrichment enrichment2 = findEnrichmentById(enrichments, "enrichment6");

            assertEnrichmentMetadata(enrichment1, "Domain1", "Owner1", "Creator1", "2024-01-01", "2024-12-31");
            assertEnrichmentMetadata(enrichment2, "Domain2", "Owner2", "Creator2", "2024-02-01", "2024-11-30");
        }

        @Test
        @DisplayName("Enrichment with no category has no inherited metadata")
        void testEnrichmentNoCategory() throws Exception {
            String yaml = createBaseYaml() + """
                enrichments:
                  - id: "enrichment7"
                    name: "No category enrichment"
                    type: "field-enrichment"
                    enabled: true
                    field-mappings:
                      - source-field: "noCategory"
                        target-field: "enrichedNoCategory"
                """;

            List<Enrichment> enrichments = factory.createEnrichments(loader.fromYamlString(yaml));
            Enrichment enrichment = findEnrichmentById(enrichments, "enrichment7");

            assertEnrichmentMetadata(enrichment, null, null, null, null, null);
        }

        @Test
        @DisplayName("Enrichment with non-existent category has no inherited metadata")
        void testEnrichmentNonExistentCategory() throws Exception {
            String yaml = createBaseYaml() + """
                enrichments:
                  - id: "enrichment8"
                    name: "Non-existent category enrichment"
                    category: "non-existent"
                    type: "field-enrichment"
                    enabled: true
                    field-mappings:
                      - source-field: "nonExistent"
                        target-field: "enrichedNonExistent"
                """;

            List<Enrichment> enrichments = factory.createEnrichments(loader.fromYamlString(yaml));
            Enrichment enrichment = findEnrichmentById(enrichments, "enrichment8");

            assertEnrichmentMetadata(enrichment, null, null, null, null, null);
        }
    }

    @Nested
    @DisplayName("Enrichment Group Inheritance Tests")
    class EnrichmentGroupInheritanceTests {

        @Test
        @DisplayName("Enrichment group inherits all metadata when none specified")
        void testEnrichmentGroupCompleteInheritance() throws Exception {
            String yaml = createBaseYaml() + """
                enrichments:
                  - id: "enrichment1"
                    name: "Test enrichment"
                    type: "field-enrichment"
                    enabled: true
                    field-mappings:
                      - source-field: "testField"
                        target-field: "enrichedField"

                enrichment-groups:
                  - id: "group1"
                    name: "Complete inheritance group"
                    category: "category1"
                    enrichment-ids:
                      - "enrichment1"
                """;

            YamlRuleConfiguration config = loader.fromYamlString(yaml);
            List<EnrichmentGroup> groups = EnrichmentGroupFactory.buildEnrichmentGroups(config);
            EnrichmentGroup group = findEnrichmentGroupById(groups, "group1");

            assertEnrichmentGroupMetadata(group, "Domain1", "Owner1", "Creator1", "2024-01-01", "2024-12-31");
        }

        @Test
        @DisplayName("Enrichment group overrides specific metadata fields")
        void testEnrichmentGroupPartialOverride() throws Exception {
            String yaml = createBaseYaml() + """
                enrichments:
                  - id: "enrichment1"
                    name: "Test enrichment"
                    type: "field-enrichment"
                    enabled: true
                    field-mappings:
                      - source-field: "testField"
                        target-field: "enrichedField"

                enrichment-groups:
                  - id: "group2"
                    name: "Partial override group"
                    category: "category1"
                    business-owner: "GroupOwner"
                    created-by: "GroupCreator"
                    enrichment-ids:
                      - "enrichment1"
                """;

            YamlRuleConfiguration config = loader.fromYamlString(yaml);
            List<EnrichmentGroup> groups = EnrichmentGroupFactory.buildEnrichmentGroups(config);
            EnrichmentGroup group = findEnrichmentGroupById(groups, "group2");

            assertEnrichmentGroupMetadata(group, "Domain1", "GroupOwner", "GroupCreator", "2024-01-01", "2024-12-31");
        }

        @Test
        @DisplayName("Enrichment group overrides all metadata fields")
        void testEnrichmentGroupCompleteOverride() throws Exception {
            String yaml = createBaseYaml() + """
                enrichments:
                  - id: "enrichment1"
                    name: "Test enrichment"
                    type: "field-enrichment"
                    enabled: true
                    field-mappings:
                      - source-field: "testField"
                        target-field: "enrichedField"

                enrichment-groups:
                  - id: "group3"
                    name: "Complete override group"
                    category: "category1"
                    business-domain: "OverrideDomain"
                    business-owner: "OverrideOwner"
                    created-by: "OverrideCreator"
                    effective-date: "2025-01-01"
                    expiration-date: "2025-12-31"
                    enrichment-ids:
                      - "enrichment1"
                """;

            YamlRuleConfiguration config = loader.fromYamlString(yaml);
            List<EnrichmentGroup> groups = EnrichmentGroupFactory.buildEnrichmentGroups(config);
            EnrichmentGroup group = findEnrichmentGroupById(groups, "group3");

            assertEnrichmentGroupMetadata(group, "OverrideDomain", "OverrideOwner", "OverrideCreator", "2025-01-01", "2025-12-31");
        }

        @Test
        @DisplayName("Enrichment group with no category has no inherited metadata")
        void testEnrichmentGroupNoCategory() throws Exception {
            String yaml = createBaseYaml() + """
                enrichments:
                  - id: "enrichment1"
                    name: "Test enrichment"
                    type: "field-enrichment"
                    enabled: true
                    field-mappings:
                      - source-field: "testField"
                        target-field: "enrichedField"

                enrichment-groups:
                  - id: "group4"
                    name: "No category group"
                    enrichment-ids:
                      - "enrichment1"
                """;

            YamlRuleConfiguration config = loader.fromYamlString(yaml);
            List<EnrichmentGroup> groups = EnrichmentGroupFactory.buildEnrichmentGroups(config);
            EnrichmentGroup group = findEnrichmentGroupById(groups, "group4");

            assertEnrichmentGroupMetadata(group, null, null, null, null, null);
        }

        @Test
        @DisplayName("Enrichment group with non-existent category has no inherited metadata")
        void testEnrichmentGroupNonExistentCategory() throws Exception {
            String yaml = createBaseYaml() + """
                enrichments:
                  - id: "enrichment1"
                    name: "Test enrichment"
                    type: "field-enrichment"
                    enabled: true
                    field-mappings:
                      - source-field: "testField"
                        target-field: "enrichedField"

                enrichment-groups:
                  - id: "group5"
                    name: "Non-existent category group"
                    category: "non-existent"
                    enrichment-ids:
                      - "enrichment1"
                """;

            YamlRuleConfiguration config = loader.fromYamlString(yaml);
            List<EnrichmentGroup> groups = EnrichmentGroupFactory.buildEnrichmentGroups(config);
            EnrichmentGroup group = findEnrichmentGroupById(groups, "group5");

            assertEnrichmentGroupMetadata(group, null, null, null, null, null);
        }
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

                enrichments:
                  - id: "enrichment1"
                    name: "Enrichment in category 1"
                    category: "category1"
                    type: "field-enrichment"
                    enabled: true
                    field-mappings:
                      - source-field: "field1"
                        target-field: "enriched1"

                  - id: "enrichment2"
                    name: "Enrichment in category 2"
                    category: "category2"
                    type: "field-enrichment"
                    enabled: true
                    field-mappings:
                      - source-field: "field2"
                        target-field: "enriched2"

                enrichment-groups:
                  - id: "group1"
                    name: "Group in category 1"
                    category: "category1"
                    enrichment-ids: ["enrichment1"]

                  - id: "group2"
                    name: "Group in category 2"
                    category: "category2"
                    enrichment-ids: ["enrichment2"]
                """;

            YamlRuleConfiguration config = loader.fromYamlString(yaml);
            List<Enrichment> enrichments = factory.createEnrichments(config);
            List<EnrichmentGroup> groups = EnrichmentGroupFactory.buildEnrichmentGroups(config);

            // Verify enrichments inherit from correct categories
            Enrichment enrichment1 = findEnrichmentById(enrichments, "enrichment1");
            Enrichment enrichment2 = findEnrichmentById(enrichments, "enrichment2");
            assertEnrichmentMetadata(enrichment1, "Domain1", "Owner1", "Creator1", "2024-01-01", "2024-12-31");
            assertEnrichmentMetadata(enrichment2, "Domain2", "Owner2", "Creator2", "2025-01-01", "2025-12-31");

            // Verify enrichment groups inherit from correct categories
            EnrichmentGroup group1 = findEnrichmentGroupById(groups, "group1");
            EnrichmentGroup group2 = findEnrichmentGroupById(groups, "group2");
            assertEnrichmentGroupMetadata(group1, "Domain1", "Owner1", "Creator1", "2024-01-01", "2024-12-31");
            assertEnrichmentGroupMetadata(group2, "Domain2", "Owner2", "Creator2", "2025-01-01", "2025-12-31");
        }
    }

    // Helper methods
    private Enrichment findEnrichmentById(List<Enrichment> enrichments, String id) {
        return enrichments.stream()
                .filter(e -> id.equals(e.getId()))
                .findFirst()
                .orElse(null);
    }

    private EnrichmentGroup findEnrichmentGroupById(List<EnrichmentGroup> groups, String id) {
        return groups.stream()
                .filter(g -> id.equals(g.getId()))
                .findFirst()
                .orElse(null);
    }

    private void assertEnrichmentMetadata(Enrichment enrichment, String expectedDomain, String expectedOwner, 
                                        String expectedCreator, String expectedEffective, String expectedExpiration) {
        assertNotNull(enrichment, "Enrichment should not be null");
        assertEquals(expectedDomain, enrichment.getBusinessDomain(), "Business domain should match");
        assertEquals(expectedOwner, enrichment.getBusinessOwner(), "Business owner should match");
        assertEquals(expectedCreator, enrichment.getCreatedBy(), "Created by should match");
        assertEquals(expectedEffective, enrichment.getEffectiveDate(), "Effective date should match");
        assertEquals(expectedExpiration, enrichment.getExpirationDate(), "Expiration date should match");
    }

    private void assertEnrichmentGroupMetadata(EnrichmentGroup group, String expectedDomain, String expectedOwner, 
                                             String expectedCreator, String expectedEffective, String expectedExpiration) {
        assertNotNull(group, "Enrichment group should not be null");
        assertEquals(expectedDomain, group.getBusinessDomain(), "Business domain should match");
        assertEquals(expectedOwner, group.getBusinessOwner(), "Business owner should match");
        assertEquals(expectedCreator, group.getCreatedBy(), "Created by should match");
        assertEquals(expectedEffective, group.getEffectiveDate(), "Effective date should match");
        assertEquals(expectedExpiration, group.getExpirationDate(), "Expiration date should match");
    }
}
