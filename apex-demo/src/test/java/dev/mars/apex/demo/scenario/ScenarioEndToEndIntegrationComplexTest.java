package dev.mars.apex.demo.scenario;

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

import dev.mars.apex.core.service.scenario.DataTypeScenarioService;
import dev.mars.apex.core.service.scenario.ScenarioExecutionResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Complex End-to-End Integration Test for APEX Scenario System with Multiple Scenarios.
 * 
 * Demonstrates classification rule routing with 2 different scenarios:
 * 1. OTC Option Scenario - for derivative trades
 * 2. Bond Scenario - for fixed income trades
 * 
 * Tests that the classification rule correctly selects the appropriate scenario
 * based on incoming data characteristics.
 * 
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 1.0.0
 */
@DisplayName("Scenario End-to-End Integration Test - Complex Multi-Scenario")
class ScenarioEndToEndIntegrationComplexTest {

    private static final Logger logger = LoggerFactory.getLogger(ScenarioEndToEndIntegrationComplexTest.class);
    private Path tempDir;
    private DataTypeScenarioService scenarioService;

    @BeforeEach
    void setUp() throws IOException {
        tempDir = Files.createTempDirectory("scenario-e2e-complex-test");
        scenarioService = new DataTypeScenarioService();
    }

    @Test
    @DisplayName("Test Classification Rule Routing with Multiple Scenarios")
    void testMultipleScenarioClassification() throws Exception {
        logger.info("\n" + "=".repeat(80));
        logger.info("SCENARIO END-TO-END INTEGRATION TEST - COMPLEX MULTI-SCENARIO");
        logger.info("=".repeat(80));

        // Create registry with 2 scenarios
        String registryYaml = createRegistryYaml();
        Path registryFile = tempDir.resolve("registry.yaml");
        Files.writeString(registryFile, registryYaml);

        logger.info("\n[REGISTRY] Configured with 2 scenarios:");
        logger.info("  1. otc-option-us - For OTC derivative trades");
        logger.info("  2. bond-us - For fixed income bond trades");

        // Create both scenario files
        String otcScenarioYaml = createOtcScenarioYaml();
        Path otcScenarioFile = tempDir.resolve("otc-option-us-scenario.yaml");
        Files.writeString(otcScenarioFile, otcScenarioYaml);

        String bondScenarioYaml = createBondScenarioYaml();
        Path bondScenarioFile = tempDir.resolve("bond-us-scenario.yaml");
        Files.writeString(bondScenarioFile, bondScenarioYaml);

        // Create validation and enrichment rules for both scenarios
        Files.writeString(tempDir.resolve("otc-validation-rules.yaml"), createOtcValidationRulesYaml());
        Files.writeString(tempDir.resolve("otc-enrichment-rules.yaml"), createOtcEnrichmentRulesYaml());
        Files.writeString(tempDir.resolve("bond-validation-rules.yaml"), createBondValidationRulesYaml());
        Files.writeString(tempDir.resolve("bond-enrichment-rules.yaml"), createBondEnrichmentRulesYaml());

        // Load registry
        scenarioService.loadScenarios(registryFile.toString());
        logger.info("[OK] Registry loaded with 2 scenarios");

        // TEST 1: OTC Option Trade
        logger.info("\n" + "=".repeat(80));
        logger.info("[TEST 1] Processing OTC Option Trade");
        logger.info("=".repeat(80));

        Map<String, Object> otcTradeData = createOtcTradeData();
        testScenarioClassification("OTC Option", otcTradeData, "otc-option-us");

        // TEST 2: Bond Trade
        logger.info("\n" + "=".repeat(80));
        logger.info("[TEST 2] Processing Bond Trade");
        logger.info("=".repeat(80));

        Map<String, Object> bondTradeData = createBondTradeData();
        testScenarioClassification("Bond", bondTradeData, "bond-us");

        logger.info("\n" + "=".repeat(80));
        logger.info("[SUCCESS] MULTI-SCENARIO CLASSIFICATION TEST PASSED");
        logger.info("=".repeat(80));
    }

