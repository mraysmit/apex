package dev.mars.apex.demo.database;

import dev.mars.apex.demo.DemoTestBase;
import dev.mars.apex.core.config.model.YamlRuleConfiguration;
import dev.mars.apex.core.engine.config.RulesEngine;
import dev.mars.apex.core.engine.model.RuleResult;
import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Simple Database Connectivity Test
 *
 * The most trivial test to show database connectivity works.
 */
public class H2SimpleDatabaseConnectivityTest extends DemoTestBase {

    private static final Logger logger = LoggerFactory.getLogger(H2SimpleDatabaseConnectivityTest.class);

    // Unique database name for this test class to avoid file locking conflicts
    private static final String DB_NAME = "h2_simple_connectivity_test";

    @BeforeEach
    void setupDatabase() {
        logger.info("Setting up simple database...");

        String jdbcUrl = "jdbc:h2:./target/h2-demo/" + DB_NAME;

        try (Connection connection = DriverManager.getConnection(jdbcUrl, "sa", "")) {
            Statement statement = connection.createStatement();

            statement.execute("DROP TABLE IF EXISTS test_table");
            statement.execute("CREATE TABLE test_table (id VARCHAR(10), name VARCHAR(50))");
            statement.execute("INSERT INTO test_table VALUES ('1', 'Test Name')");

            logger.info("[OK] Database setup completed");

        } catch (Exception e) {
            throw new RuntimeException("Database setup failed", e);
        }
    }

    @AfterEach
    public void tearDown() {
        // Shutdown H2 database to release file locks
        try (Connection connection = DriverManager.getConnection(
                "jdbc:h2:./target/h2-demo/" + DB_NAME, "sa", "")) {
            connection.createStatement().execute("SHUTDOWN");
            logger.info("[OK] Database shutdown completed");
        } catch (Exception e) {
            logger.warn("Failed to shutdown database: " + e.getMessage());
        }

        // Call parent tearDown to clean up APEX services
        super.tearDown();
    }

    @Test
    @DisplayName("Should connect to database and enrich data")
    void testDatabaseConnectivity() {
        logger.info("Testing database connectivity...");

        try {
            YamlRuleConfiguration config = yamlLoader.loadFromFile(
                "src/test/java/dev/mars/apex/demo/database/H2SimpleDatabaseConnectivityTest.yaml");

            Map<String, Object> testData = new HashMap<>();
            testData.put("id", "1");

            RulesEngine engine = RulesEngine.fromYamlConfig(config);
            RuleResult ruleResult = engine.evaluate(config, testData);
            Object result = ruleResult.getEnrichedData();
            assertNotNull(result, "Result should not be null");

            @SuppressWarnings("unchecked")
            Map<String, Object> enrichedData = (Map<String, Object>) result;

            assertEquals("Test Name", enrichedData.get("name"));
            logger.info("[OK] Database connectivity test passed");

        } catch (Exception e) {
            fail("Database connectivity test failed: " + e.getMessage());
        }
    }
}



