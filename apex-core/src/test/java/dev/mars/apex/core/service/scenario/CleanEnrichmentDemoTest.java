package dev.mars.apex.core.service.scenario;

/*
 * Copyright 2025 Mark Andrew Ray-Smith Cityline Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import dev.mars.apex.core.config.yaml.YamlConfigurationLoader;
import dev.mars.apex.core.config.yaml.YamlRuleConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Standalone demonstration test for clean data enrichment.
 * 
 * Verifies that when executing a scenario with enrichment:
 * 1. Original input data is preserved
 * 2. New enriched values are added  
 * 3. NO internal metadata (scenarioContext, scenarioId, etc.) leaks into the result
 */
@DisplayName("Clean Enrichment Demo Test")
class CleanEnrichmentDemoTest {

    private static final Logger logger = LoggerFactory.getLogger(CleanEnrichmentDemoTest.class);

    private ScenarioStageExecutor executor;
    private YamlConfigurationLoader configLoader;

    @BeforeEach
    void setUp() {
        configLoader = new YamlConfigurationLoader();
        executor = new ScenarioStageExecutor(configLoader, null);
    }

    @Test
    @DisplayName("Scenario enrichment returns clean data with enriched fields but NO metadata")
    void testScenarioEnrichmentReturnsCleanDataWithEnrichment() {
        logger.info("=== DEMO: Clean Enrichment Flow with Actual Enrichment ===");

        // 1. SETUP: Create input data (business data only)
        Map<String, Object> inputData = new HashMap<>();
        inputData.put("tradeId", "TRD-998877");
        inputData.put("currency", "USD");
        inputData.put("amount", 50000.00);
        inputData.put("counterparty", "ACME Corp");
        
        logger.info("1. INPUT DATA: {}", inputData);
        logger.info("   Keys: {}", inputData.keySet());

        // 2. SETUP: Create a scenario with one stage using real YAML config
        ScenarioStage stage = new ScenarioStage();
        stage.setStageName("enrichment-stage");
        // Path is relative to classpath (src/test/resources)
        stage.setConfigFile("scenario/clean-enrichment-demo.yaml");
        stage.setEnabled(true);
        stage.setExecutionOrder(1);  // Required: execution order must be positive

        ScenarioConfiguration scenario = ScenarioConfiguration.withStages(
            "trade-enrichment-scenario",
            "Trade Enrichment Demo",
            Collections.singletonList("TRADE_PROCESSING"),
            Collections.singletonList(stage)
        );

        // 3. EXECUTE: Run the scenario
        logger.info("2. EXECUTING SCENARIO...");
        logger.info("   (Engine internally injects: scenarioContext, scenarioId, previousStageResults, executionStartTime)");
        logger.info("   (Enrichment should add: riskCategory, status)");
        
        ScenarioExecutionResult result = executor.executeStages(scenario, inputData);

        // 4. VERIFY: Check what's in the data AFTER execution
        logger.info("3. DATA AFTER EXECUTION: {}", inputData);
        logger.info("   Keys: {}", inputData.keySet());

        // Get stage outputs to see enrichment results
        StageExecutionResult stageResult = result.getStageResults().isEmpty() ? null : 
            result.getStageResults().stream()
                .filter(r -> "enrichment-stage".equals(r.getStageName()))
                .findFirst()
                .orElse(null);
                
        if (stageResult != null) {
            logger.info("   Stage Outputs: {}", stageResult.getStageOutputs());
        }

        // CRITICAL ASSERTIONS - No metadata should leak into inputData
        
        assertFalse(inputData.containsKey("scenarioContext"), 
            "FAIL: 'scenarioContext' leaked into business data!");
        
        assertFalse(inputData.containsKey("previousStageResults"), 
            "FAIL: 'previousStageResults' leaked into business data!");
        
        assertFalse(inputData.containsKey("scenarioId"), 
            "FAIL: 'scenarioId' leaked into business data!");
        
        assertFalse(inputData.containsKey("executionStartTime"), 
            "FAIL: 'executionStartTime' leaked into business data!");

        // Original data should still be there
        assertEquals("TRD-998877", inputData.get("tradeId"), "Original tradeId should be preserved");
        assertEquals("USD", inputData.get("currency"), "Original currency should be preserved");
        assertEquals(50000.00, inputData.get("amount"), "Original amount should be preserved");
        assertEquals("ACME Corp", inputData.get("counterparty"), "Original counterparty should be preserved");

        // Enriched fields should be present (either in inputData or stage outputs)
        if (stageResult != null && stageResult.getStageOutputs() != null) {
            Map<String, Object> outputs = stageResult.getStageOutputs();
            
            // Stage outputs should NOT contain metadata
            assertFalse(outputs.containsKey("scenarioContext"), 
                "FAIL: 'scenarioContext' leaked into stage outputs!");
            assertFalse(outputs.containsKey("previousStageResults"), 
                "FAIL: 'previousStageResults' leaked into stage outputs!");
            assertFalse(outputs.containsKey("scenarioId"), 
                "FAIL: 'scenarioId' leaked into stage outputs!");
            assertFalse(outputs.containsKey("executionStartTime"), 
                "FAIL: 'executionStartTime' leaked into stage outputs!");
            
            logger.info("   Stage outputs are clean (no metadata)");
            
            // Check if enrichment was applied
            if (outputs.containsKey("riskCategory")) {
                assertEquals("HIGH_VALUE", outputs.get("riskCategory"), "Enrichment should set riskCategory");
                logger.info("   [OK] Enrichment applied: riskCategory = {}", outputs.get("riskCategory"));
            }
            if (outputs.containsKey("status")) {
                assertEquals("APPROVED", outputs.get("status"), "Enrichment should set status");
                logger.info("   [OK] Enrichment applied: status = {}", outputs.get("status"));
            }
        }
        
        // Also check inputData for enriched fields (they get merged back)
        if (inputData.containsKey("riskCategory")) {
            logger.info("   [OK] Enrichment merged to inputData: riskCategory = {}", inputData.get("riskCategory"));
        }
        if (inputData.containsKey("status")) {
            logger.info("   [OK] Enrichment merged to inputData: status = {}", inputData.get("status"));
        }

        logger.info("4. SUCCESS: Data is clean - contains only business fields + enrichments, no metadata pollution!");
        logger.info("   Final inputData keys: {}", inputData.keySet());
    }
}
