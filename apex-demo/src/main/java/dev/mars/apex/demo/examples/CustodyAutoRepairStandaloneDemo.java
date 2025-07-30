package dev.mars.apex.demo.examples;

import dev.mars.apex.demo.model.SettlementInstruction;
import dev.mars.apex.demo.model.StandingInstruction;
import dev.mars.apex.demo.model.SIRepairResult;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

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
 * Standalone demonstration of Standing Instruction (SI) Auto-Repair functionality
 * without dependencies on the rules engine. This demo shows the core concepts
 * and model classes working together to demonstrate weighted rule-based decision
 * making for custody settlement auto-repair.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-07-29
 * @version 1.0
 */
public class CustodyAutoRepairStandaloneDemo {
    
    public static void main(String[] args) {
        System.out.println("=== CUSTODY AUTO-REPAIR STANDALONE DEMO ===");
        System.out.println("Standing Instruction (SI) Auto-Repair for Asian Markets");
        System.out.println("Demonstrates weighted rule-based decision making\n");
        
        CustodyAutoRepairStandaloneDemo demo = new CustodyAutoRepairStandaloneDemo();
        
        // Run demonstration scenarios
        demo.demonstrateBasicSIMatching();
        demo.demonstrateWeightedDecisionMaking();
        demo.demonstrateHierarchicalPrioritization();
        demo.demonstrateAsianMarketSpecifics();
        demo.demonstrateExceptionHandling();
        
        System.out.println("\n=== CUSTODY AUTO-REPAIR DEMO COMPLETED ===");
    }
    
    /**
     * Demonstrate basic SI matching for simple client-level repair.
     */
    private void demonstrateBasicSIMatching() {
        System.out.println("1. BASIC SI MATCHING");
        System.out.println("===================");
        System.out.println("Scenario: Missing counterparty for known client\n");
        
        // Create settlement instruction with missing counterparty
        SettlementInstruction instruction = new SettlementInstruction(
            "SI001", "CLIENT_A", "JAPAN", "EQUITY", 
            new BigDecimal("1000000"), "JPY", LocalDate.now().plusDays(2)
        );
        instruction.setCounterpartyId(null); // Missing counterparty triggers repair
        instruction.addMissingField("counterpartyId");
        
        System.out.println("Original Instruction:");
        System.out.println("  Client: " + instruction.getClientId());
        System.out.println("  Market: " + instruction.getMarket());
        System.out.println("  Instrument: " + instruction.getInstrumentType());
        System.out.println("  Counterparty: " + instruction.getCounterpartyId() + " (MISSING)");
        System.out.println("  Requires Repair: " + instruction.isRequiresRepair());
        
        // Create client-specific standing instruction
        StandingInstruction clientSI = new StandingInstruction("SI_CLIENT_A", "CLIENT_A", "Client A Default SI");
        clientSI.setDefaultCounterpartyId("CP_CLIENT_A_DEFAULT");
        clientSI.setDefaultCounterpartyName("Client A Default Counterparty");
        
        // Apply auto-repair
        SIRepairResult repairResult = performBasicRepair(instruction, clientSI);
        
        // Display results
        System.out.println("\nRepair Results:");
        System.out.println("  Status: " + repairResult.getRepairStatus());
        System.out.println("  Fields Repaired: " + repairResult.getFieldsRepaired());
        System.out.println("  Applied SI: " + clientSI.getSiName() + " (Weight: " + clientSI.getWeight() + ")");
        System.out.println("  Counterparty: " + instruction.getCounterpartyId() + " (REPAIRED)");
    }
    
