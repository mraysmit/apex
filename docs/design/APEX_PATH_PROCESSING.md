## Plan: Unified Resource Loading for APEX Configuration

Author: Mark Andrew Ray-Smith Cityline Ltd
Date: 2025-11-12


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
**New class:** `apex-core/src/main/java/dev/mars/apex/core/config/ResourceResolver.java`

```java
public class ResourceResolver {
    private List<String> searchPaths;           // Filesystem paths to search
    private List<String> classpathPrefixes;     // Classpath locations to search
    private ClassLoader classLoader;
    
    // Resolution strategies
    public InputStream resolve(String reference);
    public InputStream resolve(String reference, String basePath);  // For relative refs
    public InputStream resolveFromFilesystem(String path);
    public InputStream resolveFromClasspath(String resourcePath);
    
    // Path management
    public void addSearchPath(String path);
    public void addClasspathPrefix(String prefix);
    public void setSearchPaths(List<String> paths);
    
    // Utility for relative path resolution
    public String resolveRelativePath(String reference, String basePath);
    public String getClasspathBase(String resourcePath);  // Extract directory from resource path
}
```

#### 1.2 Create `ConfigurationContext` for runtime name resolution
**New class:** `apex-core/src/main/java/dev/mars/apex/core/config/ConfigurationContext.java`

```java
public class ConfigurationContext {
    private Map<String, YamlRuleConfiguration> configurationsByName;
    private Map<String, YamlDataSource> dataSourcesByName;
    private Map<String, ScenarioConfiguration> scenariosByName;
    private Map<String, YamlComponent> componentsByName;  // NEW: Component registry
    private ResourceResolver resourceResolver;
    
    // Name-based lookups (by YAML `name:` or `id:` attribute)
    public YamlRuleConfiguration getConfiguration(String name);
    public YamlDataSource getDataSource(String name);
    public ScenarioConfiguration getScenario(String name);
    public YamlComponent getComponent(String name);
    
    // Registration (called during loading)
    public void registerConfiguration(String name, YamlRuleConfiguration config);
    public void registerDataSource(String name, YamlDataSource dataSource);
    public void registerComponent(String name, YamlComponent component);
    
    // Bulk loading from search paths
    public void loadAllFromSearchPaths();
    public void loadAllFromClasspath(String prefix);
}
```

---

### Part 2: Enhance Existing Loaders

#### 2.1 Add `loadAsMap(InputStream)` to [YamlConfigurationLoader](apex-core/src/main/java/dev/mars/apex/core/config/yaml/YamlConfigurationLoader.java)
- New overload that parses YAML from `InputStream` returning `Map<String, Object>`
- Mirror existing `loadAsMap(String filePath)` logic but read from stream

#### 2.2 Add stream/classpath support to [ScenarioRegistryLoader](apex-core/src/main/java/dev/mars/apex/core/service/scenario/ScenarioRegistryLoader.java)

New methods:
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

#### 2.3 Add stream/classpath support to Component loading

Enhance component file resolution to support classpath resources:
- When `file:` reference is resolved, check filesystem first, then classpath
- Support `classpathBase` for relative `file:` resolution within JARs
- Maintain circular reference detection for classpath-loaded components

```java
// In ComponentConfigurationLoader or similar
public YamlComponent loadComponent(InputStream inputStream, String classpathBase)
public YamlComponent loadComponentFromClasspath(String resourcePath)

// Recursive file: reference resolution
private InputStream resolveFileReference(String fileRef, String basePath) {
    // 1. Try absolute filesystem path
    // 2. Try relative to basePath on filesystem
    // 3. Try classpath with classpathBase prefix
    // 4. Try classpath at root
}
```

#### 2.4 Add `ResourceResolver` integration to [DataSourceResolver](apex-core/src/main/java/dev/mars/apex/core/config/datasource/DataSourceResolver.java)
- Use `ResourceResolver` for consistent file/classpath resolution
- Maintain existing `configCache` for performance

---

### Part 3: Classpath Scanning

#### 3.1 Add classpath scanning to [CatalogScanService](apex-yaml-manager/src/main/java/dev/mars/apex/yaml/service/CatalogScanService.java) or new service

New methods:
```java
// Scan classpath for YAML files under prefix
public void scanClasspath(String classpathPrefix)
public void scanClasspath(String classpathPrefix, ClassLoader classLoader)

// Combined scanning
public void scanAll(List<String> filesystemPaths, List<String> classpathPrefixes)
```

