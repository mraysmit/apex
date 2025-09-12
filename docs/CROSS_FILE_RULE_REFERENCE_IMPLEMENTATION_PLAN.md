# Cross-File Rule Reference Implementation Plan

## Overview

This document outlines the implementation plan for adding cross-file rule reference resolution to the APEX Rules Engine. The goal is to enable rule groups to reference rules defined in separate YAML files, similar to how external data source references currently work.

## Problem Statement

### Current Issue
APEX currently fails when rule groups reference rules defined in separate files:
- **Single file loading**: `yamlLoader.loadFromClasspath("rule-groups.yaml")` fails if rules are in separate files
- **Multi-file loading**: `createRulesEngineFromMultipleFiles()` fails due to validation timing issues
- **Error**: "Rule reference not found: Rule 'rule-id' referenced in rule group 'group-id' does not exist"

### Root Cause
APEX validates rule references during individual file loading, before files are merged. Referenced rules in other files are not yet available during validation.

### Working Pattern (External Data Sources)
External data sources work correctly because they:
1. Load referenced files during `processDataSourceReferences()`
2. Merge external configurations into current configuration
3. Validate AFTER merging is complete

## Solution Approach

Implement the same pattern for rule references that already works for external data sources.

## Implementation Plan

### Phase 1: Analysis and Design

#### Task 1.1: Document Current State
**Objective**: Create comprehensive documentation of existing patterns

**Deliverables**:
- Document external data source processing flow
- Document current rule reference validation failure points
- Create side-by-side comparison of working vs broken patterns

**Files to Analyze**:
- `YamlConfigurationLoader.processDataSourceReferences()`
- `DataSourceResolver.resolveDataSource()`
- `YamlConfigurationLoader.validateCrossComponentReferences()`

**Estimated Time**: 1-2 hours

#### Task 1.2: Design Rule Reference Resolution
**Objective**: Design the rule reference system architecture

**Design Components**:
1. **YamlRuleRef Class**: Similar to `YamlDataSourceRef`
2. **RuleResolver Service**: Similar to `DataSourceResolver`  
3. **YAML Syntax**: Consistent with external data source syntax
4. **Processing Flow**: Mirror external data source processing

**Key Design Decisions**:
- Use `rule-refs` section in YAML (parallel to `data-source-refs`)
- Support both classpath and file system resolution
- Maintain backward compatibility
- Process rule references before validation

**Estimated Time**: 1-2 hours

### Phase 2: Core Implementation

#### Task 2.1: Create Rule Reference Infrastructure
**Objective**: Build the foundational classes and structures

**Implementation Steps**:
1. Create `YamlRuleRef` class
2. Add `rule-refs` field to `YamlRuleConfiguration`
3. Create `RuleResolver` service class
4. Add rule reference caching mechanism

**Files to Create/Modify**:
- `apex-core/src/main/java/dev/mars/apex/core/config/yaml/YamlRuleRef.java` (new)
- `apex-core/src/main/java/dev/mars/apex/core/service/rules/RuleResolver.java` (new)
- `apex-core/src/main/java/dev/mars/apex/core/config/yaml/YamlRuleConfiguration.java` (modify)

**YAML Syntax Design**:
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

rule-groups:
  - id: "customer-validation"
    name: "Customer Validation Group"
    rule-ids:
      - "age-check"        # Resolved from customer-rules.yaml
      - "email-validation"  # Resolved from customer-rules.yaml
      - "credit-check"      # Resolved from financial-rules.yaml
```

**Estimated Time**: 2-3 hours

#### Task 2.2: Implement Rule Reference Processing
**Objective**: Add rule reference processing to configuration loading

**Implementation Steps**:
1. Add `processRuleReferences()` method to `YamlConfigurationLoader`
2. Implement rule file loading and merging logic
3. Update both `loadFromFile()` methods to call `processRuleReferences()`
4. Ensure processing happens before validation

**Processing Flow**:
```
1. Load main YAML file
2. Parse rule-refs section
3. For each rule reference:
   - Load referenced rule file
   - Extract rules from referenced file
   - Merge rules into main configuration
