# APEX Rules Engine - AI Coding Agent Instructions

## Project Overview
APEX is a comprehensive Java-based rules engine (v2.1) for financial services with YAML-driven configuration. The project emphasizes **clean architecture** through its revolutionary external data-source reference system that separates business logic from infrastructure configuration.

## Architecture & Key Concepts

### Multi-Module Structure
- **`apex-core`**: Core engine, YAML processing, external data-source reference system
- **`apex-demo`**: 16+ comprehensive demos with financial services patterns
- **`apex-playground`**: Interactive Spring Boot web UI (port 8081) for development
- **`apex-rest-api`**: Complete REST API with OpenAPI/Swagger (port 8080)
- **`apex-compiler`**: YAML validation and compilation tools

### External Data-Source Reference System (APEX 2.1)
The defining architectural pattern separating infrastructure from business logic:

```yaml
# Business Logic Configuration (lean and focused)
data-source-refs:
  - name: "customer-database"
    source: "data-sources/customer-database.yaml"  # External reference
    enabled: true

enrichments:
  - id: "customer-lookup"
    lookup-config:
      lookup-dataset:
        data-source-ref: "customer-database"  # References external config
        query-ref: "getActiveCustomer"       # Named query from external config
```

**Key Classes**: `YamlDataSourceRef`, `YamlDataSource`, `YamlConfigurationLoader.processDataSourceReferences()`

## Development Workflows

### Essential Commands
```bash
# Interactive development (REQUIRED for rapid prototyping)
cd apex-playground && mvn spring-boot:run  # http://localhost:8081/playground

# Run external data-source demos (showcase APEX 2.1 features)
cd apex-demo
mvn exec:java -Dexec.mainClass="dev.mars.apex.demo.examples.SimplePostgreSQLLookupDemo"
mvn exec:java -Dexec.mainClass="dev.mars.apex.demo.lookup.ExternalDataSourceWorkingDemo"

# Bootstrap demos (complete end-to-end scenarios)
mvn exec:java -Dexec.mainClass="dev.mars.apex.demo.enrichment.OtcOptionsBootstrapDemo"

# Fast testing (10-15 seconds)
scripts/fast-demo-tests.bat  # Windows
scripts/fast-demo-tests.sh   # Unix
```

### Test Patterns
- **ALL tests** extend `@ExtendWith(ColoredTestOutputExtension.class)` for visual feedback
- **Standard setup**: `YamlConfigurationLoader`, `YamlRulesEngineService`, `EnrichmentService`
- **100% execution validation**: Always verify `"Processed: X out of X"` in logs
- **Test configuration naming**: `src/test/java/.../[TestClass].yaml` pattern

## YAML Configuration Patterns

### Modern External Reference Pattern
```yaml
# Separate business logic from infrastructure
metadata:
  type: "rule-config"  # Business logic file

data-source-refs:
  - name: "my-database"
    source: "data-sources/my-database.yaml"  # Infrastructure file
```

### Core YAML Structures
- **Rules**: `id`, `condition`, `message`, `severity` (ERROR/WARNING/INFO)
- **Enrichments**: `id`, `type: "lookup-enrichment"`, `lookup-config`
- **Rule Groups**: Collections with aggregated validation
- **Rule Chains**: 6 patterns including `sequential-dependency`, `conditional-chaining`

## Financial Services Domain Knowledge

### Primary Use Cases
- **OTC Derivatives Validation**: Multi-tier validation framework
- **Trade Settlement**: Post-trade processing with auto-repair workflows  
- **Regulatory Compliance**: MiFID II, EMIR, Dodd-Frank reporting
- **Currency Reference Data**: ISO currency codes with metadata

### Demo Categories (16+ Examples)
- **Bootstrap Demos**: Complete workflows (`OtcOptionsBootstrapDemo`, `CommoditySwapBootstrapDemo`)
- **External Data-Source Demos**: Modern architecture patterns
- **Lookup Patterns**: YAML datasets vs external database integration

## Critical Development Practices

### When Creating Tests
1. Always use `ColoredTestOutputExtension` for consistent output
2. Load configurations via `yamlLoader.loadFromFile("path/to/TestClass.yaml")`
3. Verify all enrichments execute: check logs for execution rates
4. Use real APEX services, not mocks: `EnrichmentService`, `LookupServiceRegistry`

### When Working with External Data Sources
- Business logic files reference external configs via `data-source-ref`
- Infrastructure configs in `data-sources/` directory with `type: "external-data-config"`
- Use `YamlConfigurationLoader.processDataSourceReferences()` for resolution

### Build & Validation
```bash
# Comprehensive build
mvn clean compile test-compile

# YAML validation via compiler
cd apex-compiler && mvn exec:java -Dexec.args="path/to/config.yaml"

# Test with reports
scripts/run-tests-with-reports.bat [module-name]
```

## Key Integration Points

### Spring Boot Integration
- **Playground**: 4-panel web interface with live preview
- **REST API**: Full OpenAPI/Swagger documentation
- **Health Monitoring**: Comprehensive system health checks at `/actuator/health`

### Database Integration
- **PostgreSQL**: Primary database for demos with connection pooling
- **H2**: Embedded database with custom parameter tuning
- **Connection patterns**: Environment-specific configs with `${DB_USERNAME}` placeholders

## Anti-Patterns to Avoid
- **Mixed configurations**: Don't embed infrastructure in business logic YAML
- **Missing test validation**: Always verify 100% execution rates in demo tests
- **Manual service setup**: Use established `DemoTestBase` patterns for consistency
- **Generic advice**: Focus on APEX-specific patterns, not general Spring Boot practices

## Getting Started for New AI Agents
1. **Start with Playground**: `cd apex-playground && mvn spring-boot:run`
2. **Run SimplePostgreSQLLookupDemo**: Demonstrates external data-source references
3. **Examine test patterns**: Look at `ColoredTestOutputExtension` usage in `apex-demo/src/test`
4. **Review YAML examples**: `apex-demo/src/test/resources` for configuration patterns
5. **Understand clean architecture**: External data-source reference system is the key differentiator

This system's strength is in **clean architecture** and **financial domain expertise** - leverage both when implementing new features.