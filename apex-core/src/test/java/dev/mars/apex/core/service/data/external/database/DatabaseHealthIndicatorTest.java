package dev.mars.apex.core.service.data.external.database;

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


import dev.mars.apex.core.config.datasource.ConnectionConfig;
import dev.mars.apex.core.config.datasource.DataSourceConfiguration;
import dev.mars.apex.core.config.datasource.HealthCheckConfig;
import dev.mars.apex.core.service.data.external.DataSourceException;
import org.junit.jupiter.api.*;


import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive unit tests for DatabaseHealthIndicator.
 *
 * Tests cover:
 * - Health check execution and results
 * - Background health monitoring
 * - Failure and success threshold handling
 * - Custom health check queries
 * - Timeout handling
 * - Health status tracking
 * - Configuration-based behavior
 * - Resource management and cleanup
 *
 * Uses H2 in-memory database for real database testing without external dependencies.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 1.0.0
 */
class DatabaseHealthIndicatorTest {

    private DataSource dataSource;
    private DatabaseHealthIndicator healthIndicator;
    private DataSourceConfiguration configuration;

    @BeforeEach
    void setUp() throws DataSourceException, SQLException {
        // Setup configuration
        configuration = createTestConfiguration();

        // Create real H2 DataSource
        dataSource = JdbcTemplateFactory.createDataSource(configuration);

        // Initialize database with test data
        initializeTestDatabase();

        // Create health indicator (without background monitoring initially)
        configuration.getHealthCheck().setEnabled(false); // Disable background monitoring
        healthIndicator = new DatabaseHealthIndicator(dataSource, configuration);
    }

    @AfterEach
    void tearDown() {
        if (healthIndicator != null) {
            healthIndicator.shutdown();
        }
        JdbcTemplateFactory.clearCache();
    }

    // ========================================
    // Basic Health Check Tests
    // ========================================

    @Test
    @DisplayName("Should perform successful health check")
    void testSuccessfulHealthCheck() {
        boolean healthy = healthIndicator.performHealthCheck();

        assertTrue(healthy);

        DatabaseHealthIndicator.HealthStatus status = healthIndicator.getHealthStatus();
        assertEquals(1, status.getConsecutiveSuccesses());
        assertEquals(0, status.getConsecutiveFailures());
        assertNull(status.getLastError());
        assertNotNull(status.getLastCheckTime());
    }

    @Test
    @DisplayName("Should use custom health check query")
    void testCustomHealthCheckQuery() throws SQLException {
        String customQuery = "SELECT COUNT(*) FROM test_users";
        configuration.getHealthCheck().setQuery(customQuery);

        healthIndicator = new DatabaseHealthIndicator(dataSource, configuration);
        boolean healthy = healthIndicator.performHealthCheck();

        assertTrue(healthy);
    }

    @Test
    @DisplayName("Should handle health check failure with invalid query")
    void testHealthCheckFailureInvalidQuery() throws DataSourceException {
        // Create fresh configuration with invalid query
        DataSourceConfiguration failConfig = createTestConfiguration();
        failConfig.getHealthCheck().setEnabled(true);
        failConfig.getHealthCheck().setQuery("SELECT * FROM nonexistent_table");

        DataSource failDataSource = JdbcTemplateFactory.createDataSource(failConfig);
        DatabaseHealthIndicator failHealthIndicator = new DatabaseHealthIndicator(failDataSource, failConfig);

        boolean healthy = failHealthIndicator.performHealthCheck();

        assertFalse(healthy);

        DatabaseHealthIndicator.HealthStatus status = failHealthIndicator.getHealthStatus();
        assertEquals(0, status.getConsecutiveSuccesses());
        // The health indicator performs an initial check when created, so we expect 2 failures (1 initial + 1 manual)
        assertTrue(status.getConsecutiveFailures() >= 1); // At least 1 failure
        assertNotNull(status.getLastError());

        failHealthIndicator.shutdown();
    }

