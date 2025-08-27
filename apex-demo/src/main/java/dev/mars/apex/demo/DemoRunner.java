package dev.mars.apex.demo;

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

import dev.mars.apex.demo.examples.*;

import java.util.Scanner;

/**
 * DemoRunner - Simple entry point for all SpEL Rules Engine demonstrations.
 *
 * This class provides a unified entry point to run all demonstrations or specific demos
 * based on command line arguments. It supports both interactive and non-interactive modes.
 *
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-07-28
 * @version 1.0
 */
public class DemoRunner {

    /**
     * Interface for demo classes that can be run by the DemoRunner.
     * Implementing this interface allows demos to be executed in a consistent manner.
     */
    public interface Demo {
        /**
         * Run the demonstration.
         * This method should contain all the logic for the demo execution.
         */
        void run();
    }

    public static void main(String[] args) {
        DemoRunner runner = new DemoRunner();

        if (args.length == 0) {
            runner.runInteractiveMode();
        } else {
            runner.runNonInteractiveMode(args[0]);
        }
    }

    private void runInteractiveMode() {
        displayBanner();

        Scanner scanner = new Scanner(System.in);

        while (true) {
            displayMenu();
            System.out.print("Enter your choice (1-7, or 'q' to quit): ");
            String choice = scanner.nextLine().trim().toLowerCase();

            if (choice.equals("q") || choice.equals("quit") || choice.equals("exit")) {
                System.out.println("\nThank you for exploring the SpEL Rules Engine!");
                System.out.println("Visit our documentation for more advanced features.");
                break;
            }

            switch (choice) {
                case "1":
                    runQuickStartDemo();
                    break;
                case "2":
                    runLayeredAPIDemo();
                    break;
                case "3":
                    runYamlDatasetDemo();
                    break;
                case "4":
                    runFinancialServicesDemo();
                    break;
                case "5":
                    runPerformanceDemo();
                    break;
                case "6":
                    runAllDemos();
                    break;
                case "7":
                    displayAbout();
                    break;
                default:
                    System.out.println("Invalid choice. Please try again.");
                    System.out.println("Press Enter to continue...");
                    scanner.nextLine();
                    break;
            }
        }

        scanner.close();
    }

    private void runNonInteractiveMode(String command) {
        System.out.println("Running in non-interactive mode: " + command);
        System.out.println();

        switch (command.toLowerCase()) {
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
                displayHelp();
                break;
            case "all":
                runAllDemos();
                break;
            default:
                System.out.println("Unknown command: " + command);
                System.out.println();
                displayHelp();
                break;
        }
    }

    private void displayBanner() {
        if (isTestEnvironment()) {
            System.out.println("=== SpEL Rules Engine - Demo Suite (TEST MODE) ===");
            System.out.println("Comprehensive demonstrations of APEX");
        } else {
            System.out.println("╔══════════════════════════════════════════════════════════════════════════════╗");
            System.out.println("║                        SpEL Rules Engine - Demo Suite                       ║");
            System.out.println("║                     Comprehensive demonstrations of APEX                    ║");
            System.out.println("╚══════════════════════════════════════════════════════════════════════════════╝");
        }
        System.out.println();
    }

    private void displayMenu() {
        System.out.println("Available Demonstrations:");
        System.out.println("  1. QuickStart Demo (5 Minutes) - Perfect introduction");
        System.out.println("  2. Layered API Demo - Three-Layer API Design");
        System.out.println("  3. YAML Dataset Demo - Revolutionary dataset enrichment");
        System.out.println("  4. Financial Services - Real-world financial examples");
        System.out.println("  5. Performance Demo - Enterprise Performance Monitoring");
        System.out.println("  6. Run All Demos - Execute complete demonstration suite");
        System.out.println("  7. About - Learn about SpEL Rules Engine");
        System.out.println();
    }

