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

package dev.mars.apex.demo;

import java.io.File;

import dev.mars.apex.core.config.yaml.YamlConfigurationLoader;
import dev.mars.apex.core.config.yaml.YamlRuleConfiguration;
import dev.mars.apex.core.config.yaml.YamlRulesEngineService;
import dev.mars.apex.core.engine.config.RulesEngine;
import dev.mars.apex.core.engine.config.RulesEngineConfiguration;
import dev.mars.apex.core.engine.model.RuleResult;
import dev.mars.apex.core.config.yaml.YamlConfigurationException;
import dev.mars.apex.core.service.enrichment.EnrichmentService;
import dev.mars.apex.core.service.engine.ExpressionEvaluatorService;
import dev.mars.apex.core.service.lookup.LookupServiceRegistry;
import dev.mars.apex.core.service.error.ErrorRecoveryService;
import dev.mars.apex.core.service.monitoring.RulePerformanceMonitor;
import dev.mars.apex.core.cache.ApexCacheManager;
import dev.mars.apex.core.service.data.external.database.JdbcTemplateFactory;
import org.springframework.expression.spel.standard.SpelExpressionParser;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Base test class for all demo tests.
 * Provides common APEX service setup and validation utilities.
 */
public abstract class DemoTestBase {

    protected static final Logger logger = LoggerFactory.getLogger(DemoTestBase.class);

    // Real APEX services for testing
    protected YamlConfigurationLoader yamlLoader;
    protected EnrichmentService enrichmentService;
    protected LookupServiceRegistry serviceRegistry;
    protected ExpressionEvaluatorService expressionEvaluator;
    protected YamlRulesEngineService rulesEngineService;
    protected RulesEngineConfiguration rulesEngineConfiguration;

    @BeforeEach
    public void setUp() {
        logger.info("Setting up APEX services for testing...");

        // Clear cache and reset statistics to ensure test isolation
        // This prevents cache state from previous tests from affecting current test
        ApexCacheManager cacheManager = ApexCacheManager.getInstance();
        cacheManager.clearAll();

        // Reset statistics for all cache scopes to ensure clean state
        cacheManager.getAllStatistics().values().forEach(stats -> stats.reset());
        logger.info("Cache cleared and statistics reset for test isolation");

        // Initialize real APEX services
        this.yamlLoader = new YamlConfigurationLoader();
        this.serviceRegistry = new LookupServiceRegistry();
        this.expressionEvaluator = new ExpressionEvaluatorService();
        this.enrichmentService = new EnrichmentService(serviceRegistry, expressionEvaluator);
        this.rulesEngineService = new YamlRulesEngineService();
        this.rulesEngineConfiguration = new RulesEngineConfiguration();

        logger.info("APEX services initialized successfully");
    }

    @AfterEach
    public void tearDown() {
        logger.info("Cleaning up APEX services after test...");

        // Clear JDBC DataSource cache to ensure database connections are properly closed
        try {
            JdbcTemplateFactory.clearCache();
            logger.info("JDBC DataSource cache cleared for test isolation");
        } catch (Exception e) {
            logger.warn("Error clearing JDBC DataSource cache", e);
        }

        // Shutdown H2 database to release locks and close connections
        try {
            // Execute H2 SHUTDOWN command to properly close the database
            java.sql.Connection conn = java.sql.DriverManager.getConnection(
                "jdbc:h2:./target/test/db;DB_CLOSE_DELAY=-1;MODE=PostgreSQL", "sa", "");
            java.sql.Statement stmt = conn.createStatement();
            stmt.execute("SHUTDOWN");
            stmt.close();
            conn.close();
            logger.info("H2 database shutdown completed for test isolation");
        } catch (Exception e) {
            logger.debug("H2 database shutdown (expected if not connected): {}", e.getMessage());
        }

        // Clean up H2 database files to prevent persistence between tests
        try {
            File dbFile = new File("./target/test/db.mv.db");
            File dbTraceFile = new File("./target/test/db.trace.db");
            if (dbFile.exists() && dbFile.delete()) {
                logger.info("H2 database file cleaned up for test isolation");
            }
            if (dbTraceFile.exists() && dbTraceFile.delete()) {
                logger.info("H2 trace file cleaned up for test isolation");
            }
        } catch (Exception e) {
            logger.warn("Error cleaning up H2 database files", e);
        }

        // Reset the cache manager singleton to ensure complete isolation between tests
        // This is more thorough than just clearing cache entries and statistics
        ApexCacheManager cacheManager = ApexCacheManager.getInstance();
        cacheManager.shutdown();
        ApexCacheManager.resetInstance();
        logger.info("Cache manager singleton reset for test isolation");
    }

