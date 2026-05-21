# APEX Path Processing Quick Guide

This guide summarizes the standard APEX path-processing model from the full design document.

## Purpose

Use one consistent approach to load YAML/config resources from:
- classpath
- filesystem
- streams

and resolve references predictably across rules, scenarios, components, and data sources.

## Core Principles

1. Prefer unified resource resolution over ad hoc file handling.
2. Use classpath-first behavior by default (unless a module requires filesystem-first).
3. Keep relative references relative to the parent config location/context.
4. Support both explicit paths and name-based lookup where applicable.
5. Preserve backward compatibility: existing filesystem paths must still work.

## Standard Reference Fields

APEX supports multiple reference keys depending on context:
- `source:` for data-source refs, rule refs, enrichment refs
- `file:` for component refs and component nested files
- `config-file:` for scenario stage/registry links
- `scenario-file:` for scenario registry entries

## Resolution Order (Practical)

When resolving a config reference:
1. Absolute path: use directly.
2. `classpath:` prefix: resolve from classpath directly.
3. Registry-level search paths (if defined):
   - filesystem paths first (in listed order)
   - classpath prefixes next (in listed order)
4. Global search paths/prefixes (builder or environment configuration).
5. Relative fallback: resolve relative to current parent resource location.

For name-only lookup (no `source`), search by common patterns such as:
- `{name}.yaml`
- `{name}.yml`
- `data-sources/{name}.yaml`
- `{searchPath}/{name}.yaml`

## Recommended Runtime Patterns

### 1. Scenario Registry Loading

Use classpath-friendly loading where possible:

```java
RulesEngine engine = RulesEngine.fromScenarioRegistry("config/scenario-registry.yaml");
```

Behavior should be classpath-first with filesystem fallback.

### 2. Component File References

For component `file:` entries:
- if parent is filesystem-loaded, resolve relative to filesystem directory
- if parent is classpath-loaded, resolve relative to classpath base
- allow explicit `classpath:` references for clarity

### 3. Test-Friendly Paths

Prefer classpath resources in tests over process-global system properties.

Good:
- `classpath:dev/mars/apex/demo/...`

Avoid unless necessary:
- global `System.setProperty(...)` for path injection

## Search Path Configuration

### Registry-level (highest precedence)

```yaml
search-paths:
  filesystem:
    - "./configs/trading"
  classpath:
    - "trading/"
```

### Programmatic

```java
ConfigurationContext context = ConfigurationContext.builder()
    .addSearchPath("/etc/apex/configs")
    .addClasspathPrefix("apex/")
    .build();
```

### Environment/System Properties

- `apex.config.searchPaths`
- `apex.config.classpathPrefixes`
- `APEX_CONFIG_SEARCH_PATHS`
- `APEX_CONFIG_CLASSPATH_PREFIXES`

Precedence should be:
1. registry YAML
2. programmatic builder/loader
3. system properties
4. environment variables
5. defaults

## Error Handling Expectations

Failures should clearly indicate what failed:
- classpath resource not found
- filesystem path not found
- name not found in context/search paths

Include attempted locations in diagnostics when possible.

## Minimum Adoption Checklist

1. Use `ResourceResolver` for new loading code.
2. Keep classpath + filesystem support in all loaders.
3. Resolve nested references using parent context/base path.
4. Prefer classpath-based test assets for portability.
5. Keep old filesystem usage working during migration.

## When To Use Which API

- `YamlConfigurationLoader` stream/classpath methods: when loading raw YAML/config maps.
- `ScenarioRegistryLoader` stream/classpath methods: when loading scenario registries and scenario files.
- `RulesEngine.fromScenarioRegistry(...)`: simplest entry point for scenario registry loading.
- `ConfigurationContext`: name-based runtime lookup and bulk preload.
- `ResourceResolver`: consistent path existence and resolution behavior.

## Known Limitations

### `loadFromStream` cannot resolve relative references

`ConfigurationLoader.loadFromStream(InputStream)` has no knowledge of where the stream originated, so `sourceDirectory` is always `null`. Any `rule-refs`, `enrichment-refs`, or `data-source-refs` inside a stream-loaded config that use relative paths (e.g. `source: "rules/extra-rules.yaml"`) will fail to resolve.

**Use `loadFromFile` or `loadFromClasspath` instead.** Both methods set `sourceDirectory` before processing references:
- `loadFromFile(String)` / `loadFromFile(File)` — always sets `sourceDirectory` to the file's parent directory.
- `loadFromClasspath(String)` — sets `sourceDirectory` when the classpath resource resolves to a file on disk (standard in test environments and exploded-WAR deployments); `null` inside a JAR.

