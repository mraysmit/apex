# APEX Data-Sync - Running Tests

Quick reference for running tests in the apex-data-sync module.

> **Full Documentation**: See [APEX_DATA_SYNC_TESTING_GUIDE.md](APEX_DATA_SYNC_TESTING_GUIDE.md) for comprehensive test coverage analysis and organization details.

---

## Quick Start

```bash
# Run all tests
mvn test

# Run all tests with console output
mvn test -Dsurefire.useFile=false

# Run specific test class
mvn test -Dtest=ReadSchemaDatabasePipelineStageTest -Dsurefire.useFile=false

# Run specific test method
mvn test -Dtest=ReadSchemaDatabasePipelineStageTest#shouldReadSchemaFromMultipleTables -Dsurefire.useFile=false
```

---

## Running Tests by Category

```bash
# Schema reading tests
mvn test -Dtest=ReadSchema* -Dsurefire.useFile=false

# Validation tests
mvn test -Dtest=*ValidationTest -Dsurefire.useFile=false

# Pipeline tests
mvn test -Dtest=*PipelineTest -Dsurefire.useFile=false

# CSV tests
mvn test -Dtest=*Csv* -Dsurefire.useFile=false

# Multiple test classes
mvn test '-Dtest=ReadSchemaDatabaseTest,ReadSchemaCsvTest' -Dsurefire.useFile=false
```

---

## Enabling DEBUG Logging

### Step 1: Edit logback.xml
File: `src/main/resources/logback.xml`

```xml
<!-- Change INFO to DEBUG -->
<logger name="dev.mars.apex" level="DEBUG"/>
```

### Step 2: Run with Console Output
```bash
mvn test -Dtest=YourTestClass -Dsurefire.useFile=false
```

### Debug Log Prefixes
| Prefix | Description |
|--------|-------------|
| `[SchemaReader]` | General schema reading |
| `[SchemaReader.DB]` | Database schema operations |
| `[SchemaReader.CSV]` | CSV schema operations |
| `[Pipeline.ReadSchema]` | Pipeline read-schema steps |
| `[Pipeline.Execute]` | Pipeline execution |

---

## Viewing Test Output Files

When running without `-Dsurefire.useFile=false`, output goes to files:

```bash
# View test output
cat target/surefire-reports/dev.mars.apex.sync.ReadSchemaDatabaseTest-output.txt

# Filter for specific logs (PowerShell)
Get-Content target/surefire-reports/*.txt | Select-String -Pattern "\[SchemaReader"

# Filter for specific logs (Bash)
grep "\[SchemaReader" target/surefire-reports/*.txt
```

---

## Current Test Summary

| Category | Count | Examples |
|----------|-------|----------|
| Schema Reading | 8 | `ReadSchemaDatabaseTest`, `ReadSchemaCsvTest` |
| Validation | 6 | `CsvToPostgresMigrationTest`, `PreDeploymentValidationTest` |
| Pipeline | 5 | `CompletePipelineWithSchemaTest`, `SchemaDiffJsonIntegrationTest` |
| Integration | 11 | `CustomSchemaPostgresTest`, `TableSyncIntegrationTestH2` |
| **Total** | **39** | |

---

## Common Test Examples

### Database Schema Reading
```bash
mvn test -Dtest=ReadSchemaDatabasePipelineStageTest -Dsurefire.useFile=false
```

### CSV to PostgreSQL Migration Validation
```bash
mvn test -Dtest=CsvToPostgresMigrationTest -Dsurefire.useFile=false
```

### Complete Pipeline Execution
```bash
mvn test -Dtest=CompletePipelineWithSchemaTest -Dsurefire.useFile=false
```

### SQL Server to PostgreSQL Migration
```bash
mvn test -Dtest=SqlServerPostgresMigrationTest -Dsurefire.useFile=false
```

### Multi-Table Operations
```bash
mvn test -Dtest=MultiTableMigrationTest -Dsurefire.useFile=false
```

---

## Troubleshooting

| Issue | Solution |
|-------|----------|
| No console output | Add `-Dsurefire.useFile=false` |
| Tests not found | Check package path in `-Dtest=` argument |
| Testcontainer failures | Ensure Docker is running |
| Connection timeouts | Check database availability |

---

**Version**: 2.1 | **Updated**: January 2026