**Implementation note:** Use Spring's `PathMatchingResourcePatternResolver` or manual classpath enumeration with `ClassLoader.getResources()`.

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

#### 6.1 Add search path configuration to [RulesEngine](apex-core/src/main/java/dev/mars/apex/core/engine/RulesEngine.java)

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

#### 6.2 Environment/property-based configuration

Support configuring search paths via:
- System properties: `apex.config.searchPaths`, `apex.config.classpathPrefixes`
- Environment variables: `APEX_CONFIG_SEARCH_PATHS`, `APEX_CONFIG_CLASSPATH_PREFIXES`
- YAML configuration section:
```yaml
apex-config:
  search-paths:
    - "/etc/apex/configs"
    - "./configs"
  classpath-prefixes:
    - "apex/"
    - "META-INF/apex/"
```

---

### Part 7: Test Implementation Plan

Following APEX testing patterns (extending `DemoTestBase`, using `@DisplayName`, comprehensive logging), implement the following test classes:

---

#### 7.1 `ResourceResolverTest.java`
**Location:** `apex-core/src/test/java/dev/mars/apex/core/config/ResourceResolverTest.java`

| Test Method | Purpose | Pattern |
|-------------|---------|---------|
| `testResolveFromFilesystemAbsolutePath()` | Resolve absolute filesystem path | Unit |
| `testResolveFromFilesystemRelativePath()` | Resolve path relative to working directory | Unit |
| `testResolveFromFilesystemWithSearchPaths()` | Find file in configured search paths | Unit |
| `testResolveFromClasspathRootResource()` | Load resource from classpath root | Unit |
| `testResolveFromClasspathWithPrefix()` | Load resource with classpath prefix | Unit |
| `testResolveRelativePathWithBasePath()` | Resolve relative path against base | Unit |
| `testResolutionOrderFilesystemFirst()` | Verify filesystem checked before classpath | Unit |
| `testResolutionOrderClasspathFallback()` | Fall back to classpath when file not found | Unit |
| `testResolveNotFoundThrowsException()` | Proper error when resource not found | Negative |
| `testResolveNullPathThrowsException()` | Handle null input gracefully | Negative |
| `testGetClasspathBaseFromResourcePath()` | Extract directory from classpath path | Unit |
| `testAddSearchPathDynamically()` | Add search path after construction | Unit |
| `testMultipleSearchPathsOrder()` | Search paths checked in order | Unit |

**Test Resources:**
```
src/test/resources/
  resolver-test/
    test-config.yaml
    nested/
      nested-config.yaml
```

---

#### 7.2 `ConfigurationContextTest.java`
**Location:** `apex-core/src/test/java/dev/mars/apex/core/config/ConfigurationContextTest.java`

| Test Method | Purpose | Pattern |
|-------------|---------|---------|
| `testRegisterAndRetrieveConfiguration()` | Basic registration and lookup | Unit |
| `testRegisterAndRetrieveDataSource()` | DataSource registration/lookup | Unit |
| `testRegisterAndRetrieveScenario()` | Scenario registration/lookup | Unit |
| `testRegisterAndRetrieveComponent()` | Component registration/lookup | Unit |
| `testGetConfigurationByNameNotFound()` | Return null for unknown name | Negative |
| `testDuplicateRegistrationOverwrites()` | Later registration overwrites earlier | Unit |
| `testLoadAllFromSearchPaths()` | Bulk load from filesystem paths | Integration |
| `testLoadAllFromClasspath()` | Bulk load from classpath prefix | Integration |
| `testThreadSafetyOfRegistration()` | Concurrent registration safety | Concurrency |
| `testThreadSafetyOfLookup()` | Concurrent lookup safety | Concurrency |

---

#### 7.3 `YamlConfigurationLoaderStreamTest.java`
**Location:** `apex-core/src/test/java/dev/mars/apex/core/config/yaml/YamlConfigurationLoaderStreamTest.java`

| Test Method | Purpose | Pattern |
|-------------|---------|---------|
| `testLoadAsMapFromInputStream()` | Parse YAML map from stream | Unit |
| `testLoadAsMapFromInputStreamWithProperties()` | Property resolution in stream | Unit |
| `testLoadAsMapFromInputStreamNullThrowsException()` | Handle null stream | Negative |
| `testLoadAsMapFromInputStreamEmptyContent()` | Handle empty YAML | Negative |
| `testLoadAsMapFromInputStreamInvalidYaml()` | Handle malformed YAML | Negative |
| `testLoadFromStreamComparedToFile()` | Stream and file produce same result | Parity |

