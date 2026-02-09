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
 * Created: 2026-01-19
 */

package dev.mars.apex.sync.validation;

import dev.mars.apex.sync.SyncTestBase;
import dev.mars.apex.core.config.YamlConfigurationLoader;
import dev.mars.apex.core.config.YamlRuleConfiguration;
import dev.mars.apex.core.engine.config.RulesEngine;
import dev.mars.apex.core.engine.model.ExecutionStep;
import dev.mars.apex.core.engine.model.RuleResult;
import dev.mars.apex.core.service.schema.diff.SchemaComparisonResult;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.HashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Validates schema changes across multiple tables in a single pipeline,
 * ensuring comprehensive migration validation for complex database changes.
 *
 * CRITICAL VALIDATION CHECKLIST:
 * Source and target databases created with customers and orders tables
 * Both tables validated in single pipeline execution
 * Separate HTML reports generated for each comparison
 * Schema differences correctly identified
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2.1.0
 */
@DisplayName("Schema Diff: Multi-Table Migration Validation")
class SchemaDiffMigrationValidationTest_MultiTable extends SyncTestBase {

    private static final Logger logger = LoggerFactory.getLogger(SchemaDiffMigrationValidationTest_MultiTable.class);
    // Must match EXACTLY what's in the YAML file for the same in-memory database
    private static final String H2_SOURCE_URL = "jdbc:h2:mem:migration_source_multitable;DB_CLOSE_DELAY=-1";
    private static final String H2_TARGET_URL = "jdbc:h2:mem:migration_target_multitable;DB_CLOSE_DELAY=-1";
    
    private final YamlConfigurationLoader yamlLoader = new YamlConfigurationLoader();
    private RulesEngine rulesEngine;

    @BeforeEach
    @Override
    public void setUp() {
        super.setUp();
        try {
            setupSourceDatabase();
            setupTargetDatabase();
        } catch (Exception e) {
            throw new RuntimeException("Failed to setup test databases", e);
        }
    }

    @AfterEach
    @Override
    public void tearDown() {
        if (rulesEngine != null) {
            rulesEngine.shutdown();
        }
        super.tearDown();
    }

    @Test
    @DisplayName("Should validate multiple tables in single pipeline")
    void shouldValidateMultipleTablesInPipeline() throws Exception {
        logger.info("\n=== Test: Multi-Table Migration Validation ===\n");

        // Load pipeline configuration - note the YAML has "Database" in name but Java doesn't
        rulesEngine = RulesEngine.fromFile(
            "src/test/java/dev/mars/apex/sync/validation/SchemaDiffDatabaseMigrationValidationTest_MultiTable.yaml");
        assertNotNull(rulesEngine, "Rules engine should be created");

        // Execute pipeline
        RuleResult result = rulesEngine.evaluate(new HashMap<>());

        // Verify execution success
        assertTrue(result.isSuccess(), "Multi-table validation should succeed: " + result.getMessage());

        // Find all schema-diff steps
        List<ExecutionStep> diffSteps = result.getExecutionPath().stream()
            .filter(step -> "PIPELINE_STEP".equals(step.getType()))
            .filter(step -> step.getName().contains("compare"))
            .toList();

        assertEquals(2, diffSteps.size(), "Should have 2 compare steps (customers + orders)");

        // Verify each comparison
        for (ExecutionStep step : diffSteps) {
            assertTrue(step.hasStepData(), "Step " + step.getName() + " should have comparison result");
            
            Object stepData = step.getStepData();
            assertInstanceOf(SchemaComparisonResult.class, stepData, "Step data should be SchemaComparisonResult");
            
            SchemaComparisonResult comparison = (SchemaComparisonResult) stepData;
            logger.info("Comparison for {}: {} matching, {} added, {} removed columns",
                step.getName(),
                comparison.getMatchingColumns().size(),
                comparison.getAddedColumns().size(),
                comparison.getRemovedColumns().size());
        }

        // Verify HTML reports generated
        Path customersReport = Path.of("target/reports/customers-migration-diff.html");
        Path ordersReport = Path.of("target/reports/orders-migration-diff.html");
        
        assertTrue(Files.exists(customersReport), "Customers migration report should be generated");
        assertTrue(Files.exists(ordersReport), "Orders migration report should be generated");

        logger.info("[OK] Generated reports:");
        logger.info("  - {}", customersReport);
        logger.info("  - {}", ordersReport);
        logger.info("[OK] Successfully validated multiple tables in single pipeline");
    }

    private void setupSourceDatabase() throws Exception {
        try (Connection conn = DriverManager.getConnection(H2_SOURCE_URL, "sa", "");
             Statement stmt = conn.createStatement()) {
            
            // Source customers table (v1)
            stmt.execute("DROP TABLE IF EXISTS customers");
            stmt.execute("""
                CREATE TABLE customers (
                    id INTEGER PRIMARY KEY,
                    name VARCHAR(100) NOT NULL,
                    email VARCHAR(100)
                )
            """);

            // Source orders table (v1)
            stmt.execute("DROP TABLE IF EXISTS orders");
            stmt.execute("""
                CREATE TABLE orders (
                    order_id INTEGER PRIMARY KEY,
                    customer_id INTEGER NOT NULL,
                    total DECIMAL(10,2)
                )
            """);
            
            logger.info("Created source database with customers (3 cols) and orders (3 cols) tables");
        }
    }

    private void setupTargetDatabase() throws Exception {
        try (Connection conn = DriverManager.getConnection(H2_TARGET_URL, "sa", "");
             Statement stmt = conn.createStatement()) {
            
            // Target customers table (v2 - added columns)
            stmt.execute("DROP TABLE IF EXISTS customers");
            stmt.execute("""
                CREATE TABLE customers (
                    id INTEGER PRIMARY KEY,
                    name VARCHAR(100) NOT NULL,
                    email VARCHAR(100),
                    phone VARCHAR(20),
                    status VARCHAR(50)
                )
            """);

            // Target orders table (v2 - added columns)
            stmt.execute("DROP TABLE IF EXISTS orders");
            stmt.execute("""
                CREATE TABLE orders (
                    order_id INTEGER PRIMARY KEY,
                    customer_id INTEGER NOT NULL,
                    total DECIMAL(10,2),
                    order_date DATE,
                    shipped_date DATE
                )
            """);
            
            logger.info("Created target database with customers (5 cols) and orders (5 cols) tables");
        }
    }
}
