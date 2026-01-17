# Schema Diff JSON Architecture - Design Document

## Executive Summary

This document proposes a **layered architecture** for schema diff reporting where:
1. **Data Layer**: `SchemaComparisonResult` (Java domain model)
2. **Serialization Layer**: JSON intermediate representation
3. **Presentation Layer**: HTML/PDF/Markdown generated from JSON

This approach provides **separation of concerns**, enabling multiple report formats from a single canonical JSON representation.

---

## Current Architecture (To Be Preserved)

```
SchemaComparisonResult (Java)
         ↓
   Direct HTML String Building
         ↓
     HTML File
```

**Files**: `SchemaDiffHtmlReportGenerator.java` (725 lines)

**Characteristics**:
- ✅ Fast and simple
- ✅ Works well for current needs
- ❌ Tightly couples data to presentation
- ❌ Cannot generate other formats without duplicating logic
- ❌ Hard to test HTML generation
- ❌ Cannot be consumed by APIs or external tools

---

## Proposed Architecture (New - Parallel Implementation)

```
SchemaComparisonResult (Java Domain Model)
         ↓
   SchemaDiffJsonSerializer
         ↓
   JSON Intermediate (Canonical Format)
         ↓         ↓         ↓
      HTML       PDF    Markdown
    (Template) (Template) (Template)
```

### Key Principles

1. **Single Source of Truth**: JSON is the canonical serialization format
2. **Template-Based Rendering**: HTML/PDF/Markdown use templates + JSON data
3. **API-First**: JSON format designed for programmatic consumption
4. **Backward Compatibility**: Existing HTML generator remains unchanged
5. **Progressive Migration**: New format available via configuration flag

---

## JSON Schema Design

### 1. **Top-Level Structure**

```json
{
  "$schema": "https://apex.mars.dev/schemas/schema-diff/v1.0.json",
  "metadata": {
    "generatedAt": "2026-01-17T21:30:45Z",
    "apexVersion": "2.1.0",
    "reportVersion": "1.0",
    "comparisonType": "database-to-database"
  },
  "source": { /* DataSource */ },
  "target": { /* DataSource */ },
  "summary": { /* ComparisonSummary */ },
  "columns": {
    "matching": [ /* ColumnDiff[] */ ],
    "added": [ /* ColumnDiff[] */ ],
    "removed": [ /* ColumnDiff[] */ ],
    "changed": [ /* ColumnDiff[] */ ]
  },
  "compatibility": { /* CompatibilityAnalysis */ },
  "recommendations": [ /* Recommendation[] */ ]
}
```

### 2. **DataSource Schema**

```json
{
  "name": "source-db",
  "type": "postgresql",
  "connection": {
    "host": "localhost",
    "port": 5432,
    "database": "source_db",
    "schema": "public",
    "username": "test"
  },
  "tableMetadata": {
    "tableName": "customers",
    "rowCount": 15420,
    "columns": 6,
    "primaryKeys": 1,
    "indexes": 3,
    "constraints": 2
  }
}
```

### 3. **ComparisonSummary Schema**

```json
{
  "totalColumns": {
    "source": 4,
    "target": 6
  },
  "statistics": {
    "matching": 4,
    "added": 2,
    "removed": 0,
    "changed": 0,
    "breaking": 0
  },
  "compatible": true,
  "migrationRisk": "low",
  "estimatedEffort": "minimal"
}
```

### 4. **ColumnDiff Schema**

```json
{
  "columnName": "email",
  "status": "matching",
  "source": {
    "dataType": "VARCHAR",
    "size": 100,
    "precision": null,
    "scale": null,
    "nullable": true,
    "primaryKey": false,
    "autoIncrement": false,
    "defaultValue": null,
    "constraints": []
  },
  "target": {
    "dataType": "VARCHAR",
    "size": 100,
    "precision": null,
    "scale": null,
    "nullable": true,
    "primaryKey": false,
    "autoIncrement": false,
    "defaultValue": null,
    "constraints": []
  },
  "differences": [],
  "breakingChange": false,
  "migrationAction": "none"
}
```