    @Test
    @DisplayName("Should set query timeout when configured")
    void testQueryTimeout() {
        configuration.getHealthCheck().setTimeoutSeconds(1L);

        healthIndicator = new DatabaseHealthIndicator(dataSource, configuration);
        boolean healthy = healthIndicator.performHealthCheck();

        // Should still be healthy with a reasonable timeout
        assertTrue(healthy);
    }

    // ========================================
    // Health Status Tracking Tests
    // ========================================

    @Test
    @DisplayName("Should track consecutive failures")
    void testConsecutiveFailures() throws DataSourceException {
        // Create fresh configuration with invalid query
        DataSourceConfiguration failConfig = createTestConfiguration();
        failConfig.getHealthCheck().setEnabled(true);
        failConfig.getHealthCheck().setQuery("SELECT * FROM nonexistent_table");

        DataSource failDataSource = JdbcTemplateFactory.createDataSource(failConfig);
        DatabaseHealthIndicator failHealthIndicator = new DatabaseHealthIndicator(failDataSource, failConfig);

        // Perform multiple failed health checks
        failHealthIndicator.performHealthCheck();
        failHealthIndicator.performHealthCheck();
        failHealthIndicator.performHealthCheck();

        DatabaseHealthIndicator.HealthStatus status = failHealthIndicator.getHealthStatus();
        // The health indicator performs an initial check when created, so we expect 4 failures (1 initial + 3 manual)
        assertTrue(status.getConsecutiveFailures() >= 3); // At least 3 failures
        assertEquals(0, status.getConsecutiveSuccesses());
        assertNotNull(status.getLastError());

        failHealthIndicator.shutdown();
    }

    @Test
    @DisplayName("Should track consecutive successes")
    void testConsecutiveSuccesses() {
        // Perform multiple successful health checks
        healthIndicator.performHealthCheck();
        healthIndicator.performHealthCheck();
        healthIndicator.performHealthCheck();

        DatabaseHealthIndicator.HealthStatus status = healthIndicator.getHealthStatus();
        assertEquals(0, status.getConsecutiveFailures());
        assertEquals(3, status.getConsecutiveSuccesses());
        assertNull(status.getLastError());
    }

    @Test
    @DisplayName("Should reset consecutive counters on state change")
    void testConsecutiveCounterReset() throws DataSourceException {
        // Start with failures
        DataSourceConfiguration failConfig = createTestConfiguration();
        failConfig.getHealthCheck().setEnabled(true);
        failConfig.getHealthCheck().setQuery("SELECT * FROM nonexistent_table");

        DataSource failDataSource = JdbcTemplateFactory.createDataSource(failConfig);
        DatabaseHealthIndicator failHealthIndicator = new DatabaseHealthIndicator(failDataSource, failConfig);

        failHealthIndicator.performHealthCheck();
        failHealthIndicator.performHealthCheck();

        DatabaseHealthIndicator.HealthStatus statusAfterFailures = failHealthIndicator.getHealthStatus();
        // The health indicator performs an initial check when created, so we expect 3 failures (1 initial + 2 manual)
        assertTrue(statusAfterFailures.getConsecutiveFailures() >= 2); // At least 2 failures

        // Switch to success with new configuration
        DataSourceConfiguration successConfig = createTestConfiguration();
        successConfig.getHealthCheck().setEnabled(true);
        successConfig.getHealthCheck().setQuery("SELECT 1");

        DataSource successDataSource = JdbcTemplateFactory.createDataSource(successConfig);
        DatabaseHealthIndicator successHealthIndicator = new DatabaseHealthIndicator(successDataSource, successConfig);
        successHealthIndicator.performHealthCheck();

        DatabaseHealthIndicator.HealthStatus statusAfterSuccess = successHealthIndicator.getHealthStatus();
        assertEquals(0, statusAfterSuccess.getConsecutiveFailures());
        // The health indicator performs an initial check when created, so we expect 2 successes (1 initial + 1 manual)
        assertTrue(statusAfterSuccess.getConsecutiveSuccesses() >= 1); // At least 1 success

        failHealthIndicator.shutdown();
        successHealthIndicator.shutdown();
    }

