package dev.mars.apex.core.service.data.yaml;

import dev.mars.apex.core.config.datasource.DataSourceConfiguration;
import dev.mars.apex.core.config.yaml.YamlDataSource;
import dev.mars.apex.core.config.yaml.YamlRuleConfiguration;
import dev.mars.apex.core.service.data.external.DataSourceException;
import dev.mars.apex.core.service.data.external.ExternalDataSource;
import dev.mars.apex.core.service.data.external.config.DataSourceConfigurationService;
import dev.mars.apex.core.service.data.external.factory.DataSourceFactory;
import dev.mars.apex.core.service.data.external.manager.DataSourceManager;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for multi-source data lookup scenarios using YAML configurations.
 * 
 * This test class validates complex data lookup patterns that involve multiple
 * data sources working together, simulating real-world scenarios such as:
 * - Cache-first lookup with database fallback
 * - Data enrichment from multiple sources
 * - Failover between primary and secondary sources
 * - Data aggregation from heterogeneous sources
 * - Performance optimization through source prioritization
 * 
 * Tests are based on the mixed-example.yaml configuration patterns.
 * 
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 1.0.0
 */
class YamlMultiSourceLookupTest {

    @TempDir
    Path tempDir;

    private DataSourceFactory factory;
    private DataSourceConfigurationService configService;
    private DataSourceManager dataSourceManager;
    private Map<String, ExternalDataSource> dataSources;

    @BeforeEach
    void setUp() throws DataSourceException {
        factory = DataSourceFactory.getInstance();
        configService = DataSourceConfigurationService.getInstance();
        dataSourceManager = new DataSourceManager();
        dataSources = new HashMap<>();
        
        // Initialize with empty configuration
        YamlRuleConfiguration emptyConfig = new YamlRuleConfiguration();
        emptyConfig.setDataSources(new ArrayList<>());
        configService.initialize(emptyConfig);
    }

    @AfterEach
    void tearDown() {
        // Clean up data sources
        for (ExternalDataSource dataSource : dataSources.values()) {
            try {
                dataSource.shutdown();
            } catch (Exception e) {
                // Ignore cleanup errors
            }
        }
        dataSources.clear();
        
        // Clean up managers
        dataSourceManager.shutdown();
        factory.clearCache();
        configService.shutdown();
    }

    // ========================================
    // Cache-First with Database Fallback Tests
    // ========================================

    @Test
    @DisplayName("Should implement cache-first lookup with database fallback pattern")
    void testCacheFirstDatabaseFallbackPattern() throws DataSourceException, IOException {
        // Setup multi-source configuration
        setupCacheAndDatabaseSources();
        
        // Test cache miss scenario (data not in cache, should fallback to database)
        String userId = "user123";
        
        // First lookup - should miss cache and hit database
        Object userFromDb = lookupUserWithFallback(userId);
        assertNotNull(userFromDb, "Should retrieve user from database on cache miss");
        
        // Store in cache for next test
        storeUserInCache(userId, userFromDb);
        
        // Second lookup - should hit cache
        Object userFromCache = lookupUserWithFallback(userId);
        assertNotNull(userFromCache, "Should retrieve user from cache on cache hit");
        
        // Verify cache metrics improved
        ExternalDataSource cacheSource = dataSources.get("primary-cache");
        assertTrue(cacheSource.getMetrics().getCacheHitRate() > 0.0,
            "Cache hit rate should be greater than 0 after cache hit");
    }

    @Test
    @DisplayName("Should handle database unavailability with graceful degradation")
    void testDatabaseUnavailabilityGracefulDegradation() throws DataSourceException {
        // Setup cache source only (simulate database unavailability)
        setupCacheSource();
        
        // Pre-populate cache with some data
        ExternalDataSource cacheSource = dataSources.get("primary-cache");
        Map<String, Object> putParams = Map.of("key", "user456", "value", createTestUserData("456", "Jane Doe"));
        cacheSource.query("put", putParams);
        
        // Test lookup when database is unavailable but data is in cache
        Map<String, Object> getParams = Map.of("key", "user456");
        Object cachedUser = cacheSource.queryForObject("get", getParams);
        assertNotNull(cachedUser, "Should retrieve user from cache when database is unavailable");
        
        // Test lookup for data not in cache (should return null gracefully)
        Map<String, Object> missParams = Map.of("key", "user999");
        Object missedUser = cacheSource.queryForObject("get", missParams);
        assertNull(missedUser, "Should return null gracefully for cache miss when database unavailable");
    }

    // ========================================
    // Data Enrichment Tests
    // ========================================

