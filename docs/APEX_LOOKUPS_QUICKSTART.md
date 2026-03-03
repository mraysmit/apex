# APEX Lookups — Quick Start Guide

## What is a Lookup?

A **lookup enrichment** is the core mechanism for fetching reference data and injecting it into your processing context. An enrichment of `type: "lookup-enrichment"` evaluates a SpEL `lookup-key` expression, queries a data source, and maps result fields onto the target object via `field-mappings`.

---

## Anatomy of a Lookup (YAML)

Every lookup enrichment follows this structure:

```yaml
enrichments:
  - id: "my-lookup"
    type: "lookup-enrichment"
    condition: "<SpEL guard — when should this execute?>"
    
    lookup-config:
      lookup-key: "<SpEL expression to compute the key>"
      lookup-dataset:
        type: "<inline | database | rest-api | file-system | csv-file | yaml-file>"
        # ... type-specific config ...
    
    field-mappings:
      - source-field: "<field from lookup result>"
        target-field: "<field to set on context>"
        required: true/false
        expression: "<optional SpEL transformation>"
        default-value: <fallback>
```

---

## 6 Supported Data Source Types

| Type | YAML `type:` | Use Case | Key Config |
|------|-------------|----------|------------|
| **Inline** | `inline` | Small static reference data (<100 records) embedded in YAML | `key-field`, `data: [...]` |
| **Database** | `database` | Dynamic data from H2, PostgreSQL, etc. | `data-source-ref`, `query`, `parameters` |
| **REST API** | `rest-api` | Real-time external HTTP APIs | `data-source-ref`, `operation-ref`, `parameters` |
| **File System** | `file-system` | JSON or XML files with format config | `file-path`, `key-field`, `format-config` |
| **CSV File** | `csv-file` | CSV reference files | `file-path`, `key-field` |
| **YAML File** | `yaml-file` | External YAML data files | `file-path`, `key-field` |

---

## Type 1: Inline Lookups (Simplest)

Data is embedded directly in the YAML — great for small static reference tables:

```yaml
# From SimpleInlineDataSourceTest.yaml
lookup-config:
  lookup-key: "#currencyCode"
  lookup-dataset:
    type: "inline"
    key-field: "code"
    data:
      - code: "USD"
        name: "US Dollar"
        symbol: "$"
      - code: "EUR"
        name: "Euro"
        symbol: "€"
```

**SpEL-powered keys** can perform complex computations:

```yaml
# From LookupBasicInlineTest.yaml — mathematical operations in lookup keys
lookup-key: "{'totalAmount': #quantity * #unitPrice, 'discountRate': #customerTier == 'PLATINUM' ? 0.15 : 0.05}"
```

---

## Type 2: Database Lookups (H2 / PostgreSQL)

Defined with a `data-sources` section and referenced via `data-source-ref`:

### H2 Example

```yaml
# From MultiParameterLookupTest.yaml
data-sources:
  - name: "settlement-database"
    type: "database"
    source-type: "h2"
    connection:
      database: "./target/h2-demo/settlement_demo"
      username: "sa"
      password: ""

enrichments:
  - id: "multi-param-lookup"
    type: "lookup-enrichment"
    condition: "#counterpartyId != null && #currency != null"
    lookup-config:
      lookup-key: "{'counterpartyId': #counterpartyId, 'currency': #currency}"
      lookup-dataset:
        type: "database"
        data-source-ref: "settlement-database"
        query: |
          SELECT instruction_id, counterparty_name
          FROM settlement_instructions si
          WHERE si.counterparty_id = :counterpartyId AND si.currency = :currency
        parameters:
          - field: "counterpartyId"
            type: "string"
          - field: "currency"
            type: "string"
```

### PostgreSQL Example

PostgreSQL adds connection pooling, caching, health checks, and JSONB support:

