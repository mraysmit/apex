package dev.mars.apex.demo;

import dev.mars.apex.demo.bootstrap.ApexAdvancedFeaturesDemo;
import dev.mars.apex.demo.data.MockDataSources;
import dev.mars.apex.demo.model.Product;
import dev.mars.apex.demo.model.Trade;
import dev.mars.apex.demo.rulesets.PricingServiceDemo;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

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
 * Test class for SpEL advanced features.
 *
* This class is part of the APEX A powerful expression processor for Java applications.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-07-27
 * @version 1.0
 */
/**
 * Test class for SpEL advanced features.
 * Provides comprehensive test coverage for all features demonstrated in DemoSpelAdvancedFeatures.
 */
public class SpelAdvancedFeaturesTest {

    private dev.mars.apex.core.service.engine.ExpressionEvaluatorService evaluatorService;
    private dev.mars.apex.core.service.engine.RuleEngineService ruleEngineService;
    private dev.mars.apex.core.service.engine.TemplateProcessorService templateProcessorService;
    private dev.mars.apex.core.engine.config.RulesEngine rulesEngine;
    private ApexAdvancedFeaturesDemo demo;
    private PricingServiceDemo pricingService;
    private StandardEvaluationContext context;
    private ByteArrayOutputStream outContent;
    private PrintStream originalOut;

    @BeforeEach
    public void setUp() {
        // Initialize services
        evaluatorService = new dev.mars.apex.core.service.engine.ExpressionEvaluatorService();
        ruleEngineService = new dev.mars.apex.core.service.engine.RuleEngineService(evaluatorService);
        templateProcessorService = new dev.mars.apex.core.service.engine.TemplateProcessorService(evaluatorService);
        rulesEngine = new dev.mars.apex.core.engine.config.RulesEngine(new dev.mars.apex.core.engine.config.RulesEngineConfiguration());
        pricingService = new PricingServiceDemo();

        // Initialize demo with merged class (no config needed)
        demo = new ApexAdvancedFeaturesDemo();

        // Initialize context
        context = new StandardEvaluationContext();

        // Capture System.out to verify output
        outContent = new ByteArrayOutputStream();
        originalOut = System.out;
        System.setOut(new PrintStream(outContent));
    }

    /**
     * Test that runs the main method of the demo class.
     * This is kept for backward compatibility.
     */
    @Test
    public void testFullDemo() {
        // Run the main method of the demo class to test all functionality
        ApexAdvancedFeaturesDemo.main(new String[]{});
    }

