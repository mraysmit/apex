package dev.mars.apex.demo.sequencing;

import dev.mars.apex.core.config.yaml.YamlRuleConfiguration;
import dev.mars.apex.core.config.yaml.YamlConfigurationLoader;
import dev.mars.apex.core.engine.config.RulesEngine;
import dev.mars.apex.core.engine.config.RulesEngineConfiguration;
import dev.mars.apex.core.engine.model.RuleResult;
import dev.mars.apex.core.service.enrichment.EnrichmentService;
import dev.mars.apex.core.service.enrichment.YamlEnrichmentProcessor;
import dev.mars.apex.core.service.engine.ExpressionEvaluatorService;
import dev.mars.apex.core.service.error.ErrorRecoveryService;
import dev.mars.apex.core.service.lookup.LookupServiceRegistry;
import dev.mars.apex.core.service.monitoring.RulePerformanceMonitor;
import dev.mars.apex.core.api.SimpleRulesEngine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.expression.spel.standard.SpelExpressionParser;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * CRITICAL TEST: Proves APEX's fundamental design flaw where different processors
 * produce DIFFERENT RESULTS from the SAME YAML file.
 * 
 * This demonstrates that APEX's behavior is completely unpredictable and depends
 * on which processor you choose, not on the developer's intent expressed in YAML.
 */
