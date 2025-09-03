# H2 Database Parameter Reference for APEX

## Overview

APEX now supports custom H2 parameters directly in the YAML configuration database field. This provides unprecedented flexibility for H2 database configuration, allowing you to tune performance, enable debugging, set compatibility modes, and more.

## Parameter Format

```yaml
connection:
  # Format: "path/to/database;PARAM1=value1;PARAM2=value2"
  database: "./target/h2-demo/custom;MODE=MySQL;CACHE_SIZE=32768;TRACE_LEVEL_FILE=2"
  username: "sa"
  password: ""
```

## Parameter Categories

### Performance Tuning Parameters

| Parameter | Description | Default | Example Values |
|-----------|-------------|---------|----------------|
| `CACHE_SIZE` | Database cache size in KB | 16384 (16MB) | `32768` (32MB), `65536` (64MB) |
| `MAX_MEMORY_ROWS` | Maximum rows kept in memory | 40000 | `100000`, `200000` |
| `MAX_MEMORY_UNDO` | Maximum undo log entries in memory | 50000 | `100000`, `200000` |
| `MAX_OPERATION_MEMORY` | Maximum memory for operations in KB | 100000 | `200000`, `500000` |
| `CACHE_TYPE` | Cache algorithm | TQ | `LRU`, `SOFT_LRU` |

### Compatibility Mode Parameters

| Parameter | Description | Example |
|-----------|-------------|---------|
| `MODE` | Database compatibility mode | `PostgreSQL`, `MySQL`, `Oracle`, `DB2`, `HSQLDB` |

**Mode Details:**
- `PostgreSQL` - PostgreSQL compatibility (APEX default)
- `MySQL` - MySQL compatibility mode
- `Oracle` - Oracle compatibility mode
- `DB2` - IBM DB2 compatibility mode
- `HSQLDB` - HSQLDB compatibility mode

### Debugging and Logging Parameters

| Parameter | Description | Values | Example |
|-----------|-------------|--------|---------|
| `TRACE_LEVEL_FILE` | SQL logging level to file | 0-4 | `0` (off), `1` (error), `2` (info), `4` (debug) |
| `TRACE_LEVEL_SYSTEM_OUT` | SQL logging to console | 0-4 | `0` (off), `1` (error), `2` (info) |
| `TRACE_MAX_FILE_SIZE` | Maximum trace file size in MB | Number | `16`, `32`, `64` |

### Connection Management Parameters

| Parameter | Description | Values | Example |
|-----------|-------------|--------|---------|
| `DB_CLOSE_DELAY` | Keep database open after last connection | -1, 0, >0 | `-1` (forever), `0` (immediate), `30` (30 seconds) |
| `DB_CLOSE_ON_EXIT` | Close database when JVM exits | TRUE/FALSE | `TRUE`, `FALSE` |
| `AUTO_SERVER` | Enable automatic mixed mode | TRUE/FALSE | `TRUE`, `FALSE` |
| `AUTO_SERVER_PORT` | Port for automatic server mode | Number | `9090`, `9091` |

### Security and Access Parameters

| Parameter | Description | Values | Example |
|-----------|-------------|--------|---------|
| `ACCESS_MODE_DATA` | Database access mode | r, rw | `r` (read-only), `rw` (read-write) |
| `IFEXISTS` | Only connect if database exists | TRUE/FALSE | `TRUE`, `FALSE` |
| `CIPHER` | Encryption cipher | AES, XTEA | `AES`, `XTEA` |

### Initialization Parameters

| Parameter | Description | Example |
|-----------|-------------|---------|
| `INIT` | SQL script to run on startup | `RUNSCRIPT FROM 'classpath:init.sql'` |
| `SCHEMA` | Default schema name | `PUBLIC`, `DEMO` |

## Parameter Merging Rules

1. **Custom parameters override APEX defaults**
2. **APEX automatically adds `DB_CLOSE_DELAY=-1` if not specified**
3. **No duplicate parameters - custom takes precedence**
4. **Additional custom parameters are preserved**
5. **Parameters are case-sensitive**

## Example Configurations

### Performance-Optimized Configuration

```yaml
connection:
  database: "./target/h2-demo/performance;MODE=PostgreSQL;CACHE_SIZE=65536;MAX_MEMORY_ROWS=100000;MAX_MEMORY_UNDO=100000"
  username: "sa"
  password: ""
```

### Debug-Enabled Configuration

```yaml
connection:
  database: "./target/h2-demo/debug;TRACE_LEVEL_FILE=2;TRACE_LEVEL_SYSTEM_OUT=1;TRACE_MAX_FILE_SIZE=32"
  username: "sa"
  password: ""
```

### MySQL-Compatible Configuration

```yaml
connection:
  database: "./target/h2-demo/mysql;MODE=MySQL;CACHE_SIZE=32768"
  username: "sa"
  password: ""
```

### Read-Only Configuration

```yaml
connection:
  database: "./target/h2-demo/readonly;ACCESS_MODE_DATA=r;IFEXISTS=TRUE"
  username: "sa"
  password: ""
```

### Auto-Initialization Configuration

```yaml
connection:
  database: "./target/h2-demo/autoinit;INIT=RUNSCRIPT FROM 'classpath:schema.sql';SCHEMA=DEMO"
  username: "sa"
  password: ""
```

### In-Memory with Custom Parameters

```yaml
connection:
  database: "mem:testdb;CACHE_SIZE=16384;MODE=Oracle;TRACE_LEVEL_SYSTEM_OUT=1"
  username: "sa"
  password: ""
```

## Best Practices

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

## Migration Guide

### From Basic to Custom Parameters

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

## Troubleshooting

### Common Issues

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

## Reference Links

- [H2 Database Official Documentation](http://h2database.com/html/features.html)
- [H2 Connection Parameters](http://h2database.com/html/features.html#database_url)
- [H2 Compatibility Modes](http://h2database.com/html/features.html#compatibility)
