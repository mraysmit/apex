# Test Class to YAML Configuration Mapping

This document shows the direct mapping between test classes and their YAML configuration files in the new `test-configs/` structure.

## ✅ SOLUTION: Direct Test-to-YAML Association

Each test class now has a corresponding YAML file with a clear naming pattern:
- **Pattern**: `[TestClassName]` → `test-configs/[testclassname]-test.yaml`
- **Location**: All test configurations are in `src/main/resources/test-configs/`
- **Benefit**: Zero ambiguity about which YAML belongs to which test

## 📋 Complete Mapping

| Test Class | YAML Configuration File |
|------------|------------------------|
| **COMPILER** |
| `SimpleCompilerDemoTest.java` | `test-configs/simplecompilerdemo-test.yaml` |
| **ENRICHMENT** |
| `BatchProcessingDemoTest.java` | `test-configs/batchprocessingdemo-test.yaml` |
| `ComprehensiveFinancialSettlementDemoTest.java` | `test-configs/comprehensivefinancialsettlementdemo-test.yaml` |
| `CustodyAutoRepairBootstrapTest.java` | `test-configs/custodyautorepairbootstrap-test.yaml` |
| `CustodyAutoRepairDemoTest.java` | `test-configs/custodyautorepairdemo-test.yaml` |
| `CustomerTransformerDemoTest.java` | `test-configs/customertransformerdemo-test.yaml` |
| `DataManagementDemoTest.java` | `test-configs/datamanagementdemo-test.yaml` |
| `ExternalDataSourceDemoTest.java` | `test-configs/externaldatasourcedemo-test.yaml` |
| `OtcOptionsBootstrapDemoTest.java` | `test-configs/otcoptionsbootstrapdemo-test.yaml` |
| `YamlDatasetDemoTest.java` | `test-configs/yamldatasetdemo-test.yaml` |
| **EVALUATION** |
| `ApexAdvancedFeaturesDemoTest.java` | `test-configs/apexadvancedfeaturesdemo-test.yaml` |
| `ApexRulesEngineDemoTest.java` | `test-configs/apexrulesenginedemo-test.yaml` |
| `ComplianceServiceDemoTest.java` | `test-configs/complianceservicedemo-test.yaml` |
| `DynamicMethodExecutionDemoTest.java` | `test-configs/dynamic-method-execution-demo-test.yaml` |
| `FinancialDemoTest.java` | `test-configs/financialdemo-test.yaml` |
| `FluentRuleBuilderExampleTest.java` | `test-configs/fluentrulebuilderexample-test.yaml` |
| `LayeredAPIDemoTest.java` | `test-configs/layeredapidemo-test.yaml` |
| `PerformanceAndExceptionDemoTest.java` | `test-configs/performanceandexceptiondemo-test.yaml` |
| `PerformanceDemoTest.java` | `test-configs/performancedemo-test.yaml` |
| `PostTradeProcessingServiceDemoTest.java` | `test-configs/posttradeprocessingservicedemo-test.yaml` |
| `PricingServiceDemoTest.java` | `test-configs/pricingservicedemo-test.yaml` |
| `RiskManagementServiceTest.java` | `test-configs/riskmanagementservice-test.yaml` |
| `RuleConfigurationBootstrapTest.java` | `test-configs/ruleconfigurationbootstrap-test.yaml` |
| `RuleConfigurationDemoTest.java` | `test-configs/ruleconfigurationdemo-test.yaml` |
| `RuleConfigurationHardcodedBootstrapTest.java` | `test-configs/ruleconfigurationhardcodedbootstrap-test.yaml` |
| `RuleConfigurationHardcodedDemoTest.java` | `test-configs/ruleconfigurationhardcodeddemo-test.yaml` |
| `RuleDefinitionServiceDemoTest.java` | `test-configs/ruledefinitionservicedemo-test.yaml` |
| `ScenarioBasedProcessingDemoTest.java` | `test-configs/scenariobasedprocessingdemo-test.yaml` |
| `SimplifiedAPIDemoTest.java` | `test-configs/simplifiedapidemo-test.yaml` |
| `TradeRecordMatcherDemoTest.java` | `test-configs/traderecordmatcherdemo-test.yaml` |
| `YamlConfigurationDemoTest.java` | `test-configs/yamlconfigurationdemo-test.yaml` |
| **ETL** |
| `CsvToH2PipelineTest.java` | `test-configs/csvtoh2pipeline-test.yaml` |
| **INFRASTRUCTURE** |
| `DatabaseSetupTest.java` | `test-configs/databasesetup-test.yaml` |
| `DataServiceManagerDemoTest.java` | `test-configs/dataservicemanagerdemo-test.yaml` |
| `DataSourceVerifierTest.java` | `test-configs/datasourceverifier-test.yaml` |
| `DemoDataBootstrapTest.java` | `test-configs/demodatabootstrap-test.yaml` |
| `DemoDataLoaderTest.java` | `test-configs/demodataloader-test.yaml` |
| `DemoDataProviderTest.java` | `test-configs/demodataprovider-test.yaml` |
| `ExternalDatasetSetupTest.java` | `test-configs/externaldatasetsetup-test.yaml` |
| `FileProcessingDemoTest.java` | `test-configs/fileprocessingdemo-test.yaml` |
| `FinancialStaticDataProviderTest.java` | `test-configs/financialstaticdataprovider-test.yaml` |
| `ProductionDemoDataServiceManagerTest.java` | `test-configs/productiondemodataservicemanager-test.yaml` |
| `RuleConfigDatabaseSetupTest.java` | `test-configs/ruleconfigdatabasesetup-test.yaml` |
| `RuleConfigDataSourceVerifierTest.java` | `test-configs/ruleconfigdatasourceverifier-test.yaml` |
| `RuleConfigExternalDatasetSetupTest.java` | `test-configs/ruleconfigexternaldatasetsetup-test.yaml` |
| `XmlDataGeneratorTest.java` | `test-configs/xmldatagenerator-test.yaml` |
| **LOOKUP** |
| `AdvancedCachingDemoTest.java` | `test-configs/advancedcachingdemo-test.yaml` |
| `CompoundKeyLookupDemoTest.java` | `test-configs/compoundkeylookupdemo-test.yaml` |
| `ExternalDataSourceReferenceDemoTest.java` | `test-configs/externaldatasourcereferencedemo-test.yaml` |
| `ExternalDataSourceWorkingDemoTest.java` | `test-configs/externaldatasourceworkingdemo-test.yaml` |
| `FileSystemLookupDemoTest.java` | `test-configs/filesystemlookupdemo-test.yaml` |
| `H2CustomParametersDemoTest.java` | `test-configs/h2customparametersdemo-test.yaml` |
| `JsonFileLookupTest.java` | `test-configs/jsonfilelookup-test.yaml` |
| `MultiParameterLookupTest.java` | `test-configs/multiparameterlookup-test.yaml` |
| `NestedFieldLookupDemoTest.java` | `test-configs/nestedfieldlookupdemo-test.yaml` |
| `PostgreSQLLookupDemoTest.java` | `test-configs/postgresqllookupdemo-test.yaml` |
| `SharedDataSourceDemoTest.java` | `test-configs/shareddatasourcedemo-test.yaml` |
| `SimpleFieldLookupDemoTest.java` | `test-configs/simplefieldlookupdemo-test.yaml` |
| `SimplePostgreSQLLookupDemoTest.java` | `test-configs/simplepostgresqllookupdemo-test.yaml` |
| `XmlFileLookupTest.java` | `test-configs/xmlfilelookup-test.yaml` |
| **UTIL** |
| `YamlDependencyAnalysisDemoTest.java` | `test-configs/yamldependencyanalysisdemo-test.yaml` |
| `YamlValidationDemoTest.java` | `test-configs/yamlvalidationdemo-test.yaml` |
| **VALIDATION** |
| `BasicUsageExamplesTest.java` | `test-configs/basic-usage-examples-test.yaml` |
| `CommoditySwapValidationBootstrapTest.java` | `test-configs/commodityswapvalidationbootstrap-test.yaml` |
| `IntegratedTradeValidatorComplexDemoTest.java` | `test-configs/integrated-trade-validator-complex-demo-test.yaml` |
| `IntegratedValidatorDemoTest.java` | `test-configs/integrated-validator-demo-test.yaml` |

