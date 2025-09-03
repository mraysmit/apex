package dev.mars.apex.demo.runners;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Scanner;

/**
 * AllDemosRunner - Master APEX Demonstrations Runner
 * 
 * This runner provides access to all APEX demo categories through individual specialized runners.
 * 
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-08-29
 * @version 1.0
 */
public class AllDemosRunner {
    
    private static final Logger logger = LoggerFactory.getLogger(AllDemosRunner.class);
    
    public static void main(String[] args) {
        AllDemosRunner runner = new AllDemosRunner();
        
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
                System.out.println("\nThank you for exploring APEX Rules Engine capabilities!");
                break;
            }
            
            executeChoice(choice);
        }
        
        scanner.close();
    }
    
    private void runDirectMode(String runnerName) {
        displayBanner();
        System.out.println("Launching runner: " + runnerName);
        System.out.println();
        
        executeChoice(runnerName.toLowerCase());
    }
    
    private void displayBanner() {
        System.out.println("================================================================================");
        System.out.println("                        APEX RULES ENGINE - ALL DEMOS                          ");
        System.out.println("================================================================================");
        System.out.println();
        System.out.println("Welcome to the complete APEX Rules Engine demonstration suite!");
        System.out.println("Choose a category to explore specific functionality areas.");
        System.out.println();
    }
    
    private void displayMenu() {
        System.out.println("Available Demo Categories:");
        System.out.println("  1. Validation Demos - Data quality and business rule validation (8 demos)");
        System.out.println("  2. Enrichment Demos - Data transformation and enhancement (10 demos)");
        System.out.println("  3. Lookup Demos - Data lookup and reference operations (14 demos)");
        System.out.println("  4. Evaluation Demos - Expression and rule evaluation (20 demos)");
        System.out.println("  5. Infrastructure Demos - Configuration and setup (4 demos)");
        System.out.println("  6. Utility Demos - Testing and validation utilities (3 demos)");
        System.out.println("  7. Run All Categories - Execute all demo runners sequentially");
        System.out.println();
    }
    
    private void executeChoice(String choice) {
        try {
            switch (choice) {
                case "1":
                case "validation":
                case "validate":
                    runRunner("Validation Demos", () -> ValidationRunner.main(new String[]{}));
                    break;
                case "2":
                case "enrichment":
                case "enrich":
                    runRunner("Enrichment Demos", () -> EnrichmentRunner.main(new String[]{}));
                    break;
                case "3":
                case "lookup":
                case "lookups":
                    runRunner("Lookup Demos", () -> LookupRunner.main(new String[]{}));
                    break;
                case "4":
                case "evaluation":
                case "evaluate":
                    runRunner("Evaluation Demos", () -> EvaluationRunner.main(new String[]{}));
                    break;
                case "5":
                case "infrastructure":
                case "infra":
                    runRunner("Infrastructure Demos", () -> InfrastructureRunner.main(new String[]{}));
                    break;
                case "6":
                case "utility":
                case "util":
                    runRunner("Utility Demos", () -> UtilRunner.main(new String[]{}));
                    break;
                case "7":
                case "all":
                    runAllCategories();
                    break;
                default:
                    System.out.println("Invalid choice: " + choice);
                    System.out.println("Please try again or type 'q' to quit.");
                    break;
            }
        } catch (Exception e) {
            logger.error("Error executing demo runner: {}", e.getMessage(), e);
        }
        
        System.out.println();
    }
    
    private void runRunner(String runnerName, DemoExecutor executor) {
        logger.info("Launching: {}", runnerName);
        logger.info("--------------------------------------------------");
        
        try {
            long startTime = System.currentTimeMillis();
            executor.execute();
            long endTime = System.currentTimeMillis();
            
            logger.info("{} completed successfully", runnerName);
            logger.info("Execution time: {} ms", (endTime - startTime));
            
        } catch (Exception e) {
            logger.warn("{} encountered issues: {}", runnerName, e.getMessage());
            logger.info("This may be expected if dependencies are not available");
        }
    }
    
    private void runAllCategories() {
        logger.info("Running All Demo Categories");
        logger.info("==================================================");
        logger.info("");
        
        long totalStartTime = System.currentTimeMillis();
        
        runRunner("Validation Demos", () -> ValidationRunner.main(new String[]{"all"}));
        runRunner("Enrichment Demos", () -> EnrichmentRunner.main(new String[]{"all"}));
        runRunner("Lookup Demos", () -> LookupRunner.main(new String[]{"all"}));
        runRunner("Evaluation Demos", () -> EvaluationRunner.main(new String[]{"all"}));
        runRunner("Infrastructure Demos", () -> InfrastructureRunner.main(new String[]{"all"}));
        runRunner("Utility Demos", () -> UtilRunner.main(new String[]{"all"}));
        
        long totalEndTime = System.currentTimeMillis();
        long totalTime = totalEndTime - totalStartTime;
        
        logger.info("");
        logger.info("All demo categories completed!");
        logger.info("Total execution time: {} ms ({} seconds)", totalTime, totalTime / 1000.0);
        logger.info("");
        logger.info("You've experienced the complete APEX Rules Engine demonstration suite!");
    }
    
    @FunctionalInterface
    private interface DemoExecutor {
        void execute() throws Exception;
    }
}
