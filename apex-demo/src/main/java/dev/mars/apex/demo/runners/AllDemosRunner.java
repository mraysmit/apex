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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Scanner;

/**
 * AllDemosRunner - Master APEX Demonstrations Launcher
 * 
 * This is the main entry point for all APEX Rules Engine demonstrations, providing organized access
 * to comprehensive demo suites across all APEX operation categories.
 * 
 * APEX OPERATION CATEGORIES:
 * • Validation: Data quality, business rule validation, and compliance checking
 * • Enrichment: Data transformation, lookup operations, and data enhancement
 * • Lookup: Reference data access, database integration, and lookup patterns
 * • Evaluation: Rule execution, business logic evaluation, and decision-making
 * • Infrastructure: Data management, system setup, bootstrapping, and operations
 * 
 * Each category runner provides comprehensive coverage of all related demonstrations,
 * with consistent interfaces, error handling, and execution reporting.
 * 
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-08-29
 * @version 1.0
 */
public class AllDemosRunner {
    
    private static final Logger logger = LoggerFactory.getLogger(AllDemosRunner.class);
    
    /**
     * Main entry point for all APEX demonstrations.
     * 
     * @param args command line arguments: empty for interactive mode, category name for direct execution
     */
    public static void main(String[] args) {
        AllDemosRunner runner = new AllDemosRunner();
        
        if (args.length == 0) {
            runner.runInteractiveMode();
        } else {
            runner.runDirectMode(args[0]);
        }
    }
    
    /**
     * Run in interactive mode with category selection.
     */
    private void runInteractiveMode() {
        displayBanner();
        
        Scanner scanner = new Scanner(System.in);
        
        while (true) {
            displayMenu();
            System.out.print("Enter your choice (1-7, or 'q' to quit): ");
            String choice = scanner.nextLine().trim().toLowerCase();
            
            if (choice.equals("q") || choice.equals("quit") || choice.equals("exit")) {
                System.out.println("\n✅ Thank you for exploring the APEX Rules Engine!");
                System.out.println("🎯 You've experienced the power of configuration-driven business rules!");
                break;
            }
            
            executeChoice(choice);
        }
        
        scanner.close();
    }
    
    /**
     * Run specific category directly.
     */
    private void runDirectMode(String categoryName) {
        displayBanner();
        System.out.println("Running APEX demo category: " + categoryName);
        System.out.println();
        
        executeChoice(categoryName.toLowerCase());
    }
    
    /**
     * Display the main banner.
     */
    private void displayBanner() {
        logger.info("╔" + "═".repeat(78) + "╗");
        logger.info("║" + " ".repeat(26) + "APEX RULES ENGINE DEMOS" + " ".repeat(29) + "║");
        logger.info("║" + " ".repeat(22) + "Complete Demonstration Suite" + " ".repeat(27) + "║");
        logger.info("╚" + "═".repeat(78) + "╝");
        logger.info("");
        logger.info("Welcome to the comprehensive APEX Rules Engine demonstration suite! 🚀");
        logger.info("");
        logger.info("APEX is a powerful, configuration-driven business rules engine that enables");
        logger.info("you to implement complex business logic through simple YAML configurations.");
        logger.info("");
        logger.info("The demonstrations are organized by the 5 core APEX operations:");
        logger.info("• VALIDATION - Data quality and business rule validation");
        logger.info("• ENRICHMENT - Data transformation and enhancement");
        logger.info("• LOOKUP - Reference data access and integration");
        logger.info("• EVALUATION - Rule execution and decision-making");
        logger.info("• INFRASTRUCTURE - System setup and data management");
        logger.info("");
    }
    
    /**
     * Display the category selection menu.
     */
    private void displayMenu() {
        System.out.println("APEX Demonstration Categories:");
        System.out.println("  1. Validation Demos - Data quality and compliance checking ✅");
        System.out.println("  2. Enrichment Demos - Data transformation and enhancement 🔧");
        System.out.println("  3. Lookup Demos - Reference data access and integration 🔍");
        System.out.println("  4. Evaluation Demos - Rule execution and decision-making ⚡");
        System.out.println("  5. Infrastructure Demos - System setup and data management 🏗️");
        System.out.println("  6. Quick Tour - Run sample demos from each category 🎯");
        System.out.println("  7. Complete Suite - Run ALL demonstrations (comprehensive) 🌟");
        System.out.println();
    }
    
