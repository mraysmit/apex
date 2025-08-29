package dev.mars.apex.demo.runners;

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

import dev.mars.apex.demo.enrichment.BatchProcessingDemo;
import dev.mars.apex.demo.enrichment.ComprehensiveFinancialSettlementDemo;
import dev.mars.apex.demo.enrichment.CustodyAutoRepairBootstrap;
import dev.mars.apex.demo.enrichment.CustodyAutoRepairDemo;
import dev.mars.apex.demo.enrichment.CustomerTransformerDemo;
import dev.mars.apex.demo.enrichment.DataManagementDemo;
import dev.mars.apex.demo.enrichment.ExternalDataSourceDemo;
import dev.mars.apex.demo.enrichment.OtcOptionsBootstrapDemo;
import dev.mars.apex.demo.enrichment.TradeTransformerDemo;
import dev.mars.apex.demo.enrichment.YamlDatasetDemo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Scanner;

/**
 * EnrichmentRunner - Comprehensive APEX Enrichment Demonstrations
 * 
 * This runner provides complete coverage of all enrichment-related demos in the APEX Rules Engine,
 * demonstrating data transformation, lookup operations, and data enhancement capabilities.
 * 
 * ENRICHMENT CATEGORIES COVERED:
 * • Data Management: Core data handling and transformation patterns
 * • YAML Datasets: Inline and external dataset enrichment techniques
 * • External Data Sources: Database, API, and file-based enrichment
 * • Financial Processing: Settlement, custody, and trading enrichment
 * • Batch Processing: High-volume data transformation workflows
 * • Bootstrap Scenarios: Complete end-to-end enrichment workflows
 * 
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-08-29
 * @version 1.0
 */
public class EnrichmentRunner {
    
    private static final Logger logger = LoggerFactory.getLogger(EnrichmentRunner.class);
    
    /**
     * Main entry point for Enrichment demos.
     * 
     * @param args command line arguments: empty for interactive mode, demo name for direct execution
     */
    public static void main(String[] args) {
        EnrichmentRunner runner = new EnrichmentRunner();
        
        if (args.length == 0) {
            runner.runInteractiveMode();
        } else {
            runner.runDirectMode(args[0]);
        }
    }
    
    /**
     * Run in interactive mode with menu selection.
     */
    private void runInteractiveMode() {
        displayBanner();
        
        Scanner scanner = new Scanner(System.in);
        
        while (true) {
            displayMenu();
            System.out.print("Enter your choice (1-11, or 'q' to quit): ");
            String choice = scanner.nextLine().trim().toLowerCase();
            
            if (choice.equals("q") || choice.equals("quit") || choice.equals("exit")) {
                System.out.println("\n✅ Thank you for exploring APEX Enrichment capabilities!");
                break;
            }
            
            executeChoice(choice);
        }
        
        scanner.close();
    }
    
    /**
     * Run specific demo directly.
     */
    private void runDirectMode(String demoName) {
        displayBanner();
        System.out.println("Running enrichment demo: " + demoName);
        System.out.println();
        
        executeChoice(demoName.toLowerCase());
    }
    
    /**
     * Display the runner banner.
     */
    private void displayBanner() {
        logger.info("╔" + "═".repeat(78) + "╗");
        logger.info("║" + " ".repeat(21) + "APEX RULES ENGINE - ENRICHMENT DEMOS" + " ".repeat(20) + "║");
        logger.info("╚" + "═".repeat(78) + "╝");
        logger.info("");
        logger.info("Welcome to comprehensive APEX enrichment demonstrations! 🔧");
        logger.info("");
        logger.info("These demos showcase data transformation, lookup operations,");
        logger.info("and data enhancement capabilities of the APEX Rules Engine.");
        logger.info("");
    }
    
    /**
     * Display the interactive menu.
     */
    private void displayMenu() {
        System.out.println("Available Enrichment Demonstrations:");
        System.out.println("  1. Data Management Demo - Core data handling patterns");
        System.out.println("  2. YAML Dataset Demo - Inline dataset enrichment");
        System.out.println("  3. External Data Source Demo - Database/API enrichment");
        System.out.println("  4. Batch Processing Demo - High-volume transformations");
        System.out.println("  5. Financial Settlement Demo - Trading settlement workflows");
        System.out.println("  6. Custody Auto-Repair Demo - Exception handling workflows");
        System.out.println("  7. Custody Auto-Repair Bootstrap - Complete custody workflow");
        System.out.println("  8. OTC Options Bootstrap - Options processing pipeline");
        System.out.println("  9. Customer Transformer Demo - Customer data enrichment");
        System.out.println(" 10. Trade Transformer Demo - Trade data transformation");
        System.out.println(" 11. Run All Enrichment Demos - Execute complete suite");
        System.out.println();
    }
    
