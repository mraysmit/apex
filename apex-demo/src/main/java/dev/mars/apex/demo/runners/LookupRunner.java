package dev.mars.apex.demo.runners;

import dev.mars.apex.demo.lookup.CompoundKeyLookupDemo;
import dev.mars.apex.demo.lookup.DatabaseConnectionTest;
import dev.mars.apex.demo.lookup.DatabaseConnectivityTest;
import dev.mars.apex.demo.lookup.DataSourceConfigurationTest;
import dev.mars.apex.demo.lookup.ExternalDataSourceReferenceDemo;
import dev.mars.apex.demo.lookup.ExternalDataSourceReferenceValidationTest;
import dev.mars.apex.demo.lookup.ExternalDataSourceWorkingDemo;
import dev.mars.apex.demo.lookup.ExternalReferenceDebugTest;
import dev.mars.apex.demo.lookup.NestedFieldLookupDemo;
import dev.mars.apex.demo.lookup.ParameterExtractionTest;
import dev.mars.apex.demo.lookup.PostgreSQLExternalReferenceTest;
import dev.mars.apex.demo.lookup.PostgreSQLLookupDemo;
import dev.mars.apex.demo.lookup.SimpleFieldLookupDemo;
import dev.mars.apex.demo.lookup.SimplePostgreSQLLookupDemo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Scanner;

/**
 * LookupRunner - APEX Lookup Demonstrations
 * 
 * This runner provides complete coverage of all lookup-related demos in the APEX Rules Engine.
 * 
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-08-29
 * @version 1.0
 */
public class LookupRunner {
    
    private static final Logger logger = LoggerFactory.getLogger(LookupRunner.class);
    
    public static void main(String[] args) {
        LookupRunner runner = new LookupRunner();
        
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
                System.out.println("\nThank you for exploring APEX Lookup capabilities!");
                break;
            }
            
