# APEX Configuration UI Mockups (ASCII, Revised)

Purpose: Implementation-ready ASCII mockups for a ref-aware APEX configuration UI.
Scope: All APEX YAML configuration types — rules, enrichments, data sources, transformations, pipelines, error recovery, components, scenarios, and categories.

Design goals:
- Eliminate ref ambiguity with a central catalog and resolver.
- Match real APEX YAML vocabulary and composition patterns.
- Cover more than single-file enrichment flow (components/scenarios/registries).

---

## Scope and Coverage

### In Scope (this document covers 100% of APEX YAML configuration types)

**Screens 0–7: Lookup Enrichment Wizard** (linear creation flow)
- Catalog discovery, ref resolution, and health scoring (Screen 0)
- Configuration bundle intent and file targeting (Screen 1)
- External data source config — all 6 source types, standalone and embedded modes (Screen 2)
- Dataset contract definition with field dictionary (Screen 3)
- Process dataset profile and runtime semantics (Screen 3A)
- Process dataset test bench and stage snapshots (Screen 3B)
- Lookup mapping — inline, embedded query, and external query ref (Screen 4)
- Enrichment builder — all 4 types: lookup, calculation, field, conditional-mapping (Screen 5)
- Artifact preview, ref integrity, and approval gate (Screen 6)
- Composition into components, scenarios, and registries (Screen 7)

**Screens 8–16: Standalone Authoring Screens** (accessible from Catalog, Screen 0)
- Rule authoring — conditions, severity, priority, result-field, success/error codes (Screen 8)
- Rule group and rule refs — AND/OR groups, cross-file references, error handling (Screen 9)
- Rule chain builder — all 6 chain patterns (Screen 10)
- Enrichment group builder — AND/OR grouping, cross-group references (Screen 11)
- Transformation builder — conditional transformations, actions-true/false, nesting (Screen 12)
- Error recovery configuration — severity policies, 4 recovery strategies (Screen 13)
- ETL orchestration section — pipeline orchestration and sink configuration (Screens 14–15)
- Category management — hierarchy, labels, governance metadata (Screen 16)

### Screen Navigation

Screens 0–7 form a linear wizard for creating lookup enrichment configurations.
Screens 8–16 are standalone authoring screens accessible from the Catalog (Screen 0).
All screens share the Artifact Preview (Screen 6) for validation and YAML generation.

```text
Screen 0 (Catalog)
├── Wizard Flow: 1 → 2 → 3 → 3A → 3B → 4 → 5 → 6 → 7
├── Rule Authoring: 8 → 9 → 10 → 6
├── Enrichment Groups: 11 → 6
├── Transformations: 12 → 6
├── Error Recovery: 13 → 6
├── ETL Orchestration: 14 → 15 → 6
└── Categories: 16 → 6
```

### Non-Technical First Interaction Model

Design intent: default authoring paths should not require users to write SpEL manually.

1. **Basic Mode (default)**
  - User selects Field → Operator → Value from dropdowns.
  - UI generates the expression automatically.
  - UI shows a plain-language sentence preview.

2. **Advanced Mode (optional)**
  - Raw expression editor for expert users.
  - Round-trip supported: switching back to Basic keeps equivalent logic where possible.

3. **Live Test Panel (always visible on condition screens)**
  - Test input sample.
  - Condition result (`true` / `false`).
  - Computed lookup key / parameters.
  - Matched row preview (for lookups).

4. **Guardrail Behavior**
  - Non-blocking warnings for uncommon patterns.
  - Blocking errors only for invalid syntax, unresolved refs, or incompatible types.

5. **Starter Templates**
  - Is present
  - Equals
  - Greater than / Less than
  - In list
  - Starts with / Contains
  - All of / Any of (AND/OR)

### Process Dataset Contract and Lifecycle (Cross-Cutting)

APEX evaluates rules and enrichments against a runtime process dataset (map-like context). This contract is central and applies to every screen.

1. **Dataset Shape Contract**
  - Authoring tools declare expected fields, types, aliases, and nullability.
  - Screen 3 is the canonical place to define logical field names used by Basic and Advanced expression builders.

2. **Evaluation Context Contract**
  - Expressions resolve against a single process context with both root fields and aliases.
  - Example: `#user.age > 18` requires `user.age` to exist and be type-compatible at evaluation time.

3. **Type and Null Semantics**
  - Missing path, null value, and type coercion behavior are validated during authoring tests and again at pre-write integrity checks.
  - Unsafe coercions and ambiguous paths are surfaced as warnings or hard failures based on policy.

4. **Mutation Lifecycle**
  - Rules may emit result fields; enrichments may add/overwrite mapped fields; transformations may rewrite values.
  - Each stage contributes to an evolving dataset snapshot consumed by subsequent stages.

5. **Validation Lifecycle**
  - On-screen tests validate local logic against sample payloads.
  - Screen 6 re-validates full cross-screen dataset assumptions before write.
  - Composition in Screen 7 confirms dataset compatibility across files/components/scenarios.

6. **Traceability Contract**
  - Every screen records which dataset fields it reads and writes.
  - Preview and validation views show before/after snapshots for critical fields where available.

---

## APEX Type Coverage Matrix

```text
+------------------------------------------------------------------------------------------------------------------+
| APEX Config Type             | Create | Edit | Validate | Ref Resolve | Preview YAML | Compose | Screens        |
+-----------------------------+--------+------+----------+-------------+--------------+---------+----------------+
| process-dataset-contract    |   Y    |  Y   |    Y     |      -      |      Y       |    Y    | 3, 3A, 3B, 6   |
| external-data-config        |   Y    |  Y   |    Y     |      Y      |      Y       |    Y    | 2, 6           |
| enrichment (lookup)         |   Y    |  Y   |    Y     |      Y      |      Y       |    Y    | 4, 5, 6        |
| enrichment (calculation)    |   Y    |  Y   |    Y     |      Y      |      Y       |    Y    | 5, 6           |
| enrichment (field)          |   Y    |  Y   |    Y     |      Y      |      Y       |    Y    | 5, 6           |
| enrichment (cond-mapping)   |   Y    |  Y   |    Y     |      Y      |      Y       |    Y    | 5, 6           |
| enrichment-groups           |   Y    |  Y   |    Y     |      Y      |      Y       |    Y    | 11, 6          |
| rule-config                 |   Y    |  Y   |    Y     |      Y      |      Y       |    Y    | 8, 9, 6        |
| rule-chain                  |   Y    |  Y   |    Y     |      Y      |      Y       |    Y    | 10, 6          |
| component                   |   Y    |  Y   |    Y     |      Y      |      Y       |    Y    | 7              |
| scenario                    |   Y    |  Y   |    Y     |      Y      |      Y       |    Y    | 7              |
| scenario-registry           |   Y    |  Y   |    Y     |      Y      |      Y       |    Y    | 7              |
| transformation              |   Y    |  Y   |    Y     |      Y      |      Y       |    Y    | 12, 6          |
| pipeline-config             |   Y    |  Y   |    Y     |      Y      |      Y       |    Y    | 14, 6          |
| data-sinks (section)        |   Y    |  Y   |    Y     |      Y      |      Y       |    Y    | 15, 6          |
| categories (section)        |   Y    |  Y   |    Y     |      Y      |      Y       |    Y    | 16             |
| error-recovery              |   Y    |  Y   |    Y     |      -      |      Y       |    -    | 13             |
+-----------------------------+--------+------+----------+-------------+--------------+---------+----------------+
| Y = covered in this document                                                                                     |
+------------------------------------------------------------------------------------------------------------------+
```

### Required Metadata Fields by Document Type

Base required fields for **all** document types (enforced by `MetadataValidator`):
`id`, `name`, `version`, `description`, `type`

```text
+----------------------------+----------------------------------------------------------------------+
| Document Type              | Additional Required Fields (beyond base 5)                           |
+----------------------------+----------------------------------------------------------------------+
| rule-config                | author                                                               |
| enrichment                 | author                                                               |
| external-data-config       | author                                                               |
| rule-chain                 | author                                                               |
| pipeline / pipeline-config | author                                                               |
| scenario                   | business-domain, owner                                               |
| scenario-registry          | created-by                                                           |
| dataset                    | source                                                               |
| component                  | (none beyond base 5)                                                 |
| transformation-config      | (none beyond base 5)                                                 |
+----------------------------+----------------------------------------------------------------------+
| Common metadata fields: tags, created, last-modified                                            |
| Type-specific schemas may additionally support governance fields such as                         |
| business-domain, business-owner, display-name, criticality, sla-ms                              |
+----------------------------+----------------------------------------------------------------------+
```

---

## Screen 0: Catalog and Resolver Home

```text
+------------------------------------------------------------------------------------------------+
| APEX Authoring                                                                                  |
| Configuration Catalog                                                                           |
+------------------------------------------------------------------------------------------------+
| Workspace: apex-rules-engine                                                                    |
| Scan Source: (o) Folder ( ) Catalog API ( ) Hybrid                                             |
| Root: [ c:/Users/.../apex-rules-engine ] [ Scan Now ] [ Rescan Changed ]                      |
|                                                                                                |
| Filters: [ Type: Any v ] [ Domain: Any v ] [ Health: Any v ] [ Owner: Any v ] [ Search.... ] |
|                                                                                                |
| +----+---------------------------+----------------------+--------+----------+---------+-------+ |
| | ID | Name                      | Type                 | Health | Refs In  | Refs Out | Warn | |
| +----+---------------------------+----------------------+--------+----------+---------+-------+ |
| | 01 | otc-validation            | rule-config          | 92     | 4        | 2       | 0     | |
| | 02 | customer-database-local   | external-data-config | 88     | 7        | 1       | 1     | |
| | 03 | settlement-component      | component            | 81     | 2        | 5       | 0     | |
| | 04 | otc-option-us             | scenario             | 76     | 1        | 3       | 2     | |
| +----+---------------------------+----------------------+--------+----------+---------+-------+ |
|                                                                                                |
| Resolver Queue                                                                                  |
| [MISSING] data-source-ref: customer-db-prod in config/otc-enrich.yaml                          |
| [TYPE-MISMATCH] query-ref getCustomerTier -> source type rest-api, expected database            |
| [AMBIGUOUS] rule-ref trade-limit-check found in 2 files                                         |
|                                                                                                |
| [ Open Builder ] [ View Dependency Graph ] [ Export Catalog Snapshot ]                          |
+------------------------------------------------------------------------------------------------+
```

Notes:
- Process dataset behavior: this screen indexes dataset read/write usage metadata per artifact so downstream screens can warn about missing fields before runtime.
- Reference behavior: this screen is the source of truth for resolver status and symbol discovery used by all downstream selectors.
- `Refs In` and `Refs Out` are computed from parsed YAML plus pending wizard draft artifacts.
- Any item shown as MISSING, AMBIGUOUS, or TYPE-MISMATCH here is surfaced inline on dependent screens and blocks write on hard-fail policy.

---

## Screen 1: Configuration Intent and File Targets