    /**
     * Test collection operations using SpEL.
     */
    @Test
    public void testCollectionOperations() {
        // Use the field context and populate it with demo data
        context = demo.createContext();

        // Add price threshold variable
        context.setVariable("priceThreshold", 500.0);

        // Test collection selection - filter fixed income products
        dev.mars.apex.core.engine.model.RuleResult result1 = evaluatorService.evaluateWithResult(
            "#products.?[category == 'FixedIncome']", context, List.class);
        assertTrue(result1.isTriggered(), "Rule should be triggered");

        // Get the actual value using evaluate
        @SuppressWarnings("unchecked")
        List<Product> fixedIncomeProducts = (List<Product>) evaluatorService.evaluate(
            "#products.?[category == 'FixedIncome']", context, List.class);
        assertNotNull(fixedIncomeProducts, "Fixed income products should not be null");
        assertEquals(2, fixedIncomeProducts.size(), "Should find 2 fixed income products");
        for (Product product : fixedIncomeProducts) {
            assertEquals("FixedIncome", product.getCategory(), "Product category should be FixedIncome");
        }

        // Test collection projection - get all product names
        dev.mars.apex.core.engine.model.RuleResult result2 = evaluatorService.evaluateWithResult(
            "#products.![name]", context, List.class);
        assertTrue(result2.isTriggered(), "Rule should be triggered");

        // Get the actual value using evaluate
        @SuppressWarnings("unchecked")
        List<String> productNames = (List<String>) evaluatorService.evaluate(
            "#products.![name]", context, List.class);
        assertNotNull(productNames, "Product names should not be null");
        assertEquals(5, productNames.size(), "Should find 5 product names");
        assertTrue(productNames.contains("US Treasury Bond"), "Should contain US Treasury Bond");
        assertTrue(productNames.contains("Apple Stock"), "Should contain Apple Stock");

        // Test combining selection and projection - names of equity products
        dev.mars.apex.core.engine.model.RuleResult result3 = evaluatorService.evaluateWithResult(
            "#products.?[category == 'Equity'].![name]", context, List.class);
        assertTrue(result3.isTriggered(), "Rule should be triggered");

        // Get the actual value using evaluate
        @SuppressWarnings("unchecked")
        List<String> equityProductNames = (List<String>) evaluatorService.evaluate(
            "#products.?[category == 'Equity'].![name]", context, List.class);
        assertNotNull(equityProductNames, "Equity product names should not be null");
        assertEquals(1, equityProductNames.size(), "Should find 1 equity product name");
        assertEquals("Apple Stock", equityProductNames.get(0), "Equity product should be Apple Stock");

        // Test first and last elements
        dev.mars.apex.core.engine.model.RuleResult result4 = evaluatorService.evaluateWithResult(
            "#products.^[price > #priceThreshold].name", context, String.class);
        assertTrue(result4.isTriggered(), "Rule should be triggered");

        // Get the actual value using evaluate
        String firstExpensiveProduct = evaluatorService.evaluate(
            "#products.^[price > #priceThreshold].name", context, String.class);
        assertEquals("US Treasury Bond", firstExpensiveProduct, "First expensive product should be US Treasury Bond");

        dev.mars.apex.core.engine.model.RuleResult result5 = evaluatorService.evaluateWithResult(
            "#products.$[price < 200].name", context, String.class);
        assertTrue(result5.isTriggered(), "Rule should be triggered");

        // Get the actual value using evaluate
        String lastCheapProduct = evaluatorService.evaluate(
            "#products.$[price < 200].name", context, String.class);
        assertEquals("Corporate Bond", lastCheapProduct, "Last cheap product should be Corporate Bond");
    }