    // ========================================
    // Threshold Handling Tests
    // ========================================

    @Test
    @DisplayName("Should mark unhealthy after failure threshold")
    void testFailureThreshold() throws DataSourceException {
        // Create configuration with failure threshold of 2 and enable monitoring
        DataSourceConfiguration failConfig = createTestConfiguration();
        failConfig.getHealthCheck().setEnabled(true);
        failConfig.getHealthCheck().setFailureThreshold(2);
        failConfig.getHealthCheck().setQuery("SELECT * FROM nonexistent_table");

        DataSource failDataSource = JdbcTemplateFactory.createDataSource(failConfig);
        DatabaseHealthIndicator failHealthIndicator = new DatabaseHealthIndicator(failDataSource, failConfig);

        // Wait a moment for the initial background check to complete
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Perform manual health checks to reach the failure threshold
        failHealthIndicator.performHealthCheck();

        // Check status after reaching threshold
        DatabaseHealthIndicator.HealthStatus status = failHealthIndicator.getHealthStatus();

        // Should have at least 2 consecutive failures (background + manual checks)
        assertTrue(status.getConsecutiveFailures() >= 2,
                  "Expected at least 2 consecutive failures, but got: " + status.getConsecutiveFailures());

        // Should be marked as unhealthy
        assertFalse(status.isHealthy(), "Health indicator should be unhealthy after reaching failure threshold");

        failHealthIndicator.shutdown();
    }

    @Test
    @DisplayName("Should track success threshold correctly")
    void testSuccessThreshold() throws DataSourceException {
        // Create configuration with success threshold of 2
        DataSourceConfiguration config = createTestConfiguration();
        config.getHealthCheck().setEnabled(true);
        config.getHealthCheck().setFailureThreshold(1);
        config.getHealthCheck().setSuccessThreshold(2);
        config.getHealthCheck().setQuery("SELECT 1"); // Valid query

        DataSource testDataSource = JdbcTemplateFactory.createDataSource(config);
        DatabaseHealthIndicator testHealthIndicator = new DatabaseHealthIndicator(testDataSource, config);

        // Health indicator starts healthy and should remain healthy with successful checks
        assertTrue(testHealthIndicator.isHealthy());

        // Perform additional successful health checks
        testHealthIndicator.performHealthCheck();
        assertTrue(testHealthIndicator.isHealthy());

        testHealthIndicator.performHealthCheck();
        assertTrue(testHealthIndicator.isHealthy());

        // Verify consecutive successes are being tracked
        DatabaseHealthIndicator.HealthStatus status = testHealthIndicator.getHealthStatus();
        assertTrue(status.getConsecutiveSuccesses() >= 2);
        assertEquals(0, status.getConsecutiveFailures());

        testHealthIndicator.shutdown();
    }

    // ========================================
    // Background Monitoring Tests
    // ========================================

    @Test
    @DisplayName("Should start background health monitoring")
    void testStartHealthMonitoring() throws DataSourceException {
        // Create configuration with background monitoring enabled
        DataSourceConfiguration monitorConfig = createTestConfiguration();
        monitorConfig.getHealthCheck().setEnabled(true);
        monitorConfig.getHealthCheck().setIntervalSeconds(1L); // 1 second interval

        DataSource monitorDataSource = JdbcTemplateFactory.createDataSource(monitorConfig);
        DatabaseHealthIndicator monitorHealthIndicator = new DatabaseHealthIndicator(monitorDataSource, monitorConfig);

        // Wait for background monitoring to execute
        await().atMost(3, TimeUnit.SECONDS)
               .untilAsserted(() -> {
                   DatabaseHealthIndicator.HealthStatus status = monitorHealthIndicator.getHealthStatus();
                   assertTrue(status.getConsecutiveSuccesses() > 0);
               });

        monitorHealthIndicator.stopHealthMonitoring();
        monitorHealthIndicator.shutdown();
    }

