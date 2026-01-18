# APEX Data-Sync - Functional Test Mapping

## Module Functions & Test Coverage

This document maps **apex-data-sync module functions** to their test coverage, identifying gaps and priorities.

---

## Table of Contents

1. [Function Category Explanations](#function-category-explanations)
2. [Detailed Function Testing](#detailed-function-testing)
   - [1. Schema Reading & Discovery](#1-schema-reading--discovery)
   - [2. Schema Comparison & Diff](#2-schema-comparison--diff)
   - [3. Report Generation](#3-report-generation)
   - [4. Data Synchronization](#4-data-synchronization)
   - [5. Pipeline Orchestration](#5-pipeline-orchestration)
   - [6. Data Source Integration](#6-data-source-integration)
3. [Summary by Priority](#summary-by-priority)
4. [Execution Plan](#execution-plan)

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
✅ ReadSchemaDatabaseTest.java
   - shouldReadSchemaFromH2Database()
   
✅ ReadSchemaDatabasePipelineStageTest.java
   - shouldReadSchemaFromDatabase()
   - shouldReadSchemaFromMultipleTables()
   
✅ CustomSchemaPostgresTest.java
   - shouldReadNonDefaultSchema()
```

**Coverage Gaps**:
```
❌ SQL Server-specific schema reading
❌ MySQL-specific schema reading  
❌ Oracle-specific schema reading
❌ DB2-specific schema reading
❌ Database connection failure handling
❌ Timeout scenarios
❌ Invalid table names
❌ Empty tables
❌ Tables with 100+ columns
❌ View vs Table distinction
❌ Temporary tables
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
✅ ReadSchemaCsvTest.java
   - shouldInferSchemaFromCsv()
   - shouldInferIntegerType()
   - shouldInferDecimalType()
   - shouldInferBooleanType()
   - shouldInferTimestampType()
   
✅ ReadSchemaCsvPipelineStageTest.java
   - shouldReadSchemaFromCsv()
   
✅ ReadSchemaLargeCsvTest.java
   - shouldReadSchemaFromLargeCsv()
```

**Coverage Gaps**:
```
❌ Malformed CSV (missing headers, irregular row lengths)
❌ Unicode/encoding issues (UTF-16, special characters)
❌ Empty CSV files
❌ Single-column CSV
❌ CSV with quoted fields containing delimiters
❌ CSV with escaped characters
❌ Very large files (1M+ rows) - performance
❌ Type inference edge cases ("123.00" → INTEGER or DECIMAL?)
❌ Ambiguous timestamps (multiple formats)
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
✅ ReadSchemaDatabaseEnumerationPipelineStageTest.java
   - shouldEnumerateAllTablesInDatabase()
   - shouldEnumerateTablesInSpecificSchema()
   
✅ ReadSchemaMultiTableTest.java
   - shouldReadMultipleTablesSequentially()
```

**Coverage Gaps**:
```
❌ Enumeration with table filtering patterns
❌ Cross-schema enumeration
❌ Enumeration with access restrictions
❌ Large database (1000+ tables)
❌ Empty schema
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
✅ CsvToPostgresMigrationTest.java
   - shouldValidateCsvToPostgresMigration()
   - shouldDetectIncompatibilities()
   
✅ SqlServerPostgresMigrationTest.java
   - shouldCompareSqlServerToPostgres()
   
✅ SchemaEvolutionBreakingTest.java
   - shouldDetectBreakingChanges()
```

**Coverage Gaps**:
```
❌ Type narrowing detection (VARCHAR(200) → VARCHAR(100))
❌ Type widening detection (VARCHAR(100) → VARCHAR(200))
❌ Precision/scale changes (DECIMAL(10,2) → DECIMAL(8,2))
❌ Nullable → NOT NULL conversion
❌ NOT NULL → Nullable conversion
❌ Primary key additions
❌ Primary key removals
❌ Auto-increment changes
❌ Default value changes
❌ Case-insensitive column matching
❌ Case-sensitive column matching
❌ Column reordering (non-breaking)
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
❌ Default type mapping rules
❌ Custom type mapping configuration
❌ Cross-platform type equivalences
❌ Inferred type tolerance (CSV → Database)
❌ Type hierarchy matching (INTEGER → BIGINT)
❌ Type incompatibility detection
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
✅ SchemaEvolutionBreakingTest.java
   - shouldDetectBreakingChanges()
   
✅ PreDeploymentValidationTest.java
   - shouldFailOnBreakingChanges()
```

**Coverage Gaps**:
```
❌ Comprehensive breaking change rule coverage
❌ Edge cases in severity classification
❌ Multi-column breaking changes
❌ Cascading breaking changes
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
✅ MultiTableMigrationTest.java
   - shouldCompareMultipleTables()
```

**Coverage Gaps**:
```
❌ Table name mapping validation
❌ Partial table matching
❌ Added table detection
❌ Removed table detection
❌ Mixed compatibility (some tables compatible, some not)
❌ Schema name conflicts
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
❌ NO TESTS (major gap for Week 1-3 deliverable)
```

**Coverage Gaps**:
```
❌ Basic serialization
❌ Deserialization
❌ Schema validation
❌ JSON structure validation
❌ Null handling
❌ Empty diff handling
❌ Large diff handling (100+ columns)
❌ Unicode in JSON
```

**Proposed Tests** (5 new):
```
✨ unit/serialization/JsonSerializationTest.java
   - shouldSerializeComparisonResult()
   - shouldIncludeAllRequiredFields()
   - shouldHandleNullValues()
   - shouldHandleUnicodeCharacters()
   
✨ unit/serialization/JsonDeserializationTest.java
   - shouldDeserializeToReport()
   - shouldPreserveAllData()
   - shouldHandleRoundTrip()
   
✨ unit/serialization/JsonSchemaValidationTest.java
   - shouldValidateAgainstSchema()
   - shouldRejectInvalidJson()
   
✨ unit/serialization/JsonStructureTest.java
   - shouldHaveCorrectMetadata()
   - shouldHaveCorrectSourceTarget()
   - shouldHaveCorrectSummary()
   
✨ integration/reporting/JsonReportIntegrationTest.java
   - shouldGenerateJsonFromPipeline()
   - shouldWriteToCorrectPath()
```

**Priority**: 🔴 CRITICAL (missing tests for delivered feature)

---

### Function 3.2: Markdown Report Generation

**Functionality**:
- Generate Markdown from JSON report
- StringBuilder-based efficient rendering
- Table-formatted output
- Emoji headers for visual clarity

**Current Test Coverage**:
```
❌ NO TESTS (major gap for Week 3 deliverable)
```

**Coverage Gaps**:
```
❌ Basic markdown generation
❌ Table formatting
❌ Emoji rendering
❌ Multi-table markdown
❌ Large report handling
❌ Special character escaping
```

**Proposed Tests** (4 new):
```
✨ unit/reporting/markdown/MarkdownGenerationTest.java
   - shouldGenerateValidMarkdown()
   - shouldIncludeAllSections()
   - shouldHandleSpecialCharacters()
   
✨ unit/reporting/markdown/TableFormattingTest.java
   - shouldFormatTablesCorrectly()
   - shouldAlignColumns()
   - shouldHandleLongValues()
   
✨ unit/reporting/markdown/EmojiHeaderTest.java
   - shouldRenderEmojiHeaders()
   - shouldFallbackWithoutEmojiSupport()
   
✨ integration/reporting/MarkdownReportIntegrationTest.java
   - shouldGenerateMarkdownFromPipeline()
   - shouldWriteToCorrectPath()
```

**Priority**: 🔴 CRITICAL (missing tests for delivered feature)

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
❌ HTML structure validation
❌ CSS rendering
❌ Browser compatibility
❌ Large report handling
❌ XSS prevention
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
✅ SchemaDiffReportOutputOptionsTest.java
   - shouldGenerateJsonReport()
   - shouldGenerateHtmlReport()
   - shouldGenerateMarkdownReport()
   - shouldGenerateDualOutputReports()
```

**Coverage Gaps**:
```
❌ Filename-only path validation
❌ Relative path normalization
❌ Absolute path handling
❌ Directory creation verification
❌ Path traversal prevention
❌ Overwrite behavior
❌ Concurrent writes
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

### Function 3.5: Multi-Format Report Generation

**Functionality**:
- Generate JSON + HTML + Markdown in single pipeline
- Consistent data across formats

**Current Test Coverage**:
```
✅ SchemaDiffReportOutputOptionsTest.java
   - shouldGenerateDualOutputReports()
```

**Coverage Gaps**:
```
❌ Triple format generation
❌ Format consistency validation
❌ Selective format generation
```

**Proposed Tests** (1 new):
```
✨ integration/reporting/MultiFormatReportTest.java
   - shouldGenerateAllFormats()
   - shouldMaintainConsistency()
   - shouldAllowSelectiveGeneration()
```

**Priority**: 🟢 LOW (working, minimal gaps)

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
✅ TableSyncIntegrationTestH2.java
   - shouldSyncDataFromSourceToTarget()
   
✅ MsSqlToPostgresSyncTest.java
   - shouldSyncMsSqlToPostgres()
   
✅ SyncPipelineH2Test.java
   - shouldExecuteSyncPipeline()
```

**Coverage Gaps**:
```
❌ Upsert conflict resolution strategies
❌ Batch size variations
❌ Transaction boundary testing
❌ Failed row handling
❌ Partial sync recovery
❌ Large dataset sync (1M+ rows)
❌ NULL value handling
❌ Data type conversion during sync
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
✅ CompletePipelineWithSchemaTest.java
   - shouldExecuteCompletePipeline()
   
✅ SchemaDiffJsonIntegrationTest.java
   - shouldExecuteSchemaDiffPipeline()
```

**Coverage Gaps**:
```
❌ Invalid pipeline configurations
❌ Missing step dependencies
❌ Circular dependencies
❌ Step timeout handling
❌ Parallel execution
❌ Conditional step execution
❌ Pipeline failure recovery
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
❌ Missing data source reference
❌ Invalid data source YAML
❌ Connection pool exhaustion
❌ Credential validation
❌ Environment variable substitution
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

### Function 5.3: Pipeline Context Management

**Functionality**:
- Store step results in pipeline context
- Pass data between steps (schema metadata, comparison results)
- Context keys: `schema_<stepName>`, `schema_diff_<stepName>`

**Current Test Coverage**:
```
⚠️ Implicit in pipeline tests
```

**Coverage Gaps**:
```
❌ Context key collisions
❌ Context data persistence
❌ Context serialization
```

**Proposed Tests** (1 new):
```
✨ unit/util/PipelineContextTest.java
   - shouldStoreAndRetrieveData()
   - shouldHandleKeyCollisions()
   - shouldClearContext()
```

**Priority**: 🟢 LOW (working well)

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
✅ Via Testcontainers in integration tests
```

**Coverage Gaps**:
```
❌ Connection timeout
❌ Invalid credentials
❌ Network failures
❌ Driver not found
❌ Connection pool exhaustion
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

### Function 6.2: Query Execution

**Functionality**:
- Execute named queries from data source config
- Parameter binding
- Result set mapping
- Batch execution

**Current Test Coverage**:
```
✅ Via sync tests (implicit)
```

**Coverage Gaps**:
```
❌ SQL injection prevention
❌ Query timeout
❌ Large result sets
❌ NULL handling
```

**Proposed Tests** (1 new):
```
✨ integration/synchronization/QueryExecutionTest.java
   - shouldPreventSqlInjection()
   - shouldHandleQueryTimeout()
   - shouldHandleLargeResultSet()
```

**Priority**: 🟢 LOW (working, secure via APEX Core)

---

## SUMMARY BY PRIORITY

### 🔴 CRITICAL (Immediate Action Required)

**Missing Tests for Delivered Features**:
1. JSON Serialization (5 tests) - **Week 1 deliverable**
2. Markdown Generation (4 tests) - **Week 3 deliverable**

**Total**: 9 tests

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

## TOTAL NEW TESTS NEEDED

| Priority | Count | Timeline |
|----------|-------|----------|
| 🔴 CRITICAL | 9 | This week |
| 🔴 HIGH | 12 | Weeks 1-2 |
| 🟡 MEDIUM | 16 | Weeks 3-4 |
| 🟢 LOW | 7 | Weeks 5-6 |
| **TOTAL** | **44** | **6 weeks** |

**Current Tests**: 19  
**Proposed New Tests**: 44  
**Total After Implementation**: 63 tests (232% increase)

---

## EXECUTION PLAN

### Week 1: Critical Coverage
```
Day 1-2: JSON Serialization (5 tests)
Day 3-4: Markdown Generation (4 tests)
```

### Week 2: Core Comparison Logic
```
Day 1-2: Type Compatibility (3 tests)
Day 3: Breaking Change Detection (2 tests)
Day 4-5: Type Narrowing/Widening/Nullable (4 tests)
```

### Week 3: Cross-Platform & Edge Cases
```
Day 1-2: SQL Server/MySQL Integration (3 tests)
Day 3-4: CSV Edge Cases (5 tests)
Day 5: Multi-Table Comparison (2 tests)
```

### Week 4: Robustness
```
Day 1-2: Data Sync Error Handling (4 tests)
Day 3-4: Pipeline Error Handling (3 tests)
Day 5: Report Path + DB Connection (2 tests)
```

### Weeks 5-6: Low Priority
```
As time permits: 7 low-priority tests
```

---

**Document Version**: 1.0  
**Created**: January 18, 2026  
**Status**: Ready for Implementation
