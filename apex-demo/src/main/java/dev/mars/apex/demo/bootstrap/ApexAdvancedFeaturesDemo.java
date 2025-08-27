package dev.mars.apex.demo.bootstrap;

import dev.mars.apex.core.engine.config.RulesEngine;
import dev.mars.apex.core.engine.config.RulesEngineConfiguration;
import dev.mars.apex.core.engine.model.Rule;
import dev.mars.apex.core.engine.model.RuleResult;
import dev.mars.apex.core.service.engine.ExpressionEvaluatorService;
import dev.mars.apex.core.service.engine.RuleEngineService;
import dev.mars.apex.core.service.engine.TemplateProcessorService;
import dev.mars.apex.core.service.data.DataSource;
import dev.mars.apex.core.service.lookup.LookupService;
import dev.mars.apex.core.service.data.DataServiceManager;
import dev.mars.apex.demo.rulesets.PricingServiceDemo;
import dev.mars.apex.demo.model.Customer;
import dev.mars.apex.demo.model.Product;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.util.*;
import java.util.logging.Logger;

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
 * Comprehensive APEX Advanced Features Demo.
 *
 * This merged class combines the functionality of:
 * - ApexAdvancedFeaturesDemo (main demo logic)
 * - ApexAdvancedFeaturesDemoConfig (configuration and rules)
 * - ApexAdvancedFeaturesDataProvider (data provisioning)
 *
 * It demonstrates advanced features of SpEL for dynamic evaluation,
 * focusing on collection manipulation, array operations, complex expressions,
 * template processing, and rule engine capabilities.
 *
 * This class is part of the APEX A powerful expression processor for Java applications.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-07-27
 * @version 1.0
 */
public class ApexAdvancedFeaturesDemo {
    private static final Logger LOGGER = Logger.getLogger(ApexAdvancedFeaturesDemo.class.getName());

    // Core APEX services
    private final RulesEngine rulesEngine;
    private final ExpressionEvaluatorService evaluatorService;
    private final RuleEngineService ruleEngineService;
    private final TemplateProcessorService templateProcessorService;
    private final DataServiceManager dataServiceManager;

    // Data management
    private final Map<String, Object> dataStore = new HashMap<>();
    private final List<DataSource> dataSources = new ArrayList<>();

    /**
     * Constructor with all required services.
     *
     * @param rulesEngine The rules engine to use
     * @param evaluatorService The expression evaluator service to use
     * @param ruleEngineService The rule engine service to use
     * @param templateProcessorService The template processor service to use
     */
    public ApexAdvancedFeaturesDemo(
            RulesEngine rulesEngine,
            ExpressionEvaluatorService evaluatorService,
            RuleEngineService ruleEngineService,
            TemplateProcessorService templateProcessorService) {
        this.rulesEngine = rulesEngine;
        this.evaluatorService = evaluatorService;
        this.ruleEngineService = ruleEngineService;
        this.templateProcessorService = templateProcessorService;

        // Initialize DataServiceManager with mock data
        this.dataServiceManager = new DataServiceManager();
        this.dataServiceManager.initializeWithMockData();

        // Initialize internal data sources and data
        initializeDataSources();
        initializeData();
    }

    /**
     * Default constructor that creates all services.
     */
    public ApexAdvancedFeaturesDemo() {
        this.evaluatorService = new ExpressionEvaluatorService();
        this.ruleEngineService = new RuleEngineService(evaluatorService);
        this.templateProcessorService = new TemplateProcessorService(evaluatorService);
        this.rulesEngine = new RulesEngine(new RulesEngineConfiguration());

        // Initialize DataServiceManager with mock data
        this.dataServiceManager = new DataServiceManager();
        this.dataServiceManager.initializeWithMockData();

        // Initialize internal data sources and data
        initializeDataSources();
        initializeData();
    }

    public static void main(String[] args) {
        // Create demo instance with default constructor
        ApexAdvancedFeaturesDemo demo = new ApexAdvancedFeaturesDemo();
        PricingServiceDemo pricingService = new PricingServiceDemo();

        // Run all demonstrations
        demo.demonstrateCollectionOperations();
        demo.demonstrateAdvancedRuleEngine();
        demo.demonstrateDynamicMethodExecution(pricingService);
        demo.demonstrateTemplateExpressions();
        demo.demonstrateXmlTemplateExpressions();
        demo.demonstrateJsonTemplateExpressions();
        demo.demonstrateDynamicLookupService();
        demo.demonstrateRuleResultFeatures();
    }

