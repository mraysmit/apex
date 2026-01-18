# APEX Data-Sync - Test Organization & Coverage Proposal

> **Related Document**: See [FUNCTIONAL_TEST_MAPPING.md](FUNCTIONAL_TEST_MAPPING.md) for detailed function-by-function test analysis.

## Current State Analysis

### Existing Test Structure (19 Test Classes)

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

### Current Coverage Gaps

Based on architectural documentation review, we're missing tests for:

> **Function Category Breakdown**: The [FUNCTIONAL_TEST_MAPPING.md](FUNCTIONAL_TEST_MAPPING.md) document organizes these gaps by the 6 functional categories:
> 1. Schema Reading & Discovery (input layer)
> 2. Schema Comparison & Diff (core brain)
> 3. Report Generation (output layer)
> 4. Data Synchronization (ETL layer)
> 5. Pipeline Orchestration (control layer)
> 6. Data Source Integration (infrastructure layer)

#### 1. **JSON Serialization & Deserialization** (Week 1-3 deliverables - CRITICAL)
- ❌ JSON report generation from comparison results
- ❌ JSON schema validation against schema-diff-v1.0.json
- ❌ Deserialization of JSON reports
- ❌ JSON structure correctness (metadata, source/target info, summary)
- ❌ Edge cases: empty diffs, all breaking changes, null handling

> **Priority Justification**: Week 1 deliverable with **0% test coverage**. Maps to Function Category 3 (Report Generation) in FUNCTIONAL_TEST_MAPPING.md.

#### 2. **Markdown Report Generation** (Week 3 deliverable - CRITICAL)
- ❌ Markdown generation from JSON
- ❌ StringBuilder-based rendering tests
- ❌ Table formatting validation
- ❌ Emoji header rendering
- ❌ Multi-table markdown reports

> **Priority Justification**: Week 3 deliverable with **0% test coverage**. Maps to Function Category 3 (Report Generation) in FUNCTIONAL_TEST_MAPPING.md.

#### 3. **Schema-Diff Core Logic** (Maps to Function Category 2: Schema Comparison & Diff)
- ❌ Type compatibility rules (complex type mappings)
- ❌ Breaking change detection edge cases
- ❌ Nullable → NOT NULL conversions
- ❌ Type narrowing vs widening
- ❌ Precision/scale changes
- ❌ Primary key changes
- ❌ Auto-increment changes
- ❌ Custom type mappings
- ❌ Case-sensitivity handling

#### 4. **Multi-Table Diff**
- ❌ Table name mapping (cross-platform)
- ❌ Added/removed tables detection
- ❌ Partial table matching
- ❌ Schema enumeration without mapping
- ❌ Conflict resolution when tables renamed

#### 5. **Report Output**
- ⚠️ Partial coverage: SchemaDiffReportOutputOptionsTest (4 tests)
- ❌ Filename-only path handling
- ❌ Relative path creation
- ❌ Absolute path handling
- ❌ Directory auto-creation
- ❌ Report overwrite behavior
- ❌ Concurrent report generation
- ❌ Large report handling (100+ columns)

#### 6. **Cross-Database Platform Tests**
- ✅ PostgreSQL (covered)
- ✅ H2 (covered)
- ❌ SQL Server (only integration test)
- ❌ MySQL
- ❌ Oracle
- ❌ DB2
- ❌ Platform-specific type mappings

#### 7. **CSV Schema Reading** (Maps to Function Category 1: Schema Reading & Discovery)
- ⚠️ Partial coverage: ReadSchemaCsvTest, ReadSchemaCsvPipelineStageTest
- ❌ Malformed CSV handling
- ❌ Missing headers
- ❌ Empty files
- ❌ Large files (1M+ rows)
- ❌ Unicode/encoding issues
- ❌ Quoted fields with special characters
- ❌ Type inference edge cases (e.g., "123.00" as DECIMAL vs INTEGER)

#### 8. **Performance & Stress Tests**
- ❌ Large schema comparisons (100+ columns)
- ❌ Multi-table comparisons (50+ tables)
- ❌ Concurrent pipeline execution
- ❌ Memory leak detection
- ❌ Report generation performance benchmarks

