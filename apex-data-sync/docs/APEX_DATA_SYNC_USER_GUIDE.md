# APEX Schema Diff - User Guide

## Overview

The **schema-diff** pipeline stage enables automatic comparison of schemas from heterogeneous data sources to validate migration compatibility, detect breaking changes, and generate comprehensive HTML reports. This feature is essential for:

- **Pre-migration validation**: Verify schema compatibility before data movement
- **Breaking change detection**: Identify incompatible changes that could cause runtime failures
- **Migration planning**: Generate actionable reports with migration recommendations
- **Schema evolution tracking**: Monitor safe vs. unsafe schema changes over time

### Supported Data Sources

- **Databases**: PostgreSQL, SQL Server, MySQL, Oracle, H2, DB2
- **Files**: CSV with automatic type inference
- **Future**: Parquet, JSON, Excel, Avro (Phase 2+)

---

## Quick Start

### Basic Schema Comparison

Compare two database schemas:

```yaml
metadata:
  id: "database-migration-validation"
  type: "pipeline-config"
  version: "1.0"

data-source-refs:
  - name: "source-db"
    source: "data-sources/source-database.yaml"
    enabled: true
  - name: "target-db"
    source: "data-sources/target-database.yaml"
    enabled: true

pipeline:
  name: "migration-validation"
  execution:
    mode: "sequential"
  
  steps:
    - name: "read-source-schema"
      type: "read-schema"
      data-source-ref: "source-db"
      parameters:
        table: "customers"
    
    - name: "read-target-schema"
      type: "read-schema"
      data-source-ref: "target-db"
      parameters:
        table: "customers"
    
    - name: "compare-schemas"
      type: "schema-diff"
      parameters:
        source-step: "read-source-schema"
        target-step: "read-target-schema"
        report-output: "migration-report.html"
```

**Run the pipeline**:
```bash
java -jar apex-data-sync.jar --config=configs/migration-validation.yaml
```

**Output**:
- Console logs with comparison summary
- HTML report at `reports/migration-report.html`

---

## Common Use Cases

### Use Case 1: CSV to PostgreSQL Migration

Validate that a legacy CSV file can be migrated to a PostgreSQL table:

```yaml
metadata:
  id: "csv-to-postgres-validation"
  type: "pipeline-config"

data-source-refs:
  - name: "legacy-csv"
    source: "data-sources/legacy-customers.yaml"
  - name: "postgres-db"
    source: "data-sources/postgres.yaml"

pipeline:
  name: "csv-postgres-migration"
  execution:
    mode: "sequential"
  
  steps:
    - name: "read-csv-schema"
      type: "read-schema"
      data-source-ref: "legacy-csv"
      parameters:
        file: "legacy_customers.csv"
    
    - name: "read-postgres-schema"
      type: "read-schema"
      data-source-ref: "postgres-db"
      parameters:
        table: "public.customers"
    
    - name: "validate-compatibility"
      type: "schema-diff"
      parameters:
        source-step: "read-csv-schema"
        target-step: "read-postgres-schema"
        fail-on-incompatibility: true
        report-output: "csv-migration-validation.html"
```

**What this validates**:
- Column name matching (case-insensitive)
- Type compatibility (CSV inferred types → PostgreSQL types)
- Missing columns in target (potential data loss)
- Added columns in target (must be nullable for safety)

---

### Use Case 2: SQL Server to PostgreSQL Migration

Cross-platform database migration with type mapping:

```yaml
metadata:
  id: "sqlserver-postgres-migration"
  type: "pipeline-config"

data-source-refs:
  - name: "sqlserver-source"
    source: "data-sources/sqlserver.yaml"
  - name: "postgres-target"
    source: "data-sources/postgres.yaml"

pipeline:
  name: "cross-platform-migration"
  execution:
    mode: "sequential"
  
  steps:
    - name: "read-sqlserver-schema"
      type: "read-schema"
      data-source-ref: "sqlserver-source"
      parameters:
        table: "dbo.Orders"
    
    - name: "read-postgres-schema"
      type: "read-schema"
      data-source-ref: "postgres-target"
      parameters:
        table: "public.orders"
    
    - name: "compare-schemas"
      type: "schema-diff"
      parameters:
        source-step: "read-sqlserver-schema"
        target-step: "read-postgres-schema"
        type-mappings:
          "NVARCHAR": ["VARCHAR", "TEXT"]
          "DATETIME": ["TIMESTAMP", "TIMESTAMP WITHOUT TIME ZONE"]
          "BIT": ["BOOLEAN"]
          "INT": ["INTEGER"]
        report-output: "sqlserver-postgres-diff.html"
```