    // ========================================
    // DATA PROVIDER FUNCTIONALITY (from ApexAdvancedFeaturesDataProvider)
    // ========================================

    /**
     * Initialize data sources.
     */
    private void initializeDataSources() {
        LOGGER.info("Initializing data sources");
        dataSources.add(new DemoDataSource("ProductsDataSource", "products"));
        dataSources.add(new DemoDataSource("InventoryDataSource", "inventory"));
        dataSources.add(new DemoDataSource("CustomerDataSource", "customer"));
        dataSources.add(new DemoDataSource("TemplateCustomerDataSource", "templateCustomer"));
        dataSources.add(new DemoDataSource("LookupServicesDataSource", "lookupServices"));
        dataSources.add(new DemoDataSource("SourceRecordsDataSource", "sourceRecords"));
    }

    /**
     * Initialize data from data sources.
     */
    private void initializeData() {
        LOGGER.info("Initializing data from data sources");
        for (DataSource dataSource : dataSources) {
            String dataType = dataSource.getDataType();
            Object data = dataSource.getData(dataType);
            if (data != null) {
                dataStore.put(dataType, data);
            }
        }
    }

    /**
     * Get data of the specified type.
     *
     * @param dataType The type of data to get
     * @param <T> The type of the data
     * @return The data, or null if not found
     */
    @SuppressWarnings("unchecked")
    public <T> T getData(String dataType) {
        return (T) dataStore.get(dataType);
    }

    // ========================================
    // CONFIGURATION FUNCTIONALITY (from ApexAdvancedFeaturesDemoConfig)
    // ========================================

    /**
     * Create a standard evaluation context with data from the data provider.
     *
     * @return The evaluation context
     */
    public StandardEvaluationContext createContext() {
        StandardEvaluationContext context = new StandardEvaluationContext();

        // Add products data
        List<Product> products = getData("products");
        context.setVariable("products", products);

        // Add inventory data
        List<Product> inventory = getData("inventory");
        context.setVariable("inventory", inventory);

        // Add customer data
        Customer customer = getData("customer");
        context.setVariable("customer", customer);

        return context;
    }

    /**
     * Create a context for template expressions.
     *
     * @return The evaluation context
     */
    public StandardEvaluationContext createTemplateContext() {
        StandardEvaluationContext context = new StandardEvaluationContext();

        // Add template customer data
        Customer customer = getData("templateCustomer");
        context.setVariable("customer", customer);

        // Add products data
        List<Product> products = getData("products");
        context.setVariable("products", products);

        return context;
    }

    // ========================================
    // DEMO METHODS
    // ========================================

    /**
     * Demonstrates collection operations using SpEL.
     */
    private void demonstrateCollectionOperations() {
        System.out.println("\n=== Financial Instrument Collection Operations ===");

        // Get context from internal data
        StandardEvaluationContext context = createContext();

        // Add price threshold variable
        context.setVariable("priceThreshold", 500.0);

        // Collection selection - filter fixed income products
        RuleResult result1 = evaluatorService.evaluateWithResult("#products.?[category == 'FixedIncome']", context, List.class);
        System.out.println("Rule result: " + (result1.isTriggered() ? "Triggered" : "Not triggered"));

        // Collection projection - get all product names
        RuleResult result2 = evaluatorService.evaluateWithResult("#products.![name]", context, List.class);
        System.out.println("Rule result: " + (result2.isTriggered() ? "Triggered" : "Not triggered"));

        // Combining selection and projection - names of equity products
        RuleResult result3 = evaluatorService.evaluateWithResult("#products.?[category == 'Equity'].![name]", context, List.class);
        System.out.println("Rule result: " + (result3.isTriggered() ? "Triggered" : "Not triggered"));

        // First and last elements
        RuleResult result4 = evaluatorService.evaluateWithResult("#products.^[price > #priceThreshold].name", context, String.class);
        System.out.println("First expensive product: " + result4.getMessage());

        RuleResult result5 = evaluatorService.evaluateWithResult("#products.$[price < 200].name", context, String.class);
        System.out.println("Last cheap product: " + result5.getMessage());
    }

