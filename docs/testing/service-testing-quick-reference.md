# APEX Service Testing Quick Reference

## Test Suite Overview

| Service Category | Test Classes | Total Tests | Coverage Focus |
|------------------|--------------|-------------|----------------|
| **Cache Services** | 4 classes | 117 tests | Caching, Performance, Memory Management |
| **Database Services** | 4 classes | 70 tests | JDBC, Connections, Health Monitoring |
| **File Services** | 2 classes | 54 tests | CSV/JSON Processing, Data Loading |
| **Configuration Services** | 3 classes | 62 tests | Config Management, Events, Listeners |
| **Factory Services** | 1 class | 20 tests | Data Source Creation, Type Management |
| **Core Data Services** | 4 classes | 122 tests | Custom Sources, Metrics, Error Handling |
| **Integration Tests** | 2 classes | 17 tests | End-to-End, YAML Integration |

**Total: 20 test classes, 462 service tests**

## Quick Test Execution

### Run All Service Tests
```bash
mvn test -pl apex-core -Dtest="dev.mars.apex.core.service.**"
```

### Run by Category
```bash
# Cache Services
mvn test -pl apex-core -Dtest="*Cache*Test"

# Database Services  
mvn test -pl apex-core -Dtest="*Database*Test"

# File Services
mvn test -pl apex-core -Dtest="*DataLoader*Test"

# Configuration Services
mvn test -pl apex-core -Dtest="*Configuration*Test"

# Integration Tests
mvn test -pl apex-core -Dtest="*Integration*Test"
```

### Run Specific Test Classes
```bash
# Cache Tests
mvn test -pl apex-core -Dtest="CacheDataSourceTest"
mvn test -pl apex-core -Dtest="CacheManagerTest"
mvn test -pl apex-core -Dtest="CacheStatisticsTest"
mvn test -pl apex-core -Dtest="InMemoryCacheManagerTest"

# Database Tests
mvn test -pl apex-core -Dtest="DatabaseDataSourceTest"
mvn test -pl apex-core -Dtest="DatabaseHealthIndicatorTest"
mvn test -pl apex-core -Dtest="JdbcTemplateFactoryTest"
mvn test -pl apex-core -Dtest="PostgreSQLIntegrationTest"

# File Tests
mvn test -pl apex-core -Dtest="CsvDataLoaderTest"
mvn test -pl apex-core -Dtest="JsonDataLoaderTest"
```

## Test Class Breakdown

### Cache Services (117 tests)

#### CacheDataSourceTest (37 tests)
```java
// Key test methods
testCacheInitialization()           // Cache setup and configuration
testCacheHitScenario()             // Data retrieval from cache
testCacheMissHandling()            // Fallback to data source
testCacheInvalidation()            // Cache refresh operations
testPerformanceMetrics()           // Hit/miss ratio tracking
testErrorRecovery()                // Failure handling
```

#### CacheManagerTest (22 tests)
```java
// Key test methods
testCacheLifecycle()               // Creation and destruction
testMultiCacheCoordination()       // Multiple cache management
testConfigurationUpdates()         // Dynamic config changes
testConcurrentOperations()         // Thread safety
testHealthMonitoring()             // Cache health checks
```

#### CacheStatisticsTest (22 tests)
```java
// Key test methods
testMetricsCollection()            // Statistics gathering
testHitMissRatioCalculation()      // Performance metrics
testResponseTimeTracking()         // Latency monitoring
testMemoryUsageStats()             // Memory tracking
testAlertGeneration()              // Threshold alerts
```

#### InMemoryCacheManagerTest (36 tests)
```java
// Key test methods
testMemoryManagement()             // Memory allocation/deallocation
testEvictionPolicies()             // LRU, LFU, TTL eviction
testConcurrencyControl()           // Thread-safe operations
testDataIntegrity()                // Consistency checks
testPerformanceUnderLoad()         // Stress testing
```

### Database Services (70 tests)

#### DatabaseDataSourceTest (27 tests)
```java
// Key test methods
testConnectionEstablishment()      // Database connectivity
testQueryExecution()               // SQL operations
testTransactionManagement()        // ACID compliance
testConnectionPooling()            // Pool lifecycle
testErrorHandling()                // Failure scenarios
```

#### DatabaseHealthIndicatorTest (14 tests)
```java
// Key test methods
testHealthCheckOperations()        // Connectivity verification
testFailureDetection()             // Problem identification
testPerformanceMonitoring()        // Threshold checking
testRecoveryStatus()               // Health restoration
```

#### JdbcTemplateFactoryTest (15 tests)
```java
// Key test methods
testTemplateCreation()             // JDBC template setup
testDataSourceConfiguration()      // Connection config
testConnectionPoolIntegration()    // Pool integration
testErrorHandling()                // Creation failures
```

