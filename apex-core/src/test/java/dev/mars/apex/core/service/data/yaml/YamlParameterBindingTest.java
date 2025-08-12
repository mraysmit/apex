package dev.mars.apex.core.service.data.yaml;

import dev.mars.apex.core.config.datasource.DataSourceConfiguration;
import dev.mars.apex.core.config.yaml.YamlDataSource;
import dev.mars.apex.core.service.data.external.DataSourceException;
import dev.mars.apex.core.service.data.external.ExternalDataSource;
import dev.mars.apex.core.service.data.external.factory.DataSourceFactory;
import org.junit.jupiter.api.*;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for parameter binding and query execution in YAML-configured data sources.
 * 
 * This test class validates:
 * - Named parameter binding in database queries
 * - Parameter validation and type conversion
 * - Complex parameter scenarios (arrays, objects, null values)
 * - Parameter binding in different data source types
 * - Error handling for invalid parameters
 * - Performance of parameterized queries
 * 
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 1.0.0
 */
class YamlParameterBindingTest {

    private DataSourceFactory factory;
    private ExternalDataSource databaseSource;
    private ExternalDataSource cacheSource;

    @BeforeEach
    void setUp() throws DataSourceException {
        factory = DataSourceFactory.getInstance();
        setupTestDataSources();
    }

    @AfterEach
    void tearDown() {
        if (databaseSource != null) {
            try {
                databaseSource.shutdown();
            } catch (Exception e) {
                // Ignore cleanup errors
            }
        }
        
        if (cacheSource != null) {
            try {
                cacheSource.shutdown();
            } catch (Exception e) {
                // Ignore cleanup errors
            }
        }
        
        factory.clearCache();
    }

    // ========================================
    // Database Parameter Binding Tests
    // ========================================

    @Test
    @DisplayName("Should bind simple parameters in database queries")
    void testSimpleParameterBinding() throws DataSourceException {
        // Test single parameter binding
        Map<String, Object> params = Map.of("id", 1);
        Object user = databaseSource.queryForObject("getUserById", params);
        
        assertNotNull(user, "Should find user with ID 1");
        verifyUserData(user, 1, "Alice Johnson");
        
        // Test different parameter value
        Map<String, Object> params2 = Map.of("id", 2);
        Object user2 = databaseSource.queryForObject("getUserById", params2);
        
        assertNotNull(user2, "Should find user with ID 2");
        verifyUserData(user2, 2, "Bob Smith");
    }

    @Test
    @DisplayName("Should bind multiple parameters in database queries")
    void testMultipleParameterBinding() throws DataSourceException {
        // Test query with multiple parameters
        Map<String, Object> params = Map.of(
            "minId", 1,
            "maxId", 3,
            "status", "active"
        );
        
        List<Object> users = databaseSource.query("getUsersByIdRangeAndStatus", params);
        assertNotNull(users, "Should return users list");
        assertFalse(users.isEmpty(), "Should find users in range with active status");
        
        // Verify all returned users meet criteria
        for (Object user : users) {
            @SuppressWarnings("unchecked")
            Map<String, Object> userMap = (Map<String, Object>) user;
            Integer id = (Integer) userMap.get("ID");
            String status = (String) userMap.get("STATUS");
            
            assertTrue(id >= 1 && id <= 3, "User ID should be in range");
            assertEquals("active", status, "User status should be active");
        }
    }

    @Test
    @DisplayName("Should handle string pattern parameters")
    void testStringPatternParameterBinding() throws DataSourceException {
        // Test LIKE pattern parameter
        Map<String, Object> params = Map.of("namePattern", "Alice%");
        List<Object> users = databaseSource.query("getUsersByNamePattern", params);
        
        assertNotNull(users, "Should return users list");
        assertFalse(users.isEmpty(), "Should find users matching pattern");
        
        // Verify all returned users match pattern
        for (Object user : users) {
            @SuppressWarnings("unchecked")
            Map<String, Object> userMap = (Map<String, Object>) user;
            String name = (String) userMap.get("NAME");
            assertTrue(name.startsWith("Alice"), "User name should match pattern");
        }
    }

    @Test
    @DisplayName("Should handle null parameter values")
    void testNullParameterHandling() throws DataSourceException {
        // Test query with null parameter
        Map<String, Object> params = Map.of(
            "id", 1,
            "optionalField", (Object) null
        );
        
        // Should not throw exception with null parameter
        assertDoesNotThrow(() -> {
            databaseSource.query("getUserWithOptionalField", params);
        }, "Should handle null parameters gracefully");
    }

