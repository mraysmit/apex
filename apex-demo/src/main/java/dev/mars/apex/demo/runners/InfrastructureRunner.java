package dev.mars.apex.demo.runners;

import dev.mars.apex.demo.infrastructure.DemoDataBootstrap;
import dev.mars.apex.demo.infrastructure.DatabaseSetup;
import dev.mars.apex.demo.infrastructure.DataServiceManagerDemo;
import dev.mars.apex.demo.infrastructure.FileProcessingDemo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Scanner;

/**
 * InfrastructureRunner - APEX Infrastructure Demonstrations
 * 
 * This runner provides complete coverage of all infrastructure-related demos in the APEX Rules Engine.
 * 
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-08-29
 * @version 1.0
 */
public class InfrastructureRunner {
    
    private static final Logger logger = LoggerFactory.getLogger(InfrastructureRunner.class);
    
    public static void main(String[] args) {
        InfrastructureRunner runner = new InfrastructureRunner();
        
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
            System.out.print("Enter your choice (1-5, or 'q' to quit): ");
            String choice = scanner.nextLine().trim().toLowerCase();
            
            if (choice.equals("q") || choice.equals("quit") || choice.equals("exit")) {
                System.out.println("\nThank you for exploring APEX Infrastructure capabilities!");
                break;
            }
            
            executeChoice(choice);
        }
        
        scanner.close();
    }
    
    private void runDirectMode(String demoName) {
        displayBanner();
        System.out.println("Running infrastructure demo: " + demoName);
        System.out.println();
        
        executeChoice(demoName.toLowerCase());
    }
    
    private void displayBanner() {
        System.out.println("================================================================================");
        System.out.println("                   APEX RULES ENGINE - INFRASTRUCTURE DEMOS                    ");
        System.out.println("================================================================================");
        System.out.println();
        System.out.println("Welcome to comprehensive APEX infrastructure demonstrations!");
        System.out.println();
    }
    
    private void displayMenu() {
        System.out.println("Available Infrastructure Demonstrations:");
        System.out.println("  1. Demo Data Bootstrap - Complete data infrastructure setup");
        System.out.println("  2. Database Setup - Database initialization and configuration");
        System.out.println("  3. Data Service Manager Demo - Service management patterns");
        System.out.println("  4. File Processing Demo - File handling and processing");
        System.out.println("  5. Run All Infrastructure Demos - Execute complete suite");
        System.out.println();
    }
    
    private void executeChoice(String choice) {
        try {
            switch (choice) {
                case "1":
                case "bootstrap":
                case "data":
                    runDemo("Demo Data Bootstrap", () -> {
                        DemoDataBootstrap bootstrap = new DemoDataBootstrap();
                        bootstrap.verifyDataSources();
                    });
                    break;
                case "2":
                case "database":
                case "setup":
                    runDemo("Database Setup", () -> {
                        DatabaseSetup setup = new DatabaseSetup();
                        setup.setupCounterpartyTable();
                        setup.verifyDatabaseSetup();
                    });
                    break;
                case "3":
                case "service":
                case "manager":
                    runDemo("Data Service Manager Demo", () -> DataServiceManagerDemo.main(new String[]{}));
                    break;
                case "4":
                case "file":
                case "processing":
                    runDemo("File Processing Demo", () -> FileProcessingDemo.main(new String[]{}));
                    break;
                case "5":
                case "all":
                    runAllInfrastructureDemos();
                    break;
                default:
                    System.out.println("Invalid choice: " + choice);
                    System.out.println("Please try again or type 'q' to quit.");
                    break;
            }
        } catch (Exception e) {
            logger.error("Error executing infrastructure demo: {}", e.getMessage(), e);
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
    
    private void runAllInfrastructureDemos() {
        logger.info("Running All Infrastructure Demonstrations");
        logger.info("==================================================");
        logger.info("");
        
        long totalStartTime = System.currentTimeMillis();
        
        runDemo("Demo Data Bootstrap", () -> {
            DemoDataBootstrap bootstrap = new DemoDataBootstrap();
            bootstrap.verifyDataSources();
        });
        runDemo("Database Setup", () -> {
            DatabaseSetup setup = new DatabaseSetup();
            setup.setupCounterpartyTable();
            setup.verifyDatabaseSetup();
        });
        runDemo("Data Service Manager Demo", () -> DataServiceManagerDemo.main(new String[]{}));
        runDemo("File Processing Demo", () -> FileProcessingDemo.main(new String[]{}));
        
        long totalEndTime = System.currentTimeMillis();
        long totalTime = totalEndTime - totalStartTime;
        
        logger.info("");
        logger.info("All infrastructure demonstrations completed!");
        logger.info("Total execution time: {} ms ({} seconds)", totalTime, totalTime / 1000.0);
    }
    
    @FunctionalInterface
    private interface DemoExecutor {
        void execute() throws Exception;
    }
}
