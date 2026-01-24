/*
 * Copyright 2026 Mark Andrew Ray-Smith Cityline Ltd
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
 *
 * Created: 2026-01-18
 */

package dev.mars.apex.sync.unit.serialization;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.mars.apex.sync.SyncTestBase;
import dev.mars.apex.core.engine.config.RulesEngine;
import dev.mars.apex.core.engine.model.RuleResult;
import dev.mars.apex.core.engine.model.ExecutionStep;
import dev.mars.apex.core.service.schema.SchemaMetadata;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.HashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests JSON serialization by executing YAML-configured pipeline that generates JSON reports.
 * Validates JSON structure and content produced by APEX pipeline operations.
 */
public class JsonSerializationTest extends SyncTestBase {
    private static final Logger logger = LoggerFactory.getLogger(JsonSerializationTest.class);
    
    private Connection sourceConnection;
    private Connection targetConnection;

    @BeforeEach
    public void setUpTestDatabases() throws Exception {
        // Create source database
        sourceConnection = DriverManager.getConnection(
            "jdbc:h2:mem:json_test_source;DB_CLOSE_DELAY=-1;MODE=PostgreSQL", "sa", "");
        
        try (Statement stmt = sourceConnection.createStatement()) {
            stmt.execute("DROP TABLE IF EXISTS customers");
            stmt.execute("CREATE TABLE customers (" +
                    "id INTEGER PRIMARY KEY, " +
                    "name VARCHAR(200) NOT NULL, " +
                    "email VARCHAR(255)" +
                    ")");
            stmt.execute("INSERT INTO customers VALUES (1, 'John Doe', 'john@example.com')");
        }
        
        // Create target database with slightly different schema
        targetConnection = DriverManager.getConnection(
            "jdbc:h2:mem:json_test_target;DB_CLOSE_DELAY=-1;MODE=PostgreSQL", "sa", "");
        
        try (Statement stmt = targetConnection.createStatement()) {
            stmt.execute("DROP TABLE IF EXISTS customers");
            stmt.execute("CREATE TABLE customers (" +
                    "id INTEGER PRIMARY KEY, " +
                    "name VARCHAR(200) NOT NULL, " +
                    "email VARCHAR(255), " +
                    "created_date TIMESTAMP" +
                    ")");
        }
        
        logger.info("Created test databases for JSON serialization test");
    }

    @AfterEach
    public void tearDownDatabases() throws Exception {
        if (sourceConnection != null) sourceConnection.close();
        if (targetConnection != null) targetConnection.close();
        
        // Clean up generated report
        File report = new File("target/test-reports/schema-diff.json");
        if (report.exists()) {
            report.delete();
        }
    }

    @Test
    public void shouldSerializeSchemaMetadataToJson() throws Exception {
        // Execute YAML-configured pipeline
        RulesEngine rulesEngine = RulesEngine.fromFile("src/test/java/dev/mars/apex/sync/unit/serialization/JsonSerializationTest.yaml");
        assertNotNull(rulesEngine, "RulesEngine should be initialized");

        RuleResult result = rulesEngine.evaluate(new HashMap<>());
        assertNotNull(result, "RuleResult should not be null");
        assertTrue(result.isSuccess(), "Pipeline should execute successfully: " + result.getMessage());

        // Get schema from execution step
        List<ExecutionStep> steps = result.getExecutionPath().stream()
            .filter(step -> "PIPELINE_STEP".equals(step.getType()))
            .toList();

        assertFalse(steps.isEmpty(), "Should have pipeline steps");

        ExecutionStep readSchemaStep = steps.stream()
            .filter(step -> step.getName().contains("read-schema"))
            .findFirst()
            .orElse(null);

        assertNotNull(readSchemaStep, "Should have read-schema step");
        assertTrue(readSchemaStep.hasStepData(), "Read-schema step should have data");

        Object stepData = readSchemaStep.getStepData();
        assertInstanceOf(SchemaMetadata.class, stepData, "Step data should be SchemaMetadata");

        SchemaMetadata schema = (SchemaMetadata) stepData;

        // Serialize to JSON
        ObjectMapper mapper = new ObjectMapper();
        String json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(schema);
        
        assertNotNull(json, "JSON should not be null");
        assertFalse(json.isEmpty(), "JSON should not be empty");
        assertTrue(json.contains("sourceName"), "JSON should have sourceName field");
        assertTrue(json.contains("columns"), "JSON should contain columns array");

        // Parse back and validate structure
        JsonNode rootNode = mapper.readTree(json);
        assertTrue(rootNode.has("sourceName"), "JSON should have sourceName field");
        assertTrue(rootNode.has("sourceType"), "JSON should have sourceType field");
        assertTrue(rootNode.has("columns"), "JSON should have columns array");
        
        JsonNode columns = rootNode.get("columns");
        assertTrue(columns.isArray(), "Columns should be array");
        assertEquals(3, columns.size(), "Should have 3 columns (ID, NAME, EMAIL)");
        
        // Validate column structure
        JsonNode firstColumn = columns.get(0);
        assertTrue(firstColumn.has("name"), "Column should have name");
        assertTrue(firstColumn.has("dataType"), "Column should have dataType");
        assertTrue(firstColumn.has("nullable"), "Column should have nullable flag");

        logger.info("[OK] Schema metadata serialized to JSON successfully - {} columns",  columns.size());
        validateExecutionRate(1, 1, "Schema reading and JSON serialization");
    }

