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
import dev.mars.apex.core.config.datasource.ConnectionPoolConfig;
import dev.mars.apex.core.config.datasource.DataSourceConfiguration;
import dev.mars.apex.core.service.data.external.DataSourceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Factory class for creating JDBC DataSources and managing database connections.
 * 
 * This factory supports multiple database types and provides connection pooling
 * using HikariCP for production environments and simple connection pooling
 * for testing environments.
 * 
 * Supported databases:
 * - PostgreSQL
 * - MySQL
 * - Oracle
 * - SQL Server
 * - H2 (for testing)
 * 
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 1.0.0
 * @version 1.0
 */
public class JdbcTemplateFactory {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(JdbcTemplateFactory.class);
    
    // Cache of created data sources to avoid recreating them
    private static final ConcurrentMap<String, DataSource> DATA_SOURCE_CACHE = new ConcurrentHashMap<>();
    
    /**
     * Create a DataSource from the given configuration.
     * 
     * @param config The data source configuration
     * @return Configured DataSource
     * @throws DataSourceException if DataSource creation fails
     */
    public static DataSource createDataSource(DataSourceConfiguration config) throws DataSourceException {
        String cacheKey = generateCacheKey(config);
        
        // Validate configuration before proceeding
        if (config.getSourceType() == null || config.getSourceType().trim().isEmpty()) {
            throw DataSourceException.configurationError("Source type is required for database configuration");
        }

        // Return cached DataSource if available
        DataSource cachedDataSource = DATA_SOURCE_CACHE.get(cacheKey);
        if (cachedDataSource != null) {
            LOGGER.debug("Returning cached DataSource for '{}'", config.getName());
            return cachedDataSource;
        }

        try {
            // Ensure appropriate JDBC driver is loaded
            LOGGER.debug("Ensuring JDBC driver is loaded for '{}'", config.getName());
            ensureDriverLoaded(config);

            LOGGER.debug("Creating new DataSource for '{}'", config.getName());
            DataSource dataSource = createNewDataSource(config);

            // Test the connection before caching
            LOGGER.debug("Testing DataSource connection for '{}'", config.getName());
            testDataSource(dataSource, config);

            // Cache the DataSource
            DATA_SOURCE_CACHE.put(cacheKey, dataSource);

            LOGGER.info("Created and cached DataSource for '{}' ({})",
                config.getName(), config.getSourceType());
            LOGGER.debug("DataSource cache now contains {} entries", DATA_SOURCE_CACHE.size());

            return dataSource;
            
        } catch (Exception e) {
            throw new DataSourceException(DataSourceException.ErrorType.CONFIGURATION_ERROR,
                "Failed to create DataSource for '" + config.getName() + "'", e,
                config.getName(), "createDataSource", false);
        }
    }
    
    /**
     * Create a new DataSource instance.
     */
    private static DataSource createNewDataSource(DataSourceConfiguration config) throws DataSourceException {
        ConnectionConfig connectionConfig = config.getConnection();
        if (connectionConfig == null) {
            throw new DataSourceException(DataSourceException.ErrorType.CONFIGURATION_ERROR,
                "Connection configuration is required for database data source");
        }
        
        String sourceType = config.getSourceType();
        if (sourceType == null) {
            throw new DataSourceException(DataSourceException.ErrorType.CONFIGURATION_ERROR,
                "Source type is required for database data source");
        }
        
        // Try to use HikariCP if available, otherwise use simple DataSource
        try {
            LOGGER.debug("Attempting to create HikariCP DataSource for '{}'", config.getName());
            return createHikariDataSource(config);
        } catch (ClassNotFoundException e) {
            LOGGER.warn("HikariCP not available, using simple DataSource for '{}'", config.getName());
            LOGGER.debug("Falling back to simple DataSource for '{}'", config.getName());
            return createSimpleDataSource(config);
        }
    }
    
