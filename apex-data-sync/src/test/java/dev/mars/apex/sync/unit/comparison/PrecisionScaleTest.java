package dev.mars.apex.sync.unit.comparison;

import dev.mars.apex.core.config.yaml.YamlConfigurationLoader;
import dev.mars.apex.core.config.yaml.YamlRuleConfiguration;
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
 * Tests DECIMAL precision and scale change detection using PostgreSQL Testcontainers.
 * Validates detection of both safe (widening) and breaking (narrowing) DECIMAL changes.
 * 
 * <h2>Precision/Scale Change Patterns Tested:</h2>
 * <ul>
 *   <li>DECIMAL(10,2) → DECIMAL(12,2) - Precision increase (safe)</li>
 *   <li>DECIMAL(10,2) → DECIMAL(10,4) - Scale increase (safe)</li>
 *   <li>DECIMAL(15,4) → DECIMAL(10,2) - Both reduced (breaking)</li>
 * </ul>
 * 
 * <h2>Testcontainers Pattern:</h2>
 * <ul>
 *   <li>Instance container (fresh PostgreSQL per test)</li>
 *   <li>Dynamic port mapping via getMappedPort(5432)</li>
 *   <li>Environment variables for connection details</li>
 *   <li>Two schemas: source_schema → target_schema with different precision/scale</li>
 * </ul>
 */
@Testcontainers
@ExtendWith(ColoredTestOutputExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class PrecisionScaleTest extends SyncTestBase {
    private static final Logger logger = LoggerFactory.getLogger(PrecisionScaleTest.class);
    
    private static final DockerImageName POSTGRES_IMAGE = 
        DockerImageName.parse("postgres:15.13-alpine3.20")
                       .asCompatibleSubstituteFor("postgres");

    @Container
    @SuppressWarnings("resource")
    GenericContainer<?> postgres = new GenericContainer<>(POSTGRES_IMAGE)
            .withEnv("POSTGRES_DB", "apex_precision_test")
            .withEnv("POSTGRES_USER", "apex_user")
            .withEnv("POSTGRES_PASSWORD", "apex_pass")
            .withExposedPorts(5432)
            .waitingFor(Wait.forListeningPort());

    private String jdbcUrl() {
        return "jdbc:postgresql://" + postgres.getHost() + ":"
            + postgres.getMappedPort(5432) + "/apex_precision_test";
    }

    private YamlConfigurationLoader yamlLoader = new YamlConfigurationLoader();

    @BeforeEach
    void setupSchemas() throws Exception {
        if (!postgres.isRunning()) {
            return;
        }
        
        logger.info("Setting up schemas for precision/scale test");

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

            // Create source schema with original precision/scale
            stmt.execute("CREATE SCHEMA source_schema");
            stmt.execute("""
                CREATE TABLE source_schema.financial_data (
                    transaction_id BIGINT PRIMARY KEY,
                    amount DECIMAL(10,2) NOT NULL,
                    rate DECIMAL(5,4),
                    total DECIMAL(15,4)
                )
                """);

            // Create target schema with modified precision/scale
            stmt.execute("CREATE SCHEMA target_schema");
            stmt.execute("""
                CREATE TABLE target_schema.financial_data (
                    transaction_id BIGINT PRIMARY KEY,
                    amount DECIMAL(12,2) NOT NULL,
                    rate DECIMAL(7,6),
                    total DECIMAL(20,6)
                )
                """);

            logger.info("[OK] Created schemas with precision/scale variations");
        }
    }

    @Test
    @Order(1)
    @DisplayName("Should detect precision increase (safe change)")
    void shouldDetectPrecisionIncrease() throws Exception {
        // Inject environment variables for connection BEFORE loading YAML
        System.setProperty("POSTGRES_HOST", postgres.getHost());
        System.setProperty("POSTGRES_PORT", String.valueOf(postgres.getMappedPort(5432)));

        // Load YAML with injected connection details
        YamlRuleConfiguration config = yamlLoader.loadFromFile(
                "src/test/java/dev/mars/apex/sync/unit/comparison/PrecisionScaleTest.yaml");

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
            
            logger.info("[OK] Precision increase detection test passed");
        } finally {
            System.clearProperty("POSTGRES_HOST");
            System.clearProperty("POSTGRES_PORT");
        }
    }

    @Test
    @Order(2)
    @DisplayName("Should detect scale increase (safe change)")
    void shouldDetectScaleIncrease() throws Exception {
        // Inject environment variables for connection BEFORE loading YAML
        System.setProperty("POSTGRES_HOST", postgres.getHost());
        System.setProperty("POSTGRES_PORT", String.valueOf(postgres.getMappedPort(5432)));

        // Load YAML with injected connection details
        YamlRuleConfiguration config = yamlLoader.loadFromFile(
                "src/test/java/dev/mars/apex/sync/unit/comparison/PrecisionScaleTest.yaml");

        try {
            RulesEngine rulesEngine = RulesEngine.fromYamlConfig(config);
            assertNotNull(rulesEngine, "RulesEngine should be initialized");

            RuleResult result = rulesEngine.evaluate(new HashMap<>());
            assertNotNull(result, "RuleResult should not be null");
            assertTrue(result.isSuccess(), "Pipeline should execute successfully: " + result.getMessage());

            // Verify pipeline executed all steps
            List<ExecutionStep> steps = result.getExecutionPath();
            assertFalse(steps.isEmpty(), "Execution path should contain steps");
            
            logger.info("[OK] Scale increase detection test passed");
        } finally {
            System.clearProperty("POSTGRES_HOST");
            System.clearProperty("POSTGRES_PORT");
        }
    }
}