**Type mappings**:
- Define acceptable type conversions between platforms
- Multiple target types allowed per source type
- Case-insensitive matching

---

### Use Case 3: Multi-Table Migration

Validate entire database schema in one pipeline:

```yaml
metadata:
  id: "full-database-migration"
  type: "pipeline-config"

data-source-refs:
  - name: "legacy-db"
    source: "data-sources/legacy-database.yaml"
  - name: "modern-db"
    source: "data-sources/modern-database.yaml"

pipeline:
  name: "full-database-validation"
  execution:
    mode: "sequential"
  
  steps:
    # Customers table
    - name: "read-legacy-customers"
      type: "read-schema"
      data-source-ref: "legacy-db"
      parameters:
        table: "dbo.Customers"
    
    - name: "read-modern-customers"
      type: "read-schema"
      data-source-ref: "modern-db"
      parameters:
        table: "public.customers"
    
    - name: "compare-customers"
      type: "schema-diff"
      parameters:
        source-step: "read-legacy-customers"
        target-step: "read-modern-customers"
        report-output: "customers-diff.html"
    
    # Orders table
    - name: "read-legacy-orders"
      type: "read-schema"
      data-source-ref: "legacy-db"
      parameters:
        table: "dbo.Orders"
    
    - name: "read-modern-orders"
      type: "read-schema"
      data-source-ref: "modern-db"
      parameters:
        table: "public.orders"
    
    - name: "compare-orders"
      type: "schema-diff"
      parameters:
        source-step: "read-legacy-orders"
        target-step: "read-modern-orders"
        report-output: "orders-diff.html"
    
    # Products table
    - name: "read-legacy-products"
      type: "read-schema"
      data-source-ref: "legacy-db"
      parameters:
        table: "dbo.Products"
    
    - name: "read-modern-products"
      type: "read-schema"
      data-source-ref: "modern-db"
      parameters:
        table: "public.products"
    
    - name: "compare-products"
      type: "schema-diff"
      parameters:
        source-step: "read-legacy-products"
        target-step: "read-modern-products"
        report-output: "products-diff.html"
```

**Benefits**:
- Single pipeline run validates all tables
- Individual reports per table for detailed analysis
- Parallel validation possible with `execution.mode: "parallel"`

---

### Use Case 4: Pre-Deployment Validation

Run schema diff as part of CI/CD before deploying schema changes:

```yaml
metadata:
  id: "pre-deployment-validation"
  type: "pipeline-config"

data-source-refs:
  - name: "staging-db"
    source: "data-sources/staging.yaml"
  - name: "production-db"
    source: "data-sources/production.yaml"

pipeline:
  name: "pre-deploy-check"
  execution:
    mode: "sequential"
  
  steps:
    - name: "read-staging-schema"
      type: "read-schema"
      data-source-ref: "staging-db"
      parameters:
        table: "public.customers"
    
    - name: "read-production-schema"
      type: "read-schema"
      data-source-ref: "production-db"
      parameters:
        table: "public.customers"
    
    - name: "validate-changes"
      type: "schema-diff"
      parameters:
        source-step: "read-staging-schema"
        target-step: "read-production-schema"
        fail-on-incompatibility: true
        fail-on-breaking-changes: true
        report-output: "pre-deploy-validation.html"
```

**CI/CD Integration**:
```bash
#!/bin/bash
# In your CI pipeline
java -jar apex-data-sync.jar --config=configs/pre-deploy-validation.yaml
if [ $? -ne 0 ]; then
  echo "❌ Schema validation failed - blocking deployment"
  exit 1
fi
echo "✅ Schema validation passed - proceeding with deployment"
```

---

## Configuration Parameters

### Schema-Diff Step Parameters

| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `source-step` | String | Yes | - | Name of the read-schema step containing source schema |
| `target-step` | String | Yes | - | Name of the read-schema step containing target schema |
| `report-output` | String | No | - | Path to HTML report file (relative or absolute) |
| `fail-on-incompatibility` | Boolean | No | `false` | Fail pipeline if schemas are incompatible |
| `fail-on-breaking-changes` | Boolean | No | `false` | Fail pipeline if breaking changes detected |
| `type-mappings` | Map | No | `{}` | Custom type equivalence mappings |
| `ignore-columns` | List | No | `[]` | Column names to exclude from comparison |
| `case-sensitive` | Boolean | No | `false` | Enable case-sensitive column name matching |

### Report Output Paths

The `report-output` parameter supports flexible path formats:

```yaml
# Filename only → saved to reports/ directory (created automatically)
report-output: "migration-report.html"
# → Saved to: reports/migration-report.html

# Relative path → creates directories as needed
report-output: "output/migration/2026/january/report.html"
# → Saved to: output/migration/2026/january/report.html

# Absolute path (Windows)
report-output: "C:/migration-reports/report.html"

# Absolute path (Unix/Linux)
report-output: "/var/reports/migration/report.html"
```

### Type Mappings

Define custom type equivalences for cross-platform migrations:

```yaml
parameters:
  type-mappings:
    # SQL Server → PostgreSQL
    "NVARCHAR": ["VARCHAR", "TEXT"]
    "DATETIME": ["TIMESTAMP", "TIMESTAMP WITHOUT TIME ZONE"]
    "DATETIME2": ["TIMESTAMP"]
    "BIT": ["BOOLEAN"]
    "TINYINT": ["SMALLINT"]
    "MONEY": ["NUMERIC", "DECIMAL"]
    
    # Oracle → PostgreSQL
    "VARCHAR2": ["VARCHAR", "TEXT"]
    "NUMBER": ["NUMERIC", "DECIMAL", "INTEGER"]
    "DATE": ["TIMESTAMP", "DATE"]
    
    # MySQL → PostgreSQL
    "LONGTEXT": ["TEXT"]
    "MEDIUMTEXT": ["TEXT"]
    "TINYINT(1)": ["BOOLEAN"]
```

### Ignore Columns

Exclude specific columns from comparison (useful for auto-generated columns):

```yaml
parameters:
  ignore-columns:
    - "created_at"      # System timestamp
    - "updated_at"      # System timestamp
    - "row_version"     # Auto-increment version
    - "sync_id"         # ETL metadata
```

---

## Understanding the HTML Report

### Report Sections

The generated HTML report contains:

#### 1. **Comparison Header**
- Source and target data source names
- Visual arrow showing migration direction
- Timestamp of analysis

#### 2. **Database Connection Details**
- Database type (PostgreSQL, SQL Server, etc.)
- Host, port, database name
- Username (password redacted)

#### 3. **Comparison Summary**

Visual statistics cards:
- **Matching Columns** (blue): Columns that exist in both schemas with compatible types
- **Added Columns** (green): New columns in target (backward compatible if nullable)
- **Removed Columns** (red): Columns missing in target (⚠️ potential data loss)
- **Changed Columns** (orange): Columns with type/constraint changes
- **Breaking Changes** (dark red): Incompatible changes that will cause failures

#### 4. **Compatibility Assessment**

```
✓ Compatible Migration: Target schema is backward compatible with source schema.
```

or

```
⚠️ Incompatible Migration: Breaking changes detected that may cause data loss or runtime errors.
```

#### 5. **Source Schema Details**

Table showing all source columns:
- Column name
- Data type
- Nullable (Yes/No)
- Max length (for VARCHAR)
- Precision/Scale (for NUMERIC)
- Status badge (MATCHING, REMOVED, CHANGED)

#### 6. **Target Schema Details**

Similar table for target schema with status badges:
- MATCHING, ADDED, CHANGED

#### 7. **Breaking Changes** (if any)

Detailed list of incompatible changes:
- Type narrowing (e.g., VARCHAR(200) → VARCHAR(100))
- Nullable → NOT NULL conversions
- Column removals
- Incompatible type changes