    /**
     * Execute the selected choice.
     */
    private void executeChoice(String choice) {
        try {
            switch (choice) {
                case "1":
                case "validation":
                case "validate":
                    logger.info("🚀 Launching Validation Demonstrations...");
                    ValidationRunner.main(new String[]{});
                    break;
                case "2":
                case "enrichment":
                case "enrich":
                    logger.info("🚀 Launching Enrichment Demonstrations...");
                    EnrichmentRunner.main(new String[]{});
                    break;
                case "3":
                case "lookup":
                case "reference":
                    logger.info("🚀 Launching Lookup Demonstrations...");
                    LookupRunner.main(new String[]{});
                    break;
                case "4":
                case "evaluation":
                case "evaluate":
                case "rules":
                    logger.info("🚀 Launching Evaluation Demonstrations...");
                    EvaluationRunner.main(new String[]{});
                    break;
                case "5":
                case "infrastructure":
                case "infra":
                case "setup":
                    logger.info("🚀 Launching Infrastructure Demonstrations...");
                    InfrastructureRunner.main(new String[]{});
                    break;
                case "6":
                case "quick":
                case "tour":
                case "sample":
                    runQuickTour();
                    break;
                case "7":
                case "complete":
                case "all":
                case "everything":
                    runCompleteSuite();
                    break;
                default:
                    System.out.println("❌ Invalid choice: " + choice);
                    System.out.println("Please try again or type 'q' to quit.");
                    break;
            }
        } catch (Exception e) {
            logger.error("❌ Error executing demo category: {}", e.getMessage(), e);
        }
        
        System.out.println();
    }
    
    /**
     * Run a quick tour with sample demos from each category.
     */
    private void runQuickTour() {
        logger.info("🎯 APEX Quick Tour - Sample Demonstrations");
        logger.info("═".repeat(50));
        logger.info("");
        logger.info("Running representative demos from each APEX operation category...");
        logger.info("");
        
        long totalStartTime = System.currentTimeMillis();
        
        try {
            // Quick validation demo
            logger.info("✅ VALIDATION: Running Quick Start Demo...");
            ValidationRunner.main(new String[]{"quickstart"});
            
            // Quick enrichment demo
            logger.info("🔧 ENRICHMENT: Running YAML Dataset Demo...");
            EnrichmentRunner.main(new String[]{"yaml"});
            
            // Quick lookup demo
            logger.info("🔍 LOOKUP: Running Simple Field Lookup Demo...");
            LookupRunner.main(new String[]{"simple"});
            
            // Quick evaluation demo
            logger.info("⚡ EVALUATION: Running Simplified API Demo...");
            EvaluationRunner.main(new String[]{"simplified"});
            
            // Quick infrastructure demo
            logger.info("🏗️ INFRASTRUCTURE: Running Data Service Manager Demo...");
            InfrastructureRunner.main(new String[]{"dataservice"});
            
        } catch (Exception e) {
            logger.warn("⚠ Quick tour encountered issues: {}", e.getMessage());
            logger.info("📝 This may be expected if dependencies are not available");
        }
        
        long totalEndTime = System.currentTimeMillis();
        long totalTime = totalEndTime - totalStartTime;
        
        logger.info("");
        logger.info("🎉 APEX Quick Tour completed!");
        logger.info("Total execution time: {} ms ({} seconds)", totalTime, totalTime / 1000.0);
        logger.info("");
        logger.info("🎯 You've experienced a sample of each APEX operation category!");
        logger.info("💡 Try individual category runners for comprehensive demonstrations.");
    }
    
    /**
     * Run the complete demonstration suite.
     */
    private void runCompleteSuite() {
        logger.info("🌟 APEX Complete Demonstration Suite");
        logger.info("═".repeat(50));
        logger.info("");
        logger.info("Running ALL APEX demonstrations across all categories...");
        logger.info("⚠️  This will take significant time - please be patient!");
        logger.info("");
        
        long totalStartTime = System.currentTimeMillis();
        
        try {
            // Run all category suites
            logger.info("✅ Running ALL Validation Demonstrations...");
            ValidationRunner.main(new String[]{"all"});
            
            logger.info("🔧 Running ALL Enrichment Demonstrations...");
            EnrichmentRunner.main(new String[]{"all"});
            
            logger.info("🔍 Running ALL Lookup Demonstrations...");
            LookupRunner.main(new String[]{"all"});
            
            logger.info("⚡ Running ALL Evaluation Demonstrations...");
            EvaluationRunner.main(new String[]{"all"});
            
            logger.info("🏗️ Running ALL Infrastructure Demonstrations...");
            InfrastructureRunner.main(new String[]{"all"});
            
        } catch (Exception e) {
            logger.warn("⚠ Complete suite encountered issues: {}", e.getMessage());
            logger.info("📝 This may be expected if dependencies are not available");
        }
        
        long totalEndTime = System.currentTimeMillis();
        long totalTime = totalEndTime - totalStartTime;
        
        logger.info("");
        logger.info("🎉 APEX Complete Demonstration Suite finished!");
        logger.info("Total execution time: {} ms ({} seconds)", totalTime, totalTime / 1000.0);
        logger.info("");
        logger.info("🌟 Congratulations! You've experienced the full power of APEX!");
        logger.info("🎯 You've seen validation, enrichment, lookup, evaluation, and infrastructure capabilities.");
        logger.info("💡 APEX enables you to implement complex business logic through simple YAML configurations.");
    }
}
