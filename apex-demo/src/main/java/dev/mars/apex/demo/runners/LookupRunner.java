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

import dev.mars.apex.demo.lookup.SimpleFieldLookupDemo;
import dev.mars.apex.demo.lookup.NestedFieldLookupDemo;
import dev.mars.apex.demo.lookup.CompoundKeyLookupDemo;
import dev.mars.apex.demo.lookup.PostgreSQLLookupDemo;
import dev.mars.apex.demo.lookup.SimplePostgreSQLLookupDemo;
import dev.mars.apex.demo.lookup.ExternalDataSourceReferenceDemo;
import dev.mars.apex.demo.lookup.ExternalDataSourceWorkingDemo;
import dev.mars.apex.demo.lookup.DataSourceConfigurationTest;
import dev.mars.apex.demo.lookup.DatabaseConnectionTest;
import dev.mars.apex.demo.lookup.DatabaseConnectivityTest;
import dev.mars.apex.demo.lookup.ExternalReferenceDebugTest;
import dev.mars.apex.demo.lookup.ParameterExtractionTest;
import dev.mars.apex.demo.lookup.PostgreSQLExternalReferenceTest;
import dev.mars.apex.demo.lookup.ExternalDataSourceReferenceValidationTest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Scanner;

/**
 * LookupRunner - Comprehensive APEX Lookup Demonstrations
 * 
 * This runner provides complete coverage of all lookup-related demos in the APEX Rules Engine,
 * demonstrating reference data access, database integration, and lookup pattern implementations.
 * 
 * LOOKUP CATEGORIES COVERED:
 * • Simple Lookups: Basic key-value lookup patterns
 * • Complex Lookups: Nested fields, compound keys, conditional lookups
 * • Database Integration: PostgreSQL and other database lookups
 * • External Data Sources: API and file-based reference data
 * • Testing & Validation: Connection testing and parameter validation
 * • Advanced Patterns: Performance optimization and error handling
 * 
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-08-29
 * @version 1.0
 */
public class LookupRunner {
    
    private static final Logger logger = LoggerFactory.getLogger(LookupRunner.class);
    
