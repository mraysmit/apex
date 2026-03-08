# APEX Basics Screen Mockups

Purpose: Screen-level mockups for the basics-first flow only.
Scope: `Database Connection -> Dataset -> Lookup Mapping -> Enrichment -> Preview`.

## Screen 1: Connections Dashboard

```text
+--------------------------------------------------------------------------------------+
| APEX Authoring                                                                        |
| Basics Wizard                                                                         |
+--------------------------------------------------------------------------------------+
| Breadcrumb: Home > Data Setup > Connections                                           |
|                                                                                      |
| [ + New Connection ]   [ Import ]   [ Refresh ]                                      |
|                                                                                      |
| Filters: [ Type: Any v ] [ Environment: Any v ] [ Status: Any v ] [ Search..... ]   |
|                                                                                      |
| +----+----------------------+-----------+-------------+----------+----------------+  |
| | ID | Name                 | Type      | Environment | Status   | Last Tested    |  |
| +----+----------------------+-----------+-------------+----------+----------------+  |
| | 01 | Customer DB Prod     | Postgres  | Production  | PASS     | 2026-03-07     |  |
| | 02 | Customer DB Staging  | Postgres  | Staging     | PASS     | 2026-03-06     |  |
| | 03 | Product DB Prod      | SQLServer | Production  | FAIL     | 2026-03-05     |  |
| +----+----------------------+-----------+-------------+----------+----------------+  |
|                                                                                      |
| Selected Connection Details                                                           |
| Name: Customer DB Prod                                                                 |
| Host: db-prod.company.net:5432                                                        |
| Database: customer_master                                                              |
| Permissions: Read Only                                                                 |
| [ Test Connection ] [ Edit ] [ Continue > ]                                            |
+--------------------------------------------------------------------------------------+
```

## Screen 2: New Connection Form

```text
+--------------------------------------------------------------------------------------+
| New Connection                                                                        |
+--------------------------------------------------------------------------------------+
| Step 1 of 4                                                                           |
|                                                                                      |
| Connection Name*        [ Customer DB Prod                                         ] |
| Database Type*          [ PostgreSQL v ]                                               |
| Host*                   [ db-prod.company.net                                      ] |
| Port*                   [ 5432 ]                                                       |
| Database*               [ customer_master                                            ] |
| Username*               [ customer_reader                                            ] |
| Password*               [ *************** ]                                            |
| SSL Mode*               [ require v ]                                                  |
| Timeout (ms)           [ 5000 ]                                                        |
|                                                                                      |
| Validation                                                                              |
| [ ] Required fields complete                                                           |
| [ ] Credentials valid                                                                   |
| [ ] Reachable from runtime environment                                                 |
|                                                                                      |
| [ Test Connection ]                                      [ Cancel ] [ Save & Next > ] |
+--------------------------------------------------------------------------------------+
```

## Screen 3: Dataset Builder

```text
+--------------------------------------------------------------------------------------+
| Dataset Builder                                                                       |
+--------------------------------------------------------------------------------------+
| Step 2 of 4                                                                           |
| Connection: Customer DB Prod                                                           |
|                                                                                      |
| Dataset Label*          [ Customer Profile                                          ] |
| Source Object*          [ customer_profile_vw v ]                                      |
| Key Field*              [ customer_id v ]                                              |
| Active Field            [ status v ]                                                   |
| Active Value            [ ACTIVE ]                                                     |
|                                                                                      |
| Discover Fields [ Run ]                                                                |
|                                                                                      |
| Field Dictionary                                                                        |
| +----+-----------------------+----------------------+------------+ Include +---------+ |
| | #  | Business Field        | Physical Field       | Data Type  |    [x]  | Search  | |
| +----+-----------------------+----------------------+------------+---------+---------+ |
| | 1  | Customer ID           | customer_id          | text       |   [x]   |         | |
| | 2  | Customer Tier         | tier                 | text       |   [x]   |         | |
| | 3  | Region                | region_code          | text       |   [x]   |         | |
| | 4  | KYC Status            | kyc_status           | text       |   [x]   |         | |
| +----+-----------------------+----------------------+------------+---------+---------+ |
|                                                                                      |
| [ Validate Dataset ]                                 [ < Back ] [ Save & Next > ]     |
+--------------------------------------------------------------------------------------+
```

## Screen 4: Lookup Mapping Builder