    /**
     * Execute the selected choice.
     */
    private void executeChoice(String choice) {
        try {
            switch (choice) {
                case "1":
                case "data":
                case "management":
                    runDemo("Data Management Demo", DataManagementDemo::main);
                    break;
                case "2":
                case "yaml":
                case "dataset":
                    runDemo("YAML Dataset Demo", YamlDatasetDemo::main);
                    break;
                case "3":
                case "external":
                case "datasource":
                    runDemo("External Data Source Demo", ExternalDataSourceDemo::main);
                    break;
                case "4":
                case "batch":
                case "processing":
                    runDemo("Batch Processing Demo", BatchProcessingDemo::main);
                    break;
                case "5":
                case "financial":
                case "settlement":
                    runDemo("Financial Settlement Demo", ComprehensiveFinancialSettlementDemo::main);
                    break;
                case "6":
                case "custody":
                case "repair":
                    runDemo("Custody Auto-Repair Demo", CustodyAutoRepairDemo::main);
                    break;
                case "7":
                case "custodybootstrap":
                case "bootstrap":
                    runDemo("Custody Auto-Repair Bootstrap", CustodyAutoRepairBootstrap::main);
                    break;
                case "8":
                case "otc":
                case "options":
                    runDemo("OTC Options Bootstrap", OtcOptionsBootstrapDemo::main);
                    break;
                case "9":
                case "customer":
                case "transformer":
                    runDemo("Customer Transformer Demo", CustomerTransformerDemo::main);
                    break;
                case "10":
                case "trade":
                case "tradetransformer":
                    runDemo("Trade Transformer Demo", TradeTransformerDemo::main);
                    break;
                case "11":
                case "all":
                    runAllEnrichmentDemos();
                    break;
                default:
                    System.out.println("❌ Invalid choice: " + choice);
                    System.out.println("Please try again or type 'q' to quit.");
                    break;
            }
        } catch (Exception e) {
            logger.error("❌ Error executing enrichment demo: {}", e.getMessage(), e);
        }
        
        System.out.println();
    }
    
    /**
     * Run a single demo with error handling.
     */
    private void runDemo(String demoName, DemoExecutor executor) {
        logger.info("▶ Running: {}", demoName);
        logger.info("─".repeat(50));
        
        try {
            long startTime = System.currentTimeMillis();
            executor.execute(new String[]{});
            long endTime = System.currentTimeMillis();
            
            logger.info("✅ {} completed successfully", demoName);
            logger.info("Execution time: {} ms", (endTime - startTime));
            
        } catch (Exception e) {
            logger.warn("⚠ {} encountered issues: {}", demoName, e.getMessage());
            logger.info("📝 This may be expected if dependencies are not available");
            logger.info("🎯 The enrichment concepts and patterns are still demonstrated!");
        }
    }
    
    /**
     * Run all enrichment demos in sequence.
     */
    private void runAllEnrichmentDemos() {
        logger.info("🚀 Running All Enrichment Demonstrations");
        logger.info("═".repeat(50));
        logger.info("");
        
        long totalStartTime = System.currentTimeMillis();
        
        runDemo("Data Management Demo", DataManagementDemo::main);
        runDemo("YAML Dataset Demo", YamlDatasetDemo::main);
        runDemo("External Data Source Demo", ExternalDataSourceDemo::main);
        runDemo("Batch Processing Demo", BatchProcessingDemo::main);
        runDemo("Financial Settlement Demo", ComprehensiveFinancialSettlementDemo::main);
        runDemo("Custody Auto-Repair Demo", CustodyAutoRepairDemo::main);
        runDemo("Custody Auto-Repair Bootstrap", CustodyAutoRepairBootstrap::main);
        runDemo("OTC Options Bootstrap", OtcOptionsBootstrapDemo::main);
        runDemo("Customer Transformer Demo", CustomerTransformerDemo::main);
        runDemo("Trade Transformer Demo", TradeTransformerDemo::main);
        
        long totalEndTime = System.currentTimeMillis();
        long totalTime = totalEndTime - totalStartTime;
        
        logger.info("");
        logger.info("🎉 All enrichment demonstrations completed!");
        logger.info("Total execution time: {} ms ({} seconds)", totalTime, totalTime / 1000.0);
        logger.info("");
        logger.info("You've experienced the full range of APEX enrichment capabilities!");
    }
    
    /**
     * Functional interface for demo execution.
     */
    @FunctionalInterface
    private interface DemoExecutor {
        void execute(String[] args) throws Exception;
    }
}