### 5. **Changed Column Example**

```json
{
  "columnName": "customer_name",
  "status": "changed",
  "source": {
    "dataType": "VARCHAR",
    "size": 100,
    "nullable": false
  },
  "target": {
    "dataType": "VARCHAR",
    "size": 50,
    "nullable": false
  },
  "differences": [
    {
      "property": "size",
      "sourceValue": 100,
      "targetValue": 50,
      "changeType": "narrowing",
      "breaking": true,
      "description": "Column size reduced from 100 to 50 characters"
    }
  ],
  "breakingChange": true,
  "migrationAction": "validate_data_length"
}
```

### 6. **CompatibilityAnalysis Schema**

```json
{
  "compatible": false,
  "overallRisk": "high",
  "breakingChanges": [
    {
      "severity": "error",
      "category": "data_loss",
      "description": "Removed column: middle_name (VARCHAR)",
      "affectedColumn": "middle_name",
      "recommendation": "Add column to target or map to alternate field"
    },
    {
      "severity": "warning",
      "category": "type_narrowing",
      "description": "Type narrowing: customer_name (VARCHAR(100) → VARCHAR(50))",
      "affectedColumn": "customer_name",
      "recommendation": "Validate all data fits in 50 characters or increase target size"
    }
  ],
  "safeChanges": [
    {
      "category": "additive",
      "description": "Added nullable column: phone (VARCHAR(20))",
      "affectedColumn": "phone",
      "impact": "Backward compatible - existing applications unaffected"
    }
  ]
}
```

### 7. **Recommendation Schema**

```json
{
  "priority": "high",
  "category": "schema_fix",
  "title": "Increase customer_name column size",
  "description": "Target column 'customer_name' is smaller than source (50 vs 100). This may cause data truncation.",
  "action": {
    "type": "alter_column",
    "sql": "ALTER TABLE customers ALTER COLUMN customer_name TYPE VARCHAR(100);",
    "estimatedImpact": "Low - simple column expansion",
    "rollbackPlan": "Revert to VARCHAR(50) if needed"
  },
  "automatable": true,
  "validationRequired": true
}
```

---

## Component Architecture

### 1. **SchemaDiffJsonSerializer.java**

**Location**: `apex-core/src/main/java/dev/mars/apex/core/service/schema/diff/json/`

**Responsibility**: Convert `SchemaComparisonResult` → JSON

```java
public class SchemaDiffJsonSerializer {
    
    private final ObjectMapper objectMapper;
    
    public SchemaDiffJsonSerializer() {
        this.objectMapper = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT)
            .setSerializationInclusion(JsonInclude.Include.NON_NULL)
            .registerModule(new JavaTimeModule());
    }
    
    /**
     * Serialize comparison result to JSON file.
     */
    public String toJsonFile(SchemaComparisonResult result, 
                             DataSourceContext sourceContext,
                             DataSourceContext targetContext,
                             String outputPath) throws IOException;
    
    /**
     * Serialize comparison result to JSON string.
     */
    public String toJsonString(SchemaComparisonResult result,
                               DataSourceContext sourceContext,
                               DataSourceContext targetContext) throws JsonProcessingException;
    
    /**
     * Deserialize JSON file to strongly-typed report object.
     */
    public SchemaDiffReport fromJsonFile(String jsonPath) throws IOException;
    
    /**
     * Deserialize JSON string to strongly-typed report object.
     */
    public SchemaDiffReport fromJsonString(String json) throws JsonProcessingException;
}
```

### 2. **SchemaDiffReport.java** (JSON Domain Model)

**Location**: `apex-core/src/main/java/dev/mars/apex/core/service/schema/diff/json/`

**Responsibility**: Strongly-typed representation of JSON structure

