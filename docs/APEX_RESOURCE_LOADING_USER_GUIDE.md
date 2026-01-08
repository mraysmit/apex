# APEX Unified Resource Loading User Guide

## Introduction

APEX provides a flexible, unified system for loading configuration files from multiple sources: filesystem paths, classpath resources (including JAR files), and input streams. This enables:

- **JAR-packaged applications** to work seamlessly with embedded configurations
- **Test code** to use classpath resources without absolute paths
- **Production deployments** to use external configuration directories
- **Name-based resolution** for logical configuration references

This guide covers how to configure and use APEX's resource loading capabilities.

### Key Concepts

- **Scenario Registry** (`type: "scenario-registry"`) - A YAML file that lists multiple scenarios, their configuration files, and optional routing rules. This is the primary entry point for loading a collection of related scenarios.
- **Scenario** (`type: "scenario"`) - A YAML file defining validation rules, processing stages, and business logic for a specific use case.
- **Component** (`type: "component"`) - A reusable configuration unit that can be referenced by scenarios to share common rules, enrichments, or processing logic.
- **Search Paths** - Directories (filesystem) or prefixes (classpath) where APEX looks for referenced configuration files.

---

## Table of Contents

1. [Quick Start](#quick-start)
2. [Loading Strategies](#loading-strategies)
3. [Search Paths Configuration](#search-paths-configuration)
4. [RulesEngine Builder API](#rulesengine-builder-api)
5. [Scenario Registry Loading](#scenario-registry-loading)
6. [Component Loading](#component-loading)
7. [ResourceResolver API](#resourceresolver-api)
8. [ConfigurationContext](#configurationcontext)
9. [Environment Configuration](#environment-configuration)
10. [Best Practices](#best-practices)
11. [Troubleshooting](#troubleshooting)

---

## Quick Start

### Loading a Scenario Registry from Classpath (Recommended for JAR-packaged apps)

```java
// Load a scenario registry from classpath - returns RulesEngine with all scenarios
RulesEngine engine = RulesEngine.fromScenarioRegistry("config/scenario-registry.yaml");

// Or use the Builder API with search paths
RulesEngine engine = RulesEngine.builder()
    .addClasspathPrefix("apex/config/")
    .fromScenarioRegistry("trading-scenario-registry.yaml")
    .build();
```

### Loading a Scenario Registry from Filesystem

```java
// Absolute path to scenario registry
RulesEngine engine = RulesEngine.fromScenarioRegistry("/etc/apex/scenario-registry.yaml");

// With search paths for resolving scenario files
RulesEngine engine = RulesEngine.builder()
    .addSearchPath("/etc/apex/configs")
    .addSearchPath("./configs")
    .fromScenarioRegistry("trading-scenario-registry.yaml")
    .build();
```

---

## Loading Strategies

APEX supports four resolution strategies:

| Strategy | Description | Use Case |
|----------|-------------|----------|
| `CLASSPATH_FIRST` | Try classpath first, then filesystem | **Default** - JAR apps with optional overrides |
| `FILESYSTEM_FIRST` | Try filesystem first, then classpath | Override embedded configs with external files |
| `CLASSPATH_ONLY` | Only load from classpath | Strict JAR-packaged deployments |
| `FILESYSTEM_ONLY` | Only load from filesystem | Legacy compatibility, external configs only |

### Setting the Strategy

```java
ResourceResolver resolver = ResourceResolver.builder()
    .withStrategy(ResolutionStrategy.FILESYSTEM_FIRST)
    .build();

RulesEngine engine = RulesEngine.builder()
    .withResourceResolver(resolver)
    .fromScenarioRegistry("config/trading-scenario-registry.yaml")
    .build();
```

---

## Search Paths Configuration

Search paths define where APEX looks for configuration files when a relative path is specified. **The primary mechanism is YAML-based configuration within the scenario registry file itself** - this handles ~95% of runtime resource loading.

### YAML Configuration (Primary - Scenario Registry Level)

Each scenario registry defines its own search paths for resolving the scenario files it references. This is the standard approach:

```yaml
# trading-scenario-registry.yaml
metadata:
  id: trading-scenarios
  name: Trading Scenario Registry
  type: scenario-registry
  version: "1.0"

# Search paths - the primary mechanism for runtime resource resolution
search-paths:
  filesystem:
    - "/etc/apex/trading"                # Production configs
    - "${APEX_CONFIG_DIR}/trading"       # Environment variable expansion
    - "./configs/trading"                # Local development
  classpath:
    - "trading/scenarios/"               # JAR-embedded configs
    - "META-INF/apex/trading/"           # Standard JAR convention

# Scenarios reference config files resolved via search-paths above
scenarios:
  - scenario-id: "equity-trade"
    name: "Equity Trade Validation"
    config-file: "equity-trade-scenario.yaml"
    enabled: true
```

### Programmatic Configuration (Supplemental)

For cases requiring dynamic path configuration (rarely needed):

#### Filesystem Search Paths

```java
// Add supplemental filesystem paths programmatically
RulesEngine.builder()
    .addSearchPath("/etc/apex/configs")      // System-wide configs
    .addSearchPath("/opt/myapp/configs")     // Application configs
    .addSearchPath("./configs")              // Relative to current directory
    .build();
```

#### Classpath Prefixes

```java
// Add supplemental classpath prefixes programmatically
RulesEngine.builder()
    .addClasspathPrefix("apex/")             // Look in apex/ directory
    .addClasspathPrefix("META-INF/apex/")    // META-INF convention
    .addClasspathPrefix("config/")           // Generic config directory
    .build();
```

### Resolution Order

When resolving a scenario's `config-file` reference (e.g., `config-file: "equity-trade-scenario.yaml"`):

1. **Absolute path** - Use directly if path starts with `/` or `C:\`
2. **Explicit classpath** - Use directly if path starts with `classpath:`
3. **Scenario registry filesystem paths** - Search the registry's `search-paths.filesystem` in order
4. **Scenario registry classpath prefixes** - Search the registry's `search-paths.classpath` in order
5. **Programmatic filesystem paths** - Search programmatically added paths
6. **Programmatic classpath prefixes** - Search programmatically added prefixes
7. **Relative to scenario registry** - Resolve relative to the scenario registry file's location

---

## RulesEngine Builder API

The Builder API provides a fluent interface for configuring and creating `RulesEngine` instances.

### Basic Usage

```java
RulesEngine engine = RulesEngine.builder()
    .addSearchPath("/etc/apex/configs")
    .addClasspathPrefix("apex/")
    .fromScenarioRegistry("trading-scenario-registry.yaml")
    .build();
```

### Builder Methods

| Method | Description |
|--------|-------------|
| `addSearchPath(String)` | Add a filesystem search path for resolving configuration files |
| `addClasspathPrefix(String)` | Add a classpath prefix for resolving configuration files |
| `withContext(String, Object)` | Add a context variable for SpEL expressions |
| `withResourceResolver(ResourceResolver)` | Use a custom resource resolver |
| `fromScenarioRegistry(String)` | Specify the scenario registry file to load (loads all scenarios defined in it) |
| `fromFile(String)` | Load a single configuration file (not a scenario registry) |
| `build()` | Create the RulesEngine instance |

### With Context Variables

```java
RulesEngine engine = RulesEngine.builder()
    .addClasspathPrefix("apex/")
    .withContext("environment", "production")
    .withContext("region", "us-east-1")
    .fromScenarioRegistry("config/trading-scenario-registry.yaml")
    .build();
```

### With Custom ResourceResolver

```java
ResourceResolver resolver = ResourceResolver.builder()
    .withStrategy(ResolutionStrategy.FILESYSTEM_FIRST)
    .addSearchPath("/etc/apex")
    .addClasspathPrefix("fallback/")
    .build();

RulesEngine engine = RulesEngine.builder()
    .withResourceResolver(resolver)
    .fromScenarioRegistry("trading-scenario-registry.yaml")
    .build();
```

---

## Scenario Registry Loading

A **scenario registry** (`type: "scenario-registry"`) is a YAML file that serves as the entry point for loading a collection of related scenarios. It lists scenarios by ID and references their configuration files.

At runtime, APEX resolves each scenario's `config-file` reference using the search paths defined **within the scenario registry YAML itself**. This is the primary mechanism for resource loading (~95% of use cases).

### YAML Configuration (Primary Approach)

The scenario registry YAML file defines where to find the scenario configuration files it references:

```yaml
# trading-scenario-registry.yaml
metadata:
  id: trading-scenarios
  name: Trading Scenario Registry
  type: scenario-registry
  version: "1.0"

# Search paths - APEX uses these at runtime to resolve config-file references
search-paths:
  filesystem:
    - "/etc/apex/trading/scenarios"        # Production configs
    - "${APEX_CONFIG_DIR}/trading"         # Environment-specific
    - "./configs/trading"                  # Local development
  classpath:
    - "trading/scenarios/"                 # JAR-embedded scenarios
    - "META-INF/apex/trading/"             # Standard JAR location

# Scenarios - config-file references resolved using search-paths above
scenarios:
  - scenario-id: "equity-trade"
    name: "Equity Trade Validation"
    config-file: "equity-trade-scenario.yaml"   # Resolved at runtime via search-paths
    enabled: true
    
  - scenario-id: "fx-trade"
    name: "FX Trade Validation"  
    config-file: "fx-trade-scenario.yaml"       # Resolved at runtime via search-paths
    enabled: true
    
  - scenario-id: "bond-trade"
    name: "Bond Trade Validation"
    config-file: "bonds/bond-trade-scenario.yaml"  # Subdirectory path
    enabled: true

routing:
  strategy: "classification-based"
  default-scenario: "equity-trade"
```

**How runtime resolution works:**

When `ScenarioRegistryLoader` processes the scenario registry YAML, it performs the following steps for each scenario entry:

1. **Parse the scenario registry** - APEX reads the YAML file and extracts:
   - The `search-paths.filesystem` list (e.g., `["/etc/apex/trading/scenarios", "${APEX_CONFIG_DIR}/trading", "./configs/trading"]`)
   - The `search-paths.classpath` list (e.g., `["trading/scenarios/", "META-INF/apex/trading/"]`)
   - The `scenarios` list with each scenario's `config-file` reference

2. **Expand environment variables** - Any `${VAR}` placeholders in paths are resolved:
   - `${APEX_CONFIG_DIR}` → `/opt/apex` (from environment)
   - `${user.home}` → `/home/trader` (from system properties)

3. **Resolve each scenario's config-file** - For a scenario with `config-file: "equity-trade-scenario.yaml"`:

   **Filesystem search (in order):**
   ```
   /etc/apex/trading/scenarios/equity-trade-scenario.yaml  → Check exists? No
   /opt/apex/trading/equity-trade-scenario.yaml            → Check exists? No  
   ./configs/trading/equity-trade-scenario.yaml            → Check exists? YES ✓
   ```
   
   **If not found on filesystem, classpath search:**
   ```
   classpath: trading/scenarios/equity-trade-scenario.yaml → Check exists? ...
   classpath: META-INF/apex/trading/equity-trade-scenario.yaml → Check exists? ...
   ```

4. **First match wins** - Resolution stops at the first location where the file exists. The scenario configuration is loaded from that path.

5. **Fallback to relative path** - If no search path matches, APEX tries resolving relative to the scenario registry file's own location.

6. **Load and parse scenario** - The resolved YAML file is loaded and parsed into a `ScenarioConfiguration` object, which is then registered with its `scenario-id`.

**Example resolution trace:**

For the scenario registry above with `config-file: "bonds/bond-trade-scenario.yaml"`:
```
Resolving: bonds/bond-trade-scenario.yaml
  Trying: /etc/apex/trading/scenarios/bonds/bond-trade-scenario.yaml → NOT FOUND
  Trying: /opt/apex/trading/bonds/bond-trade-scenario.yaml → NOT FOUND
  Trying: ./configs/trading/bonds/bond-trade-scenario.yaml → FOUND
  Loading scenario from: ./configs/trading/bonds/bond-trade-scenario.yaml
  Registered scenario: bond-trade
```

### From Classpath (Programmatic)

For JAR-packaged applications loading the scenario registry itself from classpath:

```java
ScenarioRegistryLoader loader = new ScenarioRegistryLoader();

// Load scenario registry from classpath
// The registry's internal search-paths handle scenario file resolution
Map<String, ScenarioConfiguration> scenarios = 
    loader.loadRegistryFromClasspath("config/trading-scenario-registry.yaml");
```

### From Filesystem (Programmatic)

For applications loading the scenario registry from an external filesystem location:

```java
ScenarioRegistryLoader loader = new ScenarioRegistryLoader();

// Load scenario registry from filesystem path
// The registry's internal search-paths handle scenario file resolution
Map<String, ScenarioConfiguration> scenarios = 
    loader.loadRegistry("/etc/apex/trading-scenario-registry.yaml");
```

### Supplemental Programmatic Search Paths

In rare cases, you may need to add search paths programmatically (e.g., dynamically determined paths). These supplement the YAML-defined search paths:

```java
ScenarioRegistryLoader loader = new ScenarioRegistryLoader();

// Add supplemental search paths (in addition to YAML-defined paths)
loader.addSearchPath("/dynamic/path/from/config");
loader.addClasspathPrefix("dynamic/prefix/");

// Load - combines programmatic paths with YAML search-paths
Map<String, ScenarioConfiguration> scenarios = 
    loader.loadRegistry("trading-scenario-registry.yaml");
```

**Resolution order with combined paths:**
1. YAML-defined `search-paths.filesystem` (highest priority)
2. YAML-defined `search-paths.classpath`
3. Programmatic filesystem paths
4. Programmatic classpath prefixes
5. Relative to scenario registry file location (fallback)

---

## Component Loading

Components can be loaded from classpath or filesystem with full support for nested references.

### From Classpath

```java
ComponentLoader loader = new ComponentLoader();

// Load from classpath
YamlComponent component = loader.loadComponentFromClasspath("components/validation.yaml");

// With explicit classpath base
YamlComponent component = loader.loadComponent("validation.yaml", "components/");
```

### From Stream

```java
try (InputStream is = getClass().getResourceAsStream("/components/main.yaml")) {
    YamlComponent component = loader.loadComponent(is, "components/");
}
```

### Nested Component Resolution

When components reference other components via `file:` attribute, the loader automatically tracks the loading context:

```yaml
# parent-component.yaml
component:
  id: "parent"
  name: "Parent Component"
  
  component-refs:
    - file: "child-component.yaml"    # Resolved relative to parent's location
      execution-order: 1
```

---

## ResourceResolver API

The `ResourceResolver` provides unified resource resolution across filesystem and classpath.

### Creating a Resolver

```java
// Using builder
ResourceResolver resolver = ResourceResolver.builder()
    .withStrategy(ResolutionStrategy.CLASSPATH_FIRST)
    .addSearchPath("/etc/apex")
    .addClasspathPrefix("apex/")
    .build();

// Or default constructor
ResourceResolver resolver = new ResourceResolver();
resolver.addSearchPath("/etc/apex");
resolver.addClasspathPrefix("apex/");
```

### Resolving Resources

```java
// Resolve to InputStream
try (InputStream is = resolver.resolve("config.yaml")) {
    // Process the input stream
}

// Resolve with base path
try (InputStream is = resolver.resolve("config.yaml", "/etc/apex/")) {
    // Process
}

// Check existence
if (resolver.exists("config.yaml")) {
    // Resource exists
}

// Explicit resolution methods
InputStream is = resolver.resolveFromClasspath("apex/config.yaml");
InputStream is = resolver.resolveFromFilesystem("/etc/apex/config.yaml");
```

### Path Utilities

```java
// Resolve relative path
String resolved = resolver.resolveRelativePath("../common/rules.yaml", "/etc/apex/trading/");
// Result: "/etc/apex/common/rules.yaml"

// Get classpath base
String base = resolver.getClasspathBase("apex/scenarios/trading.yaml");
// Result: "apex/scenarios/"
```

---

## ConfigurationContext

The `ConfigurationContext` provides a central registry for name-based configuration lookup.

### Creating a Context

```java
ConfigurationContext context = ConfigurationContext.builder()
    .withResourceResolver(resolver)
    .addSearchPath("/etc/apex/configs")
    .addClasspathPrefix("apex/")
    .build();
```

### Registering Configurations

```java
// Register by name
context.registerConfiguration("trade-validation", ruleConfig);
context.registerDataSource("customer-db", dataSource);
context.registerScenario("equity-trade", scenario);
context.registerComponent("validation-component", component);
```

### Looking Up Configurations

```java
// Lookup by name
YamlRuleConfiguration config = context.getConfiguration("trade-validation");
YamlDataSource dataSource = context.getDataSource("customer-db");
ScenarioConfiguration scenario = context.getScenario("equity-trade");
ComponentConfiguration component = context.getComponent("validation-component");

// Check existence
if (context.containsConfiguration("trade-validation")) {
    // Configuration exists
}

// List all registered names
Set<String> configNames = context.getConfigurationNames();
```

### Bulk Loading

```java
// Load all YAML files from search paths
int loaded = context.loadAllFromSearchPaths();

// Load from specific classpath prefix
int loaded = context.loadAllFromClasspath("apex/configs/");
```

---

## Environment Configuration

APEX search paths can be configured via system properties and environment variables.

### System Properties

```bash
# Set via command line
java -Dapex.config.searchPaths=/etc/apex,/opt/myapp/configs \
     -Dapex.config.classpathPrefixes=apex/,META-INF/apex/ \
     -jar myapp.jar
```

### Environment Variables

```bash
# Linux/macOS
export APEX_CONFIG_SEARCH_PATHS=/etc/apex:/opt/myapp/configs
export APEX_CONFIG_CLASSPATH_PREFIXES=apex/:META-INF/apex/

# Windows
set APEX_CONFIG_SEARCH_PATHS=C:\apex\configs;D:\myapp\configs
set APEX_CONFIG_CLASSPATH_PREFIXES=apex/;META-INF/apex/
```

### Configuration Precedence

When multiple configuration sources are present, they are applied in this order (highest priority first):

1. **Scenario-registry-level** - `search-paths` section in the scenario registry YAML file
2. **Programmatic** - Paths added via Builder or Loader APIs
3. **System Properties** - `-Dapex.config.searchPaths=...`
4. **Environment Variables** - `APEX_CONFIG_SEARCH_PATHS=...`
5. **Built-in Defaults** - Relative to source file location

### Environment Variable Expansion

Search paths in YAML support `${VAR}` syntax:

```yaml
search-paths:
  filesystem:
    - "${APEX_HOME}/configs"           # From env var
    - "${user.home}/.apex/configs"     # From system property
    - "/etc/apex/configs"              # Literal path
```

---

## Best Practices

### 1. Use Classpath for Embedded Configurations

For applications packaged as JARs, embed configurations in the classpath:

```
src/main/resources/
  apex/
    trading-scenario-registry.yaml       # Scenario registry
    scenarios/
      trade-validation-scenario.yaml     # Individual scenario configs
    rules/
      amount-rules.yaml
```

```java
RulesEngine engine = RulesEngine.fromScenarioRegistry("apex/trading-scenario-registry.yaml");
```

### 2. Use External Paths for Environment-Specific Configs

```java
RulesEngine engine = RulesEngine.builder()
    .addSearchPath("/etc/apex/configs")  // External overrides
    .addClasspathPrefix("apex/")         // Embedded defaults
    .fromScenarioRegistry("trading-scenario-registry.yaml")
    .build();
```

### 3. Use Scenario-Registry-Level Search Paths for Modularity

Each domain/team can have its own scenario registry with specific search paths:

```yaml
# trading-scenario-registry.yaml
metadata:
  type: scenario-registry
  id: trading-scenarios
search-paths:
  filesystem:
    - "/etc/apex/trading"
  classpath:
    - "trading/"
scenarios:
  - scenario-id: equity-trade
    config-file: equity-trade-scenario.yaml
```

```yaml
# risk-scenario-registry.yaml
metadata:
  type: scenario-registry
  id: risk-scenarios
search-paths:
  filesystem:
    - "/etc/apex/risk"
  classpath:
    - "risk/"
scenarios:
  - scenario-id: risk-assessment
    config-file: risk-assessment-scenario.yaml
```

### 4. Use Environment Variables for Deployment Flexibility

```yaml
search-paths:
  filesystem:
    - "${APEX_ENV_CONFIG}/scenarios"    # /prod/apex vs /dev/apex
```

### 5. Consistent Path Separators

Use forward slashes (`/`) in classpath paths for cross-platform compatibility:

```java
// Good - works on all platforms
loader.loadRegistryFromClasspath("config/trading-scenario-registry.yaml");

// Avoid - may have issues on different platforms
loader.loadRegistryFromClasspath("config\\trading-scenario-registry.yaml");
```

---

## Troubleshooting

### Resource Not Found

**Error:** `ResourceNotFoundException: Resource not found: config.yaml`

**Solutions:**
1. Check the file exists in the expected location
2. Verify classpath prefix is correct (check for leading/trailing slashes)
3. Enable debug logging to see resolution attempts:

```properties
# logback.xml or logging.properties
logging.level.dev.mars.apex.core.config=DEBUG
```

### Circular References

**Error:** `CircularReferenceException: Circular reference detected: a.yaml -> b.yaml -> a.yaml`

**Solutions:**
1. Review component `file:` references for cycles
2. Use `component-refs` with proper dependency ordering
3. Consider breaking circular dependencies with shared base components

### Wrong Configuration Loaded

**Problem:** Getting unexpected configuration content

**Solutions:**
1. Check resolution order - first match wins
2. Use absolute paths or `classpath:` prefix for explicit resolution
3. Review search path order in the scenario registry YAML
4. Enable debug logging to see which file was actually loaded

### JAR Resource Not Found

**Error:** `Resource not found in classpath: config/trading-scenario-registry.yaml`

**Solutions:**
1. Verify file is included in JAR: `jar tf myapp.jar | grep scenario-registry`
2. Check the resource path doesn't have a leading slash
3. Verify the file is in `src/main/resources` (not `src/test/resources`)

### Windows Path Issues

**Problem:** Paths not resolving on Windows

**Solutions:**
1. Use forward slashes in classpath paths
2. Use `File.separator` for filesystem paths if building programmatically
3. For environment variables, use semicolon (`;`) as path separator on Windows

---

## Reference

### Key Classes

| Class | Purpose |
|-------|---------|
| `RulesEngine.Builder` | Fluent API for creating RulesEngine instances |
| `ScenarioRegistryLoader` | Loads scenario registries (`type: scenario-registry`) from files or classpath |
| `ComponentLoader` | Loads components with nested reference resolution |
| `ResourceResolver` | Unified filesystem/classpath resource resolution |
| `ConfigurationContext` | Central registry for name-based configuration lookups |

### Configuration Properties

| Property | Environment Variable | Description |
|----------|---------------------|-------------|
| `apex.config.searchPaths` | `APEX_CONFIG_SEARCH_PATHS` | Filesystem search paths (semicolon-separated) |
| `apex.config.classpathPrefixes` | `APEX_CONFIG_CLASSPATH_PREFIXES` | Classpath prefixes (semicolon-separated) |

### YAML Schema: search-paths

```yaml
search-paths:
  filesystem:
    - "/absolute/path"
    - "./relative/path"
    - "${ENV_VAR}/path"
  classpath:
    - "prefix/without/leading/slash/"
    - "META-INF/apex/"
```

---
