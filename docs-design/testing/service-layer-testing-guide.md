# APEX Service Layer Testing Guide

## Overview

This guide provides comprehensive documentation for the APEX Rules Engine service layer test suite. The service layer contains critical components for data source management, caching, database operations, file processing, and system integration.

## Test Architecture

The service layer tests are organized into logical groups that mirror the service architecture:

```
apex-core/src/test/java/dev/mars/apex/core/service/
├── data/                    # Data management services
│   ├── external/           # External data source services
│   └── yaml/              # YAML data integration
├── engine/                 # Expression evaluation services
├── enrichment/            # Data enrichment services
├── lookup/                # Lookup services
├── monitoring/            # Performance monitoring
├── scenario/              # Scenario testing services
├── transform/             # Data transformation services
└── validation/            # Validation services
```

## Cache Services Testing

### CacheDataSourceTest (37 tests)

**Purpose:** Tests the cache-based data source implementation that provides high-performance data access through caching mechanisms.

**Key Test Categories:**
- **Initialization & Configuration** (8 tests)
  - Cache data source creation with various configurations
  - Configuration validation and error handling
  - Cache provider integration (Redis, Hazelcast, In-Memory)

- **Data Operations** (12 tests)
  - Cache key generation and management
  - Data retrieval with cache hits and misses
  - Cache invalidation and refresh operations
  - Bulk operations and batch processing

- **Performance & Metrics** (9 tests)
  - Cache hit/miss ratio tracking
  - Performance metrics collection
  - Memory usage monitoring
  - Cache eviction policy testing

- **Error Handling & Recovery** (8 tests)
  - Cache provider failures and fallback mechanisms
  - Network connectivity issues
  - Data corruption handling
  - Timeout and retry logic

**Example Test Scenarios:**
```java
@Test
void shouldRetrieveDataFromCacheWhenAvailable() {
    // Tests cache hit scenario with performance metrics
}

@Test
void shouldFallbackToDataSourceOnCacheMiss() {
    // Tests cache miss handling and data source fallback
}
```

### CacheManagerTest (22 tests)

**Purpose:** Tests the central cache management system that coordinates multiple cache instances and providers.

**Key Test Categories:**
- **Cache Lifecycle Management** (6 tests)
  - Cache creation, initialization, and destruction
  - Cache provider registration and deregistration
  - Multi-cache coordination

- **Configuration Management** (5 tests)
  - Dynamic cache configuration updates
  - Cache policy enforcement
  - Provider-specific configuration handling

- **Monitoring & Statistics** (6 tests)
  - Cache performance monitoring
  - Statistics aggregation across multiple caches
  - Health check integration

- **Concurrency & Thread Safety** (5 tests)
  - Concurrent cache operations
  - Thread-safe cache access
  - Lock management and deadlock prevention

### CacheStatisticsTest (22 tests)

**Purpose:** Tests the cache statistics collection and reporting system for performance monitoring and optimization.

**Key Test Categories:**
- **Metrics Collection** (8 tests)
  - Hit/miss ratio calculation
  - Response time tracking
  - Memory usage statistics
  - Eviction rate monitoring

- **Reporting & Aggregation** (7 tests)
  - Statistics aggregation across time periods
  - Multi-cache statistics consolidation
  - Performance trend analysis

- **Alerting & Thresholds** (7 tests)
  - Performance threshold monitoring
  - Alert generation for cache issues
  - SLA compliance tracking

### InMemoryCacheManagerTest (36 tests)

**Purpose:** Tests the in-memory cache implementation for high-performance local caching.

**Key Test Categories:**
- **Memory Management** (12 tests)
  - Memory allocation and deallocation
  - Garbage collection integration
  - Memory leak prevention
  - Out-of-memory handling

- **Eviction Policies** (10 tests)
  - LRU (Least Recently Used) eviction
  - LFU (Least Frequently Used) eviction
  - TTL (Time To Live) expiration
  - Custom eviction strategies

