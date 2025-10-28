package dev.mars.apex.yaml.manager.integration;

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

import dev.mars.apex.yaml.manager.model.YamlConfigMetadata;
import dev.mars.apex.yaml.manager.service.CatalogScanService;
import dev.mars.apex.yaml.manager.service.CatalogService;
import dev.mars.apex.yaml.manager.service.YamlContentAnalyzer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive integration tests for Catalog and Discovery System.
 * 
 * Tests all Layer 2 features using real YAML files from test/resources:
 * - Centralized metadata index of all configurations
 * - Full-text search across descriptions, tags, authors
 * - Domain-based and use-case based discovery
 * - Relationship queries (find all files using X)
 * - Unused configuration detection
 * - Critical configuration identification
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-10-28
 * @version 1.0
 */
@DisplayName("Catalog and Discovery System Integration Tests")
public class CatalogDiscoveryIntegrationTest {

    private static final Logger logger = LoggerFactory.getLogger(CatalogDiscoveryIntegrationTest.class);

    private CatalogService catalogService;
    private CatalogScanService catalogScanService;
    private YamlContentAnalyzer contentAnalyzer;
    private String testResourcesPath;

    @BeforeEach
    public void setUp() {
        catalogService = new CatalogService();
        contentAnalyzer = new YamlContentAnalyzer();
        catalogScanService = new CatalogScanService();

        // Inject dependencies into CatalogScanService
        try {
            java.lang.reflect.Field contentAnalyzerField = CatalogScanService.class.getDeclaredField("contentAnalyzer");
            contentAnalyzerField.setAccessible(true);
            contentAnalyzerField.set(catalogScanService, contentAnalyzer);

            java.lang.reflect.Field catalogServiceField = CatalogScanService.class.getDeclaredField("catalogService");
            catalogServiceField.setAccessible(true);
            catalogServiceField.set(catalogScanService, catalogService);
        } catch (Exception e) {
            throw new RuntimeException("Failed to inject dependencies", e);
        }

        // Get path to test resources
        testResourcesPath = "src/test/resources/apex-yaml-samples";
        logger.info("Test resources path: {}", testResourcesPath);
    }

    // ========================================
    // Test 1: Centralized Metadata Index
    // ========================================