---

## Interpreting Comparison Results

### Compatible Changes (✅ Safe)

These changes are **backward compatible** and safe for migration:

| Change Type | Example | Why Safe |
|-------------|---------|----------|
| **Added nullable column** | Target adds `phone VARCHAR(20)` | Existing data doesn't need this column |
| **Type widening** | VARCHAR(100) → VARCHAR(200) | Larger size accommodates all existing values |
| **Type promotion** | INTEGER → BIGINT | Larger type can hold all smaller values |
| **Precision increase** | DECIMAL(10,2) → DECIMAL(12,2) | More digits, same scale |
| **NOT NULL → Nullable** | Column becomes nullable | Less restrictive |

### Breaking Changes (⚠️ Requires Action)

These changes are **incompatible** and require manual intervention:

| Change Type | Example | Risk | Solution |
|-------------|---------|------|----------|
| **Removed column** | Source has `middle_name`, target doesn't | Data loss | Add column to target or map to different column |
| **Type narrowing** | VARCHAR(200) → VARCHAR(100) | Truncation | Validate data fits or increase target size |
| **Nullable → NOT NULL** | Nullable → NOT NULL | NULL rejection | Set default value or ensure no NULLs |
| **Type demotion** | BIGINT → INTEGER | Overflow | Validate range or keep BIGINT |
| **Incompatible types** | VARCHAR → INTEGER | Cast failure | Add transformation logic or fix target type |

### Changed Columns (⚠️ Review Required)

These changes may or may not be compatible:

```
Column: created_date
Source: DATE
Target: TIMESTAMP
Status: CHANGED (may be compatible with type mapping)
```

**Action**: Review type mappings to confirm compatibility.

---

## Best Practices

### 1. **Always Validate Before Migration**

Run schema-diff **before** attempting data migration:

```bash
# Step 1: Validate schema compatibility
java -jar apex-data-sync.jar --config=configs/schema-validation.yaml

# Step 2: Review HTML report
open reports/migration-validation.html

# Step 3: Fix any breaking changes

# Step 4: Re-validate
java -jar apex-data-sync.jar --config=configs/schema-validation.yaml

# Step 5: Proceed with data migration
java -jar apex-data-sync.jar --config=configs/data-migration.yaml
```

### 2. **Use Type Mappings for Cross-Platform Migrations**

Always define type mappings when migrating between different database platforms:

```yaml
type-mappings:
  # Document WHY each mapping is defined
  "NVARCHAR": ["VARCHAR"]  # SQL Server Unicode → PostgreSQL UTF-8
  "DATETIME": ["TIMESTAMP"]  # SQL Server datetime → PostgreSQL timestamp
```

### 3. **Fail Fast in CI/CD**

Enable strict validation in automated pipelines:

```yaml
parameters:
  fail-on-incompatibility: true
  fail-on-breaking-changes: true
```

### 4. **Archive Reports**

Save reports with timestamps for audit trails:

```yaml
report-output: "reports/migrations/2026-01-17-migration-validation.html"
```

### 5. **Test with Real Data Sources**

Use **Testcontainers** or similar tools to validate against real database engines:

```java
@Container
private static final PostgreSQLContainer<?> postgres = 
    new PostgreSQLContainer<>("postgres:15-alpine");
```

### 6. **Ignore Metadata Columns**

Exclude system-generated columns from comparison:

```yaml
ignore-columns:
  - "created_at"
  - "updated_at"
  - "row_version"
  - "_sync_metadata"
```

---

## Troubleshooting

### Problem: "Table not found or no columns"

**Symptom**:
```
[ERROR] Failed to read database schema for table 'CUSTOMERS': Table not found or no columns: CUSTOMERS
```

**Solutions**:

1. **Check case sensitivity**: Use exact case from database
   ```yaml
   # PostgreSQL (lowercase)
   table: "customers"
   
   # SQL Server (mixed case)
   table: "Customers"
   
   # H2 (uppercase)
   table: "CUSTOMERS"
   ```

2. **Specify schema**: Include schema prefix
   ```yaml
   # PostgreSQL
   table: "public.customers"
   
   # SQL Server
   table: "dbo.Customers"
   
   # Oracle
   table: "SCHEMA_NAME.CUSTOMERS"
   ```

