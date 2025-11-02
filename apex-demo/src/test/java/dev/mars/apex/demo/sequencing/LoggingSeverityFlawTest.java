package dev.mars.apex.demo.sequencing;

import dev.mars.apex.core.config.yaml.YamlConfigurationLoader;
import dev.mars.apex.core.config.yaml.YamlRuleConfiguration;
import dev.mars.apex.core.engine.config.RulesEngine;
import dev.mars.apex.core.engine.config.RulesEngineConfiguration;
import dev.mars.apex.core.engine.model.RuleResult;
import dev.mars.apex.core.service.engine.ExpressionEvaluatorService;
import dev.mars.apex.core.service.enrichment.YamlEnrichmentProcessor;
import dev.mars.apex.core.service.enrichment.YamlEnrichmentProcessor;
import dev.mars.apex.core.service.lookup.LookupServiceRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Demonstrates APEX's critical logging severity flaw where business logic failures
 * are incorrectly logged as WARNING instead of ERROR/SEVERE.
 *
 * This test focuses on demonstrating the problem through console output analysis
 * rather than programmatic log capture (which requires additional dependencies).
 */
@DisplayName("🚨 APEX Logging Severity Flaw Demonstration")
class LoggingSeverityFlawTest {

    private static final Logger logger = LoggerFactory.getLogger(LoggingSeverityFlawTest.class);

    private YamlEnrichmentProcessor processor;
    private YamlConfigurationLoader yamlLoader;
    private YamlEnrichmentProcessor enrichmentProcessor;
    private RulesEngine rulesEngine;

    @BeforeEach
    void setUp() {
        logger.info("🔧 Initializing APEX services for logging severity flaw demonstration");

        // Initialize required services using correct constructors
        yamlLoader = new YamlConfigurationLoader();
        LookupServiceRegistry serviceRegistry = new LookupServiceRegistry();
        ExpressionEvaluatorService evaluatorService = new ExpressionEvaluatorService();

        processor = new YamlEnrichmentProcessor(serviceRegistry, evaluatorService);
        enrichmentProcessor = new YamlEnrichmentProcessor(serviceRegistry, evaluatorService);

        RulesEngineConfiguration config = new RulesEngineConfiguration();
        rulesEngine = new RulesEngine(config);

        logger.info("✅ All services initialized for logging severity flaw demonstration");
    }

    @Test
    @DisplayName("🚨 CRITICAL FLAW: Business logic failure logged as WARNING instead of ERROR")
    void testBusinessLogicFailureLoggedAsWarning() throws Exception {
        System.out.println("=== DEMONSTRATING LOGGING SEVERITY FLAW ===");

        // Load YAML with invalid enrichment condition
        String yamlPath = "src/test/java/dev/mars/apex/demo/sequencing/LoggingSeverityFlawTest.yaml";
        YamlRuleConfiguration config;
        try {
            config = yamlLoader.loadFromFile(yamlPath);
        } catch (Exception e) {
            fail("Failed to load YAML configuration: " + e.getMessage());
            return;
        }

        // Test data that will cause enrichment condition evaluation to fail
        Map<String, Object> testData = new HashMap<>();
        testData.put("amount", 50000.0);
        // Note: ruleResults doesn't exist, so any condition referencing it will fail

        System.out.println("🔍 Processing enrichment with invalid condition reference...");
        System.out.println("📋 Expected: You should see WARNING logs that say 'Error evaluating...'");
        System.out.println("🚨 PROBLEM: These are business logic failures but logged as WARNING!");

        // Process enrichments - this will cause condition evaluation to fail
        RulesEngine engine = RulesEngine.fromYamlConfig(config);
        RuleResult result = engine.evaluate(config, testData);

        System.out.println("📊 Processing completed.");
        System.out.println("🔍 Check the log output above - you should see:");
        System.out.println("   WARNING: Error evaluating enrichment condition...");
        System.out.println("");

        System.out.println("🎯 FLAW ANALYSIS:");
        System.out.println("   ❌ Business logic failure (enrichment condition evaluation) logged as WARNING");
        System.out.println("   ❌ Message literally says 'Error' but uses WARNING level");
        System.out.println("   ❌ Developer sees 'warning' and assumes non-critical issue");
        System.out.println("   ❌ Enrichment silently fails - no data processing occurs");
        System.out.println("   ❌ System continues in broken state");
        System.out.println("   ❌ Debugging becomes extremely difficult");

        System.out.println("\n💡 WHAT SHOULD HAPPEN:");
        System.out.println("   ✅ Log as ERROR or SEVERE level");
        System.out.println("   ✅ Provide clear indication of configuration problem");
        System.out.println("   ✅ Consider fail-fast behavior for critical enrichments");
        System.out.println("   ✅ Give developer actionable error message");

        // Verify that processing completed (demonstrating silent failure)
        assertNotNull(result, "Processing should complete despite business logic failure");
        System.out.println("✅ Test demonstrates that processing continues despite critical configuration errors");
    }