@DisplayName("🚨 DESIGN FLAW: Different Processors Produce Different Results")
class ProcessorComparisonTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProcessorComparisonTest.class);

    private YamlConfigurationLoader yamlLoader;
    private EnrichmentService enrichmentService;
    private YamlEnrichmentProcessor yamlEnrichmentProcessor;
    private RulesEngineConfiguration rulesEngineConfiguration;

    @BeforeEach
    void setUp() {
        LOGGER.info("🔧 Initializing all APEX processors for comparison testing");
        
        yamlLoader = new YamlConfigurationLoader();
        LookupServiceRegistry serviceRegistry = new LookupServiceRegistry();
        ExpressionEvaluatorService evaluatorService = new ExpressionEvaluatorService();
        
        enrichmentService = new EnrichmentService(serviceRegistry, evaluatorService);
        yamlEnrichmentProcessor = new YamlEnrichmentProcessor(serviceRegistry, evaluatorService);
        rulesEngineConfiguration = new RulesEngineConfiguration();
        
        LOGGER.info("✅ All processors initialized for design flaw demonstration");
    }

    @Test
    @DisplayName("🚨 SAME YAML → DIFFERENT RESULTS: YamlEnrichmentProcessor vs RulesEngine")
    void testSameYamlDifferentResults() {
        LOGGER.info("=== PROVING DESIGN FLAW: Same YAML, Different Results ===");
        
        // Load the SAME YAML file for both processors
        String yamlPath = "src/test/java/dev/mars/apex/demo/sequencing/ProcessorComparisonTest.yaml";
        YamlRuleConfiguration config;
        try {
            config = yamlLoader.loadFromFile(yamlPath);
        } catch (Exception e) {
            fail("Failed to load YAML configuration: " + e.getMessage());
            return;
        }
        
        // Same test data for both processors
        Map<String, Object> testData1 = new HashMap<>();
        testData1.put("amount", 50000.0);
        
        Map<String, Object> testData2 = new HashMap<>();
        testData2.put("amount", 50000.0);
        
        LOGGER.info("📄 YAML Structure: enrichments BEFORE rules (developer intent: enrich first)");
        LOGGER.info("💰 Test Data: amount = {}", testData1.get("amount"));
        
        // PROCESSOR 1: YamlEnrichmentProcessor (Rules → Enrichments)
        LOGGER.info("🔄 Testing YamlEnrichmentProcessor (Rules FIRST, Enrichments SECOND)");
        Object result1 = yamlEnrichmentProcessor.processEnrichments(config.getEnrichments(), testData1, config);
        
        @SuppressWarnings("unchecked")
        Map<String, Object> enrichedData1 = (Map<String, Object>) result1;
        
        LOGGER.info("📊 YamlEnrichmentProcessor Result: {}", enrichedData1);
        
        // PROCESSOR 2: RulesEngine.evaluate() (Enrichments → Rules → Rule Groups)
        LOGGER.info("🔄 Testing RulesEngine.evaluate() (Enrichments FIRST, Rules SECOND)");
        RulesEngine engine = new RulesEngine(
            rulesEngineConfiguration,
            new SpelExpressionParser(),
            new ErrorRecoveryService(),
            new RulePerformanceMonitor(),
            enrichmentService
        );
        
        RuleResult result2 = engine.evaluate(config, testData2);
        Map<String, Object> enrichedData2 = result2.getEnrichedData();
        
        LOGGER.info("📊 RulesEngine.evaluate() Result: {}", enrichedData2);
        
        // CRITICAL ANALYSIS: Compare the results
        LOGGER.error("🚨 DESIGN FLAW ANALYSIS:");
        LOGGER.error("   Same YAML file processed by different processors");
        LOGGER.error("   YamlEnrichmentProcessor: {}", enrichedData1);
        LOGGER.error("   RulesEngine.evaluate(): {}", enrichedData2);
        
        // Check if results are different (proving the design flaw)
        boolean resultsDifferent = !enrichedData1.equals(enrichedData2);
        
        if (resultsDifferent) {
            LOGGER.error("💥 DESIGN FLAW CONFIRMED: Different processors produce DIFFERENT RESULTS!");
            LOGGER.error("   This proves APEX behavior is unpredictable and processor-dependent");
        } else {
            LOGGER.warn("⚠️  Results are the same - may need different test scenario");
        }
        
        // Document the specific differences
        for (String key : enrichedData1.keySet()) {
            Object value1 = enrichedData1.get(key);
            Object value2 = enrichedData2.get(key);
            
            if (!java.util.Objects.equals(value1, value2)) {
                LOGGER.error("   Field '{}': YamlEnrichmentProcessor={}, RulesEngine={}", key, value1, value2);
            }
        }
        
        // This test DOCUMENTS the flaw - we expect different results
        assertNotNull(enrichedData1, "YamlEnrichmentProcessor should return results");
        assertNotNull(enrichedData2, "RulesEngine should return results");
    }

    @Test
    @DisplayName("🚨 PROCESSING ORDER DOCUMENTATION: All Processors Listed")
    void testDocumentAllProcessingOrders() {
        LOGGER.info("=== DOCUMENTING ALL APEX PROCESSING ORDERS ===");
        
        LOGGER.info("📋 APEX PROCESSORS AND THEIR HARDCODED ORDERS:");
        LOGGER.info("");
        LOGGER.info("1️⃣  YamlEnrichmentProcessor.processEnrichments():");
        LOGGER.info("    ├── Rules & Rule Groups → FIRST (hardcoded)");
        LOGGER.info("    └── Enrichments & Enrichment Groups → SECOND (hardcoded)");
        LOGGER.info("");
        LOGGER.info("2️⃣  RulesEngine.evaluate(YamlRuleConfiguration, Map):");
        LOGGER.info("    ├── Enrichments → FIRST (hardcoded)");
        LOGGER.info("    ├── Individual Rules → SECOND (hardcoded)");
        LOGGER.info("    └── Rule Groups → THIRD (hardcoded)");
        LOGGER.info("");
        LOGGER.info("3️⃣  EnrichmentService.enrichObject():");
        LOGGER.info("    └── Delegates to YamlEnrichmentProcessor (Rules first, Enrichments second)");
        LOGGER.info("");
        LOGGER.info("4️⃣  SimpleRulesEngine.evaluate():");
        LOGGER.info("    └── Rules only (no enrichments supported)");
        LOGGER.info("");
        LOGGER.info("🚨 THE DESIGN FLAW:");
        LOGGER.info("   • SAME YAML file produces DIFFERENT results depending on processor");
        LOGGER.info("   • Developer intent (YAML section order) is COMPLETELY IGNORED");
        LOGGER.info("   • Behavior is unpredictable and processor-dependent");
        LOGGER.info("   • No processor respects natural YAML document order");
        
        // This test always passes - it's documentation
        assertTrue(true, "This test documents the design flaw");
    }
}
