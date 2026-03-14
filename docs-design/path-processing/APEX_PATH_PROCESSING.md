## Plan: Unified Resource Loading for APEX Configuration

Author: Mark Andrew Ray-Smith Cityline Ltd
Date: 2025-11-12
Last Updated: 2026-01-08

## Implementation Status

| Phase | Description | Status |
|-------|-------------|--------|
| **Phase 1** | Stream/classpath methods for `YamlConfigurationLoader` | COMPLETE |
| **Phase 1** | Stream/classpath methods for `ScenarioRegistryLoader` | COMPLETE |
| **Phase 1** | `RulesEngine.fromScenarioRegistry()` classpath support | COMPLETE |
| **Phase 2** | Create `ResourceResolver` abstraction | COMPLETE |
| **Phase 3** | Enhance component loading with classpath support | COMPLETE |
| **Phase 4** | Create `ConfigurationContext` for name-based resolution | COMPLETE |
| **Phase 5** | Add classpath scanning to `CatalogScanService` | COMPLETE |
| **Phase 6** | Add builder pattern and environment configuration | ⏳ Planned |

### Summary of Completed Work

**Phase 1-2** (Previously completed):
- `YamlConfigurationLoader` stream/classpath methods with 12 tests
- `ScenarioRegistryLoader` stream/classpath methods with 20+ tests
- `RulesEngine.fromScenarioRegistry()` classpath-first resolution
- `ResourceResolver` abstraction with 32 tests

**Phase 3** (Completed 2026-01-08):
- `ComponentLoader` enhanced with `ResourceResolver` integration
- Full stream-based and classpath loading for components
- Classpath context (`classpathBase`) tracked through recursive resolution
- Circular reference detection works with classpath resources
- 16 new tests in `ComponentLoaderClasspathTest`
- 8 new test YAML resources in `component-classpath-test/`

**Phase 4** (Completed 2026-01-08):
- `ConfigurationContext` central registry for name-based configuration lookup
- Thread-safe registration and retrieval using `ConcurrentHashMap`
- Builder pattern for fluent construction with search paths and classpath prefixes
- Bulk loading from filesystem search paths and classpath prefixes
- Support for configurations, data sources, scenarios, and components
- 38 tests in `ConfigurationContextTest` covering registration, lookup, thread safety

**Phase 5** (Completed 2026-01-08):
- `CatalogScanService` enhanced with classpath scanning methods
- New methods: `scanClasspath(String prefix)`, `scanClasspath(String prefix, ClassLoader)`, `scanAll(List paths, List prefixes)`
- Uses Spring's `PathMatchingResourcePatternResolver` for wildcard pattern matching
- `YamlContentAnalyzer` extended with `analyzeYamlContent(InputStream, String)` for stream-based analysis
- `YamlConfigMetadata` extended with `isClasspathResource` and `classpathPrefix` fields
- 25 tests in `CatalogScanServiceClasspathTest` covering classpath scanning, combined scanning, metadata

---

## Overview

Support loading YAML configurations from multiple sources (filesystem, classpath, streams) with unified name-based resolution across the entire APEX system, including scenarios, components, rules, enrichments, and data sources.

### Problem Analysis

**Current State:**
- YAML files are loaded by `source:` field (file path) but **referenced** by `name:` field (logical identifier)
- Components use `file:` attribute for references (different from `source:` in data-source-refs)
- `YamlConfigurationLoader` supports file, classpath, and stream loading - but other loaders don't
- `ScenarioRegistryLoader` only supports filesystem paths → fails with JAR URLs
- `ComponentLoader` resolves `file:` references relative to component location → breaks for JAR resources
- `CatalogScanService` only scans filesystem directories → no classpath scanning
- No central "ConfigurationContext" mapping logical names to loaded configurations
- No mechanism to specify search paths for name-based resolution

**Root Cause:** Inconsistent loading strategies across different loaders, and no unified resource resolution layer.

---

### Reference Patterns in APEX YAML

| Context | Attribute | Example | Resolution |
|---------|-----------|---------|------------|
| Data source refs | `source:` | `source: "data-sources/db.yaml"` | File path or classpath |
| Rule refs | `source:` | `source: "rules/validation.yaml"` | File path or classpath |
| Enrichment refs | `source:` | `source: "enrichments/lookup.yaml"` | File path or classpath |
| Component file refs | `file:` | `file: "rules/trade-validation.yaml"` | Relative to component |
| Scenario stages | `config-file:` | `config-file: "components/validation.yaml"` | Relative to scenario |
| Scenario registry | `scenario-file:` | `scenario-file: "scenarios/trade.yaml"` | Relative to registry |

---

### Part 1: Core Infrastructure

#### 1.1 Create `ResourceResolver` abstraction
**Status:** COMPLETE
**New class:** `apex-core/src/main/java/dev/mars/apex/core/config/ResourceResolver.java`

**Implemented API:**
```java
public class ResourceResolver {
    // Resolution strategies
    public enum ResolutionStrategy {
        CLASSPATH_FIRST,    // Default - try classpath first, then filesystem
        FILESYSTEM_FIRST,   // Try filesystem first, then classpath
        CLASSPATH_ONLY,     // Only try classpath
        FILESYSTEM_ONLY     // Only try filesystem
    }

    // Primary resolution methods
    public InputStream resolve(String reference) throws ResourceNotFoundException;
    public InputStream resolve(String reference, String basePath) throws ResourceNotFoundException;
    public InputStream resolveFromFilesystem(String path) throws ResourceNotFoundException;
    public InputStream resolveFromClasspath(String resourcePath) throws ResourceNotFoundException;
    
    // Path management
    public void addSearchPath(String path);
    public void addClasspathPrefix(String prefix);
    public void setSearchPaths(List<String> paths);
    public void setClasspathPrefixes(List<String> prefixes);
    public List<String> getSearchPaths();
    public List<String> getClasspathPrefixes();
    
    // Strategy configuration
    public void setResolutionStrategy(ResolutionStrategy strategy);
    public ResolutionStrategy getResolutionStrategy();
    
    // Utility methods
    public String resolveRelativePath(String reference, String basePath);
    public String getClasspathBase(String resourcePath);
    public boolean exists(String reference);
    public boolean exists(String reference, String basePath);
    
    // Builder pattern
    public static Builder builder();
}
```

**Supporting class:** `apex-core/src/main/java/dev/mars/apex/core/config/ResourceNotFoundException.java`

**Test Coverage:** `ResourceResolverTest.java` (32 test methods, all passing)

#### 1.2 Create `ConfigurationContext` for runtime name resolution
**Status:** COMPLETE
**File:** `apex-core/src/main/java/dev/mars/apex/core/config/ConfigurationContext.java`

Central registry for name-based resolution of APEX configurations with thread-safe access.

