# APEX Visual Editor Keyword Coverage Analysis

**Date**: 2025-11-30 (Updated)
**File Analyzed**: `apex-playground/src/main/resources/static/apex_editor_main.html`
**Reference**: `docs/APEX_YAML_REFERENCE.md` (Version 2.4, ~155 keywords)

---

## Executive Summary

The APEX Visual Editor (Blockly-based) currently supports approximately **75 of ~155 APEX keywords** (~48% coverage). The editor covers core functionality for rule and enrichment authoring, and now includes:
- **Scenario configuration support** for workflow orchestration
- **Component references** for modular configuration management
- **Error recovery** for resilience and fault tolerance

Advanced features like rule chains and pipelines are not yet supported.

---

## Coverage by Category

### Metadata Section

| Keyword | Supported | Block | Notes |
|---------|-----------|-------|-------|
| `id` | Yes | apex_rule_config, apex_data_source_config | |
| `name` | Yes | apex_rule_config | |
| `version` | Yes | apex_rule_config, apex_data_source_config | |
| `description` | Yes | apex_rule_config, apex_data_source_config | |
| `author` | Yes | apex_rule_config, apex_data_source_config | |
| `business-domain` | Yes | apex_rule_config, apex_data_source_config | |
| `effective-date` | Yes | apex_rule_config, apex_data_source_config | |
| `expiration-date` | Yes | apex_rule_config, apex_data_source_config | |
| `tags` | Yes | apex_rule_config, apex_data_source_config | |
| `type` | No | - | Required field missing |
| `created` | No | - | |
| `last-modified` | No | - | |
| `owner` | No | - | |
| `created-by` | No | - | |

**Coverage: ~60% (9/15)**

---

### Rules Section

| Keyword | Supported | Block | Notes |
|---------|-----------|-------|-------|
| `rules` | Yes | apex_section_rules | |
| `condition` | Yes | apex_rule | |
| `message` | Yes | apex_rule | |
| `severity` | Yes | apex_rule | ERROR, WARNING, INFO |
| `enabled` | Yes | apex_rule | |
| `priority` | Yes | apex_rule | |
| `category` | Yes | apex_rule | |
| `result-field` | Yes | apex_rule | |
| `business-owner` | No | - | |
| `custom-properties` | No | - | |
| `validation` | No | - | Nested validation config |
| `error-code` | No | - | New feature |
| `success-code` | No | - | New feature |

**Coverage: ~67% (8/12)**

---

### Rule Groups Section

| Keyword | Supported | Block | Notes |
|---------|-----------|-------|-------|
| `rule-groups` | Yes | apex_section_rule_groups | |
| `operator` | Yes | apex_rule_group | AND/OR |
| `rule-ids` | Yes | apex_rule_group | |
| `stop-on-first-failure` | No | - | Important for control flow |
| `parallel-execution` | No | - | Performance feature |
| `debug-mode` | No | - | |
| `rule-references` | No | - | Detailed refs with sequence/override |
| `rule-group-references` | No | - | Nested groups |
| `error-handling` | No | - | fail-fast, continue-on-error, skip-on-error |
| `depends-on` | No | - | |

**Coverage: ~30% (3/10)**

---

### Enrichments Section

| Keyword | Supported | Block | Notes |
|---------|-----------|-------|-------|
| `enrichments` | Yes | apex_section_enrichments | |
| `type` | Yes | Generated | calculation-enrichment, field-enrichment, lookup-enrichment |
| `condition` | Yes | Multiple blocks | |
| `calculation-config` | Yes | apex_enrichment_calculation | |
| `expression` | Yes | apex_enrichment_calculation | |
| `result-field` | Yes | apex_enrichment_calculation | |
| `field-mappings` | Yes | apex_enrichment_field, apex_enrichment_lookup | |
| `source-field` | Yes | apex_field_mapping | |
| `target-field` | Yes | apex_field_mapping | |
| `lookup-config` | Yes | apex_enrichment_lookup | |
| `lookup-key` | Yes | apex_enrichment_lookup | |
| `lookup-dataset` | Yes | apex_enrichment_lookup | |
| `default-value` | No | - | Fallback for missing fields |
| `required` | No | - | Mandatory field flag |
| `conditional-mappings` | No | - | |
| `mapping-rules` | No | - | Complex mappings |
| `target-type` | No | - | |
| `execution-settings` | No | - | |
| `error-code` | No | - | |
| `success-code` | No | - | |
| `map-to-field` | No | - | |