            executeChoice(choice);
        }
        
        scanner.close();
    }
    
    private void runDirectMode(String demoName) {
        displayBanner();
        System.out.println("Running lookup demo: " + demoName);
        System.out.println();
        
        executeChoice(demoName.toLowerCase());
    }
    
    private void displayBanner() {
        System.out.println("================================================================================");
        System.out.println("                      APEX RULES ENGINE - LOOKUP DEMOS                         ");
        System.out.println("================================================================================");
        System.out.println();
        System.out.println("Welcome to comprehensive APEX lookup demonstrations!");
        System.out.println();
    }
    
    private void displayMenu() {
        System.out.println("Available Lookup Demonstrations:");
        System.out.println("  1. Simple Field Lookup Demo - Basic field lookup patterns");
        System.out.println("  2. Compound Key Lookup Demo - Multi-key lookup operations");
        System.out.println("  3. Nested Field Lookup Demo - Complex nested data lookups");
        System.out.println("  4. External Data Source Reference - Database/API lookups");
        System.out.println("  5. Database Connection Test - Database connectivity testing");
        System.out.println("  6. Database Connectivity Test - Advanced connectivity testing");
        System.out.println("  7. Data Source Configuration Test - Configuration validation");
        System.out.println("  8. External Data Source Reference Validation Test - Reference validation");
        System.out.println("  9. External Data Source Working Demo - Working external data demo");
        System.out.println(" 10. External Reference Debug Test - Debug and troubleshooting");
        System.out.println(" 11. Parameter Extraction Test - Parameter handling testing");
        System.out.println(" 12. PostgreSQL External Reference Test - PostgreSQL reference testing");
        System.out.println(" 13. PostgreSQL Lookup Demo - PostgreSQL lookup operations");
        System.out.println(" 14. Simple PostgreSQL Lookup Demo - Basic PostgreSQL lookups");
        System.out.println(" 15. Run All Lookup Demos - Execute complete suite");
        System.out.println();
    }
    
    private void executeChoice(String choice) {
        try {
            switch (choice) {
                case "1":
                case "simple":
                case "field":
                    runDemo("Simple Field Lookup Demo", () -> SimpleFieldLookupDemo.main(new String[]{}));
                    break;
                case "2":
                case "compound":
                case "key":
                    runDemo("Compound Key Lookup Demo", () -> CompoundKeyLookupDemo.main(new String[]{}));
                    break;
                case "3":
                case "nested":
                case "complex":
                    runDemo("Nested Field Lookup Demo", () -> NestedFieldLookupDemo.main(new String[]{}));
                    break;
                case "4":
                case "external":
                case "reference":
                    runDemo("External Data Source Reference", () -> ExternalDataSourceReferenceDemo.main(new String[]{}));
                    break;
                case "5":
                case "dbconnection":
                    runDemo("Database Connection Test", () -> DatabaseConnectionTest.main(new String[]{}));
                    break;
                case "6":
                case "dbconnectivity":
                    runDemo("Database Connectivity Test", () -> DatabaseConnectivityTest.main(new String[]{}));
                    break;
                case "7":
                case "datasourceconfig":
                    runDemo("Data Source Configuration Test", () -> DataSourceConfigurationTest.main(new String[]{}));
                    break;
                case "8":
                case "validation":
                    runDemo("External Data Source Reference Validation Test", () -> ExternalDataSourceReferenceValidationTest.main(new String[]{}));
                    break;
                case "9":
                case "working":
                    runDemo("External Data Source Working Demo", () -> ExternalDataSourceWorkingDemo.main(new String[]{}));
                    break;
                case "10":
                case "debug":
                    runDemo("External Reference Debug Test", () -> ExternalReferenceDebugTest.main(new String[]{}));
                    break;
                case "11":
                case "parameter":
                    runDemo("Parameter Extraction Test", () -> ParameterExtractionTest.main(new String[]{}));
                    break;
                case "12":
                case "postgresql":
                    runDemo("PostgreSQL External Reference Test", () -> PostgreSQLExternalReferenceTest.main(new String[]{}));
                    break;
                case "13":
                case "postgresqllookup":
                    runDemo("PostgreSQL Lookup Demo", () -> PostgreSQLLookupDemo.main(new String[]{}));
                    break;
                case "14":
                case "simplepostgresql":
                    runDemo("Simple PostgreSQL Lookup Demo", () -> SimplePostgreSQLLookupDemo.main(new String[]{}));
                    break;
                case "15":
                case "all":
                    runAllLookupDemos();
                    break;
                default:
                    System.out.println("Invalid choice: " + choice);
                    System.out.println("Please try again or type 'q' to quit.");
                    break;
            }
        } catch (Exception e) {
            logger.error("Error executing lookup demo: {}", e.getMessage(), e);
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
    
    private void runAllLookupDemos() {
        logger.info("Running All Lookup Demonstrations");
        logger.info("==================================================");
        logger.info("");
        
        long totalStartTime = System.currentTimeMillis();
        
        runDemo("Simple Field Lookup Demo", () -> SimpleFieldLookupDemo.main(new String[]{}));
        runDemo("Compound Key Lookup Demo", () -> CompoundKeyLookupDemo.main(new String[]{}));
        runDemo("Nested Field Lookup Demo", () -> NestedFieldLookupDemo.main(new String[]{}));
        runDemo("External Data Source Reference", () -> ExternalDataSourceReferenceDemo.main(new String[]{}));
        runDemo("Database Connection Test", () -> DatabaseConnectionTest.main(new String[]{}));
        runDemo("Database Connectivity Test", () -> DatabaseConnectivityTest.main(new String[]{}));
        runDemo("Data Source Configuration Test", () -> DataSourceConfigurationTest.main(new String[]{}));
        runDemo("External Data Source Reference Validation Test", () -> ExternalDataSourceReferenceValidationTest.main(new String[]{}));
        runDemo("External Data Source Working Demo", () -> ExternalDataSourceWorkingDemo.main(new String[]{}));
        runDemo("External Reference Debug Test", () -> ExternalReferenceDebugTest.main(new String[]{}));
        runDemo("Parameter Extraction Test", () -> ParameterExtractionTest.main(new String[]{}));
        runDemo("PostgreSQL External Reference Test", () -> PostgreSQLExternalReferenceTest.main(new String[]{}));
        runDemo("PostgreSQL Lookup Demo", () -> PostgreSQLLookupDemo.main(new String[]{}));
        runDemo("Simple PostgreSQL Lookup Demo", () -> SimplePostgreSQLLookupDemo.main(new String[]{}));
        
        long totalEndTime = System.currentTimeMillis();
        long totalTime = totalEndTime - totalStartTime;
        
        logger.info("");
        logger.info("All lookup demonstrations completed!");
        logger.info("Total execution time: {} ms ({} seconds)", totalTime, totalTime / 1000.0);
    }
    
    @FunctionalInterface
    private interface DemoExecutor {
        void execute() throws Exception;
    }
}
