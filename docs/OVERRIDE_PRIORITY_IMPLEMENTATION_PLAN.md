# Override Priority Implementation Plan

**Feature**: `override-priority` property in `rule-references`
**Status**: Documented but not implemented
**Priority**: Medium
**Estimated Effort**: 2-3 days

## Overview

The `override-priority` feature allows rule groups to override the default priority of individual rules, enabling context-sensitive rule behavior without duplicating rule definitions.

### Simplest Use Case Example

**Scenario**: A validation rule that needs different priority in different contexts.

```yaml
metadata:
  name: "Priority Override Demo"
  version: "1.0.0"

rules:
  - id: "data-validation"
    name: "Data Validation Rule"
    condition: "#value != null && #value > 0"
    message: "Data validation passed"
    severity: "ERROR"
    priority: 50  # Default medium priority

rule-groups:
  # Critical processing - validation is highest priority
  - id: "critical-processing"
    name: "Critical Data Processing"
    operator: "AND"
    rule-references:
      - rule-id: "data-validation"
        sequence: 1
        enabled: true
        override-priority: 1    # Override to highest priority

  # Batch processing - validation is lower priority
  - id: "batch-processing"
    name: "Batch Data Processing"
    operator: "AND"
    rule-references:
      - rule-id: "data-validation"
        sequence: 1
        enabled: true
        override-priority: 100  # Override to lowest priority
```

**Result**: The same `data-validation` rule executes with priority 1 in critical processing and priority 100 in batch processing, while maintaining its original priority 50 for any other usage.



## Current State

### ✅ What's Already Done
- YAML syntax is defined and documented
- `RuleReference` class has `overridePriority` property
- YAML parsing correctly reads the property
- Documentation includes examples and use cases

### ❌ What's Missing
- Engine implementation ignores the `override-priority` property
- No validation of priority values
- No tests for priority override behavior

## Implementation Plan

### Phase 1: Core Implementation (Day 1)

#### Step 1.1: Modify `YamlRuleFactory.addRulesToGroup()`

**File**: `apex-core/src/main/java/dev/mars/apex/core/config/yaml/YamlRuleFactory.java`

**Current Code** (lines 530-544):
```java
// Add rules by reference (with more detailed configuration)
if (yamlGroup.getRuleReferences() != null) {
    for (YamlRuleGroup.RuleReference ref : yamlGroup.getRuleReferences()) {
        if (ref.getEnabled() == null || ref.getEnabled()) {
            Rule rule = config.getRuleById(ref.getRuleId());
            if (rule != null) {
                int sequence = ref.getSequence() != null ? ref.getSequence() : 1;
                group.addRule(rule, sequence);
                LOGGER.fine("Added rule " + ref.getRuleId() + " to group " + group.getId() + " with sequence " + sequence);
            } else {
                LOGGER.warning("Rule not found for ID: " + ref.getRuleId() + " in group: " + group.getId());
            }
        }
    }
}
```

**New Code**:
```java
// Add rules by reference (with more detailed configuration)
if (yamlGroup.getRuleReferences() != null) {
    for (YamlRuleGroup.RuleReference ref : yamlGroup.getRuleReferences()) {
        if (ref.getEnabled() == null || ref.getEnabled()) {
            Rule originalRule = config.getRuleById(ref.getRuleId());
            if (originalRule != null) {
                int sequence = ref.getSequence() != null ? ref.getSequence() : 1;
                
                // Handle priority override
                Rule ruleToAdd = originalRule;
                if (ref.getOverridePriority() != null) {
                    validatePriorityOverride(ref.getOverridePriority(), ref.getRuleId());
                    ruleToAdd = createRuleWithOverriddenPriority(originalRule, ref.getOverridePriority(), yamlGroup.getId());
                    LOGGER.fine("Applied priority override " + ref.getOverridePriority() + " to rule " + ref.getRuleId() + " in group " + yamlGroup.getId());
                }
                
                group.addRule(ruleToAdd, sequence);
                LOGGER.fine("Added rule " + ref.getRuleId() + " to group " + group.getId() + " with sequence " + sequence);
            } else {
                LOGGER.warning("Rule not found for ID: " + ref.getRuleId() + " in group: " + group.getId());
            }
        }
    }
}
```

