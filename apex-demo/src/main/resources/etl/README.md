# APEX ETL Demo: CSV to H2 Database Pipeline

This demo showcases APEX's new data sink capabilities by implementing a complete ETL (Extract, Transform, Load) pipeline that processes CSV customer data and writes it to an H2 database.

## Overview

The demo demonstrates:

- **YAML Configuration**: Complete pipeline configuration using APEX YAML syntax
- **Data Sinks**: Database output using the new DataSink framework
- **Error Handling**: Comprehensive error handling and retry mechanisms
- **Batch Processing**: Efficient bulk operations with configurable batch sizes
- **Schema Management**: Automatic database table creation and management
- **Monitoring**: Metrics collection and performance monitoring

## Architecture

```
CSV Data → APEX Processing → H2 Database
    ↓           ↓              ↓
Sample     Enrichment/     customer_database
Records    Validation      + audit_log.json
```

## Demo Components

### 1. CsvToH2PipelineDemo.java
Main demo class that orchestrates the ETL process:
- Sets up demo environment and sample data
- Loads YAML configuration
- Creates and initializes data sinks
- Processes sample customer records
- Demonstrates both individual and batch operations
- Collects and displays metrics

### 2. csv-to-h2-pipeline.yaml
Complete APEX configuration demonstrating:
- **Data Sink Configuration**: H2 database with connection pooling
- **SQL Operations**: INSERT, UPDATE, UPSERT operations
- **Schema Management**: Auto-creation of customer table with indexes
- **Error Handling**: Retry strategies and dead letter handling
- **Batch Processing**: Optimized bulk operations
- **Health Checks**: Connection monitoring and circuit breaker
- **Audit Logging**: Secondary file-based data sink for audit trail

### 3. Sample Data
The demo creates sample customer records with:
- Customer ID, Name, Email
- Registration dates and status
- Processing timestamps

## Configuration Highlights

### Database Connection
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

### SQL Operations
```yaml
operations:
  insertCustomer: |
    INSERT INTO customers (customer_id, customer_name, email, status, processed_at, created_at)
    VALUES (:id, :customerName, :email, :status, :processedAt, CURRENT_TIMESTAMP)
  
  upsertCustomer: |
    MERGE INTO customers (customer_id, customer_name, email, status, processed_at)
    KEY (customer_id) VALUES (:id, :customerName, :email, :status, :processedAt)
```

### Error Handling
```yaml
error-handling:
  strategy: "log-and-continue"
  max-retries: 3
  retry-delay: 1000
  dead-letter-enabled: true
  dead-letter-table: "failed_customer_records"
```

### Batch Processing
```yaml
batch:
  enabled: true
  batch-size: 50
  timeout-ms: 10000
  transaction-mode: "per-batch"
  enable-metrics: true
```

## Running the Demo

### Prerequisites
- Java 21+
- Maven 3.6+
- APEX Core library

### Execution
```bash
# From the apex-demo directory
mvn compile exec:java -Dexec.mainClass="dev.mars.apex.demo.etl.CsvToH2PipelineDemo"
```

### Expected Output
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

## Generated Artifacts

After running the demo, you'll find:

### Database Files
- `./target/demo/etl/output/customer_database.mv.db` - H2 database file
- `./target/demo/etl/output/customer_database.trace.db` - H2 trace file (if enabled)

### Audit Files
- `./target/demo/etl/output/audit/customer_audit_*.json` - Audit log files

### Sample Data
- `./target/demo/etl/data/customers.csv` - Generated sample CSV data

## Database Schema

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

## Key Features Demonstrated

### 1. YAML Configuration Integration
- Complete pipeline configuration in YAML
- APEX syntax compliance with kebab-case properties
- Reuse of existing configuration patterns

### 2. Data Sink Framework
- Database data sink with H2 support
- File system data sink for audit logging
- Factory pattern for sink creation

### 3. Error Handling & Resilience
- Retry mechanisms with exponential backoff
- Dead letter handling for failed records
- Circuit breaker for connection failures

### 4. Performance Optimization
- Connection pooling for database efficiency
- Batch processing for high throughput
- Memory management and monitoring

### 5. Monitoring & Observability
- Comprehensive metrics collection
- Health checks and status monitoring
- Performance timing and success rates

## Extending the Demo

This demo can be extended to showcase additional APEX features:

1. **Data Sources**: Add CSV file input using APEX data sources
2. **Rule Processing**: Add business rules for data validation
3. **Enrichments**: Implement data transformation rules
4. **Multiple Outputs**: Route data to different sinks based on conditions
5. **Real-time Processing**: Add file watching for continuous processing

## Troubleshooting

### Common Issues

1. **Database Connection Errors**
   - Ensure target directory exists and is writable
   - Check H2 database file permissions

2. **Configuration Loading Errors**
   - Verify YAML syntax and indentation
   - Check classpath for configuration file

3. **Memory Issues**
   - Adjust batch sizes in configuration
   - Monitor memory usage with JVM flags

### Debug Mode
Add `-Dlogging.level.dev.mars.apex=DEBUG` to enable detailed logging.

## Related Documentation

- [APEX YAML Reference](../../docs/APEX_YAML_REFERENCE.md)
- [Data Sink Framework Design](../../docs/APEX_DATA_PIPELINE_OUTPUT_DESIGN.md)
- [APEX Core Documentation](../../apex-core/README.md)
