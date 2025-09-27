package dev.mars.apex.demo.database;

import dev.mars.apex.demo.DemoTestBase;
import dev.mars.apex.demo.util.TestContainerImages;
import dev.mars.apex.core.config.yaml.YamlRuleConfiguration;
import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.DockerClientFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * PostgreSQL Simple Database Connectivity Test
 * 
 * The most trivial test to show PostgreSQL database connectivity works.
 */
@Testcontainers
public class PostgreSQLSimpleDatabaseConnectivityTest extends DemoTestBase {

    private static final Logger logger = LoggerFactory.getLogger(PostgreSQLSimpleDatabaseConnectivityTest.class);

    @BeforeAll
    static void checkDockerAvailability() {
        try {
            DockerClientFactory.instance().client();
        } catch (Exception e) {
            org.junit.jupiter.api.Assumptions.assumeTrue(false,
                "Docker is not available. Skipping PostgreSQL integration tests. " +
                "To run these tests, ensure Docker is installed and running. Error: " + e.getMessage());
        }
    }

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(TestContainerImages.POSTGRES)
            .withDatabaseName("apex_test")
            .withUsername("apex_user")
            .withPassword("apex_pass");

    @BeforeEach
    void setupDatabase() {
        logger.info("Setting up simple PostgreSQL database...");

        String jdbcUrl = postgres.getJdbcUrl();
        String username = postgres.getUsername();
        String password = postgres.getPassword();

        try (Connection connection = DriverManager.getConnection(jdbcUrl, username, password)) {
            Statement statement = connection.createStatement();

            statement.execute("DROP TABLE IF EXISTS test_table");
            statement.execute("CREATE TABLE test_table (id VARCHAR(10), name VARCHAR(50))");
            statement.execute("INSERT INTO test_table VALUES ('1', 'Test Name')");

            logger.info("✓ PostgreSQL database setup completed");

        } catch (Exception e) {
            throw new RuntimeException("PostgreSQL database setup failed", e);
        }
    }

    @Test
    @DisplayName("Should connect to PostgreSQL database and enrich data")
    void testPostgreSQLDatabaseConnectivity() {
        logger.info("Testing PostgreSQL database connectivity...");

        try {
            YamlRuleConfiguration config = yamlLoader.loadFromFile(
                "src/test/java/dev/mars/apex/demo/database/PostgreSQLSimpleDatabaseConnectivityTest.yaml");

            // Update configuration with real PostgreSQL connection details
            updatePostgreSQLConnection(config);

            Map<String, Object> testData = new HashMap<>();
            testData.put("id", "1");

            Object result = enrichmentService.enrichObject(config, testData);
            assertNotNull(result, "Result should not be null");

            @SuppressWarnings("unchecked")
            Map<String, Object> enrichedData = (Map<String, Object>) result;

            assertEquals("Test Name", enrichedData.get("name"));
            logger.info("✓ PostgreSQL database connectivity test passed");

        } catch (Exception e) {
            fail("PostgreSQL database connectivity test failed: " + e.getMessage());
        }
    }

    /**
     * Update YAML configuration with real PostgreSQL connection details from Testcontainers
     */
    private void updatePostgreSQLConnection(YamlRuleConfiguration config) {
        String host = postgres.getHost();
        Integer port = postgres.getFirstMappedPort();
        String database = postgres.getDatabaseName();
        String username = postgres.getUsername();
        String password = postgres.getPassword();

        logger.info("PostgreSQL Connection Details for APEX:");
        logger.info("  Host: {}", host);
        logger.info("  Port: {}", port);
        logger.info("  Database: {}", database);
        logger.info("  Username: {}", username);

        // Update the PostgreSQL data source configuration with real Testcontainers connection details
        if (config.getDataSources() != null) {
            for (var dataSource : config.getDataSources()) {
                if ("test-database".equals(dataSource.getName())) {
                    Map<String, Object> connection = dataSource.getConnection();

                    // Update connection details with real Testcontainers values
                    connection.put("host", host);
                    connection.put("port", port);
                    connection.put("database", database);
                    connection.put("username", username);
                    connection.put("password", password);

                    logger.info("✅ Updated PostgreSQL data source '{}' with Testcontainers connection details",
                               dataSource.getName());
                    break;
                }
            }
        }
    }
}
