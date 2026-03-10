# APEX Configuration UI Mockups (ASCII, Revised)

Purpose: Implementation-ready ASCII mockups for a ref-aware APEX configuration UI.
Scope: `Catalog -> Connections -> Dataset -> Lookup -> Enrichment -> Artifact Preview -> Compose`.

Design goals:
- Eliminate ref ambiguity with a central catalog and resolver.
- Match real APEX YAML vocabulary and composition patterns.
- Cover more than single-file enrichment flow (components/scenarios/registries).

---

## APEX Type Coverage Matrix

```text
+-----------------------------------------------------------------------------------------------+
| APEX Config Type            | Create | Edit | Validate | Ref Resolve | Preview YAML | Compose |
+----------------------------+--------+------+----------+-------------+--------------+---------+
| rule-config                |   Y    |  Y   |    Y     |      Y      |      Y       |    Y    |
| enrichment                 |   Y    |  Y   |    Y     |      Y      |      Y       |    Y    |
| external-data-config       |   Y    |  Y   |    Y     |      Y      |      Y       |    Y    |
| component                  |   Y    |  Y   |    Y     |      Y      |      Y       |    Y    |
| scenario                   |   Y    |  Y   |    Y     |      Y      |      Y       |    Y    |
| scenario-registry          |   Y    |  Y   |    Y     |      Y      |      Y       |    Y    |
+----------------------------+--------+------+----------+-------------+--------------+---------+
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
| [x] external-data-config   file: data-sources/customer-db.yaml                                  |
| [x] enrichment             file: enrichments/customer-tier-enrichment.yaml                      |
| [ ] rule-config            file: rules/customer-validation.yaml                                 |
| [ ] component              file: components/customer-settlement-component.yaml                  |
| [ ] scenario               file: scenarios/customer-settlement-scenario.yaml                    |
| [ ] scenario-registry      file: scenario-registry/customer-registry.yaml                       |
|                                                                                                |
| Resolver Policy*           [ Fail on missing refs v ]                                           |
| Naming Convention*         [ kebab-case ids, snake_case db fields v ]                           |
|                                                                                                |
| [ Validate Plan ]                                         [ Cancel ] [ Save & Next > ]         |
+------------------------------------------------------------------------------------------------+
```

---

## Screen 2: Connection and External Data Config

