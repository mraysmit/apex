# Rule Reference Resolution Design

## Design Overview

Following the proven external data source pattern, implement rule reference resolution that:
1. Loads referenced rule files BEFORE validation
2. Merges rules into the main configuration
3. Validates the complete merged configuration

## Component Design

### 1. YamlRuleRef Class

**Purpose**: Define references to external rule files
**Pattern**: Exact copy of `YamlDataSourceRef` structure

```java
package dev.mars.apex.core.config.yaml;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * YAML configuration class for external rule file references.
 * 
 * This class represents a reference to an external rule configuration file,
 * enabling separation of rule definitions across multiple files.
 * 
 * Example YAML structure:
 * <pre>
 * rule-refs:
 *   - name: "customer-rules"
 *     source: "rules/customer-rules.yaml"
 *     enabled: true
 *     description: "Customer validation rules"
 * </pre>
 */
public class YamlRuleRef {
    @JsonProperty("name")
    private String name;
    
    @JsonProperty("source")
    private String source;
    
    @JsonProperty("enabled")
    private Boolean enabled;
    
    @JsonProperty("description")
    private String description;
    
    // Constructors, getters, setters (same pattern as YamlDataSourceRef)
}
```

### 2. YamlRuleConfiguration Enhancement

**Purpose**: Add rule-refs field to main configuration
**Pattern**: Same as data-source-refs field

```java
// Add to YamlRuleConfiguration.java
@JsonProperty("rule-refs")
private List<YamlRuleRef> ruleRefs;

public List<YamlRuleRef> getRuleRefs() {
    return ruleRefs;
}

public void setRuleRefs(List<YamlRuleRef> ruleRefs) {
    this.ruleRefs = ruleRefs;
}
```

### 3. Rule Reference Processing

**Purpose**: Load and merge referenced rule files
**Pattern**: Mirror `processDataSourceReferences()` exactly

```java
// Add to YamlConfigurationLoader.java
private void processRuleReferences(YamlRuleConfiguration config) throws YamlConfigurationException {
    if (config.getRuleRefs() == null || config.getRuleRefs().isEmpty()) {
        return;
    }

    LOGGER.info("Processing " + config.getRuleRefs().size() + " external rule references");

    // Initialize rules list if it doesn't exist
    if (config.getRules() == null) {
        config.setRules(new ArrayList<>());
    }

    // Process each rule reference
    for (YamlRuleRef ref : config.getRuleRefs()) {
        if (!ref.isEnabled()) {
            LOGGER.info("Skipping disabled rule reference: " + ref.getName());
            continue;
        }

        try {
            LOGGER.info("Resolving external rule reference: " + ref.getName() + " from " + ref.getSource());

            // Load the referenced rule file
            YamlRuleConfiguration referencedConfig = loadRuleFile(ref.getSource());

            // Merge rules from referenced file
            if (referencedConfig.getRules() != null) {
                config.getRules().addAll(referencedConfig.getRules());
            }

            LOGGER.info("Successfully resolved and merged rules from: " + ref.getName());

        } catch (Exception e) {
            throw new YamlConfigurationException(
                "Failed to resolve rule reference '" + ref.getName() + "' from '" + ref.getSource() + "'", e);
        }
    }

    LOGGER.info("Successfully processed all external rule references");
}
```

### 4. Rule File Loading

**Purpose**: Load referenced rule files (classpath + file system support)
**Pattern**: Reuse existing YamlConfigurationLoader methods

```java
// Add to YamlConfigurationLoader.java
private YamlRuleConfiguration loadRuleFile(String source) throws YamlConfigurationException {
    try {
        // Try file system first, then classpath (same pattern as DataSourceResolver)
        Path path = Paths.get(source);
        if (Files.exists(path)) {
            // Load from file system
            return loadFromFileWithoutProcessing(path.toFile());
        } else {
            // Load from classpath
            return loadFromClasspathWithoutProcessing(source);
        }
    } catch (Exception e) {
        throw new YamlConfigurationException("Failed to load rule file: " + source, e);
    }
}
```

### 5. Loading Without Processing

**Purpose**: Load rule files without triggering recursive processing
**Pattern**: Create variants of existing load methods

```java
// Add to YamlConfigurationLoader.java
private YamlRuleConfiguration loadFromFileWithoutProcessing(File file) throws YamlConfigurationException {
    // Same as loadFromFile() but skip processRuleReferences() and processDataSourceReferences()
    // Only do basic YAML parsing and property resolution
}

private YamlRuleConfiguration loadFromClasspathWithoutProcessing(String resourcePath) throws YamlConfigurationException {
    // Same as loadFromClasspath() but skip processRuleReferences() and processDataSourceReferences()
    // Only do basic YAML parsing and property resolution
}
```

## Processing Flow

### Updated Loading Sequence
```
loadFromFile() {
    1. Parse YAML → YamlRuleConfiguration
    2. processRuleReferences(config)        // NEW: Load and merge rule files
    3. processDataSourceReferences(config)  // EXISTING: Load and merge data sources
    4. validateConfiguration(config)        // EXISTING: Validate complete merged config
}
```

### Rule Reference Processing Detail
```
processRuleReferences() {
    for each YamlRuleRef in config.getRuleRefs():
        1. Check if enabled
        2. Load referenced rule file (without processing to avoid recursion)
        3. Extract rules from referenced file
        4. Merge rules into main config.getRules()
        5. Log success/failure
}
```

## YAML Syntax

### Main Configuration File
```yaml
metadata:
  name: "Main Configuration"
  version: "1.0.0"

# Reference external rule files
rule-refs:
  - name: "customer-rules"
    source: "rules/customer-rules.yaml"
    enabled: true
    description: "Customer validation rules"
  - name: "financial-rules"
    source: "rules/financial-rules.yaml"
    enabled: true
    description: "Financial processing rules"

# Rule groups can now reference rules from any file
rule-groups:
  - id: "customer-validation"
    name: "Customer Validation Group"
    rule-ids:
      - "age-check"        # From customer-rules.yaml
      - "email-validation"  # From customer-rules.yaml
      - "credit-check"      # From financial-rules.yaml
```

### Referenced Rule File (rules/customer-rules.yaml)
```yaml
metadata:
  name: "Customer Rules"
  version: "1.0.0"

rules:
  - id: "age-check"
    name: "Age Validation"
    condition: "#age >= 18"
    message: "Customer must be 18 or older"
  - id: "email-validation"
    name: "Email Validation"
    condition: "#email != null && #email.contains('@')"
    message: "Valid email required"
```

## Implementation Benefits

1. **✅ Consistent Pattern**: Follows proven external data source approach
2. **✅ Backward Compatible**: No rule-refs = works exactly as before
3. **✅ File System Support**: Works with both classpath and file system paths
4. **✅ Proper Validation**: Validation happens after all merging is complete
5. **✅ Clear Errors**: Meaningful error messages for missing/invalid files
6. **✅ Performance**: Reuses existing loading infrastructure

## Risk Mitigation

1. **Circular References**: Referenced files loaded without processing (no recursion)
2. **Performance**: Minimal overhead - only processes when rule-refs present
3. **Memory**: Rules merged into existing lists (no duplication)
4. **Errors**: Clear error messages with file paths and reference names

## Testing Strategy

1. **Unit Tests**: Test YamlRuleRef class and processRuleReferences() method
2. **Integration Tests**: Test complete loading with rule references
3. **Regression Tests**: Ensure existing functionality unchanged
4. **Error Tests**: Test missing files, invalid syntax, circular references

---

**Design Complete**: Ready to implement following this exact pattern.