#### 9. **Error Handling & Edge Cases** (Maps to Function Category 5: Pipeline Orchestration)
- ❌ Invalid pipeline configurations
- ❌ Missing data source references
- ❌ Network failures during DB connections
- ❌ Timeout handling
- ❌ Transaction rollback scenarios
- ❌ Partial execution recovery

#### 10. **Data Synchronization** (TableSyncRunner - Maps to Function Category 4: Data Synchronization)
- ⚠️ Minimal coverage: TableSyncIntegrationTestH2, MsSqlToPostgresSyncTest
- ❌ Upsert conflict resolution
- ❌ Batch size variations
- ❌ Transaction boundaries
- ❌ Failed row handling
- ❌ Partial sync recovery

---

## Proposed Test Organization

### New Directory Structure

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
│   │   │   ├── HtmlGenerationTest.java     # (future)
│   │   │   └── TemplateRenderingTest.java  # (future)
│   │   └── ReportPathResolutionTest.java
│   │       ├── testFilenameOnlyPath()
│   │       ├── testRelativePath()
│   │       ├── testAbsolutePath()
│   │       ├── testDirectoryCreation()
│   │       └── testPathNormalization()
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
│   │   │   ├── SqlServerSchemaReadingTest.java  # NEW
│   │   │   ├── MySqlSchemaReadingTest.java      # NEW
│   │   │   ├── OracleSchemaReadingTest.java     # NEW
│   │   │   ├── CustomSchemaTest.java
│   │   │   └── MultiTableEnumerationTest.java
│   │   │
│   │   └── csv/
│   │       ├── CsvSchemaBasicTest.java
│   │       ├── CsvSchemaLargeFileTest.java
│   │       ├── CsvSchemaUnicodeTest.java        # NEW
│   │       ├── CsvSchemaMalformedTest.java      # NEW
│   │       └── CsvSchemaEdgeCasesTest.java      # NEW
│   │
│   ├── comparison/                          # Schema diff integration
│   │   ├── platforms/
│   │   │   ├── CsvToPostgresDiffTest.java
│   │   │   ├── SqlServerToPostgresDiffTest.java
│   │   │   ├── MySqlToPostgresDiffTest.java     # NEW
│   │   │   ├── OracleToPostgresDiffTest.java    # NEW
│   │   │   └── CrossPlatformTypeMappingTest.java # NEW
│   │   │
│   │   └── scenarios/
│   │       ├── SchemaEvolutionTest.java
│   │       ├── BreakingChangesTest.java
│   │       ├── BackwardCompatibilityTest.java   # NEW
│   │       ├── AdditiveChangesTest.java         # NEW
│   │       └── DestructiveChangesTest.java      # NEW
│   │
│   ├── reporting/                           # End-to-end report generation
│   │   ├── JsonReportIntegrationTest.java
│   │   ├── MarkdownReportIntegrationTest.java
│   │   ├── MultiFormatReportTest.java           # NEW
│   │   └── ReportOutputOptionsTest.java
│   │
│   └── synchronization/                     # Data sync tests
│       ├── H2SyncTest.java
│       ├── PostgreSqlSyncTest.java
│       ├── SqlServerToPostgresSyncTest.java
│       ├── UpsertConflictResolutionTest.java    # NEW
│       ├── BatchProcessingTest.java             # NEW
│       ├── TransactionBoundaryTest.java         # NEW
│       └── PartialSyncRecoveryTest.java         # NEW
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
│   │   ├── CIIntegrationTest.java               # NEW
│   │   └── MigrationCompatibilityTest.java      # NEW
│   │
│   └── error_handling/                          # NEW category
│       ├── InvalidConfigurationTest.java
│       ├── MissingDataSourceTest.java
│       ├── NetworkFailureTest.java
│       ├── TimeoutHandlingTest.java
│       └── PartialExecutionRecoveryTest.java
│
├── performance/                             # Performance & stress tests
│   ├── LargeSchemaComparisonTest.java           # NEW
│   ├── MultiTablePerformanceTest.java           # NEW
│   ├── ConcurrentPipelineTest.java              # NEW
│   ├── MemoryLeakDetectionTest.java             # NEW
│   ├── ReportGenerationBenchmarkTest.java       # NEW
│   └── LargeDatasetSyncTest.java                # NEW
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
    ├── DatabaseTestContainerProvider.java       # NEW
    ├── CsvTestDataGenerator.java                # NEW
    ├── SchemaTestDataBuilder.java               # NEW
    └── AssertionHelpers.java                    # NEW