    /**
     * Create HikariCP DataSource (preferred for production).
     */
    private static DataSource createHikariDataSource(DataSourceConfiguration config) 
            throws ClassNotFoundException, DataSourceException {
        
        // Check if HikariCP is available
        Class.forName("com.zaxxer.hikari.HikariDataSource");
        
        try {
            // Use reflection to create HikariDataSource to avoid hard dependency
            Class<?> hikariConfigClass = Class.forName("com.zaxxer.hikari.HikariConfig");
            Class<?> hikariDataSourceClass = Class.forName("com.zaxxer.hikari.HikariDataSource");
            
            Object hikariConfig = hikariConfigClass.getDeclaredConstructor().newInstance();
            
            // Set basic connection properties
            ConnectionConfig conn = config.getConnection();
            String jdbcUrl = buildJdbcUrl(config);
            
            hikariConfigClass.getMethod("setJdbcUrl", String.class).invoke(hikariConfig, jdbcUrl);
            hikariConfigClass.getMethod("setUsername", String.class).invoke(hikariConfig, conn.getUsername());
            hikariConfigClass.getMethod("setPassword", String.class).invoke(hikariConfig, conn.getPassword());
            
            // Set connection pool properties
            ConnectionPoolConfig poolConfig = conn.getConnectionPool();
            if (poolConfig != null) {
                if (poolConfig.getMinSize() != null) {
                    hikariConfigClass.getMethod("setMinimumIdle", int.class)
                        .invoke(hikariConfig, poolConfig.getMinSize());
                }
                if (poolConfig.getMaxSize() != null) {
                    hikariConfigClass.getMethod("setMaximumPoolSize", int.class)
                        .invoke(hikariConfig, poolConfig.getMaxSize());
                }
                if (poolConfig.getConnectionTimeout() != null) {
                    hikariConfigClass.getMethod("setConnectionTimeout", long.class)
                        .invoke(hikariConfig, poolConfig.getConnectionTimeout());
                }
                if (poolConfig.getIdleTimeout() != null) {
                    hikariConfigClass.getMethod("setIdleTimeout", long.class)
                        .invoke(hikariConfig, poolConfig.getIdleTimeout());
                }
                if (poolConfig.getMaxLifetime() != null) {
                    hikariConfigClass.getMethod("setMaxLifetime", long.class)
                        .invoke(hikariConfig, poolConfig.getMaxLifetime());
                }
                if (poolConfig.getConnectionTestQuery() != null) {
                    hikariConfigClass.getMethod("setConnectionTestQuery", String.class)
                        .invoke(hikariConfig, poolConfig.getConnectionTestQuery());
                }
                if (poolConfig.getLeakDetectionThreshold() != null && poolConfig.getLeakDetectionThreshold() > 0) {
                    hikariConfigClass.getMethod("setLeakDetectionThreshold", long.class)
                        .invoke(hikariConfig, poolConfig.getLeakDetectionThreshold());
                }
            }
            
            // Set pool name
            hikariConfigClass.getMethod("setPoolName", String.class)
                .invoke(hikariConfig, "SpELRulesEngine-" + config.getName());
            
            // Create DataSource
            return (DataSource) hikariDataSourceClass.getConstructor(hikariConfigClass)
                .newInstance(hikariConfig);
                
        } catch (Exception e) {
            throw new DataSourceException(DataSourceException.ErrorType.CONFIGURATION_ERROR,
                "Failed to create HikariCP DataSource", e, config.getName(), "createHikariDataSource", false);
        }
    }
    
    /**
     * Create simple DataSource (fallback when HikariCP is not available).
     */
    private static DataSource createSimpleDataSource(DataSourceConfiguration config) throws DataSourceException {
        return new SimpleDataSource(config);
    }
    