    /**
     * Demonstrate weighted decision making with multiple matching rules.
     */
    private void demonstrateWeightedDecisionMaking() {
        System.out.println("\n\n2. WEIGHTED DECISION MAKING");
        System.out.println("===========================");
        System.out.println("Scenario: Multiple SIs match - weighted scoring determines best choice\n");
        
        // Create instruction that could match multiple SIs
        SettlementInstruction instruction = new SettlementInstruction(
            "SI002", "CLIENT_B", "HONG_KONG", "EQUITY", 
            new BigDecimal("2000000"), "HKD", LocalDate.now().plusDays(2)
        );
        instruction.setCounterpartyId(null);
        instruction.setCustodianId(null);
        instruction.addMissingField("counterpartyId");
        instruction.addMissingField("custodianId");
        
        System.out.println("Original Instruction:");
        System.out.println("  Client: " + instruction.getClientId());
        System.out.println("  Market: " + instruction.getMarket());
        System.out.println("  Missing Fields: " + instruction.getMissingFields());
        
        // Create multiple applicable SIs with different weights
        List<StandingInstruction> applicableSIs = new ArrayList<>();
        
        // Client-specific SI (highest weight: 0.6)
        StandingInstruction clientSI = new StandingInstruction("SI_CLIENT_B", "CLIENT_B", "Client B Specific SI");
        clientSI.setDefaultCounterpartyId("CP_CLIENT_B_PREMIUM");
        applicableSIs.add(clientSI);
        
        // Market-specific SI (medium weight: 0.3)
        StandingInstruction marketSI = new StandingInstruction("SI_HONG_KONG", "HONG_KONG", "Hong Kong Market SI", true);
        marketSI.setDefaultCustodianId("CUST_HK_STANDARD");
        marketSI.setDefaultCounterpartyId("CP_HK_STANDARD");
        applicableSIs.add(marketSI);
        
        // Instrument-specific SI (lowest weight: 0.1)
        StandingInstruction instrumentSI = new StandingInstruction("SI_EQUITY", "EQUITY", "Equity Instrument SI", 0);
        instrumentSI.setDefaultSettlementMethod("DVP");
        applicableSIs.add(instrumentSI);
        
        // Apply weighted repair
        SIRepairResult repairResult = performWeightedRepair(instruction, applicableSIs);
        
        // Display weighted scoring results
        System.out.println("\nWeighted Scoring Results:");
        System.out.println("  Total Weighted Score: " + String.format("%.2f", repairResult.getWeightedScore()));
        System.out.println("  Applied SIs: " + repairResult.getAppliedStandingInstructions().size());
        
        for (StandingInstruction si : repairResult.getAppliedStandingInstructions()) {
            System.out.println("    " + si.getSiName() + " (Scope: " + si.getScopeType() + 
                             ", Weight: " + si.getWeight() + ")");
        }
        
        System.out.println("  Final Repairs:");
        for (String field : repairResult.getFieldRepairs().keySet()) {
            String value = repairResult.getFieldRepairValue(field);
            StandingInstruction source = repairResult.getFieldRepairSource(field);
            System.out.println("    " + field + ": " + value + " (from " + source.getSiName() + ")");
        }
    }
    
