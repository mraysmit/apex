# APEX Deprecated Methods Migration Plan

**Created**: 2025-11-01
**Status**: 🎯 **READY TO EXECUTE**
**Scope**: Systematically update all apex-demo tests to use new static factory methods

---

## 📊 Migration Scope

### **Total Files to Update**: 42 test files + 1 base class

### **Files by Folder**:

| Folder | Files | Complexity | Priority |
|--------|-------|------------|----------|
| **basic** | 14 | Low | 1 (Start here) |
| **rulegroups** | 8 | Medium | 2 |
| **lookup** | 7 | Medium | 3 |
| **sequencing** | 5 | Medium | 4 |
| **metrics** | 5 | Low | 5 |
| **conditional** | 2 | Low | 6 |
| **DemoTestBase.java** | 1 | Low | 7 (Last) |

**Total**: 43 files

---

## 🎯 Migration Strategy

### **Approach**: Folder-by-Folder Migration

**Why folder-by-folder?**
1. ✅ Logical grouping of related tests
2. ✅ Easy to track progress
3. ✅ Can test each folder independently
4. ✅ Minimizes risk of breaking multiple areas at once
5. ✅ Clear stopping points for review

### **Migration Pattern**

**From** (Deprecated):
```java
YamlRulesEngineService service = new YamlRulesEngineService();
RulesEngine engine = service.createRulesEngineFromFile("path/to/config.yaml");
RuleResult result = engine.evaluate(config, testData);
```

**To** (New Static Factory):
```java
RulesEngine engine = RulesEngine.fromFile("path/to/config.yaml");
RuleResult result = engine.evaluate(testData);
```

---

## 📋 Folder-by-Folder Plan

### **Phase 1: basic/ (14 files) - PRIORITY 1**

**Complexity**: Low (simple validation rules, no complex dependencies)

**Files**:
1. BasicYamlRuleGroupProcessingATest.java
2. MinimalRuleTest.java
3. MinimalYamlValidationTest.java
4. SeverityComprehensiveTest.java
5. SeverityDefaultBehaviorTest.java
6. SeverityEdgeCasesTest.java
7. SeverityMixedRulesTest.java
8. SeverityNegativeTest.java
9. SeverityRuleGroupTest.java
10. SimpleAgeValidationTest.java
11. SimpleBasicYamlRuleGroupProcessingTest.java
12. SimpleValidationRuleTest.java
13. SimpleYamlValidationDemo.java
14. ValueThresholdRuleTest.java

**Estimated Time**: 1-2 hours

**Testing**: `mvn test -Dtest="dev.mars.apex.demo.basic.*"`

---

### **Phase 2: rulegroups/ (8 files) - PRIORITY 2**

**Complexity**: Medium (includes cross-file references, createRulesEngineFromMultipleFiles)

**Files**:
1. BasicYamlRuleGroupProcessingTest.java
2. CrossFileRuleGroupReferenceTest.java ⚠️ (uses createRulesEngineFromMultipleFiles)
3. RuleGroupSeverityAggregationTest.java
4. SimpleCrossFileTest.java ⚠️ (uses createRulesEngineFromMultipleFiles)
5. SimpleInlineRuleGroupStandaloneTest.java
6. SimpleInlineRuleGroupTest.java
7. StopOnFirstFailureAndGroupTest.java
8. StopOnFirstFailureOrGroupTest.java

**Special Handling**:
- Files using `createRulesEngineFromMultipleFiles` need special migration strategy
- May need to use `YamlConfigurationLoader` to merge configs manually

**Estimated Time**: 2-3 hours

**Testing**: `mvn test -Dtest="dev.mars.apex.demo.rulegroups.*"`

---

### **Phase 3: lookup/ (7 files) - PRIORITY 3**

**Complexity**: Medium (external data sources, enrichments)

**Files**:
1. BarrierOptionNestedEnrichmentTest.java
2. BarrierOptionNestedValidationTest.java
3. BasicUsageExamplesTest.java
4. CalculationMathematicalTest.java
5. LookupBasicInlineTest.java
6. RestApiIntegrationTest.java
7. TradeTransformerDemoTest.java

**Estimated Time**: 2-3 hours

**Testing**: `mvn test -Dtest="dev.mars.apex.demo.lookup.*"`

---

### **Phase 4: sequencing/ (5 files) - PRIORITY 4**

**Complexity**: Medium (sequential processing, dependencies)

**Files**:
1. AMinimalSequentialProcessingTest.java
2. ComprehensiveValidationTest.java
3. OrderedYamlParserComplexTest.java
4. RuleGroupsSequentialBasicTest.java
5. SequentialYamlProcessorTest.java

**Estimated Time**: 1-2 hours

