# Phase 4 Cleanup Plan

## Overview
Phase 4 involves safely removing the old directory structure while preserving all functionality through the new organized structure. This cleanup will:

1. **Remove Old Directories**: Clean up the legacy directory structure
2. **Update ResourcePathResolver**: Transition from migration utility to direct path usage
3. **Update Java Classes**: Remove ResourcePathResolver usage and use new paths directly
4. **Optimize Performance**: Remove the overhead of path resolution
5. **Final Validation**: Ensure everything works with the new structure

## Files to Remove (Old Structure)

### Old Directories to Remove:
- `apex-demo/src/main/resources/bootstrap/` (migrated to `demos/bootstrap/`)
- `apex-demo/src/main/resources/config/` (migrated to `demos/fundamentals/rules/`)
- `apex-demo/src/main/resources/demo-configs/` (migrated to `demos/patterns/lookups/`)
- `apex-demo/src/main/resources/demo-rules/` (migrated to `demos/quickstart/` and `demos/industry/`)
- `apex-demo/src/main/resources/examples/` (migrated to `demos/patterns/`)
- `apex-demo/src/main/resources/financial-settlement/` (migrated to `demos/industry/financial-services/settlement/`)
- `apex-demo/src/main/resources/scenarios/` (migrated to `demos/advanced/complex-scenarios/`)
- `apex-demo/src/main/resources/yaml-examples/` (migrated to `reference/syntax-examples/`)

### Old Files to Remove:
- `apex-demo/src/main/resources/batch-processing.yaml` (standalone file, check if used)
- `apex-demo/src/main/resources/file-processing-rules.yaml` (standalone file, check if used)

## Java Classes to Update

### Remove ResourcePathResolver Usage:
1. Update all Java classes to use new paths directly
2. Remove ResourcePathResolver imports
3. Update hardcoded path strings to new locations

### Classes to Update:
- `SimpleFieldLookupDemo.java`
- `ConditionalExpressionLookupDemo.java`
- `NestedFieldLookupDemo.java`
- `CompoundKeyLookupDemo.java`
- `ComprehensiveFinancialSettlementDemo.java`
- `CustodyAutoRepairBootstrap.java`
- `ComprehensiveLookupDemo.java`
- `YamlConfiguredFileProcessingDemo.java`
- `CustodyAutoRepairYamlDemo.java`
- `QuickStartDemo.java`
- `YamlFilesCoverageAnalysisTest.java`
- `CustodyAutoRepairYamlTest.java`

## ResourcePathResolver Transition

### Option 1: Remove Completely
- Remove the ResourcePathResolver class entirely
- Update all references to use new paths directly
- Clean and optimized approach

### Option 2: Keep as Reference (Recommended)
- Keep ResourcePathResolver for documentation purposes
- Add deprecation warnings
- Update documentation to show the migration history
- Useful for understanding the reorganization

## Safety Measures

### Before Cleanup:
1. **Full Backup**: Create backup of current state
2. **Validation**: Run all existing validation scripts
3. **Test Coverage**: Ensure all demos work with current structure

### During Cleanup:
1. **Incremental Approach**: Remove directories one by one
2. **Continuous Testing**: Test after each major change
3. **Rollback Plan**: Keep ability to restore if issues arise

### After Cleanup:
1. **Final Validation**: Run comprehensive tests
2. **Performance Testing**: Verify improved performance
3. **Documentation Update**: Update all documentation

## Expected Benefits

### Performance Improvements:
- **Faster Startup**: No path resolution overhead
- **Reduced Memory**: No migration mappings in memory
- **Cleaner Code**: Direct path usage, no indirection

### Maintainability Improvements:
- **Simpler Structure**: Only new organized structure exists
- **Clear Paths**: No confusion between old and new paths
- **Reduced Complexity**: No migration logic to maintain

### Developer Experience:
- **Intuitive Paths**: Developers use logical, organized paths
- **Better IDE Support**: Direct path references work better with IDEs
- **Cleaner Codebase**: No legacy migration code

## Validation Strategy

### Pre-Cleanup Validation:
1. Run `validate_migration.py` to ensure current state is good
2. Run `validate_phase3.py` to ensure Phase 3 is complete
3. Test key demo classes manually

### Post-Cleanup Validation:
1. Create `validate_phase4.py` for final validation
2. Test all demo runners work correctly
3. Verify no broken references exist
4. Confirm performance improvements

## Rollback Plan

If issues are discovered:
1. **Git Reset**: Use version control to restore previous state
2. **Selective Restore**: Restore specific directories if needed
3. **ResourcePathResolver**: Re-enable if temporary compatibility needed

## Timeline

### Estimated Duration: 30-45 minutes
1. **Preparation** (5 min): Backup and validation
2. **Java Class Updates** (15 min): Remove ResourcePathResolver usage
3. **Directory Cleanup** (10 min): Remove old directories
4. **Final Testing** (10 min): Comprehensive validation
5. **Documentation** (5 min): Update final documentation

## Success Criteria

Phase 4 is complete when:
- ✅ All old directories are removed
- ✅ All Java classes use new paths directly
- ✅ All demos work correctly
- ✅ Performance is improved (no path resolution overhead)
- ✅ Documentation is updated
- ✅ Final validation passes 100%
