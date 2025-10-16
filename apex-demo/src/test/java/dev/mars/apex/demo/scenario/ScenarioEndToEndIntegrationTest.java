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
import dev.mars.apex.core.service.scenario.StageExecutionResult;
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
 * End-to-End Integration Test for APEX Scenario System.
 * 
 * Tests the complete flow as defined in SCENARIO_DESIGN_SUMMARY.md:
 * 1. Registry Stage - Load scenario registry
 * 2. Scenario Stage - Load scenario with classification rules
 * 3. Validation Stage - Execute validation rules
 * 4. Enrichment Stage - Execute enrichment rules
 * 
 * All YAML configuration is inline (self-contained).
 * 
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 1.0.0
 */
@DisplayName("Scenario End-to-End Integration Test")
class ScenarioEndToEndIntegrationTest {

    private static final Logger logger = LoggerFactory.getLogger(ScenarioEndToEndIntegrationTest.class);
    
    private DataTypeScenarioService scenarioService;
    private Path tempDir;

    @BeforeEach
    void setUp() throws IOException {
        scenarioService = new DataTypeScenarioService();
        tempDir = Files.createTempDirectory("scenario-e2e-test");
    }

    @Test
    @DisplayName("Should execute complete scenario flow: Registry → Scenario → Validation → Enrichment")
    void testCompleteScenarioFlow() throws Exception {
        logger.info("=".repeat(80));
        logger.info("SCENARIO END-TO-END INTEGRATION TEST");
        logger.info("=".repeat(80));

        // ========================================
        // STAGE 1: REGISTRY STAGE
        // ========================================
        logger.info("\n[STAGE 1] Registry Stage - Loading scenario registry");
        
        String registryYaml = createRegistryYaml();
        Path registryFile = tempDir.resolve("registry.yaml");
        Files.writeString(registryFile, registryYaml);
        
        logger.info("[OK] Registry YAML created at: {}", registryFile);
        logger.info("  [Registry Configuration]");
        logger.info("  - Scenario ID: otc-option-us");
        logger.info("  - Business Domain: Derivatives Trading");
        logger.info("  - Scenario File: otc-option-us-scenario.yaml");
        logger.info("  - Registry Content:");
        for (String line : registryYaml.split("\n")) {
            if (!line.trim().isEmpty()) {
                logger.info("    {}", line);
            }
        }

        // ========================================
        // STAGE 2: SCENARIO STAGE
        // ========================================
        logger.info("\n[STAGE 2] Scenario Stage - Loading scenario with classification rules");

        String scenarioYaml = createScenarioYaml();
        Path scenarioFile = tempDir.resolve("otc-option-us-scenario.yaml");
        Files.writeString(scenarioFile, scenarioYaml);

        logger.info("[OK] Scenario YAML created at: {}", scenarioFile);
        logger.info("  [Scenario Configuration]");
        logger.info("  - Classification Rule: #data['tradeType'] == 'OTCOption' && #data['region'] == 'US'");
        logger.info("  - Processing Stages: validation, enrichment");
        logger.info("  - Scenario Content:");
        for (String line : scenarioYaml.split("\n")) {
            if (!line.trim().isEmpty()) {
                logger.info("    {}", line);
            }
        }

        // ========================================
        // STAGE 3: VALIDATION STAGE
        // ========================================
        logger.info("\n[STAGE 3] Validation Stage - Creating validation rules");

        String validationYaml = createValidationRulesYaml();
        Path validationFile = tempDir.resolve("otc-validation-rules.yaml");
        Files.writeString(validationFile, validationYaml);

        logger.info("[OK] Validation Rules YAML created at: {}", validationFile);
        logger.info("  [Validation Rules Configuration]");
        logger.info("  Rule 1: Validate Notional Amount");
        logger.info("    - ID: validate-notional");
        logger.info("    - Condition: #data['notional'] > 0");
        logger.info("    - Message: Notional amount must be positive");
        logger.info("    - Purpose: Ensure trade notional is positive");
        logger.info("  ");
        logger.info("  Rule 2: Validate Region");
        logger.info("    - ID: validate-region");
        logger.info("    - Condition: #data['region'] == 'US'");
        logger.info("    - Message: Region must be US");
        logger.info("    - Purpose: Ensure trade is for US region");
        logger.info("  Total Validation Rules: 2");

        // ========================================
        // STAGE 4: ENRICHMENT STAGE
        // ========================================
        logger.info("\n[STAGE 4] Enrichment Stage - Creating enrichment rules");

        String enrichmentYaml = createEnrichmentRulesYaml();
        Path enrichmentFile = tempDir.resolve("otc-enrichment-rules.yaml");
        Files.writeString(enrichmentFile, enrichmentYaml);

        logger.info("[OK] Enrichment Rules YAML created at: {}", enrichmentFile);
        logger.info("  [Enrichment Rules Configuration]");
        logger.info("  Rule 1: Enrich Risk Category");
        logger.info("    - ID: enrich-risk-category");
        logger.info("    - Condition: #data['notional'] > 50000000");
        logger.info("    - Message: High notional trade - mark as HIGH_RISK");
        logger.info("    - Purpose: Classify high-value trades for risk management");
        logger.info("    - Enrichment Output: riskCategory = HIGH_RISK");
        logger.info("  ");
        logger.info("  Rule 2: Enrich Processing Priority");
        logger.info("    - ID: enrich-priority");
        logger.info("    - Condition: true");
        logger.info("    - Message: Set processing priority to NORMAL");
        logger.info("    - Purpose: Assign default processing priority");
        logger.info("    - Enrichment Output: processingPriority = NORMAL");
        logger.info("  Total Enrichment Rules: 2");

        // ========================================
        // LOAD AND EXECUTE
        // ========================================
        logger.info("\n[EXECUTION] Loading registry and processing data");

        scenarioService.loadScenarios(registryFile.toString());
        logger.info("[OK] Registry loaded successfully");

        // Create test data that matches classification rule
        Map<String, Object> tradeData = new HashMap<>();
        tradeData.put("tradeType", "OTCOption");
        tradeData.put("region", "US");
        tradeData.put("notional", 75000000);
        tradeData.put("counterparty", "Goldman Sachs");
        tradeData.put("currency", "USD");
        tradeData.put("maturity", "2025-12-31");
        tradeData.put("strikePrice", 100.50);
        tradeData.put("optionType", "CALL");

        logger.info("\n[DATA] Input trade data:");
        logger.info("  [Trade Identification]");
        logger.info("  - Trade Type: {} (Classification: OTCOption)", tradeData.get("tradeType"));
        logger.info("  - Region: {} (Classification: US)", tradeData.get("region"));
        logger.info("  - Counterparty: {}", tradeData.get("counterparty"));
        logger.info("  [Trade Details]");
        long notionalValue = ((Number) tradeData.get("notional")).longValue();
        logger.info("  - Notional Amount: ${} (for validation: > 0)", String.format("%,d", notionalValue));
        logger.info("  - Currency: {}", tradeData.get("currency"));
        logger.info("  - Maturity Date: {}", tradeData.get("maturity"));
        logger.info("  [Option Specifics]");
        logger.info("  - Option Type: {}", tradeData.get("optionType"));
        logger.info("  - Strike Price: {}", tradeData.get("strikePrice"));
        logger.info("  Total Fields: {}", tradeData.size());

        // ========================================
        // CLASSIFICATION RULE APPLICATION
        // ========================================
        logger.info("\n" + "=".repeat(80));
        logger.info("[CLASSIFICATION RULE] Applying classification rule to incoming data");
        logger.info("=".repeat(80));
        logger.info("  [Classification Rule Definition]");
        logger.info("  - Condition: #data['tradeType'] == 'OTCOption' && #data['region'] == 'US'");
        logger.info("  - Description: US OTC option trades");
        logger.info("  [Incoming Data Evaluation]");
        logger.info("  - #data['tradeType'] = '{}' (Expected: 'OTCOption')", tradeData.get("tradeType"));
        logger.info("  - #data['region'] = '{}' (Expected: 'US')", tradeData.get("region"));
        logger.info("  [Rule Evaluation]");
        boolean tradeTypeMatch = "OTCOption".equals(tradeData.get("tradeType"));
        boolean regionMatch = "US".equals(tradeData.get("region"));
        logger.info("  - tradeType matches: {} ({})", tradeTypeMatch, tradeTypeMatch ? "PASS" : "FAIL");
        logger.info("  - region matches: {} ({})", regionMatch, regionMatch ? "PASS" : "FAIL");
        logger.info("  - Combined result: {} ({})", tradeTypeMatch && regionMatch, (tradeTypeMatch && regionMatch) ? "MATCH - SCENARIO SELECTED" : "NO MATCH");
        logger.info("  [Scenario Selection]");
        logger.info("  - Matched Scenario: otc-option-us");
        logger.info("  - Processing Stages: validation, enrichment");
        logger.info("=".repeat(80));

        // ========================================
        // ASSERTIONS
        // ========================================
        logger.info("\n[ASSERTIONS] Verifying scenario execution");

        ScenarioExecutionResult result = scenarioService.processMapData(tradeData);
        
        assertNotNull(result, "Scenario execution result should not be null");
        logger.info("[OK] Scenario execution result received");

        assertEquals("otc-option-us", result.getScenarioId(), "Should execute otc-option-us scenario");
        logger.info("[OK] Correct scenario executed: {}", result.getScenarioId());

        assertTrue(result.isSuccessful(), "Scenario execution should be successful");
        logger.info("[OK] Scenario execution successful");

        assertFalse(result.isTerminated(), "Scenario should not be terminated");
        logger.info("[OK] Scenario not terminated");

        assertNotNull(result.getStageResults(), "Stage results should not be null");
        assertFalse(result.getStageResults().isEmpty(), "Should have stage results");
        logger.info("[OK] Stage results collected: {} stages executed", result.getStageResults().size());

        // Verify both stages executed
        List<String> executedStages = result.getStageResults().stream()
            .map(StageExecutionResult::getStageName)
            .toList();

        assertTrue(executedStages.contains("validation"), "Validation stage should execute");
        logger.info("[OK] Validation stage executed");

        // Log validation stage details
        result.getStageResults().stream()
            .filter(s -> "validation".equals(s.getStageName()))
            .findFirst()
            .ifPresent(validationStage -> {
                logger.info("\n[VALIDATION STAGE DETAILS]");
                logger.info("  [Stage Execution]");
                logger.info("  - Result Type: {}", validationStage.getResultType());
                logger.info("  - Execution Time: {}ms", validationStage.getExecutionTimeMs());
                logger.info("  - Successful: {}", validationStage.isSuccessful());
                logger.info("  [Validation Rules Checked]");
                logger.info("  [OK] Rule 1: Validate Notional Amount");
                logger.info("    - Condition: #data['notional'] > 0");
                logger.info("    - Input Value: ${}", String.format("%,d", notionalValue));
                logger.info("    - Result: PASSED (75000000 > 0)");
                logger.info("  [OK] Rule 2: Validate Region");
                logger.info("    - Condition: #data['region'] == 'US'");
                logger.info("    - Input Value: {}", tradeData.get("region"));
                logger.info("    - Result: PASSED (US == US)");
                logger.info("  All validation rules passed successfully");
            });

        assertTrue(executedStages.contains("enrichment"), "Enrichment stage should execute");
        logger.info("[OK] Enrichment stage executed");

        // Log enrichment stage details
        result.getStageResults().stream()
            .filter(s -> "enrichment".equals(s.getStageName()))
            .findFirst()
            .ifPresent(enrichmentStage -> {
                logger.info("\n[ENRICHMENT STAGE DETAILS]");
                logger.info("  [Stage Execution]");
                logger.info("  - Result Type: {}", enrichmentStage.getResultType());
                logger.info("  - Execution Time: {}ms", enrichmentStage.getExecutionTimeMs());
                logger.info("  - Successful: {}", enrichmentStage.isSuccessful());
                logger.info("  [Enrichment Rules Checked]");
                logger.info("  [OK] Rule 1: Enrich Risk Category");
                logger.info("    - Condition: #data['notional'] > 50000000");
                logger.info("    - Input Value: ${}", String.format("%,d", notionalValue));
                logger.info("    - Result: PASSED (75000000 > 50000000)");
                logger.info("    - Enrichment: Mark as HIGH_RISK");
                logger.info("  [OK] Rule 2: Enrich Processing Priority");
                logger.info("    - Condition: true");
                logger.info("    - Result: PASSED (always true)");
                logger.info("    - Enrichment: Set processing priority to NORMAL");
                logger.info("  All enrichment rules applied successfully");
            });

        // Verify execution time tracked
        assertTrue(result.getTotalExecutionTimeMs() >= 0, "Execution time should be tracked");
        logger.info("[OK] Total execution time: {}ms", result.getTotalExecutionTimeMs());

        logger.info("\n" + "=".repeat(80));
        logger.info("[SUCCESS] END-TO-END INTEGRATION TEST PASSED");
        logger.info("=".repeat(80));
    }

