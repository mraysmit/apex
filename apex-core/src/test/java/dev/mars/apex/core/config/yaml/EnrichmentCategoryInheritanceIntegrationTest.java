package dev.mars.apex.core.config.yaml;

import dev.mars.apex.core.engine.model.Enrichment;
import dev.mars.apex.core.engine.model.EnrichmentGroup;
import dev.mars.apex.core.service.enrichment.EnrichmentGroupFactory;
import org.junit.jupiter.api.*;

import java.util.List;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for enrichment category inheritance in realistic scenarios.
 * Tests enterprise-scale enrichment configurations with category-based governance.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-10-23
 * @version 1.0
 */
@DisplayName("Enrichment Category Inheritance Integration Tests")
class EnrichmentCategoryInheritanceIntegrationTest {

    private static final Logger LOGGER = Logger.getLogger(EnrichmentCategoryInheritanceIntegrationTest.class.getName());

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
    @DisplayName("Enterprise enrichment scenario with multiple categories and inheritance patterns")
    void testEnterpriseEnrichmentScenario() throws Exception {
        String yaml = """
            metadata:
              name: "Enterprise Enrichment Configuration"
              type: "rule-config"
              version: "1.0.0"
              description: "Customer data enrichment with category-based governance"

            categories:
              - name: "customer-data"
                description: "Customer data enrichment rules"
                priority: 10
                enabled: true
                business-domain: "Customer Management"
                business-owner: "customer-team@company.com"
                created-by: "data-platform@company.com"
                effective-date: "2024-01-01T00:00:00Z"
                expiration-date: "2024-12-31T23:59:59Z"

              - name: "risk-scoring"
                description: "Risk assessment and scoring enrichments"
                priority: 5
                enabled: true
                business-domain: "Risk Management"
                business-owner: "risk-team@company.com"
                created-by: "risk-platform@company.com"
                effective-date: "2024-01-01T00:00:00Z"
                expiration-date: "2024-12-31T23:59:59Z"

              - name: "compliance-checks"
                description: "Regulatory compliance enrichments"
                priority: 1
                enabled: true
                business-domain: "Compliance"
                business-owner: "compliance-team@company.com"
                created-by: "compliance-platform@company.com"
                effective-date: "2024-01-01T00:00:00Z"
                expiration-date: "2024-12-31T23:59:59Z"

            enrichments:
              # Enrichment that inherits ALL metadata from category
              - id: "customer-profile-enrichment"
                name: "Customer Profile Data Enrichment"
                category: "customer-data"
                type: "lookup-enrichment"
                description: "Enriches customer data with profile information"
                enabled: true
                priority: 10
                lookup-config:
                  lookup-key: "#customerId"
                  lookup-dataset:
                    type: inline
                    key-field: "customerId"
                    data:
                      - customerId: "CUST001"
                        customerName: "Test Customer"
                field-mappings:
                  - source-field: "customerName"
                    target-field: "enrichedCustomerName"
                # No business metadata specified - should inherit everything from category

              # Enrichment that overrides some category metadata
              - id: "risk-score-calculation"
                name: "Risk Score Calculation"
                category: "risk-scoring"
                type: "calculation-enrichment"
                description: "Calculates customer risk score"
                enabled: true
                priority: 5
                created-by: "risk-engine@company.com"  # Override category's created-by
                calculation-config:
                  expression: "#riskFactor * 100"
                  result-field: "riskScore"
                field-mappings:
                  - source-field: "riskScore"
                    target-field: "enrichedRiskScore"
                # Other metadata should inherit from category

              # Enrichment with complete metadata override
              - id: "kyc-verification"
                name: "KYC Verification Enrichment"
                category: "compliance-checks"
                type: "field-enrichment"
                description: "Verifies KYC compliance status"
                enabled: true
                priority: 1
                business-domain: "Regulatory Affairs"
                business-owner: "kyc-team@company.com"
                created-by: "kyc-system@company.com"
                effective-date: "2024-02-01"
                expiration-date: "2024-11-30"
                field-mappings:
                  - source-field: "kycStatus"
                    target-field: "enrichedKycStatus"

              # Enrichment with default category (no category specified)
              - id: "data-quality-check"
                name: "Data Quality Validation"
                type: "field-enrichment"
                description: "Validates data quality metrics"
                enabled: true
                priority: 50
                field-mappings:
                  - source-field: "dataQuality"
                    target-field: "enrichedDataQuality"

            enrichment-groups:
              # Group that inherits metadata from category
              - id: "customer-onboarding-enrichments"
                name: "Customer Onboarding Enrichment Group"
                description: "Enrichments for new customer onboarding"
                category: "customer-data"
                operator: "AND"
                enrichment-ids:
                  - "customer-profile-enrichment"
                  - "data-quality-check"

              # Group that overrides some metadata
              - id: "risk-assessment-enrichments"
                name: "Risk Assessment Enrichment Group"
                description: "Enrichments for risk assessment workflow"
                category: "risk-scoring"
                operator: "AND"
                business-owner: "senior-risk-analyst@company.com"  # Override
                created-by: "risk-workflow@company.com"           # Override
                enrichment-ids:
                  - "risk-score-calculation"

              # Group that combines enrichments from different categories
              - id: "comprehensive-customer-enrichments"
                name: "Comprehensive Customer Enrichment Group"
                description: "Complete customer enrichment workflow"
                category: "customer-data"  # Primary category for inheritance
                operator: "AND"
                business-owner: "data-governance@company.com"  # Override
                enrichment-ids:
                  - "customer-profile-enrichment"
                  - "risk-score-calculation"
                  - "kyc-verification"
            """;

        YamlRuleConfiguration config = loader.fromYamlString(yaml);
        
        // Test enrichment creation with metadata inheritance
        List<Enrichment> enrichments = factory.createEnrichments(config);
        assertNotNull(enrichments, "Enrichments should be created");
        assertEquals(4, enrichments.size(), "Should have 4 enrichments");

        // Test enrichment group creation with metadata inheritance
        List<EnrichmentGroup> groups = EnrichmentGroupFactory.buildEnrichmentGroups(config);
        assertNotNull(groups, "Enrichment groups should be created");
        assertEquals(3, groups.size(), "Should have 3 enrichment groups");

        // Verify enrichment inheritance patterns
        testCustomerProfileEnrichmentInheritance(enrichments);
        testRiskScoreCalculationPartialOverride(enrichments);
        testKycVerificationCompleteOverride(enrichments);
        testDataQualityDefaultCategory(enrichments);

        // Verify enrichment group inheritance patterns
        testCustomerOnboardingGroupInheritance(groups);
        testRiskAssessmentGroupPartialOverride(groups);
        testComprehensiveGroupInheritance(groups);

        LOGGER.info("Enterprise enrichment scenario test completed successfully");
    }