```java
public class SchemaDiffReport {
    
    @JsonProperty("$schema")
    private String schema = "https://apex.mars.dev/schemas/schema-diff/v1.0.json";
    
    @JsonProperty("metadata")
    private ReportMetadata metadata;
    
    @JsonProperty("source")
    private DataSourceInfo source;
    
    @JsonProperty("target")
    private DataSourceInfo target;
    
    @JsonProperty("summary")
    private ComparisonSummary summary;
    
    @JsonProperty("columns")
    private ColumnComparison columns;
    
    @JsonProperty("compatibility")
    private CompatibilityAnalysis compatibility;
    
    @JsonProperty("recommendations")
    private List<Recommendation> recommendations;
    
    // Builder pattern for construction
    public static class Builder { /* ... */ }
}
```

### 3. **JsonBasedHtmlReportGenerator.java**

**Location**: `apex-core/src/main/java/dev/mars/apex/core/service/schema/diff/json/`

**Responsibility**: Generate HTML from JSON using templates

```java
public class JsonBasedHtmlReportGenerator {
    
    private final TemplateEngine templateEngine;
    
    public JsonBasedHtmlReportGenerator() {
        // Use Handlebars, Thymeleaf, or Freemarker
        this.templateEngine = new HandlebarsTemplateEngine();
    }
    
    /**
     * Generate HTML report from JSON file.
     */
    public String generateFromJsonFile(String jsonPath, String outputPath) throws IOException;
    
    /**
     * Generate HTML report from SchemaDiffReport object.
     */
    public String generateFromReport(SchemaDiffReport report, String outputPath) throws IOException;
    
    /**
     * Generate HTML string from JSON (no file write).
     */
    public String generateHtmlString(SchemaDiffReport report) throws IOException;
}
```

### 4. **Template Structure** (Handlebars Example)

**Location**: `apex-core/src/main/resources/templates/schema-diff/`

```
templates/
└── schema-diff/
    ├── main.hbs                  # Main template (includes all sections)
    ├── sections/
    │   ├── header.hbs            # HTML head + CSS
    │   ├── metadata.hbs          # Report metadata
    │   ├── source-info.hbs       # Source database info
    │   ├── target-info.hbs       # Target database info
    │   ├── summary.hbs           # Statistics cards
    │   ├── matching-columns.hbs  # Matching columns table
    │   ├── added-columns.hbs     # Added columns table
    │   ├── removed-columns.hbs   # Removed columns table
    │   ├── changed-columns.hbs   # Changed columns table
    │   ├── breaking-changes.hbs  # Breaking changes alerts
    │   ├── recommendations.hbs   # Migration recommendations
    │   └── footer.hbs            # Footer
    └── partials/
        ├── column-row.hbs        # Single column row
        ├── stat-card.hbs         # Statistics card
        ├── badge.hbs             # Status badge
        └── alert.hbs             # Alert box
```

### 5. **Pipeline Integration**

**Modified**: `PipelineExecutor.java` (schema-diff step)