```yaml
# From PostgreSQLSimpleLookupTest.yaml
data-sources:
  - name: "postgresql-customer-database"
    type: "database"
    source-type: "postgresql"
    connection:
      database: "apex_test"
      username: "apex_user"
      password: "apex_pass"
      host: "localhost"
      port: 5432
      connection-pool:
        max-size: 10
        min-size: 2
        connection-timeout: 30000
        idle-timeout: 600000
    cache:
      enabled: true
      ttlSeconds: 1800
      maxSize: 5000
      eviction-policy: "LRU"
    healthCheck:
      enabled: true
      query: "SELECT 1"
      intervalSeconds: 60

enrichments:
  - id: "postgresql-simple-lookup-demo"
    type: "lookup-enrichment"
    condition: "#customerId != null && #customerId != ''"
    lookup-config:
      lookup-key: "#customerId"
      lookup-dataset:
        type: "database"
        data-source-ref: "postgresql-customer-database"
        query: "SELECT customer_name, customer_type, tier, region FROM customers WHERE customer_id = :customerId"
        parameters:
          - field: "customerId"
            type: "string"
        cache-enabled: true
        cache-ttl-seconds: 900
    field-mappings:
      - source-field: "customer_name"
        target-field: "customerName"
        required: true
      - source-field: "tier"
        target-field: "customerTier"
        required: true
```

### Named Query References

Instead of inline SQL, you can reference pre-defined queries from the data source:

```yaml
# From Map-External-to-Internal-Code.yaml
lookup-dataset:
  type: "database"
  data-source-ref: "postgresql-database-localtest"
  query-ref: "getIsTranslate"   # Defined in the data-source's queries: section
  parameters:
    - field: "TRANSLATION_TYPE"
      type: "string"
```

> **Important:** H2 returns UPPERCASE column names, PostgreSQL returns lowercase. Field mappings must match accordingly.

---

## Type 3: REST API Lookups

```yaml
# From SimpleRestApiDataSourceTest.yaml
data-sources:
  - name: "test-api"
    type: "rest-api"
    connection:
      base-url: "http://localhost:8080"
      timeout: 5000
      retry-attempts: 2
    endpoints:
      customer-lookup: "/api/customers/{key}"
    cache:
      enabled: true
      ttlSeconds: 600
      maxSize: 1000

enrichments:
  - id: "customer-api-lookup"
    type: "lookup-enrichment"
    condition: "#customerId != null"
    lookup-config:
      lookup-key: "#customerId"
      lookup-dataset:
        type: "rest-api"
        data-source-ref: "test-api"
        operation-ref: "customer-lookup"
    field-mappings:
      - source-field: "customerName"
        target-field: "customerName"
      - source-field: "creditRating"
        target-field: "creditRating"
```

### Multi-Parameter REST Endpoints

Pass parameters to URL templates:

```yaml
# From RestApiBasicLookupTest.yaml
lookup-config:
  lookup-key: "#fromCurrency + '_' + #toCurrency + '_' + #amount"
  lookup-dataset:
    type: "rest-api"
    data-source-ref: "basic-rest-api-server"
    operation-ref: "currency-conversion"
    parameters:
      from: "#fromCurrency"
      to: "#toCurrency"
      amount: "#amount"
```

### Caching Configuration

```yaml
# From RestApiCachingDemoTest-fast.yaml
cache:
  enabled: true
  ttlSeconds: 300         # 5 minutes TTL
  maxIdleSeconds: 180     # 3 minutes max idle time
  maxSize: 100            # Max entries (LRU eviction)
  keyPrefix: "fast-demo"  # Cache key prefix for identification
```

---

## Types 4–6: File System Lookups (CSV, JSON, XML)

### CSV File

```yaml
# From SimpleCsvDataSourceTest.yaml
lookup-dataset:
  type: "csv-file"
  key-field: "id"
  file-path: "demo-data/csv/load-test-customers.csv"
```

### JSON File

```yaml
# From SimpleJsonDataSourceTest.yaml
lookup-dataset:
  type: "file-system"
  key-field: "id"
  file-path: "demo-data/json/products.json"
  format-config:
    type: "json"
    root-path: "$"
```

### XML File

```yaml
# From FileSystemLookupDemoTest-xml.yaml
lookup-dataset:
  type: "file-system"
  key-field: "id"
  file-path: "demo-data/xml/products.xml"
  format-config:
    type: "xml"
    recordElement: "product"
    rootElement: "products"
```