#### Step 1.2: Add Helper Methods

**Add to `YamlRuleFactory` class**:

```java
/**
 * Validate priority override value.
 */
private void validatePriorityOverride(Integer priority, String ruleId) {
    if (priority == null) return;
    
    if (priority < 1) {
        throw new YamlConfigurationException(
            "override-priority must be >= 1 for rule: " + ruleId + ", got: " + priority);
    }
    
    if (priority > 1000) {
        LOGGER.warning("Very high priority override (" + priority + ") for rule: " + ruleId + 
                      ". Consider using priorities between 1-100.");
    }
}

/**
 * Create a copy of a rule with overridden priority for use in a specific rule group.
 */
private Rule createRuleWithOverriddenPriority(Rule originalRule, int newPriority, String groupId) {
    // Create new categories with overridden priority
    Set<Category> newCategories = originalRule.getCategories().stream()
        .map(cat -> new Category(cat.getName(), newPriority))
        .collect(Collectors.toSet());
    
    // Create unique ID for this group-specific rule instance
    String newRuleId = originalRule.getId() + "_group_" + groupId + "_priority_" + newPriority;
    
    // Create new rule with same properties but different priority and ID
    Rule newRule = new Rule(
        newRuleId,
        newCategories,
        originalRule.getName(),
        originalRule.getCondition(),
        originalRule.getMessage(),
        originalRule.getDescription(),
        newPriority,
        originalRule.getMetadata()
    );
    
    return newRule;
}
```

#### Step 1.3: Add Import Statement

**Add to imports in `YamlRuleFactory.java`**:
```java
import java.util.stream.Collectors;
```

### Phase 2: Testing Implementation (Day 2)

#### Step 2.1: Create Test Class

**File**: `apex-demo/src/test/java/dev/mars/apex/demo/rulegroups/OverridePriorityTest.java`

**Key Test Cases**:
1. `testOverridePriorityChangesRuleExecution()` - Verify priority affects execution order
2. `testOriginalRuleUnchanged()` - Ensure original rule priority is preserved
3. `testMultipleGroupsWithDifferentPriorities()` - Same rule, different priorities in different groups
4. `testInvalidPriorityValidation()` - Test validation of invalid priority values
5. `testPriorityOverrideWithSequence()` - Combined priority override and sequence control

#### Step 2.2: Update Existing Tests

**Files to Update**:
- `BasicYamlRuleGroupProcessingBTest.java` - Remove `override-priority` from existing tests (since it will now work)
- Add priority override examples to existing test configurations

### Phase 3: Documentation and Validation (Day 3)

#### Step 3.1: Update Documentation

**Files to Update**:
- `docs/APEX_YAML_REFERENCE.md` - Change status from "PLANNED FEATURE" to "COMPLETE"
- Add working examples with actual priority override behavior
- Update implementation status table

#### Step 3.2: Integration Testing

**Test Scenarios**:
1. Complex rule groups with mixed priority overrides
2. Performance impact assessment
3. Edge cases (null priorities, duplicate priorities, etc.)
4. Backward compatibility verification

## Technical Considerations

### 🎯 Key Design Decisions

1. **Rule Copying Approach**: Create rule copies with new priorities (preserves original rules)
   - **Chosen**: Create rule copies with new priorities
   - **Rationale**: Preserves original rule for other uses, avoids side effects
   - **Implementation**: New rule instances with modified priority, original rules unchanged

2. **Unique ID Generation**: `{originalId}_group_{groupId}_priority_{priority}`
   - **Pattern**: `{originalId}_group_{groupId}_priority_{priority}`
   - **Rationale**: Ensures uniqueness while maintaining traceability
   - **Example**: `credit-limit-check_group_vip-processing_priority_1`

