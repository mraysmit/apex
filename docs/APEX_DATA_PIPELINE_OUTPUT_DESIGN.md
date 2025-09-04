# APEX Data Pipeline Output Design

## Overview

This document outlines the design for implementing comprehensive data pipeline output capabilities in APEX, addressing the current gap where APEX excels at data input and enrichment but lacks explicit output/sink configurations.

## Current State Analysis

### Existing Capabilities ✅

**CSV Input Support:**
- Rich file-system data sources with CSV parsing
- Column mappings, data type conversions, field transformations
- Support for headers, delimiters, encoding, file watching

**H2 Database Support:**
- Complete H2 database connectivity (file-based and in-memory)
- Connection pooling, custom parameters, compatibility modes
- Query execution and result processing

**Data Transformation:**
- Comprehensive enrichment capabilities
- Field mappings, calculations, conditional transformations
- Complex data processing workflows

### Current Gaps ❌

1. **No Output/Sink Configurations**: APEX focuses on data input and enrichment but lacks explicit output destinations
2. **Read-Only Database Usage**: Database connections primarily used for reading data, not writing
3. **Missing Pipeline Orchestration**: No built-in YAML configuration for complete data pipeline outputs
4. **Limited Batch Processing**: No native support for batch output operations

## Design Proposal

### Architecture Overview

```
Input Sources → APEX Processing → Output Sinks
     ↓              ↓              ↓
CSV Files → Enrichment/Transform → H2 Database
JSON/XML → Rule Processing → File Output
REST APIs → Data Validation → Message Queues
```

### Core Components

#### 1. DataSink Interface

```java
public interface DataSink {
    void initialize(DataSinkConfiguration config);
    void write(Object data) throws DataSinkException;
    void writeBatch(List<Object> data) throws DataSinkException;
    void close();
    boolean isHealthy();
}
```

#### 2. Pipeline Engine

```java
public class PipelineExecutor {
    public void execute(PipelineConfiguration config);
    public void executeBatch(PipelineConfiguration config, int batchSize);
    public PipelineStatus getStatus(String pipelineId);
}
```

### YAML Configuration Structure

#### Complete Pipeline Example

```yaml
metadata:
  name: "CSV to H2 Data Pipeline"
  version: "1.0.0"
  description: "Load CSV data, transform, and write to H2 database"
  type: "data-pipeline-config"

# Input Configuration (Existing)
dataSources:
  - name: "customer-csv-input"
    type: "file-system"
    enabled: true
    connection:
      basePath: "./data/input"
      filePattern: "customers_*.csv"
      watchForChanges: true
    fileFormat:
      type: "csv"
      hasHeaderRow: true
      columnMappings:
        "customer_id": "customerId"
        "customer_name": "customerName"
        "email_address": "email"
        "registration_date": "registeredAt"
      columnTypes:
        "customerId": "integer"
        "customerName": "string"
        "email": "string"
        "registeredAt": "date"

# Output Configuration (NEW)
dataSinks:
  - name: "customer-h2-output"
    type: "database"
    sourceType: "h2"
    enabled: true
    description: "Target H2 database for processed customer data"
    
    connection:
      database: "./target/output/customer_data"
      username: "sa"
      password: ""
      mode: "PostgreSQL"
      
      # Connection pool for output operations
      connectionPool:
        maxSize: 10
        minSize: 2
        connectionTimeout: 30000
    
    # Output operations
    operations:
      insertCustomer: |
        INSERT INTO customers (customer_id, customer_name, email, registered_at, processed_at, status)
        VALUES (:customerId, :customerName, :email, :registeredAt, :processedAt, :status)
      
      updateCustomer: |
        UPDATE customers 
        SET customer_name = :customerName, email = :email, last_updated = :processedAt
        WHERE customer_id = :customerId
      
      upsertCustomer: |
        MERGE INTO customers (customer_id, customer_name, email, registered_at, processed_at, status)
        KEY (customer_id)
        VALUES (:customerId, :customerName, :email, :registeredAt, :processedAt, :status)
    
    # Schema management
    schema:
      autoCreate: true
      initScript: |
        CREATE TABLE IF NOT EXISTS customers (
          customer_id INTEGER PRIMARY KEY,
          customer_name VARCHAR(255) NOT NULL,
          email VARCHAR(255) UNIQUE,
          registered_at DATE,
          processed_at TIMESTAMP,
          status VARCHAR(50) DEFAULT 'ACTIVE'
        );
    
    # Error handling
    errorHandling:
      strategy: "log-and-continue"
      deadLetterTable: "failed_records"
      maxRetries: 3
      retryDelay: 1000

# Transformation Configuration (Enhanced)
enrichments:
  - id: "customer-data-enrichment"
    type: "field-transformation"
    description: "Enrich and validate customer data for output"
    condition: "true"
    
    transformation-rules:
      # Data cleaning
      - condition: "#customerName != null"
        actions:
          - type: "set-field"
            field: "customerName"
            expression: "#customerName.trim().toUpperCase()"
      
      # Email normalization
      - condition: "#email != null"
        actions:
          - type: "set-field"
            field: "email"
            expression: "#email.toLowerCase().trim()"
      
      # Add processing metadata
      - condition: "true"
        actions:
          - type: "set-field"
            field: "processedAt"
            expression: "new java.util.Date()"
          - type: "set-field"
            field: "status"
            expression: "'PROCESSED'"

# Pipeline Configuration (NEW)
pipelines:
  - name: "customer-processing-pipeline"
    description: "Complete customer data processing pipeline"
    enabled: true
    
    # Source configuration
    source:
      dataSource: "customer-csv-input"
      batchSize: 100
      processingMode: "streaming"  # or "batch"
    
    # Processing steps
    processing:
      enrichments:
        - "customer-data-enrichment"
      
      validation:
        enabled: true
        rules:
          - field: "customerId"
            required: true
            type: "integer"
          - field: "email"
            required: true
            pattern: "^[A-Za-z0-9+_.-]+@(.+)$"
    
    # Output configuration
    sink:
      dataSink: "customer-h2-output"
      operation: "upsertCustomer"
      batchSize: 50
      
      # Output routing
      routing:
        - condition: "#status == 'NEW'"
          operation: "insertCustomer"
        - condition: "#status == 'UPDATE'"
          operation: "updateCustomer"
        - condition: "true"  # default
          operation: "upsertCustomer"
    
    # Scheduling
    scheduling:
      enabled: true
      cronExpression: "0 */5 * * * *"  # Every 5 minutes
      timezone: "UTC"
    
    # Monitoring
    monitoring:
      enabled: true
      metrics:
        - "records-processed"
        - "processing-time"
        - "error-rate"
      alerts:
        - condition: "error-rate > 0.05"
          action: "email-notification"

# Multiple Output Destinations
  - name: "customer-multi-output-pipeline"
    description: "Pipeline with multiple output destinations"
    
    source:
      dataSource: "customer-csv-input"
    
    processing:
      enrichments:
        - "customer-data-enrichment"
    
    # Multiple sinks
    sinks:
      - dataSink: "customer-h2-output"
        operation: "upsertCustomer"
        condition: "#status == 'ACTIVE'"
      
      - dataSink: "audit-file-output"
        operation: "writeAuditRecord"
        condition: "true"  # Always write audit
      
      - dataSink: "notification-queue"
        operation: "sendNotification"
        condition: "#customerName.contains('VIP')"
```