**Test Data:** In-memory YAML strings converted to `ByteArrayInputStream`

---

#### 7.4 `ScenarioRegistryLoaderStreamTest.java`
**Location:** `apex-core/src/test/java/dev/mars/apex/core/service/scenario/ScenarioRegistryLoaderStreamTest.java`

| Test Method | Purpose | Pattern |
|-------------|---------|---------|
| `testLoadRegistryFromInputStream()` | Load registry from stream | Unit |
| `testLoadRegistryFromInputStreamWithInlineScenarios()` | Scenarios embedded in registry | Unit |
| `testLoadRegistryFromClasspath()` | Convenience method for classpath | Unit |
| `testLoadRegistryFromClasspathNotFound()` | Handle missing classpath resource | Negative |
| `testLoadRegistryWithClasspathBase()` | Resolve scenario-file relative to base | Unit |
| `testLoadScenarioFromInputStream()` | Load individual scenario from stream | Unit |
| `testLoadScenarioFromClasspath()` | Load scenario from classpath | Unit |
| `testRelativeScenarioFileResolutionFromClasspath()` | Relative paths in classpath context | Integration |
| `testBackwardCompatibilityWithFilePath()` | Existing file-based API unchanged | Parity |
| `testMixedFilesystemAndClasspathScenarios()` | Registry mixes both source types | Integration |

**Test Resources:**
```
src/test/resources/
  scenario-stream-test/
    test-registry.yaml
    scenarios/
      test-scenario.yaml
      inline-scenario.yaml
```

---

#### 7.5 `ComponentClasspathLoadingTest.java`
**Location:** `apex-core/src/test/java/dev/mars/apex/core/config/component/ComponentClasspathLoadingTest.java`

| Test Method | Purpose | Pattern |
|-------------|---------|---------|
| `testLoadComponentFromClasspath()` | Load component from classpath resource | Unit |
| `testLoadComponentFromInputStream()` | Load component from stream | Unit |
| `testComponentFileRefResolutionFromClasspath()` | Resolve `file:` refs in classpath | Integration |
| `testNestedComponentLoadingFromClasspath()` | Nested components from classpath | Integration |
| `testCircularReferenceDetectionFromClasspath()` | Circular ref detection works | Negative |
| `testNestingDepthLimitFromClasspath()` | Max 5 levels enforced | Negative |
| `testMixedFilesystemAndClasspathComponents()` | Parent filesystem, child classpath | Integration |
| `testConfigFilesResolutionFromClasspath()` | `config-files:` section | Integration |
| `testRuleConfigurationsResolutionFromClasspath()` | `rule-configurations:` section | Integration |
| `testEnrichmentRefsResolutionFromClasspath()` | `enrichment-refs:` section | Integration |
| `testComponentRefsResolutionFromClasspath()` | `component-refs:` section | Integration |

**Test Resources:**
```
src/test/resources/
  component-classpath-test/
    parent-component.yaml
    nested/
      child-component.yaml
    rules/
      validation-rules.yaml
    enrichments/
      lookup-enrichment.yaml
```

---

#### 7.6 `ScenarioClasspathIntegrationTest.java`
**Location:** `apex-demo/src/test/java/dev/mars/apex/demo/scenario/ScenarioClasspathIntegrationTest.java`

**Extends:** `DemoTestBase`

| Test Method | Purpose | Pattern |
|-------------|---------|---------|
| `testEndToEndScenarioFromClasspath()` | Full scenario flow from classpath | E2E Integration |
| `testScenarioWithComponentsFromClasspath()` | Scenario → Component → Rules chain | E2E Integration |
| `testClassificationAndRoutingFromClasspath()` | Classification rules with classpath config | E2E Integration |
| `testMultiStageProcessingFromClasspath()` | Multiple stages from classpath | E2E Integration |
| `testConditionalStagesFromClasspath()` | Conditional execution with classpath | E2E Integration |
| `testFailurePoliciesFromClasspath()` | Failure policies work correctly | E2E Integration |
| `testStageDependenciesFromClasspath()` | Dependencies resolve correctly | E2E Integration |

**Test Resources:**
```
src/test/resources/
  dev/mars/apex/demo/scenario/classpath-integration/
    scenario-registry.yaml
    scenarios/
      otc-option-scenario.yaml
    components/
      validation-component.yaml
    rules/
      trade-validation.yaml
    enrichments/
      market-data.yaml
```

---