    @Test
    @DisplayName("Should build centralized metadata index from YAML files")
    public void testBuildCentralizedMetadataIndex() {
        // Scan and index all YAML files in apex-yaml-samples
        File samplesDir = new File(testResourcesPath);
        assertTrue(samplesDir.exists(), "Test samples directory should exist");

        int indexedCount = scanAndIndexDirectory(samplesDir);

        logger.info("Indexed {} YAML files", indexedCount);
        assertTrue(indexedCount > 0, "Should have indexed at least one YAML file");

        // Verify catalog contains configurations
        Collection<YamlConfigMetadata> allConfigs = catalogService.getAllConfigurations();
        assertNotNull(allConfigs);
        int catalogSize = allConfigs.size();
        logger.info("Catalog contains {} configurations", catalogSize);
        assertTrue(catalogSize > 0, "Catalog should contain indexed files");
        // Note: Some files may fail to index due to parsing errors, so we check for > 0 instead of exact match

        // Analyze metadata completeness
        int withType = 0, withName = 0, withDescription = 0, withAuthor = 0, withVersion = 0;
        int withTags = 0, withBusinessDomain = 0, withOwner = 0, withDependencies = 0;
        int withHealthScore = 0, withTimestamps = 0;

        // Verify each configuration has required metadata
        for (YamlConfigMetadata config : allConfigs) {
            assertNotNull(config.getId(), "Configuration should have an ID");
            assertNotNull(config.getPath(), "Configuration should have a path");

            // Count metadata completeness
            if (config.getType() != null) withType++;
            if (config.getName() != null) withName++;
            if (config.getDescription() != null) withDescription++;
            if (config.getAuthor() != null) withAuthor++;
            if (config.getVersion() != null) withVersion++;
            if (config.getTags() != null && !config.getTags().isEmpty()) withTags++;
            if (config.getBusinessDomain() != null) withBusinessDomain++;
            if (config.getOwner() != null) withOwner++;
            if (config.getDependencies() != null && !config.getDependencies().isEmpty()) withDependencies++;
            if (config.getHealthScore() > 0) withHealthScore++;
            if (config.getCreated() != null || config.getLastModified() != null) withTimestamps++;

            logger.debug("Indexed config: id={}, type={}, name={}, author={}, healthScore={}",
                config.getId(), config.getType(), config.getName(), config.getAuthor(), config.getHealthScore());
        }

        // Log metadata completeness statistics
        logger.info("=== Metadata Completeness ===");
        logger.info("Configurations with type: {} ({} %)", withType, (withType * 100 / catalogSize));
        logger.info("Configurations with name: {} ({} %)", withName, (withName * 100 / catalogSize));
        logger.info("Configurations with description: {} ({} %)", withDescription, (withDescription * 100 / catalogSize));
        logger.info("Configurations with author: {} ({} %)", withAuthor, (withAuthor * 100 / catalogSize));
        logger.info("Configurations with version: {} ({} %)", withVersion, (withVersion * 100 / catalogSize));
        logger.info("Configurations with tags: {} ({} %)", withTags, (withTags * 100 / catalogSize));
        logger.info("Configurations with business-domain: {} ({} %)", withBusinessDomain, (withBusinessDomain * 100 / catalogSize));
        logger.info("Configurations with owner: {} ({} %)", withOwner, (withOwner * 100 / catalogSize));
        logger.info("Configurations with dependencies: {} ({} %)", withDependencies, (withDependencies * 100 / catalogSize));
        logger.info("Configurations with health score: {} ({} %)", withHealthScore, (withHealthScore * 100 / catalogSize));
        logger.info("Configurations with timestamps: {} ({} %)", withTimestamps, (withTimestamps * 100 / catalogSize));

        // Verify that most configurations have essential metadata
        assertTrue(withType > catalogSize * 0.9, "At least 90% should have type information");
        assertTrue(withDescription > catalogSize * 0.9, "At least 90% should have descriptions");
        assertTrue(withAuthor > catalogSize * 0.9, "At least 90% should have author information");
        assertTrue(withVersion > catalogSize * 0.9, "At least 90% should have version information");
        assertTrue(withHealthScore > 0, "Configurations should have health scores");
        assertTrue(withTimestamps > catalogSize * 0.5, "At least 50% should have timestamp information");

        // Verify that tags, business domains, and owners are being captured
        assertTrue(withTags > 0, "Some configurations should have tags");
        assertTrue(withBusinessDomain > 0, "Some configurations should have business domains");
        assertTrue(withOwner > 0, "Some configurations should have owners");

        // Verify that files with dependencies are properly captured
        assertTrue(withDependencies > 0, "Some configurations should have dependencies");

        // Find a specific file with tags to verify tag extraction
        YamlConfigMetadata baseValidation = catalogService.getConfiguration("base-validation-rules");
        if (baseValidation != null) {
            assertNotNull(baseValidation.getTags(), "base-validation-rules should have tags");
            assertTrue(baseValidation.getTags().contains("validation"), "Should contain 'validation' tag");
            assertTrue(baseValidation.getTags().contains("base-rules"), "Should contain 'base-rules' tag");
            assertEquals("apex.demo@company.com", baseValidation.getAuthor(), "Should have correct author");
        }

        // Find a file with dependencies to verify dependency extraction
        YamlConfigMetadata tradeScenario = catalogService.getConfiguration("trade-processing-scenario");
        if (tradeScenario != null) {
            assertNotNull(tradeScenario.getDependencies(), "trade-processing-scenario should have dependencies");
            assertTrue(tradeScenario.getDependencies().size() > 0, "Should have at least one dependency");
            assertTrue(tradeScenario.getDependencies().contains("02-validation-groups.yaml") ||
                      tradeScenario.getDependencies().contains("03-enrichment-rules.yaml"),
                      "Should contain expected dependency files");
        }

        // Verify graph-100 files have tags, business domain, and owner
        YamlConfigMetadata rulesA = catalogService.getConfiguration("graph100-rules-a");
        if (rulesA != null) {
            assertNotNull(rulesA.getTags(), "graph100-rules-a should have tags");
            assertTrue(rulesA.getTags().contains("rules"), "Should contain 'rules' tag");
            assertTrue(rulesA.getTags().contains("validation"), "Should contain 'validation' tag");
            assertNotNull(rulesA.getBusinessDomain(), "graph100-rules-a should have business domain");
            assertEquals("Trade Validation", rulesA.getBusinessDomain(), "Should have 'Trade Validation' business domain");
        }
    }

