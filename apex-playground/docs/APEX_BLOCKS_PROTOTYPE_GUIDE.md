# APEX Blocks Prototype Guide

**Version:** 2.0
**Date:** 2025-12-07
**Status:** Prototype

## Overview

The **APEX Blocks Prototype** (`apex_editor_main.html`) is a visual rule editor designed to simplify the creation of APEX YAML configurations. By leveraging **Google Blockly**, it provides a drag-and-drop interface that allows users to construct business logic visually without needing to memorize the strict YAML syntax.

This tool automatically generates valid APEX YAML in real-time as you build your logic, making it an excellent tool for:
*   **Rapid Prototyping**: Quickly sketching out rule logic.
*   **Learning APEX**: Understanding the structure of APEX configurations visually.
*   **Non-Technical Users**: Enabling business analysts to define rules without writing code.

## Getting Started

### Accessing the Tool
The editor is located in the `apex-playground` module:
`apex-playground/src/main/resources/static/apex_editor_main.html`

To use it, simply open this file in any modern web browser. No server or build process is required for the basic prototype functionality.

### Interface Layout
1.  **Header**: Contains the title and the **Download YAML** button.
2.  **Workspace (Left)**: The main canvas where you drag and drop blocks to build your configuration.
3.  **YAML Output (Right)**: A real-time preview of the generated YAML code.
4.  **Toolbox (Popup)**: A categorized menu of available blocks organized into 12 categories.

---

## Block Reference

This section provides a detailed description of every block currently implemented, organized by their toolbox categories.

---

### 1. Configuration Category

Top-level configuration blocks that serve as root containers for APEX YAML documents.

#### 1.1 Configuration (`apex_rule_config`)

The primary configuration block for rules, enrichments, and transformations.

| Attribute | Type | Default | Description |
|-----------|------|---------|-------------|
| ID | Text | `my-config` | Unique identifier for the configuration |
| Name | Text | `My Configuration` | Human-readable name |
| Version | Text | `1.0.0` | Semantic version number |
| **Collapsible Metadata Section** ||||
| Description | Text | `My configuration` | Detailed description |
| Author | Text | `user@example.com` | Author email |
| Created By | Text | (empty) | Creator identifier |
| Created Date | Text | (empty) | Creation date |
| Last Modified | Text | (empty) | Last modification date |
| Business Domain | Text | `Trade Processing` | Business domain context |
| Effective Date | Text | (empty) | When configuration becomes active |
| Expiration Date | Text | (empty) | When configuration expires |
| Tags | Text | `tag1, tag2` | Comma-separated tags |
| **Statement Input** ||||
| Sections | Statement | - | Accepts Section blocks |

**Connection Type:** None (root block)

---

#### 1.2 Data Source Config (`apex_data_source_config`)

Top-level configuration for data source definitions.

| Attribute | Type | Default | Description |
|-----------|------|---------|-------------|
| ID | Text | `my-ds-config` | Unique identifier |
| Version | Text | `1.0.0` | Semantic version |
| **Collapsible Metadata Section** ||||
| Description | Text | `Data Source Configuration` | Description |
| Author | Text | `user@example.com` | Author email |
| Created By | Text | (empty) | Creator identifier |
| Created Date | Text | (empty) | Creation date |
| Last Modified | Text | (empty) | Last modification date |
| Business Domain | Text | `Infrastructure` | Business domain |
| Effective Date | Text | (empty) | Effective date |
| Expiration Date | Text | (empty) | Expiration date |
| Tags | Text | `tag1, tag2` | Comma-separated tags |
| **Statement Input** ||||
| Data Sources | Statement | - | Accepts DataSource blocks |

**Connection Type:** None (root block)

---

#### 1.3 Scenario Configuration (`apex_scenario_config`)

Top-level scenario configuration file.

