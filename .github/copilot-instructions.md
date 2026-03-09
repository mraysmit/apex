# APEX Rules Engine - AI Coding Agent Instructions

## Project Overview
APEX is a Java 21+ rules engine (v2.4) for financial services with YAML-driven configuration. Key differentiator: **external data-source reference system** that cleanly separates business logic from infrastructure configuration.

## Multi-Module Structure
- **`apex-core`**: Core engine, YAML processing, `RulesEngine` unified API, SpEL expression evaluation
- **`apex-demo`**: 16+ demos with financial services patterns—start here to understand usage
- **`apex-playground`**: Interactive Spring Boot web UI (port 8081) for rapid prototyping
- **`apex-rest-api`**: REST API with OpenAPI/Swagger (port 8080)
- **`apex-compiler`**: YAML validation tools
- **`apex-data-sync`**: Data synchronization with schema analysis and HTML report generation
- **`apex-yaml-manager`**: Enterprise YAML management with dependency analysis and refactoring tools

## RulesEngine API (APEX 3.0) - The Unified Entry Point
Always use `RulesEngine` for rule evaluation—it's the single entry point for all APEX processing:

```java
// One-line pattern (simplest, use for most cases)
RuleResult result = RulesEngine.fromFile("config.yaml").evaluate(data);

// Two-line pattern (reusable engine)
RulesEngine engine = RulesEngine.fromFile("config.yaml");
RuleResult result = engine.evaluate(data);
engine.shutdown();

// Scenario-based (classification-based routing)
RulesEngine engine = RulesEngine.fromScenarioRegistry("scenarios-registry.yaml");
ScenarioExecutionResult result = engine.evaluateWithClassification(trade);
```

**Key Classes**: `RulesEngine`, `RulesEngineConfiguration`, `YamlRuleConfiguration`, `RuleResult`

## External Data-Source Reference System (Clean Architecture)
The defining pattern—infrastructure configs live separately from business logic:

```yaml
# Business logic file (lean)
data-source-refs:
  - name: "customer-database"
    source: "data-sources/customer-database.yaml"  # External reference
enrichments:
  - id: "customer-lookup"
    lookup-config:
      lookup-dataset:
        data-source-ref: "customer-database"
        query-ref: "getActiveCustomer"
```

**Key Classes**: `YamlDataSourceRef`, `YamlDataSource`, `YamlConfigurationLoader.processDataSourceReferences()`

## Component Architecture (v2.2) - Reusable Configuration Groups
Components group multiple YAML files into reusable units for scenario processing:

```yaml
# components/validation-component.yaml
metadata:
  id: "comprehensive-validation"
  type: "component"  # Key: type must be "component"

rule-configurations:
  - file: "rules/basic-validation.yaml"
    failure-policy: "terminate"
  - file: "rules/compliance-rules.yaml"
    failure-policy: "continue-with-warnings"

enrichment-refs:
  - file: "enrichments/market-data.yaml"
    execution-order: 10

component-refs:  # Nested components (max depth 5)
  - file: "components/sub-component.yaml"
    execution-order: 20
```

**Nesting Depth Rules**: Levels 1-2 OK, 3-5 WARNING, 6+ ERROR (fails to load)
**Execution Order**: Explicit `execution-order` or document order (APEX default)
**Key Classes**: `ComponentLoader`, `YamlRuleConfiguration` with `type: "component"`

## Error Recovery System (Critical Feature)
Severity-based error handling with configurable recovery strategies:

### Severity Levels & Default Behavior
| Severity | Default Recovery | Default Strategy | Use Case |
|----------|------------------|------------------|----------|
| CRITICAL | Disabled | FAIL_FAST | System failures, data corruption |
| ERROR | Disabled | FAIL_FAST | Business logic failures, missing required data |
| WARNING | Enabled | CONTINUE_WITH_DEFAULT | Non-critical issues, optional validations |
| INFO | Enabled | CONTINUE_WITH_DEFAULT | Informational, audit trails |

### Recovery Strategies
- **FAIL_FAST**: Immediately fail, no recovery
- **CONTINUE_WITH_DEFAULT**: Log error, continue with safe defaults
- **RETRY_WITH_SAFE_EXPRESSION**: Retry with simplified expression
- **SKIP_RULE**: Skip failed rule, continue processing

### YAML Configuration
```yaml
error-recovery:
  enabled: true
  log-recovery-attempts: true
  default-strategy: "CONTINUE_WITH_DEFAULT"
  
  severity-policies:
    CRITICAL:
      recovery-enabled: false
      strategy: "FAIL_FAST"
    WARNING:
      recovery-enabled: true
      strategy: "CONTINUE_WITH_DEFAULT"
      max-retries: 1
      retry-delay: 100
```

### Critical Concept: ResultType vs Severity
- **ResultType** (system-level): MATCH, NO_MATCH, ERROR, ENRICHMENT_FAILURE
- **Severity** (business-level): CRITICAL, ERROR, WARNING, INFO
- When condition=TRUE → ResultType=MATCH (severity irrelevant)
- When condition=FALSE + ERROR severity + recovery disabled → ResultType=ERROR (fail-fast)