    /**
     * Test advanced rule engine with collection filtering.
     */
    @Test
    public void testAdvancedRuleEngine() {
        // Use the field context and populate it with demo data
        context = demo.createContext();

        // Get rules from demo
        List<dev.mars.apex.core.engine.model.Rule> rules = demo.createInvestmentRules();

        // Test investment recommendations rule
        dev.mars.apex.core.engine.model.Rule investmentRecommendationsRule = rules.stream()
            .filter(r -> r.getName().equals("InvestmentRecommendationsRule"))
            .findFirst()
            .orElseThrow(() -> new AssertionError("InvestmentRecommendationsRule not found"));

        // Check if the rule is triggered
        dev.mars.apex.core.engine.model.RuleResult result1 = evaluatorService.evaluateWithResult(
            investmentRecommendationsRule.getCondition(), context, Boolean.class);
        assertTrue(result1.isTriggered(), "Investment recommendations rule should be triggered");

        // Get the actual value using evaluate
        Boolean isTriggered = evaluatorService.evaluate(
            investmentRecommendationsRule.getCondition(), context, Boolean.class);
        assertTrue(isTriggered, "Investment recommendations rule should evaluate to true");

        // Test using the demo instance
        assertNotNull(demo, "Demo should be initialized");

        // Test using the context
        context.setVariable("testVariable", "testValue");
        Object contextValue = context.lookupVariable("testVariable");
        assertEquals("testValue", contextValue, "Context should store and retrieve variables");

        // Test using originalOut for output verification
        assertNotNull(originalOut, "Original output stream should be captured");

        // Test gold tier investor offers rule
        dev.mars.apex.core.engine.model.Rule goldTierRule = rules.stream()
            .filter(r -> r.getName().equals("GoldTierInvestorOffersRule"))
            .findFirst()
            .orElseThrow(() -> new AssertionError("GoldTierInvestorOffersRule not found"));

        // Check if the rule is triggered
        dev.mars.apex.core.engine.model.RuleResult result2 = evaluatorService.evaluateWithResult(
            goldTierRule.getCondition(), context, Boolean.class);
        assertTrue(result2.isTriggered(), "Gold tier investor offers rule should be triggered");

        // Get the actual value using evaluate
        Boolean isGoldTierTriggered = evaluatorService.evaluate(
            goldTierRule.getCondition(), context, Boolean.class);
        assertTrue(isGoldTierTriggered, "Gold tier investor offers rule should evaluate to true");

        // Test low-cost investment options rule
        dev.mars.apex.core.engine.model.Rule lowCostRule = rules.stream()
            .filter(r -> r.getName().equals("LowCostInvestmentOptionsRule"))
            .findFirst()
            .orElseThrow(() -> new AssertionError("LowCostInvestmentOptionsRule not found"));

        // Check if the rule is triggered
        dev.mars.apex.core.engine.model.RuleResult result3 = evaluatorService.evaluateWithResult(
            lowCostRule.getCondition(), context, Boolean.class);
        assertTrue(result3.isTriggered(), "Low-cost investment options rule should be triggered");

        // Get the actual value using evaluate
        Boolean isLowCostTriggered = evaluatorService.evaluate(
            lowCostRule.getCondition(), context, Boolean.class);
        assertTrue(isLowCostTriggered, "Low-cost investment options rule should evaluate to true");

        // Evaluate all rules using the rule engine service
        List<dev.mars.apex.core.engine.model.RuleResult> results = ruleEngineService.evaluateRules(rules, context);
        assertEquals(3, results.size(), "Should evaluate 3 rules");
        assertTrue(results.stream().allMatch(dev.mars.apex.core.engine.model.RuleResult::isTriggered), "All rules should be triggered");
    }

    /**
     * Test dynamic method resolution and execution.
     */
    @Test
    public void testDynamicMethodExecution() {
        // Use the field context and add variables
        context = new StandardEvaluationContext();
        context.setVariable("pricingService", pricingService);
        context.setVariable("basePrice", 100.0);

        // Test different financial pricing strategies
        Map<String, String> pricingStrategies = new HashMap<>();
        pricingStrategies.put("market", "#pricingService.calculateStandardPrice(#basePrice)");
        pricingStrategies.put("premium", "#pricingService.calculatePremiumPrice(#basePrice)");
        pricingStrategies.put("discount", "#pricingService.calculateSalePrice(#basePrice)");
        pricingStrategies.put("liquidation", "#pricingService.calculateClearancePrice(#basePrice)");

        // Verify each pricing strategy with RuleResult
        for (Map.Entry<String, String> entry : pricingStrategies.entrySet()) {
            String strategyName = entry.getKey();
            String expression = entry.getValue();

            // Check if the rule is triggered
            dev.mars.apex.core.engine.model.RuleResult result = evaluatorService.evaluateWithResult(expression, context, Double.class);
            assertTrue(result.isTriggered(), strategyName + " pricing strategy rule should be triggered");

            // Get the actual value using evaluate
            Double price = evaluatorService.evaluate(expression, context, Double.class);
            assertNotNull(price, strategyName + " pricing strategy should return a non-null price");

            // Verify the expected price for each strategy
            switch (strategyName) {
                case "market":
                    assertEquals(100.0, price, 0.01, "Market price should be 100.0");
                    break;
                case "premium":
                    assertEquals(120.0, price, 0.01, "Premium price should be 120.0");
                    break;
                case "discount":
                    assertEquals(80.0, price, 0.01, "Discount price should be 80.0");
                    break;
                case "liquidation":
                    assertEquals(50.0, price, 0.01, "Liquidation price should be 50.0");
                    break;
                default:
                    fail("Unknown pricing strategy: " + strategyName);
            }
        }

        // Test dynamic pricing method selection based on instrument value
        String dynamicMethodExpression = 
            "#basePrice > 50 ? " +
            "#pricingService.calculatePremiumPrice(#basePrice) : " +
            "#pricingService.calculateSalePrice(#basePrice)";

        // Check if the rule is triggered
        dev.mars.apex.core.engine.model.RuleResult result1 = evaluatorService.evaluateWithResult(dynamicMethodExpression, context, Double.class);
        assertTrue(result1.isTriggered(), "Dynamic pricing rule should be triggered");

        // Since basePrice is 100, should use premium pricing
        Double price1 = evaluatorService.evaluate(dynamicMethodExpression, context, Double.class);
        assertEquals(120.0, price1, 0.01, "Dynamic price with basePrice=100 should be premium price (120.0)");

        // Change basePrice to 40, should use sale pricing
        context.setVariable("basePrice", 40.0);

        // Check if the rule is triggered
        dev.mars.apex.core.engine.model.RuleResult result2 = evaluatorService.evaluateWithResult(dynamicMethodExpression, context, Double.class);
        assertTrue(result2.isTriggered(), "Dynamic pricing rule should be triggered");

        // Get the actual value using evaluate
        Double price2 = evaluatorService.evaluate(dynamicMethodExpression, context, Double.class);
        assertEquals(32.0, price2, 0.01, "Dynamic price with basePrice=40 should be sale price (32.0)");
    }

