# APEX Read Schema Pipeline Stage Guide

## Overview

The `read-schema` pipeline stage allows you to read schema metadata from various data sources including databases and CSV files. This feature was introduced in APEX 2.1 as part of the database table synchronization module.

## Features

- Read schema from database tables using INFORMATION_SCHEMA
- Read schema from CSV files by analyzing headers and data types  
- Extract column metadata including:
  - Column names
  - Data types
  - Nullable flags
  - Maximum length
  - Precision and scale (for numeric types)
  - Primary key flags (database only)
- Stores schema metadata in pipeline context for downstream stages

## Pipeline Configuration

### Database Schema Reading

```yaml
metadata:
  id: "read-db-schema"
  name: "Database Schema Reader"
  version: "1.0"
  type: "pipeline-config"

data-sources:
  - name: "my-database"
    type: "database"
    source-type: "h2"  # or postgresql, mysql, mssql, etc.
    connection:
      database: "mem:schema_test"
      username: "sa"
      password: ""
    enabled: true

pipeline:
  name: "read-database-schema"
  execution:
    mode: "sequential"
  
  steps:
    - name: "read-schema"
      type: "read-schema"
      source: "my-database"
      description: "Read schema metadata from database table"
      parameters:
        table: "customers"
```

### CSV File Schema Reading

```yaml
metadata:
  id: "read-csv-schema"
  name: "CSV Schema Reader"
  version: "1.0"
  type: "pipeline-config"

data-sources:
  - name: "csv-source"
    type: "file-system"
    source-type: "file"
    enabled: true
    connection:
      base-path: "."  # Base directory for file operations

pipeline:
  name: "read-csv-schema"
  execution:
    mode: "sequential"
  
  steps:
    - name: "read-schema"
      type: "read-schema"
      source: "csv-source"
      description: "Read schema metadata from CSV file"
      parameters:
        file: "customers.csv"
```

## Schema Metadata Structure

The schema metadata is stored in the pipeline context with the following structure:

```java
SchemaMetadata {
    String sourceName;           // Name of the source (table or file)
    DataSourceType sourceType;   // DATABASE or FILE_SYSTEM
    List<ColumnDefinition> columns;
}

ColumnDefinition {
    String name;                 // Column name
    String dataType;            // Data type (e.g., VARCHAR, INTEGER)
    boolean nullable;           // Whether column can be null
    boolean primaryKey;         // Whether column is primary key
    Integer maxLength;          // Maximum length (for string types)
    Integer precision;          // Precision (for numeric types)
    Integer scale;              // Scale (for numeric types)
}
```

## Accessing Schema Metadata

Schema metadata is stored in the pipeline context as `"schemaMetadata"` and can be accessed by subsequent pipeline stages:

```java
// In your pipeline step execution
SchemaMetadata schema = (SchemaMetadata) context.get("schemaMetadata");

// Iterate over columns
for (SchemaMetadata.ColumnDefinition column : schema.getColumns()) {
    System.out.println("Column: " + column.getName());
    System.out.println("  Type: " + column.getDataType());
    System.out.println("  Nullable: " + column.isNullable());
    System.out.println("  Primary Key: " + column.isPrimaryKey());
}
```

## Supported Databases

The read-schema stage supports all database types that expose INFORMATION_SCHEMA:

- PostgreSQL
- MySQL/MariaDB
- Microsoft SQL Server
- H2
- Oracle (via DBA_TAB_COLUMNS)
- DB2

## CSV Type Inference

When reading schema from CSV files, the stage analyzes the data to infer column types:

- **INTEGER**: Pure numeric values without decimals
- **DECIMAL**: Numeric values with decimal points
- **BOOLEAN**: Values like "true", "false", "yes", "no", "1", "0"
- **DATE**: Values matching ISO date patterns (yyyy-MM-dd)
- **VARCHAR**: Default for all other values

Maximum length is calculated for VARCHAR fields based on the longest value in the sample data.