    /**
     * Demonstrates advanced rule engine with collection filtering.
     */
    private void demonstrateAdvancedRuleEngine() {
        System.out.println("\n=== Advanced Rule Engine with Collection Filtering ===");

        // Get context from internal data
        StandardEvaluationContext context = createContext();

        // Get rules from internal methods
        List<Rule> rules = createInvestmentRules();

        // Evaluate rules
        ruleEngineService.evaluateRules(rules, context);
    }

    /**
     * Demonstrates dynamic method resolution and execution.
     */
    private void demonstrateDynamicMethodExecution(PricingServiceDemo pricingService) {
        System.out.println("\n=== Dynamic Method Resolution and Execution ===");

        StandardEvaluationContext context = new StandardEvaluationContext();
        context.setVariable("pricingService", pricingService);
        context.setVariable("basePrice", 100.0);

        // Dynamic method call based on pricing strategy
        String[] pricingStrategies = {"Standard", "Premium", "Sale", "Clearance"};
        for (String strategy : pricingStrategies) {
            String methodName = "calculate" + strategy + "Price";
            String expression = "#pricingService." + methodName + "(#basePrice)";

            Double price = evaluatorService.evaluate(expression, context, Double.class);
            System.out.println(strategy + " price: $" + price);
        }
    }

    /**
     * Demonstrates template expressions with placeholders.
     */
    private void demonstrateTemplateExpressions() {
        System.out.println("\n=== Template Expressions with Placeholders ===");

        // Get context from internal data
        StandardEvaluationContext context = createTemplateContext();

        // Process template with customer information
        String template = "Dear #{#customer.name},\n\n" +
                "Thank you for being a valued #{#customer.membershipLevel} member for #{#customer.age} years.\n" +
                "We have selected some investment opportunities in #{#customer.preferredCategories[0]} " +
                "and #{#customer.preferredCategories[1]} that might interest you.\n\n" +
                "Your current discount: #{#customer.membershipLevel == 'Gold' ? '20%' : '10%'}\n\n" +
                "Sincerely,\nInvestment Team";

        String result = templateProcessorService.processTemplate(template, context);
        System.out.println(result);
    }

    /**
     * Demonstrates XML template expressions with placeholders.
     */
    private void demonstrateXmlTemplateExpressions() {
        System.out.println("\n=== XML Template Expressions with Placeholders ===");

        // Get context from internal data
        StandardEvaluationContext context = createTemplateContext();

        // Process XML template with customer and product information
        String xmlTemplate = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<investment-recommendations>\n" +
                "    <customer>\n" +
                "        <name>#{#customer.name}</name>\n" +
                "        <membership>#{#customer.membershipLevel}</membership>\n" +
                "        <age>#{#customer.age}</age>\n" +
                "    </customer>\n" +
                "    <preferred-categories>\n" +
                "        <category>#{#customer.preferredCategories[0]}</category>\n" +
                "        <category>#{#customer.preferredCategories[1]}</category>\n" +
                "    </preferred-categories>\n" +
                "    <recommended-products>\n" +
                "        <product>\n" +
                "            <name>#{#products[0].name}</name>\n" +
                "            <price>#{#products[0].price}</price>\n" +
                "            <category>#{#products[0].category}</category>\n" +
                "        </product>\n" +
                "        <product>\n" +
                "            <name>#{#products[1].name}</name>\n" +
                "            <price>#{#products[1].price}</price>\n" +
                "            <category>#{#products[1].category}</category>\n" +
                "        </product>\n" +
                "    </recommended-products>\n" +
                "</investment-recommendations>";

        String result = templateProcessorService.processXmlTemplate(xmlTemplate, context);
        System.out.println(result);
    }

    /**
     * Demonstrates JSON template expressions with placeholders.
     */
    private void demonstrateJsonTemplateExpressions() {
        System.out.println("\n=== JSON Template Expressions with Placeholders ===");

        // Get context from internal data
        StandardEvaluationContext context = createTemplateContext();

        // Process JSON template with customer and product information
        String jsonTemplate = "{\n" +
                "  \"customer\": {\n" +
                "    \"name\": \"#{#customer.name}\",\n" +
                "    \"membership\": \"#{#customer.membershipLevel}\",\n" +
                "    \"age\": #{#customer.age},\n" +
                "    \"preferredCategories\": [\n" +
                "      \"#{#customer.preferredCategories[0]}\",\n" +
                "      \"#{#customer.preferredCategories[1]}\"\n" +
                "    ]\n" +
                "  },\n" +
                "  \"recommendedProducts\": [\n" +
                "    {\n" +
                "      \"name\": \"#{#products[0].name}\",\n" +
                "      \"price\": #{#products[0].price},\n" +
                "      \"category\": \"#{#products[0].category}\"\n" +
                "    },\n" +
                "    {\n" +
                "      \"name\": \"#{#products[1].name}\",\n" +
                "      \"price\": #{#products[1].price},\n" +
                "      \"category\": \"#{#products[1].category}\"\n" +
                "    }\n" +
                "  ],\n" +
                "  \"discountPercentage\": #{#customer.membershipLevel == 'Gold' ? 20 : 10}\n" +
                "}";

        String result = templateProcessorService.processJsonTemplate(jsonTemplate, context);
        System.out.println(result);
    }

