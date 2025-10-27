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
 * Test class for JSON data source extraction in ETL pipelines.
 * 
 * This test validates:
 * - JSON file parsing and data extraction
 * - JSONPath query execution
 * - Nested object handling
 * - Array processing
 * - File-system data source with JSON format
 */
@DisplayName("Pipeline ETL Execution Test - Extract JSON")
public class PipelineEtlExecutionTestExtractJson extends DemoTestBase {
    
    private static final Logger logger = LoggerFactory.getLogger(PipelineEtlExecutionTestExtractJson.class);
    
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
    @DisplayName("Should extract OTC options trade data from JSON file")
    void shouldExtractDataFromJsonFile() throws Exception {
        logger.info("=== Testing JSON OTC Options Extract Pipeline ===");

        // Load the YAML configuration
        YamlRuleConfiguration config = yamlLoader.loadFromFile(
            "src/test/java/dev/mars/apex/demo/etl/PipelineEtlExecutionTestExtractJson.yaml");

        // Initialize and execute pipeline
        pipelineEngine.initialize(config);
        YamlPipelineExecutionResult result = pipelineEngine.executePipeline("json-otc-extract-pipeline");

        // Validate results
        assertNotNull(result, "Pipeline execution result should not be null");
        assertTrue(result.isSuccess(), "Pipeline should execute successfully");
        assertEquals(1, result.getStepResults().size(), "Should have 1 step result (extract only)");

        // Validate extract step
        var extractResult = result.getStepResults().get(0);
        assertEquals("extract-otc-options", extractResult.getStepName());
        assertTrue(extractResult.isSuccess(), "Extract step should succeed");

        // Validate that data was actually extracted
        assertNotNull(extractResult.getData(), "Extract step should return data");

        // Verify data is a list
        assertTrue(extractResult.getData() instanceof List, "Extracted data should be a List");

        @SuppressWarnings("unchecked")
        List<Object> trades = (List<Object>) extractResult.getData();

        // Verify we extracted the expected number of trades
        assertEquals(6, trades.size(), "Should extract 6 OTC option trades from JSON file");

        // Verify first trade structure
        assertTrue(trades.get(0) instanceof Map, "Trade should be a Map");

        @SuppressWarnings("unchecked")
        Map<String, Object> firstTrade = (Map<String, Object>) trades.get(0);

        // Validate trade fields
        assertEquals("OTC-2025-001", firstTrade.get("tradeId"), "Trade ID should match");
        assertEquals("2025-10-15", firstTrade.get("tradeDate"), "Trade date should match");
        assertEquals("GOLDMAN_SACHS", firstTrade.get("buyerParty"), "Buyer party should match");
        assertEquals("JP_MORGAN", firstTrade.get("sellerParty"), "Seller party should match");
        assertEquals("Call", firstTrade.get("optionType"), "Option type should match");
        assertEquals("ACTIVE", firstTrade.get("status"), "Trade status should match");

        // Validate nested underlyingAsset object
        assertTrue(firstTrade.containsKey("underlyingAsset"), "Trade should have underlyingAsset");
        assertTrue(firstTrade.get("underlyingAsset") instanceof Map, "UnderlyingAsset should be a Map");

        @SuppressWarnings("unchecked")
        Map<String, Object> underlying = (Map<String, Object>) firstTrade.get("underlyingAsset");
        assertEquals("Natural Gas", underlying.get("commodity"), "Commodity should match");
        assertEquals("MMBtu", underlying.get("unit"), "Unit should match");
        assertEquals("NG", underlying.get("ticker"), "Ticker should match");

        // Validate array field (tags)
        assertTrue(firstTrade.containsKey("tags"), "Trade should have tags");
        assertTrue(firstTrade.get("tags") instanceof List, "Tags should be a List");

        @SuppressWarnings("unchecked")
        List<String> tags = (List<String>) firstTrade.get("tags");
        assertTrue(tags.contains("otc-derivative"), "Tags should contain 'otc-derivative'");

        logger.info("✓ JSON OTC options extraction executed successfully");
        logger.info("  Trades extracted: {}", trades.size());
        logger.info("  First trade: {} - {} {} on {}",
            firstTrade.get("tradeId"),
            firstTrade.get("optionType"),
            underlying.get("commodity"),
            firstTrade.get("tradeDate"));
        logger.info("  Nested data validated: underlyingAsset and tags");
    }

    @Test
    @DisplayName("Should handle JSON file with complex nested structures")
    void shouldHandleComplexNestedStructures() throws Exception {
        logger.info("=== Testing JSON Complex Nested Structures ===");

        // Load the YAML configuration
        YamlRuleConfiguration config = yamlLoader.loadFromFile(
            "src/test/java/dev/mars/apex/demo/etl/PipelineEtlExecutionTestExtractJson.yaml");
        
        // Initialize and execute pipeline
        pipelineEngine.initialize(config);
        YamlPipelineExecutionResult result = pipelineEngine.executePipeline("json-otc-extract-pipeline");

        // Validate results
        assertTrue(result.isSuccess(), "Pipeline should execute successfully");

        var extractResult = result.getStepResults().get(0);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> trades = (List<Map<String, Object>>) extractResult.getData();

        // Verify all trades have required nested structures
        for (Map<String, Object> trade : trades) {
            assertNotNull(trade.get("tradeId"), "Trade should have tradeId");
            assertNotNull(trade.get("buyerParty"), "Trade should have buyerParty");
            assertNotNull(trade.get("underlyingAsset"), "Trade should have underlyingAsset");
            assertNotNull(trade.get("tags"), "Trade should have tags");

            // Verify underlyingAsset is a Map
            assertTrue(trade.get("underlyingAsset") instanceof Map,
                "UnderlyingAsset should be a Map for trade: " + trade.get("tradeId"));

            // Verify tags is a List
            assertTrue(trade.get("tags") instanceof List,
                "Tags should be a List for trade: " + trade.get("tradeId"));
        }

        logger.info("✓ Complex nested structures validated successfully");
        logger.info("  All {} OTC option trades have valid nested data", trades.size());
    }
}

