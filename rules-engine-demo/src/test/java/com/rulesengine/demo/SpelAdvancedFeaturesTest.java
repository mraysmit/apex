package com.rulesengine.demo;

import com.rulesengine.demo.integration.RuleConfigurationDemo;
import com.rulesengine.core.engine.model.Rule;
import com.rulesengine.demo.integration.SpelAdvancedFeaturesDemo;
import com.rulesengine.demo.model.Customer;
import com.rulesengine.demo.model.Product;
import com.rulesengine.demo.model.Trade;
import com.rulesengine.core.service.engine.RuleEngineService;
import com.rulesengine.core.service.engine.TemplateProcessorService;
import com.rulesengine.core.service.engine.ExpressionEvaluatorService;
import com.rulesengine.core.service.lookup.LookupService;
import com.rulesengine.demo.service.PricingServiceDemo;
import com.rulesengine.demo.data.MockDataSources;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for SpEL advanced features.
 * Provides comprehensive test coverage for all features demonstrated in DemoSpelAdvancedFeatures.
 */
public class SpelAdvancedFeaturesTest {

    private ExpressionEvaluatorService evaluatorService;
    private RuleEngineService ruleEngineService;
    private TemplateProcessorService templateProcessorService;
    private PricingServiceDemo pricingService;
    private StandardEvaluationContext context;
    private ByteArrayOutputStream outContent;
    private PrintStream originalOut;

    @BeforeEach
    public void setUp() {
        // Initialize services
        evaluatorService = new ExpressionEvaluatorService();
        ruleEngineService = new RuleEngineService(evaluatorService);
        templateProcessorService = new TemplateProcessorService(evaluatorService);
        pricingService = new PricingServiceDemo();

        // Initialize context
        context = new StandardEvaluationContext();

        // Capture System.out to verify output
        outContent = new ByteArrayOutputStream();
        originalOut = System.out;
        System.setOut(new PrintStream(outContent));
    }

    /**
     * Original test that runs the main method of the demo class.
     * This is kept for backward compatibility.
     */
    @Test
    public void testFullDemo() {
        // Run the main method of the demo class to test all functionality
        SpelAdvancedFeaturesDemo.main(new String[]{});
    }

    /**
     * Test collection operations using SpEL.
     */
    @Test
    public void testCollectionOperations() {
        // Get products from data service
        List<Product> products = MockDataSources.getProducts();
        context.setVariable("products", products);

        // Test collection selection - filter fixed income products
        List<Product> fixedIncomeProducts = evaluatorService.evaluateQuietly(
            "#products.?[category == 'FixedIncome']", context, List.class);
        assertNotNull(fixedIncomeProducts);
        assertEquals(2, fixedIncomeProducts.size());
        for (Product product : fixedIncomeProducts) {
            assertEquals("FixedIncome", product.getCategory());
        }

        // Test collection projection - get all product names
        List<String> productNames = evaluatorService.evaluateQuietly(
            "#products.![name]", context, List.class);
        assertNotNull(productNames);
        assertEquals(5, productNames.size());
        assertTrue(productNames.contains("US Treasury Bond"));
        assertTrue(productNames.contains("Apple Stock"));

        // Test combining selection and projection - names of equity products
        List<String> equityProductNames = evaluatorService.evaluateQuietly(
            "#products.?[category == 'Equity'].![name]", context, List.class);
        assertNotNull(equityProductNames);
        assertEquals(1, equityProductNames.size());
        assertEquals("Apple Stock", equityProductNames.get(0));

        // Test first and last elements
        context.setVariable("priceThreshold", 500.0);
        String firstExpensiveProduct = evaluatorService.evaluateQuietly(
            "#products.^[price > #priceThreshold].name", context, String.class);
        assertEquals("US Treasury Bond", firstExpensiveProduct);

        String lastCheapProduct = evaluatorService.evaluateQuietly(
            "#products.$[price < 200].name", context, String.class);
        assertEquals("Corporate Bond", lastCheapProduct);
    }