## 🎯 Usage Examples

### Finding YAML for a Test Class
```java
// For IntegratedTradeValidatorComplexDemoTest.java
var config = loadAndValidateYaml("test-configs/integrated-trade-validator-complex-demo-test.yaml");

// For DynamicMethodExecutionDemoTest.java  
var config = loadAndValidateYaml("test-configs/dynamic-method-execution-demo-test.yaml");

// For SimpleCompilerDemoTest.java
var config = loadAndValidateYaml("test-configs/simplecompilerdemo-test.yaml");
```

### Finding Test Class for a YAML
```
test-configs/batchprocessingdemo-test.yaml → BatchProcessingDemoTest.java
test-configs/apexadvancedfeaturesdemo-test.yaml → ApexAdvancedFeaturesDemoTest.java
test-configs/basic-usage-examples-test.yaml → BasicUsageExamplesTest.java
```

## ✅ Benefits Achieved

1. **🎯 Perfect Association**: Every test class has exactly one corresponding YAML file
2. **🔍 Easy Discovery**: Just look for `[testclassname]-test.yaml` in `test-configs/`
3. **📁 Clean Organization**: All test configs in one dedicated folder
4. **🚀 No Confusion**: Zero ambiguity about which YAML belongs to which test
5. **🔧 Maintainable**: Easy to add new tests following the same pattern

## 📝 Notes

- **Legacy Structure**: The original category-based folders (`enrichment/`, `evaluation/`, etc.) are preserved for reference
- **Active Usage**: All tests now use the new `test-configs/` structure
- **Naming Convention**: Test YAML files use lowercase with hyphens for readability
- **Validation**: All mappings have been tested and verified to work correctly
