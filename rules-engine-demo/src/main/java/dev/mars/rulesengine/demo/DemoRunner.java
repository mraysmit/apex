package dev.mars.rulesengine.demo;

import java.util.Arrays;
import java.util.Scanner;

/**
 * DemoRunner - Simple entry point for all SpEL Rules Engine demonstrations.
 * 
 * This class provides a unified entry point to run all demonstrations or specific demos
 * based on command line arguments. It supports both interactive and non-interactive modes.
 * 
 * Usage:
 * - java DemoRunner                    # Interactive mode - shows menu
 * - java DemoRunner all                # Run all demos sequentially
 * - java DemoRunner quickstart         # Run QuickStart demo only
 * - java DemoRunner layered            # Run LayeredAPI demo only
 * - java DemoRunner dataset            # Run YamlDataset demo only
 * - java DemoRunner financial          # Run FinancialServices demo only
 * - java DemoRunner performance        # Run Performance demo only
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-07-27
 * @version 1.0
 */
public class DemoRunner {
    
    private static final String BANNER = 
        "╔══════════════════════════════════════════════════════════════╗\n" +
        "║                SpEL Rules Engine - Demo Suite               ║\n" +
        "║                                                              ║\n" +
        "║  Comprehensive demonstrations of rules engine capabilities   ║\n" +
        "║  From simple validations to enterprise-grade solutions      ║\n" +
        "╚══════════════════════════════════════════════════════════════╝";
    
    /**
     * Main entry point for the demo suite.
     */
    public static void main(String[] args) {
        System.out.println(BANNER);
        System.out.println();
        
        if (args.length == 0) {
            runInteractiveMode();
        } else {
            runNonInteractiveMode(args);
        }
    }
    
    /**
     * Run in interactive mode with a menu system.
     */
    private static void runInteractiveMode() {
        Scanner scanner = new Scanner(System.in);
        
        while (true) {
            showMenu();
            System.out.print("Enter your choice (1-7, or 'q' to quit): ");
            String choice = scanner.nextLine().trim().toLowerCase();
            
            System.out.println();
            
            switch (choice) {
                case "1":
                case "quickstart":
                    runQuickStartDemo();
                    break;
                case "2":
                case "layered":
                    runLayeredAPIDemo();
                    break;
                case "3":
                case "dataset":
                    runYamlDatasetDemo();
                    break;
                case "4":
                case "financial":
                    runFinancialServicesDemo();
                    break;
                case "5":
                case "performance":
                    runPerformanceDemo();
                    break;
                case "6":
                case "all":
                    runAllDemos();
                    break;
                case "7":
                case "about":
                    showAbout();
                    break;
                case "q":
                case "quit":
                case "exit":
                    System.out.println("👋 Thank you for exploring the SpEL Rules Engine!");
                    System.out.println("   Visit our documentation for more information.");
                    return;
                default:
                    System.out.println("❌ Invalid choice. Please try again.");
            }
            
            System.out.println();
            System.out.println("Press Enter to continue...");
            scanner.nextLine();
            System.out.println();
        }
    }
    
    /**
     * Run in non-interactive mode based on command line arguments.
     */
    private static void runNonInteractiveMode(String[] args) {
        String command = args[0].toLowerCase();
        
        System.out.println("🚀 Running in non-interactive mode: " + command);
        System.out.println();
        
        switch (command) {
            case "all":
                runAllDemos();
                break;
            case "quickstart":
            case "quick":
                runQuickStartDemo();
                break;
            case "layered":
            case "api":
                runLayeredAPIDemo();
                break;
            case "dataset":
            case "yaml":
                runYamlDatasetDemo();
                break;
            case "financial":
            case "finance":
                runFinancialServicesDemo();
                break;
            case "performance":
            case "perf":
                runPerformanceDemo();
                break;
            case "help":
            case "--help":
            case "-h":
                showHelp();
                break;
            default:
                System.out.println("❌ Unknown command: " + command);
                System.out.println();
                showHelp();
                return; // Exit gracefully instead of System.exit(1) for better testability
        }
    }
    
    /**
     * Display the interactive menu.
     */
    private static void showMenu() {
        System.out.println("📋 Available Demonstrations:");
        System.out.println();
        System.out.println("  1. QuickStart Demo        - 5-minute introduction to core concepts");
        System.out.println("  2. Layered API Demo       - Three-layer API design philosophy");
        System.out.println("  3. YAML Dataset Demo      - Revolutionary dataset enrichment");
        System.out.println("  4. Financial Services     - OTC derivatives and trading rules");
        System.out.println("  5. Performance Demo       - Monitoring and optimization");
        System.out.println("  6. Run All Demos          - Complete demonstration suite");
        System.out.println("  7. About                   - Project information and features");
        System.out.println();
    }
    
