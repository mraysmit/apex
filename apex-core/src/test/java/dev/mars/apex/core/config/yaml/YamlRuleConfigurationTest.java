package dev.mars.apex.core.config.yaml;

import org.junit.jupiter.api.*;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * API-specific integration tests for YamlRuleConfiguration.
 * 
 * Tests focus on:
 * - Actual API methods and properties (getMetadata, getRules, getRuleGroups, etc.)
 * - Integration with ConfigurationMetadata inner class
 * - Collection management for rules, groups, categories, data sources
 * - Configuration structure validation and integrity
 * 
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 1.0.0
 */
class YamlRuleConfigurationTest {

    private YamlRuleConfiguration configuration;

    @BeforeEach
    void setUp() {
        configuration = new YamlRuleConfiguration();
    }

    // ========================================
    // Constructor and Basic Properties Tests
    // ========================================

    @Test
    @DisplayName("Should create YamlRuleConfiguration with null values initially")
    void testDefaultConstructor() {
        YamlRuleConfiguration config = new YamlRuleConfiguration();
        
        assertNotNull(config, "Configuration should be created");
        assertNull(config.getMetadata(), "Metadata should be null initially");
        assertNull(config.getRules(), "Rules should be null initially");
        assertNull(config.getRuleGroups(), "Rule groups should be null initially");
        assertNull(config.getCategories(), "Categories should be null initially");
        assertNull(config.getDataSources(), "Data sources should be null initially");
        assertNull(config.getEnrichments(), "Enrichments should be null initially");
        assertNull(config.getTransformations(), "Transformations should be null initially");
        assertNull(config.getRuleChains(), "Rule chains should be null initially");
    }

    // ========================================
    // ConfigurationMetadata Tests
    // ========================================

    @Test
    @DisplayName("Should handle ConfigurationMetadata operations correctly")
    void testConfigurationMetadataOperations() {
        YamlRuleConfiguration.ConfigurationMetadata metadata = new YamlRuleConfiguration.ConfigurationMetadata();
        metadata.setName("Test Configuration");
        metadata.setVersion("1.0.0");
        metadata.setDescription("Test configuration for unit tests");
        metadata.setAuthor("Test Suite");
        metadata.setCreated("2024-01-01T00:00:00Z");
        metadata.setLastModified("2024-01-02T00:00:00Z");
        metadata.setTags(Arrays.asList("test", "configuration", "unit-test"));

        configuration.setMetadata(metadata);

        assertNotNull(configuration.getMetadata(), "Metadata should not be null");
        assertEquals("Test Configuration", configuration.getMetadata().getName(), "Name should match");
        assertEquals("1.0.0", configuration.getMetadata().getVersion(), "Version should match");
        assertEquals("Test configuration for unit tests", configuration.getMetadata().getDescription(), "Description should match");
        assertEquals("Test Suite", configuration.getMetadata().getAuthor(), "Author should match");
        assertEquals("2024-01-01T00:00:00Z", configuration.getMetadata().getCreated(), "Created date should match");
        assertEquals("2024-01-02T00:00:00Z", configuration.getMetadata().getLastModified(), "Last modified date should match");
        
        List<String> tags = configuration.getMetadata().getTags();
        assertNotNull(tags, "Tags should not be null");
        assertEquals(3, tags.size(), "Should have 3 tags");
        assertTrue(tags.contains("test"), "Should contain 'test' tag");
        assertTrue(tags.contains("configuration"), "Should contain 'configuration' tag");
        assertTrue(tags.contains("unit-test"), "Should contain 'unit-test' tag");
    }

    @Test
    @DisplayName("Should handle null metadata gracefully")
    void testNullMetadata() {
        configuration.setMetadata(null);
        assertNull(configuration.getMetadata(), "Metadata should be null when set to null");
    }

