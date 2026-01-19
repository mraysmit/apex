# YAML Co-Location Pattern

**Module**: `apex-data-sync`  
**Version**: 2.1+  
**Status**: Mandatory Convention

## Overview

The YAML Co-Location Pattern is a strict organizational principle that ensures every YAML configuration file has a corresponding Java test class in the **same directory**. This pattern provides executable documentation, enforces testability, and prevents orphaned configuration files.

## The Pattern

### Core Principle
```
✅ CORRECT: Co-located in same directory
src/test/java/dev/mars/apex/sync/schema/
├── SchemaAnalysisExample.java
└── SchemaAnalysisExample.yaml

❌ WRONG: Orphaned YAML in separate configs folder
configs/
└── schema-analysis-example.yaml
```

### Naming Convention
- **Java test class**: `{BaseName}.java` (PascalCase)
- **YAML configuration**: `{BaseName}.yaml` (PascalCase - **must exactly match** Java class name)
- Both files **must** share the **identical base name** including case
- Both files **must** reside in the same `src/test/java/` subdirectory

**Examples:**
- ✅ `SchemaAnalysisExample.java` + `SchemaAnalysisExample.yaml`
- ✅ `ReadSchemaDatabaseTest.java` + `ReadSchemaDatabaseTest.yaml`
- ❌ `SchemaDiffTest.java` + `schema-diff-test.yaml` (case mismatch)
- ❌ `TestSchema.java` + `test-schema.yaml` (kebab-case not allowed)

## Implementation Guidelines

### Directory Structure
```
apex-data-sync/
└── src/test/java/dev/mars/apex/sync/
    ├── schema/
    │   ├── ReadSchemaDatabaseTest.java
    │   ├── ReadSchemaDatabaseTest.yaml
    │   ├── ReadSchemaCsvTest.java
    │   ├── ReadSchemaCsvTest.yaml
    │   ├── SchemaAnalysisExample.java
    │   └── SchemaAnalysisExample.yaml
    ├── validation/
    │   ├── PreDeploymentValidationTest.java
    │   └── PreDeploymentValidationTest.yaml
    └── transform/
        ├── CustomTypeMappingTest.java
        └── CustomTypeMappingTest.yaml
```

### Java Test Class Template
```java
package dev.mars.apex.sync.schema;

import dev.mars.apex.core.config.yaml.YamlConfigurationLoader;
import dev.mars.apex.core.engine.config.RulesEngine;
import dev.mars.apex.sync.SyncTestBase;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SchemaAnalysisExample extends SyncTestBase {
    
    @Test
    void shouldDemonstrateSchemaAnalysisCapabilities() throws Exception {
        // Load co-located YAML configuration
        var config = yamlLoader.loadFromFile(
            "src/test/java/dev/mars/apex/sync/schema/SchemaAnalysisExample.yaml"
        );
        
        assertNotNull(config, "Configuration should load successfully");
        
        var engine = RulesEngine.fromYamlConfig(config);
        assertNotNull(engine, "Engine should be created from configuration");
        
        // Note: This example demonstrates configuration only
        // Execution patterns vary based on use case
    }
}
```

## Pattern Benefits

### 1. **Executable Documentation**
- YAML configurations are validated through actual test execution
- Examples are guaranteed to work, not just documented wishful thinking
- Changes to core engine immediately surface in test failures

### 2. **Discoverability**
- Developers find related test code and configuration together
- No searching across multiple directories or modules
- Clear ownership: tests own their configurations

### 3. **Prevents Configuration Drift**
- Orphaned YAML files are immediately visible as pattern violations
- Configuration changes require corresponding test updates
- Version control tracks changes to both files together

### 4. **Test Isolation**
- Each test has its own configuration file
- No shared configuration leading to test interdependencies
- Easy to create variations by copying both files together

## Anti-Patterns to Avoid

### ❌ Module-Root Configuration Folders
```
apex-data-sync/
├── configs/                    # WRONG: Breaks co-location
│   └── schema-example.yaml
└── src/test/java/
```

**Problem**: Separates configuration from the code that uses it, creates orphaned files.

### ❌ Shared Configuration Files
```java
// WRONG: Multiple tests sharing one YAML file
class Test1 { loadFromFile("shared-config.yaml"); }
class Test2 { loadFromFile("shared-config.yaml"); }
```

**Problem**: Creates hidden dependencies between tests, makes changes risky.

### ❌ YAML Without Java Test
```
src/test/java/dev/mars/apex/sync/
└── orphaned-config.yaml        # WRONG: No matching .java file
```

**Problem**: Untested configuration, unclear purpose, will eventually break.

## Adoption Metrics

- **Total co-located pairs**: 100+ in apex-data-sync module
- **Adoption rate**: 100% of YAML configurations since v2.1
- **Pattern violations**: 0 (enforced through code review)

## Migration Path

If you find a YAML file in the wrong location:

1. **Identify the correct package**: Where should this configuration be tested?
2. **Move the YAML**: To `src/test/java/{package}/`
3. **Create matching test**: `{BaseName}.java` in same directory
4. **Extend `SyncTestBase`**: Provides `yamlLoader`, `logger`, test setup
5. **Load configuration**: Use relative path to co-located YAML file
6. **Run test**: Verify configuration loads and executes correctly
7. **Clean up**: Remove old location, update any references

### Creating New Test Classes

**IMPORTANT**: When creating Java test classes for orphaned YAML files, always follow the patterns from existing working examples:

1. **Find reference examples**: Look at existing test classes in the same package or similar scenarios
   - Example: `SchemaAnalysisExample.java` for schema-related tests
   - Example: `ReadSchemaDatabaseTest.java` for database schema tests

2. **Copy the structure**:
   - Package declaration matching directory structure
   - Extends `SyncTestBase`
   - Standard imports: `RulesEngine`, `Test`, assertions
   - Descriptive JavaDoc explaining purpose and use cases

3. **Follow the test pattern**:
   ```java
   @Test
   void shouldDescribeWhatThisTests() throws Exception {
       logger.info("\n=== Test Description ===\n");
       var config = yamlLoader.loadFromFile("src/test/java/{package}/{FileName}.yaml");
       assertNotNull(config, "Configuration should load successfully");
       var engine = RulesEngine.fromYamlConfig(config);
       // Additional assertions or execution as needed
   }
   ```

4. **Use consistent naming**:
   - Test method names: `should...` describing expected behavior
   - Variable names: `config`, `engine`, following established conventions
   - Log messages: Consistent formatting with existing tests

5. **Match existing style**: Look at 5-10 working examples in the module to understand:
   - How tests are structured
   - What assertions are used
   - How configurations are loaded
   - When execution vs configuration-only is appropriate

**Don't improvise** - the module has 100+ working examples. Use them as templates to maintain consistency and ensure your new tests integrate seamlessly with the existing test suite.

## Enforcement

This pattern is **mandatory** for all test configurations in apex-data-sync:

- ✅ All YAML files in `src/test/java/` must have matching Java test classes
- ✅ No standalone configuration folders at module root (e.g., `configs/`, `examples/`)
- ✅ YAML and Java files must share the same base name
- ✅ Both files must reside in the same directory

**Code Review Checklist**:
- [ ] Every new YAML file has a matching Java test class
- [ ] Both files are in the same `src/test/java/` subdirectory
- [ ] Test successfully loads and validates the configuration
- [ ] No orphaned YAML files remain in the changeset

---

**Last Updated**: January 19, 2026  
**Pattern Origin**: APEX v2.1 external data-source reference system  
**Enforcement**: Code review + pattern consistency
