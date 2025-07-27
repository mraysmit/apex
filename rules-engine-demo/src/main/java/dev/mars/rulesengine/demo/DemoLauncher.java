package dev.mars.rulesengine.demo;

import dev.mars.rulesengine.demo.examples.BasicUsageExamples;
import dev.mars.rulesengine.demo.examples.LayeredAPIDemo;
import dev.mars.rulesengine.demo.examples.PerformanceMonitoringDemo;
import dev.mars.rulesengine.demo.examples.financial.CommoditySwapValidationDemo;
import dev.mars.rulesengine.demo.framework.Demo;
import dev.mars.rulesengine.demo.framework.DemoFramework;
import dev.mars.rulesengine.demo.showcase.PerformanceAndExceptionShowcase;
import dev.mars.rulesengine.demo.simplified.SimplifiedAPIDemo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Scanner;

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

/**
 * Unified demo launcher for the rationalized SpEL Rules Engine Demo Suite.
 *
 * This class is part of the PeeGeeQ message queue system, providing
 * production-ready PostgreSQL-based message queuing capabilities.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-07-27
 * @version 1.0
 */
public class DemoLauncher {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(DemoLauncher.class);
    
    private final DemoFramework framework;
    
    public DemoLauncher() {
        this.framework = new DemoFramework();
        registerAllDemos();
    }
    
    /**
     * Main entry point for the demo launcher.
     *
     * @param args Command line arguments:
     *             - No args: Interactive mode with numeric menu
     *             - [1-6]: Run specific demo by number
     */
    public static void main(String[] args) {
        LOGGER.info("Starting SpEL Rules Engine Demo Suite");

        DemoLauncher launcher = new DemoLauncher();

        try {
            if (args.length == 0) {
                // Interactive mode
                launcher.runInteractiveMode();
            } else {
                // Non-interactive mode - only numeric options
                String command = args[0];
                if (isNumeric(command)) {
                    launcher.runNumericOption(Integer.parseInt(command));
                } else {
                    System.err.println("❌ Invalid option: " + command);
                    System.err.println("💡 Valid options are 1-6. Use no arguments for interactive mode.");
                    launcher.printUsage();
                    System.exit(1);
                }
            }
        } catch (Exception e) {
            LOGGER.error("Error running demo launcher", e);
            System.err.println("❌ Error: " + e.getMessage());
            System.exit(1);
        }

        LOGGER.info("Demo suite completed");
    }
    
    /**
     * Register all available demos with the framework.
     */
    private void registerAllDemos() {
        LOGGER.info("Registering demos with framework");
        
        // Register consolidated demos
        framework.registerDemo(new BasicUsageExamples());
        framework.registerDemo(new LayeredAPIDemo());
        framework.registerDemo(new PerformanceMonitoringDemo());
        
        // Register existing financial demo (wrapped)
        framework.registerDemo(new FinancialDemoWrapper());
        
        // Register existing showcase demos (wrapped)
        framework.registerDemo(new PerformanceShowcaseWrapper());
        framework.registerDemo(new SimplifiedAPIDemoWrapper());
        
        LOGGER.info("Registered {} demos", framework.getAvailableDemos().size());
    }
    
    /**
     * Run the demo launcher in interactive mode.
     */
    private void runInteractiveMode() {
        LOGGER.info("Running in interactive mode");

        Scanner scanner = new Scanner(System.in);

        while (true) {
            printSimpleMenu();
            System.out.print("Enter your choice (1-6, or 0 to exit): ");

            String input = scanner.nextLine().trim();

            if (input.isEmpty()) {
                continue;
            }

            if (input.equals("0")) {
                System.out.println("👋 Goodbye!");
                break;
            }

            try {
                if (isNumeric(input)) {
                    int choice = Integer.parseInt(input);
                    if (choice >= 1 && choice <= 6) {
                        runNumericOption(choice);
                    } else {
                        System.err.println("❌ Invalid option: " + choice);
                        System.err.println("💡 Valid options are 1-6, or 0 to exit.");
                    }
                } else {
                    System.err.println("❌ Invalid input: " + input);
                    System.err.println("💡 Please enter a number 1-6, or 0 to exit.");
                }
            } catch (Exception e) {
                LOGGER.error("Error handling user input: {}", input, e);
                System.err.println("❌ Error: " + e.getMessage());
            }

            System.out.println("\n" + "=".repeat(80) + "\n");
        }

        scanner.close();
    }
    