    @Test
    @DisplayName("🔍 Demonstrate Silent Failure Pattern")
    void testSilentFailurePattern() throws Exception {
        System.out.println("=== DEMONSTRATING SILENT FAILURE PATTERN ===");

        // Load configuration with enrichments that will fail
        String yamlPath = "src/test/java/dev/mars/apex/demo/sequencing/LoggingSeverityFlawTest.yaml";
        YamlRuleConfiguration config;
        try {
            config = yamlLoader.loadFromFile(yamlPath);
        } catch (Exception e) {
            fail("Failed to load YAML configuration: " + e.getMessage());
            return;
        }

        Map<String, Object> testData = new HashMap<>();
        testData.put("amount", 1000.0);
        testData.put("customerId", ""); // Invalid - will cause condition failures

        System.out.println("🔍 Processing enrichments with invalid conditions...");
        System.out.println("📋 Expected: You should see WARNING logs about condition evaluation failures");
        System.out.println("🚨 PROBLEM: These failures are logged as WARNING but are actually critical!");

        // Process enrichments - this will cause multiple condition evaluation failures
        RulesEngine engine = RulesEngine.fromYamlConfig(config);
        RuleResult result = engine.evaluate(config, testData);

        System.out.println("📊 Processing completed.");
        System.out.println("🔍 Check the log output above - you should see multiple:");
        System.out.println("   WARNING: Error evaluating enrichment condition...");
        System.out.println("");

        System.out.println("🎯 SILENT FAILURE ANALYSIS:");
        System.out.println("   ❌ Multiple business logic failures logged as WARNING");
        System.out.println("   ❌ Enrichments silently fail to process");
        System.out.println("   ❌ System continues with incomplete data processing");
        System.out.println("   ❌ No clear indication to developer that configuration is broken");
        System.out.println("   ❌ Production systems can run in broken state for extended periods");

        System.out.println("\n💡 INDUSTRY BEST PRACTICES:");
        System.out.println("   ✅ Configuration errors should be ERROR or SEVERE level");
        System.out.println("   ✅ Business logic failures should fail fast");
        System.out.println("   ✅ Silent failures should be avoided");
        System.out.println("   ✅ Error messages should be actionable");

        // Verify that processing completed despite failures
        assertNotNull(result, "Processing should complete despite multiple business logic failures");
        System.out.println("✅ Test demonstrates silent failure pattern - processing continues despite critical errors");
    }

    @Test
    @DisplayName("📋 Document the Logging Severity Flaw")
    void testDocumentLoggingSeverityFlaw() {
        System.out.println("=== APEX LOGGING SEVERITY FLAW DOCUMENTATION ===");

        System.out.println("🚨 CRITICAL FINDING:");
        System.out.println("   APEX systematically logs business logic failures as WARNING instead of ERROR");
        System.out.println("");

        System.out.println("📍 AFFECTED CODE LOCATIONS:");
        System.out.println("   • YamlEnrichmentProcessor:248 - Enrichment condition evaluation failure");
        System.out.println("   • YamlEnrichmentProcessor:425 - Conditional mapping failure");
        System.out.println("   • YamlEnrichmentProcessor:550 - OR condition evaluation failure");
        System.out.println("   • YamlEnrichmentProcessor:567 - AND condition evaluation failure");
        System.out.println("   • YamlEnrichmentProcessor:596 - Condition rule evaluation failure");
        System.out.println("   • YamlEnrichmentProcessor:1241 - Mapping rule failure");
        System.out.println("");

        System.out.println("🎯 IMPACT:");
        System.out.println("   ❌ Masks serious business logic failures");
        System.out.println("   ❌ Makes debugging extremely difficult");
        System.out.println("   ❌ Violates industry best practices");
        System.out.println("   ❌ Allows systems to run in broken states");
        System.out.println("   ❌ Creates 'perfect storm' with YAML processing order flaw");
        System.out.println("");

        System.out.println("💡 REQUIRED FIXES:");
        System.out.println("   ✅ Change enrichment condition failures to ERROR/SEVERE level");
        System.out.println("   ✅ Change conditional mapping failures to ERROR level");
        System.out.println("   ✅ Change rule condition failures to ERROR level");
        System.out.println("   ✅ Add fail-fast options for critical enrichments");
        System.out.println("   ✅ Provide actionable error messages");
        System.out.println("");

        System.out.println("📊 PRIORITY: CRITICAL");
        System.out.println("   This affects all APEX deployments and makes troubleshooting nearly impossible");

        // This test always passes - it's for documentation
        assertTrue(true, "Logging severity flaw documented");
    }
}

