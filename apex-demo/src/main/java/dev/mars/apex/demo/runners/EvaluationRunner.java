package dev.mars.apex.demo.runners;

import dev.mars.apex.demo.evaluation.ApexAdvancedFeaturesDemo;
import dev.mars.apex.demo.evaluation.ApexRulesEngineDemo;
import dev.mars.apex.demo.evaluation.LayeredAPIDemo;
import dev.mars.apex.demo.evaluation.PerformanceDemo;
import dev.mars.apex.demo.evaluation.YamlConfigurationDemo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Scanner;

/**
 * EvaluationRunner - APEX Evaluation Demonstrations
 * 
 * This runner provides complete coverage of all evaluation-related demos in the APEX Rules Engine.
 * 
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-08-29
 * @version 1.0
 */
public class EvaluationRunner {
    
    private static final Logger logger = LoggerFactory.getLogger(EvaluationRunner.class);
    
    public static void main(String[] args) {
        EvaluationRunner runner = new EvaluationRunner();
        
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
            System.out.print("Enter your choice (1-6, or 'q' to quit): ");
            String choice = scanner.nextLine().trim().toLowerCase();
            
            if (choice.equals("q") || choice.equals("quit") || choice.equals("exit")) {
                System.out.println("\nThank you for exploring APEX Evaluation capabilities!");
                break;
            }
            
            executeChoice(choice);
        }
        
        scanner.close();
    }
    
    private void runDirectMode(String demoName) {
        displayBanner();
        System.out.println("Running evaluation demo: " + demoName);
        System.out.println();
        
        executeChoice(demoName.toLowerCase());
    }
    
    private void displayBanner() {
        System.out.println("================================================================================");
        System.out.println("                     APEX RULES ENGINE - EVALUATION DEMOS                      ");
        System.out.println("================================================================================");
        System.out.println();
        System.out.println("Welcome to comprehensive APEX evaluation demonstrations!");
        System.out.println();
    }
    
    private void displayMenu() {
        System.out.println("Available Evaluation Demonstrations:");
        System.out.println("  1. APEX Rules Engine Demo - Core rule engine functionality");
        System.out.println("  2. Layered API Demo - Multi-layer API demonstrations");
        System.out.println("  3. Performance Demo - Performance testing and optimization");
        System.out.println("  4. YAML Configuration Demo - Configuration-driven processing");
        System.out.println("  5. Advanced Features Demo - Complex evaluation scenarios");
        System.out.println("  6. Run All Evaluation Demos - Execute complete suite");
        System.out.println();
    }
    
    private void executeChoice(String choice) {
        try {
            switch (choice) {
                case "1":
                case "engine":
                case "rules":
                    runDemo("APEX Rules Engine Demo", () -> ApexRulesEngineDemo.main(new String[]{}));
                    break;
                case "2":
                case "layered":
                case "api":
                    runDemo("Layered API Demo", () -> LayeredAPIDemo.main(new String[]{}));
                    break;
                case "3":
                case "performance":
                case "perf":
                    runDemo("Performance Demo", () -> PerformanceDemo.main(new String[]{}));
                    break;
                case "4":
                case "yaml":
                case "config":
                    runDemo("YAML Configuration Demo", () -> YamlConfigurationDemo.main(new String[]{}));
                    break;
                case "5":
                case "advanced":
                case "features":
                    runDemo("Advanced Features Demo", () -> ApexAdvancedFeaturesDemo.main(new String[]{}));
                    break;
                case "6":
                case "all":
                    runAllEvaluationDemos();
                    break;
                default:
                    System.out.println("Invalid choice: " + choice);
                    System.out.println("Please try again or type 'q' to quit.");
                    break;
            }
        } catch (Exception e) {
            logger.error("Error executing evaluation demo: {}", e.getMessage(), e);
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
    
    private void runAllEvaluationDemos() {
        logger.info("Running All Evaluation Demonstrations");
        logger.info("==================================================");
        logger.info("");
        
        long totalStartTime = System.currentTimeMillis();
        
        runDemo("APEX Rules Engine Demo", () -> ApexRulesEngineDemo.main(new String[]{}));
        runDemo("Layered API Demo", () -> LayeredAPIDemo.main(new String[]{}));
        runDemo("Performance Demo", () -> PerformanceDemo.main(new String[]{}));
        runDemo("YAML Configuration Demo", () -> YamlConfigurationDemo.main(new String[]{}));
        runDemo("Advanced Features Demo", () -> ApexAdvancedFeaturesDemo.main(new String[]{}));
        
        long totalEndTime = System.currentTimeMillis();
        long totalTime = totalEndTime - totalStartTime;
        
        logger.info("");
        logger.info("All evaluation demonstrations completed!");
        logger.info("Total execution time: {} ms ({} seconds)", totalTime, totalTime / 1000.0);
    }
    
    @FunctionalInterface
    private interface DemoExecutor {
        void execute() throws Exception;
    }
}