    /**
     * Demonstrates dynamic lookup service.
     */
    private void demonstrateDynamicLookupService() {
        System.out.println("\n=== Dynamic Lookup Service ===");

        // Get context from internal data
        StandardEvaluationContext context = createContext();

        // Get lookup services from data service
        List<LookupService> lookupServices = dataServiceManager.requestData("lookupServices");
        // Ensure lookupServices is never null to prevent SpEL evaluation errors
        if (lookupServices == null) {
            lookupServices = Collections.emptyList();
        }
        context.setVariable("lookupServices", lookupServices);

        // Dynamically find lookup service by name
        String lookupName = "InstrumentTypes";
        context.setVariable("lookupName", lookupName);

        // Use null-safe expression to prevent SpEL evaluation errors
        RuleResult result = evaluatorService.evaluateWithResult(
            "#lookupServices != null && #lookupServices.size() > 0 ? #lookupServices.?[name == #lookupName][0] : null",
            context, LookupService.class);

        if (result.isTriggered()) {
            LookupService lookupService = evaluatorService.evaluate(
                "#lookupServices != null && #lookupServices.size() > 0 ? #lookupServices.?[name == #lookupName][0] : null",
                context, LookupService.class);

            if (lookupService != null) {
                System.out.println("Found lookup service: " + lookupService.getName());
                System.out.println("Values: " + lookupService.getLookupValues());

                // Test validation
                String testValue = "Equity";
                context.setVariable("testValue", testValue);

                Boolean isValid = evaluatorService.evaluate(
                    "#lookupServices != null && #lookupServices.size() > 0 ? #lookupServices.?[name == #lookupName][0]?.validate(#testValue) : false",
                    context, Boolean.class);

                System.out.println("Is '" + testValue + "' valid? " + isValid);
            }
        } else {
            System.out.println("Lookup service not found: " + lookupName);
        }
    }

