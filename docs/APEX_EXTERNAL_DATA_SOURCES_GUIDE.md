# External Data Source Integration - Complete Guide

APEX (Advanced Processing Engine for eXpressions) provides comprehensive support for integrating with external data sources, enabling rules to access and process data from databases, REST APIs, file systems, caches, and more. This unified guide combines all aspects of external data source integration including configuration, API reference, best practices, and database-specific guidance.

## Table of Contents

1. [Overview](#overview)
2. [Quick Start](#quick-start)
3. [Supported Data Sources](#supported-data-sources)
4. [Architecture](#architecture)
5. [Configuration](#configuration)
6. [Database Configuration](#database-configuration)
7. [API Reference](#api-reference)
8. [Usage Examples](#usage-examples)
9. [Best Practices](#best-practices)
10. [Performance Optimization](#performance-optimization)
11. [Security Guidelines](#security-guidelines)
12. [Error Handling and Resilience](#error-handling-and-resilience)
13. [Monitoring and Observability](#monitoring-and-observability)
14. [Testing Strategies](#testing-strategies)
15. [Deployment and Operations](#deployment-and-operations)
16. [Troubleshooting](#troubleshooting)

## Overview

The external data source integration provides:

- **Multiple Data Source Types**: Database, REST API, File System, Cache, and extensible custom sources
- **Unified Interface**: Consistent API across all data source types
- **Enterprise Features**: Connection pooling, health monitoring, caching, circuit breakers
- **YAML Configuration**: Declarative configuration with environment-specific overrides
- **High Availability**: Load balancing, failover, and automatic recovery
- **Performance Monitoring**: Comprehensive metrics and statistics collection
- **Thread Safety**: Concurrent access support with proper synchronization

## Quick Start

### 1. Add Dependencies

Ensure you have the SpEL Rules Engine core dependency:

```xml
<dependency>
    <groupId>dev.mars.rulesengine</groupId>
    <artifactId>rules-engine-core</artifactId>
    <version>1.0.0</version>
</dependency>
```

### 2. Basic Configuration

Create a YAML configuration file:

```yaml
# data-sources.yaml
name: "My Application"
version: "1.0.0"

dataSources:
  - name: "user-database"
    type: "database"
    sourceType: "postgresql"
    enabled: true
    
    connection:
      host: "localhost"
      port: 5432
      database: "myapp"
      username: "app_user"
      password: "${DB_PASSWORD}"
    
    queries:
      getUserById: "SELECT * FROM users WHERE id = :id"
      getAllUsers: "SELECT * FROM users ORDER BY created_at DESC"
    
    parameterNames:
      - "id"
    
    cache:
      enabled: true
      ttlSeconds: 300
      maxSize: 1000
```

### 3. Initialize and Use

```java
// Load configuration
DataSourceConfigurationService configService = DataSourceConfigurationService.getInstance();
YamlRuleConfiguration yamlConfig = loadYamlConfiguration("data-sources.yaml");
configService.initialize(yamlConfig);

// Get data source
ExternalDataSource userDb = configService.getDataSource("user-database");

// Execute queries
Map<String, Object> parameters = Map.of("id", 123);
List<Object> results = userDb.query("getUserById", parameters);

// Get single result
Object user = userDb.queryForObject("getUserById", parameters);
```

## Supported Data Sources

### Database Sources
- **PostgreSQL**: Full-featured support with connection pooling
- **MySQL**: Complete MySQL integration with SSL support
- **Oracle**: Enterprise Oracle database connectivity
- **SQL Server**: Microsoft SQL Server integration
- **H2**: In-memory and file-based H2 database support

### REST API Sources
- **Authentication**: Bearer tokens, API keys, Basic auth, OAuth2
- **HTTP Methods**: GET, POST, PUT, DELETE, PATCH
- **Features**: Circuit breakers, retry logic, response caching
- **Formats**: JSON response parsing with JSONPath support

### File System Sources
- **CSV Files**: Configurable delimiters, headers, data type conversion
- **JSON Files**: JSONPath extraction, nested object handling
- **XML Files**: XPath queries, namespace support, attribute extraction
- **Fixed-Width**: Legacy mainframe file format support
- **Plain Text**: Log files and unstructured text processing

### Cache Sources
- **In-Memory**: High-performance local caching with LRU eviction
- **Distributed**: Ready for Redis/Hazelcast integration
- **Features**: TTL support, pattern matching, statistics collection

## Configuration

### Environment Variables

Use environment variables for sensitive data:

```yaml
connection:
  username: "app_user"
  password: "${DB_PASSWORD}"  # Resolved from environment
  apiKey: "${API_KEY}"
```

### Environment-Specific Overrides

```yaml
environments:
  development:
    dataSources:
      - name: "user-database"
        connection:
          host: "localhost"
        cache:
          ttlSeconds: 60
  
  production:
    dataSources:
      - name: "user-database"
        connection:
          host: "prod-db.example.com"
          maxPoolSize: 50
        cache:
          ttlSeconds: 600
          maxSize: 5000
```

### Health Checks

```yaml
healthCheck:
  enabled: true
  intervalSeconds: 30
  timeoutSeconds: 5
  failureThreshold: 3
  query: "SELECT 1"  # Custom health check query
```

### Caching Configuration

```yaml
cache:
  enabled: true
  ttlSeconds: 300      # 5 minutes
  maxSize: 1000        # Maximum entries
  keyPrefix: "myapp"   # Cache key prefix
  evictionPolicy: "LRU" # Eviction strategy
```

## Usage Examples

### Database Operations

```java
// Simple query
List<Object> users = dataSource.query("getAllUsers", Collections.emptyMap());

// Parameterized query
Map<String, Object> params = Map.of("id", 123, "status", "active");
Object user = dataSource.queryForObject("getUserById", params);

// Batch operations
List<String> updates = List.of(
    "UPDATE users SET last_login = NOW() WHERE id = 1",
    "UPDATE users SET last_login = NOW() WHERE id = 2"
);
dataSource.batchUpdate(updates);
```

### REST API Operations

```java
// GET request
Map<String, Object> params = Map.of("userId", 123);
Object userProfile = apiSource.queryForObject("getUserProfile", params);

// POST request (configured in YAML)
Map<String, Object> newUser = Map.of("name", "John", "email", "john@example.com");
Object result = apiSource.query("createUser", newUser);
```

### File System Operations

```java
// Read CSV file
Object csvData = fileSource.getData("csv", "users.csv");

// Read JSON file with parameters
Map<String, Object> params = Map.of("filename", "config.json");
Object config = fileSource.queryForObject("getConfig", params);
```

### Cache Operations

```java
// Store in cache
Map<String, Object> params = Map.of("key", "user:123", "value", userData);
cacheSource.query("put", params);

// Retrieve from cache
Map<String, Object> getParams = Map.of("key", "user:123");
Object cachedData = cacheSource.queryForObject("get", getParams);
```

## Architecture

### Core Components

1. **DataSourceRegistry**: Centralized registry for all data sources
2. **DataSourceFactory**: Creates and configures data source instances
3. **DataSourceManager**: Coordinates multiple data sources with load balancing
4. **DataSourceConfigurationService**: High-level service for configuration management

### Data Flow

```
YAML Config → ConfigurationService → Manager → Registry → DataSource → External System
```

### Thread Safety

All components are designed for concurrent access:
- Thread-safe data structures (ConcurrentHashMap, etc.)
- Proper synchronization for shared resources
- Connection pooling for database sources
- Immutable configuration objects

## Best Practices

### Configuration Management

1. **Use Environment Variables**: Never hardcode sensitive data
2. **Environment-Specific Configs**: Use environment overrides
3. **Validation**: Always validate configurations before deployment
4. **Documentation**: Document all custom queries and endpoints

### Performance Optimization

1. **Enable Caching**: Use appropriate TTL values for your use case
2. **Connection Pooling**: Configure pool sizes based on load
3. **Query Optimization**: Use efficient queries and indexes
4. **Batch Operations**: Group multiple operations when possible

### Error Handling

1. **Circuit Breakers**: Configure for external APIs
2. **Retry Logic**: Use exponential backoff for transient failures
3. **Health Checks**: Monitor data source health continuously
4. **Graceful Degradation**: Handle failures gracefully

### Security

1. **SSL/TLS**: Always use encrypted connections in production
2. **Authentication**: Use strong authentication methods
3. **Access Control**: Limit database permissions to minimum required
4. **Audit Logging**: Log all data access for compliance

### Monitoring

1. **Metrics Collection**: Enable comprehensive metrics
2. **Health Monitoring**: Set up alerts for health check failures
3. **Performance Tracking**: Monitor response times and throughput
4. **Capacity Planning**: Track resource usage trends

## Troubleshooting

### Common Issues

#### Connection Failures
```
Error: Failed to connect to database
Solution: Check connection parameters, network connectivity, and credentials
```

#### Cache Misses
```
Issue: Low cache hit ratio
Solution: Increase TTL, review cache key patterns, check cache size limits
```

#### Circuit Breaker Trips
```
Issue: Circuit breaker preventing API calls
Solution: Check API health, review failure thresholds, verify network connectivity
```

### Debugging

Enable debug logging:
```yaml
logging:
  level:
    dev.mars.rulesengine.core.service.data.external: DEBUG
```

Check health status:
```java
ConnectionStatus status = dataSource.getConnectionStatus();
System.out.println("Status: " + status.getState());
System.out.println("Message: " + status.getMessage());
```

Review metrics:
```java
DataSourceMetrics metrics = dataSource.getMetrics();
System.out.println("Success rate: " + metrics.getSuccessRate());
System.out.println("Avg response time: " + metrics.getAverageResponseTime());
```

### Performance Tuning

1. **Database Connection Pools**:
   ```yaml
   connection:
     maxPoolSize: 20        # Adjust based on load
     minPoolSize: 5         # Keep minimum connections
     connectionTimeout: 30000
     idleTimeout: 600000
   ```

2. **API Circuit Breakers**:
   ```yaml
   circuitBreaker:
     failureThreshold: 5    # Number of failures before opening
     recoveryTimeout: 30000 # Time before attempting recovery
     halfOpenMaxCalls: 3    # Test calls in half-open state
   ```

3. **Cache Optimization**:
   ```yaml
   cache:
     maxSize: 10000         # Increase for better hit rates
     ttlSeconds: 600        # Balance freshness vs performance
     evictionPolicy: "LRU"  # Use appropriate eviction strategy
   ```

## Architecture

### Core Components

The external data source integration is built around several key components that work together to provide a robust and scalable data access layer:

1. **DataSourceRegistry**: Centralized registry for all data sources with health monitoring
2. **DataSourceFactory**: Creates and configures data source instances with resource caching
3. **DataSourceManager**: Coordinates multiple data sources with load balancing and failover
4. **DataSourceConfigurationService**: High-level service for configuration management

### Core Interfaces

#### ExternalDataSource

The main interface for all data source implementations:

```java
public interface ExternalDataSource extends DataSource {
    // Basic properties
    String getName();
    DataSourceType getSourceType();
    String getDataType();
    DataSourceConfiguration getConfiguration();

    // Lifecycle management
    void initialize(DataSourceConfiguration configuration) throws DataSourceException;
    void shutdown() throws DataSourceException;
    void refresh() throws DataSourceException;

    // Data operations
    Object getData(String queryName, Object... parameters) throws DataSourceException;
    List<Object> query(String query, Map<String, Object> parameters) throws DataSourceException;
    Object queryForObject(String query, Map<String, Object> parameters) throws DataSourceException;
    void batchUpdate(List<String> updates) throws DataSourceException;

    // Health and monitoring
    boolean isHealthy();
    boolean testConnection();
    ConnectionStatus getConnectionStatus();
    DataSourceMetrics getMetrics();

    // Capabilities
    boolean supportsDataType(String dataType);
    String[] getParameterNames();
}
```

#### DataSourceType Enumeration

Supported data source types:

```java
public enum DataSourceType {
    DATABASE("database", "Database", "Relational database systems"),
    REST_API("rest-api", "REST API", "HTTP REST API endpoints"),
    MESSAGE_QUEUE("message-queue", "Message Queue", "Message queue systems"),
    FILE_SYSTEM("file-system", "File System", "File-based data sources"),
    CACHE("cache", "Cache", "In-memory cache systems"),
    CUSTOM("custom", "Custom", "Custom data source implementations");
}
```

### Implementation Classes

- **DatabaseDataSource** - Database connectivity with connection pooling (PostgreSQL, MySQL, Oracle, SQL Server, H2)
- **RestApiDataSource** - REST API integration with circuit breakers and authentication
- **FileSystemDataSource** - File processing with format-specific readers (CSV, JSON, XML, Fixed-width)
- **CacheDataSource** - In-memory caching with TTL and eviction policies

### Data Flow

```
YAML Config → ConfigurationService → Manager → Registry → DataSource → External System
```

### Thread Safety

All components are designed for concurrent access:
- Thread-safe data structures (ConcurrentHashMap, etc.)
- Proper synchronization for shared resources
- Connection pooling for database sources
- Immutable configuration objects

## Configuration

### Environment Variables

Use environment variables for sensitive data:

```yaml
connection:
  username: "app_user"
  password: "${DB_PASSWORD}"  # Resolved from environment
  apiKey: "${API_KEY}"
```

### Environment-Specific Overrides

```yaml
environments:
  development:
    dataSources:
      - name: "user-database"
        connection:
          host: "localhost"
        cache:
          ttlSeconds: 60

  production:
    dataSources:
      - name: "user-database"
        connection:
          host: "prod-db.example.com"
          maxPoolSize: 50
        cache:
          ttlSeconds: 600
          maxSize: 5000
```

### Configuration Classes

#### DataSourceConfiguration

Main configuration class containing all settings:

```java
public class DataSourceConfiguration {
    private String name;
    private String type;
    private String sourceType;
    private String description;
    private boolean enabled = true;
    private String implementation;

    private ConnectionConfig connection;
    private CacheConfig cache;
    private HealthCheckConfig healthCheck;
    private AuthenticationConfig authentication;

    // Type-specific configurations
    private Map<String, String> queries;
    private Map<String, String> endpoints;
    private Map<String, String> topics;
    private Map<String, String> keyPatterns;
    private FileFormatConfig fileFormat;
    private CircuitBreakerConfig circuitBreaker;
    private ResponseMappingConfig responseMapping;

    // Custom properties for extensibility
    private Map<String, Object> customProperties;
}
```

#### ConnectionConfig

Connection-specific settings for different data source types:

```java
public class ConnectionConfig {
    // Database connection properties
    private String host;
    private Integer port;
    private String database;
    private String schema;
    private String username;
    private String password;
    private boolean sslEnabled = false;

    // HTTP/REST API connection properties
    private String baseUrl;
    private Integer timeout = 30000;
    private Integer retryAttempts = 3;
    private Integer retryDelay = 1000;
    private Map<String, String> headers;

    // Connection pooling configuration
    private ConnectionPoolConfig connectionPool;

    // Custom connection properties
    private Map<String, Object> customProperties;
}
```

#### CacheConfig

Caching configuration with multiple eviction policies:

```java
public class CacheConfig {
    public enum EvictionPolicy {
        LRU, LFU, FIFO, TTL_BASED, RANDOM
    }

    private Boolean enabled = true;
    private Long ttlSeconds = 3600L;
    private Long maxIdleSeconds = 1800L;
    private Integer maxSize = 10000;
    private EvictionPolicy evictionPolicy = EvictionPolicy.LRU;
    private Boolean preloadEnabled = false;
    private Boolean refreshAhead = false;
    private Long refreshAheadFactor = 75L;
    private Boolean statisticsEnabled = true;
    private String keyPrefix;
    private Boolean compressionEnabled = false;
    private String serializationFormat = "json";
}
```

#### HealthCheckConfig

Health monitoring configuration:

```java
public class HealthCheckConfig {
    private Boolean enabled = true;
    private Long intervalSeconds = 60L;
    private Long timeoutSeconds = 10L;
    private Integer retryAttempts = 3;
    private Long retryDelay = 1000L;
    private String query;
    private String endpoint;
    private String expectedResponse;
    private Integer failureThreshold = 3;
    private Integer successThreshold = 1;
    private Boolean logFailures = true;
    private Boolean alertOnFailure = false;
    private String alertEndpoint;

    // Circuit breaker integration
    private Boolean circuitBreakerIntegration = false;
    private Integer circuitBreakerFailureThreshold = 5;
    private Long circuitBreakerTimeoutSeconds = 60L;
}
```

#### AuthenticationConfig

Authentication configuration supporting multiple methods:

```java
public class AuthenticationConfig {
    public enum AuthenticationType {
        NONE, BASIC, BEARER_TOKEN, API_KEY, OAUTH2, CERTIFICATE, CUSTOM
    }

    private String type = "none";
    private String username;
    private String password;
    private String token;
    private String apiKey;
    private String apiKeyHeader = "X-API-Key";
    private String tokenHeader = "Authorization";
    private String tokenPrefix = "Bearer ";

    // OAuth2 configuration
    private String clientId;
    private String clientSecret;
    private String tokenUrl;
    private String scope;
    private String grantType = "client_credentials";

    // Certificate configuration
    private String certificatePath;
    private String certificatePassword;
    private String keyStorePath;
    private String keyStorePassword;

    // Token refresh configuration
    private Boolean autoRefresh = true;
    private Long refreshThresholdSeconds = 300L;
    private Integer maxRefreshAttempts = 3;
}
```

## Database Configuration

### Supported Databases

The database data source provides robust integration with relational databases, featuring connection pooling, query management, caching, health monitoring, and security.

#### PostgreSQL
```yaml
dataSources:
  - name: "postgres-db"
    type: "database"
    sourceType: "postgresql"
    connection:
      host: "localhost"
      port: 5432
      database: "myapp"
      username: "app_user"
      password: "${DB_PASSWORD}"
      schema: "public"
      sslEnabled: true
      sslMode: "require"
```

#### MySQL
```yaml
dataSources:
  - name: "mysql-db"
    type: "database"
    sourceType: "mysql"
    connection:
      host: "localhost"
      port: 3306
      database: "myapp"
      username: "app_user"
      password: "${DB_PASSWORD}"
      useSSL: true
      serverTimezone: "UTC"
      characterEncoding: "utf8mb4"
```

#### Oracle
```yaml
dataSources:
  - name: "oracle-db"
    type: "database"
    sourceType: "oracle"
    connection:
      host: "localhost"
      port: 1521
      serviceName: "ORCL"  # Use serviceName instead of database
      username: "app_user"
      password: "${DB_PASSWORD}"
      schema: "APP_SCHEMA"
```

#### SQL Server
```yaml
dataSources:
  - name: "sqlserver-db"
    type: "database"
    sourceType: "sqlserver"
    connection:
      host: "localhost"
      port: 1433
      database: "myapp"
      username: "app_user"
      password: "${DB_PASSWORD}"
      integratedSecurity: false
      encrypt: true
      trustServerCertificate: false
```

#### H2 Database
```yaml
dataSources:
  - name: "h2-db"
    type: "database"
    sourceType: "h2"
    connection:
      url: "jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1"
      username: "sa"
      password: ""
      mode: "PostgreSQL"  # Compatibility mode
      initScript: "classpath:schema.sql"
```

### Connection Configuration

#### Connection Parameters

| Parameter | Description | Default | Required |
|-----------|-------------|---------|----------|
| `host` | Database server hostname | localhost | Yes |
| `port` | Database server port | DB-specific | No |
| `database` | Database name | - | Yes* |
| `serviceName` | Oracle service name | - | Oracle only |
| `username` | Database username | - | Yes |
| `password` | Database password | - | Yes |
| `schema` | Default schema | - | No |
| `url` | Complete JDBC URL | Generated | No |

*For H2, you can use `url` instead of individual parameters.

#### SSL Configuration

```yaml
connection:
  sslEnabled: true
  sslMode: "require"           # PostgreSQL: disable, allow, prefer, require, verify-ca, verify-full
  sslCert: "/path/to/cert.pem" # Client certificate
  sslKey: "/path/to/key.pem"   # Client private key
  sslRootCert: "/path/to/ca.pem" # CA certificate
  sslPassword: "${SSL_PASSWORD}" # SSL key password
```

### Connection Pooling

#### HikariCP Configuration (Recommended)
```yaml
connection:
  # Pool sizing
  maxPoolSize: 20              # Maximum pool size
  minPoolSize: 5               # Minimum idle connections

  # Timeouts (milliseconds)
  connectionTimeout: 30000     # Max wait for connection
  idleTimeout: 600000          # Max idle time (10 minutes)
  maxLifetime: 1800000         # Max connection lifetime (30 minutes)

  # Validation
  validationTimeout: 5000      # Connection validation timeout
  leakDetectionThreshold: 60000 # Connection leak detection (1 minute)

  # Performance
  cachePrepStmts: true         # Enable prepared statement caching
  prepStmtCacheSize: 250       # Prepared statement cache size
  prepStmtCacheSqlLimit: 2048  # Max SQL length for caching
```

### Query Configuration

#### Named Queries
```yaml
queries:
  # Simple select
  getUserById: "SELECT * FROM users WHERE id = :id"

  # Complex query with joins
  getUserWithProfile: |
    SELECT u.id, u.username, u.email, p.first_name, p.last_name
    FROM users u
    LEFT JOIN profiles p ON u.id = p.user_id
    WHERE u.id = :id

  # Insert query
  createUser: |
    INSERT INTO users (username, email, status, created_at)
    VALUES (:username, :email, :status, NOW())
    RETURNING id

  # Update query
  updateUserEmail: |
    UPDATE users
    SET email = :email, updated_at = NOW()
    WHERE id = :id

  # Delete query
  deleteUser: "DELETE FROM users WHERE id = :id"

  # Batch query
  getUsersByIds: "SELECT * FROM users WHERE id IN (:ids)"

  # Health check query (required)
  default: "SELECT 1"
```

#### Parameter Binding
```yaml
parameterNames:
  - "id"
  - "username"
  - "email"
  - "status"
  - "ids"        # For IN clauses
  - "startDate"  # For date ranges
  - "endDate"
  - "limit"      # For pagination
  - "offset"
```

### Database-Specific Settings

#### PostgreSQL
```yaml
connection:
  applicationName: "MyApp"
  connectTimeout: 10
  socketTimeout: 0
  tcpKeepAlive: true
  logUnclosedConnections: true
```

#### MySQL
```yaml
connection:
  useSSL: true
  serverTimezone: "UTC"
  characterEncoding: "utf8mb4"
  useUnicode: true
  autoReconnect: true
  maxReconnects: 3
  initialTimeout: 2
```

#### Oracle
```yaml
connection:
  serviceName: "ORCL"
  connectionProperties:
    oracle.jdbc.ReadTimeout: "30000"
    oracle.net.CONNECT_TIMEOUT: "10000"
    oracle.jdbc.implicitStatementCacheSize: "25"
```

## API Reference

### Configuration Service

#### DataSourceConfigurationService

High-level service for managing data source configurations:

```java
public class DataSourceConfigurationService {
    // Singleton access
    public static DataSourceConfigurationService getInstance();

    // Lifecycle management
    public void initialize(YamlRuleConfiguration yamlConfig) throws DataSourceException;
    public void shutdown();
    public boolean isInitialized();
    public boolean isRunning();

    // Configuration management
    public void reloadFromYaml(YamlRuleConfiguration yamlConfig) throws DataSourceException;
    public DataSourceConfiguration getConfiguration(String name);
    public Set<String> getConfigurationNames();

    // Data source access
    public ExternalDataSource getDataSource(String name);
    public DataSourceManager getDataSourceManager();

    // Event handling
    public void addListener(DataSourceConfigurationListener listener);
    public void removeListener(DataSourceConfigurationListener listener);
}
```

#### Usage Example

```java
// Initialize service
DataSourceConfigurationService service = DataSourceConfigurationService.getInstance();
YamlRuleConfiguration yamlConfig = loadConfiguration("data-sources.yaml");
service.initialize(yamlConfig);

// Access data sources
ExternalDataSource userDb = service.getDataSource("user-database");
ExternalDataSource apiSource = service.getDataSource("external-api");

// Get configuration details
DataSourceConfiguration config = service.getConfiguration("user-database");
System.out.println("Data source type: " + config.getDataSourceType());

// Reload configuration
YamlRuleConfiguration newConfig = loadConfiguration("updated-config.yaml");
service.reloadFromYaml(newConfig);
```

### Data Source Manager

#### DataSourceManager

Coordinates multiple data sources with load balancing and failover:

```java
public class DataSourceManager {
    // Constructors
    public DataSourceManager();
    public DataSourceManager(DataSourceRegistry registry, DataSourceFactory factory);

    // Lifecycle management
    public void initialize(List<DataSourceConfiguration> configurations) throws DataSourceException;
    public void shutdown();
    public boolean isInitialized();
    public boolean isRunning();

    // Data source management
    public void addDataSource(DataSourceConfiguration configuration) throws DataSourceException;
    public boolean removeDataSource(String name);
    public ExternalDataSource getDataSource(String name);
    public Set<String> getDataSourceNames();

    // Type-based access
    public List<ExternalDataSource> getDataSourcesByType(DataSourceType type);
    public ExternalDataSource getDataSourceWithLoadBalancing(DataSourceType type);
    public List<ExternalDataSource> getHealthyDataSourcesByType(DataSourceType type);

    // Health monitoring
    public List<ExternalDataSource> getHealthyDataSources();
    public List<ExternalDataSource> getUnhealthyDataSources();
    public void refreshAll();

    // Advanced operations
    public List<Object> queryWithFailover(DataSourceType type, String query, Map<String, Object> parameters) throws DataSourceException;
    public CompletableFuture<List<Object>> queryAsync(String dataSourceName, String query, Map<String, Object> parameters);

    // Statistics and monitoring
    public DataSourceManagerStatistics getStatistics();

    // Event handling
    public void addListener(DataSourceManagerListener listener);
    public void removeListener(DataSourceManagerListener listener);
}
```

### Data Source Registry

#### DataSourceRegistry

Centralized registry for all data sources:

```java
public class DataSourceRegistry {
    // Singleton access
    public static DataSourceRegistry getInstance();

    // Registration management
    public void register(ExternalDataSource dataSource) throws DataSourceException;
    public boolean unregister(String name);
    public boolean isRegistered(String name);
    public int size();

    // Data source access
    public ExternalDataSource getDataSource(String name);
    public Set<String> getDataSourceNames();

    // Type-based queries
    public List<ExternalDataSource> getDataSourcesByType(DataSourceType type);
    public List<ExternalDataSource> getDataSourcesByTag(String tag);

    // Health monitoring
    public List<ExternalDataSource> getHealthyDataSources();
    public List<ExternalDataSource> getUnhealthyDataSources();
    public void refreshAll();

    // Statistics
    public RegistryStatistics getStatistics();

    // Event handling
    public void addListener(DataSourceRegistryListener listener);
    public void removeListener(DataSourceRegistryListener listener);

    // Lifecycle
    public void shutdown();
}
```

### Data Source Factory

#### DataSourceFactory

Creates and configures data source instances:

```java
public class DataSourceFactory {
    // Singleton access
    public static DataSourceFactory getInstance();

    // Data source creation
    public ExternalDataSource createDataSource(DataSourceConfiguration configuration) throws DataSourceException;
    public Map<String, ExternalDataSource> createDataSources(List<DataSourceConfiguration> configurations) throws DataSourceException;

    // Custom provider management
    public void registerProvider(String type, DataSourceProvider provider);
    public void unregisterProvider(String type);

    // Type support queries
    public boolean isTypeSupported(DataSourceType type);
    public boolean isCustomTypeSupported(String type);
    public Set<String> getSupportedTypes();

    // Resource management
    public void clearCache();
    public void shutdown();
}
```

### Data Source Implementations

#### DatabaseDataSource

Database-specific implementation:

```java
public class DatabaseDataSource implements ExternalDataSource {
    // Constructor
    public DatabaseDataSource(DataSource dataSource, DataSourceConfiguration configuration);

    // Database-specific methods
    public DataSource getDataSource();
    public JdbcTemplate getJdbcTemplate();

    // Query execution
    public List<Map<String, Object>> queryForList(String sql, Map<String, Object> parameters);
    public Map<String, Object> queryForMap(String sql, Map<String, Object> parameters);
    public <T> T queryForObject(String sql, Map<String, Object> parameters, Class<T> requiredType);

    // Batch operations
    public int[] batchUpdate(String sql, List<Map<String, Object>> batchParameters);
}
```

#### RestApiDataSource

REST API-specific implementation:

```java
public class RestApiDataSource implements ExternalDataSource {
    // Constructor
    public RestApiDataSource(HttpClient httpClient, DataSourceConfiguration configuration);

    // HTTP-specific methods
    public HttpClient getHttpClient();
    public HttpResponse<String> executeRequest(HttpRequest request) throws IOException, InterruptedException;

    // Request building
    public HttpRequest buildGetRequest(String endpoint, Map<String, Object> parameters);
    public HttpRequest buildPostRequest(String endpoint, Object body);
    public HttpRequest buildPutRequest(String endpoint, Object body);
    public HttpRequest buildDeleteRequest(String endpoint);
}
```

#### FileSystemDataSource

File system-specific implementation:

```java
public class FileSystemDataSource implements ExternalDataSource {
    // Constructor
    public FileSystemDataSource(DataSourceConfiguration configuration);

    // File operations
    public List<Path> listFiles(String pattern);
    public Object readFile(Path filePath);
    public Object readFile(String filename);

    // Format-specific readers
    public List<Map<String, Object>> readCsvFile(Path filePath);
    public Object readJsonFile(Path filePath);
    public List<Map<String, Object>> readXmlFile(Path filePath);
    public List<Map<String, Object>> readFixedWidthFile(Path filePath);
}
```

#### CacheDataSource

Cache-specific implementation:

```java
public class CacheDataSource implements ExternalDataSource {
    // Constructor
    public CacheDataSource(DataSourceConfiguration configuration);

    // Cache operations
    public void put(String key, Object value);
    public void put(String key, Object value, long ttlSeconds);
    public Object get(String key);
    public boolean containsKey(String key);
    public void remove(String key);
    public void clear();

    // Pattern operations
    public Set<String> getKeys(String pattern);
    public Map<String, Object> getAll(Set<String> keys);

    // Statistics
    public long size();
    public CacheStatistics getCacheStatistics();
}
```

### Exception Handling

#### DataSourceException

Main exception class for data source operations:

```java
public class DataSourceException extends Exception {
    public enum ErrorType {
        CONNECTION_ERROR("Connection failed"),
        CONFIGURATION_ERROR("Configuration error"),
        EXECUTION_ERROR("Execution failed"),
        DATA_FORMAT_ERROR("Data format error"),
        TIMEOUT_ERROR("Operation timed out"),
        AUTHENTICATION_ERROR("Authentication failed"),
        VALIDATION_ERROR("Validation failed"),
        RESOURCE_ERROR("Resource error"),
        CIRCUIT_BREAKER_OPEN("Circuit breaker is open"),
        HEALTH_CHECK_FAILED("Health check failed");

        private final String defaultMessage;

        ErrorType(String defaultMessage) {
            this.defaultMessage = defaultMessage;
        }

        public String getDefaultMessage() {
            return defaultMessage;
        }
    }

    // Constructors
    public DataSourceException(ErrorType errorType, String message);
    public DataSourceException(ErrorType errorType, String message, Throwable cause);

    // Properties
    public ErrorType getErrorType();
    public String getDataSourceName();
    public long getTimestamp();
}
```

#### Exception Handling Example

```java
try {
    ExternalDataSource dataSource = factory.createDataSource(config);
    List<Object> results = dataSource.query("getUserById", parameters);
} catch (DataSourceException e) {
    switch (e.getErrorType()) {
        case CONNECTION_ERROR:
            logger.error("Connection failed for data source: " + e.getDataSourceName(), e);
            // Implement retry logic
            break;
        case AUTHENTICATION_ERROR:
            logger.error("Authentication failed: " + e.getMessage(), e);
            // Check credentials
            break;
        case EXECUTION_ERROR:
            logger.error("Query execution failed: " + e.getMessage(), e);
            // Check query syntax
            break;
        default:
            logger.error("Unexpected error: " + e.getMessage(), e);
    }
}
```

### Metrics and Monitoring

#### DataSourceMetrics

Metrics collection for data sources:

```java
public class DataSourceMetrics {
    // Request metrics
    public long getSuccessfulRequests();
    public long getFailedRequests();
    public long getTotalRequests();
    public double getSuccessRate();

    // Timing metrics
    public double getAverageResponseTime();
    public long getMinResponseTime();
    public long getMaxResponseTime();

    // Cache metrics
    public long getCacheHits();
    public long getCacheMisses();
    public double getCacheHitRatio();

    // Connection metrics
    public int getActiveConnections();
    public int getIdleConnections();
    public int getTotalConnections();

    // Error metrics
    public Map<String, Long> getErrorCounts();
    public long getTimeoutCount();

    // Data volume metrics
    public long getBytesRead();
    public long getBytesWritten();
    public long getRecordsProcessed();

    // Lifecycle
    public LocalDateTime getCreatedAt();
    public LocalDateTime getLastResetTime();
    public void reset();
}
```

#### ConnectionStatus

Health and connection status information:

```java
public class ConnectionStatus {
    public enum State {
        NOT_INITIALIZED("Not initialized"),
        CONNECTING("Connecting"),
        CONNECTED("Connected"),
        DISCONNECTED("Disconnected"),
        ERROR("Error"),
        SHUTDOWN("Shutdown");

        private final String description;

        State(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    // Static factory methods
    public static ConnectionStatus notInitialized();
    public static ConnectionStatus connecting();
    public static ConnectionStatus connected(String message);
    public static ConnectionStatus disconnected(String message);
    public static ConnectionStatus error(String message, Throwable error);
    public static ConnectionStatus shutdown();

    // Properties
    public State getState();
    public LocalDateTime getLastUpdated();
    public LocalDateTime getLastConnected();
    public String getMessage();
    public Throwable getError();
    public long getConnectionAttempts();
    public long getSuccessfulConnections();
}
```

#### RegistryStatistics

Statistics for the data source registry:

```java
public class RegistryStatistics {
    // Basic counts
    public int getTotalDataSources();
    public int getHealthyDataSources();
    public int getUnhealthyDataSources();
    public double getHealthPercentage();

    // Type distribution
    public Map<DataSourceType, Integer> getCountByType();
    public Map<String, Integer> getCountByTag();

    // Health status
    public boolean isAllHealthy();
    public List<String> getUnhealthyDataSourceNames();

    // Summary
    public String getSummary();
    public LocalDateTime getLastUpdated();
}
```

#### Monitoring Example

```java
// Collect metrics
DataSourceMetrics metrics = dataSource.getMetrics();
RegistryStatistics registryStats = registry.getStatistics();

// Log performance metrics
logger.info("Data source performance:");
logger.info("  Success rate: {}%", metrics.getSuccessRate() * 100);
logger.info("  Average response time: {}ms", metrics.getAverageResponseTime());
logger.info("  Cache hit ratio: {}%", metrics.getCacheHitRatio() * 100);

// Monitor registry health
logger.info("Registry health: {}% ({}/{} healthy)",
    registryStats.getHealthPercentage(),
    registryStats.getHealthyDataSources(),
    registryStats.getTotalDataSources());

// Alert on issues
if (registryStats.getHealthPercentage() < 90.0) {
    alertService.sendAlert("Data source health below 90%: " + registryStats.getSummary());
}
```

## Usage Examples

### Database Operations

```java
// Simple query
List<Object> users = dataSource.query("getAllUsers", Collections.emptyMap());

// Parameterized query
Map<String, Object> params = Map.of("id", 123, "status", "active");
Object user = dataSource.queryForObject("getUserById", params);

// Batch operations
List<String> updates = List.of(
    "UPDATE users SET last_login = NOW() WHERE id = 1",
    "UPDATE users SET last_login = NOW() WHERE id = 2"
);
dataSource.batchUpdate(updates);

// Query usage in Java with complex parameters
Map<String, Object> complexParams = Map.of(
    "username", "john_doe",
    "email", "john@example.com",
    "status", "ACTIVE"
);
List<Object> results = dataSource.query("createUser", complexParams);

// Array parameters for IN clauses
Map<String, Object> arrayParams = Map.of("ids", List.of(1, 2, 3, 4, 5));
List<Object> users = dataSource.query("getUsersByIds", arrayParams);
```

### REST API Operations

```java
// GET request
Map<String, Object> params = Map.of("userId", 123);
Object userProfile = apiSource.queryForObject("getUserProfile", params);

// POST request (configured in YAML)
Map<String, Object> newUser = Map.of("name", "John", "email", "john@example.com");
Object result = apiSource.query("createUser", newUser);

// Custom headers and authentication
Map<String, Object> requestParams = Map.of(
    "endpoint", "users/123",
    "headers", Map.of("Accept", "application/json"),
    "timeout", 30000
);
Object response = apiSource.queryForObject("customRequest", requestParams);
```

### File System Operations

```java
// Read CSV file
Object csvData = fileSource.getData("csv", "users.csv");

// Read JSON file with parameters
Map<String, Object> params = Map.of("filename", "config.json");
Object config = fileSource.queryForObject("getConfig", params);

// Read XML file with XPath
Map<String, Object> xmlParams = Map.of(
    "filename", "data.xml",
    "xpath", "//user[@status='active']"
);
List<Object> activeUsers = fileSource.query("getActiveUsers", xmlParams);

// Watch for file changes
fileSource.getData("watch", "*.json");
```

### Cache Operations

```java
// Store in cache
Map<String, Object> params = Map.of("key", "user:123", "value", userData);
cacheSource.query("put", params);

// Retrieve from cache
Map<String, Object> getParams = Map.of("key", "user:123");
Object cachedData = cacheSource.queryForObject("get", getParams);

// Pattern-based operations
Map<String, Object> patternParams = Map.of("pattern", "user:*");
List<Object> userKeys = cacheSource.query("keys", patternParams);

// Batch cache operations
Map<String, Object> batchParams = Map.of(
    "keys", List.of("user:1", "user:2", "user:3")
);
Map<String, Object> batchResults = (Map<String, Object>) cacheSource.queryForObject("getAll", batchParams);
```

### Complete Integration Example

```java
public class DataSourceIntegrationExample {

    public void demonstrateIntegration() throws DataSourceException {
        // Initialize configuration service
        DataSourceConfigurationService configService = DataSourceConfigurationService.getInstance();
        YamlRuleConfiguration yamlConfig = loadYamlConfiguration("data-sources.yaml");
        configService.initialize(yamlConfig);

        // Get data sources
        ExternalDataSource userDb = configService.getDataSource("user-database");
        ExternalDataSource userCache = configService.getDataSource("user-cache");
        ExternalDataSource userApi = configService.getDataSource("user-api");

        // Implement caching strategy
        String userId = "123";
        Object userData = getUserWithCaching(userDb, userCache, userId);

        // Sync with external API
        syncUserWithExternalSystem(userApi, userData);

        // Monitor health
        monitorDataSourceHealth(configService);
    }

    private Object getUserWithCaching(ExternalDataSource db, ExternalDataSource cache, String userId)
            throws DataSourceException {

        // Try cache first
        Map<String, Object> cacheParams = Map.of("key", "user:" + userId);
        try {
            Object cachedUser = cache.queryForObject("get", cacheParams);
            if (cachedUser != null) {
                return cachedUser;
            }
        } catch (DataSourceException e) {
            // Cache miss or error, continue to database
        }

        // Fetch from database
        Map<String, Object> dbParams = Map.of("id", userId);
        Object user = db.queryForObject("getUserById", dbParams);

        // Store in cache
        if (user != null) {
            Map<String, Object> putParams = Map.of("key", "user:" + userId, "value", user);
            cache.query("put", putParams);
        }

        return user;
    }

    private void syncUserWithExternalSystem(ExternalDataSource api, Object userData)
            throws DataSourceException {

        Map<String, Object> syncParams = Map.of("userData", userData);
        try {
            api.query("syncUser", syncParams);
        } catch (DataSourceException e) {
            if (e.getErrorType() == DataSourceException.ErrorType.CIRCUIT_BREAKER_OPEN) {
                // Handle circuit breaker open state
                logger.warn("External API circuit breaker is open, skipping sync");
            } else {
                throw e;
            }
        }
    }

    private void monitorDataSourceHealth(DataSourceConfigurationService configService) {
        DataSourceManager manager = configService.getDataSourceManager();
        RegistryStatistics stats = manager.getRegistry().getStatistics();

        logger.info("Data source health summary: {}", stats.getSummary());

        if (!stats.isAllHealthy()) {
            List<String> unhealthy = stats.getUnhealthyDataSourceNames();
            logger.warn("Unhealthy data sources: {}", unhealthy);
        }
    }
}
```

## Best Practices

### Configuration Best Practices

#### 1. Environment-Specific Configuration

**✅ DO**: Use environment-specific overrides
```yaml
environments:
  development:
    dataSources:
      - name: "user-database"
        connection:
          host: "localhost"
          maxPoolSize: 5
        cache:
          ttlSeconds: 60

  production:
    dataSources:
      - name: "user-database"
        connection:
          host: "prod-db.example.com"
          maxPoolSize: 50
        cache:
          ttlSeconds: 600
```

**❌ DON'T**: Hardcode environment-specific values in base configuration

#### 2. Credential Management

**✅ DO**: Use environment variables for sensitive data
```yaml
connection:
  username: "app_user"
  password: "${DB_PASSWORD}"
  apiKey: "${API_KEY}"
```

**❌ DON'T**: Store credentials in configuration files
```yaml
# BAD - Never do this
connection:
  password: "hardcoded_password"
```

#### 3. Configuration Validation

**✅ DO**: Validate configurations before deployment
```java
public void validateConfiguration(DataSourceConfiguration config) {
    if (config.getName() == null || config.getName().trim().isEmpty()) {
        throw new IllegalArgumentException("Data source name is required");
    }

    if (config.getConnection() == null) {
        throw new IllegalArgumentException("Connection configuration is required");
    }

    // Validate connection parameters
    ConnectionConfig conn = config.getConnection();
    if (conn.getHost() == null && conn.getBaseUrl() == null) {
        throw new IllegalArgumentException("Either host or URL must be specified");
    }
}
```

#### 4. Naming Conventions

**✅ DO**: Use consistent, descriptive names
```yaml
dataSources:
  - name: "user-database-primary"      # Clear and specific
  - name: "customer-api-external"      # Indicates purpose and type
  - name: "config-files-local"         # Descriptive of content and location
```

**❌ DON'T**: Use generic or ambiguous names
```yaml
dataSources:
  - name: "db1"                        # Too generic
  - name: "api"                        # Ambiguous
  - name: "files"                      # Not specific enough
```

#### 5. Documentation and Tags

**✅ DO**: Document data sources with descriptions and tags
```yaml
dataSources:
  - name: "user-database"
    description: "Primary user database containing authentication and profile data"
    tags:
      - "production"
      - "primary"
      - "users"
      - "authentication"
```

### Health Monitoring Configuration

**✅ DO**: Configure comprehensive health checks
```yaml
healthCheck:
  enabled: true
  intervalSeconds: 30
  timeoutSeconds: 5
  failureThreshold: 3
  recoveryThreshold: 2
  query: "SELECT 1"
```

**✅ DO**: Monitor health status programmatically
```java
// Monitor connection pool usage
DataSourceMetrics metrics = dataSource.getMetrics();
int activeConnections = metrics.getActiveConnections();
int totalConnections = metrics.getTotalConnections();
double utilization = (double) activeConnections / totalConnections;

if (utilization > 0.8) {
    logger.warn("High connection pool utilization: {}%", utilization * 100);
}
```

### Caching Configuration

**✅ DO**: Use appropriate TTL values
```yaml
cache:
  # For frequently changing data
  ttlSeconds: 60              # 1 minute

  # For stable reference data
  ttlSeconds: 3600            # 1 hour

  # For configuration data
  ttlSeconds: 86400           # 24 hours
```

**✅ DO**: Size caches appropriately
```yaml
cache:
  # Consider memory usage vs hit ratio
  maxSize: 10000              # Balance memory and performance
  evictionPolicy: "LRU"       # Use appropriate eviction strategy
```

### Cache Usage Patterns

Cache keys are generated as: `{keyPrefix}:{queryName}:{parameterHash}`

Example: `myapp:getUserById:a1b2c3d4`

```java
// First call - hits database, stores in cache
Object user1 = dataSource.queryForObject("getUserById", Map.of("id", 123));

// Second call - hits cache (if within TTL)
Object user2 = dataSource.queryForObject("getUserById", Map.of("id", 123));

// Different parameters - new cache entry
Object user3 = dataSource.queryForObject("getUserById", Map.of("id", 456));
```

## Performance Optimization

### Connection Pooling

**✅ DO**: Configure appropriate pool sizes
```yaml
connection:
  # For high-throughput applications
  maxPoolSize: 50
  minPoolSize: 10

  # For low-latency requirements
  connectionTimeout: 5000
  idleTimeout: 300000
```

**✅ DO**: Monitor pool utilization
```java
// Monitor connection pool metrics
DataSourceMetrics metrics = dataSource.getMetrics();
int activeConnections = metrics.getActiveConnections();
int totalConnections = metrics.getTotalConnections();
double utilization = (double) activeConnections / totalConnections;

if (utilization > 0.8) {
    logger.warn("High connection pool utilization: {}%", utilization * 100);
}
```

### Query Optimization

**✅ DO**: Use efficient queries
```yaml
queries:
  # Use indexes effectively
  getUserByEmail: "SELECT id, username, email FROM users WHERE email = :email"

  # Limit result sets
  getRecentUsers: "SELECT * FROM users ORDER BY created_at DESC LIMIT :limit"

  # Use specific columns
  getUserSummary: "SELECT id, username FROM users WHERE id = :id"
```

**❌ DON'T**: Use inefficient queries
```yaml
queries:
  # Avoid SELECT *
  getAllUserData: "SELECT * FROM users"

  # Avoid unindexed searches
  findUserByName: "SELECT * FROM users WHERE LOWER(name) LIKE '%:name%'"
```

### Batch Operations

**✅ DO**: Use batch operations for multiple updates
```java
// Batch database updates
List<String> updates = Arrays.asList(
    "UPDATE users SET last_login = NOW() WHERE id = 1",
    "UPDATE users SET last_login = NOW() WHERE id = 2",
    "UPDATE users SET last_login = NOW() WHERE id = 3"
);
dataSource.batchUpdate(updates);
```

### Connection Pool Tuning

```yaml
connection:
  # For high-load applications
  maxPoolSize: 50
  minPoolSize: 10
  connectionTimeout: 10000

  # For low-latency requirements
  maxPoolSize: 20
  minPoolSize: 10
  idleTimeout: 300000          # 5 minutes
  maxLifetime: 900000          # 15 minutes
```

### Cache Optimization

```yaml
cache:
  # For frequently accessed data
  ttlSeconds: 600              # 10 minutes
  maxSize: 5000

  # For rarely changing data
  ttlSeconds: 3600             # 1 hour
  maxSize: 10000
```

### Performance Monitoring

**✅ DO**: Collect comprehensive metrics
```java
// Monitor key performance indicators
DataSourceMetrics metrics = dataSource.getMetrics();

// Response time metrics
double avgResponseTime = metrics.getAverageResponseTime();
long maxResponseTime = metrics.getMaxResponseTime();

// Success rate metrics
double successRate = metrics.getSuccessRate();
long failedRequests = metrics.getFailedRequests();

// Cache metrics
double cacheHitRatio = metrics.getCacheHitRatio();
long cacheHits = metrics.getCacheHits();

// Log or send to monitoring system
monitoringService.recordMetric("datasource.response_time.avg", avgResponseTime);
monitoringService.recordMetric("datasource.success_rate", successRate);
monitoringService.recordMetric("datasource.cache_hit_ratio", cacheHitRatio);
```

### Performance Tuning Guidelines

1. **Database Connection Pools**:
   ```yaml
   connection:
     maxPoolSize: 20        # Adjust based on load
     minPoolSize: 5         # Keep minimum connections
     connectionTimeout: 30000
     idleTimeout: 600000
   ```

2. **API Circuit Breakers**:
   ```yaml
   circuitBreaker:
     failureThreshold: 5    # Number of failures before opening
     recoveryTimeout: 30000 # Time before attempting recovery
     halfOpenMaxCalls: 3    # Test calls in half-open state
   ```

3. **Cache Optimization**:
   ```yaml
   cache:
     maxSize: 10000         # Increase for better hit rates
     ttlSeconds: 600        # Balance freshness vs performance
     evictionPolicy: "LRU"  # Use appropriate eviction strategy
   ```

## Security Guidelines

### Authentication and Authorization

**✅ DO**: Use strong authentication methods
```yaml
authentication:
  type: "oauth2"
  clientId: "${CLIENT_ID}"
  clientSecret: "${CLIENT_SECRET}"
  tokenUrl: "https://auth.example.com/oauth/token"
  scope: "read:data"
```

**✅ DO**: Implement proper access controls
```sql
-- Grant minimum required permissions
GRANT SELECT, INSERT, UPDATE ON users TO app_user;
-- Don't grant unnecessary permissions like DROP, CREATE, etc.
```

### Encryption and SSL

**✅ DO**: Always use SSL/TLS in production
```yaml
connection:
  sslEnabled: true
  sslMode: "require"
  sslVerifyServerCertificate: true
```

**✅ DO**: Encrypt sensitive configuration data
```yaml
connection:
  password: "ENC(AES256:encrypted_password_here)"
```

### Network Security

**✅ DO**: Use network security controls
```yaml
connection:
  # Use private network addresses
  host: "10.0.1.100"

  # Configure appropriate timeouts
  connectionTimeout: 10000
  readTimeout: 30000
```

### Credential Management

```yaml
connection:
  username: "app_user"
  password: "${DB_PASSWORD}"    # Environment variable

  # Or use encrypted passwords
  password: "ENC(encrypted_password_here)"
```

### SSL/TLS Configuration

```yaml
connection:
  sslEnabled: true
  sslMode: "require"

  # Certificate-based authentication
  sslCert: "/etc/ssl/certs/client-cert.pem"
  sslKey: "/etc/ssl/private/client-key.pem"
  sslRootCert: "/etc/ssl/certs/ca-cert.pem"

  # Verify server certificate
  sslVerifyServerCertificate: true
```

### Database Permissions

Grant minimum required permissions:
```sql
-- PostgreSQL example
CREATE USER app_user WITH PASSWORD 'secure_password';
GRANT CONNECT ON DATABASE myapp TO app_user;
GRANT USAGE ON SCHEMA public TO app_user;
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO app_user;
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO app_user;
```

### Security Best Practices

1. **SSL/TLS**: Always use encrypted connections in production
2. **Authentication**: Use strong authentication methods
3. **Access Control**: Limit database permissions to minimum required
4. **Audit Logging**: Log all data access for compliance
5. **Network Security**: Use private networks and appropriate firewall rules
6. **Credential Rotation**: Regularly rotate passwords and API keys
7. **Encryption**: Encrypt sensitive data at rest and in transit

## Error Handling and Resilience

### Circuit Breaker Pattern

**✅ DO**: Configure circuit breakers for external APIs
```yaml
circuitBreaker:
  enabled: true
  failureThreshold: 5         # Open after 5 failures
  recoveryTimeout: 30000      # Wait 30 seconds before retry
  halfOpenMaxCalls: 3         # Test with 3 calls in half-open state
```

### Retry Logic

**✅ DO**: Implement appropriate retry strategies
```yaml
connection:
  retryAttempts: 3
  retryDelay: 1000            # 1 second initial delay
  retryBackoffMultiplier: 2.0 # Exponential backoff
```

### Graceful Degradation

**✅ DO**: Handle failures gracefully
```java
public Object getUserData(String userId) {
    try {
        // Try primary data source
        return primaryDataSource.queryForObject("getUserById", Map.of("id", userId));
    } catch (DataSourceException e) {
        logger.warn("Primary data source failed, trying cache", e);

        try {
            // Fallback to cache
            return cacheDataSource.get("user:" + userId);
        } catch (DataSourceException cacheError) {
            logger.error("Both primary and cache failed", cacheError);

            // Return default/empty response
            return createDefaultUserResponse(userId);
        }
    }
}
```

### Failover Implementation

**✅ DO**: Implement automatic failover
```java
// Manager provides failover capabilities
DataSourceManager manager = configService.getDataSourceManager();

// Query with automatic failover
Map<String, Object> params = Map.of("id", 123);
List<Object> results = manager.queryWithFailover(DataSourceType.DATABASE, "getUserById", params);
```

### Circuit Breaker Configuration

```yaml
circuitBreaker:
  enabled: true
  failureThreshold: 5         # Number of failures before opening circuit
  timeoutSeconds: 60          # Time to wait before trying half-open
  successThreshold: 3         # Number of successes needed to close circuit
  requestVolumeThreshold: 10  # Minimum requests before evaluating failure rate
  failureRateThreshold: 50.0  # Failure rate percentage to open circuit
  fallbackResponse: "Service temporarily unavailable"
  logStateChanges: true
  metricsEnabled: true
```

### Error Handling Patterns

**✅ DO**: Use specific exception handling
```java
try {
    Object result = dataSource.queryForObject("getUserById", params);
    return result;
} catch (DataSourceException e) {
    switch (e.getErrorType()) {
        case CONNECTION_ERROR:
            // Retry with exponential backoff
            return retryWithBackoff(() -> dataSource.queryForObject("getUserById", params));

        case CIRCUIT_BREAKER_OPEN:
            // Use cached data or default response
            return getCachedUserOrDefault(userId);

        case TIMEOUT_ERROR:
            // Log and return partial data
            logger.warn("Query timeout for user {}", userId);
            return getPartialUserData(userId);

        case AUTHENTICATION_ERROR:
            // Refresh credentials and retry
            refreshCredentials();
            return dataSource.queryForObject("getUserById", params);

        default:
            // Log error and propagate
            logger.error("Unexpected data source error", e);
            throw e;
    }
}
```

### Health Check Integration

**✅ DO**: Integrate health checks with circuit breakers
```yaml
healthCheck:
  enabled: true
  intervalSeconds: 30
  timeoutSeconds: 5
  failureThreshold: 3
  recoveryThreshold: 2

  # Circuit breaker integration
  circuitBreakerIntegration: true
  circuitBreakerFailureThreshold: 5
  circuitBreakerTimeoutSeconds: 60
```

### Resilience Patterns

1. **Circuit Breaker**: Prevent cascading failures
2. **Retry with Backoff**: Handle transient failures
3. **Timeout**: Prevent hanging operations
4. **Bulkhead**: Isolate critical resources
5. **Fallback**: Provide alternative responses
6. **Health Checks**: Monitor system health

## Monitoring and Observability

### Metrics Collection

**✅ DO**: Collect comprehensive metrics
```java
// Monitor key performance indicators
DataSourceMetrics metrics = dataSource.getMetrics();

// Response time metrics
double avgResponseTime = metrics.getAverageResponseTime();
long maxResponseTime = metrics.getMaxResponseTime();

// Success rate metrics
double successRate = metrics.getSuccessRate();
long failedRequests = metrics.getFailedRequests();

// Cache metrics
double cacheHitRatio = metrics.getCacheHitRatio();
long cacheHits = metrics.getCacheHits();

// Log or send to monitoring system
monitoringService.recordMetric("datasource.response_time.avg", avgResponseTime);
monitoringService.recordMetric("datasource.success_rate", successRate);
monitoringService.recordMetric("datasource.cache_hit_ratio", cacheHitRatio);
```

### Alerting

**✅ DO**: Set up proactive alerts
```java
// Alert on high error rates
if (metrics.getSuccessRate() < 0.95) {
    alertService.sendAlert(AlertLevel.WARNING,
        "Data source success rate below 95%: " + metrics.getSuccessRate());
}

// Alert on slow response times
if (metrics.getAverageResponseTime() > 1000) {
    alertService.sendAlert(AlertLevel.WARNING,
        "Data source response time above 1 second: " + metrics.getAverageResponseTime() + "ms");
}

// Alert on health check failures
if (!dataSource.isHealthy()) {
    alertService.sendAlert(AlertLevel.CRITICAL,
        "Data source health check failed: " + dataSource.getName());
}
```

### Logging

**✅ DO**: Implement structured logging
```java
// Use structured logging with context
logger.info("Data source query executed",
    Map.of(
        "dataSource", dataSource.getName(),
        "query", queryName,
        "parameters", parameters,
        "responseTime", responseTime,
        "resultCount", results.size()
    ));

// Log errors with full context
logger.error("Data source query failed",
    Map.of(
        "dataSource", dataSource.getName(),
        "query", queryName,
        "parameters", parameters,
        "errorType", e.getErrorType(),
        "errorMessage", e.getMessage()
    ), e);
```

### Health Monitoring

**✅ DO**: Monitor data source health continuously
```java
// Check health status
ConnectionStatus status = dataSource.getConnectionStatus();
System.out.println("State: " + status.getState());
System.out.println("Healthy: " + dataSource.isHealthy());
System.out.println("Last Check: " + status.getLastCheckTime());

// Monitor registry health
RegistryStatistics registryStats = registry.getStatistics();
logger.info("Registry health: {}% ({}/{} healthy)",
    registryStats.getHealthPercentage(),
    registryStats.getHealthyDataSources(),
    registryStats.getTotalDataSources());
```

### Observability Best Practices

1. **Metrics Collection**: Enable comprehensive metrics
2. **Health Monitoring**: Set up alerts for health check failures
3. **Performance Tracking**: Monitor response times and throughput
4. **Capacity Planning**: Track resource usage trends
5. **Distributed Tracing**: Trace requests across data sources
6. **Log Aggregation**: Centralize logs for analysis

## Testing Strategies

### Unit Testing

**✅ DO**: Test data source configurations
```java
@Test
public void testDatabaseConfiguration() {
    DataSourceConfiguration config = createDatabaseConfig();

    // Validate configuration
    assertNotNull(config.getName());
    assertNotNull(config.getConnection());
    assertTrue(config.isEnabled());

    // Test data source creation
    ExternalDataSource dataSource = factory.createDataSource(config);
    assertNotNull(dataSource);
    assertEquals(DataSourceType.DATABASE, dataSource.getSourceType());
}

@Test
public void testConfigurationValidation() {
    DataSourceConfiguration config = new DataSourceConfiguration();

    // Test validation failures
    assertThrows(IllegalArgumentException.class, () -> config.validate());

    // Test valid configuration
    config.setName("test-db");
    config.setType("database");
    config.setConnection(createValidConnectionConfig());

    assertDoesNotThrow(() -> config.validate());
}
```

### Integration Testing

**✅ DO**: Test end-to-end workflows
```java
@Test
public void testDataSourceIntegration() throws Exception {
    // Initialize manager with test configuration
    DataSourceManager manager = new DataSourceManager();
    manager.initialize(testConfigurations);

    // Test data retrieval
    ExternalDataSource dataSource = manager.getDataSource("test-database");
    Map<String, Object> params = Map.of("id", 1);
    Object result = dataSource.queryForObject("getUserById", params);

    assertNotNull(result);

    // Test health monitoring
    assertTrue(dataSource.isHealthy());

    // Test metrics collection
    DataSourceMetrics metrics = dataSource.getMetrics();
    assertTrue(metrics.getTotalRequests() > 0);
}

@Test
public void testFailoverScenario() throws Exception {
    // Setup primary and backup data sources
    DataSourceManager manager = setupManagerWithFailover();

    // Simulate primary failure
    simulatePrimaryFailure();

    // Test automatic failover
    Map<String, Object> params = Map.of("id", 123);
    List<Object> results = manager.queryWithFailover(DataSourceType.DATABASE, "getUserById", params);

    assertNotNull(results);
    assertFalse(results.isEmpty());
}
```

### Performance Testing

**✅ DO**: Test under load
```java
@Test
public void testDataSourcePerformance() throws Exception {
    int threadCount = 10;
    int operationsPerThread = 100;
    ExecutorService executor = Executors.newFixedThreadPool(threadCount);
    CountDownLatch latch = new CountDownLatch(threadCount);

    long startTime = System.currentTimeMillis();

    for (int i = 0; i < threadCount; i++) {
        executor.submit(() -> {
            try {
                for (int j = 0; j < operationsPerThread; j++) {
                    dataSource.queryForObject("getUserById", Map.of("id", j));
                }
            } finally {
                latch.countDown();
            }
        });
    }

    latch.await(30, TimeUnit.SECONDS);
    long endTime = System.currentTimeMillis();

    double operationsPerSecond = (double) (threadCount * operationsPerThread) /
                                ((endTime - startTime) / 1000.0);

    // Assert performance requirements
    assertTrue("Performance too low: " + operationsPerSecond + " ops/sec",
               operationsPerSecond > 100);
}
```

### Mock Testing

**✅ DO**: Use mocks for external dependencies
```java
@Test
public void testWithMockDataSource() {
    // Create mock data source
    ExternalDataSource mockDataSource = Mockito.mock(ExternalDataSource.class);

    // Setup mock behavior
    when(mockDataSource.queryForObject(eq("getUserById"), any()))
        .thenReturn(createMockUser());
    when(mockDataSource.isHealthy()).thenReturn(true);

    // Test business logic
    UserService userService = new UserService(mockDataSource);
    User user = userService.getUser(123);

    assertNotNull(user);
    assertEquals("test-user", user.getUsername());

    // Verify interactions
    verify(mockDataSource).queryForObject("getUserById", Map.of("id", 123));
}
```

### Test Configuration

**✅ DO**: Use test-specific configurations
```yaml
# test-data-sources.yaml
dataSources:
  - name: "test-database"
    type: "database"
    sourceType: "h2"
    connection:
      url: "jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1"
      username: "sa"
      password: ""
    queries:
      getUserById: "SELECT * FROM users WHERE id = :id"
    cache:
      enabled: false  # Disable caching for predictable tests
    healthCheck:
      enabled: false  # Disable health checks for faster tests
```

### Testing Best Practices

1. **Unit Tests**: Test individual components in isolation
2. **Integration Tests**: Test complete workflows
3. **Performance Tests**: Validate performance requirements
4. **Mock External Dependencies**: Use mocks for reliable tests
5. **Test Data Management**: Use consistent test data
6. **Environment Isolation**: Use separate test environments

## Deployment and Operations

### Configuration Management

**✅ DO**: Use configuration management tools
```bash
# Use environment-specific configuration files
kubectl create configmap datasource-config --from-file=data-sources-prod.yaml

# Use secrets for sensitive data
kubectl create secret generic datasource-secrets \
  --from-literal=DB_PASSWORD=secure_password \
  --from-literal=API_KEY=secret_api_key
```

### Health Checks

**✅ DO**: Implement readiness and liveness probes
```yaml
# Kubernetes deployment example
spec:
  containers:
  - name: app
    livenessProbe:
      httpGet:
        path: /health/datasources
        port: 8080
      initialDelaySeconds: 30
      periodSeconds: 10

    readinessProbe:
      httpGet:
        path: /ready/datasources
        port: 8080
      initialDelaySeconds: 5
      periodSeconds: 5
```

### Capacity Planning

**✅ DO**: Monitor resource usage
```java
// Monitor connection pool usage
int activeConnections = metrics.getActiveConnections();
int maxConnections = config.getConnection().getMaxPoolSize();
double poolUtilization = (double) activeConnections / maxConnections;

// Monitor cache usage
long cacheSize = cacheMetrics.getCurrentSize();
long maxCacheSize = config.getCache().getMaxSize();
double cacheUtilization = (double) cacheSize / maxCacheSize;

// Plan for growth
if (poolUtilization > 0.8) {
    logger.warn("Consider increasing connection pool size");
}
```

### Production Configuration Example

```yaml
dataSources:
  - name: "production-database"
    type: "database"
    sourceType: "postgresql"
    enabled: true
    description: "Production PostgreSQL database"
    tags: ["production", "primary"]

    connection:
      host: "prod-db.example.com"
      port: 5432
      database: "myapp_prod"
      username: "app_user"
      password: "${PROD_DB_PASSWORD}"
      schema: "public"

      maxPoolSize: 30
      minPoolSize: 10
      connectionTimeout: 20000
      idleTimeout: 300000
      maxLifetime: 900000

      sslEnabled: true
      sslMode: "require"
      sslRootCert: "/etc/ssl/certs/ca-cert.pem"

    queries:
      getUserById: "SELECT id, username, email, status, created_at FROM users WHERE id = :id"
      getUserByEmail: "SELECT id, username, email, status FROM users WHERE email = :email"
      getActiveUsers: "SELECT id, username, email FROM users WHERE status = 'ACTIVE' ORDER BY last_login DESC LIMIT :limit"
      createUser: "INSERT INTO users (username, email, status) VALUES (:username, :email, 'ACTIVE') RETURNING id"
      updateUserStatus: "UPDATE users SET status = :status, updated_at = NOW() WHERE id = :id"
      getUserStats: "SELECT COUNT(*) as total, COUNT(CASE WHEN status = 'ACTIVE' THEN 1 END) as active FROM users"
      default: "SELECT 1"

    parameterNames: ["id", "email", "username", "status", "limit"]

    cache:
      enabled: true
      ttlSeconds: 300
      maxSize: 2000
      keyPrefix: "proddb"

    healthCheck:
      enabled: true
      intervalSeconds: 30
      timeoutSeconds: 5
      failureThreshold: 2
      query: "SELECT COUNT(*) FROM users LIMIT 1"
```

## Troubleshooting

### Common Issues and Solutions

#### Connection Failures
```
Error: Failed to connect to database
Solution: Check connection parameters, network connectivity, and credentials
```

**Debugging Steps:**
1. Verify connection parameters (host, port, database name)
2. Test network connectivity: `telnet host port`
3. Check credentials and permissions
4. Review SSL/TLS configuration
5. Check firewall rules

#### Cache Misses
```
Issue: Low cache hit ratio
Solution: Increase TTL, review cache key patterns, check cache size limits
```

**Debugging Steps:**
1. Monitor cache metrics: `metrics.getCacheHitRatio()`
2. Review TTL settings
3. Check cache key generation logic
4. Verify cache size limits
5. Analyze access patterns

#### Circuit Breaker Trips
```
Issue: Circuit breaker preventing API calls
Solution: Check API health, review failure thresholds, verify network connectivity
```

**Debugging Steps:**
1. Check circuit breaker state
2. Review failure threshold settings
3. Test API endpoint manually
4. Check network connectivity
5. Review error logs

#### High CPU Usage
```
Issue: High CPU usage from data source operations
Solution: Reduce polling frequency, optimize queries, check connection pool settings
```

**Debugging Steps:**
1. Profile application CPU usage
2. Review health check intervals
3. Optimize database queries
4. Check connection pool configuration
5. Monitor thread usage

#### Memory Issues
```
Issue: OutOfMemoryError related to data sources
Solution: Reduce cache sizes, check for connection leaks, optimize data structures
```

**Debugging Steps:**
1. Monitor heap usage
2. Check for connection leaks
3. Review cache configurations
4. Analyze memory dumps
5. Optimize data structures

### Debugging Tools

**✅ DO**: Use comprehensive logging
```yaml
logging:
  level:
    dev.mars.rulesengine.core.service.data.external: DEBUG
    com.zaxxer.hikari: DEBUG
```

**✅ DO**: Enable JMX monitoring
```java
// Enable JMX for connection pools and caches
System.setProperty("com.zaxxer.hikari.housekeeping.periodMs", "30000");
```

**✅ DO**: Monitor health status
```java
ConnectionStatus status = dataSource.getConnectionStatus();
System.out.println("Status: " + status.getState());
System.out.println("Message: " + status.getMessage());
```

**✅ DO**: Review metrics regularly
```java
DataSourceMetrics metrics = dataSource.getMetrics();
System.out.println("Success rate: " + metrics.getSuccessRate());
System.out.println("Avg response time: " + metrics.getAverageResponseTime());
```

### Performance Tuning

1. **Database Connection Pools**:
   ```yaml
   connection:
     maxPoolSize: 20        # Adjust based on load
     minPoolSize: 5         # Keep minimum connections
     connectionTimeout: 30000
     idleTimeout: 600000
   ```

2. **API Circuit Breakers**:
   ```yaml
   circuitBreaker:
     failureThreshold: 5    # Number of failures before opening
     recoveryTimeout: 30000 # Time before attempting recovery
     halfOpenMaxCalls: 3    # Test calls in half-open state
   ```

3. **Cache Optimization**:
   ```yaml
   cache:
     maxSize: 10000         # Increase for better hit rates
     ttlSeconds: 600        # Balance freshness vs performance
     evictionPolicy: "LRU"  # Use appropriate eviction strategy
   ```

### Operational Checklist

- [ ] Configuration validated and tested
- [ ] Environment variables properly set
- [ ] SSL certificates installed and valid
- [ ] Database permissions configured
- [ ] Health checks enabled and working
- [ ] Monitoring and alerting configured
- [ ] Performance baselines established
- [ ] Backup and recovery procedures tested
- [ ] Documentation updated
- [ ] Team trained on operations

This comprehensive guide covers all aspects of external data source integration in the SpEL Rules Engine, providing developers and operators with the knowledge needed to successfully implement, configure, and maintain robust data integration solutions.