**Coverage: ~60% (12/20)**

---

### Enrichment Groups Section

| Keyword | Supported | Block | Notes |
|---------|-----------|-------|-------|
| `enrichment-groups` | Yes | apex_section_enrichment_groups | |
| `operator` | Yes | apex_enrichment_group | AND/OR |
| `enrichment-ids` | Yes | apex_enrichment_group | |
| `stop-on-first-failure` | No | - | |
| `parallel-execution` | No | - | |
| `debug-mode` | No | - | |
| `enrichment-references` | No | - | Detailed refs |
| `enrichment-group-references` | No | - | Nested groups |
| `error-handling` | No | - | |

**Coverage: ~38% (3/8)**

---

### Data Sources Section

| Keyword | Supported | Block | Notes |
|---------|-----------|-------|-------|
| `data-sources` | Yes | apex_data_source_config | |
| `data-source-refs` | Yes | apex_section_data_source_refs | |
| `source-type` | Yes | Generated | database, rest-api, file |
| `connection` | Partial | apex_data_source_database | host, port, database, username, password |
| `queries` | Yes | apex_data_source_database | |
| `endpoints` | Yes | apex_data_source_rest | |
| `authentication` | Partial | apex_data_source_rest | type, username, password only |
| `base-path` | Yes | apex_data_source_file | |
| `file-pattern` | Yes | apex_data_source_file | |
| `cache` | No | - | TTL, max size, eviction |
| `cache-enabled` | No | - | |
| `cache-ttl-seconds` | No | - | |
| `connection-pool` | No | - | |
| `circuit-breaker` | No | - | Resilience feature |
| `health-check` | No | - | |
| `operations` | No | - | REST operations |
| `response-mapping` | No | - | |
| `timeout-ms` | No | - | |
| `retry` | No | - | |
| `polling-interval` | No | - | |
| `encoding` | No | - | |
| `file-format` | No | - | |

**Coverage: ~40% (10/25)**

---

### Transformations Section

| Keyword | Supported | Block | Notes |
|---------|-----------|-------|-------|
| `transformations` | Yes | apex_section_transformations | |
| `transformation-rules` | Yes | apex_conditional_transformation | |
| `condition` | Yes | apex_conditional_transformation | |
| `actions-true` | Yes | apex_conditional_transformation | |
| `actions-false` | Yes | apex_conditional_transformation | |
| `set-field` action | Yes | apex_action_set_field | |
| `else-actions` | No | - | Alias for actions-false |
| `actions` | No | - | Default actions list |

**Coverage: ~63% (5/8)**

---

## Scenarios Section (NEW - Implemented 2025-11-30)

| Keyword | Supported | Block | Notes |
|---------|-----------|-------|-------|
| `scenario` | Yes | apex_section_scenario | Scenario definition block |
| `scenario-id` | Yes | apex_section_scenario | Unique scenario identifier |
| `description` | Yes | apex_section_scenario, apex_processing_stage | |
| `data-types` | Yes | apex_section_scenario | Comma-separated list |
| `classification-rule` | Yes | apex_classification_rule | Condition + description |
| `processing-stages` | Yes | apex_section_scenario | Container for stages |
| `stage-name` | Yes | apex_processing_stage | |
| `config-file` | Yes | apex_processing_stage | Path to config file |
| `execution-order` | Yes | apex_processing_stage | Numeric order |
| `failure-policy` | Yes | apex_processing_stage | terminate, continue-with-warnings, flag-for-review |
| `required` | Yes | apex_processing_stage | Checkbox |
| `depends-on` | Yes | apex_processing_stage | Comma-separated list |
| `stage-metadata` | No | - | Not yet implemented |
| `name` | No | - | Scenario name (use metadata) |

**Coverage: ~85% (12/14)**

**New Blocks Added:**
- `apex_scenario_config` - Top-level scenario configuration
- `apex_section_scenario` - Scenario definition with classification and stages
- `apex_classification_rule` - Classification rule with condition
- `apex_processing_stage` - Processing stage configuration

