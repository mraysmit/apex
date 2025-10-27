package dev.mars.apex.demo.etl;

import dev.mars.apex.core.config.yaml.YamlConfigurationLoader;
import dev.mars.apex.core.config.yaml.YamlRuleConfiguration;
import dev.mars.apex.core.engine.pipeline.DataPipelineEngine;
import dev.mars.apex.core.engine.pipeline.YamlPipelineExecutionResult;
import dev.mars.apex.demo.DemoTestBase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for XML data source extraction in ETL pipelines.
 *
 * This test validates OTC (Over-The-Counter) options trade data extraction from XML:
 * - XML file parsing and data extraction
 * - XML element and attribute handling (trade id, status)
 * - Nested element processing (counterparties, optionDetails, riskMetrics)
 * - Complex XML structure parsing for middle office trade processing
 * - File-system data source with XML format
 */
@DisplayName("Pipeline ETL Execution Test - Extract OTC Trades XML")
public class PipelineEtlExecutionTestExtractXml extends DemoTestBase {
    
    private static final Logger logger = LoggerFactory.getLogger(PipelineEtlExecutionTestExtractXml.class);
    
    private DataPipelineEngine pipelineEngine;
    private YamlConfigurationLoader yamlLoader;

    @BeforeEach
    public void setUp() {
        super.setUp();
        pipelineEngine = new DataPipelineEngine();
        yamlLoader = new YamlConfigurationLoader();
    }

    @AfterEach
    public void tearDown() {
        if (pipelineEngine != null) {
            pipelineEngine.shutdown();
        }
        super.tearDown();
    }

    @Test
    @DisplayName("Should extract OTC trade data from XML file")
    void shouldExtractDataFromXmlFile() throws Exception {
        logger.info("=== Testing XML OTC Trades Extract Pipeline ===");

        // Load the YAML configuration
        YamlRuleConfiguration config = yamlLoader.loadFromFile(
            "src/test/java/dev/mars/apex/demo/etl/PipelineEtlExecutionTestExtractXml.yaml");

        // Initialize and execute pipeline
        pipelineEngine.initialize(config);
        YamlPipelineExecutionResult result = pipelineEngine.executePipeline("xml-otc-extract-pipeline");

        // Validate results
        assertNotNull(result, "Pipeline execution result should not be null");
        assertTrue(result.isSuccess(), "Pipeline should execute successfully");
        assertEquals(1, result.getStepResults().size(), "Should have 1 step result (extract only)");

        // Validate extract step
        var extractResult = result.getStepResults().get(0);
        assertEquals("extract-otc-trades", extractResult.getStepName());
        assertTrue(extractResult.isSuccess(), "Extract step should succeed");

        // Validate that data was actually extracted
        assertNotNull(extractResult.getData(), "Extract step should return data");

        // Verify data is a list
        assertTrue(extractResult.getData() instanceof List, "Extracted data should be a List");

        @SuppressWarnings("unchecked")
        List<Object> trades = (List<Object>) extractResult.getData();

        // Verify we extracted the expected number of trades
        assertEquals(4, trades.size(), "Should extract 4 OTC trades from XML file");

        // Verify first trade structure
        assertTrue(trades.get(0) instanceof Map, "Trade should be a Map");

        @SuppressWarnings("unchecked")
        Map<String, Object> firstTrade = (Map<String, Object>) trades.get(0);

        // Validate trade attributes (prefixed with @)
        assertEquals("OTC-2025-001", firstTrade.get("@id"), "Trade ID attribute should match");
        assertEquals("CONFIRMED", firstTrade.get("@status"), "Trade status attribute should match");

        // Validate trade elements
        assertTrue(firstTrade.containsKey("tradeDate"), "Trade should have tradeDate");
        assertTrue(firstTrade.containsKey("expiryDate"), "Trade should have expiryDate");

        // Validate nested counterparties object
        assertTrue(firstTrade.containsKey("counterparties"), "Trade should have counterparties");
        assertTrue(firstTrade.get("counterparties") instanceof Map, "Counterparties should be a Map");

        @SuppressWarnings("unchecked")
        Map<String, Object> counterparties = (Map<String, Object>) firstTrade.get("counterparties");
        assertTrue(counterparties.containsKey("buyer"), "Counterparties should have buyer");
        assertTrue(counterparties.containsKey("seller"), "Counterparties should have seller");

        @SuppressWarnings("unchecked")
        Map<String, Object> buyer = (Map<String, Object>) counterparties.get("buyer");
        assertEquals("GOLDMAN_SACHS", buyer.get("partyId"), "Buyer party ID should match");

        // Validate nested optionDetails
        assertTrue(firstTrade.containsKey("optionDetails"), "Trade should have optionDetails");

        logger.info("✓ XML OTC trades extraction executed successfully");
        logger.info("  Trades extracted: {}", trades.size());
        logger.info("  First trade: {} - Status: {}", firstTrade.get("@id"), firstTrade.get("@status"));
        logger.info("  Nested data validated: counterparties and optionDetails");
    }