```text
+------------------------------------------------------------------------------------------------+
| New Configuration Bundle                                                                        |
+------------------------------------------------------------------------------------------------+
| Step 1 of 9                                                                                     |
|                                                                                                |
| Bundle Name*              [ Customer Settlement Baseline                                      ] |
| Environment*              [ local-dev v ]                                                       |
| Root Folder*              [ config/customer-settlement/                                       ] |
|                                                                                                |
| Create Artifacts                                                                               |
| [x] external-data-config   file: data-sources/customer-db.yaml           (author required)      |
| [x] enrichment             file: enrichments/customer-tier-enrichment.yaml (author required)    |
| [ ] rule-config            file: rules/customer-validation.yaml          (author required)      |
| [ ] component              file: components/customer-settlement-component.yaml                  |
| [ ] scenario               file: scenarios/customer-settlement-scenario.yaml (domain required)  |
| [ ] scenario-registry      file: scenario-registry/customer-registry.yaml  (created-by required) |
|                                                                                                |
| Bundle Metadata                                                                                 |
| Author*                    [ trading-team@company.com                                         ] |
| Business Domain            [ Derivatives Trading v ]                                            |
| Version                    [ 1.0.0 ]                                                            |
| Tags                       [ settlement, customer, otc ]                                        |
| Processing Mode            [ document-order v ]  (document-order | priority-order)              |
| Description                [ Customer settlement baseline configuration                       ] |
|                                                                                                |
| Data Source References (document-level wiring)                                                   |
| +----+---------------------------+--------------------------------------------+-----------+     |
| | #  | Ref Name                  | External Source File                       | Status    |     |
| +----+---------------------------+--------------------------------------------+-----------+     |
| | 1  | customer-database         | data-sources/customer-db.yaml              | RESOLVED  |     |
| +----+---------------------------+--------------------------------------------+-----------+     |
| [ + Add Ref ] (auto-populated from selected artifacts; editable)                                |
|                                                                                                |
| Resolver Policy*           [ Fail on missing refs v ]                                           |
| Naming Convention*         [ kebab-case ids, snake_case db fields v ]                           |
|                                                                                                |
| [ Validate Plan ]                                         [ Cancel ] [ Save & Next > ]         |
+------------------------------------------------------------------------------------------------+
```

Notes:
- Author and Business Domain are propagated into generated `metadata:` blocks per artifact.
- Data Source References become the `data-source-refs:` section in business logic YAML files.
- Ref status is validated against the catalog: RESOLVED, MISSING, or AMBIGUOUS.
- Process dataset behavior: this screen defines the initial dataset intent for the bundle (expected business object scope, naming conventions, and processing mode assumptions used by all later validation).

---

## Screen 2: Connection and External Data Config

APEX supports two data source wiring modes. This screen handles both:
- **Standalone external config** — produces a separate `data-sources/*.yaml` file (type: `external-data-config`)
- **Embedded in business file** — produces a `data-sources:` section inside the enrichment YAML, referenced by `connection-name`

### Screen 2A: Standalone External Data Config (default)

Use when: data source is shared across multiple business logic files.
Generates: `data-sources/customer-db.yaml` with `type: "external-data-config"`.
Referenced by: `data-source-ref` + `query-ref` in business YAML files.
Supports all 6 APEX source types: `database`, `rest-api`, `file-system`, `cache`, `message-queue`, `custom`.

```text
+------------------------------------------------------------------------------------------------+
| External Data Source Builder                                                                    |
+------------------------------------------------------------------------------------------------+
| Step 2 of 9                                                                                     |
| Artifact: external-data-config -> data-sources/customer-db.yaml                                 |
|                                                                                                |
| Config Mode*              (o) Standalone External Config    ( ) Embedded in Business File       |
|                                                                                                |
| Data Source Name*          [ customer-database                                                 ] |
| Source Type*               [ database v ]                                                       |
|                            (database | rest-api | file-system | cache | message-queue | custom) |
|                                                                                                |
+-- Source Type Panel (swaps based on Source Type selection) ------------------------------------+ |
|                                                                                                |
| === Database (active when Source Type = database) =========================================== | |
| DB Engine*                 [ POSTGRESQL v ]                                                     |
| Host*                      [ db-prod.company.net                                               ] |
| Port*                      [ 5432 ]                                                             |
| Database*                  [ customer_master                                                   ] |
| Schema                     [ public v ]                                                         |
| Username*                  [ customer_reader                                                   ] |
| Password*                  [ *************** ]                                                   |
| Properties (kv)            [ sslmode=require;connectTimeout=5000                              ] |
|                                                                                                |
| Named Queries                                                                                   |
| Format*                    (o) Map Format    ( ) Array Format   (APEX 2.2+ dual-format)        |
| +----+---------------------+------------------------------------------------------------------+ |
| | #  | Query Name           | SQL                                                              | |
| +----+---------------------+------------------------------------------------------------------+ |
| | 1  | getCustomerTier      | SELECT tier FROM customer_profile_vw WHERE customer_id=:id      | |
| | 2  | getCustomerProfile   | SELECT customer_name,tier,status FROM customer_profile_vw ...   | |
| +----+---------------------+------------------------------------------------------------------+ |
| Parameter Names            [ id, status ]  (for parameterized queries)                          |
|                                                                                                |
| Init Scripts (optional — executed when data source is initialized)                               |
| +----+------------------------------------------------------------------+                      |
| | #  | Script Path                                                       |                      |
| +----+------------------------------------------------------------------+                      |
| | 1  | scripts/init-schema.sql                                           |                      |
| +----+------------------------------------------------------------------+                      |
| [ + Add Script ]                                                                                |
|                                                                                                |
| === REST API (active when Source Type = rest-api) =========================================== | |
| Base URL*                  [ https://api.company.net/v2                                        ] |
| Authentication*            [ Bearer Token v ]  (Bearer Token | Basic | API Key | OAuth2)       |
| Token / Credentials        [ ************                                                      ] |
|                                                                                                |
| Named Operations (dual-format: map or array)                                                    |
| +----+---------------------+---------+--------------------+----------------------------------+ |
| | #  | Operation Name       | Method  | Path               | Response Mapping                 | |
| +----+---------------------+---------+--------------------+----------------------------------+ |
| | 1  | getCustomer          | GET     | /customers/{id}    | $.data                           | |
| | 2  | searchSettlements    | POST    | /settlements/query | $.results                        | |
| +----+---------------------+---------+--------------------+----------------------------------+ |
|                                                                                                |
| Circuit Breaker            [x] enabled   Failure Threshold [ 5 ]   Timeout (ms) [ 10000 ]     |
| Health Check               [x] enabled   Endpoint [ /health ]                                  |
|                                                                                                |
| === File System (active when Source Type = file-system) ===================================== | |
| File Path*                 [ data/reference/instruments.csv                                    ] |
| File Format*               [ CSV v ]  (CSV | JSON | XML)                                       |
| Format Config                                                                                   |
|   Delimiter                [ , ]     Header Row [x]     Encoding [ UTF-8 ]                     |
|                                                                                                |
| === Cache (active when Source Type = cache) ================================================= | |
| Cache Backend*             [ Redis v ]  (Redis | In-Memory)                                    |
| Host*                      [ cache-prod.company.net ]   Port [ 6379 ]                          |
| Key Patterns               [ customer:{id}, settlement:{tradeId} ]                             |
| Cache TTL (seconds)        [ 3600 ]                                                             |
|                                                                                                |
| === Message Queue (active when Source Type = message-queue) ================================= | |
| Queue Type*                [ Kafka v ]                                                          |
| Bootstrap Servers*         [ kafka-prod:9092                                                   ] |
| Topics                                                                                          |
| +----+---------------------+---------------------+                                             |
| | #  | Topic Name           | Consumer Group       |                                             |
| +----+---------------------+---------------------+                                             |
| | 1  | trade-events         | settlement-consumer  |                                             |
| +----+---------------------+---------------------+                                             |
|                                                                                                |
| === Custom (active when Source Type = custom) =============================================== | |
| Implementation Class*      [ com.company.apex.CustomDataSource                                 ] |
| Custom Properties (kv)     [ endpoint=internal://data-service;timeout=5000                    ] |
|                                                                                                |
+-- End Source Type Panel ---------------------------------------------------------------------- |
|                                                                                                |
| Caching (common to all source types)                                                            |
| Cache Enabled              [x]    Cache TTL (seconds) [ 300 ]                                  |
|                                                                                                |
| Validation                                                                                      |
| [x] Connection / endpoint reachable                                                              |
| [x] Source type config complete                                                                  |
| [x] Query / operation names unique                                                               |
|                                                                                                |
| [ Test Connection ] [ Discover Schema ]                    [ < Back ] [ Save & Next > ]       |
+------------------------------------------------------------------------------------------------+
```

### Screen 2B: Embedded Data Source (connection-name mode)

Use when: data source is specific to one business file (demos, prototyping, single-use lookups).
Generates: `data-sources:` section inside the enrichment YAML file.
Referenced by: `connection-name` in `lookup-dataset` (not `data-source-ref`).

Real examples: `H2SimpleDatabaseConnectivityTest.yaml`, `SimpleDatabaseDataSourceTest.yaml`.

```text
+------------------------------------------------------------------------------------------------+
| Embedded Data Source Builder                                                                    |
+------------------------------------------------------------------------------------------------+
| Step 2 of 9                                                                                     |
| Artifact: embedded in enrichments/customer-tier-enrichment.yaml                                  |
|                                                                                                |
| Config Mode*              ( ) Standalone External Config    (o) Embedded in Business File       |
|                                                                                                |
| Connection Name*           [ demo-database                                                     ] |
| Source Type*               [ database v ]                                                       |
| DB Engine*                 [ H2 v ]                                                             |
| Database Path*             [ ./target/h2-demo/customer_test                                    ] |
| Username*                  [ sa ]                                                               |
| Password                   [                                                                   ] |
|                                                                                                |
| Note: Embedded data sources produce a `data-sources:` block inside the business YAML file.      |
| Lookups reference this source via `connection-name: "demo-database"` (not data-source-ref).     |
| For shared/reusable data sources, use Standalone External Config mode instead.                   |
|                                                                                                |
| Validation                                                                                      |
| [x] Connection reachable                                                                         |
| [x] Connection name unique within file                                                           |
|                                                                                                |
| [ Test Connection ]                                        [ < Back ] [ Save & Next > ]       |
+------------------------------------------------------------------------------------------------+
```

### Generated YAML Comparison

```text
Standalone (2A) generates:                    Embedded (2B) generates:
--- data-sources/customer-db.yaml ---         --- enrichments/customer-tier-enrichment.yaml ---
metadata:                                     metadata:
  id: "customer-db-config"                      id: "customer-tier-enrichment"
  name: "Customer Database"                     name: "Customer Tier Enrichment"
  version: "1.0.0"                              version: "1.0.0"
  description: "Customer master DB"             description: "Tier lookup enrichment"
  type: "external-data-config"                  type: "enrichment"
  author: "trading-team@company.com"            author: "trading-team@company.com"
data-sources:                                 data-sources:
  - name: "customer-database"                   - name: "demo-database"
    source-type: "postgresql"                     source-type: "h2"
    connection:                                   connection:
      host: "db-prod.company.net"                   database: "./target/h2-demo/..."
      ...
    queries:                                  enrichments:
      getCustomerTier: "SELECT ..."             - id: "customer-lookup"
                                                  lookup-config:
Business file references via:                       lookup-dataset:
  data-source-ref: "customer-database"                connection-name: "demo-database"
  query-ref: "getCustomerTier"                        query: "SELECT ... WHERE id = :id"
```

Notes:
- Process dataset behavior: this screen defines where process dataset records originate and what source-level shape is expected before mapping into logical fields.
- Reference behavior: standalone mode publishes `data-source` names and named queries/operations into the catalog so Screens 1, 4, and 5 can resolve `data-source-ref` and `query-ref`.
- Embedded mode does not publish cross-file refs; it only exposes `connection-name` inside the current document context.
- Resolution timing: connection/query symbol validation runs on `Test Connection`, `Save & Next`, and full validation.

---

## Screen 3: Dataset Contract Builder