```java
// After schema comparison
SchemaComparisonResult result = schemaDiffService.compare(...);

// NEW: Check for JSON report generation
String jsonReportPath = (String) parameters.get("json-report-output");
if (jsonReportPath != null) {
    SchemaDiffJsonSerializer serializer = new SchemaDiffJsonSerializer();
    String jsonPath = serializer.toJsonFile(result, sourceContext, targetContext, jsonReportPath);
    pipelineContext.put("schema-diff-json-report", jsonPath);
    LOGGER.info("[Pipeline.SchemaDiff] JSON report generated: {}", jsonPath);
}

// NEW: Check for template-based HTML generation
String templateReportPath = (String) parameters.get("template-report-output");
Boolean useTemplateEngine = (Boolean) parameters.get("use-template-engine");
if (templateReportPath != null && Boolean.TRUE.equals(useTemplateEngine)) {
    // Load JSON (either from file or serialize in-memory)
    SchemaDiffReport report = new SchemaDiffReportBuilder()
        .fromComparisonResult(result, sourceContext, targetContext)
        .build();
    
    JsonBasedHtmlReportGenerator generator = new JsonBasedHtmlReportGenerator();
    String htmlPath = generator.generateFromReport(report, templateReportPath);
    pipelineContext.put("schema-diff-template-html-report", htmlPath);
    LOGGER.info("[Pipeline.SchemaDiff] Template-based HTML report generated: {}", htmlPath);
}

// EXISTING: Legacy HTML generation (unchanged)
String reportPath = (String) parameters.get("report-output");
if (reportPath != null) {
    SchemaDiffHtmlReportGenerator reportGenerator = new SchemaDiffHtmlReportGenerator();
    String generatedReportPath = reportGenerator.generateReport(result, sourceContext, targetContext, reportPath);
    // ... existing code
}
```

---

## YAML Configuration Changes

### Option 1: Separate Output Parameters

```yaml
steps:
  - name: "compare-schemas"
    type: "schema-diff"
    parameters:
      source-step: "read-source-schema"
      target-step: "read-target-schema"
      
      # Legacy HTML (hard-coded)
      report-output: "legacy-report.html"
      
      # NEW: JSON intermediate
      json-report-output: "schema-diff.json"
      
      # NEW: Template-based HTML
      template-report-output: "template-report.html"
      use-template-engine: true
```

### Option 2: Output Format Array

```yaml
steps:
  - name: "compare-schemas"
    type: "schema-diff"
    parameters:
      source-step: "read-source-schema"
      target-step: "read-target-schema"
      
      outputs:
        - format: "json"
          path: "reports/schema-diff.json"
        
        - format: "html-legacy"
          path: "reports/legacy.html"
        
        - format: "html-template"
          path: "reports/template.html"
          template: "default"  # or "bootstrap", "minimal", etc.
        
        - format: "markdown"
          path: "reports/schema-diff.md"
        
        - format: "pdf"
          path: "reports/schema-diff.pdf"
```

### Option 3: Report Generator Specification

```yaml
steps:
  - name: "compare-schemas"
    type: "schema-diff"
    parameters:
      source-step: "read-source-schema"
      target-step: "read-target-schema"
      
      report-generator: "template"  # "legacy" | "template" | "api"
      report-format: "html"         # "html" | "json" | "markdown" | "pdf"
      report-output: "reports/schema-diff.html"
      
      # Optional: Template customization
      template-name: "bootstrap"    # "default" | "bootstrap" | "minimal"
      template-params:
        theme: "dark"
        includeCharts: true
```

---

## Template Engine Selection

### Recommended: **Handlebars.java**

**Pros**:
- ✅ Logic-less templates (clean separation)
- ✅ Lightweight (minimal dependencies)
- ✅ JavaScript Handlebars compatibility
- ✅ Excellent performance
- ✅ Extensive helper system

**Cons**:
- ❌ Less feature-rich than Thymeleaf

**Example Template**:

```handlebars
{{!-- templates/schema-diff/sections/summary.hbs --}}
<div class="summary">
  <h3>Comparison Summary</h3>
  <div class="stats-grid">
    {{> stat-card label="Matching" value=summary.statistics.matching class="stat-matching"}}
    {{> stat-card label="Added" value=summary.statistics.added class="stat-added"}}
    {{> stat-card label="Removed" value=summary.statistics.removed class="stat-removed"}}
    {{> stat-card label="Changed" value=summary.statistics.changed class="stat-changed"}}
  </div>
  
  {{#if summary.compatible}}
    {{> alert type="success" message="✓ Compatible Migration: Target schema is backward compatible with source schema."}}
  {{else}}
    {{> alert type="danger" message="⚠️ Incompatible Migration: Breaking changes detected that may cause data loss or runtime errors."}}
  {{/if}}
</div>
```