4. Process data-source-refs (existing)
5. Validate complete merged configuration
```

**Files to Modify**:
- `apex-core/src/main/java/dev/mars/apex/core/config/yaml/YamlConfigurationLoader.java`

**Key Methods**:
- `processRuleReferences(YamlRuleConfiguration config)`
- `mergeRulesFromReference(YamlRuleConfiguration target, YamlRuleConfiguration source, YamlRuleRef ref)`

**Estimated Time**: 2-3 hours

#### Task 2.3: Update Validation Logic
**Objective**: Ensure validation works correctly with merged configurations

**Implementation Steps**:
1. Verify validation order (rule processing → data source processing → validation)
2. Test cross-component validation with merged rules
3. Ensure no regression in single-file scenarios
4. Add proper error handling for rule reference failures

**Validation Flow**:
```
loadFromFile() {
    1. Parse YAML → YamlRuleConfiguration
    2. processRuleReferences(config)     // NEW: Before validation
    3. processDataSourceReferences(config) // EXISTING: Before validation  
    4. validateConfiguration(config)       // EXISTING: After all processing
}
```

**Files to Verify**:
- `YamlConfigurationLoader.validateConfiguration()`
- `YamlConfigurationLoader.validateCrossComponentReferences()`

**Estimated Time**: 1-2 hours

### Phase 3: Testing and Validation

#### Task 3.1: Create Comprehensive Tests
**Objective**: Ensure rule reference resolution works in all scenarios

**Test Categories**:
1. **Basic Rule References**: Simple rule group referencing external rules
2. **Classpath Loading**: Rule references from classpath resources
3. **File System Loading**: Rule references from file system paths
4. **Mixed Scenarios**: Rules + rule groups + external data sources together
5. **Error Handling**: Missing files, circular references, invalid syntax
6. **Performance**: Large rule sets, caching behavior

**Test Files to Create**:
- `ClasspathRuleReferenceTest.java`
- `FileSystemRuleReferenceTest.java` 
- `MixedReferenceIntegrationTest.java`
- `RuleReferenceErrorHandlingTest.java`

**Test Resources**:
- Sample rule files for testing
- Sample rule group files with references
- Invalid reference scenarios

**Estimated Time**: 2-3 hours

#### Task 3.2: Update Existing Tests
**Objective**: Ensure no regressions and verify fixes

**Tests to Update**:
- `ClasspathRuleGroupProcessingTest` should now PASS
- `SeparateFilesRuleGroupProcessingTest` should still work
- All existing `YamlConfigurationLoader` tests should pass

**Regression Testing**:
- Run full apex-core test suite
- Run apex-demo test suite  
- Verify performance benchmarks

**Estimated Time**: 1-2 hours

#### Task 3.3: Integration Testing
**Objective**: Test end-to-end scenarios

**Integration Scenarios**:
1. `createRulesEngineFromMultipleFiles` with rule references
2. Single file loading with rule references
3. Mixed loading scenarios
4. Real-world configuration examples

**Performance Testing**:
- Measure rule reference resolution overhead
- Test caching effectiveness
- Verify memory usage patterns

**Estimated Time**: 1-2 hours

### Phase 4: Documentation and Cleanup

#### Task 4.1: Update Documentation
**Objective**: Document the new rule reference capabilities

**Documentation Updates**:
1. **APEX_YAML_REFERENCE.md**: Add rule-refs syntax documentation
2. **RULE_REFERENCES_FEATURE_SUMMARY.md**: Update with new capabilities
3. **User Guide**: Add examples and best practices
4. **Technical Reference**: Document API changes

**Example Documentation**:
```yaml
# Rule Reference Syntax
rule-refs:
  - name: "validation-rules"      # Logical name for the reference
    source: "rules/validation.yaml"  # Path to rule file (classpath or file system)
    enabled: true                 # Optional: enable/disable reference
    description: "Core validation rules"  # Optional: description
```

**Estimated Time**: 1-2 hours

#### Task 4.2: Code Cleanup
**Objective**: Finalize implementation with proper standards

**Cleanup Tasks**:
1. Remove temporary workarounds or debug code
2. Ensure consistent error messages and logging
3. Add comprehensive JavaDoc documentation
4. Verify code style and formatting
5. Update version numbers if needed

**Code Review Checklist**:
- [ ] All new classes have proper JavaDoc
- [ ] Error messages are user-friendly and actionable
- [ ] Logging levels are appropriate
- [ ] No hardcoded paths or values
- [ ] Proper exception handling
- [ ] Thread safety considerations

**Estimated Time**: 1 hour

## Success Criteria

### Functional Requirements
- [ ] Rule groups can reference rules in separate files
- [ ] Works with both classpath and file system loading
- [ ] `ClasspathRuleGroupProcessingTest` passes
- [ ] `createRulesEngineFromMultipleFiles` works with rule references
- [ ] Backward compatibility maintained (no rule-refs = works as before)

### Non-Functional Requirements  
- [ ] Performance impact < 10% for typical configurations
- [ ] Memory usage remains reasonable with caching
- [ ] Error messages are clear and actionable
- [ ] Full test coverage (>90%) for new functionality

### Integration Requirements
- [ ] All existing tests continue to pass
- [ ] No breaking changes to public APIs
- [ ] Documentation is complete and accurate
- [ ] Examples work as documented

## Risk Mitigation

### Technical Risks
- **Validation Timing**: Ensure rule processing happens before validation
- **Circular References**: Implement detection and prevention
- **Performance Impact**: Use caching and lazy loading where appropriate
- **Memory Leaks**: Proper cleanup of cached rule configurations

### Implementation Risks
- **Backward Compatibility**: Extensive testing of existing functionality
- **Complex Scenarios**: Test mixed rule references and data source references
- **Error Handling**: Comprehensive error scenarios and user-friendly messages

## Timeline Estimate

| Phase | Tasks | Estimated Time | Dependencies |
|-------|-------|----------------|--------------|
| Phase 1 | Analysis & Design | 2-4 hours | None |
| Phase 2 | Core Implementation | 5-8 hours | Phase 1 complete |
| Phase 3 | Testing & Validation | 4-7 hours | Phase 2 complete |
| Phase 4 | Documentation & Cleanup | 2-3 hours | Phase 3 complete |
| **Total** | **All Phases** | **13-22 hours** | Sequential |

## Next Steps

1. **Get Approval**: Review and approve this implementation plan
2. **Start Phase 1**: Begin with analysis and design documentation
3. **Incremental Implementation**: Complete each phase before moving to next
4. **Regular Check-ins**: Review progress and adjust plan as needed
5. **Final Validation**: Comprehensive testing before completion

---

**Document Version**: 1.0  
**Created**: 2025-09-13  
**Author**: APEX Development Team  
**Status**: Draft - Pending Approval
