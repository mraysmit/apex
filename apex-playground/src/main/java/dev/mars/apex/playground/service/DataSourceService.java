/*
 * Copyright 2025 Mark Raysmith
 * Licensed under the Apache License, Version 2.0
 */
package dev.mars.apex.playground.service;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import dev.mars.apex.playground.model.DataSourceConnection;
import dev.mars.apex.playground.model.DatabaseSchema;
import dev.mars.apex.playground.model.QueryRequest;
import dev.mars.apex.playground.model.QueryResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.sql.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for managing database connections, executing queries, and introspecting schemas.
 * Provides connection pooling via HikariCP for performance and resource management.
 */
@Service
public class DataSourceService {
    
    private static final Logger logger = LoggerFactory.getLogger(DataSourceService.class);
    
    private final Map<String, HikariDataSource> connectionPools = new ConcurrentHashMap<>();
    private final Map<String, DataSourceConnection> connections = new ConcurrentHashMap<>();
    
    /**
     * Create a new database connection with connection pooling.
     * 
     * @param connection The connection configuration
     * @return The created connection with ID and connection status
     * @throws RuntimeException if connection fails
     */
    public DataSourceConnection createConnection(DataSourceConnection connection) {
        connection.setId(UUID.randomUUID().toString());
        connection.setCreatedAt(Instant.now());
        
        // Create connection pool
        HikariDataSource dataSource = createDataSource(connection);
        
        // Test connection
        try (Connection conn = dataSource.getConnection()) {
            connection.setConnected(conn.isValid(5));
        } catch (SQLException e) {
            dataSource.close();
            throw new RuntimeException("Failed to connect: " + e.getMessage(), e);
        }
        
        connectionPools.put(connection.getId(), dataSource);
        connections.put(connection.getId(), connection);
        
        logger.info("Created connection: {} ({}) - {}", connection.getName(), connection.getId(), connection.getType());
        return connection;
    }
    
    /**
     * Test a connection without creating it permanently.
     * 
     * @param connection The connection configuration to test
     * @return true if connection is valid, false otherwise
     */
    public boolean testConnection(DataSourceConnection connection) {
        HikariDataSource testDataSource = null;
        try {
            testDataSource = createDataSource(connection);
            try (Connection conn = testDataSource.getConnection()) {
                return conn.isValid(5);
            }
        } catch (Exception e) {
            logger.warn("Connection test failed for {}: {}", connection.getName(), e.getMessage());
            return false;
        } finally {
            if (testDataSource != null) {
                testDataSource.close();
            }
        }
    }
    