---

## Unsupported Sections

### Rule Chains (0% Coverage)

No support for the 6 rule chaining patterns:

| Missing Keyword | Pattern |
|-----------------|---------|
| `rule-chains` | Container for rule chains |
| `trigger-rule` | Conditional chaining trigger |
| `conditional-rules` | Conditional chaining rules |
| `on-trigger` | Execute when trigger matches |
| `on-no-trigger` | Execute when trigger fails |
| `stages` | Sequential dependency stages |
| `output-variable` | Sequential dependency output |
| `routes` | Result-based routing paths |
| `routing-rule` | Rule to determine route |
| `accumulator` | Accumulative chaining initial value |
| `accumulation-rules` | Accumulative chaining rules |
| `weight` | Rule weight for accumulation |
| `decision-rule` | Final decision rule |
| `builder-target` | Fluent builder target |
| `on-success` | Fluent builder success path |
| `on-failure` | Fluent builder failure path |

---

### Pipeline Configuration (0% Coverage)

| Missing Keyword | Description |
|-----------------|-------------|
| `pipeline` | Pipeline configuration block |
| `mode` | sequential, parallel |
| `error-handling` | fail-fast, continue-on-error |
| `max-retries` | Maximum retry attempts |
| `retry-delay-ms` | Delay between retries |
| `collect-metrics` | Enable metrics collection |

---

### Error Recovery (88% Coverage) - NEW

| Keyword | Supported | Block | Notes |
|---------|-----------|-------|-------|
| `error-recovery` | Yes | apex_error_recovery | Top-level section |
| `enabled` | Yes | apex_error_recovery | Enable/disable recovery |
| `log-recovery-attempts` | Yes | apex_error_recovery | Log recovery attempts |
| `metrics-enabled` | Yes | apex_error_recovery | Enable metrics |
| `default-strategy` | Yes | apex_error_recovery | FAIL_FAST, CONTINUE_WITH_DEFAULT, SKIP_RULE, RETRY_WITH_SAFE_EXPRESSION |
| `severity-policies` | Yes | apex_error_recovery | Container for policies |
| `recovery-enabled` | Yes | apex_severity_policy | Per-severity enable |
| `strategy` | Yes | apex_severity_policy | Recovery strategy |
| `max-retries` | Yes | apex_severity_policy | Maximum retry attempts |
| `retry-delay` | Yes | apex_severity_policy | Delay between retries |
| `backoff-multiplier` | No | - | Exponential backoff |
| `alert-on-failure` | No | - | Trigger alerts |

**Coverage: 88% (7/8 keywords)**

---

### Component References (80% Coverage) - NEW

| Keyword | Supported | Block | Notes |
|---------|-----------|-------|-------|
| `type: component` | Yes | apex_component_config | Metadata type |
| `rule-configurations` | Yes | apex_file_reference | File reference type |
| `enrichment-refs` | Yes | apex_file_reference | File reference type |
| `component-refs` | Yes | apex_file_reference | File reference type |
| `config-files` | Yes | apex_file_reference | File reference type |
| `file` | Yes | apex_file_reference | File path |
| `execution-order` | Yes | apex_file_reference | Execution sequence |
| `failure-policy` | Yes | apex_file_reference | terminate, continue-with-warnings, flag-for-review |
| `criticality` | Yes | apex_component_config | high, medium, low |
| `sla-ms` | Yes | apex_component_config | SLA in milliseconds |
| `max-nesting-depth` | No | - | Nesting limit |
| `circular-ref-detection` | No | - | Automatic |

**Coverage: 80% (10/12 keywords)**

---

### Categories (0% Coverage)

| Missing Keyword | Description |
|-----------------|-------------|
| `categories` | Category definitions section |
| `parent-category` | Hierarchical category parent |

---

### Data Sinks (0% Coverage)

| Missing Keyword | Description |
|-----------------|-------------|
| `data-sinks` | Output destinations |
| `sink` | Single sink configuration |

---

## Coverage Summary Table