    // ========================================
    // Test 2: Full-text Search by Tags
    // ========================================

    @Test
    @DisplayName("Should search configurations by tags")
    public void testSearchByTags() {
        // Index sample files
        scanAndIndexDirectory(new File(testResourcesPath));
        
        // Search for configurations with specific tags
        List<YamlConfigMetadata> validationConfigs = catalogService.findByTag("validation");
        List<YamlConfigMetadata> baseRulesConfigs = catalogService.findByTag("base-rules");
        List<YamlConfigMetadata> reusableConfigs = catalogService.findByTag("reusable");
        
        logger.info("Found {} configs with 'validation' tag", validationConfigs.size());
        logger.info("Found {} configs with 'base-rules' tag", baseRulesConfigs.size());
        logger.info("Found {} configs with 'reusable' tag", reusableConfigs.size());
        
        // Verify results
        assertNotNull(validationConfigs);
        assertNotNull(baseRulesConfigs);
        assertNotNull(reusableConfigs);
        
        // Log found configurations
        for (YamlConfigMetadata config : validationConfigs) {
            logger.debug("Validation config: {}", config.getId());
        }
    }

    // ========================================
    // Test 3: Search by Author
    // ========================================

    @Test
    @DisplayName("Should search configurations by author")
    public void testSearchByAuthor() {
        // Index sample files
        scanAndIndexDirectory(new File(testResourcesPath));
        
        // Search for configurations by author
        List<YamlConfigMetadata> demoConfigs = catalogService.findByAuthor("apex.demo@company.com");
        
        logger.info("Found {} configs by author 'apex.demo@company.com'", demoConfigs.size());
        assertNotNull(demoConfigs);
        
        // Verify all results have the correct author
        for (YamlConfigMetadata config : demoConfigs) {
            assertEquals("apex.demo@company.com", config.getAuthor());
            logger.debug("Config by author: id={}, name={}", config.getId(), config.getName());
        }
    }

    // ========================================
    // Test 4: Domain-based Discovery (by Type)
    // ========================================

    @Test
    @DisplayName("Should discover configurations by type (domain-based)")
    public void testDomainBasedDiscoveryByType() {
        // Index sample files
        scanAndIndexDirectory(new File(testResourcesPath));
        
        // Search by different types (actual types from YamlContentAnalyzer)
        List<YamlConfigMetadata> ruleConfigs = catalogService.findByType("rules");
        List<YamlConfigMetadata> enrichmentConfigs = catalogService.findByType("enrichments");
        List<YamlConfigMetadata> scenarioConfigs = catalogService.findByType("scenario");

        logger.info("Found {} rules files", ruleConfigs.size());
        logger.info("Found {} enrichments files", enrichmentConfigs.size());
        logger.info("Found {} scenario files", scenarioConfigs.size());

        assertNotNull(ruleConfigs);
        assertNotNull(enrichmentConfigs);
        assertNotNull(scenarioConfigs);

        // Verify type consistency
        for (YamlConfigMetadata config : ruleConfigs) {
            assertEquals("rules", config.getType());
        }
    }

    // ========================================
    // Test 5: Use-case Based Discovery (by Business Domain)
    // ========================================

    @Test
    @DisplayName("Should discover configurations by business domain (use-case based)")
    public void testUseCaseBasedDiscoveryByBusinessDomain() {
        // Index sample files
        scanAndIndexDirectory(new File(testResourcesPath));

        // Get all distinct business domains
        Set<String> allBusinessDomains = catalogService.getAllBusinessDomains();
        logger.info("=== All Distinct Business Domains ({}) ===", allBusinessDomains.size());
        allBusinessDomains.stream().sorted().forEach(domain -> {
            int count = catalogService.findByMetadataAttribute("business-domain", domain).size();
            logger.info("  - {} ({} configs)", domain, count);
        });

        // Search by specific business domains that we know exist from graph-100 files
        List<YamlConfigMetadata> tradeValidation = catalogService.findByMetadataAttribute("business-domain", "Trade Validation");
        List<YamlConfigMetadata> dataEnrichment = catalogService.findByMetadataAttribute("business-domain", "Data Enrichment");
        List<YamlConfigMetadata> tradeProcessing = catalogService.findByMetadataAttribute("business-domain", "Trade Processing");

        logger.info("\n=== Business Domain Search Results ===");
        logger.info("Found {} configs in 'Trade Validation' business domain", tradeValidation.size());
        logger.info("Found {} configs in 'Data Enrichment' business domain", dataEnrichment.size());
        logger.info("Found {} configs in 'Trade Processing' business domain", tradeProcessing.size());

        // Assertions
        assertNotNull(allBusinessDomains);
        assertTrue(allBusinessDomains.size() > 0, "Should have at least one business domain");

        assertNotNull(tradeValidation);
        assertNotNull(dataEnrichment);
        assertNotNull(tradeProcessing);

        // Verify we have configs in these business domains
        assertTrue(tradeValidation.size() > 0, "Should have configs in 'Trade Validation' business domain");
        assertTrue(dataEnrichment.size() > 0, "Should have configs in 'Data Enrichment' business domain");
        assertTrue(tradeProcessing.size() > 0, "Should have configs in 'Trade Processing' business domain");
    }