**Implemented API:**
```java
public class ConfigurationContext {
    // Thread-safe maps for name-based lookups (ConcurrentHashMap)
    private final Map<String, YamlRuleConfiguration> configurationsByName;
    private final Map<String, YamlDataSource> dataSourcesByName;
    private final Map<String, ScenarioConfiguration> scenariosByName;
    private final Map<String, ComponentConfiguration> componentsByName;
    private final ResourceResolver resourceResolver;
    
    // Constructors
    public ConfigurationContext();  // Default ResourceResolver
    public ConfigurationContext(ResourceResolver resourceResolver);
    
    // Registration methods
    public void registerConfiguration(String name, YamlRuleConfiguration config);
    public void registerDataSource(String name, YamlDataSource dataSource);
    public void registerScenario(String name, ScenarioConfiguration scenario);
    public void registerComponent(String name, ComponentConfiguration component);
    
    // Name-based lookups (returns null if not found)
    public YamlRuleConfiguration getConfiguration(String name);
    public YamlDataSource getDataSource(String name);
    public ScenarioConfiguration getScenario(String name);
    public ComponentConfiguration getComponent(String name);
    
    // Contains/exists checks
    public boolean containsConfiguration(String name);
    public boolean containsDataSource(String name);
    public boolean containsScenario(String name);
    public boolean containsComponent(String name);
    
    // Collection access
    public Set<String> getConfigurationNames();
    public Set<String> getDataSourceNames();
    public Set<String> getScenarioNames();
    public Set<String> getComponentNames();
    public int size();
    public boolean isEmpty();
    
    // Bulk loading from search paths
    public int loadAllFromSearchPaths();
    public int loadAllFromClasspath(String classpathPrefix);
    public YamlRuleConfiguration loadConfiguration(String path);
    public ComponentConfiguration loadComponent(String path);
    
    // Clear/remove operations
    public void clear();
    public YamlRuleConfiguration removeConfiguration(String name);
    public YamlDataSource removeDataSource(String name);
    public ScenarioConfiguration removeScenario(String name);
    public ComponentConfiguration removeComponent(String name);
    
    // Accessor
    public ResourceResolver getResourceResolver();
    
    // Builder pattern
    public static Builder builder();
}
```

**Builder API:**
```java
ConfigurationContext context = ConfigurationContext.builder()
    .withResourceResolver(resolver)      // Optional custom resolver
    .addSearchPath("/etc/apex/configs")  // Filesystem search paths
    .addClasspathPrefix("apex/")         // Classpath prefixes
    .build();
```

**Test Coverage:** `ConfigurationContextTest.java` (38 test methods, all passing)

---

### Part 2: Enhance Existing Loaders

#### 2.1 Add `loadAsMap(InputStream)` to [YamlConfigurationLoader](apex-core/src/main/java/dev/mars/apex/core/config/yaml/YamlConfigurationLoader.java)
**Status:** COMPLETE

Implemented methods:
```java
// Stream-based loading
public Map<String, Object> loadAsMap(InputStream inputStream) throws YamlConfigurationException

// Classpath convenience method
public Map<String, Object> loadAsMapFromClasspath(String resourcePath) throws YamlConfigurationException
```

**Test Coverage:** `YamlConfigurationLoaderStreamTest.java` (345 lines, 12+ test cases)

#### 2.2 Add stream/classpath support to [ScenarioRegistryLoader](apex-core/src/main/java/dev/mars/apex/core/config/yaml/ScenarioRegistryLoader.java)
**Status:** COMPLETE

Implemented methods:
```java
// Stream-based loading
public Map<String, ScenarioConfiguration> loadRegistry(InputStream inputStream)
public Map<String, ScenarioConfiguration> loadRegistry(InputStream inputStream, String classpathBase)

// Classpath convenience
public Map<String, ScenarioConfiguration> loadRegistryFromClasspath(String resourcePath)

// Individual scenario loading
public ScenarioConfiguration loadScenarioFromStream(InputStream inputStream)
public ScenarioConfiguration loadScenarioFromClasspath(String resourcePath)
```

**Test Coverage:** `ScenarioRegistryLoaderStreamTest.java` (598 lines, 20+ test cases)

#### 2.3 Add stream/classpath support to Component loading
**Status:** COMPLETE
**File:** `apex-core/src/main/java/dev/mars/apex/core/config/component/ComponentLoader.java`

Enhanced component file resolution to support classpath resources with full `ResourceResolver` integration:
- When `file:` reference is resolved, check filesystem first, then classpath
- Support `classpathBase` for relative `file:` resolution within JARs
- Maintain circular reference detection for classpath-loaded components
- Tracks loading context through recursive component resolution

**Implemented API:**
```java
// Constructor overloads
public ComponentLoader()  // Uses default ResourceResolver
public ComponentLoader(ResourceResolver resourceResolver)  // Custom resolver

// Stream-based loading
public YamlComponent loadComponent(InputStream inputStream)
public YamlComponent loadComponent(InputStream inputStream, String classpathBase)

// Classpath convenience
public YamlComponent loadComponentFromClasspath(String resourcePath)

// Classpath-aware file loading
public YamlComponent loadComponent(String filePath, String classpathBase)

// Accessor
public ResourceResolver getResourceResolver()
```

**Key Implementation Details:**
- `loadComponentFile()` uses `resourceResolver.resolve(filePath, classpathBase)` for unified resolution
- `resolveAllReferences()` passes `classpathBase` through recursive calls
- `detectCircularReferences()` and `hasCircularReference()` track `classpathBase` context
- `resolveRelativePath()` delegates to `resourceResolver.resolveRelativePath()`
- New helper methods: `getBasePath()`, `isAbsolutePath()`

**Test Coverage:** `ComponentLoaderClasspathTest.java` (16 test methods, all passing)

#### 2.4 Add `ResourceResolver` integration to [DataSourceResolver](apex-core/src/main/java/dev/mars/apex/core/config/datasource/DataSourceResolver.java)
**Status:** ⏳ PLANNED

- Use `ResourceResolver` for consistent file/classpath resolution
- Maintain existing `configCache` for performance

#### 2.5 Enhance `RulesEngine.fromScenarioRegistry()` for classpath support
**Status:** COMPLETE

**File:** `apex-core/src/main/java/dev/mars/apex/core/engine/config/RulesEngine.java`

The `fromScenarioRegistry(String registryPath)` static factory method has been enhanced to try classpath first, then filesystem (consistent with APEX resolution strategy):