| Category | Keywords in APEX | Supported | Coverage |
|----------|------------------|-----------|----------|
| Metadata | ~15 | 9 | 60% |
| Rules | ~12 | 8 | 67% |
| Rule Groups | ~10 | 3 | 30% |
| Enrichments | ~20 | 12 | 60% |
| Enrichment Groups | ~8 | 3 | 38% |
| Data Sources | ~25 | 10 | 40% |
| Transformations | ~8 | 5 | 63% |
| **Scenarios** | **~14** | **12** | **85%** |
| **Error Recovery** | **~8** | **7** | **88%** |
| **Component Refs** | **~12** | **10** | **80%** |
| Rule Chains | ~15 | 0 | 0% |
| Pipeline | ~6 | 0 | 0% |
| Categories | ~2 | 0 | 0% |
| Data Sinks | ~2 | 0 | 0% |
| **Total** | **~155** | **~79** | **~51%** |

---

## Recommendations

### High Priority

Add missing keywords to existing blocks:

1. **Metadata Block**
   - Add `type` field (required by APEX)

2. **Rule Groups / Enrichment Groups**
   - Add `stop-on-first-failure` checkbox
   - Add `parallel-execution` checkbox
   - Add `error-handling` dropdown (fail-fast, continue-on-error, skip-on-error)

3. **Field Mappings**
   - Add `default-value` field
   - Add `required` checkbox

4. **Data Sources**
   - Add `cache` configuration block
   - Add `circuit-breaker` configuration
   - Add `timeout-ms` field

### Medium Priority

Add new block types:

1. **Scenario Blocks** - COMPLETED (2025-11-30)
   - `apex_scenario_config` - Main scenario configuration
   - `apex_section_scenario` - Scenario definition
   - `apex_classification_rule` - Classification rule
   - `apex_processing_stage` - Individual processing stage

2. **Pipeline Configuration Block**
   - `apex_pipeline_config` - Pipeline settings
   - Mode, error handling, retries

3. **Error Recovery Block** - COMPLETED (2025-11-30)
   - `apex_error_recovery` - Recovery configuration with global settings
   - `apex_severity_policy` - Per-severity recovery policies

4. **Component References** - COMPLETED (2025-11-30)
   - `apex_component_config` - Component configuration with metadata
   - `apex_file_reference` - File references with execution order and failure policy
   - `apex_data_source_ref` - External data source references

### Lower Priority

Add advanced features:

1. **Rule Chains**
   - Support for all 6 chaining patterns
   - Complex workflow visualization

2. **Categories Hierarchy**
   - Category definition blocks
   - Parent-child relationships

---

## Appendix: Block Type Inventory

### Currently Implemented Blocks

```
Configuration:
  - apex_rule_config
  - apex_data_source_config
  - apex_scenario_config
  - apex_component_config (NEW)

Sections:
  - apex_section_data_source_refs
  - apex_section_enrichment_groups
  - apex_section_enrichments
  - apex_section_rule_groups
  - apex_section_rules
  - apex_section_transformations
  - apex_section_scenario

Data Sources:
  - apex_data_source_database
  - apex_data_source_rest
  - apex_data_source_file
  - apex_data_source_query
  - apex_data_source_endpoint
  - apex_data_source_ref

Rules:
  - apex_rule
  - apex_rule_group
  - apex_rule_ref

Enrichments:
  - apex_enrichment_group
  - apex_enrichment_ref
  - apex_enrichment_calculation
  - apex_enrichment_field
  - apex_field_mapping

Lookups:
  - apex_enrichment_lookup
  - apex_lookup_dataset_reference
  - apex_lookup_dataset_database
  - apex_lookup_parameter

Transformations:
  - apex_conditional_transformation
  - apex_action_set_field

Scenarios:
  - apex_classification_rule
  - apex_processing_stage

Components (NEW):
  - apex_file_reference
  - apex_data_source_ref

Error Recovery (NEW):
  - apex_error_recovery
  - apex_severity_policy

Logic:
  - apex_condition_compare
  - apex_condition_logic
  - apex_field_ref
  - text (Blockly built-in)
  - math_number (Blockly built-in)
  - logic_boolean (Blockly built-in)
```

### Suggested New Blocks

```
Pipeline:
  - apex_pipeline_config

Rule Chains:
  - apex_rule_chain
  - apex_conditional_chain
  - apex_sequential_dependency
  - apex_result_routing
  - apex_accumulative_chain
  - apex_fluent_builder

Categories:
  - apex_category_definition
```

