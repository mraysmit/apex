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

package dev.mars.apex.sync.pipeline;

import dev.mars.apex.core.config.loader.ConfigurationLoader;
import dev.mars.apex.core.config.model.YamlRuleConfiguration;
import dev.mars.apex.engine.core.RulesEngine;
import dev.mars.apex.engine.model.ExecutionStep;
import dev.mars.apex.engine.model.RuleResult;
import dev.mars.apex.sync.ColoredTestOutputExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;


/**
 * Integration test for Data Transformation during SQL Server to PostgreSQL migration.
 * Uses H2 compatibility modes for zero-infrastructure testing.
 * 
 * <p>Demonstrates APEX transformation capabilities:
 * <ul>
 *   <li>String concatenation (first_name + last_name → full_name)</li>
 *   <li>Field value transformation (uppercase, lowercase)</li>
 *   <li>Calculated fields (SpEL expressions)</li>
 * </ul>
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2.1.0
 */
@ExtendWith(ColoredTestOutputExtension.class)
public class DataTransformationH2Test {

    private static final Logger log = LoggerFactory.getLogger(DataTransformationH2Test.class);

    // H2 in SQL Server compatibility mode (source)
    private static final String SOURCE_URL = "jdbc:h2:mem:transform_source;MODE=MSSQLServer;DB_CLOSE_DELAY=-1";
    // H2 in PostgreSQL compatibility mode (target)
    private static final String TARGET_URL = "jdbc:h2:mem:transform_target;MODE=PostgreSQL;DB_CLOSE_DELAY=-1";