```text
+--------------------------------------------------------------------------------------+
| Lookup Mapping Builder                                                                 |
+--------------------------------------------------------------------------------------+
| Step 3 of 4                                                                           |
| Dataset: Customer Profile                                                              |
|                                                                                      |
| Mapping Name*               [ Get Customer Tier                                     ] |
| Lookup Mode*                ( ) Embedded Query     (o) External Query Ref            |
| Match Input Field*          [ customerId v ]                                            |
| Return Field*               [ Customer Tier v ]                                        |
| No Match Behavior*          [ Return empty v ]                                         |
|                                                                                      |
| External Query Ref Mode                                                                |
| Data Source Ref*            [ customer-database v ]                                    |
| Query Ref*                  [ getCustomerTier v ]                                      |
|                                                                                      |
| Embedded Query Mode (shown when selected)                                              |
| Connection Name*           [ demo-database v ]                                         |
| SQL Template*                                                                         |
| [ SELECT tier FROM customer_profile_vw WHERE customer_id = :customerId            ] |
| Parameters                                                                           |
| +----+--------------------+-----------+-------------------------------------------+ |
| | #  | Parameter Name     | Type      | Value Source                              | |
| +----+--------------------+-----------+-------------------------------------------+ |
| | 1  | customerId         | string    | Input field: customerId                   | |
| +----+--------------------+-----------+-------------------------------------------+ |
|                                                                                      |
| Binding Preview (read-only)                                                            |
| lookup-config.lookup-dataset.type = database                                           |
| lookup-config.lookup-dataset.data-source-ref = customer-database                       |
| lookup-config.lookup-dataset.query-ref = getCustomerTier                               |
|                                                                                      |
| [ Validate Mapping ]                              [ < Back ] [ Save & Next > ]        |
+--------------------------------------------------------------------------------------+
```

## Screen 5: Enrichment Builder

```text
+--------------------------------------------------------------------------------------+
| Enrichment Builder                                                                     |
+--------------------------------------------------------------------------------------+
| Step 4 of 4                                                                           |
| Lookup Mapping: Get Customer Tier                                                      |
|                                                                                      |
| Enrichment Name*            [ Populate Settlement Instruction                       ] |
| Apply When                  [ ProductType in IRS, FX_OPTION                         ] |
| Missing Data Behavior*      [ Leave existing value v ]                                |
| Execution Priority          [ 100 ]                                                    |
|                                                                                      |
| Output Mappings                                                                         |
| +----+----------------------+-------------------------------+----------+ Condition --+ |
| | #  | Lookup Return Field  | Target Output Field           | Mode     | (optional)  | |
| +----+----------------------+-------------------------------+----------+------------+ |
| | 1  | Customer Tier        | settlementInstruction         | set      |            | |
| +----+----------------------+-------------------------------+----------+------------+ |
|                                                                                      |
| Validation                                                                              |
| [ ] Lookup mapping resolves                                                            |
| [ ] Target fields exist                                                                 |
| [ ] Condition syntax valid                                                              |
|                                                                                      |
| [ Validate Enrichment ]        [ < Back ] [ Save Draft ] [ Generate Preview > ]      |
+--------------------------------------------------------------------------------------+
```

## Screen 6: Generated Artifact Preview

```text
+--------------------------------------------------------------------------------------+
| Generated Artifact Preview                                                             |
+--------------------------------------------------------------------------------------+
| Change Set: CS-2026-03-07-001                                                          |
| Source: Basics Wizard                                                                   |
|                                                                                      |
| Tabs: [Business Summary] [Technical Bindings] [Generated YAML] [Validation]           |
|                                                                                      |
| Business Summary                                                                        |
| - Connection: Customer DB Prod                                                         |
| - Dataset: Customer Profile                                                            |
| - Lookup Mapping: Get Customer Tier                                                    |
| - Enrichment: Populate Settlement Instruction                                          |
|                                                                                      |
| Technical Bindings (read-only)                                                         |
| - data-source-ref: customer-database                                                   |
| - query-ref: getCustomerTier                                                           |
| - target-field: settlementInstruction                                                  |
|                                                                                      |
| Validation Results                                                                      |
| [PASS] Connection reachable                                                            |
| [PASS] Dataset fields resolved                                                         |
| [PASS] Lookup mapping valid                                                            |
| [PASS] Enrichment mapping valid                                                        |
|                                                                                      |
| [ < Back ] [ Export Spec ] [ Approve for Generate ]                                    |
+--------------------------------------------------------------------------------------+
```

## Mobile Layout Notes

```text
- Use stepper at top with one section expanded at a time.
- Replace wide tables with card rows and inline edit drawers.
- Keep Binding Preview collapsed by default.
- Keep primary CTA fixed at bottom: Next / Save / Validate.
```

## Interaction Notes

```text
- Every step has immediate validation and does not allow forward navigation on hard failures.
- Technical fields are always read-only unless user has platform role.
- Generated YAML is preview-only until explicit approval.
```
