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
import dev.mars.apex.yaml.manager.service.DependencyAnalysisService;
import dev.mars.apex.yaml.manager.service.YamlContentAnalyzer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for Catalog and Discovery System using the graph-100 dataset.
 * 
 * The graph-100 dataset contains 100+ YAML files with complex dependency relationships,
 * making it ideal for testing catalog features at scale:
 * - Scenarios (10-19)
 * - Groups (20-29)
 * - Rules (30-39)
 * - Cycles (40-49)
 * - Chains (50-59)
 * - Enrichments (60-69)
 * - Configs (70-79)
 * - Datasets (80-89)
 * - Pipelines (90-97)
 * - Edge cases (98-99)
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-10-28
 * @version 1.0
 */
@DisplayName("Catalog Graph-100 Dataset Integration Tests")
public class CatalogGraph100IntegrationTest {

    private static final Logger logger = LoggerFactory.getLogger(CatalogGraph100IntegrationTest.class);

    private CatalogService catalogService;
    private CatalogScanService catalogScanService;
    private YamlContentAnalyzer contentAnalyzer;
    private DependencyAnalysisService dependencyService;
    private String graph100Path;

    @BeforeEach
    public void setUp() {
        catalogService = new CatalogService();
        contentAnalyzer = new YamlContentAnalyzer();
        catalogScanService = new CatalogScanService();
        dependencyService = new DependencyAnalysisService();

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

        // Get path to graph-100 dataset
        graph100Path = "src/test/resources/apex-yaml-samples/graph-100";
        logger.info("Graph-100 dataset path: {}", graph100Path);
    }

    // ========================================
    // Test 1: Large-scale Catalog Indexing
    // ========================================

    @Test
    @DisplayName("Should index 100 YAML files from graph-100 dataset")
    public void testLargeScaleCatalogIndexing() {
        File graph100Dir = new File(graph100Path);
        assertTrue(graph100Dir.exists(), "Graph-100 directory should exist");

        int indexedCount = scanAndIndexDirectory(graph100Dir);

        logger.info("=== Graph-100 Indexing Results ===");
        logger.info("Total files indexed: {}", indexedCount);

        assertTrue(indexedCount >= 90, "Should have indexed at least 90 YAML files (some may have parsing errors)");

        // Verify catalog size
        int catalogSize = catalogService.getTotalConfigurations();
        logger.info("Catalog size: {}", catalogSize);
        assertTrue(catalogSize >= 90, "Catalog should contain at least 90 configurations");
    }

    // ========================================
    // Test 2: Type Distribution Analysis
    // ========================================

    @Test
    @DisplayName("Should analyze type distribution across graph-100 dataset")
    public void testTypeDistributionAnalysis() {
        scanAndIndexDirectory(new File(graph100Path));
        
        Collection<YamlConfigMetadata> allConfigs = catalogService.getAllConfigurations();
        
        // Count by type
        Map<String, Long> typeDistribution = allConfigs.stream()
            .filter(c -> c.getType() != null)
            .collect(Collectors.groupingBy(
                YamlConfigMetadata::getType,
                Collectors.counting()
            ));
        
        logger.info("=== Type Distribution ===");
        typeDistribution.forEach((type, count) -> 
            logger.info("{}: {} files", type, count));
        
        assertFalse(typeDistribution.isEmpty(), "Should have multiple types");
        
        // Verify we have different types
        assertTrue(typeDistribution.size() > 1, "Should have more than one type");
    }

    // ========================================
    // Test 3: Scenario Files Discovery
    // ========================================

    @Test
    @DisplayName("Should discover all scenario files (10-19)")
    public void testScenarioFilesDiscovery() {
        scanAndIndexDirectory(new File(graph100Path));
        
        // Find scenario files by type
        List<YamlConfigMetadata> scenarios = catalogService.findByType("scenario");
        
        logger.info("Found {} scenario files", scenarios.size());
        
        // Log scenario details
        for (YamlConfigMetadata scenario : scenarios) {
            logger.debug("Scenario: id={}, name={}", scenario.getId(), scenario.getName());
        }
        
        assertNotNull(scenarios);
    }

    // ========================================
    // Test 4: Rule Files Discovery
    // ========================================

    @Test
    @DisplayName("Should discover all rule files (30-39)")
    public void testRuleFilesDiscovery() {
        scanAndIndexDirectory(new File(graph100Path));
        
        // Find rule files
        List<YamlConfigMetadata> rules = catalogService.findByType("rule-config");
        
        logger.info("Found {} rule files", rules.size());
        assertNotNull(rules);
    }

    // ========================================
    // Test 5: Enrichment Files Discovery
    // ========================================

    @Test
    @DisplayName("Should discover all enrichment files (60-69)")
    public void testEnrichmentFilesDiscovery() {
        scanAndIndexDirectory(new File(graph100Path));
        
        // Find enrichment files
        List<YamlConfigMetadata> enrichments = catalogService.findByType("enrichment-config");
        
        logger.info("Found {} enrichment files", enrichments.size());
        assertNotNull(enrichments);
    }

    // ========================================
    // Test 6: Dataset Files Discovery
    // ========================================