#### PostgreSQLIntegrationTest (14 tests)
```java
// Key test methods
testPostgreSQLFeatures()           // PG-specific functionality
testJSONSupport()                  // JSON/JSONB operations
testArrayOperations()              // Array handling
testPerformanceOptimization()      // Query optimization
```

### File Services (54 tests)

#### CsvDataLoaderTest (27 tests)
```java
// Key test methods
testCsvParsing()                   // CSV format handling
testHeaderProcessing()             // Header row management
testDelimiterHandling()            // Custom delimiters
testEncodingSupport()              // Character encoding
testDataTransformation()           // Type conversion
testErrorHandling()                // Malformed data
```

#### JsonDataLoaderTest (27 tests)
```java
// Key test methods
testJsonParsing()                  // JSON structure parsing
testNestedObjectHandling()         // Complex structures
testArrayProcessing()              // Array operations
testJsonPathEvaluation()           // Path expressions
testSchemaValidation()             // Structure validation
testStreamingProcessing()          // Large file handling
```

### Configuration Services (62 tests)

#### DataSourceConfigurationServiceTest (24 tests)
```java
// Key test methods
testServiceInitialization()       // Service startup
testConfigurationLoading()        // Config management
testDynamicUpdates()               // Runtime changes
testEventNotification()           // Change events
testErrorRecovery()                // Failure handling
```

#### DataSourceConfigurationEventTest (24 tests)
```java
// Key test methods
testEventGeneration()              // Event creation
testEventProcessing()              // Event handling
testAsynchronousHandling()         // Async processing
testEventPersistence()             // Event storage
testErrorPropagation()             // Error handling
```

#### DataSourceConfigurationListenerTest (14 tests)
```java
// Key test methods
testListenerRegistration()         // Listener management
testEventHandling()                // Event processing
testFilteringAndRouting()          // Event routing
testPerformanceImpact()            // Performance testing
```

## Test Data and Fixtures

### Test Resources Location
```
src/test/resources/
├── application-test.yml           # Test configuration
├── test-data/
│   ├── sample.csv                 # CSV test data
│   ├── sample.json                # JSON test data
│   └── complex-structure.json     # Complex JSON
├── test-configs/
│   ├── cache-config.yml           # Cache configurations
│   ├── database-config.yml        # Database configurations
│   └── datasource-config.yml      # Data source configurations
└── sql/
    ├── test-schema.sql             # Test database schema
    └── test-data.sql               # Test data inserts
```

### Common Test Patterns

#### Cache Test Pattern
```java
@Test
void testCacheOperation() {
    // Given: Cache configuration
    CacheConfig config = createTestCacheConfig();
    CacheDataSource dataSource = new CacheDataSource(config);
    
    // When: Perform cache operation
    Object result = dataSource.getData("test-key");
    
    // Then: Verify result and metrics
    assertNotNull(result);
    assertTrue(dataSource.getMetrics().getHitRatio() > 0);
}
```

#### Database Test Pattern
```java
@Test
void testDatabaseOperation() {
    // Given: Database connection
    DatabaseDataSource dataSource = createTestDatabaseDataSource();
    
    // When: Execute query
    List<Map<String, Object>> results = dataSource.executeQuery("SELECT * FROM test_table");
    
    // Then: Verify results
    assertFalse(results.isEmpty());
    assertEquals(expectedRowCount, results.size());
}
```

#### File Processing Test Pattern
```java
@Test
void testFileProcessing() {
    // Given: Test file
    String testFile = "test-data/sample.csv";
    CsvDataLoader loader = new CsvDataLoader();
    
    // When: Load data
    List<Map<String, Object>> data = loader.loadData(testFile);
    
    // Then: Verify data structure
    assertFalse(data.isEmpty());
    assertTrue(data.get(0).containsKey("expected_column"));
}
```

## Performance Benchmarks

### Expected Test Execution Times

| Test Category | Execution Time | Notes |
|---------------|----------------|-------|
| Cache Services | 6-8 seconds | Includes cache warming |
| Database Services | 15-20 seconds | Includes connection setup |
| File Services | 2-3 seconds | Small test files |
| Configuration Services | 1-2 seconds | In-memory operations |
| Integration Tests | 20-25 seconds | Full system tests |

### Performance Thresholds

```yaml
# Test performance expectations
cache_operations:
  max_response_time: 10ms
  min_hit_ratio: 80%
  
database_operations:
  max_connection_time: 5s
  max_query_time: 1s
  
file_operations:
  max_parse_time_per_mb: 500ms
  max_memory_usage_per_mb: 10MB
```

## Troubleshooting Guide

### Common Test Failures

