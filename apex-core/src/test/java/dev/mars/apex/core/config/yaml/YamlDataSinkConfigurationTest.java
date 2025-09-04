package dev.mars.apex.core.config.yaml;

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
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import dev.mars.apex.core.config.datasink.DataSinkConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for YAML data sink configuration integration.
 * 
 * Tests the new data-sinks section in APEX YAML configuration,
 * ensuring it follows APEX syntax conventions and integrates
 * properly with existing configuration patterns.
 * 
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 1.0.0
 * @version 1.0
 */
public class YamlDataSinkConfigurationTest {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(YamlDataSinkConfigurationTest.class);
    
    private ObjectMapper yamlMapper;
    
    @BeforeEach
    void setUp() {
        yamlMapper = new ObjectMapper(new YAMLFactory());
    }
    
    @Test
    void testBasicDataSinkYamlParsing() throws Exception {
        LOGGER.info("TEST: Testing basic data sink YAML parsing");
        
        String yamlContent = """
            metadata:
              name: "Test Configuration with Data Sinks"
              version: "1.0.0"
              description: "Test APEX configuration with data sinks"
              author: "test.team@company.com"
            
            data-sinks:
              - name: "test-h2-sink"
                type: "database"
                source-type: "h2"
                enabled: true
                description: "Test H2 database sink"
                
                connection:
                  database: "./target/test/test_data"
                  username: "sa"
                  password: ""
                  mode: "PostgreSQL"
                
                operations:
                  insertRecord: "INSERT INTO test_table (id, name, value) VALUES (:id, :name, :value)"
                  updateRecord: "UPDATE test_table SET name = :name, value = :value WHERE id = :id"
            """;
        
        // Parse YAML
        YamlRuleConfiguration config = yamlMapper.readValue(yamlContent, YamlRuleConfiguration.class);
        
        // Verify metadata
        assertNotNull(config.getMetadata());
        assertEquals("Test Configuration with Data Sinks", config.getMetadata().getName());
        assertEquals("1.0.0", config.getMetadata().getVersion());
        
        // Verify data sinks
        assertNotNull(config.getDataSinks());
        assertEquals(1, config.getDataSinks().size());
        
        YamlDataSink dataSink = config.getDataSinks().get(0);
        assertEquals("test-h2-sink", dataSink.getName());
        assertEquals("database", dataSink.getType());
        assertEquals("h2", dataSink.getSourceType());
        assertTrue(dataSink.getEnabled());
        assertEquals("Test H2 database sink", dataSink.getDescription());
        
        // Verify connection configuration
        assertNotNull(dataSink.getConnection());
        assertEquals("./target/test/test_data", dataSink.getConnection().get("database"));
        assertEquals("sa", dataSink.getConnection().get("username"));
        assertEquals("", dataSink.getConnection().get("password"));
        assertEquals("PostgreSQL", dataSink.getConnection().get("mode"));
        
        // Verify operations
        assertNotNull(dataSink.getOperations());
        assertEquals(2, dataSink.getOperations().size());
        assertTrue(dataSink.getOperations().containsKey("insertRecord"));
        assertTrue(dataSink.getOperations().containsKey("updateRecord"));
        
        LOGGER.info("✓ Basic data sink YAML parsing test passed");
    }
    