    /**
     * Demonstrate hierarchical prioritization (Client > Market > Instrument).
     */
    private void demonstrateHierarchicalPrioritization() {
        System.out.println("\n\n3. HIERARCHICAL PRIORITIZATION");
        System.out.println("==============================");
        System.out.println("Scenario: Client-level rules override market and instrument rules\n");
        
        // Create instruction for premium client
        SettlementInstruction instruction = new SettlementInstruction(
            "SI003", "PREMIUM_CLIENT_X", "SINGAPORE", "FIXED_INCOME", 
            new BigDecimal("5000000"), "SGD", LocalDate.now().plusDays(1)
        );
        instruction.setSettlementMethod(null);
        instruction.setCustodianId(null);
        instruction.addMissingField("settlementMethod");
        instruction.addMissingField("custodianId");
        
        System.out.println("Original Instruction:");
        System.out.println("  Client: " + instruction.getClientId() + " (Premium client)");
        System.out.println("  Market: " + instruction.getMarket() + " (Standard market)");
        System.out.println("  Instrument: " + instruction.getInstrumentType() + " (Standard instrument)");
        System.out.println("  Missing Fields: " + instruction.getMissingFields());
        
        // Create hierarchical SIs
        List<StandingInstruction> hierarchicalSIs = new ArrayList<>();
        
        // Premium client SI (overrides everything)
        StandingInstruction premiumClientSI = new StandingInstruction("SI_PREMIUM_X", "PREMIUM_CLIENT_X", "Premium Client X SI");
        premiumClientSI.setDefaultSettlementMethod("DVP_PREMIUM");
        premiumClientSI.setDefaultCustodianId("CUST_PREMIUM_GLOBAL");
        hierarchicalSIs.add(premiumClientSI);
        
        // Market SI (would apply if no client SI)
        StandingInstruction singaporeSI = new StandingInstruction("SI_SINGAPORE", "SINGAPORE", "Singapore Market SI", true);
        singaporeSI.setDefaultSettlementMethod("DVP_STANDARD");
        singaporeSI.setDefaultCustodianId("CUST_SG_STANDARD");
        hierarchicalSIs.add(singaporeSI);
        
        // Apply hierarchical repair
        SIRepairResult repairResult = performHierarchicalRepair(instruction, hierarchicalSIs);
        
        System.out.println("\nHierarchical Results:");
        System.out.println("  Decision Logic: Client-level SIs take precedence");
        System.out.println("  Winning SI: " + repairResult.getAppliedStandingInstructions().get(0).getSiName());
        System.out.println("  Settlement Method: " + instruction.getSettlementMethod() + " (Premium service)");
        System.out.println("  Custodian: " + instruction.getCustodianId() + " (Global premium custodian)");
    }
    
    /**
     * Demonstrate Asian market-specific settlement conventions.
     */
    private void demonstrateAsianMarketSpecifics() {
        System.out.println("\n\n4. ASIAN MARKET SPECIFICS");
        System.out.println("=========================");
        System.out.println("Scenario: Market-specific settlement conventions\n");
        
        String[] markets = {"JAPAN", "HONG_KONG", "SINGAPORE", "KOREA"};
        String[] currencies = {"JPY", "HKD", "SGD", "KRW"};
        String[] custodians = {"CUST_JP_STANDARD", "CUST_HK_STANDARD", "CUST_SG_STANDARD", "CUST_KR_STANDARD"};
        
        for (int i = 0; i < markets.length; i++) {
            String market = markets[i];
            String currency = currencies[i];
            String custodian = custodians[i];
            
            System.out.println("Market: " + market);
            
            SettlementInstruction instruction = new SettlementInstruction(
                "SI_" + market, "CLIENT_GENERIC", market, "EQUITY",
                new BigDecimal("1000000"), currency, LocalDate.now().plusDays(2)
            );
            instruction.setCustodianId(null);
            instruction.addMissingField("custodianId");
            
            StandingInstruction marketSI = new StandingInstruction("SI_" + market, market, market + " Market SI", true);
            marketSI.setDefaultCustodianId(custodian);
            marketSI.setDefaultSettlementMethod("DVP");
            
            SIRepairResult result = performBasicRepair(instruction, marketSI);
            
            System.out.println("  → Custodian: " + instruction.getCustodianId());
            System.out.println("  → Settlement Method: " + marketSI.getDefaultSettlementMethod());
            System.out.println("  → Confidence: " + String.format("%.1f%%", marketSI.getConfidenceLevel() * 100));
            System.out.println();
        }
    }

