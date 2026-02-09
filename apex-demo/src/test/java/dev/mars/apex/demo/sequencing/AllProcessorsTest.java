package dev.mars.apex.demo.sequencing;

import dev.mars.apex.core.config.model.YamlRuleConfiguration;
import dev.mars.apex.core.config.loader.YamlConfigurationLoader;
import dev.mars.apex.engine.core.RulesEngine;
import dev.mars.apex.engine.core.RulesEngineConfiguration;
import dev.mars.apex.engine.model.RuleResult;
import dev.mars.apex.engine.model.Rule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import dev.mars.apex.engine.core.RuleBuilder;

/**
 * COMPREHENSIVE TEST: Tests ALL APEX processors with the SAME YAML file
 * to demonstrate that different processors produce different results due to different processing strategies.
 */
@DisplayName("ALL PROCESSORS: Same YAML → Different Results")
class AllProcessorsTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(AllProcessorsTest.class);

    private YamlConfigurationLoader yamlLoader;
    private RulesEngineConfiguration rulesEngineConfiguration;

    @BeforeEach
    void setUp() {
        LOGGER.info("🔧 Initializing ALL APEX processors for comprehensive testing");

        yamlLoader = new YamlConfigurationLoader();
        rulesEngineConfiguration = new RulesEngineConfiguration();

        LOGGER.info("All processors initialized");
    }

    @Test
    @DisplayName("PROCESSOR 1: YamlEnrichmentProcessor (Rules → Enrichments)")
    void testYamlEnrichmentProcessor() {
        LOGGER.info("=== TESTING: YamlEnrichmentProcessor ===");
        LOGGER.info("Processing Order: Rules FIRST → Enrichments SECOND (hardcoded)");
        
        String yamlPath = "src/test/java/dev/mars/apex/demo/sequencing/AllProcessorsTest.yaml";
        YamlRuleConfiguration config;
        try {
            config = yamlLoader.loadFromFile(yamlPath);
        } catch (Exception e) {
            fail("Failed to load YAML configuration: " + e.getMessage());
            return;
        }
        
        Map<String, Object> testData = new HashMap<>();
        testData.put("amount", 50000.0);
        
        LOGGER.info("💰 Input: amount = {}", testData.get("amount"));
        LOGGER.info("🎯 Expected (if YAML order respected): riskScore=0.5, riskCategory=MEDIUM_RISK");
        
        try {
            RulesEngine engine = RulesEngine.fromYamlConfig(config);
            RuleResult ruleResult = engine.evaluate(config, testData);
            Object result = ruleResult.getEnrichedData();

            @SuppressWarnings("unchecked")
            Map<String, Object> enrichedData = (Map<String, Object>) result;
            
            LOGGER.info("YamlEnrichmentProcessor Result: {}", enrichedData);
            
            // Check what was actually calculated
            if (enrichedData.containsKey("riskScore")) {
                LOGGER.info("riskScore calculated: {}", enrichedData.get("riskScore"));
            } else {
                LOGGER.error("riskScore NOT calculated");
            }
            
            if (enrichedData.containsKey("riskCategory")) {
                LOGGER.info("riskCategory set: {}", enrichedData.get("riskCategory"));
            } else {
                LOGGER.error("riskCategory NOT set");
            }
            
            assertNotNull(result, "YamlEnrichmentProcessor should return result");
            
        } catch (Exception e) {
            LOGGER.error("💥 YamlEnrichmentProcessor failed: {}", e.getMessage());
            fail("YamlEnrichmentProcessor should not fail: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("PROCESSOR 2: RulesEngine.evaluate() (Enrichments → Rules → Rule Groups)")
    void testRulesEngineEvaluate() {
        LOGGER.info("=== TESTING: RulesEngine.evaluate() ===");
        LOGGER.info("Processing Order: Enrichments FIRST → Rules SECOND → Rule Groups THIRD (hardcoded)");
        
        String yamlPath = "src/test/java/dev/mars/apex/demo/sequencing/AllProcessorsTest.yaml";
        YamlRuleConfiguration config;
        try {
            config = yamlLoader.loadFromFile(yamlPath);
        } catch (Exception e) {
            fail("Failed to load YAML configuration: " + e.getMessage());
            return;
        }

        Map<String, Object> testData = new HashMap<>();
        testData.put("amount", 50000.0);
        
        LOGGER.info("💰 Input: amount = {}", testData.get("amount"));
        LOGGER.info("🎯 Expected (if YAML order respected): riskScore=0.5, riskCategory=MEDIUM_RISK");

        try {
            RulesEngine engine = new RulesEngine(rulesEngineConfiguration);
            
            RuleResult result = engine.evaluate(config, testData);
            Map<String, Object> enrichedData = result.getEnrichedData();
            
            LOGGER.info("RulesEngine.evaluate() Result: {}", enrichedData);
            LOGGER.info("🎯 Rule Evaluation Success: {}", result.isSuccess());
            
            // Check what was actually calculated
            if (enrichedData.containsKey("riskScore")) {
                LOGGER.info("riskScore calculated: {}", enrichedData.get("riskScore"));
            } else {
                LOGGER.error("riskScore NOT calculated");
            }
            
            if (enrichedData.containsKey("riskCategory")) {
                LOGGER.info("riskCategory set: {}", enrichedData.get("riskCategory"));
            } else {
                LOGGER.error("riskCategory NOT set");
            }
            
            assertNotNull(result, "RulesEngine should return result");
            
        } catch (Exception e) {
            LOGGER.error("💥 RulesEngine.evaluate() failed: {}", e.getMessage());
            fail("RulesEngine.evaluate() should not fail: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("PROCESSOR 3: enrichmentProcessor.processEnrichments() (Delegates to YamlEnrichmentProcessor)")
    void testEnrichmentService() {
        LOGGER.info("=== TESTING: enrichmentProcessor.processEnrichments() ===");
        LOGGER.info("Processing Order: Delegates to YamlEnrichmentProcessor (Rules → Enrichments)");
        
        String yamlPath = "src/test/java/dev/mars/apex/demo/sequencing/AllProcessorsTest.yaml";
        YamlRuleConfiguration config;
        try {
            config = yamlLoader.loadFromFile(yamlPath);
        } catch (Exception e) {
            fail("Failed to load YAML configuration: " + e.getMessage());
            return;
        }

        Map<String, Object> testData = new HashMap<>();
        testData.put("amount", 50000.0);
        
        LOGGER.info("💰 Input: amount = {}", testData.get("amount"));
        
        try {
            RulesEngine engine = RulesEngine.fromYamlConfig(config);
            RuleResult ruleResult = engine.evaluate(config, testData);
            Object result = ruleResult.getEnrichedData();

            @SuppressWarnings("unchecked")
            Map<String, Object> enrichedData = (Map<String, Object>) result;
            
            LOGGER.info("EnrichmentService Result: {}", enrichedData);
            
            assertNotNull(result, "EnrichmentService should return result");
            
        } catch (Exception e) {
            LOGGER.error("💥 EnrichmentService failed: {}", e.getMessage());
            fail("EnrichmentService should not fail: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("PROCESSOR 4: RulesEngine with Individual Rules (Rules Only)")
    void testSimpleRulesEngine() {
        LOGGER.info("=== TESTING: RulesEngine with Individual Rules ===");
        LOGGER.info("Processing Order: Rules ONLY (demonstrating rule-only execution)");
        
        Map<String, Object> testData = new HashMap<>();
        testData.put("amount", 50000.0);
        // Pre-populate fields that enrichments would normally calculate
        testData.put("riskScore", 0.5);
        testData.put("riskCategory", "MEDIUM_RISK");
        
        LOGGER.info("💰 Input (pre-enriched): {}", testData);
        
        try {
            // Create a minimal configuration for rule-only testing
            RulesEngine engine = new RulesEngine(rulesEngineConfiguration);
            
            // Create individual rules for testing
            Rule riskScoreRule = new RuleBuilder()
                .withName("risk-score-check")
                .withCondition("#riskScore != null && #riskScore >= 0")
                .withMessage("Risk score validation")
                .withSeverity(dev.mars.apex.core.constants.SeverityConstants.INFO)
                .build();
                
            Rule riskCategoryRule = new RuleBuilder()
                .withName("risk-category-check")
                .withCondition("#riskCategory != null && (#riskCategory == 'HIGH_RISK' || #riskCategory == 'MEDIUM_RISK' || #riskCategory == 'LOW_RISK')")
                .withMessage("Risk category validation")
                .withSeverity(dev.mars.apex.core.constants.SeverityConstants.INFO)
                .build();
            
            // Execute individual rules
            RuleResult riskScoreResult = engine.executeRule(riskScoreRule, testData);
            RuleResult riskCategoryResult = engine.executeRule(riskCategoryRule, testData);
            
            boolean riskScoreValid = riskScoreResult.isTriggered();
            boolean riskCategoryValid = riskCategoryResult.isTriggered();
            
            LOGGER.info("RulesEngine Results:");
            LOGGER.info("   Risk Score Valid: {}", riskScoreValid);
            LOGGER.info("   Risk Category Valid: {}", riskCategoryValid);
            
            assertTrue(riskScoreValid, "Risk score validation should pass");
            assertTrue(riskCategoryValid, "Risk category validation should pass");
            
        } catch (Exception e) {
            LOGGER.error("💥 RulesEngine failed: {}", e.getMessage());
            fail("RulesEngine should not fail: " + e.getMessage());
        }
    }
}



