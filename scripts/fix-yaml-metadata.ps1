# PowerShell script to fix YAML metadata for APEX compliance
# This script adds proper metadata sections to all YAML files that are missing them

$resourcesPath = "apex-demo\src\main\resources"
$yamlFiles = Get-ChildItem -Path $resourcesPath -Recurse -Filter "*.yaml"

Write-Host "Found $($yamlFiles.Count) YAML files to process..."

foreach ($file in $yamlFiles) {
    $content = Get-Content $file.FullName -Raw
    $relativePath = $file.FullName.Replace((Get-Location).Path + "\", "").Replace("\", "/")
    
    Write-Host "Processing: $relativePath"
    
    # Skip files that already have proper metadata
    if ($content -match "^metadata:\s*\n\s+name:") {
        Write-Host "  ✓ Already has proper metadata"
        continue
    }
    
    # Determine file type based on path and content
    $fileType = "enrichment"  # default
    $author = "Mark Andrew Ray-Smith Cityline Ltd"
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
    elseif ($relativePath -match "transformation") {
        $fileType = "rule-config"
        $domain = "Data Transformation"
        $tags += "transformation"
    }
    elseif ($relativePath -match "util") {
        $fileType = "rule-config"
        $domain = "Utilities"
        $tags += "utilities"
    }
    elseif ($relativePath -match "lookup") {
        $fileType = "external-data-config"
        $domain = "Data Lookup"
        $tags += "lookup"
    }
    elseif ($relativePath -match "infrastructure") {
        $fileType = "external-data-config"
        $domain = "Infrastructure"
        $tags += "infrastructure"
    }
    
    # Content-based type detection
    if ($content -match "dataSources:|data-sources:") {
        $fileType = "external-data-config"
    }
    elseif ($content -match "scenario:|scenarios:") {
        $fileType = "scenario"
        $domain = "Business Scenarios"
        $tags += "scenario"
    }
    elseif ($content -match "bootstrap:|rule-chains:") {
        $fileType = "bootstrap"
    }
    
    # Generate file name from path
    $fileName = [System.IO.Path]::GetFileNameWithoutExtension($file.Name)
    $displayName = $fileName -replace "-", " " -replace "_", " "
    $displayName = (Get-Culture).TextInfo.ToTitleCase($displayName.ToLower())
    
    # Create metadata section
    $metadataSection = @"
metadata:
  name: "$displayName"
  version: "2.0.0"
  description: "APEX $fileType configuration for $displayName"
  type: "$fileType"
  author: "$author"
  created-date: "2025-09-01"
  domain: "$domain"
  tags: [$($tags | ForEach-Object { '"' + $_ + '"' } | Join-String -Separator ", ")]

"@
    
    # Add type-specific required fields
    if ($fileType -eq "scenario") {
        $metadataSection = $metadataSection.Replace("  tags:", "  business-domain: `"$domain`"`n  owner: `"demo.team@company.com`"`n  tags:")
    }
    elseif ($fileType -eq "bootstrap") {
        $metadataSection = $metadataSection.Replace("  tags:", "  business-domain: `"$domain`"`n  created-by: `"demo.team@company.com`"`n  tags:")
    }
    elseif ($fileType -eq "dataset") {
        $metadataSection = $metadataSection.Replace("  tags:", "  source: `"demo-data`"`n  tags:")
    }
    
    # Handle existing type/name/description lines
    $newContent = $content
    
    # Remove old format type/name/description lines if they exist
    $newContent = $newContent -replace "^type:\s*`"[^`"]*`"\s*\n", ""
    $newContent = $newContent -replace "^name:\s*`"[^`"]*`"\s*\n", ""
    $newContent = $newContent -replace "^description:\s*`"[^`"]*`"\s*\n", ""
    
    # Add metadata section at the beginning (after comments)
    $lines = $newContent -split "`n"
    $insertIndex = 0
    
    # Find where to insert metadata (after initial comments)
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
    $newLines += $metadataSection -split "`n"
    $newLines += $afterLines
    
    # Write back to file
    $newContent = $newLines -join "`n"
    Set-Content -Path $file.FullName -Value $newContent -Encoding UTF8
    
    Write-Host "  ✓ Added metadata section with type: $fileType"
}

Write-Host "`nCompleted processing all YAML files!"
Write-Host "All files now have proper APEX metadata sections."