### Alternative: **Thymeleaf**

**Pros**:
- ✅ Spring ecosystem integration
- ✅ Natural templating (valid HTML)
- ✅ Rich expression language
- ✅ Already in classpath (Spring Boot)

**Cons**:
- ❌ Heavier weight
- ❌ Steeper learning curve

---

## JSON Schema Validation

**Location**: `apex-core/src/main/resources/schemas/schema-diff-v1.0.json`

```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "$id": "https://apex.mars.dev/schemas/schema-diff/v1.0.json",
  "title": "APEX Schema Diff Report",
  "description": "Schema comparison report for database migrations",
  "type": "object",
  "required": ["metadata", "source", "target", "summary", "columns", "compatibility"],
  "properties": {
    "metadata": {
      "type": "object",
      "required": ["generatedAt", "apexVersion", "reportVersion"],
      "properties": {
        "generatedAt": {
          "type": "string",
          "format": "date-time"
        },
        "apexVersion": {
          "type": "string",
          "pattern": "^\\d+\\.\\d+\\.\\d+$"
        },
        "reportVersion": {
          "type": "string"
        },
        "comparisonType": {
          "type": "string",
          "enum": ["database-to-database", "csv-to-database", "csv-to-csv"]
        }
      }
    },
    "source": {
      "$ref": "#/definitions/DataSource"
    },
    "target": {
      "$ref": "#/definitions/DataSource"
    }
    // ... additional properties
  },
  "definitions": {
    "DataSource": {
      "type": "object",
      "required": ["name", "type"],
      "properties": {
        // ... definition
      }
    }
  }
}
```

**Validation in Code**:

```java
public class SchemaDiffJsonValidator {
    
    private final JsonSchema schema;
    
    public SchemaDiffJsonValidator() throws IOException {
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7);
        InputStream schemaStream = getClass().getResourceAsStream("/schemas/schema-diff-v1.0.json");
        this.schema = factory.getSchema(schemaStream);
    }
    
    public ValidationResult validate(String jsonReport) {
        Set<ValidationMessage> errors = schema.validate(new ObjectMapper().readTree(jsonReport));
        return new ValidationResult(errors.isEmpty(), errors);
    }
}
```

---

## Implementation Phases

### Phase 1: JSON Serialization (Week 1)

**Tasks**:
1. Create `json` package structure
2. Implement `SchemaDiffReport` domain model with Jackson annotations
3. Implement `SchemaDiffJsonSerializer`
4. Create JSON schema for validation
5. Unit tests for serialization/deserialization
6. Integration with pipeline (json-report-output parameter)

**Deliverables**:
- JSON reports generated from schema-diff pipeline
- Validated against JSON schema
- Unit test coverage: 90%+

**Files Created**:
- `SchemaDiffReport.java`
- `SchemaDiffJsonSerializer.java`
- `SchemaDiffReportBuilder.java`
- `ReportMetadata.java`, `DataSourceInfo.java`, etc. (supporting classes)
- `schema-diff-v1.0.json` (JSON schema)
- Unit tests

### Phase 2: Template Engine Integration (Week 2)

**Tasks**:
1. Add Handlebars.java dependency
2. Create template directory structure
3. Implement `JsonBasedHtmlReportGenerator`
4. Create base templates (header, footer, sections)
5. Create partials (column-row, stat-card, badge, alert)
6. Integration tests comparing template output to legacy output

**Deliverables**:
- HTML reports generated from JSON
- Template-based rendering functional
- Visual parity with legacy reports

**Files Created**:
- `JsonBasedHtmlReportGenerator.java`
- `TemplateEngine.java` (abstraction)
- `HandlebarsTemplateEngine.java`
- 15+ template files (.hbs)
- Integration tests

### Phase 3: Additional Formats (Week 3)