    @Test
    @DisplayName("Should handle empty metadata")
    void testEmptyMetadata() {
        YamlRuleConfiguration.ConfigurationMetadata emptyMetadata = new YamlRuleConfiguration.ConfigurationMetadata();
        configuration.setMetadata(emptyMetadata);
        
        assertNotNull(configuration.getMetadata(), "Metadata should not be null");
        assertNull(configuration.getMetadata().getName(), "Name should be null");
        assertNull(configuration.getMetadata().getVersion(), "Version should be null");
        assertNull(configuration.getMetadata().getDescription(), "Description should be null");
    }

    // ========================================
    // Rules Management Tests
    // ========================================

    @Test
    @DisplayName("Should manage rules list correctly")
    void testRulesManagement() {
        List<YamlRule> rules = new ArrayList<>();
        
        // Create test rules
        YamlRule rule1 = createTestRule("rule-1", "Test Rule 1", "true", "Test message 1");
        YamlRule rule2 = createTestRule("rule-2", "Test Rule 2", "#amount > 1000", "High amount detected");
        YamlRule rule3 = createTestRule("rule-3", "Test Rule 3", "#customerTier == 'PREMIUM'", "Premium customer");
        
        rules.add(rule1);
        rules.add(rule2);
        rules.add(rule3);

        configuration.setRules(rules);

        assertNotNull(configuration.getRules(), "Rules should not be null");
        assertEquals(3, configuration.getRules().size(), "Should have 3 rules");
        assertEquals("rule-1", configuration.getRules().get(0).getId(), "First rule ID should match");
        assertEquals("rule-2", configuration.getRules().get(1).getId(), "Second rule ID should match");
        assertEquals("rule-3", configuration.getRules().get(2).getId(), "Third rule ID should match");
    }

    @Test
    @DisplayName("Should handle null rules list")
    void testNullRulesList() {
        configuration.setRules(null);
        assertNull(configuration.getRules(), "Rules should be null when set to null");
    }

    @Test
    @DisplayName("Should handle empty rules list")
    void testEmptyRulesList() {
        List<YamlRule> emptyRules = new ArrayList<>();
        configuration.setRules(emptyRules);
        
        assertNotNull(configuration.getRules(), "Rules should not be null");
        assertTrue(configuration.getRules().isEmpty(), "Rules should be empty");
    }

    // ========================================
    // Rule Groups Management Tests
    // ========================================

    @Test
    @DisplayName("Should manage rule groups correctly")
    void testRuleGroupsManagement() {
        List<YamlRuleGroup> ruleGroups = new ArrayList<>();
        
        // Create test rule groups
        YamlRuleGroup group1 = createTestRuleGroup("group-1", "Transaction Rules", 
                                                   Arrays.asList("rule-1", "rule-2"));
        YamlRuleGroup group2 = createTestRuleGroup("group-2", "Customer Rules", 
                                                   Arrays.asList("rule-3", "rule-4"));
        
        ruleGroups.add(group1);
        ruleGroups.add(group2);

        configuration.setRuleGroups(ruleGroups);

        assertNotNull(configuration.getRuleGroups(), "Rule groups should not be null");
        assertEquals(2, configuration.getRuleGroups().size(), "Should have 2 rule groups");
        assertEquals("group-1", configuration.getRuleGroups().get(0).getId(), "First group ID should match");
        assertEquals("Transaction Rules", configuration.getRuleGroups().get(0).getName(), "First group name should match");
        assertEquals("group-2", configuration.getRuleGroups().get(1).getId(), "Second group ID should match");
        assertEquals("Customer Rules", configuration.getRuleGroups().get(1).getName(), "Second group name should match");
    }