    private void testCustomerProfileEnrichmentInheritance(List<Enrichment> enrichments) {
        LOGGER.info("--- Testing Customer Profile Enrichment Inheritance ---");

        Enrichment enrichment = findEnrichmentById(enrichments, "customer-profile-enrichment");
        assertNotNull(enrichment, "Should find customer profile enrichment");

        // Verify category assignment
        assertTrue(enrichment.getCategories().stream().anyMatch(c -> "customer-data".equals(c.getName())), 
                  "Enrichment should be in customer-data category");

        // Verify metadata inheritance
        assertEquals("Customer Management", enrichment.getBusinessDomain(), "Should inherit business domain");
        assertEquals("customer-team@company.com", enrichment.getBusinessOwner(), "Should inherit business owner");
        assertEquals("data-platform@company.com", enrichment.getCreatedBy(), "Should inherit created by");
        assertEquals("2024-01-01T00:00:00Z", enrichment.getEffectiveDate(), "Should inherit effective date");
        assertEquals("2024-12-31T23:59:59Z", enrichment.getExpirationDate(), "Should inherit expiration date");

        LOGGER.info("Customer profile enrichment inheritance verified");
    }

    private void testRiskScoreCalculationPartialOverride(List<Enrichment> enrichments) {
        LOGGER.info("--- Testing Risk Score Calculation Partial Override ---");

        Enrichment enrichment = findEnrichmentById(enrichments, "risk-score-calculation");
        assertNotNull(enrichment, "Should find risk score calculation enrichment");

        // Verify category assignment
        assertTrue(enrichment.getCategories().stream().anyMatch(c -> "risk-scoring".equals(c.getName())), 
                  "Enrichment should be in risk-scoring category");

        // Verify metadata inheritance and override
        assertEquals("Risk Management", enrichment.getBusinessDomain(), "Should inherit business domain");
        assertEquals("risk-team@company.com", enrichment.getBusinessOwner(), "Should inherit business owner");
        assertEquals("risk-engine@company.com", enrichment.getCreatedBy(), "Should override created by");
        assertEquals("2024-01-01T00:00:00Z", enrichment.getEffectiveDate(), "Should inherit effective date");
        assertEquals("2024-12-31T23:59:59Z", enrichment.getExpirationDate(), "Should inherit expiration date");

        LOGGER.info("Risk score calculation partial override verified");
    }