    /**
     * Demonstrate exception handling for high-value transactions and opt-outs.
     */
    private void demonstrateExceptionHandling() {
        System.out.println("\n5. EXCEPTION HANDLING");
        System.out.println("=====================");
        System.out.println("Scenario: High-value transactions and client opt-outs\n");

        // High-value transaction
        SettlementInstruction highValueInstruction = new SettlementInstruction(
            "SI_HIGH_VALUE", "CLIENT_C", "JAPAN", "EQUITY",
            new BigDecimal("50000000"), "JPY", LocalDate.now().plusDays(3)
        );
        highValueInstruction.setHighValueTransaction(true);
        highValueInstruction.setCounterpartyId(null);
        highValueInstruction.addMissingField("counterpartyId");

        System.out.println("High-Value Transaction:");
        System.out.println("  Amount: " + highValueInstruction.getSettlementAmount());
        System.out.println("  High Value Flag: " + highValueInstruction.isHighValueTransaction());
        System.out.println("  Eligible for Auto-Repair: " + highValueInstruction.isEligibleForAutoRepair());

        SIRepairResult highValueResult = checkEligibilityAndSkip(highValueInstruction, "High-value transaction requires manual intervention");
        System.out.println("  → Result: " + highValueResult.getRepairStatus());
        System.out.println("  → Reason: " + highValueResult.getFailureReason());

        // Client opt-out
        SettlementInstruction optOutInstruction = new SettlementInstruction(
            "SI_OPT_OUT", "CLIENT_OPT_OUT", "KOREA", "EQUITY",
            new BigDecimal("3000000"), "KRW", LocalDate.now().plusDays(2)
        );
        optOutInstruction.setClientOptOut(true);
        optOutInstruction.setCounterpartyId(null);
        optOutInstruction.addMissingField("counterpartyId");

        System.out.println("\nClient Opt-Out:");
        System.out.println("  Client: " + optOutInstruction.getClientId());
        System.out.println("  Opt-Out Flag: " + optOutInstruction.isClientOptOut());
        System.out.println("  Eligible for Auto-Repair: " + optOutInstruction.isEligibleForAutoRepair());

        SIRepairResult optOutResult = checkEligibilityAndSkip(optOutInstruction, "Client has opted out of auto-repair");
        System.out.println("  → Result: " + optOutResult.getRepairStatus());
        System.out.println("  → Reason: " + optOutResult.getFailureReason());
    }

    // Helper methods for repair operations

    /**
     * Perform basic repair using a single standing instruction.
     */
    private SIRepairResult performBasicRepair(SettlementInstruction instruction, StandingInstruction si) {
        SIRepairResult result = new SIRepairResult(instruction.getInstructionId());
        result.setProcessedBy("CustodyAutoRepairStandaloneDemo");

        if (!instruction.isEligibleForAutoRepair()) {
            result.markAsSkipped("Instruction not eligible for auto-repair");
            return result;
        }

        if (si.isApplicableToInstruction(instruction)) {
            applyStandingInstruction(result, si, instruction);
            result.addRuleScore(si.getSiId(), 1.0, si.getWeight());
            result.markAsSuccessful("Basic repair completed using " + si.getSiName());
        } else {
            result.markAsFailed("Standing instruction not applicable to this instruction");
        }

        result.calculateFinalScores();
        return result;
    }

    /**
     * Perform weighted repair using multiple standing instructions.
     */
    private SIRepairResult performWeightedRepair(SettlementInstruction instruction, List<StandingInstruction> sis) {
        SIRepairResult result = new SIRepairResult(instruction.getInstructionId());
        result.setProcessedBy("CustodyAutoRepairStandaloneDemo");

        if (!instruction.isEligibleForAutoRepair()) {
            result.markAsSkipped("Instruction not eligible for auto-repair");
            return result;
        }

        // Apply SIs in order of weight (highest first)
        sis.sort((a, b) -> Double.compare(b.getWeight(), a.getWeight()));

        for (StandingInstruction si : sis) {
            if (si.isApplicableToInstruction(instruction)) {
                applyStandingInstructionForRemainingFields(result, si, instruction);
                result.addRuleScore(si.getSiId(), 1.0, si.getWeight());
            }
        }

        if (result.getFieldsRepaired() > 0) {
            result.markAsSuccessful("Weighted repair completed using " + result.getAppliedStandingInstructions().size() + " SIs");
        } else {
            result.markAsFailed("No applicable standing instructions found");
        }

        result.calculateFinalScores();
        return result;
    }