    /**
     * Check if a string represents a numeric value.
     */
    private static boolean isNumeric(String str) {
        try {
            Integer.parseInt(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * Run demo based on numeric option (compatible with ComprehensiveRulesEngineDemo).
     *
     * @param option The numeric option (1-6)
     */
    private void runNumericOption(int option) {
        System.out.println("Running demo option " + option + "...\n");

        switch (option) {
            case 1 -> {
                // Financial Instrument Validation (Commodity Swaps)
                System.out.println("🏦 Financial Instrument Validation (Commodity Swaps)\n");
                runDemoSafely("Financial Instrument Validation");
            }
            case 2 -> {
                // Simplified APIs Demonstration
                System.out.println("🚀 Simplified APIs Demonstration\n");
                runDemoSafely("Simplified API Demo (Legacy)");
            }
            case 3 -> {
                // Performance & Exception Handling Showcase
                System.out.println("⚡ Performance & Exception Handling Showcase\n");
                runDemoSafely("Performance & Exception Showcase");
            }
            case 4 -> {
                // Complete End-to-End Demo (run all)
                System.out.println("🔄 Complete End-to-End Demo\n");
                framework.runAllDemos();
            }
            case 5 -> {
                // Basic Usage Examples (Static Data Validation)
                System.out.println("📊 Basic Usage Examples\n");
                runDemoSafely("Basic Usage Examples");
            }
            case 6 -> {
                // Layered API Demo (Quick Start Guide)
                System.out.println("🎯 Layered API Demo (Quick Start Guide)\n");
                runDemoSafely("Layered API Demonstration");
            }
            default -> {
                System.err.println("❌ Invalid option: " + option);
                System.err.println("💡 Valid options are 1-6. Use 'help' for more information.");
                printUsage();
                System.exit(1);
            }
        }
    }

    /**
     * Helper method to run a demo safely with error handling.
     */
    private void runDemoSafely(String demoName) {
        Demo demo = framework.findDemoByName(demoName);
        if (demo != null) {
            framework.runDemo(demoName);
        } else {
            System.err.println("❌ Demo not found: '" + demoName + "'");
            System.err.println("💡 Available demos:");
            framework.getAvailableDemos().forEach(d ->
                System.err.println("   - '" + d.getName() + "'"));
        }
    }

    /**
     * Print simple menu for interactive mode.
     */
    private void printSimpleMenu() {
        System.out.println("""

            ╔══════════════════════════════════════════════════════════════════════════════╗
            ║                        SpEL Rules Engine Demo Suite                          ║
            ╚══════════════════════════════════════════════════════════════════════════════╝

            Please select a demo to run:

            1. Financial Instrument Validation (Commodity Swaps)
            2. Simplified APIs Demonstration
            3. Performance & Exception Handling Showcase
            4. Complete End-to-End Demo (run all)
            5. Basic Usage Examples
            6. Layered API Demo (Quick Start Guide)

            0. Exit
            """);
    }

    /**
     * Print usage information.
     */
    private void printUsage() {
        System.out.println("""

            ╔══════════════════════════════════════════════════════════════════════════════╗
            ║                        SpEL Rules Engine Demo Suite                          ║
            ║                           Usage Information                                  ║
            ╚══════════════════════════════════════════════════════════════════════════════╝

            Usage: java DemoLauncher [option]

            Options:
              (no args)     Run in interactive mode with numeric menu
              1             Financial Instrument Validation (Commodity Swaps)
              2             Simplified APIs Demonstration
              3             Performance & Exception Handling Showcase
              4             Complete End-to-End Demo (run all)
              5             Basic Usage Examples
              6             Layered API Demo (Quick Start Guide)

            Examples:
              java DemoLauncher                    # Interactive mode
              java DemoLauncher 1                  # Financial demo
              java DemoLauncher 4                  # Run all demos
              java DemoLauncher 5                  # Basic usage examples

            """);
    }
    
    // Wrapper classes for existing demos to integrate with new framework
    
    /**
     * Wrapper for the existing CommoditySwapValidationDemo.
     */
    private static class FinancialDemoWrapper implements Demo {
        private final CommoditySwapValidationDemo delegate = new CommoditySwapValidationDemo();
        
        @Override
        public String getName() {
            return "Financial Instrument Validation";
        }
        
        @Override
        public String getDescription() {
            return "OTC Commodity Total Return Swap validation and enrichment with static data integration";
        }
        
        @Override
        public dev.mars.rulesengine.demo.framework.DemoCategory getCategory() {
            return dev.mars.rulesengine.demo.framework.DemoCategory.FINANCIAL_EXAMPLES;
        }
        
        @Override
        public int getEstimatedRuntimeSeconds() {
            return 45;
        }
        
        @Override
        public void run() {
            runNonInteractive();
        }
        
        @Override
        public void runNonInteractive() {
            // Run the existing demo's main method
            CommoditySwapValidationDemo.main(new String[]{});
        }
    }
    
    /**
     * Wrapper for the existing PerformanceAndExceptionShowcase.
     */
    private static class PerformanceShowcaseWrapper implements Demo {
        @Override
        public String getName() {
            return "Performance & Exception Showcase";
        }
        
        @Override
        public String getDescription() {
            return "Advanced performance monitoring and exception handling features";
        }
        
        @Override
        public dev.mars.rulesengine.demo.framework.DemoCategory getCategory() {
            return dev.mars.rulesengine.demo.framework.DemoCategory.PERFORMANCE_MONITORING;
        }
        
        @Override
        public int getEstimatedRuntimeSeconds() {
            return 60;
        }
        
        @Override
        public void run() {
            runNonInteractive();
        }
        
        @Override
        public void runNonInteractive() {
            // Run the existing demo's main method
            PerformanceAndExceptionShowcase.main(new String[]{});
        }
    }
    
    /**
     * Wrapper for the existing SimplifiedAPIDemo.
     */
    private static class SimplifiedAPIDemoWrapper implements Demo {
        @Override
        public String getName() {
            return "Simplified API Demo (Legacy)";
        }
        
        @Override
        public String getDescription() {
            return "Legacy simplified API demonstration (use 'Layered API Demonstration' for improved version)";
        }
        
        @Override
        public dev.mars.rulesengine.demo.framework.DemoCategory getCategory() {
            return dev.mars.rulesengine.demo.framework.DemoCategory.API_DEMONSTRATIONS;
        }
        
        @Override
        public int getEstimatedRuntimeSeconds() {
            return 30;
        }
        
        @Override
        public void run() {
            runNonInteractive();
        }
        
        @Override
        public void runNonInteractive() {
            // Run the existing demo's main method
            SimplifiedAPIDemo.main(new String[]{});
        }
    }
}