    @Test
    @DisplayName("Should validate required parameters")
    void testRequiredParameterValidation() throws DataSourceException {
        // Test missing required parameter
        Map<String, Object> emptyParams = Collections.emptyMap();
        
        assertThrows(DataSourceException.class, () -> {
            databaseSource.queryForObject("getUserById", emptyParams);
        }, "Should throw exception for missing required parameter");
    }

    // ========================================
    // Cache Parameter Binding Tests
    // ========================================

    @Test
    @DisplayName("Should bind parameters in cache operations")
    void testCacheParameterBinding() throws DataSourceException {
        // Test parameterized cache put
        Map<String, Object> putParams = Map.of(
            "key", "user:123",
            "value", Map.of("id", 123, "name", "Test User"),
            "ttl", 300
        );
        
        cacheSource.query("putWithTtl", putParams);
        
        // Test parameterized cache get
        Map<String, Object> getParams = Map.of("key", "user:123");
        Object result = cacheSource.queryForObject("get", getParams);
        
        assertNotNull(result, "Should retrieve cached value");
        
        @SuppressWarnings("unchecked")
        Map<String, Object> cachedUser = (Map<String, Object>) result;
        assertEquals(123, cachedUser.get("id"));
        assertEquals("Test User", cachedUser.get("name"));
    }

    @Test
    @DisplayName("Should handle complex cache key patterns")
    void testComplexCacheKeyPatterns() throws DataSourceException {
        // Store multiple values with pattern-based keys
        for (int i = 1; i <= 5; i++) {
            Map<String, Object> putParams = Map.of(
                "key", "user:profile:" + i,
                "value", "Profile data for user " + i
            );
            cacheSource.query("put", putParams);
        }
        
        // Test pattern-based retrieval
        Map<String, Object> patternParams = Map.of("pattern", "user:profile:*");
        List<Object> keys = cacheSource.query("keys", patternParams);
        
        assertNotNull(keys, "Should return matching keys");
        assertEquals(5, keys.size(), "Should find all 5 matching keys");
        
        // Verify all keys match pattern
        for (Object key : keys) {
            assertTrue(key.toString().startsWith("user:profile:"), 
                "All keys should match pattern");
        }
    }

    // ========================================
    // Parameter Type Conversion Tests
    // ========================================

    @Test
    @DisplayName("Should handle parameter type conversion")
    void testParameterTypeConversion() throws DataSourceException {
        // Test string to integer conversion
        Map<String, Object> stringIdParams = Map.of("id", "1");
        Object user = databaseSource.queryForObject("getUserById", stringIdParams);
        assertNotNull(user, "Should handle string to integer conversion");
        
        // Test boolean parameter
        Map<String, Object> booleanParams = Map.of(
            "id", 1,
            "isActive", true
        );
        
        assertDoesNotThrow(() -> {
            databaseSource.query("updateUserActiveStatus", booleanParams);
        }, "Should handle boolean parameters");
    }

    @Test
    @DisplayName("Should handle date and timestamp parameters")
    void testDateTimeParameterBinding() throws DataSourceException {
        // Test date parameter
        Date testDate = new Date();
        Map<String, Object> dateParams = Map.of(
            "userId", 1,
            "loginDate", testDate
        );
        
        assertDoesNotThrow(() -> {
            databaseSource.query("updateLastLogin", dateParams);
        }, "Should handle date parameters");
        
        // Test timestamp range query
        Date startDate = new Date(System.currentTimeMillis() - 86400000); // 24 hours ago
        Date endDate = new Date();
        
        Map<String, Object> rangeParams = Map.of(
            "startDate", startDate,
            "endDate", endDate
        );
        
        List<Object> recentLogins = databaseSource.query("getRecentLogins", rangeParams);
        assertNotNull(recentLogins, "Should handle date range parameters");
    }

    // ========================================
    // Error Handling Tests
    // ========================================

    @Test
    @DisplayName("Should handle invalid parameter types gracefully")
    void testInvalidParameterTypeHandling() {
        // Test invalid parameter type
        Map<String, Object> invalidParams = Map.of(
            "id", new Object() // Invalid type for ID parameter
        );
        
        assertThrows(DataSourceException.class, () -> {
            databaseSource.queryForObject("getUserById", invalidParams);
        }, "Should throw exception for invalid parameter type");
    }