    /**
     * Perform hierarchical repair with client-level precedence.
     */
    private SIRepairResult performHierarchicalRepair(SettlementInstruction instruction, List<StandingInstruction> sis) {
        SIRepairResult result = new SIRepairResult(instruction.getInstructionId());
        result.setProcessedBy("CustodyAutoRepairStandaloneDemo");

        if (!instruction.isEligibleForAutoRepair()) {
            result.markAsSkipped("Instruction not eligible for auto-repair");
            return result;
        }

        // Find client-level SI first (highest priority)
        StandingInstruction clientSI = sis.stream()
            .filter(si -> "CLIENT".equals(si.getScopeType()) && si.isApplicableToInstruction(instruction))
            .findFirst()
            .orElse(null);

        if (clientSI != null) {
            applyStandingInstruction(result, clientSI, instruction);
            result.addRuleScore(clientSI.getSiId(), 1.0, clientSI.getWeight());
            result.markAsSuccessful("Hierarchical repair completed using client-level SI: " + clientSI.getSiName());
        } else {
            // Fall back to market-level or instrument-level SIs
            for (StandingInstruction si : sis) {
                if (si.isApplicableToInstruction(instruction)) {
                    applyStandingInstructionForRemainingFields(result, si, instruction);
                    result.addRuleScore(si.getSiId(), 1.0, si.getWeight());
                }
            }

            if (result.getFieldsRepaired() > 0) {
                result.markAsSuccessful("Hierarchical repair completed using fallback SIs");
            } else {
                result.markAsFailed("No applicable standing instructions found");
            }
        }

        result.calculateFinalScores();
        return result;
    }

    /**
     * Check eligibility and skip if not eligible.
     */
    private SIRepairResult checkEligibilityAndSkip(SettlementInstruction instruction, String reason) {
        SIRepairResult result = new SIRepairResult(instruction.getInstructionId());
        result.setProcessedBy("CustodyAutoRepairStandaloneDemo");
        result.markAsSkipped(reason);
        return result;
    }

    /**
     * Apply standing instruction to repair missing fields.
     */
    private void applyStandingInstruction(SIRepairResult result, StandingInstruction si, SettlementInstruction instruction) {
        result.addAppliedStandingInstruction(si);

        for (String missingField : instruction.getMissingFields()) {
            if (si.hasDefaultValue(missingField)) {
                String defaultValue = si.getDefaultValue(missingField);
                result.addFieldRepair(missingField, defaultValue, si);
                updateInstructionField(instruction, missingField, defaultValue);
            }
        }

        si.incrementUsage();
        si.updateSuccessRate(true);
    }

    /**
     * Apply standing instruction only for fields not already repaired.
     */
    private void applyStandingInstructionForRemainingFields(SIRepairResult result, StandingInstruction si, SettlementInstruction instruction) {
        boolean applied = false;

        for (String missingField : instruction.getMissingFields()) {
            if (!result.hasFieldRepair(missingField) && si.hasDefaultValue(missingField)) {
                String defaultValue = si.getDefaultValue(missingField);
                result.addFieldRepair(missingField, defaultValue, si);
                updateInstructionField(instruction, missingField, defaultValue);
                applied = true;
            }
        }

        if (applied) {
            result.addAppliedStandingInstruction(si);
            si.incrementUsage();
            si.updateSuccessRate(true);
        }
    }

    /**
     * Update instruction field with repaired value.
     */
    private void updateInstructionField(SettlementInstruction instruction, String fieldName, String value) {
        switch (fieldName.toLowerCase()) {
            case "counterpartyid":
                instruction.setCounterpartyId(value);
                break;
            case "custodianid":
                instruction.setCustodianId(value);
                break;
            case "settlementmethod":
                instruction.setSettlementMethod(value);
                break;
            case "deliveryinstruction":
                instruction.setDeliveryInstruction(value);
                break;
            // Add more field mappings as needed
        }
    }
}
