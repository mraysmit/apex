package dev.mars.apex.demo;

import dev.mars.apex.core.config.yaml.YamlConfigurationLoader;
import dev.mars.apex.core.config.yaml.YamlRuleConfiguration;
import dev.mars.apex.core.service.enrichment.EnrichmentService;
import dev.mars.apex.core.service.lookup.LookupServiceRegistry;
import dev.mars.apex.core.service.engine.ExpressionEvaluatorService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    
    @BeforeEach
    void setUp() {
        logger.info("Setting up APEX services for testing...");
        
        // Initialize real APEX services
        this.yamlLoader = new YamlConfigurationLoader();
        this.serviceRegistry = new LookupServiceRegistry();
        this.expressionEvaluator = new ExpressionEvaluatorService();
        this.enrichmentService = new EnrichmentService(serviceRegistry, expressionEvaluator);
        
        logger.info("APEX services initialized successfully");
    }
    
    /**
     * Test that APEX services are properly initialized.
     */
    @Test
    void testApexServicesInitialization() {
        assertNotNull(yamlLoader, "YamlConfigurationLoader should be initialized");
        assertNotNull(serviceRegistry, "LookupServiceRegistry should be initialized");
        assertNotNull(expressionEvaluator, "ExpressionEvaluatorService should be initialized");
        assertNotNull(enrichmentService, "EnrichmentService should be initialized");
        
        logger.info("✅ All APEX services properly initialized");
    }
    
    /**
     * Utility method to load and validate YAML configuration.
     */
    protected YamlRuleConfiguration loadAndValidateYaml(String yamlPath) {
        try {
            logger.info("Loading YAML configuration: {}", yamlPath);
            YamlRuleConfiguration config = yamlLoader.loadFromClasspath(yamlPath);
            
            assertNotNull(config, "YAML configuration should not be null");
            logger.info("✅ YAML configuration loaded successfully: {}", yamlPath);
            
            return config;
        } catch (Exception e) {
            logger.error("❌ Failed to load YAML configuration: {}", yamlPath, e);
            fail("Failed to load YAML configuration: " + yamlPath + " - " + e.getMessage());
            return null;
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
            logger.info("✅ Enrichment processing successful");
            
            return result;
        } catch (Exception e) {
            logger.error("❌ Enrichment processing failed", e);
            fail("Enrichment processing failed: " + e.getMessage());
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
}