| Attribute | Type | Default | Description |
|-----------|------|---------|-------------|
| ID | Text | `my-scenario` | Unique scenario identifier |
| Name | Text | `My Scenario` | Human-readable name |
| Version | Text | `1.0.0` | Semantic version |
| Business Domain | Text | `Trade Processing` | Business domain |
| **Collapsible Metadata Section** ||||
| Description | Text | `Scenario description` | Description |
| Author | Text | `user@example.com` | Author email |
| Created By | Text | (empty) | Creator identifier |
| Created Date | Text | (empty) | Creation date |
| Last Modified | Text | (empty) | Last modification date |
| Owner | Text | `team@example.com` | Owner contact |
| Tags | Text | `scenario, workflow` | Comma-separated tags |
| **Statement Input** ||||
| Scenario | Statement | - | Accepts ScenarioSection blocks |

**Connection Type:** None (root block)

---

#### 1.4 Scenario Registry (`apex_scenario_registry`)

Registry that references multiple scenario configuration files.

| Attribute | Type | Default | Description |
|-----------|------|---------|-------------|
| ID | Text | `my-registry` | Unique registry identifier |
| Name | Text | `My Scenario Registry` | Human-readable name |
| Version | Text | `1.0.0` | Semantic version |
| **Collapsible Metadata Section** ||||
| Description | Text | `Registry description` | Description |
| Created By | Text | `user@example.com` | Creator identifier |
| Created Date | Text | (empty) | Creation date |
| Last Modified | Text | (empty) | Last modification date |
| Tags | Text | `registry, scenarios` | Comma-separated tags |
| **Routing Configuration** ||||
| Routing Strategy | Dropdown | `type-based` | Options: `type-based`, `classification-based` |
| Default Scenario | Text | (empty) | Default scenario ID |
| **Statement Input** ||||
| Scenarios | Statement | - | Accepts ScenarioRef blocks |

**Connection Type:** None (root block)

---

#### 1.5 Component Configuration (`apex_component_config`)

Component configuration grouping multiple config files.

| Attribute | Type | Default | Description |
|-----------|------|---------|-------------|
| ID | Text | `my-component` | Unique component identifier |
| Name | Text | `My Component` | Human-readable name |
| Version | Text | `1.0.0` | Semantic version |
| **Collapsible Metadata Section** ||||
| Description | Text | `Component description` | Description |
| Business Domain | Text | `Trade Processing` | Business domain |
| Owner | Text | `team@example.com` | Owner contact |
| Created By | Text | (empty) | Creator identifier |
| Created Date | Text | (empty) | Creation date |
| Last Modified | Text | (empty) | Last modification date |
| Criticality | Dropdown | `high` | Options: `high`, `medium`, `low` |
| SLA (ms) | Number | `200` | SLA in milliseconds |
| Tags | Text | `component, reusable` | Comma-separated tags |
| **Statement Input** ||||
| File References | Statement | - | Accepts FileReference blocks |

**Connection Type:** None (root block)

---

### 2. Scenarios Category

Blocks for defining scenario workflows and classification logic.

#### 2.1 Scenario (`apex_section_scenario`)

Scenario definition with classification and processing stages.

| Attribute | Type | Default | Description |
|-----------|------|---------|-------------|
| Scenario ID | Text | `my-scenario` | Unique scenario identifier |
| Description | Text | `Scenario workflow` | Description |
| Data Types | Text | `HashMap, Map` | Comma-separated data types |
| **Value Input** ||||
| Classification Rule | Value | - | Accepts ClassificationRule block |
| **Statement Input** ||||
| Processing Stages | Statement | - | Accepts ProcessingStage blocks |

**Connection Type:** Previous/Next: `ScenarioSection`

---

#### 2.2 Classification Rule (`apex_classification_rule`)

Classification rule to determine if scenario applies.

| Attribute | Type | Default | Description |
|-----------|------|---------|-------------|
| Description | Text | `Route matching trades` | Rule description |
| **Value Input** ||||
| Condition | Value | - | Accepts Boolean or String (SpEL expression) |

**Connection Type:** Output: `ClassificationRule`

---

#### 2.3 Processing Stage (`apex_processing_stage`)

Processing stage in the scenario workflow.

| Attribute | Type | Default | Description |
|-----------|------|---------|-------------|
| Stage Name | Text | `validation` | Stage identifier |
| Description | Text | `Validate incoming data` | Stage description |
| Config File | Text | `config/validation-rules.yaml` | Path to configuration file |
| Execution Order | Number | `1` | Order of execution (min: 1) |
| Failure Policy | Dropdown | `terminate` | Options: `terminate`, `continue-with-warnings`, `flag-for-review` |
| Required | Checkbox | `true` | Whether stage is required |
| Depends On | Text | (empty) | Comma-separated stage dependencies |