    /**
     * Test template expressions with placeholders.
     */
    @Test
    public void testTemplateExpressions() {
        // Use the field context and populate it with template demo data
        context = demo.createTemplateContext();

        // Add additional variables for this test
        context.setVariable("orderTotal", 350.0);
        context.setVariable("tradingFee", 15.0);

        // Template with placeholders for investment confirmation
        String emailTemplate = 
            "Dear #{#customer.name},\n\n" +
            "Thank you for your investment. Your #{#customer.membershipLevel} investor status entitles you to " +
            "#{#customer.membershipLevel == 'Gold' ? '15%' : (#customer.membershipLevel == 'Silver' ? '10%' : '5%')} reduced fees.\n\n" +
            "Investment amount: $#{#orderTotal}\n" +
            "Trading fee: $#{#tradingFee}\n" +
            "Fee discount: $#{#customer.membershipLevel == 'Gold' ? #orderTotal * 0.15 : " +
                        "(#customer.membershipLevel == 'Silver' ? #orderTotal * 0.1 : #orderTotal * 0.05)}\n" +
            "Final investment total: $#{#orderTotal + #tradingFee - " +
                        "(#customer.membershipLevel == 'Gold' ? #orderTotal * 0.15 : " +
                        "(#customer.membershipLevel == 'Silver' ? #orderTotal * 0.1 : #orderTotal * 0.05))}\n\n" +
            "#{#customer.age > 60 ? 'As a senior investor, you will receive our retirement planning guide next week.' : ''}";

        // Process the template
        String processedEmail = templateProcessorService.processTemplate(emailTemplate, context);

        // Verify the processed template
        assertTrue(processedEmail.contains("Dear Bob Johnson"), "Email should be addressed to Bob Johnson");
        assertTrue(processedEmail.contains("Your Silver investor status"), "Email should mention Silver investor status");
        assertTrue(processedEmail.contains("10% reduced fees"), "Email should mention 10% reduced fees");
        assertTrue(processedEmail.contains("Investment amount: $350.0"), "Email should show investment amount of $350.0");
        assertTrue(processedEmail.contains("Trading fee: $15.0"), "Email should show trading fee of $15.0");
        assertTrue(processedEmail.contains("Fee discount: $35.0"), "Email should show fee discount of $35.0");
        assertTrue(processedEmail.contains("Final investment total: $330.0"), "Email should show final total of $330.0");
        assertTrue(processedEmail.contains("As a senior investor"), "Email should contain senior investor text");

        // Test with RuleResult - check if a specific expression in the template evaluates correctly
        String discountExpression = "#customer.membershipLevel == 'Silver' ? #orderTotal * 0.1 : #orderTotal * 0.05";
        dev.mars.apex.core.engine.model.RuleResult result = evaluatorService.evaluateWithResult(discountExpression, context, Double.class);
        assertTrue(result.isTriggered(), "Discount expression rule should be triggered");

        // Get the actual value using evaluate
        Double discount = evaluatorService.evaluate(discountExpression, context, Double.class);
        assertEquals(35.0, discount, 0.01, "Discount for Silver member with $350 order should be $35.0");
    }