    /**
     * Build JDBC URL from configuration.
     */
    private static String buildJdbcUrl(DataSourceConfiguration config) throws DataSourceException {
        ConnectionConfig conn = config.getConnection();

        if (config.getSourceType() == null || config.getSourceType().trim().isEmpty()) {
            throw DataSourceException.configurationError("Source type is required for database configuration");
        }

        String sourceType = config.getSourceType().toLowerCase();
        
        switch (sourceType) {
            case "postgresql":
                return String.format("jdbc:postgresql://%s:%d/%s", 
                    conn.getHost(), conn.getPort(), conn.getDatabase());
                    
            case "mysql":
                return String.format("jdbc:mysql://%s:%d/%s", 
                    conn.getHost(), conn.getPort(), conn.getDatabase());
                    
            case "oracle":
                return String.format("jdbc:oracle:thin:@%s:%d:%s", 
                    conn.getHost(), conn.getPort(), conn.getDatabase());
                    
            case "sqlserver":
                return String.format("jdbc:sqlserver://%s:%d;databaseName=%s", 
                    conn.getHost(), conn.getPort(), conn.getDatabase());
                    
            case "h2":
                // H2 can be file-based, in-memory, or TCP server mode
                if (conn.getHost() != null) {
                    // Check if host contains H2 parameters (no port specified) or is a real hostname
                    if (conn.getPort() != null) {
                        // H2 TCP Server mode - host is a real hostname
                        return String.format("jdbc:h2:tcp://%s:%d/%s",
                            conn.getHost(), conn.getPort(), conn.getDatabase());
                    } else {
                        // H2 in-memory mode with custom parameters via host field
                        return String.format("jdbc:h2:mem:%s;%s",
                            conn.getDatabase(), conn.getHost());
                    }
                } else {
                    // H2 file-based or in-memory with default parameters
                    String database = conn.getDatabase();
                    String jdbcUrl;

                    if (database != null && database.startsWith("mem:")) {
                        // Explicit in-memory database: "mem:testdb" or "mem:testdb;PARAM=value"
                        jdbcUrl = buildH2JdbcUrl(database);
                    } else if (database != null && database.equals("mem")) {
                        // Private in-memory database: "mem"
                        jdbcUrl = "jdbc:h2:mem:;DB_CLOSE_DELAY=-1;MODE=PostgreSQL";
                    } else if (database != null) {
                        // File-based database (default for demos): "./path/to/db" or "./path/to/db;PARAM=value"
                        jdbcUrl = buildH2JdbcUrl(database);
                    } else {
                        // Fallback for null database
                        jdbcUrl = "jdbc:h2:mem:default;DB_CLOSE_DELAY=-1;MODE=PostgreSQL";
                    }

                    LOGGER.info("Built H2 JDBC URL: {}", jdbcUrl);
                    return jdbcUrl;
                }
                
            default:
                throw new DataSourceException(DataSourceException.ErrorType.CONFIGURATION_ERROR,
                    "Unsupported database type: " + sourceType);
        }
    }

    /**
     * Build H2 JDBC URL with support for optional parameters in the database field.
     *
     * Supports formats like:
     * - "./target/h2-demo/testdb" → "jdbc:h2:./target/h2-demo/testdb;DB_CLOSE_DELAY=-1;MODE=PostgreSQL"
     * - "./target/h2-demo/testdb;MODE=MySQL" → "jdbc:h2:./target/h2-demo/testdb;MODE=MySQL;DB_CLOSE_DELAY=-1"
     * - "mem:testdb;TRACE_LEVEL_FILE=4" → "jdbc:h2:mem:testdb;TRACE_LEVEL_FILE=4;DB_CLOSE_DELAY=-1;MODE=PostgreSQL"
     *
     * @param database The database field from configuration (may contain parameters)
     * @return Complete H2 JDBC URL with default and custom parameters
     */
    private static String buildH2JdbcUrl(String database) {
        if (database == null || database.trim().isEmpty()) {
            return "jdbc:h2:mem:default;DB_CLOSE_DELAY=-1;MODE=PostgreSQL";
        }

        // Check if database field contains parameters (semicolon-separated)
        String[] parts = database.split(";", 2);
        String databasePath = parts[0].trim();
        String customParams = parts.length > 1 ? parts[1].trim() : "";

        // Build base JDBC URL
        String baseUrl = "jdbc:h2:" + databasePath;

        // Prepare default parameters
        String defaultParams = "DB_CLOSE_DELAY=-1;MODE=PostgreSQL";

        if (customParams.isEmpty()) {
            // No custom parameters - use defaults
            return baseUrl + ";" + defaultParams;
        } else {
            // Custom parameters provided - merge with defaults
            return mergeH2Parameters(baseUrl, customParams, defaultParams);
        }
    }

