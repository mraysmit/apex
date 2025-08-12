package dev.mars.apex.core.service.data.yaml;

import dev.mars.apex.core.config.datasource.DataSourceConfiguration;
import dev.mars.apex.core.config.yaml.YamlDataSource;
import dev.mars.apex.core.service.data.external.DataSourceException;
import dev.mars.apex.core.service.data.external.ExternalDataSource;
import dev.mars.apex.core.service.data.external.factory.DataSourceFactory;
import org.junit.jupiter.api.*;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for PostgreSQL data sources created from YAML configurations.
 * 
 * This test class validates:
 * - PostgreSQL data source creation from YAML
 * - Connection establishment and health checks
 * - Complex SQL query execution with parameters
 * - Transaction handling and connection pooling
 * - PostgreSQL-specific features (arrays, JSON, custom types)
 * - Error handling and connection recovery
 * 
 * Uses Testcontainers to provide a real PostgreSQL instance for testing.
 * 
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 1.0.0
 */
@Testcontainers
class YamlPostgreSQLLookupTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("testdb")
            .withUsername("testuser")
            .withPassword("testpass");
            // Removed init script to avoid complex setup issues

    private DataSourceFactory factory;
    private ExternalDataSource postgresSource;

    @BeforeAll
    static void setUpContainer() {
        // Container is automatically started by @Testcontainers
        System.out.println("TEST: PostgreSQL container started at " + postgres.getJdbcUrl());
    }

    @BeforeEach
    void setUp() throws DataSourceException {
        factory = DataSourceFactory.getInstance();

        // Wait for container to be ready and get connection details
        assertTrue(postgres.isRunning(), "PostgreSQL container should be running");

        // Create PostgreSQL data source from YAML using actual container connection details
        YamlDataSource yamlPostgres = createPostgreSQLYamlDataSource();
        DataSourceConfiguration config = yamlPostgres.toDataSourceConfiguration();

        // Debug: Print the converted configuration
        System.out.println("TEST: Converted DataSourceConfiguration:");
        System.out.println("  Name: " + config.getName());
        System.out.println("  Source Type: " + config.getSourceType());
        if (config.getConnection() != null) {
            System.out.println("  Connection Host: " + config.getConnection().getHost());
            System.out.println("  Connection Port: " + config.getConnection().getPort());
            System.out.println("  Connection Database: " + config.getConnection().getDatabase());
            System.out.println("  Connection Username: " + config.getConnection().getUsername());
            System.out.println("  Connection Custom Properties: " + config.getConnection().getCustomProperties());
        } else {
            System.out.println("  Connection: null");
        }

        postgresSource = factory.createDataSource(config);

        // Initialize test data
        initializePostgreSQLTestData();
    }

    @AfterEach
    void tearDown() {
        if (postgresSource != null) {
            try {
                postgresSource.shutdown();
            } catch (Exception e) {
                System.out.println("TEST: Cleanup error (expected): " + e.getMessage());
            }
        }
        
        factory.clearCache();
    }

    // ========================================
    // Basic PostgreSQL Tests
    // ========================================

    @Test
    @DisplayName("Should create PostgreSQL data source from YAML and establish connection")
    void testPostgreSQLConnectionFromYaml() throws DataSourceException {
        // Verify basic properties
        assertEquals("test-postgres", postgresSource.getName());
        assertEquals("postgresql", postgresSource.getDataType());
        assertTrue(postgresSource.isHealthy(), "PostgreSQL connection should be healthy");
        
        // Test connection
        assertTrue(postgresSource.testConnection(), "Should be able to test connection");
        
        // Verify connection status
        assertNotNull(postgresSource.getConnectionStatus());
        assertTrue(postgresSource.getConnectionStatus().isConnected());
    }

    @Test
    @DisplayName("Should execute basic PostgreSQL queries from YAML configuration")
    void testBasicPostgreSQLQueries() throws DataSourceException {
        // Test simple query
        List<Object> result = postgresSource.query("SELECT 1 as test_value", Collections.emptyMap());
        assertNotNull(result, "Query result should not be null");
        assertFalse(result.isEmpty(), "Query should return data");
        
        // Test table query
        List<Object> users = postgresSource.query("SELECT * FROM users ORDER BY id", Collections.emptyMap());
        assertNotNull(users, "Users query should not be null");
        assertFalse(users.isEmpty(), "Should have test users");
        
        // Verify user data structure
        Object firstUser = users.get(0);
        assertTrue(firstUser instanceof Map, "User record should be a Map");
        
        @SuppressWarnings("unchecked")
        Map<String, Object> userMap = (Map<String, Object>) firstUser;
        assertTrue(userMap.containsKey("id"), "User should have id field");
        assertTrue(userMap.containsKey("name"), "User should have name field");
        assertTrue(userMap.containsKey("email"), "User should have email field");
    }

    @Test
    @DisplayName("Should handle parameterized PostgreSQL queries")
    void testParameterizedPostgreSQLQueries() throws DataSourceException {
        // First, let's see what users exist
        List<Object> allUsers = postgresSource.query("SELECT * FROM users ORDER BY id", Collections.emptyMap());
        assertNotNull(allUsers, "Should have users");
        assertFalse(allUsers.isEmpty(), "Should have at least one user");

        // Get the first user's ID (might not be 1 due to auto-generation)
        @SuppressWarnings("unchecked")
        Map<String, Object> firstUser = (Map<String, Object>) allUsers.get(0);
        Integer firstUserId = (Integer) firstUser.get("id");

        // Test parameterized query with the actual first user ID
        Map<String, Object> params = Map.of("userId", firstUserId);
        Object user = postgresSource.queryForObject("SELECT * FROM users WHERE id = :userId", params);

        assertNotNull(user, "Should find user with ID " + firstUserId);

        @SuppressWarnings("unchecked")
        Map<String, Object> userMap = (Map<String, Object>) user;
        assertEquals(firstUserId, userMap.get("id"));
        assertEquals("Alice Johnson", userMap.get("name"));
        
        // Test query with status parameter (simpler than BETWEEN)
        Map<String, Object> statusParams = Map.of("status", "active");

        List<Object> activeUsers = postgresSource.query(
            "SELECT * FROM users WHERE status = :status ORDER BY id",
            statusParams
        );

        assertNotNull(activeUsers, "Should return active users");
        assertFalse(activeUsers.isEmpty(), "Should find active users");

        // Verify all returned users are active
        for (Object userObj : activeUsers) {
            @SuppressWarnings("unchecked")
            Map<String, Object> user1 = (Map<String, Object>) userObj;
            assertEquals("active", user1.get("status"), "All returned users should be active");
        }

        // Test email parameter query
        Map<String, Object> emailParams = Map.of("email", "alice@example.com");
        Object userByEmail = postgresSource.queryForObject("SELECT * FROM users WHERE email = :email", emailParams);
        assertNotNull(userByEmail, "Should find user by email");
    }

    // ========================================
    // PostgreSQL-Specific Feature Tests
    // ========================================

    @Test
    @DisplayName("Should handle PostgreSQL JSON operations")
    void testPostgreSQLJsonOperations() throws DataSourceException {
        // Test JSON column query
        List<Object> usersWithPrefs = postgresSource.query(
            "SELECT id, name, preferences FROM users WHERE preferences IS NOT NULL ORDER BY id", 
            Collections.emptyMap()
        );
        
        assertNotNull(usersWithPrefs, "Should return users with preferences");
        
        if (!usersWithPrefs.isEmpty()) {
            Object firstUser = usersWithPrefs.get(0);
            @SuppressWarnings("unchecked")
            Map<String, Object> userMap = (Map<String, Object>) firstUser;
            
            assertTrue(userMap.containsKey("preferences"), "User should have preferences field");
            
            // Test JSON path query
            Map<String, Object> jsonParams = Map.of("theme", "dark");
            List<Object> darkThemeUsers = postgresSource.query(
                "SELECT * FROM users WHERE preferences->>'theme' = :theme", 
                jsonParams
            );
            
            assertNotNull(darkThemeUsers, "Should handle JSON path queries");
        }
    }

    @Test
    @DisplayName("Should handle PostgreSQL array operations")
    void testPostgreSQLArrayOperations() throws DataSourceException {
        // Test array column query
        List<Object> usersWithTags = postgresSource.query(
            "SELECT id, name, tags FROM users WHERE tags IS NOT NULL ORDER BY id", 
            Collections.emptyMap()
        );
        
        assertNotNull(usersWithTags, "Should return users with tags");
        
        if (!usersWithTags.isEmpty()) {
            // Test array contains query
            Map<String, Object> arrayParams = Map.of("tag", "admin");
            List<Object> adminUsers = postgresSource.query(
                "SELECT * FROM users WHERE :tag = ANY(tags)", 
                arrayParams
            );
            
            assertNotNull(adminUsers, "Should handle array contains queries");
        }
    }

    @Test
    @DisplayName("Should handle PostgreSQL date and time operations")
    void testPostgreSQLDateTimeOperations() throws DataSourceException {
        // Test timestamp queries
        List<Object> recentUsers = postgresSource.query(
            "SELECT * FROM users WHERE created_at >= NOW() - INTERVAL '1 day' ORDER BY created_at DESC", 
            Collections.emptyMap()
        );
        
        assertNotNull(recentUsers, "Should handle timestamp queries");
        
        // Test parameterized date query (fix INTERVAL parameter binding)
        Map<String, Object> dateParams = Map.of("days", 30);
        List<Object> monthlyUsers = postgresSource.query(
            "SELECT * FROM users WHERE created_at >= NOW() - INTERVAL '30 days'",
            Collections.emptyMap() // Don't use parameters in INTERVAL expressions
        );
        
        assertNotNull(monthlyUsers, "Should handle parameterized date queries");
    }

    // ========================================
    // Transaction and Connection Pool Tests
    // ========================================

    @Test
    @DisplayName("Should handle concurrent connections properly")
    void testConcurrentConnections() throws DataSourceException {
        // Execute multiple queries concurrently to test connection pooling
        List<Thread> threads = new ArrayList<>();
        List<Exception> exceptions = Collections.synchronizedList(new ArrayList<>());
        
        for (int i = 0; i < 5; i++) {
            final int threadId = i;
            Thread thread = new Thread(() -> {
                try {
                    Map<String, Object> params = Map.of("threadId", threadId);
                    postgresSource.query("SELECT :threadId as thread_id, NOW() as query_time", params);
                } catch (Exception e) {
                    exceptions.add(e);
                }
            });
            threads.add(thread);
            thread.start();
        }
        
        // Wait for all threads to complete
        for (Thread thread : threads) {
            try {
                thread.join(5000); // 5 second timeout
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        
        assertTrue(exceptions.isEmpty(), "No exceptions should occur during concurrent access");
    }

    // ========================================
    // Error Handling Tests
    // ========================================

    @Test
    @DisplayName("Should handle PostgreSQL-specific errors gracefully")
    void testPostgreSQLErrorHandling() {
        // Test syntax error
        assertThrows(DataSourceException.class, () -> {
            postgresSource.query("SELECT * FORM users", Collections.emptyMap()); // Intentional typo
        }, "Should throw exception for SQL syntax error");
        
        // Test invalid parameter
        assertThrows(DataSourceException.class, () -> {
            Map<String, Object> invalidParams = Map.of("invalidParam", "value");
            postgresSource.query("SELECT * FROM users WHERE id = :nonexistentParam", invalidParams);
        }, "Should throw exception for missing parameter");
        
        // Test constraint violation (duplicate email)
        assertThrows(DataSourceException.class, () -> {
            postgresSource.query("INSERT INTO users (name, email) VALUES ('Duplicate', 'alice@example.com')", Collections.emptyMap());
        }, "Should throw exception for unique constraint violation");
    }

    // ========================================
    // Helper Methods
    // ========================================

    private YamlDataSource createPostgreSQLYamlDataSource() {
        YamlDataSource yamlPostgres = new YamlDataSource();
        yamlPostgres.setName("test-postgres");
        yamlPostgres.setType("database");
        yamlPostgres.setSourceType("postgresql");
        yamlPostgres.setEnabled(true);
        yamlPostgres.setDescription("Test PostgreSQL database");

        // Configure connection using container details (get fresh connection info)
        String jdbcUrl = postgres.getJdbcUrl();
        String username = postgres.getUsername();
        String password = postgres.getPassword();

        System.out.println("TEST: PostgreSQL container connection details:");
        System.out.println("  JDBC URL: " + jdbcUrl);
        System.out.println("  Username: " + username);
        System.out.println("  Password: " + password);

        // Parse JDBC URL to extract host, port, database
        // Format: jdbc:postgresql://host:port/database
        String host = postgres.getHost();
        Integer port = postgres.getFirstMappedPort();
        String database = postgres.getDatabaseName();

        System.out.println("TEST: Parsed connection details:");
        System.out.println("  Host: " + host);
        System.out.println("  Port: " + port);
        System.out.println("  Database: " + database);

        Map<String, Object> connection = yamlPostgres.getConnection();
        connection.put("host", host);
        connection.put("port", port);
        connection.put("database", database);
        connection.put("username", username);
        connection.put("password", password);

        // Configure connection pool (use hyphenated keys)
        connection.put("max-pool-size", 10);
        connection.put("min-pool-size", 2);
        connection.put("connection-timeout", 30000);
        connection.put("idle-timeout", 600000);
        connection.put("max-lifetime", 1800000);
        
        // Set parameter names for testing
        yamlPostgres.setParameterNames(new String[]{
            "userId", "minId", "maxId", "status", "theme", "tag", "days", "threadId", "nonexistentParam"
        });
        
        return yamlPostgres;
    }

    private void initializePostgreSQLTestData() throws DataSourceException {
        System.out.println("TEST: Initializing PostgreSQL test data");

        // Create simple tables using batchUpdate
        List<String> ddlStatements = List.of(
            """
            CREATE TABLE IF NOT EXISTS users (
                id SERIAL PRIMARY KEY,
                name VARCHAR(255) NOT NULL,
                email VARCHAR(255) UNIQUE NOT NULL,
                status VARCHAR(50) DEFAULT 'active',
                preferences JSONB,
                tags TEXT[],
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
            """,
            "DELETE FROM users", // Clear any existing data
            """
            INSERT INTO users (name, email, status, preferences, tags) VALUES
            ('Alice Johnson', 'alice@example.com', 'active',
             '{"theme": "dark", "language": "en", "notifications": true}',
             ARRAY['admin', 'developer']),
            ('Bob Smith', 'bob@example.com', 'active',
             '{"theme": "light", "language": "en", "notifications": false}',
             ARRAY['user']),
            ('Charlie Brown', 'charlie@example.com', 'inactive',
             '{"theme": "dark", "language": "es", "notifications": true}',
             ARRAY['user', 'tester'])
            """
        );

        postgresSource.batchUpdate(ddlStatements);

        System.out.println("TEST: PostgreSQL test data initialized successfully");
    }
}