**Testing**: `mvn test -Dtest="dev.mars.apex.demo.sequencing.*"`

---

### **Phase 5: metrics/ (5 files) - PRIORITY 5**

**Complexity**: Low (performance metrics, error recovery)

**Files**:
1. BasicPerformanceMetricsDemo.java
2. MetricsCollectionDemo.java
3. RecoveryPerformanceImpactDemo.java
4. RecoveryStrategyComparisonDemo.java
5. SimpleErrorRecoveryDemo.java

**Estimated Time**: 1-2 hours

**Testing**: `mvn test -Dtest="dev.mars.apex.demo.metrics.*"`

---

### **Phase 6: conditional/ (2 files) - PRIORITY 6**

**Complexity**: Low (conditional logic tests)

**Files**:
1. UpdateStageFxTransactionApexTest.java
2. UpdateStageFxTransactionSimplifiedTest.java

**Estimated Time**: 30 minutes

**Testing**: `mvn test -Dtest="dev.mars.apex.demo.conditional.*"`

---

### **Phase 7: DemoTestBase.java (1 file) - PRIORITY 7**

**Complexity**: Low (base class utility methods)

**File**:
1. DemoTestBase.java

**Special Handling**:
- This is a base class used by many tests
- Update utility methods to use new pattern
- Test all dependent tests after update

**Estimated Time**: 30 minutes

**Testing**: `mvn test` (full apex-demo test suite)

---

## 🔧 Migration Workflow (Per Folder)

### **Step 1: Prepare**
```bash
# Create a branch for the folder
git checkout -b migrate-deprecated-methods-<folder-name>
```

### **Step 2: Update Files**
- Use str-replace-editor to update each file
- Replace deprecated patterns with static factory methods
- Remove unused imports (YamlRulesEngineService)
- Update evaluate() calls to use simplified signature

### **Step 3: Test**
```bash
# Test the specific folder
cd apex-demo
mvn test -Dtest="dev.mars.apex.demo.<folder>.*"
```

### **Step 4: Verify**
- Check test output for failures
- Scan logs for errors (not just exit codes)
- Verify no deprecation warnings for updated files

### **Step 5: Commit**
```bash
git add .
git commit -m "Migrate <folder> tests to use RulesEngine static factory methods"
```

### **Step 6: Full Test**
```bash
# Run full apex-demo test suite
mvn clean test
```

---

## ⚠️ Special Cases

### **createRulesEngineFromMultipleFiles**

**Files Affected**:
- CrossFileRuleGroupReferenceTest.java
- SimpleCrossFileTest.java

**Migration Strategy**:
```java
// OLD: createRulesEngineFromMultipleFiles
YamlRulesEngineService service = new YamlRulesEngineService();
RulesEngine engine = service.createRulesEngineFromMultipleFiles(file1, file2);

// NEW: Manual merge + static factory
YamlConfigurationLoader loader = new YamlConfigurationLoader();
YamlRuleConfiguration config1 = loader.loadFromFile(file1);
YamlRuleConfiguration config2 = loader.loadFromFile(file2);

// Merge configs manually (or use utility method)
YamlRuleConfiguration merged = mergeConfigs(config1, config2);

RulesEngine engine = RulesEngine.fromYamlConfig(merged);
```

---

## 📈 Progress Tracking

### **Completion Checklist**

- [ ] Phase 1: basic/ (14 files)
- [ ] Phase 2: rulegroups/ (8 files)
- [ ] Phase 3: lookup/ (7 files)
- [ ] Phase 4: sequencing/ (5 files)
- [ ] Phase 5: metrics/ (5 files)
- [ ] Phase 6: conditional/ (2 files)
- [ ] Phase 7: DemoTestBase.java (1 file)
- [ ] Full apex-demo test suite passing
- [ ] No deprecation warnings in apex-demo
- [ ] Documentation updated

---

## 🎯 Success Criteria

1. ✅ All 43 files updated to use static factory methods
2. ✅ All apex-demo tests passing
3. ✅ No deprecation warnings in apex-demo module
4. ✅ Code is cleaner and more maintainable
5. ✅ Migration documented for future reference

---

## 📝 Notes

- **Estimated Total Time**: 8-12 hours
- **Can be done incrementally**: Each phase is independent
- **Low risk**: Each folder can be tested independently
- **Reversible**: Can revert individual commits if needed
- **Clear progress**: Easy to see what's done and what's left

---

## 🚀 Ready to Start?

**Recommended**: Start with Phase 1 (basic/) as it's the simplest and will establish the pattern for the rest.

**Command to begin**:
```bash
cd apex-demo
mvn test -Dtest="dev.mars.apex.demo.basic.*"
```

This will show current test status before migration begins.

