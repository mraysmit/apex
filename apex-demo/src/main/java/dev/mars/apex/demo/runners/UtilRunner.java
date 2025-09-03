package dev.mars.apex.demo.runners;

import dev.mars.apex.demo.util.TestUtilities;
import dev.mars.apex.demo.util.YamlDependencyAnalysisDemo;
import dev.mars.apex.demo.util.YamlValidationDemo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Scanner;

/**
 * UtilRunner - APEX Utility Demonstrations
 * 
 * This runner provides complete coverage of all utility-related demos in the APEX Rules Engine.
 * 
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-09-03
 * @version 1.0
 */
public class UtilRunner {
    
    private static final Logger logger = LoggerFactory.getLogger(UtilRunner.class);
    
    public static void main(String[] args) {
        UtilRunner runner = new UtilRunner();
        
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
            System.out.print("Enter your choice (1-4, or 'q' to quit): ");
            String choice = scanner.nextLine().trim().toLowerCase();
            
            if (choice.equals("q") || choice.equals("quit") || choice.equals("exit")) {
                System.out.println("\nThank you for exploring APEX Utility capabilities!");
                break;
            }
            
            executeChoice(choice);
        }
        
        scanner.close();
    }
    
    private void runDirectMode(String demoName) {
        displayBanner();
        System.out.println("Running utility demo: " + demoName);
        System.out.println();
        
        executeChoice(demoName.toLowerCase());
    }
    
    private void displayBanner() {
        System.out.println("================================================================================");
        System.out.println("                       APEX RULES ENGINE - UTILITY DEMOS                       ");
        System.out.println("================================================================================");
        System.out.println();
        System.out.println("Welcome to comprehensive APEX utility demonstrations!");
        System.out.println();
    }
    
    private void displayMenu() {
        System.out.println("Available Utility Demonstrations:");
        System.out.println("  1. Test Utilities - Testing and validation utilities");
        System.out.println("  2. YAML Dependency Analysis Demo - YAML dependency analysis");
        System.out.println("  3. YAML Validation Demo - YAML configuration validation");
        System.out.println("  4. Run All Utility Demos - Execute complete suite");
        System.out.println();
    }
    
    private void executeChoice(String choice) {
        try {
            switch (choice) {
                case "1":
                case "test":
                case "utilities":
                    runDemo("Test Utilities", () -> TestUtilities.main(new String[]{}));
                    break;
                case "2":
                case "dependency":
                case "analysis":
                    runDemo("YAML Dependency Analysis Demo", () -> YamlDependencyAnalysisDemo.main(new String[]{}));
                    break;
                case "3":
                case "validation":
                case "yaml":
                    runDemo("YAML Validation Demo", () -> YamlValidationDemo.main(new String[]{}));
                    break;
                case "4":
                case "all":
                    runAllUtilityDemos();
                    break;
                default:
                    System.out.println("Invalid choice: " + choice);
                    System.out.println("Please try again or type 'q' to quit.");
                    break;
            }
        } catch (Exception e) {
            logger.error("Error executing utility demo: {}", e.getMessage(), e);
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
    
    private void runAllUtilityDemos() {
        logger.info("Running All Utility Demonstrations");
        logger.info("==================================================");
        logger.info("");
        
        long totalStartTime = System.currentTimeMillis();
        
        runDemo("Test Utilities", () -> TestUtilities.main(new String[]{}));
        runDemo("YAML Dependency Analysis Demo", () -> YamlDependencyAnalysisDemo.main(new String[]{}));
        runDemo("YAML Validation Demo", () -> YamlValidationDemo.main(new String[]{}));
        
        long totalEndTime = System.currentTimeMillis();
        long totalTime = totalEndTime - totalStartTime;
        
        logger.info("");
        logger.info("All utility demonstrations completed!");
        logger.info("Total execution time: {} ms ({} seconds)", totalTime, totalTime / 1000.0);
    }
    
    @FunctionalInterface
    private interface DemoExecutor {
        void execute() throws Exception;
    }
}