```text
+------------------------------------------------------------------------------------------------+
| Dataset Contract Builder                                                                         |
+------------------------------------------------------------------------------------------------+
| Step 3 of 9                                                                                     |
| Source: customer-database / public.customer_profile_vw                                           |
|                                                                                                |
| Dataset ID*                [ customer-profile-dataset                                          ] |
| Source Object*             [ customer_profile_vw v ]                                             |
| Key Strategy*              [ Single Key v ]                                                      |
| Key Field (physical)*      [ customer_id v ]                                                     |
| Input Alias (logical)*     [ customerId ]                                                        |
| Active Filter (SpEL)       [ #status == 'ACTIVE' ]                                               |
|                                                                                                |
| Field Contract                                                                                  |
| +----+----------------------+----------------------+------------+ Required + Indexed +---------+ |
| | #  | Logical Field         | Physical Field       | Data Type  |   [x]    |   [ ]   | Search  | |
| +----+----------------------+----------------------+------------+----------+---------+---------+ |
| | 1  | customerId            | customer_id          | string     |   [x]    |   [x]   |         | |
| | 2  | customerTier          | tier                 | string     |   [x]    |   [ ]   |         | |
| | 3  | customerStatus        | status               | string     |   [x]    |   [ ]   |         | |
| | 4  | regionCode            | region_code          | string     |   [ ]    |   [ ]   |         | |
| +----+----------------------+----------------------+------------+----------+---------+---------+ |
|                                                                                                |
| [ Validate Dataset Contract ]                               [ < Back ] [ Save & Next > ]       |
+------------------------------------------------------------------------------------------------+
```

Notes:
- Process dataset behavior: this is the canonical dataset contract screen for logical field names, type expectations, and alias mappings consumed by expressions in later screens.
- Reference behavior: this screen defines local logical fields and aliases consumed by SpEL builders in Screens 4, 5, 8, and 12.
- No external reference is created here; scope is the current artifact unless explicitly exported through composition in Screen 7.

---

## Screen 3A: Process Dataset Profile and Runtime Semantics

```text
+------------------------------------------------------------------------------------------------+
| Process Dataset Profile                                                                         |
+------------------------------------------------------------------------------------------------+
| Step 4 of 9                                                                                    |
| Context: Dataset contract from Screen 3                                                        |
|                                                                                                |
| Dataset Root Alias*         [ user v ]   (user | trade | root | custom)                       |
| Dataset Mode*               [ strict-schema v ]  (strict-schema | permissive-schema)          |
| Missing Path Policy*        [ error v ]  (error | warn-and-null | default-value)              |
| Type Coercion Policy*       [ safe-only v ]  (none | safe-only | aggressive)                  |
| Null Handling*              [ explicit-null v ] (explicit-null | empty-as-null)               |
|                                                                                                |
| Runtime Field Semantics                                                                         |
| +----+----------------------+------------+------------+----------------+---------------------+ |
| | #  | Field Path            | Type       | Nullable   | Missing Action | Default (optional)  | |
| +----+----------------------+------------+------------+----------------+---------------------+ |
| | 1  | user.age              | integer    | [ ] no     | error          | --                  | |
| | 2  | user.countryCode      | string     | [x] yes    | warn-and-null  | --                  | |
| | 3  | user.status           | string     | [x] yes    | default-value  | "UNKNOWN"           | |
| +----+----------------------+------------+------------+----------------+---------------------+ |
| [ + Add Field Semantics ]                                                                        |
|                                                                                                |
| Expression Probe                                                                                |
| Expression                [ #user.age > 18                                                    ] |
| Required Paths            [ user.age ]                                                         |
| Type Check               [ PASS ] user.age => integer                                          |
| Null Safety              [ PASS ] policy requires non-null                                     |
|                                                                                                |
| Dataset Compatibility Summary                                                                    |
| [PASS] All required paths declared                                                              |
| [WARN] 2 optional fields have no defaults                                                       |
| [PASS] No unsafe coercion paths detected                                                        |
|                                                                                                |
| [ Validate Semantics ] [ Preview Contract YAML ]              [ < Back ] [ Save & Next > ]    |
+------------------------------------------------------------------------------------------------+
```

Notes:
- Process dataset behavior: this screen defines runtime semantics for path resolution, coercion, and null/missing handling that govern expression evaluation across all authoring screens.
- Output from this screen is consumed by on-screen test panels (Screens 4, 5, 8, 10, 12) and by final integrity validation (Screen 6).

### Screen 3A UI to YAML Mapping

| UI Field | YAML Key | Example |
|----------|----------|---------|
| Dataset Root Alias | `process-dataset.root-alias` | `"user"` |
| Dataset Mode | `process-dataset.mode` | `"strict-schema"` |
| Missing Path Policy | `process-dataset.runtime.missing-path-policy` | `"error"` |
| Type Coercion Policy | `process-dataset.runtime.type-coercion-policy` | `"safe-only"` |
| Null Handling | `process-dataset.runtime.null-handling` | `"explicit-null"` |
| Field Path | `process-dataset.runtime.field-semantics[].path` | `"user.age"` |
| Field Type | `process-dataset.runtime.field-semantics[].type` | `"integer"` |
| Nullable | `process-dataset.runtime.field-semantics[].nullable` | `false` |
| Missing Action | `process-dataset.runtime.field-semantics[].missing-action` | `"error"` |
| Default Value | `process-dataset.runtime.field-semantics[].default-value` | `"UNKNOWN"` |

### Screen 3A API Contract

| Action | Method + Path | Purpose |
|--------|----------------|---------|
| Validate semantics | `POST /api/v1/process-datasets/validate-semantics` | Validate path existence, nullability, and coercion rules. |
| Probe expression | `POST /api/v1/process-datasets/probe-expression` | Evaluate a single expression against profile + sample payload. |
| Save profile | `PUT /api/v1/process-datasets/{datasetId}/profile` | Persist runtime semantics for a dataset contract. |
| Get profile | `GET /api/v1/process-datasets/{datasetId}/profile` | Load saved dataset profile for editing. |

---

## Screen 3B: Process Dataset Test Bench and Stage Snapshots

```text
+------------------------------------------------------------------------------------------------+
| Process Dataset Test Bench                                                                      |
+------------------------------------------------------------------------------------------------+
| Step 5 of 9                                                                                    |
| Profile: strict-schema / missing=error / coercion=safe-only                                   |
|                                                                                                |
| Test Payload Library                                                                            |
| +----+---------------------------+-------------------------------+----------------------------+ |
| | #  | Payload Name              | Purpose                       | Expected Result            | |
| +----+---------------------------+-------------------------------+----------------------------+ |
| | 1  | adult-us-user             | baseline pass                 | #user.age > 18 => true     | |
| | 2  | minor-user                | baseline fail                 | #user.age > 18 => false    | |
| | 3  | missing-age               | required-path failure         | missing path error         | |
| +----+---------------------------+-------------------------------+----------------------------+ |
| [ + Add Payload ] [ Import JSON ]                                                             |
|                                                                                                |
| Payload Editor (selected: adult-us-user)                                                       |
| {                                                                                              |
|   "user": {                                                                                   |
|     "id": "U-1001",                                                                         |
|     "age": 27,                                                                               |
|     "countryCode": "US",                                                                    |
|     "status": "ACTIVE"                                                                      |
|   }                                                                                            |
| }                                                                                              |
|                                                                                                |
| Stage Snapshot Timeline                                                                         |
| [Input] -> [After Rules] -> [After Enrichments] -> [After Transformations]                    |
|                                                                                                |
| Snapshot Diff (Input vs After Rules)                                                           |
| +--------------------------+-----------------------+-----------------------+                    |
| | Field                    | Before                | After                 |                    |
| +--------------------------+-----------------------+-----------------------+                    |
| | user.age                 | 27                    | 27                    |                    |
| | requiredFieldsValid      | --                    | true                  |                    |
| | customerTier             | --                    | --                    |                    |
| +--------------------------+-----------------------+-----------------------+                    |
|                                                                                                |
| Evaluation Results                                                                              |
| [PASS] #user.age > 18 => true                                                                  |
| [PASS] #user.countryCode == 'US' => true                                                       |
| [WARN] customerTier unavailable until enrichment stage                                          |
|                                                                                                |
| [ Run Full Dataset Simulation ] [ Save Fixtures ]             [ < Back ] [ Save & Next > ]    |
+------------------------------------------------------------------------------------------------+
```

Notes:
- Process dataset behavior: this screen makes dataset lifecycle observable with stage-by-stage snapshots and expression outcomes before users proceed to lookup/rule/enrichment authoring.
- Fixture payloads created here are reused by condition test panels in later screens and by regression validation in Screen 6.

### Screen 3B UI to YAML Mapping

| UI Field | YAML Key | Example |
|----------|----------|---------|
| Payload Name | `process-dataset.fixtures[].name` | `"adult-us-user"` |
| Payload Body | `process-dataset.fixtures[].payload` | `{ "user": { "age": 27 } }` |
| Purpose | `process-dataset.fixtures[].purpose` | `"baseline pass"` |
| Expected Result | `process-dataset.fixtures[].expected[]` | `"#user.age > 18 => true"` |
| Snapshot Stage | `process-dataset.snapshot-stages[]` | `"after-rules"` |

### Screen 3B API Contract

| Action | Method + Path | Purpose |
|--------|----------------|---------|
| Save fixture | `PUT /api/v1/process-datasets/{datasetId}/fixtures/{fixtureName}` | Create or update a named test payload fixture. |
| List fixtures | `GET /api/v1/process-datasets/{datasetId}/fixtures` | Load fixture library for test bench. |
| Run simulation | `POST /api/v1/process-datasets/{datasetId}/simulate` | Execute rule/enrichment/transformation pipeline and return staged snapshots. |
| Diff snapshots | `POST /api/v1/process-datasets/{datasetId}/snapshot-diff` | Compute field-level before/after diff between two stages. |

---

## Screen 4: Lookup Mapping Builder (Ref-Aware)

```text
+------------------------------------------------------------------------------------------------+
| Lookup Mapping Builder                                                                           |
+------------------------------------------------------------------------------------------------+
| Step 6 of 9                                                                                     |
|                                                                                                |
| Mapping ID*                [ get-customer-tier                                                 ] |
| Lookup Enrichment ID*      [ customer-tier-lookup                                               ] |
| Condition*                 [ Basic Builder v ]  (Basic Builder | Advanced Expression)          |
| Condition Preview          [ Run lookup when customerId is present ]                            |
| Generated Expression       [ #customerId != null ]                                               |
| Lookup Key (SpEL)*         [ #customerId ]                                                       |
|                                                                                                |
| Dataset Mode*              ( ) Inline      ( ) Embedded Query      (o) External Query Ref      |
|                                                                                                |
+-- Mode Panel (swaps based on Dataset Mode selection) -----------------------------------------+|
|                                                                                                |
| === External Query Ref Mode (active when Dataset Mode = External Query Ref) ================== |
| Data Source Ref*           [ customer-database v ]   (from catalog / Screen 1 refs)             |
| Query Ref*                 [ getCustomerTier v ]     (from named queries in external config)    |
| Parameters                                                                                      |
| +----+------------------+--------------+-------------------------------+                        |
| | #  | Query Param Name | Type         | Value Source (SpEL)           |                        |
| +----+------------------+--------------+-------------------------------+                        |
| | 1  | id               | string       | #customerId                    |                        |
| +----+------------------+--------------+-------------------------------+                        |
|                                                                                                |
| === Embedded Query Mode (active when Dataset Mode = Embedded Query) ========================== |
| Connection Name*           [ demo-database v ]       (from embedded data-sources in this file)  |
| SQL Query*                 [ SELECT tier FROM customer_profile_vw WHERE customer_id = :id     ] |
| Parameters                                                                                      |
| +----+------------------+--------------+-------------------------------+                        |
| | #  | Param Name        | Type         | Value Source (SpEL)           |                        |
| +----+------------------+--------------+-------------------------------+                        |
| | 1  | id               | string       | #customerId                    |                        |
| +----+------------------+--------------+-------------------------------+                        |
| Cache Enabled              [x]    Cache TTL (seconds)   [ 300 ]                                 |
|                                                                                                |
| === Inline Mode (active when Dataset Mode = Inline) ========================================= |
| Key Field*                 [ code v ]                                                           |
| Inline Data                                                                                     |
| +----+----------+------------------+------------------+                                         |
| | #  | code     | name             | symbol           |                                         |
| +----+----------+------------------+------------------+                                         |
| | 1  | USD      | US Dollar        | $                |                                         |
| | 2  | EUR      | Euro             | €                |                                         |
| +----+----------+------------------+------------------+                                         |
| [ + Add Row ]                                                                                   |
|                                                                                                |
+-- End Mode Panel ---------------------------------------------------------------------------- |
|                                                                                                |
| Field Mappings (common to all modes)                                                            |
| +----+---------------------+-----------------------+ Required + Default ---------------------+ |
| | #  | Source Field         | Target Field          |   [x]    | [ ]                          | |
| +----+---------------------+-----------------------+----------+------------------------------+ |
| | 1  | tier                 | customerTier          |   [x]    |                              | |
| +----+---------------------+-----------------------+----------+------------------------------+ |
|                                                                                                |
| Binding Preview (generated — changes based on selected mode)                                    |
| [External Ref]  lookup-config.lookup-dataset.data-source-ref = "customer-database"             |
|                 lookup-config.lookup-dataset.query-ref = "getCustomerTier"                      |
| [Embedded]      lookup-config.lookup-dataset.connection-name = "demo-database"                 |
|                 lookup-config.lookup-dataset.query = "SELECT tier FROM ..."                     |
| [Inline]        lookup-config.lookup-dataset.type = "inline"                                   |
|                 lookup-config.lookup-dataset.key-field = "code"                                 |
|                 lookup-config.lookup-dataset.data = [ ... ]                                     |
|                                                                                                |
| Condition Test (sample record)                                                                   |
| Input:  { "customerId": "C12345" }                                                            |
| Result: condition = true, lookup-key = "C12345", rows matched = 1                              |
|                                                                                                |
| [ Resolve Refs ] [ Validate Mapping ]                       [ < Back ] [ Save & Next > ]       |
+------------------------------------------------------------------------------------------------+
```