    @Test
    @DisplayName("Should provide meaningful error messages for parameter issues")
    void testParameterErrorMessages() {
        // Test missing parameter
        try {
            databaseSource.queryForObject("getUserById", Collections.emptyMap());
            fail("Should throw exception for missing parameter");
        } catch (DataSourceException e) {
            assertTrue(e.getMessage().contains("parameter") || e.getMessage().contains("id"),
                "Error message should mention parameter issue");
        }
    }

    // ========================================
    // Helper Methods
    // ========================================

    private void setupTestDataSources() throws DataSourceException {
        // Setup database source
        YamlDataSource yamlDb = createDatabaseYamlDataSource();
        DataSourceConfiguration dbConfig = yamlDb.toDataSourceConfiguration();
        databaseSource = factory.createDataSource(dbConfig);
        initializeDatabaseWithTestData();
        
        // Setup cache source
        YamlDataSource yamlCache = createCacheYamlDataSource();
        DataSourceConfiguration cacheConfig = yamlCache.toDataSourceConfiguration();
        cacheSource = factory.createDataSource(cacheConfig);
    }

    private YamlDataSource createDatabaseYamlDataSource() {
        YamlDataSource yamlDb = new YamlDataSource();
        yamlDb.setName("param-test-db");
        yamlDb.setType("database");
        yamlDb.setSourceType("h2");
        yamlDb.setEnabled(true);
        
        Map<String, Object> connection = yamlDb.getConnection();
        connection.put("url", "jdbc:h2:mem:paramtestdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE");
        connection.put("username", "sa");
        connection.put("password", "");
        connection.put("driverClassName", "org.h2.Driver");
        
        Map<String, String> queries = yamlDb.getQueries();
        queries.put("createTable", "CREATE TABLE IF NOT EXISTS users (id INT PRIMARY KEY, name VARCHAR(255), email VARCHAR(255), status VARCHAR(50), last_login TIMESTAMP)");
        queries.put("insertTestData", "INSERT INTO users (id, name, email, status) VALUES (1, 'Alice Johnson', 'alice@example.com', 'active'), (2, 'Bob Smith', 'bob@example.com', 'active'), (3, 'Charlie Brown', 'charlie@example.com', 'inactive')");
        queries.put("getUserById", "SELECT * FROM users WHERE id = :id");
        queries.put("getUsersByIdRangeAndStatus", "SELECT * FROM users WHERE id BETWEEN :minId AND :maxId AND status = :status");
        queries.put("getUsersByNamePattern", "SELECT * FROM users WHERE name LIKE :namePattern");
        queries.put("getUserWithOptionalField", "SELECT * FROM users WHERE id = :id AND (:optionalField IS NULL OR status = :optionalField)");
        queries.put("updateUserActiveStatus", "UPDATE users SET status = CASE WHEN :isActive THEN 'active' ELSE 'inactive' END WHERE id = :id");
        queries.put("updateLastLogin", "UPDATE users SET last_login = :loginDate WHERE id = :userId");
        queries.put("getRecentLogins", "SELECT * FROM users WHERE last_login BETWEEN :startDate AND :endDate");
        
        yamlDb.setParameterNames(new String[]{"id", "minId", "maxId", "status", "namePattern", "optionalField", "isActive", "userId", "loginDate", "startDate", "endDate"});
        
        return yamlDb;
    }

    private YamlDataSource createCacheYamlDataSource() {
        YamlDataSource yamlCache = new YamlDataSource();
        yamlCache.setName("param-test-cache");
        yamlCache.setType("cache");
        yamlCache.setSourceType("memory");
        yamlCache.setEnabled(true);
        
        Map<String, Object> cache = yamlCache.getCache();
        cache.put("enabled", true);
        cache.put("maxSize", 1000);
        cache.put("ttlSeconds", 300);
        cache.put("evictionPolicy", "LRU");
        
        return yamlCache;
    }

    private void initializeDatabaseWithTestData() throws DataSourceException {
        databaseSource.query("createTable", Collections.emptyMap());
        databaseSource.query("insertTestData", Collections.emptyMap());
    }

    private void verifyUserData(Object user, int expectedId, String expectedName) {
        assertNotNull(user, "User should not be null");
        assertTrue(user instanceof Map, "User should be a Map");
        
        @SuppressWarnings("unchecked")
        Map<String, Object> userMap = (Map<String, Object>) user;
        
        assertEquals(expectedId, userMap.get("ID"), "User ID should match");
        assertEquals(expectedName, userMap.get("NAME"), "User name should match");
    }
}