**Implemented Code:**
```java
public static RulesEngine fromScenarioRegistry(String registryPath) throws YamlConfigurationException {
    logger.info("Creating RulesEngine from scenario registry: {}", registryPath);

    ScenarioRegistryLoader loader = new ScenarioRegistryLoader();
    Map<String, dev.mars.apex.core.service.scenario.ScenarioConfiguration> scenarios;
    
    // Try classpath first (enables JAR-packaged resources and test resources)
    try (java.io.InputStream is = RulesEngine.class.getClassLoader().getResourceAsStream(registryPath)) {
        if (is != null) {
            // Derive classpath base for relative path resolution
            String classpathBase = deriveClasspathBase(registryPath);
            scenarios = loader.loadRegistry(is, classpathBase);
            logger.info("Loaded {} scenarios from classpath registry: {}", scenarios.size(), registryPath);
        } else {
            // Fallback to filesystem loading (existing behavior)
            scenarios = loader.loadRegistry(registryPath);
            logger.info("Loaded {} scenarios from filesystem registry: {}", scenarios.size(), registryPath);
        }
    } catch (java.io.IOException e) {
        throw new YamlConfigurationException("Failed to load scenario registry: " + registryPath, e);
    }

    if (scenarios == null || scenarios.isEmpty()) {
        throw new YamlConfigurationException(
            "Scenario registry is empty or failed to load: " + registryPath
        );
    }

    // Create a minimal RulesEngineConfiguration for scenario-only engine
    RulesEngineConfiguration config = new RulesEngineConfiguration();

    // Create RulesEngine with scenario registry
    return new RulesEngine(config, null, scenarios);
}

private static String deriveClasspathBase(String resourcePath) {
    int lastSlash = resourcePath.lastIndexOf('/');
    return lastSlash > 0 ? resourcePath.substring(0, lastSlash + 1) : "";
}
```

**Test Coverage:** `RulesEngineFromScenarioRegistryTest.java` (372 lines, 10+ test cases)

**Benefits:**
- Test code can use simple classpath paths like `"config/scenario-registry.yaml"`
- JAR-packaged applications work without modification
- Backward compatible with existing filesystem paths
- Follows APEX resolution strategy: classpath first, then filesystem

---

### Part 3: Classpath Scanning

#### 3.1 Add classpath scanning to [CatalogScanService](apex-yaml-manager/src/main/java/dev/mars/apex/yaml/manager/service/CatalogScanService.java)
**Status:** COMPLETE

**File:** `apex-yaml-manager/src/main/java/dev/mars/apex/yaml/manager/service/CatalogScanService.java`

New methods implemented:
```java
// Scan classpath for YAML files under prefix
public Map<String, Object> scanClasspath(String classpathPrefix)
public Map<String, Object> scanClasspath(String classpathPrefix, ClassLoader classLoader)

// Combined scanning
public Map<String, Object> scanAll(List<String> filesystemPaths, List<String> classpathPrefixes)
```

**Implementation Details:**
- Uses Spring's `PathMatchingResourcePatternResolver` for wildcard pattern matching
- Scans for both `.yaml` and `.yml` extensions
- Normalizes prefixes (removes leading slash, ensures trailing slash for directories)
- Thread-safe with detailed scan statistics

**Supporting Changes:**
- `YamlContentAnalyzer.analyzeYamlContent(InputStream, String)` - Stream-based YAML analysis
- `YamlConfigMetadata.isClasspathResource` - Flag indicating classpath-loaded resource
- `YamlConfigMetadata.classpathPrefix` - Prefix used during scanning

**Test Coverage:** `CatalogScanServiceClasspathTest.java` (25 test methods, all passing)

---

### Part 4: Name-Based Resolution

#### 4.1 Enhance reference resolution to support name-only lookups

Current YAML pattern:
```yaml
data-source-refs:
  - name: "customer-database"
    source: "data-sources/customer-database.yaml"  # Required file path
```

Enhanced pattern (either `source` OR resolution by `name`):
```yaml
data-source-refs:
  - name: "customer-database"
    # source: optional if name can be resolved from search paths/context
```

**Resolution order:**
1. If `source:` specified → load from that path (file or classpath)
2. If no `source:` → search `ConfigurationContext` by `name`
3. If not in context → search filesystem paths for `{name}.yaml`
4. If not found → search classpath prefixes for `{name}.yaml`

#### 4.2 Support multiple file patterns for name lookup

When resolving `name: "customer-database"`, search for:
- `customer-database.yaml`
- `customer-database.yml`
- `data-sources/customer-database.yaml`
- `{searchPath}/customer-database.yaml`

---

### Part 5: Component-Specific Requirements

#### 5.1 Component `file:` reference resolution with classpath support

Components reference files using multiple sections with `file:` attribute:
```yaml
# All these sections use file: references
config-files:
  - file: "rules/trade-validation.yaml"
    
rule-configurations:
  - file: "rules/amount-validation.yaml"
    
enrichment-refs:
  - file: "enrichments/market-data.yaml"
    
component-refs:
  - file: "components/base-validation.yaml"
```

**Current behavior:** Resolves relative to component file location on filesystem.

**Enhanced behavior:**
1. If component loaded from filesystem → resolve `file:` relative to component directory (existing)
2. If component loaded from classpath → resolve `file:` relative to component's classpath location
3. Support absolute classpath paths: `file: "classpath:apex/rules/validation.yaml"`
4. Support project-relative paths: `file: "src/test/resources/..."` (for test compatibility)

#### 5.2 Nested component resolution from classpath

Components can reference other components:
```yaml
component-refs:
  - file: "components/common-validation.yaml"
    execution-order: 1
```

**Requirements:**
- Track loading context (filesystem vs classpath) through recursion
- Maintain nesting depth limits (max 5 levels)
- Circular reference detection works for classpath resources
- Mixed mode: parent from filesystem can reference child from classpath (and vice versa)

#### 5.3 Scenario `config-file:` resolution

Scenarios reference components or config files in processing stages:
```yaml
processing-stages:
  - stage-name: "validation"
    config-file: "components/validation-component.yaml"
    execution-order: 1
```

And scenario registries reference scenario files:
```yaml
scenarios:
  - scenario-id: "otc-option-us"
    config-file: "scenarios/otc-option-us.yaml"
```

**Enhanced behavior:**
- If scenario/registry loaded from classpath, resolve `config-file:` as classpath resource
- Support explicit classpath prefix: `config-file: "classpath:scenarios/trade.yaml"`
- Maintain backward compatibility with project-relative paths for tests

#### 5.4 Test file path patterns

Tests currently use paths like:
```yaml
config-file: "src/test/java/dev/mars/apex/demo/scenario/ComponentScenarioTest-scenario.yaml"
```

**Requirements:**
- Continue supporting project-relative paths for backward compatibility
- Allow tests to also use classpath resources via `loadRegistryFromClasspath()`
- `ResourceResolver` should try filesystem first, then classpath (existing pattern)

---

### Part 6: Configuration & Integration

#### 6.1 Scenario Registry Search Paths

Each scenario registry can define its own search paths for resolving scenario files and configurations. This provides encapsulation - different registries can look in different locations.

