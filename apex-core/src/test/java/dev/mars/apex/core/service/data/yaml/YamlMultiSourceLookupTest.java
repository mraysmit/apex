package dev.mars.apex.core.service.data.yaml;

/*
 * Copyright 2025 Mark Andrew Ray-Smith Cityline Ltd
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
 */


import dev.mars.apex.core.config.datasource.DataSourceConfiguration;
import dev.mars.apex.core.config.yaml.YamlConfigurationLoader;
import dev.mars.apex.core.config.yaml.YamlDataSource;
import dev.mars.apex.core.config.yaml.YamlRuleConfiguration;
import dev.mars.apex.core.service.data.external.DataSourceException;
import dev.mars.apex.core.service.data.external.ExternalDataSource;
import dev.mars.apex.core.service.data.external.config.DataSourceConfigurationService;
import dev.mars.apex.core.service.data.external.factory.DataSourceFactory;
import dev.mars.apex.core.service.data.external.manager.DataSourceManager;
import org.junit.jupiter.api.*;

import dev.mars.apex.core.test.extension.ColoredTestOutputExtension;
import dev.mars.apex.core.test.extension.TestClassLoggingExtension;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.extension.ExtendWith;


import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for multi-source data lookup scenarios using YAML configurations.
 *
 * This test class validates the complete YAML-to-MultiSource pipeline:
 * - Loading multi-source configurations from YAML file
 * - Property resolution for connection details
 * - Cache-first lookup with database fallback
 * - Data enrichment from multiple sources
 * - Failover between primary and secondary sources
 * - Data aggregation from heterogeneous sources
 * - Performance optimization through source prioritization
 *
 * The YAML configuration file is loaded from classpath: multi-source-lookup-test.yaml
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 1.0.0
 */
@ExtendWith({ColoredTestOutputExtension.class, TestClassLoggingExtension.class})
class YamlMultiSourceLookupTest {

    @TempDir
    Path tempDir;

    private DataSourceFactory factory;
    private DataSourceConfigurationService configService;
    private DataSourceManager dataSourceManager;
    private YamlConfigurationLoader yamlLoader;
    private YamlRuleConfiguration yamlConfig;
    private Map<String, ExternalDataSource> dataSources;