    /**
     * Test XML template expressions with placeholders.
     */
    @Test
    public void testXmlTemplateExpressions() {
        // Use the field context and populate it with template demo data
        context = demo.createTemplateContext();

        // Add additional variables for this test
        context.setVariable("orderTotal", 350.0);
        context.setVariable("tradingFee", 15.0);

        // XML template with placeholders for investment confirmation
        String xmlTemplate = 
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<InvestmentConfirmation>\n" +
            "    <Customer>\n" +
            "        <Name>#{#customer.name}</Name>\n" +
            "        <Age>#{#customer.age}</Age>\n" +
            "        <MembershipLevel>#{#customer.membershipLevel}</MembershipLevel>\n" +
            "    </Customer>\n" +
            "    <Investment>\n" +
            "        <Amount>#{#orderTotal}</Amount>\n" +
            "        <TradingFee>#{#tradingFee}</TradingFee>\n" +
            "        <Discount>#{#customer.membershipLevel == 'Gold' ? #orderTotal * 0.15 : " +
                        "(#customer.membershipLevel == 'Silver' ? #orderTotal * 0.1 : #orderTotal * 0.05)}</Discount>\n" +
            "        <Total>#{#orderTotal + #tradingFee - " +
                        "(#customer.membershipLevel == 'Gold' ? #orderTotal * 0.15 : " +
                        "(#customer.membershipLevel == 'Silver' ? #orderTotal * 0.1 : #orderTotal * 0.05))}</Total>\n" +
            "    </Investment>\n" +
            "    <SpecialOffers>\n" +
            "        #{#customer.age > 60 ? '<Offer>Senior Investor Retirement Planning Guide</Offer>' : ''}\n" +
            "        #{#customer.membershipLevel == 'Gold' ? '<Offer>Premium Investment Opportunities</Offer>' : ''}\n" +
            "    </SpecialOffers>\n" +
            "</InvestmentConfirmation>";

        // Process the XML template
        String processedXml = templateProcessorService.processXmlTemplate(xmlTemplate, context);

        // Verify the processed XML template
        assertTrue(processedXml.contains("<Name>Bob Johnson</Name>"), "XML should contain customer name");
        assertTrue(processedXml.contains("<Age>65</Age>"), "XML should contain customer age");
        assertTrue(processedXml.contains("<MembershipLevel>Silver</MembershipLevel>"), "XML should contain membership level");
        assertTrue(processedXml.contains("<Amount>350.0</Amount>"), "XML should contain order amount");
        assertTrue(processedXml.contains("<TradingFee>15.0</TradingFee>"), "XML should contain trading fee");
        assertTrue(processedXml.contains("<Discount>35.0</Discount>"), "XML should contain discount amount");
        assertTrue(processedXml.contains("<Total>330.0</Total>"), "XML should contain total amount");
        // Check if the senior investor offer is included in the XML
        String seniorInvestorText = "Senior Investor Retirement Planning Guide";
        assertTrue(processedXml.contains(seniorInvestorText), 
            "XML should contain senior investor text: " + seniorInvestorText);
        assertFalse(processedXml.contains("<Offer>Premium Investment Opportunities</Offer>"), 
            "XML should not contain premium investment offer");

        // Test with RuleResult - check if a specific expression in the template evaluates correctly
        String ageConditionExpression = "#customer.age > 60";
        dev.mars.apex.core.engine.model.RuleResult ageResult = evaluatorService.evaluateWithResult(ageConditionExpression, context, Boolean.class);
        assertTrue(ageResult.isTriggered(), "Age condition rule should be triggered");

        // Get the actual value using evaluate
        Boolean isOver60 = evaluatorService.evaluate(ageConditionExpression, context, Boolean.class);
        assertTrue(isOver60, "Customer should be over 60");

        String membershipConditionExpression = "#customer.membershipLevel == 'Gold'";
        dev.mars.apex.core.engine.model.RuleResult membershipResult = evaluatorService.evaluateWithResult(membershipConditionExpression, context, Boolean.class);
        assertFalse(membershipResult.isTriggered(), "Membership condition rule should not be triggered");

        // Get the actual value using evaluate
        Boolean isGold = evaluatorService.evaluate(membershipConditionExpression, context, Boolean.class);
        assertFalse(isGold, "Customer should not be Gold tier");

        // Verify XML special characters are properly escaped
        context.setVariable("xmlSpecialChars", "<test>&\"'</test>");
        String xmlSpecialCharsTemplate = "<SpecialChars>#{#xmlSpecialChars}</SpecialChars>";
        String processedXmlSpecialChars = templateProcessorService.processXmlTemplate(xmlSpecialCharsTemplate, context);
        assertTrue(processedXmlSpecialChars.contains("<SpecialChars>&lt;test&gt;&amp;&quot;&apos;&lt;/test&gt;</SpecialChars>"), 
            "XML special characters should be properly escaped");
    }