**YAML Configuration in Scenario Registry:**
```yaml
metadata:
  id: trading-scenario-registry
  name: Trading Scenarios
  version: 1.0.0

# Registry-specific search paths (NEW)
search-paths:
  filesystem:
    - "/etc/apex/trading"
    - "./configs/trading"
  classpath:
    - "trading/"
    - "META-INF/apex/trading/"

scenarios:
  - scenario-id: "otc-option-us"
    config-file: "scenarios/otc-option-us.yaml"  # Resolved using search paths above
  - scenario-id: "equity-swap"
    config-file: "scenarios/equity-swap.yaml"
```

**Resolution Order for `config-file:` within a registry:**
1. If absolute path → use directly
2. If `classpath:` prefix → resolve from classpath
3. Search registry's filesystem paths (in order)
4. Search registry's classpath prefixes (in order)
5. Fallback to global search paths (if configured)
6. Fallback to relative resolution from registry location

**Java API:**
```java
// ScenarioRegistryLoader automatically uses search paths from YAML
ScenarioRegistryLoader loader = new ScenarioRegistryLoader();
Map<String, ScenarioConfiguration> scenarios = loader.loadRegistry("trading-registry.yaml");

// Or programmatically add search paths
loader.addSearchPath("/etc/apex/trading");
loader.addClasspathPrefix("trading/");
```

#### 6.2 Add search path configuration to [RulesEngine](apex-core/src/main/java/dev/mars/apex/core/engine/RulesEngine.java)

Global search paths for the RulesEngine (applies when not overridden by registry):

```java
// New factory methods
public static RulesEngine fromClasspath(String resourcePath)
public static RulesEngine withSearchPaths(List<String> searchPaths)
public static RulesEngine withResourceResolver(ResourceResolver resolver)

// Builder pattern
RulesEngine.builder()
    .addSearchPath("/etc/apex/configs")
    .addClasspathPrefix("apex/")
    .withContext(configurationContext)
    .loadFromFile("main-config.yaml")
    .build();
```

#### 6.3 Environment/property-based configuration

Support configuring **global default** search paths via:
- System properties: `apex.config.searchPaths`, `apex.config.classpathPrefixes`
- Environment variables: `APEX_CONFIG_SEARCH_PATHS`, `APEX_CONFIG_CLASSPATH_PREFIXES`

**Precedence Order (highest to lowest):**
1. Registry-level `search-paths:` in YAML
2. Programmatic configuration via loader/builder
3. System properties
4. Environment variables
5. Built-in defaults

---

### Part 7: Test Implementation Plan

Following APEX testing patterns (extending `DemoTestBase`, using `@DisplayName`, comprehensive logging), implement the following test classes:

---

#### 7.1 `ResourceResolverTest.java`
**Status:** COMPLETE
**Location:** `apex-core/src/test/java/dev/mars/apex/core/config/ResourceResolverTest.java`

| Test Method | Purpose | Status |
|-------------|---------|--------|
| `testResolveFromClasspathRootResource()` | Resolve resource from classpath root | |
| `testResolveFromClasspathWithPrefix()` | Load resource with classpath prefix | |
| `testResolveNestedClasspathResource()` | Load nested classpath resource | |
| `testResolveFromClasspathExplicit()` | Explicit classpath resolution | |
| `testResolveFromClasspathNotFound()` | Handle missing classpath resource | |
| `testResolveFromFilesystemAbsolutePath()` | Resolve absolute filesystem path | |
| `testResolveFromFilesystemWithSearchPaths()` | Find file in configured search paths | |
| `testResolveFromFilesystemExplicit()` | Explicit filesystem resolution | |
| `testResolveFromFilesystemNotFound()` | Handle missing file | |
| `testResolveRelativePathWithBasePath()` | Resolve relative path against base | |
| `testResolveRelativePathWithDotSlash()` | Handle ./ prefix | |
| `testResolveRelativePathUtility()` | Utility method tests | |
| `testGetClasspathBaseFromResourcePath()` | Extract directory from classpath path | |
| `testDefaultStrategyIsClasspathFirst()` | Default strategy verification | |
| `testClasspathFirstStrategy()` | Classpath first resolution | |
| `testFilesystemFirstStrategy()` | Filesystem first resolution | |
| `testClasspathOnlyStrategy()` | Classpath-only mode | |
| `testFilesystemOnlyStrategy()` | Filesystem-only mode | |
| `testAddSearchPathDynamically()` | Add search path after construction | |
| `testAddClasspathPrefixDynamically()` | Add classpath prefix dynamically | |
| `testSetSearchPaths()` | Replace search paths | |
| `testSetClasspathPrefixes()` | Replace classpath prefixes | |
| `testMultipleSearchPathsOrder()` | Search paths checked in order | |
| `testResolveNullPathThrowsException()` | Handle null input gracefully | |
| `testResolveNotFoundThrowsException()` | Proper error when resource not found | |
| `testAddNullSearchPathThrowsException()` | Handle null search path | |
| `testAddNullClasspathPrefixThrowsException()` | Handle null prefix | |
| `testExistsForClasspathResource()` | Exists check for classpath | |
| `testExistsForFilesystemResource()` | Exists check for filesystem | |
| `testExistsForNullOrEmpty()` | Exists for null/empty | |
| `testBuilderCreatesResolver()` | Builder pattern creates resolver | |
| `testBuilderAllOptions()` | Builder with all configuration options | |

**Implementation:** 500+ lines, 32 test methods, all passing

---

#### 7.2 `ConfigurationContextTest.java`
**Status:** COMPLETE
**Location:** `apex-core/src/test/java/dev/mars/apex/core/config/ConfigurationContextTest.java`

| Test Method | Purpose | Status |
|-------------|---------|--------|
| `testRegisterAndRetrieveConfiguration()` | Basic registration and lookup | |
| `testRegisterAndRetrieveDataSource()` | DataSource registration/lookup | |
| `testRegisterAndRetrieveScenario()` | Scenario registration/lookup | |
| `testRegisterAndRetrieveComponent()` | Component registration/lookup | |
| `testRegisterConfigurationNullName()` | Handle null name | |
| `testRegisterConfigurationNullValue()` | Handle null value | |
| `testDuplicateRegistrationOverwrites()` | Later registration overwrites earlier | |
| `testGetConfigurationNotFound()` | Return null for unknown name | |
| `testGetWithNullName()` | Handle null lookup | |
| `testContainsConfiguration()` | Check existence by name | |
| `testContainsNullName()` | Contains check for null | |
| `testGetConfigurationNames()` | Return set of registered names | |
| `testSize()` | Total count across all types | |
| `testIsEmpty()` | Empty state check | |
| `testClear()` | Clear all registered items | |
| `testRemoveConfiguration()` | Remove by name | |
| `testRemoveNonExistent()` | Remove non-existent item | |
| `testBuilderCreatesContext()` | Builder creates context | |
| `testBuilderWithResourceResolver()` | Builder with custom resolver | |
| `testBuilderWithSearchPaths()` | Builder with search paths | |
| `testBuilderWithClasspathPrefixes()` | Builder with classpath prefixes | |
| `testBuilderIgnoresInvalidPaths()` | Ignore null/empty paths | |
| `testLoadAllFromSearchPaths()` | Bulk load from filesystem paths | |
| `testLoadFromNonExistentSearchPath()` | Handle non-existent path | |
| `testLoadAllFromClasspath()` | Bulk load from classpath prefix | |
| `testThreadSafetyOfRegistration()` | Concurrent registration safety | |
| `testThreadSafetyOfLookup()` | Concurrent lookup safety | |
| `testMixedConcurrentAccess()` | Mixed read/write concurrency | |
| `testDefaultResourceResolver()` | Default resolver creation | |
| `testCustomResourceResolver()` | Custom resolver injection | |
| `testNullResourceResolver()` | Reject null resolver | |

