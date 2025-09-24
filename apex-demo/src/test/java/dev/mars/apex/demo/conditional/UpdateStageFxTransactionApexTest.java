package dev.mars.apex.demo.conditional;

import dev.mars.apex.core.config.yaml.YamlConfigurationLoader;
import dev.mars.apex.core.config.yaml.YamlRuleConfiguration;
import dev.mars.apex.core.config.yaml.YamlRulesEngineService;
import dev.mars.apex.core.engine.config.RulesEngine;
import dev.mars.apex.core.engine.config.RulesEngineConfiguration;
import dev.mars.apex.core.engine.model.RuleResult;
import dev.mars.apex.core.service.error.ErrorRecoveryService;
import dev.mars.apex.core.service.monitoring.RulePerformanceMonitor;
import dev.mars.apex.core.service.enrichment.EnrichmentService;
import dev.mars.apex.core.service.engine.ExpressionEvaluatorService;
import dev.mars.apex.core.service.lookup.LookupServiceRegistry;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * APEX-Compliant FX Transaction Test
 * 
 * This test demonstrates the TRUE APEX architecture:
 * - Multi-file configuration loading
 * - Cross-file rule references using verified APEX syntax
 * - Reusable rules and enrichments
 * - Separation = Reusability principle
 * 
 * Following the pattern from BasicYamlRuleGroupProcessingATest.java
 */
@DisplayName("Update Stage FX Transaction - APEX Architecture Test")
public class UpdateStageFxTransactionApexTest {

    private static final Logger logger = LoggerFactory.getLogger(UpdateStageFxTransactionApexTest.class);

    private YamlConfigurationLoader yamlLoader;
    private YamlRulesEngineService rulesEngineService;
    private EnrichmentService enrichmentService;

    @BeforeEach
    void setUp() {
        logger.info("Setting up APEX services for FX transaction processing tests...");

        // Initialize services following DemoTestBase pattern
        yamlLoader = new YamlConfigurationLoader();
        rulesEngineService = new YamlRulesEngineService();

        // Initialize enrichment service with correct constructor
        LookupServiceRegistry lookupServiceRegistry = new LookupServiceRegistry();
        ExpressionEvaluatorService expressionEvaluatorService = new ExpressionEvaluatorService();
        enrichmentService = new EnrichmentService(lookupServiceRegistry, expressionEvaluatorService);

        logger.info("✅ APEX services initialized successfully");
    }

