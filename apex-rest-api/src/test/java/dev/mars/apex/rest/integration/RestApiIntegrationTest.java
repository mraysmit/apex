package dev.mars.apex.rest.integration;

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
 * Integration tests for all new REST API endpoints.
 * Tests the complete flow from HTTP request to response using actual service instances.
 */
@SpringBootTest(classes = ApexRestApiApplication.class)
@AutoConfigureWebMvc
@ActiveProfiles("test")
class RestApiIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        objectMapper = new ObjectMapper();
    }

    // ===== TRANSFORMATION CONTROLLER TESTS =====

    @Test
    @DisplayName("Should get registered transformers successfully")
    void testGetRegisteredTransformers() throws Exception {
        mockMvc.perform(get("/api/transformations/transformers"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.transformers", isA(List.class)))
                .andExpect(jsonPath("$.count", isA(Number.class)));
    }

    @Test
    @DisplayName("Should apply dynamic transformation rules")
    void testDynamicTransformation() throws Exception {
        Map<String, Object> request = new HashMap<>();
        
        // Test data
        Map<String, Object> data = new HashMap<>();
        data.put("firstName", "john");
        data.put("lastName", "doe");
        data.put("email", "JOHN.DOE@EXAMPLE.COM");
        request.put("data", data);

        // Transformation rules
        List<Map<String, Object>> rules = Arrays.asList(
            Map.of(
                "name", "normalize-firstName",
                "condition", "#firstName != null",
                "transformation", "#firstName.substring(0,1).toUpperCase() + #firstName.substring(1).toLowerCase()",
                "targetField", "firstName"
            ),
            Map.of(
                "name", "normalize-email",
                "condition", "#email != null",
                "transformation", "#email.toLowerCase()",
                "targetField", "email"
            )
        );
        request.put("transformerRules", rules);

        String requestJson = objectMapper.writeValueAsString(request);

        mockMvc.perform(post("/api/transformations/dynamic")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.originalData", notNullValue()))
                .andExpect(jsonPath("$.transformedData", notNullValue()))
                .andExpect(jsonPath("$.appliedRules", is(2)));
    }

    // ===== ENRICHMENT CONTROLLER TESTS =====

    @Test
    @DisplayName("Should get predefined enrichment configurations")
    void testGetPredefinedConfigurations() throws Exception {
        mockMvc.perform(get("/api/enrichment/configurations"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.configurations", isA(Collection.class)))
                .andExpect(jsonPath("$.count", isA(Number.class)));
    }

    @Test
    @DisplayName("Should enrich object using YAML configuration")
    void testEnrichObject() throws Exception {
        Map<String, Object> request = new HashMap<>();
        
        // Target object to enrich
        Map<String, Object> targetObject = new HashMap<>();
        targetObject.put("customerId", "CUST001");
        targetObject.put("transactionAmount", 1500.0);
        request.put("targetObject", targetObject);

        // YAML configuration
        String yamlConfig = """
            metadata:
              name: "Test Enrichment"
              version: "1.0.0"
            
            enrichments:
              - name: "customer-lookup"
                condition: "#customerId != null"
                enrichmentType: "lookup"
                sourceField: "customerId"
                targetFields:
                  - "customerName"
                  - "customerTier"
            """;
        request.put("yamlConfiguration", yamlConfig);

        String requestJson = objectMapper.writeValueAsString(request);

        mockMvc.perform(post("/api/enrichment/enrich")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.originalObject", notNullValue()))
                .andExpect(jsonPath("$.enrichedObject", notNullValue()));
    }

    // ===== TEMPLATE CONTROLLER TESTS =====

    @Test
    @DisplayName("Should process JSON template successfully")
    void testProcessJsonTemplate() throws Exception {
        Map<String, Object> request = new HashMap<>();
        
        // Template with expressions
        String jsonTemplate = """
            {
              "customerId": "#{#customerId}",
              "customerName": "#{#customerName}",
              "totalAmount": #{#totalAmount},
              "currency": "#{#currency}",
              "status": "#{#amount > 1000 ? 'HIGH_VALUE' : 'STANDARD'}"
            }
            """;
        request.put("template", jsonTemplate);

        // Context data
        Map<String, Object> context = new HashMap<>();
        context.put("customerId", "CUST001");
        context.put("customerName", "John Doe");
        context.put("totalAmount", 1500.0);
        context.put("currency", "USD");
        context.put("amount", 1500.0);
        request.put("context", context);

        String requestJson = objectMapper.writeValueAsString(request);

        mockMvc.perform(post("/api/templates/json")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.templateType", is("JSON")))
                .andExpect(jsonPath("$.originalTemplate", notNullValue()))
                .andExpect(jsonPath("$.processedTemplate", notNullValue()))
                .andExpect(jsonPath("$.context", notNullValue()));
    }

    @Test
    @DisplayName("Should process XML template successfully")
    void testProcessXmlTemplate() throws Exception {
        Map<String, Object> request = new HashMap<>();
        
        String xmlTemplate = """
            <?xml version="1.0" encoding="UTF-8"?>
            <customer>
              <id>#{#customerId}</id>
              <name>#{#customerName}</name>
              <amount>#{#totalAmount}</amount>
            </customer>
            """;
        request.put("template", xmlTemplate);

        Map<String, Object> context = new HashMap<>();
        context.put("customerId", "CUST001");
        context.put("customerName", "John Doe");
        context.put("totalAmount", 1500.0);
        request.put("context", context);

        String requestJson = objectMapper.writeValueAsString(request);

        mockMvc.perform(post("/api/templates/xml")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.templateType", is("XML")))
                .andExpect(jsonPath("$.processedTemplate", notNullValue()));
    }

    @Test
    @DisplayName("Should process batch templates successfully")
    void testProcessBatchTemplates() throws Exception {
        Map<String, Object> request = new HashMap<>();
        
        // Multiple templates
        List<Map<String, Object>> templates = Arrays.asList(
            Map.of("name", "json-template", "type", "JSON", "template", "{\"id\": \"#{#id}\"}"),
            Map.of("name", "xml-template", "type", "XML", "template", "<id>#{#id}</id>"),
            Map.of("name", "text-template", "type", "TEXT", "template", "ID: #{#id}")
        );
        request.put("templates", templates);

        Map<String, Object> context = Map.of("id", "TEST123");
        request.put("context", context);

        String requestJson = objectMapper.writeValueAsString(request);

        mockMvc.perform(post("/api/templates/batch")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.totalTemplates", is(3)))
                .andExpect(jsonPath("$.processedTemplates", hasSize(3)));
    }

    // ===== DATA SOURCE CONTROLLER TESTS =====

    @Test
    @DisplayName("Should get all data sources")
    void testGetAllDataSources() throws Exception {
        mockMvc.perform(get("/api/datasources"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.dataSources", isA(List.class)))
                .andExpect(jsonPath("$.count", isA(Number.class)));
    }

    @Test
    @DisplayName("Should perform data source lookup")
    void testPerformDataSourceLookup() throws Exception {
        // First, get available data sources to use one for testing
        String dataSourceName = "testDataSource"; // Assuming this exists in test configuration
        
        Map<String, Object> request = Map.of("key", "TEST_KEY");
        String requestJson = objectMapper.writeValueAsString(request);

        mockMvc.perform(post("/api/datasources/{name}/lookup", dataSourceName)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.dataSource", is(dataSourceName)))
                .andExpect(jsonPath("$.key", is("TEST_KEY")));
    }

    // ===== EXPRESSION CONTROLLER TESTS =====

    @Test
    @DisplayName("Should evaluate SpEL expression successfully")
    void testEvaluateExpression() throws Exception {
        Map<String, Object> request = new HashMap<>();
        request.put("expression", "#amount * #rate + #fee");
        
        Map<String, Object> context = new HashMap<>();
        context.put("amount", 1000.0);
        context.put("rate", 0.05);
        context.put("fee", 25.0);
        request.put("context", context);

        String requestJson = objectMapper.writeValueAsString(request);

        mockMvc.perform(post("/api/expressions/evaluate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.expression", is("#amount * #rate + #fee")))
                .andExpect(jsonPath("$.result", is(75.0)))
                .andExpect(jsonPath("$.resultType", is("Double")));
    }

    @Test
    @DisplayName("Should validate expression syntax")
    void testValidateExpression() throws Exception {
        Map<String, Object> request = Map.of("expression", "#amount > 1000 && #currency == 'USD'");
        String requestJson = objectMapper.writeValueAsString(request);

        mockMvc.perform(post("/api/expressions/validate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.valid", is(true)))
                .andExpect(jsonPath("$.message", containsString("valid")));
    }

    @Test
    @DisplayName("Should get available SpEL functions")
    void testGetAvailableFunctions() throws Exception {
        mockMvc.perform(get("/api/expressions/functions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.functions", notNullValue()))
                .andExpect(jsonPath("$.functions.mathematical", isA(List.class)))
                .andExpect(jsonPath("$.functions.string", isA(List.class)))
                .andExpect(jsonPath("$.functions.logical", isA(List.class)));
    }

    @Test
    @DisplayName("Should evaluate batch expressions")
    void testEvaluateBatchExpressions() throws Exception {
        Map<String, Object> request = new HashMap<>();
        
        List<Map<String, Object>> expressions = Arrays.asList(
            Map.of("name", "total-calc", "expression", "#amount * #rate"),
            Map.of("name", "age-check", "expression", "#age >= 18"),
            Map.of("name", "currency-check", "expression", "#currency == 'USD'")
        );
        request.put("expressions", expressions);

        Map<String, Object> context = new HashMap<>();
        context.put("amount", 1000.0);
        context.put("rate", 0.05);
        context.put("age", 25);
        context.put("currency", "USD");
        request.put("context", context);

        String requestJson = objectMapper.writeValueAsString(request);

        mockMvc.perform(post("/api/expressions/batch")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.totalExpressions", is(3)))
                .andExpect(jsonPath("$.successfulExpressions", is(3)))
                .andExpect(jsonPath("$.expressionResults", hasSize(3)));
    }

    // ===== RULES CONTROLLER ENHANCED TESTS =====

    @Test
    @DisplayName("Should execute single rule successfully")
    void testExecuteRule() throws Exception {
        Map<String, Object> request = new HashMap<>();
        
        Map<String, Object> rule = new HashMap<>();
        rule.put("name", "high-value-transaction");
        rule.put("condition", "#amount > 1000 && #currency == 'USD'");
        rule.put("message", "High value USD transaction detected");
        request.put("rule", rule);

        Map<String, Object> facts = new HashMap<>();
        facts.put("amount", 1500.0);
        facts.put("currency", "USD");
        facts.put("customerTier", "GOLD");
        request.put("facts", facts);

        String requestJson = objectMapper.writeValueAsString(request);

        mockMvc.perform(post("/api/rules/execute")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.facts", notNullValue()))
                .andExpect(jsonPath("$.result.triggered", is(true)))
                .andExpect(jsonPath("$.result.ruleName", is("high-value-transaction")));
    }

    @Test
    @DisplayName("Should execute batch rules successfully")
    void testExecuteBatchRules() throws Exception {
        Map<String, Object> request = new HashMap<>();
        
        List<Map<String, Object>> rules = Arrays.asList(
            Map.of("name", "high-value", "condition", "#amount > 1000", "message", "High value"),
            Map.of("name", "gold-customer", "condition", "#customerTier == 'GOLD'", "message", "Gold customer"),
            Map.of("name", "usd-currency", "condition", "#currency == 'USD'", "message", "USD currency")
        );
        request.put("rules", rules);

        Map<String, Object> facts = new HashMap<>();
        facts.put("amount", 1500.0);
        facts.put("currency", "USD");
        facts.put("customerTier", "GOLD");
        request.put("facts", facts);

        String requestJson = objectMapper.writeValueAsString(request);

        mockMvc.perform(post("/api/rules/batch")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.totalRules", is(3)))
                .andExpect(jsonPath("$.triggeredRules", is(3)))
                .andExpect(jsonPath("$.results", hasSize(3)));
    }
}