**Implementation:** 660+ lines, 38 test methods, all passing

---

#### 7.3 `YamlConfigurationLoaderStreamTest.java`
**Status:** COMPLETE
**Location:** `apex-core/src/test/java/dev/mars/apex/core/config/yaml/YamlConfigurationLoaderStreamTest.java`

| Test Method | Purpose | Status |
|-------------|---------|--------|
| `testLoadAsMapFromInputStream()` | Parse YAML map from stream | |
| `testThrowExceptionForNullInputStream()` | Handle null stream | |
| `testThrowExceptionForEmptyYaml()` | Handle empty YAML | |
| `testThrowExceptionForInvalidYamlSyntax()` | Handle malformed YAML | |
| `testLoadComplexYamlWithNestedStructures()` | Complex nested structures | |
| `testLoadYamlWithLists()` | YAML with list structures | |
| `testLoadAsMapFromClasspath()` | Classpath resource loading | |
| `testLoadAsMapFromClasspathNotFound()` | Handle missing resource | |
| `testLoadAsMapFromClasspathNullPath()` | Handle null path | |
| `testLoadAsMapFromClasspathEmptyPath()` | Handle empty path | |
| `testLoadFromStreamProducesSameResultAsFile()` | Stream/file parity | |
| `testLoadApexRuleConfiguration()` | Load full rule config | |

**Implementation:** 345 lines, 12 test methods

**Test Data:** In-memory YAML strings converted to `ByteArrayInputStream`, plus classpath resources in `scenario-stream-test/`

---

#### 7.4 `ScenarioRegistryLoaderStreamTest.java`
**Status:** COMPLETE
**Location:** `apex-core/src/test/java/dev/mars/apex/core/config/yaml/ScenarioRegistryLoaderStreamTest.java`

| Test Method | Purpose | Status |
|-------------|---------|--------|
| `testLoadRegistryFromInputStream()` | Load registry from stream | |
| `testLoadRegistryWithMultipleScenarios()` | Multiple scenarios in registry | |
| `testLoadRegistryThrowsExceptionForNullStream()` | Handle null stream | |
| `testLoadRegistryThrowsExceptionForEmptyRegistry()` | Handle empty registry | |
| `testLoadRegistryWithClasspathBase()` | Resolve with classpath base | |
| `testLoadRegistryWithNullClasspathBase()` | Handle null classpath base | |
| `testLoadRegistryFromClasspath()` | Convenience classpath method | |
| `testLoadRegistryFromClasspathNotFound()` | Handle missing resource | |
| `testLoadRegistryFromClasspathEmptyPath()` | Handle empty path | |
| `testLoadRegistryFromClasspathNullPath()` | Handle null path | |
| `testLoadScenarioFromStream()` | Load individual scenario | |
| `testLoadScenarioFromStreamWithStages()` | Scenario with stages | |
| `testLoadScenarioFromStreamThrowsExceptionForNull()` | Handle null stream | |
| `testLoadScenarioFromClasspath()` | Load scenario from classpath | |
| `testLoadScenarioFromClasspathNotFound()` | Handle missing scenario | |
| `testLoadScenarioFromClasspathNullPath()` | Handle null path | |
| `testRelativePathResolutionFromClasspath()` | Relative path resolution | |
| `testBackwardCompatibilityWithFilePath()` | Filesystem API unchanged | |
| `testRegistryWithDisabledScenarios()` | Disabled scenario handling | |
| `testRegistryWithRoutingConfiguration()` | Routing config parsing | |

**Implementation:** 598 lines, 20+ test methods

**Test Resources:**
```
src/test/resources/
  scenario-stream-test/
    test-registry.yaml
    basic-validation-scenario.yaml
    complex-rules-scenario.yaml
```

---

#### 7.5 `ComponentLoaderClasspathTest.java`
**Status:** COMPLETE
**Location:** `apex-core/src/test/java/dev/mars/apex/core/config/component/ComponentLoaderClasspathTest.java`

| Test Method | Purpose | Status |
|-------------|---------|--------|
| `testDefaultConstructorCreatesResourceResolver()` | Default constructor creates resolver | |
| `testCustomResourceResolverInjection()` | Custom resolver injection | |
| `testLoadComponentFromClasspath()` | Load component from classpath | |
| `testLoadNestedComponentFromClasspath()` | Nested component resolution | |
| `testLoadComponentWithRuleReferencesFromClasspath()` | Rule refs from classpath | |
| `testLoadComponentFromInputStream()` | Stream-based loading | |
| `testLoadComponentFromInputStreamWithClasspathBase()` | Stream with classpath context | |
| `testCircularReferenceDetectionFromClasspath()` | Circular ref detection | |
| `testLoadComponentWithClasspathBase()` | Explicit classpath base | |
| `testClasspathBaseTrackingThroughRecursion()` | Context tracking | |
| `testGetResourceResolver()` | Accessor method | |
| `testResourceResolverUsedForResolution()` | Resolver integration | |
| `testLoadFromClasspathNotFound()` | Missing resource handling | |
| `testLoadFromStreamNull()` | Null stream handling | |
| `testLoadFromClasspathNull()` | Null path handling | |
| `testLoadFromClasspathEmpty()` | Empty path handling | |

**Implementation:** 300+ lines, 16 test methods, all passing

**Test Resources:**
```
src/test/resources/
  component-classpath-test/
    nested-parent.yaml         # Parent referencing nested child
    nested-child.yaml          # Child component with rules
    parent-rules.yaml          # Rule configuration
    child-rules.yaml           # Nested rule configuration
    circular-classpath-a.yaml  # Circular reference test
    circular-classpath-b.yaml  # Circular reference test
    stream-load-component.yaml # Stream loading test
    stream-rules.yaml          # Stream loading rules
```

---

