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
- Pipeline / ETL orchestration — steps, dependencies, monitoring (Screen 14)
- Data sink configuration — all 6 sink types (Screen 15)
- Category management — hierarchy, labels, governance metadata (Screen 16)

### Screen Navigation

Screens 0–7 form a linear wizard for creating lookup enrichment configurations.
Screens 8–16 are standalone authoring screens accessible from the Catalog (Screen 0).
All screens share the Artifact Preview (Screen 6) for validation and YAML generation.

```text
Screen 0 (Catalog)
├── Wizard Flow: 1 → 2 → 3 → 4 → 5 → 6 → 7
├── Rule Authoring: 8 → 9 → 10 → 6
├── Enrichment Groups: 11 → 6
├── Transformations: 12 → 6
├── Error Recovery: 13 → 6
├── Pipeline / ETL: 14 → 6
├── Data Sinks: 15 → 6
└── Categories: 16 → 6
```

---

## APEX Type Coverage Matrix

```text
+------------------------------------------------------------------------------------------------------------------+
| APEX Config Type             | Create | Edit | Validate | Ref Resolve | Preview YAML | Compose | Screens        |
+-----------------------------+--------+------+----------+-------------+--------------+---------+----------------+
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
| data-sinks                  |   Y    |  Y   |    Y     |      Y      |      Y       |    Y    | 15, 6          |
| categories                  |   Y    |  Y   |    Y     |      Y      |      Y       |    Y    | 16             |
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
| All types also support: tags, created, last-modified, processing-mode, environment,             |
| business-domain, business-owner, display-name, documentation-url, criticality, sla-ms          |
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

---

## Screen 1: Configuration Intent and File Targets

```text
+------------------------------------------------------------------------------------------------+
| New Configuration Bundle                                                                        |
+------------------------------------------------------------------------------------------------+
| Step 1 of 7                                                                                     |
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
| Step 2 of 7                                                                                     |
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
| Step 2 of 7                                                                                     |
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

---

## Screen 3: Dataset Contract Builder