    @Test
    @DisplayName("Should handle rule groups with different configurations")
    void testRuleGroupsWithDifferentConfigurations() {
        List<YamlRuleGroup> ruleGroups = new ArrayList<>();
        
        // Group with many rules
        YamlRuleGroup largeGroup = createTestRuleGroup("large-group", "Large Group", 
                                                       Arrays.asList("rule-1", "rule-2", "rule-3", "rule-4", "rule-5"));
        
        // Group with single rule
        YamlRuleGroup singleGroup = createTestRuleGroup("single-group", "Single Group", 
                                                        Arrays.asList("rule-6"));
        
        // Group with no rules
        YamlRuleGroup emptyGroup = createTestRuleGroup("empty-group", "Empty Group", 
                                                       new ArrayList<>());
        
        ruleGroups.add(largeGroup);
        ruleGroups.add(singleGroup);
        ruleGroups.add(emptyGroup);

        configuration.setRuleGroups(ruleGroups);

        assertEquals(3, configuration.getRuleGroups().size(), "Should have 3 rule groups");
        assertEquals(5, configuration.getRuleGroups().get(0).getRuleIds().size(), "Large group should have 5 rules");
        assertEquals(1, configuration.getRuleGroups().get(1).getRuleIds().size(), "Single group should have 1 rule");
        assertEquals(0, configuration.getRuleGroups().get(2).getRuleIds().size(), "Empty group should have 0 rules");
    }

    // ========================================
    // Categories Management Tests
    // ========================================

    @Test
    @DisplayName("Should manage categories correctly")
    void testCategoriesManagement() {
        List<YamlCategory> categories = new ArrayList<>();
        
        // Create test categories
        YamlCategory category1 = createTestCategory("financial", "Financial Rules", "Financial processing rules");
        YamlCategory category2 = createTestCategory("security", "Security Rules", "Security validation rules");
        
        categories.add(category1);
        categories.add(category2);

        configuration.setCategories(categories);

        assertNotNull(configuration.getCategories(), "Categories should not be null");
        assertEquals(2, configuration.getCategories().size(), "Should have 2 categories");
        assertEquals("financial", configuration.getCategories().get(0).getName(), "First category name should match");
        assertEquals("Financial Rules", configuration.getCategories().get(0).getDisplayName(), "First category display name should match");
        assertEquals("security", configuration.getCategories().get(1).getName(), "Second category name should match");
        assertEquals("Security Rules", configuration.getCategories().get(1).getDisplayName(), "Second category display name should match");
    }

    // ========================================
    // Data Sources Management Tests
    // ========================================

    @Test
    @DisplayName("Should manage data sources correctly")
    void testDataSourcesManagement() {
        List<YamlDataSource> dataSources = new ArrayList<>();
        
        // Create test data sources
        YamlDataSource dataSource1 = createTestDataSource("main-db", "Main Database", "postgresql");
        YamlDataSource dataSource2 = createTestDataSource("cache-db", "Cache Database", "redis");
        YamlDataSource dataSource3 = createTestDataSource("analytics-db", "Analytics Database", "mongodb");
        
        dataSources.add(dataSource1);
        dataSources.add(dataSource2);
        dataSources.add(dataSource3);

        configuration.setDataSources(dataSources);

        assertNotNull(configuration.getDataSources(), "Data sources should not be null");
        assertEquals(3, configuration.getDataSources().size(), "Should have 3 data sources");
        assertEquals("main-db", configuration.getDataSources().get(0).getName(), "First data source name should match");
        assertEquals("postgresql", configuration.getDataSources().get(0).getType(), "First data source type should match");
        assertEquals("cache-db", configuration.getDataSources().get(1).getName(), "Second data source name should match");
        assertEquals("redis", configuration.getDataSources().get(1).getType(), "Second data source type should match");
        assertEquals("analytics-db", configuration.getDataSources().get(2).getName(), "Third data source name should match");
        assertEquals("mongodb", configuration.getDataSources().get(2).getType(), "Third data source type should match");
    }

    // ========================================
    // Enrichments and Transformations Tests
    // ========================================