    @Test
    @DisplayName("Should perform data enrichment from multiple sources")
    void testDataEnrichmentFromMultipleSources() throws DataSourceException, IOException {
        // Setup multiple sources: database, file, and cache
        setupMultipleSourcesForEnrichment();
        
        // Test enrichment workflow
        String userId = "user789";
        
        // Step 1: Get base user data from database
        Object baseUser = getBaseUserData(userId);
        assertNotNull(baseUser, "Should get base user data from database");
        
        // Step 2: Enrich with profile data from file
        Object enrichedUser = enrichUserWithProfileData(baseUser, userId);
        assertNotNull(enrichedUser, "Should enrich user with profile data from file");
        
        // Step 3: Add preferences from cache
        Object fullyEnrichedUser = addUserPreferencesFromCache(enrichedUser, userId);
        assertNotNull(fullyEnrichedUser, "Should add user preferences from cache");
        
        // Verify enriched data structure
        verifyEnrichedUserData(fullyEnrichedUser);
    }

    // ========================================
    // Performance Optimization Tests
    // ========================================

    @Test
    @DisplayName("Should optimize performance through source prioritization")
    void testPerformanceOptimizationThroughPrioritization() throws DataSourceException {
        // Setup sources with different performance characteristics
        setupSourcesWithDifferentPerformance();
        
        // Measure lookup times for different sources
        long cacheTime = measureLookupTime("primary-cache", "fast-data");
        long dbTime = measureLookupTime("user-database", "slow-data");
        
        // Cache should be significantly faster
        assertTrue(cacheTime < dbTime, 
            "Cache lookup should be faster than database lookup");
        
        // Verify cache is prioritized for repeated lookups
        long secondCacheTime = measureLookupTime("primary-cache", "fast-data");
        assertTrue(secondCacheTime <= cacheTime, 
            "Subsequent cache lookups should be as fast or faster");
    }

    // ========================================
    // Error Handling and Resilience Tests
    // ========================================

    @Test
    @DisplayName("Should handle partial source failures gracefully")
    void testPartialSourceFailureHandling() throws DataSourceException {
        // Setup multiple sources
        setupMultipleSourcesForResilience();
        
        // Simulate failure of one source by shutting it down
        ExternalDataSource dbSource = dataSources.get("user-database");
        dbSource.shutdown();
        
        // Verify other sources still work
        ExternalDataSource cacheSource = dataSources.get("primary-cache");
        assertTrue(cacheSource.isHealthy(), "Cache source should remain healthy");
        
        // Test that cache operations still work
        Map<String, Object> putParams = Map.of("key", "resilience-test", "value", "test-data");
        assertDoesNotThrow(() -> cacheSource.query("put", putParams),
            "Cache operations should work despite database failure");
        
        Map<String, Object> getParams = Map.of("key", "resilience-test");
        Object result = cacheSource.queryForObject("get", getParams);
        assertEquals("test-data", result, "Should retrieve data from working cache source");
    }

    // ========================================
    // Helper Methods for Source Setup
    // ========================================

    private void setupCacheAndDatabaseSources() throws DataSourceException {
        // Create cache source
        setupCacheSource();
        
        // Create database source
        setupDatabaseSource();
    }

    private void setupCacheSource() throws DataSourceException {
        YamlDataSource yamlCache = createCacheYamlDataSource();
        DataSourceConfiguration cacheConfig = yamlCache.toDataSourceConfiguration();
        ExternalDataSource cacheSource = factory.createDataSource(cacheConfig);
        dataSources.put("primary-cache", cacheSource);
    }

    private void setupDatabaseSource() throws DataSourceException {
        YamlDataSource yamlDb = createDatabaseYamlDataSource();
        DataSourceConfiguration dbConfig = yamlDb.toDataSourceConfiguration();
        ExternalDataSource dbSource = factory.createDataSource(dbConfig);
        dataSources.put("user-database", dbSource);
        
        // Initialize database with test data
        initializeDatabaseWithTestData(dbSource);
    }

    private void setupMultipleSourcesForEnrichment() throws DataSourceException, IOException {
        setupCacheAndDatabaseSources();
        
        // Add file source for profile data
        Path profileFile = createUserProfileFile();
        YamlDataSource yamlFile = createFileYamlDataSource(profileFile.getParent().toString());
        DataSourceConfiguration fileConfig = yamlFile.toDataSourceConfiguration();
        ExternalDataSource fileSource = factory.createDataSource(fileConfig);
        dataSources.put("profile-files", fileSource);
        
        // Pre-populate cache with preferences
        populateCacheWithPreferences();
    }

