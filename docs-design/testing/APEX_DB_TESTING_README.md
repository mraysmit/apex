# PostgreSQL Schema Configuration Tests

This folder contains comprehensive tests validating APEX's PostgreSQL schema configuration functionality.

## Overview

APEX enables PostgreSQL queries to execute against custom schemas (e.g., `trading`, `sales`, `inventory`) without hardcoding schema prefixes in SQL queries. The schema is configured declaratively in YAML, and APEX automatically sets the PostgreSQL `currentSchema` JDBC parameter.

## Test Suites

### 1. PostgreSQLSchemaConfigurationTest.java
**Purpose**: Validates core schema configuration functionality

**Tests**:
- Schema parameter is read from YAML configuration
- Queries execute against configured schema without prefixes
- Multiple schemas are supported via multiple data-sources
- Schema parameter prevents defaulting to `public` schema

**Key Pattern**:
```yaml
data-sources:
  - name: "trading-database"
    connection:
      schema: "trading"  # ← Sets PostgreSQL currentSchema parameter

queries:
  getProduct: "SELECT * FROM products"  # ← No schema prefix needed
```

### 2. JdbcUrlSchemaParameterTest.java
**Purpose**: Low-level JDBC URL validation

**Tests**:
- JDBC URL includes `?currentSchema=<schema>` parameter
- PostgreSQL `search_path` is correctly configured
- Different `currentSchema` values query different schemas
- Schema isolation prevents accidental public schema queries

**Implementation Reference**: See `JdbcTemplateFactory.java` lines 237-241

### 3. EnvironmentPromotionTest.java
**Purpose**: Validates zero-code-change environment promotion pattern

**Tests**:
- Same YAML files work across DEV/UAT/PROD environments
- Connection details are updated externally (not in YAML)
- Schema names differ per environment without code changes
- Full DEV → UAT → PROD promotion workflow

**Promotion Pattern**:
- **DEV**: `dev_trading` schema
- **UAT**: `uat_trading` schema  
- **PROD**: `trading` schema (no prefix)

## TestContainers 2.0 Pattern - GenericContainer

All tests migrated to **TestContainers 2.0** using `GenericContainer` (vendor-agnostic) instead of deprecated `PostgreSQLContainer`.

### Modern Pattern (TestContainers 2.0+)

```java
@Testcontainers
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class PostgreSQLSchemaConfigurationTest {
    
    private static final DockerImageName POSTGRES_IMAGE = 
        DockerImageName.parse(TestContainerImages.POSTGRES)
                       .asCompatibleSubstituteFor("postgres");
    
    // Static container = shared across all tests (faster, but less isolation)
    @Container
    static GenericContainer<?> postgres = new GenericContainer<>(POSTGRES_IMAGE)
            .withEnv("POSTGRES_DB", "apex_test")
            .withEnv("POSTGRES_USER", "apex_user")
            .withEnv("POSTGRES_PASSWORD", "apex_pass")
            .withExposedPorts(5432)
            .waitingFor(Wait.forListeningPort());
    
    // Manual JDBC URL construction (no convenience methods)
    private static String jdbcUrl() {
        return "jdbc:postgresql://" + postgres.getHost() + ":" 
            + postgres.getMappedPort(5432) + "/apex_test";
    }

    @BeforeAll
    static void setupDatabase() throws Exception {
        if (!postgres.isRunning()) {
            return;
        }
        
        // Setup using manual JDBC URL construction
        try (Connection conn = DriverManager.getConnection(
                jdbcUrl(), "apex_user", "apex_pass")) {
            // Setup database schema and test data
        }
    }
    
    private void updateDataSourceConnection(YamlRuleConfiguration config, String dataSourceName) {
        // Update YAML config with GenericContainer connection details
        String host = postgres.getHost();
        Integer port = postgres.getMappedPort(5432);  // Not getFirstMappedPort()
        String username = "apex_user";  // Hard-coded (no getUsername() method)
        String password = "apex_pass";  // Hard-coded (no getPassword() method)
        // ... set connection parameters
    }
}
```

### Instance Container Pattern (Per-Test Isolation)

For tests requiring complete isolation between test methods:

```java
class JdbcUrlSchemaParameterTest {
    
    // Instance variable = new container for each @Test method
    @Container
    GenericContainer<?> postgres = new GenericContainer<>(POSTGRES_IMAGE)
            .withEnv("POSTGRES_DB", "apex_jdbc_test")
            .withEnv("POSTGRES_USER", "apex_user")
            .withEnv("POSTGRES_PASSWORD", "apex_pass")
            .withExposedPorts(5432)
            .waitingFor(Wait.forListeningPort());
    
    @BeforeEach  // Runs before EACH test (not @BeforeAll)
    void setupTestSchemas() throws Exception {
        // Fresh database setup for every test method
        // Add retry logic for PostgreSQL startup timing
        int maxRetries = 3;
        Connection conn = null;
        for (int i = 0; i < maxRetries; i++) {
            try {
                conn = DriverManager.getConnection(jdbcUrl(), "apex_user", "apex_pass");
                break;
            } catch (Exception e) {
                if (i < maxRetries - 1) {
                    Thread.sleep(1000);  // Wait for PostgreSQL to fully initialize
                } else {
                    throw e;
                }
            }
        }
    }
}
```

### Key Differences: GenericContainer vs PostgreSQLContainer

| Feature | PostgreSQLContainer (Deprecated) | GenericContainer (Modern) |
|---------|----------------------------------|---------------------------|
| **Configuration** | Convenience methods (`.withDatabaseName()`) | Environment variables (`.withEnv()`) |
| **JDBC URL** | `postgres.getJdbcUrl()` returns URL with params | Manual construction via `jdbcUrl()` helper |
| **Credentials** | `postgres.getUsername()`, `getPassword()` | Hard-coded in test code |
| **Port Mapping** | `postgres.getFirstMappedPort()` | `postgres.getMappedPort(5432)` |
| **URL Parameters** | First param uses `&` (existing params in URL) | First param uses `?` (clean URL) |
| **Vendor Lock-in** | PostgreSQL-specific API | Vendor-agnostic, works with any DB |
| **Future-proof** | Deprecated in Testcontainers 1.20+ | Recommended pattern going forward |

### Migration Notes

**JDBC URL Parameter Syntax Change**:
```java
// OLD (PostgreSQLContainer): URL had existing params, use &
String url = postgres.getJdbcUrl() + "&currentSchema=trading";
// Result: jdbc:postgresql://...?loggerLevel=OFF&currentSchema=trading

// NEW (GenericContainer): Clean URL, first param uses ?
String url = jdbcUrl() + "?currentSchema=trading";
// Result: jdbc:postgresql://localhost:12345/apex_test?currentSchema=trading
```

**Startup Timing Resilience**:
- GenericContainer with `Wait.forListeningPort()` may return before PostgreSQL is fully initialized
- Add retry logic in `@BeforeEach`/`@BeforeAll` setup methods (3 attempts, 1-second delays)
- See `JdbcUrlSchemaParameterTest.setupTestSchemas()` for reference implementation

## Running Tests

```bash
# Run all schema tests
cd apex-core
mvn test -Dtest="PostgreSQLSchemaConfigurationTest,JdbcUrlSchemaParameterTest,EnvironmentPromotionTest"

# Run single test suite
mvn test -Dtest="PostgreSQLSchemaConfigurationTest"
```

## YAML Configuration Structure

### Business Logic Configuration
```yaml
metadata:
  id: "enrichment-config"

data-source-refs:
  - name: "trading-database"
    source: "data-sources/trading-database.yaml"
    enabled: true

enrichments:
  - id: "product-lookup"
    lookup-config:
      lookup-dataset:
        data-source-ref: "trading-database"
        query-ref: "getProduct"
```

### External Data-Source Configuration
```yaml
metadata:
  type: "external-data-config"

name: "trading-database"
type: "database"
source-type: "postgresql"

connection:
  host: "localhost"
  port: 5432
  database: "apex_db"
  username: "user"
  password: "pass"
  schema: "trading"  # ← PostgreSQL schema configuration

queries:
  getProduct: |
    SELECT * FROM products WHERE id = :id
```

## Critical Success Criteria

✅ Schema parameter applied from YAML to JDBC URL  
✅ Queries execute without schema prefix  
✅ Multiple schemas supported simultaneously  
✅ Schema isolation prevents public schema fallback  
✅ JDBC URL includes `?currentSchema=<schema>`  
✅ PostgreSQL `search_path` correctly configured  
✅ Environment promotion works without code changes

## Design Documentation

See `docs-design/postgresql-schema-configuration-testing.md` for comprehensive architecture documentation.

## References

- **Implementation**: `apex-core/src/main/java/.../JdbcTemplateFactory.java`
- **Demo Tests**: `apex-demo/src/test/java/.../CustomSchemaEnrichmentTest.java`
- **Data Sync Tests**: `apex-data-sync/src/test/java/.../CustomSchemaPostgresTest.java`
