# APEX Service Layer Testing Methodology

**Last Updated:** 2026-03-19  
**Java Version:** 21+  
**JUnit:** 5.11.4  
**Testcontainers:** 2.0.2  
**Spring Boot:** 3.4.0

---

## Table of Contents

1. [Core Policies](#core-policies)
2. [Test Infrastructure](#test-infrastructure)
3. [Test Categories](#test-categories)
4. [Standard Test Pattern](#standard-test-pattern)
5. [Database Testing with Testcontainers 2.0](#database-testing-with-testcontainers-20)
6. [Error Handling Tests](#error-handling-tests)
7. [Test Data and YAML Configuration](#test-data-and-yaml-configuration)
8. [Cache and Resource Cleanup](#cache-and-resource-cleanup)
9. [Validation Checklist](#validation-checklist)
10. [Running Tests](#running-tests)

---

## Core Policies

### No Mocking — Real Services Only

APEX tests use real service instances exclusively. Mockito and all mocking frameworks are forbidden. Tests exercise the actual APEX engine, YAML deserialization, SpEL evaluation, enrichment processing, and data source integration.

**Rationale:** APEX is a configuration-driven engine where behavior emerges from the interaction of YAML configuration, SpEL expressions, and service orchestration. Mocking any layer hides the integration bugs that matter most.

### No Reflection in Tests

Reflection-based test techniques are forbidden. Do not use `getDeclaredMethod`, `setAccessible`, `Method.invoke`, `Field.set`, or similar reflective access to private/internal methods or fields.

Tests must validate behavior through public APIs and supported entry points (`RulesEngine`, service interfaces, YAML-driven execution paths). If a behavior is not testable through public contracts, redesign test coverage around externally observable outcomes rather than internal implementation details.

**Rationale:** Reflection-coupled tests are brittle under refactoring, break encapsulation boundaries, and create false failures when private signatures evolve without behavioral regressions.

### Two-Tier Error Logging

All production code follows a strict ERROR/DEBUG separation (see [APEX_TESTS_WITH_EXCEPTIONS.md](APEX_TESTS_WITH_EXCEPTIONS.md)):

- **ERROR level:** Clean one-line message with business context — no stack traces
- **DEBUG level:** Full exception detail with stack trace

Tests that deliberately trigger errors are marked with the `IntentionalError` or `IntentionalFailure` naming suffix and `INTENTIONAL ERROR TEST` log markers, so ERROR-level output is not mistaken for real failures.

### Error Propagation Through RuleResult

Exceptions must flow through the APEX architecture and be accessible via `RuleResult`:

```
Exception thrown
  → catch block logs ERROR (message) + DEBUG (stack trace)
  → wraps in DataSourceException / EnrichmentException
  → caught by RulesEngine / EnrichmentProcessor / TransformationProcessor
  → converted to RuleResult with ResultType.ERROR
  → RuleResult.hasFailures() == true
  → RuleResult.getFailureMessages() contains actionable detail
```

Tests must verify the **full propagation chain**, not just that no exception was thrown.

---

## Test Infrastructure

### DemoTestBase — Base Class for Demo Tests

All demo/integration tests extend `DemoTestBase`, which provides real APEX service instances and handles per-test isolation.

**Location:** `apex-demo/src/test/java/dev/mars/apex/demo/DemoTestBase.java`

**Protected fields:**

| Field | Type | Purpose |
|-------|------|---------|
| `yamlLoader` | `ConfigurationLoader` | Loads YAML test configurations |
| `serviceRegistry` | `LookupServiceRegistry` | Manages data source lookups |
| `expressionEvaluator` | `ExpressionEvaluatorService` | SpEL expression evaluation |
| `rulesEngineConfiguration` | `RulesEngineConfiguration` | Engine configuration |
| `logger` | `Logger` (static) | SLF4J logger |

**Setup (`@BeforeEach`):** Clears `ApexCacheManager` singleton, resets cache statistics, initializes fresh service instances.

**Teardown (`@AfterEach`):** Clears `JdbcTemplateFactory` cache, `DataSourceFactory` cache, `DataSourceRegistry` singleton, shuts down H2 databases, deletes H2 files, resets `ApexCacheManager` singleton.

### ColoredTestOutputExtension — Visual Test Feedback

**Location:** `apex-core/src/test/java/dev/mars/apex/core/test/extension/ColoredTestOutputExtension.java`

Implements `BeforeEachCallback`, `AfterEachCallback`, and `TestWatcher` to provide color-coded console output:

| Event | Output |
|-------|--------|
| Start | Blue bold `STARTING: ClassName.testName` |
| Pass | Green bold `[OK] PASSED: ClassName.testName` |
| Fail | Red bold `✗ FAILED: ClassName.testName` + error |
| Abort | Yellow bold `⚠ ABORTED: ClassName.testName` |

Static utility methods: `logInfo()`, `logSuccess()`, `logError()`.

### TestContainerImages — Docker Image Constants

**Location:** `apex-demo/src/test/java/dev/mars/apex/demo/util/TestContainerImages.java`

```java
public static final String POSTGRES = System.getProperty("docker.postgres.version",
    "postgres:15.13-alpine3.20");
public static final String VAULT = System.getProperty("docker.vault.version",
    "hashicorp/vault:1.20.0");
public static final String REDIS = System.getProperty("docker.redis.version",
    "redis:6-alpine");
```

Image versions are defined in the parent `pom.xml` and read via `System.getProperty()` with hardcoded fallbacks. Single source of truth for all container tests.

---

## Test Categories

### 1. Service Component Tests (apex-core)

Test individual service methods using real APEX services. No external dependencies.

- Fast execution (< 100ms per test)
- Use `@ExtendWith(ColoredTestOutputExtension.class)` directly
- YAML configurations embedded as string literals or loaded from `src/test/resources`

### 2. Demo Integration Tests (apex-demo)

Test end-to-end APEX processing: YAML loading → rule evaluation → enrichment → result extraction.

- Extend `DemoTestBase`
- Use `@ExtendWith(ColoredTestOutputExtension.class)`
- YAML files co-located with test classes
- Moderate execution time (1-10 seconds)

### 3. Database Integration Tests (apex-demo, apex-data-sync)

Test database lookups, enrichments, and schema operations with real PostgreSQL via Testcontainers.

- Extend `DemoTestBase`
- Use `GenericContainer` (Testcontainers 2.0) — not `PostgreSQLContainer`
- Docker required
- Execution time 5-30 seconds (container startup)

---

## Standard Test Pattern

Every demo test follows this structure:

```java
@ExtendWith(ColoredTestOutputExtension.class)
class MyFeatureTest extends DemoTestBase {

    @Test
    @DisplayName("Should enrich trade data with product details")
    void testProductEnrichment() {
        // 1. Load YAML configuration
        YamlRuleConfiguration config = yamlLoader.loadFromFile(
            "src/test/java/dev/mars/apex/demo/category/MyFeatureTest.yaml");
        assertNotNull(config);

        // 2. Create test data with meaningful business values
        Map<String, Object> testData = new HashMap<>();
        testData.put("productId", "PROD001");
        testData.put("tradeAmount", 5000000.0);

        // 3. Create engine and evaluate
        RulesEngine engine = RulesEngine.fromYamlConfig(config);
        RuleResult result = engine.evaluate(config, testData);

        // 4. Extract enriched data
        @SuppressWarnings("unchecked")
        Map<String, Object> enriched = (Map<String, Object>) result.getEnrichedData();

        // 5. Assert every enrichment result — no partial validation
        assertEquals("EUR/USD FX Forward", enriched.get("productName"));
        assertEquals("FX", enriched.get("assetClass"));
        assertEquals("USD", enriched.get("currency"));
    }
}
```

**One-liner for simple cases:**

```java
RuleResult result = RulesEngine.fromFile("config.yaml").evaluate(testData);
```

**Scenario-based tests:**

```java
RulesEngine engine = RulesEngine.fromScenarioRegistry("path/to/registry.yaml");
ScenarioExecutionResult result = engine.evaluateWithClassification(tradeData);
assertTrue(result.isSuccessful());
assertFalse(result.getStageResults().isEmpty());
```

---

## Database Testing with Testcontainers 2.0

Use `GenericContainer` (vendor-agnostic) — not the deprecated `PostgreSQLContainer`.

### Static Container Pattern (Shared Across All Tests)

```java
@Testcontainers
@ExtendWith(ColoredTestOutputExtension.class)
class PostgreSQLLookupTest extends DemoTestBase {

    private static final DockerImageName POSTGRES_IMAGE =
        DockerImageName.parse(TestContainerImages.POSTGRES);

    @Container
    static GenericContainer<?> postgres = new GenericContainer<>(POSTGRES_IMAGE)
            .withEnv("POSTGRES_DB", "apex_test")
            .withEnv("POSTGRES_USER", "apex_user")
            .withEnv("POSTGRES_PASSWORD", "apex_pass")
            .withExposedPorts(5432)
            .waitingFor(Wait.forListeningPort());

    // Manual JDBC URL construction — no getJdbcUrl() convenience method
    private static String jdbcUrl() {
        return "jdbc:postgresql://" + postgres.getHost() + ":"
            + postgres.getMappedPort(5432) + "/apex_test";
    }

    @BeforeAll
    static void setupDatabase() throws Exception {
        // Retry logic — Wait.forListeningPort() may return before PostgreSQL is ready
        int maxRetries = 3;
        Connection conn = null;
        for (int i = 0; i < maxRetries; i++) {
            try {
                conn = DriverManager.getConnection(jdbcUrl(), "apex_user", "apex_pass");
                break;
            } catch (Exception e) {
                if (i < maxRetries - 1) Thread.sleep(1000);
                else throw e;
            }
        }

        try (conn; Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE SCHEMA IF NOT EXISTS trading");
            stmt.execute("""
                CREATE TABLE trading.products (
                    product_id VARCHAR(20) PRIMARY KEY,
                    product_name VARCHAR(100) NOT NULL
                )
            """);
            stmt.execute("INSERT INTO trading.products VALUES ('PROD001', 'FX Forward')");
        }
    }
}
```

### Instance Container Pattern (Fresh Per Test)

Use an instance (non-static) `@Container` field when tests need complete database isolation:

```java
@Container  // Instance field, not static — new container per @Test
GenericContainer<?> postgres = new GenericContainer<>(POSTGRES_IMAGE)
        .withEnv("POSTGRES_DB", "apex_test")
        .withEnv("POSTGRES_USER", "apex_user")
        .withEnv("POSTGRES_PASSWORD", "apex_pass")
        .withExposedPorts(5432)
        .waitingFor(Wait.forListeningPort());
```

### Key Differences from Deprecated PostgreSQLContainer

| Feature | GenericContainer (current) | PostgreSQLContainer (deprecated) |
|---------|---------------------------|----------------------------------|
| Configuration | `.withEnv("POSTGRES_DB", "name")` | `.withDatabaseName("name")` |
| JDBC URL | Manual construction via helper | `postgres.getJdbcUrl()` |
| Port mapping | `getMappedPort(5432)` | `getFirstMappedPort()` |
| Credentials | Hard-coded in test | `getUsername()`, `getPassword()` |
| First URL param | `?` (clean URL) | `&` (existing params in URL) |

### Custom Schema Configuration

```java
// Append currentSchema to JDBC URL for custom schema support
String url = jdbcUrl() + "?currentSchema=trading";
```

See [APEX_DB_SCHEMA_CONFIG_TESTING.md](APEX_DB_SCHEMA_CONFIG_TESTING.md) for comprehensive schema testing patterns.

---

## Error Handling Tests

### Naming Convention

Tests that deliberately trigger errors use the suffix `IntentionalFailure` or `IntentionalError`:

```java
@Test
@DisplayName("Should handle connection failure gracefully")
void testConnectionFailureIntentional() {
    logger.info("=== INTENTIONAL ERROR TEST: Verifying connection failure handling ===");
    assertThrows(DataSourceException.class, () -> {
        engine.evaluate(config, testData);
    });
}
```

### RuleResult Error Verification

Tests exercising error paths must verify the full propagation chain — not just `assertNotNull(result)`:

```java
@Test
void testErrorPropagationIntentionalError() {
    logger.info("=== INTENTIONAL ERROR TEST: Verifying error propagation ===");

    YamlRuleConfiguration config = createConfigWithMissingDatasource();
    RuleResult result = engine.evaluate(config, inputData);

    // Verify error is captured in RuleResult
    assertNotNull(result);
    assertFalse(result.isSuccess());
    assertTrue(result.hasFailures());

    // Verify failure messages contain actionable detail
    List<String> failures = result.getFailureMessages();
    assertFalse(failures.isEmpty());
    assertTrue(failures.stream().anyMatch(msg -> msg.contains("missing-datasource")));

    // Verify ResultType
    assertEquals(RuleResult.ResultType.ERROR, result.getResultType());
}
```

### Anti-Pattern: Incomplete Error Verification

```java
// ❌ INCOMPLETE — verifies nothing about the error
RuleResult result = engine.evaluate(config, inputData);
assertNotNull(result);

// ✅ COMPLETE — verifies error state, messages, and result type
RuleResult result = engine.evaluate(config, inputData);
assertNotNull(result);
assertTrue(result.hasFailures());
assertFalse(result.getFailureMessages().isEmpty());
```

See [APEX_TESTS_WITH_EXCEPTIONS.md](APEX_TESTS_WITH_EXCEPTIONS.md) for the full audit of 49 exception-throwing tests and their classifications.

---

## Test Data and YAML Configuration

### YAML File Naming Convention

Test YAML files follow strict naming: `[TestClassName].yaml` or `[TestClassName]-[variant].yaml`

```
SimpleFieldLookupDemoTest.java      → SimpleFieldLookupDemoTest.yaml
ComponentScenarioTest.java          → ComponentScenarioTest-registry.yaml
                                    → ComponentScenarioTest-simple-component-scenario.yaml
FunctionMappingTypeDemoTest.java    → FunctionMappingTypeDemoTest.yaml
                                    → FunctionMappingTypeDemoTest-mixed.yaml
```

YAML files are co-located with their test classes in `src/test/java/dev/mars/apex/demo/[category]/`.

### Test Data Construction

Use meaningful business values that exercise the YAML conditions:

```java
Map<String, Object> tradeData = new HashMap<>();
tradeData.put("productId", "PROD001");
tradeData.put("tradeAmount", 5000000.0);
tradeData.put("currency", "USD");
tradeData.put("counterpartyId", "CP001");
```

### H2 In-Memory Databases

For tests that need a database without Docker, use H2 with unique database names per test class:

```java
private static final String DB_NAME = "simple_datasource_test";
Connection conn = DriverManager.getConnection(
    "jdbc:h2:./target/h2-demo/" + DB_NAME, "sa", "");
```

`DemoTestBase` handles H2 shutdown and file cleanup in teardown.

---

## Cache and Resource Cleanup

`DemoTestBase` executes a four-phase cleanup sequence in `@AfterEach` to ensure test isolation:

| Phase | Action | Purpose |
|-------|--------|---------|
| 1. Cache clear | `ApexCacheManager.getInstance().clearAll()` | Remove cached entries |
| 2. DataSource clear | `JdbcTemplateFactory.clearCache()`, `DataSourceFactory.getInstance().clearCache()`, `DataSourceRegistry.getInstance().clear()` | Release connections |
| 3. Database shutdown | H2 `SHUTDOWN` command + delete `db.mv.db`, `db.trace.db` files | Prevent file locking |
| 4. Singleton reset | `ApexCacheManager.resetInstance()` | Next test gets fresh instance |

**Order matters:** Clear entries first → shutdown connections → delete files → reset singletons.

---

## Validation Checklist

Every demo test should apply this checklist (established in `SimpleFieldLookupDemoTest`):

```
1. Count enrichments in YAML — X enrichments expected
2. Verify log shows "Processed: X out of X" — Must be 100% execution rate
3. Check EVERY enrichment condition — Test data triggers conditions
4. Validate EVERY business calculation — Test actual logic
5. Assert ALL enrichment results — Every field mapping has assertEquals
```

**No partial validation.** If the YAML has 5 enrichments, the test must assert all 5 outputs.

---

## Running Tests

### Prerequisites

- Java 21+
- Maven 3.8+
- Docker running (for Testcontainers-based tests)

### Commands

```bash
# All tests in a module
cd apex-demo
mvn test "-Djacoco.skip=true"

# Specific test class
mvn test -Dtest="SimpleFieldLookupDemoTest" -pl apex-demo "-Djacoco.skip=true"

# apex-core tests (no apex-core install needed)
mvn test -Dtest="FunctionMappingTypeTest" -pl apex-core "-Djacoco.skip=true"

# apex-demo tests (requires apex-core installed first)
mvn install -pl apex-core -DskipTests "-Djacoco.skip=true"
mvn test -Dtest="FunctionMappingTypeDemoTest" -pl apex-demo "-Djacoco.skip=true"

# Full build across all modules
mvn clean compile test-compile
```

**Note:** The `-Djacoco.skip=true` flag must be quoted in PowerShell: `"-Djacoco.skip=true"`. JaCoCo coverage checks can block `mvn install` if thresholds are not met.

---

## Related Documentation

- [APEX_DB_SCHEMA_CONFIG_TESTING.md](APEX_DB_SCHEMA_CONFIG_TESTING.md) — PostgreSQL custom schema testing
- [APEX_DB_TESTING_README.md](APEX_DB_TESTING_README.md) — Database test quick-start and TestContainers 2.0 migration
- [APEX_TESTS_WITH_EXCEPTIONS.md](APEX_TESTS_WITH_EXCEPTIONS.md) — Full audit of exception-throwing tests with classifications