**Connection Type:** Previous/Next: `ProcessingStage`

---

#### 2.4 Scenario Reference (`apex_scenario_ref`)

Reference to a scenario configuration file.

| Attribute | Type | Default | Description |
|-----------|------|---------|-------------|
| Scenario ID | Text | `my-scenario` | Scenario identifier |
| Config File | Text | `scenarios/my-scenario.yaml` | Path to scenario config |
| Business Domain | Text | `Trading` | Business domain |

**Connection Type:** Previous/Next: `ScenarioRef`

---

### 3. Components Category

Blocks for referencing external configuration files and data sources.

#### 3.1 File Reference (`apex_file_reference`)

Reference to an external configuration file.

| Attribute | Type | Default | Description |
|-----------|------|---------|-------------|
| Type | Dropdown | `rule-configurations` | Options: `rule-configurations`, `enrichment-refs`, `component-refs`, `config-files` |
| File | Text | `path/to/config.yaml` | Path to configuration file |
| Execution Order | Number | `1` | Order of execution (min: 1) |
| Failure Policy | Dropdown | `terminate` | Options: `terminate`, `continue-with-warnings`, `flag-for-review` |

**Connection Type:** Previous/Next: `FileReference`

---

#### 3.2 Data Source Reference (`apex_data_source_ref`)

Reference to an external data source configuration file.

| Attribute | Type | Default | Description |
|-----------|------|---------|-------------|
| Name | Text | `my-database` | Data source name |
| Source | Text | `data-sources/database-config.yaml` | Path to data source config |
| Enabled | Checkbox | `true` | Whether reference is enabled |
| Description | Text | `Reference to external database configuration` | Description |

**Connection Type:** Previous/Next: `DataSourceRef`

---

### 4. Sections Category

Container blocks that organize content within configurations.

#### 4.1 Data Source Refs Section (`apex_section_data_source_refs`)

Container for data source references.

| Attribute | Type | Default | Description |
|-----------|------|---------|-------------|
| **Statement Input** ||||
| Data Source Refs | Statement | - | Accepts DataSourceRef blocks |

**Connection Type:** Previous/Next: `Section`

---

#### 4.2 Enrichment Groups Section (`apex_section_enrichment_groups`)

Container for enrichment groups.

| Attribute | Type | Default | Description |
|-----------|------|---------|-------------|
| **Statement Input** ||||
| Enrichment Groups | Statement | - | Accepts EnrichmentGroup blocks |

**Connection Type:** Previous/Next: `Section`

---

#### 4.3 Enrichments Section (`apex_section_enrichments`)

Container for enrichments.

| Attribute | Type | Default | Description |
|-----------|------|---------|-------------|
| **Statement Input** ||||
| Enrichments | Statement | - | Accepts Enrichment blocks |

**Connection Type:** Previous/Next: `Section`

---

#### 4.4 Rule Groups Section (`apex_section_rule_groups`)

Container for rule groups.

| Attribute | Type | Default | Description |
|-----------|------|---------|-------------|
| **Statement Input** ||||
| Rule Groups | Statement | - | Accepts RuleGroup blocks |

**Connection Type:** Previous/Next: `Section`

---

#### 4.5 Rules Section (`apex_section_rules`)

Container for rules.

| Attribute | Type | Default | Description |
|-----------|------|---------|-------------|
| **Statement Input** ||||
| Rules | Statement | - | Accepts Rule blocks |

**Connection Type:** Previous/Next: `Section`

---

#### 4.6 Transformations Section (`apex_section_transformations`)

Container for transformations.

| Attribute | Type | Default | Description |
|-----------|------|---------|-------------|
| **Statement Input** ||||
| Transformations | Statement | - | Accepts Transformation blocks |

**Connection Type:** Previous/Next: `Section`

---

#### 4.7 Error Recovery Section (`apex_error_recovery`)

Error recovery configuration section.

