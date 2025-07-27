package dev.mars.rulesengine.demo;

import dev.mars.rulesengine.demo.examples.financial.CommoditySwapValidationDemo;
import dev.mars.rulesengine.demo.showcase.PerformanceAndExceptionShowcase;
import dev.mars.rulesengine.demo.simplified.SimplifiedAPIDemo;

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
 * Comprehensive Rules Engine Demo
 * 
 * This class orchestrates various demo scenarios to showcase the capabilities
 * of the rules engine framework. It supports both interactive and non-interactive modes.
 */
public class ComprehensiveRulesEngineDemo {
    
    public static void main(String[] args) {
        System.out.println("=== Comprehensive Rules Engine Demo ===");
        System.out.println();
        
        if (args.length > 0) {
            // Non-interactive mode
            String demoType = args[0].toLowerCase();
            System.out.println("Running in non-interactive mode: " + demoType);
            runDemo(demoType);
        } else {
            // Interactive mode
            runInteractiveMode();
        }
    }
    
    private static void runInteractiveMode() {
        Scanner scanner = new Scanner(System.in);
        
        while (true) {
            displayMenu();
            System.out.print("Enter your choice (1-6): ");
            
            String choice = scanner.nextLine().trim();
            
            switch (choice) {
                case "1":
                    runDemo("simplified");
                    break;
                case "2":
                    runDemo("financial");
                    break;
                case "3":
                    runDemo("performance");
                    break;
                case "4":
                    runDemo("complete");
                    break;
                case "5":
                    displayHelp();
                    break;
                case "6":
                    System.out.println("Thank you for using the Rules Engine Demo!");
                    return;
                default:
                    System.out.println("Invalid choice. Please try again.");
            }
            
            System.out.println();
            System.out.println("Press Enter to continue...");
            scanner.nextLine();
        }
    }
    
    private static void displayMenu() {
        System.out.println("\n=== Demo Menu ===");
        System.out.println("1. Simplified API Demo - Basic rules engine usage");
        System.out.println("2. Financial Domain Demo - Commodity swap validation");
        System.out.println("3. Performance & Exception Demo - Advanced features");
        System.out.println("4. Complete Demo Suite - Run all demos");
        System.out.println("5. Help - Learn more about the demos");
        System.out.println("6. Exit");
        System.out.println();
    }
    
    private static void runDemo(String demoType) {
        try {
            switch (demoType.toLowerCase()) {
                case "simplified":
                    System.out.println("\n--- Running Simplified API Demo ---");
                    SimplifiedAPIDemo.main(new String[]{});
                    break;
                    
                case "financial":
                    System.out.println("\n--- Running Financial Domain Demo ---");
                    CommoditySwapValidationDemo.main(new String[]{});
                    break;
                    
                case "performance":
                    System.out.println("\n--- Running Performance & Exception Demo ---");
                    PerformanceAndExceptionShowcase.main(new String[]{});
                    break;
                    
                case "complete":
                    System.out.println("\n--- Running Complete Demo Suite ---");
                    runDemo("simplified");
                    System.out.println("\n" + "=".repeat(50));
                    runDemo("financial");
                    System.out.println("\n" + "=".repeat(50));
                    runDemo("performance");
                    System.out.println("\n--- Complete Demo Suite Finished ---");
                    break;
                    
                default:
                    System.out.println("Unknown demo type: " + demoType);
                    System.out.println("Available types: simplified, financial, performance, complete");
            }
        } catch (Exception e) {
            System.err.println("Error running demo '" + demoType + "': " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static void displayHelp() {
        System.out.println("\n=== Demo Help ===");
        System.out.println();
        System.out.println("This comprehensive demo showcases the Rules Engine framework capabilities:");
        System.out.println();
        System.out.println("1. SIMPLIFIED API DEMO");
        System.out.println("   - Demonstrates basic rules engine usage");
        System.out.println("   - Shows simple rule creation and evaluation");
        System.out.println("   - Perfect for getting started");
        System.out.println();
        System.out.println("2. FINANCIAL DOMAIN DEMO");
        System.out.println("   - Real-world financial instrument validation");
        System.out.println("   - Commodity swap validation rules");
        System.out.println("   - Static data enrichment examples");
        System.out.println();
        System.out.println("3. PERFORMANCE & EXCEPTION DEMO");
        System.out.println("   - Advanced performance monitoring");
        System.out.println("   - Exception handling strategies");
        System.out.println("   - Layered API architecture");
        System.out.println();
        System.out.println("4. COMPLETE DEMO SUITE");
        System.out.println("   - Runs all demos in sequence");
        System.out.println("   - Comprehensive showcase of all features");
        System.out.println("   - Best for full evaluation");
        System.out.println();
        System.out.println("Command Line Usage:");
        System.out.println("  java ComprehensiveRulesEngineDemo [demo-type]");
        System.out.println("  Where demo-type is: simplified, financial, performance, or complete");
        System.out.println();
    }
}
