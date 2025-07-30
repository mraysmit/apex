package dev.mars.rulesengine.demo.examples;

import dev.mars.rulesengine.core.config.yaml.YamlConfigurationLoader;
import dev.mars.rulesengine.core.config.yaml.YamlRuleChain;
import dev.mars.rulesengine.core.config.yaml.YamlRuleConfiguration;
import dev.mars.rulesengine.core.engine.context.ChainedEvaluationContext;
import dev.mars.rulesengine.core.engine.executor.RuleChainExecutor;
import dev.mars.rulesengine.core.engine.model.RuleChainResult;
import dev.mars.rulesengine.core.service.engine.ExpressionEvaluatorService;
import dev.mars.rulesengine.core.service.engine.RuleEngineService;
import dev.mars.rulesengine.demo.model.SettlementInstruction;
import dev.mars.rulesengine.demo.model.StandingInstruction;
import dev.mars.rulesengine.demo.model.SIRepairResult;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
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
 * Demonstration of Standing Instruction (SI) Auto-Repair Rules for custody settlement.
 * This demo showcases weighted rule-based decision making for automatically repairing
 * incomplete or ambiguous settlement instructions using predefined standing instructions.
 *
 * Key Features Demonstrated:
 * - Hierarchical rule prioritization (Client > Market > Instrument)
 * - Weighted decision making with confidence scoring
 * - Asian market-specific settlement conventions
 * - Comprehensive audit trail and compliance tracking
 * - Exception handling for high-value transactions
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-07-29
 * @version 1.0
 */
public class CustodyAutoRepairDemo {
    
    private final RuleChainExecutor ruleChainExecutor;
    private final YamlConfigurationLoader yamlLoader;
    
    public CustodyAutoRepairDemo() {
        // Initialize services following established pattern
        ExpressionEvaluatorService evaluatorService = new ExpressionEvaluatorService();
        RuleEngineService ruleEngineService = new RuleEngineService(evaluatorService);
        this.ruleChainExecutor = new RuleChainExecutor(ruleEngineService, evaluatorService);
        this.yamlLoader = new YamlConfigurationLoader();
    }
    