| Attribute | Type | Default | Description |
|-----------|------|---------|-------------|
| Enabled | Checkbox | `true` | Enable error recovery |
| Log Recovery Attempts | Checkbox | `true` | Log recovery attempts |
| Metrics Enabled | Checkbox | `true` | Enable metrics collection |
| Default Strategy | Dropdown | `CONTINUE_WITH_DEFAULT` | Options: `CONTINUE_WITH_DEFAULT`, `FAIL_FAST`, `SKIP_RULE`, `RETRY_WITH_SAFE_EXPRESSION` |
| **Statement Input** ||||
| Severity Policies | Statement | - | Accepts SeverityPolicy blocks |

**Connection Type:** Previous/Next: `Section`

---

### 5. Error Recovery Category

Blocks for configuring error handling policies.

#### 5.1 Severity Policy (`apex_severity_policy`)

Severity-specific error recovery policy.

| Attribute | Type | Default | Description |
|-----------|------|---------|-------------|
| Severity | Dropdown | `CRITICAL` | Options: `CRITICAL`, `ERROR`, `WARNING`, `INFO` |
| Recovery Enabled | Checkbox | `false` | Enable recovery for this severity |
| Strategy | Dropdown | `FAIL_FAST` | Options: `FAIL_FAST`, `CONTINUE_WITH_DEFAULT`, `SKIP_RULE`, `RETRY_WITH_SAFE_EXPRESSION` |
| Max Retries | Number | `0` | Maximum retry attempts (min: 0) |
| Retry Delay (ms) | Number | `100` | Delay between retries in milliseconds (min: 0) |

**Connection Type:** Previous/Next: `SeverityPolicy`

---

### 6. Data Sources Category

Blocks for defining data source connections.

#### 6.1 Database Source (`apex_data_source_database`)

Defines a database data source.

| Attribute | Type | Default | Description |
|-----------|------|---------|-------------|
| ID | Text | `ds-db-1` | Unique identifier |
| Name | Text | `db-source` | Data source name |
| Enabled | Checkbox | `true` | Whether source is enabled |
| Description | Text | (empty) | Description |
| Host | Text | `localhost` | Database host |
| Port | Number | `5432` | Database port |
| Database | Text | `mydb` | Database name |
| Username | Text | `user` | Database username |
| Password | Text | `pass` | Database password |
| **Statement Input** ||||
| Queries | Statement | - | Accepts DataSourceQuery blocks |

**Connection Type:** Previous/Next: `DataSource`

---

#### 6.2 REST API Source (`apex_data_source_rest`)

Defines a REST API data source.

| Attribute | Type | Default | Description |
|-----------|------|---------|-------------|
| ID | Text | `ds-rest-1` | Unique identifier |
| Name | Text | `api-source` | Data source name |
| Enabled | Checkbox | `true` | Whether source is enabled |
| Description | Text | (empty) | Description |
| Base URL | Text | `https://api.example.com` | Base URL for API |
| Timeout (ms) | Number | `30000` | Request timeout in milliseconds |
| **Statement Input** ||||
| Endpoints | Statement | - | Accepts DataSourceEndpoint blocks |

**Connection Type:** Previous/Next: `DataSource`

---

#### 6.3 File Source (`apex_data_source_file`)

Defines a file system data source.

| Attribute | Type | Default | Description |
|-----------|------|---------|-------------|
| ID | Text | `ds-file-1` | Unique identifier |
| Name | Text | `file-source` | Data source name |
| Enabled | Checkbox | `true` | Whether source is enabled |
| Description | Text | (empty) | Description |
| Base Path | Text | `/data/files` | Base directory path |
| File Pattern | Text | `*.csv` | File matching pattern |
| Format | Dropdown | `csv` | Options: `CSV`, `JSON`, `XML` |

**Connection Type:** Previous/Next: `DataSource`

---

#### 6.4 Query (`apex_data_source_query`)

Defines a query for a data source.

| Attribute | Type | Default | Description |
|-----------|------|---------|-------------|
| ID | Text | `q-1` | Unique identifier |
| Name | Text | `query1` | Query name |
| SQL/Path | Text | `SELECT * FROM table` | SQL query or file path |

**Connection Type:** Previous/Next: `DataSourceQuery`

