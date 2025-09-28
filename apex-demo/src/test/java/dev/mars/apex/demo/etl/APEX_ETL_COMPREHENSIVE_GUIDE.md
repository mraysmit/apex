# APEX ETL Pipeline Comprehensive Guide

## Overview

The APEX Rules Engine provides a powerful ETL (Extract, Transform, Load) pipeline system that supports YAML-driven configuration for data processing workflows. This comprehensive guide covers pipeline configuration, error handling, operation resolution, practical demonstrations, and best practices.

## Table of Contents

1. [Pipeline Configuration Structure](#pipeline-configuration-structure)
2. [Error Handling Configuration](#error-handling-configuration)
3. [Operation Resolution System](#operation-resolution-system)
4. [Step Types and Dependencies](#step-types-and-dependencies)
5. [Practical Demo: CSV to H2 Database Pipeline](#practical-demo-csv-to-h2-database-pipeline)
6. [Best Practices](#best-practices)
7. [Troubleshooting](#troubleshooting)
8. [Example Configurations](#example-configurations)

## Pipeline Configuration Structure

### Basic Pipeline YAML Structure

```yaml
metadata:                                    # Configuration metadata and identification
  id: "pipeline-id"                        # Unique identifier for this configuration
  name: "Pipeline Name"                    # Human-readable name displayed in logs and UI
  version: "1.0.0"                        # Version number for tracking configuration changes
  type: "pipeline-config"                 # Configuration type: defines a data pipeline

pipeline:                                   # Main pipeline definition
  name: "pipeline-name"                    # Pipeline execution name
  description: "Pipeline description"      # Brief description of pipeline purpose
  
  execution:                              # Execution behavior settings for the entire pipeline
    mode: "sequential"                    # Execute pipeline steps one after another in order (vs "parallel")
    error-handling: "stop-on-error"      # Stop entire pipeline if any step fails (vs "continue-on-error")
    max-retries: 3                        # Retry failed steps up to 3 times before giving up (0 = no retries)
    retry-delay-ms: 1000                  # Wait time between retry attempts in milliseconds

  steps:                                  # List of pipeline steps to execute
    - name: "step-name"                   # Unique name for this step
      type: "extract"                     # Step type: extract, transform, load, audit
      source: "data-source-name"          # References data-sources section
      operation: "operation-name"         # Operation to execute on the source/sink
      description: "Step description"     # Human-readable description
      depends-on: ["previous-step"]       # List of steps that must complete first
      # optional: true                    # CRITICAL: Overrides pipeline-level error-handling!

data-sources:                             # Input data source definitions
  - name: "source-name"                   # Unique identifier for this data source
    type: "file-system"                   # Data source type: file-system, database, api, etc.
    enabled: true                         # Whether this data source is active
    connection:                           # Connection parameters specific to source type
      base-path: "./data"                 # File system: directory path
      file-pattern: "*.csv"               # File system: filename pattern
      encoding: "UTF-8"                   # File system: character encoding
    operations:                           # Named operations available on this source
      getAllData: "SELECT * FROM csv"     # SQL-like query for file sources

data-sinks:                               # Output data sink definitions
  - name: "sink-name"                     # Unique identifier for this data sink
    type: "database"                      # Data sink type: database, file-system, api, etc.
    enabled: true                         # Whether this data sink is active
    connection:                           # Connection parameters specific to sink type
      url: "jdbc:h2:./database"          # Database: JDBC connection URL
      driver: "org.h2.Driver"             # Database: JDBC driver class
    operations:                           # Named operations available on this sink
      insertRecord: "INSERT INTO table..."  # SQL statement with parameter binding
```

## Error Handling Configuration

### Pipeline-Level Error Handling

The `execution.error-handling` setting controls how the pipeline responds to step failures:

- **`"stop-on-error"`** (Recommended): Pipeline stops immediately when any step fails
- **`"continue-on-error"`**: Pipeline continues executing remaining steps even if some fail

### Step-Level Error Handling Override

**⚠️ CRITICAL CONFIGURATION PRECEDENCE:**

```yaml
pipeline:
  execution:
    error-handling: "stop-on-error"      # Pipeline-level setting

  steps:
    - name: "critical-step"
      type: "load"
      # optional: true                    # ❌ OVERRIDES pipeline-level error-handling!
      # If uncommented, this step's failure will NOT stop the pipeline
```

**Key Insights:**
- Step-level `optional: true` **overrides** pipeline-level `error-handling: "stop-on-error"`
- Use `optional: true` only for truly optional steps (logging, notifications, etc.)
- For critical steps, omit `optional` to respect pipeline-level error handling

### Retry Configuration

```yaml
execution:
  max-retries: 3                          # Number of retry attempts (0 = no retries)
  retry-delay-ms: 1000                    # Wait time between retries in milliseconds
```

## Operation Resolution System

### Data Source Operations

File system data sources support SQL-like queries:

```yaml
data-sources:
  - name: "csv-input"
    type: "file-system"
    operations:
      getAllRecords: "SELECT * FROM csv"           # Read all columns
      getActiveOnly: "SELECT * FROM csv WHERE status = 'ACTIVE'"  # Filtered query
```

### Data Sink Operations

#### Database Sinks

```yaml
data-sinks:
  - name: "database-output"
    type: "database"
    operations:
      insertCustomer: |                   # Multi-line SQL with parameter binding
        INSERT INTO customers (id, name, email, status, created_at)
        VALUES (:column_1, :column_2, :column_3, :column_5, CURRENT_TIMESTAMP)
```

#### File System Sinks

File system sinks support operation mapping from configured names to built-in operations:

```yaml
data-sinks:
  - name: "audit-log"
    type: "file-system"
    operations:
      writeAuditRecord: "write"           # Maps custom name to built-in "write" operation
      appendLog: "append"                 # Maps custom name to built-in "append" operation
```

**Built-in File System Operations:**
- `write`: Write data to file (overwrites existing content)
- `append`: Append data to existing file
- `overwrite`: Explicitly overwrite file content
- `rotate`: Rotate log files (create new file, archive old)
- `archive`: Archive current file and create new one

### Operation Resolution Process

1. **Direct Match**: Check if operation name directly matches a built-in operation
2. **Configuration Mapping**: Look up operation name in the `operations` configuration
3. **Validation**: Verify that the resolved operation is supported by the sink type
4. **Execution**: Execute the resolved operation with provided data

## Step Types and Dependencies

### Step Types

- **`extract`**: Read data from a data source
- **`transform`**: Process/modify data (requires custom transformation logic)
- **`load`**: Write data to a data sink
- **`audit`**: Write audit/logging records for compliance

### Step Dependencies

```yaml
steps:
  - name: "extract-data"
    type: "extract"
    # No dependencies - runs first

  - name: "load-data"
    type: "load"
    depends-on: ["extract-data"]          # Waits for extract-data to complete

  - name: "audit-logging"
    type: "audit"
    depends-on: ["load-data"]             # Waits for load-data to complete
```

## Practical Demo: CSV to H2 Database Pipeline

This demo showcases APEX's new data sink capabilities by implementing a complete ETL (Extract, Transform, Load) pipeline that processes CSV customer data and writes it to an H2 database.

### Demo Overview

The demo demonstrates:

- **YAML Configuration**: Complete pipeline configuration using APEX YAML syntax
- **Data Sinks**: Database output using the new DataSink framework
- **Error Handling**: Comprehensive error handling and retry mechanisms
- **Batch Processing**: Efficient bulk operations with configurable batch sizes
- **Schema Management**: Automatic database table creation and management
- **Monitoring**: Metrics collection and performance monitoring

### Architecture

```
CSV Data → APEX Processing → H2 Database
    ↓           ↓              ↓
Sample     Enrichment/     customer_database
Records    Validation      + audit_log.json
```

### Demo Components

#### 1. CsvToH2PipelineDemo.java
Main demo class that orchestrates the ETL process:
- Sets up demo environment and sample data
- Loads YAML configuration
- Creates and initializes data sinks
- Processes sample customer records
- Demonstrates both individual and batch operations
- Collects and displays metrics

#### 2. csv-to-h2-pipeline.yaml
Complete APEX configuration demonstrating:
- **Data Sink Configuration**: H2 database with connection pooling
- **SQL Operations**: INSERT, UPDATE, UPSERT operations
- **Schema Management**: Auto-creation of customer table with indexes
- **Error Handling**: Retry strategies and dead letter handling
- **Batch Processing**: Optimized bulk operations
- **Health Checks**: Connection monitoring and circuit breaker
- **Audit Logging**: Secondary file-based data sink for audit trail

#### 3. Sample Data
The demo creates sample customer records with:
- Customer ID, Name, Email
- Registration dates and status
- Processing timestamps

### Configuration Highlights

#### Database Connection
```yaml
connection:
  database: "./target/demo/etl/output/customer_database"
  username: "sa"
  password: ""
  mode: "PostgreSQL"
  connection-pool:
    max-size: 10
    min-size: 2
    connection-timeout: 30000
```

#### SQL Operations
```yaml
operations:
  insertCustomer: |
    INSERT INTO customers (customer_id, customer_name, email, status, processed_at, created_at)
    VALUES (:id, :customerName, :email, :status, :processedAt, CURRENT_TIMESTAMP)

  upsertCustomer: |
    MERGE INTO customers (customer_id, customer_name, email, status, processed_at)
    KEY (customer_id) VALUES (:id, :customerName, :email, :status, :processedAt)
```

#### Error Handling
```yaml
error-handling:
  strategy: "log-and-continue"
  max-retries: 3
  retry-delay: 1000
  dead-letter-enabled: true
  dead-letter-table: "failed_customer_records"
```

#### Batch Processing
```yaml
batch:
  enabled: true
  batch-size: 50
  timeout-ms: 10000
  transaction-mode: "per-batch"
  enable-metrics: true
```

### Running the Demo

#### Prerequisites
- Java 21+
- Maven 3.6+
- APEX Core library

#### Execution
```bash
# From the apex-demo directory
mvn compile exec:java -Dexec.mainClass="dev.mars.apex.demo.etl.CsvToH2PipelineDemo"
```

#### Expected Output
```
=== APEX ETL Demo: CSV to H2 Database Pipeline ===
Setting up demo environment...
✓ Created sample CSV file: ./target/demo/etl/data/customers.csv
✓ Demo environment setup complete
Loading YAML configuration...
✓ Configuration loaded: CSV to H2 ETL Pipeline Demo
✓ Data sinks configured: 2
Creating data sink...
✓ Data sink created and initialized: customer-h2-database
✓ Sink type: Database
✓ Connection status: CONNECTED
Processing sample data...
Processing 5 customer records individually...
Processing additional records in batch...
✓ Batch processed 5 records successfully
✓ Sample data processing complete
Verifying ETL results...
=== ETL Processing Metrics ===
Total writes: 6
Successful writes: 6
Failed writes: 0
Total batches: 1
Successful batches: 1
Total records written: 10
Write success rate: 100.00%
Average write time: 15.50ms
Data sink health status: HEALTHY
✓ ETL process completed successfully with 10 records processed
=== Demo completed successfully ===
```

### Generated Artifacts

After running the demo, you'll find:

#### Database Files
- `./target/demo/etl/output/customer_database.mv.db` - H2 database file
- `./target/demo/etl/output/customer_database.trace.db` - H2 trace file (if enabled)

#### Audit Files
- `./target/demo/etl/output/audit/customer_audit_*.json` - Audit log files

#### Sample Data
- `./target/demo/etl/data/customers.csv` - Generated sample CSV data

### Database Schema

The demo creates a `customers` table with the following structure:

```sql
CREATE TABLE customers (
  customer_id INTEGER PRIMARY KEY,
  customer_name VARCHAR(255) NOT NULL,
  email VARCHAR(255) UNIQUE,
  status VARCHAR(50) DEFAULT 'ACTIVE',
  processed_at TIMESTAMP,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

### Key Features Demonstrated

#### 1. YAML Configuration Integration
- Complete pipeline configuration in YAML
- APEX syntax compliance with kebab-case properties
- Reuse of existing configuration patterns

#### 2. Data Sink Framework
- Database data sink with H2 support
- File system data sink for audit logging
- Factory pattern for sink creation

#### 3. Error Handling & Resilience
- Retry mechanisms with exponential backoff
- Dead letter handling for failed records
- Circuit breaker for connection failures

#### 4. Performance Optimization
- Connection pooling for database efficiency
- Batch processing for high throughput
- Memory management and monitoring

#### 5. Monitoring & Observability
- Comprehensive metrics collection
- Health checks and status monitoring
- Performance timing and success rates

### Extending the Demo

This demo can be extended to showcase additional APEX features:

1. **Data Sources**: Add CSV file input using APEX data sources
2. **Rule Processing**: Add business rules for data validation
3. **Enrichments**: Implement data transformation rules
4. **Multiple Outputs**: Route data to different sinks based on conditions
5. **Real-time Processing**: Add file watching for continuous processing

## Best Practices

### Error Handling
1. Use `error-handling: "stop-on-error"` for production pipelines
2. Only use `optional: true` for non-critical steps (logging, notifications)
3. Configure appropriate retry settings for transient failures
4. Monitor pipeline execution logs for error patterns

### Configuration Organization
1. Use descriptive names for steps, sources, and sinks
2. Add comprehensive comments explaining configuration choices
3. Group related operations logically
4. Version your pipeline configurations

### Performance Optimization
1. Use `mode: "parallel"` for independent steps when possible
2. Configure appropriate buffer sizes for large datasets
3. Consider database connection pooling for high-throughput scenarios
4. Monitor step execution times and optimize bottlenecks

### Data Integrity
1. Handle data type conversions explicitly
2. Validate required fields before processing
3. Implement proper error handling for constraint violations
4. Use transactions for database operations when needed

## Troubleshooting

### Common Issues

1. **Pipeline continues despite failures**
   - Check for `optional: true` on critical steps
   - Verify `error-handling: "stop-on-error"` is set

2. **Operation not supported errors**
   - Verify operation names match configuration
   - Check that sink type supports the operation
   - Review operation resolution logs

3. **Data integrity violations**
   - Check for duplicate primary keys
   - Validate data types match target schema
   - Review constraint definitions

4. **Database Connection Errors**
   - Ensure target directory exists and is writable
   - Check H2 database file permissions

5. **Configuration Loading Errors**
   - Verify YAML syntax and indentation
   - Check classpath for configuration file

6. **Memory Issues**
   - Adjust batch sizes in configuration
   - Monitor memory usage with JVM flags

### Debugging Tips

1. Enable DEBUG logging for detailed execution traces
2. Check operation resolution logs for mapping issues
3. Verify data source/sink connectivity before pipeline execution
4. Use small test datasets for initial pipeline validation

### Debug Mode
Add `-Dlogging.level.dev.mars.apex=DEBUG` to enable detailed logging.

## Example Configurations

See the following example files in the `apex-demo/src/test/java/dev/mars/apex/demo/etl/` directory:

- `SimplePipelineTest.yaml`: Basic pipeline for learning
- `CsvToH2PipelineTest.yaml`: Production-ready CSV to database pipeline
- `PipelineEtlTest.yaml`: Advanced enterprise ETL processing

Each example includes comprehensive inline comments explaining configuration options and best practices.

## Related Documentation

- [APEX YAML Reference](../../docs/APEX_YAML_REFERENCE.md)
- [Data Sink Framework Design](../../docs/APEX_DATA_PIPELINE_OUTPUT_DESIGN.md)
- [APEX Core Documentation](../../apex-core/README.md)