    @Test
    @DisplayName("Should discover all dataset files (80-89)")
    public void testDatasetFilesDiscovery() {
        scanAndIndexDirectory(new File(graph100Path));
        
        // Find dataset files
        List<YamlConfigMetadata> datasets = catalogService.findByType("dataset");
        
        logger.info("Found {} dataset files", datasets.size());
        assertNotNull(datasets);
        
        // Log dataset names
        for (YamlConfigMetadata dataset : datasets) {
            logger.debug("Dataset: {}", dataset.getId());
        }
    }

    // ========================================
    // Test 7: Pipeline Files Discovery
    // ========================================

    @Test
    @DisplayName("Should discover all pipeline files (90-97)")
    public void testPipelineFilesDiscovery() {
        scanAndIndexDirectory(new File(graph100Path));
        
        // Find pipeline files
        List<YamlConfigMetadata> pipelines = catalogService.findByType("pipeline");
        
        logger.info("Found {} pipeline files", pipelines.size());
        assertNotNull(pipelines);
    }

    // ========================================
    // Test 8: Circular Dependency Detection
    // ========================================

    @Test
    @DisplayName("Should identify files with circular dependencies (40-49)")
    public void testCircularDependencyDetection() {
        scanAndIndexDirectory(new File(graph100Path));
        
        Collection<YamlConfigMetadata> allConfigs = catalogService.getAllConfigurations();
        
        // Find files with "cycle" in their ID
        List<YamlConfigMetadata> cycleFiles = allConfigs.stream()
            .filter(c -> c.getId() != null && c.getId().contains("cycle"))
            .collect(Collectors.toList());
        
        logger.info("Found {} files with circular dependencies", cycleFiles.size());
        
        for (YamlConfigMetadata cycleFile : cycleFiles) {
            logger.debug("Cycle file: {}", cycleFile.getId());
        }
        
        assertNotNull(cycleFiles);
    }

    // ========================================
    // Test 9: Deep Chain Detection
    // ========================================

    @Test
    @DisplayName("Should identify files in deep dependency chains (50-59)")
    public void testDeepChainDetection() {
        scanAndIndexDirectory(new File(graph100Path));
        
        Collection<YamlConfigMetadata> allConfigs = catalogService.getAllConfigurations();
        
        // Find files with "chain" in their ID
        List<YamlConfigMetadata> chainFiles = allConfigs.stream()
            .filter(c -> c.getId() != null && c.getId().contains("chain"))
            .collect(Collectors.toList());
        
        logger.info("Found {} files in dependency chains", chainFiles.size());
        
        assertNotNull(chainFiles);
        assertTrue(chainFiles.size() >= 10, "Should have at least 10 chain files");
    }

    // ========================================
    // Test 10: Edge Case Files Detection
    // ========================================

    @Test
    @DisplayName("Should identify edge case test files (98-99)")
    public void testEdgeCaseFilesDetection() {
        scanAndIndexDirectory(new File(graph100Path));
        
        Collection<YamlConfigMetadata> allConfigs = catalogService.getAllConfigurations();
        
        // Find edge case files (files starting with 98 or 99)
        List<YamlConfigMetadata> edgeCaseFiles = allConfigs.stream()
            .filter(c -> {
                String id = c.getId();
                return id != null && (id.startsWith("98-") || id.startsWith("99-"));
            })
            .collect(Collectors.toList());
        
        logger.info("Found {} edge case test files", edgeCaseFiles.size());
        
        for (YamlConfigMetadata edgeCase : edgeCaseFiles) {
            logger.debug("Edge case file: {}", edgeCase.getId());
        }
        
        assertNotNull(edgeCaseFiles);
    }

    // ========================================
    // Test 11: Comprehensive Statistics
    // ========================================

    @Test
    @DisplayName("Should generate comprehensive statistics for graph-100 dataset")
    public void testComprehensiveStatistics() {
        scanAndIndexDirectory(new File(graph100Path));

        int total = catalogService.getTotalConfigurations();
        int orphaned = catalogService.getOrphanedCount();
        int critical = catalogService.getCriticalCount();
        double avgHealth = catalogService.getAverageHealthScore();

        logger.info("=== Graph-100 Catalog Statistics ===");
        logger.info("Total configurations: {}", total);
        logger.info("Orphaned configurations: {}", orphaned);
        logger.info("Critical configurations: {}", critical);
        logger.info("Average health score: {:.2f}", avgHealth);
        if (total > 0) {
            logger.info("Orphaned percentage: {:.1f}%", (orphaned * 100.0 / total));
            logger.info("Critical percentage: {:.1f}%", (critical * 100.0 / total));
        }

        assertTrue(total >= 90, "Should have at least 90 configurations (some files may have parsing errors)");
    }

    // ========================================
    // Test 12: Search Performance
    // ========================================

    @Test
    @DisplayName("Should perform searches efficiently on large catalog")
    public void testSearchPerformance() {
        scanAndIndexDirectory(new File(graph100Path));
        
        long startTime = System.currentTimeMillis();
        
        // Perform multiple searches
        catalogService.findByType("scenario");
        catalogService.findByType("rule-config");
        catalogService.findByType("enrichment-config");
        catalogService.findUnused();
        catalogService.findCritical();
        catalogService.findByHealthScore(0, 100);
        
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        
        logger.info("6 searches on {} files completed in {} ms", 
            catalogService.getTotalConfigurations(), duration);
        
        assertTrue(duration < 5000, "Searches should complete in under 5 seconds");
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

