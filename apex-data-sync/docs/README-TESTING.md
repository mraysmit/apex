# APEX Data Sync - Testing Guide

## Console Logging for Tests

### Quick Reference

To run tests with DEBUG logging visible in the console:

```bash
# Step 1: Set logging level to DEBUG in src/main/resources/logback.xml
# Change: <logger name="dev.mars.apex" level="INFO"/>
# To:     <logger name="dev.mars.apex" level="DEBUG"/>

# Step 2: Run tests with console output (overrides parent pom redirect setting)
mvn test -Dtest=YourTestClass -Dsurefire.useFile=false
```

### Configuration Details

#### 1. Logback Configuration
File: `src/main/resources/logback.xml`

```xml
<configuration>
    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss} %-5level [%thread] %logger{36} - %msg%n</pattern>
        </encoder>
    </appender>

    <!-- Set to DEBUG for detailed schema reader logs -->
    <logger name="dev.mars.apex" level="DEBUG"/>
    
    <root level="INFO">
        <appender-ref ref="CONSOLE"/>
    </root>
</configuration>
```

#### 2. Maven Surefire Override
The parent pom.xml has `redirectTestOutputToFile=true` by default. Override with:

```bash
-Dsurefire.useFile=false
```

### Examples

```bash
# Run all database schema tests with DEBUG logs to console
mvn test -Dtest=ReadSchemaDatabasePipelineStageTest -Dsurefire.useFile=false

# Run all CSV schema tests with DEBUG logs to console
mvn test -Dtest=ReadSchemaCsvPipelineStageTest -Dsurefire.useFile=false

# Run both test classes
mvn test '-Dtest=ReadSchemaDatabasePipelineStageTest,ReadSchemaCsvPipelineStageTest' -Dsurefire.useFile=false

# Run specific test method with DEBUG logs
mvn test -Dtest=ReadSchemaDatabasePipelineStageTest#shouldReadSchemaFromMultipleTables -Dsurefire.useFile=false

# Run with INFO level (default)
# Change logback.xml back to level="INFO" first
mvn test -Dtest=ReadSchemaDatabasePipelineStageTest -Dsurefire.useFile=false
```

### Debug Log Prefixes

The schema reader uses structured logging prefixes:
- `[SchemaReader]` - General schema reading operations
- `[SchemaReader.DB]` - Database-specific schema operations
- `[SchemaReader.CSV]` - CSV-specific schema operations  
- `[Pipeline.ReadSchema]` - Pipeline execution for read-schema steps
- `[Pipeline.Execute]` - General pipeline execution
- `[Pipeline.Validation]` - Pipeline validation steps

### Viewing Logs in Files (Alternative)

If you prefer file-based logging (default):

```bash
# Run tests normally (output goes to file)
mvn test -Dtest=ReadSchemaDatabasePipelineStageTest

# View the output
cat target/surefire-reports/dev.mars.apex.sync.ReadSchemaDatabasePipelineStageTest-output.txt

# Filter for specific logs
Get-Content target/surefire-reports/*.txt | Select-String -Pattern "\[SchemaReader"
```

## Test Organization

### Report Output Configuration

Schema analysis reports can be generated automatically during test execution. The report output path can be configured with automatic directory handling:

**Configuration Options**:
```yaml
parameters:
  # Option 1: Filename only - saved to default 'reports/' directory
  report-output: "schema-report.html"  # → reports/schema-report.html
  
  # Option 2: Relative path - creates directories as needed
  report-output: "target/reports/analysis.html"  # → target/reports/analysis.html
  
  # Option 3: Full path with subdirectories
  report-output: "output/database-analysis/schema.html"  # → output/database-analysis/schema.html
```

**Automatic Behavior**:
- If only a filename is provided, the report is saved to the `reports/` directory
- Parent directories are automatically created if they don't exist
- No manual directory creation needed

### Database Schema Tests
**ReadSchemaDatabasePipelineStageTest**: Tests for reading schema from database tables
- `shouldReadSchemaFromDatabase()` - Single H2 table (3 columns)
- `shouldReadSchemaFromMultipleTables()` - 5 H2 tables (30 total columns: customers-5, orders-6, products-7, inventory-4, transactions-8)

### CSV Schema Tests
**ReadSchemaCsvPipelineStageTest**: Tests for reading schema from CSV files with type inference
- `shouldReadSchemaFromCsv()` - Small CSV file (4 columns)
- `shouldReadSchemaFromLargeCsv()` - Large CSV with 11 columns (demonstrates INTEGER, VARCHAR, DECIMAL, BOOLEAN, TIMESTAMP inference)

### Data Sync Tests
**MsSqlToPostgresSyncTest**: MS SQL Server to PostgreSQL synchronization tests

## Summary

**For console DEBUG logging:**
1. Edit `src/main/resources/logback.xml` → set `level="DEBUG"`
2. Run with `-Dsurefire.useFile=false`

**Revert to normal:**
1. Edit `src/main/resources/logback.xml` → set `level="INFO"`
2. Run normally (logs go to `target/surefire-reports/*.txt`)