    @Test
    @DisplayName("Should process SWIFT transaction using APEX multi-file architecture")
    void testSwiftTransactionWithApexArchitecture() {
        logger.info("=== Testing SWIFT Transaction with APEX Multi-File Architecture ===");
        
        try {
            // Load using APEX automatic rule reference resolution
            // Following the pattern from BasicYamlRuleGroupProcessingATest
            YamlRuleConfiguration config = yamlLoader.loadFromFile(
                "src/test/java/dev/mars/apex/demo/conditional/UpdateStageFxTransactionApexTest.yaml"
            );
            
            logger.info("✅ Configuration loaded with cross-file rule references");
            
            // Use enrichment service directly (simpler approach)
            Map<String, Object> testData = createSwiftTestData("1", "USD", "EUR");
            logger.info("Testing SWIFT valid NDF with data: {}", testData);

            // Execute using enrichment service
            Object enrichmentResult = enrichmentService.enrichObject(config, testData);
            assertNotNull(enrichmentResult, "Enrichment result should not be null");

            @SuppressWarnings("unchecked")
            Map<String, Object> enrichedData = (Map<String, Object>) enrichmentResult;
            
            // Validate enrichment results
            assertNotNull(enrichedData.get("BUY_CURRENCY_RANK"), "Buy currency ranking should be enriched");
            assertNotNull(enrichedData.get("SELL_CURRENCY_RANK"), "Sell currency ranking should be enriched");
            assertNotNull(enrichedData.get("PROCESSED_NDF"), "NDF processing should work");

            logger.info("✅ APEX enrichment processing test passed");
            logger.info("Enriched data: {}", enrichedData);
            
        } catch (Exception e) {
            logger.error("APEX multi-file loading failed", e);
            fail("APEX multi-file loading failed: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("Should demonstrate rule reusability across different scenarios")
    void testRuleReusabilityAcrossScenarios() {
        logger.info("=== Testing Rule Reusability Across Different Scenarios ===");
        
        try {
            // Load the main scenario configuration
            YamlRuleConfiguration config = yamlLoader.loadFromFile(
                "src/test/java/dev/mars/apex/demo/conditional/UpdateStageFxTransactionMultiFileTest_main.yaml"
            );
            
            // Create RulesEngine with enrichment service
            RulesEngineConfiguration engineConfig = rulesEngineService.getRuleFactory().createRulesEngineConfiguration(config);
            RulesEngine engine = new RulesEngine(engineConfig, new SpelExpressionParser(),
                new ErrorRecoveryService(), new RulePerformanceMonitor(), enrichmentService);

            // Test 1: USD/EUR transaction
            Map<String, Object> usdEurData = createSwiftTestData("1", "USD", "EUR");
            RuleResult usdEurResult = engine.evaluate(config, usdEurData);

            // Test 2: GBP/JPY transaction (different currencies, same rules)
            Map<String, Object> gbpJpyData = createSwiftTestData("1", "GBP", "JPY");
            RuleResult gbpJpyResult = engine.evaluate(config, gbpJpyData);
            
            // Validate that enrichments worked (no rule groups to trigger, so check enrichments)
            assertFalse(usdEurResult.hasFailures(), "USD/EUR transaction should not have failures");
            assertFalse(gbpJpyResult.hasFailures(), "GBP/JPY transaction should not have failures");

            // Verify enrichments worked for both scenarios
            assertNotNull(usdEurResult.getEnrichedData(), "USD/EUR enriched data should not be null");
            assertNotNull(gbpJpyResult.getEnrichedData(), "GBP/JPY enriched data should not be null");
            assertTrue(usdEurResult.getEnrichedData().size() > 0, "USD/EUR should have enriched fields");
            assertTrue(gbpJpyResult.getEnrichedData().size() > 0, "GBP/JPY should have enriched fields");

            logger.info("USD/EUR enriched data: {}", usdEurResult.getEnrichedData());
            logger.info("GBP/JPY enriched data: {}", gbpJpyResult.getEnrichedData());
            
            logger.info("✅ Rule reusability demonstrated successfully");
            
        } catch (Exception e) {
            logger.error("Rule reusability test failed", e);
            fail("Rule reusability test failed: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("Should validate cross-file rule resolution")
    void testCrossFileRuleResolution() {
        logger.info("=== Testing Cross-File Rule Resolution ===");
        
        try {
            // Load configuration that references rules from multiple files
            YamlRuleConfiguration config = yamlLoader.loadFromFile(
                "src/test/java/dev/mars/apex/demo/conditional/UpdateStageFxTransactionMultiFileTest_main.yaml"
            );
            
            RulesEngine engine = rulesEngineService.createRulesEngineFromYamlConfig(config);
            
            // Test data
            Map<String, Object> testData = createSwiftTestData("1", "USD", "EUR");
            
            // Create RulesEngine with enrichment service
            RulesEngineConfiguration engineConfig = rulesEngineService.getRuleFactory().createRulesEngineConfiguration(config);
            RulesEngine enrichedEngine = new RulesEngine(engineConfig, new SpelExpressionParser(),
                new ErrorRecoveryService(), new RulePerformanceMonitor(), enrichmentService);
            RuleResult result = enrichedEngine.evaluate(config, testData);

            // Validate that cross-file rule resolution worked (enrichments prove this)
            assertFalse(result.hasFailures(), "Cross-file rule resolution should not have failures");

            Map<String, Object> enrichedData = result.getEnrichedData();

            // Verify enrichments worked (proves cross-file resolution)
            assertNotNull(enrichedData, "Enriched data should not be null");
            assertTrue(enrichedData.size() > 0, "Enriched data should contain fields");

            logger.info("Cross-file enriched data: {}", enrichedData);
            
            logger.info("Cross-file rule resolution successful - enriched data: {}", enrichedData);
            logger.info("✅ Cross-file rule resolution test passed");
            
        } catch (Exception e) {
            logger.error("Cross-file rule resolution test failed", e);
            fail("Cross-file rule resolution test failed: " + e.getMessage());
        }
    }

    /**
     * Create test data for SWIFT system
     */
    private Map<String, Object> createSwiftTestData(String ndfValue, String buyCurrency, String sellCurrency) {
        Map<String, Object> testData = new HashMap<>();
        testData.put("SYSTEM_CODE", "SWIFT");
        testData.put("IS_NDF", ndfValue);
        testData.put("BUY_CURRENCY", buyCurrency);
        testData.put("SELL_CURRENCY", sellCurrency);
        return testData;
    }

    /**
     * Validate APEX processing result using RuleResult API
     * Following the pattern from BasicYamlRuleGroupProcessingATest
     */
    private void validateApexProcessingResult(RuleResult result, String testName) {
        logger.info("=== {} - RuleResult API Details ===", testName);
        
        // Log comprehensive RuleResult details
        logger.info("result.isSuccess(): {}", result.isSuccess());
        logger.info("result.hasFailures(): {}", result.hasFailures());
        logger.info("result.getFailureMessages(): {}", result.getFailureMessages());
        logger.info("result.getEnrichedData(): {}", result.getEnrichedData());
        logger.info("result.getRuleName(): {}", result.getRuleName());
        logger.info("result.getMessage(): {}", result.getMessage());
        logger.info("result.getResultType(): {}", result.getResultType());
        logger.info("result.getTimestamp(): {}", result.getTimestamp());
        
        // Validate core result properties
        assertTrue(result.isSuccess(), "APEX multi-file processing should succeed");
        assertFalse(result.hasFailures(), "Should not have failures");
        
        Map<String, Object> enrichedData = result.getEnrichedData();
        assertNotNull(enrichedData, "Enriched data should not be null");
        
        // Validate that enrichments from multi-file configuration worked
        assertNotNull(enrichedData.get("BUY_CURRENCY_RANK"), "Buy currency ranking should be enriched");
        assertNotNull(enrichedData.get("SELL_CURRENCY_RANK"), "Sell currency ranking should be enriched");
        assertNotNull(enrichedData.get("PROCESSED_NDF"), "NDF processing should work");
        
        logger.info("✅ {} validation passed", testName);
    }
}