    @Test
    @DisplayName("Should manage enrichments correctly")
    void testEnrichmentsManagement() {
        List<YamlEnrichment> enrichments = new ArrayList<>();
        
        // Create test enrichments
        YamlEnrichment enrichment1 = createTestEnrichment("customer-enrichment", "Customer Data Enrichment");
        YamlEnrichment enrichment2 = createTestEnrichment("location-enrichment", "Location Data Enrichment");
        
        enrichments.add(enrichment1);
        enrichments.add(enrichment2);

        configuration.setEnrichments(enrichments);

        assertNotNull(configuration.getEnrichments(), "Enrichments should not be null");
        assertEquals(2, configuration.getEnrichments().size(), "Should have 2 enrichments");
        assertEquals("customer-enrichment", configuration.getEnrichments().get(0).getId(), "First enrichment ID should match");
        assertEquals("location-enrichment", configuration.getEnrichments().get(1).getId(), "Second enrichment ID should match");
    }

    @Test
    @DisplayName("Should manage transformations correctly")
    void testTransformationsManagement() {
        List<YamlTransformation> transformations = new ArrayList<>();
        
        // Create test transformations
        YamlTransformation transformation1 = createTestTransformation("normalize-data", "Data Normalization");
        YamlTransformation transformation2 = createTestTransformation("format-output", "Output Formatting");
        
        transformations.add(transformation1);
        transformations.add(transformation2);

        configuration.setTransformations(transformations);

        assertNotNull(configuration.getTransformations(), "Transformations should not be null");
        assertEquals(2, configuration.getTransformations().size(), "Should have 2 transformations");
        assertEquals("normalize-data", configuration.getTransformations().get(0).getId(), "First transformation ID should match");
        assertEquals("format-output", configuration.getTransformations().get(1).getId(), "Second transformation ID should match");
    }

    // ========================================
    // Rule Chains Management Tests
    // ========================================

    @Test
    @DisplayName("Should manage rule chains correctly")
    void testRuleChainsManagement() {
        List<YamlRuleChain> ruleChains = new ArrayList<>();
        
        // Create test rule chains
        YamlRuleChain chain1 = createTestRuleChain("conditional-chain", "Conditional Chain", "conditional-chaining");
        YamlRuleChain chain2 = createTestRuleChain("sequential-chain", "Sequential Chain", "sequential-dependency");
        
        ruleChains.add(chain1);
        ruleChains.add(chain2);

        configuration.setRuleChains(ruleChains);

        assertNotNull(configuration.getRuleChains(), "Rule chains should not be null");
        assertEquals(2, configuration.getRuleChains().size(), "Should have 2 rule chains");
        assertEquals("conditional-chain", configuration.getRuleChains().get(0).getId(), "First chain ID should match");
        assertEquals("conditional-chaining", configuration.getRuleChains().get(0).getPattern(), "First chain pattern should match");
        assertEquals("sequential-chain", configuration.getRuleChains().get(1).getId(), "Second chain ID should match");
        assertEquals("sequential-dependency", configuration.getRuleChains().get(1).getPattern(), "Second chain pattern should match");
    }

    // ========================================
    // Complete Configuration Tests
    // ========================================

