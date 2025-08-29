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

import dev.mars.apex.demo.infrastructure.DataServiceManagerDemo;
import dev.mars.apex.demo.infrastructure.DatabaseSetup;
import dev.mars.apex.demo.infrastructure.DemoDataBootstrap;
import dev.mars.apex.demo.infrastructure.ExternalDatasetSetup;
import dev.mars.apex.demo.infrastructure.FileProcessingDemo;
import dev.mars.apex.demo.infrastructure.ProductionDemoDataServiceManager;
import dev.mars.apex.demo.infrastructure.RuleConfigDatabaseSetup;
import dev.mars.apex.demo.infrastructure.XmlDataGenerator;
import dev.mars.apex.demo.infrastructure.DataProviderComplianceTest;
import dev.mars.apex.demo.infrastructure.DataSourceVerifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Scanner;

/**
 * InfrastructureRunner - Comprehensive APEX Infrastructure Demonstrations
 * 
 * This runner provides complete coverage of all infrastructure-related demos in the APEX Rules Engine,
 * demonstrating data management, system setup, bootstrapping, and operational capabilities.
 * 
 * INFRASTRUCTURE CATEGORIES COVERED:
 * • Data Management: Service managers, data providers, and data source management
 * • System Setup: Database configuration, external dataset setup, and bootstrapping
 * • File Processing: XML generation, file handling, and data transformation
 * • Testing & Validation: Compliance testing, data source verification, and quality assurance
 * • Production Support: Production-ready configurations and operational utilities
 * • Bootstrap Operations: Complete system initialization and demo data setup
 * 
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-08-29
 * @version 1.0
 */
public class InfrastructureRunner {
    
    private static final Logger logger = LoggerFactory.getLogger(InfrastructureRunner.class);
    
    /**
     * Main entry point for Infrastructure demos.
     * 
     * @param args command line arguments: empty for interactive mode, demo name for direct execution
     */
    public static void main(String[] args) {
        InfrastructureRunner runner = new InfrastructureRunner();
        
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
                System.out.println("\n✅ Thank you for exploring APEX Infrastructure capabilities!");
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
        System.out.println("Running infrastructure demo: " + demoName);
        System.out.println();
        
        executeChoice(demoName.toLowerCase());
    }
    
    /**
     * Display the runner banner.
     */
    private void displayBanner() {
        logger.info("╔" + "═".repeat(78) + "╗");
        logger.info("║" + " ".repeat(19) + "APEX RULES ENGINE - INFRASTRUCTURE DEMOS" + " ".repeat(18) + "║");
        logger.info("╚" + "═".repeat(78) + "╝");
        logger.info("");
        logger.info("Welcome to comprehensive APEX infrastructure demonstrations! 🏗️");
        logger.info("");
        logger.info("These demos showcase data management, system setup, bootstrapping,");
        logger.info("and operational capabilities of the APEX Rules Engine.");
        logger.info("");
    }
    
    /**
     * Display the interactive menu.
     */
    private void displayMenu() {
        System.out.println("Available Infrastructure Demonstrations:");
        System.out.println("  1. Data Service Manager Demo - Core data service management");
        System.out.println("  2. Production Data Service Manager - Production-ready configuration");
        System.out.println("  3. Database Setup - Database initialization and configuration");
        System.out.println("  4. Rule Config Database Setup - Rule configuration database setup");
        System.out.println("  5. Demo Data Bootstrap - Complete demo data initialization");
        System.out.println("  6. External Dataset Setup - External data source configuration");
        System.out.println("  7. File Processing Demo - File handling and transformation");
        System.out.println("  8. XML Data Generator - XML data generation utilities");
        System.out.println("  9. Data Provider Compliance Test - Compliance validation testing");
        System.out.println(" 10. Data Source Verifier - Data source validation and verification");
        System.out.println(" 11. Run All Infrastructure Demos - Execute complete suite");
        System.out.println();
    }
    
    /**
     * Execute the selected choice.
     */
    private void executeChoice(String choice) {
        try {
            switch (choice) {
                case "1":
                case "dataservice":
                case "manager":
                    runDemo("Data Service Manager Demo", DataServiceManagerDemo::main);
                    break;
                case "2":
                case "production":
                case "productionmanager":
                    runDemo("Production Data Service Manager", ProductionDemoDataServiceManager::main);
                    break;
                case "3":
                case "database":
                case "setup":
                    runDemo("Database Setup", DatabaseSetup::main);
                    break;
                case "4":
                case "ruleconfig":
                case "configsetup":
                    runDemo("Rule Config Database Setup", RuleConfigDatabaseSetup::main);
                    break;
                case "5":
                case "bootstrap":
                case "demodata":
                    runDemo("Demo Data Bootstrap", DemoDataBootstrap::main);
                    break;
                case "6":
                case "external":
                case "dataset":
                    runDemo("External Dataset Setup", ExternalDatasetSetup::main);
                    break;
                case "7":
                case "file":
                case "processing":
                    runDemo("File Processing Demo", FileProcessingDemo::main);
                    break;
                case "8":
                case "xml":
                case "generator":
                    runDemo("XML Data Generator", XmlDataGenerator::main);
                    break;
                case "9":
                case "compliance":
                case "test":
                    runDemo("Data Provider Compliance Test", DataProviderComplianceTest::main);
                    break;
                case "10":
                case "verifier":
                case "verification":
                    runDemo("Data Source Verifier", DataSourceVerifier::main);
                    break;
                case "11":
                case "all":
                    runAllInfrastructureDemos();
                    break;
                default:
                    System.out.println("❌ Invalid choice: " + choice);
                    System.out.println("Please try again or type 'q' to quit.");
                    break;
            }
        } catch (Exception e) {
            logger.error("❌ Error executing infrastructure demo: {}", e.getMessage(), e);
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
            logger.info("🎯 The infrastructure concepts and patterns are still demonstrated!");
        }
    }
    
    /**
     * Run all infrastructure demos in sequence.
     */
    private void runAllInfrastructureDemos() {
        logger.info("🚀 Running All Infrastructure Demonstrations");
        logger.info("═".repeat(50));
        logger.info("");
        
        long totalStartTime = System.currentTimeMillis();
        
        // Data management
        runDemo("Data Service Manager Demo", DataServiceManagerDemo::main);
        runDemo("Production Data Service Manager", ProductionDemoDataServiceManager::main);
        
        // System setup
        runDemo("Database Setup", DatabaseSetup::main);
        runDemo("Rule Config Database Setup", RuleConfigDatabaseSetup::main);
        runDemo("Demo Data Bootstrap", DemoDataBootstrap::main);
        runDemo("External Dataset Setup", ExternalDatasetSetup::main);
        
        // File processing
        runDemo("File Processing Demo", FileProcessingDemo::main);
        runDemo("XML Data Generator", XmlDataGenerator::main);
        
        // Testing and validation
        runDemo("Data Provider Compliance Test", DataProviderComplianceTest::main);
        runDemo("Data Source Verifier", DataSourceVerifier::main);
        
        long totalEndTime = System.currentTimeMillis();
        long totalTime = totalEndTime - totalStartTime;
        
        logger.info("");
        logger.info("🎉 All infrastructure demonstrations completed!");
        logger.info("Total execution time: {} ms ({} seconds)", totalTime, totalTime / 1000.0);
        logger.info("");
        logger.info("You've experienced the full range of APEX infrastructure capabilities!");
    }
    
    /**
     * Functional interface for demo execution.
     */
    @FunctionalInterface
    private interface DemoExecutor {
        void execute(String[] args) throws Exception;
    }
}