- **Concurrency Control** (8 tests)
  - Concurrent read/write operations
  - Lock-free data structures
  - Thread contention handling
  - Performance under load

- **Data Integrity** (6 tests)
  - Data consistency across operations
  - Atomic operations
  - Transaction-like behavior
  - Corruption detection

## Database Services Testing

### DatabaseDataSourceTest (27 tests)

**Purpose:** Tests database connectivity and data access operations for relational database integration.

**Key Test Categories:**
- **Connection Management** (8 tests)
  - Database connection establishment
  - Connection pooling and lifecycle
  - Connection timeout and retry logic
  - SSL/TLS connection security

- **Query Execution** (10 tests)
  - SQL query execution and result processing
  - Prepared statement handling
  - Batch operation support
  - Transaction management

- **Error Handling** (9 tests)
  - Database connectivity failures
  - SQL syntax and execution errors
  - Timeout and deadlock handling
  - Recovery and retry mechanisms

### DatabaseHealthIndicatorTest (14 tests)

**Purpose:** Tests database health monitoring and status reporting for system reliability.

**Key Test Categories:**
- **Health Check Operations** (6 tests)
  - Database connectivity verification
  - Query execution health checks
  - Performance threshold monitoring
  - Health status reporting

- **Failure Detection** (4 tests)
  - Connection failure detection
  - Query timeout detection
  - Performance degradation alerts
  - Recovery status monitoring

- **Integration Testing** (4 tests)
  - Health check service integration
  - Monitoring system integration
  - Alert system integration
  - Dashboard reporting

### JdbcTemplateFactoryTest (15 tests)

**Purpose:** Tests JDBC template creation and configuration for database operations.

**Key Test Categories:**
- **Template Creation** (6 tests)
  - JDBC template instantiation
  - DataSource configuration
  - Connection pool integration
  - Template customization

- **Configuration Management** (5 tests)
  - Database-specific configurations
  - Connection parameter handling
  - Security configuration
  - Performance tuning

- **Error Handling** (4 tests)
  - Invalid configuration handling
  - Connection failures
  - Template creation errors
  - Resource cleanup

### PostgreSQLIntegrationTest (14 tests)

**Purpose:** Tests PostgreSQL-specific integration and functionality.

**Key Test Categories:**
- **PostgreSQL Features** (6 tests)
  - PostgreSQL-specific data types
  - Advanced query features
  - JSON/JSONB support
  - Array operations

- **Performance Testing** (4 tests)
  - Query performance optimization
  - Connection pool performance
  - Bulk operation efficiency
  - Index utilization

- **Integration Scenarios** (4 tests)
  - Real-world data scenarios
  - Complex query operations
  - Transaction handling
  - Error recovery

## File Services Testing

### CsvDataLoaderTest (27 tests)

**Purpose:** Tests CSV file processing and data loading capabilities.

**Key Test Categories:**
- **File Parsing** (10 tests)
  - CSV format parsing and validation
  - Header row processing
  - Delimiter and quote handling
  - Encoding support (UTF-8, ISO-8859-1)

- **Data Transformation** (8 tests)
  - Data type conversion
  - Field mapping and transformation
  - Null value handling
  - Data validation and cleansing

- **Error Handling** (9 tests)
  - Malformed CSV handling
  - File access errors
  - Data validation failures
  - Large file processing

### JsonDataLoaderTest (27 tests)

**Purpose:** Tests JSON file processing and data loading capabilities.

**Key Test Categories:**
- **JSON Parsing** (10 tests)
  - JSON structure parsing
  - Nested object handling
  - Array processing
  - Schema validation

- **Data Extraction** (8 tests)
  - JSONPath expression evaluation
  - Data flattening operations
  - Complex data structure handling
  - Type conversion

- **Error Handling** (9 tests)
  - Invalid JSON handling
  - Schema validation errors
  - Memory management for large files
  - Streaming JSON processing

## Configuration Services Testing

### DataSourceConfigurationServiceTest (24 tests)

**Purpose:** Tests the central data source configuration management service.

