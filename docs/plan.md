# APEX Demo Classes to JUnit 5 Tests Conversion Plan

**Version:** 1.0  
**Date:** 2025-09-13  
**Author:** APEX Development Team  

## 📋 **OBJECTIVE**

Convert all classes in `apex-demo/src/main/java/dev/mars/apex/demo` to properly configured JUnit 5 tests following APEX test writing guidelines, making them more reliable and easier to run.

## 📊 **CURRENT STATE ANALYSIS**

### **Main Source Classes to Convert: 73 classes**
- **enrichment/**: 10 classes
- **evaluation/**: 23 classes  
- **lookup/**: 18 classes
- **infrastructure/**: 15 classes
- **etl/**: 1 class
- **validation/**: 5 classes
- **util/**: 3 classes
- **runners/**: 6 classes
- **compiler/**: 1 class
- **model/**: 25 classes (data classes - may not need conversion)

### **Existing Test Classes: 22 classes** (good patterns to follow)

## 🔧 **APEX TEST WRITING GUIDELINES**

### **CORE PRINCIPLES:**
1. ✅ **Never validate YAML syntax** - there are dedicated YAML validation tests
2. ✅ **Always test actual functionality** - execute real APEX operations, not configuration parsing
3. ✅ **Follow existing working patterns** - model after successful tests like `MultiParameterLookupTest.java`
4. ✅ **Set up real data sources** - H2 databases, JSON files, etc.
5. ✅ **Use real APEX services** - EnrichmentService, YamlConfigurationLoader, etc.
6. ✅ **Extend DemoTestBase** for consistent test setup
7. ✅ **Validate functional results** - specific assertions on enriched data and business logic
8. ✅ **Test end-to-end workflows** - complete data transformation pipelines

### **REQUIRED TEST STRUCTURE:**
- Extend `DemoTestBase` for consistent APEX service setup
- Use `@Test` methods with descriptive names
- Import only what you actually use
- Assert on actual enriched data: `assertEquals(expected, result.get("fieldName"))`
- Test with real data that exists in test databases/files
- Validate specific business logic outcomes

## 📋 **CONVERSION PLAN BY CATEGORY**

### **PHASE 1: HIGH-PRIORITY FUNCTIONAL DEMOS (Week 1)**
**Target: 15 classes → 15 test classes**

#### **1.1 Enrichment Demos (10 classes)**
- `BatchProcessingDemo.java` → `BatchProcessingDemoTest.java`
- `ComprehensiveFinancialSettlementDemo.java` → `ComprehensiveFinancialSettlementDemoTest.java` 
- `CustomerTransformerDemo.java` → `CustomerTransformerDemoTest.java`
- `DataManagementDemo.java` → `DataManagementDemoTest.java`
- `ExternalDataSourceDemo.java` → `ExternalDataSourceDemoTest.java`
- `TradeTransformerDemo.java` → `TradeTransformerDemoTest.java`
- `YamlDatasetDemo.java` → `YamlDatasetDemoTest.java`
- `CustodyAutoRepairBootstrap.java` → `CustodyAutoRepairBootstrapTest.java`
- `OtcOptionsBootstrapDemo.java` → `OtcOptionsBootstrapDemoTest.java`
- ✅ `CustodyAutoRepairDemo.java` → `CustodyAutoRepairDemoTest.java` (COMPLETED)

#### **1.2 ETL Demos (1 class)**
- ✅ `CsvToH2PipelineDemo.java` → `CsvToH2PipelineDemoTest.java` (COMPLETED)

#### **1.3 Validation Demos (5 classes)**
- `BasicUsageExamples.java` → `BasicUsageExamplesTest.java`
- `CommoditySwapValidationBootstrap.java` → `CommoditySwapValidationBootstrapTest.java`
- `CommoditySwapValidationQuickDemo.java` → `CommoditySwapValidationQuickDemoTest.java`
- `IntegratedTradeValidatorComplexDemo.java` → `IntegratedTradeValidatorComplexDemoTest.java`
- `IntegratedValidatorDemo.java` → `IntegratedValidatorDemoTest.java`
- `QuickStartDemo.java` → `QuickStartDemoTest.java`

### **PHASE 2: EVALUATION DEMOS (Week 2)**
**Target: 23 classes → 23 test classes**

#### **2.1 Core Evaluation Demos (12 classes)**
- `ApexAdvancedFeaturesDemo.java` → `ApexAdvancedFeaturesDemoTest.java`
- `ApexRulesEngineDemo.java` → `ApexRulesEngineDemoTest.java`
- `ComplianceServiceDemo.java` → `ComplianceServiceDemoTest.java`
- `DynamicMethodExecutionDemo.java` → `DynamicMethodExecutionDemoTest.java`
- `FinancialDemo.java` → `FinancialDemoTest.java`
- `FluentRuleBuilderExample.java` → `FluentRuleBuilderExampleTest.java`
- `LayeredAPIDemo.java` → `LayeredAPIDemoTest.java`
- `PerformanceAndExceptionDemo.java` → `PerformanceAndExceptionDemoTest.java`
- `PerformanceDemo.java` → `PerformanceDemoTest.java`
- `PostTradeProcessingServiceDemo.java` → `PostTradeProcessingServiceDemoTest.java`
- `PricingServiceDemo.java` → `PricingServiceDemoTest.java`
- `RiskManagementService.java` → `RiskManagementServiceTest.java`

#### **2.2 Configuration Demos (11 classes)**
- `RuleConfigurationBootstrap.java` → `RuleConfigurationBootstrapTest.java`
- `RuleConfigurationDemo.java` → `RuleConfigurationDemoTest.java`
- `RuleConfigurationHardcodedBootstrap.java` → `RuleConfigurationHardcodedBootstrapTest.java`
- `RuleConfigurationHardcodedDemo.java` → `RuleConfigurationHardcodedDemoTest.java`
- `RuleDefinitionServiceDemo.java` → `RuleDefinitionServiceDemoTest.java`
- `ScenarioBasedProcessingDemo.java` → `ScenarioBasedProcessingDemoTest.java`
- `SimplifiedAPIDemo.java` → `SimplifiedAPIDemoTest.java`
- `TradeRecordMatcherDemo.java` → `TradeRecordMatcherDemoTest.java`
- `YamlConfigurationDemo.java` → `YamlConfigurationDemoTest.java`

### **PHASE 3: LOOKUP DEMOS (Week 3)**
**Target: 18 classes → 18 test classes**

#### **3.1 Basic Lookup Demos (9 classes)**
- `AbstractLookupDemo.java` → `AbstractLookupDemoTest.java`
- `CompoundKeyLookupDemo.java` → `CompoundKeyLookupDemoTest.java`
- `ExternalDataSourceReferenceDemo.java` → `ExternalDataSourceReferenceDemoTest.java`
- `ExternalDataSourceWorkingDemo.java` → `ExternalDataSourceWorkingDemoTest.java`
- `FileSystemLookupDemo.java` → `FileSystemLookupDemoTest.java`
- `H2CustomParametersDemo.java` → `H2CustomParametersDemoTest.java`
- `NestedFieldLookupDemo.java` → `NestedFieldLookupDemoTest.java`
- `SharedDataSourceDemo.java` → `SharedDataSourceDemoTest.java`
- `SimpleFieldLookupDemo.java` → `SimpleFieldLookupDemoTest.java`

#### **3.2 Database Lookup Demos (9 classes)**
- `PostgreSQLExternalReferenceTest.java` → Move to test folder (already a test)
- `PostgreSQLLookupDemo.java` → `PostgreSQLLookupDemoTest.java`
- `SimplePostgreSQLLookupDemo.java` → `SimplePostgreSQLLookupDemoTest.java`
- `DatabaseConnectionTest.java` → Move to test folder (already a test)
- `DatabaseConnectivityTest.java` → Move to test folder (already a test)
- `DataSourceConfigurationTest.java` → Move to test folder (already a test)
- `ExternalDataSourceReferenceValidationTest.java` → Move to test folder (already a test)
- `ExternalReferenceDebugTest.java` → Move to test folder (already a test)
- `ParameterExtractionTest.java` → Move to test folder (already a test)

### **PHASE 4: INFRASTRUCTURE & UTILITIES (Week 4)**
**Target: 25 classes → Selective conversion**

#### **4.1 Infrastructure Classes (15 classes)**
- `DataServiceManagerDemo.java` → `DataServiceManagerDemoTest.java`
- `DatabaseSetup.java` → Keep as utility (used by tests)
- `DemoDataBootstrap.java` → `DemoDataBootstrapTest.java`
- `DemoDataLoader.java` → Keep as utility
- `DemoDataProvider.java` → Keep as utility
- `ExternalDatasetSetup.java` → Keep as utility
- `FileProcessingDemo.java` → `FileProcessingDemoTest.java`
- `FinancialStaticDataProvider.java` → Keep as utility
- `ProductionDemoDataServiceManager.java` → Keep as utility
- `RuleConfigDataSourceVerifier.java` → Keep as utility
- `RuleConfigDatabaseSetup.java` → Keep as utility
- `RuleConfigExternalDatasetSetup.java` → Keep as utility
- `StaticDataEntities.java` → Keep as utility
- `XmlDataGenerator.java` → Keep as utility
- `DataSourceVerifier.java` → Keep as utility
- ✅ `DataProviderComplianceTest.java` → Move to test folder (already a test)
- ✅ `DemoDataBootstrapTest.java` → Move to test folder (already a test)

#### **4.2 Utility Classes (3 classes)**
- `TestUtilities.java` → Keep as utility
- `YamlDependencyAnalysisDemo.java` → `YamlDependencyAnalysisDemoTest.java`
- `YamlValidationDemo.java` → `YamlValidationDemoTest.java`

#### **4.3 Compiler Classes (1 class)**
- `SimpleCompilerDemo.java` → `SimpleCompilerDemoTest.java`

#### **4.4 Runner Classes (6 classes)**
- Convert to test utilities or integration tests
- `AllDemosRunner.java` → `AllDemosRunnerTest.java` (integration test)
- Others → Keep as utilities for manual testing

#### **4.5 Model Classes (25 classes)**
- Keep as data model classes (no conversion needed)
- These are POJOs used by tests and demos

## 🔄 **CONVERSION METHODOLOGY**

### **For Each Demo Class:**

1. **Analyze Current Structure**
   - Identify main() method functionality
   - Extract core business logic
   - Identify YAML configuration files used
   - Note any hardcoded data or simulation

2. **Create Test Class**
   - Extend `DemoTestBase`
   - Convert main() logic to `@Test` methods
   - Use descriptive test method names
   - Follow AAA pattern (Arrange, Act, Assert)

3. **Apply APEX Guidelines**
   - Remove any YAML syntax validation
   - Focus on functional testing
   - Use real APEX services
   - Assert on business outcomes
   - Set up real test data

4. **Validate and Clean**
   - Remove unused imports
   - Add proper JavaDoc
   - Ensure tests are independent
   - Verify tests pass reliably

## 📁 **FILE ORGANIZATION**

### **Test Structure:**
```
apex-demo/src/test/java/dev/mars/apex/demo/
├── enrichment/          # Enrichment demo tests
├── evaluation/          # Evaluation demo tests  
├── lookup/              # Lookup demo tests
├── etl/                 # ETL demo tests
├── validation/          # Validation demo tests
├── infrastructure/      # Infrastructure demo tests
├── util/                # Utility demo tests
├── compiler/            # Compiler demo tests
├── DemoTestBase.java    # Base test class
└── ColoredTestOutputExtension.java
```

### **Source Structure (After Cleanup):**
```
apex-demo/src/main/java/dev/mars/apex/demo/
├── model/               # Keep - Data model classes
├── infrastructure/      # Keep utilities only
├── util/                # Keep utilities only
└── runners/             # Keep for manual testing
```

## ⚡ **EXECUTION STRATEGY**

### **Week 1: High-Priority Functional Demos**
- Start with enrichment demos (most critical)
- Convert ETL and validation demos
- Focus on classes with existing YAML configs

### **Week 2: Evaluation Demos**
- Convert core evaluation demos
- Handle configuration demos
- Address any hardcoded simulation issues

### **Week 3: Lookup Demos**
- Convert basic lookup demos
- Move existing test classes to proper location
- Handle database-specific demos

### **Week 4: Infrastructure & Cleanup**
- Convert remaining infrastructure demos
- Clean up utilities and runners
- Final validation and testing

## ✅ **SUCCESS CRITERIA**

1. **All functional demo classes converted to JUnit 5 tests**
2. **Tests follow APEX test writing guidelines**
3. **Tests extend DemoTestBase for consistency**
4. **Tests execute actual APEX functionality (no hardcoded simulation)**
5. **Tests are reliable and can be run repeatedly**
6. **Test execution time is reasonable (< 5 minutes for full suite)**
7. **All tests pass consistently**
8. **Source folder contains only utilities and data models**

## 🎯 **IMMEDIATE NEXT STEPS**

1. **Start with Phase 1** - High-priority functional demos
2. **Begin with enrichment demos** - most critical functionality
3. **Use existing successful tests as templates**
4. **Test each conversion thoroughly before proceeding**
5. **Update documentation as we progress**

---

**Generated by APEX Development Team**  
**Last Updated:** 2025-09-13

This systematic approach will ensure we convert all demo classes to proper JUnit 5 tests while maintaining reliability and following APEX best practices.
