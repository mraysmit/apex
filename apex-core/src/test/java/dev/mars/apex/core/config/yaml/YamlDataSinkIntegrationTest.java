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
import dev.mars.apex.core.service.data.external.factory.DataSinkFactory;
import dev.mars.apex.core.service.data.external.DataSink;
import dev.mars.apex.core.service.data.external.DataSinkType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for YAML data sink configuration.
 * 
 * Tests the complete integration from YAML parsing through
 * DataSinkConfiguration creation to actual DataSink instantiation.
 * 
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 1.0.0
 * @version 1.0
 */
public class YamlDataSinkIntegrationTest {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(YamlDataSinkIntegrationTest.class);
    
    private ObjectMapper yamlMapper;
    private DataSinkFactory dataSinkFactory;
    
    @BeforeEach
    void setUp() {
        yamlMapper = new ObjectMapper(new YAMLFactory());
        dataSinkFactory = DataSinkFactory.getInstance();
    }
    
    @Test
    void testCompleteYamlToDataSinkIntegration() throws Exception {
        LOGGER.info("TEST: Complete YAML to DataSink integration");
        
        String yamlContent = """
            metadata:
              name: "Integration Test Configuration"
              version: "1.0.0"
              description: "Integration test for YAML data sink configuration"
              author: "test.team@company.com"
            
            data-sinks:
              - name: "test-h2-database-sink"
                type: "database"
                source-type: "h2"
                enabled: true
                description: "Test H2 database sink for integration testing"
                tags: ["test", "integration", "h2"]
                
                connection:
                  database: "./target/test/integration_test_db"
                  username: "sa"
                  password: ""
                  mode: "PostgreSQL"
                
                operations:
                  insertTestRecord: "INSERT INTO test_records (id, name, value, created_at) VALUES (:id, :name, :value, :createdAt)"
                  updateTestRecord: "UPDATE test_records SET name = :name, value = :value WHERE id = :id"
                
                schema:
                  auto-create: true
                  table-name: "test_records"
                  init-script: |
                    CREATE TABLE IF NOT EXISTS test_records (
                      id INTEGER PRIMARY KEY,
                      name VARCHAR(255) NOT NULL,
                      value VARCHAR(255),
                      created_at TIMESTAMP
                    );
                
                error-handling:
                  strategy: "log-and-continue"
                  max-retries: 3
                  retry-delay: 1000
                
                batch:
                  enabled: true
                  batch-size: 10
                  timeout-ms: 5000
              
              - name: "test-file-system-sink"
                type: "file-system"
                source-type: "json"
                enabled: true
                description: "Test file system sink for integration testing"
                tags: ["test", "integration", "file"]
                
                connection:
                  base-path: "./target/test/output"
                  file-pattern: "test_output_{timestamp}.json"
                  encoding: "UTF-8"
                
                operations:
                  writeJsonRecord: "WRITE_JSON"
                  appendJsonRecord: "APPEND_JSON"
                
                output-format:
                  format: "json"
                  pretty-print: true
                  encoding: "UTF-8"
                
                batch:
                  enabled: true
                  batch-size: 50
                  flush-interval-ms: 2000
            """;
        
        // Step 1: Parse YAML configuration
        YamlRuleConfiguration yamlConfig = yamlMapper.readValue(yamlContent, YamlRuleConfiguration.class);
        
        assertNotNull(yamlConfig);
        assertNotNull(yamlConfig.getMetadata());
        assertNotNull(yamlConfig.getDataSinks());
        assertEquals(2, yamlConfig.getDataSinks().size());
        
        LOGGER.info("✓ YAML parsing successful");
        
        // Step 2: Convert YAML data sinks to DataSinkConfiguration objects
        YamlDataSink yamlDbSink = yamlConfig.getDataSinks().get(0);
        YamlDataSink yamlFileSink = yamlConfig.getDataSinks().get(1);
        
        DataSinkConfiguration dbSinkConfig = yamlDbSink.toDataSinkConfiguration();
        DataSinkConfiguration fileSinkConfig = yamlFileSink.toDataSinkConfiguration();
        
        // Verify database sink configuration
        assertNotNull(dbSinkConfig);
        assertEquals("test-h2-database-sink", dbSinkConfig.getName());
        assertEquals("database", dbSinkConfig.getType());
        assertEquals("h2", dbSinkConfig.getSourceType());
        assertEquals(DataSinkType.DATABASE, dbSinkConfig.getSinkType());
        assertTrue(dbSinkConfig.isEnabled());
        
        // Verify file sink configuration
        assertNotNull(fileSinkConfig);
        assertEquals("test-file-system-sink", fileSinkConfig.getName());
        assertEquals("file-system", fileSinkConfig.getType());
        assertEquals("json", fileSinkConfig.getSourceType());
        assertEquals(DataSinkType.FILE_SYSTEM, fileSinkConfig.getSinkType());
        assertTrue(fileSinkConfig.isEnabled());
        
        LOGGER.info("✓ DataSinkConfiguration conversion successful");
        
        // Step 3: Create actual DataSink instances using the factory
        // Note: We create without initialization to avoid database/file system dependencies in unit tests
        DataSink dbSink = dataSinkFactory.createDataSinkWithoutInitialization(dbSinkConfig);
        DataSink fileSink = dataSinkFactory.createDataSinkWithoutInitialization(fileSinkConfig);
        
        // Verify database sink instance
        assertNotNull(dbSink);
        assertEquals(DataSinkType.DATABASE, dbSink.getSinkType());
        assertEquals("test-h2-database-sink", dbSink.getName());
        
        // Verify file sink instance
        assertNotNull(fileSink);
        assertEquals(DataSinkType.FILE_SYSTEM, fileSink.getSinkType());
        assertEquals("test-file-system-sink", fileSink.getName());
        
        LOGGER.info("✓ DataSink instantiation successful");
        
        // Step 4: Verify supported operations
        assertTrue(dbSink.supportsOperation("insert"));
        assertTrue(dbSink.supportsOperation("update"));
        assertTrue(dbSink.supportsOperation("batch"));
        
        assertTrue(fileSink.supportsOperation("write"));
        assertTrue(fileSink.supportsOperation("append"));
        assertTrue(fileSink.supportsOperation("overwrite"));
        
        LOGGER.info("✓ Operation support verification successful");
        
        // Step 5: Verify configuration properties are accessible
        assertEquals(dbSinkConfig, dbSink.getConfiguration());
        assertEquals(fileSinkConfig, fileSink.getConfiguration());
        
        // Verify operations were properly converted
        assertNotNull(dbSinkConfig.getOperations());
        assertTrue(dbSinkConfig.getOperations().containsKey("insertTestRecord"));
        assertTrue(dbSinkConfig.getOperations().containsKey("updateTestRecord"));
        
        assertNotNull(fileSinkConfig.getOperations());
        assertTrue(fileSinkConfig.getOperations().containsKey("writeJsonRecord"));
        assertTrue(fileSinkConfig.getOperations().containsKey("appendJsonRecord"));
        
        LOGGER.info("✓ Configuration property verification successful");
        
        LOGGER.info("✓ Complete YAML to DataSink integration test passed");
    }
    