3. **Priority Validation**: Range 1-1000 with warnings for >100
   - **Range**: 1-1000 (with warning for >100)
   - **Rationale**: Prevents invalid values while allowing flexibility
   - **Validation**: Strict enforcement of minimum value, warnings for unusually high values

4. **Backward Compatibility**: 100% maintained - feature is purely additive
   - **Approach**: Purely additive feature with no breaking changes
   - **Existing Configurations**: Continue to work without modification
   - **Migration**: Optional upgrade path available

### 💡 Use Cases Documented

1. **Context-Sensitive Priority**: Same rule, different priorities in different contexts
   - **Example**: Settlement date validation - critical in HFT, lower priority in batch processing
   - **Benefit**: Single rule definition, context-appropriate behavior

2. **Regulatory Compliance**: KYC rules with higher priority for high-value transactions
   - **Example**: KYC verification - highest priority for large transactions, warning level for standard
   - **Benefit**: Compliance requirements met without rule duplication

3. **Environment-Specific**: Different priorities across dev/staging/production
   - **Example**: API timeout checks - critical in production, informational in development
   - **Benefit**: Same configuration, environment-appropriate behavior

4. **Customer Tiers**: VIP vs standard customer processing
   - **Example**: Credit limit checks - flexible for VIP customers, strict for standard customers
   - **Benefit**: Differentiated service levels with shared rule logic

5. **Seasonal Adjustments**: Holiday trading with adjusted priorities
   - **Example**: Market hours validation - relaxed during holidays, strict during normal trading
   - **Benefit**: Temporal rule behavior without configuration changes

### Performance Impact

- **Memory**: Minimal increase (rule copies only created when override is used)
- **CPU**: Negligible overhead during rule group creation
- **Runtime**: No impact on rule execution performance

### Backward Compatibility

- **Existing Configurations**: No changes required
- **API Compatibility**: No breaking changes
- **Migration**: Seamless - feature is additive only

## Testing Strategy

### Unit Tests
- Rule creation with priority override
- Validation of priority values
- Original rule preservation

### Integration Tests
- End-to-end rule group execution with priority overrides
- Multiple groups using same rule with different priorities
- Complex scenarios with sequence + priority + enabled combinations

### Performance Tests
- Memory usage with large numbers of priority overrides
- Rule group creation performance impact

## Acceptance Criteria

### Functional Requirements
- ✅ `override-priority` property is processed correctly
- ✅ Rule execution order respects overridden priorities
- ✅ Original rules remain unchanged
- ✅ Invalid priority values are rejected with clear error messages
- ✅ Feature works with both AND and OR rule groups
- ✅ Compatible with `sequence` and `enabled` properties

### Non-Functional Requirements
- ✅ Performance impact < 5% for rule group creation
- ✅ Memory overhead < 10% for configurations using priority overrides
- ✅ 100% backward compatibility maintained
- ✅ Comprehensive test coverage (>95%)

## Risks and Mitigation

### Risk 1: Performance Impact
- **Mitigation**: Lazy rule copying (only when override is used)
- **Monitoring**: Performance tests in CI/CD pipeline

### Risk 2: Memory Usage
- **Mitigation**: Efficient rule copying, shared metadata objects
- **Monitoring**: Memory profiling tests

### Risk 3: Complexity
- **Mitigation**: Clear documentation, comprehensive examples
- **Monitoring**: User feedback and support tickets

## Rollout Plan

### Phase 1: Internal Testing
- Implement feature in development branch
- Run comprehensive test suite
- Performance validation

### Phase 2: Beta Release
- Release to select users for feedback
- Monitor for issues and edge cases
- Refine based on feedback

### Phase 3: General Availability
- Full release with updated documentation
- Blog post with examples and use cases
- Training materials for users

## Success Metrics

- **Adoption**: >20% of rule groups use priority overrides within 6 months
- **Performance**: <5% impact on rule group creation time
- **Quality**: Zero critical bugs related to priority override functionality
- **User Satisfaction**: Positive feedback on feature usefulness and ease of use
