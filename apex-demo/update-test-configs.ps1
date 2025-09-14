# PowerShell script to update test classes to use test-configs/[TestName]-test.yaml pattern

# Define the mapping of test classes to their current YAML files
$testMappings = @{
    "ApexAdvancedFeaturesDemoTest" = "evaluation/apex-advanced-features-demo-config.yaml"
    "ApexRulesEngineDemoTest" = "evaluation/apex-rules-engine-demo-config.yaml"
    "BatchProcessingDemoTest" = "transformation/batch-processing-demo-config.yaml"
    "CommoditySwapValidationBootstrapTest" = "validation/commodity-swap-validation-bootstrap-demo.yaml"
    "ComplianceServiceDemoTest" = "evaluation/compliance-service-demo.yaml"
    "CompoundKeyLookupDemoTest" = "lookup/compound-key-lookup-demo-config.yaml"
    "ComprehensiveFinancialSettlementDemoTest" = "enrichment/comprehensive-financial-settlement-demo-config.yaml"
    "CsvToH2PipelineTest" = "etl/csv-to-h2-pipeline-demo-config.yaml"
    "CustodyAutoRepairBootstrapTest" = "enrichment/custody-auto-repair-bootstrap-demo.yaml"
    "CustodyAutoRepairDemoTest" = "enrichment/custody-auto-repair-demo-config.yaml"
    "CustomerTransformerDemoTest" = "enrichment/customer-transformer-demo.yaml"
    "DatabaseSetupTest" = "infrastructure/database-setup-config.yaml"
    "DataManagementDemoTest" = "enrichment/data-management-demo-config.yaml"
    "DataServiceManagerDemoTest" = "infrastructure/data-service-manager-demo-config.yaml"
    "DataSourceVerifierTest" = "infrastructure/data-source-verifier-config.yaml"
    "DemoDataBootstrapTest" = "infrastructure/demo-data-bootstrap-config.yaml"
    "DemoDataLoaderTest" = "infrastructure/demo-data-loader-config.yaml"
    "DemoDataProviderTest" = "infrastructure/demo-data-provider-config.yaml"
    "ExternalDatasetSetupTest" = "infrastructure/external-dataset-setup-config.yaml"
    "ExternalDataSourceDemoTest" = "enrichment/external-data-source-demo-config.yaml"
    "ExternalDataSourceReferenceDemoTest" = "lookup/external-data-source-reference-demo-config.yaml"
    "ExternalDataSourceWorkingDemoTest" = "lookup/external-data-source-working-demo-config.yaml"
    "FileProcessingDemoTest" = "infrastructure/file-processing-demo-config.yaml"
    "FileSystemLookupDemoTest" = "lookup/file-system-lookup-demo-config.yaml"
    "FinancialDemoTest" = "evaluation/financial-demo-config.yaml"
    "FinancialStaticDataProviderTest" = "infrastructure/financial-static-data-provider-config.yaml"
    "FluentRuleBuilderExampleTest" = "evaluation/fluent-rule-builder-demo-config.yaml"
    "H2CustomParametersDemoTest" = "lookup/h2-custom-parameters-demo-config.yaml"
    "LayeredAPIDemoTest" = "evaluation/layered-api-demo-config.yaml"
    "NestedFieldLookupDemoTest" = "lookup/nested-field-lookup-demo-config.yaml"
    "OtcOptionsBootstrapDemoTest" = "enrichment/otc-options-bootstrap-demo.yaml"
    "PerformanceAndExceptionDemoTest" = "evaluation/performance-and-exception-demo-config.yaml"
    "PerformanceDemoTest" = "evaluation/performance-demo-config.yaml"
    "PostgreSQLLookupDemoTest" = "lookup/postgresql-lookup-demo-config.yaml"
    "PostTradeProcessingServiceDemoTest" = "evaluation/post-trade-processing-service-demo-config.yaml"
    "PricingServiceDemoTest" = "evaluation/pricing-service-demo-config.yaml"
    "ProductionDemoDataServiceManagerTest" = "infrastructure/production-demo-data-service-manager-config.yaml"
    "RiskManagementServiceTest" = "evaluation/risk-management-service-config.yaml"
    "RuleConfigDatabaseSetupTest" = "infrastructure/rule-config-database-setup-config.yaml"
    "RuleConfigDataSourceVerifierTest" = "infrastructure/rule-config-data-source-verifier-config.yaml"
    "RuleConfigExternalDatasetSetupTest" = "infrastructure/rule-config-external-dataset-setup-config.yaml"
    "RuleConfigurationBootstrapTest" = "evaluation/rule-configuration-bootstrap-config.yaml"
    "RuleConfigurationDemoTest" = "evaluation/rule-configuration-demo-config.yaml"
    "RuleConfigurationHardcodedBootstrapTest" = "evaluation/rule-configuration-hardcoded-bootstrap-config.yaml"
    "RuleConfigurationHardcodedDemoTest" = "evaluation/rule-configuration-hardcoded-demo-config.yaml"
    "RuleDefinitionServiceDemoTest" = "evaluation/rule-definition-service-demo-config.yaml"
    "ScenarioBasedProcessingDemoTest" = "evaluation/scenario-based-processing-demo-config.yaml"
    "SharedDataSourceDemoTest" = "lookup/shared-data-source-demo-config.yaml"
    "SimpleCompilerDemoTest" = "compiler/simple-compiler-demo-config.yaml"
    "SimpleFieldLookupDemoTest" = "lookup/simple-field-lookup-demo-config.yaml"
    "SimplePostgreSQLLookupDemoTest" = "lookup/simple-postgresql-lookup-demo-config.yaml"
    "SimplifiedAPIDemoTest" = "evaluation/simplified-api-demo-config.yaml"
    "TradeRecordMatcherDemoTest" = "evaluation/trade-record-matcher-demo-config.yaml"
    "TradeTransformerDemoTest" = "enrichment/trade-transformer-demo.yaml"
    "XmlDataGeneratorTest" = "infrastructure/xml-data-generator-config.yaml"
    "YamlConfigurationDemoTest" = "evaluation/yaml-configuration-demo-config.yaml"
    "YamlDatasetDemoTest" = "enrichment/yaml-dataset-demo-config.yaml"
    "YamlDependencyAnalysisDemoTest" = "util/yaml-dependency-analysis-demo-config.yaml"
    "YamlValidationDemoTest" = "util/yaml-validation-demo-config.yaml"
}

Write-Host "Starting test configuration update process..."

foreach ($testClass in $testMappings.Keys) {
    $sourceYaml = $testMappings[$testClass]
    $targetYaml = "test-configs/$($testClass.ToLower() -replace 'test$', '-test').yaml"
    
    Write-Host "Processing $testClass..."
    
    # Copy YAML file if source exists
    $sourcePath = "src\main\resources\$sourceYaml"
    $targetPath = "src\main\resources\$targetYaml"
    
    if (Test-Path $sourcePath) {
        Copy-Item $sourcePath $targetPath -Force
        Write-Host "  ✅ Copied $sourceYaml -> $targetYaml"
    } else {
        Write-Host "  ❌ Source file not found: $sourcePath"
    }
}

Write-Host "YAML file copying completed!"