    // ========================================
    // Test 6: Comprehensive Search Across All Metadata Fields
    // ========================================

    @Test
    @DisplayName("Should search configurations across all metadata fields")
    public void testComprehensiveSearch() {
        // Index sample files
        scanAndIndexDirectory(new File(testResourcesPath));

        logger.info("\n=== Comprehensive Search Tests ===");

        // Search by ID fragment
        List<YamlConfigMetadata> idSearch = catalogService.search("graph100-rules");
        logger.info("Search 'graph100-rules' (ID): {} results", idSearch.size());
        assertTrue(idSearch.size() > 0, "Should find configs with 'graph100-rules' in ID");

        // Search by type
        List<YamlConfigMetadata> typeSearch = catalogService.search("scenario");
        logger.info("Search 'scenario' (type): {} results", typeSearch.size());
        assertTrue(typeSearch.size() > 0, "Should find configs with 'scenario' in type");

        // Search by tag
        List<YamlConfigMetadata> tagSearch = catalogService.search("validation");
        logger.info("Search 'validation' (tag): {} results", tagSearch.size());
        assertTrue(tagSearch.size() > 0, "Should find configs with 'validation' tag");

        // Search by category
        List<YamlConfigMetadata> categorySearch = catalogService.search("Trade Processing");
        logger.info("Search 'Trade Processing' (category): {} results", categorySearch.size());
        assertTrue(categorySearch.size() > 0, "Should find configs in 'Trade Processing' category");

        // Search by author
        List<YamlConfigMetadata> authorSearch = catalogService.search("apex.demo");
        logger.info("Search 'apex.demo' (author): {} results", authorSearch.size());
        assertTrue(authorSearch.size() > 0, "Should find configs by 'apex.demo' author");

        // Search by description
        List<YamlConfigMetadata> descSearch = catalogService.search("enrichment");
        logger.info("Search 'enrichment' (description): {} results", descSearch.size());
        assertTrue(descSearch.size() > 0, "Should find configs with 'enrichment' in description");

        // Search by path
        List<YamlConfigMetadata> pathSearch = catalogService.search("graph-100");
        logger.info("Search 'graph-100' (path): {} results", pathSearch.size());
        assertTrue(pathSearch.size() > 0, "Should find configs with 'graph-100' in path");

        // Search by dependency
        List<YamlConfigMetadata> depSearch = catalogService.search("30-rules-a.yaml");
        logger.info("Search '30-rules-a.yaml' (dependency): {} results", depSearch.size());

        // Verify search is case-insensitive
        List<YamlConfigMetadata> upperSearch = catalogService.search("VALIDATION");
        List<YamlConfigMetadata> lowerSearch = catalogService.search("validation");
        assertEquals(upperSearch.size(), lowerSearch.size(), "Search should be case-insensitive");

        // Empty query should return empty list
        List<YamlConfigMetadata> emptySearch = catalogService.search("");
        assertEquals(0, emptySearch.size(), "Empty query should return no results");

        logger.info("\n=== Search Summary ===");
        logger.info("All search tests passed - catalog search is working across all metadata fields!");
    }

    // ========================================
    // Test 7: Get Distinct Values by Attribute Name
    // ========================================