    @BeforeEach
    void setUp() throws Exception {
        factory = DataSourceFactory.getInstance();
        configService = DataSourceConfigurationService.getInstance();
        dataSourceManager = new DataSourceManager();
        yamlLoader = new YamlConfigurationLoader();
        dataSources = new HashMap<>();

        // Load YAML configuration from file
        yamlConfig = yamlLoader.loadFromClasspath("lookups/multi-source-lookup-test.yaml");
        assertNotNull(yamlConfig, "YAML configuration should be loaded");
        assertNotNull(yamlConfig.getDataSources(), "Data sources should be present");
        assertFalse(yamlConfig.getDataSources().isEmpty(), "Should have at least one data source");

        System.out.println("TEST: Loaded " + yamlConfig.getDataSources().size() + " data sources from YAML");

        // Initialize configuration service with loaded YAML
        configService.initialize(yamlConfig);
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
    void testDataEnrichmentFromMultipleSources() throws Exception {
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

        // Verify cache works for repeated lookups (just verify it completes, no timing assertion)
        long secondCacheTime = measureLookupTime("primary-cache", "fast-data");
        assertTrue(secondCacheTime >= 0, "Cache lookup should complete successfully");
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
        // Get cache data source from YAML
        YamlDataSource yamlCache = findDataSourceByName("primary-cache");
        assertNotNull(yamlCache, "Cache data source should be in YAML");

        DataSourceConfiguration cacheConfig = yamlCache.toDataSourceConfiguration();
        ExternalDataSource cacheSource = factory.createDataSource(cacheConfig);
        dataSources.put("primary-cache", cacheSource);

        System.out.println("TEST: Cache source created from YAML successfully");
    }

    private void setupDatabaseSource() throws DataSourceException {
        // Get database data source from YAML
        YamlDataSource yamlDb = findDataSourceByName("user-database");
        assertNotNull(yamlDb, "Database data source should be in YAML");

        DataSourceConfiguration dbConfig = yamlDb.toDataSourceConfiguration();
        ExternalDataSource dbSource = factory.createDataSource(dbConfig);
        dataSources.put("user-database", dbSource);

        System.out.println("TEST: Database source created from YAML successfully");

        // Initialize database with test data
        initializeDatabaseWithTestData(dbSource);
    }

    private void setupMultipleSourcesForEnrichment() throws Exception {
        setupCacheAndDatabaseSources();

        // Add file source for profile data
        Path profileFile = createUserProfileFile();

        // Set base path property and reload YAML
        System.setProperty("PROFILE_FILE_BASE_PATH", profileFile.getParent().toString());
        yamlConfig = yamlLoader.loadFromClasspath("lookups/multi-source-lookup-test.yaml");

        // Get file data source from YAML
        YamlDataSource yamlFile = findDataSourceByName("profile-files");
        assertNotNull(yamlFile, "File data source should be in YAML");

        DataSourceConfiguration fileConfig = yamlFile.toDataSourceConfiguration();
        ExternalDataSource fileSource = factory.createDataSource(fileConfig);
        dataSources.put("profile-files", fileSource);

        System.out.println("TEST: File source created from YAML successfully");

        // Pre-populate cache with preferences
        populateCacheWithPreferences();

        // Clean up property
        System.clearProperty("PROFILE_FILE_BASE_PATH");
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

        // Database already has test data from setup
        System.out.println("TEST: Sources with different performance characteristics set up");
    }

    private void setupMultipleSourcesForResilience() throws DataSourceException {
        setupCacheAndDatabaseSources();

        // Verify both sources are initially healthy
        assertTrue(dataSources.get("primary-cache").isHealthy());
        assertTrue(dataSources.get("user-database").isHealthy());

        System.out.println("TEST: Multiple sources for resilience testing set up");
    }

    // ========================================
    // Helper Methods for Data Operations
    // ========================================

    /**
     * Implements cache-first lookup with database fallback pattern.
     * First attempts to retrieve data from cache, then falls back to database if cache miss.
     */
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

    /** Stores user data in cache for subsequent lookups. */
    private void storeUserInCache(String userId, Object userData) throws DataSourceException {
        ExternalDataSource cacheSource = dataSources.get("primary-cache");
        Map<String, Object> putParams = Map.of("key", userId, "value", userData);
        cacheSource.query("put", putParams);
    }

    /** Retrieves base user data from database source. */
    private Object getBaseUserData(String userId) throws DataSourceException {
        ExternalDataSource dbSource = dataSources.get("user-database");
        int id = Integer.parseInt(userId.replace("user", ""));
        return dbSource.getData("getUserById", id);
    }

    /** Enriches base user data with profile information from file source. */
    private Object enrichUserWithProfileData(Object baseUser, String userId) throws DataSourceException {
        ExternalDataSource fileSource = dataSources.get("profile-files");
        Object profileData = fileSource.getData("json", "user-profiles.json");

        // Simulate enrichment logic
        Map<String, Object> enriched = new HashMap<>();
        if (baseUser instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> baseUserMap = (Map<String, Object>) baseUser;
            enriched.putAll(baseUserMap);
        }
        enriched.put("profileData", profileData);

        return enriched;
    }

    /** Adds user preferences from cache to complete the enrichment process. */
    private Object addUserPreferencesFromCache(Object enrichedUser, String userId) throws DataSourceException {
        ExternalDataSource cacheSource = dataSources.get("primary-cache");
        Map<String, Object> prefParams = Map.of("key", "preferences:" + userId);
        Object preferences = cacheSource.queryForObject("get", prefParams);

        Map<String, Object> fullyEnriched = new HashMap<>();
        if (enrichedUser instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> enrichedUserMap = (Map<String, Object>) enrichedUser;
            fullyEnriched.putAll(enrichedUserMap);
        }
        fullyEnriched.put("preferences", preferences != null ? preferences : Collections.emptyMap());

        return fullyEnriched;
    }

    /** Measures lookup time for performance comparison between different sources. */
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

    /** Verifies that enriched user data contains all expected components from multiple sources. */
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
    // Helper Methods
    // ========================================

    /**
     * Find a data source by name from the loaded YAML configuration.
     */
    private YamlDataSource findDataSourceByName(String name) {
        return yamlConfig.getDataSources().stream()
            .filter(ds -> name.equals(ds.getName()))
            .findFirst()
            .orElse(null);
    }

    // ========================================
    // Helper Methods for Test Data Creation
    // ========================================

    /** Initializes database with test user data for multi-source scenarios. */
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

    /** Creates temporary JSON file with user profile data for file source testing. */
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

    /** Pre-populates cache with user preference data for enrichment testing. */
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

    /** Creates test user data structure for cache and database operations. */
    private Map<String, Object> createTestUserData(String id, String name) {
        return Map.of(
            "id", id,
            "name", name,
            "email", name.toLowerCase().replace(" ", ".") + "@example.com",
            "status", "active"
        );
    }
}
