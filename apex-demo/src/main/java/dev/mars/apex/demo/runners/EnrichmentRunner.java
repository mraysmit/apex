package dev.mars.apex.demo.runners;

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
 * EnrichmentRunner - APEX Enrichment Demonstrations
 * 
 * This runner provides complete coverage of all enrichment-related demos in the APEX Rules Engine.
 * 
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-08-29
 * @version 1.0
 */
public class EnrichmentRunner {
    
    private static final Logger logger = LoggerFactory.getLogger(EnrichmentRunner.class);
    
    public static void main(String[] args) {
        EnrichmentRunner runner = new EnrichmentRunner();
        
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
            System.out.print("Enter your choice (1-11, or 'q' to quit): ");
            String choice = scanner.nextLine().trim().toLowerCase();
            
            if (choice.equals("q") || choice.equals("quit") || choice.equals("exit")) {
                System.out.println("\nThank you for exploring APEX Enrichment capabilities!");
                break;
            }
            
            executeChoice(choice);
        }
        
        scanner.close();
    }
    
    private void runDirectMode(String demoName) {
        displayBanner();
        System.out.println("Running enrichment demo: " + demoName);
        System.out.println();
        
        executeChoice(demoName.toLowerCase());
    }
    
    private void displayBanner() {
        System.out.println("================================================================================");
        System.out.println("                    APEX RULES ENGINE - ENRICHMENT DEMOS                       ");
        System.out.println("================================================================================");
        System.out.println();
        System.out.println("Welcome to comprehensive APEX enrichment demonstrations!");
        System.out.println();
    }
    
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
    
    private void executeChoice(String choice) {
        try {
            switch (choice) {
                case "1":
                case "data":
                case "management":
                    runDemo("Data Management Demo", () -> DataManagementDemo.main(new String[]{}));
                    break;
                case "2":
                case "yaml":
                case "dataset":
                    runDemo("YAML Dataset Demo", () -> YamlDatasetDemo.main(new String[]{}));
                    break;
                case "3":
                case "external":
                case "datasource":
                    runDemo("External Data Source Demo", () -> ExternalDataSourceDemo.main(new String[]{}));
                    break;
                case "4":
                case "batch":
                case "processing":
                    runDemo("Batch Processing Demo", () -> BatchProcessingDemo.main(new String[]{}));
                    break;
                case "5":
                case "financial":
                case "settlement":
                    runDemo("Financial Settlement Demo", () -> ComprehensiveFinancialSettlementDemo.main(new String[]{}));
                    break;
                case "6":
                case "custody":
                case "repair":
                    runDemo("Custody Auto-Repair Demo", () -> CustodyAutoRepairDemo.main(new String[]{}));
                    break;
                case "7":
                case "custodybootstrap":
                case "bootstrap":
                    runDemo("Custody Auto-Repair Bootstrap", () -> CustodyAutoRepairBootstrap.main(new String[]{}));
                    break;
                case "8":
                case "otc":
                case "options":
                    runDemo("OTC Options Bootstrap", () -> OtcOptionsBootstrapDemo.main(new String[]{}));
                    break;
                case "9":
                case "customer":
                case "transformer":
                    runDemo("Customer Transformer Demo", () -> CustomerTransformerDemo.main(new String[]{}));
                    break;
                case "10":
                case "trade":
                case "tradetransformer":
                    runDemo("Trade Transformer Demo", () -> TradeTransformerDemo.main(new String[]{}));
                    break;
                case "11":
                case "all":
                    runAllEnrichmentDemos();
                    break;
                default:
                    System.out.println("Invalid choice: " + choice);
                    System.out.println("Please try again or type 'q' to quit.");
                    break;
            }
        } catch (Exception e) {
            logger.error("Error executing enrichment demo: {}", e.getMessage(), e);
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
    
    private void runAllEnrichmentDemos() {
        logger.info("Running All Enrichment Demonstrations");
        logger.info("==================================================");
        logger.info("");
        
        long totalStartTime = System.currentTimeMillis();
        
        runDemo("Data Management Demo", () -> DataManagementDemo.main(new String[]{}));
        runDemo("YAML Dataset Demo", () -> YamlDatasetDemo.main(new String[]{}));
        runDemo("External Data Source Demo", () -> ExternalDataSourceDemo.main(new String[]{}));
        runDemo("Batch Processing Demo", () -> BatchProcessingDemo.main(new String[]{}));
        runDemo("Financial Settlement Demo", () -> ComprehensiveFinancialSettlementDemo.main(new String[]{}));
        runDemo("Custody Auto-Repair Demo", () -> CustodyAutoRepairDemo.main(new String[]{}));
        runDemo("Custody Auto-Repair Bootstrap", () -> CustodyAutoRepairBootstrap.main(new String[]{}));
        runDemo("OTC Options Bootstrap", () -> OtcOptionsBootstrapDemo.main(new String[]{}));
        runDemo("Customer Transformer Demo", () -> CustomerTransformerDemo.main(new String[]{}));
        runDemo("Trade Transformer Demo", () -> TradeTransformerDemo.main(new String[]{}));
        
        long totalEndTime = System.currentTimeMillis();
        long totalTime = totalEndTime - totalStartTime;
        
        logger.info("");
        logger.info("All enrichment demonstrations completed!");
        logger.info("Total execution time: {} ms ({} seconds)", totalTime, totalTime / 1000.0);
    }
    
    @FunctionalInterface
    private interface DemoExecutor {
        void execute() throws Exception;
    }
}
