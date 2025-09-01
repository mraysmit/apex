package dev.mars.apex.demo;

import dev.mars.apex.core.config.yaml.YamlRuleConfiguration;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class to validate all YAML configuration files in the demo module.
 * Ensures that all YAML files have proper APEX metadata and can be loaded successfully.
 */
public class YamlConfigurationValidationTest extends DemoTestBase {

    /**
     * Test that key demo YAML configurations can be loaded successfully.
     */
    @Test
    void testCustomerTransformerYamlLoading() {
        String yamlPath = "enrichment/customer-transformer-demo.yaml";
        logger.info("Testing YAML configuration: {}", yamlPath);

        YamlRuleConfiguration config = loadAndValidateYaml(yamlPath);
        assertNotNull(config, "Configuration should not be null");

        logger.info("✅ YAML configuration validation passed: {}", yamlPath);
    }

    @Test
    void testTradeTransformerYamlLoading() {
        String yamlPath = "enrichment/trade-transformer-demo.yaml";
        logger.info("Testing YAML configuration: {}", yamlPath);

        YamlRuleConfiguration config = loadAndValidateYaml(yamlPath);
        assertNotNull(config, "Configuration should not be null");

        logger.info("✅ YAML configuration validation passed: {}", yamlPath);
    }

    @Test
    void testFinancialSettlementYamlLoading() {
        String yamlPath = "enrichment/comprehensive-financial-settlement-demo-config.yaml";
        logger.info("Testing YAML configuration: {}", yamlPath);

        YamlRuleConfiguration config = loadAndValidateYaml(yamlPath);
        assertNotNull(config, "Configuration should not be null");

        logger.info("✅ YAML configuration validation passed: {}", yamlPath);
    }

    /**
     * Test external data configuration files.
     */
    @Test
    void testFinancialSettlementDataYamlLoading() {
        String yamlPath = "enrichment/comprehensive-financial-settlement-demo-data.yaml";
        logger.info("Testing external data configuration: {}", yamlPath);

        YamlRuleConfiguration config = loadAndValidateYaml(yamlPath);
        assertNotNull(config, "External data configuration should not be null");

        logger.info("✅ External data configuration validation passed: {}", yamlPath);
    }

    @Test
    void testDataManagementDataYamlLoading() {
        String yamlPath = "enrichment/data-management-demo-data.yaml";
        logger.info("Testing external data configuration: {}", yamlPath);

        YamlRuleConfiguration config = loadAndValidateYaml(yamlPath);
        assertNotNull(config, "External data configuration should not be null");

        logger.info("✅ External data configuration validation passed: {}", yamlPath);
    }

    /**
     * Test that enrichment configurations have proper enrichment sections.
     */
    @Test
    void testEnrichmentConfigurationStructure() {
        logger.info("Testing enrichment configuration structure...");
        
        YamlRuleConfiguration config = loadAndValidateYaml("enrichment/customer-transformer-demo.yaml");
        
        // Test basic enrichment functionality
        Map<String, Object> testData = createSampleTestData();
        testData.put("transformerType", "customer-segments-processing");
        testData.put("segmentType", "membership-based");
        
        Object result = testEnrichment(config, testData);
        assertNotNull(result, "Enrichment result should not be null");
        
        logger.info("✅ Enrichment configuration structure validation passed");
    }

    /**
     * Test that bootstrap configuration files can be loaded.
     */
    @Test
    void testAutoRepairRulesConfigLoading() {
        String yamlPath = "enrichment/custody-bootstrap/auto-repair-rules-config.yaml";
        logger.info("Testing bootstrap configuration: {}", yamlPath);

        YamlRuleConfiguration config = loadAndValidateYaml(yamlPath);
        assertNotNull(config, "Bootstrap configuration should not be null");

        logger.info("✅ Bootstrap configuration validation passed: {}", yamlPath);
    }

    /**
     * Test that transformer configuration files can be loaded.
     */
    @Test
    void testCustomerSegmentsConfigLoading() {
        String yamlPath = "enrichment/customer-transformer/customer-segments-config.yaml";
        logger.info("Testing transformer configuration: {}", yamlPath);

        YamlRuleConfiguration config = loadAndValidateYaml(yamlPath);
        assertNotNull(config, "Transformer configuration should not be null");

        logger.info("✅ Transformer configuration validation passed: {}", yamlPath);
    }
}