**Tasks**:
1. Implement Markdown generator from JSON
2. Implement PDF generator from JSON (using iText or similar)
3. Support multiple output formats in single pipeline run
4. Create format registry/factory pattern

**Deliverables**:
- Markdown report generation
- PDF report generation (optional)
- Multi-format output support

**Files Created**:
- `JsonBasedMarkdownReportGenerator.java`
- `JsonBasedPdfReportGenerator.java` (optional)
- `ReportGeneratorFactory.java`

### Phase 4: Documentation & Migration (Week 4)

**Tasks**:
1. Update user guide with new format options
2. Create migration guide (legacy → template)
3. Create template customization guide
4. Performance benchmarking (legacy vs template)
5. Deprecation plan for legacy generator

**Deliverables**:
- Updated documentation
- Migration guide
- Performance comparison report

---

## Testing Strategy

### 1. **Unit Tests**

```java
@Test
void shouldSerializeComparisonResultToJson() {
    SchemaComparisonResult result = createTestResult();
    SchemaDiffJsonSerializer serializer = new SchemaDiffJsonSerializer();
    
    String json = serializer.toJsonString(result, sourceContext, targetContext);
    
    assertNotNull(json);
    assertTrue(json.contains("\"compatible\": true"));
    assertTrue(json.contains("\"matching\": 4"));
}

@Test
void shouldDeserializeJsonToReport() throws IOException {
    String json = loadTestJson("test-schema-diff.json");
    SchemaDiffJsonSerializer serializer = new SchemaDiffJsonSerializer();
    
    SchemaDiffReport report = serializer.fromJsonString(json);
    
    assertEquals(4, report.getSummary().getStatistics().getMatching());
    assertTrue(report.getCompatibility().isCompatible());
}

@Test
void shouldValidateAgainstJsonSchema() {
    String json = createTestJson();
    SchemaDiffJsonValidator validator = new SchemaDiffJsonValidator();
    
    ValidationResult result = validator.validate(json);
    
    assertTrue(result.isValid(), "JSON should conform to schema");
}
```

### 2. **Integration Tests**

```java
@Test
void shouldGenerateHtmlFromJson() throws IOException {
    // Generate JSON
    SchemaDiffJsonSerializer serializer = new SchemaDiffJsonSerializer();
    String jsonPath = serializer.toJsonFile(comparisonResult, sourceCtx, targetCtx, "test.json");
    
    // Generate HTML from JSON
    JsonBasedHtmlReportGenerator generator = new JsonBasedHtmlReportGenerator();
    String htmlPath = generator.generateFromJsonFile(jsonPath, "test.html");
    
    // Verify HTML contains expected content
    String html = Files.readString(Path.of(htmlPath));
    assertTrue(html.contains("Comparison Summary"));
    assertTrue(html.contains("4</span> <!-- matching count -->"));
}

@Test
void shouldMatchLegacyOutputStructure() throws IOException {
    // Generate both reports
    String legacyHtml = generateLegacyReport(result);
    String templateHtml = generateTemplateReport(result);
    
    // Compare key sections (not exact match, but structural equivalence)
    assertTrue(bothContain(legacyHtml, templateHtml, "Matching Columns"));
    assertTrue(bothContain(legacyHtml, templateHtml, "Added Columns"));
    assertEquals(extractStatValue(legacyHtml, "matching"), 
                 extractStatValue(templateHtml, "matching"));
}
```

### 3. **Performance Tests**

```java
@Test
void shouldGenerateJsonFasterThanLegacyHtml() {
    long jsonTime = measureTime(() -> serializer.toJsonFile(...));
    long legacyTime = measureTime(() -> legacyGenerator.generateReport(...));
    
    assertTrue(jsonTime < legacyTime * 1.5, 
               "JSON generation should be comparable to legacy");
}

@Test
void shouldGenerateTemplateHtmlWithinAcceptableTime() {
    long templateTime = measureTime(() -> templateGenerator.generateFromJsonFile(...));
    
    assertTrue(templateTime < 500, 
               "Template HTML generation should complete in < 500ms");
}
```