```text
+------------------------------------------------------------------------------------------------+
| External Data Source Builder                                                                    |
+------------------------------------------------------------------------------------------------+
| Step 2 of 7                                                                                     |
| Artifact: external-data-config -> data-sources/customer-db.yaml                                 |
|                                                                                                |
| Data Source Name*          [ customer-database                                                 ] |
| Source Type*               [ database v ]                                                       |
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
| +----+---------------------+------------------------------------------------------------------+ |
| | #  | Query Name           | SQL                                                              | |
| +----+---------------------+------------------------------------------------------------------+ |
| | 1  | getCustomerTier      | SELECT tier FROM customer_profile_vw WHERE customer_id=:id      | |
| | 2  | getCustomerProfile   | SELECT customer_name,tier,status FROM customer_profile_vw ...   | |
| +----+---------------------+------------------------------------------------------------------+ |
|                                                                                                |
| Validation                                                                                      |
| [x] Connection reachable                                                                         |
| [x] Schemas discovered                                                                           |
| [x] Query names unique                                                                           |
|                                                                                                |
| [ Test Connection ] [ Discover Schema ]                    [ < Back ] [ Save & Next > ]       |
+------------------------------------------------------------------------------------------------+
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
| External Query Ref Mode (active)                                                                 |
| Data Source Ref*           [ customer-database v ]                                               |
| Query Ref*                 [ getCustomerTier v ]                                                 |
| Parameters                                                                                      |
| +----+------------------+--------------+-------------------------------+                        |
| | #  | Query Param Name | Type         | Value Source (SpEL)           |                        |
| +----+------------------+--------------+-------------------------------+                        |
| | 1  | id               | string       | #customerId                    |                        |
| +----+------------------+--------------+-------------------------------+                        |
|                                                                                                |
| Field Mappings                                                                                  |
| +----+---------------------+-----------------------+ Required + Default ---------------------+ |
| | #  | Source Field         | Target Field          |   [x]    | [ ]                          | |
| +----+---------------------+-----------------------+----------+------------------------------+ |
| | 1  | tier                 | customerTier          |   [x]    |                              | |
| +----+---------------------+-----------------------+----------+------------------------------+ |
|                                                                                                |
| Binding Preview (generated)                                                                      |
| lookup-config.lookup-key = "#customerId"                                                       |
| lookup-config.lookup-dataset.data-source-ref = "customer-database"                            |
| lookup-config.lookup-dataset.query-ref = "getCustomerTier"                                     |
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
| Lookup Mapping: get-customer-tier                                                                |
|                                                                                                |
| Enrichment Name*            [ Populate Settlement Instruction                                  ] |
| Enrichment Type*            [ lookup-enrichment v ]                                              |
| Apply When (SpEL)           [ #productType == 'IRS' || #productType == 'FX_OPTION' ]            |
| Missing Data Behavior*      [ Leave existing value v ]                                           |
| Execution Priority          [ 100 ]                                                               |
|                                                                                                |
| Output Mappings                                                                                  |
| +----+---------------------+----------------------------+ Mode    + Condition (optional) -----+ |
| | #  | Lookup Return Field  | Target Output Field       | set     |                            | |
| +----+---------------------+----------------------------+---------+----------------------------+ |
| | 1  | customerTier         | settlementInstruction      | set     | #customerTier != null      | |
| +----+---------------------+----------------------------+---------+----------------------------+ |
|                                                                                                |
| Validation                                                                                       |
| [x] lookup-config complete                                                                       |
| [x] target fields valid                                                                          |
| [x] SpEL syntax valid                                                                            |
| [x] referenced query parameters bound                                                            |
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
| metadata: { id: customer-db-config, type: external-data-config, ... }                            |
| data-sources: ...                                                                                |
| --- file: enrichments/customer-tier-enrichment.yaml -----------------------------------------  |
| metadata: { id: customer-tier-enrichment, type: enrichment, ... }                                |
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
| rule-configurations       [ + Add v ]                                                            |
| enrichment-refs           [ enrichments/customer-tier-enrichment.yaml ]                          |
| component-refs            [ none ]                                                               |
|                                                                                                |
| Create Scenario?          [x] yes                                                                |
| Scenario ID*              [ customer-settlement-scenario                                         ] |
| Classification Rule*      [ #tradeType == 'OTCOption' && #region == 'US' ]                      |
| Stage 1 config-file       [ components/customer-settlement-component.yaml ]                      |
| Failure Policy            [ terminate v ]                                                        |
|                                                                                                |
| Add to Scenario Registry?  [x] yes                                                               |
| Registry File             [ scenario-registry/customer-registry.yaml ]                           |
|                                                                                                |
| Final Checks                                                                                     |
| [x] config-file refs resolve                                                                     |
| [x] component-refs depth <= 5                                                                    |
| [x] no circular dependency introduced                                                            |
|                                                                                                |
| [ Run Full Validation ]                                 [ < Back ] [ Finish and Write Files ]   |
+------------------------------------------------------------------------------------------------+
```

---

## Interaction Notes (Revised)

```text
- Forward navigation is blocked only on hard failures; warnings are visible but non-blocking.
- Every ref selector is catalog-backed with status: RESOLVED / MISSING / AMBIGUOUS / TYPE-MISMATCH.
- SpEL fields validate syntax on blur and show normalized expression hints.
- Query parameter binding is explicit (query param name -> value source SpEL).
- Generated YAML remains read-only until explicit approval at Screen 6.
- Composition step supports multi-file generation across component/scenario/registry types.
```

## Mobile Notes (Revised)

```text
- Keep a compact stepper with "Resolver Status" badge always visible.
- Move tables into stacked cards with expandable detail rows.
- Keep "Resolve Refs" and "Validate" as sticky bottom CTAs.
- Collapse Generated YAML by default; expose copy/export actions in overflow menu.
```