## Implementation Plan

### Phase 1: Core Infrastructure

#### 1.1 DataSink Framework
- Create `DataSink` interface and base implementations
- Implement `DatabaseDataSink` for H2/PostgreSQL/MySQL
- Add `FileSystemDataSink` for CSV/JSON output
- Create `DataSinkConfiguration` classes

#### 1.2 YAML Configuration Support
- Extend `YamlRuleConfiguration` with `dataSinks` property
- Create `YamlDataSink` configuration class
- Update `YamlConfigurationLoader` to parse sink configurations
- Add validation for sink configurations

#### 1.3 Basic Pipeline Engine
- Implement `PipelineExecutor` for simple source→sink flows
- Add batch processing capabilities
- Create error handling and retry mechanisms
- Implement basic monitoring and logging

### Phase 2: Advanced Features

#### 2.1 Enhanced Pipeline Configuration
- Add `YamlPipeline` configuration support
- Implement conditional routing to multiple sinks
- Add scheduling and cron-based execution
- Create pipeline status and monitoring APIs

#### 2.2 Schema Management
- Auto-creation of database tables from data structure
- Schema migration and versioning support
- Data type mapping between sources and sinks
- Constraint validation and enforcement

#### 2.3 Performance Optimization
- Connection pooling for database sinks
- Asynchronous processing capabilities
- Memory-efficient batch processing
- Parallel pipeline execution

### Phase 3: Enterprise Features

#### 3.1 Advanced Error Handling
- Dead letter queues for failed records
- Configurable retry strategies
- Data quality reporting
- Recovery and replay mechanisms

#### 3.2 Monitoring and Observability
- Comprehensive metrics collection
- Pipeline health checks
- Performance monitoring
- Integration with monitoring systems

#### 3.3 Additional Sink Types
- Message queue sinks (Kafka, RabbitMQ)
- REST API output sinks
- Cloud storage sinks (S3, Azure Blob)
- NoSQL database sinks (MongoDB, Cassandra)

## Technical Considerations

### Database Connection Management
- Separate connection pools for read and write operations
- Transaction management for batch operations
- Connection health monitoring and failover
- Support for multiple database types

### Data Consistency
- Transactional batch processing
- Rollback capabilities for failed batches
- Idempotent operations for retry scenarios
- Data validation before output

### Performance
- Configurable batch sizes for optimal throughput
- Memory management for large datasets
- Parallel processing where appropriate
- Efficient data serialization

### Security
- Secure credential management for output destinations
- Encryption for sensitive data in transit
- Audit logging for all output operations
- Access control for pipeline configurations

## Migration Strategy

### Backward Compatibility
- Existing APEX configurations remain unchanged
- New features are opt-in through configuration
- Gradual migration path for existing users
- Clear deprecation timeline for old patterns

### Documentation and Examples
- Comprehensive configuration examples
- Migration guides from current patterns
- Best practices documentation
- Performance tuning guidelines

## Success Metrics

### Functional Metrics
- Support for CSV→H2 pipeline (primary use case)
- Batch processing of 10,000+ records
- Sub-second latency for small batches
- 99.9% data consistency guarantee

### Operational Metrics
- Zero-downtime deployment of new pipelines
- Comprehensive error reporting and recovery
- Integration with existing APEX monitoring
- Clear performance characteristics

## Conclusion

This design provides a comprehensive solution for APEX data pipeline outputs while maintaining the framework's existing strengths in data input and enrichment. The phased implementation approach ensures backward compatibility while delivering immediate value for common use cases like CSV to H2 database pipelines.

The proposed architecture extends APEX's current capabilities naturally, leveraging existing patterns for configuration and processing while adding the missing output layer that completes the data pipeline story.
