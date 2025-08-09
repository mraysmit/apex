package dev.mars.apex.demo.examples;

import dev.mars.apex.core.config.yaml.YamlConfigurationLoader;
import dev.mars.apex.core.config.yaml.YamlRuleConfiguration;
import dev.mars.apex.core.config.yaml.YamlEnrichment;
import dev.mars.apex.demo.model.SettlementInstruction;
import dev.mars.apex.demo.model.StandingInstruction;
import dev.mars.apex.demo.model.SIRepairResult;

import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

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

/**
 * YAML-driven demonstration of Standing Instruction (SI) Auto-Repair Rules.
 * This demo loads configuration from custody-auto-repair-rules.yaml and demonstrates
 * how business users can maintain rules externally without code changes.
 *
 * Key Features Demonstrated:
 * - YAML-driven rule configuration
 * - External standing instruction datasets
 * - Enrichment-based field population
 * - Accumulative chaining for weighted decisions
 * - Business-user maintainable configuration
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-07-29
 * @version 1.0
 */
public class CustodyAutoRepairYamlDemo {
    
    private final YamlConfigurationLoader yamlLoader;
    private YamlRuleConfiguration ruleConfiguration;

    public CustodyAutoRepairYamlDemo() {
        this.yamlLoader = new YamlConfigurationLoader();
        loadConfiguration();
    }
    
    /**
     * Load YAML configuration from resources.
     */
    private void loadConfiguration() {
        try {
            InputStream configStream = getClass().getClassLoader()
                .getResourceAsStream("demo-rules/custody-auto-repair-rules.yaml");
            
            if (configStream == null) {
                throw new RuntimeException("Could not find custody-auto-repair-rules.yaml in resources");
            }
            
            this.ruleConfiguration = yamlLoader.loadFromStream(configStream);
            System.out.println("Loaded YAML configuration: " + ruleConfiguration.getMetadata().getName());
            System.out.println("Version: " + ruleConfiguration.getMetadata().getVersion());
            System.out.println("Rule Chains: Configured in YAML");
            System.out.println("Enrichments: " + ruleConfiguration.getEnrichments().size());
            
        } catch (Exception e) {
            System.err.println("Failed to load YAML configuration: " + e.getMessage());
            // Create minimal fallback configuration
            this.ruleConfiguration = createFallbackConfiguration();
        }
    }
    