#### 7.6 `ScenarioClasspathIntegrationTest.java`
**Status:** COMPLETE
**Location:** `apex-demo/src/test/java/dev/mars/apex/demo/scenario/ScenarioClasspathIntegrationTest.java`

**Extends:** `DemoTestBase`

| Test Method | Purpose | Status |
|-------------|---------|--------|
| `testLoadScenariosFromClasspath()` | Load full registry from classpath | |
| `testScenarioWithProcessingStages()` | Multi-stage scenario parsing | |
| `testOtcOptionScenarioConfiguration()` | OTC option scenario details | |
| `testSimpleTradeScenarioConfiguration()` | Simple trade scenario details | |
| `testScenarioWithRuleConfigurations()` | Inline rule configs | |
| `testStageDependencyResolution()` | Stage dependency parsing | |
| `testConditionalStageExecution()` | Conditional stage configs | |
| `testFailurePolicyConfiguration()` | Failure policy parsing | |
| `testMissingClasspathResourceError()` | Error handling for missing | |
| `testInvalidScenarioConfiguration()` | Invalid config handling | |

**Implementation:** 343 lines, 10 test methods

**Test Resources:**
```
src/test/resources/
  dev/mars/apex/demo/scenario/classpath-integration/
    scenario-registry.yaml
    scenarios/
      otc-option-scenario.yaml
      simple-trade-scenario.yaml
```

---

#### 7.7 `RulesEngineFromScenarioRegistryTest.java`
**Status:** COMPLETE (replaces planned JarResourceLoadingTest)
**Location:** `apex-core/src/test/java/dev/mars/apex/core/engine/config/RulesEngineFromScenarioRegistryTest.java`

**Purpose:** Test `RulesEngine.fromScenarioRegistry()` classpath loading support

| Test Method | Purpose | Status |
|-------------|---------|--------|
| `testLoadRegistryFromClasspath()` | Load registry from classpath resource | |
| `testRelativePathResolutionInClasspath()` | Relative paths work in classpath | |
| `testScenarioConfigurationsAvailable()` | Loaded scenarios are accessible | |
| `testLoadRegistryFromFilesystem()` | Filesystem fallback works | |
| `testFilesystemPathsStillWork()` | Backward compatibility | |
| `testNonExistentResourceThrowsException()` | Error for missing resource | |
| `testNonExistentFilesystemPathThrowsException()` | Error for missing file | |
| `testEmptyRegistryThrowsException()` | Error for empty registry | |
| `testNullRegistryPathThrowsException()` | Error for null path | |
| `testClasspathTakesPrecedenceOverFilesystem()` | Classpath first resolution | |

**Implementation:** 372 lines, 10 test methods

**Implementation Note:** This test class validates the enhanced `fromScenarioRegistry()` method that now supports both classpath and filesystem loading, addressing the original JAR URL issue.

---

#### 7.8 `ResourceLoadingBackwardCompatibilityTest.java`
**Status:** ⏳ PLANNED (incorporated into other test classes)
**Location:** `apex-core/src/test/java/dev/mars/apex/core/config/ResourceLoadingBackwardCompatibilityTest.java`

**Note:** Backward compatibility is tested within:
- `ScenarioRegistryLoaderStreamTest.testBackwardCompatibilityWithFilePath()` ✅
- `RulesEngineFromScenarioRegistryTest.testFilesystemPathsStillWork()` ✅
- `RulesEngineFromScenarioRegistryTest.testLoadRegistryFromFilesystem()` ✅

---

#### 7.9 Test Resource Organization

**Implemented Structure:**
```
apex-core/src/test/resources/
├── scenario-stream-test/                    IMPLEMENTED
│   ├── test-registry.yaml
│   ├── basic-validation-scenario.yaml
│   └── complex-rules-scenario.yaml
├── resolver-test/                           ⏳ PLANNED
│   ├── test-config.yaml
│   └── nested/nested-config.yaml
├── component-classpath-test/                IMPLEMENTED
│   ├── parent-component.yaml
│   ├── nested/child-component.yaml
│   ├── rules/validation-rules.yaml
│   └── enrichments/lookup-enrichment.yaml
└── config/  (existing)
    └── groups-only-logic/  (existing)

apex-demo/src/test/resources/
└── dev/mars/apex/demo/scenario/
    └── classpath-integration/               IMPLEMENTED
        ├── scenario-registry.yaml
        └── scenarios/
            ├── otc-option-scenario.yaml
            └── simple-trade-scenario.yaml
```

---

#### 7.10 Phase 6 Test Plan: Registry Search Paths & Configuration
**Status:** ⏳ PLANNED

##### 7.10.1 `ScenarioRegistrySearchPathsTest.java`
**Location:** `apex-core/src/test/java/dev/mars/apex/core/config/yaml/ScenarioRegistrySearchPathsTest.java`

**POSITIVE TESTS - Registry-Level Search Paths:**

| Test Method | Purpose |
|-------------|---------|
| `testLoadRegistryWithFilesystemSearchPaths()` | Registry YAML with `search-paths.filesystem` resolves scenarios |
| `testLoadRegistryWithClasspathSearchPaths()` | Registry YAML with `search-paths.classpath` resolves scenarios |
| `testLoadRegistryWithBothSearchPathTypes()` | Combined filesystem + classpath search paths |
| `testSearchPathOrderIsRespected()` | First path in list takes precedence |
| `testFilesystemSearchPathsBeforeClasspath()` | Filesystem paths searched before classpath paths |
| `testRelativePathResolutionWithSearchPaths()` | `config-file: "scenario.yaml"` resolved via search paths |
| `testAbsolutePathBypassesSearchPaths()` | Absolute paths ignore search paths |
| `testClasspathPrefixBypassesSearchPaths()` | `classpath:` prefix uses direct classpath resolution |
| `testSearchPathsWithNestedScenarios()` | Scenarios referencing components via search paths |
| `testSearchPathsInheritedToComponents()` | Component `file:` refs use registry's search paths |
| `testMultipleRegistriesWithDifferentSearchPaths()` | Each registry uses its own search paths |
| `testSearchPathsWithTrailingSlash()` | Paths with/without trailing slash work correctly |
| `testSearchPathsWithEnvironmentVariables()` | `${ENV_VAR}` expansion in search paths |

**NEGATIVE TESTS - Registry-Level Search Paths:**

| Test Method | Purpose |
|-------------|---------|
| `testEmptySearchPathsSection()` | Empty `search-paths:` falls back to relative resolution |
| `testNullSearchPathsList()` | Null/missing `search-paths.filesystem` handled gracefully |
| `testInvalidSearchPathIgnored()` | Invalid path in list doesn't break resolution |
| `testNonExistentFilesystemSearchPath()` | Non-existent directory logged but not fatal |
| `testNonExistentClasspathPrefixIgnored()` | Non-existent prefix skipped without error |
| `testScenarioNotFoundInAnySearchPath()` | Clear error when scenario not in any path |
| `testCircularSearchPathReference()` | No infinite loop if paths reference each other |
| `testMalformedSearchPathsYaml()` | Invalid YAML structure gives helpful error |
| `testSearchPathsWithSpecialCharacters()` | Paths with spaces, unicode handled |
| `testSearchPathsExceedMaxDepth()` | Deep nesting (>5 levels) is rejected |

