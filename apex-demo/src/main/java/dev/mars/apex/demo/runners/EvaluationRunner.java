package dev.mars.apex.demo.runners;

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

import dev.mars.apex.demo.evaluation.RuleConfigurationHardcodedBootstrap;
import dev.mars.apex.demo.evaluation.RuleConfigurationHardcodedDemo;
import dev.mars.apex.demo.evaluation.RuleDefinitionServiceDemo;
import dev.mars.apex.demo.evaluation.ScenarioBasedProcessingDemo;
import dev.mars.apex.demo.evaluation.SimplifiedAPIDemo;
import dev.mars.apex.demo.evaluation.TradeRecordMatcherDemo;
import dev.mars.apex.demo.evaluation.YamlConfigurationDemo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Scanner;

/**
 * EvaluationRunner - APEX Evaluation Demonstrations
 * 
 * This runner provides complete coverage of all evaluation-related demos in the APEX Rules Engine.
 * 
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-08-29
 * @version 1.0
 */
public class EvaluationRunner {
    
    private static final Logger logger = LoggerFactory.getLogger(EvaluationRunner.class);
    
    public static void main(String[] args) {
        EvaluationRunner runner = new EvaluationRunner();
        
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
                System.out.println("\nThank you for exploring APEX Evaluation capabilities!");
                break;
            }
            