    private void displayAbout() {
        System.out.println("About SpEL Rules Engine");
        System.out.println("=======================");
        System.out.println();
        System.out.println("Key Features:");
        System.out.println("• Three-Layer API Design - From simple one-liners to enterprise configuration");
        System.out.println("• YAML Dataset Enrichment - Revolutionary approach to data-driven rules");
        System.out.println("• Enterprise Performance Monitoring - Production-ready performance tracking");
        System.out.println("• Spring Expression Language - Powerful, flexible expression evaluation");
        System.out.println("• Comprehensive Error Handling - Robust error recovery and reporting");
        System.out.println();
    }

    private void displayHelp() {
        System.out.println("Command Line Help");
        System.out.println("=================");
        System.out.println();
        System.out.println("Usage:");
        System.out.println("  java DemoRunner                    # Interactive mode");
        System.out.println("  java DemoRunner <command>          # Run specific demo");
        System.out.println();
        System.out.println("Commands:");
        System.out.println("  quickstart, quick    - Quick Start (5 Minutes)");
        System.out.println("  layered, api         - Three-Layer API Design");
        System.out.println("  dataset, yaml        - YAML Dataset Enrichment");
        System.out.println("  financial, finance   - Financial Services Examples");
        System.out.println("  performance, perf    - Performance Monitoring");
        System.out.println("  all                  - Run all demonstrations");
        System.out.println("  help                 - Show this help");
        System.out.println();
        System.out.println("Examples:");
        System.out.println("  java DemoRunner quickstart");
        System.out.println("  java DemoRunner financial");
        System.out.println();
    }

    private void runQuickStartDemo() {
        try {
            if (isTestEnvironment()) {
                System.out.println("Quick Start (5 Minutes) - TEST MODE");
            } else {
                System.out.println("Quick Start (5 Minutes)");
                System.out.println("=======================");
            }
            new QuickStartDemoB().run();
        } catch (Exception e) {
            System.out.println("Error running QuickStart demo: " + e.getMessage());
        }
    }

    private void runLayeredAPIDemo() {
        try {
            System.out.println("Three-Layer API Design");
            System.out.println("======================");
            new LayeredAPIDemo().run();
        } catch (Exception e) {
            System.out.println("Error running Layered API demo: " + e.getMessage());
        }
    }

    private void runYamlDatasetDemo() {
        try {
            System.out.println("YAML Dataset Demo");
            System.out.println("=================");
            new YamlDatasetDemo().run();
        } catch (Exception e) {
            System.out.println("Error running YAML Dataset demo: " + e.getMessage());
        }
    }

    private void runFinancialServicesDemo() {
        try {
            System.out.println("Financial Services Demo");
            System.out.println("=======================");
            new FinancialServicesDemo().run();
        } catch (Exception e) {
            System.out.println("Error running Financial Services demo: " + e.getMessage());
        }
    }

    private void runPerformanceDemo() {
        try {
            System.out.println("Performance Monitoring Demo");
            System.out.println("===========================");
            new PerformanceDemo().run();
        } catch (Exception e) {
            System.out.println("Error running Performance demo: " + e.getMessage());
        }
    }

    private void runAllDemos() {
        System.out.println("Running All Demonstrations");
        System.out.println("==========================");
        System.out.println();

        runQuickStartDemo();
        System.out.println();

        runLayeredAPIDemo();
        System.out.println();

        runYamlDatasetDemo();
        System.out.println();

        runFinancialServicesDemo();
        System.out.println();

        runPerformanceDemo();
        System.out.println();

        System.out.println("All demonstrations completed!");
    }

    /**
     * Check if we're running in a test environment.
     */
    private boolean isTestEnvironment() {
        // Check for test environment property
        String testEnv = System.getProperty("test.environment");
        if ("true".equals(testEnv)) {
            return true;
        }

        // Also check for test class in stack trace
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        for (StackTraceElement element : stackTrace) {
            String className = element.getClassName();
            if (className.contains("Test") || className.contains("test")) {
                return true;
            }
        }

        return false;
    }
}
