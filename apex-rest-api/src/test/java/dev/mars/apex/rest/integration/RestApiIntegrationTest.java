package dev.mars.apex.rest.integration;

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


import com.fasterxml.jackson.databind.ObjectMapper;
import dev.mars.apex.rest.ApexRestApiApplication;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for all new REST API endpoints using real HTTP requests.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-08-28
 * @version 2.0 - Refactored to use TestRestTemplate instead of MockMvc
 */
@SpringBootTest(classes = ApexRestApiApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class RestApiIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
    }

    // ===== TRANSFORMATION CONTROLLER TESTS =====

    @Test
    @DisplayName("Should get registered transformers successfully")
    void testGetRegisteredTransformers() {
        ResponseEntity<Map> response = restTemplate.getForEntity("/api/transformations/transformers", Map.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(true, response.getBody().get("success"));
        assertNotNull(response.getBody().get("transformers"));
        assertNotNull(response.getBody().get("count"));
    }

    @Test
    @DisplayName("Should apply dynamic transformation rules")
    void testDynamicTransformation() {
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

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> httpRequest = new HttpEntity<>(request, headers);

        ResponseEntity<Map> response = restTemplate.postForEntity("/api/transformations/dynamic", httpRequest, Map.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(true, response.getBody().get("success"));
        assertNotNull(response.getBody().get("originalData"));
        assertNotNull(response.getBody().get("transformedData"));
        assertEquals(2, response.getBody().get("appliedRules"));
    }

    // ===== ENRICHMENT CONTROLLER TESTS =====

    @Test
    @DisplayName("Should get predefined enrichment configurations")
    void testGetPredefinedConfigurations() {
        ResponseEntity<Map> response = restTemplate.getForEntity("/api/enrichment/configurations", Map.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(true, response.getBody().get("success"));
        assertNotNull(response.getBody().get("configurations"));
        assertNotNull(response.getBody().get("count"));
    }

    @Test
    @DisplayName("Should enrich object using YAML configuration")
    void testEnrichObject() {
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
              - id: "customer-lookup"
                name: "customer-lookup"
                type: "lookup-enrichment"
                condition: "#customerId != null"
                lookup-config:
                  lookup-key: "#customerId"
                  lookup-dataset:
                    type: "inline"
                    key-field: "customerId"
                    data:
                      - customerId: "CUST123"
                        customerName: "Test Customer"
                        customerTier: "GOLD"
                field-mappings:
                  - source-field: "customerName"
                    target-field: "customerName"
                  - source-field: "customerTier"
                    target-field: "customerTier"
            """;
        request.put("yamlConfiguration", yamlConfig);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> httpRequest = new HttpEntity<>(request, headers);

        ResponseEntity<Map> response = restTemplate.postForEntity("/api/enrichment/enrich", httpRequest, Map.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(true, response.getBody().get("success"));
        assertNotNull(response.getBody().get("originalObject"));
        assertNotNull(response.getBody().get("enrichedObject"));
    }

    // ===== TEMPLATE CONTROLLER TESTS =====

    @Test
    @DisplayName("Should process JSON template successfully")
    void testProcessJsonTemplate() {
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

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> httpRequest = new HttpEntity<>(request, headers);

        ResponseEntity<Map> response = restTemplate.postForEntity("/api/templates/json", httpRequest, Map.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(true, response.getBody().get("success"));
        assertEquals("JSON", response.getBody().get("templateType"));
        assertNotNull(response.getBody().get("originalTemplate"));
        assertNotNull(response.getBody().get("processedTemplate"));
    }

    @Test
    @DisplayName("Should process XML template successfully")
    void testProcessXmlTemplate() {
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

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> httpRequest = new HttpEntity<>(request, headers);

        ResponseEntity<Map> response = restTemplate.postForEntity("/api/templates/xml", httpRequest, Map.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(true, response.getBody().get("success"));
        assertEquals("XML", response.getBody().get("templateType"));
        assertNotNull(response.getBody().get("processedTemplate"));
    }

    @Test
    @DisplayName("Should process batch templates successfully")
    void testProcessBatchTemplates() {
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

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> httpRequest = new HttpEntity<>(request, headers);

        ResponseEntity<Map> response = restTemplate.postForEntity("/api/templates/batch", httpRequest, Map.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(true, response.getBody().get("success"));
        assertEquals(3, response.getBody().get("totalTemplates"));
    }

    // ===== DATA SOURCE CONTROLLER TESTS =====

    @Test
    @DisplayName("Should get all data sources")
    void testGetAllDataSources() {
        ResponseEntity<Map> response = restTemplate.getForEntity("/api/datasources", Map.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(true, response.getBody().get("success"));
        assertNotNull(response.getBody().get("dataSources"));
        assertNotNull(response.getBody().get("count"));
    }

    @Test
    @DisplayName("Should perform data source lookup")
    void testPerformDataSourceLookup() {
        String dataSourceName = "testDataSource";

        Map<String, Object> request = Map.of("key", "TEST_KEY");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> httpRequest = new HttpEntity<>(request, headers);

        ResponseEntity<Map> response = restTemplate.postForEntity(
            "/api/datasources/" + dataSourceName + "/lookup",
            httpRequest,
            Map.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(true, response.getBody().get("success"));
        assertEquals(dataSourceName, response.getBody().get("dataSource"));
        assertEquals("TEST_KEY", response.getBody().get("key"));
    }

    // ===== EXPRESSION CONTROLLER TESTS =====

    @Test
    @DisplayName("Should evaluate SpEL expression successfully")
    void testEvaluateExpression() {
        Map<String, Object> request = new HashMap<>();
        request.put("expression", "#amount * #rate + #fee");

        Map<String, Object> context = new HashMap<>();
        context.put("amount", 1000.0);
        context.put("rate", 0.05);
        context.put("fee", 25.0);
        request.put("context", context);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> httpRequest = new HttpEntity<>(request, headers);

        ResponseEntity<Map> response = restTemplate.postForEntity("/api/expressions/evaluate", httpRequest, Map.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(true, response.getBody().get("success"));
        assertEquals("#amount * #rate + #fee", response.getBody().get("expression"));
        assertEquals(75.0, ((Number) response.getBody().get("result")).doubleValue());
        assertEquals("Double", response.getBody().get("resultType"));
    }

    @Test
    @DisplayName("Should validate expression syntax")
    void testValidateExpression() {
        Map<String, Object> request = Map.of("expression", "#amount > 1000 && #currency == 'USD'");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> httpRequest = new HttpEntity<>(request, headers);

        ResponseEntity<Map> response = restTemplate.postForEntity("/api/expressions/validate", httpRequest, Map.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(true, response.getBody().get("success"));
        assertEquals(true, response.getBody().get("valid"));
    }

    @Test
    @DisplayName("Should get available SpEL functions")
    void testGetAvailableFunctions() {
        ResponseEntity<Map> response = restTemplate.getForEntity("/api/expressions/functions", Map.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(true, response.getBody().get("success"));
        assertNotNull(response.getBody().get("functions"));
    }

    @Test
    @DisplayName("Should evaluate batch expressions")
    void testEvaluateBatchExpressions() {
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

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> httpRequest = new HttpEntity<>(request, headers);

        ResponseEntity<Map> response = restTemplate.postForEntity("/api/expressions/batch", httpRequest, Map.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(true, response.getBody().get("success"));
        assertEquals(3, response.getBody().get("totalExpressions"));
        assertEquals(3, response.getBody().get("successfulExpressions"));
    }

    // ===== RULES CONTROLLER ENHANCED TESTS =====

    @Test
    @DisplayName("Should execute single rule successfully")
    void testExecuteRule() {
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

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> httpRequest = new HttpEntity<>(request, headers);

        ResponseEntity<Map> response = restTemplate.postForEntity("/api/rules/execute", httpRequest, Map.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(true, response.getBody().get("success"));
        assertNotNull(response.getBody().get("facts"));
        Map<String, Object> result = (Map<String, Object>) response.getBody().get("result");
        assertEquals(true, result.get("triggered"));
        assertEquals("high-value-transaction", result.get("ruleName"));
    }

    @Test
    @DisplayName("Should execute batch rules successfully")
    void testExecuteBatchRules() {
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

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> httpRequest = new HttpEntity<>(request, headers);

        ResponseEntity<Map> response = restTemplate.postForEntity("/api/rules/batch", httpRequest, Map.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(true, response.getBody().get("success"));
        assertEquals(3, response.getBody().get("totalRules"));
        assertEquals(3, response.getBody().get("triggeredRules"));
    }
}