Notes:
- Process dataset behavior: parameter value sources and field mappings explicitly map from process dataset input fields to lookup outputs, making read/write field flow visible.
- Reference behavior: `data-source-ref` options come from Screen 1 bundle refs plus catalog-discovered external-data-config artifacts.
- `query-ref` options are filtered by selected `data-source-ref` and source type; incompatible query/operation types are marked TYPE-MISMATCH.
- Failure behavior: missing or ambiguous refs block `Save & Next` when resolver policy is hard-fail; warn-only policy permits draft save but blocks final write in Screens 6/7.

---

## Screen 5: Enrichment Builder

```text
+------------------------------------------------------------------------------------------------+
| Enrichment Builder                                                                               |
+------------------------------------------------------------------------------------------------+
| Step 7 of 9                                                                                     |
|                                                                                                |
| Enrichment ID*             [ customer-tier-lookup                                              ] |
| Enrichment Type*           [ lookup-enrichment v ]                                               |
|                            (lookup-enrichment | calculation-enrichment |                         |
|                             field-enrichment | conditional-mapping-enrichment)                   |
| Condition                  [ Basic Builder v ]  (Basic Builder | Advanced Expression)           |
| Condition Preview          [ Run enrichment when customerId is present ]                         |
| Generated Expression       [ #customerId != null ]                                               |
| Result Field               [ tierLookupResult ]  (stores enrichment outcome in data map)        |
| Success Code               [ TIER_FOUND ]                                                        |
| Error Code                 [ TIER_LOOKUP_FAILED ]                                                |
| Execution Priority         [ 100 ]                                                               |
|                                                                                                |
| Execution Settings                                                                               |
| Timeout (ms)               [ 5000 ]           Retry Count       [ 2 ]                           |
| Retry Delay (ms)           [ 500 ]            Backoff Multiplier [ 1.5 ]                        |
| Circuit Breaker            [ ] enabled        Failure Threshold  [ 5 ]                          |
|                                                                                                |
+-- Enrichment Type Panel (swaps based on Enrichment Type selection) ---------------------------+ |
|                                                                                                |
| === Lookup Enrichment (active when type = lookup-enrichment) ================================ | |
| Lookup Key (SpEL)*         [ #customerId ]                                                      |
| Map To Field               [ customerData ]  (stores full lookup result; String or comma-list)  |
|                                                                                                |
| Dataset Mode*              ( ) Inline      ( ) Embedded Query      (o) External Query Ref      |
| (see Screen 4 for full dataset mode detail with inline/embedded/external panels)                |
|                                                                                                |
| Field Mappings                                                                                   |
| +----+---------------------+-----------------------+----------+------------------------------+  |
| | #  | Source Field         | Target Field          | Required | Default Value                |  |
| +----+---------------------+-----------------------+----------+------------------------------+  |
| | 1  | tier                 | customerTier          |   [x]    |                              |  |
| | 2  | status               | customerStatus        |   [ ]    | UNKNOWN                      |  |
| +----+---------------------+-----------------------+----------+------------------------------+  |
|                                                                                                |
| === Calculation Enrichment (active when type = calculation-enrichment) ====================== | |
| Calculations (execute sequentially — later rows can reference earlier fields)                   |
| +----+---------------------+--------------------------------------------------------------+    |
| | #  | Target Field         | Expression (SpEL)                                            |    |
| +----+---------------------+--------------------------------------------------------------+    |
| | 1  | tradeValue           | #quantity * #price                                           |    |
| | 2  | commission           | #tradeValue * 0.001                                          |    |
| | 3  | netAmount            | #tradeValue + #commission                                    |    |
| | 4  | formattedAmount      | T(String).format('%.2f', #netAmount)                         |    |
| +----+---------------------+--------------------------------------------------------------+    |
| [ + Add Calculation ]                                                                           |
|                                                                                                |
| === Field Enrichment (active when type = field-enrichment) ================================== | |
| Field Mappings (source can be SpEL expression, target supports nested paths)                    |
| +----+-------------------------------+---------------------------+----------+---------+         |
| | #  | Source Expression (SpEL)       | Target Field              | Required | Default |         |
| +----+-------------------------------+---------------------------+----------+---------+         |
| | 1  | #trade.instrument.id          | instrumentId              |   [x]    |         |         |
| | 2  | #counterparty?.lei            | counterpartyLEI           |   [ ]    | UNKNOWN |         |
| | 3  | #amount > 0 ? 'CREDIT' : 'DEBIT' | transactionDirection  |   [x]    |         |         |
| +----+-------------------------------+---------------------------+----------+---------+         |
| [ + Add Mapping ]                                                                               |
|                                                                                                |
| === Conditional-Mapping Enrichment (active when type = conditional-mapping-enrichment) ====== | |
| Conditional Mappings (evaluated top-to-bottom; first match wins — waterfall pattern)            |
| +----+-------------------------------+-----------------------+-------------------------------+  |
| | #  | Condition (SpEL)               | Target Field          | Value / Expression            |  |
| +----+-------------------------------+-----------------------+-------------------------------+  |
| | 1  | #amount > 1000000              | riskLevel             | HIGH                          |  |
| | 2  | #amount > 100000               | riskLevel             | MEDIUM                        |  |
| | 3  | (default — no condition)       | riskLevel             | LOW                           |  |
| +----+-------------------------------+-----------------------+-------------------------------+  |
| [ + Add Mapping Rule ]                                                                          |
|                                                                                                |
+-- End Enrichment Type Panel ----------------------------------------------------------------- | |
|                                                                                                |
| Validation                                                                                       |
| [x] enrichment config complete                                                                   |
| [x] SpEL syntax valid in all expressions                                                         |
| [x] target fields valid                                                                          |
| [x] referenced data sources / queries resolved                                                   |
| [x] condition test passed on sample input                                                        |
|                                                                                                |
| [ Validate Enrichment ]                                  [ < Back ] [ Save & Next > ]          |
+------------------------------------------------------------------------------------------------+
```

Notes:
- Process dataset behavior: this screen is a primary dataset mutation point where target fields are created/updated and result fields are materialized for downstream rules/groups/chains.
- Reference behavior: in lookup mode, this screen reuses the same dataset resolution contract as Screen 4 (external `data-source-ref`/`query-ref`, embedded `connection-name`, or inline data).
- Enrichment IDs authored here are published as local symbols for Screen 11 and as file-level symbols for Screen 7 composition.
- Renaming an enrichment ID triggers dependent reference updates in current draft and flags cross-file usages for confirmation.

---

## Screen 6: Artifact Preview and Ref Integrity

```text
+------------------------------------------------------------------------------------------------+
| Generated Artifact Preview                                                                       |
+------------------------------------------------------------------------------------------------+
| Step 8 of 9                                                                                     |
| Change Set: CS-2026-03-10-001                                                                    |
|                                                                                                |
| Tabs: [Bundle Summary] [Ref Integrity] [Generated YAML] [Validation] [Diff]                     |
|                                                                                                |
| Bundle Summary                                                                                   |
| - external-data-config: data-sources/customer-db.yaml                                            |
| - enrichment: enrichments/customer-tier-enrichment.yaml                                          |
|                                                                                                |
| Ref Integrity                                                                                    |
| [PASS] data-source-ref customer-database resolved -> data-sources/customer-db.yaml              |
| [PASS] query-ref getCustomerTier resolved in external-data-config                               |
| [PASS] all field mappings target existing output fields                                          |
| [WARN] scenario/component artifacts not yet included in this bundle                              |
|                                                                                                |
| Generated YAML (read-only until approval)                                                        |
| --- file: data-sources/customer-db.yaml ------------------------------------------------------  |
| metadata:                                                                                        |
|   id: customer-db-config                                                                         |
|   name: Customer Database                                                                        |
|   version: 1.0.0                                                                                 |
|   description: Customer master database connection                                               |
|   type: external-data-config                                                                     |
|   author: trading-team@company.com                                                               |
| data-sources: ...                                                                                |
| --- file: enrichments/customer-tier-enrichment.yaml -----------------------------------------  |
| metadata:                                                                                        |
|   id: customer-tier-enrichment                                                                   |
|   name: Customer Tier Enrichment                                                                 |
|   version: 1.0.0                                                                                 |
|   description: Lookup customer tier for settlement routing                                       |
|   type: enrichment                                                                               |
|   author: trading-team@company.com                                                               |
| enrichments: ...                                                                                 |
|                                                                                                |
| [ < Back ] [ Export Bundle ] [ Approve and Continue > ]                                          |
+------------------------------------------------------------------------------------------------+
```

Notes:
- Process dataset behavior: this screen validates end-to-end dataset assumptions (required inputs, type compatibility, and produced fields) across all generated artifacts in the change set.
- Reference behavior: this is the authoritative pre-write integrity gate; all refs are re-resolved against current catalog + draft change set snapshot.
- Approval freezes resolved symbol bindings for the change set to prevent drift between preview and file write.
- Any unresolved hard failures must be fixed before moving to composition/write.

---

## Screen 7: Compose into Component / Scenario / Registry

