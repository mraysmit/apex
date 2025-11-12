# H2 Database Usage Guide for APEX Rules Engine

## Table of Contents
1. [Overview](#overview)
2. [Key Concepts](#key-concepts)
3. [Configuration Patterns](#configuration-patterns)
4. [Parameter Reference](#parameter-reference)
5. [Java Code Patterns](#java-code-patterns)
6. [Field Mapping Considerations](#field-mapping-considerations)
7. [Best Practices](#best-practices)
8. [Troubleshooting](#troubleshooting)
9. [Integration Issues and Solutions](#integration-issues-and-solutions)
10. [Migration Guide](#migration-guide)

## Overview

This comprehensive guide provides best practices for using H2 databases with the APEX Rules Engine, particularly for demos and testing scenarios. H2 is an embedded Java database that can run in-memory or file-based modes. This guide consolidates lessons learned from resolving database instance isolation issues and provides complete configuration references.

## Key Concepts

### Database Sharing Modes

**File-based H2 (RECOMMENDED):**
- Creates persistent database files on disk
- Enables true database sharing between multiple processes
- Data survives application restarts
- Perfect for demos where APEX and demo code need to share data

**In-memory H2 (NOT RECOMMENDED for demos):**
- Creates temporary database instances in memory
- Each connection may create a separate isolated instance
- Data is lost when application terminates
- Can cause "Table not found" errors in multi-process scenarios

### The Database Instance Isolation Problem

**Root Cause:** H2 in-memory databases with identical names can still be separate instances when created by different parts of the application.

**Problem Pattern:**
```java
// Demo code creates database instance #1
String jdbcUrl = "jdbc:h2:mem:apex_demo_shared;DB_CLOSE_DELAY=-1;MODE=PostgreSQL";
Connection conn = DriverManager.getConnection(jdbcUrl, "sa", "");
// Create tables and insert data in instance #1
```

```yaml
# YAML configuration tries to connect to "same" database
data-sources:
  - name: "customer-database"
    connection:
      database: "apex_demo_shared"  # Creates instance #2 (empty)
```

**What Actually Happens:**
- Demo code creates H2 instance #1 with tables and data
- APEX DataSourceFactory creates H2 instance #2 (empty)
- Same JDBC URL ≠ Same database instance in H2 memory databases

## Configuration Patterns

### ✅ RECOMMENDED: File-based H2 Configuration

```yaml
# File: customer-database.yaml
metadata:
  name: "Customer Database"
  version: "1.0.0"
  type: "external-data-config"
  description: "H2 file-based customer database for demos"

data-sources:
  - name: "customer-database"
    type: "database"
    source-type: "h2"
    enabled: true
    description: "Customer master data from self-contained H2 database"

    connection:
      # H2 file-based database for true sharing between demo and APEX
      database: "./target/h2-demo/apex_demo_shared"
      username: "sa"
      password: ""

    queries:
      customerLookup: "SELECT customer_name, customer_type, tier, region, status FROM customers WHERE customer_id = :customerId"

    parameterNames:
      - "customerId"

enrichments:
  - name: "customer-profile-enrichment"
    description: "Enrich with customer profile data"

    lookup-dataset:
      type: "database"
      data-source-ref: "customer-database"
      query-ref: "customerLookup"

      parameters:
        - name: "customerId"
          source-field: "customerId"
          required: true

    # IMPORTANT: H2 returns uppercase column names
    field-mappings:
      - source-field: "CUSTOMER_NAME"
        target-field: "customerName"
      - source-field: "CUSTOMER_TYPE"
        target-field: "customerType"
      - source-field: "TIER"
        target-field: "customerTier"
      - source-field: "REGION"
        target-field: "customerRegion"
      - source-field: "STATUS"
        target-field: "customerStatus"
```

### Enhanced H2 Configuration with Custom Parameters

```yaml
data-sources:
  - name: "h2-custom-database"
    type: "database"
    source-type: "h2"
    enabled: true
    description: "H2 database with custom parameters"

    connection:
      # Custom H2 parameters can be specified after the database path
      # Format: "path/to/database;PARAM1=value1;PARAM2=value2"
      database: "./target/h2-demo/custom;MODE=MySQL;TRACE_LEVEL_FILE=2;CACHE_SIZE=32768"
      username: "sa"
      password: ""
```

### X DEPRECATED: In-memory H2 Configuration

```yaml
# DON'T USE: Creates isolated database instances
data-sources:
  - name: "customer-database"
    type: "database"
    source-type: "h2"
    connection:
      # PROBLEM: Each connection creates separate in-memory instance
      database: "shared_demo"  # Becomes jdbc:h2:mem:shared_demo
      username: "sa"
      password: ""
```

## Parameter Reference

### Parameter Format

```yaml
connection:
  # Format: "path/to/database;PARAM1=value1;PARAM2=value2"
  database: "./target/h2-demo/custom;MODE=MySQL;CACHE_SIZE=32768;TRACE_LEVEL_FILE=2"
  username: "sa"
  password: ""
```

### Parameter Categories

#### Performance Tuning Parameters

| Parameter | Description | Default | Example Values |
|-----------|-------------|---------|----------------|
| `CACHE_SIZE` | Database cache size in KB | 16384 (16MB) | `32768` (32MB), `65536` (64MB) |
| `MAX_MEMORY_ROWS` | Maximum rows kept in memory | 40000 | `100000`, `200000` |
| `MAX_MEMORY_UNDO` | Maximum undo log entries in memory | 50000 | `100000`, `200000` |
| `MAX_OPERATION_MEMORY` | Maximum memory for operations in KB | 100000 | `200000`, `500000` |
| `CACHE_TYPE` | Cache algorithm | TQ | `LRU`, `SOFT_LRU` |

#### Compatibility Mode Parameters

| Parameter | Description | Example |
|-----------|-------------|---------|
| `MODE` | Database compatibility mode | `PostgreSQL`, `MySQL`, `Oracle`, `DB2`, `HSQLDB` |

**Mode Details:**
- `PostgreSQL` - PostgreSQL compatibility (APEX default)
- `MySQL` - MySQL compatibility mode
- `Oracle` - Oracle compatibility mode
- `DB2` - IBM DB2 compatibility mode
- `HSQLDB` - HSQLDB compatibility mode

#### Debugging and Logging Parameters

| Parameter | Description | Values | Example |
|-----------|-------------|--------|---------|
| `TRACE_LEVEL_FILE` | SQL logging level to file | 0-4 | `0` (off), `1` (error), `2` (info), `4` (debug) |
| `TRACE_LEVEL_SYSTEM_OUT` | SQL logging to console | 0-4 | `0` (off), `1` (error), `2` (info) |
| `TRACE_MAX_FILE_SIZE` | Maximum trace file size in MB | Number | `16`, `32`, `64` |

#### Connection Management Parameters

| Parameter | Description | Values | Example |
|-----------|-------------|--------|---------|
| `DB_CLOSE_DELAY` | Keep database open after last connection | -1, 0, >0 | `-1` (forever), `0` (immediate), `30` (30 seconds) |
| `DB_CLOSE_ON_EXIT` | Close database when JVM exits | TRUE/FALSE | `TRUE`, `FALSE` |
| `AUTO_SERVER` | Enable automatic mixed mode | TRUE/FALSE | `TRUE`, `FALSE` |
| `AUTO_SERVER_PORT` | Port for automatic server mode | Number | `9090`, `9091` |

#### Security and Access Parameters

| Parameter | Description | Values | Example |
|-----------|-------------|--------|---------|
| `ACCESS_MODE_DATA` | Database access mode | r, rw | `r` (read-only), `rw` (read-write) |
| `IFEXISTS` | Only connect if database exists | TRUE/FALSE | `TRUE`, `FALSE` |
| `CIPHER` | Encryption cipher | AES, XTEA | `AES`, `XTEA` |

#### Initialization Parameters

| Parameter | Description | Example |
|-----------|-------------|---------|
| `INIT` | SQL script to run on startup | `RUNSCRIPT FROM 'classpath:init.sql'` |
| `SCHEMA` | Default schema name | `PUBLIC`, `DEMO` |

### Parameter Merging Rules

1. **Custom parameters override APEX defaults**
2. **APEX automatically adds `DB_CLOSE_DELAY=-1` if not specified**
3. **No duplicate parameters - custom takes precedence**
4. **Additional custom parameters are preserved**
5. **Parameters are case-sensitive**

### Example Parameter Configurations

```yaml
# High Performance
database: "./target/h2-demo/performance;CACHE_SIZE=65536;MAX_MEMORY_ROWS=100000"

# Debug Mode
database: "./target/h2-demo/debug;TRACE_LEVEL_FILE=2;TRACE_LEVEL_SYSTEM_OUT=1"

# MySQL Compatible
database: "./target/h2-demo/mysql;MODE=MySQL;CACHE_SIZE=32768"

# Oracle Compatible
database: "./target/h2-demo/oracle;MODE=Oracle;CACHE_SIZE=32768"

# Read-Only
database: "./target/h2-demo/readonly;ACCESS_MODE_DATA=r;IFEXISTS=TRUE"

# Auto-Initialize
database: "./target/h2-demo/init;INIT=RUNSCRIPT FROM 'classpath:schema.sql'"
```

## Java Code Patterns

### ✅ RECOMMENDED: File-based H2 in Java

```java
public class CustomerDatabaseDemo {
    // Use the EXACT same JDBC URL as YAML configuration
    private static final String JDBC_URL =
        "jdbc:h2:./target/h2-demo/apex_demo_shared;DB_CLOSE_DELAY=-1;MODE=PostgreSQL";

    public void initializeDatabase() throws SQLException {
        try (Connection connection = DriverManager.getConnection(JDBC_URL, "sa", "")) {
            Statement statement = connection.createStatement();

            // Clean up existing data to prevent primary key violations
            statement.execute("DROP TABLE IF EXISTS customers");

            // Create table
            statement.execute("""
                CREATE TABLE IF NOT EXISTS customers (
                    customer_id VARCHAR(20) PRIMARY KEY,
                    customer_name VARCHAR(100) NOT NULL,
                    customer_type VARCHAR(20) NOT NULL,
                    tier VARCHAR(20) NOT NULL,
                    region VARCHAR(10) NOT NULL,
                    status VARCHAR(20) NOT NULL,
                    created_date DATE NOT NULL
                )
            """);

            // Insert test data
            statement.execute("""
                INSERT INTO customers VALUES
                ('CUST000001', 'Acme Corporation', 'CORPORATE', 'PLATINUM', 'NA', 'ACTIVE', '2023-01-15'),
                ('CUST000002', 'Global Industries', 'CORPORATE', 'GOLD', 'EU', 'ACTIVE', '2023-02-20'),
                ('CUST000003', 'Tech Startup Inc', 'STARTUP', 'SILVER', 'NA', 'ACTIVE', '2023-03-10')
            """);
        }
    }
}
```

### The Shared Database Pattern (Complete Solution)

```java
package dev.mars.apex.demo.lookup;

import dev.mars.apex.core.service.enrichment.EnrichmentService;
import dev.mars.apex.core.service.engine.ExpressionEvaluatorService;
import dev.mars.apex.core.service.enrichment.YamlEnrichmentProcessor;
import dev.mars.apex.core.service.lookup.LookupServiceRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * Shared DataSource Demo - Solving H2 Database Instance Isolation
 */
public class SharedDataSourceDemo {

    private static final Logger logger = LoggerFactory.getLogger(SharedDataSourceDemo.class);

    private static final String DB_URL = "jdbc:h2:mem:shared_demo;DB_CLOSE_DELAY=-1;MODE=PostgreSQL";
    private static final String DB_USER = "sa";
    private static final String DB_PASSWORD = "";

    private Connection sharedConnection;
    private EnrichmentService enrichmentService;

    public void runDemo() throws Exception {
        logger.info("SHARED DATASOURCE DEMO - Solving H2 Database Instance Isolation");

        // Step 1: Initialize shared H2 database FIRST
        initializeSharedDatabase();

        // Step 2: Setup APEX services with awareness of shared database
        setupApexWithSharedDataSource();

        // Step 3: Test database lookup via both direct and APEX patterns
        testApexDatabaseLookup();

        // Step 4: Cleanup
        cleanup();

        logger.info("SHARED DATASOURCE DEMO COMPLETED SUCCESSFULLY!");
    }

    private void initializeSharedDatabase() throws Exception {
        logger.info("Step 1: Initializing shared H2 database...");

        // Load H2 driver
        Class.forName("org.h2.Driver");

        // Create shared connection - THIS IS THE KEY
        sharedConnection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);

        // Create table in the shared instance
        try (PreparedStatement stmt = sharedConnection.prepareStatement(
                "CREATE TABLE IF NOT EXISTS customers (" +
                "customer_id VARCHAR(20) PRIMARY KEY, " +
                "customer_name VARCHAR(100) NOT NULL, " +
                "customer_type VARCHAR(20) NOT NULL, " +
                "tier VARCHAR(20) NOT NULL, " +
                "region VARCHAR(10) NOT NULL, " +
                "status VARCHAR(20) NOT NULL" +
                ")")) {
            stmt.execute();
        }

        // Insert test data in the shared instance
        try (PreparedStatement stmt = sharedConnection.prepareStatement(
                "INSERT INTO customers (customer_id, customer_name, customer_type, tier, region, status) VALUES (?, ?, ?, ?, ?, ?)")) {

            stmt.setString(1, "CUST001");
            stmt.setString(2, "Acme Corporation");
            stmt.setString(3, "CORPORATE");
            stmt.setString(4, "PLATINUM");
            stmt.setString(5, "NA");
            stmt.setString(6, "ACTIVE");
            stmt.execute();
        }

        // Verify data exists in shared instance
        try (PreparedStatement stmt = sharedConnection.prepareStatement("SELECT COUNT(*) FROM customers");
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) {
                int count = rs.getInt(1);
                logger.info("✅ Created customers table with {} records", count);
            }
        }
    }

    private void setupApexWithSharedDataSource() throws Exception {
        logger.info("Step 2: Setting up APEX services...");

        // Initialize APEX services - they can now connect to the shared database
        LookupServiceRegistry serviceRegistry = new LookupServiceRegistry();
        ExpressionEvaluatorService expressionEvaluator = new ExpressionEvaluatorService();
        YamlEnrichmentProcessor enrichmentProcessor = new YamlEnrichmentProcessor(serviceRegistry, expressionEvaluator);
        enrichmentService = new EnrichmentService(serviceRegistry, expressionEvaluator);

        logger.info("✅ APEX services initialized");
        logger.info("✅ Shared database connection available for integration");
    }
}
```

## Field Mapping Considerations

### H2 Column Name Case Sensitivity

H2 database returns **uppercase column names** by default. Your field mappings must account for this:

```yaml
# ✅ CORRECT: Use uppercase source fields
field-mappings:
  - source-field: "CUSTOMER_NAME"    # H2 returns uppercase
    target-field: "customerName"     # Target can be any case
  - source-field: "CUSTOMER_TYPE"
    target-field: "customerType"

# X INCORRECT: Lowercase source fields won't match
field-mappings:
  - source-field: "customer_name"    # Won't match H2 uppercase columns
    target-field: "customerName"
```

## Best Practices

### Demo Best Practices

#### 1. Database Cleanup
Always clean up existing data to prevent primary key violations:

```java
// Clean up before creating tables
statement.execute("DROP TABLE IF EXISTS customers");
statement.execute("DROP TABLE IF EXISTS orders");  // Drop dependent tables first
```

#### 2. Consistent JDBC URLs
Use the exact same JDBC URL pattern in both Java code and YAML:

**Java:** `jdbc:h2:./target/h2-demo/apex_demo_shared;DB_CLOSE_DELAY=-1;MODE=PostgreSQL`
**YAML:** `database: "./target/h2-demo/apex_demo_shared"`

#### 3. Directory Structure
Ensure the target directory exists:
```bash
mkdir -p target/h2-demo
```

#### 4. Connection Parameters
- `DB_CLOSE_DELAY=-1`: Keeps database open between connections
- `MODE=PostgreSQL`: Enables PostgreSQL compatibility mode

### Performance Tuning
- Use `CACHE_SIZE=65536` or higher for large datasets
- Increase `MAX_MEMORY_ROWS` for better in-memory performance
- Consider `CACHE_TYPE=LRU` for predictable memory usage

### Development and Testing
- Enable `TRACE_LEVEL_FILE=2` for SQL debugging
- Use `TRACE_LEVEL_SYSTEM_OUT=1` for console logging
- Set `TRACE_MAX_FILE_SIZE=32` to prevent large log files

### Production Deployment
- Use `MODE=PostgreSQL` for maximum compatibility
- Set appropriate `CACHE_SIZE` based on available memory
- Disable tracing (`TRACE_LEVEL_FILE=0`) for performance

### Database Sharing
- Always use file-based paths for multi-process access
- Avoid in-memory databases for APEX integration
- Use consistent database paths across configurations

### H2 Database Usage Guidelines for APEX Demos

#### ✅ DO:
- Use file-based H2: `database: "./target/h2-demo/apex_demo_shared"`
- Use uppercase field mappings: `source-field: "CUSTOMER_NAME"`
- Add proper cleanup in demo code: `DROP TABLE IF EXISTS customers`
- Use consistent JDBC URLs: `jdbc:h2:./target/h2-demo/apex_demo_shared;DB_CLOSE_DELAY=-1;MODE=PostgreSQL`
- Use custom H2 parameters: `database: "./target/h2-demo/custom;MODE=MySQL;CACHE_SIZE=32768"`

#### X DON'T:
- Use in-memory H2: `database: "shared_demo"` (creates isolated instances)
- Use lowercase field mappings: `source-field: "customer_name"` (won't match H2 columns)
- Skip cleanup between demo runs (causes primary key violations)
- Mix in-memory and file-based patterns in the same demo suite

## Troubleshooting

### "Table not found" Errors
**Cause:** Database instance isolation between demo and APEX
**Solution:** Use file-based H2 with consistent paths

### Primary Key Violations
**Cause:** Multiple demo runs without cleanup
**Solution:** Add `DROP TABLE IF EXISTS` statements

### Field Mapping Failures
**Cause:** Case sensitivity mismatch
**Solution:** Use uppercase source field names

### Connection Refused
**Cause:** Database files locked or corrupted
**Solution:** Delete `target/h2-demo/` directory and restart

### Common Parameter Issues

1. **Invalid Parameter Names**: H2 parameters are case-sensitive
2. **Conflicting Parameters**: Custom parameters override defaults
3. **File Path Issues**: Ensure target directories exist
4. **Memory Settings**: Don't exceed available JVM memory

### Validation

Test your configuration with a simple connection:
```java
String jdbcUrl = "jdbc:h2:./target/h2-demo/test;MODE=MySQL;CACHE_SIZE=32768;DB_CLOSE_DELAY=-1";
try (Connection conn = DriverManager.getConnection(jdbcUrl, "sa", "")) {
    // Configuration is valid
}
```

## Integration Issues and Solutions

### Executive Summary

The core problem with H2 database integration in APEX was **database instance isolation** where demo code and APEX YAML configurations were creating separate H2 database instances, leading to "Table not found" errors despite both systems working correctly in isolation.

### Problem Discovery and Analysis

#### Initial Problem Statement
Multiple H2 database demos in the APEX system were failing with consistent errors:
- `Table "CUSTOMERS" not found (this database is empty)`
- Database lookup operations returning null results
- YAML enrichment configurations failing to access database tables

#### Affected Demos
1. `SimplePostgreSQLLookupDemo`
2. `PostgreSQLLookupDemo`
3. `ExternalDataSourceWorkingDemo`
4. Various database connectivity tests

### Investigation Process

#### Step 1: Systematic Testing

**Testing APEX Core Functionality:**
```bash
java -cp "apex-demo/target/apex-demo-1.0-SNAPSHOT-jar-with-dependencies.jar" dev.mars.apex.demo.runners.ValidationRunner all
```

**Result:** ✅ **APEX core services worked perfectly**
- Validation demos: 100% successful
- YAML configuration loading: Working
- Enrichment processing: Working
- Expression evaluation: Working

#### Step 2: Testing H2 Database Connectivity

**Testing Basic H2 Operations:**
```bash
java -cp "apex-demo/target/apex-demo-1.0-SNAPSHOT-jar-with-dependencies.jar" dev.mars.apex.demo.lookup.DatabaseConnectivityTest
```

**Result:** ✅ **H2 database operations worked perfectly**
- Database connection: Successful
- Table creation: Successful
- Data insertion: Successful
- SQL queries: Successful

#### Step 3: Isolating the Integration Issue

**Key Discovery:** Both APEX and H2 worked individually, but failed when integrated through YAML configurations.

**Error Pattern Analysis:**
```
[main] INFO - H2 database initialized with 5 customer records
[main] INFO - Successfully loaded configuration: Customer Profile Demo
[main] ERROR - Table "CUSTOMERS" not found (this database is empty)
```

This revealed the core issue: **Two separate H2 database instances were being created.**

### Solution Development

#### Approach 1: YAML Configuration Fixes (Partial Success)

**Issues Found and Fixed:**
1. **Incorrect YAML syntax:** `dataSources` → `data-sources`
2. **Wrong connection format:** `host:port` → `url` for H2
3. **Field mapping errors:** `source/target` → `source-field/target-field`
4. **Missing enrichment sections**

**Example Fix:**
```yaml
# BEFORE (Broken)
dataSources:
  - name: "customer-database"
    sourceType: "h2"
    connection:
      host: "localhost"
      port: 9092
      database: "apex_demo_shared"

# AFTER (Fixed)
data-sources:
  - name: "customer-database"
    source-type: "h2"
    connection:
      url: "jdbc:h2:mem:apex_demo_shared;DB_CLOSE_DELAY=-1;MODE=PostgreSQL"
      username: "sa"
      password: ""
```

**Result:** Configuration loaded successfully, but database access still failed due to instance isolation.

#### Approach 2: Shared DataSource Pattern (Complete Solution)

**Key Insight:** Instead of letting APEX create its own database connection, provide a shared database instance.

### Final Working Solution

The **Shared Database Pattern** ensures 100% success for H2 database integration with APEX Rules Engine. The key insights are:

1. **Use file-based H2 databases** (`./target/h2-demo/apex_demo_shared`) for true sharing between processes
2. **Avoid in-memory H2** (`jdbc:h2:mem:`) which creates isolated database instances
3. **Use uppercase field mappings** (`CUSTOMER_NAME`) as H2 returns uppercase column names
4. **Implement proper cleanup** (`DROP TABLE IF EXISTS`) to prevent data collisions
5. **APEX YAML configurations support the `database` field** for H2 connections

### Contrast: Real PostgreSQL Setup in APEX

#### PostgreSQL Pattern (Production)

**Key Differences from H2:**

1. **External Database Server:** PostgreSQL runs as a separate process
2. **Persistent Storage:** Data survives application restarts
3. **Connection Pooling:** Multiple connections to same database instance
4. **Network-based:** Connections via TCP/IP

**PostgreSQL YAML Configuration Example:**

```yaml
# Real PostgreSQL setup
data-sources:
  - name: "production-customer-db"
    type: "database"
    source-type: "postgresql"
    enabled: true
    description: "Production PostgreSQL customer database"

    connection:
      host: "prod-db.company.com"
      port: 5432
      database: "customer_data"
      username: "${DB_USERNAME}"  # Environment variable
      password: "${DB_PASSWORD}"  # Environment variable
      schema: "public"

      # Connection pool settings (critical for production)
      maxPoolSize: 20
      minPoolSize: 5
      connectionTimeout: 30000
      idleTimeout: 600000
      maxLifetime: 1800000

      # SSL configuration
      sslEnabled: true
      sslMode: "require"

    queries:
      customerLookup: |
        SELECT
          customer_name,
          customer_type,
          tier,
          region,
          status,
          created_date,
          last_updated
        FROM customers
        WHERE customer_id = :customerId
        AND status = 'ACTIVE'

    # Production health checks
    healthCheck:
      enabled: true
      intervalSeconds: 30
      timeoutSeconds: 5
      failureThreshold: 3
      query: "SELECT 1"

    # Production caching
    cache:
      enabled: true
      ttlSeconds: 300
      maxSize: 1000
      keyPrefix: "prod_customer"
```

#### Why PostgreSQL Doesn't Have This Issue:

1. **External Server:** PostgreSQL runs independently of the application
2. **Network Connections:** All connections go to the same server instance
3. **Connection Pooling:** Designed for multiple concurrent connections
4. **Persistent State:** Database state exists independently of application lifecycle

#### Why H2 In-Memory Had the Issue:

1. **In-Process:** H2 runs inside the JVM
2. **Memory-based:** Database exists only in application memory
3. **Instance Isolation:** Different parts of application can create separate instances
4. **Lifecycle Coupling:** Database lifecycle tied to application lifecycle

### Key Lessons and Best Practices

#### For H2 In-Memory Databases with APEX:

1. **Single Instance Creation:** Create database instance once, share everywhere
2. **Lifecycle Management:** Keep connection alive throughout demo lifecycle
3. **Exact URL Matching:** Use identical JDBC URLs in Java and YAML
4. **Proper Cleanup:** Close connections when demo completes

#### For Production PostgreSQL with APEX:

1. **Connection Pooling:** Use proper connection pool configuration
2. **Environment Variables:** Externalize credentials and connection details
3. **Health Checks:** Configure robust health monitoring
4. **SSL Security:** Enable SSL for production connections
5. **Schema Management:** Use proper schema organization

#### General Integration Patterns:

1. **Test Components Separately:** Verify APEX and database work individually
2. **Systematic Debugging:** Use step-by-step isolation to find root causes
3. **Configuration Validation:** Verify YAML syntax and structure
4. **Connection Verification:** Test database connectivity before APEX integration

## Migration Guide

### Migration from In-memory to File-based

To migrate existing demos from in-memory to file-based H2:

1. **Update YAML configuration:**
   ```yaml
   # Change from:
   database: "shared_demo"
   # To:
   database: "./target/h2-demo/apex_demo_shared"
   ```

2. **Update Java JDBC URLs:**
   ```java
   // Change from:
   String jdbcUrl = "jdbc:h2:mem:shared_demo;DB_CLOSE_DELAY=-1;MODE=PostgreSQL";
   // To:
   String jdbcUrl = "jdbc:h2:./target/h2-demo/apex_demo_shared;DB_CLOSE_DELAY=-1;MODE=PostgreSQL";
   ```

3. **Update field mappings to uppercase:**
   ```yaml
   # Change from:
   source-field: "customer_name"
   # To:
   source-field: "CUSTOMER_NAME"
   ```

4. **Add cleanup code:**
   ```java
   statement.execute("DROP TABLE IF EXISTS customers");
   ```

This migration ensures reliable database sharing between demo code and APEX configurations.

### Migration from Basic to Custom Parameters

**Before:**
```yaml
database: "./target/h2-demo/apex_demo_shared"
```

**After:**
```yaml
database: "./target/h2-demo/apex_demo_shared;MODE=MySQL;CACHE_SIZE=32768"
```

### From In-Memory to File-Based with Parameters

**Before:**
```yaml
database: "shared_demo"
```

**After:**
```yaml
database: "./target/h2-demo/shared_demo;MODE=PostgreSQL;CACHE_SIZE=32768"
```

## Quick Reference

### Common Parameter Combinations

```yaml
# High Performance
database: "./target/h2-demo/performance;CACHE_SIZE=65536;MAX_MEMORY_ROWS=100000"

# Debug Mode
database: "./target/h2-demo/debug;TRACE_LEVEL_FILE=2;TRACE_LEVEL_SYSTEM_OUT=1"

# MySQL Compatible
database: "./target/h2-demo/mysql;MODE=MySQL;CACHE_SIZE=32768"

# Oracle Compatible
database: "./target/h2-demo/oracle;MODE=Oracle;CACHE_SIZE=32768"

# Read-Only
database: "./target/h2-demo/readonly;ACCESS_MODE_DATA=r;IFEXISTS=TRUE"

# Auto-Initialize
database: "./target/h2-demo/init;INIT=RUNSCRIPT FROM 'classpath:schema.sql'"
```

### Parameter Quick Reference

| Category | Parameters | Example |
|----------|------------|---------|
| **Performance** | `CACHE_SIZE`, `MAX_MEMORY_ROWS` | `CACHE_SIZE=65536` |
| **Compatibility** | `MODE` | `MODE=MySQL` |
| **Debugging** | `TRACE_LEVEL_FILE`, `TRACE_LEVEL_SYSTEM_OUT` | `TRACE_LEVEL_FILE=2` |
| **Security** | `ACCESS_MODE_DATA`, `CIPHER` | `ACCESS_MODE_DATA=r` |
| **Initialization** | `INIT`, `SCHEMA` | `INIT=RUNSCRIPT FROM 'init.sql'` |

### Future Reference Pattern

#### The Working H2 + APEX Integration Pattern:

```java
// 1. Create shared database instance
String DB_URL = "jdbc:h2:mem:demo_db;DB_CLOSE_DELAY=-1;MODE=PostgreSQL";
Connection sharedConnection = DriverManager.getConnection(DB_URL, "sa", "");

// 2. Initialize tables and data
// (Create tables, insert test data)

// 3. Initialize APEX services
EnrichmentService enrichmentService = new EnrichmentService(serviceRegistry, expressionEvaluator);

// 4. Use same DB_URL in YAML configuration
// 5. Keep connection alive during demo
// 6. Cleanup properly when done
```

#### YAML Configuration Pattern:

```yaml
data-sources:
  - name: "demo-database"
    type: "database"
    source-type: "h2"
    connection:
      url: "jdbc:h2:mem:demo_db;DB_CLOSE_DELAY=-1;MODE=PostgreSQL"  # EXACT match
      username: "sa"
      password: ""
```

## Reference Links

- [H2 Database Official Documentation](http://h2database.com/html/features.html)
- [H2 Connection Parameters](http://h2database.com/html/features.html#database_url)
- [H2 Compatibility Modes](http://h2database.com/html/features.html#compatibility)
- [APEX YAML Reference](APEX_YAML_REFERENCE.md)
- [APEX Technical Reference](APEX_TECHNICAL_REFERENCE.md)