```text
+------------------------------------------------------------------------------------------------+
| Dataset Contract Builder                                                                         |
+------------------------------------------------------------------------------------------------+
| Step 3 of 7                                                                                     |
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

---

## Screen 4: Lookup Mapping Builder (Ref-Aware)

```text
+------------------------------------------------------------------------------------------------+
| Lookup Mapping Builder                                                                           |
+------------------------------------------------------------------------------------------------+
| Step 4 of 7                                                                                     |
|                                                                                                |
| Mapping ID*                [ get-customer-tier                                                 ] |
| Lookup Enrichment ID*      [ customer-tier-lookup                                               ] |
| Condition (SpEL)*          [ #customerId != null ]                                               |
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
| [ Resolve Refs ] [ Validate Mapping ]                       [ < Back ] [ Save & Next > ]       |
+------------------------------------------------------------------------------------------------+
```

---

## Screen 5: Enrichment Builder

```text
+------------------------------------------------------------------------------------------------+
| Enrichment Builder                                                                               |
+------------------------------------------------------------------------------------------------+
| Step 5 of 7                                                                                     |
|                                                                                                |
| Enrichment ID*             [ customer-tier-lookup                                              ] |
| Enrichment Type*           [ lookup-enrichment v ]                                               |
|                            (lookup-enrichment | calculation-enrichment |                         |
|                             field-enrichment | conditional-mapping-enrichment)                   |
| Condition (SpEL)           [ #customerId != null ]                                               |
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
|                                                                                                |
| [ Validate Enrichment ]                                  [ < Back ] [ Save & Next > ]          |
+------------------------------------------------------------------------------------------------+
```

---

## Screen 6: Artifact Preview and Ref Integrity

```text
+------------------------------------------------------------------------------------------------+
| Generated Artifact Preview                                                                       |
+------------------------------------------------------------------------------------------------+
| Step 6 of 7                                                                                     |
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

---

## Screen 7: Compose into Component / Scenario / Registry

```text
+------------------------------------------------------------------------------------------------+
| Composition Builder                                                                              |
+------------------------------------------------------------------------------------------------+
| Step 7 of 7                                                                                     |
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
| Condition (SpEL)*        [ #tradeId != null && #counterpartyName != null && #amount != null    ] |
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
|                                                                                                |
| [ Validate Rules ]  [ Preview YAML ]                       [ < Back ] [ Save & Next > ]        |
+------------------------------------------------------------------------------------------------+
```

Notes:
- `result-field` stores the rule's boolean outcome (`true`/`false`) in the data map for downstream reference.
- `no-match-message` is displayed when the condition evaluates to `false` (complement of `message`).
- `success-code` / `error-code` enable business-level result tracking across rules and enrichments.
- Rules execute in document order by default; `priority` overrides when `processing-mode: "priority-order"`.

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
| +----------+-----------+-------------------------------+-----------+-----------+-----------+     |
| | Severity | Recovery  | Strategy                       | Max Retry | Delay(ms)| Backoff   |     |
| +----------+-----------+-------------------------------+-----------+-----------+-----------+     |
| | CRITICAL | [ ] off   | FAIL_FAST                      | --        | --        | --        |     |
| | ERROR    | [ ] off   | FAIL_FAST                      | --        | --        | --        |     |
| | WARNING  | [x] on    | CONTINUE_WITH_DEFAULT           | 1         | 100       | --        |     |
| | INFO     | [x] on    | CONTINUE_WITH_DEFAULT           | 0         | --        | --        |     |
| +----------+-----------+-------------------------------+-----------+-----------+-----------+     |
|                                                                                                |
| Policy Detail (selected: WARNING)                                                                |
| Severity                 WARNING                                                                 |
| Recovery Enabled*        [x]                                                                     |
| Strategy*                [ CONTINUE_WITH_DEFAULT v ]                                             |
| Max Retries              [ 1 ]                                                                   |
| Retry Delay (ms)         [ 100 ]                                                                 |
| Backoff Multiplier       [ -- ]  (for exponential backoff; blank = no backoff)                   |
| Max Delay (ms)           [ -- ]                                                                  |
| Fallback Value           [ null ]  (value used when CONTINUE_WITH_DEFAULT applies)              |
|                                                                                                |
| Strategy Reference:                                                                              |
|  FAIL_FAST                 — Immediately fail; no recovery attempted                             |
|  CONTINUE_WITH_DEFAULT     — Log error, continue with safe default values                       |
|  RETRY_WITH_SAFE_EXPRESSION — Retry with simplified expression                                  |
|  SKIP_RULE                 — Skip failed rule entirely, continue processing                     |
|                                                                                                |
| Environment Overrides                                                                            |
| +----------+-------------------------------+-----------------------------------------+           |
| | Env       | Override                       | Value                                   |           |
| +----------+-------------------------------+-----------------------------------------+           |
| | dev       | default-strategy               | CONTINUE_WITH_DEFAULT                    |           |
| | prod      | default-strategy               | FAIL_FAST                                |           |
| +----------+-------------------------------+-----------------------------------------+           |
| [ + Add Override ]                                                                               |
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
- Environment overrides allow different error handling behavior per deployment environment.
- The ResultType vs Severity distinction is critical: ResultType is system-level (MATCH/NO_MATCH/ERROR), Severity is business-level classification.

---

## Screen 14: Pipeline / ETL Orchestration

```text
+------------------------------------------------------------------------------------------------+
| Pipeline / ETL Orchestration                                                                     |
+------------------------------------------------------------------------------------------------+
| Editing: pipelines/settlement-pipeline.yaml                                                      |
|                                                                                                |
| Pipeline Metadata                                                                                |
| Pipeline Name*           [ Settlement Data Pipeline                                            ] |
| Type*                    [ pipeline-config ]                                                     |
| Description              [ Extract, transform, and load settlement data                        ] |
|                                                                                                |
| Pipeline Steps                                                                                   |
| +----+---------------------+-----------+-------------------+-------------------+---------+      |
| | #  | Step ID              | Type      | Depends On        | Execution Order   | Enabled |      |
| +----+---------------------+-----------+-------------------+-------------------+---------+      |
| | 1  | extract-trades       | extract   | --                | 1                 | [x]     |      |
| | 2  | validate-data        | transform | extract-trades    | 2                 | [x]     |      |
| | 3  | enrich-settlement    | transform | validate-data     | 3                 | [x]     |      |
| | 4  | load-downstream      | load      | enrich-settlement | 4                 | [x]     |      |
| | 5  | audit-trail          | audit     | load-downstream   | 5                 | [x]     |      |
| +----+---------------------+-----------+-------------------+-------------------+---------+      |
| [ + Add Step ]                                                                                   |
|                                                                                                |
| Step Detail (selected: extract-trades)                                                           |
| Step ID*                 [ extract-trades                                                      ] |
| Step Type*               [ extract v ]  (extract | transform | load | audit | read-schema | schema-diff) |
| Description              [ Extract raw trade data from source system                           ] |
| Depends On               [ -- v ]  (select predecessor step or -- for none)                     |
| Config File              [ config/extract/trade-extract.yaml ]                                   |
| Execution Order          [ 1 ]                                                                   |
| Enabled                  [x]                                                                     |
|                                                                                                |
| Execution Configuration                                                                          |
| Mode*                    [ sequential v ]  (sequential | parallel)                               |
| Timeout (ms)             [ 30000 ]                                                               |
| Retry Count              [ 2 ]                                                                   |
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
- Pipeline steps declare explicit dependencies via `depends-on` for execution ordering.
- Step types map to YAML sections: `extract` (read-schema), `transform`, `load`, `audit`, plus `read-schema` and `schema-diff` for data-sync pipelines.
- Monitoring configuration enables SLA tracking and failure alerting per pipeline.
- The data flow preview is auto-generated from the step dependency graph.

---

## Screen 15: Data Sink Configuration

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