**Key Test Categories:**
- **Configuration Management** (8 tests)
  - Configuration loading and validation
  - Dynamic configuration updates
  - Configuration persistence
  - Multi-environment support

- **Service Lifecycle** (6 tests)
  - Service initialization and shutdown
  - Configuration reload operations
  - Event notification system
  - Error recovery

- **Integration Testing** (10 tests)
  - YAML configuration integration
  - Data source factory integration
  - Event listener integration
  - Monitoring integration

### DataSourceConfigurationEventTest (24 tests)

**Purpose:** Tests the event system for configuration changes and notifications.

**Key Test Categories:**
- **Event Generation** (8 tests)
  - Configuration change events
  - Event payload validation
  - Event timing and ordering
  - Event filtering

- **Event Processing** (8 tests)
  - Event listener registration
  - Event dispatch mechanisms
  - Asynchronous event handling
  - Event persistence

- **Error Handling** (8 tests)
  - Event processing failures
  - Listener exception handling
  - Event queue management
  - Recovery mechanisms

### DataSourceConfigurationListenerTest (14 tests)

**Purpose:** Tests event listeners for configuration change notifications.

**Key Test Categories:**
- **Listener Registration** (5 tests)
  - Listener registration and deregistration
  - Multiple listener support
  - Listener priority handling
  - Dynamic listener management

- **Event Handling** (5 tests)
  - Configuration change processing
  - Event filtering and routing
  - Asynchronous processing
  - Error propagation

- **Integration Testing** (4 tests)
  - Service integration
  - Event system integration
  - Monitoring integration
  - Performance impact

## Factory Services Testing

### DataSourceFactoryTest (20 tests)

**Purpose:** Tests the factory pattern implementation for creating data source instances.

**Key Test Categories:**
- **Factory Operations** (8 tests)
  - Data source creation by type
  - Configuration-based instantiation
  - Factory registration and lookup
  - Instance lifecycle management

- **Type Support** (6 tests)
  - Database data source creation
  - Cache data source creation
  - File data source creation
  - Custom data source creation

- **Error Handling** (6 tests)
  - Invalid configuration handling
  - Unsupported type handling
  - Creation failure recovery
  - Resource cleanup

## Core Data Services Testing

### CustomDataSourceTest (32 tests)

**Purpose:** Tests custom data source implementations and extensibility.

**Key Test Categories:**
- **Custom Implementation** (12 tests)
  - Custom data source registration
  - Implementation validation
  - Interface compliance
  - Performance characteristics

- **Integration Testing** (10 tests)
  - Factory integration
  - Configuration integration
  - Monitoring integration
  - Event system integration

- **Extensibility** (10 tests)
  - Plugin architecture support
  - Dynamic loading
  - Version compatibility
  - Migration support

### DataSourceTypeTest (33 tests)

**Purpose:** Tests data source type enumeration and type-specific behavior.

**Key Test Categories:**
- **Type Enumeration** (12 tests)
  - Type definition and validation
  - Type conversion operations
  - Type compatibility checking
  - Type metadata handling

- **Type-Specific Behavior** (12 tests)
  - Database-specific operations
  - Cache-specific operations
  - File-specific operations
  - Custom type operations

- **Validation & Constraints** (9 tests)
  - Type validation rules
  - Configuration constraints
  - Compatibility requirements
  - Error handling

### DataSourceMetricsTest (32 tests)

**Purpose:** Tests metrics collection and performance monitoring for data sources.

**Key Test Categories:**
- **Metrics Collection** (12 tests)
  - Performance metrics gathering
  - Usage statistics tracking
  - Error rate monitoring
  - Resource utilization

- **Aggregation & Reporting** (10 tests)
  - Metrics aggregation across sources
  - Time-series data handling
  - Report generation
  - Dashboard integration

- **Alerting & Monitoring** (10 tests)
  - Threshold-based alerting
  - Performance degradation detection
  - SLA monitoring
  - Automated responses

### DataSourceExceptionTest (25 tests)