```text
+------------------------------------------------------------------------------------------------+
| Composition Builder                                                                              |
+------------------------------------------------------------------------------------------------+
| Step 9 of 9                                                                                     |
|                                                                                                |
| Create Component?         [x] yes                                                                |
| Component ID*             [ customer-settlement-component                                       ] |
|                                                                                                |
| Rule Configurations (config-files with execution order and failure policy)                       |
| +----+--------------------------------------------+-----------+------------------+---------+     |
| | #  | Config File                                 | Exec Order| Failure Policy   | Enabled |     |
| +----+--------------------------------------------+-----------+------------------+---------+     |
| | 1  | rules/basic-validation.yaml                 | 10        | terminate        | [x]     |     |
| | 2  | rules/compliance-rules.yaml                 | 20        | continue-with-warnings | [x] |     |
| +----+--------------------------------------------+-----------+------------------+---------+     |
| [ + Add Rule Config ]                                                                            |
|                                                                                                |
| Enrichment Refs (enrichment files with execution order)                                          |
| +----+--------------------------------------------+-----------+---------+                        |
| | #  | Enrichment File                             | Exec Order| Enabled |                        |
| +----+--------------------------------------------+-----------+---------+                        |
| | 1  | enrichments/customer-tier-enrichment.yaml    | 10        | [x]     |                        |
| | 2  | enrichments/settlement-instructions.yaml     | 20        | [x]     |                        |
| +----+--------------------------------------------+-----------+---------+                        |
| [ + Add Enrichment Ref ]                                                                         |
|                                                                                                |
| Component Refs (nested components — max depth 5)                                                 |
| +----+--------------------------------------------+-----------+---------+                        |
| | #  | Component File                              | Exec Order| Enabled |                        |
| +----+--------------------------------------------+-----------+---------+                        |
| | 1  | components/sub-validation-component.yaml     | 30        | [x]     |                        |
| +----+--------------------------------------------+-----------+---------+                        |
| [ + Add Component Ref ]                                                                          |
| Nesting Depth: 2 of 5 (OK)   [Levels 1–2: OK, 3–5: WARNING, 6+: ERROR]                        |
|                                                                                                |
| Create Scenario?          [x] yes                                                                |
| Scenario ID*              [ customer-settlement-scenario                                         ] |
| Classification Rule*      [ #tradeType == 'OTCOption' && #region == 'US' ]                      |
|                                                                                                |
| Stages (multi-stage scenario processing)                                                         |
| +----+---------------------+--------------------------------------------+-----------+-----------+|
| | #  | Stage Name           | Config File                                | Exec Order| Condition ||
| +----+---------------------+--------------------------------------------+-----------+-----------+|
| | 1  | validation           | components/customer-settlement-component.yaml | 10       | (always)  ||
| | 2  | enrichment           | enrichments/settlement-enrichment.yaml     | 20        | (always)  ||
| | 3  | compliance           | rules/compliance-rules.yaml                | 30        | #region=='US' ||
| +----+---------------------+--------------------------------------------+-----------+-----------+|
| [ + Add Stage ]                                                                                  |
| Failure Policy*            [ terminate v ]  (terminate | continue-with-warnings | flag-for-review) |
|                                                                                                |
| Add to Scenario Registry?  [x] yes                                                               |
| Registry File             [ scenario-registry/customer-registry.yaml ]                           |
|                                                                                                |
| Final Checks                                                                                     |
| [x] config-file refs resolve                                                                     |
| [x] component-refs depth <= 5                                                                    |
| [x] no circular dependency introduced                                                            |
| [x] stage execution-orders unique                                                                |
| [x] stage conditions are valid SpEL                                                              |
|                                                                                                |
| [ Run Full Validation ]                                 [ < Back ] [ Finish and Write Files ]   |
+------------------------------------------------------------------------------------------------+
```

Notes:
- Process dataset behavior: composition validates dataset contract compatibility across included rule/enrichment/component/scenario files so stage handoffs do not break at runtime.
- Reference behavior: `config-file`, `enrichment-ref`, and `component-ref` selectors are catalog-backed and type-filtered.
- Cross-file references are validated for existence, type compatibility, depth rules, and cycle safety before `Finish and Write Files`.
- New component/scenario/registry IDs become immediately discoverable in Screen 0 after write.

---

## Screen 8: Rule Builder

```text
+------------------------------------------------------------------------------------------------+
| Rule Builder                                                                                     |
+------------------------------------------------------------------------------------------------+
| Editing: rules/customer-validation.yaml                                                          |
|                                                                                                |
| Rules                                                                                            |
| [ + Add Rule ]                                                                                   |
| +----+-------------------------------+-----------+----------+------------+---------+---------+   |
| | #  | Rule ID                        | Severity  | Priority | Category   | Enabled | Status  |   |
| +----+-------------------------------+-----------+----------+------------+---------+---------+   |
| | 1  | required-fields-check          | ERROR     | 1        | validation | [x]     | VALID   |   |
| | 2  | amount-threshold               | WARNING   | 2        | risk       | [x]     | VALID   |   |
| | 3  | counterparty-active            | ERROR     | 3        | compliance | [x]     | VALID   |   |
| +----+-------------------------------+-----------+----------+------------+---------+---------+   |
|                                                                                                |
| Rule Detail (selected: required-fields-check)                                                    |
| Rule ID*                 [ required-fields-check                                               ] |
| Rule Name                [ Required Fields Validation                                          ] |
| Condition*               [ Basic Builder v ]  (Basic Builder | Advanced Expression)            |
| Condition Preview        [ Rule matches when tradeId, counterpartyName, and amount are present ] |
| Generated Expression     [ #tradeId != null && #counterpartyName != null && #amount != null    ] |
| Message*                 [ Trade ID, counterparty, and amount are required fields              ] |
| No-Match Message         [ All required fields present                                         ] |
| Severity*                [ ERROR v ]    (CRITICAL | ERROR | WARNING | INFO)                     |
| Priority                 [ 1 ]                                                                   |
| Result Field             [ requiredFieldsValid ]  (stores true/false in data map)               |
| Category                 [ validation v ]                                                        |
| Success Code             [ FIELDS_PRESENT ]                                                      |
| Error Code               [ MISSING_REQUIRED_FIELDS ]                                             |
| Enabled                  [x]                                                                     |
|                                                                                                |
| Validation                                                                                       |
| [x] SpEL syntax valid                                                                            |
| [x] Severity recognized                                                                          |
| [x] No duplicate rule IDs                                                                        |
| [x] condition test passed on sample input                                                        |
|                                                                                                |
| [ Validate Rules ]  [ Preview YAML ]                       [ < Back ] [ Save & Next > ]        |
+------------------------------------------------------------------------------------------------+
```

Notes:
- `result-field` stores the rule's boolean outcome (`true`/`false`) in the data map for downstream reference.
- `no-match-message` is displayed when the condition evaluates to `false` (complement of `message`).
- `success-code` / `error-code` enable business-level result tracking across rules and enrichments.
- Rules execute in document order by default; `priority` overrides when `processing-mode: "priority-order"`.
- Process dataset behavior: rule conditions read from the current process dataset snapshot, and `result-field` writes a deterministic boolean output for downstream consumers.
- Reference behavior: rule IDs defined here are the primary local symbol source for Screen 9 (`rule-ids` and structured `rule-references`).
- Rename/delete behavior: local group memberships are auto-updated in draft; cross-file refs are marked for explicit confirmation.

---

## Screen 9: Rule Group and Rule Refs Builder

```text
+------------------------------------------------------------------------------------------------+
| Rule Group Builder                                                                               |
+------------------------------------------------------------------------------------------------+
| Editing: rules/customer-validation.yaml                                                          |
|                                                                                                |
| Rule Groups                                                                                      |
| [ + Add Group ]                                                                                  |
| +----+-------------------------------+-----------+--------------------+-----------+---------+    |
| | #  | Group ID                       | Operator  | Members            | Behavior  | Debug   |    |
| +----+-------------------------------+-----------+--------------------+-----------+---------+    |
| | 1  | all-validations                | AND       | 3 rules            | stop-first| [ ]     |    |
| | 2  | any-risk-flag                  | OR        | 2 rules            | run-all   | [x]     |    |
| +----+-------------------------------+-----------+--------------------+-----------+---------+    |
|                                                                                                |
| Group Detail (selected: all-validations)                                                         |
| Group ID*                [ all-validations                                                     ] |
| Operator*                [ AND v ]  (AND | OR)                                                   |
| Stop on First Failure    [x]        (short-circuit when AND and first rule fails)               |
| Parallel Execution       [ ]        (execute rules concurrently)                                |
| Debug Mode               [ ]        (detailed execution logging)                                |
| Error Handling*          [ fail-fast v ]  (fail-fast | continue-on-error | skip-on-error)       |
|                                                                                                |
| Rule Members                                                                                     |
| +----+-------------------------------+-----------+---------+----------------------------------+ |
| | #  | Rule / Ref                     | Source     | Seq     | Override Priority                | |
| +----+-------------------------------+-----------+---------+----------------------------------+ |
| | 1  | required-fields-check          | local      | 1       | --                               | |
| | 2  | amount-threshold               | local      | 2       | --                               | |
| | 3  | counterparty-active            | local      | 3       | --                               | |
| +----+-------------------------------+-----------+---------+----------------------------------+ |
| [ + Add Rule ID ] [ + Add Rule Reference ] [ + Add Group Reference ]                           |
|                                                                                                |
| Member Modes:                                                                                    |
|  - Rule ID: references a rule in this file (rule-ids list)                                      |
|  - Rule Reference: structured ref with sequence, enabled, override-priority (rule-references)   |
|  - Group Reference: cross-file reference to another rule group (rule-group-references)          |
|                                                                                                |
| Cross-File Rule Refs (rule-refs section — imports from other YAML files)                        |
| +----+-------------------------------+--------------------------------------------+---------+   |
| | #  | Ref Name                       | Source File                                | Status  |   |
| +----+-------------------------------+--------------------------------------------+---------+   |
| | 1  | base-compliance-rules          | rules/compliance/base-rules.yaml           | RESOLVED|   |
| +----+-------------------------------+--------------------------------------------+---------+   |
| [ + Add Rule Ref ]                                                                               |
|                                                                                                |
| [ Validate Groups ]  [ Preview YAML ]                      [ < Back ] [ Save & Next > ]        |
+------------------------------------------------------------------------------------------------+
```

Notes:
- `rule-ids` is a simple list of rule ID strings for the group.
- `rule-references` is the structured form with `rule-id`, `sequence`, `enabled`, `override-priority`.
- `rule-group-references` imports rule groups defined in other YAML files.
- `rule-refs` is the top-level section for importing entire rule configurations from external files.
- Process dataset behavior: group execution does not introduce a new dataset schema; it orchestrates evaluation order and error semantics over rules that read/write the existing process dataset context.
- Reference behavior: `+ Add Rule ID` resolves only against local rules (Screen 8 in the same file), while `+ Add Rule Reference`/`rule-refs` resolve through the catalog and imported files.
- Resolution order: local symbols first, then explicitly imported `rule-refs`; ambiguous external matches require source-file disambiguation before save.

---

## Screen 10: Rule Chain Builder

