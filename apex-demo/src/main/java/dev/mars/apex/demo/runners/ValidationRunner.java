package dev.mars.apex.demo.runners;

import dev.mars.apex.demo.validation.QuickStartDemo;
import dev.mars.apex.demo.validation.BasicUsageExamples;
import dev.mars.apex.demo.validation.CommoditySwapValidationBootstrap;
import dev.mars.apex.demo.validation.CommoditySwapValidationQuickDemo;
import dev.mars.apex.demo.validation.IntegratedValidatorDemo;
import dev.mars.apex.demo.validation.IntegratedTradeValidatorComplexDemo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Scanner;

/**
 * ValidationRunner - APEX Validation Demonstrations
 * 
 * This runner provides complete coverage of all validation-related demos in the APEX Rules Engine.
 * 
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-08-29
 * @version 1.0
 */
public class ValidationRunner {
    
    private static final Logger logger = LoggerFactory.getLogger(ValidationRunner.class);
    
    public static void main(String[] args) {
        ValidationRunner runner = new ValidationRunner();
        
        if (args.length == 0) {
            runner.runInteractiveMode();
        } else {
            runner.runDirectMode(args[0]);
        }
    }
    
    private void runInteractiveMode() {
        displayBanner();
        
        Scanner scanner = new Scanner(System.in);
        
        while (true) {
            displayMenu();
            System.out.print("Enter your choice (1-9, or 'q' to quit): ");
            String choice = scanner.nextLine().trim().toLowerCase();
            
            if (choice.equals("q") || choice.equals("quit") || choice.equals("exit")) {
                System.out.println("\nThank you for exploring APEX Validation capabilities!");
                break;
            }
            
            executeChoice(choice);
        }
        
        scanner.close();
    }
    
    private void runDirectMode(String demoName) {
        displayBanner();
        System.out.println("Running validation demo: " + demoName);
        System.out.println();
        
        executeChoice(demoName.toLowerCase());
    }
    
    private void displayBanner() {
        System.out.println("================================================================================");
        System.out.println("                    APEX RULES ENGINE - VALIDATION DEMOS                       ");
        System.out.println("================================================================================");
        System.out.println();
        System.out.println("Welcome to comprehensive APEX validation demonstrations!");
        System.out.println();
    }
    
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
    
    private void executeChoice(String choice) {
        try {
            switch (choice) {
                case "1":
                case "quickstart":
                case "quick":
                    runDemo("Quick Start Demo", () -> new QuickStartDemo().run());
                    break;
                case "2":
                case "basic":
                case "usage":
                    runDemo("Basic Usage Examples", () -> BasicUsageExamples.main(new String[]{}));
                    break;
                case "3":
                case "commodity":
                case "bootstrap":
                    runDemo("Commodity Swap Validation (Bootstrap)", () -> CommoditySwapValidationBootstrap.main(new String[]{}));
                    break;
                case "4":
                case "commodityquick":
                case "swap":
                    runDemo("Commodity Swap Validation (Quick)", () -> CommoditySwapValidationQuickDemo.main(new String[]{}));
                    break;
                case "5":
                case "customer":
                    runDemo("Integrated Customer Validator", () -> IntegratedValidatorDemo.main(new String[]{"Customer"}));
                    break;
                case "6":
                case "trade":
                    runDemo("Integrated Trade Validator", () -> IntegratedValidatorDemo.main(new String[]{"Trade"}));
                    break;
                case "7":
                case "tradecomplex":
                case "complex":
                    runDemo("Integrated Trade Validator (Complex)", () -> IntegratedTradeValidatorComplexDemo.main(new String[]{}));
                    break;
                case "8":
                case "product":
                    runDemo("Integrated Product Validator", () -> IntegratedValidatorDemo.main(new String[]{"Product"}));
                    break;
                case "9":
                case "all":
                    runAllValidationDemos();
                    break;
                default:
                    System.out.println("Invalid choice: " + choice);
                    System.out.println("Please try again or type 'q' to quit.");
                    break;
            }
        } catch (Exception e) {
            logger.error("Error executing validation demo: {}", e.getMessage(), e);
        }

        System.out.println();
    }
    
    private void runDemo(String demoName, DemoExecutor executor) {
        logger.info("Running: {}", demoName);
        logger.info("--------------------------------------------------");

        try {
            long startTime = System.currentTimeMillis();
            executor.execute();
            long endTime = System.currentTimeMillis();

            logger.info("{} completed successfully", demoName);
            logger.info("Execution time: {} ms", (endTime - startTime));

        } catch (Exception e) {
            logger.warn("{} encountered issues: {}", demoName, e.getMessage());
            logger.info("This may be expected if dependencies are not available");
        }
    }
    
    private void runAllValidationDemos() {
        logger.info("Running All Validation Demonstrations");
        logger.info("==================================================");
        logger.info("");

        long totalStartTime = System.currentTimeMillis();

        runDemo("Quick Start Demo", () -> new QuickStartDemo().run());
        runDemo("Basic Usage Examples", () -> BasicUsageExamples.main(new String[]{}));
        runDemo("Commodity Swap Validation (Bootstrap)", () -> CommoditySwapValidationBootstrap.main(new String[]{}));
        runDemo("Commodity Swap Validation (Quick)", () -> CommoditySwapValidationQuickDemo.main(new String[]{}));
        runDemo("Integrated Customer Validator", () -> IntegratedValidatorDemo.main(new String[]{"Customer"}));
        runDemo("Integrated Trade Validator", () -> IntegratedValidatorDemo.main(new String[]{"Trade"}));
        runDemo("Integrated Trade Validator (Complex)", () -> IntegratedTradeValidatorComplexDemo.main(new String[]{}));
        runDemo("Integrated Product Validator", () -> IntegratedValidatorDemo.main(new String[]{"Product"}));

        long totalEndTime = System.currentTimeMillis();
        long totalTime = totalEndTime - totalStartTime;

        logger.info("");
        logger.info("All validation demonstrations completed!");
        logger.info("Total execution time: {} ms ({} seconds)", totalTime, totalTime / 1000.0);
    }
    
    @FunctionalInterface
    private interface DemoExecutor {
        void execute() throws Exception;
    }
}
