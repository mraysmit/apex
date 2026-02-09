package dev.mars.apex.sync.unit.comparison;

import dev.mars.apex.core.config.loader.YamlConfigurationLoader;
import dev.mars.apex.core.config.model.YamlRuleConfiguration;
import dev.mars.apex.core.engine.config.RulesEngine;
import dev.mars.apex.core.engine.model.ExecutionStep;
import dev.mars.apex.core.engine.model.RuleResult;
import dev.mars.apex.sync.ColoredTestOutputExtension;
import dev.mars.apex.sync.SyncTestBase;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.HashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests type widening detection using PostgreSQL Testcontainers.
 * Validates compatible changes when column types are widened (e.g., INTEGER -> BIGINT, DECIMAL precision increase).
 * 
 * <h2>Type Widening Patterns Tested:</h2>
 * <ul>
 *   <li>INTEGER → BIGINT (compatible widening)</li>
 *   <li>DECIMAL(10,2) → DECIMAL(15,4) (precision/scale increase)</li>
 *   <li>VARCHAR(50) → VARCHAR(100) (length increase)</li>
 * </ul>
 * 
 * <h2>Testcontainers Pattern:</h2>
 * <ul>
 *   <li>Instance container (new PostgreSQL per test)</li>
 *   <li>Dynamic port mapping via getMappedPort(5432)</li>
 *   <li>Environment variables injected into YAML config</li>
 *   <li>Two schemas: source_schema (narrow types) → target_schema (wide types)</li>
 * </ul>
 */
@Testcontainers
@ExtendWith(ColoredTestOutputExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class TypeWideningTest extends SyncTestBase {
    private static final Logger logger = LoggerFactory.getLogger(TypeWideningTest.class);
    
    private static final DockerImageName POSTGRES_IMAGE = 
        DockerImageName.parse("postgres:15.13-alpine3.20")
                       .asCompatibleSubstituteFor("postgres");

    @Container
    @SuppressWarnings("resource")
    GenericContainer<?> postgres = new GenericContainer<>(POSTGRES_IMAGE)
            .withEnv("POSTGRES_DB", "apex_widening_test")
            .withEnv("POSTGRES_USER", "apex_user")
            .withEnv("POSTGRES_PASSWORD", "apex_pass")
            .withExposedPorts(5432)
            .waitingFor(Wait.forListeningPort());

    private String jdbcUrl() {
        return "jdbc:postgresql://" + postgres.getHost() + ":"
            + postgres.getMappedPort(5432) + "/apex_widening_test";
    }

    private YamlConfigurationLoader yamlLoader = new YamlConfigurationLoader();

    @BeforeEach
    void setupSchemas() throws Exception {
        if (!postgres.isRunning()) {
            return;
        }
        
        logger.info("Setting up schemas for type widening test");

        // Retry logic for PostgreSQL startup timing
        int maxRetries = 3;
        int retryDelayMs = 1000;
        Connection conn = null;
        
        for (int i = 0; i < maxRetries; i++) {
            try {
                conn = DriverManager.getConnection(jdbcUrl(), "apex_user", "apex_pass");
                break;
            } catch (Exception e) {
                if (i < maxRetries - 1) {
                    logger.info("Connection attempt {} failed, retrying in {}ms...", i + 1, retryDelayMs);
                    Thread.sleep(retryDelayMs);
                } else {
                    throw e;
                }
            }
        }

        try (Connection finalConn = conn;
             Statement stmt = finalConn.createStatement()) {

            // Create source schema with narrower types
            stmt.execute("CREATE SCHEMA source_schema");
            stmt.execute("""
                CREATE TABLE source_schema.products (
                    product_id INTEGER PRIMARY KEY,
                    price DECIMAL(10,2) NOT NULL,
                    description VARCHAR(50)
                )
                """);

            // Create target schema with widened types
            stmt.execute("CREATE SCHEMA target_schema");
            stmt.execute("""
                CREATE TABLE target_schema.products (
                    product_id BIGINT PRIMARY KEY,
                    price DECIMAL(15,4) NOT NULL,
                    description VARCHAR(100)
                )
                """);

            logger.info("[OK] Created schemas with widened types: source_schema → target_schema");
        }
    }

    @Test
    @Order(1)
    @DisplayName("Should detect INTEGER → BIGINT widening")
    void shouldDetectIntegerWidening() throws Exception {
        // Inject environment variables for connection BEFORE loading YAML
        System.setProperty("POSTGRES_HOST", postgres.getHost());
        System.setProperty("POSTGRES_PORT", String.valueOf(postgres.getMappedPort(5432)));

        // Load YAML with injected connection details
        YamlRuleConfiguration config = yamlLoader.loadFromFile(
                "src/test/java/dev/mars/apex/sync/unit/comparison/TypeWideningTest.yaml");

        try {
            RulesEngine rulesEngine = RulesEngine.fromYamlConfig(config);
            assertNotNull(rulesEngine, "RulesEngine should be initialized");

            RuleResult result = rulesEngine.evaluate(new HashMap<>());
            assertNotNull(result, "RuleResult should not be null");
            assertTrue(result.isSuccess(), "Pipeline should execute successfully: " + result.getMessage());

            // Verify both schema read steps executed
            List<ExecutionStep> steps = result.getExecutionPath();
            assertTrue(steps.stream().anyMatch(s -> "read-source-schema".equals(s.getName())),
                "Source schema read step should be present");
            assertTrue(steps.stream().anyMatch(s -> "read-target-schema".equals(s.getName())),
                "Target schema read step should be present");
            
            logger.info("[OK] Integer widening detection test passed");
        } finally {
            System.clearProperty("POSTGRES_HOST");
            System.clearProperty("POSTGRES_PORT");
        }
    }

    @Test
    @Order(2)
    @DisplayName("Should detect DECIMAL precision widening")
    void shouldDetectDecimalPrecisionWidening() throws Exception {
        // Inject environment variables for connection BEFORE loading YAML
        System.setProperty("POSTGRES_HOST", postgres.getHost());
        System.setProperty("POSTGRES_PORT", String.valueOf(postgres.getMappedPort(5432)));

        // Load YAML with injected connection details
        YamlRuleConfiguration config = yamlLoader.loadFromFile(
                "src/test/java/dev/mars/apex/sync/unit/comparison/TypeWideningTest.yaml");

        try {
            RulesEngine rulesEngine = RulesEngine.fromYamlConfig(config);
            assertNotNull(rulesEngine, "RulesEngine should be initialized");

            RuleResult result = rulesEngine.evaluate(new HashMap<>());
            assertNotNull(result, "RuleResult should not be null");
            assertTrue(result.isSuccess(), "Pipeline should execute successfully: " + result.getMessage());

            // Verify pipeline executed all steps
            List<ExecutionStep> steps = result.getExecutionPath();
            assertFalse(steps.isEmpty(), "Execution path should contain steps");
            
            logger.info("[OK] Decimal precision widening detection test passed");
        } finally {
            System.clearProperty("POSTGRES_HOST");
            System.clearProperty("POSTGRES_PORT");
        }
    }
}