---

## Benefits of Layered Architecture

### 1. **Separation of Concerns**

| Layer | Responsibility | Changes Independently |
|-------|---------------|----------------------|
| **Domain Model** | Business logic, comparison rules | When comparison algorithm changes |
| **JSON Serialization** | Data structure, schema definition | When API contract changes |
| **HTML Templates** | Presentation, styling, UX | When design changes |

### 2. **Multiple Output Formats**

```
SchemaComparisonResult → JSON → {
    HTML (Handlebars)
    Markdown (Template)
    PDF (iText + Template)
    REST API Response
    Excel (Apache POI)
    CSV (Summary)
}
```

### 3. **Template Customization**

Users can create custom templates without Java code:

```yaml
parameters:
  template-report-output: "custom-report.html"
  template-name: "bootstrap-dark"
  template-directory: "custom-templates/"
```

### 4. **API Integration**

```java
@RestController
@RequestMapping("/api/schema-diff")
public class SchemaDiffController {
    
    @PostMapping("/compare")
    public ResponseEntity<SchemaDiffReport> compareSchemas(@RequestBody CompareRequest request) {
        SchemaComparisonResult result = schemaDiffService.compare(...);
        SchemaDiffReport report = reportBuilder.fromComparisonResult(result);
        return ResponseEntity.ok(report);  // Auto-serialized to JSON
    }
}
```

### 5. **Testability**

```java
// Test JSON structure without HTML parsing
SchemaDiffReport report = serializer.fromJsonString(json);
assertEquals(4, report.getSummary().getStatistics().getMatching());

// Test template rendering with mock data
SchemaDiffReport mockReport = createMockReport();
String html = generator.generateFromReport(mockReport);
assertContains(html, "4 Matching Columns");
```

---

## Migration Path

### Phase 1: Parallel Operation (6 months)

```yaml
# Both legacy and new formats available
outputs:
  - format: "html-legacy"
    path: "reports/legacy.html"
  - format: "json"
    path: "reports/schema-diff.json"
  - format: "html-template"
    path: "reports/template.html"
```

### Phase 2: Default Transition (3 months)

```yaml
# New format becomes default
report-generator: "template"  # Default changed from "legacy"
report-output: "reports/schema-diff.html"
```

### Phase 3: Legacy Deprecation (3 months)

```yaml
# Legacy marked deprecated
report-generator: "legacy"  # @Deprecated - use "template" instead
```

### Phase 4: Legacy Removal (After 1 year)

- Remove `SchemaDiffHtmlReportGenerator.java`
- Keep in Git history for reference
- Document breaking change in release notes

---

## File Structure

```
apex-core/src/main/java/dev/mars/apex/core/service/schema/diff/
├── json/
│   ├── SchemaDiffJsonSerializer.java
│   ├── SchemaDiffJsonValidator.java
│   ├── SchemaDiffReportBuilder.java
│   ├── model/
│   │   ├── SchemaDiffReport.java
│   │   ├── ReportMetadata.java
│   │   ├── DataSourceInfo.java
│   │   ├── ComparisonSummary.java
│   │   ├── ColumnComparison.java
│   │   ├── ColumnDiff.java
│   │   ├── CompatibilityAnalysis.java
│   │   ├── BreakingChange.java
│   │   ├── Recommendation.java
│   │   └── MigrationAction.java
│   └── generators/
│       ├── JsonBasedHtmlReportGenerator.java
│       ├── JsonBasedMarkdownReportGenerator.java
│       ├── JsonBasedPdfReportGenerator.java
│       └── TemplateEngine.java
├── SchemaDiffHtmlReportGenerator.java  # PRESERVED - unchanged
└── SchemaDiffService.java

apex-core/src/main/resources/
├── schemas/
│   └── schema-diff-v1.0.json
└── templates/
    └── schema-diff/
        ├── main.hbs
        ├── sections/
        │   ├── header.hbs
        │   ├── metadata.hbs
        │   ├── summary.hbs
        │   ├── source-info.hbs
        │   ├── target-info.hbs
        │   ├── matching-columns.hbs
        │   ├── added-columns.hbs
        │   ├── removed-columns.hbs
        │   ├── changed-columns.hbs
        │   ├── breaking-changes.hbs
        │   ├── recommendations.hbs
        │   └── footer.hbs
        └── partials/
            ├── column-row.hbs
            ├── stat-card.hbs
            ├── badge.hbs
            └── alert.hbs

apex-core/src/test/java/dev/mars/apex/core/service/schema/diff/json/
├── SchemaDiffJsonSerializerTest.java
├── SchemaDiffJsonValidatorTest.java
├── JsonBasedHtmlReportGeneratorTest.java
├── TemplateEngineTest.java
└── fixtures/
    ├── test-schema-diff.json
    ├── expected-html-output.html
    └── expected-markdown-output.md
```