    @Test
    void testYamlDataSinkSyntaxValidation() throws Exception {
        LOGGER.info("TEST: YAML data sink syntax validation");
        
        String yamlContent = """
            metadata:
              name: "Syntax Validation Test"
              version: "1.0.0"
              author: "test.team@company.com"
            
            data-sinks:
              - name: "syntax-validation-sink"
                type: "database"
                source-type: "postgresql"
                enabled: true
                description: "Test APEX YAML syntax conventions"
                
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
                
                tags: ["syntax", "validation", "test"]
            """;
        
        // Parse YAML and verify all kebab-case properties are handled correctly
        YamlRuleConfiguration config = yamlMapper.readValue(yamlContent, YamlRuleConfiguration.class);
        
        assertNotNull(config);
        assertNotNull(config.getDataSinks());
        assertEquals(1, config.getDataSinks().size());
        
        YamlDataSink sink = config.getDataSinks().get(0);
        
        // Verify all kebab-case properties were parsed
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
        
        // Verify arrays and complex properties
        assertEquals(4, sink.getParameterNames().length);
        assertEquals("id", sink.getParameterNames()[0]);
        assertEquals("timestamp", sink.getParameterNames()[3]);
        
        // Convert to DataSinkConfiguration and verify
        DataSinkConfiguration sinkConfig = sink.toDataSinkConfiguration();
        assertNotNull(sinkConfig);
        assertEquals("syntax-validation-sink", sinkConfig.getName());
        assertEquals("database", sinkConfig.getType());
        assertEquals("postgresql", sinkConfig.getSourceType());
        
        LOGGER.info("✓ YAML data sink syntax validation test passed");
    }
    
    @Test
    void testDataSinkFactoryIntegration() throws Exception {
        LOGGER.info("TEST: DataSink factory integration");
        
        // Test that the factory can handle different sink types
        DataSinkConfiguration dbConfig = new DataSinkConfiguration("test-db", "database");
        dbConfig.setSourceType("h2");
        dbConfig.setEnabled(true);
        
        DataSinkConfiguration fileConfig = new DataSinkConfiguration("test-file", "file-system");
        fileConfig.setSourceType("json");
        fileConfig.setEnabled(true);
        
        // Verify factory can create different types
        assertTrue(dataSinkFactory.isTypeSupported(DataSinkType.DATABASE));
        assertTrue(dataSinkFactory.isTypeSupported(DataSinkType.FILE_SYSTEM));
        
        // Create instances without initialization
        DataSink dbSink = dataSinkFactory.createDataSinkWithoutInitialization(dbConfig);
        DataSink fileSink = dataSinkFactory.createDataSinkWithoutInitialization(fileConfig);
        
        assertNotNull(dbSink);
        assertNotNull(fileSink);
        assertEquals(DataSinkType.DATABASE, dbSink.getSinkType());
        assertEquals(DataSinkType.FILE_SYSTEM, fileSink.getSinkType());
        
        LOGGER.info("✓ DataSink factory integration test passed");
    }
}