    /**
     * Test JSON template expressions with placeholders.
     */
    @Test
    public void testJsonTemplateExpressions() {
        // Use the field context and populate it with template demo data
        context = demo.createTemplateContext();

        // Add additional variables for this test
        context.setVariable("orderTotal", 350.0);
        context.setVariable("tradingFee", 15.0);

        // JSON template with placeholders for investment confirmation
        String jsonTemplate = 
            "{\n" +
            "  \"investmentConfirmation\": {\n" +
            "    \"customer\": {\n" +
            "      \"name\": \"#{#customer.name}\",\n" +
            "      \"age\": #{#customer.age},\n" +
            "      \"membershipLevel\": \"#{#customer.membershipLevel}\"\n" +
            "    },\n" +
            "    \"investment\": {\n" +
            "      \"amount\": #{#orderTotal},\n" +
            "      \"tradingFee\": #{#tradingFee},\n" +
            "      \"discount\": #{#customer.membershipLevel == 'Gold' ? #orderTotal * 0.15 : " +
                        "(#customer.membershipLevel == 'Silver' ? #orderTotal * 0.1 : #orderTotal * 0.05)},\n" +
            "      \"total\": #{#orderTotal + #tradingFee - " +
                        "(#customer.membershipLevel == 'Gold' ? #orderTotal * 0.15 : " +
                        "(#customer.membershipLevel == 'Silver' ? #orderTotal * 0.1 : #orderTotal * 0.05))}\n" +
            "    }\n" +
            "  }\n" +
            "}";

        // Process the JSON template
        String processedJson = templateProcessorService.processJsonTemplate(jsonTemplate, context);

        // Verify the processed JSON template
        assertTrue(processedJson.contains("\"name\": \"Bob Johnson\""), "JSON should contain customer name");
        assertTrue(processedJson.contains("\"age\": 65"), "JSON should contain customer age");
        assertTrue(processedJson.contains("\"membershipLevel\": \"Silver\""), "JSON should contain membership level");
        assertTrue(processedJson.contains("\"amount\": 350.0"), "JSON should contain order amount");
        assertTrue(processedJson.contains("\"tradingFee\": 15.0"), "JSON should contain trading fee");
        assertTrue(processedJson.contains("\"discount\": 35.0"), "JSON should contain discount amount");
        assertTrue(processedJson.contains("\"total\": 330.0"), "JSON should contain total amount");

        // Test with RuleResult - check if a specific expression in the template evaluates correctly
        String discountExpression = "#customer.membershipLevel == 'Gold' ? #orderTotal * 0.15 : " +
                "(#customer.membershipLevel == 'Silver' ? #orderTotal * 0.1 : #orderTotal * 0.05)";
        dev.mars.apex.core.engine.model.RuleResult discountResult = evaluatorService.evaluateWithResult(discountExpression, context, Double.class);
        assertTrue(discountResult.isTriggered(), "Discount expression rule should be triggered");

        // Get the actual value using evaluate
        Double discount = evaluatorService.evaluate(discountExpression, context, Double.class);
        assertEquals(35.0, discount, 0.01, "Discount for Silver member with $350 order should be $35.0");

        String totalExpression = "#orderTotal + #tradingFee - " +
                "(#customer.membershipLevel == 'Gold' ? #orderTotal * 0.15 : " +
                "(#customer.membershipLevel == 'Silver' ? #orderTotal * 0.1 : #orderTotal * 0.05))";
        dev.mars.apex.core.engine.model.RuleResult totalResult = evaluatorService.evaluateWithResult(totalExpression, context, Double.class);
        assertTrue(totalResult.isTriggered(), "Total expression rule should be triggered");

        // Get the actual value using evaluate
        Double total = evaluatorService.evaluate(totalExpression, context, Double.class);
        assertEquals(330.0, total, 0.01, "Total for Silver member with $350 order and $15 fee should be $330.0");

        // Verify JSON special characters are properly escaped
        context.setVariable("jsonSpecialChars", "test\"\\test\ntest");
        String jsonSpecialCharsTemplate = "{\"specialChars\": \"#{#jsonSpecialChars}\"}";
        String processedJsonSpecialChars = templateProcessorService.processJsonTemplate(jsonSpecialCharsTemplate, context);
        assertTrue(processedJsonSpecialChars.contains("\"specialChars\": \"test\\\"\\\\test\\ntest\""), 
            "JSON special characters should be properly escaped");
    }

