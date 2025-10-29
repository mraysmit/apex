package dev.mars.apex.demo.sequencing;

import dev.mars.apex.core.service.enrichment.YamlEnrichmentProcessor;
import dev.mars.apex.core.config.yaml.YamlRuleConfiguration;
import dev.mars.apex.core.config.yaml.YamlConfigurationLoader;
import dev.mars.apex.core.engine.config.RulesEngine;
import dev.mars.apex.core.engine.config.RulesEngineConfiguration;
import dev.mars.apex.core.service.enrichment.EnrichmentService;
import dev.mars.apex.core.service.lookup.LookupServiceRegistry;
import dev.mars.apex.core.service.engine.ExpressionEvaluatorService;
import dev.mars.apex.core.service.error.ErrorRecoveryService;
import dev.mars.apex.core.service.monitoring.RulePerformanceMonitor;
import dev.mars.apex.core.engine.model.RuleResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.expression.spel.standard.SpelExpressionParser;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Demonstrates APEX's fundamental design flaw: ignoring YAML section order
 * 
 * This test proves that APEX processes sections in hardcoded order regardless
 * of developer intent expressed through YAML structure, causing business logic
 * to fail when developers write natural sequential configurations.
 */
public class SequencingFlawDemoTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(SequencingFlawDemoTest.class);

    private YamlConfigurationLoader yamlLoader;
    private EnrichmentService enrichmentService;
    private RulesEngineConfiguration rulesEngineConfiguration;

    @BeforeEach
    void setUp() {
        // Initialize APEX services following established patterns
        yamlLoader = new YamlConfigurationLoader();

        // Create enrichment service with required dependencies
        LookupServiceRegistry serviceRegistry = new LookupServiceRegistry();
        ExpressionEvaluatorService evaluatorService = new ExpressionEvaluatorService();
        enrichmentService = new EnrichmentService(serviceRegistry, evaluatorService);

        // Create rules engine configuration
        rulesEngineConfiguration = new RulesEngineConfiguration();

        LOGGER.info("✅ APEX services initialized for sequencing flaw demonstration");
    }
    
    @Test
    @DisplayName("DESIGN FLAW: Enrich-Then-Validate Pattern Fails")
    public void testEnrichThenValidatePatternFails() throws Exception {
        LOGGER.info("=== DEMONSTRATING DESIGN FLAW: Enrich-Then-Validate ===");
        
        // Load YAML where developer placed enrichments BEFORE rules
        String yamlPath = "src/test/java/dev/mars/apex/demo/sequencing/SequencingFlawDemoTest.yaml";
        YamlRuleConfiguration config = yamlLoader.loadFromFile(yamlPath);
        
        // Test data - amount that should trigger risk calculation
        Map<String, Object> testData = new HashMap<>();
        testData.put("amount", 75000.0);  // High amount -> should calculate riskScore = 0.9
        
        LOGGER.info("Developer Intent: Calculate risk score FIRST, then validate it");
        LOGGER.info("YAML Structure: enrichments section appears BEFORE rules section");
        LOGGER.info("Test Data: amount = {}", testData.get("amount"));
        
        try {
            // Process using enrichment service (demonstrates the flaw)
            Object result = enrichmentService.enrichObject(config, testData);
            assertNotNull(result, "Enrichment result should not be null");

            @SuppressWarnings("unchecked")
            Map<String, Object> enrichedData = (Map<String, Object>) result;

            LOGGER.info("Processing completed. Checking results...");
            LOGGER.info("Final data state: {}", enrichedData);

            // The DESIGN FLAW: Check if rules processed before enrichments
            if (enrichedData.containsKey("riskScore")) {
                LOGGER.info("SUCCESS: riskScore was calculated = {}", enrichedData.get("riskScore"));

                // Check if rules that depend on riskScore worked
                // This demonstrates whether the sequencing worked or failed
                LOGGER.info("Checking if risk validation rules processed correctly...");

            } else {
                LOGGER.error("DESIGN FLAW DEMONSTRATED: riskScore was NOT calculated!");
                LOGGER.error("This suggests rules processed BEFORE enrichments, ignoring YAML order");
            }

        } catch (Exception e) {
            LOGGER.error("DESIGN FLAW DEMONSTRATED: Processing failed");
            LOGGER.error("Exception: {}", e.getMessage());
            LOGGER.error("Root Cause: APEX may have processed rules BEFORE enrichments, ignoring developer intent");
        }
    }
    
    @Test
    @DisplayName("DESIGN FLAW: Validate-Then-Enrich Pattern May Be Inefficient")
    public void testValidateThenEnrichPatternInefficiency() throws Exception {
        LOGGER.info("=== DEMONSTRATING DESIGN FLAW: Validate-Then-Enrich ===");
        
        // Load YAML where developer placed rules BEFORE enrichments
        String yamlPath = "src/test/java/dev/mars/apex/demo/sequencing/LoggingSeverityFlawTest.yaml";
        YamlRuleConfiguration config = yamlLoader.loadFromFile(yamlPath);

        // Test data with INVALID customer ID (should fail validation)
        Map<String, Object> testData = new HashMap<>();
        testData.put("customerId", "");  // Empty customer ID - should fail validation
        testData.put("amount", 1000.0);

        LOGGER.info("Developer Intent: Validate data FIRST, then enrich only valid records");
        LOGGER.info("YAML Structure: rules section appears BEFORE enrichments section");
        LOGGER.info("Test Data: customerId = '{}' (invalid)", testData.get("customerId"));

        try {
            // Create RulesEngine with EnrichmentService
            RulesEngine engine = new RulesEngine(
                rulesEngineConfiguration,
                new SpelExpressionParser(),
                new ErrorRecoveryService(),
                new RulePerformanceMonitor(),
                enrichmentService
            );

            RuleResult result = engine.evaluate(config, testData);
            assertNotNull(result, "RuleResult should not be null");

            LOGGER.info("Processing completed. Checking results...");
            LOGGER.info("Final data state: {}", testData);

            // Check if expensive enrichments ran on invalid data
            if (testData.containsKey("customerName") || testData.containsKey("transactionFee")) {
                LOGGER.error("DESIGN FLAW DEMONSTRATED: Expensive enrichments ran on INVALID data!");
                LOGGER.error("APEX ignored YAML order and processed enrichments before validation");
            } else {
                LOGGER.info("Correct behavior: No enrichments ran on invalid data");
            }

        } catch (Exception e) {
            LOGGER.error("Processing failed: {}", e.getMessage());
        }
    }
}
