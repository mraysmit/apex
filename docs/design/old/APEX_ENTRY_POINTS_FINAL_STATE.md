# APEX Entry Points - Final State and Architecture

**Document Date**: 2025-11-03  
**Status**: ✅ **ARCHITECTURE COMPLETE**  
**Priority**: INFORMATIONAL - Documents current entry point architecture and usage patterns

---

## Executive Summary

This document describes the final state of APEX entry point architecture after completing the rationalization effort. APEX now provides a clean, content-agnostic architecture with a universal entry point for processing any YAML configuration.

### Architectural Principles

1. **Content-Agnostic Design**: Services don't assume YAML structure - developers don't need to inspect YAML content
2. **Universal Processing**: One entry point handles all YAML content types (enrichments, rules, rule-groups, scenarios, pipelines)
3. **Business-Focused**: Developers focus on business logic, not YAML inspection
4. **Proper Encapsulation**: Internal services are not exposed as public APIs

### Key Achievement

**Before**: 11 different entry points requiring developers to know YAML content in advance  
**After**: 1 universal entry point (`RulesEngine`) that processes any YAML content

---

## Current Entry Point Architecture

### ✅ **RECOMMENDED ENTRY POINT** (Universal Pattern)

**Simplest Pattern** - One line for maximum simplicity:

```java
// ⭐ ONE LINE - For general YAML processing (rules, enrichments, pipelines, etc.)
RuleResult result = RulesEngine.fromFile("path/to/config.yaml").evaluate(inputData);

// ⭐ ONE LINE - For scenario-based processing
ScenarioExecutionResult result = RulesEngine.fromScenarioRegistry("path/to/config.yaml").evaluateScenario("scenario-id", inputData);
```

**Two-Line Pattern** - When you need to reuse the engine:

```java
// For general YAML processing (rules, enrichments, pipelines, etc.)
RulesEngine engine = RulesEngine.fromFile("path/to/config.yaml");
RuleResult result = engine.evaluate(inputData);

// For scenario-based processing
RulesEngine engine = RulesEngine.fromScenarioRegistry("path/to/config.yaml");
ScenarioExecutionResult result = engine.evaluateScenario("scenario-id", inputData);
```

**When to use each**:
- **One-line**: Single evaluation, maximum simplicity
- **Two-line**: Multiple evaluations with the same engine (reuse the engine instance)

**Key Benefits**:
- ✅ Works for ANY YAML content type (enrichments, rules, pipelines, scenarios)
- ✅ Simplest possible API (1-2 lines of code)
- ✅ Content-agnostic (no inspection needed)
- ✅ Industry-standard pattern (static factory methods + fluent API)
- ✅ Backward compatible (existing code still works)

---

### ⚙️ **ADVANCED PATTERN** (Rare Edge Cases Only)

**When to use**: Only when you need to inspect or modify the YAML configuration **before** creating the engine.

**Important**: This pattern is **more verbose** than even the old deprecated patterns. Only use it when absolutely necessary.

```java
// Load configuration explicitly
YamlConfigurationLoader loader = new YamlConfigurationLoader();
YamlRuleConfiguration yamlConfig = loader.loadFromFile("path/to/config.yaml");

// Inspect or modify config (this is the ONLY reason to use this pattern)
if (yamlConfig.getMetadata() != null) {
    System.out.println("Config version: " + yamlConfig.getMetadata().getVersion());
}
// Or modify config programmatically
yamlConfig.getRules().add(customRule);

// Create engine from modified config
RulesEngine engine = RulesEngine.fromYamlConfig(yamlConfig);
RuleResult result = engine.evaluate(inputData);
```

**Verbosity Comparison**:

| **Pattern** | **Lines of Code** | **When to Use** |
|-------------|-------------------|-----------------|
| **One-Line (Simplest)** | 1 line 🏆 | Single evaluation, maximum simplicity |
| **Two-Line (Primary)** | 2 lines ✅ | Multiple evaluations, engine reuse |
| **Old Deprecated** | 3 lines ❌ | Never (deprecated) |
| **Advanced** | 7+ lines ⚠️ | Only when you need config inspection/modification |

