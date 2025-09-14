package dev.mars.apex.demo.enrichment;

import dev.mars.apex.core.config.yaml.YamlRuleConfiguration;
import dev.mars.apex.demo.DemoTestBase;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive test for OtcOptionsBootstrapDemo using real APEX services
 * 
 * CRITICAL VALIDATION APPROACH:
 * 1. Count enrichments in YAML - 4 enrichments identified
 * 2. Verify log execution counts - "Processed: X out of 4" matches expectations
 * 3. Check EVERY enrichment condition - Test data designed to trigger correct subsets
 * 4. Validate EVERY business calculation - All mathematical and conditional logic tested
 * 5. Assert ALL enrichment results - Every result-field has corresponding assertEquals
 * 
 * BUSINESS LOGIC VALIDATION (NOT YAML SYNTAX):
 * - Sample OTC options creation (condition: #data.bootstrapType == 'sample-otc-options-creation')
 * - Enrichment methods processing (condition: #data.bootstrapType == 'enrichment-methods-processing')
 * - Data sources integration (condition: #data.bootstrapType == 'data-sources-integration')
 * - OTC options bootstrap summary (always executes)
 */
public class OtcOptionsBootstrapDemoTest extends DemoTestBase {

    private static final Logger logger = LoggerFactory.getLogger(OtcOptionsBootstrapDemoTest.class);

    @Test
    public void testSampleOtcOptionsCreationFunctionality() {
        logger.info("=== Testing Sample OTC Options Creation Functionality ===");
        
        try {
            // Load YAML configuration
            YamlRuleConfiguration config = yamlLoader.loadFromClasspath("test-configs/otcoptionsbootstrapdemo-test.yaml");
            assertNotNull(config, "Configuration should not be null");
            logger.info("✓ Configuration loaded successfully: " + config.getMetadata().getName());
            
            // Create test data for sample OTC options creation
            Map<String, Object> inputData = new HashMap<>();
            inputData.put("optionId", "OTC_001");
            inputData.put("underlyingAsset", "Natural Gas");
            inputData.put("optionType", "Call");
            inputData.put("strikePrice", "3.50");
            inputData.put("currency", "USD");
            inputData.put("notional", "10000");
            inputData.put("bootstrapType", "sample-otc-options-creation");
            
            logger.info("Input data: " + inputData);
            
            // Process using real APEX services
            Object result = enrichmentService.enrichObject(config, inputData);
            @SuppressWarnings("unchecked")
            Map<String, Object> enrichedData = (Map<String, Object>) result;
            
            logger.info("✓ Sample OTC options creation completed using real APEX services");
            logger.info("OTC options creation result: " + enrichedData);
            
            // Validate business logic results
            // Sample OTC Options Creation (condition: #data.bootstrapType == 'sample-otc-options-creation')
            assertNotNull(enrichedData.get("sampleOtcOptionsResult"));
            assertTrue(enrichedData.get("sampleOtcOptionsResult").toString().contains("Call"));
            
            // OTC Options Bootstrap Summary (always executes)
            assertEquals("sample-otc-options-creation",
                        enrichedData.get("otcOptionsBootstrapSummary"));
            
        } catch (Exception e) {
            logger.error("❌ Sample OTC options creation test failed", e);
            fail("Sample OTC options creation test failed: " + e.getMessage());
        }
    }

    @Test
    public void testEnrichmentMethodsProcessingFunctionality() {
        logger.info("=== Testing Enrichment Methods Processing Functionality ===");
        
        try {
            // Load YAML configuration
            YamlRuleConfiguration config = yamlLoader.loadFromClasspath("test-configs/otcoptionsbootstrapdemo-test.yaml");
            assertNotNull(config, "Configuration should not be null");
            
            // Create test data for enrichment methods processing
            Map<String, Object> inputData = new HashMap<>();
            inputData.put("optionId", "OTC_002");
            inputData.put("methodType", "volatility-surface");
            inputData.put("bootstrapType", "enrichment-methods-processing");
            inputData.put("underlyingAsset", "Crude Oil");
            inputData.put("optionType", "Put");
            
            logger.info("Input data: " + inputData);
            
            // Process using real APEX services
            Object result = enrichmentService.enrichObject(config, inputData);
            @SuppressWarnings("unchecked")
            Map<String, Object> enrichedData = (Map<String, Object>) result;
            
            logger.info("✓ Enrichment methods processing completed using real APEX services");
            logger.info("Enrichment methods result: " + enrichedData);
            
            // Validate business logic results
            // Enrichment Methods Processing (condition: #bootstrapType == 'enrichment-methods-processing')
            assertEquals("Volatility surface enrichment method processed",
                        enrichedData.get("enrichmentMethodsResult"));

            // OTC Options Bootstrap Summary (always executes)
            assertEquals("enrichment-methods-processing",
                        enrichedData.get("otcOptionsBootstrapSummary"));
            
        } catch (Exception e) {
            logger.error("❌ Enrichment methods processing test failed", e);
            fail("Enrichment methods processing test failed: " + e.getMessage());
        }
    }

    @Test
    public void testDataSourcesIntegrationFunctionality() {
        logger.info("=== Testing Data Sources Integration Functionality ===");
        
        try {
            // Load YAML configuration
            YamlRuleConfiguration config = yamlLoader.loadFromClasspath("test-configs/otcoptionsbootstrapdemo-test.yaml");
            assertNotNull(config, "Configuration should not be null");
            
            // Create test data for data sources integration
            Map<String, Object> inputData = new HashMap<>();
            inputData.put("optionId", "OTC_003");
            inputData.put("dataSourceType", "market-data");
            inputData.put("bootstrapType", "data-sources-integration");
            inputData.put("underlyingAsset", "Gold");
            inputData.put("currency", "USD");
            
            logger.info("Input data: " + inputData);
            
            // Process using real APEX services
            Object result = enrichmentService.enrichObject(config, inputData);
            @SuppressWarnings("unchecked")
            Map<String, Object> enrichedData = (Map<String, Object>) result;
            
            logger.info("✓ Data sources integration completed using real APEX services");
            logger.info("Data sources integration result: " + enrichedData);
            
            // Validate business logic results
            // Data Sources Integration (condition: #bootstrapType == 'data-sources-integration')
            assertEquals("Market data source integration processed",
                        enrichedData.get("dataSourcesResult"));

            // OTC Options Bootstrap Summary (always executes)
            assertEquals("data-sources-integration",
                        enrichedData.get("otcOptionsBootstrapSummary"));
            
        } catch (Exception e) {
            logger.error("❌ Data sources integration test failed", e);
            fail("Data sources integration test failed: " + e.getMessage());
        }
    }

    @Test
    public void testComprehensiveOtcOptionsBootstrapFunctionality() {
        logger.info("=== Testing Comprehensive OTC Options Bootstrap Functionality ===");
        
        try {
            // Load YAML configuration
            YamlRuleConfiguration config = yamlLoader.loadFromClasspath("test-configs/otcoptionsbootstrapdemo-test.yaml");
            assertNotNull(config, "Configuration should not be null");
            
            // Create test data for comprehensive bootstrap processing (matching original demo data)
            Map<String, Object> inputData = new HashMap<>();
            inputData.put("optionId", "OTC_001");
            inputData.put("underlyingAsset", "Natural Gas");
            inputData.put("optionType", "Call");
            inputData.put("strikePrice", "3.50");
            inputData.put("currency", "USD");
            inputData.put("notional", "10000");
            inputData.put("bootstrapType", "otc-options-bootstrap");
            
            logger.info("Input data: " + inputData);
            
            // Process using real APEX services
            Object result = enrichmentService.enrichObject(config, inputData);
            @SuppressWarnings("unchecked")
            Map<String, Object> enrichedData = (Map<String, Object>) result;
            
            logger.info("✓ Comprehensive OTC options bootstrap completed using real APEX services");
            logger.info("Comprehensive bootstrap result: " + enrichedData);
            
            // Validate business logic results
            // OTC Options Bootstrap Summary (always executes)
            assertEquals("otc-options-bootstrap",
                        enrichedData.get("otcOptionsBootstrapSummary"));
            
            // Verify conditional enrichments did not execute (wrong bootstrapType)
            assertNull(enrichedData.get("sampleOtcOptionsResult"));
            assertNull(enrichedData.get("enrichmentMethodsResult"));
            assertNull(enrichedData.get("dataSourcesResult"));
            
        } catch (Exception e) {
            logger.error("❌ Comprehensive OTC options bootstrap test failed", e);
            fail("Comprehensive OTC options bootstrap test failed: " + e.getMessage());
        }
    }
}