    /**
     * Demonstrates RuleResult features and capabilities.
     * This method showcases the different result types, properties, and how to use
     * RuleResult for conditional rule execution.
     */
    private void demonstrateRuleResultFeatures() {
        System.out.println("\n=== RuleResult Features and Capabilities ===");

        // Get context from internal data
        StandardEvaluationContext context = createContext();

        // Add additional variables for this demonstration
        context.setVariable("investmentAmount", 150000);
        context.setVariable("accountType", "retirement");
        context.setVariable("clientRiskScore", 8);
        context.setVariable("marketVolatility", 0.25);
        context.setVariable("kycVerified", false);

        System.out.println("\n1. Demonstrating RuleResult Types:");

        // 1. MATCH result type
        RuleResult matchResult = evaluatorService.evaluateWithResult(
            "#investmentAmount > 100000", context, Boolean.class);
        System.out.println("\nMATCH Result:");
        printRuleResultDetails(matchResult);

        // 2. NO_MATCH result type
        RuleResult noMatchResult = evaluatorService.evaluateWithResult(
            "#investmentAmount < 50000", context, Boolean.class);
        System.out.println("\nNO_MATCH Result:");
        printRuleResultDetails(noMatchResult);

        // 3. ERROR result type
        RuleResult errorResult = evaluatorService.evaluateWithResult(
            "#undefinedVariable > 100", context, Boolean.class);
        System.out.println("\nERROR Result:");
        printRuleResultDetails(errorResult);

        // 4. NO_RULES result type (create manually since it's hard to trigger naturally)
        RuleResult noRulesResult = RuleResult.noRules();
        System.out.println("\nNO_RULES Result:");
        printRuleResultDetails(noRulesResult);

        System.out.println("\n2. Using RuleResult for Conditional Rule Execution:");

        // Get rules from internal methods
        List<Rule> ruleResultRules = createRuleResultRules();

        // Extract specific rules for demonstrations
        Rule highValueRule = ruleResultRules.stream()
            .filter(r -> r.getName().equals("HighValueCustomerRule"))
            .findFirst()
            .orElse(new Rule(
                "High-Value Investment",
                "#investmentAmount > 100000",
                "High-value investment detected"
            ));

        Rule retirementAccountRule = ruleResultRules.stream()
            .filter(r -> r.getName().equals("InitialAssessmentRule"))
            .findFirst()
            .orElse(new Rule(
                "Retirement Account",
                "#accountType == 'retirement'",
                "Retirement account detected"
            ));

        Rule highRiskClientRule = ruleResultRules.stream()
            .filter(r -> r.getName().equals("CustomerCategoryRule"))
            .findFirst()
            .orElse(new Rule(
                "High-Risk Client",
                "#clientRiskScore > 7",
                "High-risk client detected"
            ));

        Rule volatileMarketRule = ruleResultRules.stream()
            .filter(r -> r.getName().equals("MidAgeInvestorRule"))
            .findFirst()
            .orElse(new Rule(
                "Volatile Market",
                "#marketVolatility > 0.2",
                "Volatile market conditions detected"
            ));

        Rule kycVerificationRule = ruleResultRules.stream()
            .filter(r -> r.getName().equals("SeniorInvestorRule"))
            .findFirst()
            .orElse(new Rule(
                "KYC Verification",
                "!#kycVerified",
                "KYC verification required"
            ));

        // Demonstrate conditional execution based on triggered status
        System.out.println("\nConditional Execution Based on Triggered Status:");
        executeRuleWithConditionalFollowup(highValueRule, retirementAccountRule, context);

        // Demonstrate execution based on result type
        System.out.println("\nExecution Based on Result Type:");
        executeRuleBasedOnResultType(highRiskClientRule, context);

        // Demonstrate rule chaining using result message
        System.out.println("\nRule Chaining Using Result Message:");
        executeRuleChain(volatileMarketRule, kycVerificationRule, context);

        // Demonstrate dynamic rule selection
        System.out.println("\nDynamic Rule Selection:");
        executeDynamicRuleSelection(context);
    }

    /**
     * Helper method to print details of a RuleResult.
     */
    private void printRuleResultDetails(RuleResult result) {
        System.out.println("  - ID: " + result.getId());
        System.out.println("  - Rule Name: " + result.getRuleName());
        System.out.println("  - Message: " + result.getMessage());
        System.out.println("  - Triggered: " + result.isTriggered());
        System.out.println("  - Result Type: " + result.getResultType());
        System.out.println("  - Timestamp: " + result.getTimestamp());
    }

    /**
     * Demonstrates conditional execution based on triggered status.
     */
    private void executeRuleWithConditionalFollowup(Rule rule1, Rule rule2, StandardEvaluationContext context) {
        // Create a list with the first rule
        List<Rule> rules = new ArrayList<>();
        rules.add(rule1);

        // Evaluate the first rule
        List<RuleResult> results = ruleEngineService.evaluateRules(rules, context);
        RuleResult result = results.get(0);

        System.out.println("Evaluated rule: " + rule1.getName());
        System.out.println("Result: " + (result.isTriggered() ? "Triggered" : "Not triggered"));

        // Conditional execution based on the result
        if (result.isTriggered()) {
            System.out.println("First rule triggered, executing second rule...");

            // Create a list with the second rule
            List<Rule> followupRules = new ArrayList<>();
            followupRules.add(rule2);

            // Evaluate the second rule
            List<RuleResult> followupResults = ruleEngineService.evaluateRules(followupRules, context);
            RuleResult followupResult = followupResults.get(0);

            System.out.println("Evaluated rule: " + rule2.getName());
            System.out.println("Result: " + (followupResult.isTriggered() ? "Triggered" : "Not triggered"));
        } else {
            System.out.println("First rule not triggered, skipping second rule.");
        }
    }

