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

package dev.mars.apex.sync.unit.comparison;

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

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.HashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests nullable constraint changes via YAML-configured pipeline.
 */
public class NullableConversionTest extends SyncTestBase {
    private static final Logger logger = LoggerFactory.getLogger(NullableConversionTest.class);
    
    private Connection sourceConnection;
    private Connection targetConnection;

    @BeforeEach
    public void setUpTestDatabases() throws Exception {
        sourceConnection = DriverManager.getConnection(
            "jdbc:h2:mem:nullable_source;DB_CLOSE_DELAY=-1;MODE=PostgreSQL", "sa", "");
        
        try (Statement stmt = sourceConnection.createStatement()) {
            stmt.execute("DROP TABLE IF EXISTS contacts");
            stmt.execute("CREATE TABLE contacts (" +
                    "id INTEGER PRIMARY KEY, " +
                    "email VARCHAR(255)" +
                    ")");
        }
        
        targetConnection = DriverManager.getConnection(
            "jdbc:h2:mem:nullable_target;DB_CLOSE_DELAY=-1;MODE=PostgreSQL", "sa", "");
        
        try (Statement stmt = targetConnection.createStatement()) {
            stmt.execute("DROP TABLE IF EXISTS contacts");
            stmt.execute("CREATE TABLE contacts (" +
                    "id INTEGER PRIMARY KEY, " +
                    "email VARCHAR(255) NOT NULL" +
                    ")");
        }
        
        logger.info("Created test databases for nullable conversion test");
    }

    @AfterEach
    public void tearDownDatabases() throws Exception {
        if (sourceConnection != null) sourceConnection.close();
        if (targetConnection != null) targetConnection.close();
    }

    @Test
    public void shouldDetectNullableToNotNullConversion() throws Exception {
        RulesEngine rulesEngine = RulesEngine.fromFile(
            "src/test/java/dev/mars/apex/sync/unit/comparison/NullableConversionTest.yaml");
        RuleResult result = rulesEngine.evaluate(new HashMap<>());
        assertTrue(result.isSuccess());

        List<ExecutionStep> steps = result.getExecutionPath().stream()
            .filter(step -> "PIPELINE_STEP".equals(step.getType()))
            .toList();

        SchemaMetadata sourceSchema = (SchemaMetadata) steps.get(0).getStepData();
        SchemaMetadata targetSchema = (SchemaMetadata) steps.get(1).getStepData();
        
        var sourceEmail = sourceSchema.getColumns().stream()
            .filter(col -> "EMAIL".equalsIgnoreCase(col.getName()))
            .findFirst()
            .orElse(null);
        var targetEmail = targetSchema.getColumns().stream()
            .filter(col -> "EMAIL".equalsIgnoreCase(col.getName()))
            .findFirst()
            .orElse(null);
        
        assertNotNull(sourceEmail);
        assertNotNull(targetEmail);
        
        assertTrue(sourceEmail.isNullable(), "Source email should be nullable");
        assertFalse(targetEmail.isNullable(), "Target email should NOT be nullable");
        
        logger.info("✓ Nullable to NOT NULL conversion detected");
        validateExecutionRate(2, 2, "Nullable conversion");
    }

    @Test
    public void shouldValidatePrimaryKeyNullability() throws Exception {
        RulesEngine rulesEngine = RulesEngine.fromFile(
            "src/test/java/dev/mars/apex/sync/unit/comparison/NullableConversionTest.yaml");
        RuleResult result = rulesEngine.evaluate(new HashMap<>());
        assertTrue(result.isSuccess());

        List<ExecutionStep> steps = result.getExecutionPath().stream()
            .filter(step -> "PIPELINE_STEP".equals(step.getType()))
            .toList();

        SchemaMetadata sourceSchema = (SchemaMetadata) steps.get(0).getStepData();
        
        var idColumn = sourceSchema.getColumns().stream()
            .filter(col -> "ID".equalsIgnoreCase(col.getName()))
            .findFirst()
            .orElse(null);
        
        assertNotNull(idColumn);
        assertFalse(idColumn.isNullable(), "Primary key should not be nullable");
        
        logger.info("✓ Primary key nullability validated");
        validateExecutionRate(2, 2, "PK nullability");
    }
}