    /**
     * Main entry point for Lookup demos.
     * 
     * @param args command line arguments: empty for interactive mode, demo name for direct execution
     */
    public static void main(String[] args) {
        LookupRunner runner = new LookupRunner();
        
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
            System.out.print("Enter your choice (1-15, or 'q' to quit): ");
            String choice = scanner.nextLine().trim().toLowerCase();
            
            if (choice.equals("q") || choice.equals("quit") || choice.equals("exit")) {
                System.out.println("\n✅ Thank you for exploring APEX Lookup capabilities!");
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
        System.out.println("Running lookup demo: " + demoName);
        System.out.println();
        
        executeChoice(demoName.toLowerCase());
    }
    
    /**
     * Display the runner banner.
     */
    private void displayBanner() {
        logger.info("╔" + "═".repeat(78) + "╗");
        logger.info("║" + " ".repeat(24) + "APEX RULES ENGINE - LOOKUP DEMOS" + " ".repeat(21) + "║");
        logger.info("╚" + "═".repeat(78) + "╝");
        logger.info("");
        logger.info("Welcome to comprehensive APEX lookup demonstrations! 🔍");
        logger.info("");
        logger.info("These demos showcase reference data access, database integration,");
        logger.info("and lookup pattern implementations in the APEX Rules Engine.");
        logger.info("");
    }
    
    /**
     * Display the interactive menu.
     */
    private void displayMenu() {
        System.out.println("Available Lookup Demonstrations:");
        System.out.println("  1. Simple Field Lookup - Basic key-value lookups");
        System.out.println("  2. Nested Field Lookup - Complex object navigation");
        System.out.println("  3. Compound Key Lookup - Multi-field lookup keys");
        System.out.println("  4. PostgreSQL Lookup - Database integration patterns");
        System.out.println("  5. Simple PostgreSQL Lookup - Streamlined database demo");
        System.out.println("  6. External Data Source Reference - External system integration");
        System.out.println("  7. External Data Source Working - Live external data demo");
        System.out.println("  8. Data Source Configuration Test - Configuration validation");
        System.out.println("  9. Database Connection Test - Connection testing utilities");
        System.out.println(" 10. Database Connectivity Test - Advanced connectivity testing");
        System.out.println(" 11. External Reference Debug Test - Debugging external references");
        System.out.println(" 12. Parameter Extraction Test - Parameter handling validation");
        System.out.println(" 13. PostgreSQL External Reference Test - External reference testing");
        System.out.println(" 14. External Data Source Validation Test - Comprehensive validation");
        System.out.println(" 15. Run All Lookup Demos - Execute complete suite");
        System.out.println();
    }
    
    /**
     * Execute the selected choice.
     */
    private void executeChoice(String choice) {
        try {
            switch (choice) {
                case "1":
                case "simple":
                case "field":
                    runDemo("Simple Field Lookup", SimpleFieldLookupDemo::main);
                    break;
                case "2":
                case "nested":
                    runDemo("Nested Field Lookup", NestedFieldLookupDemo::main);
                    break;
                case "3":
                case "compound":
                case "key":
                    runDemo("Compound Key Lookup", CompoundKeyLookupDemo::main);
                    break;
                case "4":
                case "postgresql":
                case "database":
                    runDemo("PostgreSQL Lookup", PostgreSQLLookupDemo::main);
                    break;
                case "5":
                case "simplepostgresql":
                case "simpledatabase":
                    runDemo("Simple PostgreSQL Lookup", SimplePostgreSQLLookupDemo::main);
                    break;
                case "6":
                case "external":
                case "reference":
                    runDemo("External Data Source Reference", ExternalDataSourceReferenceDemo::main);
                    break;
                case "7":
                case "externalworking":
                case "working":
                    runDemo("External Data Source Working", ExternalDataSourceWorkingDemo::main);
                    break;
                case "8":
                case "configuration":
                case "config":
                    runDemo("Data Source Configuration Test", DataSourceConfigurationTest::main);
                    break;
                case "9":
                case "connection":
                case "connectiontest":
                    runDemo("Database Connection Test", DatabaseConnectionTest::main);
                    break;
                case "10":
                case "connectivity":
                    runDemo("Database Connectivity Test", DatabaseConnectivityTest::main);
                    break;
                case "11":
                case "debug":
                case "externaldebug":
                    runDemo("External Reference Debug Test", ExternalReferenceDebugTest::main);
                    break;
                case "12":
                case "parameter":
                case "extraction":
                    runDemo("Parameter Extraction Test", ParameterExtractionTest::main);
                    break;
                case "13":
                case "postgresqlexternal":
                case "externalreference":
                    runDemo("PostgreSQL External Reference Test", PostgreSQLExternalReferenceTest::main);
                    break;
                case "14":
                case "validation":
                case "externalvalidation":
                    runDemo("External Data Source Validation Test", ExternalDataSourceReferenceValidationTest::main);
                    break;
                case "15":
                case "all":
                    runAllLookupDemos();
                    break;
                default:
                    System.out.println("❌ Invalid choice: " + choice);
                    System.out.println("Please try again or type 'q' to quit.");
                    break;
            }
        } catch (Exception e) {
            logger.error("❌ Error executing lookup demo: {}", e.getMessage(), e);
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
            logger.info("🎯 The lookup concepts and patterns are still demonstrated!");
        }
    }
    
    /**
     * Run all lookup demos in sequence.
     */
    private void runAllLookupDemos() {
        logger.info("🚀 Running All Lookup Demonstrations");
        logger.info("═".repeat(50));
        logger.info("");
        
        long totalStartTime = System.currentTimeMillis();
        
        // Core lookup patterns
        runDemo("Simple Field Lookup", SimpleFieldLookupDemo::main);
        runDemo("Nested Field Lookup", NestedFieldLookupDemo::main);
        runDemo("Compound Key Lookup", CompoundKeyLookupDemo::main);
        
        // Database integration
        runDemo("PostgreSQL Lookup", PostgreSQLLookupDemo::main);
        runDemo("Simple PostgreSQL Lookup", SimplePostgreSQLLookupDemo::main);
        
        // External data sources
        runDemo("External Data Source Reference", ExternalDataSourceReferenceDemo::main);
        runDemo("External Data Source Working", ExternalDataSourceWorkingDemo::main);
        
        // Testing and validation
        runDemo("Data Source Configuration Test", DataSourceConfigurationTest::main);
        runDemo("Database Connection Test", DatabaseConnectionTest::main);
        runDemo("Database Connectivity Test", DatabaseConnectivityTest::main);
        runDemo("External Reference Debug Test", ExternalReferenceDebugTest::main);
        runDemo("Parameter Extraction Test", ParameterExtractionTest::main);
        runDemo("PostgreSQL External Reference Test", PostgreSQLExternalReferenceTest::main);
        runDemo("External Data Source Validation Test", ExternalDataSourceReferenceValidationTest::main);
        
        long totalEndTime = System.currentTimeMillis();
        long totalTime = totalEndTime - totalStartTime;
        
        logger.info("");
        logger.info("🎉 All lookup demonstrations completed!");
        logger.info("Total execution time: {} ms ({} seconds)", totalTime, totalTime / 1000.0);
        logger.info("");
        logger.info("You've experienced the full range of APEX lookup capabilities!");
    }
    
    /**
     * Functional interface for demo execution.
     */
    @FunctionalInterface
    private interface DemoExecutor {
        void execute(String[] args) throws Exception;
    }
}