**Purpose:** Tests exception handling and error management across data sources.

**Key Test Categories:**
- **Exception Hierarchy** (8 tests)
  - Exception type classification
  - Error code management
  - Exception chaining
  - Context preservation

- **Error Handling** (9 tests)
  - Graceful degradation
  - Retry mechanisms
  - Fallback strategies
  - Recovery procedures

- **Logging & Monitoring** (8 tests)
  - Error logging and tracking
  - Exception metrics
  - Alert generation
  - Diagnostic information

## Integration Tests

### ExternalDataSourceIntegrationTest (8 tests)

**Purpose:** Tests end-to-end integration of external data sources with the rules engine.

**Key Test Categories:**
- **End-to-End Scenarios** (4 tests)
  - Complete data flow testing
  - Multi-source integration
  - Performance under load
  - Real-world use cases

- **System Integration** (4 tests)
  - Rules engine integration
  - Configuration system integration
  - Monitoring system integration
  - Error handling integration

### YamlDatasetIntegrationTest (9 tests)

**Purpose:** Tests YAML-based dataset configuration and integration.

**Key Test Categories:**
- **YAML Processing** (4 tests)
  - YAML parsing and validation
  - Configuration mapping
  - Schema compliance
  - Error handling

- **Dataset Integration** (5 tests)
  - Dataset loading and initialization
  - Data source binding
  - Performance optimization
  - Memory management

## Running the Tests

### Prerequisites
- Java 17 or higher
- Maven 3.8+
- PostgreSQL (for integration tests)
- Redis (optional, for cache tests)

### Execution Commands

```bash
# Run all service tests
mvn test -pl apex-core -Dtest="dev.mars.apex.core.service.**"

# Run specific service category
mvn test -pl apex-core -Dtest="*Cache*Test"
mvn test -pl apex-core -Dtest="*Database*Test"
mvn test -pl apex-core -Dtest="*File*Test"

# Run with specific profiles
mvn test -pl apex-core -Pintegration-tests
mvn test -pl apex-core -Pperformance-tests
```

### Test Configuration

Tests use configuration files located in `src/test/resources/`:
- `application-test.yml` - Test application configuration
- `test-data/` - Test data files and fixtures
- `test-configs/` - Test-specific configurations

## Best Practices

### Test Design Principles
1. **Isolation** - Each test is independent and can run in any order
2. **Repeatability** - Tests produce consistent results across runs
3. **Performance** - Tests complete within reasonable time limits
4. **Coverage** - Tests cover both happy path and error scenarios
5. **Documentation** - Tests serve as living documentation

### Maintenance Guidelines
1. **Regular Updates** - Keep tests updated with code changes
2. **Performance Monitoring** - Monitor test execution times
3. **Resource Management** - Properly clean up test resources
4. **Error Analysis** - Investigate and fix flaky tests promptly
5. **Coverage Analysis** - Maintain high test coverage levels

## Troubleshooting

### Common Issues
1. **Database Connection Failures** - Check PostgreSQL service status
2. **Cache Provider Issues** - Verify Redis/Hazelcast availability
3. **File Access Errors** - Check file permissions and paths
4. **Memory Issues** - Increase JVM heap size for large tests
5. **Timeout Errors** - Adjust test timeout configurations

### Debug Configuration
```yaml
logging:
  level:
    dev.mars.apex.core.service: DEBUG
    org.springframework.jdbc: DEBUG
    org.springframework.cache: DEBUG
```

## Metrics and Reporting

The service layer tests generate comprehensive metrics:
- **Test Execution Time** - Performance tracking per test class
- **Coverage Reports** - Code coverage analysis
- **Performance Metrics** - Service performance under test conditions
- **Error Analysis** - Failure pattern analysis and reporting

## Conclusion

The APEX service layer test suite provides comprehensive coverage of all critical service components, ensuring reliability, performance, and maintainability of the rules engine platform. Regular execution of these tests helps maintain system quality and catch regressions early in the development cycle.