```text
+------------------------------------------------------------------------------------------------+
| Rule Chain Builder                                                                               |
+------------------------------------------------------------------------------------------------+
| Editing: rules/settlement-chain.yaml                                                             |
|                                                                                                |
| Rule Chains                                                                                      |
| [ + Add Chain ]                                                                                  |
| +----+-------------------------------+-------------------------------+---------+                |
| | #  | Chain ID                       | Pattern                       | Rules   |                |
| +----+-------------------------------+-------------------------------+---------+                |
| | 1  | settlement-routing             | conditional-chaining           | 4       |                |
| | 2  | trade-accumulation             | accumulative-chaining          | 3       |                |
| +----+-------------------------------+-------------------------------+---------+                |
|                                                                                                |
| Chain Detail (selected: settlement-routing)                                                      |
| Chain ID*                [ settlement-routing                                                  ] |
| Pattern*                 [ conditional-chaining v ]                                              |
|                          (conditional-chaining | sequential-dependency | result-based-routing |  |
|                           accumulative-chaining | complex-workflow | fluent-builder)             |
|                                                                                                |
+-- Pattern Panel (swaps based on Pattern selection) -------------------------------------------+ |
|                                                                                                |
| === Conditional Chaining (active pattern) =================================================== | |
| Trigger Rule*            [ is-high-value-trade ]  (rule whose match/no-match drives routing)   |
| On Trigger (match):                                                                              |
| +----+-------------------------------+                                                          |
| | 1  | enhanced-validation            |                                                          |
| | 2  | senior-approval-check          |                                                          |
| +----+-------------------------------+                                                          |
| On No Trigger (no match):                                                                        |
| +----+-------------------------------+                                                          |
| | 1  | standard-validation            |                                                          |
| +----+-------------------------------+                                                          |
| [ + Add On-Trigger Rule ] [ + Add On-No-Trigger Rule ]                                         |
|                                                                                                |
| === Sequential Dependency (when selected) ==================================================== | |
| Stages:                                                                                          |
| +----+---------------------+-----------------------+-----------------------+                    |
| | #  | Stage Rule           | Output Variable       | Depends On            |                    |
| +----+---------------------+-----------------------+-----------------------+                    |
| | 1  | fetch-rate           | currentRate            | --                    |                    |
| | 2  | calculate-spread     | spreadResult           | fetch-rate            |                    |
| +----+---------------------+-----------------------+-----------------------+                    |
| [ + Add Stage ]                                                                                  |
|                                                                                                |
| === Result-Based Routing (when selected) ===================================================== | |
| Routing Rule*            [ determine-region ]                                                    |
| Routes:                                                                                          |
| +----+---------------------+-------------------------------+                                   |
| | Key| Route Name           | Rules                         |                                   |
| +----+---------------------+-------------------------------+                                   |
| | US | us-processing        | [us-compliance, us-tax]       |                                   |
| | EU | eu-processing        | [eu-mifid, eu-emir]           |                                   |
| +----+---------------------+-------------------------------+                                   |
| [ + Add Route ]                                                                                  |
|                                                                                                |
| === Accumulative Chaining (when selected) ==================================================== | |
| Accumulator (initial)*   [ 0 ]                                                                   |
| Accumulation Rules:                                                                              |
| +----+---------------------+---------+-------------------------------+                          |
| | #  | Rule ID              | Weight  | Condition                     |                          |
| +----+---------------------+---------+-------------------------------+                          |
| | 1  | credit-score-check   | 30      | #creditScore > 700            |                          |
| | 2  | income-check         | 40      | #annualIncome > 50000         |                          |
| | 3  | employment-check     | 30      | #employmentYears > 2          |                          |
| +----+---------------------+---------+-------------------------------+                          |
| Decision Rule*           [ final-approval ]  (evaluates accumulated score)                      |
| [ + Add Accumulation Rule ]                                                                      |
|                                                                                                |
| === Fluent Builder (when selected) ========================================================== | |
| Builder Target*          [ trade-enrichment ]                                                    |
| On Success Rules:        [ post-trade-notify, audit-log ]                                       |
| On Failure Rules:        [ error-handler, rollback-action ]                                     |
|                                                                                                |
+-- End Pattern Panel ---------------------------------------------------------------------- -- | |
|                                                                                                |
| [ Validate Chain ]  [ Preview YAML ]                       [ < Back ] [ Save & Next > ]        |
+------------------------------------------------------------------------------------------------+
```

Notes:
- `conditional-chaining`: Trigger rule drives binary routing (on-trigger vs on-no-trigger).
- `sequential-dependency`: Stages with output-variable and depends-on form a data pipeline.
- `result-based-routing`: Routing rule's result maps to named sets of rules (like a switch statement).
- `accumulative-chaining`: Weighted rules accumulate a score; decision rule evaluates the total.
- `complex-workflow`: Combination of the above patterns for multi-path workflows.
- `fluent-builder`: Builder pattern with on-success/on-failure callbacks.
- Process dataset behavior: chain patterns model dataset evolution step-by-step, where each selected rule/stage may consume fields produced by previous steps.
- Reference behavior: trigger/stage/route rule selectors resolve from local rules and imported rule refs, using the same ambiguity rules as Screen 9.
- Failure behavior: unresolved chain members block chain validation and prevent `Save & Next`.

---

## Screen 11: Enrichment Group Builder

```text
+------------------------------------------------------------------------------------------------+
| Enrichment Group Builder                                                                         |
+------------------------------------------------------------------------------------------------+
| Editing: enrichments/settlement-enrichment-groups.yaml                                           |
|                                                                                                |
| Enrichment Groups                                                                                |
| [ + Add Group ]                                                                                  |
| +----+-------------------------------+-----------+--------------------+-----------+              |
| | #  | Group ID                       | Operator  | Enrichments        | Behavior  |              |
| +----+-------------------------------+-----------+--------------------+-----------+              |
| | 1  | all-settlement-enrichments     | AND       | 3 enrichments      | stop-first|              |
| | 2  | any-pricing-source             | OR        | 2 enrichments      | run-all   |              |
| +----+-------------------------------+-----------+--------------------+-----------+              |
|                                                                                                |
| Group Detail (selected: all-settlement-enrichments)                                              |
| Group ID*                [ all-settlement-enrichments                                          ] |
| Operator*                [ AND v ]  (AND | OR)                                                   |
| Stop on First Failure    [x]                                                                     |
|                                                                                                |
| Enrichment Members                                                                               |
| +----+-------------------------------+-----------+---------+                                    |
| | #  | Enrichment ID                  | Source     | Seq     |                                    |
| +----+-------------------------------+-----------+---------+                                    |
| | 1  | customer-tier-lookup           | local      | 1       |                                    |
| | 2  | trade-value-calculation         | local      | 2       |                                    |
| | 3  | settlement-instructions         | local      | 3       |                                    |
| +----+-------------------------------+-----------+---------+                                    |
| [ + Add Enrichment ID ] [ + Add Enrichment Reference ] [ + Add Group Reference ]               |
|                                                                                                |
| Member Modes:                                                                                    |
|  - Enrichment ID: references an enrichment in this file (enrichment-ids list)                   |
|  - Enrichment Reference: structured ref with sequence, enabled (enrichment-references)          |
|  - Group Reference: cross-group reference (enrichment-group-references / enrichment-group)      |
|                                                                                                |
| [ Validate Groups ]  [ Preview YAML ]                      [ < Back ] [ Save & Next > ]        |
+------------------------------------------------------------------------------------------------+
```

Notes:
- Enrichment groups mirror rule groups: `AND` requires all to succeed, `OR` requires at least one.
- `stop-on-first-failure` provides short-circuit evaluation for AND groups.
- `enrichment-group-references` (plural) and `enrichment-group` (singular) both enable hierarchical composition.
- Process dataset behavior: group members execute against the shared process dataset and can apply cumulative field mutations based on member order and failure policy.
- Reference behavior: `+ Add Enrichment ID` resolves only local enrichment IDs (from Screen 5 in this file); reference modes resolve via catalog/imported symbols.
- Failure behavior: missing enrichment refs are non-runnable errors and block group validation/write.

---

## Screen 12: Transformation Builder

```text
+------------------------------------------------------------------------------------------------+
| Transformation Builder                                                                           |
+------------------------------------------------------------------------------------------------+
| Editing: transformations/trade-transformations.yaml                                              |
|                                                                                                |
| Transformations                                                                                  |
| [ + Add Transformation ]                                                                         |
| +----+-------------------------------+-------------------------------+---------+                |
| | #  | Transformation ID              | Type                          | Rules   |                |
| +----+-------------------------------+-------------------------------+---------+                |
| | 1  | priority-classification        | conditional-transformation     | 2       |                |
| | 2  | region-tagging                  | conditional-transformation     | 3       |                |
| +----+-------------------------------+-------------------------------+---------+                |
|                                                                                                |
| Transformation Detail (selected: priority-classification)                                        |
| Transformation ID*       [ priority-classification                                             ] |
| Type*                    [ conditional-transformation v ]                                        |
|                                                                                                |
| Transformation Rules                                                                             |
| +----+-------------------------------+----------------------------+----------------------------+ |
| | #  | Condition (SpEL)               | Actions True               | Actions False              | |
| +----+-------------------------------+----------------------------+----------------------------+ |
| | 1  | #root['amount'] > 10000        | set priority = "high"      | set priority = "normal"    | |
| | 2  | #root['region'] == 'US'        | set needsUSCompliance=true | (none)                     | |
| +----+-------------------------------+----------------------------+----------------------------+ |
| [ + Add Rule ]                                                                                   |
|                                                                                                |
| Rule Detail (selected: rule 1)                                                                   |
| Condition*               [ #root['amount'] > 10000                                             ] |
|                                                                                                |
| Actions True (actions-true — executes when condition is true)                                    |
| +----+---------------+-----------------------+-------------------------------+                  |
| | #  | Action Type    | Field                 | Value / Expression            |                  |
| +----+---------------+-----------------------+-------------------------------+                  |
| | 1  | set-field      | priority              | high                          |                  |
| +----+---------------+-----------------------+-------------------------------+                  |
| [ + Add Action ]                                                                                 |
|                                                                                                |
| Actions False (actions-false / else-actions — executes when condition is false)                  |
| +----+---------------+-----------------------+-------------------------------+                  |
| | #  | Action Type    | Field                 | Value / Expression            |                  |
| +----+---------------+-----------------------+-------------------------------+                  |
| | 1  | set-field      | priority              | normal                        |                  |
| +----+---------------+-----------------------+-------------------------------+                  |
| [ + Add Action ]                                                                                 |
|                                                                                                |
| Nesting: Actions can contain nested conditional-transformation for multi-level branching.       |
| [ + Nest Conditional Transformation ] (adds sub-transformation inside actions-true/false)       |
|                                                                                                |
| [ Validate Transformations ]  [ Preview YAML ]              [ < Back ] [ Save & Next > ]       |
+------------------------------------------------------------------------------------------------+
```

Notes:
- `actions-true` takes precedence over `actions` when both are specified.
- `actions-false` is an alias for `else-actions` (both work identically).
- Nested conditional-transformations enable multi-level branching (if/else-if/else pattern).
- Multiple sibling transformation-rules execute independently (all conditions evaluated).
- Process dataset behavior: transformations are explicit dataset rewrite steps; each action defines target-field mutations on the current snapshot.
- Reference behavior: transformation expressions may reference current data fields and prior rule/enrichment result fields from the same processing context; no new cross-file reference type is introduced here.

---

## Screen 13: Error Recovery Configuration

```text
+------------------------------------------------------------------------------------------------+
| Error Recovery Configuration                                                                     |
+------------------------------------------------------------------------------------------------+
| Editing: config/error-recovery.yaml                                                              |
|                                                                                                |
| Error Recovery             [x] Enabled                                                           |
| Log Recovery Attempts      [x]                                                                   |
| Default Strategy*          [ CONTINUE_WITH_DEFAULT v ]                                           |
|                            (FAIL_FAST | CONTINUE_WITH_DEFAULT |                                 |
|                             RETRY_WITH_SAFE_EXPRESSION | SKIP_RULE)                             |
|                                                                                                |
| Severity Policies                                                                                |
| +----------+-----------+-------------------------------+-----------+-----------+            |
| | Severity | Recovery  | Strategy                       | Max Retry | Delay(ms)|            |
| +----------+-----------+-------------------------------+-----------+-----------+            |
| | CRITICAL | [ ] off   | FAIL_FAST                      | --        | --        |            |
| | ERROR    | [ ] off   | FAIL_FAST                      | --        | --        |            |
| | WARNING  | [x] on    | CONTINUE_WITH_DEFAULT           | 1         | 100       |            |
| | INFO     | [x] on    | CONTINUE_WITH_DEFAULT           | 0         | --        |            |
| +----------+-----------+-------------------------------+-----------+-----------+            |
|                                                                                                |
| Policy Detail (selected: WARNING)                                                                |
| Severity                 WARNING                                                                 |
| Recovery Enabled*        [x]                                                                     |
| Strategy*                [ CONTINUE_WITH_DEFAULT v ]                                             |
| Max Retries              [ 1 ]                                                                   |
| Retry Delay (ms)         [ 100 ]                                                                 |
|                                                                                                |
| Strategy Reference:                                                                              |
|  FAIL_FAST                 — Immediately fail; no recovery attempted                             |
|  CONTINUE_WITH_DEFAULT     — Log error, continue with safe default values                       |
|  RETRY_WITH_SAFE_EXPRESSION — Retry with simplified expression                                  |
|  SKIP_RULE                 — Skip failed rule entirely, continue processing                     |
|                                                                                                |
| Concept: ResultType vs Severity                                                                  |
|  ResultType (system): MATCH, NO_MATCH, ERROR, ENRICHMENT_FAILURE                                |
|  Severity (business): CRITICAL, ERROR, WARNING, INFO                                            |
|  When condition=TRUE → ResultType=MATCH (severity irrelevant)                                   |
|  When condition=FALSE + ERROR severity + recovery disabled → ResultType=ERROR (fail-fast)       |
|                                                                                                |
| [ Validate Recovery Config ]  [ Preview YAML ]              [ < Back ] [ Save & Next > ]       |
+------------------------------------------------------------------------------------------------+
```

