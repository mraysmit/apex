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

import dev.mars.apex.demo.evaluation.ApexAdvancedFeaturesDemo;
import dev.mars.apex.demo.evaluation.ApexRulesEngineDemo;
import dev.mars.apex.demo.evaluation.ComplianceServiceDemo;
import dev.mars.apex.demo.evaluation.DynamicMethodExecutionDemo;
import dev.mars.apex.demo.evaluation.FinancialDemo;
import dev.mars.apex.demo.evaluation.FluentRuleBuilderExample;
import dev.mars.apex.demo.evaluation.LayeredAPIDemo;
import dev.mars.apex.demo.evaluation.PerformanceAndExceptionDemo;
import dev.mars.apex.demo.evaluation.PerformanceDemo;
import dev.mars.apex.demo.evaluation.PostTradeProcessingServiceDemo;
import dev.mars.apex.demo.evaluation.PricingServiceDemo;
import dev.mars.apex.demo.evaluation.RiskManagementService;
import dev.mars.apex.demo.evaluation.RuleConfigurationBootstrap;
import dev.mars.apex.demo.evaluation.RuleDefinitionServiceDemo;
import dev.mars.apex.demo.evaluation.ScenarioBasedProcessingDemo;
import dev.mars.apex.demo.evaluation.SimplifiedAPIDemo;
import dev.mars.apex.demo.evaluation.TradeRecordMatcherDemo;
import dev.mars.apex.demo.evaluation.YamlConfigurationDemo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Scanner;

/**
 * EvaluationRunner - Comprehensive APEX Evaluation Demonstrations
 * 
 * This runner provides complete coverage of all evaluation-related demos in the APEX Rules Engine,
 * demonstrating rule execution, business logic evaluation, and decision-making capabilities.
 * 
 * EVALUATION CATEGORIES COVERED:
 * • Core Engine: Basic rule evaluation and engine capabilities
 * • API Layers: Simplified, layered, and advanced API demonstrations
 * • Financial Services: Trading, pricing, risk management, and compliance
 * • Performance: Optimization, monitoring, and exception handling
 * • Configuration: YAML-based rule configuration and management
 * • Advanced Features: Dynamic execution, fluent builders, and complex scenarios
 * 
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-08-29
 * @version 1.0
 */
public class EvaluationRunner {
    
    private static final Logger logger = LoggerFactory.getLogger(EvaluationRunner.class);
    
    /**
     * Main entry point for Evaluation demos.
     * 
     * @param args command line arguments: empty for interactive mode, demo name for direct execution
     */
    public static void main(String[] args) {
        EvaluationRunner runner = new EvaluationRunner();
        
        if (args.length == 0) {
            runner.runInteractiveMode();
        } else {
            runner.runDirectMode(args[0]);
        }
    }
    
    /**
     * Run in interactive mode with menu selection.
     */
    private void runInteractiveMode() {
        displayBanner();
        
        Scanner scanner = new Scanner(System.in);
        
        while (true) {
            displayMenu();
            System.out.print("Enter your choice (1-18, or 'q' to quit): ");
            String choice = scanner.nextLine().trim().toLowerCase();
            
            if (choice.equals("q") || choice.equals("quit") || choice.equals("exit")) {
                System.out.println("\n✅ Thank you for exploring APEX Evaluation capabilities!");
                break;
            }
            
            executeChoice(choice);
        }
        
        scanner.close();
    }
    
    /**
     * Run specific demo directly.
     */
    private void runDirectMode(String demoName) {
        displayBanner();
        System.out.println("Running evaluation demo: " + demoName);
        System.out.println();
        
        executeChoice(demoName.toLowerCase());
    }
    
    /**
     * Display the runner banner.
     */
    private void displayBanner() {
        logger.info("╔" + "═".repeat(78) + "╗");
        logger.info("║" + " ".repeat(22) + "APEX RULES ENGINE - EVALUATION DEMOS" + " ".repeat(20) + "║");
        logger.info("╚" + "═".repeat(78) + "╝");
        logger.info("");
        logger.info("Welcome to comprehensive APEX evaluation demonstrations! ⚡");
        logger.info("");
        logger.info("These demos showcase rule execution, business logic evaluation,");
        logger.info("and decision-making capabilities of the APEX Rules Engine.");
        logger.info("");
    }
    
