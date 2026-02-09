package dev.mars.apex.core.config;
import dev.mars.apex.core.config.model.*;
import dev.mars.apex.core.config.loader.*;
import dev.mars.apex.core.config.exception.*;
import dev.mars.apex.core.config.service.*;

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

import org.junit.jupiter.api.BeforeEach;

import dev.mars.apex.core.test.extension.ColoredTestOutputExtension;
import dev.mars.apex.core.test.extension.TestClassLoggingExtension;
import org.junit.jupiter.api.DisplayName;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for dual format support (map and array formats).
 * 
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2.2.0
 */
@DisplayName("Dual Format Support Tests")
class DualFormatDeserializationTest {
    
    private static final Logger logger = LoggerFactory.getLogger(DualFormatDeserializationTest.class);
    
    private YamlConfigurationLoader loader;
    
    @BeforeEach
    public void setUp() {
        logger.info("Setting up YAML configuration loader for dual format testing...");
        this.loader = new YamlConfigurationLoader();
    }
    
    @Test
    @DisplayName("Should deserialize traditional map format for queries")
    void testTraditionalMapFormat() throws Exception {
        String yaml = """
            data-sources:
              - name: "test-db"
                type: "database"
                queries:
                  getCustomer: "SELECT * FROM customers WHERE id = :id"
                  getAllActive: "SELECT * FROM customers WHERE status = 'ACTIVE'"
            """;
        
        YamlRuleConfiguration config = loader.fromYamlString(yaml);
        
        assertNotNull(config.getDataSources());
        assertEquals(1, config.getDataSources().size());
        
        YamlDataSource dataSource = config.getDataSources().get(0);
        Map<String, String> queries = dataSource.getQueries();
        
        assertNotNull(queries);
        assertEquals(2, queries.size());
        assertEquals("SELECT * FROM customers WHERE id = :id", queries.get("getCustomer"));
        assertEquals("SELECT * FROM customers WHERE status = 'ACTIVE'", queries.get("getAllActive"));
    }
    
    @Test
    @DisplayName("Should deserialize new array format for queries")
    void testNewArrayFormat() throws Exception {
        String yaml = """
            data-sources:
              - name: "test-db"
                type: "database"
                queries:
                  - name: "getCustomer"
                    query: "SELECT * FROM customers WHERE id = :id"
                    description: "Get customer by ID"
                    parameters: ["id"]
                  - name: "getAllActive"
                    query: "SELECT * FROM customers WHERE status = 'ACTIVE'"
                    tags: ["customer", "list"]
            """;
        
        YamlRuleConfiguration config = loader.fromYamlString(yaml);
        
        assertNotNull(config.getDataSources());
        YamlDataSource dataSource = config.getDataSources().get(0);
        Map<String, String> queries = dataSource.getQueries();
        
        assertNotNull(queries);
        assertEquals(2, queries.size());
        assertEquals("SELECT * FROM customers WHERE id = :id", queries.get("getCustomer"));
        assertEquals("SELECT * FROM customers WHERE status = 'ACTIVE'", queries.get("getAllActive"));
    }
    
    @Test
    @DisplayName("Should handle multiline queries in array format")
    void testMultilineQueriesArrayFormat() throws Exception {
        String yaml = """
            data-sources:
              - name: "test-db"
                type: "database"
                queries:
                  - name: "complexQuery"
                    query: |
                      SELECT 
                        c.customer_id,
                        c.customer_name,
                        c.email
                      FROM customers c
                      WHERE c.customer_id = :customerId
                        AND c.status = 'ACTIVE'
            """;
        
        YamlRuleConfiguration config = loader.fromYamlString(yaml);
        YamlDataSource dataSource = config.getDataSources().get(0);
        Map<String, String> queries = dataSource.getQueries();
        
        String query = queries.get("complexQuery");
        assertNotNull(query);
        assertTrue(query.contains("SELECT"));
        assertTrue(query.contains("FROM customers c"));
        assertTrue(query.contains("WHERE c.customer_id = :customerId"));
    }
    
    @Test
    @DisplayName("Should deserialize operations in map format")
    void testOperationsMapFormat() throws Exception {
        String yaml = """
            data-sources:
              - name: "test-db"
                type: "database"
                operations:
                  insertCustomer: "INSERT INTO customers (name) VALUES (:name)"
                  updateStatus: "UPDATE customers SET status = :status WHERE id = :id"
            """;
        
        YamlRuleConfiguration config = loader.fromYamlString(yaml);
        YamlDataSource dataSource = config.getDataSources().get(0);
        Map<String, String> operations = dataSource.getOperations();
        
        assertNotNull(operations);
        assertEquals(2, operations.size());
        assertTrue(operations.containsKey("insertCustomer"));
        assertTrue(operations.containsKey("updateStatus"));
    }
    