---

#### 6.5 Endpoint (`apex_data_source_endpoint`)

Defines a REST endpoint.

| Attribute | Type | Default | Description |
|-----------|------|---------|-------------|
| ID | Text | `ep-1` | Unique identifier |
| Name | Text | `endpoint1` | Endpoint name |
| Path | Text | `/v1/resource` | Endpoint path |

**Connection Type:** Previous/Next: `DataSourceEndpoint`

---

#### 6.6 Data Source Reference (`apex_data_source_ref`)

Reference to an external data source configuration.

| Attribute | Type | Default | Description |
|-----------|------|---------|-------------|
| Name | Text | `my-db` | Reference name |
| Source | Text | `data-sources/my-db.yaml` | Path to data source config |
| Enabled | Checkbox | `true` | Whether reference is enabled |
| Description | Text | (empty) | Description |

**Connection Type:** Previous/Next: `DataSourceRef`

---

### 7. Rules Category

Blocks for defining business rules and rule groups.

#### 7.1 Rule (`apex_rule`)

Defines a single business rule.

| Attribute | Type | Default | Description |
|-----------|------|---------|-------------|
| ID | Text | `rule-1` | Unique rule identifier |
| Name | Text | `My Rule` | Human-readable name |
| **Collapsible Options Section** ||||
| Enabled | Checkbox | `true` | Whether rule is enabled |
| Message | Text | `Rule triggered` | Message when rule fires |
| Severity | Dropdown | `ERROR` | Options: `ERROR`, `WARNING`, `INFO` |
| Priority | Text | (empty) | Rule priority |
| Category | Text | (empty) | Rule category |
| Result Field | Value | - | Field to store result |
| **Value Input** ||||
| Condition | Value | - | Accepts Boolean or String (SpEL expression) |

**Connection Type:** Previous/Next: `Rule`

---

#### 7.2 Rule Group (`apex_rule_group`)

Groups rules together with logical operator.

| Attribute | Type | Default | Description |
|-----------|------|---------|-------------|
| ID | Text | `rule-group-1` | Unique group identifier |
| Name | Text | `My Rule Group` | Human-readable name |
| Enabled | Checkbox | `true` | Whether group is enabled |
| Operator | Dropdown | `AND` | Options: `AND`, `OR` |
| **Statement Input** ||||
| Rules | Statement | - | Accepts RuleRef blocks |

**Connection Type:** Previous/Next: `RuleGroup`

---

#### 7.3 Rule Reference (`apex_rule_ref`)

Reference to a rule ID (dynamic dropdown populated from workspace rules).

| Attribute | Type | Default | Description |
|-----------|------|---------|-------------|
| Rule ID | Dropdown | (dynamic) | Dropdown populated with rule IDs from workspace |

**Connection Type:** Previous/Next: `RuleRef`

---

### 8. Transformations Category

Blocks for conditional transformations and field operations.

#### 8.1 Conditional Transformation (`apex_conditional_transformation`)

Conditionally executes transformations.

| Attribute | Type | Default | Description |
|-----------|------|---------|-------------|
| ID (Optional) | Text | (empty) | Optional identifier |
| Enabled | Checkbox | `true` | Whether transformation is enabled |
| **Value Input** ||||
| Condition | Value | - | Accepts Boolean or String (SpEL expression) |
| **Statement Inputs** ||||
| Actions (True) | Statement | - | Transformations when condition is true |
| Actions (False) | Statement | - | Transformations when condition is false |

**Connection Type:** Previous/Next: `Transformation`

---

#### 8.2 Set Field (`apex_action_set_field`)

Sets a field to a specific value.

| Attribute | Type | Default | Description |
|-----------|------|---------|-------------|
| **Value Inputs** ||||
| Field | Value | - | Field name (accepts String) |
| Value | Value | - | Value to set (accepts String, Number, Boolean) |

**Connection Type:** Previous/Next: `Transformation`

---

### 9. Enrichments Category

Blocks for data enrichment operations.

#### 9.1 Enrichment Group (`apex_enrichment_group`)

Groups enrichments together.

