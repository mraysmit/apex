# PowerShell script to generate 200+ YAML files with proper APEX dependency patterns
$baseDir = "src/test/resources/apex-yaml-samples/graph-200"

# Function to create YAML file with proper APEX dependency patterns
function Create-ApexYamlFile {
    param(
        [string]$fileName,
        [string]$id,
        [string]$type,
        [string]$description,
        [string[]]$ruleConfigurations = @(),
        [string[]]$enrichmentRefs = @(),
        [string[]]$configFiles = @(),
        [string[]]$dependencies = @(),
        [hashtable]$customContent = @{}
    )
    
    $content = @"
metadata:
  id: $id
  type: $type
  version: "1.0"
  description: "$description"
  created-date: "2025-10-20"
  author: "APEX Test Generator"
"@

    # Add APEX-style dependencies first
    $hasDeps = $false
    if ($ruleConfigurations.Count -gt 0) {
        $content += "`n`n# File-based dependencies following APEX patterns"
        $content += "`nrule-configurations:"
        foreach ($dep in $ruleConfigurations) {
            $content += "`n  - `"$dep`""
        }
        $hasDeps = $true
    }
    
    if ($enrichmentRefs.Count -gt 0) {
        if (-not $hasDeps) {
            $content += "`n`n# File-based dependencies following APEX patterns"
        }
        $content += "`nenrichment-refs:"
        foreach ($dep in $enrichmentRefs) {
            $content += "`n  - `"$dep`""
        }
        $hasDeps = $true
    }
    
    if ($configFiles.Count -gt 0) {
        if (-not $hasDeps) {
            $content += "`n`n# File-based dependencies following APEX patterns"
        }
        $content += "`nconfig-files:"
        foreach ($dep in $configFiles) {
            $content += "`n  - `"$dep`""
        }
        $hasDeps = $true
    }
    
    if ($dependencies.Count -gt 0) {
        if (-not $hasDeps) {
            $content += "`n`n# File-based dependencies following APEX patterns"
        }
        $content += "`ndependencies:"
        foreach ($dep in $dependencies) {
            $content += "`n  - `"$dep`""
        }
    }

    # Add type-specific content
    switch ($type) {
        "scenario" {
            $content += @"

scenario:
  scenario-id: "$id"
  description: "$description"
  
  classification-rule:
    condition: "#dataType != null && #dataType == 'TRADE'"
    description: "Route trade data to processing"
  
  processing-stages:
    - stage-name: "validation"
      description: "Validate incoming data"
      config-file: "validation-rules.yaml"
      execution-order: 1
      failure-policy: "terminate"
    - stage-name: "enrichment"
      description: "Enrich with reference data"
      config-file: "enrichment-rules.yaml"
      execution-order: 2
      failure-policy: "continue"
  
  data-types:
    - "Trade"
    - "java.util.Map"
"@
        }
        "rule-config" {
            $content += @"

rule-groups:
  - id: $id-group-1
    name: "Primary Rule Group"
    description: "Primary rule group for $description"
    operator: AND
    stop-on-first-failure: true
    priority: 10
    rule-ids:
      - "$id-rule-1"
      - "$id-rule-2"

rules:
  - id: $id-rule-1
    name: "Primary Validation"
    description: "Primary validation rule for $description"
    condition: "trade.field1 != null && trade.field1.length() > 0"
    message: "Field1 validation failed"
    severity: "ERROR"
  - id: $id-rule-2
    name: "Secondary Validation"
    description: "Secondary validation rule for $description"
    condition: "trade.field2 != null && trade.field2 > 0"
    message: "Field2 validation failed"
    severity: "WARNING"
"@
        }
        "enrichment" {
            $content += @"

enrichments:
  - id: $id-enrichment-1
    name: "Primary Enrichment"
    type: lookup-enrichment
    description: "Primary enrichment for $description"
    condition: "#symbol != null"
    lookup-config:
      lookup-key: "#symbol"
      lookup-dataset:
        type: external
        data-source-ref: primary-data-source
    field-mappings:
      - source-field: enrichedValue1
        target-field: enriched.field1
  - id: $id-enrichment-2
    name: "Secondary Enrichment"
    type: calculation-enrichment
    description: "Secondary enrichment for $description"
    condition: "#value != null"
    calculation-config:
      expression: "#value * 1.1"
      result-field: calculatedValue
    field-mappings:
      - source-field: calculatedValue
        target-field: enriched.field2
"@
        }
        "external-data-config" {
            $content += @"

data-sources:
  - name: $id-datasource
    type: "postgresql"
    connection-string: "jdbc:postgresql://localhost:5432/apex_db"
    username: "apex_user"
    password: "apex_password"
    pool-size: 10
    timeout-ms: 5000
  - name: $id-cache
    type: "redis"
    connection-string: "redis://localhost:6379"
    ttl: 3600
    max-connections: 20
"@
        }
        "pipeline-config" {
            $content += @"

pipeline:
  name: "$description Pipeline"
  description: "Processing pipeline for $description"
  stages:
    - name: "validation"
      type: "rule-execution"
      config: "validation-rules.yaml"
      execution-order: 1
      failure-policy: "terminate"
    - name: "enrichment"
      type: "data-enrichment"
      config: "enrichment-rules.yaml"
      execution-order: 2
      failure-policy: "continue"
    - name: "output"
      type: "data-output"
      config: "output-config.yaml"
      execution-order: 3
      failure-policy: "continue"
"@
        }
    }

    # Add custom content
    foreach ($key in $customContent.Keys) {
        $content += "`n`n${key}:"
        $value = $customContent[$key]
        if ($value -is [array]) {
            foreach ($item in $value) {
                $content += "`n  - `"$item`""
            }
        } else {
            $content += " $value"
        }
    }

    $filePath = Join-Path $baseDir $fileName
    $content | Out-File -FilePath $filePath -Encoding UTF8
    Write-Host "Created: $fileName"
}

Write-Host "Generating APEX-compliant YAML files with proper dependency chains..."

# Update core scenario files with proper APEX dependencies
Write-Host "`nUpdating core scenario files..."

# 011-fixed-income.yaml
Create-ApexYamlFile -fileName "011-fixed-income.yaml" -id "fixed-income-scenario" -type "scenario" -description "Fixed income trading scenario" `
    -ruleConfigurations @("054-fixed-income-validation.yaml", "055-fixed-income-risk.yaml") `
    -enrichmentRefs @("153-yield-curve-enrichment.yaml", "154-credit-rating-enrichment.yaml") `
    -configFiles @("252-bond-data-config.yaml", "253-rating-data-config.yaml")

# 012-derivatives.yaml  
Create-ApexYamlFile -fileName "012-derivatives.yaml" -id "derivatives-scenario" -type "scenario" -description "Derivatives trading scenario" `
    -ruleConfigurations @("056-derivatives-validation.yaml", "057-derivatives-risk.yaml") `
    -enrichmentRefs @("155-greeks-enrichment.yaml", "156-volatility-enrichment.yaml") `
    -configFiles @("254-derivatives-data-config.yaml", "255-volatility-data-config.yaml")

Write-Host "`nGenerating rule configuration files with dependencies..."

# 054-fixed-income-validation.yaml
Create-ApexYamlFile -fileName "054-fixed-income-validation.yaml" -id "fixed-income-validation" -type "rule-config" -description "Fixed income validation rules" `
    -ruleConfigurations @("100-common-validation-rules.yaml", "103-bond-validation-rules.yaml")

# 055-fixed-income-risk.yaml
Create-ApexYamlFile -fileName "055-fixed-income-risk.yaml" -id "fixed-income-risk" -type "rule-config" -description "Fixed income risk assessment rules" `
    -ruleConfigurations @("104-duration-risk-rules.yaml", "105-credit-risk-rules.yaml")

# 056-derivatives-validation.yaml
Create-ApexYamlFile -fileName "056-derivatives-validation.yaml" -id "derivatives-validation" -type "rule-config" -description "Derivatives validation rules" `
    -ruleConfigurations @("100-common-validation-rules.yaml", "106-derivatives-rules.yaml")

# 057-derivatives-risk.yaml
Create-ApexYamlFile -fileName "057-derivatives-risk.yaml" -id "derivatives-risk" -type "rule-config" -description "Derivatives risk assessment rules" `
    -ruleConfigurations @("107-greeks-risk-rules.yaml", "108-volatility-risk-rules.yaml")

Write-Host "`nGenerating base rule files (leaf nodes)..."

# Base validation rules (100-108)
$baseRules = @(
    @{file="103-bond-validation-rules.yaml"; id="bond-validation"; desc="Bond-specific validation rules"}
    @{file="104-duration-risk-rules.yaml"; id="duration-risk"; desc="Duration risk assessment rules"}
    @{file="105-credit-risk-rules.yaml"; id="credit-risk"; desc="Credit risk assessment rules"}
    @{file="106-derivatives-rules.yaml"; id="derivatives-base"; desc="Base derivatives validation rules"}
    @{file="107-greeks-risk-rules.yaml"; id="greeks-risk"; desc="Greeks risk assessment rules"}
    @{file="108-volatility-risk-rules.yaml"; id="volatility-risk"; desc="Volatility risk assessment rules"}
)

foreach ($rule in $baseRules) {
    Create-ApexYamlFile -fileName $rule.file -id $rule.id -type "rule-config" -description $rule.desc
}

Write-Host "`nGenerating enrichment files with dependencies..."

# Enrichment files with dependencies
Create-ApexYamlFile -fileName "153-yield-curve-enrichment.yaml" -id "yield-curve-enrichment" -type "enrichment" -description "Yield curve enrichment" `
    -dependencies @("252-bond-data-config.yaml")

Create-ApexYamlFile -fileName "154-credit-rating-enrichment.yaml" -id "credit-rating-enrichment" -type "enrichment" -description "Credit rating enrichment" `
    -dependencies @("253-rating-data-config.yaml")

Create-ApexYamlFile -fileName "155-greeks-enrichment.yaml" -id "greeks-enrichment" -type "enrichment" -description "Greeks calculation enrichment" `
    -dependencies @("254-derivatives-data-config.yaml")

Create-ApexYamlFile -fileName "156-volatility-enrichment.yaml" -id "volatility-enrichment" -type "enrichment" -description "Volatility enrichment" `
    -dependencies @("255-volatility-data-config.yaml")

Write-Host "`nGenerating external data config files (leaf nodes)..."

# External data config files (leaf nodes)
$dataConfigs = @(
    @{file="252-bond-data-config.yaml"; id="bond-data-config"; desc="Bond reference data configuration"}
    @{file="253-rating-data-config.yaml"; id="rating-data-config"; desc="Credit rating data configuration"}
    @{file="254-derivatives-data-config.yaml"; id="derivatives-data-config"; desc="Derivatives data configuration"}
    @{file="255-volatility-data-config.yaml"; id="volatility-data-config"; desc="Volatility data configuration"}
)

foreach ($config in $dataConfigs) {
    Create-ApexYamlFile -fileName $config.file -id $config.id -type "external-data-config" -description $config.desc
}

Write-Host "`nCompleted generating APEX-compliant dependency chain files!"
Write-Host "Files created with proper dependency patterns:"
Write-Host "- Scenarios reference rule-configs, enrichments, and external-data-configs"
Write-Host "- Rule-configs reference other rule-configs"
Write-Host "- Enrichments reference external-data-configs via dependencies"
Write-Host "- External-data-configs are leaf nodes"