---

## External Data-Source References (Clean Architecture)

Business logic YAML files can reference **separate infrastructure config files** via `data-source-refs`, keeping concerns separated:

```yaml
# Business logic file (lean)
data-source-refs:
  - name: "postgresql-database-localtest"
    source: "postgresql-database-localtest.yaml"    # Separate file with connection details
    enabled: true

enrichments:
  - id: "get-is-translate"
    type: "lookup-enrichment"
    lookup-config:
      lookup-dataset:
        data-source-ref: "postgresql-database-localtest"
        query-ref: "getIsTranslate"
```

This pattern allows infrastructure teams to manage connection details independently from the business rules.

---

## Field Mappings

Every lookup uses `field-mappings` to transfer results into the processing context:

| Property | Purpose |
|----------|---------|
| `source-field` | Column/field name from lookup result |
| `target-field` | Destination field name on context object |
| `required` | If `true`, null result fails the enrichment |
| `expression` | SpEL transformation applied to the value |
| `default-value` | Fallback when source is null |

### Basic Mapping

```yaml
field-mappings:
  - source-field: "customer_name"
    target-field: "customerName"
    required: true
```

### With SpEL Transformation

```yaml
# From PostgreSQLSimpleLookupTest.yaml — type conversion via expression
field-mappings:
  - source-field: "employee_count"
    target-field: "customerEmployeeCount"
    required: false
    expression: "#value != null ? T(java.lang.Integer).parseInt(#value.toString()) : 0"
  - source-field: "financial_value"
    target-field: "customerFinancialValue"
    required: false
    expression: "#value != null ? T(java.lang.Long).parseLong(#value.toString()) : 0L"
```

### With Default Values

```yaml
field-mappings:
  - source-field: "is_translate"
    target-field: "IS_TRANSLATE"
    default-value: 0
```

---

## Advanced Patterns

### Nested Field Navigation

Extract fields from nested object structures:

```yaml
# From NestedFieldLookupDemoTest.yaml
enrichments:
  # Step 1: Extract nested field
  - id: "nested-counterparty-country-extraction"
    type: "calculation-enrichment"
    condition: "#trade != null && #trade['counterparty'] != null"
    calculation-config:
      expression: "#trade['counterparty']['countryCode']"
      result-field: "extractedCountryCode"

  # Step 2: Use extracted value as lookup key
  - id: "country-settlement-lookup"
    type: "lookup-enrichment"
    condition: "#extractedCountryCode != null"
    lookup-config:
      lookup-key: "#extractedCountryCode"
      lookup-dataset:
        type: "inline"
        key-field: "countryCode"
        data:
          - countryCode: "US"
            settlementSystem: "DTC"
            custodianBank: "Bank of New York Mellon"
          - countryCode: "GB"
            settlementSystem: "CREST"
            custodianBank: "HSBC Custody Services"
```

### Multi-Parameter Database Lookups

Use SpEL map expressions to pass multiple parameters:

```yaml
# From MultiParameterLookupTest.yaml
lookup-config:
  lookup-key: "{'counterpartyId': #counterpartyId, 'instrumentType': #instrumentType, 'currency': #currency, 'market': #market}"
  lookup-dataset:
    type: "database"
    data-source-ref: "settlement-database"
    query: |
      SELECT instruction_id, counterparty_name
      FROM settlement_instructions si
      WHERE si.counterparty_id = :counterpartyId
        AND si.instrument_type = :instrumentType
        AND si.currency = :currency
        AND si.market = :market
    parameters:
      - field: "counterpartyId"
        type: "string"
      - field: "instrumentType"
        type: "string"
      - field: "currency"
        type: "string"
      - field: "market"
        type: "string"
```

### SpEL Array/Collection Search Patterns

```yaml
# From ArraySearchBasedSpelTest.yaml

# Find first matching element (.^[condition])
expression: "#trade.legs.^[legType == 'FLOATING']"

# Find all matching elements (.?[condition])
expression: "#trade.legs.?[currency == 'USD']"

# Count matching elements
expression: "#trade.legs.?[notionalAmount > 1000000].size()"

# Navigate into matched element
expression: "#trade.legs.^[legType == 'FLOATING']?.notionalAmount"
```