    private void testScenarioClassification(String tradeType, Map<String, Object> tradeData, String expectedScenarioId) {
        logger.info("\n[DATA] Input {} trade data:", tradeType);
        logger.info("  - Trade Type: {}", tradeData.get("tradeType"));
        logger.info("  - Asset Class: {}", tradeData.get("assetClass"));
        logger.info("  - Amount: ${}", String.format("%,d", ((Number) tradeData.get("amount")).longValue()));

        logger.info("\n" + "=".repeat(80));
        logger.info("[CLASSIFICATION RULE] Applying classification rule to incoming data");
        logger.info("=".repeat(80));

        ScenarioExecutionResult result = scenarioService.processMapData(tradeData);

        assertNotNull(result, "Scenario execution result should not be null");
        assertEquals(expectedScenarioId, result.getScenarioId(), "Should execute " + expectedScenarioId + " scenario");
        assertTrue(result.isSuccessful(), "Scenario execution should be successful");

        logger.info("  [Classification Result]");
        logger.info("  - Matched Scenario: {}", result.getScenarioId());
        logger.info("  - Execution Status: {}", result.isSuccessful() ? "SUCCESS" : "FAILED");
        logger.info("  - Stages Executed: {}", result.getStageResults().size());
        logger.info("=".repeat(80));
    }

    private Map<String, Object> createOtcTradeData() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("tradeType", "OTCOption");
        data.put("assetClass", "Derivatives");
        data.put("region", "US");
        data.put("amount", 75000000L);
        data.put("counterparty", "Goldman Sachs");
        data.put("currency", "USD");
        return data;
    }

    private Map<String, Object> createBondTradeData() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("tradeType", "Bond");
        data.put("assetClass", "FixedIncome");
        data.put("region", "US");
        data.put("amount", 50000000L);
        data.put("issuer", "US Treasury");
        data.put("currency", "USD");
        data.put("maturity", "2030-12-31");
        return data;
    }

    private String createRegistryYaml() {
        String path = tempDir.toString().replace("\\", "/");
        return """
            metadata:
              id: "multi-scenario-registry"
              name: "Multi-Scenario Registry"
              version: "1.0.0"
              description: "Registry with OTC and Bond scenarios"
              type: "scenario-registry"

            scenarios:
              - scenario-id: "otc-option-us"
                config-file: "%s/otc-option-us-scenario.yaml"
                business-domain: "Derivatives Trading"
              
              - scenario-id: "bond-us"
                config-file: "%s/bond-us-scenario.yaml"
                business-domain: "Fixed Income"
            """.formatted(path, path);
    }

    private String createOtcScenarioYaml() {
        String path = tempDir.toString().replace("\\", "/");
        return """
            metadata:
              id: "otc-option-us"
              name: "OTC Option US Processing"
              type: "scenario"

            scenario:
              scenario-id: "otc-option-us"
              name: "OTC Option US Processing"
              
              classification-rule:
                condition: "#data['tradeType'] == 'OTCOption' && #data['region'] == 'US'"
                description: "US OTC option trades"

              processing-stages:
                - stage-name: "validation"
                  config-file: "%s/otc-validation-rules.yaml"
                  execution-order: 1
                  failure-policy: "terminate"
                  required: true
            """.formatted(path);
    }

    private String createBondScenarioYaml() {
        String path = tempDir.toString().replace("\\", "/");
        return """
            metadata:
              id: "bond-us"
              name: "Bond US Processing"
              type: "scenario"

            scenario:
              scenario-id: "bond-us"
              name: "Bond US Processing"
              
              classification-rule:
                condition: "#data['tradeType'] == 'Bond' && #data['region'] == 'US'"
                description: "US bond trades"

              processing-stages:
                - stage-name: "validation"
                  config-file: "%s/bond-validation-rules.yaml"
                  execution-order: 1
                  failure-policy: "terminate"
                  required: true
            """.formatted(path);
    }

    private String createOtcValidationRulesYaml() {
        return """
            metadata:
              id: "otc-validation-rules"
              name: "OTC Validation Rules"
              type: "rule-config"
            
            rules:
              - id: "validate-amount"
                name: "Validate Amount"
                condition: "#data['amount'] > 0"
                message: "Amount must be positive"
                enabled: true
            """;
    }

    private String createOtcEnrichmentRulesYaml() {
        return """
            metadata:
              id: "otc-enrichment-rules"
              name: "OTC Enrichment Rules"
              type: "rule-config"
            
            rules:
              - id: "enrich-risk"
                name: "Enrich Risk"
                condition: "true"
                message: "Mark as derivative"
                enabled: true
            """;
    }

    private String createBondValidationRulesYaml() {
        return """
            metadata:
              id: "bond-validation-rules"
              name: "Bond Validation Rules"
              type: "rule-config"
            
            rules:
              - id: "validate-amount"
                name: "Validate Amount"
                condition: "#data['amount'] > 0"
                message: "Amount must be positive"
                enabled: true
            """;
    }

    private String createBondEnrichmentRulesYaml() {
        return """
            metadata:
              id: "bond-enrichment-rules"
              name: "Bond Enrichment Rules"
              type: "rule-config"
            
            rules:
              - id: "enrich-type"
                name: "Enrich Type"
                condition: "true"
                message: "Mark as fixed income"
                enabled: true
            """;
    }
}

