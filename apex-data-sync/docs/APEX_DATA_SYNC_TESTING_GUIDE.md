# APEX Data-Sync - Comprehensive Testing Guide

**Version:** 2.1  
**Date:** 2026-01-21  
**Author:** Mark Andrew Ray-Smith Cityline Ltd

> **Consolidated Document**: This guide combines functional test mapping, test organization, YAML co-location patterns, and testing best practices into a single comprehensive reference.

---

## Table of Contents

1. [Quick Reference](#quick-reference)
2. [Function Category Explanations](#function-category-explanations)
3. [Detailed Function Testing](#detailed-function-testing)
   - [1. Schema Reading & Discovery](#1-schema-reading--discovery)
   - [2. Schema Comparison & Diff](#2-schema-comparison--diff)
   - [3. Report Generation](#3-report-generation)
   - [4. Data Synchronization](#4-data-synchronization)
   - [5. Pipeline Orchestration](#5-pipeline-orchestration)
   - [6. Data Source Integration](#6-data-source-integration)
4. [Test Organization & Structure](#test-organization--structure)
5. [YAML Co-Location Pattern](#yaml-co-location-pattern)
6. [Console Logging for Tests](#console-logging-for-tests)
7. [Summary by Priority](#summary-by-priority)
8. [Priority Implementation Plan](#priority-implementation-plan)
9. [Test Count Summary](#test-count-summary)

---

## Quick Reference

### Running Tests

```bash
# Run all tests with console output
mvn test -Dsurefire.useFile=false

# Run specific test class
mvn test -Dtest=ReadSchemaDatabasePipelineStageTest -Dsurefire.useFile=false

# Run with DEBUG logging (edit logback.xml first to set level="DEBUG")
mvn test -Dtest=YourTestClass -Dsurefire.useFile=false

# Run tests by category
mvn test -Dtest=unit/**
mvn test -Dtest=integration/schema/**
mvn test -Dtest=pipeline/**
```

### Current Test Status

| Category | Count | Status |
|----------|-------|--------|
| Unit Tests | 17 | Phase 1 deliverable |
| Integration Tests | 11 | Partial coverage |
| Pipeline Tests | 5 | Basic flows covered |
| Validation Tests | 6 | Core scenarios |
| **Total Current** | **39** | |
| **Planned New** | **32** | Phases 2-3 |
| **Target Total** | **71** | 274% increase |

---

## Function Category Explanations

### Category 1: Schema Reading & Discovery

**What It Does**: Extracts schema metadata from various data sources and converts it into APEX's internal `SchemaMetadata` format. This is the "input layer" that reads structure (not data) from databases, CSV files, or other sources.

**Why It's Critical**:
- **Source-Agnostic Foundation**: All downstream comparison logic depends on having schemas in a unified format
- **Platform Independence**: Whether reading from PostgreSQL, SQL Server, or CSV, the output is always the same `SchemaMetadata` object
- **Type Inference**: For CSV files, automatically detects data types by sampling rows (e.g., "123" → INTEGER, "2026-01-18" → TIMESTAMP)

**Key Capabilities**:
- Database schema reading via JDBC (queries `INFORMATION_SCHEMA`)
- CSV type inference with configurable sampling
- Multi-table enumeration (read entire databases)
- Platform-specific type mappings

---

### Category 2: Schema Comparison & Diff

**What It Does**: The **core brain** of apex-data-sync - compares two `SchemaMetadata` objects and produces detailed analysis of differences. Detects breaking changes, type compatibility issues, and migration risks.

**Why It's Critical**:
- **Migration Safety**: Prevents data loss by detecting incompatible changes before migration
- **Automated Analysis**: Replaces manual spreadsheet comparisons with automated validation
- **Multi-Dimensional**: Checks column names, types, sizes, nullability, primary keys, defaults

**Key Capabilities**:
- Breaking change detection (type narrowing, constraint tightening)
- Type compatibility with custom mappings (`NVARCHAR` ↔ `VARCHAR`)
- Multi-table database comparison
- Severity classification (INFO, WARNING, BREAKING)

**Example Breaking Changes**:
```
VARCHAR(200) → VARCHAR(100)  // Data truncation risk
NULL allowed → NOT NULL      // Existing nulls would fail
DECIMAL(10,2) → DECIMAL(8,2) // Precision loss
```

---

### Category 3: Report Generation

**What It Does**: Renders `SchemaComparisonResult` objects into human-readable formats: JSON (canonical), HTML (visual), and Markdown (portable). This is the "output layer" that makes comparison results accessible.

**Why It's Critical**:
- **Multi-Audience**: Developers read JSON, analysts read HTML, management reads Markdown
- **JSON-First Architecture**: Canonical JSON format enables programmatic API access
- **Template-Based**: Can customize reports without changing Java code

**Key Capabilities**:
- JSON serialization following `schema-diff-v1.0.json` schema
- Markdown generation with emoji headers and tables
- HTML reports with inline CSS and color-coding
- Report path resolution (auto-create directories)

**Three-Layer Architecture**:
```
Layer 1: Domain Model (SchemaComparisonResult)
         ↓
Layer 2: JSON Serialization (Canonical Format)
         ↓
Layer 3: Presentation (HTML/Markdown templates)
```

---

### Category 4: Data Synchronization

**What It Does**: Copies data from source to target databases after schema validation. This is the "ETL layer" - Extract, Transform, Load with APEX validation rules applied during transformation.

**Why It's Critical**:
- **Completes the Migration**: Schema validation is step 1, data sync is step 2
- **APEX-Powered**: Can apply validation rules during sync (e.g., reject invalid emails)
- **Batch Processing**: Handles large datasets efficiently
- **Upsert Support**: Can update existing rows or insert new ones

**Key Capabilities**:
- Extract: Execute SQL queries from named query configs
- Transform: Apply APEX validation rules to data in-flight
- Load: Batch INSERT or UPSERT with configurable batch sizes
- Transaction boundaries with rollback on failure

---

### Category 5: Pipeline Orchestration

**What It Does**: Coordinates multi-step workflows by chaining together schema reading, comparison, reporting, and synchronization steps. This is the "control layer" that makes complex migrations declarative.

**Why It's Critical**:
- **Zero-Code Migrations**: End users write YAML, not Java
- **Context Passing**: Results from step 1 feed into step 2 automatically
- **Dependency Management**: Steps execute in correct order
- **Error Handling**: Pipeline can fail fast or continue on errors

**Key Capabilities**:
- Sequential and parallel execution modes
- Pipeline context management (store/retrieve intermediate results)
- Data source reference resolution (external YAML configs)
- Step dependency tracking

**Pipeline Context Keys**:
```
schema_<stepName>      → SchemaMetadata object
schema_diff_<stepName> → SchemaComparisonResult
```

---

### Category 6: Data Source Integration

**What It Does**: Manages connections to databases and file systems. This is the "infrastructure layer" that handles JDBC connection pooling, credential management, and platform-specific drivers.

**Why It's Critical**:
- **Connection Pooling**: Reuses database connections efficiently (HikariCP)
- **Credential Security**: Supports environment variable substitution (`${DB_PASSWORD}`)
- **Multi-Platform**: PostgreSQL, SQL Server, MySQL, Oracle, H2
- **Named Queries**: Reusable SQL queries in data-source configs

**Key Capabilities**:
- HikariCP connection pooling with configurable timeouts
- Platform-specific JDBC drivers (PostgreSQL, SQL Server, MySQL, Oracle)
- Dual-format query support (legacy vs target platform)
- Environment variable substitution for credentials

---

### How Categories Work Together

**Complete End-to-End Migration Workflow**:

```yaml
# Category 6: Establish database connections
data-source-refs:
  - name: "sqlserver-legacy"
    source: "data-sources/sqlserver-prod.yaml"
  - name: "postgres-target"
    source: "data-sources/postgres-prod.yaml"

# Category 5: Orchestrate pipeline
pipeline:
  name: "validate-migration"
  steps:
    # Category 1: Read schemas from both databases
    - name: "read-source"
      type: "read-schema"
      source: "sqlserver-legacy"
    
    - name: "read-target"
      type: "read-schema"
      source: "postgres-target"
    
    # Category 2: Compare schemas, detect breaking changes
    - name: "compare"
      type: "schema-diff"
      parameters:
        source-step: "read-source"
        target-step: "read-target"
        fail-on-incompatibility: true
        type-mappings:
          "NVARCHAR": ["VARCHAR"]
          "DATETIME2": ["TIMESTAMP"]
    
    # Category 3: Generate reports (JSON → HTML → Markdown)
    - name: "generate-reports"
      type: "report"
      parameters:
        comparison-step: "compare"
        json-output: "migration-validation.json"
        html-output: "migration-validation.html"
        markdown-output: "migration-validation.md"
    
    # Category 4: Sync data (only if validation passes)
    - name: "sync-data"
      type: "sync"
      condition: "${compare.compatible == true}"
      parameters:
        source: "sqlserver-legacy"
        target: "postgres-target"
        batch-size: 1000
        mode: "upsert"
```

**Execution Flow**:
1. **Category 6** establishes database connections (connection pooling)
2. **Category 5** starts pipeline orchestration
3. **Category 1** reads schemas from both databases via JDBC
4. **Category 2** compares schemas, detects breaking changes
5. **Category 3** generates JSON → HTML → Markdown reports
6. **Category 4** syncs data (only if step 4 passed validation)

This modular design allows:
- Running **only schema validation** (categories 1+2+3) without data sync
- Adding **new data sources** (e.g., Parquet) by implementing category 1 only
- Customizing **report formats** (category 3) without touching comparison logic
- Using **different databases** (category 6) transparently

---

## Detailed Function Testing

## 1. SCHEMA READING & DISCOVERY

### Function 1.1: Database Schema Reading (JDBC)

**Functionality**:
- Connect to relational databases via JDBC
- Query INFORMATION_SCHEMA for table metadata
- Extract column definitions (name, type, size, nullable, PK, constraints)
- Support multiple database platforms

**Current Test Coverage**:
```
ReadSchemaDatabaseTest.java
   - shouldReadSchemaFromH2Database()
   
ReadSchemaDatabasePipelineStageTest.java
   - shouldReadSchemaFromDatabase()
   - shouldReadSchemaFromMultipleTables()
   
CustomSchemaPostgresTest.java
   - shouldReadNonDefaultSchema()
```

**Coverage Gaps**:
```
SQL Server-specific schema reading
MySQL-specific schema reading  
Oracle-specific schema reading
DB2-specific schema reading
Database connection failure handling
Timeout scenarios
Invalid table names
Empty tables
Tables with 100+ columns
View vs Table distinction
Temporary tables
```

**Proposed Tests** (8 new):
```
✨ integration/schema/database/SqlServerSchemaReadingTest.java
   - shouldReadSchemaFromSqlServer()
   - shouldHandleSqlServerSpecificTypes()
   - shouldReadFromDboSchema()
   
✨ integration/schema/database/MySqlSchemaReadingTest.java
   - shouldReadSchemaFromMySql()
   - shouldHandleMySqlSpecificTypes()
   
✨ integration/schema/database/OracleSchemaReadingTest.java
   - shouldReadSchemaFromOracle()
   - shouldHandleOracleSpecificTypes()
   
✨ integration/schema/database/DatabaseErrorHandlingTest.java
   - shouldHandleConnectionTimeout()
   - shouldHandleInvalidTableName()
   - shouldHandleEmptyTable()
   - shouldHandleLargeTable()
```

**Priority**: 🔴 HIGH (critical for multi-platform support)

---

### Function 1.2: CSV Schema Reading & Type Inference

**Functionality**:
- Parse CSV files and infer schema from headers
- Automatic type detection (INTEGER, DECIMAL, VARCHAR, BOOLEAN, TIMESTAMP)
- Handle large CSV files efficiently
- Support various encodings (UTF-8, UTF-16)

**Current Test Coverage**:
```
ReadSchemaCsvTest.java
   - shouldInferSchemaFromCsv()
   - shouldInferIntegerType()
   - shouldInferDecimalType()
   - shouldInferBooleanType()
   - shouldInferTimestampType()
   
ReadSchemaCsvPipelineStageTest.java
   - shouldReadSchemaFromCsv()
   
ReadSchemaLargeCsvTest.java
   - shouldReadSchemaFromLargeCsv()
```

**Coverage Gaps**:
```
Malformed CSV (missing headers, irregular row lengths)
Unicode/encoding issues (UTF-16, special characters)
Empty CSV files
Single-column CSV
CSV with quoted fields containing delimiters
CSV with escaped characters
Very large files (1M+ rows) - performance
Type inference edge cases ("123.00" → INTEGER or DECIMAL?)
Ambiguous timestamps (multiple formats)
```

**Proposed Tests** (5 new):
```
✨ integration/schema/csv/CsvSchemaEdgeCasesTest.java
   - shouldHandleEmptyCsv()
   - shouldHandleSingleColumnCsv()
   - shouldHandleQuotedFieldsWithDelimiters()
   - shouldHandleEscapedCharacters()
   
✨ integration/schema/csv/CsvSchemaMalformedTest.java
   - shouldHandleMissingHeaders()
   - shouldHandleIrregularRowLengths()
   - shouldHandleInconsistentDelimiters()
   
✨ integration/schema/csv/CsvSchemaUnicodeTest.java
   - shouldHandleUtf16Encoding()
   - shouldHandleSpecialCharacters()
   - shouldHandleMultiByteCharacters()
   
✨ integration/schema/csv/CsvSchemaTypeInferenceTest.java
   - shouldInferDecimalFrom12300()
   - shouldHandleAmbiguousTimestamps()
   - shouldInferMixedTypes()
   
✨ performance/LargeCsvPerformanceTest.java
   - shouldRead1MillionRowCsvWithin10Seconds()
```

**Priority**: 🟡 MEDIUM (important but partial coverage exists)

---

### Function 1.3: Multi-Table Enumeration

**Functionality**:
- Enumerate all tables in a database/schema
- Filter by schema/catalog
- Return list of SchemaMetadata objects

**Current Test Coverage**:
```
ReadSchemaDatabaseEnumerationPipelineStageTest.java
   - shouldEnumerateAllTablesInDatabase()
   - shouldEnumerateTablesInSpecificSchema()
   
ReadSchemaMultiTableTest.java
   - shouldReadMultipleTablesSequentially()
```

**Coverage Gaps**:
```
Enumeration with table filtering patterns
Cross-schema enumeration
Enumeration with access restrictions
Large database (1000+ tables)
Empty schema
```

**Proposed Tests** (2 new):
```
✨ integration/schema/database/MultiTableEnumerationTest.java
   - shouldEnumerateWithTablePattern()
   - shouldHandleAccessRestrictedTables()
   - shouldEnumerateEmptySchema()
   
✨ performance/LargeDatabaseEnumerationTest.java
   - shouldEnumerate1000TablesWithin30Seconds()
```

**Priority**: 🟢 LOW (existing coverage adequate for now)

---

## 2. SCHEMA COMPARISON & DIFF

### Function 2.1: Schema Comparison (Single Table)

**Functionality**:
- Compare source and target schemas
- Detect: matching columns, added columns, removed columns, changed columns
- Classify changes by severity (INFO, WARNING, BREAKING)
- Generate ColumnDifference objects with recommendations

**Current Test Coverage**:
```
CsvToPostgresMigrationTest.java
   - shouldValidateCsvToPostgresMigration()
   - shouldDetectIncompatibilities()
   
SqlServerPostgresMigrationTest.java
   - shouldCompareSqlServerToPostgres()
   
SchemaEvolutionBreakingTest.java
   - shouldDetectBreakingChanges()
```

**Coverage Gaps**:
```
Type narrowing detection (VARCHAR(200) → VARCHAR(100))
Type widening detection (VARCHAR(100) → VARCHAR(200))
Precision/scale changes (DECIMAL(10,2) → DECIMAL(8,2))
Nullable → NOT NULL conversion
NOT NULL → Nullable conversion
Primary key additions
Primary key removals
Auto-increment changes
Default value changes
Case-insensitive column matching
Case-sensitive column matching
Column reordering (non-breaking)
```

**Proposed Tests** (9 new):
```
✨ unit/comparison/TypeNarrowingTest.java
   - shouldDetectVarcharNarrowing()
   - shouldDetectDecimalPrecisionReduction()
   - shouldClassifyAsBreakingChange()
   
✨ unit/comparison/TypeWideningTest.java
   - shouldAllowVarcharWidening()
   - shouldAllowDecimalPrecisionIncrease()
   - shouldClassifyAsNonBreaking()
   
✨ unit/comparison/NullableConversionTest.java
   - shouldDetectNullableToNotNull() // BREAKING
   - shouldAllowNotNullToNullable()  // NON-BREAKING
   
✨ unit/comparison/PrimaryKeyChangeTest.java
   - shouldDetectPrimaryKeyAddition()
   - shouldDetectPrimaryKeyRemoval()
   - shouldClassifyAsBreaking()
   
✨ unit/comparison/ColumnDifferenceTest.java
   - shouldGenerateAppropriateRecommendation()
   - shouldCalculateSeverityCorrectly()
   
✨ unit/comparison/CaseSensitivityTest.java
   - shouldMatchCaseInsensitiveByDefault()
   - shouldMatchCaseSensitiveWhenConfigured()
```

**Priority**: 🔴 HIGH (core function with major gaps)

---

### Function 2.2: Type Compatibility Rules

**Functionality**:
- Define type equivalence mappings
- Platform-specific type conversions (SQL Server NVARCHAR ↔ PostgreSQL VARCHAR)
- User-defined type mappings
- Loose vs strict type matching

**Current Test Coverage**:
```
⚠️ Implicit in integration tests, no dedicated unit tests
```

**Coverage Gaps**:
```
Default type mapping rules
Custom type mapping configuration
Cross-platform type equivalences
Inferred type tolerance (CSV → Database)
Type hierarchy matching (INTEGER → BIGINT)
Type incompatibility detection
```

**Proposed Tests** (3 new):
```
✨ unit/comparison/TypeCompatibilityTest.java
   - shouldMatchExactTypes()
   - shouldMatchMappedTypes()
   - shouldRejectIncompatibleTypes()
   - shouldApplyPlatformSpecificMappings()
   
✨ unit/comparison/CustomTypeMappingTest.java
   - shouldApplyUserDefinedMappings()
   - shouldPrioritizeCustomOverDefault()
   
✨ integration/comparison/platforms/CrossPlatformTypeMappingTest.java
   - shouldMapSqlServerToPostgresTypes()
   - shouldMapMySqlToPostgresTypes()
   - shouldMapOracleToPostgresTypes()
```

**Priority**: 🔴 HIGH (foundational logic)

---

### Function 2.3: Breaking Change Detection

**Functionality**:
- Classify schema changes as BREAKING vs NON-BREAKING
- Identify data loss risks
- Flag incompatible type conversions

**Current Test Coverage**:
```
SchemaEvolutionBreakingTest.java
   - shouldDetectBreakingChanges()
   
PreDeploymentValidationTest.java
   - shouldFailOnBreakingChanges()
```

**Coverage Gaps**:
```
Comprehensive breaking change rule coverage
Edge cases in severity classification
Multi-column breaking changes
Cascading breaking changes
```

**Proposed Tests** (2 new):
```
✨ unit/comparison/BreakingChangeDetectionTest.java
   - shouldDetectAllBreakingChangeTypes()
   - shouldCalculateBreakingChangeSeverity()
   - shouldIdentifyDataLossRisks()
   
✨ integration/comparison/scenarios/DestructiveChangesTest.java
   - shouldDetectColumnRemoval()
   - shouldDetectTypeNarrowing()
   - shouldDetectNullConstraintTightening()
```

**Priority**: 🔴 HIGH (critical for migration safety)

---

### Function 2.4: Multi-Table Comparison

**Functionality**:
- Compare entire databases (all tables)
- Table name mapping (cross-platform)
- Detect added/removed tables
- Aggregate compatibility across tables

**Current Test Coverage**:
```
MultiTableMigrationTest.java
   - shouldCompareMultipleTables()
```

**Coverage Gaps**:
```
Table name mapping validation
Partial table matching
Added table detection
Removed table detection
Mixed compatibility (some tables compatible, some not)
Schema name conflicts
```

**Proposed Tests** (2 new):
```
✨ integration/comparison/scenarios/MultiTableComparisonTest.java
   - shouldMapTableNamesAcrossPlatforms()
   - shouldDetectAddedTables()
   - shouldDetectRemovedTables()
   - shouldHandleMixedCompatibility()
   
✨ integration/comparison/scenarios/TableMappingConflictTest.java
   - shouldResolveSchemaNameConflicts()
   - shouldHandleAmbiguousTableNames()
```

**Priority**: 🟡 MEDIUM (important feature, partial coverage)

---

## 3. REPORT GENERATION

### Function 3.1: JSON Report Generation

**Functionality**:
- Serialize SchemaComparisonResult to JSON
- Follow schema-diff-v1.0.json schema
- Include metadata, source/target info, summary, columns, compatibility, recommendations
- Support deserialization (round-trip)

**Current Test Coverage**:
```
JsonSerializationTest.java (8 tests) - Week 1 deliverable
   - Basic serialization/deserialization, round-trip validation
   - Unicode handling (客户表, Straße), null values, empty schemas
   - Numeric precision (DECIMAL 10,2), multiple column types
```

**Coverage Gaps**:
```
JSON schema validation (schema compliance)
Large diff handling (100+ columns)
```

**Proposed Tests** (2 new):
```
✨ unit/serialization/JsonSchemaValidationTest.java
   - shouldValidateAgainstSchema()
   - shouldRejectInvalidJson()
   
✨ integration/reporting/JsonReportIntegrationTest.java
   - shouldGenerateJsonFromPipeline()
   - shouldWriteToCorrectPath()
```

**Priority**: MOSTLY COMPLETE (Week 1 deliverable done)

---

### Function 3.2: Markdown Report Generation

**Functionality**:
- Generate Markdown from JSON report
- StringBuilder-based efficient rendering
- Table-formatted output
- Emoji headers for visual clarity

**Current Test Coverage**:
```
MarkdownGenerationTest.java (9 tests) - Week 3 deliverable
   - Basic table generation, emoji headers (📊, 🔑, ✓)
   - Table alignment, special character escaping (|, *)
   - Unicode, summary sections, constraint indicators
```

**Coverage Gaps**:
```
Multi-table markdown reports
Large report handling
```

**Proposed Tests** (1 new):
```
✨ integration/reporting/MarkdownReportIntegrationTest.java
   - shouldGenerateMarkdownFromPipeline()
   - shouldWriteToCorrectPath()
```

**Priority**: MOSTLY COMPLETE (Week 3 deliverable done)

---

### Function 3.3: HTML Report Generation (Legacy)

**Functionality**:
- Generate HTML reports from comparison results
- Inline CSS styling
- Responsive design
- Color-coded changes

**Current Test Coverage**:
```
⚠️ Implicit in integration tests, no dedicated tests
```

**Coverage Gaps**:
```
HTML structure validation
CSS rendering
Browser compatibility
Large report handling
XSS prevention
```

**Proposed Tests** (2 new):
```
✨ unit/reporting/html/HtmlGenerationTest.java
   - shouldGenerateValidHtml()
   - shouldIncludeInlineCss()
   - shouldEscapeHtmlEntities()
   
✨ integration/reporting/HtmlReportIntegrationTest.java
   - shouldGenerateHtmlFromPipeline()
```

**Priority**: 🟢 LOW (legacy system, stable)

---

### Function 3.4: Report Path Resolution

**Functionality**:
- Handle filename-only paths (save to reports/)
- Handle relative paths (create directories)
- Handle absolute paths
- Auto-create parent directories

**Current Test Coverage**:
```
SchemaDiffReportOutputOptionsTest.java
   - shouldGenerateJsonReport()
   - shouldGenerateHtmlReport()
   - shouldGenerateMarkdownReport()
   - shouldGenerateDualOutputReports()
```

**Coverage Gaps**:
```
Filename-only path validation
Relative path normalization
Absolute path handling
Directory creation verification
Path traversal prevention
Overwrite behavior
Concurrent writes
```

**Proposed Tests** (1 new):
```
✨ unit/reporting/ReportPathResolutionTest.java
   - shouldResolveFilenameOnlyPath()
   - shouldResolveRelativePath()
   - shouldResolveAbsolutePath()
   - shouldCreateParentDirectories()
   - shouldPreventPathTraversal()
   - shouldHandleConcurrentWrites()
```

**Priority**: 🟡 MEDIUM (partial coverage, needs edge cases)

---

## 4. DATA SYNCHRONIZATION

### Function 4.1: Table Synchronization (Extract-Transform-Load)

**Functionality**:
- Extract data from source (SQL query execution)
- Transform data (SpEL expressions, validation rules)
- Load data to target (INSERT, UPSERT operations)
- Batch processing

**Current Test Coverage**:
```
TableSyncIntegrationTestH2.java
   - shouldSyncDataFromSourceToTarget()
   
MsSqlToPostgresSyncTest.java
   - shouldSyncMsSqlToPostgres()
   
SyncPipelineH2Test.java
   - shouldExecuteSyncPipeline()
```

**Coverage Gaps**:
```
Upsert conflict resolution strategies
Batch size variations
Transaction boundary testing
Failed row handling
Partial sync recovery
Large dataset sync (1M+ rows)
NULL value handling
Data type conversion during sync
```

**Proposed Tests** (4 new):
```
✨ integration/synchronization/UpsertConflictResolutionTest.java
   - shouldResolveConflictWithTargetWins()
   - shouldResolveConflictWithSourceWins()
   - shouldDetectPrimaryKeyConflicts()
   
✨ integration/synchronization/BatchProcessingTest.java
   - shouldProcessBatchSize100()
   - shouldProcessBatchSize1000()
   - shouldHandleBatchFailure()
   
✨ integration/synchronization/TransactionBoundaryTest.java
   - shouldCommitSuccessfulBatch()
   - shouldRollbackFailedBatch()
   
✨ integration/synchronization/PartialSyncRecoveryTest.java
   - shouldResumeFromLastSuccessfulBatch()
   - shouldTrackSyncProgress()
```

**Priority**: 🟡 MEDIUM (basic sync works, needs robustness)

---

## 5. PIPELINE ORCHESTRATION

### Function 5.1: Pipeline Execution

**Functionality**:
- Execute multi-step pipelines (read-schema → diff → report)
- Sequential vs parallel execution
- Step dependencies
- Context passing between steps

**Current Test Coverage**:
```
CompletePipelineWithSchemaTest.java
   - shouldExecuteCompletePipeline()
   
SchemaDiffJsonIntegrationTest.java
   - shouldExecuteSchemaDiffPipeline()
```

**Coverage Gaps**:
```
Invalid pipeline configurations
Missing step dependencies
Circular dependencies
Step timeout handling
Parallel execution
Conditional step execution
Pipeline failure recovery
```

**Proposed Tests** (3 new):
```
✨ pipeline/error_handling/InvalidConfigurationTest.java
   - shouldRejectInvalidYaml()
   - shouldRejectMissingSteps()
   - shouldRejectCircularDependencies()
   
✨ pipeline/error_handling/StepExecutionFailureTest.java
   - shouldHandleStepTimeout()
   - shouldPropagateStepFailure()
   - shouldExecuteErrorHandlers()
   
✨ pipeline/workflows/ParallelExecutionTest.java
   - shouldExecuteIndependentStepsInParallel()
   - shouldRespectDependencies()
```

**Priority**: 🟡 MEDIUM (critical path works, edge cases missing)

---

### Function 5.2: Data Source References

**Functionality**:
- Resolve data-source-ref to actual data source configs
- Load external YAML configurations
- Connection pooling
- Credential management

**Current Test Coverage**:
```
⚠️ Implicit in all integration tests
```

**Coverage Gaps**:
```
Missing data source reference
Invalid data source YAML
Connection pool exhaustion
Credential validation
Environment variable substitution
```

**Proposed Tests** (1 new):
```
✨ pipeline/error_handling/MissingDataSourceTest.java
   - shouldFailOnMissingDataSourceRef()
   - shouldValidateDataSourceYaml()
   - shouldSubstituteEnvironmentVariables()
```

**Priority**: 🟢 LOW (stable feature)

---

## 6. DATA SOURCE INTEGRATION

### Function 6.1: Database Connection Management

**Functionality**:
- JDBC connection pooling (HikariCP)
- Connection string parsing
- Credential management
- Platform-specific drivers (PostgreSQL, SQL Server, MySQL, Oracle)

**Current Test Coverage**:
```
Via Testcontainers in integration tests
```

**Coverage Gaps**:
```
Connection timeout
Invalid credentials
Network failures
Driver not found
Connection pool exhaustion
```

**Proposed Tests** (1 new):
```
✨ integration/error_handling/DatabaseConnectionTest.java
   - shouldHandleConnectionTimeout()
   - shouldHandleInvalidCredentials()
   - shouldHandleNetworkFailure()
   - shouldHandleMissingDriver()
```

**Priority**: 🟡 MEDIUM (important for robustness)

---

## Test Organization & Structure

### Current Test Structure (19 Test Classes)

```
apex-data-sync/src/test/java/dev/mars/apex/sync/
├── pipeline/ (4 tests)
│   ├── CompletePipelineWithSchemaTest.java
│   ├── MsSqlToPostgresSyncTest.java
│   ├── SchemaDiffJsonIntegrationTest.java
│   └── SyncPipelineH2Test.java (legacy)
│   └── SyncPipelineContainersTest.java
├── schema/ (8 tests)
│   ├── ReadSchemaCsvPipelineStageTest.java
│   ├── ReadSchemaCsvTest.java
│   ├── ReadSchemaDatabaseEnumerationPipelineStageTest.java
│   ├── ReadSchemaDatabasePipelineStageTest.java
│   ├── ReadSchemaDatabaseTest.java
│   ├── ReadSchemaLargeCsvTest.java
│   └── ReadSchemaMultiTableTest.java
├── schemas/ (1 test)
│   └── CustomSchemaPostgresTest.java
├── validation/ (6 tests)
│   ├── CsvToPostgresMigrationTest.java
│   ├── MultiTableMigrationTest.java
│   ├── PreDeploymentValidationTest.java
│   ├── SchemaDiffReportOutputOptionsTest.java
│   ├── SchemaEvolutionBreakingTest.java
│   └── SqlServerPostgresMigrationTest.java
├── ColoredTestOutputExtension.java
├── SyncTestBase.java
├── TableSyncIntegrationTestH2.java
├── TestConstants.java
└── TestContainerImages.java
```

### Proposed Directory Structure

```
apex-data-sync/src/test/java/dev/mars/apex/sync/
│
├── unit/                                    # Fast unit tests (no I/O)
│   ├── comparison/                          # Schema comparison logic
│   │   ├── TypeCompatibilityTest.java
│   │   ├── BreakingChangeDetectionTest.java
│   │   ├── ColumnDifferenceTest.java
│   │   ├── NullableConversionTest.java
│   │   ├── TypeNarrowingTest.java
│   │   ├── TypeWideningTest.java
│   │   ├── PrecisionScaleTest.java
│   │   ├── PrimaryKeyChangeTest.java
│   │   └── CustomTypeMappingTest.java
│   │
│   ├── serialization/                       # JSON serialization
│   │   ├── JsonSerializationTest.java
│   │   ├── JsonDeserializationTest.java
│   │   ├── JsonSchemaValidationTest.java
│   │   ├── JsonStructureTest.java
│   │   ├── MetadataGenerationTest.java
│   │   └── EdgeCaseHandlingTest.java
│   │
│   ├── reporting/                           # Report generation
│   │   ├── markdown/
│   │   │   ├── MarkdownGenerationTest.java
│   │   │   ├── TableFormattingTest.java
│   │   │   ├── EmojiHeaderTest.java
│   │   │   └── MultiTableMarkdownTest.java
│   │   ├── html/
│   │   │   ├── HtmlGenerationTest.java
│   │   │   └── TemplateRenderingTest.java
│   │   └── ReportPathResolutionTest.java
│   │
│   └── util/                                # Utility classes
│       ├── CsvTypeInferenceTest.java
│       ├── SchemaMetadataBuilderTest.java
│       └── DataSourceContextTest.java
│
├── integration/                             # Database integration tests
│   ├── schema/                              # Schema reading (real DBs)
│   │   ├── database/
│   │   │   ├── PostgreSqlSchemaReadingTest.java
│   │   │   ├── H2SchemaReadingTest.java
│   │   │   ├── SqlServerSchemaReadingTest.java
│   │   │   ├── MySqlSchemaReadingTest.java
│   │   │   ├── OracleSchemaReadingTest.java
│   │   │   ├── CustomSchemaTest.java
│   │   │   └── MultiTableEnumerationTest.java
│   │   │
│   │   └── csv/
│   │       ├── CsvSchemaBasicTest.java
│   │       ├── CsvSchemaLargeFileTest.java
│   │       ├── CsvSchemaUnicodeTest.java
│   │       ├── CsvSchemaMalformedTest.java
│   │       └── CsvSchemaEdgeCasesTest.java
│   │
│   ├── comparison/                          # Schema diff integration
│   │   ├── platforms/
│   │   │   ├── CsvToPostgresDiffTest.java
│   │   │   ├── SqlServerToPostgresDiffTest.java
│   │   │   ├── MySqlToPostgresDiffTest.java
│   │   │   ├── OracleToPostgresDiffTest.java
│   │   │   └── CrossPlatformTypeMappingTest.java
│   │   │
│   │   └── scenarios/
│   │       ├── SchemaEvolutionTest.java
│   │       ├── BreakingChangesTest.java
│   │       ├── BackwardCompatibilityTest.java
│   │       ├── AdditiveChangesTest.java
│   │       └── DestructiveChangesTest.java
│   │
│   ├── reporting/                           # End-to-end report generation
│   │   ├── JsonReportIntegrationTest.java
│   │   ├── MarkdownReportIntegrationTest.java
│   │   ├── MultiFormatReportTest.java
│   │   └── ReportOutputOptionsTest.java
│   │
│   └── synchronization/                     # Data sync tests
│       ├── H2SyncTest.java
│       ├── PostgreSqlSyncTest.java
│       ├── SqlServerToPostgresSyncTest.java
│       ├── UpsertConflictResolutionTest.java
│       ├── BatchProcessingTest.java
│       ├── TransactionBoundaryTest.java
│       └── PartialSyncRecoveryTest.java
│
├── pipeline/                                # End-to-end pipeline tests
│   ├── workflows/
│   │   ├── SimpleSchemaReadPipelineTest.java
│   │   ├── SchemaDiffPipelineTest.java
│   │   ├── MultiTableDiffPipelineTest.java
│   │   ├── DataSyncPipelineTest.java
│   │   └── CompleteE2EPipelineTest.java
│   │
│   ├── validation/
│   │   ├── PreDeploymentValidationTest.java
│   │   ├── CIIntegrationTest.java
│   │   └── MigrationCompatibilityTest.java
│   │
│   └── error_handling/
│       ├── InvalidConfigurationTest.java
│       ├── MissingDataSourceTest.java
│       ├── NetworkFailureTest.java
│       ├── TimeoutHandlingTest.java
│       └── PartialExecutionRecoveryTest.java
│
├── performance/                             # Performance & stress tests
│   ├── LargeSchemaComparisonTest.java
│   ├── MultiTablePerformanceTest.java
│   ├── ConcurrentPipelineTest.java
│   ├── MemoryLeakDetectionTest.java
│   ├── ReportGenerationBenchmarkTest.java
│   └── LargeDatasetSyncTest.java
│
├── fixtures/                                # Test data & resources
│   ├── schemas/
│   │   ├── postgresql/
│   │   ├── sqlserver/
│   │   ├── mysql/
│   │   └── oracle/
│   ├── csv/
│   │   ├── valid/
│   │   ├── malformed/
│   │   ├── unicode/
│   │   └── large/
│   ├── json/
│   │   ├── valid-reports/
│   │   └── invalid-reports/
│   └── yaml/
│       ├── pipelines/
│       └── data-sources/
│
└── support/                                 # Test infrastructure
    ├── ColoredTestOutputExtension.java
    ├── SyncTestBase.java
    ├── TestConstants.java
    ├── TestContainerImages.java
    ├── DatabaseTestContainerProvider.java
    ├── CsvTestDataGenerator.java
    ├── SchemaTestDataBuilder.java
    └── AssertionHelpers.java
```

### Test Categories & Guidelines

#### 1. **Unit Tests** (`unit/`)
**Purpose**: Fast, isolated tests with no external dependencies

**Characteristics**:
- No database connections
- No file I/O (use in-memory data)
- Mock external dependencies
- Execution time: < 50ms per test
- Coverage target: 95%+

**Example**:
```java
@Test
void shouldDetectTypeNarrowing() {
    ColumnDefinition source = new ColumnDefinition("name", "VARCHAR", 200);
    ColumnDefinition target = new ColumnDefinition("name", "VARCHAR", 100);
    
    ColumnDifference diff = comparator.compare(source, target);
    
    assertEquals(ChangeType.SIZE_CHANGED, diff.getChangeType());
    assertEquals(Severity.BREAKING, diff.getSeverity());
    assertTrue(diff.getMessage().contains("narrowing"));
}
```

#### 2. **Integration Tests** (`integration/`)
**Purpose**: Test interactions with real databases and file systems

**Characteristics**:
- Use Testcontainers for real databases
- Actual file system operations
- Real JDBC connections
- Execution time: < 5 seconds per test
- Coverage target: 90%+

**Example**:
```java
@Testcontainers
class PostgreSqlSchemaReadingTest extends SyncTestBase {
    
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15");
    
    @Test
    void shouldReadSchemaFromPostgres() {
        // Create actual table in Testcontainer
        // Read schema via SchemaReaderService
        // Verify metadata accuracy
    }
}
```

#### 3. **Pipeline Tests** (`pipeline/`)
**Purpose**: End-to-end workflow validation

**Characteristics**:
- Complete YAML pipeline execution
- Multiple stages (read-schema → diff → report)
- Real RulesEngine execution
- Execution time: < 10 seconds per test
- Coverage target: 85%+

**Example**:
```java
@Test
void shouldExecuteCompleteMigrationValidationPipeline() {
    RulesEngine engine = RulesEngine.fromFile("test-migration-pipeline.yaml");
    RuleResult result = engine.evaluate(Map.of());
    
    assertTrue(result.isSuccess());
    assertReportGenerated("reports/migration-diff.json");
    assertReportGenerated("reports/migration-diff.html");
}
```

#### 4. **Performance Tests** (`performance/`)
**Purpose**: Validate performance characteristics and resource usage

**Characteristics**:
- Large datasets (100+ columns, 50+ tables)
- Concurrent execution scenarios
- Memory profiling
- Execution time tracking
- Coverage target: Key scenarios only

**Example**:
```java
@Test
@Timeout(value = 30, unit = TimeUnit.SECONDS)
void shouldCompare100TablesWithin30Seconds() {
    List<SchemaMetadata> source = generate100Tables();
    List<SchemaMetadata> target = generate100Tables();
    
    long start = System.currentTimeMillis();
    SchemaDiff diff = service.compareSchemas(source, target, options);
    long duration = System.currentTimeMillis() - start;
    
    assertTrue(duration < 30000, "Should complete in < 30 seconds");
    assertEquals(100, diff.getTableDiffs().size());
}
```

### Test Naming Conventions

| Pattern | Usage | Examples |
|---------|-------|----------|
| `should[Action][UnderCondition]` | Unit tests | `shouldDetectTypeNarrowing()`, `shouldAllowTypeWidening()` |
| `should[Action]From[Source]` | Integration tests | `shouldReadSchemaFromPostgres()`, `shouldReadSchemaFromCsvWithUnicode()` |
| `should[Action][Workflow]` | Pipeline tests | `shouldExecuteCompleteMigrationPipeline()`, `shouldFailOnIncompatibleSchemas()` |
| `should[Action][Resource]Within[Benchmark]` | Performance tests | `shouldCompare100TablesWithin30Seconds()` |

---

## YAML Co-Location Pattern

### Overview

The YAML Co-Location Pattern is a **strict organizational principle** that ensures every YAML configuration file has a corresponding Java test class in the **same directory**. This pattern provides executable documentation, enforces testability, and prevents orphaned configuration files.

### The Pattern

#### Core Principle
```
CORRECT: Co-located in same directory
src/test/java/dev/mars/apex/sync/schema/
├── SchemaAnalysisExample.java
└── SchemaAnalysisExample.yaml

WRONG: Orphaned YAML in separate configs folder
configs/
└── schema-analysis-example.yaml
```

#### Naming Convention
- **Java test class**: `{BaseName}.java` (PascalCase)
- **YAML configuration**: `{BaseName}.yaml` (PascalCase - **must exactly match** Java class name)
- Both files **must** share the **identical base name** including case
- Both files **must** reside in the same `src/test/java/` subdirectory

**Examples:**
- `SchemaAnalysisExample.java` + `SchemaAnalysisExample.yaml`
- `ReadSchemaDatabaseTest.java` + `ReadSchemaDatabaseTest.yaml`
- `SchemaDiffTest.java` + `schema-diff-test.yaml` (case mismatch)
- `TestSchema.java` + `test-schema.yaml` (kebab-case not allowed)

### Directory Structure Example
```
apex-data-sync/
└── src/test/java/dev/mars/apex/sync/
    ├── schema/
    │   ├── ReadSchemaDatabaseTest.java
    │   ├── ReadSchemaDatabaseTest.yaml
    │   ├── ReadSchemaCsvTest.java
    │   ├── ReadSchemaCsvTest.yaml
    │   ├── SchemaAnalysisExample.java
    │   └── SchemaAnalysisExample.yaml
    ├── validation/
    │   ├── PreDeploymentValidationTest.java
    │   └── PreDeploymentValidationTest.yaml
    └── transform/
        ├── CustomTypeMappingTest.java
        └── CustomTypeMappingTest.yaml
```

### Java Test Class Template
```java
package dev.mars.apex.sync.schema;

import dev.mars.apex.core.config.yaml.YamlConfigurationLoader;
import dev.mars.apex.core.engine.config.RulesEngine;
import dev.mars.apex.sync.SyncTestBase;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SchemaAnalysisExample extends SyncTestBase {
    
    @Test
    void shouldDemonstrateSchemaAnalysisCapabilities() throws Exception {
        // Load co-located YAML configuration
        var config = yamlLoader.loadFromFile(
            "src/test/java/dev/mars/apex/sync/schema/SchemaAnalysisExample.yaml"
        );
        
        assertNotNull(config, "Configuration should load successfully");
        
        var engine = RulesEngine.fromYamlConfig(config);
        assertNotNull(engine, "Engine should be created from configuration");
        
        // Note: This example demonstrates configuration only
        // Execution patterns vary based on use case
    }
}
```

### Pattern Benefits

| Benefit | Description |
|---------|-------------|
| **Executable Documentation** | YAML configurations are validated through actual test execution |
| **Discoverability** | Developers find related test code and configuration together |
| **Prevents Configuration Drift** | Orphaned YAML files are immediately visible as pattern violations |
| **Test Isolation** | Each test has its own configuration file |

### Anti-Patterns to Avoid

| Anti-Pattern | Problem |
|--------------|---------|
| Module-root config folders (`configs/`) | Separates configuration from the code that uses it |
| Shared configuration files | Creates hidden dependencies between tests |
| YAML without Java test | Untested configuration, unclear purpose |

### Enforcement

This pattern is **mandatory** for all test configurations in apex-data-sync:

- All YAML files in `src/test/java/` must have matching Java test classes
- No standalone configuration folders at module root
- YAML and Java files must share the same base name
- Both files must reside in the same directory

**Code Review Checklist**:
- [ ] Every new YAML file has a matching Java test class
- [ ] Both files are in the same `src/test/java/` subdirectory
- [ ] Test successfully loads and validates the configuration
- [ ] No orphaned YAML files remain in the changeset

### Adoption Metrics

- **Total co-located pairs**: 100+ in apex-data-sync module
- **Adoption rate**: 100% of YAML configurations since v2.1
- **Pattern violations**: 0 (enforced through code review)

---

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

# Filter for specific logs (PowerShell)
Get-Content target/surefire-reports/*.txt | Select-String -Pattern "\[SchemaReader"
```

### Report Output Configuration

Schema analysis reports can be generated automatically during test execution:

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

---

## Summary by Priority

### 🔴 CRITICAL (Immediate Action Required)

**Missing Tests for Delivered Features**:
1. JSON Schema Validation (2 tests)
2. Markdown Pipeline Integration (1 test)

**Total**: 3 tests remaining

---

### 🔴 HIGH (Within 2 Weeks)

**Core Function Gaps**:
1. Type Compatibility (3 tests)
2. Breaking Change Detection (2 tests)
3. Type Narrowing/Widening (2 tests)
4. Nullable Conversion (1 test)
5. Primary Key Changes (1 test)
6. Cross-Platform DB Support (3 tests)

**Total**: 12 tests

---

### 🟡 MEDIUM (Within 4 Weeks)

**Robustness & Edge Cases**:
1. CSV Edge Cases (5 tests)
2. Multi-Table Comparison (2 tests)
3. Report Path Resolution (1 test)
4. Data Sync Robustness (4 tests)
5. Pipeline Error Handling (3 tests)
6. Database Connection Errors (1 test)

**Total**: 16 tests

---

### 🟢 LOW (As Time Permits)

**Nice-to-Have Coverage**:
1. HTML Report Tests (2 tests)
2. Multi-Format Reports (1 test)
3. Multi-Table Enumeration (2 tests)
4. Pipeline Context (1 test)
5. Query Execution (1 test)

**Total**: 7 tests

---

## Priority Implementation Plan

### Phase 1: Critical Coverage (Week 1-2) MOSTLY COMPLETE
**Goal**: Close most critical gaps for production readiness

1. **JSON Serialization** (8 tests) COMPLETE
2. **Markdown Generation** (9 tests) COMPLETE
3. **Type Compatibility** (8 tests) - IN PROGRESS
4. **Report Path Handling** (5 tests) - IN PROGRESS

**Total Phase 1**: 22 tests (17 complete, 5 remaining)

### Phase 2: Enhanced Coverage (Week 3-4)
**Goal**: Comprehensive platform and edge case coverage

1. **Cross-Platform Tests** (4 tests)
   - `SqlServerSchemaReadingTest`
   - `MySqlSchemaReadingTest`
   - `OracleSchemaReadingTest`
   - `CrossPlatformTypeMappingTest`

2. **CSV Edge Cases** (4 tests)
   - `CsvSchemaUnicodeTest`
   - `CsvSchemaMalformedTest`
   - `CsvSchemaEdgeCasesTest`
   - `CsvSchemaLargeFileTest`

3. **Scenario-Based Tests** (3 tests)
   - `BackwardCompatibilityTest`
   - `AdditiveChangesTest`
   - `DestructiveChangesTest`

**Total Phase 2**: 11 new test classes

### Phase 3: Robustness & Performance (Week 5-6)
**Goal**: Production-grade reliability

1. **Error Handling** (5 tests)
   - `InvalidConfigurationTest`
   - `MissingDataSourceTest`
   - `NetworkFailureTest`
   - `TimeoutHandlingTest`
   - `PartialExecutionRecoveryTest`

2. **Performance Tests** (6 tests)
   - `LargeSchemaComparisonTest`
   - `MultiTablePerformanceTest`
   - `ConcurrentPipelineTest`
   - `MemoryLeakDetectionTest`
   - `ReportGenerationBenchmarkTest`
   - `LargeDatasetSyncTest`

3. **Synchronization Tests** (4 tests)
   - `UpsertConflictResolutionTest`
   - `BatchProcessingTest`
   - `TransactionBoundaryTest`
   - `PartialSyncRecoveryTest`

**Total Phase 3**: 15 new test classes

---

## Test Count Summary

| Category | Current | Phase 1 | Phase 2 | Phase 3 | **Total** |
|----------|---------|---------|---------|---------|-----------|
| **Unit Tests** | 17 | 5 | 0 | 0 | **22** |
| **Integration Tests** | 11 | 0 | 11 | 0 | **22** |
| **Pipeline Tests** | 5 | 0 | 0 | 5 | **10** |
| **Performance Tests** | 0 | 0 | 0 | 6 | **6** |
| **Sync Tests** | 0 | 0 | 0 | 4 | **4** |
| **Validation Tests** | 6 | 0 | 0 | 0 | **6** |
| **Support Classes** | 4 | 0 | 0 | 4 | **8** |
| **TOTAL** | **39** | **5** | **11** | **19** | **71** |

**Growth**: From 39 tests → 71 tests (82% increase from current state)

**Estimated Effort**:
- Phase 1 Remaining: 8 hours (0.5 weeks)
- Phase 2: 24 hours (1.5 weeks)
- Phase 3: 40 hours (2 weeks)
- **Total Remaining**: ~72 hours (4 weeks for 1 developer)

---

## Migration Strategy

### Step 1: Create New Directory Structure
```bash
# Create new directories
mkdir -p src/test/java/dev/mars/apex/sync/unit/{comparison,serialization,reporting/markdown,util}
mkdir -p src/test/java/dev/mars/apex/sync/integration/{schema/database,schema/csv,comparison/platforms,comparison/scenarios,reporting,synchronization}
mkdir -p src/test/java/dev/mars/apex/sync/pipeline/{workflows,validation,error_handling}
mkdir -p src/test/java/dev/mars/apex/sync/performance
mkdir -p src/test/java/dev/mars/apex/sync/fixtures/{schemas,csv,json,yaml}
mkdir -p src/test/java/dev/mars/apex/sync/support
```

### Step 2: Move Existing Tests (Preserve Git History)
```bash
# Schema tests
git mv src/test/java/dev/mars/apex/sync/schema/* src/test/java/dev/mars/apex/sync/integration/schema/database/

# Validation tests
git mv src/test/java/dev/mars/apex/sync/validation/* src/test/java/dev/mars/apex/sync/integration/comparison/scenarios/

# Pipeline tests
git mv src/test/java/dev/mars/apex/sync/pipeline/* src/test/java/dev/mars/apex/sync/pipeline/workflows/

# Support classes
git mv src/test/java/dev/mars/apex/sync/*.java src/test/java/dev/mars/apex/sync/support/
```

### Step 3: Update Package Declarations
```java
// Before
package dev.mars.apex.sync.schema;

// After
package dev.mars.apex.sync.integration.schema.database;
```

---

## Success Criteria

### Phase 1 Completion (Week 2)
- All JSON serialization tests passing
- Markdown generation tests passing
- ⏳ Type compatibility tests comprehensive (95%+ coverage)
- ⏳ Report path handling robust
- Zero regressions in existing tests

### Phase 2 Completion (Week 4)
- SQL Server, MySQL integration tests passing
- CSV edge cases handled
- Scenario-based tests comprehensive
- All existing tests migrated to new structure
- 90%+ integration test coverage

### Phase 3 Completion (Week 6)
- Error handling tests comprehensive
- Performance benchmarks established
- Synchronization tests robust
- 95%+ overall test coverage
- CI/CD pipeline executing all tests successfully

---

**Document Version**: 2.1  
**Last Updated**: January 21, 2026  
**Author**: Mark Andrew Ray-Smith Cityline Ltd  
**Status**: Consolidated - Ready for Implementation
