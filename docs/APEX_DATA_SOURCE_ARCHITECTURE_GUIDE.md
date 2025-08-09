# APEX Data Source Architecture Guide

## Overview

This guide clarifies the distinction between testing utilities and production data sources in the APEX Rules Engine, addressing previous confusion between `MockDataSource` (for testing) and production `ExternalDataSource` implementations.

## 🚨 Important Architectural Changes

### Previous Issue
The demo code was incorrectly using `MockDataSource` as if it were a production implementation, creating confusion about the proper data source architecture.

### Resolution
- **MockDataSource** → Moved to test packages (for testing only)
- **Production Demos** → Now use proper `ExternalDataSource` implementations
- **Clear Separation** → Testing utilities vs. production capabilities

## 📊 Data Source Architecture

### 1. Base DataSource Interface

```java
public interface DataSource {
    String getName();
    String getDataType();
    boolean supportsDataType(String dataType);
    <T> T getData(String dataType, Object... parameters);
    default Object lookup(String key) { ... }
}
```

### 2. Production ExternalDataSource Interface

```java
public interface ExternalDataSource extends DataSource {
    // Enterprise features
    DataSourceType getSourceType();
    ConnectionStatus getConnectionStatus();
    DataSourceMetrics getMetrics();
    void initialize(DataSourceConfiguration config);
    void shutdown();
    boolean isHealthy();
    
    // Query capabilities
    <T> List<T> query(String query, Map<String, Object> parameters);
    <T> T queryForObject(String query, Map<String, Object> parameters);
    
    // Batch operations
    <T> List<T> batchQuery(List<String> queries);
    void batchUpdate(List<String> updates);
}
```

## 🏭 Production Data Source Implementations

### 1. DatabaseDataSource
**Purpose**: Enterprise database connectivity
**Features**:
- JDBC connection pooling
- Multiple database support (PostgreSQL, MySQL, Oracle, SQL Server, H2)
- Health monitoring and metrics
- Transaction management
- Connection lifecycle management

**Usage**:
```java
DatabaseDataSource dbSource = new DatabaseDataSource();
DataSourceConfiguration config = createDatabaseConfig();
dbSource.initialize(config);
```

### 2. FileSystemDataSource
**Purpose**: File-based data processing
**Features**:
- Multiple format support (CSV, JSON, XML, Fixed-width)
- File watching and auto-reload
- Caching with TTL
- Pattern-based file discovery
- Format-specific parsers

**Usage**:
```java
FileSystemDataSource fileSource = new FileSystemDataSource();
DataSourceConfiguration config = createFileConfig("data/", "*.json");
fileSource.initialize(config);
```

### 3. RestApiDataSource
**Purpose**: REST API integration
**Features**:
- Circuit breaker pattern
- Authentication support
- Retry mechanisms
- Response caching
- Health monitoring

**Usage**:
```java
RestApiDataSource apiSource = new RestApiDataSource();
DataSourceConfiguration config = createRestConfig("https://api.example.com");
apiSource.initialize(config);
```

### 4. CacheDataSource
**Purpose**: High-performance caching
**Features**:
- Multiple eviction policies (LRU, LFU, FIFO, TTL)
- Cache statistics and monitoring
- Key pattern matching
- Batch operations
- Distributed caching support

**Usage**:
```java
CacheDataSource cacheSource = new CacheDataSource();
DataSourceConfiguration config = createCacheConfig(1000, 3600);
cacheSource.initialize(config);
```

## 🧪 Testing vs Production

### MockDataSource (Testing Only)

**Location**: `apex-demo/src/test/java/dev/mars/apex/demo/test/data/MockDataSource.java`

**Purpose**: Unit testing and isolated testing scenarios

**Limitations**:
- ❌ No health monitoring
- ❌ No connection management
- ❌ No metrics collection
- ❌ No configuration-driven setup
- ❌ No error handling and recovery
- ❌ No scalability features
- ❌ Hardcoded data types only

**Correct Usage**:
```java
// IN TESTS ONLY
@Test
public void testRuleExecution() {
    MockDataSource mockSource = new MockDataSource("test-source", "products");
    // ... test logic
}
```

### Production Data Sources

**Location**: `apex-core/src/main/java/dev/mars/apex/core/service/data/external/`