**Why the advanced pattern is verbose**: It exposes the internal loading mechanism, which is normally hidden by the static factory methods. This is intentional - the verbosity signals that you're doing something unusual that most developers don't need.

---

## Static Factory Methods

**Implementation Status**: ✅ **COMPLETE**

RulesEngine provides static factory methods for simplified usage. These are the **recommended entry points** for all APEX processing.

### Available Factory Methods

```java
// ⭐ SIMPLEST: One-line pattern (use this for single evaluations)
RuleResult result = RulesEngine.fromFile("path/to/config.yaml").evaluate(inputData);

// ⭐ PRIMARY: Two-line pattern (use this when reusing the engine)
RulesEngine engine = RulesEngine.fromFile("path/to/config.yaml");
RuleResult result = engine.evaluate(inputData);

// ⭐ SCENARIOS: One-line scenario pattern
ScenarioExecutionResult result = RulesEngine.fromScenarioRegistry("path/to/registry.yaml").evaluateScenario("scenario-id", inputData);

// ⭐ SCENARIOS: Two-line scenario pattern (when reusing)
RulesEngine engine = RulesEngine.fromScenarioRegistry("path/to/registry.yaml");
ScenarioExecutionResult result = engine.evaluateScenario("scenario-id", inputData);

// ⚙️ ADVANCED: From YAML configuration object (only when you need config inspection/modification)
YamlConfigurationLoader loader = new YamlConfigurationLoader();
YamlRuleConfiguration yamlConfig = loader.loadFromFile("path/to/config.yaml");
// ... inspect or modify yamlConfig ...
RulesEngine engine = RulesEngine.fromYamlConfig(yamlConfig);
RuleResult result = engine.evaluate(inputData);
```

### Why Static Factory Methods?

**The Problem with Old Patterns**:
- Required developers to instantiate service objects
- Required passing configuration multiple times
- Exposed internal implementation details
- Different services for different YAML content types

**The Solution**:
- **Single entry point**: One class (RulesEngine) for all YAML content
- **Minimal code**: 2 lines instead of 3-7 lines
- **Content-agnostic**: No need to inspect YAML before choosing a service
- **Industry standard**: Static factory methods are a well-known Java pattern

### Pattern Comparison

| **Aspect** | **Old Deprecated** | **New One-Line** | **New Two-Line** | **New Advanced** |
|------------|-------------------|------------------|------------------|------------------|
| **Lines of Code** | 3 lines | **1 line** 🏆 | **2 lines** ✅ | 7+ lines ⚠️ |
| **Ease of Use** | ⭐⭐ | **⭐⭐⭐⭐⭐** 🏆 | **⭐⭐⭐⭐⭐** ✅ | ⭐⭐ |
| **Engine Reuse** | ❌ | ❌ | **✅** | **✅** |
| **Content-Agnostic** | ❌ | **✅** | **✅** | **✅** |
| **Config Inspection** | ❌ | ❌ | ❌ | **✅** |
| **When to Use** | Never | **Single evaluation** | **Multiple evaluations** | Rare edge cases |

---

## Deprecated Entry Points

### ❌ **DEPRECATED** - Content-Aware Services

These services assumed developers knew YAML content in advance:

```java
// ❌ DEPRECATED - Assumes YAML contains only enrichments
@Deprecated(since = "3.0", forRemoval = true)
YamlEnrichmentProcessor enrichmentProcessor = new YamlEnrichmentProcessor(...);

// ❌ DEPRECATED - Assumes YAML contains only scenarios
@Deprecated(since = "3.0", forRemoval = true)
DataTypeScenarioService scenarioService = new DataTypeScenarioService();

// ❌ DEPRECATED - Assumes YAML contains only pipelines
@Deprecated(since = "3.0", forRemoval = true)
DataPipelineEngine pipelineEngine = new DataPipelineEngine();

// ❌ DEPRECATED - Factory methods replaced by static factory methods
@Deprecated(since = "3.0", forRemoval = true)
YamlRulesEngineService service = new YamlRulesEngineService();
RulesEngine engine = service.createRulesEngineFromFile("config.yaml");
```