| Attribute | Type | Default | Description |
|-----------|------|---------|-------------|
| ID | Text | `eg-1` | Unique group identifier |
| Name | Text | `My Enrichment Group` | Human-readable name |
| Enabled | Checkbox | `true` | Whether group is enabled |
| Operator | Dropdown | `AND` | Options: `AND`, `OR` |
| **Statement Input** ||||
| Enrichments | Statement | - | Accepts EnrichmentRef blocks |

**Connection Type:** Previous/Next: `EnrichmentGroup`

---

#### 9.2 Enrichment Reference (`apex_enrichment_ref`)

Reference to an enrichment ID.

| Attribute | Type | Default | Description |
|-----------|------|---------|-------------|
| Enrichment ID | Text | `enrichment-1` | Enrichment identifier |

**Connection Type:** Previous/Next: `EnrichmentRef`

---

#### 9.3 Calculation Enrichment (`apex_enrichment_calculation`)

Performs a calculation and stores the result.

| Attribute | Type | Default | Description |
|-----------|------|---------|-------------|
| ID | Text | `calc-1` | Unique identifier |
| Enabled | Checkbox | `true` | Whether enrichment is enabled |
| Result Field | Text | `calculatedValue` | Field to store result |
| **Value Inputs** ||||
| Condition | Value | - | Accepts Boolean or String (SpEL expression) |
| Expression | Value | - | Calculation expression (accepts String, Number) |

**Connection Type:** Previous/Next: `Enrichment`

---

#### 9.4 Field Enrichment (`apex_enrichment_field`)

Maps or transforms fields.

| Attribute | Type | Default | Description |
|-----------|------|---------|-------------|
| ID | Text | `field-enrich-1` | Unique identifier |
| Enabled | Checkbox | `true` | Whether enrichment is enabled |
| **Value Input** ||||
| Condition | Value | - | Accepts Boolean or String (SpEL expression) |
| **Statement Input** ||||
| Transformations | Statement | - | Accepts Transformation blocks |

**Connection Type:** Previous/Next: `Enrichment`

---

#### 9.5 Field Mapping (`apex_field_mapping`)

Maps a single field.

| Attribute | Type | Default | Description |
|-----------|------|---------|-------------|
| Source | Text | `sourceField` | Source field name |
| Target | Text | `targetField` | Target field name |
| **Value Input** ||||
| Expression | Value | - | Transformation expression (accepts String, Number) |

**Connection Type:** Previous/Next: `Transformation`

---

### 10. Lookups Category

Blocks for data lookup operations.

#### 10.1 Lookup Enrichment (`apex_enrichment_lookup`)

Look up data from an external dataset.

| Attribute | Type | Default | Description |
|-----------|------|---------|-------------|
| ID | Text | `lookup-1` | Unique identifier |
| Enabled | Checkbox | `true` | Whether lookup is enabled |
| **Value Inputs** ||||
| Condition | Value | - | Accepts Boolean or String (SpEL expression) |
| Lookup Key | Value | - | Key for lookup (accepts String) |
| Dataset Config | Value | - | Accepts LookupDataset block |
| **Statement Input** ||||
| Mappings | Statement | - | Accepts Transformation blocks |

**Connection Type:** Previous/Next: `Enrichment`

---

#### 10.2 Reference Dataset (`apex_lookup_dataset_reference`)

Reference to a pre-defined dataset.

| Attribute | Type | Default | Description |
|-----------|------|---------|-------------|
| ID | Text | `dataset-1` | Dataset identifier |
| Key Field | Text | `id` | Key field for lookup |

**Connection Type:** Output: `LookupDataset`

---

#### 10.3 Database Dataset (`apex_lookup_dataset_database`)

Database query lookup.

| Attribute | Type | Default | Description |
|-----------|------|---------|-------------|
| Data Source Ref | Text | `customer-database` | Reference to data source |
| Query | Text | `SELECT * FROM table WHERE id = :id` | SQL query with parameters |
| **Statement Input** ||||
| Parameters | Statement | - | Accepts LookupParameter blocks |

**Connection Type:** Output: `LookupDataset`

---

#### 10.4 Lookup Parameter (`apex_lookup_parameter`)

Query parameter mapping.