**Purpose**: Production applications and realistic demos

**Features**:
- ✅ Enterprise-grade health monitoring
- ✅ Connection pooling and management
- ✅ Comprehensive metrics collection
- ✅ YAML-based configuration
- ✅ Robust error handling and recovery
- ✅ Horizontal scalability
- ✅ Flexible data type support

**Correct Usage**:
```java
// IN PRODUCTION AND DEMOS
FileSystemDataSource productionSource = new FileSystemDataSource();
DataSourceConfiguration config = loadFromYaml("data-sources.yaml");
productionSource.initialize(config);
```

## 📋 Migration Guide

### Before (Incorrect)
```java
// DON'T DO THIS - MockDataSource in production demos
public class DemoDataServiceManager extends DataServiceManager {
    @Override
    public DataServiceManager initializeWithMockData() {
        loadDataSource(new MockDataSource("ProductsDataSource", "products"));
        loadDataSource(new MockDataSource("CustomerDataSource", "customer"));
        return this;
    }
}
```

### After (Correct)
```java
// DO THIS - Production data sources in demos
public class ProductionDemoDataServiceManager extends DataServiceManager {
    @Override
    public DataServiceManager initializeWithMockData() {
        // Use FileSystemDataSource for demo data
        FileSystemDataSource fileSource = new FileSystemDataSource();
        fileSource.initialize(createFileConfig("demo-data/"));
        loadDataSource(fileSource);
        
        // Use CacheDataSource for fast lookups
        CacheDataSource cacheSource = new CacheDataSource();
        cacheSource.initialize(createCacheConfig());
        loadDataSource(cacheSource);
        
        return this;
    }
}
```

## 🔧 Configuration Examples

### File System Data Source
```yaml
data-sources:
  - name: "demo-json-files"
    type: "FILE_SYSTEM"
    enabled: true
    
    connection:
      base-path: "demo-data/json"
      file-pattern: "*.json"
      encoding: "UTF-8"
      
    properties:
      auto-reload: true
      watch-for-changes: true
      cache-parsed-files: true
      cache-ttl-seconds: 300
```

### Cache Data Source
```yaml
data-sources:
  - name: "demo-products-cache"
    type: "CACHE"
    enabled: true
    
    properties:
      max-size: 1000
      ttl-seconds: 3600
      eviction-policy: "LRU"
      statistics-enabled: true
```

### Database Data Source
```yaml
data-sources:
  - name: "demo-database"
    type: "DATABASE"
    enabled: true
    
    connection:
      url: "jdbc:postgresql://localhost:5432/apex"
      driver-class: "org.postgresql.Driver"
      username: "apex_user"
      password: "apex_password"
      
    connection-pool:
      initial-size: 5
      max-size: 20
      min-idle: 2
      max-wait-seconds: 30
```

## 🎯 Best Practices

### 1. Use Appropriate Data Sources
- **Testing**: Use `MockDataSource` in unit tests only
- **Demos**: Use production `ExternalDataSource` implementations
- **Production**: Use production `ExternalDataSource` implementations with proper configuration

### 2. Configuration Management
- Store data source configurations in YAML files
- Use environment-specific overrides
- Include proper connection pooling and timeout settings

### 3. Error Handling
- Implement proper health checks
- Use circuit breaker patterns for external sources
- Provide fallback mechanisms

### 4. Monitoring
- Enable metrics collection
- Monitor connection health
- Track performance statistics

### 5. Resource Management
- Properly initialize and shutdown data sources
- Use connection pooling for database sources
- Implement proper caching strategies

## 📚 Related Documentation

- [APEX External Data Sources Guide](APEX_EXTERNAL_DATA_SOURCES_GUIDE.md)
- [APEX Data Management Guide](APEX_DATA_MANAGEMENT_GUIDE.md)
- [APEX Configuration Reference](APEX_CONFIGURATION_REFERENCE.md)

## 🔄 Summary

The key takeaway is clear separation of concerns:

- **MockDataSource** = Testing utilities only
- **ExternalDataSource implementations** = Production and realistic demos
- **Proper configuration** = YAML-based setup with monitoring and error handling
- **Resource management** = Proper initialization, health checks, and shutdown

This architecture provides a clear path from development and testing through to production deployment, with appropriate data source implementations for each stage.