#### 7.7 `JarResourceLoadingTest.java`
**Location:** `apex-demo/src/test/java/dev/mars/apex/demo/scenario/JarResourceLoadingTest.java`

**Purpose:** Simulate JAR-packaged resource loading (the original issue)

| Test Method | Purpose | Pattern |
|-------------|---------|---------|
| `testLoadRegistryFromJarUrl()` | Handle `jar:file:/...!/resource.yaml` URL | Regression |
| `testResolveRelativePathsInJar()` | Relative paths work inside JAR | Regression |
| `testComponentChainInJar()` | Full component chain from JAR | Regression |
| `testMixedJarAndFilesystemResources()` | JAR resources + filesystem overrides | Integration |

**Implementation Note:** Use `URLClassLoader` with a test JAR or mock JAR URLs

---

#### 7.8 `ResourceLoadingBackwardCompatibilityTest.java`
**Location:** `apex-core/src/test/java/dev/mars/apex/core/config/ResourceLoadingBackwardCompatibilityTest.java`

**Purpose:** Ensure existing functionality unchanged

| Test Method | Purpose | Pattern |
|-------------|---------|---------|
| `testExistingFilePathLoadingUnchanged()` | `loadRegistry(String path)` works | Parity |
| `testExistingFromScenarioRegistryUnchanged()` | `RulesEngine.fromScenarioRegistry()` | Parity |
| `testProjectRelativePathsWork()` | `src/test/java/...` paths resolve | Parity |
| `testComponentWithFilesystemPathsWork()` | File-based component refs work | Parity |
| `testExistingTestsStillPass()` | Run subset of existing scenario tests | Regression |

---

#### 7.9 Test Resource Organization

```
apex-core/src/test/resources/
├── resolver-test/
│   ├── test-config.yaml
│   └── nested/nested-config.yaml
├── scenario-stream-test/
│   ├── test-registry.yaml
│   └── scenarios/
│       ├── test-scenario.yaml
│       └── inline-scenario.yaml
├── component-classpath-test/
│   ├── parent-component.yaml
│   ├── nested/child-component.yaml
│   ├── rules/validation-rules.yaml
│   └── enrichments/lookup-enrichment.yaml
└── config/  (existing)
    └── groups-only-logic/  (existing)

apex-demo/src/test/resources/
└── dev/mars/apex/demo/scenario/
    └── classpath-integration/
        ├── scenario-registry.yaml
        ├── scenarios/otc-option-scenario.yaml
        ├── components/validation-component.yaml
        ├── rules/trade-validation.yaml
        └── enrichments/market-data.yaml
```

---

#### 7.10 Test Implementation Order

**Phase 1 - Core Infrastructure (implement first):**
1. `YamlConfigurationLoaderStreamTest` - Foundation for stream-based loading
2. `ScenarioRegistryLoaderStreamTest` - Immediate JAR URL fix validation

**Phase 2 - Resource Resolution:**
3. `ResourceResolverTest` - Unified resolution abstraction
4. `ConfigurationContextTest` - Name-based resolution

**Phase 3 - Component Support:**
5. `ComponentClasspathLoadingTest` - Component classpath loading

**Phase 4 - End-to-End:**
6. `ScenarioClasspathIntegrationTest` - Full integration tests
7. `JarResourceLoadingTest` - Original issue regression tests

**Phase 5 - Backward Compatibility:**
8. `ResourceLoadingBackwardCompatibilityTest` - Ensure no regressions

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

- **Backward compatibility**: All existing methods remain unchanged; new methods are additive
- **Resolution strategy**: Filesystem → Classpath → ConfigurationContext (configurable order)
- **Caching**: `ConfigurationContext` acts as cache; `DataSourceResolver.configCache` remains for data sources
- **Error messages**: Clear distinction between "file not found", "classpath resource not found", "name not registered"
- **Thread safety**: `ConfigurationContext` and `ResourceResolver` must be thread-safe
- **Loading context tracking**: Pass loading context (filesystem/classpath + base path) through recursive loading

### Migration Path

1. **Phase 1** (immediate): Add stream/classpath methods to `ScenarioRegistryLoader` (fixes JAR URL issue)
2. **Phase 2**: Create `ResourceResolver` and integrate with existing loaders
3. **Phase 3**: Enhance component loading with classpath support
4. **Phase 4**: Create `ConfigurationContext` for name-based resolution
5. **Phase 5**: Add classpath scanning to `CatalogScanService`
6. **Phase 6**: Add builder pattern and environment configuration
