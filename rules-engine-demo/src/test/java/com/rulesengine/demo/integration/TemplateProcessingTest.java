package com.rulesengine.demo.integration;

import com.rulesengine.core.service.engine.ExpressionEvaluatorService;
import com.rulesengine.core.service.engine.TemplateProcessorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for template processing with SpEL.
 * This class tests template processing without depending on demo classes.
 */
public class TemplateProcessingTest {
    
    private ExpressionEvaluatorService evaluatorService;
    private TemplateProcessorService templateProcessorService;
    private StandardEvaluationContext context;
    
    @BeforeEach
    public void setUp() {
        // Initialize services
        evaluatorService = new ExpressionEvaluatorService();
        templateProcessorService = new TemplateProcessorService(evaluatorService);
        
        // Initialize context
        context = new StandardEvaluationContext();
        
        // Create test data
        TestCustomer customer = new TestCustomer("John Doe", 42, "Silver", Arrays.asList("Equity", "FixedIncome"));
        List<TestProduct> products = createTestProducts();
        
        // Add test data to context
        context.setVariable("customer", customer);
        context.setVariable("products", products);
        
        // Add additional variables
        context.setVariable("orderTotal", 350.0);
        context.setVariable("tradingFee", 15.0);
    }
    
    /**
     * Test plain text template processing.
     */
    @Test
    public void testPlainTextTemplateProcessing() {
        // Create a template with placeholders
        String template = "Dear #{#customer.name},\n\n" +
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
        String processedTemplate = templateProcessorService.processTemplate(template, context);
        
        // Verify the processed template
        assertTrue(processedTemplate.contains("Dear John Doe"), "Template should contain customer name");
        assertTrue(processedTemplate.contains("Your Silver investor status"), "Template should contain membership level");
        assertTrue(processedTemplate.contains("10% reduced fees"), "Template should contain correct discount percentage");
        assertTrue(processedTemplate.contains("Investment amount: $350.0"), "Template should contain order total");
        assertTrue(processedTemplate.contains("Trading fee: $15.0"), "Template should contain trading fee");
        assertTrue(processedTemplate.contains("Fee discount: $35.0"), "Template should contain correct fee discount");
        assertTrue(processedTemplate.contains("Final investment total: $330.0"), "Template should contain correct final total");
        assertFalse(processedTemplate.contains("As a senior investor"), "Template should not contain senior investor text");
    }
    
    /**
     * Test XML template processing.
     */
    @Test
    public void testXmlTemplateProcessing() {
        // Create an XML template with placeholders
        String xmlTemplate = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
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
        
        // Verify the processed XML
        assertTrue(processedXml.contains("<Name>John Doe</Name>"), "XML should contain customer name");
        assertTrue(processedXml.contains("<Age>42</Age>"), "XML should contain customer age");
        assertTrue(processedXml.contains("<MembershipLevel>Silver</MembershipLevel>"), "XML should contain membership level");
        assertTrue(processedXml.contains("<Amount>350.0</Amount>"), "XML should contain order amount");
        assertTrue(processedXml.contains("<TradingFee>15.0</TradingFee>"), "XML should contain trading fee");
        assertTrue(processedXml.contains("<Discount>35.0</Discount>"), "XML should contain discount amount");
        assertTrue(processedXml.contains("<Total>330.0</Total>"), "XML should contain total amount");
        assertFalse(processedXml.contains("<Offer>Senior Investor Retirement Planning Guide</Offer>"), 
            "XML should not contain senior investor offer");
        assertFalse(processedXml.contains("<Offer>Premium Investment Opportunities</Offer>"), 
            "XML should not contain premium investment offer");
        
        // Test XML special characters escaping
        context.setVariable("xmlSpecialChars", "<test>&\"'</test>");
        String xmlSpecialCharsTemplate = "<SpecialChars>#{#xmlSpecialChars}</SpecialChars>";
        String processedXmlSpecialChars = templateProcessorService.processXmlTemplate(xmlSpecialCharsTemplate, context);
        assertTrue(processedXmlSpecialChars.contains("<SpecialChars>&lt;test&gt;&amp;&quot;&apos;&lt;/test&gt;</SpecialChars>"), 
            "XML special characters should be properly escaped");
    }
    
    /**
     * Test JSON template processing.
     */
    @Test
    public void testJsonTemplateProcessing() {
        // Create a JSON template with placeholders
        String jsonTemplate = "{\n" +
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
        
        // Verify the processed JSON
        assertTrue(processedJson.contains("\"name\": \"John Doe\""), "JSON should contain customer name");
        assertTrue(processedJson.contains("\"age\": 42"), "JSON should contain customer age");
        assertTrue(processedJson.contains("\"membershipLevel\": \"Silver\""), "JSON should contain membership level");
        assertTrue(processedJson.contains("\"amount\": 350.0"), "JSON should contain order amount");
        assertTrue(processedJson.contains("\"tradingFee\": 15.0"), "JSON should contain trading fee");
        assertTrue(processedJson.contains("\"discount\": 35.0"), "JSON should contain discount amount");
        assertTrue(processedJson.contains("\"total\": 330.0"), "JSON should contain total amount");
        
        // Test JSON special characters escaping
        context.setVariable("jsonSpecialChars", "test\"\\test\ntest");
        String jsonSpecialCharsTemplate = "{\"specialChars\": \"#{#jsonSpecialChars}\"}";
        String processedJsonSpecialChars = templateProcessorService.processJsonTemplate(jsonSpecialCharsTemplate, context);
        assertTrue(processedJsonSpecialChars.contains("\"specialChars\": \"test\\\"\\\\test\\ntest\""), 
            "JSON special characters should be properly escaped");
    }
    
    /**
     * Create test products for the tests.
     * 
     * @return List of test products
     */
    private List<TestProduct> createTestProducts() {
        List<TestProduct> products = new ArrayList<>();
        products.add(new TestProduct("US Treasury Bond", 1200.0, "FixedIncome"));
        products.add(new TestProduct("Apple Stock", 800.0, "Equity"));
        return products;
    }
    
    /**
     * Simple customer class for testing.
     */
    public static class TestCustomer {
        private String name;
        private int age;
        private String membershipLevel;
        private List<String> preferredCategories;
        
        public TestCustomer(String name, int age, String membershipLevel, List<String> preferredCategories) {
            this.name = name;
            this.age = age;
            this.membershipLevel = membershipLevel;
            this.preferredCategories = preferredCategories;
        }
        
        public String getName() {
            return name;
        }
        
        public int getAge() {
            return age;
        }
        
        public String getMembershipLevel() {
            return membershipLevel;
        }
        
        public List<String> getPreferredCategories() {
            return preferredCategories;
        }
    }
    
    /**
     * Simple product class for testing.
     */
    public static class TestProduct {
        private String name;
        private double price;
        private String category;
        
        public TestProduct(String name, double price, String category) {
            this.name = name;
            this.price = price;
            this.category = category;
        }
        
        public String getName() {
            return name;
        }
        
        public double getPrice() {
            return price;
        }
        
        public String getCategory() {
            return category;
        }
    }
}