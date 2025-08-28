package dev.mars.apex.demo.bootstrap;

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
import dev.mars.apex.core.service.scenario.ScenarioConfiguration;
import dev.mars.apex.demo.bootstrap.model.OtcOption;
import dev.mars.apex.demo.bootstrap.model.UnderlyingAsset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Demonstration of scenario-based data processing using the DataTypeScenarioService.
 * 
 * This demo shows how different data types (OTC Options, Commodity Swaps, Settlement
 * Instructions) can be automatically routed to their appropriate processing pipelines
 * based on scenario configurations.
 * 
 * KEY FEATURES DEMONSTRATED:
 * - Automatic data type detection and routing
 * - Scenario-specific processing pipeline configuration
 * - Rules and enrichments association with data types
 * - Multiple scenarios per data type for different contexts
 * - Fallback routing for unknown data types
 * 
 * PROCESSING FLOW:
 * 1. Load scenario configurations from YAML
 * 2. Create sample data records of different types
 * 3. Route each data record to its appropriate scenario
 * 4. Execute scenario-specific processing pipeline
 * 5. Display results showing different processing for each type
 * 
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 1.0.0
 */
public class ScenarioBasedProcessingDemo {
    
    private static final Logger logger = LoggerFactory.getLogger(ScenarioBasedProcessingDemo.class);
    
    private DataTypeScenarioService scenarioService;
    
    public static void main(String[] args) {
        System.out.println("=================================================================");
        System.out.println("SCENARIO-BASED DATA PROCESSING DEMONSTRATION");
        System.out.println("=================================================================");
        System.out.println("Demo Purpose: Show automatic routing of data types to processing scenarios");
        System.out.println("Architecture: One scenario per YAML file with registry-based loading");
        System.out.println("Data Types: OTC Options, Commodity Swaps, Settlement Instructions");
        System.out.println("Scenarios: Type-specific validation, enrichment, and business rules");
        System.out.println("Files: Registry + 3 individual scenario configuration files");
        System.out.println("Expected Duration: ~3-5 seconds");
        System.out.println("=================================================================");

        ScenarioBasedProcessingDemo demo = new ScenarioBasedProcessingDemo();
        long totalStartTime = System.currentTimeMillis();

        try {
            System.out.println("Initializing Scenario-Based Processing Demo...");
            demo.initialize();

            System.out.println("Executing scenario-based processing demonstration...");
            demo.runDemo();

            long totalEndTime = System.currentTimeMillis();
            long totalDuration = totalEndTime - totalStartTime;

            System.out.println("=================================================================");
            System.out.println("SCENARIO-BASED PROCESSING DEMO COMPLETED SUCCESSFULLY!");
            System.out.println("=================================================================");
            System.out.println("Total Execution Time: " + totalDuration + " ms");
            System.out.println("Scenarios Loaded: " + demo.scenarioService.getAvailableScenarios().size());
            System.out.println("Data Types Supported: " + demo.scenarioService.getSupportedDataTypes().size());
            System.out.println("Configuration Files: 1 registry + " + demo.scenarioService.getAvailableScenarios().size() + " scenario files");
            System.out.println("Architecture: One scenario per YAML file");
            System.out.println("Demo Status: SUCCESS");
            System.out.println("=================================================================");

        } catch (Exception e) {
            long totalEndTime = System.currentTimeMillis();
            long totalDuration = totalEndTime - totalStartTime;

            System.err.println("=================================================================");
            System.err.println("SCENARIO-BASED PROCESSING DEMO FAILED!");
            System.err.println("=================================================================");
            System.err.println("Error Message: " + e.getMessage());
            System.err.println("Execution Time: " + totalDuration + " ms");
            System.err.println("Demo Status: FAILED");
            System.err.println("=================================================================");
            e.printStackTrace();
        }
    }
    
    /**
     * Initialize the demo by loading scenario configurations.
     */
    private void initialize() throws Exception {
        logger.info("Initializing scenario-based processing demo...");

        // Initialize the scenario service
        this.scenarioService = new DataTypeScenarioService();

        // Load scenario configurations from registry
        logger.info("Loading scenario registry and individual scenario configurations...");
        scenarioService.loadScenarios("config/data-type-scenarios.yaml");

        logger.info("Scenario service initialized successfully");
        logger.info("Available scenarios: {}", scenarioService.getAvailableScenarios());
        logger.info("Supported data types: {}", scenarioService.getSupportedDataTypes());

        // Log details about loaded scenarios
        for (String scenarioId : scenarioService.getAvailableScenarios()) {
            ScenarioConfiguration scenario = scenarioService.getScenario(scenarioId);
            if (scenario != null) {
                logger.info("Loaded scenario '{}': {} (Domain: {}, Owner: {})",
                    scenarioId, scenario.getName(), scenario.getBusinessDomain(), scenario.getOwner());
            }
        }
    }
    
    /**
     * Run the main demonstration.
     */
    private void runDemo() {
        logger.info("Starting scenario-based processing demonstration...");
        
        // Create sample data records of different types
        List<Object> sampleData = createSampleDataRecords();
        
        logger.info("Processing {} sample data records through scenario routing...", sampleData.size());
        
        // Process each data record through scenario-based routing
        for (int i = 0; i < sampleData.size(); i++) {
            Object dataRecord = sampleData.get(i);
            
            logger.info("\n--- Processing Data Record {} ---", i + 1);
            logger.info("Data Type: {}", dataRecord.getClass().getSimpleName());
            
            // Route to appropriate scenario
            ScenarioConfiguration scenario = scenarioService.getScenarioForData(dataRecord);
            
            if (scenario != null) {
                logger.info("Routed to Scenario: {} ({})", scenario.getScenarioId(), scenario.getName());
                logger.info("Business Domain: {}", scenario.getBusinessDomain());
                logger.info("Risk Category: {}", scenario.getRiskCategory());
                
                // Execute scenario-specific processing
                executeScenarioProcessing(dataRecord, scenario);
                
            } else {
                logger.warn("No scenario found for data type: {}", dataRecord.getClass().getSimpleName());
                logger.info("Would use default/fallback processing");
            }
        }
        
        logger.info("\nScenario-based processing demonstration completed");
    }
    