**Why Deprecated**: These services violated the Open/Closed Principle by requiring developers to inspect YAML content before choosing the correct service.

### ✅ **REPLACEMENT** - Universal Pattern

```java
// ✅ NEW - Works for ANY YAML content
RulesEngine engine = RulesEngine.fromFile("config.yaml");
RuleResult result = engine.evaluate(inputData);
```

---

## Migration Examples

### Example 1: YamlEnrichmentProcessor → RulesEngine

**OLD (Deprecated)**:
```java
LookupServiceRegistry serviceRegistry = new LookupServiceRegistry();
ExpressionEvaluatorService evaluatorService = new ExpressionEvaluatorService();

YamlEnrichmentProcessor enrichmentProcessor = 
    new YamlEnrichmentProcessor(serviceRegistry, evaluatorService);

Object result = enrichmentProcessor.processEnrichments(
    config.getEnrichments(), 
    testData, 
    config
);
```

**NEW (Current)**:
```java
RulesEngine engine = RulesEngine.fromFile("path/to/enrichment-config.yaml");
RuleResult result = engine.evaluate(testData);

// Access enriched data from result
Object enrichedData = result.getEnrichedData();
```

### Example 2: DataTypeScenarioService → RulesEngine

**OLD (Deprecated)**:
```java
DataTypeScenarioService scenarioService = new DataTypeScenarioService();
scenarioService.loadScenarios("path/to/config.yaml");
ScenarioConfiguration scenario = scenarioService.getScenario("scenario-id");
Object result = scenarioService.processDataWithScenario(testData, scenario);
ScenarioExecutionResult scenarioResult = (ScenarioExecutionResult) result;
```

**NEW (Current)**:
```java
RulesEngine engine = RulesEngine.fromScenarioRegistry("path/to/config.yaml");
ScenarioExecutionResult scenarioResult = engine.evaluateScenario("scenario-id", testData);
```

### Example 3: YamlRulesEngineService Factory → Static Factory

**OLD (Deprecated)**:
```java
YamlRulesEngineService service = new YamlRulesEngineService();
RulesEngine engine = service.createRulesEngineFromFile("config.yaml");
RuleResult result = engine.evaluate(config, testData);
```

**NEW (Current)**:
```java
RulesEngine engine = RulesEngine.fromFile("config.yaml");
RuleResult result = engine.evaluate(testData);
```

---

## Entry Point Usage Statistics

### Current State (apex-demo tests)

| **Entry Point** | **Files Using** | **Status** |
|----------------|-----------------|------------|
| **RulesEngine.fromFile()** | 0 files | ✅ New pattern (migration in progress) |
| **RulesEngine.fromScenarioRegistry()** | 11 files | ✅ Current (scenario tests) |
| **YamlEnrichmentProcessor** | 0 files | ✅ Migrated away |
| **DataTypeScenarioService** | 0 files | ✅ Migrated away |
| **YamlRulesEngineService factories** | 65 files | ⚠️ Deprecated (still functional) |
| **Direct RulesEngine constructor** | 15 files | ⚠️ Advanced use cases only |

### Migration Progress

- ✅ **apex-demo**: All deprecated service usage eliminated
- ✅ **Scenario tests**: Migrated to RulesEngine.fromScenarioRegistry()
- ⚠️ **Factory method tests**: Still using deprecated YamlRulesEngineService (functional but deprecated)
- ✅ **apex-core**: Unit tests appropriately test deprecated classes

---

## Architecture Diagram

