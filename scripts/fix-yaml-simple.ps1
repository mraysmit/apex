# Simple PowerShell script to fix YAML metadata for APEX compliance
# Focuses on the most common patterns to fix quickly

$resourcesPath = "apex-demo\src\main\resources"

# List of files that need complete metadata sections (missing 'metadata' section)
$filesToFix = @(
    "enrichment/customer-transformer/customer-segments-config.yaml",
    "enrichment/customer-transformer/field-actions-config.yaml", 
    "enrichment/customer-transformer/transformer-rules-config.yaml",
    "enrichment/customer-transformer-demo.yaml",
    "enrichment/otc-bootstrap/data-sources-config.yaml",
    "enrichment/otc-bootstrap/enrichment-methods-config.yaml",
    "enrichment/otc-bootstrap/sample-otc-options-config.yaml",
    "enrichment/otc-options-bootstrap-demo.yaml",
    "enrichment/trade-transformer/trade-field-actions-config.yaml",
    "enrichment/trade-transformer/trade-risk-ratings-config.yaml",
    "enrichment/trade-transformer/trade-transformer-rules-config.yaml",
    "enrichment/trade-transformer-demo.yaml",
    "evaluation/advanced-features/advanced-features-test-data.yaml",
    "evaluation/advanced-features/collection-operations-config.yaml",
    "evaluation/advanced-features/dynamic-lookup-config.yaml",
    "evaluation/advanced-features/rule-engine-config.yaml",
    "evaluation/advanced-features/rule-result-features-config.yaml",
    "evaluation/advanced-features/template-processing-config.yaml",
    "evaluation/apex-advanced-features-demo.yaml",
    "evaluation/bootstrap/bootstrap-test-data.yaml",
    "evaluation/bootstrap/discount-rules.yaml",
    "evaluation/bootstrap/loan-approval-rules.yaml"
)

Write-Host "Fixing $($filesToFix.Count) YAML files with missing metadata sections..."

foreach ($relativePath in $filesToFix) {
    $fullPath = Join-Path $resourcesPath $relativePath
    
    if (-not (Test-Path $fullPath)) {
        Write-Host "  ⚠️  File not found: $relativePath"
        continue
    }
    
    Write-Host "Processing: $relativePath"
    
    $content = Get-Content $fullPath -Raw
    
    # Skip if already has metadata
    if ($content -match "^metadata:\s*\r?\n") {
        Write-Host "  ✓ Already has metadata"
        continue
    }
    
    # Determine file type and properties
    $fileName = [System.IO.Path]::GetFileNameWithoutExtension((Split-Path $relativePath -Leaf))
    $displayName = ($fileName -replace "-", " " -replace "_", " ").Split(' ') | ForEach-Object { $_.Substring(0,1).ToUpper() + $_.Substring(1).ToLower() } | Join-String -Separator ' '
    
    $fileType = "enrichment"
    $domain = "Financial Services"
    $tags = @("demo", "apex")
    
    # Path-based type detection
    if ($relativePath -match "bootstrap") {
        $fileType = "bootstrap"
        $domain = "Bootstrap Demonstrations"
        $tags += "bootstrap"
    }
    elseif ($relativePath -match "validation") {
        $fileType = "rule-config"
        $domain = "Data Validation"
        $tags += "validation"
    }
    elseif ($relativePath -match "evaluation") {
        $fileType = "enrichment"
        $domain = "Rule Evaluation"
        $tags += "evaluation"
    }
    elseif ($relativePath -match "transformer") {
        $fileType = "enrichment"
        $domain = "Data Transformation"
        $tags += "transformation"
    }
    
    # Create metadata section
    $metadataSection = @"
metadata:
  name: "$displayName"
  version: "2.0.0"
  description: "APEX $fileType configuration for $displayName"
  type: "$fileType"
  author: "Mark Andrew Ray-Smith Cityline Ltd"
  created-date: "2025-09-01"
  domain: "$domain"
  tags: [$($tags | ForEach-Object { '"' + $_ + '"' } | Join-String -Separator ", ")]

"@
    
    # Add type-specific required fields
    if ($fileType -eq "bootstrap") {
        $metadataSection = $metadataSection.Replace('  tags:', "  business-domain: `"$domain`"`r`n  created-by: `"demo.team@company.com`"`r`n  tags:")
    }
    
    # Find insertion point (after comments)
    $lines = $content -split "`r?`n"
    $insertIndex = 0
    
    for ($i = 0; $i -lt $lines.Length; $i++) {
        if ($lines[$i] -notmatch "^#" -and $lines[$i].Trim() -ne "") {
            $insertIndex = $i
            break
        }
    }
    
    # Insert metadata
    $beforeLines = $lines[0..($insertIndex-1)]
    $afterLines = $lines[$insertIndex..($lines.Length-1)]
    
    $newLines = @()
    $newLines += $beforeLines
    $newLines += ""
    $newLines += $metadataSection -split "`r?`n"
    $newLines += $afterLines
    
    # Write back to file
    $newContent = $newLines -join "`r`n"
    Set-Content -Path $fullPath -Value $newContent -Encoding UTF8
    
    Write-Host "  ✓ Added metadata section"
}

Write-Host "`nCompleted fixing YAML files!"
