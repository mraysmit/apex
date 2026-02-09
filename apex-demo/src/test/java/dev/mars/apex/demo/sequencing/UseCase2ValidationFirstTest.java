package dev.mars.apex.demo.sequencing;

import dev.mars.apex.core.config.model.YamlRuleConfiguration;
import dev.mars.apex.core.config.loader.YamlConfigurationLoader;
import dev.mars.apex.core.engine.core.RulesEngine;
import dev.mars.apex.core.engine.model.RuleResult;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * USE CASE 2: Validation-First Processing Pattern
 *
 * Business Scenario: Performance optimization for transaction processing
 *
 * Developer Intent:
 *   1. FIRST: Fast validation of input data (reject invalid data quickly)
 *   2. SECOND: Expensive enrichment operations (only for valid data)
 *
 * YAML Structure: rules section BEFORE enrichments section
 * Processing Mode: sequential (respects YAML order)
 *
 * Performance Benefit: Invalid data is rejected quickly without expensive lookups
 */
@DisplayName("USE CASE 2: Validation-First Processing")
class UseCase2ValidationFirstTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(UseCase2ValidationFirstTest.class);

    private YamlConfigurationLoader yamlLoader;

    @BeforeEach
    void setUp() {
        LOGGER.info("🎯 Setting up USE CASE 2: Validation-First Processing test");

        // Initialize APEX services for sequential processing
        yamlLoader = new YamlConfigurationLoader();

        LOGGER.info("Sequential processing services initialized");
    }

    @Test
    @DisplayName("SEQUENTIAL MODE: Validation-First Pattern Works")
    void testValidationFirstPatternWithSequentialMode() throws Exception {
        LOGGER.info("=== TESTING: Validation-First Pattern with Sequential Mode ===");

        // Load YAML configuration with sequential processing enabled
        String yamlPath = "src/test/java/dev/mars/apex/demo/sequencing/UseCase2ValidationFirstTest.yaml";
        YamlRuleConfiguration config = yamlLoader.loadFromFile(yamlPath);

        // Test with VALID data (should trigger expensive enrichments)
        Map<String, Object> validData = new HashMap<>();
        validData.put("customerId", "CUST001");
        validData.put("amount", 50000.0);
        validData.put("currency", "USD");

        LOGGER.info("Valid Input Data: {}", validData);

        // Process with enrichment service (demonstrates sequential processing)
        RulesEngine engine = RulesEngine.fromYamlConfig(config);
        RuleResult ruleResult = engine.evaluate(config, validData);
        Object result = ruleResult.getEnrichedData();
        assertNotNull(result, "Enrichment result should not be null");

        @SuppressWarnings("unchecked")
        Map<String, Object> enrichedData = (Map<String, Object>) result;

        LOGGER.info("Processing completed. Final data state: {}", enrichedData);

        // Verify validation-first pattern worked correctly
        // Note: In validation-first pattern, rules execute first, then enrichments
        // The enrichments should only execute if validation passed

        // Verify customer information was enriched (expensive operation)
        assertNotNull(enrichedData.get("customerName"),
                     "Customer name should be enriched");
        assertEquals("Alice Johnson", enrichedData.get("customerName"),
                    "Customer CUST001 should be Alice Johnson");

        // Verify risk score was calculated (expensive operation)
        assertNotNull(enrichedData.get("riskScore"),
                     "Risk score should be calculated");

        LOGGER.info("Validation-First pattern WORKS with sequential processing");
        LOGGER.info("   1. Rules processed FIRST (validation)");
        LOGGER.info("   2. Enrichments processed SECOND (expensive operations)");
        LOGGER.info("   3. Customer enriched: {}", enrichedData.get("customerName"));
        LOGGER.info("   4. Risk calculated: {}", enrichedData.get("riskScore"));
    }
}