    private void testKycVerificationCompleteOverride(List<Enrichment> enrichments) {
        LOGGER.info("--- Testing KYC Verification Complete Override ---");

        Enrichment enrichment = findEnrichmentById(enrichments, "kyc-verification");
        assertNotNull(enrichment, "Should find KYC verification enrichment");

        // Verify category assignment
        assertTrue(enrichment.getCategories().stream().anyMatch(c -> "compliance-checks".equals(c.getName())), 
                  "Enrichment should be in compliance-checks category");

        // Verify complete metadata override
        assertEquals("Regulatory Affairs", enrichment.getBusinessDomain(), "Should override business domain");
        assertEquals("kyc-team@company.com", enrichment.getBusinessOwner(), "Should override business owner");
        assertEquals("kyc-system@company.com", enrichment.getCreatedBy(), "Should override created by");
        assertEquals("2024-02-01", enrichment.getEffectiveDate(), "Should override effective date");
        assertEquals("2024-11-30", enrichment.getExpirationDate(), "Should override expiration date");

        LOGGER.info("KYC verification complete override verified");
    }

    private void testDataQualityDefaultCategory(List<Enrichment> enrichments) {
        LOGGER.info("--- Testing Data Quality Default Category ---");

        Enrichment enrichment = findEnrichmentById(enrichments, "data-quality-check");
        assertNotNull(enrichment, "Should find data quality check enrichment");

        // Verify default category assignment
        assertTrue(enrichment.getCategories().stream().anyMatch(c -> "default".equals(c.getName())), 
                  "Enrichment should be in default category");

        LOGGER.info("Data quality default category verified");
    }

    private void testCustomerOnboardingGroupInheritance(List<EnrichmentGroup> groups) {
        LOGGER.info("--- Testing Customer Onboarding Group Inheritance ---");

        EnrichmentGroup group = findEnrichmentGroupById(groups, "customer-onboarding-enrichments");
        assertNotNull(group, "Should find customer onboarding group");

        // Verify metadata inheritance
        assertEquals("Customer Management", group.getBusinessDomain(), "Should inherit business domain");
        assertEquals("customer-team@company.com", group.getBusinessOwner(), "Should inherit business owner");
        assertEquals("data-platform@company.com", group.getCreatedBy(), "Should inherit created by");
        assertEquals("2024-01-01T00:00:00Z", group.getEffectiveDate(), "Should inherit effective date");
        assertEquals("2024-12-31T23:59:59Z", group.getExpirationDate(), "Should inherit expiration date");

        LOGGER.info("Customer onboarding group inheritance verified");
    }

    private void testRiskAssessmentGroupPartialOverride(List<EnrichmentGroup> groups) {
        LOGGER.info("--- Testing Risk Assessment Group Partial Override ---");

        EnrichmentGroup group = findEnrichmentGroupById(groups, "risk-assessment-enrichments");
        assertNotNull(group, "Should find risk assessment group");

        // Verify metadata inheritance and override
        assertEquals("Risk Management", group.getBusinessDomain(), "Should inherit business domain");
        assertEquals("senior-risk-analyst@company.com", group.getBusinessOwner(), "Should override business owner");
        assertEquals("risk-workflow@company.com", group.getCreatedBy(), "Should override created by");
        assertEquals("2024-01-01T00:00:00Z", group.getEffectiveDate(), "Should inherit effective date");
        assertEquals("2024-12-31T23:59:59Z", group.getExpirationDate(), "Should inherit expiration date");

        LOGGER.info("Risk assessment group partial override verified");
    }

    private void testComprehensiveGroupInheritance(List<EnrichmentGroup> groups) {
        LOGGER.info("--- Testing Comprehensive Group Inheritance ---");

        EnrichmentGroup group = findEnrichmentGroupById(groups, "comprehensive-customer-enrichments");
        assertNotNull(group, "Should find comprehensive customer group");

        // Verify metadata inheritance and override
        assertEquals("Customer Management", group.getBusinessDomain(), "Should inherit business domain from primary category");
        assertEquals("data-governance@company.com", group.getBusinessOwner(), "Should override business owner");
        assertEquals("data-platform@company.com", group.getCreatedBy(), "Should inherit created by");
        assertEquals("2024-01-01T00:00:00Z", group.getEffectiveDate(), "Should inherit effective date");
        assertEquals("2024-12-31T23:59:59Z", group.getExpirationDate(), "Should inherit expiration date");

        LOGGER.info("Comprehensive group inheritance verified");
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
}
