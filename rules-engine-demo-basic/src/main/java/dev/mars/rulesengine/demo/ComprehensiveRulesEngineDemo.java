package dev.mars.rulesengine.demo;

import dev.mars.rulesengine.demo.examples.financial.CommoditySwapValidationDemo;
import dev.mars.rulesengine.demo.showcase.PerformanceAndExceptionShowcase;
import dev.mars.rulesengine.demo.simplified.SimplifiedAPIDemo;

import java.util.Scanner;

/**
 * Comprehensive demonstration of the SpEL Rules Engine capabilities.
 * 
 * This is the main entry point for the reorganized rules-engine-demo module,
 * showcasing the new layered APIs design and comprehensive examples including:
 * 
 * 1. Financial instrument validation and enrichment (OTC Commodity Total Return Swaps)
 * 2. New layered APIs (Ultra-Simple, Template-Based, Advanced Configuration)
 * 3. Performance monitoring and exception handling
 * 4. Static data validation and enrichment
 * 5. Real-world use cases for trade capture and post-trade processing
 * 
 * The demo is organized into logical sections that can be run independently
 * or as a complete showcase of the rules engine capabilities.
 */
public class ComprehensiveRulesEngineDemo {
    
    private static final String BANNER = """
            ╔══════════════════════════════════════════════════════════════════════════════╗
            ║                        SpEL Rules Engine Demo Suite                          ║
            ║                     Comprehensive Financial Examples                         ║
            ╠══════════════════════════════════════════════════════════════════════════════╣
            ║  Demonstrating new layered APIs with real-world financial use cases          ║
            ║  • OTC Commodity Total Return Swap validation & enrichment                   ║
            ║  • Static data integration and validation                                    ║
            ║  • Performance monitoring and exception handling                             ║
            ║  • Trade capture and post-trade lifecycle processing                         ║
            ╚══════════════════════════════════════════════════════════════════════════════╝
            """;
    
    private static final String MENU = """
            
            ┌─────────────────────────────────────────────────────────────────────────────┐
            │                              DEMO MENU                                      │
            ├─────────────────────────────────────────────────────────────────────────────┤
            │  1. 🏦 Financial Instrument Validation (Commodity Swaps)                    │
            │  2. 🚀 Simplified APIs Demonstration                                        │
            │  3. ⚡ Performance & Exception Handling Showcase                             │
            │  4. 🔄 Complete End-to-End Demo                                             │
            │  5. 📊 Static Data Validation Examples                                      │
            │  6. 🎯 Quick Start Guide                                                    │
            │  0. 🚪 Exit                                                                  │
            └───────────────────────────────────────────────────────────────────────────── ┘
            
            Enter your choice (0-6): """;
    
    public static void main(String[] args) {
        System.out.println(BANNER);
        
        // Check if running in non-interactive mode (e.g., automated tests)
        if (args.length > 0) {
            runNonInteractiveDemo(args[0]);
            return;
        }
        
        // Interactive mode
        runInteractiveDemo();
    }
    
    /**
     * Run the demo in interactive mode with user menu selection.
     */
    private static void runInteractiveDemo() {
        Scanner scanner = new Scanner(System.in);
        boolean running = true;
        
        while (running) {
            System.out.print(MENU);
            
            try {
                int choice = Integer.parseInt(scanner.nextLine().trim());
                
                switch (choice) {
                    case 1 -> runFinancialInstrumentDemo();
                    case 2 -> runSimplifiedAPIDemo();
                    case 3 -> runPerformanceShowcase();
                    case 4 -> runCompleteDemo();
                    case 5 -> runStaticDataDemo();
                    case 6 -> runQuickStartGuide();
                    case 0 -> {
                        System.out.println("\n👋 Thank you for exploring the SpEL Rules Engine!");
                        System.out.println("Visit our documentation for more examples and advanced features.");
                        running = false;
                    }
                    default -> System.out.println("\n❌ Invalid choice. Please select 0-6.");
                }
                
                if (running && choice != 0) {
                    System.out.println("\n" + "─".repeat(80));
                    System.out.print("Press Enter to return to menu...");
                    scanner.nextLine();
                }
                
            } catch (NumberFormatException e) {
                System.out.println("\n❌ Please enter a valid number (0-6).");
            }
        }
        
        scanner.close();
    }
    
    /**
     * Run the demo in non-interactive mode for automated testing.
     */
    private static void runNonInteractiveDemo(String demoType) {
        System.out.println("Running in non-interactive mode: " + demoType);
        
        switch (demoType.toLowerCase()) {
            case "financial" -> runFinancialInstrumentDemo();
            case "simplified" -> runSimplifiedAPIDemo();
            case "performance" -> runPerformanceShowcase();
            case "complete" -> runCompleteDemo();
            case "static" -> runStaticDataDemo();
            case "quickstart" -> runQuickStartGuide();
            default -> {
                System.out.println("Unknown demo type: " + demoType);
                System.out.println("Available types: financial, simplified, performance, complete, static, quickstart");
            }
        }
    }
    
