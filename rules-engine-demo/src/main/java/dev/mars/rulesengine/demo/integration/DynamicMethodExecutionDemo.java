package dev.mars.rulesengine.demo.integration;

import dev.mars.rulesengine.core.engine.config.RulesEngine;
import dev.mars.rulesengine.core.engine.config.RulesEngineConfiguration;
import dev.mars.rulesengine.core.service.engine.ExpressionEvaluatorService;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.util.*;

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
 * Demonstration of dynamic method execution using SpEL expressions.
 *
 * This class is part of the PeeGeeQ message queue system, providing
 * production-ready PostgreSQL-based message queuing capabilities.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-07-27
 * @version 1.0
 */
/**
 * Demonstration of dynamic method execution using SpEL expressions.
 * This class shows the step-by-step process of creating and using dynamic method execution
 * with SpEL expressions, without relying on external demo classes.
 *
 * This is a demo class with no public constructors or methods except for the main method.
 */
public class DynamicMethodExecutionDemo {

    /**
     * Private constructor to prevent instantiation.
     * This is a demo class that should only be run via the main method.
     */
    private DynamicMethodExecutionDemo() {
        // Private constructor to prevent instantiation
    }

    /**
     * Main method to run the demonstration.
     *
     * @param args Command line arguments (not used)
     */
    public static void main(String[] args) {
        // Run the demonstration
        runDynamicMethodExecutionDemo();
    }

    /**
     * Run the dynamic method execution demonstration.
     * This method shows the step-by-step process of creating and using dynamic method execution.
     */
    private static void runDynamicMethodExecutionDemo() {
        System.out.println("Starting dynamic method execution demonstration");

        // Step 1: Create a RulesEngine
        System.out.println("Step 1: Creating a RulesEngine");
        RulesEngine rulesEngine = new RulesEngine(new RulesEngineConfiguration());

        // Step 2: Create an ExpressionEvaluatorService
        System.out.println("Step 2: Creating an ExpressionEvaluatorService");
        ExpressionEvaluatorService evaluatorService = new ExpressionEvaluatorService();

        // Step 3: Create a DynamicMethodExecutionDemoConfig
        System.out.println("Step 3: Creating a DynamicMethodExecutionDemoConfig");
        DynamicMethodExecutionDemoConfig config = new DynamicMethodExecutionDemoConfig(rulesEngine, evaluatorService);

        // Step 4: Create an evaluation context with self-contained services
        System.out.println("Step 4: Creating an evaluation context");
        StandardEvaluationContext context = config.createContext();

        // Step 5: Demonstrate settlement processing
        System.out.println("Step 5: Demonstrating settlement processing");
        demonstrateSettlementProcessing(config, context);

        // Step 6: Demonstrate risk management
        System.out.println("Step 6: Demonstrating risk management");
        demonstrateRiskManagement(config, context);

        // Step 7: Demonstrate compliance and regulatory reporting
        System.out.println("Step 7: Demonstrating compliance and regulatory reporting");
        demonstrateComplianceAndReporting(config, context);

        // Step 8: Demonstrate fee calculations
        System.out.println("Step 8: Demonstrating fee calculations");
        demonstrateFeeCalculations(config, context);

        // Step 9: Demonstrate conditional processing
        System.out.println("Step 9: Demonstrating conditional processing");
        demonstrateConditionalProcessing(config, context);

        System.out.println("Dynamic method execution demonstration completed");
    }

    /**
     * Demonstrate settlement processing.
     *
     * @param config The dynamic method execution config
     * @param context The evaluation context
     */
    private static void demonstrateSettlementProcessing(DynamicMethodExecutionDemoConfig config, StandardEvaluationContext context) {
        System.out.println("\n----- CATEGORY 1: SETTLEMENT PROCESSING -----");

        // Example 1: Calculate settlement days for different trade types
        String[] tradeTypes = {"equity", "fixedIncome", "derivative", "forex", "commodity"};
        for (String tradeType : tradeTypes) {
            String expression = "#settlementService.calculateSettlementDays(#trades['" + tradeType + "'])";
            Integer days = config.evaluateSettlementProcessing(expression, context, Integer.class);
            System.out.println("Example 1." + (tradeType.charAt(0) - 'a' + 1) +
                    ": Settlement days for " + tradeType + " trade: " + days);
        }

        // Example 2: Determine settlement method for different trade types
        for (String tradeType : tradeTypes) {
            String expression = "#settlementService.determineSettlementMethod(#trades['" + tradeType + "'])";
            String method = config.evaluateSettlementProcessing(expression, context, String.class);
            System.out.println("Example 2." + (tradeType.charAt(0) - 'a' + 1) +
                    ": Settlement method for " + tradeType + " trade: " + method);
        }
    }

