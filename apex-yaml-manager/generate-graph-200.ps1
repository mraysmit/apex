# PowerShell script to generate 200 YAML files for dependency testing
$baseDir = "src/test/resources/apex-yaml-samples/graph-200"

# Function to create YAML file
function Create-YamlFile {
    param(
        [string]$fileName,
        [string]$id,
        [string]$type,
        [string]$description,
        [string[]]$dependencies = @(),
        [int]$ruleCount = 2,
        [int]$enrichmentCount = 0
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

    if ($type -eq "rule-config") {
        $content += @"
rule-groups:
  - id: ${id}-group
    description: "$description group"
    rule-ids:
"@
        for ($i = 1; $i -le $ruleCount; $i++) {
            $content += "`n      - `"${id}-rule-${i}`""
        }
        
        $content += "`n`nrules:"
        for ($i = 1; $i -le $ruleCount; $i++) {
            $content += @"

  - id: ${id}-rule-${i}
    description: "Rule ${i} for ${description}"
    condition: "trade.field${i} != null && trade.field${i} > 0"
    severity: "ERROR"
"@
        }
    }
    
    if ($type -eq "enrichment") {
        $content += @"
enrichments:
"@
        for ($i = 1; $i -le $enrichmentCount; $i++) {
            $content += @"

  - id: ${id}-enrichment-${i}
    description: "Enrichment ${i} for ${description}"
    source-type: "database"
    target-field: "enriched.field${i}"
    condition: "trade.field${i} != null"
"@
        }
    }
    
    if ($type -eq "external-data-config") {
        $content += @"
data-sources:
  - id: ${id}-source
    type: "database"
    connection-string: "jdbc:postgresql://localhost:5432/apex_test"
    query: "SELECT * FROM ${id}_data WHERE id = ?"
    cache-ttl: 300
"@
    }
    
    if ($type -eq "pipeline-config") {
        $content += @"
pipeline:
  id: $id
  description: "$description"
  stages:
    - name: "validation"
      type: "rule-execution"
    - name: "enrichment"
      type: "data-enrichment"
    - name: "output"
      type: "data-output"
"@
    }
    
    if ($dependencies.Count -gt 0) {
        $content += "`n`ndependencies:"
        foreach ($dep in $dependencies) {
            $content += "`n  - `"$dep`""
        }
    }
    
    $filePath = Join-Path $baseDir $fileName
    $content | Out-File -FilePath $filePath -Encoding UTF8
    Write-Host "Created: $fileName"
}

# Create remaining scenarios (013-034)
$scenarios = @(
    @{file="013-fx-trading.yaml"; id="fx-trading-scenario"; desc="Foreign exchange trading scenario"; deps=@("062-fx-validation.yaml", "063-fx-settlement.yaml", "159-fx-rate-enrichment.yaml")}
    @{file="014-commodities.yaml"; id="commodities-scenario"; desc="Commodities trading scenario"; deps=@("064-commodity-validation.yaml", "065-commodity-settlement.yaml", "160-commodity-enrichment.yaml")}
    @{file="015-crypto-trading.yaml"; id="crypto-scenario"; desc="Cryptocurrency trading scenario"; deps=@("066-crypto-validation.yaml", "067-crypto-settlement.yaml", "161-crypto-enrichment.yaml")}
    @{file="016-repo-trading.yaml"; id="repo-scenario"; desc="Repurchase agreement trading"; deps=@("068-repo-validation.yaml", "069-repo-settlement.yaml", "162-repo-enrichment.yaml")}
    @{file="017-securities-lending.yaml"; id="sec-lending-scenario"; desc="Securities lending scenario"; deps=@("070-lending-validation.yaml", "071-lending-settlement.yaml", "163-lending-enrichment.yaml")}
    @{file="018-prime-brokerage.yaml"; id="prime-brokerage-scenario"; desc="Prime brokerage services"; deps=@("072-prime-validation.yaml", "073-prime-settlement.yaml", "164-prime-enrichment.yaml")}
    @{file="019-clearing-settlement.yaml"; id="clearing-scenario"; desc="Central clearing and settlement"; deps=@("074-clearing-validation.yaml", "075-clearing-settlement.yaml", "165-clearing-enrichment.yaml")}
    @{file="020-americas-trading.yaml"; id="americas-scenario"; desc="Americas region trading"; deps=@("076-americas-validation.yaml", "077-americas-settlement.yaml", "166-americas-enrichment.yaml")}
    @{file="021-emea-trading.yaml"; id="emea-scenario"; desc="EMEA region trading"; deps=@("078-emea-validation.yaml", "079-emea-settlement.yaml", "167-emea-enrichment.yaml")}
    @{file="022-apac-trading.yaml"; id="apac-scenario"; desc="APAC region trading"; deps=@("080-apac-validation.yaml", "081-apac-settlement.yaml", "168-apac-enrichment.yaml")}
    @{file="023-latam-trading.yaml"; id="latam-scenario"; desc="Latin America trading"; deps=@("082-latam-validation.yaml", "083-latam-settlement.yaml", "169-latam-enrichment.yaml")}
    @{file="024-mena-trading.yaml"; id="mena-scenario"; desc="MENA region trading"; deps=@("084-mena-validation.yaml", "085-mena-settlement.yaml", "170-mena-enrichment.yaml")}
    @{file="030-mifid-compliance.yaml"; id="mifid-scenario"; desc="MiFID II compliance"; deps=@("086-mifid-validation.yaml", "087-mifid-reporting.yaml", "171-mifid-enrichment.yaml")}
    @{file="031-dodd-frank.yaml"; id="dodd-frank-scenario"; desc="Dodd-Frank compliance"; deps=@("088-dodd-frank-validation.yaml", "089-dodd-frank-reporting.yaml", "172-dodd-frank-enrichment.yaml")}
    @{file="032-basel-compliance.yaml"; id="basel-scenario"; desc="Basel III compliance"; deps=@("090-basel-validation.yaml", "091-basel-reporting.yaml", "173-basel-enrichment.yaml")}
    @{file="033-emir-compliance.yaml"; id="emir-scenario"; desc="EMIR compliance"; deps=@("092-emir-validation.yaml", "093-emir-reporting.yaml", "174-emir-enrichment.yaml")}
    @{file="034-cftc-compliance.yaml"; id="cftc-scenario"; desc="CFTC compliance"; deps=@("094-cftc-validation.yaml", "095-cftc-reporting.yaml", "175-cftc-enrichment.yaml")}
)

foreach ($scenario in $scenarios) {
    Create-YamlFile -fileName $scenario.file -id $scenario.id -type "scenario" -description $scenario.desc -dependencies $scenario.deps
}

Write-Host "Created scenarios 013-034"

# Create rule-config files (051-099)
$ruleConfigs = @()
for ($i = 51; $i -le 99; $i++) {
    $name = switch ($i) {
        51 { @{name="equity-enrichment"; desc="Equity enrichment rules"; deps=@("100-common-validation-rules.yaml")} }
        52 { @{name="equity-risk"; desc="Equity risk calculation rules"; deps=@("100-common-validation-rules.yaml", "110-reference-data-rules.yaml")} }
        53 { @{name="equity-settlement"; desc="Equity settlement rules"; deps=@("111-timestamp-rules.yaml", "112-currency-rules.yaml")} }
        54 { @{name="bond-validation"; desc="Bond validation rules"; deps=@("100-common-validation-rules.yaml")} }
        55 { @{name="bond-pricing"; desc="Bond pricing rules"; deps=@("113-pricing-rules.yaml")} }
        56 { @{name="bond-settlement"; desc="Bond settlement rules"; deps=@("111-timestamp-rules.yaml")} }
        57 { @{name="bond-risk"; desc="Bond risk calculation rules"; deps=@("114-risk-rules.yaml")} }
        58 { @{name="options-validation"; desc="Options validation rules"; deps=@("100-common-validation-rules.yaml")} }
        59 { @{name="futures-validation"; desc="Futures validation rules"; deps=@("100-common-validation-rules.yaml")} }
        60 { @{name="swaps-validation"; desc="Swaps validation rules"; deps=@("100-common-validation-rules.yaml")} }
        61 { @{name="derivatives-risk"; desc="Derivatives risk rules"; deps=@("114-risk-rules.yaml", "115-margin-rules.yaml")} }
        62 { @{name="fx-validation"; desc="FX validation rules"; deps=@("100-common-validation-rules.yaml")} }
        63 { @{name="fx-settlement"; desc="FX settlement rules"; deps=@("111-timestamp-rules.yaml", "112-currency-rules.yaml")} }
        64 { @{name="commodity-validation"; desc="Commodity validation rules"; deps=@("100-common-validation-rules.yaml")} }
        65 { @{name="commodity-settlement"; desc="Commodity settlement rules"; deps=@("116-physical-settlement-rules.yaml")} }
        66 { @{name="crypto-validation"; desc="Crypto validation rules"; deps=@("100-common-validation-rules.yaml")} }
        67 { @{name="crypto-settlement"; desc="Crypto settlement rules"; deps=@("117-digital-settlement-rules.yaml")} }
        68 { @{name="repo-validation"; desc="Repo validation rules"; deps=@("100-common-validation-rules.yaml")} }
        69 { @{name="repo-settlement"; desc="Repo settlement rules"; deps=@("111-timestamp-rules.yaml")} }
        70 { @{name="lending-validation"; desc="Securities lending validation"; deps=@("100-common-validation-rules.yaml")} }
        71 { @{name="lending-settlement"; desc="Securities lending settlement"; deps=@("118-collateral-rules.yaml")} }
        72 { @{name="prime-validation"; desc="Prime brokerage validation"; deps=@("100-common-validation-rules.yaml")} }
        73 { @{name="prime-settlement"; desc="Prime brokerage settlement"; deps=@("119-prime-services-rules.yaml")} }
        74 { @{name="clearing-validation"; desc="Clearing validation rules"; deps=@("100-common-validation-rules.yaml")} }
        75 { @{name="clearing-settlement"; desc="Clearing settlement rules"; deps=@("120-clearing-rules.yaml")} }
        76 { @{name="americas-validation"; desc="Americas validation rules"; deps=@("100-common-validation-rules.yaml", "121-regional-rules.yaml")} }
        77 { @{name="americas-settlement"; desc="Americas settlement rules"; deps=@("121-regional-rules.yaml")} }
        78 { @{name="emea-validation"; desc="EMEA validation rules"; deps=@("100-common-validation-rules.yaml", "122-emea-rules.yaml")} }
        79 { @{name="emea-settlement"; desc="EMEA settlement rules"; deps=@("122-emea-rules.yaml")} }
        80 { @{name="apac-validation"; desc="APAC validation rules"; deps=@("100-common-validation-rules.yaml", "123-apac-rules.yaml")} }
        81 { @{name="apac-settlement"; desc="APAC settlement rules"; deps=@("123-apac-rules.yaml")} }
        82 { @{name="latam-validation"; desc="LATAM validation rules"; deps=@("100-common-validation-rules.yaml", "124-latam-rules.yaml")} }
        83 { @{name="latam-settlement"; desc="LATAM settlement rules"; deps=@("124-latam-rules.yaml")} }
        84 { @{name="mena-validation"; desc="MENA validation rules"; deps=@("100-common-validation-rules.yaml", "125-mena-rules.yaml")} }
        85 { @{name="mena-settlement"; desc="MENA settlement rules"; deps=@("125-mena-rules.yaml")} }
        86 { @{name="mifid-validation"; desc="MiFID validation rules"; deps=@("126-regulatory-base.yaml")} }
        87 { @{name="mifid-reporting"; desc="MiFID reporting rules"; deps=@("127-reporting-base.yaml")} }
        88 { @{name="dodd-frank-validation"; desc="Dodd-Frank validation"; deps=@("126-regulatory-base.yaml")} }
        89 { @{name="dodd-frank-reporting"; desc="Dodd-Frank reporting"; deps=@("127-reporting-base.yaml")} }
        90 { @{name="basel-validation"; desc="Basel validation rules"; deps=@("126-regulatory-base.yaml")} }
        91 { @{name="basel-reporting"; desc="Basel reporting rules"; deps=@("127-reporting-base.yaml")} }
        92 { @{name="emir-validation"; desc="EMIR validation rules"; deps=@("126-regulatory-base.yaml")} }
        93 { @{name="emir-reporting"; desc="EMIR reporting rules"; deps=@("127-reporting-base.yaml")} }
        94 { @{name="cftc-validation"; desc="CFTC validation rules"; deps=@("126-regulatory-base.yaml")} }
        95 { @{name="cftc-reporting"; desc="CFTC reporting rules"; deps=@("127-reporting-base.yaml")} }
        96 { @{name="stress-test-validation"; desc="Stress test validation"; deps=@("128-stress-test-base.yaml")} }
        97 { @{name="liquidity-validation"; desc="Liquidity validation"; deps=@("129-liquidity-base.yaml")} }
        98 { @{name="capital-validation"; desc="Capital validation"; deps=@("130-capital-base.yaml")} }
        99 { @{name="operational-risk"; desc="Operational risk rules"; deps=@("131-operational-base.yaml")} }
        default { @{name="rule-config-$i"; desc="Rule configuration $i"; deps=@("100-common-validation-rules.yaml")} }
    }

    Create-YamlFile -fileName "$($i.ToString('000'))-$($name.name).yaml" -id $name.name -type "rule-config" -description $name.desc -dependencies $name.deps -ruleCount 3
}

Write-Host "Created rule configs 051-099"

# Create base rule files (100-131)
$baseRules = @(
    @{file="101-market-data-rules.yaml"; id="market-data-rules"; desc="Market data validation rules"; deps=@("110-reference-data-rules.yaml")}
    @{file="102-regulatory-rules.yaml"; id="regulatory-rules"; desc="Basic regulatory rules"; deps=@("126-regulatory-base.yaml")}
    @{file="110-reference-data-rules.yaml"; id="reference-data-rules"; desc="Reference data rules"; deps=@()}
    @{file="111-timestamp-rules.yaml"; id="timestamp-rules"; desc="Timestamp validation rules"; deps=@()}
    @{file="112-currency-rules.yaml"; id="currency-rules"; desc="Currency validation rules"; deps=@()}
    @{file="113-pricing-rules.yaml"; id="pricing-rules"; desc="Pricing calculation rules"; deps=@("110-reference-data-rules.yaml")}
    @{file="114-risk-rules.yaml"; id="risk-rules"; desc="Risk calculation rules"; deps=@("110-reference-data-rules.yaml")}
    @{file="115-margin-rules.yaml"; id="margin-rules"; desc="Margin calculation rules"; deps=@("114-risk-rules.yaml")}
    @{file="116-physical-settlement-rules.yaml"; id="physical-settlement-rules"; desc="Physical settlement rules"; deps=@("111-timestamp-rules.yaml")}
    @{file="117-digital-settlement-rules.yaml"; id="digital-settlement-rules"; desc="Digital settlement rules"; deps=@("111-timestamp-rules.yaml")}
    @{file="118-collateral-rules.yaml"; id="collateral-rules"; desc="Collateral management rules"; deps=@("114-risk-rules.yaml")}
    @{file="119-prime-services-rules.yaml"; id="prime-services-rules"; desc="Prime services rules"; deps=@("118-collateral-rules.yaml")}
    @{file="120-clearing-rules.yaml"; id="clearing-rules"; desc="Central clearing rules"; deps=@("115-margin-rules.yaml")}
    @{file="121-regional-rules.yaml"; id="regional-rules"; desc="Regional trading rules"; deps=@("110-reference-data-rules.yaml")}
    @{file="122-emea-rules.yaml"; id="emea-rules"; desc="EMEA specific rules"; deps=@("121-regional-rules.yaml")}
    @{file="123-apac-rules.yaml"; id="apac-rules"; desc="APAC specific rules"; deps=@("121-regional-rules.yaml")}
    @{file="124-latam-rules.yaml"; id="latam-rules"; desc="LATAM specific rules"; deps=@("121-regional-rules.yaml")}
    @{file="125-mena-rules.yaml"; id="mena-rules"; desc="MENA specific rules"; deps=@("121-regional-rules.yaml")}
    @{file="126-regulatory-base.yaml"; id="regulatory-base"; desc="Base regulatory framework"; deps=@("110-reference-data-rules.yaml")}
    @{file="127-reporting-base.yaml"; id="reporting-base"; desc="Base reporting framework"; deps=@("126-regulatory-base.yaml")}
    @{file="128-stress-test-base.yaml"; id="stress-test-base"; desc="Stress testing framework"; deps=@("114-risk-rules.yaml")}
    @{file="129-liquidity-base.yaml"; id="liquidity-base"; desc="Liquidity management base"; deps=@("114-risk-rules.yaml")}
    @{file="130-capital-base.yaml"; id="capital-base"; desc="Capital calculation base"; deps=@("114-risk-rules.yaml")}
    @{file="131-operational-base.yaml"; id="operational-base"; desc="Operational risk base"; deps=@("110-reference-data-rules.yaml")}
)

foreach ($rule in $baseRules) {
    Create-YamlFile -fileName $rule.file -id $rule.id -type "rule-config" -description $rule.desc -dependencies $rule.deps -ruleCount 4
}

Write-Host "Created base rules 101-131"

# Create enrichment files (150-199)
$enrichments = @()
for ($i = 150; $i -le 199; $i++) {
    $name = switch ($i) {
        150 { @{name="market-data-enrichment"; desc="Market data enrichment"; deps=@("250-market-data-config.yaml")} }
        151 { @{name="counterparty-enrichment"; desc="Counterparty enrichment"; deps=@("251-reference-data-config.yaml")} }
        152 { @{name="instrument-enrichment"; desc="Instrument enrichment"; deps=@("251-reference-data-config.yaml")} }
        153 { @{name="yield-curve-enrichment"; desc="Yield curve enrichment"; deps=@("252-yield-curve-config.yaml")} }
        154 { @{name="credit-rating-enrichment"; desc="Credit rating enrichment"; deps=@("253-credit-rating-config.yaml")} }
        155 { @{name="accrued-interest-enrichment"; desc="Accrued interest enrichment"; deps=@("252-yield-curve-config.yaml")} }
        156 { @{name="volatility-enrichment"; desc="Volatility enrichment"; deps=@("254-volatility-surface-config.yaml")} }
        157 { @{name="greeks-calculation"; desc="Greeks calculation enrichment"; deps=@("254-volatility-surface-config.yaml", "255-interest-rate-config.yaml")} }
        158 { @{name="margin-calculation"; desc="Margin calculation enrichment"; deps=@("256-margin-config.yaml")} }
        159 { @{name="fx-rate-enrichment"; desc="FX rate enrichment"; deps=@("257-fx-rate-config.yaml")} }
        160 { @{name="commodity-enrichment"; desc="Commodity price enrichment"; deps=@("258-commodity-config.yaml")} }
        161 { @{name="crypto-enrichment"; desc="Crypto price enrichment"; deps=@("259-crypto-config.yaml")} }
        162 { @{name="repo-enrichment"; desc="Repo rate enrichment"; deps=@("260-repo-config.yaml")} }
        163 { @{name="lending-enrichment"; desc="Securities lending enrichment"; deps=@("261-lending-config.yaml")} }
        164 { @{name="prime-enrichment"; desc="Prime brokerage enrichment"; deps=@("262-prime-config.yaml")} }
        165 { @{name="clearing-enrichment"; desc="Clearing enrichment"; deps=@("263-clearing-config.yaml")} }
        166 { @{name="americas-enrichment"; desc="Americas region enrichment"; deps=@("264-americas-config.yaml")} }
        167 { @{name="emea-enrichment"; desc="EMEA region enrichment"; deps=@("265-emea-config.yaml")} }
        168 { @{name="apac-enrichment"; desc="APAC region enrichment"; deps=@("266-apac-config.yaml")} }
        169 { @{name="latam-enrichment"; desc="LATAM region enrichment"; deps=@("267-latam-config.yaml")} }
        170 { @{name="mena-enrichment"; desc="MENA region enrichment"; deps=@("268-mena-config.yaml")} }
        171 { @{name="mifid-enrichment"; desc="MiFID enrichment"; deps=@("269-mifid-config.yaml")} }
        172 { @{name="dodd-frank-enrichment"; desc="Dodd-Frank enrichment"; deps=@("270-dodd-frank-config.yaml")} }
        173 { @{name="basel-enrichment"; desc="Basel enrichment"; deps=@("271-basel-config.yaml")} }
        174 { @{name="emir-enrichment"; desc="EMIR enrichment"; deps=@("272-emir-config.yaml")} }
        175 { @{name="cftc-enrichment"; desc="CFTC enrichment"; deps=@("273-cftc-config.yaml")} }
        default { @{name="enrichment-$i"; desc="Enrichment $i"; deps=@("250-market-data-config.yaml")} }
    }

    Create-YamlFile -fileName "$($i.ToString('000'))-$($name.name).yaml" -id $name.id -type "enrichment" -description $name.desc -dependencies $name.deps -enrichmentCount 2
}

Write-Host "Created enrichments 150-199"

# Create external data config files (250-299)
$externalConfigs = @()
for ($i = 250; $i -le 299; $i++) {
    $name = switch ($i) {
        250 { @{name="market-data-config"; desc="Market data configuration"} }
        251 { @{name="reference-data-config"; desc="Reference data configuration"} }
        252 { @{name="yield-curve-config"; desc="Yield curve data configuration"} }
        253 { @{name="credit-rating-config"; desc="Credit rating data configuration"} }
        254 { @{name="volatility-surface-config"; desc="Volatility surface configuration"} }
        255 { @{name="interest-rate-config"; desc="Interest rate configuration"} }
        256 { @{name="margin-config"; desc="Margin calculation configuration"} }
        257 { @{name="fx-rate-config"; desc="FX rate configuration"} }
        258 { @{name="commodity-config"; desc="Commodity data configuration"} }
        259 { @{name="crypto-config"; desc="Cryptocurrency data configuration"} }
        260 { @{name="repo-config"; desc="Repo rate configuration"} }
        261 { @{name="lending-config"; desc="Securities lending configuration"} }
        262 { @{name="prime-config"; desc="Prime brokerage configuration"} }
        263 { @{name="clearing-config"; desc="Clearing configuration"} }
        264 { @{name="americas-config"; desc="Americas region configuration"} }
        265 { @{name="emea-config"; desc="EMEA region configuration"} }
        266 { @{name="apac-config"; desc="APAC region configuration"} }
        267 { @{name="latam-config"; desc="LATAM region configuration"} }
        268 { @{name="mena-config"; desc="MENA region configuration"} }
        269 { @{name="mifid-config"; desc="MiFID configuration"} }
        270 { @{name="dodd-frank-config"; desc="Dodd-Frank configuration"} }
        271 { @{name="basel-config"; desc="Basel configuration"} }
        272 { @{name="emir-config"; desc="EMIR configuration"} }
        273 { @{name="cftc-config"; desc="CFTC configuration"} }
        default { @{name="external-config-$i"; desc="External configuration $i"} }
    }

    Create-YamlFile -fileName "$($i.ToString('000'))-$($name.name).yaml" -id $name.id -type "external-data-config" -description $name.desc
}

Write-Host "Created external configs 250-299"

# Create pipeline config files (350-399)
$pipelineConfigs = @()
for ($i = 350; $i -le 399; $i++) {
    $name = switch ($i) {
        350 { @{name="equity-processing-pipeline"; desc="Equity processing pipeline"; deps=@("050-equity-validation.yaml", "150-market-data-enrichment.yaml")} }
        351 { @{name="bond-processing-pipeline"; desc="Bond processing pipeline"; deps=@("054-bond-validation.yaml", "153-yield-curve-enrichment.yaml")} }
        352 { @{name="derivatives-processing-pipeline"; desc="Derivatives processing pipeline"; deps=@("058-options-validation.yaml", "156-volatility-enrichment.yaml")} }
        353 { @{name="fx-processing-pipeline"; desc="FX processing pipeline"; deps=@("062-fx-validation.yaml", "159-fx-rate-enrichment.yaml")} }
        354 { @{name="commodity-processing-pipeline"; desc="Commodity processing pipeline"; deps=@("064-commodity-validation.yaml", "160-commodity-enrichment.yaml")} }
        355 { @{name="crypto-processing-pipeline"; desc="Crypto processing pipeline"; deps=@("066-crypto-validation.yaml", "161-crypto-enrichment.yaml")} }
        356 { @{name="repo-processing-pipeline"; desc="Repo processing pipeline"; deps=@("068-repo-validation.yaml", "162-repo-enrichment.yaml")} }
        357 { @{name="lending-processing-pipeline"; desc="Securities lending pipeline"; deps=@("070-lending-validation.yaml", "163-lending-enrichment.yaml")} }
        358 { @{name="prime-processing-pipeline"; desc="Prime brokerage pipeline"; deps=@("072-prime-validation.yaml", "164-prime-enrichment.yaml")} }
        359 { @{name="clearing-processing-pipeline"; desc="Clearing pipeline"; deps=@("074-clearing-validation.yaml", "165-clearing-enrichment.yaml")} }
        default { @{name="pipeline-$i"; desc="Processing pipeline $i"; deps=@()} }
    }

    Create-YamlFile -fileName "$($i.ToString('000'))-$($name.name).yaml" -id $name.id -type "pipeline-config" -description $name.desc -dependencies $name.deps
}

Write-Host "Created pipeline configs 350-399"

Write-Host "`nGeneration complete! Created 200 YAML files for comprehensive dependency testing."
