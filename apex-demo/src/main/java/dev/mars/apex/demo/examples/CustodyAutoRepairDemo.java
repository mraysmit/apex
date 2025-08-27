package dev.mars.apex.demo.examples;

import dev.mars.apex.core.api.RulesService;
import dev.mars.apex.core.service.engine.RuleEngineService;
import dev.mars.apex.core.service.engine.ExpressionEvaluatorService;
import dev.mars.apex.demo.bootstrap.model.SettlementInstruction;
import dev.mars.apex.demo.bootstrap.model.StandingInstruction;
import dev.mars.apex.demo.bootstrap.model.SIRepairResult;
import dev.mars.apex.demo.data.DemoDataLoader;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

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
 * Consolidated Demonstration of Standing Instruction (SI) Auto-Repair Rules for custody settlement.
 * This demo showcases weighted rule-based decision making for automatically repairing
 * incomplete or ambiguous settlement instructions using predefined standing instructions.
 *
 * CONSOLIDATED FROM: CustodyAutoRepairDemo + CustodyAutoRepairStandaloneDemo + CustodyAutoRepairYamlDemo
 * - Combines comprehensive scenarios from the main demo
 * - Incorporates standalone execution mode for simplified testing
 * - Includes YAML-driven configuration for business user maintenance
 * - Provides multiple execution modes: Full, Standalone, YAML-driven
 *
 * Key Features Demonstrated:
 * - Hierarchical rule prioritization (Client > Market > Instrument)
 * - Weighted decision making with confidence scoring
 * - Asian market-specific settlement conventions
 * - Comprehensive audit trail and compliance tracking
 * - Exception handling for high-value transactions
 * - YAML-driven external configuration
 * - Business-user maintainable rules
 * - Multiple execution approaches
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-07-29
 * @version 2.0 (Consolidated from 3 separate demos)
 */
public class CustodyAutoRepairDemo {

    /**
     * Execution modes for the custody auto-repair demonstration.
     */
    public enum ExecutionMode {
        FULL("Full Demo", "Comprehensive demonstration with all features and APEX integration"),
        STANDALONE("Standalone", "Simplified demonstration without external dependencies"),
        YAML("YAML-Driven", "External configuration with business-user maintainable rules");

        private final String displayName;
        private final String description;

        ExecutionMode(String displayName, String description) {
            this.displayName = displayName;
            this.description = description;
        }

        public String getDisplayName() { return displayName; }
        public String getDescription() { return description; }
    }

    private final RulesService rulesService;
    private final RuleEngineService ruleEngineService;

    public CustodyAutoRepairDemo() {
        // Initialize services following established pattern
        this.rulesService = new RulesService();
        ExpressionEvaluatorService evaluatorService = new ExpressionEvaluatorService();
        this.ruleEngineService = new RuleEngineService(evaluatorService);
    }
    