---

## Processing Flow (Under the Hood)

```
RulesEngine.evaluate(data)
  → EnrichmentProcessor sorts enrichments by priority
    → For each lookup-enrichment:
      1. Evaluate condition (SpEL guard)
      2. Resolve LookupService via DatasetLookupServiceFactory
         (dispatches by type: inline → in-memory map, database → SQL, rest-api → HTTP, etc.)
      3. Evaluate lookup-key (SpEL) to get the search key
      4. lookupService.transform(key) → Map<String, Object>
      5. Apply field-mappings (with expressions, defaults, required checks)
      6. Set enriched fields on target object
```

### Key Classes

| Class | Package | Role |
|-------|---------|------|
| `EnrichmentProcessor` | `...service.enrichment` | Core orchestrator — processes all enrichment types |
| `DatasetLookupServiceFactory` | `...service.lookup` | Factory creating type-specific lookup services |
| `DatasetLookupService` | `...service.lookup` | In-memory map-based lookup (inline, file, yaml) |
| `DatabaseLookupService` | `...service.lookup` | SQL query execution against databases |
| `RestApiLookupService` | `...service.lookup` | HTTP calls to REST endpoints |
| `LookupServiceRegistry` | `...service.lookup` | Registry for pre-registered named services |
| `YamlEnrichment` | `...config.model` | YAML model with inner classes: `LookupConfig`, `LookupDataset`, `FieldMapping` |
| `YamlDataSource` | `...config.model` | Data source connection configuration model |
| `YamlDataSourceRef` | `...config.model` | External data-source reference pointer |

---

## Where to Find Examples

| Directory | Content |
|-----------|---------|
| `apex-demo/src/test/resources/.../lookup/` | 94 YAML files — inline, database, REST API, file system, caching, nested, multi-param |
| `apex-demo/src/test/resources/.../datasources/inline/` | Inline data source examples |
| `apex-demo/src/test/resources/.../datasources/database/` | H2 database examples |
| `apex-demo/src/test/resources/.../datasources/restapi/` | REST API examples |
| `apex-demo/src/test/resources/.../datasources/filesystem/` | CSV, JSON, XML file examples |
| `apex-demo/src/test/resources/.../conditional/` | External data-source ref patterns |
| `apex-demo/src/test/resources/.../sequencing/` | Enrichment ordering and sequencing |

---

## Quick Reference: Minimal Working Examples

### Inline (Zero Dependencies)
```yaml
enrichments:
  - id: "currency-lookup"
    type: "lookup-enrichment"
    condition: "#currencyCode != null"
    lookup-config:
      lookup-key: "#currencyCode"
      lookup-dataset:
        type: "inline"
        key-field: "code"
        data:
          - code: "USD"
            name: "US Dollar"
    field-mappings:
      - source-field: "name"
        target-field: "currencyName"
```

### Database
```yaml
data-sources:
  - name: "my-db"
    type: "database"
    source-type: "h2"
    connection:
      database: "./target/h2-demo/mydb"
      username: "sa"
      password: ""

enrichments:
  - id: "db-lookup"
    type: "lookup-enrichment"
    condition: "#id != null"
    lookup-config:
      lookup-key: "#id"
      lookup-dataset:
        type: "database"
        data-source-ref: "my-db"
        query: "SELECT name, status FROM my_table WHERE id = :id"
        parameters:
          - field: "id"
            type: "string"
    field-mappings:
      - source-field: "NAME"
        target-field: "itemName"
```

### CSV File
```yaml
enrichments:
  - id: "csv-lookup"
    type: "lookup-enrichment"
    condition: "#customerId != null"
    lookup-config:
      lookup-key: "#customerId"
      lookup-dataset:
        type: "csv-file"
        key-field: "id"
        file-path: "data/customers.csv"
    field-mappings:
      - source-field: "name"
        target-field: "customerName"
```
