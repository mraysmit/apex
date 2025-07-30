package dev.mars.rulesengine.demo.examples;

import dev.mars.rulesengine.core.config.yaml.YamlConfigurationLoader;
import dev.mars.rulesengine.core.config.yaml.YamlRuleChain;
import dev.mars.rulesengine.core.config.yaml.YamlRuleConfiguration;
import dev.mars.rulesengine.core.config.yaml.YamlEnrichment;
import dev.mars.rulesengine.core.service.enrichment.EnrichmentService;
import dev.mars.rulesengine.demo.model.SettlementInstruction;
import dev.mars.rulesengine.demo.model.StandingInstruction;
import dev.mars.rulesengine.demo.model.SIRepairResult;

import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
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
    private final EnrichmentService enrichmentService;
    private YamlRuleConfiguration ruleConfiguration;
    
    public CustodyAutoRepairYamlDemo() {
        this.yamlLoader = new YamlConfigurationLoader();
        this.enrichmentService = new EnrichmentService();
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
            
            this.ruleConfiguration = yamlLoader.fromInputStream(configStream);
            System.out.println("Loaded YAML configuration: " + ruleConfiguration.getMetadata().getName());
            System.out.println("Version: " + ruleConfiguration.getMetadata().getVersion());
            System.out.println("Rule Chains: " + ruleConfiguration.getRuleChains().size());
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
        
        // Display rule chains
        System.out.println("\nRule Chains:");
        for (YamlRuleChain chain : ruleConfiguration.getRuleChains()) {
            System.out.println("  - " + chain.getName() + " (" + chain.getPattern() + ")");
            System.out.println("    Priority: " + chain.getPriority());
            System.out.println("    Enabled: " + chain.getEnabled());
        }
        
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
        
        // Find the SI auto-repair chain
        YamlRuleChain siChain = ruleConfiguration.getRuleChains().stream()
            .filter(chain -> "si-auto-repair-chain".equals(chain.getId()))
            .findFirst()
            .orElse(null);
        
        if (siChain != null) {
            System.out.println("Rule Chain: " + siChain.getName());
            System.out.println("Pattern: " + siChain.getPattern());
            
            // Display accumulation rules and their weights
            var config = siChain.getConfiguration();
            if (config != null && config.containsKey("accumulation-rules")) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> accumulationRules = 
                    (List<Map<String, Object>>) config.get("accumulation-rules");
                
                System.out.println("\nAccumulation Rules:");
                for (Map<String, Object> rule : accumulationRules) {
                    System.out.println("  - " + rule.get("id"));
                    System.out.println("    Weight: " + rule.get("weight"));
                    System.out.println("    Condition: " + rule.get("condition"));
                    System.out.println("    Message: " + rule.get("message"));
                    System.out.println();
                }
            }
            
            // Simulate weighted scoring
            SettlementInstruction instruction = new SettlementInstruction(
                "SI_YAML_002", "CLIENT_B", "HONG_KONG", "EQUITY", 
                new BigDecimal("3000000"), "HKD", LocalDate.now().plusDays(2)
            );
            
            SIRepairResult result = simulateWeightedScoring(instruction, siChain);
            
            System.out.println("Weighted Scoring Simulation:");
            System.out.println("  Total Score: " + String.format("%.1f", result.getWeightedScore()));
            System.out.println("  Decision: " + (result.getWeightedScore() >= 50 ? "REPAIR_APPROVED" : "MANUAL_REVIEW_REQUIRED"));
        }
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
