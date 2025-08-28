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


import dev.mars.apex.core.config.datasource.DataSourceConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Health indicator for database data sources.
 * 
 * This class monitors the health of database connections by periodically
 * executing health check queries and tracking connection status.
 * 
 * Features:
 * - Configurable health check intervals
 * - Custom health check queries
 * - Failure threshold tracking
 * - Automatic recovery detection
 * - Background health monitoring
 * 
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 1.0.0
 * @version 1.0
 */
public class DatabaseHealthIndicator {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseHealthIndicator.class);
    
    private final DataSource dataSource;
    private final DataSourceConfiguration configuration;
    
    // Health status tracking
    private final AtomicBoolean healthy = new AtomicBoolean(true);
    private final AtomicInteger consecutiveFailures = new AtomicInteger(0);
    private final AtomicInteger consecutiveSuccesses = new AtomicInteger(0);
    private final AtomicReference<LocalDateTime> lastCheckTime = new AtomicReference<>();
    private final AtomicReference<String> lastError = new AtomicReference<>();
    
    // Background health monitoring
    private ScheduledExecutorService healthCheckExecutor;
    private volatile boolean monitoring = false;
    
    // Default health check query
    private static final String DEFAULT_HEALTH_CHECK_QUERY = "SELECT 1";
    
    /**
     * Constructor with DataSource and configuration.
     * 
     * @param dataSource The JDBC DataSource to monitor
     * @param configuration The data source configuration
     */
    public DatabaseHealthIndicator(DataSource dataSource, DataSourceConfiguration configuration) {
        this.dataSource = dataSource;
        this.configuration = configuration;
        
        // Start background health monitoring if configured
        if (isHealthCheckEnabled()) {
            startHealthMonitoring();
        }
    }
    
    /**
     * Check if the database is currently healthy.
     * 
     * @return true if the database is healthy
     */
    public boolean isHealthy() {
        // If background monitoring is not enabled, perform immediate check
        if (!monitoring) {
            return performHealthCheck();
        }
        
        return healthy.get();
    }
    
    /**
     * Perform an immediate health check.
     * 
     * @return true if the health check passes
     */
    public boolean performHealthCheck() {
        long startTime = System.currentTimeMillis();
        boolean checkPassed = false;
        
        try {
            String healthCheckQuery = getHealthCheckQuery();
            
            try (Connection connection = dataSource.getConnection();
                 Statement statement = connection.createStatement()) {
                
                // Set query timeout if configured
                if (isHealthCheckEnabled() && configuration.getHealthCheck().getTimeoutSeconds() != null) {
                    statement.setQueryTimeout(configuration.getHealthCheck().getTimeoutSeconds().intValue());
                }
                
                // Execute health check query
                try (ResultSet resultSet = statement.executeQuery(healthCheckQuery)) {
                    checkPassed = resultSet.next();
                }
            }
            
            // Update health status
            if (checkPassed) {
                onHealthCheckSuccess();
            } else {
                onHealthCheckFailure("Health check query returned no results");
            }
            
        } catch (SQLException e) {
            onHealthCheckFailure("Health check failed: " + e.getMessage());
            if (shouldLogFailures()) {
                LOGGER.warn("Database health check failed for '{}': {}", 
                    configuration.getName(), e.getMessage());
            }
        } catch (Exception e) {
            onHealthCheckFailure("Unexpected error during health check: " + e.getMessage());
            if (shouldLogFailures()) {
                LOGGER.error("Unexpected error during database health check for '{}'", 
                    configuration.getName(), e);
            }
        } finally {
            lastCheckTime.set(LocalDateTime.now());
            
            long duration = System.currentTimeMillis() - startTime;
            if (duration > getHealthCheckTimeoutMillis()) {
                LOGGER.warn("Database health check for '{}' took {}ms (timeout: {}ms)", 
                    configuration.getName(), duration, getHealthCheckTimeoutMillis());
            }
        }
        
        return checkPassed;
    }
    
    /**
     * Start background health monitoring.
     */
    public void startHealthMonitoring() {
        if (monitoring || !isHealthCheckEnabled()) {
            return;
        }
        
        long intervalSeconds = configuration.getHealthCheck().getIntervalSeconds();
        
        healthCheckExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r, "DatabaseHealthCheck-" + configuration.getName());
            thread.setDaemon(true);
            return thread;
        });
        
        healthCheckExecutor.scheduleAtFixedRate(
            this::performHealthCheck,
            0, // Initial delay
            intervalSeconds,
            TimeUnit.SECONDS
        );
        
        monitoring = true;
        LOGGER.info("Started health monitoring for database '{}' with interval {}s", 
            configuration.getName(), intervalSeconds);
    }
    
    /**
     * Stop background health monitoring.
     */
    public void stopHealthMonitoring() {
        if (!monitoring) {
            return;
        }
        
        if (healthCheckExecutor != null) {
            healthCheckExecutor.shutdown();
            try {
                if (!healthCheckExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                    healthCheckExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                healthCheckExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        
        monitoring = false;
        LOGGER.info("Stopped health monitoring for database '{}'", configuration.getName());
    }
    
    /**
     * Get health status information.
     * 
     * @return Health status details
     */
    public HealthStatus getHealthStatus() {
        return new HealthStatus(
            healthy.get(),
            consecutiveFailures.get(),
            consecutiveSuccesses.get(),
            lastCheckTime.get(),
            lastError.get()
        );
    }
    
    /**
     * Handle successful health check.
     */
    private void onHealthCheckSuccess() {
        consecutiveSuccesses.incrementAndGet();
        consecutiveFailures.set(0);
        lastError.set(null);
        
        // Mark as healthy if we have enough consecutive successes
        if (!healthy.get()) {
            int successThreshold = getSuccessThreshold();
            if (consecutiveSuccesses.get() >= successThreshold) {
                healthy.set(true);
                LOGGER.info("Database '{}' is now healthy after {} consecutive successful checks", 
                    configuration.getName(), consecutiveSuccesses.get());
            }
        }
    }
    
    /**
     * Handle failed health check.
     */
    private void onHealthCheckFailure(String errorMessage) {
        consecutiveFailures.incrementAndGet();
        consecutiveSuccesses.set(0);
        lastError.set(errorMessage);
        
        // Mark as unhealthy if we have too many consecutive failures
        if (healthy.get()) {
            int failureThreshold = getFailureThreshold();
            if (consecutiveFailures.get() >= failureThreshold) {
                healthy.set(false);
                LOGGER.warn("Database '{}' is now unhealthy after {} consecutive failed checks", 
                    configuration.getName(), consecutiveFailures.get());
            }
        }
    }
    
    /**
     * Get the health check query to execute.
     */
    private String getHealthCheckQuery() {
        if (isHealthCheckEnabled() && configuration.getHealthCheck().getQuery() != null) {
            return configuration.getHealthCheck().getQuery();
        }
        return DEFAULT_HEALTH_CHECK_QUERY;
    }
    
    /**
     * Check if health checking is enabled.
     */
    private boolean isHealthCheckEnabled() {
        return configuration.getHealthCheck() != null && configuration.getHealthCheck().isEnabled();
    }
    
    /**
     * Check if failures should be logged.
     */
    private boolean shouldLogFailures() {
        return !isHealthCheckEnabled() || configuration.getHealthCheck().shouldLogFailures();
    }
    
    /**
     * Get the failure threshold.
     */
    private int getFailureThreshold() {
        if (isHealthCheckEnabled() && configuration.getHealthCheck().getFailureThreshold() != null) {
            return configuration.getHealthCheck().getFailureThreshold();
        }
        return 3; // Default
    }
    
    /**
     * Get the success threshold.
     */
    private int getSuccessThreshold() {
        if (isHealthCheckEnabled() && configuration.getHealthCheck().getSuccessThreshold() != null) {
            return configuration.getHealthCheck().getSuccessThreshold();
        }
        return 1; // Default
    }
    
    /**
     * Get the health check timeout in milliseconds.
     */
    private long getHealthCheckTimeoutMillis() {
        if (isHealthCheckEnabled() && configuration.getHealthCheck().getTimeoutSeconds() != null) {
            return configuration.getHealthCheck().getTimeoutSeconds() * 1000L;
        }
        return 10000L; // Default 10 seconds
    }
    
    /**
     * Shutdown the health indicator.
     */
    public void shutdown() {
        stopHealthMonitoring();
    }
    
    /**
     * Health status information holder.
     */
    public static class HealthStatus {
        private final boolean healthy;
        private final int consecutiveFailures;
        private final int consecutiveSuccesses;
        private final LocalDateTime lastCheckTime;
        private final String lastError;
        
        public HealthStatus(boolean healthy, int consecutiveFailures, int consecutiveSuccesses,
                          LocalDateTime lastCheckTime, String lastError) {
            this.healthy = healthy;
            this.consecutiveFailures = consecutiveFailures;
            this.consecutiveSuccesses = consecutiveSuccesses;
            this.lastCheckTime = lastCheckTime;
            this.lastError = lastError;
        }
        
        public boolean isHealthy() { return healthy; }
        public int getConsecutiveFailures() { return consecutiveFailures; }
        public int getConsecutiveSuccesses() { return consecutiveSuccesses; }
        public LocalDateTime getLastCheckTime() { return lastCheckTime; }
        public String getLastError() { return lastError; }
        
        @Override
        public String toString() {
            return "HealthStatus{" +
                   "healthy=" + healthy +
                   ", consecutiveFailures=" + consecutiveFailures +
                   ", consecutiveSuccesses=" + consecutiveSuccesses +
                   ", lastCheckTime=" + lastCheckTime +
                   ", lastError='" + lastError + '\'' +
                   '}';
        }
    }
}