    /**
     * Test that APEX services are properly initialized.
     */
    @Test
    public void testApexServicesInitialization() {
        assertNotNull(yamlLoader, "YamlConfigurationLoader should be initialized");
        assertNotNull(serviceRegistry, "LookupServiceRegistry should be initialized");
        assertNotNull(expressionEvaluator, "ExpressionEvaluatorService should be initialized");
        assertNotNull(enrichmentService, "EnrichmentService should be initialized");

        logger.info("** All APEX services properly initialized");
    }

    /**
     * Utility method to load and validate YAML configuration.
     */
    protected YamlRuleConfiguration loadAndValidateYaml(String yamlPath) {
        try {
            logger.info("Loading YAML configuration: {}", yamlPath);
            YamlRuleConfiguration config = yamlLoader.loadFromClasspath(yamlPath);

            assertNotNull(config, "YAML configuration should not be null");
            logger.info("** YAML configuration loaded successfully: {}", yamlPath);

            return config;
        } catch (Exception e) {
            logger.error("** Failed to load YAML configuration: {}", yamlPath, e);
            fail("Failed to load YAML configuration: " + yamlPath + " - " + e.getMessage());
            return null;
        }
    }

    /**
     * Safely load YAML configuration from file path with proper error handling.
     * Returns RuleResult with error details instead of throwing exceptions.
     * This follows APEX's error handling architecture.
     */
    protected RuleResult safeLoadYamlConfiguration(String filePath) {
        try {
            logger.info("Loading YAML configuration from file: {}", filePath);
            YamlRuleConfiguration config = yamlLoader.loadFromFile(filePath);

            if (config == null) {
                List<String> failureMessages = new ArrayList<>();
                failureMessages.add("YAML configuration loaded but is null: " + filePath);
                return RuleResult.evaluationFailure(failureMessages, new HashMap<String, Object>(),
                    "configuration-loading", "Configuration loading returned null");
            }

            logger.info("** YAML configuration loaded successfully: {}", filePath);
            return RuleResult.evaluationSuccess(new HashMap<String, Object>(), "configuration-loading", "Configuration loaded successfully");

        } catch (YamlConfigurationException e) {
            logger.error("Configuration loading FAILED for file: {} - {}", filePath, e.getMessage());
            List<String> failureMessages = new ArrayList<>();
            failureMessages.add("CRITICAL ERROR: Failed to load configuration from file: " + filePath);
            failureMessages.add("Error details: " + e.getMessage());

            return RuleResult.evaluationFailure(failureMessages, new HashMap<String, Object>(),
                "configuration-loading", "CRITICAL: Configuration file loading failed: " + e.getMessage());

        } catch (Exception e) {
            logger.error("Unexpected error loading YAML configuration: {}", e.getMessage(), e);
            List<String> failureMessages = new ArrayList<>();
            failureMessages.add("Unexpected error loading configuration from file: " + filePath);
            failureMessages.add("Error: " + e.getMessage());

            return RuleResult.evaluationFailure(failureMessages, new HashMap<String, Object>(),
                "configuration-loading", "Unexpected configuration loading error: " + e.getMessage());
        }
    }

    /**
     * Utility method to test enrichment processing.
     */
    protected Object testEnrichment(YamlRuleConfiguration config, Map<String, Object> testData) {
        try {
            logger.info("Testing enrichment with config and test data...");
            Object result = enrichmentService.enrichObject(config, testData);

            assertNotNull(result, "Enrichment result should not be null");
            logger.info("** Enrichment processing successful");

            return result;
        } catch (Exception e) {
            logger.error("** Enrichment processing failed", e);
            fail("Enrichment processing failed: " + e.getMessage());
            return null;
        }
    }

    /**
     * Utility method to create RulesEngine for complete evaluation workflow.
     */
    protected RulesEngine createRulesEngine() {
        try {
            logger.info("Creating RulesEngine with EnrichmentService...");
            // Use full constructor to pass EnrichmentService for enrichment processing
            RulesEngine engine = new RulesEngine(
                rulesEngineConfiguration,
                new SpelExpressionParser(),
                new ErrorRecoveryService(),
                new RulePerformanceMonitor(),
                enrichmentService
            );

            assertNotNull(engine, "RulesEngine should not be null");
            logger.info("** RulesEngine created successfully");

            return engine;
        } catch (Exception e) {
            logger.error("** RulesEngine creation failed", e);
            fail("RulesEngine creation failed: " + e.getMessage());
            return null;
        }
    }

    /**
     * Utility method to test complete APEX evaluation workflow.
     */
    protected RuleResult testEvaluation(YamlRuleConfiguration config, Map<String, Object> testData) {
        try {
            logger.info("Testing complete APEX evaluation with config and test data...");
            RulesEngine engine = createRulesEngine();
            RuleResult result = engine.evaluate(config, testData);

            assertNotNull(result, "RuleResult should not be null");
            logger.info("** APEX evaluation successful");

            return result;
        } catch (Exception e) {
            logger.error("** APEX evaluation failed", e);
            fail("APEX evaluation failed: " + e.getMessage());
            return null;
        }
    }