    /**
     * Merge custom H2 parameters with default parameters, avoiding duplicates.
     * Custom parameters take precedence over defaults.
     *
     * @param baseUrl The base JDBC URL (e.g., "jdbc:h2:./path/to/db")
     * @param customParams Custom parameters from user configuration
     * @param defaultParams Default parameters to apply if not overridden
     * @return Complete JDBC URL with merged parameters
     */
    private static String mergeH2Parameters(String baseUrl, String customParams, String defaultParams) {
        // Parse custom parameters into a map
        Map<String, String> paramMap = new HashMap<>();

        // Add default parameters first
        for (String param : defaultParams.split(";")) {
            if (!param.trim().isEmpty()) {
                String[] keyValue = param.split("=", 2);
                if (keyValue.length == 2) {
                    paramMap.put(keyValue[0].trim(), keyValue[1].trim());
                } else {
                    paramMap.put(keyValue[0].trim(), "");
                }
            }
        }

        // Override with custom parameters
        for (String param : customParams.split(";")) {
            if (!param.trim().isEmpty()) {
                String[] keyValue = param.split("=", 2);
                if (keyValue.length == 2) {
                    paramMap.put(keyValue[0].trim(), keyValue[1].trim());
                } else {
                    paramMap.put(keyValue[0].trim(), "");
                }
            }
        }

        // Build final parameter string
        StringBuilder finalParams = new StringBuilder();
        for (Map.Entry<String, String> entry : paramMap.entrySet()) {
            if (finalParams.length() > 0) {
                finalParams.append(";");
            }
            finalParams.append(entry.getKey());
            if (!entry.getValue().isEmpty()) {
                finalParams.append("=").append(entry.getValue());
            }
        }

        return baseUrl + ";" + finalParams.toString();
    }

    /**
     * Ensure the appropriate JDBC driver is loaded for the given database type.
     * This handles both modular (automatic via SPI) and classpath (manual loading) scenarios.
     */
    private static void ensureDriverLoaded(DataSourceConfiguration config) throws DataSourceException {
        if (config.getSourceType() == null || config.getSourceType().trim().isEmpty()) {
            throw DataSourceException.configurationError("Source type is required for database configuration");
        }

        String sourceType = config.getSourceType().toLowerCase();
        LOGGER.debug("Ensuring JDBC driver is loaded for database type: {}", sourceType);
        String driverClassName = getDriverClassName(sourceType);

        if (driverClassName != null) {
            try {
                // Try to load the driver class explicitly
                // This works in both modular and classpath scenarios
                Class.forName(driverClassName);
                LOGGER.debug("Successfully loaded JDBC driver: {} for database type: {}",
                    driverClassName, sourceType);
            } catch (ClassNotFoundException e) {
                LOGGER.warn("JDBC driver not found: {} for database type: {}. " +
                    "Relying on automatic driver loading.", driverClassName, sourceType);
                // Don't throw exception - let automatic driver loading handle it
                // If the driver is truly missing, the connection attempt will fail with a clear error
            }
        }
    }