3. **Verify connection**: Test database connectivity
   ```bash
   # PostgreSQL
   psql -h localhost -p 5432 -U test -d testdb -c "\dt"
   
   # SQL Server
   sqlcmd -S localhost -U sa -Q "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES"
   ```

---

### Problem: "No schema metadata found for step"

**Symptom**:
```
[ERROR] No schema metadata found for step: read-source-schema
```

**Solutions**:

1. **Verify step names match**:
   ```yaml
   steps:
     - name: "read-source-schema"  # ← Must match exactly
       type: "read-schema"
   
   parameters:
     source-step: "read-source-schema"  # ← Must match exactly
   ```

2. **Check step execution order**: Schema-diff must come AFTER read-schema steps

3. **Verify read-schema success**: Check logs for successful schema reading

---

### Problem: "Report not generated"

**Symptom**: HTML report file not created

**Solutions**:

1. **Check file permissions**: Ensure write access to output directory

2. **Create parent directories**: APEX creates them automatically, but verify

3. **Use absolute paths**: For troubleshooting
   ```yaml
   report-output: "C:/temp/test-report.html"
   ```

4. **Check disk space**: Ensure sufficient space available

---

### Problem: "False positive breaking changes"

**Symptom**: Compatible changes flagged as breaking

**Solutions**:

1. **Add type mappings**:
   ```yaml
   type-mappings:
     "VARCHAR": ["CHARACTER VARYING"]
     "INT": ["INTEGER"]
   ```

2. **Case-insensitive comparison**: Default behavior
   ```yaml
   case-sensitive: false  # Default
   ```

3. **Review type compatibility logic**: Some platform-specific types may need custom mappings

---

## Performance Considerations

### Schema Reading Performance

| Data Source | Typical Time | Notes |
|-------------|--------------|-------|
| **PostgreSQL** | 20-50ms per table | Fast metadata queries |
| **SQL Server** | 30-60ms per table | INFORMATION_SCHEMA queries |
| **CSV** | 5-15ms per file | In-memory parsing |
| **H2** | 10-30ms per table | In-memory database |

### Large Schema Comparisons

For databases with 100+ tables:

1. **Parallel execution**:
   ```yaml
   pipeline:
     execution:
       mode: "parallel"
       max-threads: 4
   ```

2. **Batch tables**: Group related tables in separate pipelines

3. **Filter schemas**: Only compare relevant schemas
   ```yaml
   # In data source config
   schema: "public"  # Limit to specific schema
   ```

---

## Integration Examples

### Spring Boot Integration

```java
@Service
public class MigrationValidationService {
    
    @Autowired
    private RulesEngineService rulesEngine;
    
    public MigrationValidationResult validateMigration(String configPath) {
        RulesEngine engine = RulesEngine.fromFile(configPath);
        RuleResult result = engine.evaluate(Map.of());
        
        // Extract schema comparison result
        SchemaComparisonResult comparison = result.getExecutionPath().stream()
            .filter(s -> "PIPELINE_STEP".equals(s.getType()))
            .filter(s -> s.getName().contains("schema-diff"))
            .findFirst()
            .map(s -> (SchemaComparisonResult) s.getStepData())
            .orElse(null);
        
        return new MigrationValidationResult(
            result.isSuccess(),
            comparison.isCompatible(),
            comparison.getBreakingChanges()
        );
    }
}
```

### CI/CD Pipeline (GitHub Actions)

```yaml
name: Schema Migration Validation

on:
  pull_request:
    paths:
      - 'database/migrations/**'

jobs:
  validate-schema:
    runs-on: ubuntu-latest
    
    steps:
      - uses: actions/checkout@v3
      
      - name: Set up JDK 17
        uses: actions/setup-java@v3
        with:
          java-version: '17'
      
      - name: Start PostgreSQL
        run: |
          docker run -d \
            -e POSTGRES_PASSWORD=test \
            -p 5432:5432 \
            postgres:15-alpine
      
      - name: Run Schema Validation
        run: |
          java -jar apex-data-sync.jar \
            --config=configs/schema-validation.yaml
      
      - name: Upload Validation Report
        if: always()
        uses: actions/upload-artifact@v3
        with:
          name: schema-validation-report
          path: reports/*.html
      
      - name: Comment on PR
        if: failure()
        uses: actions/github-script@v6
        with:
          script: |
            github.rest.issues.createComment({
              issue_number: context.issue.number,
              owner: context.repo.owner,
              repo: context.repo.repo,
              body: '⚠️ Schema validation failed! Review the validation report.'
            })
```