    public static void main(String[] args) {
        System.out.println("=== CUSTODY AUTO-REPAIR DEMO ===");
        System.out.println("Standing Instruction (SI) Auto-Repair Rules for Asian Markets");
        System.out.println("Demonstrates weighted rule-based decision making for settlement repair\n");
        
        CustodyAutoRepairDemo demo = new CustodyAutoRepairDemo();
        
        try {
            // Demonstrate different auto-repair scenarios
            demo.demonstrateBasicSIMatching();
            demo.demonstrateWeightedDecisionMaking();
            demo.demonstrateHierarchicalPrioritization();
            demo.demonstrateAsianMarketSpecifics();
            demo.demonstrateExceptionHandling();
            demo.demonstrateComprehensiveScenario();
            
            System.out.println("\n=== CUSTODY AUTO-REPAIR DEMO COMPLETED ===");
            
        } catch (Exception e) {
            System.err.println("Demo failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Demonstrate basic SI matching for simple client-level repair.
     */
    private void demonstrateBasicSIMatching() {
        System.out.println("1. BASIC SI MATCHING");
        System.out.println("===================");
        System.out.println("Scenario: Missing counterparty for known client\n");
        
        try {
            // Create settlement instruction with missing counterparty
            SettlementInstruction instruction = createIncompleteInstruction();
            instruction.setCounterpartyId(null); // Missing counterparty triggers repair
            instruction.addMissingField("counterpartyId");
            
            System.out.println("Original Instruction:");
            System.out.println("  Client: " + instruction.getClientId());
            System.out.println("  Market: " + instruction.getMarket());
            System.out.println("  Instrument: " + instruction.getInstrumentType());
            System.out.println("  Counterparty: " + instruction.getCounterpartyId() + " (MISSING)");
            System.out.println("  Requires Repair: " + instruction.isRequiresRepair());
            
            // Apply auto-repair using rules engine
            SIRepairResult repairResult = performAutoRepair(instruction, "basic-si-matching");
            
            // Display results
            displayRepairResults(repairResult);
            
        } catch (Exception e) {
            System.err.println("Error in basic SI matching demo: " + e.getMessage());
        }
    }
    
    /**
     * Demonstrate weighted decision making with multiple matching rules.
     */
    private void demonstrateWeightedDecisionMaking() {
        System.out.println("\n\n2. WEIGHTED DECISION MAKING");
        System.out.println("===========================");
        System.out.println("Scenario: Multiple SIs match - weighted scoring determines best choice\n");
        
        try {
            // Create instruction that matches multiple SIs
            SettlementInstruction instruction = createIncompleteInstruction();
            instruction.setCounterpartyId(null);
            instruction.setCustodianId(null);
            instruction.addMissingField("counterpartyId");
            instruction.addMissingField("custodianId");
            
            System.out.println("Original Instruction:");
            System.out.println("  Client: " + instruction.getClientId() + " (has client-specific SI, weight: 0.6)");
            System.out.println("  Market: " + instruction.getMarket() + " (has market-specific SI, weight: 0.3)");
            System.out.println("  Instrument: " + instruction.getInstrumentType() + " (has instrument-specific SI, weight: 0.1)");
            System.out.println("  Missing Fields: " + instruction.getMissingFields());
            
            // Apply weighted auto-repair
            SIRepairResult repairResult = performAutoRepair(instruction, "weighted-decision-making");
            
            // Display detailed scoring
            displayWeightedScoringResults(repairResult);
            
        } catch (Exception e) {
            System.err.println("Error in weighted decision making demo: " + e.getMessage());
        }
    }
    
    /**
     * Demonstrate hierarchical prioritization (Client > Market > Instrument).
     */
    private void demonstrateHierarchicalPrioritization() {
        System.out.println("\n\n3. HIERARCHICAL PRIORITIZATION");
        System.out.println("==============================");
        System.out.println("Scenario: Client-level rules override market and instrument rules\n");
        
        try {
            // Create instruction for premium client with specific requirements
            SettlementInstruction instruction = createPremiumClientInstruction();
            instruction.setSettlementMethod(null); // Missing settlement method
            instruction.addMissingField("settlementMethod");
            
            System.out.println("Original Instruction:");
            System.out.println("  Client: " + instruction.getClientId() + " (Premium client with specific SIs)");
            System.out.println("  Market: " + instruction.getMarket() + " (Standard market conventions)");
            System.out.println("  Instrument: " + instruction.getInstrumentType() + " (Default instrument rules)");
            System.out.println("  Settlement Method: " + instruction.getSettlementMethod() + " (MISSING)");
            
            // Apply hierarchical repair
            SIRepairResult repairResult = performAutoRepair(instruction, "hierarchical-prioritization");
            
            // Show how client-level rules take precedence
            displayHierarchicalResults(repairResult);
            
        } catch (Exception e) {
            System.err.println("Error in hierarchical prioritization demo: " + e.getMessage());
        }
    }
    
    /**
     * Demonstrate Asian market-specific settlement conventions.
     */
    private void demonstrateAsianMarketSpecifics() {
        System.out.println("\n\n4. ASIAN MARKET SPECIFICS");
        System.out.println("=========================");
        System.out.println("Scenario: Market-specific settlement conventions for different Asian markets\n");
        
        try {
            // Test different Asian markets
            String[] markets = {"JAPAN", "HONG_KONG", "SINGAPORE", "KOREA"};
            String[] instrumentTypes = {"EQUITY", "FIXED_INCOME"};
            
            for (String market : markets) {
                for (String instrumentType : instrumentTypes) {
                    System.out.println("Market: " + market + ", Instrument: " + instrumentType);
                    
                    SettlementInstruction instruction = createMarketSpecificInstruction(market, instrumentType);
                    instruction.setCustodianId(null); // Missing custodian
                    instruction.addMissingField("custodianId");
                    
                    SIRepairResult repairResult = performAutoRepair(instruction, "asian-market-specifics");
                    
                    System.out.println("  → Custodian: " + repairResult.getFieldRepairValue("custodianId"));
                    System.out.println("  → Confidence: " + String.format("%.1f%%", repairResult.getTotalConfidenceScore() * 100));
                    System.out.println();
                }
            }
            
        } catch (Exception e) {
            System.err.println("Error in Asian market specifics demo: " + e.getMessage());
        }
    }
    
    /**
     * Demonstrate exception handling for high-value transactions and opt-outs.
     */
    private void demonstrateExceptionHandling() {
        System.out.println("\n\n5. EXCEPTION HANDLING");
        System.out.println("=====================");
        System.out.println("Scenario: High-value transactions and client opt-outs require manual intervention\n");
        
        try {
            // High-value transaction
            SettlementInstruction highValueInstruction = createHighValueInstruction();
            highValueInstruction.setCounterpartyId(null);
            highValueInstruction.addMissingField("counterpartyId");
            
            System.out.println("High-Value Transaction:");
            System.out.println("  Amount: " + highValueInstruction.getSettlementAmount());
            System.out.println("  High Value Flag: " + highValueInstruction.isHighValueTransaction());
            System.out.println("  Eligible for Auto-Repair: " + highValueInstruction.isEligibleForAutoRepair());
            
            SIRepairResult highValueResult = performAutoRepair(highValueInstruction, "exception-handling");
            System.out.println("  → Result: " + highValueResult.getRepairStatus());
            System.out.println("  → Reason: " + highValueResult.getFailureReason());
            
            // Client opt-out
            SettlementInstruction optOutInstruction = createOptOutInstruction();
            optOutInstruction.setCounterpartyId(null);
            optOutInstruction.addMissingField("counterpartyId");
            
            System.out.println("\nClient Opt-Out:");
            System.out.println("  Client: " + optOutInstruction.getClientId());
            System.out.println("  Opt-Out Flag: " + optOutInstruction.isClientOptOut());
            System.out.println("  Eligible for Auto-Repair: " + optOutInstruction.isEligibleForAutoRepair());
            
            SIRepairResult optOutResult = performAutoRepair(optOutInstruction, "exception-handling");
            System.out.println("  → Result: " + optOutResult.getRepairStatus());
            System.out.println("  → Reason: " + optOutResult.getFailureReason());
            
        } catch (Exception e) {
            System.err.println("Error in exception handling demo: " + e.getMessage());
        }
    }