    /**
     * Execute a SQL query and return results.
     * 
     * @param connectionId The connection ID to use
     * @param request The query request with SQL and pagination
     * @return Query results with columns, rows, and metadata
     * @throws RuntimeException if query execution fails
     */
    public QueryResult executeQuery(String connectionId, QueryRequest request) {
        HikariDataSource dataSource = getDataSource(connectionId);
        DataSourceConnection connection = connections.get(connectionId);
        
        long startTime = System.currentTimeMillis();
        String sql = request.getSql().trim();
        String sqlUpper = sql.toUpperCase();
        
        // Determine if this is a query (SELECT) or an update statement (INSERT, UPDATE, DELETE, CREATE, etc.)
        boolean isQuery = sqlUpper.startsWith("SELECT") || sqlUpper.startsWith("WITH");
        
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            
            if (isQuery) {
                // Handle SELECT queries
                stmt.setMaxRows(request.getLimit() + 1); // +1 to check for more rows
                
                try (ResultSet rs = stmt.executeQuery(sql)) {
                    QueryResult result = new QueryResult();
                    
                    // Get column metadata
                    ResultSetMetaData metaData = rs.getMetaData();
                    int columnCount = metaData.getColumnCount();
                    List<String> columns = new ArrayList<>();
                    for (int i = 1; i <= columnCount; i++) {
                        columns.add(metaData.getColumnName(i));
                    }
                    result.setColumns(columns);
                    
                    // Get rows
                    List<List<Object>> rows = new ArrayList<>();
                    int rowCount = 0;
                    
                    while (rs.next() && rowCount < request.getLimit()) {
                        List<Object> row = new ArrayList<>();
                        for (int i = 1; i <= columnCount; i++) {
                            row.add(rs.getObject(i));
                        }
                        rows.add(row);
                        rowCount++;
                    }
                    
                    // Check if there are more rows
                    result.setHasMore(rs.next());
                    
                    result.setRows(rows);
                    result.setRowCount(rowCount);
                    result.setExecutionTimeMs(System.currentTimeMillis() - startTime);
                    
                    // Update last used timestamp
                    connection.setLastUsed(Instant.now());
                    
                    logger.info("Query executed successfully: {} rows in {}ms", rowCount, result.getExecutionTimeMs());
                    return result;
                }
            } else {
                // Handle DML/DDL statements (INSERT, UPDATE, DELETE, CREATE, ALTER, DROP, etc.)
                int affectedRows = stmt.executeUpdate(sql);
                
                QueryResult result = new QueryResult();
                result.setColumns(new ArrayList<>());
                result.setRows(new ArrayList<>());
                result.setRowCount(affectedRows);
                result.setExecutionTimeMs(System.currentTimeMillis() - startTime);
                result.setHasMore(false);
                
                // Update last used timestamp
                connection.setLastUsed(Instant.now());
                
                logger.info("Statement executed successfully: {} rows affected in {}ms", affectedRows, result.getExecutionTimeMs());
                return result;
            }
            
        } catch (SQLException e) {
            logger.error("Query execution failed for connection {}: {}", connectionId, e.getMessage());
            throw new RuntimeException("Query failed: " + e.getMessage(), e);
        }
    }
    
    /**
     * Get database schema information including tables and columns.
     * 
     * @param connectionId The connection ID to introspect
     * @return Database schema with table and column metadata
     * @throws RuntimeException if schema introspection fails
     */
    public DatabaseSchema getSchema(String connectionId) {
        HikariDataSource dataSource = getDataSource(connectionId);
        
        try (Connection conn = dataSource.getConnection()) {
            DatabaseMetaData metaData = conn.getMetaData();
            
            DatabaseSchema schema = new DatabaseSchema();
            schema.setDatabase(conn.getCatalog());
            
            List<DatabaseSchema.TableInfo> tables = new ArrayList<>();
            
            // Get all tables
            try (ResultSet rs = metaData.getTables(null, null, "%", new String[]{"TABLE"})) {
                while (rs.next()) {
                    DatabaseSchema.TableInfo table = new DatabaseSchema.TableInfo();
                    table.setName(rs.getString("TABLE_NAME"));
                    table.setSchema(rs.getString("TABLE_SCHEM"));
                    
                    // Get columns for this table
                    List<DatabaseSchema.ColumnInfo> columns = new ArrayList<>();
                    try (ResultSet colRs = metaData.getColumns(null, table.getSchema(), table.getName(), "%")) {
                        while (colRs.next()) {
                            DatabaseSchema.ColumnInfo column = new DatabaseSchema.ColumnInfo();
                            column.setName(colRs.getString("COLUMN_NAME"));
                            column.setType(colRs.getString("TYPE_NAME"));
                            column.setNullable(colRs.getInt("NULLABLE") == DatabaseMetaData.columnNullable);
                            columns.add(column);
                        }
                    }
                    
                    // Get primary keys for this table
                    try (ResultSet pkRs = metaData.getPrimaryKeys(null, table.getSchema(), table.getName())) {
                        Set<String> pkColumns = new HashSet<>();
                        while (pkRs.next()) {
                            pkColumns.add(pkRs.getString("COLUMN_NAME"));
                        }
                        columns.forEach(col -> col.setPrimaryKey(pkColumns.contains(col.getName())));
                    }
                    
                    table.setColumns(columns);
                    tables.add(table);
                }
            }
            
            schema.setTables(tables);
            logger.info("Retrieved schema for connection {}: {} tables", connectionId, tables.size());
            return schema;
            
        } catch (SQLException e) {
            logger.error("Schema introspection failed for connection {}: {}", connectionId, e.getMessage());
            throw new RuntimeException("Failed to get schema: " + e.getMessage(), e);
        }
    }
    
    /**
     * Get all registered connections.
     * 
     * @return List of all connections
     */
    public List<DataSourceConnection> getAllConnections() {
        return new ArrayList<>(connections.values());
    }
    
    /**
     * Get a specific connection by ID.
     * 
     * @param connectionId The connection ID
     * @return The connection or null if not found
     */
    public DataSourceConnection getConnection(String connectionId) {
        return connections.get(connectionId);
    }
    
    /**
     * Delete a connection and close its connection pool.
     * 
     * @param connectionId The connection ID to delete
     */
    public void deleteConnection(String connectionId) {
        HikariDataSource dataSource = connectionPools.remove(connectionId);
        if (dataSource != null) {
            dataSource.close();
        }
        DataSourceConnection removed = connections.remove(connectionId);
        if (removed != null) {
            logger.info("Deleted connection: {} ({})", removed.getName(), connectionId);
        }
    }
    
    /**
     * Update an existing connection.
     * Note: This closes the old pool and creates a new one.
     * 
     * @param connectionId The connection ID to update
     * @param updatedConnection The updated connection configuration
     * @return The updated connection
     */
    public DataSourceConnection updateConnection(String connectionId, DataSourceConnection updatedConnection) {
        // Delete old connection
        deleteConnection(connectionId);
        
        // Create new connection with same ID
        updatedConnection.setId(connectionId);
        updatedConnection.setCreatedAt(connections.get(connectionId) != null ? 
            connections.get(connectionId).getCreatedAt() : Instant.now());
        
        return createConnection(updatedConnection);
    }
    
    // Private helper methods
    
    /**
     * Create a HikariCP data source from connection configuration.
     */
    private HikariDataSource createDataSource(DataSourceConnection connection) {
        HikariConfig config = new HikariConfig();
        
        config.setJdbcUrl(buildJdbcUrl(connection));
        config.setUsername(connection.getUsername());
        config.setPassword(connection.getPassword());
        
        // Connection pool settings
        config.setMaximumPoolSize(5);
        config.setMinimumIdle(1);
        config.setConnectionTimeout(10000);
        config.setIdleTimeout(300000);
        config.setMaxLifetime(600000);
        
        // Add custom properties
        if (connection.getProperties() != null) {
            connection.getProperties().forEach(config::addDataSourceProperty);
        }
        
        // Set pool name for better logging
        config.setPoolName("apex-" + connection.getName());
        
        return new HikariDataSource(config);
    }
    
    /**
     * Build JDBC URL from connection configuration.
     */
    private String buildJdbcUrl(DataSourceConnection connection) {
        switch (connection.getType()) {
            case POSTGRESQL:
                return String.format("jdbc:postgresql://%s:%d/%s", 
                    connection.getHost(), connection.getPort(), connection.getDatabase());
            case MYSQL:
                return String.format("jdbc:mysql://%s:%d/%s", 
                    connection.getHost(), connection.getPort(), connection.getDatabase());
            case ORACLE:
                return String.format("jdbc:oracle:thin:@%s:%d:%s", 
                    connection.getHost(), connection.getPort(), connection.getDatabase());
            case SQLSERVER:
                return String.format("jdbc:sqlserver://%s:%d;databaseName=%s", 
                    connection.getHost(), connection.getPort(), connection.getDatabase());
            case H2:
                // For H2, database can be a file path or memory DB
                if (connection.getDatabase().startsWith("mem:")) {
                    return "jdbc:h2:" + connection.getDatabase() + ";DB_CLOSE_DELAY=-1";
                } else {
                    return "jdbc:h2:file:" + connection.getDatabase();
                }
            default:
                throw new IllegalArgumentException("Unsupported database type: " + connection.getType());
        }
    }
    
    /**
     * Get data source by connection ID.
     * 
     * @throws IllegalArgumentException if connection not found
     */
    private HikariDataSource getDataSource(String connectionId) {
        HikariDataSource dataSource = connectionPools.get(connectionId);
        if (dataSource == null) {
            throw new IllegalArgumentException("Connection not found: " + connectionId);
        }
        return dataSource;
    }
}
