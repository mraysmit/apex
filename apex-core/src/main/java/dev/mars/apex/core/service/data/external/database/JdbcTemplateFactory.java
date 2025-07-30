package dev.mars.apex.core.service.data.external.database;

import dev.mars.apex.core.config.datasource.ConnectionConfig;
import dev.mars.apex.core.config.datasource.ConnectionPoolConfig;
import dev.mars.apex.core.config.datasource.DataSourceConfiguration;
import dev.mars.apex.core.service.data.external.DataSourceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
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
 * @author SpEL Rules Engine Team
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
        
        // Return cached DataSource if available
        DataSource cachedDataSource = DATA_SOURCE_CACHE.get(cacheKey);
        if (cachedDataSource != null) {
            LOGGER.debug("Returning cached DataSource for '{}'", config.getName());
            return cachedDataSource;
        }
        
        try {
            DataSource dataSource = createNewDataSource(config);
            
            // Test the connection before caching
            testDataSource(dataSource, config);
            
            // Cache the DataSource
            DATA_SOURCE_CACHE.put(cacheKey, dataSource);
            
            LOGGER.info("Created and cached DataSource for '{}' ({})", 
                config.getName(), config.getSourceType());
            
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
            return createHikariDataSource(config);
        } catch (ClassNotFoundException e) {
            LOGGER.warn("HikariCP not available, using simple DataSource for '{}'", config.getName());
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
                // H2 can be file-based or in-memory
                if (conn.getHost() != null) {
                    return String.format("jdbc:h2:tcp://%s:%d/%s", 
                        conn.getHost(), conn.getPort(), conn.getDatabase());
                } else {
                    return String.format("jdbc:h2:mem:%s", conn.getDatabase());
                }
                
            default:
                throw new DataSourceException(DataSourceException.ErrorType.CONFIGURATION_ERROR,
                    "Unsupported database type: " + sourceType);
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