    @Test
    void testDataSinkConfigurationConversion() throws Exception {
        LOGGER.info("TEST: Testing data sink configuration conversion");
        
        // Create YamlDataSink programmatically
        YamlDataSink yamlDataSink = new YamlDataSink();
        yamlDataSink.setName("test-file-sink");
        yamlDataSink.setType("file-system");
        yamlDataSink.setSourceType("csv");
        yamlDataSink.setEnabled(true);
        yamlDataSink.setDescription("Test CSV file sink");
        yamlDataSink.setTags(Arrays.asList("test", "csv", "output"));
        
        // Set connection configuration
        Map<String, Object> connection = new HashMap<>();
        connection.put("base-path", "./target/test/output");
        connection.put("file-pattern", "test_output_{timestamp}.csv");
        connection.put("encoding", "UTF-8");
        yamlDataSink.setConnection(connection);
        
        // Set operations
        Map<String, String> operations = new HashMap<>();
        operations.put("write", "WRITE_CSV");
        operations.put("append", "APPEND_CSV");
        yamlDataSink.setOperations(operations);
        
        // Set output format
        Map<String, Object> outputFormat = new HashMap<>();
        outputFormat.put("format", "csv");
        outputFormat.put("delimiter", ",");
        outputFormat.put("include-header", true);
        outputFormat.put("encoding", "UTF-8");
        yamlDataSink.setOutputFormat(outputFormat);
        
        // Set batch configuration
        Map<String, Object> batch = new HashMap<>();
        batch.put("enabled", true);
        batch.put("batch-size", 100);
        batch.put("timeout-ms", 5000);
        yamlDataSink.setBatch(batch);
        
        // Convert to DataSinkConfiguration
        DataSinkConfiguration config = yamlDataSink.toDataSinkConfiguration();
        
        // Verify conversion
        assertNotNull(config);
        assertEquals("test-file-sink", config.getName());
        assertEquals("file-system", config.getType());
        assertEquals("csv", config.getSourceType());
        assertTrue(config.isEnabled());
        assertEquals("Test CSV file sink", config.getDescription());
        
        // Verify operations were converted
        assertNotNull(config.getOperations());
        assertEquals(2, config.getOperations().size());
        assertEquals("WRITE_CSV", config.getOperations().get("write"));
        assertEquals("APPEND_CSV", config.getOperations().get("append"));
        
        // Verify tags were converted
        assertNotNull(config.getTags());
        assertEquals(3, config.getTags().size());
        assertTrue(config.getTags().contains("test"));
        assertTrue(config.getTags().contains("csv"));
        assertTrue(config.getTags().contains("output"));
        
        LOGGER.info("✓ Data sink configuration conversion test passed");
    }
    
    @Test
    void testCompleteYamlConfigurationWithDataSinks() throws Exception {
        LOGGER.info("TEST: Testing complete YAML configuration with data sinks");
        
        String yamlContent = """
            metadata:
              name: "Complete Configuration with Data Sinks"
              version: "1.0.0"
              description: "Complete APEX configuration demonstrating data sinks integration"
              author: "test.team@company.com"
              tags: ["test", "integration", "data-sinks"]
            
            rules:
              - id: "test-validation-rule"
                name: "Test Validation Rule"
                condition: "#amount > 0"
                message: "Amount must be positive"
                enabled: true
            
            enrichments:
              - id: "test-enrichment"
                name: "Test Data Enrichment"
                type: "field-transformation"
                description: "Test enrichment for data processing"
                condition: "true"
                enabled: true
            
            data-sinks:
              - name: "primary-database-sink"
                type: "database"
                source-type: "h2"
                enabled: true
                description: "Primary H2 database for processed data"
                tags: ["primary", "database", "h2"]
                
                connection:
                  database: "./target/test/primary_data"
                  username: "sa"
                  password: ""
                  mode: "PostgreSQL"
                  connection-pool:
                    max-size: 10
                    min-size: 2
                    connection-timeout: 30000
                
                operations:
                  insertCustomer: "INSERT INTO customers (id, name, email, processed_at) VALUES (:id, :processedName, :email, :processedAt)"
                  updateCustomer: "UPDATE customers SET name = :processedName, email = :email WHERE id = :id"
                  upsertCustomer: "MERGE INTO customers (id, name, email, processed_at) KEY (id) VALUES (:id, :processedName, :email, :processedAt)"
                
                schema:
                  auto-create: true
                  table-name: "customers"
                  init-script: |
                    CREATE TABLE IF NOT EXISTS customers (
                      id INTEGER PRIMARY KEY,
                      name VARCHAR(255) NOT NULL,
                      email VARCHAR(255),
                      processed_at TIMESTAMP
                    );
                
                error-handling:
                  strategy: "log-and-continue"
                  max-retries: 3
                  retry-delay: 1000
                  dead-letter-table: "failed_records"
                
                batch:
                  enabled: true
                  batch-size: 50
                  timeout-ms: 10000
                  transaction-mode: "per-batch"
              
              - name: "audit-file-sink"
                type: "file-system"
                source-type: "json"
                enabled: true
                description: "Audit trail file output"
                tags: ["audit", "file", "json"]
                
                connection:
                  base-path: "./target/test/audit"
                  file-pattern: "audit_{timestamp}.json"
                  encoding: "UTF-8"
                
                operations:
                  writeAuditRecord: "WRITE_JSON"
                  appendAuditRecord: "APPEND_JSON"
                
                output-format:
                  format: "json"
                  pretty-print: true
                  encoding: "UTF-8"
                  include-timestamp: true
                
                batch:
                  enabled: true
                  batch-size: 100
                  flush-interval-ms: 5000
            """;
        
        // Parse YAML
        YamlRuleConfiguration config = yamlMapper.readValue(yamlContent, YamlRuleConfiguration.class);
        
        // Verify complete configuration structure
        assertNotNull(config.getMetadata());
        assertNotNull(config.getRules());
        assertNotNull(config.getEnrichments());
        assertNotNull(config.getDataSinks());
        
        // Verify rules section
        assertEquals(1, config.getRules().size());
        assertEquals("test-validation-rule", config.getRules().get(0).getId());
        
        // Verify enrichments section
        assertEquals(1, config.getEnrichments().size());
        assertEquals("test-enrichment", config.getEnrichments().get(0).getId());
        
        // Verify data sinks section
        assertEquals(2, config.getDataSinks().size());
        
        // Verify database sink
        YamlDataSink dbSink = config.getDataSinks().get(0);
        assertEquals("primary-database-sink", dbSink.getName());
        assertEquals("database", dbSink.getType());
        assertEquals("h2", dbSink.getSourceType());
        assertNotNull(dbSink.getConnection());
        assertNotNull(dbSink.getOperations());
        assertNotNull(dbSink.getSchema());
        assertNotNull(dbSink.getErrorHandling());
        assertNotNull(dbSink.getBatch());
        
        // Verify file sink
        YamlDataSink fileSink = config.getDataSinks().get(1);
        assertEquals("audit-file-sink", fileSink.getName());
        assertEquals("file-system", fileSink.getType());
        assertEquals("json", fileSink.getSourceType());
        assertNotNull(fileSink.getConnection());
        assertNotNull(fileSink.getOperations());
        assertNotNull(fileSink.getOutputFormat());
        assertNotNull(fileSink.getBatch());
        
        LOGGER.info("✓ Complete YAML configuration with data sinks test passed");
    }
    