    @Test
    @DisplayName("Should get distinct values for any metadata attribute by name")
    public void testGetDistinctValuesByAttributeName() {
        // Index sample files
        scanAndIndexDirectory(new File(testResourcesPath));

        logger.info("\n=== Get Distinct Values by Attribute Name ===");

        // Test all supported attributes
        Set<String> tags = catalogService.getDistinctValues("tags");
        logger.info("Distinct tags: {} values", tags.size());
        assertTrue(tags.size() > 0, "Should have distinct tags");

        Set<String> businessDomains = catalogService.getDistinctValues("business-domain");
        logger.info("Distinct business-domains: {} values", businessDomains.size());
        assertTrue(businessDomains.size() > 0, "Should have distinct business domains");

        Set<String> owners = catalogService.getDistinctValues("owner");
        logger.info("Distinct owners: {} values", owners.size());
        assertTrue(owners.size() > 0, "Should have distinct owners");

        Set<String> types = catalogService.getDistinctValues("types");
        logger.info("Distinct types: {} values", types.size());
        assertTrue(types.size() > 0, "Should have distinct types");

        Set<String> authors = catalogService.getDistinctValues("authors");
        logger.info("Distinct authors: {} values", authors.size());
        assertTrue(authors.size() > 0, "Should have distinct authors");

        Set<String> versions = catalogService.getDistinctValues("versions");
        logger.info("Distinct versions: {} values", versions.size());
        assertTrue(versions.size() > 0, "Should have distinct versions");

        Set<String> ids = catalogService.getDistinctValues("ids");
        logger.info("Distinct ids: {} values", ids.size());
        assertEquals(369, ids.size(), "Should have 369 distinct IDs");

        Set<String> names = catalogService.getDistinctValues("names");
        logger.info("Distinct names: {} values", names.size());
        assertTrue(names.size() > 0, "Should have distinct names");

        Set<String> descriptions = catalogService.getDistinctValues("descriptions");
        logger.info("Distinct descriptions: {} values", descriptions.size());
        assertTrue(descriptions.size() > 0, "Should have distinct descriptions");

        Set<String> paths = catalogService.getDistinctValues("paths");
        logger.info("Distinct paths: {} values", paths.size());
        assertEquals(369, paths.size(), "Should have 369 distinct paths");

        // Test case-insensitivity
        Set<String> tagsUpper = catalogService.getDistinctValues("TAGS");
        Set<String> tagsLower = catalogService.getDistinctValues("tags");
        assertEquals(tagsUpper.size(), tagsLower.size(), "Attribute name should be case-insensitive");

        // Test singular form
        Set<String> tag = catalogService.getDistinctValues("tag");
        assertEquals(tags.size(), tag.size(), "Should support singular form 'tag'");

        Set<String> owner = catalogService.getDistinctValues("owners");
        assertEquals(owners.size(), owner.size(), "Should support plural form 'owners'");

        // Test invalid attribute
        Set<String> invalid = catalogService.getDistinctValues("invalid-attribute");
        assertEquals(0, invalid.size(), "Invalid attribute should return empty set");

        // Test null/empty attribute
        Set<String> nullAttr = catalogService.getDistinctValues(null);
        assertEquals(0, nullAttr.size(), "Null attribute should return empty set");

        Set<String> emptyAttr = catalogService.getDistinctValues("");
        assertEquals(0, emptyAttr.size(), "Empty attribute should return empty set");

        logger.info("\n=== Distinct Values Summary ===");
        logger.info("✅ Tags: {}", tags.size());
        logger.info("✅ Business Domains: {}", businessDomains.size());
        logger.info("✅ Owners: {}", owners.size());
        logger.info("✅ Types: {}", types.size());
        logger.info("✅ Authors: {}", authors.size());
        logger.info("✅ Versions: {}", versions.size());
        logger.info("✅ IDs: {}", ids.size());
        logger.info("✅ Names: {}", names.size());
        logger.info("✅ Descriptions: {}", descriptions.size());
        logger.info("✅ Paths: {}", paths.size());

        // Display actual distinct values for key attributes
        logger.info("\n=== Actual Distinct Values ===");

        logger.info("\nDistinct Types ({}):", types.size());
        types.stream().sorted().forEach(t -> logger.info("  - {}", t));

        logger.info("\nDistinct Authors ({}):", authors.size());
        authors.stream().sorted().forEach(a -> logger.info("  - {}", a));

        logger.info("\nDistinct Versions ({}):", versions.size());
        versions.stream().sorted().forEach(v -> logger.info("  - {}", v));

        logger.info("\nDistinct Tags (showing first 20 of {}):", tags.size());
        tags.stream().sorted().limit(20).forEach(tg -> logger.info("  - {}", tg));

        logger.info("\nDistinct Business Domains (all {}):", businessDomains.size());
        businessDomains.stream().sorted().forEach(domain -> logger.info("  - {}", domain));

        logger.info("\nDistinct Owners (all {}):", owners.size());
        owners.stream().sorted().forEach(own -> logger.info("  - {}", own));

        logger.info("\nAll distinct value queries working correctly!");
    }