##### 7.10.2 `RulesEngineBuilderTest.java`
**Location:** `apex-core/src/test/java/dev/mars/apex/core/engine/RulesEngineBuilderTest.java`

**POSITIVE TESTS - Builder Pattern:**

| Test Method | Purpose |
|-------------|---------|
| `testBuilderWithSingleSearchPath()` | `builder().addSearchPath().build()` creates engine |
| `testBuilderWithMultipleSearchPaths()` | Multiple `addSearchPath()` calls accumulate |
| `testBuilderWithSingleClasspathPrefix()` | `builder().addClasspathPrefix().build()` |
| `testBuilderWithMultipleClasspathPrefixes()` | Multiple prefixes accumulate |
| `testBuilderWithConfigurationContext()` | `withContext()` injects pre-loaded context |
| `testBuilderWithResourceResolver()` | `withResourceResolver()` injects custom resolver |
| `testBuilderLoadFromFile()` | `loadFromFile()` loads config using search paths |
| `testBuilderLoadFromClasspath()` | `loadFromClasspath()` loads from classpath |
| `testBuilderChaining()` | Full fluent chain: paths → prefixes → context → load → build |
| `testBuilderCreatesIndependentInstances()` | Each `build()` creates new engine |
| `testBuilderWithEmptyConfiguration()` | Builder with no config creates minimal engine |
| `testBuilderSearchPathsPassedToLoaders()` | Search paths used by internal loaders |

**NEGATIVE TESTS - Builder Pattern:**

| Test Method | Purpose |
|-------------|---------|
| `testBuilderWithNullSearchPath()` | Null path ignored or throws |
| `testBuilderWithEmptySearchPath()` | Empty string path ignored |
| `testBuilderWithNullClasspathPrefix()` | Null prefix ignored or throws |
| `testBuilderWithEmptyClasspathPrefix()` | Empty prefix ignored |
| `testBuilderWithNullContext()` | Null context throws IllegalArgumentException |
| `testBuilderWithNullResourceResolver()` | Null resolver throws |
| `testBuilderLoadFromNonExistentFile()` | Clear error for missing config file |
| `testBuilderLoadFromNonExistentClasspath()` | Clear error for missing resource |
| `testBuilderCalledWithoutBuild()` | Verify builder doesn't leak state |
| `testBuilderBuildCalledTwice()` | Second build() creates fresh instance |

##### 7.10.3 `EnvironmentConfigurationTest.java`
**Location:** `apex-core/src/test/java/dev/mars/apex/core/config/EnvironmentConfigurationTest.java`

**POSITIVE TESTS - System Properties:**

| Test Method | Purpose |
|-------------|---------|
| `testSearchPathsFromSystemProperty()` | `-Dapex.config.searchPaths=/path1,/path2` |
| `testClasspathPrefixesFromSystemProperty()` | `-Dapex.config.classpathPrefixes=prefix1/,prefix2/` |
| `testSystemPropertyOverridesDefault()` | System property takes precedence over defaults |
| `testSystemPropertyWithSinglePath()` | Single path without comma works |
| `testSystemPropertyWithWindowsPaths()` | `C:\path\to\config` handled on Windows |
| `testSystemPropertyWithUnixPaths()` | `/etc/apex/config` handled on Unix |

**POSITIVE TESTS - Environment Variables:**

| Test Method | Purpose |
|-------------|---------|
| `testSearchPathsFromEnvironmentVariable()` | `APEX_CONFIG_SEARCH_PATHS=/path1:/path2` |
| `testClasspathPrefixesFromEnvironmentVariable()` | `APEX_CONFIG_CLASSPATH_PREFIXES=prefix1/:prefix2/` |
| `testEnvironmentVariableWithPathSeparator()` | Uses OS-specific path separator |
| `testEnvironmentVariableLowerPriorityThanSystemProperty()` | System prop beats env var |

**POSITIVE TESTS - Precedence:**

| Test Method | Purpose |
|-------------|---------|
| `testRegistryOverridesProgrammatic()` | YAML search-paths > builder paths |
| `testProgrammaticOverridesSystemProperty()` | Builder paths > system property |
| `testSystemPropertyOverridesEnvironment()` | System property > env var |
| `testEnvironmentOverridesDefault()` | Env var > built-in defaults |
| `testFullPrecedenceChain()` | Registry > Programmatic > SysProp > Env > Default |

**NEGATIVE TESTS - Environment Configuration:**

| Test Method | Purpose |
|-------------|---------|
| `testInvalidSystemPropertyFormat()` | Malformed property value handled |
| `testEmptySystemProperty()` | Empty property value uses defaults |
| `testNullEnvironmentVariable()` | Missing env var uses fallback |
| `testMalformedEnvironmentVariable()` | Invalid env var format handled |
| `testConflictingConfigurations()` | Clear which source wins |

##### 7.10.4 `SearchPathResolutionOrderTest.java`
**Location:** `apex-core/src/test/java/dev/mars/apex/core/config/SearchPathResolutionOrderTest.java`

**POSITIVE TESTS - Resolution Order:**

| Test Method | Purpose |
|-------------|---------|
| `testAbsolutePathResolvedFirst()` | `/absolute/path/config.yaml` doesn't use search |
| `testClasspathPrefixResolvedSecond()` | `classpath:config.yaml` uses classpath directly |
| `testRegistryFilesystemPathsThird()` | Registry's filesystem paths searched in order |
| `testRegistryClasspathPrefixesFourth()` | Registry's classpath prefixes searched next |
| `testGlobalFilesystemPathsFifth()` | Global/builder filesystem paths |
| `testGlobalClasspathPrefixesSixth()` | Global/builder classpath prefixes |
| `testRelativeToRegistryLastResort()` | Relative to registry file as final fallback |
| `testFirstMatchWins()` | First matching path wins, no further searching |
| `testResolutionOrderLogged()` | Debug logging shows resolution attempt order |

**NEGATIVE TESTS - Resolution Order:**

| Test Method | Purpose |
|-------------|---------|
| `testAllPathsExhaustedError()` | Clear error when no path resolves |
| `testErrorIncludesSearchedPaths()` | Error message lists all paths tried |
| `testPartialMatchNotAccepted()` | Directory match without file not accepted |

##### 7.10.5 Test Resources for Phase 6