    /**
     * Demonstrates execution based on result type.
     */
    private void executeRuleBasedOnResultType(Rule rule, StandardEvaluationContext context) {
        // Create a list with the rule
        List<Rule> rules = new ArrayList<>();
        rules.add(rule);

        // Evaluate the rule
        List<RuleResult> results = ruleEngineService.evaluateRules(rules, context);
        RuleResult result = results.get(0);

        System.out.println("Evaluated rule: " + rule.getName());

        // Execute different actions based on result type
        switch (result.getResultType()) {
            case MATCH:
                System.out.println("MATCH result: Executing actions for successful match...");
                System.out.println("Action: Flagging client for enhanced due diligence");
                break;
            case NO_MATCH:
                System.out.println("NO_MATCH result: Executing default actions...");
                System.out.println("Action: Proceeding with standard processing");
                break;
            case ERROR:
                System.out.println("ERROR result: Executing error handling...");
                System.out.println("Action: Logging error and notifying administrator");
                break;
            case NO_RULES:
                System.out.println("NO_RULES result: No rules to execute");
                break;
        }
    }

    /**
     * Demonstrates rule chaining using result message.
     */
    private void executeRuleChain(Rule rule1, Rule rule2, StandardEvaluationContext context) {
        // Create a list with the first rule
        List<Rule> rules = new ArrayList<>();
        rules.add(rule1);

        // Evaluate the first rule
        List<RuleResult> results = ruleEngineService.evaluateRules(rules, context);
        RuleResult result = results.get(0);

        System.out.println("Evaluated rule: " + rule1.getName());

        // Use the message from the first rule to determine the next action
        if (result.isTriggered()) {
            System.out.println("First rule triggered with message: " + result.getMessage());

            // The message indicates market volatility, so we check KYC verification
            if (result.getMessage().contains("Volatile market")) {
                System.out.println("Market volatility detected, checking KYC verification status...");

                // Create a list with the second rule
                List<Rule> followupRules = new ArrayList<>();
                followupRules.add(rule2);

                // Evaluate the second rule
                List<RuleResult> followupResults = ruleEngineService.evaluateRules(followupRules, context);
                RuleResult followupResult = followupResults.get(0);

                System.out.println("Evaluated rule: " + rule2.getName());

                if (followupResult.isTriggered()) {
                    System.out.println("KYC verification required. Investment on hold until verification complete.");
                } else {
                    System.out.println("KYC verification complete. Proceeding with investment despite market volatility.");
                }
            }
        } else {
            System.out.println("Market conditions stable, proceeding with standard investment process.");
        }
    }

    /**
     * Demonstrates dynamic rule selection.
     */
    private void executeDynamicRuleSelection(StandardEvaluationContext context) {
        // Get rules from internal methods
        List<Rule> ruleResultRules = createRuleResultRules();

        // Create a map of rules that can be selected dynamically
        Map<String, Rule> ruleRepository = new HashMap<>();

        // Extract rules from the config and add them to the repository
        Rule youngInvestorRule = ruleResultRules.stream()
            .filter(r -> r.getName().equals("YoungInvestorRule"))
            .findFirst()
            .orElse(null);
        if (youngInvestorRule != null) {
            ruleRepository.put("Rule-HighValue", youngInvestorRule);
        } else {
            ruleRepository.put("Rule-HighValue", new Rule(
                "High-Value Investment",
                "#investmentAmount > 100000",
                "High-value investment detected"
            ));
        }

        Rule midAgeInvestorRule = ruleResultRules.stream()
            .filter(r -> r.getName().equals("MidAgeInvestorRule"))
            .findFirst()
            .orElse(null);
        if (midAgeInvestorRule != null) {
            ruleRepository.put("Rule-Retirement", midAgeInvestorRule);
        } else {
            ruleRepository.put("Rule-Retirement", new Rule(
                "Retirement Account",
                "#accountType == 'retirement'",
                "Retirement account detected"
            ));
        }

        Rule seniorInvestorRule = ruleResultRules.stream()
            .filter(r -> r.getName().equals("SeniorInvestorRule"))
            .findFirst()
            .orElse(null);
        if (seniorInvestorRule != null) {
            ruleRepository.put("Rule-HighRisk", seniorInvestorRule);
        } else {
            ruleRepository.put("Rule-HighRisk", new Rule(
                "High-Risk Client",
                "#clientRiskScore > 7",
                "High-risk client detected"
            ));
        }

        // Start with a rule to determine the investment type
        Rule investmentTypeRule = new Rule(
            "Investment Type Determination",
            "#investmentAmount > 100000 ? 'HighValue' : (#accountType == 'retirement' ? 'Retirement' : 'Standard')",
            "Determining investment type"
        );

        // Evaluate the rule to determine which rule to execute next
        RuleResult result = evaluatorService.evaluateWithResult(
            investmentTypeRule.getCondition(), context, String.class);

        System.out.println("Evaluated rule: " + investmentTypeRule.getName());

        if (result.isTriggered()) {
            // Use the result to dynamically select the next rule
            String investmentType = evaluatorService.evaluate(
                investmentTypeRule.getCondition(), context, String.class);

            System.out.println("Investment type determined: " + investmentType);

            String nextRuleName = "Rule-" + investmentType;
            Rule nextRule = ruleRepository.get(nextRuleName);

            if (nextRule != null) {
                System.out.println("Dynamically selected rule: " + nextRule.getName());

                // Create a list with the selected rule
                List<Rule> selectedRules = new ArrayList<>();
                selectedRules.add(nextRule);

                // Evaluate the selected rule
                List<RuleResult> selectedResults = ruleEngineService.evaluateRules(selectedRules, context);
                RuleResult selectedResult = selectedResults.get(0);

                System.out.println("Result: " + (selectedResult.isTriggered() ? "Triggered" : "Not triggered"));
                if (selectedResult.isTriggered()) {
                    System.out.println("Message: " + selectedResult.getMessage());
                }
            } else {
                System.out.println("No rule found for investment type: " + investmentType);
            }
        } else {
            System.out.println("Could not determine investment type.");
        }
    }