    /**
     * Test advanced rule engine with collection filtering.
     */
    @Test
    public void testAdvancedRuleEngine() {
        // Get inventory and customer from data service
        List<Product> inventory = MockDataSources.getInventory();
        Customer customer = MockDataSources.getCustomer();

        // Create context with variables
        context.setVariable("inventory", inventory);
        context.setVariable("customer", customer);

        // Test investment recommendations rule
        Rule investmentRecommendationsRule = RuleConfigurationDemo.createInvestmentRecommendationsRule();
        List<Product> recommendedProducts = evaluatorService.evaluateQuietly(
            investmentRecommendationsRule.getCondition(), context, List.class);
        assertNotNull(recommendedProducts);
        assertTrue(recommendedProducts.size() > 0);
        for (Product product : recommendedProducts) {
            assertTrue(customer.getPreferredCategories().contains(product.getCategory()));
        }

        // Test gold tier investor offers rule
        Rule goldTierRule = RuleConfigurationDemo.createGoldTierInvestorOffersRule();
        List<String> goldTierOffers = evaluatorService.evaluateQuietly(
            goldTierRule.getCondition(), context, List.class);
        assertNotNull(goldTierOffers);
        assertTrue(goldTierOffers.size() > 0);
        // Since customer is Gold tier, offers should include discount
        for (String offer : goldTierOffers) {
            assertTrue(offer.contains("discount"));
        }

        // Test low-cost investment options rule
        Rule lowCostRule = RuleConfigurationDemo.createLowCostInvestmentOptionsRule();
        List<String> lowCostOptions = evaluatorService.evaluateQuietly(
            lowCostRule.getCondition(), context, List.class);
        assertNotNull(lowCostOptions);
        assertTrue(lowCostOptions.size() > 0);
        // Verify format of low-cost options
        for (String option : lowCostOptions) {
            assertTrue(option.contains(" - $"));
        }
    }

    /**
     * Test dynamic method resolution and execution.
     */
    @Test
    public void testDynamicMethodExecution() {
        // Create context with variables
        context.setVariable("service", pricingService);
        context.setVariable("basePrice", 100.0);

        // Test different financial pricing strategies
        Map<String, String> pricingStrategies = new HashMap<>();
        pricingStrategies.put("market", "#service.calculateStandardPrice(#basePrice)");
        pricingStrategies.put("premium", "#service.calculatePremiumPrice(#basePrice)");
        pricingStrategies.put("discount", "#service.calculateSalePrice(#basePrice)");
        pricingStrategies.put("liquidation", "#service.calculateClearancePrice(#basePrice)");

        // Verify each pricing strategy
        assertEquals(100.0, evaluatorService.evaluateQuietly(pricingStrategies.get("market"), context, Double.class));
        assertEquals(120.0, evaluatorService.evaluateQuietly(pricingStrategies.get("premium"), context, Double.class));
        assertEquals(80.0, evaluatorService.evaluateQuietly(pricingStrategies.get("discount"), context, Double.class));
        assertEquals(50.0, evaluatorService.evaluateQuietly(pricingStrategies.get("liquidation"), context, Double.class));

        // Test dynamic pricing method selection based on instrument value
        String dynamicMethodExpression = 
            "#basePrice > 50 ? " +
            "#service.calculatePremiumPrice(#basePrice) : " +
            "#service.calculateSalePrice(#basePrice)";

        // Since basePrice is 100, should use premium pricing
        assertEquals(120.0, evaluatorService.evaluateQuietly(dynamicMethodExpression, context, Double.class));

        // Change basePrice to 40, should use sale pricing
        context.setVariable("basePrice", 40.0);
        assertEquals(32.0, evaluatorService.evaluateQuietly(dynamicMethodExpression, context, Double.class));
    }

    /**
     * Test template expressions with placeholders.
     */
    @Test
    public void testTemplateExpressions() {
        // Create a context with variables for financial services
        context.setVariable("customer", MockDataSources.getTemplateCustomer());
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
        assertTrue(processedEmail.contains("Dear Bob Johnson"));
        assertTrue(processedEmail.contains("Your Silver investor status"));
        assertTrue(processedEmail.contains("10% reduced fees"));
        assertTrue(processedEmail.contains("Investment amount: $350.0"));
        assertTrue(processedEmail.contains("Trading fee: $15.0"));
        assertTrue(processedEmail.contains("Fee discount: $35.0"));
        assertTrue(processedEmail.contains("Final investment total: $330.0"));
        assertFalse(processedEmail.contains("As a senior investor")); // Bob is 42, not over 60
    }

    /**
     * Test XML template expressions with placeholders.
     */
    @Test
    public void testXmlTemplateExpressions() {
        // Create a context with variables for financial services
        context.setVariable("customer", MockDataSources.getTemplateCustomer());
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
        assertTrue(processedXml.contains("<Name>Bob Johnson</Name>"));
        assertTrue(processedXml.contains("<Age>42</Age>"));
        assertTrue(processedXml.contains("<MembershipLevel>Silver</MembershipLevel>"));
        assertTrue(processedXml.contains("<Amount>350.0</Amount>"));
        assertTrue(processedXml.contains("<TradingFee>15.0</TradingFee>"));
        assertTrue(processedXml.contains("<Discount>35.0</Discount>"));
        assertTrue(processedXml.contains("<Total>330.0</Total>"));
        assertFalse(processedXml.contains("<Offer>Senior Investor Retirement Planning Guide</Offer>")); // Bob is 42, not over 60
        assertFalse(processedXml.contains("<Offer>Premium Investment Opportunities</Offer>")); // Bob is Silver, not Gold

        // Verify XML special characters are properly escaped
        context.setVariable("xmlSpecialChars", "<test>&\"'</test>");
        String xmlSpecialCharsTemplate = "<SpecialChars>#{#xmlSpecialChars}</SpecialChars>";
        String processedXmlSpecialChars = templateProcessorService.processXmlTemplate(xmlSpecialCharsTemplate, context);
        assertTrue(processedXmlSpecialChars.contains("<SpecialChars>&lt;test&gt;&amp;&quot;&apos;&lt;/test&gt;</SpecialChars>"));
    }