    // ========================================
    // Test 8: Unused Configuration Detection
    // ========================================

    @Test
    @DisplayName("Should detect unused (orphaned) configurations")
    public void testUnusedConfigurationDetection() {
        // Index sample files
        scanAndIndexDirectory(new File(testResourcesPath));
        
        // Find unused configurations
        List<YamlConfigMetadata> unusedConfigs = catalogService.findUnused();
        
        logger.info("Found {} unused/orphaned configurations", unusedConfigs.size());
        assertNotNull(unusedConfigs);
        
        // Log unused configurations
        for (YamlConfigMetadata config : unusedConfigs) {
            assertTrue(config.isOrphaned(), "Unused config should be marked as orphaned");
            logger.debug("Unused config: id={}, path={}", config.getId(), config.getPath());
        }
    }

    // ========================================
    // Test 7: Critical Configuration Identification
    // ========================================

    @Test
    @DisplayName("Should identify critical configurations")
    public void testCriticalConfigurationIdentification() {
        // Index sample files
        scanAndIndexDirectory(new File(testResourcesPath));
        
        // Find critical configurations
        List<YamlConfigMetadata> criticalConfigs = catalogService.findCritical();
        
        logger.info("Found {} critical configurations", criticalConfigs.size());
        assertNotNull(criticalConfigs);
        
        // Verify all results are marked as critical
        for (YamlConfigMetadata config : criticalConfigs) {
            assertTrue(config.isCritical(), "Critical config should be marked as critical");
            logger.debug("Critical config: id={}, path={}", config.getId(), config.getPath());
        }
    }

    // ========================================
    // Test 8: Health Score Queries
    // ========================================

    @Test
    @DisplayName("Should query configurations by health score range")
    public void testHealthScoreQueries() {
        // Index sample files
        scanAndIndexDirectory(new File(testResourcesPath));
        
        // Query by different health score ranges
        List<YamlConfigMetadata> excellentHealth = catalogService.findByHealthScore(90, 100);
        List<YamlConfigMetadata> goodHealth = catalogService.findByHealthScore(75, 89);
        List<YamlConfigMetadata> fairHealth = catalogService.findByHealthScore(50, 74);
        List<YamlConfigMetadata> poorHealth = catalogService.findByHealthScore(0, 49);
        
        logger.info("Excellent health (90-100): {} configs", excellentHealth.size());
        logger.info("Good health (75-89): {} configs", goodHealth.size());
        logger.info("Fair health (50-74): {} configs", fairHealth.size());
        logger.info("Poor health (0-49): {} configs", poorHealth.size());
        
        assertNotNull(excellentHealth);
        assertNotNull(goodHealth);
        assertNotNull(fairHealth);
        assertNotNull(poorHealth);
    }

    // ========================================
    // Test 9: Catalog Statistics
    // ========================================

    @Test
    @DisplayName("Should calculate catalog statistics")
    public void testCatalogStatistics() {
        // Index sample files
        scanAndIndexDirectory(new File(testResourcesPath));
        
        // Get statistics
        int totalConfigs = catalogService.getTotalConfigurations();
        int orphanedCount = catalogService.getOrphanedCount();
        int criticalCount = catalogService.getCriticalCount();
        double avgHealthScore = catalogService.getAverageHealthScore();
        
        logger.info("=== Catalog Statistics ===");
        logger.info("Total configurations: {}", totalConfigs);
        logger.info("Orphaned configurations: {}", orphanedCount);
        logger.info("Critical configurations: {}", criticalCount);
        logger.info("Average health score: {}", avgHealthScore);
        
        assertTrue(totalConfigs > 0, "Should have at least one configuration");
        assertTrue(avgHealthScore >= 0 && avgHealthScore <= 100, "Average health score should be 0-100");
    }

    // ========================================
    // Helper Methods
    // ========================================

    /**
     * Scan directory and index all YAML files using CatalogScanService.
     */
    private int scanAndIndexDirectory(File directory) {
        Map<String, Object> result = catalogScanService.scanDirectory(directory.getAbsolutePath());
        return (int) result.get("filesIndexed");
    }
}