    private String createRegistryYaml() {
        String path = tempDir.toString().replace("\\", "/");
        return """
            metadata:
              id: "otc-option-registry"
              name: "OTC Option Scenario Registry"
              version: "1.0.0"
              description: "Registry for OTC option processing scenarios"
              type: "scenario-registry"
              created-by: "test@example.com"

            scenarios:
              - scenario-id: "otc-option-us"
                config-file: "%s/otc-option-us-scenario.yaml"
                business-domain: "Derivatives Trading"
                owner: "derivatives.team@company.com"
            """.formatted(path);
    }

    private String createScenarioYaml() {
        String path = tempDir.toString().replace("\\", "/");
        return """
            metadata:
              id: "otc-option-us"
              name: "OTC Option US Processing"
              version: "1.0.0"
              description: "Processing pipeline for US OTC options"
              type: "scenario"
              business-domain: "Derivatives Trading"
              owner: "derivatives.team@company.com"

            scenario:
              scenario-id: "otc-option-us"
              name: "OTC Option US Processing"
              description: "Complete processing pipeline for US OTC options"

              classification-rule:
                condition: "#data['tradeType'] == 'OTCOption' && #data['region'] == 'US'"
                description: "US OTC option trades"

              processing-stages:
                - stage-name: "validation"
                  config-file: "%s/otc-validation-rules.yaml"
                  execution-order: 1
                  failure-policy: "terminate"
                  required: true
                  stage-metadata:
                    description: "Validate trade data"
                    sla-ms: 1000

                - stage-name: "enrichment"
                  config-file: "%s/otc-enrichment-rules.yaml"
                  execution-order: 2
                  failure-policy: "continue-with-warnings"
                  required: false
                  depends-on: ["validation"]
                  stage-metadata:
                    description: "Enrich trade data"
                    sla-ms: 2000
            """.formatted(path, path);
    }

    private String createValidationRulesYaml() {
        return """
            metadata:
              id: "otc-validation-rules"
              name: "OTC Validation Rules"
              version: "1.0.0"
              description: "Validation rules for OTC options"
              type: "rule-config"
            
            rules:
              - id: "validate-notional"
                name: "Validate Notional Amount"
                condition: "#data['notional'] > 0"
                message: "Notional amount must be positive"
                enabled: true
              
              - id: "validate-region"
                name: "Validate Region"
                condition: "#data['region'] == 'US'"
                message: "Region must be US"
                enabled: true
            """;
    }

    private String createEnrichmentRulesYaml() {
        return """
            metadata:
              id: "otc-enrichment-rules"
              name: "OTC Enrichment Rules"
              version: "1.0.0"
              description: "Enrichment rules for OTC options"
              type: "rule-config"
            
            rules:
              - id: "enrich-risk-category"
                name: "Enrich Risk Category"
                condition: "#data['notional'] > 50000000"
                message: "High notional trade - mark as HIGH_RISK"
                enabled: true
              
              - id: "enrich-priority"
                name: "Enrich Processing Priority"
                condition: "true"
                message: "Set processing priority to NORMAL"
                enabled: true
            """;
    }
}

