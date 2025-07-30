# APEX Rules Engine Testing Guide

This guide provides comprehensive information about testing the APEX Rules Engine, including unit tests, integration tests, and demonstration applications.

## Table of Contents

1. [Overview](#overview)
2. [Test Structure](#test-structure)
3. [PostgreSQL Integration Tests](#postgresql-integration-tests)
4. [YAML Dataset Tests](#yaml-dataset-tests)
5. [External Data Source Tests](#external-data-source-tests)
6. [Demo Applications](#demo-applications)
7. [Running Tests](#running-tests)
8. [Test Data Management](#test-data-management)

## Overview

The APEX Rules Engine includes comprehensive test coverage across all major components:

- **Unit Tests**: Test individual components in isolation
- **Integration Tests**: Test component interactions and real-world scenarios
- **Demo Applications**: Showcase features and provide learning examples
- **Performance Tests**: Validate performance characteristics

## Test Structure

```
apex-core/src/test/java/
├── dev/mars/apex/core/
│   ├── service/data/external/
│   │   ├── database/PostgreSQLIntegrationTest.java
│   │   └── ExternalDataSourceIntegrationTest.java
│   └── service/data/yaml/
│       └── YamlDatasetIntegrationTest.java
└── ...

apex-demo/src/main/java/
├── dev/mars/apex/demo/examples/
│   ├── DataManagementDemo.java
│   └── ExternalDataSourceDemo.java
└── ...
```

## PostgreSQL Integration Tests

### Overview

The `PostgreSQLIntegrationTest` class provides comprehensive testing of PostgreSQL database integration using TestContainers for realistic database scenarios.

### Features Tested

- **Database Connection Management**: Connection pooling, timeouts, SSL
- **Query Execution**: Parameterized queries, batch operations, transactions
- **Data Enrichment**: Complex JOIN queries, data transformation
- **Performance Monitoring**: Response times, connection metrics
- **Error Handling**: Connection failures, query errors, recovery

### Key Test Scenarios

```java
@Test
void testBasicDatabaseOperations() throws DataSourceException {
    // Test basic CRUD operations
    insertTestUsers();
    
    Map<String, Object> parameters = Map.of("email", "john@example.com");
    Object result = dataSource.queryForObject("getUserByEmail", parameters);
    
    assertNotNull(result);
    // Verify user data
}

@Test
void testComplexQueries() throws DataSourceException {
    // Test complex JOIN queries for data enrichment
    Map<String, Object> parameters = Map.of("email", "john@example.com");
    Object result = dataSource.queryForObject("getUserOrderSummary", parameters);
    
    assertNotNull(result);
    // Verify enriched data structure
}
```

### Database Schema

The tests use a comprehensive schema including:

- **Users Table**: Basic user information
- **Orders Table**: Order data with foreign keys
- **Currencies Table**: Reference data for enrichment
- **Performance Test Table**: Large dataset for performance testing

### Running PostgreSQL Tests

```bash
# Run all PostgreSQL integration tests
mvn test -Dtest=PostgreSQLIntegrationTest

# Run specific test method
mvn test -Dtest=PostgreSQLIntegrationTest#testComplexQueries
```

## YAML Dataset Tests

### Overview

The `YamlDatasetIntegrationTest` class covers all YAML dataset scenarios from the Data Management Guide, including nested objects, enrichment, and validation.

### Features Tested

- **Basic Dataset Structure**: Metadata, data sections, versioning
- **Simple Data Enrichment**: Lookup operations, field mapping
- **Complex Data Enrichment**: Multi-field enrichment, nested structures
- **Validation with Enriched Data**: Rule evaluation on enriched data
- **Multi-Dataset Scenarios**: Combining multiple data sources
- **Error Handling**: Default values, missing data handling

### Key Test Scenarios

```java
@Test
void testBasicDatasetStructure() throws IOException {
    // Test basic YAML dataset loading and parsing
    YamlDataset dataset = loadDatasetFromFile(datasetFile);
    
    assertEquals("Basic Currency Data", dataset.getName());
    assertEquals("1.0.0", dataset.getVersion());
    assertEquals(3, dataset.getData().size());
}

@Test
void testSimpleDataEnrichment() {
    // Test lookup enrichment with currency data
    YamlEnrichment enrichment = createCurrencyEnrichment();
    
    Map<String, Object> transaction = new HashMap<>();
    transaction.put("currency", "USD");
    
    Object result = enrichmentProcessor.processEnrichment(enrichment, transaction);
    
    // Verify enrichment was applied
    assertNotNull(result);
}
```

### YAML Configuration Examples

The tests demonstrate various YAML configuration patterns:

```yaml
# Basic Dataset Structure
metadata:
  name: "Currency Reference Data"
  version: "1.0.0"
  type: "dataset"

data:
  - code: "USD"
    name: "US Dollar"
    active: true
    region: "North America"
```

### Running YAML Dataset Tests

```bash
# Run all YAML dataset tests
mvn test -Dtest=YamlDatasetIntegrationTest

# Run specific enrichment tests
mvn test -Dtest=YamlDatasetIntegrationTest#testSimpleDataEnrichment
```

## External Data Source Tests

### Overview

The `ExternalDataSourceIntegrationTest` class provides comprehensive testing of external data source integration covering all scenarios from the External Data Sources Guide.

### Features Tested

- **REST API Integration**: HTTP connections, authentication, error handling
- **File System Integration**: CSV, JSON, XML file processing
- **Cache Integration**: TTL, eviction policies, performance
- **Circuit Breaker Patterns**: Failure detection, recovery
- **Health Monitoring**: Connection status, metrics collection
- **Performance Optimization**: Response times, throughput

### Key Test Scenarios

```java
@Test
void testRestApiDataSource() throws DataSourceException {
    // Test REST API integration
    DataSourceConfiguration config = createRestApiConfiguration();
    ExternalDataSource dataSource = factory.createDataSource(config);

    assertTrue(dataSource.testConnection());
    assertTrue(dataSource.isHealthy());
    
    // Test query execution
    Map<String, Object> parameters = Map.of("id", "123");
    List<Object> results = dataSource.query("getUserById", parameters);
    
    assertNotNull(results);
}

@Test
void testCacheIntegration() throws DataSourceException {
    // Test caching behavior
    DataSourceConfiguration config = createCachedCsvConfiguration(filePath);
    ExternalDataSource dataSource = factory.createDataSource(config);

    // First query - cache miss
    dataSource.query("findByName", parameters);
    
    // Second query - cache hit
    dataSource.query("findByName", parameters);
    
    // Verify cache metrics
    DataSourceMetrics metrics = dataSource.getMetrics();
    assertTrue(metrics.getCacheHitRate() > 0);
}
```

### Configuration Examples

The tests demonstrate various data source configurations:

```java
// REST API Configuration
DataSourceConfiguration config = new DataSourceConfiguration();
config.setName("test-rest-api");
config.setSourceType("rest-api");
config.setDataSourceType(DataSourceType.REST_API);

ConnectionConfig connectionConfig = new ConnectionConfig();
connectionConfig.setHost("api.example.com");
connectionConfig.setPort(443);
connectionConfig.setTimeout(30000);
config.setConnection(connectionConfig);
```

### Running External Data Source Tests

```bash
# Run all external data source tests
mvn test -Dtest=ExternalDataSourceIntegrationTest

# Run specific integration tests
mvn test -Dtest=ExternalDataSourceIntegrationTest#testCacheIntegration
```

## Demo Applications

### Data Management Demo

The `DataManagementDemo` class provides a comprehensive demonstration of data management capabilities:

- **Basic Dataset Structure**: Loading and parsing YAML datasets
- **Simple Enrichment**: Currency lookup and field mapping
- **Complex Enrichment**: Multi-field product enrichment
- **Nested Structures**: Hierarchical data handling
- **Multi-Dataset Scenarios**: Combining multiple data sources
- **Validation**: Rule evaluation with enriched data
- **Error Handling**: Default values and error recovery

### External Data Source Demo

The `ExternalDataSourceDemo` class showcases external data source integration:

- **REST API Integration**: HTTP connections and query execution
- **File System Integration**: CSV and JSON file processing
- **Cache Integration**: Performance optimization with caching
- **Health Monitoring**: Connection status and metrics
- **Error Handling**: Resilience patterns and recovery
- **Performance Metrics**: Response time and throughput analysis

### Running Demo Applications

```bash
# Run data management demo
java -cp apex-demo/target/classes dev.mars.apex.demo.examples.DataManagementDemo

# Run external data source demo
java -cp apex-demo/target/classes dev.mars.apex.demo.examples.ExternalDataSourceDemo
```

## Running Tests

### Prerequisites

- Java 17 or higher
- Maven 3.8 or higher
- Docker (for TestContainers)

### Running All Tests

```bash
# Run all tests
mvn test

# Run tests with coverage
mvn test jacoco:report

# Run integration tests only
mvn test -Dgroups=integration

# Run tests in parallel
mvn test -T 4
```

### Running Specific Test Categories

```bash
# Database integration tests
mvn test -Dtest=*DatabaseTest,*PostgreSQLTest

# YAML and configuration tests
mvn test -Dtest=*YamlTest,*ConfigurationTest

# External data source tests
mvn test -Dtest=*ExternalDataSourceTest,*DataSourceTest
```

### Test Profiles

```bash
# Fast tests (unit tests only)
mvn test -Pfast

# Full tests (including integration tests)
mvn test -Pfull

# Performance tests
mvn test -Pperformance
```

## Test Data Management

### Test Data Location

```
src/test/resources/
├── data/
│   ├── currencies.yaml
│   ├── products.json
│   ├── users.csv
│   └── test-schema.sql
├── config/
│   ├── test-datasources.yaml
│   └── test-rules.yaml
└── ...
```

### Creating Test Data

The tests use various approaches for test data:

1. **Inline Data**: Small datasets defined directly in test code
2. **Resource Files**: Larger datasets loaded from test resources
3. **Generated Data**: Programmatically created test data
4. **TestContainers**: Real databases with schema and data

### Test Data Best Practices

- **Isolation**: Each test uses independent data
- **Cleanup**: Tests clean up after themselves
- **Realistic Data**: Test data reflects real-world scenarios
- **Performance**: Large datasets for performance testing
- **Edge Cases**: Data that tests boundary conditions

## Conclusion

The APEX Rules Engine testing framework provides comprehensive coverage of all major components and integration scenarios. The combination of unit tests, integration tests, and demo applications ensures reliability, performance, and ease of use.

For more information about specific testing scenarios, refer to the individual test classes and their documentation.
