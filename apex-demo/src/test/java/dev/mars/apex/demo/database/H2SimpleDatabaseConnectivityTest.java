package dev.mars.apex.demo.database;

import dev.mars.apex.demo.DemoTestBase;
import dev.mars.apex.core.config.yaml.YamlRuleConfiguration;
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

    @BeforeEach
    void setupDatabase() {
        logger.info("Setting up simple database...");

        String jdbcUrl = "jdbc:h2:./target/h2-demo/simple_test;DB_CLOSE_DELAY=-1";

        try (Connection connection = DriverManager.getConnection(jdbcUrl, "sa", "")) {
            Statement statement = connection.createStatement();

            statement.execute("DROP TABLE IF EXISTS test_table");
            statement.execute("CREATE TABLE test_table (id VARCHAR(10), name VARCHAR(50))");
            statement.execute("INSERT INTO test_table VALUES ('1', 'Test Name')");

            logger.info("✓ Database setup completed");

        } catch (Exception e) {
            throw new RuntimeException("Database setup failed", e);
        }
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

            Object result = enrichmentService.enrichObject(config, testData);
            assertNotNull(result, "Result should not be null");

            @SuppressWarnings("unchecked")
            Map<String, Object> enrichedData = (Map<String, Object>) result;

            assertEquals("Test Name", enrichedData.get("name"));
            logger.info("✓ Database connectivity test passed");

        } catch (Exception e) {
            fail("Database connectivity test failed: " + e.getMessage());
        }
    }
}