    /**
     * Run all demonstrations sequentially.
     */
    private static void runAllDemos() {
        System.out.println("🔄 Running Complete Demo Suite");
        System.out.println("=" .repeat(60));
        System.out.println("This will run all demonstrations in sequence.");
        System.out.println("Estimated time: 10-15 minutes");
        System.out.println();
        
        try {
            // Run demos in logical order
            runQuickStartDemo();
            waitBetweenDemos();
            
            runLayeredAPIDemo();
            waitBetweenDemos();
            
            runYamlDatasetDemo();
            waitBetweenDemos();
            
            runFinancialServicesDemo();
            waitBetweenDemos();
            
            runPerformanceDemo();
            
            System.out.println();
            System.out.println("🎉 COMPLETE DEMO SUITE FINISHED!");
            System.out.println("=" .repeat(60));
            System.out.println("You've seen the full power of the SpEL Rules Engine:");
            System.out.println("  ✅ Quick Start - Core concepts and basic usage");
            System.out.println("  ✅ Layered API - Progressive complexity design");
            System.out.println("  ✅ YAML Datasets - Revolutionary data enrichment");
            System.out.println("  ✅ Financial Services - Production-ready trading rules");
            System.out.println("  ✅ Performance - Enterprise monitoring and optimization");
            System.out.println();
            System.out.println("🚀 Ready to integrate into your applications!");
            
        } catch (Exception e) {
            System.out.println("❌ Error during demo execution: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Run the QuickStart demonstration.
     */
    private static void runQuickStartDemo() {
        try {
            new QuickStartDemo().run();
        } catch (Exception e) {
            System.out.println("❌ Error in QuickStart demo: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Run the Layered API demonstration.
     */
    private static void runLayeredAPIDemo() {
        try {
            new LayeredAPIDemo().run();
        } catch (Exception e) {
            System.out.println("❌ Error in Layered API demo: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Run the YAML Dataset demonstration.
     */
    private static void runYamlDatasetDemo() {
        try {
            new YamlDatasetDemo().run();
        } catch (Exception e) {
            System.out.println("❌ Error in YAML Dataset demo: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Run the Financial Services demonstration.
     */
    private static void runFinancialServicesDemo() {
        try {
            new FinancialServicesDemo().run();
        } catch (Exception e) {
            System.out.println("❌ Error in Financial Services demo: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Run the Performance demonstration.
     */
    private static void runPerformanceDemo() {
        try {
            new PerformanceDemo().run();
        } catch (Exception e) {
            System.out.println("❌ Error in Performance demo: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Show project information and features.
     */
    private static void showAbout() {
        System.out.println("📖 About SpEL Rules Engine");
        System.out.println("=" .repeat(40));
        System.out.println();
        System.out.println("🌟 Key Features:");
        System.out.println("  • Three-Layer API Design (Simple → Structured → Advanced)");
        System.out.println("  • YAML Dataset Enrichment (Revolutionary!)");
        System.out.println("  • Enterprise Performance Monitoring");
        System.out.println("  • Financial Services Ready");
        System.out.println("  • 100% Backward Compatible");
        System.out.println("  • Production-Grade Reliability");
        System.out.println();
        System.out.println("🎯 Perfect For:");
        System.out.println("  • Business Rule Management");
        System.out.println("  • Data Validation and Enrichment");
        System.out.println("  • Financial Trading Systems");
        System.out.println("  • Regulatory Compliance");
        System.out.println("  • Real-time Decision Making");
        System.out.println();
        System.out.println("📚 Documentation:");
        System.out.println("  • Complete User Guide: docs/COMPLETE_USER_GUIDE.md");
        System.out.println("  • Technical Reference: docs/TECHNICAL_IMPLEMENTATION_GUIDE.md");
        System.out.println("  • YAML Dataset Guide: docs/YAML-Dataset-Enrichment-Guide.md");
        System.out.println("  • Financial Services: docs/FINANCIAL_SERVICES_GUIDE.md");
        System.out.println();
        System.out.println("🏢 Enterprise Ready:");
        System.out.println("  • High Performance (< 1% monitoring overhead)");
        System.out.println("  • Comprehensive Error Handling");
        System.out.println("  • Full Audit Trail Support");
        System.out.println("  • REST API with OpenAPI/Swagger");
    }
    
    /**
     * Show command line help.
     */
    private static void showHelp() {
        System.out.println("📖 SpEL Rules Engine Demo Suite - Command Line Help");
        System.out.println("=" .repeat(60));
        System.out.println();
        System.out.println("Usage:");
        System.out.println("  java DemoRunner [command]");
        System.out.println();
        System.out.println("Commands:");
        System.out.println("  (no args)     Run in interactive mode with menu");
        System.out.println("  all           Run all demonstrations sequentially");
        System.out.println("  quickstart    Run QuickStart demo (5-minute intro)");
        System.out.println("  layered       Run Layered API demo (API design)");
        System.out.println("  dataset       Run YAML Dataset demo (data enrichment)");
        System.out.println("  financial     Run Financial Services demo (trading)");
        System.out.println("  performance   Run Performance demo (monitoring)");
        System.out.println("  help          Show this help message");
        System.out.println();
        System.out.println("Examples:");
        System.out.println("  java DemoRunner                    # Interactive mode");
        System.out.println("  java DemoRunner all                # Run all demos");
        System.out.println("  java DemoRunner quickstart         # Quick introduction");
        System.out.println("  java DemoRunner financial          # Financial services");
    }
    
    /**
     * Wait between demos when running all demos.
     */
    private static void waitBetweenDemos() {
        System.out.println();
        System.out.println("⏸️  Pausing between demos...");
        try {
            Thread.sleep(2000); // 2 second pause
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        System.out.println();
    }
}