    /**
     * Demonstrate risk management.
     *
     * @param config The dynamic method execution config
     * @param context The evaluation context
     */
    private static void demonstrateRiskManagement(DynamicMethodExecutionDemoConfig config, StandardEvaluationContext context) {
        System.out.println("\n----- CATEGORY 2: RISK MANAGEMENT -----");

        // Example 3: Calculate market risk for different trade types
        String[] tradeTypes = {"equity", "fixedIncome", "derivative", "forex", "commodity"};
        for (String tradeType : tradeTypes) {
            String expression = "#riskService.calculateMarketRisk(#trades['" + tradeType + "'])";
            Double risk = config.evaluateRiskManagement(expression, context, Double.class);
            System.out.println("Example 3." + (tradeType.charAt(0) - 'a' + 1) +
                    ": Market risk for " + tradeType + " trade: " + risk);
        }

        // Example 4: Calculate credit risk for different trade types
        for (String tradeType : tradeTypes) {
            String expression = "#riskService.calculateCreditRisk(#trades['" + tradeType + "'])";
            Double risk = config.evaluateRiskManagement(expression, context, Double.class);
            System.out.println("Example 4." + (tradeType.charAt(0) - 'a' + 1) +
                    ": Credit risk for " + tradeType + " trade: " + risk);
        }
    }

    /**
     * Demonstrate compliance and regulatory reporting.
     *
     * @param config The dynamic method execution config
     * @param context The evaluation context
     */
    private static void demonstrateComplianceAndReporting(DynamicMethodExecutionDemoConfig config, StandardEvaluationContext context) {
        System.out.println("\n----- CATEGORY 3: COMPLIANCE AND REGULATORY REPORTING -----");

        // Example 5: Get applicable regulations for different trade types
        String[] tradeTypes = {"equity", "fixedIncome", "derivative", "forex", "commodity"};
        for (String tradeType : tradeTypes) {
            String expression = "#complianceService.getApplicableRegulations(#trades['" + tradeType + "'])";
            List<String> regulations = config.evaluateComplianceAndReporting(expression, context, List.class);
            System.out.println("Example 5." + (tradeType.charAt(0) - 'a' + 1) +
                    ": Applicable regulations for " + tradeType + " trade: " + regulations);
        }
    }

    /**
     * Demonstrate fee calculations.
     *
     * @param config The dynamic method execution config
     * @param context The evaluation context
     */
    private static void demonstrateFeeCalculations(DynamicMethodExecutionDemoConfig config, StandardEvaluationContext context) {
        System.out.println("\n----- CATEGORY 4: FEE CALCULATIONS -----");

        // Example 6: Calculate standard price for different base prices
        double[] basePrices = {100.0, 500.0, 1000.0, 5000.0};
        for (int i = 0; i < basePrices.length; i++) {
            String expression = "#pricingService.calculateStandardPrice(" + basePrices[i] + ")";
            Double price = config.evaluateFeeCalculation(expression, context, Double.class);
            System.out.println("Example 6." + (i + 1) +
                    ": Standard price for base price $" + basePrices[i] + ": $" + price);
        }

        // Example 7: Calculate premium price for different base prices
        for (int i = 0; i < basePrices.length; i++) {
            String expression = "#pricingService.calculatePremiumPrice(" + basePrices[i] + ")";
            Double price = config.evaluateFeeCalculation(expression, context, Double.class);
            System.out.println("Example 7." + (i + 1) +
                    ": Premium price for base price $" + basePrices[i] + ": $" + price);
        }
    }

    /**
     * Demonstrate conditional processing.
     *
     * @param config The dynamic method execution config
     * @param context The evaluation context
     */
    private static void demonstrateConditionalProcessing(DynamicMethodExecutionDemoConfig config, StandardEvaluationContext context) {
        System.out.println("\n----- CATEGORY 5: CONDITIONAL PROCESSING -----");

        // Example 8: Conditional processing based on trade type
        String[] tradeTypes = {"equity", "fixedIncome", "derivative", "forex", "commodity"};
        for (String tradeType : tradeTypes) {
            String expression = "#trades['" + tradeType + "'].value == 'Equity' ? 'Apply equity rules' : " +
                    "#trades['" + tradeType + "'].value == 'FixedIncome' ? 'Apply fixed income rules' : " +
                    "#trades['" + tradeType + "'].value == 'Derivative' ? 'Apply derivative rules' : " +
                    "#trades['" + tradeType + "'].value == 'Forex' ? 'Apply forex rules' : " +
                    "#trades['" + tradeType + "'].value == 'Commodity' ? 'Apply commodity rules' : " +
                    "'Apply default rules'";
            String result = config.evaluateConditionalProcessing(expression, context, String.class);
            System.out.println("Example 8." + (tradeType.charAt(0) - 'a' + 1) +
                    ": Conditional processing for " + tradeType + " trade: " + result);
        }
    }
}