---

## Advanced Topics

### Custom Type Compatibility Rules

For specialized type conversions, extend the type mapping system:

```yaml
type-mappings:
  # Spatial types
  "GEOGRAPHY": ["GEOMETRY", "POINT"]
  "GEOMETRY": ["GEOGRAPHY"]
  
  # JSON types
  "JSONB": ["JSON", "TEXT"]
  "JSON": ["JSONB", "TEXT"]
  
  # Array types
  "INTEGER[]": ["INT ARRAY"]
  "TEXT[]": ["VARCHAR ARRAY"]
  
  # Custom types
  "UUID": ["CHAR(36)", "VARCHAR(36)"]
```

### Schema Evolution Patterns

**Pattern 1: Additive Changes Only**
```yaml
# Only allow new columns (no removals)
fail-on-breaking-changes: true
```

**Pattern 2: Backward Compatible Evolution**
```yaml
# Allow removals if columns are nullable
# (Applications can handle missing columns)
```

**Pattern 3: Version-Based Migration**
```yaml
# Compare v1 → v2 → v3 in sequence
pipeline:
  steps:
    - name: "compare-v1-to-v2"
      type: "schema-diff"
      parameters:
        source-step: "read-v1"
        target-step: "read-v2"
    
    - name: "compare-v2-to-v3"
      type: "schema-diff"
      parameters:
        source-step: "read-v2"
        target-step: "read-v3"
```

---

## FAQ

### Q: Can I compare schemas across different database types?

**A**: Yes! Schema-diff works with any combination:
- CSV ↔ PostgreSQL
- SQL Server ↔ PostgreSQL
- MySQL ↔ Oracle
- H2 ↔ PostgreSQL

Use `type-mappings` to handle platform-specific type differences.

---

### Q: What happens if a breaking change is detected?

**A**: Depends on your configuration:
- `fail-on-breaking-changes: false` (default): Pipeline continues, report shows warnings
- `fail-on-breaking-changes: true`: Pipeline fails immediately

---

### Q: Can I customize the HTML report format?

**A**: Currently, the HTML report uses a fixed template optimized for readability. Custom templates are planned for Phase 2.

---

### Q: Does schema-diff compare data?

**A**: No, Phase 1 only compares **schema structure** (columns, types, constraints). **Data-diff** for row-by-row comparison is planned for Phase 2, leveraging APEX validation and enrichment capabilities.

---

### Q: How do I handle renamed columns?

**A**: Schema-diff detects renamed columns as:
- 1 removed column (old name)
- 1 added column (new name)

For migrations with column renames, use data transformation logic in your migration pipeline.

---

### Q: Can I run schema-diff in production?

**A**: Yes, but with read-only access:
- Schema-diff only performs **SELECT** queries on INFORMATION_SCHEMA
- No data modifications
- Minimal performance impact (metadata queries only)

---

## Next Steps

1. **Review** the [APEX_DATA_SYNC.md](APEX_DATA_SYNC.md) for overall architecture
2. **Explore** the [SCHEMA_DIFF_DESIGN.md](SCHEMA_DIFF_DESIGN.md) for technical details
3. **Check** the [README-TESTING.md](README-TESTING.md) for testing guidelines
4. **Try** the example configurations in `configs/`

---

## Support & Feedback

For issues, questions, or feature requests related to schema-diff:
- Review test cases in `src/test/java/dev/mars/apex/sync/validation/`
- Check design documentation in `docs/SCHEMA_DIFF_DESIGN.md`
- Examine working examples in test YAML configurations

**Phase 1 Status**: ✅ Production Ready  
**Phase 2 (Data-Diff)**: Planned - will leverage APEX validation/enrichment for data content comparison