Notes:
- Error recovery is a cross-cutting concern applied to the entire YAML configuration.
- Severity policies override the default strategy per severity level.
- The ResultType vs Severity distinction is critical: ResultType is system-level (MATCH/NO_MATCH/ERROR), Severity is business-level classification.
- Process dataset behavior: recovery strategy determines how dataset state is handled after rule/enrichment failures (preserve current snapshot, apply defaults, retry, or skip mutation).
- Reference behavior: no explicit symbol refs are authored here; settings apply transitively to referenced rules, groups, chains, and enrichments at runtime.

### Screen 13 UI to YAML Mapping

| UI Field | YAML Key | Example |
|----------|----------|---------|
| Error Recovery Enabled | `error-recovery.enabled` | `true` |
| Log Recovery Attempts | `error-recovery.log-recovery-attempts` | `true` |
| Default Strategy | `error-recovery.default-strategy` | `"CONTINUE_WITH_DEFAULT"` |
| Severity (row key) | `error-recovery.severity-policies.<SEVERITY>` | `WARNING` |
| Recovery Enabled (policy) | `error-recovery.severity-policies.<SEVERITY>.recovery-enabled` | `true` |
| Strategy (policy) | `error-recovery.severity-policies.<SEVERITY>.strategy` | `"CONTINUE_WITH_DEFAULT"` |
| Max Retries (policy) | `error-recovery.severity-policies.<SEVERITY>.max-retries` | `1` |
| Retry Delay (ms) (policy) | `error-recovery.severity-policies.<SEVERITY>.retry-delay` | `100` |

---

## ETL Orchestration

This section covers pipeline authoring and sink configuration for end-to-end ETL flows.

### Screen 14: Pipeline Orchestration

```text
+------------------------------------------------------------------------------------------------+
| Pipeline / ETL Orchestration                                                                     |
+------------------------------------------------------------------------------------------------+
| Editing: pipelines/settlement-pipeline.yaml                                                      |
|                                                                                                |
| Pipeline Metadata                                                                                |
| Pipeline Name*           [ Settlement Data Pipeline                                            ] |
| Type*                    [ pipeline v ]  (pipeline | pipeline-config)                           |
| Description              [ Extract, transform, and load settlement data                        ] |
|                                                                                                |
| Pipeline Steps                                                                                   |
| +----+---------------------+-----------+-------------------+-------------------+                 |
| | #  | Step Name            | Type      | Depends On        | Operation          |                 |
| +----+---------------------+-----------+-------------------+-------------------+                 |
| | 1  | extract-trades       | extract   | --                | getAllTrades       |                 |
| | 2  | validate-data        | transform | extract-trades    | validateTrades     |                 |
| | 3  | enrich-settlement    | transform | validate-data     | enrichSettlement   |                 |
| | 4  | load-downstream      | load      | enrich-settlement | writeSettlement    |                 |
| | 5  | audit-trail          | audit     | load-downstream   | writeAuditTrail    |                 |
| +----+---------------------+-----------+-------------------+-------------------+                 |
| [ + Add Step ]                                                                                   |
|                                                                                                |
| Step Detail (selected: extract-trades)                                                           |
| Step Name*               [ extract-trades                                                      ] |
| Step Type*               [ extract v ]  (extract | transform | load | audit | read-schema | schema-diff) |
| Description              [ Extract raw trade data from source system                           ] |
| Depends On               [ -- v ]  (select predecessor step or -- for none)                     |
| Source                   [ settlement-input v ]   (required for extract)                         |
| Sink                     [ -- v ]                  (required for load)                            |
| Operation                [ getAllTrades ]                                                       |
| Condition (SpEL)         [ -- ]                                                                  |
| Optional                 [ ]  (if enabled, step failure does not stop pipeline)                  |
|                                                                                                |
| Step Retry                                                                                       |
| Max Attempts             [ 3 ]                                                                   |
| Delay (ms)               [ 1000 ]                                                                |
| Backoff Multiplier       [ 2.0 ]                                                                 |
| Max Delay (ms)           [ 30000 ]                                                               |
|                                                                                                |
| Execution Configuration                                                                          |
| Mode*                    [ sequential v ]  (sequential | parallel)                               |
| Error Handling           [ stop-on-error v ]  (stop-on-error | continue-on-error)               |
| Max Retries              [ 3 ]                                                                   |
| Retry Delay (ms)         [ 1000 ]                                                                |
| Log Progress             [x]                                                                     |
| Collect Metrics          [x]                                                                     |
|                                                                                                |
| Monitoring                                                                                       |
| Metrics Enabled          [x]                                                                     |
| Alert on Failure         [x]                                                                     |
| SLA (ms)                 [ 60000 ]                                                               |
|                                                                                                |
| Data Flow Preview (auto-generated from depends-on):                                             |
| extract-trades → validate-data → enrich-settlement → load-downstream → audit-trail             |
|                                                                                                |
| [ Validate Pipeline ]  [ Preview YAML ]                    [ < Back ] [ Save & Next > ]        |
+------------------------------------------------------------------------------------------------+
```

Notes:
- Pipeline steps declare explicit dependencies via `depends-on`.
- Step schema aligns with `name`, `type`, `source`/`sink`, `operation`, `depends-on`, `condition`, `optional`, and `retry`.
- Step types include `extract`, `transform`, `load`, `audit`, `read-schema`.
- Monitoring configuration enables SLA tracking and failure alerting per pipeline.
- The data flow preview is auto-generated from the step dependency graph.
- Process dataset behavior: each step declares dataset inputs/outputs so field availability can be validated along dependency edges before execution.
- Reference behavior: `source` and `sink` selectors resolve against catalog symbols from data-source/data-sink configs (Screens 2 and 15); missing symbols fail pipeline validation.

#### Screen 14 UI to YAML Mapping

| UI Field | YAML Key | Example |
|----------|----------|---------|
| Pipeline Name | `pipeline.name` | `"customer-etl-pipeline"` |
| Pipeline Description | `pipeline.description` | `"Extract, transform, and load"` |
| Step Name | `pipeline.steps[].name` | `"extract-trades"` |
| Step Type | `pipeline.steps[].type` | `"extract"` |
| Depends On | `pipeline.steps[].depends-on` | `["extract-trades"]` |
| Source | `pipeline.steps[].source` | `"settlement-input"` |
| Sink | `pipeline.steps[].sink` | `"settlement-output"` |
| Operation | `pipeline.steps[].operation` | `"getAllTrades"` |
| Condition (SpEL) | `pipeline.steps[].condition` | `"#region == 'US'"` |
| Optional | `pipeline.steps[].optional` | `false` |
| Retry Max Attempts | `pipeline.steps[].retry.max-attempts` | `3` |
| Retry Delay (ms) | `pipeline.steps[].retry.delay-ms` | `1000` |
| Retry Backoff Multiplier | `pipeline.steps[].retry.backoff-multiplier` | `2.0` |
| Retry Max Delay (ms) | `pipeline.steps[].retry.max-delay-ms` | `30000` |
| Execution Mode | `pipeline.execution.mode` | `"sequential"` |
| Execution Error Handling | `pipeline.execution.error-handling` | `"stop-on-error"` |
| Execution Max Retries | `pipeline.execution.max-retries` | `3` |
| Execution Retry Delay (ms) | `pipeline.execution.retry-delay-ms` | `1000` |
| Monitoring Log Progress | `pipeline.monitoring.log-progress` | `true` |
| Monitoring Collect Metrics | `pipeline.monitoring.collect-metrics` | `true` |
| Monitoring Alert on Failure | `pipeline.monitoring.alert-on-failure` | `true` |

---

### Screen 15: Data Sink Configuration

```text
+------------------------------------------------------------------------------------------------+
| Data Sink Configuration                                                                          |
+------------------------------------------------------------------------------------------------+
| Editing: config/data-sinks.yaml                                                                  |
|                                                                                                |
| Data Sinks                                                                                       |
| [ + Add Sink ]                                                                                   |
| +----+-------------------------------+-----------+-----------+---------+                        |
| | #  | Sink Name                      | Type      | Format    | Enabled |                        |
| +----+-------------------------------+-----------+-----------+---------+                        |
| | 1  | settlement-output              | database  | --        | [x]     |                        |
| | 2  | audit-file                     | file-system | CSV     | [x]     |                        |
| | 3  | notification-queue             | message-queue | JSON  | [x]     |                        |
| +----+-------------------------------+-----------+-----------+---------+                        |
|                                                                                                |
| Sink Detail (selected: settlement-output)                                                        |
| Sink Name*               [ settlement-output                                                   ] |
| Sink Type*               [ database v ]                                                          |
|                          (database | file-system | rest-api | cache | message-queue | custom)   |
|                                                                                                |
+-- Sink Type Panel (swaps based on Sink Type selection) --------------------------------------- | |
|                                                                                                |
| === Database Sink (active type) ============================================================= | |
| Connection*              [ settlement-db v ]  (from data-source-refs or embedded data-sources)  |
| Target Table / Query     [ INSERT INTO settlements (trade_id, amount, status) VALUES (?,?,?)   ] |
|                                                                                                |
| === File System Sink (when type = file-system) ============================================== | |
| File Path*               [ output/settlements/daily-settlements.csv                            ] |
| Output Format*           [ CSV v ]  (CSV | JSON | XML)                                          |
| Append Mode              [ ] overwrite   [x] append                                             |
|                                                                                                |
| === REST API Sink (when type = rest-api) ===================================================== | |
| Endpoint*                [ https://downstream.company.net/api/settlements                      ] |
| Method*                  [ POST v ]                                                              |
| Authentication           [ Bearer Token v ]  [ ************ ]                                   |
| Output Format            [ JSON ]                                                                |
|                                                                                                |
| === Cache Sink (when type = cache) ========================================================== | |
| Cache Backend*           [ Redis v ]                                                             |
| Host*                    [ cache-prod:6379 ]                                                     |
| Key Pattern*             [ settlement:{tradeId} ]                                                |
| TTL (seconds)            [ 3600 ]                                                                |
|                                                                                                |
| === Message Queue Sink (when type = message-queue) ========================================== | |
| Topic*                   [ settlement-events                                                   ] |
| Bootstrap Servers*       [ kafka-prod:9092                                                     ] |
| Output Format*           [ JSON v ]                                                              |
|                                                                                                |
| === Custom Sink (when type = custom) ======================================================== | |
| Implementation Class*    [ com.company.apex.CustomSink                                         ] |
| Custom Properties (kv)   [ endpoint=internal://sink;batchSize=100                              ] |
|                                                                                                |
+-- End Sink Type Panel -------------------------------------------------------------------- -- | |
|                                                                                                |
| [ Validate Sink ]  [ Preview YAML ]                        [ < Back ] [ Save & Next > ]        |
+------------------------------------------------------------------------------------------------+
```

