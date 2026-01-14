# APEX Database Gotchas & Learnings

> **Document Version**: 1.0  
> **Last Updated**: January 2026  
> **Context**: Lessons learned from debugging test failures across H2, PostgreSQL, and SQL Server

This document captures critical database compatibility issues and their solutions discovered during APEX development. These "gotchas" can save hours of debugging time.

---

## Table of Contents

1. [H2 Database with MODE=PostgreSQL](#h2-database-with-modepostgresql)
2. [H2 INFORMATION_SCHEMA System Tables](#h2-information_schema-system-tables)
3. [Schema Name Case Sensitivity](#schema-name-case-sensitivity)
4. [SQL Server JDBC Driver](#sql-server-jdbc-driver)
5. [SQL Server 2022 Encryption Requirements](#sql-server-2022-encryption-requirements)
6. [TestContainers Connection Patterns](#testcontainers-connection-patterns)
7. [PostgreSQL Views Support](#postgresql-views-support)
8. [Quick Reference Table](#quick-reference-table)

---

## H2 Database with MODE=PostgreSQL

### The Problem

When using H2 with `MODE=PostgreSQL`, unquoted SQL identifiers are stored in **UPPERCASE** (following SQL standard), but PostgreSQL stores them in **lowercase**.

```sql
-- This SQL in H2 MODE=PostgreSQL:
CREATE TABLE customers (id INT, name VARCHAR(100));

-- Creates a table named 'CUSTOMERS' (uppercase)
-- But the same SQL in PostgreSQL creates 'customers' (lowercase)
```

### Symptoms

- `Table "customers" not found` errors in H2
- Tests pass with real PostgreSQL but fail with H2
- Schema queries return empty results

### Solution

**Option 1: Always use uppercase table names in H2 queries**
```java
// In your Java code
String tableName = "CUSTOMERS";  // Uppercase for H2
jdbcTemplate.query("SELECT * FROM " + tableName, ...);
```

**Option 2: Use quoted identifiers (preserves case)**
```sql
CREATE TABLE "customers" (id INT, name VARCHAR(100));
-- This creates 'customers' (lowercase) even in H2
```

**Option 3: Ensure MODE=PostgreSQL is in the JDBC URL**
```yaml
connection:
  url: "jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;MODE=PostgreSQL"
```

### APEX Implementation

In `JdbcTemplateFactory.java`, we automatically append `MODE=PostgreSQL` to H2 URLs:
```java
private String buildJdbcUrl(ConnectionConfig conn) {
    if ("h2".equalsIgnoreCase(conn.getType())) {
        String url = conn.getUrl();
        if (!url.contains("MODE=")) {
            url += ";MODE=PostgreSQL";
        }
        return url;
    }
    // ...
}
```

---

## H2 INFORMATION_SCHEMA System Tables

### The Problem

H2's `INFORMATION_SCHEMA.COLUMNS` view includes system tables like `USERS` that don't exist in your schema. This can pollute schema queries.

### Symptoms

- Schema queries return unexpected columns like `USER_NAME`, `IS_ADMIN`, `REMARKS`
- Column counts don't match expected values
- Tests fail with "expected 4 columns but got 7"

### The Culprit

H2 has a system table `INFORMATION_SCHEMA.USERS` with columns:
- `USER_NAME`
- `IS_ADMIN`  
- `REMARKS`

If you query `INFORMATION_SCHEMA.COLUMNS` without filtering by schema, you get these system columns mixed in.

### Solution

**Always filter by TABLE_SCHEMA in INFORMATION_SCHEMA queries:**

```sql
-- BAD: No schema filter - includes system tables
SELECT COLUMN_NAME, DATA_TYPE 
FROM INFORMATION_SCHEMA.COLUMNS 
WHERE TABLE_NAME = 'CUSTOMERS';

-- GOOD: Filter by schema
SELECT COLUMN_NAME, DATA_TYPE 
FROM INFORMATION_SCHEMA.COLUMNS 
WHERE TABLE_NAME = 'CUSTOMERS' 
  AND TABLE_SCHEMA = 'PUBLIC';
```

### APEX Implementation

In `SchemaReaderService.java`:
```java
String query = """
    SELECT COLUMN_NAME, DATA_TYPE, IS_NULLABLE, ...
    FROM INFORMATION_SCHEMA.COLUMNS 
    WHERE TABLE_NAME = ? 
      AND LOWER(TABLE_SCHEMA) = LOWER(?)
    ORDER BY ORDINAL_POSITION
    """;
```

---

## Schema Name Case Sensitivity

### The Problem

Different databases handle schema names differently:

| Database   | Default Schema | Case Storage |
|------------|----------------|--------------|
| H2         | `PUBLIC`       | UPPERCASE    |
| PostgreSQL | `public`       | lowercase    |
| SQL Server | `dbo`          | As specified |

### Symptoms

- `WHERE TABLE_SCHEMA = 'PUBLIC'` works in H2 but fails in PostgreSQL
- `WHERE TABLE_SCHEMA = 'public'` works in PostgreSQL but fails in H2
- Tests pass on one database but fail on another

### Solution

**Use case-insensitive comparison:**

```sql
-- Works on both H2 and PostgreSQL
WHERE LOWER(TABLE_SCHEMA) = LOWER('PUBLIC')
```

### APEX Implementation

```java
// In SchemaReaderService.java
String schemaFilter = "LOWER(TABLE_SCHEMA) = LOWER('" + schemaName + "')";
```

---

## SQL Server JDBC Driver

### The Problem

SQL Server requires its own JDBC driver that's not included by default in most Java projects.

### Symptoms

- `ClassNotFoundException: com.microsoft.sqlserver.jdbc.SQLServerDriver`
- `No suitable driver found for jdbc:sqlserver://`
- DataSource creation fails silently

### Solution

**Add the SQL Server JDBC driver to your pom.xml:**

```xml
<dependency>
    <groupId>com.microsoft.sqlserver</groupId>
    <artifactId>mssql-jdbc</artifactId>
    <version>12.8.1.jre11</version>
    <scope>test</scope>
</dependency>
```

**Version Selection:**
- Use `jre11` suffix for Java 11+
- Use `jre8` suffix for Java 8
- Check [Maven Central](https://mvnrepository.com/artifact/com.microsoft.sqlserver/mssql-jdbc) for latest versions

---

## SQL Server 2022 Encryption Requirements

### The Problem

SQL Server 2022 (and Azure SQL) **requires encrypted connections by default**. The JDBC driver will fail to connect without proper encryption settings.

### Symptoms

- `The driver could not establish a secure connection to SQL Server`
- `SSL/TLS handshake failed`
- Connection works with older SQL Server versions but fails with 2022
- TestContainers SQL Server tests fail mysteriously

### Solution

**For development/testing (non-production):**

Add encryption parameters to your JDBC URL:
```
jdbc:sqlserver://localhost:1433;databaseName=mydb;encrypt=false;trustServerCertificate=true
```

**For production with self-signed certificates:**
```
jdbc:sqlserver://host:1433;databaseName=mydb;encrypt=true;trustServerCertificate=true
```

**For production with proper SSL:**
```
jdbc:sqlserver://host:1433;databaseName=mydb;encrypt=true;trustStore=/path/to/truststore;trustStorePassword=xxx
```

### APEX Implementation

In `JdbcTemplateFactory.buildJdbcUrl()`:
```java
case "sqlserver":
    return String.format(
        "jdbc:sqlserver://%s:%d;databaseName=%s;encrypt=false;trustServerCertificate=true",
        conn.getHost(), 
        conn.getPort(), 
        conn.getDatabase()
    );
```

### Important Notes

- `encrypt=false` disables TLS - **only use for local development/testing**
- `trustServerCertificate=true` bypasses certificate validation
- TestContainers provides SQL Server without SSL, so these settings are required

---

## TestContainers Connection Patterns

### The Problem

When using TestContainers, the container's host and port are **dynamically assigned** at runtime. You can't hardcode connection details in YAML files.

### Symptoms

- Tests fail with "Connection refused" 
- Tests fail with "Host not found"
- YAML-configured connections don't work with TestContainers
- Works with static databases but fails with containers

### The Wrong Approach

❌ **Don't use System properties in YAML:**
```yaml
# This is fragile and hard to debug
connection:
  host: "${sys:SQLSERVER_HOST}"
  port: "${sys:SQLSERVER_PORT}"
```

### The Correct Approach (Proven Pattern)

✅ **Load YAML, then programmatically update connection details:**

```java
@Test
void testWithContainers() {
    // 1. Start containers
    PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15");
    postgres.start();
    
    // 2. Load YAML configuration
    YamlConfigurationLoader loader = new YamlConfigurationLoader();
    YamlRuleConfiguration config = loader.loadFromFile("test-config.yaml");
    
    // 3. Programmatically update connection details
    Map<String, Object> connection = getConnectionMap(config, "my-datasource");
    connection.put("host", postgres.getHost());
    connection.put("port", postgres.getMappedPort(5432));
    connection.put("database", postgres.getDatabaseName());
    connection.put("username", postgres.getUsername());
    connection.put("password", postgres.getPassword());
    
    // 4. Create engine with modified config
    RulesEngine engine = RulesEngine.fromYamlConfig(config);
    
    // 5. Run test
    engine.evaluate(data);
}

@SuppressWarnings("unchecked")
private Map<String, Object> getConnectionMap(YamlRuleConfiguration config, String sourceName) {
    List<Map<String, Object>> sources = (List<Map<String, Object>>) config.getDataSources();
    for (Map<String, Object> source : sources) {
        if (sourceName.equals(source.get("name"))) {
            return (Map<String, Object>) source.get("connection");
        }
    }
    throw new IllegalArgumentException("Data source not found: " + sourceName);
}
```

### Reference Implementation

See `PostgreSQLSimpleLookupTest` in `apex-demo` for the canonical example of this pattern.

---

## Quick Reference Table

| Issue | Database | Solution |
|-------|----------|----------|
| Table not found (case) | H2 | Use uppercase table names or `MODE=PostgreSQL` |
| Extra system columns | H2 | Add `TABLE_SCHEMA` filter to INFORMATION_SCHEMA queries |
| Schema name mismatch | H2/PostgreSQL | Use `LOWER(TABLE_SCHEMA) = LOWER('xxx')` |
| Missing JDBC driver | SQL Server | Add `mssql-jdbc` dependency |
| Encryption required | SQL Server 2022 | Add `encrypt=false;trustServerCertificate=true` to URL |
| Dynamic container ports | TestContainers | Load YAML, then use `connection.put()` to update |

---

## Debugging Checklist

When database tests fail, check these in order:

1. **Is the JDBC driver on the classpath?**
   - Check `pom.xml` for the appropriate driver dependency

2. **Is the JDBC URL correct?**
   - Add logging to show the actual URL being used
   - Check for required parameters (MODE, encrypt, etc.)

3. **Is the table name case correct?**
   - H2 with MODE=PostgreSQL uses UPPERCASE
   - Real PostgreSQL uses lowercase

4. **Are you filtering by schema?**
   - Always include `TABLE_SCHEMA` in INFORMATION_SCHEMA queries

5. **For TestContainers: Are you updating connection details programmatically?**
   - Don't rely on YAML placeholders
   - Use the proven `connection.put()` pattern

6. **Are you querying a VIEW instead of a TABLE?**
   - APEX now includes both `BASE TABLE` and `VIEW` in schema enumeration
   - See [PostgreSQL Views Support](#postgresql-views-support) section

---

## PostgreSQL Views Support

### The Problem

When querying PostgreSQL views, users may encounter "view not found" errors. This occurs because schema enumeration queries may only look for `BASE TABLE` types and exclude `VIEW` types.

### Symptoms

- `View not found` or `Table not found` errors when querying a view
- Schema enumeration returns only tables, not views
- Direct SQL queries against views work, but APEX enrichments fail

### Solution

APEX's `SchemaReaderService` now includes both tables AND views in schema queries:

```sql
-- Old query (tables only):
SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_TYPE = 'BASE TABLE'

-- New query (tables AND views):
SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_TYPE IN ('BASE TABLE', 'VIEW')
```

### Key Points

1. **Views in INFORMATION_SCHEMA**: In PostgreSQL, views have `TABLE_TYPE = 'VIEW'` in `INFORMATION_SCHEMA.TABLES`
2. **Column metadata works the same**: `INFORMATION_SCHEMA.COLUMNS` contains column information for both tables and views
3. **Schema qualification**: Always use `schema.view_name` for views in non-default schemas

### Example YAML Configuration for Views

```yaml
data-sources:
  - name: "mydb"
    type: "database"
    source-type: "postgresql"
    connection:
      host: "localhost"
      port: 5432
      database: "mydb"
      schema: "reporting"  # Schema where the view exists

enrichments:
  - id: "view-lookup"
    lookup-config:
      lookup-dataset:
        type: "database"
        data-source-ref: "mydb"
        # Query the view with schema qualification
        query: "SELECT * FROM reporting.customer_summary_view WHERE customer_id = :customerId"
```

---

## Related Documentation

- [APEX H2 Database Usage Guide](APEX_H2_DATABASE_USAGE_GUIDE.md)
- [APEX Lookup Configuration Guide](APEX_LOOKUP_CONFIGURATION_GUIDE.md)
- [APEX Technical Reference](APEX_TECHNICAL_REFERENCE.md)

---

*This document should be updated whenever new database compatibility issues are discovered.*