    @Test
    void testDataSinkYamlSyntaxConventions() throws Exception {
        LOGGER.info("TEST: Testing APEX YAML syntax conventions for data sinks");
        
        String yamlContent = """
            metadata:
              name: "APEX Syntax Convention Test"
              version: "1.0.0"
              author: "test.team@company.com"
            
            data-sinks:
              - name: "syntax-test-sink"
                type: "database"
                source-type: "postgresql"
                enabled: true
                
                # Test kebab-case property names (APEX convention)
                connection:
                  host: "localhost"
                  port: 5432
                  database: "testdb"
                  username: "testuser"
                  password: "testpass"
                  connection-pool:
                    max-size: 10
                    min-size: 2
                    connection-timeout: 30000
                
                health-check:
                  enabled: true
                  interval-seconds: 30
                  timeout-seconds: 5
                
                circuit-breaker:
                  enabled: true
                  failure-threshold: 5
                  timeout-seconds: 60
                
                error-handling:
                  strategy: "retry-and-continue"
                  max-retries: 3
                  retry-delay: 1000
                  dead-letter-enabled: true
                  dead-letter-table: "failed_records"
                
                output-format:
                  format: "sql"
                  date-format: "yyyy-MM-dd HH:mm:ss"
                  include-timestamp: true
                
                custom-properties:
                  custom-setting-one: "value1"
                  custom-setting-two: "value2"
                
                parameter-names: ["id", "name", "email", "timestamp"]
            """;
        
        // Parse YAML
        YamlRuleConfiguration config = yamlMapper.readValue(yamlContent, YamlRuleConfiguration.class);
        
        // Verify parsing succeeded
        assertNotNull(config);
        assertNotNull(config.getDataSinks());
        assertEquals(1, config.getDataSinks().size());
        
        YamlDataSink sink = config.getDataSinks().get(0);
        
        // Verify kebab-case properties were parsed correctly
        assertNotNull(sink.getConnection());
        assertNotNull(sink.getHealthCheck());
        assertNotNull(sink.getCircuitBreaker());
        assertNotNull(sink.getErrorHandling());
        assertNotNull(sink.getOutputFormat());
        assertNotNull(sink.getCustomProperties());
        assertNotNull(sink.getParameterNames());
        
        // Verify nested kebab-case properties
        assertTrue(sink.getConnection().containsKey("connection-pool"));
        assertTrue(sink.getHealthCheck().containsKey("interval-seconds"));
        assertTrue(sink.getCircuitBreaker().containsKey("failure-threshold"));
        assertTrue(sink.getErrorHandling().containsKey("max-retries"));
        assertTrue(sink.getOutputFormat().containsKey("date-format"));
        
        // Verify parameter names array
        assertEquals(4, sink.getParameterNames().length);
        assertEquals("id", sink.getParameterNames()[0]);
        assertEquals("timestamp", sink.getParameterNames()[3]);
        
        LOGGER.info("✓ APEX YAML syntax conventions test passed");
    }
}
