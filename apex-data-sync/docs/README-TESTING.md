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

### Directory Structure

The test suite is organized into 5 functional categories:

```
src/test/java/dev/mars/apex/sync/
├── unit/                           # Unit tests for individual components
│   ├── comparison/                 # Type compatibility, breaking change detection
│   ├── serialization/              # JSON serialization/deserialization
│   ├── reporting/                  # Report generation (Markdown, HTML, JSON)
│   │   └── markdown/               # Markdown-specific tests
│   └── util/                       # Utility classes and helpers
├── integration/                    # Integration tests across components
│   ├── schema/                     # Schema reading and discovery
│   │   ├── database/               # Database schema reading
│   │   └── csv/                    # CSV schema reading
│   ├── comparison/                 # Schema comparison workflows
│   │   ├── scenarios/              # Schema evolution scenarios
│   │   └── platforms/              # Cross-platform comparisons
│   └── sync/                       # Data synchronization workflows
├── pipeline/                       # Complete pipeline orchestration
│   ├── workflows/                  # End-to-end workflows
│   ├── validation/                 # Pipeline validation
│   └── error_handling/             # Error recovery and handling
├── performance/                    # Performance and scalability tests
│   ├── benchmarks/                 # Performance benchmarks
│   └── scalability/                # Large dataset tests
└── support/                        # Test infrastructure
    ├── fixtures/                   # Test data and fixtures
    ├── containers/                 # Testcontainers support
    └── base/                       # Base test classes
```

**For detailed test planning**, see:
- [TEST_ORGANIZATION_PROPOSAL.md](../docs-design/testing/TEST_ORGANIZATION_PROPOSAL.md) - 52 new tests across 3 phases
- [FUNCTIONAL_TEST_MAPPING.md](../docs-design/testing/FUNCTIONAL_TEST_MAPPING.md) - 44 tests by function category

### Test Naming Conventions

All tests follow the pattern: `shouldXxxWhenYyy()` or `shouldXxx()`

Examples:
- `shouldSerializeSchemaMetadata()` - Basic positive test
- `shouldHandleNullValues()` - Edge case handling
- `shouldRejectIncompatibleTypes()` - Negative test
- `shouldGenerateMarkdownWithEmoji()` - Feature-specific test

### Running Tests by Category

```bash
# Run all unit tests
mvn test -Dtest=unit/**

# Run specific category
mvn test -Dtest=unit/serialization/**
mvn test -Dtest=integration/schema/**
mvn test -Dtest=pipeline/**

# Run specific test class
mvn test -Dtest=JsonSerializationTest
mvn test -Dtest=MarkdownGenerationTest

# Run with console output (DEBUG logging)
mvn test -Dtest=JsonSerializationTest -Dsurefire.useFile=false
```

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

### Current Test Coverage

#### Unit Tests (17 tests)
- **JsonSerializationTest** (8 tests) - Week 1 deliverable ✅
  - Basic serialization/deserialization, round-trip validation
  - Unicode handling (客户表, Straße), null values, empty schemas
  - Numeric precision (DECIMAL 10,2), multiple column types
  
- **MarkdownGenerationTest** (9 tests) - Week 3 deliverable ✅
  - Basic table generation, emoji headers (📊, 🔑, ✓)
  - Table alignment, special character escaping (|, *)
  - Unicode, summary sections, constraint indicators

#### Integration Tests (11 tests)
- **ReadSchemaDatabaseTest** (6 tests) - Database schema reading
  - `shouldReadSchemaFromDatabase()` - Single H2 table (3 columns)
  - `shouldReadSchemaFromMultipleTables()` - 5 H2 tables (30 total columns)
  - PostgreSQL, H2, and multi-table scenarios
  
- **ReadSchemaCsvTest** (2 tests) - CSV schema reading with type inference
  - `shouldReadSchemaFromCsv()` - Small CSV file (4 columns)
  - `shouldReadSchemaFromLargeCsv()` - Large CSV with 11 columns (INTEGER, VARCHAR, DECIMAL, BOOLEAN, TIMESTAMP inference)
  
- **SchemaEvolutionBreakingTest** (3 tests) - Breaking change detection
  - Schema evolution scenarios with breaking change validation

#### Pipeline Tests (5 tests)
- **CompletePipelineWithSchemaTest** - Full pipeline with schema analysis
- **MsSqlToPostgresSyncTest** - MS SQL Server to PostgreSQL synchronization
- **SchemaDiffJsonIntegrationTest** - JSON-based schema diff integration
- **SyncPipelineH2Test** - H2 database synchronization
- **SyncPipelineContainersTest** - Testcontainers-based sync tests

#### Validation Tests (6 tests)
- **CsvToPostgresMigrationValidationTest** - CSV to PostgreSQL migration
- **MultiTableMigrationValidationTest** - Multi-table migration scenarios
- **PreDeploymentValidationTest** - Pre-deployment validation checks
- **SchemaDiffReportOutputOptionsTest** - Report output configuration
- **SchemaEvolutionValidationTest** - Schema evolution validation
- **SqlServerPostgresMigrationValidationTest** - SQL Server to PostgreSQL migration

**Total: 39 tests (19 existing + 17 new + 3 moved)**

### Test Implementation Status

**Phase 1 (Week 1-2) - CRITICAL**: 17/22 tests completed
- ✅ JsonSerializationTest (8 tests) - Week 1 deliverable
- ✅ MarkdownGenerationTest (9 tests) - Week 3 deliverable
- ⏳ TypeCompatibilityTest (8 tests) - HIGH priority, core logic
- ⏳ BreakingChangeDetectionTest (2 tests) - HIGH priority
- ⏳ ReportPathResolutionTest (5 tests) - MEDIUM priority

**Phase 2 (Week 3-4)**: 11 tests planned
- Cross-platform database tests (SQL Server, MySQL)
- CSV edge cases (empty files, malformed data, encoding)

**Phase 3 (Week 5-6)**: 19 tests planned
- Error handling and recovery
- Performance benchmarks and scalability

**Target**: 71 total tests (39 current + 32 planned)

## Summary

**For console DEBUG logging:**
1. Edit `src/main/resources/logback.xml` → set `level="DEBUG"`
2. Run with `-Dsurefire.useFile=false`

**Revert to normal:**
1. Edit `src/main/resources/logback.xml` → set `level="INFO"`
2. Run normally (logs go to `target/surefire-reports/*.txt`)