---

## Dependencies

### Maven Dependencies (pom.xml)

```xml
<!-- JSON Schema Validation -->
<dependency>
    <groupId>com.networknt</groupId>
    <artifactId>json-schema-validator</artifactId>
    <version>1.0.87</version>
</dependency>

<!-- Handlebars Template Engine -->
<dependency>
    <groupId>com.github.jknack</groupId>
    <artifactId>handlebars</artifactId>
    <version>4.3.1</version>
</dependency>

<!-- Already present: Jackson for JSON -->
<!-- Already present: SLF4J for logging -->
```

---

## Success Criteria

### Functional

- ✅ JSON reports generated with complete schema information
- ✅ JSON validates against defined schema
- ✅ HTML generated from JSON matches legacy output (structurally)
- ✅ Template customization works (user can provide custom templates)
- ✅ Multiple output formats supported (JSON, HTML, Markdown)

### Non-Functional

- ✅ JSON generation: < 50ms for typical schema
- ✅ HTML template rendering: < 200ms
- ✅ JSON report size: < 500KB for 100-column schema
- ✅ Memory overhead: < 10MB additional for template engine
- ✅ Test coverage: > 90% for new components

### Quality

- ✅ No breaking changes to existing API
- ✅ Legacy generator remains fully functional
- ✅ Documentation complete and clear
- ✅ Examples provided for all formats
- ✅ Migration guide available

---

## Risk Assessment

| Risk | Probability | Impact | Mitigation |
|------|------------|--------|------------|
| Template engine performance | Low | Medium | Benchmark early, cache compiled templates |
| JSON schema versioning | Medium | Medium | Use semantic versioning, support backward compatibility |
| Template complexity | Medium | Low | Keep templates simple, provide examples |
| Memory overhead | Low | Low | Use streaming for large schemas |
| Breaking changes | Low | High | Keep legacy generator, gradual migration |

---

## Next Steps

1. **Review & Approve Design** - Stakeholder review of this document
2. **Prototype JSON Serialization** - Build Phase 1 components
3. **Template Selection** - Finalize Handlebars vs Thymeleaf
4. **Phase 1 Implementation** - JSON serialization (1 week)
5. **Phase 2 Implementation** - Template rendering (1 week)
6. **Testing & Documentation** - Comprehensive testing (1 week)
7. **Production Deployment** - Parallel operation with legacy (ongoing)

---

## Appendix A: Sample JSON Output

See: `apex-core/src/test/resources/fixtures/sample-schema-diff-report.json`

## Appendix B: Sample Template

See: `apex-core/src/main/resources/templates/schema-diff/main.hbs`

## Appendix C: Performance Benchmarks

TBD after implementation

---

**Document Version**: 1.0  
**Author**: APEX Development Team  
**Date**: 2026-01-17  
**Status**: Design Review
