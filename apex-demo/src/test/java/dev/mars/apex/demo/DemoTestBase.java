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

import dev.mars.apex.core.config.yaml.YamlConfigurationLoader;
import dev.mars.apex.core.config.yaml.YamlRuleConfiguration;
import dev.mars.apex.core.config.yaml.YamlRulesEngineService;
import dev.mars.apex.core.engine.config.RulesEngine;
import dev.mars.apex.core.engine.config.RulesEngineConfiguration;
import dev.mars.apex.core.engine.model.RuleResult;
import dev.mars.apex.core.config.yaml.YamlConfigurationException;
import dev.mars.apex.core.service.enrichment.EnrichmentService;
import dev.mars.apex.core.service.engine.ExpressionEvaluatorService;
import dev.mars.apex.core.service.error.ErrorRecoveryService;
import dev.mars.apex.core.service.lookup.LookupServiceRegistry;
import dev.mars.apex.core.service.monitoring.RulePerformanceMonitor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.springframework.expression.spel.standard.SpelExpressionParser;

import java.util.HashMap;
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

        // Initialize real APEX services
        this.yamlLoader = new YamlConfigurationLoader();
        this.serviceRegistry = new LookupServiceRegistry();
        this.expressionEvaluator = new ExpressionEvaluatorService();
        this.enrichmentService = new EnrichmentService(serviceRegistry, expressionEvaluator);
        this.rulesEngineService = new YamlRulesEngineService();
        this.rulesEngineConfiguration = new RulesEngineConfiguration();

        logger.info("APEX services initialized successfully");
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
                return RuleResult.evaluationFailure(failureMessages, new HashMap<>(),
                    "configuration-loading", "Configuration loading returned null");
            }

            logger.info("** YAML configuration loaded successfully: {}", filePath);
            return RuleResult.evaluationSuccess(new HashMap<>(), "configuration-loading", "Configuration loaded successfully");

        } catch (YamlConfigurationException e) {
            logger.error("Configuration loading FAILED for file: {} - {}", filePath, e.getMessage());
            List<String> failureMessages = new ArrayList<>();
            failureMessages.add("CRITICAL ERROR: Failed to load configuration from file: " + filePath);
            failureMessages.add("Error details: " + e.getMessage());

            return RuleResult.evaluationFailure(failureMessages, new HashMap<>(),
                "configuration-loading", "CRITICAL: Configuration file loading failed: " + e.getMessage());

        } catch (Exception e) {
            logger.error("Unexpected error loading YAML configuration: {}", e.getMessage(), e);
            List<String> failureMessages = new ArrayList<>();
            failureMessages.add("Unexpected error loading configuration from file: " + filePath);
            failureMessages.add("Error: " + e.getMessage());

            return RuleResult.evaluationFailure(failureMessages, new HashMap<>(),
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
            RulesEngine engine = new RulesEngine(rulesEngineConfiguration, new SpelExpressionParser(),
                    new ErrorRecoveryService(), new RulePerformanceMonitor(), enrichmentService);

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