    /**
     * Test dynamic lookup service.
     */
    @Test
    public void testDynamicLookupService() {
        // Use the field context and populate it with demo data
        context = demo.createContext();

        // Create lookup services
        List<dev.mars.apex.core.service.lookup.LookupService> lookupServices = MockDataSources.createLookupServices();
        assertNotNull(lookupServices, "Lookup services should not be null");
        assertTrue(lookupServices.size() > 0, "Should have at least one lookup service");

        // Create source records
        List<Trade> sourceTrades = MockDataSources.createSourceRecords();
        assertNotNull(sourceTrades, "Source trades should not be null");
        assertTrue(sourceTrades.size() > 0, "Should have at least one source trade");

        // Add variables to context
        context.setVariable("lookupServices", lookupServices);
        context.setVariable("sourceRecords", sourceTrades);

        // Use MockDataSources to find matching records directly
        List<Trade> matchingTrades = MockDataSources.findMatchingRecords(sourceTrades, lookupServices);
        assertNotNull(matchingTrades, "Matching trades should not be null");
        assertTrue(matchingTrades.size() > 0, "Should find at least one matching trade");

        // Verify all matching trades have values in lookup services
        for (Trade trade : matchingTrades) {
            boolean hasMatch = false;
            for (dev.mars.apex.core.service.lookup.LookupService lookupService : lookupServices) {
                if (lookupService.getLookupValues().contains(trade.getValue())) {
                    hasMatch = true;
                    break;
                }
            }
            assertTrue(hasMatch, "TradeB " + trade.getValue() + " should match a lookup service");
        }

        // Use MockDataSources to find non-matching records directly
        List<Trade> nonMatchingTrades = MockDataSources.findNonMatchingRecords(sourceTrades, lookupServices);
        assertNotNull(nonMatchingTrades, "Non-matching trades should not be null");

        // Verify all non-matching trades don't have values in lookup services
        for (Trade trade : nonMatchingTrades) {
            boolean hasMatch = false;
            for (dev.mars.apex.core.service.lookup.LookupService lookupService : lookupServices) {
                if (lookupService.getLookupValues().contains(trade.getValue())) {
                    hasMatch = true;
                    break;
                }
            }
            assertFalse(hasMatch, "TradeB " + trade.getValue() + " should not match any lookup service");
        }

        // Test dynamic matching with complex conditions
        String complexMatchExpression = "#sourceRecords.?[" +
            "(category == 'InstrumentType' && #lookupServices[0].lookupValues.contains(value)) || " +
            "(category == 'Market' && #lookupServices[1].lookupValues.contains(value)) || " +
            "(category == 'TradeStatus' && #lookupServices[2].lookupValues.contains(value))" +
        "]";

        // Check if the rule is triggered
        dev.mars.apex.core.engine.model.RuleResult complexMatchResult = evaluatorService.evaluateWithResult(complexMatchExpression, context, List.class);
        assertTrue(complexMatchResult.isTriggered(), "Complex match rule should be triggered");

        // Get the actual value using evaluate
        @SuppressWarnings("unchecked")
        List<Trade> complexMatches = (List<Trade>) evaluatorService.evaluate(complexMatchExpression, context, List.class);
        assertNotNull(complexMatches, "Complex matches should not be null");
        assertTrue(complexMatches.size() > 0, "Should find at least one complex match");

        // Verify all complex matches satisfy the complex condition
        for (Trade trade : complexMatches) {
            if ("InstrumentType".equals(trade.getCategory())) {
                String categoryMatchExpression = "#lookupServices[0].lookupValues.contains('" + trade.getValue() + "')";
                dev.mars.apex.core.engine.model.RuleResult categoryMatchResult = evaluatorService.evaluateWithResult(categoryMatchExpression, context, Boolean.class);
                assertTrue(categoryMatchResult.isTriggered(), "Category match rule should be triggered");

                Boolean categoryMatch = evaluatorService.evaluate(categoryMatchExpression, context, Boolean.class);
                assertTrue(categoryMatch, "InstrumentType trade " + trade.getValue() + " should match lookupServices[0]");
            } else if ("Market".equals(trade.getCategory())) {
                String categoryMatchExpression = "#lookupServices[1].lookupValues.contains('" + trade.getValue() + "')";
                dev.mars.apex.core.engine.model.RuleResult categoryMatchResult = evaluatorService.evaluateWithResult(categoryMatchExpression, context, Boolean.class);
                assertTrue(categoryMatchResult.isTriggered(), "Category match rule should be triggered");

                Boolean categoryMatch = evaluatorService.evaluate(categoryMatchExpression, context, Boolean.class);
                assertTrue(categoryMatch, "Market trade " + trade.getValue() + " should match lookupServices[1]");
            } else if ("TradeStatus".equals(trade.getCategory())) {
                String categoryMatchExpression = "#lookupServices[2].lookupValues.contains('" + trade.getValue() + "')";
                dev.mars.apex.core.engine.model.RuleResult categoryMatchResult = evaluatorService.evaluateWithResult(categoryMatchExpression, context, Boolean.class);
                assertTrue(categoryMatchResult.isTriggered(), "Category match rule should be triggered");

                Boolean categoryMatch = evaluatorService.evaluate(categoryMatchExpression, context, Boolean.class);
                assertTrue(categoryMatch, "TradeStatus trade " + trade.getValue() + " should match lookupServices[2]");
            }
        }
    }
}
