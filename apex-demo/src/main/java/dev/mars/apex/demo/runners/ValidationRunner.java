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

import dev.mars.apex.demo.validation.QuickStartDemo;
import dev.mars.apex.demo.validation.BasicUsageExamples;
import dev.mars.apex.demo.validation.CommoditySwapValidationBootstrap;
import dev.mars.apex.demo.validation.CommoditySwapValidationQuickDemo;
import dev.mars.apex.demo.validation.IntegratedCustomerValidatorDemo;
import dev.mars.apex.demo.validation.IntegratedTradeValidatorDemo;
import dev.mars.apex.demo.validation.IntegratedTradeValidatorComplexDemo;
import dev.mars.apex.demo.validation.IntegratedProductValidatorDemo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Scanner;

/**
 * ValidationRunner - Comprehensive APEX Validation Demonstrations
 * 
 * This runner provides complete coverage of all validation-related demos in the APEX Rules Engine,
 * demonstrating data quality, business rule validation, and compliance checking capabilities.
 * 
 * VALIDATION CATEGORIES COVERED:
 * • Quick Start: Basic validation concepts and simple examples
 * • Basic Usage: Fundamental validation patterns and techniques  
 * • Commodity Swaps: Financial instrument validation scenarios
 * • Integrated Validators: Complex multi-entity validation workflows
 * • Advanced Scenarios: Production-ready validation implementations
 * 
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-08-29
 * @version 1.0
 */
public class ValidationRunner {
    
    private static final Logger logger = LoggerFactory.getLogger(ValidationRunner.class);
    
    /**
     * Main entry point for Validation demos.
     * 
     * @param args command line arguments: empty for interactive mode, demo name for direct execution
     */
    public static void main(String[] args) {
        ValidationRunner runner = new ValidationRunner();
        
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
            System.out.print("Enter your choice (1-9, or 'q' to quit): ");
            String choice = scanner.nextLine().trim().toLowerCase();
            
            if (choice.equals("q") || choice.equals("quit") || choice.equals("exit")) {
                System.out.println("\n✅ Thank you for exploring APEX Validation capabilities!");
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
        System.out.println("Running validation demo: " + demoName);
        System.out.println();
        
        executeChoice(demoName.toLowerCase());
    }
    
    /**
     * Display the runner banner.
     */
    private void displayBanner() {
        logger.info("╔" + "═".repeat(78) + "╗");
        logger.info("║" + " ".repeat(22) + "APEX RULES ENGINE - VALIDATION DEMOS" + " ".repeat(19) + "║");
        logger.info("╚" + "═".repeat(78) + "╝");
        logger.info("");
        logger.info("Welcome to comprehensive APEX validation demonstrations! ✅");
        logger.info("");
        logger.info("These demos showcase data quality, business rule validation,");
        logger.info("and compliance checking capabilities of the APEX Rules Engine.");
        logger.info("");
    }
    
    /**
     * Display the interactive menu.
     */
    private void displayMenu() {
        System.out.println("Available Validation Demonstrations:");
        System.out.println("  1. Quick Start Demo - Perfect introduction to validation");
        System.out.println("  2. Basic Usage Examples - Fundamental validation patterns");
        System.out.println("  3. Commodity Swap Validation (Bootstrap) - Complete workflow");
        System.out.println("  4. Commodity Swap Validation (Quick) - Streamlined demo");
        System.out.println("  5. Integrated Customer Validator - Multi-entity validation");
        System.out.println("  6. Integrated Trade Validator - Trading validation workflows");
        System.out.println("  7. Integrated Trade Validator (Complex) - Advanced scenarios");
        System.out.println("  8. Integrated Product Validator - Product validation patterns");
        System.out.println("  9. Run All Validation Demos - Execute complete suite");
        System.out.println();
    }
    
    /**
     * Execute the selected choice.
     */
    private void executeChoice(String choice) {
        try {
            switch (choice) {
                case "1":
                case "quickstart":
                case "quick":
                    runDemo("Quick Start Demo", QuickStartDemo::main);
                    break;
                case "2":
                case "basic":
                case "usage":
                    runDemo("Basic Usage Examples", BasicUsageExamples::main);
                    break;
                case "3":
                case "commodity":
                case "bootstrap":
                    runDemo("Commodity Swap Validation (Bootstrap)", CommoditySwapValidationBootstrap::main);
                    break;
                case "4":
                case "commodityquick":
                case "swap":
                    runDemo("Commodity Swap Validation (Quick)", CommoditySwapValidationQuickDemo::main);
                    break;
                case "5":
                case "customer":
                    runDemo("Integrated Customer Validator", IntegratedCustomerValidatorDemo::main);
                    break;
                case "6":
                case "trade":
                    runDemo("Integrated Trade Validator", IntegratedTradeValidatorDemo::main);
                    break;
                case "7":
                case "tradecomplex":
                case "complex":
                    runDemo("Integrated Trade Validator (Complex)", IntegratedTradeValidatorComplexDemo::main);
                    break;
                case "8":
                case "product":
                    runDemo("Integrated Product Validator", IntegratedProductValidatorDemo::main);
                    break;
                case "9":
                case "all":
                    runAllValidationDemos();
                    break;
                default:
                    System.out.println("❌ Invalid choice: " + choice);
                    System.out.println("Please try again or type 'q' to quit.");
                    break;
            }
        } catch (Exception e) {
            logger.error("❌ Error executing validation demo: {}", e.getMessage(), e);
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
            logger.info("🎯 The validation concepts and patterns are still demonstrated!");
        }
    }
    
    /**
     * Run all validation demos in sequence.
     */
    private void runAllValidationDemos() {
        logger.info("🚀 Running All Validation Demonstrations");
        logger.info("═".repeat(50));
        logger.info("");
        
        long totalStartTime = System.currentTimeMillis();
        
        runDemo("Quick Start Demo", QuickStartDemo::main);
        runDemo("Basic Usage Examples", BasicUsageExamples::main);
        runDemo("Commodity Swap Validation (Bootstrap)", CommoditySwapValidationBootstrap::main);
        runDemo("Commodity Swap Validation (Quick)", CommoditySwapValidationQuickDemo::main);
        runDemo("Integrated Customer Validator", IntegratedCustomerValidatorDemo::main);
        runDemo("Integrated Trade Validator", IntegratedTradeValidatorDemo::main);
        runDemo("Integrated Trade Validator (Complex)", IntegratedTradeValidatorComplexDemo::main);
        runDemo("Integrated Product Validator", IntegratedProductValidatorDemo::main);
        
        long totalEndTime = System.currentTimeMillis();
        long totalTime = totalEndTime - totalStartTime;
        
        logger.info("");
        logger.info("🎉 All validation demonstrations completed!");
        logger.info("Total execution time: {} ms ({} seconds)", totalTime, totalTime / 1000.0);
        logger.info("");
        logger.info("You've experienced the full range of APEX validation capabilities!");
    }
    
    /**
     * Functional interface for demo execution.
     */
    @FunctionalInterface
    private interface DemoExecutor {
        void execute(String[] args) throws Exception;
    }
}