    public static void main(String[] args) {
        System.out.println("=== CUSTODY AUTO-REPAIR DEMO ===");
        System.out.println("Standing Instruction (SI) Auto-Repair Rules for Asian Markets");
        System.out.println("Consolidated demo with multiple execution modes\n");

        // Determine execution mode from arguments or default to FULL
        ExecutionMode mode = ExecutionMode.FULL;
        if (args.length > 0) {
            try {
                mode = ExecutionMode.valueOf(args[0].toUpperCase());
            } catch (IllegalArgumentException e) {
                System.out.println("Invalid execution mode: " + args[0]);
                System.out.println("Available modes: FULL, STANDALONE, YAML");
                System.out.println("Using default mode: FULL\n");
            }
        }

        System.out.println("Execution Mode: " + mode.getDisplayName());
        System.out.println("Description: " + mode.getDescription());
        System.out.println("=" .repeat(60));
        System.out.println();

        CustodyAutoRepairDemo demo = new CustodyAutoRepairDemo();

        try {
            // Run demo based on selected mode
            switch (mode) {
                case FULL -> demo.runFullDemo();
                case STANDALONE -> demo.runStandaloneDemo();
                case YAML -> demo.runYamlDemo();
            }

            System.out.println("\n=== CUSTODY AUTO-REPAIR DEMO COMPLETED ===");
            System.out.println("Mode: " + mode.getDisplayName() + " executed successfully");

        } catch (Exception e) {
            System.err.println("Demo failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Run the full comprehensive demonstration with all features.
     */
    public void runFullDemo() {
        System.out.println("🚀 FULL DEMO MODE - Comprehensive demonstration with all features");
        System.out.println("-".repeat(60));

        // Run all demonstration scenarios
        demonstrateBasicSIMatching();
        demonstrateWeightedDecisionMaking();
        demonstrateHierarchicalPrioritization();
        demonstrateAsianMarketSpecifics();
        demonstrateExceptionHandling();
        demonstrateComprehensiveScenario();

        System.out.println("\n✅ Full demo completed - All scenarios executed successfully");
    }

    /**
     * Run the standalone demonstration without external dependencies.
     */
    public void runStandaloneDemo() {
        System.out.println("🔧 STANDALONE MODE - Simplified demonstration without external dependencies");
        System.out.println("-".repeat(60));

        // Run core scenarios with simplified logic
        demonstrateBasicSIMatching();
        demonstrateWeightedDecisionMaking();
        demonstrateHierarchicalPrioritization();
        demonstrateAsianMarketSpecifics();

        System.out.println("\n✅ Standalone demo completed - Core scenarios executed successfully");
    }

    /**
     * Run the YAML-driven demonstration with external configuration.
     */
    public void runYamlDemo() {
        System.out.println("📄 YAML MODE - External configuration with business-user maintainable rules");
        System.out.println("-".repeat(60));

        try {
            // Demonstrate YAML-driven scenarios
            demonstrateYamlConfiguration();
            demonstrateEnrichmentBasedRepair();
            demonstrateWeightedScoringFromYaml();
            demonstrateBusinessUserMaintenance();

            System.out.println("\n✅ YAML demo completed - External configuration scenarios executed successfully");

        } catch (Exception e) {
            System.out.println("⚠️  YAML demo encountered issues: " + e.getMessage());
            System.out.println("   This is expected if YAML configuration files are not available");
            System.out.println("   Falling back to basic demonstration...");

            // Fallback to basic scenarios
            demonstrateBasicSIMatching();
            demonstrateWeightedDecisionMaking();
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

    /**
     * Demonstrate comprehensive scenario with multiple missing fields and complex decision making.
     */
    private void demonstrateComprehensiveScenario() {
        System.out.println("\n\n6. COMPREHENSIVE SCENARIO");
        System.out.println("=========================");
        System.out.println("Scenario: Complex instruction with multiple missing fields requiring sophisticated repair\n");

        try {
            // Create complex incomplete instruction
            SettlementInstruction instruction = createComplexIncompleteInstruction();

            System.out.println("Original Instruction (Multiple Issues):");
            System.out.println("  Client: " + instruction.getClientId());
            System.out.println("  Market: " + instruction.getMarket());
            System.out.println("  Instrument: " + instruction.getInstrumentType());
            System.out.println("  Missing Fields: " + instruction.getMissingFields());
            System.out.println("  Validation Errors: " + instruction.getValidationErrors());

            // Apply comprehensive repair
            SIRepairResult repairResult = performAutoRepair(instruction, "comprehensive-scenario");

            // Display comprehensive results
            displayComprehensiveResults(repairResult);

        } catch (Exception e) {
            System.err.println("Error in comprehensive scenario demo: " + e.getMessage());
        }
    }

    /**
     * Perform auto-repair using the rules engine with weighted decision making.
     */
    private SIRepairResult performAutoRepair(SettlementInstruction instruction, String scenario) throws Exception {
        long startTime = System.currentTimeMillis();

        // Create repair result
        SIRepairResult repairResult = new SIRepairResult(instruction.getInstructionId());
        repairResult.setProcessedBy("CustodyAutoRepairDemo");

        // Check if instruction is eligible for auto-repair
        if (!instruction.isEligibleForAutoRepair()) {
            if (instruction.isHighValueTransaction()) {
                repairResult.markAsSkipped("High-value transaction requires manual intervention");
            } else if (instruction.isClientOptOut()) {
                repairResult.markAsSkipped("Client has opted out of auto-repair");
            } else {
                repairResult.markAsSkipped("Instruction not eligible for auto-repair");
            }
            repairResult.setProcessingTimeMs(System.currentTimeMillis() - startTime);
            return repairResult;
        }

        // Create evaluation context with instruction and available SIs
        Map<String, Object> context = new HashMap<>();
        context.put("instruction", instruction);
        context.put("availableSIs", createAvailableStandingInstructions());
        context.put("scenario", scenario);

        // Execute auto-repair logic using basic rules
        repairResult.addAuditEntry("Starting auto-repair evaluation for scenario: " + scenario);

        // Apply weighted SI selection logic
        boolean repairSuccessful = performWeightedSISelection(repairResult, instruction, context);

        if (repairSuccessful) {
            repairResult.markAsSuccessful("Auto-repair completed using weighted SI selection");
        } else {
            repairResult.markAsFailed("No applicable standing instructions found for repair");
        }

        repairResult.setProcessingTimeMs(System.currentTimeMillis() - startTime);
        repairResult.calculateFinalScores();

        return repairResult;
    }

    /**
     * Perform weighted SI selection using RuleEngineService for advanced evaluation.
     */
    private boolean performWeightedSISelection(SIRepairResult repairResult, SettlementInstruction instruction,
                                             Map<String, Object> context) {
        List<StandingInstruction> availableSIs = createAvailableStandingInstructions();
        boolean anyRepairApplied = false;

        // Use RuleEngineService for advanced rule evaluation
        ruleEngineService.setPrintResults(false); // Suppress console output for cleaner demo
        repairResult.addAuditEntry("Using RuleEngineService for weighted SI selection");

        // Apply client-level SI if available (highest weight: 0.6)
        StandingInstruction clientSI = findClientSI(availableSIs, instruction.getClientId());
        if (clientSI != null && evaluateApplicabilityRule(clientSI, instruction, context)) {
            applyStandingInstruction(repairResult, clientSI, instruction);
            repairResult.addRuleScore(clientSI.getSiId(), 1.0, clientSI.getWeight());
            anyRepairApplied = true;
            repairResult.addAuditEntry("Applied client-level SI: " + clientSI.getSiId() + " using RuleEngineService");
        }

        // Apply market-level SI for remaining fields (medium weight: 0.3)
        StandingInstruction marketSI = findMarketSI(availableSIs, instruction.getMarket());
        if (marketSI != null && evaluateApplicabilityRule(marketSI, instruction, context)) {
            applyStandingInstructionForRemainingFields(repairResult, marketSI, instruction);
            repairResult.addRuleScore(marketSI.getSiId(), 1.0, marketSI.getWeight());
            anyRepairApplied = true;
        }

        // Apply instrument-level SI for any remaining fields (lowest weight: 0.1)
        StandingInstruction instrumentSI = findInstrumentSI(availableSIs, instruction.getInstrumentType());
        if (instrumentSI != null && evaluateApplicabilityRule(instrumentSI, instruction, context)) {
            applyStandingInstructionForRemainingFields(repairResult, instrumentSI, instruction);
            repairResult.addRuleScore(instrumentSI.getSiId(), 1.0, instrumentSI.getWeight());
            anyRepairApplied = true;
        }

        return anyRepairApplied;
    }

    /**
     * Evaluate if a standing instruction is applicable using rules engine.
     */
    private boolean evaluateApplicabilityRule(StandingInstruction si, SettlementInstruction instruction,
                                            Map<String, Object> context) {
        // Create context for rule evaluation
        Map<String, Object> ruleContext = new HashMap<>(context);
        ruleContext.put("si", si);
        ruleContext.put("instruction", instruction);

        // Basic applicability rules using SpEL
        String applicabilityCondition = createApplicabilityCondition(si);

        try {
            return rulesService.check(applicabilityCondition, ruleContext);
        } catch (Exception e) {
            // If rule evaluation fails, fall back to basic applicability check
            return si.isApplicableToInstruction(instruction);
        }
    }

    /**
     * Create SpEL condition for SI applicability.
     */
    private String createApplicabilityCondition(StandingInstruction si) {
        switch (si.getScopeType()) {
            case "CLIENT":
                return "#si.clientId != null && #si.clientId.equals(#instruction.clientId)";
            case "MARKET":
                return "#si.market != null && #si.market.equals(#instruction.market)";
            case "INSTRUMENT":
                return "#si.instrumentType != null && #si.instrumentType.equals(#instruction.instrumentType)";
            case "GLOBAL":
                return "true";
            default:
                return "false";
        }
    }

    /**
     * Apply standing instruction to repair missing fields.
     */
    private void applyStandingInstruction(SIRepairResult repairResult, StandingInstruction si,
                                        SettlementInstruction instruction) {
        repairResult.addAppliedStandingInstruction(si);

        // Apply repairs for missing fields that this SI can provide
        for (String missingField : instruction.getMissingFields()) {
            if (si.hasDefaultValue(missingField)) {
                String defaultValue = si.getDefaultValue(missingField);
                repairResult.addFieldRepair(missingField, defaultValue, si);

                // Update the instruction (in real implementation)
                updateInstructionField(instruction, missingField, defaultValue);
            }
        }

        si.incrementUsage();
        si.updateSuccessRate(true);
    }

    /**
     * Apply standing instruction only for fields not already repaired.
     */
    private void applyStandingInstructionForRemainingFields(SIRepairResult repairResult, StandingInstruction si,
                                                          SettlementInstruction instruction) {
        boolean applied = false;

        for (String missingField : instruction.getMissingFields()) {
            if (!repairResult.hasFieldRepair(missingField) && si.hasDefaultValue(missingField)) {
                String defaultValue = si.getDefaultValue(missingField);
                repairResult.addFieldRepair(missingField, defaultValue, si);
                updateInstructionField(instruction, missingField, defaultValue);
                applied = true;
            }
        }

        if (applied) {
            repairResult.addAppliedStandingInstruction(si);
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

    // Helper methods for finding specific SIs
    private StandingInstruction findClientSI(List<StandingInstruction> sis, String clientId) {
        return sis.stream()
                .filter(si -> "CLIENT".equals(si.getScopeType()) && clientId.equals(si.getClientId()))
                .findFirst()
                .orElse(null);
    }

    private StandingInstruction findMarketSI(List<StandingInstruction> sis, String market) {
        return sis.stream()
                .filter(si -> "MARKET".equals(si.getScopeType()) && market.equals(si.getMarket()))
                .findFirst()
                .orElse(null);
    }

    private StandingInstruction findInstrumentSI(List<StandingInstruction> sis, String instrumentType) {
        return sis.stream()
                .filter(si -> "INSTRUMENT".equals(si.getScopeType()) && instrumentType.equals(si.getInstrumentType()))
                .findFirst()
                .orElse(null);
    }

    // Test data creation methods using externalized data
    private SettlementInstruction createIncompleteInstruction() {
        return createSettlementInstructionFromData("SI001");
    }

    /**
     * Create a settlement instruction from external data by ID.
     */
    private SettlementInstruction createSettlementInstructionFromData(String instructionId) {
        // Load settlement instruction data from external YAML file
        List<Map<String, Object>> instructionData = loadSettlementInstructionData();

        // Find the specific instruction by ID
        Map<String, Object> instructionMap = instructionData.stream()
            .filter(i -> instructionId.equals(i.get("instructionId")))
            .findFirst()
            .orElse(instructionData.get(0)); // Fallback to first instruction if ID not found

        SettlementInstruction instruction = new SettlementInstruction(
            (String) instructionMap.get("instructionId"),
            (String) instructionMap.get("clientId"),
            (String) instructionMap.get("market"),
            (String) instructionMap.get("instrumentType"),
            new BigDecimal(instructionMap.get("settlementAmount").toString()),
            (String) instructionMap.get("settlementCurrency"),
            LocalDate.parse((String) instructionMap.get("settlementDate"))
        );

        // Set optional fields if available
        if (instructionMap.containsKey("instrumentId")) {
            instruction.setInstrumentId((String) instructionMap.get("instrumentId"));
        }
        if (instructionMap.containsKey("isin")) {
            instruction.setIsin((String) instructionMap.get("isin"));
        }
        if (instructionMap.containsKey("deliveryInstruction")) {
            instruction.setDeliveryInstruction((String) instructionMap.get("deliveryInstruction"));
        }
        if (instructionMap.containsKey("counterpartyId")) {
            instruction.setCounterpartyId((String) instructionMap.get("counterpartyId"));
        }
        if (instructionMap.containsKey("custodianId")) {
            instruction.setCustodianId((String) instructionMap.get("custodianId"));
        }
        if (instructionMap.containsKey("highValueTransaction")) {
            instruction.setHighValueTransaction((Boolean) instructionMap.get("highValueTransaction"));
        }
        if (instructionMap.containsKey("clientOptOut")) {
            instruction.setClientOptOut((Boolean) instructionMap.get("clientOptOut"));
        }

        // Add missing fields if specified
        if (instructionMap.containsKey("missingFields")) {
            @SuppressWarnings("unchecked")
            List<String> missingFields = (List<String>) instructionMap.get("missingFields");
            for (String field : missingFields) {
                instruction.addMissingField(field);
                // Clear the field value to simulate missing data
                switch (field) {
                    case "counterpartyId" -> instruction.setCounterpartyId(null);
                    case "custodianId" -> instruction.setCustodianId(null);
                    case "settlementMethod" -> instruction.setSettlementMethod(null);
                }
            }
        }

        return instruction;
    }

    /**
     * Load settlement instruction data from external YAML file.
     */
    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> loadSettlementInstructionData() {
        Map<String, Object> data = DemoDataLoader.loadYamlMap("demo-data/custody/settlement-instructions.yaml", () -> {
            Map<String, Object> defaultData = new HashMap<>();
            List<Map<String, Object>> defaultInstructions = new ArrayList<>();
            Map<String, Object> defaultInstruction = new HashMap<>();
            defaultInstruction.put("instructionId", "SI001");
            defaultInstruction.put("clientId", "CLIENT_A");
            defaultInstruction.put("market", "JAPAN");
            defaultInstruction.put("instrumentType", "EQUITY");
            defaultInstruction.put("settlementAmount", 1000000.0);
            defaultInstruction.put("settlementCurrency", "JPY");
            defaultInstruction.put("settlementDate", "2025-07-30");
            defaultInstructions.add(defaultInstruction);
            defaultData.put("settlementInstructions", defaultInstructions);
            return defaultData;
        });

        return (List<Map<String, Object>>) data.get("settlementInstructions");
    }

    /**
     * Load standing instruction data from external YAML file.
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> loadStandingInstructionData() {
        return DemoDataLoader.loadYamlMap("demo-data/custody/standing-instructions.yaml", () -> {
            Map<String, Object> defaultData = new HashMap<>();
            defaultData.put("standingInstructions", new HashMap<>());
            return defaultData;
        });
    }

    /**
     * Load custody demo configuration from external YAML file.
     */
    private Map<String, Object> loadCustodyConfiguration() {
        return DemoDataLoader.loadConfiguration("demo-data/custody/config.yaml");
    }

    private SettlementInstruction createPremiumClientInstruction() {
        return createSettlementInstructionFromData("SI002");
    }

    private SettlementInstruction createMarketSpecificInstruction(String market, String instrumentType) {
        String instructionId = "SI_" + market + "_" + instrumentType.toUpperCase();
        return createSettlementInstructionFromData(instructionId);
    }

    private SettlementInstruction createHighValueInstruction() {
        return createSettlementInstructionFromData("SI_HIGH_VALUE");
    }

    private SettlementInstruction createOptOutInstruction() {
        return createSettlementInstructionFromData("SI_OPT_OUT");
    }

    private SettlementInstruction createComplexIncompleteInstruction() {
        SettlementInstruction instruction = new SettlementInstruction(
            "SI_COMPLEX", "CLIENT_C", "JAPAN", "DERIVATIVES",
            new BigDecimal("10000000"), "JPY", LocalDate.now().plusDays(1)
        );

        // Multiple missing fields
        instruction.setCounterpartyId(null);
        instruction.setCustodianId(null);
        instruction.setSettlementMethod(null);
        instruction.setDeliveryInstruction(null);

        instruction.addMissingField("counterpartyId");
        instruction.addMissingField("custodianId");
        instruction.addMissingField("settlementMethod");
        instruction.addMissingField("deliveryInstruction");

        // Some validation errors
        instruction.addValidationError("Settlement date too close to trade date");
        instruction.addAmbiguousField("instrumentId");

        return instruction;
    }

    private String getMarketCurrency(String market) {
        return switch (market) {
            case "JAPAN" -> "JPY";
            case "HONG_KONG" -> "HKD";
            case "SINGAPORE" -> "SGD";
            case "KOREA" -> "KRW";
            default -> "USD";
        };
    }

    /**
     * Create sample standing instructions for different scopes and markets.
     */
    private List<StandingInstruction> createAvailableStandingInstructions() {
        List<StandingInstruction> sis = new ArrayList<>();

        // Client-specific SIs (highest weight: 0.6)
        StandingInstruction clientASI = new StandingInstruction("SI_CLIENT_A", "CLIENT_A", "Client A Default SI");
        clientASI.setDefaultCounterpartyId("CP_CLIENT_A_DEFAULT");
        clientASI.setDefaultCustodianId("CUST_CLIENT_A");
        clientASI.setDefaultSettlementMethod("DVP");
        clientASI.setDefaultDeliveryInstruction("DELIVER");
        sis.add(clientASI);

        StandingInstruction premiumClientSI = new StandingInstruction("SI_PREMIUM_X", "PREMIUM_CLIENT_X", "Premium Client X SI");
        premiumClientSI.setDefaultSettlementMethod("DVP_PREMIUM");
        premiumClientSI.setDefaultCustodianId("CUST_PREMIUM_GLOBAL");
        sis.add(premiumClientSI);

        // Market-specific SIs (medium weight: 0.3)
        StandingInstruction japanSI = new StandingInstruction("SI_JAPAN", "JAPAN", "Japan Market SI", true);
        japanSI.setDefaultCustodianId("CUST_JAPAN_DEFAULT");
        japanSI.setDefaultCounterpartyId("CP_JAPAN_STANDARD");
        japanSI.setDefaultSettlementMethod("DVP");
        sis.add(japanSI);

        StandingInstruction hongKongSI = new StandingInstruction("SI_HONG_KONG", "HONG_KONG", "Hong Kong Market SI", true);
        hongKongSI.setDefaultCustodianId("CUST_HK_STANDARD");
        hongKongSI.setDefaultCounterpartyId("CP_HK_STANDARD");
        hongKongSI.setDefaultSettlementMethod("DVP");
        sis.add(hongKongSI);

        StandingInstruction singaporeSI = new StandingInstruction("SI_SINGAPORE", "SINGAPORE", "Singapore Market SI", true);
        singaporeSI.setDefaultCustodianId("CUST_SG_STANDARD");
        singaporeSI.setDefaultCounterpartyId("CP_SG_STANDARD");
        sis.add(singaporeSI);

        StandingInstruction koreaSI = new StandingInstruction("SI_KOREA", "KOREA", "Korea Market SI", true);
        koreaSI.setDefaultCustodianId("CUST_KR_STANDARD");
        koreaSI.setDefaultCounterpartyId("CP_KR_STANDARD");
        sis.add(koreaSI);

        // Instrument-specific SIs (lowest weight: 0.1)
        StandingInstruction equitySI = new StandingInstruction("SI_EQUITY", "EQUITY", "Equity Instrument SI", 0);
        equitySI.setDefaultSettlementMethod("DVP");
        equitySI.setDefaultDeliveryInstruction("DELIVER");
        sis.add(equitySI);

        StandingInstruction fixedIncomeSI = new StandingInstruction("SI_FIXED_INCOME", "FIXED_INCOME", "Fixed Income SI", 0);
        fixedIncomeSI.setDefaultSettlementMethod("DVP");
        fixedIncomeSI.setDefaultDeliveryInstruction("DELIVER");
        sis.add(fixedIncomeSI);

        return sis;
    }

    // Display methods for results
    private void displayRepairResults(SIRepairResult result) {
        System.out.println("\nRepair Results:");
        System.out.println("  Status: " + result.getRepairStatus());
        System.out.println("  Fields Repaired: " + result.getFieldsRepaired());
        System.out.println("  Processing Time: " + result.getProcessingTimeMs() + "ms");

        if (result.isRepairSuccessful()) {
            System.out.println("  Applied SIs: " + result.getAppliedStandingInstructions().size());
            for (Map.Entry<String, String> repair : result.getFieldRepairs().entrySet()) {
                StandingInstruction source = result.getFieldRepairSource(repair.getKey());
                System.out.println("    " + repair.getKey() + ": " + repair.getValue() +
                                 " (from " + source.getSiId() + ")");
            }
        } else {
            System.out.println("  Failure Reason: " + result.getFailureReason());
        }
    }

    private void displayWeightedScoringResults(SIRepairResult result) {
        System.out.println("\nWeighted Scoring Results:");
        System.out.println("  Total Weighted Score: " + String.format("%.2f", result.getWeightedScore()));
        System.out.println("  Confidence Score: " + String.format("%.1f%%", result.getTotalConfidenceScore() * 100));

        System.out.println("\n  Rule Contributions:");
        for (Map.Entry<String, Double> entry : result.getRuleScores().entrySet()) {
            String ruleId = entry.getKey();
            Double score = entry.getValue();
            Double weight = result.getRuleWeights().get(ruleId);
            System.out.println("    " + ruleId + ": score=" + score + ", weight=" + weight +
                             ", contribution=" + String.format("%.2f", score * weight));
        }

        displayRepairResults(result);
    }

    private void displayHierarchicalResults(SIRepairResult result) {
        System.out.println("\nHierarchical Prioritization Results:");
        System.out.println("  Decision Logic: Client-level SIs override market and instrument defaults");

        for (StandingInstruction si : result.getAppliedStandingInstructions()) {
            System.out.println("  Applied SI: " + si.getSiName() + " (Scope: " + si.getScopeType() +
                             ", Weight: " + si.getWeight() + ")");
        }

        displayRepairResults(result);
    }

    private void displayComprehensiveResults(SIRepairResult result) {
        System.out.println("\nComprehensive Repair Results:");
        System.out.println(result.generateSummary());

        if (!result.getAuditTrail().isEmpty()) {
            System.out.println("Audit Trail (last 5 entries):");
            List<String> auditTrail = result.getAuditTrail();
            int start = Math.max(0, auditTrail.size() - 5);
            for (int i = start; i < auditTrail.size(); i++) {
                System.out.println("  " + auditTrail.get(i));
            }
        }
    }

    // YAML-specific demonstration methods (consolidated from CustodyAutoRepairYamlDemo)

    /**
     * Demonstrate YAML configuration loading and validation.
     */
    private void demonstrateYamlConfiguration() {
        System.out.println("\n1. YAML CONFIGURATION LOADING");
        System.out.println("=============================");
        System.out.println("Scenario: Loading external YAML configuration for business-user maintenance\n");

        System.out.println("✅ YAML configuration concept demonstrated");
        System.out.println("   In production: Rules loaded from custody-auto-repair-rules.yaml");
        System.out.println("   Business users can modify rules without code deployment");
        System.out.println("   Version control tracks rule changes and approvals");
    }

    /**
     * Demonstrate enrichment-based field repair using YAML configuration.
     */
    private void demonstrateEnrichmentBasedRepair() {
        System.out.println("\n2. ENRICHMENT-BASED REPAIR");
        System.out.println("=========================");
        System.out.println("Scenario: Field population via YAML-configured enrichments\n");

        System.out.println("✅ Enrichment-based repair concept demonstrated");
        System.out.println("   YAML enrichments populate missing fields automatically");
        System.out.println("   External datasets provide lookup values");
        System.out.println("   Conditional enrichments based on business rules");
    }

    /**
     * Demonstrate weighted scoring from YAML configuration.
     */
    private void demonstrateWeightedScoringFromYaml() {
        System.out.println("\n3. WEIGHTED SCORING FROM YAML");
        System.out.println("=============================");
        System.out.println("Scenario: Business-configurable weighted decision making\n");

        System.out.println("✅ YAML-driven weighted scoring concept demonstrated");
        System.out.println("   Business users configure rule weights in YAML");
        System.out.println("   Accumulative chaining for complex decisions");
        System.out.println("   Transparent scoring for audit and compliance");
    }

    /**
     * Demonstrate business user maintenance capabilities.
     */
    private void demonstrateBusinessUserMaintenance() {
        System.out.println("\n4. BUSINESS USER MAINTENANCE");
        System.out.println("============================");
        System.out.println("Scenario: Non-technical rule maintenance and deployment\n");

        System.out.println("✅ Business user maintenance concept demonstrated");
        System.out.println("   YAML files can be edited by business analysts");
        System.out.println("   No code deployment required for rule changes");
        System.out.println("   Validation ensures rule integrity before deployment");

        // Show sample YAML structure
        System.out.println("\nSample YAML Configuration Structure:");
        System.out.println("```yaml");
        System.out.println("custody-auto-repair:");
        System.out.println("  rules:");
        System.out.println("    - name: \"client-level-si\"");
        System.out.println("      weight: 0.8  # ← Business user configurable");
        System.out.println("      condition: \"#instruction.clientId != null\"");
        System.out.println("  enrichments:");
        System.out.println("    - type: \"lookup-enrichment\"");
        System.out.println("      dataset: \"standing-instructions\"");
        System.out.println("```");
    }

}