    private void setupSourcesWithDifferentPerformance() throws DataSourceException {
        // Fast cache source
        setupCacheSource();
        
        // Slower database source
        setupDatabaseSource();
        
        // Pre-populate with test data
        ExternalDataSource cacheSource = dataSources.get("primary-cache");
        Map<String, Object> fastData = Map.of("key", "fast-data", "value", "cached-value");
        cacheSource.query("put", fastData);
        
        ExternalDataSource dbSource = dataSources.get("user-database");
        // Database already has test data from setup
    }

    private void setupMultipleSourcesForResilience() throws DataSourceException {
        setupCacheAndDatabaseSources();
        
        // Verify both sources are initially healthy
        assertTrue(dataSources.get("primary-cache").isHealthy());
        assertTrue(dataSources.get("user-database").isHealthy());
    }

    // ========================================
    // Helper Methods for Data Operations
    // ========================================

    private Object lookupUserWithFallback(String userId) throws DataSourceException {
        // Try cache first
        ExternalDataSource cacheSource = dataSources.get("primary-cache");
        if (cacheSource != null) {
            Map<String, Object> cacheParams = Map.of("key", userId);
            Object cachedUser = cacheSource.queryForObject("get", cacheParams);
            if (cachedUser != null) {
                return cachedUser;
            }
        }
        
        // Fallback to database - use getData() for query keys
        ExternalDataSource dbSource = dataSources.get("user-database");
        if (dbSource != null) {
            int id = Integer.parseInt(userId.replace("user", ""));
            return dbSource.getData("getUserById", id);
        }
        
        return null;
    }

    private void storeUserInCache(String userId, Object userData) throws DataSourceException {
        ExternalDataSource cacheSource = dataSources.get("primary-cache");
        Map<String, Object> putParams = Map.of("key", userId, "value", userData);
        cacheSource.query("put", putParams);
    }

    private Object getBaseUserData(String userId) throws DataSourceException {
        ExternalDataSource dbSource = dataSources.get("user-database");
        int id = Integer.parseInt(userId.replace("user", ""));
        return dbSource.getData("getUserById", id);
    }

    private Object enrichUserWithProfileData(Object baseUser, String userId) throws DataSourceException {
        ExternalDataSource fileSource = dataSources.get("profile-files");
        Object profileData = fileSource.getData("json", "user-profiles.json");
        
        // Simulate enrichment logic
        Map<String, Object> enriched = new HashMap<>();
        if (baseUser instanceof Map) {
            enriched.putAll((Map<String, Object>) baseUser);
        }
        enriched.put("profileData", profileData);
        
        return enriched;
    }

    private Object addUserPreferencesFromCache(Object enrichedUser, String userId) throws DataSourceException {
        ExternalDataSource cacheSource = dataSources.get("primary-cache");
        Map<String, Object> prefParams = Map.of("key", "preferences:" + userId);
        Object preferences = cacheSource.queryForObject("get", prefParams);
        
        Map<String, Object> fullyEnriched = new HashMap<>();
        if (enrichedUser instanceof Map) {
            fullyEnriched.putAll((Map<String, Object>) enrichedUser);
        }
        fullyEnriched.put("preferences", preferences != null ? preferences : Collections.emptyMap());
        
        return fullyEnriched;
    }

    private long measureLookupTime(String sourceName, String dataKey) throws DataSourceException {
        ExternalDataSource source = dataSources.get(sourceName);
        
        long startTime = System.nanoTime();
        
        if ("primary-cache".equals(sourceName)) {
            Map<String, Object> params = Map.of("key", dataKey);
            source.queryForObject("get", params);
        } else {
            source.getData(dataKey);
        }
        
        return System.nanoTime() - startTime;
    }

    private void verifyEnrichedUserData(Object enrichedUser) {
        assertNotNull(enrichedUser, "Enriched user should not be null");
        assertTrue(enrichedUser instanceof Map, "Enriched user should be a Map");

        @SuppressWarnings("unchecked")
        Map<String, Object> userMap = (Map<String, Object>) enrichedUser;

        // Should have base user data
        assertTrue(userMap.containsKey("ID") || userMap.containsKey("id"),
            "Should contain user ID from base data");

        // Should have profile data
        assertTrue(userMap.containsKey("profileData"),
            "Should contain profile data from file source");

        // Should have preferences
        assertTrue(userMap.containsKey("preferences"),
            "Should contain preferences from cache");
    }

    // ========================================
    // Helper Methods for Creating YAML Configurations
    // ========================================