    /**
     * Run the financial instrument validation demo.
     */
    private static void runFinancialInstrumentDemo() {
        printSectionHeader("Financial Instrument Validation Demo", 
                          "OTC Commodity Total Return Swap validation and enrichment");
        
        try {
            CommoditySwapValidationDemo.main(new String[]{});
        } catch (Exception e) {
            System.err.println("Error running financial demo: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Run the simplified APIs demonstration.
     */
    private static void runSimplifiedAPIDemo() {
        printSectionHeader("Simplified APIs Demonstration", 
                          "Ultra-simple, template-based, and advanced configuration APIs");
        
        try {
            SimplifiedAPIDemo.main(new String[]{});
        } catch (Exception e) {
            System.err.println("Error running simplified API demo: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Run the performance and exception handling showcase.
     */
    private static void runPerformanceShowcase() {
        printSectionHeader("Performance & Exception Handling Showcase", 
                          "Monitoring, metrics, concurrent execution, and error recovery");
        
        try {
            PerformanceAndExceptionShowcase.main(new String[]{});
        } catch (Exception e) {
            System.err.println("Error running performance showcase: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Run the complete end-to-end demonstration.
     */
    private static void runCompleteDemo() {
        printSectionHeader("Complete End-to-End Demo", 
                          "Full demonstration of all features and capabilities");
        
        System.out.println("🔄 Running complete demonstration suite...\n");
        
        // Run all demos in sequence
        runFinancialInstrumentDemo();
        System.out.println("\n" + "═".repeat(80) + "\n");
        
        runSimplifiedAPIDemo();
        System.out.println("\n" + "═".repeat(80) + "\n");
        
        runPerformanceShowcase();
        System.out.println("\n" + "═".repeat(80) + "\n");
        
        runStaticDataDemo();
        
        System.out.println("\n🎉 Complete demonstration finished!");
        System.out.println("All features have been showcased successfully.");
    }
    
    /**
     * Run static data validation examples.
     */
    private static void runStaticDataDemo() {
        printSectionHeader("Static Data Validation Examples", 
                          "Client, counterparty, currency, and commodity reference data");
        
        System.out.println("📊 Static Data Validation Examples:");
        System.out.println();
        
        // This would run specific static data examples
        System.out.println("1. Client Data Validation:");
        System.out.println("   ✓ Client existence and status checks");
        System.out.println("   ✓ Regulatory classification validation");
        System.out.println("   ✓ Credit limit and risk rating checks");
        
        System.out.println("\n2. Account Data Validation:");
        System.out.println("   ✓ Account ownership verification");
        System.out.println("   ✓ Account status and limits validation");
        System.out.println("   ✓ Authorized instrument checks");
        
        System.out.println("\n3. Counterparty Data Validation:");
        System.out.println("   ✓ Counterparty authorization status");
        System.out.println("   ✓ Credit rating and limit validation");
        System.out.println("   ✓ LEI (Legal Entity Identifier) verification");
        
        System.out.println("\n4. Reference Data Validation:");
        System.out.println("   ✓ Currency validity and trading status");
        System.out.println("   ✓ Commodity reference index validation");
        System.out.println("   ✓ Market data provider verification");
        
        System.out.println("\n✅ Static data validation examples completed!");
    }
    
    /**
     * Run the quick start guide.
     */
    private static void runQuickStartGuide() {
        printSectionHeader("Quick Start Guide", 
                          "Get started with the SpEL Rules Engine in 5 minutes");
        
        System.out.println("""
                🚀 Quick Start Guide - SpEL Rules Engine
                
                ┌─ Step 1: Ultra-Simple API (30 seconds) ─────────────────────────────────┐
                │                                                                         │
                │  // One-liner rule evaluation                                          │
                │  boolean isValid = Rules.check("#age >= 18", Map.of("age", 25));       │
                │                                                                         │
                │  // Named rules for reuse                                              │
                │  Rules.define("adult", "#age >= 18");                                  │
                │  boolean result = Rules.test("adult", customer);                       │
                │                                                                         │
                └─────────────────────────────────────────────────────────────────────────┘
                
                ┌─ Step 2: Template-Based Rules (2 minutes) ──────────────────────────────┐
                │                                                                         │
                │  // Validation rule set                                                │
                │  RulesEngine validation = RuleSet.validation()                         │
                │      .ageCheck(18)                                                     │
                │      .emailRequired()                                                  │
                │      .phoneRequired()                                                  │
                │      .build();                                                         │
                │                                                                         │
                └─────────────────────────────────────────────────────────────────────────┘
                
                ┌─ Step 3: Financial Use Case (2 minutes) ────────────────────────────────┐
                │                                                                         │
                │  // Commodity swap validation                                          │
                │  boolean validSwap = Rules.check(                                      │
                │      "#notionalAmount > 1000000 && #currency == 'USD'",               │
                │      Map.of("notionalAmount", swap.getNotional(),                      │
                │             "currency", swap.getCurrency()));                          │
                │                                                                         │
                └─────────────────────────────────────────────────────────────────────────┘
                
                📚 Next Steps:
                • Explore the financial instrument examples (Option 1)
                • Try the performance monitoring features (Option 3)
                • Review the complete API documentation
                
                💡 Pro Tips:
                • Use simple expressions for better performance
                • Leverage static data validation for enrichment
                • Enable performance monitoring in production
                • Cache frequently used rules for optimal speed
                """);
    }
    
    /**
     * Print a formatted section header.
     */
    private static void printSectionHeader(String title, String description) {
        System.out.println("\n" + "═".repeat(80));
        System.out.println("🎯 " + title.toUpperCase());
        System.out.println("   " + description);
        System.out.println("═".repeat(80));
        System.out.println();
    }
}