```

---

## Test Categories & Guidelines

### 1. **Unit Tests** (`unit/`)
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

### 2. **Integration Tests** (`integration/`)
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

### 3. **Pipeline Tests** (`pipeline/`)
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

### 4. **Performance Tests** (`performance/`)
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

---

## Priority Test Implementation Plan

> **Alignment with FUNCTIONAL_TEST_MAPPING.md**:
> - **Phase 1** = 🔴 CRITICAL priority (9 tests) + 🔴 HIGH priority (12 tests) = 21 tests
> - **Phase 2** = 🟡 MEDIUM priority (11 tests)
> - **Phase 3** = Remaining gaps + 🟢 LOW priority (19 tests)

### Phase 1: Critical Coverage (Week 1-2)
**Goal**: Close most critical gaps for production readiness (🔴 CRITICAL + 🔴 HIGH from FUNCTIONAL_TEST_MAPPING)

1. **JSON Serialization** (5 tests)
   - `JsonSerializationTest` - basic serialization
   - `JsonDeserializationTest` - round-trip testing
   - `JsonSchemaValidationTest` - schema compliance
   - `JsonStructureTest` - verify JSON structure
   - `EdgeCaseHandlingTest` - nulls, empty diffs

2. **Markdown Generation** (4 tests)
   - `MarkdownGenerationTest` - basic markdown output
   - `TableFormattingTest` - table structure validation
   - `EmojiHeaderTest` - emoji rendering
   - `MultiTableMarkdownTest` - multi-table reports

3. **Type Compatibility** (8 tests)
   - `TypeCompatibilityTest` - basic type matching
   - `BreakingChangeDetectionTest` - breaking change rules
   - `TypeNarrowingTest` - VARCHAR(200) → VARCHAR(100)
   - `TypeWideningTest` - VARCHAR(100) → VARCHAR(200)
   - `NullableConversionTest` - NULL ↔ NOT NULL
   - `PrecisionScaleTest` - DECIMAL precision changes
   - `PrimaryKeyChangeTest` - PK modifications
   - `CustomTypeMappingTest` - user-defined type maps

4. **Report Path Handling** (5 tests)
   - `ReportPathResolutionTest` - all path types
   - Directory creation, normalization, overwrites

**Total Phase 1**: 22 new test classes

### Phase 2: Enhanced Coverage (Week 3-4)
**Goal**: Comprehensive platform and edge case coverage

1. **Cross-Platform Tests** (4 tests)
   - `SqlServerSchemaReadingTest` - SQL Server integration
   - `MySqlSchemaReadingTest` - MySQL integration
   - `OracleSchemaReadingTest` - Oracle integration (if available)
   - `CrossPlatformTypeMappingTest` - platform-specific mappings

2. **CSV Edge Cases** (4 tests)
   - `CsvSchemaUnicodeTest` - UTF-8, UTF-16 handling
   - `CsvSchemaMalformedTest` - missing headers, irregular rows
   - `CsvSchemaEdgeCasesTest` - empty files, single column
   - `CsvSchemaLargeFileTest` - 1M+ rows (performance)

3. **Scenario-Based Tests** (3 tests)
   - `BackwardCompatibilityTest` - additive-only changes
   - `AdditiveChangesTest` - new nullable columns
   - `DestructiveChangesTest` - column removals, type narrowing

**Total Phase 2**: 11 new test classes

### Phase 3: Robustness & Performance (Week 5-6)
**Goal**: Production-grade reliability

1. **Error Handling** (5 tests)
   - `InvalidConfigurationTest` - malformed YAML
   - `MissingDataSourceTest` - missing refs
   - `NetworkFailureTest` - DB connection failures
   - `TimeoutHandlingTest` - long-running queries
   - `PartialExecutionRecoveryTest` - resume from failure

2. **Performance Tests** (6 tests)
   - `LargeSchemaComparisonTest` - 100+ columns
   - `MultiTablePerformanceTest` - 50+ tables
   - `ConcurrentPipelineTest` - parallel execution
   - `MemoryLeakDetectionTest` - heap profiling
   - `ReportGenerationBenchmarkTest` - report speed
   - `LargeDatasetSyncTest` - 1M+ rows sync

3. **Synchronization Tests** (4 tests)
   - `UpsertConflictResolutionTest` - PK conflicts
   - `BatchProcessingTest` - batch size variations
   - `TransactionBoundaryTest` - rollback scenarios
   - `PartialSyncRecoveryTest` - resume from failure

**Total Phase 3**: 15 new test classes

---

## Testing Infrastructure Enhancements

### New Test Support Classes

#### 1. **DatabaseTestContainerProvider.java**
```java
public class DatabaseTestContainerProvider {
    public static PostgreSQLContainer<?> createPostgres();
    public static MySQLContainer<?> createMySql();
    public static MSSQLServerContainer<?> createSqlServer();
    public static OracleContainer createOracle();  // If license allows
}
```

#### 2. **CsvTestDataGenerator.java**
```java
public class CsvTestDataGenerator {
    public static Path generateValidCsv(int rows, int columns);
    public static Path generateMalformedCsv(MalformedType type);
    public static Path generateUnicodeCsv(Charset charset);
    public static Path generateLargeCsv(int rows);  // 1M+ rows
}
```

#### 3. **SchemaTestDataBuilder.java**
```java
public class SchemaTestDataBuilder {
    public SchemaMetadata buildPostgresSchema(String tableName);
    public SchemaMetadata buildSqlServerSchema(String tableName);
    public SchemaMetadata buildCsvSchema(String fileName);
    public List<SchemaMetadata> buildMultiTableSchema(int tableCount);
}
```

#### 4. **AssertionHelpers.java**
```java
public class AssertionHelpers {
    public static void assertSchemaEquals(SchemaMetadata expected, SchemaMetadata actual);
    public static void assertDiffContainsBreakingChange(SchemaDiff diff, String columnName);
    public static void assertReportGenerated(Path reportPath);
    public static void assertJsonValid(String json, String schemaPath);
}
```

---

## Test Naming Conventions

### Pattern 1: **Unit Tests**
```
should[Action][UnderCondition]
```
Examples:
- `shouldDetectTypeNarrowing()`
- `shouldAllowTypeWidening()`
- `shouldFailOnNullableToNotNullConversion()`

### Pattern 2: **Integration Tests**
```
should[Action]From[Source][OptionalCondition]
```
Examples:
- `shouldReadSchemaFromPostgres()`
- `shouldReadSchemaFromCsvWithUnicode()`
- `shouldCompareSchemasCrossDatabase()`

### Pattern 3: **Pipeline Tests**
```
should[Action][WorkflowDescription]
```
Examples:
- `shouldExecuteCompleteMigrationPipeline()`
- `shouldFailOnIncompatibleSchemas()`
- `shouldGenerateAllReportFormats()`

### Pattern 4: **Performance Tests**
```
should[Action][Resource][WithinBenchmark]
```
Examples:
- `shouldCompare100TablesWithin30Seconds()`
- `shouldGenerateReportWithin500Milliseconds()`
- `shouldHandle1MillionRowsWithoutMemoryLeak()`

---

## Coverage Metrics & Goals

### Current Coverage (Estimated)
- **Unit Tests**: ~30% (mostly missing comparison logic)
- **Integration Tests**: ~60% (schema reading covered, sync partial)
- **Pipeline Tests**: ~50% (basic flows covered, error handling missing)
- **Performance Tests**: ~5% (minimal benchmarking)

### Target Coverage (After Implementation)
- **Unit Tests**: 95%+ (all comparison logic, serialization, utilities)
- **Integration Tests**: 90%+ (all DB platforms, CSV edge cases)
- **Pipeline Tests**: 85%+ (all workflows, error scenarios)
- **Performance Tests**: Key scenarios benchmarked (100+ cols, 50+ tables, 1M+ rows)

### Critical Paths (Must be 100% Covered)
1. Type compatibility checking (`areTypesCompatible()`)
2. Breaking change detection (`isBreakingTypeChange()`)
3. JSON serialization/deserialization
4. Report path resolution
5. Pipeline step validation
6. Schema metadata parsing

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

### Step 4: Implement Phase 1 Tests (Parallel to Refactor)
- No need to wait for migration to complete
- New tests go directly into new structure

### Step 5: Update Build & CI Configuration
```xml
<!-- pom.xml: Ensure test source includes all subdirectories -->
<testSourceDirectory>src/test/java</testSourceDirectory>