    @Test
    @DisplayName("Should stop background health monitoring")
    void testStopHealthMonitoring() throws InterruptedException {
        configuration.getHealthCheck().setIntervalSeconds(1L);
        healthIndicator = new DatabaseHealthIndicator(dataSource, configuration);

        // Let it run for a bit
        Thread.sleep(1500);

        DatabaseHealthIndicator.HealthStatus statusBeforeStop = healthIndicator.getHealthStatus();
        int successesBeforeStop = statusBeforeStop.getConsecutiveSuccesses();

        // Stop monitoring
        healthIndicator.stopHealthMonitoring();

        // Wait and verify no more health checks
        Thread.sleep(1500);

        DatabaseHealthIndicator.HealthStatus statusAfterStop = healthIndicator.getHealthStatus();
        assertEquals(successesBeforeStop, statusAfterStop.getConsecutiveSuccesses());
    }

    @Test
    @DisplayName("Should not start monitoring when health check is disabled")
    void testNoMonitoringWhenDisabled() throws InterruptedException {
        configuration.getHealthCheck().setEnabled(false);
        healthIndicator = new DatabaseHealthIndicator(dataSource, configuration);

        // Wait a bit
        Thread.sleep(1000);

        // Should not have executed any health checks automatically
        DatabaseHealthIndicator.HealthStatus status = healthIndicator.getHealthStatus();
        assertEquals(0, status.getConsecutiveSuccesses());
        assertEquals(0, status.getConsecutiveFailures());
    }

    // ========================================
    // Configuration Tests
    // ========================================

    @Test
    @DisplayName("Should use default values when health check is disabled")
    void testDefaultValuesWhenDisabled() {
        configuration.setHealthCheck(null);
        healthIndicator = new DatabaseHealthIndicator(dataSource, configuration);

        // Should perform immediate check since monitoring is disabled
        boolean healthy = healthIndicator.isHealthy();

        assertTrue(healthy); // Should use default query "SELECT 1"
    }

    @Test
    @DisplayName("Should handle null health check configuration")
    void testNullHealthCheckConfiguration() {
        configuration.setHealthCheck(null);
        healthIndicator = new DatabaseHealthIndicator(dataSource, configuration);

        assertDoesNotThrow(() -> {
            healthIndicator.performHealthCheck();
        });

        assertTrue(healthIndicator.isHealthy());
    }

    // ========================================
    // Helper Methods
    // ========================================

    private DataSourceConfiguration createTestConfiguration() {
        DataSourceConfiguration config = new DataSourceConfiguration();
        config.setName("test-database");
        config.setType("database");
        config.setSourceType("h2");
        config.setEnabled(true);

        ConnectionConfig connectionConfig = new ConnectionConfig();
        connectionConfig.setHost(null); // In-memory H2
        connectionConfig.setDatabase("testdb_" + System.nanoTime()); // Unique database name per test
        connectionConfig.setUsername("sa");
        connectionConfig.setPassword("");

        config.setConnection(connectionConfig);

        // Set parameter names for parameterized queries
        config.setParameterNames(new String[]{"id"});

        HealthCheckConfig healthCheck = new HealthCheckConfig();
        healthCheck.setEnabled(false); // Disable by default for tests
        healthCheck.setTimeoutSeconds(10L);
        healthCheck.setFailureThreshold(3);
        healthCheck.setSuccessThreshold(1);
        healthCheck.setLogFailures(true);
        config.setHealthCheck(healthCheck);

        return config;
    }

    private void initializeTestDatabase() throws SQLException {
        try (Connection connection = dataSource.getConnection();
             Statement stmt = connection.createStatement()) {

            // Drop table if exists and recreate to ensure clean state
            stmt.execute("DROP TABLE IF EXISTS test_users");
            stmt.execute("CREATE TABLE test_users (id INTEGER PRIMARY KEY, name VARCHAR(50))");

            // Insert test data
            stmt.execute("INSERT INTO test_users VALUES (1, 'Test User')");
        }
    }
}