```
apex-core/src/test/resources/
├── search-path-test/
│   ├── registry-with-search-paths.yaml       # Registry with search-paths section
│   ├── registry-no-search-paths.yaml         # Registry without search-paths
│   ├── path-a/
│   │   └── scenario-a.yaml                   # Scenario in first search path
│   ├── path-b/
│   │   └── scenario-b.yaml                   # Scenario in second search path
│   ├── path-conflict/
│   │   └── scenario-a.yaml                   # Same name, different content (precedence test)
│   └── scenarios/
│       └── relative-scenario.yaml            # For relative path tests
├── builder-test/
│   ├── main-config.yaml                      # Config for builder tests
│   └── components/
│       └── validation.yaml                   # Component referenced by config
└── env-config-test/
    ├── sys-prop-config.yaml                  # Config for system property tests
    └── env-var-config.yaml                   # Config for env var tests
```

##### 7.10.6 Test Execution Commands for Phase 6

```bash
# Run all Phase 6 tests
mvn test -pl apex-core -Dtest="*SearchPath*,*Builder*,*EnvironmentConfiguration*"

# Run with system property override
mvn test -pl apex-core -Dtest="EnvironmentConfigurationTest" \
    -Dapex.config.searchPaths=/tmp/test1,/tmp/test2

# Run specific test class
mvn test -pl apex-core -Dtest="ScenarioRegistrySearchPathsTest"

# Run positive tests only (by naming convention)
mvn test -pl apex-core -Dtest="*SearchPath*#test*With*"

# Run negative tests only (by naming convention)  
mvn test -pl apex-core -Dtest="*SearchPath*#test*Invalid*,*SearchPath*#test*Null*,*SearchPath*#test*Empty*"
```

---

#### 7.10 Test Implementation Order

**Phase 1 - Core Infrastructure (COMPLETE):**
1. `YamlConfigurationLoaderStreamTest` - Foundation for stream-based loading
2. `ScenarioRegistryLoaderStreamTest` - Immediate JAR URL fix validation
3. `RulesEngineFromScenarioRegistryTest` - Classpath loading for RulesEngine factory

**Phase 2 - Resource Resolution (⏳ PLANNED):**
4. ⏳ `ResourceResolverTest` - Unified resolution abstraction
5. ⏳ `ConfigurationContextTest` - Name-based resolution

**Phase 3 - Component Support (⏳ PLANNED):**
6. ⏳ `ComponentClasspathLoadingTest` - Component classpath loading

**Phase 4 - End-to-End (COMPLETE):**
7. `ScenarioClasspathIntegrationTest` - Full integration tests

**Phase 5 - Backward Compatibility (COMPLETE - integrated into other tests):**
8. Backward compatibility verified in existing test classes

---

#### 7.11 Test Execution Commands

```bash
# Run all resource loading tests
mvn test -pl apex-core -Dtest="*ResourceResolver*,*Stream*,*Classpath*"

# Run integration tests
mvn test -pl apex-demo -Dtest="*ClasspathIntegration*,*JarResource*"

# Run backward compatibility tests
mvn test -pl apex-core,apex-demo -Dtest="*BackwardCompatibility*"

# Full test suite
mvn clean test -pl apex-core,apex-demo
```

---

### Implementation Notes

- **Backward compatibility**: All existing methods remain unchanged; new methods are additive ✅
- **Resolution strategy**: Classpath → Filesystem (implemented in `RulesEngine.fromScenarioRegistry()`) ✅
- **Caching**: `ConfigurationContext` acts as cache; `DataSourceResolver.configCache` remains for data sources
- **Error messages**: Clear distinction between "file not found", "classpath resource not found", "name not registered" ✅
- **Thread safety**: `ConfigurationContext` and `ResourceResolver` must be thread-safe
- **Loading context tracking**: Pass loading context (filesystem/classpath + base path) through recursive loading ✅

### Migration Path

1. **Phase 1** (COMPLETE): Add stream/classpath methods to `YamlConfigurationLoader` and `ScenarioRegistryLoader`
2. **Phase 1.5** (COMPLETE): Enhance `RulesEngine.fromScenarioRegistry()` with classpath support
3. **Phase 2** (COMPLETE): Create `ResourceResolver` abstraction with unified resolution
4. **Phase 3** (⏳ PLANNED): Enhance component loading with classpath support
5. **Phase 4** (⏳ PLANNED): Create `ConfigurationContext` for name-based resolution
6. **Phase 5** (⏳ PLANNED): Add classpath scanning to `CatalogScanService`
7. **Phase 6** (⏳ PLANNED): Add builder pattern and environment configuration

---

## Completed Implementation Summary (2026-01-08)

### Files Created (Phase 2)

| File | Lines | Purpose |
|------|-------|---------|
| `ResourceResolver.java` | 580+ | Unified resource resolution abstraction |
| `ResourceNotFoundException.java` | 50 | Exception for missing resources |
| `ResourceResolverTest.java` | 500+ | Comprehensive unit tests (32 tests) |

### Files Modified (Phase 1)

| File | Changes |
|------|---------|
| `apex-core/.../YamlConfigurationLoader.java` | Added `loadAsMap(InputStream)`, `loadAsMapFromClasspath(String)` |
| `apex-core/.../ScenarioRegistryLoader.java` | Added stream/classpath methods (291 lines new) |
| `apex-core/.../RulesEngine.java` | Enhanced `fromScenarioRegistry()` with classpath support |

### Test Files Created

| File | Lines | Coverage |
|------|-------|----------|
| `YamlConfigurationLoaderStreamTest.java` | 345 | Stream + classpath loading |
| `ScenarioRegistryLoaderStreamTest.java` | 598 | Registry stream + classpath loading |
| `RulesEngineFromScenarioRegistryTest.java` | 372 | Classpath + filesystem fallback |
| `ScenarioClasspathIntegrationTest.java` | 343 | End-to-end classpath scenarios |
| `ResourceResolverTest.java` | 500+ | ResourceResolver (32 tests) |

### Test Resources Created

| Resource | Purpose |
|----------|---------|
| `scenario-stream-test/test-registry.yaml` | Test scenario registry |
| `scenario-stream-test/basic-validation-scenario.yaml` | Basic scenario config |
| `scenario-stream-test/complex-rules-scenario.yaml` | Complex scenario with rules |
| `classpath-integration/scenario-registry.yaml` | E2E test registry |
| `classpath-integration/scenarios/otc-option-scenario.yaml` | OTC option scenario |
| `classpath-integration/scenarios/simple-trade-scenario.yaml` | Simple trade scenario |

### Key Commits

- `fa4b264` - Enhance RulesEngine classpath loading + unit tests
- `f86a356` - RulesEngine classpath support with filesystem fallback
- `3f8ec96` - Classpath integration tests and scenarios
- `98f15f9` - Unit tests for YAML configuration loading and ScenarioRegistryLoader