#### Cache Test Failures
```bash
# Issue: Cache provider not available
# Solution: Start Redis/Hazelcast or use in-memory cache
mvn test -Dspring.cache.type=simple

# Issue: Memory issues with large cache tests
# Solution: Increase heap size
mvn test -Dtest=InMemoryCacheManagerTest -Xmx2g
```

#### Database Test Failures
```bash
# Issue: PostgreSQL not running
# Solution: Start PostgreSQL service
sudo service postgresql start

# Issue: Connection timeout
# Solution: Increase timeout in test config
mvn test -Dtest.database.timeout=30000
```

#### File Test Failures
```bash
# Issue: File encoding problems
# Solution: Set file encoding
mvn test -Dfile.encoding=UTF-8

# Issue: Large file memory issues
# Solution: Use streaming mode
mvn test -Dtest.file.streaming=true
```

## Test Environment Setup

### Docker Image Version Management

**APEX uses centralized Docker image version constants to ensure consistency across all tests:**

#### **Global Constants Classes:**
- **apex-core**: `dev.mars.apex.core.test.TestContainerImages`
- **apex-demo**: `dev.mars.apex.demo.test.TestContainerImages`

#### **Version Definitions:**
All Docker image versions are defined in the root `pom.xml`:
```xml
<properties>
    <!-- Docker Image Versions for Testcontainers -->
    <docker.postgres.version>postgres:15.13-alpine3.20</docker.postgres.version>
    <docker.vault.version>hashicorp/vault:1.15</docker.vault.version>
    <docker.redis.version>redis:6-alpine</docker.redis.version>
</properties>
```

#### **Usage in Tests:**
```java
import dev.mars.apex.core.test.TestContainerImages;

@Container
static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(TestContainerImages.POSTGRES)
        .withDatabaseName("test")
        .withUsername("test")
        .withPassword("test");

@Container
static VaultContainer<?> vault = new VaultContainer<>(TestContainerImages.VAULT)
        .withVaultToken("myroot");
```

#### **Benefits:**
- ✅ **Single source of truth** for all Docker image versions
- ✅ **Easy version updates** across entire project
- ✅ **Consistent testing environment**
- ✅ **Reduced Docker downloads** (same versions reused)
- ✅ **Better CI/CD performance** with Docker layer caching

### Required Services
```bash
# PostgreSQL (for database tests)
# Version defined in pom.xml as docker.postgres.version
docker run -d --name test-postgres \
  -e POSTGRES_DB=apex_test \
  -e POSTGRES_USER=test \
  -e POSTGRES_PASSWORD=test \
  -p 5432:5432 postgres:15.13-alpine3.20

# Redis (for cache tests)
docker run -d --name test-redis \
  -p 6379:6379 redis:6-alpine
```

### Environment Variables
```bash
export TEST_DB_URL=jdbc:postgresql://localhost:5432/apex_test
export TEST_DB_USER=test
export TEST_DB_PASSWORD=test
export TEST_REDIS_URL=redis://localhost:6379
export TEST_LOG_LEVEL=INFO
```

## Continuous Integration

### CI Pipeline Configuration
```yaml
# .github/workflows/service-tests.yml
name: Service Tests
on: [push, pull_request]
jobs:
  service-tests:
    runs-on: ubuntu-latest
    services:
      postgres:
        # Version defined in pom.xml as docker.postgres.version
        image: postgres:15.13-alpine3.20
        env:
          POSTGRES_DB: apex_test
          POSTGRES_USER: test
          POSTGRES_PASSWORD: test
        options: >-
          --health-cmd pg_isready
          --health-interval 10s
          --health-timeout 5s
          --health-retries 5
      redis:
        image: redis:6-alpine
        options: >-
          --health-cmd "redis-cli ping"
          --health-interval 10s
          --health-timeout 5s
          --health-retries 5
    steps:
      - uses: actions/checkout@v3
      - uses: actions/setup-java@v3
        with:
          java-version: '17'
      - run: mvn test -pl apex-core -Dtest="dev.mars.apex.core.service.**"
```

## Monitoring and Metrics

### Test Metrics Collection
- **Execution Time Tracking** - Per test class and method
- **Memory Usage Monitoring** - Heap and non-heap memory
- **Resource Utilization** - Database connections, cache usage
- **Error Rate Analysis** - Failure patterns and trends

### Reporting
- **JUnit Reports** - Standard test results
- **Coverage Reports** - JaCoCo code coverage
- **Performance Reports** - Execution time trends
- **Resource Reports** - Memory and connection usage

## Best Practices Summary

1. **Test Isolation** - Each test is independent
2. **Resource Cleanup** - Proper cleanup in @AfterEach
3. **Realistic Data** - Use representative test data
4. **Performance Awareness** - Monitor test execution times
5. **Error Scenarios** - Test both success and failure paths
6. **Documentation** - Keep test documentation updated
7. **Continuous Monitoring** - Track test health over time