**Key Classes**: `ErrorRecoveryConfig`, `SeverityRecoveryPolicy`, `ErrorRecoveryService`, `SeverityConstants`

## Database Testing with Testcontainers 2.0
Use `GenericContainer` (not deprecated `PostgreSQLContainer`) for database tests:

```java
@Testcontainers
class MyDatabaseTest {
    private static final DockerImageName POSTGRES_IMAGE = 
        DockerImageName.parse(TestContainerImages.POSTGRES);  // "postgres:15.13-alpine3.20"
    
    @Container
    static GenericContainer<?> postgres = new GenericContainer<>(POSTGRES_IMAGE)
        .withEnv("POSTGRES_DB", "apex_test")
        .withEnv("POSTGRES_USER", "apex_user")
        .withEnv("POSTGRES_PASSWORD", "apex_pass")
        .withExposedPorts(5432)
        .waitingFor(Wait.forListeningPort());
    
    // Manual JDBC URL construction (no convenience methods)
    private static String jdbcUrl() {
        return "jdbc:postgresql://" + postgres.getHost() + ":" 
            + postgres.getMappedPort(5432) + "/apex_test";
    }
}
```

**Key Differences from PostgreSQLContainer**:
- Use `.withEnv()` not `.withDatabaseName()`
- Manual JDBC URL construction (no `getJdbcUrl()`)
- First URL param uses `?` not `&` (clean URL)
- Add retry logic in setup—`Wait.forListeningPort()` may return before PostgreSQL ready

**Docker Images** (defined in parent `pom.xml`):
- PostgreSQL: `postgres:15.13-alpine3.20`
- Vault: `hashicorp/vault:1.20.0`
- Redis: `redis:6-alpine`

## Development Workflows

### Essential Commands
```bash
# Interactive development (start here)
cd apex-playground && mvn spring-boot:run  # http://localhost:8081/playground

# Run demos (apex-demo module)
cd apex-demo
mvn exec:java -Dexec.mainClass="dev.mars.apex.demo.examples.SimplePostgreSQLLookupDemo"
mvn exec:java -Dexec.mainClass="dev.mars.apex.demo.enrichment.OtcOptionsBootstrapDemo"

# Build all modules
mvn clean compile test-compile

# YAML validation
cd apex-compiler && mvn exec:java -Dexec.args="path/to/config.yaml"
```

### Test Patterns
All tests **must** extend `DemoTestBase` or use `@ExtendWith(ColoredTestOutputExtension.class)`:

```java
@ExtendWith(ColoredTestOutputExtension.class)
class MyDemoTest extends DemoTestBase {
    @Test
    void testSomething() {
        YamlRuleConfiguration config = yamlLoader.loadFromFile("path/to/MyDemoTest.yaml");
        RulesEngine engine = RulesEngine.fromYamlConfig(config);
        RuleResult result = engine.evaluate(testData);
        // Verify 100% execution in logs: "Processed: X out of X"
    }
}
```

- **Test config naming**: `src/test/resources/.../[TestClass].yaml`
- **Real services only**: Use `EnrichmentService`, `LookupServiceRegistry`—avoid mocks
- **DemoTestBase**: Provides `yamlLoader`, `serviceRegistry`, `expressionEvaluator`, handles cache cleanup

## YAML Configuration Patterns

### Core Structures
- **Rules**: `id`, `condition` (SpEL), `message`, `severity` (ERROR/WARNING/INFO)
- **Enrichments**: `id`, `type: "lookup-enrichment"`, `lookup-config`
- **Rule Groups**: Collections with `aggregated-validation-result`
- **Rule Chains**: 6 patterns including `sequential-dependency`, `conditional-chaining`
- **Pipelines**: `read-schema`, `extract`, `transform`, `load`
- **Scenarios**: Multi-stage processing with classification-based routing
- **Components**: Grouping containers with `rule-configurations`, `enrichment-refs`, `component-refs`

### Metadata Types
```yaml
metadata:
  type: "rule-config"           # Business logic
  type: "external-data-config"  # Infrastructure (in data-sources/ directory)
  type: "scenario-registry"     # Scenario definitions
  type: "component"             # Reusable configuration groups
```

## Financial Services Domain
Primary use cases: OTC derivatives validation, trade settlement, regulatory compliance (MiFID II, EMIR, Dodd-Frank), risk assessment.

## apex-demo Module - Learning by Example
The `apex-demo` module is the **canonical reference** for APEX patterns. Study these directories:

### Demo Categories (by directory)
| Directory | Purpose | Key Tests |
|-----------|---------|-----------|
| `basic/` | Rule groups, AND/OR logic, rule-refs | `BasicYamlRuleGroupProcessingATest` |
| `lookup/` | All data source types, inline/database/REST | `SimpleFieldLookupDemoTest`, `PostgreSQLSimpleLookupTest` |
| `enrichment/` | Field enrichment, nested targets | `ComprehensiveFinancialSettlementDemoTest` |
| `scenario/` | Multi-stage processing, components | `ComponentScenarioTest`, `BasicStageConfigurationTest` |
| `errorhandling/` | Failure policies, severity handling | `SimpleErrorHandlingTest`, `SimpleFailurePolicyTerminateTest` |
| `severity/` | Severity levels, aggregation | `SeverityComprehensiveTest`, `SeverityMixedRulesTest` |
| `datasources/` | All 6 data source types | See `datasources/README.md` |
| `categories/` | Enterprise governance, metadata inheritance | `CategoryExamplesValidationTest` |
| `conditional/` | Ternary, rule-based routing, waterfall | `UltraSimpleTernaryTest`, `WaterfallPatternTest` |
| `rulegroups/` | AND/OR, stop-on-first-failure, cross-file | `CrossFileRuleGroupReferenceTest` |
| `sequencing/` | Document order processing, refs ordering | `SequentialProcessingIntegrationTest` |
| `transformation/` | Conditional transformations, nested | `ComprehensiveConditionalTransformationTest` |

### Data Source Types (in `datasources/`)
```
datasources/
├── inline/      # Small static data (<100 records) - embedded in YAML
├── database/    # Large dynamic data - H2, PostgreSQL
├── filesystem/  # CSV, JSON, XML files
└── restapi/     # Real-time external APIs
```

### Conditional Logic Patterns (in `conditional/`)
4 progressive complexity levels demonstrated:
1. **Ultra Simple Ternary**: Pure SpEL with nested `?:` operators—no rules needed
2. **Rule OR Logic**: Rules → Rule Group (OR) → Conditional Enrichments
3. **Rule Result References**: `#ruleResults['rule-id']` drives conditional processing
4. **Complex Business Logic**: Real FX transaction routing with multiple conditions

### Rule Group Patterns (in `rulegroups/`)
```yaml
# AND group with stop-on-first-failure (short-circuit evaluation)
rule-groups:
  - id: "and-group"
    operator: "AND"
    stop-on-first-failure: true  # Stops on first false rule
    rule-ids: ["rule-1", "rule-2"]

# Cross-file references
rule-groups:
  - id: "composite"
    rule-group-references: ["base_validation"]  # From another YAML file
```

### Critical Test Pattern: Validation Checklist
Every demo test follows this validation pattern (from `SimpleFieldLookupDemoTest`):
```java
/**
 * CRITICAL VALIDATION CHECKLIST APPLIED:
 * 1. Count enrichments in YAML - X enrichments expected
 * 2. Verify log shows "Processed: X out of X" - Must be 100% execution rate
 * 3. Check EVERY enrichment condition - Test data triggers conditions
 * 4. Validate EVERY business calculation - Test actual logic
 * 5. Assert ALL enrichment results - Every field mapping has assertEquals
 */
```

### Scenario Testing Pattern (from `ComponentScenarioTest`)
```java
// 1. Create test data with meaningful business values
Map<String, Object> tradeData = createValidTradeData();

// 2. Load scenario registry
RulesEngine engine = RulesEngine.fromScenarioRegistry("path/to/registry.yaml");

// 3. Execute scenario (component expands automatically)
ScenarioExecutionResult result = engine.evaluateScenario("scenario-id", tradeData);

// 4. Validate stage execution
assertTrue(result.isSuccessful());
assertFalse(result.getStageResults().isEmpty());
```

### YAML File Naming Convention
Test YAML files follow strict naming: `[TestClassName].yaml` or `[TestClassName]-[variant].yaml`
```
SimpleFieldLookupDemoTest.java      → SimpleFieldLookupDemoTest.yaml
ComponentScenarioTest.java          → ComponentScenarioTest-registry.yaml
                                    → ComponentScenarioTest-simple-component-scenario.yaml
```

## Critical Practices

### When Creating New Features
1. Check `apex-demo` for existing patterns before implementing
2. Use `RulesEngine.fromFile()` or `RulesEngine.fromYamlConfig()`—never instantiate directly
3. Separate infrastructure configs into `data-sources/` directory
4. Add tests with `ColoredTestOutputExtension` for visual feedback

### When Editing YAML Classes
Key YAML model classes in `apex-core/src/main/java/dev/mars/apex/core/config/yaml/`:
- `YamlRuleConfiguration` - Root configuration
- `YamlRule`, `YamlRuleGroup`, `YamlRuleChain` - Rule structures
- `YamlEnrichment`, `YamlEnrichmentGroup` - Enrichment structures
- `YamlDataSource`, `YamlDataSourceRef` - Data source patterns

## Anti-Patterns to Avoid
- ❌ Don't embed infrastructure in business logic YAML
- ❌ Don't use mocks in demo tests—use real APEX services
- ❌ Don't skip `DemoTestBase` patterns for test setup
- ❌ Don't instantiate `RulesEngine` constructor directly—use static factory methods

## Getting Started Checklist
1. Run playground: `cd apex-playground && mvn spring-boot:run`
2. Run a demo: `SimplePostgreSQLLookupDemo` shows external data-source references
3. Study `DemoTestBase` and `ColoredTestOutputExtension` in `apex-demo/src/test`
4. Review YAML examples in `apex-demo/src/test/resources/examples/`