<!-- Maven Surefire: Run all tests -->
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-surefire-plugin</artifactId>
    <configuration>
        <includes>
            <include>**/*Test.java</include>
        </includes>
    </configuration>
</plugin>
```

---

## Success Criteria

### Phase 1 Completion (Week 2)
- ✅ All JSON serialization tests passing
- ✅ Markdown generation tests passing
- ✅ Type compatibility tests comprehensive (95%+ coverage)
- ✅ Report path handling robust
- ✅ Zero regressions in existing tests

### Phase 2 Completion (Week 4)
- ✅ SQL Server, MySQL integration tests passing
- ✅ CSV edge cases handled
- ✅ Scenario-based tests comprehensive
- ✅ All existing tests migrated to new structure
- ✅ 90%+ integration test coverage

### Phase 3 Completion (Week 6)
- ✅ Error handling tests comprehensive
- ✅ Performance benchmarks established
- ✅ Synchronization tests robust
- ✅ 95%+ overall test coverage
- ✅ CI/CD pipeline executing all tests successfully

---

## Recommendations

### Immediate Actions (This Week)
1. ✅ **Review & approve** this test organization proposal
2. ✅ **Create new directory structure** (30 minutes)
3. ✅ **Move 3-5 existing tests** as proof-of-concept (2 hours)
4. ✅ **Implement 2-3 critical unit tests** (JsonSerializationTest, TypeCompatibilityTest) (4 hours)
5. ✅ **Update README-TESTING.md** to reference new structure (1 hour)

### Short-Term Actions (Next 2 Weeks)
1. **Complete Phase 1** test implementation (22 new tests)
2. **Migrate remaining existing tests** to new structure
3. **Update CI/CD** to run categorized tests
4. **Establish baseline coverage metrics**

### Long-Term Actions (Next 6 Weeks)
1. **Complete Phase 2 & 3** test implementation
2. **Establish performance benchmarks**
3. **Document test infrastructure** in APEX_DATA_SYNC_ARCHITECTURE.md
4. **Create test templates** for contributors

---

## Appendix: Test Count Summary

> **Note**: This proposal identifies **52 new tests** across 3 phases. The [FUNCTIONAL_TEST_MAPPING.md](FUNCTIONAL_TEST_MAPPING.md) document identifies **44 new tests** organized by function category. The difference (8 tests) represents infrastructure/support tests included here but not in the function-specific mapping.

| Category | Current | Phase 1 | Phase 2 | Phase 3 | **Total** |
|----------|---------|---------|---------|---------|-----------|
| **Unit Tests** | 0 | 22 | 0 | 0 | **22** |
| **Integration Tests** | 15 | 0 | 11 | 0 | **26** |
| **Pipeline Tests** | 4 | 0 | 0 | 5 | **9** |
| **Performance Tests** | 0 | 0 | 0 | 6 | **6** |
| **Sync Tests** | 0 | 0 | 0 | 4 | **4** |
| **Support Classes** | 4 | 0 | 0 | 4 | **8** |
| **TOTAL** | **19** | **22** | **11** | **19** | **71** |

**Growth**: From 19 tests → 71 tests (274% increase)

**Estimated Effort**:
- Phase 1: 40 hours (2 weeks)
- Phase 2: 24 hours (1.5 weeks)
- Phase 3: 40 hours (2 weeks)
- **Total**: ~104 hours (5.5 weeks for 1 developer)

---

**Document Version**: 1.0  
**Created**: January 18, 2026  
**Author**: APEX Development Team  
**Status**: Proposal - Awaiting Approval