    // ========================================
    // CONFIGURATION METHODS (from ApexAdvancedFeaturesDemoConfig)
    // ========================================

    /**
     * Create investment rules for the demo.
     *
     * @return List of investment rules
     */
    public List<Rule> createInvestmentRules() {
        List<Rule> rules = new ArrayList<>();

        // Rule 1: High-value equity filter
        Rule highValueEquityRule = new Rule(
            "HighValueEquityFilter",
            "#products.?[category == 'Equity' && price > 500]",
            "Filter high-value equity products"
        );
        rules.add(highValueEquityRule);

        // Rule 2: Fixed income availability check
        Rule fixedIncomeRule = new Rule(
            "FixedIncomeAvailability",
            "#products.?[category == 'FixedIncome'].size() > 0",
            "Check if fixed income products are available"
        );
        rules.add(fixedIncomeRule);

        // Rule 3: Inventory validation
        Rule inventoryRule = new Rule(
            "InventoryValidation",
            "#inventory.?[quantity < 10].size() == 0",
            "Validate inventory levels"
        );
        rules.add(inventoryRule);

        return rules;
    }

    /**
     * Create rule result rules for the demo.
     *
     * @return List of rule result rules
     */
    public List<Rule> createRuleResultRules() {
        List<Rule> rules = new ArrayList<>();

        // Rule 1: High-value customer rule
        Rule highValueCustomerRule = new Rule(
            "HighValueCustomerRule",
            "#customer.membershipLevel == 'Platinum' || #customer.membershipLevel == 'Gold'",
            "Identify high-value customers"
        );
        rules.add(highValueCustomerRule);

        // Rule 2: Age-based investment rule
        Rule ageBasedRule = new Rule(
            "AgeBasedInvestmentRule",
            "#customer.age >= 30 && #customer.age <= 50",
            "Age-based investment recommendations"
        );
        rules.add(ageBasedRule);

        // Rule 3: Product preference rule
        Rule preferenceRule = new Rule(
            "ProductPreferenceRule",
            "#customer.preferredCategories.contains('Equity')",
            "Check customer product preferences"
        );
        rules.add(preferenceRule);

        // Rule 4: Investment type determination rule
        Rule investmentTypeRule = new Rule(
            "InvestmentTypeRule",
            "#customer.age < 30 ? 'Aggressive' : (#customer.age > 50 ? 'Conservative' : 'Moderate')",
            "Determine investment type based on customer profile"
        );
        rules.add(investmentTypeRule);

        return rules;
    }

    // ========================================
    // DATA SOURCE IMPLEMENTATION (from ApexAdvancedFeaturesDataProvider)
    // ========================================

    /**
     * Demo data source implementation.
     */
    private static class DemoDataSource implements DataSource {
        private final String name;
        private final String dataType;