## Example Use Cases

### 1. Schema Validation Before ETL

```yaml
steps:
  - name: "validate-source-schema"
    type: "read-schema"
    source: "source-db"
    parameters:
      table: "source_table"
  
  - name: "validate-target-schema"
    type: "read-schema"
    source: "target-db"
    parameters:
      table: "target_table"
  
  - name: "compare-schemas"
    type: "transform"
    # Custom transformation to compare schemas
  
  - name: "extract-data"
    type: "extract"
    source: "source-db"
    # Proceed with ETL if schemas match
```

### 2. Dynamic Table Creation

```yaml
steps:
  - name: "read-csv-structure"
    type: "read-schema"
    source: "csv-file"
    parameters:
      file: "import.csv"
  
  - name: "create-table"
    type: "transform"
    # Use schema metadata to generate CREATE TABLE statement
  
  - name: "load-data"
    type: "load"
    target: "database"
```

### 3. Data Type Mapping

```yaml
steps:
  - name: "read-oracle-schema"
    type: "read-schema"
    source: "oracle-db"
    parameters:
      table: "customers"
  
  - name: "map-to-postgres-types"
    type: "transform"
    # Convert Oracle types to PostgreSQL equivalents
  
  - name: "create-postgres-table"
    type: "load"
    target: "postgres-db"
```

## Implementation Details

### Key Classes

- **SchemaReaderService**: Main service for reading schemas
  - `readSchema()`: Dispatches to database or CSV reader
  - `readDatabaseSchema()`: Queries INFORMATION_SCHEMA
  - `readCsvSchema()`: Parses CSV and infers types

- **SchemaMetadata**: Model class for schema information
  - Contains source name, type, and column definitions
  - Immutable after creation

- **PipelineExecutor**: Orchestrates pipeline execution
  - `executeReadSchemaStep()`: Executes read-schema steps
  - Stores result in pipeline context

### Database Schema Query

The service uses the following SQL query pattern:

```sql
SELECT 
    COLUMN_NAME,
    DATA_TYPE,
    IS_NULLABLE,
    CHARACTER_MAXIMUM_LENGTH,
    NUMERIC_PRECISION,
    NUMERIC_SCALE
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_NAME = ?
ORDER BY ORDINAL_POSITION
```

Primary key detection is performed via:

```sql
SELECT COLUMN_NAME
FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE
WHERE TABLE_NAME = ?
  AND CONSTRAINT_NAME IN (
    SELECT CONSTRAINT_NAME
    FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS
    WHERE TABLE_NAME = ?
      AND CONSTRAINT_TYPE = 'PRIMARY KEY'
  )
```

## Testing

Comprehensive integration tests are available in:
- `apex-data-sync/src/test/java/dev/mars/apex/sync/ReadSchemaPipelineStageTest.java`

Test coverage includes:
- Reading schema from H2 database
- Reading schema from CSV files
- Column metadata extraction
- Type inference for CSV
- Pipeline context storage

Run tests with:
```bash
cd apex-data-sync
mvn test -Dtest=ReadSchemaPipelineStageTest
```

## Best Practices

1. **Always validate schemas** before performing ETL operations
2. **Store schema metadata** in pipeline context for reuse
3. **Handle type differences** between source and target databases
4. **Use CSV schema reading** for data profiling and validation
5. **Check nullable flags** before inserting data
6. **Verify primary keys** to ensure data integrity

## Future Enhancements

Potential improvements for future versions:

- Foreign key detection
- Index information extraction
- Column constraints (CHECK, UNIQUE)
- Extended data type metadata
- Schema comparison utilities
- Automated type mapping between databases

## See Also

- [APEX Pipeline Orchestration Guide](APEX_DATA_PIPELINE_ORCHESTRATION_GUIDE.md)
- [APEX External Data Source Guide](APEX_README.md)
- [APEX Data Sync Module](../apex-data-sync/README.md)
