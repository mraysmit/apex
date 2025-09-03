# H2 Database Usage Guide for APEX Rules Engine

## Overview

This guide provides best practices for using H2 databases with the APEX Rules Engine, particularly for demos and testing scenarios. H2 is an embedded Java database that can run in-memory or file-based modes.

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

## YAML Configuration Patterns

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

# Enhanced H2 Configuration with Custom Parameters (NEW!)
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

### ❌ DEPRECATED: In-memory H2 Configuration

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

## H2 Parameter Support (Enhanced!)

### Supported Parameter Formats

APEX now supports custom H2 parameters directly in the `database` field:

```yaml
# Basic file-based (uses APEX defaults)
database: "./target/h2-demo/apex_demo_shared"
# → jdbc:h2:./target/h2-demo/apex_demo_shared;DB_CLOSE_DELAY=-1;MODE=PostgreSQL

# Custom parameters (override defaults)
database: "./target/h2-demo/custom;MODE=MySQL;TRACE_LEVEL_FILE=2"
# → jdbc:h2:./target/h2-demo/custom;MODE=MySQL;TRACE_LEVEL_FILE=2;DB_CLOSE_DELAY=-1

# In-memory with custom parameters
database: "mem:testdb;CACHE_SIZE=16384;MODE=Oracle"
# → jdbc:h2:mem:testdb;CACHE_SIZE=16384;MODE=Oracle;DB_CLOSE_DELAY=-1;MODE=PostgreSQL
```

### Parameter Merging Rules

1. **Custom parameters override APEX defaults**
2. **APEX automatically adds `DB_CLOSE_DELAY=-1` if not specified**
3. **Additional custom parameters are preserved**
4. **Parameter order: custom parameters first, then APEX defaults**

### Common H2 Parameters

| Parameter | Description | Example Values |
|-----------|-------------|----------------|
| `MODE` | Database compatibility mode | `PostgreSQL`, `MySQL`, `Oracle`, `DB2` |
| `CACHE_SIZE` | Database cache size in KB | `32768` (32MB), `65536` (64MB) |
| `TRACE_LEVEL_FILE` | SQL logging level to file | `0` (off), `1` (error), `2` (info), `4` (debug) |
| `TRACE_LEVEL_SYSTEM_OUT` | SQL logging to console | `0` (off), `1` (error), `2` (info) |
| `DB_CLOSE_DELAY` | Keep DB open after last connection | `-1` (forever), `0` (immediate), `>0` (seconds) |
| `INIT` | SQL script to run on startup | `RUNSCRIPT FROM 'classpath:init.sql'` |

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

# ❌ INCORRECT: Lowercase source fields won't match
field-mappings:
  - source-field: "customer_name"    # Won't match H2 uppercase columns
    target-field: "customerName"
```

## Demo Best Practices

### 1. Database Cleanup
Always clean up existing data to prevent primary key violations:

```java
// Clean up before creating tables
statement.execute("DROP TABLE IF EXISTS customers");
statement.execute("DROP TABLE IF EXISTS orders");  // Drop dependent tables first
```

### 2. Consistent JDBC URLs
Use the exact same JDBC URL pattern in both Java code and YAML:

**Java:** `jdbc:h2:./target/h2-demo/apex_demo_shared;DB_CLOSE_DELAY=-1;MODE=PostgreSQL`
**YAML:** `database: "./target/h2-demo/apex_demo_shared"`

### 3. Directory Structure
Ensure the target directory exists:
```bash
mkdir -p target/h2-demo
```

### 4. Connection Parameters
- `DB_CLOSE_DELAY=-1`: Keeps database open between connections
- `MODE=PostgreSQL`: Enables PostgreSQL compatibility mode

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

## Migration from In-memory to File-based

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

For complete parameter reference, see [H2_Parameter_Reference.md](H2_Parameter_Reference.md).