    /**
     * Display the interactive menu.
     */
    private void displayMenu() {
        System.out.println("Available Evaluation Demonstrations:");
        System.out.println("  1. APEX Rules Engine Demo - Core engine capabilities");
        System.out.println("  2. Simplified API Demo - Easy-to-use API layer");
        System.out.println("  3. Layered API Demo - Three-tier API architecture");
        System.out.println("  4. YAML Configuration Demo - Configuration-driven rules");
        System.out.println("  5. Advanced Features Demo - Sophisticated rule patterns");
        System.out.println("  6. Financial Demo - Comprehensive financial services");
        System.out.println("  7. Performance Demo - Performance monitoring and optimization");
        System.out.println("  8. Performance & Exception Demo - Advanced performance patterns");
        System.out.println("  9. Dynamic Method Execution Demo - Runtime method invocation");
        System.out.println(" 10. Fluent Rule Builder Example - Programmatic rule building");
        System.out.println(" 11. Compliance Service Demo - Regulatory compliance patterns");
        System.out.println(" 12. Risk Management Service - Risk assessment and monitoring");
        System.out.println(" 13. Pricing Service Demo - Dynamic pricing calculations");
        System.out.println(" 14. Post-Trade Processing Demo - Trade lifecycle management");
        System.out.println(" 15. Trade Record Matcher Demo - Trade matching algorithms");
        System.out.println(" 16. Rule Configuration Bootstrap - Configuration management");
        System.out.println(" 17. Scenario-Based Processing Demo - Complex workflow scenarios");
        System.out.println(" 18. Run All Evaluation Demos - Execute complete suite");
        System.out.println();
    }
    
    /**
     * Execute the selected choice.
     */
    private void executeChoice(String choice) {
        try {
            switch (choice) {
                case "1":
                case "apex":
                case "engine":
                    runDemo("APEX Rules Engine Demo", ApexRulesEngineDemo::main);
                    break;
                case "2":
                case "simplified":
                case "simple":
                    runDemo("Simplified API Demo", SimplifiedAPIDemo::main);
                    break;
                case "3":
                case "layered":
                case "api":
                    runDemo("Layered API Demo", LayeredAPIDemo::main);
                    break;
                case "4":
                case "yaml":
                case "configuration":
                    runDemo("YAML Configuration Demo", YamlConfigurationDemo::main);
                    break;
                case "5":
                case "advanced":
                case "features":
                    runDemo("Advanced Features Demo", ApexAdvancedFeaturesDemo::main);
                    break;
                case "6":
                case "financial":
                case "finance":
                    runDemo("Financial Demo", FinancialDemo::main);
                    break;
                case "7":
                case "performance":
                case "perf":
                    runDemo("Performance Demo", PerformanceDemo::main);
                    break;
                case "8":
                case "performanceexception":
                case "exception":
                    runDemo("Performance & Exception Demo", PerformanceAndExceptionDemo::main);
                    break;
                case "9":
                case "dynamic":
                case "method":
                    runDemo("Dynamic Method Execution Demo", DynamicMethodExecutionDemo::main);
                    break;
                case "10":
                case "fluent":
                case "builder":
                    runDemo("Fluent Rule Builder Example", FluentRuleBuilderExample::main);
                    break;
                case "11":
                case "compliance":
                    runDemo("Compliance Service Demo", ComplianceServiceDemo::main);
                    break;
                case "12":
                case "risk":
                case "management":
                    runDemo("Risk Management Service", RiskManagementService::main);
                    break;
                case "13":
                case "pricing":
                case "price":
                    runDemo("Pricing Service Demo", PricingServiceDemo::main);
                    break;
                case "14":
                case "posttrade":
                case "trade":
                    runDemo("Post-Trade Processing Demo", PostTradeProcessingServiceDemo::main);
                    break;
                case "15":
                case "matcher":
                case "matching":
                    runDemo("Trade Record Matcher Demo", TradeRecordMatcherDemo::main);
                    break;
                case "16":
                case "bootstrap":
                case "config":
                    runDemo("Rule Configuration Bootstrap", RuleConfigurationBootstrap::main);
                    break;
                case "17":
                case "scenario":
                case "scenarios":
                    runDemo("Scenario-Based Processing Demo", ScenarioBasedProcessingDemo::main);
                    break;
                case "18":
                case "all":
                    runAllEvaluationDemos();
                    break;
                default:
                    System.out.println("❌ Invalid choice: " + choice);
                    System.out.println("Please try again or type 'q' to quit.");
                    break;
            }
        } catch (Exception e) {
            logger.error("❌ Error executing evaluation demo: {}", e.getMessage(), e);
        }
        
        System.out.println();
    }
    
