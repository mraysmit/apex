# PowerShell script to update test classes to use test-configs/[TestName]-test.yaml pattern

# Define the mapping of test classes to their new YAML files
$testUpdates = @{
    "src\test\java\dev\mars\apex\demo\evaluation\ApexAdvancedFeaturesDemoTest.java" = @{
        "old" = "evaluation/apex-advanced-features-demo-config.yaml"
        "new" = "test-configs/apexadvancedfeaturesdemo-test.yaml"
    }
    "src\test\java\dev\mars\apex\demo\evaluation\ApexRulesEngineDemoTest.java" = @{
        "old" = "evaluation/apex-rules-engine-demo-config.yaml"
        "new" = "test-configs/apexrulesenginedemo-test.yaml"
    }
    "src\test\java\dev\mars\apex\demo\enrichment\BatchProcessingDemoTest.java" = @{
        "old" = "transformation/batch-processing-demo-config.yaml"
        "new" = "test-configs/batchprocessingdemo-test.yaml"
    }
    "src\test\java\dev\mars\apex\demo\validation\CommoditySwapValidationBootstrapTest.java" = @{
        "old" = "validation/commodity-swap-validation-bootstrap-demo.yaml"
        "new" = "test-configs/commodityswapvalidationbootstrap-test.yaml"
    }
    "src\test\java\dev\mars\apex\demo\evaluation\ComplianceServiceDemoTest.java" = @{
        "old" = "evaluation/compliance-service-demo.yaml"
        "new" = "test-configs/complianceservicedemo-test.yaml"
    }
    "src\test\java\dev\mars\apex\demo\lookup\CompoundKeyLookupDemoTest.java" = @{
        "old" = "lookup/compound-key-lookup-demo-config.yaml"
        "new" = "test-configs/compoundkeylookupdemo-test.yaml"
    }
    "src\test\java\dev\mars\apex\demo\enrichment\ComprehensiveFinancialSettlementDemoTest.java" = @{
        "old" = "enrichment/comprehensive-financial-settlement-demo-config.yaml"
        "new" = "test-configs/comprehensivefinancialsettlementdemo-test.yaml"
    }
    "src\test\java\dev\mars\apex\demo\etl\CsvToH2PipelineTest.java" = @{
        "old" = "etl/csv-to-h2-pipeline-demo-config.yaml"
        "new" = "test-configs/csvtoh2pipeline-test.yaml"
    }
    "src\test\java\dev\mars\apex\demo\enrichment\CustodyAutoRepairBootstrapTest.java" = @{
        "old" = "enrichment/custody-auto-repair-bootstrap-demo.yaml"
        "new" = "test-configs/custodyautorepairbootstrap-test.yaml"
    }
    "src\test\java\dev\mars\apex\demo\enrichment\CustodyAutoRepairDemoTest.java" = @{
        "old" = "enrichment/custody-auto-repair-demo-config.yaml"
        "new" = "test-configs/custodyautorepairdemo-test.yaml"
    }
    "src\test\java\dev\mars\apex\demo\enrichment\CustomerTransformerDemoTest.java" = @{
        "old" = "enrichment/customer-transformer-demo.yaml"
        "new" = "test-configs/customertransformerdemo-test.yaml"
    }
    "src\test\java\dev\mars\apex\demo\infrastructure\DatabaseSetupTest.java" = @{
        "old" = "infrastructure/database-setup-config.yaml"
        "new" = "test-configs/databasesetup-test.yaml"
    }
    "src\test\java\dev\mars\apex\demo\enrichment\DataManagementDemoTest.java" = @{
        "old" = "enrichment/data-management-demo-config.yaml"
        "new" = "test-configs/datamanagementdemo-test.yaml"
    }
    "src\test\java\dev\mars\apex\demo\infrastructure\DataServiceManagerDemoTest.java" = @{
        "old" = "infrastructure/data-service-manager-demo-config.yaml"
        "new" = "test-configs/dataservicemanagerdemo-test.yaml"
    }
    "src\test\java\dev\mars\apex\demo\infrastructure\DataSourceVerifierTest.java" = @{
        "old" = "infrastructure/data-source-verifier-config.yaml"
        "new" = "test-configs/datasourceverifier-test.yaml"
    }
    "src\test\java\dev\mars\apex\demo\infrastructure\DemoDataBootstrapTest.java" = @{
        "old" = "infrastructure/demo-data-bootstrap-config.yaml"
        "new" = "test-configs/demodatabootstrap-test.yaml"
    }
    "src\test\java\dev\mars\apex\demo\infrastructure\DemoDataLoaderTest.java" = @{
        "old" = "infrastructure/demo-data-loader-config.yaml"
        "new" = "test-configs/demodataloader-test.yaml"
    }
    "src\test\java\dev\mars\apex\demo\infrastructure\DemoDataProviderTest.java" = @{
        "old" = "infrastructure/demo-data-provider-config.yaml"
        "new" = "test-configs/demodataprovider-test.yaml"
    }
    "src\test\java\dev\mars\apex\demo\infrastructure\ExternalDatasetSetupTest.java" = @{
        "old" = "infrastructure/external-dataset-setup-config.yaml"
        "new" = "test-configs/externaldatasetsetup-test.yaml"
    }
    "src\test\java\dev\mars\apex\demo\enrichment\ExternalDataSourceDemoTest.java" = @{
        "old" = "enrichment/external-data-source-demo-config.yaml"
        "new" = "test-configs/externaldatasourcedemo-test.yaml"
    }
    "src\test\java\dev\mars\apex\demo\lookup\ExternalDataSourceReferenceDemoTest.java" = @{
        "old" = "lookup/external-data-source-reference-demo-config.yaml"
        "new" = "test-configs/externaldatasourcereferencedemo-test.yaml"
    }
    "src\test\java\dev\mars\apex\demo\lookup\ExternalDataSourceWorkingDemoTest.java" = @{
        "old" = "lookup/external-data-source-working-demo-config.yaml"
        "new" = "test-configs/externaldatasourceworkingdemo-test.yaml"
    }
    "src\test\java\dev\mars\apex\demo\infrastructure\FileProcessingDemoTest.java" = @{
        "old" = "infrastructure/file-processing-demo-config.yaml"
        "new" = "test-configs/fileprocessingdemo-test.yaml"
    }
    "src\test\java\dev\mars\apex\demo\lookup\FileSystemLookupDemoTest.java" = @{
        "old" = "lookup/file-system-lookup-demo-config.yaml"
        "new" = "test-configs/filesystemlookupdemo-test.yaml"
    }
    "src\test\java\dev\mars\apex\demo\evaluation\FinancialDemoTest.java" = @{
        "old" = "evaluation/financial-demo-config.yaml"
        "new" = "test-configs/financialdemo-test.yaml"
    }
    "src\test\java\dev\mars\apex\demo\infrastructure\FinancialStaticDataProviderTest.java" = @{
        "old" = "infrastructure/financial-static-data-provider-config.yaml"
        "new" = "test-configs/financialstaticdataprovider-test.yaml"
    }
    "src\test\java\dev\mars\apex\demo\evaluation\FluentRuleBuilderExampleTest.java" = @{
        "old" = "evaluation/fluent-rule-builder-demo-config.yaml"
        "new" = "test-configs/fluentrulebuilderexample-test.yaml"
    }
    "src\test\java\dev\mars\apex\demo\lookup\H2CustomParametersDemoTest.java" = @{
        "old" = "lookup/h2-custom-parameters-demo-config.yaml"
        "new" = "test-configs/h2customparametersdemo-test.yaml"
    }
    "src\test\java\dev\mars\apex\demo\validation\IntegratedValidatorDemoTest.java" = @{
        "old" = "validation/integrated-validator-demo.yaml"
        "new" = "test-configs/integrated-validator-demo-test.yaml"
    }
    "src\test\java\dev\mars\apex\demo\evaluation\LayeredAPIDemoTest.java" = @{
        "old" = "evaluation/layered-api-demo-config.yaml"
        "new" = "test-configs/layeredapidemo-test.yaml"
    }
    "src\test\java\dev\mars\apex\demo\lookup\NestedFieldLookupDemoTest.java" = @{
        "old" = "lookup/nested-field-lookup-demo-config.yaml"
        "new" = "test-configs/nestedfieldlookupdemo-test.yaml"
    }
    "src\test\java\dev\mars\apex\demo\enrichment\OtcOptionsBootstrapDemoTest.java" = @{
        "old" = "enrichment/otc-options-bootstrap-demo.yaml"
        "new" = "test-configs/otcoptionsbootstrapdemo-test.yaml"
    }
    "src\test\java\dev\mars\apex\demo\evaluation\PerformanceAndExceptionDemoTest.java" = @{
        "old" = "evaluation/performance-and-exception-demo-config.yaml"
        "new" = "test-configs/performanceandexceptiondemo-test.yaml"
    }
    "src\test\java\dev\mars\apex\demo\evaluation\PerformanceDemoTest.java" = @{
        "old" = "evaluation/performance-demo-config.yaml"
        "new" = "test-configs/performancedemo-test.yaml"
    }
    "src\test\java\dev\mars\apex\demo\lookup\PostgreSQLLookupDemoTest.java" = @{
        "old" = "lookup/postgresql-lookup-demo-config.yaml"
        "new" = "test-configs/postgresqllookupdemo-test.yaml"
    }
    "src\test\java\dev\mars\apex\demo\evaluation\PostTradeProcessingServiceDemoTest.java" = @{
        "old" = "evaluation/post-trade-processing-service-demo-config.yaml"
        "new" = "test-configs/posttradeprocessingservicedemo-test.yaml"
    }
    "src\test\java\dev\mars\apex\demo\evaluation\PricingServiceDemoTest.java" = @{
        "old" = "evaluation/pricing-service-demo-config.yaml"
        "new" = "test-configs/pricingservicedemo-test.yaml"
    }
    "src\test\java\dev\mars\apex\demo\compiler\SimpleCompilerDemoTest.java" = @{
        "old" = "compiler/simple-compiler-demo-config.yaml"
        "new" = "test-configs/simplecompilerdemo-test.yaml"
    }
    "src\test\java\dev\mars\apex\demo\util\YamlValidationDemoTest.java" = @{
        "old" = "util/yaml-validation-demo-config.yaml"
        "new" = "test-configs/yamlvalidationdemo-test.yaml"
    }
}

Write-Host "Starting test class update process..."

foreach ($testFile in $testUpdates.Keys) {
    $oldPath = $testUpdates[$testFile]["old"]
    $newPath = $testUpdates[$testFile]["new"]
    
    Write-Host "Processing $testFile..."
    
    if (Test-Path $testFile) {
        # Read the file content
        $content = Get-Content $testFile -Raw
        
        # Replace the old path with the new path
        $updatedContent = $content -replace [regex]::Escape($oldPath), $newPath
        
        # Write the updated content back
        Set-Content $testFile $updatedContent -NoNewline
        
        Write-Host "  ✅ Updated $oldPath -> $newPath"
    } else {
        Write-Host "  ❌ Test file not found: $testFile"
    }
}

Write-Host "Test class update completed!"
