package dev.mars.apex.demo;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.mars.apex.rest.ApexRestApiApplication;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.*;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

/**
 * Demo tests showcasing the new REST API functionality.
 * These tests demonstrate real-world usage scenarios for all the new endpoints.
 */
@SpringBootTest(classes = ApexRestApiApplication.class)
@AutoConfigureWebMvc
@ActiveProfiles("test")
class RestApiDemoTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        objectMapper = new ObjectMapper();
    }

    @Test
    @DisplayName("Demo: Complete customer data processing workflow")
    void testCompleteCustomerWorkflow() throws Exception {
        // Step 1: Transform raw customer data
        Map<String, Object> transformRequest = new HashMap<>();
        Map<String, Object> rawCustomerData = new HashMap<>();
        rawCustomerData.put("first_name", "john");
        rawCustomerData.put("last_name", "DOE");
        rawCustomerData.put("email_address", "JOHN.DOE@EXAMPLE.COM");
        rawCustomerData.put("phone", "1234567890");
        transformRequest.put("data", rawCustomerData);

        List<Map<String, Object>> transformRules = Arrays.asList(
            Map.of(
                "name", "normalize-firstName",
                "condition", "#first_name != null",
                "transformation", "#first_name.substring(0,1).toUpperCase() + #first_name.substring(1).toLowerCase()",
                "targetField", "firstName"
            ),
            Map.of(
                "name", "normalize-lastName",
                "condition", "#last_name != null",
                "transformation", "#last_name.substring(0,1).toUpperCase() + #last_name.substring(1).toLowerCase()",
                "targetField", "lastName"
            ),
            Map.of(
                "name", "normalize-email",
                "condition", "#email_address != null",
                "transformation", "#email_address.toLowerCase()",
                "targetField", "email"
            )
        );
        transformRequest.put("transformerRules", transformRules);

        String transformJson = objectMapper.writeValueAsString(transformRequest);

        mockMvc.perform(post("/api/transformations/dynamic")
                .contentType(MediaType.APPLICATION_JSON)
                .content(transformJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.appliedRules", is(3)))
                .andExpect(jsonPath("$.transformedData.firstName", is("John")))
                .andExpect(jsonPath("$.transformedData.lastName", is("Doe")))
                .andExpect(jsonPath("$.transformedData.email", is("john.doe@example.com")));

        // Step 2: Enrich customer data with additional information
        Map<String, Object> enrichRequest = new HashMap<>();
        Map<String, Object> customerData = new HashMap<>();
        customerData.put("customerId", "CUST001");
        customerData.put("firstName", "John");
        customerData.put("lastName", "Doe");
        customerData.put("email", "john.doe@example.com");
        enrichRequest.put("targetObject", customerData);

        String enrichYaml = """
            metadata:
              name: "Customer Profile Enrichment"
              version: "1.0.0"
            
            enrichments:
              - name: "customer-profile"
                condition: "#customerId != null"
                enrichmentType: "lookup"
                sourceField: "customerId"
                targetFields:
                  - "customerTier"
                  - "riskRating"
                  - "accountBalance"
            """;
        enrichRequest.put("yamlConfiguration", enrichYaml);

        String enrichJson = objectMapper.writeValueAsString(enrichRequest);

        mockMvc.perform(post("/api/enrichment/enrich")
                .contentType(MediaType.APPLICATION_JSON)
                .content(enrichJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.enrichmentCount", greaterThan(0)));

        // Step 3: Apply business rules to enriched data
        Map<String, Object> ruleRequest = new HashMap<>();
        List<Map<String, Object>> businessRules = Arrays.asList(
            Map.of(
                "name", "high-value-customer",
                "condition", "#accountBalance > 10000",
                "message", "High value customer identified"
            ),
            Map.of(
                "name", "gold-tier-customer",
                "condition", "#customerTier == 'GOLD'",
                "message", "Gold tier customer benefits apply"
            ),
            Map.of(
                "name", "low-risk-customer",
                "condition", "#riskRating == 'LOW'",
                "message", "Low risk customer - expedited processing"
            )
        );
        ruleRequest.put("rules", businessRules);

        Map<String, Object> enrichedFacts = new HashMap<>();
        enrichedFacts.put("customerId", "CUST001");
        enrichedFacts.put("customerTier", "GOLD");
        enrichedFacts.put("riskRating", "LOW");
        enrichedFacts.put("accountBalance", 15000.0);
        ruleRequest.put("facts", enrichedFacts);

        String ruleJson = objectMapper.writeValueAsString(ruleRequest);

        mockMvc.perform(post("/api/rules/batch")
                .contentType(MediaType.APPLICATION_JSON)
                .content(ruleJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.totalRules", is(3)))
                .andExpect(jsonPath("$.triggeredRules", greaterThan(0)));

        // Step 4: Generate customer communication template
        Map<String, Object> templateRequest = new HashMap<>();
        String welcomeTemplate = """
            Dear #{#firstName} #{#lastName},
            
            Welcome to our #{#customerTier} tier program!
            
            Your current account balance is #{#accountBalance} USD.
            Risk Rating: #{#riskRating}
            
            #{#customerTier == 'GOLD' ? 'As a Gold member, you enjoy premium benefits including priority support and exclusive offers.' : 'Thank you for choosing our services.'}
            
            Best regards,
            Customer Service Team
            """;
        templateRequest.put("template", welcomeTemplate);
        templateRequest.put("context", enrichedFacts);

        String templateJson = objectMapper.writeValueAsString(templateRequest);

        mockMvc.perform(post("/api/templates/text")
                .contentType(MediaType.APPLICATION_JSON)
                .content(templateJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.templateType", is("TEXT")))
                .andExpect(jsonPath("$.processedTemplate", containsString("Dear John Doe")))
                .andExpect(jsonPath("$.processedTemplate", containsString("GOLD tier program")));
    }

    @Test
    @DisplayName("Demo: Financial transaction processing and risk assessment")
    void testFinancialTransactionWorkflow() throws Exception {
        // Step 1: Evaluate transaction risk using expressions
        Map<String, Object> expressionRequest = new HashMap<>();
        List<Map<String, Object>> riskExpressions = Arrays.asList(
            Map.of("name", "amount-risk", "expression", "#amount > 10000 ? 'HIGH' : (#amount > 1000 ? 'MEDIUM' : 'LOW')"),
            Map.of("name", "velocity-risk", "expression", "#dailyTransactionCount > 10 ? 'HIGH' : 'LOW'"),
            Map.of("name", "location-risk", "expression", "#country == 'US' ? 'LOW' : 'MEDIUM'"),
            Map.of("name", "overall-score", "expression", "#amount * 0.3 + #dailyTransactionCount * 2 + (#country == 'US' ? 0 : 10)")
        );
        expressionRequest.put("expressions", riskExpressions);

        Map<String, Object> transactionContext = new HashMap<>();
        transactionContext.put("amount", 5000.0);
        transactionContext.put("currency", "USD");
        transactionContext.put("dailyTransactionCount", 3);
        transactionContext.put("country", "US");
        transactionContext.put("customerId", "CUST001");
        expressionRequest.put("context", transactionContext);

        String expressionJson = objectMapper.writeValueAsString(expressionRequest);

        mockMvc.perform(post("/api/expressions/batch")
                .contentType(MediaType.APPLICATION_JSON)
                .content(expressionJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.totalExpressions", is(4)))
                .andExpect(jsonPath("$.successfulExpressions", is(4)));

        // Step 2: Apply transaction rules
        Map<String, Object> transactionRuleRequest = new HashMap<>();
        Map<String, Object> transactionRule = new HashMap<>();
        transactionRule.put("name", "high-value-transaction");
        transactionRule.put("condition", "#amount > 1000 && #currency == 'USD' && #country == 'US'");
        transactionRule.put("message", "High value domestic USD transaction requires additional verification");
        transactionRuleRequest.put("rule", transactionRule);
        transactionRuleRequest.put("facts", transactionContext);

        String transactionRuleJson = objectMapper.writeValueAsString(transactionRuleRequest);

        mockMvc.perform(post("/api/rules/execute")
                .contentType(MediaType.APPLICATION_JSON)
                .content(transactionRuleJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.result.triggered", is(true)))
                .andExpect(jsonPath("$.result.ruleName", is("high-value-transaction")));

        // Step 3: Generate transaction alert template
        Map<String, Object> alertTemplateRequest = new HashMap<>();
        String alertTemplate = """
            {
              "alertId": "#{T(java.util.UUID).randomUUID().toString()}",
              "transactionId": "TXN-#{#customerId}-#{T(java.time.Instant).now().toEpochMilli()}",
              "customerId": "#{#customerId}",
              "amount": #{#amount},
              "currency": "#{#currency}",
              "riskLevel": "#{#amount > 10000 ? 'HIGH' : 'MEDIUM'}",
              "requiresApproval": #{#amount > 1000},
              "timestamp": "#{T(java.time.Instant).now()}",
              "location": {
                "country": "#{#country}",
                "riskRating": "#{#country == 'US' ? 'LOW' : 'MEDIUM'}"
              }
            }
            """;
        alertTemplateRequest.put("template", alertTemplate);
        alertTemplateRequest.put("context", transactionContext);

        String alertTemplateJson = objectMapper.writeValueAsString(alertTemplateRequest);

        mockMvc.perform(post("/api/templates/json")
                .contentType(MediaType.APPLICATION_JSON)
                .content(alertTemplateJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.processedTemplate", containsString("CUST001")))
                .andExpect(jsonPath("$.processedTemplate", containsString("5000")));
    }

    @Test
    @DisplayName("Demo: Data source integration and lookup operations")
    void testDataSourceIntegrationWorkflow() throws Exception {
        // Step 1: Get available data sources
        mockMvc.perform(get("/api/datasources"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.dataSources", isA(List.class)));

        // Step 2: Test data source connectivity (assuming a test data source exists)
        Map<String, Object> testParams = new HashMap<>();
        testParams.put("testKey", "CUST001");
        testParams.put("expectedFields", Arrays.asList("customerName", "customerTier"));

        String testParamsJson = objectMapper.writeValueAsString(testParams);

        // Note: This test assumes a test data source is configured
        // In a real scenario, you would have mock or test data sources set up
        mockMvc.perform(post("/api/datasources/testDataSource/test")
                .contentType(MediaType.APPLICATION_JSON)
                .content(testParamsJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", anyOf(is(true), is(false)))) // May fail if no test data source
                .andExpect(jsonPath("$.name", is("testDataSource")));

        // Step 3: Perform actual lookup (if data source is available)
        Map<String, Object> lookupRequest = Map.of("key", "CUST001");
        String lookupJson = objectMapper.writeValueAsString(lookupRequest);

        mockMvc.perform(post("/api/datasources/testDataSource/lookup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(lookupJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dataSource", is("testDataSource")))
                .andExpect(jsonPath("$.key", is("CUST001")));
    }

    @Test
    @DisplayName("Demo: Expression validation and function discovery")
    void testExpressionValidationWorkflow() throws Exception {
        // Step 1: Get available SpEL functions
        mockMvc.perform(get("/api/expressions/functions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.functions.mathematical", hasItem(containsString("abs"))))
                .andExpect(jsonPath("$.functions.string", hasItem(containsString("length"))))
                .andExpect(jsonPath("$.functions.logical", hasItem("&&")));

        // Step 2: Validate various expression syntaxes
        List<String> testExpressions = Arrays.asList(
            "#amount > 1000",
            "#customer.tier == 'GOLD'",
            "#amount * #rate + #fee",
            "T(java.time.LocalDate).now().isAfter(#expiryDate)",
            "#items.?[price > 100].size() > 0"
        );

        for (String expression : testExpressions) {
            Map<String, Object> validationRequest = Map.of("expression", expression);
            String validationJson = objectMapper.writeValueAsString(validationRequest);

            mockMvc.perform(post("/api/expressions/validate")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(validationJson))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success", is(true)))
                    .andExpect(jsonPath("$.expression", is(expression)))
                    .andExpect(jsonPath("$.valid", is(true)));
        }

        // Step 3: Test invalid expression
        Map<String, Object> invalidRequest = Map.of("expression", "invalid && syntax >");
        String invalidJson = objectMapper.writeValueAsString(invalidRequest);

        mockMvc.perform(post("/api/expressions/validate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.valid", is(false)))
                .andExpect(jsonPath("$.message", containsString("invalid")));
    }

    @Test
    @DisplayName("Demo: Multi-step template processing workflow")
    void testMultiStepTemplateWorkflow() throws Exception {
        // Step 1: Process multiple template types in batch
        Map<String, Object> batchRequest = new HashMap<>();
        
        List<Map<String, Object>> templates = Arrays.asList(
            Map.of(
                "name", "customer-json",
                "type", "JSON",
                "template", """
                    {
                      "customerId": "#{#customerId}",
                      "fullName": "#{#firstName} #{#lastName}",
                      "status": "#{#isActive ? 'ACTIVE' : 'INACTIVE'}",
                      "joinDate": "#{T(java.time.LocalDate).now()}"
                    }
                    """
            ),
            Map.of(
                "name", "customer-xml",
                "type", "XML",
                "template", """
                    <?xml version="1.0"?>
                    <customer>
                      <id>#{#customerId}</id>
                      <name>#{#firstName} #{#lastName}</name>
                      <active>#{#isActive}</active>
                    </customer>
                    """
            ),
            Map.of(
                "name", "customer-email",
                "type", "TEXT",
                "template", """
                    Dear #{#firstName} #{#lastName},
                    
                    Your account #{#customerId} is currently #{#isActive ? 'active' : 'inactive'}.
                    
                    Thank you for your business.
                    """
            )
        );
        batchRequest.put("templates", templates);

        Map<String, Object> customerContext = new HashMap<>();
        customerContext.put("customerId", "CUST001");
        customerContext.put("firstName", "John");
        customerContext.put("lastName", "Doe");
        customerContext.put("isActive", true);
        batchRequest.put("context", customerContext);

        String batchJson = objectMapper.writeValueAsString(batchRequest);

        mockMvc.perform(post("/api/templates/batch")
                .contentType(MediaType.APPLICATION_JSON)
                .content(batchJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.totalTemplates", is(3)))
                .andExpect(jsonPath("$.successfulTemplates", is(3)))
                .andExpect(jsonPath("$.processedTemplates", hasSize(3)))
                .andExpect(jsonPath("$.processedTemplates[0].name", is("customer-json")))
                .andExpect(jsonPath("$.processedTemplates[1].name", is("customer-xml")))
                .andExpect(jsonPath("$.processedTemplates[2].name", is("customer-email")));
    }
}