    /**
     * Test JSON template expressions with placeholders.
     */
    @Test
    public void testJsonTemplateExpressions() {
        // Create a context with variables for financial services
        context.setVariable("customer", MockDataSources.getTemplateCustomer());
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
        assertTrue(processedJson.contains("\"name\": \"Bob Johnson\""));
        assertTrue(processedJson.contains("\"age\": 42"));
        assertTrue(processedJson.contains("\"membershipLevel\": \"Silver\""));
        assertTrue(processedJson.contains("\"amount\": 350.0"));
        assertTrue(processedJson.contains("\"tradingFee\": 15.0"));
        assertTrue(processedJson.contains("\"discount\": 35.0"));
        assertTrue(processedJson.contains("\"total\": 330.0"));

        // Verify JSON special characters are properly escaped
        context.setVariable("jsonSpecialChars", "test\"\\test\ntest");
        String jsonSpecialCharsTemplate = "{\"specialChars\": \"#{#jsonSpecialChars}\"}";
        String processedJsonSpecialChars = templateProcessorService.processJsonTemplate(jsonSpecialCharsTemplate, context);
        assertTrue(processedJsonSpecialChars.contains("\"specialChars\": \"test\\\"\\\\test\\ntest\""));
    }

    /**
     * Test dynamic lookup service.
     */
    @Test
    public void testDynamicLookupService() {
        // Create lookup services and source records
        List<LookupService> lookupServices = MockDataSources.createLookupServices();
        List<Trade> sourceTrades = MockDataSources.createSourceRecords();

        // Test finding matching records
        List<Trade> matchingTrades = MockDataSources.findMatchingRecords(sourceTrades, lookupServices);
        assertNotNull(matchingTrades);
        assertTrue(matchingTrades.size() > 0);
        // Verify all matching trades have values in lookup services
        for (Trade trade : matchingTrades) {
            boolean hasMatch = false;
            for (LookupService lookupService : lookupServices) {
                if (lookupService.getLookupValues().contains(trade.getValue())) {
                    hasMatch = true;
                    break;
                }
            }
            assertTrue(hasMatch);
        }

        // Test finding non-matching records
        List<Trade> nonMatchingTrades = MockDataSources.findNonMatchingRecords(sourceTrades, lookupServices);
        assertNotNull(nonMatchingTrades);
        // Verify all non-matching trades don't have values in lookup services
        for (Trade trade : nonMatchingTrades) {
            boolean hasMatch = false;
            for (LookupService lookupService : lookupServices) {
                if (lookupService.getLookupValues().contains(trade.getValue())) {
                    hasMatch = true;
                    break;
                }
            }
            assertFalse(hasMatch);
        }

        // Test dynamic matching with complex conditions
        context.setVariable("lookupServices", lookupServices);
        context.setVariable("sourceRecords", sourceTrades);

        String complexMatchExpression = "#sourceRecords.?[" +
            "(category == 'InstrumentType' && #lookupServices[0].lookupValues.contains(value)) || " +
            "(category == 'Market' && #lookupServices[1].lookupValues.contains(value)) || " +
            "(category == 'TradeStatus' && #lookupServices[2].lookupValues.contains(value))" +
        "]";

        List<Trade> complexMatches = evaluatorService.evaluateQuietly(complexMatchExpression, context, List.class);
        assertNotNull(complexMatches);
        assertTrue(complexMatches.size() > 0);

        // Verify all complex matches satisfy the complex condition
        for (Trade trade : complexMatches) {
            if ("InstrumentType".equals(trade.getCategory())) {
                assertTrue(lookupServices.get(0).getLookupValues().contains(trade.getValue()));
            } else if ("Market".equals(trade.getCategory())) {
                assertTrue(lookupServices.get(1).getLookupValues().contains(trade.getValue()));
            } else if ("TradeStatus".equals(trade.getCategory())) {
                assertTrue(lookupServices.get(2).getLookupValues().contains(trade.getValue()));
            }
        }
    }
}