```
Developer
    ↓
Choose Entry Point
    ↓
    ├─→ ⭐ PRIMARY: RulesEngine.fromFile("config.yaml")
    │       ↓
    │   RulesEngine Instance
    │       ↓
    │   engine.evaluate(inputData)
    │       ↓
    │   Processes ANY YAML Content:
    │       ├─→ Enrichments
    │       ├─→ Rules
    │       ├─→ Rule Groups
    │       ├─→ Scenarios
    │       ├─→ Pipelines
    │       └─→ Mixed Content
    │           ↓
    │       RuleResult / ScenarioExecutionResult
    │
    ├─→ ⭐ SCENARIOS: RulesEngine.fromScenarioRegistry("registry.yaml")
    │       ↓
    │   engine.evaluateScenario("scenario-id", inputData)
    │       ↓
    │   ScenarioExecutionResult
    │
    └─→ ❌ DEPRECATED: Content-specific services
            (YamlEnrichmentProcessor, DataTypeScenarioService, etc.)
```

---

## Public API Services

### ✅ **CURRENT PUBLIC INTERFACES**

**1. RulesEngine** - ⭐ **PRIMARY ENTRY POINT**
- Static factory methods for simplified usage
- Universal YAML processing
- Handles all content types automatically

**2. RulesService** - API wrapper for individual rule evaluation
- Spring Bean configured
- Used in REST controllers
- Content-agnostic design

**3. GenericTransformerService** - Data transformation operations
- Spring Bean configured
- Used in REST controllers
- Works with TransformerRule objects

### ❌ **INTERNAL SERVICES** (Not exposed as public APIs)

- RuleEngineService - Low-level rule evaluation
- YamlEnrichmentProcessor - Internal enrichment processing
- SequentialYamlProcessor - Internal YAML processing

---

## Best Practices

### ✅ **DO THIS**

1. **Use the one-line pattern for single evaluations** (simplest, 1 line):
   ```java
   // 🏆 SIMPLEST - One line for maximum simplicity
   RuleResult result = RulesEngine.fromFile("config.yaml").evaluate(inputData);
   ```

2. **Use the two-line pattern when reusing the engine** (2 lines):
   ```java
   // ✅ EFFICIENT - Reuse the engine for multiple evaluations
   RulesEngine engine = RulesEngine.fromFile("config.yaml");
   RuleResult result1 = engine.evaluate(inputData1);
   RuleResult result2 = engine.evaluate(inputData2);
   ```

3. **Use scenario registry for scenario-based processing**:
   ```java
   // One-line scenario pattern
   ScenarioExecutionResult result = RulesEngine.fromScenarioRegistry("registry.yaml").evaluateScenario("scenario-id", data);

   // Two-line when reusing
   RulesEngine engine = RulesEngine.fromScenarioRegistry("registry.yaml");
   ScenarioExecutionResult result = engine.evaluateScenario("scenario-id", data);
   ```

4. **Trust the universal entry point** - don't inspect YAML content:
   ```java
   // ✅ CORRECT - Works for ANY YAML content (enrichments, rules, scenarios, pipelines)
   RuleResult result = RulesEngine.fromFile("config.yaml").evaluate(inputData);
   ```

### ❌ **DON'T DO THIS**

1. **Don't use content-specific services**:
   ```java
   // ❌ WRONG - Assumes YAML contains only enrichments
   YamlEnrichmentProcessor processor = new YamlEnrichmentProcessor(...);
   ```

2. **Don't inspect YAML to choose service**:
   ```java
   // ❌ WRONG - Developer shouldn't need to know YAML structure
   if (yamlContainsEnrichments) {
       useEnrichmentService();
   } else {
       useRulesEngine();
   }
   ```

3. **Don't use deprecated factory methods**:
   ```java
   // ❌ DEPRECATED - Use static factory methods instead
   YamlRulesEngineService service = new YamlRulesEngineService();
   RulesEngine engine = service.createRulesEngineFromFile("config.yaml");
   ```