    @Test
    @DisplayName("Should handle complete configuration with all components")
    void testCompleteConfiguration() {
        // Set up complete configuration
        YamlRuleConfiguration.ConfigurationMetadata metadata = new YamlRuleConfiguration.ConfigurationMetadata();
        metadata.setName("Complete Test Configuration");
        metadata.setVersion("1.0.0");
        metadata.setDescription("Complete configuration with all components");
        configuration.setMetadata(metadata);

        // Add rules
        List<YamlRule> rules = Arrays.asList(
            createTestRule("rule-1", "Rule 1", "true", "Message 1"),
            createTestRule("rule-2", "Rule 2", "#amount > 100", "Message 2")
        );
        configuration.setRules(rules);

        // Add rule groups
        List<YamlRuleGroup> ruleGroups = Arrays.asList(
            createTestRuleGroup("group-1", "Group 1", Arrays.asList("rule-1", "rule-2"))
        );
        configuration.setRuleGroups(ruleGroups);

        // Add categories
        List<YamlCategory> categories = Arrays.asList(
            createTestCategory("category-1", "Category 1", "Test category")
        );
        configuration.setCategories(categories);

        // Add data sources
        List<YamlDataSource> dataSources = Arrays.asList(
            createTestDataSource("db-1", "Database 1", "postgresql")
        );
        configuration.setDataSources(dataSources);

        // Add enrichments
        List<YamlEnrichment> enrichments = Arrays.asList(
            createTestEnrichment("enrichment-1", "Enrichment 1")
        );
        configuration.setEnrichments(enrichments);

        // Add transformations
        List<YamlTransformation> transformations = Arrays.asList(
            createTestTransformation("transformation-1", "Transformation 1")
        );
        configuration.setTransformations(transformations);

        // Add rule chains
        List<YamlRuleChain> ruleChains = Arrays.asList(
            createTestRuleChain("chain-1", "Chain 1", "conditional-chaining")
        );
        configuration.setRuleChains(ruleChains);

        // Validate all components are properly set
        assertNotNull(configuration.getMetadata(), "Metadata should be set");
        assertEquals(2, configuration.getRules().size(), "Should have 2 rules");
        assertEquals(1, configuration.getRuleGroups().size(), "Should have 1 rule group");
        assertEquals(1, configuration.getCategories().size(), "Should have 1 category");
        assertEquals(1, configuration.getDataSources().size(), "Should have 1 data source");
        assertEquals(1, configuration.getEnrichments().size(), "Should have 1 enrichment");
        assertEquals(1, configuration.getTransformations().size(), "Should have 1 transformation");
        assertEquals(1, configuration.getRuleChains().size(), "Should have 1 rule chain");
    }

    // ========================================
    // Helper Methods
    // ========================================

    private YamlRule createTestRule(String id, String name, String condition, String message) {
        YamlRule rule = new YamlRule();
        rule.setId(id);
        rule.setName(name);
        rule.setCondition(condition);
        rule.setMessage(message);
        rule.setEnabled(true);
        rule.setPriority(100);
        return rule;
    }

    private YamlRuleGroup createTestRuleGroup(String id, String name, List<String> ruleIds) {
        YamlRuleGroup group = new YamlRuleGroup();
        group.setId(id);
        group.setName(name);
        group.setDescription("Test rule group: " + name);
        group.setRuleIds(ruleIds);
        return group;
    }

    private YamlCategory createTestCategory(String name, String displayName, String description) {
        YamlCategory category = new YamlCategory();
        category.setName(name);
        category.setDisplayName(displayName);
        category.setDescription(description);
        category.setEnabled(true);
        return category;
    }

    private YamlDataSource createTestDataSource(String name, String description, String type) {
        YamlDataSource dataSource = new YamlDataSource();
        dataSource.setName(name);
        dataSource.setDescription(description);
        dataSource.setType(type);
        dataSource.setEnabled(true);
        return dataSource;
    }

    private YamlEnrichment createTestEnrichment(String id, String name) {
        YamlEnrichment enrichment = new YamlEnrichment();
        enrichment.setId(id);
        enrichment.setName(name);
        enrichment.setEnabled(true);
        return enrichment;
    }

    private YamlTransformation createTestTransformation(String id, String name) {
        YamlTransformation transformation = new YamlTransformation();
        transformation.setId(id);
        transformation.setName(name);
        transformation.setEnabled(true);
        return transformation;
    }

    private YamlRuleChain createTestRuleChain(String id, String name, String pattern) {
        YamlRuleChain ruleChain = new YamlRuleChain();
        ruleChain.setId(id);
        ruleChain.setName(name);
        ruleChain.setPattern(pattern);
        ruleChain.setEnabled(true);
        ruleChain.setPriority(100);
        return ruleChain;
    }
}