    /**
     * Merge multiple YAML files into a single YamlRuleConfiguration for enrichment execution.
     * Mirrors YamlRulesEngineService.createRulesEngineFromMultipleFiles merge behavior
     * and additionally merges enrichment-groups. First metadata encountered is retained.
     */
    protected YamlRuleConfiguration mergeYamlConfigsForEnrichment(String... filePaths) throws YamlConfigurationException {
        YamlRuleConfiguration merged = new YamlRuleConfiguration();
        for (String filePath : filePaths) {
            YamlRuleConfiguration part = yamlLoader.loadFromFileWithoutValidation(filePath);
            mergeYamlForEnrichment(merged, part);
        }
        yamlLoader.processReferencesAndValidate(merged);
        return merged;
    }

    // Local merge helper for tests (replicates core merge + enrichment-groups)
    private void mergeYamlForEnrichment(YamlRuleConfiguration target, YamlRuleConfiguration source) {
        // Metadata: prefer target if already set (first wins)
        if (target.getMetadata() == null && source.getMetadata() != null) {
            target.setMetadata(source.getMetadata());
        }
        // Data sources
        if (source.getDataSources() != null) {
            if (target.getDataSources() == null) target.setDataSources(new java.util.ArrayList<>());
            target.getDataSources().addAll(source.getDataSources());
        }
        // Data source refs
        if (source.getDataSourceRefs() != null) {
            if (target.getDataSourceRefs() == null) target.setDataSourceRefs(new java.util.ArrayList<>());
            target.getDataSourceRefs().addAll(source.getDataSourceRefs());
        }
        // Rule refs
        if (source.getRuleRefs() != null) {
            if (target.getRuleRefs() == null) target.setRuleRefs(new java.util.ArrayList<>());
            target.getRuleRefs().addAll(source.getRuleRefs());
        }
        // Data sinks
        if (source.getDataSinks() != null) {
            if (target.getDataSinks() == null) target.setDataSinks(new java.util.ArrayList<>());
            target.getDataSinks().addAll(source.getDataSinks());
        }
        // Categories
        if (source.getCategories() != null) {
            if (target.getCategories() == null) target.setCategories(new java.util.ArrayList<>());
            target.getCategories().addAll(source.getCategories());
        }
        // Rules
        if (source.getRules() != null) {
            if (target.getRules() == null) target.setRules(new java.util.ArrayList<>());
            target.getRules().addAll(source.getRules());
        }
        // Rule groups
        if (source.getRuleGroups() != null) {
            if (target.getRuleGroups() == null) target.setRuleGroups(new java.util.ArrayList<>());
            target.getRuleGroups().addAll(source.getRuleGroups());
        }
        // Enrichments
        if (source.getEnrichments() != null) {
            if (target.getEnrichments() == null) target.setEnrichments(new java.util.ArrayList<>());
            target.getEnrichments().addAll(source.getEnrichments());
        }
        // Enrichment groups (additional to core merge)
        if (source.getEnrichmentGroups() != null) {
            if (target.getEnrichmentGroups() == null) target.setEnrichmentGroups(new java.util.ArrayList<>());
            target.getEnrichmentGroups().addAll(source.getEnrichmentGroups());
        }
        // Rule chains
        if (source.getRuleChains() != null) {
            if (target.getRuleChains() == null) target.setRuleChains(new java.util.ArrayList<>());
            target.getRuleChains().addAll(source.getRuleChains());
        }
    }

    /**
     * Create sample test data for demos.
     */
    protected Map<String, Object> createSampleTestData() {
        Map<String, Object> testData = new HashMap<>();
        testData.put("testId", "TEST001");
        testData.put("testType", "demo-validation");
        testData.put("timestamp", System.currentTimeMillis());
        return testData;
    }

    /**
     * Update REST API base URL in YAML configuration.
     * This method updates the base-url in the first REST API data source found.
     */
    protected void updateRestApiBaseUrl(YamlRuleConfiguration config, String newBaseUrl) {
        if (config == null || config.getDataSources() == null) {
            logger.warn("No data sources found in configuration");
            return;
        }

        config.getDataSources().stream()
                .filter(ds -> "rest-api".equals(ds.getType()))
                .findFirst()
                .ifPresent(ds -> {
                    if (ds.getConnection() != null) {
                        ds.getConnection().put("base-url", newBaseUrl);
                        logger.info("Updated REST API base URL to: {}", newBaseUrl);
                    } else {
                        logger.warn("No connection configuration found in REST API data source");
                    }
                });
    }
}
