# Graph-100 Test Dataset

This directory contains **100 YAML files** designed to test complex dependency scenarios for the APEX YAML Manager dependency tree viewer.

## File Structure

The files are organized by category with numeric prefixes:

- **00-09**: Registry files (1 file)
  - `00-scenario-registry.yaml` - Root registry file that references main scenarios

- **10-19**: Scenario files (10 files)
  - `10-scenario-a.yaml` - Complex scenario with deep chains and cycles
  - `11-scenario-b.yaml` - Scenario with missing file references
  - `12-scenario-c.yaml` - Simple scenario referencing groups C
  - `13-scenario-d.yaml` - Simple scenario referencing groups D
  - `14-scenario-e.yaml` through `19-scenario-j.yaml` - Extended scenarios with various patterns

- **20-29**: Group files (10 files)
  - `20-groups-a.yaml` - Complex groups with multiple dependencies
  - `21-groups-b.yaml` - Groups with missing file references
  - `22-groups-c.yaml` - Simple groups C
  - `23-groups-d.yaml` - Simple groups D
  - `24-groups-e.yaml` - Extended groups E
  - `25-groups-common.yaml` - Shared groups referenced by multiple files
  - `26-28-groups-parallel-*.yaml` - Parallel processing groups
  - `29-groups-aggregator.yaml` - Aggregation group

- **30-39**: Rule files (10 files)
  - `30-rules-a.yaml` through `39-rules-parallel-2.yaml` - Various rule configurations
  - `35-rules-common.yaml` - Shared rules
  - `37-rules-e.yaml` - Extended rules E
  - `38-39-rules-parallel-*.yaml` - Parallel processing rules

- **40-49**: Cycle files (10 files)
  - `40-cycle-1.yaml`, `41-cycle-2.yaml` - 2-node circular dependency
  - `42-49-cycle-*.yaml` - Various circular dependency patterns including 3-node cycles, independent cycles, complex cycles, self-reference

- **50-59**: Chain files (10 files)
  - `50-chain-1.yaml` through `59-chain-10.yaml` - Extended deep dependency chain (depth 10)

- **60-69**: Enrichment files (10 files)
  - `60-enrich-a.yaml`, `61-enrich-b.yaml`, `62-enrich-common.yaml` - Basic enrichments
  - `63-69-enrich-*.yaml` - Extended enrichments for validation, parallel processing, aggregation, calculation, timestamp

- **70-79**: Configuration files (10 files)
  - `70-config-db.yaml`, `71-config-cache.yaml` - Basic configurations
  - `72-79-config-*.yaml` - Extended configurations for market data, reference data, clearing, risk, settlement, messaging, file system, monitoring

- **80-89**: Dataset files (10 files)
  - `80-89-dataset-*.yaml` - Compliance, currencies, markets, instruments, counterparties, holidays, settlement cycles, risk factors, business rules, fee schedules

- **90-99**: Pipeline and test files (10 files)
  - `90-95-pipeline-*.yaml` - Audit, reporting, settlement, risk calculation, fee calculation, data quality pipelines
  - `96-pipeline-monitoring.yaml`, `97-pipeline-reconciliation.yaml` - Additional pipelines
  - `98-invalid.yaml` - Invalid YAML for error testing
  - `99-*.yaml` - Various test files including missing metadata, circular self-reference, deep reference chains, fan-out stress, cross-category complex, performance test, integration test, edge case test, load test, final test

## Dependency Patterns

The dataset includes these complex dependency patterns:

1. **Deep Chains**: 10-level dependency chain (50-59)
2. **Circular Dependencies**: 2-node, 3-node, and complex cycles (40-49)
3. **Fan-out**: Single file referencing multiple dependencies (99-fan-out-stress.yaml)
4. **Fan-in**: Multiple files referencing same dependency
5. **Cross-category**: Dependencies across different file types (99-cross-category-complex.yaml)
6. **Missing Files**: References to non-existent files for error testing
7. **Invalid Files**: Malformed YAML for error handling
8. **Self-reference**: Files that reference themselves (99-circular-self-ref.yaml)
9. **Parallel Processing**: Multiple parallel execution paths
10. **Performance Testing**: Maximum load scenarios (99-load-test.yaml)

## Usage

Use `00-scenario-registry.yaml` as the root file for dependency tree analysis. This will traverse the most complex dependency patterns in the dataset.

Example REST API call:
```
GET /yaml-manager/api/dependencies/tree?rootFile=C:/path/to/graph-100/00-scenario-registry.yaml
```

## APEX Compliance

All files strictly follow the APEX_YAML_REFERENCE.md specification:
- Required metadata fields: id, name, version, description, type
- Recommended fields: author, created-date
- Proper YAML structure for each document type
- Correct kebab-case naming for APEX keywords
- ISO date format (YYYY-MM-DD) for created-date field

## Test Coverage

The 100-file dataset provides comprehensive test coverage for:
- **Dependency Analysis**: Complex chains, cycles, fan-out/in patterns
- **Content Analysis**: All APEX document types with proper content counting
- **Error Handling**: Missing files, invalid YAML, circular references
- **Performance**: Large-scale dependency graphs with 100+ files
- **Edge Cases**: Self-reference, deep chains, maximum fan-out scenarios