| Attribute | Type | Default | Description |
|-----------|------|---------|-------------|
| Field | Text | `customerId` | Field name for parameter |
| Type | Dropdown | `String` | Options: `String`, `Integer`, `Boolean` |

**Connection Type:** Previous/Next: `LookupParameter`

---

### 11. Logic Category

Blocks for building SpEL expressions and conditions.

#### 11.1 Comparison (`apex_condition_compare`)

Comparison operation.

| Attribute | Type | Default | Description |
|-----------|------|---------|-------------|
| Operator | Dropdown | `==` | Options: `==`, `!=`, `>`, `<`, `>=`, `<=` |
| **Value Inputs** ||||
| A | Value | - | Left operand |
| B | Value | - | Right operand |

**Connection Type:** Output: `Boolean`

---

#### 11.2 Logic (`apex_condition_logic`)

Logical operation.

| Attribute | Type | Default | Description |
|-----------|------|---------|-------------|
| Operator | Dropdown | `AND` | Options: `AND` (&&), `OR` (\|\|) |
| **Value Inputs** ||||
| A | Value | - | Left operand (accepts Boolean) |
| B | Value | - | Right operand (accepts Boolean) |

**Connection Type:** Output: `Boolean`

---

#### 11.3 Field Reference (`apex_field_ref`)

Reference a data field from loaded JSON.

| Attribute | Type | Default | Description |
|-----------|------|---------|-------------|
| Field | Dropdown | (dynamic) | Dropdown populated from loaded JSON data |

**Connection Type:** Output: `String`

---

#### 11.4 Text (`text`)

Standard Blockly text block for string values.

| Attribute | Type | Default | Description |
|-----------|------|---------|-------------|
| TEXT | Text | (empty) | Text value |

**Connection Type:** Output: `String`

---

#### 11.5 Number (`math_number`)

Standard Blockly number block for numeric values.

| Attribute | Type | Default | Description |
|-----------|------|---------|-------------|
| NUM | Number | `0` | Numeric value |

**Connection Type:** Output: `Number`

---

#### 11.6 Boolean (`logic_boolean`)

Standard Blockly boolean block for true/false values.

| Attribute | Type | Default | Description |
|-----------|------|---------|-------------|
| BOOL | Dropdown | `TRUE` | Options: `TRUE`, `FALSE` |

**Connection Type:** Output: `Boolean`

---

### 12. Templates Category

Custom category for pre-built template configurations. Templates are dynamically loaded and provide quick-start configurations for common use cases.

---

## Block-to-YAML Mapping

The following table illustrates how visual blocks map to the APEX YAML specification:

| Visual Block | YAML Section | Description |
| :--- | :--- | :--- |
| **Configuration** | `metadata`, `rules`, `enrichments` | The root document structure. |
| **Rule** | `rules` list item | Defines a single validation rule. |
| **Rule Group** | `rule-groups` list item | Groups rules with logic. |
| **Lookup Enrichment** | `enrichments` (type: `lookup-enrichment`) | Configures data lookups. |
| **Calculation** | `enrichments` (type: `calculation-enrichment`) | Performs SpEL calculations. |
| **Field Ref (`#field`)** | SpEL Expression (`#field`) | Direct field access syntax. |
| **Error Recovery** | `error-recovery` section | Error handling configuration. |
| **Severity Policy** | `severity-policies` list item | Severity-specific recovery policy. |
| **Scenario** | `scenario` section | Scenario workflow definition. |
| **Processing Stage** | `processing-stages` list item | Stage in scenario workflow. |

## Technical Details

*   **Library**: Built on [Google Blockly](https://developers.google.com/blockly).
*   **YAML Generation**: Uses `js-yaml` for client-side YAML serialization.
*   **Customization**: The block definitions and code generators are located in the `<script>` section of the HTML file.
*   **Collapsible Sections**: Uses custom `FieldCollapsibleSection` field for collapsing optional metadata.
*   **Dynamic Dropdowns**: Rule Reference and Field Reference blocks use dynamic dropdowns populated from workspace content.

## Future Improvements
*   Support for `error-handling` configuration in Rule Groups.
*   Integration with the APEX REST API for direct deployment.
*   Support for `Rule Chains` and `Pipelines`.
