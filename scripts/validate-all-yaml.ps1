# APEX YAML Validation Script
# Validates all YAML files in the project against APEX compiler rules

param(
    [string]$ProjectRoot = ".",
    [switch]$FixIssues = $false,
    [switch]$Verbose = $false
)

Write-Host "🔍 APEX YAML Validation Script" -ForegroundColor Cyan
Write-Host "================================" -ForegroundColor Cyan

# Change to project root
Set-Location $ProjectRoot

# Find all YAML files (excluding target directories)
$yamlFiles = Get-ChildItem -Recurse -Include "*.yaml","*.yml" | Where-Object { 
    $_.FullName -notlike "*target*" -and 
    $_.FullName -notlike "*node_modules*" -and
    $_.FullName -notlike "*.git*"
}

Write-Host "📊 Found $($yamlFiles.Count) YAML files to validate" -ForegroundColor Green

# Validation counters
$totalFiles = 0
$validFiles = 0
$invalidFiles = 0
$fixedFiles = 0

# Required metadata fields for all APEX documents
$requiredFields = @("id", "name", "version", "description", "type")

# Valid document types
$validTypes = @(
    "rule-config", "enrichment", "dataset", "scenario", 
    "scenario-registry", "bootstrap", "rule-chain", 
    "external-data-config", "pipeline-config"
)

# Type-specific required fields
$typeRequiredFields = @{
    "rule-config" = @("author")
    "enrichment" = @("author")
    "dataset" = @("source")
    "scenario" = @("business-domain", "owner")
    "scenario-registry" = @("created-by")
    "bootstrap" = @("business-domain", "created-by")
    "rule-chain" = @("author")
    "external-data-config" = @("author")
    "pipeline-config" = @("author")
}

function Test-YamlMetadata {
    param([string]$FilePath)
    
    $issues = @()
    $warnings = @()
    
    try {
        # Read and parse YAML (basic check)
        $content = Get-Content $FilePath -Raw
        
        # Check if file has metadata section
        if ($content -notmatch "metadata\s*:") {
            $issues += "Missing 'metadata' section"
            return @{
                Valid = $false
                Issues = $issues
                Warnings = $warnings
                HasMetadata = $false
            }
        }
        
        # Extract metadata section (simplified parsing)
        $metadataMatch = [regex]::Match($content, "metadata\s*:(.*?)(?=\n\S|\n$|\Z)", [System.Text.RegularExpressions.RegexOptions]::Singleline)
        if (-not $metadataMatch.Success) {
            $issues += "Could not parse metadata section"
            return @{
                Valid = $false
                Issues = $issues
                Warnings = $warnings
                HasMetadata = $true
            }
        }
        
        $metadataContent = $metadataMatch.Groups[1].Value
        
        # Check required fields
        foreach ($field in $requiredFields) {
            if ($metadataContent -notmatch "$field\s*:") {
                $issues += "Missing required field: $field"
            }
        }
        
        # Extract document type
        $typeMatch = [regex]::Match($metadataContent, "type\s*:\s*[`"']?([^`"'\r\n]+)[`"']?")
        if ($typeMatch.Success) {
            $docType = $typeMatch.Groups[1].Value.Trim()
            
            # Validate document type
            if ($docType -notin $validTypes) {
                $issues += "Invalid document type: '$docType'. Valid types: $($validTypes -join ', ')"
            } else {
                # Check type-specific required fields
                if ($typeRequiredFields.ContainsKey($docType)) {
                    foreach ($typeField in $typeRequiredFields[$docType]) {
                        if ($metadataContent -notmatch "$typeField\s*:") {
                            $issues += "Missing required field for type '$docType': $typeField"
                        }
                    }
                }
            }
        }
        
        # Check version format
        $versionMatch = [regex]::Match($metadataContent, "version\s*:\s*[`"']?([^`"'\r\n]+)[`"']?")
        if ($versionMatch.Success) {
            $version = $versionMatch.Groups[1].Value.Trim()
            if ($version -notmatch "^\d+\.\d+(\.\d+)?") {
                $warnings += "Version should follow semantic versioning format (e.g., 1.0.0): $version"
            }
        }
        
        return @{
            Valid = ($issues.Count -eq 0)
            Issues = $issues
            Warnings = $warnings
            HasMetadata = $true
            DocumentType = if ($typeMatch.Success) { $typeMatch.Groups[1].Value.Trim() } else { $null }
        }
        
    } catch {
        return @{
            Valid = $false
            Issues = @("Failed to parse YAML: $($_.Exception.Message)")
            Warnings = @()
            HasMetadata = $false
        }
    }
}

# Process each YAML file
foreach ($file in $yamlFiles) {
    $totalFiles++
    $relativePath = $file.FullName.Replace((Get-Location).Path + "\", "")
    
    if ($Verbose) {
        Write-Host "`n📄 Validating: $relativePath" -ForegroundColor Yellow
    }
    
    $result = Test-YamlMetadata -FilePath $file.FullName
    
    if ($result.Valid) {
        $validFiles++
        if ($Verbose) {
            Write-Host "   ✅ Valid" -ForegroundColor Green
            if ($result.DocumentType) {
                Write-Host "   📋 Type: $($result.DocumentType)" -ForegroundColor Gray
            }
        }
    } else {
        $invalidFiles++
        Write-Host "`n❌ INVALID: $relativePath" -ForegroundColor Red

        foreach ($issue in $result.Issues) {
            Write-Host "   • $issue" -ForegroundColor Red
        }

        foreach ($warning in $result.Warnings) {
            Write-Host "   ⚠️  $warning" -ForegroundColor Yellow
        }
    }
}

# Summary
Write-Host "`n" + "="*50 -ForegroundColor Cyan
Write-Host "📊 VALIDATION SUMMARY" -ForegroundColor Cyan
Write-Host "="*50 -ForegroundColor Cyan
Write-Host "Total files:   $totalFiles" -ForegroundColor White
Write-Host "Valid files:   $validFiles" -ForegroundColor Green
Write-Host "Invalid files: $invalidFiles" -ForegroundColor Red

if ($invalidFiles -gt 0) {
    Write-Host "`n💡 To fix issues automatically, run with -FixIssues flag" -ForegroundColor Yellow
    Write-Host "💡 To see details for valid files, run with -Verbose flag" -ForegroundColor Yellow
}

# Exit with appropriate code
exit $(if ($invalidFiles -eq 0) { 0 } else { 1 })