    /**
     * Get the JDBC driver class name for a given database type.
     */
    private static String getDriverClassName(String sourceType) {
        switch (sourceType) {
            case "postgresql":
                return "org.postgresql.Driver";
            case "mysql":
                return "com.mysql.cj.jdbc.Driver";
            case "oracle":
                return "oracle.jdbc.OracleDriver";
            case "sqlserver":
                return "com.microsoft.sqlserver.jdbc.SQLServerDriver";
            case "h2":
                return "org.h2.Driver";
            default:
                LOGGER.debug("Unknown database type: {}. No explicit driver loading.", sourceType);
                return null;
        }
    }

    /**
     * Test the DataSource by attempting to get a connection.
     */
    private static void testDataSource(DataSource dataSource, DataSourceConfiguration config) 
            throws DataSourceException {
        try (Connection connection = dataSource.getConnection()) {
            if (connection == null || connection.isClosed()) {
                throw new DataSourceException(DataSourceException.ErrorType.CONNECTION_ERROR,
                    "Failed to establish database connection", null, config.getName(), "testConnection", true);
            }
            LOGGER.debug("Successfully tested DataSource connection for '{}'", config.getName());
        } catch (SQLException e) {
            throw new DataSourceException(DataSourceException.ErrorType.CONNECTION_ERROR,
                "Database connection test failed", e, config.getName(), "testConnection", true);
        }
    }
    
    /**
     * Generate cache key for DataSource.
     */
    private static String generateCacheKey(DataSourceConfiguration config) {
        ConnectionConfig conn = config.getConnection();
        if (conn == null) {
            return String.format("%s:%s:null",
                config.getName(),
                config.getSourceType());
        }
        return String.format("%s:%s:%s:%d:%s",
            config.getName(),
            config.getSourceType(),
            conn.getHost(),
            conn.getPort(),
            conn.getDatabase());
    }
    
    /**
     * Remove DataSource from cache.
     */
    public static void removeFromCache(DataSourceConfiguration config) {
        String cacheKey = generateCacheKey(config);
        DataSource removed = DATA_SOURCE_CACHE.remove(cacheKey);
        if (removed != null) {
            LOGGER.info("Removed DataSource from cache for '{}'", config.getName());
        }
    }
    
    /**
     * Clear all cached DataSources.
     */
    public static void clearCache() {
        int size = DATA_SOURCE_CACHE.size();
        DATA_SOURCE_CACHE.clear();
        LOGGER.info("Cleared {} DataSources from cache", size);
    }
    
    /**
     * Simple DataSource implementation for fallback scenarios.
     */
    private static class SimpleDataSource implements DataSource {
        private final String jdbcUrl;
        private final String username;
        private final String password;
        private final Properties properties;
        
        public SimpleDataSource(DataSourceConfiguration config) throws DataSourceException {
            this.jdbcUrl = buildJdbcUrl(config);
            this.username = config.getConnection().getUsername();
            this.password = config.getConnection().getPassword();
            this.properties = new Properties();
            
            if (username != null) properties.setProperty("user", username);
            if (password != null) properties.setProperty("password", password);
        }
        
        @Override
        public Connection getConnection() throws SQLException {
            return java.sql.DriverManager.getConnection(jdbcUrl, properties);
        }
        
        @Override
        public Connection getConnection(String username, String password) throws SQLException {
            Properties props = new Properties(properties);
            props.setProperty("user", username);
            props.setProperty("password", password);
            return java.sql.DriverManager.getConnection(jdbcUrl, props);
        }
        
        // Other DataSource methods with default implementations
        @Override public java.io.PrintWriter getLogWriter() { return null; }
        @Override public void setLogWriter(java.io.PrintWriter out) {}
        @Override public void setLoginTimeout(int seconds) {}
        @Override public int getLoginTimeout() { return 0; }
        @Override public java.util.logging.Logger getParentLogger() { return null; }
        @Override public <T> T unwrap(Class<T> iface) throws SQLException { 
            throw new SQLException("Not supported"); 
        }
        @Override public boolean isWrapperFor(Class<?> iface) { return false; }
    }
}