    @Test
    @DisplayName("Should deserialize operations in array format")
    void testOperationsArrayFormat() throws Exception {
        String yaml = """
            data-sources:
              - name: "test-db"
                type: "database"
                operations:
                  - name: "insertCustomer"
                    query: "INSERT INTO customers (name) VALUES (:name)"
                    description: "Create new customer"
                  - name: "updateStatus"
                    query: "UPDATE customers SET status = :status WHERE id = :id"
            """;
        
        YamlRuleConfiguration config = loader.fromYamlString(yaml);
        YamlDataSource dataSource = config.getDataSources().get(0);
        Map<String, String> operations = dataSource.getOperations();
        
        assertNotNull(operations);
        assertEquals(2, operations.size());
        assertEquals("INSERT INTO customers (name) VALUES (:name)", operations.get("insertCustomer"));
    }
    
    @Test
    @DisplayName("Should deserialize endpoints in map format")
    void testEndpointsMapFormat() throws Exception {
        String yaml = """
            data-sources:
              - name: "api"
                type: "rest-api"
                connection:
                  base-url: "https://api.example.com"
                endpoints:
                  getUser: "/api/users/{id}"
                  createUser: "/api/users"
            """;
        
        YamlRuleConfiguration config = loader.fromYamlString(yaml);
        YamlDataSource dataSource = config.getDataSources().get(0);
        Map<String, String> endpoints = dataSource.getEndpoints();
        
        assertNotNull(endpoints);
        assertEquals(2, endpoints.size());
        assertEquals("/api/users/{id}", endpoints.get("getUser"));
        assertEquals("/api/users", endpoints.get("createUser"));
    }
    
    @Test
    @DisplayName("Should deserialize endpoints in array format")
    void testEndpointsArrayFormat() throws Exception {
        String yaml = """
            data-sources:
              - name: "api"
                type: "rest-api"
                connection:
                  base-url: "https://api.example.com"
                endpoints:
                  - name: "getUser"
                    endpoint: "/api/users/{id}"
                    method: "GET"
                    description: "Get user by ID"
                  - name: "createUser"
                    endpoint: "/api/users"
                    method: "POST"
            """;
        
        YamlRuleConfiguration config = loader.fromYamlString(yaml);
        YamlDataSource dataSource = config.getDataSources().get(0);
        Map<String, String> endpoints = dataSource.getEndpoints();
        
        assertNotNull(endpoints);
        assertEquals(2, endpoints.size());
        assertEquals("/api/users/{id}", endpoints.get("getUser"));
        assertEquals("/api/users", endpoints.get("createUser"));
    }
    
    @Test
    @DisplayName("Should handle mixed formats in same configuration")
    void testMixedFormats() throws Exception {
        String yaml = """
            data-sources:
              - name: "test-db"
                type: "database"
                queries:
                  simpleQuery: "SELECT * FROM customers"
                operations:
                  - name: "complexOp"
                    query: "INSERT INTO logs (message) VALUES (:msg)"
                    description: "Log message"
            """;
        
        YamlRuleConfiguration config = loader.fromYamlString(yaml);
        YamlDataSource dataSource = config.getDataSources().get(0);
        
        Map<String, String> queries = dataSource.getQueries();
        Map<String, String> operations = dataSource.getOperations();
        
        assertNotNull(queries);
        assertEquals(1, queries.size());
        assertEquals("SELECT * FROM customers", queries.get("simpleQuery"));
        
        assertNotNull(operations);
        assertEquals(1, operations.size());
        assertEquals("INSERT INTO logs (message) VALUES (:msg)", operations.get("complexOp"));
    }
    
    @Test
    @DisplayName("Should reject duplicate query names in array format")
    void testDuplicateQueryNames() {
        String yaml = """
            data-sources:
              - name: "test-db"
                type: "database"
                queries:
                  - name: "getCustomer"
                    query: "SELECT * FROM customers WHERE id = :id"
                  - name: "getCustomer"
                    query: "SELECT * FROM customers WHERE email = :email"
            """;
        
        assertThrows(YamlConfigurationException.class, () -> {
            loader.fromYamlString(yaml);
        });
    }
    
