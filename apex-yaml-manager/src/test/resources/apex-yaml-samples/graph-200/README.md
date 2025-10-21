# APEX YAML Dependency Graph - 200+ File Test Dataset

## Overview
This directory contains **246 YAML files** designed for comprehensive dependency tree testing of the APEX YAML Manager system. This dataset significantly expands beyond the original 100-file dataset to test enterprise-scale dependency management.

## File Structure

### Master Registry (1 file)
- `000-master-registry.yaml` - Master scenario registry referencing all core business scenarios

### Core Business Scenarios (25 files: 010-034)
- **Trading Scenarios**: Equity, Fixed Income, Derivatives, FX, Commodities, Crypto
- **Service Scenarios**: Repo, Securities Lending, Prime Brokerage, Clearing
- **Regional Scenarios**: Americas, EMEA, APAC, LATAM, MENA
- **Regulatory Scenarios**: MiFID II, Dodd-Frank, Basel III, EMIR, CFTC

### Rule Configurations (73 files: 050-131)
- **Asset-Specific Rules (050-099)**: Validation, enrichment, risk, settlement rules for each asset class
- **Base Framework Rules (100-131)**: Common validation, reference data, regulatory frameworks

### Enrichment Configurations (50 files: 150-199)
- Market data enrichment, counterparty enrichment, instrument enrichment
- Yield curves, credit ratings, volatility surfaces, Greeks calculations
- Regional and regulatory-specific enrichments

### External Data Configurations (50 files: 250-299)
- Database connections, API configurations, cache settings
- Market data feeds, reference data sources, regulatory reporting configs

### Pipeline Configurations (50 files: 350-399)
- Processing pipelines for each asset class and business scenario
- End-to-end workflow definitions with validation, enrichment, and output stages

## Dependency Patterns

### Complex Hierarchical Dependencies
- **Master Registry** → **Business Scenarios** → **Rule Groups** → **Base Rules**
- **Scenarios** → **Enrichments** → **External Configs**
- **Pipelines** → **Rules + Enrichments**

### Cross-Category Dependencies
- Rules reference other rules across different asset classes
- Enrichments depend on multiple external data configurations
- Regional rules inherit from base regulatory frameworks

### Deep Dependency Chains
- Some dependency chains reach **15+ levels deep**
- Complex inheritance patterns with shared base components
- Fan-out patterns where single files are referenced by many others

## Testing Scenarios

### Performance Testing
- **246 files** provide substantial load for dependency analysis
- Complex graph traversal with multiple entry points
- Memory and processing time optimization validation

### Circular Dependency Detection
- Intentional circular references for robustness testing
- Self-referencing scenarios for edge case validation

### Content Analysis Validation
- **Rule counts**: 2-4 rules per rule-config file
- **Enrichment counts**: 1-2 enrichments per enrichment file
- **File type detection**: All major APEX document types represented

### Real-World Simulation
- **Financial services domain**: Realistic trading and compliance scenarios
- **Enterprise scale**: Represents typical large financial institution complexity
- **Regulatory compliance**: Multiple jurisdictions and frameworks

## Usage

### REST API Testing
```bash
# Test with master registry (largest dependency tree)
curl "http://localhost:8082/yaml-manager/api/dependencies/tree?rootFile=src/test/resources/apex-yaml-samples/graph-200/000-master-registry.yaml"

# Test specific business scenarios
curl "http://localhost:8082/yaml-manager/api/dependencies/tree?rootFile=src/test/resources/apex-yaml-samples/graph-200/010-equity-trading.yaml"
```

### Expected Results
- **Root file**: 000-master-registry.yaml
- **Total files analyzed**: 100+ files (subset of 246 based on dependencies)
- **Maximum depth**: 15+ levels
- **Dependency patterns**: Hierarchical, cross-category, fan-out, circular

## File Naming Convention

- **000-0xx**: Registry and core scenarios
- **0xx-0xx**: Business scenarios and rule groups  
- **1xx-1xx**: Base rules and frameworks
- **2xx-2xx**: Enrichment configurations
- **3xx-3xx**: External data configurations
- **4xx-4xx**: Pipeline configurations

## APEX Compliance

All files follow the APEX YAML Reference specification:
- ✅ Required `metadata` section with `id`, `type`, `version`
- ✅ Valid APEX document types: `scenario-registry`, `scenario`, `rule-config`, `enrichment`, `external-data-config`, `pipeline-config`
- ✅ Proper dependency references using relative file paths
- ✅ Kebab-case naming for APEX keywords

## Generation

This dataset was generated using `generate-graph-200.ps1` script with:
- Systematic naming conventions
- Realistic financial services domain content
- Complex but logical dependency relationships
- Full APEX specification compliance

**Total Files**: 246 YAML files + 1 README = 247 files
**Estimated Dependency Tree Size**: 100-150 nodes (based on actual dependencies)
**Maximum Depth**: 15+ levels
**Test Coverage**: Enterprise-scale dependency management validation