4. **Don't use the advanced pattern unless you really need it**:
   ```java
   // ❌ UNNECESSARILY VERBOSE - Only use if you need config inspection/modification
   YamlConfigurationLoader loader = new YamlConfigurationLoader();
   YamlRuleConfiguration yamlConfig = loader.loadFromFile("config.yaml");
   RulesEngine engine = RulesEngine.fromYamlConfig(yamlConfig);

   // ✅ BETTER - Use the primary pattern instead
   RulesEngine engine = RulesEngine.fromFile("config.yaml");
   ```

### ⚙️ **WHEN TO USE THE ADVANCED PATTERN**

Only use the advanced pattern when you have a **specific need** to inspect or modify the configuration:

```java
// ✅ VALID USE CASE: Need to inspect config metadata
YamlConfigurationLoader loader = new YamlConfigurationLoader();
YamlRuleConfiguration yamlConfig = loader.loadFromFile("config.yaml");

if (yamlConfig.getMetadata().getVersion().equals("2.0")) {
    // Apply version-specific logic
}

RulesEngine engine = RulesEngine.fromYamlConfig(yamlConfig);
```

```java
// ✅ VALID USE CASE: Need to modify config programmatically
YamlConfigurationLoader loader = new YamlConfigurationLoader();
YamlRuleConfiguration yamlConfig = loader.loadFromFile("config.yaml");

// Add custom rule at runtime
yamlConfig.getRules().add(buildCustomRule());

RulesEngine engine = RulesEngine.fromYamlConfig(yamlConfig);
```

**Key Point**: If you're not inspecting or modifying the config, use the primary pattern. The advanced pattern's verbosity is intentional - it signals you're doing something unusual.

---

## Conclusion

### Architecture Status: ✅ **COMPLETE**

APEX now has a clean, content-agnostic architecture with:
- **1 universal entry point** (RulesEngine) instead of 11 different services
- **Static factory methods** for simplified usage (1-2 lines of code)
- **One-line pattern** for maximum simplicity (single evaluations)
- **Two-line pattern** for engine reuse (multiple evaluations)
- **Content-agnostic design** - no need to inspect YAML
- **Backward compatibility** - existing code still works

### Key Principles Established

1. **Content-Agnostic Design**: Services don't assume YAML structure
2. **Universal Entry Points**: One primary service handles all YAML content types
3. **Business-Focused Development**: Developers focus on business logic, not YAML inspection
4. **Proper Encapsulation**: Internal services are not exposed as public APIs
5. **Simplicity by Default**: Primary pattern is 2 lines; verbosity signals advanced usage

### The Verbosity Argument

**Why the advanced pattern is intentionally verbose**:

The advanced pattern (7+ lines) is **more verbose** than even the old deprecated patterns (3 lines). This is **intentional design**:

- **Verbosity signals complexity**: If you're writing more code, you're doing something unusual
- **Discourages misuse**: Most developers will choose the simpler 2-line pattern
- **Self-documenting**: The extra code makes it obvious you're inspecting/modifying config
- **Rare use case**: Config inspection/modification is needed in <1% of cases

**The right pattern for the right job**:

| **Use Case** | **Pattern** | **Lines** | **Frequency** |
|--------------|-------------|-----------|---------------|
| Single evaluation | One-line | 1 line 🏆 | Very common |
| Multiple evaluations (engine reuse) | Two-line | 2 lines ✅ | Common |
| Scenario-based processing | Scenario registry | 1-2 lines | Common |
| Config inspection/modification | Advanced | 7+ lines ⚠️ | <1% of cases |

**Bottom line**:
- **Default to one-line** for single evaluations
- **Use two-line** when you need to reuse the engine
- **Only use advanced** if you really need to inspect or modify the config

### Recommended Next Steps

1. ✅ **COMPLETE**: Core architecture rationalization
2. 🔄 **ONGOING**: Migrate remaining tests from deprecated factory methods to static factory methods
3. 📚 **DOCUMENTATION**: Update guides to reference RulesEngine as primary API
4. 🔮 **FUTURE**: Remove deprecated services in version 4.0

---

**Document Status**: FINAL - Reflects completed architecture rationalization as of 2025-11-03