    @BeforeEach
    void setupDatabases() throws Exception {
        // Setup source database (simulating SQL Server)
        try (Connection conn = DriverManager.getConnection(SOURCE_URL, "sa", "")) {
            try (Statement stmt = conn.createStatement()) {
                // Drop and recreate for clean state
                stmt.execute("DROP TABLE IF EXISTS employees");
                stmt.execute("""
                    CREATE TABLE employees (
                        id INT PRIMARY KEY,
                        first_name VARCHAR(100),
                        last_name VARCHAR(100),
                        email VARCHAR(255),
                        department VARCHAR(50),
                        salary DECIMAL(10,2)
                    )
                """);
                
                // Insert test data
                stmt.execute("INSERT INTO employees VALUES (1, 'John', 'Doe', 'john.doe@company.com', 'Engineering', 75000.00)");
                stmt.execute("INSERT INTO employees VALUES (2, 'Jane', 'Smith', 'jane.smith@company.com', 'Marketing', 65000.00)");
                stmt.execute("INSERT INTO employees VALUES (3, 'Bob', 'Johnson', 'bob.johnson@company.com', 'Engineering', 80000.00)");
                stmt.execute("INSERT INTO employees VALUES (4, 'Alice', 'Williams', 'alice.williams@company.com', 'Sales', 70000.00)");
            }
        }
        log.info("[OK] Source database (SQL Server mode) initialized with 4 employees");

        // Setup target database (simulating PostgreSQL) - empty, will be populated by sync
        try (Connection conn = DriverManager.getConnection(TARGET_URL, "sa", "")) {
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("DROP TABLE IF EXISTS employees_transformed");
                stmt.execute("""
                    CREATE TABLE employees_transformed (
                        id INT PRIMARY KEY,
                        full_name VARCHAR(200),
                        email_lower VARCHAR(255),
                        department_upper VARCHAR(50),
                        annual_bonus DECIMAL(10,2)
                    )
                """);
            }
        }
        log.info("[OK] Target database (PostgreSQL mode) initialized with empty transformed table");
    }

    @Test
    @DisplayName("Should transform data during SQL Server to PostgreSQL sync")
    void testDataTransformationSync() throws Exception {
        log.info("\n=== Data Transformation Test: SQL Server → PostgreSQL (H2 Mode) ===\n");

        // Load YAML configuration and run pipeline
        ConfigurationLoader loader = new ConfigurationLoader();
        YamlRuleConfiguration yamlConfig = loader.loadFromFile(
            "src/test/java/dev/mars/apex/sync/pipeline/DataTransformationH2Test.yaml"
        );
        
        RulesEngine engine = RulesEngine.fromYamlConfig(yamlConfig);
        RuleResult result = engine.evaluate(new HashMap<>());

        // Verify pipeline execution
        assertTrue(result.isSuccess(), "Pipeline failed: " + result.getMessage());
        log.info("[OK] Pipeline executed successfully");

        // Print execution metrics
        printExecutionMetrics(result);

        // Verify transformed data in target
        verifyTransformedData();
        
        engine.shutdown();
    }

    private void printExecutionMetrics(RuleResult result) {
        log.info("\n=== Pipeline Execution Metrics ===");
        for (ExecutionStep step : result.getExecutionPath()) {
            if ("PIPELINE_STEP".equals(step.getType())) {
                log.info("Step: {} - Status: {} - Duration: {} ms",
                    step.getName(), step.getStatus(), step.getDurationMs());
                
                if (step.getRecordsProcessed() != null) {
                    log.info("  Records Processed: {}", step.getRecordsProcessed());
                }
                if (step.getRecordsFailed() != null && step.getRecordsFailed() > 0) {
                    log.info("  Records Failed: {}", step.getRecordsFailed());
                }
            }
        }
    }

    private void verifyTransformedData() throws Exception {
        log.info("\n=== Verifying Transformed Data ===");
        
        try (Connection conn = DriverManager.getConnection(TARGET_URL, "sa", "")) {
            try (Statement stmt = conn.createStatement()) {
                ResultSet rs = stmt.executeQuery(
                    "SELECT id, full_name, email_lower, department_upper, annual_bonus " +
                    "FROM employees_transformed ORDER BY id"
                );

                int count = 0;
                while (rs.next()) {
                    count++;
                    int id = rs.getInt("id");
                    String fullName = rs.getString("full_name");
                    String emailLower = rs.getString("email_lower");
                    String deptUpper = rs.getString("department_upper");
                    double bonus = rs.getDouble("annual_bonus");

                    log.info("Record {}: full_name='{}', email_lower='{}', dept_upper='{}', bonus={}",
                        id, fullName, emailLower, deptUpper, bonus);

                    // Verify transformations
                    assertNotNull(fullName, "full_name should not be null");
                    assertTrue(fullName.contains(" "), "full_name should contain space (first + last)");
                    
                    assertEquals(emailLower, emailLower.toLowerCase(), "email should be lowercase");
                    assertEquals(deptUpper, deptUpper.toUpperCase(), "department should be uppercase");
                    assertTrue(bonus > 0, "annual_bonus should be calculated");
                }

                assertEquals(4, count, "Should have transformed all 4 employees");
                log.info("[OK] All {} records verified with correct transformations", count);
            }
        }
    }

    @Test
    @DisplayName("Should handle string concatenation transformation")
    void testStringConcatenation() throws Exception {
        log.info("\n=== String Concatenation Transformation Test ===\n");

        // Run the pipeline
        ConfigurationLoader loader = new ConfigurationLoader();
        YamlRuleConfiguration yamlConfig = loader.loadFromFile(
            "src/test/java/dev/mars/apex/sync/pipeline/DataTransformationH2Test.yaml"
        );
        
        RulesEngine engine = RulesEngine.fromYamlConfig(yamlConfig);
        RuleResult result = engine.evaluate(new HashMap<>());
        assertTrue(result.isSuccess());

        // Verify concatenation: first_name + ' ' + last_name → full_name
        try (Connection conn = DriverManager.getConnection(TARGET_URL, "sa", "")) {
            try (Statement stmt = conn.createStatement()) {
                ResultSet rs = stmt.executeQuery(
                    "SELECT full_name FROM employees_transformed WHERE id = 1"
                );
                assertTrue(rs.next());
                assertEquals("John Doe", rs.getString("full_name"), 
                    "full_name should be concatenation of first_name and last_name");
                log.info("[OK] String concatenation verified: 'John' + ' ' + 'Doe' = 'John Doe'");
            }
        }
        
        engine.shutdown();
    }

    @Test
    @DisplayName("Should calculate annual bonus from salary")
    void testCalculatedField() throws Exception {
        log.info("\n=== Calculated Field Test (SpEL Expression) ===\n");

        // Run the pipeline
        ConfigurationLoader loader = new ConfigurationLoader();
        YamlRuleConfiguration yamlConfig = loader.loadFromFile(
            "src/test/java/dev/mars/apex/sync/pipeline/DataTransformationH2Test.yaml"
        );
        
        RulesEngine engine = RulesEngine.fromYamlConfig(yamlConfig);
        RuleResult result = engine.evaluate(new HashMap<>());
        assertTrue(result.isSuccess());

        // Verify calculation: salary * 0.10 → annual_bonus (10% bonus)
        try (Connection conn = DriverManager.getConnection(TARGET_URL, "sa", "")) {
            try (Statement stmt = conn.createStatement()) {
                // John Doe has salary 75000, bonus should be 7500
                ResultSet rs = stmt.executeQuery(
                    "SELECT annual_bonus FROM employees_transformed WHERE id = 1"
                );
                assertTrue(rs.next());
                double bonus = rs.getDouble("annual_bonus");
                assertEquals(7500.00, bonus, 0.01, 
                    "annual_bonus should be 10% of salary (75000 * 0.10 = 7500)");
                log.info("[OK] Calculated field verified: salary 75000 × 0.10 = bonus {}", bonus);
            }
        }
        
        engine.shutdown();
    }
}