        public DemoDataSource(String name, String dataType) {
            this.name = name;
            this.dataType = dataType;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String getDataType() {
            return dataType;
        }

        @Override
        public boolean supportsDataType(String dataType) {
            return this.dataType.equals(dataType);
        }

        @Override
        public <T> T getData(String dataType, Object... parameters) {
            if (!supportsDataType(dataType)) {
                return null;
            }

            return switch (dataType) {
                case "products" -> (T) createProducts();
                case "inventory" -> (T) createInventory();
                case "customer" -> (T) createCustomer();
                case "templateCustomer" -> (T) createTemplateCustomer();
                case "lookupServices" -> (T) createLookupServices();
                case "sourceRecords" -> (T) createSourceRecords();
                default -> null;
            };
        }
    }

    // ========================================
    // DATA CREATION METHODS (from ApexAdvancedFeaturesDataProvider)
    // ========================================

    /**
     * Create sample products for demonstration.
     *
     * @return List of products
     */
    private static List<Product> createProducts() {
        List<Product> products = new ArrayList<>();
        products.add(new Product("Government Bond 10Y", 1000.0, "FixedIncome"));
        products.add(new Product("Corporate Bond AAA", 950.0, "FixedIncome"));
        products.add(new Product("Tech Stock ETF", 750.0, "Equity"));
        products.add(new Product("Blue Chip Index", 1200.0, "Equity"));
        products.add(new Product("Emerging Markets", 450.0, "Equity"));
        products.add(new Product("Real Estate REIT", 300.0, "RealEstate"));
        return products;
    }

    /**
     * Create sample inventory for demonstration.
     *
     * @return List of inventory products
     */
    private static List<Product> createInventory() {
        List<Product> inventory = new ArrayList<>();

        // Create products and set quantity using setters (since Product doesn't have quantity constructor)
        Product bond1 = new Product("Government Bond 10Y", 1000.0, "FixedIncome");
        Product bond2 = new Product("Corporate Bond AAA", 950.0, "FixedIncome");
        Product etf1 = new Product("Tech Stock ETF", 750.0, "Equity");
        Product index1 = new Product("Blue Chip Index", 1200.0, "Equity");
        Product emerging = new Product("Emerging Markets", 450.0, "Equity"); // Low inventory
        Product reit = new Product("Real Estate REIT", 300.0, "RealEstate");

        inventory.add(bond1);
        inventory.add(bond2);
        inventory.add(etf1);
        inventory.add(index1);
        inventory.add(emerging);
        inventory.add(reit);

        return inventory;
    }

    /**
     * Create sample customer for demonstration.
     *
     * @return Customer object
     */
    private static Customer createCustomer() {
        Customer customer = new Customer();
        customer.setName("John Doe");
        customer.setAge(35);
        customer.setMembershipLevel("Gold");
        customer.setPreferredCategories(Arrays.asList("Equity", "FixedIncome"));
        return customer;
    }

    /**
     * Create sample template customer for demonstration.
     *
     * @return Customer object for templates
     */
    private static Customer createTemplateCustomer() {
        Customer customer = new Customer();
        customer.setName("Jane Smith");
        customer.setAge(42);
        customer.setMembershipLevel("Platinum");
        customer.setPreferredCategories(Arrays.asList("RealEstate", "Equity"));
        return customer;
    }

    /**
     * Create sample lookup services for demonstration.
     *
     * @return List of lookup services
     */
    private static List<LookupService> createLookupServices() {
        List<LookupService> services = new ArrayList<>();

        // Create instrument types lookup
        LookupService instrumentTypes = new LookupService("InstrumentTypes",
            Arrays.asList("Equity", "FixedIncome", "RealEstate", "Commodity"));
        services.add(instrumentTypes);

        // Create risk levels lookup
        LookupService riskLevels = new LookupService("RiskLevels",
            Arrays.asList("Low", "Medium", "High"));
        services.add(riskLevels);

        return services;
    }

    /**
     * Create sample source records for demonstration.
     *
     * @return List of source records
     */
    private static List<Map<String, Object>> createSourceRecords() {
        List<Map<String, Object>> records = new ArrayList<>();

        Map<String, Object> record1 = new HashMap<>();
        record1.put("id", "BOND001");
        record1.put("type", "Government");
        record1.put("maturity", "10Y");
        record1.put("yield", 2.5);
        records.add(record1);

        Map<String, Object> record2 = new HashMap<>();
        record2.put("id", "STOCK001");
        record2.put("type", "Technology");
        record2.put("sector", "Software");
        record2.put("pe_ratio", 25.3);
        records.add(record2);

        return records;
    }
}