    @Test
    @DisplayName("Should reject array format without name field")
    void testArrayFormatMissingName() {
        String yaml = """
            data-sources:
              - name: "test-db"
                type: "database"
                queries:
                  - query: "SELECT * FROM customers"
                    description: "Missing name field"
            """;
        
        assertThrows(YamlConfigurationException.class, () -> {
            loader.fromYamlString(yaml);
        });
    }
    
    @Test
    @DisplayName("Should reject array format without query field")
    void testArrayFormatMissingQuery() {
        String yaml = """
            data-sources:
              - name: "test-db"
                type: "database"
                queries:
                  - name: "getCustomer"
                    description: "Missing query field"
            """;
        
        assertThrows(YamlConfigurationException.class, () -> {
            loader.fromYamlString(yaml);
        });
    }
    
    @Test
    @DisplayName("Should reject plain array of strings")
    void testPlainArrayOfStrings() {
        String yaml = """
            data-sources:
              - name: "test-db"
                type: "database"
                queries:
                  - "SELECT * FROM customers"
                  - "SELECT * FROM orders"
            """;
        
        assertThrows(YamlConfigurationException.class, () -> {
            loader.fromYamlString(yaml);
        });
    }
    
    @Test
    @DisplayName("Should work with enrichments using array-format queries")
    void testEnrichmentWithArrayFormatQuery() throws Exception {
        String yaml = """
            data-sources:
              - name: "customer-db"
                type: "database"
                connection:
                  driver: "org.h2.Driver"
                  url: "jdbc:h2:mem:test"
                queries:
                  - name: "getCustomerProfile"
                    query: "SELECT * FROM customers WHERE id = :id"
                    description: "Get customer profile"
                    tags: ["customer", "profile"]
            
            enrichments:
              - id: "customer-lookup"
                type: "lookup-enrichment"
                lookup-config:
                  lookup-key: "#customerId"
                  lookup-dataset:
                    type: "database"
                    data-source-ref: "customer-db"
                    query-ref: "getCustomerProfile"
                  output-field: "customerData"
            """;
        
        YamlRuleConfiguration config = loader.fromYamlString(yaml);
        
        assertNotNull(config.getDataSources());
        assertNotNull(config.getEnrichments());
        assertEquals(1, config.getEnrichments().size());
        
        YamlEnrichment enrichment = config.getEnrichments().get(0);
        assertEquals("customer-lookup", enrichment.getId());
        assertEquals("getCustomerProfile", enrichment.getLookupConfig().getLookupDataset().getQueryRef());
    }
    
    @Test
    @DisplayName("Should support all metadata fields in array format")
    void testCompleteMetadataFields() throws Exception {
        String yaml = """
            data-sources:
              - name: "test-db"
                type: "database"
                queries:
                  - id: "Q-001"
                    name: "getCustomer"
                    query: "SELECT * FROM customers WHERE id = :id"
                    description: "Retrieve customer by ID"
                    parameters: ["id"]
                    tags: ["customer", "read", "pii"]
                    version: "2.0"
                    deprecated: false
                    author: "Customer Team"
            """;
        
        YamlRuleConfiguration config = loader.fromYamlString(yaml);
        YamlDataSource dataSource = config.getDataSources().get(0);
        Map<String, String> queries = dataSource.getQueries();
        
        // Metadata is preserved internally but runtime uses Map<String, String>
        assertNotNull(queries);
        assertEquals("SELECT * FROM customers WHERE id = :id", queries.get("getCustomer"));
    }
    
    @Test
    @DisplayName("Should handle empty queries in both formats")
    void testEmptyQueries() throws Exception {
        String yamlMap = """
            data-sources:
              - name: "test-db"
                type: "database"
                queries: {}
            """;
        
        String yamlArray = """
            data-sources:
              - name: "test-db"
                type: "database"
                queries: []
            """;
        
        YamlRuleConfiguration configMap = loader.fromYamlString(yamlMap);
        YamlRuleConfiguration configArray = loader.fromYamlString(yamlArray);
        
        assertNotNull(configMap.getDataSources().get(0).getQueries());
        assertTrue(configMap.getDataSources().get(0).getQueries().isEmpty());
        
        assertNotNull(configArray.getDataSources().get(0).getQueries());
        assertTrue(configArray.getDataSources().get(0).getQueries().isEmpty());
    }
}