            executeChoice(choice);
        }
        
        scanner.close();
    }
    
    private void runDirectMode(String demoName) {
        displayBanner();
        System.out.println("Running evaluation demo: " + demoName);
        System.out.println();
        
        executeChoice(demoName.toLowerCase());
    }
    
    private void displayBanner() {
        System.out.println("================================================================================");
        System.out.println("                     APEX RULES ENGINE - EVALUATION DEMOS                      ");
        System.out.println("================================================================================");
        System.out.println();
        System.out.println("Welcome to comprehensive APEX evaluation demonstrations!");
        System.out.println();
    }
    
    private void displayMenu() {
        System.out.println("Available Evaluation Demonstrations:");
        System.out.println("  1. APEX Rules Engine Demo - Core rule engine functionality");
        System.out.println("  2. Layered API Demo - Multi-layer API demonstrations");
        System.out.println("  3. Performance Demo - Performance testing and optimization");
        System.out.println("  4. YAML Configuration Demo - Configuration-driven processing");
        System.out.println("  5. Advanced Features Demo - Complex evaluation scenarios");
        System.out.println("  6. Compliance Service Demo - Compliance rule processing");
        System.out.println("  7. Dynamic Method Execution Demo - Dynamic method invocation");
        System.out.println("  8. Financial Demo - Financial calculations and rules");
        System.out.println("  9. Fluent Rule Builder Example - Fluent API demonstrations");
        System.out.println(" 10. Performance & Exception Demo - Error handling and performance");
        System.out.println(" 11. Post Trade Processing Service Demo - Trade processing workflows");
        System.out.println(" 12. Pricing Service Demo - Pricing calculations");
        System.out.println(" 13. Risk Management Service - Risk assessment rules");
        System.out.println(" 14. Rule Configuration Bootstrap - Configuration setup");
        System.out.println(" 15. Rule Configuration Hardcoded Bootstrap - Hardcoded setup");
        System.out.println(" 16. Rule Configuration Hardcoded Demo - Hardcoded configuration");
        System.out.println(" 17. Rule Definition Service Demo - Rule definition management");
        System.out.println(" 18. Scenario Based Processing Demo - Scenario-driven processing");
        System.out.println(" 19. Simplified API Demo - Simplified API usage");
        System.out.println(" 20. Trade Record Matcher Demo - Trade matching algorithms");
        System.out.println(" 21. Run All Evaluation Demos - Execute complete suite");
        System.out.println();
    }
    
    private void executeChoice(String choice) {
        try {
            switch (choice) {
                case "1":
                case "engine":
                case "rules":
                    runDemo("APEX Rules Engine Demo", () -> ApexRulesEngineDemo.main(new String[]{}));
                    break;
                case "2":
                case "layered":
                case "api":
                    runDemo("Layered API Demo", () -> LayeredAPIDemo.main(new String[]{}));
                    break;
                case "3":
                case "performance":
                case "perf":
                    runDemo("Performance Demo", () -> PerformanceDemo.main(new String[]{}));
                    break;
                case "4":
                case "yaml":
                case "config":
                    runDemo("YAML Configuration Demo", () -> YamlConfigurationDemo.main(new String[]{}));
                    break;
                case "5":
                case "advanced":
                case "features":
                    runDemo("Advanced Features Demo", () -> ApexAdvancedFeaturesDemo.main(new String[]{}));
                    break;
                case "6":
                case "compliance":
                    runDemo("Compliance Service Demo", () -> ComplianceServiceDemo.main(new String[]{}));
                    break;
                case "7":
                case "dynamic":
                case "method":
                    runDemo("Dynamic Method Execution Demo", () -> DynamicMethodExecutionDemo.main(new String[]{}));
                    break;
                case "8":
                case "financial":
                    runDemo("Financial Demo", () -> FinancialDemo.main(new String[]{}));
                    break;
                case "9":
                case "fluent":
                case "builder":
                    runDemo("Fluent Rule Builder Example", () -> FluentRuleBuilderExample.main(new String[]{}));
                    break;
                case "10":
                case "exception":
                    runDemo("Performance & Exception Demo", () -> PerformanceAndExceptionDemo.main(new String[]{}));
                    break;
                case "11":
                case "posttrade":
                case "trade":
                    runDemo("Post Trade Processing Service Demo", () -> PostTradeProcessingServiceDemo.main(new String[]{}));
                    break;
                case "12":
                case "pricing":
                    runDemo("Pricing Service Demo", () -> PricingServiceDemo.main(new String[]{}));
                    break;
                case "13":
                case "risk":
                    runDemo("Risk Management Service", () -> RiskManagementService.main(new String[]{}));
                    break;
                case "14":
                case "bootstrap":
                    runDemo("Rule Configuration Bootstrap", () -> RuleConfigurationBootstrap.main(new String[]{}));
                    break;

                case "15":
                case "hardcoded":
                    runDemo("Rule Configuration Hardcoded Bootstrap", () -> RuleConfigurationHardcodedBootstrap.main(new String[]{}));
                    break;
                case "16":
                case "hardcodeddemo":
                    runDemo("Rule Configuration Hardcoded Demo", () -> RuleConfigurationHardcodedDemo.main(new String[]{}));
                    break;
                case "17":
                case "definition":
                    runDemo("Rule Definition Service Demo", () -> RuleDefinitionServiceDemo.main(new String[]{}));
                    break;
                case "18":
                case "scenario":
                    runDemo("Scenario Based Processing Demo", () -> ScenarioBasedProcessingDemo.main(new String[]{}));
                    break;
                case "19":
                case "simplified":
                    runDemo("Simplified API Demo", () -> SimplifiedAPIDemo.main(new String[]{}));
                    break;
                case "20":
                case "matcher":
                    runDemo("Trade Record Matcher Demo", () -> TradeRecordMatcherDemo.main(new String[]{}));
                    break;
                case "21":
                case "all":
                    runAllEvaluationDemos();
                    break;
                default:
                    System.out.println("Invalid choice: " + choice);
                    System.out.println("Please try again or type 'q' to quit.");
                    break;
            }
        } catch (Exception e) {
            logger.error("Error executing evaluation demo: {}", e.getMessage(), e);
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
    
    private void runAllEvaluationDemos() {
        logger.info("Running All Evaluation Demonstrations");
        logger.info("==================================================");
        logger.info("");
        
        long totalStartTime = System.currentTimeMillis();
        
        runDemo("APEX Rules Engine Demo", () -> ApexRulesEngineDemo.main(new String[]{}));
        runDemo("Layered API Demo", () -> LayeredAPIDemo.main(new String[]{}));
        runDemo("Performance Demo", () -> PerformanceDemo.main(new String[]{}));
        runDemo("YAML Configuration Demo", () -> YamlConfigurationDemo.main(new String[]{}));
        runDemo("Advanced Features Demo", () -> ApexAdvancedFeaturesDemo.main(new String[]{}));
        runDemo("Compliance Service Demo", () -> ComplianceServiceDemo.main(new String[]{}));
        runDemo("Dynamic Method Execution Demo", () -> DynamicMethodExecutionDemo.main(new String[]{}));
        runDemo("Financial Demo", () -> FinancialDemo.main(new String[]{}));
        runDemo("Fluent Rule Builder Example", () -> FluentRuleBuilderExample.main(new String[]{}));
        runDemo("Performance & Exception Demo", () -> PerformanceAndExceptionDemo.main(new String[]{}));
        runDemo("Post Trade Processing Service Demo", () -> PostTradeProcessingServiceDemo.main(new String[]{}));
        runDemo("Pricing Service Demo", () -> PricingServiceDemo.main(new String[]{}));
        runDemo("Risk Management Service", () -> RiskManagementService.main(new String[]{}));
        runDemo("Rule Configuration Bootstrap", () -> RuleConfigurationBootstrap.main(new String[]{}));

        runDemo("Rule Configuration Hardcoded Bootstrap", () -> RuleConfigurationHardcodedBootstrap.main(new String[]{}));
        runDemo("Rule Configuration Hardcoded Demo", () -> RuleConfigurationHardcodedDemo.main(new String[]{}));
        runDemo("Rule Definition Service Demo", () -> RuleDefinitionServiceDemo.main(new String[]{}));
        runDemo("Scenario Based Processing Demo", () -> ScenarioBasedProcessingDemo.main(new String[]{}));
        runDemo("Simplified API Demo", () -> SimplifiedAPIDemo.main(new String[]{}));
        runDemo("Trade Record Matcher Demo", () -> TradeRecordMatcherDemo.main(new String[]{}));
        
        long totalEndTime = System.currentTimeMillis();
        long totalTime = totalEndTime - totalStartTime;
        
        logger.info("");
        logger.info("All evaluation demonstrations completed!");
        logger.info("Total execution time: {} ms ({} seconds)", totalTime, totalTime / 1000.0);
    }
    
    @FunctionalInterface
    private interface DemoExecutor {
        void execute() throws Exception;
    }
}