    /**
     * Run a single demo with error handling.
     */
    private void runDemo(String demoName, DemoExecutor executor) {
        logger.info("▶ Running: {}", demoName);
        logger.info("─".repeat(50));
        
        try {
            long startTime = System.currentTimeMillis();
            executor.execute(new String[]{});
            long endTime = System.currentTimeMillis();
            
            logger.info("✅ {} completed successfully", demoName);
            logger.info("Execution time: {} ms", (endTime - startTime));
            
        } catch (Exception e) {
            logger.warn("⚠ {} encountered issues: {}", demoName, e.getMessage());
            logger.info("📝 This may be expected if dependencies are not available");
            logger.info("🎯 The evaluation concepts and patterns are still demonstrated!");
        }
    }
    
    /**
     * Run all evaluation demos in sequence.
     */
    private void runAllEvaluationDemos() {
        logger.info("🚀 Running All Evaluation Demonstrations");
        logger.info("═".repeat(50));
        logger.info("");
        
        long totalStartTime = System.currentTimeMillis();
        
        // Core engine demos
        runDemo("APEX Rules Engine Demo", ApexRulesEngineDemo::main);
        runDemo("Simplified API Demo", SimplifiedAPIDemo::main);
        runDemo("Layered API Demo", LayeredAPIDemo::main);
        runDemo("YAML Configuration Demo", YamlConfigurationDemo::main);
        runDemo("Advanced Features Demo", ApexAdvancedFeaturesDemo::main);
        
        // Financial services demos
        runDemo("Financial Demo", FinancialDemo::main);
        runDemo("Compliance Service Demo", ComplianceServiceDemo::main);
        runDemo("Risk Management Service", RiskManagementService::main);
        runDemo("Pricing Service Demo", PricingServiceDemo::main);
        runDemo("Post-Trade Processing Demo", PostTradeProcessingServiceDemo::main);
        runDemo("Trade Record Matcher Demo", TradeRecordMatcherDemo::main);
        
        // Performance and advanced demos
        runDemo("Performance Demo", PerformanceDemo::main);
        runDemo("Performance & Exception Demo", PerformanceAndExceptionDemo::main);
        runDemo("Dynamic Method Execution Demo", DynamicMethodExecutionDemo::main);
        runDemo("Fluent Rule Builder Example", FluentRuleBuilderExample::main);
        runDemo("Rule Configuration Bootstrap", RuleConfigurationBootstrap::main);
        runDemo("Scenario-Based Processing Demo", ScenarioBasedProcessingDemo::main);
        
        long totalEndTime = System.currentTimeMillis();
        long totalTime = totalEndTime - totalStartTime;
        
        logger.info("");
        logger.info("🎉 All evaluation demonstrations completed!");
        logger.info("Total execution time: {} ms ({} seconds)", totalTime, totalTime / 1000.0);
        logger.info("");
        logger.info("You've experienced the full range of APEX evaluation capabilities!");
    }
    
    /**
     * Functional interface for demo execution.
     */
    @FunctionalInterface
    private interface DemoExecutor {
        void execute(String[] args) throws Exception;
    }
}