    @Test
    public void shouldSerializeColumnsWithAllDataTypes() throws Exception {
        RulesEngine rulesEngine = RulesEngine.fromFile("src/test/java/dev/mars/apex/sync/unit/serialization/JsonSerializationTest.yaml");
        RuleResult result = rulesEngine.evaluate(new HashMap<>());
        assertTrue(result.isSuccess());

        List<ExecutionStep> steps = result.getExecutionPath().stream()
            .filter(step -> "PIPELINE_STEP".equals(step.getType()))
            .toList();
        
        SchemaMetadata schema = (SchemaMetadata) steps.get(0).getStepData();
        ObjectMapper mapper = new ObjectMapper();
        JsonNode rootNode = mapper.readTree(mapper.writeValueAsString(schema));
        JsonNode columns = rootNode.get("columns");
        
        // Verify each column has required fields
        for (JsonNode column : columns) {
            assertTrue(column.has("name"), "Column should have name");
            assertTrue(column.has("dataType"), "Column should have dataType");
            assertTrue(column.has("nullable"), "Column should have nullable");
        }
        
        logger.info("[OK] All column data types serialized correctly");
        validateExecutionRate(1, 1, "Column data type serialization");
    }

    @Test
    public void shouldSerializeNullableFlags() throws Exception {
        RulesEngine rulesEngine = RulesEngine.fromFile("src/test/java/dev/mars/apex/sync/unit/serialization/JsonSerializationTest.yaml");
        RuleResult result = rulesEngine.evaluate(new HashMap<>());
        assertTrue(result.isSuccess());

        List<ExecutionStep> steps = result.getExecutionPath().stream()
            .filter(step -> "PIPELINE_STEP".equals(step.getType()))
            .toList();
        
        SchemaMetadata schema = (SchemaMetadata) steps.get(0).getStepData();
        ObjectMapper mapper = new ObjectMapper();
        JsonNode rootNode = mapper.readTree(mapper.writeValueAsString(schema));
        JsonNode columns = rootNode.get("columns");
        
        // ID column should not be nullable
        JsonNode idColumn = null;
        for (JsonNode col : columns) {
            if ("ID".equals(col.get("name").asText())) {
                idColumn = col;
                break;
            }
        }
        assertNotNull(idColumn, "ID column should exist");
        assertFalse(idColumn.get("nullable").asBoolean(), "ID should not be nullable");
        
        logger.info("[OK] Nullable flags serialized correctly");
        validateExecutionRate(1, 1, "Nullable flag serialization");
    }

    @Test
    public void shouldSerializeSourceTypeInformation() throws Exception {
        RulesEngine rulesEngine = RulesEngine.fromFile("src/test/java/dev/mars/apex/sync/unit/serialization/JsonSerializationTest.yaml");
        RuleResult result = rulesEngine.evaluate(new HashMap<>());
        assertTrue(result.isSuccess());

        List<ExecutionStep> steps = result.getExecutionPath().stream()
            .filter(step -> "PIPELINE_STEP".equals(step.getType()))
            .toList();
        
        SchemaMetadata schema = (SchemaMetadata) steps.get(0).getStepData();
        ObjectMapper mapper = new ObjectMapper();
        JsonNode rootNode = mapper.readTree(mapper.writeValueAsString(schema));
        
        assertTrue(rootNode.has("sourceType"), "Should have sourceType");
        String sourceType = rootNode.get("sourceType").asText();
        assertTrue(sourceType.contains("Database"), "Source type should indicate database");
        
        logger.info("[OK] Source type information serialized: {}", sourceType);
        validateExecutionRate(1, 1, "Source type serialization");
    }

    @Test
    public void shouldPreserveColumnOrder() throws Exception {
        RulesEngine rulesEngine = RulesEngine.fromFile("src/test/java/dev/mars/apex/sync/unit/serialization/JsonSerializationTest.yaml");
        RuleResult result = rulesEngine.evaluate(new HashMap<>());
        assertTrue(result.isSuccess());

        List<ExecutionStep> steps = result.getExecutionPath().stream()
            .filter(step -> "PIPELINE_STEP".equals(step.getType()))
            .toList();
        
        SchemaMetadata schema = (SchemaMetadata) steps.get(0).getStepData();
        ObjectMapper mapper = new ObjectMapper();
        JsonNode rootNode = mapper.readTree(mapper.writeValueAsString(schema));
        JsonNode columns = rootNode.get("columns");
        
        // Verify column order: ID, NAME, EMAIL
        assertEquals("ID", columns.get(0).get("name").asText(), "First column should be ID");
        assertEquals("NAME", columns.get(1).get("name").asText(), "Second column should be NAME");
        assertEquals("EMAIL", columns.get(2).get("name").asText(), "Third column should be EMAIL");
        
        logger.info("[OK] Column order preserved in JSON");
        validateExecutionRate(1, 1, "Column order preservation");
    }

    @Test
    public void shouldRoundTripSerialization() throws Exception {
        RulesEngine rulesEngine = RulesEngine.fromFile("src/test/java/dev/mars/apex/sync/unit/serialization/JsonSerializationTest.yaml");
        RuleResult result = rulesEngine.evaluate(new HashMap<>());
        assertTrue(result.isSuccess());

        List<ExecutionStep> steps = result.getExecutionPath().stream()
            .filter(step -> "PIPELINE_STEP".equals(step.getType()))
            .toList();
        
        SchemaMetadata original = (SchemaMetadata) steps.get(0).getStepData();
        ObjectMapper mapper = new ObjectMapper();
        
        // Serialize to JSON
        String json = mapper.writeValueAsString(original);
        
        // Deserialize back
        SchemaMetadata deserialized = mapper.readValue(json, SchemaMetadata.class);
        
        // Verify round-trip
        assertEquals(original.getSourceName(), deserialized.getSourceName());
        assertEquals(original.getColumns().size(), deserialized.getColumns().size());
        
        logger.info("[OK] Round-trip serialization successful");
        validateExecutionRate(1, 1, "Round-trip serialization");
    }
}