    /**
     * Create sample data records of different types for demonstration.
     */
    private List<Object> createSampleDataRecords() {
        List<Object> sampleData = new ArrayList<>();
        
        // 1. OTC Option (should route to otc-options-standard scenario)
        OtcOption otcOption = new OtcOption(
            LocalDate.of(2025, 8, 2),
            "GOLDMAN_SACHS",
            "JP_MORGAN", 
            "Call",
            new UnderlyingAsset("Natural Gas", "MMBtu"),
            new BigDecimal("3.50"),
            "USD",
            new BigDecimal("10000"),
            LocalDate.of(2025, 12, 28),
            "Cash"
        );
        sampleData.add(otcOption);
        
        // 2. Commodity Swap (would route to commodity-swaps-standard scenario)
        // For demo purposes, we'll create a simple map to represent a commodity swap
        // In a real implementation, this would be a proper CommodityTotalReturnSwap object
        java.util.Map<String, Object> commoditySwap = new java.util.HashMap<>();
        commoditySwap.put("dataType", "CommoditySwap");
        commoditySwap.put("tradeId", "CS_20250802_001");
        commoditySwap.put("counterpartyId", "CP001");
        commoditySwap.put("clientId", "CLI001");
        commoditySwap.put("commodityType", "ENERGY");
        commoditySwap.put("referenceIndex", "WTI");
        commoditySwap.put("notionalAmount", new BigDecimal("5000000"));
        commoditySwap.put("notionalCurrency", "USD");
        commoditySwap.put("tradeDate", LocalDate.of(2025, 8, 2));
        commoditySwap.put("maturityDate", LocalDate.of(2027, 8, 2));
        sampleData.add(commoditySwap);
        
        // 3. Settlement Instruction (would route to settlement-auto-repair scenario)
        java.util.Map<String, Object> settlementInstruction = new java.util.HashMap<>();
        settlementInstruction.put("dataType", "SettlementInstruction");
        settlementInstruction.put("instructionId", "SI_20250802_001");
        settlementInstruction.put("clientId", "CLIENT_PREMIUM_ASIA_001");
        settlementInstruction.put("market", "JAPAN");
        settlementInstruction.put("instrumentType", "EQUITY");
        settlementInstruction.put("settlementAmount", new BigDecimal("2000000"));
        settlementInstruction.put("settlementCurrency", "JPY");
        settlementInstruction.put("settlementDate", LocalDate.of(2025, 8, 4));
        settlementInstruction.put("requiresRepair", true);
        sampleData.add(settlementInstruction);
        
        return sampleData;
    }
    
    /**
     * Execute scenario-specific processing for a data record.
     */
    private void executeScenarioProcessing(Object dataRecord, ScenarioConfiguration scenario) {
        logger.info("Executing scenario-specific processing...");

        // Get rule configurations (references to existing rule files)
        var ruleConfigurations = scenario.getRuleConfigurations();
        if (ruleConfigurations != null && !ruleConfigurations.isEmpty()) {
            logger.info("Rule Configuration Files:");
            for (String ruleConfig : ruleConfigurations) {
                logger.info("  - Rule File: {}", ruleConfig);
            }
        } else {
            logger.info("No rule configurations defined for this scenario");
        }

        // Simulate processing execution
        long processingStart = System.currentTimeMillis();

        // In a real implementation, this would:
        // 1. Load the referenced rule configuration files
        // 2. Execute validation rules from those files
        // 3. Apply enrichments defined in the rule files
        // 4. Run business rules and calculations
        // 5. Generate audit records

        simulateProcessingSteps(dataRecord, scenario);

        long processingEnd = System.currentTimeMillis();
        long processingDuration = processingEnd - processingStart;

        logger.info("Processing completed in {} ms", processingDuration);

        // Check against SLA if defined
        Integer slaMs = scenario.getProcessingSlaMs();
        if (slaMs != null) {
            if (processingDuration <= slaMs) {
                logger.info("Processing within SLA target of {} ms", slaMs);
            } else {
                logger.warn("Processing exceeded SLA target of {} ms", slaMs);
            }
        }
    }
    
    /**
     * Simulate the execution of processing steps for demonstration.
     */
    private void simulateProcessingSteps(Object dataRecord, ScenarioConfiguration scenario) {
        logger.info("Simulating processing steps...");

        var ruleConfigurations = scenario.getRuleConfigurations();
        if (ruleConfigurations != null && !ruleConfigurations.isEmpty()) {
            for (int i = 0; i < ruleConfigurations.size(); i++) {
                String ruleFile = ruleConfigurations.get(i);
                logger.info("  [{}/{}] Loading and executing rules from: {}", i + 1, ruleConfigurations.size(), ruleFile);
                try { Thread.sleep(15); } catch (InterruptedException e) { /* ignore */ }
                logger.info("        Rules from {}: EXECUTED", ruleFile);
            }
        } else {
            logger.info("  No rule configurations to execute");
        }

        logger.info("Processing simulation completed successfully");
    }
}