Notes:
- Data sinks mirror data source types (database, file-system, rest-api, cache, message-queue, custom).
- Sinks are configured in the `data-sinks` YAML section, separate from data sources.
- The `output-format` keyword controls serialization format for non-database sinks.
- Process dataset behavior: sink mappings define final projection of process dataset fields to external outputs and are validated for missing required fields.
- Reference behavior: sink names defined here are exported to the catalog and become selectable in Screen 14 step `sink` fields.

---

## Screen 16: Category Management

```text
+------------------------------------------------------------------------------------------------+
| Category Management                                                                              |
+------------------------------------------------------------------------------------------------+
| Editing: config/categories.yaml                                                                  |
|                                                                                                |
| Categories                                                                                       |
| [ + Add Category ]                                                                               |
| +----+-------------------------------+--------------------+----------------------------+--------+|
| | #  | Name                           | Parent             | Tags                       | Rules  ||
| +----+-------------------------------+--------------------+----------------------------+--------+|
| | 1  | regulatory                     | --                 | compliance, governance     | 5      ||
| | 2  | mifid-ii                       | regulatory         | EU, trading                | 3      ||
| | 3  | dodd-frank                     | regulatory         | US, trading                | 2      ||
| | 4  | risk-management                | --                 | risk, assessment           | 4      ||
| +----+-------------------------------+--------------------+----------------------------+--------+|
|                                                                                                |
| Category Detail (selected: mifid-ii)                                                             |
| Category Name*           [ mifid-ii                                                            ] |
| Display Name             [ MiFID II Compliance                                                 ] |
| Parent Category          [ regulatory v ]  (creates hierarchy: regulatory > mifid-ii)           |
| Description              [ European MiFID II regulatory compliance rules                       ] |
| Priority                 [ 5 ]      (default 100; lower = higher priority)                      |
| Enabled                  [x]                                                                     |
|                                                                                                |
| Execution Settings                                                                               |
| Stop on First Failure    [ ]        (short-circuit when first rule in category fails)           |
| Parallel Execution       [ ]        (execute rules in this category concurrently)               |
|                                                                                                |
| Enterprise Governance                                                                            |
| Business Domain          [ Regulatory Compliance                                               ] |
| Business Owner           [ Chief Compliance Officer                                            ] |
| Created By               [ Compliance Team                                                     ] |
| Effective Date           [ 2025-01-01 ]                                                          |
| Expiration Date          [ 2025-12-31 ]                                                          |
|                                                                                                |
| Tags (string list for filtering and searching in catalog)                                       |
| +----+-------------------------------+                                                          |
| | #  | Tag                           |                                                          |
| +----+-------------------------------+                                                          |
| | 1  | EU                            |                                                          |
| | 2  | trading                       |                                                          |
| | 3  | mifid                         |                                                          |
| +----+-------------------------------+                                                          |
| [ + Add Tag ]                                                                                    |
|                                                                                                |
| Assigned Rules (rules with category: "mifid-ii")                                                |
| +----+-------------------------------+-------------------------------+                          |
| | #  | Rule ID                        | Config File                   |                          |
| +----+-------------------------------+-------------------------------+                          |
| | 1  | mifid-pre-trade-check          | rules/mifid-validation.yaml   |                          |
| | 2  | mifid-reporting-flag           | rules/mifid-reporting.yaml    |                          |
| | 3  | mifid-best-execution           | rules/mifid-execution.yaml    |                          |
| +----+-------------------------------+-------------------------------+                          |
|                                                                                                |
| Category Hierarchy Preview:                                                                      |
| regulatory                                                                                       |
| ├── mifid-ii (3 rules)                                                                          |
| └── dodd-frank (2 rules)                                                                        |
| risk-management (4 rules)                                                                        |
|                                                                                                |
| [ Validate Categories ]  [ Preview YAML ]                  [ < Back ] [ Save & Next > ]        |
+------------------------------------------------------------------------------------------------+
```

Notes:
- Categories use `parent-category` to create hierarchical trees for governance.
- `tags` is a simple string list used for filtering and searching in the catalog (Screen 0).
- Rules and enrichments reference categories via their `category` keyword.
- Enterprise governance fields (`business-domain`, `business-owner`, `created-by`, `effective-date`, `expiration-date`) enable lifecycle management.
- `priority` controls execution ordering; `stop-on-first-failure` and `parallel-execution` control rule execution within the category.
- The hierarchy preview is auto-generated from parent-category relationships.
- Process dataset behavior: categories do not change dataset shape directly, but they govern which dataset-affecting rules/enrichments run and in what priority context.
- Reference behavior: category names authored here are catalog symbols consumed by category selectors in rule/enrichment authoring; renames require dependent assignment updates.

---

## Interaction Notes (Revised)

```text
- Forward navigation is blocked only on hard failures; warnings are visible but non-blocking.
- Every ref selector is catalog-backed with status: RESOLVED / MISSING / AMBIGUOUS / TYPE-MISMATCH.
- SpEL fields validate syntax on blur and show normalized expression hints.
- Query parameter binding is explicit (query param name -> value source SpEL).
- Generated YAML remains read-only until explicit approval at Screen 6.
- Composition step supports multi-file generation across component/scenario/registry types.
- Screens 8–16 are standalone; they share the catalog (Screen 0) and preview (Screen 6).
- Source type / enrichment type / chain pattern panels swap dynamically based on selector.
- Rule chain patterns show only the relevant configuration panel for the selected pattern.
- Error recovery and categories are cross-cutting — changes propagate to dependent configs.
- Process dataset lifecycle is cross-cutting — every screen must declare dataset reads/writes and participate in pre-write dataset contract validation.
```

## Mobile Notes (Revised)

```text
- Keep a compact stepper with "Resolver Status" badge always visible.
- Move tables into stacked cards with expandable detail rows.
- Keep "Resolve Refs" and "Validate" as sticky bottom CTAs.
- Collapse Generated YAML by default; expose copy/export actions in overflow menu.
- Standalone screens (8–16) use bottom tab navigation from the catalog.
- Transformation nesting and rule chain patterns collapse to summary cards on mobile.
```

---

## Appendix B: Missing apex-api Endpoints for YAML Authoring Screens

Scope: This appendix covers only YAML authoring requirements from Screens 0-16. Runtime-only operations are intentionally excluded.

### 1. Catalog and Discovery

| Endpoint | Method | Required By | Purpose |
|----------|--------|-------------|---------|
| `/api/catalog/workspaces` | GET | Screen 0 | List registered scan roots/workspaces available to the authoring UI. |
| `/api/catalog/workspaces` | POST | Screen 0 | Register a new scan root/workspace (name, root path, include/exclude patterns, environment). |
| `/api/catalog/workspaces/{workspaceId}` | PATCH | Screen 0 | Update workspace root and scan options without redeploying service. |
| `/api/catalog/workspaces/{workspaceId}` | DELETE | Screen 0 | Remove a workspace registration from catalog scanning. |
| `/api/catalog/configurations` | GET | Screen 0 | List filterable YAML artifacts across workspace (type, owner, domain, health, refs). |
| `/api/catalog/scan` | POST | Screen 0 | Trigger full or incremental workspace scan for catalog refresh. |
| `/api/catalog/resolver-queue` | GET | Screen 0 | Return unresolved, ambiguous, and type-mismatch references for resolver queue. |
| `/api/catalog/dependency-graph` | GET | Screen 0 | Provide graph model for cross-file dependency visualization. |
| `/api/catalog/snapshot` | GET | Screen 0 | Export point-in-time catalog snapshot. |

Catalog workspace scoping rules:
- All catalog read/scan endpoints should be workspace-scoped.
- Workspace context must be explicit on every request by either:
  - path parameter where applicable, or
  - `workspaceId` query parameter, or
  - `X-APEX-Workspace-Id` request header.
- If no workspace context is provided, return `400 Bad Request`.
- Do not rely on implicit active-workspace state for production APIs.
- For service-to-service/background automation, always send explicit `workspaceId`.

Optional UX convenience (not recommended as required API contract):
- `POST /api/catalog/workspaces/{workspaceId}/activate` can exist as a UI-only default selector.
- If implemented, it must be caller-local scope only and never global state.

### 2. Bundle Authoring Lifecycle

| Endpoint | Method | Required By | Purpose |
|----------|--------|-------------|---------|
| `/api/authoring/bundles` | POST | Screen 1 | Create draft bundle from selected artifacts and metadata intent. |
| `/api/authoring/bundles/{bundleId}` | PATCH | Screens 1-7 | Save incremental edits while moving through wizard steps. |
| `/api/authoring/bundles/{bundleId}/validate-plan` | POST | Screen 1 | Validate plan before generation (naming, refs, required metadata). |
| `/api/authoring/bundles/{bundleId}/render` | POST | Screens 2-16 | Render generated YAML preview from current UI state without writing files. |
| `/api/authoring/bundles/{bundleId}/diff` | GET | Screen 6 | Show generated change diff before approval. |
| `/api/authoring/bundles/{bundleId}/approve` | POST | Screen 6 | Record approval gate for generated artifacts. |
| `/api/authoring/bundles/{bundleId}/export` | GET | Screen 6 | Export full bundle payload for review or handoff. |
| `/api/authoring/bundles/{bundleId}/write-files` | POST | Screen 7 | Persist approved generated artifacts to target files. |

### 3. Reference Resolution and Integrity

| Endpoint | Method | Required By | Purpose |
|----------|--------|-------------|---------|
| `/api/refs/resolve` | POST | Screens 0, 4, 5 | Resolve `data-source-ref`, `query-ref`, `rule-ref`, and related cross-file refs. |
| `/api/refs/validate-integrity` | POST | Screen 6 | Validate full ref integrity across bundle and referenced files. |
| `/api/refs/suggest` | POST | Screens 0, 4 | Suggest likely ref targets for missing or ambiguous references. |

### 4. Data Source Authoring Gaps

| Endpoint | Method | Required By | Purpose |
|----------|--------|-------------|---------|
| `/api/datasources` | POST | Screen 2A | Create new external data source definitions via authoring workflow. |
| `/api/datasources/{name}` | PUT | Screen 2A | Update existing external data source definitions. |
| `/api/datasources/{name}` | DELETE | Screen 2A | Remove external data source definitions from authoring set. |
| `/api/datasources/{name}/discover-schema` | POST | Screen 2A | Discover source schema for field mapping and dataset contract scaffolding. |
| `/api/datasources/{name}/named-queries` | GET | Screen 4 | Retrieve named query list for `query-ref` selection. |

### 5. Composition Authoring Gaps

| Endpoint | Method | Required By | Purpose |
|----------|--------|-------------|---------|
| `/api/authoring/compose/component` | POST | Screen 7 | Build component config from selected rule configs, enrichment refs, and component refs. |
| `/api/authoring/compose/scenario` | POST | Screen 7 | Build scenario config with classification, stages, and failure policy. |
| `/api/authoring/compose/scenario-registry` | POST | Screen 7 | Add or update scenario entries in a registry document. |
| `/api/authoring/compose/validate` | POST | Screen 7 | Validate cycles, depth limits, stage order uniqueness, and ref consistency. |

### Existing Endpoints Already Covering Part of Authoring

- Configuration load/upload/validate is already available under `/api/config`.
- Dependency analysis primitives are already available under `/api/dependencies`.
- Expression validation/evaluation is already available under `/api/expressions`.
- Basic data source read/test/lookup is already available under `/api/datasources`.

Note: This appendix intentionally does not include runtime orchestration endpoints unrelated to YAML authoring UI flows.