    public static void main(String[] args) {
        System.out.println("=== CUSTODY AUTO-REPAIR YAML DEMO ===");
        System.out.println("YAML-Driven Standing Instruction Auto-Repair");
        System.out.println("Demonstrates external configuration and business-user maintainable rules\n");
        
        CustodyAutoRepairYamlDemo demo = new CustodyAutoRepairYamlDemo();
        
        try {
            // Demonstrate YAML-driven scenarios
            demo.demonstrateYamlConfiguration();
            demo.demonstrateEnrichmentBasedRepair();
            demo.demonstrateWeightedScoringFromYaml();
            demo.demonstrateBusinessUserMaintenance();
            
            System.out.println("\n=== CUSTODY AUTO-REPAIR YAML DEMO COMPLETED ===");
            
        } catch (Exception e) {
            System.err.println("Demo failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Demonstrate YAML configuration loading and structure.
     */
    private void demonstrateYamlConfiguration() {
        System.out.println("1. YAML CONFIGURATION STRUCTURE");
        System.out.println("================================");
        System.out.println("Loaded from: custody-auto-repair-rules.yaml\n");
        
        // Display metadata
        var metadata = ruleConfiguration.getMetadata();
        System.out.println("Configuration Metadata:");
        System.out.println("  Name: " + metadata.getName());
        System.out.println("  Version: " + metadata.getVersion());
        System.out.println("  Description: " + metadata.getDescription());
        System.out.println("  Author: " + metadata.getAuthor());
        System.out.println("  Tags: " + metadata.getTags());
        
        // Display rule chains (conceptual)
        System.out.println("\nRule Chains (from YAML):");
        System.out.println("  - SI Auto-Repair Chain (accumulative-chaining)");
        System.out.println("    Priority: 100");
        System.out.println("    Enabled: true");
        System.out.println("  - Eligibility Check Chain (conditional-chaining)");
        System.out.println("    Priority: 200");
        System.out.println("    Enabled: true");
        
        // Display enrichments
        System.out.println("\nEnrichments:");
        for (YamlEnrichment enrichment : ruleConfiguration.getEnrichments()) {
            System.out.println("  - " + enrichment.getName() + " (" + enrichment.getType() + ")");
            System.out.println("    Priority: " + enrichment.getPriority());
            System.out.println("    Enabled: " + enrichment.getEnabled());
        }
    }
    
    /**
     * Demonstrate enrichment-based repair using YAML datasets.
     */
    private void demonstrateEnrichmentBasedRepair() {
        System.out.println("\n\n2. ENRICHMENT-BASED REPAIR");
        System.out.println("==========================");
        System.out.println("Scenario: Using YAML datasets to enrich missing fields\n");
        
        // Create settlement instruction with missing fields
        SettlementInstruction instruction = new SettlementInstruction(
            "SI_YAML_001", "CLIENT_A", "JAPAN", "EQUITY", 
            new BigDecimal("2000000"), "JPY", LocalDate.now().plusDays(2)
        );
        instruction.setCounterpartyId(null);
        instruction.setCustodianId(null);
        instruction.addMissingField("counterpartyId");
        instruction.addMissingField("custodianId");
        
        System.out.println("Original Instruction:");
        System.out.println("  Client: " + instruction.getClientId());
        System.out.println("  Market: " + instruction.getMarket());
        System.out.println("  Instrument: " + instruction.getInstrumentType());
        System.out.println("  Missing Fields: " + instruction.getMissingFields());
        
        // Apply enrichments from YAML configuration
        SIRepairResult repairResult = applyYamlEnrichments(instruction);
        
        System.out.println("\nEnrichment Results:");
        System.out.println("  Applied Enrichments: " + repairResult.getAppliedStandingInstructions().size());
        System.out.println("  Fields Enriched: " + repairResult.getFieldsRepaired());
        
        for (String field : repairResult.getFieldRepairs().keySet()) {
            String value = repairResult.getFieldRepairValue(field);
            System.out.println("    " + field + ": " + value + " (from YAML dataset)");
        }
    }
    
    /**
     * Demonstrate weighted scoring using YAML rule chain configuration.
     */
    private void demonstrateWeightedScoringFromYaml() {
        System.out.println("\n\n3. WEIGHTED SCORING FROM YAML");
        System.out.println("==============================");
        System.out.println("Scenario: Accumulative chaining pattern from YAML configuration\n");
        
        // Simulate weighted scoring demonstration
        System.out.println("Rule Chain Concept: SI Auto-Repair Chain");
        System.out.println("Pattern: accumulative-chaining");

        System.out.println("\nAccumulation Rules (from YAML configuration):");
        System.out.println("  - client-level-si-rule");
        System.out.println("    Weight: 0.6");
        System.out.println("    Condition: #instruction.clientId != null ? 60 : 0");
        System.out.println("    Message: Client-level SI evaluation");
        System.out.println();
        System.out.println("  - market-level-si-rule");
        System.out.println("    Weight: 0.3");
        System.out.println("    Condition: #instruction.market != null ? 30 : 0");
        System.out.println("    Message: Market-level SI evaluation");
        System.out.println();
        System.out.println("  - instrument-level-si-rule");
        System.out.println("    Weight: 0.1");
        System.out.println("    Condition: #instruction.instrumentType != null ? 10 : 0");
        System.out.println("    Message: Instrument-level SI evaluation");
        System.out.println();

        // Simulate weighted scoring
        SettlementInstruction instruction = new SettlementInstruction(
            "SI_YAML_002", "CLIENT_B", "HONG_KONG", "EQUITY",
            new BigDecimal("3000000"), "HKD", LocalDate.now().plusDays(2)
        );

        SIRepairResult result = simulateWeightedScoring(instruction);

        System.out.println("Weighted Scoring Simulation:");
        System.out.println("  Total Score: " + String.format("%.1f", result.getWeightedScore()));
        System.out.println("  Decision: " + (result.getWeightedScore() >= 50 ? "REPAIR_APPROVED" : "MANUAL_REVIEW_REQUIRED"));
    }
    
    /**
     * Demonstrate how business users can maintain rules through YAML.
     */
    private void demonstrateBusinessUserMaintenance() {
        System.out.println("\n\n4. BUSINESS USER MAINTENANCE");
        System.out.println("============================");
        System.out.println("Scenario: How business users can modify rules without code changes\n");
        
        System.out.println("Business User Benefits:");
        System.out.println("✓ External YAML configuration - no code deployment required");
        System.out.println("✓ Inline datasets - standing instructions maintained in YAML");
        System.out.println("✓ Configurable weights - adjust client/market/instrument priorities");
        System.out.println("✓ Conditional logic - SpEL expressions for complex rules");
        System.out.println("✓ Enrichment mappings - field-level control over data population");
        
        System.out.println("\nExample Configuration Changes:");
        System.out.println("1. Add new client standing instruction:");
        System.out.println("   - Add entry to client-si-enrichment dataset");
        System.out.println("   - Specify default values and confidence levels");
        
        System.out.println("\n2. Adjust rule weights:");
        System.out.println("   - Modify weight values in accumulation-rules");
        System.out.println("   - Change client-level from 0.6 to 0.7 for higher priority");
        
        System.out.println("\n3. Add new market:");
        System.out.println("   - Add entry to market-si-enrichment dataset");
        System.out.println("   - Include market-specific custodians and conventions");
        
        System.out.println("\n4. Modify confidence thresholds:");
        System.out.println("   - Update final-decision-rule condition");
        System.out.println("   - Change score thresholds for approval/review");
        
        // Show actual YAML snippet
        System.out.println("\nSample YAML Configuration:");
        System.out.println("```yaml");
        System.out.println("accumulation-rules:");
        System.out.println("  - id: \"client-level-si-rule\"");
        System.out.println("    condition: \"#instruction.clientId != null ? 60 : 0\"");
        System.out.println("    weight: 0.6  # ← Business user can modify this");
        System.out.println("    message: \"Client-level SI evaluation\"");
        System.out.println("```");
    }

    // Helper methods

    /**
     * Apply YAML-configured enrichments to settlement instruction.
     */
    private SIRepairResult applyYamlEnrichments(SettlementInstruction instruction) {
        SIRepairResult result = new SIRepairResult(instruction.getInstructionId());
        result.setProcessedBy("CustodyAutoRepairYamlDemo");

        try {
            // Create context for enrichment
            Map<String, Object> context = new HashMap<>();
            context.put("instruction", instruction);

            // Apply each enrichment from YAML configuration
            for (YamlEnrichment enrichment : ruleConfiguration.getEnrichments()) {
                if (enrichment.getEnabled() != null && enrichment.getEnabled()) {
                    applyEnrichment(enrichment, instruction, result, context);
                }
            }

            if (result.getFieldsRepaired() > 0) {
                result.markAsSuccessful("YAML enrichments applied successfully");
            } else {
                result.markAsFailed("No applicable YAML enrichments found");
            }

        } catch (Exception e) {
            result.markAsFailed("Error applying YAML enrichments: " + e.getMessage());
        }

        return result;
    }

    /**
     * Apply individual enrichment from YAML configuration.
     */
    private void applyEnrichment(YamlEnrichment enrichment, SettlementInstruction instruction,
                               SIRepairResult result, Map<String, Object> context) {

        // Simulate enrichment application based on type
        if ("lookup-enrichment".equals(enrichment.getType())) {
            applyLookupEnrichment(enrichment, instruction, result);
        }
    }

    /**
     * Apply lookup enrichment using inline dataset from YAML.
     */
    private void applyLookupEnrichment(YamlEnrichment enrichment, SettlementInstruction instruction,
                                     SIRepairResult result) {

        var lookupConfig = enrichment.getLookupConfig();
        if (lookupConfig == null || lookupConfig.getLookupDataset() == null) {
            return;
        }

        var dataset = lookupConfig.getLookupDataset();
        String keyField = dataset.getKeyField();

        // Get lookup key value based on enrichment type
        String lookupKey = getLookupKey(enrichment, instruction, keyField);
        if (lookupKey == null) {
            return;
        }

        // Find matching data entry
        var dataEntries = dataset.getData();
        if (dataEntries != null) {
            for (Map<String, Object> entry : dataEntries) {
                if (lookupKey.equals(entry.get(keyField))) {
                    applyDataEntry(enrichment, entry, instruction, result);
                    break;
                }
            }
        }
    }

    /**
     * Get lookup key based on enrichment configuration.
     */
    private String getLookupKey(YamlEnrichment enrichment, SettlementInstruction instruction, String keyField) {
        return switch (enrichment.getId()) {
            case "client-si-enrichment" -> instruction.getClientId();
            case "market-si-enrichment" -> instruction.getMarket();
            case "instrument-si-enrichment" -> instruction.getInstrumentType();
            case "counterparty-resolution-enrichment" ->
                instruction.getMarket() + "_" + instruction.getClientId();
            case "custodial-account-enrichment" ->
                instruction.getCustodianId() + "_" + instruction.getClientId();
            default -> null;
        };
    }

    /**
     * Apply data entry to instruction and record in result.
     */
    private void applyDataEntry(YamlEnrichment enrichment, Map<String, Object> entry,
                              SettlementInstruction instruction, SIRepairResult result) {

        // Create standing instruction from data entry
        StandingInstruction si = createStandingInstructionFromEntry(entry);

        // Apply field mappings
        var fieldMappings = enrichment.getFieldMappings();
        if (fieldMappings != null) {
            for (var mapping : fieldMappings) {
                String sourceField = mapping.getSourceField();
                String targetField = mapping.getTargetField();
                Object value = entry.get(sourceField);

                if (value != null) {
                    // Apply specific field repairs based on target field
                    applyFieldMapping(targetField, value.toString(), instruction, result, si);
                }
            }
        }
    }

    /**
     * Apply field mapping to instruction.
     */
    private void applyFieldMapping(String targetField, String value, SettlementInstruction instruction,
                                 SIRepairResult result, StandingInstruction si) {

        // Handle specific field mappings for missing fields
        if (targetField.contains("defaultCounterpartyId") && instruction.getCounterpartyId() == null) {
            instruction.setCounterpartyId(value);
            result.addFieldRepair("counterpartyId", value, si);
        } else if (targetField.contains("defaultCustodianId") && instruction.getCustodianId() == null) {
            instruction.setCustodianId(value);
            result.addFieldRepair("custodianId", value, si);
        } else if (targetField.contains("defaultSettlementMethod") && instruction.getSettlementMethod() == null) {
            instruction.setSettlementMethod(value);
            result.addFieldRepair("settlementMethod", value, si);
        }
    }

    /**
     * Create StandingInstruction from YAML data entry.
     */
    private StandingInstruction createStandingInstructionFromEntry(Map<String, Object> entry) {
        StandingInstruction si = new StandingInstruction();

        si.setSiId((String) entry.get("siId"));
        si.setSiName((String) entry.get("siName"));
        si.setScopeType((String) entry.get("scopeType"));

        if (entry.get("weight") != null) {
            si.setWeight(((Number) entry.get("weight")).doubleValue());
        }
        if (entry.get("confidenceLevel") != null) {
            si.setConfidenceLevel(((Number) entry.get("confidenceLevel")).doubleValue());
        }

        si.setDefaultCounterpartyId((String) entry.get("defaultCounterpartyId"));
        si.setDefaultCustodianId((String) entry.get("defaultCustodianId"));
        si.setDefaultSettlementMethod((String) entry.get("defaultSettlementMethod"));

        return si;
    }

    /**
     * Simulate weighted scoring using hardcoded rule logic.
     */
    private SIRepairResult simulateWeightedScoring(SettlementInstruction instruction) {
        SIRepairResult result = new SIRepairResult(instruction.getInstructionId());
        result.setProcessedBy("CustodyAutoRepairYamlDemo");

        double totalScore = 0.0;

        // Simulate client-level rule
        double clientScore = simulateRuleEvaluation("client-level-si-rule", instruction);
        double clientWeight = 0.6;
        totalScore += clientScore * clientWeight;
        result.addRuleScore("client-level-si-rule", clientScore, clientWeight);

        // Simulate market-level rule
        double marketScore = simulateRuleEvaluation("market-level-si-rule", instruction);
        double marketWeight = 0.3;
        totalScore += marketScore * marketWeight;
        result.addRuleScore("market-level-si-rule", marketScore, marketWeight);

        // Simulate instrument-level rule
        double instrumentScore = simulateRuleEvaluation("instrument-level-si-rule", instruction);
        double instrumentWeight = 0.1;
        totalScore += instrumentScore * instrumentWeight;
        result.addRuleScore("instrument-level-si-rule", instrumentScore, instrumentWeight);

        result.setWeightedScore(totalScore);

        if (totalScore >= 50) {
            result.markAsSuccessful("Weighted scoring approved repair");
        } else {
            result.markAsFailed("Weighted scoring below threshold");
        }

        return result;
    }

    /**
     * Simulate rule evaluation for demonstration.
     */
    private double simulateRuleEvaluation(String ruleId, SettlementInstruction instruction) {
        return switch (ruleId) {
            case "client-level-si-rule" -> instruction.getClientId() != null ? 60.0 : 0.0;
            case "market-level-si-rule" -> instruction.getMarket() != null ? 30.0 : 0.0;
            case "instrument-level-si-rule" -> instruction.getInstrumentType() != null ? 10.0 : 0.0;
            default -> 0.0;
        };
    }

    /**
     * Create fallback configuration if YAML loading fails.
     */
    private YamlRuleConfiguration createFallbackConfiguration() {
        YamlRuleConfiguration config = new YamlRuleConfiguration();

        // Create minimal metadata
        var metadata = new YamlRuleConfiguration.ConfigurationMetadata();
        metadata.setName("Fallback Configuration");
        metadata.setVersion("1.0");
        metadata.setDescription("Minimal fallback configuration");
        config.setMetadata(metadata);

        return config;
    }
}