    @Test
    @DisplayName("Should handle XML file with deeply nested structures")
    void shouldHandleDeeplyNestedStructures() throws Exception {
        logger.info("=== Testing XML Deeply Nested Structures ===");

        // Load the YAML configuration
        YamlRuleConfiguration config = yamlLoader.loadFromFile(
            "src/test/java/dev/mars/apex/demo/etl/PipelineEtlExecutionTestExtractXml.yaml");

        // Initialize and execute pipeline
        pipelineEngine.initialize(config);
        YamlPipelineExecutionResult result = pipelineEngine.executePipeline("xml-otc-extract-pipeline");

        // Validate results
        assertTrue(result.isSuccess(), "Pipeline should execute successfully");

        var extractResult = result.getStepResults().get(0);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> trades = (List<Map<String, Object>>) extractResult.getData();

        // Verify all trades have required nested structures
        for (Map<String, Object> trade : trades) {
            assertNotNull(trade.get("@id"), "Trade should have ID attribute");
            assertNotNull(trade.get("@status"), "Trade should have status attribute");
            assertNotNull(trade.get("counterparties"), "Trade should have counterparties");
            assertNotNull(trade.get("optionDetails"), "Trade should have optionDetails");
            assertNotNull(trade.get("riskMetrics"), "Trade should have riskMetrics");

            // Verify counterparties is a Map with buyer and seller
            assertTrue(trade.get("counterparties") instanceof Map,
                "Counterparties should be a Map for trade: " + trade.get("@id"));

            @SuppressWarnings("unchecked")
            Map<String, Object> counterparties = (Map<String, Object>) trade.get("counterparties");
            assertNotNull(counterparties.get("buyer"), "Counterparties should have buyer");
            assertNotNull(counterparties.get("seller"), "Counterparties should have seller");

            @SuppressWarnings("unchecked")
            Map<String, Object> buyer = (Map<String, Object>) counterparties.get("buyer");
            assertNotNull(buyer.get("partyId"), "Buyer should have partyId");
            assertNotNull(buyer.get("legalName"), "Buyer should have legalName");
            assertNotNull(buyer.get("lei"), "Buyer should have LEI");

            // Verify optionDetails has nested underlyingAsset
            assertTrue(trade.get("optionDetails") instanceof Map,
                "OptionDetails should be a Map for trade: " + trade.get("@id"));

            @SuppressWarnings("unchecked")
            Map<String, Object> optionDetails = (Map<String, Object>) trade.get("optionDetails");
            assertNotNull(optionDetails.get("optionType"), "OptionDetails should have optionType");
            assertNotNull(optionDetails.get("underlyingAsset"), "OptionDetails should have underlyingAsset");

            // Verify underlyingAsset is a Map
            assertTrue(optionDetails.get("underlyingAsset") instanceof Map,
                "UnderlyingAsset should be a Map for trade: " + trade.get("@id"));

            @SuppressWarnings("unchecked")
            Map<String, Object> underlyingAsset = (Map<String, Object>) optionDetails.get("underlyingAsset");
            assertNotNull(underlyingAsset.get("commodity"), "UnderlyingAsset should have commodity");
            assertNotNull(underlyingAsset.get("unit"), "UnderlyingAsset should have unit");
            assertNotNull(underlyingAsset.get("ticker"), "UnderlyingAsset should have ticker");
        }

        logger.info("✓ Deeply nested structures validated successfully");
        logger.info("  All {} OTC trades have valid nested data (counterparties, optionDetails, riskMetrics)", trades.size());
    }
}