    private YamlDataSource createCacheYamlDataSource() {
        YamlDataSource yamlCache = new YamlDataSource();
        yamlCache.setName("primary-cache");
        yamlCache.setType("cache");
        yamlCache.setSourceType("memory");
        yamlCache.setEnabled(true);
        yamlCache.setDescription("Primary cache for multi-source testing");

        Map<String, Object> cache = yamlCache.getCache();
        cache.put("enabled", true);
        cache.put("maxSize", 1000);
        cache.put("ttlSeconds", 300);
        cache.put("evictionPolicy", "LRU");
        cache.put("keyPrefix", "multi");

        return yamlCache;
    }

    private YamlDataSource createDatabaseYamlDataSource() {
        YamlDataSource yamlDb = new YamlDataSource();
        yamlDb.setName("user-database");
        yamlDb.setType("database");
        yamlDb.setSourceType("h2");
        yamlDb.setEnabled(true);
        yamlDb.setDescription("User database for multi-source testing");

        Map<String, Object> connection = yamlDb.getConnection();
        connection.put("url", "jdbc:h2:mem:multisourcedb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE");
        connection.put("username", "sa");
        connection.put("password", "");
        connection.put("driverClassName", "org.h2.Driver");

        Map<String, String> queries = yamlDb.getQueries();
        queries.put("createTable", "CREATE TABLE IF NOT EXISTS users (id INT PRIMARY KEY, name VARCHAR(255), email VARCHAR(255), status VARCHAR(50))");
        queries.put("insertTestData", "INSERT INTO users (id, name, email, status) VALUES (123, 'John Doe', 'john@example.com', 'active'), (456, 'Jane Smith', 'jane@example.com', 'active'), (789, 'Bob Johnson', 'bob@example.com', 'inactive')");
        queries.put("getUserById", "SELECT * FROM users WHERE id = :id");
        queries.put("getAllUsers", "SELECT * FROM users");
        queries.put("default", "SELECT 1 as health_check");

        yamlDb.setParameterNames(new String[]{"id"});

        return yamlDb;
    }

    private YamlDataSource createFileYamlDataSource(String basePath) {
        YamlDataSource yamlFile = new YamlDataSource();
        yamlFile.setName("profile-files");
        yamlFile.setType("file-system");
        yamlFile.setEnabled(true);
        yamlFile.setDescription("User profile files for enrichment");

        Map<String, Object> connection = yamlFile.getConnection();
        connection.put("base-path", basePath);  // Use hyphenated key as expected by conversion
        connection.put("file-pattern", "*.json");
        connection.put("encoding", "UTF-8");
        connection.put("watch-for-changes", false);

        Map<String, Object> fileFormat = yamlFile.getFileFormat();
        fileFormat.put("type", "json");
        fileFormat.put("rootPath", "$");

        return yamlFile;
    }

    // ========================================
    // Helper Methods for Test Data Creation
    // ========================================

    private void initializeDatabaseWithTestData(ExternalDataSource dbSource) throws DataSourceException {
        // Get the actual SQL statements from configuration
        DataSourceConfiguration config = dbSource.getConfiguration();
        String createTableSql = config.getQueries().get("createTable");
        String insertDataSql = config.getQueries().get("insertTestData");

        // Clean up any existing data first
        String dropTableSql = "DROP TABLE IF EXISTS users";

        // Execute DDL and DML statements using batchUpdate
        List<String> statements = List.of(dropTableSql, createTableSql, insertDataSql);
        dbSource.batchUpdate(statements);
    }

    private Path createUserProfileFile() throws IOException {
        String profileContent = """
            {
                "profiles": [
                    {
                        "userId": "789",
                        "avatar": "avatar789.jpg",
                        "bio": "Software engineer with 5 years experience",
                        "skills": ["Java", "Python", "SQL"],
                        "location": "San Francisco, CA"
                    }
                ]
            }
            """;

        Path profileFile = tempDir.resolve("user-profiles.json");
        Files.writeString(profileFile, profileContent);
        return profileFile;
    }

    private void populateCacheWithPreferences() throws DataSourceException {
        ExternalDataSource cacheSource = dataSources.get("primary-cache");

        Map<String, Object> preferences = Map.of(
            "theme", "dark",
            "language", "en",
            "notifications", true,
            "timezone", "America/Los_Angeles"
        );

        Map<String, Object> putParams = Map.of("key", "preferences:user789", "value", preferences);
        cacheSource.query("put", putParams);
    }

    private Map<String, Object> createTestUserData(String id, String name) {
        return Map.of(
            "id", id,
            "name", name,
            "email", name.toLowerCase().replace(" ", ".") + "@example.com",
            "status", "active"
        );
    }
}
